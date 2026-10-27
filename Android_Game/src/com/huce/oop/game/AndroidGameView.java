package com.huce.oop.game;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import combat.CombatEngine;
import core.GameWorld;
import core.Player;
import core.Vector2D;
import network.NetworkGameClient;
import render.Camera2D;
import render.Direction;
import render.LayerType;
import render.LpcSpriteSheet;
import render.SpriteLayer;

/**
 * SurfaceView trò chơi trên nền tảng Android.
 * Tích hợp cụm phím kỹ năng hình cánh quạt chuẩn Liên Quân Mobile (AOV):
 * - Nút Đánh Thường lớn ở góc dưới phải
 * - 3 Kỹ năng (Chiêu 1: ⚡ Xung Kích, Chiêu 2: 🔥 Xoáy Lửa, Chiêu 3: ☄️ Thiên Phạt Ultimate)
 *   xếp vòng cung kèm hiệu ứng quạt đếm ngược hồi chiêu (Radial Cooldown Sweep).
 */
public class AndroidGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private final SurfaceHolder surfaceHolder;

    private final String playerName;
    private final String host;
    private final int port;
    private final String roomCode;
    private final Runnable onExitLobby;

    private GameWorld world;
    private NetworkGameClient networkClient;
    private Camera2D camera;
    private AndroidGameRenderer renderer;

    private VirtualJoystick joystick;
    private TouchButton attackBtn;
    private TouchButton skill1Btn;
    private TouchButton skill2Btn;
    private TouchButton skill3Btn;
    private TouchButton cancelSkillBtn;
    private TouchButton weaponBtn;
    private TouchButton exitBtn;
    private TouchButton copyCodeBtn;

    private volatile boolean attackRequested = false;
    private volatile int skillRequested = 0;
    private volatile double skillAimRequestedX = 0.0;
    private volatile double skillAimRequestedY = 0.0;

    // Trạng thái giữ kéo chọn vùng kỹ năng chuẩn Liên Quân Mobile (AOV)
    private volatile int activeAimingSkill = 0;
    private volatile boolean isCancelHovered = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int currentWeaponIndex = 0;
    private final String[] weaponNames = {"Kiếm Sắt", "Thương Dài", "Chùy Gai"};

    public AndroidGameView(Context context, String playerName, String host, int port, String roomCode, Runnable onExitLobby) {
        super(context);
        this.playerName = playerName;
        this.host = host;
        this.port = port;
        this.roomCode = roomCode != null ? roomCode : "SOLO";
        this.onExitLobby = onExitLobby;

        this.surfaceHolder = getHolder();
        this.surfaceHolder.addCallback(this);
        setFocusable(true);
    }

    public AndroidGameView(Context context) {
        this(context, "Đấu Sĩ Nam", null, 0, "SOLO", null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) width = 1280;
        if (height <= 0) height = 720;

        initGame(width, height);

        isRunning = true;
        gameThread = new Thread(this, "GameLoopThread");
        gameThread.start();
    }

    private void initGame(int width, int height) {
        this.world = new GameWorld(playerName);
        this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        this.renderer = new AndroidGameRenderer();

        if (host != null && port > 0) {
            this.networkClient = new NetworkGameClient(host, port, playerName);
            new Thread(() -> {
                boolean connected = networkClient.connect();
                if (!connected) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "⚠️ Không thể kết nối tới Server, chuyển về Chơi Đơn!", Toast.LENGTH_LONG).show();
                    });
                }
            }, "AndroidNet-Connector").start();
        } else {
            this.networkClient = null;
        }

        setupControlLayout(width, height);
    }

    /**
     * Bố trí cụm phím điều khiển chuẩn Liên Quân Mobile (AOV):
     * - Bên trái: Cần Joystick 360 độ
     * - Bên phải: Nút Đánh Thường ở tâm góc phải + 3 Kỹ năng xếp hình vòng cung quạt (172°, 128°, 84°)
     */
    private void setupControlLayout(int width, int height) {
        float minDim = Math.min(width, height);

        // 1. Joystick góc dưới bên trái
        float joyX = Math.max(165f, width * 0.16f);
        float joyY = height - Math.max(155f, height * 0.27f);
        float joyBaseR = minDim * 0.135f;
        float joyKnobR = joyBaseR * 0.45f;
        if (this.joystick == null) {
            this.joystick = new VirtualJoystick(joyX, joyY, joyBaseR, joyKnobR);
        } else {
            this.joystick.setCenter(joyX, joyY);
        }

        // 2. Tâm nút Đánh Thường (Normal Attack) góc dưới bên phải
        float atkR = minDim * 0.115f;
        float atkX = width - atkR * 1.38f;
        float atkY = height - atkR * 1.32f;
        this.attackBtn = new TouchButton(atkX, atkY, atkR, "⚔", "ĐÁNH", Color.rgb(215, 38, 56), Color.rgb(255, 90, 95));

        // 3. Ba nút Kỹ Năng (Skill 1, Skill 2, Skill 3 Ultimate) xếp hình cánh quạt quanh nút Đánh Thường
        float arcRadius = atkR * 2.15f;
        float skillR = atkR * 0.68f;
        float ultR = atkR * 0.76f;

        // Chiêu 1 (Góc 174 độ - Bên trái nút Đánh thường): ⚡ Xung Kích (Kéo thả mũi tên 360°)
        double rad1 = Math.toRadians(174.0);
        float s1X = atkX + (float) (Math.cos(rad1) * arcRadius);
        float s1Y = atkY - (float) (Math.sin(rad1) * arcRadius);
        this.skill1Btn = new TouchButton(s1X, s1Y, skillR, "⚡", "XUNG KÍCH", Color.rgb(0, 145, 234), Color.rgb(64, 196, 255))
                .setDragAimable(true);

        // Chiêu 2 (Góc 130 độ - Chéo trên trái nút Đánh thường): 🔥 Xoáy Lửa (Kéo thả chọn vùng tròn)
        double rad2 = Math.toRadians(130.0);
        float s2X = atkX + (float) (Math.cos(rad2) * arcRadius);
        float s2Y = atkY - (float) (Math.sin(rad2) * arcRadius);
        this.skill2Btn = new TouchButton(s2X, s2Y, skillR, "🔥", "XOÁY LỬA", Color.rgb(230, 81, 0), Color.rgb(255, 145, 0))
                .setDragAimable(true);

        // Chiêu 3 Ultimate (Góc 86 độ - Phía trên nút Đánh thường): ☄️ Thiên Phạt (Kéo thả thiên thạch tầm xa)
        double rad3 = Math.toRadians(86.0);
        float s3X = atkX + (float) (Math.cos(rad3) * arcRadius);
        float s3Y = atkY - (float) (Math.sin(rad3) * arcRadius);
        this.skill3Btn = new TouchButton(s3X, s3Y, ultR, "☄️", "ULTIMATE", Color.rgb(142, 36, 170), Color.rgb(224, 64, 251))
                .setDragAimable(true);

        // Nút Hủy Chiêu (❌ HỦY CHIÊU) xuất hiện phía trên góc phải khi đang giữ kéo kỹ năng chuẩn Liên Quân
        float cancelR = atkR * 0.56f;
        float cancelX = width - cancelR * 1.6f;
        float cancelY = Math.max(185f, s3Y - ultR * 2.15f);
        this.cancelSkillBtn = new TouchButton(cancelX, cancelY, cancelR, "✖", "HỦY CHIÊU", Color.rgb(198, 40, 40), Color.rgb(255, 23, 68));

        // 4. Nút Đổi Vũ Khí nhỏ gọn phía ngoài cùng bên trái cụm kỹ năng
        float wpnR = atkR * 0.50f;
        float wpnX = s1X - skillR * 1.65f;
        float wpnY = s1Y + skillR * 0.15f;
        this.weaponBtn = new TouchButton(wpnX, wpnY, wpnR, "🗡", "ĐỔI VK", Color.rgb(45, 90, 130), Color.rgb(70, 130, 180));

        // 5. Nút Sảnh (Rời phòng về sảnh chờ) góc dưới cùng bên trái
        this.exitBtn = new TouchButton(55f, height - 55f, 30f, "🏠", "SẢNH", Color.rgb(180, 40, 40), Color.rgb(220, 60, 60));

        // 6. Nút Copy mã mời (nếu ở chế độ Multiplayer)
        if (host != null && port > 0) {
            this.copyCodeBtn = new TouchButton(width / 2f, 102f, 24f, "📋", "COPY", Color.rgb(40, 120, 200), Color.rgb(60, 150, 240));
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        }
        setupControlLayout(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pauseGame();
        if (networkClient != null) {
            networkClient.disconnect();
        }
    }

    public void pauseGame() {
        isRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            gameThread = null;
        }
    }

    public void resumeGame() {
        if (!isRunning && surfaceHolder.getSurface().isValid()) {
            isRunning = true;
            gameThread = new Thread(this, "GameLoopThread");
            gameThread.start();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double nsPerTick = 1_000_000_000.0 / 60.0; // 60 FPS

        while (isRunning) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            if (dt > 0.1) dt = 0.1;

            try {
                updateGame(dt);
                renderGame();
            } catch (Throwable ignored) {
                // Bảo vệ tuyệt đối vòng lặp GameLoopThread 60 FPS không bao giờ bị văng khi spam cảm ứng đa điểm
            }

            long elapsedNs = System.nanoTime() - now;
            long sleepMs = (long) ((nsPerTick - elapsedNs) / 1_000_000);
            if (sleepMs > 1) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void updateGame(double dt) {
        double jx = (joystick != null && joystick.isPressed()) ? joystick.getNormalizedX() : 0.0;
        double jy = (joystick != null && joystick.isPressed()) ? joystick.getNormalizedY() : 0.0;
        double speed = 220.0;

        Player player = world != null ? world.getPlayer() : null;

        boolean isAtk = attackRequested;
        int skill = skillRequested;
        double aimX = skillAimRequestedX;
        double aimY = skillAimRequestedY;
        attackRequested = false;
        skillRequested = 0;

        if (networkClient != null && networkClient.isConnected()) {
            // Cập nhật bộ đếm thời gian hồi chiêu cục bộ cho nút bấm
            if (player != null) {
                if (joystick != null && joystick.getDirection() != null) {
                    player.setFacingDirection(joystick.getDirection());
                }
                player.updatePhysics(dt);

                if (skill >= 1 && skill <= 3 && player.canCastSkill(skill)) {
                    player.triggerSkillCooldown(skill);
                    double vxAim = aimX;
                    double vyAim = aimY;
                    if (Math.hypot(vxAim, vyAim) < 0.08) {
                        Direction dir = player.getFacingDirection();
                        vxAim = dir.getDirX() * (skill == 1 ? 1.0 : 0.55);
                        vyAim = dir.getDirY() * (skill == 1 ? 1.0 : 0.55);
                    }
                    world.getCombatEngine().spawnSkillVisualOnly(camera.getX(), camera.getY(), vxAim, vyAim, skill);
                } else if (isAtk && player.canAttack()) {
                    player.triggerAttackCooldown(0.35);
                    Direction dir = player.getFacingDirection();
                    world.getCombatEngine().getSlashEffects().add(new CombatEngine.SlashEffect(
                            camera.getX() + dir.getDirX() * 32,
                            camera.getY() + dir.getDirY() * 32,
                            dir, 0.20
                    ));
                    world.getCombatEngine().spawnHitParticles(
                            camera.getX() + dir.getDirX() * 32,
                            camera.getY() + dir.getDirY() * 32,
                            CombatEngine.COLOR_YELLOW, 5
                    );
                }
            }

            networkClient.sendInput(jx * speed, jy * speed, isAtk, skill, aimX, aimY);
            world.getCombatEngine().updateEffects(dt);
        } else {
            // Chế độ Chơi Đơn Offline: Thực thi đồng bộ trên GameLoopThread chống xung đột luồng 100%
            if (world != null && player != null) {
                if (isAtk) {
                    if (!player.isAlive()) {
                        world.restartGame();
                    } else {
                        world.handlePlayerAttack();
                    }
                }
                if (skill >= 1 && skill <= 3) {
                    world.handlePlayerSkill(skill, aimX, aimY);
                }
                if (player.isAlive()) {
                    player.setVelocity(new Vector2D(jx * speed, jy * speed));
                    if (joystick != null && joystick.getDirection() != null) {
                        player.setFacingDirection(joystick.getDirection());
                    }
                }
                world.update(dt);
                if (camera != null) {
                    camera.update(player.getPosition(), 0.15);
                }
            }
        }

        // Đồng bộ đồng hồ đếm ngược hồi chiêu lên 3 nút Kỹ năng AOV
        if (player != null) {
            if (skill1Btn != null) skill1Btn.setCooldown(player.getSkillCooldown(1), Player.SKILL1_CD);
            if (skill2Btn != null) skill2Btn.setCooldown(player.getSkillCooldown(2), Player.SKILL2_CD);
            if (skill3Btn != null) skill3Btn.setCooldown(player.getSkillCooldown(3), Player.SKILL3_CD);
        }
    }

    private void renderGame() {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                int width = getWidth();
                int height = getHeight();

                if (networkClient != null && networkClient.isConnected()) {
                    renderer.renderMultiplayer(canvas, networkClient, camera, width, height, roomCode, world.getCombatEngine());
                } else if (renderer != null && world != null && camera != null) {
                    renderer.render(canvas, world, camera, width, height);
                }

                // Vẽ Vùng Chọn Kéo Thả / Mũi Tên Định Hướng 360° dưới mặt đất nếu đang giữ nút kỹ năng
                int aimingSkill = activeAimingSkill;
                if (aimingSkill >= 1 && aimingSkill <= 3 && renderer != null && camera != null && world != null) {
                    TouchButton activeBtn = (aimingSkill == 1) ? skill1Btn : ((aimingSkill == 2) ? skill2Btn : skill3Btn);
                    if (activeBtn != null && activeBtn.isPressed()) {
                        double heroX = (networkClient != null && networkClient.isConnected())
                                ? camera.getX()
                                : world.getPlayer().getPosition().getX();
                        double heroY = (networkClient != null && networkClient.isConnected())
                                ? camera.getY()
                                : world.getPlayer().getPosition().getY();
                        renderer.renderSkillTargetIndicator(
                                canvas, camera, heroX, heroY,
                                world.getPlayer().getFacingDirection(),
                                aimingSkill, activeBtn.getAimX(), activeBtn.getAimY(),
                                isCancelHovered
                        );
                    }
                }

                // Vẽ cụm điều khiển cảm ứng chuẩn AOV
                if (joystick != null) joystick.draw(canvas, paint);
                if (attackBtn != null) attackBtn.draw(canvas, paint);
                if (skill1Btn != null) skill1Btn.draw(canvas, paint);
                if (skill2Btn != null) skill2Btn.draw(canvas, paint);
                if (skill3Btn != null) skill3Btn.draw(canvas, paint);
                if (aimingSkill >= 1 && aimingSkill <= 3 && cancelSkillBtn != null) {
                    cancelSkillBtn.draw(canvas, paint);
                }
                if (weaponBtn != null) weaponBtn.draw(canvas, paint);
                if (exitBtn != null) exitBtn.draw(canvas, paint);
                if (copyCodeBtn != null) copyCodeBtn.draw(canvas, paint);
            }
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float touchX = event.getX(pointerIndex);
        float touchY = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleTouchDown(pointerId, touchX, touchY);
                break;

            case MotionEvent.ACTION_MOVE:
                boolean cancelHover = false;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float px = event.getX(i);
                    float py = event.getY(i);
                    if (joystick != null) {
                        joystick.onTouchMove(pId, px, py);
                    }
                    boolean movedSkill = false;
                    if (skill1Btn != null && skill1Btn.onTouchMove(pId, px, py)) movedSkill = true;
                    if (skill2Btn != null && skill2Btn.onTouchMove(pId, px, py)) movedSkill = true;
                    if (skill3Btn != null && skill3Btn.onTouchMove(pId, px, py)) movedSkill = true;

                    if (movedSkill && cancelSkillBtn != null && cancelSkillBtn.containsPoint(px, py)) {
                        cancelHover = true;
                    }
                }
                isCancelHovered = cancelHover;
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleTouchUp(pointerId);
                break;
        }
        return true;
    }

    private void handleTouchDown(int pointerId, float touchX, float touchY) {
        if (exitBtn != null && exitBtn.onTouchDown(pointerId, touchX, touchY)) {
            exitToLobby();
            return;
        }

        if (copyCodeBtn != null && copyCodeBtn.onTouchDown(pointerId, touchX, touchY)) {
            copyInviteCode();
            return;
        }

        // Nửa trái màn hình: Joystick
        if (touchX < getWidth() * 0.45f) {
            if (joystick != null) {
                joystick.onTouchDown(pointerId, touchX, touchY);
            }
            return;
        }

        // Nửa phải màn hình: Cụm phím Đánh Thường + 3 Chiêu AOV (Giữ để kéo vùng chọn, thả tay để tung chiêu)
        if (skill1Btn != null && skill1Btn.onTouchDown(pointerId, touchX, touchY)) {
            activeAimingSkill = 1;
            isCancelHovered = false;
        } else if (skill2Btn != null && skill2Btn.onTouchDown(pointerId, touchX, touchY)) {
            activeAimingSkill = 2;
            isCancelHovered = false;
        } else if (skill3Btn != null && skill3Btn.onTouchDown(pointerId, touchX, touchY)) {
            activeAimingSkill = 3;
            isCancelHovered = false;
        } else if (attackBtn != null && attackBtn.onTouchDown(pointerId, touchX, touchY)) {
            attackRequested = true;
        } else if (weaponBtn != null && weaponBtn.onTouchDown(pointerId, touchX, touchY)) {
            switchWeapon();
        }
    }

    private void triggerSkill(int skillId, double aimX, double aimY) {
        Player player = world != null ? world.getPlayer() : null;
        if (player == null || !player.canCastSkill(skillId)) return;

        skillRequested = skillId;
        skillAimRequestedX = aimX;
        skillAimRequestedY = aimY;
    }

    private void handleTouchUp(int pointerId) {
        if (joystick != null) joystick.onTouchUp(pointerId);
        if (attackBtn != null) attackBtn.onTouchUp(pointerId);

        if (skill1Btn != null) {
            double ax = skill1Btn.getAimX();
            double ay = skill1Btn.getAimY();
            if (skill1Btn.onTouchUp(pointerId)) {
                if (!isCancelHovered) {
                    triggerSkill(1, ax, ay);
                } else {
                    showCancelToast();
                }
                activeAimingSkill = 0;
                isCancelHovered = false;
            }
        }
        if (skill2Btn != null) {
            double ax = skill2Btn.getAimX();
            double ay = skill2Btn.getAimY();
            if (skill2Btn.onTouchUp(pointerId)) {
                if (!isCancelHovered) {
                    triggerSkill(2, ax, ay);
                } else {
                    showCancelToast();
                }
                activeAimingSkill = 0;
                isCancelHovered = false;
            }
        }
        if (skill3Btn != null) {
            double ax = skill3Btn.getAimX();
            double ay = skill3Btn.getAimY();
            if (skill3Btn.onTouchUp(pointerId)) {
                if (!isCancelHovered) {
                    triggerSkill(3, ax, ay);
                } else {
                    showCancelToast();
                }
                activeAimingSkill = 0;
                isCancelHovered = false;
            }
        }

        if (weaponBtn != null) weaponBtn.onTouchUp(pointerId);
        if (exitBtn != null) exitBtn.onTouchUp(pointerId);
        if (copyCodeBtn != null) copyCodeBtn.onTouchUp(pointerId);
    }

    private void showCancelToast() {
        if (world != null && camera != null) {
            world.getCombatEngine().spawnDamageNumber(
                    camera.getX(),
                    camera.getY() - 35,
                    "✖ ĐÃ HỦY CHIÊU",
                    CombatEngine.COLOR_RED
            );
        }
    }

    private void exitToLobby() {
        pauseGame();
        if (networkClient != null) {
            networkClient.disconnect();
        }
        if (onExitLobby != null) {
            new Handler(Looper.getMainLooper()).post(onExitLobby);
        }
    }

    private void copyInviteCode() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("GameRoomCode", roomCode);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "📋 Đã sao chép mã phòng: " + roomCode, Toast.LENGTH_SHORT).show();
        }
    }

    private void switchWeapon() {
        currentWeaponIndex = (currentWeaponIndex + 1) % weaponNames.length;
        String newWpn = weaponNames[currentWeaponIndex];
        if (world != null && world.getPlayerRenderer() != null) {
            world.getPlayerRenderer().getVisual().setLayer(
                    new SpriteLayer(LayerType.WEAPON, new LpcSpriteSheet("weapon", "weapon_" + currentWeaponIndex + ".png"))
            );
            world.getCombatEngine().spawnDamageNumber(
                    camera.getX(),
                    camera.getY() - 25,
                    "Trang bị: " + newWpn,
                    CombatEngine.COLOR_YELLOW
            );
        }
    }
}
