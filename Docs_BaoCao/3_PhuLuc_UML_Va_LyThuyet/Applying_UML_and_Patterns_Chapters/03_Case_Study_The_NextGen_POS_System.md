[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 02: Iterative Development and the Unified Process](./02_Iterative_Development_and_the_Unified_Process.md) | [Chương 04: Inception ►](./04_Inception.md)

---

## CHƯƠNG 3: CASE STUDY: THE NEXTGEN POS SYSTEM

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **3.1. The NextGen POS System** *(Hệ thống Điểm bán hàng NextGen POS)*
* **3.2. Architectural Layers and Case Study Emphasis** *(Các tầng kiến trúc và Trọng tâm nghiên cứu tình huống)*
  * *3.2.1. User Interface Layer (Tầng Giao diện người dùng)*
  * *3.2.2. Application Logic and Domain Objects Layer (Tầng Logic nghiệp vụ và Đối tượng miền)*
  * *3.2.3. Technical Services / Infrastructure Layer (Tầng Dịch vụ kỹ thuật / Hạ tầng)*
* **3.3. The Book's Strategy: Iterative Learning and Development** *(Chiến lược học tập và phát triển lặp của cuốn sách)*
  * *3.3.1. Iteration 1: Fundamental OOA/D Skills (Vòng lặp 1: Kỹ năng OOA/D nền tảng)*
  * *3.3.2. Iteration 2: Advanced GRASP and GoF Patterns (Vòng lặp 2: Mẫu hình nâng cao)*
  * *3.3.3. Iteration 3: Architectural Analysis and Persistence (Vòng lặp 3: Phân tích kiến trúc & Lưu trữ)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 3 đặt ra bài toán kiến trúc cho một hệ thống bán hàng đa thiết bị, đa dịch vụ với các yêu cầu cực kỳ phức tạp: Hỗ trợ nhiều thiết bị phần cứng đầu cuối (máy quét mã vạch, cân điện tử, máy quẹt thẻ), tích hợp đa dạng dịch vụ ngoài (tính thuế, cấp phép thẻ tín dụng), và **khả năng sống sót khi mất mạng (*Offline Fault Tolerance*)**.

Nếu thiết kế theo lối mòn thiếu phân tầng chuẩn mực, hệ thống sẽ hứng chịu thảm họa:
* **Hội chứng Khóa cứng Nhà cung cấp (Vendor Lock-in):** Logic phần mềm bị ràng buộc cứng với một loại máy quét mã vạch hoặc một cổng thanh toán cụ thể. Khi siêu thị muốn đổi nhà cung cấp cổng thanh toán để giảm phí giao dịch, lập trình viên phải sửa đổi hàng chục nghìn dòng code logic bán hàng.
* **Tê liệt toàn diện khi đứt kết nối mạng (Network Single Point of Failure):** Nếu mã nguồn logic thanh toán gọi trực tiếp API đám mây mà không có cơ chế phân tầng và lưu trữ đệm cục bộ (*Local Store-and-Forward*), toàn bộ quầy thu ngân của chuỗi siêu thị sẽ phải ngừng hoạt động ngay khi đường truyền internet gặp sự cố.
* **Giao diện người dùng ký sinh vào Nghiệp vụ (UI-Business Entanglement):** Các biến trạng thái của màn hình (như trạng thái nút bấm, màu sắc nhãn) bị đem vào tính toán giá thành hóa đơn, khiến việc tự động hóa kiểm thử đơn vị (*Unit Testing*) cho logic thanh toán trở thành nhiệm vụ bất khả thi.

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------+
|                           USER INTERFACE LAYER (GUI)                              |
|           Màn hình bán hàng, Bàn phím ảo, Hiển thị hóa đơn (Thụ động)             |
+-----------------------------------------------------------------------------------+
                                         |
                                         | Chỉ phụ thuộc 1 chiều xuống dưới
                                         v
