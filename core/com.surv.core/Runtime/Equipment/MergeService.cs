using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Equipment
{
    public enum MergeError
    {
        None,
        MainNotFound,
        MainAtMaxTier,
        WrongIngredientCount,
        IngredientNotFound,
        IngredientIsMain,
        DuplicateIngredient,
        IngredientEquipped,
        IngredientWrongSlot,
        IngredientWrongType,
        IngredientWrongTier,
    }

    public sealed class MergeOutcome
    {
        private MergeOutcome(MergeError error, Item result, long refundedGold, long refundedPlans)
        {
            Error = error;
            Result = result;
            RefundedGold = refundedGold;
            RefundedPlans = refundedPlans;
        }

        public MergeError Error { get; }
        public bool Succeeded => Error == MergeError.None;
        public Item Result { get; }
        public long RefundedGold { get; }
        public long RefundedPlans { get; }

        internal static MergeOutcome Fail(MergeError error) => new MergeOutcome(error, null, 0, 0);
        internal static MergeOutcome Success(Item result, long gold, long plans) => new MergeOutcome(MergeError.None, result, gold, plans);
    }

    /// <summary>
    /// Merges per SPEC W23: the main item keeps its uid, type, level and equipped state and moves one tier up;
    /// ingredients are consumed and the gold and plans spent on upgrading them are refunded.
    /// </summary>
    public sealed class MergeService
    {
        private readonly UpgradeService _upgrades;

        public MergeService(UpgradeService upgrades)
        {
            _upgrades = upgrades ?? throw new ArgumentNullException(nameof(upgrades));
        }

        public MergeError Validate(Inventory inventory, int mainUid, IReadOnlyList<int> ingredientUids)
        {
            if (!inventory.TryGet(mainUid, out var main)) return MergeError.MainNotFound;
            var recipe = MergeRules.RecipeFor(main.Tier);
            if (recipe == null) return MergeError.MainAtMaxTier;
            if (ingredientUids == null || ingredientUids.Count != recipe.IngredientCount) return MergeError.WrongIngredientCount;
            if (ingredientUids.Distinct().Count() != ingredientUids.Count) return MergeError.DuplicateIngredient;

            foreach (var uid in ingredientUids)
            {
                var error = CheckIngredient(inventory, main, recipe, uid);
                if (error != MergeError.None) return error;
            }
            return MergeError.None;
        }

        public MergeOutcome Merge(Inventory inventory, int mainUid, IReadOnlyList<int> ingredientUids)
        {
            var error = Validate(inventory, mainUid, ingredientUids);
            if (error != MergeError.None) return MergeOutcome.Fail(error);

            var main = inventory.Get(mainUid);
            var slot = inventory.SlotOf(main);
            long gold = 0, plans = 0;
            foreach (var uid in ingredientUids)
            {
                var (g, p) = _upgrades.InvestedUpTo(inventory.Get(uid).Level);
                gold += g;
                plans += p;
                inventory.Remove(uid);
            }
            inventory.AddGold(gold);
            inventory.AddPlans(slot, plans);

            var result = main.WithTier(main.Tier.Next());
            inventory.Replace(result);
            return MergeOutcome.Success(result, gold, plans);
        }

        /// <summary>Items that may be used as ingredients for <paramref name="mainUid"/> (for highlighting in the UI).</summary>
        public IReadOnlyList<Item> Candidates(Inventory inventory, int mainUid)
        {
            if (!inventory.TryGet(mainUid, out var main)) return Array.Empty<Item>();
            var recipe = MergeRules.RecipeFor(main.Tier);
            if (recipe == null) return Array.Empty<Item>();
            return inventory.Items
                .Where(i => CheckIngredient(inventory, main, recipe, i.Uid) == MergeError.None)
                .OrderBy(i => i.Level)
                .ThenBy(i => i.Uid)
                .ToList();
        }

        private static MergeError CheckIngredient(Inventory inventory, Item main, MergeRecipe recipe, int uid)
        {
            if (uid == main.Uid) return MergeError.IngredientIsMain;
            if (!inventory.TryGet(uid, out var ingredient)) return MergeError.IngredientNotFound;
            if (inventory.IsEquipped(uid)) return MergeError.IngredientEquipped;
            if (inventory.SlotOf(ingredient) != inventory.SlotOf(main)) return MergeError.IngredientWrongSlot;
            if (recipe.Match == IngredientMatch.SameType && ingredient.TypeId != main.TypeId) return MergeError.IngredientWrongType;
            if (ingredient.Tier != recipe.IngredientTier) return MergeError.IngredientWrongTier;
            return MergeError.None;
        }
    }
}
