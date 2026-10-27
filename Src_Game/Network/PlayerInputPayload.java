package network;

import core.Vector2D;
import java.io.Serializable;

/**
 * Gói tin điều khiển từ Client gửi lên Authoritative Server.
 * Client KHÔNG gửi vị trí X, Y (chống gian lận / Speed Hack).
 * Client chỉ gửi vector hướng di chuyển (vx, vy) và cờ tấn công (isAttacking).
 */
public class PlayerInputPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    public final double moveVx;
    public final double moveVy;
    public final boolean isAttacking;

    public PlayerInputPayload(double moveVx, double moveVy, boolean isAttacking) {
        this.moveVx = moveVx;
        this.moveVy = moveVy;
        this.isAttacking = isAttacking;
    }
}
