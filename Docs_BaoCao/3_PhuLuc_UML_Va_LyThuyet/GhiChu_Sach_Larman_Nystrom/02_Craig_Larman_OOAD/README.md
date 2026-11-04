# Applying UML and Patterns (3rd Edition) — Craig Larman
## Trích Xuất Lý Thuyết Toàn Diện 38 Chương (100% Hoàn Thành)

> **Tác phẩm:** *Applying UML and Patterns: An Introduction to Object-Oriented Analysis and Design and Iterative Development (3rd Edition)*  
> **Tác giả:** Craig Larman  
> **Thành viên phụ trách học thuật:** **Trung Kiên** *(Trưởng nhóm)* & **Duy Nam** *(Lead Software Architect)* — Nhóm 1 (HUCE OOP 2026)  
> **Quy mô:** Toàn bộ 38 Chương chuyên sâu (~532 KB), phân bổ qua 5 Phần chuẩn mực của Agile Unified Process (UP), 9 Mẫu GRASP, Mẫu GoF, và Ánh xạ từ Thiết kế sang Mã nguồn Java.

---

### 📖 Bản Gộp Toàn Văn Hoàn Chỉnh
- **File tổng hợp toàn diện duy nhất**: [`FULL_Craig_Larman_Applying_UML_and_Patterns.md`](./FULL_Craig_Larman_Applying_UML_and_Patterns.md) *(~532 KB, gộp đầy đủ 38 chương, loại bỏ điều hướng rời rạc, hỗ trợ tìm kiếm Ctrl+F xuyên suốt toàn bộ giáo trình)*

---

### 📂 Cấu Trúc 38 Chương Chuyên Sâu (Thư mục `38_Chapters/`)

#### PHẦN I: GIỚI THIỆU & NỀN TẢNG (INTRODUCTION & FOUNDATIONS)
| Chương | Tập tin chi tiết | Trọng tâm học thuật & Kỹ thuật |
| :---: | :--- | :--- |
| **01** | [`01_Object_Oriented_Analysis_and_Design.md`](./38_Chapters/01_Object_Oriented_Analysis_and_Design.md) | Bản chất OOA/D, Phân bổ trách nhiệm, Cầu nối ngữ nghĩa giữa Problem Space & Solution Space, Ngăn chặn Blob Anti-pattern. |
| **02** | [`02_Iterative_Development_and_the_Unified_Process.md`](./38_Chapters/02_Iterative_Development_and_the_Unified_Process.md) | Phát triển Lặp cuốn chiếu (Iterative), Agile Unified Process (UP), Quản trị rủi ro cao trước (Risk-Driven). |
| **03** | [`03_Case_Study_The_NextGen_POS_System.md`](./38_Chapters/03_Case_Study_The_NextGen_POS_System.md) | Bài toán mẫu NextGen POS & Game Cờ tỷ phú (Monopoly), Phân tích kiến trúc đa thiết bị và chịu tải. |

#### PHẦN II: KHỞI TẠO DỰ ÁN (INCEPTION PHASE)
| Chương | Tập tin chi tiết | Trọng tâm học thuật & Kỹ thuật |
| :---: | :--- | :--- |
| **04** | [`04_Inception.md`](./38_Chapters/04_Inception.md) | Khởi tạo dự án Inception, Xác định tính khả thi, Ước lượng thô, Tránh bẫy sa đà vào viết yêu cầu thác nước (Waterfall trap). |
| **05** | [`05_Understanding_Requirements.md`](./38_Chapters/05_Understanding_Requirements.md) | Phân loại yêu cầu phần mềm theo mô hình FURPS+ (Functionality, Usability, Reliability, Performance, Supportability). |
| **06** | [`06_Use_Case_Model_Writing_Requirements_in_Context.md`](./38_Chapters/06_Use_Case_Model_Writing_Requirements_in_Context.md) | Viết Use Case chuẩn mực UP (Brief, Casual, Fully Dressed), Luồng chính (Main Success) & Luồng rẽ nhánh ngoại lệ. |
| **07** | [`07_Identifying_Other_Requirements.md`](./38_Chapters/07_Identifying_Other_Requirements.md) | Đặc tả Bổ sung (Supplementary Specification), Thuật ngữ nghiệp vụ (Glossary), Quy tắc Nghiệp vụ (Business Rules). |

