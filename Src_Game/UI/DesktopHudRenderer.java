package ui;

import combat.CombatEngine;
import core.Player;
import render.Camera2D;
import render.Direction;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Bộ kết xuất Giao Diện Chỉ Số (HUD), Thanh 3 Kỹ Năng AOV và Vùng Chọn Kéo Thả trên PC.
 * Tách biệt khỏi GameCanvas theo nguyên lý Đơn Trách Nhiệm (Single Responsibility Principle - SRP).
 */
public final class DesktopHudRenderer {
    private DesktopHudRenderer() {}

    public static void renderHUD(Graphics2D g2d, int width, int height, Player player,
                                 String name, int hp, int maxHp, String stateName,
                                 int kills, int wave, int score, int monsterCount,
                                 int playerCount, int currentFps, boolean isOnline, Camera2D camera) {
        int hudX = 20;
        int hudY = 20;
        int barW = 220;
        int barH = 18;

        g2d.setColor(new Color(18, 20, 26, 220));
        g2d.fillRoundRect(hudX - 8, hudY - 8, barW + 110, 68, 12, 12);
        g2d.setColor(isOnline ? new Color(46, 204, 113) : new Color(60, 65, 80));
        g2d.drawRoundRect(hudX - 8, hudY - 8, barW + 110, 68, 12, 12);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2d.setColor(Color.WHITE);
        g2d.drawString("⚔️ " + name, hudX, hudY + 12);

        Color stateBadgeColor = stateName.equals("ATTACKING") ? Color.ORANGE :
                (stateName.equals("DEAD") ? Color.RED : (stateName.equals("MOVING") ? Color.CYAN : Color.LIGHT_GRAY));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.setColor(stateBadgeColor);
        g2d.drawString("[" + stateName + "]", hudX + 160, hudY + 12);

        g2d.setColor(new Color(40, 20, 20));
        g2d.fillRoundRect(hudX, hudY + 22, barW, barH, 8, 8);

        float hpPct = Math.max(0f, Math.min(1f, (float) hp / maxHp));
        Color barColor = hpPct > 0.5f ? new Color(46, 204, 113) : (hpPct > 0.25f ? new Color(241, 196, 15) : new Color(231, 76, 60));
        g2d.setColor(barColor);
        g2d.fillRoundRect(hudX, hudY + 22, (int) (barW * hpPct), barH, 8, 8);

        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.drawRoundRect(hudX, hudY + 22, barW, barH, 8, 8);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.setColor(Color.WHITE);
        g2d.drawString(hp + " / " + maxHp + " HP", hudX + barW / 2 - 30, hudY + 36);

        if (isOnline) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2d.setColor(new Color(46, 204, 113));
            g2d.drawString("● ĐANG KẾT NỐI SERVER (LERP 60FPS)", hudX, hudY + 54);
        }

        // TOP-CENTER
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2d.setColor(new Color(255, 215, 0));
        String waveText = "ĐỢT " + wave + (isOnline ? " [PHÒNG ĐẤU MẠNG]" : "");
        int waveW = g2d.getFontMetrics().stringWidth(waveText);
        g2d.drawString(waveText, width / 2 - waveW / 2, 32);

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2d.setColor(Color.LIGHT_GRAY);
        String subWave = isOnline ? ("Người chơi trong phòng: " + playerCount)
                : ("Điểm: " + score + "  |  Hạ gục: " + kills + "  |  Quái còn: " + monsterCount);
        int subW = g2d.getFontMetrics().stringWidth(subWave);
        g2d.drawString(subWave, width / 2 - subW / 2, 50);

        // TOP-RIGHT: FPS & 3D Camera Mode Switcher Badge
        g2d.setFont(new Font("Consolas", Font.BOLD, 12));
        g2d.setColor(Color.GREEN);
        g2d.drawString(currentFps + " FPS (60Hz)", width - 140, 26);

        if (camera != null) {
            String camBadge = "[Phím V] " + camera.getCameraModeLabel();
            g2d.setColor(new Color(18, 26, 42, 225));
            g2d.fillRoundRect(width - 215, 36, 195, 28, 10, 10);
            g2d.setColor(new Color(0, 225, 255));
            g2d.drawRoundRect(width - 215, 36, 195, 28, 10, 10);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2d.setColor(new Color(0, 240, 255));
            g2d.drawString(camBadge, width - 203, 54);
        }

