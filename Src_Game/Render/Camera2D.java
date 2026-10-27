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

        // Smooth follow LERP
        this.x += (targetPos.getX() - this.x) * smoothFactor;
        this.y += (targetPos.getY() - this.y) * smoothFactor;

        // Kẹp giới hạn camera không bị tràn khỏi biên bản đồ
        double halfW = viewWidth / 2.0;
        double halfH = viewHeight / 2.0;
        this.x = Math.max(halfW, Math.min(worldWidth - halfW, this.x));
        this.y = Math.max(halfH, Math.min(worldHeight - halfH, this.y));
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
