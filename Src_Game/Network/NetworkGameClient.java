package network;

import core.Vector2D;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trình khách mạng (Network Game Client) kết nối với Dedicated Server.
 * Tích hợp Công nghệ Kết Nối Lai (Hybrid LAN + Cloud Internet Relay):
 * 1. Cùng WiFi (LAN): Kết nối TCP trực tiếp độ trễ ~1ms.
 * 2. Khác WiFi / Mạng 4G / 5G (Internet): Tự động chuyển sang kênh Cloud Internet Relay (MQTT Tunnel)
 *    theo Mã Phòng (ví dụ: ARENA-1-17) mà KHÔNG cần mở Port Router!
 * 3. Luồng gửi bất đồng bộ (Non-blocking Async Sender Thread 30Hz) + Client-Side Prediction 60 FPS.
 */
public class NetworkGameClient {
    private final String host;
    private final int port;
    private final String playerName;

    // Kênh 1: Kết nối LAN TCP trực tiếp (Cùng WiFi)
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Kênh 2: Kết nối Cloud Internet Relay (Khác WiFi / 4G / 5G)
    private MiniMqttClient cloudMqttClient;
    private String cloudRoomCode;
    private String cloudSessionId;
    private volatile boolean usingCloudRelay = false;

    private volatile boolean isConnected = false;
    private volatile String assignedPlayerId;
    private final SnapshotInterpolator interpolator;
    private final Map<String, WorldSnapshotPayload.EntityState> latestStates = new ConcurrentHashMap<>();
    private final Map<String, Vector2D> smoothedPositions = new ConcurrentHashMap<>();
    private volatile int currentWave = 1;

    // Biến đệm Input bất đồng bộ (Non-blocking Input State)
    private volatile double pendingVx = 0.0;
    private volatile double pendingVy = 0.0;
    private volatile boolean pendingAttack = false;
    private volatile int pendingSkillId = 0;
    private volatile double pendingSkillAimX = 0.0;
    private volatile double pendingSkillAimY = 0.0;
    private volatile boolean hasNewInput = false;

