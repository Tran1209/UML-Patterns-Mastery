namespace SurvivalHeroes.Core.ObjectPool
{
    using System;
    using System.Collections.Generic;
    using Godot;

    /// <summary>
    /// [Object Pool Pattern] Bộ quản lý tái sử dụng Node3D tổng quát nhằm triệt tiêu cấp phát rác (GC Spike).
    /// </summary>
    public sealed class GodotNodePool<T> where T : Node3D
    {
        private readonly Stack<T> _inactiveStack = new();
        private readonly Func<T> _createFunc;
        private readonly Action<T> _onGet;
        private readonly Action<T> _onRelease;

        public int CountInactive => _inactiveStack.Count;
        public int TotalCreated { get; private set; }

        public GodotNodePool(Func<T> createFunc, Action<T> onGet = null, Action<T> onRelease = null, int prewarmCount = 0)
        {
            _createFunc = createFunc ?? throw new ArgumentNullException(nameof(createFunc));
            _onGet = onGet;
            _onRelease = onRelease;

            for (int i = 0; i < prewarmCount; i++)
            {
                T item = _createFunc();
                TotalCreated++;
                _onRelease?.Invoke(item);
                _inactiveStack.Push(item);
            }
        }

        public T Get()
        {
            T item = _inactiveStack.Count > 0 ? _inactiveStack.Pop() : CreateNew();
            _onGet?.Invoke(item);
            return item;
        }

        public void Release(T item)
        {
            if (item == null) return;
            _onRelease?.Invoke(item);
            _inactiveStack.Push(item);
        }

        private T CreateNew()
        {
            TotalCreated++;
            return _createFunc();
        }
    }
}
