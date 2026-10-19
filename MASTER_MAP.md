# 🗺️ BẢN ĐỒ TỔNG THỂ DỰ ÁN (MASTER MAP)
> **Đọc file này trước khi làm bất cứ thứ gì trong dự án!**

---

## 🏛️ DỰ ÁN LÀ GÌ? (1 DÒNG)

> Xây dựng **Game Multiplayer 2D đấu trường bằng Java** theo đúng barem báo cáo của HUCE — vừa ăn trọn điểm môn OOP, vừa dùng kỹ thuật hiện đại thực chiến năm 2026.

---

## 🧱 BỨC TRANH LỚN: 3 TẦNG

```
┌─────────────────────────────────────────────────────┐
│ TẦNG 3: BÁO CÁO ĐẠI HỌC (Nộp thầy cô chấm điểm)   │
│  → 7 Chương chuẩn HUCE · ~100 trang · Mermaid UML   │
├─────────────────────────────────────────────────────┤
│ TẦNG 2: KIẾN THỨC LÝ THUYẾT (Hiểu để viết báo cáo)  │
│  → 5 Cuốn sách kinh điển · 9 GRASP · GoF · Netcode  │
├─────────────────────────────────────────────────────┤
│ TẦNG 1: MÃ NGUỒN JAVA (Thực thể chạy được)          │
│  → 5 Module · 5 Nhánh Git · JUnit Test · Java 17    │
└─────────────────────────────────────────────────────┘
```

---

## 👥 AI LÀM GÌ? (5 NGƯỜI = 5 VIỆC = 5 CUỐN SÁCH)

| Người | Việc chính | Sách học | Module code | Nhánh Git |
|:---:|:---|:---|:---|:---|
| **Nam** ★ | Kiến trúc tổng thể, Core Loop, State Machine, tổng hợp báo cáo | Craig Larman + Vaughn Vernon | `Src_Game/Core/` | `main` |
| **Kiên** | Server 20Hz, Socket, Gói tin DTO, LERP chống giật hình | Glazer & Madhav | `Src_Game/Server/` `Src_Game/Network/` | `feature/network-server` |
| **Anh** | Hitbox AABB, Strategy Damage, Spatial Grid va chạm | Robert Nystrom | `Src_Game/Combat/` | `feature/combat-system` |
| **Huy** | Canvas 60fps, HUD, Input Adapter, Observer Event | Robert Nystrom + GoF | `Src_Game/UI/` | `feature/client-ui` |
| **Hoàng** | Lưu dữ liệu JSON, JUnit Test, Đo đạc Jitter | Martin Fowler | `Src_Game/Persistence/` `Tests/` | `feature/data-testing` |

---

## 📚 5 CUỐN SÁCH — MỖI CUỐN GIẢI QUYẾT 1 VẤN ĐỀ CỤ THỂ

| # | Tác giả | Cuốn sách | Giải quyết vấn đề gì trong đồ án? |
|:---:|:---|:---|:---|
| 1 | **Craig Larman** | *Applying UML & Patterns* | Cấu trúc báo cáo đúng barem, 9 GRASP phân vai lớp, Domain Model vs DCD |
| 2 | **Robert Nystrom** | *Game Programming Patterns* | Game Loop tách render/update, Component thay kế thừa sâu, Grid va chạm $O(N)$ |
| 3 | **Joshua Glazer & Sanjay Madhav** | *Multiplayer Game Programming* | Server Authoritative chống hack, LERP 20Hz→60fps chống giật hình |
| 4 | **Martin Fowler** | *Patterns of Enterprise Application Architecture* | Data Mapper tách domain khỏi Database, Repository Pattern |
| 5 | **Vaughn Vernon** | *Implementing Domain-Driven Design* | Aggregate Root đóng gói GameRoom, Value Object bất biến |

---

## 📑 7 CHƯƠNG BÁO CÁO — ĐÃ LÀM ĐẾN ĐÂU?

| # | Chương | File | Tình trạng |
|:---:|:---|:---|:---:|
| 1 | Phát biểu bài toán & FURPS+ | `Docs_BaoCao/Phan_01_PhatBieu_BaiToan.md` | ✅ Hoàn thành |
| 2 | Phân tích OOA (Use Case, SSD, Domain Model) | `Docs_BaoCao/Phan_02_PhanTich_OOA.md` | ✅ Hoàn thành |
| 3 | Thiết kế OOD (9 GRASP, DCD, GoF, Deployment) | `Docs_BaoCao/Phan_03_...` | 🔲 Chưa làm |
| 4 | Cài đặt OOP + Netcode Java | `Docs_BaoCao/Phan_04_...` | 🔲 Chưa làm |
| 5 | Kiểm thử & Đo lường Jitter | `Docs_BaoCao/Phan_05_...` | 🔲 Chưa làm |
| 6 | Kết luận & Phản biện Larman vs Hiện đại | `Docs_BaoCao/Phan_06_...` | 🔲 Chưa làm |
| 7 | Tài liệu tham khảo (5 cuốn sách) | Đính kèm cuối | 🔲 Chưa làm |

---

## ⚠️ 3 VẤN ĐỀ KỸ THUẬT TRỌNG TÂM CẦN GIẢI QUYẾT

### Vấn đề 1: Giật hình (Micro-stutter / Jitter) ← **ĐIỂM SÁng nhất của báo cáo**
- **Nguyên nhân**: Server gửi 20Hz (50ms/gói), Client render 60fps (16ms/khung) → nhân vật nhảy cóc
- **Giải pháp**: Thuật toán LERP trong `SnapshotInterpolator.java`
$$P_{render} = P_{old} + (P_{new} - P_{old}) \times \alpha \quad (\alpha = \frac{t_{now} - t_{old}}{t_{new} - t_{old}})$$
- **Ai làm**: Kiên (Server) + Huy (Client Canvas)

