package combat;

import core.Monster;
import core.Player;
import core.Vector2D;
import java.util.List;

/**
 * Chiêu 2 (🔥 VÒNG XOÁY LỬA): Chọn vùng tròn tầm trung 210px — Nổ lửa bán kính 120px tại tâm ngắm và hồi +18 HP.
 */
public class FireWhirlwindSkill implements ISkillAction {
    @Override
    public int getSkillId() {
        return 2;
    }

    @Override
    public double getMaxRange() {
        return CombatEngine.SKILL2_MAX_RANGE;
    }

    @Override
    public double getAoeRadius() {
        return CombatEngine.SKILL2_AOE_RADIUS;
    }

    @Override
    public int execute(Player caster, double aimX, double aimY, List<Monster> monsters,
                       IDamageStrategy damageStrategy, CombatEngine engine) {
        double startX = caster.getPosition().getX();
        double startY = caster.getPosition().getY();
        double targetX = Math.max(40, Math.min(1560, startX + aimX * getMaxRange()));
        double targetY = Math.max(40, Math.min(1160, startY + aimY * getMaxRange()));
        Vector2D targetPos = new Vector2D(targetX, targetY);

        caster.heal(18);
        engine.notifyHealthChanged(caster);
        spawnVisuals(startX, startY, aimX, aimY, engine);

        int hitCount = 0;
        for (Monster m : monsters) {
            if (!m.isAlive()) continue;
            if (m.getPosition().distanceTo(targetPos) <= getAoeRadius() + 18.0) {
                int dmg = (int) (damageStrategy.calculateDamage(20, caster, m) * 1.8);
                int actual = m.takeDamage(dmg);
                hitCount++;
                engine.spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), actual, CombatEngine.COLOR_ORANGE);
                engine.spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), CombatEngine.COLOR_ORANGE, 12);
                engine.notifyMonsterIfDead(caster, m);
            }
        }
        return hitCount;
    }

    @Override
    public void spawnVisuals(double startX, double startY, double aimX, double aimY, CombatEngine engine) {
        double targetX = Math.max(40, Math.min(1560, startX + aimX * getMaxRange()));
        double targetY = Math.max(40, Math.min(1160, startY + aimY * getMaxRange()));

        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(targetX, targetY, getAoeRadius(), CombatEngine.COLOR_ORANGE, 0.48, 2));
        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(targetX, targetY, getAoeRadius() * 0.68, CombatEngine.COLOR_YELLOW, 0.38, 2));
        engine.spawnHitParticles(targetX, targetY, CombatEngine.COLOR_ORANGE, 22);
        engine.spawnDamageNumber(targetX, targetY - 30, "🔥 VÒNG XOÁY LỬA (+18 HP)", CombatEngine.COLOR_GREEN);
    }
}
