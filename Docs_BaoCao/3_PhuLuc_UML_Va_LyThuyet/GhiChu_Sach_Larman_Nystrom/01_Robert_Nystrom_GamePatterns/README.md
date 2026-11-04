# Game Programming Patterns - Robert Nystrom
## Trích xuất Lý thuyết Toàn diện (10 Đợt - 100% Hoàn thành)

Thư mục này chứa toàn bộ hệ thống lý thuyết, cấu trúc mẫu thiết kế, sơ đồ luồng điều khiển và phân tích dòng-từng-dòng code từ tác phẩm **"Game Programming Patterns"** của tác giả **Robert Nystrom**.

---

### 📖 Bản Gộp Toàn Văn Hoàn Chỉnh
- **File tổng hợp duy nhất**: [`FULL_Game_Programming_Patterns.md`](./FULL_Game_Programming_Patterns.md) *(~324 KB, đầy đủ toàn bộ 10 đợt trích xuất, có mục lục liên kết)*

---

### 📂 Chi tiết 10 Đợt Trích Xuất (Thư mục `10_Runs/`)

| Đợt | Tập tin | Nội dung và Mẫu thiết kế phân tích |
| :---: | :--- | :--- |
| **01** | [`Dot_01_Architecture_va_Command.md`](./10_Runs/Dot_01_Architecture_va_Command.md) | **Introduction & Architecture**: Architecture, Performance, Games; **Command Pattern**: Undo/Redo, Replay, Input Mapping, AI Command Stream. |
| **02** | [`Dot_02_Flyweight_va_Observer.md`](./10_Runs/Dot_02_Flyweight_va_Observer.md) | **Flyweight Pattern**: Khử trùng lặp bộ nhớ Texture/Terrain/Mesh; **Observer Pattern**: Bắn hạ coupling giữa Game Event và Audio/UI/Achievements. |
| **03** | [`Dot_03_Prototype_va_Singleton.md`](./10_Runs/Dot_03_Prototype_va_Singleton.md) | **Prototype Pattern**: Clone quái/đạn, JSON Data-driven Spawning; **Singleton Pattern**: Rủi ro biến toàn cục và giải pháp thay thế. |
| **04** | [`Dot_04_State_va_DoubleBuffer.md`](./10_Runs/Dot_04_State_va_DoubleBuffer.md) | **State Pattern**: FSM nhân vật (Idle, Run, Attack, Hurt, Dead), Concurrent/Hierarchical FSM; **Double Buffer Pattern**: Tránh xé hình, khử race condition đồ họa. |
| **05** | [`Dot_05_GameLoop_va_UpdateMethod.md`](./10_Runs/Dot_05_GameLoop_va_UpdateMethod.md) | **Game Loop Pattern**: Fixed timestep, Variable timestep, Semi-fixed timestep với Interpolation; **Update Method Pattern**: Mô phỏng thực thể theo frame. |
| **06** | [`Dot_06_Bytecode_va_SubclassSandbox.md`](./10_Runs/Dot_06_Bytecode_va_SubclassSandbox.md) | **Bytecode Pattern**: Máy ảo Virtual Machine, Stack Machine cho phép modder cấu hình skill; **Subclass Sandbox Pattern**: Quản trị hàng trăm phép thuật an toàn. |
| **07** | [`Dot_07_TypeObject_va_Component.md`](./10_Runs/Dot_07_TypeObject_va_Component.md) | **Type Object Pattern**: Data-driven RPG Monster Breeding; **Component Pattern**: Kiến trúc ECS tách biệt Render, Physics, Input, Audio. |
| **08** | [`Dot_08_EventQueue_va_ServiceLocator.md`](./10_Runs/Dot_08_EventQueue_va_ServiceLocator.md) | **Event Queue Pattern**: Xử lý âm thanh/sự kiện bất đồng bộ theo hàng đợi; **Service Locator Pattern**: Dependency Injection nhẹ nhàng, Null Service Fallback. |
| **09** | [`Dot_09_DataLocality_va_DirtyFlag.md`](./10_Runs/Dot_09_DataLocality_va_DirtyFlag.md) | **Data Locality Pattern**: Tối ưu CPU Cache L1/L2/L3, Contiguous Arrays, Hot/Cold Splitting; **Dirty Flag Pattern**: Tránh tính toán lại Transform/Scene Graph vô nghĩa. |
| **10** | [`Dot_10_ObjectPool_va_SpatialPartition.md`](./10_Runs/Dot_10_ObjectPool_va_SpatialPartition.md) | **Object Pool Pattern**: Khử Garbage Collection lag giật, tái chế Bullet/Particle; **Spatial Partition Pattern**: Grid/Quadtree tối ưu kiểm tra va chạm từ $O(N^2)$ về $O(N)$. |
