package render;

import core.GameWorld;

import java.awt.*;
import java.util.List;

/**
 * Bộ kết xuất các thực thể tĩnh / chướng ngại vật 2.5D trong môi trường (Props Renderer).
 * Áp dụng nguyên lý SRP (Single Responsibility Principle):
 * Đóng gói toàn bộ logic hình học, bóng đổ và Z-Ordering cho Cây cổ thụ (Trees),
 * Tảng đá rêu phong (Boulders) và Trụ phong ấn ma thuật (Arcane Pillars).
 */
public final class EnvironmentPropsRenderer {

    private EnvironmentPropsRenderer() {}

    // Vị trí và loại cây cối 2.5D (Tree X, Tree Y, Type: 0 = Ancient Oak, 1 = Autumn Gold Maple)
    public static final double[][] TREE_COORDINATES = {
            // Rặng cây bờ sông phía Bắc (North Riverbank Grove)
            {140, 150, 1}, {330, 125, 0}, {680, 135, 1}, {970, 120, 0}, {1350, 140, 1}, {1520, 115, 0},
            // Cây ven bờ sông phía Nam (South Riverbank)
            {160, 440, 0}, {720, 435, 1}, {1450, 440, 0},
            // Rừng cây râm mát mạn Tây (West Glade)
            {110, 740, 1}, {140, 1020, 0}, {280, 1110, 1},
            // Rừng cây mạn Đông (East Grove)
            {1500, 750, 1}, {1470, 1030, 0}, {1310, 1110, 1},
            // Cây trang trí thảm cỏ phía Nam (South Park)
            {450, 920, 0}, {1160, 930, 1}
    };

    // Vị trí các tảng đá rêu phong 2.5D (Boulder X, Boulder Y, Radius)
    public static final double[][] BOULDER_COORDINATES = {
            {240, 600, 26}, {1370, 620, 28}, {620, 1020, 24}, {980, 1030, 26}
    };

    public static void render25DTree(Graphics2D g2d, Camera2D camera, double worldX, double worldY, int treeType, int screenW, int screenH) {
        double sx = camera.worldToScreenX(worldX);
        double sy = camera.worldToScreenY(worldY);

        if (sx < -140 || sx > screenW + 140 || sy < -180 || sy > screenH + 140) return;

        int px = (int) sx;
        int py = (int) sy;

        // 1. Bóng đổ nghiêng 2.5D trên mặt cỏ
        g2d.setColor(new Color(15, 30, 18, 95));
        g2d.fillOval(px - 34, py - 6, 68, 26);

        // 2. Thân cây gỗ đại thụ 3D
        int trunkH = 50;
        int trunkBaseW = 20;
        int trunkTopW = 12;

        // Rễ cây
        g2d.setColor(new Color(60, 38, 22));
        g2d.fillOval(px - 14, py - 6, 28, 12);

        // Thân bên phải (Khuất sáng)
        g2d.setColor(new Color(65, 42, 25));
        Polygon rightTrunk = new Polygon(
                new int[]{px, px + trunkTopW / 2, px + trunkBaseW / 2, px},
                new int[]{py - trunkH, py - trunkH, py, py},
                4
        );
        g2d.fillPolygon(rightTrunk);

        // Thân bên trái (Đón ánh sáng)
        g2d.setColor(new Color(105, 70, 42));
        Polygon leftTrunk = new Polygon(
                new int[]{px - trunkTopW / 2, px, px, px - trunkBaseW / 2},
                new int[]{py - trunkH, py - trunkH, py, py},
                4
        );
        g2d.fillPolygon(leftTrunk);

        // Vết vân gỗ vỏ cây
        g2d.setColor(new Color(45, 28, 16));
        g2d.drawLine(px - 3, py - 6, px - 2, py - trunkH + 8);
        g2d.drawLine(px + 4, py - 6, px + 3, py - trunkH + 8);

        // 3. Tán lá đa tầng 2.5D với độ lay chuyển của gió
        long time = System.currentTimeMillis();
        double sway = Math.sin(time * 0.0022 + worldX * 0.04) * 3.0;

        Color shadowLeaf = (treeType == 0) ? new Color(20, 65, 30) : new Color(125, 42, 20);
        Color bodyLeaf   = (treeType == 0) ? new Color(42, 122, 54) : new Color(195, 78, 28);
        Color lightLeaf  = (treeType == 0) ? new Color(82, 178, 76) : new Color(245, 148, 42);
        Color tipLeaf    = (treeType == 0) ? new Color(140, 220, 118, 190) : new Color(255, 215, 85, 200);

        int cy = py - trunkH - 8;

        // Lớp lá đáy
        g2d.setColor(shadowLeaf);
        g2d.fillOval((int) (px - 36 + sway * 0.4), cy - 6, 44, 38);
        g2d.fillOval((int) (px - 6 + sway * 0.5), cy - 4, 46, 38);

        // Lớp lá giữa
        g2d.setColor(bodyLeaf);
        g2d.fillOval((int) (px - 30 + sway * 0.7), cy - 24, 46, 42);
        g2d.fillOval((int) (px - 14 + sway * 0.8), cy - 22, 48, 42);
        g2d.fillOval((int) (px - 24 + sway * 0.9), cy - 42, 50, 44);

        // Lớp lá đón sáng phía Tây-Bắc
        g2d.setColor(lightLeaf);
        g2d.fillOval((int) (px - 28 + sway * 0.9), cy - 44, 36, 34);
        g2d.fillOval((int) (px - 18 + sway * 0.8), cy - 28, 26, 24);

        // Đốm sáng đỉnh ngọn lá
        g2d.setColor(tipLeaf);
        g2d.fillOval((int) (px - 22 + sway), cy - 48, 22, 18);
        g2d.fillOval((int) (px - 32 + sway * 0.7), cy - 32, 14, 12);
    }

