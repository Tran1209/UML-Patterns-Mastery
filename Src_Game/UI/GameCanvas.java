package ui;

import combat.CombatEffects;
import combat.CombatEngine;
import combat.Hitbox;
import core.GameWorld;
import core.Monster;
import core.Player;
import core.Vector2D;
import network.NetworkGameClient;
import network.WorldSnapshotPayload;
import render.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bảng điều khiển kết xuất đồ họa Canvas 60 FPS tích hợp điều khiển bàn phím và hệ thống HUD.
 * Hỗ trợ đồng thời:
 * 1. Chế độ Chơi Đơn (Offline Local World)
 * 2. Chế độ Đấu Mạng (Multiplayer Online) tích hợp Snapshot Interpolation (LERP 20Hz -> 60 FPS).
 */
public class GameCanvas extends JPanel {
    private final GameWorld world;
    private final NetworkGameClient networkClient;
    private final Camera2D camera;
    private final Set<Integer> pressedKeys = new HashSet<>();

    private final Map<String, CharacterRenderer> remoteRenderers = new ConcurrentHashMap<>();
    private boolean showDebugOverlay = false;
    private int currentFps = 60;
    private int frameCounter = 0;
    private long lastFpsUpdateTime = System.currentTimeMillis();
    private boolean attackRequested = false;
    private int skillRequested = 0;
    private double skillAimRequestedX = 0.0;
    private double skillAimRequestedY = 0.0;

    // Trạng thái giữ kéo chọn vùng kỹ năng chuẩn Liên Quân Mobile (AOV) trên PC
    private int activeAimingSkill = 0;
    private double currentAimX = 0.0;
    private double currentAimY = 0.0;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean draggingFromSkillBar = false;
    private int dragStartBoxCenterX = 0;
    private int dragStartBoxCenterY = 0;

