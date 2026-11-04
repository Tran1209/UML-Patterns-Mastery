---
description: Quy chuẩn kiến trúc, quy trình làm việc và tiêu chuẩn kỹ thuật bắt buộc cho đồ án OOP 2D Action RPG Arena (HUCE 2026)
---

# Bối cảnh Dự án Nhóm 1 — 2D Action RPG Arena (HUCE OOP 2026)

1. **Thông tin Nhóm & Kho chứa**:
   - Repository: `https://github.com/Tran1209/UML-Patterns-Mastery.git`
   - Nhóm trưởng (Team Leader): **Trung Kiên** (`KienEsto`, nhánh cá nhân: `Kien`).
   - Kiến trúc sư & Core/Android: **Duy Nam** (`Tran1209`, nhánh `main`).
   - Kỹ sư Mạng: **Nguyễn Đức Huy** (`nguyenduchuy20070406-oss`).

2. **Kiến trúc Phân tầng & Cấu trúc Thư mục**:
   - `Src_Game/` (Pure Java đa nền tảng — tuyệt đối không import `java.awt.*` vào các package `core`, `combat`, `network`, `server`, `persistence` để dùng chung với Android):
     - `Core/`: Domain Entities & Aggregate Roots (`LivingEntity`, `Player`, `Monster`, `Vector2D`), FSM (`IPlayerState`, `IdleState`, `MovingState`, `AttackingState`, `DeadState`), `WaveManager` (Creator pattern cho quái vật & tiến trình đợt).
     - `Combat/`: `CombatEngine` (điều phối qua `Map<Integer, ISkillAction>`), `ISkillAction` (Strategy/Command Pattern), `DashStrikeSkill` (Q - Chiêu 1), `FireWhirlwindSkill` (W - Chiêu 2), `MeteorUltimateSkill` (E/R - Chiêu 3), `SpatialGridAoI` (Spatial Partitioning), `CombatEffects`.
     - `Render/`: `Camera2D` (MOBA dimetric follow), `Environment25DRenderer` (kết xuất địa hình, sông ngòi, thềm đá, cây cối, trụ đá), `CombatVisualRenderer` (hiệu ứng chiêu thức 2.5D), `Renderable25D` (Abstraction cho Painter's Algorithm Z-Depth sorting), `LpcSpriteSheet` (Flyweight), `CompositeCharacterVisual` (Composite), `LpcCharacterBuilder` (Builder).
     - `Network/` & `Server/`: `DedicatedGameServer` (20Hz Authoritative), `NetworkGameClient`, `SnapshotInterpolator` (LERP Entity Interpolation), `LanDiscovery` (UDP), `MiniMqttClient` & `CloudServerBridge` (Relay).
     - `UI/`: `GameWindow`, `LobbyPanel`, `GameCanvas`, `DesktopHudRenderer`, `NavigationMarkerManager` (hiệu ứng ping click chuột LOL), `HudObserver` (Observer Pattern).
   - `Android_Game/`: Phiên bản di động Android (`MainActivity`, `AndroidGameView`, `AndroidGameRenderer`).
   - `Tests/`: `GameUnitTestSuite.java` (Automated Unit Test Suite).
   - `Docs_BaoCao/`: Toàn bộ 7 chương báo cáo OOP, `NhatKy_DuAn.md`, và `Huong_Dan_Sua_Chua_Va_Lam_Chu_Du_An.md`.

3. **Lệnh Biên dịch & Kiểm thử Chuẩn**:
   - Biên dịch và chạy Unit Test Suite:
     ```powershell
     javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path Src_Game, Tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })
     java -cp build\classes tests.GameUnitTestSuite
     ```
   - Đóng gói file JAR:
     ```powershell
     cmd.exe /c "echo Main-Class: Main> build\manifest.txt && jar cfm build\UmlPatternsMasteryGame.jar build\manifest.txt -C build\classes ."
     ```
   - Menu điều hành: `QuanLy_DuAn.bat` (Lưu ý: Luôn giữ chuẩn xuống dòng `CRLF` cho mọi file `.bat`).

---

# BỘ TIÊU CHUẨN KỸ THUẬT & QUY TRÌNH BẮT BUỘC (MANDATORY STANDARDS)

### 0. QUY TRÌNH LÀM VIỆC BẮT BUỘC (Workflow)
- **Hiểu trước, code sau**: Với tác vụ không tầm thường (>1 file hoặc >50 dòng), trước tiên viết ngắn gọn: mục tiêu, giả định, phạm vi ảnh hưởng, kế hoạch các bước. Chờ xác nhận nếu có điểm mơ hồ ảnh hưởng thiết kế.
- **Đọc code hiện có trước khi sửa**: Tuân theo convention, cấu trúc thư mục, tên gọi đã có. Không tự ý đổi framework/thư viện/phong cách.
- **Lặp nhỏ (Iterative)**: Chia việc thành các bước nhỏ, mỗi bước chạy được và kiểm thử được. Không sinh một "cục" code khổng lồ.
- **Thay đổi tối thiểu**: Chỉ sửa những gì cần thiết. Không refactor lan man ngoài phạm vi yêu cầu; nếu phát hiện vấn đề khác, ghi chú lại thay vì sửa ngầm.
- **Tự kiểm tra trước khi báo xong**: Chạy build/lint/test, đọc lại diff, đối chiếu với yêu cầu. Báo cáo cuối gồm: đã làm gì, file nào đổi, cách kiểm chứng, rủi ro còn lại.
- **Không bịa**: Không tự tạo API/hàm/thư viện không tồn tại. Không chắc thì nói "không chắc" và kiểm tra lại bằng cách đọc code/tài liệu.
- **Đồng bộ Danh mục Class bắt buộc**: Khi thêm mới, thay đổi hoặc sửa đổi bất kỳ class/interface/enum nào trong thư mục `Src_Game/`, BẮT BUỘC phải cập nhật ngay vào tệp [`DANH_SACH_CLASS_SRC_GAME.md`](file:///d:/KiênEsto/DELL/Downloads/UML-Patterns-Mastery-Kien/DANH_SACH_CLASS_SRC_GAME.md) (tên class, tệp chứa, quan hệ kế thừa/interface, chức năng nghiệp vụ và mẫu thiết kế liên quan).

### 1. PHÂN TÍCH & MÔ HÌNH HÓA NGHIỆP VỤ (Larman + Vernon)
- Bắt đầu từ Use Case / User Story: ai (actor) làm gì, mục tiêu gì, kết quả thành công và các luồng ngoại lệ.
- Với tính năng mới, phác thảo Domain Model (khái niệm, thuộc tính, quan hệ) trước khi viết lớp. Tên lớp lấy từ ngôn ngữ nghiệp vụ, không phải ngôn ngữ kỹ thuật (Low Representational Gap).
- Dùng **Ubiquitous Language**: một khái niệm = một tên duy nhất, thống nhất trong code, test, tài liệu, commit. Cấm đặt tên mơ hồ (`Manager`, `Helper`, `Util`, `Data`, `Info`) trừ khi thật sự đúng nghĩa và là Pure Fabrication có chủ đích.
- Hệ thống lớn: chia Bounded Context rõ ràng. Mỗi context có model riêng; giao tiếp giữa các context qua interface/event/Anti-Corruption Layer, không chia sẻ trực tiếp entity nội bộ.
- Với thao tác phức tạp, mô tả hợp đồng (Operation Contract): tiền điều kiện, hậu điều kiện, lỗi có thể xảy ra.

### 2. THIẾT KẾ ĐỐI TƯỢNG & PHÂN CHIA TRÁCH NHIỆM (GRASP + SOLID + GoF)
- **Nguyên tắc GRASP**:
  * **Information Expert**: Giao trách nhiệm cho lớp nắm dữ liệu cần thiết (ví dụ: `Player` tự tính toán điều hướng di chuyển, `LivingEntity` tự trừ máu theo giáp).
  * **Creator**: Lớp chứa/tổng hợp/sử dụng chặt chẽ đối tượng thì mới tạo nó (ví dụ: `WaveManager` tạo `Monster`).
  * **Controller**: Lớp điều phối use case, KHÔNG chứa logic nghiệp vụ nặng.
  * **Low Coupling / High Cohesion**: Tiêu chí ưu tiên hàng đầu khi cân nhắc thiết kế.
  * **Polymorphism**: Thay cho chuỗi if/switch theo kiểu type.
  * **Pure Fabrication / Indirection**: Tạo lớp trung gian thuần kỹ thuật (`SpatialGridAoI`, `SnapshotInterpolator`) để giữ domain sạch.
  * **Protected Variations**: Bọc điểm có khả năng biến đổi sau interface ổn định.
- **SOLID & Quy tắc Code**:
  * **SRP**: Một lớp/hàm chỉ có một lý do để thay đổi.
  * **DIP**: Phụ thuộc vào abstraction, không phụ thuộc cài đặt cụ thể. Inject dependency qua constructor.
  * Ưu tiên composition hơn inheritance. Kế thừa chỉ cho quan hệ "is-a" thật sự và tối đa 2 tầng.
  * Giới hạn kích thước: Hàm < 30 dòng, ≤ 3-4 tham số, một mức trừu tượng. Lớp < 300 dòng. File < 500 dòng. Vượt thì tách.
  * Tránh: God class, global mutable state, magic number/string, copy-paste, deep nesting (> 3 tầng, dùng early return).
- **Mẫu Thiết kế (GoF)**:
  * Nhiều thuật toán hoán đổi được → **Strategy** (`IDamageStrategy`).
  * Khởi tạo đối tượng phức tạp/đa dạng → **Factory / Builder** (`LpcCharacterBuilder`).
  * Tích hợp API ngoài, đổi nhà cung cấp → **Adapter + Protected Variations**.
  * Thông báo thay đổi 1-nhiều → **Observer / Event** (`IEntityEventListener`).
  * Hành động cần undo, queue, log, replay → **Command** (`ISkillAction`).
  * Hành vi theo trạng thái → **State** (`IPlayerState`).
  * Cấu trúc cây/đối tượng phức hợp → **Composite** (`CompositeCharacterVisual`).
  * Đơn giản hóa hệ thống con phức tạp → **Facade**.
  * Nhiều đối tượng cùng loại khác dữ liệu → **Type Object / Flyweight** (`LpcSpriteSheet`).
  * Cấm over-engineering: Không thêm pattern "phòng khi cần sau này" (YAGNI). Ba lần lặp lại mới tách (Rule of Three). Mỗi pattern dùng phải có comment 1 dòng nêu lý do.

### 3. KIẾN TRÚC PHÂN TẦNG (Fowler + Larman + Vernon)
- **Tầng bắt buộc**: `Presentation` → `Application/Service` → `Domain` → `Infrastructure`. Phụ thuộc chỉ đi một chiều hướng vào Domain. Domain KHÔNG import UI, DB, HTTP, framework, network.
- Áp dụng nguyên tắc Hexagonal (Ports & Adapters): Domain định nghĩa port (interface), hạ tầng cung cấp adapter. Có thể chạy domain bằng test mà không cần DB hay mạng.
- Cấu hình, URL, tham số giới hạn: đưa vào hằng số cấu hình, không hard-code.

### 4. XỬ LÝ LỖI, VALIDATION, BẢO MẬT (Bắt buộc)
- Validate ở ranh giới hệ thống (input người dùng, gói tin mạng, file I/O). Bên trong domain dùng invariant/guard rõ ràng; fail fast.
- Không nuốt lỗi (nghiêm cấm catch rỗng). Lỗi phải được xử lý hoặc bọc kèm ngữ cảnh rõ ràng.
- Phân biệt lỗi nghiệp vụ (dự kiến) với lỗi hệ thống (bất ngờ). Log có cấu trúc, không log dữ liệu nhạy cảm.

### 5. LƯU TRỮ & NHẤT QUÁN DỮ LIỆU (Fowler + Vernon)
- Truy cập dữ liệu qua `Repository` (giao diện hướng domain như `IMatchRepository`); cài đặt bằng Infrastructure adapter (`JsonFileMatchRepository`).
- Entity có định danh (`id`); Value Object bất biến (`Vector2D`), so sánh theo giá trị. Tránh "primitive obsession".

### 6. KIỂM THỬ & CHẤT LƯỢNG (Bắt buộc)
- Mọi logic nghiệp vụ mới/sửa phải đi kèm test. Bug fix → viết test tái hiện bug trước.
- Test đặt tên theo hành vi: `should_<kết_quả>_when_<điều_kiện>` hoặc rõ mục tiêu nghiệp vụ; cấu trúc Arrange-Act-Assert; mỗi test một ý.
- Không để test phụ thuộc thứ tự chạy, thời gian thực, hay mạng thật.
- Commit nhỏ, thông điệp theo Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`), mô tả "tại sao" chứ không chỉ "cái gì".

### 7. GAME, THỜI GIAN THỰC & MULTIPLAYER (Nystrom + Glazer/Madhav)
- **Vòng lặp & Cập nhật**: Dùng Fixed Timestep cho logic/vật lý (`GameEngineLoop`, 60 FPS) và tách render. Mọi cập nhật qua Update Method; không dùng sleep/timer rải rác trong gameplay loop.
- **Hiệu năng**: Không cấp phát bộ nhớ tùy tiện trong vòng lặp nóng. Dùng Object Pool cho đạn/hiệu ứng nếu cần. Dùng Spatial Partitioning (`SpatialGridAoI`) cho va chạm và vùng lân cận.
- **Mạng**: Mặc định Authoritative Server (`DedicatedGameServer` 20Hz): Server là nguồn sự thật; Client chỉ gửi input (`PlayerInputPayload`). Client-side prediction & Entity Interpolation (`SnapshotInterpolator` LERP) để bù độ trễ. Validate mọi input phía server.

### 8. QUY TẮC GIAO TIẾP CỦA AGENT
- Trả lời bằng tiếng Việt; giữ nguyên thuật ngữ kỹ thuật tiếng Anh; code, tên biến, commit viết bằng tiếng Anh.
- Ngắn gọn, đi thẳng vào kết quả. Dùng danh sách/bảng khi có nhiều phương án; nêu đánh đổi (trade-off) và đề xuất 1 phương án khuyến nghị.
- Gặp yêu cầu mơ hồ ảnh hưởng thiết kế → hỏi tối đa vài câu then chốt, rồi mới làm. Việc nhỏ, rõ ràng → làm luôn, ghi giả định.
- Thao tác rủi ro (xóa file/dữ liệu, đổi schema, đổi public API, chạy lệnh phá hủy, cài thư viện lớn) → xin xác nhận trước.
- Cuối mỗi tác vụ báo cáo: (1) Đã làm gì (2) File thay đổi (3) Cách chạy/kiểm chứng (4) Rủi ro/việc còn lại.

### 9. CHECKLIST TRƯỚC KHI BÁO "HOÀN THÀNH"
- [x] Đúng yêu cầu và use case; đã xét luồng ngoại lệ
- [x] Tên gọi theo ngôn ngữ nghiệp vụ; không có magic value
- [x] Domain không phụ thuộc hạ tầng; phụ thuộc đi một chiều
- [x] Trách nhiệm đặt đúng chỗ (GRASP); không over-engineering
- [x] Lỗi được xử lý; input được validate; không lộ secret
- [x] Có test cho logic mới; test + lint + build đều xanh
- [x] Giao dịch/nhất quán dữ liệu đúng ranh giới Aggregate
- [x] (Game/Realtime) không cấp phát trong vòng lặp nóng; đã đo hiệu năng; server là nguồn sự thật
- [x] Đã đồng bộ mọi thay đổi/thêm mới class trong `Src_Game/` vào tệp `DANH_SACH_CLASS_SRC_GAME.md`
- [x] Diff gọn, không thay đổi ngoài phạm vi; tài liệu/ADR đã cập nhật nếu cần
