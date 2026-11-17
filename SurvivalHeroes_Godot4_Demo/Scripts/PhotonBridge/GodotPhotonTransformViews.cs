// ----------------------------------------------------------------------------
// GodotPhotonTransformView3D.cs & GodotPhotonTransformView2D.cs
// Chuyển đổi từ: PhotonUnityNetworking/Code/Views/PhotonTransformView.cs
// Chức năng: Đồng bộ mượt vị trí, góc xoay và tỉ lệ của Node3D / Node2D trong Godot 4
// có bù trễ mạng (Lag Extrapolation) và nội suy (Interpolation).
// ----------------------------------------------------------------------------

namespace Photon.Pun.GodotBridge
{
    using System;
    using Godot;

    /// <summary>Đồng bộ Transform cho thực thể 3D (Node3D / CharacterBody3D / RigidBody3D) trong Godot 4.</summary>
    public partial class GodotPhotonTransformView3D : Node, IPunObservable
    {
        [Export] public bool SynchronizePosition { get; set; } = true;
        [Export] public bool SynchronizeRotation { get; set; } = true;
        [Export] public bool SynchronizeScale { get; set; } = false;

        private Node3D _target3D;
        private GodotPhotonView _photonView;

        private float _distance;
        private float _angle;
        private Vector3 _direction;
        private Vector3 _networkPosition;
        private Vector3 _storedPosition;
        private Quaternion _networkRotation = Quaternion.Identity;
        private bool _firstTake = true;

        public override void _Ready()
        {
            _target3D = GetParent() as Node3D;
            _photonView = GodotPhotonView.Get(this);
            if (_target3D != null)
            {
                _storedPosition = _target3D.Position;
                _networkPosition = _target3D.Position;
                _networkRotation = _target3D.Quaternion;
            }
        }

        public override void _Process(double delta)
        {
            if (_target3D == null || _photonView == null || _photonView.IsMine) return;

            float rate = GodotPhotonService.Instance?.SerializationRate ?? 10;
            float stepFactor = (float)delta * rate;

            if (SynchronizePosition)
            {
                _target3D.Position = _target3D.Position.MoveToward(_networkPosition, _distance * stepFactor);
            }

            if (SynchronizeRotation && _angle > 0.0001f)
            {
                float weight = Mathf.Clamp(stepFactor, 0f, 1f);
                _target3D.Quaternion = _target3D.Quaternion.Slerp(_networkRotation, weight);
            }
        }

        public void OnPhotonSerializeView(PhotonStream stream, PhotonMessageInfo info)
        {
            if (_target3D == null) return;

            if (stream.IsWriting)
            {
                if (SynchronizePosition)
                {
                    _direction = _target3D.Position - _storedPosition;
                    _storedPosition = _target3D.Position;
                    stream.SendNext(_target3D.Position);
                    stream.SendNext(_direction);
                }
                if (SynchronizeRotation)
                {
                    stream.SendNext(_target3D.Quaternion);
                }
                if (SynchronizeScale)
                {
                    stream.SendNext(_target3D.Scale);
                }
            }
            else
            {
                if (SynchronizePosition)
                {
                    _networkPosition = (Vector3)stream.ReceiveNext();
                    _direction = (Vector3)stream.ReceiveNext();

                    if (_firstTake)
                    {
                        _target3D.Position = _networkPosition;
                        _distance = 0f;
                    }
                    else
                    {
                        double serverTime = GodotPhotonService.Instance?.Time ?? info.SentServerTime;
                        float lag = MathF.Abs((float)(serverTime - info.SentServerTime));
                        _networkPosition += _direction * lag;
                        _distance = _target3D.Position.DistanceTo(_networkPosition);
                    }
                }

                if (SynchronizeRotation)
                {
                    _networkRotation = (Quaternion)stream.ReceiveNext();
                    if (_firstTake)
                    {
                        _angle = 0f;
                        _target3D.Quaternion = _networkRotation;
                    }
                    else
                    {
                        _angle = _target3D.Quaternion.AngleTo(_networkRotation);
                    }
                }

                if (SynchronizeScale)
                {
                    _target3D.Scale = (Vector3)stream.ReceiveNext();
                }

                if (_firstTake)
                {
                    _firstTake = false;
                }
            }
        }
    }

    /// <summary>Đồng bộ Transform cho thực thể 2D (Node2D / CharacterBody2D) trong Godot 4.</summary>
    public partial class GodotPhotonTransformView2D : Node, IPunObservable
    {
        [Export] public bool SynchronizePosition { get; set; } = true;
        [Export] public bool SynchronizeRotation { get; set; } = true;
        [Export] public bool SynchronizeScale { get; set; } = false;

        private Node2D _target2D;
        private GodotPhotonView _photonView;

        private float _distance;
        private Vector2 _direction;
        private Vector2 _networkPosition;
        private Vector2 _storedPosition;
        private float _networkRotation;
        private bool _firstTake = true;

        public override void _Ready()
        {
            _target2D = GetParent() as Node2D;
            _photonView = GodotPhotonView.Get(this);
            if (_target2D != null)
            {
                _storedPosition = _target2D.Position;
                _networkPosition = _target2D.Position;
                _networkRotation = _target2D.Rotation;
            }
        }

        public override void _Process(double delta)
        {
            if (_target2D == null || _photonView == null || _photonView.IsMine) return;

            float rate = GodotPhotonService.Instance?.SerializationRate ?? 10;
            float stepFactor = (float)delta * rate;

            if (SynchronizePosition)
            {
                _target2D.Position = _target2D.Position.MoveToward(_networkPosition, _distance * stepFactor);
            }

            if (SynchronizeRotation)
            {
                _target2D.Rotation = Mathf.LerpAngle(_target2D.Rotation, _networkRotation, Mathf.Clamp(stepFactor, 0f, 1f));
            }
        }

        public void OnPhotonSerializeView(PhotonStream stream, PhotonMessageInfo info)
        {
            if (_target2D == null) return;

            if (stream.IsWriting)
            {
                if (SynchronizePosition)
                {
                    _direction = _target2D.Position - _storedPosition;
                    _storedPosition = _target2D.Position;
                    stream.SendNext(_target2D.Position);
                    stream.SendNext(_direction);
                }
                if (SynchronizeRotation)
                {
                    stream.SendNext(_target2D.Rotation);
                }
                if (SynchronizeScale)
                {
                    stream.SendNext(_target2D.Scale);
                }
            }
            else
            {
                if (SynchronizePosition)
                {
                    _networkPosition = (Vector2)stream.ReceiveNext();
                    _direction = (Vector2)stream.ReceiveNext();

                    if (_firstTake)
                    {
                        _target2D.Position = _networkPosition;
                        _distance = 0f;
                    }
                    else
                    {
                        double serverTime = GodotPhotonService.Instance?.Time ?? info.SentServerTime;
                        float lag = MathF.Abs((float)(serverTime - info.SentServerTime));
                        _networkPosition += _direction * lag;
                        _distance = _target2D.Position.DistanceTo(_networkPosition);
                    }
                }

                if (SynchronizeRotation)
                {
                    _networkRotation = (float)stream.ReceiveNext();
                    if (_firstTake)
                    {
                        _target2D.Rotation = _networkRotation;
                    }
                }

                if (SynchronizeScale)
                {
                    _target2D.Scale = (Vector2)stream.ReceiveNext();
                }

                if (_firstTake)
                {
                    _firstTake = false;
                }
            }
        }
    }
}
