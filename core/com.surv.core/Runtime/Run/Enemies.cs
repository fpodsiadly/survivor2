using System;
using System.Collections.Generic;
using System.Linq;
using Surv.Core.Common;

namespace Surv.Core.Run
{
    public enum ChargerState
    {
        Chase,
        Telegraph,
        Charge,
        Recover,
    }

    /// <summary>
    /// Charging enemy (SPEC W20 type 5; PLAN P17): walks toward the player; within range it stops, shows the
    /// direction for at least <see cref="TelegraphSeconds"/>, then charges in a straight line along that locked
    /// direction, then recovers.
    /// </summary>
    public sealed class ChargerBrain
    {
        public ChargerBrain(float walkSpeed, float chargeSpeed, float triggerDistance,
            double telegraphSeconds = 0.7, double chargeSeconds = 0.8, double recoverSeconds = 1.0)
        {
            if (telegraphSeconds < 0.7) throw new ArgumentOutOfRangeException(nameof(telegraphSeconds), "SPEC W20: at least 0.7 s.");
            WalkSpeed = walkSpeed;
            ChargeSpeed = chargeSpeed;
            TriggerDistance = triggerDistance;
            TelegraphSeconds = telegraphSeconds;
            ChargeSeconds = chargeSeconds;
            RecoverSeconds = recoverSeconds;
        }

        public float WalkSpeed { get; }
        public float ChargeSpeed { get; }
        public float TriggerDistance { get; }
        public double TelegraphSeconds { get; }
        public double ChargeSeconds { get; }
        public double RecoverSeconds { get; }

        public ChargerState State { get; private set; } = ChargerState.Chase;

        /// <summary>Locked charge direction, shown to the player during the telegraph.</summary>
        public Vec2 Direction { get; private set; }

        private double _stateTime;

        /// <summary>Advances the state and returns the velocity for this frame.</summary>
        public Vec2 Tick(double deltaSeconds, Vec2 self, Vec2 player)
        {
            _stateTime += deltaSeconds;
            switch (State)
            {
                case ChargerState.Chase:
                    var toPlayer = player - self;
                    if (toPlayer.Length <= TriggerDistance)
                    {
                        Enter(ChargerState.Telegraph);
                        Direction = toPlayer.Normalized;
                        return Vec2.Zero;
                    }
                    return toPlayer.Normalized * WalkSpeed;
                case ChargerState.Telegraph:
                    if (_stateTime >= TelegraphSeconds) Enter(ChargerState.Charge);
                    return Vec2.Zero;
                case ChargerState.Charge:
                    if (_stateTime >= ChargeSeconds)
                    {
                        Enter(ChargerState.Recover);
                        return Vec2.Zero;
                    }
                    return Direction * ChargeSpeed;
                default:
                    if (_stateTime >= RecoverSeconds) Enter(ChargerState.Chase);
                    return Vec2.Zero;
            }
        }

        private void Enter(ChargerState state)
        {
            State = state;
            _stateTime = 0;
        }
    }

    /// <summary>One boss attack (SPEC W12): damage as a fraction of the player's base max HP.</summary>
    public sealed class BossAttack
    {
        public BossAttack(string id, double damageFraction, double telegraphSeconds)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Attack id is required.", nameof(id));
            Id = id;
            DamageFraction = damageFraction;
            TelegraphSeconds = telegraphSeconds;
        }

        public string Id { get; }
        public double DamageFraction { get; }
        public double TelegraphSeconds { get; }
    }

    public sealed class BossDefinition
    {
        public BossDefinition(string id, IReadOnlyList<BossAttack> attacks, double contactDamageFraction)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Boss id is required.", nameof(id));
            Id = id;
            Attacks = attacks ?? throw new ArgumentNullException(nameof(attacks));
            ContactDamageFraction = contactDamageFraction;
        }

        public string Id { get; }
        public IReadOnlyList<BossAttack> Attacks { get; }

        /// <summary>Touching the boss; must stay below the telegraph threshold (PLAN P18).</summary>
        public double ContactDamageFraction { get; }
    }

    /// <summary>Data check for bosses (SPEC W12; PLAN P18). Empty list = valid.</summary>
    public static class BossValidator
    {
        public const double TelegraphThreshold = 0.15;
        public const double MinTelegraphSeconds = 0.7;

        public static IReadOnlyList<string> Validate(BossDefinition boss)
        {
            var errors = new List<string>();
            if (boss.Attacks.Count < 2) errors.Add($"{boss.Id}: at least 2 attacks are required.");
            errors.AddRange(boss.Attacks
                .Where(a => a.DamageFraction >= TelegraphThreshold && a.TelegraphSeconds < MinTelegraphSeconds)
                .Select(a => $"{boss.Id}/{a.Id}: deals {a.DamageFraction:P0} so needs a telegraph of at least {MinTelegraphSeconds} s."));
            if (boss.ContactDamageFraction >= TelegraphThreshold)
            {
                errors.Add($"{boss.Id}: contact damage must stay below {TelegraphThreshold:P0}.");
            }
            return errors;
        }
    }
}
