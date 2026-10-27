package server;

import combat.CombatEngine;
import core.GameWorld;
import core.LivingEntity;
import core.Monster;
import core.Player;
import core.Vector2D;
import network.*;
import render.Direction;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Máy chủ chuyên dụng độc lập Đa người chơi (Dedicated Authoritative Server 20Hz).
 * Hoạt động hoàn toàn ở chế độ không đầu (Headless Mode - KHÔNG chứa thư viện đồ họa GUI).
 * Tham chiếu kiến trúc: Joshua Glazer & Sanjay Madhav, Multiplayer Game Programming.
 * 
 * Chức năng:
 * 1. Chấp nhận nhiều Client kết nối đồng thời qua Socket TCP.
 * 2. Nắm giữ quyền uy tối cao (Authoritative Simulation): Nhận input thô (vx, vy, attack) và tính toán vật lý/va chạm tập trung.
 * 3. Chống hack/gian lận: Client không được tự quyết định vị trí hay lượng máu.
 * 4. Phát sóng trạng thái thế giới (World Snapshot) đều đặn ở tần số 20 Hz (50ms).
 */
public class DedicatedGameServer {
    private final int port;
    private final Map<String, Player> connectedPlayers = new ConcurrentHashMap<>();
    private final List<Monster> monsters = new CopyOnWriteArrayList<>();
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final CombatEngine combatEngine = new CombatEngine(128);

    private volatile boolean isRunning = false;
    private ServerSocket serverSocket;
    private ScheduledExecutorService tickExecutor;

    private int currentWave = 1;
    private double waveSpawnTimer = 0.0;
    private boolean waveInProgress = false;
    private int nextPlayerNumericId = 1;

    public DedicatedGameServer(int port) {
        this.port = port;
        startWave(1);
    }

