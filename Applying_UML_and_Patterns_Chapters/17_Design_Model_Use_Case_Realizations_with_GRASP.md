[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 16: GRASP Designing Objects with Responsibilities](./16_GRASP_Designing_Objects_with_Responsibilities.md) | [Chương 18: Design Model Determining Visibility ►](./18_Design_Model_Determining_Visibility.md)

---

## CHƯƠNG 17: DESIGN MODEL: USE-CASE REALIZATIONS WITH GRASP PATTERNS

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **17.1. Do the Design: Use-Case Realizations** *(Hiện thực hóa Ca sử dụng: Biến kịch bản thành thiết kế đối tượng)*
* **17.2. Artifact Comments: Dynamic Design with GRASP** *(Đặc tả tạo tác: Mô hình động áp dụng mẫu hình)*
* **17.3. Use-Case Realizations for NextGen POS** *(Hiện thực hóa toàn bộ các Thao tác Hệ thống trong NextGen POS)*
  * *Realizing makeNewSale (Hiện thực hóa khởi tạo đơn hàng)*
  * *Realizing enterItem (Hiện thực hóa nhập mặt hàng)*
  * *Realizing endSale (Hiện thực hóa kết thúc đơn hàng)*
  * *Realizing makePayment (Hiện thực hóa thanh toán tiền mặt)*
* **17.4. Connecting the Design Model to Other Artifacts** *(Kết nối Mô hình Thiết kế với Hợp đồng và Mô hình Miền)*
* **17.5. Collaborations and Interactions in Design** *(Cộng tác và Tương tác trong Thiết kế)*
* **17.6. Summary of GRASP Applications in NextGen POS** *(Tổng kết ứng dụng mẫu hình GRASP)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 17 là bài thực hành tổng lực: **Hiện thực hóa Ca sử dụng (*Use-Case Realization*)**. Nó giải quyết bài toán: Làm sao đảm bảo tất cả các cam kết trong **Hợp đồng Vận hành (Chương 13)** được chuyển hóa trọn vẹn 100% thành các tương tác đối tượng có thể chạy được.

Nếu không thực hiện Use-Case Realization bài bản, hệ thống sẽ gặp các thảm họa:
* **Đứt gãy giữa Phân tích và Thiết kế (Analysis-Design Disconnect):** Tài liệu phân tích viết một đằng, code thiết kế chạy một nẻo. Các thuộc tính và liên kết được định nghĩa rất hay trong Hợp đồng hoàn toàn bị bỏ quên trong mã nguồn.
* **Mất dấu vết yêu cầu (Traceability Loss):** Khi có lỗi nghiệp vụ phát sinh (ví dụ: tiền thừa tính sai), không ai có thể truy vết ngược lại xem thông điệp tính tiền thừa bắt nguồn từ ca sử dụng nào và do chuỗi đối tượng nào chịu trách nhiệm.
* **Kiến trúc chắp vá theo kịch bản đơn lẻ (Patchwork Architecture):** Mỗi lập trình viên tự cài đặt một hàm theo ý mình mà không có sự đồng nhất về đối tượng điều phối (Controller) và đối tượng chuyên gia (Expert).

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------------+
|                  USE-CASE REALIZATION: MAPPING CONTRACTS TO INTERACTIONS                |
+-----------------------------------------------------------------------------------------+
| SYSTEM OPERATION (SSD)   ===>   OPERATION CONTRACT POSTCONDITIONS                       |
|   enterItem(id, qty)            1. SalesLineItem 'sli' was created.                     |
|                                 2. 'sli' was associated with current Sale.              |
|                                 3. 'sli.quantity' became qty.                           |
|                                 4. 'sli' was associated with ProductDescription.        |
+-----------------------------------------------------------------------------------------+
                                         |
                                         | [ÁP DỤNG MẪU HÌNH GRASP]
                                         v
