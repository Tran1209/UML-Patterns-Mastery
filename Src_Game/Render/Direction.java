package render;

/**
 * Hướng nhìn của nhân vật trong không gian 2D.
 * Ánh xạ trực tiếp với chỉ số hàng (Row Offset) trong cấu trúc chuẩn 4 hướng của Universal LPC.
 * Quy ước hàng chuẩn của LPC cho mỗi khối hoạt họa:
 * Hàng 0: UP (Bắc / Hướng lên)
 * Hàng 1: LEFT (Tây / Hướng sang trái)
 * Hàng 2: DOWN (Nam / Hướng xuống dưới)
 * Hàng 3: RIGHT (Đông / Hướng sang phải)
 */
public enum Direction {
    UP(0, 0, -1),
    LEFT(1, -1, 0),
    DOWN(2, 0, 1),
    RIGHT(3, 1, 0);

    private final int rowOffset;
    private final int dirX;
    private final int dirY;

    Direction(int rowOffset, int dirX, int dirY) {
        this.rowOffset = rowOffset;
        this.dirX = dirX;
        this.dirY = dirY;
    }

    public int getRowOffset() {
        return rowOffset;
    }

    public int getDirX() {
        return dirX;
    }

    public int getDirY() {
        return dirY;
    }

    /**
     * Xác định hướng nhìn dựa trên vector vận tốc (vx, vy).
     * @param vx Vận tốc trục X
     * @param vy Vận tốc trục Y
     * @param fallback Hướng mặc định nếu vận tốc xấp xỉ 0
     * @return Direction tương ứng
     */
    public static Direction fromVelocity(double vx, double vy, Direction fallback) {
        if (Math.abs(vx) < 1e-3 && Math.abs(vy) < 1e-3) {
            return fallback;
        }
        if (Math.abs(vx) > Math.abs(vy)) {
            return vx > 0 ? RIGHT : LEFT;
        } else {
            return vy > 0 ? DOWN : UP;
        }
    }
}
