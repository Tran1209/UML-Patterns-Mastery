# 05. Patterns of Enterprise Application Architecture (PoEAA) - Martin Fowler

Cuốn sách kinh điển của Martin Fowler cung cấp nền tảng kiến trúc doanh nghiệp, giải quyết bài toán: **Tổ chức logic nghiệp vụ và ánh xạ dữ liệu giữa Đối tượng (Object) và Cơ sở dữ liệu quan hệ (RDBMS)**.

Liên kết chéo:
- [[01_Process_and_UML|Quy trình OOAD & UML]]
- [[02_GRASP_9_Principles|9 Nguyên lý GRASP]]
- [[04_NextGen_POS_Evolution|Tiến hóa ca học NextGen POS]]
- [[06_Domain_Driven_Design_Aggregates|DDD & Aggregate Design (Vaughn Vernon)]]

---

## 1. Tổ chức Logic Nghiệp vụ (Organizing Domain Logic)

Martin Fowler phân chia 4 cách tiếp cận chính để xây dựng tầng nghiệp vụ:

| Mẫu (Pattern) | Đặc điểm | Khi nào nên dùng? |
|---|---|---|
| **Transaction Script** | Tổ chức logic theo từng quy trình/thao tác (thủ tục đơn giản từ request đến DB). | Hệ thống đơn giản, ít nghiệp vụ, CRUD cơ bản. |
| **Domain Model** | Mạng lưới các đối tượng hướng đối tượng phong phú kết hợp cả dữ liệu lẫn hành vi (Rich Domain Model). | Hệ thống phức tạp, nhiều quy tắc nghiệp vụ biến động (như NextGen POS). |
| **Table Module** | Một lớp duy nhất đại diện cho toàn bộ bảng DB (thường dùng trong .NET DataSet/DataTable cũ). | Kiến trúc lấy dữ liệu dạng bảng làm trung tâm. |
| **Service Layer** | Định nghĩa ranh giới ứng dụng, điều phối các use case và giao dịch (Transaction/Security). | Cung cấp API chuẩn cho nhiều loại client (Web, Mobile, External API). |

---

## 2. Ánh xạ Đối tượng - Quan hệ (O/R Mapping Patterns)

### 2.1. Data Mapper (Bộ Ánh Xạ Dữ Liệu)
- Tách biệt hoàn toàn lớp nghiệp vụ (`Sale`, `Product`) khỏi cấu trúc bảng cơ sở dữ liệu (`sales`, `products`).
- Đối tượng Domain không biết gì về SQL hay kết nối database $\rightarrow$ Đạt nguyên lý **Low Coupling** và **Separated Interface**.

### 2.2. Unit of Work (Đơn vị Công việc)
- Theo dõi tất cả các đối tượng bị thay đổi (Insert, Update, Delete) trong suốt một phiên làm việc (business transaction).
- Khi kết thúc phiên, tự động đồng bộ tất cả thay đổi xuống database trong một Database Transaction duy nhất để tối ưu hiệu năng và đảm bảo tính toàn vẹn (ACID).

### 2.3. Identity Map (Bản đồ Định danh)
- Đảm bảo mỗi dòng trong database chỉ được nạp lên bộ nhớ thành đúng một đối tượng duy nhất.
- Tránh việc nạp trùng lặp gây xung đột dữ liệu lúc cập nhật.

### 2.4. Repository (Kho lưu trữ)
- Đóng vai trò như một tập hợp đối tượng trong bộ nhớ (In-memory collection).
- Cung cấp các hàm tìm kiếm (`findById`, `findByCustomer`) và che giấu toàn bộ chi tiết truy vấn cơ sở dữ liệu.

---

## 3. Các Mẫu Cơ Bản (Base Patterns)

- **Value Object**: Đối tượng được định danh bằng giá trị của nó thay vì một mã ID (ví dụ: `Money`, `Address`, `Quantity`). Bất biến (Immutable).
- **Money Pattern**: Đóng gói số tiền cùng loại tiền tệ (`amount`, `currency`), giải quyết triệt để lỗi làm tròn số thực dấu phẩy động trong thanh toán.
- **Gateway**: Bao bọc quyền truy cập vào một hệ thống hoặc tài nguyên bên ngoài (tương đồng với Adapter trong GoF).
- **Separated Interface**: Khai báo interface ở một package nghiệp vụ và hiện thực ở package hạ tầng để đảo ngược phụ thuộc (Dependency Inversion).
