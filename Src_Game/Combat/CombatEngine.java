package combat;

import core.LivingEntity;
import core.Monster;
import core.Player;
import render.Direction;
import ui.IEntityEventListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Động cơ điều phối chiến đấu (Combat Engine) tuân thủ chuẩn SOLID:
 * - SRP (Single Responsibility): Chỉ điều phối va chạm lưới không gian & ủy quyền kỹ năng.
 * - OCP (Open/Closed): Các kỹ năng AOV được đóng gói độc lập qua giao diện {@link ISkillAction}.
 * - Hiệu ứng thị giác được tách riêng vào {@link CombatEffects}.
 */
public class CombatEngine {
    public static final int COLOR_YELLOW = 0xFFFFD700;
    public static final int COLOR_RED = 0xFFFF2222;
    public static final int COLOR_ORANGE = 0xFFFF8C00;
    public static final int COLOR_CYAN = 0xFF00E5FF;
    public static final int COLOR_GREEN = 0xFF00E676;
    public static final int COLOR_PURPLE = 0xFFE040FB;

    public static final double SKILL1_MAX_RANGE = 165.0;
    public static final double SKILL1_CORRIDOR_WIDTH = 64.0;
    public static final double SKILL2_MAX_RANGE = 210.0;
    public static final double SKILL2_AOE_RADIUS = 120.0;
    public static final double SKILL3_MAX_RANGE = 280.0;
    public static final double SKILL3_AOE_RADIUS = 165.0;

    private final SpatialGridAoI spatialGrid;
    private final IDamageStrategy physicalStrategy;
    private final Map<Integer, ISkillAction> skillRegistry = new HashMap<>();

    private final List<CombatEffects.FloatingText> floatingTexts = new CopyOnWriteArrayList<>();
    private final List<CombatEffects.HitParticle> particles = new CopyOnWriteArrayList<>();
    private final List<CombatEffects.SlashEffect> slashEffects = new CopyOnWriteArrayList<>();
    private final List<CombatEffects.SkillRingEffect> skillRings = new CopyOnWriteArrayList<>();
    private final List<IEntityEventListener> eventListeners = new CopyOnWriteArrayList<>();

    public CombatEngine(int gridCellSize) {
        this.spatialGrid = new SpatialGridAoI(gridCellSize);
        this.physicalStrategy = new PhysicalDamageStrategy();

        // Đăng ký bộ 3 Kỹ năng AOV theo mẫu Command / Strategy Pattern
        registerSkill(new DashStrikeSkill());
        registerSkill(new FireWhirlwindSkill());
        registerSkill(new MeteorUltimateSkill());
    }

    public void registerSkill(ISkillAction skillAction) {
        if (skillAction != null) {
            skillRegistry.put(skillAction.getSkillId(), skillAction);
        }
    }

    public void addEventListener(IEntityEventListener listener) {
        if (listener != null) {
            this.eventListeners.add(listener);
        }
    }

    public SpatialGridAoI getSpatialGrid() {
        return spatialGrid;
    }

    public List<CombatEffects.FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    public List<CombatEffects.HitParticle> getParticles() {
        return particles;
    }

    public List<CombatEffects.SlashEffect> getSlashEffects() {
        return slashEffects;
    }

    public List<CombatEffects.SkillRingEffect> getSkillRings() {
        return skillRings;
    }

    public void rebuildSpatialGrid(List<LivingEntity> allEntities) {
        spatialGrid.clear();
        for (LivingEntity e : allEntities) {
            if (e.isAlive()) {
                spatialGrid.insert(e);
            }
        }
    }

    public void updateEffects(double dt) {
        floatingTexts.removeIf(ft -> { ft.update(dt); return ft.isExpired(); });
        particles.removeIf(p -> { p.update(dt); return p.isExpired(); });
        slashEffects.removeIf(s -> { s.update(dt); return s.isExpired(); });
        skillRings.removeIf(sr -> { sr.update(dt); return sr.isExpired(); });
    }

    public int executePlayerAttack(Player attacker, List<Monster> monsters) {
        if (!attacker.canAttack()) return 0;

        attacker.changeState(new Player.AttackingState());
        attacker.triggerAttackCooldown(0.35);

        Hitbox attackBox = attacker.getAttackArea();
        Direction dir = attacker.getFacingDirection();

        slashEffects.add(new CombatEffects.SlashEffect(
                attacker.getPosition().getX() + dir.getDirX() * 32,
                attacker.getPosition().getY() + dir.getDirY() * 32,
                dir, 0.20
        ));

        List<LivingEntity> nearby = spatialGrid.queryNearby(attacker.getPosition());
        int hitCount = 0;

        for (LivingEntity entity : nearby) {
            if (entity instanceof Monster && entity.isAlive()) {
                Monster monster = (Monster) entity;
                if (attackBox.intersects(monster.getHitbox())) {
                    int damage = physicalStrategy.calculateDamage(8, attacker, monster);
                    int actualDamage = monster.takeDamage(damage);

                    hitCount++;
                    spawnDamageNumber(monster.getPosition().getX(), monster.getPosition().getY(), actualDamage, COLOR_YELLOW);
                    spawnHitParticles(monster.getPosition().getX(), monster.getPosition().getY(), COLOR_ORANGE, 8);
                    notifyMonsterIfDead(attacker, monster);
                }
            }
        }
        return hitCount;
    }

