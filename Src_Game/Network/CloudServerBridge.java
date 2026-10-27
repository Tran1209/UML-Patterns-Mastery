package network;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cầu nối Đám Mây (Cloud Internet Relay Bridge) dành cho Máy Chủ (Host).
 * Tự động ánh xạ các gói tin từ người chơi ở mạng khác (Khác WiFi / 4G / 5G Internet)
 * thông qua MQTT Cloud Broker về thẳng DedicatedGameServer cục bộ (127.0.0.1:port).
 */
public class CloudServerBridge {
    private final String roomCode;
    private final int localServerPort;
    private MiniMqttClient mqttClient;
    private volatile boolean running = false;
    private final Map<String, LocalLoopbackSession> sessions = new ConcurrentHashMap<>();

    public CloudServerBridge(String roomCode, int localServerPort) {
        this.roomCode = roomCode.trim().toUpperCase();
        this.localServerPort = localServerPort;
    }

    public void start() {
        if (running) return;
        running = true;
        new Thread(() -> {
            mqttClient = new MiniMqttClient(this::onCloudMessage);
            if (mqttClient.connect()) {
                String c2sTopic = getC2STopicPrefix() + "+";
                mqttClient.subscribe(c2sTopic);
                System.out.println("☁️ [Cloud Relay] Đã mở kênh xuyên mạng Internet (4G/Khác WiFi) cho mã phòng: " + roomCode);
            }
        }, "CloudServerBridge-Init").start();
    }

    private String getC2STopicPrefix() {
        return "huce_oop_2026/room/" + roomCode + "/c2s/";
    }

    private String getS2CTopic(String sessionId) {
        return "huce_oop_2026/room/" + roomCode + "/s2c/" + sessionId;
    }

    private void onCloudMessage(String topic, byte[] payload) {
        if (!running) return;
        String prefix = getC2STopicPrefix();
        if (!topic.startsWith(prefix)) return;

        String sessionId = topic.substring(prefix.length()).trim();
        if (sessionId.isEmpty()) return;

        try {
            GamePacket packet = deserialize(payload);
            LocalLoopbackSession session = sessions.computeIfAbsent(sessionId, id -> {
                LocalLoopbackSession s = new LocalLoopbackSession(id);
                s.connectLocal();
                return s;
            });
            session.forwardToServer(packet);
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        running = false;
        for (LocalLoopbackSession s : sessions.values()) {
            s.close();
        }
        sessions.clear();
        if (mqttClient != null) {
            mqttClient.close();
        }
    }

    private class LocalLoopbackSession {
        private final String sessionId;
        private Socket localSocket;
        private ObjectOutputStream localOut;
        private ObjectInputStream localIn;
        private volatile boolean active = false;

        LocalLoopbackSession(String sessionId) {
            this.sessionId = sessionId;
        }

        void connectLocal() {
            try {
                localSocket = new Socket("127.0.0.1", localServerPort);
                localSocket.setTcpNoDelay(true);
                localOut = new ObjectOutputStream(new BufferedOutputStream(localSocket.getOutputStream(), 8192));
                localOut.flush();
                localIn = new ObjectInputStream(new BufferedInputStream(localSocket.getInputStream(), 8192));
                active = true;

                Thread listener = new Thread(this::listenFromLocalServer, "CloudBridge-Session-" + sessionId);
                listener.setDaemon(true);
                listener.start();
            } catch (Exception e) {
                close();
            }
        }

        synchronized void forwardToServer(GamePacket packet) {
            if (!active || localOut == null) return;
            try {
                localOut.writeObject(packet);
                localOut.reset();
                localOut.flush();
            } catch (IOException e) {
                close();
            }
        }

        private void listenFromLocalServer() {
            long lastSnapshotSentTime = 0;
            try {
                while (active && running && localSocket != null && !localSocket.isClosed()) {
                    GamePacket response = (GamePacket) localIn.readObject();
                    if (response == null) break;

                    if (response.getType() == PacketType.WORLD_SNAPSHOT) {
                        long now = System.currentTimeMillis();
                        if (now - lastSnapshotSentTime < 45) {
                            continue; // Điều tiết 22Hz qua Cloud Internet giúp đường truyền 4G mượt và không nghẽn
                        }
                        lastSnapshotSentTime = now;
                    }

                    byte[] bytes = serialize(response);
                    if (mqttClient != null && mqttClient.isConnected()) {
                        mqttClient.publish(getS2CTopic(sessionId), bytes);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                close();
                sessions.remove(sessionId);
            }
        }

        void close() {
            active = false;
            try {
                if (localSocket != null && !localSocket.isClosed()) {
                    localSocket.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static byte[] serialize(GamePacket packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(packet);
            oos.flush();
        }
        return baos.toByteArray();
    }

    public static GamePacket deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (GamePacket) ois.readObject();
        }
    }
}
