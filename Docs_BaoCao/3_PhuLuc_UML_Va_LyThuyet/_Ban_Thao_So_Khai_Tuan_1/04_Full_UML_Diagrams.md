# 04. Full UML Diagrams (Mermaid)

> Phân biệt đúng 3 lớp sơ đồ theo Larman: Domain Model · SSD · DCD (+ Interaction).

## Liên kết
- [[01_UseCase_and_DomainModel]]
- [[02_GRASP_Principles_Analysis]]
- [[03_GoF_and_StateMachine_Design]]

---

## 1. Domain Model (Ch.9) — không method

Xem chi tiết tại [[01_UseCase_and_DomainModel#2. Domain Model]].

---

## 2. System Sequence Diagram — SSD (Ch.10)

Actor ↔ System (black box). Nhận diện system operations.

```mermaid
sequenceDiagram
    actor Player
    participant System as :System

    Player ->> System: startSession()
    Player ->> System: startCombat(enemyId)
    Player ->> System: attack(targetId)
    System -->> Player: combatResult
    Player ->> System: useItem(itemId)
    Player ->> System: endCombat()
```

---

## 3. Interaction / Design Sequence (Ch.15)

Giải thích *vì sao* object A gọi B (RDD + GRASP).

```mermaid
sequenceDiagram
    participant UI
    participant CC as CombatController
    participant Enc as CombatEncounter
    participant P as Player
    participant E as Enemy

    UI ->> CC: attack(targetId)
    Note over CC: GRASP Controller
    CC ->> Enc: resolveAttack(attacker, targetId)
    Enc ->> P: getAttackPower()
    Note over P: Information Expert
    Enc ->> E: applyDamage(amount)
    E -->> Enc: remainingHp
    Enc -->> CC: AttackResult
    CC -->> UI: AttackResult
```

---

## 4. Design Class Diagram — DCD (Ch.16)

Có types, method signatures, navigability, relationships.

```mermaid
classDiagram
    class CombatController {
        -encounter: CombatEncounter
        +attack(targetId: string): AttackResult
        +useItem(itemId: string): void
    }
    class CombatEncounter {
        -players: List~Player~
        -enemies: List~Enemy~
        +resolveAttack(attacker: Player, targetId: string): AttackResult
    }
    class Player {
        -hp: int
        -attackPower: int
        +getAttackPower(): int
        +isAlive(): bool
    }
    class Enemy {
        -hp: int
        +applyDamage(amount: int): int
        +isAlive(): bool
    }
    class IEnemyBehavior {
        <<interface>>
        +decideAction(self: Enemy, ctx: CombatEncounter): void
    }
    class EnemyFactory {
        +create(specId: string): Enemy
    }

    CombatController --> CombatEncounter : coordinates
    CombatEncounter o-- Player
    CombatEncounter o-- Enemy
    Enemy ..|> IEnemyBehavior : uses
    EnemyFactory ..> Enemy : creates
```

---

## 5. Checklist báo cáo

- [ ] Domain Model không lẫn method / class kỹ thuật
- [ ] SSD chỉ Actor + `:System`
- [ ] Sequence design chỉ rõ GRASP trên từng message quan trọng
- [ ] DCD khớp 1:1 với code trong `Src_Game/`
