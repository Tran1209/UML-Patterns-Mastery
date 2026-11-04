[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 24: Iteration 3 and Its Requirements](./24_Iteration_3_and_Its_Requirements.md) | [Chương 26: Modeling Generalization ►](./26_Modeling_Generalization.md)

---

## CHƯƠNG 25: RELATING USE CASES

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **25.1. Relating Use Cases** *(Thiết lập mối quan hệ giữa các Ca sử dụng)*
* **25.2. The `«include»` Relationship** *(Quan hệ Bao gồm: Tái sử dụng logic chung vô điều kiện)*
* **25.3. The `«extend»` Relationship** *(Quan hệ Mở rộng: Mở rộng hành vi có điều kiện tại Extension Points)*
* **25.4. The `«generalize»` (Specialization) Relationship** *(Quan hệ Tổng quát hóa / Chuyên biệt hóa giữa các Use Cases)*
* **25.5. Use Case Diagrams with Relationships** *(Vẽ Sơ đồ Ca sử dụng tích hợp quan hệ)*
* **25.6. Caution: Don't Spend Too Much Time Decomposing Use Cases!** *(Cảnh báo nghiêm khắc: Cạm bẫy phân rã chức năng vụn vặt)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 25 giải quyết bài toán tổ chức và quản lý độ phức tạp khi **số lượng Use Cases tăng lên hàng chục hoặc hàng trăm**. Nó cung cấp công cụ để tái sử dụng yêu cầu mà không làm méo mó bản chất của ca sử dụng.

