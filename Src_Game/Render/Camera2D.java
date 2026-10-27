package render;

import core.Vector2D;

/**
 * Camera 2D theo dõi nhân vật mượt mà với thuật toán nội suy làm mịn (Smooth LERP Follow).
 * Đảm bảo góc nhìn của người chơi luôn tập trung vào chiến trường.
 */
public class Camera2D {
    private double x;
    private double y;
    private final int viewWidth;
    private final int viewHeight;
    private final double worldWidth;
    private final double worldHeight;

    public Camera2D(int viewWidth, int viewHeight, double worldWidth, double worldHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.x = worldWidth / 2.0;
        this.y = worldHeight / 2.0;
    }

    public void update(Vector2D targetPos, double smoothFactor) {
        if (targetPos == null) return;

        // Smooth follow LERP giữ nhân vật luôn ở trung tâm màn hình chuẩn MOBA / Liên Quân
        // Giúp ngón tay trái (Joystick) và ngón tay phải (3 nút Kỹ năng) không bao giờ che khuất nhân vật!
        this.x += (targetPos.getX() - this.x) * smoothFactor;
        this.y += (targetPos.getY() - this.y) * smoothFactor;

        this.x = Math.max(0.0, Math.min(worldWidth, this.x));
        this.y = Math.max(0.0, Math.min(worldHeight, this.y));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double worldToScreenX(double worldX) {
        return worldX - (x - viewWidth / 2.0);
    }

    public double worldToScreenY(double worldY) {
        return worldY - (y - viewHeight / 2.0);
    }
}
