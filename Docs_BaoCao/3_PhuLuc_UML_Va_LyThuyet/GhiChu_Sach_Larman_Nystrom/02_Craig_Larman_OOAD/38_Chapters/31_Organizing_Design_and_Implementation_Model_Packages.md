[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 30: Designing the Logical Architecture with Patterns](./30_Designing_the_Logical_Architecture_with_Patterns.md) | [Chương 32: Introduction to Architectural Analysis and the SAD ►](./32_Introduction_to_Architectural_Analysis_and_the_SAD.md)

---

## CHƯƠNG 31: ORGANIZING THE DESIGN AND IMPLEMENTATION MODEL PACKAGES

### 1. ĐỀ MỤC & MỤC TIÊU CỐT LÕI (CORE PURPOSE)

#### Hệ thống đề mục (Heading Hierarchy)
* **31.1. Package Diagrams** *(Sơ đồ Gói UML: Ký hiệu thư mục và Đường phụ thuộc nét đứt)*
* **31.2. Package Organization Guidelines** *(Các nguyên tắc chỉ đạo tổ chức Gói)*
  * *Organization by Subsystem, Subject, and Layer (Tổ chức theo Hệ thống con, Chủ đề và Tầng kiến trúc)*
  * *Minimize Dependencies: The Acyclic Dependencies Principle - ADP (Nguyên lý Phụ thuộc Phi chu trình)*
  * *Package Cohesion Principles: CCP and CRP (Nguyên lý Gắn kết Gói: Đóng chung và Dùng chung)*
* **31.3. Organizing the NextGen POS Packages** *(Bố cục tổ chức Gói chuẩn mực cho NextGen POS)*
* **31.4. Managing Dependencies and Visibility in Packages** *(Kiểm soát phụ thuộc và Tầm nhìn Gói: Quyền truy cập Package-Private)*

#### Vấn đề kỹ thuật thực tế & Thảm họa kiến trúc được ngăn chặn
Chương 31 giải quyết bài toán tổ chức vật lý mã nguồn khi dự án tăng trưởng từ vài file lên **hàng trăm thư mục, packages, namespaces và thư viện DLL/JAR**.

Nếu không có chiến lược đóng gói chuẩn mực, dự án sẽ đối mặt với các thảm họa:
* **Thảm họa Phụ thuộc Chu trình Gói (Package Dependency Cycles Disaster):** Package A phụ thuộc Package B, Package B phụ thuộc Package C, rồi Package C lại phụ thuộc ngược lại Package A ($A \to B \to C \to A$). Hậu quả: Không thể biên dịch riêng lẻ từng module, thay đổi 1 dòng code ở Package C buộc hệ thống phải biên dịch lại toàn bộ dự án từ đầu, thời gian build tăng từ 30 giây lên 45 phút!
* **Gói Thùng rác (The Junk-Drawer Package Anti-Pattern):** Tạo ra các package vô nghĩa mang tên `common`, `utils`, `helpers` chứa hàng trăm class thập cẩm không liên quan. Mọi module trong hệ thống đều import `utils`, biến nó thành điểm nghẽn phụ thuộc nguy hiểm nhất.
* **Lộ lọt Chi tiết Cài đặt Nội bộ (Internal Implementation Leak):** Đặt tất cả các class trong package ở chế độ `public`. Các lập trình viên bên ngoài tùy tiện gọi vào các class hỗ trợ nội bộ thay vì đi qua Facade của package, phá hủy tính bao bọc (*Package Encapsulation*).

---

### 2. PHÂN TÍCH LÝ THUYẾT & NGUYÊN LÝ CHUYÊN SÂU

