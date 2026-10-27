package com.huce.oop.game;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import network.LanDiscovery;
import server.DedicatedGameServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Giao diện Sảnh Chờ (In-Game Lobby) trên nền tảng Android.
 * Đồng nhất 1:1 với trải nghiệm trên PC:
 * 1. Cơ chế Tạo Phòng (Host Room) & Tự động sinh Mã Mời (ARENA-XXXX).
 * 2. Hộp thoại Sao chép Mã Mời (Copy to Clipboard) để gửi cho bạn bè qua Zalo/Mess.
 * 3. Tự động Dò Tìm Phòng Qua Mạng WiFi/LAN (Auto-Discovery) - Không cần gõ IP.
 * 4. Cơ chế Nhập Mã Phòng / Địa chỉ IP để kết nối từ xa qua 4G.
 * 5. Chế độ Chơi Đơn Luyện Tập (Offline Solo).
 */
public class AndroidLobbyView extends RelativeLayout {
    public interface LobbyListener {
        void onStartGame(String playerName, String host, int port, String roomCode);
    }

    private final LobbyListener listener;
    private final LanDiscovery lanDiscovery;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRoomsRunnable;

    private EditText txtPlayerName;
    private ListView listRooms;
    private ArrayAdapter<String> roomAdapter;
    private final List<String> roomDisplayList = new ArrayList<>();
    private final List<LanDiscovery.DiscoveredRoom> activeRoomsCache = new ArrayList<>();

    public AndroidLobbyView(Context context, LobbyListener listener) {
        super(context);
        this.listener = listener;
        this.lanDiscovery = new LanDiscovery();

        setBackgroundColor(Color.rgb(24, 26, 33)); // #181A21 chuẩn màu PC

        initUI(context);

        // Bắt đầu quét phòng tự động qua WiFi
        lanDiscovery.startListening();

        refreshRoomsRunnable = new Runnable() {
            @Override
            public void run() {
                refreshDiscoveredRooms();
                handler.postDelayed(this, 1500);
            }
        };
        handler.post(refreshRoomsRunnable);
    }

    private void initUI(Context context) {
        // --- 1. TIÊU ĐỀ TRÊN CÙNG ---
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setId(View.generateViewId());
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setGravity(Gravity.CENTER);
        headerLayout.setPadding(16, 20, 16, 12);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("⚔️ UML & PATTERNS MASTERY - SẢNH CHỜ ĐẤU MẠNG");
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.rgb(255, 215, 0)); // Màu vàng hoàng kim
        tvTitle.setGravity(Gravity.CENTER);
        headerLayout.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Hệ thống phòng đấu mạng thời gian thực — Hỗ trợ Mã Mời & Tự động quét WiFi LAN");
        tvSub.setTextSize(12f);
        tvSub.setTextColor(Color.rgb(170, 175, 190));
        tvSub.setGravity(Gravity.CENTER);
        headerLayout.addView(tvSub);

        LayoutParams headerParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        addView(headerLayout, headerParams);

        // --- 2. VÙNG NỘI DUNG 2 CỘT CHÍNH ---
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setPadding(24, 0, 24, 16);

