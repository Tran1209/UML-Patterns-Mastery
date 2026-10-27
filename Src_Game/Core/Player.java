package core;

import combat.Hitbox;
import render.Direction;

/**
 * Thực thể nhân vật người chơi trong thế giới game.
 * Kế thừa LivingEntity và tích hợp State Machine quản lý trạng thái hành vi.
 */
public class Player extends LivingEntity {
    public static final double SKILL1_CD = 4.0;  // Chiêu 1: Xung Kích (Lướt)
    public static final double SKILL2_CD = 6.0;  // Chiêu 2: Vòng Xoáy Lửa (AoE + Hồi máu)
    public static final double SKILL3_CD = 10.0; // Chiêu 3: Thiên Phạt (Ultimate)

    private final String playerName;
    private IPlayerState currentState;
    private Direction facingDirection;
    private double attackCooldownTimer;
    private double skill1Timer;
    private double skill2Timer;
    private double skill3Timer;
    private int killCount;
    private int deathCount;
    private final Hitbox hitbox;

    public Player(String id, String playerName, Vector2D spawnPosition) {
        super(id, spawnPosition, 100, 25, 10, 220.0);
        this.playerName = playerName;
        this.currentState = new IdleState();
        this.facingDirection = Direction.DOWN;
        this.attackCooldownTimer = 0.0;
        this.skill1Timer = 0.0;
        this.skill2Timer = 0.0;
        this.skill3Timer = 0.0;
        this.killCount = 0;
        this.deathCount = 0;
        this.hitbox = new Hitbox(spawnPosition.getX(), spawnPosition.getY(), 40, 40);
        this.currentState.onEnter(this);
    }

    public Direction getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(Direction facingDirection) {
        if (facingDirection != null) {
            this.facingDirection = facingDirection;
        }
    }

    public Hitbox getHitbox() {
        hitbox.updateCenter(position);
        return hitbox;
    }

    /**
     * Tạo một Hitbox hình quạt/khu vực chém vũ khí trước mặt nhân vật theo hướng nhìn.
     */
    public Hitbox getAttackArea() {
        double reach = 50.0;
        double attackX = position.getX() + facingDirection.getDirX() * reach;
        double attackY = position.getY() + facingDirection.getDirY() * reach;
        return new Hitbox(attackX, attackY, 65, 65);
    }

    public void respawn(Vector2D spawnPosition) {
        this.position = spawnPosition;
        this.velocity = new Vector2D(0, 0);
        this.currentHp = this.maxHp;
        this.isAlive = true;
        this.attackCooldownTimer = 0.0;
        this.skill1Timer = 0.0;
        this.skill2Timer = 0.0;
        this.skill3Timer = 0.0;
        changeState(new IdleState());
    }

    public void heal(int amount) {
        if (isAlive && amount > 0) {
            this.currentHp = Math.min(this.maxHp, this.currentHp + amount);
        }
    }

    public boolean canCastSkill(int skillId) {
        if (!isAlive()) return false;
        switch (skillId) {
            case 1: return skill1Timer <= 0;
            case 2: return skill2Timer <= 0;
            case 3: return skill3Timer <= 0;
            default: return false;
        }
    }

    public void triggerSkillCooldown(int skillId) {
        switch (skillId) {
            case 1: skill1Timer = SKILL1_CD; break;
            case 2: skill2Timer = SKILL2_CD; break;
            case 3: skill3Timer = SKILL3_CD; break;
        }
    }

    public double getSkillCooldown(int skillId) {
        switch (skillId) {
            case 1: return skill1Timer;
            case 2: return skill2Timer;
            case 3: return skill3Timer;
            default: return 0.0;
        }
    }

    public double getSkillMaxCooldown(int skillId) {
        switch (skillId) {
            case 1: return SKILL1_CD;
            case 2: return SKILL2_CD;
            case 3: return SKILL3_CD;
            default: return 1.0;
        }
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
        if (velocity.length() > 1e-3) {
            this.facingDirection = Direction.fromVelocity(velocity.getX(), velocity.getY(), this.facingDirection);
        }
        if (attackCooldownTimer > 0) {
            attackCooldownTimer = Math.max(0, attackCooldownTimer - deltaTime);
        }
        if (skill1Timer > 0) {
            skill1Timer = Math.max(0, skill1Timer - deltaTime);
        }
        if (skill2Timer > 0) {
            skill2Timer = Math.max(0, skill2Timer - deltaTime);
        }
        if (skill3Timer > 0) {
            skill3Timer = Math.max(0, skill3Timer - deltaTime);
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