### Vấn đề 2: Va chạm chậm nếu nhiều nhân vật
- **Nguyên nhân**: Kiểm tra tất cả vs tất cả = $O(N^2)$
- **Giải pháp**: Chia bản đồ thành ô lưới `SpatialGridAoI.java` → chỉ test trong ô lân cận = $O(N)$
- **Ai làm**: Anh (Combat)

### Vấn đề 3: Client gian lận / hack tốc độ
- **Nguyên nhân**: Client tự tính vị trí và gửi lên Server
- **Giải pháp**: Server nắm quyền tuyệt đối (Authoritative), Client chỉ gửi *ý định* (`InputCommandPacket`)
- **Ai làm**: Kiên (Server)

---

## 🗂️ CẤU TRÚC THƯ MỤC (Biết file nào ở đâu)

```
E:\UML-Patterns-Mastery\
│
├── 📋 DASHBOARD.md              ← Trang chủ Obsidian (Mở đây trước)
├── 📋 Architecture_Canvas.canvas ← Sơ đồ kiến trúc 2D trực quan
│
├── 📚 01_Report_Notes\          ← 8 chuyên đề nghiên cứu lý thuyết
│   ├── 01_Process_and_UML.md    (Craig Larman: UP & UML)
│   ├── 02_GRASP_9_Principles.md (9 Nguyên lý GRASP)
│   ├── 03_GoF_Patterns_Applied.md (Strategy, Observer, State...)
│   ├── 05_Martin_Fowler_PoEAA.md (Data Mapper)
│   ├── 06_Domain_Driven_Design.md (DDD Aggregates)
│   ├── 07_Robert_Nystrom_Game_Programming_Patterns.md ← MỚI
│   └── 08_Glazer_Madhav_Multiplayer_Netcode.md ← MỚI
│
├── 📝 Docs_BaoCao\              ← Báo cáo học thuật
│   ├── 00_Trang_Bia_BaoCao_HUCE.md  ← Trang bìa (5 thành viên)
│   ├── Trang_Bia_HUCE.html          ← Bìa in được (Chrome → PDF)
│   ├── NhatKy_DuAn.md               ← Nhật ký & RACI (✅ Đầy đủ)
│   ├── Phan_01_PhatBieu_BaiToan.md  ← Chương 1 (✅ Hoàn thành)
│   ├── HocPhan_01_NenTang_OOAD.md   ← Lý thuyết nền OOAD & UP
│   └── images\
│       ├── Bia_BaoCao_Technical_Report.jpg  ← Ảnh bìa (Chiến binh)
│       └── Bia_BaoCao_OOP_Architecture.jpg  ← Ảnh bìa (Thuần công nghệ)
│
├── 💻 Src_Game\                 ← Mã nguồn Java (5 module)
│   ├── Core\       ← NAM: GameLoop, PlayerStateMachine
│   ├── Server\     ← KIÊN: Dedicated Server 20Hz
│   ├── Network\    ← KIÊN: Packet DTO, LERP Interpolator
│   ├── Combat\     ← ANH: Hitbox AABB, Strategy Damage
│   ├── UI\         ← HUY: Canvas 60fps, HUD Observer
│   └── Persistence\ ← HOÀNG: Data Mapper, JSON Save
│
└── 🧪 Tests\                    ← HOÀNG: JUnit 5 Test Suite
```

---

## 🚀 VIỆC CẦN LÀM TIẾP THEO (THEO THỨ TỰ ƯU TIÊN)

### Tuần này (Tuần 1 — Pha Inception):
- [x] Git repository & cấu trúc thư mục
- [x] Trang bìa HUCE & Nhật ký dự án  
- [x] Chương 1: Phát biểu bài toán & FURPS+
- [ ] **Chốt tên game** (Còn chưa quyết định!)
- [ ] Điền Mã Sinh Viên và Họ tên đầy đủ của 5 người vào `NhatKy_DuAn.md`
- [ ] Mời 4 thành viên vào GitHub repo `Tran1209/UML-Patterns-Mastery`

### Tuần 2-3 (Iteration 1 — Elaboration):
- [ ] Chương 2: Use Case `UC-01` Fully Dressed + Domain Model (không method!)
- [ ] Code Java: `Player.java`, `GameLoop.java`, `PlayerStateMachine.java`
- [ ] Server Socket cơ bản kết nối 2 Client

### Tuần 4-5 (Iteration 2):
- [ ] Chương 3: DCD + 9 GRASP + Deployment Diagram
- [ ] Code: Combat Engine, LERP Interpolator, HUD Observer
- [ ] JUnit Test: Va chạm AABB + Benchmark Jitter

### Tuần 6-7 (Hoàn thiện & Nộp):
- [ ] Chương 4, 5, 6 + Tổng hợp 100 trang
- [ ] Quay video demo 4 máy kết nối

---

## 💬 QUY TẮC VÀNG CỦA NHÓM (3 NGUYÊN TẮC BẤT DI BẤT DỊCH)

1. **"Không ai sửa code của người khác"** — Mỗi người một thư mục riêng, một nhánh Git riêng.
2. **"Code trước, test ngay"** — Mỗi Class Java mới viết xong là phải có ít nhất 1 JUnit test đi kèm.
3. **"Viết báo cáo song song với code"** — Tuần nào code gì thì ghi vào chương báo cáo tương ứng ngay trong tuần đó, không để dồn cuối kỳ.
