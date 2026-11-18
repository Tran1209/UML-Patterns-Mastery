namespace SurvivalHeroes.Core.Interfaces
{
    using Godot;

    /// <summary>
    /// [DIP - Dependency Inversion] Giao diện kích hoạt công tắc/trụ/lều trừu tượng.
    /// Nhận Node3D tổng quát thay vì phụ thuộc ngược vào lớp HeroCharacter của tầng Gameplay.
    /// </summary>
    public interface ISwitchable
    {
        bool IsActive { get; }
        void Activate(Node3D activator);
        void Deactivate();
    }
}
