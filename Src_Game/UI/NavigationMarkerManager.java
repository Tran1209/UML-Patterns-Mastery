package ui;

import render.Camera2D;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý và kết xuất các hiệu ứng chỉ thị click chuột trên mặt đất (Navigation Ping & Ripple System).
 * Áp dụng nguyên lý SRP (Single Responsibility Principle):
 * Tách biệt hoàn toàn việc lưu trữ, cập nhật hoạt ảnh và vẽ hiệu ứng click khỏi GameCanvas.
 */
public class NavigationMarkerManager {

    public enum MarkerType {
        MOVE,
        ATTACK
    }

    public static class ClickMarker {
        private final double worldX;
        private final double worldY;
        private final MarkerType type;
        private final double maxLife = 0.35;
        private double life = 0.35;

        public ClickMarker(double worldX, double worldY, MarkerType type) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.type = type;
        }

        public boolean update(double dt) {
            life -= dt;
            return life <= 0;
        }

        public void render(Graphics2D g2d, Camera2D camera) {
            double sx = camera.worldToScreenX(worldX);
            double sy = camera.worldToScreenY(worldY);
            float progress = (float) (life / maxLife); // 1.0 -> 0.0
            int alpha = (int) Math.max(0, Math.min(255, progress * 240));

            double ratio = 0.58; // Tỷ lệ nén mặt đất 2.5D
            int radius = (int) (14 + (1.0f - progress) * 8);
            int radiusY = (int) (radius * ratio);

            if (type == MarkerType.ATTACK) {
                // Hiệu ứng click tấn công quái vật: Màu đỏ neon (Target Ping LOL)
                g2d.setColor(new Color(255, 50, 50, alpha));
                g2d.setStroke(new BasicStroke(2.2f));
                g2d.drawOval((int) sx - radius, (int) sy - radiusY, radius * 2, radiusY * 2);

                int tick = 6;
                g2d.drawLine((int) sx - radius - tick, (int) sy, (int) sx - radius + 2, (int) sy);
                g2d.drawLine((int) sx + radius - 2, (int) sy, (int) sx + radius + tick, (int) sy);
                g2d.drawLine((int) sx, (int) (sy - radiusY - tick * ratio), (int) sx, (int) (sy - radiusY + 2));
                g2d.drawLine((int) sx, (int) (sy + radiusY - 2), (int) sx, (int) (sy + radiusY + tick * ratio));
            } else {
                // Hiệu ứng click di chuyển: Màu xanh lục/ngọc (Move Ping LOL)
                g2d.setColor(new Color(46, 204, 113, alpha));
                g2d.setStroke(new BasicStroke(2.2f));
                g2d.drawOval((int) sx - radius, (int) sy - radiusY, radius * 2, radiusY * 2);

                int innerR = 4;
                int innerRy = (int) (innerR * ratio);
                g2d.setColor(new Color(150, 255, 180, alpha));
                g2d.fillOval((int) sx - innerR, (int) sy - innerRy, innerR * 2, innerRy * 2);

                int waveR = radius + 6;
                int waveRy = (int) (waveR * ratio);
                int waveAlpha = (int) (alpha * 0.45);
                g2d.setColor(new Color(46, 204, 113, waveAlpha));
                g2d.drawOval((int) sx - waveR, (int) sy - waveRy, waveR * 2, waveRy * 2);
            }
        }
    }

    private final List<ClickMarker> markers = new CopyOnWriteArrayList<>();

    public void addMoveMarker(double worldX, double worldY) {
        if (markers.size() < 15) {
            markers.add(new ClickMarker(worldX, worldY, MarkerType.MOVE));
        }
    }

    public void addAttackMarker(double worldX, double worldY) {
        if (markers.size() < 15) {
            markers.add(new ClickMarker(worldX, worldY, MarkerType.ATTACK));
        }
    }

    public void update(double dt) {
        markers.removeIf(m -> m.update(dt));
    }

    public void render(Graphics2D g2d, Camera2D camera) {
        if (markers.isEmpty()) return;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (ClickMarker marker : markers) {
            marker.render(g2d, camera);
        }
        g2d.setStroke(new BasicStroke(1f));
    }

    public void clear() {
        markers.clear();
    }
}
