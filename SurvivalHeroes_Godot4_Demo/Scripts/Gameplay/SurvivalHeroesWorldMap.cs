// ----------------------------------------------------------------------------
// SurvivalHeroesWorldMap.cs
// Tái hiện Bản đồ Thế giới "Survival Heroes" từ trên nhìn xuống (Top-Down Map)
// đúng theo ảnh chụp màn hình gốc:
//   - Đủ 20 Địa Danh Tiếng Việt (Quảng Trường Trung Tâm, Tàng Kim Trấn,
//     Giáo Đường Huyết Sắc, Nơi Lưu Đày, Nhà Giam, Vườn Hoa, Bộ Lạc Man Tộc,
//     Di Tích Thất Lạc, Di Tích Tiên Tổ, Vực Sâu Hoang Vu, Khu Xưởng, Khu Nhà Ở,
//     Đường Mòn U Ám, Khu Chợ, Địa Lao, Phế Thôn, Đường Ray, Mỏ Kho Báu,
//     Trạm Gác, Mê Cung Hài Cốt, Nghĩa Địa, Thần Điện Hắc Ám).
//   - Hệ thống Đường Bay Khinh Khí Cầu + Nút "NHẢY" Dù chọn điểm rơi đầu trận.
//   - Giao diện Bản Đồ Chiến Thuật [Phím M] có:
//       + Đánh dấu tọa độ (Click chuột lên bản đồ để cắm cờ giống ViiShiin)
//       + Bảng phải: "Xem vị trí Rồng" + "Xem vị trí Cửa hàng thần bí (Lều 1, 2, 3)"
//   - 3 Cửa Hàng Thần Bí 3D (MysteryShop3D : ISwitchable) & Boss Rồng Cổ Đại.
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using System;
    using System.Collections.Generic;
    using Godot;
    using Photon.Pun.GodotBridge;
    using SurvivalHeroes.Core.Events;
    using SurvivalHeroes.Core.Interfaces;
    using SurvivalHeroes.Gameplay.World;

    public sealed class MapPoiDefinition
    {
        public string Name { get; init; }
        public Vector3 Center3D { get; init; }
        public Vector2 FootprintSize { get; init; }
        public bool HasGoldenChest { get; init; }
        public int MysteryShopIndex { get; init; } // 0 = không có, 1..3 = Lều Thần Bí 1..3
        public bool IsCircularTemple { get; init; }
        public int BuildingCount { get; init; }
    }

    /// <summary>
    /// Cửa Hàng Thần Bí (Lều 1, 2, 3 trên bản đồ) - Thực thi ISwitchable (Dependency Inversion).
    /// Khi người chơi đi vào hoặc bấm [F]/[B], nhận trang bị Thần Thoại (+Sát thương, +Giáp Hoàng Kim, +Tốc chạy).
    /// </summary>
    public partial class MysteryShopTent3D : Node3D, ISwitchable
    {
        public int ShopNumber { get; private set; }
        public string LocationName { get; private set; }
        public bool IsActive { get; private set; } = true;

        private float _restockTimer;
        private MeshInstance3D _tentRoof;
        private Label3D _label;
        private float _animTime;

        public void Initialize(int shopNumber, string locationName, Vector3 worldPos)
        {
            ShopNumber = shopNumber;
            LocationName = locationName;
            GlobalPosition = worldPos;

            // Đế lều phát sáng màu lam ngọc
            MeshInstance3D basePad = new();
            CylinderMesh padMesh = new() { TopRadius = 2.6f, BottomRadius = 2.8f, Height = 0.14f };
            StandardMaterial3D padMat = new()
            {
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded,
                AlbedoColor = new Color(0.15f, 0.78f, 1.0f, 0.42f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha
            };
            basePad.Mesh = padMesh;
            basePad.MaterialOverride = padMat;
            AddChild(basePad);

            // Thân lều trắng xanh đặc trưng của Cửa Hàng Thần Bí
            MeshInstance3D tentBody = new();
            CylinderMesh bodyCyl = new() { TopRadius = 1.8f, BottomRadius = 2.1f, Height = 1.8f, RadialSegments = 8 };
            tentBody.Mesh = bodyCyl;
            tentBody.MaterialOverride = new StandardMaterial3D { AlbedoColor = new Color(0.85f, 0.92f, 1.0f) };
            tentBody.Position = new Vector3(0f, 0.9f, 0f);
            AddChild(tentBody);

            // Mái lều chóp nón màu xanh lam phát sáng
            _tentRoof = new MeshInstance3D();
            CylinderMesh roofCone = new() { TopRadius = 0.05f, BottomRadius = 2.35f, Height = 1.5f, RadialSegments = 8 };
            StandardMaterial3D roofMat = new()
            {
                AlbedoColor = new Color(0.18f, 0.62f, 0.98f),
                EmissionEnabled = true,
                Emission = new Color(0.12f, 0.55f, 0.95f),
                EmissionEnergyMultiplier = 1.1f
            };
            _tentRoof.Mesh = roofCone;
            _tentRoof.MaterialOverride = roofMat;
            _tentRoof.Position = new Vector3(0f, 2.55f, 0f);
            AddChild(_tentRoof);

            _label = new Label3D
            {
                Text = $"🎪 CỬA HÀNG THẦN BÍ #{shopNumber} ({locationName})",
                PixelSize = 0.012f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                Modulate = new Color(0.45f, 0.92f, 1.0f),
                OutlineSize = 0,
                Position = new Vector3(0f, 3.8f, 0f)
            };
            AddChild(_label);
        }

        public void Activate(Node3D activatorNode)
        {
            if (!IsActive || activatorNode is not HeroCharacter activator || activator.IsDead) return;
            IsActive = false;
            _restockTimer = 20.0f;

            // Ban trang bị Thần Bí theo số thứ tự Lều (#1, #2, #3)
            WeaponType bonusWeapon = ShopNumber switch
            {
                1 => WeaponType.Greatsword,
                2 => WeaponType.FireStaff,
                _ => WeaponType.RangerBow
            };

            activator.MaxHealth = 280f;
            activator.HealAndBuffFromShrine(120f);
            activator.Shield = 120f;

            if (activator.CurrentWeaponType == WeaponType.Unarmed && activator.PhotonView != null)
            {
                activator.PhotonView.RPC(nameof(HeroCharacter.RpcEquipWeapon), Photon.Pun.RpcTarget.All, (int)bonusWeapon);
            }

            if (_label != null)
            {
                _label.Text = $"🎪 CỬA HÀNG THẦN BÍ #{ShopNumber}\n(Đang nhập hàng mới: {_restockTimer:F0}s)";
            }

            ArenaEventBus.RaiseFloatingText(activator.GlobalPosition, $"🎪 ĐỒ THẦN BÍ #{ShopNumber}! +120 GIÁP HOÀNG KIM", new Color(0.3f, 0.95f, 1f));
            ArenaEventBus.RaiseAnnouncement($"🎪 {activator.HeroName} đã ghé Cửa Hàng Thần Bí #{ShopNumber} tại [{LocationName}]!");
        }

        public void Deactivate()
        {
            IsActive = true;
            if (_label != null)
            {
                _label.Text = $"🎪 CỬA HÀNG THẦN BÍ #{ShopNumber}\n({LocationName})\n[Chạm vào nhận Giáp Hoàng Kim + Vũ Khí]";
            }
        }

        public override void _Process(double delta)
        {
            float dt = (float)delta;
            _animTime += dt * 2.0f;
            if (_tentRoof != null)
            {
                _tentRoof.Rotation = new Vector3(0f, _animTime * 0.4f, 0f);
            }

            if (IsActive)
            {
                if (ArenaGameManager.Instance != null)
                {
                    foreach (HeroCharacter hero in ArenaGameManager.Instance.AllHeroes)
                    {
                        if (hero == null || hero.IsDead) continue;
                        float d = new Vector2(hero.GlobalPosition.X - GlobalPosition.X, hero.GlobalPosition.Z - GlobalPosition.Z).Length();
                        if (d <= 2.9f)
                        {
                            Activate(hero);
                            break;
                        }
                    }
                }
            }
            else
            {
                _restockTimer -= dt;
                if (_restockTimer <= 0f)
                {
                    Deactivate();
                }
            }
        }
    }

    /// <summary>
    /// Vẽ trực tiếp Bản đồ Chiến thuật 2D từ trên nhìn xuống (Top-Down Map Control)
    /// giống hệt giao diện trong ảnh Survival Heroes:
    ///   - Nền xanh đen + Đường phố đá + Khu nhà mái vàng + Sông nước
    ///   - 20 Địa danh tiếng Việt
    ///   - Đường bay chấm bi + Máy bay/Khinh khí cầu + Các chấm nhảy dù đỏ
    ///   - Vòng tròn đỏ khu vực giao tranh & Vòng Bo
    ///   - 3 biểu tượng Lều Thần Bí (#1, #2, #3) + 4 Hòm Hoàng Kim
    ///   - Click chuột trái vào bất kỳ đâu trên bản đồ để "Đánh dấu tọa độ" (hoặc nhảy dù tới đó!)
    /// </summary>
    public partial class TacticalMapCanvas : Control
    {
        public bool ShowDetailedLabels { get; set; } = true;
        public float WorldHalfExtent { get; set; } = 85f;
        public Vector2? MarkedPingWorldPos { get; private set; }
        public string MarkedPingLocationName { get; private set; } = "TÀNG KIM TRẤN";

        public Vector2 FlightStartWorld { get; set; } = new(-80f, -38f);
        public Vector2 FlightEndWorld { get; set; } = new(80f, -38f);
        public float FlightProgress01 { get; set; } = 0.45f;
        public bool IsAirshipPhase { get; set; } = true;

        public Action<Vector3, string> OnMapLocationClicked;

        private readonly List<Vector2> _redDropDots = new()
        {
            new Vector2(18f, -40f), new Vector2(22f, -37f), new Vector2(26f, -35f),
            new Vector2(15f, -43f), new Vector2(28f, -32f), new Vector2(33f, -29f),
            new Vector2(36f, -34f), new Vector2(24f, -46f), new Vector2(31f, -42f),
            new Vector2(40f, -36f), new Vector2(12f, -38f), new Vector2(20f, -31f)
        };

        public Vector2 WorldToMapPixel(Vector2 worldXZ)
        {
            float nx = (worldXZ.X + WorldHalfExtent) / (WorldHalfExtent * 2f);
            float ny = (worldXZ.Y + WorldHalfExtent) / (WorldHalfExtent * 2f);
            return new Vector2(Mathf.Clamp(nx, 0.02f, 0.98f) * Size.X, Mathf.Clamp(ny, 0.02f, 0.98f) * Size.Y);
        }

        public Vector2 MapPixelToWorld(Vector2 localPixel)
        {
            float nx = Mathf.Clamp(localPixel.X / MathF.Max(1f, Size.X), 0f, 1f);
            float ny = Mathf.Clamp(localPixel.Y / MathF.Max(1f, Size.Y), 0f, 1f);
            return new Vector2(
                (nx * 2f - 1f) * WorldHalfExtent,
                (ny * 2f - 1f) * WorldHalfExtent
            );
        }

        public override void _GuiInput(InputEvent @event)
        {
            if (@event is InputEventMouseButton mb && mb.Pressed && mb.ButtonIndex == MouseButton.Left)
            {
                Vector2 worldXZ = MapPixelToWorld(mb.Position);
                MarkedPingWorldPos = worldXZ;
                MarkedPingLocationName = SurvivalHeroesWorldBuilder.FindClosestPoiName(new Vector3(worldXZ.X, 0f, worldXZ.Y));
                OnMapLocationClicked?.Invoke(new Vector3(worldXZ.X, 0f, worldXZ.Y), MarkedPingLocationName);
                QueueRedraw();
            }
        }

        public override void _Draw()
        {
            Rect2 fullRect = new(Vector2.Zero, Size);

            // 1. Nền bản đồ xanh đen sẫm (Dark Slate Blue giống ảnh gốc)
            DrawRect(fullRect, new Color(0.11f, 0.14f, 0.21f, 0.97f), true);

            // 2. Sông & Hồ nước phía Đông Bắc (gần Bộ Lạc Man Tộc / Di Tích Thất Lạc) và Tây Nam
            DrawPolygon(new Vector2[]
            {
                WorldToMapPixel(new Vector2(10f, -82f)),
                WorldToMapPixel(new Vector2(44f, -82f)),
                WorldToMapPixel(new Vector2(52f, -45f)),
                WorldToMapPixel(new Vector2(18f, -48f))
            }, new Color[] { new(0.16f, 0.34f, 0.48f, 0.85f) });

            DrawPolygon(new Vector2[]
            {
                WorldToMapPixel(new Vector2(-82f, 52f)),
                WorldToMapPixel(new Vector2(-42f, 58f)),
                WorldToMapPixel(new Vector2(-48f, 82f)),
                WorldToMapPixel(new Vector2(-82f, 82f))
            }, new Color[] { new(0.16f, 0.34f, 0.48f, 0.85f) });

            // 3. Hệ thống đường cái lát đá nối các khu vực (Road network)
            Color roadColor = new(0.24f, 0.28f, 0.36f, 0.9f);
            (Vector2 a, Vector2 b)[] mainRoads =
            {
                (new(-75f, -72f), new(0f, 0f)),
                (new(0f, 0f), new(62f, 68f)),
                (new(-72f, 65f), new(0f, 0f)),
                (new(0f, 0f), new(55f, -62f)),
                (new(-75f, -5f), new(72f, -5f)),
                (new(-5f, -78f), new(-5f, 78f)),
                (new(-50f, 35f), new(45f, 35f)),
                (new(-45f, -42f), new(50f, -42f))
            };
            foreach (var (a, b) in mainRoads)
            {
                DrawLine(WorldToMapPixel(a), WorldToMapPixel(b), roadColor, 4.5f);
            }

            // 4. Vẽ từng Khu nhà mái vàng nâu & Tên 20 Địa Danh Tiếng Việt
            Font defaultFont = ThemeDB.FallbackFont;
            Color roofGold = new(0.68f, 0.59f, 0.42f, 0.95f);
            Color roofBorder = new(0.22f, 0.18f, 0.12f, 0.95f);

            foreach (MapPoiDefinition poi in SurvivalHeroesWorldBuilder.AllPois)
            {
                Vector2 centerPx = WorldToMapPixel(new Vector2(poi.Center3D.X, poi.Center3D.Z));

                if (poi.IsCircularTemple)
                {
                    // Thần Điện Hắc Ám góc Đông Nam: Kiến trúc tròn cổ đại
                    DrawCircle(centerPx, 22f, new Color(0.45f, 0.39f, 0.28f, 0.9f));
                    DrawArc(centerPx, 22f, 0f, Mathf.Tau, 28, roofGold, 2.5f);
                }
                else
                {
                    // Vẽ cụm các tòa nhà mái vàng xung quanh tâm địa danh
                    int count = poi.BuildingCount;
                    for (int i = 0; i < count; i++)
                    {
                        float angle = i * (Mathf.Tau / MathF.Max(1, count)) + 0.35f;
                        float radiusPx = (i % 2 == 0) ? 12f : 21f;
                        Vector2 bPos = centerPx + new Vector2(MathF.Cos(angle), MathF.Sin(angle)) * radiusPx;
                        Vector2 bSize = (poi.Name == "QUẢNG TRƯỜNG TRUNG TÂM" && i == 0)
                            ? new Vector2(26f, 20f)
                            : new Vector2(10f, 8f);

                        Rect2 bRect = new(bPos - bSize * 0.5f, bSize);
                        DrawRect(bRect, roofGold, true);
                        DrawRect(bRect, roofBorder, false, 1.2f);
                    }
                }

                // Biểu tượng Hòm Hoàng Kim (nếu có)
                if (poi.HasGoldenChest)
                {
                    Vector2 chestPx = centerPx + new Vector2(0f, -16f);
                    DrawRect(new Rect2(chestPx - new Vector2(7f, 5f), new Vector2(14f, 10f)), new Color(1.0f, 0.82f, 0.2f), true);
                    DrawRect(new Rect2(chestPx - new Vector2(7f, 5f), new Vector2(14f, 10f)), Colors.Black, false, 1.2f);
                }

                // Biểu tượng Lều Cửa Hàng Thần Bí (#1, #2, #3)
                if (poi.MysteryShopIndex > 0)
                {
                    Vector2 tentPx = centerPx + new Vector2(12f, -10f);
                    DrawCircle(tentPx, 10f, new Color(0.15f, 0.65f, 0.98f));
                    DrawArc(tentPx, 10f, 0f, Mathf.Tau, 16, Colors.White, 1.8f);
                    if (defaultFont != null)
                    {
                        DrawString(defaultFont, tentPx + new Vector2(-4f, 4f), poi.MysteryShopIndex.ToString(), HorizontalAlignment.Center, -1, 12, Colors.Yellow);
                    }
                }
            }

            // 5. Vòng Tròn Đỏ quanh TÀNG KIM TRẤN (giống hệt ảnh chụp!) + Vòng Bo Sinh Tồn hiện tại
            Vector2 tangKimTranPx = WorldToMapPixel(new Vector2(-48f, 28f));
            DrawArc(tangKimTranPx, 58f, 0f, Mathf.Tau, 48, new Color(0.95f, 0.15f, 0.15f, 0.9f), 2.5f);

            if (SafeZoneController.Instance != null)
            {
                Vector2 zoneCenterPx = WorldToMapPixel(new Vector2(SafeZoneController.Instance.ZoneCenter.X, SafeZoneController.Instance.ZoneCenter.Z));
                float radiusPx = (SafeZoneController.Instance.CurrentRadius / (WorldHalfExtent * 2f)) * Size.X;
                DrawArc(zoneCenterPx, MathF.Max(12f, radiusPx), 0f, Mathf.Tau, 64, new Color(0.25f, 0.9f, 1.0f, 0.85f), 2.0f);
            }

            // 6. Đường Bay Khinh Khí Cầu (Dotted Line ngang phía Bắc) + Các chấm đỏ nhảy dù
            Vector2 fStartPx = WorldToMapPixel(FlightStartWorld);
            Vector2 fEndPx = WorldToMapPixel(FlightEndWorld);
            int dots = 42;
            for (int i = 0; i <= dots; i++)
            {
                Vector2 p = fStartPx.Lerp(fEndPx, i / (float)dots);
                DrawCircle(p, 1.8f, new Color(0.95f, 0.95f, 0.95f, 0.85f));
            }

            foreach (Vector2 rDot in _redDropDots)
            {
                Vector2 rp = WorldToMapPixel(rDot);
                DrawCircle(rp, 4.2f, new Color(0.92f, 0.18f, 0.22f, 0.92f));
            }

            // Biểu tượng Tàu Bay phát sáng vàng trên đường bay
            Vector2 airshipPx = fStartPx.Lerp(fEndPx, FlightProgress01);
            DrawCircle(airshipPx, 9f, new Color(1.0f, 0.82f, 0.22f, 0.95f));
            DrawCircle(airshipPx, 5f, new Color(1.0f, 0.35f, 0.1f, 1.0f));

            // 7. Vẽ Tên 22 Địa Danh Tiếng Việt nổi bật trên bản đồ lớn (Tắt trên Minimap nhỏ để mượt 144 FPS!)
            if (ShowDetailedLabels && defaultFont != null)
            {
                foreach (MapPoiDefinition poi in SurvivalHeroesWorldBuilder.AllPois)
                {
                    Vector2 centerPx = WorldToMapPixel(new Vector2(poi.Center3D.X, poi.Center3D.Z));
                    Vector2 textPos = centerPx + new Vector2(-poi.Name.Length * 3.1f, 5f);
                    DrawString(defaultFont, textPos + new Vector2(1f, 1f), poi.Name, HorizontalAlignment.Left, -1, 11, Colors.Black);
                    DrawString(defaultFont, textPos, poi.Name, HorizontalAlignment.Left, -1, 11, Colors.White);
                }
            }

            // 8. Vẽ vị trí của Người chơi & các mục tiêu nằm trong Tầm nhìn Fog of War trên bản đồ
            if (ArenaGameManager.Instance != null)
            {
                foreach (HeroCharacter h in ArenaGameManager.Instance.AllHeroes)
                {
                    if (h == null || h.IsDead || !h.IsVisibleInFogOfWar) continue;
                    Vector2 hPx = WorldToMapPixel(new Vector2(h.GlobalPosition.X, h.GlobalPosition.Z));
                    if (h.IsLocalHumanPlayer)
                    {
                        DrawCircle(hPx, 7.5f, new Color(0.2f, 1.0f, 0.45f));
                        DrawArc(hPx, 9.5f, 0f, Mathf.Tau, 16, Colors.White, 2.0f);
                    }
                    else
                    {
                        DrawCircle(hPx, 5.0f, new Color(1.0f, 0.45f, 0.2f, 0.9f));
                    }
                }
            }

            // 9. Hiển thị thẻ "ViiShiin | Đánh dấu tọa độ" giống hệt ảnh chụp khi người chơi click lên bản đồ
            if (MarkedPingWorldPos.HasValue && defaultFont != null)
            {
                Vector2 pingPx = WorldToMapPixel(MarkedPingWorldPos.Value);
                DrawCircle(pingPx, 6f, new Color(0.25f, 0.9f, 1.0f));
                DrawArc(pingPx, 11f, 0f, Mathf.Tau, 20, new Color(0.25f, 0.9f, 1.0f), 2.0f);

                Rect2 tagRect = new(pingPx + new Vector2(-95f, -44f), new Vector2(195f, 30f));
                DrawRect(tagRect, new Color(0.12f, 0.17f, 0.26f, 0.94f), true);
                DrawRect(tagRect, new Color(0.65f, 0.78f, 0.95f, 0.9f), false, 1.5f);
                DrawString(defaultFont, tagRect.Position + new Vector2(8f, 19f), $"📍 ViiShiin: Đánh dấu [{MarkedPingLocationName}]", HorizontalAlignment.Left, -1, 11, new Color(0.95f, 0.95f, 1.0f));
            }

            // 10. Khung viền Vàng Kim cổ điển quanh bản đồ
            DrawRect(fullRect, new Color(0.76f, 0.65f, 0.38f, 0.95f), false, 3.0f);
        }
    }

    public static class SurvivalHeroesWorldBuilder
    {
        /// <summary>
        /// Danh sách đầy đủ 22 Địa Danh chính xác từ bản đồ gốc tiếng Việt của Survival Heroes
        /// được quy hoạch trên mặt phẳng 2.5D rộng 170m x 170m (-85m .. +85m).
        /// </summary>
        public static readonly List<MapPoiDefinition> AllPois = new()
        {
            // --- TÂY BẮC (NORTH-WEST) ---
            new MapPoiDefinition { Name = "NƠI LƯU ĐÀY",           Center3D = new(-58f, 0f, -66f), FootprintSize = new(18f, 16f), HasGoldenChest = true,  BuildingCount = 5 },
            new MapPoiDefinition { Name = "NHÀ GIAM",              Center3D = new(-42f, 0f, -46f), FootprintSize = new(18f, 16f), MysteryShopIndex = 3,   BuildingCount = 6 },
            new MapPoiDefinition { Name = "VƯỜN HOA",              Center3D = new(-18f, 0f, -38f), FootprintSize = new(16f, 14f), BuildingCount = 5 },

            // --- BẮC & ĐÔNG BẮC (NORTH & NORTH-EAST) ---
            new MapPoiDefinition { Name = "BỘ LẠC MAN TỘC",        Center3D = new(-4f,  0f, -68f), FootprintSize = new(18f, 14f), HasGoldenChest = true,  BuildingCount = 5 },
            new MapPoiDefinition { Name = "DI TÍCH THẤT LẠC",      Center3D = new(18f,  0f, -48f), FootprintSize = new(18f, 16f), BuildingCount = 5 },
            new MapPoiDefinition { Name = "DI TÍCH TIÊN TỔ",       Center3D = new(48f,  0f, -52f), FootprintSize = new(18f, 16f), HasGoldenChest = true,  BuildingCount = 5 },
            new MapPoiDefinition { Name = "VỰC SÂU HOANG VU",      Center3D = new(46f,  0f, -20f), FootprintSize = new(18f, 16f), BuildingCount = 4 },

            // --- TÂY & TRUNG TÂM (WEST & CENTER) ---
            new MapPoiDefinition { Name = "GIÁO ĐƯỜNG HUYẾT SẮC",  Center3D = new(-64f, 0f, -14f), FootprintSize = new(20f, 18f), BuildingCount = 6 },
            new MapPoiDefinition { Name = "KHU XƯỞNG",             Center3D = new(-28f, 0f, -12f), FootprintSize = new(18f, 16f), BuildingCount = 6 },
            new MapPoiDefinition { Name = "QUẢNG TRƯỜNG TRUNG TÂM",Center3D = new(-4f,  0f, -6f),  FootprintSize = new(26f, 24f), BuildingCount = 8 },
            new MapPoiDefinition { Name = "KHU NHÀ Ở",             Center3D = new(16f,  0f,  4f),  FootprintSize = new(18f, 16f), BuildingCount = 6 },
            new MapPoiDefinition { Name = "ĐƯỜNG MÒN U ÁM",        Center3D = new(38f,  0f, 14f),  FootprintSize = new(16f, 14f), BuildingCount = 4 },

            // --- TÂY NAM (SOUTH-WEST) ---
            new MapPoiDefinition { Name = "TÀNG KIM TRẤN",         Center3D = new(-48f, 0f, 28f),  FootprintSize = new(26f, 24f), MysteryShopIndex = 2,   BuildingCount = 9 },
            new MapPoiDefinition { Name = "ĐƯỜNG RAY",             Center3D = new(-58f, 0f, 70f),  FootprintSize = new(18f, 14f), BuildingCount = 4 },
            new MapPoiDefinition { Name = "MỎ KHO BÁU",            Center3D = new(-36f, 0f, 58f),  FootprintSize = new(18f, 16f), BuildingCount = 5 },

            // --- NAM & ĐÔNG NAM (SOUTH & SOUTH-EAST) ---
            new MapPoiDefinition { Name = "KHU CHỢ",               Center3D = new(-14f, 0f, 18f),  FootprintSize = new(18f, 16f), BuildingCount = 6 },
            new MapPoiDefinition { Name = "ĐỊA LAO",               Center3D = new(-6f,  0f, 38f),  FootprintSize = new(18f, 16f), HasGoldenChest = true,  BuildingCount = 5 },
            new MapPoiDefinition { Name = "TRẠM GÁC",              Center3D = new(-14f, 0f, 54f),  FootprintSize = new(16f, 14f), BuildingCount = 5 },
            new MapPoiDefinition { Name = "MÊ CUNG HÀI CỐT",       Center3D = new(-8f,  0f, 70f),  FootprintSize = new(18f, 16f), BuildingCount = 6 },
            new MapPoiDefinition { Name = "PHẾ THÔN",              Center3D = new(24f,  0f, 34f),  FootprintSize = new(18f, 16f), BuildingCount = 5 },
            new MapPoiDefinition { Name = "NGHĨA ĐỊA",             Center3D = new(12f,  0f, 58f),  FootprintSize = new(18f, 16f), MysteryShopIndex = 1,   BuildingCount = 5 },
            new MapPoiDefinition { Name = "THẦN ĐIỆN HẮC ÁM",      Center3D = new(46f,  0f, 68f),  FootprintSize = new(22f, 22f), IsCircularTemple = true, BuildingCount = 6 }
        };

        public static string FindClosestPoiName(Vector3 worldPos)
        {
            string bestName = "QUẢNG TRƯỜNG TRUNG TÂM";
            float bestDistSq = float.MaxValue;
            foreach (MapPoiDefinition poi in AllPois)
            {
                float dSq = worldPos.DistanceSquaredTo(poi.Center3D);
                if (dSq < bestDistSq)
                {
                    bestDistSq = dSq;
                    bestName = poi.Name;
                }
            }
            return bestName;
        }
    }
}