+-----------------------------------------------------------------------------------+
|              APPLICATION LOGIC & DOMAIN OBJECTS LAYER (TRỌNG TÂM CỦA SÁCH)        |
|               Register, Sale, SalesLineItem, Payment, Store, ProductCatalog       |
+-----------------------------------------------------------------------------------+
                                         |
                                         | Chỉ phụ thuộc 1 chiều xuống dưới
                                         v
+-----------------------------------------------------------------------------------+
|                       TECHNICAL SERVICES / INFRASTRUCTURE                         |
|     Persistence (Database), Logging, Hardware Drivers, Third-Party Adapters       |
+-----------------------------------------------------------------------------------+
```

#### 2.1. Kiến trúc phân tầng logic (Logical Layered Architecture)
Larman xác định rõ ranh giới giữa 3 tầng then chốt:
1. **Tầng Giao diện người dùng (User Interface - UI):** 
   * Trách nhiệm: Nhận sự kiện từ người dùng (click, phím bấm, quét mã) và hiển thị kết quả. 
   * Nguyên tắc tối thượng: **Tuyệt đối không chứa logic nghiệp vụ.** UI không được phép tự tính toán thuế, không tự cộng tiền, không tự quyết định hóa đơn có hợp lệ hay không.
2. **Tầng Logic Ứng dụng & Đối tượng Miền (Application Logic & Domain Objects):**
   * Đây là trái tim của hệ thống. Chứa các đối tượng nghiệp vụ thuần túy đại diện cho các khái niệm thế giới thực (`Sale`, `Register`, `Item`).
   * Tầng này hoàn toàn không biết gì về giao diện (UI) đang hiển thị nó (dù là Swing, Web, CLI hay Mobile App). Điều này đảm bảo tính độc lập và khả năng kiểm thử tự động (*Headless Automated Testing*).
3. **Tầng Dịch vụ Kỹ thuật (Technical Services / Infrastructure):**
   * Trách nhiệm: Cung cấp các dịch vụ hỗ trợ kỹ thuật không phụ thuộc nghiệp vụ cụ thể như: Ghi log (*Logging*), Lưu trữ dữ liệu (*Database Persistence*), Giao tiếp cổng COM/USB với phần cứng (*Hardware I/O*).

#### 2.2. Quy tắc Phụ thuộc Một Chiều (The One-Way Dependency Rule)
* Các tầng trên có thể gọi xuống tầng dưới ngay cạnh nó.
* **Tuyệt đối không có chiều ngược lại:** Tầng dưới không được phép phụ thuộc trực tiếp (import/reference) tầng trên. Tầng Miền nghiệp vụ không được phép import bất kỳ thư viện nào của tầng UI.
* Nếu tầng dưới cần thông báo dữ liệu cho tầng trên (ví dụ: cập nhật màn hình khi nhận được tiền), việc đó phải được thực hiện gián tiếp thông qua các mẫu hình như **Observer (GoF)** hoặc **Event-Driven Architecture**, nhằm bảo vệ sự độc lập hoàn hảo của Tầng Miền.

#### 2.3. Chiến lược học tập lặp 3 giai đoạn của Cuốn sách
Larman không dồn toàn bộ kiến thức vào một lần, mà tổ chức theo đúng lộ trình lặp của một dự án thực tế:
* **Vòng lặp 1 (Iteration 1):** Nắm vững các kỹ năng cốt lõi cơ bản nhất của OOA/D: Phân tích Use Case, xây dựng Domain Model ban đầu, phân bổ trách nhiệm bằng các mẫu GRASP cơ bản (Creator, Information Expert, Controller), vẽ DCD và chuyển sang mã nguồn Java.
* **Vòng lặp 2 (Iteration 2):** Đối mặt với các yêu cầu thay đổi và độ phức tạp tăng lên: Ứng dụng các mẫu hình GRASP nâng cao (Polymorphism, Pure Fabrication, Indirection, Protected Variations) và các mẫu hình GoF kinh điển (Strategy, Factory, Adapter, Singleton).
* **Vòng lặp 3 (Iteration 3):** Xử lý các bài toán kiến trúc quy mô lớn: Phân tích kiến trúc chuyên sâu (*Architectural Analysis*), biểu đồ trạng thái cho luồng thanh toán phức tạp, và tự tay thiết kế một Framework lưu trữ dữ liệu đối tượng (*Object-Relational Mapping / Persistence Framework*).

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là đoạn mã kiến trúc minh họa sự phân tách rạch ròi giữa **Tầng UI**, đối tượng điều phối **Controller (Tầng Miền)**, và các **Domain Objects**, hiện thực hóa quy tắc phụ thuộc một chiều trong ca sử dụng nhập mặt hàng (`enterItem`):

```java
// ==================== TẦNG GIAO DIỆN NGƯỜI DÙNG (UI LAYER) ====================
public class ProcessSaleJFrame {
    private Register register; // Tham chiếu xuống tầng dưới

