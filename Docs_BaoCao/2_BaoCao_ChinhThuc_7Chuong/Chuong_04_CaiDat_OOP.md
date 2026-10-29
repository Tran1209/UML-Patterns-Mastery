# PHẦN IV: CÀI ĐẶT HƯỚNG ĐỐI TƯỢNG (OOP IMPLEMENTATION & NETCODE)

> **Môn học**: Lập trình Hướng đối tượng (OOP)  
> **Trường**: Đại học Xây dựng Hà Nội (HUCE) — Khoa Công nghệ Thông tin  
> **Nhóm sinh viên thực hiện**:  
> 1. **Nguyễn Hoài Nam** (Trưởng nhóm / Lead Architect) — *Phụ trách Core Architecture & FSM*  
> 2. **Trần Trung Kiên** — *Phụ trách Dedicated Server & Real-Time Netcode*  
> 3. **Phạm Quang Anh** — *Phụ trách Combat Engine, Physics & Collision*  
> 4. **Vũ Quang Huy** — *Phụ trách Client Rendering 60 FPS, HUD & Observer*  
> 5. **Bùi Minh Hoàng** — *Phụ trách Persistence, Data Mapper & QA/Benchmarking*  
> **Môi trường cài đặt**: Java SE Development Kit 17/21 (LTS), Pure Socket TCP API, Không sử dụng thư viện/Game Engine ngoài.

---

## 4.1. TỔNG QUAN KIẾN TRÚC MÃ NGUỒN VÀ PHÂN GÓI

### 4.1.1. Cấu trúc Gói Phân vùng Trách nhiệm (Package Architecture)

