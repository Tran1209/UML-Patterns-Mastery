# UML & Patterns Mastery

Dự án OOAD theo **"Applying UML and Patterns"** (Craig Larman): kiến thức sách → Đồ án Game OOP → Báo cáo UML/GRASP/GoF.

---

## Cấu trúc thư mục

```text
UML-Patterns-Mastery/
├── .cursor/rules/larman-ooad.mdc
├── _Knowledge_Base_Larman/          # gitignored — sách & chương đã tách
│   ├── raw_pdf/applying-uml-and-patterns.pdf
│   └── chapters_extracted/Chap_XX_.../
├── Docs_BaoCao/                     # Báo cáo học thuật (Mermaid UML)
├── Src_Game/                        # Source Đồ án Game OOP
└── scripts/                         # gitignored — công cụ tách sách
    └── extract_larman_book.py
```

Thư mục cũ (`00_Raw_PDF/`, `01_Report_Notes/`, `02_Code_Demo/`) có thể giữ tạm; luồng chính dùng cấu trúc trên.

---

## Tách sách Larman (local)

1. Đặt PDF tại:
   `_Knowledge_Base_Larman/raw_pdf/applying-uml-and-patterns.pdf`
2. Cài thư viện:
   ```bash
   pip install pymupdf
   ```
3. Chạy script:
   ```bash
   python scripts/extract_larman_book.py
   ```

Mỗi chương xuất vào `_Knowledge_Base_Larman/chapters_extracted/Chap_XX_TenChuong/` với `content.md` và `images/`.

`_Knowledge_Base_Larman/` và `scripts/` nằm trong `.gitignore` để không push nhầm sách nặng lên GitHub đồ án.

---

## Báo cáo & Code

| Thư mục | Nội dung |
|---------|----------|
| `Docs_BaoCao/` | Use Case/Domain, GRASP, GoF/State, Full UML |
| `Src_Game/` | Implementation Game OOP |

Mở vault bằng Obsidian tại root project để dùng liên kết `[[...]]`.
