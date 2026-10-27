package network;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cơ chế tự động dò tìm phòng chơi trong mạng nội bộ (LAN Auto-Discovery via UDP Broadcast).
 * Giúp người chơi cùng mạng WiFi thấy phòng của nhau ngay lập tức mà KHÔNG CẦN nhập IP hay Port!
 */
public class LanDiscovery {
    public static final int BROADCAST_PORT = 8888;
    public static final String BROADCAST_HEADER = "UML_ARENA_ROOM:";

    public static class DiscoveredRoom {
        public final String roomCode;
        public final String hostName;
        public final String hostIp;
        public final int serverPort;
        public final int playerCount;
        public long lastSeenTime;

        public DiscoveredRoom(String roomCode, String hostName, String hostIp, int serverPort, int playerCount) {
            this.roomCode = roomCode;
            this.hostName = hostName;
            this.hostIp = hostIp;
            this.serverPort = serverPort;
            this.playerCount = playerCount;
            this.lastSeenTime = System.currentTimeMillis();
        }
    }

    private DatagramSocket socket;
    private volatile boolean isRunning = false;
    private final Map<String, DiscoveredRoom> activeRooms = new ConcurrentHashMap<>();

    /**
     * Bắt đầu phát sóng thông tin phòng ra toàn mạng nội bộ (Dành cho máy Host).
     */
    public void startBroadcasting(String roomCode, String hostName, int serverPort) {
        isRunning = true;
        new Thread(() -> {
            try (DatagramSocket broadcastSocket = new DatagramSocket()) {
                broadcastSocket.setBroadcast(true);

                while (isRunning) {
                    String message = String.format("%s%s;%s;%d;1", BROADCAST_HEADER, roomCode, hostName, serverPort);
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);

                    DatagramPacket packet = new DatagramPacket(
                            data, data.length,
                            InetAddress.getByName("255.255.255.255"), BROADCAST_PORT
                    );
                    broadcastSocket.send(packet);
                    Thread.sleep(1500); // Phát sóng mỗi 1.5 giây
                }
            } catch (Exception ignored) {}
        }, "LanDiscovery-Broadcaster").start();
    }

    /**
     * Bắt đầu lắng nghe các phòng đang mở trong mạng (Dành cho máy Khách).
     */
    public void startListening() {
        isRunning = true;
        new Thread(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(BROADCAST_PORT));
                socket.setBroadcast(true);
                byte[] buffer = new byte[1024];

                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (text.startsWith(BROADCAST_HEADER)) {
                        String body = text.substring(BROADCAST_HEADER.length());
                        String[] parts = body.split(";");
                        if (parts.length >= 4) {
                            String code = parts[0];
                            String hostName = parts[1];
                            int port = Integer.parseInt(parts[2]);
                            int players = Integer.parseInt(parts[3]);
                            String hostIp = packet.getAddress().getHostAddress();

                            activeRooms.put(code, new DiscoveredRoom(code, hostName, hostIp, port, players));
                        }
                    }

                    // Dọn dẹp phòng cũ không còn phát sóng quá 4 giây
                    long now = System.currentTimeMillis();
                    activeRooms.values().removeIf(r -> now - r.lastSeenTime > 4000);
                }
            } catch (Exception ignored) {
            } finally {
                stop();
            }
        }, "LanDiscovery-Listener").start();
    }

    public Map<String, DiscoveredRoom> getActiveRooms() {
        return activeRooms;
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
