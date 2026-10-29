# PHẦN VII: TÀI LIỆU THAM KHẢO (ACADEMIC REFERENCES)

> **Môn học**: Lập trình Hướng đối tượng (OOP)  
> **Trường**: Đại học Xây dựng Hà Nội (HUCE) — Khoa Công nghệ Thông tin  
> **Nhóm sinh viên thực hiện**: Nam · Kiên · Anh · Huy · Hoàng  
> **Chuẩn trích dẫn**: IEEE / APA Academic Citation Standard  
> **Kho dữ liệu số**: Đã bóc tách toàn văn các chương trong `00_Raw_PDF/chapters_md/`

---

## 7.1. BỘ NGŨ TRỤ CỘT KỸ NGHỆ KINH ĐIỂN (THE 5 CORE TEXTBOOKS)

Dự án được xây dựng dựa trên sự tổng hòa lý thuyết và kỹ thuật của 5 tác phẩm kinh điển hàng đầu thế giới về kỹ nghệ phần mềm và lập trình trò chơi:

### [1] Craig Larman — Khung Phương pháp luận OOAD & 9 Nguyên lý GRASP
* **Tên tác phẩm**: *Applying UML and Patterns: An Introduction to Object-Oriented Analysis and Design and Iterative Development* (3rd Edition).
* **Nhà xuất bản**: Prentice Hall PTR, 2004. ISBN: 0-13-148906-2.
* **File nguồn**: `00_Raw_PDF/applying-uml-and-patterns.pdf` (Đã bóc tách 38 chương tại `00_Raw_PDF/_Knowledge_Base_Larman/chapters_extracted/`).
* **Đóng góp vào đồ án**:
  * Cung cấp toàn bộ khung quy trình 4 pha của **Unified Process (Inception, Elaboration, Construction, Transition)**.
  * Phương pháp mô hình hóa ca sử dụng chuẩn: Use Case Diagrams, Fully Dressed Use Cases, và System Sequence Diagrams (SSD).
  * Quy tắc vàng phân tầng: Phân biệt tuyệt đối giữa **Domain Model (thế giới thực, không methods)** và **Design Class Diagram (DCD với methods và kiểu dữ liệu)**.
  * **9 Nguyên lý GRASP**: Information Expert, Creator, Controller, Low Coupling, High Cohesion, Polymorphism, Pure Fabrication, Indirection, Protected Variations.

---

### [2] Robert Nystrom — Mẫu Thiết kế Game & Tối ưu Hóa Hiệu năng
* **Tên tác phẩm**: *Game Programming Patterns*.
* **Nhà xuất bản**: Genever Bench, 2014. ISBN: 978-0-9905829-0-8.
* **File nguồn**: `00_Raw_PDF/Game Programming Patterns.pdf` (604 trang, đã bóc tách 21 chương tại `00_Raw_PDF/_Knowledge_Base_Nystrom/chapters_extracted/`).
* **Đóng góp cụ thể theo chương**:
  * **Game Loop Pattern** *(Sequencing Patterns — Trang 133 – 147)*: Cài đặt vòng lặp trò chơi với Fixed Timestep cho vật lý logic và Variable Timestep cho vẽ đồ họa Canvas 60 FPS nội suy.
  * **Component Pattern** *(Decoupling Patterns — Trang 219 – 239)*: Triết lý thành phần hóa thay thế cây kế thừa sâu phân cấp (Composition over Inheritance).
  * **Spatial Partition Pattern** *(Optimization Patterns — Trang 325 – 355)*: Thuật toán ô lưới không gian (Spatial Grid AoI) hạ độ phức tạp tính toán va chạm từ $O(N^2)$ xuống $O(N)$.
  * **Command Pattern** *(Design Patterns Revisited — Trang 28 – 39)*: Đóng gói thao tác người chơi thành đối tượng lệnh có thể truyền mạng và hoàn nguyên.
  * **State Pattern** *(Design Patterns Revisited — Trang 97 – 115)*: Quản lý máy trạng thái hữu hạn FSM cho nhân vật đấu sĩ.
  * **Double Buffer Pattern** *(Sequencing Patterns — Trang 117 – 132)*: Bộ đệm kép ngăn ngừa hiện tượng xé hình (Screen Tearing).

---

