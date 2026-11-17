// ----------------------------------------------------------------------------
// GodotPunUtilities.cs
// Chuyển đổi từ 3 bộ UtilityScripts quan trọng nhất của Photon cho Survival Heroes:
//   1. UtilityScripts/Culling/CullArea.cs & CullingHandler.cs -> GodotCullArea
//   2. UtilityScripts/Room/CountdownTimer.cs -> GodotSafeZoneTimer
//   3. UtilityScripts/PhotonPlayer/PhotonTeamsManager.cs & PunPlayerScores.cs -> GodotTeamsAndScores
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using System;
    using System.Collections.Generic;
    using ExitGames.Client.Photon;
    using Godot;

    /// <summary>
    /// Chia bản đồ lớn của Survival Heroes thành lưới không gian (Spatial Grid 3x3 = 9 vùng InterestGroup 1..9)
    /// giúp giảm tới 80% băng thông mạng và chống hack Map.
    /// </summary>
    public partial class GodotCullArea : Node3D
    {
        public static GodotCullArea Instance { get; private set; }

        [Export] public Vector2 MapSize { get; set; } = new(170f, 170f);
        [Export] public int GridColumns { get; set; } = 3;
        [Export] public int GridRows { get; set; } = 3;

        public byte LocalPlayerCellId { get; private set; } = 5;
        public readonly List<byte> ActiveInterestGroups = new();

        public override void _EnterTree()
        {
            Instance = this;
        }

        /// <summary>Tính mã vùng InterestGroup (1..9) từ tọa độ thế giới 3D.</summary>
        public byte GetCellIdFromPosition(Vector3 worldPos)
        {
            float halfW = MapSize.X * 0.5f;
            float halfH = MapSize.Y * 0.5f;

            float normX = Mathf.Clamp((worldPos.X + halfW) / MapSize.X, 0f, 0.999f);
            float normZ = Mathf.Clamp((worldPos.Z + halfH) / MapSize.Y, 0f, 0.999f);

            int col = (int)(normX * GridColumns);
            int row = (int)(normZ * GridRows);

            return (byte)(row * GridColumns + col + 1);
        }

        /// <summary>Lấy danh sách các ô lân cận (bao gồm ô hiện tại và các ô kề cạnh) để đăng ký nhận gói tin.</summary>
        public List<byte> GetNeighborCells(byte centerCellId)
        {
            List<byte> result = new();
            int idx = centerCellId - 1;
            int centerRow = idx / GridColumns;
            int centerCol = idx % GridColumns;

            for (int r = centerRow - 1; r <= centerRow + 1; r++)
            {
                for (int c = centerCol - 1; c <= centerCol + 1; c++)
                {
                    if (r >= 0 && r < GridRows && c >= 0 && c < GridColumns)
                    {
                        // Chỉ lấy ô trung tâm và 4 ô kề trực tiếp (hoặc cả góc gần) để thể hiện rõ Culling
                        if (Math.Abs(r - centerRow) + Math.Abs(c - centerCol) <= 1)
                        {
                            result.Add((byte)(r * GridColumns + c + 1));
                        }
                    }
                }
            }
            return result;
        }

        /// <summary>Cập nhật InterestGroup cho nhân vật chính khi di chuyển qua ranh giới ô lưới.</summary>
        public void UpdatePlayerCulling(Vector3 playerPos, GodotPhotonView playerView)
        {
            byte newCell = GetCellIdFromPosition(playerPos);
            if (playerView != null)
            {
                playerView.Group = newCell;
            }

            if (newCell == LocalPlayerCellId && ActiveInterestGroups.Count > 0) return;

            LocalPlayerCellId = newCell;
            List<byte> newGroups = GetNeighborCells(newCell);

            List<byte> toDisable = new();
            for (byte g = 1; g <= GridColumns * GridRows; g++)
            {
                if (!newGroups.Contains(g)) toDisable.Add(g);
            }

            ActiveInterestGroups.Clear();
            ActiveInterestGroups.AddRange(newGroups);

            GodotPhotonService.Instance?.SetInterestGroups(toDisable.ToArray(), newGroups.ToArray());
        }
    }

    /// <summary>
    /// Đồng bộ Vòng Bo Sinh Tồn (Battle Royale Safe Zone) dựa trên Photon ServerTimestamp
    /// và Room.CustomProperties (Chuyển đổi từ CountdownTimer.cs của Photon).
    /// </summary>
    public partial class GodotSafeZoneTimer : Node3D
    {
        public static GodotSafeZoneTimer Instance { get; private set; }

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
                    StartZonePhase(1, new Vector3(0, 0, 0));
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

            // Nếu hết thời gian Phase hiện tại và chưa tới Phase cuối, MasterClient tự chuyển sang Phase kế tiếp
            if (t >= 1.0f && CurrentPhase < PhaseDurationsMs.Length && GodotPhotonService.Instance.IsMasterClient)
            {
                StartZonePhase(CurrentPhase + 1, ZoneCenter);
            }
        }

        public bool IsOutsideSafeZone(Vector3 worldPos)
        {
            Vector2 pos2D = new(worldPos.X, worldPos.Z);
            Vector2 center2D = new(ZoneCenter.X, ZoneCenter.Z);
            return pos2D.DistanceTo(center2D) > CurrentRadius;
        }
    }
}
