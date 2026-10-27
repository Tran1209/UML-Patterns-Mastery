package render;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Đại diện cho một Spritesheet chuẩn Universal LPC theo mẫu thiết kế Flyweight (GoF / Robert Nystrom).
 * Đóng gói trạng thái nội tại (Intrinsic State):
 * - Kích thước ô chuẩn (64x64 pixel)
 * - Đường dẫn tài nguyên (resourcePath)
 * - Thuật toán tính toán khung cắt hình học (Sub-rectangle Clipping)
 * - Quản lý Texture Cache dùng chung trong bộ nhớ RAM
 * 
 * Lớp này được chia sẻ chung giữa hàng trăm thực thể mà không nhân bản dữ liệu bộ nhớ.
 */
public class LpcSpriteSheet {
    public static final int TILE_SIZE = 64;

    private static final Map<String, BufferedImage> TEXTURE_CACHE = new HashMap<>();

    private final String sheetId;
    private final String assetPath;
    private final int totalColumns;
    private final int totalRows;
    private BufferedImage cachedImage;
    private boolean loadAttempted = false;

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
     * Nạp ảnh Texture từ đĩa cứng theo cơ chế Flyweight Cache dùng chung.
     */
    public synchronized BufferedImage getTextureImage() {
        if (!loadAttempted) {
            loadAttempted = true;
            if (assetPath != null && !assetPath.isEmpty()) {
                if (TEXTURE_CACHE.containsKey(assetPath)) {
                    cachedImage = TEXTURE_CACHE.get(assetPath);
                } else {
                    File file = new File(assetPath);
                    if (file.exists() && file.isFile()) {
                        try {
                            cachedImage = ImageIO.read(file);
                            TEXTURE_CACHE.put(assetPath, cachedImage);
                        } catch (IOException e) {
                            cachedImage = null;
                        }
                    }
                }
            }
        }
        return cachedImage;
    }

    public boolean hasLoadedImage() {
        return getTextureImage() != null;
    }

    /**
     * Tính toán hình chữ nhật cắt (Sub-image Bounding Box) trên Spritesheet dựa trên hành động, hướng và số frame.
     * Áp dụng nguyên lý đóng gói (Encapsulation) toán học ma trận pixel.
     */
    public Rectangle getFrameBounds(LpcAnimationType animType, Direction direction, int frameIndex) {
        int row = animType.calculateRowIndex(direction);
        int col = Math.floorMod(frameIndex, animType.getFrameCount());

        int x = col * TILE_SIZE;
        int y = row * TILE_SIZE;

        return new Rectangle(x, y, TILE_SIZE, TILE_SIZE);
    }
}