### [3] Joshua Glazer & Sanjay Madhav — Lập trình Mạng & Netcode Thời gian thực
* **Tên tác phẩm**: *Multiplayer Game Programming: Architecting Networked Games*.
* **Nhà xuất bản**: Addison-Wesley Professional, 2015. ISBN: 978-0-13-403430-0.
* **File nguồn**: `00_Raw_PDF/Multiplayer Game Programming.pdf` (385 trang, đã bóc tách 14 chương tại `00_Raw_PDF/_Knowledge_Base_Glazer_Madhav/chapters_extracted/`).
* **Đóng góp cụ thể theo chương**:
  * **Network Topologies & Client-Server** *(Chương 6 — Trang 184 – 217)*: Kiến trúc Dedicated Authoritative Server, máy chủ nắm quyền phán quyết tuyệt đối, Client không có quyền can thiệp vào logic thế giới.
  * **Latency, Jitter, and Reliability** *(Chương 7 — Trang 218 – 251)*: Bản chất của Network Jitter (sự biến thiên độ trễ truyền gói tin), kỹ thuật đo đạc và giả lập trễ mạng $\pm 25$ms.
  * **Client-Side Interpolation (LERP)** *(Chương 8 — Trang 252 – 271)*: Giải thuật toán học nội suy tuyến tính triệt tiêu lỗi giật hình (Micro-stutter / Jitter) khi lệch pha giữa Server 20Hz và Client 60 FPS.
  * **Client-Side Prediction & Reconciliation** *(Chương 8 — Trang 257 – 266)*: Cơ chế dự đoán phản hồi phím bấm tức thì cho người chơi bản địa.
  * **Object Serialization & Streams** *(Chương 4 — Trang 120 – 157)*: Tuần tự hóa nhị phân dữ liệu qua TCP Socket, tối ưu gói tin dưới 100 bytes.
  * **Security & Cheat Detection** *(Chương 10 — Trang 284 – 297)*: Cơ chế xác thực đầu vào (Input Validation), chống Speed Hack và Teleport Hack.

---

### [4] Martin Fowler — Mẫu Kiến trúc Ứng dụng Doanh nghiệp & Lưu trữ Dữ liệu
* **Tên tác phẩm**: *Patterns of Enterprise Application Architecture* (PoEAA).
* **Nhà xuất bản**: Addison-Wesley Professional, 2002. ISBN: 0-321-12742-0.
* **File nguồn**: `00_Raw_PDF/Patterns of Enterprise Application Architecture - Martin Fowler.pdf` (559 trang, đã bóc tách 20 chương tại `00_Raw_PDF/_Knowledge_Base_Fowler/chapters_extracted/`).
* **Đóng góp vào đồ án**:
  * **Data Mapper Pattern (Chương 10)**: Lớp trung gian ánh xạ đối tượng miền nghiệp vụ sang dữ liệu lưu trữ, giữ cho `LivingEntity` và `GameRoom` hoàn toàn không phụ thuộc vào thư viện file JSON hay cơ sở dữ liệu.
  * **Repository Pattern**: Interface trừu tượng cô lập thao tác lưu trữ dữ liệu lịch sử trận đấu (`IMatchRepository`).

---

### [5] Vaughn Vernon — Thiết kế Hướng Miền Nghiệp vụ Hiện đại (Domain-Driven Design)
* **Tên tác phẩm**: *Implementing Domain-Driven Design* (The Red Book).
* **Nhà xuất bản**: Addison-Wesley Professional, 2013. ISBN: 978-0-321-83457-7.
* **File nguồn**: `00_Raw_PDF/Implementing Domain-Driven Design.pdf` (Đã bóc tách tại `00_Raw_PDF/_Knowledge_Base_Vernon/chapters_extracted/`).
* **Đóng góp vào đồ án**:
  * **Aggregate Pattern & Aggregate Root (Chương 10)**: Thiết kế `GameRoom` và `Player` thành các cụm thực thể bảo vệ tính toàn vẹn và bất biến nghiệp vụ trong môi trường đa luồng.
  * **Value Objects (Chương 6)**: Thiết kế lớp `Vector2D` bất biến (Immutable), loại bỏ hoàn toàn các lỗi tranh chấp bộ nhớ (Race Condition).

---

## 7.2. TÀI LIỆU TIÊU CHUẨN THIẾT KẾ & NGÔN NGỮ

* **[6] Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides (Gang of Four - GoF)**. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley. (Áp dụng: Strategy, State, Observer, Composite, Command).
* **[7] Object Management Group (OMG)**. (2017). *OMG Unified Modeling Language (OMG UML) Superstructure, Version 2.5.1*. OMG Document formal/2017-12-05.
* **[8] Oracle Corporation**. (2023). *Java SE 17 & 21 (LTS) Language and Virtual Machine Specifications*. Oracle Documentation.
* **[9] Kent Beck**. (2002). *Test-Driven Development: By Example*. Addison-Wesley Professional. (Bộ quy chuẩn Unit Test JUnit).
* **[10] Martin Fowler**. (2018). *Refactoring: Improving the Design of Existing Code* (2nd Edition). Addison-Wesley Professional.

---

*Hết Phần VII — Danh mục Tài liệu Tham khảo.*
