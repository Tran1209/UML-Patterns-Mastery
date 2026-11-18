namespace SurvivalHeroes.Gameplay.Items
{
    using System;
    using System.Collections.Generic;
    using Godot;

    public enum ItemRarity
    {
        Common = 0,
        Rare = 1,
        Epic = 2,
        Legendary = 3
    }

    public enum ArmorSlot
    {
        Helmet = 0,
        Chest = 1,
        Legs = 2,
        Boots = 3,
        Ring = 4,
        Amulet = 5
    }

    public sealed class EquipmentPiece
    {
        public ArmorSlot Slot { get; init; }
        public ItemRarity Rarity { get; init; }
        public string Name { get; init; }
        public float BonusHp { get; init; }
        public float BonusAttack { get; init; }
        public float BonusMoveSpeed { get; init; }
        public float CooldownReduction { get; init; }

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
}
