// ----------------------------------------------------------------------------
// GlobalUsings.cs
// Giải quyết xung đột tên (CS0104) giữa ExitGames.Client.Photon.Hashtable và
// System.Collections.Hashtable khi biên dịch PhotonRealtime bên ngoài Unity
// (khi SUPPORTED_UNITY = false) mà không cần sửa mã nguồn gốc của PhotonRealtime!
// ----------------------------------------------------------------------------

global using Hashtable = ExitGames.Client.Photon.Hashtable;
global using SupportClass = ExitGames.Client.Photon.SupportClass;
