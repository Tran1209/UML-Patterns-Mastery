// ----------------------------------------------------------------------------
// SurvivalHeroesRpgSystems.cs
// Hệ thống Gameplay cốt lõi làm nên bản sắc riêng của "SURVIVAL HEROES"
// (MOBA + Battle Royale):
//   1. Kho Đồ 6 Ô Giáp (Mũ, Áo, Quần, Giày, Nhẫn, Dây Chuyền) theo 4 bậc phẩm chất:
//      Thường (Trắng) -> Hiếm (Lam) -> Sử Thi (Tím) -> Thần Bí (Vàng Cam).
//   2. Vũ Khí Chính & Vũ Khí Phụ: Bấm [Tab] tráo đổi tức thì giữa 2 bộ Vũ khí (8 chiêu!).
//   3. Vật phẩm Chiến thuật:
//      - [Phím 4] Bình Máu (Health Potion)
//      - [Phím 5] Cắm Mắt Kiểm Soát (Scout Ward) phát hiện tàng hình trong Bụi Cỏ
//      - [Phím Space] Phép bổ trợ Tốc Biến (Flash)
//   4. Quái Rừng (Jungle Monsters) & Boss Rồng Cổ Đại (Ancient Dragon) tại Di Tích Thất Lạc:
//      Đánh quái/đập hòm rớt Vàng (Gold) + EXP lên cấp (Lv.1..15) + Trang bị.
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using System;
    using System.Collections.Generic;
    using Godot;
    using SurvivalHeroes.Patterns;

    public enum ItemRarity
    {
        Common = 0,     // Trắng
        Rare = 1,       // Xanh Lam
        Epic = 2,       // Tím Sử Thi
        Legendary = 3   // Vàng Cam (Đồ Cửa Hàng Thần Bí / Rồng)
    }

    public enum ArmorSlot
    {
        Helmet = 0,     // Mũ
        Chest = 1,      // Giáp Ngực
        Legs = 2,       // Giáp Chân
        Boots = 3,      // Giày
        Ring = 4,       // Nhẫn
        Amulet = 5      // Dây Chuyền
    }

    public sealed class EquipmentPiece
    {
        public ArmorSlot Slot { get; init; }
        public ItemRarity Rarity { get; init; }
        public string Name { get; init; }
        public float BonusHp { get; init; }
        public float BonusAttack { get; init; }
        public float BonusMoveSpeed { get; init; }
        public float CooldownReduction { get; init; } // 0.0 .. 0.35

        public static Color GetRarityColor(ItemRarity rarity) => rarity switch
        {
            ItemRarity.Common => new Color(0.82f, 0.84f, 0.88f),
            ItemRarity.Rare => new Color(0.25f, 0.68f, 1.0f),
            ItemRarity.Epic => new Color(0.78f, 0.32f, 1.0f),
            ItemRarity.Legendary => new Color(1.0f, 0.72f, 0.15f),
            _ => Colors.White
        };

        public static string GetRarityTag(ItemRarity rarity) => rarity switch
        {
            ItemRarity.Common => "Thường",
            ItemRarity.Rare => "Hiếm",
            ItemRarity.Epic => "Sử Thi",
            ItemRarity.Legendary => "Thần Bí",
            _ => ""
        };

        public static string GetSlotShortName(ArmorSlot slot) => slot switch
        {
            ArmorSlot.Helmet => "🪖Mũ",
            ArmorSlot.Chest => "🥋Áo",
            ArmorSlot.Legs => "👖Quần",
            ArmorSlot.Boots => "👢Giày",
            ArmorSlot.Ring => "💍Nhẫn",
            ArmorSlot.Amulet => "📿Dây",
            _ => "Đồ"
        };

        public static EquipmentPiece Generate(ArmorSlot slot, ItemRarity rarity)
        {
            float mult = rarity switch
            {
                ItemRarity.Common => 1.0f,
                ItemRarity.Rare => 1.6f,
                ItemRarity.Epic => 2.4f,
                ItemRarity.Legendary => 3.6f,
                _ => 1.0f
            };

            string prefix = GetRarityTag(rarity);
            return slot switch
            {
                ArmorSlot.Helmet => new EquipmentPiece { Slot = slot, Rarity = rarity, Name = $"Mũ Chiến Thần ({prefix})", BonusHp = 25f * mult, CooldownReduction = 0.03f * mult },
                ArmorSlot.Chest  => new EquipmentPiece { Slot = slot, Rarity = rarity, Name = $"Giáp Hoàng Kim ({prefix})", BonusHp = 45f * mult },
                ArmorSlot.Legs   => new EquipmentPiece { Slot = slot, Rarity = rarity, Name = $"Hộ Chân Thiết Giáp ({prefix})", BonusHp = 30f * mult, BonusMoveSpeed = 0.15f * mult },
                ArmorSlot.Boots  => new EquipmentPiece { Slot = slot, Rarity = rarity, Name = $"Giày Tốc Hành ({prefix})", BonusMoveSpeed = 0.45f * mult, CooldownReduction = 0.02f * mult },
                ArmorSlot.Ring   => new EquipmentPiece { Slot = slot, Rarity = rarity, Name = $"Nhẫn Cuồng Nộ ({prefix})", BonusAttack = 6f * mult, CooldownReduction = 0.03f * mult },
                _                => new EquipmentPiece { Slot = slot, Rarity = rarity, Name = $"Dây Chuyền Hư Không ({prefix})", BonusAttack = 5f * mult, BonusHp = 20f * mult }
            };
        }
    }

    /// <summary>
    /// Quản lý Cấp độ (Lv.1..15), Vàng, Vũ khí Phụ, Bình Máu, Mắt Kiểm Soát và 6 Ô Giáp của Tướng.
    /// </summary>
    public sealed class SurvivalHeroProgression
    {
        public int Level { get; private set; } = 1;
        public float CurrentExp { get; private set; } = 0f;
        public float ExpToNextLevel => 80f + (Level - 1) * 45f;

        public int Gold { get; set; } = 150;
        public int HealthPotions { get; set; } = 2;
        public int ScoutWards { get; set; } = 2;
        public bool HasDragonBuff { get; set; }

        public WeaponType SecondaryWeapon { get; set; } = WeaponType.RangerBow;
        public readonly Dictionary<ArmorSlot, EquipmentPiece> EquippedArmor = new();

        public float TotalBonusHp { get; private set; }
        public float TotalBonusAttack { get; private set; }
        public float TotalBonusSpeed { get; private set; }
        public float TotalCdr { get; private set; }

        public bool GainExp(float expAmount)
        {
            if (Level >= 15) return false;
            CurrentExp += expAmount;
            bool leveledUp = false;
            while (CurrentExp >= ExpToNextLevel && Level < 15)
            {
                CurrentExp -= ExpToNextLevel;
                Level++;
                leveledUp = true;
            }
            RecalculateTotals();
            return leveledUp;
        }

        public bool TryEquipBetterPiece(EquipmentPiece candidate)
        {
            if (candidate == null) return false;
            if (!EquippedArmor.TryGetValue(candidate.Slot, out EquipmentPiece existing) || candidate.Rarity >= existing.Rarity)
            {
                EquippedArmor[candidate.Slot] = candidate;
                RecalculateTotals();
                return true;
            }
            return false;
        }

        public void EquipFullSet(ItemRarity rarity)
        {
            foreach (ArmorSlot slot in Enum.GetValues(typeof(ArmorSlot)))
            {
                TryEquipBetterPiece(EquipmentPiece.Generate(slot, rarity));
            }
        }

        public void RecalculateTotals()
        {
            float hp = (Level - 1) * 18f;
            float atk = (Level - 1) * 3.5f + (HasDragonBuff ? 22f : 0f);
            float spd = HasDragonBuff ? 1.0f : 0f;
            float cdr = 0f;

            foreach (var kvp in EquippedArmor)
            {
                hp += kvp.Value.BonusHp;
                atk += kvp.Value.BonusAttack;
                spd += kvp.Value.BonusMoveSpeed;
                cdr += kvp.Value.CooldownReduction;
            }

            TotalBonusHp = hp;
            TotalBonusAttack = atk;
            TotalBonusSpeed = spd;
            TotalCdr = Mathf.Clamp(cdr, 0f, 0.40f);
        }

        public string FormatArmorSummaryBar()
        {
            List<string> parts = new();
            foreach (ArmorSlot slot in Enum.GetValues(typeof(ArmorSlot)))
            {
                if (EquippedArmor.TryGetValue(slot, out EquipmentPiece piece))
                {
                    parts.Add($"{EquipmentPiece.GetSlotShortName(slot)}:[{EquipmentPiece.GetRarityTag(piece.Rarity)}]");
                }
                else
                {
                    parts.Add($"{EquipmentPiece.GetSlotShortName(slot)}:[Trống]");
                }
            }
            return string.Join("  ", parts);
        }
    }

    /// <summary>
    /// Mắt Kiểm Soát (Scout Ward) cắm xuống đất bằng [Phím 5]:
    /// Phát hiện toàn bộ kẻ địch đang tàng hình trong Bụi Cỏ trong bán kính 11m suốt 25 giây!
    /// </summary>
    public partial class ScoutWard3D : Node3D
    {
        public int OwnerTeamId { get; private set; }
        public float VisionRadius { get; private set; } = 11.0f;
        public bool IsExpired => _lifeTimer <= 0f;

        private float _lifeTimer = 25.0f;
        private MeshInstance3D _beaconMesh;

        public void Initialize(int ownerTeamId, Vector3 worldPos)
        {
            OwnerTeamId = ownerTeamId;
            GlobalPosition = new Vector3(worldPos.X, 0f, worldPos.Z);

            // Cọc mắt phát sáng vàng
            _beaconMesh = new MeshInstance3D
            {
                Mesh = new CylinderMesh { TopRadius = 0.25f, BottomRadius = 0.35f, Height = 1.4f },
                Position = new Vector3(0f, 0.7f, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(1.0f, 0.9f, 0.2f),
                    EmissionEnabled = true,
                    Emission = new Color(1.0f, 0.85f, 0.1f),
                    EmissionEnergyMultiplier = 1.4f
                }
            };
            AddChild(_beaconMesh);

            // Vòng tầm nhìn soi bụi cỏ
            MeshInstance3D ring = new()
            {
                Mesh = new CylinderMesh { TopRadius = VisionRadius, BottomRadius = VisionRadius, Height = 0.04f, RadialSegments = 36 },
                Position = new Vector3(0f, DepthSorter25D.ElevationLootPad, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = new Color(1.0f, 0.92f, 0.25f, 0.14f),
                    Transparency = BaseMaterial3D.TransparencyEnum.Alpha,
                    ShadingMode = BaseMaterial3D.ShadingModeEnum.Unshaded
                }
            };
            AddChild(ring);

            AddChild(new Label3D
            {
                Text = "👁️ Mắt Kiểm Soát (Soi Tàng Hình)",
                PixelSize = 0.01f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                Modulate = new Color(1.0f, 0.92f, 0.3f),
                Position = new Vector3(0f, 1.85f, 0f)
            });
        }

        public override void _Process(double delta)
        {
            _lifeTimer -= (float)delta;
            if (_lifeTimer <= 0f)
            {
                QueueFree();
            }
        }
    }

    /// <summary>
    /// Quái Rừng & Boss Rồng Cổ Đại (Jungle Monster & Ancient Dragon) thực thi IDamageable.
    /// Khi bị tiêu diệt: Thưởng Vàng (Gold), Kinh Nghiệm (EXP), và tự động rơi Trang Bị!
    /// </summary>
    public partial class JungleMonster3D : Node3D, IDamageable
    {
        public string MonsterTitle { get; private set; }
        public bool IsDragonBoss { get; private set; }
        public Vector3 WorldPosition25D => GlobalPosition;
        public bool IsDestroyed { get; private set; }

        private float _maxHp;
        private float _hp;
        private float _attackDamage;
        private int _goldReward;
        private float _expReward;
        private float _respawnTimer;
        private float _attackCooldown;
        private Vector3 _homePos;

        private Node3D _visualRoot;
        private Label3D _infoLabel;

        public void Initialize(string title, Vector3 spawnPos, bool isDragonBoss)
        {
            MonsterTitle = title;
            IsDragonBoss = isDragonBoss;
            _homePos = new Vector3(spawnPos.X, 0f, spawnPos.Z);
            GlobalPosition = _homePos;

            _maxHp = isDragonBoss ? 650f : 150f;
            _hp = _maxHp;
            _attackDamage = isDragonBoss ? 28f : 12f;
            _goldReward = isDragonBoss ? 350 : 75;
            _expReward = isDragonBoss ? 220f : 65f;

            _visualRoot = new Node3D();
            AddChild(_visualRoot);

            Color bodyColor = isDragonBoss ? new Color(0.85f, 0.15f, 0.95f) : new Color(0.65f, 0.42f, 0.22f);
            float scaleMult = isDragonBoss ? 2.2f : 1.0f;

            MeshInstance3D body = new()
            {
                Mesh = new BoxMesh { Size = new Vector3(1.2f, 1.5f, 1.6f) * scaleMult },
                Position = new Vector3(0f, 0.8f * scaleMult, 0f),
                MaterialOverride = new StandardMaterial3D
                {
                    AlbedoColor = bodyColor,
                    EmissionEnabled = isDragonBoss,
                    Emission = bodyColor,
                    EmissionEnergyMultiplier = 0.7f
                }
            };
            _visualRoot.AddChild(body);

            if (isDragonBoss)
            {
                // Đôi cánh Rồng Cổ Đại
                MeshInstance3D wings = new()
                {
                    Mesh = new BoxMesh { Size = new Vector3(5.8f, 0.25f, 1.8f) },
                    Position = new Vector3(0f, 2.2f, 0f),
                    MaterialOverride = new StandardMaterial3D { AlbedoColor = new Color(0.95f, 0.3f, 0.6f) }
                };
                _visualRoot.AddChild(wings);
            }

            _infoLabel = new Label3D
            {
                PixelSize = isDragonBoss ? 0.0135f : 0.0105f,
                Billboard = BaseMaterial3D.BillboardModeEnum.Enabled,
                NoDepthTest = true,
                RenderPriority = DepthSorter25D.PriorityOverheadUi,
                OutlineSize = 0,
                Position = new Vector3(0f, isDragonBoss ? 4.2f : 2.3f, 0f)
            };
            AddChild(_infoLabel);
            UpdateMonsterLabel();
        }

        public void TakeDamage(float amount, int attackerViewId)
        {
            if (IsDestroyed) return;
            _hp = MathF.Max(0f, _hp - amount);
            ArenaEventBus.RaiseFloatingText(GlobalPosition, $"-{Mathf.RoundToInt(amount)}", new Color(1f, 0.55f, 0.2f));
            UpdateMonsterLabel();

            if (_hp <= 0f)
            {
                IsDestroyed = true;
                _respawnTimer = IsDragonBoss ? 45f : 20f;
                if (_visualRoot != null) _visualRoot.Visible = false;
                UpdateMonsterLabel();

                if (ArenaGameManager.Instance != null)
                {
                    ArenaGameManager.Instance.NotifyMonsterSlain(this, attackerViewId, _goldReward, _expReward);
                }
            }
        }

        public override void _Process(double delta)
        {
            float dt = (float)delta;
            if (IsDestroyed)
            {
                _respawnTimer -= dt;
                if (_respawnTimer <= 0f)
                {
                    IsDestroyed = false;
                    _hp = _maxHp;
                    GlobalPosition = _homePos;
                    if (_visualRoot != null) _visualRoot.Visible = true;
                }
                UpdateMonsterLabel();
                return;
            }

            // Quái rừng tự động đánh trả nếu Tướng đứng sát trong bán kính 4.5m
            _attackCooldown = MathF.Max(0f, _attackCooldown - dt);
            if (_attackCooldown <= 0f && ArenaGameManager.Instance != null)
            {
                foreach (HeroCharacter hero in ArenaGameManager.Instance.AllHeroes)
                {
                    if (hero == null || hero.IsDead) continue;
                    float dist = PlanarPhysics25D.Distance25D(GlobalPosition, hero.GlobalPosition);
                    if (dist <= (IsDragonBoss ? 6.5f : 3.6f))
                    {
                        _attackCooldown = IsDragonBoss ? 1.8f : 1.5f;
                        ArenaGameManager.Instance.SpawnAoeRingEffect(hero.GlobalPosition, IsDragonBoss ? 2.8f : 1.5f, Colors.Purple, 0.28f);
                        hero.TakeDamage(_attackDamage, 0);
                        break;
                    }
                }
            }
        }

        private string _lastMonsterLabel = "";

        private void UpdateMonsterLabel()
        {
            if (_infoLabel == null) return;
            if (IsDestroyed)
            {
                string deadTxt = $"⏳ {MonsterTitle} (Hồi sinh {Mathf.CeilToInt(_respawnTimer)}s)";
                if (_lastMonsterLabel != deadTxt)
                {
                    _lastMonsterLabel = deadTxt;
                    _infoLabel.Text = deadTxt;
                    _infoLabel.Modulate = new Color(0.65f, 0.65f, 0.7f);
                }
                return;
            }

            string icon = IsDragonBoss ? "🐉 BOSS" : "🐺 Quái Rừng";
            string aliveTxt = $"{icon}: {MonsterTitle}\n❤️ {(int)_hp}/{(int)_maxHp} (+{_goldReward}💰)";
            if (_lastMonsterLabel != aliveTxt)
            {
                _lastMonsterLabel = aliveTxt;
                _infoLabel.Text = aliveTxt;
                _infoLabel.Modulate = IsDragonBoss ? new Color(1.0f, 0.45f, 0.95f) : new Color(1.0f, 0.85f, 0.45f);
            }
        }
    }
}
