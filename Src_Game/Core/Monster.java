package core;

import combat.Hitbox;
import render.Direction;

/**
 * Thực thể quái vật (Monster / AI Entity) trong đấu trường 2D.
 * Kế thừa LivingEntity, ứng dụng nguyên lý Kế thừa và Đa hình.
 * Tích hợp AI hành vi cơ bản (State-based / Proximity Aggro AI).
 */
public class Monster extends LivingEntity {
    public enum MonsterType {
        GOBLIN(60, 15, 4, 120.0, 350.0, "Goblin"),
        ORC_WARRIOR(120, 25, 12, 80.0, 300.0, "Orc Chiến Binh"),
        SLIME(40, 10, 2, 140.0, 400.0, "Slime Độc");

        public final int hp;
        public final int attack;
        public final int defense;
        public final double speed;
        public final double aggroRadius;
        public final String displayName;

        MonsterType(int hp, int attack, int defense, double speed, double aggroRadius, String displayName) {
            this.hp = hp;
            this.attack = attack;
            this.defense = defense;
            this.speed = speed;
            this.aggroRadius = aggroRadius;
            this.displayName = displayName;
        }
    }

    private final MonsterType type;
    private Direction facingDirection;
    private double attackCooldownTimer;
    private double wanderTimer;
    private Vector2D wanderVelocity;
    private final Hitbox hitbox;

    public Monster(String id, MonsterType type, Vector2D spawnPosition) {
        super(id, spawnPosition, type.hp, type.attack, type.defense, type.speed);
        this.type = type;
        this.facingDirection = Direction.DOWN;
        this.attackCooldownTimer = 0.0;
        this.wanderTimer = 0.0;
        this.wanderVelocity = new Vector2D(0, 0);
        this.hitbox = new Hitbox(spawnPosition.getX(), spawnPosition.getY(), 44, 44);
    }

    public MonsterType getType() {
        return type;
    }

    public Direction getFacingDirection() {
        return facingDirection;
    }

    public Hitbox getHitbox() {
        hitbox.updateCenter(position);
        return hitbox;
    }

    public boolean canAttack() {
        return isAlive() && attackCooldownTimer <= 0;
    }

    public void triggerAttackCooldown(double cooldownSeconds) {
        this.attackCooldownTimer = cooldownSeconds;
    }

    /**
     * Cập nhật AI hành vi của quái vật đối với mục tiêu người chơi.
     */
    public void updateAI(Player targetPlayer, double deltaTime) {
        if (!isAlive()) {
            setVelocity(new Vector2D(0, 0));
            return;
        }

        if (attackCooldownTimer > 0) {
            attackCooldownTimer = Math.max(0, attackCooldownTimer - deltaTime);
        }

        if (targetPlayer == null || !targetPlayer.isAlive()) {
            wander(deltaTime);
            return;
        }

        Vector2D targetPos = targetPlayer.getPosition();
        Vector2D diff = targetPos.subtract(this.position);
        double distance = diff.length();

        if (distance <= type.aggroRadius) {
            // Trong phạm vi phát hiện -> Truy đuổi người chơi
            if (distance > 36.0) {
                Vector2D moveDir = diff.normalize();
                this.velocity = moveDir.multiply(this.moveSpeed);
                this.facingDirection = Direction.fromVelocity(velocity.getX(), velocity.getY(), this.facingDirection);
            } else {
                // Đã áp sát tầm đánh cận chiến
                this.velocity = new Vector2D(0, 0);
            }
        } else {
            // Ngoài phạm vi -> Đi tuần lang thang (Wander)
            wander(deltaTime);
        }
    }

    private void wander(double deltaTime) {
        wanderTimer -= deltaTime;
        if (wanderTimer <= 0) {
            wanderTimer = 1.5 + Math.random() * 2.0; // 1.5 - 3.5 giây đổi hướng một lần
            if (Math.random() < 0.4) {
                wanderVelocity = new Vector2D(0, 0); // Đứng yên
            } else {
                double angle = Math.random() * Math.PI * 2;
                wanderVelocity = new Vector2D(Math.cos(angle), Math.sin(angle)).multiply(moveSpeed * 0.5);
                this.facingDirection = Direction.fromVelocity(wanderVelocity.getX(), wanderVelocity.getY(), this.facingDirection);
            }
        }
        this.velocity = wanderVelocity;
    }

    @Override
    protected void onDeath() {
        this.velocity = new Vector2D(0, 0);
    }
}
