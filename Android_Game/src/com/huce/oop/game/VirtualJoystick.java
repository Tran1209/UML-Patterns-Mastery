package com.huce.oop.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import render.Direction;

/**
 * Cần điều khiển ảo (Virtual Analog Joystick) cho màn hình cảm ứng Android.
 * Hỗ trợ cảm ứng đa điểm (Multi-touch) mượt mà với bán kính co dãn linh hoạt.
 */
public class VirtualJoystick {
    private float baseCenterX;
    private float baseCenterY;
    private float baseRadius;

    private float knobX;
    private float knobY;
    private float knobRadius;

    private int activePointerId = -1;
    private boolean isPressed = false;

    private double normalizedX = 0.0;
    private double normalizedY = 0.0;

    public VirtualJoystick(float centerX, float centerY, float baseRadius, float knobRadius) {
        this.baseCenterX = centerX;
        this.baseCenterY = centerY;
        this.baseRadius = baseRadius;
        this.knobRadius = knobRadius;
        this.knobX = centerX;
        this.knobY = centerY;
    }

    public void setCenter(float x, float y) {
        this.baseCenterX = x;
        this.baseCenterY = y;
        if (!isPressed) {
            this.knobX = x;
            this.knobY = y;
        }
    }

    public boolean onTouchDown(int pointerId, float touchX, float touchY) {
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distSq = dx * dx + dy * dy;

        // Vùng kích hoạt mở rộng gấp 1.8 lần bán kính đế để người chơi dễ chạm
        float activeRadius = baseRadius * 1.8f;
        if (distSq <= activeRadius * activeRadius && activePointerId == -1) {
            activePointerId = pointerId;
            isPressed = true;
            updateKnobPosition(touchX, touchY);
            return true;
        }
        return false;
    }

    public boolean onTouchMove(int pointerId, float touchX, float touchY) {
        if (pointerId == activePointerId) {
            updateKnobPosition(touchX, touchY);
            return true;
        }
        return false;
    }

    public boolean onTouchUp(int pointerId) {
        if (pointerId == activePointerId) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        activePointerId = -1;
        isPressed = false;
        knobX = baseCenterX;
        knobY = baseCenterY;
        normalizedX = 0.0;
        normalizedY = 0.0;
    }

    private void updateKnobPosition(float touchX, float touchY) {
        float dx = touchX - baseCenterX;
        float dy = touchY - baseCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > baseRadius) {
            // Giới hạn núm điều khiển trong vòng tròn đế
            knobX = baseCenterX + (dx / distance) * baseRadius;
            knobY = baseCenterY + (dy / distance) * baseRadius;
            normalizedX = dx / distance;
            normalizedY = dy / distance;
        } else {
            knobX = touchX;
            knobY = touchY;
            if (baseRadius > 0) {
                normalizedX = dx / baseRadius;
                normalizedY = dy / baseRadius;
            } else {
                normalizedX = 0;
                normalizedY = 0;
            }
        }
    }

    public double getNormalizedX() {
        return normalizedX;
    }

    public double getNormalizedY() {
        return normalizedY;
    }

    public boolean isPressed() {
        return isPressed;
    }

    public Direction getDirection() {
        if (!isPressed || (Math.abs(normalizedX) < 0.2 && Math.abs(normalizedY) < 0.2)) {
            return null;
        }
        return Direction.fromVelocity(normalizedX, normalizedY, Direction.DOWN);
    }

    public void draw(Canvas canvas, Paint paint) {
        // 1. Vòng tròn nền ngoài
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 20, 30, 50));
        canvas.drawCircle(baseCenterX, baseCenterY, baseRadius, paint);

        // 2. Viền ngoài phát sáng
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(isPressed ? Color.argb(200, 0, 220, 255) : Color.argb(120, 100, 150, 200));
        canvas.drawCircle(baseCenterX, baseCenterY, baseRadius, paint);

        // 3. Đường tâm định hướng chữ thập
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(60, 255, 255, 255));
        canvas.drawLine(baseCenterX - baseRadius * 0.7f, baseCenterY, baseCenterX + baseRadius * 0.7f, baseCenterY, paint);
        canvas.drawLine(baseCenterX, baseCenterY - baseRadius * 0.7f, baseCenterX, baseCenterY + baseRadius * 0.7f, paint);

        // 4. Núm điều khiển (Knob)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isPressed ? Color.argb(230, 0, 200, 255) : Color.argb(160, 50, 120, 180));
        canvas.drawCircle(knobX, knobY, knobRadius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(255, 255, 255, 255));
        canvas.drawCircle(knobX, knobY, knobRadius, paint);
    }
}