Toàn bộ mã nguồn Java của trò chơi được đặt tại thư mục [`Src_Game/`](file:///E:/UML-Patterns-Mastery/Src_Game) và được phân chia thành 5 gói độc lập, tương ứng chính xác với 5 phân vùng lãnh thổ và 5 nhánh Git của các thành viên trong nhóm:

```
Src_Game/
├── Core/           (Nam   - feature/core-loop)      : Vector2D, LivingEntity, Player, GameEngineLoop, IPlayerState
├── Combat/         (Anh   - feature/combat-system)  : Hitbox, IDamageStrategy, PhysicalDamageStrategy, SpatialGridAoI
├── Network/        (Kiên  - feature/network-server) : SnapshotInterpolator, Protocol Packets, Socket Handler
├── UI/             (Huy   - feature/client-ui)       : IEntityEventListener, HudObserver, CanvasRenderer
└── Persistence/    (Hoàng - feature/data-testing)    : MatchSummary, IMatchRepository, JsonFileMatchRepository
```

### 4.1.2. Môi trường Thực thi và Ràng buộc Kỹ thuật
* **Nền tảng**: Java 17/21 LTS — Khai thác các đặc tính hiện đại: `record`, `switch pattern matching`, và `Virtual Threads (Project Loom)` cho máy chủ socket đa luồng hiệu năng cao.
* **Nguyên tắc "Pure OOP"**: Nhóm tự tay cài đặt toàn bộ Game Loop, thuật toán va chạm hình học, bộ đệm nội suy LERP và tuần tự hóa nhị phân bằng thư viện chuẩn `java.net.*` và `java.io.*` — không dựa dẫm vào bất kỳ game engine thương mại nào (Unity, LibGDX, Unreal), nhằm chứng minh năng lực lập trình hướng đối tượng thuần túy.

---

## 4.2. CÀI ĐẶT MODULE CORE (VÒNG LẶP THẾ GIỚI & MÁY TRẠNG THÁI)

*(Thành viên phụ trách: **Nguyễn Hoài Nam** — Nhánh `feature/core-loop`)*

Module Core là trái tim của hệ thống, quản lý sự sống của các thực thể và nhịp đập thời gian của thế giới game.

### 4.2.1. Vector2D — Thiết kế Đối tượng Giá trị Bất biến (Value Object)
Theo nguyên lý Domain-Driven Design (Vaughn Vernon), tọa độ không gian $(x, y)$ là một đối tượng giá trị (Value Object) — không có định danh độc lập và phải luôn bất biến (Immutable):

```java
// File: Src_Game/Core/Vector2D.java
package core;

public final class Vector2D {
    private final double x;
    private final double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }
    // ... length(), normalize()
}
```
> **Điểm sáng kiến trúc**: Mỗi phép toán cộng, trừ, nhân vector đều trả về một `new Vector2D(...)` mới. Điều này loại bỏ hoàn toàn lỗi tranh chấp bộ nhớ (Race Condition) khi nhiều luồng cùng truy cập tọa độ nhân vật.

---

### 4.2.2. LivingEntity & Player — Áp dụng Kế thừa và Information Expert
Lớp trừu tượng [`LivingEntity`](file:///E:/UML-Patterns-Mastery/Src_Game/Core/LivingEntity.java) tự bảo vệ trạng thái của mình thông qua phương thức nghiệp vụ đóng gói:

```java
// File: Src_Game/Core/LivingEntity.java
public abstract class LivingEntity {
    protected final String id;
    protected Vector2D position;
    protected int currentHp;
    protected int maxHp;
    protected int defense;
    protected boolean isAlive;

    public int takeDamage(int incomingDamage) {
        if (!isAlive()) return 0;
        int effectiveDamage = Math.max(1, incomingDamage - (defense / 2));
        currentHp = Math.max(0, currentHp - effectiveDamage);
        if (currentHp == 0) {
            isAlive = false;
            onDeath(); // Template Method Pattern
        }
        return effectiveDamage;
    }
    protected abstract void onDeath();
}
```

Lớp con [`Player`](file:///E:/UML-Patterns-Mastery/Src_Game/Core/Player.java) kế thừa `LivingEntity` và tích hợp máy trạng thái thông qua mẫu thiết kế **State Pattern (GoF)**:
* `IdleState`: Đứng yên, vận tốc bằng 0, có thể tấn công và di chuyển.
* `MovingState`: Đang di chuyển, cập nhật tọa độ liên tục.
* `AttackingState`: Vung kiếm trong 300ms, tạm thời khóa di chuyển để tạo trọng lượng cho đòn đánh.
* `DeadState`: Khi $HP = 0$, khóa hoàn toàn mọi tương tác.

---

### 4.2.3. GameEngineLoop — Vòng lặp Fixed Timestep (Robert Nystrom)
Để đảm bảo tính toán vật lý luôn đồng nhất giữa máy cấu hình cao và máy cấu hình thấp, nhóm cài đặt Game Loop theo mô hình **Fixed Update** (Robert Nystrom, Ch. 9):

```java
// File: Src_Game/Core/GameEngineLoop.java
public abstract class GameEngineLoop implements Runnable {
    private final double timeStepSeconds = 1.0 / 60.0; // 60Hz logic

    @Override
    public void run() {
        long previousTime = System.nanoTime();
        double accumulator = 0.0;

        while (isRunning) {
            long currentTime = System.nanoTime();
            double elapsedTime = (currentTime - previousTime) / 1_000_000_000.0;
            previousTime = currentTime;
            accumulator += Math.min(elapsedTime, 0.25); // Chống spiral of death

            // Vòng lặp vật lý với chu kỳ cố định 16.6ms
            while (accumulator >= timeStepSeconds) {
                fixedUpdate(timeStepSeconds);
                accumulator -= timeStepSeconds;
            }

            // Tính hệ số nội suy alpha phục vụ vẽ mượt mà
            float alpha = (float) (accumulator / timeStepSeconds);
            render(alpha);
        }
    }
}
```

---

## 4.3. CÀI ĐẶT MODULE COMBAT & PHYSICS (VA CHẠM & CHIẾN LƯỢC SÁT THƯƠNG)

*(Thành viên phụ trách: **Phạm Quang Anh** — Nhánh `feature/combat-system`)*

### 4.3.1. Hitbox — Kiểm tra Va chạm Hộp bao AABB
Lớp [`Hitbox`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/Hitbox.java) tự kiểm tra va chạm với các hộp bao khác mà không cần đối tượng trung gian:

```java
// File: Src_Game/Combat/Hitbox.java
public class Hitbox {
    private double centerX, centerY;
    private final double halfWidth, halfHeight;

    public boolean intersects(Hitbox other) {
        if (this.getMaxX() < other.getMinX() || this.getMinX() > other.getMaxX()) return false;
        if (this.getMaxY() < other.getMinY() || this.getMinY() > other.getMaxY()) return false;
        return true;
    }
}
```

---

### 4.3.2. IDamageStrategy — Hiện thực hóa Strategy Pattern
Chiến lược tính sát thương vật lý được cô lập trong [`PhysicalDamageStrategy`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/PhysicalDamageStrategy.java), cho phép dễ dàng bổ sung các loại sát thương phép thuật hoặc sát thương chuẩn trong tương lai mà không sửa đổi lớp `LivingEntity`:

```java
// File: Src_Game/Combat/PhysicalDamageStrategy.java
public class PhysicalDamageStrategy implements IDamageStrategy {
    @Override
    public int calculateDamage(int basePower, LivingEntity attacker, LivingEntity victim) {
        int armor = victim.getDefense();
        return Math.max(1, (attacker.getAttackPower() + basePower) - armor);
    }
}
```

---

### 4.3.3. SpatialGridAoI — Thuật toán Phân vùng Không gian Tối ưu $O(N)$
Khi số lượng thực thể tăng cao, việc kiểm tra va chạm cặp đôi từng thực thể với nhau có độ phức tạp $O(N^2)$ — làm sụt giảm nghiêm trọng hiệu năng Server. Lớp [`SpatialGridAoI`](file:///E:/UML-Patterns-Mastery/Src_Game/Combat/SpatialGridAoI.java) chia bản đồ thành các ô lưới kích thước $128 \times 128$ pixels. Một thực thể chỉ kiểm tra va chạm với các thực thể nằm trong ô hiện tại và 8 ô lân cận:

```java
// File: Src_Game/Combat/SpatialGridAoI.java
public class SpatialGridAoI {
    private final int cellSize;
    private final Map<Long, List<LivingEntity>> gridBuckets = new HashMap<>();

    public List<LivingEntity> queryNearby(Vector2D centerPosition) {
        List<LivingEntity> nearby = new ArrayList<>();
        int centerCellX = (int) Math.floor(centerPosition.getX() / cellSize);
        int centerCellY = (int) Math.floor(centerPosition.getY() / cellSize);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                long key = hashCoordinate(centerCellX + dx, centerCellY + dy);
                List<LivingEntity> bucket = gridBuckets.get(key);
                if (bucket != null) nearby.addAll(bucket);
            }
        }
        return nearby; // Chỉ quét các thực thể trong lân cận -> O(N)
    }
}
```

---

## 4.4. CÀI ĐẶT MODULE NETWORK & DEDICATED SERVER (REAL-TIME NETCODE)

*(Thành viên phụ trách: **Trần Trung Kiên** — Nhánh `feature/network-server`)*

### 4.4.1. Kiến trúc Dedicated Authoritative Server
Máy chủ Socket TCP lắng nghe kết nối của các Client trên cổng 8080. Client chỉ có quyền gửi gói tin ý định di chuyển (`InputCommandPacket`). Server là nguồn sự thật duy nhất (Single Source of Truth), tự kiểm tra va chạm và phát sóng ảnh chụp thế giới (`WorldSnapshotPacket`) định kỳ ở chu kỳ 20Hz (50ms một lần).

---

### 4.4.2. SnapshotInterpolator — Giải thuật LERP Triệt tiêu Giật hình (Micro-stutter)
Đây là **thành tựu kỹ thuật xuất sắc nhất trong đồ án**. Nhóm giải quyết bài toán lệch pha giữa Server phát trạng thái 20 lần/giây và Màn hình Client vẽ 60 khung hình/giây. Lớp [`SnapshotInterpolator`](file:///E:/UML-Patterns-Mastery/Src_Game/Network/SnapshotInterpolator.java) lưu trữ một hàng đợi các ảnh chụp trong quá khứ và tính toán nội suy vị trí mượt mà theo công thức:

$$P_{render}(t) = P_{old} + \bigl(P_{new} - P_{old}\bigr) \times \alpha, \quad \alpha = \frac{t_{render} - t_{old}}{t_{new} - t_{old}}$$

```java
// File: Src_Game/Network/SnapshotInterpolator.java
public class SnapshotInterpolator {
    private final long interpolationDelayMs = 100; // Độ trễ bộ đệm 100ms (2 ticks 20Hz)

    public Vector2D interpolate(String entityId, long currentClientTime) {
        Deque<EntityTransform> buffer = historyBuffers.get(entityId);
        long renderTimestamp = currentClientTime - interpolationDelayMs;

        EntityTransform pOld = null, pNew = null;
        for (EntityTransform t : buffer) {
            if (t.timestamp <= renderTimestamp) pOld = t;
            else { pNew = t; break; }
        }

        if (pOld == null || pNew == null) return buffer.peekLast().position;

        double progress = renderTimestamp - pOld.timestamp;
        double duration = pNew.timestamp - pOld.timestamp;
        float alpha = (float) Math.max(0.0, Math.min(1.0, progress / duration));

        return lerp(pOld.position, pNew.position, alpha);
    }
}
```

---

## 4.5. CÀI ĐẶT MODULE UI & PERSISTENCE (QUAN SÁT VIÊN & LƯU TRỮ DỮ LIỆU)

*(Thành viên phụ trách: **Vũ Quang Huy** — Nhánh `feature/client-ui` & **Bùi Minh Hoàng** — Nhánh `feature/data-testing`)*

### 4.5.1. HudObserver — Áp dụng Mẫu Observer Pattern
Lớp [`HudObserver`](file:///E:/UML-Patterns-Mastery/Src_Game/UI/HudObserver.java) hiện thực giao diện [`IEntityEventListener`](file:///E:/UML-Patterns-Mastery/Src_Game/UI/IEntityEventListener.java), giúp giao diện thanh máu và thông báo trận đấu lắng nghe các sự kiện tử vong và sát thương mà không phụ thuộc vào lớp `LivingEntity`.

---

### 4.5.2. JsonFileMatchRepository — Hiện thực hóa Data Mapper Pattern (Martin Fowler)
Lớp [`JsonFileMatchRepository`](file:///E:/UML-Patterns-Mastery/Src_Game/Persistence/JsonFileMatchRepository.java) hiện thực interface [`IMatchRepository`](file:///E:/UML-Patterns-Mastery/Src_Game/Persistence/IMatchRepository.java), tuần tự hóa đối tượng `MatchSummary` thành tệp văn bản JSON tiêu chuẩn khi trận đấu kết thúc:

```java
// File: Src_Game/Persistence/JsonFileMatchRepository.java
public class JsonFileMatchRepository implements IMatchRepository {
    @Override
    public synchronized boolean saveMatch(MatchSummary summary) {
        // Tuần tự hóa JSON thuần túy, an toàn và độc lập với thư viện ngoài
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append(String.format("  \"matchId\": \"%s\",\n", summary.getMatchId()));
        jsonBuilder.append(String.format("  \"winnerId\": \"%s\",\n", summary.getWinnerId()));
        jsonBuilder.append(String.format("  \"durationSeconds\": %d\n", summary.getDurationSeconds()));
        jsonBuilder.append("}\n");

        try (FileWriter writer = new FileWriter(new File(filePath), true)) {
            writer.write(jsonBuilder.toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
```

---

## 4.6. ĐÁNH GIÁ CHẤT LƯỢNG MÃ NGUỒN THEO NGUYÊN LÝ SOLID & GRASP

| Nguyên lý | Biểu hiện cụ thể trong mã nguồn Java của nhóm | Đánh giá đạt chuẩn |
|:---|:---|:---:|
| **Single Responsibility (SRP)** | `SnapshotInterpolator` chỉ làm việc LERP, `SpatialGridAoI` chỉ lo phân vùng lưới, `HudObserver` chỉ lo vẽ HUD. | ⭐⭐⭐⭐⭐ |
| **Open-Closed (OCP)** | Thêm vũ khí mới bằng cách tạo class mới cài đặt `IDamageStrategy`, không sửa code `LivingEntity`. | ⭐⭐⭐⭐⭐ |
| **Liskov Substitution (LSP)** | Mọi lớp con kế thừa `LivingEntity` (`Player`) đều có thể thay thế hoàn hảo cho lớp cha mà không làm sập chương trình. | ⭐⭐⭐⭐⭐ |
| **Interface Segregation (ISP)** | Tách nhỏ các giao diện: `IPlayerState`, `IDamageStrategy`, `IEntityEventListener`, `IMatchRepository`. | ⭐⭐⭐⭐⭐ |
| **Dependency Inversion (DIP)** | Các tầng cao phụ thuộc vào Interface trừu tượng (`IMatchRepository`) thay vì lớp cụ thể (`JsonFileMatchRepository`). | ⭐⭐⭐⭐⭐ |

---

*Tài liệu tham khảo chuyên sâu cho chương này:*
- *[1] Joshua Glazer & Sanjay Madhav — Multiplayer Game Programming (2015), Chương 7: Latency, Jitter, and Reliability.*
- *[2] Robert Nystrom — Game Programming Patterns (2014), Chương 9 (Game Loop) & Chương 19 (Spatial Partition).*
- *[3] Martin Fowler — Patterns of Enterprise Application Architecture (2002), Chương 10 (Data Mapper).*