    public ProcessSaleJFrame(Register register) {
        this.register = register;
    }

    // Xử lý sự kiện khi máy quét barcode đọc được mã
    public void onBarcodeScanned(String itemId, int quantity) {
        // UI CHỈ chuyển tiếp thông tin, KHÔNG làm logic tại đây
        register.enterItem(new ItemID(itemId), quantity);
        updateDisplay();
    }
    
    private void updateDisplay() {
        // Lấy thông tin hóa đơn hiện tại để vẽ lên giao diện
    }
}

// ================= TẦNG LOGIC MIỀN (APPLICATION / DOMAIN LAYER) =================
public class Register {
    private ProductCatalog catalog;
    private Sale currentSale;

    public Register(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    // Phương thức xử lý hệ thống (System Operation) đóng vai trò Controller
    public void enterItem(ItemID id, int quantity) {
        ProductDescription desc = catalog.getProductDescription(id);
        if (desc == null) {
            throw new ItemNotFoundException("Không tìm thấy mặt hàng: " + id);
        }
        currentSale.createSalesLineItem(desc, quantity);
    }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Dòng code / Đoạn code then chốt | Giải thích kỹ thuật & Ý nghĩa thiết kế kiến trúc |
| :--- | :--- |
| `private Register register;` | Tầng UI giữ tham chiếu tới một đối tượng thuộc Tầng Miền đóng vai trò **Controller**. UI chỉ biết gửi thông điệp xuống, Tầng Miền hoàn toàn không giữ tham chiếu ngược lại `ProcessSaleJFrame`. |
| `register.enterItem(new ItemID(itemId), quantity);` | **Tách biệt mối quan tâm (Separation of Concerns):** UI không tự chọc vào Database để tìm mặt hàng, không tự tạo `SalesLineItem`. Nó gửi sự kiện hệ thống `enterItem` kèm các tham số kiểu dữ liệu nguyên thủy/value object. |
| `public class Register ...` | Hiện thực hóa mẫu hình **Controller (GRASP)**: `Register` đóng vai trò là điểm tiếp nhận sự kiện hệ thống đầu tiên sau tầng giao diện, điều phối luồng nghiệp vụ mà không làm nhiệm vụ hiển thị đồ họa. |
| `ProductDescription desc = catalog.getProductDescription(id);` | **Ủy thác nghiệp vụ chính xác:** `Register` biết rằng `ProductCatalog` là chuyên gia nắm giữ danh mục thông tin sản phẩm (*Information Expert*), do đó ủy thác việc tìm kiếm cho `catalog`. |
| `currentSale.createSalesLineItem(desc, quantity);` | Thể hiện mối quan hệ **Creator (GRASP)**: Đảm bảo việc thêm sản phẩm vào hóa đơn thuộc về chính đối tượng `Sale`, duy trì tính toàn vẹn trạng thái nội bộ của đơn hàng. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ phân tầng kiến trúc (Package Diagram / Logical Architecture) của hệ thống NextGen POS thể hiện rõ ranh giới các tầng và sự tuân thủ nghiêm ngặt Quy tắc Phụ thuộc Một Chiều:

```mermaid
classDiagram
    direction TB

    namespace UI_Layer {
        class ProcessSaleView {
            +onBarcodeScanned(id, qty)
            +onPaymentSubmitted()
        }
    }

    namespace Domain_Layer {
        class Register {
            +enterItem(id, qty)
            +endSale()
            +makePayment(cashTendered)
        }
        class Sale {
            -dateTime: Date
            +createSalesLineItem(desc, qty)
            +getTotal(): Money
        }
        class ProductCatalog {
            +getProductDescription(id): ProductDescription
        }
    }

    namespace Infrastructure_Layer {
        class IPersistenceFacade {
            <<interface>>
            +get(id, Class): Object
            +put(id, Object)
        }
        class ITaxCalculatorAdapter {
            <<interface>>
            +getTax(Sale): Money
        }
        class ScannerDriver {
            +listenPort()
        }
    }

    ProcessSaleView --> Register : Gửi sự kiện nghiệp vụ
    Register --> ProductCatalog : Tra cứu thông tin
    Register --> Sale : Quản lý hóa đơn hiện tại
    Domain_Layer ..> Infrastructure_Layer : Sử dụng dịch vụ gián tiếp qua Interface

    style UI_Layer fill:#e1f5fe,stroke:#01579b
    style Domain_Layer fill:#e8f5e9,stroke:#2e7d32
    style Infrastructure_Layer fill:#fff3e0,stroke:#e65100
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Kiến trúc Phân tầng trong Game 2.5D Multiplayer (tách biệt Game Engine / Simulation / Network Transport).
* **Vấn đề thực tế nếu vi phạm kiến trúc phân tầng NextGen POS:**
  Lập trình viên thường viết mã điều khiển nhân vật gắn chặt với API của Engine (ví dụ: trong Unity gọi trực tiếp `Input.GetAxis()`, `transform.Translate()`, `Rigidbody.AddForce()` và `GetComponent<PhotonView>().RPC()`). Điều này tạo ra một mớ bòng bong không thể tách rời: Logic chiến đấu gắn chết với đồ họa và engine mạng. Khi muốn chuyển từ Unity sang Godot, hoặc muốn chạy game logic trên Linux Server không có màn hình (*Headless Server*) để chống hack/cheat, toàn bộ dự án coi như phá sản vì Server không thể chạy các hàm liên quan đến GPU hay Engine Transform.
* **Ứng dụng tư duy Phân tầng Kiến trúc từ Chương 3:**
  1. *Presentation Layer (UI/Render):* Chỉ chứa các script hiển thị: Sprite Renderer, Animation Controller, Audio Player, Particle System. Tầng này chỉ lắng nghe trạng thái từ tầng dưới để vẽ lên màn hình.
  2. *Domain / Simulation Layer (Pure C# / Pure C++):*
     * Chứa các thực thể toán học và nghiệp vụ thuần túy: `CharacterState`, `CombatCalculator`, `SkillTree`, `Vector2D` (tự viết, không dùng Vector của Engine).
     * Tuyệt đối không import bất kỳ thư viện đồ họa nào. Logic di chuyển, tính toán trừ máu (*Damage calculation*), thời gian hồi chiêu (*Cooldown*) diễn ra hoàn toàn tại đây.
  3. *Infrastructure Layer:* Chứa các Adapter mạng (Socket client, WebSocket, LiteNetLib) và Adapter phần cứng (Input Reader).
  4. *Lợi ích tối thượng:* Cùng một tầng **Domain / Simulation Layer** này có thể biên dịch thẳng thành file `.dll` hoặc binary chạy trên **Dedicated Server** để làm trọng tài tính toán chống gian lận (*Authoritative Server Simulation*), đồng thời chạy được trên Client để dự đoán khung hình (*Client-side Prediction*), tối ưu hóa tái sử dụng mã nguồn 100%.

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 02: Iterative Development and the Unified Process](./02_Iterative_Development_and_the_Unified_Process.md) | [Chương 04: Inception ►](./04_Inception.md)
