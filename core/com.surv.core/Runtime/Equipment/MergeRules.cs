namespace Surv.Core.Equipment
{
    public enum IngredientMatch
    {
        /// <summary>Same item type as the main item ("identyczny").</summary>
        SameType,

        /// <summary>Any item type of the main item's slot ("dowolny").</summary>
        AnyTypeSameSlot,
    }

    /// <summary>What a main item of a given tier needs to merge into the next tier.</summary>
    public sealed class MergeRecipe
    {
        public MergeRecipe(int ingredientCount, Tier ingredientTier, IngredientMatch match)
        {
            IngredientCount = ingredientCount;
            IngredientTier = ingredientTier;
            Match = match;
        }

        public int IngredientCount { get; }
        public Tier IngredientTier { get; }
        public IngredientMatch Match { get; }
    }

    /// <summary>The merge table of SPEC W23 (RESEARCH §5.3). Ingredients are always on a base tier except Excellent+2.</summary>
    public static class MergeRules
    {
        /// <summary>Recipe for a main item of <paramref name="mainTier"/>, or null at Legend+4.</summary>
        public static MergeRecipe RecipeFor(Tier mainTier)
        {
            switch (mainTier)
            {
                case Tier.Normal:
                case Tier.Good:
                case Tier.Better:
                    return new MergeRecipe(2, mainTier, IngredientMatch.SameType);
                case Tier.Excellent:
                    return new MergeRecipe(1, Tier.Excellent, IngredientMatch.AnyTypeSameSlot);
                case Tier.Excellent1:
                    return new MergeRecipe(2, Tier.Excellent, IngredientMatch.AnyTypeSameSlot);
                case Tier.Excellent2:
                    return new MergeRecipe(1, Tier.Excellent2, IngredientMatch.SameType);
                case Tier.Epic:
                case Tier.Epic1:
                case Tier.Epic2:
                    return new MergeRecipe(1, Tier.Epic, IngredientMatch.AnyTypeSameSlot);
                case Tier.Epic3:
                    return new MergeRecipe(2, Tier.Epic, IngredientMatch.SameType);
                case Tier.Legend:
                case Tier.Legend1:
                case Tier.Legend2:
                case Tier.Legend3:
                    return new MergeRecipe(1, Tier.Legend, IngredientMatch.AnyTypeSameSlot);
                default:
                    return null;
            }
        }
    }
}
