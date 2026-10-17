# 02. Phân tích 9 nguyên lý GRASP

> Mỗi quyết định gán trách nhiệm trong `Src_Game/` phải biện luận theo khung 4 ý Larman.

## Liên kết
- [[01_UseCase_and_DomainModel]]
- [[03_GoF_and_StateMachine_Design]]
- [[04_Full_UML_Diagrams]]

---

## Khung phân tích (bắt buộc)

1. **Vấn đề** — Spaghetti/Procedural mở rộng thế nào sẽ gãy?
2. **Giải pháp** — GRASP nào?
3. **Ánh xạ** — POS (`Sale`/`Register`) → class Game nào?
4. **Trade-offs** — lợi ích / chi phí

---

## Bảng GRASP → Game

| GRASP | Vai trò trong Game | Ví dụ class |
|-------|--------------------|-------------|
| Information Expert | Ai có data thì tính toán | `Player` tính `isAlive`; `Inventory` biết còn chỗ |
| Creator | Ai tạo đối tượng | `CombatEncounter` tạo `DamageEvent`; `EnemyFactory` tạo `Enemy` |
| Controller | Nhận system event từ UI | `GameController` / `CombatController` |
| Low Coupling | Giảm phụ thuộc cứng | Interface `IDamageable` thay vì phụ thuộc concrete |
| High Cohesion | Một class một mối quan tâm | Tách `CombatSystem` khỏi `Inventory` |
| Polymorphism | Biến thể hành vi | `IEnemyBehavior` thay `switch(enemyType)` |
| Pure Fabrication | Class kỹ thuật không có trong Domain | `SaveService`, `AudioService` |
| Indirection | Trung gian giảm coupling | `EventBus` / Mediator giữa UI và Model |
| Protected Variations | Bao điểm dễ đổi | `IInputAdapter` cho keyboard/gamepad |

---

## Ví dụ mẫu (điền khi code sẵn)

### Controller nhận `attack()`

- **Vấn đề**: UI gọi trực tiếp mọi entity → đổi UI phải sửa combat.
- **Giải pháp**: GRASP Controller + High Cohesion.
- **Ánh xạ**: `Register` → `CombatController`.
- **Trade-offs**: Thêm lớp điều phối; UI và domain tách sạch hơn.
