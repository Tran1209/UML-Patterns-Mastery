# 02. Chín Nguyên Lý Phân Bổ Trách Nhiệm GRASP

GRASP (General Responsibility Assignment Software Patterns) là hệ thống các nguyên lý cơ bản giúp kỹ sư phần mềm quyết định: **"Class nào nên giữ method nào?"**.

Liên kết chéo:
- [[01_Process_and_UML|Quy trình OOAD & UML]]
- [[03_GoF_Patterns_Applied|Các Mẫu thiết kế GoF]]
- [[04_NextGen_POS_Evolution|Ca học NextGen POS]]

---

## 1. Information Expert (Chuyên gia Thông tin)
- **Vấn đề**: Gán trách nhiệm cho lớp nào?
- **Giải pháp**: Gán trách nhiệm cho lớp sở hữu đầy đủ thông tin cần thiết nhất để hoàn thành trách nhiệm đó.
- **Ví dụ**: Tính tổng tiền của hóa đơn `Sale.getTotal()`. Lớp `Sale` biết danh sách các dòng mặt hàng `SalesLineItem`, và mỗi `SalesLineItem` biết số lượng và giá của `ProductDescription`. Do đó `Sale` tính tổng bằng cách lặp và yêu cầu từng `SalesLineItem.getSubtotal()`.

## 2. Creator (Người Khởi Tạo)
- **Vấn đề**: Ai nên chịu trách nhiệm tạo ra một thực thể mới của lớp A?
- **Giải pháp**: Gán cho lớp B nếu thỏa mãn một trong các điều kiện:
  - B chứa (contains) hoặc tập hợp (aggregates) A.
  - B ghi nhận (records) A.
  - B sử dụng mật thiết (closely uses) A.
  - B có dữ liệu khởi tạo của A.
- **Ví dụ**: `Sale` chứa nhiều `SalesLineItem` $\rightarrow$ `Sale` tạo `SalesLineItem`.

## 3. Controller (Bộ Điều Phối)
- **Vấn đề**: Đối tượng đầu tiên nào ở tầng nghiệp vụ tiếp nhận và điều phối sự kiện hệ thống từ UI?
- **Giải pháp**: Gán trách nhiệm cho đối tượng đại diện cho toàn bộ hệ thống/root (`Register`, `Store`) hoặc đối tượng đại diện cho kịch bản ca sử dụng (`ProcessSaleHandler`). Tránh dồn toàn bộ code nghiệp vụ vào Controller (chống anti-pattern Fat Controller).

## 4. Low Coupling (Kết Hợp Lỏng Lẻo)
- Giảm thiểu sự phụ thuộc qua lại giữa các lớp để việc thay đổi ở một lớp không gây phản ứng dây chuyền làm hỏng các lớp khác.

## 5. High Cohesion (Độ Gắn Kết Cao)
- Một lớp chỉ nên tập trung làm tốt một nhóm nhiệm vụ có liên quan chặt chẽ. Tránh tạo lớp "God Object" làm mọi việc.

## 6. Polymorphism (Đa Hình)
- **Vấn đề**: Xử lý các biến thể hành vi dựa trên chủng loại như thế nào?
- **Giải pháp**: Sử dụng thao tác đa hình (Interface hoặc Kế thừa) thay vì các khối lệnh điều kiện phức tạp `if (type == A) ... else if (type == B) ...`.

## 7. Pure Fabrication (Chế Tác Thuần Túy)
- **Vấn đề**: Khi không có khái niệm nào trong Domain Model thực tế phù hợp để đảm bảo Low Coupling và High Cohesion?
- **Giải pháp**: Tự định nghĩa ra một lớp nhân tạo không thuộc về miền thực tế, ví dụ: `SaleRepository`, `Logger`, `DatabaseGateway`.

## 8. Indirection (Chuyển Tiếp Gián Tiếp)
- **Vấn đề**: Làm sao để tránh ghép nối trực tiếp giữa hai thành phần?
- **Giải pháp**: Gán trách nhiệm cho một đối tượng trung gian làm cầu nối trung chuyển tương tác.

## 9. Protected Variations (Bảo Vệ Khỏi Biến Động)
- **Vấn đề**: Làm sao để bảo vệ hệ thống khỏi những thay đổi không ổn định của môi trường bên ngoài hoặc phần cứng?
- **Giải pháp**: Xác định các điểm có xu hướng thay đổi và bọc chúng lại đằng sau một giao diện ổn định (Interface/Contract).
