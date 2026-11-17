# Phân tích Toàn diện Photon Unity & Sổ tay Chọn lọc Chuyển đổi sang Godot 4 C# (.NET)

Tài liệu này phân tích trực tiếp từ mã nguồn thực tế của bộ **Photon Unity (`PhotonLibs` + `PhotonRealtime` + `PhotonUnityNetworking (PUN 2)` + `PhotonChat` + `PhotonVoice`)** tại thư mục [repo](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo), nhằm giúp bạn:
1. Hiểu rõ **kiến trúc phân tầng** của Photon bên dưới Unity.
2. **Tự chọn lọc** chính xác những file/module nào cần **Giữ nguyên (`🟢 KEEP`)**, **Chuyển đổi sang Godot 4 C# (`🟡 ADAPT`)**, hoặc **Loại bỏ (`🔴 PRUNE`)**.
3. Nắm được **cách gỡ bỏ 100% phụ thuộc `UnityEngine`** nhưng vẫn giữ nguyên toàn bộ sức mạnh của Photon Cloud (Room, Lobby, Matchmaking, CustomProperties, RaiseEvent, PhotonView, `[PunRPC]`, `PhotonStream`, Lag Compensation) trên **Godot 4 C# (.NET 8)**.

---

## Phần 1: Sơ đồ Cây Thư mục & Nhãn Chọn lọc Tổng thể

Dưới đây là sơ đồ cây toàn bộ kho mã nguồn `PhotonPackage` (619 files). Các thư mục chỉ chứa tài nguyên Editor/UI/Native Audio đã được gom gọn, trong khi **100% các file logic C# (`.cs`) và thư viện lõi (`.dll`)** được liệt kê đầy đủ kèm nhãn khuyến nghị:

