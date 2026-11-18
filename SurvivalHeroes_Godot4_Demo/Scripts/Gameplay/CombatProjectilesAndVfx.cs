// ----------------------------------------------------------------------------
// CombatProjectilesAndVfx.cs
// Hệ thống Object Pool cho Đạn bay (Projectiles) và Vùng Nổ Kỹ Năng (AOE Rings)
// có bù trễ mạng (Lag Compensation dựa trên SentServerTime của PhotonMessageInfo).
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using System.Collections.Generic;
    using Godot;

    public partial class PooledProjectile3D : Node3D
    {
        public bool IsActive { get; private set; }
        public int OwnerViewId { get; private set; }
        public Vector3 Direction { get; private set; }
        public float Speed { get; private set; }
        public float MaxDistance { get; private set; }
        public float Damage { get; private set; }
        public float ExplosionRadius { get; private set; }
        public Color ProjectileColor { get; private set; }

        private float _traveledDistance;
        private MeshInstance3D _mesh;
        private StandardMaterial3D _mat;

        public override void _Ready()
        {
            _mesh = new MeshInstance3D();
            SphereMesh sphere = new() { Radius = 0.28f, Height = 0.56f };
            _mat = new StandardMaterial3D
            {
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded,
                AlbedoColor = Colors.Orange
            };
            _mesh.Mesh = sphere;
            _mesh.MaterialOverride = _mat;
            AddChild(_mesh);
            Visible = false;
        }

        public void Activate(
            int ownerViewId,
            Vector3 startPos,
            Vector3 direction,
            float speed,
            float maxDist,
            float damage,
            float explosionRadius,
            Color color,
            float lagSeconds)
        {
            OwnerViewId = ownerViewId;
            Direction = direction.Normalized();
            Speed = speed;
            MaxDistance = maxDist;
            Damage = damage;
            ExplosionRadius = explosionRadius;
            ProjectileColor = color;

            // Bù trễ mạng (Lag Compensation): Tua nhanh vị trí đạn theo thời gian gói tin đi trên mạng
            float catchUpDist = Mathf.Clamp(lagSeconds * speed, 0f, maxDist * 0.6f);
            GlobalPosition = startPos + Direction * catchUpDist;
            _traveledDistance = catchUpDist;

            if (_mat != null) _mat.AlbedoColor = color;
            Scale = explosionRadius > 1.0f ? new Vector3(1.6f, 1.6f, 1.6f) : Vector3.One;

            IsActive = true;
            Visible = true;
        }

        public void Deactivate()
        {
            IsActive = false;
            Visible = false;
        }

        public override void _PhysicsProcess(double delta)
        {
            if (!IsActive) return;

            float step = Speed * (float)delta;
            GlobalPosition += Direction * step;
            _traveledDistance += step;

            // Kiểm tra va chạm với các Hero trong đấu trường
            if (ArenaGameManager.Instance != null)
            {
                foreach (HeroCharacter hero in ArenaGameManager.Instance.AllHeroes)
                {
                    if (hero == null || !GodotObject.IsInstanceValid(hero) || hero.IsDead) continue;
                    if (hero.PhotonView != null && hero.PhotonView.ViewID == OwnerViewId) continue;
                    if (ArenaGameManager.Instance.AreSameTeam(OwnerViewId, hero)) continue;

                    float dist2D = new Vector2(GlobalPosition.X - hero.GlobalPosition.X, GlobalPosition.Z - hero.GlobalPosition.Z).Length();
                    if (dist2D <= 1.05f)
                    {
                        if (ExplosionRadius > 0.5f)
                        {
                            ArenaGameManager.Instance.SpawnAoeRingEffect(hero.GlobalPosition, ExplosionRadius, ProjectileColor, 0.35f);
                            ArenaGameManager.Instance.ApplyAoeDamage(OwnerViewId, hero.GlobalPosition, ExplosionRadius, Damage);
                        }
                        else
                        {
                            hero.TakeDamage(Damage, OwnerViewId);
                        }
                        Deactivate();
                        return;
                    }
                }

                // Kiểm tra va chạm với Thùng Thuốc Nổ 3D XZ (IDamageable / IExplodable)
                foreach (var barrel in ArenaGameManager.Instance.ExplosiveBarrels)
                {
                    if (barrel == null || barrel.IsDestroyed) continue;
                    float dist2D = new Vector2(GlobalPosition.X - barrel.WorldPositionXZ.X, GlobalPosition.Z - barrel.WorldPositionXZ.Z).Length();
                    if (dist2D <= 1.0f)
                    {
                        barrel.TakeDamage(Damage, OwnerViewId);
                        Deactivate();
                        return;
                    }
                }

                // Kiểm tra va chạm với Quái Rừng & Boss Rồng Cổ Đại (JungleMonster3D : IDamageable)
                foreach (var monster in ArenaGameManager.Instance.JungleMonsters)
                {
                    if (monster == null || monster.IsDestroyed) continue;
                    float hitRad = monster.IsDragonBoss ? 2.1f : 1.15f;
                    float dist2D = new Vector2(GlobalPosition.X - monster.WorldPositionXZ.X, GlobalPosition.Z - monster.WorldPositionXZ.Z).Length();
                    if (dist2D <= hitRad)
                    {
                        monster.TakeDamage(Damage, OwnerViewId);
                        Deactivate();
                        return;
                    }
                }
            }

            if (_traveledDistance >= MaxDistance)
            {
                if (ExplosionRadius > 0.5f && ArenaGameManager.Instance != null)
                {
                    ArenaGameManager.Instance.SpawnAoeRingEffect(GlobalPosition, ExplosionRadius, ProjectileColor, 0.35f);
                    ArenaGameManager.Instance.ApplyAoeDamage(OwnerViewId, GlobalPosition, ExplosionRadius, Damage);
                }
                Deactivate();
            }
        }
    }

    /// <summary>Hiệu ứng vòng tròn kỹ năng AOE / Vệt chém hiển thị trên mặt đất (tái sử dụng qua Object Pool).</summary>
    public partial class PooledAoeRing3D : Node3D
    {
        public bool IsActive { get; private set; }
        private float _timer;
        private float _duration;
        private MeshInstance3D _mesh;
        private StandardMaterial3D _mat;

        public override void _Ready()
        {
            _mesh = new MeshInstance3D();
            CylinderMesh cyl = new() { TopRadius = 1.0f, BottomRadius = 1.0f, Height = 0.08f, RadialSegments = 24 };
            _mat = new StandardMaterial3D
            {
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded,
                AlbedoColor = new Color(1f, 0.5f, 0.1f, 0.55f)
            };
            _mesh.Mesh = cyl;
            _mesh.MaterialOverride = _mat;
            AddChild(_mesh);
            Visible = false;
        }

        public void ShowEffect(Vector3 worldPos, float radius, Color color, float duration)
        {
            GlobalPosition = new Vector3(worldPos.X, 0.08f, worldPos.Z);
            Scale = new Vector3(radius, 1f, radius);
            if (_mat != null)
            {
                _mat.AlbedoColor = new Color(color.R, color.G, color.B, 0.55f);
            }
            _duration = Mathf.Max(0.1f, duration);
            _timer = _duration;
            IsActive = true;
            Visible = true;
        }

        public override void _Process(double delta)
        {
            if (!IsActive) return;
            _timer -= (float)delta;
            if (_timer <= 0f)
            {
                IsActive = false;
                Visible = false;
                return;
            }
            if (_mat != null)
            {
                Color c = _mat.AlbedoColor;
                c.A = 0.55f * (_timer / _duration);
                _mat.AlbedoColor = c;
            }
        }
    }
}