    public GameCanvas(GameWorld world, NetworkGameClient networkClient, int width, int height) {
        this.world = world;
        this.networkClient = networkClient;
        this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);

        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(25, 27, 34));
        setFocusable(true);
        setDoubleBuffered(true);

        setupKeyBindings();
    }

    public GameCanvas(GameWorld world, int width, int height) {
        this(world, null, width, height);
    }

    private void setupKeyBindings() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                boolean isNewPress = pressedKeys.add(code);

                if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_J) {
                    if (networkClient != null && networkClient.isConnected()) {
                        attackRequested = true;
                    } else {
                        world.handlePlayerAttack();
                    }
                } else if ((code == KeyEvent.VK_1 || code == KeyEvent.VK_U) && isNewPress) {
                    startDesktopSkillAim(1);
                } else if ((code == KeyEvent.VK_2 || code == KeyEvent.VK_I) && isNewPress) {
                    startDesktopSkillAim(2);
                } else if ((code == KeyEvent.VK_3 || code == KeyEvent.VK_O) && isNewPress) {
                    startDesktopSkillAim(3);
                } else if (code == KeyEvent.VK_ESCAPE) {
                    activeAimingSkill = 0;
                    draggingFromSkillBar = false;
                } else if (code == KeyEvent.VK_R) {
                    if (networkClient == null && !world.getPlayer().isAlive()) {
                        world.restartGame();
                    }
                } else if (code == KeyEvent.VK_F1) {
                    showDebugOverlay = !showDebugOverlay;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                pressedKeys.remove(code);

                if ((code == KeyEvent.VK_1 || code == KeyEvent.VK_U) && activeAimingSkill == 1 && !draggingFromSkillBar) {
                    triggerDesktopSkill(1, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                } else if ((code == KeyEvent.VK_2 || code == KeyEvent.VK_I) && activeAimingSkill == 2 && !draggingFromSkillBar) {
                    triggerDesktopSkill(2, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                } else if ((code == KeyEvent.VK_3 || code == KeyEvent.VK_O) && activeAimingSkill == 3 && !draggingFromSkillBar) {
                    triggerDesktopSkill(3, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastMouseX = e.getX();
                lastMouseY = e.getY();

                if (SwingUtilities.isRightMouseButton(e)) {
                    activeAimingSkill = 0;
                    draggingFromSkillBar = false;
                    return;
                }

                // Kiểm tra nếu nhấn chuột trực tiếp vào 1 trong 3 ô kỹ năng dưới màn hình để kéo thả như điện thoại
                int clickedBoxSkill = getHoveredSkillBox(e.getX(), e.getY());
                if (clickedBoxSkill >= 1 && clickedBoxSkill <= 3) {
                    if (world.getPlayer().canCastSkill(clickedBoxSkill)) {
                        activeAimingSkill = clickedBoxSkill;
                        draggingFromSkillBar = true;
                        int boxW = 148, gap = 14;
                        int totalW = boxW * 3 + gap * 2;
                        int startX = getWidth() / 2 - totalW / 2;
                        dragStartBoxCenterX = startX + (clickedBoxSkill - 1) * (boxW + gap) + boxW / 2;
                        dragStartBoxCenterY = getHeight() - 88 + 22;
                        currentAimX = 0.0;
                        currentAimY = 0.0;
                    }
                    return;
                }

                // Nếu đang bật ngắm bằng phím và click chuột trái lên bản đồ -> Tung chiêu ngay tại vị trí con trỏ
                if (activeAimingSkill >= 1 && activeAimingSkill <= 3) {
                    updateAimFromMouse(e.getX(), e.getY());
                    triggerDesktopSkill(activeAimingSkill, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                    draggingFromSkillBar = false;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingFromSkillBar && activeAimingSkill >= 1 && activeAimingSkill <= 3) {
                    triggerDesktopSkill(activeAimingSkill, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                    draggingFromSkillBar = false;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (activeAimingSkill >= 1 && activeAimingSkill <= 3 && !draggingFromSkillBar) {
                    updateAimFromMouse(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (activeAimingSkill >= 1 && activeAimingSkill <= 3) {
                    if (draggingFromSkillBar) {
                        double dx = e.getX() - dragStartBoxCenterX;
                        double dy = e.getY() - dragStartBoxCenterY;
                        double maxDrag = 95.0;
                        double dist = Math.hypot(dx, dy);
                        if (dist > maxDrag && dist > 0.001) {
                            currentAimX = dx / dist;
                            currentAimY = dy / dist;
                        } else {
                            currentAimX = dx / maxDrag;
                            currentAimY = dy / maxDrag;
                        }
                    } else {
                        updateAimFromMouse(e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private int getHoveredSkillBox(int mx, int my) {
        int boxW = 148, boxH = 44, gap = 14;
        int totalW = boxW * 3 + gap * 2;
        int startX = getWidth() / 2 - totalW / 2;
        int boxY = getHeight() - 88;
        if (my < boxY || my > boxY + boxH) return 0;
        for (int i = 0; i < 3; i++) {
            int bx = startX + i * (boxW + gap);
            if (mx >= bx && mx <= bx + boxW) return i + 1;
        }
        return 0;
    }

    private void startDesktopSkillAim(int skillId) {
        if (!world.getPlayer().canCastSkill(skillId)) return;
        activeAimingSkill = skillId;
        draggingFromSkillBar = false;
        if (lastMouseX >= 0 && lastMouseY >= 0) {
            updateAimFromMouse(lastMouseX, lastMouseY);
        } else {
            Direction dir = world.getPlayer().getFacingDirection();
            currentAimX = dir.getDirX() * (skillId == 1 ? 1.0 : 0.55);
            currentAimY = dir.getDirY() * (skillId == 1 ? 1.0 : 0.55);
        }
    }

    private Vector2D getLocalPlayerWorldPos() {
        if (networkClient != null && networkClient.isConnected()) {
            String myId = networkClient.getAssignedPlayerId();
            if (myId != null) {
                Vector2D pos = networkClient.getInterpolatedPosition(myId, System.currentTimeMillis());
                if (pos != null) return pos;
                WorldSnapshotPayload.EntityState st = networkClient.getLatestStates().get(myId);
                if (st != null) return new Vector2D(st.posX, st.posY);
            }
        }
        return world.getPlayer().getPosition();
    }

    private void updateAimFromMouse(int mx, int my) {
        Vector2D heroPos = getLocalPlayerWorldPos();
        double heroSx = camera.worldToScreenX(heroPos.getX());
        double heroSy = camera.worldToScreenY(heroPos.getY());

        double dx = mx - heroSx;
        double dy = my - heroSy;
        double maxRange = (activeAimingSkill == 1) ? CombatEngine.SKILL1_MAX_RANGE
                : ((activeAimingSkill == 2) ? CombatEngine.SKILL2_MAX_RANGE : CombatEngine.SKILL3_MAX_RANGE);
        double dist = Math.hypot(dx, dy);
        if (dist > maxRange && dist > 0.001) {
            currentAimX = dx / dist;
            currentAimY = dy / dist;
        } else {
            currentAimX = dx / maxRange;
            currentAimY = dy / maxRange;
        }
    }

    private void triggerDesktopSkill(int skillId, double aimX, double aimY) {
        Player player = world.getPlayer();
        if (!player.canCastSkill(skillId)) return;

        if (networkClient != null && networkClient.isConnected()) {
            player.triggerSkillCooldown(skillId);
            skillRequested = skillId;
            skillAimRequestedX = aimX;
            skillAimRequestedY = aimY;
            if (Math.hypot(aimX, aimY) < 0.08) {
                Direction dir = player.getFacingDirection();
                aimX = dir.getDirX() * (skillId == 1 ? 1.0 : 0.55);
                aimY = dir.getDirY() * (skillId == 1 ? 1.0 : 0.55);
            }
            Vector2D heroPos = getLocalPlayerWorldPos();
            world.getCombatEngine().spawnSkillVisualOnly(
                    heroPos.getX(), heroPos.getY(), aimX, aimY, skillId
            );
        } else {
            world.handlePlayerSkill(skillId, aimX, aimY);
        }
    }

    /**
     * Thu thập phím bấm từ người chơi và gửi gói tin điều khiển hoặc cập nhật vận tốc.
     */
    public void processInput() {
        double vx = 0;
        double vy = 0;

        if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) vy -= 1.0;
        if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) vy += 1.0;
        if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT)) vx -= 1.0;
        if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) vx += 1.0;

        if (vx != 0 && vy != 0) {
            double invSqrt2 = 0.70710678;
            vx *= invSqrt2;
            vy *= invSqrt2;
        }

        double speed = 220.0;

        if (networkClient != null && networkClient.isConnected()) {
            // Cập nhật hồi chiêu cục bộ & hướng nhìn
            Player player = world.getPlayer();
            if (vx != 0 || vy != 0) {
                player.setFacingDirection(Direction.fromVelocity(vx, vy, player.getFacingDirection()));
            }
            player.updatePhysics(1.0 / 60.0);
            world.getCombatEngine().updateEffects(1.0 / 60.0);

            boolean isAttacking = attackRequested;
            int skill = skillRequested;
            double aimX = skillAimRequestedX;
            double aimY = skillAimRequestedY;
            attackRequested = false;
            skillRequested = 0;
            networkClient.sendInput(vx * speed, vy * speed, isAttacking, skill, aimX, aimY);
        } else {
            // Chế độ Chơi Đơn Offline
            Player player = world.getPlayer();
            if (!player.isAlive()) return;

            player.setVelocity(new Vector2D(vx * speed, vy * speed));
            if (player.getCurrentState() instanceof Player.IdleState && (vx != 0 || vy != 0)) {
                player.changeState(new Player.MovingState());
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (networkClient != null && networkClient.isConnected()) {
            renderMultiplayerWorld(g2d);
        } else {
            renderOfflineWorld(g2d);
        }

        // Đếm FPS
        frameCounter++;
        long now = System.currentTimeMillis();
        if (now - lastFpsUpdateTime >= 1000) {
            currentFps = frameCounter;
            frameCounter = 0;
            lastFpsUpdateTime = now;
        }
    }

    private void renderOfflineWorld(Graphics2D g2d) {
        camera.update(world.getPlayer().getPosition(), 0.12);
        renderArenaBackground(g2d);

        for (Monster m : world.getMonsters()) {
            double sx = camera.worldToScreenX(m.getPosition().getX());
            double sy = camera.worldToScreenY(m.getPosition().getY());
            LpcCanvasRenderer.renderMonster(g2d, m, sx, sy);
        }

        Player p = world.getPlayer();
        double px = camera.worldToScreenX(p.getPosition().getX());
        double py = camera.worldToScreenY(p.getPosition().getY());
        LpcCanvasRenderer.renderCharacter(g2d, world.getPlayerRenderer(), px, py, p.getPlayerName(), p.getCurrentHp(), p.getMaxHp(), true);

        renderCombatEffects(g2d);

        if (showDebugOverlay) {
            renderDebugOverlay(g2d);
        }

        renderHUD(g2d, p.getPlayerName(), p.getCurrentHp(), p.getMaxHp(), p.getCurrentState().getStateName(),
                p.getKillCount(), world.getCurrentWave(), world.getScore(), world.getMonsters().size(), false);

        if (!p.isAlive()) {
            renderGameOverOverlay(g2d, getWidth(), getHeight());
        }
    }

    private void renderMultiplayerWorld(Graphics2D g2d) {
        long clientTime = System.currentTimeMillis();
        String myId = networkClient.getAssignedPlayerId();
        Map<String, WorldSnapshotPayload.EntityState> states = networkClient.getLatestStates();

        // 1. Tìm vị trí nhân vật cục bộ để điều khiển Camera
        Vector2D myInterpolatedPos = null;
        WorldSnapshotPayload.EntityState myState = null;

        if (myId != null && states.containsKey(myId)) {
            myState = states.get(myId);
            myInterpolatedPos = networkClient.getInterpolatedPosition(myId, clientTime);
        }

        if (myInterpolatedPos != null) {
            camera.update(myInterpolatedPos, 0.15);
        }

        // 2. Vẽ nền đấu trường
        renderArenaBackground(g2d);

        // 3. Vẽ tất cả thực thể nhận từ Snapshot thông qua SnapshotInterpolator LERP
        for (WorldSnapshotPayload.EntityState state : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(state.id, clientTime);
            if (pos == null) {
                pos = new Vector2D(state.posX, state.posY);
            }

            double sx = camera.worldToScreenX(pos.getX());
            double sy = camera.worldToScreenY(pos.getY());

            if (state.isMonster) {
                // Quái vật giả lập
                renderInterpolatedMonster(g2d, state, sx, sy);
            } else {
                // Người chơi (Cục bộ hoặc Bạn cùng chơi trực tuyến)
                boolean isMe = state.id.equals(myId);
                CharacterRenderer renderer = remoteRenderers.computeIfAbsent(state.id, k -> {
                    CompositeCharacterVisual visual = new LpcCharacterBuilder()
                            .withBody("body.png")
                            .withShirt("shirt.png")
                            .withArmor("armor.png")
                            .withWeapon("weapon.png")
                            .build();
                    return new CharacterRenderer(visual);
                });

                Direction dir = Direction.valueOf(state.dirName != null ? state.dirName : "DOWN");
                renderer.setDirection(dir);
                if ("ATTACKING".equals(state.stateName)) {
                    renderer.playAnimation(LpcAnimationType.SLASH);
                } else if ("MOVING".equals(state.stateName)) {
                    renderer.playAnimation(LpcAnimationType.WALK);
                } else if ("DEAD".equals(state.stateName)) {
                    renderer.playAnimation(LpcAnimationType.HURT);
                } else {
                    renderer.playAnimation(LpcAnimationType.WALK);
                }

                String displayTag = isMe ? ("⭐ " + state.name + " (BẠN)") : ("🌐 " + state.name);
                LpcCanvasRenderer.renderCharacter(g2d, renderer, sx, sy, displayTag, state.hp, state.maxHp, true);
            }
        }

        // 3.5. Vẽ hiệu ứng kỹ năng & chiến đấu
        renderCombatEffects(g2d);

        // 4. Vẽ HUD chế độ Đấu Mạng
        int myHp = myState != null ? myState.hp : 100;
        int myMaxHp = myState != null ? myState.maxHp : 100;
        String myStateName = myState != null ? myState.stateName : "CONNECTED";
        String myName = myState != null ? myState.name : "Player";

        renderHUD(g2d, myName, myHp, myMaxHp, myStateName,
                0, networkClient.getCurrentWave(), 0, (int) states.values().stream().filter(e -> e.isMonster).count(), true);
    }

    private void renderInterpolatedMonster(Graphics2D g2d, WorldSnapshotPayload.EntityState state, double sx, double sy) {
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval((int) sx - 14, (int) sy + 14, 28, 8);

        int px = (int) sx;
        int py = (int) sy;

        // Vẽ quái vật
        g2d.setColor(new Color(85, 150, 65));
        g2d.fillOval(px - 10, py - 18, 20, 16);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(px - 6, py - 12, 3, 3);
        g2d.fillRect(px + 3, py - 12, 3, 3);
        g2d.setColor(new Color(110, 70, 40));
        g2d.fillRoundRect(px - 8, py - 2, 16, 14, 4, 4);

        // Thanh máu
        int barW = 36;
        int barH = 5;
        int barX = px - barW / 2;
        int barY = py - 30;

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2d.setColor(Color.WHITE);
        g2d.drawString(state.name, px - g2d.getFontMetrics().stringWidth(state.name) / 2, barY - 4);

        g2d.setColor(new Color(20, 20, 20, 200));
        g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

        float pct = Math.max(0f, Math.min(1f, (float) state.hp / state.maxHp));
        g2d.setColor(pct > 0.5f ? new Color(50, 205, 50) : Color.RED);
        g2d.fillRect(barX, barY, (int) (barW * pct), barH);
    }

    private void renderArenaBackground(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();

        int tileSize = 64;
        double startX = camera.worldToScreenX(0);
        double startY = camera.worldToScreenY(0);
        double endX = camera.worldToScreenX(GameWorld.WORLD_WIDTH);
        double endY = camera.worldToScreenY(GameWorld.WORLD_HEIGHT);

        g2d.setColor(new Color(15, 16, 20));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(38, 42, 53));
        g2d.fillRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));

        g2d.setColor(new Color(50, 56, 70));
        for (double wx = 0; wx <= GameWorld.WORLD_WIDTH; wx += tileSize) {
            int sx = (int) camera.worldToScreenX(wx);
            g2d.drawLine(sx, (int) startY, sx, (int) endY);
        }
        for (double wy = 0; wy <= GameWorld.WORLD_HEIGHT; wy += tileSize) {
            int sy = (int) camera.worldToScreenY(wy);
            g2d.drawLine((int) startX, sy, (int) endX, sy);
        }

        g2d.setColor(new Color(255, 175, 40));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));
        g2d.setStroke(new BasicStroke(1));
    }

    private void renderCombatEffects(Graphics2D g2d) {
        if (activeAimingSkill >= 1 && activeAimingSkill <= 3) {
            Vector2D heroPos = getLocalPlayerWorldPos();
            DesktopHudRenderer.renderSkillTargetIndicator(
                    g2d,
                    camera.worldToScreenX(heroPos.getX()),
                    camera.worldToScreenY(heroPos.getY()),
                    world.getPlayer().getFacingDirection(),
                    activeAimingSkill,
                    currentAimX,
                    currentAimY
            );
        }

        CombatEngine ce = world.getCombatEngine();

        for (CombatEffects.SkillRingEffect ring : ce.getSkillRings()) {
            double sx = camera.worldToScreenX(ring.x);
            double sy = camera.worldToScreenY(ring.y);
            float alpha = (float) Math.max(0f, ring.lifeTime / ring.maxLifeTime);
            int c = ring.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            int rad = (int) (ring.maxRadius * (0.25 + 0.75 * (1.0 - alpha)));

            g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.22f));
            g2d.fillOval((int) sx - rad, (int) sy - rad, rad * 2, rad * 2);

            g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha * 0.9f));
            g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawOval((int) sx - rad, (int) sy - rad, rad * 2, rad * 2);
            g2d.setStroke(new BasicStroke(1));
        }

        for (CombatEffects.SlashEffect slash : ce.getSlashEffects()) {
            double sx = camera.worldToScreenX(slash.x);
            double sy = camera.worldToScreenY(slash.y);
            float alpha = (float) Math.max(0f, slash.lifeTime / slash.maxLifeTime);

            g2d.setColor(new Color(1f, 0.9f, 0.4f, alpha * 0.8f));
            g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arcAngle = slash.direction.getRowOffset() * 90;
            g2d.drawArc((int) sx - 24, (int) sy - 24, 48, 48, -arcAngle - 45, 90);
            g2d.setStroke(new BasicStroke(1));
        }

        for (CombatEffects.HitParticle p : ce.getParticles()) {
            double sx = camera.worldToScreenX(p.x);
            double sy = camera.worldToScreenY(p.y);
            float alpha = (float) Math.max(0f, p.lifeTime / p.maxLifeTime);
            int c = p.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha));
            g2d.fillOval((int) sx, (int) sy, (int) p.size, (int) p.size);
        }

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        for (CombatEffects.FloatingText ft : ce.getFloatingTexts()) {
            double sx = camera.worldToScreenX(ft.x);
            double sy = camera.worldToScreenY(ft.y);
            float alpha = (float) Math.max(0f, ft.lifeTime / ft.maxLifeTime);
            int c = ft.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            g2d.setColor(new Color(r / 255f, g / 255f, b / 255f, alpha));
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

        Hitbox pAttack = world.getPlayer().getAttackArea();
        g2d.setColor(new Color(255, 50, 50, 140));
        g2d.drawRect(
                (int) camera.worldToScreenX(pAttack.getMinX()),
                (int) camera.worldToScreenY(pAttack.getMinY()),
                (int) (pAttack.getMaxX() - pAttack.getMinX()),
                (int) (pAttack.getMaxY() - pAttack.getMinY())
        );
    }

    private void renderHUD(Graphics2D g2d, String name, int hp, int maxHp, String stateName,
                           int kills, int wave, int score, int monsterCount, boolean isOnline) {
        int playerCount = (isOnline && networkClient != null) ? networkClient.getLatestStates().size() : 1;
        DesktopHudRenderer.renderHUD(
                g2d, getWidth(), getHeight(), world.getPlayer(),
                name, hp, maxHp, stateName, kills, wave, score, monsterCount,
                playerCount, currentFps, isOnline
        );
    }

    private void renderGameOverOverlay(Graphics2D g2d, int width, int height) {
        DesktopHudRenderer.renderGameOverOverlay(g2d, width, height);
    }
}
