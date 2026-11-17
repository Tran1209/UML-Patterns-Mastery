// ----------------------------------------------------------------------------
// HeroCharacter.cs
// Nhân vật Tướng trong Survival Heroes (Dùng chung cho Người chơi điều khiển,
// Người chơi mạng từ xa qua Photon Cloud, và AI Bot Survivors).
// Kết hợp:
//   - GodotPhotonView + IPunObservable (PhotonStream) + [PunRPC]
//   - GodotPhotonTransformView3D (Đồng bộ vị trí/góc xoay bù trễ)
//   - GodotCullArea (Tự động cập nhật InterestGroup theo ô lưới 1..9)
//   - WeaponLoadout Strategy (Nhặt vũ khí đổi bộ 4 chiêu LMB / Q / W / E)
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using System;
    using Godot;
    using Photon.Pun;
    using Photon.Pun.GodotBridge;
    using SurvivalHeroes.Patterns;

    public partial class HeroCharacter : CharacterBody3D, IPunObservable, IDamageable
    {
        public GodotPhotonView PhotonView { get; private set; }
        public GodotPhotonTransformView3D TransformView { get; private set; }
        public HeroStateMachine StateMachine { get; } = new();
        public InputBufferQueue SkillInputBuffer { get; } = new(0.18);

        private readonly IHeroState _stateCombatReady = new HeroSimpleState("⚔️ Sẵn Sàng Chiến Đấu");
        private readonly IHeroState _stateParachuteGliding = new HeroSimpleState("🪂 Đang Bay Dù Hạ Cánh");
        private readonly IHeroState _stateDashing = new HeroSimpleState("💨 Đang Lướt (Dash)");
        private readonly IHeroState _stateStealthed = new HeroSimpleState("🌿 Ẩn Thân Bụi Cỏ");
        private readonly IHeroState _stateDead = new HeroSimpleState("💀 Đang Hồi Sinh");

        public SurvivalHeroProgression Progression { get; } = new();
        public bool IsParachuteGliding { get; private set; }
        public float CooldownFlash { get; private set; }

        public Vector3 WorldPosition25D => GlobalPosition;
        public bool IsDestroyed => IsDead;

        public string HeroName { get; set; } = "Survivor";
        public bool IsLocalHumanPlayer { get; set; }
        public bool IsAiBot { get; set; }
        public int TeamId { get; set; }

        public float MaxHealth { get; set; } = 220f;
        public float Health { get; set; } = 220f;
        public float Shield { get; set; } = 0f;
        public int Kills { get; set; } = 0;
        public bool IsDead => Health <= 0f;
        public bool InStealthBush { get; private set; }

        public WeaponType CurrentWeaponType { get; private set; } = WeaponType.Unarmed;
        public WeaponLoadout CurrentLoadout { get; private set; } = WeaponLoadout.Get(WeaponType.Unarmed);

        public float CooldownBasic { get; private set; }
        public float CooldownQ { get; private set; }
        public float CooldownW { get; private set; }
        public float CooldownE { get; private set; }

        private float _baseMoveSpeed = 7.2f;
        private float _speedBuffTimer;
        private Vector3 _dashVelocity;
        private float _dashTimer;
        private float _zoneTickTimer;
        private float _respawnTimer;
        private float _aiThinkTimer;
        private Vector3 _aiMoveTarget;

        private MeshInstance3D _bodyMesh;
        private MeshInstance3D _weaponHandMesh;
        private MeshInstance3D _shieldSphereMesh;
        private MeshInstance3D _parachuteCanopyMesh;
        private StandardMaterial3D _bodyMat;
        private StandardMaterial3D _weaponMat;
        private Label3D _overheadLabel;
        private bool _tabWasPressed;
        private bool _key4WasPressed;
        private bool _key5WasPressed;
        private bool _spaceWasPressed;

        public void SetupHero(int viewId, string heroName, bool isLocalHuman, bool isAiBot, int teamId, Vector3 spawnPos, Color bodyColor)
        {
            HeroName = heroName;
            IsLocalHumanPlayer = isLocalHuman;
            IsAiBot = isAiBot;
            TeamId = teamId;
            GlobalPosition = spawnPos;
            _aiMoveTarget = spawnPos;

            // 0. Khóa trục vật lý 2.5D trực tiếp trên Jolt Physics 3D (Khóa cứng trục Y=0, khóa đổ ngã X/Z)
            PlanarPhysics25D.ApplyHardwareAxisLock(this, PlaneMode25D.TopDownIsometric_LockY);

            // 1. Khởi tạo Collider vật lý
            CollisionShape3D col = new();
            CapsuleShape3D capShape = new() { Radius = 0.55f, Height = 1.8f };
            col.Shape = capShape;
            col.Position = new Vector3(0, 0.9f, 0);
            AddChild(col);

            // 2. Khởi tạo thân hình 3D của Tướng
            _bodyMesh = new MeshInstance3D();
            CapsuleMesh capMesh = new() { Radius = 0.52f, Height = 1.75f };
            _bodyMat = new StandardMaterial3D
            {
                AlbedoColor = bodyColor,
                Roughness = 0.45f,
                Transparency = BaseMaterial3D.TransparencyEnum.Disabled
            };
            _bodyMesh.Mesh = capMesh;
            _bodyMesh.MaterialOverride = _bodyMat;
            _bodyMesh.Position = new Vector3(0, 0.9f, 0);
            AddChild(_bodyMesh);

            // Mắt / Mũ định hướng mặt trước (-Z / +Z)
            MeshInstance3D visor = new();
            BoxMesh visorBox = new() { Size = new Vector3(0.65f, 0.22f, 0.45f) };
            StandardMaterial3D visorMat = new() { AlbedoColor = new Color(0.1f, 0.12f, 0.16f) };
            visor.Mesh = visorBox;
            visor.MaterialOverride = visorMat;
            visor.Position = new Vector3(0, 1.35f, 0.38f);
            AddChild(visor);

            // 3. Khởi tạo Vũ khí cầm trên tay phải
            _weaponHandMesh = new MeshInstance3D();
            BoxMesh weaponBox = new() { Size = new Vector3(0.22f, 0.22f, 1.35f) };
            _weaponMat = new StandardMaterial3D
            {
                AlbedoColor = Colors.White,
                EmissionEnabled = true,
                Emission = Colors.Orange,
                EmissionEnergyMultiplier = 0.8f
            };
            _weaponHandMesh.Mesh = weaponBox;
            _weaponHandMesh.MaterialOverride = _weaponMat;
            _weaponHandMesh.Position = new Vector3(0.68f, 1.0f, 0.55f);
            _weaponHandMesh.Visible = false;
            AddChild(_weaponHandMesh);

            // 4. Khởi tạo Vòng Khiên Bảo Vệ (Shield VFX)
            _shieldSphereMesh = new MeshInstance3D();
            SphereMesh shieldSphere = new() { Radius = 1.15f, Height = 2.3f };
            StandardMaterial3D shieldMat = new()
            {
                AlbedoColor = new Color(0.2f, 0.85f, 1.0f, 0.28f),
                Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded,
                RenderPriority = DepthSorter25D.PriorityShieldVfx
            };
            _shieldSphereMesh.Mesh = shieldSphere;
            _shieldSphereMesh.MaterialOverride = shieldMat;
            _shieldSphereMesh.Position = new Vector3(0, 1.0f, 0);
            _shieldSphereMesh.Visible = false;
            AddChild(_shieldSphereMesh);

            // 5. Thanh thông tin trên đầu (Bật NoDepthTest, tắt OutlineSize để không rebuild Glyph Mesh gây tụt FPS!)
            _overheadLabel = new Label3D
            {
                PixelSize = 0.0105f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                RenderPriority = DepthSorter25D.PriorityOverheadUi,
                OutlineSize = 0,
                Position = new Vector3(0, 2.55f, 0)
            };
            AddChild(_overheadLabel);

            // 5B. Khởi tạo Dù Lượn 3D trên đầu khi Nhảy Dù từ Khinh Khí Cầu
            _parachuteCanopyMesh = new MeshInstance3D
            {
                Mesh = new CylinderMesh { TopRadius = 0.35f, BottomRadius = 2.4f, Height = 1.0f, RadialSegments = 12 },
                Position = new Vector3(0f, 3.3f, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(0.2f, 0.82f, 1.0f),
                    EmissionEnabled = true,
                    Emission = new Color(0.15f, 0.65f, 0.95f),
                    EmissionEnergyMultiplier = 0.85f
                },
                Visible = false
            };
            AddChild(_parachuteCanopyMesh);

            // 6. Gắn GodotPhotonView & GodotPhotonTransformView3D
            PhotonView = new GodotPhotonView
            {
                Name = "GodotPhotonView",
                ViewID = viewId,
                OwnerActorNr = isLocalHuman ? (GodotPhotonService.Instance?.LocalPlayer?.ActorNumber ?? 1) : (isAiBot ? 0 : viewId / 1000)
            };
            AddChild(PhotonView);

            TransformView = new GodotPhotonTransformView3D
            {
                Name = "GodotPhotonTransformView3D"
            };
            AddChild(TransformView);

            PhotonView.RefreshObservedAndRpcTargets();
            GodotPhotonService.Instance?.RegisterPhotonView(PhotonView);

            StateMachine.Initialize(_stateCombatReady);
            Progression.TryEquipBetterPiece(EquipmentPiece.Generate(ArmorSlot.Boots, ItemRarity.Common));
            Progression.TryEquipBetterPiece(EquipmentPiece.Generate(ArmorSlot.Chest, ItemRarity.Common));
            ApplyWeaponLoadoutLocal(WeaponType.Greatsword);
            UpdateOverheadInfo();
        }

        public float GetScaledDamage(float baseDamage) => baseDamage + Progression.TotalBonusAttack;
        public float GetScaledCooldown(float baseCooldown) => MathF.Max(0.15f, baseCooldown * (1f - Progression.TotalCdr));

        /// <summary>
        /// Khởi động hiệu ứng Nhảy Dù 3D từ Khinh Khí Cầu trên cao (Y = 14.5m) hạ cánh xuống địa danh đã chọn!
        /// </summary>
        [PunRPC]
        public void RpcStartParachuteDrop(Vector3 targetGroundPos, PhotonMessageInfo info)
        {
            StartParachuteDrop(targetGroundPos);
        }

        public void StartParachuteDrop(Vector3 targetGroundPos)
        {
            IsParachuteGliding = true;
            AxisLockLinearY = false;
            Health = MaxHealth;
            Shield = 50f;
            GlobalPosition = new Vector3(
                Mathf.Clamp(targetGroundPos.X, -82f, 82f),
                14.5f,
                Mathf.Clamp(targetGroundPos.Z, -82f, 82f)
            );
            if (_parachuteCanopyMesh != null) _parachuteCanopyMesh.Visible = true;
            StateMachine.TransitionTo(_stateParachuteGliding);
        }

        public override void _PhysicsProcess(double delta)
        {
            float dt = (float)delta;

            if (CooldownBasic > 0f) CooldownBasic = MathF.Max(0f, CooldownBasic - dt);
            if (CooldownQ > 0f) CooldownQ = MathF.Max(0f, CooldownQ - dt);
            if (CooldownW > 0f) CooldownW = MathF.Max(0f, CooldownW - dt);
            if (CooldownE > 0f) CooldownE = MathF.Max(0f, CooldownE - dt);
            if (CooldownFlash > 0f) CooldownFlash = MathF.Max(0f, CooldownFlash - dt);
            if (_speedBuffTimer > 0f) _speedBuffTimer = MathF.Max(0f, _speedBuffTimer - dt);

            if (IsDead)
            {
                StateMachine.TransitionTo(_stateDead);
                _respawnTimer -= dt;
                if (_respawnTimer <= 0f && (IsLocalHumanPlayer || (IsAiBot && GodotPhotonService.Instance.IsMasterClient)))
                {
                    Vector3 respawnPos = new((GD.Randf() - 0.5f) * 32f, 0f, (GD.Randf() - 0.5f) * 32f);
                    PhotonView.RPC(nameof(RpcRespawnHero), RpcTarget.All, respawnPos);
                }
                UpdateOverheadInfo();
                return;
            }

            // Nếu là người chơi từ xa đang nhảy dù và đã chạm đất (Y <= 0.12m), tự động thu dù
            if (!IsLocalHumanPlayer && IsParachuteGliding && GlobalPosition.Y <= 0.12f)
            {
                IsParachuteGliding = false;
                if (_parachuteCanopyMesh != null) _parachuteCanopyMesh.Visible = false;
            }

            // Kiểm tra đứng trong Bụi Cỏ MOBA (Nếu bị Mắt Kiểm Soát đối phương soi trúng thì mất tàng hình!)
            bool insideBush = ArenaGameManager.Instance != null && ArenaGameManager.Instance.IsInsideAnyBush(GlobalPosition);
            bool revealedByWard = ArenaGameManager.Instance != null && ArenaGameManager.Instance.IsRevealedByEnemyWard(GlobalPosition, TeamId);
            InStealthBush = insideBush && !revealedByWard && !IsParachuteGliding;

            // Cập nhật State Machine (_DesignPatterns/State)
            if (IsParachuteGliding)
                StateMachine.TransitionTo(_stateParachuteGliding);
            else if (_dashTimer > 0f)
                StateMachine.TransitionTo(_stateDashing);
            else if (InStealthBush)
                StateMachine.TransitionTo(_stateStealthed);
            else
                StateMachine.TransitionTo(_stateCombatReady);

            StateMachine.Tick(dt);

            // Cập nhật ô lưới CullArea cho mọi nhân vật
            if (GodotCullArea.Instance != null && PhotonView != null)
            {
                if (IsLocalHumanPlayer)
                {
                    GodotCullArea.Instance.UpdatePlayerCulling(GlobalPosition, PhotonView);
                }
                else
                {
                    PhotonView.Group = GodotCullArea.Instance.GetCellIdFromPosition(GlobalPosition);
                }
            }

            // Sát thương khi đứng ngoài Vòng Bo Độc (Safe Zone)
            if (GodotSafeZoneTimer.Instance != null && GodotSafeZoneTimer.Instance.IsOutsideSafeZone(GlobalPosition))
            {
                _zoneTickTimer += dt;
                if (_zoneTickTimer >= 1.0f)
                {
                    _zoneTickTimer = 0f;
                    ApplyDamageFromSkill(GodotSafeZoneTimer.Instance.DamagePerSecondOutside, 0);
                }
            }
            else
            {
                _zoneTickTimer = 0f;
            }

            // Điều khiển di chuyển & chiến đấu
            if (IsLocalHumanPlayer)
            {
                ProcessHumanControl(dt);
            }
            else if (IsAiBot && GodotPhotonService.Instance != null && GodotPhotonService.Instance.IsMasterClient)
            {
                ProcessAiBotBehavior(dt);
            }

            UpdateVisualVisibilityAndOverhead();
        }

        private void ProcessHumanControl(float dt)
        {
            Vector3 moveDir = Vector3.Zero;
            if (Input.IsKeyPressed(Key.W) || Input.IsKeyPressed(Key.Up)) moveDir.Z -= 1f;
            if (Input.IsKeyPressed(Key.S) || Input.IsKeyPressed(Key.Down)) moveDir.Z += 1f;
            if (Input.IsKeyPressed(Key.A) || Input.IsKeyPressed(Key.Left)) moveDir.X -= 1f;
            if (Input.IsKeyPressed(Key.D) || Input.IsKeyPressed(Key.Right)) moveDir.X += 1f;

            float speed = _baseMoveSpeed + CurrentLoadout.MoveSpeedBonus + Progression.TotalBonusSpeed + (_speedBuffTimer > 0f ? 2.8f : 0f);

            // Nếu đang bay Dù Lượn 3D từ trên cao xuống: Cho phép lái dù bằng WASD và hạ độ cao dần về Y = 0
            if (IsParachuteGliding)
            {
                Vector3 glideVel = moveDir.LengthSquared() > 0.01f ? moveDir.Normalized() * (speed * 1.45f) : Vector3.Zero;
                float newY = GlobalPosition.Y - 5.2f * dt;
                GlobalPosition = new Vector3(
                    Mathf.Clamp(GlobalPosition.X + glideVel.X * dt, -82.5f, 82.5f),
                    MathF.Max(0f, newY),
                    Mathf.Clamp(GlobalPosition.Z + glideVel.Z * dt, -82.5f, 82.5f)
                );

                if (GlobalPosition.Y <= 0.05f)
                {
                    IsParachuteGliding = false;
                    if (_parachuteCanopyMesh != null) _parachuteCanopyMesh.Visible = false;
                    PlanarPhysics25D.ApplyHardwareAxisLock(this, PlaneMode25D.TopDownIsometric_LockY);
                    ClampInsideMapBounds();
                    ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 3.6f, new Color(0.25f, 0.9f, 1.0f), 0.45f);
                    ArenaEventBus.RaiseFloatingText(GlobalPosition, "🪂 HẠ CÁNH AN TOÀN!", new Color(0.35f, 1f, 0.65f));
                }
                return;
            }

            if (_dashTimer > 0f)
            {
                _dashTimer -= dt;
                Velocity = _dashVelocity;
            }
            else
            {
                Velocity = moveDir.LengthSquared() > 0.01f ? moveDir.Normalized() * speed : Vector3.Zero;
            }

            MoveAndSlide();
            ClampInsideMapBounds();

            // Xoay mặt Tướng theo vị trí con trỏ chuột trên mặt đất
            Vector3 aimWorld = ArenaGameManager.Instance != null ? ArenaGameManager.Instance.GetMouseGroundPosition(GlobalPosition) : GlobalPosition + Vector3.Forward;
            Vector3 lookDiff = new(aimWorld.X - GlobalPosition.X, 0f, aimWorld.Z - GlobalPosition.Z);
            if (lookDiff.LengthSquared() > 0.05f)
            {
                float targetYaw = MathF.Atan2(lookDiff.X, lookDiff.Z);
                Rotation = new Vector3(0f, targetYaw, 0f);
            }

            // Phím tắt nhặt vũ khí gần nhất (F) hoặc tự động nhặt khi đi sát qua
            if (ArenaGameManager.Instance != null)
            {
                ArenaGameManager.Instance.TryPickupNearbyWeapon(this, Input.IsKeyPressed(Key.F));
            }

            // [Phím Tab]: Tráo đổi Vũ Khí Chính <-> Vũ Khí Phụ (2 Vũ khí = 8 Chiêu thức chuẩn Survival Heroes!)
            bool tabDown = Input.IsKeyPressed(Key.Tab);
            if (tabDown && !_tabWasPressed)
            {
                WeaponType oldMain = CurrentWeaponType;
                WeaponType nextMain = Progression.SecondaryWeapon != WeaponType.Unarmed ? Progression.SecondaryWeapon : WeaponType.RangerBow;
                Progression.SecondaryWeapon = oldMain != WeaponType.Unarmed ? oldMain : WeaponType.Greatsword;
                PhotonView.RPC(nameof(RpcEquipWeapon), RpcTarget.All, (int)nextMain);
                ArenaEventBus.RaiseFloatingText(GlobalPosition, $"🔄 ĐỔI VŨ KHÍ: {WeaponLoadout.Get(nextMain).Title}", new Color(1f, 0.9f, 0.3f));
            }
            _tabWasPressed = tabDown;

            // [Phím 4]: Uống Bình Máu (+95 HP)
            bool key4Down = Input.IsKeyPressed(Key.Key4);
            if (key4Down && !_key4WasPressed && Progression.HealthPotions > 0 && Health < MaxHealth)
            {
                Progression.HealthPotions--;
                Health = MathF.Min(MaxHealth, Health + 95f);
                ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 1.8f, Colors.LimeGreen, 0.3f);
                ArenaEventBus.RaiseFloatingText(GlobalPosition, $"🧪 +95 HP (Còn {Progression.HealthPotions} bình)", Colors.LimeGreen);
            }
            _key4WasPressed = key4Down;

            // [Phím 5]: Cắm Mắt Kiểm Soát (Soi Bụi Cỏ Tàng Hình 25s)
            bool key5Down = Input.IsKeyPressed(Key.Key5);
            if (key5Down && !_key5WasPressed && Progression.ScoutWards > 0)
            {
                Progression.ScoutWards--;
                ArenaGameManager.Instance?.PlaceScoutWard(TeamId, GlobalPosition + lookDiff.Normalized() * 2.2f);
                ArenaEventBus.RaiseFloatingText(GlobalPosition, $"👁️ CẮM MẮT SOI BỤI CỎ (Còn {Progression.ScoutWards})", Colors.Yellow);
            }
            _key5WasPressed = key5Down;

            // [Phím Space]: Phép bổ trợ Tốc Biến (Flash) theo hướng chuột
            bool spaceDown = Input.IsKeyPressed(Key.Space);
            if (spaceDown && !_spaceWasPressed && CooldownFlash <= 0f)
            {
                CooldownFlash = 10.0f;
                Vector3 flashDir = lookDiff.LengthSquared() > 0.01f ? lookDiff.Normalized() : Vector3.Forward;
                ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 1.8f, Colors.Gold, 0.25f);
                GlobalPosition += flashDir * 7.5f;
                ClampInsideMapBounds();
                ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 2.2f, Colors.Gold, 0.3f);
                ArenaEventBus.RaiseFloatingText(GlobalPosition, "⚡ TỐC BIẾN!", Colors.Gold);
            }
            _spaceWasPressed = spaceDown;

            // Phím tắt đổi nhanh Vũ khí để test (Phím 1: Đại Kiếm, Phím 2: Cung, Phím 3: Trượng Lửa)
            if (Input.IsKeyPressed(Key.Key1) && CurrentWeaponType != WeaponType.Greatsword)
                PhotonView.RPC(nameof(RpcEquipWeapon), RpcTarget.All, (int)WeaponType.Greatsword);
            else if (Input.IsKeyPressed(Key.Key2) && CurrentWeaponType != WeaponType.RangerBow)
                PhotonView.RPC(nameof(RpcEquipWeapon), RpcTarget.All, (int)WeaponType.RangerBow);
            else if (Input.IsKeyPressed(Key.Key3) && CurrentWeaponType != WeaponType.FireStaff)
                PhotonView.RPC(nameof(RpcEquipWeapon), RpcTarget.All, (int)WeaponType.FireStaff);

            // Tung chiêu LMB / Q / W / E qua Command Pattern InputBufferQueue (Đệm phím combo 180ms) + [PunRPC]
            if (Input.IsMouseButtonPressed(MouseButton.Left))
            {
                if (CooldownBasic <= 0f)
                {
                    CooldownBasic = GetScaledCooldown(CurrentLoadout.BasicAttack.Cooldown);
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 0, GlobalPosition, aimWorld);
                }
                else if (CooldownBasic <= 0.18f && SkillInputBuffer.BufferedCount == 0)
                {
                    SkillInputBuffer.EnqueueCommand(new DelegateCombatCommand(
                        CurrentLoadout.BasicAttack.Name,
                        () => CooldownBasic <= 0f && !IsDead,
                        () =>
                        {
                            CooldownBasic = GetScaledCooldown(CurrentLoadout.BasicAttack.Cooldown);
                            Vector3 aim = ArenaGameManager.Instance != null ? ArenaGameManager.Instance.GetMouseGroundPosition(GlobalPosition) : aimWorld;
                            PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 0, GlobalPosition, aim);
                        }));
                }
            }
            if (Input.IsKeyPressed(Key.Q))
            {
                if (CooldownQ <= 0f)
                {
                    CooldownQ = GetScaledCooldown(CurrentLoadout.SkillQ.Cooldown);
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 1, GlobalPosition, aimWorld);
                }
                else if (CooldownQ <= 0.18f && SkillInputBuffer.BufferedCount == 0)
                {
                    SkillInputBuffer.EnqueueCommand(new DelegateCombatCommand(
                        $"[Q] {CurrentLoadout.SkillQ.Name}",
                        () => CooldownQ <= 0f && !IsDead,
                        () =>
                        {
                            CooldownQ = GetScaledCooldown(CurrentLoadout.SkillQ.Cooldown);
                            Vector3 aim = ArenaGameManager.Instance != null ? ArenaGameManager.Instance.GetMouseGroundPosition(GlobalPosition) : aimWorld;
                            PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 1, GlobalPosition, aim);
                        }));
                }
            }
            if (Input.IsKeyPressed(Key.W))
            {
                if (CooldownW <= 0f)
                {
                    CooldownW = GetScaledCooldown(CurrentLoadout.SkillW.Cooldown);
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 2, GlobalPosition, aimWorld);
                }
                else if (CooldownW <= 0.18f && SkillInputBuffer.BufferedCount == 0)
                {
                    SkillInputBuffer.EnqueueCommand(new DelegateCombatCommand(
                        $"[W] {CurrentLoadout.SkillW.Name}",
                        () => CooldownW <= 0f && !IsDead,
                        () =>
                        {
                            CooldownW = GetScaledCooldown(CurrentLoadout.SkillW.Cooldown);
                            Vector3 aim = ArenaGameManager.Instance != null ? ArenaGameManager.Instance.GetMouseGroundPosition(GlobalPosition) : aimWorld;
                            PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 2, GlobalPosition, aim);
                        }));
                }
            }
            if (Input.IsKeyPressed(Key.E))
            {
                if (CooldownE <= 0f)
                {
                    CooldownE = GetScaledCooldown(CurrentLoadout.SkillE.Cooldown);
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 3, GlobalPosition, aimWorld);
                }
                else if (CooldownE <= 0.18f && SkillInputBuffer.BufferedCount == 0)
                {
                    SkillInputBuffer.EnqueueCommand(new DelegateCombatCommand(
                        $"[E] {CurrentLoadout.SkillE.Name}",
                        () => CooldownE <= 0f && !IsDead,
                        () =>
                        {
                            CooldownE = GetScaledCooldown(CurrentLoadout.SkillE.Cooldown);
                            Vector3 aim = ArenaGameManager.Instance != null ? ArenaGameManager.Instance.GetMouseGroundPosition(GlobalPosition) : aimWorld;
                            PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 3, GlobalPosition, aim);
                        }));
                }
            }

            // Thực thi lệnh combo đang đệm trong hàng đợi (nếu vừa hết hồi chiêu)
            SkillInputBuffer.ProcessBufferedCommands();
        }

        private void ProcessAiBotBehavior(float dt)
        {
            if (ArenaGameManager.Instance == null) return;

            // Tự động nhặt vũ khí nếu đi ngang qua
            if (CurrentWeaponType == WeaponType.Unarmed)
            {
                ArenaGameManager.Instance.TryPickupNearbyWeapon(this, true);
            }

            _aiThinkTimer -= dt;
            HeroCharacter nearestEnemy = ArenaGameManager.Instance.FindNearestVisibleEnemy(this, 19.0f);

            if (_aiThinkTimer <= 0f)
            {
                _aiThinkTimer = 0.45f;

                // Ưu tiên 1: Chạy vào Vòng Bo nếu đang đứng ngoài vòng an toàn
                if (GodotSafeZoneTimer.Instance != null && GodotSafeZoneTimer.Instance.IsOutsideSafeZone(GlobalPosition))
                {
                    _aiMoveTarget = GodotSafeZoneTimer.Instance.ZoneCenter + new Vector3((GD.Randf() - 0.5f) * 8f, 0f, (GD.Randf() - 0.5f) * 8f);
                }
                // Ưu tiên 2: Nếu tay không, tìm hộp Vũ khí gần nhất
                else if (CurrentWeaponType == WeaponType.Unarmed)
                {
                    WeaponLootPickup3D crate = ArenaGameManager.Instance.FindNearestAvailableWeaponCrate(GlobalPosition);
                    if (crate != null) _aiMoveTarget = crate.GlobalPosition;
                }
                // Ưu tiên 3: Giao tranh với đối thủ gần nhất
                else if (nearestEnemy != null)
                {
                    float desiredDist = CurrentWeaponType == WeaponType.Greatsword ? 2.2f : 9.5f;
                    Vector3 toMe = (GlobalPosition - nearestEnemy.GlobalPosition).Normalized();
                    _aiMoveTarget = nearestEnemy.GlobalPosition + toMe * desiredDist;
                }
                else
                {
                    _aiMoveTarget = GlobalPosition + new Vector3((GD.Randf() - 0.5f) * 18f, 0f, (GD.Randf() - 0.5f) * 18f);
                }
            }

            Vector3 diff = new(_aiMoveTarget.X - GlobalPosition.X, 0f, _aiMoveTarget.Z - GlobalPosition.Z);
            float speed = (_baseMoveSpeed * 0.85f) + CurrentLoadout.MoveSpeedBonus;

            if (_dashTimer > 0f)
            {
                _dashTimer -= dt;
                Velocity = _dashVelocity;
            }
            else if (diff.Length() > 0.85f)
            {
                Velocity = diff.Normalized() * speed;
            }
            else
            {
                Velocity = Vector3.Zero;
            }

            MoveAndSlide();
            ClampInsideMapBounds();

            Vector3 lookTarget = nearestEnemy != null ? nearestEnemy.GlobalPosition : _aiMoveTarget;
            Vector3 lookVec = new(lookTarget.X - GlobalPosition.X, 0f, lookTarget.Z - GlobalPosition.Z);
            if (lookVec.LengthSquared() > 0.05f)
            {
                Rotation = new Vector3(0f, MathF.Atan2(lookVec.X, lookVec.Z), 0f);
            }

            // AI tung kỹ năng qua [PunRPC] khi có mục tiêu trong tầm
            if (nearestEnemy != null)
            {
                float distToEnemy = new Vector2(nearestEnemy.GlobalPosition.X - GlobalPosition.X, nearestEnemy.GlobalPosition.Z - GlobalPosition.Z).Length();
                Vector3 aimPos = nearestEnemy.GlobalPosition;

                if (CooldownQ <= 0f && distToEnemy <= CurrentLoadout.SkillQ.Range)
                {
                    CooldownQ = CurrentLoadout.SkillQ.Cooldown + 0.8f;
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 1, GlobalPosition, aimPos);
                }
                else if (CooldownW <= 0f && distToEnemy <= Mathf.Max(5.0f, CurrentLoadout.SkillW.Range))
                {
                    CooldownW = CurrentLoadout.SkillW.Cooldown + 1.0f;
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 2, GlobalPosition, aimPos);
                }
                else if (CooldownBasic <= 0f && distToEnemy <= CurrentLoadout.BasicAttack.Range)
                {
                    CooldownBasic = CurrentLoadout.BasicAttack.Cooldown + 0.35f;
                    PhotonView.RPC(nameof(RpcCastSkill), RpcTarget.All, 0, GlobalPosition, aimPos);
                }
            }
        }

        private void ClampInsideMapBounds()
        {
            Velocity = PlanarPhysics25D.ClampVelocityToPlane(Velocity, PlaneMode25D.TopDownIsometric_LockY);
            GlobalPosition = PlanarPhysics25D.ClampPositionToPlaneAndBounds(
                GlobalPosition,
                mapHalfExtent: 82.5f,
                fixedPlaneCoord: 0f,
                mode: PlaneMode25D.TopDownIsometric_LockY);
        }

        [PunRPC]
        public void RpcEquipWeapon(int weaponTypeInt, PhotonMessageInfo info)
        {
            ApplyWeaponLoadoutLocal((WeaponType)weaponTypeInt);
        }

        private void ApplyWeaponLoadoutLocal(WeaponType newType)
        {
            CurrentWeaponType = newType;
            CurrentLoadout = WeaponLoadout.Get(newType);

            if (_weaponHandMesh != null && _weaponMat != null)
            {
                _weaponHandMesh.Visible = newType != WeaponType.Unarmed;
                _weaponMat.AlbedoColor = CurrentLoadout.ThemeColor;
                _weaponMat.Emission = CurrentLoadout.ThemeColor;
            }

            CooldownBasic = 0f;
            CooldownQ = 0f;
            CooldownW = 0f;
            CooldownE = 0f;
        }

        /// <summary>
        /// Thực thi toàn bộ các chiêu thức của 3 dòng Vũ khí (Đại Kiếm, Cung, Trượng Lửa)
        /// đồng bộ qua Photon [PunRPC] kèm bù trễ SentServerTime!
        /// </summary>
        [PunRPC]
        public void RpcCastSkill(int skillSlot, Vector3 castOrigin, Vector3 targetWorldPos, PhotonMessageInfo info)
        {
            if (IsDead || ArenaGameManager.Instance == null) return;

            double serverNow = GodotPhotonService.Instance?.Time ?? info.SentServerTime;
            float lagSeconds = MathF.Abs((float)(serverNow - info.SentServerTime));

            Vector3 dir = new(targetWorldPos.X - castOrigin.X, 0f, targetWorldPos.Z - castOrigin.Z);
            if (dir.LengthSquared() < 0.001f) dir = new Vector3(MathF.Sin(Rotation.Y), 0f, MathF.Cos(Rotation.Y));
            dir = dir.Normalized();

            int myViewId = PhotonView?.ViewID ?? 0;
            Color themeColor = CurrentLoadout.ThemeColor;

            switch (CurrentWeaponType)
            {
                case WeaponType.Greatsword:
                    ExecuteGreatswordSkill(skillSlot, myViewId, castOrigin, dir, themeColor);
                    break;
                case WeaponType.RangerBow:
                    ExecuteRangerBowSkill(skillSlot, myViewId, castOrigin, targetWorldPos, dir, themeColor, lagSeconds);
                    break;
                case WeaponType.FireStaff:
                    ExecuteFireStaffSkill(skillSlot, myViewId, castOrigin, targetWorldPos, dir, themeColor, lagSeconds);
                    break;
                default:
                    ExecuteUnarmedSkill(skillSlot, myViewId, castOrigin, dir, themeColor);
                    break;
            }
        }

        private void ExecuteGreatswordSkill(int slot, int myViewId, Vector3 origin, Vector3 dir, Color color)
        {
            if (slot == 0) // LMB: Trảm Kiếm cận chiến phía trước
            {
                Vector3 hitCenter = origin + dir * 2.1f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(hitCenter, 2.2f, color, 0.22f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, hitCenter, 2.3f, GetScaledDamage(CurrentLoadout.BasicAttack.Damage));
            }
            else if (slot == 1) // Q: Lướt Chém (Dash Strike)
            {
                _dashVelocity = dir * 24.0f;
                _dashTimer = 0.26f;
                Vector3 slashCenter = origin + dir * 3.5f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(slashCenter, 2.8f, color, 0.3f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, slashCenter, 3.0f, GetScaledDamage(CurrentLoadout.SkillQ.Damage));
            }
            else if (slot == 2) // W: Xoáy Kiếm 360 độ (Whirlwind)
            {
                ArenaGameManager.Instance.SpawnAoeRingEffect(origin, 4.5f, new Color(1f, 0.25f, 0.1f), 0.45f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, origin, 4.5f, GetScaledDamage(CurrentLoadout.SkillW.Damage));
            }
            else if (slot == 3) // E: Khiên Dũng Sĩ + Tăng Tốc
            {
                Shield = MathF.Min(120f, Shield + 75f);
                _speedBuffTimer = 4.0f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(origin, 2.0f, Colors.Cyan, 0.35f);
            }
        }

        private void ExecuteRangerBowSkill(int slot, int myViewId, Vector3 origin, Vector3 targetPos, Vector3 dir, Color color, float lag)
        {
            Vector3 muzzle = origin + new Vector3(0, 1.0f, 0) + dir * 0.8f;
            if (slot == 0) // LMB: Tên Xuyên Phá
            {
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 26f, CurrentLoadout.BasicAttack.Range, GetScaledDamage(CurrentLoadout.BasicAttack.Damage), 0f, color, lag);
            }
            else if (slot == 1) // Q: Tán Xạ 5 Mũi Tên Hình Nón
            {
                for (int i = -2; i <= 2; i++)
                {
                    float angleOffset = i * Mathf.DegToRad(11f);
                    Vector3 rotatedDir = dir.Rotated(Vector3.Up, angleOffset);
                    ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, rotatedDir, 24f, CurrentLoadout.SkillQ.Range, GetScaledDamage(CurrentLoadout.SkillQ.Damage), 0f, color, lag);
                }
            }
            else if (slot == 2) // W: Lộn Nhào Bắn Lùi (Backflip)
            {
                _dashVelocity = -dir * 20.0f;
                _dashTimer = 0.24f;
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 25f, CurrentLoadout.SkillW.Range, GetScaledDamage(CurrentLoadout.SkillW.Damage), 2.2f, Colors.Yellow, lag);
            }
            else if (slot == 3) // E: Mưa Tên AOE tại vị trí chuột
            {
                Vector3 clampedTarget = origin + (targetPos - origin).LimitLength(CurrentLoadout.SkillE.Range);
                ArenaGameManager.Instance.SpawnAoeRingEffect(clampedTarget, 4.2f, color, 0.55f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, clampedTarget, 4.2f, GetScaledDamage(CurrentLoadout.SkillE.Damage));
            }
        }

        private void ExecuteFireStaffSkill(int slot, int myViewId, Vector3 origin, Vector3 targetPos, Vector3 dir, Color color, float lag)
        {
            Vector3 muzzle = origin + new Vector3(0, 1.05f, 0) + dir * 0.8f;
            if (slot == 0) // LMB: Hỏa Cầu
            {
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 21f, CurrentLoadout.BasicAttack.Range, GetScaledDamage(CurrentLoadout.BasicAttack.Damage), 0f, color, lag);
            }
            else if (slot == 1) // Q: Đại Hỏa Cầu Nổ
            {
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 18f, CurrentLoadout.SkillQ.Range, GetScaledDamage(CurrentLoadout.SkillQ.Damage), 3.6f, new Color(1f, 0.2f, 0.05f), lag);
            }
            else if (slot == 2) // W: Thiên Thạch Rơi AOE
            {
                Vector3 clampedTarget = origin + (targetPos - origin).LimitLength(CurrentLoadout.SkillW.Range);
                ArenaGameManager.Instance.SpawnAoeRingEffect(clampedTarget, 4.8f, new Color(0.95f, 0.15f, 0.8f), 0.6f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, clampedTarget, 4.8f, GetScaledDamage(CurrentLoadout.SkillW.Damage));
            }
            else if (slot == 3) // E: Tốc Biến Hư Không (Arcane Blink)
            {
                Vector3 blinkDest = origin + (targetPos - origin).LimitLength(CurrentLoadout.SkillE.Range);
                ArenaGameManager.Instance.SpawnAoeRingEffect(origin, 2.0f, color, 0.25f);
                GlobalPosition = new Vector3(blinkDest.X, 0f, blinkDest.Z);
                ClampInsideMapBounds();
                ArenaGameManager.Instance.SpawnAoeRingEffect(GlobalPosition, 2.8f, color, 0.35f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, GlobalPosition, 2.8f, GetScaledDamage(CurrentLoadout.SkillE.Damage));
            }
        }

        private void ExecuteUnarmedSkill(int slot, int myViewId, Vector3 origin, Vector3 dir, Color color)
        {
            if (slot == 0)
            {
                Vector3 hitPos = origin + dir * 1.6f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(hitPos, 1.5f, color, 0.18f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, hitPos, 1.6f, CurrentLoadout.BasicAttack.Damage);
            }
            else if (slot == 1)
            {
                _dashVelocity = dir * 18.0f;
                _dashTimer = 0.22f;
            }
            else if (slot == 2)
            {
                Health = MathF.Min(MaxHealth, Health + 35f);
                ArenaGameManager.Instance.SpawnAoeRingEffect(origin, 1.8f, Colors.Green, 0.3f);
            }
            else if (slot == 3)
            {
                Shield = MathF.Min(100f, Shield + 40f);
                _speedBuffTimer = 3.0f;
            }
        }

        public void TakeDamage(float amount, int attackerViewId)
            => ApplyDamageFromSkill(amount, attackerViewId);

        public void HealAndBuffFromShrine(float healAmount)
        {
            if (IsDead) return;
            Health = MathF.Min(MaxHealth, Health + healAmount);
            _speedBuffTimer = 3.5f;
        }

        public void ApplyDamageFromSkill(float rawDamage, int attackerViewId)
        {
            if (IsDead || rawDamage <= 0f) return;

            ArenaEventBus.RaiseFloatingText(
                GlobalPosition,
                $"-{Mathf.RoundToInt(rawDamage)}",
                IsLocalHumanPlayer ? new Color(1f, 0.3f, 0.3f) : new Color(1f, 0.92f, 0.25f));

            float remainingDamage = rawDamage;
            if (Shield > 0f)
            {
                if (Shield >= remainingDamage)
                {
                    Shield -= remainingDamage;
                    remainingDamage = 0f;
                }
                else
                {
                    remainingDamage -= Shield;
                    Shield = 0f;
                }
            }

            Health = MathF.Max(0f, Health - remainingDamage);

            if (Health <= 0f)
            {
                _respawnTimer = 4.5f;
                Velocity = Vector3.Zero;
                if (attackerViewId != 0 && ArenaGameManager.Instance != null)
                {
                    ArenaGameManager.Instance.NotifyHeroKilled(attackerViewId, this);
                }
            }
        }

        [PunRPC]
        public void RpcRespawnHero(Vector3 spawnPos, PhotonMessageInfo info)
        {
            GlobalPosition = spawnPos;
            Health = MaxHealth;
            Shield = 30f;
        }

        private void UpdateVisualVisibilityAndOverhead()
        {
            if (_shieldSphereMesh != null)
            {
                _shieldSphereMesh.Visible = !IsDead && Shield > 0.5f;
                DepthSorter25D.UpdateIsometricDepthOrder(_shieldSphereMesh, GlobalPosition.Z, 0.15f);
            }

            if (_bodyMesh != null && _bodyMat != null)
            {
                _bodyMesh.Visible = !IsDead;
                // Chỉ bật Alpha Transparency khi đang đứng trong Bụi Cỏ Tàng Hình, còn bình thường để Opaque ghi vào Hardware Z-Buffer chuẩn 100%!
                _bodyMat.Transparency = InStealthBush
                    ? BaseMaterial3D.TransparencyEnum.Alpha
                    : BaseMaterial3D.TransparencyEnum.Disabled;

                Color c = _bodyMat.AlbedoColor;
                c.A = InStealthBush ? 0.38f : 1.0f;
                _bodyMat.AlbedoColor = c;

                if (InStealthBush)
                {
                    _bodyMat.RenderPriority = DepthSorter25D.ComputeDynamicRenderPriority(GlobalPosition.Z, DepthSorter25D.PriorityHeroAlpha);
                    DepthSorter25D.UpdateIsometricDepthOrder(_bodyMesh, GlobalPosition.Z, 0f);
                }
            }

            if (_overheadLabel != null)
            {
                DepthSorter25D.UpdateIsometricDepthOrder(_overheadLabel, GlobalPosition.Z, 0.35f);
            }

            UpdateOverheadInfo();
        }

        private string _lastOverheadText = "";

        private void UpdateOverheadInfo()
        {
            if (_overheadLabel == null) return;

            byte cellId = PhotonView?.Group ?? 5;
            bool isSubscribedCell = GodotPhotonService.Instance == null || GodotPhotonService.Instance.IsGroupSubscribed(cellId);

            if (IsDead)
            {
                int secLeft = Mathf.CeilToInt(_respawnTimer);
                string deadTxt = $"💀 {HeroName} (Hồi sinh {secLeft}s)";
                if (_lastOverheadText != deadTxt)
                {
                    _lastOverheadText = deadTxt;
                    _overheadLabel.Text = deadTxt;
                    _overheadLabel.Modulate = new Color(1f, 0.35f, 0.35f);
                }
                return;
            }

            string stealthTag = InStealthBush ? " [🌿TÀNG HÌNH]" : "";
            string cullTag = isSubscribedCell ? $"[Ô #{cellId}]" : $"[Ô #{cellId} - NGOÀI VÙNG MẠNG]";
            string shieldTxt = Shield > 0.5f ? $" + 🛡️{(int)Shield}" : "";
            bool isTeammate = ArenaGameManager.Instance?.LocalHero != null && ArenaGameManager.Instance.LocalHero.TeamId == TeamId;

            string aliveTxt = $"[Đội {TeamId}] {HeroName} ({Kills}⚔️) {cullTag}{stealthTag}\n[{CurrentLoadout.Title}] ❤️ {(int)Health}/{(int)MaxHealth}{shieldTxt}";
            if (_lastOverheadText != aliveTxt)
            {
                _lastOverheadText = aliveTxt;
                _overheadLabel.Text = aliveTxt;
            }
            _overheadLabel.Modulate = IsLocalHumanPlayer
                ? new Color(0.35f, 1.0f, 0.55f)
                : (isTeammate ? new Color(0.35f, 0.85f, 1.0f) : (isSubscribedCell ? new Color(1.0f, 0.92f, 0.65f) : new Color(0.6f, 0.6f, 0.65f, 0.55f)));
        }

        /// <summary>Đồng bộ trạng thái liên tục qua PhotonStream (IPunObservable).</summary>
        public void OnPhotonSerializeView(PhotonStream stream, PhotonMessageInfo info)
        {
            if (stream.IsWriting)
            {
                stream.SendNext(Health);
                stream.SendNext(Shield);
                stream.SendNext((int)CurrentWeaponType);
                stream.SendNext(Kills);
                stream.SendNext(InStealthBush);
                stream.SendNext(TeamId);
                stream.SendNext(HeroName);
            }
            else
            {
                Health = (float)stream.ReceiveNext();
                Shield = (float)stream.ReceiveNext();
                WeaponType syncedWeapon = (WeaponType)(int)stream.ReceiveNext();
                if (syncedWeapon != CurrentWeaponType)
                {
                    ApplyWeaponLoadoutLocal(syncedWeapon);
                }
                Kills = (int)stream.ReceiveNext();
                InStealthBush = (bool)stream.ReceiveNext();
                TeamId = (int)stream.ReceiveNext();
                HeroName = (string)stream.ReceiveNext();
            }
        }
    }
}
