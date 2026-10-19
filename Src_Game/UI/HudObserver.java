package ui;

/**
 * Lớp quan sát viên giao diện người dùng (HUD).
 * Hiển thị thanh máu, thông báo hạ gục mà không phụ thuộc trực tiếp vào logic chiến đấu.
 */
public class HudObserver implements IEntityEventListener {
    private int playerCurrentHp = 100;
    private int playerMaxHp = 100;
    private String lastKillNotification = "";

    @Override
    public void onHealthChanged(String entityId, int currentHp, int maxHp) {
        this.playerCurrentHp = currentHp;
        this.playerMaxHp = maxHp;
    }

    @Override
    public void onEntityDied(String victimId, String killerId) {
        this.lastKillNotification = String.format("Đấu sĩ %s đã hạ gục %s!", killerId, victimId);
    }

    public int getPlayerCurrentHp() {
        return playerCurrentHp;
    }

    public int getPlayerMaxHp() {
        return playerMaxHp;
    }

    public String getLastKillNotification() {
        return lastKillNotification;
    }

    public float getHpPercentage() {
        if (playerMaxHp <= 0) return 0f;
        return (float) playerCurrentHp / playerMaxHp;
    }
}
