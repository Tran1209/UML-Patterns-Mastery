# 📐 Cơ Chế Vật Lý Lai 2.5D (2D vs 3D), Khóa Trục & Depth Sorting Trong *Survival Heroes* (Godot 4 C# / Unity)

> **File mã nguồn cài đặt trực tiếp**: [`SurvivalHeroes_Godot4_Demo/Scripts/Patterns/Physics25DAndDepthSorting.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/Patterns/Physics25DAndDepthSorting.cs)

---

## 1. Giải Đáp Thắc Mắc: Vật Lý Trong Game 2.5D Này Là 2D Hay 3D?

Câu trả lời chính xác là: **Mô hình Vật lý Lai 2.5D (Hybrid 2.5D Physics)** — kết hợp **Vật lý 3D cho Địa hình/Tường** và **Hình học 2D phẳng cho Chiến đấu (Đạn, Kỹ năng AOE, Bụi cỏ, Vòng Bo)**.

| Tầng Tương Tác | Dùng Hệ Thống Nào? | Lớp Cài Đặt Cụ Thể | Lý Do Kỹ Thuật (Tại sao không dùng thuần 3D hay thuần 2D?) |
| :--- | :--- | :--- | :--- |
| **1. Di chuyển Nhân vật & Va chạm Tường / Nhà cửa** | **Vật lý 3D (`Jolt Physics 3D`)** | `CharacterBody3D` (`CapsuleShape3D`) + `StaticBody3D` (`BoxShape3D`) + `MoveAndSlide()` | Cho phép tận dụng trực tiếp không gian 3D (ánh sáng đổ bóng `DirectionalLight3D`, `Camera3D` góc nghiêng `-55°`, trượt dọc theo mép tường nhà mượt mà). |
| **2. Đạn bay, Chiêu thức AOE, Bụi cỏ, Nhặt đồ & Vòng Bo** | **Toán học Hình học Phẳng 2D (`Vector2`)** | `PlanarPhysics25D.Distance25D(a, b)` quy chiếu `(X, Y, Z) -> (X, Z)` | Trong góc nhìn chéo Isometric `-55°`, nếu dùng hình cầu 3D thì viên đạn bay ở độ cao `Y = 1.05m` dễ bị lệch thị giác so với vòng tròn dưới chân tướng. Chiếu phẳng xuống 2D giúp **trúng 100% chính xác theo vòng chỉ thị mặt đất** và nhanh hơn ~20 lần so với tạo hàng trăm `RigidBody3D`! |
| **3. Ngắm Chuột trên Mặt đất (Mouse Aiming)** | **Ray-Plane Intersection (Tia 3D cắt Mặt phẳng 2D)** | `Plane(Vector3.Up, 0f).IntersectsRay(rayOrigin, rayDir)` | Không dùng `PhysicsRayQueryParameters3D` tốn tài nguyên, mà giải phương trình giao điểm trực tiếp giữa tia `Camera3D` và mặt phẳng toán học $Y = 0$. |

---

## 2. Tại Sao Lúc Thì Nói "Khóa Trục Z", Lúc Lại "Khóa Trục Y"? Cách Khóa Cứng 2 Lớp

Trong Engine 3D (**Godot 4 3D** và **Unity**), trục thẳng đứng hướng lên trời là **trục `Y` (`Vector3.Up`)**, còn hai trục nằm ngang là **`X` và `Z`**. Do đó có **2 kiểu game 2.5D** với trục khóa khác nhau (đều đã được hỗ trợ sẵn trong `PlaneMode25D` của [`Physics25DAndDepthSorting.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/Patterns/Physics25DAndDepthSorting.cs)):

```text
   [KIỂU A: TOP-DOWN / ISOMETRIC 2.5D]                 [KIỂU B: SIDE-SCROLLER 2.5D]
   (Survival Heroes, Liên Quân, Hades)                 (Hollow Knight, Ori, Metroid Dread)

             Trục Y (Độ cao = 0)                                 Trục Y (Nhảy lên/xuống)
                 ▲                                                   ▲
                 │   / Trục Z (Bắc - Nam)                            │
                 │  /                                                │
                 │ /                                                 └───────────► Trục X (Trái - Phải)
   ──────────────┼───────────► Trục X (Tây - Đông)                  /
                                                                   / Trục Z (Độ sâu = 0)
   👉 Di chuyển trên mặt phẳng (X, Z)                  👉 Di chuyển trên mặt phẳng (X, Y)
   👉 KHÓA CỨNG TRỤC Y = 0                             👉 KHÓA CỨNG TRỤC Z = 0
```

### Cơ chế Khóa Trục 2 Lớp (Dual-Layer Axis Lock) trong `PlanarPhysics25D`:

