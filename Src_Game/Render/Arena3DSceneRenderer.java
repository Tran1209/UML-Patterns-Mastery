package render;

import combat.CombatEffects;
import core.GameWorld;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Bộ kết xuất Bản Đồ Đấu Trường 2.5D Chuẩn Liên Quân / Liên Minh (MOBA 2.5D Dimetric Scene Renderer).
 * Đặc trưng:
 * - Mặt đất nghiêng 2.5D mượt mà (0% méo hình thang khi di chuyển).
 * - Địa hình MOBA Bình Nguyên Vô Tận: Thảm cỏ rừng 2.5D, Sông Ngân Hà có bậc đá bờ sông (wz = -12),
 *   Cầu Đá Đường Giữa (Mid Lane Bridge), Tế Đàn Trung Tâm (800, 600).
 * - 4 Trụ Bảo Vệ MOBA 2.5D & 8 Cây Cổ Thụ Rừng 2.5D có chiều cao Z thực tế, tham gia Z-Sorting che khuất nhân vật.
 */
public final class Arena3DSceneRenderer {

    public static final double[][] OBELISK_POSITIONS = {
            {340.0, 280.0},
            {1260.0, 280.0},
            {340.0, 920.0},
            {1260.0, 920.0}
    };

    public static final double[][] TREE_POSITIONS = {
            {180.0, 200.0}, {540.0, 190.0}, {1060.0, 190.0}, {1420.0, 200.0},
            {180.0, 1000.0}, {540.0, 1010.0}, {1060.0, 1010.0}, {1420.0, 1000.0}
    };

    private Arena3DSceneRenderer() {}

