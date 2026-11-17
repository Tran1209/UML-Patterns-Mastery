# Phân Tích Toàn Diện 177 Scripts `Unity-Technologies/game-programming-patterns-demo` & Cẩm Nang Lọc Kiến Trúc Game 2.5D (*Survival Heroes*)

> **Kho mã nguồn gốc phân tích**: `https://github.com/Unity-Technologies/game-programming-patterns-demo` (`Assets/UnityTechnologies/`)  
> **Bản tiếng Anh đầy đủ (1.209 dòng)**: [02_UNITY_25D_ARCHITECTURE_ANALYSIS_EN.md](./02_UNITY_25D_ARCHITECTURE_ANALYSIS_EN.md)  
> **Tài liệu chuyển đổi Mạng Photon sang Godot 4 C#**: [03_PhanTich_ToanDien_Photon_ChuyenDoi_Godot4_CSharp_VI.md](./03_PhanTich_ToanDien_Photon_ChuyenDoi_Godot4_CSharp_VI.md)  
> **Dự án Game Demo Thực Chiến (Godot 4.7 .NET 8)**: [SurvivalHeroes_Godot4_Demo](../../SurvivalHeroes_Godot4_Demo/)

---

## 1. Tổng Quan Kiểm Kê Kho Mã Nguồn (`Assets/UnityTechnologies/`)

Qua rà soát trực tiếp từng file trên cây mã nguồn, toàn bộ thư mục `Assets/UnityTechnologies/` gồm **687 file (33.54 MB)** chia làm **9 thư mục cấp cao**, trong đó có chính xác **177 file C# (`.cs`)** và **13 file Prefab lõi**:

| Thư mục Cấp cao | Tổng số File (Không tính `.meta`) | Số Script C# | Dung lượng (MB) | Vai trò Kiến trúc & Phân loại |
| :--- | :---: | :---: | :---: | :--- |
| `Art/` | 166 | 0 | 26.31 | Tài nguyên đồ họa/âm thanh phi logic (`.mat`, `.png`, `.fbx`, `.shader`, `.aif`) — **Lược bỏ khi tái sử dụng code** |
| `Prefabs/` | 13 | 0 (13 prefabs) | 0.05 | 7 Prefab gắn logic (`SceneLoader`, `StateMachine`, `MainMenuUI`, `Cameras`) + 6 Prefab hình mẫu |
| `Resources/` | 74 | 0 | 0.07 | Dữ liệu `ScriptableObject` (`NavigationButtonSO`, `InfoTextSO`, Event Channels) nạp lúc chạy |
| `Scenes/` | 3 | 0 | 0.03 | Scene khởi động `Boot.unity` và thiết lập ánh sáng URP |
| `Scripts/` | 64 | **64** | 0.17 | **Khung sườn ứng dụng dùng chung**: `StateMachine`, `SceneLoader`, `BaseEventSO`, `UIPresenter` |
| `UI Toolkit/` | 14 | 0 | 0.03 | Giao diện `.uxml`, `.uss`, `.tss` cho menu chính |
| `URPSettings/` | 3 | 0 | 0.02 | Cấu hình Universal Render Pipeline |
| `_DesignPatterns/` | 263 | **70** | 6.46 | **12 mẫu Design Patterns (GoF & Game Patterns)**: `ObjectPool`, `Command`, `State`, `Strategy`, `Factory`, `Flyweight`, `Observer`, `MVP`, `MVVM`, `DirtyFlag`, `Singleton`, `TypeObject` |
| `_SOLID/` | 87 | **43** | 0.40 | **5 nguyên lý SOLID**: `SingleResponsibility`, `OpenClosed`, `LiskovSubstitution`, `InterfaceSegregation`, `DependencyInversion` (kèm bản `Unrefactored` đối chiếu) |
| **TỔNG CỘNG** | **687** | **177** | **33.54 MB** | **78 Giữ nguyên (`KEEP`) • 58 Tùy biến (`ADAPT`) • 41 Lược bỏ (`PRUNE`)** |

---

## 2. Sơ Đồ Cây Phân Cấp Toàn Bộ 177 Scripts Logic & Nhãn Chọn Lọc Cho Game 2.5D