```
+-----------------------------------------------------------------------------------------+
|                  ACYCLIC DEPENDENCIES PRINCIPLE (ADP) & PHÁ VỠ CHU TRÌNH                |
+-----------------------------------------------------------------------------------------+
| [SAI - PHỤ THUỘC VÒNG TRÒN (CYCLE)]:                                                    |
|     +---------------+         +---------------+                                         |
|     |   Package A   | ------> |   Package B   |                                         |
|     +---------------+         +---------------+                                         |
|             ^                         |                                                 |
|             |                         v                                                 |
|             +------------------ +---------------+                                       |
|                                 |   Package C   |                                       |
|                                 +---------------+                                       |
+-----------------------------------------------------------------------------------------+
| [ĐÚNG - PHÁ VỠ CHU TRÌNH BẰNG ĐẢO NGƯỢC PHỤ THUỘC (DIP)]:                               |
|     +---------------+         +---------------+                                         |
|     |   Package A   | ------> |   Package B   |                                         |
|     +---------------+         +---------------+                                         |
|             |                         |                                                 |
|             v                         v                                                 |
|     +-------------------------------------------+                                       |
|     |   Package C (Chứa Interfaces trừu tượng)  |  ===> Đồ thị có hướng phi chu trình   |
|     +-------------------------------------------+       (Directed Acyclic Graph - DAG)  |
+-----------------------------------------------------------------------------------------+
```

#### 2.1. Cú pháp Sơ đồ Gói UML (Package Diagram Notation)
* Trong UML, một Package được biểu diễn bằng hình tượng **Thư mục có ngăn thẻ (*Tabbed Folder*)**.
* Mối quan hệ phụ thuộc giữa các package được thể hiện bằng **Mũi tên nét đứt có nhãn phụ thuộc (*Dependency Arrow* $\dashrightarrow$)**. Mũi tên từ Gói A trỏ sang Gói B có nghĩa là: Sự thay đổi bên trong Gói B có thể buộc Gói A phải thay đổi hoặc biên dịch lại.

#### 2.2. Ba Nguyên lý Đóng gói Cốt tử (Package Design Principles)
Craig Larman và Robert C. Martin (Uncle Bob) đặt ra 3 nguyên lý sống còn:
1. **Acyclic Dependencies Principle (ADP - Nguyên lý Phụ thuộc Phi chu trình):**
   * Đồ thị phụ thuộc giữa các package **tuyệt đối không được phép chứa chu trình (*No Cycles Allowed*)**.
   * Nó bắt buộc phải là một **Đồ thị Có hướng Phi chu trình (*Directed Acyclic Graph - DAG*)**.
   * *Giải pháp phá vỡ chu trình:* Nếu A phụ thuộc B và B phụ thuộc A $\to$ Trích xuất Interface chung đặt vào một Package thứ ba độc lập, áp dụng Dependency Inversion Principle.
2. **Common Closure Principle (CCP - Nguyên lý Đóng chung):**
   * Các lớp có cùng lý do thay đổi và thay đổi cùng một thời điểm thì nên được gom vào chung một Package.
   * Đây chính là nguyên lý Single Responsibility Principle (SRP) ở cấp độ Package.
3. **Common Reuse Principle (CRP - Nguyên lý Dùng chung):**
   * Các lớp không được sử dụng cùng nhau thì không nên bị ép buộc đóng gói chung với nhau. Tránh việc một client chỉ cần dùng 1 class nhỏ mà phải import cả một thư viện nặng 50MB.

#### 2.3. Kiểm soát Quyền truy cập Gói: Sức mạnh của Package-Private
* Nhiều lập trình viên chỉ biết `public` và `private`.
* Trong Java và C#, quyền truy cập cấp gói (**Package-Private** – không ghi modifier trong Java, hoặc `internal` trong C#) là công cụ cực kỳ lợi hại:
  * Trong một Package gồm 10 class: Chỉ có **1 Class Facade được để `public`** để thế giới bên ngoài nhìn thấy.
  * 9 Class còn lại để ở chế độ **Package-Private**.
  * **Lợi ích:** Toàn bộ chi tiết hiện thực phức tạp được che giấu kín đáo bên trong thư mục. Lập trình viên bên ngoài không thể "vô tình" gọi sai hàm nội bộ.

---

### 3. MỔ XẺ MÃ NGUỒN HOẶC ĐẶC TẢ THIẾT KẾ (DESIGN / CODE DEEP-DIVE)