    public static void renderArena3DFloorAndSky(Graphics2D g2d, Camera2D camera, int width, int height) {
        // 1. Nền vực thẳm bao quanh đấu trường 2.5D
        GradientPaint abyssGrad = new GradientPaint(
                0, 0, new Color(10, 15, 24),
                0, height, new Color(16, 22, 34)
        );
        g2d.setPaint(abyssGrad);
        g2d.fillRect(0, 0, width, height);

        double W = GameWorld.WORLD_WIDTH;
        double H = GameWorld.WORLD_HEIGHT;
        double zoom = camera.getZoom();
        double pitch = camera.getPitchTilt();

        double leftX = camera.worldToScreenX(0, 0);
        double rightX = camera.worldToScreenX(W, 0);
        double topY = camera.worldToScreenY(0, 0);
        double bottomY = camera.worldToScreenY(H, 0);
        int floorW = (int) (rightX - leftX);
        int floorH = (int) (bottomY - topY);

        // 2. Vách Đá Đế Đấu Trường Nổi 2.5D (Front Cliff Wall tại wz = -75..0)
        if (camera.is3DMode()) {
            double cliffBottomY = camera.worldToScreenY(H, -75.0);
            int cliffH = Math.max(0, (int) (cliffBottomY - bottomY));
            GradientPaint cliffPaint = new GradientPaint(
                    0, (float) bottomY, new Color(34, 42, 54),
                    0, (float) cliffBottomY, new Color(12, 16, 24)
            );
            g2d.setPaint(cliffPaint);
            g2d.fillRect((int) leftX, (int) bottomY, floorW, cliffH);

            g2d.setColor(new Color(65, 80, 100, 130));
            g2d.setStroke(new BasicStroke(2f));
            for (double gx = 0; gx <= W; gx += 160) {
                int sx = (int) camera.worldToScreenX(gx, H);
                g2d.drawLine(sx, (int) bottomY, sx, (int) cliffBottomY);
            }
        }

        // 3. Nền Thảm Cỏ Rừng MOBA 2.5D (Emerald MOBA Forest Turf)
        GradientPaint turfGrad = new GradientPaint(
                0, (float) topY, new Color(28, 46, 40),
                0, (float) bottomY, new Color(36, 58, 48)
        );
        g2d.setPaint(turfGrad);
        g2d.fillRect((int) leftX, (int) topY, floorW, floorH);

        // 4. Lưới Ô Đá Mờ 2.5D (Tạo cảm giác mặt phẳng nghiêng 2.5D mượt mà khi di chuyển)
        int tileSize = 80;
        g2d.setStroke(new BasicStroke(1f));
        g2d.setColor(new Color(255, 255, 255, 18));
        for (double wx = 0; wx <= W; wx += tileSize) {
            int sx = (int) camera.worldToScreenX(wx, 0);
            g2d.drawLine(sx, (int) topY, sx, (int) bottomY);
        }
        for (double wy = 0; wy <= H; wy += tileSize) {
            int sy = (int) camera.worldToScreenY(wy, 0);
            g2d.drawLine((int) leftX, sy, (int) rightX, sy);
        }

        // 5. Sông Ngân Hà MOBA (MOBA River chảy ngang wy = 535..665 với bờ đá chìm 2.5D)
        double riverTopY = camera.worldToScreenY(535, 0);
        double riverBotY = camera.worldToScreenY(665, 0);
        double riverBankSubY = camera.worldToScreenY(535, -12.0);

        // Bờ đá chìm 2.5D phía bắc dòng sông
        if (camera.is3DMode()) {
            g2d.setColor(new Color(42, 52, 64));
            g2d.fillRect((int) leftX, (int) riverTopY, floorW, Math.max(2, (int) (riverBankSubY - riverTopY)));
        }

        // Mặt nước sông phát sáng
        GradientPaint riverPaint = new GradientPaint(
                0, (float) riverTopY, new Color(18, 78, 110, 210),
                0, (float) riverBotY, new Color(24, 108, 142, 210)
        );
        g2d.setPaint(riverPaint);
        g2d.fillRect((int) leftX, (int) riverBankSubY, floorW, Math.max(4, (int) (riverBotY - riverBankSubY)));

        // 6. Đường Lát Đá Giữa Bản Đồ (Mid Lane Stone Road & Cầu Đá Bắc Qua Sông)
        double laneLeftX = camera.worldToScreenX(690, 0);
        double laneRightX = camera.worldToScreenX(910, 0);
        int laneW = (int) (laneRightX - laneLeftX);
        g2d.setColor(new Color(58, 66, 78, 215));
        g2d.fillRect((int) laneLeftX, (int) topY, laneW, floorH);
        g2d.setColor(new Color(120, 140, 165, 120));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine((int) laneLeftX, (int) topY, (int) laneLeftX, (int) bottomY);
        g2d.drawLine((int) laneRightX, (int) topY, (int) laneRightX, (int) bottomY);

        // Đường ngang nối 2 cánh rừng (Side Lane Connector)
        double hLaneTop = camera.worldToScreenY(560, 0);
        double hLaneBot = camera.worldToScreenY(640, 0);
        g2d.setColor(new Color(64, 74, 88, 185));
        g2d.fillRect((int) laneLeftX - (int) (20 * zoom), (int) hLaneTop, laneW + (int) (40 * zoom), (int) (hLaneBot - hLaneTop));

        // 7. Vòng Tế Đàn Cổ Ngữ 2.5D ở Tâm Bản Đồ (800, 600)
        double centerX = camera.worldToScreenX(W / 2.0, H / 2.0);
        double centerY = camera.worldToScreenY(H / 2.0, 0);
        int runeRx = (int) (145 * zoom);
        int runeRy = (int) (145 * zoom * pitch);

        g2d.setColor(new Color(48, 56, 68, 235));
        g2d.fillOval((int) centerX - runeRx, (int) centerY - runeRy, runeRx * 2, runeRy * 2);
        g2d.setColor(new Color(0, 215, 255, 130));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawOval((int) centerX - runeRx, (int) centerY - runeRy, runeRx * 2, runeRy * 2);
        g2d.setColor(new Color(255, 200, 60, 120));
        g2d.drawOval((int) centerX - (int) (runeRx * 0.68), (int) centerY - (int) (runeRy * 0.68),
                (int) (runeRx * 1.36), (int) (runeRy * 1.36));

        // 8. Tường Thành Đá 2.5D Bao Quanh Đấu Trường (wz = 0..42)
        if (camera.is3DMode()) {
            double topWallRoofY = camera.worldToScreenY(0, 42.0);
            int wallH = Math.max(0, (int) (topY - topWallRoofY));
            g2d.setColor(new Color(46, 56, 72));
            g2d.fillRect((int) leftX, (int) topWallRoofY, floorW, wallH);
            g2d.setColor(new Color(74, 88, 110));
            g2d.fillRect((int) leftX, (int) topWallRoofY - 6, floorW, 6);
        }

        g2d.setColor(new Color(215, 165, 55, 210));
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.drawRect((int) leftX, (int) topY, floorW, floorH);
        g2d.setStroke(new BasicStroke(1f));
    }

