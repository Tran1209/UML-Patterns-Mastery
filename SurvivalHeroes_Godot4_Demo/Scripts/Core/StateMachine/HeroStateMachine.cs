namespace SurvivalHeroes.Core.StateMachine
{
    using System;

    public interface IHeroState
    {
        string StateDisplayName { get; }
        void Enter();
        void Tick(float delta);
        void Exit();
    }

    /// <summary>
    /// [State Pattern] Máy trạng thái hữu hạn thuần C# (POCO) không phụ thuộc MonoBehaviour/Node.
    /// </summary>
    public sealed class HeroStateMachine
    {
        public IHeroState CurrentState { get; private set; }
        public event Action<IHeroState> OnStateChanged;

        public void Initialize(IHeroState startingState)
        {
            CurrentState = startingState;
            CurrentState?.Enter();
            OnStateChanged?.Invoke(CurrentState);
        }

        public void TransitionTo(IHeroState nextState)
        {
            if (nextState == null || ReferenceEquals(CurrentState, nextState)) return;
            CurrentState?.Exit();
            CurrentState = nextState;
            CurrentState.Enter();
            OnStateChanged?.Invoke(CurrentState);
        }

        public void Tick(float delta) => CurrentState?.Tick(delta);
    }

    public sealed class HeroSimpleState : IHeroState
    {
        public string StateDisplayName { get; }
        public HeroSimpleState(string displayName) => StateDisplayName = displayName;
        public void Enter() { }
        public void Tick(float delta) { }
        public void Exit() { }
    }
}