#### PHẦN III: TINH CHỈNH - LẶP 1 (ELABORATION ITERATION 1: CƠ BẢN)
| Chương | Tập tin chi tiết | Trọng tâm học thuật & Kỹ thuật |
| :---: | :--- | :--- |
| **08** | [`08_From_Inception_to_Elaboration.md`](./38_Chapters/08_From_Inception_to_Elaboration.md) | Chuyển dịch từ Inception sang Elaboration: Xây dựng khung kiến trúc cốt lõi (Architectural Baseline), Giảm thiểu rủi ro cao nhất. |
| **09** | [`09_Use_Case_Model_Drawing_System_Sequence_Diagrams.md`](./38_Chapters/09_Use_Case_Model_Drawing_System_Sequence_Diagrams.md) | Sơ đồ Tuần tự Hệ thống (SSD), Coi hệ thống như Hộp đen (Black Box), Xác định các Sự kiện Hệ thống (System Events). |
| **10** | [`10_Domain_Models_Visualizing_Concepts.md`](./38_Chapters/10_Domain_Models_Visualizing_Concepts.md) | Mô hình Miền (Domain Model), Khái niệm trực quan thế giới thực, Danh sách Phân loại Khái niệm (Category List). |
| **11** | [`11_Domain_Model_Adding_Associations.md`](./38_Chapters/11_Domain_Model_Adding_Associations.md) | Bổ sung Quan hệ Kết hợp (Associations), Tiêu chuẩn "Cần phải ghi nhớ" (Need-to-remember), Tránh bẫy vẽ quá nhiều đường nối. |
| **12** | [`12_Domain_Model_Adding_Attributes.md`](./38_Chapters/12_Domain_Model_Adding_Attributes.md) | Thuộc tính miền (Attributes), Kiểu dữ liệu nguyên thủy vs Khái niệm lớp, Khi nào nâng cấp thuộc tính thành Class riêng. |
| **13** | [`13_Use_Case_Model_Adding_Detail_with_Operation_Contracts.md`](./38_Chapters/13_Use_Case_Model_Adding_Detail_with_Operation_Contracts.md) | Khế ước Hoạt động (Operation Contracts), Tiền điều kiện (Preconditions) và Hậu điều kiện (Postconditions) thay đổi trạng thái. |
| **14** | [`14_From_Requirements_to_Design_in_this_Iteration.md`](./38_Chapters/14_From_Requirements_to_Design_in_this_Iteration.md) | Cầu nối từ Yêu cầu sang Thiết kế OOD: Tinh thần kiến trúc lặp, Không vẽ hình vô nghĩa, Chuyển dịch sang mô hình tương tác. |
| **15** | [`15_Interaction_Diagram_Notation.md`](./38_Chapters/15_Interaction_Diagram_Notation.md) | Cú pháp Sơ đồ Tương tác UML: So sánh Sequence Diagrams vs Communication Diagrams, Ký hiệu vòng lặp, rẽ nhánh, kích hoạt. |
| **16** | [`16_GRASP_Designing_Objects_with_Responsibilities.md`](./38_Chapters/16_GRASP_Designing_Objects_with_Responsibilities.md) | **9 Nguyên tắc GRASP Cốt lõi**: Creator, Information Expert, Low Coupling, High Cohesion, Controller. |
| **17** | [`17_Design_Model_Use_Case_Realizations_with_GRASP.md`](./38_Chapters/17_Design_Model_Use_Case_Realizations_with_GRASP.md) | Hiện thực hóa Use Case bằng GRASP: Phân tích luồng `makeNewSale`, `enterItem`, `endSale`, `makePayment`. |
| **18** | [`18_Design_Model_Determining_Visibility.md`](./38_Chapters/18_Design_Model_Determining_Visibility.md) | Xác định Tầm nhìn (Visibility) giữa các đối tượng: Attribute, Parameter, Local, Global Visibility trong OOP. |
| **19** | [`19_Design_Model_Creating_Design_Class_Diagrams.md`](./38_Chapters/19_Design_Model_Creating_Design_Class_Diagrams.md) | Tạo Sơ đồ Lớp Thiết kế (DCDs): Khác biệt DCD với Domain Model, Thêm phương thức, kiểu dữ liệu, quan hệ phụ thuộc. |
| **20** | [`20_Implementation_Model_Mapping_Designs_to_Code.md`](./38_Chapters/20_Implementation_Model_Mapping_Designs_to_Code.md) | Ánh xạ Thiết kế sang Mã nguồn Java/C#: Tạo lớp, thuộc tính, khởi tạo tham chiếu, phương thức xử lý nghiệp vụ theo DCD. |