```text
PhotonPackage/
├── PhotonLibs/                                       [TẦNG 1: TRANSPORT & SERIALIZATION LÕI]
│   ├── netstandard2.0/
│   │   └── Photon3Unity3D.dll                        🟢 [KEEP] Thư viện lõi .NET Standard 2.0 (KHÔNG dính UnityEngine!)
│   ├── Photon3Unity3D.dll                            🔴 [PRUNE] Bản build cũ cho .NET 3.5 của Unity cũ
│   ├── Metro/Photon3Unity3D.dll                      🔴 [PRUNE] Bản đặc thù cho UWP/HoloLens cũ
│   └── WebSocket/
│       ├── SocketWebTcp.cs                           🟡 [ADAPT] Chỉ cần nếu xuất bản Web (HTML5/WebAssembly)
│       ├── WebSocket.cs                              🟡 [ADAPT] Wrapper WebSocket cho Web
│       └── websocket-sharp.dll                       🟢 [KEEP] Thư viện WebSocket thuần .NET
│
├── PhotonRealtime/                                   [TẦNG 2: TRÁI TIM KẾT NỐI PHOTON CLOUD - 15 FILES]
│   └── Code/                                         (Đã có sẵn #if SUPPORTED_UNITY tách rời UnityEngine!)
│       ├── AppSettings.cs                            🟢 [KEEP] Cấu hình AppId, Region, Protocol, Port
│       ├── ConnectionHandler.cs                      🟢 [KEEP] Luồng giữ kết nối ngầm (Fallback thread)
│       ├── Extensions.cs                             🟢 [KEEP] Hàm mở rộng cho Hashtable (Merge, StripKeysWithNullValues)
│       ├── FriendInfo.cs                             🟢 [KEEP] Model trạng thái bạn bè (Online, Room đang ở)
│       ├── LoadBalancingClient.cs                    🟢 [KEEP] (4,211 dòng) Trái tim điều phối Master/Game Server, Lobby, Room, Events
│       ├── LoadbalancingPeer.cs                      🟢 [KEEP] (2,196 dòng) Đóng gói các OperationCode gửi lên Photon Server
│       ├── PhotonPing.cs                             🟢 [KEEP] Các lớp Ping đo độ trễ (PingMono thuần .NET Socket)
│       ├── PhotonPingClasses.cs                      🟢 [KEEP] File giữ chỗ (1 dòng)
│       ├── Player.cs                                 🟢 [KEEP] Thông tin người chơi (ActorNumber, NickName, CustomProperties)
│       ├── Region.cs                                 🟢 [KEEP] Model dữ liệu cụm máy chủ (eu, asia, jp, us...)
│       ├── RegionHandler.cs                          🟢 [KEEP] Tự động Ping và chọn cụm máy chủ có Ping thấp nhất
│       ├── Room.cs                                   🟢 [KEEP] Quản lý phòng hiện tại (MasterClientId, MaxPlayers, IsOpen, Players)
│       ├── RoomInfo.cs                               🟢 [KEEP] Thông tin tóm tắt phòng khi xem danh sách ở Lobby
│       ├── SupportLogger.cs                          🟢 [KEEP] Ghi log thống kê mạng, ping, trạng thái kết nối
│       └── WebRpc.cs                                 🟢 [KEEP] Gọi Webhook HTTP thông qua Photon Cloud
│
├── PhotonUnityNetworking/                            [TẦNG 3: ĐỒNG BỘ GAMEPLAY CẤP CAO (PUN 2)]
│   ├── Code/
│   │   ├── Enums.cs                                  🟢 [KEEP] (100% Pure C#) RpcTarget, ViewSynchronization, OwnershipOption
│   │   ├── CustomTypes.cs                            🟡 [ADAPT] Đăng ký kiểu Vector2, Vector3, Quaternion -> Đổi sang kiểu Godot
│   │   ├── PhotonHandler.cs                          🟡 [ADAPT] Vòng lặp gọi Dispatch/Send -> Đổi sang Godot Autoload Node (_Process)
│   │   ├── PhotonNetwork.cs                          🟡 [ADAPT] (3,172 dòng) Facade tĩnh -> Tách phần Room/RPC/Instantiate sang GodotPhotonService
│   │   ├── PhotonNetworkPart.cs                      🟡 [ADAPT] (2,499 dòng) Phần thực thi RPC, OnPhotonSerializeView, CleanupCache
│   │   ├── PhotonStreamQueue.cs                      🟡 [ADAPT] Nén nhiều mẫu dữ liệu theo frame -> Chỉ cần thay Time.time bằng Time.GetTicksMsec()
│   │   ├── PhotonView.cs                             🟡 [ADAPT] (893 dòng) Định danh đối tượng mạng -> Chuyển thành GodotPhotonView : Node
│   │   ├── PunClasses.cs                             🟡 [ADAPT] (974 dòng) Giữ [PunRPC], PhotonStream, PunEvent, PhotonMessageInfo; đổi DefaultPool sang PackedScene
│   │   ├── ServerSettings.cs                         🔴 [PRUNE] ScriptableObject của Unity (Trong Godot dùng Resource hoặc AppSettings thuần)
│   │   ├── Interfaces/
│   │   │   ├── IPhotonViewCallbacks.cs               🟡 [ADAPT] Giao diện callback của View (đổi tham số GameObject -> Node)
│   │   │   └── IPunCallbacks.cs                      🟡 [ADAPT] Giữ IPunObservable; đổi IPunPrefabPool sang nhận/trả về Godot.Node
│   │   ├── Utilities/
│   │   │   └── NestedComponentUtilities.cs           🔴 [PRUNE] Hàm tìm kiếm Component trên Unity Transform (Godot đã có GetChildren/FindChildren)
│   │   ├── Views/
│   │   │   ├── PhotonTransformView.cs                🟡 [ADAPT] Đồng bộ Position/Rotation kèm bù lag -> Chuyển sang Node3D / Node2D của Godot
│   │   │   ├── PhotonTransformViewClassic.cs         🔴 [PRUNE] Bản cũ phức tạp, đã bị thay thế bởi PhotonTransformView
│   │   │   ├── PhotonAnimatorView.cs                 🔴 [PRUNE] Phụ thuộc cứng vào UnityEngine.Animator (Trong Godot đồng bộ qua AnimationTree/AnimationPlayer)
│   │   │   ├── PhotonRigidbodyView.cs                🔴 [PRUNE] Phụ thuộc Unity Rigidbody (Dễ dàng viết 30 dòng cho Godot RigidBody3D)
│   │   │   └── PhotonRigidbody2DView.cs              🔴 [PRUNE] Phụ thuộc Unity Rigidbody2D
│   │   └── Editor/ (17 files Inspector/Wizard)       🔴 [PRUNE] 100% chỉ chạy trong Unity Editor (UnityEditor namespace)
│   │
│   └── UtilityScripts/                               [TẦNG 3B: CÔNG CỤ PHỤ TRỢ GAMEPLAY]
│       ├── Culling/
│       │   ├── CullArea.cs                           🟡 [ADAPT] Thuật toán chia cây phân vùng không gian (Interest Group) rất hay để giảm băng thông
│       │   ├── CullingHandler.cs                     🟡 [ADAPT] Tự động bật/tắt nhóm nhận gói tin theo vị trí nhân vật
│       │   └── Editor/CullAreaEditor.cs              🔴 [PRUNE] Unity Editor Inspector
│       ├── PhotonPlayer/
│       │   ├── PhotonTeamsManager.cs                 🟡 [ADAPT] Quản lý chia đội (Team) qua Player CustomProperties
│       │   ├── PlayerNumbering.cs                    🟡 [ADAPT] Đánh số thứ tự cố định cho người chơi trong phòng (0, 1, 2...)
│       │   ├── PunPlayerScores.cs                    🟡 [ADAPT] Extension cộng/set điểm số người chơi qua CustomProperties
│       │   ├── PunTeams.cs                           🔴 [PRUNE] Bản cũ (Obsolete), đã được thay bằng PhotonTeamsManager
│       │   └── Editor/ (3 files)                     🔴 [PRUNE] Unity Editor Inspector
│       ├── Room/
│       │   └── CountdownTimer.cs                     🟡 [ADAPT] Bộ đếm ngược đồng bộ tuyệt đối giữa mọi máy qua ServerTimestamp
│       ├── TurnBased/
│       │   └── PunTurnManager.cs                     🟡 [ADAPT] Quản lý lượt đi (Turn-based game) dựa trên Room Properties & RaiseEvent
│       ├── PhotonView/
│       │   └── SmoothSyncMovement.cs                 🟡 [ADAPT] Mẫu nội suy Lerp vị trí/góc xoay đơn giản
│       ├── Prototyping/ (8 files)                    🔴 [PRUNE] Các script mẫu thử nghiệm nhanh của Unity (MoveByKeys, OnClickInstantiate...)
│       ├── Debugging/ (4 files *Gui.cs)              🔴 [PRUNE] Dùng hệ thống IMGUI (OnGUI) cũ của Unity
│       └── UI/ (7 files)                             🔴 [PRUNE] Phụ thuộc hoàn toàn vào UnityEngine.UI (Canvas, Toggle, ScrollRect)
│
├── PhotonChat/                                       [TẦNG 4A: HỆ THỐNG CHAT ĐỘC LẬP - 13 FILES]
│   └── Code/                                         (100% biên dịch thẳng sang Godot 4 C# không cần sửa!)
│       ├── ChannelCreationOptions.cs                 🟢 [KEEP] Tùy chọn tạo kênh chat
│       ├── ChannelWellKnownProperties.cs             🟢 [KEEP] Thuộc tính kênh chat
│       ├── ChatAppSettings.cs                        🟢 [KEEP] Cấu hình kết nối Photon Chat
│       ├── ChatChannel.cs                            🟢 [KEEP] Quản lý lịch sử tin nhắn trong kênh
│       ├── ChatClient.cs                             🟢 [KEEP] (1,766 dòng) Client kết nối máy chủ Chat, gửi tin riêng/kênh, trạng thái bạn bè
│       ├── ChatDisconnectCause.cs                    🟢 [KEEP] Enum nguyên nhân ngắt kết nối Chat
│       ├── ChatEventCode.cs                          🟢 [KEEP] Mã sự kiện Chat
│       ├── ChatOperationCode.cs                      🟢 [KEEP] Mã lệnh Chat
│       ├── ChatParameterCode.cs                      🟢 [KEEP] Mã tham số gói tin Chat
│       ├── ChatPeer.cs                               🟢 [KEEP] Peer kết nối cấp thấp của Chat
│       ├── ChatState.cs                              🟢 [KEEP] Trạng thái máy khách Chat
│       ├── ChatUserStatus.cs                         🟢 [KEEP] Trạng thái Online/Away/DND
│       └── IChatClientListener.cs                    🟢 [KEEP] Interface nhận callback tin nhắn đến
│
└── PhotonVoice/                                      [TẦNG 4B: ĐÀM THOẠI ÂM THANH (VOICE CHAT)]
    ├── PhotonVoiceApi/
    │   ├── Core/ (30 files, gồm POpusCodec)          🟢 [KEEP] 100% Pure C# xử lý nén âm thanh Opus, bộ đệm đồng bộ AudioSyncBuffer, VoiceClient
    │   ├── LoadBalancingTransport.cs                 🟢 [KEEP] Truyền luồng âm thanh qua LoadBalancingClient
    │   ├── LoadBalancingTransport2.cs                🟢 [KEEP] Bản tối ưu truyền tải frame âm thanh
    │   ├── PhotonTransport.cs                        🟢 [KEEP] Giao thức đóng gói kênh thoại
    │   └── Platforms/ (Unity, Apple, Windows)        🔴 [PRUNE] Phụ thuộc AudioClip/Microphone của Unity (Trong Godot cần nối với AudioStreamGenerator / AudioEffectCapture)
    ├── PhotonVoiceLibs/ (opus_egpv.dll, webrtc...)   🟡 [ADAPT] Thư viện native C/C++ giải mã Opus & khử vọng (chỉ giữ bản x86_64 nếu dùng Voice)
    └── Code/ (Recorder.cs, Speaker.cs, Editor/...)   🔴 [PRUNE] Gắn chặt với Unity AudioSource & MonoBehaviour
```

