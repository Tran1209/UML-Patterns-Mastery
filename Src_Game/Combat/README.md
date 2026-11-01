# ⚔️ MODULE CHIẾN ĐẤU & VA CHẠM (COMBAT ENGINE & PHYSICS)
> **Thành viên phụ trách chính**: **Trung Kiên (`KienEsto`)** — *Nhóm trưởng (Team Leader)*  
> **Nhánh làm việc độc quyền**: `Kien`  
> **Giáo trình tham chiếu**: Robert Nystrom (*Game Programming Patterns*) — *Spatial Partition, Strategy & State Pattern*

---

## 🎯 PHẠM VI TRÁCH NHIỆM TRÊN NHÁNH `Kien`:
1. **Lưới Phân Vùng Không Gian (Spatial Grid AoI)**:
   - Thuật toán `SpatialGridAoI.java` chia map $1600 \times 1200$ thành các ô $128 \times 128$.
   - Hạ độ phức tạp va chạm từ $O(N^2)$ xuống $O(N)$.
2. **Chiến Lược Tính Sát Thương Đa Hình (Strategy Pattern)**:
   - `IDamageStrategy.java`, `PhysicalDamageStrategy.java` tính toán giáp và sát thương bạo kích.
3. **Bộ Kỹ Năng Đấu Sĩ Chuẩn MOBA (Open/Closed Principle)**:
   - `ISkillAction.java`: Đóng gói 3 kỹ năng (`DashStrikeSkill`, `FireWhirlwindSkill`, `MeteorUltimateSkill`).
4. **Hệ Thống Hiệu Ứng Chiến Đấu & Hitbox**:
   - `Hitbox.java` (AABB Check), `CombatEffects.java` (Số sát thương, vệt chém kiếm, và hạt va chạm).
