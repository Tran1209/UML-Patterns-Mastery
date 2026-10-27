package network;

import core.Vector2D;
import java.io.Serializable;
import java.util.List;

/**
 * Bản sao trạng thái thế giới (World Snapshot) do Dedicated Server phát sóng (Broadcast) 20Hz.
 */
public class WorldSnapshotPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class EntityState implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String id;
        public final String name;
        public final double posX;
        public final double posY;
        public final int hp;
        public final int maxHp;
        public final String stateName;
        public final boolean isMonster;

        public EntityState(String id, String name, double posX, double posY, int hp, int maxHp, String stateName, boolean isMonster) {
            this.id = id;
            this.name = name;
            this.posX = posX;
            this.posY = posY;
            this.hp = hp;
            this.maxHp = maxHp;
            this.stateName = stateName;
            this.isMonster = isMonster;
        }
    }

    public final long serverTimestamp;
    public final List<EntityState> entities;
    public final int wave;

    public WorldSnapshotPayload(long serverTimestamp, List<EntityState> entities, int wave) {
        this.serverTimestamp = serverTimestamp;
        this.entities = entities;
        this.wave = wave;
    }
}
