package ui;

import combat.CombatEngine;
import core.Player;
import render.CombatVisualRenderer;
import render.Direction;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Bộ kết xuất Giao Diện Chỉ Số (HUD), Thanh 3 Kỹ Năng AOV và Vùng Chọn Kéo Thả trên PC.
 * Tách biệt khỏi GameCanvas theo nguyên lý Đơn Trách Nhiệm (Single Responsibility Principle - SRP).
 */
public final class DesktopHudRenderer {
    // Cache Flyweight cho Font và Hằng số Màu để chống Garbage Collection Churn trong Render Loop
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_WAVE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_SUBWAVE = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_FPS = new Font("Consolas", Font.BOLD, 12);
    private static final Font FONT_SKILL_TITLE = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_SKILL_DESC = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_GUIDE = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_GAMEOVER_TITLE = new Font("Segoe UI", Font.BOLD, 36);
    private static final Font FONT_GAMEOVER_PROMPT = new Font("Segoe UI", Font.BOLD, 18);

    private static final Color COLOR_PANEL_BG = new Color(18, 20, 26, 220);
    private static final Color COLOR_PANEL_BORDER_ONLINE = new Color(46, 204, 113);
    private static final Color COLOR_PANEL_BORDER_OFFLINE = new Color(60, 65, 80);
    private static final Color COLOR_HP_BG = new Color(40, 20, 20);
    private static final Color COLOR_HP_GREEN = new Color(46, 204, 113);
    private static final Color COLOR_HP_YELLOW = new Color(241, 196, 15);
    private static final Color COLOR_HP_RED = new Color(231, 76, 60);
    private static final Color COLOR_WHITE_80 = new Color(255, 255, 255, 80);
    private static final Color COLOR_GOLD = new Color(255, 215, 0);
    private static final Color COLOR_SKILL_BG = new Color(18, 22, 32, 230);
    private static final Color COLOR_SKILL_MASK = new Color(0, 0, 0, 170);
    private static final Color COLOR_SKILL_BORDER_CD = new Color(90, 95, 110);
    private static final Color COLOR_GUIDE_BG = new Color(15, 17, 23, 200);
    private static final Color COLOR_GUIDE_TEXT = new Color(200, 205, 220);

    private static final String[] SKILL_TITLES = {"[Q] ⚡ XUNG KÍCH", "[W] 🔥 XOÁY LỬA", "[E/R] ☄️ THIÊN PHẠT"};
    private static final String[] SKILL_DESCS = {"Lướt + Sát thương", "AoE + Hồi 18 HP", "ULTIMATE + Đẩy lùi"};
    private static final Color[] SKILL_COLORS = {new Color(0, 188, 212), new Color(255, 111, 0), new Color(170, 0, 255)};
    private static final Color[] SKILL_COLORS_BRIGHT = {
            new Color(0, 188, 212).brighter(),
            new Color(255, 111, 0).brighter(),
            new Color(170, 0, 255).brighter()
    };

    private DesktopHudRenderer() {}

    public static void renderHUD(Graphics2D g2d, int width, int height, Player player,
                                 String name, int hp, int maxHp, String stateName,
                                 int kills, int wave, int score, int monsterCount,
                                 int playerCount, int currentFps, boolean isOnline) {
        int hudX = 20;
        int hudY = 20;
        int barW = 220;
        int barH = 18;

        g2d.setColor(COLOR_PANEL_BG);
        g2d.fillRoundRect(hudX - 8, hudY - 8, barW + 110, 68, 12, 12);
        g2d.setColor(isOnline ? COLOR_PANEL_BORDER_ONLINE : COLOR_PANEL_BORDER_OFFLINE);
        g2d.drawRoundRect(hudX - 8, hudY - 8, barW + 110, 68, 12, 12);

        g2d.setFont(FONT_TITLE);
        g2d.setColor(Color.WHITE);
        g2d.drawString("⚔️ " + name, hudX, hudY + 12);

        Color stateBadgeColor = stateName.equals("ATTACKING") ? Color.ORANGE :
                (stateName.equals("DEAD") ? Color.RED : (stateName.equals("MOVING") ? Color.CYAN : Color.LIGHT_GRAY));
        g2d.setFont(FONT_BADGE);
        g2d.setColor(stateBadgeColor);
        g2d.drawString("[" + stateName + "]", hudX + 160, hudY + 12);

        g2d.setColor(COLOR_HP_BG);
        g2d.fillRoundRect(hudX, hudY + 22, barW, barH, 8, 8);

        float hpPct = Math.max(0f, Math.min(1f, (float) hp / maxHp));
        Color barColor = hpPct > 0.5f ? COLOR_HP_GREEN : (hpPct > 0.25f ? COLOR_HP_YELLOW : COLOR_HP_RED);
        g2d.setColor(barColor);
        g2d.fillRoundRect(hudX, hudY + 22, (int) (barW * hpPct), barH, 8, 8);

        g2d.setColor(COLOR_WHITE_80);
        g2d.drawRoundRect(hudX, hudY + 22, barW, barH, 8, 8);

        g2d.setFont(FONT_BADGE);
        g2d.setColor(Color.WHITE);
        g2d.drawString(hp + " / " + maxHp + " HP", hudX + barW / 2 - 30, hudY + 36);

        if (isOnline) {
            g2d.setFont(FONT_BADGE);
            g2d.setColor(COLOR_HP_GREEN);
            g2d.drawString("● ĐANG KẾT NỐI SERVER (LERP 60FPS)", hudX, hudY + 54);
        }

        // TOP-CENTER
        g2d.setFont(FONT_WAVE);
        g2d.setColor(COLOR_GOLD);
        String waveText = "ĐỢT " + wave + (isOnline ? " [PHÒNG ĐẤU MẠNG]" : "");
        int waveW = g2d.getFontMetrics().stringWidth(waveText);
        g2d.drawString(waveText, width / 2 - waveW / 2, 32);

        g2d.setFont(FONT_SUBWAVE);
        g2d.setColor(Color.LIGHT_GRAY);
        String subWave = isOnline ? ("Người chơi trong phòng: " + playerCount)
                : ("Điểm: " + score + "  |  Hạ gục: " + kills + "  |  Quái còn: " + monsterCount);
        int subW = g2d.getFontMetrics().stringWidth(subWave);
        g2d.drawString(subWave, width / 2 - subW / 2, 50);

        // TOP-RIGHT
        g2d.setFont(FONT_FPS);
        g2d.setColor(COLOR_HP_GREEN);
        g2d.drawString("🌲 ĐỒ HỌA: 2.5D NATURE ARENA", width - 215, 24);
        g2d.setColor(Color.GREEN);
        g2d.drawString(currentFps + " FPS (60Hz)", width - 215, 42);

        renderSkillBarAndGuide(g2d, width, height, player, isOnline);
    }

