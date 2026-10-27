package ui;

import combat.CombatEngine;
import combat.Hitbox;
import core.GameWorld;
import core.Monster;
import core.Player;
import core.Vector2D;
import render.Camera2D;
import render.Direction;
import render.LpcCanvasRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Bảng điều khiển kết xuất đồ họa Canvas 60 FPS tích hợp điều khiển bàn phím và hệ thống HUD.
 * Tuân thủ nguyên lý Observer Pattern và phân tầng Presentation Layer sạch sẽ.
 */
public class GameCanvas extends JPanel {
    private final GameWorld world;
    private final Camera2D camera;
    private final Set<Integer> pressedKeys = new HashSet<>();

    private boolean showDebugOverlay = false;
    private int currentFps = 60;
    private int frameCounter = 0;
    private long lastFpsUpdateTime = System.currentTimeMillis();

    public GameCanvas(GameWorld world, int width, int height) {
        this.world = world;
        this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);

        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(25, 27, 34));
        setFocusable(true);
        setDoubleBuffered(true);

        setupKeyBindings();
    }

    private void setupKeyBindings() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                pressedKeys.add(e.getKeyCode());

                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_J) {
                    world.handlePlayerAttack();
                } else if (e.getKeyCode() == KeyEvent.VK_R) {
                    if (!world.getPlayer().isAlive()) {
                        world.restartGame();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_F1) {
                    showDebugOverlay = !showDebugOverlay;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });
    }

    /**
     * Thu thập phím bấm từ người chơi và điều khiển vận tốc của nhân vật.
     */
    public void processInput() {
        Player player = world.getPlayer();
        if (!player.isAlive()) return;

        double vx = 0;
        double vy = 0;

        if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) vy -= 1.0;
        if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) vy += 1.0;
        if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT)) vx -= 1.0;
        if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) vx += 1.0;

        if (vx != 0 && vy != 0) {
            // Chuẩn hóa vector di chuyển chéo để tốc độ luôn không đổi (1 / sqrt(2))
            double invSqrt2 = 0.70710678;
            vx *= invSqrt2;
            vy *= invSqrt2;
        }

        double speed = player.getMoveSpeed();
        player.setVelocity(new Vector2D(vx * speed, vy * speed));

        if (player.getCurrentState() instanceof Player.IdleState && (vx != 0 || vy != 0)) {
            player.changeState(new Player.MovingState());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Bật khử răng cưa và làm mịn hình ảnh chất lượng cao
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Cập nhật tọa độ Camera bám sát Player
        camera.update(world.getPlayer().getPosition(), 0.12);

        // 1. Vẽ nền đấu trường (Arena Floor & Grid)
        renderArenaBackground(g2d);

        // 2. Vẽ các quái vật
        for (Monster m : world.getMonsters()) {
            double sx = camera.worldToScreenX(m.getPosition().getX());
            double sy = camera.worldToScreenY(m.getPosition().getY());
            LpcCanvasRenderer.renderMonster(g2d, m, sx, sy);
        }

        // 3. Vẽ nhân vật người chơi (Player)
        Player p = world.getPlayer();
        double px = camera.worldToScreenX(p.getPosition().getX());
        double py = camera.worldToScreenY(p.getPosition().getY());
        LpcCanvasRenderer.renderCharacter(g2d, world.getPlayerRenderer(), px, py, p.getPlayerName(), p.getCurrentHp(), p.getMaxHp(), true);

        // 4. Vẽ các hiệu ứng chiến đấu (Slashes, Particles, Floating Text)
        renderCombatEffects(g2d);

        // 5. Vẽ Debug Overlay (Nếu bật F1)
        if (showDebugOverlay) {
            renderDebugOverlay(g2d);
        }

        // 6. Vẽ toàn bộ giao diện màn hình (HUD, Minimap, Overlays)
        renderHUD(g2d);

        // Đếm FPS
        frameCounter++;
        long now = System.currentTimeMillis();
        if (now - lastFpsUpdateTime >= 1000) {
            currentFps = frameCounter;
            frameCounter = 0;
            lastFpsUpdateTime = now;
        }
    }

    private void renderArenaBackground(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();

        // Vẽ các ô gạch sàn đấu trường
        int tileSize = 64;
        double startX = camera.worldToScreenX(0);
        double startY = camera.worldToScreenY(0);
        double endX = camera.worldToScreenX(GameWorld.WORLD_WIDTH);
        double endY = camera.worldToScreenY(GameWorld.WORLD_HEIGHT);

        // Vùng ngoài bản đồ (Void)
        g2d.setColor(new Color(15, 16, 20));
        g2d.fillRect(0, 0, width, height);

        // Mặt sàn đấu trường (Arena Ground)
        g2d.setColor(new Color(38, 42, 53));
        g2d.fillRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));

        // Lưới ô gạch đá (Stone Tile Grids)
        g2d.setColor(new Color(50, 56, 70));
        for (double wx = 0; wx <= GameWorld.WORLD_WIDTH; wx += tileSize) {
            int sx = (int) camera.worldToScreenX(wx);
            g2d.drawLine(sx, (int) startY, sx, (int) endY);
        }
        for (double wy = 0; wy <= GameWorld.WORLD_HEIGHT; wy += tileSize) {
            int sy = (int) camera.worldToScreenY(wy);
            g2d.drawLine((int) startX, sy, (int) endX, sy);
        }

        // Đường biên bao quanh đấu trường phát sáng (Border Walls)
        g2d.setColor(new Color(255, 175, 40));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));
        g2d.setStroke(new BasicStroke(1));
    }

    private void renderCombatEffects(Graphics2D g2d) {
        CombatEngine ce = world.getCombatEngine();

        // 1. Hiệu ứng vệt chém kiếm (Slash Arcs)
        for (CombatEngine.SlashEffect slash : ce.getSlashEffects()) {
            double sx = camera.worldToScreenX(slash.x);
            double sy = camera.worldToScreenY(slash.y);
            float alpha = (float) Math.max(0f, slash.lifeTime / slash.maxLifeTime);

            g2d.setColor(new Color(1f, 0.9f, 0.4f, alpha * 0.8f));
            g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arcAngle = slash.direction.getRowOffset() * 90;
            g2d.drawArc((int) sx - 24, (int) sy - 24, 48, 48, -arcAngle - 45, 90);
            g2d.setStroke(new BasicStroke(1));
        }

        // 2. Hiệu ứng bụi hạt va chạm (Hit Particles)
        for (CombatEngine.HitParticle p : ce.getParticles()) {
            double sx = camera.worldToScreenX(p.x);
            double sy = camera.worldToScreenY(p.y);
            float alpha = (float) Math.max(0f, p.lifeTime / p.maxLifeTime);
            Color c = p.color;
            g2d.setColor(new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha));
            g2d.fillOval((int) sx, (int) sy, (int) p.size, (int) p.size);
        }

        // 3. Số sát thương nảy lên (Floating Damage Numbers)
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        for (CombatEngine.FloatingText ft : ce.getFloatingTexts()) {
            double sx = camera.worldToScreenX(ft.x);
            double sy = camera.worldToScreenY(ft.y);
            float alpha = (float) Math.max(0f, ft.lifeTime / ft.maxLifeTime);
            Color c = ft.color;
            g2d.setColor(new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha));
            g2d.drawString(ft.text, (int) sx, (int) sy);
        }
    }

    private void renderDebugOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 255, 255, 120));
        Hitbox pHitbox = world.getPlayer().getHitbox();
        g2d.drawRect(
                (int) camera.worldToScreenX(pHitbox.getMinX()),
                (int) camera.worldToScreenY(pHitbox.getMinY()),
                (int) (pHitbox.getMaxX() - pHitbox.getMinX()),
                (int) (pHitbox.getMaxY() - pHitbox.getMinY())
        );

        // Hitbox tấn công
        Hitbox pAttack = world.getPlayer().getAttackArea();
        g2d.setColor(new Color(255, 50, 50, 140));
        g2d.drawRect(
                (int) camera.worldToScreenX(pAttack.getMinX()),
                (int) camera.worldToScreenY(pAttack.getMinY()),
                (int) (pAttack.getMaxX() - pAttack.getMinX()),
                (int) (pAttack.getMaxY() - pAttack.getMinY())
        );

        // Hitbox quái vật
        g2d.setColor(new Color(255, 255, 0, 120));
        for (Monster m : world.getMonsters()) {
            Hitbox mHitbox = m.getHitbox();
            g2d.drawRect(
                    (int) camera.worldToScreenX(mHitbox.getMinX()),
                    (int) camera.worldToScreenY(mHitbox.getMinY()),
                    (int) (mHitbox.getMaxX() - mHitbox.getMinX()),
                    (int) (mHitbox.getMaxY() - mHitbox.getMinY())
            );
        }
    }

    private void renderHUD(Graphics2D g2d) {
        Player p = world.getPlayer();
        int width = getWidth();
        int height = getHeight();

        // --- TOP-LEFT: Thanh Sinh Lực (Health Bar) & Trạng thái ---
        int hudX = 20;
        int hudY = 20;
        int barW = 220;
        int barH = 18;

        // Khung nền HUD
        g2d.setColor(new Color(18, 20, 26, 220));
        g2d.fillRoundRect(hudX - 8, hudY - 8, barW + 110, 68, 12, 12);
        g2d.setColor(new Color(60, 65, 80));
        g2d.drawRoundRect(hudX - 8, hudY - 8, barW + 110, 68, 12, 12);

        // Tên người chơi
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2d.setColor(Color.WHITE);
        g2d.drawString("⚔️ " + p.getPlayerName(), hudX, hudY + 12);

        // Huy hiệu State Machine Badge
        String stateName = p.getCurrentState().getStateName();
        Color stateBadgeColor = stateName.equals("ATTACKING") ? Color.ORANGE :
                (stateName.equals("DEAD") ? Color.RED : (stateName.equals("MOVING") ? Color.CYAN : Color.LIGHT_GRAY));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.setColor(stateBadgeColor);
        g2d.drawString("[" + stateName + "]", hudX + 160, hudY + 12);

        // Thanh máu
        g2d.setColor(new Color(40, 20, 20));
        g2d.fillRoundRect(hudX, hudY + 22, barW, barH, 8, 8);

        float hpPct = Math.max(0f, Math.min(1f, (float) p.getCurrentHp() / p.getMaxHp()));
        Color barColor = hpPct > 0.5f ? new Color(46, 204, 113) : (hpPct > 0.25f ? new Color(241, 196, 15) : new Color(231, 76, 60));
        g2d.setColor(barColor);
        g2d.fillRoundRect(hudX, hudY + 22, (int) (barW * hpPct), barH, 8, 8);

        // Viền bóng thanh máu
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.drawRoundRect(hudX, hudY + 22, barW, barH, 8, 8);

        // Số máu hiển thị
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.setColor(Color.WHITE);
        String hpText = p.getCurrentHp() + " / " + p.getMaxHp() + " HP";
        g2d.drawString(hpText, hudX + barW / 2 - 30, hudY + 36);

        // Thông báo hạ gục mới nhất từ HudObserver
        String lastKill = world.getHudObserver().getLastKillNotification();
        if (!lastKill.isEmpty()) {
            g2d.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(lastKill, hudX, hudY + 54);
        }

        // --- TOP-CENTER: Wave, Điểm Số & Thời Gian ---
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2d.setColor(new Color(255, 215, 0));
        String waveText = "ĐỢT " + world.getCurrentWave();
        int waveW = g2d.getFontMetrics().stringWidth(waveText);
        g2d.drawString(waveText, width / 2 - waveW / 2, 32);

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2d.setColor(Color.LIGHT_GRAY);
        String subWave = "Điểm: " + world.getScore() + "  |  Hạ gục: " + p.getKillCount() + "  |  Quái còn: " + world.getMonsters().size();
        int subW = g2d.getFontMetrics().stringWidth(subWave);
        g2d.drawString(subWave, width / 2 - subW / 2, 50);

        // --- TOP-RIGHT: Bản Đồ Con (Mini-map) & FPS ---
        renderMinimap(g2d, width - 165, 20);

        g2d.setFont(new Font("Consolas", Font.BOLD, 12));
        g2d.setColor(Color.GREEN);
        g2d.drawString(currentFps + " FPS (Fixed 60Hz)", width - 165, 142);

        // --- BOTTOM-CENTER: Hướng Dẫn Điều Khiển ---
        renderBottomControlGuide(g2d, width, height);

        // --- MÀN HÌNH GAME OVER KHI TỬ VẬN ---
        if (!p.isAlive()) {
            renderGameOverOverlay(g2d, width, height);
        }
    }

    private void renderMinimap(Graphics2D g2d, int mapX, int mapY) {
        int mapW = 145;
        int mapH = 105;

        g2d.setColor(new Color(15, 17, 23, 220));
        g2d.fillRoundRect(mapX, mapY, mapW, mapH, 8, 8);
        g2d.setColor(new Color(70, 75, 90));
        g2d.drawRoundRect(mapX, mapY, mapW, mapH, 8, 8);

        double scaleX = (double) mapW / GameWorld.WORLD_WIDTH;
        double scaleY = (double) mapH / GameWorld.WORLD_HEIGHT;

        // Vẽ quái vật (Dấu chấm đỏ)
        g2d.setColor(Color.RED);
        for (Monster m : world.getMonsters()) {
            int mx = mapX + (int) (m.getPosition().getX() * scaleX);
            int my = mapY + (int) (m.getPosition().getY() * scaleY);
            g2d.fillRect(mx - 1, my - 1, 3, 3);
        }

        // Vẽ người chơi (Dấu chấm xanh lục sáng)
        Player p = world.getPlayer();
        int px = mapX + (int) (p.getPosition().getX() * scaleX);
        int py = mapY + (int) (p.getPosition().getY() * scaleY);
        g2d.setColor(Color.CYAN);
        g2d.fillOval(px - 2, py - 2, 5, 5);

        // Khung nhìn camera thu nhỏ
        int camBoxW = (int) (getWidth() * scaleX);
        int camBoxH = (int) (getHeight() * scaleY);
        int camBoxX = mapX + (int) ((camera.getX() - getWidth() / 2.0) * scaleX);
        int camBoxY = mapY + (int) ((camera.getY() - getHeight() / 2.0) * scaleY);
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.drawRect(camBoxX, camBoxY, camBoxW, camBoxH);
    }

    private void renderBottomControlGuide(Graphics2D g2d, int width, int height) {
        String guide = "⌨️ [W/A/S/D]: Di chuyển  |  [SPACE / J]: Chém kiếm  |  [R]: Tái sinh  |  [F1]: Bật/Tắt Hitbox AABB";
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int gW = fm.stringWidth(guide);

        g2d.setColor(new Color(15, 17, 23, 200));
        g2d.fillRoundRect(width / 2 - gW / 2 - 12, height - 36, gW + 24, 26, 8, 8);
        g2d.setColor(new Color(200, 205, 220));
        g2d.drawString(guide, width / 2 - gW / 2, height - 19);
    }

    private void renderGameOverOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, width, height);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
        g2d.setColor(new Color(231, 76, 60));
        String title = "💀 CHIẾN BINH ĐÃ NGÃ XUỐNG!";
        int tw = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, width / 2 - tw / 2, height / 2 - 50);

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g2d.setColor(Color.WHITE);
        String stats = "Đợt sinh tồn: " + world.getCurrentWave() + "  |  Tổng điểm: " + world.getScore() + "  |  Số quái tiêu diệt: " + world.getPlayer().getKillCount();
        int sw = g2d.getFontMetrics().stringWidth(stats);
        g2d.drawString(stats, width / 2 - sw / 2, height / 2 - 10);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2d.setColor(new Color(241, 196, 15));
        String prompt = "👉 NHẤN PHÍM [R] ĐỂ TÁI SINH VÀ PHỤC THÙ 👈";
        int pw = g2d.getFontMetrics().stringWidth(prompt);
        g2d.drawString(prompt, width / 2 - pw / 2, height / 2 + 40);
    }
}
