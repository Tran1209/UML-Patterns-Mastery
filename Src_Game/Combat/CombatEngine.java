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
    public static final int COLOR_CYAN = 0xFF00E5FF;
    public static final int COLOR_GREEN = 0xFF00E676;
    public static final int COLOR_PURPLE = 0xFFE040FB;

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

    public static class SkillRingEffect {
        public final double x, y;
        public final double maxRadius;
        public final int color;
        public double lifeTime;
        public final double maxLifeTime;
        public final int skillType; // 1: Xung Kích, 2: Vòng Xoáy Lửa, 3: Thiên Phạt Ultimate

        public SkillRingEffect(double x, double y, double maxRadius, int color, double maxLifeTime, int skillType) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
            this.skillType = skillType;
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
    private final List<SkillRingEffect> skillRings = new ArrayList<>();
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

    public List<SkillRingEffect> getSkillRings() {
        return skillRings;
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

        skillRings.removeIf(sr -> {
            sr.update(dt);
            return sr.isExpired();
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
     * Thực thi 3 Kỹ năng (Skills) chuẩn phong cách Liên Quân Mobile (AOV):
     * - Skill 1 (⚡ XUNG KÍCH - Lướt chém xuyên phá): Lướt 155px về phía trước, gây sát thương dọc đường lướt.
     * - Skill 2 (🔥 VÒNG XOÁY LỬA - Kiếm Khí AoE + Hồi phục): Xoáy lửa bán kính 135px quanh thân + Hồi 18 HP.
     * - Skill 3 (☄️ THIÊN PHẠT - Ultimate Đại Địa Chấn): Nổ thiên thạch bán kính 210px, sát thương chí mạng x3.5 + Đẩy lùi!
     */
    public int executePlayerSkill(Player caster, int skillId, List<Monster> monsters) {
        if (caster == null || !caster.canCastSkill(skillId)) return 0;

        caster.triggerSkillCooldown(skillId);
        caster.changeState(new Player.AttackingState());
        Direction dir = caster.getFacingDirection();
        double startX = caster.getPosition().getX();
        double startY = caster.getPosition().getY();
        int hitCount = 0;

        if (skillId == 1) {
            // CHIÊU 1: ⚡ XUNG KÍCH (Lướt nhanh 155px + Chém xuyên thấu)
            double dashDist = 155.0;
            double endX = Math.max(40, Math.min(1560, startX + dir.getDirX() * dashDist));
            double endY = Math.max(40, Math.min(1160, startY + dir.getDirY() * dashDist));
            double midX = (startX + endX) * 0.5;
            double midY = (startY + endY) * 0.5;

            caster.setPosition(new Vector2D(endX, endY));
            spawnSkillVisualOnly(startX, startY, dir, 1);

            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                double dStart = m.getPosition().distanceTo(new Vector2D(startX, startY));
                double dMid = m.getPosition().distanceTo(new Vector2D(midX, midY));
                double dEnd = m.getPosition().distanceTo(new Vector2D(endX, endY));
                if (dStart <= 85 || dMid <= 85 || dEnd <= 95) {
                    int dmg = (int) (physicalStrategy.calculateDamage(15, caster, m) * 1.5);
                    int actual = m.takeDamage(dmg);
                    hitCount++;
                    spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), actual, COLOR_CYAN);
                    spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), COLOR_CYAN, 10);
                    checkMonsterDeath(caster, m);
                }
            }
        } else if (skillId == 2) {
            // CHIÊU 2: 🔥 VÒNG XOÁY LỬA (Xoáy AoE bán kính 135px + Hút máu chiến trường +18 HP)
            caster.heal(18);
            for (IEntityEventListener listener : eventListeners) {
                listener.onHealthChanged(caster.getId(), caster.getCurrentHp(), caster.getMaxHp());
            }
            spawnSkillVisualOnly(startX, startY, dir, 2);

            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                if (m.getPosition().distanceTo(caster.getPosition()) <= 140.0) {
                    int dmg = (int) (physicalStrategy.calculateDamage(20, caster, m) * 1.8);
                    int actual = m.takeDamage(dmg);
                    hitCount++;
                    spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), actual, COLOR_ORANGE);
                    spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), COLOR_ORANGE, 12);
                    checkMonsterDeath(caster, m);
                }
            }
        } else if (skillId == 3) {
            // CHIÊU 3 (ULTIMATE): ☄️ THIÊN PHẠT (Đại Địa Chấn Hoàng Kim bán kính 215px + Chí mạng x3.2 + Đẩy lùi)
            double impactX = startX + dir.getDirX() * 55;
            double impactY = startY + dir.getDirY() * 55;
            Vector2D impactPos = new Vector2D(impactX, impactY);
            spawnSkillVisualOnly(startX, startY, dir, 3);

            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                double dist = m.getPosition().distanceTo(impactPos);
                if (dist <= 215.0) {
                    int dmg = (int) (physicalStrategy.calculateDamage(32, caster, m) * 2.8);
                    int actual = m.takeDamage(dmg);
                    hitCount++;
                    spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), "💥 CRIT -" + actual, COLOR_PURPLE);
                    spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), COLOR_YELLOW, 16);

                    // Hiệu ứng Đẩy lùi (Knockback) chuẩn Ultimate AOV
                    if (m.isAlive() && dist > 1.0) {
                        double kx = m.getPosition().getX() + ((m.getPosition().getX() - impactX) / dist) * 65.0;
                        double ky = m.getPosition().getY() + ((m.getPosition().getY() - impactY) / dist) * 65.0;
                        m.setPosition(new Vector2D(
                                Math.max(40, Math.min(1560, kx)),
                                Math.max(40, Math.min(1160, ky))
                        ));
                    }
                    checkMonsterDeath(caster, m);
                }
            }
        }
        return hitCount;
    }

    /**
     * Khởi tạo hiệu ứng thị giác của 3 chiêu thức (dùng chung cho cả Offline lẫn Multiplayer).
     */
    public void spawnSkillVisualOnly(double startX, double startY, Direction dir, int skillId) {
        if (dir == null) dir = Direction.DOWN;
        if (skillId == 1) {
            double endX = Math.max(40, Math.min(1560, startX + dir.getDirX() * 155.0));
            double endY = Math.max(40, Math.min(1160, startY + dir.getDirY() * 155.0));
            double midX = (startX + endX) * 0.5;
            double midY = (startY + endY) * 0.5;

            skillRings.add(new SkillRingEffect(startX, startY, 55, COLOR_CYAN, 0.30, 1));
            skillRings.add(new SkillRingEffect(midX, midY, 65, COLOR_CYAN, 0.35, 1));
            skillRings.add(new SkillRingEffect(endX, endY, 85, COLOR_CYAN, 0.40, 1));
            slashEffects.add(new SlashEffect(endX + dir.getDirX() * 28, endY + dir.getDirY() * 28, dir, 0.28));
            spawnHitParticles(endX, endY, COLOR_CYAN, 14);
            spawnDamageNumber(endX, endY - 25, "⚡ XUNG KÍCH!", COLOR_CYAN);
        } else if (skillId == 2) {
            skillRings.add(new SkillRingEffect(startX, startY, 140, COLOR_ORANGE, 0.45, 2));
            skillRings.add(new SkillRingEffect(startX, startY, 95, COLOR_YELLOW, 0.38, 2));
            spawnHitParticles(startX, startY, COLOR_ORANGE, 20);
            spawnDamageNumber(startX, startY - 30, "🔥 VÒNG XOÁY LỬA (+18 HP)", COLOR_GREEN);
        } else if (skillId == 3) {
            double impactX = startX + dir.getDirX() * 55;
            double impactY = startY + dir.getDirY() * 55;
            skillRings.add(new SkillRingEffect(impactX, impactY, 215, COLOR_YELLOW, 0.60, 3));
            skillRings.add(new SkillRingEffect(impactX, impactY, 155, COLOR_PURPLE, 0.50, 3));
            skillRings.add(new SkillRingEffect(impactX, impactY, 90, COLOR_RED, 0.40, 3));
            spawnHitParticles(impactX, impactY, COLOR_YELLOW, 25);
            spawnHitParticles(impactX, impactY, COLOR_PURPLE, 18);
            spawnDamageNumber(impactX, impactY - 35, "☄️ THIÊN PHẠT (ULTIMATE)!", COLOR_YELLOW);
        }
    }

    private void checkMonsterDeath(Player attacker, Monster monster) {
        if (!monster.isAlive()) {
            attacker.addKill();
            spawnDamageNumber(monster.getPosition().getX(), monster.getPosition().getY() - 20, "DIỆT!", COLOR_RED);
            for (IEntityEventListener listener : eventListeners) {
                listener.onEntityDied(monster.getType().displayName, attacker.getPlayerName());
            }
        }
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