    public int executePlayerSkill(Player caster, int skillId, List<Monster> monsters) {
        return executePlayerSkill(caster, skillId, 0.0, 0.0, monsters);
    }

    public int executePlayerSkill(Player caster, int skillId, double aimX, double aimY, List<Monster> monsters) {
        ISkillAction action = skillRegistry.get(skillId);
        if (caster == null || action == null || !caster.canCastSkill(skillId)) return 0;

        caster.triggerSkillCooldown(skillId);
        caster.changeState(new Player.AttackingState());

        double[] resolvedAim = resolveSmartAim(caster, action, aimX, aimY, monsters);
        if (Math.hypot(resolvedAim[0], resolvedAim[1]) > 0.05) {
            caster.setFacingDirection(Direction.fromVelocity(resolvedAim[0], resolvedAim[1], caster.getFacingDirection()));
        }

        return action.execute(caster, resolvedAim[0], resolvedAim[1], monsters, physicalStrategy, this);
    }

    public void spawnSkillVisualOnly(double startX, double startY, Direction dir, int skillId) {
        if (dir == null) dir = Direction.DOWN;
        spawnSkillVisualOnly(startX, startY, dir.getDirX(), dir.getDirY(), skillId);
    }

    public void spawnSkillVisualOnly(double startX, double startY, double aimX, double aimY, int skillId) {
        ISkillAction action = skillRegistry.get(skillId);
        if (action == null) return;

        double mag = Math.hypot(aimX, aimY);
        if (mag > 1.0) {
            aimX /= mag;
            aimY /= mag;
        }
        action.spawnVisuals(startX, startY, aimX, aimY, this);
    }

    private double[] resolveSmartAim(Player caster, ISkillAction action, double aimX, double aimY, List<Monster> monsters) {
        double mag = Math.hypot(aimX, aimY);
        if (mag < 0.08) {
            double maxRange = action.getMaxRange();
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
                double dx = nearest.getPosition().getX() - caster.getPosition().getX();
                double dy = nearest.getPosition().getY() - caster.getPosition().getY();
                double ratio = (action.getSkillId() == 1) ? 1.0 : Math.min(1.0, bestDist / maxRange);
                aimX = (dx / bestDist) * ratio;
                aimY = (dy / bestDist) * ratio;
            } else {
                Direction fDir = caster.getFacingDirection();
                aimX = fDir.getDirX() * (action.getSkillId() == 1 ? 1.0 : 0.55);
                aimY = fDir.getDirY() * (action.getSkillId() == 1 ? 1.0 : 0.55);
            }
            mag = Math.hypot(aimX, aimY);
        }
        if (mag > 1.0) {
            aimX /= mag;
            aimY /= mag;
        }
        return new double[]{aimX, aimY};
    }

    public void notifyMonsterIfDead(Player attacker, Monster monster) {
        if (!monster.isAlive()) {
            attacker.addKill();
            spawnDamageNumber(monster.getPosition().getX(), monster.getPosition().getY() - 20, "DIỆT!", COLOR_RED);
            for (IEntityEventListener listener : eventListeners) {
                listener.onEntityDied(monster.getType().displayName, attacker.getPlayerName());
            }
        }
    }

    public void notifyHealthChanged(Player player) {
        for (IEntityEventListener listener : eventListeners) {
            listener.onHealthChanged(player.getId(), player.getCurrentHp(), player.getMaxHp());
        }
    }

    public void executeMonsterAttacks(List<Monster> monsters, Player player) {
        if (player == null || !player.isAlive()) return;

        for (Monster monster : monsters) {
            if (!monster.isAlive() || !monster.canAttack()) continue;

            if (monster.getHitbox().intersects(player.getHitbox())) {
                monster.triggerAttackCooldown(1.0);
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
        if (floatingTexts.size() < 40) {
            floatingTexts.add(new CombatEffects.FloatingText(x, y - 10, "-" + damage, color, 0.8));
        }
    }

    public void spawnDamageNumber(double x, double y, String text, int color) {
        if (floatingTexts.size() < 40) {
            floatingTexts.add(new CombatEffects.FloatingText(x, y - 10, text, color, 1.2));
        }
    }

    public void spawnHitParticles(double x, double y, int color, int count) {
        if (particles.size() > 160) return;
        List<CombatEffects.HitParticle> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 40 + Math.random() * 120;
            batch.add(new CombatEffects.HitParticle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    color,
                    0.25 + Math.random() * 0.25,
                    2.0 + Math.random() * 3.0
            ));
        }
        particles.addAll(batch);
    }
}
