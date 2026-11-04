[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 26: Modeling Generalization](./26_Modeling_Generalization.md) | [Chương 28: Adding New SSDs and Contracts ►](./28_Adding_New_SSDs_and_Contracts.md)

---

## CHƯƠNG 27: REFINING THE DOMAIN MODEL

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **27.1. Association Classes** *(Lớp Quan hệ / Lớp Kết hợp: Khi mối liên kết sở hữu thuộc tính riêng)*
* **27.2. Aggregation and Composition** *(Thu gom vs. Hợp thành: Ranh giới Kim cương Đen và Kim cương Trắng)*
  * *Composition: The Black Diamond (Hợp thành sống còn, sở hữu độc quyền)*
  * *Aggregation: The White Diamond (Thu gom chia sẻ – Lời khuyên né tránh của Larman)*
* **27.3. Time Intervals and Historical Association** *(Khoảng thời gian và Quan hệ Lịch sử)*
* **27.4. Qualified Associations** *(Quan hệ Định danh / Có phẩm định: Ký hiệu Qualifier và Bảng băm)*
* **27.5. Association Role Names** *(Tên vai trò ở các đầu mối quan hệ)*
* **27.6. Packages: Organizing the Domain Model** *(Gói: Tổ chức Mô hình Miền quy mô lớn)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 27 cung cấp các công cụ mô hình hóa tinh vi để giải quyết các cấu trúc dữ liệu phức tạp trong thực tế: **Thuộc tính của mối quan hệ**, **Vòng đời ràng buộc giữa các thành phần**, và **Tối ưu hóa tìm kiếm dữ liệu $O(1)$**.

