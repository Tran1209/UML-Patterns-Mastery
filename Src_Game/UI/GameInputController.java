package ui;

import combat.CombatEngine;
import core.GameWorld;
import core.Monster;
import core.Player;
import core.Vector2D;
import network.NetworkGameClient;
import network.WorldSnapshotPayload;
import render.Camera2D;
import render.Direction;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bộ điều phối sự kiện nhập liệu bàn phím & chuột (GameInputController).
 * Áp dụng nguyên lý Controller (GRASP) & Command Pattern (GoF):
 * Đóng gói cơ chế điều khiển tối ưu chuẩn Game Hành động / MOBA:
 * - Chuột trái & Chuột phải: Di chuyển mặt đất & Khóa mục tiêu quái vật (Targeting / Auto-attack).
 * - Bàn phím điều hướng (WASD & Mũi tên): Di chuyển 8 hướng mượt mà không xung đột.
 * - Phím [S] / [H]: Lệnh dừng di chuyển tức thời (Stop Command).
 * - Kỹ năng [Q]/[1], [E]/[2], [R]/[3]: Định vị Smart Cast theo con trỏ chuột.
 * - Phím [Space] / [A]: Đánh thường cơ bản (phóng tia sét từ Gậy Phép).
 * Đảm bảo tính đơn trách nhiệm (SRP), tách biệt hoàn toàn khỏi GameCanvas.
 */
public class GameInputController {
    private final Component hostComponent;
    private final GameWorld world;
    private final NetworkGameClient networkClient;
    private final Camera2D camera;
    private final NavigationMarkerManager markerManager;

    private final Set<Integer> pressedKeys = new HashSet<>();
    private boolean showDebugOverlay = false;

    private boolean attackRequested = false;
    private int skillRequested = 0;
    private double skillAimRequestedX = 0.0;
    private double skillAimRequestedY = 0.0;

    // Trạng thái giữ kéo chọn vùng kỹ năng chuẩn MOBA trên PC
    private int activeAimingSkill = 0;
    private double currentAimX = 0.0;
    private double currentAimY = 0.0;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean draggingFromSkillBar = false;
    private int dragStartBoxCenterX = 0;
    private int dragStartBoxCenterY = 0;
    private boolean isMouseNavigating = false;

    public GameInputController(Component hostComponent, GameWorld world, NetworkGameClient networkClient,
                               Camera2D camera, NavigationMarkerManager markerManager) {
        this.hostComponent = hostComponent;
        this.world = world;
        this.networkClient = networkClient;
        this.camera = camera;
        this.markerManager = markerManager;
    }