---

## Phần 2: Phân tích Chi tiết Từng File Logic & Khuyến nghị Chọn lọc

### 2.1. Tầng 1 & Tầng 2: `PhotonLibs` + `PhotonRealtime/Code` (Giữ nguyên 100%)

Đây là phát hiện quan trọng nhất khi mổ xẻ mã nguồn Photon:
- Trong thư mục [PhotonLibs/netstandard2.0/Photon3Unity3D.dll](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonLibs/netstandard2.0/Photon3Unity3D.dll), ExitGames cung cấp sẵn bản build chuẩn `.NET Standard 2.0` của namespace `ExitGames.Client.Photon` (`PhotonPeer`, `StreamBuffer`, `Protocol`, `Hashtable`, `SendOptions`, `EventData`, `OperationResponse`). Bản DLL này **không hề tham chiếu tới `UnityEngine.dll`** và nạp thẳng vào Godot 4 (.NET 8) mượt mà.
- Toàn bộ 15 file trong [PhotonRealtime/Code/](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code) đều có cấu trúc đầu file như sau:
  ```csharp
  #if UNITY_4_7 || UNITY_5 || UNITY_5_3_OR_NEWER
  #define SUPPORTED_UNITY
  #endif
  ```
  Khi đưa sang dự án **Godot 4 C#**, các hằng số biên dịch `UNITY_*` không được định nghĩa, nên `SUPPORTED_UNITY` tự động tắt. Nhờ vậy, mọi đoạn code phụ thuộc `UnityEngine` (như kế thừa `MonoBehaviour` trong `ConnectionHandler.cs` hay `SupportLogger.cs`) đều tự động chuyển sang nhánh `#else` là các class C# thuần!
- **Lưu ý thực chiến duy nhất khi biên dịch trên .NET 8 (`CS0104` Hashtable)**: Trong 12 file của `PhotonRealtime/Code/`, dòng khai báo `using Hashtable = ExitGames.Client.Photon.Hashtable;` được đặt bên trong `#if SUPPORTED_UNITY || NETFX_CORE`. Khi dùng `netstandard2.0/Photon3Unity3D.dll` trên .NET 8 (nơi `SUPPORTED_UNITY = false`), chỉ cần đổi dòng đó thành `#if true` (đã được xử lý sẵn trong thư mục `godot-photon-bridge/PhotonRealtime/` của bạn) là toàn bộ 15 file `PhotonRealtime` và 13 file `PhotonChat` biên dịch **0 lỗi (`0 Error(s)`)**!

