namespace SurvivalHeroes.Gameplay.Hero
{
    using System;
    using Godot;
    using Photon.Pun;
    using Photon.Pun.GodotBridge;
    using Photon.Realtime;
    using SurvivalHeroes.Core.Physics;
    using SurvivalHeroes.Gameplay.Items;
    using SurvivalHeroes.Gameplay.World;

    /// <summary>
    /// [SRP - Single Responsibility Component]:
    /// Bộ não điều khiển AI Bot độc lập, CHỈ gắn vào các nhân vật AI Bot.
    /// Tự động lắng nghe sự kiện OnMasterClientChanged để tiếp quản điều khiển AI ngay lập tức
    /// khi người chơi giữ vai trò MasterClient trước đó thoát trận!
    /// </summary>
    public partial class HeroAiBotController : Node
    {
        private HeroCharacter _bot;
        private float _aiThinkTimer;
        private Vector3 _aiMoveTarget;

        public override void _Ready()
        {
            _bot = GetParent() as HeroCharacter;
            if (_bot != null)
            {
                _aiMoveTarget = _bot.GlobalPosition;
            }
            if (GodotPhotonService.Instance != null)
            {
                GodotPhotonService.Instance.OnMasterClientChanged += OnMasterClientSwitched;
            }
        }

        public override void _ExitTree()
        {
            if (GodotPhotonService.Instance != null)
            {
                GodotPhotonService.Instance.OnMasterClientChanged -= OnMasterClientSwitched;
            }
        }

        private void OnMasterClientSwitched(Player newMaster)
        {
            if (_bot == null || _bot.PhotonView == null) return;
            // Đảm bảo AI Bot luôn là RoomView (CreatorActorNr = 0, OwnerActorNr = 0)
            // để máy MasterClient mới lập tức có IsMine = true và tiếp tục điều khiển Bot!
            _bot.PhotonView.OwnerActorNr = 0;
            _bot.PhotonView.CreatorActorNr = 0;
            _aiThinkTimer = 0f;
        }

        public override void _PhysicsProcess(double delta)
        {
            if (_bot == null || !_bot.IsAiBot || _bot.IsDead) return;
            if (GodotPhotonService.Instance != null && !GodotPhotonService.Instance.IsMasterClient) return;
            if (ArenaGameManager.Instance == null) return;

            float dt = (float)delta;

            if (_bot.CurrentWeaponType == WeaponType.Unarmed)
            {
                ArenaGameManager.Instance.RequestPickupNearbyWeapon(_bot, true);
            }

            _aiThinkTimer -= dt;
            HeroCharacter nearestEnemy = ArenaGameManager.Instance.FindNearestVisibleEnemy(_bot, 19.0f);

            if (_aiThinkTimer <= 0f)
            {
                _aiThinkTimer = 0.45f;

                if (SafeZoneController.Instance != null && SafeZoneController.Instance.IsOutsideSafeZone(_bot.GlobalPosition))
                {
                    _aiMoveTarget = SafeZoneController.Instance.ZoneCenter + new Vector3((GD.Randf() - 0.5f) * 8f, 0f, (GD.Randf() - 0.5f) * 8f);
                }
                else if (_bot.CurrentWeaponType == WeaponType.Unarmed)
                {
                    WeaponLootPickup3D crate = ArenaGameManager.Instance.FindNearestAvailableWeaponCrate(_bot.GlobalPosition);
                    if (crate != null) _aiMoveTarget = crate.GlobalPosition;
                }
                else if (nearestEnemy != null)
                {
                    float desiredDist = _bot.CurrentWeaponType == WeaponType.Greatsword ? 2.2f : 9.5f;
                    Vector3 toMe = (_bot.GlobalPosition - nearestEnemy.GlobalPosition).Normalized();
                    _aiMoveTarget = nearestEnemy.GlobalPosition + toMe * desiredDist;
                }
                else
                {
                    _aiMoveTarget = _bot.GlobalPosition + new Vector3((GD.Randf() - 0.5f) * 18f, 0f, (GD.Randf() - 0.5f) * 18f);
                }
            }

            Vector3 diff = new(_aiMoveTarget.X - _bot.GlobalPosition.X, 0f, _aiMoveTarget.Z - _bot.GlobalPosition.Z);
            Vector3 moveDir = diff.Length() > 0.85f ? diff.Normalized() : Vector3.Zero;
            _bot.StepLocomotion(moveDir * 0.85f, dt);

            Vector3 lookTarget = nearestEnemy != null ? nearestEnemy.GlobalPosition : _aiMoveTarget;
            Vector3 lookVec = new(lookTarget.X - _bot.GlobalPosition.X, 0f, lookTarget.Z - _bot.GlobalPosition.Z);
            if (lookVec.LengthSquared() > 0.05f)
            {
                _bot.Rotation = new Vector3(0f, MathF.Atan2(lookVec.X, lookVec.Z), 0f);
            }

            if (nearestEnemy != null)
            {
                float distToEnemy = PlanarConstraintXZ.DistanceXZ(nearestEnemy.GlobalPosition, _bot.GlobalPosition);
                Vector3 aimPos = nearestEnemy.GlobalPosition;

                if (_bot.CooldownQ <= 0f && distToEnemy <= _bot.CurrentLoadout.SkillQ.Range)
                {
                    _bot.TriggerSkillSlotRpc(1, aimPos, 0.8f);
                }
                else if (_bot.CooldownW <= 0f && distToEnemy <= Mathf.Max(5.0f, _bot.CurrentLoadout.SkillW.Range))
                {
                    _bot.TriggerSkillSlotRpc(2, aimPos, 1.0f);
                }
                else if (_bot.CooldownBasic <= 0f && distToEnemy <= _bot.CurrentLoadout.BasicAttack.Range)
                {
                    _bot.TriggerSkillSlotRpc(0, aimPos, 0.35f);
                }
            }
        }
    }
}
