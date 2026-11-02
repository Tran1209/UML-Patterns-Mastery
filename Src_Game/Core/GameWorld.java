package core;

import combat.CombatEngine;
import render.CompositeCharacterVisual;
import render.LpcCharacterBuilder;
import render.CharacterRenderer;

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
    private final WaveManager waveManager;

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
        this.waveManager = new WaveManager();

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

    public void addEventListener(IEntityEventListener listener) {
        if (listener != null) {
            combatEngine.addEventListener(listener);
        }
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public int getCurrentWave() {
        return waveManager.getCurrentWave();
    }

    public int getScore() {
        return waveManager.getScore();
    }

    public double getMatchTimeSeconds() {
        return matchTimeSeconds;
    }

    public void startWave(int waveNumber) {
        waveManager.startWave(waveNumber, player.getPosition(), WORLD_WIDTH, WORLD_HEIGHT, monsters);
    }

    /**
     * Cập nhật toàn bộ thế giới trò chơi trong chu kỳ Fixed Timestep.
     */
    public void update(double dt) {
        matchTimeSeconds += dt;

        // 1. Cập nhật Player (Điều hướng di chuyển tự động & Vật lý)
        player.updateNavigation(dt);
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

        // 6. Cập nhật tiến độ đợt quái & điểm số (Ủy quyền cho WaveManager)
        waveManager.update(dt, monsters, player.getPosition(), WORLD_WIDTH, WORLD_HEIGHT);
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
        matchTimeSeconds = 0.0;
        waveManager.reset();
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