1. **Lớp 1 — Khóa phần cứng ngay trong Engine Vật lý (`ApplyHardwareAxisLock`)**:
   ```csharp
   // Khóa dịch chuyển trục đứng và khóa đổ ngã nghiêng X/Z trên Jolt Physics 3D
   body.AxisLockLinearY = true;   // (Hoặc AxisLockLinearZ = true nếu là Side-Scroller)
   body.AxisLockAngularX = true;
   body.AxisLockAngularZ = true;
   ```
2. **Lớp 2 — Ép phẳng toán học mỗi Physics Frame (`ClampInsideMapBounds`)**:
   Ngay sau khi gọi `MoveAndSlide()`, cả vận tốc `Velocity` và vị trí `GlobalPosition` đều được kẹp cứng về mặt phẳng chuẩn để triệt tiêu mọi sai số dấu phẩy động:
   ```csharp
   Velocity = PlanarPhysics25D.ClampVelocityToPlane(Velocity, PlaneMode25D.TopDownIsometric_LockY);
   GlobalPosition = PlanarPhysics25D.ClampPositionToPlaneAndBounds(GlobalPosition, 82.5f, 0f);
   ```

---

## 3. Xử Lý Depth Sorting (Thứ Tự Vẽ Trước-Sau) & Chống Nhấp Nháy (Anti Z-Fighting)

Trong góc nhìn 2.5D Isometric (Camera đặt ở phía Nam `+Z`, cao `Y = 22m`, nhìn chúc xuống `-55°` về phía Bắc `-Z`), có **3 vấn đề hiển thị kinh điển** và cách [`DepthSorter25D`](../../SurvivalHeroes_Godot4_Demo/Scripts/Patterns/Physics25DAndDepthSorting.cs) giải quyết triệt để:

### 3.1. Vấn đề 1: Thân nhân vật 3D bị vẽ xuyên thấu sai thứ tự khi bật `Transparency = Alpha`
- **Nguyên nhân**: Trong 3D, các vật liệu bật `Transparency = Alpha` **không ghi vào Depth Buffer (Z-Buffer)** mà chỉ sắp xếp tương đối theo tâm Mesh. Nếu thân Tướng lúc bình thường cũng bật `Alpha`, tay cầm vũ khí hoặc nhân vật đứng sau có thể đè lên nhân vật đứng trước.
- **Giải pháp đã cài đặt trong [`HeroCharacter.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/Gameplay/HeroCharacter.cs)**:
  - Khi **không ở trong bụi cỏ**: Đặt `_bodyMat.Transparency = Disabled` (Opaque 100%) $\rightarrow$ GPU dùng **Hardware Z-Buffer** theo từng pixel, chính xác tuyệt đối 100%!
  - Khi **chui vào Bụi Cỏ Tàng Hình (`InStealthBush = true`)**: Bật `Transparency = Alpha` (`A = 0.38`) đồng thời gọi `DepthSorter25D.ComputeDynamicRenderPriority(GlobalPosition.Z)` và `UpdateIsometricDepthOrder()` để **nhân vật nào có tọa độ `Z` lớn hơn (gần màn hình hơn) luôn có `RenderPriority` và `SortingOffset` cao hơn** nhân vật đứng phía sau!

### 3.2. Vấn đề 2: Các vòng tròn dưới mặt đất (Đường lộ, Sân gạch, Vòng Bo, Đế Vũ Khí, Vòng Nổ AOE, Tâm Ngắm) đè lên nhau bị nhấp nháy rách hình (`Z-Fighting`)
- **Giải pháp (`DepthSorter25D` Elevation Layers)**: Phân tách độ cao vi mô theo 8 tầng cố định từ `0.00m` đến `0.08m`:
  - `ElevationFloorBase = 0.00f` (Mặt đất gốc)
  - `ElevationGridLines = 0.02f` (Vạch kẻ 9 ô lưới mạng)
  - `ElevationWaterAndRoad = 0.03f` (Mặt sông & Đường cái)
  - `ElevationTownPlaza = 0.04f` (Nền sân gạch 22 Địa danh)
  - `ElevationSafeZoneRing = 0.05f` (Vòng Bo Sinh Tồn)
  - `ElevationLootPad = 0.06f` (Đế phát sáng Hộp Vũ Khí & Lều Thần Bí)
  - `ElevationAoeSkillRing = 0.07f` (Vòng nổ chiêu thức Q/W/E)
  - `ElevationAimReticle = 0.08f` (Vòng tâm ngắm con trỏ chuột)

### 3.3. Vấn đề 3: Thanh máu, Tên Tướng & Số sát thương nảy (`Label3D`) bị mái nhà hoặc tường che mất
- **Giải pháp**: Bật `NoDepthTest = true`, `Billboard = Enabled`, và `RenderPriority = PriorityOverheadUi (20)` kết hợp `SortingOffset` theo trục `Z`. Nhờ đó thông tin chiến đấu luôn nổi rõ ràng phía trên mọi vật cản 3D.
