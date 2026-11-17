// ----------------------------------------------------------------------------
// ArenaGameManager.cs
// Quản lý toàn bộ Thế giới & Đấu trường Survival Heroes 2.5D (170x170m):
//   - Tái hiện đầy đủ 22 Địa Danh Tiếng Việt từ bản đồ gốc (Quảng Trường Trung Tâm,
//     Tàng Kim Trấn, Giáo Đường Huyết Sắc, Thần Điện Hắc Ám, Nhà Giam, Địa Lao...)
//   - Giao diện Bản Đồ Từ Trên Nhìn Xuống [Phím M] giống hệt ảnh chụp:
//       + Click chuột lên bản đồ: "ViiShiin | Đánh dấu tọa độ"
//       + Đường bay Khinh Khí Cầu + Nút tròn xanh "🪂 NHẢY" dù chọn điểm rơi
//       + Bảng phải: "Xem vị trí Rồng" + "Xem vị trí Cửa hàng thần bí (Lều 1, 2, 3)"
//   - 3 Cửa Hàng Thần Bí 3D (MysteryShopTent3D : ISwitchable)
//   - Thùng Thuốc Nổ (IDamageable + IExplodable), Trụ Hồi Máu (ISwitchable)
//   - Object Pool cho Đạn, Vòng nổ AOE & Số sát thương nảy 3D (Floating Text)
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using System;
    using System.Collections.Generic;
    using Godot;
    using Photon.Pun;
    using Photon.Pun.GodotBridge;
    using Photon.Realtime;
    using SurvivalHeroes.Patterns;

    public partial class ArenaGameManager : Node3D
    {
        public static ArenaGameManager Instance { get; private set; }

        public const byte EventClaimWeaponLoot = 10;

        public readonly List<HeroCharacter> AllHeroes = new();
        public readonly List<WeaponLootPickup3D> WeaponCrates = new();
        public readonly List<StealthBushZone3D> Bushes = new();
        public readonly List<ExplosiveBarrel3D> ExplosiveBarrels = new();
        public readonly List<HealingShrineSwitch3D> HealingShrines = new();
        public readonly List<MysteryShopTent3D> MysteryShops = new();
        public readonly List<JungleMonster3D> JungleMonsters = new();
        public readonly List<ScoutWard3D> ScoutWards = new();

        public HeroCharacter LocalHero { get; private set; }

        private readonly List<PooledProjectile3D> _projectilePool = new();
        private readonly List<PooledAoeRing3D> _aoeRingPool = new();
        private GodotNodePool<PooledFloatingText3D> _floatingTextPool;

        private GodotCullArea _cullArea;
        private GodotSafeZoneTimer _safeZoneTimer;
        private Camera3D _isoCamera;
        private MeshInstance3D _safeZoneRingMesh;
        private MeshInstance3D _aimReticleMesh;

        // UI & Tactical Map Overlay Elements
        private Label _photonStatusLabel;
        private Label _zoneTimerLabel;
        private Label _cullAreaInfoLabel;
        private Label _currentPoiLabel;
        private Label _heroStatsLabel;
        private Label _skillBarLabel;
        private Label _killFeedLabel;
        private Label _dragonStatusLabel;
        private LineEdit _appIdInput;

        private Control _fullMapModalRoot;
        private TacticalMapCanvas _bigMapCanvas;
        private TacticalMapCanvas _miniMapCanvas;
        private Button _jumpParachuteBtn;
        private bool _mKeyWasPressed;
        private float _airshipFlightTimer;
        private float _dragonSpawnCountdown = 90f;
        private bool _dragonSpawned;
        private readonly Queue<string> _killFeedMessages = new();

        public override void _EnterTree()
        {
            Instance = this;
        }

        public override void _Ready()
        {
            if (GodotPhotonService.Instance != null)
            {
                GodotPhotonService.Instance.OnCustomNetworkEvent += OnCustomPhotonEventReceived;
            }
            ArenaEventBus.OnFloatingTextRequested += SpawnFloatingTextFromPool;
            ArenaEventBus.OnArenaAnnouncement += AddKillFeed;

            BuildEnvironmentAndLighting();
            BuildArenaFloorAndCullGrid();
            BuildSurvivalHeroes22TownsAndRoads();
            BuildObstaclesAndBushes();
            BuildWeaponLootCrates();
            BuildObjectPools();
            BuildSafeZoneVisualAndCamera();
            SpawnHeroes();
            BuildMobaHudAndTacticalMap();

            AddKillFeed("⚡ Bản đồ SURVIVAL HEROES (22 Địa Danh + 3 Cửa Hàng Thần Bí) đã sẵn sàng!");
            AddKillFeed("🗺️ Click lên Bản đồ để Đánh Dấu Tọa Độ -> Bấm [🪂 NHẢY] (hoặc phím [M] để đóng/mở Bản đồ)!");
        }

        public override void _ExitTree()
        {
            if (GodotPhotonService.Instance != null)
            {
                GodotPhotonService.Instance.OnCustomNetworkEvent -= OnCustomPhotonEventReceived;
            }
            ArenaEventBus.OnFloatingTextRequested -= SpawnFloatingTextFromPool;
            ArenaEventBus.OnArenaAnnouncement -= AddKillFeed;
        }

        private void BuildEnvironmentAndLighting()
        {
            DirectionalLight3D sun = new()
            {
                RotationDegrees = new Vector3(-54f, -35f, 0f),
                LightEnergy = 1.3f,
                ShadowEnabled = true
            };
            AddChild(sun);

            WorldEnvironment worldEnv = new();
            Godot.Environment env = new()
            {
                BackgroundMode = Godot.Environment.BGMode.Color,
                BackgroundColor = new Color(0.07f, 0.10f, 0.16f),
                AmbientLightSource = Godot.Environment.AmbientSource.Color,
                AmbientLightColor = new Color(0.36f, 0.43f, 0.56f),
                AmbientLightEnergy = 0.88f,
                GlowEnabled = true,
                GlowIntensity = 0.55f
            };
            worldEnv.Environment = env;
            AddChild(worldEnv);
        }

        private void BuildArenaFloorAndCullGrid()
        {
            // 1. Khởi tạo GodotCullArea (170x170m, 3x3 = 9 InterestGroup Cells) và GodotSafeZoneTimer
            _cullArea = new GodotCullArea { Name = "GodotCullArea", MapSize = new Vector2(170f, 170f), GridColumns = 3, GridRows = 3 };
            AddChild(_cullArea);

            _safeZoneTimer = new GodotSafeZoneTimer { Name = "GodotSafeZoneTimer" };
            AddChild(_safeZoneTimer);

            // 2. Nền đá xanh đen 170x170m đúng tông màu bản đồ Survival Heroes
            StaticBody3D floorBody = new() { Name = "ArenaFloor" };
            CollisionShape3D floorCol = new() { Shape = new BoxShape3D { Size = new Vector3(170f, 1f, 170f) }, Position = new Vector3(0f, -0.5f, 0f) };
            floorBody.AddChild(floorCol);

            MeshInstance3D floorMesh = new()
            {
                Mesh = new BoxMesh { Size = new Vector3(170f, 1f, 170f) },
                Position = new Vector3(0f, -0.5f, 0f),
                MaterialOverride = new StandardMaterial3D { AlbedoColor = new Color(0.14f, 0.18f, 0.25f), Roughness = 0.92f }
            };
            floorBody.AddChild(floorMesh);
            AddChild(floorBody);

            // 3. Vẽ đường kẻ phân chia 9 Ô Lưới Mạng (Photon CullArea Cells #1..#9)
            StandardMaterial3D lineMat = new()
            {
                AlbedoColor = new Color(0.3f, 0.65f, 0.95f, 0.25f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded
            };

            float[] splitCoords = { -28.3f, 28.3f };
            foreach (float coord in splitCoords)
            {
                AddChild(new MeshInstance3D
                {
                    Mesh = new BoxMesh { Size = new Vector3(0.22f, 0.03f, 170f) },
                    Position = new Vector3(coord, 0.02f, 0f),
                    MaterialOverride = lineMat
                });
                AddChild(new MeshInstance3D
                {
                    Mesh = new BoxMesh { Size = new Vector3(170f, 0.03f, 0.22f) },
                    Position = new Vector3(0f, 0.02f, coord),
                    MaterialOverride = lineMat
                });
            }
        }

        /// <summary>
        /// Dựng trực tiếp 22 Khu vực (POIs) có tên tiếng Việt, các tòa nhà mái vàng nâu,
        /// đường đá cổ, sông hồ và 3 Cửa Hàng Thần Bí (#1 Nghĩa Địa, #2 Tàng Kim Trấn, #3 Nhà Giam)
        /// trong không gian 3D 2.5D!
        /// </summary>
        private void BuildSurvivalHeroes22TownsAndRoads()
        {
            Node3D worldRoot = new() { Name = "SurvivalHeroesWorld22Pois" };
            AddChild(worldRoot);

            // 1. Sông nước Đông Bắc (gần Bộ Lạc Man Tộc & Di Tích Thất Lạc) & Tây Nam (Đường Ray)
            StandardMaterial3D waterMat = new()
            {
                AlbedoColor = new Color(0.14f, 0.38f, 0.58f, 0.85f),
                Roughness = 0.15f,
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha
            };
            MeshInstance3D neRiver = new()
            {
                Mesh = new BoxMesh { Size = new Vector3(36f, 0.08f, 38f) },
                Position = new Vector3(28f, 0.03f, -62f),
                MaterialOverride = waterMat
            };
            worldRoot.AddChild(neRiver);

            MeshInstance3D swRiver = new()
            {
                Mesh = new BoxMesh { Size = new Vector3(34f, 0.08f, 26f) },
                Position = new Vector3(-64f, 0.03f, 68f),
                MaterialOverride = waterMat
            };
            worldRoot.AddChild(swRiver);

            // 2. Đường phố lát đá nối giữa các thị trấn
            StandardMaterial3D roadMat = new() { AlbedoColor = new Color(0.24f, 0.28f, 0.36f), Roughness = 0.88f };
            (Vector3 center, Vector3 size, float rotYDeg)[] roads =
            {
                (new Vector3(0f, 0.02f, -6f),   new Vector3(150f, 0.04f, 4.2f), 0f),
                (new Vector3(-4f, 0.02f, 0f),   new Vector3(4.2f, 0.04f, 150f), 0f),
                (new Vector3(0f, 0.02f, 0f),    new Vector3(165f, 0.04f, 3.8f), 42f),
                (new Vector3(0f, 0.02f, 0f),    new Vector3(165f, 0.04f, 3.8f), -42f),
                (new Vector3(-5f, 0.02f, 32f),  new Vector3(110f, 0.04f, 3.5f), 0f),
                (new Vector3(0f, 0.02f, -44f),  new Vector3(110f, 0.04f, 3.5f), 0f)
            };
            foreach (var r in roads)
            {
                MeshInstance3D roadMesh = new()
                {
                    Mesh = new BoxMesh { Size = r.size },
                    Position = r.center,
                    RotationDegrees = new Vector3(0f, r.rotYDeg, 0f),
                    MaterialOverride = roadMat
                };
                worldRoot.AddChild(roadMesh);
            }

            // 3. Vật liệu cho các tòa nhà mái vàng nâu & nền gạch từng khu vực
            StandardMaterial3D plazaMat = new() { AlbedoColor = new Color(0.32f, 0.30f, 0.26f), Roughness = 0.85f };
            StandardMaterial3D wallMat = new() { AlbedoColor = new Color(0.42f, 0.38f, 0.32f), Roughness = 0.8f };
            StandardMaterial3D goldenRoofMat = new()
            {
                AlbedoColor = new Color(0.74f, 0.62f, 0.38f),
                Roughness = 0.55f
            };

            foreach (MapPoiDefinition poi in SurvivalHeroesWorldBuilder.AllPois)
            {
                Node3D poiNode = new() { Name = poi.Name, Position = poi.Center3D };
                worldRoot.AddChild(poiNode);

                // Nền sân đá của khu vực
                if (poi.IsCircularTemple)
                {
                    MeshInstance3D templeDisk = new()
                    {
                        Mesh = new CylinderMesh { TopRadius = 11f, BottomRadius = 11.5f, Height = 0.14f, RadialSegments = 24 },
                        Position = new Vector3(0f, 0.06f, 0f),
                        MaterialOverride = plazaMat
                    };
                    poiNode.AddChild(templeDisk);
                }
                else
                {
                    MeshInstance3D townCourtyard = new()
                    {
                        Mesh = new BoxMesh { Size = new Vector3(poi.FootprintSize.X, 0.1f, poi.FootprintSize.Y) },
                        Position = new Vector3(0f, 0.04f, 0f),
                        MaterialOverride = plazaMat
                    };
                    poiNode.AddChild(townCourtyard);
                }

                // Biển tên Địa Danh 3D lơ lửng phía trên khu vực
                string extraTag = poi.MysteryShopIndex > 0
                    ? $"\n[🎪 CỬA HÀNG THẦN BÍ #{poi.MysteryShopIndex}]"
                    : (poi.HasGoldenChest ? "\n[📦 KHU VỰC HÒM HOÀNG KIM]" : "");

                Label3D banner = new()
                {
                    Text = $"🏛️ {poi.Name}{extraTag}",
                    PixelSize = 0.0155f,
                    Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                    Modulate = poi.MysteryShopIndex > 0
                        ? new Color(0.45f, 0.95f, 1.0f)
                        : (poi.HasGoldenChest ? new Color(1.0f, 0.88f, 0.35f) : new Color(0.96f, 0.92f, 0.82f)),
                    OutlineSize = 10,
                    Position = new Vector3(0f, 4.8f, 0f)
                };
                poiNode.AddChild(banner);

                // Tạo cụm kiến trúc nhà mái vàng xung quanh rìa khu vực (để giữa sân rộng cho Tướng giao tranh)
                int houseCount = Math.Min(4, poi.BuildingCount);
                for (int i = 0; i < houseCount; i++)
                {
                    float angle = i * (Mathf.Tau / houseCount) + 0.78f;
                    float dist = (poi.FootprintSize.X * 0.42f);
                    Vector3 localHousePos = new(MathF.Cos(angle) * dist, 0f, MathF.Sin(angle) * dist);

                    StaticBody3D building = new() { Position = localHousePos };
                    Vector3 wallSize = new(3.4f, 2.2f, 2.8f);
                    building.AddChild(new CollisionShape3D
                    {
                        Shape = new BoxShape3D { Size = wallSize },
                        Position = new Vector3(0f, 1.1f, 0f)
                    });
                    building.AddChild(new MeshInstance3D
                    {
                        Mesh = new BoxMesh { Size = wallSize },
                        Position = new Vector3(0f, 1.1f, 0f),
                        MaterialOverride = wallMat
                    });
                    // Mái nhà màu vàng nâu đặc trưng trên bản đồ nhìn từ trên xuống
                    building.AddChild(new MeshInstance3D
                    {
                        Mesh = new BoxMesh { Size = new Vector3(wallSize.X + 0.5f, 0.45f, wallSize.Z + 0.5f) },
                        Position = new Vector3(0f, 2.35f, 0f),
                        MaterialOverride = goldenRoofMat
                    });
                    poiNode.AddChild(building);
                }

                // Nếu khu vực này có Cửa Hàng Thần Bí (#1, #2, #3), dựng Lều Thần Bí ngay tại trung tâm!
                if (poi.MysteryShopIndex > 0)
                {
                    MysteryShopTent3D shop = new();
                    AddChild(shop);
                    shop.Initialize(poi.MysteryShopIndex, poi.Name, poi.Center3D + new Vector3(2.8f, 0f, 0f));
                    MysteryShops.Add(shop);
                }
            }
        }

        private void BuildObstaclesAndBushes()
        {
            // Bụi cỏ tàng hình MOBA phân bố tại các địa danh chiến lược
            Vector3[] bushCoords =
            {
                new(-8f, 0f, -2f),   new(6f, 0f, -2f),     // Quảng Trường Trung Tâm
                new(-44f, 0f, 24f),  new(-52f, 0f, 32f),   // Tàng Kim Trấn
                new(-38f, 0f, -42f), new(14f, 0f, -44f),   // Nhà Giam & Di Tích Thất Lạc
                new(-2f, 0f, 34f),   new(16f, 0f, 54f),    // Địa Lao & Nghĩa Địa
                new(42f, 0f, 64f),   new(-58f, 0f, -12f),  // Thần Điện Hắc Ám & Giáo Đường Huyết Sắc
                new(-24f, 0f, -10f), new(14f, 0f, 8f)      // Khu Xưởng & Khu Nhà Ở
            };

            foreach (Vector3 bp in bushCoords)
            {
                StealthBushZone3D bush = new();
                AddChild(bush);
                bush.Initialize(bp, 3.2f);
                Bushes.Add(bush);
            }

            // Thùng Thuốc Nổ 2.5D (IDamageable + IExplodable) tại các ngã tư giao tranh
            Vector3[] barrelCoords =
            {
                new(-9f, 0f, -8f),   new(5f, 0f, -8f),     // Quảng Trường Trung Tâm
                new(-45f, 0f, 28f),  new(-51f, 0f, 25f),   // Tàng Kim Trấn
                new(-14f, 0f, 16f),  new(14f, 0f, 2f),     // Khu Chợ & Khu Nhà Ở
                new(-40f, 0f, -48f), new(10f, 0f, 56f)     // Nhà Giam & Nghĩa Địa
            };
            foreach (Vector3 brPos in barrelCoords)
            {
                ExplosiveBarrel3D barrel = new();
                AddChild(barrel);
                barrel.Initialize(brPos);
                ExplosiveBarrels.Add(barrel);
            }

            // Trụ Hồi Phục Cổ Đại (ISwitchable) tại Vườn Hoa, Quảng Trường, Địa Lao, Phế Thôn
            Vector3[] shrineCoords =
            {
                new(0f, 0f, -11f),
                new(-18f, 0f, -36f),
                new(-6f, 0f, 35f),
                new(22f, 0f, 32f)
            };
            foreach (Vector3 shPos in shrineCoords)
            {
                HealingShrineSwitch3D shrine = new();
                AddChild(shrine);
                shrine.Initialize(shPos);
                HealingShrines.Add(shrine);
            }

            // Quái Rừng & Boss Rồng Cổ Đại (JungleMonster3D : IDamageable)
            (string title, Vector3 pos, bool isDragon)[] monsterSpawns =
            {
                ("Hắc Long Cổ Đại [Di Tích Thất Lạc]", new Vector3(24f, 0f, -52f), true),
                ("Sói Tinh Rừng Rậm [Vườn Hoa]",      new Vector3(-22f, 0f, -34f), false),
                ("Golem Đá Cổ [Tàng Kim Trấn]",       new Vector3(-42f, 0f, 22f),  false),
                ("Ác Quỷ Hầm Ngục [Địa Lao]",         new Vector3(-2f,  0f, 42f),  false),
                ("Oan Hồn Cổ Mộ [Nghĩa Địa]",         new Vector3(16f,  0f, 62f),  false),
                ("Chiến Binh Xương [Mê Cung Hài Cốt]",new Vector3(-12f, 0f, 68f),  false),
                ("Thủ Vệ Hắc Ám [Thần Điện Hắc Ám]",  new Vector3(42f,  0f, 62f),  false)
            };
            foreach (var ms in monsterSpawns)
            {
                JungleMonster3D monster = new();
                AddChild(monster);
                monster.Initialize(ms.title, ms.pos, ms.isDragon);
                JungleMonsters.Add(monster);
            }
        }

        private void BuildWeaponLootCrates()
        {
            // Rải Hộp Vũ Khí & Hòm Hoàng Kim khắp 22 Địa Danh trên bản đồ
            int lootId = 1;
            WeaponType[] cycle = { WeaponType.Greatsword, WeaponType.RangerBow, WeaponType.FireStaff };

            // 3 vũ khí sát bên Quảng Trường Trung Tâm để người chơi test ngay lập tức
            (Vector3 pos, WeaponType w)[] starterCrates =
            {
                (new Vector3(-5.5f, 0f, -3.5f), WeaponType.Greatsword),
                (new Vector3(3.5f, 0f, -3.5f),  WeaponType.RangerBow),
                (new Vector3(-1.0f, 0f, 3.5f),  WeaponType.FireStaff)
            };
            foreach (var sc in starterCrates)
            {
                WeaponLootPickup3D pickup = new();
                AddChild(pickup);
                pickup.Initialize(lootId++, sc.w, sc.pos);
                WeaponCrates.Add(pickup);
            }

            // Mỗi Địa Danh trong 22 POI đều có Hộp Vũ khí cho người nhảy dù xuống nhặt!
            for (int i = 0; i < SurvivalHeroesWorldBuilder.AllPois.Count; i++)
            {
                MapPoiDefinition poi = SurvivalHeroesWorldBuilder.AllPois[i];
                WeaponType wt = cycle[i % cycle.Length];
                WeaponLootPickup3D pickup = new();
                AddChild(pickup);
                pickup.Initialize(lootId++, wt, poi.Center3D + new Vector3(-2.2f, 0f, 1.8f));
                WeaponCrates.Add(pickup);
            }
        }

        private void BuildObjectPools()
        {
            Node3D poolContainer = new() { Name = "CombatObjectPool" };
            AddChild(poolContainer);

            for (int i = 0; i < 50; i++)
            {
                PooledProjectile3D proj = new();
                poolContainer.AddChild(proj);
                _projectilePool.Add(proj);
            }

            for (int i = 0; i < 28; i++)
            {
                PooledAoeRing3D ring = new();
                poolContainer.AddChild(ring);
                _aoeRingPool.Add(ring);
            }

            _floatingTextPool = new GodotNodePool<PooledFloatingText3D>(
                createFunc: () =>
                {
                    var ft = new PooledFloatingText3D();
                    poolContainer.AddChild(ft);
                    return ft;
                },
                onGet: ft => ft.Visible = true,
                onRelease: ft => ft.Visible = false,
                prewarmCount: 35
            );
        }

        private void SpawnFloatingTextFromPool(Vector3 worldPos, string text, Color color)
        {
            if (_floatingTextPool == null) return;
            PooledFloatingText3D popup = _floatingTextPool.Get();
            popup.SpawnPopup(worldPos, text, color, item => _floatingTextPool.Release(item));
        }

        private void BuildSafeZoneVisualAndCamera()
        {
            _safeZoneRingMesh = new MeshInstance3D();
            CylinderMesh zoneCyl = new() { TopRadius = 1.0f, BottomRadius = 1.0f, Height = 0.06f, RadialSegments = 64 };
            StandardMaterial3D zoneMat = new()
            {
                AlbedoColor = new Color(0.15f, 0.85f, 1.0f, 0.14f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded
            };
            _safeZoneRingMesh.Mesh = zoneCyl;
            _safeZoneRingMesh.MaterialOverride = zoneMat;
            _safeZoneRingMesh.Position = new Vector3(0f, 0.03f, 0f);
            AddChild(_safeZoneRingMesh);

            _aimReticleMesh = new MeshInstance3D();
            CylinderMesh aimCyl = new() { TopRadius = 0.55f, BottomRadius = 0.55f, Height = 0.08f, RadialSegments = 20 };
            StandardMaterial3D aimMat = new()
            {
                AlbedoColor = new Color(1.0f, 0.9f, 0.2f, 0.7f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded
            };
            _aimReticleMesh.Mesh = aimCyl;
            _aimReticleMesh.MaterialOverride = aimMat;
            AddChild(_aimReticleMesh);

            _isoCamera = new Camera3D
            {
                Position = new Vector3(-4f, 22f, 9f),
                RotationDegrees = new Vector3(-55f, 0f, 0f),
                Fov = 50f,
                Current = true
            };
            AddChild(_isoCamera);
        }

        private void SpawnHeroes()
        {
            // Khởi tạo Tướng người chơi chính tại Quảng Trường Trung Tâm (hoặc nhảy dù tới bất kỳ đâu!)
            LocalHero = new HeroCharacter { Name = "Hero_Player" };
            AddChild(LocalHero);
            LocalHero.SetupHero(
                viewId: 1001,
                heroName: "ViiShiin (BẠN)",
                isLocalHuman: true,
                isAiBot: false,
                teamId: 1,
                spawnPos: new Vector3(-4f, 0f, -6f),
                bodyColor: new Color(0.18f, 0.65f, 1.0f)
            );
            AllHeroes.Add(LocalHero);

            // Khởi tạo 7 AI Survivors phân bố tại các địa danh nổi tiếng trên bản đồ
            (string name, Vector3 pos, Color col, int team)[] botConfigs =
            {
                ("Bot_Arthur [Tàng Kim Trấn]",     new Vector3(-48f, 0f, 28f),  new Color(0.95f, 0.3f, 0.25f), 2),
                ("Bot_Sylvanas [Quảng Trường]",    new Vector3(8f, 0f, -8f),    new Color(0.95f, 0.55f, 0.2f), 3),
                ("Bot_Ignis [Khu Xưởng]",          new Vector3(-28f, 0f, -12f), new Color(0.85f, 0.25f, 0.85f), 4),
                ("Bot_Valkyrie [Khu Chợ]",         new Vector3(-14f, 0f, 18f),  new Color(0.95f, 0.8f, 0.2f),  5),
                ("Bot_Kael [Nhà Giam]",            new Vector3(-42f, 0f, -46f), new Color(1.0f, 0.35f, 0.55f), 6),
                ("Bot_zephyr [Địa Lao]",           new Vector3(-6f, 0f, 38f),   new Color(0.35f, 0.95f, 0.75f), 7),
                ("Bot_Malakor [Thần Điện Hắc Ám]", new Vector3(46f, 0f, 68f),   new Color(0.9f, 0.2f, 0.45f),  8)
            };

            for (int i = 0; i < botConfigs.Length; i++)
            {
                HeroCharacter bot = new() { Name = $"Bot_{i + 1}" };
                AddChild(bot);
                bot.SetupHero(
                    viewId: 2001 + i,
                    heroName: botConfigs[i].name,
                    isLocalHuman: false,
                    isAiBot: true,
                    teamId: botConfigs[i].team,
                    spawnPos: botConfigs[i].pos,
                    bodyColor: botConfigs[i].col
                );
                AllHeroes.Add(bot);
            }
        }

        /// <summary>
        /// Dựng cả HUD Chiến đấu + Minimap góc trái + Bảng Bản Đồ Từ Trên Nhìn Xuống (Full Tactical Map [M])
        /// tái hiện 100% bố cục trong ảnh chụp màn hình Survival Heroes của bạn!
        /// </summary>
        private void BuildMobaHudAndTacticalMap()
        {
            CanvasLayer hud = new() { Name = "MobaHudLayer" };
            AddChild(hud);

            // ================================================================
            // A. MINIMAP TỪ TRÊN NHÌN XUỐNG Ở GÓC TRÊN BÊN TRÁI + NÚT MỞ BẢN ĐỒ LỚN [M]
            // ================================================================
            PanelContainer miniMapHolder = new()
            {
                Position = new Vector2(12, 10),
                Size = new Vector2(215, 242)
            };
            VBoxContainer miniMapVBox = new();
            miniMapHolder.AddChild(miniMapVBox);

            _miniMapCanvas = new TacticalMapCanvas
            {
                CustomMinimumSize = new Vector2(205, 200),
                Size = new Vector2(205, 200)
            };
            _miniMapCanvas.OnMapLocationClicked += (worldPos, poiName) =>
            {
                ParachuteDropHeroTo(worldPos, poiName);
            };
            miniMapVBox.AddChild(_miniMapCanvas);

            Button openBigMapBtn = new()
            {
                Text = "🗺️ BẢN ĐỒ LỚN & NHẢY DÙ [Phím M]",
                CustomMinimumSize = new Vector2(205, 30)
            };
            openBigMapBtn.Pressed += () => ToggleFullTacticalMap();
            miniMapVBox.AddChild(openBigMapBtn);
            hud.AddChild(miniMapHolder);

            // ================================================================
            // B. BẢNG THÔNG TIN TRẠNG THÁI MẠNG PHOTON & ĐỊA DANH HIỆN TẠI
            // ================================================================
            PanelContainer topPanel = new()
            {
                Position = new Vector2(236, 10),
                Size = new Vector2(575, 118)
            };
            VBoxContainer topBox = new();
            topPanel.AddChild(topBox);

            _currentPoiLabel = new Label { Modulate = new Color(1.0f, 0.92f, 0.35f) };
            _photonStatusLabel = new Label { Modulate = new Color(0.45f, 1.0f, 0.65f) };
            _cullAreaInfoLabel = new Label { Modulate = new Color(0.6f, 0.88f, 1.0f) };
            _zoneTimerLabel = new Label { Modulate = new Color(1.0f, 0.80f, 0.35f) };

            topBox.AddChild(_currentPoiLabel);
            topBox.AddChild(_photonStatusLabel);
            topBox.AddChild(_cullAreaInfoLabel);
            topBox.AddChild(_zoneTimerLabel);
            hud.AddChild(topPanel);

            // ================================================================
            // C. KHUNG NHẬP PHOTON APPID & KILL FEED BÊN PHẢI
            // ================================================================
            PanelContainer cloudPanel = new()
            {
                Position = new Vector2(820, 10),
                Size = new Vector2(534, 78)
            };
            VBoxContainer cloudBox = new();
            cloudPanel.AddChild(cloudBox);

            cloudBox.AddChild(new Label
            {
                Text = "🌐 KẾT NỐI PHOTON CLOUD (Hoặc chạy Local Loopback Sim):"
            });

            HBoxContainer inputRow = new();
            _appIdInput = new LineEdit
            {
                PlaceholderText = "Dán Photon Realtime AppId...",
                CustomMinimumSize = new Vector2(290, 30)
            };
            Button connectBtn = new() { Text = "Kết Nối Cloud" };
            connectBtn.Pressed += () =>
            {
                GodotPhotonService.Instance?.ConnectToPhotonCloud(_appIdInput.Text, "asia");
                AddKillFeed($"🌐 Đang kết nối Photon Cloud với AppId: {_appIdInput.Text}");
            };
            Button simBtn = new() { Text = "Local Sim" };
            simBtn.Pressed += () =>
            {
                GodotPhotonService.Instance?.SwitchToLocalSimulationMode();
                AddKillFeed("🔄 Đã chuyển về chế độ Photon Loopback Room + AI Survivors.");
            };

            inputRow.AddChild(_appIdInput);
            inputRow.AddChild(connectBtn);
            inputRow.AddChild(simBtn);
            cloudBox.AddChild(inputRow);
            hud.AddChild(cloudPanel);

            PanelContainer feedPanel = new()
            {
                Position = new Vector2(820, 96),
                Size = new Vector2(534, 125)
            };
            _killFeedLabel = new Label { Modulate = new Color(0.95f, 0.92f, 0.85f) };
            feedPanel.AddChild(_killFeedLabel);
            hud.AddChild(feedPanel);

            // ================================================================
            // D. THANH KỸ NĂNG MOBA DƯỚI ĐÁY MÀN HÌNH
            // ================================================================
            PanelContainer bottomPanel = new()
            {
                Position = new Vector2(210, 628),
                Size = new Vector2(950, 128)
            };
            VBoxContainer bottomBox = new();
            bottomPanel.AddChild(bottomBox);

            _heroStatsLabel = new Label { Modulate = new Color(0.35f, 1.0f, 0.55f) };
            _skillBarLabel = new Label { Modulate = Colors.White };
            Label helpLabel = new()
            {
                Text = "🎮 [M] Mở Bản Đồ Nhảy Dù | [WASD] Di chuyển | [LMB / Q / W / E] Tung chiêu | [F] Nhặt Vũ khí | [1/2/3] Đổi Kiếm/Cung/Trượng",
                Modulate = new Color(0.75f, 0.85f, 1.0f)
            };

            bottomBox.AddChild(_heroStatsLabel);
            bottomBox.AddChild(_skillBarLabel);
            bottomBox.AddChild(helpLabel);
            hud.AddChild(bottomPanel);

            // ================================================================
            // E. GIAO DIỆN BẢN ĐỒ CHIẾN THUẬT TỪ TRÊN NHÌN XUỐNG (GIỐNG ẢNH GỐC 100%)
            // ================================================================
            _fullMapModalRoot = new PanelContainer
            {
                Position = new Vector2(65, 22),
                Size = new Vector2(1235, 595),
                Visible = true // Mở sẵn đầu trận để người chơi chọn điểm Nhảy Dù giống hệt ảnh!
            };
            hud.AddChild(_fullMapModalRoot);

            HBoxContainer modalSplit = new() { CustomMinimumSize = new Vector2(1220, 580) };
            _fullMapModalRoot.AddChild(modalSplit);

            // 1. Cột bên trái: Thanh Zoom (+/-) + Logo SURVIVAL HEROES
            VBoxContainer leftZoomCol = new() { CustomMinimumSize = new Vector2(54, 560) };
            Label zoomMinus = new() { Text = "  ⊝\n  |\n  |\n  |\n  ⊕", Modulate = new Color(0.8f, 0.88f, 1.0f) };
            leftZoomCol.AddChild(zoomMinus);
            modalSplit.AddChild(leftZoomCol);

            // 2. Cột chính giữa: Bản đồ vuông từ trên nhìn xuống (565x565)
            _bigMapCanvas = new TacticalMapCanvas
            {
                CustomMinimumSize = new Vector2(565, 565),
                Size = new Vector2(565, 565)
            };
            _bigMapCanvas.OnMapLocationClicked += (worldPos, poiName) =>
            {
                if (_jumpParachuteBtn != null)
                {
                    _jumpParachuteBtn.Text = $"🪂 NHẢY XUỐNG\n[{poiName}]";
                }
                AddKillFeed($"📍 ViiShiin đã đánh dấu tọa độ: [{poiName}] ({worldPos.X:F0}, {worldPos.Z:F0})");
            };
            modalSplit.AddChild(_bigMapCanvas);

            // 3. Cột giữa-phải: Nút Tròn Xanh "🪂 NHẢY" giống hệt ảnh chụp!
            VBoxContainer jumpBtnCol = new()
            {
                CustomMinimumSize = new Vector2(145, 560),
                Alignment = BoxContainer.AlignmentMode.Center
            };
            _jumpParachuteBtn = new Button
            {
                Text = "🪂 NHẢY\n[TÀNG KIM TRẤN]",
                CustomMinimumSize = new Vector2(138, 92),
                Modulate = new Color(0.35f, 0.85f, 1.0f)
            };
            _jumpParachuteBtn.Pressed += () =>
            {
                Vector2 targetXZ = _bigMapCanvas.MarkedPingWorldPos ?? new Vector2(-48f, 28f); // Mặc định Tàng Kim Trấn
                string locName = _bigMapCanvas.MarkedPingLocationName;
                ParachuteDropHeroTo(new Vector3(targetXZ.X, 0f, targetXZ.Y), locName);
                _fullMapModalRoot.Visible = false;
            };
            jumpBtnCol.AddChild(_jumpParachuteBtn);

            Label clickHint = new()
            {
                Text = "👆 Click chuột lên\nBản đồ để chọn\n1 trong 22 Khu vực\nrồi bấm [NHẢY]!",
                HorizontalAlignment = HorizontalAlignment.Center,
                Modulate = new Color(0.95f, 0.90f, 0.55f)
            };
            jumpBtnCol.AddChild(clickHint);
            modalSplit.AddChild(jumpBtnCol);

            // 4. Cột bên phải: "Xem vị trí Rồng" + "Xem vị trí Cửa hàng thần bí (Lều 1, 2, 3)" + Nút [X]
            VBoxContainer rightInfoCol = new() { CustomMinimumSize = new Vector2(430, 565) };
            modalSplit.AddChild(rightInfoCol);

            HBoxContainer headerRow = new();
            Label mapHeaderTitle = new()
            {
                Text = "🗺️ BẢN ĐỒ CHIẾN THUẬT SURVIVAL HEROES   ",
                Modulate = new Color(1.0f, 0.88f, 0.45f)
            };
            Button closeXBtn = new()
            {
                Text = " ✕ ĐÓNG [M] ",
                CustomMinimumSize = new Vector2(110, 34)
            };
            closeXBtn.Pressed += () => _fullMapModalRoot.Visible = false;
            headerRow.AddChild(mapHeaderTitle);
            headerRow.AddChild(closeXBtn);
            rightInfoCol.AddChild(headerRow);

            // Hộp 1: Xem vị trí Rồng
            PanelContainer dragonBox = new() { CustomMinimumSize = new Vector2(420, 68) };
            VBoxContainer dragonVBox = new();
            dragonBox.AddChild(dragonVBox);
            dragonVBox.AddChild(new Label
            {
                Text = "🐉 Xem vị trí Rồng",
                Modulate = new Color(0.85f, 0.92f, 1.0f)
            });
            _dragonStatusLabel = new Label
            {
                Text = "   Chưa làm mới Rồng (Sắp xuất hiện tại Sông Đông Bắc)",
                Modulate = new Color(0.72f, 0.78f, 0.86f)
            };
            dragonVBox.AddChild(_dragonStatusLabel);
            rightInfoCol.AddChild(dragonBox);

            // Hộp 2: Xem vị trí Cửa hàng thần bí (Lều 1, 2, 3 kèm trang bị Hoàng Kim)
            PanelContainer shopBox = new() { CustomMinimumSize = new Vector2(420, 395) };
            VBoxContainer shopVBox = new();
            shopBox.AddChild(shopVBox);

            shopVBox.AddChild(new Label
            {
                Text = "🎪 Xem vị trí Cửa hàng thần bí                          ▲",
                Modulate = new Color(0.45f, 0.95f, 1.0f)
            });

            (int num, string town, Vector3 pos, string items)[] mysteryConfigs =
            {
                (1, "NGHĨA ĐỊA (Đông Nam)",     new Vector3(12f, 0f, 58f),  "⚔️ Đại Kiếm Hoàng Kim | 🛡️ Giáp Chiến Thần | 👢 Giày Tốc Hành"),
                (2, "TÀNG KIM TRẤN (Tây Nam)",  new Vector3(-48f, 0f, 28f), "🔮 Trượng Hỏa Long    | 🛡️ Áo Choàng Hư Không | 💍 Nhẫn Ma Thuật"),
                (3, "NHÀ GIAM (Tây Bắc)",       new Vector3(-42f, 0f, -46f),"🏹 Cung Tuyệt Mệnh    | 🛡️ Giáp Ánh Sáng      | ⚡ Huy Hiệu Cuồng Nộ")
            };

            foreach (var mc in mysteryConfigs)
            {
                PanelContainer rowPanel = new() { CustomMinimumSize = new Vector2(405, 96) };
                VBoxContainer rowVBox = new();
                rowPanel.AddChild(rowVBox);

                rowVBox.AddChild(new Label
                {
                    Text = $"🎪 Lều #{mc.num} — Tại [{mc.town}]",
                    Modulate = new Color(1.0f, 0.88f, 0.35f)
                });
                rowVBox.AddChild(new Label
                {
                    Text = $"   Trang bị bán: {mc.items}",
                    Modulate = new Color(0.90f, 0.94f, 1.0f)
                });

                Button jumpToShopBtn = new()
                {
                    Text = $"🪂 Nhảy Dù thẳng tới Cửa Hàng Thần Bí #{mc.num} ({mc.town})"
                };
                Vector3 targetPos = mc.pos;
                string targetTown = mc.town;
                jumpToShopBtn.Pressed += () =>
                {
                    ParachuteDropHeroTo(targetPos, targetTown);
                    _fullMapModalRoot.Visible = false;
                };
                rowVBox.AddChild(jumpToShopBtn);
                shopVBox.AddChild(rowPanel);
            }

            rightInfoCol.AddChild(shopBox);

            // Thẻ tên người chơi góc dưới phải giống ảnh ("ViiShiin 📍")
            rightInfoCol.AddChild(new Label
            {
                Text = "👤 Người chơi: ViiShiin  📍 (Bấm phím [M] bất kỳ lúc nào để Bật/Tắt Bản Đồ)",
                Modulate = new Color(0.45f, 0.92f, 1.0f)
            });
        }

        public void ToggleFullTacticalMap()
        {
            if (_fullMapModalRoot == null) return;
            _fullMapModalRoot.Visible = !_fullMapModalRoot.Visible;
            _bigMapCanvas?.QueueRedraw();
        }

        public void ParachuteDropHeroTo(Vector3 targetWorldPos, string locationName)
        {
            if (LocalHero == null) return;
            Vector3 clamped = new(
                Mathf.Clamp(targetWorldPos.X, -82f, 82f),
                0f,
                Mathf.Clamp(targetWorldPos.Z, -82f, 82f)
            );

            LocalHero.StartParachuteDrop(clamped);
            SpawnAoeRingEffect(clamped, 4.5f, new Color(0.25f, 0.9f, 1.0f), 0.75f);
            ArenaEventBus.RaiseFloatingText(clamped, $"🪂 ĐANG BAY DÙ XUỐNG: {locationName}!", new Color(0.35f, 0.95f, 1.0f));
            AddKillFeed($"🪂 ViiShiin đang bung dù lượn xuống [{locationName}] ({clamped.X:F0}, {clamped.Z:F0}) — Dùng WASD lái dù!");
        }

        public override void _Process(double delta)
        {
            float dt = (float)delta;

            // Phím tắt [M] để Bật / Tắt Bản Đồ Chiến Thuật từ trên nhìn xuống
            bool mPressed = Input.IsKeyPressed(Key.M);
            if (mPressed && !_mKeyWasPressed)
            {
                ToggleFullTacticalMap();
            }
            _mKeyWasPressed = mPressed;

            // Cập nhật tiến trình đường bay Khinh Khí Cầu trên Bản đồ
            _airshipFlightTimer = (_airshipFlightTimer + dt * 0.045f) % 1.0f;
            if (_bigMapCanvas != null)
            {
                _bigMapCanvas.FlightProgress01 = _airshipFlightTimer;
                if (_fullMapModalRoot != null && _fullMapModalRoot.Visible)
                {
                    _bigMapCanvas.QueueRedraw();
                }
            }
            if (_miniMapCanvas != null)
            {
                _miniMapCanvas.FlightProgress01 = _airshipFlightTimer;
                _miniMapCanvas.QueueRedraw();
            }

            // Đếm ngược làm mới Rồng Cổ Đại
            if (!_dragonSpawned)
            {
                _dragonSpawnCountdown -= dt;
                if (_dragonStatusLabel != null)
                {
                    if (_dragonSpawnCountdown > 0f)
                        _dragonStatusLabel.Text = $"   Chưa làm mới Rồng (Làm mới tại Di Tích Thất Lạc sau {_dragonSpawnCountdown:F0}s)";
                    else
                    {
                        _dragonSpawned = true;
                        _dragonStatusLabel.Text = "   🔥 RỒNG HẮC ÁM ĐÃ XUẤT HIỆN TẠI DI TÍCH THẤT LẠC!";
                        AddKillFeed("🐉 RỒNG CỔ ĐẠI đã xuất hiện tại khu vực [DI TÍCH THẤT LẠC]!");
                    }
                }
            }

            // Camera Isometric bám mượt theo Tướng của người chơi
            if (LocalHero != null && _isoCamera != null)
            {
                Vector3 desiredCamPos = LocalHero.GlobalPosition + new Vector3(0f, 22f, 15f);
                _isoCamera.GlobalPosition = _isoCamera.GlobalPosition.Lerp(desiredCamPos, dt * 7.5f);

                Vector3 mouseGround = GetMouseGroundPosition(LocalHero.GlobalPosition);
                if (_aimReticleMesh != null)
                {
                    _aimReticleMesh.GlobalPosition = new Vector3(mouseGround.X, 0.06f, mouseGround.Z);
                }
            }

            // Cập nhật bán kính Vòng Bo Sinh Tồn trên mặt đất
            if (_safeZoneTimer != null && _safeZoneRingMesh != null)
            {
                float r = MathF.Max(1.0f, _safeZoneTimer.CurrentRadius);
                _safeZoneRingMesh.Scale = new Vector3(r, 1f, r);
                _safeZoneRingMesh.GlobalPosition = new Vector3(_safeZoneTimer.ZoneCenter.X, 0.03f, _safeZoneTimer.ZoneCenter.Z);
            }

            UpdateHudTexts();
        }

        private void UpdateHudTexts()
        {
            if (LocalHero != null && _currentPoiLabel != null)
            {
                string currentTown = SurvivalHeroesWorldBuilder.FindClosestPoiName(LocalHero.GlobalPosition);
                _currentPoiLabel.Text = $"🏛️ KHU VỰC HIỆN TẠI: [{currentTown}] (Tọa độ: {LocalHero.GlobalPosition.X:F0}, {LocalHero.GlobalPosition.Z:F0}) | Bấm [M] mở Bản Đồ Nhảy Dù";
            }

            if (GodotPhotonService.Instance != null && _photonStatusLabel != null)
            {
                _photonStatusLabel.Text = $"📡 PHOTON: {GodotPhotonService.Instance.StatusMessage} | ServerTime: {GodotPhotonService.Instance.Time:F1}s | Views: {GodotPhotonService.Instance.GetAllViews().Count}";
            }

            if (_cullArea != null && _cullAreaInfoLabel != null)
            {
                string groups = string.Join(", ", _cullArea.ActiveInterestGroups);
                _cullAreaInfoLabel.Text = $"🗺️ CULL AREA (3x3 Grid 170x170m): Đang ở Ô #{_cullArea.LocalPlayerCellId} | Nhận gói tin từ Ô: [{groups}]";
            }

            if (_safeZoneTimer != null && _zoneTimerLabel != null && LocalHero != null)
            {
                bool outside = _safeZoneTimer.IsOutsideSafeZone(LocalHero.GlobalPosition);
                string warn = outside ? " ⚠️ [NGOÀI VÒNG BO - MẤT MÁU!]" : " ✅ [TRONG VÒNG AN TOÀN]";
                _zoneTimerLabel.Text = $"🌪️ VÒNG BO Phase {_safeZoneTimer.CurrentPhase}/4 | Bán kính: {_safeZoneTimer.CurrentRadius:F0}m -> {_safeZoneTimer.TargetRadius:F0}m ({_safeZoneTimer.RemainingPhaseSeconds:F0}s){warn}";
            }

            if (LocalHero != null && _heroStatsLabel != null && _skillBarLabel != null)
            {
                var w = LocalHero.CurrentLoadout;
                var p = LocalHero.Progression;
                string subW = WeaponLoadout.Get(p.SecondaryWeapon).Title;
                string stateName = LocalHero.StateMachine.CurrentState?.StateDisplayName ?? "Sẵn Sàng";
                _heroStatsLabel.Text =
                    $"⭐ Lv.{p.Level} | 💰 {p.Gold} Vàng | ⚔️ Chính: [{w.Title}] <-> [Tab] Phụ: [{subW}] | ❤️ {(int)LocalHero.Health}/{(int)LocalHero.MaxHealth} (+🛡️{(int)LocalHero.Shield}) | 🏆 {LocalHero.Kills} Kills | {stateName}\n" +
                    $"🎒 6 Ô GIÁP: {p.FormatArmorSummaryBar()}";

                string cd0 = LocalHero.CooldownBasic > 0f ? $"({LocalHero.CooldownBasic:F1}s)" : "[SẴN SÀNG]";
                string cdQ = LocalHero.CooldownQ > 0f ? $"({LocalHero.CooldownQ:F1}s)" : "[SẴN SÀNG]";
                string cdW = LocalHero.CooldownW > 0f ? $"({LocalHero.CooldownW:F1}s)" : "[SẴN SÀNG]";
                string cdE = LocalHero.CooldownE > 0f ? $"({LocalHero.CooldownE:F1}s)" : "[SẴN SÀNG]";
                string cdFlash = LocalHero.CooldownFlash > 0f ? $"({LocalHero.CooldownFlash:F0}s)" : "[SẴN SÀNG]";

                _skillBarLabel.Text =
                    $"[LMB] {w.BasicAttack.Name} {cd0} | [Q] {w.SkillQ.Name} {cdQ} | [W] {w.SkillW.Name} {cdW} | [E] {w.SkillE.Name} {cdE} | [Space] Tốc Biến {cdFlash} | [4] Bình Máu({p.HealthPotions}) | [5] Cắm Mắt({p.ScoutWards})";
            }
        }

        public Vector3 GetMouseGroundPosition(Vector3 fallbackPos)
        {
            if (_isoCamera == null) return fallbackPos;
            Vector2 mousePos = GetViewport().GetMousePosition();
            Vector3 rayOrigin = _isoCamera.ProjectRayOrigin(mousePos);
            Vector3 rayDir = _isoCamera.ProjectRayNormal(mousePos);

            Plane groundPlane = new(Vector3.Up, 0f);
            Vector3? hit = groundPlane.IntersectsRay(rayOrigin, rayDir);
            return hit ?? fallbackPos;
        }

        public void SpawnProjectile(int ownerViewId, Vector3 startPos, Vector3 dir, float speed, float maxDist, float damage, float explosionRadius, Color color, float lagSeconds)
        {
            foreach (PooledProjectile3D p in _projectilePool)
            {
                if (!p.IsActive)
                {
                    p.Activate(ownerViewId, startPos, dir, speed, maxDist, damage, explosionRadius, color, lagSeconds);
                    return;
                }
            }
        }

        public void SpawnAoeRingEffect(Vector3 pos, float radius, Color color, float duration)
        {
            foreach (PooledAoeRing3D ring in _aoeRingPool)
            {
                if (!ring.IsActive)
                {
                    ring.ShowEffect(pos, radius, color, duration);
                    return;
                }
            }
        }

        public void ApplyAoeDamage(int attackerViewId, Vector3 center, float radius, float damage)
        {
            foreach (HeroCharacter hero in AllHeroes)
            {
                if (hero == null || !GodotObject.IsInstanceValid(hero) || hero.IsDead) continue;
                if (hero.PhotonView != null && hero.PhotonView.ViewID == attackerViewId) continue;

                float dist = new Vector2(hero.GlobalPosition.X - center.X, hero.GlobalPosition.Z - center.Z).Length();
                if (dist <= radius + 0.55f)
                {
                    hero.TakeDamage(damage, attackerViewId);
                }
            }

            for (int i = 0; i < ExplosiveBarrels.Count; i++)
            {
                ExplosiveBarrel3D barrel = ExplosiveBarrels[i];
                if (barrel == null || barrel.IsDestroyed) continue;
                float dist = new Vector2(barrel.WorldPosition25D.X - center.X, barrel.WorldPosition25D.Z - center.Z).Length();
                if (dist <= radius + 0.6f)
                {
                    barrel.TakeDamage(damage, attackerViewId);
                }
            }

            for (int i = 0; i < JungleMonsters.Count; i++)
            {
                JungleMonster3D monster = JungleMonsters[i];
                if (monster == null || monster.IsDestroyed) continue;
                float dist = new Vector2(monster.WorldPosition25D.X - center.X, monster.WorldPosition25D.Z - center.Z).Length();
                if (dist <= radius + (monster.IsDragonBoss ? 1.8f : 0.9f))
                {
                    monster.TakeDamage(damage, attackerViewId);
                }
            }
        }

        public void PlaceScoutWard(int teamId, Vector3 worldPos)
        {
            ScoutWard3D ward = new();
            AddChild(ward);
            ward.Initialize(teamId, worldPos);
            ScoutWards.Add(ward);
            AddKillFeed("👁️ Đã cắm Mắt Kiểm Soát soi Bụi Cỏ Tàng Hình (bán kính 11m trong 25s)!");
        }

        public bool IsRevealedByEnemyWard(Vector3 pos, int heroTeamId)
        {
            ScoutWards.RemoveAll(w => w == null || !GodotObject.IsInstanceValid(w) || w.IsExpired);
            foreach (ScoutWard3D ward in ScoutWards)
            {
                if (ward.OwnerTeamId == heroTeamId) continue;
                if (PlanarPhysics25D.Distance25D(pos, ward.GlobalPosition) <= ward.VisionRadius)
                    return true;
            }
            return false;
        }

        public void NotifyMonsterSlain(JungleMonster3D monster, int killerViewId, int goldReward, float expReward)
        {
            GodotPhotonView killerPv = GodotPhotonService.Instance?.GetPhotonView(killerViewId);
            HeroCharacter killer = (killerPv != null && killerPv.GetParent() is HeroCharacter h) ? h : LocalHero;
            if (killer == null) return;

            killer.Progression.Gold += goldReward;
            bool lvUp = killer.Progression.GainExp(expReward);

            // Rơi ngẫu nhiên 1 món Giáp (Nếu diệt Rồng Cổ Đại nhận nguyên Set Thần Bí Vàng Cam!)
            if (monster.IsDragonBoss)
            {
                killer.Progression.HasDragonBuff = true;
                killer.Progression.EquipFullSet(ItemRarity.Legendary);
                killer.MaxHealth = 220f + killer.Progression.TotalBonusHp;
                killer.Health = killer.MaxHealth;
                killer.Shield = 150f;
                ArenaEventBus.RaiseFloatingText(killer.GlobalPosition, "🐉 BÙA RỒNG + FULL GIÁP THẦN BÍ!", Colors.Gold);
                AddKillFeed($"🐉 {killer.HeroName} đã hạ gục [{monster.MonsterTitle}] -> Nhận BÙA RỒNG + Full 6 Ô Giáp Thần Bí!");
            }
            else
            {
                Array slots = Enum.GetValues(typeof(ArmorSlot));
                ArmorSlot randomSlot = (ArmorSlot)slots.GetValue(GD.Randi() % slots.Length);
                ItemRarity rarity = (GD.Randi() % 100) < 35 ? ItemRarity.Epic : ItemRarity.Rare;
                EquipmentPiece piece = EquipmentPiece.Generate(randomSlot, rarity);
                killer.Progression.TryEquipBetterPiece(piece);
                killer.MaxHealth = 220f + killer.Progression.TotalBonusHp;
                killer.Health = MathF.Min(killer.MaxHealth, killer.Health + 35f);

                string lvTag = lvUp ? $" -> LÊN CẤP Lv.{killer.Progression.Level}!" : "";
                ArenaEventBus.RaiseFloatingText(killer.GlobalPosition, $"+{goldReward}💰 | Nhặt [{piece.Name}]{lvTag}", EquipmentPiece.GetRarityColor(rarity));
                AddKillFeed($"🐺 {killer.HeroName} tiêu diệt [{monster.MonsterTitle}] (+{goldReward}💰, nhặt {piece.Name}){lvTag}");
            }
        }

        public void TryPickupNearbyWeapon(HeroCharacter hero, bool manualPress)
        {
            float pickupRadius = manualPress ? 2.8f : 1.55f;
            foreach (WeaponLootPickup3D crate in WeaponCrates)
            {
                if (!crate.IsAvailable) continue;
                float dist = new Vector2(hero.GlobalPosition.X - crate.GlobalPosition.X, hero.GlobalPosition.Z - crate.GlobalPosition.Z).Length();
                if (dist <= pickupRadius)
                {
                    int[] payload = { crate.LootId, hero.PhotonView.ViewID, (int)crate.WeaponKind };
                    GodotPhotonService.Instance?.RaiseCustomEvent(EventClaimWeaponLoot, payload, ReceiverGroup.All, true);
                    break;
                }
            }
        }

        private void OnCustomPhotonEventReceived(byte eventCode, object data, int senderActor)
        {
            if (eventCode == EventClaimWeaponLoot && data is int[] arr && arr.Length == 3)
            {
                int lootId = arr[0];
                int heroViewId = arr[1];
                WeaponType weaponKind = (WeaponType)arr[2];

                WeaponLootPickup3D crate = WeaponCrates.Find(c => c.LootId == lootId);
                if (crate != null && crate.IsAvailable)
                {
                    crate.ConsumePickup();
                    GodotPhotonView pv = GodotPhotonService.Instance?.GetPhotonView(heroViewId);
                    if (pv != null && pv.GetParent() is HeroCharacter hero)
                    {
                        pv.RPC(nameof(HeroCharacter.RpcEquipWeapon), RpcTarget.All, (int)weaponKind);
                        AddKillFeed($"🗡️ {hero.HeroName} đã nhặt [{WeaponLoadout.Get(weaponKind).Title}]!");
                    }
                }
            }
        }

        public bool IsInsideAnyBush(Vector3 pos)
        {
            foreach (StealthBushZone3D bush in Bushes)
            {
                if (bush.ContainsPoint(pos)) return true;
            }
            return false;
        }

        public WeaponLootPickup3D FindNearestAvailableWeaponCrate(Vector3 fromPos)
        {
            WeaponLootPickup3D best = null;
            float bestDist = float.MaxValue;
            foreach (WeaponLootPickup3D crate in WeaponCrates)
            {
                if (!crate.IsAvailable) continue;
                float d = fromPos.DistanceSquaredTo(crate.GlobalPosition);
                if (d < bestDist)
                {
                    bestDist = d;
                    best = crate;
                }
            }
            return best;
        }

        public HeroCharacter FindNearestVisibleEnemy(HeroCharacter seeker, float maxRange)
        {
            HeroCharacter best = null;
            float bestDist = maxRange;
            foreach (HeroCharacter candidate in AllHeroes)
            {
                if (candidate == null || candidate == seeker || candidate.IsDead) continue;

                float dist = new Vector2(candidate.GlobalPosition.X - seeker.GlobalPosition.X, candidate.GlobalPosition.Z - seeker.GlobalPosition.Z).Length();
                if (candidate.InStealthBush && dist > 3.2f) continue;

                if (dist < bestDist)
                {
                    bestDist = dist;
                    best = candidate;
                }
            }
            return best;
        }

        public void NotifyHeroKilled(int killerViewId, HeroCharacter victim)
        {
            GodotPhotonView killerPv = GodotPhotonService.Instance?.GetPhotonView(killerViewId);
            if (killerPv != null && killerPv.GetParent() is HeroCharacter killer)
            {
                killer.Kills++;
                AddKillFeed($"🔥 {killer.HeroName} đã hạ gục {victim.HeroName} bằng [{killer.CurrentLoadout.Title}]! ({killer.Kills} Kills)");
            }
            else
            {
                AddKillFeed($"☠️ {victim.HeroName} đã bị Vòng Bo Độc nuốt chửng!");
            }
        }

        public void AddKillFeed(string message)
        {
            _killFeedMessages.Enqueue(message);
            while (_killFeedMessages.Count > 5)
            {
                _killFeedMessages.Dequeue();
            }
            if (_killFeedLabel != null)
            {
                _killFeedLabel.Text = string.Join("\n", _killFeedMessages);
            }
        }
    }
}
