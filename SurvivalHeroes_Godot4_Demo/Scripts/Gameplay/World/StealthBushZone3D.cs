namespace SurvivalHeroes.Gameplay.World
{
    using Godot;
    using SurvivalHeroes.Core.Physics;

    /// <summary>Bụi cỏ MOBA trong không gian 3D XZ: Khi đứng trong bán kính bụi cỏ, nhân vật được ẩn thân.</summary>
    public partial class StealthBushZone3D : Node3D
    {
        public float BushRadius { get; private set; } = 3.2f;

        public void Initialize(Vector3 worldPos, float radius = 3.2f)
        {
            GlobalPosition = new Vector3(worldPos.X, 0f, worldPos.Z);
            BushRadius = radius;

            MeshInstance3D bushBase = new();
            CylinderMesh cyl = new() { TopRadius = radius, BottomRadius = radius * 1.05f, Height = 1.35f, RadialSegments = 16 };
            StandardMaterial3D mat = new()
            {
                AlbedoColor = new Color(0.12f, 0.58f, 0.22f, 0.48f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha
            };
            bushBase.Mesh = cyl;
            bushBase.MaterialOverride = mat;
            bushBase.Position = new Vector3(0f, 0.65f, 0f);
            AddChild(bushBase);
        }

        public bool ContainsPoint(Vector3 pos)
            => PlanarConstraintXZ.DistanceXZ(pos, GlobalPosition) <= BushRadius;
    }
}
