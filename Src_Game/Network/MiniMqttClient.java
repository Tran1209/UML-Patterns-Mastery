package network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Trình khách giao thức MQTT v3.1.1 thuần Java (Zero External Dependency).
 * Cho phép truyền tải gói tin thời gian thực xuyên mạng (Khác WiFi / Mạng 4G / 5G Internet)
 * thông qua Cloud Message Relay mà không cần mở Port Router (Port Forwarding).
 */
public class MiniMqttClient {
    private static final String[] PUBLIC_BROKERS = {
            "broker.emqx.io",
            "broker.hivemq.com"
    };
    private static final int BROKER_PORT = 1883;

    public interface MessageListener {
        void onMessage(String topic, byte[] payload);
    }

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean connected = false;
    private final MessageListener listener;
    private int nextPacketId = 1;

    public MiniMqttClient(MessageListener listener) {
        this.listener = listener;
    }

    public boolean connect() {
        for (String host : PUBLIC_BROKERS) {
            if (tryConnectBroker(host, BROKER_PORT)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryConnectBroker(String host, int port) {
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setTrafficClass(0x10); // IPTOS_LOWDELAY tối ưu độ trễ gói tin game
            socket.connect(new InetSocketAddress(host, port), 3500);

            in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 16384));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 16384));

            String clientId = "huce_arena_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream varHeaderAndPayload = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(varHeaderAndPayload);
            // Protocol Name: "MQTT"
            dos.writeShort(4);
            dos.writeBytes("MQTT");
            // Protocol Level: 4 (v3.1.1)
            dos.writeByte(0x04);
            // Connect Flags: 0x02 (Clean Session)
            dos.writeByte(0x02);
            // Keep Alive: 45s
            dos.writeShort(45);
            // Payload: Client ID
            dos.writeShort(clientIdBytes.length);
            dos.write(clientIdBytes);
            dos.flush();

            byte[] body = varHeaderAndPayload.toByteArray();
            synchronized (out) {
                out.writeByte(0x10); // CONNECT packet type
                writeRemainingLength(out, body.length);
                out.write(body);
                out.flush();
            }

            // Đọc phản hồi CONNACK (0x20)
            int header = in.readUnsignedByte();
            int remLen = readRemainingLength(in);
            if ((header & 0xF0) != 0x20 || remLen < 2) {
                close();
                return false;
            }
            in.readUnsignedByte(); // Acknowledge Flags
            int returnCode = in.readUnsignedByte();
            for (int i = 2; i < remLen; i++) {
                in.readUnsignedByte();
            }
            if (returnCode != 0) {
                close();
                return false;
            }

            connected = true;

            // Luồng nhận gói tin từ Cloud Relay
            Thread readerThread = new Thread(this::readLoop, "CloudRelay-Reader");
            readerThread.setDaemon(true);
            readerThread.start();

            // Luồng gửi nhịp tim PINGREQ giữ kết nối bền vững
            Thread pingThread = new Thread(this::pingLoop, "CloudRelay-Ping");
            pingThread.setDaemon(true);
            pingThread.start();

            return true;
        } catch (Exception e) {
            close();
            return false;
        }
    }

    public void subscribe(String topicFilter) {
        if (!connected || out == null) return;
        try {
            byte[] topicBytes = topicFilter.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(nextPacketId++);
            dos.writeShort(topicBytes.length);
            dos.write(topicBytes);
            dos.writeByte(0x00); // QoS 0 (Tốc độ tức thời cho Real-time Multiplayer)
            dos.flush();

            byte[] body = baos.toByteArray();
            synchronized (out) {
                out.writeByte(0x82); // SUBSCRIBE
                writeRemainingLength(out, body.length);
                out.write(body);
                out.flush();
            }
        } catch (IOException e) {
            close();
        }
    }

    public void publish(String topic, byte[] payload) {
        if (!connected || out == null || payload == null) return;
        try {
            byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
            int remLen = 2 + topicBytes.length + payload.length;

            synchronized (out) {
                out.writeByte(0x30); // PUBLISH QoS 0
                writeRemainingLength(out, remLen);
                out.writeShort(topicBytes.length);
                out.write(topicBytes);
                out.write(payload);
                out.flush();
            }
        } catch (IOException e) {
            close();
        }
    }

    private void readLoop() {
        try {
            while (connected && socket != null && !socket.isClosed()) {
                int header = in.readUnsignedByte();
                int remLen = readRemainingLength(in);
                byte[] data = new byte[remLen];
                in.readFully(data);

                int packetType = header & 0xF0;
                if (packetType == 0x30) { // PUBLISH
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                    int topicLen = dis.readUnsignedShort();
                    byte[] topicBytes = new byte[topicLen];
                    dis.readFully(topicBytes);
                    String topic = new String(topicBytes, StandardCharsets.UTF_8);

                    int headerOffset = 2 + topicLen;
                    int qos = (header & 0x06) >> 1;
                    if (qos > 0) {
                        dis.readUnsignedShort(); // Packet ID
                        headerOffset += 2;
                    }

                    int payloadLen = remLen - headerOffset;
                    if (payloadLen >= 0) {
                        byte[] payload = new byte[payloadLen];
                        System.arraycopy(data, headerOffset, payload, 0, payloadLen);
                        if (listener != null) {
                            listener.onMessage(topic, payload);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            close();
        }
    }

    private void pingLoop() {
        try {
            while (connected && socket != null && !socket.isClosed()) {
                Thread.sleep(20000);
                if (!connected) break;
                synchronized (out) {
                    out.writeByte(0xC0); // PINGREQ
                    out.writeByte(0x00);
                    out.flush();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void writeRemainingLength(DataOutputStream out, int length) throws IOException {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0) {
                digit |= 0x80;
            }
            out.writeByte(digit);
        } while (length > 0);
    }

    private static int readRemainingLength(DataInputStream in) throws IOException {
        int multiplier = 1;
        int value = 0;
        int encodedByte;
        do {
            encodedByte = in.readUnsignedByte();
            value += (encodedByte & 127) * multiplier;
            multiplier *= 128;
            if (multiplier > 128 * 128 * 128) {
                throw new IOException("Malformed Remaining Length");
            }
        } while ((encodedByte & 128) != 0);
        return value;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
