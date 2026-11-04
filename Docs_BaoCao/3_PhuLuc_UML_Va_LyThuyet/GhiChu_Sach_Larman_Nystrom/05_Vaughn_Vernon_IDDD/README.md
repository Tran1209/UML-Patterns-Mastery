# 📚 Implementing Domain-Driven Design (IDDD) - Vaughn Vernon
## Kho Kiến Thức Bóc Tách Chuyên Sâu Toàn Tập (14 Chương & Phụ Lục A)

> **Tác giả:** Vaughn Vernon ("Cuốn Sách Đỏ" kinh điển của trường phái DDD thực chiến)  
> **Ánh xạ thực nghiệm:** Kiến trúc Game 2.5D OOP Nhiều người chơi (`UML-Patterns-Mastery`)  
> **Quy chuẩn bóc tách:** Mỗi chương được giải phẫu theo cấu trúc 5 phần: Đề mục & Vấn đề cốt lõi · Lý thuyết & Đánh đổi kiến trúc · Bảng mổ xẻ Code chi tiết · Sơ đồ Mermaid trực quan · Ánh xạ Game OOP.

---

### 📑 Tài liệu Toàn tập (Master Merged Edition)
* 📖 **Bản đọc liền toàn bộ 14 Chương & Phụ lục A (~361 KB):** [00_TOAN_TAP_IDDD_14_Chuong.md](./00_TOAN_TAP_IDDD_14_Chuong.md)

---

### 📋 Danh mục 10 Đợt Bóc Tách Chi Tiết

| Đợt | Chương & Chủ đề | File chi tiết | Trọng tâm phân tích |
| :-: | :--- | :--- | :--- |
| **01** | **Chương 1 & Chương 2** | [Dot_01_Chuong_1_Va_2_GettingStarted_BoundedContexts.md](./Dot_01_Chuong_1_Va_2_GettingStarted_BoundedContexts.md) | Bảng điểm *DDD Scorecard*, chữa bệnh *Anemic Model*, *Ubiquitous Language*, phân rã *Subdomains (Core/Supporting/Generic)* sang *Bounded Contexts* của SaaSOvation. |
| **02** | **Chương 3 & Chương 4 (P1)** | [Dot_02_Chuong_3_Va_4P1_ContextMaps_Architecture.md](./Dot_02_Chuong_3_Va_4P1_ContextMaps_Architecture.md) | 9 mẫu quan hệ *Context Mapping* (Upstream/Downstream), *Anticorruption Layer (ACL)*, *Dependency Inversion (DIP)* & *Hexagonal Architecture (Ports and Adapters)*. |
| **03** | **Chương 4 (P2)** | [Dot_03_Chuong_4P2_CQRS_EDA_Sagas_EventSourcing_DataFabric.md](./Dot_03_Chuong_4P2_CQRS_EDA_Sagas_EventSourcing_DataFabric.md) | Kiến trúc *CQRS*, *Event-Driven Architecture*, *Long-Running Processes (Sagas)* với Correlation ID, *Event Sourcing* và *Data Fabric*. |
| **04** | **Chương 5 & Chương 6** | [Dot_04_Chuong_5_Va_6_Entities_ValueObjects.md](./Dot_04_Chuong_5_Va_6_Entities_ValueObjects.md) | 4 chiến lược sinh định danh Entity, *Surrogate ID* trong Layer Supertype, 6 đặc trưng vàng của *Value Object* (Bất biến, Không tác dụng phụ, lưu ORM chống rò rỉ). |
| **05** | **Chương 7 & Chương 8** | [Dot_05_Chuong_7_Va_8_Services_DomainEvents.md](./Dot_05_Chuong_7_Va_8_Services_DomainEvents.md) | *Domain Service* (Stateless) vs. *Application Service*, thiết kế *Domain Event*, cơ chế phát sự kiện *ThreadLocal* và mẫu *Transactional Outbox (EventStore)*. |
| **06** | **Chương 9 & Chương 10 (P1)** | [Dot_06_Chuong_9_Va_10P1_Modules_AggregatesRules.md](./Dot_06_Chuong_9_Va_10P1_Modules_AggregatesRules.md) | Phân chia *Modules/Packages* theo miền, mổ xẻ lỗi *Large-Cluster Aggregate*, và **4 Quy tắc vàng Aggregate (Rules of Thumb)**. |
| **07** | **Chương 10 (P2)** | [Dot_07_Chuong_10P2_Aggregates_BreakRules_BOTE_Impl.md](./Dot_07_Chuong_10P2_Aggregates_BreakRules_BOTE_Impl.md) | Lý do phá luật Aggregate, tính toán định lượng *BOTE Calculations*, nguyên lý *Law of Demeter*, *Tell Don't Ask*, cấm tiêm DI vào Aggregate. |
| **08** | **Chương 11 & Chương 12** | [Dot_08_Chuong_11_Va_12_Factories_Repositories.md](./Dot_08_Chuong_11_Va_12_Factories_Repositories.md) | Factory Method trên Aggregate Root, *Collection-Oriented Repositories (Hibernate Unit of Work)* vs. *Persistence-Oriented Repositories (NoSQL save)*. |
| **09** | **Chương 13** | [Dot_09_Chuong_13_IntegratingBoundedContexts.md](./Dot_09_Chuong_13_IntegratingBoundedContexts.md) | Tích hợp Bounded Contexts qua REST và Messaging, 8 ảo tưởng mạng, *Process State Machine* kết hợp *Time-out Tracker* kích hoạt bù trừ. |
| **10** | **Chương 14 & Phụ lục A** | [Dot_10_Chuong_14_Va_AppendixA_Application_A_ES.md](./Dot_10_Chuong_14_Va_AppendixA_Application_A_ES.md) | Application Service điều phối, xuất trạng thái qua *Mediator Double-Dispatch*, và kiến trúc *A+ES (Aggregates + Event Sourcing)* với pha execute vs. apply. |
