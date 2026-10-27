package com.huce.oop.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.Locale;

/**
 * Nút bấm cảm ứng ảo (Virtual Touch Button) chuẩn phong cách Liên Quân Mobile (AOV).
 * Tích hợp:
 * 1. Hiệu ứng quạt đếm ngược hồi chiêu (Radial Cooldown Sweep) và đồng hồ giây thực tế.
 * 2. Cơ chế Kéo Thả Chọn Vùng / Định Hướng 360° (Skill Drag Joystick) ngay trên nút kỹ năng.
 */
public class TouchButton {
    private float centerX;
    private float centerY;
    private float radius;
    private final String label;
    private final String subLabel;
    private final int baseColor;
    private final int pressedColor;

    private int activePointerId = -1;
    private boolean isPressed = false;
    private float cooldownRemaining = 0f;
    private float maxCooldown = 1f;

    // Trạng thái kéo thả chọn vùng kỹ năng (AOV Skill Drag-Aiming)
    private boolean dragAimable = false;
    private float knobX;
    private float knobY;
    private double aimX = 0.0;
    private double aimY = 0.0;

    private final Rect textBounds = new Rect();
    private final RectF arcBounds = new RectF();

    public TouchButton(float centerX, float centerY, float radius, String label, String subLabel, int baseColor, int pressedColor) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.label = label;
        this.subLabel = subLabel;
        this.baseColor = baseColor;
        this.pressedColor = pressedColor;
        this.knobX = centerX;
        this.knobY = centerY;
    }

    public TouchButton setDragAimable(boolean dragAimable) {
        this.dragAimable = dragAimable;
        return this;
    }

    public boolean isDragAimable() {
        return dragAimable;
    }

    public void setCenter(float cx, float cy) {
        this.centerX = cx;
        this.centerY = cy;
        if (!isPressed) {
            this.knobX = cx;
            this.knobY = cy;
        }
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }

    public double getAimX() {
        return aimX;
    }

    public double getAimY() {
        return aimY;
    }

    public boolean containsPoint(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (dx * dx + dy * dy) <= (radius * 1.25f) * (radius * 1.25f);
    }

    public void setCooldown(double remaining, double max) {
        this.cooldownRemaining = (float) Math.max(0.0, remaining);
        this.maxCooldown = (float) Math.max(0.1, max);
    }

    public boolean isOnCooldown() {
        return cooldownRemaining > 0.05f;
    }

    public boolean onTouchDown(int pointerId, float touchX, float touchY) {
        float dx = touchX - centerX;
        float dy = touchY - centerY;
        if (dx * dx + dy * dy <= radius * radius && activePointerId == -1) {
            if (isOnCooldown()) {
                return false; // Đang hồi chiêu thì không kích hoạt
            }
            activePointerId = pointerId;
            isPressed = true;
            knobX = centerX;
            knobY = centerY;
            aimX = 0.0;
            aimY = 0.0;
            if (dragAimable) {
                updateDragKnob(touchX, touchY);
            }
            return true;
        }
        return false;
    }

    public boolean onTouchMove(int pointerId, float touchX, float touchY) {
        if (pointerId == activePointerId && isPressed && dragAimable) {
            updateDragKnob(touchX, touchY);
            return true;
        }
        return false;
    }

    private void updateDragKnob(float touchX, float touchY) {
        float dx = touchX - centerX;
        float dy = touchY - centerY;
        float dist = (float) Math.hypot(dx, dy);
        float maxDrag = radius * 1.75f;

        if (dist > maxDrag && dist > 0.001f) {
            knobX = centerX + (dx / dist) * maxDrag;
            knobY = centerY + (dy / dist) * maxDrag;
            aimX = dx / dist;
            aimY = dy / dist;
        } else {
            knobX = touchX;
            knobY = touchY;
            aimX = dx / maxDrag;
            aimY = dy / maxDrag;
        }
    }

    public boolean onTouchUp(int pointerId) {
        if (pointerId == activePointerId) {
            activePointerId = -1;
            boolean wasPressed = isPressed;
            isPressed = false;
            knobX = centerX;
            knobY = centerY;
            return wasPressed;
        }
        return false;
    }

    public void reset() {
        activePointerId = -1;
        isPressed = false;
        knobX = centerX;
        knobY = centerY;
        aimX = 0.0;
        aimY = 0.0;
    }

    public boolean isPressed() {
        return isPressed;
    }

    public void draw(Canvas canvas, Paint paint) {
        // 0. Nếu đang giữ kéo thả chọn vùng (AOV Skill Drag Ring): Vẽ vòng giới hạn kéo + núm điều hướng
        if (dragAimable && isPressed) {
            float maxDrag = radius * 1.75f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(55, 0, 229, 255));
            canvas.drawCircle(centerX, centerY, maxDrag, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.8f);
            paint.setColor(Color.argb(190, 0, 229, 255));
            canvas.drawCircle(centerX, centerY, maxDrag, paint);

            // Đường nối từ tâm nút tới núm kéo
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(200, 255, 255, 255));
            canvas.drawLine(centerX, centerY, knobX, knobY, paint);
        }

        // 1. Vòng hào quang nền ngoài chuẩn AOV
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(60, 0, 0, 0));
        canvas.drawCircle(centerX, centerY, radius + 5f, paint);

        // 2. Nền nút chính
        paint.setColor(isPressed ? pressedColor : baseColor);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // 3. Chữ/Biểu tượng chính (Label)
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(radius * 0.44f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.getTextBounds(label, 0, label.length(), textBounds);

        float textY = (subLabel != null && !subLabel.isEmpty())
                ? centerY - 3f
                : centerY + (textBounds.height() / 2f);
        canvas.drawText(label, centerX, textY, paint);

        // 4. Chữ phụ (SubLabel)
        if (subLabel != null && !subLabel.isEmpty()) {
            paint.setTextSize(radius * 0.24f);
            paint.setColor(Color.argb(240, 255, 240, 180));
            canvas.drawText(subLabel, centerX, centerY + radius * 0.46f, paint);
        }

        // 5. Hiệu ứng Quạt Đếm Ngược Hồi Chiêu (AOV Radial Cooldown Overlay)
        if (isOnCooldown()) {
            arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            float sweepAngle = Math.min(360f, (cooldownRemaining / maxCooldown) * 360f);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(185, 10, 14, 22));
            canvas.drawArc(arcBounds, -90f, sweepAngle, true, paint);

            // Số giây đếm ngược chính giữa nút (ví dụ: 3.5s)
            paint.setColor(Color.rgb(255, 230, 50));
            paint.setTextSize(radius * 0.48f);
            String cdText = String.format(Locale.US, "%.1fs", cooldownRemaining);
            canvas.drawText(cdText, centerX, centerY + radius * 0.16f, paint);
        }

        // 6. Viền kim loại sáng bên ngoài
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(isPressed ? 5f : 3.2f);
        paint.setColor(isOnCooldown() ? Color.argb(130, 150, 160, 180) : Color.rgb(255, 235, 160));
        canvas.drawCircle(centerX, centerY, radius, paint);

        // 7. Vẽ núm tròn điều hướng (Thumb Knob) khi người chơi đang kéo chọn vùng chiêu thức
        if (dragAimable && isPressed) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(235, 255, 255, 255));
            canvas.drawCircle(knobX, knobY, radius * 0.42f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(pressedColor);
            canvas.drawCircle(knobX, knobY, radius * 0.42f, paint);
        }
    }
}
