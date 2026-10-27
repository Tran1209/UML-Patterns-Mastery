package network;

import core.Vector2D;
import java.util.*;

/**
 * Bộ đệm và giải thuật nội suy tuyến tính (Snapshot Interpolation - LERP).
 * Tham chiếu: Joshua Glazer & Sanjay Madhav, Multiplayer Game Programming, Ch. 7.
 * Giải quyết triệt để lỗi giật hình (Micro-stutter / Jitter) khi Server gửi 20Hz nhưng Client vẽ 60 FPS.
 * Đảm bảo Thread-Safe tuyệt đối giữa luồng nhận mạng (Network Thread) và luồng dựng hình (Render Thread).
 */
public class SnapshotInterpolator {
    public static class EntityTransform {
        public final Vector2D position;
        public final long timestamp;

        public EntityTransform(Vector2D position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }

    private final Map<String, Deque<EntityTransform>> historyBuffers = new HashMap<>();
    private final long interpolationDelayMs; // Độ trễ bộ đệm nội suy (thường là 100ms = 2 ticks của 20Hz)

    public SnapshotInterpolator(long interpolationDelayMs) {
        this.interpolationDelayMs = interpolationDelayMs;
    }

    public synchronized void pushTransform(String entityId, Vector2D position, long timestamp) {
        Deque<EntityTransform> buffer = historyBuffers.computeIfAbsent(entityId, k -> new ArrayDeque<>());
        buffer.addLast(new EntityTransform(position, timestamp));

        // Dọn dẹp các mẫu cũ để tránh rò rỉ bộ nhớ
        while (buffer.size() > 20) {
            buffer.removeFirst();
        }
    }

    /**
     * Nội suy vị trí thực thể tại thời điểm render hiện tại.
     * Sử dụng công thức: P_render = P_old + (P_new - P_old) * alpha
     */
    public synchronized Vector2D interpolate(String entityId, long currentClientTime) {
        Deque<EntityTransform> buffer = historyBuffers.get(entityId);
        if (buffer == null || buffer.isEmpty()) {
            return null;
        }

        long renderTimestamp = currentClientTime - interpolationDelayMs;

        // Nếu chỉ có 1 mẫu hoặc mẫu mới nhất vẫn cũ hơn thời điểm render
        if (buffer.size() == 1 || buffer.peekLast().timestamp <= renderTimestamp) {
            return buffer.peekLast().position;
        }

        // Nếu mẫu cũ nhất vẫn mới hơn thời điểm render
        if (buffer.peekFirst().timestamp >= renderTimestamp) {
            return buffer.peekFirst().position;
        }

        // Tìm 2 ảnh chụp (P_old và P_new) kẹp giữa renderTimestamp
        EntityTransform pOld = null;
        EntityTransform pNew = null;

        for (EntityTransform t : buffer) {
            if (t.timestamp <= renderTimestamp) {
                pOld = t;
            } else {
                pNew = t;
                break;
            }
        }

        if (pOld == null || pNew == null) {
            return buffer.peekLast().position;
        }

        // Tính toán hệ số alpha trong đoạn [0.0, 1.0]
        double duration = pNew.timestamp - pOld.timestamp;
        if (duration <= 0) {
            return pNew.position;
        }
        double progress = renderTimestamp - pOld.timestamp;
        float alpha = (float) Math.max(0.0, Math.min(1.0, progress / duration));

        // Áp dụng công thức LERP cho Vector 2D
        return lerp(pOld.position, pNew.position, alpha);
    }

    private Vector2D lerp(Vector2D start, Vector2D end, float alpha) {
        double newX = start.getX() + (end.getX() - start.getX()) * alpha;
        double newY = start.getY() + (end.getY() - start.getY()) * alpha;
        return new Vector2D(newX, newY);
    }
}
