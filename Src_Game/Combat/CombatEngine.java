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

    // Thông số Tầm xa (Cast Range) & Bán kính vùng ảnh hưởng (AoE Radius) chuẩn Liên Quân Mobile (AOV)
    public static final double SKILL1_MAX_RANGE = 165.0;
    public static final double SKILL1_CORRIDOR_WIDTH = 64.0;
    public static final double SKILL2_MAX_RANGE = 210.0;
    public static final double SKILL2_AOE_RADIUS = 120.0;
    public static final double SKILL3_MAX_RANGE = 280.0;
    public static final double SKILL3_AOE_RADIUS = 165.0;

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
     * Thực thi 3 Kỹ năng (Skills) chuẩn phong cách Liên Quân Mobile (AOV) kèm Chọn Vùng Kéo Thả (aimX, aimY):
     * - Skill 1 (⚡ XUNG KÍCH - Mũi tên định hướng 360°): Lướt 165px theo hướng kéo (aimX, aimY), chém xuyên phá.
     * - Skill 2 (🔥 VÒNG XOÁY LỬA - Chọn vùng tròn tầm trung 210px): Nổ lửa bán kính 120px tại tâm ngắm + Hồi 18 HP.
     * - Skill 3 (☄️ THIÊN PHẠT - Chọn vùng Ultimate tầm xa 280px): Giáng thiên thạch bán kính 165px tại tâm ngắm, Chí mạng + Đẩy lùi!
     */
    public int executePlayerSkill(Player caster, int skillId, List<Monster> monsters) {
        return executePlayerSkill(caster, skillId, 0.0, 0.0, monsters);
    }

    public int executePlayerSkill(Player caster, int skillId, double aimX, double aimY, List<Monster> monsters) {
        if (caster == null || !caster.canCastSkill(skillId)) return 0;

        caster.triggerSkillCooldown(skillId);
        caster.changeState(new Player.AttackingState());
        double startX = caster.getPosition().getX();
        double startY = caster.getPosition().getY();

        // Nếu người chơi bấm nhanh (Quick Tap) không kéo tâm ngắm -> Tự động khóa mục tiêu quái gần nhất trong tầm (Smart Auto-Aim AOV)
        double aimMag = Math.hypot(aimX, aimY);
        if (aimMag < 0.08) {
            double maxRange = (skillId == 1) ? SKILL1_MAX_RANGE : ((skillId == 2) ? SKILL2_MAX_RANGE : SKILL3_MAX_RANGE);
            Monster nearest = null;
            double bestDist = maxRange + 80.0;
            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                double d = m.getPosition().distanceTo(caster.getPosition());
                if (d < bestDist) {
                    bestDist = d;
                    nearest = m;
                }
            }
            if (nearest != null && bestDist > 1.0) {
                double dx = nearest.getPosition().getX() - startX;
                double dy = nearest.getPosition().getY() - startY;
                if (skillId == 1) {
                    aimX = dx / bestDist;
                    aimY = dy / bestDist;
                } else {
                    double ratio = Math.min(1.0, bestDist / maxRange);
                    aimX = (dx / bestDist) * ratio;
                    aimY = (dy / bestDist) * ratio;
                }
            } else {
                Direction fDir = caster.getFacingDirection();
                aimX = fDir.getDirX() * (skillId == 1 ? 1.0 : 0.55);
                aimY = fDir.getDirY() * (skillId == 1 ? 1.0 : 0.55);
            }
            aimMag = Math.hypot(aimX, aimY);
        }

        // Chuẩn hóa vector tâm ngắm không vượt quá bán kính tối đa 1.0
        if (aimMag > 1.0) {
            aimX /= aimMag;
            aimY /= aimMag;
            aimMag = 1.0;
        }

        if (aimMag > 0.05) {
            caster.setFacingDirection(Direction.fromVelocity(aimX, aimY, caster.getFacingDirection()));
        }

        int hitCount = 0;

        if (skillId == 1) {
            // CHIÊU 1: ⚡ XUNG KÍCH (Định hướng mũi tên 360 độ - Lướt nhanh 165px)
            double ux = (aimMag > 0.01) ? (aimX / aimMag) : caster.getFacingDirection().getDirX();
            double uy = (aimMag > 0.01) ? (aimY / aimMag) : caster.getFacingDirection().getDirY();
            double endX = Math.max(40, Math.min(1560, startX + ux * SKILL1_MAX_RANGE));
            double endY = Math.max(40, Math.min(1160, startY + uy * SKILL1_MAX_RANGE));
            double midX = (startX + endX) * 0.5;
            double midY = (startY + endY) * 0.5;

            caster.setPosition(new Vector2D(endX, endY));
            spawnSkillVisualOnly(startX, startY, ux, uy, 1);

            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                double dStart = m.getPosition().distanceTo(new Vector2D(startX, startY));
                double dMid = m.getPosition().distanceTo(new Vector2D(midX, midY));
                double dEnd = m.getPosition().distanceTo(new Vector2D(endX, endY));
                if (dStart <= 80 || dMid <= 80 || dEnd <= 92) {
                    int dmg = (int) (physicalStrategy.calculateDamage(15, caster, m) * 1.5);
                    int actual = m.takeDamage(dmg);
                    hitCount++;
                    spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), actual, COLOR_CYAN);
                    spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), COLOR_CYAN, 10);
                    checkMonsterDeath(caster, m);
                }
            }
        } else if (skillId == 2) {
            // CHIÊU 2: 🔥 VÒNG XOÁY LỬA (Chọn vùng tròn bán kính 120px trong tầm 210px + Hồi 18 HP)
            double targetX = Math.max(40, Math.min(1560, startX + aimX * SKILL2_MAX_RANGE));
            double targetY = Math.max(40, Math.min(1160, startY + aimY * SKILL2_MAX_RANGE));
            Vector2D targetPos = new Vector2D(targetX, targetY);

            caster.heal(18);
            for (IEntityEventListener listener : eventListeners) {
                listener.onHealthChanged(caster.getId(), caster.getCurrentHp(), caster.getMaxHp());
            }
            spawnSkillVisualOnly(startX, startY, aimX, aimY, 2);

            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                if (m.getPosition().distanceTo(targetPos) <= SKILL2_AOE_RADIUS + 18.0) {
                    int dmg = (int) (physicalStrategy.calculateDamage(20, caster, m) * 1.8);
                    int actual = m.takeDamage(dmg);
                    hitCount++;
                    spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), actual, COLOR_ORANGE);
                    spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), COLOR_ORANGE, 12);
                    checkMonsterDeath(caster, m);
                }
            }
        } else if (skillId == 3) {
            // CHIÊU 3 (ULTIMATE): ☄️ THIÊN PHẠT (Chọn vùng thiên thạch tầm xa 280px, bán kính nổ 165px + Chí mạng + Đẩy lùi)
            double impactX = Math.max(40, Math.min(1560, startX + aimX * SKILL3_MAX_RANGE));
            double impactY = Math.max(40, Math.min(1160, startY + aimY * SKILL3_MAX_RANGE));
            Vector2D impactPos = new Vector2D(impactX, impactY);
            spawnSkillVisualOnly(startX, startY, aimX, aimY, 3);

            for (Monster m : monsters) {
                if (!m.isAlive()) continue;
                double dist = m.getPosition().distanceTo(impactPos);
                if (dist <= SKILL3_AOE_RADIUS + 20.0) {
                    int dmg = (int) (physicalStrategy.calculateDamage(32, caster, m) * 2.8);
                    int actual = m.takeDamage(dmg);
                    hitCount++;
                    spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), "💥 CRIT -" + actual, COLOR_PURPLE);
                    spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), COLOR_YELLOW, 16);

                    // Hiệu ứng Đẩy lùi (Knockback) văng ra khỏi tâm vùng chọn chuẩn Ultimate AOV
                    if (m.isAlive()) {
                        double pushDirX = (dist > 1.0) ? ((m.getPosition().getX() - impactX) / dist) : 1.0;
                        double pushDirY = (dist > 1.0) ? ((m.getPosition().getY() - impactY) / dist) : 0.0;
                        double kx = m.getPosition().getX() + pushDirX * 70.0;
                        double ky = m.getPosition().getY() + pushDirY * 70.0;
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
     * Khởi tạo hiệu ứng thị giác của 3 chiêu thức theo tọa độ vùng chọn (dùng chung cho cả Offline lẫn Multiplayer).
     */
    public void spawnSkillVisualOnly(double startX, double startY, Direction dir, int skillId) {
        if (dir == null) dir = Direction.DOWN;
        spawnSkillVisualOnly(startX, startY, dir.getDirX(), dir.getDirY(), skillId);
    }

    public void spawnSkillVisualOnly(double startX, double startY, double aimX, double aimY, int skillId) {
        double mag = Math.hypot(aimX, aimY);
        if (mag > 1.0) {
            aimX /= mag;
            aimY /= mag;
            mag = 1.0;
        }
        Direction dir = Direction.fromVelocity(aimX, aimY, Direction.DOWN);

        if (skillId == 1) {
            double ux = (mag > 0.01) ? (aimX / mag) : dir.getDirX();
            double uy = (mag > 0.01) ? (aimY / mag) : dir.getDirY();
            double endX = Math.max(40, Math.min(1560, startX + ux * SKILL1_MAX_RANGE));
            double endY = Math.max(40, Math.min(1160, startY + uy * SKILL1_MAX_RANGE));
            double midX = (startX + endX) * 0.5;
            double midY = (startY + endY) * 0.5;

            skillRings.add(new SkillRingEffect(startX, startY, 55, COLOR_CYAN, 0.30, 1));
            skillRings.add(new SkillRingEffect(midX, midY, 65, COLOR_CYAN, 0.35, 1));
            skillRings.add(new SkillRingEffect(endX, endY, 85, COLOR_CYAN, 0.40, 1));
            slashEffects.add(new SlashEffect(endX + ux * 28, endY + uy * 28, dir, 0.28));
            spawnHitParticles(endX, endY, COLOR_CYAN, 14);
            spawnDamageNumber(endX, endY - 25, "⚡ XUNG KÍCH!", COLOR_CYAN);
        } else if (skillId == 2) {
            double targetX = Math.max(40, Math.min(1560, startX + aimX * SKILL2_MAX_RANGE));
            double targetY = Math.max(40, Math.min(1160, startY + aimY * SKILL2_MAX_RANGE));
            skillRings.add(new SkillRingEffect(targetX, targetY, SKILL2_AOE_RADIUS, COLOR_ORANGE, 0.48, 2));
            skillRings.add(new SkillRingEffect(targetX, targetY, SKILL2_AOE_RADIUS * 0.68, COLOR_YELLOW, 0.38, 2));
            spawnHitParticles(targetX, targetY, COLOR_ORANGE, 22);
            spawnDamageNumber(targetX, targetY - 30, "🔥 VÒNG XOÁY LỬA (+18 HP)", COLOR_GREEN);
        } else if (skillId == 3) {
            double impactX = Math.max(40, Math.min(1560, startX + aimX * SKILL3_MAX_RANGE));
            double impactY = Math.max(40, Math.min(1160, startY + aimY * SKILL3_MAX_RANGE));
            skillRings.add(new SkillRingEffect(impactX, impactY, SKILL3_AOE_RADIUS, COLOR_YELLOW, 0.62, 3));
            skillRings.add(new SkillRingEffect(impactX, impactY, SKILL3_AOE_RADIUS * 0.72, COLOR_PURPLE, 0.50, 3));
            skillRings.add(new SkillRingEffect(impactX, impactY, SKILL3_AOE_RADIUS * 0.42, COLOR_RED, 0.40, 3));
            spawnHitParticles(impactX, impactY, COLOR_YELLOW, 26);
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
