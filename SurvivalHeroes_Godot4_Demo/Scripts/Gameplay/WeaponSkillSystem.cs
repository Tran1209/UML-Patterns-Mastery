// ----------------------------------------------------------------------------
// WeaponSkillSystem.cs
// Áp dụng Strategy Pattern cho cơ chế đặc trưng nhất của Survival Heroes:
// "Nhặt Vũ khí nào -> Đổi toàn bộ 4 Kỹ năng (LMB, Q, W, E) theo Vũ khí đó!"
// ----------------------------------------------------------------------------

namespace SurvivalHeroes.Gameplay
{
    using Godot;

    public enum WeaponType
    {
        Unarmed = 0,
        Greatsword = 1,
        RangerBow = 2,
        FireStaff = 3
    }

    public struct SkillDefinition
    {
        public string Name;
        public string KeyLabel;
        public float Cooldown;
        public float Damage;
        public float Range;
        public string Description;
    }

    public class WeaponLoadout
    {
        public WeaponType Type { get; init; }
        public string Title { get; init; }
        public Color ThemeColor { get; init; }
        public float MoveSpeedBonus { get; init; }
        public SkillDefinition BasicAttack { get; init; }
        public SkillDefinition SkillQ { get; init; }
        public SkillDefinition SkillW { get; init; }
        public SkillDefinition SkillE { get; init; }

        public static WeaponLoadout Get(WeaponType type)
        {
            return type switch
            {
                WeaponType.Greatsword => new WeaponLoadout
                {
                    Type = WeaponType.Greatsword,
                    Title = "Đại Kiếm Chiến Binh",
                    ThemeColor = new Color(1.0f, 0.45f, 0.15f),
                    MoveSpeedBonus = 0.8f,
                    BasicAttack = new SkillDefinition
                    {
                        Name = "Trảm Kiếm", KeyLabel = "LMB", Cooldown = 0.55f, Damage = 26f, Range = 3.6f,
                        Description = "Chém vòng cung cận chiến phía trước"
                    },
                    SkillQ = new SkillDefinition
                    {
                        Name = "Lướt Chém", KeyLabel = "Q", Cooldown = 4.0f, Damage = 38f, Range = 7.5f,
                        Description = "Lao nhanh theo hướng chuột và chém xuyên kẻ địch"
                    },
                    SkillW = new SkillDefinition
                    {
                        Name = "Xoáy Kiếm 360°", KeyLabel = "W", Cooldown = 6.0f, Damage = 45f, Range = 4.5f,
                        Description = "Xoay kiếm bão táp gây sát thương toàn bộ xung quanh"
                    },
                    SkillE = new SkillDefinition
                    {
                        Name = "Khiên Dũng Sĩ", KeyLabel = "E", Cooldown = 9.0f, Damage = 0f, Range = 0f,
                        Description = "Nhận +75 Giáp Ảo và tăng tốc chạy"
                    }
                },
                WeaponType.RangerBow => new WeaponLoadout
                {
                    Type = WeaponType.RangerBow,
                    Title = "Cung Tuyệt Mệnh",
                    ThemeColor = new Color(0.25f, 0.95f, 0.45f),
                    MoveSpeedBonus = 1.2f,
                    BasicAttack = new SkillDefinition
                    {
                        Name = "Tên Xuyên Phá", KeyLabel = "LMB", Cooldown = 0.45f, Damage = 20f, Range = 18.0f,
                        Description = "Bắn mũi tên tốc độ cao tầm xa"
                    },
                    SkillQ = new SkillDefinition
                    {
                        Name = "Tán Xạ 5 Tên", KeyLabel = "Q", Cooldown = 4.5f, Damage = 18f, Range = 15.0f,
                        Description = "Bắn chùm 5 mũi tên hình nón về phía trước"
                    },
                    SkillW = new SkillDefinition
                    {
                        Name = "Lộn Nhào Bắn Lùi", KeyLabel = "W", Cooldown = 6.5f, Damage = 30f, Range = 14.0f,
                        Description = "Nhảy lùi về sau né đòn và bắn tên nổ"
                    },
                    SkillE = new SkillDefinition
                    {
                        Name = "Mưa Tên AOE", KeyLabel = "E", Cooldown = 8.0f, Damage = 48f, Range = 16.0f,
                        Description = "Gọi mưa tên trút xuống vị trí con trỏ chuột"
                    }
                },
                WeaponType.FireStaff => new WeaponLoadout
                {
                    Type = WeaponType.FireStaff,
                    Title = "Trượng Hỏa Long",
                    ThemeColor = new Color(0.75f, 0.3f, 1.0f),
                    MoveSpeedBonus = 0.4f,
                    BasicAttack = new SkillDefinition
                    {
                        Name = "Hỏa Cầu", KeyLabel = "LMB", Cooldown = 0.6f, Damage = 22f, Range = 15.0f,
                        Description = "Bắn cầu lửa tầm trung"
                    },
                    SkillQ = new SkillDefinition
                    {
                        Name = "Đại Hỏa Cầu Nổ", KeyLabel = "Q", Cooldown = 4.0f, Damage = 46f, Range = 17.0f,
                        Description = "Bắn cầu lửa khổng lồ phát nổ diện rộng"
                    },
                    SkillW = new SkillDefinition
                    {
                        Name = "Thiên Thạch Rơi", KeyLabel = "W", Cooldown = 7.0f, Damage = 62f, Range = 16.0f,
                        Description = "Triệu hồi thiên thạch nổ lớn tại vị trí chuột"
                    },
                    SkillE = new SkillDefinition
                    {
                        Name = "Tốc Biến Hư Không", KeyLabel = "E", Cooldown = 8.5f, Damage = 25f, Range = 9.0f,
                        Description = "Dịch chuyển tức thời tới vị trí chuột và gây nổ nhẹ"
                    }
                },
                _ => new WeaponLoadout
                {
                    Type = WeaponType.Unarmed,
                    Title = "Tay Không (Tìm Vũ Khí!)",
                    ThemeColor = new Color(0.8f, 0.8f, 0.8f),
                    MoveSpeedBonus = 1.5f,
                    BasicAttack = new SkillDefinition
                    {
                        Name = "Đấm Thường", KeyLabel = "LMB", Cooldown = 0.5f, Damage = 12f, Range = 2.6f,
                        Description = "Đấm cận chiến cơ bản"
                    },
                    SkillQ = new SkillDefinition
                    {
                        Name = "Lăn Nhào", KeyLabel = "Q", Cooldown = 3.5f, Damage = 15f, Range = 5.5f,
                        Description = "Lướt nhanh về phía trước"
                    },
                    SkillW = new SkillDefinition
                    {
                        Name = "Hồi Thở", KeyLabel = "W", Cooldown = 8.0f, Damage = 0f, Range = 0f,
                        Description = "Hồi ngay +35 Máu"
                    },
                    SkillE = new SkillDefinition
                    {
                        Name = "Giáp Tạm", KeyLabel = "E", Cooldown = 8.0f, Damage = 0f, Range = 0f,
                        Description = "Nhận +40 Giáp Ảo để chạy đi nhặt vũ khí"
                    }
                }
            };
        }
    }
}
