package render;

import core.Vector2D;

/**
 * Camera 2.5D Chuẩn MOBA (Liên Quân Mobile / Liên Minh Huyền Thoại).
 * Sử dụng phép chiếu Dimetric / Orthographic-Tilt 2.5D kết hợp nội suy góc nghiêng mượt mà (Smooth Pitch LERP):
 * - Không gây méo hình thang khi di chuyển (0% Perspective Swimming/Warping).
 * - Khi bấm [V] chuyển đổi giữa 2D và 2.5D, góc nghiêng mặt đất (pitchTilt) và trục chiều cao (zScale)
 *   chuyển động mượt mà 60 FPS (Smooth Camera Tilt Transition).
 */
public class Camera2D {
    public static final int MODE_25D_MOBA = 0;
    public static final int MODE_25D_LOW = 1;
    public static final int MODE_2D_CLASSIC = 2;

    private double x;
    private double y;
    private final int viewWidth;
    private final int viewHeight;
    private final double worldWidth;
    private final double worldHeight;

    private int cameraMode = MODE_25D_MOBA;

    // Các thông số góc nhìn 2.5D được nội suy mượt (Smooth LERP) mỗi khung hình
    private double currentPitchTilt = 0.68;
    private double targetPitchTilt = 0.68;

    private double currentZScale = 1.00;
    private double targetZScale = 1.00;

    private double currentZoom = 1.06;
    private double targetZoom = 1.06;

    public Camera2D(int viewWidth, int viewHeight, double worldWidth, double worldHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.x = worldWidth / 2.0;
        this.y = worldHeight / 2.0;
    }

    public void update(Vector2D targetPos, double smoothFactor) {
        if (targetPos != null) {
            this.x += (targetPos.getX() - this.x) * smoothFactor;
            this.y += (targetPos.getY() - this.y) * smoothFactor;

            this.x = Math.max(0.0, Math.min(worldWidth, this.x));
            this.y = Math.max(0.0, Math.min(worldHeight, this.y));
        }

        // Nội suy mượt mà (Smooth LERP) góc nghiêng 2D <-> 2.5D
        double camSmooth = 0.14;
        this.currentPitchTilt += (this.targetPitchTilt - this.currentPitchTilt) * camSmooth;
        this.currentZScale += (this.targetZScale - this.currentZScale) * camSmooth;
        this.currentZoom += (this.targetZoom - this.currentZoom) * camSmooth;
    }

    public void cycleCameraMode() {
        setCameraMode((this.cameraMode + 1) % 3);
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int mode) {
        this.cameraMode = Math.max(0, Math.min(2, mode));
        if (this.cameraMode == MODE_25D_MOBA) {
            // Góc nhìn chuẩn Liên Quân / LMHT (Nghiêng ~55 độ từ trên cao xuống)
            this.targetPitchTilt = 0.68;
            this.targetZScale = 1.00;
            this.targetZoom = 1.06;
        } else if (this.cameraMode == MODE_25D_LOW) {
            // Góc nhìn 2.5D Cận cảnh (Nghiêng sâu ~42 độ, khối 2.5D nổi cao)
            this.targetPitchTilt = 0.55;
            this.targetZScale = 1.28;
            this.targetZoom = 1.14;
        } else {
            // Góc nhìn 2D Top-Down 90 độ phẳng hoàn toàn
            this.targetPitchTilt = 1.00;
            this.targetZScale = 0.00;
            this.targetZoom = 1.00;
        }
    }

    public boolean is3DMode() {
        return currentZScale > 0.05;
    }

    public String getCameraModeLabel() {
        if (cameraMode == MODE_25D_MOBA) return "🎥 2.5D MOBA (55°)";
        if (cameraMode == MODE_25D_LOW) return "🎥 2.5D CẬN CẢNH (42°)";
        return "🎥 2D TOP-DOWN (90°)";
    }

    public double getPitchTilt() {
        return currentPitchTilt;
    }

    public double getZScale() {
        return currentZScale;
    }

    public double getZoom() {
        return currentZoom;
    }

    /**
     * Trong chuẩn 2.5D MOBA (Dimetric Projection), tỉ lệ ngang được giữ đồng nhất theo Zoom
     * để khi di chuyển 8 hướng mặt đất trôi êm ru 100%, không bị méo hình thang.
     */
    public double getPerspectiveScale(double worldY) {
        return currentZoom;
    }

    /**
     * Chiếu tọa độ thế giới (worldX, worldY) sang trục X màn hình.
     */
    public double worldToScreenX(double worldX, double worldY) {
        return (viewWidth / 2.0) + (worldX - this.x) * currentZoom;
    }

    /**
     * Chiếu tọa độ thế giới (worldY, worldZ) sang trục Y màn hình.
     * - worldY bị nén theo góc nghiêng mặt đất currentPitchTilt (0.68 ở góc 55°).
     * - Độ cao worldZ dựng thẳng đứng lên trên màn hình theo currentZScale.
     */
    public double worldToScreenY(double worldY, double worldZ) {
        return (viewHeight / 2.0)
                + (worldY - this.y) * currentPitchTilt * currentZoom
                - worldZ * currentZScale * currentZoom;
    }

    public double worldToScreenX(double worldX) {
        return worldToScreenX(worldX, this.y);
    }

    public double worldToScreenY(double worldY) {
        return worldToScreenY(worldY, 0.0);
    }

    /**
     * Phép chiếu ngược chính xác 100% từ vị trí con trỏ chuột (screenX, screenY) về mặt đất 2.5D (wz = 0).
     */
    public double screenToWorldX(double screenX) {
        return this.x + (screenX - viewWidth / 2.0) / currentZoom;
    }

    public double screenToWorldY(double screenY) {
        return this.y + (screenY - viewHeight / 2.0) / (currentPitchTilt * currentZoom);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getViewWidth() {
        return viewWidth;
    }

    public int getViewHeight() {
        return viewHeight;
    }
}
