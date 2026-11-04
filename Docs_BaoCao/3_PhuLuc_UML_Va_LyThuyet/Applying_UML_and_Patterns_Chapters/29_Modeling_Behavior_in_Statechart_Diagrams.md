[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 28: Adding New SSDs and Contracts](./28_Adding_New_SSDs_and_Contracts.md) | [Chương 30: Designing the Logical Architecture with Patterns ►](./30_Designing_the_Logical_Architecture_with_Patterns.md)

---

## CHƯƠNG 29: MODELING BEHAVIOR IN STATECHART DIAGRAMS

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **29.1. Events, States, and Transitions** *(Sự kiện, Trạng thái và Sự chuyển dịch)*
* **29.2. Statechart Diagrams (UML State Machine Diagrams)** *(Sơ đồ Biểu đồ Trạng thái)*
* **29.3. Statechart Diagrams in the Unified Process?** *(Vị trí của Biểu đồ Trạng thái trong UP)*
* **29.4. Use Case Statechart Diagrams** *(Biểu đồ Trạng thái cho Ca sử dụng: Định nghĩa trình tự hợp lệ của các thao tác)*
* **29.5. Use Case Statechart Diagrams for the POS Application** *(Ứng dụng Biểu đồ Trạng thái vào Hệ thống NextGen POS)*
* **29.6. Classes that Benefit from Statechart Diagrams** *(Những loại lớp nào hưởng lợi nhiều nhất từ Biểu đồ Trạng thái?)*
  * *State-dependent objects (Đối tượng phụ thuộc trạng thái)*
  * *Reactive objects (Đối tượng phản ứng / Thiết bị phần cứng)*
  * *Communication protocols (Giao thức truyền thông)*
* **29.7. External and Event-Driven Systems** *(Hệ thống Hướng sự kiện và Tích hợp bên ngoài)*
* **29.8. State Modeling Notation** *(Ký hiệu mô hình hóa trạng thái chi tiết)*
  * *Event / Trigger, Guard Condition, Action (Cú pháp: trigger [guard] / action)*
  * *Composite / Nested States (Trạng thái lồng nhau)*
  * *History States (Trạng thái lịch sử)*
* **29.9. Further Readings** *(Tài liệu tham khảo chuyên sâu)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 29 cung cấp công cụ toán học và trực quan hóa mạnh mẽ nhất để làm chủ **Các đối tượng phụ thuộc trạng thái (*State-Dependent Objects*)** – những đối tượng mà hành vi phản ứng của chúng thay đổi hoàn toàn tùy thuộc vào trạng thái hiện tại.

