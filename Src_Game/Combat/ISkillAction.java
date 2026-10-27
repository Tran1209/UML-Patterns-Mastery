package combat;

import core.Monster;
import core.Player;
import java.util.List;

/**
 * Giao diện Chiến lược / Mệnh lệnh Kỹ năng (Skill Command / Strategy Pattern - GoF).
 * Tuân thủ nguyên lý Đóng/Mở (Open/Closed Principle - SOLID):
 * Có thể mở rộng thêm vô số kỹ năng mới mà không cần sửa đổi mã nguồn của CombatEngine.
 */
public interface ISkillAction {
    int getSkillId();
    double getMaxRange();
    double getAoeRadius();

    /**
     * Thực thi tác động vật lý & sát thương của kỹ năng lên chiến trường.
     */
    int execute(Player caster, double aimX, double aimY, List<Monster> monsters,
                IDamageStrategy damageStrategy, CombatEngine engine);

    /**
     * Khởi tạo hiệu ứng thị giác (Sóng xung kích, Hạt tia lửa, Chữ nổi) của kỹ năng.
     */
    void spawnVisuals(double startX, double startY, double aimX, double aimY, CombatEngine engine);
}
