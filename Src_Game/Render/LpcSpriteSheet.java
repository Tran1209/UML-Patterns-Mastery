package render;

/**
 * Đại diện cho một Spritesheet chuẩn Universal LPC theo mẫu thiết kế Flyweight (GoF / Robert Nystrom).
 * Đóng gói trạng thái nội tại (Intrinsic State):
 * - Kích thước ô chuẩn (64x64 pixel)
 * - Đường dẫn tài nguyên (resourcePath / sheetId)
 * - Thuật toán tính toán khung cắt hình học (Sub-rectangle Clipping) độc lập nền tảng
 * 
 * Lớp này được chia sẻ chung giữa hàng trăm thực thể mà không nhân bản dữ liệu bộ nhớ,
 * tương thích 100% với cả Java Desktop lẫn Android ART/Dalvik.
 */
public class LpcSpriteSheet {
    public static final int TILE_SIZE = 64;

    private final String sheetId;
    private final String assetPath;
    private final int totalColumns;
    private final int totalRows;

    public LpcSpriteSheet(String sheetId, String assetPath, int totalColumns, int totalRows) {
        this.sheetId = sheetId;
        this.assetPath = assetPath;
        this.totalColumns = totalColumns;
        this.totalRows = totalRows;
    }

    public LpcSpriteSheet(String sheetId, String assetPath) {
        this(sheetId, assetPath, 13, 21); // Chuẩn LPC phổ thông: 13 cột tối đa (hành động SHOOT), 21 hàng
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public int getTotalColumns() {
        return totalColumns;
    }

    public int getTotalRows() {
        return totalRows;
    }

    /**
     * Tính toán hình chữ nhật cắt (Sub-image Bounding Box) trên Spritesheet dựa trên hành động, hướng và số frame.
     * Áp dụng nguyên lý đóng gói (Encapsulation) toán học ma trận pixel LPC.
     */
    public FrameBounds getFrameBounds(LpcAnimationType animType, Direction direction, int frameIndex) {
        int row = animType.calculateRowIndex(direction);
        int col = Math.floorMod(frameIndex, animType.getFrameCount());

        int x = col * TILE_SIZE;
        int y = row * TILE_SIZE;

        return new FrameBounds(x, y, TILE_SIZE, TILE_SIZE);
    }
}
