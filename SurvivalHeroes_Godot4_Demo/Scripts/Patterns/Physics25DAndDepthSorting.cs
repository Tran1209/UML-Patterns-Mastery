// ----------------------------------------------------------------------------
// Physics25DAndDepthSorting.cs
// Giải quyết triệt để 3 bài toán cốt lõi của Kiến trúc Game 2.5D:
//   1. Vật lý Lai 2.5D (Hybrid 2.5D Physics):
//      - Chuyển động & Va chạm tường/nhà dùng Jolt Physics 3D (CharacterBody3D)
//      - Tương tác chiến đấu (Đạn, Vòng nổ AOE, Bụi cỏ, Vòng Bo) dùng Hình học 2D phẳng
//   2. Khóa Trục Vật Lý (Axis Locking & Planar Projection):
//      - Hỗ trợ cả 2 chế độ 2.5D:
//          + TopDownIsometric_LockY (Survival Heroes / MOBA: mặt đất XZ, khóa cứng Y = 0)
//          + SideScroller_LockZ     (Platformer 2.5D: mặt đứng XY, khóa cứng Z = 0)
//      - Khóa trực tiếp trên PhysicsServer3D (AxisLockLinearY/Z, AxisLockAngularX/Z)
//        kết hợp ép phẳng Vector vận tốc và vị trí mỗi Physics Frame.
//   3. Sắp xếp Độ sâu Hiển thị (2.5D Depth Sorting / Z-Ordering & Anti Z-Fighting):
//      - Tự động tính SortingOffset & RenderPriority theo tọa độ trục Z (gần Camera hơn
//        vẽ đè lên xa Camera hơn) cho các vật thể bán trong suốt (Khiên, Bụi cỏ tàng hình,
//        Sprite3D, Thanh máu trên đầu).
//      - Phân tầng độ cao vi mô (Ground Layer Elevation) chống nhấp nháy mặt đất (Z-fighting).
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Patterns
{
    using System;
    using Godot;

    /// <summary>Hai hệ quy chiếu mặt phẳng 2.5D phổ biến nhất trong phát triển game.</summary>
    public enum PlaneMode25D
    {
        /// <summary>
        /// Góc nhìn từ trên xuống / Isometric (Survival Heroes, Liên Quân, Hades):
        /// Mặt phẳng di chuyển là (X, Z), trục bị khóa cứng là trục độ cao Y = 0.
        /// Độ sâu vẽ trước-sau (Depth Sorting) tính theo trục Z (+Z gần Camera hơn).
        /// </summary>
        TopDownIsometric_LockY = 0,

        /// <summary>
        /// Góc nhìn ngang (Side-scroller 2.5D như Hollow Knight, Ori, Metroid Dread):
        /// Mặt phẳng di chuyển là (X, Y), trục bị khóa cứng là trục độ sâu Z = 0.
        /// </summary>
        SideScroller_LockZ = 1
    }

    /// <summary>
    /// Bộ khóa trục vật lý & chiếu phẳng toán học 2.5D (Planar Constraint Engine).
    /// </summary>
    public static class PlanarPhysics25D
    {
        public static PlaneMode25D CurrentMode { get; set; } = PlaneMode25D.TopDownIsometric_LockY;

        /// <summary>
        /// Cấu hình khóa trục trực tiếp ở tầng Engine Vật lý 3D (Jolt Physics / GodotPhysics3D)
        /// để va chạm vật lý không bao giờ đẩy nhân vật bay lên trời hoặc lật nghiêng.
        /// </summary>
        public static void ApplyHardwareAxisLock(PhysicsBody3D body, PlaneMode25D mode = PlaneMode25D.TopDownIsometric_LockY)
        {
            if (body == null) return;

            if (mode == PlaneMode25D.TopDownIsometric_LockY)
            {
                // Khóa dịch chuyển trục Y (không bị nảy lên/chìm xuống đất)
                body.AxisLockLinearY = true;
                body.AxisLockLinearX = false;
                body.AxisLockLinearZ = false;

                // Khóa xoay trục X và Z (không bị đổ ngã), chỉ cho phép xoay hướng mặt quanh trục Y (Yaw)
                body.AxisLockAngularX = true;
                body.AxisLockAngularZ = true;
                body.AxisLockAngularY = false;
            }
            else
            {
                // Chế độ Side-Scroller 2.5D: Di chuyển trên mặt phẳng (X, Y), khóa cứng trục Z = 0
                body.AxisLockLinearZ = true;
                body.AxisLockLinearX = false;
                body.AxisLockLinearY = false;

                body.AxisLockAngularX = true;
                body.AxisLockAngularY = true;
                body.AxisLockAngularZ = false;
            }
        }

        /// <summary>Ép vector vận tốc 3D nằm tuyệt đối trên mặt phẳng 2.5D.</summary>
        public static Vector3 ClampVelocityToPlane(Vector3 velocity, PlaneMode25D mode = PlaneMode25D.TopDownIsometric_LockY)
        {
            return mode == PlaneMode25D.TopDownIsometric_LockY
                ? new Vector3(velocity.X, 0f, velocity.Z)
                : new Vector3(velocity.X, velocity.Y, 0f);
        }

        /// <summary>Ép tọa độ 3D về mặt phẳng chuẩn và giới hạn trong biên bản đồ.</summary>
        public static Vector3 ClampPositionToPlaneAndBounds(
            Vector3 position,
            float mapHalfExtent,
            float fixedPlaneCoord = 0f,
            PlaneMode25D mode = PlaneMode25D.TopDownIsometric_LockY)
        {
            if (mode == PlaneMode25D.TopDownIsometric_LockY)
            {
                return new Vector3(
                    Mathf.Clamp(position.X, -mapHalfExtent, mapHalfExtent),
                    fixedPlaneCoord, // Khóa cứng Y = 0
                    Mathf.Clamp(position.Z, -mapHalfExtent, mapHalfExtent)
                );
            }

            return new Vector3(
                Mathf.Clamp(position.X, -mapHalfExtent, mapHalfExtent),
                Mathf.Clamp(position.Y, -mapHalfExtent, mapHalfExtent),
                fixedPlaneCoord // Khóa cứng Z = 0
            );
        }

        /// <summary>Chuyển tọa độ 3D sang tọa độ phẳng 2D để tính va chạm Hitbox/Hurtbox siêu tốc.</summary>
        public static Vector2 ToPlane2D(Vector3 pos3D, PlaneMode25D mode = PlaneMode25D.TopDownIsometric_LockY)
        {
            return mode == PlaneMode25D.TopDownIsometric_LockY
                ? new Vector2(pos3D.X, pos3D.Z)
                : new Vector2(pos3D.X, pos3D.Y);
        }

        /// <summary>Tính khoảng cách phẳng 2.5D (bỏ qua độ cao Y của đạn bay/hiệu ứng).</summary>
        public static float Distance25D(Vector3 a, Vector3 b, PlaneMode25D mode = PlaneMode25D.TopDownIsometric_LockY)
        {
            return ToPlane2D(a, mode).DistanceTo(ToPlane2D(b, mode));
        }
    }

    /// <summary>
    /// Bộ xử lý Depth Sorting (Y-Sort / Z-Sort) & Chống Z-Fighting cho không gian 2.5D Isometric.
    /// </summary>
    public static class DepthSorter25D
    {
        // ====================================================================
        // 1. BẢNG PHÂN TẦNG ĐỘ CAO VI MÔ TRÊN MẶT ĐẤT (ANTI Z-FIGHTING LAYERS)
        // ====================================================================
        public const float ElevationFloorBase   = 0.00f;
        public const float ElevationGridLines   = 0.02f;
        public const float ElevationWaterAndRoad= 0.03f;
        public const float ElevationTownPlaza   = 0.04f;
        public const float ElevationSafeZoneRing= 0.05f;
        public const float ElevationLootPad     = 0.06f;
        public const float ElevationAoeSkillRing= 0.07f;
        public const float ElevationAimReticle  = 0.08f;

        // ====================================================================
        // 2. BẢNG THỨ TỰ VẼ ƯU TIÊN (RENDER PRIORITY CHO VẬT LIỆU TRONG SUỐT)
        // ====================================================================
        public const int PriorityGroundDecal = -10;
        public const int PrioritySafeZone    = -8;
        public const int PriorityLootPad     = -6;
        public const int PriorityAoeRing     = -4;
        public const int PriorityAimReticle  = -2;
        public const int PriorityStealthBush = 2;
        public const int PriorityHeroAlpha   = 5;
        public const int PriorityShieldVfx   = 8;
        public const int PriorityOverheadUi  = 20;

        /// <summary>
        /// Cập nhật SortingOffset theo trục Z cho VisualInstance3D (MeshInstance3D, Sprite3D, Label3D).
        /// Trong góc nhìn Isometric (Camera đặt ở +Z nhìn về -Z):
        ///   - Thực thể có tọa độ Z lớn hơn nằm gần Camera hơn -> cần SortingOffset dương lớn hơn
        ///     để luôn vẽ đè lên thực thể đứng phía sau (Z nhỏ hơn) khi bật Transparency Alpha!
        /// </summary>
        public static void UpdateIsometricDepthOrder(VisualInstance3D visual, float worldZ, float extraBias = 0f)
        {
            if (visual == null) return;
            // Quy đổi Z (-85..+85) sang độ lệch ưu tiên глубины nhỏ (SortingOffset)
            visual.SortingOffset = (worldZ * 0.02f) + extraBias;
        }

        /// <summary>
        /// Tính chỉ số RenderPriority động theo tọa độ Z cho các Material bán trong suốt
        /// (ví dụ 2 nhân vật cùng tàng hình trong bụi cỏ hoặc 2 vòng khiên chồng lên nhau).
        /// </summary>
        public static int ComputeDynamicRenderPriority(float worldZ, int basePriority = PriorityHeroAlpha)
        {
            // Chia bản đồ -85..+85m thành dải ưu tiên -15..+15 cộng vào basePriority (nằm gọn trong -128..127 của Godot)
            int zBucket = Mathf.Clamp(Mathf.RoundToInt(worldZ / 6.0f), -15, 15);
            return Mathf.Clamp(basePriority + zBucket, -120, 120);
        }
    }
}
