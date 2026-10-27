package com.huce.oop.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import combat.CombatEngine;
import core.GameWorld;
import core.Monster;
import core.Player;
import core.Vector2D;
import render.Camera2D;
import render.Direction;
import ui.HudObserver;

import java.util.List;

/**
 * Bộ dựng hình đồ họa Game Canvas 2D tối ưu hóa cho màn hình cảm ứng Android.
 * Render mượt mà 60 FPS không cần nạp tệp ảnh rời, chống crash 100%.
 */
public class AndroidGameRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private final Path slashPath = new Path();

    public AndroidGameRenderer() {
        strokePaint.setStyle(Paint.Style.STROKE);
    }

    public void render(Canvas canvas, GameWorld world, Camera2D camera, int screenWidth, int screenHeight) {
        if (canvas == null || world == null || camera == null) return;

        // 1. Vẽ mặt đất đấu trường (Arena Floor)
        renderArenaFloor(canvas, camera, screenWidth, screenHeight);

        // 2. Vẽ hiệu ứng chém kiếm (Slash Arcs)
        renderSlashes(canvas, world.getCombatEngine(), camera);

        // 3. Vẽ Quái vật (Monsters)
        for (Monster monster : world.getMonsters()) {
            renderMonster(canvas, monster, camera);
        }

        // 4. Vẽ Người chơi từ xa (Remote Players trong Multiplayer)
        for (Player remotePlayer : world.getRemotePlayers()) {
            renderPlayerCharacter(canvas, remotePlayer, camera, false);
        }

        // 5. Vẽ Người chơi chính (Local Player)
        renderPlayerCharacter(canvas, world.getPlayer(), camera, true);

        // 6. Vẽ Tia lửa tóe ra khi va chạm (Hit Particles)
        renderParticles(canvas, world.getCombatEngine(), camera);

        // 7. Vẽ Số sát thương nảy lên (Floating Combat Text)
        renderFloatingTexts(canvas, world.getCombatEngine(), camera);

        // 8. Vẽ Giao diện chỉ số (HUD Overhead & Minimap Radar)
        renderHUD(canvas, world, screenWidth, screenHeight);
    }

    private void renderArenaFloor(Canvas canvas, Camera2D camera, int screenW, int screenH) {
        // Nền đen sẫm tổng thể
        canvas.drawColor(Color.rgb(18, 22, 28));

        // Mặt sàn đấu trường 1600x1200
        float left = (float) camera.worldToScreenX(0);
        float top = (float) camera.worldToScreenY(0);
        float right = (float) camera.worldToScreenX(GameWorld.WORLD_WIDTH);
        float bottom = (float) camera.worldToScreenY(GameWorld.WORLD_HEIGHT);

        // Sàn đá đấu trường
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(36, 42, 54));
        canvas.drawRect(left, top, right, bottom, paint);

        // Các đường lưới ô gạch đá 128x128
        paint.setColor(Color.argb(45, 255, 255, 255));
        paint.setStrokeWidth(1.5f);
        paint.setStyle(Paint.Style.STROKE);

        for (double gx = 0; gx <= GameWorld.WORLD_WIDTH; gx += 128) {
            float sx = (float) camera.worldToScreenX(gx);
            canvas.drawLine(sx, top, sx, bottom, paint);
        }
        for (double gy = 0; gy <= GameWorld.WORLD_HEIGHT; gy += 128) {
            float sy = (float) camera.worldToScreenY(gy);
            canvas.drawLine(left, sy, right, sy, paint);
        }

        // Tường thành bảo vệ bao quanh đấu trường
        strokePaint.setColor(Color.rgb(0, 180, 216));
        strokePaint.setStrokeWidth(8f);
        canvas.drawRect(left, top, right, bottom, strokePaint);
    }

    private void renderPlayerCharacter(Canvas canvas, Player player, Camera2D camera, boolean isLocal) {
        if (player == null) return;
        float sx = (float) camera.worldToScreenX(player.getPosition().getX());
        float sy = (float) camera.worldToScreenY(player.getPosition().getY());

        // Bóng đổ dưới chân
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 0, 0, 0));
        rectF.set(sx - 20, sy + 18, sx + 20, sy + 28);
        canvas.drawOval(rectF, paint);

        // Chân & Giày
        paint.setColor(Color.rgb(60, 40, 20));
        canvas.drawCircle(sx - 9, sy + 14, 6, paint);
        canvas.drawCircle(sx + 9, sy + 14, 6, paint);

        // Thân áo / Giáp ngực (Composite Pattern Layer)
        paint.setColor(isLocal ? Color.rgb(20, 120, 220) : Color.rgb(200, 150, 40));
        rectF.set(sx - 14, sy - 8, sx + 14, sy + 14);
        canvas.drawRoundRect(rectF, 4, 4, paint);

        // Viền giáp
        strokePaint.setColor(Color.rgb(255, 215, 0));
        strokePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(rectF, 4, 4, strokePaint);

        // Đầu & Khuôn mặt
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 219, 172)); // Màu da
        canvas.drawCircle(sx, sy - 18, 12, paint);

        // Tóc (Hair Layer)
        paint.setColor(Color.rgb(50, 30, 15));
        rectF.set(sx - 13, sy - 31, sx + 13, sy - 15);
        canvas.drawArc(rectF, 180, 180, true, paint);

        // Mũ chiến binh / Băng đô
        strokePaint.setColor(Color.rgb(220, 40, 40));
        strokePaint.setStrokeWidth(3f);
        canvas.drawLine(sx - 12, sy - 21, sx + 12, sy - 21, strokePaint);

        // Mắt theo hướng nhìn
        paint.setColor(Color.BLACK);
        Direction dir = player.getFacingDirection();
        float eyeOffX = (float) dir.getDirX() * 4;
        float eyeOffY = (float) dir.getDirY() * 3;
        canvas.drawCircle(sx - 4 + eyeOffX, sy - 19 + eyeOffY, 2f, paint);
        canvas.drawCircle(sx + 4 + eyeOffX, sy - 19 + eyeOffY, 2f, paint);

        // Vũ khí (Kiếm chiến binh)
        float wpx = sx + (float) dir.getDirX() * 22;
        float wpy = sy + (float) dir.getDirY() * 18;
        strokePaint.setColor(Color.rgb(220, 220, 240));
        strokePaint.setStrokeWidth(4f);
        canvas.drawLine(sx, sy, wpx, wpy, strokePaint);

        // Tên nhân vật & Thanh máu
        paint.setTextSize(20f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(isLocal ? Color.CYAN : Color.YELLOW);
        canvas.drawText(player.getPlayerName(), sx, sy - 40, paint);

        // Thanh máu
        float hpW = 50f;
        float hpH = 6f;
        float hpRatio = Math.max(0f, (float) player.getCurrentHp() / player.getMaxHp());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 40, 0, 0));
        canvas.drawRect(sx - hpW / 2, sy - 36, sx + hpW / 2, sy - 36 + hpH, paint);
        paint.setColor(Color.rgb(0, 230, 118));
        canvas.drawRect(sx - hpW / 2, sy - 36, sx - hpW / 2 + hpW * hpRatio, sy - 36 + hpH, paint);
    }

    private void renderMonster(Canvas canvas, Monster monster, Camera2D camera) {
        if (monster == null || !monster.isAlive()) return;
        float sx = (float) camera.worldToScreenX(monster.getPosition().getX());
        float sy = (float) camera.worldToScreenY(monster.getPosition().getY());

        // Bóng đổ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 0, 0, 0));
        rectF.set(sx - 18, sy + 16, sx + 18, sy + 24);
        canvas.drawOval(rectF, paint);

        switch (monster.getType()) {
            case GOBLIN:
                // Thân Goblin xanh lá
                paint.setColor(Color.rgb(46, 139, 87));
                canvas.drawCircle(sx, sy, 16, paint);
                // Tai nhọn Goblin
                Path earPath = new Path();
                earPath.moveTo(sx - 16, sy - 4);
                earPath.lineTo(sx - 26, sy - 14);
                earPath.lineTo(sx - 10, sy - 10);
                canvas.drawPath(earPath, paint);
                Path earPath2 = new Path();
                earPath2.moveTo(sx + 16, sy - 4);
                earPath2.lineTo(sx + 26, sy - 14);
                earPath2.lineTo(sx + 10, sy - 10);
                canvas.drawPath(earPath2, paint);
                // Mắt đỏ hung hăng
                paint.setColor(Color.RED);
                canvas.drawCircle(sx - 5, sy - 4, 3, paint);
                canvas.drawCircle(sx + 5, sy - 4, 3, paint);
                break;

            case ORC_WARRIOR:
                // Orc Chiến Binh cơ bắp xám đen to lớn
                paint.setColor(Color.rgb(105, 105, 105));
                rectF.set(sx - 22, sy - 20, sx + 22, sy + 20);
                canvas.drawRoundRect(rectF, 8, 8, paint);
                // Giáp vai gai nhọn
                paint.setColor(Color.rgb(180, 50, 50));
                canvas.drawCircle(sx - 20, sy - 12, 8, paint);
                canvas.drawCircle(sx + 20, sy - 12, 8, paint);
                // Sừng chiến binh
                strokePaint.setColor(Color.rgb(230, 230, 200));
                strokePaint.setStrokeWidth(4f);
                canvas.drawLine(sx - 12, sy - 20, sx - 22, sy - 34, strokePaint);
                canvas.drawLine(sx + 12, sy - 20, sx + 22, sy - 34, strokePaint);
                break;

            case SLIME:
                // Slime ngọc bích phát sáng
                paint.setColor(Color.rgb(0, 230, 118));
                rectF.set(sx - 18, sy - 12, sx + 18, sy + 18);
                canvas.drawOval(rectF, paint);
                // Lõi nhân Slime
                paint.setColor(Color.rgb(178, 255, 89));
                canvas.drawCircle(sx, sy + 2, 8, paint);
                // Mắt đen tròn
                paint.setColor(Color.BLACK);
                canvas.drawCircle(sx - 6, sy - 2, 2.5f, paint);
                canvas.drawCircle(sx + 6, sy - 2, 2.5f, paint);
                break;
        }

        // Tên quái & Thanh máu
        paint.setTextSize(18f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 160, 160));
        canvas.drawText(monster.getType().displayName, sx, sy - 32, paint);

        float hpW = 44f;
        float hpH = 5f;
        float hpRatio = Math.max(0f, (float) monster.getCurrentHp() / monster.getMaxHp());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 50, 0, 0));
        canvas.drawRect(sx - hpW / 2, sy - 28, sx + hpW / 2, sy - 28 + hpH, paint);
        paint.setColor(Color.rgb(255, 50, 50));
        canvas.drawRect(sx - hpW / 2, sy - 28, sx - hpW / 2 + hpW * hpRatio, sy - 28 + hpH, paint);
    }

    private void renderSlashes(Canvas canvas, CombatEngine ce, Camera2D camera) {
        if (ce == null) return;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(6f);

        for (CombatEngine.SlashEffect slash : ce.getSlashEffects()) {
            float sx = (float) camera.worldToScreenX(slash.x);
            float sy = (float) camera.worldToScreenY(slash.y);
            float alpha = (float) Math.max(0f, slash.lifeTime / slash.maxLifeTime);

            strokePaint.setColor(Color.argb((int) (alpha * 220), 255, 215, 0));
            rectF.set(sx - 35, sy - 35, sx + 35, sy + 35);
            float startAngle = slash.direction.getRowOffset() * 90f - 45f;
            canvas.drawArc(rectF, startAngle, 90f, false, strokePaint);
        }
    }

    private void renderParticles(Canvas canvas, CombatEngine ce, Camera2D camera) {
        if (ce == null) return;
        paint.setStyle(Paint.Style.FILL);

        for (CombatEngine.HitParticle p : ce.getParticles()) {
            float sx = (float) camera.worldToScreenX(p.x);
            float sy = (float) camera.worldToScreenY(p.y);
            float alpha = (float) Math.max(0f, p.lifeTime / p.maxLifeTime);

            int c = p.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            paint.setColor(Color.argb((int) (alpha * 255), r, g, b));
            canvas.drawCircle(sx, sy, (float) p.size, paint);
        }
    }

    private void renderFloatingTexts(Canvas canvas, CombatEngine ce, Camera2D camera) {
        if (ce == null) return;
        paint.setTextSize(26f);
        paint.setTextAlign(Paint.Align.CENTER);

        for (CombatEngine.FloatingText ft : ce.getFloatingTexts()) {
            float sx = (float) camera.worldToScreenX(ft.x);
            float sy = (float) camera.worldToScreenY(ft.y);
            float alpha = (float) Math.max(0f, ft.lifeTime / ft.maxLifeTime);

            int c = ft.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            // Đổ bóng chữ đen
            paint.setColor(Color.argb((int) (alpha * 200), 0, 0, 0));
            canvas.drawText(ft.text, sx + 2, sy + 2, paint);

            // Chữ màu chính
            paint.setColor(Color.argb((int) (alpha * 255), r, g, b));
            canvas.drawText(ft.text, sx, sy, paint);
        }
    }

    private void renderHUD(Canvas canvas, GameWorld world, int screenW, int screenH) {
        Player player = world.getPlayer();
        HudObserver hud = world.getHudObserver();

        // 1. Thẻ trạng thái nhân vật (Top-Left HUD Panel)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 15, 20, 30));
        rectF.set(24, 24, 380, 130);
        canvas.drawRoundRect(rectF, 12, 12, paint);

        strokePaint.setColor(Color.argb(140, 0, 200, 255));
        strokePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(rectF, 12, 12, strokePaint);

        // Tên & Huy hiệu
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(24f);
        paint.setColor(Color.WHITE);
        canvas.drawText(player.getPlayerName(), 40, 58, paint);

        paint.setTextSize(18f);
        paint.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("⚔ KILLS: " + player.getKillCount() + "  |  ĐỢT: " + world.getCurrentWave(), 40, 84, paint);

        // Thanh máu dài
        float barX = 40;
        float barY = 96;
        float barW = 320;
        float barH = 16;
        float hpRatio = hud.getHpPercentage();

        paint.setColor(Color.argb(200, 60, 10, 10));
        canvas.drawRect(barX, barY, barX + barW, barY + barH, paint);

        paint.setColor(Color.rgb(0, 230, 118));
        canvas.drawRect(barX, barY, barX + barW * hpRatio, barY + barH, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(hud.getPlayerCurrentHp() + " / " + hud.getPlayerMaxHp() + " HP", barX + barW / 2, barY + 13, paint);

        // 2. Radar thu nhỏ (Top-Right Minimap)
        float mapW = 160f;
        float mapH = 120f;
        float mapX = screenW - mapW - 24;
        float mapY = 24;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(190, 10, 15, 25));
        rectF.set(mapX, mapY, mapX + mapW, mapY + mapH);
        canvas.drawRoundRect(rectF, 8, 8, paint);

        strokePaint.setColor(Color.argb(160, 0, 220, 255));
        strokePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(rectF, 8, 8, strokePaint);

        // Điểm quái vật trên radar (chấm đỏ)
        paint.setColor(Color.RED);
        float scaleX = mapW / (float) GameWorld.WORLD_WIDTH;
        float scaleY = mapH / (float) GameWorld.WORLD_HEIGHT;

        for (Monster m : world.getMonsters()) {
            if (m.isAlive()) {
                float mx = mapX + (float) m.getPosition().getX() * scaleX;
                float my = mapY + (float) m.getPosition().getY() * scaleY;
                canvas.drawCircle(mx, my, 3f, paint);
            }
        }

        // Điểm người chơi chính (chấm xanh ngọc phát sáng)
        paint.setColor(Color.CYAN);
        float px = mapX + (float) player.getPosition().getX() * scaleX;
        float py = mapY + (float) player.getPosition().getY() * scaleY;
        canvas.drawCircle(px, py, 4.5f, paint);

        // 3. Thông báo diệt quái (Banner thông báo đầu màn hình)
        String killNotif = hud.getLastKillNotification();
        if (killNotif != null && !killNotif.isEmpty()) {
            paint.setTextSize(22f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.rgb(255, 235, 59));
            canvas.drawText("⭐ " + killNotif, screenW / 2f, 50, paint);
        }

        // 4. Nếu người chơi tử trận
        if (!player.isAlive()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRect(0, 0, screenW, screenH, paint);

            paint.setTextSize(48f);
            paint.setColor(Color.RED);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("BẠN ĐÃ TỬ TRẬN!", screenW / 2f, screenH / 2f - 20, paint);

            paint.setTextSize(26f);
            paint.setColor(Color.WHITE);
            canvas.drawText("Chạm vào nút Tấn Công để Hồi Sinh tiếp tục chiến đấu", screenW / 2f, screenH / 2f + 30, paint);
        }
    }
}
