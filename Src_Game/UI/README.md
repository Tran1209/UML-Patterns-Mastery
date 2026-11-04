# 🎨 MODULE GIAO DIỆN & KẾT XUẤT ĐỒ HỌA 2.5D (CLIENT RENDERING, CAMERA2D & HUD)
> **Thành viên phụ trách chính**: **Đức Anh (`Alonee-code`)** — *Kỹ sư Đồ họa & Giao diện (Graphics & UI Engineer)*  
> **Nhánh làm việc độc quyền**: `Anh`  
> **Giáo trình tham chiếu**: Robert Nystrom (*Game Programming Patterns*) — *Double Buffer, Observer Pattern & 2.5D Dimetric Rendering*

---

## 🎯 PHẠM VI TRÁCH NHIỆM TRÊN NHÁNH `Anh`:
1. **Dựng Hình Môi Trường 2.5D & Z-Sorting**:
   - `GameCanvas.java`: Pipeline vẽ chiều sâu Z-Order theo tọa độ chân thực thể (Painter's Algorithm), dòng sông uốn lượn, thảm cỏ, thềm đá và trụ đá phong ấn.
2. **Camera2D Khóa Tâm & Nội Suy Mượt**:
   - `Camera2D.java`: Thuật toán bám đuổi theo nhân vật chính với hệ số chuyển động mượt $0.12$, triệt tiêu hiện tượng rung lắc màn hình.
3. **Thanh Máu, Radar & Giao Diện HUD Chuẩn MOBA**:
   - `DesktopHudRenderer.java`: Vòng ngắm chiêu thức Elip 2.5D bám sát mặt đất, thanh máu phân đoạn (Segmented HP Bar), bảng điểm thời gian thực.
4. **Mô Hình Observer Bắt Sự Kiện Giao Diện**:
   - `HudObserver.java`, `IEntityEventListener.java`: Nhận thông báo từ tầng Logic để cập nhật HUD mà không làm phụ thuộc mã nguồn.
