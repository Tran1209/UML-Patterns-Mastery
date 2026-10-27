package server;

import core.GameWorld;
import core.LivingEntity;
import core.Monster;
import core.Player;
import network.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Máy chủ chuyên dụng độc lập (Dedicated Authoritative Server 20Hz).
 * Hoạt động hoàn toàn ở chế độ không đầu (Headless Mode - KHÔNG chứa thư viện đồ họa GUI).
 * Tham chiếu kiến trúc: Joshua Glazer & Sanjay Madhav, Multiplayer Game Programming.
 */
public class DedicatedGameServer {
    private final int port;
    private final GameWorld world;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean isRunning = false;
    private ServerSocket serverSocket;
    private ScheduledExecutorService tickExecutor;

    public DedicatedGameServer(int port) {
        this.port = port;
        this.world = new GameWorld("DedicatedServerHost");
    }

    public void start() {
        isRunning = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("==================================================");
            System.out.println("🌐 DEDICATED AUTHORITATIVE SERVER ĐANG CHẠY");
            System.out.println("📡 Cổng lắng nghe (Port): " + port);
            System.out.println("⏱️ Tần số đồng bộ (Tick Rate): 20 Hz (50ms / snapshot)");
            System.out.println("🖥️ Môi trường: Headless Pure Java (Không đồ họa)");
            System.out.println("==================================================");

            // Bắt đầu vòng lặp vật lý và phát sóng 20Hz (mỗi 50ms)
            tickExecutor = Executors.newSingleThreadScheduledExecutor();
            tickExecutor.scheduleAtFixedRate(this::tickServer20Hz, 50, 50, TimeUnit.MILLISECONDS);

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
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler, "ClientHandler-" + clientSocket.getRemoteSocketAddress()).start();
                System.out.println("✅ Người chơi mới kết nối: " + clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                if (!isRunning) break;
            }
        }
    }

    private void tickServer20Hz() {
        try {
            double dt = 0.050; // 50ms cho chu kỳ 20Hz
            world.update(dt);

            // Xây dựng gói tin WorldSnapshot
            List<WorldSnapshotPayload.EntityState> states = new ArrayList<>();
            Player p = world.getPlayer();
            states.add(new WorldSnapshotPayload.EntityState(
                    p.getId(), p.getPlayerName(),
                    p.getPosition().getX(), p.getPosition().getY(),
                    p.getCurrentHp(), p.getMaxHp(),
                    p.getCurrentState().getStateName(), false
            ));

            for (Monster m : world.getMonsters()) {
                states.add(new WorldSnapshotPayload.EntityState(
                        m.getId(), m.getType().displayName,
                        m.getPosition().getX(), m.getPosition().getY(),
                        m.getCurrentHp(), m.getMaxHp(),
                        m.isAlive() ? "ALIVE" : "DEAD", true
                ));
            }

            WorldSnapshotPayload snapshot = new WorldSnapshotPayload(System.currentTimeMillis(), states, world.getCurrentWave());
            GamePacket packet = new GamePacket(PacketType.WORLD_SNAPSHOT, "SERVER", snapshot);
            broadcast(packet);

        } catch (Exception e) {
            System.err.println("⚠️ Lỗi vòng lặp Server 20Hz: " + e.getMessage());
        }
    }

    public void broadcast(GamePacket packet) {
        for (ClientHandler client : clients) {
            client.sendPacket(packet);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("🔌 Người chơi đã ngắt kết nối: " + client.getClientId());
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
        private String clientId = "Unknown";

        public ClientHandler(Socket socket, DedicatedGameServer server) {
            this.socket = socket;
            this.server = server;
        }

        public String getClientId() {
            return clientId;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    GamePacket packet = (GamePacket) in.readObject();
                    if (packet == null) break;

                    if (packet.getType() == PacketType.JOIN_REQUEST) {
                        this.clientId = (String) packet.getPayload();
                        System.out.println("👤 Xác thực danh tính người chơi: " + clientId);
                        sendPacket(new GamePacket(PacketType.JOIN_RESPONSE, "SERVER", "SUCCESS"));
                    } else if (packet.getType() == PacketType.PLAYER_INPUT) {
                        PlayerInputPayload input = (PlayerInputPayload) packet.getPayload();
                        // Authoritative Server áp dụng vector vận tốc lên thế giới
                        server.world.getPlayer().setVelocity(new core.Vector2D(input.moveVx, input.moveVy));
                        if (input.isAttacking) {
                            server.world.handlePlayerAttack();
                        }
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
