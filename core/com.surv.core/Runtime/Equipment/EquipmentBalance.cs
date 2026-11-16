using System;
using System.Collections.Generic;

namespace Surv.Core.Equipment
{
    /// <summary>
    /// Tunable equipment numbers (SPEC W24, W25, W41). Unity fills it from ScriptableObjects;
    /// <see cref="CreateDefault"/> holds the starting values from the spec and is used by tests.
    /// </summary>
    public sealed class EquipmentBalance
    {
        public const int TierCount = 15;

        public EquipmentBalance(
            IReadOnlyList<int> levelCaps,
            IReadOnlyList<double> weaponBaseStat,
            double healthSlotMultiplier,
            double attackPerLevel,
            double healthPerLevel,
            Func<int, long> goldCostForLevel,
            Func<int, long> planCostForLevel)
        {
            if (levelCaps == null || levelCaps.Count != TierCount) throw new ArgumentException("One level cap per tier is required.", nameof(levelCaps));
            if (weaponBaseStat == null || weaponBaseStat.Count != TierCount) throw new ArgumentException("One base stat per tier is required.", nameof(weaponBaseStat));
            for (var i = 1; i < TierCount; i++)
            {
                if (levelCaps[i] < levelCaps[i - 1]) throw new ArgumentException("Level caps must not decrease with tier.", nameof(levelCaps));
            }
            LevelCaps = levelCaps;
            WeaponBaseStat = weaponBaseStat;
            HealthSlotMultiplier = healthSlotMultiplier;
            AttackPerLevel = attackPerLevel;
            HealthPerLevel = healthPerLevel;
            GoldCostForLevel = goldCostForLevel ?? throw new ArgumentNullException(nameof(goldCostForLevel));
            PlanCostForLevel = planCostForLevel ?? throw new ArgumentNullException(nameof(planCostForLevel));
        }

        /// <summary>Max item level per tier (SPEC W25).</summary>
        public IReadOnlyList<int> LevelCaps { get; }

        /// <summary>Base main stat per tier for ATK slots (SPEC W24, RESEARCH §5.3).</summary>
        public IReadOnlyList<double> WeaponBaseStat { get; }

        /// <summary>HP slots use the ATK scale times this factor (PLAN P20).</summary>
        public double HealthSlotMultiplier { get; }

        public double AttackPerLevel { get; }
        public double HealthPerLevel { get; }

        /// <summary>Gold to go from <c>level</c> to <c>level + 1</c>.</summary>
        public Func<int, long> GoldCostForLevel { get; }

        /// <summary>Slot plans to go from <c>level</c> to <c>level + 1</c>.</summary>
        public Func<int, long> PlanCostForLevel { get; }

        public int LevelCap(Tier tier) => LevelCaps[(int)tier];

        public static EquipmentBalance CreateDefault()
        {
            const double legend = 231;
            return new EquipmentBalance(
                levelCaps: new[] { 10, 20, 30, 40, 45, 50, 60, 65, 70, 75, 80, 85, 90, 95, 100 },
                weaponBaseStat: new[]
                {
                    10, 20, 30, 50, 60, 70, 132, 154, 176, 198, legend,
                    legend * 1.1, legend * 1.2, legend * 1.3, legend * 1.4,
                },
                healthSlotMultiplier: 10,
                attackPerLevel: 2,
                healthPerLevel: 20,
                goldCostForLevel: level => 100L * level,
                planCostForLevel: level => 1L + level / 10);
        }
    }
}
