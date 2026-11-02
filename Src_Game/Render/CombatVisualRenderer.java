package render;

import combat.CombatEffects;
import combat.CombatEngine;
import core.Vector2D;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Bộ kết xuất hiệu ứng hình ảnh chiến đấu 2.5D (Combat Visual Effects Renderer).
 * Áp dụng nguyên lý SRP (Single Responsibility Principle) & Data-driven Rendering:
 * Tách biệt hoàn toàn việc kết xuất đồ họa hiệu ứng kỹ năng AOV/LOL (thiên thạch, vòng lửa, vệt chém, số sát thương)
 * khỏi GameCanvas để đảm bảo tính module hóa và dễ bảo trì.
 */
public final class CombatVisualRenderer {

    private CombatVisualRenderer() {}

    public static void renderCombatVisuals(Graphics2D g2d, Camera2D camera, CombatEngine ce,
                                          int activeAimingSkill, Vector2D heroPos, Direction heroFacing,
                                          double currentAimX, double currentAimY) {
        if (ce == null || camera == null) return;

        // 1. Kết xuất vòng định vị ngắm chiêu (Skill Indicator)
        if (activeAimingSkill >= 1 && activeAimingSkill <= 3 && heroPos != null) {
            renderSkillTargetIndicator(
                    g2d,
                    camera.worldToScreenX(heroPos.getX()),
                    camera.worldToScreenY(heroPos.getY()),
                    heroFacing,
                    activeAimingSkill,
                    currentAimX,
                    currentAimY
            );
        }

        double ratio = 0.58; // Tỷ lệ phối cảnh 2.5D mặt đất (Ground Plane Compression)

        // 2. Kết xuất các vòng nổ kỹ năng 2.5D (Skill Rings)
        for (CombatEffects.SkillRingEffect ring : ce.getSkillRings()) {
            double sx = camera.worldToScreenX(ring.x);
            double sy = camera.worldToScreenY(ring.y);
            float alpha = (float) Math.max(0f, ring.lifeTime / ring.maxLifeTime);
            double progress = 1.0 - alpha;
            int c = ring.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            int radX = (int) (ring.maxRadius * (0.25 + 0.75 * (1.0 - alpha)));
            int radY = (int) (radX * ratio);

            if (ring.skillType == 3) {
                // --- CHIÊU 3: ☄️ THIÊN PHẠT 2.5D (METEOR DESCENT FROM 3D SKY) ---
                if (progress < 0.75) {
                    double flightProg = progress / 0.75;
                    // Tọa độ thiên thạch rơi xiên từ góc cao bầu trời (3D Altitude) xuống tâm sàn đấu
                    double meteorX = sx + (1.0 - flightProg) * 150.0;
                    double meteorY = sy - (1.0 - flightProg) * 270.0;

                    // Vệt khói lửa kéo dài trên không trung (Fiery Tail)
                    g2d.setColor(new Color(255, 69, 0, (int) (alpha * 180)));
                    g2d.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine((int) meteorX, (int) meteorY, (int) (meteorX + 35), (int) (meteorY - 60));
                    g2d.setColor(new Color(255, 215, 0, (int) (alpha * 240)));
                    g2d.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine((int) meteorX, (int) meteorY, (int) (meteorX + 20), (int) (meteorY - 35));

                    // Quả cầu thiên thạch rực lửa
                    g2d.setColor(new Color(255, 100, 0, 240));
                    g2d.fillOval((int) meteorX - 16, (int) meteorY - 16, 32, 32);
                    g2d.setColor(new Color(255, 255, 230, 255));
                    g2d.fillOval((int) meteorX - 8, (int) meteorY - 8, 16, 16);

                    // Điểm ngắm cảnh báo hình Elip 2.5D thu hẹp dần trên mặt đất
                    int targetWarnRx = (int) (ring.maxRadius * (1.0 - flightProg * 0.4));
                    int targetWarnRy = (int) (targetWarnRx * ratio);
                    g2d.setColor(new Color(255, 0, 80, 50));
                    g2d.fillOval((int) sx - targetWarnRx, (int) sy - targetWarnRy, targetWarnRx * 2, targetWarnRy * 2);
                    g2d.setColor(new Color(255, 215, 0, 200));
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawOval((int) sx - targetWarnRx, (int) sy - targetWarnRy, targetWarnRx * 2, targetWarnRy * 2);
                } else {
                    // KHOẢNH KHẮC THIÊN THẠCH CHẠM ĐẤT: NỔ SÓNG XUNG KÍCH 2.5D & CỘT LỬA DỌC Z
                    // 1. Cột lửa bốc lên trời (Vertical Shockwave along Z-axis)
                    g2d.setPaint(new GradientPaint(
                            (float) sx, (float) (sy - 120), new Color(255, 255, 255, 0),
                            (float) sx, (float) sy, new Color(255, 140, 0, (int) (alpha * 220))
                    ));
                    g2d.fillRect((int) sx - 24, (int) sy - 120, 48, 120);

                    // 2. Vành sóng nổ 2.5D quét phẳng mặt đất
                    g2d.setColor(new Color(255, 120, 0, (int) (alpha * 80)));
                    g2d.fillOval((int) sx - radX, (int) sy - radY, radX * 2, radY * 2);
                    g2d.setColor(new Color(255, 230, 100, (int) (alpha * 255)));
                    g2d.setStroke(new BasicStroke(6f));
                    g2d.drawOval((int) sx - radX, (int) sy - radY, radX * 2, radY * 2);
                }
            } else if (ring.skillType == 2) {
                // --- CHIÊU 2: 🔥 XOÁY LỬA 2.5D (ROTATING VORTEX WITH 3D HEIGHT) ---
                // Sóng lửa elip 2.5D bám mặt đất
                g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.35f));
                g2d.fillOval((int) sx - radX, (int) sy - radY, radX * 2, radY * 2);

                g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.95f));
                g2d.setStroke(new BasicStroke(5f));
                g2d.drawOval((int) sx - radX, (int) sy - radY, radX * 2, radY * 2);

                // Lưỡi lửa xoắn ốc bốc lên theo chiều cao Z (Flame Spirals)
                int flameHeight = (int) (35 * alpha);
                g2d.setColor(new Color(255, 220, 80, (int) (alpha * 200)));
                g2d.drawOval((int) sx - radX / 2, (int) (sy - flameHeight - radY / 2), radX, radY);
            } else {
                // --- CHIÊU 1: ⚡ XUNG KÍCH 2.5D ---
                g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.28f));
                g2d.fillOval((int) sx - radX, (int) sy - radY, radX * 2, radY * 2);

                g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.9f));
                g2d.setStroke(new BasicStroke(5f));
                g2d.drawOval((int) sx - radX, (int) sy - radY, radX * 2, radY * 2);
            }
            g2d.setStroke(new BasicStroke(1));
        }

        // 3. Vệt chém kiếm 2.5D (Perspective Curved Slash Arcs)
        for (CombatEffects.SlashEffect slash : ce.getSlashEffects()) {
            double sx = camera.worldToScreenX(slash.x);
            double sy = camera.worldToScreenY(slash.y);
            float alpha = (float) Math.max(0f, slash.lifeTime / slash.maxLifeTime);

            g2d.setColor(new Color(1f, 0.92f, 0.5f, alpha * 0.9f));
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arcAngle = slash.direction.getRowOffset() * 90;
            g2d.drawArc((int) sx - 28, (int) (sy - 28 * ratio), 56, (int) (56 * ratio), -arcAngle - 45, 90);
            g2d.setStroke(new BasicStroke(1));
        }

        // 4. Hạt tia lửa va chạm 2.5D (Particles with Bounce Trajectory)
        for (CombatEffects.HitParticle p : ce.getParticles()) {
            double sx = camera.worldToScreenX(p.x);
            double sy = camera.worldToScreenY(p.y);
            float alpha = (float) Math.max(0f, p.lifeTime / p.maxLifeTime);
            int c = p.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            // Bóng nhỏ của hạt trên mặt sàn
            g2d.setColor(new Color(0, 0, 0, (int) (alpha * 60)));
            g2d.fillOval((int) sx - 1, (int) sy + 3, (int) p.size, (int) (p.size * ratio));

            // Hạt tia lửa phát sáng
            g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha));
            g2d.fillOval((int) sx, (int) sy, (int) p.size, (int) p.size);
        }

        // 5. Chữ số sát thương nảy lên 2.5D dạng 3D Pop-up
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 15));
        for (CombatEffects.FloatingText ft : ce.getFloatingTexts()) {
            double sx = camera.worldToScreenX(ft.x);
            double sy = camera.worldToScreenY(ft.y);
            float alpha = (float) Math.max(0f, ft.lifeTime / ft.maxLifeTime);
            int c = ft.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            // Bóng chữ 3D (Text Drop Shadow)
            g2d.setColor(new Color(0, 0, 0, (int) (alpha * 200)));
            g2d.drawString(ft.text, (int) sx + 2, (int) sy + 2);

            // Chữ chính sáng rực
            g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha));
            g2d.drawString(ft.text, (int) sx, (int) sy);
        }
    }

    /**
     * Dựng vòng định vị ngắm chiêu (Skill Indicator) theo chuẩn MOBA / Liên Minh / Liên Quân.
     */
    public static void renderSkillTargetIndicator(Graphics2D g2d, double sx, double sy,
                                                  Direction fallbackDir, int activeAimingSkill,
                                                  double aimX, double aimY) {
        if (activeAimingSkill < 1 || activeAimingSkill > 3) return;

        double mag = Math.hypot(aimX, aimY);
        if (mag < 0.05 && fallbackDir != null) {
            aimX = fallbackDir.getDirX() * (activeAimingSkill == 1 ? 1.0 : 0.55);
            aimY = fallbackDir.getDirY() * (activeAimingSkill == 1 ? 1.0 : 0.55);
            mag = Math.hypot(aimX, aimY);
        }
        if (mag > 1.0) {
            aimX /= mag;
            aimY /= mag;
            mag = 1.0;
        }

        Color mainColor = (activeAimingSkill == 1) ? new Color(0, 229, 255)
                : ((activeAimingSkill == 2) ? new Color(255, 145, 0) : new Color(224, 64, 251));

        double ratio = 0.58; // Tỷ lệ phối cảnh 2.5D mặt đất (Ground Plane Compression)

        if (activeAimingSkill == 1) {
            int range = (int) CombatEngine.SKILL1_MAX_RANGE;
            int rangeY = (int) (range * ratio);
            double ux = (mag > 0.01) ? (aimX / mag) : 0.0;
            double uy = (mag > 0.01) ? (aimY / mag) : 1.0;
            double endX = sx + ux * range;
            double endY = sy + uy * range * ratio;

            // Vành elip 2.5D giới hạn tầm lướt quanh chân tướng
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 28));
            g2d.fillOval((int) sx - range, (int) sy - rangeY, range * 2, rangeY * 2);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 140));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval((int) sx - range, (int) sy - rangeY, range * 2, rangeY * 2);

            double px = -uy;
            double py = ux * ratio;
            double halfW = 24.0;
            double headLen = 34.0;
            double bodyEndX = endX - ux * headLen;
            double bodyEndY = endY - uy * headLen * ratio;

            Path2D arrow = new Path2D.Double();
            arrow.moveTo(sx + px * halfW, sy + py * halfW);
            arrow.lineTo(bodyEndX + px * halfW, bodyEndY + py * halfW);
            arrow.lineTo(bodyEndX + px * (halfW * 1.65), bodyEndY + py * (halfW * 1.65));
            arrow.lineTo(endX, endY);
            arrow.lineTo(bodyEndX - px * (halfW * 1.65), bodyEndY - py * (halfW * 1.65));
            arrow.lineTo(bodyEndX - px * halfW, bodyEndY - py * halfW);
            arrow.lineTo(sx - px * halfW, sy - py * halfW);
            arrow.closePath();

            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 90));
            g2d.fill(arrow);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 235));
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.draw(arrow);
            g2d.setStroke(new BasicStroke(1f));
        } else {
            int maxRange = (int) (activeAimingSkill == 2 ? CombatEngine.SKILL2_MAX_RANGE : CombatEngine.SKILL3_MAX_RANGE);
            int maxRangeY = (int) (maxRange * ratio);
            int aoeRadius = (int) (activeAimingSkill == 2 ? CombatEngine.SKILL2_AOE_RADIUS : CombatEngine.SKILL3_AOE_RADIUS);
            int aoeRadiusY = (int) (aoeRadius * ratio);
            int targetX = (int) (sx + aimX * maxRange);
            int targetY = (int) (sy + aimY * maxRange * ratio);

            // 1. Vành elip 2.5D tầm thi triển quanh chân
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 22));
            g2d.fillOval((int) sx - maxRange, (int) sy - maxRangeY, maxRange * 2, maxRangeY * 2);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 140));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval((int) sx - maxRange, (int) sy - maxRangeY, maxRange * 2, maxRangeY * 2);

            // Đường dẫn tia ngắm
            g2d.drawLine((int) sx, (int) sy, targetX, targetY);

            // 2. Vùng nổ AoE dạng hình Elip 2.5D bám mặt đất (2.5D Ground Decal)
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 75));
            g2d.fillOval(targetX - aoeRadius, targetY - aoeRadiusY, aoeRadius * 2, aoeRadiusY * 2);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 240));
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.drawOval(targetX - aoeRadius, targetY - aoeRadiusY, aoeRadius * 2, aoeRadiusY * 2);

            // 3. Cột sáng chỉ điểm 3D (Vertical Light Beam along Z-axis)
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 45));
            g2d.fillRect(targetX - 8, targetY - 60, 16, 60);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 160));
            g2d.drawLine(targetX, targetY, targetX, targetY - 60);

            // Vòng tâm elip 2.5D bên trong
            g2d.setColor(new Color(255, 245, 180, 200));
            g2d.setStroke(new BasicStroke(1.8f));
            int innerRx = (int) (aoeRadius * 0.45);
            int innerRy = (int) (innerRx * ratio);
            g2d.drawOval(targetX - innerRx, targetY - innerRy, innerRx * 2, innerRy * 2);
            g2d.drawLine(targetX - 16, targetY, targetX + 16, targetY);
            g2d.drawLine(targetX, targetY - 10, targetX, targetY + 10);
            g2d.setStroke(new BasicStroke(1f));
        }
    }
}
