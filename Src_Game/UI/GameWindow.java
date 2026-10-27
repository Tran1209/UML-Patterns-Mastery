package ui;

import core.GameEngineLoop;
import core.GameWorld;
import persistence.JsonFileMatchRepository;
import persistence.MatchSummary;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Cửa sổ trò chơi chính (Main Game Window) xây dựng trên nền tảng Java Swing.
 * Tích hợp vòng lặp GameEngineLoop chuẩn Robert Nystrom và lưu vết lịch sử trận đấu (Data Mapper).
 */
public class GameWindow extends JFrame {
    private final GameWorld world;
    private final GameCanvas canvas;
    private final GameEngineLoop engineLoop;
    private final JsonFileMatchRepository matchRepo;

    public GameWindow(String playerName, int width, int height) {
        super("⚔️ UML & Patterns Mastery - 2D Action RPG Arena (HUCE - 2026)");

        this.world = new GameWorld(playerName);
        this.canvas = new GameCanvas(world, width, height);
        this.matchRepo = new JsonFileMatchRepository("build/match_history.json");

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
                world.update(deltaTime);
            }

            @Override
            protected void render(float alpha) {
                // Yêu cầu vẽ lại khung hình Canvas với tốc độ màn hình
                canvas.repaint();
            }
        };

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                engineLoop.stop();
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
