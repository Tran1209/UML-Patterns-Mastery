namespace SurvivalHeroes.Core.Events
{
    using System;
    using Godot;

    /// <summary>
    /// [Observer Pattern] Kênh sự kiện tĩnh tách rời hoàn toàn tầng Combat/Entities với tầng UI HUD.
    /// </summary>
    public static class ArenaEventBus
    {
        public static event Action<Vector3, string, Color> OnFloatingTextRequested;
        public static event Action<string, string> OnComboBuffered;
        public static event Action<string> OnArenaAnnouncement;

        public static void RaiseFloatingText(Vector3 worldPos, string text, Color color)
            => OnFloatingTextRequested?.Invoke(worldPos, text, color);

        public static void RaiseComboBuffered(string heroName, string skillKey)
            => OnComboBuffered?.Invoke(heroName, skillKey);

        public static void RaiseAnnouncement(string message)
            => OnArenaAnnouncement?.Invoke(message);
    }
}