+-----------------------------------------------------------------------------------------+
| USE-CASE REALIZATION (Interaction Diagram):                                             |
|   Register (Controller)                                                                 |
|      |                                                                                  |
|      +--- 1. getProductDescription(id) ---> ProductCatalog (Expert)                     |
|      |                                                                                  |
|      +--- 2. makeLineItem(desc, qty) -----> Sale (Creator & Expert)                     |
|                                               |                                         |
|                                               +--- 2.1. <<create>>(desc, qty) ---> sli  |
+-----------------------------------------------------------------------------------------+
```

#### 2.1. Khái niệm Hiện thực hóa Ca sử dụng (Use-Case Realization)
* Một **Use-Case Realization** là sự mô tả trực quan (thường thông qua Sơ đồ Tuần tự hoặc Sơ đồ Cộng tác) thể hiện **cách thức các đối tượng phần mềm cộng tác với nhau** thông qua truyền thông điệp để thỏa mãn một ca sử dụng cụ thể.
* Nó là chiếc cầu nối trực tiếp biến các phát biểu tĩnh của Hợp đồng Vận hành (*Operation Contracts*) thành các luồng thực thi động của phần mềm.

#### 2.2. Quy trình 4 bước Hiện thực hóa một Thao tác Hệ thống:
Craig Larman chuẩn hóa quy trình phân tích thiết kế thành 4 bước tuần tự:
1. **Xác định Thao tác Hệ thống từ SSD:** Ví dụ: `enterItem(itemID, quantity)`.
2. **Đọc Hợp đồng Vận hành tương ứng:** Rà soát lại 4 loại hậu điều kiện bắt buộc phải đạt được.
3. **Chọn Controller tiếp nhận:** Sử dụng GRASP Controller (ví dụ: `Register`).
4. **Vẽ Sơ đồ Tương tác áp dụng GRASP:**
   * Cần tra cứu dữ liệu? $\to$ Tìm **Information Expert** để gửi thông điệp (`ProductCatalog`).
   * Cần tạo đối tượng mới theo hậu điều kiện? $\to$ Tìm **Creator** để gửi thông điệp `makeLineItem()` (`Sale`).
   * Cần đảm bảo khớp nối thấp và gắn kết cao? $\to$ Đánh giá lại bằng **Low Coupling / High Cohesion**.

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là mã nguồn hiện thực hóa trọn vẹn ca sử dụng `Process Sale` thông qua việc xâu chuỗi 4 Thao tác Hệ thống cốt lõi (`makeNewSale`, `enterItem`, `endSale`, `makePayment`) trên cùng một luồng nghiệp vụ:

```java
// ==================== USE-CASE REALIZATION: PROCESS SALE ====================
public class ProcessSaleRealizationIntegrationTest {

    @Test
    public void testFullProcessSaleLifecycle() {
        // [SETUP]: Khởi tạo hạ tầng miền
        ProductCatalog catalog = new ProductCatalog();
        ItemID milkID = new ItemID("MILK-01");
        catalog.addProductDescription(milkID, new Money(15.0, Currency.getInstance("USD")), "Sữa tươi");

        Register register = new Register(catalog);

        // ==================== THAO TÁC 1: makeNewSale() ====================
        // GRASP: Register đóng vai trò Controller tiếp nhận; Sale được khởi tạo
        register.makeNewSale();
        Sale sale = register.getCurrentSale();
        assertNotNull(sale, "Hậu điều kiện vi phạm: Sale chưa được tạo!");

        // ==================== THAO TÁC 2: enterItem() ====================
        // GRASP: Catalog là Expert tra cứu; Sale là Creator tạo SalesLineItem
        register.enterItem(milkID, 2);
        assertEquals(1, sale.getLineItems().size(), "Hậu điều kiện vi phạm: Thiếu line item!");
        assertEquals(2, sale.getLineItems().get(0).getQuantity());

        // ==================== THAO TÁC 3: endSale() ====================
        // GRASP: Sale là Information Expert tính tổng tiền toàn bộ hóa đơn
        Money total = register.endSale();
        assertEquals(30.0, total.getAmount().doubleValue(), "Hậu điều kiện vi phạm: Sai tổng tiền!");
        assertTrue(sale.isComplete(), "Hậu điều kiện vi phạm: Trạng thái sale chưa hoàn tất!");

        // ==================== THAO TÁC 4: makePayment() ====================
        // GRASP: Sale là Creator tạo Payment; kiểm tra tiền thừa
        ReceiptDTO receipt = register.makePayment(new Money(50.0, Currency.getInstance("USD")));
        assertNotNull(receipt, "Hậu điều kiện vi phạm: Không sinh biên lai!");
        assertEquals(20.0, receipt.getChangeBalance().getAmount().doubleValue(), "Sai tiền thừa trả khách!");
        assertNotNull(sale.getPayment(), "Hậu điều kiện vi phạm: Payment chưa liên kết với Sale!");
    }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Thao tác Hệ thống | Các mẫu GRASP phối hợp | Hậu điều kiện của Hợp đồng được thỏa mãn |
| :--- | :--- | :--- |
| `makeNewSale()` | **Controller** (`Register`) + **Creator** (`Register` tạo `Sale`) | Thể hiện `Sale` được tạo và trở thành phiên bản hiện tại của `Register`. |
| `enterItem(...)` | **Controller** + **Expert** (`ProductCatalog`) + **Creator** (`Sale` tạo `SalesLineItem`) | `sli` được tạo, gắn vào `Sale`, số lượng được gán, liên kết với `ProductDescription`. |
| `endSale()` | **Controller** + **Information Expert** (`Sale` duyệt `lineItems` tính tổng) | Thuộc tính `Sale.isComplete` đổi thành `true`; tổng tiền được tính toán toàn vẹn. |
| `makePayment(...)` | **Controller** + **Creator** (`Sale` tạo `Payment`) + **Expert** (`Sale` tính tiền thừa) | Thể hiện `Payment` được tạo, gán tiền đưa, liên kết với `Sale`, biên lai DTO được xuất ra. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ Tuần tự Hiện thực hóa Ca sử dụng (Use-Case Realization Sequence Diagram) kết nối toàn diện 4 Thao tác Hệ thống trong NextGen POS:

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as Thu ngân (UI)
    participant Reg as :Register
    participant Cat as :ProductCatalog
    participant S as currentSale:Sale
    participant SLI as :SalesLineItem
    participant P as p:Payment