        renderSkillBarAndGuide(g2d, width, height, player, isOnline);
    }

    private static void renderSkillBarAndGuide(Graphics2D g2d, int width, int height, Player p, boolean isOnline) {
        String[] skillTitles = {"[1/Q] ⚡ XUNG KÍCH", "[2/E] 🔥 XOÁY LỬA", "[3/F] ☄️ THIÊN PHẠT"};
        String[] skillDescs = {"Lướt + Sát thương", "AoE + Hồi 18 HP", "ULTIMATE + Đẩy lùi"};
        Color[] skillColors = {new Color(0, 188, 212), new Color(255, 111, 0), new Color(170, 0, 255)};

        int boxW = 148;
        int boxH = 44;
        int gap = 14;
        int totalW = boxW * 3 + gap * 2;
        int startX = width / 2 - totalW / 2;
        int boxY = height - 88;

        for (int i = 0; i < 3; i++) {
            int skillId = i + 1;
            int bx = startX + i * (boxW + gap);
            double cd = p.getSkillCooldown(skillId);
            double maxCd = p.getSkillMaxCooldown(skillId);

            g2d.setColor(new Color(18, 22, 32, 230));
            g2d.fillRoundRect(bx, boxY, boxW, boxH, 10, 10);

            if (cd > 0.02) {
                float ratio = (float) Math.min(1.0, cd / maxCd);
                g2d.setColor(new Color(0, 0, 0, 170));
                g2d.fillRoundRect(bx, boxY, (int) (boxW * ratio), boxH, 10, 10);
                g2d.setColor(new Color(90, 95, 110));
            } else {
                g2d.setColor(skillColors[i]);
            }
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(bx, boxY, boxW, boxH, 10, 10);
            g2d.setStroke(new BasicStroke(1));

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2d.setColor(cd > 0.02 ? new Color(160, 165, 180) : Color.WHITE);
            g2d.drawString(skillTitles[i], bx + 10, boxY + 18);

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
            if (cd > 0.02) {
                g2d.setColor(new Color(255, 215, 64));
                g2d.drawString(String.format("⏳ Hồi chiêu: %.1fs", cd), bx + 10, boxY + 35);
            } else {
                g2d.setColor(skillColors[i].brighter());
                g2d.drawString(skillDescs[i], bx + 10, boxY + 35);
            }
        }

        String guide = isOnline ? "🌐 ĐẤU MẠNG 2.5D: [WASD]: Di chuyển | [Chuột Trái/SPACE]: Đánh | Giữ [Q/E/F] hoặc [1/2/3] để Chọn Vùng | [V]: Đổi góc 2.5D"
                : "⌨️ GÓC NHÌN MOBA 2.5D: [WASD]: Di chuyển | [Chuột Trái/SPACE]: Đánh | Giữ [Q/E/F] hoặc [1/2/3] Chọn Vùng | [V]: Nghiêng Camera 2D/2.5D";
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int gW = fm.stringWidth(guide);

        g2d.setColor(new Color(15, 17, 23, 200));
        g2d.fillRoundRect(width / 2 - gW / 2 - 12, height - 36, gW + 24, 26, 8, 8);
        g2d.setColor(new Color(200, 205, 220));
        g2d.drawString(guide, width / 2 - gW / 2, height - 19);
    }

    public static void renderSkillTargetIndicator(Graphics2D g2d, double sx, double sy,
                                                  Direction fallbackDir, int activeAimingSkill,
                                                  double aimX, double aimY, double pitchTilt) {
        double mag = Math.hypot(aimX, aimY);
        if (mag < 0.06 && fallbackDir != null) {
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

        if (activeAimingSkill == 1) {
            int rangeX = (int) CombatEngine.SKILL1_MAX_RANGE;
            int rangeY = (int) (rangeX * pitchTilt);
            double ux = (mag > 0.01) ? (aimX / mag) : 0.0;
            double uy = (mag > 0.01) ? (aimY / mag) : 1.0;
            double endX = sx + ux * rangeX;
            double endY = sy + uy * rangeY;

            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 25));
            g2d.fillOval((int) sx - rangeX, (int) sy - rangeY, rangeX * 2, rangeY * 2);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 130));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval((int) sx - rangeX, (int) sy - rangeY, rangeX * 2, rangeY * 2);

            double dx = endX - sx;
            double dy = endY - sy;
            double screenLen = Math.max(1.0, Math.hypot(dx, dy));
            double sux = dx / screenLen;
            double suy = dy / screenLen;
            double px = -suy;
            double py = sux;
            double halfW = 24.0;
            double headLen = 36.0;
            double bodyEndX = endX - sux * headLen;
            double bodyEndY = endY - suy * headLen;

            Path2D arrow = new Path2D.Double();
            arrow.moveTo(sx + px * halfW, sy + py * halfW);
            arrow.lineTo(bodyEndX + px * halfW, bodyEndY + py * halfW);
            arrow.lineTo(bodyEndX + px * (halfW * 1.65), bodyEndY + py * (halfW * 1.65));
            arrow.lineTo(endX, endY);
            arrow.lineTo(bodyEndX - px * (halfW * 1.65), bodyEndY - py * (halfW * 1.65));
            arrow.lineTo(bodyEndX - px * halfW, bodyEndY - py * halfW);
            arrow.lineTo(sx - px * halfW, sy - py * halfW);
            arrow.closePath();

            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 85));
            g2d.fill(arrow);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 230));
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.draw(arrow);
            g2d.setStroke(new BasicStroke(1f));
        } else {
            int maxRangeX = (int) (activeAimingSkill == 2 ? CombatEngine.SKILL2_MAX_RANGE : CombatEngine.SKILL3_MAX_RANGE);
            int maxRangeY = (int) (maxRangeX * pitchTilt);
            int aoeRx = (int) (activeAimingSkill == 2 ? CombatEngine.SKILL2_AOE_RADIUS : CombatEngine.SKILL3_AOE_RADIUS);
            int aoeRy = (int) (aoeRx * pitchTilt);
            int targetX = (int) (sx + aimX * maxRangeX);
            int targetY = (int) (sy + aimY * maxRangeY);

            // 1. Vòng giới hạn tầm chiêu Elip 3D dưới chân tướng
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 22));
            g2d.fillOval((int) sx - maxRangeX, (int) sy - maxRangeY, maxRangeX * 2, maxRangeY * 2);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 140));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval((int) sx - maxRangeX, (int) sy - maxRangeY, maxRangeX * 2, maxRangeY * 2);

            g2d.drawLine((int) sx, (int) sy, targetX, targetY);

            // 2. Vòng nổ AoE Elip 3D trên mặt đất
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 75));
            g2d.fillOval(targetX - aoeRx, targetY - aoeRy, aoeRx * 2, aoeRy * 2);
            g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 235));
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.drawOval(targetX - aoeRx, targetY - aoeRy, aoeRx * 2, aoeRy * 2);

            // 3. Cột Sáng Định Vị 3D (3D Vertical Targeting Light Cylinder) dựng thẳng đứng trên tâm vùng chọn
            if (pitchTilt < 0.95) {
                int beamH = 140;
                GradientPaint beamGrad = new GradientPaint(
                        targetX, targetY - beamH, new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 0),
                        targetX, targetY, new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 95)
                );
                g2d.setPaint(beamGrad);
                int bw = (int) (aoeRx * 0.35);
                g2d.fillRect(targetX - bw, targetY - beamH, bw * 2, beamH);

                // Vòng khóa mục tiêu lơ lửng trên không (Z = 45)
                int ringUpRx = (int) (aoeRx * 0.65);
                int ringUpRy = (int) (aoeRy * 0.65);
                g2d.setColor(new Color(255, 245, 180, 175));
                g2d.setStroke(new BasicStroke(1.8f));
                g2d.drawOval(targetX - ringUpRx, targetY - 45 - ringUpRy, ringUpRx * 2, ringUpRy * 2);
            }

            g2d.setColor(new Color(255, 245, 180, 200));
            g2d.setStroke(new BasicStroke(1.8f));
            int innerRx = (int) (aoeRx * 0.45);
            int innerRy = (int) (aoeRy * 0.45);
            g2d.drawOval(targetX - innerRx, targetY - innerRy, innerRx * 2, innerRy * 2);
            g2d.drawLine(targetX - 16, targetY, targetX + 16, targetY);
            g2d.drawLine(targetX, targetY - 12, targetX, targetY + 12);
            g2d.setStroke(new BasicStroke(1f));
        }
    }

    public static void renderGameOverOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, width, height);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
        g2d.setColor(new Color(231, 76, 60));
        String title = "💀 CHIẾN BINH ĐÃ NGÃ XUỐNG!";
        int tw = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, width / 2 - tw / 2, height / 2 - 50);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2d.setColor(new Color(241, 196, 15));
        String prompt = "👉 NHẤN PHÍM [R] ĐỂ TÁI SINH VÀ PHỤC THÙ 👈";
        int pw = g2d.getFontMetrics().stringWidth(prompt);
        g2d.drawString(prompt, width / 2 - pw / 2, height / 2 + 30);
    }
}