Dưới đây là cấu trúc tổ chức thư mục mã nguồn NextGen POS và kỹ thuật phá vỡ phụ thuộc vòng tròn giữa Tầng Nghiệp vụ và Tầng Dịch vụ ngoài:

```
src/
├── com/nextgen/
│   ├── ui/                           # TẦNG GIAO DIỆN
│   │   ├── ProcessSaleJFrame.java
│   │   └── CashierConsoleView.java
│   │
│   ├── domain/                       # TẦNG MIỀN NGHIỆP VỤ (CORE)
│   │   ├── model/                    # Các thực thể nghiệp vụ cốt lõi
│   │   │   ├── Sale.java
│   │   │   ├── SalesLineItem.java
│   │   │   └── Register.java        # Package Facade
│   │   │
│   │   └── servicecontracts/         # INTERFACES TRỪU TƯỢNG (DIP)
│   │       ├── ITaxCalculator.java   # Phá vỡ chu trình phụ thuộc!
│   │       └── IAccounting.java
│   │
│   └── services/                     # TẦNG DỊCH VỤ HẠ TẦNG (PLUGINS)
│       ├── tax/
│       │   ├── GoodAsGoldTaxAdapter.java
│       │   └── TaxMasterAdapter.java
│       └── accounting/
│           └── SapAccountingAdapter.java
```

#### Đoạn mã minh chứng Phá vỡ Chu trình Phụ thuộc qua Interface:

```java
// ==================== PACKAGE: com.nextgen.domain.servicecontracts ====================
// Package này hoàn toàn độc lập, đứng ở vị trí thượng nguồn
package com.nextgen.domain.servicecontracts;
import com.nextgen.domain.model.Sale;

public interface ITaxCalculator {
    double calculateTax(Sale sale);
}

// ==================== PACKAGE: com.nextgen.services.tax ====================
// Tầng Hạ tầng phụ thuộc VÀO Interface của Tầng Miền (Không có chiều ngược lại!)
package com.nextgen.services.tax;

import com.nextgen.domain.model.Sale;
import com.nextgen.domain.servicecontracts.ITaxCalculator;

public class GoodAsGoldTaxAdapter implements ITaxCalculator {
    @Override
    public double calculateTax(Sale sale) {
        // Thực thi kết nối SDK thuế
        return sale.getTotalAmount() * 0.1;
    }
}
```

#### Bảng phân tích kỹ thuật và ý nghĩa thiết kế:

| Thành phần cấu trúc Package | Nguyên lý Thiết kế Gói áp dụng | Lợi ích Kiến trúc & Tối ưu hóa Build |
| :--- | :--- | :--- |
| `com.nextgen.domain.servicecontracts` | **Acyclic Dependencies Principle (ADP)** | Đóng gói toàn bộ các Interface trừu tượng ra khỏi lớp thực thi, đảm bảo đồ hình phụ thuộc là một cây DAG xuôi chiều, không có chu trình. |
| `com.nextgen.domain.model` | **Common Closure Principle (CCP)** | Gom toàn bộ logic bán hàng cốt lõi (`Sale`, `Register`, `SLI`) vào chung một package vì khi quy tắc đơn hàng đổi, chúng luôn thay đổi cùng nhau. |
| `com.nextgen.services.tax` | **Common Reuse Principle (CRP)** | Tách riêng module thuế ra thành một package độc lập. Nếu siêu thị không cần tính thuế, có thể tháo gỡ package này mà không ảnh hưởng core domain. |

---

### 4. SƠ ĐỒ TRỰC QUAN HÓA (UML MERMAID)

Sơ đồ Gói UML (Package Diagram) hoàn chỉnh cho Hệ thống NextGen POS, chứng minh sự tuân thủ nghiêm ngặt Nguyên lý Phi chu trình (ADP):

