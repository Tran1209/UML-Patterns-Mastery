namespace SurvivalHeroes.Core.Command
{
    using System;
    using System.Collections.Generic;
    using Godot;

    public interface ICombatCommand
    {
        string CommandName { get; }
        bool CanExecute();
        void Execute();
    }

    public sealed class DelegateCombatCommand : ICombatCommand
    {
        public string CommandName { get; }
        private readonly Func<bool> _canExecute;
        private readonly Action _execute;

        public DelegateCombatCommand(string commandName, Func<bool> canExecute, Action execute)
        {
            CommandName = commandName;
            _canExecute = canExecute;
            _execute = execute;
        }

        public bool CanExecute() => _canExecute != null && _canExecute();
        public void Execute() => _execute?.Invoke();
    }

    /// <summary>
    /// [Command Pattern] Hàng đợi đệm lệnh kỹ năng (Input Buffer 180ms) giúp chuỗi combo LMB/Q/W/E mượt mà.
    /// </summary>
    public sealed class InputBufferQueue
    {
        private struct BufferedEntry
        {
            public ICombatCommand Command;
            public double ExpireTimeSec;
        }

        private readonly Queue<BufferedEntry> _queue = new();
        private readonly double _bufferWindowSec;

        public int BufferedCount => _queue.Count;
        public string LastBufferedSkillName { get; private set; } = "Không";

        public InputBufferQueue(double bufferWindowSec = 0.18)
        {
            _bufferWindowSec = bufferWindowSec;
        }

        public void EnqueueCommand(ICombatCommand command)
        {
            if (command == null) return;
            double now = Time.GetTicksMsec() / 1000.0;
            _queue.Enqueue(new BufferedEntry
            {
                Command = command,
                ExpireTimeSec = now + _bufferWindowSec
            });
            LastBufferedSkillName = command.CommandName;
        }

        public bool ProcessBufferedCommands()
        {
            double now = Time.GetTicksMsec() / 1000.0;
            while (_queue.Count > 0)
            {
                BufferedEntry peek = _queue.Peek();
                if (now > peek.ExpireTimeSec)
                {
                    _queue.Dequeue();
                    continue;
                }

                if (peek.Command.CanExecute())
                {
                    _queue.Dequeue();
                    peek.Command.Execute();
                    return true;
                }
                break;
            }
            return false;
        }

        public void Clear() => _queue.Clear();
    }
}