    Note over Cashier, Reg: 1. BẮT ĐẦU ĐƠN HÀNG MỚI
    Cashier->>Reg: makeNewSale()
    create participant S
    Reg->>S: <<create>>()

    Note over Cashier, SLI: 2. NHẬP MẶT HÀNG (Lặp)
    Cashier->>Reg: enterItem(itemID, qty)
    Reg->>Cat: desc = getProductDescription(itemID)
    Reg->>S: makeLineItem(desc, qty)
    create participant SLI
    S->>SLI: <<create>>(desc, qty)

    Note over Cashier, S: 3. KẾT THÚC NHẬP HÀNG
    Cashier->>Reg: endSale()
    Reg->>S: total = getTotal()
    activate S
    S->>SLI: getSubtotal()
    S-->>Reg: total
    deactivate S

    Note over Cashier, P: 4. THANH TOÁN TIỀN MẶT
    Cashier->>Reg: makePayment(amount)
    Reg->>S: makePayment(amount)
    activate S
    create participant P
    S->>P: <<create>>(amount)
    S-->>Reg: receiptData
    deactivate S
    Reg-->>Cashier: displayReceipt()
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Hiện thực hóa Ca sử dụng `CastSpell` (Thi triển Phép thuật diện rộng - AoE Spell) trong Game 2.5D Multiplayer.
* **Quy trình Use-Case Realization bài bản cho Game:**
  1. *System Operation:* `RequestCastAoESpell(spellID, targetPosition2D)`.
  2. *Controller:* `SpellCastController` tiếp nhận gói tin từ tầng Mạng (Network Receiver).
  3. *Information Expert 1:* `SpellDataCatalog` chịu trách nhiệm tra cứu thông số phép (Bán kính nổ, Mana tiêu hao, Sát thương cơ bản).
  4. *Creator:* `SpellCastController` kiểm tra nếu người chơi đủ mana thì ủy thác cho `WorldGridManager` đóng vai trò Creator để tạo ra một thể hiện `ActiveSpellZone` (Vùng nổ phép thuật) đặt tại tọa độ `targetPosition2D`.
  5. *Information Expert 2:* `ActiveSpellZone` nắm giữ tọa độ tâm và bán kính. Nó là Expert duy nhất biết cách quét Spatial Grid để tìm danh sách các `TargetEntity` nằm trong vùng ảnh hưởng.
  6. *Delegation:* `ActiveSpellZone` lặp qua từng mục tiêu, gửi thông điệp `target.TakeDamage(calculatedDmg)`.
* **Lợi ích kiến trúc:** Từng đối tượng làm đúng thiên chức của mình. Nếu sau này Game Designer đổi quy tắc phép từ nổ tức thì sang giật độc theo thời gian (*DoT - Damage over Time*), ta chỉ cần thay đổi hành vi bên trong `ActiveSpellZone`, toàn bộ Controller và Network Packet hoàn toàn không bị ảnh hưởng.

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 16: GRASP Designing Objects with Responsibilities](./16_GRASP_Designing_Objects_with_Responsibilities.md) | [Chương 18: Design Model Determining Visibility ►](./18_Design_Model_Determining_Visibility.md)
