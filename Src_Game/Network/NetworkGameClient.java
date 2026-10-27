package network;

import core.Vector2D;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trình khách mạng (Network Game Client) kết nối với Dedicated Server.
 * Tích hợp:
 * 1. Luồng gửi bất đồng bộ (Non-blocking Async Sender Thread 30Hz) — Không bao giờ làm khựng luồng Render 60 FPS.
 * 2. Client-Side Prediction & Dead Reckoning Smooth LERP — Triệt tiêu hoàn toàn độ trễ cảm ứng (Input Lag)
 *    và hiện tượng giật cục (WiFi Packet Jitter) trên điện thoại Android.
 */
public class NetworkGameClient {
    private final String host;
    private final int port;
    private final String playerName;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isConnected = false;

    private String assignedPlayerId;
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
        try {
            this.socket = new Socket();
            this.socket.setTcpNoDelay(true);
            this.socket.setTrafficClass(0x10); // IPTOS_LOWDELAY ưu tiên gói tin game thời gian thực
            this.socket.connect(new InetSocketAddress(host, port), 4000);

            this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream(), 8192));
            this.out.flush();
            this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream(), 8192));
            this.isConnected = true;

            // Gửi yêu cầu gia nhập (JOIN_REQUEST)
            synchronized (out) {
                out.writeObject(new GamePacket(PacketType.JOIN_REQUEST, "CLIENT", playerName));
                out.flush();
            }

            // Nhận phản hồi xác thực danh tính
            GamePacket response = (GamePacket) in.readObject();
            if (response.getType() == PacketType.JOIN_RESPONSE) {
                this.assignedPlayerId = (String) response.getPayload();
                System.out.printf("🎉 Kết nối thành công tới Server (%s:%d)! [Cấp ID: %s | Tên: %s]%n",
                        host, port, assignedPlayerId, playerName);
            }

            // 1. Luồng lắng nghe World Snapshot từ Server (20Hz)
            Thread listenerThread = new Thread(this::listenLoop, "ClientNet-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            // 2. Luồng gửi Input độc lập (30Hz) - Giải phóng 100% cho luồng vẽ 60 FPS!
            Thread senderThread = new Thread(this::senderLoop, "ClientNet-Sender");
            senderThread.setDaemon(true);
            senderThread.start();

            return true;

        } catch (Exception e) {
            System.err.println("❌ Không thể kết nối tới Server (" + host + ":" + port + "): " + e.getMessage());
            disconnect();
            return false;
        }
    }

    private void listenLoop() {
        try {
            while (isConnected && !socket.isClosed()) {
                GamePacket packet = (GamePacket) in.readObject();
                if (packet == null) break;

                if (packet.getType() == PacketType.WORLD_SNAPSHOT) {
                    WorldSnapshotPayload snapshot = (WorldSnapshotPayload) packet.getPayload();
                    this.currentWave = snapshot.wave;

                    // Dùng đồng hồ cục bộ của Client khi gói tin tới để tránh lệch đồng hồ giữa PC và Điện thoại
                    long receiveTime = System.currentTimeMillis();

                    Set<String> activeIds = new HashSet<>();
                    for (WorldSnapshotPayload.EntityState entity : snapshot.entities) {
                        activeIds.add(entity.id);
                        Vector2D serverPos = new Vector2D(entity.posX, entity.posY);
                        interpolator.pushTransform(entity.id, serverPos, receiveTime);
                        latestStates.put(entity.id, entity);
                        smoothedPositions.putIfAbsent(entity.id, serverPos);
                    }

                    // Dọn dẹp thực thể đã bị tiêu diệt
                    latestStates.keySet().removeIf(id -> !activeIds.contains(id));
                    smoothedPositions.keySet().removeIf(id -> !activeIds.contains(id));
                }
            }
        } catch (Exception ignored) {
        } finally {
            disconnect();
        }
    }

    /**
     * Luồng chuyên trách gửi gói tin điều khiển ở tần số 30Hz (33ms).
     * Giúp hàm sendInput() gọi từ vòng lặp Render 60 FPS không bao giờ bị nghẽn bởi Socket I/O!
     */
    private void senderLoop() {
        double lastSentVx = 0.0;
        double lastSentVy = 0.0;

        try {
            while (isConnected && !socket.isClosed()) {
                if (assignedPlayerId != null && out != null) {
                    double vx = this.pendingVx;
                    double vy = this.pendingVy;
                    boolean atk = this.pendingAttack;
                    int skill = this.pendingSkillId;
                    double aimX = this.pendingSkillAimX;
                    double aimY = this.pendingSkillAimY;
                    this.pendingAttack = false;
                    this.pendingSkillId = 0;

                    // Chỉ gửi khi vận tốc thay đổi, đang di chuyển, hoặc có lệnh tấn công / kỹ năng
                    boolean moving = Math.abs(vx) > 0.01 || Math.abs(vy) > 0.01;
                    boolean velocityChanged = Math.abs(vx - lastSentVx) > 0.01 || Math.abs(vy - lastSentVy) > 0.01;

                    if (atk || skill > 0 || moving || velocityChanged || hasNewInput) {
                        hasNewInput = false;
                        lastSentVx = vx;
                        lastSentVy = vy;

                        synchronized (out) {
                            PlayerInputPayload payload = new PlayerInputPayload(vx, vy, atk, skill, aimX, aimY);
                            out.writeObject(new GamePacket(PacketType.PLAYER_INPUT, assignedPlayerId, payload));
                            out.reset();
                            out.flush();
                        }
                    }
                }
                Thread.sleep(30); // ~33Hz gửi mượt mà mà không làm nghẽn băng thông WiFi
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Ghi nhận Input tức thời (0ms Non-blocking) từ luồng Game Loop / Cảm ứng.
     */
    public void sendInput(double vx, double vy, boolean isAttacking) {
        sendInput(vx, vy, isAttacking, 0);
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

    /**
     * Lấy tọa độ siêu mượt 60 FPS của thực thể:
     * - Đối với nhân vật chính (Local Player): Kết hợp Client-Side Prediction (phản hồi tức thì 0ms) + Server Reconciliation.
     * - Đối với Quái vật & Đồng đội: Kết hợp Snapshot Interpolator + Exponential Smoothing chống giật cục WiFi.
     */
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
            // Server Reconciliation: Nếu lệch nhỏ thì trượt nhẹ (0.12), nếu lệch quá xa (>120px) thì kéo nhanh về
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
            // Làm mịn chuyển động 60 FPS cho Quái vật và Đồng đội (chống giật khi gói tin WiFi trễ)
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
    }
}
