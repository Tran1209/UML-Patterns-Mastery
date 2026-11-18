namespace SurvivalHeroes.Gameplay.World
{
    using System.Collections.Generic;
    using Godot;
    using SurvivalHeroes.Core.Physics;
    using SurvivalHeroes.Gameplay.Entities;

    /// <summary>
    /// [TẦNG 5 - HỆ THỐNG TẦM NHÌN & SƯƠNG MÙ CHIẾN TRANH (FOG OF WAR / LINE-OF-SIGHT)]:
    /// Giải quyết triệt để bài toán chiến thuật MOBA + Battle Royale:
    ///   1. Chắn tầm nhìn sau Tường / Tòa nhà (Ray-AABB Line-of-Sight Occlusion trên mặt phẳng XZ):
    ///      Nếu đoạn thẳng nối từ Tướng (hoặc Đồng đội cùng Team) tới kẻ địch cắt qua bất kỳ
    ///      khối Tường/Tòa nhà nào -> Kẻ địch bị khuất tầm nhìn (Fog of War) và ẩn hoàn toàn!
    ///   2. Ẩn thân trong Bụi Cỏ (Bush Stealth):
    ///      Kẻ địch đứng trong Bụi Cỏ bị ẩn trừ khi người chơi đứng cùng trong bụi đó (< 3.2m)
    ///      HOẶC có Mắt Kiểm Soát [Phím 5] (ScoutWard3D) cắm gần đó!
    ///   3. Tầm nhìn chiến thuật của Mắt Kiểm Soát [Phím 5] (ScoutWard3D):
    ///      Mắt Kiểm Soát cung cấp tầm nhìn XUYÊN ĐỊA HÌNH / VƯỢT TƯỜNG và SOI BỤI CỎ
    ///      trong bán kính 12m cho toàn bộ đồng đội cùng Team!
    /// </summary>
    public sealed class FogOfWarSystem
    {
        public float HeroVisionRange { get; set; } = 28.0f;
        public readonly List<Rect2> VisionBlockerRectsXZ = new();

        public void RegisterWallBlocker(Vector3 center3D, Vector3 size3D)
        {
            Vector2 half = new(size3D.X * 0.5f, size3D.Z * 0.5f);
            VisionBlockerRectsXZ.Add(new Rect2(new Vector2(center3D.X, center3D.Z) - half, new Vector2(size3D.X, size3D.Z)));
        }

        /// <summary>Kiểm tra đoạn thẳng nối 2 điểm trên mặt phẳng XZ có bị tường/nhà chắn ngang hay không.</summary>
        public bool IsLineOfSightBlockedByWall(Vector3 fromPos, Vector3 toPos)
        {
            Vector2 a = PlanarConstraintXZ.ToXZ(fromPos);
            Vector2 b = PlanarConstraintXZ.ToXZ(toPos);

            for (int i = 0; i < VisionBlockerRectsXZ.Count; i++)
            {
                if (SegmentIntersectsRect(a, b, VisionBlockerRectsXZ[i]))
                {
                    return true;
                }
            }
            return false;
        }

        /// <summary>
        /// Tính toán xem một mục tiêu (targetHero) có nằm trong tầm nhìn hợp lệ của phe người chơi (viewerTeamId) hay không.
        /// Kết hợp: Tầm nhìn Tướng + Đồng đội (bị chắn bởi Tường & Bụi cỏ) + Tầm nhìn Mắt Kiểm Soát [Phím 5] (Xuyên tường & Soi bụi!).
        /// </summary>
        public bool IsTargetVisibleToTeam(
            HeroCharacter target,
            int viewerTeamId,
            IReadOnlyList<HeroCharacter> allHeroes,
            IReadOnlyList<ScoutWard3D> activeWards)
        {
            if (target == null || target.IsDead) return false;
            if (target.TeamId == viewerTeamId) return true; // Đồng đội luôn thấy nhau

            // 1. Kiểm tra Mắt Kiểm Soát [Phím 5] của phe mình (Soi xuyên tường + Soi bụi cỏ trong bán kính 12m!)
            if (activeWards != null)
            {
                for (int i = 0; i < activeWards.Count; i++)
                {
                    ScoutWard3D ward = activeWards[i];
                    if (ward == null || !GodotObject.IsInstanceValid(ward) || ward.IsExpired) continue;
                    if (ward.OwnerTeamId != viewerTeamId) continue;

                    if (PlanarConstraintXZ.DistanceXZ(ward.GlobalPosition, target.GlobalPosition) <= ward.VisionRadius)
                    {
                        return true; // Mắt Kiểm Soát đã phát hiện mục tiêu sau tường / trong bụi!
                    }
                }
            }

            // 2. Kiểm tra tầm nhìn trực tiếp (Line-of-Sight) từ các Tướng thuộc phe viewerTeamId
            if (allHeroes != null)
            {
                for (int i = 0; i < allHeroes.Count; i++)
                {
                    HeroCharacter ally = allHeroes[i];
                    if (ally == null || ally.IsDead || ally.TeamId != viewerTeamId) continue;

                    float dist = PlanarConstraintXZ.DistanceXZ(ally.GlobalPosition, target.GlobalPosition);
                    if (dist > HeroVisionRange) continue;

                    // Nếu mục tiêu đang nấp trong Bụi Cỏ và không bị lộ bởi Mắt -> chỉ thấy khi đứng sát <= 3.2m
                    if (target.InStealthBush && dist > 3.2f) continue;

                    // Nếu đang bay dù trên cao (Y > 2.5m) thì không bị tường thấp dưới đất che khuất
                    if (ally.IsParachuteGliding || target.IsParachuteGliding) return true;

                    // Kiểm tra có bị Tường / Tòa nhà chắn tầm nhìn (Line-of-Sight) hay không
                    if (!IsLineOfSightBlockedByWall(ally.GlobalPosition, target.GlobalPosition))
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        private static bool SegmentIntersectsRect(Vector2 p1, Vector2 p2, Rect2 r)
        {
            // Kiểm tra nhanh Bounding Box
            float minX = Mathf.Min(p1.X, p2.X);
            float maxX = Mathf.Max(p1.X, p2.X);
            float minY = Mathf.Min(p1.Y, p2.Y);
            float maxY = Mathf.Max(p1.Y, p2.Y);

            if (maxX < r.Position.X || minX > r.End.X || maxY < r.Position.Y || minY > r.End.Y)
                return false;

            Vector2 tl = r.Position;
            Vector2 tr = new(r.End.X, r.Position.Y);
            Vector2 br = r.End;
            Vector2 bl = new(r.Position.X, r.End.Y);

            return SegmentsIntersect(p1, p2, tl, tr)
                || SegmentsIntersect(p1, p2, tr, br)
                || SegmentsIntersect(p1, p2, br, bl)
                || SegmentsIntersect(p1, p2, bl, tl);
        }

        private static bool SegmentsIntersect(Vector2 a, Vector2 b, Vector2 c, Vector2 d)
        {
            float ccw1 = Ccw(a, b, c);
            float ccw2 = Ccw(a, b, d);
            float ccw3 = Ccw(c, d, a);
            float ccw4 = Ccw(c, d, b);
            return ((ccw1 > 0f && ccw2 < 0f) || (ccw1 < 0f && ccw2 > 0f))
                && ((ccw3 > 0f && ccw4 < 0f) || (ccw3 < 0f && ccw4 > 0f));
        }

        private static float Ccw(Vector2 a, Vector2 b, Vector2 c)
            => (b.X - a.X) * (c.Y - a.Y) - (b.Y - a.Y) * (c.X - a.X);
    }
}
