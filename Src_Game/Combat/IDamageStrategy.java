package combat;

import core.LivingEntity;

/**
 * Giao diện chiến lược tính sát thương theo mẫu Strategy Pattern (GoF).
 */
public interface IDamageStrategy {
    int calculateDamage(int basePower, LivingEntity attacker, LivingEntity victim);
}