Nếu lập trình viên hiểu sai quan hệ Use Case, hệ thống sẽ gặp các thảm họa:
* **Cạm bẫy Phân rã Chức năng Vụn vặt (Functional Decomposition Antipattern):** Lập trình viên lấy một Use Case chuẩn rồi xé nhỏ nó thành 20 Use Case tý hon (ví dụ: tạo ca sử dụng "Bấm nút", "Nhập text", "Kiểm tra mật khẩu") rồi nối bằng chằng chịt mũi tên `«include»`. Kết quả: Tài liệu biến thành sơ đồ giải thuật lưu đồ (Flowchart) trá hình, mất hoàn toàn khái niệm về Giá trị Nghiệp vụ (EBP).
* **Nhầm lẫn tai hại giữa `«include»` và `«extend»`:** Dùng sai mũi tên khiến luồng xử lý bị đảo lộn, không biết đâu là luồng bắt buộc và đâu là nhánh tùy chọn mở rộng.
* **Phá vỡ Tính Độc lập của Ca sử dụng Gốc:** Sửa đổi ca sử dụng chính mỗi khi có một tính năng phụ trợ mới xuất hiện (ví dụ: cứ mỗi khi thêm chương trình khuyến mãi lại sửa lại văn bản Use Case `Process Sale`).

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------------+
|                  SO SÁNH BẢN CHẤT: «include» VS. «extend»                               |
+-----------------------------------------------------------------------------------------+
| TIÊU CHÍ                  QUAN HỆ «include»                QUAN HỆ «extend»             |
+-----------------------------------------------------------------------------------------+
| Bản chất thực thi         BẮT BUỘC (Unconditional)         TÙY CHỌN (Conditional)       |
| Nhận thức của Base UC     Base UC BIẾT rõ sub-UC           Base UC HOÀN TOÀN KHÔNG BIẾT |
| Điểm chèn                 Tại bước cụ thể trong luồng      Tại các Extension Points     |
| Hướng mũi tên             Base UC ----«include»---> Sub UC Extension UC ---«extend»--> Base|
| Ý nghĩa kiến trúc         Tái sử dụng logic dùng chung     Mở rộng mà không sửa đổi (OCP|
+-----------------------------------------------------------------------------------------+
```

#### 2.1. Quan hệ `«include»` (Bao hàm / Tái sử dụng)
* **Ý nghĩa:** Trích xuất một đoạn kịch bản hành vi lặp lại ở nhiều ca sử dụng khác nhau thành một ca sử dụng con dùng chung (*Sub-use case*).
* **Cơ chế:** Hoạt động giống như một **lời gọi hàm (*Subroutine Call*)**. Khi kịch bản của Base Use Case chạy tới bước đó, nó bắt buộc phải gọi Sub-use case thực thi xong rồi mới chạy tiếp.
* *Ví dụ:* Cả ca sử dụng `Process Sale` (Bán hàng) và `Handle Returns` (Trả hàng) đều cần thực hiện bước xác thực danh tính khách hàng $\to$ Tách thành ca sử dụng dùng chung `Authenticate Customer` và nối bằng mũi tên `«include»`.

#### 2.2. Quan hệ `«extend»` (Mở rộng có điều kiện)
* **Ý nghĩa:** Cho phép bổ sung hành vi mới vào một Use Case có sẵn **mà không được phép chỉnh sửa bất kỳ dòng chữ nào trong Use Case gốc** (Hiện thân của Open-Closed Principle ở cấp độ Yêu cầu).
* **Cơ chế:**
  1. Base Use Case định nghĩa các **Điểm mở rộng (*Extension Points*)**, ví dụ: `Point 1: Sau khi thanh toán hoàn tất`.
  2. Extension Use Case chỉ được kích hoạt **NẾU một điều kiện đặc thù xảy ra** (ví dụ: `Condition: Khách hàng mua trên 1 triệu và là hội viên VIP`).
  3. **ĐIỀU KỲ DIỆU:** Base Use Case hoàn toàn "mù", không hề biết có sự tồn tại của Extension Use Case!

#### 2.3. Lời cảnh báo của Craig Larman: Tránh xa Functional Decomposition!
* Quy tắc số một: **90% Use Cases của bạn KHÔNG CẦN quan hệ `«include»` hay `«extend»`!**
* Đừng tốn thời gian ngồi xé nhỏ ca sử dụng để thỏa mãn thú vui vẽ sơ đồ. Chỉ dùng quan hệ khi có sự trùng lặp kịch bản thực sự nghiêm trọng hoặc khi cần mở rộng hệ thống một cách có kiểm soát.

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là bản đặc tả kịch bản Use Case áp dụng chuẩn mực quan hệ `«include»` và `«extend»`, kèm theo kiến trúc mã nguồn **Hook Interceptor** hiện thực hóa quan hệ `«extend»` trong phần mềm:

```java
// ==================== ĐẶC TẢ TEXT CỦA USE CASE ====================
/*
USE CASE: Process Sale
Main Success Scenario:
    1. Customer arrives...
    2. Cashier starts sale...
    3. Cashier enters items...
    4. System computes total...
    5. Cashier handles payment: INCLUDE USE CASE: "Handle Payment".
    6. System logs transaction.
Extension Points:
    VIP_GIFT_AWARD: Sau bước 6.

USE CASE: Award VIP Bonus Gift (Extension Use Case)
Trigger: Được kích hoạt tại điểm mở rộng VIP_GIFT_AWARD của "Process Sale".
Condition: Tổng hóa đơn > $500 và Khách hàng sở hữu thẻ Platinum VIP.
Scenario:
    1. Hệ thống tự động sinh mã phiếu quà tặng trị giá $50.
    2. In phiếu quà tặng đính kèm vào biên lai của khách.
*/

// ==================== HIỆN THỰC HÓA «extend» QUA INTERCEPTOR ====================
public interface IPostSaleExtensionHook {
    void execute(Sale sale);
}

// Extension Use Case được hiện thực hóa thành một Plugin độc lập
public class VipBonusGiftExtensionPlugin implements IPostSaleExtensionHook {
    @Override
    public void execute(Sale sale) {
        // [CONDITION]: Chỉ thực thi nếu thỏa mãn điều kiện mở rộng
        if (sale.getTotal().getAmount().doubleValue() > 500.0 && sale.getCustomer().isPlatinumVIP()) {
            System.out.println("KÍCH HOẠT EXTENSION: Tặng phiếu quà tặng 50$ cho khách VIP!");
            sale.addBonusVoucher(new Voucher(50.0));
        }
    }
}

public class ProcessSaleWorkflow {
    private final List<IPostSaleExtensionHook> extensionHooks = new ArrayList<>();

    public void registerExtensionHook(IPostSaleExtensionHook hook) {
        extensionHooks.add(hook);
    }

    public void completeSale(Sale sale) {
        // Bước 5: INCLUDE USE CASE: Xử lý thanh toán
        executeHandlePaymentSubUseCase(sale);

        // Bước 6: EXTENSION POINT: Kích hoạt các luồng mở rộng nếu thỏa điều kiện
        for (IPostSaleExtensionHook hook : extensionHooks) {
            hook.execute(sale); // Base workflow hoàn toàn không biết logic cụ thể của hook!
        }
    }

    private void executeHandlePaymentSubUseCase(Sale sale) {
        // Luồng bắt buộc của Include
    }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Thành phần thiết kế | Quan hệ Use Case tương ứng | Ý nghĩa Kiến trúc & Tuân thủ OCP |
| :--- | :--- | :--- |
| `executeHandlePaymentSubUseCase(...)` | **Quan hệ `«include»`** | Luồng bắt buộc phải chạy; tách thành hàm con để tái sử dụng cho các nghiệp vụ khác. |
| `interface IPostSaleExtensionHook` | **Extension Point** | Cung cấp điểm neo cắm rút (*Hook*) để các tính năng mở rộng móc vào mà không cần sửa mã nguồn gốc của luồng bán hàng. |
| `VipBonusGiftExtensionPlugin` | **Quan hệ `«extend»`** | Đóng gói toàn bộ điều kiện và hành động của Extension Use Case thành một module độc lập, hiện thực hóa 100% tinh thần Open-Closed. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ Ca sử dụng UML thể hiện sự phân định rạch ròi giữa quan hệ `«include»` và `«extend»`:

```mermaid
flowchart LR
    Cashier((Thu ngân)) --> BaseUC(["Process Sale<br/>(Ca sử dụng Cốt lõi)"])

    SubUC(["Handle Payment<br/>(Xử lý Thanh toán)"])
    ExtUC(["Award VIP Bonus Gift<br/>(Tặng quà tri ân VIP)"])
    ReturnUC(["Handle Returns<br/>(Xử lý Trả hàng)"])

    Manager((Quản lý)) --> ReturnUC

    BaseUC -.->|«include»<br/>(Bắt buộc gọi)| SubUC
    ReturnUC -.->|«include»<br/>(Tái sử dụng logic)| SubUC

    ExtUC -.->|«extend»<br/>(Chỉ chạy khi Bill > 500$ & VIP)| BaseUC

    style BaseUC fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style SubUC fill:#e1f5fe,stroke:#0288d1
    style ExtUC fill:#fff3e0,stroke:#f57c00,stroke-dasharray: 5 5
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Quan hệ giữa các Ca sử dụng Gameplay trong Game 2.5D Multiplayer: Ca sử dụng `CompleteQuest` (Hoàn thành Nhiệm vụ).
* **Ứng dụng chuẩn mực «include» và «extend» vào Game Architecture:**
  1. *Quan hệ «include»:*
     * Khi người chơi hoàn thành nhiệm vụ (`CompleteQuest`) hoặc tiêu diệt Boss thế giới (`KillWorldBoss`), cả hai đều phải thực hiện bước: **Trao thưởng Kinh nghiệm và Tiền vàng**.
     * $\to$ Tách thành ca sử dụng dùng chung: `GrantReward` và nối bằng mũi tên `«include»`.
  2. *Quan hệ «extend»:*
     * Game có tính năng Thành tựu (*Achievement System*): Khi hoàn thành một chuỗi nhiệm vụ, hệ thống kiểm tra nếu đây là nhiệm vụ thứ 100 thì mở khóa danh hiệu "Kẻ Chinh Phục".
     * $\to$ Ca sử dụng gốc `CompleteQuest` **không nên chứa logic kiểm tra thành tựu**.
     * $\to$ Tạo ca sử dụng mở rộng: `UnlockAchievement` nối bằng mũi tên `«extend»` vào điểm mở rộng `OnQuestCompleted` của `CompleteQuest`. Game Designer có thể thêm 50 loại thành tựu mới mà không bao giờ làm lỗi code trả thưởng nhiệm vụ gốc!

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 24: Iteration 3 and Its Requirements](./24_Iteration_3_and_Its_Requirements.md) | [Chương 26: Modeling Generalization ►](./26_Modeling_Generalization.md)
