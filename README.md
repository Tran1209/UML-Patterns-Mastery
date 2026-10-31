# 🎮 2D Action RPG Arena — Đồ Án Lập Trình Hướng Đối Tượng (HUCE 2026)

> **Đề tài Bài Tập Lớn OOP**: Thiết kế và cài đặt hệ thống trò chơi hành động nhập vai đa nền tảng (**PC Java Swing & Android Mobile**) hỗ trợ đấu mạng thời gian thực (**Hybrid LAN + Cloud Internet 4G Relay**) áp dụng nguyên lý **SOLID**, **GRASP**, và **GoF Design Patterns**.

---

## 👥 Thành Viên Nhóm 5

| STT | Họ và Tên | GitHub | Vai Trò & Phân hệ Phụ Trách |
| :-: | :--- | :--- | :--- |
| 1 | **Trung Kiên** *(Nhóm trưởng)* | [`KienEsto`](https://github.com/KienEsto) | **Team Leader**, Hạng mục 7 (Tối ưu Phím `WASD + Q/E/R` & Chuột) + Hạng mục 9 (Team Đối Địch & AI Bot) |
| 2 | **Duy Nam** | [`Tran1209`](https://github.com/Tran1209) | **Lead Architect**, Hạng mục 2 (Map Lớn đi bộ 5 phút `66,000px`) + Hạng mục 6 (4 Hệ Nhân Vật) |
| 3 | **Nguyễn Đức Huy** | [`duchuy462007`](https://github.com/duchuy462007) | **Network Engineer**, Hạng mục 1 (Sảnh Chờ Chọn Tướng/Team/Bot) + Hạng mục 8 (Vòng Bo Sinh Tồn) |
| 4 | **Đức Anh** | *(Thành viên)* | **2.5D Graphics Engineer**, Hạng mục 3 (Đồ họa 2.5D & Z-Sorting) + Hạng mục 5 (Tối ưu Model Nhân vật & Mobs) |
| 5 | **Huy Hoàng** | *(Thành viên)* | **Mobs & QA Engineer**, Hạng mục 4 (Thêm 6 loại Mobs đa dạng & Đồng bộ Server) + 15/15 Unit Tests |

> 📌 **BẢN KẾ HOẠCH CHI TIẾT 9 HẠNG MỤC NÂNG CẤP CHO TỪNG THÀNH VIÊN**:
> Xem chi tiết tại 👉 **[`Docs_BaoCao/1_QuanLy_Nhom_Va_NhatKy/04_KeHoach_PhanCong_9_HangMuc.md`](Docs_BaoCao/1_QuanLy_Nhom_Va_NhatKy/04_KeHoach_PhanCong_9_HangMuc.md)**

---

## 📂 Cấu Trúc Thư Mục Dự Án (Chuẩn Clean Architecture)

```text
E:\UML-Patterns-Mastery\
├── 📁 Src_Game/                 # Mã nguồn cốt lõi (100% Pure Java - Đa nền tảng)
│   ├── 📁 Core/                 # Thực thể (LivingEntity, Player, Monster, Vector2D, State Pattern)
│   ├── 📁 Combat/               # Hệ thống chiến đấu & 3 Kỹ năng AOV (ISkillAction, CombatEngine)
│   ├── 📁 Render/               # Đồ họa 2D & Hoạt ảnh LPC (Camera2D, Flyweight, Builder, Composite)
│   ├── 📁 Network/              # Đồng bộ mạng (Hybrid LAN UDP/TCP + MQTT Cloud Relay 4G, LERP)
│   ├── 📁 Server/               # Máy chủ Authoritative 20Hz (DedicatedGameServer, SpatialGridAoI)
│   ├── 📁 Persistence/          # Lưu trữ lịch sử đấu (IMatchRepository, JsonFileMatchRepository)
│   ├── 📁 UI/                   # Giao diện PC (GameWindow, LobbyPanel, GameCanvas, DesktopHudRenderer)
│   └── 📄 Main.java             # Điểm khởi chạy (Entry Point) cho PC / Server / Unit Test
│
├── 📁 Android_Game/             # Mã nguồn phiên bản Di động Android (60 FPS SurfaceView + Multi-Touch)
│   ├── 📁 src/com/huce/oop/game/# MainActivity, AndroidGameView (Kéo thả chiêu AOV), AndroidGameRenderer
│   └── 📄 AndroidManifest.xml   # Cấu hình ứng dụng Android
│
├── 📁 Tests/                    # Bộ kiểm thử tự động (15/15 Unit Tests kiểm chứng OOP & Patterns)
├── 📁 Docs_BaoCao/              # Toàn bộ Báo cáo Word/Markdown, Sơ đồ UML & Hướng dẫn Git cho nhóm
├── 📁 build/                    # Thư mục chứa file thực thi (.jar cho PC và .apk cho Android)
├── 📁 scripts/                  # Các kịch bản biên dịch (.bat) và công cụ đóng gói tự động
└── ⚙️ QuanLy_DuAn.bat           # MENU ĐIỀU KHIỂN TRUNG TÂM (Chạy Game, Test, Build PC & Android)
```

---

## 🚀 Hướng Dẫn Sử Dụng Nhanh Cho Các Thành Viên

Chỉ cần bấm đúp vào file **[`QuanLy_DuAn.bat`](file:///E:/UML-Patterns-Mastery/QuanLy_DuAn.bat)** ngay tại thư mục gốc để mở bảng điều khiển trung tâm:

1. **Phím `[1]`**: Chạy Game trên PC (Mở Sảnh chờ: Chơi Đơn / Tạo Phòng LAN-4G / Vào Phòng).
2. **Phím `[2]`**: Chạy Demo Multiplayer nhanh trên 1 máy PC (Tự động bật 1 Server 20Hz + 2 cửa sổ Nhân vật *Duy Nam* & *Trung Kiên*).
3. **Phím `[3]`**: Chạy 15/15 bài Kiểm thử Tự động (`GameUnitTestSuite`).
4. **Phím `[4]`**: Biên dịch lại mã nguồn Java ra file `build/UmlPatternsMasteryGame.jar`.
5. **Phím `[5]` & `[6]`**: Biên dịch file `build/UmlPatternsMasteryGame.apk` và cài thẳng vào điện thoại Android qua cáp USB.

---

## 🏛️ Các Mẫu Thiết Kế (Design Patterns) & Nguyên Lý SOLID Đã Áp Dụng

* **State Pattern**: Quản lý vòng đời trạng thái nhân vật (`IDLE`, `MOVING`, `ATTACKING`, `DEAD`) trong [`IPlayerState.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Core/IPlayerState.java) và [`Player.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Core/Player.java).
* **Strategy & Command Pattern**:
  * Công thức tính sát thương trừ giáp trong [`IDamageStrategy.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/IDamageStrategy.java).
  * Hệ thống 3 kỹ năng định hướng kéo thả kiểu Liên Quân (AOV) trong [`ISkillAction.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/ISkillAction.java) ([`DashStrikeSkill`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/DashStrikeSkill.java), [`FireWhirlwindSkill`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/FireWhirlwindSkill.java), [`MeteorUltimateSkill`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/MeteorUltimateSkill.java)), giúp [`CombatEngine.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/CombatEngine.java) tuân thủ tuyệt đối **Open/Closed Principle (OCP)**.
* **Observer Pattern**: Đồng bộ sự kiện sát thương/hạ gục lên thanh máu HUD qua [`IEntityEventListener.java`](file:///E:/UML-Patterns-Mastery/Src_Game/UI/IEntityEventListener.java) và [`HudObserver.java`](file:///E:/UML-Patterns-Mastery/Src_Game/UI/HudObserver.java).
* **Flyweight + Composite + Builder Pattern**: Tối ưu bộ nhớ đệm Sprite Sheet và lắp ráp lớp nhân vật 2D trong [`LpcSpriteSheet.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Render/LpcSpriteSheet.java), [`CompositeCharacterVisual.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Render/CompositeCharacterVisual.java), [`LpcCharacterBuilder.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Render/LpcCharacterBuilder.java).
* **Repository Pattern**: Tách biệt tầng lưu trữ lịch sử trận đấu trong [`IMatchRepository.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Persistence/IMatchRepository.java) và [`JsonFileMatchRepository.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Persistence/JsonFileMatchRepository.java).
