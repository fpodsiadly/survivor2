using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Run
{
    /// <summary>Stats that equipment, talents and passive skills change (SPEC W16, W24, W30).</summary>
    public enum Stat
    {
        Attack,
        MaxHealth,
        MoveSpeed,
        CooldownReduction,
        Area,
        Duration,
        ProjectileSpeed,
        PickupRadius,
        XpGain,
        GoldGain,
        DamageReduction,

        /// <summary>% of max HP healed every 5 s.</summary>
        Regeneration,

        /// <summary>Extra healing from meat, as a fraction of the base 30% (talent, SPEC W30).</summary>
        MeatHealing,
    }

    public enum ModifierSource
    {
        Equipment,
        Talent,
        Passive,
    }

    /// <summary>A percentage bonus, e.g. 0.10 = +10%.</summary>
    public sealed class StatModifier
    {
        public StatModifier(Stat stat, double value, ModifierSource source)
        {
            Stat = stat;
            Value = value;
            Source = source;
        }

        public Stat Stat { get; }
        public double Value { get; }
        public ModifierSource Source { get; }
    }

    /// <summary>A passive skill's effect: <see cref="Level1"/> at level 1 to <see cref="Level5"/> at level 5, linear between (PLAN P10).</summary>
    public sealed class PassiveEffect
    {
        public PassiveEffect(string skillId, Stat stat, double level1, double level5)
        {
            SkillId = skillId;
            Stat = stat;
            Level1 = level1;
            Level5 = level5;
        }

        public string SkillId { get; }
        public Stat Stat { get; }
        public double Level1 { get; }
        public double Level5 { get; }

        public double ValueAt(int level)
        {
            if (level < 1 || level > SkillCatalog.MaxLevel) throw new ArgumentOutOfRangeException(nameof(level));
            return Level1 + (Level5 - Level1) * (level - 1) / (SkillCatalog.MaxLevel - 1);
        }
    }

    /// <summary>Base values of the player (SPEC "Wartości bazowe").</summary>
    public sealed class BaseStats
    {
        public double MaxHealth { get; set; } = 100;
        public double MoveSpeed { get; set; } = 3;
        public double PickupRadius { get; set; } = 1.5;
    }

    /// <summary>
    /// Final stats: equipment bonuses add up among themselves; the equipment, talent and passive groups multiply
    /// (SPEC W24, W30). Damage and cooldown reductions combine as 1 − Π(1 − r) so they never reach 100%.
    /// </summary>
    public static class StatCalculator
    {
        private static readonly HashSet<Stat> Reductions = new HashSet<Stat> { Stat.DamageReduction, Stat.CooldownReduction };

        /// <summary>Multiplier for a scaling stat, or the combined fraction for a reduction or additive stat.</summary>
        public static double Factor(Stat stat, IEnumerable<StatModifier> modifiers)
        {
            var bySource = modifiers.Where(m => m.Stat == stat).GroupBy(m => m.Source).Select(g => g.Sum(m => m.Value)).ToList();
            if (Reductions.Contains(stat))
            {
                return 1 - bySource.Aggregate(1.0, (keep, r) => keep * (1 - r));
            }
            return bySource.Aggregate(1.0, (factor, bonus) => factor * (1 + bonus));
        }

        public static double MaxHealth(BaseStats baseStats, double equipmentHealth, IEnumerable<StatModifier> modifiers) =>
            (baseStats.MaxHealth + equipmentHealth) * Factor(Stat.MaxHealth, modifiers);

        public static double Attack(double equipmentAttack, IEnumerable<StatModifier> modifiers) =>
            equipmentAttack * Factor(Stat.Attack, modifiers);

        /// <summary>Passive modifiers of the owned passive skills at their levels.</summary>
        public static IEnumerable<StatModifier> FromPassives(Loadout loadout, IEnumerable<PassiveEffect> effects)
        {
            var bySkill = effects.ToDictionary(e => e.SkillId);
            foreach (var owned in loadout.Passives)
            {
                if (bySkill.TryGetValue(owned.SkillId, out var effect))
                {
                    yield return new StatModifier(effect.Stat, effect.ValueAt(owned.Level), ModifierSource.Passive);
                }
            }
        }
    }

    /// <summary>Player HP with contact damage and invulnerability after a hit (SPEC W13; PLAN P8).</summary>
    public sealed class PlayerHealth
    {
        private readonly double _invulnerableSeconds;
        private double _invulnerableUntil = double.NegativeInfinity;

        public PlayerHealth(double maxHealth, double invulnerableSeconds = 0.5)
        {
            if (maxHealth <= 0) throw new ArgumentOutOfRangeException(nameof(maxHealth));
            MaxHealth = maxHealth;
            Health = maxHealth;
            _invulnerableSeconds = invulnerableSeconds;
        }

        public double MaxHealth { get; }
        public double Health { get; private set; }
        public bool IsDead => Health <= 0;

        public bool IsInvulnerable(double now) => now < _invulnerableUntil;

        /// <summary>Damage per contact hit: rate (fraction of base HP per second) × invulnerability window × base HP.</summary>
        public static double ContactHit(double ratePerSecond, double baseMaxHealth, double invulnerableSeconds = 0.5) =>
            ratePerSecond * invulnerableSeconds * baseMaxHealth;

        /// <summary>Applies a hit unless invulnerable; returns the damage taken.</summary>
        public double TakeHit(double now, double damage, double damageReduction)
        {
            if (IsDead || IsInvulnerable(now) || damage <= 0) return 0;
            var taken = damage * (1 - damageReduction);
            Health = Math.Max(0, Health - taken);
            _invulnerableUntil = now + _invulnerableSeconds;
            return taken;
        }

        public void Heal(double amount)
        {
            if (IsDead || amount <= 0) return;
            Health = Math.Min(MaxHealth, Health + amount);
        }

        /// <summary>Meat heals 30% of max HP, raised by the meat talent (SPEC W18, W30).</summary>
        public static double MeatHeal(double maxHealth, double meatHealingBonus) => maxHealth * 0.30 * (1 + meatHealingBonus);
    }
}