    public void start() {
        isRunning = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("==================================================");
            System.out.println("🌐 DEDICATED AUTHORITATIVE MULTIPLAYER SERVER 20Hz");
            System.out.println("📡 Cổng lắng nghe (Port): " + port);
            System.out.println("⏱️ Tần số đồng bộ (Tick Rate): 20 Hz (50ms / snapshot)");
            System.out.println("🖥️ Môi trường: Headless Pure Java (Authoritative Netcode)");
            System.out.println("==================================================");

            // Bắt đầu vòng lặp vật lý và phát sóng 30Hz (mỗi 33ms) siêu mượt
            tickExecutor = Executors.newSingleThreadScheduledExecutor();
            tickExecutor.scheduleAtFixedRate(this::tickServer20Hz, 33, 33, TimeUnit.MILLISECONDS);

            // Luồng chấp nhận kết nối người chơi mới
            new Thread(this::acceptClientsLoop, "Server-AcceptLoop").start();

        } catch (IOException e) {
            System.err.println("❌ Lỗi khởi động Server Socket: " + e.getMessage());
        }
    }

    private void acceptClientsLoop() {
        while (isRunning && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true); // Tắt thuật toán Nagle để gửi gói tin tức thì qua WiFi (0ms delay)
                try {
                    clientSocket.setTrafficClass(0x10); // IPTOS_LOWDELAY
                } catch (Exception ignored) {}
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler, "ClientHandler-" + clientSocket.getRemoteSocketAddress()).start();
                System.out.println("✅ Người chơi mới kết nối từ: " + clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                if (!isRunning) break;
            }
        }
    }

    public synchronized String registerNewPlayer(String requestedName) {
        String playerId = "p_" + (nextPlayerNumericId++);
        String displayName = (requestedName != null && !requestedName.trim().isEmpty()) ? requestedName : ("Hero_" + playerId);

        // Sinh vị trí ngẫu nhiên quanh trung tâm đấu trường
        double offsetX = (Math.random() - 0.5) * 200;
        double offsetY = (Math.random() - 0.5) * 200;
        Vector2D spawnPos = new Vector2D(GameWorld.WORLD_WIDTH / 2.0 + offsetX, GameWorld.WORLD_HEIGHT / 2.0 + offsetY);

        Player newPlayer = new Player(playerId, displayName, spawnPos);
        connectedPlayers.put(playerId, newPlayer);
        System.out.printf("⚔️ Đã tạo thực thể nhân vật: [ID: %s | Tên: %s | Tọa độ: (%.0f, %.0f)]%n",
                playerId, displayName, spawnPos.getX(), spawnPos.getY());
        return playerId;
    }

    public void handleClientInput(String playerId, PlayerInputPayload input) {
        Player player = connectedPlayers.get(playerId);
        if (player == null || !player.isAlive()) return;

        double vx = input.moveVx;
        double vy = input.moveVy;
        player.setVelocity(new Vector2D(vx, vy));

        if (vx != 0 || vy != 0) {
            Direction dir = Direction.fromVelocity(vx, vy, player.getFacingDirection());
            player.setFacingDirection(dir);
            if (player.getCurrentState() instanceof Player.IdleState) {
                player.changeState(new Player.MovingState());
            }
        }

        if (input.skillId >= 1 && input.skillId <= 3 && player.canCastSkill(input.skillId)) {
            combatEngine.executePlayerSkill(player, input.skillId, input.skillAimX, input.skillAimY, new ArrayList<>(monsters));
        } else if (input.isAttacking && player.canAttack()) {
            combatEngine.executePlayerAttack(player, new ArrayList<>(monsters));
        }
    }

    private void tickServer20Hz() {
        try {
            double dt = 0.033; // 33ms cho chu kỳ 30Hz mượt mà

            // 1. Cập nhật vật lý cho tất cả người chơi
            for (Player player : connectedPlayers.values()) {
                player.updatePhysics(dt);
                clampWithinBounds(player);
            }

            // 2. Cập nhật AI và vật lý quái vật (Nhắm vào người chơi gần nhất)
            for (Monster monster : monsters) {
                Player nearest = findNearestPlayer(monster.getPosition());
                monster.updateAI(nearest, dt);
                monster.updatePhysics(dt);
                clampWithinBounds(monster);
            }

            // 3. Tái xây dựng phân vùng không gian Spatial Grid
            List<LivingEntity> allEntities = new ArrayList<>(connectedPlayers.values());
            allEntities.addAll(monsters);
            combatEngine.rebuildSpatialGrid(allEntities);

            // 4. Xử lý quái vật tấn công người chơi
            for (Player player : connectedPlayers.values()) {
                combatEngine.executeMonsterAttacks(new ArrayList<>(monsters), player);
            }

            // 5. Cập nhật hiệu ứng chiến đấu
            combatEngine.updateEffects(dt);

            // 6. Dọn dẹp quái vật đã tử trận
            monsters.removeIf(m -> !m.isAlive());

            // 7. Kiểm tra hoàn thành đợt quái (Wave Clear)
            if (monsters.isEmpty() && waveInProgress) {
                waveInProgress = false;
                waveSpawnTimer = 3.0; // 3 giây chờ đợt mới
            }
            if (!waveInProgress) {
                waveSpawnTimer -= dt;
                if (waveSpawnTimer <= 0) {
                    startWave(currentWave + 1);
                }
            }

            // 8. Đóng gói và phát sóng WorldSnapshot (20Hz)
            List<WorldSnapshotPayload.EntityState> states = new ArrayList<>();
            for (Player p : connectedPlayers.values()) {
                states.add(new WorldSnapshotPayload.EntityState(
                        p.getId(), p.getPlayerName(),
                        p.getPosition().getX(), p.getPosition().getY(),
                        p.getCurrentHp(), p.getMaxHp(),
                        p.getCurrentState().getStateName(),
                        p.getFacingDirection().name(),
                        false
                ));
            }

            for (Monster m : monsters) {
                states.add(new WorldSnapshotPayload.EntityState(
                        m.getId(), m.getType().displayName,
                        m.getPosition().getX(), m.getPosition().getY(),
                        m.getCurrentHp(), m.getMaxHp(),
                        m.isAlive() ? "ALIVE" : "DEAD",
                        m.getFacingDirection().name(),
                        true
                ));
            }

            WorldSnapshotPayload snapshot = new WorldSnapshotPayload(System.currentTimeMillis(), states, currentWave);
            GamePacket packet = new GamePacket(PacketType.WORLD_SNAPSHOT, "SERVER", snapshot);
            broadcast(packet);

        } catch (Exception e) {
            System.err.println("⚠️ Lỗi vòng lặp Server 20Hz: " + e.getMessage());
        }
    }

    private Player findNearestPlayer(Vector2D monsterPos) {
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Player p : connectedPlayers.values()) {
            if (p.isAlive()) {
                double dist = p.getPosition().distanceTo(monsterPos);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    public void startWave(int waveNumber) {
        this.currentWave = waveNumber;
        this.waveInProgress = true;
        this.monsters.clear();

        int goblinCount = 3 + waveNumber * 2;
        int slimeCount = 2 + waveNumber;
        int orcCount = Math.max(0, waveNumber - 1);

        Random rnd = new Random();
        Vector2D center = new Vector2D(GameWorld.WORLD_WIDTH / 2.0, GameWorld.WORLD_HEIGHT / 2.0);

        for (int i = 0; i < goblinCount; i++) {
            Vector2D spawn = getRandSpawnAround(center, 400, 750, rnd);
            monsters.add(new Monster("goblin_" + waveNumber + "_" + i, Monster.MonsterType.GOBLIN, spawn));
        }
        for (int i = 0; i < slimeCount; i++) {
            Vector2D spawn = getRandSpawnAround(center, 350, 700, rnd);
            monsters.add(new Monster("slime_" + waveNumber + "_" + i, Monster.MonsterType.SLIME, spawn));
        }
        for (int i = 0; i < orcCount; i++) {
            Vector2D spawn = getRandSpawnAround(center, 450, 800, rnd);
            monsters.add(new Monster("orc_" + waveNumber + "_" + i, Monster.MonsterType.ORC_WARRIOR, spawn));
        }
    }

    private Vector2D getRandSpawnAround(Vector2D center, double minR, double maxR, Random rnd) {
        double angle = rnd.nextDouble() * Math.PI * 2;
        double r = minR + rnd.nextDouble() * (maxR - minR);
        double sx = Math.max(64, Math.min(GameWorld.WORLD_WIDTH - 64, center.getX() + Math.cos(angle) * r));
        double sy = Math.max(64, Math.min(GameWorld.WORLD_HEIGHT - 64, center.getY() + Math.sin(angle) * r));
        return new Vector2D(sx, sy);
    }

    private void clampWithinBounds(LivingEntity entity) {
        Vector2D pos = entity.getPosition();
        double clampedX = Math.max(40, Math.min(GameWorld.WORLD_WIDTH - 40, pos.getX()));
        double clampedY = Math.max(40, Math.min(GameWorld.WORLD_HEIGHT - 40, pos.getY()));
        if (clampedX != pos.getX() || clampedY != pos.getY()) {
            entity.setPosition(new Vector2D(clampedX, clampedY));
        }
    }

    public void broadcast(GamePacket packet) {
        for (ClientHandler client : clients) {
            client.sendPacket(packet);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getPlayerId() != null) {
            connectedPlayers.remove(client.getPlayerId());
            System.out.printf("🔌 Người chơi rời phòng: [ID: %s]%n", client.getPlayerId());
        }
    }

    public void stop() {
        isRunning = false;
        if (tickExecutor != null) tickExecutor.shutdown();
        try {
            if (serverSocket != null) serverSocket.close();
            for (ClientHandler c : clients) c.close();
        } catch (IOException ignored) {}
    }

    public static class ClientHandler implements Runnable {
        private final Socket socket;
        private final DedicatedGameServer server;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String playerId;

        public ClientHandler(Socket socket, DedicatedGameServer server) {
            this.socket = socket;
            this.server = server;
        }

        public String getPlayerId() {
            return playerId;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream(), 8192));
                out.flush();
                in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream(), 8192));

                while (true) {
                    GamePacket packet = (GamePacket) in.readObject();
                    if (packet == null) break;

                    if (packet.getType() == PacketType.JOIN_REQUEST) {
                        String requestedName = (String) packet.getPayload();
                        this.playerId = server.registerNewPlayer(requestedName);
                        // Trả về JOIN_RESPONSE kèm theo playerId được cấp phát
                        sendPacket(new GamePacket(PacketType.JOIN_RESPONSE, "SERVER", playerId));
                    } else if (packet.getType() == PacketType.PLAYER_INPUT) {
                        PlayerInputPayload input = (PlayerInputPayload) packet.getPayload();
                        server.handleClientInput(playerId, input);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                close();
                server.removeClient(this);
            }
        }

        public synchronized void sendPacket(GamePacket packet) {
            try {
                if (out != null) {
                    out.writeObject(packet);
                    out.reset();
                    out.flush();
                }
            } catch (IOException e) {
                close();
            }
        }

        public void close() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
