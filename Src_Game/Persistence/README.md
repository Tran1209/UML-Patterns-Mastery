# 💾 MODULE LƯU TRỮ DỮ LIỆU & KIỂM THỬ (PERSISTENCE & AUTOMATED QA)
> **Thành viên phụ trách chính**: **Huy Hoàng (`HHoang2903`)** — *Kỹ sư Lưu trữ Dữ liệu & Đảm bảo Chất lượng (QA & Persistence Engineer)*  
> **Nhánh làm việc độc quyền**: `Hoang`  
> **Giáo trình tham chiếu**: Martin Fowler (*Patterns of Enterprise Application Architecture*) & Kent Beck (*TDD By Example*)

---

## 🎯 PHẠM VI TRÁCH NHIỆM TRÊN NHÁNH `Hoang`:
1. **Lớp Trung Gian Lưu Trữ (Repository Pattern & Data Mapper)**:
   - `IMatchRepository.java`, `JsonFileMatchRepository.java`: Lưu vết tóm tắt kết quả trận đấu (`MatchSummary`) xuống tệp tin JSON an toàn.
   - Giữ cho toàn bộ Domain Logic của game độc lập hoàn toàn khỏi tầng lưu trữ.
2. **Bộ Kiểm Thử Tự Động 15/15 Unit Tests**:
   - `GameUnitTestSuite.java`: Kiểm thử độc lập toàn diện các hệ thống FSM, Strategy Sát thương, Spatial Grid, và Thuật toán LERP chống giật hình.
3. **Đo Đạc Hiệu Năng & Jitter Benchmark**:
   - Đo lường độ trễ mạng $\pm 25$ms, kiểm thử tải đa luồng phòng đấu và độ ổn định 60 FPS.
