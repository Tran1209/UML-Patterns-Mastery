package render;

import core.Vector2D;

/**
 * Camera 3D Phối Cảnh (Perspective 3D MOBA Camera) kiêm hỗ trợ chuyển đổi 2D/3D thời gian thực.
 * - Chế độ 0 (Mặc định): 3D MOBA Perspective (Góc nghiêng 52° chuẩn Liên Quân / LMHT, có điểm tụ xa-gần và trục độ cao Z).
 * - Chế độ 1: 3D Cinematic Low-Angle (Góc nghiêng 38° cận cảnh chiều sâu 3D mạnh).
 * - Chế độ 2: 2D Top-Down Classic (Góc nhìn 90° phẳng truyền thống dùng để đối chiếu kiến trúc).
 */
public class Camera2D {
    public static final int MODE_3D_MOBA = 0;
    public static final int MODE_3D_CINEMATIC = 1;
    public static final int MODE_2D_CLASSIC = 2;

    private double x;
    private double y;
    private final int viewWidth;
    private final int viewHeight;
    private final double worldWidth;
    private final double worldHeight;
    private int cameraMode = MODE_3D_MOBA;

    public Camera2D(int viewWidth, int viewHeight, double worldWidth, double worldHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.x = worldWidth / 2.0;
        this.y = worldHeight / 2.0;
    }

    public void update(Vector2D targetPos, double smoothFactor) {
        if (targetPos == null) return;

        this.x += (targetPos.getX() - this.x) * smoothFactor;
        this.y += (targetPos.getY() - this.y) * smoothFactor;

        this.x = Math.max(0.0, Math.min(worldWidth, this.x));
        this.y = Math.max(0.0, Math.min(worldHeight, this.y));
    }

    public void cycleCameraMode() {
        this.cameraMode = (this.cameraMode + 1) % 3;
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int mode) {
        this.cameraMode = Math.max(0, Math.min(2, mode));
    }

    public boolean is3DMode() {
        return cameraMode != MODE_2D_CLASSIC;
    }

    public String getCameraModeLabel() {
        if (cameraMode == MODE_3D_MOBA) return "🎥 3D MOBA (52°)";
        if (cameraMode == MODE_3D_CINEMATIC) return "🎥 3D CẬN CẢNH (38°)";
        return "🎥 2D CỔ ĐIỂN (90°)";
    }

    /**
     * Hệ số nén trục Y theo góc nghiêng Camera (Pitch Tilt).
     */
    public double getPitchTilt() {
        if (cameraMode == MODE_3D_MOBA) return 0.72;
        if (cameraMode == MODE_3D_CINEMATIC) return 0.56;
        return 1.0;
    }

    /**
     * Hệ số phóng đại phối cảnh xa-gần (Perspective Foreshortening Scale) tại tọa độ worldY.
     * Vật thể ở xa (worldY < y) nhỏ lại hội tụ về đường chân trời; vật thể ở gần (worldY > y) to hơn.
     */
    public double getPerspectiveScale(double worldY) {
        if (cameraMode == MODE_2D_CLASSIC) return 1.0;
        double focalDist = (cameraMode == MODE_3D_MOBA) ? 1350.0 : 1050.0;
        double fovDepth = (cameraMode == MODE_3D_MOBA) ? 0.36 : 0.46;
        double dy = worldY - this.y;
        double denom = Math.max(420.0, focalDist - dy * fovDepth);
        double scale = focalDist / denom;
        return Math.max(0.45, Math.min(1.75, scale));
    }

    /**
     * Chiếu tọa độ 3D (worldX, worldY) sang trục X màn hình có tính hội tụ phối cảnh 3D.
     */
    public double worldToScreenX(double worldX, double worldY) {
        if (cameraMode == MODE_2D_CLASSIC) {
            return worldX - (x - viewWidth / 2.0);
        }
        double scale = getPerspectiveScale(worldY);
        return (viewWidth / 2.0) + (worldX - this.x) * scale;
    }

    /**
     * Chiếu tọa độ 3D (worldY, worldZ) sang trục Y màn hình có tính góc nghiêng mặt đất và độ cao trục Z.
     */
    public double worldToScreenY(double worldY, double worldZ) {
        if (cameraMode == MODE_2D_CLASSIC) {
            return worldY - (y - viewHeight / 2.0) - worldZ;
        }
        double scale = getPerspectiveScale(worldY);
        double pitch = getPitchTilt();
        return (viewHeight / 2.0) + (worldY - this.y) * pitch * scale - worldZ * 0.92 * scale;
    }

    public double worldToScreenX(double worldX) {
        return worldToScreenX(worldX, this.y);
    }

    public double worldToScreenY(double worldY) {
        return worldToScreenY(worldY, 0.0);
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
