---
aliases: [Game Programming Patterns, Nystrom, ECS, Component Pattern, Game Loop, Spatial Partition]
tags: [game-dev, patterns, nystrom, ecs, game-loop, spatial-partition, optimization]
created: 2026-10-19
updated: 2026-10-20
pillar: 2
raw_pdf: "00_Raw_PDF/Game Programming Patterns.pdf"
extracted_chapters: "00_Raw_PDF/_Knowledge_Base_Nystrom/chapters_extracted/"
---

# 07. Robert Nystrom — Game Programming Patterns (Kỹ Nghệ Game Thực Chiến)

> **Tác giả**: Robert Nystrom (Kỹ sư phần mềm kỳ cựu tại Electronic Arts, Google; thành viên nhóm thiết kế ngôn ngữ Dart)  
> **Nguồn trích xuất gốc**: `00_Raw_PDF/Game Programming Patterns.pdf` (604 trang)  
> **Thư mục chương Markdown bóc tách**: `00_Raw_PDF/_Knowledge_Base_Nystrom/chapters_extracted/`  
> **Vai trò trong Đồ án**: Cung cấp kiến trúc game loop chuẩn, giải thuật tối ưu hóa không gian $O(N)$ và giải pháp thành phần hóa (Component Pattern) khắc phục triệt để hạn chế của kế thừa sâu OOP truyền thống.

---

## 1. TẠI SAO ĐỒ ÁN GAME MULTIPLAYER BẮT BUỘC PHẢI CÓ NYSTROM?

Giáo trình của Craig Larman (*Applying UML and Patterns*) tập trung vào hệ thống bán hàng NextGen POS — nơi đối tượng thụ động chờ người dùng nhấn nút. Trái lại, trong trò chơi đấu trường hành động:
* **Thời gian là kẻ thù số 1**: Vòng lặp phải cập nhật liên tục 60 lần mỗi giây.
* **Quy luật bộ nhớ**: Nếu lạm dụng đa hình và con trỏ kế thừa sâu (vốn phổ biến trong sách OOP lý thuyết), CPU sẽ liên tục bị **Cache Miss** khi duyệt hàng trăm thực thể.
* **Bài toán va chạm**: Duyệt cặp $N \times N$ thực thể sẽ làm sập Server khi $N > 50$.

Cuốn sách của Robert Nystrom là cẩm nang kinh điển giải quyết toàn diện các thách thức này.

---

## 2. BẢN ĐỒ CÁC MẪU THIẾT KẾ NYSTROM TRONG SÁCH & ÁP DỤNG VÀO ĐỒ ÁN

### 2.1. Nhóm Mẫu Tuần Tự (Sequencing Patterns)

#### 🎮 Game Loop Pattern (Trang sách: 133 – 147)
* **Vấn đề**: Các máy tính khác nhau có tốc độ xử lý khác nhau. Nếu chạy vòng lặp kiểu `while(true) { update(); render(); }`, game sẽ chạy quá nhanh trên máy mạnh và giật lag trên máy yếu.
* **Giải pháp của Nystrom**: Sử dụng **Fixed Timestep with Variable Render (Thời gian cập nhật cố định & Vẽ nội suy)**.
* **Mã nguồn đồ án**: [`Src_Game/Core/GameEngineLoop.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Core/GameEngineLoop.java)
* **Công thức điều phối**:
  $$\text{accumulator} += \Delta t_{\text{elapsed}}$$
  $$\text{while } (\text{accumulator} \ge \text{MS\_PER\_UPDATE}) \implies \text{fixedUpdate}(), \quad \text{accumulator} -= \text{MS\_PER\_UPDATE}$$
  $$\alpha = \frac{\text{accumulator}}{\text{MS\_PER\_UPDATE}} \implies \text{render}(\alpha)$$

#### 🖼️ Double Buffer Pattern (Trang sách: 117 – 132)
* **Vấn đề**: Hiện tượng xé hình (Screen Tearing) khi màn hình quét điểm ảnh cùng lúc đồ họa đang vẽ dở.
* **Giải pháp**: Một bộ đệm khung hình để hiển thị (`Front Buffer`), một bộ đệm để vẽ ngầm (`Back Buffer`), sau đó tráo đổi con trỏ (`Swap Buffers`).

---

### 2.2. Nhóm Mẫu Tách Ghép Nối (Decoupling Patterns)

#### 🧩 Component Pattern (Trang sách: 219 – 239)
* **Nguyên lý cốt lõi**: **Composition over Inheritance** (Ưu tiên cắm ghép hơn kế thừa).
* **Phản biện Larman**: Thay vì tạo cây kế thừa khổng lồ:
  $$\text{Object} \to \text{Entity} \to \text{LivingEntity} \to \text{WarriorPlayer} \to \text{FlyingWarriorPlayer}$$
  Nystrom chia một thực thể thành các thành phần độc lập:
  * `InputComponent`: Nhận phím hoặc gói tin mạng.
  * `PhysicsComponent`: Tính toán vận tốc, ma sát, va chạm AABB.
  * `GraphicsComponent`: Vẽ hoạt ảnh tương ứng.
* **Ứng dụng**: Nhân vật [`Player.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Core/Player.java) cắm ghép `Hitbox`, `CompositeBuff`, và `IPlayerState` thay vì gom tất cả thành thuộc tính cứng.