#### PHẦN IV: TINH CHỈNH - LẶP 2 (ELABORATION ITERATION 2: MẪU HÌNH BỔ SUNG)
| Chương | Tập tin chi tiết | Trọng tâm học thuật & Kỹ thuật |
| :---: | :--- | :--- |
| **21** | [`21_Iteration_2_and_Its_Requirements.md`](./38_Chapters/21_Iteration_2_and_Its_Requirements.md) | Bắt đầu Vòng lặp 2: Xử lý các biến thể thanh toán, giảm giá, kết nối dịch vụ ngoài (External Accounting/Inventory Services). |
| **22** | [`22_GRASP_More_Patterns_for_Assigning_Responsibilities.md`](./38_Chapters/22_GRASP_More_Patterns_for_Assigning_Responsibilities.md) | **4 Mẫu GRASP Nâng cao**: Polymorphism, Pure Fabrication, Indirection, Protected Variations (Bảo vệ biến đổi). |
| **23** | [`23_Designing_Use_Case_Realizations_with_GoF_Patterns.md`](./38_Chapters/23_Designing_Use_Case_Realizations_with_GoF_Patterns.md) | Ứng dụng GoF Design Patterns: Adapter (kết nối máy in, cổng thanh toán), Factory (tạo đối tượng an toàn), Singleton, Strategy, Composite. |
| **24** | [`24_Iteration_3_and_Its_Requirements.md`](./38_Chapters/24_Iteration_3_and_Its_Requirements.md) | Khởi động Vòng lặp 3: Các trường hợp biên phức tạp, hoàn trả hàng, xử lý ngoại lệ, đồng bộ dữ liệu. |
| **25** | [`25_Relating_Use_Cases.md`](./38_Chapters/25_Relating_Use_Cases.md) | Quan hệ giữa các Use Case: Quan hệ Bao gồm (`<<include>>`), Mở rộng (`<<extend>>`), và Kế thừa Khái quát hóa (`Generalization`). |

#### PHẦN V: TINH CHỈNH - LẶP 3 & KIẾN TRÚC PHẦN MỀM NÂNG CAO
| Chương | Tập tin chi tiết | Trọng tâm học thuật & Kỹ thuật |
| :---: | :--- | :--- |
| **26** | [`26_Modeling_Generalization.md`](./38_Chapters/26_Modeling_Generalization.md) | Mô hình hóa Kế thừa & Khái quát hóa: Định lý 100% và Is-a Rule, Phân lớp hành vi vs Phân lớp dữ liệu. |
| **27** | [`27_Refining_the_Domain_Model.md`](./38_Chapters/27_Refining_the_Domain_Model.md) | Tinh chỉnh Domain Model: Lớp liên kết (Association Classes), Thuộc tính dẫn xuất, Kiểu dữ liệu cấu trúc (Data Types). |
| **28** | [`28_Adding_New_SSDs_and_Contracts.md`](./38_Chapters/28_Adding_New_SSDs_and_Contracts.md) | Bổ sung SSDs và Khế ước hoạt động cho Vòng lặp 3: Xử lý nghiệp vụ hoàn tiền, xử lý mã giảm giá phức hợp. |
| **29** | [`29_Modeling_Behavior_in_Statechart_Diagrams.md`](./38_Chapters/29_Modeling_Behavior_in_Statechart_Diagrams.md) | Sơ đồ Máy trạng thái (Statechart Diagrams): State-dependent objects, Sự kiện (Events), Chuyển trạng thái (Transitions), Hành động (Actions). |
| **30** | [`30_Designing_the_Logical_Architecture_with_Patterns.md`](./38_Chapters/30_Designing_the_Logical_Architecture_with_Patterns.md) | Thiết kế Kiến trúc Logic: Kiến trúc Phân tầng (Layered Architecture), Mối quan hệ giữa Tầng Giao diện (UI), Tầng Nghiệp vụ (Domain), Tầng Kỹ thuật (Technical Services). |
| **31** | [`31_Organizing_Design_and_Implementation_Model_Packages.md`](./38_Chapters/31_Organizing_Design_and_Implementation_Model_Packages.md) | Đóng gói Gói UML (Package Diagrams): Tổ chức package mã nguồn theo tầng kiến trúc, Tránh phụ thuộc vòng giữa các package. |
| **32** | [`32_Introduction_to_Architectural_Analysis_and_the_SAD.md`](./38_Chapters/32_Introduction_to_Architectural_Analysis_and_the_SAD.md) | Phân tích Kiến trúc & Tài liệu Đặc tả Kiến trúc (SAD): Xác định các yếu tố kiến trúc trọng yếu (Architectural Factors). |
| **33** | [`33_Designing_for_More_Architectural_Patterns.md`](./38_Chapters/33_Designing_for_More_Architectural_Patterns.md) | Các Mẫu Kiến trúc Mở rộng: Observer Pattern trong việc cập nhật giao diện không ghép nối, Publish-Subscribe. |
| **34** | [`34_Designing_a_Persistence_Framework_with_Patterns.md`](./38_Chapters/34_Designing_a_Persistence_Framework_with_Patterns.md) | Thiết kế Khung Lưu trữ Dữ liệu (Persistence Framework): Tách rời Database khỏi Domain Model, Mẫu Mapper, Broker. |
| **35** | [`35_On_Drawing_and_Tools.md`](./38_Chapters/35_On_Drawing_and_Tools.md) | Triết lý vẽ sơ đồ: Công cụ UML, Vẽ phác thảo trên bảng trắng (Agile Modeling) vs Công cụ CASE tự động hóa. |
| **36** | [`36_Introduction_to_Iterative_Planning_and_Project_Issues.md`](./38_Chapters/36_Introduction_to_Iterative_Planning_and_Project_Issues.md) | Lập kế hoạch Lặp và Quản trị Dự án: Timeboxing, Phân bổ nguồn lực, Đánh giá tiến độ theo phần mềm chạy được. |
| **37** | [`37_Comments_on_Iterative_Development_and_the_UP.md`](./38_Chapters/37_Comments_on_Iterative_Development_and_the_UP.md) | Bình luận & Tổng kết về UP: Triết lý Agile, Những sai lầm phổ biến khi áp dụng OOA/D trong công nghiệp phần mềm. |
| **38** | [`38_More_UML_Notation.md`](./38_Chapters/38_More_UML_Notation.md) | Ký hiệu UML Bổ sung: Template Classes, Activity Diagrams, Component Diagrams, Deployment Diagrams mở rộng. |