        // ================= CỘT TRÁI: TÊN NHÂN VẬT & CÁC NÚT TẠO/VÀO =================
        LinearLayout leftCol = new LinearLayout(context);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.48f);
        leftParams.setMargins(0, 0, 16, 0);
        leftCol.setLayoutParams(leftParams);
        leftCol.setPadding(20, 16, 20, 16);
        applyCardBackground(leftCol);

        TextView lblName = new TextView(context);
        lblName.setText("👤 Tên nhân vật của bạn:");
        lblName.setTextSize(14f);
        lblName.setTypeface(Typeface.DEFAULT_BOLD);
        lblName.setTextColor(Color.WHITE);
        leftCol.addView(lblName);

        txtPlayerName = new EditText(context);
        txtPlayerName.setText("Đấu Sĩ Nam");
        txtPlayerName.setTextColor(Color.WHITE);
        txtPlayerName.setTextSize(15f);
        txtPlayerName.setPadding(16, 12, 16, 12);
        applyInputBackground(txtPlayerName);
        LinearLayout.LayoutParams nameFieldParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameFieldParams.setMargins(0, 8, 0, 16);
        leftCol.addView(txtPlayerName, nameFieldParams);

        // Nút Tạo Phòng Mới (Màu xanh lục)
        Button btnCreateRoom = createStyledButton(context, "🏠 TẠO PHÒNG MỚI (Lấy Mã Mời)", Color.rgb(46, 204, 113), Color.WHITE, 14f);
        btnCreateRoom.setOnClickListener(v -> handleCreateRoom(context));
        leftCol.addView(btnCreateRoom, createButtonParams(10));

        // Nút Nhập Mã Phòng (Màu xanh dương)
        Button btnJoinByCode = createStyledButton(context, "🔑 NHẬP MÃ PHÒNG / IP ĐỂ VÀO", Color.rgb(52, 152, 219), Color.WHITE, 13f);
        btnJoinByCode.setOnClickListener(v -> handleJoinByCode(context));
        leftCol.addView(btnJoinByCode, createButtonParams(10));

        // Nút Chơi Đơn (Màu xám)
        Button btnSolo = createStyledButton(context, "⚔️ Chơi Đơn Luyện Tập (Offline)", Color.rgb(75, 80, 95), Color.rgb(220, 220, 220), 12f);
        btnSolo.setOnClickListener(v -> handlePlaySolo());
        leftCol.addView(btnSolo, createButtonParams(0));

        // ================= CỘT PHẢI: QUÉT PHÒNG LAN TỰ ĐỘNG =================
        LinearLayout rightCol = new LinearLayout(context);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.52f);
        rightCol.setLayoutParams(rightParams);
        rightCol.setPadding(20, 16, 20, 16);
        applyCardBackground(rightCol);

        TextView lblScan = new TextView(context);
        lblScan.setText("📡 PHÒNG ĐẤU QUANH ĐÂY (Dò tìm tự động):");
        lblScan.setTextSize(14f);
        lblScan.setTypeface(Typeface.DEFAULT_BOLD);
        lblScan.setTextColor(Color.rgb(255, 215, 0));
        rightCol.addView(lblScan);

        TextView lblSubScan = new TextView(context);
        lblSubScan.setText("(Tự động thấy phòng máy tính PC hoặc điện thoại trong cùng mạng WiFi)");
        lblSubScan.setTextSize(11f);
        lblSubScan.setTextColor(Color.rgb(170, 175, 190));
        rightCol.addView(lblSubScan);

        listRooms = new ListView(context);
        listRooms.setBackgroundColor(Color.rgb(20, 22, 28));
        roomAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_single_choice, roomDisplayList);
        listRooms.setAdapter(roomAdapter);
        listRooms.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listParams.setMargins(0, 8, 0, 12);
        rightCol.addView(listRooms, listParams);

        Button btnJoinSelected = createStyledButton(context, "👉 VÀO PHÒNG ĐÃ CHỌN", Color.rgb(241, 196, 15), Color.BLACK, 14f);
        btnJoinSelected.setOnClickListener(v -> handleJoinSelectedRoom(context));
        rightCol.addView(btnJoinSelected, createButtonParams(0));

        contentLayout.addView(leftCol);
        contentLayout.addView(rightCol);

        LayoutParams contentParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        contentParams.addRule(RelativeLayout.BELOW, headerLayout.getId());
        addView(contentLayout, contentParams);
    }

    private void refreshDiscoveredRooms() {
        roomDisplayList.clear();
        activeRoomsCache.clear();

        for (LanDiscovery.DiscoveredRoom r : lanDiscovery.getActiveRooms().values()) {
            activeRoomsCache.add(r);
            roomDisplayList.add(String.format("🏰 [%s] Phòng của %s\n   IP: %s:%d",
                    r.roomCode, r.hostName, r.hostIp, r.serverPort));
        }

        if (roomDisplayList.isEmpty()) {
            roomDisplayList.add("⏳ Đang dò tìm phòng trong mạng WiFi... (Chưa có phòng nào)");
        }
        roomAdapter.notifyDataSetChanged();
    }

    private void handleCreateRoom(Context context) {
        String pName = getPlayerName();
        String roomCode = "ARENA-" + (1000 + new Random().nextInt(9000));
        int port = 7777;

        // Khởi động Dedicated Server ngầm ngay trên điện thoại (đóng vai trò Host)
        new Thread(() -> {
            DedicatedGameServer server = new DedicatedGameServer(port);
            server.start();
        }, "Host-InternalServer-Android").start();

        // Phát sóng phòng ra toàn mạng WiFi
        lanDiscovery.startBroadcasting(roomCode, pName, port);

        String inviteMessage = String.format(
                "🎉 BẠN ĐÃ TẠO PHÒNG THÀNH CÔNG!\n\n" +
                "🔑 MÃ PHÒNG CỦA BẠN:  %s\n" +
                "📡 CỔNG KẾT NỐI:  %d\n\n" +
                "👉 Bạn bè dùng máy tính PC hoặc điện thoại cùng mạng WiFi sẽ tự thấy phòng của bạn trên danh sách!\n" +
                "👉 Bạn bè dùng 4G ở xa chỉ cần nhập địa chỉ IP của bạn để vào.",
                roomCode, port
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Phòng Đấu Đã Tạo - " + roomCode);
        builder.setMessage(inviteMessage);

        builder.setPositiveButton("📋 SAO CHÉP MÃ MỜI & VÀO PHÒNG", (dialog, which) -> {
            copyToClipboard(context, "Vào chơi game cùng mình nhé! Mã phòng: " + roomCode);
            Toast.makeText(context, "Đã sao chép mã mời vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show();
            launchGame(pName, "127.0.0.1", port, roomCode);
        });

        builder.setNegativeButton("VÀO PHÒNG NGAY", (dialog, which) -> {
            launchGame(pName, "127.0.0.1", port, roomCode);
        });

        builder.show();
    }

    private void handleJoinByCode(Context context) {
        String pName = getPlayerName();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tham Gia Phòng Đấu");
        builder.setMessage("Nhập Mã Phòng (ví dụ: ARENA-1234) hoặc Địa chỉ IP Server:");

        final EditText input = new EditText(context);
        input.setHint("192.168.1.15 hoặc ARENA-1234");
        input.setTextColor(Color.BLACK);
        input.setPadding(30, 20, 30, 20);
        builder.setView(input);

        builder.setPositiveButton("VÀO PHÒNG", (dialog, which) -> {
            String str = input.getText().toString().trim();
            if (str.isEmpty()) return;

            String host = str;
            int port = 7777;
            String roomCode = "ARENA-ROOM";

            // Kiểm tra xem có khớp với phòng nào đang dò tìm qua WiFi không
            for (LanDiscovery.DiscoveredRoom r : activeRoomsCache) {
                if (r.roomCode.equalsIgnoreCase(str)) {
                    host = r.hostIp;
                    port = r.serverPort;
                    roomCode = r.roomCode;
                    break;
                }
            }

            if (str.contains(":")) {
                String[] parts = str.split(":");
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            launchGame(pName, host, port, roomCode);
        });

        builder.setNegativeButton("HỦY", null);
        builder.show();
    }

    private void handleJoinSelectedRoom(Context context) {
        int pos = listRooms.getCheckedItemPosition();
        if (pos >= 0 && pos < activeRoomsCache.size()) {
            LanDiscovery.DiscoveredRoom r = activeRoomsCache.get(pos);
            launchGame(getPlayerName(), r.hostIp, r.serverPort, r.roomCode);
        } else {
            Toast.makeText(context, "Vui lòng chạm chọn một phòng trong danh sách dò tìm!", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePlaySolo() {
        launchGame(getPlayerName(), null, 0, "SOLO");
    }

    private void launchGame(String playerName, String host, int port, String roomCode) {
        handler.removeCallbacks(refreshRoomsRunnable);
        lanDiscovery.stop();
        if (listener != null) {
            listener.onStartGame(playerName, host, port, roomCode);
        }
    }

    public void resumeDiscovery() {
        lanDiscovery.startListening();
        handler.post(refreshRoomsRunnable);
    }

    public void stopDiscovery() {
        handler.removeCallbacks(refreshRoomsRunnable);
        lanDiscovery.stop();
    }

    private String getPlayerName() {
        String name = txtPlayerName.getText().toString().trim();
        return name.isEmpty() ? "Đấu Sĩ Nam" : name;
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("GameRoomCode", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    private void applyCardBackground(View view) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.rgb(32, 35, 45)); // #20232D
        gd.setCornerRadius(14f);
        gd.setStroke(2, Color.rgb(55, 60, 75)); // #373C4B
        view.setBackground(gd);
    }

    private void applyInputBackground(View view) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.rgb(20, 22, 28));
        gd.setCornerRadius(8f);
        gd.setStroke(2, Color.rgb(70, 75, 95));
        view.setBackground(gd);
    }

    private Button createStyledButton(Context context, String text, int bgColor, int textColor, float textSize) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(textSize);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(16, 12, 16, 12);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(10f);
        btn.setBackground(gd);
        return btn;
    }

    private LinearLayout.LayoutParams createButtonParams(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }
}
