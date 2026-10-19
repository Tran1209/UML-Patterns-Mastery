package core;

/**
 * Thực thể nhân vật người chơi trong thế giới game.
 * Kế thừa LivingEntity và tích hợp State Machine quản lý trạng thái hành vi.
 */
public class Player extends LivingEntity {
    private final String playerName;
    private IPlayerState currentState;
    private double attackCooldownTimer;
    private int killCount;
    private int deathCount;

    public Player(String id, String playerName, Vector2D spawnPosition) {
        super(id, spawnPosition, 100, 20, 10, 200.0);
        this.playerName = playerName;
        this.currentState = new IdleState();
        this.attackCooldownTimer = 0.0;
        this.killCount = 0;
        this.deathCount = 0;
        this.currentState.onEnter(this);
    }

    public String getPlayerName() {
        return playerName;
    }

    public IPlayerState getCurrentState() {
        return currentState;
    }

    public void changeState(IPlayerState newState) {
        if (this.currentState != null) {
            this.currentState.onExit(this);
        }
        this.currentState = newState;
        this.currentState.onEnter(this);
    }

    public boolean canAttack() {
        return isAlive() && attackCooldownTimer <= 0 && currentState.canAttack();
    }

    public void triggerAttackCooldown(double durationSeconds) {
        this.attackCooldownTimer = durationSeconds;
    }

    public void addKill() {
        this.killCount++;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getDeathCount() {
        return deathCount;
    }

    @Override
    public void updatePhysics(double deltaTime) {
        super.updatePhysics(deltaTime);
        if (attackCooldownTimer > 0) {
            attackCooldownTimer = Math.max(0, attackCooldownTimer - deltaTime);
        }
        if (currentState != null) {
            currentState.onUpdate(this, deltaTime);
        }
    }

    @Override
    protected void onDeath() {
        this.deathCount++;
        changeState(new DeadState());
    }

    // --- Các lớp trạng thái cụ thể (State Pattern implementations) ---
    public static class IdleState implements IPlayerState {
        @Override
        public String getStateName() { return "IDLE"; }
        @Override
        public void onEnter(Player player) { player.setVelocity(new Vector2D(0, 0)); }
        @Override
        public void onUpdate(Player player, double deltaTime) {}
        @Override
        public void onExit(Player player) {}
        @Override
        public boolean canAttack() { return true; }
        @Override
        public boolean canMove() { return true; }
    }

    public static class MovingState implements IPlayerState {
        @Override
        public String getStateName() { return "MOVING"; }
        @Override
        public void onEnter(Player player) {}
        @Override
        public void onUpdate(Player player, double deltaTime) {
            if (player.getVelocity().length() < 1e-3) {
                player.changeState(new IdleState());
            }
        }
        @Override
        public void onExit(Player player) {}
        @Override
        public boolean canAttack() { return true; }
        @Override
        public boolean canMove() { return true; }
    }

    public static class AttackingState implements IPlayerState {
        private double timer = 0.3; // 300ms vung đòn
        @Override
        public String getStateName() { return "ATTACKING"; }
        @Override
        public void onEnter(Player player) { this.timer = 0.3; }
        @Override
        public void onUpdate(Player player, double deltaTime) {
            timer -= deltaTime;
            if (timer <= 0) {
                player.changeState(new IdleState());
            }
        }
        @Override
        public void onExit(Player player) {}
        @Override
        public boolean canAttack() { return false; }
        @Override
        public boolean canMove() { return false; } // Khóa di chuyển khi vung kiếm
    }

    public static class DeadState implements IPlayerState {
        @Override
        public String getStateName() { return "DEAD"; }
        @Override
        public void onEnter(Player player) {
            player.setVelocity(new Vector2D(0, 0));
        }
        @Override
        public void onUpdate(Player player, double deltaTime) {}
        @Override
        public void onExit(Player player) {}
        @Override
        public boolean canAttack() { return false; }
        @Override
        public boolean canMove() { return false; }
    }
}
