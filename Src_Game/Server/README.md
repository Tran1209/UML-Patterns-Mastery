# 🌐 MODULE MÁY CHỦ & ĐỒNG BỘ MẠNG THỜI GIAN THỰC (DEDICATED SERVER & NETCODE)
> **Thành viên phụ trách chính**: **Nguyễn Đức Huy (`nguyenduchuy20070406-oss`)** — *Kỹ sư Mạng & Máy chủ (Network Engineer)*  
> **Nhánh làm việc độc quyền**: `Huy`  
> **Giáo trình tham chiếu**: Joshua Glazer & Sanjay Madhav (*Multiplayer Game Programming*) — *Dedicated Authoritative Server & Snapshot Interpolation (LERP)*

---

## 🎯 PHẠM VI TRÁCH NHIỆM TRÊN NHÁNH `Huy`:
1. **Kiến Trúc Dedicated Authoritative Server**:
   - `DedicatedGameServer.java`: Quản lý đa phòng đấu, vòng lặp Server Tick chuẩn 20Hz (50ms/chu kỳ), phán quyết va chạm và trừ máu quái/người chơi.
2. **Giao Thức Truyền Dẫn & Tuần Tự Hóa Gói Tin (Object Streams)**:
   - Đóng gói dữ liệu nhị phân qua TCP Socket, tối ưu kích thước gói tin dưới 100 bytes để giảm thiểu trễ mạng.
3. **Bộ Đệm Nội Suy Chống Giật Lag (Client-Side Interpolation - LERP)**:
   - `SnapshotInterpolator.java`: Triệt tiêu hoàn toàn hiện tượng nhảy cóc (jitter) khi lệch pha giữa nhịp Server 20Hz và màn hình Client 60 FPS.
4. **Hạ Tầng Kết Nối Hybrid LAN & Cloud Relay**:
   - `MiniMqttClient.java`, `CloudServerBridge.java`, `NetworkGameClient.java` phục vụ chơi mạng qua Internet 4G/Wifi.
