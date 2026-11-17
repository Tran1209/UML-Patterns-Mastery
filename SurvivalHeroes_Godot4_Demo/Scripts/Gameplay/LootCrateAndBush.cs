// ----------------------------------------------------------------------------
// LootCrateAndBush.cs
// Hệ thống Hộp Vũ Khí / Vật Phẩm trên mặt đất (Loot Pickup) và Bụi Cỏ Tàng Hình
// (MOBA Stealth Bush) của Survival Heroes.
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using Godot;

    public partial class WeaponLootPickup3D : Node3D
    {
        public int LootId { get; set; }
        public WeaponType WeaponKind { get; set; }
        public bool IsAvailable { get; private set; } = true;

        private float _respawnTimer;
        private float _bobTime;
        private MeshInstance3D _weaponMesh;
        private Label3D _titleLabel;
        private StandardMaterial3D _mat;

        public void Initialize(int lootId, WeaponType weaponKind, Vector3 worldPos)
        {
            LootId = lootId;
            WeaponKind = weaponKind;
            GlobalPosition = worldPos;

            WeaponLoadout loadout = WeaponLoadout.Get(weaponKind);

            // Tạo đế phát sáng dưới đất
            MeshInstance3D pad = new();
            CylinderMesh padCyl = new() { TopRadius = 1.15f, BottomRadius = 1.25f, Height = 0.1f };
            StandardMaterial3D padMat = new()
            {
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded,
                AlbedoColor = new Color(loadout.ThemeColor.R, loadout.ThemeColor.G, loadout.ThemeColor.B, 0.35f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha
            };
            pad.Mesh = padCyl;
            pad.MaterialOverride = padMat;
            AddChild(pad);

            // Tạo khối biểu tượng Vũ khí xoay lơ lửng
            _weaponMesh = new MeshInstance3D();
            BoxMesh box = weaponKind switch
            {
                WeaponType.Greatsword => new BoxMesh { Size = new Vector3(0.35f, 1.45f, 0.18f) },
                WeaponType.RangerBow => new BoxMesh { Size = new Vector3(0.95f, 0.95f, 0.22f) },
                _ => new BoxMesh { Size = new Vector3(0.28f, 1.55f, 0.28f) }
            };
            _mat = new StandardMaterial3D
            {
                AlbedoColor = loadout.ThemeColor,
                EmissionEnabled = true,
                Emission = loadout.ThemeColor,
                EmissionEnergyMultiplier = 1.4f
            };
            _weaponMesh.Mesh = box;
            _weaponMesh.MaterialOverride = _mat;
            _weaponMesh.Position = new Vector3(0, 1.0f, 0);
            AddChild(_weaponMesh);

            _titleLabel = new Label3D
            {
                Text = $"[F] {loadout.Title}",
                PixelSize = 0.011f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                Modulate = loadout.ThemeColor,
                OutlineSize = 0,
                Position = new Vector3(0, 2.15f, 0)
            };
            AddChild(_titleLabel);
        }

        public void ConsumePickup()
        {
            IsAvailable = false;
            _respawnTimer = 14.0f;
            if (_weaponMesh != null) _weaponMesh.Visible = false;
            if (_titleLabel != null) _titleLabel.Text = "Đang hồi trang bị...";
        }

        public override void _Process(double delta)
        {
            _bobTime += (float)delta * 2.5f;
            if (IsAvailable)
            {
                if (_weaponMesh != null)
                {
                    _weaponMesh.Rotation = new Vector3(0.3f, _bobTime, 0.25f);
                    _weaponMesh.Position = new Vector3(0f, 1.0f + Mathf.Sin(_bobTime) * 0.18f, 0f);
                }
            }
            else
            {
                _respawnTimer -= (float)delta;
                if (_respawnTimer <= 0f)
                {
                    IsAvailable = true;
                    if (_weaponMesh != null) _weaponMesh.Visible = true;
                    if (_titleLabel != null) _titleLabel.Text = $"[F] {WeaponLoadout.Get(WeaponKind).Title}";
                }
            }
        }
    }

    /// <summary>Bụi cỏ MOBA: Khi đứng trong bán kính bụi cỏ, nhân vật được ẩn thân (Stealth).</summary>
    public partial class StealthBushZone3D : Node3D
    {
        public float BushRadius { get; private set; } = 3.2f;

        public void Initialize(Vector3 worldPos, float radius = 3.2f)
        {
            GlobalPosition = worldPos;
            BushRadius = radius;

            // Vòng nền bụi cỏ bán trong suốt
            MeshInstance3D bushBase = new();
            CylinderMesh cyl = new() { TopRadius = radius, BottomRadius = radius * 1.05f, Height = 1.35f, RadialSegments = 18 };
            StandardMaterial3D mat = new()
            {
                AlbedoColor = new Color(0.12f, 0.58f, 0.22f, 0.48f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha
            };
            bushBase.Mesh = cyl;
            bushBase.MaterialOverride = mat;
            bushBase.Position = new Vector3(0, 0.65f, 0);
            AddChild(bushBase);

            Label3D label = new()
            {
                Text = "🌿 Bụi Cỏ Ẩn Thân",
                PixelSize = 0.01f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                Modulate = new Color(0.55f, 1.0f, 0.65f),
                Position = new Vector3(0, 1.65f, 0)
            };
            AddChild(label);
        }

        public bool ContainsPoint(Vector3 pos)
        {
            return new Vector2(pos.X - GlobalPosition.X, pos.Z - GlobalPosition.Z).Length() <= BushRadius;
        }
    }
}
