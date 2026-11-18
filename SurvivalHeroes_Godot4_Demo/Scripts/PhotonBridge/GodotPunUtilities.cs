// ----------------------------------------------------------------------------
// GodotPunUtilities.cs
// [TẦNG 3 - HẠ TẦNG MẠNG DÙNG CHUNG]:
// Chỉ chứa bộ chia lưới không gian tổng quát (GodotCullArea) ánh xạ tọa độ sang
// InterestGroup (1..N) của Photon.
// Không chứa bất kỳ logic Gameplay cụ thể nào (Vòng Bo SafeZone đã được tách
// sang Tầng 5: Scripts/Gameplay/World/SafeZoneController.cs).
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using System;
    using System.Collections.Generic;
    using Godot;

    /// <summary>
    /// Hạ tầng chia bản đồ thành lưới không gian (Spatial Interest Management Grid).
    /// Nhận cấu hình kích thước (MapSize, GridColumns, GridRows) truyền từ tầng Gameplay xuống.
    /// </summary>
    public partial class GodotCullArea : Node3D
    {
        public static GodotCullArea Instance { get; private set; }

        public Vector2 MapSize { get; private set; } = new(100f, 100f);
        public int GridColumns { get; private set; } = 3;
        public int GridRows { get; private set; } = 3;

        public byte LocalPlayerCellId { get; private set; } = 5;
        public readonly List<byte> ActiveInterestGroups = new();

        public override void _EnterTree()
        {
            Instance = this;
        }

        public void ConfigureGrid(Vector2 mapSize, int columns, int rows)
        {
            MapSize = new Vector2(MathF.Max(1f, mapSize.X), MathF.Max(1f, mapSize.Y));
            GridColumns = Math.Max(1, columns);
            GridRows = Math.Max(1, rows);
        }

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
                        if (Math.Abs(r - centerRow) + Math.Abs(c - centerCol) <= 1)
                        {
                            result.Add((byte)(r * GridColumns + c + 1));
                        }
                    }
                }
            }
            return result;
        }

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
}
