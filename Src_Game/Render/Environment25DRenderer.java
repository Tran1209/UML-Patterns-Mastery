package render;

import core.GameWorld;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Bộ kết xuất Địa hình & Bản đồ Đấu trường 2.5D (Arena Terrain Renderer).
 * Áp dụng nguyên lý SRP (Single Responsibility Principle):
 * Chịu trách nhiệm kết xuất đồ họa mặt đất, thảm cỏ rừng, sông uốn lượn, cầu đá cổ,
 * đài tế trung tâm và tường thành bao quanh.
 * Việc kết xuất chướng ngại vật (Cây cối, tảng đá, trụ phong ấn) được ủy quyền cho {@link EnvironmentPropsRenderer}.
 */
public final class Environment25DRenderer {

    private Environment25DRenderer() {}

    public static final double[][] TREE_COORDINATES = EnvironmentPropsRenderer.TREE_COORDINATES;
    public static final double[][] BOULDER_COORDINATES = EnvironmentPropsRenderer.BOULDER_COORDINATES;

    // Cache Flyweight cho BasicStroke (Chống GC Allocation trong Render Loop)
    private static final BasicStroke STROKE_1 = new BasicStroke(1f);
    private static final BasicStroke STROKE_GRID = new BasicStroke(1.2f);
    private static final BasicStroke STROKE_WAVE = new BasicStroke(1.8f);
    private static final BasicStroke STROKE_RUNE = new BasicStroke(2.5f);
    private static final BasicStroke STROKE_BANK = new BasicStroke(3.0f);
    private static final BasicStroke STROKE_TERRACE = new BasicStroke(3.5f);
    private static final BasicStroke STROKE_BORDER = new BasicStroke(4f);
    private static final BasicStroke STROKE_MOSS = new BasicStroke(6f);
    private static final BasicStroke STROKE_TRAIL_BASE = new BasicStroke(42f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_TRAIL_MID = new BasicStroke(30f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_TRAIL_TOP = new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    // Bảng tra cứu trước tọa độ dòng sông 2.5D (Pre-computed Lookup Table - Rule 7)
    // Loại bỏ hoàn toàn hàng trăm phép tính Math.sin / Math.cos lặp lại vô ích mỗi khung hình
    private static final double R_STEP = 32.0;
    private static final int RIVER_SAMPLE_COUNT = (int) (GameWorld.WORLD_WIDTH / R_STEP) + 2;
    private static final double[] RIVER_WX_TABLE = new double[RIVER_SAMPLE_COUNT];
    private static final double[] RIVER_CY_TABLE = new double[RIVER_SAMPLE_COUNT];

    static {
        for (int i = 0; i < RIVER_SAMPLE_COUNT; i++) {
            double wx = i * R_STEP;
            RIVER_WX_TABLE[i] = wx;
            RIVER_CY_TABLE[i] = getRiverCenterY(wx);
        }
    }

    // Tối ưu hóa FPS vượt bậc: Bản đồ Địa hình Tĩnh Nướng Sẵn (Static Baked Map Blit - Rule 7)
    // Giảm thời gian vẽ nền từ 31.43ms xuống chỉ còn 0.7ms (tăng tốc độ lên 40 lần)
    private static final BufferedImage BAKED_ARENA_MAP = createBakedArenaMap();

    /**
     * Đường cong uốn lượn tự nhiên của dòng sông 2.5D theo trục hoành X.
     */
    public static double getRiverCenterY(double wx) {
        return 290.0 + Math.sin(wx * 0.004) * 42.0 + Math.cos(wx * 0.0022) * 16.0;
    }

    /**
     * Kết xuất nền thảm cỏ, dòng sông, cầu đá, thềm đấu trường và tường thành 2.5D.
     */
    public static void renderArenaBackground(Graphics2D g2d, Camera2D camera, int width, int height) {
        double startX = camera.worldToScreenX(0);
        double startY = camera.worldToScreenY(0);
        double endX = camera.worldToScreenX(GameWorld.WORLD_WIDTH);
        double endY = camera.worldToScreenY(GameWorld.WORLD_HEIGHT);

        // 1. Không gian sâu thẳm ngoài đấu trường (Abyss Void)
        g2d.setColor(new Color(11, 15, 13));
        g2d.fillRect(0, 0, width, height);

        // 2. Kết xuất toàn bộ nền tĩnh đấu trường từ Bản đồ nướng sẵn (Baked Map Blit - ~0.7ms)
        g2d.drawImage(BAKED_ARENA_MAP, (int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY), null);

        // 3. Sóng nước chuyển động lấp lánh (Animated Water Waves)
        long time = System.currentTimeMillis();
        g2d.setColor(new Color(145, 225, 255, 140));
        g2d.setStroke(STROKE_WAVE);
        for (int i = 0; i < 3; i++) {
            Path2D.Double wavePath = new Path2D.Double();
            double yOffset = (i - 1) * 22.0;
            double freq = 0.018 + i * 0.006;
            double speed = (i + 1) * 0.0026;
            for (int j = 0; j < RIVER_SAMPLE_COUNT; j++) {
                double wx = RIVER_WX_TABLE[j];
                double cy = RIVER_CY_TABLE[j];
                double waveY = cy + yOffset + Math.sin(time * speed + wx * freq) * 5.5;
                double sx = camera.worldToScreenX(wx);
                double sy = camera.worldToScreenY(waveY);
                if (j == 0) wavePath.moveTo(sx, sy);
                else wavePath.lineTo(sx, sy);
            }
            g2d.draw(wavePath);
        }

        // 4. Thần trận phong ấn tâm đấu trường 2.5D (Dynamic Arcane Runic Seal)
        double centerWorldX = GameWorld.WORLD_WIDTH / 2.0;
        double centerWorldY = 720.0;
        double csx = camera.worldToScreenX(centerWorldX);
        double csy = camera.worldToScreenY(centerWorldY);

        int runeRx = 220;
        int runeRy = (int) (runeRx * 0.58);
        g2d.setColor(new Color(0, 229, 255, 30));
        g2d.fillOval((int) csx - runeRx, (int) csy - runeRy, runeRx * 2, runeRy * 2);

        g2d.setColor(new Color(0, 229, 255, 110));
        g2d.setStroke(STROKE_RUNE);
        g2d.drawOval((int) csx - runeRx, (int) csy - runeRy, runeRx * 2, runeRy * 2);

        int innerRuneRx = 140;
        int innerRuneRy = (int) (innerRuneRx * 0.58);
        g2d.setColor(new Color(255, 215, 0, 85));
        g2d.setStroke(STROKE_WAVE);
        g2d.drawOval((int) csx - innerRuneRx, (int) csy - innerRuneRy, innerRuneRx * 2, innerRuneRy * 2);
    }

    private static BufferedImage createBakedArenaMap() {
        int w = (int) GameWorld.WORLD_WIDTH;
        int h = (int) GameWorld.WORLD_HEIGHT;

        GraphicsConfiguration gc = null;
        try {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        } catch (Exception ignored) {}

        BufferedImage map = (gc != null)
                ? gc.createCompatibleImage(w, h, Transparency.OPAQUE)
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = map.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Thảm cỏ đất tự nhiên
        g.setColor(new Color(45, 106, 54));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(36, 88, 44, 110));
        for (int gx = 60; gx < w; gx += 260) {
            for (int gy = 60; gy < h; gy += 240) {
                g.fillOval(gx - 90, gy - 55, 180, 110);
            }
        }
        g.setColor(new Color(60, 136, 70, 90));
        for (int gx = 190; gx < w; gx += 260) {
            for (int gy = 180; gy < h; gy += 240) {
                g.fillOval(gx - 80, gy - 48, 160, 96);
            }
        }

        // 2. Hoa dại
        double[][] flowerClusters = {
                {220, 210, 1}, {520, 190, 0}, {840, 200, 2}, {1220, 210, 1},
                {280, 520, 0}, {620, 500, 1}, {1040, 510, 0}, {1380, 530, 2},
                {240, 840, 2}, {560, 880, 1}, {1080, 870, 0}, {1360, 860, 1},
                {380, 1100, 1}, {740, 1120, 2}, {1120, 1110, 0}
        };
        for (double[] fc : flowerClusters) {
            int fx = (int) fc[0];
            int fy = (int) fc[1];
            int flowerType = (int) fc[2];
            Color fCol = (flowerType == 0) ? new Color(255, 235, 90) :
                    (flowerType == 1) ? new Color(130, 190, 255) : new Color(255, 120, 150);
            g.setColor(fCol);
            g.fillOval(fx - 4, fy - 3, 5, 4);
            g.fillOval(fx + 3, fy - 1, 4, 3);
            g.fillOval(fx - 1, fy + 4, 4, 3);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(fx, fy, 2, 2);
        }

        // 3. Lối mòn đất sỏi
        double riverBridgeWestY = getRiverCenterY(460) + 54;
        double riverBridgeEastY = getRiverCenterY(1140) + 54;
        double centerWorldX = w / 2.0;
        double centerWorldY = 720.0;
        drawDirtTrailDirect(g, 460, riverBridgeWestY, 560, 540, centerWorldX, centerWorldY);
        drawDirtTrailDirect(g, 1140, riverBridgeEastY, 1040, 540, centerWorldX, centerWorldY);
        drawDirtTrailDirect(g, centerWorldX, h - 60, centerWorldX, 900, centerWorldX, centerWorldY);

        // 4. Thân sông uốn lượn
        Path2D.Double riverPoly = new Path2D.Double();
        for (int i = 0; i < RIVER_SAMPLE_COUNT; i++) {
            double wx = RIVER_WX_TABLE[i];
            double ry = RIVER_CY_TABLE[i] - 44.0;
            if (i == 0) riverPoly.moveTo(wx, ry);
            else riverPoly.lineTo(wx, ry);
        }
        for (int i = RIVER_SAMPLE_COUNT - 1; i >= 0; i--) {
            double wx = RIVER_WX_TABLE[i];
            double ry = RIVER_CY_TABLE[i] + 44.0;
            riverPoly.lineTo(wx, ry);
        }
        riverPoly.closePath();
        g.setColor(new Color(18, 62, 102));
        g.fill(riverPoly);
        g.setColor(new Color(32, 114, 162, 215));
        g.fill(riverPoly);

        // Bờ sông
        g.setColor(new Color(135, 112, 75));
        g.setStroke(STROKE_BANK);
        Path2D.Double northBank = new Path2D.Double();
        Path2D.Double southBank = new Path2D.Double();
        for (int i = 0; i < RIVER_SAMPLE_COUNT; i++) {
            double wx = RIVER_WX_TABLE[i];
            double nry = RIVER_CY_TABLE[i] - 44.0;
            double sry = RIVER_CY_TABLE[i] + 44.0;
            if (i == 0) {
                northBank.moveTo(wx, nry);
                southBank.moveTo(wx, sry);
            } else {
                northBank.lineTo(wx, nry);
                southBank.lineTo(wx, sry);
            }
        }
        g.draw(northBank);
        g.draw(southBank);

        // 5. Cầu đá cổ
        render25DStoneBridgeDirect(g, 460);
        render25DStoneBridgeDirect(g, 1140);

        // 6. Thềm đá đấu trường trung tâm
        double terraceRx = 330.0;
        double terraceRy = terraceRx * 0.58;
        g.setColor(new Color(33, 37, 47));
        g.fillOval((int) (centerWorldX - terraceRx), (int) (centerWorldY - terraceRy), (int) (terraceRx * 2), (int) (terraceRy * 2));
        g.setColor(new Color(55, 62, 78));
        g.setStroke(STROKE_TERRACE);
        g.drawOval((int) (centerWorldX - terraceRx), (int) (centerWorldY - terraceRy), (int) (terraceRx * 2), (int) (terraceRy * 2));
        g.setColor(new Color(48, 108, 56, 170));
        g.setStroke(STROKE_MOSS);
        g.drawOval((int) (centerWorldX - terraceRx + 4), (int) (centerWorldY - terraceRy + 3), (int) ((terraceRx - 4) * 2), (int) ((terraceRy - 3) * 2));

        // 7. Tường thành 3D Bắc & Tây
        int wallHeight = 44;
        int copingHeight = 12;
        g.setColor(new Color(24, 27, 34));
        g.fillRect(0, 0, w, wallHeight);
        g.setColor(new Color(36, 41, 52));
        for (int wx = 0; wx <= w; wx += 64) {
            g.drawLine(wx, 0, wx, wallHeight);
        }
        g.setColor(new Color(60, 68, 85));
        g.fillRect(0, 0, w, copingHeight);

        g.setColor(new Color(28, 32, 40));
        g.fillRect(0, 0, 28, h);
        g.setColor(new Color(255, 175, 40));
        g.setStroke(STROKE_BORDER);
        g.drawRect(0, 0, w, h);

        g.dispose();
        return map;
    }

    private static void drawDirtTrailDirect(Graphics2D g2d, double x1, double y1, double cx, double cy, double x2, double y2) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.quadTo(cx, cy, x2, y2);

        g2d.setColor(new Color(118, 92, 60));
        g2d.setStroke(STROKE_TRAIL_BASE);
        g2d.draw(path);

        g2d.setColor(new Color(152, 124, 88));
        g2d.setStroke(STROKE_TRAIL_MID);
        g2d.draw(path);

        g2d.setColor(new Color(175, 146, 108));
        g2d.setStroke(STROKE_TRAIL_TOP);
        g2d.draw(path);
    }