    public static void render25DBoulder(Graphics2D g2d, Camera2D camera, double worldX, double worldY, double radius, int screenW, int screenH) {
        double sx = camera.worldToScreenX(worldX);
        double sy = camera.worldToScreenY(worldY);

        if (sx < -80 || sx > screenW + 80 || sy < -80 || sy > screenH + 80) return;

        int px = (int) sx;
        int py = (int) sy;
        int r = (int) radius;

        // 1. Bóng đổ tảng đá
        g2d.setColor(new Color(15, 25, 18, 90));
        g2d.fillOval(px - r - 4, py - (int) (r * 0.25), (int) (r * 2.2), (int) (r * 0.8));

        // 2. Thân đá khối 3D (Shadow Side)
        g2d.setColor(new Color(48, 54, 62));
        g2d.fillRoundRect(px - r, py - (int) (r * 1.1), r * 2, (int) (r * 1.3), 12, 12);

        // Thân đá đón sáng (Northwest Highlight Facet)
        g2d.setColor(new Color(88, 98, 112));
        g2d.fillRoundRect(px - r + 3, py - (int) (r * 1.1) + 2, (int) (r * 1.3), (int) (r * 0.9), 10, 10);

        // Khe nứt đá (Cracks)
        g2d.setColor(new Color(32, 36, 42));
        g2d.drawLine(px - 2, py - (int) (r * 0.8), px + 4, py - (int) (r * 0.3));

        // 3. Rêu xanh phong trần phủ trên đỉnh đá (Moss Patch)
        g2d.setColor(new Color(55, 120, 60));
        g2d.fillOval(px - (int) (r * 0.7), py - (int) (r * 1.15), (int) (r * 1.2), (int) (r * 0.55));
        g2d.setColor(new Color(90, 175, 80));
        g2d.fillOval(px - (int) (r * 0.5), py - (int) (r * 1.15) + 1, (int) (r * 0.7), (int) (r * 0.35));
    }

    public static void render25DPillar(Graphics2D g2d, Camera2D camera, double worldX, double worldY) {
        double sx = camera.worldToScreenX(worldX);
        double sy = camera.worldToScreenY(worldY);

        int px = (int) sx;
        int py = (int) sy;

        // 1. Bóng đổ xiên của trụ trên mặt đất (2.5D Base Shadow)
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval(px - 14, py + 12, 44, 20);

        // 2. Chân đế đá vuông phối cảnh 2.5D (Octagonal Plinth)
        g2d.setColor(new Color(40, 45, 55));
        g2d.fillRoundRect(px - 20, py + 4, 40, 16, 6, 6);
        g2d.setColor(new Color(65, 72, 88));
        g2d.fillRoundRect(px - 18, py + 2, 36, 12, 4, 4);

        // 3. Thân cột đá 3D cao 64px vát sáng-tối theo góc Tây-Bắc
        int pillarHeight = 64;
        int colY = py + 2 - pillarHeight;

        // Nửa thân bên phải (Khuất bóng tối)
        g2d.setColor(new Color(45, 50, 62));
        g2d.fillRect(px, colY, 14, pillarHeight);
        // Nửa thân bên trái (Đón ánh sáng)
        g2d.setColor(new Color(75, 84, 104));
        g2d.fillRect(px - 14, colY, 14, pillarHeight);

        // Rãnh đá trang trí trên thân cột
        g2d.setColor(new Color(30, 34, 42));
        g2d.drawLine(px - 6, colY + 8, px - 6, colY + pillarHeight - 8);
        g2d.drawLine(px + 6, colY + 8, px + 6, colY + pillarHeight - 8);

        // 4. Đỉnh cột / Bệ đỡ ngọn đuốc (Capital)
        g2d.setColor(new Color(85, 95, 118));
        g2d.fillRoundRect(px - 18, colY - 6, 36, 10, 4, 4);

        // 5. Ngọn lửa năng lượng ma thuật bập bùng trên đỉnh trụ
        long time = System.currentTimeMillis();
        double pulse = Math.sin(time * 0.006 + worldX * 0.05) * 3.0;
        int flameY = (int) (colY - 18 + pulse);

        // Vầng sáng tỏa xung quanh ngọn lửa
        g2d.setColor(new Color(0, 229, 255, 30));
        g2d.fillOval(px - 28, flameY - 14, 56, 44);

        // Lớp lửa ngoài
        g2d.setColor(new Color(0, 200, 255, 200));
        g2d.fillOval(px - 10, flameY, 20, 20);
        // Lõi lửa trắng nóng
        g2d.setColor(new Color(240, 255, 255, 240));
        g2d.fillOval(px - 5, flameY + 4, 10, 12);
    }