    /**
     * Vẽ Cây Cổ Thụ Rừng 2.5D (Volumetric 2.5D Jungle Tree) có thân gỗ trục Z và 3 tán lá cầu đổ bóng.
     */
    public static void render25DForestTree(Graphics2D g2d, Camera2D camera, double wx, double wy) {
        double zoom = camera.getZoom();
        double pitch = camera.getPitchTilt();
        double sx = camera.worldToScreenX(wx, wy);
        double syGround = camera.worldToScreenY(wy, 0);

        int shadowRx = (int) (34 * zoom);
        int shadowRy = (int) (34 * zoom * pitch);
        g2d.setColor(new Color(0, 0, 0, 85));
        g2d.fillOval((int) (sx - shadowRx + 10 * zoom), (int) (syGround - shadowRy * 0.8), shadowRx * 2, shadowRy * 2);

        double trunkTopY = camera.worldToScreenY(wy, 38.0);
        int trunkW = (int) (14 * zoom);
        if (camera.is3DMode()) {
            g2d.setColor(new Color(85, 55, 32));
            g2d.fillRoundRect((int) sx - trunkW / 2, (int) trunkTopY, trunkW, Math.max(8, (int) (syGround - trunkTopY)), 6, 6);
        }

        // 3 Tầng tán lá 2.5D xếp theo độ cao Z (wz = 38, 56, 72)
        double c1Y = camera.worldToScreenY(wy, 38.0);
        double c2Y = camera.worldToScreenY(wy, 56.0);
        double c3Y = camera.worldToScreenY(wy, 72.0);
        int r1 = (int) (32 * zoom);
        int r2 = (int) (26 * zoom);
        int r3 = (int) (18 * zoom);

        g2d.setColor(new Color(26, 92, 52));
        g2d.fillOval((int) sx - r1, (int) c1Y - r1, r1 * 2, r1 * 2);
        g2d.setColor(new Color(38, 126, 68));
        g2d.fillOval((int) (sx - r2 - 4 * zoom), (int) c2Y - r2, r2 * 2, r2 * 2);
        g2d.setColor(new Color(62, 164, 88));
        g2d.fillOval((int) (sx - r3 + 3 * zoom), (int) c3Y - r3, r3 * 2, r3 * 2);
    }

