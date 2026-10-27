package combat;

import core.Monster;
import core.Player;
import core.Vector2D;
import java.util.List;

/**
 * Chiêu 3 Ultimate (☄️ THIÊN PHẠT): Chọn vùng thiên thạch tầm xa 280px, bán kính nổ 165px,
 * gây sát thương Chí Mạng 280% và hất văng (Knockback) toàn bộ mục tiêu khỏi tâm vụ nổ.
 */
public class MeteorUltimateSkill implements ISkillAction {
    @Override
    public int getSkillId() {
        return 3;
    }

    @Override
    public double getMaxRange() {
        return CombatEngine.SKILL3_MAX_RANGE;
    }

    @Override
    public double getAoeRadius() {
        return CombatEngine.SKILL3_AOE_RADIUS;
    }

    @Override
    public int execute(Player caster, double aimX, double aimY, List<Monster> monsters,
                       IDamageStrategy damageStrategy, CombatEngine engine) {
        double startX = caster.getPosition().getX();
        double startY = caster.getPosition().getY();
        double impactX = Math.max(40, Math.min(1560, startX + aimX * getMaxRange()));
        double impactY = Math.max(40, Math.min(1160, startY + aimY * getMaxRange()));
        Vector2D impactPos = new Vector2D(impactX, impactY);

        spawnVisuals(startX, startY, aimX, aimY, engine);

        int hitCount = 0;
        for (Monster m : monsters) {
            if (!m.isAlive()) continue;
            double dist = m.getPosition().distanceTo(impactPos);
            if (dist <= getAoeRadius() + 20.0) {
                int dmg = (int) (damageStrategy.calculateDamage(32, caster, m) * 2.8);
                int actual = m.takeDamage(dmg);
                hitCount++;
                engine.spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), "💥 CRIT -" + actual, CombatEngine.COLOR_PURPLE);
                engine.spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), CombatEngine.COLOR_YELLOW, 16);

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
                engine.notifyMonsterIfDead(caster, m);
            }
        }
        return hitCount;
    }

    @Override
    public void spawnVisuals(double startX, double startY, double aimX, double aimY, CombatEngine engine) {
        double impactX = Math.max(40, Math.min(1560, startX + aimX * getMaxRange()));
        double impactY = Math.max(40, Math.min(1160, startY + aimY * getMaxRange()));

        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(impactX, impactY, getAoeRadius(), CombatEngine.COLOR_YELLOW, 0.62, 3));
        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(impactX, impactY, getAoeRadius() * 0.72, CombatEngine.COLOR_PURPLE, 0.50, 3));
        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(impactX, impactY, getAoeRadius() * 0.42, CombatEngine.COLOR_RED, 0.40, 3));
        engine.spawnHitParticles(impactX, impactY, CombatEngine.COLOR_YELLOW, 26);
        engine.spawnHitParticles(impactX, impactY, CombatEngine.COLOR_PURPLE, 18);
        engine.spawnDamageNumber(impactX, impactY - 35, "☄️ THIÊN PHẠT (ULTIMATE)!", CombatEngine.COLOR_YELLOW);
    }
}