Nếu không tinh chỉnh Domain Model bài bản, hệ thống sẽ gặp các thảm họa:
* **Gán nhầm thuộc tính của mối quan hệ vào thực thể (Attribute Misplacement Disaster):** Mức lương (`salary`) của nhân viên không thuộc về riêng `Employee` (vì một người có thể làm việc cho nhiều công ty với mức lương khác nhau), cũng không thuộc về riêng `Company`. Nhét `salary` vào `Employee` sẽ làm mất khả năng mô hình hóa đa hợp đồng lao động.
* **Mơ hồ về Vòng đời Bộ nhớ (Memory Lifecycle Leak via Aggregation Confusion):** Không phân định được Composition (phần con chết khi phần cha chết) và Association thông thường, dẫn đến việc giải phóng đối tượng cha mà để sót hàng triệu đối tượng con mồ côi rải rác trong bộ nhớ (*Dangling Objects*).
* **Mô hình hóa Tìm kiếm Kém hiệu năng ($O(N)$ Traversal Bottleneck):** Không dùng Qualified Associations, khiến lập trình viên phải viết vòng lặp duyệt qua toàn bộ mảng hàng nghìn phần tử để tìm kiếm thay vì tra cứu trực tiếp qua Bảng băm (*Hash Map Lookup $O(1)$*).

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------------+
|                  CÁC CÔNG CỤ TINH CHỈNH MÔ HÌNH MIỀN NÂNG CAO                           |
+-----------------------------------------------------------------------------------------+
| [ASSOCIATION CLASS]     : Một lớp được gắn vào một đường Association (nối bằng nét đứt).|
|                           Dùng khi thuộc tính phụ thuộc vào SỰ KẾT HỢP của cả 2 lớp.    |
+-----------------------------------------------------------------------------------------+
| [COMPOSITION]           : Kim cương ĐEN (*--): Quan hệ Sở hữu Tuyệt đối.                |
|                           - Phần con chỉ thuộc về DUY NHẤT một phần cha tại một thời điểm|
|                           - Khi phần cha bị tiêu hủy, phần con BẮT BUỘC bị tiêu hủy theo|
+-----------------------------------------------------------------------------------------+
| [QUALIFIED ASSOCIATION] : Hộp Qualifier nằm ở đầu Association (như một Khóa Tra cứu).   |
|                           Giảm bản số từ [1 -- *] xuống [1 -- 0..1] qua khóa băm Map.  |
+-----------------------------------------------------------------------------------------+
```

#### 2.1. Association Classes (Lớp Quan hệ)
* **Khi nào sử dụng?** Khi một giá trị thuộc tính chỉ có ý nghĩa **trong mối quan hệ giữa hai thực thể**, không thuộc về riêng thực thể nào.
* *Ví dụ trong NextGen POS:* Giá bán của một mặt hàng tại một chi nhánh siêu thị cụ thể (`StoreItemPrice`). Cùng một hộp sữa, nhưng bán ở siêu thị Quận 1 có giá 30k, bán ở siêu thị ngoại ô có giá 28k. Do đó, `price` không thể nhét cứng vào `ProductDescription` toàn cục; nó là một **Association Class** nằm giữa `Store` và `ProductDescription`.

#### 2.2. Composition (Hợp thành) vs. Aggregation (Thu gom)
Craig Larman đưa ra quan điểm kỹ thuật vô cùng rõ ràng:
* **Composition (Kim cương ĐEN $\blacklozenge$):** Có giá trị ngữ nghĩa mạnh mẽ và rõ ràng:
  1. *Tính không chia sẻ (*Non-sharable*):* Một bộ phận con chỉ thuộc về đúng 1 vật cha tại 1 thời điểm.
  2. *Vòng đời phụ thuộc (*Coincident Lifetime*):* Khi vật cha bị xóa khỏi bộ nhớ, toàn bộ các bộ phận con tự động bị tiêu hủy theo.
  3. *Ví dụ:* `Sale` $\blacklozenge\longrightarrow$ `SalesLineItem`. Nếu xóa một đơn hàng, các dòng hàng trong đơn hàng đó bắt buộc phải bị tiêu hủy.
* **Aggregation (Kim cương TRẮNG $\lozenge$):** Larman đưa ra lời khuyên: **HÃY NÉ TRÁNH KIM CƯƠNG TRẮNG!** Ngữ nghĩa của Aggregation trong chuẩn UML cực kỳ mơ hồ và không mang lại giá trị gia tăng nào cho việc thiết kế code. Hãy dùng Association thông thường hoặc Composition.

#### 2.3. Qualified Associations (Quan hệ Phẩm định / Có khóa)
* Thể hiện một bộ chọn (*Selector / Qualifier Key*) làm giảm bản số của mối quan hệ.
* Thay vì vẽ: `ProductCatalog` [1] $\longleftrightarrow$ [*] `ProductDescription` (muốn tìm phải duyệt mảng),
* Ta vẽ một **Hộp Qualifier `[itemID]`** nằm bên phía `ProductCatalog`: `ProductCatalog` [itemID] $\longleftrightarrow$ [0..1] `ProductDescription`.
* **Ánh xạ Mã nguồn:** Đây là hình ảnh đại diện trực quan hoàn hảo cho cấu trúc **Bảng băm / Từ điển (*Map / Dictionary*)**:
  ```java
  private Map<ItemID, ProductDescription> descriptions = new HashMap<>();
  ```

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là mã nguồn Java hiện thực hóa chuẩn mực cả 3 cấu trúc tinh chỉnh: **Qualified Association** (tra cứu Map $O(1)$), **Composition** (tiêu hủy dây chuyền), và **Association Class**:

```java
// ==================== 1. QUALIFIED ASSOCIATION VIA MAP ====================
public class ProductCatalog {
    // Ánh xạ từ Qualified Association: Catalog [itemID] ----> [0..1] ProductDescription
    // Cho phép tra cứu trực tiếp O(1) thay vì duyệt mảng O(N)
    private final Map<ItemID, ProductDescription> descriptions = new HashMap<>();

    public void addDescription(ItemID id, ProductDescription desc) {
        descriptions.put(id, desc);
    }

    public ProductDescription getProductDescription(ItemID id) {
        return descriptions.get(id); // Tra cứu tức thì bằng khóa
    }
}

// ==================== 2. COMPOSITION LIFECYCLE DESTRUCTION ====================
public class Sale {
    // Composition (Black Diamond): Sale sở hữu độc quyền lineItems
    private final List<SalesLineItem> lineItems = new ArrayList<>();

    public void makeLineItem(ProductDescription desc, int quantity) {
        lineItems.add(new SalesLineItem(desc, quantity));
    }

    // Khi Sale bị tiêu hủy/hủy bỏ, các SalesLineItem lập tức bị giải phóng
    public void clearAndDispose() {
        lineItems.clear(); // Giải phóng tham chiếu để Garbage Collector thu hồi vùng nhớ
    }
}

// ==================== 3. ASSOCIATION CLASS IN CODE ====================
// Thuộc tính giá bán theo từng chi nhánh Store
public class StoreItemPrice {
    private final Store store;
    private final ProductDescription product;
    private Money localizedPrice;

    public StoreItemPrice(Store store, ProductDescription product, Money price) {
        this.store = store;
        this.product = product;
        this.localizedPrice = price;
    }

