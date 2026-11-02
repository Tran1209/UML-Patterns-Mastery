package render;

import java.awt.Graphics2D;

/**
 * Giao diện trừu tượng cho mọi thực thể và đối tượng tham gia hàng đợi phối cảnh chiều sâu 2.5D (Z-Ordering / Depth Sorting).
 * Áp dụng nguyên lý Abstraction & Polymorphism: Cho phép nhân vật, quái vật, trụ đá, cây cối,
 * và các vật thể môi trường được sắp xếp và kết xuất thống nhất theo thuật toán Painter's Algorithm.
 */
public interface Renderable25D {
    /**
     * Tọa độ Y chiều sâu trong không gian thế giới (Ground Anchor Depth).
     * @return Tọa độ Y dùng để sắp xếp thứ tự vẽ từ xa tới gần.
     */
    double getDepthY();

    /**
     * Lệnh kết xuất đồ họa lên màn hình (Command / Visitor pattern).
     * @param g2d Đối tượng đồ họa 2D.
     */
    void render(Graphics2D g2d);
}
