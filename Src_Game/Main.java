import server.DedicatedGameServer;
import tests.GameUnitTestSuite;
import ui.GameWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Điểm khởi chạy chính (Main Application Entry Point) của đồ án Game.
 * Hỗ trợ đa chế độ:
 * 1. Chơi Game Đồ họa Cục bộ 60 FPS
 * 2. Đấu mạng nhiều người chơi (Multiplayer Client kết nối 20Hz Dedicated Server)
 * 3. Máy chủ Dedicated Server 20Hz (Headless Mode)
 * 4. Chạy Toàn bộ Bộ Kiểm thử Tự động 15 Test Cases
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();

            // Chế độ 1: Máy chủ Dedicated Server
            if (mode.equals("--server") || mode.equals("-s")) {
                int port = 7777;
                if (args.length > 1) {
                    try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                }
                DedicatedGameServer server = new DedicatedGameServer(port);
                server.start();
                return;
            }

            // Chế độ 2: Trình khách kết nối Đấu mạng (Multiplayer Client)
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

            // Nếu truyền trực tiếp tên người chơi
            if (!mode.startsWith("-")) {
                launchClient(args[0], null, 0);
                return;
            }
        }

        // Mặc định khi không có cờ tham số: Hiển thị Menu lựa chọn chế độ
        SwingUtilities.invokeLater(Main::showModeSelectionDialog);
    }

    private static void showModeSelectionDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("⚔️ UML & PATTERNS MASTERY - CHỌN CHẾ ĐỘ CHƠI", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        panel.add(titleLabel);

        JButton soloBtn = new JButton("1. ⚔️ Chơi Đơn Offline (Chiến Binh Duy Nam - Đấu Quái)");
        JButton multiBtn = new JButton("2. 🌐 Đấu Mạng Multiplayer (Kết Nối Dedicated Server 20Hz)");
        JButton serverBtn = new JButton("3. 🛡️ Khởi Động Dedicated Server (Cổng 7777)");

        soloBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        multiBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        serverBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(soloBtn);
        panel.add(multiBtn);
        panel.add(serverBtn);

        JDialog dialog = new JDialog((Frame) null, "Lựa Chọn Chế Độ Trò Chơi", true);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        soloBtn.addActionListener(e -> {
            dialog.dispose();
            launchClient("Duy Nam (Hero)", null, 0);
        });

        multiBtn.addActionListener(e -> {
            dialog.dispose();
            String name = JOptionPane.showInputDialog(null, "Nhập tên nhân vật của bạn:", "Duy Nam");
            if (name == null || name.trim().isEmpty()) name = "Duy Nam";
            String host = JOptionPane.showInputDialog(null, "Nhập địa chỉ máy chủ (Host):", "127.0.0.1");
            if (host == null || host.trim().isEmpty()) host = "127.0.0.1";
            launchClient(name, host, 7777);
        });

        serverBtn.addActionListener(e -> {
            dialog.dispose();
            new Thread(() -> {
                DedicatedGameServer server = new DedicatedGameServer(7777);
                server.start();
            }).start();
            JOptionPane.showMessageDialog(null, "✅ Dedicated Server 20Hz đã khởi động thành công trên cổng 7777!\nBây giờ bạn có thể mở các cửa sổ Client để cùng tham chiến.", "Máy Chủ Đang Chạy", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.setVisible(true);
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
        System.out.println("1. Chạy Menu Đồ Họa Lựa Chọn:");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar");
        System.out.println();
        System.out.println("2. Đấu Mạng Trực Tiếp (Multiplayer Client):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar --client 127.0.0.1 7777 \"Trung Kien\"");
        System.out.println();
        System.out.println("3. Chạy Dedicated Server Đấu Mạng (20Hz Headless):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar --server 7777");
        System.out.println();
        System.out.println("4. Chạy Toàn Bộ Kiểm Thử Tự Động (15 Test Cases):");
        System.out.println("   java -jar build/UmlPatternsMasteryGame.jar --test");
        System.out.println("==================================================");
    }
}
