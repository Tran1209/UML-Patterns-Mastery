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

    /**
     * Dựng hình thế giới Đấu Mạng Thời Gian Thực (Authoritative Server + Snapshot Interpolation LERP).
     * Đồng bộ mượt mà giữa máy tính PC và điện thoại Android trên cùng một sàn đấu!
     */
    public void renderMultiplayer(Canvas canvas, network.NetworkGameClient networkClient, Camera2D camera, int screenW, int screenH, String roomCode, CombatEngine ce) {
        if (canvas == null || networkClient == null || camera == null) return;

        long clientTime = System.currentTimeMillis();
        String myId = networkClient.getAssignedPlayerId();
        java.util.Map<String, network.WorldSnapshotPayload.EntityState> states = networkClient.getLatestStates();

        // 1. Tìm vị trí nhân vật cục bộ để điều khiển Camera
        Vector2D myInterpolatedPos = null;
        network.WorldSnapshotPayload.EntityState myState = null;

        if (myId != null && states.containsKey(myId)) {
            myState = states.get(myId);
            myInterpolatedPos = networkClient.getInterpolatedPosition(myId, clientTime);
        }

        if (myInterpolatedPos != null) {
            camera.update(myInterpolatedPos, 0.15);
        }

        // 2. Vẽ mặt đất đấu trường
        renderArenaFloor(canvas, camera, screenW, screenH);

        // 3. Vẽ hiệu ứng chém kiếm và tia lửa
        if (ce != null) {
            renderSlashes(canvas, ce, camera);
            renderParticles(canvas, ce, camera);
            renderFloatingTexts(canvas, ce, camera);
        }

        // 4. Vẽ tất cả thực thể nhận từ Snapshot thông qua SnapshotInterpolator LERP
        int monsterCount = 0;
        int playerCount = 0;

        for (network.WorldSnapshotPayload.EntityState state : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(state.id, clientTime);
            if (pos == null) {
                pos = new Vector2D(state.posX, state.posY);
            }

            if (state.isMonster) {
                monsterCount++;
                renderSnapshotMonster(canvas, state, pos, camera);
            } else {
                playerCount++;
                boolean isLocal = (myId != null && state.id.equals(myId));
                renderSnapshotPlayer(canvas, state, pos, camera, isLocal);
            }
        }

        // 5. Vẽ giao diện HUD Multiplayer
        renderMultiplayerHUD(canvas, myState, networkClient.getCurrentWave(), roomCode, playerCount, monsterCount, screenW, screenH, states, clientTime, networkClient);
    }

    /**
     * Vẽ Vùng Chọn Kéo Thả / Mũi Tên Định Hướng 360° chuẩn Liên Quân Mobile (AOV Skill Indicator).
     * - Chiêu 1 (⚡ Xung Kích): Mũi tên băng giá xoay 360° quanh chân tướng + Điểm đáp.
     * - Chiêu 2 (🔥 Vòng Xoáy Lửa): Vòng giới hạn tầm xa 210px + Vòng tròn lửa AoE 120px di chuyển theo tay kéo.
     * - Chiêu 3 (☄️ Thiên Phạt Ultimate): Vòng tầm xa 280px + Hồng tâm thiên thạch 165px di chuyển theo tay kéo.
     * - Khi kéo tay vào nút HỦY CHIÊU: Toàn bộ vùng chọn chuyển sang màu ĐỎ CẢNH BÁO!
     */
    public void renderSkillTargetIndicator(Canvas canvas, Camera2D camera, double heroWorldX, double heroWorldY,
                                           Direction fallbackDir, int activeSkillId, double aimX, double aimY, boolean isCancelHovered) {
        if (canvas == null || camera == null || activeSkillId < 1 || activeSkillId > 3) return;

        float sx = (float) camera.worldToScreenX(heroWorldX);
        float sy = (float) camera.worldToScreenY(heroWorldY);

        double mag = Math.hypot(aimX, aimY);
        if (mag < 0.06 && fallbackDir != null) {
            aimX = fallbackDir.getDirX() * (activeSkillId == 1 ? 1.0 : 0.55);
            aimY = fallbackDir.getDirY() * (activeSkillId == 1 ? 1.0 : 0.55);
            mag = Math.hypot(aimX, aimY);
        }
        if (mag > 1.0) {
            aimX /= mag;
            aimY /= mag;
            mag = 1.0;
        }

        int mainR, mainG, mainB;
        if (isCancelHovered) {
            mainR = 255; mainG = 45; mainB = 45; // Đỏ cảnh báo hủy chiêu
        } else if (activeSkillId == 1) {
            mainR = 0; mainG = 229; mainB = 255; // Xanh băng giá
        } else if (activeSkillId == 2) {
            mainR = 255; mainG = 145; mainB = 0; // Cam rực lửa
        } else {
            mainR = 224; mainG = 64; mainB = 251; // Tím Hoàng Kim Ultimate
        }

        if (activeSkillId == 1) {
            // CHIÊU 1: MŨI TÊN ĐỊNH HƯỚNG 360° (Directional Skillshot Arrow)
            float range = (float) CombatEngine.SKILL1_MAX_RANGE;
            double ux = (mag > 0.01) ? (aimX / mag) : 0.0;
            double uy = (mag > 0.01) ? (aimY / mag) : 1.0;
            float endX = sx + (float) (ux * range);
            float endY = sy + (float) (uy * range);

            // 1. Vòng tròn tầm lướt quanh chân tướng
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(22, mainR, mainG, mainB));
            canvas.drawCircle(sx, sy, range, paint);

            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2.5f);
            strokePaint.setColor(Color.argb(120, mainR, mainG, mainB));
            canvas.drawCircle(sx, sy, range, strokePaint);

            // 2. Thân mũi tên định hướng (Corridor Box + Arrowhead)
            float px = (float) -uy;
            float py = (float) ux;
            float halfW = 24f;
            float headLen = 36f;
            float bodyEndX = endX - (float) (ux * headLen);
            float bodyEndY = endY - (float) (uy * headLen);

            slashPath.reset();
            slashPath.moveTo(sx + px * halfW, sy + py * halfW);
            slashPath.lineTo(bodyEndX + px * halfW, bodyEndY + py * halfW);
            slashPath.lineTo(bodyEndX + px * (halfW * 1.65f), bodyEndY + py * (halfW * 1.65f));
            slashPath.lineTo(endX, endY);
            slashPath.lineTo(bodyEndX - px * (halfW * 1.65f), bodyEndY - py * (halfW * 1.65f));
            slashPath.lineTo(bodyEndX - px * halfW, bodyEndY - py * halfW);
            slashPath.lineTo(sx - px * halfW, sy - py * halfW);
            slashPath.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(85, mainR, mainG, mainB));
            canvas.drawPath(slashPath, paint);

            strokePaint.setStrokeWidth(4f);
            strokePaint.setColor(Color.argb(230, mainR, mainG, mainB));
            canvas.drawPath(slashPath, strokePaint);

            // 3. Tâm điểm đáp ở đầu mũi tên
            canvas.drawCircle(endX, endY, 18f, strokePaint);
        } else {
            // CHIÊU 2 & CHIÊU 3: CHỌN VÙNG TRÒN MẶT ĐẤT (Ground AoE Target Circle)
            float maxRange = (float) (activeSkillId == 2 ? CombatEngine.SKILL2_MAX_RANGE : CombatEngine.SKILL3_MAX_RANGE);
            float aoeRadius = (float) (activeSkillId == 2 ? CombatEngine.SKILL2_AOE_RADIUS : CombatEngine.SKILL3_AOE_RADIUS);

            float targetX = sx + (float) (aimX * maxRange);
            float targetY = sy + (float) (aimY * maxRange);

            // 1. Vòng tròn giới hạn tầm thi triển lớn quanh tướng
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(18, mainR, mainG, mainB));
            canvas.drawCircle(sx, sy, maxRange, paint);

            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2.5f);
            strokePaint.setColor(Color.argb(135, mainR, mainG, mainB));
            canvas.drawCircle(sx, sy, maxRange, strokePaint);

            // 2. Đường dẫn tia ngắm từ chân tướng tới tâm vùng chọn
            strokePaint.setStrokeWidth(3f);
            strokePaint.setColor(Color.argb(170, mainR, mainG, mainB));
            canvas.drawLine(sx, sy, targetX, targetY, strokePaint);

            // 3. Vòng tròn vùng nổ AoE di chuyển theo ngón tay kéo
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(75, mainR, mainG, mainB));
            canvas.drawCircle(targetX, targetY, aoeRadius, paint);

            strokePaint.setStrokeWidth(4.5f);
            strokePaint.setColor(Color.argb(240, mainR, mainG, mainB));
            canvas.drawCircle(targetX, targetY, aoeRadius, strokePaint);

            // 4. Hồng tâm ngắm (Bullseye Crosshair) bên trong vùng chọn
            strokePaint.setStrokeWidth(2f);
            strokePaint.setColor(Color.argb(200, 255, 245, 180));
            canvas.drawCircle(targetX, targetY, aoeRadius * 0.45f, strokePaint);
            canvas.drawLine(targetX - 18f, targetY, targetX + 18f, targetY, strokePaint);
            canvas.drawLine(targetX, targetY - 18f, targetX, targetY + 18f, strokePaint);
        }
    }

    private void renderSnapshotMonster(Canvas canvas, network.WorldSnapshotPayload.EntityState state, Vector2D pos, Camera2D camera) {
        float sx = (float) camera.worldToScreenX(pos.getX());
        float sy = (float) camera.worldToScreenY(pos.getY());

        // Bóng đổ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 0, 0, 0));
        rectF.set(sx - 18, sy + 16, sx + 18, sy + 24);
        canvas.drawOval(rectF, paint);

        String name = state.name.toLowerCase();
        if (name.contains("goblin")) {
            paint.setColor(Color.rgb(46, 139, 87));
            canvas.drawCircle(sx, sy, 16, paint);
            paint.setColor(Color.RED);
            canvas.drawCircle(sx - 5, sy - 4, 3, paint);
            canvas.drawCircle(sx + 5, sy - 4, 3, paint);
        } else if (name.contains("orc")) {
            paint.setColor(Color.rgb(105, 105, 105));
            rectF.set(sx - 22, sy - 20, sx + 22, sy + 20);
            canvas.drawRoundRect(rectF, 8, 8, paint);
            strokePaint.setColor(Color.rgb(230, 230, 200));
            strokePaint.setStrokeWidth(4f);
            canvas.drawLine(sx - 12, sy - 20, sx - 22, sy - 34, strokePaint);
            canvas.drawLine(sx + 12, sy - 20, sx + 22, sy - 34, strokePaint);
        } else {
            // Slime
            paint.setColor(Color.rgb(0, 230, 118));
            rectF.set(sx - 18, sy - 12, sx + 18, sy + 18);
            canvas.drawOval(rectF, paint);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(sx - 6, sy - 2, 2.5f, paint);
            canvas.drawCircle(sx + 6, sy - 2, 2.5f, paint);
        }

        // Tên quái & Thanh máu
        paint.setTextSize(18f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 160, 160));
        canvas.drawText(state.name, sx, sy - 32, paint);

        float hpW = 44f;
        float hpH = 5f;
        float hpRatio = state.maxHp > 0 ? Math.max(0f, (float) state.hp / state.maxHp) : 0f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 50, 0, 0));
        canvas.drawRect(sx - hpW / 2, sy - 28, sx + hpW / 2, sy - 28 + hpH, paint);
        paint.setColor(Color.rgb(255, 50, 50));
        canvas.drawRect(sx - hpW / 2, sy - 28, sx - hpW / 2 + hpW * hpRatio, sy - 28 + hpH, paint);
    }

    private void renderSnapshotPlayer(Canvas canvas, network.WorldSnapshotPayload.EntityState state, Vector2D pos, Camera2D camera, boolean isLocal) {
        float sx = (float) camera.worldToScreenX(pos.getX());
        float sy = (float) camera.worldToScreenY(pos.getY());

        // Bóng đổ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 0, 0, 0));
        rectF.set(sx - 20, sy + 18, sx + 20, sy + 28);
        canvas.drawOval(rectF, paint);

        // Thân áo
        paint.setColor(isLocal ? Color.rgb(20, 120, 220) : Color.rgb(220, 120, 30));
        rectF.set(sx - 14, sy - 8, sx + 14, sy + 14);
        canvas.drawRoundRect(rectF, 4, 4, paint);

        // Đầu & Tóc
        paint.setColor(Color.rgb(255, 219, 172));
        canvas.drawCircle(sx, sy - 18, 12, paint);
        paint.setColor(Color.rgb(50, 30, 15));
        rectF.set(sx - 13, sy - 31, sx + 13, sy - 15);
        canvas.drawArc(rectF, 180, 180, true, paint);

        // Hướng nhìn & Mắt
        Direction dir = Direction.DOWN;
        try {
            if (state.dirName != null) dir = Direction.valueOf(state.dirName);
        } catch (Exception ignored) {}

        float eyeOffX = (float) dir.getDirX() * 4;
        float eyeOffY = (float) dir.getDirY() * 3;
        paint.setColor(Color.BLACK);
        canvas.drawCircle(sx - 4 + eyeOffX, sy - 19 + eyeOffY, 2f, paint);
        canvas.drawCircle(sx + 4 + eyeOffX, sy - 19 + eyeOffY, 2f, paint);

        // Vũ khí
        float wpx = sx + (float) dir.getDirX() * 22;
        float wpy = sy + (float) dir.getDirY() * 18;
        strokePaint.setColor(isLocal ? Color.rgb(0, 230, 255) : Color.rgb(255, 215, 0));
        strokePaint.setStrokeWidth(4f);
        canvas.drawLine(sx, sy, wpx, wpy, strokePaint);

        // Tên nhân vật
        paint.setTextSize(20f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(isLocal ? Color.CYAN : Color.YELLOW);
        String label = isLocal ? ("★ [BẠN] " + state.name + " ★") : ("⚔ " + state.name);
        canvas.drawText(label, sx, sy - 40, paint);

        // Thanh máu
        float hpW = 50f;
        float hpH = 6f;
        float hpRatio = state.maxHp > 0 ? Math.max(0f, (float) state.hp / state.maxHp) : 0f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 40, 0, 0));
        canvas.drawRect(sx - hpW / 2, sy - 36, sx + hpW / 2, sy - 36 + hpH, paint);
        paint.setColor(isLocal ? Color.rgb(0, 230, 118) : Color.rgb(255, 180, 0));
        canvas.drawRect(sx - hpW / 2, sy - 36, sx - hpW / 2 + hpW * hpRatio, sy - 36 + hpH, paint);
    }

    private void renderMultiplayerHUD(Canvas canvas, network.WorldSnapshotPayload.EntityState myState, int wave, String roomCode, int playerCount, int monsterCount, int screenW, int screenH, java.util.Map<String, network.WorldSnapshotPayload.EntityState> states, long clientTime, network.NetworkGameClient networkClient) {
        // 1. Thẻ trạng thái người chơi (Top-Left)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(190, 15, 20, 30));
        rectF.set(24, 24, 380, 130);
        canvas.drawRoundRect(rectF, 12, 12, paint);

        strokePaint.setColor(Color.argb(140, 0, 200, 255));
        strokePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(rectF, 12, 12, strokePaint);

        String pName = myState != null ? myState.name : "Đang đồng bộ...";
        int curHp = myState != null ? myState.hp : 100;
        int maxHp = myState != null ? myState.maxHp : 100;

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(24f);
        paint.setColor(Color.WHITE);
        canvas.drawText("👤 " + pName, 40, 58, paint);

        paint.setTextSize(18f);
        paint.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("🌐 MULTIPLAYER  |  ĐỢT QUÁI: " + wave, 40, 84, paint);

        // Thanh máu
        float barX = 40;
        float barY = 96;
        float barW = 320;
        float barH = 16;
        float hpRatio = maxHp > 0 ? Math.max(0f, (float) curHp / maxHp) : 0f;

        paint.setColor(Color.argb(200, 60, 10, 10));
        canvas.drawRect(barX, barY, barX + barW, barY + barH, paint);
        paint.setColor(Color.rgb(0, 230, 118));
        canvas.drawRect(barX, barY, barX + barW * hpRatio, barY + barH, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(curHp + " / " + maxHp + " HP", barX + barW / 2, barY + 13, paint);

        // 2. Huy hiệu Mã Phòng (Top-Center)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 24, 32, 48));
        rectF.set(screenW / 2f - 220, 20, screenW / 2f + 220, 78);
        canvas.drawRoundRect(rectF, 10, 10, paint);

        strokePaint.setColor(Color.rgb(255, 215, 0));
        strokePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(rectF, 10, 10, strokePaint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(22f);
        paint.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("🔑 PHÒNG: " + roomCode + "  (" + playerCount + " Người chơi)", screenW / 2f, 48, paint);

        paint.setTextSize(14f);
        paint.setColor(Color.rgb(180, 220, 255));
        canvas.drawText("Chơi chung thời gian thực giữa PC & Android", screenW / 2f, 68, paint);

        // 3. Mini-map Radar (Top-Right)
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

        float scaleX = mapW / (float) GameWorld.WORLD_WIDTH;
        float scaleY = mapH / (float) GameWorld.WORLD_HEIGHT;

        for (network.WorldSnapshotPayload.EntityState st : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(st.id, clientTime);
            if (pos == null) pos = new Vector2D(st.posX, st.posY);

            float mx = mapX + (float) pos.getX() * scaleX;
            float my = mapY + (float) pos.getY() * scaleY;

            if (st.isMonster) {
                paint.setColor(Color.RED);
                canvas.drawCircle(mx, my, 3f, paint);
            } else {
                paint.setColor(myState != null && st.id.equals(myState.id) ? Color.CYAN : Color.YELLOW);
                canvas.drawCircle(mx, my, 4.5f, paint);
            }
        }
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
                // Tai nhọn Goblin (tái sử dụng slashPath để Zero GC Allocation)
                slashPath.reset();
                slashPath.moveTo(sx - 16, sy - 4);
                slashPath.lineTo(sx - 26, sy - 14);
                slashPath.lineTo(sx - 10, sy - 10);
                slashPath.moveTo(sx + 16, sy - 4);
                slashPath.lineTo(sx + 26, sy - 14);
                slashPath.lineTo(sx + 10, sy - 10);
                canvas.drawPath(slashPath, paint);
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

        // 1. Vẽ hiệu ứng Vòng Xoáy / Sóng Xung Kích của 3 Kỹ Năng AOV (Skill Rings)
        for (CombatEngine.SkillRingEffect ring : ce.getSkillRings()) {
            float sx = (float) camera.worldToScreenX(ring.x);
            float sy = (float) camera.worldToScreenY(ring.y);
            float progress = (float) Math.max(0.0, Math.min(1.0, 1.0 - (ring.lifeTime / ring.maxLifeTime)));
            float currentRadius = (float) (ring.maxRadius * (0.25f + 0.75f * progress));
            float alpha = (float) Math.max(0f, ring.lifeTime / ring.maxLifeTime);

            int c = ring.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            // Vùng sáng nền bán trong suốt
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int) (alpha * 65), r, g, b));
            canvas.drawCircle(sx, sy, currentRadius, paint);

            // Viền sóng xung kích phát sáng
            strokePaint.setStrokeWidth(ring.skillType == 3 ? 8f : 5f);
            strokePaint.setColor(Color.argb((int) (alpha * 240), r, g, b));
            canvas.drawCircle(sx, sy, currentRadius, strokePaint);
        }

        // 2. Vẽ vệt chém thường (Slash Arcs)
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