---

### 2.3. Nhóm Mẫu Tối Ưu Hóa (Optimization Patterns)

#### 🌐 Spatial Partition Pattern (Trang sách: 325 – 340)
* **Vấn đề**: Khi kiểm tra va chạm giữa $N$ đòn đánh và $N$ nhân vật, giải thuật ngây thơ tốn $O(N^2)$ phép thử va chạm hình học.
* **Giải pháp Nystrom**: Chia không gian thế giới thành các ô lưới (Grid Buckets). Mỗi thực thể đăng ký vào ô chứa tọa độ của nó.
* **Độ phức tạp**: Hạ từ $O(N^2)$ xuống **$O(N)$** vì một thực thể chỉ kiểm tra với các thực thể trong cùng ô và 8 ô lân cận.
* **Mã nguồn đồ án**: [`Src_Game/Combat/SpatialGridAoI.java`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/SpatialGridAoI.java)

---

### 2.4. Nhóm Mẫu Hành Vi & Trạng Thái (Behavioral & State Patterns)

#### 🕹️ Command Pattern (Trang sách: 28 – 39)
* Biến mỗi thao tác phím người chơi thành một đối tượng lệnh có thể tuần tự hóa, truyền qua mạng, và hỗ trợ hoàn nguyên (Undo / Reconciliation).

#### 🔄 State Pattern (Trang sách: 97 – 115)
* Mô hình hóa máy trạng thái hữu hạn (FSM) cho đấu sĩ: `IdleState`, `MovingState`, `AttackingState`, `DeadState`. Loại bỏ hoàn toàn bẫy code `if-else` cờ biến lồng nhau.

---

## 3. ÁNH XẠ TRUY VẾT TỪ NYSTROM SANG HỒ SƠ BÁO CÁO HUCE

| Chương Báo Cáo | Khái niệm & Kỹ thuật từ Robert Nystrom được tích hợp |
|:---|:---|
| **Phần I (FURPS+)** | Yêu cầu hiệu năng Client 60 FPS, Server 20Hz Tick Rate; cấu trúc Game Loop chuẩn. |
| **Phần III (Thiết kế OOD)** | Áp dụng Component Pattern kết hợp 9 GRASP; thiết kế State Pattern cho nhân vật. |
| **Phần IV (Cài đặt OOP)** | Hiện thực hóa `GameEngineLoop.java` (Fixed Timestep) và `SpatialGridAoI.java`. |
| **Phần V (Kiểm thử)** | Đo đạc hiệu năng va chạm $O(N)$ của Spatial Grid so với duyệt thô $O(N^2)$. |
| **Phần VI (Phản biện)** | Phản biện sự lỗi thời của Kế thừa sâu (Deep Inheritance) của Larman so với Component Pattern của Nystrom. |
