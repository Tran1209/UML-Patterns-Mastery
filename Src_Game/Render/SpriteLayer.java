package render;

import java.awt.Rectangle;

/**
 * Một thành phần hiển thị (Component) trong mẫu thiết kế Composite Pattern.
 * Đóng gói một lớp trang phục/vũ khí/cơ thể cụ thể và liên kết tới đối tượng Flyweight LpcSpriteSheet.
 */
public class SpriteLayer implements Comparable<SpriteLayer> {
    private final LayerType layerType;
    private final int zIndex;
    private final LpcSpriteSheet spriteSheet;
    private boolean isVisible;

    public SpriteLayer(LayerType layerType, LpcSpriteSheet spriteSheet, int zIndex) {
        this.layerType = layerType;
        this.spriteSheet = spriteSheet;
        this.zIndex = zIndex;
        this.isVisible = true;
    }

    public SpriteLayer(LayerType layerType, LpcSpriteSheet spriteSheet) {
        this(layerType, spriteSheet, layerType.getDefaultZIndex());
    }

    public LayerType getLayerType() {
        return layerType;
    }

    public int getZIndex() {
        return zIndex;
    }

    public LpcSpriteSheet getSpriteSheet() {
        return spriteSheet;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public Rectangle calculateSubRect(LpcAnimationType animType, Direction direction, int frameIndex) {
        return spriteSheet.getFrameBounds(animType, direction, frameIndex);
    }

    @Override
    public int compareTo(SpriteLayer other) {
        return Integer.compare(this.zIndex, other.zIndex);
    }
}