```text
Assets/UnityTechnologies/
├── Art/                         [166 files - 26.31 MB] 🔴 PRUNE (Tài nguyên 3D/Audio mẫu của demo)
├── Resources/                   [74 files  - 0.07 MB]  🟡 ADAPT (Chuyển ý tưởng EventChannelSO/DataSO)
├── Scenes/                      [3 files   - 0.03 MB]  🟢 KEEP  (Kiến trúc Bootloader Scene)
├── UI Toolkit/                  [14 files  - 0.03 MB]  🟡 ADAPT (Layout UI mẫu)
├── URPSettings/                 [3 files   - 0.02 MB]  🔴 PRUNE (Cấu hình render riêng của Unity)
│
├── Prefabs/                     [13 prefabs]
│   ├── Cameras/                 🟡 ADAPT (Chuyển sang Camera Isometric 2.5D góc nghiêng -55 độ)
│   ├── Managers/
│   │   ├── SceneLoader.prefab   🟢 KEEP  (Quản lý chuyển Scene bất đồng bộ)
│   │   └── StateMachine.prefab  🟢 KEEP  (Điều phối vòng đời ứng dụng)
│   ├── Models/Plane.prefab      🔴 PRUNE (Mặt phẳng 3D mẫu)
│   └── UI/                      🟡 ADAPT (Menu & màn hình chờ)
│
├── _SOLID/                      [43 C# Scripts]
│   ├── 1_SingleResponsibility/
│   │   ├── Player.cs            🟢 KEEP  (Lớp lắp ghép trung tâm theo hướng thành phần)
│   │   ├── PlayerInput.cs       🟡 ADAPT (Đổi sang đọc vector di chuyển 2.5D + Đệm phím)
│   │   ├── PlayerMovement.cs    🟡 ADAPT (Khóa mặt phẳng 2.5D, bỏ di chuyển tự do trục Y)
│   │   ├── PlayerAudio.cs       🟢 KEEP  (Tách biệt hoàn toàn âm thanh khỏi logic)
│   │   ├── PlayerFX.cs          🟢 KEEP  (Tách biệt hiệu ứng hạt/VFX khỏi va chạm)
│   │   └── UnrefactoredPlayer.cs 🔴 PRUNE (Code mẫu phản ví dụ - God Class)
│   │
│   ├── 2_OpenClosed/
│   │   ├── AreaOfEffect.cs      🟢 KEEP  (Lớp trừu tượng tính vùng sát thương/hiệu ứng AOE)
│   │   ├── CircleEffect.cs      🟢 KEEP  (Công thức vùng tròn AOE)
│   │   ├── HexagonalEffect.cs   🟢 KEEP  (Vùng lục giác AOE)
│   │   ├── SquareEffect.cs      🟢 KEEP  (Vùng vuông AOE)
│   │   ├── TriangularEffect.cs  🟢 KEEP  (Vùng tam giác/hình nón AOE)
│   │   └── UnrefactoredAreaOfEffect.cs 🔴 PRUNE (Code mẫu vi phạm OCP dùng switch-case cứng)
│   │
│   ├── 3_LiskovSubstitution/
│   │   ├── Health/
│   │   │   ├── Health.cs              🟢 KEEP  (Quản lý Máu chuẩn, hỗ trợ kế thừa an toàn)
│   │   │   ├── InvulnerableHealth.cs  🟢 KEEP  (Trạng thái bất tử / Khiên chắn)
│   │   │   └── Unrefactored*.cs (2)   🔴 PRUNE (Code mẫu vi phạm LSP)
│   │   ├── PowerUps/
│   │   │   ├── PowerUp.cs             🟡 ADAPT (Cơ sở cho Vật phẩm nhặt trên bản đồ 2.5D)
│   │   │   ├── HealthBoost.cs         🟢 KEEP  (Hồi máu khi nhặt)
│   │   │   ├── SpeedBoost.cs          🟢 KEEP  (Tăng tốc chạy tạm thời)
│   │   │   └── Unrefactored*.cs (2)   🔴 PRUNE (Code mẫu vi phạm LSP)
│   │   └── Navigator.cs / Rotator.cs  🟡 ADAPT (Điều chỉnh cho hướng nhìn 2.5D)
│   │
│   ├── 4_InterfaceSegregation/
│   │   ├── IDamageable.cs       🟢 KEEP  (Giao diện nhận sát thương tối giản - Cực kỳ quan trọng!)
│   │   ├── IEffectTrigger.cs    🟢 KEEP  (Giao diện kích hoạt hiệu ứng vùng)
│   │   ├── IExplodable.cs       🟢 KEEP  (Giao diện phát nổ dây chuyền - Dùng cho Thùng Thuốc Nổ)
│   │   ├── IMovable.cs          🟢 KEEP  (Giao diện di chuyển)
│   │   ├── IUnitStats.cs        🟢 KEEP  (Giao diện chỉ số đơn vị)
│   │   ├── ExplodingBarrel.cs   🟡 ADAPT (Thùng thuốc nổ thực thi IDamageable + IExplodable)
│   │   ├── Target.cs            🟡 ADAPT (Mục tiêu nhận sát thương)
│   │   ├── Turret.cs / Projectile.cs 🟡 ADAPT (Trụ bắn & Đạn 2.5D)
│   │   └── UnrefactoredTarget.cs 🔴 PRUNE (Interface béo phì)
│   │
│   └── 5_DependencyInversion/
│       ├── ISwitchable.cs       🟢 KEEP  (Giao diện đóng/mở trừu tượng cho Bẫy, Cửa, Trụ Hồi Máu, Lều Thần Bí)
│       ├── Switch.cs            🟡 ADAPT (Bộ kích hoạt phụ thuộc vào ISwitchable thay vì lớp cụ thể)
│       ├── Door.cs / Trap.cs    🟡 ADAPT (Cửa & Bẫy trong đấu trường 2.5D)
│       └── Unrefactored*.cs (3) 🔴 PRUNE (Code mẫu phụ thuộc cứng)
│
├── _DesignPatterns/             [70 C# Scripts]
│   ├── 1_Factory/               🟡 ADAPT (Product.cs, ConcreteFactory*.cs -> Dùng tạo Vũ khí / Quái)
│   ├── 2_ObjectPool/
│   │   ├── RevisedGun.cs        🟡 ADAPT (Dùng ObjectPool chuẩn cho Đạn / VFX / Số sát thương)
│   │   ├── RevisedProjectile.cs 🟡 ADAPT (Đạn tự trả về Pool sau khi chạm mục tiêu hoặc hết tầm)
│   │   └── ObjectPool.cs / ExampleGun.cs 🔴 PRUNE (Bản tự viết cũ, thay bằng Pool chuẩn)
│   ├── 3_Singleton/
│   │   ├── PersistentSingleton.cs 🟢 KEEP (Singleton giữ nguyên xuyên suốt các Scene)
│   │   └── Singleton.cs / SimpleSingleton.cs 🔴 PRUNE (Gắn chặt theo từng Scene, dễ lỗi)
│   ├── 4_Command/
│   │   ├── ICommand.cs          🟢 KEEP  (Giao diện đóng gói hành động)
│   │   ├── CommandInvoker.cs    🟡 ADAPT (Chuyển từ Undo/Redo mê cung sang InputBufferQueue 180ms cho 2.5D!)
│   │   └── MoveCommand.cs       🟡 ADAPT (Đóng gói lệnh tung chiêu Q/W/E/LMB)
│   ├── 5_State/
│   │   ├── IState.cs            🟢 KEEP  (Giao diện trạng thái thuần C#)
│   │   ├── StateMachine.cs      🟢 KEEP  (Bộ máy chuyển trạng thái: Idle, Walk, Dash, Stealth, Dead)
│   │   ├── States/*.cs (3)      🟡 ADAPT (Cập nhật theo vận tốc mặt phẳng 2.5D)
│   │   └── UnrefactoredPlayerController.cs 🔴 PRUNE
│   ├── 6_Observer/              🟢 KEEP  (Sử dụng C# Event Bus tách rời hoàn toàn Combat và UI HUD)
│   ├── 7_MVP/                   🔴 PRUNE (Bản UGUI cũ, đã có MVVM/EventBus hiện đại hơn)
│   ├── 8_MVVM/                  🟢 KEEP  (Kiến trúc tách biệt ViewModel và View)
│   ├── 9_Strategy/
│   │   ├── Ability.cs           🟢 KEEP  (Mỗi kỹ năng/vũ khí là một Strategy độc lập!)
│   │   ├── FireballAbility.cs /DashAbility.cs /HealAbility.cs /RageAbility.cs 🟡 ADAPT (Hệ thống 4 chiêu LMB/Q/W/E của Survival Heroes)
│   │   └── AbilityRunner.cs     🟢 KEEP  (Thực thi Strategy hiện hành của Tướng)
│   ├── 10_Flyweight/            🟢 KEEP  (Chia sẻ dữ liệu tĩnh WeaponLoadout / MapPoiDefinition cho hàng trăm thực thể)
│   ├── 11_DirtyFlag/            🟡 ADAPT (Chỉ tính toán lại vùng mạng / UI khi tọa độ thay đổi qua ngưỡng)
│   └── 12_TypeObject/           🟢 KEEP  (Định nghĩa chủng loại Vũ khí / Quái qua cấu hình dữ liệu)
│
└── Scripts/                     [64 C# Scripts - Khung sườn Ứng dụng]
    ├── Core/                    🟢 KEEP  (SceneLoader, ApplicationStateMachine)
    ├── EventChannels/           🟢 KEEP  (Kiến trúc kênh sự kiện tập trung)
    ├── StateMachine/            🟢 KEEP  (BaseState, StateMachine, Transition, FuncPredicate)
    └── UI/                      🟢 KEEP  (Quản lý hiển thị các bảng HUD & Modal)
```

