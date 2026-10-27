package render;

import core.LivingEntity;
import core.Monster;
import core.Player;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Bộ vẽ đồ họa Canvas 2D theo chuẩn Universal LPC và cơ chế vẽ dự phòng (Procedural Fallback).
 * Nếu tệp ảnh PNG tồn tại trên đĩa: Thực hiện cắt ghép ảnh ma trận 64x64 siêu tốc (Hardware Blitting).
 * Nếu chưa tải ảnh PNG: Tự động vẽ giả lập mô hình nhân vật Vector/Pixel đa lớp chuẩn LPC
 * (Đảm bảo game luôn chạy mượt mà 100% không bao giờ gặp lỗi thiếu ảnh).
 */
public class LpcCanvasRenderer {

    public static void renderCharacter(Graphics2D g2d, CharacterRenderer renderer, double screenX, double screenY, String nameTag, int currentHp, int maxHp, boolean isPlayer) {
        CompositeCharacterVisual visual = renderer.getVisual();
        Direction dir = renderer.getCurrentDirection();
        LpcAnimationType anim = renderer.getCurrentAnimation();
        int frameIdx = renderer.getCurrentFrameIndex();

        // 1. Kiểm tra xem có ảnh kết cấu thật từ Spritesheet không
        boolean hasTexture = false;
        List<SpriteLayer> layers = visual.getVisibleLayersSorted();
        for (SpriteLayer layer : layers) {
            if (layer.getSpriteSheet().hasLoadedImage()) {
                hasTexture = true;
                break;
            }
        }

        // Vẽ bóng đổ (Drop Shadow) dưới chân nhân vật
        g2d.setColor(new Color(0, 0, 0, 70));
        g2d.fillOval((int) screenX - 16, (int) screenY + 18, 32, 10);

        if (hasTexture) {
            // Vẽ bằng SpriteSheet thật (Painter's Algorithm: Lớp Z thấp vẽ trước, Z cao vẽ sau)
            Map<LayerType, Rectangle> frameMap = visual.resolveRenderFrames(anim, dir, frameIdx);
            for (SpriteLayer layer : layers) {
                BufferedImage img = layer.getSpriteSheet().getTextureImage();
                Rectangle clip = frameMap.get(layer.getLayerType());
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
            // Vẽ Procedural Vector/Pixel Avatar chuẩn các lớp LPC
            renderProceduralLpcAvatar(g2d, renderer, screenX, screenY, isPlayer);
        }

        // 2. Vẽ thanh máu nhỏ (Health Bar) và Tên nhân vật trên đầu
        renderOverheadStatus(g2d, screenX, screenY, nameTag, currentHp, maxHp, isPlayer);
    }

    private static void renderProceduralLpcAvatar(Graphics2D g2d, CharacterRenderer renderer, double x, double y, boolean isPlayer) {
        Direction dir = renderer.getCurrentDirection();
        LpcAnimationType anim = renderer.getCurrentAnimation();
        int frame = renderer.getCurrentFrameIndex();

        // Tính toán độ nhún khi đi bộ (Bobbing effect)
        double bobY = (anim == LpcAnimationType.WALK) ? Math.sin(frame * 0.8) * 2.5 : 0;
        double swingAngle = 0.0;
        if (anim == LpcAnimationType.SLASH) {
            swingAngle = (frame - 2.5) * 0.6; // Động tác chém xoay kiếm
        }

        int px = (int) x;
        int py = (int) (y + bobY);

        Color skinColor = isPlayer ? new Color(240, 195, 155) : new Color(130, 185, 120);
        Color shirtColor = isPlayer ? new Color(45, 115, 195) : new Color(165, 80, 50);
        Color armorColor = isPlayer ? new Color(190, 200, 215) : new Color(95, 105, 115);
        Color hairColor = isPlayer ? new Color(90, 50, 30) : new Color(40, 35, 30);
        Color pantsColor = new Color(55, 60, 70);

        // --- LAYER 1: Chân & Quần (Pants) ---
        g2d.setColor(pantsColor);
        int legOffset = (anim == LpcAnimationType.WALK) ? (int) (Math.sin(frame * 1.2) * 5) : 0;
        g2d.fillRect(px - 8, py + 10, 6, 12 + legOffset);
        g2d.fillRect(px + 2, py + 10, 6, 12 - legOffset);

        // --- LAYER 2: Thân & Áo (Torso & Shirt) ---
        g2d.setColor(shirtColor);
        g2d.fillRoundRect(px - 10, py - 4, 20, 16, 6, 6);

        // --- LAYER 3: Giáp Ngực (Armor Cuirass) ---
        g2d.setColor(armorColor);
        g2d.fillRoundRect(px - 8, py - 2, 16, 12, 4, 4);
        g2d.setColor(armorColor.brighter());
        g2d.drawRoundRect(px - 8, py - 2, 16, 12, 4, 4);

        // --- LAYER 4: Đầu & Mặt (Head & Eyes) ---
        g2d.setColor(skinColor);
        g2d.fillOval(px - 9, py - 22, 18, 18);

        // Mắt theo hướng nhìn
        g2d.setColor(Color.BLACK);
        if (dir == Direction.DOWN) {
            g2d.fillRect(px - 5, py - 14, 3, 3);
            g2d.fillRect(px + 2, py - 14, 3, 3);
        } else if (dir == Direction.LEFT) {
            g2d.fillRect(px - 7, py - 14, 3, 3);
        } else if (dir == Direction.RIGHT) {
            g2d.fillRect(px + 4, py - 14, 3, 3);
        } // Hướng UP (quay lưng) không vẽ mắt

        // --- LAYER 5: Tóc / Mũ (Hair / Helmet) ---
        g2d.setColor(hairColor);
        g2d.fillArc(px - 10, py - 24, 20, 16, 0, 180);
        if (dir == Direction.UP) {
            g2d.fillOval(px - 9, py - 22, 18, 14); // Nhìn từ sau thấy trọn tóc
        }

        // --- LAYER 6: Vũ khí (Weapon) ---
        AffineTransform oldTransform = g2d.getTransform();
        int weaponHandX = px + (dir == Direction.LEFT ? -12 : 12);
        int weaponHandY = py + 2;

        g2d.translate(weaponHandX, weaponHandY);
        if (anim == LpcAnimationType.SLASH) {
            g2d.rotate(swingAngle);
        } else {
            g2d.rotate(dir.getRowOffset() * 0.4 - 0.6);
        }

        // Vẽ kiếm sắt
        g2d.setColor(new Color(130, 90, 40)); // Chuôi kiếm gỗ
        g2d.fillRect(-2, 0, 4, 8);
        g2d.setColor(new Color(220, 190, 50)); // Chắn tay hoàng kim
        g2d.fillRect(-6, -2, 12, 3);
        g2d.setColor(new Color(230, 235, 245)); // Lưỡi kiếm bạc
        g2d.fillRect(-3, -24, 6, 22);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(0, -24, 0, -2); // Sống kiếm phát sáng

        g2d.setTransform(oldTransform);
    }

    public static void renderMonster(Graphics2D g2d, Monster monster, double screenX, double screenY) {
        if (!monster.isAlive()) return;

        double x = screenX;
        double y = screenY;
        Direction dir = monster.getFacingDirection();

        // Bóng đổ
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval((int) x - 14, (int) y + 14, 28, 8);

        Monster.MonsterType type = monster.getType();
        int px = (int) x;
        int py = (int) y;

        switch (type) {
            case SLIME:
                // Slime dạng thạch rung rinh
                g2d.setColor(new Color(50, 205, 50, 220));
                g2d.fillOval(px - 16, py - 12, 32, 24);
                g2d.setColor(new Color(144, 238, 144));
                g2d.fillOval(px - 10, py - 10, 10, 6);
                // Mắt Slime
                g2d.setColor(Color.BLACK);
                g2d.fillOval(px - 6 + dir.getDirX() * 2, py - 4, 4, 4);
                g2d.fillOval(px + 3 + dir.getDirX() * 2, py - 4, 4, 4);
                break;

            case GOBLIN:
                // Goblin yêu tinh xanh lá
                g2d.setColor(new Color(85, 150, 65));
                g2d.fillOval(px - 10, py - 18, 20, 16); // Đầu
                // Tai nhọn
                g2d.fillPolygon(new int[]{px - 10, px - 16, px - 8}, new int[]{py - 14, py - 20, py - 10}, 3);
                g2d.fillPolygon(new int[]{px + 10, px + 16, px + 8}, new int[]{py - 14, py - 20, py - 10}, 3);
                // Mắt vàng rực
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(px - 6, py - 12, 3, 3);
                g2d.fillRect(px + 3, py - 12, 3, 3);
                // Thân mặc áo da nâu
                g2d.setColor(new Color(110, 70, 40));
                g2d.fillRoundRect(px - 8, py - 2, 16, 14, 4, 4);
                // Dao găm
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(px + 8, py + 2, 8, 3);
                break;

            case ORC_WARRIOR:
                // Orc chiến binh to lớn, giáp sắt
                g2d.setColor(new Color(70, 110, 80));
                g2d.fillOval(px - 13, py - 24, 26, 20); // Đầu bự
                // Răng nanh chìa lên
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(new int[]{px - 6, px - 4, px - 2}, new int[]{py - 8, py - 14, py - 8}, 3);
                g2d.fillPolygon(new int[]{px + 2, px + 4, px + 6}, new int[]{py - 8, py - 14, py - 8}, 3);
                // Thân giáp sắt nặng
                g2d.setColor(new Color(80, 85, 95));
                g2d.fillRoundRect(px - 12, py - 4, 24, 18, 6, 6);
                // Rìu chiến khổng lồ
                g2d.setColor(new Color(180, 50, 50));
                g2d.fillRect(px + 12, py - 18, 6, 24);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillArc(px + 8, py - 22, 16, 16, 0, 180);
                break;
        }

        // Trạng thái thanh máu quái vật
        renderOverheadStatus(g2d, screenX, screenY, type.displayName, monster.getCurrentHp(), monster.getMaxHp(), false);
    }

    private static void renderOverheadStatus(Graphics2D g2d, double x, double y, String nameTag, int currentHp, int maxHp, boolean isPlayer) {
        int barW = 36;
        int barH = 5;
        int barX = (int) x - barW / 2;
        int barY = (int) y - 30;

        // Vẽ tên
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int nameW = fm.stringWidth(nameTag);

        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRoundRect((int) x - nameW / 2 - 3, barY - 14, nameW + 6, 12, 4, 4);
        g2d.setColor(isPlayer ? Color.CYAN : Color.WHITE);
        g2d.drawString(nameTag, (int) x - nameW / 2, barY - 4);

        // Khung thanh máu
        g2d.setColor(new Color(20, 20, 20, 200));
        g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

        // Lượng máu hiện tại
        float pct = Math.max(0f, Math.min(1f, (float) currentHp / maxHp));
        Color hpColor = pct > 0.5f ? new Color(50, 205, 50) : (pct > 0.25f ? Color.ORANGE : Color.RED);
        g2d.setColor(hpColor);
        g2d.fillRect(barX, barY, (int) (barW * pct), barH);
    }
}