Nếu không mô hình hóa trạng thái bài bản, hệ thống sẽ hứng chịu các thảm họa:
* **Hội chứng Bùng nổ Biến cờ Boolean (Boolean Flag Explosion):** Lập trình viên cố gắng kiểm soát trạng thái bằng hàng tá biến cờ boolean: `isStarted`, `isItemEntered`, `isEnded`, `isPaid`, `isCancelled`. Khi số lượng cờ tăng lên, tổ hợp trạng thái ($2^N$) biến code thành ma trận `if-else` lồng nhau rối rắm, không ai kiểm soát nổi.
* **Vi phạm Thứ tự Giao dịch Hợp lệ (Illegal Sequence Violations):** Khách hàng quẹt thẻ thanh toán khi chưa nhập mặt hàng nào, hoặc thu ngân tiếp tục quét thêm mặt hàng sau khi khách đã trả tiền xong. Thiếu máy trạng thái (*State Machine*), hệ thống không có chốt chặn ngăn cản các thao tác phi lý.
* **Treo kết nối Giao thức (Protocol Deadlocks):** Trong các giao thức mạng hoặc giao tiếp thiết bị phần cứng (máy in, máy quẹt thẻ), việc nhận thông điệp sai thời điểm làm đối tượng rơi vào trạng thái "chờ vĩnh viễn" (*Hanging State*), gây tê liệt toàn bộ quầy thu ngân.

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------------+
|                              CÚ PHÁP CHUYỂN DỊCH TRẠNG THÁI UML                         |
+-----------------------------------------------------------------------------------------+
|                        trigger (event) [guard] / action                                 |
|      +---------------+ -----------------------------------> +---------------+            |
|      |  Open (State) |                                      | ReadyToPay    |            |
|      +---------------+                                      +---------------+            |
|                                                                                         |
|  - Trigger : Sự kiện kích hoạt (ví dụ: endSale())                                       |
|  - Guard   : Điều kiện bảo vệ (ví dụ: [itemCount > 0] - chỉ chuyển nếu thỏa mãn)       |
|  - Action  : Hành động thực thi tức thì khi chuyển dịch (ví dụ: / computeGrandTotal())   |
+-----------------------------------------------------------------------------------------+
```

#### 2.1. Ba Thành tố Cốt lõi của Máy Trạng thái (Finite State Machine - FSM)
1. **Trạng thái (*State*):** Điều kiện hoặc tình trạng tồn tại của một đối tượng tại một thời điểm, thỏa mãn một số tiêu chí hoặc đang chờ đợi một sự kiện.
2. **Sự kiện (*Event / Trigger*):** Một biến cố đáng chú ý xảy ra trong thời gian và không gian, có khả năng kích hoạt sự chuyển đổi trạng thái (ví dụ: nhận thông điệp `makePayment()`).
3. **Sự chuyển dịch (*Transition*):** Mối quan hệ giữa hai trạng thái chỉ ra rằng một đối tượng ở trạng thái thứ nhất sẽ bước vào trạng thái thứ hai khi một sự kiện xảy ra và điều kiện bảo vệ được thỏa mãn.

#### 2.2. Cú pháp Chuyển dịch Đầy đủ trong UML
$$\text{trigger} \ (\text{param}) \ [\text{guard}] \ / \ \text{action}$$
* **Trigger:** Sự kiện đánh thức quá trình chuyển dịch (ví dụ: `enterItem(id, qty)`).
* **Guard Condition (`[...]`):** Biểu thức logic Boolean. Chuyển dịch CHỈ xảy ra nếu Guard trả về `true` tại thời điểm sự kiện kích hoạt.
* **Action (`/...`):** Một hành động tính toán ngắn gọn, không thể bị gián đoạn (*Atomic Action*) được thực thi ngay trong lúc đang chuyển dịch (ví dụ: `/ createSalesLineItem()`).

#### 2.3. Hai Ứng dụng Lớn của Statechart trong Quy trình UP
Craig Larman phân định rõ hai mục đích sử dụng Biểu đồ Trạng thái:
1. **Use Case Statechart Diagrams:** Mô phỏng **trình tự hợp lệ của các Thao tác Hệ thống** trong một ca sử dụng. Nó đóng vai trò là bản đặc tả API hợp lệ cho lập trình viên Client/Frontend: Thao tác nào được phép gọi trước, thao tác nào chỉ được gọi sau.
2. **Object Statechart Diagrams:** Mô phỏng vòng đời phức tạp của một **đối tượng miền cụ thể** (như `Sale` chuyển từ *Open $\to$ ReadyToPay $\to$ Completed $\to$ Cancelled*).

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là mã nguồn Java hiện thực hóa trọn vẹn Máy Trạng thái của `Sale` sử dụng **State Pattern (GoF)**, loại bỏ sạch sẽ các biến cờ boolean và câu lệnh `switch-case`:

```java
// ==================== STATE PATTERN FOR SALE LIFECYCLE ====================

// Giao diện trừu tượng đại diện cho một trạng thái hợp lệ
public interface ISaleState {
    void enterItem(Sale context, ProductDescription desc, int qty);
    Money endSale(Sale context);
    void makePayment(Sale context, Money amount);
}

