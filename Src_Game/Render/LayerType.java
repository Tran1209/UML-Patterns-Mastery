package render;

/**
 * Phân loại các lớp hiển thị (Layer Z-Order) chuẩn theo quy cách Universal LPC.
 * Đảm bảo các thành phần đồ họa xếp chồng đúng thứ tự hiển thị:
 * Thân thể -> Mắt -> Quần áo lót -> Áo giáp -> Tóc -> Mũ -> Vũ khí -> Khiên.
 */
public enum LayerType {
    BEHIND_BODY(10),
    BODY(20),
    EYES(30),
    PANTS(40),
    SHIRT(50),
    ARMOR(60),
    HAIR(70),
    HELMET(80),
    WEAPON(90),
    SHIELD(100);

    private final int defaultZIndex;

    LayerType(int defaultZIndex) {
        this.defaultZIndex = defaultZIndex;
    }

    public int getDefaultZIndex() {
        return defaultZIndex;
    }
}
