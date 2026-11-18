namespace SurvivalHeroes.Gameplay.Entities
{
    using System;
    using Godot;
    using SurvivalHeroes.Core.Events;
    using SurvivalHeroes.Core.Interfaces;
    using SurvivalHeroes.Core.Physics;

    /// <summary>
    /// Quái Rừng & Boss Rồng Cổ Đại thực thi IDamageable trong không gian 3D XZ.
    /// </summary>
    public partial class JungleMonster3D : Node3D, IDamageable
    {
        public string MonsterTitle { get; private set; }
        public bool IsDragonBoss { get; private set; }
        public Vector3 WorldPositionXZ => GlobalPosition;
        public bool IsDestroyed { get; private set; }

        private float _maxHp;
        private float _hp;
        private float _attackDamage;
        private int _goldReward;
        private float _expReward;
        private float _respawnTimer;
        private float _attackCooldown;
        private Vector3 _homePos;

        private Node3D _visualRoot;
        private Label3D _infoLabel;
        private string _lastMonsterLabel = "";

        public void Initialize(string title, Vector3 spawnPos, bool isDragonBoss)
        {
            MonsterTitle = title;
            IsDragonBoss = isDragonBoss;
            _homePos = new Vector3(spawnPos.X, 0f, spawnPos.Z);
            GlobalPosition = _homePos;

            _maxHp = isDragonBoss ? 650f : 150f;
            _hp = _maxHp;
            _attackDamage = isDragonBoss ? 28f : 12f;
            _goldReward = isDragonBoss ? 350 : 75;
            _expReward = isDragonBoss ? 220f : 65f;

            _visualRoot = new Node3D();
            AddChild(_visualRoot);

            Color bodyColor = isDragonBoss ? new Color(0.85f, 0.15f, 0.95f) : new Color(0.65f, 0.42f, 0.22f);
            float scaleMult = isDragonBoss ? 2.2f : 1.0f;

            MeshInstance3D body = new()
            {
                Mesh = new BoxMesh { Size = new Vector3(1.2f, 1.5f, 1.6f) * scaleMult },
                Position = new Vector3(0f, 0.8f * scaleMult, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = bodyColor,
                    EmissionEnabled = isDragonBoss,
                    Emission = bodyColor,
                    EmissionEnergyMultiplier = 0.7f
                }
            };
            _visualRoot.AddChild(body);

            if (isDragonBoss)
            {
                MeshInstance3D wings = new()
                {
                    Mesh = new BoxMesh { Size = new Vector3(5.8f, 0.25f, 1.8f) },
                    Position = new Vector3(0f, 2.2f, 0f),
                    MaterialOverride = new StandardMaterial3D { AlbedoColor = new Color(0.95f, 0.3f, 0.6f) }
                };
                _visualRoot.AddChild(wings);
            }

            _infoLabel = new Label3D
            {
                PixelSize = isDragonBoss ? 0.0135f : 0.0105f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                OutlineSize = 0,
                Position = new Vector3(0f, isDragonBoss ? 4.2f : 2.3f, 0f)
            };
            AddChild(_infoLabel);
            UpdateMonsterLabel();
        }

        public void TakeDamage(float amount, int attackerViewId)
        {
            if (IsDestroyed) return;
            _hp = MathF.Max(0f, _hp - amount);
            ArenaEventBus.RaiseFloatingText(GlobalPosition, $"-{Mathf.RoundToInt(amount)}", new Color(1f, 0.55f, 0.2f));
            UpdateMonsterLabel();

            if (_hp <= 0f)
            {
                IsDestroyed = true;
                _respawnTimer = IsDragonBoss ? 45f : 20f;
                if (_visualRoot != null) _visualRoot.Visible = false;
                UpdateMonsterLabel();

                ArenaGameManager.Instance?.NotifyMonsterSlain(this, attackerViewId, _goldReward, _expReward);
            }
        }

        public override void _Process(double delta)
        {
            float dt = (float)delta;
            if (IsDestroyed)
            {
                _respawnTimer -= dt;
                if (_respawnTimer <= 0f)
                {
                    IsDestroyed = false;
                    _hp = _maxHp;
                    GlobalPosition = _homePos;
                    if (_visualRoot != null) _visualRoot.Visible = true;
                }
                UpdateMonsterLabel();
                return;
            }

            _attackCooldown = MathF.Max(0f, _attackCooldown - dt);
            if (_attackCooldown <= 0f && ArenaGameManager.Instance != null)
            {
                foreach (HeroCharacter hero in ArenaGameManager.Instance.AllHeroes)
                {
                    if (hero == null || hero.IsDead) continue;
                    float dist = PlanarConstraintXZ.DistanceXZ(GlobalPosition, hero.GlobalPosition);
                    if (dist <= (IsDragonBoss ? 6.5f : 3.6f))
                    {
                        _attackCooldown = IsDragonBoss ? 1.8f : 1.5f;
                        ArenaGameManager.Instance.SpawnAoeRingEffect(hero.GlobalPosition, IsDragonBoss ? 2.8f : 1.5f, Colors.Purple, 0.28f);
                        hero.TakeDamage(_attackDamage, 0);
                        break;
                    }
                }
            }
        }

        private void UpdateMonsterLabel()
        {
            if (_infoLabel == null) return;
            if (IsDestroyed)
            {
                string deadTxt = $"⏳ {MonsterTitle} (Hồi sinh {Mathf.CeilToInt(_respawnTimer)}s)";
                if (_lastMonsterLabel != deadTxt)
                {
                    _lastMonsterLabel = deadTxt;
                    _infoLabel.Text = deadTxt;
                    _infoLabel.Modulate = new Color(0.65f, 0.65f, 0.7f);
                }
                return;
            }

            string icon = IsDragonBoss ? "🐉 BOSS" : "🐺 Quái Rừng";
            string aliveTxt = $"{icon}: {MonsterTitle}\n❤️ {(int)_hp}/{(int)_maxHp} (+{_goldReward}💰)";
            if (_lastMonsterLabel != aliveTxt)
            {
                _lastMonsterLabel = aliveTxt;
                _infoLabel.Text = aliveTxt;
                _infoLabel.Modulate = IsDragonBoss ? new Color(1.0f, 0.45f, 0.95f) : new Color(1.0f, 0.85f, 0.45f);
            }
        }
    }
}