// 1. TRẠNG THÁI: OPEN (Đang quét hàng)
public class OpenSaleState implements ISaleState {
    @Override
    public void enterItem(Sale context, ProductDescription desc, int qty) {
        // [Hành động hợp lệ]: Cho phép thêm dòng hàng
        context.getLineItemsInternal().add(new SalesLineItem(desc, qty));
    }

    @Override
    public Money endSale(Sale context) {
        // [GUARD CHECK]: Phải có ít nhất 1 mặt hàng mới cho kết thúc
        if (context.getLineItemsInternal().isEmpty()) {
            throw new IllegalStateException("Không thể kết thúc đơn hàng rỗng!");
        }
        // Chuyển dịch trạng thái: Open -> ReadyToPay
        context.setState(new ReadyToPaySaleState());
        return context.calculateTotal();
    }

    @Override
    public void makePayment(Sale context, Money amount) {
        throw new IllegalStateException("Lỗi thứ tự: Phải gọi endSale() trước khi thanh toán!");
    }
}

// 2. TRẠNG THÁI: READY TO PAY (Chờ thanh toán)
public class ReadyToPaySaleState implements ISaleState {
    @Override
    public void enterItem(Sale context, ProductDescription desc, int qty) {
        throw new IllegalStateException("Đơn hàng đã chốt! Không thể nhập thêm mặt hàng.");
    }

    @Override
    public Money endSale(Sale context) {
        return context.calculateTotal(); // Idempotent
    }

    @Override
    public void makePayment(Sale context, Money amount) {
        if (amount.getAmount().compareTo(context.calculateTotal().getAmount()) < 0) {
            throw new IllegalArgumentException("Số tiền đưa không đủ!");
        }
        context.setPaymentInternal(new Payment(amount));
        // Chuyển dịch trạng thái: ReadyToPay -> Completed
        context.setState(new CompletedSaleState());
    }
}

// 3. TRẠNG THÁI: COMPLETED (Đã thanh toán xong)
public class CompletedSaleState implements ISaleState {
    @Override
    public void enterItem(Sale context, ProductDescription desc, int qty) {
        throw new IllegalStateException("Giao dịch đã đóng vĩnh viễn!");
    }
    @Override
    public Money endSale(Sale context) {
        throw new IllegalStateException("Giao dịch đã hoàn tất!");
    }
    @Override
    public void makePayment(Sale context, Money amount) {
        throw new IllegalStateException("Hóa đơn này đã được trả tiền rồi!");
    }
}

// Context: Sale ủy thác toàn bộ hành vi cho trạng thái hiện tại
public class Sale {
    private ISaleState currentState = new OpenSaleState();
    private final List<SalesLineItem> lineItems = new ArrayList<>();
    private Payment payment;

    public void setState(ISaleState newState) { this.currentState = newState; }
    
    public void enterItem(ProductDescription desc, int qty) {
        currentState.enterItem(this, desc, qty);
    }
    public Money endSale() {
        return currentState.endSale(this);
    }
    public void makePayment(Money amount) {
        currentState.makePayment(this, amount);
    }

