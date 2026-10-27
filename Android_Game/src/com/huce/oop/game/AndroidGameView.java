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
 * Vận hành vòng lặp Game Loop 60 FPS trên luồng phụ độc lập (Worker Thread),
 * hỗ trợ cả Chơi Đơn (Offline Solo) lẫn Đấu Mạng Thời Gian Thực (Multiplayer Server).
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
    private TouchButton weaponBtn;
    private TouchButton exitBtn;
    private TouchButton copyCodeBtn;

    private volatile boolean attackRequested = false;
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

        // Nếu là chế độ Multiplayer -> Khởi tạo Network Client kết nối tới Dedicated Server
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

        // 1. Joystick góc dưới bên trái
        float joyX = Math.max(160f, width * 0.16f);
        float joyY = height - Math.max(160f, height * 0.28f);
        float joyBaseR = Math.min(width, height) * 0.14f;
        float joyKnobR = joyBaseR * 0.45f;
        this.joystick = new VirtualJoystick(joyX, joyY, joyBaseR, joyKnobR);

        // 2. Nút Tấn công to góc dưới bên phải
        float atkW = Math.min(width, height) * 0.12f;
        float atkX = width - atkW * 1.6f;
        float atkY = height - atkW * 1.5f;
        this.attackBtn = new TouchButton(atkX, atkY, atkW, "⚔", "TẤN CÔNG", Color.rgb(215, 38, 56), Color.rgb(255, 90, 95));

        // 3. Nút Đổi vũ khí phía trên nút Tấn công
        float wpnW = atkW * 0.72f;
        float wpnX = atkX - atkW * 1.25f;
        float wpnY = atkY + atkW * 0.1f;
        this.weaponBtn = new TouchButton(wpnX, wpnY, wpnW, "🗡", "ĐỔI VK", Color.rgb(33, 118, 174), Color.rgb(65, 173, 237));

        // 4. Nút Sảnh (Rời phòng về sảnh chờ) góc dưới bên trái
        this.exitBtn = new TouchButton(60f, height - 60f, 32f, "🏠", "SẢNH", Color.rgb(180, 40, 40), Color.rgb(220, 60, 60));

        // 5. Nút Copy mã mời (nếu ở chế độ Multiplayer)
        if (host != null && port > 0) {
            this.copyCodeBtn = new TouchButton(width / 2f, 102f, 24f, "📋", "COPY", Color.rgb(40, 120, 200), Color.rgb(60, 150, 240));
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        }
        if (joystick != null) {
            float joyX = Math.max(160f, width * 0.16f);
            float joyY = height - Math.max(160f, height * 0.28f);
            joystick.setCenter(joyX, joyY);
        }
        if (attackBtn != null) {
            float atkW = Math.min(width, height) * 0.12f;
            float atkX = width - atkW * 1.6f;
            float atkY = height - atkW * 1.5f;
            attackBtn.setCenter(atkX, atkY);
            if (weaponBtn != null) {
                float wpnX = atkX - atkW * 1.25f;
                float wpnY = atkY + atkW * 0.1f;
                weaponBtn.setCenter(wpnX, wpnY);
            }
        }
        if (exitBtn != null) {
            exitBtn.setCenter(60f, height - 60f);
        }
        if (copyCodeBtn != null) {
            copyCodeBtn.setCenter(width / 2f, 102f);
        }
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

            updateGame(dt);
            renderGame();

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

        if (networkClient != null && networkClient.isConnected()) {
            // Chế độ Đấu Mạng: Gửi vận tốc & cờ tấn công lên Dedicated Server
            boolean isAtk = attackRequested;
            attackRequested = false;
            networkClient.sendInput(jx * speed, jy * speed, isAtk);

            // Cập nhật hiệu ứng thị giác cục bộ
            world.getCombatEngine().updateEffects(dt);
        } else {
            // Chế độ Chơi Đơn Offline
            if (world != null) {
                Player player = world.getPlayer();
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
    }

    private void renderGame() {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                int width = getWidth();
                int height = getHeight();

                if (networkClient != null && networkClient.isConnected()) {
                    // Dựng hình chế độ Đấu Mạng Multiplayer với Snapshot Interpolator LERP
                    renderer.renderMultiplayer(canvas, networkClient, camera, width, height, roomCode, world.getCombatEngine());
                } else if (renderer != null && world != null && camera != null) {
                    // Dựng hình chế độ Chơi Đơn Offline
                    renderer.render(canvas, world, camera, width, height);
                }

                // Vẽ các nút điều khiển cảm ứng
                if (joystick != null) joystick.draw(canvas, paint);
                if (attackBtn != null) attackBtn.draw(canvas, paint);
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
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float px = event.getX(i);
                    float py = event.getY(i);
                    if (joystick != null) {
                        joystick.onTouchMove(pId, px, py);
                    }
                }
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
        // 1. Kiểm tra nút Thoát về Sảnh
        if (exitBtn != null && exitBtn.onTouchDown(pointerId, touchX, touchY)) {
            exitToLobby();
            return;
        }

        // 2. Kiểm tra nút Copy mã mời
        if (copyCodeBtn != null && copyCodeBtn.onTouchDown(pointerId, touchX, touchY)) {
            copyInviteCode();
            return;
        }

        // 3. Nửa trái màn hình: Joystick
        if (touchX < getWidth() * 0.5f) {
            if (joystick != null) {
                joystick.onTouchDown(pointerId, touchX, touchY);
            }
        } else {
            // Nửa phải màn hình: Các nút bấm hành động
            if (attackBtn != null && attackBtn.onTouchDown(pointerId, touchX, touchY)) {
                attackRequested = true;
                if (networkClient == null || !networkClient.isConnected()) {
                    if (!world.getPlayer().isAlive()) {
                        world.restartGame();
                    } else {
                        world.handlePlayerAttack();
                    }
                } else {
                    // Xuất chiêu hiệu ứng chém kiếm ngay lập tức cho mượt
                    Direction dir = joystick != null && joystick.getDirection() != null ? joystick.getDirection() : Direction.DOWN;
                    world.getCombatEngine().spawnHitParticles(
                            camera.getX() + dir.getDirX() * 32,
                            camera.getY() + dir.getDirY() * 32,
                            CombatEngine.COLOR_YELLOW, 6
                    );
                }
            } else if (weaponBtn != null && weaponBtn.onTouchDown(pointerId, touchX, touchY)) {
                switchWeapon();
            }
        }
    }

    private void handleTouchUp(int pointerId) {
        if (joystick != null) joystick.onTouchUp(pointerId);
        if (attackBtn != null) attackBtn.onTouchUp(pointerId);
        if (weaponBtn != null) weaponBtn.onTouchUp(pointerId);
        if (exitBtn != null) exitBtn.onTouchUp(pointerId);
        if (copyCodeBtn != null) copyCodeBtn.onTouchUp(pointerId);
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
        ClipData clip = ClipData.newPlainText("GameRoomCode", "Vào chơi cùng mình nhé! Mã phòng: " + roomCode);
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
