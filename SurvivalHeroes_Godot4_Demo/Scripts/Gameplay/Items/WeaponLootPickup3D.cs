namespace SurvivalHeroes.Gameplay.Items
{
    using Godot;

    /// <summary>
    /// Hộp Vũ Khí trên mặt đất (Weapon Loot Crate).
    /// Được duyệt quyền nhặt thông qua MasterClient (2-Phase Request -> Grant) để chống nhân bản trang bị!
    /// </summary>
    public partial class WeaponLootPickup3D : Node3D
    {
        public int LootId { get; set; }
        public WeaponType WeaponKind { get; set; }
        public bool IsAvailable { get; private set; } = true;

        private float _respawnTimer;
        private float _bobTime;
        private MeshInstance3D _weaponMesh;
        private Label3D _titleLabel;

        public void Initialize(int lootId, WeaponType weaponKind, Vector3 worldPos)
        {
            LootId = lootId;
            WeaponKind = weaponKind;
            GlobalPosition = new Vector3(worldPos.X, 0f, worldPos.Z);

            WeaponLoadout loadout = WeaponLoadout.Get(weaponKind);

            MeshInstance3D pad = new();
            CylinderMesh padCyl = new() { TopRadius = 1.15f, BottomRadius = 1.25f, Height = 0.08f };
            StandardMaterial3D padMat = new()
            {
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded,
                AlbedoColor = new Color(loadout.ThemeColor.R, loadout.ThemeColor.G, loadout.ThemeColor.B, 0.35f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha
            };
            pad.Mesh = padCyl;
            pad.MaterialOverride = padMat;
            pad.Position = new Vector3(0f, 0.04f, 0f);
            AddChild(pad);

            _weaponMesh = new MeshInstance3D();
            BoxMesh box = weaponKind switch
            {
                WeaponType.Greatsword => new BoxMesh { Size = new Vector3(0.35f, 1.45f, 0.18f) },
                WeaponType.RangerBow => new BoxMesh { Size = new Vector3(0.95f, 0.95f, 0.22f) },
                _ => new BoxMesh { Size = new Vector3(0.28f, 1.55f, 0.28f) }
            };
            StandardMaterial3D mat = new()
            {
                AlbedoColor = loadout.ThemeColor,
                EmissionEnabled = true,
                Emission = loadout.ThemeColor,
                EmissionEnergyMultiplier = 1.4f
            };
            _weaponMesh.Mesh = box;
            _weaponMesh.MaterialOverride = mat;
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
}
