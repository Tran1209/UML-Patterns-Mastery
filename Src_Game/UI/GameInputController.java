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
import java.util.Set;

/**
 * Bộ điều phối sự kiện nhập liệu bàn phím & chuột (Input Controller).
 * Áp dụng nguyên lý Controller (GRASP) & Command Pattern (GoF):
 * Đóng gói toàn bộ cơ chế điều khiển chuẩn MOBA / Liên Minh / Liên Quân:
 * - Chuột phải: Di chuyển & Khóa mục tiêu quái vật (Targeting / Auto-attack).
 * - Phím [S]: Lệnh dừng di chuyển tức thời (Stop Command).
 * - Kỹ năng [Q], [W], [E], [R]: Định vị Smart Cast theo con trỏ chuột.
 * - Phím [Space] / [A]: Đánh thường.
 * Tách biệt hoàn toàn khỏi GameCanvas nhằm đảm bảo tính đơn trách nhiệm (SRP).
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

    // Trạng thái giữ kéo chọn vùng kỹ năng chuẩn AOV trên PC
    private int activeAimingSkill = 0;
    private double currentAimX = 0.0;
    private double currentAimY = 0.0;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean draggingFromSkillBar = false;
    private int dragStartBoxCenterX = 0;
    private int dragStartBoxCenterY = 0;
    private boolean isRightMouseDown = false;

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

    private void handleKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        boolean isNewPress = pressedKeys.add(code);

        // Đánh thường (Auto-attack): Phím Space hoặc A chuẩn MOBA/LOL
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_A) {
            if (!world.getPlayer().isAlive()) {
                if (networkClient == null) {
                    world.restartGame();
                }
            } else if (networkClient != null && networkClient.isConnected()) {
                attackRequested = true;
            } else {
                world.handlePlayerAttack();
            }
        // Kỹ năng Q: Chiêu 1 (Xung Kích)
        } else if ((code == KeyEvent.VK_Q || code == KeyEvent.VK_1) && isNewPress) {
            startDesktopSkillAim(1);
        // Kỹ năng W: Chiêu 2 (Xoáy Lửa)
        } else if ((code == KeyEvent.VK_W || code == KeyEvent.VK_2) && isNewPress) {
            startDesktopSkillAim(2);
        // Kỹ năng E / R: Chiêu 3 (Thiên Phạt - Ultimate chuẩn LOL)
        } else if ((code == KeyEvent.VK_E || code == KeyEvent.VK_R || code == KeyEvent.VK_3) && isNewPress) {
            if (!world.getPlayer().isAlive()) {
                if (networkClient == null) {
                    world.restartGame();
                }
            } else {
                startDesktopSkillAim(3);
            }
        // Phím S: Lệnh dừng di chuyển ngay lập tức (Stop Command chuẩn LOL)
        } else if (code == KeyEvent.VK_S) {
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
        } else if ((code == KeyEvent.VK_W || code == KeyEvent.VK_2) && activeAimingSkill == 2 && !draggingFromSkillBar) {
            triggerDesktopSkill(2, currentAimX, currentAimY);
            activeAimingSkill = 0;
        } else if ((code == KeyEvent.VK_E || code == KeyEvent.VK_R || code == KeyEvent.VK_3) && activeAimingSkill == 3 && !draggingFromSkillBar) {
            triggerDesktopSkill(3, currentAimX, currentAimY);
            activeAimingSkill = 0;
        }
    }

    private void handleMousePressed(MouseEvent e) {
        hostComponent.requestFocusInWindow();
        lastMouseX = e.getX();
        lastMouseY = e.getY();

        // Chuột phải chuẩn LOL: Di chuyển hoặc Tấn công mục tiêu
        if (SwingUtilities.isRightMouseButton(e)) {
            isRightMouseDown = true;
            handleRightClick(e.getX(), e.getY());
            return;
        }

        // Nhấn chuột trực tiếp vào 1 trong 3 ô kỹ năng dưới màn hình để kéo thả
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

        // Đang ngắm bằng phím và click chuột trái -> Tung chiêu ngay tại vị trí con trỏ
        if (activeAimingSkill >= 1 && activeAimingSkill <= 3) {
            updateAimFromMouse(e.getX(), e.getY());
            triggerDesktopSkill(activeAimingSkill, currentAimX, currentAimY);
            activeAimingSkill = 0;
            draggingFromSkillBar = false;
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            isRightMouseDown = false;
        }
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

        // Giữ chuột phải kéo để điều khiển nhân vật chạy theo con trỏ chuột liên tục
        if (isRightMouseDown || SwingUtilities.isRightMouseButton(e)) {
            double wx = camera.screenToWorldX(e.getX());
            double wy = camera.screenToWorldY(e.getY());
            wx = Math.max(40, Math.min(GameWorld.WORLD_WIDTH - 40, wx));
            wy = Math.max(40, Math.min(GameWorld.WORLD_HEIGHT - 40, wy));
            world.getPlayer().moveTo(new Vector2D(wx, wy));
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

        // Dừng lệnh di chuyển cũ khi tung chiêu (chuẩn MOBA/LOL)
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

    private void handleRightClick(int screenX, int screenY) {
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
        for (Monster m : world.getMonsters()) {
            if (m.isAlive()) {
                double dist = m.getPosition().distanceTo(new Vector2D(wx, wy));
                if (dist < bestDist) {
                    bestDist = dist;
                    clickedMonster = m;
                }
            }
        }

        if (clickedMonster != null) {
            player.targetEnemy(clickedMonster);
            markerManager.addAttackMarker(clickedMonster.getPosition().getX(), clickedMonster.getPosition().getY());
        } else {
            player.moveTo(new Vector2D(wx, wy));
            markerManager.addMoveMarker(wx, wy);
        }
    }

    public void stopPlayerMovement() {
        isRightMouseDown = false;
        world.getPlayer().stopMoving();
    }

    /**
     * Thu thập điều khiển và đồng bộ gói tin mạng trong chu kỳ fixed timestep.
     */
    public void processInput() {
        Player player = world.getPlayer();
        markerManager.update(1.0 / 60.0);

        if (!player.isAlive()) {
            player.stopMoving();
            return;
        }

        if (isRightMouseDown && lastMouseX >= 0 && lastMouseY >= 0) {
            double wx = camera.screenToWorldX(lastMouseX);
            double wy = camera.screenToWorldY(lastMouseY);
            wx = Math.max(40, Math.min(GameWorld.WORLD_WIDTH - 40, wx));
            wy = Math.max(40, Math.min(GameWorld.WORLD_HEIGHT - 40, wy));
            player.moveTo(new Vector2D(wx, wy));
        }

        Monster target = player.getTargetMonster();
        if (target != null && target.isAlive()) {
            double dist = player.getPosition().distanceTo(target.getPosition());
            if (dist <= 65.0 && player.canAttack()) {
                if (networkClient != null && networkClient.isConnected()) {
                    attackRequested = true;
                } else {
                    world.handlePlayerAttack();
                }
            }
        }

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

    public int getActiveAimingSkill() {
        return activeAimingSkill;
    }

    public double getCurrentAimX() {
        return currentAimX;
    }

    public double getCurrentAimY() {
        return currentAimY;
    }

    public boolean isShowDebugOverlay() {
        return showDebugOverlay;
    }

    public void toggleDebugOverlay() {
        this.showDebugOverlay = !this.showDebugOverlay;
    }
}
