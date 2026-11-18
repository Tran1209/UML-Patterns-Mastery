namespace SurvivalHeroes.Gameplay.World
{
    using System;
    using ExitGames.Client.Photon;
    using Godot;
    using Photon.Pun.GodotBridge;
    using SurvivalHeroes.Core.Physics;

    /// <summary>
    /// [TẦNG 5 - GAMEPLAY WORLD]:
    /// Quản lý Vòng Bo Sinh Tồn (Battle Royale Safe Zone) đồng bộ qua Photon ServerTimestamp
    /// và Room.CustomProperties. (Đã tách khỏi Tầng 3 PhotonBridge để đảm bảo đúng phân tầng).
    /// </summary>
    public partial class SafeZoneController : Node3D
    {
        public static SafeZoneController Instance { get; private set; }

        public const string PropZonePhase = "ZonePhase";
        public const string PropZoneStartTime = "ZoneStartTime";
        public const string PropZoneDuration = "ZoneDuration";
        public const string PropStartRadius = "ZoneStartRad";
        public const string PropEndRadius = "ZoneEndRad";
        public const string PropCenter = "ZoneCenter";

        public int CurrentPhase { get; private set; } = 1;
        public float CurrentRadius { get; private set; } = 82f;
        public float TargetRadius { get; private set; } = 58f;
        public Vector3 ZoneCenter { get; private set; } = Vector3.Zero;
        public float RemainingPhaseSeconds { get; private set; } = 45f;
        public float DamagePerSecondOutside => CurrentPhase * 4.0f;

        private int _phaseStartTimestamp;
        private int _phaseDurationMs = 45000;
        private float _startRadius = 82f;

        private static readonly float[] PhaseRadii = { 82f, 58f, 36f, 20f, 8f };
        private static readonly int[] PhaseDurationsMs = { 45000, 38000, 32000, 26000 };

        public override void _EnterTree()
        {
            Instance = this;
        }

        public override void _Ready()
        {
            if (GodotPhotonService.Instance != null)
            {
                GodotPhotonService.Instance.OnRoomPropertiesChanged += OnRoomPropertiesUpdated;
                if (GodotPhotonService.Instance.IsMasterClient)
                {
                    StartZonePhase(1, Vector3.Zero);
                }
            }
        }

        public override void _ExitTree()
        {
            if (GodotPhotonService.Instance != null)
            {
                GodotPhotonService.Instance.OnRoomPropertiesChanged -= OnRoomPropertiesUpdated;
            }
        }

        public void StartZonePhase(int phase, Vector3 center)
        {
            int idx = Mathf.Clamp(phase - 1, 0, PhaseDurationsMs.Length - 1);
            float startR = PhaseRadii[idx];
            float endR = PhaseRadii[idx + 1];
            int durationMs = PhaseDurationsMs[idx];
            int nowServer = GodotPhotonService.Instance?.ServerTimestamp ?? (int)Time.GetTicksMsec();

            Hashtable props = new()
            {
                [PropZonePhase] = phase,
                [PropZoneStartTime] = nowServer,
                [PropZoneDuration] = durationMs,
                [PropStartRadius] = startR,
                [PropEndRadius] = endR,
                [PropCenter] = center
            };

            GodotPhotonService.Instance?.SetRoomCustomProperties(props);
        }

        private void OnRoomPropertiesUpdated(Hashtable changedProps)
        {
            var allProps = GodotPhotonService.Instance.SharedRoomProperties;
            if (allProps.ContainsKey(PropZonePhase)) CurrentPhase = (int)allProps[PropZonePhase];
            if (allProps.ContainsKey(PropZoneStartTime)) _phaseStartTimestamp = (int)allProps[PropZoneStartTime];
            if (allProps.ContainsKey(PropZoneDuration)) _phaseDurationMs = (int)allProps[PropZoneDuration];
            if (allProps.ContainsKey(PropStartRadius)) _startRadius = (float)allProps[PropStartRadius];
            if (allProps.ContainsKey(PropEndRadius)) TargetRadius = (float)allProps[PropEndRadius];
            if (allProps.ContainsKey(PropCenter)) ZoneCenter = (Vector3)allProps[PropCenter];
        }

        public override void _Process(double delta)
        {
            if (GodotPhotonService.Instance == null) return;

            int elapsedMs = GodotPhotonService.Instance.ServerTimestamp - _phaseStartTimestamp;
            float t = Mathf.Clamp((float)elapsedMs / Math.Max(1, _phaseDurationMs), 0f, 1f);

            CurrentRadius = Mathf.Lerp(_startRadius, TargetRadius, t);
            RemainingPhaseSeconds = MathF.Max(0f, (_phaseDurationMs - elapsedMs) / 1000f);

            if (t >= 1.0f && CurrentPhase < PhaseDurationsMs.Length && GodotPhotonService.Instance.IsMasterClient)
            {
                StartZonePhase(CurrentPhase + 1, ZoneCenter);
            }
        }

        public bool IsOutsideSafeZone(Vector3 worldPos)
            => PlanarConstraintXZ.DistanceXZ(worldPos, ZoneCenter) > CurrentRadius;
    }
}
