# THƯ VIỆN LÝ THUYẾT NỀN TẢNG KIẾN TRÚC VÀ MẪU THIẾT KẾ (OOP & GAME ARCHITECTURE)
## Đồ án Lập trình Hướng đối tượng - Nhóm 1 (HUCE 2026)

Thư mục này chứa toàn bộ hệ thống lý thuyết chuyên sâu được trích xuất từ 5 giáo trình kinh điển quốc tế về Phân tích Thiết kế Hướng đối tượng (OOAD), Kiến trúc Game (Game Engine Architecture), Kiến trúc Mạng Đa người chơi (Multiplayer Netcode) và Thiết kế Hướng miền (DDD).

---

### 📂 Cấu trúc Thư mục 5 Đầu Sách Chuyên Khảo

| Thư mục | Tác phẩm & Tác giả | Quy mô & Phạm vi kiến thức | Trạng thái |
| :--- | :--- | :--- | :---: |
| [`01_Robert_Nystrom_GamePatterns/`](./01_Robert_Nystrom_GamePatterns/README.md) | **Game Programming Patterns**<br>*(Robert Nystrom)* | 10 Đợt trích xuất toàn diện (~320 KB markdown): Sequenced Patterns, Behavioral Patterns, Decoupling, Optimization. Kèm bản hợp nhất `FULL_Game_Programming_Patterns.md`. | **Hoàn thành (10/10 Runs)** |
| [`02_Craig_Larman_OOAD/`](./02_Craig_Larman_OOAD/README.md) | **Applying UML and Patterns (3rd Ed)**<br>*(Craig Larman)* | Toàn tập 38 Chương chuyên sâu (~532 KB markdown): 5 Phần Agile UP, 9 GRASP Principles, GoF Patterns, DCD, Mapping Designs to Code. Kèm bản hợp nhất `FULL_Craig_Larman_Applying_UML_and_Patterns.md`. | **Hoàn thành (38/38 Chương)** |
| [`03_Glazer_Madhav_Netcode/`](./03_Glazer_Madhav_Netcode/README.md) | **Multiplayer Game Programming**<br>*(Joshua Glazer & Sanjay Madhav)* | 10 Đợt trích xuất: Socket UDP/TCP, Packet Serialization, Client-Side Prediction, Server Reconciliation, Lag Compensation. | **Đang trích xuất (10 Runs)** |
| [`04_Martin_Fowler_PoEAA/`](./04_Martin_Fowler_PoEAA/README.md) | **Patterns of Enterprise Application Architecture**<br>*(Martin Fowler)* | 10 Đợt trích xuất: Domain Logic Patterns, Data Source Architectural Patterns, Object-Relational Behavioral/Structural Patterns. | **Đang trích xuất (10 Runs)** |
| [`05_Vaughn_Vernon_IDDD/`](./05_Vaughn_Vernon_IDDD/README.md) | **Implementing Domain-Driven Design**<br>*(Vaughn Vernon)* | 10 Đợt trích xuất toàn diện 14 Chương & Phụ lục (~365 KB): Bounded Context, Entities, Value Objects, Aggregates, Domain Events, Repositories, Event Sourcing. Kèm bản hợp nhất `00_TOAN_TAP_IDDD_14_Chuong.md`. | **Hoàn thành (10/10 Runs)** |
| [`_Ban_Tom_Tat_Cu_Tuan_Truoc/`](./_Ban_Tom_Tat_Cu_Tuan_Truoc/README.md) | **Lưu trữ ghi chú sơ khai tuần trước** | 8 file markdown tóm tắt nhanh tuần trước (01_... đến 08_...) phục vụ tra cứu đối chiếu lịch sử. | **Lưu trữ (Archive)** |

---

### 📌 Nguyên tắc Đóng góp Lý thuyết của Nhóm 1
1. **Chia nhỏ thành 10 Đợt chạy prompt**: Mỗi cuốn sách được phân tích thành 10 phần độc lập trong thư mục `10_Runs/` (tương tự như sách Nystrom).
2. **Tiêu chuẩn trích xuất**:
   - Trích xuất toàn văn bản chất lý thuyết, không tóm tắt hời hợt.
   - Giải thích cặn kẽ từng dòng code mẫu: mục đích, cơ chế con trỏ/bộ nhớ, luồng dữ liệu.
   - Minh họa sơ đồ trực quan (ASCII Art / Mermaid diagram) cho mọi cấu trúc và luồng điều khiển.
   - Phân tích ưu/nhược điểm (Design Trade-offs) và liên hệ thực tế vào project Game 2D Action RPG.
3. **Bản gộp hoàn chỉnh (Merged Full)**: Sau khi hoàn tất 10 đợt, gộp thành file `FULL_<TenSach>.md` đặt ngay tại thư mục gốc của sách đó.
