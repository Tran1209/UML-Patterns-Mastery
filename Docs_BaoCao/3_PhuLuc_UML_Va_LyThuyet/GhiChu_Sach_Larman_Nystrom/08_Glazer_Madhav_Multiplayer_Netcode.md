---
aliases: [Multiplayer Game Programming, Glazer Madhav, Netcode, LERP, Client Prediction, Jitter]
tags: [multiplayer, netcode, authoritative-server, lerp, jitter, glazer-madhav, serialization, replication]
created: 2026-10-19
updated: 2026-10-20
pillar: 3
raw_pdf: "00_Raw_PDF/Multiplayer Game Programming.pdf"
extracted_chapters: "00_Raw_PDF/_Knowledge_Base_Glazer_Madhav/chapters_extracted/"
---

# 08. Joshua Glazer & Sanjay Madhav — Multiplayer Game Programming (Kỹ Thuật Mạng Real-Time)

> **Tác giả**: Joshua Glazer & Sanjay Madhav (Giảng viên Kỹ nghệ Game tại University of Southern California - USC; Cựu kỹ sư mạng tại Electronic Arts)  
> **Nguồn trích xuất gốc**: `00_Raw_PDF/Multiplayer Game Programming.pdf` (385 trang)  
> **Thư mục chương Markdown bóc tách**: `00_Raw_PDF/_Knowledge_Base_Glazer_Madhav/chapters_extracted/`  
> **Vai trò trong Đồ án**: Cung cấp cơ sở lý thuyết toán học và giải thuật mạng thời gian thực chuyên sâu; giải quyết triệt để lỗi giật hình (Micro-stutter / Jitter 20Hz vs 60fps), kiến trúc Server Authoritative và tuần tự hóa gói tin.

---

## 1. TẠI SAO ĐỒ ÁN GAME MULTIPLAYER BẮT BUỘC PHẢI CÓ GLAZER & MADHAV?

Giáo trình của Craig Larman chỉ nghiên cứu hệ thống phần mềm đơn máy hoặc giao dịch dạng Request-Response (độ trễ hàng giây không thành vấn đề). Khi chuyển sang bài toán Đấu trường Trực tuyến nhiều người chơi:
1. **Lỗi nghiêm trọng 1: Tin tưởng Client (Client-Authoritative)**: Để máy khách tự tính toán tọa độ và phán quyết sát thương $\longrightarrow$ Bị hack tốc độ (Speed Hack), hack xuyên tường (Teleport Hack).
2. **Lỗi nghiêm trọng 2: Lệch pha tần số (Frequency Mismatch)**: Máy chủ phát trạng thái 20 lần/giây, màn hình vẽ 60 lần/giây $\longrightarrow$ Nhân vật đối thủ bị giật cục liên tục (*Micro-stutter*).
3. **Lỗi nghiêm trọng 3: Mạng chập chờn (Network Jitter & Packet Loss)**: Gói tin đến không đều đặn khiến thời gian hiển thị bị biến dạng.

Cuốn sách của Glazer & Madhav cung cấp các giải thuật chuẩn mực công nghiệp để giải quyết trọn vẹn các vấn đề này.

---

## 2. CÁC TRỤ CỘT KỸ THUẬT TỪ SÁCH ÁP DỤNG TRỰC TIẾP VÀO MÃ NGUỒN

### 2.1. Kiến Trúc Máy Chủ Authoritative (Chương 6: Topologies & Chương 10: Security)
* **Triết lý**: *"Never trust the client"* (Trang 289 – Input Validation & Trang 290 – Cheat Detection).
* **Luồng xử lý chuẩn mực**:
  1. Client gửi **Ý định hành động** (`InputCommandPacket` chứa $dx, dy$, số thứ tự `sequenceNumber` và `timestamp`).
  2. Server kiểm tra tính hợp lệ của vector vận tốc (kẹp giá trị tốc độ tối đa).
  3. Server là **Nguồn Sự Thật Duy Nhất (Single Source of Truth)** phán quyết va chạm và trừ máu.

---

