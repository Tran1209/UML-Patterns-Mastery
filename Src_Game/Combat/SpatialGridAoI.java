package combat;

import core.LivingEntity;
import core.Vector2D;
import java.util.*;

/**
 * Lưới phân vùng không gian (Spatial Partition Pattern - Robert Nystrom Ch. 19).
 * Chia bản đồ thành các ô lưới kích thước cố định để giảm độ phức tạp va chạm từ O(N^2) xuống O(N).
 */
public class SpatialGridAoI {
    private final int cellSize;
    private final Map<Long, List<LivingEntity>> gridBuckets;

    public SpatialGridAoI(int cellSize) {
        this.cellSize = cellSize;
        this.gridBuckets = new HashMap<>();
    }

    private long hashCoordinate(int cellX, int cellY) {
        return (((long) cellX) << 32) | (cellY & 0xFFFFFFFFL);
    }

    public void clear() {
        gridBuckets.clear();
    }

    public void insert(LivingEntity entity) {
        Vector2D pos = entity.getPosition();
        int cellX = (int) Math.floor(pos.getX() / cellSize);
        int cellY = (int) Math.floor(pos.getY() / cellSize);
        long key = hashCoordinate(cellX, cellY);

        gridBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
    }

    /**
     * Truy vấn các thực thể nằm trong ô hiện tại và 8 ô lân cận xung quanh.
     */
    public List<LivingEntity> queryNearby(Vector2D centerPosition) {
        List<LivingEntity> nearby = new ArrayList<>();
        int centerCellX = (int) Math.floor(centerPosition.getX() / cellSize);
        int centerCellY = (int) Math.floor(centerPosition.getY() / cellSize);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                long key = hashCoordinate(centerCellX + dx, centerCellY + dy);
                List<LivingEntity> bucket = gridBuckets.get(key);
                if (bucket != null) {
                    nearby.addAll(bucket);
                }
            }
        }
        return nearby;
    }
}
