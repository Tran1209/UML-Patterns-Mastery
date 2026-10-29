package render;

import combat.CombatEffects;
import core.GameWorld;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Bộ kết xuất Không Gian Đấu Trường 3D Phối Cảnh (3D Perspective Arena & Volumetric VFX Renderer).
 * Dựng hình:
 * - Bầu trời Tinh vân & Đế Đấu Trường Nổi 3D (3D Extruded Floating Citadel Slab).
 * - Lưới mặt đất hội tụ phối cảnh 3D & Vòng tròn Triệu hồi Trung tâm.
 * - 4 Trụ Tháp Cổ Đại 3D (3D Obelisk Towers) có khối đa diện chiều cao Z, đổ bóng nghiêng và Pha lê năng lượng lơ lửng.
 * - Hiệu ứng Kỹ năng 3D: Lốc xoáy lửa nhiều tầng Z (Skill 2) và Thiên thạch rơi từ bầu trời Z=420 xuống đất (Skill 3).
 */
public final class Arena3DSceneRenderer {

    public static final double[][] OBELISK_POSITIONS = {
            {320.0, 280.0},
            {1280.0, 280.0},
            {320.0, 920.0},
            {1280.0, 920.0}
    };

    private Arena3DSceneRenderer() {}

    public static void renderArena3DFloorAndSky(Graphics2D g2d, Camera2D camera, int width, int height) {
        // 1. Bầu trời đêm Tinh Vân 3D (3D Atmospheric Nebula Sky Gradient)
        GradientPaint skyGrad = new GradientPaint(
                0, 0, new Color(8, 12, 26),
                0, height, new Color(18, 24, 38)
        );
        g2d.setPaint(skyGrad);
        g2d.fillRect(0, 0, width, height);

        double W = GameWorld.WORLD_WIDTH;
        double H = GameWorld.WORLD_HEIGHT;

        // Tọa độ 4 góc mặt sàn đấu trường (wz = 0)
        double tlX = camera.worldToScreenX(0, 0);
        double tlY = camera.worldToScreenY(0, 0);
        double trX = camera.worldToScreenX(W, 0);
        double trY = camera.worldToScreenY(0, 0);
        double brX = camera.worldToScreenX(W, H);
        double brY = camera.worldToScreenY(H, 0);
        double blX = camera.worldToScreenX(0, H);
        double blY = camera.worldToScreenY(H, 0);

        // 2. Vẽ Khối Đá Đế Đấu Trường 3D (3D Extruded Floating Cliff Base ở độ sâu wz = -85)
        if (camera.is3DMode()) {
            double cliffDepth = -85.0;
            double blBottomY = camera.worldToScreenY(H, cliffDepth);
            double brBottomY = camera.worldToScreenY(H, cliffDepth);
            double tlBottomY = camera.worldToScreenY(0, cliffDepth);
            double trBottomY = camera.worldToScreenY(0, cliffDepth);

            // Vách đá mặt tiền (Front Cliff Wall)
            Path2D frontCliff = new Path2D.Double();
            frontCliff.moveTo(blX, blY);
            frontCliff.lineTo(brX, brY);
            frontCliff.lineTo(brX, brBottomY);
            frontCliff.lineTo(blX, blBottomY);
            frontCliff.closePath();
            GradientPaint cliffPaint = new GradientPaint(
                    0, (float) blY, new Color(28, 36, 52),
                    0, (float) blBottomY, new Color(10, 14, 24)
            );
            g2d.setPaint(cliffPaint);
            g2d.fill(frontCliff);

            // Các gờ cột đá 3D mặt tiền
            g2d.setColor(new Color(55, 70, 95, 140));
            g2d.setStroke(new BasicStroke(2f));
            for (double gx = 0; gx <= W; gx += 160) {
                double sxTop = camera.worldToScreenX(gx, H);
                g2d.drawLine((int) sxTop, (int) blY, (int) sxTop, (int) blBottomY);
            }

            // Vách đá bên trái & bên phải nếu nhìn thấy
            Path2D leftCliff = new Path2D.Double();
            leftCliff.moveTo(tlX, tlY);
            leftCliff.lineTo(blX, blY);
            leftCliff.lineTo(blX, blBottomY);
            leftCliff.lineTo(tlX, tlBottomY);
            leftCliff.closePath();
            g2d.setColor(new Color(20, 26, 38));
            g2d.fill(leftCliff);

            Path2D rightCliff = new Path2D.Double();
            rightCliff.moveTo(trX, trY);
            rightCliff.lineTo(brX, brY);
            rightCliff.lineTo(brX, brBottomY);
            rightCliff.lineTo(trX, trBottomY);
            rightCliff.closePath();
            g2d.setColor(new Color(22, 28, 42));
            g2d.fill(rightCliff);
        }

        // 3. Mặt Sàn Đấu Trường Hình Thang Phối Cảnh 3D (3D Perspective Trapezoid Floor)
        Path2D floorPoly = new Path2D.Double();
        floorPoly.moveTo(tlX, tlY);
        floorPoly.lineTo(trX, trY);
        floorPoly.lineTo(brX, brY);
        floorPoly.lineTo(blX, blY);
        floorPoly.closePath();

        GradientPaint floorGrad = new GradientPaint(
                0, (float) tlY, new Color(28, 34, 46),
                0, (float) brY, new Color(44, 52, 68)
        );
        g2d.setPaint(floorGrad);
        g2d.fill(floorPoly);

        // 4. Lưới Ô Gạch Đá Hội Tụ Phối Cảnh 3D (Converging 3D Perspective Grid)
        int tileSize = 80;
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.setColor(new Color(65, 78, 102, 135));
        for (double wx = 0; wx <= W; wx += tileSize) {
            int x1 = (int) camera.worldToScreenX(wx, 0);
            int y1 = (int) camera.worldToScreenY(0, 0);
            int x2 = (int) camera.worldToScreenX(wx, H);
            int y2 = (int) camera.worldToScreenY(H, 0);
            g2d.drawLine(x1, y1, x2, y2);
        }
        for (double wy = 0; wy <= H; wy += tileSize) {
            int x1 = (int) camera.worldToScreenX(0, wy);
            int y1 = (int) camera.worldToScreenY(wy, 0);
            int x2 = (int) camera.worldToScreenX(W, wy);
            int y2 = (int) camera.worldToScreenY(wy, 0);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // 5. Vòng Tròn Cổ Ngữ Triệu Hồi 3D ở Trung Tâm Bản Đồ (800, 600)
        double centerX = camera.worldToScreenX(W / 2.0, H / 2.0);
        double centerY = camera.worldToScreenY(H / 2.0, 0);
        double centerScale = camera.getPerspectiveScale(H / 2.0);
        double pitch = camera.getPitchTilt();
        int runeRx = (int) (150 * centerScale);
        int runeRy = (int) (150 * centerScale * pitch);

        g2d.setColor(new Color(0, 210, 255, 28));
        g2d.fillOval((int) centerX - runeRx, (int) centerY - runeRy, runeRx * 2, runeRy * 2);
        g2d.setColor(new Color(0, 210, 255, 95));
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawOval((int) centerX - runeRx, (int) centerY - runeRy, runeRx * 2, runeRy * 2);
        g2d.drawOval((int) centerX - (int) (runeRx * 0.68), (int) centerY - (int) (runeRy * 0.68),
                (int) (runeRx * 1.36), (int) (runeRy * 1.36));

        // 6. Tường Thành Bảo Vệ Hậu Cảnh 3D (3D Extruded Back Wall tại wy = 0, wz = 0..46)
        if (camera.is3DMode()) {
            double wallH = 46.0;
            double tlTopY = camera.worldToScreenY(0, wallH);
            double trTopY = camera.worldToScreenY(0, wallH);

            Path2D backWall = new Path2D.Double();
            backWall.moveTo(tlX, tlY);
            backWall.lineTo(trX, trY);
            backWall.lineTo(trX, trTopY);
            backWall.lineTo(tlX, tlTopY);
            backWall.closePath();

            g2d.setColor(new Color(45, 56, 78, 220));
            g2d.fill(backWall);
            g2d.setColor(new Color(0, 220, 255, 180));
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.draw(backWall);
        }

        // 7. Viền Hào Quang Đấu Trường 3D
        g2d.setColor(new Color(255, 185, 45, 220));
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.draw(floorPoly);
        g2d.setStroke(new BasicStroke(1f));
    }

    /**
     * Dựng khối Trụ Đá Cổ Đại 3D (3D Extruded Obelisk Tower) có mặt bên, mặt đỉnh ở độ cao Z và Pha lê năng lượng xoay.
     */
    public static void render3DObeliskTower(Graphics2D g2d, Camera2D camera, double wx, double wy, long timeMs) {
        double scale = camera.getPerspectiveScale(wy);
        double pitch = camera.getPitchTilt();
        double baseX = camera.worldToScreenX(wx, wy);
        double baseY = camera.worldToScreenY(wy, 0);

        double halfW = 28.0 * scale;
        double halfD = 22.0 * scale * pitch;
        double towerHeightZ = 92.0;
        double topY = camera.worldToScreenY(wy, towerHeightZ);

        // 1. Bóng đổ nghiêng 3D dưới chân trụ
        g2d.setColor(new Color(0, 0, 0, 95));
        g2d.fillOval((int) (baseX - halfW * 1.15 + 14 * scale), (int) (baseY - halfD * 0.9 + 6 * scale),
                (int) (halfW * 2.5), (int) (halfD * 2.0));

        // 2. Bệ đá năng lượng dưới chân (wz = 0)
        g2d.setColor(new Color(0, 210, 255, 55));
        g2d.fillOval((int) (baseX - halfW * 1.35), (int) (baseY - halfD * 1.35),
                (int) (halfW * 2.7), (int) (halfD * 2.7));

        if (!camera.is3DMode()) {
            // Chế độ 2D cổ điển: chỉ vẽ mặt cắt ngang
            g2d.setColor(new Color(65, 80, 105));
            g2d.fillRoundRect((int) (baseX - halfW), (int) (baseY - halfW), (int) (halfW * 2), (int) (halfW * 2), 10, 10);
            g2d.setColor(new Color(0, 229, 255));
            g2d.drawRoundRect((int) (baseX - halfW), (int) (baseY - halfW), (int) (halfW * 2), (int) (halfW * 2), 10, 10);
            return;
        }

        // 3. Mặt bên trái Trụ 3D (Shaded Left Face)
        Path2D leftFace = new Path2D.Double();
        leftFace.moveTo(baseX - halfW, baseY);
        leftFace.lineTo(baseX, baseY + halfD);
        leftFace.lineTo(baseX, topY + halfD * 0.75);
        leftFace.lineTo(baseX - halfW * 0.78, topY);
        leftFace.closePath();
        g2d.setColor(new Color(42, 54, 76));
        g2d.fill(leftFace);

        // 4. Mặt bên phải Trụ 3D (Lit Right Face)
        Path2D rightFace = new Path2D.Double();
        rightFace.moveTo(baseX, baseY + halfD);
        rightFace.lineTo(baseX + halfW, baseY);
        rightFace.lineTo(baseX + halfW * 0.78, topY);
        rightFace.lineTo(baseX, topY + halfD * 0.75);
        rightFace.closePath();
        g2d.setColor(new Color(64, 82, 112));
        g2d.fill(rightFace);

        // 5. Mặt đỉnh Trụ 3D ở độ cao wz = 92 (Top Roof Diamond)
        double topHalfW = halfW * 0.78;
        double topHalfD = halfD * 0.75;
        Path2D topFace = new Path2D.Double();
        topFace.moveTo(baseX, topY - topHalfD);
        topFace.lineTo(baseX + topHalfW, topY);
        topFace.lineTo(baseX, topY + topHalfD);
        topFace.lineTo(baseX - topHalfW, topY);
        topFace.closePath();
        g2d.setColor(new Color(90, 115, 150));
        g2d.fill(topFace);

        // Viền phát sáng cạnh 3D
        g2d.setColor(new Color(0, 225, 255, 175));
        g2d.setStroke(new BasicStroke((float) (1.8 * scale)));
        g2d.draw(leftFace);
        g2d.draw(rightFace);
        g2d.draw(topFace);

        // 6. Khối Pha Lê Năng Lượng 3D Lơ Lửng trên đỉnh trụ (wz = 122 + bob)
        double bobZ = Math.sin(timeMs * 0.004 + wx) * 8.0;
        double crystalY = camera.worldToScreenY(wy, towerHeightZ + 28.0 + bobZ);
        double cw = 11.0 * scale;
        double ch = 18.0 * scale;

        Path2D crystal = new Path2D.Double();
        crystal.moveTo(baseX, crystalY - ch);
        crystal.lineTo(baseX + cw, crystalY);
        crystal.lineTo(baseX, crystalY + ch * 0.75);
        crystal.lineTo(baseX - cw, crystalY);
        crystal.closePath();

        g2d.setColor(new Color(0, 240, 255, 210));
        g2d.fill(crystal);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(crystal);
        g2d.setStroke(new BasicStroke(1f));
    }

    /**
     * Vẽ Hiệu Ứng Kỹ Năng Không Gian 3D (Lốc xoáy lửa đa tầng Z và Thiên thạch rơi từ bầu trời Z=420).
     */
    public static void render3DSkillEffect(Graphics2D g2d, Camera2D camera, CombatEffects.SkillRingEffect ring) {
        double sx = camera.worldToScreenX(ring.x, ring.y);
        double syGround = camera.worldToScreenY(ring.y, 0);
        double scale = camera.getPerspectiveScale(ring.y);
        double pitch = camera.getPitchTilt();

        float alpha = (float) Math.max(0f, Math.min(1f, ring.lifeTime / ring.maxLifeTime));
        double progress = Math.max(0.0, Math.min(1.0, 1.0 - alpha));

        int c = ring.color;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        int radX = (int) (ring.maxRadius * (0.25 + 0.75 * progress) * scale);
        int radY = (int) (radX * pitch);

        // 1. Vòng sóng xung kích mặt đất theo Elip phối cảnh 3D (wz = 0)
        g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.24f));
        g2d.fillOval((int) sx - radX, (int) syGround - radY, radX * 2, radY * 2);