    // Các hàm nội bộ
    List<SalesLineItem> getLineItemsInternal() { return lineItems; }
    void setPaymentInternal(Payment p) { this.payment = p; }
    Money calculateTotal() { /* tính tổng tiền */ return new Money(100.0); }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Phương thức / Trạng thái | Kỹ thuật Máy trạng thái áp dụng | Lợi ích Kiến trúc & Ngăn chặn lỗi |
| :--- | :--- | :--- |
| `OpenSaleState.makePayment(...)` | **Chặn hành vi bất hợp lệ theo trạng thái** | Quăng ngoại lệ tức thì nếu thu ngân cố tình gọi thanh toán khi chưa kết thúc quét hàng, triệt tiêu lỗi logic nghiệp vụ. |
| `context.setState(new ReadyToPaySaleState())` | **Chuyển dịch trạng thái nguyên tử (*State Transition*)** | Hoàn thành chuyển dịch trạng thái mà không cần sửa bất kỳ biến cờ boolean nào trong lớp `Sale`. |
| `CompletedSaleState` | **Trạng thái kết thúc (*Terminal State*)** | Khóa chặt đối tượng sau khi hoàn tất, ngăn chặn hoàn toàn các lỗi cố tình sửa đổi đơn hàng đã xuất hóa đơn. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ Biểu đồ Trạng thái (Statechart Diagram) hoàn chỉnh cho vòng đời của một Đơn hàng (`Sale`) trong NextGen POS:

```mermaid
stateDiagram-v2
    direction TB

    [*] --> Open : makeNewSale()

    state Open {
        [*] --> EmptySale
        EmptySale --> HasItems : enterItem(id, qty) [valid id] / addLineItem()
        HasItems --> HasItems : enterItem(id, qty) / addLineItem()
    }

    Open --> ReadyToPay : endSale() [itemCount > 0] / calculateTotal()
    
    state ReadyToPay {
        [*] --> AwaitingPayment
        AwaitingPayment --> ReadyToPay : makePayment(amount) [amount < total] / promptRemaining()
    }

    ReadyToPay --> Completed : makePayment(amount) [amount >= total] / createPayment(), printReceipt()
    
    state Completed {
        [*] --> Closed
        note right of Closed : Hóa đơn bất biến, không thể sửa đổi
    }

    Open --> Cancelled : cancelSale() / clearItems()
    ReadyToPay --> Cancelled : cancelSale() / rollback()
    
    Completed --> [*]
    Cancelled --> [*]
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Máy Trạng thái Điều khiển Nhân vật (*Character Finite State Machine - FSM*) trong Game 2.5D Multiplayer.
* **Thảm họa game nếu dùng biến cờ boolean:**
  Lập trình viên khai báo trong script nhân vật:
  `bool isIdle, isMoving, isAttacking, isStunned, isCasting, isDead;`
  $\to$ **Lỗ hổng Game kinh điển (Game Exploits):**
  * Người chơi bị dính chiêu đóng băng của quái (`isStunned = true`), nhưng nếu nhanh tay bấm phím tấn công cùng lúc, hàm đánh kiểm tra thiếu điều kiện $\to$ Nhân vật bị đóng băng thành tượng đá nhưng kiếm vẫn tự động chém quái xung quanh (*Stun-Attack Glitch*)!
  * Nhân vật chết rồi (`isDead = true`) nhưng vẫn nhận được buff tăng tốc độ chạy và nhặt được đồ trên đất.
* **Ứng dụng Statechart từ Chương 29 vào Game:**
  1. *Định nghĩa các Trạng thái Rời rạc độc quyền:* `IdleState`, `MovingState`, `AttackingState`, `StunnedState`, `DeadState`. Tại một thời điểm, nhân vật CHỈ ĐƯỢC PHÉP ở đúng 1 trạng thái duy nhất.
  2. *Quy tắc Chuyển dịch khắt khe:*
     * Khi đang ở `StunnedState`: Mọi sự kiện phím bấm `OnAttackInput` hay `OnMoveInput` đều bị **nuốt chửng (ignored)**, không có transition nào đi ra từ sự kiện bấm phím.
     * Chỉ có một transition duy nhất: `TimeElapsed [duration >= stunTime] / ChangeState(IdleState)`.
  3. *Lợi ích:* Triệt tiêu 100% bug chồng chéo hoạt ảnh và khai thác lỗi trạng thái trong game đối kháng/chiến đấu nhiều người chơi.

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 28: Adding New SSDs and Contracts](./28_Adding_New_SSDs_and_Contracts.md) | [Chương 30: Designing the Logical Architecture with Patterns ►](./30_Designing_the_Logical_Architecture_with_Patterns.md)
