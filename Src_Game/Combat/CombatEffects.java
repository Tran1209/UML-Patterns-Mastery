package combat;

import render.Direction;

/**
 * Các lớp thực thể hiệu ứng thị giác chiến đấu (Visual Effects DTOs).
 * Tách biệt hoàn toàn khỏi CombatEngine theo nguyên lý Đơn Trách Nhiệm (Single Responsibility Principle - SRP).
 */
public final class CombatEffects {
    private CombatEffects() {}

    public static class FloatingText {
        public double x, y;
        public final String text;
        public final int color;
        public double lifeTime;
        public final double maxLifeTime;

        public FloatingText(double x, double y, String text, int color, double maxLifeTime) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
        }

        public void update(double dt) {
            y -= 35.0 * dt;
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    public static class HitParticle {
        public double x, y;
        public final double vx, vy;
        public final int color;
        public double lifeTime;
        public final double maxLifeTime;
        public final double size;

        public HitParticle(double x, double y, double vx, double vy, int color, double maxLifeTime, double size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
            this.size = size;
        }

        public void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    public static class SlashEffect {
        public final double x, y;
        public final Direction direction;
        public double lifeTime;
        public final double maxLifeTime;

        public SlashEffect(double x, double y, Direction direction, double maxLifeTime) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
        }

        public void update(double dt) {
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    public static class SkillRingEffect {
        public final double x, y;
        public final double maxRadius;
        public final int color;
        public double lifeTime;
        public final double maxLifeTime;
        public final int skillType;

        public SkillRingEffect(double x, double y, double maxRadius, int color, double maxLifeTime, int skillType) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.color = color;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
            this.skillType = skillType;
        }

        public void update(double dt) {
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }

    /**
     * Hiệu ứng tia sét ma thuật phóng ra từ gậy phép (Arcane Lightning Bolt Effect).
     * Bao gồm chuỗi điểm zíc-zắc ngẫu nhiên và nhánh sét phụ giật sang bên.
     */
    public static class LightningBoltEffect {
        public final double startX, startY;
        public final double targetX, targetY;
        public final Direction direction;
        public double lifeTime;
        public final double maxLifeTime;
        public final double[] segmentsOffset;
        public final double branchAngle;

        public LightningBoltEffect(double startX, double startY, double targetX, double targetY, Direction direction, double maxLifeTime) {
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.direction = direction;
            this.lifeTime = maxLifeTime;
            this.maxLifeTime = maxLifeTime;
            this.segmentsOffset = new double[5];
            java.util.Random rnd = new java.util.Random();
            for (int i = 0; i < segmentsOffset.length; i++) {
                this.segmentsOffset[i] = (rnd.nextDouble() - 0.5) * 26.0;
            }
            this.branchAngle = (rnd.nextDouble() - 0.5) * 0.75;
        }

        public void update(double dt) {
            lifeTime -= dt;
        }

        public boolean isExpired() {
            return lifeTime <= 0;
        }
    }
}
