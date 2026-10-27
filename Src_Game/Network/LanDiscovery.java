package network;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cơ chế tự động dò tìm phòng chơi trong mạng nội bộ (LAN Auto-Discovery via UDP Broadcast)
 * kết hợp Mã Phòng Thông Minh (Smart IP-Encoded Room Code).
 *
 * Ưu điểm vượt trội:
 * 1. Tự động phát sóng (UDP Broadcast) liên tục suốt thời gian chủ phòng ở trong trận chiến.
 * 2. Mã phòng thông minh (ví dụ: ARENA-1-17 ứng với IP 192.168.1.17) giúp giải mã ra ngay IP
 *    kể cả khi Router WiFi bật chế độ chặn gói tin quảng bá (AP Isolation / UDP Filter)!
 */
public class LanDiscovery {
    public static final int BROADCAST_PORT = 8888;
    public static final String BROADCAST_HEADER = "UML_ARENA_ROOM:";
    public static final String CLOUD_LOBBY_TOPIC = "huce_oop_2026/lobby/rooms";

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

    private DatagramSocket listenSocket;
    private MiniMqttClient cloudListenClient;
    private MiniMqttClient cloudBroadcastClient;
    private CloudServerBridge cloudServerBridge;
    private volatile boolean isListening = false;
    private volatile boolean isBroadcasting = false;
    private final Map<String, DiscoveredRoom> activeRooms = new ConcurrentHashMap<>();

    /**
     * Lấy địa chỉ IPv4 LAN/WiFi thực tế của thiết bị (PC hoặc Điện thoại Android).
     */
    public static String getLocalLanIp() {
        // Cách 1: Dùng bảng định tuyến UDP của hệ điều hành (nhanh và chính xác nhất)
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = s.getLocalAddress().getHostAddress();
            if (ip != null && !ip.equals("0.0.0.0") && !ip.equals("127.0.0.1")) {
                return ip;
            }
        } catch (Exception ignored) {
        }