| File trong `PhotonRealtime/Code/` | Số dòng | Vai trò & Kiến trúc | Phụ thuộc Unity | Khuyến nghị |
| :--- | :---: | :--- | :--- | :---: |
| [LoadBalancingClient.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/LoadBalancingClient.cs) | 4,211 | **Trái tim của Photon**: Quản lý State Machine kết nối (`NameServer` -> `MasterServer` -> `GameServer`), tạo/vào phòng (`OpCreateRoom`, `OpJoinRoom`, `OpJoinRandomRoom`), gửi sự kiện (`OpRaiseEvent`), và điều phối 7 nhóm Callback (`Connection`, `Matchmaking`, `InRoom`, `Lobby`, `OnEvent`, `WebRpc`, `ErrorInfo`). | Đã bọc `#if SUPPORTED_UNITY` | 🟢 **KEEP** |
| [LoadbalancingPeer.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/LoadbalancingPeer.cs) | 2,196 | Lớp kế thừa `PhotonPeer`, chịu trách nhiệm đóng gói tham số nhị phân cho từng lệnh mạng cấp thấp (`OpAuthenticate`, `OpSetPropertiesOfActor`, `OpSetPropertiesOfRoom`, `OpRaiseEvent`, `OpFindFriends`). | Đã bọc `#if SUPPORTED_UNITY` | 🟢 **KEEP** |
| [Room.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/Room.cs) | 564 | Đại diện cho phòng chơi hiện tại. Quản lý danh sách `Players` (`Dictionary<int, Player>`), `MasterClientId`, `IsOpen`, `IsVisible`, `MaxPlayers`, và đồng bộ `CustomProperties` (qua cơ chế CAS - Check And Swap). | Không có (`UsesUnity: False`) | 🟢 **KEEP** |
| [RoomInfo.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/RoomInfo.cs) | 275 | Chứa thông tin tóm tắt của các phòng nhận được từ Lobby (`Name`, `PlayerCount`, `MaxPlayers`, `RemovedFromList`, `CustomProperties`). | Không có | 🟢 **KEEP** |
| [Player.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/Player.cs) | 452 | Đại diện cho một người chơi trong phòng (`ActorNumber`, `NickName`, `UserId`, `IsMasterClient`, `IsInactive`, `CustomProperties`, `TagObject`). | Đã bọc `#if SUPPORTED_UNITY` | 🟢 **KEEP** |
| [RegionHandler.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/RegionHandler.cs) | 691 | Nhận danh sách cụm máy chủ từ NameServer và chạy `PhotonPing` song song để tìm cụm máy chủ có độ trễ (Ping) thấp nhất cho người chơi. | Đã bọc `#if SUPPORTED_UNITY` (tự dùng ThreadPool C# khi ngoài Unity) | 🟢 **KEEP** |
| [PhotonPing.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/PhotonPing.cs) | 477 | Thực thi `PingMono` sử dụng `System.Net.Sockets.Socket` thuần của .NET để gửi gói UDP đo RTT tới các Region của Photon. | Đã bọc `#if UNITY_WEBGL` | 🟢 **KEEP** |
| [ConnectionHandler.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/ConnectionHandler.cs) | 186 | Tạo một luồng chạy ngầm (`SupportClass.StartBackgroundCalls`) gửi gói tin ACK giữ kết nối (`SendAcksOnly`) khi luồng chính bị treo lúc chuyển Scene hoặc thu nhỏ cửa sổ. | Đã bọc `#if SUPPORTED_UNITY` | 🟢 **KEEP** |
| [AppSettings.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/AppSettings.cs) | 154 | Class POCO chứa cấu hình kết nối: `AppIdRealtime`, `AppIdChat`, `AppIdVoice`, `AppVersion`, `FixedRegion`, `Server`, `Port`, `Protocol` (UDP/TCP/WSS). | Không có | 🟢 **KEEP** |
| [Extensions.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/Extensions.cs) | 216 | Các phương thức mở rộng giúp gộp `Hashtable` (`Merge`, `MergeStringKeys`) và lọc bỏ các key có giá trị `null` khi cập nhật `CustomProperties`. | Đã bọc `#if SUPPORTED_UNITY` | 🟢 **KEEP** |
| [SupportLogger.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/SupportLogger.cs) | 399 | Tự động lắng nghe mọi callback của `LoadBalancingClient` để in log chi tiết quá trình kết nối, vào phòng, ping. | Đã bọc `#if SUPPORTED_UNITY` | 🟢 **KEEP** |
| [FriendInfo.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/FriendInfo.cs), [Region.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/Region.cs), [WebRpc.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/WebRpc.cs), [PhotonPingClasses.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonRealtime/Code/PhotonPingClasses.cs) | 328 (tổng) | Các cấu trúc dữ liệu cho danh sách bạn bè, vùng máy chủ và phản hồi WebRPC. | Không có | 🟢 **KEEP** |

---

### 2.2. Tầng 3: `PhotonUnityNetworking/Code` (Nơi cần Chọn lọc & Chuyển đổi sang Godot 4 C#)

Tầng này không có `#if SUPPORTED_UNITY` vì nó được thiết kế làm cầu nối giữa `PhotonRealtime` và hệ thống `GameObject`/`MonoBehaviour` của Unity. Tuy nhiên, khi bóc tách ra, ta thấy **chỉ có 6 cơ chế thực sự có giá trị** cần giữ lại và chuyển sang Godot 4 C#, còn lại có thể loại bỏ hoàn toàn:

| File trong `PhotonUnityNetworking/Code/` | Số dòng | Chức năng cốt lõi | Phụ thuộc Unity & Giải pháp trên Godot 4 C# | Khuyến nghị |
| :--- | :---: | :--- | :--- | :---: |
| [Enums.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Enums.cs) | 94 | Định nghĩa `RpcTarget` (`All`, `Others`, `MasterClient`, `AllBuffered`...), `ViewSynchronization` (`Off`, `ReliableDeltaCompressed`, `Unreliable`, `UnreliableOnChange`), `OwnershipOption`. | **Không có bất kỳ phụ thuộc Unity nào!** | 🟢 **KEEP** |
| [CustomTypes.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/CustomTypes.cs) | 177 | Đăng ký hàm `Serialize`/`Deserialize` nhị phân cho `Vector2` (`'W'`), `Vector3` (`'V'`), `Quaternion` (`'Q'`), `Player` (`'P'`) vào `PhotonPeer.RegisterType`. | Thay `UnityEngine.Vector2/Vector3/Quaternion` (`.x, .y, .z, .w`) bằng `Godot.Vector2/Vector3/Quaternion` (`.X, .Y, .Z, .W`) và bổ sung `Godot.Color` (`'C'`). *(Đã viết sẵn trong `GodotCustomTypes.cs`)* | 🟡 **ADAPT** |
| [PhotonHandler.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/PhotonHandler.cs) | 379 | `MonoBehaviour` chạy ngầm gọi `DispatchIncomingCommands()` và `SendOutgoingCommands()` theo chu kỳ `SendRate` / `SerializationRate`. | Thay bằng một `Godot.Node` Autoload Singleton (`GodotPhotonService.cs`), đặt `ProcessMode = ProcessModeEnum.Always`, chạy trong `_Process(double delta)` và bắt sự kiện tắt game qua `_Notification(NotificationWMCloseRequest)`. | 🟡 **ADAPT** |
| [PunClasses.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/PunClasses.cs) | 974 | Chứa 7 thành phần nhỏ: `[PunRPC]`, `MonoBehaviourPun`, `MonoBehaviourPunCallbacks`, `PhotonMessageInfo`, `PunEvent` (mã sự kiện 200..212), `PhotonStream`, và `DefaultPool`. | - **Giữ nguyên**: `[PunRPC]`, `PhotonMessageInfo`, `PunEvent`, `PhotonStream` (đổi kiểu `Vector` sang Godot).<br/>- **Thay thế**: Đổi `DefaultPool` (`Resources.Load<GameObject>`) sang `GodotScenePool` (`GD.Load<PackedScene>()` + `Instantiate()` + `QueueFree()`).<br/>- **Loại bỏ**: `SceneManagerHelper`. | 🟡 **ADAPT** |
| [PhotonView.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/PhotonView.cs) | 893 | Component gắn vào thực thể mạng để quản lý `ViewID`, `OwnerActorNr`, `CreatorActorNr`, `IsMine`, và danh sách `ObservedComponents` (`IPunObservable`). | Chuyển thành `GodotPhotonView : Node` (`GodotPhotonView.cs`). Khi gắn làm Node con của một `CharacterBody2D`/`CharacterBody3D`, trong `_EnterTree()` nó tự động quét Node cha và các Node con để gom `IPunObservable` và cache các hàm có gắn `[PunRPC]`. | 🟡 **ADAPT** |
| [PhotonNetwork.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/PhotonNetwork.cs) & [PhotonNetworkPart.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/PhotonNetworkPart.cs) | 5,671 (tổng) | Lớp tĩnh khổng lồ vừa bọc lại `LoadBalancingClient`, vừa xử lý cấp phát `ViewID` (`AllocateViewID`), gửi/nhận sự kiện `PunEvent.RPC` (200), `PunEvent.SendSerialize` (201/206), `PunEvent.Instantiation` (202), `PunEvent.Destroy` (204), và đồng bộ chuyển Scene (`AutomaticallySyncScene`). | - **Không nên bê nguyên 5,600 dòng** vì quá nhiều đoạn xử lý `UnityEditor`, `SceneManager`, `GameObject` rườm rà.<br/>- **Nên trích xuất**: Giữ lại đúng 4 luồng giao thức cốt lõi (`AllocateViewID`, `SendRpc`, `RunViewUpdate`, `InstantiateNetworked`, `OnEvent`) gọn trong ~350 dòng tại `GodotPhotonService.cs`, còn các thao tác phòng/lobby gọi trực tiếp qua `Client` (`LoadBalancingClient`). | 🟡 **ADAPT** |
| [PhotonStreamQueue.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/PhotonStreamQueue.cs) | 197 | Hàng đợi giúp ghi mẫu (sample) dữ liệu ở tốc độ khung hình cao (ví dụ 60 FPS) rồi gom lại gửi đi trong 1 gói `PhotonStream` ở tốc độ thấp hơn (ví dụ 10 Hz) và phát lại mượt ở máy nhận. | 98% là C# thuần (`List<object>`). Chỉ có đúng 1 dòng dùng `Time.realtimeSinceStartup` -> thay bằng `Godot.Time.GetTicksMsec() / 1000.0f`. | 🟡 **ADAPT** |
| [Views/PhotonTransformView.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Views/PhotonTransformView.cs) | 131 | Đồng bộ `localPosition`, `localRotation`, `localScale` qua `IPunObservable`, kèm công thức bù trễ mạng (`m_NetworkPosition += m_Direction * lag`) và nội suy `MoveTowards`. | Chuyển thành `GodotPhotonTransformView3D` (cho `Node3D`) và `GodotPhotonTransformView2D` (cho `Node2D`) dùng `Position.MoveToward` và `Quaternion.Slerp` / `Mathf.LerpAngle`. *(Đã viết sẵn trong `GodotPhotonTransformViews.cs`)* | 🟡 **ADAPT** |
| [Interfaces/IPunCallbacks.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Interfaces/IPunCallbacks.cs) & [IPhotonViewCallbacks.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Interfaces/IPhotonViewCallbacks.cs) | 181 (tổng) | Khai báo `IPunObservable`, `IPunOwnershipCallbacks`, `IPunInstantiateMagicCallback`, `IPunPrefabPool`. | Giữ `IPunObservable` và `IGodotPunScenePool`. | 🟡 **ADAPT** |
| [ServerSettings.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/ServerSettings.cs) | 100 | `ScriptableObject` lưu `AppSettings` và danh sách `RpcList` trong thư mục `Resources/` của Unity. | **Loại bỏ**: Trong Godot 4, khởi tạo trực tiếp `new AppSettings { AppIdRealtime = "..." }` hoặc dùng `Godot.Resource` tùy chỉnh. | 🔴 **PRUNE** |
| [Utilities/NestedComponentUtilities.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Utilities/NestedComponentUtilities.cs) | 386 | Các hàm tìm `Component` trên cây `Transform` của Unity tránh cấp phát bộ nhớ rác (GC). | **Loại bỏ**: Cây `Node` của Godot 4 duyệt trực tiếp qua `GetChildren()` rất gọn. | 🔴 **PRUNE** |
| [Views/PhotonTransformViewClassic.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Views/PhotonTransformViewClassic.cs), [PhotonAnimatorView.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Views/PhotonAnimatorView.cs), [PhotonRigidbodyView.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Views/PhotonRigidbodyView.cs), [PhotonRigidbody2DView.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/Code/Views/PhotonRigidbody2DView.cs) | 1,352 (tổng) | Đồng bộ `Animator`, `Rigidbody`, `Rigidbody2D` đặc thù của Unity. | **Loại bỏ**: Hệ thống Animation (`AnimationTree`/`AnimationPlayer`) và Vật lý (`CharacterBody2D/3D`, `RigidBody2D/3D`) của Godot khác hẳn Unity; trong Godot bạn chỉ cần gửi biến trạng thái (`Velocity`, `AnimState`) qua `IPunObservable`. | 🔴 **PRUNE** |
| `PhotonUnityNetworking/Code/Editor/*` (17 files) | ~3,500 | Giao diện Inspector và Wizard cài đặt trong Unity Editor. | **Loại bỏ 100%**. | 🔴 **PRUNE** |

---

### 2.3. Tầng 3B: `PhotonUnityNetworking/UtilityScripts` (Chọn lọc các Pattern hay cho Game)

Trong thư mục `UtilityScripts/`, rất nhiều người lầm tưởng tất cả đều là code mẫu rác của Unity, nhưng thực tế có **6 file chứa thuật toán/pattern mạng rất đáng giữ lại**:

| Nhóm / File trong `UtilityScripts/` | Số dòng | Giá trị Kỹ thuật & Cách dùng trong Godot 4 | Khuyến nghị |
| :--- | :---: | :--- | :---: |
| **1. Đồng bộ Thời gian & Lượt chơi**<br/>- [CountdownTimer.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/Room/CountdownTimer.cs)<br/>- [PunTurnManager.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/TurnBased/PunTurnManager.cs) | 176<br/>430 | - **`CountdownTimer`**: Thay vì gửi gói tin đếm ngược mỗi giây, nó chỉ lưu đúng 1 số nguyên `StartTime = PhotonNetwork.ServerTimestamp` vào `Room.CustomProperties`. Mọi máy khách tự tính thời gian còn lại dựa trên đồng hồ máy chủ. Bỏ tham chiếu `UnityEngine.UI.Text` là dùng ngay trên Godot.<br/>- **`PunTurnManager`**: Quản lý game theo lượt (Turn-based / Card game / Board game) hoàn toàn bằng `Room.CustomProperties` và `OpRaiseEvent`. Rất dễ chuyển sang kế thừa `Godot.Node`. | 🟡 **ADAPT** *(Rất đáng giữ nếu làm game có đếm giờ / theo lượt)* |
| **2. Quản lý Đội, Thứ tự & Điểm số**<br/>- [PhotonTeamsManager.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/PhotonPlayer/PhotonTeamsManager.cs)<br/>- [PlayerNumbering.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/PhotonPlayer/PlayerNumbering.cs)<br/>- [PunPlayerScores.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/PhotonPlayer/PunPlayerScores.cs) | 628<br/>267<br/>62 | Lưu trữ đội (`Team`), số thứ tự ghế ngồi cố định trong phòng (`0..MaxPlayers-1`) và điểm số (`Score`) vào `Player.CustomProperties`, tự động cập nhật khi có người vào/ra phòng. Chỉ cần bỏ vỏ `MonoBehaviour` là chạy 100% trên C# thuần. | 🟡 **ADAPT** *(Đáng giữ cho Lobby/Phòng chờ)* |
| **3. Tối ưu Băng thông theo Vùng (Interest Management)**<br/>- [CullArea.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/Culling/CullArea.cs)<br/>- [CullingHandler.cs](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/repo/PhotonUnityNetworking/UtilityScripts/Culling/CullingHandler.cs) | 478<br/>234 | Chia bản đồ lớn thành cây lưới ô vuông (Subdivision Tree), ánh xạ mỗi ô với một `InterestGroup` (`byte`) của Photon. Khi nhân vật di chuyển sang ô nào, máy khách gọi `OpChangeGroups` để chỉ nhận gói tin từ các ô lân cận. | 🟡 **ADAPT** *(Giữ nếu làm game bản đồ rộng / đông người chơi)* |
| **4. Các thư mục còn lại**<br/>- `Prototyping/*` (8 files)<br/>- `Debugging/*Gui.cs` (4 files)<br/>- `UI/*` (7 files)<br/>- `PhotonPlayer/PunTeams.cs` | ~1,600 | Toàn bộ là script thử nghiệm gắn với `Input` cũ của Unity, giao diện `OnGUI` hoặc `UnityEngine.UI`. | 🔴 **PRUNE** *(Xóa bỏ 100%)* |

---

### 2.4. Tầng 4: `PhotonChat` & `PhotonVoice`

- **`PhotonChat/Code/` (13 files - 2,909 dòng)**:
  - **Đánh giá**: 🟢 **KEEP 100%**. Giống hệt `PhotonRealtime`, các file trong `PhotonChat/Code/` (`ChatClient.cs`, `ChatPeer.cs`, `ChatChannel.cs`...) đã bọc sẵn `#if SUPPORTED_UNITY`. Khi đưa sang Godot 4 C#, cả 13 file biên dịch thành công ngay lập tức mà **không phải sửa một dòng code nào**! Bạn chỉ cần gọi `chatClient.Service()` trong hàm `_Process()` của Godot.
- **`PhotonVoice/`**:
  - Phần lõi `PhotonVoice/PhotonVoiceApi/Core/` (30 files, bao gồm bộ mã hóa `POpusCodec`, `VoiceClient.cs`, `AudioSyncBuffer.cs`) và `LoadBalancingTransport.cs` là **100% Pure C#** (🟢 **KEEP** nếu cần làm tính năng Voice Chat).
  - Phần `PhotonVoice/Code/` (`Recorder.cs`, `Speaker.cs`) và `Platforms/Unity/` phụ thuộc vào `AudioClip` và `Microphone` của Unity (🔴 **PRUNE**), nếu muốn dùng trên Godot 4 thì nối `VoiceClient` với `AudioEffectCapture` (thu mic) và `AudioStreamGeneratorPlayback` (phát loa) của Godot.

---

## Phần 3: Bảng Ánh xạ (Mapping) Khi Chuyển Code Mạng Unity sang Godot 4 C#

Khi bạn đọc hoặc chuyển đổi bất kỳ đoạn code mạng Photon nào từ dự án Unity sang Godot 4 C#, hãy áp dụng bảng quy đổi tương đương 1-1 sau:

| Khái niệm / API trong Unity + PUN 2 | Tương đương trong Godot 4 C# + `GodotPunBridge` | Ghi chú kỹ thuật |
| :--- | :--- | :--- |
| `MonoBehaviour` / `MonoBehaviourPun` | `Godot.Node` (hoặc `Node2D`, `Node3D`, `CharacterBody3D`) | Trong Godot 4 C#, nhớ thêm từ khóa `partial` (`public partial class MyPlayer : CharacterBody3D`). |
| `Awake()` / `OnEnable()` | `_EnterTree()` hoặc `_Ready()` | Đăng ký callback mạng (`Client.AddCallbackTarget(this)`) trong `_EnterTree()`. |
| `OnDisable()` / `OnDestroy()` | `_ExitTree()` | Hủy đăng ký callback (`Client.RemoveCallbackTarget(this)`) trong `_ExitTree()`. |
| `Update()` / `Time.deltaTime` | `_Process(double delta)` | Dùng để chạy `DispatchIncomingCommands()`, `SendOutgoingCommands()` và nội suy vị trí. |
| `DontDestroyOnLoad(this.gameObject)` | **Project Settings -> Globals -> Autoload** | Thêm `GodotPhotonService` vào Autoload của Godot và đặt `ProcessMode = ProcessModeEnum.Always`. |
| `OnApplicationQuit()` | `_Notification(int what)` với `what == NotificationWMCloseRequest` | Gọi `Client.Disconnect()` để báo ngay cho máy chủ Photon khi người chơi đóng cửa sổ game. |
| `UnityEngine.Vector2` / `Vector3` (`v.x, v.y, v.z`) | `Godot.Vector2` / `Vector3` (`v.X, v.Y, v.Z`) | Thuộc tính tọa độ trong Godot 4 C# viết hoa (`X`, `Y`, `Z`). |
| `UnityEngine.Quaternion` (`Quaternion.identity`) | `Godot.Quaternion` (`Quaternion.Identity`) | Đã được đăng ký tự động qua `GodotCustomTypes.Register()`. |
| `Resources.Load<GameObject>(path)` | `GD.Load<PackedScene>("res://...")` | Trong Godot, mỗi Prefab là một file `.tscn` (`PackedScene`). |
| `GameObject.Instantiate(prefab, pos, rot)` | `packedScene.Instantiate<Node3D>()` | Gán `Position = pos`, `Quaternion = rot` và thêm vào cây bằng `GetTree().CurrentScene.AddChild(instance)`. |
| `GameObject.Destroy(go)` | `node.QueueFree()` | Giải phóng Node an toàn ở cuối frame của Godot. |
| `SceneManager.LoadScene(name)` | `GetTree().ChangeSceneToFile("res://...")` | Vì `GodotPhotonService` là Autoload nằm trên `root` của `SceneTree`, việc đổi Scene chính không làm đứt kết nối Photon! |
| `Debug.Log` / `Debug.LogWarning` / `Debug.LogError` | `GD.Print` / `GD.PushWarning` / `GD.PrintErr` | In trực tiếp ra bảng Output/Debugger của Godot Editor. |

---

## Phần 4: Cấu trúc Bộ Khung `godot-photon-bridge` Đã Tạo Sẵn Cho Bạn

Để bạn không phải mất công lọc thủ công hàng trăm file, tôi đã trích xuất sẵn toàn bộ phần lõi sạch nhất vào thư mục [godot-photon-bridge](file:///C:/Users/nam/.gemini/antigravity/scratch/photon-godot-analysis/godot-photon-bridge):

```text
godot-photon-bridge/
├── GodotPhotonBridge.csproj                   # File project .NET 8 cấu hình sẵn GodotSharp + Photon3Unity3D.dll
├── PhotonLibs/
│   └── Photon3Unity3D.dll                     # Bản .NET Standard 2.0 sạch bóng UnityEngine
├── PhotonRealtime/                            # 15 file C# nguyên bản của PhotonRealtime (tự tắt SUPPORTED_UNITY)
│   ├── AppSettings.cs
│   ├── ConnectionHandler.cs
│   ├── Extensions.cs
│   ├── FriendInfo.cs
│   ├── LoadBalancingClient.cs
│   ├── LoadbalancingPeer.cs
│   ├── PhotonPing.cs
│   ├── PhotonPingClasses.cs
│   ├── Player.cs
│   ├── Region.cs
│   ├── RegionHandler.cs
│   ├── Room.cs
│   ├── RoomInfo.cs
│   ├── SupportLogger.cs
│   └── WebRpc.cs
├── PhotonChat/                                # 13 file C# nguyên bản của PhotonChat (tùy chọn giữ hoặc xóa nếu không dùng Chat)
│   └── *.cs
└── GodotPunBridge/                            # 5 file cầu nối thay thế tầng PUN 2 cho Godot 4 C#
    ├── Enums.cs                               # RpcTarget, ViewSynchronization, OwnershipOption
    ├── GodotCustomTypes.cs                    # Tuần tự hóa nhị phân Godot.Vector2, Vector3, Quaternion, Color, Player
    ├── GodotPhotonStream.cs                   # [PunRPC], PhotonStream, PhotonMessageInfo, PunEvent, GodotScenePool
    ├── GodotPhotonView.cs                     # Node định danh ViewID, IsMine, Owner, RPC(), IPunObservable
    ├── GodotPhotonService.cs                  # Autoload Singleton điều phối vòng lặp mạng, RPC, Sync, InstantiateNetworked
    └── GodotPhotonTransformViews.cs           # Đồng bộ mượt Node3D & Node2D có bù trễ mạng (Lag Compensation)
```

### Ví dụ Cách Sử dụng Trong Dự án Godot 4 C# của Bạn

#### 1. Kết nối tới Photon Cloud và Vào Phòng
```csharp
using Godot;
using Photon.Realtime;
using Photon.Pun.GodotBridge;

public partial class LobbyManager : Node, IConnectionCallbacks, IMatchmakingCallbacks
{
    public override void _EnterTree()
    {
        GodotPhotonService.Instance.Client.AddCallbackTarget(this);
    }

    public override void _ExitTree()
    {
        GodotPhotonService.Instance?.Client.RemoveCallbackTarget(this);
    }

    public void StartConnect(string appId)
    {
        var settings = new AppSettings
        {
            AppIdRealtime = appId,
            FixedRegion = "asia", // hoặc "jp", "sg"
            AppVersion = "1.0"
        };
        GodotPhotonService.Instance.ConnectUsingSettings(settings);
    }

    public void OnConnectedToMaster()
    {
        GD.Print("Đã kết nối tới Photon Master Server! Đang tìm hoặc tạo phòng...");
        GodotPhotonService.Instance.Client.OpJoinRandomOrCreateRoom(null, new RoomOptions { MaxPlayers = 4 });
    }

    public void OnJoinedRoom()
    {
        GD.Print($"Đã vào phòng: {GodotPhotonService.Instance.CurrentRoom.Name}");
        // Khởi tạo nhân vật từ PackedScene "res://Prefabs/Player3D.tscn" đồng bộ cho mọi người chơi
        GodotPhotonService.Instance.InstantiateNetworked(
            "res://Prefabs/Player3D.tscn",
            new Vector3(0, 1, 0),
            Quaternion.Identity
        );
    }

    // Các callback bắt buộc khác của interface...
    public void OnConnected() { }
    public void OnDisconnected(DisconnectCause cause) => GD.PrintErr($"Ngắt kết nối: {cause}");
    public void OnRegionListReceived(RegionHandler regionHandler) { }
    public void OnCustomAuthenticationResponse(System.Collections.Generic.Dictionary<string, object> data) { }
    public void OnCustomAuthenticationFailed(string debugMessage) { }
    public void OnFriendListUpdate(System.Collections.Generic.List<FriendInfo> friendList) { }
    public void OnCreatedRoom() { }
    public void OnCreateRoomFailed(short returnCode, string message) { }
    public void OnJoinRoomFailed(short returnCode, string message) { }
    public void OnJoinRandomFailed(short returnCode, string message) { }
    public void OnLeftRoom() { }
}
```

#### 2. Viết Script Nhân vật có `IsMine`, `[PunRPC]` và `OnPhotonSerializeView` trên Godot 4
```csharp
using Godot;
using Photon.Pun;
using Photon.Pun.GodotBridge;

public partial class NetworkPlayer3D : CharacterBody3D, IPunObservable
{
    private GodotPhotonView _photonView;
    public int Health = 100;

    public override void _Ready()
    {
        // Lấy GodotPhotonView gắn làm Node con của CharacterBody3D này
        _photonView = GodotPhotonView.Get(this);
    }

    public override void _PhysicsProcess(double delta)
    {
        // Chỉ điều khiển nhân vật nếu thuộc sở hữu của máy mình (IsMine)
        if (_photonView == null || !_photonView.IsMine) return;

        Vector2 inputDir = Input.GetVector("ui_left", "ui_right", "ui_up", "ui_down");
        Velocity = new Vector3(inputDir.X * 5f, Velocity.Y, inputDir.Y * 5f);
        MoveAndSlide();

        if (Input.IsActionJustPressed("ui_accept"))
        {
            // Gọi hàm từ xa (RPC) trên tất cả các máy khách trong phòng
            _photonView.RPC(nameof(RpcTakeDamage), RpcTarget.All, 10);
        }
    }

    [PunRPC]
    public void RpcTakeDamage(int damage, PhotonMessageInfo info)
    {
        Health -= damage;
        GD.Print($"Nhân vật {_photonView.ViewID} nhận {damage} sát thương từ {info.Sender?.NickName}. Máu còn: {Health}");
    }

    // Đồng bộ biến liên tục qua PhotonStream
    public void OnPhotonSerializeView(PhotonStream stream, PhotonMessageInfo info)
    {
        stream.Serialize(ref Health);
    }
}
```
