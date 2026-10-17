# 04. Tiến Hóa Thiết Kế Ca Học Thực Tế: NextGen POS

NextGen POS (Point of Sale) là hệ thống mẫu xuyên suốt cuốn sách của Craig Larman. Thiết kế của hệ thống phát triển dần dần qua 3 chu kỳ lặp (Iterations).

Liên kết chéo:
- [[01_Process_and_UML|Quy trình OOAD & UML]]
- [[02_GRASP_9_Principles|9 Nguyên lý GRASP]]
- [[03_GoF_Patterns_Applied|Các Mẫu thiết kế GoF]]

---

## 1. Iteration 1: Ca sử dụng cơ bản "Process Sale" (Thanh toán tiền mặt)
- **Mục tiêu**: Xây dựng luồng xử lý cốt lõi khi khách hàng mua hàng và trả tiền mặt.
- **Các thành phần cốt lõi**:
  - `Register`: Đóng vai trò Controller tiếp nhận các sự kiện từ thu ngân.
  - `Sale`: Lưu trữ thời gian, trạng thái và danh sách các `SalesLineItem`.
  - `SalesLineItem`: Lưu số lượng và tham chiếu tới `ProductDescription`.
  - `ProductCatalog`: Tra cứu thông tin giá và tên sản phẩm theo mã `ItemID`.
  - `Payment`: Ghi nhận khoản thanh toán tiền mặt.

---

## 2. Iteration 2: Mở rộng tính năng và Tinh chỉnh thiết kế
- Bổ sung nhiều phương thức thanh toán (Thẻ tín dụng, Phiếu quà tặng).
- Áp dụng nguyên lý GRASP:
  - **Polymorphism**: Thiết kế các lớp con của `Payment` (`CreditPayment`, `CashPayment`, `CheckPayment`).
  - **Indirection & Protected Variations**: Thiết kế `CreditAuthorizationServiceAdapter` để trừu tượng hóa các cổng thanh toán.

---

## 3. Iteration 3: Kiến trúc hoàn chỉnh với Mẫu thiết kế GoF
- Áp dụng **Strategy & Composite** để xử lý các chính sách chiết khấu phức tạp (`ISalePricingStrategy`).
- Sử dụng **Factory** để khởi tạo các dịch vụ ngoại vi và **Singleton** quản lý `ServicesFactory`.
- Áp dụng kiến trúc phân tầng (Layered Architecture):
  - **UI Layer** (Giao diện người dùng)
  - **Domain Layer** (Logic nghiệp vụ và các mô hình thực thể)
  - **Technical Services Layer** (Ghi log, kết nối DB, Adapter bên ngoài)