        g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.92f));
        g2d.setStroke(new BasicStroke((float) (5.5 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawOval((int) sx - radX, (int) syGround - radY, radX * 2, radY * 2);

        if (!camera.is3DMode()) {
            g2d.setStroke(new BasicStroke(1f));
            return;
        }

        if (ring.skillType == 1) {
            // CHIÊU 1 (⚡ XUNG KÍCH 3D): Sóng năng lượng băng giá 2 tầng độ cao Z
            double syUpper = camera.worldToScreenY(ring.y, 26.0);
            int upRx = (int) (radX * 0.78);
            int upRy = (int) (radY * 0.78);
            g2d.setColor(new Color(0.5f, 0.95f, 1.0f, alpha * 0.65f));
            g2d.setStroke(new BasicStroke((float) (3.0 * scale)));
            g2d.drawOval((int) sx - upRx, (int) syUpper - upRy, upRx * 2, upRy * 2);
        } else if (ring.skillType == 2) {
            // CHIÊU 2 (🔥 VÒNG XOÁY LỬA 3D): Cột Lốc Xoáy Lửa 3D (5 vòng lửa xếp chồng từ Z=0 lên Z=115)
            for (int layer = 1; layer <= 4; layer++) {
                double zHeight = layer * 26.0 * (0.5 + 0.5 * progress);
                double syLayer = camera.worldToScreenY(ring.y, zHeight);
                double taper = 1.0 - layer * 0.12;
                int lRx = (int) (radX * taper);
                int lRy = (int) (radY * taper);
                double swirlX = Math.cos(progress * 10.0 + layer * 1.4) * 8.0 * scale;

                g2d.setColor(new Color(1.0f, Math.min(1f, (g + layer * 25) / 255f), 0.1f, alpha * (0.75f - layer * 0.12f)));
                g2d.setStroke(new BasicStroke((float) ((4.5 - layer * 0.6) * scale)));
                g2d.drawOval((int) (sx + swirlX) - lRx, (int) syLayer - lRy, lRx * 2, lRy * 2);
            }
        } else if (ring.skillType == 3) {
            // CHIÊU 3 (☄️ THIÊN PHẠT ULTIMATE 3D): Thiên Thạch Lửa 3D rơi từ bầu trời (Z=420 -> Z=0) + Cột sáng Thiên Phạt
            if (progress < 0.55) {
                double fallRatio = 1.0 - (progress / 0.55); // 1.0 (trên trời) -> 0.0 (chạm đất)
                double meteorZ = 420.0 * fallRatio;
                double meteorX = sx - 110.0 * fallRatio * scale;
                double meteorY = camera.worldToScreenY(ring.y, meteorZ);
                int mRadius = (int) (36.0 * scale);

                // Đuôi lửa thiên thạch 3D trên không trung
                g2d.setColor(new Color(1.0f, 0.45f, 0.0f, 0.65f));
                g2d.setStroke(new BasicStroke((float) (18.0 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine((int) meteorX, (int) meteorY,
                        (int) (meteorX - 65 * scale), (int) (meteorY - 120 * scale));

                // Quả cầu Thiên Thạch 3D
                g2d.setColor(new Color(255, 80, 20, 240));
                g2d.fillOval((int) meteorX - mRadius, (int) meteorY - mRadius, mRadius * 2, mRadius * 2);
                g2d.setColor(new Color(255, 240, 150, 250));
                int coreR = (int) (mRadius * 0.58);
                g2d.fillOval((int) meteorX - coreR, (int) meteorY - coreR, coreR * 2, coreR * 2);
            }

            // Cột Ánh Sáng Thiên Phạt 3D (3D Celestial Pillar of Light bắn từ mặt đất Z=0 lên Z=280)
            double pillarTopY = camera.worldToScreenY(ring.y, 280.0);
            int pillarHalfW = (int) (radX * 0.38 * alpha);
            if (pillarHalfW > 2) {
                g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.32f));
                g2d.fillRect((int) sx - pillarHalfW, (int) pillarTopY, pillarHalfW * 2, (int) (syGround - pillarTopY));
            }

            // Vòm Năng Lượng Nổ 3D (3D Shockwave Dome)
            g2d.setColor(new Color(1.0f, 0.85f, 1.0f, alpha * 0.75f));
            g2d.setStroke(new BasicStroke((float) (3.5 * scale)));
            g2d.drawArc((int) sx - radX, (int) syGround - (int) (radX * 0.85), radX * 2, (int) (radX * 1.7), 0, 180);
        }

        g2d.setStroke(new BasicStroke(1f));
    }
}