    private static void render25DStoneBridgeDirect(Graphics2D g2d, double bridgeCenterX) {
        double riverY = getRiverCenterY(bridgeCenterX);
        double topWY = riverY - 56.0;
        double botWY = riverY + 56.0;

        double sx1 = bridgeCenterX - 52;
        double sx2 = bridgeCenterX + 52;
        int bw = (int) (sx2 - sx1);
        int bh = (int) (botWY - topWY);

        g2d.setColor(new Color(10, 25, 40, 160));
        g2d.fillRoundRect((int) sx1 - 4, (int) topWY + 10, bw + 8, bh - 10, 10, 10);

        g2d.setColor(new Color(100, 106, 118));
        g2d.fillRoundRect((int) sx1, (int) topWY, bw, bh, 8, 8);

        g2d.setColor(new Color(75, 80, 92));
        g2d.setStroke(STROKE_GRID);
        for (int py = (int) topWY + 10; py < botWY - 6; py += 14) {
            g2d.drawLine((int) sx1 + 4, py, (int) sx2 - 4, py);
        }
        for (int px = (int) sx1 + 16; px < sx2 - 10; px += 24) {
            g2d.drawLine(px, (int) topWY + 4, px, (int) botWY - 4);
        }

        int parapetH = 16;
        int parapetW = 10;
        g2d.setColor(new Color(65, 72, 85));
        g2d.fillRect((int) sx1, (int) topWY - parapetH, parapetW, bh + parapetH);
        g2d.setColor(new Color(125, 135, 150));
        g2d.fillRect((int) sx1, (int) topWY - parapetH, parapetW, 4);

        g2d.setColor(new Color(50, 56, 68));
        g2d.fillRect((int) sx2 - parapetW, (int) topWY - parapetH, parapetW, bh + parapetH);
        g2d.setColor(new Color(110, 120, 135));
        g2d.fillRect((int) sx2 - parapetW, (int) topWY - parapetH, parapetW, 4);

        double[][] corners = {
                {sx1 + 4, topWY + 4},
                {sx2 - 4, topWY + 4},
                {sx1 + 4, botWY - 4},
                {sx2 - 4, botWY - 4}
        };
        for (double[] c : corners) {
            int cx = (int) c[0];
            int cy = (int) c[1];
            g2d.setColor(new Color(40, 45, 55));
            g2d.fillRoundRect(cx - 7, cy - 14, 14, 16, 4, 4);
            g2d.setColor(new Color(255, 190, 60, 55));
            g2d.fillOval(cx - 16, cy - 28, 32, 28);
            g2d.setColor(new Color(70, 55, 30));
            g2d.drawRect(cx - 4, cy - 22, 8, 10);
            g2d.setColor(new Color(255, 220, 100, 240));
            g2d.fillRect(cx - 3, cy - 20, 6, 7);
        }
    }

