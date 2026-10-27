package ui;

import network.LanDiscovery;
import server.DedicatedGameServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Random;

/**
 * Giao diện Sảnh Chờ (In-Game Lobby) mô phỏng trải nghiệm game online hiện đại:
 * 1. Cơ chế Tạo Phòng (Host Room) & Sinh Mã Mời tự động.
 * 2. Cơ chế Vào Bằng Mã Phòng (Join by Room Code).
 * 3. Tự động Dò Tìm Phòng Qua Mạng LAN/WiFi (Auto-discovery - Click là vào, không cần gõ IP).
 * 4. Chơi Đơn Luyện Tập (Offline Solo).
 */
public class LobbyPanel extends JPanel {
    private final JFrame parentFrame;
    private final JTextField nameField;
    private final LanDiscovery lanDiscovery;
    private final DefaultListModel<String> roomListModel;
    private final JList<String> roomJList;

    public LobbyPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.lanDiscovery = new LanDiscovery();
        this.roomListModel = new DefaultListModel<>();
        this.roomJList = new JList<>(roomListModel);

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(24, 26, 33));
        setBorder(new EmptyBorder(25, 30, 25, 30));

        // --- 1. TIÊU ĐỀ TRÊN CÙNG ---
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("⚔️ UML & PATTERNS MASTERY - SẢNH CHỜ ĐẤU MẠNG", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(255, 215, 0));

        JLabel subtitleLabel = new JLabel("Hệ thống phòng đấu mạng thời gian thực — Hỗ trợ Mã Mời & Tự động quét phòng", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(170, 175, 190));

        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. KHU VỰC TRUNG TÂM (TÊN NGƯỜI CHƠI & CÁC TÙY CHỌN) ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        centerPanel.setOpaque(false);

        // Cột Trái: Đăng ký tên & Tạo phòng
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setBackground(new Color(32, 35, 45));
        leftCol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 60, 75), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel namePrompt = new JLabel("👤 Tên nhân vật của bạn:");
        namePrompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        namePrompt.setForeground(Color.WHITE);
        namePrompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameField = new JTextField("Duy Nam");
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createRoomBtn = new JButton("🏠 TẠO PHÒNG MỚI (Lấy Mã Mời)");
        createRoomBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createRoomBtn.setBackground(new Color(46, 204, 113));
        createRoomBtn.setForeground(Color.WHITE);
        createRoomBtn.setFocusPainted(false);
        createRoomBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        createRoomBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createRoomBtn.addActionListener(e -> handleCreateRoom());

        JButton joinByCodeBtn = new JButton("🔑 NHẬP MÃ PHÒNG ĐỂ VÀO");
        joinByCodeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        joinByCodeBtn.setBackground(new Color(52, 152, 219));
        joinByCodeBtn.setForeground(Color.WHITE);
        joinByCodeBtn.setFocusPainted(false);
        joinByCodeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        joinByCodeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        joinByCodeBtn.addActionListener(e -> handleJoinByCode());

        JButton soloBtn = new JButton("⚔️ Chơi Đơn Luyện Tập (Offline)");
        soloBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        soloBtn.setBackground(new Color(75, 80, 95));
        soloBtn.setForeground(Color.LIGHT_GRAY);
        soloBtn.setFocusPainted(false);
        soloBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        soloBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        soloBtn.addActionListener(e -> handlePlaySolo());

        leftCol.add(namePrompt);
        leftCol.add(Box.createVerticalStrut(6));
        leftCol.add(nameField);
        leftCol.add(Box.createVerticalStrut(20));
        leftCol.add(createRoomBtn);
        leftCol.add(Box.createVerticalStrut(12));
        leftCol.add(joinByCodeBtn);
        leftCol.add(Box.createVerticalStrut(12));
        leftCol.add(soloBtn);

        // Cột Phải: Danh sách phòng tự động dò tìm (LAN Auto-Discovery)
        JPanel rightCol = new JPanel(new BorderLayout(8, 8));
        rightCol.setBackground(new Color(32, 35, 45));
        rightCol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 60, 75), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel scanTitle = new JLabel("📡 PHÒNG ĐẤU QUANH ĐÂY (Dò tìm tự động):");
        scanTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        scanTitle.setForeground(new Color(255, 215, 0));

        roomJList.setBackground(new Color(20, 22, 28));
        roomJList.setForeground(Color.WHITE);
        roomJList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roomJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(roomJList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 80)));

        JButton joinSelectedBtn = new JButton("👉 VÀO PHÒNG ĐÃ CHỌN");
        joinSelectedBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        joinSelectedBtn.setBackground(new Color(241, 196, 15));
        joinSelectedBtn.setForeground(Color.BLACK);
        joinSelectedBtn.setFocusPainted(false);
        joinSelectedBtn.addActionListener(e -> handleJoinSelectedRoom());

        rightCol.add(scanTitle, BorderLayout.NORTH);
        rightCol.add(scrollPane, BorderLayout.CENTER);
        rightCol.add(joinSelectedBtn, BorderLayout.SOUTH);

        centerPanel.add(leftCol);
        centerPanel.add(rightCol);
        add(centerPanel, BorderLayout.CENTER);

        // Bắt đầu lắng nghe các phòng LAN tự động
        lanDiscovery.startListening();
        startRoomListRefreshTimer();
    }

    private void startRoomListRefreshTimer() {
        Timer timer = new Timer(1500, e -> {
            roomListModel.clear();
            for (LanDiscovery.DiscoveredRoom r : lanDiscovery.getActiveRooms().values()) {
                roomListModel.addElement(String.format("🏰 [%s] Phòng của %s | IP: %s:%d",
                        r.roomCode, r.hostName, r.hostIp, r.serverPort));
            }
            if (roomListModel.isEmpty()) {
                roomListModel.addElement("⏳ Đang dò tìm phòng trong mạng WiFi... (Chưa có phòng nào)");
            }
        });
        timer.start();
    }

    private void handleCreateRoom() {
        String pName = getPlayerName();
        String roomCode = "ARENA-" + (1000 + new Random().nextInt(9000));
        int port = 7777;

        // Khởi động Dedicated Server ngầm ngay bên trong máy Host (không cần mở terminal ngoài)
        new Thread(() -> {
            DedicatedGameServer server = new DedicatedGameServer(port);
            server.start();
        }, "Host-InternalServer").start();

        // Phát sóng phòng ra toàn mạng LAN
        lanDiscovery.startBroadcasting(roomCode, pName, port);

        // Hiển thị hộp thoại mã mời với nút Copy
        String inviteMessage = String.format(
                "🎉 BẠN ĐÃ TẠO PHÒNG THÀNH CÔNG!\n\n" +
                "🔑 MÃ PHÒNG CỦA BẠN:  %s\n" +
                "📡 CỔNG KẾT NỐI:  %d\n\n" +
                "👉 Bạn bè cùng mạng WiFi sẽ tự thấy phòng của bạn trên danh sách!\n" +
                "👉 Bạn bè dùng 4G ở xa chỉ cần nhập địa chỉ IP/Tunnel của bạn để vào.",
                roomCode, port
        );

        int choice = JOptionPane.showOptionDialog(
                parentFrame,
                inviteMessage,
                "Phòng Đấu Đã Tạo - Mã Mời: " + roomCode,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"📋 COPY MÃ MỜI VÀ VÀO PHÒNG", "VÀO PHÒNG NGAY"},
                "📋 COPY MÃ MỜI VÀ VÀO PHÒNG"
        );

        if (choice == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection("Vào chơi game cùng mình nhé! Mã phòng: " + roomCode), null
            );
        }

        // Vào phòng với tư cách Chủ phòng (Host)
        launchGameWindow(pName, "127.0.0.1", port);
    }

    private void handleJoinByCode() {
        String pName = getPlayerName();
        String input = JOptionPane.showInputDialog(
                parentFrame,
                "Nhập Mã Phòng (ví dụ: ARENA-1234) hoặc Địa chỉ IP Server:",
                "127.0.0.1"
        );

        if (input != null && !input.trim().isEmpty()) {
            input = input.trim();
            String host = input;
            int port = 7777;

            // Nếu người dùng nhập dạng host:port
            if (input.contains(":")) {
                String[] parts = input.split(":");
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            launchGameWindow(pName, host, port);
        }
    }

    private void handleJoinSelectedRoom() {
        int selectedIdx = roomJList.getSelectedIndex();
        if (selectedIdx >= 0 && selectedIdx < lanDiscovery.getActiveRooms().size()) {
            LanDiscovery.DiscoveredRoom room = new java.util.ArrayList<>(lanDiscovery.getActiveRooms().values()).get(selectedIdx);
            launchGameWindow(getPlayerName(), room.hostIp, room.serverPort);
        } else {
            JOptionPane.showMessageDialog(parentFrame, "Vui lòng chọn một phòng trong danh sách dò tìm!");
        }
    }

    private void handlePlaySolo() {
        launchGameWindow(getPlayerName(), null, 0);
    }

    private String getPlayerName() {
        String name = nameField.getText().trim();
        return name.isEmpty() ? "Duy Nam" : name;
    }

    private void launchGameWindow(String playerName, String host, int port) {
        lanDiscovery.stop();
        parentFrame.dispose();

        SwingUtilities.invokeLater(() -> {
            GameWindow gameWindow = new GameWindow(playerName, host, port, 1152, 720);
            gameWindow.launch();
        });
    }
}
