package core;

import java.util.List;
import java.util.Random;

/**
 * Trình quản lý các đợt quái vật và điểm số trận đấu (Wave & Progression Manager).
 * Áp dụng nguyên lý SOLID - Single Responsibility Principle (SRP) và GRASP Creator:
 * Đóng gói toàn bộ logic tính toán số lượng quái theo từng đợt, thuật toán phân bổ tọa độ sinh quái
 * vòng cung quanh người chơi, và bộ đếm thời gian chuyển đợt (Wave Transition Timer).
 */
public class WaveManager {
    private int currentWave = 1;
    private int score = 0;
    private double waveSpawnTimer = 0.0;
    private boolean waveInProgress = false;
    private final Random random = new Random();

    public WaveManager() {}

    public int getCurrentWave() {
        return currentWave;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int delta) {
        if (delta > 0) {
            this.score += delta;
        }
    }

    public boolean isWaveInProgress() {
        return waveInProgress;
    }

    public double getWaveSpawnTimer() {
        return waveSpawnTimer;
    }

    public void reset() {
        this.currentWave = 1;
        this.score = 0;
        this.waveSpawnTimer = 0.0;
        this.waveInProgress = false;
    }

    /**
     * Khởi động một đợt quái mới, tạo sinh quái vật theo công thức cấp độ và thêm vào danh sách quái.
     */
    public void startWave(int waveNumber, Vector2D playerPos, double worldWidth, double worldHeight, List<Monster> monsters) {
        this.currentWave = waveNumber;
        this.waveInProgress = true;
        monsters.clear();

        int goblinCount = 3 + waveNumber * 2;
        int slimeCount = 2 + waveNumber;
        int orcCount = Math.max(0, waveNumber - 1);

        for (int i = 0; i < goblinCount; i++) {
            Vector2D spawn = getRandSpawnAround(playerPos, 350, 600, worldWidth, worldHeight);
            monsters.add(new Monster("goblin_" + waveNumber + "_" + i, Monster.MonsterType.GOBLIN, spawn));
        }
        for (int i = 0; i < slimeCount; i++) {
            Vector2D spawn = getRandSpawnAround(playerPos, 300, 550, worldWidth, worldHeight);
            monsters.add(new Monster("slime_" + waveNumber + "_" + i, Monster.MonsterType.SLIME, spawn));
        }
        for (int i = 0; i < orcCount; i++) {
            Vector2D spawn = getRandSpawnAround(playerPos, 400, 700, worldWidth, worldHeight);
            monsters.add(new Monster("orc_" + waveNumber + "_" + i, Monster.MonsterType.ORC_WARRIOR, spawn));
        }
    }

    /**
     * Cập nhật tiến độ đợt quái theo thời gian thực (Fixed Timestep).
     */
    public void update(double dt, List<Monster> monsters, Vector2D playerPos, double worldWidth, double worldHeight) {
        // Dọn dẹp quái vật đã tử vong và cộng điểm
        monsters.removeIf(m -> {
            if (!m.isAlive()) {
                addScore(m.getType().hp * 2);
                return true;
            }
            return false;
        });

        // Kiểm tra hoàn thành đợt quái (Wave Clear)
        if (monsters.isEmpty() && waveInProgress) {
            waveInProgress = false;
            waveSpawnTimer = 3.0; // 3 giây chuẩn bị đợt mới
        }

        if (!waveInProgress) {
            waveSpawnTimer -= dt;
            if (waveSpawnTimer <= 0) {
                startWave(currentWave + 1, playerPos, worldWidth, worldHeight, monsters);
            }
        }
    }

    private Vector2D getRandSpawnAround(Vector2D center, double minR, double maxR, double worldWidth, double worldHeight) {
        double angle = random.nextDouble() * Math.PI * 2;
        double r = minR + random.nextDouble() * (maxR - minR);
        double sx = Math.max(64, Math.min(worldWidth - 64, center.getX() + Math.cos(angle) * r));
        double sy = Math.max(64, Math.min(worldHeight - 64, center.getY() + Math.sin(angle) * r));
        return new Vector2D(sx, sy);
    }
}