---

### 🧠 Bảng Tra Cứu Nhanh 9 Mẫu GRASP trong Sách Craig Larman

| Mẫu GRASP | Bản chất kỹ thuật & Trách nhiệm | Chương phân tích cốt lõi |
| :--- | :--- | :---: |
| **Information Expert** | Gán trách nhiệm cho lớp nắm giữ thông tin cần thiết nhất để hoàn thành trách nhiệm đó. | Chương 16, 17 |
| **Creator** | Gán trách nhiệm tạo đối tượng $B$ cho lớp $A$ nếu $A$ chứa, tổng hợp, hoặc sử dụng chặt chẽ $B$. | Chương 16, 17 |
| **Low Coupling** | Phân bổ trách nhiệm sao cho sự phụ thuộc giữa các lớp ở mức thấp nhất, tăng khả năng tái sử dụng. | Chương 16 |
| **High Cohesion** | Giữ các lớp tập trung vào một nhiệm vụ cụ thể, rõ ràng, không phân tán logic lan man. | Chương 16 |
| **Controller** | Lớp đầu tiên đón nhận và điều phối sự kiện hệ thống (System Event) từ tầng UI vào Domain. | Chương 16, 17 |
| **Polymorphism** | Xử lý các biến thể hành vi dựa trên đa hình kiểu đối tượng thay vì chuỗi `if-else` / `switch-case`. | Chương 22 |
| **Pure Fabrication** | Tạo một lớp nhân tạo không có trong thế giới thực (như `Repository`, `Validator`) để bảo toàn High Cohesion. | Chương 22 |
| **Indirection** | Thêm một đối tượng trung gian (như Adapter, Broker) để ngắt ghép nối trực tiếp giữa hai thành phần. | Chương 22 |
| **Protected Variations** | Bảo vệ hệ thống khỏi những biến đổi bằng cách bọc vùng biến đổi sau một Interface/Abstraction ổn định. | Chương 22 |

---

### 🎮 Ánh Xạ OOA/D Craig Larman Vào Dự Án Game 2D Action RPG (Nhóm 1)
1. **Controller Pattern:** Lớp `GameLoop` và `GameEngine` đóng vai trò Controller trung gian nhận input chuột/bàn phím từ người chơi, không xử lý đồ họa mà phân bổ xuống `CharacterStateMachine` và `CombatManager`.
2. **Information Expert:** Trách nhiệm tính toán lượng máu mất và hiệu ứng đẩy lùi được gán trực tiếp cho `HitboxManager` và `StatsComponent` – nơi nắm giữ tọa độ AABB và chỉ số giáp/kháng phép.
3. **Protected Variations & Indirection:** Giao tiếp qua `ISkillAction`, `INetworkRelay`, `IMatchRepository` giúp tầng logic chiến đấu hoàn toàn độc lập với việc chơi Offline hay kết nối Dedicated Server qua Socket TCP/UDP.
4. **Pure Fabrication:** Các lớp quản lý kỹ thuật như `ObjectPool<T>`, `SpatialGrid`, `Camera2D` là các tạo phẩm thuần túy giúp tối ưu bộ nhớ và số phép tính va chạm từ $O(N^2)$ xuống $O(N)$.

---
[🏠 Quay lại Thư viện Lý thuyết Nền tảng (Master Index)](../README.md)
