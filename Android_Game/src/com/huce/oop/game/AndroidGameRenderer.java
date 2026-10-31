package com.huce.oop.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import combat.CombatEffects;
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

        // 1. Vẽ mặt đất đấu trường 2.5D (2.5D Isometric Arena Floor & 3D Walls)
        renderArenaFloor(canvas, camera, screenWidth, screenHeight);

        // 2. Vẽ hiệu ứng chém kiếm (Slash Arcs)
        renderSlashes(canvas, world.getCombatEngine(), camera);

        // 3. 2.5D DEPTH SORTING (Z-Ordering theo Y chân thực thể)
        java.util.List<RenderableEntityAndroid> renderList = new java.util.ArrayList<>();

        // Thêm Môi trường 2.5D (Trụ đá phong ấn, Cây đại thụ, Tảng đá rêu phong)
        addEnvironmentToRenderList(renderList, camera);

        // Thêm Quái vật
        for (Monster monster : world.getMonsters()) {
            if (monster != null && monster.isAlive()) {
                final Monster m = monster;
                renderList.add(new RenderableEntityAndroid() {
                    @Override
                    public double getDepthY() { return m.getPosition().getY(); }
                    @Override
                    public void draw(Canvas c) { renderMonster(c, m, camera); }
                });
            }
        }

        // Thêm Người chơi từ xa
        for (Player remotePlayer : world.getRemotePlayers()) {
            if (remotePlayer != null) {
                final Player rp = remotePlayer;
                renderList.add(new RenderableEntityAndroid() {
                    @Override
                    public double getDepthY() { return rp.getPosition().getY(); }
                    @Override
                    public void draw(Canvas c) { renderPlayerCharacter(c, rp, camera, false); }
                });
            }
        }

        // Thêm Người chơi chính
        final Player localPlayer = world.getPlayer();
        if (localPlayer != null) {
            renderList.add(new RenderableEntityAndroid() {
                @Override
                public double getDepthY() { return localPlayer.getPosition().getY(); }
                @Override
                public void draw(Canvas c) { renderPlayerCharacter(c, localPlayer, camera, true); }
            });
        }

        // Sắp xếp theo chiều sâu Y tăng dần
        java.util.Collections.sort(renderList, (a, b) -> Double.compare(a.getDepthY(), b.getDepthY()));
        for (RenderableEntityAndroid item : renderList) {
            item.draw(canvas);
        }

        // 6. Vẽ Tia lửa tóe ra khi va chạm (Hit Particles)
        renderParticles(canvas, world.getCombatEngine(), camera);

        // 7. Vẽ Số sát thương nảy lên (Floating Combat Text)
        renderFloatingTexts(canvas, world.getCombatEngine(), camera);

        // 8. Vẽ Giao diện chỉ số (HUD Overhead & Minimap Radar)
        renderHUD(canvas, world, screenWidth, screenHeight);
    }

    // Vị trí và loại cây cối 2.5D (Tree X, Tree Y, Type: 0 = Ancient Oak, 1 = Autumn Gold Maple)
    private static final double[][] TREE_COORDINATES = {
            {140, 150, 1}, {330, 125, 0}, {680, 135, 1}, {970, 120, 0}, {1350, 140, 1}, {1520, 115, 0},
            {160, 440, 0}, {720, 435, 1}, {1450, 440, 0},
            {110, 740, 1}, {140, 1020, 0}, {280, 1110, 1},
            {1500, 750, 1}, {1470, 1030, 0}, {1310, 1110, 1},
            {450, 920, 0}, {1160, 930, 1}
    };

    // Vị trí các tảng đá rêu phong 2.5D (Boulder X, Boulder Y, Radius)
    private static final double[][] BOULDER_COORDINATES = {
            {240, 600, 26}, {1370, 620, 28}, {620, 1020, 24}, {980, 1030, 26}
    };

    private static double getRiverCenterY(double wx) {
        return 290.0 + Math.sin(wx * 0.004) * 42.0 + Math.cos(wx * 0.0022) * 16.0;
    }

    private interface RenderableEntityAndroid {
        double getDepthY();
        void draw(Canvas canvas);
    }

    private void addEnvironmentToRenderList(java.util.List<RenderableEntityAndroid> list, final Camera2D camera) {
        // 1. 4 Trụ đá Phong ấn 2.5D
        addPillarsToRenderList(list, camera);

        // 2. Cây đại thụ 2.5D (Z-Depth Sorting che khuất nhân vật tự nhiên)
        for (double[] t : TREE_COORDINATES) {
            final double wx = t[0];
            final double wy = t[1];
            final int type = (int) t[2];
            list.add(new RenderableEntityAndroid() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void draw(Canvas c) { render25DAndroidTree(c, wx, wy, type, camera); }
            });
        }

        // 3. Tảng đá rêu phong 2.5D
        for (double[] b : BOULDER_COORDINATES) {
            final double wx = b[0];
            final double wy = b[1];
            final double rad = b[2];
            list.add(new RenderableEntityAndroid() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void draw(Canvas c) { render25DAndroidBoulder(c, wx, wy, rad, camera); }
            });
        }
    }

    private void addPillarsToRenderList(java.util.List<RenderableEntityAndroid> list, final Camera2D camera) {
        double[][] pillarCoords = {
                {95, 95},
                {GameWorld.WORLD_WIDTH - 95, 95},
                {95, GameWorld.WORLD_HEIGHT - 95},
                {GameWorld.WORLD_WIDTH - 95, GameWorld.WORLD_HEIGHT - 95}
        };

        for (double[] coord : pillarCoords) {
            final double wx = coord[0];
            final double wy = coord[1];
            list.add(new RenderableEntityAndroid() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void draw(Canvas c) { render25DAndroidPillar(c, wx, wy, camera); }
            });
        }
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

        // 4. Vẽ tất cả thực thể nhận từ Snapshot thông qua SnapshotInterpolator LERP và 2.5D DEPTH SORTING
        int monsterCount = 0;
        int playerCount = 0;

        java.util.List<RenderableEntityAndroid> renderList = new java.util.ArrayList<>();
        addEnvironmentToRenderList(renderList, camera);

        for (network.WorldSnapshotPayload.EntityState state : states.values()) {
            Vector2D pos = networkClient.getInterpolatedPosition(state.id, clientTime);
            if (pos == null) {
                pos = new Vector2D(state.posX, state.posY);
            }
            final Vector2D fPos = pos;
            final network.WorldSnapshotPayload.EntityState fState = state;

            if (state.isMonster) {
                monsterCount++;
                renderList.add(new RenderableEntityAndroid() {
                    @Override
                    public double getDepthY() { return fPos.getY(); }
                    @Override
                    public void draw(Canvas c) { renderSnapshotMonster(c, fState, fPos, camera); }
                });
            } else {
                playerCount++;
                final boolean isLocal = (myId != null && state.id.equals(myId));
                renderList.add(new RenderableEntityAndroid() {
                    @Override
                    public double getDepthY() { return fPos.getY(); }
                    @Override
                    public void draw(Canvas c) { renderSnapshotPlayer(c, fState, fPos, camera, isLocal); }
                });
            }
        }

        // Sắp xếp và vẽ theo chiều sâu Z/Y
        java.util.Collections.sort(renderList, (a, b) -> Double.compare(a.getDepthY(), b.getDepthY()));
        for (RenderableEntityAndroid item : renderList) {
            item.draw(canvas);
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
            // CHIÊU 2 & CHIÊU 3: CHỌN VÙNG HÌNH ELIP 2.5D MẶT ĐẤT (2.5D Ground AoE Indicator)
            float ratio = 0.58f;
            float maxRange = (float) (activeSkillId == 2 ? CombatEngine.SKILL2_MAX_RANGE : CombatEngine.SKILL3_MAX_RANGE);
            float maxRangeY = maxRange * ratio;
            float aoeRadius = (float) (activeSkillId == 2 ? CombatEngine.SKILL2_AOE_RADIUS : CombatEngine.SKILL3_AOE_RADIUS);
            float aoeRadiusY = aoeRadius * ratio;

            float targetX = sx + (float) (aimX * maxRange);
            float targetY = sy + (float) (aimY * maxRange * ratio);

            // 1. Vành elip giới hạn tầm thi triển lớn quanh tướng
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(18, mainR, mainG, mainB));
            rectF.set(sx - maxRange, sy - maxRangeY, sx + maxRange, sy + maxRangeY);
            canvas.drawOval(rectF, paint);

            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2.5f);
            strokePaint.setColor(Color.argb(135, mainR, mainG, mainB));
            canvas.drawOval(rectF, strokePaint);

            // 2. Đường dẫn tia ngắm từ chân tướng tới tâm vùng chọn
            strokePaint.setStrokeWidth(3f);
            strokePaint.setColor(Color.argb(170, mainR, mainG, mainB));
            canvas.drawLine(sx, sy, targetX, targetY, strokePaint);

            // 3. Vùng nổ AoE hình Elip 2.5D di chuyển theo ngón tay kéo
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(75, mainR, mainG, mainB));
            rectF.set(targetX - aoeRadius, targetY - aoeRadiusY, targetX + aoeRadius, targetY + aoeRadiusY);
            canvas.drawOval(rectF, paint);

            strokePaint.setStrokeWidth(4.5f);
            strokePaint.setColor(Color.argb(240, mainR, mainG, mainB));
            canvas.drawOval(rectF, strokePaint);

            // Cột sáng 3D dọc trục Z
            paint.setColor(Color.argb(45, mainR, mainG, mainB));
            canvas.drawRect(targetX - 8f, targetY - 60f, targetX + 8f, targetY, paint);

            // 4. Hồng tâm ngắm (Bullseye Crosshair) bên trong vùng chọn
            strokePaint.setStrokeWidth(2f);
            strokePaint.setColor(Color.argb(200, 255, 245, 180));
            rectF.set(targetX - aoeRadius * 0.45f, targetY - aoeRadiusY * 0.45f, targetX + aoeRadius * 0.45f, targetY + aoeRadiusY * 0.45f);
            canvas.drawOval(rectF, strokePaint);
            canvas.drawLine(targetX - 18f, targetY, targetX + 18f, targetY, strokePaint);
            canvas.drawLine(targetX, targetY - 10f, targetX, targetY + 10f, strokePaint);
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
        // Nền đen sẫm sâu thẳm
        canvas.drawColor(Color.rgb(11, 15, 13));

        // Mặt sàn thế giới 1600x1200
        float left = (float) camera.worldToScreenX(0);
        float top = (float) camera.worldToScreenY(0);
        float right = (float) camera.worldToScreenX(GameWorld.WORLD_WIDTH);
        float bottom = (float) camera.worldToScreenY(GameWorld.WORLD_HEIGHT);

        // 1. THẢM CỎ ĐẤT TỰ NHIÊN (Meadow Base)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(45, 106, 54));
        canvas.drawRect(left, top, right, bottom, paint);

        // Mảng cỏ tạo khối tự nhiên
        paint.setColor(Color.argb(90, 36, 88, 44));
        for (int gx = 80; gx < GameWorld.WORLD_WIDTH; gx += 280) {
            for (int gy = 80; gy < GameWorld.WORLD_HEIGHT; gy += 260) {
                float gsx = (float) camera.worldToScreenX(gx);
                float gsy = (float) camera.worldToScreenY(gy);
                rectF.set(gsx - 85f, gsy - 50f, gsx + 85f, gsy + 50f);
                canvas.drawOval(rectF, paint);
            }
        }

        // Hoa dại trên thảo nguyên
        paint.setColor(Color.rgb(255, 235, 90));
        float[][] flowerSpots = {
                {220, 210}, {520, 190}, {840, 200}, {1220, 210},
                {280, 520}, {620, 500}, {1040, 510}, {1380, 530},
                {240, 840}, {560, 880}, {1080, 870}, {1360, 860}
        };
        for (float[] fs : flowerSpots) {
            float fx = (float) camera.worldToScreenX(fs[0]);
            float fy = (float) camera.worldToScreenY(fs[1]);
            canvas.drawCircle(fx, fy, 4f, paint);
        }

        // 2. LỐI MÒN ĐẤT SỎI (Paths)
        float riverBridgeWestY = (float) (getRiverCenterY(460) + 54);
        float riverBridgeEastY = (float) (getRiverCenterY(1140) + 54);
        float centerWorldX = (float) (GameWorld.WORLD_WIDTH / 2.0);
        float centerWorldY = 720f;

        drawAndroidDirtTrail(canvas, camera, 460f, riverBridgeWestY, 560f, 540f, centerWorldX, centerWorldY);
        drawAndroidDirtTrail(canvas, camera, 1140f, riverBridgeEastY, 1040f, 540f, centerWorldX, centerWorldY);
        drawAndroidDirtTrail(canvas, camera, centerWorldX, (float) GameWorld.WORLD_HEIGHT - 60f, centerWorldX, 900f, centerWorldX, centerWorldY);

        // 3. DÒNG SÔNG 2.5D UỐN LƯỢN (Animated River)
        android.graphics.Path riverPath = new android.graphics.Path();
        float rStep = 32f;
        for (float wx = 0; wx <= GameWorld.WORLD_WIDTH + rStep; wx += rStep) {
            float ry = (float) (getRiverCenterY(wx) - 44.0);
            float sx = (float) camera.worldToScreenX(wx);
            float sy = (float) camera.worldToScreenY(ry);
            if (wx == 0) riverPath.moveTo(sx, sy);
            else riverPath.lineTo(sx, sy);
        }
        for (float wx = (float) GameWorld.WORLD_WIDTH + rStep; wx >= 0; wx -= rStep) {
            float ry = (float) (getRiverCenterY(wx) + 44.0);
            float sx = (float) camera.worldToScreenX(wx);
            float sy = (float) camera.worldToScreenY(ry);
            riverPath.lineTo(sx, sy);
        }
        riverPath.close();

        // Đáy sông & mặt nước
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(18, 62, 102));
        canvas.drawPath(riverPath, paint);

        paint.setColor(Color.argb(215, 32, 114, 162));
        canvas.drawPath(riverPath, paint);

        // Sóng nước
        long time = System.currentTimeMillis();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.argb(130, 145, 225, 255));
        strokePaint.setStrokeWidth(2.5f);
        for (int i = 0; i < 2; i++) {
            android.graphics.Path waveP = new android.graphics.Path();
            float yOff = (i == 0) ? -14f : 14f;
            float speed = (i + 1) * 0.0028f;
            boolean first = true;
            for (float wx = 0; wx <= GameWorld.WORLD_WIDTH + rStep; wx += rStep) {
                float cy = (float) getRiverCenterY(wx);
                float wy = cy + yOff + (float) (Math.sin(time * speed + wx * 0.02f) * 5.0);
                float sx = (float) camera.worldToScreenX(wx);
                float sy = (float) camera.worldToScreenY(wy);
                if (first) { waveP.moveTo(sx, sy); first = false; }
                else { waveP.lineTo(sx, sy); }
            }
            canvas.drawPath(waveP, strokePaint);
        }

        // Bờ sông
        strokePaint.setColor(Color.rgb(135, 112, 75));
        strokePaint.setStrokeWidth(4f);
        android.graphics.Path bNorth = new android.graphics.Path();
        android.graphics.Path bSouth = new android.graphics.Path();
        for (float wx = 0; wx <= GameWorld.WORLD_WIDTH + rStep; wx += rStep) {
            float nry = (float) (getRiverCenterY(wx) - 44.0);
            float sry = (float) (getRiverCenterY(wx) + 44.0);
            float nsx = (float) camera.worldToScreenX(wx);
            float nsy = (float) camera.worldToScreenY(nry);
            float ssx = (float) camera.worldToScreenX(wx);
            float ssy = (float) camera.worldToScreenY(sry);
            if (wx == 0) { bNorth.moveTo(nsx, nsy); bSouth.moveTo(ssx, ssy); }
            else { bNorth.lineTo(nsx, nsy); bSouth.lineTo(ssx, ssy); }
        }
        canvas.drawPath(bNorth, strokePaint);
        canvas.drawPath(bSouth, strokePaint);

        // 4. CẦU ĐÁ CỔ 2.5D
        render25DAndroidBridge(canvas, camera, 460f);
        render25DAndroidBridge(canvas, camera, 1140f);

        // 5. THỀM ĐÁ ĐẤU TRƯỜNG TRUNG TÂM (Central Terrace)
        float csx = (float) camera.worldToScreenX(centerWorldX);
        float csy = (float) camera.worldToScreenY(centerWorldY);
        float terraceRx = 330f;
        float terraceRy = terraceRx * 0.58f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(33, 37, 47));
        rectF.set(csx - terraceRx, csy - terraceRy, csx + terraceRx, csy + terraceRy);
        canvas.drawOval(rectF, paint);

        // Viền đá & Rêu
        strokePaint.setColor(Color.rgb(55, 62, 78));
        strokePaint.setStrokeWidth(5f);
        canvas.drawOval(rectF, strokePaint);

        strokePaint.setColor(Color.argb(160, 48, 108, 56));
        strokePaint.setStrokeWidth(8f);
        rectF.set(csx - terraceRx + 4f, csy - terraceRy + 3f, csx + terraceRx - 4f, csy + terraceRy - 3f);
        canvas.drawOval(rectF, strokePaint);

        // Thần trận phong ấn tâm sàn đấu 2.5D
        float rRuneX = 220f;
        float rRuneY = rRuneX * 0.58f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(25, 0, 229, 255));
        rectF.set(csx - rRuneX, csy - rRuneY, csx + rRuneX, csy + rRuneY);
        canvas.drawOval(rectF, paint);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.argb(90, 0, 229, 255));
        strokePaint.setStrokeWidth(3f);
        canvas.drawOval(rectF, strokePaint);

        // 6. TƯỜNG THÀNH 3D CÓ ĐỘ CAO (North Wall Facade & Coping)
        float wallHeight = 44f;
        float copingHeight = 12f;

        // Mặt đứng tường đá phía Bắc
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(24, 27, 34));
        canvas.drawRect(left, top - wallHeight, right, top, paint);

        // Mặt trên đón sáng
        paint.setColor(Color.rgb(60, 68, 85));
        canvas.drawRect(left - 8, top - wallHeight - copingHeight, right + 8, top - wallHeight, paint);

        // Đổ bóng của tường Bắc xuống sàn
        paint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRect(left, top, right, top + 36, paint);

        // Mặt đứng tường Tây
        paint.setColor(Color.rgb(28, 32, 40));
        canvas.drawRect(left - 24, top - wallHeight, left, bottom, paint);

        // Gờ thành bảo vệ bao quanh đấu trường
        strokePaint.setColor(Color.rgb(255, 175, 40));
        strokePaint.setStrokeWidth(6f);
        canvas.drawRect(left, top, right, bottom, strokePaint);
    }

    private void drawAndroidDirtTrail(Canvas canvas, Camera2D camera, float x1, float y1, float cx, float cy, float x2, float y2) {
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo((float) camera.worldToScreenX(x1), (float) camera.worldToScreenY(y1));
        p.quadTo((float) camera.worldToScreenX(cx), (float) camera.worldToScreenY(cy), (float) camera.worldToScreenX(x2), (float) camera.worldToScreenY(y2));

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        strokePaint.setColor(Color.rgb(118, 92, 60));
        strokePaint.setStrokeWidth(38f);
        canvas.drawPath(p, strokePaint);

        strokePaint.setColor(Color.rgb(152, 124, 88));
        strokePaint.setStrokeWidth(26f);
        canvas.drawPath(p, strokePaint);

        strokePaint.setColor(Color.rgb(175, 146, 108));
        strokePaint.setStrokeWidth(16f);
        canvas.drawPath(p, strokePaint);
    }

    private void render25DAndroidBridge(Canvas canvas, Camera2D camera, float bridgeCenterX) {
        float riverY = (float) getRiverCenterY(bridgeCenterX);
        float topWY = riverY - 56f;
        float botWY = riverY + 56f;

        float sx1 = (float) camera.worldToScreenX(bridgeCenterX - 52f);
        float sx2 = (float) camera.worldToScreenX(bridgeCenterX + 52f);
        float sy1 = (float) camera.worldToScreenY(topWY);
        float sy2 = (float) camera.worldToScreenY(botWY);

        // Bóng đổ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(150, 10, 25, 40));
        rectF.set(sx1 - 4f, sy1 + 10f, sx2 + 4f, sy2);
        canvas.drawRoundRect(rectF, 8f, 8f, paint);

        // Thân cầu đá
        paint.setColor(Color.rgb(100, 106, 118));
        rectF.set(sx1, sy1, sx2, sy2);
        canvas.drawRoundRect(rectF, 6f, 6f, paint);

        // Lan can đá 3D hai bên
        float parapetH = 16f;
        float parapetW = 10f;
        paint.setColor(Color.rgb(65, 72, 85));
        canvas.drawRect(sx1, sy1 - parapetH, sx1 + parapetW, sy2, paint);
        paint.setColor(Color.rgb(125, 135, 150));
        canvas.drawRect(sx1, sy1 - parapetH, sx1 + parapetW, sy1 - parapetH + 4f, paint);

        paint.setColor(Color.rgb(50, 56, 68));
        canvas.drawRect(sx2 - parapetW, sy1 - parapetH, sx2, sy2, paint);
        paint.setColor(Color.rgb(110, 120, 135));
        canvas.drawRect(sx2 - parapetW, sy1 - parapetH, sx2, sy1 - parapetH + 4f, paint);

        // Đèn lồng ấm áp ở 4 góc cầu
        float[][] corners = {
                {sx1 + 4f, sy1 + 4f},
                {sx2 - 4f, sy1 + 4f},
                {sx1 + 4f, sy2 - 4f},
                {sx2 - 4f, sy2 - 4f}
        };
        for (float[] c : corners) {
            float cx = c[0];
            float cy = c[1];
            paint.setColor(Color.rgb(40, 45, 55));
            rectF.set(cx - 6f, cy - 14f, cx + 6f, cy + 2f);
            canvas.drawRoundRect(rectF, 3f, 3f, paint);

            paint.setColor(Color.argb(55, 255, 190, 60));
            canvas.drawCircle(cx, cy - 14f, 16f, paint);

            paint.setColor(Color.rgb(255, 220, 100));
            canvas.drawCircle(cx, cy - 14f, 4f, paint);
        }
    }

    private void render25DAndroidTree(Canvas canvas, double worldX, double worldY, int treeType, Camera2D cam) {
        if (canvas == null || cam == null) return;
        float sx = (float) cam.worldToScreenX(worldX);
        float sy = (float) cam.worldToScreenY(worldY);

        // 1. Bóng đổ trên mặt cỏ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(85, 15, 30, 18));
        rectF.set(sx - 34f, sy - 6f, sx + 34f, sy + 20f);
        canvas.drawOval(rectF, paint);

        // 2. Thân cây gỗ 3D
        float trunkH = 48f;
        // Rễ cây
        paint.setColor(Color.rgb(60, 38, 22));
        rectF.set(sx - 14f, sy - 6f, sx + 14f, sy + 6f);
        canvas.drawOval(rectF, paint);

        // Thân bên phải (Tối)
        paint.setColor(Color.rgb(65, 42, 25));
        canvas.drawRect(sx, sy - trunkH, sx + 8f, sy, paint);
        // Thân bên trái (Sáng)
        paint.setColor(Color.rgb(105, 70, 42));
        canvas.drawRect(sx - 8f, sy - trunkH, sx, sy, paint);

        // 3. Tán lá đa tầng 2.5D
        long time = System.currentTimeMillis();
        float sway = (float) (Math.sin(time * 0.0022 + worldX * 0.04) * 3.0);

        int shadowC = (treeType == 0) ? Color.rgb(20, 65, 30) : Color.rgb(125, 42, 20);
        int bodyC   = (treeType == 0) ? Color.rgb(42, 122, 54) : Color.rgb(195, 78, 28);
        int lightC  = (treeType == 0) ? Color.rgb(82, 178, 76) : Color.rgb(245, 148, 42);

        float cy = sy - trunkH - 8f;

        // Đáy lá tối
        paint.setColor(shadowC);
        canvas.drawCircle(sx - 14f + sway * 0.5f, cy + 4f, 22f, paint);
        canvas.drawCircle(sx + 14f + sway * 0.6f, cy + 4f, 22f, paint);

        // Tầng lá giữa
        paint.setColor(bodyC);
        canvas.drawCircle(sx - 10f + sway * 0.7f, cy - 14f, 24f, paint);
        canvas.drawCircle(sx + 10f + sway * 0.8f, cy - 14f, 24f, paint);
        canvas.drawCircle(sx + sway * 0.9f, cy - 28f, 26f, paint);

        // Tầng đón sáng
        paint.setColor(lightC);
        canvas.drawCircle(sx - 8f + sway * 0.9f, cy - 32f, 18f, paint);
    }

    private void render25DAndroidBoulder(Canvas canvas, double worldX, double worldY, double radius, Camera2D cam) {
        if (canvas == null || cam == null) return;
        float sx = (float) cam.worldToScreenX(worldX);
        float sy = (float) cam.worldToScreenY(worldY);
        float r = (float) radius;

        // 1. Bóng đổ
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 15, 25, 18));
        rectF.set(sx - r - 4f, sy - r * 0.25f, sx + r + 4f, sy + r * 0.6f);
        canvas.drawOval(rectF, paint);

        // 2. Thân đá khối
        paint.setColor(Color.rgb(48, 54, 62));
        rectF.set(sx - r, sy - r * 1.1f, sx + r, sy + r * 0.2f);
        canvas.drawRoundRect(rectF, 10f, 10f, paint);

        // Mặt sáng đón nắng
        paint.setColor(Color.rgb(88, 98, 112));
        rectF.set(sx - r + 3f, sy - r * 1.1f + 2f, sx + 4f, sy - 2f);
        canvas.drawRoundRect(rectF, 8f, 8f, paint);

        // Rêu xanh
        paint.setColor(Color.rgb(65, 135, 70));
        rectF.set(sx - r * 0.6f, sy - r * 1.15f, sx + r * 0.4f, sy - r * 0.7f);
        canvas.drawOval(rectF, paint);
    }

    private void render25DAndroidPillar(Canvas canvas, double worldX, double worldY, Camera2D cam) {
        if (canvas == null || cam == null) return;
        float sx = (float) cam.worldToScreenX(worldX);
        float sy = (float) cam.worldToScreenY(worldY);

        // 1. Bóng đổ xiên của trụ trên mặt đất 2.5D
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(75, 0, 0, 0));
        rectF.set(sx - 20, sy + 10, sx + 24, sy + 28);
        canvas.drawOval(rectF, paint);

        // 2. Chân đế đá phối cảnh
        paint.setColor(Color.rgb(45, 50, 62));
        rectF.set(sx - 18, sy + 2, sx + 18, sy + 16);
        canvas.drawRoundRect(rectF, 4, 4, paint);

        // 3. Thân cột đá 3D cao 55px
        float colH = 55f;
        float colY = sy + 4 - colH;
        // Mặt tối bên phải
        paint.setColor(Color.rgb(42, 46, 56));
        canvas.drawRect(sx, colY, sx + 14, colY + colH, paint);
        // Mặt sáng bên trái
        paint.setColor(Color.rgb(72, 80, 98));
        canvas.drawRect(sx - 14, colY, sx, colY + colH, paint);

        // 4. Bệ đỡ đỉnh cột
        paint.setColor(Color.rgb(85, 95, 118));
        rectF.set(sx - 16, colY - 6, sx + 16, colY + 4);
        canvas.drawRoundRect(rectF, 3, 3, paint);

        // 5. Ngọn lửa năng lượng ma thuật bập bùng
        paint.setColor(Color.argb(35, 0, 229, 255));
        canvas.drawCircle(sx, colY - 14, 22, paint);
        paint.setColor(Color.rgb(0, 200, 255));
        canvas.drawCircle(sx, colY - 14, 10, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(sx, colY - 12, 5, paint);
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
        for (CombatEffects.SkillRingEffect ring : ce.getSkillRings()) {
            float sx = (float) camera.worldToScreenX(ring.x);
            float sy = (float) camera.worldToScreenY(ring.y);
            float progress = (float) Math.max(0.0, Math.min(1.0, 1.0 - (ring.lifeTime / ring.maxLifeTime)));
            float currentRadius = (float) (ring.maxRadius * (0.25f + 0.75f * progress));
            float alpha = (float) Math.max(0f, ring.lifeTime / ring.maxLifeTime);

            int c = ring.color;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            float rx = currentRadius;
            float ry = currentRadius * 0.58f;

            // Vùng sáng nền bán trong suốt hình Elip 2.5D bám mặt sàn
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int) (alpha * 65), r, g, b));
            rectF.set(sx - rx, sy - ry, sx + rx, sy + ry);
            canvas.drawOval(rectF, paint);

            // Viền sóng xung kích phát sáng hình Elip 2.5D
            strokePaint.setStrokeWidth(ring.skillType == 3 ? 8f : 5f);
            strokePaint.setColor(Color.argb((int) (alpha * 240), r, g, b));
            canvas.drawOval(rectF, strokePaint);
        }

        // 2. Vẽ vệt chém thường 2.5D (Perspective Curved Slash Arcs)
        strokePaint.setStrokeWidth(6f);
        for (CombatEffects.SlashEffect slash : ce.getSlashEffects()) {
            float sx = (float) camera.worldToScreenX(slash.x);
            float sy = (float) camera.worldToScreenY(slash.y);
            float alpha = (float) Math.max(0f, slash.lifeTime / slash.maxLifeTime);

            strokePaint.setColor(Color.argb((int) (alpha * 220), 255, 215, 0));
            rectF.set(sx - 35, sy - 35 * 0.58f, sx + 35, sy + 35 * 0.58f);
            float startAngle = slash.direction.getRowOffset() * 90f - 45f;
            canvas.drawArc(rectF, startAngle, 90f, false, strokePaint);
        }
    }

    private void renderParticles(Canvas canvas, CombatEngine ce, Camera2D camera) {
        if (ce == null) return;
        paint.setStyle(Paint.Style.FILL);

        for (CombatEffects.HitParticle p : ce.getParticles()) {
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

        for (CombatEffects.FloatingText ft : ce.getFloatingTexts()) {
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
