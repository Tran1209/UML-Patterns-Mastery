---
aliases: [Game Programming Patterns, Nystrom, ECS, Component Pattern]
tags: [game-dev, patterns, nystrom, ecs, game-loop, spatial-partition]
created: 2026-10-19
pillar: 2
---

# 07. Robert Nystrom — Game Programming Patterns (Kỹ Nghệ Game Thực Chiến)
> **Tác giả**: Robert Nystrom (Kỹ sư kỳ cựu tại Electronic Arts / Google)  
> **Vai trò trong Đồ án**: Khắc phục các hạn chế ("điểm lỏ") của OOP kế thừa truyền thống; tối ưu hóa hiệu năng vòng lặp game và giải thuật va chạm.

---

## 1. TẠI SAO ĐỒ ÁN GAME PHẢI CÓ CUỐN NÀY?
Giáo trình Craig Larman dạy thiết kế hệ thống phần mềm doanh nghiệp (POS, Ngân hàng), nơi đối tượng chủ yếu nằm chờ tương tác người dùng. Trái lại, trong **Trò chơi hành động thời gian thực (Real-time Action Game)**:
* Hệ thống phải chạy liên tục ở chu kỳ **60 khung hình/giây (60 FPS)**.
* Hàng trăm thực thể (Player, Enemy, Projectile, Hitbox) tương tác đồng thời.
* Nếu lạm dụng kế thừa đa tầng của OOP cổ điển, CPU sẽ bị nghẽn do **Cache Miss**.

Cuốn sách của Robert Nystrom cung cấp các mẫu thiết kế giải quyết triệt để bài toán này.

---

## 2. CÁC MẪU THIẾT KẾ ÁP DỤNG TRỰC TIẾP VÀO MÃ NGUỒN

### 2.1. Game Loop Pattern (Vòng Lặp Trò Chơi)
* **Ý tưởng**: Tách biệt hoàn toàn giữa tốc độ tính toán vật lý/logic (Fixed Timestep 20Hz - 50ms) và tốc độ dựng hình (Render 60 FPS).
* **Vị trí áp dụng**: `Src_Game/Core/GameLoop.java`.

```java
// Logic tách biệt render và update vật lý
while (running) {
    long current = System.currentTimeMillis();
    long elapsed = current - previous;
    previous = current;
    lag += elapsed;

    processInput();

    // Cập nhật logic với fixed delta-time để chống giật
    while (lag >= MS_PER_UPDATE) {
        updatePhysics();
        lag -= MS_PER_UPDATE;
    }

    render(lag / MS_PER_UPDATE); // Nội suy mượt mà
}
```

### 2.2. Component Pattern (Thành phần hóa — Cội nguồn của ECS)
* **Thay thế Deep Inheritance**: Thay vì bắt `Player` kế thừa từ `LivingEntity`, ta trang bị cho thực thể các Component độc lập:
  * `InputComponent`: Xử lý phím.
  * `PhysicsComponent`: Xử lý vận tốc, gia tốc, trọng lực.
  * `RenderComponent`: Chịu trách nhiệm vẽ lên màn hình.
  * `HealthComponent`: Chịu trách nhiệm quản lý máu và sát thương.
* **Nguyên tắc**: **Composition over Inheritance** (Ghép nối thay vì kế thừa).

### 2.3. Spatial Partition Pattern (Phân Vùng Không Gian)
* **Vấn đề**: Nếu có $N$ thực thể, giải thuật kiểm tra va chạm ngây thơ sẽ tốn $O(N^2)$ phép so sánh (lag giật nghiêm trọng).
* **Giải pháp của Nystrom**: Chia thế giới thành các ô lưới (Grid Bucket / Spatial Grid AoI). Thực thể chỉ kiểm tra va chạm với các đối tượng nằm trong cùng một ô lưới hoặc ô lân cận $\rightarrow$ Giảm độ phức tạp xuống $O(N)$.
* **Vị trí áp dụng**: `Src_Game/Combat/SpatialGridAoI.java`.

### 2.4. State Pattern & Double Buffer
* **State Pattern**: Quản lý các trạng thái hoạt ảnh và hành động nhân vật (`IdleState`, `MovingState`, `AttackingState`, `StunnedState`) tránh bẫy câu lệnh `if-else` lồng nhau vô tận.
* **Double Buffer**: Sử dụng 2 bộ đệm khung hình để ngăn hiện tượng xé hình (Screen Tearing) khi vẽ Canvas 60 FPS.

---

## 3. LIÊN KẾT ĐẾN BÁO CÁO ĐỒ ÁN
* Báo cáo **Phần III (Thiết kế OOD)**: Trình bày sự kết hợp giữa 9 nguyên lý GRASP của Larman và Component Pattern của Nystrom.
* Báo cáo **Phần IV (Cài đặt OOP)**: Trích dẫn mã nguồn `SpatialGridAoI.java` và `GameLoop.java`.