    private static void renderSkillBarAndGuide(Graphics2D g2d, int width, int height, Player p, boolean isOnline) {
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

            g2d.setColor(COLOR_SKILL_BG);
            g2d.fillRoundRect(bx, boxY, boxW, boxH, 10, 10);

            if (cd > 0.02) {
                float ratio = (float) Math.min(1.0, cd / maxCd);
                g2d.setColor(COLOR_SKILL_MASK);
                g2d.fillRoundRect(bx, boxY, (int) (boxW * ratio), boxH, 10, 10);
                g2d.setColor(COLOR_SKILL_BORDER_CD);
            } else {
                g2d.setColor(SKILL_COLORS[i]);
            }
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(bx, boxY, boxW, boxH, 10, 10);
            g2d.setStroke(new BasicStroke(1));

            g2d.setFont(FONT_SKILL_TITLE);
            g2d.setColor(cd > 0.02 ? COLOR_GUIDE_TEXT : Color.WHITE);
            g2d.drawString(SKILL_TITLES[i], bx + 10, boxY + 18);

            g2d.setFont(FONT_SKILL_DESC);
            if (cd > 0.02) {
                g2d.setColor(COLOR_GOLD);
                g2d.drawString(String.format("⏳ Hồi chiêu: %.1fs", cd), bx + 10, boxY + 35);
            } else {
                g2d.setColor(SKILL_COLORS_BRIGHT[i]);
                g2d.drawString(SKILL_DESCS[i], bx + 10, boxY + 35);
            }
        }

        String guide = isOnline ? "🌐 ĐẤU MẠNG: [Chuột phải]: Di chuyển/Tấn công | [S]: Dừng | [Q/W/E/R]: Kỹ năng theo chuột | [Space/A]: Đánh"
                : "🖱️ ĐIỀU KHIỂN LOL: [Chuột phải]: Di chuyển/Tấn công | [S]: Dừng | [Q/W/E/R]: Kỹ năng theo chuột | [Space/A]: Đánh";
        g2d.setFont(FONT_GUIDE);
        FontMetrics fm = g2d.getFontMetrics();
        int gW = fm.stringWidth(guide);

        g2d.setColor(COLOR_GUIDE_BG);
        g2d.fillRoundRect(width / 2 - gW / 2 - 12, height - 36, gW + 24, 26, 8, 8);
        g2d.setColor(COLOR_GUIDE_TEXT);
        g2d.drawString(guide, width / 2 - gW / 2, height - 19);
    }

    public static void renderSkillTargetIndicator(Graphics2D g2d, double sx, double sy,
                                                  Direction fallbackDir, int activeAimingSkill,
                                                  double aimX, double aimY) {
        CombatVisualRenderer.renderSkillTargetIndicator(g2d, sx, sy, fallbackDir, activeAimingSkill, aimX, aimY);
    }

    public static void renderGameOverOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, width, height);

        g2d.setFont(FONT_GAMEOVER_TITLE);
        g2d.setColor(COLOR_HP_RED);
        String title = "💀 CHIẾN BINH ĐÃ NGÃ XUỐNG!";
        int tw = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, width / 2 - tw / 2, height / 2 - 50);

        g2d.setFont(FONT_GAMEOVER_PROMPT);
        g2d.setColor(COLOR_HP_YELLOW);
        String prompt = "👉 NHẤN PHÍM [R] ĐỂ TÁI SINH VÀ PHỤC THÙ 👈";
        int pw = g2d.getFontMetrics().stringWidth(prompt);
        g2d.drawString(prompt, width / 2 - pw / 2, height / 2 + 30);
    }
}
