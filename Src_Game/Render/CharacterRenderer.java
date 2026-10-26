package render;

import core.LivingEntity;
import core.Player;

/**
 * Lớp điều khiển hiển thị (CharacterRenderer) độc lập theo kiến trúc tách rời (Decoupled Architecture).
 * Chịu trách nhiệm:
 * 1. Đọc trạng thái từ thực thể logic (Player / LivingEntity) mà không can thiệp vào logic game.
 * 2. Cập nhật thời gian hoạt họa (Animation Timing & Frame Stepping).
 * 3. Ánh xạ trạng thái FSM (State Pattern) sang hành động chuẩn LPC (LpcAnimationType).
 */
public class CharacterRenderer {
    private final CompositeCharacterVisual visual;
    private Direction currentDirection;
    private LpcAnimationType currentAnimation;
    private double animationTimer;
    private int currentFrameIndex;

    public CharacterRenderer(CompositeCharacterVisual visual) {
        this.visual = visual;
        this.currentDirection = Direction.DOWN;
        this.currentAnimation = LpcAnimationType.WALK;
        this.animationTimer = 0.0;
        this.currentFrameIndex = 0;
    }

    public CompositeCharacterVisual getVisual() {
        return visual;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            this.currentDirection = direction;
        }
    }

    public LpcAnimationType getCurrentAnimation() {
        return currentAnimation;
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public void playAnimation(LpcAnimationType animationType) {
        if (this.currentAnimation != animationType) {
            this.currentAnimation = animationType;
            this.currentFrameIndex = 0;
            this.animationTimer = 0.0;
        }
    }

    /**
     * Đồng bộ hóa trạng thái hoạt họa từ thực thể Player theo thời gian trôi (deltaTime).
     */
    public void syncFromPlayer(Player player, double deltaTime) {
        if (player == null) return;

        // 1. Cập nhật hướng nhìn dựa trên vận tốc
        double vx = player.getVelocity().getX();
        double vy = player.getVelocity().getY();
        this.currentDirection = Direction.fromVelocity(vx, vy, this.currentDirection);

        // 2. Ánh xạ State Pattern từ FSM sang LPC Action Block
        String stateName = player.getCurrentState() != null ? player.getCurrentState().getStateName() : "IDLE";
        switch (stateName) {
            case "DEAD":
                playAnimation(LpcAnimationType.HURT);
                break;
            case "ATTACKING":
                playAnimation(LpcAnimationType.SLASH);
                break;
            case "MOVING":
                playAnimation(LpcAnimationType.WALK);
                advanceFrame(deltaTime);
                break;
            case "IDLE":
            default:
                playAnimation(LpcAnimationType.WALK);
                // Ở chuẩn LPC, Frame 0 của hàng WALK chính là tư thế đứng yên (Idle Pose)
                this.currentFrameIndex = 0;
                this.animationTimer = 0.0;
                break;
        }

        if (stateName.equals("ATTACKING") || stateName.equals("DEAD")) {
            advanceFrame(deltaTime);
        }
    }

    private void advanceFrame(double deltaTime) {
        animationTimer += deltaTime;
        double frameDuration = currentAnimation.getFrameDurationSeconds();
        if (animationTimer >= frameDuration) {
            animationTimer -= frameDuration;
            int totalFrames = currentAnimation.getFrameCount();

            if (currentAnimation.isLooping()) {
                currentFrameIndex = (currentFrameIndex + 1) % totalFrames;
            } else {
                if (currentFrameIndex < totalFrames - 1) {
                    currentFrameIndex++;
                }
            }
        }
    }
}
