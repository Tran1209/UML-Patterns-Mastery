// ----------------------------------------------------------------------------
// GodotPhotonService.cs
// Chuyển đổi từ: PhotonHandler.cs + ConnectionHandler.cs + PhotonNetwork.cs + PhotonNetworkPart.cs
// Hỗ trợ song song:
//   1. Kết nối Photon Cloud thật (qua LoadBalancingClient + Photon3Unity3D.dll)
//   2. Chế độ Local Photon Loopback Room (cho phép chạy thử ngay lập tức toàn bộ
//      pipeline GodotPhotonView, [PunRPC], PhotonStream, CullArea, SafeZoneTimer
//      ngay cả khi chưa điền AppId của Photon Cloud!).
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using System;
    using System.Collections.Generic;
    using ExitGames.Client.Photon;
    using Godot;
    using Photon.Realtime;

    public partial class GodotPhotonService : Node, IConnectionCallbacks, IMatchmakingCallbacks, IInRoomCallbacks, IOnEventCallback
    {
        public static GodotPhotonService Instance { get; private set; }

        public const int MaxViewIds = 1000;

        public LoadBalancingClient Client { get; private set; }
        public IGodotPunScenePool ScenePool { get; set; } = new GodotScenePool();

        [Export] public int SendRate { get; set; } = 20;
        [Export] public int SerializationRate { get; set; } = 12;

        /// <summary>True khi đang chạy phòng giả lập Loopback tại máy để test ngay lập tức.</summary>
        public bool IsLocalSimulationMode { get; private set; } = true;
        public string StatusMessage { get; private set; } = "Photon Loopback Room (Local Sim + AI)";

        public Room CurrentRoom => Client?.CurrentRoom;
        public Player LocalPlayer => Client?.LocalPlayer;
        public new bool IsConnected => IsLocalSimulationMode || (Client != null && Client.IsConnected);
        public bool InRoom => IsLocalSimulationMode || (Client != null && Client.InRoom);
        public bool IsMasterClient => IsLocalSimulationMode || (CurrentRoom != null && LocalPlayer != null && CurrentRoom.MasterClientId == LocalPlayer.ActorNumber);

        public int ServerTimestamp => (Client != null && Client.InRoom)
            ? Client.LoadBalancingPeer.ServerTimeInMilliSeconds
            : (int)(Godot.Time.GetTicksMsec() & 0x7FFFFFFF);

        public double Time => ((uint)ServerTimestamp) / 1000.0d;

        /// <summary>Các thuộc tính của Phòng (dùng chung choทั้ง Photon Cloud Room và Local Loopback Room).</summary>
        public readonly Hashtable SharedRoomProperties = new();

        public event Action<Hashtable> OnRoomPropertiesChanged;
        public event Action<byte, object, int> OnCustomNetworkEvent;

        private readonly Dictionary<int, GodotPhotonView> _photonViews = new();
        private readonly PhotonStream _serializeStreamOut = new(true, null);
        private readonly PhotonStream _serializeStreamIn = new(false, null);
        private readonly HashSet<byte> _subscribedInterestGroups = new() { 0 };

        private double _sendAccumulator;
        private double _serializeAccumulator;
        private int _lastAllocatedViewSubId;

        private const byte KeyViewId = 0;
        private const byte KeyRpcMethodName = 3;
        private const byte KeyRpcParams = 4;
        private const byte KeyInstantiationPrefab = 0;
        private const byte KeyInstantiationPos = 1;
        private const byte KeyInstantiationRot = 2;
        private const byte KeyInstantiationViewIds = 7;

        public override void _EnterTree()
        {
            if (Instance != null && Instance != this)
            {
                QueueFree();
                return;
            }
            Instance = this;
            ProcessMode = ProcessModeEnum.Always;

            GodotCustomTypes.Register();

            Client = new LoadBalancingClient();
            Client.AddCallbackTarget(this);
            Client.LocalPlayer.NickName = "Hero_" + GD.Randi() % 900;
        }

        public override void _ExitTree()
        {
            if (Client != null)
            {
                Client.RemoveCallbackTarget(this);
                if (Client.IsConnected)
                {
                    Client.Disconnect();
                }
            }
            if (Instance == this)
            {
                Instance = null;
            }
        }

        public override void _Notification(int what)
        {
            if (what == NotificationWMCloseRequest)
            {
                if (Client != null && Client.IsConnected)
                {
                    Client.Disconnect();
                }
            }
        }

        public override void _Process(double delta)
        {
            if (Client?.LoadBalancingPeer != null && !IsLocalSimulationMode)
            {
                while (Client.LoadBalancingPeer.DispatchIncomingCommands())
                {
                }
            }

            double serializeInterval = 1.0 / Math.Max(1, SerializationRate);
            _serializeAccumulator += delta;
            if (_serializeAccumulator >= serializeInterval)
            {
                _serializeAccumulator = 0.0;
                if (InRoom)
                {
                    RunViewUpdate();
                }
            }

            double sendInterval = 1.0 / Math.Max(1, SendRate);
            _sendAccumulator += delta;
            if (_sendAccumulator >= sendInterval)
            {
                _sendAccumulator = 0.0;
                if (Client?.LoadBalancingPeer != null && !IsLocalSimulationMode)
                {
                    Client.LoadBalancingPeer.SendOutgoingCommands();
                }
            }
        }

        /// <summary>Kết nối lên Photon Cloud thật bằng AppId của bạn.</summary>
        public bool ConnectToPhotonCloud(string appId, string region = "asia")
        {
            if (string.IsNullOrWhiteSpace(appId))
            {
                StatusMessage = "AppId trống -> Đang chạy chế độ Photon Loopback Sim";
                IsLocalSimulationMode = true;
                return false;
            }

            IsLocalSimulationMode = false;
            StatusMessage = $"Đang kết nối Photon Cloud ({region})...";
            AppSettings settings = new()
            {
                AppIdRealtime = appId.Trim(),
                FixedRegion = region,
                AppVersion = "1.0-survival-heroes"
            };
            return Client.ConnectUsingSettings(settings);
        }

        public void SwitchToLocalSimulationMode()
        {
            if (Client != null && Client.IsConnected)
            {
                Client.Disconnect();
            }
            IsLocalSimulationMode = true;
            StatusMessage = "Photon Loopback Room (Local Sim + AI)";
        }

        /// <summary>Cập nhật Room CustomProperties (ví dụ: trạng thái Vòng Bo, Thời gian trận đấu).</summary>
        public void SetRoomCustomProperties(Hashtable propsToSet)
        {
            foreach (var key in propsToSet.Keys)
            {
                SharedRoomProperties[key] = propsToSet[key];
            }

            if (!IsLocalSimulationMode && Client != null && Client.InRoom)
            {
                Client.CurrentRoom.SetCustomProperties(propsToSet);
            }
            else
            {
                OnRoomPropertiesChanged?.Invoke(propsToSet);
            }
        }

        /// <summary>Cập nhật các nhóm vùng quan tâm (Interest Groups) từ hệ thống CullArea.</summary>
        public void SetInterestGroups(byte[] disableGroups, byte[] enableGroups)
        {
            if (disableGroups != null)
            {
                foreach (byte g in disableGroups) _subscribedInterestGroups.Remove(g);
            }
            if (enableGroups != null)
            {
                foreach (byte g in enableGroups) _subscribedInterestGroups.Add(g);
            }
            _subscribedInterestGroups.Add(0); // Nhóm 0 luôn mặc định nhận

            if (!IsLocalSimulationMode && Client != null && Client.InRoom)
            {
                Client.OpChangeGroups(disableGroups, enableGroups);
            }
        }

        public bool IsGroupSubscribed(byte group)
        {
            return group == 0 || _subscribedInterestGroups.Contains(group);
        }

        public int AllocateViewID(bool roomObject = false)
        {
            int actorNr = (LocalPlayer != null && LocalPlayer.ActorNumber > 0) ? LocalPlayer.ActorNumber : 1;
            int ownerId = roomObject ? 0 : actorNr;
            for (int i = 1; i < MaxViewIds; i++)
            {
                _lastAllocatedViewSubId = (_lastAllocatedViewSubId % (MaxViewIds - 1)) + 1;
                int candidateViewId = ownerId * MaxViewIds + _lastAllocatedViewSubId;
                if (!_photonViews.ContainsKey(candidateViewId))
                {
                    return candidateViewId;
                }
            }
            throw new InvalidOperationException($"Đã hết ViewID trống cho Actor {ownerId}!");
        }

        public void RegisterPhotonView(GodotPhotonView view)
        {
            if (view.ViewID == 0) return;
            _photonViews[view.ViewID] = view;
        }

        public void UnregisterPhotonView(GodotPhotonView view)
        {
            if (view.ViewID != 0 && _photonViews.TryGetValue(view.ViewID, out var existing) && existing == view)
            {
                _photonViews.Remove(view.ViewID);
            }
        }

        public GodotPhotonView GetPhotonView(int viewId)
        {
            _photonViews.TryGetValue(viewId, out var view);
            return view;
        }

        public IReadOnlyDictionary<int, GodotPhotonView> GetAllViews() => _photonViews;

        /// <summary>Gửi sự kiện mạng tùy chỉnh (ví dụ: nhặt vũ khí trên đất, thông báo hạ gục).</summary>
        public void RaiseCustomEvent(byte eventCode, object customData, ReceiverGroup receivers = ReceiverGroup.All, bool reliable = true)
        {
            if (!IsLocalSimulationMode && Client != null && Client.InRoom)
            {
                RaiseEventOptions opts = new() { Receivers = receivers };
                SendOptions sendOpts = reliable ? SendOptions.SendReliable : SendOptions.SendUnreliable;
                Client.OpRaiseEvent(eventCode, customData, opts, sendOpts);
                if (receivers == ReceiverGroup.All || (receivers == ReceiverGroup.MasterClient && IsMasterClient))
                {
                    OnCustomNetworkEvent?.Invoke(eventCode, customData, LocalPlayer?.ActorNumber ?? 1);
                }
            }
            else
            {
                OnCustomNetworkEvent?.Invoke(eventCode, customData, LocalPlayer?.ActorNumber ?? 1);
            }
        }

        public Node InstantiateNetworked(string scenePath, Vector3 position, Quaternion rotation, byte group = 0)
        {
            int actorNr = (LocalPlayer != null && LocalPlayer.ActorNumber > 0) ? LocalPlayer.ActorNumber : 1;
            int viewId = AllocateViewID(false);

            if (!IsLocalSimulationMode && Client != null && Client.InRoom)
            {
                Hashtable instData = new()
                {
                    [KeyInstantiationPrefab] = scenePath,
                    [KeyInstantiationPos] = position,
                    [KeyInstantiationRot] = rotation,
                    [KeyInstantiationViewIds] = new int[] { viewId }
                };

                RaiseEventOptions options = new()
                {
                    CachingOption = EventCaching.AddToRoomCache,
                    Receivers = ReceiverGroup.Others,
                    InterestGroup = group
                };

                Client.OpRaiseEvent(PunEvent.Instantiation, instData, options, SendOptions.SendReliable);
            }

            return ExecuteLocalInstantiation(scenePath, position, rotation, new int[] { viewId }, actorNr);
        }

        private Node ExecuteLocalInstantiation(string scenePath, Vector3 position, Quaternion rotation, int[] viewIds, int creatorActorNr)
        {
            Node instance = ScenePool.Instantiate(scenePath, position, rotation);
            if (instance == null) return null;

            GodotPhotonView pv = GodotPhotonView.Get(instance);
            if (pv != null && viewIds != null && viewIds.Length > 0)
            {
                pv.ViewID = viewIds[0];
                pv.OwnerActorNr = creatorActorNr;
                pv.CreatorActorNr = creatorActorNr;
                RegisterPhotonView(pv);
            }

            GetTree().CurrentScene?.AddChild(instance);
            return instance;
        }

        internal void SendRpc(GodotPhotonView view, string methodName, RpcTarget target, Player targetPlayer, object[] parameters)
        {
            if (!InRoom) return;

            PhotonMessageInfo localInfo = new(LocalPlayer, ServerTimestamp, view);

            if (IsLocalSimulationMode)
            {
                // Trong chế độ Loopback, thực thi trực tiếp qua bộ định tuyến RPC của GodotPhotonView
                if (target != RpcTarget.Others && target != RpcTarget.OthersBuffered)
                {
                    view.ExecuteLocalRpc(methodName, parameters, localInfo);
                }
                else
                {
                    // Ngay cả khi gửi Others trong Loopback, các thực thể trong cùng Scene vẫn nhận được
                    view.ExecuteLocalRpc(methodName, parameters, localInfo);
                }
                return;
            }

            Hashtable rpcEvent = new()
            {
                [KeyViewId] = view.ViewID,
                [KeyRpcMethodName] = methodName
            };
            if (parameters != null && parameters.Length > 0)
            {
                rpcEvent[KeyRpcParams] = parameters;
            }

            if (targetPlayer != null)
            {
                RaiseEventOptions opts = new() { TargetActors = new int[] { targetPlayer.ActorNumber } };
                Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, opts, SendOptions.SendReliable);
                if (targetPlayer.ActorNumber == LocalPlayer.ActorNumber)
                {
                    view.ExecuteLocalRpc(methodName, parameters, localInfo);
                }
                return;
            }

            RaiseEventOptions options = new() { InterestGroup = view.Group };
            switch (target)
            {
                case RpcTarget.All:
                    options.Receivers = ReceiverGroup.Others;
                    Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    view.ExecuteLocalRpc(methodName, parameters, localInfo);
                    break;
                case RpcTarget.Others:
                    options.Receivers = ReceiverGroup.Others;
                    Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    break;
                case RpcTarget.MasterClient:
                    if (IsMasterClient)
                    {
                        view.ExecuteLocalRpc(methodName, parameters, localInfo);
                    }
                    else
                    {
                        options.Receivers = ReceiverGroup.MasterClient;
                        Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    }
                    break;
                case RpcTarget.AllBuffered:
                    options.Receivers = ReceiverGroup.Others;
                    options.CachingOption = EventCaching.AddToRoomCache;
                    Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    view.ExecuteLocalRpc(methodName, parameters, localInfo);
                    break;
                case RpcTarget.OthersBuffered:
                    options.Receivers = ReceiverGroup.Others;
                    options.CachingOption = EventCaching.AddToRoomCache;
                    Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    break;
                case RpcTarget.AllViaServer:
                    options.Receivers = ReceiverGroup.All;
                    Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    break;
                case RpcTarget.AllBufferedViaServer:
                    options.Receivers = ReceiverGroup.All;
                    options.CachingOption = EventCaching.AddToRoomCache;
                    Client.OpRaiseEvent(PunEvent.RPC, rpcEvent, options, SendOptions.SendReliable);
                    break;
            }
        }

        private void RunViewUpdate()
        {
            foreach (var kvp in _photonViews)
            {
                GodotPhotonView view = kvp.Value;
                if (view == null || !GodotObject.IsInstanceValid(view)) continue;
                if (view.Synchronization == ViewSynchronization.Off || !view.IsMine || view.ObservedComponents.Count == 0)
                {
                    continue;
                }

                List<object> dataList = new();
                _serializeStreamOut.SetWriteStream(dataList);
                PhotonMessageInfo info = new(LocalPlayer, ServerTimestamp, view);
                view.SerializeView(_serializeStreamOut, info);

                if (_serializeStreamOut.Count == 0) continue;
                if (IsLocalSimulationMode) continue; // Trong Local Sim không cần gửi ra socket ngoài

                object[] payload = _serializeStreamOut.ToArray();
                Hashtable msg = new()
                {
                    [(byte)0] = ServerTimestamp,
                    [(byte)1] = view.ViewID,
                    [(byte)2] = payload
                };

                bool reliable = view.Synchronization == ViewSynchronization.ReliableDeltaCompressed;
                SendOptions sendOptions = reliable ? SendOptions.SendReliable : SendOptions.SendUnreliable;
                byte eventCode = reliable ? PunEvent.SendSerializeReliable : PunEvent.SendSerialize;

                RaiseEventOptions raiseOptions = new()
                {
                    InterestGroup = view.Group,
                    Receivers = ReceiverGroup.Others
                };

                Client.OpRaiseEvent(eventCode, msg, raiseOptions, sendOptions);
            }
        }

        public void OnEvent(EventData photonEvent)
        {
            int senderActor = photonEvent.Sender;
            Player sender = CurrentRoom?.GetPlayer(senderActor);

            switch (photonEvent.Code)
            {
                case PunEvent.RPC:
                    if (photonEvent.CustomData is Hashtable rpcData)
                    {
                        int viewId = (int)rpcData[KeyViewId];
                        string methodName = (string)rpcData[KeyRpcMethodName];
                        object[] args = rpcData.ContainsKey(KeyRpcParams) ? (object[])rpcData[KeyRpcParams] : Array.Empty<object>();

                        GodotPhotonView view = GetPhotonView(viewId);
                        if (view != null)
                        {
                            PhotonMessageInfo info = new(sender, ServerTimestamp, view);
                            view.ExecuteLocalRpc(methodName, args, info);
                        }
                    }
                    break;

                case PunEvent.SendSerialize:
                case PunEvent.SendSerializeReliable:
                    if (photonEvent.CustomData is Hashtable syncData)
                    {
                        int timestamp = (int)syncData[(byte)0];
                        int viewId = (int)syncData[(byte)1];
                        object[] values = (object[])syncData[(byte)2];

                        GodotPhotonView view = GetPhotonView(viewId);
                        if (view != null && !view.IsMine)
                        {
                            _serializeStreamIn.SetReadStream(values);
                            PhotonMessageInfo info = new(sender, timestamp, view);
                            view.DeserializeView(_serializeStreamIn, info);
                        }
                    }
                    break;

                case PunEvent.Instantiation:
                    if (photonEvent.CustomData is Hashtable instData)
                    {
                        string scenePath = (string)instData[KeyInstantiationPrefab];
                        Vector3 pos = instData.ContainsKey(KeyInstantiationPos) ? (Vector3)instData[KeyInstantiationPos] : Vector3.Zero;
                        Quaternion rot = instData.ContainsKey(KeyInstantiationRot) ? (Quaternion)instData[KeyInstantiationRot] : Quaternion.Identity;
                        int[] viewIds = (int[])instData[KeyInstantiationViewIds];
                        ExecuteLocalInstantiation(scenePath, pos, rot, viewIds, senderActor);
                    }
                    break;

                default:
                    if (photonEvent.Code < 200)
                    {
                        OnCustomNetworkEvent?.Invoke(photonEvent.Code, photonEvent.CustomData, senderActor);
                    }
                    break;
            }
        }

        // --- IConnectionCallbacks & IMatchmakingCallbacks ---
        public void OnConnected() => StatusMessage = "Đã kết nối NameServer...";
        public void OnConnectedToMaster()
        {
            StatusMessage = "Đã kết nối Photon Master! Đang vào phòng SurvivalHeroes...";
            RoomOptions roomOpts = new() { MaxPlayers = 20, IsOpen = true, IsVisible = true };
            Client.OpJoinOrCreateRoom(new EnterRoomParams { RoomName = "SurvivalHeroes_Arena_1", RoomOptions = roomOpts });
        }
        public void OnDisconnected(DisconnectCause cause)
        {
            StatusMessage = $"Ngắt kết nối Photon Cloud ({cause}) -> Chuyển về Local Sim";
            IsLocalSimulationMode = true;
        }
        public void OnRegionListReceived(RegionHandler regionHandler) { }
        public void OnCustomAuthenticationResponse(Dictionary<string, object> data) { }
        public void OnCustomAuthenticationFailed(string debugMessage) { }
        public void OnFriendListUpdate(List<FriendInfo> friendList) { }
        public void OnCreatedRoom() => StatusMessage = "Đã tạo phòng Photon Cloud (MasterClient)!";
        public void OnJoinedRoom()
        {
            StatusMessage = $"Online Photon Room: {CurrentRoom?.Name} ({CurrentRoom?.PlayerCount} Players)";
            if (CurrentRoom?.CustomProperties != null)
            {
                foreach (var k in CurrentRoom.CustomProperties.Keys)
                {
                    SharedRoomProperties[k] = CurrentRoom.CustomProperties[k];
                }
                OnRoomPropertiesChanged?.Invoke(CurrentRoom.CustomProperties);
            }
        }
        public void OnCreateRoomFailed(short returnCode, string message) => StatusMessage = $"Lỗi tạo phòng: {message}";
        public void OnJoinRoomFailed(short returnCode, string message) => StatusMessage = $"Lỗi vào phòng: {message}";
        public void OnJoinRandomFailed(short returnCode, string message) { }
        public void OnLeftRoom() => StatusMessage = "Đã rời phòng.";
        public void OnPlayerEnteredRoom(Player newPlayer) => StatusMessage = $"{newPlayer.NickName} vừa vào đấu trường!";
        public void OnPlayerLeftRoom(Player otherPlayer) => StatusMessage = $"{otherPlayer.NickName} đã rời đấu trường.";
        public void OnRoomPropertiesUpdate(Hashtable propertiesThatChanged)
        {
            foreach (var k in propertiesThatChanged.Keys)
            {
                SharedRoomProperties[k] = propertiesThatChanged[k];
            }
            OnRoomPropertiesChanged?.Invoke(propertiesThatChanged);
        }
        public void OnPlayerPropertiesUpdate(Player targetPlayer, Hashtable changedProps) { }
        public void OnMasterClientSwitched(Player newMasterClient) { }
    }
}
