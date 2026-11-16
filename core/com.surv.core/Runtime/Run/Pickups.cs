using System;
using System.Collections.Generic;

namespace Surv.Core.Run
{
    /// <summary>An enemy as the bomb sees it.</summary>
    public struct BombTarget
    {
        public BombTarget(EnemyClass enemyClass, double maxHealth, double health, bool onScreen)
        {
            Class = enemyClass;
            MaxHealth = maxHealth;
            Health = health;
            OnScreen = onScreen;
        }

        public EnemyClass Class { get; }
        public double MaxHealth { get; }
        public double Health { get; }
        public bool OnScreen { get; }
    }

    public struct BombHit
    {
        public BombHit(double damage, bool killed)
        {
            Damage = damage;
            Killed = killed;
        }

        public double Damage { get; }
        public bool Killed { get; }
    }

    /// <summary>
    /// Bomb pickup (SPEC W18, also the revive blast of W33): kills normal and reinforced enemies on screen;
    /// elites and bosses on screen lose a share of their max HP (default 10%). Killed enemies drop crystals.
    /// </summary>
    public static class Bomb
    {
        public static IReadOnlyList<BombHit> Explode(IReadOnlyList<BombTarget> targets, double eliteBossShare = 0.10)
        {
            var hits = new BombHit[targets.Count];
            for (var i = 0; i < targets.Count; i++)
            {
                var t = targets[i];
                if (!t.OnScreen || t.Health <= 0)
                {
                    hits[i] = new BombHit(0, false);
                    continue;
                }
                var damage = t.Class == EnemyClass.Elite || t.Class == EnemyClass.Boss
                    ? Math.Min(t.Health, t.MaxHealth * eliteBossShare)
                    : t.Health;
                hits[i] = new BombHit(damage, damage >= t.Health);
            }
            return hits;
        }
    }

    /// <summary>A pickup disappears after 60 s spent continuously off screen (SPEC W18; PLAN P13).</summary>
    public sealed class OffscreenTimer
    {
        private readonly double _limitSeconds;
        private double _offscreenFor;

        public OffscreenTimer(double limitSeconds = 60)
        {
            if (limitSeconds <= 0) throw new ArgumentOutOfRangeException(nameof(limitSeconds));
            _limitSeconds = limitSeconds;
        }

        public bool Expired { get; private set; }

        /// <summary>Returns true once the pickup should vanish.</summary>
        public bool Tick(double deltaSeconds, bool onScreen)
        {
            if (Expired) return true;
            _offscreenFor = onScreen ? 0 : _offscreenFor + deltaSeconds;
            Expired = _offscreenFor >= _limitSeconds;
            return Expired;
        }
    }
}
