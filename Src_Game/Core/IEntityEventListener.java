package core;

/**
 * Cổng giao tiếp sự kiện thực thể (Domain Event Listener Port).
 * Áp dụng nguyên lý Hexagonal Architecture (Ports & Adapters) & DIP:
 * Interface này thuộc tầng Domain (Core). Tầng Presentation (UI) cài đặt interface này
 * để nhận thông báo mà Domain không cần biết chi tiết về UI.
 */
public interface IEntityEventListener {
    /**
     * Thông báo lượng máu của thực thể thay đổi.
     * @param entityId Định danh thực thể
     * @param currentHp Máu hiện tại
     * @param maxHp Máu tối đa
     */
    void onHealthChanged(String entityId, int currentHp, int maxHp);

    /**
     * Thông báo thực thể đã tử vong.
     * @param victimId Tên/ID nạn nhân
     * @param killerId Tên/ID kẻ hạ gục
     */
    void onEntityDied(String victimId, String killerId);
}
