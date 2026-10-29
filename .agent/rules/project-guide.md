---
description: Quy chuẩn kiến trúc, lệnh biên dịch và quy trình làm việc nhóm cho dự án 2D Action RPG Arena (HUCE OOP 2026)
---

# Bối cảnh Dự án Nhóm 5 — 2D Action RPG Arena (HUCE OOP 2026)

1. **Thông tin Nhóm & Kho chứa**:
   - Repository: `https://github.com/Tran1209/UML-Patterns-Mastery.git`
   - Nhóm trưởng (Team Leader): **Trung Kiên** (`KienEsto`, nhánh `Kien` / `main`).
   - Kiến trúc sư & Core/Android: **Duy Nam** (`Tran1209`, nhánh `main`).
   - Kỹ sư Mạng: **Nguyễn Đức Huy** (`nguyenduchuy20070406-oss`).

2. **Kiến trúc Mã nguồn (Clean Architecture & SOLID)**:
   - `Src_Game/` (Pure Java đa nền tảng — tuyệt đối không import `java.awt.*` vào các package `core`, `combat`, `network`, `server`, `persistence` để dùng chung với Android):
     - `Core/`: `LivingEntity`, `Player`, `Monster`, `Vector2D`, `IPlayerState` (State Pattern).
     - `Combat/`: `CombatEngine` (điều phối qua `Map<Integer, ISkillAction>`), `ISkillAction` (Strategy/Command Pattern), `DashStrikeSkill` (Chiêu 1), `FireWhirlwindSkill` (Chiêu 2), `MeteorUltimateSkill` (Chiêu 3), `CombatEffects`.
     - `Render/`: `Camera2D` (MOBA center-follow), `LpcSpriteSheet` (Flyweight), `CompositeCharacterVisual` (Composite), `LpcCharacterBuilder` (Builder).
     - `Network/` & `Server/`: `DedicatedGameServer` (20Hz Authoritative), `NetworkGameClient`, `SnapshotInterpolator` (LERP), `LanDiscovery` (UDP), `MiniMqttClient` & `CloudServerBridge` (4G Cloud Internet Relay).
     - `UI/`: `GameWindow`, `LobbyPanel`, `GameCanvas`, `DesktopHudRenderer`, `HudObserver` (Observer Pattern).
   - `Android_Game/`: Phiên bản di động Android (`MainActivity`, `AndroidGameView`, `AndroidGameRenderer`).
   - `Tests/`: `GameUnitTestSuite.java` (15/15 Unit Tests).
   - `Docs_BaoCao/`: Toàn bộ 7 chương báo cáo OOP, `NhatKy_DuAn.md`, và `Huong_Dan_Sua_Chua_Va_Lam_Chu_Du_An.md`.

3. **Các Lệnh Biên dịch, Kiểm thử & Đồng bộ Chuẩn**:
   - Biên dịch bản PC (`.jar`) và chạy 15/15 Unit Tests:
     ```powershell
     javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })
     jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes .
     java -jar build\UmlPatternsMasteryGame.jar --test
     ```
   - Chạy Menu điều khiển dự án: `QuanLy_DuAn.bat` (Lưu ý luôn giữ chuẩn xuống dòng `CRLF` cho mọi file `.bat`).