```mermaid
graph TD
    subgraph P_UI ["com.nextgen.ui"]
        UI_Classes["ProcessSaleView, DesktopGUI"]
    end

    subgraph P_DOMAIN ["com.nextgen.domain.model"]
        Register["Register (Facade)"]
        DomainEntities["Sale, SalesLineItem, ProductCatalog"]
    end

    subgraph P_CONTRACTS ["com.nextgen.domain.contracts"]
        TaxInterface["ITaxCalculatorAdapter"]
        AccountingInterface["IAccountingAdapter"]
    end

    subgraph P_TAX_SERVICES ["com.nextgen.services.tax"]
        GoodAsGold["GoodAsGoldTaxAdapter"]
        TaxMaster["TaxMasterAdapter"]
    end

    subgraph P_ACCOUNTING ["com.nextgen.services.accounting"]
        SapAdapter["SapAccountingAdapter"]
    end

    P_UI -.->|depends on| P_DOMAIN
    P_DOMAIN -.->|depends on| P_CONTRACTS
    
    P_TAX_SERVICES -.->|implements| P_CONTRACTS
    P_ACCOUNTING -.->|implements| P_CONTRACTS

    note for P_CONTRACTS "ĐỒ THỊ PHI CHU TRÌNH (DAG):<br/>Không có bất kỳ mũi tên nào quay ngược lại!<br/>Tuân thủ 100% nguyên lý ADP."

    style P_UI fill:#e1f5fe,stroke:#0288d1
    style P_DOMAIN fill:#e8f5e9,stroke:#2e7d32
    style P_CONTRACTS fill:#fffde7,stroke:#fbc02d,stroke-width:2px
    style P_TAX_SERVICES fill:#fff3e0,stroke:#f57c00
    style P_ACCOUNTING fill:#fff3e0,stroke:#f57c00
```

---

### 5. ÁNH XẠ ỨNG DỤNG VÀO DỰ ÁN GAME OOP THỰC CHIẾN

* **Bối cảnh áp dụng:** Thiết kế Cấu trúc Module và Assembly Definition Files (`.asmdef` trong Unity hoặc Project Dependencies trong C++/C#) cho Game 2.5D Multiplayer.
* **Thảm họa Biên dịch trong Game Studio lớn:**
  Dự án game có 2.000 file script code, tất cả bị nhét chung vào một thư mục gốc `Assets/Scripts`.
  $\to$ **Cơn ác mộng Biên dịch 10 phút (*Compilation Hell*):**
  * Mỗi khi một Game Designer mở 1 file sửa đúng 1 con số sát thương vũ khí, Unity phải biên dịch lại toàn bộ 2.000 file script. Toàn bộ 50 lập trình viên trong công ty phải ngồi chờ 10 phút mỗi lần nhấn lưu file!
  * Lập trình viên UI lén lút gọi thẳng vào biến con trỏ của hệ thống AI quái vật, tạo ra mớ bòng bong không thể kiểm soát.
* **Ứng dụng Phân chia Gói (Chương 31) vào Game:**
  1. *Chia tách thành các Assembly độc lập qua file `.asmdef`:*
     * `Game.Core.asmdef` (Toán học, Vector2D, Event Bus): Độc lập 100%, không phụ thuộc ai.
     * `Game.Simulation.asmdef` (Combat Logic, Inventory): Chỉ phụ thuộc `Game.Core`.
     * `Game.Network.asmdef` (Socket, Packets): Phụ thuộc `Game.Core`.
     * `Game.Presentation.asmdef` (UI, Sprites, VFX): Phụ thuộc vào `Game.Simulation` và `Game.Core`.
  2. *Triệt tiêu Chu trình Phụ thuộc:* Cấm tuyệt đối `Game.Simulation` tham chiếu tới `Game.Presentation`.
  3. *Kết quả:* Khi sửa code UI, Unity **chỉ biên dịch đúng assembly `Game.Presentation` mất đúng 2 giây**! Thời gian làm việc của đội ngũ kỹ thuật tăng tốc gấp 10 lần.

---

[🏠 Trang Chủ (MOC)](../README.md) | [◄ Chương 30: Designing the Logical Architecture with Patterns](./30_Designing_the_Logical_Architecture_with_Patterns.md) | [Chương 32: Introduction to Architectural Analysis and the SAD ►](./32_Introduction_to_Architectural_Analysis_and_the_SAD.md)
