import server.DedicatedGameServer;
import tests.GameUnitTestSuite;
import ui.GameWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Điểm khởi chạy chính (Main Application Entry Point) của đồ án Game.
 * Hỗ trợ đa chế độ:
 * 1. Chơi Game Đồ họa Cục bộ 60 FPS (Mặc định): java -jar game.jar
 * 2. Máy chủ Dedicated Server 20Hz (Headless Mode): java -jar game.jar --server 7777
 * 3. Chạy Toàn bộ Bộ Kiểm thử Tự động: java -jar game.jar --test
 */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();

            if (mode.equals("--server") || mode.equals("-s")) {
                int port = 7777;
                if (args.length > 1) {
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                DedicatedGameServer server = new DedicatedGameServer(port);
                server.start();
                return;
            } else if (mode.equals("--test") || mode.equals("-t")) {
                GameUnitTestSuite.main(new String[0]);
                return;
            } else if (mode.equals("--help") || mode.equals("-h")) {
                printHelp();
                return;
            }
        }

        // Mặc định: Khởi động Game Client Đồ họa 60 FPS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String playerName = "Duy Nam (Chiến Binh)";
        if (args.length > 0 && !args[0].startsWith("-")) {
            playerName = args[0];
        }

        final String finalPlayerName = playerName;
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(finalPlayerName, 1152, 720);
            window.launch();
            System.out.println("🚀 Trò chơi đã khởi động thành công trên màn hình (1152x720 @ 60 FPS)!");
        });
    }

    private static void printHelp() {
        System.out.println("==================================================");
        System.out.println("🎮 UML & PATTERNS MASTERY - HƯỚNG DẪN KHỞI CHẠY");
        System.out.println("==================================================");
        System.out.println("1. Chơi Game Ngay (Đồ họa 60 FPS):");
        System.out.println("   java -jar UmlPatternsMasteryGame.jar [Tên_Người_Chơi]");
        System.out.println();
        System.out.println("2. Chạy Dedicated Server Đấu Mạng (20Hz Headless):");
        System.out.println("   java -jar UmlPatternsMasteryGame.jar --server [Cổng]");
        System.out.println("   (Ví dụ: java -jar UmlPatternsMasteryGame.jar --server 7777)");
        System.out.println();
        System.out.println("3. Chạy Toàn Bộ Kiểm Thử Tự Động (15 Test Cases):");
        System.out.println("   java -jar UmlPatternsMasteryGame.jar --test");
        System.out.println("==================================================");
    }
}
