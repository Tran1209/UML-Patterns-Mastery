package render;

/**
 * Cấu trúc hình học vùng cắt khung hình (Frame Bounding Box).
 * Đóng gói độc lập nền tảng (Platform-Independent), loại bỏ phụ thuộc vào java.awt.Rectangle.
 * Cho phép mã nguồn chạy xuyên suốt trên cả Java Desktop lẫn Android Mobile.
 */
public class FrameBounds {
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public FrameBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return String.format("FrameBounds[x=%d, y=%d, w=%d, h=%d]", x, y, width, height);
    }
}
