using System;

namespace Surv.Core.Equipment
{
    /// <summary>The 15-step rarity ladder from SPEC W23.</summary>
    public enum Tier
    {
        Normal = 0,
        Good = 1,
        Better = 2,
        Excellent = 3,
        Excellent1 = 4,
        Excellent2 = 5,
        Epic = 6,
        Epic1 = 7,
        Epic2 = 8,
        Epic3 = 9,
        Legend = 10,
        Legend1 = 11,
        Legend2 = 12,
        Legend3 = 13,
        Legend4 = 14,
    }

    /// <summary>The six base tiers that own a frame colour; "+N" sub-tiers share the colour of their base (SPEC W23).</summary>
    public enum BaseTier
    {
        Normal,
        Good,
        Better,
        Excellent,
        Epic,
        Legend,
    }

    public static class TierExtensions
    {
        public const Tier Max = Tier.Legend4;

        public static BaseTier GetBase(this Tier tier)
        {
            switch (tier)
            {
                case Tier.Normal: return BaseTier.Normal;
                case Tier.Good: return BaseTier.Good;
                case Tier.Better: return BaseTier.Better;
                case Tier.Excellent:
                case Tier.Excellent1:
                case Tier.Excellent2: return BaseTier.Excellent;
                case Tier.Epic:
                case Tier.Epic1:
                case Tier.Epic2:
                case Tier.Epic3: return BaseTier.Epic;
                case Tier.Legend:
                case Tier.Legend1:
                case Tier.Legend2:
                case Tier.Legend3:
                case Tier.Legend4: return BaseTier.Legend;
                default: throw new ArgumentOutOfRangeException(nameof(tier), tier, null);
            }
        }

        /// <summary>Number of "+" marks shown on the frame (0 for base tiers).</summary>
        public static int PlusMarks(this Tier tier) => (int)tier - (int)tier.GetBase().ToTier();

        public static Tier ToTier(this BaseTier baseTier)
        {
            switch (baseTier)
            {
                case BaseTier.Normal: return Tier.Normal;
                case BaseTier.Good: return Tier.Good;
                case BaseTier.Better: return Tier.Better;
                case BaseTier.Excellent: return Tier.Excellent;
                case BaseTier.Epic: return Tier.Epic;
                case BaseTier.Legend: return Tier.Legend;
                default: throw new ArgumentOutOfRangeException(nameof(baseTier), baseTier, null);
            }
        }

        public static bool IsMax(this Tier tier) => tier == Max;

        public static Tier Next(this Tier tier)
        {
            if (tier.IsMax())
            {
                throw new InvalidOperationException("Legend+4 is the highest tier.");
            }
            return tier + 1;
        }
    }
}
