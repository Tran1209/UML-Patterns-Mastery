// ----------------------------------------------------------------------------
// HeroCharacter.cs
// [TẦNG 5 - KHUNG THỰC THỂ TƯỚNG CƠ SỞ (HERO BASE)]:
// Đã được tách sạch sẽ theo Component Pattern (Single Responsibility):
//   - HeroCharacter: Chỉ quản lý Trạng thái (Máu, Giáp, Vũ khí, Thực thi Kỹ năng,
//     Hiển thị 3D & Fog of War, Đồng bộ PhotonStream).
//   - HeroLocalInputController: Component độc lập chuyên đọc Input cho LocalHero.
//   - HeroAiBotController: Component độc lập chuyên chạy AI cho Bot (có tiếp quản MasterClient).
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using System;
    using Godot;
    using Photon.Pun;
    using Photon.Pun.GodotBridge;
    using SurvivalHeroes.Core.Command;
    using SurvivalHeroes.Core.Events;
    using SurvivalHeroes.Core.Interfaces;
    using SurvivalHeroes.Core.Physics;
    using SurvivalHeroes.Core.StateMachine;
    using SurvivalHeroes.Gameplay.Hero;
    using SurvivalHeroes.Gameplay.Items;
    using SurvivalHeroes.Gameplay.World;

    public partial class HeroCharacter : CharacterBody3D, IPunObservable, IDamageable
    {
        public GodotPhotonView PhotonView { get; private set; }
        public GodotPhotonTransformView3D TransformView { get; private set; }
        public HeroStateMachine StateMachine { get; } = new();
        public InputBufferQueue SkillInputBuffer { get; } = new(0.18);
        public SurvivalHeroProgression Progression { get; } = new();

        private readonly IHeroState _stateCombatReady = new HeroSimpleState("⚔️ Sẵn Sàng Chiến Đấu");
        private readonly IHeroState _stateParachuteGliding = new HeroSimpleState("🪂 Đang Bay Dù Hạ Cánh");
        private readonly IHeroState _stateDashing = new HeroSimpleState("💨 Đang Lướt (Dash)");
        private readonly IHeroState _stateStealthed = new HeroSimpleState("🌿 Ẩn Thân Bụi Cỏ");
        private readonly IHeroState _stateDead = new HeroSimpleState("💀 Đang Hồi Sinh");

        public Vector3 WorldPositionXZ => GlobalPosition;
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
        public bool IsParachuteGliding { get; private set; }
        public bool IsVisibleInFogOfWar { get; private set; } = true;

        public WeaponType CurrentWeaponType { get; private set; } = WeaponType.Unarmed;
        public WeaponLoadout CurrentLoadout { get; private set; } = WeaponLoadout.Get(WeaponType.Unarmed);

        public float CooldownBasic { get; private set; }
        public float CooldownQ { get; private set; }
        public float CooldownW { get; private set; }
        public float CooldownE { get; private set; }
        public float CooldownFlash { get; private set; }

        private float _baseMoveSpeed = 7.2f;
        private float _speedBuffTimer;
        private Vector3 _dashVelocity;
        private float _dashTimer;
        private float _zoneTickTimer;
        private float _respawnTimer;

        private MeshInstance3D _bodyMesh;
        private MeshInstance3D _visorMesh;
        private MeshInstance3D _weaponHandMesh;
        private MeshInstance3D _shieldSphereMesh;
        private MeshInstance3D _parachuteCanopyMesh;
        private StandardMaterial3D _bodyMat;
        private StandardMaterial3D _weaponMat;
        private Label3D _overheadLabel;
        private string _lastOverheadText = "";

        public void SetupHero(int viewId, string heroName, bool isLocalHuman, bool isAiBot, int teamId, Vector3 spawnPos, Color bodyColor)
        {
            HeroName = heroName;
            IsLocalHumanPlayer = isLocalHuman;
            IsAiBot = isAiBot;
            TeamId = teamId;
            GlobalPosition = new Vector3(spawnPos.X, 0f, spawnPos.Z);

            // 1. Khóa cứng mặt phẳng 3D XZ (Y = 0) trên Jolt Physics 3D
            PlanarConstraintXZ.ApplyGroundLock(this);

            CollisionShape3D col = new()
            {
                Shape = new CapsuleShape3D { Radius = 0.55f, Height = 1.8f },
                Position = new Vector3(0, 0.9f, 0)
            };
            AddChild(col);

            // 2. Thân hình 3D (Dùng Hardware Z-Buffer của GPU)
            _bodyMesh = new MeshInstance3D();
            _bodyMat = new StandardMaterial3D
            {
                AlbedoColor = bodyColor,
                Roughness = 0.45f,
                Transparency = BaseMaterial3D.TransparencyEnum.Disabled
            };
            _bodyMesh.Mesh = new CapsuleMesh { Radius = 0.52f, Height = 1.75f };
            _bodyMesh.MaterialOverride = _bodyMat;
            _bodyMesh.Position = new Vector3(0, 0.9f, 0);
            AddChild(_bodyMesh);

            _visorMesh = new MeshInstance3D
            {
                Mesh = new BoxMesh { Size = new Vector3(0.65f, 0.22f, 0.45f) },
                MaterialOverride = new StandardMaterial3D { AlbedoColor = new Color(0.1f, 0.12f, 0.16f) },
                Position = new Vector3(0, 1.35f, 0.38f)
            };
            AddChild(_visorMesh);

            _weaponHandMesh = new MeshInstance3D();
            _weaponMat = new StandardMaterial3D
            {
                AlbedoColor = Colors.White,
                EmissionEnabled = true,
                Emission = Colors.Orange,
                EmissionEnergyMultiplier = 0.8f
            };
            _weaponHandMesh.Mesh = new BoxMesh { Size = new Vector3(0.22f, 0.22f, 1.35f) };
            _weaponHandMesh.MaterialOverride = _weaponMat;
            _weaponHandMesh.Position = new Vector3(0.68f, 1.0f, 0.55f);
            _weaponHandMesh.Visible = false;
            AddChild(_weaponHandMesh);

            _shieldSphereMesh = new MeshInstance3D
            {
                Mesh = new SphereMesh { Radius = 1.15f, Height = 2.3f },
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(0.2f, 0.85f, 1.0f, 0.28f),
                    Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                    ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded
                },
                Position = new Vector3(0, 1.0f, 0),
                Visible = false
            };
            AddChild(_shieldSphereMesh);

            _overheadLabel = new Label3D
            {
                PixelSize = 0.0105f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                OutlineSize = 0,
                Position = new Vector3(0, 2.55f, 0)
            };
            AddChild(_overheadLabel);

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

            // 3. Gắn GodotPhotonView & GodotPhotonTransformView3D
            PhotonView = new GodotPhotonView
            {
                Name = "GodotPhotonView",
                ViewID = viewId,
                OwnerActorNr = isLocalHuman ? (GodotPhotonService.Instance?.LocalPlayer?.ActorNumber ?? 1) : (isAiBot ? 0 : viewId / 1000),
                CreatorActorNr = isAiBot ? 0 : (isLocalHuman ? 1 : viewId / 1000)
            };
            AddChild(PhotonView);

            TransformView = new GodotPhotonTransformView3D { Name = "GodotPhotonTransformView3D" };
            AddChild(TransformView);

            // 4. Gắn Component Điều khiển độc lập theo đúng vai trò (Local Input vs AI Bot vs Remote Network)
            if (isLocalHuman)
            {
                AddChild(new HeroLocalInputController { Name = "HeroLocalInputController" });
            }
            else if (isAiBot)
            {
                AddChild(new HeroAiBotController { Name = "HeroAiBotController" });
            }

            PhotonView.RefreshObservedAndRpcTargets();
            GodotPhotonService.Instance?.RegisterPhotonView(PhotonView);

            StateMachine.Initialize(_stateCombatReady);
            Progression.TryEquipBetterPiece(EquipmentPiece.Generate(ArmorSlot.Boots, ItemRarity.Common));
            Progression.TryEquipBetterPiece(EquipmentPiece.Generate(ArmorSlot.Chest, ItemRarity.Common));
            ApplyWeaponLoadoutLocal(isLocalHuman ? WeaponType.Greatsword : WeaponType.Unarmed);
            UpdateOverheadInfo();
        }

        public float GetScaledDamage(float baseDamage) => baseDamage + Progression.TotalBonusAttack;
        public float GetScaledCooldown(float baseCooldown) => MathF.Max(0.15f, baseCooldown * (1f - Progression.TotalCdr));

        public float GetSlotCooldown(int slot) => slot switch
        {
            0 => CooldownBasic,
            1 => CooldownQ,
            2 => CooldownW,
            3 => CooldownE,
            _ => 0f
        };

        public void TriggerSkillSlotRpc(int slot, Vector3 aimWorld, float extraAiCooldown = 0f)
        {
            switch (slot)
            {
                case 0: CooldownBasic = GetScaledCooldown(CurrentLoadout.BasicAttack.Cooldown) + extraAiCooldown; break;
                case 1: CooldownQ = GetScaledCooldown(CurrentLoadout.SkillQ.Cooldown) + extraAiCooldown; break;
                case 2: CooldownW = GetScaledCooldown(CurrentLoadout.SkillW.Cooldown) + extraAiCooldown; break;
                case 3: CooldownE = GetScaledCooldown(CurrentLoadout.SkillE.Cooldown) + extraAiCooldown; break;
            }
            PhotonView?.RPC(nameof(RpcCastSkill), RpcTarget.All, slot, GlobalPosition, aimWorld);
        }

        public void StepParachuteGlide(Vector3 moveDir, float dt)
        {
            float speed = _baseMoveSpeed + CurrentLoadout.MoveSpeedBonus + Progression.TotalBonusSpeed;
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
                PlanarConstraintXZ.ApplyGroundLock(this);
                ClampInsideMapBounds();
                ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 3.6f, new Color(0.25f, 0.9f, 1.0f), 0.45f);
                ArenaEventBus.RaiseFloatingText(GlobalPosition, "🪂 HẠ CÁNH AN TOÀN!", new Color(0.35f, 1f, 0.65f));
            }
        }

        public void StepLocomotion(Vector3 moveDir, float dt)
        {
            float speed = _baseMoveSpeed + CurrentLoadout.MoveSpeedBonus + Progression.TotalBonusSpeed + (_speedBuffTimer > 0f ? 2.8f : 0f);
            if (_dashTimer > 0f)
            {
                _dashTimer -= dt;
                Velocity = _dashVelocity;
            }
            else
            {
                Velocity = moveDir.LengthSquared() > 0.01f ? moveDir * speed : Vector3.Zero;
            }
            MoveAndSlide();
            ClampInsideMapBounds();
        }

        public void SwapMainAndSecondaryWeapon()
        {
            WeaponType oldMain = CurrentWeaponType;
            WeaponType nextMain = Progression.SecondaryWeapon != WeaponType.Unarmed ? Progression.SecondaryWeapon : WeaponType.RangerBow;
            Progression.SecondaryWeapon = oldMain != WeaponType.Unarmed ? oldMain : WeaponType.Greatsword;
            PhotonView?.RPC(nameof(RpcEquipWeapon), RpcTarget.All, (int)nextMain);
            ArenaEventBus.RaiseFloatingText(GlobalPosition, $"🔄 ĐỔI VŨ KHÍ: {WeaponLoadout.Get(nextMain).Title}", new Color(1f, 0.9f, 0.3f));
        }

        public void TryConsumeHealthPotion()
        {
            if (Progression.HealthPotions <= 0 || Health >= MaxHealth) return;
            Progression.HealthPotions--;
            Health = MathF.Min(MaxHealth, Health + 95f);
            ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 1.8f, Colors.LimeGreen, 0.3f);
            ArenaEventBus.RaiseFloatingText(GlobalPosition, $"🧪 +95 HP (Còn {Progression.HealthPotions} bình)", Colors.LimeGreen);
        }

        public void TryPlaceScoutWard(Vector3 wardPos)
        {
            if (Progression.ScoutWards <= 0) return;
            Progression.ScoutWards--;
            ArenaGameManager.Instance?.PlaceScoutWard(TeamId, wardPos);
            ArenaEventBus.RaiseFloatingText(GlobalPosition, $"👁️ CẮM MẮT SOI TẦM NHÌN (Còn {Progression.ScoutWards})", Colors.Yellow);
        }

        public void TryCastFlash(Vector3 flashDir)
        {
            if (CooldownFlash > 0f) return;
            CooldownFlash = 10.0f;
            ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 1.8f, Colors.Gold, 0.25f);
            GlobalPosition += flashDir * 7.5f;
            ClampInsideMapBounds();
            ArenaGameManager.Instance?.SpawnAoeRingEffect(GlobalPosition, 2.2f, Colors.Gold, 0.3f);
            ArenaEventBus.RaiseFloatingText(GlobalPosition, "⚡ TỐC BIẾN!", Colors.Gold);
        }

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
                    PhotonView?.RPC(nameof(RpcRespawnHero), RpcTarget.All, respawnPos);
                }
                UpdateOverheadInfo();
                return;
            }

            if (!IsLocalHumanPlayer && IsParachuteGliding && GlobalPosition.Y <= 0.12f)
            {
                IsParachuteGliding = false;
                if (_parachuteCanopyMesh != null) _parachuteCanopyMesh.Visible = false;
            }

            bool insideBush = ArenaGameManager.Instance != null && ArenaGameManager.Instance.IsInsideAnyBush(GlobalPosition);
            bool revealedByWard = ArenaGameManager.Instance != null && ArenaGameManager.Instance.IsRevealedByEnemyWard(GlobalPosition, TeamId);
            InStealthBush = insideBush && !revealedByWard && !IsParachuteGliding;

            if (IsParachuteGliding)
                StateMachine.TransitionTo(_stateParachuteGliding);
            else if (_dashTimer > 0f)
                StateMachine.TransitionTo(_stateDashing);
            else if (InStealthBush)
                StateMachine.TransitionTo(_stateStealthed);
            else
                StateMachine.TransitionTo(_stateCombatReady);

            StateMachine.Tick(dt);

            if (GodotCullArea.Instance != null && PhotonView != null)
            {
                if (IsLocalHumanPlayer)
                    GodotCullArea.Instance.UpdatePlayerCulling(GlobalPosition, PhotonView);
                else
                    PhotonView.Group = GodotCullArea.Instance.GetCellIdFromPosition(GlobalPosition);
            }

            if (SafeZoneController.Instance != null && SafeZoneController.Instance.IsOutsideSafeZone(GlobalPosition))
            {
                _zoneTickTimer += dt;
                if (_zoneTickTimer >= 1.0f)
                {
                    _zoneTickTimer = 0f;
                    ApplyDamageFromSkill(SafeZoneController.Instance.DamagePerSecondOutside, 0);
                }
            }
            else
            {
                _zoneTickTimer = 0f;
            }

            UpdateVisualVisibilityAndOverhead();
        }

        private void ClampInsideMapBounds()
        {
            Velocity = PlanarConstraintXZ.ClampVelocityXZ(Velocity);
            GlobalPosition = PlanarConstraintXZ.ClampPositionXZ(GlobalPosition, 82.5f);
        }

        public void SetFogOfWarVisibility(bool isVisibleToLocalTeam)
        {
            IsVisibleInFogOfWar = IsLocalHumanPlayer || isVisibleToLocalTeam;
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
                _weaponHandMesh.Visible = newType != WeaponType.Unarmed && IsVisibleInFogOfWar;
                _weaponMat.AlbedoColor = CurrentLoadout.ThemeColor;
                _weaponMat.Emission = CurrentLoadout.ThemeColor;
            }

            CooldownBasic = 0f;
            CooldownQ = 0f;
            CooldownW = 0f;
            CooldownE = 0f;
        }

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
            if (slot == 0)
            {
                Vector3 hitCenter = origin + dir * 2.1f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(hitCenter, 2.2f, color, 0.22f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, hitCenter, 2.3f, GetScaledDamage(CurrentLoadout.BasicAttack.Damage));
            }
            else if (slot == 1)
            {
                _dashVelocity = dir * 24.0f;
                _dashTimer = 0.26f;
                Vector3 slashCenter = origin + dir * 3.5f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(slashCenter, 2.8f, color, 0.3f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, slashCenter, 3.0f, GetScaledDamage(CurrentLoadout.SkillQ.Damage));
            }
            else if (slot == 2)
            {
                ArenaGameManager.Instance.SpawnAoeRingEffect(origin, 4.5f, new Color(1f, 0.25f, 0.1f), 0.45f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, origin, 4.5f, GetScaledDamage(CurrentLoadout.SkillW.Damage));
            }
            else if (slot == 3)
            {
                Shield = MathF.Min(120f, Shield + 75f);
                _speedBuffTimer = 4.0f;
                ArenaGameManager.Instance.SpawnAoeRingEffect(origin, 2.0f, Colors.Cyan, 0.35f);
            }
        }

        private void ExecuteRangerBowSkill(int slot, int myViewId, Vector3 origin, Vector3 targetPos, Vector3 dir, Color color, float lag)
        {
            Vector3 muzzle = origin + new Vector3(0, 1.0f, 0) + dir * 0.8f;
            if (slot == 0)
            {
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 26f, CurrentLoadout.BasicAttack.Range, GetScaledDamage(CurrentLoadout.BasicAttack.Damage), 0f, color, lag);
            }
            else if (slot == 1)
            {
                for (int i = -2; i <= 2; i++)
                {
                    float angleOffset = i * Mathf.DegToRad(11f);
                    Vector3 rotatedDir = dir.Rotated(Vector3.Up, angleOffset);
                    ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, rotatedDir, 24f, CurrentLoadout.SkillQ.Range, GetScaledDamage(CurrentLoadout.SkillQ.Damage), 0f, color, lag);
                }
            }
            else if (slot == 2)
            {
                _dashVelocity = -dir * 20.0f;
                _dashTimer = 0.24f;
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 25f, CurrentLoadout.SkillW.Range, GetScaledDamage(CurrentLoadout.SkillW.Damage), 2.2f, Colors.Yellow, lag);
            }
            else if (slot == 3)
            {
                Vector3 clampedTarget = origin + (targetPos - origin).LimitLength(CurrentLoadout.SkillE.Range);
                ArenaGameManager.Instance.SpawnAoeRingEffect(clampedTarget, 4.2f, color, 0.55f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, clampedTarget, 4.2f, GetScaledDamage(CurrentLoadout.SkillE.Damage));
            }
        }

        private void ExecuteFireStaffSkill(int slot, int myViewId, Vector3 origin, Vector3 targetPos, Vector3 dir, Color color, float lag)
        {
            Vector3 muzzle = origin + new Vector3(0, 1.05f, 0) + dir * 0.8f;
            if (slot == 0)
            {
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 21f, CurrentLoadout.BasicAttack.Range, GetScaledDamage(CurrentLoadout.BasicAttack.Damage), 0f, color, lag);
            }
            else if (slot == 1)
            {
                ArenaGameManager.Instance.SpawnProjectile(myViewId, muzzle, dir, 18f, CurrentLoadout.SkillQ.Range, GetScaledDamage(CurrentLoadout.SkillQ.Damage), 3.6f, new Color(1f, 0.2f, 0.05f), lag);
            }
            else if (slot == 2)
            {
                Vector3 clampedTarget = origin + (targetPos - origin).LimitLength(CurrentLoadout.SkillW.Range);
                ArenaGameManager.Instance.SpawnAoeRingEffect(clampedTarget, 4.8f, new Color(0.95f, 0.15f, 0.8f), 0.6f);
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, clampedTarget, 4.8f, GetScaledDamage(CurrentLoadout.SkillW.Damage));
            }
            else if (slot == 3)
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
                ArenaGameManager.Instance.ApplyAoeDamage(myViewId, hitPos, 1.6f, GetScaledDamage(CurrentLoadout.BasicAttack.Damage));
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
            GlobalPosition = new Vector3(spawnPos.X, 0f, spawnPos.Z);
            Health = MaxHealth;
            Shield = 30f;
        }

        private void UpdateVisualVisibilityAndOverhead()
        {
            bool showModel = !IsDead && IsVisibleInFogOfWar;

            if (_shieldSphereMesh != null)
            {
                _shieldSphereMesh.Visible = showModel && Shield > 0.5f;
            }
            if (_visorMesh != null)
            {
                _visorMesh.Visible = showModel;
            }
            if (_weaponHandMesh != null)
            {
                _weaponHandMesh.Visible = showModel && CurrentWeaponType != WeaponType.Unarmed;
            }
            if (_bodyMesh != null && _bodyMat != null)
            {
                _bodyMesh.Visible = showModel;
                _bodyMat.Transparency = InStealthBush
                    ? BaseMaterial3D.TransparencyEnum.Alpha
                    : BaseMaterial3D.TransparencyEnum.Disabled;

                Color c = _bodyMat.AlbedoColor;
                c.A = InStealthBush ? 0.38f : 1.0f;
                _bodyMat.AlbedoColor = c;
            }
            if (_overheadLabel != null)
            {
                _overheadLabel.Visible = IsLocalHumanPlayer || IsVisibleInFogOfWar || IsDead;
            }

            UpdateOverheadInfo();
        }

        private void UpdateOverheadInfo()
        {
            if (_overheadLabel == null || !_overheadLabel.Visible) return;

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
