package core;

/**
 * Lớp cơ sở trừu tượng cho mọi thực thể có sự sống trong trò chơi.
 * Áp dụng nguyên lý Information Expert của Craig Larman:
 * LivingEntity tự nắm giữ chỉ số máu và tự chịu trách nhiệm phán quyết sự sống/chết.
 */
public abstract class LivingEntity {
    protected final String id;
    protected Vector2D position;
    protected Vector2D velocity;
    protected int currentHp;
    protected int maxHp;
    protected int attackPower;
    protected int defense;
    protected double moveSpeed;
    protected boolean isAlive;

    public LivingEntity(String id, Vector2D position, int maxHp, int attackPower, int defense, double moveSpeed) {
        this.id = id;
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.attackPower = attackPower;
        this.defense = defense;
        this.moveSpeed = moveSpeed;
        this.isAlive = true;
    }

    public String getId() {
        return id;
    }

    public Vector2D getPosition() {
        return position;
    }

    public void setPosition(Vector2D position) {
        this.position = position;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public int getDefense() {
        return defense;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public boolean isAlive() {
        return isAlive && currentHp > 0;
    }

    /**
     * Phương thức trừ máu nghiệp vụ theo nguyên lý Information Expert.
     * @param incomingDamage Lượng sát thương từ kẻ tấn công
     * @return Lượng sát thương thực tế đã trừ vào máu
     */
    public int takeDamage(int incomingDamage) {
        if (!isAlive()) {
            return 0;
        }
        int effectiveDamage = Math.max(1, incomingDamage - (defense / 2));
        currentHp = Math.max(0, currentHp - effectiveDamage);
        if (currentHp == 0) {
            isAlive = false;
            onDeath();
        }
        return effectiveDamage;
    }

    public void heal(int amount) {
        if (!isAlive()) {
            return;
        }
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    /**
     * Hook method kích hoạt khi sinh lực về 0.
     */
    protected abstract void onDeath();

    /**
     * Cập nhật vị trí vật lý theo chu kỳ thời gian.
     */
    public void updatePhysics(double deltaTime) {
        if (isAlive && velocity.length() > 0) {
            this.position = this.position.add(this.velocity.multiply(deltaTime));
        }
    }
}
