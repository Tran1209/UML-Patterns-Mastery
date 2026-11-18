namespace SurvivalHeroes.Core.Interfaces
{
    using Godot;

    /// <summary>
    /// [ISP - Interface Segregation] Giao diện nhận sát thương trong không gian 3D Isometric (mặt phẳng XZ, Y = 0).
    /// Hoàn toàn độc lập với tầng Gameplay.
    /// </summary>
    public interface IDamageable
    {
        Vector3 WorldPositionXZ { get; }
        bool IsDestroyed { get; }
        void TakeDamage(float amount, int attackerViewId);
    }
}
