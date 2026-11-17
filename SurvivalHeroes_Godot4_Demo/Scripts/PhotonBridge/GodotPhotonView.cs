// ----------------------------------------------------------------------------
// GodotPhotonView.cs
// Chuyển đổi từ: PhotonUnityNetworking/Code/PhotonView.cs
// Chức năng: Node định danh đối tượng mạng trên Godot 4, quản lý ViewID,
// quyền sở hữu (IsMine / Owner), danh sách IPunObservable và gọi hàm từ xa (RPC).
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using System;
    using System.Collections.Generic;
    using System.Reflection;
    using Godot;
    using Photon.Realtime;

    public partial class GodotPhotonView : Node
    {
        [Export]
        public int ViewID { get; set; }

        [Export]
        public byte Group { get; set; } = 0;

        [Export]
        public ViewSynchronization Synchronization { get; set; } = ViewSynchronization.ReliableDeltaCompressed;

        [Export]
        public OwnershipOption OwnershipTransfer { get; set; } = OwnershipOption.Fixed;

        public int OwnerActorNr { get; internal set; }
        public int CreatorActorNr { get; internal set; }

        /// <summary>Người chơi sở hữu PhotonView này trên mạng (dùng new để phân biệt với Node.Owner của SceneTree).</summary>
        public new Player Owner => GodotPhotonService.Instance?.CurrentRoom?.GetPlayer(OwnerActorNr);
        public bool IsRoomView => CreatorActorNr == 0;

        public bool IsMine
        {
            get
            {
                if (GodotPhotonService.Instance?.LocalPlayer == null) return false;
                if (OwnerActorNr == GodotPhotonService.Instance.LocalPlayer.ActorNumber) return true;
                return IsRoomView && GodotPhotonService.Instance.IsMasterClient;
            }
        }

        public readonly List<IPunObservable> ObservedComponents = new();

        private readonly Dictionary<string, List<(Node Target, MethodInfo Method, ParameterInfo[] Params)>> _rpcCache = new();

        public override void _EnterTree()
        {
            RefreshObservedAndRpcTargets();
            if (ViewID != 0)
            {
                GodotPhotonService.Instance?.RegisterPhotonView(this);
            }
        }

        public override void _ExitTree()
        {
            if (ViewID != 0)
            {
                GodotPhotonService.Instance?.UnregisterPhotonView(this);
            }
        }

        /// <summary>
        /// Quét Node cha (Root của thực thể) và các Node con để tự động tìm IPunObservable và [PunRPC].
        /// </summary>
        public void RefreshObservedAndRpcTargets()
        {
            ObservedComponents.Clear();
            _rpcCache.Clear();

            Node entityRoot = GetParent() ?? this;
            ScanNodeHierarchy(entityRoot);
        }

        private void ScanNodeHierarchy(Node node)
        {
            if (node is IPunObservable observable && !ObservedComponents.Contains(observable))
            {
                ObservedComponents.Add(observable);
            }

            MethodInfo[] methods = node.GetType().GetMethods(BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance);
            foreach (MethodInfo method in methods)
            {
                if (method.IsDefined(typeof(PunRPC), true))
                {
                    if (!_rpcCache.TryGetValue(method.Name, out var list))
                    {
                        list = new List<(Node, MethodInfo, ParameterInfo[])>();
                        _rpcCache[method.Name] = list;
                    }
                    list.Add((node, method, method.GetParameters()));
                }
            }

            foreach (Node child in node.GetChildren())
            {
                // Không quét lấn sang một GodotPhotonView lồng bên dưới khác
                if (child is GodotPhotonView && child != this) continue;
                ScanNodeHierarchy(child);
            }
        }

        public void SerializeView(PhotonStream stream, PhotonMessageInfo info)
        {
            for (int i = 0; i < ObservedComponents.Count; i++)
            {
                ObservedComponents[i]?.OnPhotonSerializeView(stream, info);
            }
        }

        public void DeserializeView(PhotonStream stream, PhotonMessageInfo info)
        {
            for (int i = 0; i < ObservedComponents.Count; i++)
            {
                ObservedComponents[i]?.OnPhotonSerializeView(stream, info);
            }
        }

        /// <summary>Gửi lệnh gọi hàm từ xa (RPC) tới các máy khách trong phòng.</summary>
        public void RPC(string methodName, RpcTarget target, params object[] parameters)
        {
            GodotPhotonService.Instance?.SendRpc(this, methodName, target, null, parameters);
        }

        /// <summary>Gửi lệnh gọi hàm từ xa (RPC) tới một người chơi cụ thể.</summary>
        public void RpcSecure(string methodName, Player targetPlayer, params object[] parameters)
        {
            GodotPhotonService.Instance?.SendRpc(this, methodName, RpcTarget.Others, targetPlayer, parameters);
        }

        internal void ExecuteLocalRpc(string methodName, object[] args, PhotonMessageInfo info)
        {
            if (!_rpcCache.TryGetValue(methodName, out var handlers) || handlers.Count == 0)
            {
                GD.PrintErr($"[GodotPhotonView {ViewID}] Không tìm thấy phương thức [PunRPC] '{methodName}'.");
                return;
            }

            int argCount = args?.Length ?? 0;
            foreach (var (target, method, paramInfos) in handlers)
            {
                if (!GodotObject.IsInstanceValid(target)) continue;

                if (paramInfos.Length == argCount)
                {
                    method.Invoke(target, args);
                }
                else if (paramInfos.Length == argCount + 1 && paramInfos[^1].ParameterType == typeof(PhotonMessageInfo))
                {
                    object[] extendedArgs = new object[argCount + 1];
                    if (argCount > 0) Array.Copy(args, extendedArgs, argCount);
                    extendedArgs[argCount] = info;
                    method.Invoke(target, extendedArgs);
                }
            }
        }

        /// <summary>Hàm tiện ích lấy GodotPhotonView gắn trên một Node hoặc con trực tiếp của Node đó.</summary>
        public static GodotPhotonView Get(Node node)
        {
            if (node == null) return null;
            if (node is GodotPhotonView pv) return pv;
            foreach (Node child in node.GetChildren())
            {
                if (child is GodotPhotonView childPv) return childPv;
            }
            return node.GetParent() != null ? Get(node.GetParent()) : null;
        }
    }
}
