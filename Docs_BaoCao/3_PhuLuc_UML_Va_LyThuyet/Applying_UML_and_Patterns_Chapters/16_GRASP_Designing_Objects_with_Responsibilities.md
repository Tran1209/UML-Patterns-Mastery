[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 15: Interaction Diagram Notation](./15_Interaction_Diagram_Notation.md) | [Chương 17: Design Model Use Case Realizations with GRASP ►](./17_Design_Model_Use_Case_Realizations_with_GRASP.md)

---

## CHƯƠNG 16: GRASP: DESIGNING OBJECTS WITH RESPONSIBILITIES

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **16.1. Responsibilities and Methods** *(Trách nhiệm và Phương thức: Mối quan hệ mật thiết)*
* **16.2. Responsibilities and Interaction Diagrams** *(Trách nhiệm trong Sơ đồ Tương tác: Nơi quyết định hình thành)*
* **16.3. Patterns** *(Mẫu hình là gì? Khái niệm Pattern trong thiết kế)*
* **16.4. GRASP: Patterns of General Principles in Assigning Responsibilities** *(GRASP: Các mẫu nguyên lý chung trong việc phân bổ trách nhiệm)*
* **16.5. The UML Class Diagram Notation** *(Ký hiệu Sơ đồ Lớp UML trong thiết kế)*
* **16.6. Information Expert (or Expert)** *(Chuyên gia Thông tin – Nguyên tắc vàng số một)*
* **16.7. Creator** *(Người Tạo lập – Ai chịu trách nhiệm khởi tạo đối tượng?)*
* **16.8. Low Coupling** *(Khớp nối Lỏng – Mẫu hình đánh giá khả năng bảo trì)*
* **16.9. High Cohesion** *(Tính Gắn kết Cao – Mẫu hình đánh giá sự tập trung)*
* **16.10. Controller** *(Bộ Điều phối – Cửa sổ tiếp nhận sự kiện hệ thống)*
  * *Façade Controller (Bộ điều phối kiểu Mặt tiền)*
  * *Use-Case Controller (Bộ điều phối theo Ca sử dụng)*
  * *Bloated Controller (Cảnh báo: Hội chứng Bộ điều phối quá tải)*
* **16.11. Further Readings** *(Tài liệu tham khảo chuyên sâu)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 16 là **trái tim triết lý của toàn bộ cuốn sách**. Nó trả lời câu hỏi cốt tử mà mọi lập trình viên OOP phải đối mặt: *"Lớp nào nên nắm giữ phương thức này? Đối tượng nào nên tạo ra đối tượng kia?"*.

