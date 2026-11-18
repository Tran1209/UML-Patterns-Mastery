namespace SurvivalHeroes.Gameplay.Hero
{
    using System;
    using Godot;
    using Photon.Pun;
    using SurvivalHeroes.Core.Command;
    using SurvivalHeroes.Core.Events;

    /// <summary>
    /// [SRP - Single Responsibility Component]:
    /// Bộ điều khiển nhận Input Bàn phím / Chuột độc lập, CHỈ gắn vào nhân vật của người chơi cục bộ (LocalHero).
    /// Tách rời hoàn toàn khỏi logic AI Bot và Remote Network Player!
    /// </summary>
    public partial class HeroLocalInputController : Node
    {
        private HeroCharacter _hero;
        private bool _tabWasPressed;
        private bool _key4WasPressed;
        private bool _key5WasPressed;
        private bool _spaceWasPressed;

        public override void _Ready()
        {
            _hero = GetParent() as HeroCharacter;
        }

        public override void _PhysicsProcess(double delta)
        {
            if (_hero == null || !_hero.IsLocalHumanPlayer || _hero.IsDead) return;
            float dt = (float)delta;

            Vector3 moveDir = Vector3.Zero;
            if (Input.IsKeyPressed(Key.W) || Input.IsKeyPressed(Key.Up)) moveDir.Z -= 1f;
            if (Input.IsKeyPressed(Key.S) || Input.IsKeyPressed(Key.Down)) moveDir.Z += 1f;
            if (Input.IsKeyPressed(Key.A) || Input.IsKeyPressed(Key.Left)) moveDir.X -= 1f;
            if (Input.IsKeyPressed(Key.D) || Input.IsKeyPressed(Key.Right)) moveDir.X += 1f;

            // Nếu đang bay Dù Lượn 3D từ trên cao xuống: lái dù bằng WASD
            if (_hero.IsParachuteGliding)
            {
                _hero.StepParachuteGlide(moveDir, dt);
                return;
            }

            _hero.StepLocomotion(moveDir, dt);

            // Xoay mặt Tướng theo vị trí con trỏ chuột trên mặt phẳng XZ
            Vector3 aimWorld = ArenaGameManager.Instance != null
                ? ArenaGameManager.Instance.GetMouseGroundPosition(_hero.GlobalPosition)
                : _hero.GlobalPosition + Vector3.Forward;

            Vector3 lookDiff = new(aimWorld.X - _hero.GlobalPosition.X, 0f, aimWorld.Z - _hero.GlobalPosition.Z);
            if (lookDiff.LengthSquared() > 0.05f)
            {
                _hero.Rotation = new Vector3(0f, MathF.Atan2(lookDiff.X, lookDiff.Z), 0f);
            }

            // Nhặt Hộp Vũ Khí (Gửi yêu cầu lên MasterClient duyệt chống Race Condition)
            ArenaGameManager.Instance?.RequestPickupNearbyWeapon(_hero, Input.IsKeyPressed(Key.F));

            // [Phím Tab]: Tráo đổi Vũ Khí Chính <-> Vũ Khí Phụ
            bool tabDown = Input.IsKeyPressed(Key.Tab);
            if (tabDown && !_tabWasPressed)
            {
                _hero.SwapMainAndSecondaryWeapon();
            }
            _tabWasPressed = tabDown;

            // [Phím 4]: Uống Bình Máu (+95 HP)
            bool key4Down = Input.IsKeyPressed(Key.Key4);
            if (key4Down && !_key4WasPressed)
            {
                _hero.TryConsumeHealthPotion();
            }
            _key4WasPressed = key4Down;

            // [Phím 5]: Cắm Mắt Kiểm Soát (Soi Xuyên Tường & Bụi Cỏ)
            bool key5Down = Input.IsKeyPressed(Key.Key5);
            if (key5Down && !_key5WasPressed)
            {
                Vector3 wardPos = _hero.GlobalPosition + (lookDiff.LengthSquared() > 0.01f ? lookDiff.Normalized() * 2.2f : Vector3.Forward * 2.0f);
                _hero.TryPlaceScoutWard(wardPos);
            }
            _key5WasPressed = key5Down;

            // [Phím Space]: Phép bổ trợ Tốc Biến (Flash)
            bool spaceDown = Input.IsKeyPressed(Key.Space);
            if (spaceDown && !_spaceWasPressed)
            {
                Vector3 flashDir = lookDiff.LengthSquared() > 0.01f ? lookDiff.Normalized() : Vector3.Forward;
                _hero.TryCastFlash(flashDir);
            }
            _spaceWasPressed = spaceDown;

            // Phím tắt đổi nhanh Vũ khí để test (1: Đại Kiếm, 2: Cung, 3: Trượng Lửa)
            if (Input.IsKeyPressed(Key.Key1) && _hero.CurrentWeaponType != WeaponType.Greatsword)
                _hero.PhotonView?.RPC(nameof(HeroCharacter.RpcEquipWeapon), RpcTarget.All, (int)WeaponType.Greatsword);
            else if (Input.IsKeyPressed(Key.Key2) && _hero.CurrentWeaponType != WeaponType.RangerBow)
                _hero.PhotonView?.RPC(nameof(HeroCharacter.RpcEquipWeapon), RpcTarget.All, (int)WeaponType.RangerBow);
            else if (Input.IsKeyPressed(Key.Key3) && _hero.CurrentWeaponType != WeaponType.FireStaff)
                _hero.PhotonView?.RPC(nameof(HeroCharacter.RpcEquipWeapon), RpcTarget.All, (int)WeaponType.FireStaff);

            // Tung chiêu LMB / Q / W / E qua Command Pattern InputBufferQueue (Đệm phím 180ms)
            HandleSkillInputWithBuffer(MouseButton.Left, 0, _hero.CurrentLoadout.BasicAttack.Name, aimWorld);
            HandleKeySkillWithBuffer(Key.Q, 1, $"[Q] {_hero.CurrentLoadout.SkillQ.Name}", aimWorld);
            HandleKeySkillWithBuffer(Key.W, 2, $"[W] {_hero.CurrentLoadout.SkillW.Name}", aimWorld);
            HandleKeySkillWithBuffer(Key.E, 3, $"[E] {_hero.CurrentLoadout.SkillE.Name}", aimWorld);

            _hero.SkillInputBuffer.ProcessBufferedCommands();
        }

        private void HandleSkillInputWithBuffer(MouseButton mb, int slot, string cmdName, Vector3 aimWorld)
        {
            if (!Input.IsMouseButtonPressed(mb)) return;
            TryExecuteOrBufferSlot(slot, cmdName, aimWorld);
        }

        private void HandleKeySkillWithBuffer(Key key, int slot, string cmdName, Vector3 aimWorld)
        {
            if (!Input.IsKeyPressed(key)) return;
            TryExecuteOrBufferSlot(slot, cmdName, aimWorld);
        }

        private void TryExecuteOrBufferSlot(int slot, string cmdName, Vector3 aimWorld)
        {
            float cd = _hero.GetSlotCooldown(slot);
            if (cd <= 0f)
            {
                _hero.TriggerSkillSlotRpc(slot, aimWorld);
            }
            else if (cd <= 0.18f && _hero.SkillInputBuffer.BufferedCount == 0)
            {
                _hero.SkillInputBuffer.EnqueueCommand(new DelegateCombatCommand(
                    cmdName,
                    () => _hero.GetSlotCooldown(slot) <= 0f && !_hero.IsDead,
                    () =>
                    {
                        Vector3 aim = ArenaGameManager.Instance != null
                            ? ArenaGameManager.Instance.GetMouseGroundPosition(_hero.GlobalPosition)
                            : aimWorld;
                        _hero.TriggerSkillSlotRpc(slot, aim);
                    }));
            }
        }
    }
}
