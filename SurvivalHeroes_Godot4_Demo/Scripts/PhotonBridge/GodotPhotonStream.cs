// ----------------------------------------------------------------------------
// GodotPhotonStream.cs
// Chuyển đổi từ: PhotonUnityNetworking/Code/PunClasses.cs & Interfaces/IPunCallbacks.cs
// Chức năng: Cung cấp [PunRPC], PhotonMessageInfo, PunEvent, PhotonStream,
// IPunObservable, và GodotScenePool (thay thế DefaultPool dùng Resources.Load của Unity).
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using System;
    using System.Collections.Generic;
    using Godot;
    using Photon.Realtime;

    /// <summary>Đánh dấu phương thức C# trên Node có thể được gọi từ xa qua Photon RPC.</summary>
    [AttributeUsage(AttributeTargets.Method, Inherited = true)]
    public sealed class PunRPC : Attribute
    {
    }

    /// <summary>Giao diện cho các Node muốn đồng bộ trạng thái liên tục qua PhotonStream.</summary>
    public interface IPunObservable
    {
        void OnPhotonSerializeView(PhotonStream stream, PhotonMessageInfo info);
    }

    /// <summary>Giao diện quản lý khởi tạo/hủy Scene qua mạng trong Godot 4.</summary>
    public interface IGodotPunScenePool
    {
        Node Instantiate(string scenePath, Vector3 position, Quaternion rotation);
        void Destroy(Node node);
    }

    /// <summary>Thông tin gói tin đính kèm khi nhận RPC hoặc OnPhotonSerializeView.</summary>
    public readonly struct PhotonMessageInfo
    {
        private readonly int _timeInt;
        public readonly Player Sender;
        public readonly GodotPhotonView PhotonView;

        public PhotonMessageInfo(Player player, int timestamp, GodotPhotonView view)
        {
            Sender = player;
            _timeInt = timestamp;
            PhotonView = view;
        }

        public double SentServerTime => ((uint)_timeInt) / 1000.0d;
        public int SentServerTimestamp => _timeInt;

        public override string ToString()
        {
            return $"[PhotonMessageInfo: Sender='{Sender}' SentTime={SentServerTime:F3}]";
        }
    }

    /// <summary>Mã sự kiện nội bộ của giao thức PUN trên Photon Cloud.</summary>
    public static class PunEvent
    {
        public const byte RPC = 200;
        public const byte SendSerialize = 201;
        public const byte Instantiation = 202;
        public const byte CloseConnection = 203;
        public const byte Destroy = 204;
        public const byte RemoveCachedRPCs = 205;
        public const byte SendSerializeReliable = 206;
        public const byte DestroyPlayer = 207;
        public const byte OwnershipRequest = 209;
        public const byte OwnershipTransfer = 210;
        public const byte VacantViewIds = 211;
        public const byte OwnershipUpdate = 212;
    }

    /// <summary>Luồng đọc/ghi tuần tự dữ liệu mạng cho OnPhotonSerializeView.</summary>
    public class PhotonStream
    {
        private List<object> _writeData;
        private object[] _readData;
        private int _currentItem;

        public bool IsWriting { get; private set; }
        public bool IsReading => !IsWriting;
        public int Count => IsWriting ? _writeData.Count : (_readData?.Length ?? 0);

        public PhotonStream(bool write, object[] incomingData)
        {
            IsWriting = write;
            if (!write && incomingData != null)
            {
                _readData = incomingData;
            }
            else if (write)
            {
                _writeData = new List<object>(16);
            }
        }

        public void SetReadStream(object[] incomingData, int pos = 0)
        {
            _readData = incomingData;
            _currentItem = pos;
            IsWriting = false;
        }

        internal void SetWriteStream(List<object> newWriteData, int pos = 0)
        {
            _writeData = newWriteData;
            _currentItem = pos;
            IsWriting = true;
        }

        internal List<object> GetWriteStream() => _writeData;

        public object ReceiveNext()
        {
            if (IsWriting)
            {
                GD.PrintErr("[PhotonStream] Không thể đọc (ReceiveNext) khi luồng đang ở chế độ ghi (IsWriting)!");
                return null;
            }
            if (_readData == null || _currentItem >= _readData.Length)
            {
                GD.PrintErr("[PhotonStream] ReceiveNext vượt quá số phần tử trong gói tin!");
                return null;
            }
            return _readData[_currentItem++];
        }

        public object PeekNext()
        {
            if (IsWriting || _readData == null || _currentItem >= _readData.Length)
            {
                return null;
            }
            return _readData[_currentItem];
        }

        public void SendNext(object obj)
        {
            if (!IsWriting)
            {
                GD.PrintErr("[PhotonStream] Không thể ghi (SendNext) khi luồng đang ở chế độ đọc (IsReading)!");
                return;
            }
            _writeData.Add(obj);
        }

        public object[] ToArray() => IsWriting ? _writeData.ToArray() : _readData;

        public void Serialize(ref bool val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (bool)_readData[_currentItem++];
        }

        public void Serialize(ref int val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (int)_readData[_currentItem++];
        }

        public void Serialize(ref float val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (float)_readData[_currentItem++];
        }

        public void Serialize(ref string val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (string)_readData[_currentItem++];
        }

        public void Serialize(ref Vector2 val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (Vector2)_readData[_currentItem++];
        }

        public void Serialize(ref Vector3 val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (Vector3)_readData[_currentItem++];
        }

        public void Serialize(ref Quaternion val)
        {
            if (IsWriting) _writeData.Add(val);
            else if (_readData.Length > _currentItem) val = (Quaternion)_readData[_currentItem++];
        }
    }

    /// <summary>
    /// Bộ nạp PackedScene mặc định của Godot 4 thay thế cho DefaultPool (Resources.Load) của Unity.
    /// Hỗ trợ truyền đường dẫn đầy đủ "res://..." hoặc tên ngắn trong thư mục cấu hình.
    /// </summary>
    public class GodotScenePool : IGodotPunScenePool
    {
        public readonly Dictionary<string, PackedScene> SceneCache = new();
        public string DefaultPrefabDirectory { get; set; } = "res://Prefabs/";

        public Node Instantiate(string sceneId, Vector3 position, Quaternion rotation)
        {
            string resPath = sceneId.StartsWith("res://", StringComparison.Ordinal)
                ? sceneId
                : $"{DefaultPrefabDirectory.TrimEnd('/')}/{sceneId}.tscn";

            if (!SceneCache.TryGetValue(resPath, out PackedScene packedScene))
            {
                packedScene = GD.Load<PackedScene>(resPath);
                if (packedScene == null)
                {
                    GD.PrintErr($"[GodotScenePool] Không tìm thấy PackedScene tại '{resPath}'.");
                    return null;
                }
                SceneCache[resPath] = packedScene;
            }

            Node instance = packedScene.Instantiate();
            if (instance is Node3D node3D)
            {
                node3D.Position = position;
                node3D.Quaternion = rotation;
            }
            else if (instance is Node2D node2D)
            {
                node2D.Position = new Vector2(position.X, position.Y);
                node2D.Rotation = rotation.GetEuler().Z;
            }
            return instance;
        }

        public void Destroy(Node node)
        {
            if (GodotObject.IsInstanceValid(node))
            {
                node.QueueFree();
            }
        }
    }
}