    public void attachListeners() {
        hostComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyReleased(e);
            }
        });

        hostComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
        });

        hostComponent.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMoved(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        });
    }

    private void triggerAttack() {
        Player player = world.getPlayer();
        if (!player.isAlive()) {
            if (networkClient == null) {
                world.restartGame();
            }
            return;
        }
        if (!player.canAttack()) return;

        player.stopMoving();
        if (player.getTargetMonster() == null && lastMouseX >= 0 && lastMouseY >= 0) {
            double wx = camera.screenToWorldX(lastMouseX);
            double wy = camera.screenToWorldY(lastMouseY);
            double dx = wx - player.getPosition().getX();
            double dy = wy - player.getPosition().getY();
            player.setFacingDirection(Direction.fromVelocity(dx, dy, player.getFacingDirection()));
        }
        if (networkClient != null && networkClient.isConnected()) {
            attackRequested = true;
            world.handlePlayerAttack();
        } else {
            world.handlePlayerAttack();
        }
    }

    private void handleKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        boolean isNewPress = pressedKeys.add(code);

        // Đánh thường (Auto-attack): Phím A hoặc Space
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_A) {
            triggerAttack();
        // Kỹ năng 1: Phím Q hoặc Phím 1 (Xung Kích)
        } else if ((code == KeyEvent.VK_Q || code == KeyEvent.VK_1) && isNewPress) {
            startDesktopSkillAim(1);
        // Kỹ năng 2: Phím E hoặc Phím 2 (Vòng Xoáy Lửa)
        } else if ((code == KeyEvent.VK_E || code == KeyEvent.VK_2) && isNewPress) {
            startDesktopSkillAim(2);
        // Kỹ năng 3: Phím R hoặc Phím 3 (Thiên Phạt - Ultimate)
        } else if ((code == KeyEvent.VK_R || code == KeyEvent.VK_3) && isNewPress) {
            if (!world.getPlayer().isAlive()) {
                if (networkClient == null) {
                    world.restartGame();
                }
            } else {
                startDesktopSkillAim(3);
            }
        // Phím S: Lệnh dừng di chuyển tức thời khi đang chạy bằng chuột
        } else if (code == KeyEvent.VK_S && world.getPlayer().hasMoveTarget()) {
            stopPlayerMovement();
        } else if (code == KeyEvent.VK_ESCAPE) {
            activeAimingSkill = 0;
            draggingFromSkillBar = false;
        } else if (code == KeyEvent.VK_F1) {
            showDebugOverlay = !showDebugOverlay;
        }
    }

    private void handleKeyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        pressedKeys.remove(code);

        // Quick Cast on Release (Smart Cast): Nhả phím là tung chiêu theo hướng con trỏ chuột
        if ((code == KeyEvent.VK_Q || code == KeyEvent.VK_1) && activeAimingSkill == 1 && !draggingFromSkillBar) {
            triggerDesktopSkill(1, currentAimX, currentAimY);
            activeAimingSkill = 0;
        } else if ((code == KeyEvent.VK_E || code == KeyEvent.VK_2) && activeAimingSkill == 2 && !draggingFromSkillBar) {
            triggerDesktopSkill(2, currentAimX, currentAimY);
            activeAimingSkill = 0;
        } else if ((code == KeyEvent.VK_R || code == KeyEvent.VK_3) && activeAimingSkill == 3 && !draggingFromSkillBar) {
            triggerDesktopSkill(3, currentAimX, currentAimY);
            activeAimingSkill = 0;
        }
    }

    private void handleMousePressed(MouseEvent e) {
        hostComponent.requestFocusInWindow();
        lastMouseX = e.getX();
        lastMouseY = e.getY();

        // 1. Nhấn chuột phải chuẩn MOBA/LOL: Di chuyển hoặc Tấn công mục tiêu
        if (SwingUtilities.isRightMouseButton(e)) {
            isMouseNavigating = true;
            handleMovementOrTargetClick(e.getX(), e.getY(), true);
            return;
        }

        // 2. Nhấn chuột trực tiếp vào 1 trong 3 ô kỹ năng dưới màn hình để kéo thả
        int clickedBoxSkill = getHoveredSkillBox(e.getX(), e.getY());
        if (clickedBoxSkill >= 1 && clickedBoxSkill <= 3) {
            if (world.getPlayer().canCastSkill(clickedBoxSkill)) {
                activeAimingSkill = clickedBoxSkill;
                draggingFromSkillBar = true;
                int boxW = 148, gap = 14;
                int totalW = boxW * 3 + gap * 2;
                int startX = hostComponent.getWidth() / 2 - totalW / 2;
                dragStartBoxCenterX = startX + (clickedBoxSkill - 1) * (boxW + gap) + boxW / 2;
                dragStartBoxCenterY = hostComponent.getHeight() - 88 + 22;
                currentAimX = 0.0;
                currentAimY = 0.0;
            }
            return;
        }

        // 3. Đang ngắm bằng phím và click chuột trái -> Tung chiêu ngay tại vị trí con trỏ
        if (activeAimingSkill >= 1 && activeAimingSkill <= 3) {
            updateAimFromMouse(e.getX(), e.getY());
            triggerDesktopSkill(activeAimingSkill, currentAimX, currentAimY);
            activeAimingSkill = 0;
            draggingFromSkillBar = false;
            return;
        }

        // 4. Chuột trái click trên mặt đất: Hỗ trợ di chuyển & tấn công mục tiêu trực quan
        if (SwingUtilities.isLeftMouseButton(e)) {
            isMouseNavigating = true;
            handleMovementOrTargetClick(e.getX(), e.getY(), true);
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        isMouseNavigating = false;
        if (draggingFromSkillBar && activeAimingSkill >= 1 && activeAimingSkill <= 3) {
            triggerDesktopSkill(activeAimingSkill, currentAimX, currentAimY);
            activeAimingSkill = 0;
            draggingFromSkillBar = false;
        }
    }

    private void handleMouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        if (activeAimingSkill >= 1 && activeAimingSkill <= 3 && !draggingFromSkillBar) {
            updateAimFromMouse(e.getX(), e.getY());
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();

        // Kéo chuột liên tục trên mặt sân để nhân vật đuổi theo con trỏ chuột
        if (isMouseNavigating && activeAimingSkill == 0) {
            handleMovementOrTargetClick(e.getX(), e.getY(), false);
        }

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

    private int getHoveredSkillBox(int mx, int my) {
        int boxW = 148, boxH = 44, gap = 14;
        int totalW = boxW * 3 + gap * 2;
        int startX = hostComponent.getWidth() / 2 - totalW / 2;
        int boxY = hostComponent.getHeight() - 88;
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

    public Vector2D getLocalPlayerWorldPos() {
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

        // Dừng lệnh di chuyển cũ khi tung chiêu (chuẩn MOBA)
        player.stopMoving();

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
     * Xử lý lệnh nhấp chuột: Xác định di chuyển hay tấn công quái vật.
     * Áp dụng nguyên lý Information Expert: Tọa độ được ánh xạ chính xác và giao cho Player.
     */
    private void handleMovementOrTargetClick(int screenX, int screenY, boolean showMarker) {
        lastMouseX = screenX;
        lastMouseY = screenY;
        activeAimingSkill = 0;
        draggingFromSkillBar = false;

        Player player = world.getPlayer();
        if (!player.isAlive()) return;

        double wx = camera.screenToWorldX(screenX);
        double wy = camera.screenToWorldY(screenY);
        wx = Math.max(40, Math.min(GameWorld.WORLD_WIDTH - 40, wx));
        wy = Math.max(40, Math.min(GameWorld.WORLD_HEIGHT - 40, wy));

        Monster clickedMonster = null;
        double bestDist = 45.0; // Bán kính click trúng hitbox quái

        if (networkClient != null && networkClient.isConnected()) {
            Map<String, WorldSnapshotPayload.EntityState> states = networkClient.getLatestStates();
            for (WorldSnapshotPayload.EntityState state : states.values()) {
                if (state.isMonster && state.hp > 0) {
                    double dist = Math.hypot(state.posX - wx, state.posY - wy);
                    if (dist < bestDist) {
                        bestDist = dist;
                        clickedMonster = new Monster(state.id, Monster.MonsterType.GOBLIN, new Vector2D(state.posX, state.posY));
                    }
                }
            }
        } else {
            for (Monster m : world.getMonsters()) {
                if (m.isAlive()) {
                    double dist = m.getPosition().distanceTo(new Vector2D(wx, wy));
                    if (dist < bestDist) {
                        bestDist = dist;
                        clickedMonster = m;
                    }
                }
            }
        }

        if (clickedMonster != null) {
            player.targetEnemy(clickedMonster);
            if (showMarker) {
                markerManager.addAttackMarker(clickedMonster.getPosition().getX(), clickedMonster.getPosition().getY());
            }
        } else {
            player.moveTo(new Vector2D(wx, wy));
            if (showMarker) {
                markerManager.addMoveMarker(wx, wy);
            }
        }
    }

    public void stopPlayerMovement() {
        isMouseNavigating = false;
        world.getPlayer().stopMoving();
    }

    /**
     * Thu thập điều khiển và đồng bộ gói tin mạng trong chu kỳ fixed timestep 60Hz.
     */
    public void processInput() {
        Player player = world.getPlayer();
        markerManager.update(1.0 / 60.0);

        if (!player.isAlive()) {
            player.stopMoving();
            return;
        }

        // 1. Hỗ trợ điều khiển bàn phím kết hợp song song với chuột (VK_LEFT / Right / Up / Down)
        double keyVx = 0, keyVy = 0;
        if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) keyVy -= 1.0;
        if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) {
            if (!player.hasMoveTarget() || pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_D)) {
                keyVy += 1.0;
            }
        }
        if (pressedKeys.contains(KeyEvent.VK_LEFT)) keyVx -= 1.0;
        if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) keyVx += 1.0;

        if (keyVx != 0 || keyVy != 0) {
            if (keyVx != 0 && keyVy != 0) {
                keyVx *= 0.70710678;
                keyVy *= 0.70710678;
            }
            double speed = player.getMoveSpeed();
            player.stopMoving();
            player.setVelocity(new Vector2D(keyVx * speed, keyVy * speed));
            player.setFacingDirection(Direction.fromVelocity(keyVx, keyVy, player.getFacingDirection()));
            if (player.getCurrentState() instanceof Player.IdleState) {
                player.changeState(new Player.MovingState());
            }
        }

        // Tự động đánh thường liên tục khi giữ phím A hoặc Space
        if ((pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_SPACE)) && player.canAttack()) {
            triggerAttack();
        }

        // 2. Tự động tấn công khi mục tiêu quái vật đã nằm trong tầm đánh xa của gậy phép
        Monster target = player.getTargetMonster();
        if (target != null && target.isAlive()) {
            double dist = player.getPosition().distanceTo(target.getPosition());
            if (dist <= Player.DEFAULT_ATTACK_RANGE + 15.0 && player.canAttack()) {
                if (networkClient != null && networkClient.isConnected()) {
                    attackRequested = true;
                } else {
                    world.handlePlayerAttack();
                }
            }
        }

        // 3. Đồng bộ gói tin mạng Client-Server (Fixed Timestep 60Hz)
        if (networkClient != null && networkClient.isConnected()) {
            player.updateNavigation(1.0 / 60.0);
            player.updatePhysics(1.0 / 60.0);
            world.getCombatEngine().updateEffects(1.0 / 60.0);

            boolean isAttacking = attackRequested;
            int skill = skillRequested;
            double aimX = skillAimRequestedX;
            double aimY = skillAimRequestedY;
            attackRequested = false;
            skillRequested = 0;
            networkClient.sendInput(player.getVelocity().getX(), player.getVelocity().getY(), isAttacking, skill, aimX, aimY);
        }
    }

    public int getActiveAimingSkill() { return activeAimingSkill; }
    public double getCurrentAimX() { return currentAimX; }
    public double getCurrentAimY() { return currentAimY; }
    public boolean isShowDebugOverlay() { return showDebugOverlay; }
    public void toggleDebugOverlay() { this.showDebugOverlay = !this.showDebugOverlay; }
}
