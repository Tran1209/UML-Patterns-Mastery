// ----------------------------------------------------------------------------
// GodotCustomTypes.cs
// Chuyển đổi từ: PhotonUnityNetworking/Code/CustomTypes.cs
// Chức năng: Đăng ký các kiểu dữ liệu toán học của Godot 4 (Vector2, Vector3,
// Quaternion, Color) vào bộ tuần tự hóa nhị phân (StreamBuffer) của Photon.
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using Godot;
    using Photon.Realtime;
    using ExitGames.Client.Photon;

    public static class GodotCustomTypes
    {
        public static void Register()
        {
            PhotonPeer.RegisterType(typeof(Vector2), (byte)'W', SerializeVector2, DeserializeVector2);
            PhotonPeer.RegisterType(typeof(Vector3), (byte)'V', SerializeVector3, DeserializeVector3);
            PhotonPeer.RegisterType(typeof(Quaternion), (byte)'Q', SerializeQuaternion, DeserializeQuaternion);
            PhotonPeer.RegisterType(typeof(Color), (byte)'C', SerializeColor, DeserializeColor);
            PhotonPeer.RegisterType(typeof(Player), (byte)'P', SerializePhotonPlayer, DeserializePhotonPlayer);
        }

        private static readonly byte[] memVector3 = new byte[3 * 4];

        private static short SerializeVector3(StreamBuffer outStream, object customobject)
        {
            Vector3 vo = (Vector3)customobject;
            int index = 0;
            lock (memVector3)
            {
                byte[] bytes = memVector3;
                Protocol.Serialize(vo.X, bytes, ref index);
                Protocol.Serialize(vo.Y, bytes, ref index);
                Protocol.Serialize(vo.Z, bytes, ref index);
                outStream.Write(bytes, 0, 3 * 4);
            }
            return 3 * 4;
        }

        private static object DeserializeVector3(StreamBuffer inStream, short length)
        {
            Vector3 vo = new Vector3();
            lock (memVector3)
            {
                inStream.Read(memVector3, 0, 3 * 4);
                int index = 0;
                Protocol.Deserialize(out vo.X, memVector3, ref index);
                Protocol.Deserialize(out vo.Y, memVector3, ref index);
                Protocol.Deserialize(out vo.Z, memVector3, ref index);
            }
            return vo;
        }

        private static readonly byte[] memVector2 = new byte[2 * 4];

        private static short SerializeVector2(StreamBuffer outStream, object customobject)
        {
            Vector2 vo = (Vector2)customobject;
            lock (memVector2)
            {
                byte[] bytes = memVector2;
                int index = 0;
                Protocol.Serialize(vo.X, bytes, ref index);
                Protocol.Serialize(vo.Y, bytes, ref index);
                outStream.Write(bytes, 0, 2 * 4);
            }
            return 2 * 4;
        }

        private static object DeserializeVector2(StreamBuffer inStream, short length)
        {
            Vector2 vo = new Vector2();
            lock (memVector2)
            {
                inStream.Read(memVector2, 0, 2 * 4);
                int index = 0;
                Protocol.Deserialize(out vo.X, memVector2, ref index);
                Protocol.Deserialize(out vo.Y, memVector2, ref index);
            }
            return vo;
        }

        private static readonly byte[] memQuaternion = new byte[4 * 4];

        private static short SerializeQuaternion(StreamBuffer outStream, object customobject)
        {
            Quaternion o = (Quaternion)customobject;
            lock (memQuaternion)
            {
                byte[] bytes = memQuaternion;
                int index = 0;
                Protocol.Serialize(o.W, bytes, ref index);
                Protocol.Serialize(o.X, bytes, ref index);
                Protocol.Serialize(o.Y, bytes, ref index);
                Protocol.Serialize(o.Z, bytes, ref index);
                outStream.Write(bytes, 0, 4 * 4);
            }
            return 4 * 4;
        }

        private static object DeserializeQuaternion(StreamBuffer inStream, short length)
        {
            Quaternion o = new Quaternion();
            lock (memQuaternion)
            {
                inStream.Read(memQuaternion, 0, 4 * 4);
                int index = 0;
                Protocol.Deserialize(out o.W, memQuaternion, ref index);
                Protocol.Deserialize(out o.X, memQuaternion, ref index);
                Protocol.Deserialize(out o.Y, memQuaternion, ref index);
                Protocol.Deserialize(out o.Z, memQuaternion, ref index);
            }
            return o;
        }

        private static readonly byte[] memColor = new byte[4 * 4];

        private static short SerializeColor(StreamBuffer outStream, object customobject)
        {
            Color c = (Color)customobject;
            lock (memColor)
            {
                byte[] bytes = memColor;
                int index = 0;
                Protocol.Serialize(c.R, bytes, ref index);
                Protocol.Serialize(c.G, bytes, ref index);
                Protocol.Serialize(c.B, bytes, ref index);
                Protocol.Serialize(c.A, bytes, ref index);
                outStream.Write(bytes, 0, 4 * 4);
            }
            return 4 * 4;
        }

        private static object DeserializeColor(StreamBuffer inStream, short length)
        {
            Color c = new Color();
            lock (memColor)
            {
                inStream.Read(memColor, 0, 4 * 4);
                int index = 0;
                Protocol.Deserialize(out c.R, memColor, ref index);
                Protocol.Deserialize(out c.G, memColor, ref index);
                Protocol.Deserialize(out c.B, memColor, ref index);
                Protocol.Deserialize(out c.A, memColor, ref index);
            }
            return c;
        }

        private static readonly byte[] memPlayer = new byte[4];

        private static short SerializePhotonPlayer(StreamBuffer outStream, object customobject)
        {
            int id = ((Player)customobject).ActorNumber;
            lock (memPlayer)
            {
                byte[] bytes = memPlayer;
                int off = 0;
                Protocol.Serialize(id, bytes, ref off);
                outStream.Write(bytes, 0, 4);
                return 4;
            }
        }

        private static object DeserializePhotonPlayer(StreamBuffer inStream, short length)
        {
            int id;
            lock (memPlayer)
            {
                inStream.Read(memPlayer, 0, length);
                int off = 0;
                Protocol.Deserialize(out id, memPlayer, ref off);
            }

            if (GodotPhotonService.Instance?.Client?.CurrentRoom != null)
            {
                return GodotPhotonService.Instance.Client.CurrentRoom.GetPlayer(id);
            }
            return null;
        }
    }
}
