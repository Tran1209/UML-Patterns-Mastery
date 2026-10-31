package render;

import core.LivingEntity;
import core.Monster;
import core.Player;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ dựng hình đồ họa Canvas 2.5D (Isometric / Dimetric Depth Projection)
 * tích hợp chuẩn Universal LPC và mô hình thể tích giả lập 3D (2.5D Volumetric Shading).
 * 
 * Các nâng cấp 2.5D cốt lõi:
 * 1. Hệ thống bóng đổ xiên 2.5D đa lớp (Multi-layer Directional Drop Shadow) theo góc chiếu sáng Tây-Bắc.
 * 2. Đổ bóng thể tích 3D (Volumetric Shading & Rim Lighting): Giáp ngực vát cạnh 3D (Beveled Plates),
 *    vũ khí lưỡi vát kim loại phản quang, vệt chém quét 3D theo góc phối cảnh.
 * 3. Quái vật 2.5D: Slime thạch cầu 3D với điểm sáng gương (Specular Highlight), Orc giáp vai gai 3D,
 *    Goblin dao găm vát thép nổi.
 * 4. Thanh máu & Danh hiệu dạng 3D Floating Billboard có bóng đổ tách lớp.
 */
public class LpcCanvasRenderer {

    private static final Map<String, BufferedImage> TEXTURE_CACHE = new HashMap<>();
    public static final double PROJECTION_RATIO_25D = 0.58; // Tỷ lệ nén trục Y trong phối cảnh 2.5D

