package combat;

import core.Monster;
import core.Player;
import core.Vector2D;
import render.Direction;
import java.util.List;

/**
 * Chiêu 1 (⚡ XUNG KÍCH): Kỹ năng định hướng mũi tên 360° — Lướt nhanh 165px và chém xuyên phá dọc đường lướt.
 */
public class DashStrikeSkill implements ISkillAction {
    @Override
    public int getSkillId() {
        return 1;
    }

    @Override
    public double getMaxRange() {
        return CombatEngine.SKILL1_MAX_RANGE;
    }

    @Override
    public double getAoeRadius() {
        return CombatEngine.SKILL1_CORRIDOR_WIDTH;
    }

    @Override
    public int execute(Player caster, double aimX, double aimY, List<Monster> monsters,
                       IDamageStrategy damageStrategy, CombatEngine engine) {
        double startX = caster.getPosition().getX();
        double startY = caster.getPosition().getY();
        double mag = Math.hypot(aimX, aimY);
        double ux = (mag > 0.01) ? (aimX / mag) : caster.getFacingDirection().getDirX();
        double uy = (mag > 0.01) ? (aimY / mag) : caster.getFacingDirection().getDirY();

        double endX = Math.max(40, Math.min(1560, startX + ux * getMaxRange()));
        double endY = Math.max(40, Math.min(1160, startY + uy * getMaxRange()));
        double midX = (startX + endX) * 0.5;
        double midY = (startY + endY) * 0.5;

        caster.setPosition(new Vector2D(endX, endY));
        spawnVisuals(startX, startY, ux, uy, engine);

        int hitCount = 0;
        for (Monster m : monsters) {
            if (!m.isAlive()) continue;
            double dStart = m.getPosition().distanceTo(new Vector2D(startX, startY));
            double dMid = m.getPosition().distanceTo(new Vector2D(midX, midY));
            double dEnd = m.getPosition().distanceTo(new Vector2D(endX, endY));
            if (dStart <= 80 || dMid <= 80 || dEnd <= 92) {
                int dmg = (int) (damageStrategy.calculateDamage(15, caster, m) * 1.5);
                int actual = m.takeDamage(dmg);
                hitCount++;
                engine.spawnDamageNumber(m.getPosition().getX(), m.getPosition().getY(), actual, CombatEngine.COLOR_CYAN);
                engine.spawnHitParticles(m.getPosition().getX(), m.getPosition().getY(), CombatEngine.COLOR_CYAN, 10);
                engine.notifyMonsterIfDead(caster, m);
            }
        }
        return hitCount;
    }

    @Override
    public void spawnVisuals(double startX, double startY, double aimX, double aimY, CombatEngine engine) {
        double mag = Math.hypot(aimX, aimY);
        Direction dir = Direction.fromVelocity(aimX, aimY, Direction.DOWN);
        double ux = (mag > 0.01) ? (aimX / mag) : dir.getDirX();
        double uy = (mag > 0.01) ? (aimY / mag) : dir.getDirY();

        double endX = Math.max(40, Math.min(1560, startX + ux * getMaxRange()));
        double endY = Math.max(40, Math.min(1160, startY + uy * getMaxRange()));
        double midX = (startX + endX) * 0.5;
        double midY = (startY + endY) * 0.5;

        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(startX, startY, 55, CombatEngine.COLOR_CYAN, 0.30, 1));
        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(midX, midY, 65, CombatEngine.COLOR_CYAN, 0.35, 1));
        engine.getSkillRings().add(new CombatEffects.SkillRingEffect(endX, endY, 85, CombatEngine.COLOR_CYAN, 0.40, 1));
        engine.getSlashEffects().add(new CombatEffects.SlashEffect(endX + ux * 28, endY + uy * 28, dir, 0.28));
        engine.spawnHitParticles(endX, endY, CombatEngine.COLOR_CYAN, 14);
        engine.spawnDamageNumber(endX, endY - 25, "⚡ XUNG KÍCH!", CombatEngine.COLOR_CYAN);
    }
}