    /**
     * Dựng Trụ Bảo Vệ MOBA 2.5D (MOBA Sanctum Turret) có vòng tầm bắn mặt đất, thân trụ trục Z và Pha lê năng lượng.
     */
    public static void render3DObeliskTower(Graphics2D g2d, Camera2D camera, double wx, double wy, long timeMs) {
        double scale = camera.getZoom();
        double pitch = camera.getPitchTilt();
        double baseX = camera.worldToScreenX(wx, wy);
        double baseY = camera.worldToScreenY(wy, 0);

        double halfW = 28.0 * scale;
        double halfD = 24.0 * scale * pitch;
        double towerHeightZ = 90.0;
        double topY = camera.worldToScreenY(wy, towerHeightZ);

        // 1. Bóng đổ nghiêng 2.5D dưới chân trụ
        g2d.setColor(new Color(0, 0, 0, 95));
        g2d.fillOval((int) (baseX - halfW * 1.1 + 14 * scale), (int) (baseY - halfD * 0.85 + 6 * scale),
                (int) (halfW * 2.4), (int) (halfD * 1.9));

        // 2. Bệ đá tròn 2.5D dưới chân trụ (wz = 0)
        g2d.setColor(new Color(0, 210, 255, 50));
        g2d.fillOval((int) (baseX - halfW * 1.35), (int) (baseY - halfD * 1.35),
                (int) (halfW * 2.7), (int) (halfD * 2.7));
        g2d.setColor(new Color(255, 200, 65, 160));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval((int) (baseX - halfW * 1.35), (int) (baseY - halfD * 1.35),
                (int) (halfW * 2.7), (int) (halfD * 2.7));

        if (!camera.is3DMode()) {
            g2d.setColor(new Color(65, 80, 105));
            g2d.fillOval((int) (baseX - halfW), (int) (baseY - halfW), (int) (halfW * 2), (int) (halfW * 2));
            g2d.setColor(new Color(255, 205, 55));
            g2d.drawOval((int) (baseX - halfW), (int) (baseY - halfW), (int) (halfW * 2), (int) (halfW * 2));
            return;
        }

        // 3. Mặt bên trái Trụ 2.5D (Shaded Left Face)
        Path2D leftFace = new Path2D.Double();
        leftFace.moveTo(baseX - halfW, baseY);
        leftFace.lineTo(baseX, baseY + halfD);
        leftFace.lineTo(baseX, topY + halfD * 0.75);
        leftFace.lineTo(baseX - halfW * 0.78, topY);
        leftFace.closePath();
        g2d.setColor(new Color(48, 60, 82));
        g2d.fill(leftFace);

        // 4. Mặt bên phải Trụ 2.5D (Lit Right Face)
        Path2D rightFace = new Path2D.Double();
        rightFace.moveTo(baseX, baseY + halfD);
        rightFace.lineTo(baseX + halfW, baseY);
        rightFace.lineTo(baseX + halfW * 0.78, topY);
        rightFace.lineTo(baseX, topY + halfD * 0.75);
        rightFace.closePath();
        g2d.setColor(new Color(72, 90, 120));
        g2d.fill(rightFace);

        // 5. Mặt đỉnh Trụ 2.5D ở độ cao wz = 90
        double topHalfW = halfW * 0.78;
        double topHalfD = halfD * 0.75;
        Path2D topFace = new Path2D.Double();
        topFace.moveTo(baseX, topY - topHalfD);
        topFace.lineTo(baseX + topHalfW, topY);
        topFace.lineTo(baseX, topY + topHalfD);
        topFace.lineTo(baseX - topHalfW, topY);
        topFace.closePath();
        g2d.setColor(new Color(102, 125, 160));
        g2d.fill(topFace);

        // Viền giáp vàng MOBA quanh thân trụ
        g2d.setColor(new Color(245, 195, 55, 200));
        g2d.setStroke(new BasicStroke((float) (2.0 * scale)));
        g2d.draw(leftFace);
        g2d.draw(rightFace);
        g2d.draw(topFace);

        // 6. Khối Pha Lê Năng Lượng 2.5D Lơ Lửng trên đỉnh trụ (wz = 118 + bob)
        double bobZ = Math.sin(timeMs * 0.004 + wx) * 7.0;
        double crystalY = camera.worldToScreenY(wy, towerHeightZ + 26.0 + bobZ);
        double cw = 11.0 * scale;
        double ch = 17.0 * scale;

        Path2D crystal = new Path2D.Double();
        crystal.moveTo(baseX, crystalY - ch);
        crystal.lineTo(baseX + cw, crystalY);
        crystal.lineTo(baseX, crystalY + ch * 0.75);
        crystal.lineTo(baseX - cw, crystalY);
        crystal.closePath();

        g2d.setColor(new Color(0, 235, 255, 225));
        g2d.fill(crystal);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(crystal);
        g2d.setStroke(new BasicStroke(1f));
    }

