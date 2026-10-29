# CẨM NANG HƯỚNG DẪN SỬ DỤNG GIT & GITHUB CHO NHÓM 5 NGƯỜI
> **Dành riêng cho Đồ án Game OOP & Báo cáo Đại học**  
> *(Gửi file này cho 4 thành viên trong nhóm đọc để ngày mai làm việc nhịp nhàng)*

---

## 🎯 NGUYÊN TẮC VÀNG ĐỂ KHÔNG BAO GIỜ BỊ XUNG ĐỘT CODE (MERGE CONFLICT)

Lý do lớn nhất khiến sinh viên sợ Git là bị **xung đột code (Conflict)** khi 2 người cùng sửa chung 1 file. 
Để **triệt tiêu 100% nguy cơ conflict**, nhóm chúng ta áp dụng 2 nguyên tắc sau:

1. **Nhánh `main` là "Bất khả xâm phạm"**:
   * Không ai được phép trực tiếp sửa code trên nhánh `main`. Nhánh `main` chỉ chứa phiên bản đã chạy ngon lành.
   * Mỗi thành viên chỉ làm việc trên **nhánh riêng của mình**.
2. **Quy tắc phân vùng thư mục (Mỗi người một lãnh thổ)**:
   * **TV 1 (Leader)**: Code tại thư mục `Src_Game/Core/`
   * **TV 2 (Network)**: Code tại thư mục `Src_Game/Server/` và `Src_Game/Network/`
   * **TV 3 (Combat)**: Code tại thư mục `Src_Game/Combat/`
   * **TV 4 (UI)**: Code tại thư mục `Src_Game/UI/`
   * **TV 5 (Data & Test)**: Code tại thư mục `Src_Game/Persistence/` và `Tests/`
   $\rightarrow$ *Vì mỗi người làm ở một thư mục riêng biệt, bạn commit thoải mái mà không bao giờ sợ đè lên code của bạn khác!*

---

## 🚀 BƯỚC 1: DÀNH CHO TRƯỞNG NHÓM (KHỞI TẠO REPO)

1. Mở trình duyệt, đăng nhập vào tài khoản GitHub của bạn (`Tran1209`).
2. Bấm nút **New Repository** (màu xanh lá):
   * Repository name: `UML-Patterns-Mastery` (hoặc `OOP-Multiplayer-Game`).
   * Chọn chế độ: **Public** (để giảng viên dễ xem) hoặc **Private** (nếu muốn bảo mật nội bộ).
   * **Lưu ý quan trọng**: Không tích chọn *Add a README file*, không tích chọn *.gitignore* (vì máy bạn đã có sẵn hết rồi).
   * Bấm **Create repository**.
3. Mời 4 thành viên vào dự án:
   * Vào **Settings** của Repo $\rightarrow$ chọn mục **Collaborators** ở cột bên trái $\rightarrow$ bấm **Add people** $\rightarrow$ gõ username GitHub hoặc email của 4 bạn để gửi lời mời.

---

## 💻 BƯỚC 2: DÀNH CHO 4 THÀNH VIÊN (KHI BẮT ĐẦU DỰ ÁN)

### 2.1. Chấp nhận lời mời & Tải code về máy (Clone)
1. Kiểm tra Email hoặc vào thông báo trên trang GitHub để bấm **Accept Invitation** (Chấp nhận lời mời của Trưởng nhóm).
2. Mở terminal (Git Bash, Command Prompt hoặc Cursor/VS Code) trên máy bạn:
   ```bash
   git clone https://github.com/Tran1209/UML-Patterns-Mastery.git
   cd UML-Patterns-Mastery
   ```

### 2.2. Tạo nhánh riêng của mình (BẮT BUỘC)
Sau khi clone về, mỗi bạn lập tức tạo một nhánh riêng theo vai trò:
* **TV 2 (Mạng)** gõ lệnh:
  ```bash
  git checkout -b feature/network-server
  ```
* **TV 3 (Combat)** gõ lệnh:
  ```bash
  git checkout -b feature/combat-system
  ```
* **TV 4 (UI)** gõ lệnh:
  ```bash
  git checkout -b feature/client-ui
  ```
* **TV 5 (Data & Test)** gõ lệnh:
  ```bash
  git checkout -b feature/data-testing
  ```

---

## 🔄 BƯỚC 3: QUY TRÌNH LÀM VIỆC HÀNG NGÀY (CỰC KỲ ĐƠN GIẢN)

Mỗi khi bạn hoàn thành một đoạn code hoặc viết xong một trang báo cáo, chỉ cần chạy đúng **3 câu lệnh thần thánh**:

1. **Lưu các file đã sửa**:
   ```bash
   git add .
   ```
2. **Ghi chú nội dung đã làm (Commit)**:
   ```bash
   git commit -m "feat: them class tinh toan sat thuong damage strategy"
   ```
3. **Đẩy lên GitHub**:
   ```bash
   git push origin <ten-nhanh-cua-ban>
   ```
   *(Ví dụ: `git push origin feature/combat-system`)*

---

## 📥 BƯỚC 4: LẤY CODE MỚI NHẤT CỦA CẢ NHÓM VỀ MÁY MÌNH
Trước khi bắt đầu ngày làm việc mới, để lấy những tính năng mà bạn bè đã làm về máy mình:

```bash
# 1. Chuyển về nhánh main
git checkout main

# 2. Kéo code mới nhất từ GitHub về
git pull origin main

# 3. Quay lại nhánh của mình và cập nhật code mới vào nhánh mình
git checkout <ten-nhanh-cua-ban>
git merge main
```

---

## 📋 DANH SÁCH CHECKLIST CHO CUỘC HỌP NGÀY MAI (18/10)
- [ ] Cả 5 người đã có tài khoản GitHub.
- [ ] Trưởng nhóm đã bấm tạo repo và add 4 thành viên vào Collaborators.
- [ ] 4 thành viên đã clone repo về máy và chuyển sang nhánh riêng thành công.
- [ ] Mở file `Docs_BaoCao/NhatKy_DuAn.md` để điền họ tên, mã sinh viên và username GitHub của 5 người.
- [ ] Thống nhất ý tưởng Game để chốt Phần 1 Báo cáo!
