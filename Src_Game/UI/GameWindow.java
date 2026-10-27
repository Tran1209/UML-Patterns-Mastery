package ui;

import core.GameEngineLoop;
import core.GameWorld;
import network.NetworkGameClient;
import persistence.JsonFileMatchRepository;
import persistence.MatchSummary;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Cửa sổ trò chơi chính (Main Game Window) xây dựng trên nền tảng Java Swing.
 * Tích hợp vòng lặp GameEngineLoop chuẩn Robert Nystrom và hỗ trợ cả Chơi Đơn lẫn Đấu Mạng Multiplayer.
 */
public class GameWindow extends JFrame {
    private final GameWorld world;
    private final NetworkGameClient networkClient;
    private final GameCanvas canvas;
    private final GameEngineLoop engineLoop;
    private final JsonFileMatchRepository matchRepo;

    /**
     * Khởi tạo cửa sổ Chơi Đơn (Single-player Offline).
     */
    public GameWindow(String playerName, int width, int height) {
        this(playerName, null, 0, width, height);
    }

    /**
     * Khởi tạo cửa sổ Đấu Mạng Multiplayer (Kết nối tới Dedicated Server).
     */
    public GameWindow(String playerName, String host, int port, int width, int height) {
        super(host != null ? ("⚔️ [MULTIPLAYER] UML Patterns Arena - " + playerName + " (" + host + ":" + port + ")")
                : ("⚔️ [OFFLINE SOLO] UML Patterns Arena - " + playerName));

        this.world = new GameWorld(playerName);
        this.matchRepo = new JsonFileMatchRepository("build/match_history.json");

        if (host != null && port > 0) {
            this.networkClient = new NetworkGameClient(host, port, playerName);
            boolean connected = this.networkClient.connect();
            if (!connected) {
                System.err.println("⚠️ Không thể kết nối tới Server, chuyển về chế độ Chơi Đơn!");
            }
        } else {
            this.networkClient = null;
        }

        this.canvas = new GameCanvas(world, networkClient, width, height);

        setContentPane(canvas);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        // Khởi tạo vòng lặp 60 Hz chuẩn theo Game Programming Patterns
        this.engineLoop = new GameEngineLoop(60) {
            @Override
            protected void fixedUpdate(double deltaTime) {
                canvas.processInput();
                if (networkClient == null) {
                    world.update(deltaTime);
                }
            }

            @Override
            protected void render(float alpha) {
                canvas.repaint();
            }
        };

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                engineLoop.stop();
                if (networkClient != null) {
                    networkClient.disconnect();
                }
                // Lưu vết trận đấu trước khi thoát game (Persistence)
                try {
                    MatchSummary summary = new MatchSummary(
                            "MATCH_" + System.currentTimeMillis(),
                            world.getPlayer().getPlayerName(),
                            world.getScore(),
                            List.of("Quái tiêu diệt: " + world.getPlayer().getKillCount(), "Đợt vượt qua: " + world.getCurrentWave())
                    );
                    matchRepo.saveMatch(summary);
                } catch (Exception ignored) {}
            }
        });
    }

    public void launch() {
        setVisible(true);
        canvas.requestFocusInWindow();
        engineLoop.start();
    }
}