    public Money getLocalizedPrice() { return localizedPrice; }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Đoạn mã then chốt | Kỹ thuật Tinh chỉnh áp dụng | Lợi ích Kiến trúc & Tối ưu hóa |
| :--- | :--- | :--- |
| `Map<ItemID, ProductDescription>` | **Qualified Association** | Chuyển đổi liên kết nhiều thành cấu trúc Dictionary tra cứu theo khóa với độ phức tạp thời gian tối ưu $O(1)$. |
| `lineItems.clear();` | **Composition Destruction** | Đảm bảo tính toàn vẹn vòng đời của Composition: Thành phần con không bao giờ tồn tại mồ côi nếu thành phần cha bị xóa sổ. |
| `public class StoreItemPrice` | **Association Class** | Tách thuộc tính phụ thuộc vào 2 thực thể thành một lớp trung gian độc lập, hỗ trợ đa giá bán cho nhiều chi nhánh siêu thị. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ Lớp Thiết kế thể hiện đầy đủ các kỹ thuật tinh chỉnh: Qualified Association, Composition (Kim cương Đen), và Association Class:

```mermaid
classDiagram
    direction TB

    class ProductCatalog {
        +getProductDescription(id: ItemID) ProductDescription
    }

    class ProductDescription {
        -id : ItemID
        -description : String
    }

    class Store {
        -storeID : String
    }

    class StoreItemPrice {
        <<Association Class>>
        -localizedPrice : Money
        -effectiveDate : Date
    }

    class Sale {
        -dateTime : Date
        +clearAndDispose() void
    }

    class SalesLineItem {
        -quantity : int
    }

    ProductCatalog o--> ProductDescription : descriptions (Qualified by itemID)
    
    Sale "1" *--> "*" SalesLineItem : lineItems (Composition)
    
    Store "1" -- "*" ProductDescription : Sells >
    (Store, ProductDescription) .. StoreItemPrice

    note for ProductCatalog "QUALIFIED ASSOCIATION:<br/>Tra cứu qua khóa itemID<br/>Ánh xạ thành Map~ItemID, Desc~"
    note for Sale "COMPOSITION (*-->):<br/>Kim cương Đen: Xóa Sale<br/>là tiêu hủy toàn bộ LineItems!"
    note for StoreItemPrice "ASSOCIATION CLASS:<br/>Giá bán phụ thuộc vào sự kết hợp<br/>giữa Chi nhánh Store và Sản phẩm."
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Thiết kế Bản đồ Lưới Ô cờ (*Tilemap Grid*) và Trạng thái Thành viên Đội nhóm (*Party Member Status*) trong Game 2.5D Multiplayer.
* **Ứng dụng từ Chương 27 vào Game:**
  1. *Qualified Association cho Bản đồ 2.5D:*
     * Thay vì `GridMap` lưu một mảng phẳng 1 triệu ô gạch và duyệt `for` tuần tự để tìm ô gạch tại tọa độ $(x, y)$ ($O(N)$ gây tụt FPS thảm hại),
     * Ta mô hình hóa một **Qualified Association**: `GridMap [TileCoord] ----> [0..1] TileData`.
     * Trong mã nguồn C#, ánh xạ thành `Dictionary<Vector2Int, TileData> tiles;` $\to$ Cho phép tra cứu ô gạch người chơi đang đứng chỉ mất đúng 1 nano-giây ($O(1)$).
  2. *Association Class cho Đội nhóm (Party):*
     * Một `Player` tham gia vào một `Party`. Thuộc tính "Tỷ lệ ăn chia điểm kinh nghiệm" (*ExpShareRatio: 40%*) và "Thời gian gia nhập" không thuộc về riêng người chơi, cũng không thuộc về cả nhóm.
     * $\to$ Tạo Association Class `PartyMembership` nằm giữa `Player` và `Party`.
  3. *Composition cho Boss và Bộ phận Cơ thể:*
     * Con Rồng khổng lồ (`BossDragon`) có Đầu, Thân, Đuôi, và 2 Cánh.
     * $\to$ Sử dụng **Composition (Kim cương Đen)**: `BossDragon` $\blacklozenge\longrightarrow$ `BossLimbComponent`. Khi Boss bị tiêu diệt và biến mất, toàn bộ các bộ phận hitbox chân, cánh tự động bị tiêu hủy sạch sẽ khỏi bộ nhớ.

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 26: Modeling Generalization](./26_Modeling_Generalization.md) | [Chương 28: Adding New SSDs and Contracts ►](./28_Adding_New_SSDs_and_Contracts.md)
