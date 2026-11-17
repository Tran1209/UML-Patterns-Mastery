// ----------------------------------------------------------------------------
// SolidAndDesignPatterns25D.cs
// Bộ khung Kiến trúc SOLID & Game Programming Patterns (chắt lọc từ 177 scripts
// của Unity-Technologies/game-programming-patterns-demo, chuyển đổi sang Godot 4 C# 2.5D):
//   1. [ISP - Interface Segregation]: IDamageable, IExplodable, ISwitchable
//   2. [DIP - Dependency Inversion]: HealingTotemSwitch3D & ExplosiveBarrel3D
//   3. [Command Pattern - 2.5D Adaptation]: InputBufferQueue (Đệm phím combo 180ms)
//   4. [State Pattern]: IHeroState & HeroStateMachine (Quản lý trạng thái Tướng)
//   5. [Object Pool Pattern]: GodotNodePool<T> & PooledFloatingText3D (Số sát thương nảy)
//   6. [Observer Pattern]: ArenaEventBus (Kênh sự kiện C# tách rời UI và Combat)
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Patterns
{
    using System;
    using System.Collections.Generic;
    using Godot;
    using SurvivalHeroes.Gameplay;

    // ========================================================================
    // 1. INTERFACE SEGREGATION PRINCIPLE (_SOLID/InterfaceSegregation)
    // ========================================================================

    /// <summary>Mọi thực thể có thể nhận sát thương trên mặt phẳng 2.5D (Tướng, Thùng thuốc nổ, Quái).</summary>
    public interface IDamageable
    {
        Vector3 WorldPosition25D { get; }
        bool IsDestroyed { get; }
        void TakeDamage(float amount, int attackerViewId);
    }

    /// <summary>Thực thể có khả năng phát nổ dây chuyền khi bị phá hủy.</summary>
    public interface IExplodable
    {
        float ExplosionRadius { get; }
        float ExplosionDamage { get; }
        void TriggerExplosion(int instigatorViewId);
    }

    /// <summary>DEPENDENCY INVERSION (_SOLID/DependencyInversion): Công tắc/Trụ kích hoạt trừu tượng.</summary>
    public interface ISwitchable
    {
        bool IsActive { get; }
        void Activate(HeroCharacter activator);
        void Deactivate();
    }

    // ========================================================================
    // 2. OBSERVER PATTERN (_DesignPatterns/Observer - C# Static Event Bus)
    // ========================================================================

    public static class ArenaEventBus
    {
        public static event Action<Vector3, string, Color> OnFloatingTextRequested;
        public static event Action<string, string> OnComboBuffered;
        public static event Action<string> OnArenaAnnouncement;

        public static void RaiseFloatingText(Vector3 worldPos, string text, Color color)
            => OnFloatingTextRequested?.Invoke(worldPos, text, color);

        public static void RaiseComboBuffered(string heroName, string skillKey)
            => OnComboBuffered?.Invoke(heroName, skillKey);

        public static void RaiseAnnouncement(string message)
            => OnArenaAnnouncement?.Invoke(message);
    }

    // ========================================================================
    // 3. COMMAND PATTERN (_DesignPatterns/Command -> 2.5D Input Action Buffer)
    // ========================================================================

    public interface ICombatCommand
    {
        string CommandName { get; }
        bool CanExecute();
        void Execute();
    }

    public sealed class DelegateCombatCommand : ICombatCommand
    {
        public string CommandName { get; }
        private readonly Func<bool> _canExecute;
        private readonly Action _execute;

        public DelegateCombatCommand(string commandName, Func<bool> canExecute, Action execute)
        {
            CommandName = commandName;
            _canExecute = canExecute;
            _execute = execute;
        }

        public bool CanExecute() => _canExecute != null && _canExecute();
        public void Execute() => _execute?.Invoke();
    }

    /// <summary>
    /// Hàng đợi đệm lệnh 2.5D (Input Buffer Queue):
    /// Giữ thao tác bấm phím kỹ năng (Q/W/E/LMB) trong cửa sổ 180ms để người chơi
    /// bấm trước khi hết hồi chiêu hoặc đang lướt (Dash) vẫn tự động tung chiêu mượt mà.
    /// </summary>
    public sealed class InputBufferQueue
    {
        private struct BufferedEntry
        {
            public ICombatCommand Command;
            public double ExpireTimeSec;
        }

        private readonly Queue<BufferedEntry> _queue = new();
        private readonly double _bufferWindowSec;

        public int BufferedCount => _queue.Count;
        public string LastBufferedSkillName { get; private set; } = "Không";

        public InputBufferQueue(double bufferWindowSec = 0.18)
        {
            _bufferWindowSec = bufferWindowSec;
        }

        public void EnqueueCommand(ICombatCommand command)
        {
            if (command == null) return;
            double now = Time.GetTicksMsec() / 1000.0;
            _queue.Enqueue(new BufferedEntry
            {
                Command = command,
                ExpireTimeSec = now + _bufferWindowSec
            });
            LastBufferedSkillName = command.CommandName;
        }

        /// <summary>Duyệt và thực thi lệnh hợp lệ đầu tiên trong hàng đợi mỗi Physics Frame.</summary>
        public bool ProcessBufferedCommands()
        {
            double now = Time.GetTicksMsec() / 1000.0;

            while (_queue.Count > 0)
            {
                BufferedEntry peek = _queue.Peek();
                if (now > peek.ExpireTimeSec)
                {
                    _queue.Dequeue();
                    continue;
                }

                if (peek.Command.CanExecute())
                {
                    _queue.Dequeue();
                    peek.Command.Execute();
                    return true;
                }

                // Lệnh đầu hàng đợi chưa sẵn sàng (ví dụ còn 0.05s hồi chiêu), giữ lại chờ frame kế tiếp
                break;
            }
            return false;
        }

        public void Clear() => _queue.Clear();
    }

    // ========================================================================
    // 4. STATE PATTERN (_DesignPatterns/State - Pure C# Hero State Machine)
    // ========================================================================

    public interface IHeroState
    {
        string StateDisplayName { get; }
        void Enter();
        void Tick(float delta);
        void Exit();
    }

    public sealed class HeroStateMachine
    {
        public IHeroState CurrentState { get; private set; }
        public event Action<IHeroState> OnStateChanged;

        public void Initialize(IHeroState startingState)
        {
            CurrentState = startingState;
            CurrentState?.Enter();
            OnStateChanged?.Invoke(CurrentState);
        }

        public void TransitionTo(IHeroState nextState)
        {
            if (nextState == null || ReferenceEquals(CurrentState, nextState)) return;
            CurrentState?.Exit();
            CurrentState = nextState;
            CurrentState.Enter();
            OnStateChanged?.Invoke(CurrentState);
        }

        public void Tick(float delta) => CurrentState?.Tick(delta);
    }

    public sealed class HeroSimpleState : IHeroState
    {
        public string StateDisplayName { get; }
        public HeroSimpleState(string displayName) => StateDisplayName = displayName;
        public void Enter() { }
        public void Tick(float delta) { }
        public void Exit() { }
    }

    // ========================================================================
    // 5. OBJECT POOL PATTERN (_DesignPatterns/ObjectPool -> GodotNodePool<T>)
    // ========================================================================

    /// <summary>Bộ quản lý Object Pool tổng quát cho Node3D trong Godot (tương đương UnityEngine.Pool.IObjectPool).</summary>
    public sealed class GodotNodePool<T> where T : Node3D
    {
        private readonly Stack<T> _inactiveStack = new();
        private readonly Func<T> _createFunc;
        private readonly Action<T> _onGet;
        private readonly Action<T> _onRelease;

        public int CountInactive => _inactiveStack.Count;
        public int TotalCreated { get; private set; }

        public GodotNodePool(Func<T> createFunc, Action<T> onGet = null, Action<T> onRelease = null, int prewarmCount = 0)
        {
            _createFunc = createFunc ?? throw new ArgumentNullException(nameof(createFunc));
            _onGet = onGet;
            _onRelease = onRelease;

            for (int i = 0; i < prewarmCount; i++)
            {
                T item = _createFunc();
                TotalCreated++;
                _onRelease?.Invoke(item);
                _inactiveStack.Push(item);
            }
        }

        public T Get()
        {
            T item = _inactiveStack.Count > 0 ? _inactiveStack.Pop() : CreateNew();
            _onGet?.Invoke(item);
            return item;
        }

        public void Release(T item)
        {
            if (item == null) return;
            _onRelease?.Invoke(item);
            _inactiveStack.Push(item);
        }

        private T CreateNew()
        {
            TotalCreated++;
            return _createFunc();
        }
    }

    /// <summary>Chữ số sát thương/hồi máu bay lên (Floating Combat Text 3D) tái sử dụng qua Object Pool.</summary>
    public partial class PooledFloatingText3D : Node3D
    {
        private Label3D _label;
        private float _lifeTimer;
        private Action<PooledFloatingText3D> _returnToPoolCallback;
        private bool _isActive;

        public override void _Ready()
        {
            _label = new Label3D
            {
                PixelSize = 0.014f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                OutlineSize = 0,
                Modulate = Colors.Yellow
            };
            AddChild(_label);
            Visible = false;
        }

        public void SpawnPopup(Vector3 worldPos, string text, Color color, Action<PooledFloatingText3D> returnCallback)
        {
            _returnToPoolCallback = returnCallback;
            GlobalPosition = worldPos + new Vector3((float)GD.RandRange(-0.4, 0.4), 2.3f, (float)GD.RandRange(-0.4, 0.4));
            if (_label != null)
            {
                _label.Text = text;
                _label.Modulate = color;
            }
            _lifeTimer = 0.85f;
            _isActive = true;
            Visible = true;
        }

        public override void _Process(double delta)
        {
            if (!_isActive) return;
            float dt = (float)delta;
            _lifeTimer -= dt;
            GlobalPosition += new Vector3(0f, 2.2f * dt, 0f);

            if (_lifeTimer <= 0f)
            {
                _isActive = false;
                Visible = false;
                _returnToPoolCallback?.Invoke(this);
            }
        }
    }

    // ========================================================================
    // 6. CONCRETE SOLID ENTITIES: ExplosiveBarrel3D & HealingShrineSwitch3D
    // ========================================================================

    /// <summary>
    /// Thùng Thuốc Nổ 2.5D: Minh họa trực tiếp Interface Segregation (IDamageable + IExplodable).
    /// Khi bị đạn hoặc kỹ năng đánh trúng -> Phát nổ gây sát thương diện rộng!
    /// </summary>
    public partial class ExplosiveBarrel3D : Node3D, IDamageable, IExplodable
    {
        public Vector3 WorldPosition25D => GlobalPosition;
        public bool IsDestroyed { get; private set; }
        public float ExplosionRadius => 5.0f;
        public float ExplosionDamage => 55f;

        private float _health = 40f;
        private float _respawnTimer;
        private MeshInstance3D _barrelMesh;
        private Label3D _label;

        public void Initialize(Vector3 worldPos)
        {
            GlobalPosition = worldPos;

            _barrelMesh = new MeshInstance3D();
            CylinderMesh cyl = new() { TopRadius = 0.65f, BottomRadius = 0.65f, Height = 1.4f };
            StandardMaterial3D mat = new()
            {
                AlbedoColor = new Color(0.92f, 0.22f, 0.18f),
                EmissionEnabled = true,
                Emission = new Color(0.6f, 0.1f, 0.05f),
                EmissionEnergyMultiplier = 0.5f
            };
            _barrelMesh.Mesh = cyl;
            _barrelMesh.MaterialOverride = mat;
            _barrelMesh.Position = new Vector3(0f, 0.7f, 0f);
            AddChild(_barrelMesh);

            _label = new Label3D
            {
                Text = "🧨 Thùng Nổ",
                PixelSize = 0.0095f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                Modulate = new Color(1f, 0.55f, 0.4f),
                OutlineSize = 0,
                Position = new Vector3(0f, 1.85f, 0f)
            };
            AddChild(_label);
        }

        public void TakeDamage(float amount, int attackerViewId)
        {
            if (IsDestroyed) return;
            _health -= amount;
            ArenaEventBus.RaiseFloatingText(GlobalPosition, $"-{Mathf.RoundToInt(amount)}", Colors.OrangeRed);

            if (_health <= 0f)
            {
                TriggerExplosion(attackerViewId);
            }
        }

        public void TriggerExplosion(int instigatorViewId)
        {
            if (IsDestroyed) return;
            IsDestroyed = true;
            _respawnTimer = 18f;
            if (_barrelMesh != null) _barrelMesh.Visible = false;
            if (_label != null) _label.Text = "💥 Đã phát nổ (Hồi sau 18s)";

            ArenaEventBus.RaiseFloatingText(GlobalPosition, "💥 BÙM! -55 AOE", new Color(1f, 0.35f, 0.1f));
            ArenaEventBus.RaiseAnnouncement("🧨 Thùng Thuốc Nổ [IExplodable] vừa kích nổ dây chuyền!");

            if (ArenaGameManager.Instance != null)
            {
                ArenaGameManager.Instance.SpawnAoeRingEffect(GlobalPosition, ExplosionRadius, new Color(1f, 0.35f, 0.1f), 0.45f);
                ArenaGameManager.Instance.ApplyAoeDamage(instigatorViewId, GlobalPosition, ExplosionRadius, ExplosionDamage);
            }
        }

        public override void _Process(double delta)
        {
            if (!IsDestroyed) return;
            _respawnTimer -= (float)delta;
            if (_respawnTimer <= 0f)
            {
                IsDestroyed = false;
                _health = 40f;
                if (_barrelMesh != null) _barrelMesh.Visible = true;
                if (_label != null) _label.Text = "🧨 Thùng Nổ [IDamageable]";
            }
        }
    }

    /// <summary>
    /// Trụ Hồi Phục Cổ Đại: Minh họa trực tiếp Dependency Inversion (ISwitchable).
    /// Khi Tướng đứng gần, tự động kích hoạt hồi Máu + Tăng tốc.
    /// </summary>
    public partial class HealingShrineSwitch3D : Node3D, ISwitchable
    {
        public bool IsActive { get; private set; } = true;
        private float _cooldownTimer;
        private MeshInstance3D _crystalMesh;
        private Label3D _label;
        private float _spinAngle;

        public void Initialize(Vector3 worldPos)
        {
            GlobalPosition = worldPos;

            _crystalMesh = new MeshInstance3D();
            PrismMesh prism = new() { Size = new Vector3(1.1f, 1.6f, 1.1f) };
            StandardMaterial3D mat = new()
            {
                AlbedoColor = new Color(0.2f, 0.95f, 0.55f),
                EmissionEnabled = true,
                Emission = new Color(0.15f, 0.85f, 0.45f),
                EmissionEnergyMultiplier = 1.2f
            };
            _crystalMesh.Mesh = prism;
            _crystalMesh.MaterialOverride = mat;
            _crystalMesh.Position = new Vector3(0f, 1.1f, 0f);
            AddChild(_crystalMesh);

            _label = new Label3D
            {
                Text = "💚 Trụ Hồi Máu",
                PixelSize = 0.01f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                Modulate = new Color(0.35f, 1.0f, 0.65f),
                OutlineSize = 0,
                Position = new Vector3(0f, 2.3f, 0f)
            };
            AddChild(_label);
        }

        public void Activate(HeroCharacter activator)
        {
            if (!IsActive || activator == null || activator.IsDead) return;
            IsActive = false;
            _cooldownTimer = 15f;
            if (_crystalMesh != null) _crystalMesh.Visible = false;
            if (_label != null) _label.Text = "⏳ Đang nạp năng lượng...";

            activator.HealAndBuffFromShrine(65f);
            ArenaEventBus.RaiseFloatingText(activator.GlobalPosition, "+65 HP [ISwitchable]", new Color(0.25f, 1f, 0.5f));
            ArenaEventBus.RaiseAnnouncement($"💚 {activator.HeroName} kích hoạt Trụ Hồi Phục (+65 HP)!");
        }

        public void Deactivate()
        {
            IsActive = true;
            if (_crystalMesh != null) _crystalMesh.Visible = true;
            if (_label != null) _label.Text = "💚 Trụ Hồi Máu [ISwitchable]";
        }

        public override void _Process(double delta)
        {
            float dt = (float)delta;
            _spinAngle += dt * 2.0f;

            if (IsActive)
            {
                if (_crystalMesh != null)
                {
                    _crystalMesh.Rotation = new Vector3(0f, _spinAngle, 0f);
                }

                if (ArenaGameManager.Instance != null)
                {
                    foreach (HeroCharacter hero in ArenaGameManager.Instance.AllHeroes)
                    {
                        if (hero == null || hero.IsDead) continue;
                        float dist = new Vector2(hero.GlobalPosition.X - GlobalPosition.X, hero.GlobalPosition.Z - GlobalPosition.Z).Length();
                        if (dist <= 2.2f)
                        {
                            Activate(hero);
                            break;
                        }
                    }
                }
            }
            else
            {
                _cooldownTimer -= dt;
                if (_cooldownTimer <= 0f)
                {
                    Deactivate();
                }
            }
        }
    }
}
