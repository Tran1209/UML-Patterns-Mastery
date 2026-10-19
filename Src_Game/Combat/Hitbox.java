package combat;

import core.Vector2D;

/**
 * Khối hộp chữ nhật bao quanh thực thể (Axis-Aligned Bounding Box - AABB).
 * Áp dụng nguyên lý Information Expert: Hitbox tự chịu trách nhiệm kiểm tra giao cắt.
 */
public class Hitbox {
    private double centerX;
    private double centerY;
    private final double halfWidth;
    private final double halfHeight;

    public Hitbox(double centerX, double centerY, double width, double height) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.halfWidth = width / 2.0;
        this.halfHeight = height / 2.0;
    }

    public void updateCenter(Vector2D position) {
        this.centerX = position.getX();
        this.centerY = position.getY();
    }

    public double getMinX() { return centerX - halfWidth; }
    public double getMaxX() { return centerX + halfWidth; }
    public double getMinY() { return centerY - halfHeight; }
    public double getMaxY() { return centerY + halfHeight; }

    /**
     * Thuật toán kiểm tra giao cắt giữa hai hộp bao AABB.
     */
    public boolean intersects(Hitbox other) {
        if (this.getMaxX() < other.getMinX() || this.getMinX() > other.getMaxX()) {
            return false;
        }
        if (this.getMaxY() < other.getMinY() || this.getMinY() > other.getMaxY()) {
            return false;
        }
        return true;
    }
}
