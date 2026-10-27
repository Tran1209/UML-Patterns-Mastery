package com.huce.oop.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import combat.CombatEngine;
import core.GameWorld;
import core.Player;
import core.Vector2D;
import render.Camera2D;
import render.Direction;
import render.LayerType;
import render.LpcSpriteSheet;
import render.SpriteLayer;

/**
 * SurfaceView trò chơi trên nền tảng Android.
 * Vận hành vòng lặp Game Loop 60 FPS trên luồng phụ độc lập (Worker Thread),
 * xử lý cảm ứng đa điểm (Multi-touch) mượt mà cho Joystick và các phím kỹ năng.
 */
public class AndroidGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private final SurfaceHolder surfaceHolder;

    private GameWorld world;
    private Camera2D camera;
    private AndroidGameRenderer renderer;

    private VirtualJoystick joystick;
    private TouchButton attackBtn;
    private TouchButton weaponBtn;
    private TouchButton respawnBtn;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int currentWeaponIndex = 0;
    private final String[] weaponNames = {"Kiếm Sắt", "Thương Dài", "Chùy Gai"};

    public AndroidGameView(Context context) {
        super(context);
        this.surfaceHolder = getHolder();
        this.surfaceHolder.addCallback(this);
        setFocusable(true);
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
        this.world = new GameWorld("Đấu Sĩ Nam");
        this.camera = new Camera2D(width, height, GameWorld.WORLD_WIDTH, GameWorld.WORLD_HEIGHT);
        this.renderer = new AndroidGameRenderer();

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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pauseGame();
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

            // Kẹp dt để tránh giật lag khi chuyển đổi ứng dụng
            if (dt > 0.1) dt = 0.1;

            // 1. Cập nhật vật lý và trạng thái game
            updateGame(dt);

            // 2. Dựng hình khung hình
            renderGame();

            // 3. Nghỉ ngơi điều tiết tốc độ khung hình
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
        if (world == null) return;
        Player player = world.getPlayer();

        if (joystick != null && player.isAlive()) {
            if (joystick.isPressed()) {
                double jx = joystick.getNormalizedX();
                double jy = joystick.getNormalizedY();
                double speed = player.getMoveSpeed();
                player.setVelocity(new Vector2D(jx * speed, jy * speed));

                Direction dir = joystick.getDirection();
                if (dir != null) {
                    player.setFacingDirection(dir);
                }
            } else {
                player.setVelocity(new Vector2D(0, 0));
            }
        }

        // Cập nhật toàn bộ thực thể & chiến đấu
        world.update(dt);

        // Camera bám theo nhân vật mượt mà
        if (camera != null) {
            camera.update(player.getPosition(), 0.15);
        }
    }

    private void renderGame() {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                int width = getWidth();
                int height = getHeight();

                // 1. Dựng hình thế giới trò chơi
                if (renderer != null && world != null && camera != null) {
                    renderer.render(canvas, world, camera, width, height);
                }

                // 2. Dựng hình các phím ảo cảm ứng (On-screen Controls)
                if (joystick != null) {
                    joystick.draw(canvas, paint);
                }
                if (attackBtn != null) {
                    attackBtn.draw(canvas, paint);
                }
                if (weaponBtn != null) {
                    weaponBtn.draw(canvas, paint);
                }
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
        // Nửa trái màn hình: Ưu tiên Joystick
        if (touchX < getWidth() * 0.5f) {
            if (joystick != null) {
                joystick.onTouchDown(pointerId, touchX, touchY);
            }
        } else {
            // Nửa phải màn hình: Các nút bấm hành động
            if (attackBtn != null && attackBtn.onTouchDown(pointerId, touchX, touchY)) {
                if (!world.getPlayer().isAlive()) {
                    world.restartGame();
                } else {
                    world.handlePlayerAttack();
                }
            } else if (weaponBtn != null && weaponBtn.onTouchDown(pointerId, touchX, touchY)) {
                switchWeapon();
            }
        }
    }

    private void handleTouchUp(int pointerId) {
        if (joystick != null) {
            joystick.onTouchUp(pointerId);
        }
        if (attackBtn != null) {
            attackBtn.onTouchUp(pointerId);
        }
        if (weaponBtn != null) {
            weaponBtn.onTouchUp(pointerId);
        }
    }

    private void switchWeapon() {
        currentWeaponIndex = (currentWeaponIndex + 1) % weaponNames.length;
        String newWpn = weaponNames[currentWeaponIndex];
        // Ứng dụng GoF Builder/Composite Pattern hoán đổi vũ khí động
        if (world != null && world.getPlayerRenderer() != null) {
            world.getPlayerRenderer().getVisual().setLayer(
                    new SpriteLayer(LayerType.WEAPON, new LpcSpriteSheet("weapon", "weapon_" + currentWeaponIndex + ".png"))
            );
            world.getCombatEngine().spawnDamageNumber(
                    world.getPlayer().getPosition().getX(),
                    world.getPlayer().getPosition().getY() - 25,
                    "Trang bị: " + newWpn,
                    CombatEngine.COLOR_YELLOW
            );
        }
    }
}
