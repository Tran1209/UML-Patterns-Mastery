package network;

import core.Vector2D;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trình khách mạng (Network Game Client) kết nối với Dedicated Server.
 * Tích hợp bộ giải thuật Snapshot Interpolation (LERP) của Joshua Glazer & Sanjay Madhav (Ch. 7).
 * Triệt tiêu hoàn toàn hiện tượng giật lắc hình (Micro-stutter) giữa gói tin 20Hz và render 60 FPS.
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
    private volatile int currentWave = 1;

    public NetworkGameClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.interpolator = new SnapshotInterpolator(100); // 100ms đệm nội suy = 2 ticks của 20Hz
    }

    public boolean connect() {
        try {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.isConnected = true;

            // Gửi yêu cầu gia nhập (JOIN_REQUEST)
            out.writeObject(new GamePacket(PacketType.JOIN_REQUEST, "CLIENT", playerName));
            out.flush();

            // Nhận phản hồi xác thực danh tính
            GamePacket response = (GamePacket) in.readObject();
            if (response.getType() == PacketType.JOIN_RESPONSE) {
                this.assignedPlayerId = (String) response.getPayload();
                System.out.printf("🎉 Kết nối thành công tới Server! [Cấp ID: %s | Tên: %s]%n", assignedPlayerId, playerName);
            }

            // Bắt đầu luồng lắng nghe World Snapshot từ Server (20Hz)
            new Thread(this::listenLoop, "ClientNet-Listener").start();
            return true;

        } catch (Exception e) {
            System.err.println("❌ Không thể kết nối tới Server: " + e.getMessage());
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

                    // Đưa dữ liệu tọa độ từng thực thể vào bộ đệm nội suy LERP
                    for (WorldSnapshotPayload.EntityState entity : snapshot.entities) {
                        interpolator.pushTransform(
                                entity.id,
                                new Vector2D(entity.posX, entity.posY),
                                snapshot.serverTimestamp
                        );
                        latestStates.put(entity.id, entity);
                    }

                    // Dọn dẹp thực thể không còn tồn tại trong snapshot
                    Set<String> activeIds = new HashSet<>();
                    for (WorldSnapshotPayload.EntityState e : snapshot.entities) {
                        activeIds.add(e.id);
                    }
                    latestStates.keySet().removeIf(id -> !activeIds.contains(id));
                }
            }
        } catch (Exception ignored) {
        } finally {
            disconnect();
        }
    }

    public void sendInput(double vx, double vy, boolean isAttacking) {
        if (!isConnected || out == null) return;
        try {
            PlayerInputPayload payload = new PlayerInputPayload(vx, vy, isAttacking);
            out.writeObject(new GamePacket(PacketType.PLAYER_INPUT, assignedPlayerId, payload));
            out.reset();
        } catch (IOException e) {
            disconnect();
        }
    }

    /**
     * Lấy tọa độ mượt mà của thực thể tại thời điểm vẽ 60 FPS thông qua thuật toán LERP.
     */
    public Vector2D getInterpolatedPosition(String entityId, long clientTime) {
        return interpolator.interpolate(entityId, clientTime);
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

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
