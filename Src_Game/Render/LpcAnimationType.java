package render;

/**
 * Định nghĩa các khối hành động chuẩn theo quy cách của Universal LPC Spritesheet Generator.
 * Mỗi hành động quy định hàng bắt đầu (startRow) và số lượng khung hình (frameCount) trên mỗi hướng.
 */
public enum LpcAnimationType {
    SPELLCAST(0, 7, 0.10, true),
    THRUST(4, 8, 0.08, false),
    WALK(8, 9, 0.09, true),
    SLASH(12, 6, 0.07, false),
    SHOOT(16, 13, 0.06, false),
    HURT(20, 6, 0.12, false);

    private final int startRow;
    private final int frameCount;
    private final double frameDurationSeconds;
    private final boolean isLooping;

    LpcAnimationType(int startRow, int frameCount, double frameDurationSeconds, boolean isLooping) {
        this.startRow = startRow;
        this.frameCount = frameCount;
        this.frameDurationSeconds = frameDurationSeconds;
        this.isLooping = isLooping;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public double getFrameDurationSeconds() {
        return frameDurationSeconds;
    }

    public boolean isLooping() {
        return isLooping;
    }

    /**
     * Tính toán chỉ số hàng tuyệt đối (0-indexed) trên ma trận Spritesheet 64x64.
     * Đối với HURT, chuẩn LPC chỉ có 1 hàng đơn (Row 20).
     */
    public int calculateRowIndex(Direction direction) {
        if (this == HURT) {
            return startRow;
        }
        return startRow + direction.getRowOffset();
    }
}