    /**
     * Vẽ Hiệu Ứng Kỹ Năng 2.5D Chuẩn Liên Quân (Mặt phẳng Elip 2.5D + Hiệu ứng trục Z).
     */
    public static void render3DSkillEffect(Graphics2D g2d, Camera2D camera, CombatEffects.SkillRingEffect ring) {
        double sx = camera.worldToScreenX(ring.x, ring.y);
        double syGround = camera.worldToScreenY(ring.y, 0);
        double scale = camera.getZoom();
        double pitch = camera.getPitchTilt();

        float alpha = (float) Math.max(0f, Math.min(1f, ring.lifeTime / ring.maxLifeTime));
        double progress = Math.max(0.0, Math.min(1.0, 1.0 - alpha));

        int c = ring.color;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        int radX = (int) (ring.maxRadius * (0.25 + 0.75 * progress) * scale);
        int radY = (int) (radX * pitch);

        // 1. Vòng sóng xung kích mặt đất theo Elip 2.5D (wz = 0)
        g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.24f));
        g2d.fillOval((int) sx - radX, (int) syGround - radY, radX * 2, radY * 2);

        g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.92f));
        g2d.setStroke(new BasicStroke((float) (5.0 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawOval((int) sx - radX, (int) syGround - radY, radX * 2, radY * 2);

        if (!camera.is3DMode()) {
            g2d.setStroke(new BasicStroke(1f));
            return;
        }

        if (ring.skillType == 1) {
            double syUpper = camera.worldToScreenY(ring.y, 24.0);
            int upRx = (int) (radX * 0.78);
            int upRy = (int) (radY * 0.78);
            g2d.setColor(new Color(0.5f, 0.95f, 1.0f, alpha * 0.65f));
            g2d.setStroke(new BasicStroke((float) (3.0 * scale)));
            g2d.drawOval((int) sx - upRx, (int) syUpper - upRy, upRx * 2, upRy * 2);
        } else if (ring.skillType == 2) {
            for (int layer = 1; layer <= 4; layer++) {
                double zHeight = layer * 24.0 * (0.5 + 0.5 * progress);
                double syLayer = camera.worldToScreenY(ring.y, zHeight);
                double taper = 1.0 - layer * 0.12;
                int lRx = (int) (radX * taper);
                int lRy = (int) (radY * taper);
                double swirlX = Math.cos(progress * 10.0 + layer * 1.4) * 7.0 * scale;

                g2d.setColor(new Color(1.0f, Math.min(1f, (g + layer * 25) / 255f), 0.1f, alpha * (0.75f - layer * 0.12f)));
                g2d.setStroke(new BasicStroke((float) ((4.2 - layer * 0.5) * scale)));
                g2d.drawOval((int) (sx + swirlX) - lRx, (int) syLayer - lRy, lRx * 2, lRy * 2);
            }
        } else if (ring.skillType == 3) {
            if (progress < 0.55) {
                double fallRatio = 1.0 - (progress / 0.55);
                double meteorZ = 380.0 * fallRatio;
                double meteorX = sx - 95.0 * fallRatio * scale;
                double meteorY = camera.worldToScreenY(ring.y, meteorZ);
                int mRadius = (int) (34.0 * scale);

                g2d.setColor(new Color(1.0f, 0.45f, 0.0f, 0.65f));
                g2d.setStroke(new BasicStroke((float) (16.0 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine((int) meteorX, (int) meteorY,
                        (int) (meteorX - 60 * scale), (int) (meteorY - 110 * scale));

                g2d.setColor(new Color(255, 80, 20, 240));
                g2d.fillOval((int) meteorX - mRadius, (int) meteorY - mRadius, mRadius * 2, mRadius * 2);
                g2d.setColor(new Color(255, 240, 150, 250));
                int coreR = (int) (mRadius * 0.58);
                g2d.fillOval((int) meteorX - coreR, (int) meteorY - coreR, coreR * 2, coreR * 2);
            }

            double pillarTopY = camera.worldToScreenY(ring.y, 260.0);
            int pillarHalfW = (int) (radX * 0.36 * alpha);
            if (pillarHalfW > 2) {
                g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.30f));
                g2d.fillRect((int) sx - pillarHalfW, (int) pillarTopY, pillarHalfW * 2, (int) (syGround - pillarTopY));
            }
        }

        g2d.setStroke(new BasicStroke(1f));
    }
}
