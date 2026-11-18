namespace SurvivalHeroes.Core.Physics
{
    using Godot;

    /// <summary>
    /// Chốt dứt điểm 100% hệ quy chiếu Không gian 3D Isometric (Mặt phẳng ngang XZ, khóa cứng độ cao Y = 0):
    ///   - Độ sâu hiển thị (Depth Sorting) do Hardware Z-Buffer của GPU xử lý tự động.
    ///   - Khóa cứng dịch chuyển trục Y và khóa lật nghiêng trục X/Z trên Jolt Physics 3D.
    ///   - Mọi truy vấn khoảng cách Hitbox/Hurtbox và Line-of-Sight chiếu phẳng lên mặt XZ.
    /// </summary>
    public static class PlanarConstraintXZ
    {
        public static void ApplyGroundLock(PhysicsBody3D body)
        {
            if (body == null) return;
            body.AxisLockLinearY = true;
            body.AxisLockLinearX = false;
            body.AxisLockLinearZ = false;

            body.AxisLockAngularX = true;
            body.AxisLockAngularZ = true;
            body.AxisLockAngularY = false;
        }

        public static Vector3 ClampVelocityXZ(Vector3 velocity)
            => new(velocity.X, 0f, velocity.Z);

        public static Vector3 ClampPositionXZ(Vector3 position, float halfExtent)
            => new(
                Mathf.Clamp(position.X, -halfExtent, halfExtent),
                0f,
                Mathf.Clamp(position.Z, -halfExtent, halfExtent)
            );

        public static Vector2 ToXZ(Vector3 pos3D)
            => new(pos3D.X, pos3D.Z);

        public static float DistanceXZ(Vector3 a, Vector3 b)
            => new Vector2(a.X - b.X, a.Z - b.Z).Length();
    }
}
