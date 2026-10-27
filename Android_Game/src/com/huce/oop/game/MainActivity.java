package com.huce.oop.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

/**
 * Màn hình Activity chính của game trên Android.
 * Tích hợp trọn vẹn 2 trạng thái màn hình đồng nhất với bản PC:
 * 1. Sảnh Chờ Đấu Mạng (Lobby Screen - Tạo phòng, Lấy mã mời, Quét WiFi LAN, Vào bằng mã).
 * 2. Đấu Trường Thực Chiến 60 FPS (Game Arena View - Chơi đơn hoặc Đấu mạng chung với PC).
 */
public class MainActivity extends Activity implements AndroidLobbyView.LobbyListener {
    private AndroidLobbyView lobbyView;
    private AndroidGameView gameView;
    private WifiManager.MulticastLock multicastLock;
    private boolean isInGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Giữ màn hình luôn sáng trong suốt trận chiến
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();

        // Kích hoạt Multicast Lock để hỗ trợ quét phòng WiFi LAN trên Android
        acquireMulticastLock();

        // Khởi động vào thẳng Sảnh Chờ (Lobby) giống hệt bản PC
        showLobby();
    }

    private void acquireMulticastLock() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("UmlArenaDiscoveryLock");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
            }
        } catch (Exception ignored) {}
    }

    private void releaseMulticastLock() {
        try {
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
            }
        } catch (Exception ignored) {}
    }

    public void showLobby() {
        isInGame = false;
        if (gameView != null) {
            gameView.pauseGame();
            gameView = null;
        }

        if (lobbyView == null) {
            lobbyView = new AndroidLobbyView(this, this);
        } else {
            lobbyView.resumeDiscovery();
        }

        setContentView(lobbyView);
        hideSystemUI();
    }

    @Override
    public void onStartGame(String playerName, String host, int port, String roomCode) {
        isInGame = true;
        if (lobbyView != null) {
            lobbyView.stopDiscovery();
        }

        gameView = new AndroidGameView(this, playerName, host, port, roomCode, this::showLobby);
        setContentView(gameView);
        hideSystemUI();
    }

    @Override
    public void onBackPressed() {
        if (isInGame) {
            // Đang trong trận -> Hỏi người chơi có muốn về Sảnh Chờ không
            new AlertDialog.Builder(this)
                    .setTitle("Rời Đấu Trường")
                    .setMessage("Bạn có muốn rời trận chiến và quay lại Sảnh Chờ không?")
                    .setPositiveButton("RỜI PHÒNG", (dialog, which) -> showLobby())
                    .setNegativeButton("TIẾP TỤC ĐÁNH", null)
                    .show();
        } else {
            // Đang ở Sảnh Chờ -> Xác nhận thoát game
            new AlertDialog.Builder(this)
                    .setTitle("Thoát Game")
                    .setMessage("Bạn có chắc chắn muốn thoát khỏi UML Patterns Arena?")
                    .setPositiveButton("THOÁT", (dialog, which) -> finish())
                    .setNegativeButton("Ở LẠI", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (isInGame && gameView != null) {
            gameView.resumeGame();
        } else if (!isInGame && lobbyView != null) {
            lobbyView.resumeDiscovery();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isInGame && gameView != null) {
            gameView.pauseGame();
        } else if (!isInGame && lobbyView != null) {
            lobbyView.stopDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMulticastLock();
        if (lobbyView != null) {
            lobbyView.stopDiscovery();
        }
        if (gameView != null) {
            gameView.pauseGame();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
}
