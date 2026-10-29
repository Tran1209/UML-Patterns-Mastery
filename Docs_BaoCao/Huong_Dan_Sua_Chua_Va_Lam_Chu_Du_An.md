# 📘 CẨM NANG LÀM CHỦ, SỬA CHỮA & MỞ RỘNG DỰ ÁN (DÀNH CHO NHÓM TRƯỞNG KIÊN & CÁC THÀNH VIÊN)

> **Tài liệu này giúp Nhóm trưởng Trung Kiên (`KienEsto`) và các thành viên trong nhóm làm được 100% mọi thao tác giống hệt trên máy khởi tạo**: từ tải dự án về máy, chạy thử Server/Client, chỉnh sửa thông số Tướng/Chiêu thức/Bản đồ, thêm Chiêu thức mới theo chuẩn SOLID, biên dịch lại file `.jar` / `.apk`, cho đến đẩy code lên GitHub để ghi nhận lịch sử đóng góp.

---

## PHẦN 1: CÁCH LẤY DỰ ÁN VỀ MÁY CỦA KIÊN (LẦN ĐẦU TIÊN)

1. **Chấp nhận lời mời vào kho chứa Private**:
   * Mở trình duyệt (đã đăng nhập tài khoản GitHub `KienEsto` hoặc `nguyenduchuy20070406-oss`) và bấm vào:
     👉 **https://github.com/Tran1209/UML-Patterns-Mastery/invitations**
   * Bấm nút màu xanh **`Accept invitation`**.
