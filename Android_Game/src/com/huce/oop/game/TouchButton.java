package com.huce.oop.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Nút bấm cảm ứng ảo (Virtual Touch Button) cho điện thoại Android.
 * Đóng gói trạng thái phản hồi xúc giác, màu sắc và sự kiện chạm đa điểm.
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
    private final Rect textBounds = new Rect();

    public TouchButton(float centerX, float centerY, float radius, String label, String subLabel, int baseColor, int pressedColor) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.label = label;
        this.subLabel = subLabel;
        this.baseColor = baseColor;
        this.pressedColor = pressedColor;
    }

    public void setCenter(float cx, float cy) {
        this.centerX = cx;
        this.centerY = cy;
    }

    public boolean onTouchDown(int pointerId, float touchX, float touchY) {
        float dx = touchX - centerX;
        float dy = touchY - centerY;
        if (dx * dx + dy * dy <= radius * radius && activePointerId == -1) {
            activePointerId = pointerId;
            isPressed = true;
            return true;
        }
        return false;
    }

    public boolean onTouchUp(int pointerId) {
        if (pointerId == activePointerId) {
            activePointerId = -1;
            boolean wasPressed = isPressed;
            isPressed = false;
            return wasPressed;
        }
        return false;
    }

    public void reset() {
        activePointerId = -1;
        isPressed = false;
    }

    public boolean isPressed() {
        return isPressed;
    }

    public void draw(Canvas canvas, Paint paint) {
        // Nền nút
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isPressed ? pressedColor : baseColor);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // Viền sáng
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(isPressed ? 5f : 3f);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // Chữ chính (Label)
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(radius * 0.45f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.getTextBounds(label, 0, label.length(), textBounds);
        
        float textY = (subLabel != null && !subLabel.isEmpty()) 
                ? centerY - 4 
                : centerY + (textBounds.height() / 2f);
        canvas.drawText(label, centerX, textY, paint);

        // Chữ phụ (SubLabel)
        if (subLabel != null && !subLabel.isEmpty()) {
            paint.setTextSize(radius * 0.25f);
            paint.setColor(Color.argb(220, 255, 230, 150));
            canvas.drawText(subLabel, centerX, centerY + radius * 0.45f, paint);
        }
    }
}
