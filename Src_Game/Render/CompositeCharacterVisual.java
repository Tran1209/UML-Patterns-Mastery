package render;

import java.util.*;

/**
 * Mẫu thiết kế Composite Pattern (GoF) quản lý tổ hợp hiển thị nhiều lớp của nhân vật LPC.
 * Chịu trách nhiệm:
 * 1. Lưu giữ và quản lý các lớp (Layers) như Body, Hair, Clothes, Weapon, Armor.
 * 2. Tự động sắp xếp các lớp theo Z-Index từ thấp đến cao trước khi xuất khung hình.
 * 3. Cho phép thay đổi trang bị động (Hot-swapping equipment) mà không làm ảnh hưởng đến các lớp khác.
 */
public class CompositeCharacterVisual {
    private final Map<LayerType, SpriteLayer> layerMap;

    public CompositeCharacterVisual() {
        this.layerMap = new EnumMap<>(LayerType.class);
    }

    public void setLayer(SpriteLayer layer) {
        if (layer != null) {
            this.layerMap.put(layer.getLayerType(), layer);
        }
    }

    public void removeLayer(LayerType layerType) {
        this.layerMap.remove(layerType);
    }

    public boolean hasLayer(LayerType layerType) {
        return this.layerMap.containsKey(layerType);
    }

    public SpriteLayer getLayer(LayerType layerType) {
        return this.layerMap.get(layerType);
    }

    /**
     * Trả về danh sách các lớp đang hiển thị, được sắp xếp tăng dần theo Z-Index.
     * Đây là thứ tự chuẩn để thuật toán Painter's Algorithm vẽ đè lớp trên lên lớp dưới.
     */
    public List<SpriteLayer> getVisibleLayersSorted() {
        List<SpriteLayer> sortedList = new ArrayList<>();
        for (SpriteLayer layer : layerMap.values()) {
            if (layer.isVisible()) {
                sortedList.add(layer);
            }
        }
        Collections.sort(sortedList);
        return Collections.unmodifiableList(sortedList);
    }

    /**
     * Tính toán tập hợp các vùng cắt hình ảnh (Sub-image Rectangles) cho toàn bộ các lớp đang kích hoạt.
     * Phục vụ quá trình render đồng bộ đa lớp (Multi-layer Blitting).
     */
    public Map<LayerType, FrameBounds> resolveRenderFrames(LpcAnimationType animType, Direction direction, int frameIndex) {
        Map<LayerType, FrameBounds> frameMap = new LinkedHashMap<>();
        for (SpriteLayer layer : getVisibleLayersSorted()) {
            FrameBounds rect = layer.calculateSubRect(animType, direction, frameIndex);
            frameMap.put(layer.getLayerType(), rect);
        }
        return frameMap;
    }
}
