# NextGen POS - Code Demo

Thư mục chứa mã nguồn thực nghiệm (C# / Java / TypeScript / Python) minh họa kiến trúc NextGen POS theo sách của Craig Larman.

### Cấu trúc gợi ý:
- `Domain/`: Các lớp nghiệp vụ cốt lõi (`Register`, `Sale`, `SalesLineItem`, `ProductDescription`, `Store`, `Payment`).
- `Services/`: Các Adapter (`CreditAuthorizationAdapter`, `TaxCalculatorAdapter`) và Factory (`ServicesFactory`).
- `Strategies/`: Các mẫu giảm giá (`ISalePricingStrategy`, `PercentDiscountStrategy`, `CompositePricingStrategy`).
