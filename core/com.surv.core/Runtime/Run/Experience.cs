using System;
using Surv.Core.Common;

namespace Surv.Core.Run
{
    /// <summary>In-run level and XP bar (SPEC W14). The curve comes from data.</summary>
    public sealed class ExperienceBar
    {
        private readonly Func<int, long> _xpToNextLevel;

        /// <param name="xpToNextLevel">XP needed to go from the given level to the next; must grow or stay equal.</param>
        public ExperienceBar(Func<int, long> xpToNextLevel)
        {
            _xpToNextLevel = xpToNextLevel ?? throw new ArgumentNullException(nameof(xpToNextLevel));
        }

        public int Level { get; private set; } = 1;
        public long Xp { get; private set; }
        public long XpToNext => _xpToNextLevel(Level);

        /// <summary>Starting value (decyzja autora, PLAN P9): 5 XP for level 1, +3 per level.</summary>
        public static Func<int, long> DefaultCurve => level => 5 + 3L * (level - 1);

        /// <summary>Adds XP, carrying the excess over; returns how many level-ups happened (each one shows a card choice).</summary>
        public int Add(long xp)
        {
            if (xp < 0) throw new ArgumentOutOfRangeException(nameof(xp));
            Xp += xp;
            var levelUps = 0;
            while (Xp >= XpToNext)
            {
                Xp -= XpToNext;
                Level++;
                levelUps++;
            }
            return levelUps;
        }
    }

    public enum CrystalKind
    {
        None,
        Green,
        Blue,
        Yellow,
    }

    public enum EnemyClass
    {
        Normal,

        /// <summary>Strengthened variant that appears after a boss (SPEC W6).</summary>
        Reinforced,
        Elite,
        Boss,
    }

    /// <summary>Which crystal an enemy leaves (SPEC W14; PLAN P5 reading of "elites always yellow").</summary>
    public sealed class CrystalDrops
    {
        private readonly IRandom _random;
        private readonly double _yellowChance;

        public CrystalDrops(IRandom random, double yellowChance = 0.02)
        {
            _random = random ?? throw new ArgumentNullException(nameof(random));
            if (yellowChance < 0 || yellowChance > 1) throw new ArgumentOutOfRangeException(nameof(yellowChance));
            _yellowChance = yellowChance;
        }

        public static int Value(CrystalKind kind)
        {
            switch (kind)
            {
                case CrystalKind.Green: return 1;
                case CrystalKind.Blue: return 5;
                case CrystalKind.Yellow: return 25;
                default: return 0;
            }
        }

        /// <param name="removedByBossEntry">Enemies cleared when a boss enters drop nothing (SPEC W14).</param>
        public CrystalKind Drop(EnemyClass enemy, bool removedByBossEntry = false)
        {
            if (removedByBossEntry) return CrystalKind.None;
            switch (enemy)
            {
                case EnemyClass.Elite:
                case EnemyClass.Boss:
                    return CrystalKind.Yellow;
                case EnemyClass.Reinforced:
                    return _random.NextDouble() < _yellowChance ? CrystalKind.Yellow : CrystalKind.Blue;
                default:
                    return _random.NextDouble() < _yellowChance ? CrystalKind.Yellow : CrystalKind.Green;
            }
        }
    }
}
