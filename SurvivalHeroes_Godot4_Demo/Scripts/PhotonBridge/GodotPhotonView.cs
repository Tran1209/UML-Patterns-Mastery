// ----------------------------------------------------------------------------
// GodotPhotonView.cs
// [TẦNG 3 - CẦU NỐI PHOTON]:
//   - Đã tối ưu hóa StaticTypeRpcCache (Dictionary tĩnh cấp độ Type):
//     Quét Reflection tìm [PunRPC] đúng 1 lần duy nhất cho mỗi class khi nạp game,
//     triệt tiêu 100% GC Spike khi khởi tạo Node trong trận đấu!
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
        /// <summary>
        /// Bộ nhớ đệm tĩnh lưu danh sách phương thức [PunRPC] theo từng Type.
        /// Mỗi lớp C# chỉ quét Reflection đúng 1 lần duy nhất trong suốt vòng đời ứng dụng!
        /// </summary>
        private static readonly Dictionary<Type, (MethodInfo Method, ParameterInfo[] Params)[]> StaticTypeRpcCache = new();

        public static void PrewarmRpcCacheForTypes(params Type[] types)
        {
            if (types == null) return;
            foreach (Type t in types)
            {
                if (t != null) GetOrBuildTypeRpcMethods(t);
            }
        }

        private static (MethodInfo Method, ParameterInfo[] Params)[] GetOrBuildTypeRpcMethods(Type type)
        {
            if (StaticTypeRpcCache.TryGetValue(type, out var cached))
            {
                return cached;
            }

            List<(MethodInfo, ParameterInfo[])> found = new();
            MethodInfo[] methods = type.GetMethods(BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance);
            foreach (MethodInfo method in methods)
            {
                if (method.IsDefined(typeof(PunRPC), true))
                {
                    found.Add((method, method.GetParameters()));
                }
            }

            var arr = found.ToArray();
            StaticTypeRpcCache[type] = arr;
            return arr;
        }

        [Export] public int ViewID { get; set; }
        [Export] public byte Group { get; set; } = 0;
        [Export] public ViewSynchronization Synchronization { get; set; } = ViewSynchronization.ReliableDeltaCompressed;
        [Export] public OwnershipOption OwnershipTransfer { get; set; } = OwnershipOption.Fixed;

        public int OwnerActorNr { get; internal set; }
        public int CreatorActorNr { get; internal set; }

        public new Player Owner => GodotPhotonService.Instance?.CurrentRoom?.GetPlayer(OwnerActorNr);
        public bool IsRoomView => CreatorActorNr == 0;

        public bool IsMine
        {
            get
            {
                if (GodotPhotonService.Instance == null) return OwnerActorNr == 1 || IsRoomView;
                int localActor = (!GodotPhotonService.Instance.IsLocalSimulationMode
                    && GodotPhotonService.Instance.LocalPlayer != null
                    && GodotPhotonService.Instance.LocalPlayer.ActorNumber > 0)
                    ? GodotPhotonService.Instance.LocalPlayer.ActorNumber
                    : 1;
                if (OwnerActorNr == localActor) return true;
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

            var rpcMethods = GetOrBuildTypeRpcMethods(node.GetType());
            for (int i = 0; i < rpcMethods.Length; i++)
            {
                var (method, paramInfos) = rpcMethods[i];
                if (!_rpcCache.TryGetValue(method.Name, out var list))
                {
                    list = new List<(Node, MethodInfo, ParameterInfo[])>(2);
                    _rpcCache[method.Name] = list;
                }
                list.Add((node, method, paramInfos));
            }

            foreach (Node child in node.GetChildren())
            {
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

        public void RPC(string methodName, RpcTarget target, params object[] parameters)
        {
            GodotPhotonService.Instance?.SendRpc(this, methodName, target, null, parameters);
        }

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
