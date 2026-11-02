package render;

import core.GameWorld;

import java.awt.*;
import java.awt.geom.Path2D;
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

        // 2. THẢM CỎ ĐẤT TỰ NHIÊN (Lush Natural Meadow Base)
        g2d.setColor(new Color(45, 106, 54));
        g2d.fillRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));

        // Mảng cỏ chuyển màu tạo khối tự nhiên
        g2d.setColor(new Color(36, 88, 44, 110));
        for (int gx = 60; gx < GameWorld.WORLD_WIDTH; gx += 260) {
            for (int gy = 60; gy < GameWorld.WORLD_HEIGHT; gy += 240) {
                int gsx = (int) camera.worldToScreenX(gx);
                int gsy = (int) camera.worldToScreenY(gy);
                g2d.fillOval(gsx - 90, gsy - 55, 180, 110);
            }
        }
        g2d.setColor(new Color(60, 136, 70, 90));
        for (int gx = 190; gx < GameWorld.WORLD_WIDTH; gx += 260) {
            for (int gy = 180; gy < GameWorld.WORLD_HEIGHT; gy += 240) {
                int gsx = (int) camera.worldToScreenX(gx);
                int gsy = (int) camera.worldToScreenY(gy);
                g2d.fillOval(gsx - 80, gsy - 48, 160, 96);
            }
        }

        // Hoa dại rực rỡ điểm xuyết trên thảo nguyên (Wildflowers)
        double[][] flowerClusters = {
                {220, 210, 1}, {520, 190, 0}, {840, 200, 2}, {1220, 210, 1},
                {280, 520, 0}, {620, 500, 1}, {1040, 510, 0}, {1380, 530, 2},
                {240, 840, 2}, {560, 880, 1}, {1080, 870, 0}, {1360, 860, 1},
                {380, 1100, 1}, {740, 1120, 2}, {1120, 1110, 0}
        };
        for (double[] fc : flowerClusters) {
            int fx = (int) camera.worldToScreenX(fc[0]);
            int fy = (int) camera.worldToScreenY(fc[1]);
            int flowerType = (int) fc[2];
            Color fCol = (flowerType == 0) ? new Color(255, 235, 90) :
                    (flowerType == 1) ? new Color(130, 190, 255) :
                            new Color(255, 120, 150);
            g2d.setColor(fCol);
            g2d.fillOval(fx - 4, fy - 3, 5, 4);
            g2d.fillOval(fx + 3, fy - 1, 4, 3);
            g2d.fillOval(fx - 1, fy + 4, 4, 3);
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillOval(fx, fy, 2, 2);
        }

        // 3. LỐI MÒN ĐẤT SỎI UỐN LƯỢN (Winding Cobblestone & Dirt Paths)
        double riverBridgeWestY = getRiverCenterY(460) + 54;
        double riverBridgeEastY = getRiverCenterY(1140) + 54;
        double centerWorldX = GameWorld.WORLD_WIDTH / 2.0;
        double centerWorldY = 720.0;

        drawDirtTrail(g2d, camera, 460, riverBridgeWestY, 560, 540, centerWorldX, centerWorldY);
        drawDirtTrail(g2d, camera, 1140, riverBridgeEastY, 1040, 540, centerWorldX, centerWorldY);
        drawDirtTrail(g2d, camera, centerWorldX, GameWorld.WORLD_HEIGHT - 60, centerWorldX, 900, centerWorldX, centerWorldY);

        // 4. DÒNG SÔNG 2.5D UỐN LƯỢN (Animated Meandering 2.5D River với Bảng Tra Cứu Trước Tọa Độ)
        Path2D.Double riverPoly = new Path2D.Double();
        for (int i = 0; i < RIVER_SAMPLE_COUNT; i++) {
            double wx = RIVER_WX_TABLE[i];
            double ry = RIVER_CY_TABLE[i] - 44.0;
            double sx = camera.worldToScreenX(wx);
            double sy = camera.worldToScreenY(ry);
            if (i == 0) riverPoly.moveTo(sx, sy);
            else riverPoly.lineTo(sx, sy);
        }
        for (int i = RIVER_SAMPLE_COUNT - 1; i >= 0; i--) {
            double wx = RIVER_WX_TABLE[i];
            double ry = RIVER_CY_TABLE[i] + 44.0;
            double sx = camera.worldToScreenX(wx);
            double sy = camera.worldToScreenY(ry);
            riverPoly.lineTo(sx, sy);
        }
        riverPoly.closePath();

        // Đáy sông sâu thẳm
        g2d.setColor(new Color(18, 62, 102));
        g2d.fill(riverPoly);

        // Mặt nước trong xanh biếc
        g2d.setColor(new Color(32, 114, 162, 215));
        g2d.fill(riverPoly);

        // Sóng nước chuyển động lấp lánh (Animated Water Waves)
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
                if (j == 0) {
                    wavePath.moveTo(sx, sy);
                } else {
                    wavePath.lineTo(sx, sy);
                }
            }
            g2d.draw(wavePath);
        }

        // Bờ sông Bắc & Nam (Moist Riverbanks)
        g2d.setColor(new Color(135, 112, 75));
        g2d.setStroke(STROKE_BANK);
        Path2D.Double northBank = new Path2D.Double();
        Path2D.Double southBank = new Path2D.Double();
        for (int i = 0; i < RIVER_SAMPLE_COUNT; i++) {
            double wx = RIVER_WX_TABLE[i];
            double nry = RIVER_CY_TABLE[i] - 44.0;
            double sry = RIVER_CY_TABLE[i] + 44.0;
            double nsx = camera.worldToScreenX(wx);
            double nsy = camera.worldToScreenY(nry);
            double ssx = camera.worldToScreenX(wx);
            double ssy = camera.worldToScreenY(sry);
            if (i == 0) {
                northBank.moveTo(nsx, nsy);
                southBank.moveTo(ssx, ssy);
            } else {
                northBank.lineTo(nsx, nsy);
                southBank.lineTo(ssx, ssy);
            }
        }
        g2d.draw(northBank);
        g2d.draw(southBank);

        // 5. CẦU ĐÁ CỔ BẮC QUA SÔNG 2.5D (Ancient 2.5D Stone Arch Bridges)
        render25DStoneBridge(g2d, camera, 460);
        render25DStoneBridge(g2d, camera, 1140);

        // 6. THỀM ĐÁ ĐẤU TRƯỜNG TRUNG TÂM (Central Stone Arena Terrace)
        double terraceRx = 330.0;
        double terraceRy = terraceRx * 0.58;
        double csx = camera.worldToScreenX(centerWorldX);
        double csy = camera.worldToScreenY(centerWorldY);

        // Thềm đá nền
        g2d.setColor(new Color(33, 37, 47));
        g2d.fillOval((int) (csx - terraceRx), (int) (csy - terraceRy), (int) (terraceRx * 2), (int) (terraceRy * 2));

        // Gờ đá bao quanh thềm đấu
        g2d.setColor(new Color(55, 62, 78));
        g2d.setStroke(STROKE_TERRACE);
        g2d.drawOval((int) (csx - terraceRx), (int) (csy - terraceRy), (int) (terraceRx * 2), (int) (terraceRy * 2));

        // Rêu phong phủ rìa thềm đá (Overgrown Moss)
        g2d.setColor(new Color(48, 108, 56, 170));
        g2d.setStroke(STROKE_MOSS);
        g2d.drawOval((int) (csx - terraceRx + 4), (int) (csy - terraceRy + 3), (int) ((terraceRx - 4) * 2), (int) ((terraceRy - 3) * 2));

        // Mạng lưới gạch kim cương 2.5D trên mặt đài tế (Tối ưu hóa phạm vi quét)
        g2d.setColor(new Color(42, 47, 59));
        g2d.setStroke(STROKE_GRID);
        double tileSize = 96.0;
        double minD = centerWorldX - 520.0;
        double maxD = centerWorldX + 520.0;
        for (double d = minD; d <= maxD; d += tileSize) {
            double x1 = d;
            double y1 = 0;
            double x2 = d + GameWorld.WORLD_HEIGHT;
            double y2 = GameWorld.WORLD_HEIGHT;

            int sx1 = (int) camera.worldToScreenX(x1);
            int sy1 = (int) camera.worldToScreenY(y1);
            int sx2 = (int) camera.worldToScreenX(x2);
            int sy2 = (int) camera.worldToScreenY(y2);

            g2d.drawLine(sx1, sy1, sx2, sy2);

            double x3 = d;
            double y3 = GameWorld.WORLD_HEIGHT;
            double x4 = d + GameWorld.WORLD_HEIGHT;
            double y4 = 0;
            int sx3 = (int) camera.worldToScreenX(x3);
            int sy3 = (int) camera.worldToScreenY(y3);
            int sx4 = (int) camera.worldToScreenX(x4);
            int sy4 = (int) camera.worldToScreenY(y4);
            g2d.drawLine(sx3, sy3, sx4, sy4);
        }

        // 7. THẦN TRẬN PHONG ẤN TÂM ĐẤU TRƯỜNG 2.5D (Central Arcane Runic Seal)
        int runeRx = 220;
        int runeRy = (int) (runeRx * 0.58);

        // Vòng ngoài phát sáng
        g2d.setColor(new Color(0, 229, 255, 30));
        g2d.fillOval((int) csx - runeRx, (int) csy - runeRy, runeRx * 2, runeRy * 2);

        g2d.setColor(new Color(0, 229, 255, 110));
        g2d.setStroke(STROKE_RUNE);
        g2d.drawOval((int) csx - runeRx, (int) csy - runeRy, runeRx * 2, runeRy * 2);

        // Vòng khắc bên trong
        int innerRuneRx = 140;
        int innerRuneRy = (int) (innerRuneRx * 0.58);
        g2d.setColor(new Color(255, 215, 0, 85));
        g2d.setStroke(STROKE_WAVE);
        g2d.drawOval((int) csx - innerRuneRx, (int) csy - innerRuneRy, innerRuneRx * 2, innerRuneRy * 2);

        // Họa tiết ngôi sao 8 cánh xoay tròn 2.5D
        double rot = time * 0.0008;
        for (int i = 0; i < 8; i++) {
            double angle = rot + i * (Math.PI / 4.0);
            double rx1 = csx + Math.cos(angle) * runeRx;
            double ry1 = csy + Math.sin(angle) * runeRy;
            double rx2 = csx + Math.cos(angle + Math.PI) * runeRx;
            double ry2 = csy + Math.sin(angle + Math.PI) * runeRy;
            g2d.setColor(new Color(0, 229, 255, 45));
            g2d.drawLine((int) rx1, (int) ry1, (int) rx2, (int) ry2);
        }

        // 8. TƯỜNG THÀNH 3D CÓ ĐỘ CAO (3D Perimeter Walls with Height & Shadows)
        int wallHeight = 44;
        int copingHeight = 12;

        // --- TƯỜNG PHÍA BẮC (North Wall) ---
        g2d.setColor(new Color(24, 27, 34));
        g2d.fillRect((int) startX, (int) startY - wallHeight, (int) (endX - startX), wallHeight);

        // Gạch đá trên mặt đứng
        g2d.setColor(new Color(36, 41, 52));
        for (double wx = 0; wx <= GameWorld.WORLD_WIDTH; wx += 64) {
            int wsx = (int) camera.worldToScreenX(wx);
            g2d.drawLine(wsx, (int) startY - wallHeight, wsx, (int) startY);
        }

        // Mặt trên bờ tường đón sáng
        g2d.setColor(new Color(60, 68, 85));
        g2d.fillRect((int) startX - 8, (int) startY - wallHeight - copingHeight, (int) (endX - startX) + 16, copingHeight);
        g2d.setColor(new Color(90, 102, 126));
        g2d.drawLine((int) startX - 8, (int) startY - wallHeight - copingHeight, (int) endX + 8, (int) startY - wallHeight - copingHeight);

        // Đổ bóng của tường Bắc xuống mặt sàn đấu trường
        g2d.setPaint(new GradientPaint(
                0, (float) startY, new Color(0, 0, 0, 140),
                0, (float) (startY + 38), new Color(0, 0, 0, 0)
        ));
        g2d.fillRect((int) startX, (int) startY, (int) (endX - startX), 38);

        // --- TƯỜNG PHÍA TÂY (West Wall) ---
        int westWallW = 28;
        g2d.setColor(new Color(28, 32, 40));
        g2d.fillRect((int) startX - westWallW, (int) startY - wallHeight, westWallW, (int) (endY - startY) + wallHeight);
        g2d.setPaint(new GradientPaint(
                (float) startX, 0, new Color(0, 0, 0, 120),
                (float) (startX + 28), 0, new Color(0, 0, 0, 0)
        ));
        g2d.fillRect((int) startX, (int) startY, 28, (int) (endY - startY));

        // --- GỜ ĐÁ BẢO VỆ PHÍA ĐÔNG & NAM ---
        g2d.setColor(new Color(255, 175, 40));
        g2d.setStroke(STROKE_BORDER);
        g2d.drawRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));
        g2d.setStroke(STROKE_1);
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
