namespace SurvivalHeroes.Gameplay.Entities
{
    using Godot;

    /// <summary>
    /// Mắt Kiểm Soát (Scout Ward [Phím 5]):
    /// Cung cấp tầm nhìn chiến thuật XUYÊN ĐỊA HÌNH / VƯỢT TƯỜNG và SOI BỤI CỎ
    /// trong bán kính 12m suốt 25 giây cho toàn bộ đồng đội trong hệ thống FogOfWarSystem!
    /// </summary>
    public partial class ScoutWard3D : Node3D
    {
        public int OwnerTeamId { get; private set; }
        public float VisionRadius { get; private set; } = 12.0f;
        public bool IsExpired => _lifeTimer <= 0f;

        private float _lifeTimer = 25.0f;

        public void Initialize(int ownerTeamId, Vector3 worldPos)
        {
            OwnerTeamId = ownerTeamId;
            GlobalPosition = new Vector3(worldPos.X, 0f, worldPos.Z);

            MeshInstance3D beaconMesh = new()
            {
                Mesh = new CylinderMesh { TopRadius = 0.25f, BottomRadius = 0.35f, Height = 1.4f },
                Position = new Vector3(0f, 0.7f, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(1.0f, 0.9f, 0.2f),
                    EmissionEnabled = true,
                    Emission = new Color(1.0f, 0.85f, 0.1f),
                    EmissionEnergyMultiplier = 1.4f
                }
            };
            AddChild(beaconMesh);

            MeshInstance3D ring = new()
            {
                Mesh = new CylinderMesh { TopRadius = VisionRadius, BottomRadius = VisionRadius, Height = 0.04f, RadialSegments = 32 },
                Position = new Vector3(0f, 0.05f, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(1.0f, 0.92f, 0.25f, 0.14f),
                    Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                    ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded
                }
            };
            AddChild(ring);

            AddChild(new Label3D
            {
                Text = "👁️ Mắt Kiểm Soát (Soi Xuyên Tường & Bụi)",
                PixelSize = 0.01f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                OutlineSize = 0,
                Modulate = new Color(1.0f, 0.92f, 0.3f),
                Position = new Vector3(0f, 1.85f, 0f)
            });
        }

        public override void _Process(double delta)
        {
            _lifeTimer -= (float)delta;
            if (_lifeTimer <= 0f)
            {
                QueueFree();
            }
        }
    }
}