2. **Tải toàn bộ dự án về máy tính**:
   * Mở cửa sổ **CMD** hoặc **PowerShell** tại ổ đĩa muốn lưu (ví dụ ổ `D:\` hoặc `E:\`) và chạy lệnh:
     ```bash
     git clone https://github.com/Tran1209/UML-Patterns-Mastery.git
     cd UML-Patterns-Mastery
     ```
3. **Tải sẵn 2 file chạy ngay (`.jar` và `.apk`) từ mục Releases** *(nếu chưa muốn biên dịch ngay)*:
   * Vào link: **https://github.com/Tran1209/UML-Patterns-Mastery/releases/tag/v1.0.0**
   * Tải `UmlPatternsMasteryGame.jar` bỏ vào thư mục `build/` trên máy.
4. **Mở Bảng Điều Khiển Trung Tâm**:
   * Bấm đúp vào file **`QuanLy_DuAn.bat`** ngay ngoài thư mục gốc.
   * Gõ phím **`[4]`** để tự động biên dịch toàn bộ code Java ra file `build\UmlPatternsMasteryGame.jar`.
   * Gõ phím **`[1]`** để mở Menu Game hoặc phím **`[2]`** để bật cùng lúc 1 Server + 2 cửa sổ Nhân vật (*Duy Nam* & *Trung Kiên*) test đấu mạng ngay trên 1 máy!

---

## PHẦN 2: BẢN ĐỒ "MUỐN SỬA CHỖ NÀO THÌ MỞ FILE NÀO?" (CHEAT SHEET SỬA CODE)

Khi Kiên hoặc các bạn trong nhóm muốn tự tay chỉnh sửa game để hiểu sâu hoặc biểu diễn trước giảng viên khi bảo vệ đồ án, chỉ cần tra bảng dưới đây:

### 1. Muốn chỉnh sửa Máu, Giáp, Sát thương, Tốc độ chạy hoặc Thời gian Hồi chiêu (Cooldown)
* **Mở file**: `Src_Game/Core/Player.java`
* **Chỗ cần sửa**:
  * **Máu & Giáp khởi tạo**: Xem hàm khởi tạo `public Player(...)` (khoảng dòng `35-50`).
  * **Thời gian hồi 3 chiêu thức (Cooldown)**: Xem mảng `skillMaxCooldowns = {0.0, 4.0, 6.0, 10.0}` (Chiêu 1 hồi `4 giây`, Chiêu 2 hồi `6 giây`, Chiêu 3 Ultimate hồi `10 giây`). Muốn test spam chiêu không hồi chiêu thì đổi thành `{0.0, 0.5, 0.5, 1.0}`!

---

### 2. Muốn chỉnh sửa 3 Chiêu Thức Kéo Thả (Hoặc tạo thêm Chiêu 4 mới)
Nhờ đã tái cấu trúc theo **Strategy / Command Pattern (`ISkillAction`)**, mỗi chiêu thức nằm gọn trong 1 file riêng biệt dưới thư mục `Src_Game/Combat/`:

| Chiêu thức | File cần mở | Các thông số có thể sửa ngay trong file |
| :--- | :--- | :--- |
| **Chiêu 1: ⚡ Xung Kích** | `Src_Game/Combat/DashStrikeSkill.java` | • `getMaxRange()`: Độ dài mũi tên lướt (`220.0` px)<br>• `getAoeRadius()`: Độ rộng đường lướt (`65.0` px)<br>• Hệ số sát thương: `player.getAttackPower() * 2 + 15` |
| **Chiêu 2: 🔥 Vòng Xoáy Lửa** | `Src_Game/Combat/FireWhirlwindSkill.java` | • `getMaxRange()`: Tầm kéo thả tối đa (`260.0` px)<br>• `getAoeRadius()`: Bán kính nổ vòng lửa (`120.0` px)<br>• Lượng hồi máu hút huyết: `player.heal(25)` |
| **Chiêu 3: ☄️ Thiên Phạt Ultimate** | `Src_Game/Combat/MeteorUltimateSkill.java` | • `getMaxRange()`: Tầm thả thiên thạch (`320.0` px)<br>• `getAoeRadius()`: Bán kính vụ nổ lớn (`180.0` px)<br>• Sát thương chuẩn + Khoảng cách đẩy lùi quái (`pushDist = 55.0`) |

> 💡 **Câu hỏi bảo vệ đồ án hay gặp**: *"Nếu thầy yêu cầu em thêm Chiêu thứ 4 (ví dụ: Đóng Băng) ngay tại chỗ thì em sửa những đâu?"*
> * **Trả lời chuẩn điểm A+**: Nhờ áp dụng **Open/Closed Principle (SOLID)** và **Strategy Pattern**, em **không cần sửa bất kỳ dòng logic nào của 3 chiêu cũ** trong `CombatEngine`. Em chỉ cần tạo class `IceFreezeSkill implements ISkillAction` rồi gọi `registerSkill(new IceFreezeSkill())` trong `CombatEngine` là hệ thống tự nhận diện tầm ngắm, bán kính AoE và thực thi chiêu mới!

---

### 3. Muốn chỉnh số lượng Quái vật (Goblin, Skeleton, Boss Orc) hoặc Kích thước Bản đồ
* **Mở file**: `Src_Game/Core/GameWorld.java` (cho chế độ Chơi Đơn) và `Src_Game/Server/DedicatedGameServer.java` (cho chế độ Đấu Mạng).
* **Chỗ cần sửa**:
  * Kích thước bản đồ: `WORLD_WIDTH = 1600`, `WORLD_HEIGHT = 1200`.
  * Hàm `spawnInitialMonsters()`: Đang sinh ra các quái vật `Goblin Trinh Sát`, `Bộ Xương Chiến Binh`, `Thủ Lĩnh Orc`. Bạn có thể đổi tên quái, tăng máu hoặc nhân đôi số lượng quái tại đây.

---

### 4. Muốn chỉnh sửa Giao diện Nút bấm, Thanh Máu, Vòng Ngắm Kéo Thả
* **Trên máy tính (PC)**: Mở file `Src_Game/UI/DesktopHudRenderer.java` (chứa toàn bộ hàm vẽ thanh máu góc trái, 4 ô kỹ năng bên dưới, và vòng tròn ngắm kéo thả chuột `drawSkillTargetIndicator`).
* **Trên điện thoại (Android)**: Mở file `Android_Game/src/com/huce/oop/game/AndroidGameRenderer.java` (hàm `renderSkillButtons` và `renderSkillTargetIndicator`).

---

## PHẦN 3: QUY TRÌNH SỬA CODE & ĐẨY LÊN GITHUB ĐỂ LƯU DẤU ẤN CỦA KIÊN

Để bảng lịch sử GitHub (`Insights -> Contributors` và `Commits`) hiển thị rõ tài khoản của **Nhóm trưởng Kiên (`KienEsto`)** và các thành viên khác phục vụ thầy cô kiểm tra:

1. **Bước 1**: Mở 1 file bất kỳ (ví dụ `Docs_BaoCao/NhatKy_DuAn.md` để điền Mã số sinh viên của Kiên, hoặc chỉnh thông số trong `DashStrikeSkill.java`).
2. **Bước 2**: Bấm đúp vào **`QuanLy_DuAn.bat`**:
   * Chọn phím **`[4]`** để biên dịch kiểm tra xem code có lỗi cú pháp không.
   * Chọn phím **`[3]`** để chạy **15/15 Unit Tests** đảm bảo 100% PASS.
3. **Bước 3**: Trong **`QuanLy_DuAn.bat`**, gõ phím **`[7]` (Đồng bộ Code lên GitHub)**:
   * Nhập nội dung commit, ví dụ: `feat(combat): Kien tinh chinh sat thuong chieu 1 va cap nhat MSSV`.
   * Nhấn `Enter` — Code sẽ tự động được đẩy lên GitHub dưới chính tên tài khoản `KienEsto` của Kiên!
