package core;

import combat.CombatEngine;
import render.CompositeCharacterVisual;
import render.LpcCharacterBuilder;
import render.CharacterRenderer;
import ui.HudObserver;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thế giới trò chơi (Game World) quản lý toàn bộ thực thể, quy tắc chiến đấu và vòng lặp đấu trường.
 * Áp dụng nguyên lý High Cohesion: Đóng gói toàn bộ logic quản lý trận đấu.
 */
public class GameWorld {
    public static final double WORLD_WIDTH = 1600.0;
    public static final double WORLD_HEIGHT = 1200.0;

    private final Player player;
    private final CharacterRenderer playerRenderer;
    private final List<Monster> monsters = new CopyOnWriteArrayList<>();
    private final List<Player> remotePlayers = new CopyOnWriteArrayList<>();
    private final CombatEngine combatEngine;
    private final HudObserver hudObserver;

    private int currentWave = 1;
    private int score = 0;
    private double waveSpawnTimer = 0.0;
    private boolean waveInProgress = false;
    private double matchTimeSeconds = 0.0;

    public GameWorld(String playerName) {
        this.player = new Player("player_local", playerName, new Vector2D(WORLD_WIDTH / 2.0, WORLD_HEIGHT / 2.0));

        CompositeCharacterVisual visual = new LpcCharacterBuilder()
                .withBody("assets/sprites/body.png")
                .withShirt("assets/sprites/shirt.png")
                .withArmor("assets/sprites/armor.png")
                .withWeapon("assets/sprites/weapon.png")
                .build();
        this.playerRenderer = new CharacterRenderer(visual);

        this.combatEngine = new CombatEngine(128); // Ô lưới 128x128 cho Spatial Grid
        this.hudObserver = new HudObserver();
        this.combatEngine.addEventListener(hudObserver);

        startWave(1);
    }

    public Player getPlayer() {
        return player;
    }

    public CharacterRenderer getPlayerRenderer() {
        return playerRenderer;
    }

    public List<Monster> getMonsters() {
        return monsters;
    }

    public List<Player> getRemotePlayers() {
        return remotePlayers;
    }

    public CombatEngine getCombatEngine() {
        return combatEngine;
    }

    public HudObserver getHudObserver() {
        return hudObserver;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getScore() {
        return score;
    }

    public double getMatchTimeSeconds() {
        return matchTimeSeconds;
    }

    public void startWave(int waveNumber) {
        this.currentWave = waveNumber;
        this.waveInProgress = true;
        this.monsters.clear();

        int goblinCount = 3 + waveNumber * 2;
        int slimeCount = 2 + waveNumber;
        int orcCount = Math.max(0, waveNumber - 1);

        Random rnd = new Random();
        for (int i = 0; i < goblinCount; i++) {
            Vector2D spawn = getRandSpawnAround(player.getPosition(), 350, 600, rnd);
            monsters.add(new Monster("goblin_" + waveNumber + "_" + i, Monster.MonsterType.GOBLIN, spawn));
        }
        for (int i = 0; i < slimeCount; i++) {
            Vector2D spawn = getRandSpawnAround(player.getPosition(), 300, 550, rnd);
            monsters.add(new Monster("slime_" + waveNumber + "_" + i, Monster.MonsterType.SLIME, spawn));
        }
        for (int i = 0; i < orcCount; i++) {
            Vector2D spawn = getRandSpawnAround(player.getPosition(), 400, 700, rnd);
            monsters.add(new Monster("orc_" + waveNumber + "_" + i, Monster.MonsterType.ORC_WARRIOR, spawn));
        }
    }

    private Vector2D getRandSpawnAround(Vector2D center, double minR, double maxR, Random rnd) {
        double angle = rnd.nextDouble() * Math.PI * 2;
        double r = minR + rnd.nextDouble() * (maxR - minR);
        double sx = Math.max(64, Math.min(WORLD_WIDTH - 64, center.getX() + Math.cos(angle) * r));
        double sy = Math.max(64, Math.min(WORLD_HEIGHT - 64, center.getY() + Math.sin(angle) * r));
        return new Vector2D(sx, sy);
    }

    /**
     * Cập nhật toàn bộ thế giới trò chơi trong chu kỳ Fixed Timestep.
     */
    public void update(double dt) {
        matchTimeSeconds += dt;

        // 1. Cập nhật Player
        player.updatePhysics(dt);
        clampWithinBounds(player);
        playerRenderer.syncFromPlayer(player, dt);

        // 2. Cập nhật Monsters AI & Vật lý
        for (Monster m : monsters) {
            m.updateAI(player, dt);
            m.updatePhysics(dt);
            clampWithinBounds(m);
        }

        // 3. Tái xây dựng phân vùng không gian Spatial Grid
        List<LivingEntity> allEntities = new ArrayList<>();
        allEntities.add(player);
        allEntities.addAll(monsters);
        allEntities.addAll(remotePlayers);
        combatEngine.rebuildSpatialGrid(allEntities);

        // 4. Xử lý quái vật cắn/tấn công người chơi
        combatEngine.executeMonsterAttacks(monsters, player);

        // 5. Cập nhật các hiệu ứng hạt, số sát thương
        combatEngine.updateEffects(dt);

        // 6. Dọn dẹp quái vật đã tử vong và cộng điểm (Thread-Safe với CopyOnWriteArrayList)
        monsters.removeIf(m -> {
            if (!m.isAlive()) {
                score += m.getType().hp * 2;
                return true;
            }
            return false;
        });

        // 7. Kiểm tra hoàn thành đợt quái (Wave Clear)
        if (monsters.isEmpty() && waveInProgress) {
            waveInProgress = false;
            waveSpawnTimer = 3.0; // 3 giây chuẩn bị đợt mới
        }

        if (!waveInProgress) {
            waveSpawnTimer -= dt;
            if (waveSpawnTimer <= 0) {
                startWave(currentWave + 1);
            }
        }
    }

    public void handlePlayerAttack() {
        if (player.isAlive()) {
            combatEngine.executePlayerAttack(player, monsters);
        }
    }

    public boolean handlePlayerSkill(int skillId) {
        return handlePlayerSkill(skillId, 0.0, 0.0);
    }

    public boolean handlePlayerSkill(int skillId, double aimX, double aimY) {
        if (player.isAlive() && player.canCastSkill(skillId)) {
            combatEngine.executePlayerSkill(player, skillId, aimX, aimY, monsters);
            return true;
        }
        return false;
    }

    public void restartGame() {
        player.respawn(new Vector2D(WORLD_WIDTH / 2.0, WORLD_HEIGHT / 2.0));
        score = 0;
        matchTimeSeconds = 0.0;
        startWave(1);
    }

    private void clampWithinBounds(LivingEntity entity) {
        Vector2D pos = entity.getPosition();
        double clampedX = Math.max(40, Math.min(WORLD_WIDTH - 40, pos.getX()));
        double clampedY = Math.max(40, Math.min(WORLD_HEIGHT - 40, pos.getY()));
        if (clampedX != pos.getX() || clampedY != pos.getY()) {
            entity.setPosition(new Vector2D(clampedX, clampedY));
        }
    }
}
