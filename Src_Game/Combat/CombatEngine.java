package combat;

import core.LivingEntity;
import core.Monster;
import core.Player;
import core.Vector2D;
import render.Direction;
import ui.IEntityEventListener;

import java.util.*;

/**
 * Động cơ xử lý chiến đấu (Combat Engine) theo mẫu kiến trúc phân vùng không gian.
 * Độc lập nền tảng 100%, không phụ thuộc java.awt.Color, tương thích cả Desktop lẫn Android.
 */
public class CombatEngine {
    public static final int COLOR_YELLOW = 0xFFFFD700;
    public static final int COLOR_RED = 0xFFFF2222;
    public static final int COLOR_ORANGE = 0xFFFF8C00;

    public static class FloatingText {
        public double x, y;
        public final String text;
        public final int color;
        public double lifeTime;
        public final double maxLifeTime;

        public FloatingText(double x, double y, String text, int color, double maxLifeTime) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
        }

        public void update(double dt) {
            y -= 45.0 * dt;
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    public static class HitParticle {
        public double x, y, vx, vy;
        public final int color;
        public double lifeTime;
        public final double maxLifeTime;
        public final double size;

        public HitParticle(double x, double y, double vx, double vy, int color, double maxLifeTime, double size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
            this.size = size;
        }

        public void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    public static class SlashEffect {
        public final double x, y;
        public final Direction direction;
        public double lifeTime;
        public final double maxLifeTime;

        public SlashEffect(double x, double y, Direction direction, double maxLifeTime) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
        }

        public void update(double dt) {
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    private final SpatialGridAoI spatialGrid;
    private final IDamageStrategy physicalStrategy;
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<HitParticle> particles = new ArrayList<>();
    private final List<SlashEffect> slashEffects = new ArrayList<>();
    private final List<IEntityEventListener> eventListeners = new ArrayList<>();

    public CombatEngine(int gridCellSize) {
        this.spatialGrid = new SpatialGridAoI(gridCellSize);
        this.physicalStrategy = new PhysicalDamageStrategy();
    }

    public void addEventListener(IEntityEventListener listener) {
        if (listener != null) {
            this.eventListeners.add(listener);
        }
    }

    public SpatialGridAoI getSpatialGrid() {
        return spatialGrid;
    }

    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    public List<HitParticle> getParticles() {
        return particles;
    }

    public List<SlashEffect> getSlashEffects() {
        return slashEffects;
    }

    /**
     * Tái tạo lại cây phân vùng lưới không gian với danh sách thực thể sống.
     */
    public void rebuildSpatialGrid(List<LivingEntity> allEntities) {
        spatialGrid.clear();
        for (LivingEntity e : allEntities) {
            if (e.isAlive()) {
                spatialGrid.insert(e);
            }
        }
    }

    /**
     * Cập nhật chuyển động của các hiệu ứng hạt và chữ nổi.
     */
    public void updateEffects(double dt) {
        floatingTexts.removeIf(ft -> {
            ft.update(dt);
            return ft.isExpired();
        });

        particles.removeIf(p -> {
            p.update(dt);
            return p.isExpired();
        });

        slashEffects.removeIf(s -> {
            s.update(dt);
            return s.isExpired();
        });
    }

    /**
     * Thực hiện đòn tấn công từ người chơi vào các quái vật lân cận.
     */
    public int executePlayerAttack(Player attacker, List<Monster> monsters) {
        if (!attacker.canAttack()) return 0;

        attacker.changeState(new Player.AttackingState());
        attacker.triggerAttackCooldown(0.35); // Cooldown 350ms

        Hitbox attackBox = attacker.getAttackArea();
        Direction dir = attacker.getFacingDirection();

        // Thêm hiệu ứng chém gió (Slash Effect)
        slashEffects.add(new SlashEffect(
                attacker.getPosition().getX() + dir.getDirX() * 32,
                attacker.getPosition().getY() + dir.getDirY() * 32,
                dir, 0.20
        ));

        // Lấy danh sách quái vật lân cận qua SpatialGrid
        List<LivingEntity> nearby = spatialGrid.queryNearby(attacker.getPosition());
        int hitCount = 0;

        for (LivingEntity entity : nearby) {
            if (entity instanceof Monster && entity.isAlive()) {
                Monster monster = (Monster) entity;
                if (attackBox.intersects(monster.getHitbox())) {
                    // Trúng đòn: Tính sát thương qua Strategy Pattern
                    int damage = physicalStrategy.calculateDamage(8, attacker, monster);
                    int actualDamage = monster.takeDamage(damage);

                    hitCount++;
                    spawnDamageNumber(monster.getPosition().getX(), monster.getPosition().getY(), actualDamage, COLOR_YELLOW);
                    spawnHitParticles(monster.getPosition().getX(), monster.getPosition().getY(), COLOR_ORANGE, 8);

                    // Kiểm tra tử vong
                    if (!monster.isAlive()) {
                        attacker.addKill();
                        spawnDamageNumber(monster.getPosition().getX(), monster.getPosition().getY() - 20, "DIỆT!", COLOR_RED);
                        for (IEntityEventListener listener : eventListeners) {
                            listener.onEntityDied(monster.getType().displayName, attacker.getPlayerName());
                        }
                    }
                }
            }
        }
        return hitCount;
    }

    /**
     * Kiểm tra và thực hiện đòn tấn công từ quái vật vào người chơi khi tiếp cận.
     */
    public void executeMonsterAttacks(List<Monster> monsters, Player player) {
        if (player == null || !player.isAlive()) return;

        for (Monster monster : monsters) {
            if (!monster.isAlive() || !monster.canAttack()) continue;

            if (monster.getHitbox().intersects(player.getHitbox())) {
                monster.triggerAttackCooldown(1.0); // 1 giây đánh 1 lần
                int damage = physicalStrategy.calculateDamage(0, monster, player);
                int actualDealt = player.takeDamage(damage);

                spawnDamageNumber(player.getPosition().getX(), player.getPosition().getY(), actualDealt, COLOR_RED);
                spawnHitParticles(player.getPosition().getX(), player.getPosition().getY(), COLOR_RED, 6);

                for (IEntityEventListener listener : eventListeners) {
                    listener.onHealthChanged(player.getId(), player.getCurrentHp(), player.getMaxHp());
                    if (!player.isAlive()) {
                        listener.onEntityDied(player.getPlayerName(), monster.getType().displayName);
                    }
                }
            }
        }
    }

    public void spawnDamageNumber(double x, double y, int damage, int color) {
        floatingTexts.add(new FloatingText(x, y - 10, "-" + damage, color, 0.8));
    }

    public void spawnDamageNumber(double x, double y, String text, int color) {
        floatingTexts.add(new FloatingText(x, y - 10, text, color, 1.2));
    }

    public void spawnHitParticles(double x, double y, int color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 40 + Math.random() * 120;
            particles.add(new HitParticle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    color,
                    0.25 + Math.random() * 0.25,
                    2.0 + Math.random() * 3.0
            ));
        }
    }
}