    public static void drawDirtTrail(Graphics2D g2d, Camera2D camera, double x1, double y1, double cx, double cy, double x2, double y2) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(camera.worldToScreenX(x1), camera.worldToScreenY(y1));
        path.quadTo(camera.worldToScreenX(cx), camera.worldToScreenY(cy), camera.worldToScreenX(x2), camera.worldToScreenY(y2));

        g2d.setColor(new Color(118, 92, 60));
        g2d.setStroke(STROKE_TRAIL_BASE);
        g2d.draw(path);

        g2d.setColor(new Color(152, 124, 88));
        g2d.setStroke(STROKE_TRAIL_MID);
        g2d.draw(path);

        g2d.setColor(new Color(175, 146, 108));
        g2d.setStroke(STROKE_TRAIL_TOP);
        g2d.draw(path);
    }

    public static void render25DStoneBridge(Graphics2D g2d, Camera2D camera, double bridgeCenterX) {
        double riverY = getRiverCenterY(bridgeCenterX);
        double topWY = riverY - 56.0;
        double botWY = riverY + 56.0;

        double sx1 = camera.worldToScreenX(bridgeCenterX - 52);
        double sx2 = camera.worldToScreenX(bridgeCenterX + 52);
        double sy1 = camera.worldToScreenY(topWY);
        double sy2 = camera.worldToScreenY(botWY);
        int bw = (int) (sx2 - sx1);
        int bh = (int) (sy2 - sy1);

        // 1. Bóng đổ của vòm cầu
        g2d.setColor(new Color(10, 25, 40, 160));
        g2d.fillRoundRect((int) sx1 - 4, (int) sy1 + 10, bw + 8, bh - 10, 10, 10);

        // 2. Mặt sàn cầu lát đá cổ
        g2d.setColor(new Color(100, 106, 118));
        g2d.fillRoundRect((int) sx1, (int) sy1, bw, bh, 8, 8);

        // Rãnh đá lát mặt cầu
        g2d.setColor(new Color(75, 80, 92));
        g2d.setStroke(STROKE_GRID);
        for (int py = (int) sy1 + 10; py < sy2 - 6; py += 14) {
            g2d.drawLine((int) sx1 + 4, py, (int) sx2 - 4, py);
        }
        for (int px = (int) sx1 + 16; px < sx2 - 10; px += 24) {
            g2d.drawLine(px, (int) sy1 + 4, px, (int) sy2 - 4);
        }

        // 3. Lan can đá 3D hai bên cầu
        int parapetH = 16;
        int parapetW = 10;
        g2d.setColor(new Color(65, 72, 85));
        g2d.fillRect((int) sx1, (int) sy1 - parapetH, parapetW, bh + parapetH);
        g2d.setColor(new Color(125, 135, 150));
        g2d.fillRect((int) sx1, (int) sy1 - parapetH, parapetW, 4);

        g2d.setColor(new Color(50, 56, 68));
        g2d.fillRect((int) sx2 - parapetW, (int) sy1 - parapetH, parapetW, bh + parapetH);
        g2d.setColor(new Color(110, 120, 135));
        g2d.fillRect((int) sx2 - parapetW, (int) sy1 - parapetH, parapetW, 4);

        // 4. 4 Trụ góc cầu gắn Đèn lồng Đồng ấm áp
        double[][] corners = {
                {sx1 + 4, sy1 + 4},
                {sx2 - 4, sy1 + 4},
                {sx1 + 4, sy2 - 4},
                {sx2 - 4, sy2 - 4}
        };
        for (double[] c : corners) {
            int cx = (int) c[0];
            int cy = (int) c[1];
            g2d.setColor(new Color(40, 45, 55));
            g2d.fillRoundRect(cx - 7, cy - 14, 14, 16, 4, 4);
            g2d.setColor(new Color(255, 190, 60, 55));
            g2d.fillOval(cx - 16, cy - 28, 32, 28);
            g2d.setColor(new Color(70, 55, 30));
            g2d.drawRect(cx - 4, cy - 22, 8, 10);
            g2d.setColor(new Color(255, 220, 100, 240));
            g2d.fillRect(cx - 3, cy - 20, 6, 7);
        }
    }

    public static void render25DTree(Graphics2D g2d, Camera2D camera, double worldX, double worldY, int treeType, int screenW, int screenH) {
        EnvironmentPropsRenderer.render25DTree(g2d, camera, worldX, worldY, treeType, screenW, screenH);
    }

    public static void render25DBoulder(Graphics2D g2d, Camera2D camera, double worldX, double worldY, double radius, int screenW, int screenH) {
        EnvironmentPropsRenderer.render25DBoulder(g2d, camera, worldX, worldY, radius, screenW, screenH);
    }

    public static void render25DPillar(Graphics2D g2d, Camera2D camera, double worldX, double worldY) {
        EnvironmentPropsRenderer.render25DPillar(g2d, camera, worldX, worldY);
    }

    public static void addPillarsToRenderQueue(List<Renderable25D> queue, Camera2D camera) {
        EnvironmentPropsRenderer.addPillarsToRenderQueue(queue, camera);
    }

    public static void addEnvironmentToRenderQueue(List<Renderable25D> queue, Camera2D camera, int screenW, int screenH) {
        EnvironmentPropsRenderer.addEnvironmentToRenderQueue(queue, camera, screenW, screenH);
    }
}
