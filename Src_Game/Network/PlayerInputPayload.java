package network;

import java.io.Serializable;

/**
 * Gói tin điều khiển từ Client gửi lên Authoritative Server.
 * Client KHÔNG gửi vị trí X, Y (chống gian lận / Speed Hack).
 * Client chỉ gửi vector hướng di chuyển (vx, vy), cờ đánh thường (isAttacking) và kỹ năng (skillId: 1, 2, 3).
 */
public class PlayerInputPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    public final double moveVx;
    public final double moveVy;
    public final boolean isAttacking;
    public final int skillId; // 0: Không tung chiêu, 1: Xung Kích, 2: Vòng Xoáy Lửa, 3: Thiên Phạt (Ultimate)
    public final double skillAimX; // Vector tâm ngắm kéo thả (-1.0 -> 1.0)
    public final double skillAimY; // Vector tâm ngắm kéo thả (-1.0 -> 1.0)

    public PlayerInputPayload(double moveVx, double moveVy, boolean isAttacking, int skillId, double skillAimX, double skillAimY) {
        this.moveVx = moveVx;
        this.moveVy = moveVy;
        this.isAttacking = isAttacking;
        this.skillId = skillId;
        this.skillAimX = skillAimX;
        this.skillAimY = skillAimY;
    }

    public PlayerInputPayload(double moveVx, double moveVy, boolean isAttacking, int skillId) {
        this(moveVx, moveVy, isAttacking, skillId, 0.0, 0.0);
    }

    public PlayerInputPayload(double moveVx, double moveVy, boolean isAttacking) {
        this(moveVx, moveVy, isAttacking, 0, 0.0, 0.0);
    }
}
