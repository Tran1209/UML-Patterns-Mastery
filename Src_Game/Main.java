import server.DedicatedGameServer;
import tests.GameUnitTestSuite;
import ui.GameWindow;
import ui.LobbyPanel;

import javax.swing.*;

/**
 * Điểm khởi chạy chính (Main Application Entry Point) của đồ án Game.
 * Tích hợp hệ thống Sảnh Chờ (Lobby) hiện đại:
 * 1. Mặc định mở Sảnh Chờ (Lobby UI) hỗ trợ Tạo Phòng, Sinh Mã Mời, Tự động quét phòng LAN.
 * 2. Hỗ trợ khởi động nhanh qua tham số dòng lệnh CLI (--server, --client, --test).
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();

            // Chế độ 1: Máy chủ Dedicated Server Headless
            if (mode.equals("--server") || mode.equals("-s")) {
                int port = 7777;
                if (args.length > 1) {
                    try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                }
                DedicatedGameServer server = new DedicatedGameServer(port);
                server.start();
                return;
            }

            // Chế độ 2: Trình khách kết nối Đấu mạng nhanh (Multiplayer Client)
            if (mode.equals("--client") || mode.equals("-c") || mode.equals("--multi")) {
                String host = (args.length > 1) ? args[1] : "127.0.0.1";
                int port = 7777;
                if (args.length > 2) {
                    try { port = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                String name = (args.length > 3) ? args[3] : "Duy Nam";
                launchClient(name, host, port);
                return;
            }

            // Chế độ 3: Chơi đơn trực tiếp theo tên
            if (mode.equals("--solo") || mode.equals("--single")) {
                String name = (args.length > 1) ? args[1] : "Duy Nam";
                launchClient(name, null, 0);
                return;
            }

            // Chế độ 4: Chạy kiểm thử tự động
            if (mode.equals("--test") || mode.equals("-t")) {
                GameUnitTestSuite.main(new String[0]);
                return;
            }

            if (mode.equals("--help") || mode.equals("-h")) {
                printHelp();
                return;
            }
        }

        // Mặc định: Mở Sảnh Chờ Hiện Đại (Lobby Window)
        SwingUtilities.invokeLater(Main::launchLobbyWindow);
    }

    private static void launchLobbyWindow() {
        JFrame lobbyFrame = new JFrame("⚔️ UML & Patterns Mastery - Sảnh Chờ Đấu Mạng");
        LobbyPanel lobbyPanel = new LobbyPanel(lobbyFrame);

        lobbyFrame.setContentPane(lobbyPanel);
        lobbyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        lobbyFrame.setSize(840, 480);
        lobbyFrame.setLocationRelativeTo(null);
        lobbyFrame.setResizable(false);
        lobbyFrame.setVisible(true);
    }

    private static void launchClient(String playerName, String host, int port) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(playerName, host, port, 1152, 720);
            window.launch();
            System.out.printf("🚀 Khởi chạy trò chơi: [Tên: %s | Host: %s | Port: %d]%n", playerName, host, port);
        });
    }

    private static void printHelp() {
        System.out.println("==================================================");
        System.out.println("🎮 UML & PATTERNS MASTERY - HƯỚNG DẪN KHỞI CHẠY");
        System.out.println("==================================================");
        System.out.println("1. Mở Sảnh Chờ Game (Lobby UI - Tạo phòng & Nhập mã):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar");
        System.out.println();
        System.out.println("2. Đấu Mạng Trực Tiếp (Multiplayer Client):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 \"Duy Nam\"");
        System.out.println();
        System.out.println("3. Chạy Dedicated Server Đấu Mạng (20Hz Headless):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar --server 7777");
        System.out.println();
        System.out.println("4. Chạy Toàn Bộ Kiểm Thử Tự Động (15 Test Cases):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar --test");
        System.out.println("==================================================");
    }
}
