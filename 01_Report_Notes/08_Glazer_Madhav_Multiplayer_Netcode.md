---
aliases: [Multiplayer Game Programming, Glazer Madhav, Netcode, LERP, Client Prediction]
tags: [multiplayer, netcode, authoritative-server, lerp, jitter, glazer-madhav]
created: 2026-10-19
pillar: 3
---

# 08. Joshua Glazer & Sanjay Madhav — Multiplayer Game Programming (Kỹ Thuật Mạng Real-Time)
> **Tác giả**: Joshua Glazer & Sanjay Madhav (Giảng viên Kỹ nghệ Game tại USC / Kỹ sư Netcode kỳ cựu)  
> **Vai trò trong Đồ án**: Cung cấp lý thuyết và giải thuật chuyên sâu cho tầng Network; giải quyết triệt để bài toán giật hình (Micro-stutter / Jitter) giữa Server 20Hz và Client 60fps.

---

## 1. TẠI SAO ĐỒ ÁN GAME MULTIPLAYER BẮT BUỘC PHẢI CÓ CUỐN NÀY?
Khi làm game qua mạng, các lập trình viên nghiệp dư thường mắc lỗi:
1. **Tin tưởng Client (Client-Authoritative)**: Để Client tự tính toán kết quả di chuyển và gửi vị trí lên Server $\rightarrow$ Dễ dàng bị hack tốc độ, hack xuyên tường (Teleport Hack).
2. **Không bù trừ độ trễ (No Lag Compensation)**: Server gửi tọa độ ở tần số 20Hz, Client nhận được thì "dịch chuyển tức thời" (Teleport jump) đến tọa độ mới $\rightarrow$ Nhân vật bị giật cục liên tục (*Micro-stutter*).

Giáo trình Craig Larman hoàn toàn không đề cập đến hiện tượng này. Cuốn sách của Glazer & Madhav là lời giải kỹ thuật mẫu mực.

---

## 2. 3 TRỤ CỘT NETCODE ÁP DỤNG TRỰC TIẾP VÀO MÃ NGUỒN

### 2.1. Server-Authoritative Architecture (Máy Chủ Nắm Quyền Tuyệt Đối)
* **Triết lý**: *"Never trust the client"* (Tuyệt đối không tin máy khách).
* **Luồng xử lý**:
  1. Client bấm phím WASD $\rightarrow$ Chỉ gửi **Ý định hành động** (`InputCommandPacket`: `dx, dy, timestamp, sequenceNumber`).
  2. Server là nơi duy nhất chạy mô phỏng vật lý, tính toán va chạm và phán quyết vị trí hợp lệ.
  3. Server định kỳ phát sóng **Ảnh chụp thế giới** (`WorldSnapshotPacket`) chứa tọa độ chuẩn của tất cả người chơi.

### 2.2. Snapshot Interpolation (Nội Suy Tuyến Tính - LERP)
* **Bài toán phát hiện trong Pha Inception**: 
  * Server gửi snapshot ở tần số **20Hz** (mỗi 50ms một gói tin).
  * Màn hình Client vẽ lại ở tần số **60 FPS** (mỗi 16.6ms một khung hình).
  * Trong khoảng thời gian giữa 2 gói tin snapshot ($t_1$ và $t_2$), Client không có dữ liệu mới. Nếu không có thuật toán, nhân vật sẽ đứng khựng lại rồi nhảy vọt lên $\rightarrow$ **Giật hình (Micro-stutter)**!
* **Giải thuật LERP (Linear Interpolation)**:
  * Client lưu trữ các snapshot vào một hàng đợi đệm (`SnapshotBuffer`).
  * Thực hiện nội suy tọa độ mượt mà giữa snapshot quá khứ ($S_{old}$) và snapshot hiện tại ($S_{new}$):
  $$\vec{P}_{render} = \vec{P}_{old} + (\vec{P}_{new} - \vec{P}_{old}) \times \alpha \quad (0 \le \alpha \le 1)$$
* **Vị trí cài đặt**: `Src_Game/Network/SnapshotInterpolator.java`.

### 2.3. Client-Side Prediction & Reconciliation (Đoán Trước & Sửa Sai)
* Để người chơi bản địa không có cảm giác bị "delay" nút bấm khi ping cao:
  * Khi bấm 'W', Client lập tức cho nhân vật tiến lên phía trước ngay lập tức (**Prediction**).
  * Lưu lại lệnh vào bộ đệm `ClientPredictionBuffer`.
  * Khi nhận được snapshot phản hồi từ Server: Nếu tọa độ Server khác với tọa độ Client dự đoán, Client sẽ áp dụng lại các lệnh chưa được Server xác nhận để đưa nhân vật về vị trí đúng (**Reconciliation**).

---

## 3. LIÊN KẾT ĐẾN BÁO CÁO ĐỒ ÁN
* Báo cáo **Phần I (Yêu cầu FURPS+)**: Đặt ra chỉ số hiệu năng cụ thể (Latency < 50ms, Jitter < 5ms).
* Báo cáo **Phần V (Kiểm thử & Khắc phục Lỗi Kỹ thuật)**: Dùng cuốn sách này làm cơ sở lý thuyết để phân tích thực nghiệm biểu đồ so sánh: *Chuyển động có LERP vs. Chuyển động không có LERP*.
