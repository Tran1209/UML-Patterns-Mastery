namespace SurvivalHeroes.Gameplay.Entities
{
    using System;
    using Godot;
    using SurvivalHeroes.Core.Events;
    using SurvivalHeroes.Core.Interfaces;
    using SurvivalHeroes.Core.Physics;

    /// <summary>
    /// [TẦNG 5 - GAMEPLAY ENTITIES]:
    /// Đã chuyển từ Tầng 4 sang đúng Tầng 5 để không gây phụ thuộc ngược (Layer Violation).
    /// Bao gồm: PooledFloatingText3D, ExplosiveBarrel3D (IDamageable, IExplodable), và HealingShrineSwitch3D (ISwitchable).
    /// </summary>
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
            if (_label != null && _label.Text != text)
            {
                _label.Text = text;
            }
            if (_label != null) _label.Modulate = color;
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

    public partial class ExplosiveBarrel3D : Node3D, IDamageable, IExplodable
    {
        public Vector3 WorldPositionXZ => GlobalPosition;
        public bool IsDestroyed { get; private set; }
        public float ExplosionRadius => 5.0f;
        public float ExplosionDamage => 55f;

        private float _health = 40f;
        private float _respawnTimer;
        private MeshInstance3D _barrelMesh;
        private Label3D _label;

        public void Initialize(Vector3 worldPos)
        {
            GlobalPosition = new Vector3(worldPos.X, 0f, worldPos.Z);

            _barrelMesh = new MeshInstance3D
            {
                Mesh = new CylinderMesh { TopRadius = 0.65f, BottomRadius = 0.65f, Height = 1.4f },
                Position = new Vector3(0f, 0.7f, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(0.92f, 0.22f, 0.18f),
                    EmissionEnabled = true,
                    Emission = new Color(0.6f, 0.1f, 0.05f),
                    EmissionEnergyMultiplier = 0.5f
                }
            };
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
            if (_label != null) _label.Text = "💥 Đã phát nổ";

            ArenaEventBus.RaiseFloatingText(GlobalPosition, "💥 BÙM! -55 AOE", new Color(1f, 0.35f, 0.1f));
            ArenaEventBus.RaiseAnnouncement("🧨 Thùng Thuốc Nổ vừa kích nổ dây chuyền!");

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
                if (_label != null) _label.Text = "🧨 Thùng Nổ";
            }
        }
    }

    public partial class HealingShrineSwitch3D : Node3D, ISwitchable
    {
        public bool IsActive { get; private set; } = true;
        private float _cooldownTimer;
        private MeshInstance3D _crystalMesh;
        private Label3D _label;
        private float _spinAngle;

        public void Initialize(Vector3 worldPos)
        {
            GlobalPosition = new Vector3(worldPos.X, 0f, worldPos.Z);

            _crystalMesh = new MeshInstance3D
            {
                Mesh = new PrismMesh { Size = new Vector3(1.1f, 1.6f, 1.1f) },
                Position = new Vector3(0f, 1.1f, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(0.2f, 0.95f, 0.55f),
                    EmissionEnabled = true,
                    Emission = new Color(0.15f, 0.85f, 0.45f),
                    EmissionEnergyMultiplier = 1.2f
                }
            };
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

        public void Activate(Node3D activator)
        {
            if (!IsActive || activator is not HeroCharacter hero || hero.IsDead) return;
            IsActive = false;
            _cooldownTimer = 15f;
            if (_crystalMesh != null) _crystalMesh.Visible = false;
            if (_label != null) _label.Text = "⏳ Đang nạp...";

            hero.HealAndBuffFromShrine(65f);
            ArenaEventBus.RaiseFloatingText(hero.GlobalPosition, "+65 HP", new Color(0.25f, 1f, 0.5f));
            ArenaEventBus.RaiseAnnouncement($"💚 {hero.HeroName} kích hoạt Trụ Hồi Phục (+65 HP)!");
        }

        public void Deactivate()
        {
            IsActive = true;
            if (_crystalMesh != null) _crystalMesh.Visible = true;
            if (_label != null) _label.Text = "💚 Trụ Hồi Máu";
        }

        public override void _Process(double delta)
        {
            float dt = (float)delta;
            _spinAngle += dt * 2.0f;

            if (IsActive)
            {
                if (_crystalMesh != null) _crystalMesh.Rotation = new Vector3(0f, _spinAngle, 0f);
                if (ArenaGameManager.Instance != null)
                {
                    foreach (HeroCharacter hero in ArenaGameManager.Instance.AllHeroes)
                    {
                        if (hero == null || hero.IsDead) continue;
                        if (PlanarConstraintXZ.DistanceXZ(hero.GlobalPosition, GlobalPosition) <= 2.2f)
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
                if (_cooldownTimer <= 0f) Deactivate();
            }
        }
    }
}