### 2.2. Đo Lường & Bản Chất Của Jitter (Chương 7: Latency, Jitter, and Reliability — Trang 218 – 251)
* **Khái niệm Jitter (Trang 223)**: Jitter là **sự biến thiên của độ trễ mạng theo thời gian** (Variance in Packet Arrival Times). Nếu một gói tin mất 40ms, gói kế tiếp mất 90ms, gói thứ ba mất 30ms, khoảng cách thời gian giữa các gói tin không còn là 50ms nữa.
* **Hậu quả**: Nếu Client kết xuất dữ liệu ngay khi vừa nhận được gói tin, người chơi sẽ nhìn thấy chuyển động co giật, lúc nhanh lúc chậm.
* **Mô phỏng điều kiện thực tế (Trang 247)**: Nhóm đã đưa cơ chế giả lập trễ mạng và Jitter $\pm 25$ms vào bài kiểm thử Chương 5.

---

### 2.3. Giải Thuật Nội Suy Tuyến Tính LERP (Chương 8: Improved Latency Handling — Trang 252 – 271)

#### 📉 Client-Side Interpolation (Trang 255 – 257)
* **Vấn đề**: Server gửi gói tin 20Hz (mỗi 50ms có 1 gói), Canvas vẽ 60 FPS (mỗi 16.6ms vẽ 1 khung hình).
* **Giải pháp của Glazer & Madhav**: Client không vẽ vị trí mới nhất ngay lập tức, mà đặt thời điểm kết xuất đồ họa lùi về quá khứ một khoảng thời gian đệm $\Delta t_{\text{interp}}$ (thường bằng $100$ms, tương đương 2 chu kỳ tick của Server):
  $$t_{\text{render}} = t_{\text{client\_now}} - \Delta t_{\text{interp}}$$
* **Công thức toán học LERP**:
  Tìm hai ảnh chụp quá khứ liền kề $S_{\text{old}}$ (tại $t_{\text{old}}$) và $S_{\text{new}}$ (tại $t_{\text{new}}$) sao cho $t_{\text{old}} \le t_{\text{render}} \le t_{\text{new}}$:
  $$\alpha = \frac{t_{\text{render}} - t_{\text{old}}}{t_{\text{new}} - t_{\text{old}}} \quad (0.0 \le \alpha \le 1.0)$$
  $$\vec{P}_{\text{render}} = \vec{P}_{\text{old}} + \left(\vec{P}_{\text{new}} - \vec{P}_{\text{old}}\right) \times \alpha$$
* **Mã nguồn đồ án**: Đã cài đặt hoàn chỉnh tại [`Src_Game/Network/SnapshotInterpolator.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Network/SnapshotInterpolator.java).

---

### 2.4. Tuần Tự Hóa & Sao Chép Thực Thể (Chương 4 & Chương 5)
* **Object Serialization (Chương 4 — Trang 120 – 157)**: Tối ưu hóa kích thước dữ liệu truyền qua TCP Socket bằng cách chỉ gửi các trường biến động (Delta Compression) thay vì gửi toàn bộ đối tượng cồng kềnh.
* **Object Replication (Chương 5 — Trang 158 – 183)**: Gói tin `WorldSnapshotPacket` chỉ đồng bộ trạng thái thế giới (vị trí, máu, trạng thái sống/chết), giữ kích thước mỗi gói tin dưới **$100$ bytes**.

---

## 3. ÁNH XẠ TRUY VẾT TỪ GLAZER & MADHAV SANG HỒ SƠ BÁO CÁO HUCE

| Chương Báo Cáo | Khái niệm & Kỹ thuật từ Glazer & Madhav được tích hợp |
|:---|:---|
| **Phần I (FURPS+)** | Ràng buộc kỹ thuật Netcode: Dedicated Server, độ trễ $< 50$ms, triệt tiêu Jitter bằng LERP. |
| **Phần II (Phân tích OOA)** | Ca sử dụng UC-01 đồng bộ di chuyển 20Hz; SSD mô tả vòng lặp Snapshot. |
| **Phần III (Thiết kế OOD)** | Thiết kế `SnapshotInterpolator` như một Pure Fabrication; thiết kế lớp gói tin DTO. |
| **Phần IV (Cài đặt OOP)** | Hiện thực hóa mã nguồn Java của giải thuật LERP trong `SnapshotInterpolator.java`. |
| **Phần V (Kiểm thử)** | Đồ thị đo đạc và thực nghiệm so sánh độ lệch vị trí (Jitter Displacement Error) có LERP vs không LERP. |
| **Phần VI (Phản biện)** | Phản biện khoảng trống lý thuyết của Larman 2004 về mạng phân tán thời gian thực. |
