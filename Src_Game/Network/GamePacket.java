package network;

import java.io.Serializable;

/**
 * Đối tượng truyền dữ liệu mạng (Data Transfer Object - DTO).
 * Tuân thủ nguyên lý đóng gói dữ liệu truyền tải giữa Client và Server.
 */
public class GamePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    private final PacketType type;
    private final String senderId;
    private final long timestamp;
    private final Object payload;

    public GamePacket(PacketType type, String senderId, Object payload) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
    }

    public PacketType getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Object getPayload() {
        return payload;
    }
}