Nếu không làm chủ 5 nguyên lý nền tảng của GRASP, hệ thống sẽ rơi vào các thảm họa:
* **Hội chứng Mô hình Miền Thiếu máu (Anemic Domain Model):** Các lớp nghiệp vụ bị biến thành các bị bông chứa dữ liệu thuần túy (*Dumb Data Holders*) chỉ có getter/setter. Toàn bộ logic bị rút ruột và nhét vào các lớp Service/Manager khổng lồ.
* **The God Object / Bloated Controller:** Một lớp duy nhất (như `POSManager` hay `GameManager`) ôm đồm hàng trăm hàm, biết tất cả mọi thứ, điều khiển tất cả các lớp khác. Khi lớp này bị lỗi, toàn bộ hệ thống sụp đổ; việc kiểm thử nó đòi hỏi phải mock hàng tá phụ thuộc.
* **Mạng nhện phụ thuộc (High Coupling Nightmare):** Các đối tượng can thiệp sâu vào cấu trúc nội bộ của nhau, vi phạm định luật Demeter (*Law of Demeter*). Thay đổi kiểu dữ liệu của một trường trong Class A làm vỡ vụn code ở 20 Class khác.

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------------+
|                                5 MẪU HÌNH GRASP NỀN TẢNG                                |
+-----------------------------------------------------------------------------------------+
| [CONTROLLER]          : Đối tượng đầu tiên ngoài UI tiếp nhận sự kiện hệ thống.         |
|                         (Phân loại: Façade Controller hoặc Use-Case Controller).        |
+-----------------------------------------------------------------------------------------+
| [CREATOR]             : Ai tạo B? Là A nếu A chứa B, A ghi nhận B, hoặc A có dữ liệu    |
|                         khởi tạo B (A là Expert về việc tạo B).                         |
+-----------------------------------------------------------------------------------------+
| [INFORMATION EXPERT]  : Giao trách nhiệm cho lớp nắm giữ thông tin cần thiết.          |
|                         (Nguyên lý: Tell, Don't Ask).                                   |
+-----------------------------------------------------------------------------------------+
| [LOW COUPLING]        : Mẫu hình đánh giá (Evaluative): Giữ phụ thuộc ở mức tối thiểu   |
|                         để tăng tính tái sử dụng và dễ kiểm thử.                        |
+-----------------------------------------------------------------------------------------+
| [HIGH COHESION]       : Mẫu hình đánh giá (Evaluative): Trách nhiệm tập trung, gắn kết, |
|                         không ôm đồm quá nhiều việc không liên quan.                     |
+-----------------------------------------------------------------------------------------+
```

#### 2.1. Bản chất của GRASP (General Responsibility Assignment Software Patterns)
* GRASP không phải là các mẫu thiết kế cấu trúc phức tạp như GoF (Factory, Adapter, Decorator).
* GRASP là **Bộ quy tắc tư duy nền tảng (*Mental Toolkit*)** hướng dẫn các kỹ sư phần mềm cách phân bổ trách nhiệm và phương thức vào các lớp trong quá trình vẽ Sơ đồ Tương tác.

#### 2.2. Phân tích Chi tiết 5 Mẫu hình Cốt lõi:

##### 1. Information Expert (Chuyên gia Thông tin)
* **Vấn đề:** Nguyên tắc cơ bản nhất để gán trách nhiệm cho một lớp là gì?
* **Giải pháp:** Gán trách nhiệm cho lớp nào **nắm giữ đầy đủ thông tin cần thiết nhất** để hoàn thành trách nhiệm đó.
* **Cơ chế hoạt động:** 
  * Ai tính tổng tiền của một dòng hàng? `SalesLineItem` nắm giữ `quantity` và liên kết với `ProductDescription` (nắm `price`), do đó `SalesLineItem` chính là Expert để tính `getSubtotal()`.
  * Ai tính tổng tiền của cả đơn hàng? `Sale` nắm giữ danh sách toàn bộ các `SalesLineItem`, do đó `Sale` là Expert để tính `getTotal()`.
* **Quy tắc: Tell, Don't Ask:** Đừng hỏi dữ liệu của đối tượng để tự tính toán bên ngoài; hãy ra lệnh cho đối tượng tự làm việc trên dữ liệu của chính nó.

##### 2. Creator (Người Tạo lập)
* **Vấn đề:** Ai nên chịu trách nhiệm tạo ra một thể hiện mới của lớp `A`?
* **Giải pháp:** Gán cho lớp `B` trách nhiệm tạo `A` nếu một trong các điều kiện sau được thỏa mãn (càng nhiều càng tốt):
  1. `B` chứa hoặc bao bọc `A` (*B contains or aggregates A*).
  2. `B` ghi nhận `A` (*B records A*).
  3. `B` sử dụng chặt chẽ `A` (*B closely uses A*).
  4. `B` có dữ liệu khởi tạo để truyền cho `A` khi tạo (*B has initializing data for A*).
* *Ví dụ:* `Sale` chứa danh sách các `SalesLineItem`, ghi nhận chúng và truyền dữ liệu cho chúng $\to$ `Sale` là Creator hợp tự nhiên của `SalesLineItem`.

##### 3. Low Coupling (Khớp nối Lỏng) & 4. High Cohesion (Gắn kết Cao)
* Đây là hai **Mẫu hình Đánh giá (*Evaluative Patterns*)**. Chúng đóng vai trò là chiếc la bàn đo lường chất lượng của mọi quyết định thiết kế:
  * **Low Coupling:** Đảm bảo một lớp không phụ thuộc vào quá nhiều lớp khác. Hạn chế tối đa các đường liên kết thừa. Lợi ích: Thay đổi một lớp không làm sập lớp khác, dễ tái sử dụng độc lập.
  * **High Cohesion:** Đảm bảo các trách nhiệm của một lớp có mối liên hệ mật thiết với nhau, không biến thành một lớp "làm tất cả mọi việc". Lớp có độ gắn kết cao thường có số lượng phương thức vừa phải, code ngắn gọn, cực kỳ dễ bảo trì.

##### 5. Controller (Bộ Điều phối)
* **Vấn đề:** Đối tượng đầu tiên nào ở Tầng Miền (*Domain Layer*) tiếp nhận thông điệp từ Tầng Giao diện (*UI Layer*)?
* **Giải pháp:** Gán trách nhiệm cho một lớp đại diện cho một trong hai lựa chọn:
  1. **Façade Controller:** Đại diện cho toàn bộ hệ thống, một thiết bị phần cứng, hoặc một hệ thống con (ví dụ: `Register`, `POSSystem`). Thích hợp khi hệ thống có số lượng sự kiện vừa phải.
  2. **Use-Case Controller:** Đại diện cho một kịch bản ca sử dụng cụ thể (ví dụ: `ProcessSaleHandler`, `ProcessSaleSession`). Thích hợp khi một ca sử dụng có quá nhiều sự kiện phức tạp để tránh làm phình to Façade Controller.
* **Cảnh báo Bloated Controller (Bộ điều phối Quá tải):** Nếu một Controller bắt đầu kiêm luôn việc tính toán thuế, tự kiểm tra mã vạch, tự tạo database query $\to$ Nó đã bị ung thư kiến trúc. Controller **chỉ điều phối luồng, không tự thực thi logic nghiệp vụ nặng**. Nó phải ủy thác (*Delegate*) ngay lập tức cho các Information Experts ở bên dưới.

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là đoạn mã Java chuẩn mực minh họa sự phối hợp nhịp nhàng giữa **Controller**, **Creator**, **Information Expert**, bảo đảm **Low Coupling** và **High Cohesion**:

```java
// ==================== GRASP: CONTROLLER ====================
// Register đóng vai trò Façade Controller: Tiếp nhận sự kiện, điều phối luồng
public class Register {
    private ProductCatalog catalog;
    private Sale currentSale;

    public Register(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    public void enterItem(ItemID itemID, int quantity) {
        // [Information Expert]: Catalog nắm giữ thông tin sản phẩm -> ủy thác tra cứu
        ProductDescription desc = catalog.getProductDescription(itemID);
        if (desc == null) {
            throw new ItemNotFoundException("Không tìm thấy mã: " + itemID);
        }
        
        // [High Cohesion / Delegation]: Register không tự tạo SalesLineItem!
        // Ủy thác cho currentSale làm Creator
        currentSale.makeLineItem(desc, quantity);
    }
}

// ==================== GRASP: CREATOR & INFORMATION EXPERT ====================
public class Sale {
    private List<SalesLineItem> lineItems = new ArrayList<>();
    private Payment payment;

    // [CREATOR]: Sale chứa lineItems -> Sale chịu trách nhiệm tạo SalesLineItem
    public void makeLineItem(ProductDescription desc, int quantity) {
        lineItems.add(new SalesLineItem(desc, quantity));
    }

    // [INFORMATION EXPERT]: Sale nắm giữ danh sách toàn bộ lineItems -> Sale tính tổng
    public Money getTotal() {
        Money total = new Money(0.0);
        for (SalesLineItem lineItem : lineItems) {
            // [TELL, DON'T ASK]: Yêu cầu lineItem tự tính thành tiền, Sale chỉ cộng dồn
            total = total.add(lineItem.getSubtotal());
        }
        return total;
    }
}

// ==================== GRASP: INFORMATION EXPERT ====================
public class SalesLineItem {
    private int quantity;
    private ProductDescription description;

    public SalesLineItem(ProductDescription desc, int quantity) {
        this.description = desc;
        this.quantity = quantity;
    }

    // [INFORMATION EXPERT]: Nắm giữ quantity và tham chiếu ProductDescription
    // Nó là đối tượng duy nhất có đủ tri thức để tính thành tiền của chính mình
    public Money getSubtotal() {
        return description.getPrice().multiply(quantity);
    }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Dòng code / Đoạn code then chốt | Mẫu hình GRASP áp dụng & Ý nghĩa kiến trúc |
| :--- | :--- |
| `public class Register` | **Controller:** Đóng vai trò là bức tường phòng thủ đầu tiên, ngăn chặn sự kiện UI chọc thẳng vào các thực thể `Sale` hay `ProductCatalog`. |
| `ProductDescription desc = catalog...` | **Information Expert & Low Coupling:** `Register` không tự lục lọi cơ sở dữ liệu; nó hỏi `ProductCatalog`. `Register` chỉ phụ thuộc vào `ProductCatalog` qua giao diện chuẩn. |
| `currentSale.makeLineItem(desc, quantity)` | **Creator & High Cohesion:** Bảo vệ sự gắn kết của `Register`. Nếu `Register` tự gọi `new SalesLineItem()`, nó sẽ bị khớp nối với cả `SalesLineItem`, làm tăng độ phức tạp phụ thuộc. |
| `total = total.add(lineItem.getSubtotal())` | **Information Expert (Tell, Don't Ask):** Tránh hiện tượng *Feature Envy*. `Sale` tôn trọng ranh giới đóng gói của `SalesLineItem`, không tò mò rút ruột biến `quantity` ra ngoài. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ phân bổ trách nhiệm theo 5 mẫu hình GRASP trong ca sử dụng nhập hàng (`enterItem`):

```mermaid
sequenceDiagram
    autonumber
    actor UI as Tầng Giao diện (UI)
    participant Reg as :Register (GRASP: Controller)
    participant Cat as :ProductCatalog (GRASP: Expert tra cứu)
    participant S as :Sale (GRASP: Creator & Expert tính tổng)
    participant SLI as :SalesLineItem (GRASP: Expert tính giá dòng)

    UI->>Reg: enterItem(itemID, quantity)
    activate Reg
    Note over Reg: GRASP: Controller<br/>Điều phối luồng nghiệp vụ

    Reg->>Cat: getProductDescription(itemID)
    activate Cat
    Note over Cat: GRASP: Information Expert<br/>Nắm giữ kho thông tin sản phẩm
    Cat-->>Reg: desc
    deactivate Cat

    Reg->>S: makeLineItem(desc, quantity)
    activate S
    Note over S: GRASP: Creator<br/>Sale chứa SalesLineItem nên Sale tạo nó
    
    create participant SLI
    S->>SLI: <<create>>(desc, quantity)
    activate SLI
    SLI-->>S: sli
    deactivate SLI

    S-->>Reg: return
    deactivate S

    Reg-->>UI: displayUpdatedSale()
    deactivate Reg
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Thiết kế Hệ thống Chiến đấu & Trừ máu (*Combat & Health Reduction System*) trong Game 2.5D Multiplayer.
* **Thảm họa nếu vi phạm GRASP:**
  Lập trình viên tạo class `CombatManager` (Bloated Controller):
  ```csharp
  // THẢM HỌA: Anemic Domain Model & Feature Envy
  void ProcessHit(Player attacker, Monster target) {
      int baseDmg = attacker.weapon.damage;
      int def = target.armor.defense;
      int finalDmg = baseDmg - def; // CombatManager tự tính!
      target.currentHP = target.currentHP - finalDmg; // Tự trừ máu của quái!
      if (target.currentHP <= 0) target.Die();
  }
  ```
  $\to$ **Hậu quả:** `Player` và `Monster` chỉ là những cái xác rỗng. `CombatManager` dính chặt vào cấu trúc nội bộ của vũ khí, giáp, thanh máu. Khi game thêm cơ chế "Quái có khiên hấp thụ sát thương", ta phải sửa thẳng vào `CombatManager`, vi phạm nghiêm trọng Open-Closed Principle.
* **Ứng dụng GRASP từ Chương 16:**
  1. *Controller:* `CombatEventController` chỉ nhận sự kiện `OnHitEvent(attackerID, targetID)` từ mạng và điều phối.
  2. *Information Expert (Damage Calculation):* Giao việc tính sát thương cuối cùng cho `TargetMonster`:
     * `target.TakeDamage(incomingDamage)`: Chính con quái vật mới là Expert nắm rõ nó đang có khiên hay không, giáp bao nhiêu, đang có buff giảm sát thương không.
  3. *Creator:* Khi một đòn đánh chí mạng xảy ra, `TargetMonster` đóng vai trò là Creator để tạo ra một thể hiện `FloatingCombatText(amount, isCrit)` bay lên màn hình.
  4. *Kết quả:* `CombatEventController` cực kỳ gọn nhẹ (*High Cohesion*), logic bảo vệ giáp nằm kín bên trong `Monster` (*Encapsulation & Low Coupling*).

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 15: Interaction Diagram Notation](./15_Interaction_Diagram_Notation.md) | [Chương 17: Design Model Use Case Realizations with GRASP ►](./17_Design_Model_Use_Case_Realizations_with_GRASP.md)