    // Context tạm thời cho chu kỳ vẽ (Flyweight Pattern để tái sử dụng instance và chống cấp phát động)
    private static Camera2D activeCamera;
    private static int activeScreenW;
    private static int activeScreenH;

    // Cache sẵn 4 Trụ đá, 17 Cây cổ thụ và 4 Tảng đá (Flyweight / Object Cache - Rule 7)
    private static final Renderable25D[] CACHED_PILLARS;
    private static final Renderable25D[] CACHED_TREES;
    private static final Renderable25D[] CACHED_BOULDERS;

    static {
        double[][] pillarCoords = {
                {95, 95},
                {GameWorld.WORLD_WIDTH - 95, 95},
                {95, GameWorld.WORLD_HEIGHT - 95},
                {GameWorld.WORLD_WIDTH - 95, GameWorld.WORLD_HEIGHT - 95}
        };
        CACHED_PILLARS = new Renderable25D[pillarCoords.length];
        for (int i = 0; i < pillarCoords.length; i++) {
            final double wx = pillarCoords[i][0];
            final double wy = pillarCoords[i][1];
            CACHED_PILLARS[i] = new Renderable25D() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void render(Graphics2D g) {
                    render25DPillar(g, activeCamera, wx, wy);
                }
            };
        }

        CACHED_TREES = new Renderable25D[TREE_COORDINATES.length];
        for (int i = 0; i < TREE_COORDINATES.length; i++) {
            final double wx = TREE_COORDINATES[i][0];
            final double wy = TREE_COORDINATES[i][1];
            final int type = (int) TREE_COORDINATES[i][2];
            CACHED_TREES[i] = new Renderable25D() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void render(Graphics2D g) {
                    render25DTree(g, activeCamera, wx, wy, type, activeScreenW, activeScreenH);
                }
            };
        }

        CACHED_BOULDERS = new Renderable25D[BOULDER_COORDINATES.length];
        for (int i = 0; i < BOULDER_COORDINATES.length; i++) {
            final double wx = BOULDER_COORDINATES[i][0];
            final double wy = BOULDER_COORDINATES[i][1];
            final double rad = BOULDER_COORDINATES[i][2];
            CACHED_BOULDERS[i] = new Renderable25D() {
                @Override
                public double getDepthY() { return wy; }
                @Override
                public void render(Graphics2D g) {
                    render25DBoulder(g, activeCamera, wx, wy, rad, activeScreenW, activeScreenH);
                }
            };
        }
    }

    public static void addPillarsToRenderQueue(List<Renderable25D> queue, Camera2D camera) {
        activeCamera = camera;
        for (int i = 0; i < CACHED_PILLARS.length; i++) {
            queue.add(CACHED_PILLARS[i]);
        }
    }

    /**
     * Nạp chướng ngại vật vào hàng đợi chiều sâu Z tích hợp Frustum Culling (Rule 7: Viewport Culling).
     * Loại bỏ các vật thể ngoài màn hình trước khi sắp xếp, giảm tải đáng kể cho Painter's Algorithm.
     */
    public static void addEnvironmentToRenderQueue(List<Renderable25D> queue, Camera2D camera, int screenW, int screenH) {
        activeCamera = camera;
        activeScreenW = screenW;
        activeScreenH = screenH;

        // 1. 4 Trụ đá Phong ấn 2.5D (Frustum Culling)
        for (int i = 0; i < CACHED_PILLARS.length; i++) {
            double wy = CACHED_PILLARS[i].getDepthY();
            double sy = camera.worldToScreenY(wy);
            if (sy >= -100 && sy <= screenH + 80) {
                queue.add(CACHED_PILLARS[i]);
            }
        }

        // 2. Cây đại thụ 2.5D (Frustum Culling)
        for (int i = 0; i < TREE_COORDINATES.length; i++) {
            double wx = TREE_COORDINATES[i][0];
            double wy = TREE_COORDINATES[i][1];
            double sx = camera.worldToScreenX(wx);
            double sy = camera.worldToScreenY(wy);
            if (sx >= -140 && sx <= screenW + 140 && sy >= -180 && sy <= screenH + 140) {
                queue.add(CACHED_TREES[i]);
            }
        }

        // 3. Tảng đá rêu phong 2.5D (Frustum Culling)
        for (int i = 0; i < BOULDER_COORDINATES.length; i++) {
            double wx = BOULDER_COORDINATES[i][0];
            double wy = BOULDER_COORDINATES[i][1];
            double sx = camera.worldToScreenX(wx);
            double sy = camera.worldToScreenY(wy);
            if (sx >= -80 && sx <= screenW + 80 && sy >= -80 && sy <= screenH + 80) {
                queue.add(CACHED_BOULDERS[i]);
            }
        }
    }
}