    public static BufferedImage getTextureImage(LpcSpriteSheet sheet) {
        if (sheet == null || sheet.getAssetPath() == null || sheet.getAssetPath().isEmpty()) return null;
        String path = sheet.getAssetPath();
        if (TEXTURE_CACHE.containsKey(path)) return TEXTURE_CACHE.get(path);
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            try {
                BufferedImage img = ImageIO.read(file);
                TEXTURE_CACHE.put(path, img);
                return img;
            } catch (Exception e) {
                // Fallback to procedural
            }
        }
        return null;
    }

    public static boolean hasLoadedImage(LpcSpriteSheet sheet) {
        return getTextureImage(sheet) != null;
    }

    /**
     * Dựng hình nhân vật với hiệu ứng chiều sâu 2.5D.
     */
    public static void renderCharacter(Graphics2D g2d, CharacterRenderer renderer, double screenX, double screenY,
                                       String nameTag, int currentHp, int maxHp, boolean isPlayer) {
        CompositeCharacterVisual visual = renderer.getVisual();
        Direction dir = renderer.getCurrentDirection();
        LpcAnimationType anim = renderer.getCurrentAnimation();
        int frameIdx = renderer.getCurrentFrameIndex();

        // 1. Kiểm tra ảnh Spritesheet thật
        boolean hasTexture = false;
        List<SpriteLayer> layers = visual.getVisibleLayersSorted();
        for (SpriteLayer layer : layers) {
            if (hasLoadedImage(layer.getSpriteSheet())) {
                hasTexture = true;
                break;
            }
        }

        // --- 2.5D MULTI-LAYER DIRECTIONAL SHADOW (Bóng đổ xiên theo nguồn sáng Tây-Bắc) ---
        render25DDirectionalShadow(g2d, screenX, screenY, 34, 16);

        if (hasTexture) {
            // Vẽ bằng SpriteSheet thật (Painter's Algorithm: Lớp Z thấp vẽ trước, Z cao vẽ sau)
            Map<LayerType, FrameBounds> frameMap = visual.resolveRenderFrames(anim, dir, frameIdx);
            for (SpriteLayer layer : layers) {
                BufferedImage img = getTextureImage(layer.getSpriteSheet());
                FrameBounds clip = frameMap.get(layer.getLayerType());
                if (img != null && clip != null) {
                    int dx1 = (int) screenX - 32;
                    int dy1 = (int) screenY - 32;
                    int dx2 = dx1 + 64;
                    int dy2 = dy1 + 64;
                    int sx1 = clip.x;
                    int sy1 = clip.y;
                    int sx2 = sx1 + clip.width;
                    int sy2 = sy1 + clip.height;
                    g2d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                }
            }
        } else {
            // Vẽ mô hình thể tích 2.5D giả lập đa lớp chuẩn LPC
            render25DProceduralLpcAvatar(g2d, renderer, screenX, screenY, isPlayer);
        }

        // 2. Vẽ thanh máu nhỏ (Health Bar) và Tên nhân vật dạng 3D Billboard trên đầu
        renderOverheadStatus(g2d, screenX, screenY, nameTag, currentHp, maxHp, isPlayer);
    }

    /**
     * Vẽ bóng đổ xiên 2.5D đa lớp (Soft Directional Drop Shadow).
     * Nguồn sáng từ góc Tây-Bắc (Top-Left) làm bóng đổ hơi chếch sang Đông-Nam (Bottom-Right).
     */
    public static void render25DDirectionalShadow(Graphics2D g2d, double x, double y, int width, int height) {
        int shadowX = (int) x - width / 2 + 3;
        int shadowY = (int) y + 16;

        // Lớp bóng mờ ngoài (Penumbra)
        g2d.setColor(new Color(0, 0, 0, 45));
        g2d.fillOval(shadowX - 3, shadowY - 1, width + 6, height + 3);

        // Lớp bóng đậm giữa (Umbra)
        g2d.setColor(new Color(0, 0, 0, 85));
        g2d.fillOval(shadowX, shadowY, width, height);

        // Lớp bóng tiếp xúc mặt sàn (Contact Shadow)
        g2d.setColor(new Color(0, 0, 0, 130));
        g2d.fillOval((int) x - width / 4 + 2, shadowY + height / 4, width / 2, height / 2);
    }

    /**
     * Dựng hình nhân vật theo mô hình thể tích 2.5D (Volumetric Pseudo-3D LPC).
     */
    private static void render25DProceduralLpcAvatar(Graphics2D g2d, CharacterRenderer renderer, double x, double y, boolean isPlayer) {
        Direction dir = renderer.getCurrentDirection();
        LpcAnimationType anim = renderer.getCurrentAnimation();
        int frame = renderer.getCurrentFrameIndex();

        // Độ nhún khi di chuyển
        double bobY = (anim == LpcAnimationType.WALK) ? Math.sin(frame * 0.8) * 2.5 : 0;
        double swingAngle = 0.0;
        if (anim == LpcAnimationType.SLASH) {
            swingAngle = (frame - 2.5) * 0.65;
        }

        int px = (int) x;
        int py = (int) (y + bobY);

        Color skinColor = isPlayer ? new Color(245, 202, 165) : new Color(135, 190, 125);
        Color skinShadow = isPlayer ? new Color(205, 155, 120) : new Color(95, 140, 90);
        Color shirtColor = isPlayer ? new Color(50, 125, 210) : new Color(175, 85, 55);
        Color shirtShadow = isPlayer ? new Color(30, 80, 145) : new Color(120, 50, 30);
        Color armorBase = isPlayer ? new Color(195, 205, 220) : new Color(100, 110, 120);
        Color armorHighlight = isPlayer ? new Color(240, 245, 255) : new Color(155, 165, 175);
        Color armorShadow = isPlayer ? new Color(130, 140, 155) : new Color(60, 65, 75);
        Color hairColor = isPlayer ? new Color(95, 55, 32) : new Color(45, 40, 35);
        Color pantsColor = new Color(55, 60, 72);
        Color pantsShadow = new Color(35, 40, 48);

        // --- LAYER 1: Chân & Quần (2.5D Depth Stride) ---
        int legOffset = (anim == LpcAnimationType.WALK) ? (int) (Math.sin(frame * 1.2) * 5) : 0;
        // Chân sau (tối hơn, tạo chiều sâu xa-gần)
        g2d.setColor(pantsShadow);
        g2d.fillRoundRect(px - 9, py + 10, 6, 12 - legOffset, 3, 3);
        // Chân trước (sáng hơn)
        g2d.setColor(pantsColor);
        g2d.fillRoundRect(px + 3, py + 10, 6, 12 + legOffset, 3, 3);

        // --- LAYER 2: Thân & Áo (2.5D Beveled Shading) ---
        g2d.setColor(shirtShadow);
        g2d.fillRoundRect(px - 10, py - 4, 20, 16, 6, 6);
        g2d.setColor(shirtColor);
        g2d.fillRoundRect(px - 10, py - 4, 16, 14, 6, 6); // Highlight bên trái (hướng ánh sáng)

        // --- LAYER 3: Giáp Ngực 3D (Beveled Cuirass Plate) ---
        // Phần thân giáp bóng tối
        g2d.setColor(armorShadow);
        g2d.fillRoundRect(px - 8, py - 2, 16, 12, 4, 4);
        // Phần thân giáp chính
        g2d.setColor(armorBase);
        g2d.fillRoundRect(px - 8, py - 2, 13, 10, 4, 4);
        // Gờ phản quang kim loại (Specular Rim Highlight)
        g2d.setColor(armorHighlight);
        g2d.drawLine(px - 7, py - 2, px + 2, py - 2);
        g2d.drawLine(px - 7, py - 2, px - 7, py + 7);

        // --- LAYER 4: Đầu & Mặt 3D (Cầu phối cảnh) ---
        g2d.setColor(skinShadow);
        g2d.fillOval(px - 9, py - 22, 18, 18);
        g2d.setColor(skinColor);
        g2d.fillOval(px - 9, py - 22, 15, 15); // Highlight hướng sáng phía trên bên trái

        // Mắt theo hướng nhìn
        g2d.setColor(new Color(25, 25, 30));
        if (dir == Direction.DOWN) {
            g2d.fillRect(px - 5, py - 14, 3, 3);
            g2d.fillRect(px + 2, py - 14, 3, 3);
            // Ánh mắt (Glint)
            g2d.setColor(Color.WHITE);
            g2d.fillRect(px - 5, py - 14, 1, 1);
            g2d.fillRect(px + 2, py - 14, 1, 1);
        } else if (dir == Direction.LEFT) {
            g2d.fillRect(px - 7, py - 14, 3, 3);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(px - 7, py - 14, 1, 1);
        } else if (dir == Direction.RIGHT) {
            g2d.fillRect(px + 4, py - 14, 3, 3);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(px + 4, py - 14, 1, 1);
        }

        // --- LAYER 5: Tóc / Mũ Giáp (Hair / Helmet 3D Volume) ---
        g2d.setColor(hairColor);
        g2d.fillArc(px - 10, py - 24, 20, 16, 0, 180);
        g2d.setColor(hairColor.brighter());
        g2d.drawArc(px - 9, py - 23, 17, 13, 40, 110); // Ánh sáng đỉnh đầu
        if (dir == Direction.UP) {
            g2d.setColor(hairColor);
            g2d.fillOval(px - 9, py - 22, 18, 14);
        }

        // --- LAYER 6: Vũ khí 2.5D (Extruded 3D Weapon with Metallic Glint) ---
        AffineTransform oldTransform = g2d.getTransform();
        int weaponHandX = px + (dir == Direction.LEFT ? -12 : 12);
        int weaponHandY = py + 2;

        g2d.translate(weaponHandX, weaponHandY);
        if (anim == LpcAnimationType.SLASH) {
            g2d.rotate(swingAngle);
        } else {
            g2d.rotate(dir.getRowOffset() * 0.4 - 0.6);
        }

        // Vẽ kiếm sắt thể tích 3D
        // Bóng dưới chuôi kiếm
        g2d.setColor(new Color(60, 40, 20));
        g2d.fillRect(-1, 2, 4, 8);
        // Chuôi kiếm gỗ phong
        g2d.setColor(new Color(145, 95, 45));
        g2d.fillRect(-2, 0, 4, 8);
        // Chắn tay hoàng kim có gờ nổi 3D
        g2d.setColor(new Color(180, 140, 30));
        g2d.fillRect(-6, -1, 12, 3);
        g2d.setColor(new Color(255, 220, 70));
        g2d.fillRect(-6, -2, 11, 2);
        // Lưỡi kiếm bạc vát 2 mặt (Dual-tone Beveled Blade)
        g2d.setColor(new Color(185, 195, 210)); // Mặt khuất bóng
        g2d.fillRect(0, -26, 3, 24);
        g2d.setColor(new Color(240, 245, 255)); // Mặt đón sáng
        g2d.fillRect(-3, -26, 3, 24);
        // Sống kiếm phát sáng phản quang (Specular Core Line)
        g2d.setColor(Color.WHITE);
        g2d.drawLine(0, -26, 0, -2);

        // Mũi nhọn kiếm
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(new int[]{-3, 0, 3}, new int[]{-26, -30, -26}, 3);

        g2d.setTransform(oldTransform);
    }

    /**
     * Dựng hình quái vật với hiệu ứng thể tích 2.5D.
     */
    public static void renderMonster(Graphics2D g2d, Monster monster, double screenX, double screenY) {
        if (!monster.isAlive()) return;

        Direction dir = monster.getFacingDirection();
        Monster.MonsterType type = monster.getType();
        int px = (int) screenX;
        int py = (int) screenY;

        switch (type) {
            case SLIME:
                // Bóng đổ dẹt dưới chân
                render25DDirectionalShadow(g2d, screenX, screenY + 4, 36, 14);

                // Thân Slime dạng giọt nước thạch 3D
                g2d.setColor(new Color(35, 145, 35, 200)); // Lớp đáy bóng
                g2d.fillOval(px - 16, py - 10, 32, 22);
                g2d.setColor(new Color(50, 215, 60, 225)); // Lớp chính
                g2d.fillOval(px - 15, py - 13, 30, 22);

                // Nhân thạch phát sáng bên trong (Internal Core)
                g2d.setColor(new Color(120, 255, 130, 180));
                g2d.fillOval(px - 8, py - 8, 16, 12);

                // Điểm sáng phản chiếu gương 3D (3D Specular Reflection)
                g2d.setColor(new Color(255, 255, 255, 220));
                g2d.fillOval(px - 10, py - 11, 7, 4);

                // Mắt Slime 3D
                g2d.setColor(new Color(20, 40, 20));
                g2d.fillOval(px - 6 + dir.getDirX() * 2, py - 4, 4, 4);
                g2d.fillOval(px + 3 + dir.getDirX() * 2, py - 4, 4, 4);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(px - 6 + dir.getDirX() * 2, py - 4, 1, 1);
                g2d.fillRect(px + 3 + dir.getDirX() * 2, py - 4, 1, 1);
                break;

            case GOBLIN:
                render25DDirectionalShadow(g2d, screenX, screenY, 30, 14);

                // Đầu Goblin
                g2d.setColor(new Color(60, 110, 45)); // Bóng đầu
                g2d.fillOval(px - 10, py - 18, 20, 16);
                g2d.setColor(new Color(88, 160, 68)); // Sáng đầu
                g2d.fillOval(px - 10, py - 18, 16, 14);

                // Tai nhọn 3D
                g2d.setColor(new Color(88, 160, 68));
                g2d.fillPolygon(new int[]{px - 10, px - 18, px - 8}, new int[]{py - 14, py - 20, py - 10}, 3);
                g2d.fillPolygon(new int[]{px + 10, px + 18, px + 8}, new int[]{py - 14, py - 20, py - 10}, 3);

                // Mắt vàng rực
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillRect(px - 6, py - 12, 3, 3);
                g2d.fillRect(px + 3, py - 12, 3, 3);
                g2d.setColor(Color.BLACK);
                g2d.fillRect(px - 5, py - 12, 1, 3);
                g2d.fillRect(px + 4, py - 12, 1, 3);

                // Thân áo da nâu 2.5D
                g2d.setColor(new Color(85, 50, 30));
                g2d.fillRoundRect(px - 8, py - 2, 16, 14, 4, 4);
                g2d.setColor(new Color(130, 80, 45));
                g2d.fillRoundRect(px - 8, py - 2, 12, 12, 4, 4);

                // Dao găm kim loại có sống sắc bén
                g2d.setColor(new Color(80, 85, 95));
                g2d.fillRect(px + 8, py + 3, 9, 3);
                g2d.setColor(Color.WHITE);
                g2d.drawLine(px + 8, py + 3, px + 16, py + 3);
                break;

            case ORC_WARRIOR:
                render25DDirectionalShadow(g2d, screenX, screenY + 2, 44, 18);

                // Thân giáp sắt nặng 3D vát khối
                g2d.setColor(new Color(50, 55, 65));
                g2d.fillRoundRect(px - 14, py - 4, 28, 20, 6, 6);
                g2d.setColor(new Color(95, 105, 120));
                g2d.fillRoundRect(px - 13, py - 4, 20, 16, 6, 6);

                // Giáp vai gai nhọn (Heavy Pauldrons)
                g2d.setColor(new Color(185, 55, 55));
                g2d.fillOval(px - 18, py - 12, 10, 10);
                g2d.fillOval(px + 8, py - 12, 10, 10);
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(new int[]{px - 14, px - 18, px - 10}, new int[]{py - 16, py - 10, py - 10}, 3);
                g2d.fillPolygon(new int[]{px + 14, px + 10, px + 18}, new int[]{py - 16, py - 10, py - 10}, 3);

                // Đầu Orc bự cơ bắp
                g2d.setColor(new Color(55, 90, 65));
                g2d.fillOval(px - 13, py - 24, 26, 20);
                g2d.setColor(new Color(75, 125, 85));
                g2d.fillOval(px - 13, py - 24, 20, 16);

                // Mắt đỏ ngầu
                g2d.setColor(Color.RED);
                g2d.fillRect(px - 6, py - 16, 3, 2);
                g2d.fillRect(px + 3, py - 16, 3, 2);

                // Răng nanh chìa lên
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(new int[]{px - 6, px - 4, px - 2}, new int[]{py - 8, py - 14, py - 8}, 3);
                g2d.fillPolygon(new int[]{px + 2, px + 4, px + 6}, new int[]{py - 8, py - 14, py - 8}, 3);

                // Rìu chiến khổng lồ 2.5D (Double-edged Battleaxe)
                g2d.setColor(new Color(90, 50, 30));
                g2d.fillRect(px + 14, py - 20, 5, 26);
                g2d.setColor(new Color(140, 150, 165));
                g2d.fillArc(px + 8, py - 24, 18, 18, 0, 180);
                g2d.setColor(Color.WHITE);
                g2d.drawArc(px + 8, py - 24, 18, 18, 0, 180); // Lưỡi rìu sắc bén
                break;
        }

        // Trạng thái thanh máu & Tên dạng 3D Overhead Billboard
        renderOverheadStatus(g2d, screenX, screenY, type.displayName, monster.getCurrentHp(), monster.getMaxHp(), false);
    }

    /**
     * Dựng hình bảng chỉ số trên đầu (Overhead Status Billboard) với độ nổi 2.5D.
     */
    private static void renderOverheadStatus(Graphics2D g2d, double x, double y, String nameTag, int currentHp, int maxHp, boolean isPlayer) {
        int barW = 38;
        int barH = 5;
        int barX = (int) x - barW / 2;
        int barY = (int) y - 32;

        // Vẽ tên
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int nameW = fm.stringWidth(nameTag);

        // Nền tên có bóng đổ
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect((int) x - nameW / 2 - 4, barY - 14, nameW + 8, 12, 4, 4);
        g2d.setColor(isPlayer ? new Color(0, 240, 255) : Color.WHITE);
        g2d.drawString(nameTag, (int) x - nameW / 2, barY - 4);

        // Khung thanh máu có viền 3D dập nổi
        g2d.setColor(new Color(15, 15, 20, 230));
        g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);
        g2d.setColor(new Color(50, 55, 65));
        g2d.drawRect(barX - 1, barY - 1, barW + 2, barH + 2);

        // Lượng máu hiện tại với gradient
        float pct = Math.max(0f, Math.min(1f, (float) currentHp / maxHp));
        Color hpColor = pct > 0.5f ? new Color(46, 204, 113) : (pct > 0.25f ? new Color(241, 196, 15) : new Color(231, 76, 60));
        g2d.setColor(hpColor);
        g2d.fillRect(barX, barY, (int) (barW * pct), barH);

        // Gờ sáng trên cùng thanh máu
        g2d.setColor(new Color(255, 255, 255, 90));
        g2d.drawLine(barX, barY, barX + (int) (barW * pct) - 1, barY);
    }
}
