package core;

/**
 * Giao diện trạng thái nhân vật theo mẫu thiết kế State Pattern (GoF).
 */
public interface IPlayerState {
    String getStateName();
    void onEnter(Player player);
    void onUpdate(Player player, double deltaTime);
    void onExit(Player player);
    boolean canAttack();
    boolean canMove();
}
