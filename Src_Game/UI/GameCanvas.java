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

    // ==========================================
    // KIẾN TRÚC KẾT XUẤT ĐỒ HỌA 2.5D (ISOMETRIC / DEPTH SORTING)
    // ==========================================

    private interface Renderable25D {
        double getDepthY();
        void render(Graphics2D g2d);
    }

    private void renderOfflineWorld(Graphics2D g2d) {
        camera.update(world.getPlayer().getPosition(), 0.12);
        renderArenaBackground(g2d);

        // 2.5D DEPTH SORTING (Z-Ordering theo tọa độ Y chân thực thể)
        List<Renderable25D> renderQueue = new ArrayList<>();

        // 1. Thêm Môi trường 2.5D (Trụ phong ấn, Cây đại thụ, Tảng đá rêu phong) vào hàng đợi chiều sâu
        addEnvironmentToRenderQueue(renderQueue);

        // 2. Thêm tất cả Quái vật
        for (Monster m : world.getMonsters()) {
            if (m.isAlive()) {
                renderQueue.add(new Renderable25D() {
                    @Override
                    public double getDepthY() { return m.getPosition().getY(); }
                    @Override
                    public void render(Graphics2D g) {
                        double sx = camera.worldToScreenX(m.getPosition().getX());
                        double sy = camera.worldToScreenY(m.getPosition().getY());
                        LpcCanvasRenderer.renderMonster(g, m, sx, sy);
                    }
                });
            }
        }

        // 3. Thêm Người chơi chính
        Player p = world.getPlayer();
        renderQueue.add(new Renderable25D() {
            @Override
            public double getDepthY() { return p.getPosition().getY(); }
            @Override
            public void render(Graphics2D g) {
                double px = camera.worldToScreenX(p.getPosition().getX());
                double py = camera.worldToScreenY(p.getPosition().getY());
                LpcCanvasRenderer.renderCharacter(g, world.getPlayerRenderer(), px, py, p.getPlayerName(), p.getCurrentHp(), p.getMaxHp(), true);
            }
        });

        // 4. Sắp xếp toàn bộ hàng đợi theo chiều sâu Y tăng dần (Painter's Algorithm 2.5D)
        renderQueue.sort(Comparator.comparingDouble(Renderable25D::getDepthY));
        for (Renderable25D item : renderQueue) {
            item.render(g2d);
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

        // 2. Vẽ nền đấu trường 2.5D
        renderArenaBackground(g2d);

        // 3. 2.5D DEPTH SORTING cho toàn bộ thực thể Đấu Mạng
        List<Renderable25D> renderQueue = new ArrayList<>();
        addEnvironmentToRenderQueue(renderQueue);

        for (WorldSnapshotPayload.EntityState state : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(state.id, clientTime);
            if (pos == null) {
                pos = new Vector2D(state.posX, state.posY);
            }
            final Vector2D fPos = pos;
            final double sx = camera.worldToScreenX(fPos.getX());
            final double sy = camera.worldToScreenY(fPos.getY());

            if (state.isMonster) {
                renderQueue.add(new Renderable25D() {
                    @Override
                    public double getDepthY() { return fPos.getY(); }
                    @Override
                    public void render(Graphics2D g) {
                        renderInterpolatedMonster(g, state, sx, sy);
                    }
                });
            } else {
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
                renderQueue.add(new Renderable25D() {
                    @Override
                    public double getDepthY() { return fPos.getY(); }
                    @Override
                    public void render(Graphics2D g) {
                        LpcCanvasRenderer.renderCharacter(g, renderer, sx, sy, displayTag, state.hp, state.maxHp, true);
                    }
                });
            }
        }

        // Sắp xếp và vẽ theo chiều sâu Z/Y
        renderQueue.sort(Comparator.comparingDouble(Renderable25D::getDepthY));
        for (Renderable25D item : renderQueue) {
            item.render(g2d);
        }

        // 3.5. Vẽ hiệu ứng kỹ năng & chiến đấu 2.5D
        renderCombatEffects(g2d);

        // 4. Vẽ HUD chế độ Đấu Mạng
        int myHp = myState != null ? myState.hp : 100;
        int myMaxHp = myState != null ? myState.maxHp : 100;
        String myStateName = myState != null ? myState.stateName : "CONNECTED";
        String myName = myState != null ? myState.name : "Player";

        renderHUD(g2d, myName, myHp, myMaxHp, myStateName,
                0, networkClient.getCurrentWave(), 0, (int) states.values().stream().filter(e -> e.isMonster).count(), true);
    }

    // Vị trí và loại cây cối 2.5D (Tree X, Tree Y, Type: 0 = Ancient Oak, 1 = Autumn Gold Maple)
    private static final double[][] TREE_COORDINATES = {
            // Rặng cây bờ sông phía Bắc (North Riverbank Grove)
            {140, 150, 1}, {330, 125, 0}, {680, 135, 1}, {970, 120, 0}, {1350, 140, 1}, {1520, 115, 0},
            // Cây ven bờ sông phía Nam (South Riverbank)
            {160, 440, 0}, {720, 435, 1}, {1450, 440, 0},
            // Rừng cây râm mát mạn Tây (West Glade)
            {110, 740, 1}, {140, 1020, 0}, {280, 1110, 1},
            // Rừng cây mạn Đông (East Grove)
            {1500, 750, 1}, {1470, 1030, 0}, {1310, 1110, 1},
            // Cây trang trí thảm cỏ phía Nam (South Park)
            {450, 920, 0}, {1160, 930, 1}
    };

    // Vị trí các tảng đá rêu phong 2.5D (Boulder X, Boulder Y, Radius)
    private static final double[][] BOULDER_COORDINATES = {
            {240, 600, 26}, {1370, 620, 28}, {620, 1020, 24}, {980, 1030, 26}
    };

    /**
     * Đường cong uốn lượn tự nhiên của dòng sông 2.5D theo trục hoành X.
     */
    public static double getRiverCenterY(double wx) {
        return 290.0 + Math.sin(wx * 0.004) * 42.0 + Math.cos(wx * 0.0022) * 16.0;
    }

    private void addEnvironmentToRenderQueue(List<Renderable25D> queue) {
        // 1. 4 Trụ đá Phong ấn 2.5D
        addPillarsToRenderQueue(queue);

        // 2. Toàn bộ cây đại thụ 2.5D (Hỗ trợ che khuất nhân vật theo chiều sâu Z)
        for (double[] t : TREE_COORDINATES) {
            final double wx = t[0];
            final double wy = t[1];
            final int type = (int) t[2];
            queue.add(new Renderable25D() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void render(Graphics2D g) {
                    render25DTree(g, wx, wy, type);
                }
            });
        }

        // 3. Các tảng đá rêu phong 2.5D
        for (double[] b : BOULDER_COORDINATES) {
            final double wx = b[0];
            final double wy = b[1];
            final double rad = b[2];
            queue.add(new Renderable25D() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void render(Graphics2D g) {
                    render25DBoulder(g, wx, wy, rad);
                }
            });
        }
    }

    private void addPillarsToRenderQueue(List<Renderable25D> queue) {
        double[][] pillarCoords = {
                {95, 95},
                {GameWorld.WORLD_WIDTH - 95, 95},
                {95, GameWorld.WORLD_HEIGHT - 95},
                {GameWorld.WORLD_WIDTH - 95, GameWorld.WORLD_HEIGHT - 95}
        };

        for (double[] coord : pillarCoords) {
            double wx = coord[0];
            double wy = coord[1];
            queue.add(new Renderable25D() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void render(Graphics2D g) {
                    render25DPillar(g, wx, wy);
                }
            });
        }
    }

    private void render25DTree(Graphics2D g2d, double worldX, double worldY, int treeType) {
        double sx = camera.worldToScreenX(worldX);
        double sy = camera.worldToScreenY(worldY);

        if (sx < -140 || sx > getWidth() + 140 || sy < -180 || sy > getHeight() + 140) return;

        int px = (int) sx;
        int py = (int) sy;

        // 1. Bóng đổ nghiêng 2.5D trên mặt cỏ (Directional Soft Ground Shadow)
        g2d.setColor(new Color(15, 30, 18, 95));
        g2d.fillOval(px - 34, py - 6, 68, 26);

        // 2. Thân cây gỗ đại thụ 3D (Volumetric Gnarled Trunk)
        int trunkH = 50;
        int trunkBaseW = 20;
        int trunkTopW = 12;

        // Rễ cây đâm xuống đất
        g2d.setColor(new Color(60, 38, 22));
        g2d.fillOval(px - 14, py - 6, 28, 12);

        // Thân bên phải (Khuất sáng / Bóng tối)
        g2d.setColor(new Color(65, 42, 25));
        Polygon rightTrunk = new Polygon(
                new int[]{px, px + trunkTopW / 2, px + trunkBaseW / 2, px},
                new int[]{py - trunkH, py - trunkH, py, py},
                4
        );
        g2d.fillPolygon(rightTrunk);

        // Thân bên trái (Đón ánh sáng Tây-Bắc)
        g2d.setColor(new Color(105, 70, 42));
        Polygon leftTrunk = new Polygon(
                new int[]{px - trunkTopW / 2, px, px, px - trunkBaseW / 2},
                new int[]{py - trunkH, py - trunkH, py, py},
                4
        );
        g2d.fillPolygon(leftTrunk);

        // Vết vân gỗ vỏ cây
        g2d.setColor(new Color(45, 28, 16));
        g2d.drawLine(px - 3, py - 6, px - 2, py - trunkH + 8);
        g2d.drawLine(px + 4, py - 6, px + 3, py - trunkH + 8);

        // 3. Tán lá đa tầng 2.5D (Multi-layered Volumetric Canopy with Wind Sway)
        long time = System.currentTimeMillis();
        double sway = Math.sin(time * 0.0022 + worldX * 0.04) * 3.0;

        Color shadowLeaf = (treeType == 0) ? new Color(20, 65, 30) : new Color(125, 42, 20);
        Color bodyLeaf   = (treeType == 0) ? new Color(42, 122, 54) : new Color(195, 78, 28);
        Color lightLeaf  = (treeType == 0) ? new Color(82, 178, 76) : new Color(245, 148, 42);
        Color tipLeaf    = (treeType == 0) ? new Color(140, 220, 118, 190) : new Color(255, 215, 85, 200);

        int cy = py - trunkH - 8;

        // Lớp lá đáy (Bottom Underside Shadow Lobes)
        g2d.setColor(shadowLeaf);
        g2d.fillOval((int) (px - 36 + sway * 0.4), cy - 6, 44, 38);
        g2d.fillOval((int) (px - 6 + sway * 0.5), cy - 4, 46, 38);

        // Lớp lá giữa (Midtone Body)
        g2d.setColor(bodyLeaf);
        g2d.fillOval((int) (px - 30 + sway * 0.7), cy - 24, 46, 42);
        g2d.fillOval((int) (px - 14 + sway * 0.8), cy - 22, 48, 42);
        g2d.fillOval((int) (px - 24 + sway * 0.9), cy - 42, 50, 44);

        // Lớp lá đón sáng phía trên Tây-Bắc (Sunlit Highlights)
        g2d.setColor(lightLeaf);
        g2d.fillOval((int) (px - 28 + sway * 0.9), cy - 44, 36, 34);
        g2d.fillOval((int) (px - 18 + sway * 0.8), cy - 28, 26, 24);

        // Đốm sáng đỉnh ngọn lá (Leaf Tip Highlights)
        g2d.setColor(tipLeaf);
        g2d.fillOval((int) (px - 22 + sway), cy - 48, 22, 18);
        g2d.fillOval((int) (px - 32 + sway * 0.7), cy - 32, 14, 12);
    }

    private void render25DBoulder(Graphics2D g2d, double worldX, double worldY, double radius) {
        double sx = camera.worldToScreenX(worldX);
        double sy = camera.worldToScreenY(worldY);

        if (sx < -80 || sx > getWidth() + 80 || sy < -80 || sy > getHeight() + 80) return;

        int px = (int) sx;
        int py = (int) sy;
        int r = (int) radius;

        // 1. Bóng đổ tảng đá
        g2d.setColor(new Color(15, 25, 18, 90));
        g2d.fillOval(px - r - 4, py - (int) (r * 0.25), (int) (r * 2.2), (int) (r * 0.8));

        // 2. Thân đá khối 3D (Shadow Side)
        g2d.setColor(new Color(48, 54, 62));
        g2d.fillRoundRect(px - r, py - (int) (r * 1.1), r * 2, (int) (r * 1.3), 12, 12);

        // Thân đá đón sáng (Northwest Highlight Facet)
        g2d.setColor(new Color(88, 98, 112));
        g2d.fillRoundRect(px - r + 3, py - (int) (r * 1.1) + 2, (int) (r * 1.3), (int) (r * 0.9), 10, 10);

        // Khe nứt đá (Cracks)
        g2d.setColor(new Color(32, 36, 42));
        g2d.drawLine(px - 2, py - (int) (r * 0.8), px + 4, py - (int) (r * 0.3));

        // 3. Rêu xanh phong trần phủ trên đỉnh đá (Moss Patch)
        g2d.setColor(new Color(55, 120, 60));
        g2d.fillOval(px - (int) (r * 0.7), py - (int) (r * 1.15), (int) (r * 1.2), (int) (r * 0.55));
        g2d.setColor(new Color(90, 175, 80));
        g2d.fillOval(px - (int) (r * 0.5), py - (int) (r * 1.15) + 1, (int) (r * 0.7), (int) (r * 0.35));
    }

    private void render25DPillar(Graphics2D g2d, double worldX, double worldY) {
        double sx = camera.worldToScreenX(worldX);
        double sy = camera.worldToScreenY(worldY);

        int px = (int) sx;
        int py = (int) sy;

        // 1. Bóng đổ xiên của trụ trên mặt đất (2.5D Base Shadow)
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval(px - 14, py + 12, 44, 20);

        // 2. Chân đế đá vuông phối cảnh 2.5D (Octagonal Plinth)
        g2d.setColor(new Color(40, 45, 55));
        g2d.fillRoundRect(px - 20, py + 4, 40, 16, 6, 6);
        g2d.setColor(new Color(65, 72, 88));
        g2d.fillRoundRect(px - 18, py + 2, 36, 12, 4, 4);

        // 3. Thân cột đá 3D cao 64px vát sáng-tối theo góc Tây-Bắc
        int pillarHeight = 64;
        int colY = py + 2 - pillarHeight;

        // Nửa thân bên phải (Khuất bóng tối)
        g2d.setColor(new Color(45, 50, 62));
        g2d.fillRect(px, colY, 14, pillarHeight);
        // Nửa thân bên trái (Đón ánh sáng)
        g2d.setColor(new Color(75, 84, 104));
        g2d.fillRect(px - 14, colY, 14, pillarHeight);

        // Rãnh đá trang trí trên thân cột
        g2d.setColor(new Color(30, 34, 42));
        g2d.drawLine(px - 6, colY + 8, px - 6, colY + pillarHeight - 8);
        g2d.drawLine(px + 6, colY + 8, px + 6, colY + pillarHeight - 8);

        // 4. Đỉnh cột / Bệ đỡ ngọn đuốc (Capital)
        g2d.setColor(new Color(85, 95, 118));
        g2d.fillRoundRect(px - 18, colY - 6, 36, 10, 4, 4);

        // 5. Ngọn lửa năng lượng ma thuật bập bùng trên đỉnh trụ (Animated Arcane Flame)
        long time = System.currentTimeMillis();
        double pulse = Math.sin(time * 0.006 + worldX * 0.05) * 3.0;
        int flameY = (int) (colY - 18 + pulse);

        // Vầng sáng tỏa xung quanh ngọn lửa
        g2d.setColor(new Color(0, 229, 255, 30));
        g2d.fillOval(px - 28, flameY - 14, 56, 44);

        // Lớp lửa ngoài
        g2d.setColor(new Color(0, 200, 255, 200));
        g2d.fillOval(px - 10, flameY, 20, 20);
        // Lõi lửa trắng nóng
        g2d.setColor(new Color(240, 255, 255, 240));
        g2d.fillOval(px - 5, flameY + 4, 10, 12);
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

        drawDirtTrail(g2d, 460, riverBridgeWestY, 560, 540, centerWorldX, centerWorldY);
        drawDirtTrail(g2d, 1140, riverBridgeEastY, 1040, 540, centerWorldX, centerWorldY);
        drawDirtTrail(g2d, centerWorldX, GameWorld.WORLD_HEIGHT - 60, centerWorldX, 900, centerWorldX, centerWorldY);

        // 4. DÒNG SÔNG 2.5D UỐN LƯỢN (Animated Meandering 2.5D River)
        Path2D.Double riverPoly = new Path2D.Double();
        double rStep = 24.0;
        for (double wx = 0; wx <= GameWorld.WORLD_WIDTH + rStep; wx += rStep) {
            double ry = getRiverCenterY(wx) - 44.0;
            double sx = camera.worldToScreenX(wx);
            double sy = camera.worldToScreenY(ry);
            if (wx == 0) riverPoly.moveTo(sx, sy);
            else riverPoly.lineTo(sx, sy);
        }
        for (double wx = GameWorld.WORLD_WIDTH + rStep; wx >= 0; wx -= rStep) {
            double ry = getRiverCenterY(wx) + 44.0;
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
        g2d.setStroke(new BasicStroke(1.8f));
        for (int i = 0; i < 3; i++) {
            Path2D.Double wavePath = new Path2D.Double();
            double yOffset = (i - 1) * 22.0;
            double freq = 0.018 + i * 0.006;
            double speed = (i + 1) * 0.0026;
            boolean first = true;
            for (double wx = 0; wx <= GameWorld.WORLD_WIDTH + rStep; wx += rStep) {
                double cy = getRiverCenterY(wx);
                double waveY = cy + yOffset + Math.sin(time * speed + wx * freq) * 5.5;
                double sx = camera.worldToScreenX(wx);
                double sy = camera.worldToScreenY(waveY);
                if (first) {
                    wavePath.moveTo(sx, sy);
                    first = false;
                } else {
                    wavePath.lineTo(sx, sy);
                }
            }
            g2d.draw(wavePath);
        }

        // Bờ sông Bắc & Nam (Moist Riverbanks)
        g2d.setColor(new Color(135, 112, 75));
        g2d.setStroke(new BasicStroke(3.0f));
        Path2D.Double northBank = new Path2D.Double();
        Path2D.Double southBank = new Path2D.Double();
        for (double wx = 0; wx <= GameWorld.WORLD_WIDTH + rStep; wx += rStep) {
            double nry = getRiverCenterY(wx) - 44.0;
            double sry = getRiverCenterY(wx) + 44.0;
            double nsx = camera.worldToScreenX(wx);
            double nsy = camera.worldToScreenY(nry);
            double ssx = camera.worldToScreenX(wx);
            double ssy = camera.worldToScreenY(sry);
            if (wx == 0) {
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
        render25DStoneBridge(g2d, 460);
        render25DStoneBridge(g2d, 1140);

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
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.drawOval((int) (csx - terraceRx), (int) (csy - terraceRy), (int) (terraceRx * 2), (int) (terraceRy * 2));

        // Rêu phong phủ rìa thềm đá (Overgrown Moss)
        g2d.setColor(new Color(48, 108, 56, 170));
        g2d.setStroke(new BasicStroke(6f));
        g2d.drawOval((int) (csx - terraceRx + 4), (int) (csy - terraceRy + 3), (int) ((terraceRx - 4) * 2), (int) ((terraceRy - 3) * 2));

        // Mạng lưới gạch kim cương 2.5D trên mặt đài tế
        g2d.setColor(new Color(42, 47, 59));
        g2d.setStroke(new BasicStroke(1.2f));
        double tileSize = 96.0;
        for (double d = -GameWorld.WORLD_HEIGHT; d <= GameWorld.WORLD_WIDTH + GameWorld.WORLD_HEIGHT; d += tileSize) {
            double x1 = d;
            double y1 = 0;
            double x2 = d + GameWorld.WORLD_HEIGHT;
            double y2 = GameWorld.WORLD_HEIGHT;

            int sx1 = (int) camera.worldToScreenX(x1);
            int sy1 = (int) camera.worldToScreenY(y1);
            int sx2 = (int) camera.worldToScreenX(x2);
            int sy2 = (int) camera.worldToScreenY(y2);

            // Chỉ vẽ các đường nằm trong khu vực đài tế để giữ thảm cỏ tự nhiên bên ngoài
            if (Math.abs(x1 - centerWorldX) < 400 || Math.abs(x2 - centerWorldX) < 400) {
                g2d.drawLine(sx1, sy1, sx2, sy2);
            }

            double x3 = d;
            double y3 = GameWorld.WORLD_HEIGHT;
            double x4 = d + GameWorld.WORLD_HEIGHT;
            double y4 = 0;
            int sx3 = (int) camera.worldToScreenX(x3);
            int sy3 = (int) camera.worldToScreenY(y3);
            int sx4 = (int) camera.worldToScreenX(x4);
            int sy4 = (int) camera.worldToScreenY(y4);
            if (Math.abs(x3 - centerWorldX) < 400 || Math.abs(x4 - centerWorldX) < 400) {
                g2d.drawLine(sx3, sy3, sx4, sy4);
            }
        }

        // 7. THẦN TRẬN PHONG ẤN TÂM ĐẤU TRƯỜNG 2.5D (Central Arcane Runic Seal)
        int runeRx = 220;
        int runeRy = (int) (runeRx * 0.58);

        // Vòng ngoài phát sáng
        g2d.setColor(new Color(0, 229, 255, 30));
        g2d.fillOval((int) csx - runeRx, (int) csy - runeRy, runeRx * 2, runeRy * 2);

        g2d.setColor(new Color(0, 229, 255, 110));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawOval((int) csx - runeRx, (int) csy - runeRy, runeRx * 2, runeRy * 2);

        // Vòng khắc bên trong
        int innerRuneRx = 140;
        int innerRuneRy = (int) (innerRuneRx * 0.58);
        g2d.setColor(new Color(255, 215, 0, 85));
        g2d.setStroke(new BasicStroke(1.8f));
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

        // --- TƯỜNG PHÍA BẮC (North Wall): Mặt đứng thẳng + Mặt trên đón sáng ---
        g2d.setColor(new Color(24, 27, 34));
        g2d.fillRect((int) startX, (int) startY - wallHeight, (int) (endX - startX), wallHeight);

        // Gạch đá trên mặt đứng
        g2d.setColor(new Color(36, 41, 52));
        for (double wx = 0; wx <= GameWorld.WORLD_WIDTH; wx += 64) {
            int wsx = (int) camera.worldToScreenX(wx);
            g2d.drawLine(wsx, (int) startY - wallHeight, wsx, (int) startY);
        }

        // Mặt trên bờ tường đón sáng (Top Coping Stones)
        g2d.setColor(new Color(60, 68, 85));
        g2d.fillRect((int) startX - 8, (int) startY - wallHeight - copingHeight, (int) (endX - startX) + 16, copingHeight);
        g2d.setColor(new Color(90, 102, 126));
        g2d.drawLine((int) startX - 8, (int) startY - wallHeight - copingHeight, (int) endX + 8, (int) startY - wallHeight - copingHeight);

        // Đổ bóng của tường Bắc xuống mặt sàn đấu trường (Ambient Occlusion Shadow)
        g2d.setPaint(new GradientPaint(
                0, (float) startY, new Color(0, 0, 0, 140),
                0, (float) (startY + 38), new Color(0, 0, 0, 0)
        ));
        g2d.fillRect((int) startX, (int) startY, (int) (endX - startX), 38);

        // --- TƯỜNG PHÍA TÂY (West Wall): Mặt đứng nhìn sang phía Đông ---
        int westWallW = 28;
        g2d.setColor(new Color(28, 32, 40));
        g2d.fillRect((int) startX - westWallW, (int) startY - wallHeight, westWallW, (int) (endY - startY) + wallHeight);
        g2d.setPaint(new GradientPaint(
                (float) startX, 0, new Color(0, 0, 0, 120),
                (float) (startX + 28), 0, new Color(0, 0, 0, 0)
        ));
        g2d.fillRect((int) startX, (int) startY, 28, (int) (endY - startY));

        // --- GỜ ĐÁ BẢO VỆ PHÍA ĐÔNG & NAM (South & East Perimeter Curbs) ---
        g2d.setColor(new Color(255, 175, 40));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect((int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawDirtTrail(Graphics2D g2d, double x1, double y1, double cx, double cy, double x2, double y2) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(camera.worldToScreenX(x1), camera.worldToScreenY(y1));
        path.quadTo(camera.worldToScreenX(cx), camera.worldToScreenY(cy), camera.worldToScreenX(x2), camera.worldToScreenY(y2));

        // Viền đất sỏi
        g2d.setColor(new Color(118, 92, 60));
        g2d.setStroke(new BasicStroke(42f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);

        // Thân đường mòn
        g2d.setColor(new Color(152, 124, 88));
        g2d.setStroke(new BasicStroke(30f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);

        // Lòng đường mòn nhẵn
        g2d.setColor(new Color(175, 146, 108));
        g2d.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);
    }

    private void render25DStoneBridge(Graphics2D g2d, double bridgeCenterX) {
        double riverY = getRiverCenterY(bridgeCenterX);
        double topWY = riverY - 56.0;
        double botWY = riverY + 56.0;

        double sx1 = camera.worldToScreenX(bridgeCenterX - 52);
        double sx2 = camera.worldToScreenX(bridgeCenterX + 52);
        double sy1 = camera.worldToScreenY(topWY);
        double sy2 = camera.worldToScreenY(botWY);
        int bw = (int) (sx2 - sx1);
        int bh = (int) (sy2 - sy1);

        // 1. Bóng đổ của vòm cầu xuống dòng nước bên dưới
        g2d.setColor(new Color(10, 25, 40, 160));
        g2d.fillRoundRect((int) sx1 - 4, (int) sy1 + 10, bw + 8, bh - 10, 10, 10);

        // 2. Mặt sàn cầu lát đá cổ
        g2d.setColor(new Color(100, 106, 118));
        g2d.fillRoundRect((int) sx1, (int) sy1, bw, bh, 8, 8);

        // Rãnh đá lát mặt cầu
        g2d.setColor(new Color(75, 80, 92));
        g2d.setStroke(new BasicStroke(1.2f));
        for (int py = (int) sy1 + 10; py < sy2 - 6; py += 14) {
            g2d.drawLine((int) sx1 + 4, py, (int) sx2 - 4, py);
        }
        for (int px = (int) sx1 + 16; px < sx2 - 10; px += 24) {
            g2d.drawLine(px, (int) sy1 + 4, px, (int) sy2 - 4);
        }

        // 3. Lan can đá 3D hai bên cầu (Parapets)
        int parapetH = 16;
        int parapetW = 10;
        // Lan can phía Tây (Bên trái đón sáng)
        g2d.setColor(new Color(65, 72, 85));
        g2d.fillRect((int) sx1, (int) sy1 - parapetH, parapetW, bh + parapetH);
        g2d.setColor(new Color(125, 135, 150));
        g2d.fillRect((int) sx1, (int) sy1 - parapetH, parapetW, 4);

        // Lan can phía Đông (Bên phải khuất sáng)
        g2d.setColor(new Color(50, 56, 68));
        g2d.fillRect((int) sx2 - parapetW, (int) sy1 - parapetH, parapetW, bh + parapetH);
        g2d.setColor(new Color(110, 120, 135));
        g2d.fillRect((int) sx2 - parapetW, (int) sy1 - parapetH, parapetW, 4);

        // 4. 4 Trụ góc cầu gắn Đèn lồng Đồng ấm áp (Brass Lanterns)
        double[][] corners = {
                {sx1 + 4, sy1 + 4},
                {sx2 - 4, sy1 + 4},
                {sx1 + 4, sy2 - 4},
                {sx2 - 4, sy2 - 4}
        };
        for (double[] c : corners) {
            int cx = (int) c[0];
            int cy = (int) c[1];
            // Chân đế đá của đèn
            g2d.setColor(new Color(40, 45, 55));
            g2d.fillRoundRect(cx - 7, cy - 14, 14, 16, 4, 4);
            // Vầng sáng ấm áp
            g2d.setColor(new Color(255, 190, 60, 55));
            g2d.fillOval(cx - 16, cy - 28, 32, 28);
            // Khung kim loại lồng đèn
            g2d.setColor(new Color(70, 55, 30));
            g2d.drawRect(cx - 4, cy - 22, 8, 10);
            // Lõi lửa vàng rực
            g2d.setColor(new Color(255, 220, 100, 240));
            g2d.fillRect(cx - 3, cy - 20, 6, 7);
        }
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
        double ratio = 0.58; // Tỷ lệ phối cảnh 2.5D mặt đất

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

        // Vệt chém kiếm 2.5D (Perspective Curved Slash Arcs)
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

        // Hạt tia lửa va chạm 2.5D (Particles with Bounce Trajectory)
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

        // Chữ số sát thương nảy lên 2.5D dạng 3D Pop-up
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
