package ui;

/**
 * Giao diện quan sát viên nhận thông báo sự kiện từ Domain sang UI.
 * Mẫu thiết kế Observer Pattern (GoF) đảm bảo Model-View Separation của Craig Larman.
 */
public interface IEntityEventListener {
    void onHealthChanged(String entityId, int currentHp, int maxHp);
    void onEntityDied(String victimId, String killerId);
}
