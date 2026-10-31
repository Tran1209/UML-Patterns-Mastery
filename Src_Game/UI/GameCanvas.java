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
                } else if ((code == KeyEvent.VK_1 || code == KeyEvent.VK_Q || code == KeyEvent.VK_U) && isNewPress) {
                    startDesktopSkillAim(1);
                } else if ((code == KeyEvent.VK_2 || code == KeyEvent.VK_E || code == KeyEvent.VK_I) && isNewPress) {
                    startDesktopSkillAim(2);
                } else if ((code == KeyEvent.VK_3 || code == KeyEvent.VK_F || code == KeyEvent.VK_O) && isNewPress) {
                    startDesktopSkillAim(3);
                } else if (code == KeyEvent.VK_ESCAPE) {
                    activeAimingSkill = 0;
                    draggingFromSkillBar = false;
                } else if (code == KeyEvent.VK_R) {
                    if (networkClient == null && !world.getPlayer().isAlive()) {
                        world.restartGame();
                    } else if (isNewPress) {
                        startDesktopSkillAim(3);
                    }
                } else if (code == KeyEvent.VK_F1) {
                    showDebugOverlay = !showDebugOverlay;
                } else if (code == KeyEvent.VK_V && isNewPress) {
                    camera.cycleCameraMode();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                pressedKeys.remove(code);

                if ((code == KeyEvent.VK_1 || code == KeyEvent.VK_Q || code == KeyEvent.VK_U) && activeAimingSkill == 1 && !draggingFromSkillBar) {
                    triggerDesktopSkill(1, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                } else if ((code == KeyEvent.VK_2 || code == KeyEvent.VK_E || code == KeyEvent.VK_I) && activeAimingSkill == 2 && !draggingFromSkillBar) {
                    triggerDesktopSkill(2, currentAimX, currentAimY);
                    activeAimingSkill = 0;
                } else if ((code == KeyEvent.VK_3 || code == KeyEvent.VK_F || code == KeyEvent.VK_R || code == KeyEvent.VK_O) && activeAimingSkill == 3 && !draggingFromSkillBar) {
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

                // Kiểm tra nhấn vào nút chuyển đổi góc nhìn 3D ở góc trên bên phải
                if (e.getX() >= getWidth() - 215 && e.getX() <= getWidth() - 20 && e.getY() >= 36 && e.getY() <= 64) {
                    camera.cycleCameraMode();
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
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    // Chuột trái đánh thường hướng về vị trí con trỏ (thao tác 1 tay mượt mà)
                    Vector2D heroPos = getLocalPlayerWorldPos();
                    double wx = camera.screenToWorldX(e.getX());
                    double wy = camera.screenToWorldY(e.getY());
                    double dx = wx - heroPos.getX();
                    double dy = wy - heroPos.getY();
                    if (Math.hypot(dx, dy) > 4.0) {
                        world.getPlayer().setFacingDirection(Direction.fromVelocity(dx, dy, world.getPlayer().getFacingDirection()));
                    }
                    if (networkClient != null && networkClient.isConnected()) {
                        attackRequested = true;
                    } else {
                        world.handlePlayerAttack();
                    }
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
        double heroSx = camera.worldToScreenX(heroPos.getX(), heroPos.getY());
        double heroSy = camera.worldToScreenY(heroPos.getY(), 0.0);

        double dx = mx - heroSx;
        double dy = (my - heroSy) / camera.getPitchTilt();
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

        long nowMs = System.currentTimeMillis();
        // Sắp xếp thứ tự vẽ theo chiều sâu trục Y (Painter's Algorithm / 3D Depth Buffer)
        List<Runnable> depthQueue = new ArrayList<>();
        List<Double> depthY = new ArrayList<>();

        for (double[] ob : Arena3DSceneRenderer.OBELISK_POSITIONS) {
            final double ox = ob[0], oy = ob[1];
            depthY.add(oy);
            depthQueue.add(() -> Arena3DSceneRenderer.render3DObeliskTower(g2d, camera, ox, oy, nowMs));
        }

        for (double[] tree : Arena3DSceneRenderer.TREE_POSITIONS) {
            final double tx = tree[0], ty = tree[1];
            depthY.add(ty);
            depthQueue.add(() -> Arena3DSceneRenderer.render25DForestTree(g2d, camera, tx, ty));
        }

        for (Monster m : world.getMonsters()) {
            if (!m.isAlive()) continue;
            final Monster monster = m;
            double wy = monster.getPosition().getY();
            depthY.add(wy);
            depthQueue.add(() -> {
                double sx = camera.worldToScreenX(monster.getPosition().getX(), wy);
                double sy = camera.worldToScreenY(wy, 0);
                double scale = camera.getPerspectiveScale(wy);
                LpcCanvasRenderer.renderMonster(g2d, monster, sx, sy, scale);
            });
        }

        Player p = world.getPlayer();
        double pWy = p.getPosition().getY();
        depthY.add(pWy);
        depthQueue.add(() -> {
            double px = camera.worldToScreenX(p.getPosition().getX(), pWy);
            double py = camera.worldToScreenY(pWy, 0);
            double scale = camera.getPerspectiveScale(pWy);
            LpcCanvasRenderer.renderCharacter(g2d, world.getPlayerRenderer(), px, py, p.getPlayerName(), p.getCurrentHp(), p.getMaxHp(), true, scale);
        });

        Integer[] order = new Integer[depthQueue.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(depthY::get));
        for (int idx : order) {
            depthQueue.get(idx).run();
        }

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

        // 2. Vẽ nền đấu trường 3D
        renderArenaBackground(g2d);

        // 3. Sắp xếp chiều sâu Z-Buffer (Painter's Algorithm) cho 4 Trụ 3D + Quái + Người chơi
        List<Runnable> depthQueue = new ArrayList<>();
        List<Double> depthY = new ArrayList<>();

        for (double[] ob : Arena3DSceneRenderer.OBELISK_POSITIONS) {
            final double ox = ob[0], oy = ob[1];
            depthY.add(oy);
            depthQueue.add(() -> Arena3DSceneRenderer.render3DObeliskTower(g2d, camera, ox, oy, clientTime));
        }

        for (double[] tree : Arena3DSceneRenderer.TREE_POSITIONS) {
            final double tx = tree[0], ty = tree[1];
            depthY.add(ty);
            depthQueue.add(() -> Arena3DSceneRenderer.render25DForestTree(g2d, camera, tx, ty));
        }

        for (WorldSnapshotPayload.EntityState state : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(state.id, clientTime);
            if (pos == null) {
                pos = new Vector2D(state.posX, state.posY);
            }
            final Vector2D finalPos = pos;
            final WorldSnapshotPayload.EntityState finalState = state;
            double wy = finalPos.getY();
            depthY.add(wy);

            depthQueue.add(() -> {
                double sx = camera.worldToScreenX(finalPos.getX(), wy);
                double sy = camera.worldToScreenY(wy, 0);
                double scale = camera.getPerspectiveScale(wy);

                if (finalState.isMonster) {
                    renderInterpolatedMonster(g2d, finalState, sx, sy, scale);
                } else {
                    boolean isMe = finalState.id.equals(myId);
                    CharacterRenderer renderer = remoteRenderers.computeIfAbsent(finalState.id, k -> {
                        CompositeCharacterVisual visual = new LpcCharacterBuilder()
                                .withBody("body.png")
                                .withShirt("shirt.png")
                                .withArmor("armor.png")
                                .withWeapon("weapon.png")
                                .build();
                        return new CharacterRenderer(visual);
                    });

                    Direction dir = Direction.valueOf(finalState.dirName != null ? finalState.dirName : "DOWN");
                    renderer.setDirection(dir);
                    if ("ATTACKING".equals(finalState.stateName)) {
                        renderer.playAnimation(LpcAnimationType.SLASH);
                    } else if ("MOVING".equals(finalState.stateName)) {
                        renderer.playAnimation(LpcAnimationType.WALK);
                    } else if ("DEAD".equals(finalState.stateName)) {
                        renderer.playAnimation(LpcAnimationType.HURT);
                    } else {
                        renderer.playAnimation(LpcAnimationType.WALK);
                    }

                    String displayTag = isMe ? ("⭐ " + finalState.name + " (BẠN)") : ("🌐 " + finalState.name);
                    LpcCanvasRenderer.renderCharacter(g2d, renderer, sx, sy, displayTag, finalState.hp, finalState.maxHp, true, scale);
                }
            });
        }

        Integer[] order = new Integer[depthQueue.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(depthY::get));
        for (int idx : order) {
            depthQueue.get(idx).run();
        }

        // 3.5. Vẽ hiệu ứng kỹ năng & chiến đấu 3D
        renderCombatEffects(g2d);

        // 4. Vẽ HUD chế độ Đấu Mạng
        int myHp = myState != null ? myState.hp : 100;
        int myMaxHp = myState != null ? myState.maxHp : 100;
        String myStateName = myState != null ? myState.stateName : "CONNECTED";
        String myName = myState != null ? myState.name : "Player";

        renderHUD(g2d, myName, myHp, myMaxHp, myStateName,
                0, networkClient.getCurrentWave(), 0, (int) states.values().stream().filter(e -> e.isMonster).count(), true);
    }

    private void renderInterpolatedMonster(Graphics2D g2d, WorldSnapshotPayload.EntityState state, double sx, double sy, double scale) {
        java.awt.geom.AffineTransform saved = g2d.getTransform();
        if (Math.abs(scale - 1.0) > 0.01) {
            g2d.translate(sx, sy);
            g2d.scale(scale, scale);
            g2d.translate(-sx, -sy);
        }

        g2d.setColor(new Color(231, 76, 60, 45));
        g2d.fillOval((int) sx - 18, (int) sy + 11, 36, 13);
        g2d.setColor(new Color(0, 0, 0, 80));
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
        g2d.setTransform(saved);
    }

    private void renderArenaBackground(Graphics2D g2d) {
        Arena3DSceneRenderer.renderArena3DFloorAndSky(g2d, camera, getWidth(), getHeight());
    }

    private void renderCombatEffects(Graphics2D g2d) {
        if (activeAimingSkill >= 1 && activeAimingSkill <= 3) {
            Vector2D heroPos = getLocalPlayerWorldPos();
            DesktopHudRenderer.renderSkillTargetIndicator(
                    g2d,
                    camera.worldToScreenX(heroPos.getX(), heroPos.getY()),
                    camera.worldToScreenY(heroPos.getY(), 0.0),
                    world.getPlayer().getFacingDirection(),
                    activeAimingSkill,
                    currentAimX,
                    currentAimY,
                    camera.getPitchTilt()
            );
        }

        CombatEngine ce = world.getCombatEngine();

        for (CombatEffects.SkillRingEffect ring : ce.getSkillRings()) {
            Arena3DSceneRenderer.render3DSkillEffect(g2d, camera, ring);
        }

        for (CombatEffects.SlashEffect slash : ce.getSlashEffects()) {
            double sx = camera.worldToScreenX(slash.x, slash.y);
            double sy = camera.worldToScreenY(slash.y, 12.0);
            float alpha = (float) Math.max(0f, slash.lifeTime / slash.maxLifeTime);

            g2d.setColor(new Color(1f, 0.9f, 0.4f, alpha * 0.8f));
            g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arcAngle = slash.direction.getRowOffset() * 90;
            g2d.drawArc((int) sx - 24, (int) sy - 18, 48, 36, -arcAngle - 45, 90);
            g2d.setStroke(new BasicStroke(1));
        }

        for (CombatEffects.HitParticle p : ce.getParticles()) {
            double sx = camera.worldToScreenX(p.x, p.y);
            double sy = camera.worldToScreenY(p.y, 14.0);
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
            double sx = camera.worldToScreenX(ft.x, ft.y);
            double sy = camera.worldToScreenY(ft.y, 28.0);
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
                (int) camera.worldToScreenX(pHitbox.getMinX(), pHitbox.getMinY()),
                (int) camera.worldToScreenY(pHitbox.getMinY(), 0),
                (int) (pHitbox.getMaxX() - pHitbox.getMinX()),
                (int) (pHitbox.getMaxY() - pHitbox.getMinY())
        );

        Hitbox pAttack = world.getPlayer().getAttackArea();
        g2d.setColor(new Color(255, 50, 50, 140));
        g2d.drawRect(
                (int) camera.worldToScreenX(pAttack.getMinX(), pAttack.getMinY()),
                (int) camera.worldToScreenY(pAttack.getMinY(), 0),
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
                playerCount, currentFps, isOnline, camera
        );
    }

    private void renderGameOverOverlay(Graphics2D g2d, int width, int height) {
        DesktopHudRenderer.renderGameOverOverlay(g2d, width, height);
    }
}
