namespace SurvivalHeroes.Core.Interfaces
{
    /// <summary>
    /// [ISP - Interface Segregation] Giao diện cho thực thể có khả năng phát nổ dây chuyền khi bị phá hủy.
    /// </summary>
    public interface IExplodable
    {
        float ExplosionRadius { get; }
        float ExplosionDamage { get; }
        void TriggerExplosion(int instigatorViewId);
    }
}