    public NetworkGameClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.interpolator = new SnapshotInterpolator(65); // 65ms đệm nội suy tối ưu phản hồi nhanh
    }

    public boolean connect() {
        // Bước 1: Thử kết nối trực tiếp qua mạng nội bộ LAN/WiFi (timeout nhanh 1.4 giây)
        if (tryConnectDirectLan()) {
            return true;
        }

        // Bước 2: Nếu khác mạng WiFi hoặc dùng 4G/5G -> Tự động kết nối xuyên mạng qua Cloud Internet Relay!
        System.out.println("☁️ Không cùng lớp mạng WiFi với " + host + ", đang chuyển sang kênh Cloud Internet (4G/Khác WiFi)...");
        return tryConnectCloudRelay();
    }

    private boolean tryConnectDirectLan() {
        // Nếu người dùng truyền trực tiếp chuỗi dạng ARENA-X-Y chưa phân giải thì bỏ qua LAN để vào Cloud hoặc phân giải trước
        if (host == null || host.toUpperCase().startsWith("ARENA-")) {
            return false;
        }
        try {
            this.socket = new Socket();
            this.socket.setTcpNoDelay(true);
            this.socket.setTrafficClass(0x10);
            int timeoutMs = (host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost")) ? 1500 : 1400;
            this.socket.connect(new InetSocketAddress(host, port), timeoutMs);

            this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream(), 8192));
            this.out.flush();
            this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream(), 8192));
            this.usingCloudRelay = false;
            this.isConnected = true;

            synchronized (out) {
                out.writeObject(new GamePacket(PacketType.JOIN_REQUEST, "CLIENT", playerName));
                out.flush();
            }

            GamePacket response = (GamePacket) in.readObject();
            if (response.getType() == PacketType.JOIN_RESPONSE) {
                this.assignedPlayerId = (String) response.getPayload();
                System.out.printf("🎉 [LAN WiFi] Kết nối thành công tới Server (%s:%d)! [Cấp ID: %s | Tên: %s]%n",
                        host, port, assignedPlayerId, playerName);
            }

            Thread listenerThread = new Thread(this::listenLoop, "ClientNet-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            Thread senderThread = new Thread(this::senderLoop, "ClientNet-Sender");
            senderThread.setDaemon(true);
            senderThread.start();

            return true;
        } catch (Exception e) {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
            socket = null;
            return false;
        }
    }

    private boolean tryConnectCloudRelay() {
        try {
            this.cloudRoomCode = LanDiscovery.ipOrInputToRoomCode(host);
            this.cloudSessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            this.assignedPlayerId = null;

            this.cloudMqttClient = new MiniMqttClient(this::onCloudMessage);
            if (!this.cloudMqttClient.connect()) {
                return false;
            }

            String s2cTopic = "huce_oop_2026/room/" + cloudRoomCode + "/s2c/" + cloudSessionId;
            String c2sTopic = "huce_oop_2026/room/" + cloudRoomCode + "/c2s/" + cloudSessionId;
            this.cloudMqttClient.subscribe(s2cTopic);

            // Chờ 120ms để đăng ký kênh nhận S2C hoàn tất trên Broker rồi gửi JOIN_REQUEST
            Thread.sleep(120);

            byte[] joinBytes = CloudServerBridge.serialize(new GamePacket(PacketType.JOIN_REQUEST, "CLIENT", playerName));
            for (int attempt = 0; attempt < 8 && assignedPlayerId == null; attempt++) {
                this.cloudMqttClient.publish(c2sTopic, joinBytes);
                for (int w = 0; w < 6 && assignedPlayerId == null; w++) {
                    Thread.sleep(100);
                }
            }

            if (assignedPlayerId != null) {
                this.usingCloudRelay = true;
                this.isConnected = true;
                System.out.printf("🌐 [Cloud Internet 4G/WiFi] Đã vào phòng [%s] thành công! [ID: %s | Tên: %s]%n",
                        cloudRoomCode, assignedPlayerId, playerName);

                Thread senderThread = new Thread(this::senderLoop, "ClientCloud-Sender");
                senderThread.setDaemon(true);
                senderThread.start();
                return true;
            }

            disconnect();
            return false;
        } catch (Exception e) {
            disconnect();
            return false;
        }
    }

    private void onCloudMessage(String topic, byte[] payload) {
        try {
            GamePacket packet = CloudServerBridge.deserialize(payload);
            processIncomingPacket(packet);
        } catch (Exception ignored) {
        }
    }

    private void listenLoop() {
        try {
            while (isConnected && socket != null && !socket.isClosed()) {
                GamePacket packet = (GamePacket) in.readObject();
                if (packet == null) break;
                processIncomingPacket(packet);
            }
        } catch (Exception ignored) {
        } finally {
            disconnect();
        }
    }

    private void processIncomingPacket(GamePacket packet) {
        if (packet == null) return;

        if (packet.getType() == PacketType.JOIN_RESPONSE) {
            this.assignedPlayerId = (String) packet.getPayload();
        } else if (packet.getType() == PacketType.WORLD_SNAPSHOT) {
            WorldSnapshotPayload snapshot = (WorldSnapshotPayload) packet.getPayload();
            this.currentWave = snapshot.wave;

            long receiveTime = System.currentTimeMillis();

            Set<String> activeIds = new HashSet<>();
            for (WorldSnapshotPayload.EntityState entity : snapshot.entities) {
                activeIds.add(entity.id);
                Vector2D serverPos = new Vector2D(entity.posX, entity.posY);
                interpolator.pushTransform(entity.id, serverPos, receiveTime);
                latestStates.put(entity.id, entity);
                smoothedPositions.putIfAbsent(entity.id, serverPos);
            }

            latestStates.keySet().removeIf(id -> !activeIds.contains(id));
            smoothedPositions.keySet().removeIf(id -> !activeIds.contains(id));
        }
    }

    /**
     * Luồng chuyên trách gửi gói tin điều khiển ở tần số ~30Hz.
     * Hỗ trợ cả 2 chế độ: Socket LAN trực tiếp và Cloud Internet Relay (4G / Khác WiFi).
     */
    private void senderLoop() {
        double lastSentVx = 0.0;
        double lastSentVy = 0.0;
        String c2sTopic = (cloudRoomCode != null && cloudSessionId != null)
                ? ("huce_oop_2026/room/" + cloudRoomCode + "/c2s/" + cloudSessionId)
                : null;

        try {
            while (isConnected) {
                if (assignedPlayerId != null) {
                    double vx = this.pendingVx;
                    double vy = this.pendingVy;
                    boolean atk = this.pendingAttack;
                    int skill = this.pendingSkillId;
                    double aimX = this.pendingSkillAimX;
                    double aimY = this.pendingSkillAimY;
                    this.pendingAttack = false;
                    this.pendingSkillId = 0;

                    boolean moving = Math.abs(vx) > 0.01 || Math.abs(vy) > 0.01;
                    boolean velocityChanged = Math.abs(vx - lastSentVx) > 0.01 || Math.abs(vy - lastSentVy) > 0.01;

                    if (atk || skill > 0 || moving || velocityChanged || hasNewInput) {
                        hasNewInput = false;
                        lastSentVx = vx;
                        lastSentVy = vy;

                        PlayerInputPayload payload = new PlayerInputPayload(vx, vy, atk, skill, aimX, aimY);
                        GamePacket packet = new GamePacket(PacketType.PLAYER_INPUT, assignedPlayerId, payload);

                        if (usingCloudRelay) {
                            if (cloudMqttClient != null && cloudMqttClient.isConnected() && c2sTopic != null) {
                                byte[] bytes = CloudServerBridge.serialize(packet);
                                cloudMqttClient.publish(c2sTopic, bytes);
                            }
                        } else if (out != null) {
                            synchronized (out) {
                                out.writeObject(packet);
                                out.reset();
                                out.flush();
                            }
                        }
                    }
                }
                Thread.sleep(usingCloudRelay ? 40 : 30);
            }
        } catch (Exception ignored) {
        }
    }

    public void sendInput(double vx, double vy, boolean isAttacking) {
        sendInput(vx, vy, isAttacking, 0, 0.0, 0.0);
    }

    public void sendInput(double vx, double vy, boolean isAttacking, int skillId) {
        sendInput(vx, vy, isAttacking, skillId, 0.0, 0.0);
    }

    public void sendInput(double vx, double vy, boolean isAttacking, int skillId, double skillAimX, double skillAimY) {
        this.pendingVx = vx;
        this.pendingVy = vy;
        if (isAttacking) {
            this.pendingAttack = true;
        }
        if (skillId > 0) {
            this.pendingSkillId = skillId;
            this.pendingSkillAimX = skillAimX;
            this.pendingSkillAimY = skillAimY;
        }
        this.hasNewInput = true;

        // Tiên đoán chuyển động phía Client (Client-Side Prediction) cho nhân vật của chính mình
        if (assignedPlayerId != null) {
            Vector2D cur = smoothedPositions.get(assignedPlayerId);
            if (cur != null) {
                double dt = 1.0 / 60.0;
                double nextX = Math.max(40, Math.min(1560, cur.getX() + vx * dt));
                double nextY = Math.max(40, Math.min(1160, cur.getY() + vy * dt));
                if (skillId == 1) {
                    double mag = Math.hypot(skillAimX, skillAimY);
                    if (mag > 0.05) {
                        nextX = Math.max(40, Math.min(1560, cur.getX() + (skillAimX / mag) * 165.0));
                        nextY = Math.max(40, Math.min(1160, cur.getY() + (skillAimY / mag) * 165.0));
                    }
                }
                smoothedPositions.put(assignedPlayerId, new Vector2D(nextX, nextY));
            }
        }
    }

    public Vector2D getInterpolatedPosition(String entityId, long clientTime) {
        WorldSnapshotPayload.EntityState state = latestStates.get(entityId);
        Vector2D targetPos = interpolator.interpolate(entityId, clientTime);
        if (targetPos == null && state != null) {
            targetPos = new Vector2D(state.posX, state.posY);
        }
        if (targetPos == null) return null;

        Vector2D currentSmooth = smoothedPositions.get(entityId);
        if (currentSmooth == null) {
            smoothedPositions.put(entityId, targetPos);
            return targetPos;
        }

        boolean isLocalPlayer = entityId.equals(assignedPlayerId);
        if (isLocalPlayer && state != null) {
            Vector2D authPos = new Vector2D(state.posX, state.posY);
            double dist = currentSmooth.distanceTo(authPos);
            double blend = (dist > 120.0) ? 0.5 : 0.12;
            Vector2D next = new Vector2D(
                    currentSmooth.getX() + (authPos.getX() - currentSmooth.getX()) * blend,
                    currentSmooth.getY() + (authPos.getY() - currentSmooth.getY()) * blend
            );
            smoothedPositions.put(entityId, next);
            return next;
        } else {
            double blend = 0.25;
            Vector2D next = new Vector2D(
                    currentSmooth.getX() + (targetPos.getX() - currentSmooth.getX()) * blend,
                    currentSmooth.getY() + (targetPos.getY() - currentSmooth.getY()) * blend
            );
            smoothedPositions.put(entityId, next);
            return next;
        }
    }

    public String getAssignedPlayerId() {
        return assignedPlayerId;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isUsingCloudRelay() {
        return usingCloudRelay;
    }

    public Map<String, WorldSnapshotPayload.EntityState> getLatestStates() {
        return latestStates;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        if (cloudMqttClient != null) {
            cloudMqttClient.close();
        }
    }
}