---

## 3. Cách Áp Dụng Trực Tiếp Vào Game Demo *Survival Heroes 2.5D* (Godot 4.7 C#)

Toàn bộ các mẫu thiết kế được đánh nhãn **🟢 KEEP** và **🟡 ADAPT** ở trên, kết hợp cùng bộ cầu nối **Photon Cloud (`GodotPunBridge`)**, đã được hiện thực hóa thành mã nguồn chạy thực tế trong thư mục [`SurvivalHeroes_Godot4_Demo/`](../../SurvivalHeroes_Godot4_Demo/):

| Thành phần trong Game Demo (`SurvivalHeroes_Godot4_Demo`) | Mẫu Thiết Kế Áp Dụng (Từ 2 Tài Liệu) | Chức năng Thực Tế Trong Trận Đấu |
| :--- | :--- | :--- |
| [`SolidAndDesignPatterns25D.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/Patterns/SolidAndDesignPatterns25D.cs) | **ISP** (`IDamageable`, `IExplodable`), **DIP** (`ISwitchable`), **Command** (`InputBufferQueue`), **State** (`HeroStateMachine`), **ObjectPool** (`GodotNodePool<T>`), **Observer** (`ArenaEventBus`) | Đệm phím combo 180ms (`Q/W/E/LMB`), Thùng thuốc nổ dây chuyền, Trụ hồi máu cổ đại, Pool chữ số sát thương nảy 3D. |
| [`WeaponSkillSystem.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/Gameplay/WeaponSkillSystem.cs) | **Strategy Pattern** + **Flyweight / TypeObject** | Nhặt vũ khí nào (**Đại Kiếm**, **Cung Tuyệt Mệnh**, **Trượng Hỏa Long**) lập tức thay đổi toàn bộ 4 kỹ năng `Chuột Trái`, `Q`, `W`, `E`. |
| [`SurvivalHeroesWorldMap.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/Gameplay/SurvivalHeroesWorldMap.cs) | **Flyweight** (`MapPoiDefinition`) + **DIP** (`MysteryShopTent3D : ISwitchable`) + **DirtyFlag / Custom Draw** | Tái hiện bản đồ **170m $\times$ 170m** gồm **22 Địa Danh Tiếng Việt** (*Quảng Trường Trung Tâm*, *Tàng Kim Trấn*, *Giáo Đường Huyết Sắc*, *Thần Điện Hắc Ám*...), **3 Lều Cửa Hàng Thần Bí**, **Đường Bay & Nhảy Dù**, **Đánh dấu tọa độ (`ViiShiin`)**. |
| [`GodotPhotonService.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/PhotonBridge/GodotPhotonService.cs) & [`GodotPunUtilities.cs`](../../SurvivalHeroes_Godot4_Demo/Scripts/PhotonBridge/GodotPunUtilities.cs) | **Singleton** (`Autoload`) + **Spatial Culling** (`GodotCullArea` 9 ô lưới) + **ServerTimestamp Timer** (`GodotSafeZoneTimer`) | Đồng bộ vị trí/kỹ năng qua `[PunRPC]` & `PhotonStream`, chia 9 ô lưới giảm 80% băng thông mạng, và đồng bộ Vòng Bo Độc theo giờ máy chủ Photon. |
