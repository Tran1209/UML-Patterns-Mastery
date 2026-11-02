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
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final NavigationMarkerManager markerManager = new NavigationMarkerManager();
    private final GameInputController inputController;
    private final Map<String, CharacterRenderer> remoteRenderers = new ConcurrentHashMap<>();
    private int currentFps = 60;
    private int frameCounter = 0;
    private long lastFpsUpdateTime = System.currentTimeMillis();

    // Tối ưu hóa hiệu năng & Chống GC Churn (Rule 7: Zero Allocation in Hot Path)
    private static final Comparator<Renderable25D> DEPTH_COMPARATOR = (a, b) -> Double.compare(a.getDepthY(), b.getDepthY());
    private final List<Renderable25D> renderQueue = new ArrayList<>(128);
    private final Renderable25D playerRenderable;

    // Object Pool cho Quái vật (Rule 7: Game Programming Patterns - Object Pool)
    private static class MonsterRenderItem implements Renderable25D {
        private Monster monster;
        private Camera2D camera;

        public void set(Monster monster, Camera2D camera) {
            this.monster = monster;
            this.camera = camera;
        }

        @Override
        public double getDepthY() {
            return monster.getPosition().getY();
        }

        @Override
        public void render(Graphics2D g) {
            double sx = camera.worldToScreenX(monster.getPosition().getX());
            double sy = camera.worldToScreenY(monster.getPosition().getY());
            LpcCanvasRenderer.renderMonster(g, monster, sx, sy);
        }
    }
    private final List<MonsterRenderItem> monsterRenderPool = new ArrayList<>(64);

    // Object Pool cho Thực thể Đấu Mạng Multiplayer
    private static class MultiplayerEntityRenderItem implements Renderable25D {
        private WorldSnapshotPayload.EntityState state;
        private Vector2D pos;
        private Camera2D camera;
        private CharacterRenderer renderer;
        private String displayTag;

        public void setMonster(WorldSnapshotPayload.EntityState state, Vector2D pos, Camera2D camera) {
            this.state = state;
            this.pos = pos;
            this.camera = camera;
            this.renderer = null;
            this.displayTag = null;
        }

        public void setPlayer(WorldSnapshotPayload.EntityState state, Vector2D pos, Camera2D camera,
                              CharacterRenderer renderer, String displayTag) {
            this.state = state;
            this.pos = pos;
            this.camera = camera;
            this.renderer = renderer;
            this.displayTag = displayTag;
        }

        @Override
        public double getDepthY() {
            return pos.getY();
        }

        @Override
        public void render(Graphics2D g) {
            double sx = camera.worldToScreenX(pos.getX());
            double sy = camera.worldToScreenY(pos.getY());
            if (state.isMonster) {
                LpcCanvasRenderer.renderInterpolatedMonster(g, state, sx, sy);
            } else if (renderer != null) {
                LpcCanvasRenderer.renderCharacter(g, renderer, sx, sy, displayTag, state.hp, state.maxHp, true);
            }
        }
    }
    private final List<MultiplayerEntityRenderItem> mpEntityRenderPool = new ArrayList<>(64);

    public GameCanvas(GameWorld world, NetworkGameClient networkClient, int width, int height) {
        this.world = world;
        this.networkClient = networkClient;
        this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);

        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(25, 27, 34));
        setFocusable(true);
        setDoubleBuffered(true);

        this.inputController = new GameInputController(this, world, networkClient, camera, markerManager);
        this.inputController.attachListeners();

        this.playerRenderable = new Renderable25D() {
            @Override
            public double getDepthY() {
                return world.getPlayer().getPosition().getY();
            }

            @Override
            public void render(Graphics2D g) {
                Player p = world.getPlayer();
                double px = camera.worldToScreenX(p.getPosition().getX());
                double py = camera.worldToScreenY(p.getPosition().getY());
                LpcCanvasRenderer.renderCharacter(g, world.getPlayerRenderer(), px, py, p.getPlayerName(), p.getCurrentHp(), p.getMaxHp(), true);
            }
        };
    }

    public GameCanvas(GameWorld world, int width, int height) {
        this(world, null, width, height);
    }

    private Vector2D getLocalPlayerWorldPos() {
        return inputController.getLocalPlayerWorldPos();
    }

    /**
     * Thu thập điều khiển và đồng bộ vòng lặp (Ủy quyền cho GameInputController).
     */
    public void processInput() {
        inputController.processInput();
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

        // Đồng bộ buffer GPU phần cứng (Direct3D/OpenGL Hardware Sync)
        Toolkit.getDefaultToolkit().sync();
    }

    // ==========================================
    // KIẾN TRÚC KẾT XUẤT ĐỒ HỌA 2.5D (ISOMETRIC / DEPTH SORTING)
    // ==========================================

    private void renderOfflineWorld(Graphics2D g2d) {
        camera.update(world.getPlayer().getPosition(), 0.12);
        Environment25DRenderer.renderArenaBackground(g2d, camera, getWidth(), getHeight());
        markerManager.render(g2d, camera);

        // 2.5D DEPTH SORTING (Tái sử dụng renderQueue - Zero Allocation)
        renderQueue.clear();

        // 1. Thêm Môi trường 2.5D (Trụ phong ấn, Cây đại thụ, Tảng đá rêu phong)
        Environment25DRenderer.addEnvironmentToRenderQueue(renderQueue, camera, getWidth(), getHeight());

        // 2. Thêm tất cả Quái vật (Object Pool + Frustum Culling)
        int monsterIdx = 0;
        int screenW = getWidth();
        int screenH = getHeight();
        for (Monster m : world.getMonsters()) {
            if (m.isAlive()) {
                double sx = camera.worldToScreenX(m.getPosition().getX());
                double sy = camera.worldToScreenY(m.getPosition().getY());
                // Frustum Culling: Bỏ qua quái nếu nằm ngoài màn hình
                if (sx < -64 || sx > screenW + 64 || sy < -64 || sy > screenH + 64) {
                    continue;
                }
                if (monsterIdx >= monsterRenderPool.size()) {
                    monsterRenderPool.add(new MonsterRenderItem());
                }
                MonsterRenderItem item = monsterRenderPool.get(monsterIdx++);
                item.set(m, camera);
                renderQueue.add(item);
            }
        }

        // 3. Thêm Người chơi chính
        renderQueue.add(playerRenderable);

        // 4. Sắp xếp toàn bộ hàng đợi theo chiều sâu Y tăng dần (Painter's Algorithm 2.5D)
        renderQueue.sort(DEPTH_COMPARATOR);
        for (int i = 0; i < renderQueue.size(); i++) {
            renderQueue.get(i).render(g2d);
        }

        renderCombatEffects(g2d);

        if (inputController.isShowDebugOverlay()) {
            renderDebugOverlay(g2d);
        }

        Player p = world.getPlayer();
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
        Environment25DRenderer.renderArenaBackground(g2d, camera, getWidth(), getHeight());
        markerManager.render(g2d, camera);

        // 3. 2.5D DEPTH SORTING cho toàn bộ thực thể Đấu Mạng (Object Pool + Zero Allocation)
        renderQueue.clear();
        Environment25DRenderer.addEnvironmentToRenderQueue(renderQueue, camera, getWidth(), getHeight());

        int mpIdx = 0;
        int screenW = getWidth();
        int screenH = getHeight();

        for (WorldSnapshotPayload.EntityState state : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(state.id, clientTime);
            if (pos == null) {
                pos = new Vector2D(state.posX, state.posY);
            }
            double sx = camera.worldToScreenX(pos.getX());
            double sy = camera.worldToScreenY(pos.getY());

            // Frustum Culling
            if (sx < -64 || sx > screenW + 64 || sy < -64 || sy > screenH + 64) {
                continue;
            }

            if (mpIdx >= mpEntityRenderPool.size()) {
                mpEntityRenderPool.add(new MultiplayerEntityRenderItem());
            }
            MultiplayerEntityRenderItem item = mpEntityRenderPool.get(mpIdx++);

            if (state.isMonster) {
                item.setMonster(state, pos, camera);
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
                item.setPlayer(state, pos, camera, renderer, displayTag);
            }
            renderQueue.add(item);
        }

        // Sắp xếp và vẽ theo chiều sâu Z/Y
        renderQueue.sort(DEPTH_COMPARATOR);
        for (int i = 0; i < renderQueue.size(); i++) {
            renderQueue.get(i).render(g2d);
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

    /**
     * Đường cong uốn lượn tự nhiên của dòng sông 2.5D theo trục hoành X (Ủy quyền cho Environment25DRenderer).
     */
    public static double getRiverCenterY(double wx) {
        return Environment25DRenderer.getRiverCenterY(wx);
    }

    private void renderCombatEffects(Graphics2D g2d) {
        Vector2D heroPos = getLocalPlayerWorldPos();
        CombatVisualRenderer.renderCombatVisuals(
                g2d,
                camera,
                world.getCombatEngine(),
                inputController.getActiveAimingSkill(),
                heroPos,
                world.getPlayer().getFacingDirection(),
                inputController.getCurrentAimX(),
                inputController.getCurrentAimY()
        );
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