        // Cách 2: Quét các card mạng vật lý đang hoạt động
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallbackIp = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.")) {
                            return ip; // Ưu tiên dải mạng WiFi gia đình phổ biến nhất
                        }
                        if (ip.startsWith("10.") || ip.startsWith("172.")) {
                            fallbackIp = ip;
                        }
                    }
                }
            }
            if (fallbackIp != null) return fallbackIp;
        } catch (Exception ignored) {
        }

        return "127.0.0.1";
    }

    /**
     * Sinh Mã Phòng Thông Minh tự động mã hóa địa chỉ IP LAN của máy chủ.
     * Ví dụ: IP 192.168.1.17 -> Mã phòng: ARENA-1-17
     */
    public static String generateSmartRoomCode() {
        String ip = getLocalLanIp();
        if (ip.startsWith("192.168.")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return "ARENA-" + parts[2] + "-" + parts[3];
            }
        }
        if (!ip.equals("127.0.0.1") && ip.contains(".")) {
            return "ARENA-" + ip.replace('.', '-');
        }
        return "ARENA-" + (1000 + new Random().nextInt(9000));
    }

    /**
     * Giải mã Mã Phòng (ví dụ: ARENA-1-17, 1-17, 17) hoặc chuỗi nhập vào thành Địa chỉ IP máy chủ.
     */
    public String resolveRoomCodeToIp(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return "127.0.0.1";
        }

        String clean = rawInput.trim();
        // Nếu có kèm port dạng host:port thì tách lấy phần host trước
        if (clean.contains(":")) {
            clean = clean.split(":")[0].trim();
        }

        // 1. Tra cứu trong danh sách phòng đã dò tìm qua UDP Broadcast
        for (DiscoveredRoom r : activeRooms.values()) {
            if (r.roomCode.equalsIgnoreCase(clean)) {
                return r.hostIp;
            }
        }

        // 2. Giải mã trực tiếp từ cấu trúc Mã Phòng Thông Minh (ARENA-X-Y)
        String upper = clean.toUpperCase();
        String codeBody = upper.startsWith("ARENA-") ? upper.substring(6).trim() : upper;

        // Trường hợp nhập 2 cụm số: ví dụ "1-17" hoặc "ARENA-1-17" -> 192.168.1.17
        String[] dashParts = codeBody.split("[-.]");
        if (dashParts.length == 2) {
            try {
                int c = Integer.parseInt(dashParts[0].trim());
                int d = Integer.parseInt(dashParts[1].trim());
                if (c >= 0 && c <= 255 && d >= 1 && d <= 254) {
                    return "192.168." + c + "." + d;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Trường hợp nhập 4 cụm số: ví dụ "ARENA-172-20-10-2" -> 172.20.10.2
        if (dashParts.length == 4) {
            try {
                int a = Integer.parseInt(dashParts[0].trim());
                int b = Integer.parseInt(dashParts[1].trim());
                int c = Integer.parseInt(dashParts[2].trim());
                int d = Integer.parseInt(dashParts[3].trim());
                if (a >= 1 && a <= 255 && b >= 0 && b <= 255 && c >= 0 && c <= 255 && d >= 1 && d <= 254) {
                    return a + "." + b + "." + c + "." + d;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Trường hợp người dùng chỉ gõ số cuối của IP (ví dụ "17") -> Tự ghép với lớp mạng hiện tại
        if (dashParts.length == 1) {
            try {
                int lastOctet = Integer.parseInt(codeBody);
                if (lastOctet >= 1 && lastOctet <= 254) {
                    String myIp = getLocalLanIp();
                    int lastDot = myIp.lastIndexOf('.');
                    if (lastDot > 0) {
                        return myIp.substring(0, lastDot + 1) + lastOctet;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Mặc định trả về nguyên bản (nếu là IP thật 192.168.x.x hoặc tên miền Tunnel)
        return clean;
    }

    /**
     * Chuyển đổi địa chỉ IP hoặc mã nhập bất kỳ (ví dụ: 192.168.1.17, 1-17, ARENA-1-17)
     * về chuẩn Mã Phòng Đám Mây (ví dụ: ARENA-1-17) để kết nối xuyên WiFi / 4G.
     */
    public static String ipOrInputToRoomCode(String rawHostOrCode) {
        if (rawHostOrCode == null || rawHostOrCode.trim().isEmpty()) {
            return "ARENA-DEFAULT";
        }
        String clean = rawHostOrCode.trim().toUpperCase();
        if (clean.contains(":")) {
            clean = clean.split(":")[0].trim();
        }
        if (clean.startsWith("192.168.")) {
            String[] parts = clean.split("\\.");
            if (parts.length == 4) {
                return "ARENA-" + parts[2] + "-" + parts[3];
            }
        }
        if (clean.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return "ARENA-" + clean.replace('.', '-');
        }
        if (clean.startsWith("ARENA-")) {
            return clean;
        }
        return "ARENA-" + clean;
    }

    /**
     * Bắt đầu phát sóng thông tin phòng ra toàn mạng nội bộ (LAN UDP) VÀ Đám mây Internet (4G/Khác WiFi).
     * Tiếp tục phát sóng ngay cả khi Host đã vào trong trận đấu!
     */
    public void startBroadcasting(String roomCode, String hostName, int serverPort) {
        isBroadcasting = true;

        // 1. Khởi chạy Cầu Nối Đám Mây (CloudServerBridge) cho phép bạn bè khác WiFi / 4G vào thẳng phòng này!
        if (cloudServerBridge != null) {
            cloudServerBridge.stop();
        }
        cloudServerBridge = new CloudServerBridge(roomCode, serverPort);
        cloudServerBridge.start();

        new Thread(() -> {
            try (DatagramSocket broadcastSocket = new DatagramSocket()) {
                broadcastSocket.setBroadcast(true);
                String localIp = getLocalLanIp();

                // Kết nối Cloud Relay để phát danh sách phòng lên Sảnh Chờ Toàn Cầu
                cloudBroadcastClient = new MiniMqttClient(null);
                cloudBroadcastClient.connect();

                while (isBroadcasting) {
                    // Đính kèm cả localIp vào gói tin để chuẩn xác 100%
                    String message = String.format("%s%s;%s;%d;1;%s",
                            BROADCAST_HEADER, roomCode, hostName, serverPort, localIp);
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);

                    // 1. Gửi tới địa chỉ Broadcast toàn cục 255.255.255.255 (Cùng WiFi)
                    try {
                        DatagramPacket packet1 = new DatagramPacket(
                                data, data.length,
                                InetAddress.getByName("255.255.255.255"), BROADCAST_PORT
                        );
                        broadcastSocket.send(packet1);
                    } catch (Exception ignored) {
                    }

                    // 2. Gửi tới địa chỉ Subnet Broadcast (ví dụ 192.168.1.255)
                    int lastDot = localIp.lastIndexOf('.');
                    if (lastDot > 0 && !localIp.equals("127.0.0.1")) {
                        try {
                            String subnetBroadcast = localIp.substring(0, lastDot + 1) + "255";
                            DatagramPacket packet2 = new DatagramPacket(
                                    data, data.length,
                                    InetAddress.getByName(subnetBroadcast), BROADCAST_PORT
                            );
                            broadcastSocket.send(packet2);
                        } catch (Exception ignored) {
                        }
                    }

                    // 3. Phát sóng lên Cloud Internet Lobby (Cho phép máy khác WiFi / 4G thấy phòng tự động!)
                    if (cloudBroadcastClient != null && cloudBroadcastClient.isConnected()) {
                        cloudBroadcastClient.publish(CLOUD_LOBBY_TOPIC, data);
                    }

                    Thread.sleep(1500);
                }
            } catch (Exception ignored) {
            }
        }, "LanDiscovery-Broadcaster").start();
    }

    /**
     * Bắt đầu lắng nghe các phòng đang mở trong cả mạng LAN (UDP) lẫn Internet Cloud (MQTT).
     */
    public void startListening() {
        if (isListening) return;
        isListening = true;

        // 1. Luồng lắng nghe từ Cloud Internet Lobby (Khác WiFi / 4G vẫn thấy phòng!)
        new Thread(() -> {
            cloudListenClient = new MiniMqttClient((topic, payload) -> {
                if (!isListening) return;
                String text = new String(payload, StandardCharsets.UTF_8);
                parseRoomAnnouncement(text, null);
            });
            if (cloudListenClient.connect()) {
                cloudListenClient.subscribe(CLOUD_LOBBY_TOPIC);
            }
        }, "CloudDiscovery-Listener").start();

        // 2. Luồng lắng nghe UDP Broadcast nội bộ (Cùng WiFi)
        new Thread(() -> {
            try {
                listenSocket = new DatagramSocket(null);
                listenSocket.setReuseAddress(true);
                listenSocket.bind(new InetSocketAddress(BROADCAST_PORT));
                listenSocket.setBroadcast(true);
                byte[] buffer = new byte[1024];

                while (isListening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    listenSocket.receive(packet);

                    String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    parseRoomAnnouncement(text, packet.getAddress().getHostAddress());

                    long now = System.currentTimeMillis();
                    activeRooms.values().removeIf(r -> now - r.lastSeenTime > 6000);
                }
            } catch (Exception ignored) {
            } finally {
                stopListening();
            }
        }, "LanDiscovery-Listener").start();
    }

    private void parseRoomAnnouncement(String text, String fallbackSenderIp) {
        if (text != null && text.startsWith(BROADCAST_HEADER)) {
            String body = text.substring(BROADCAST_HEADER.length());
            String[] parts = body.split(";");
            if (parts.length >= 4) {
                try {
                    String code = parts[0];
                    String hostName = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    int players = Integer.parseInt(parts[3]);
                    String hostIp = (parts.length >= 5 && !parts[4].equals("127.0.0.1"))
                            ? parts[4]
                            : (fallbackSenderIp != null ? fallbackSenderIp : code);

                    activeRooms.put(code, new DiscoveredRoom(code, hostName, hostIp, port, players));
                } catch (Exception ignored) {
                }
            }
        }
    }

    public Map<String, DiscoveredRoom> getActiveRooms() {
        long now = System.currentTimeMillis();
        activeRooms.values().removeIf(r -> now - r.lastSeenTime > 6500);
        return activeRooms;
    }

    /**
     * Chỉ dừng lắng nghe khi rời Sảnh Chờ vào trận (KHÔNG tắt sóng phát phòng & Cloud Bridge của Host!).
     */
    public void stopListening() {
        isListening = false;
        if (listenSocket != null && !listenSocket.isClosed()) {
            listenSocket.close();
        }
        if (cloudListenClient != null) {
            cloudListenClient.close();
        }
    }

    /**
     * Dừng toàn bộ (cả phát sóng lẫn lắng nghe).
     */
    public void stop() {
        isBroadcasting = false;
        if (cloudBroadcastClient != null) {
            cloudBroadcastClient.close();
        }
        if (cloudServerBridge != null) {
            cloudServerBridge.stop();
        }
        stopListening();
    }
}
