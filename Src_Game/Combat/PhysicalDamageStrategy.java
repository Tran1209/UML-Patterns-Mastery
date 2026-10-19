package combat;

import core.LivingEntity;

/**
 * Chiến lược tính sát thương vật lý: Trừ trực tiếp theo điểm giáp của nạn nhân.
 */
public class PhysicalDamageStrategy implements IDamageStrategy {
    @Override
    public int calculateDamage(int basePower, LivingEntity attacker, LivingEntity victim) {
        int armor = victim.getDefense();
        int netDamage = Math.max(1, (attacker.getAttackPower() + basePower) - armor);
        return netDamage;
    }
}
