using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Equipment
{
    /// <summary>
    /// "Połącz wszystko" (SPEC W23): merges 3 identical items on Normal, Good and Better, chained until nothing
    /// is left. Main item: the equipped one if it is in the group, otherwise the highest level, then the lowest uid
    /// (PLAN P20). Ingredients are the lowest-level remaining items, so the smallest refunds are paid.
    /// </summary>
    public sealed class MergeAllService
    {
        private static readonly Tier[] AutoTiers = { Tier.Normal, Tier.Good, Tier.Better };
        private readonly MergeService _merge;

        public MergeAllService(MergeService merge)
        {
            _merge = merge ?? throw new ArgumentNullException(nameof(merge));
        }

        /// <summary>Returns the merges done, in order.</summary>
        public IReadOnlyList<MergeOutcome> MergeAll(Inventory inventory)
        {
            var done = new List<MergeOutcome>();
            bool mergedInPass;
            do
            {
                mergedInPass = false;
                foreach (var tier in AutoTiers)
                {
                    var groups = inventory.Items
                        .Where(i => i.Tier == tier)
                        .GroupBy(i => i.TypeId)
                        .Select(g => g.ToList())
                        .Where(g => g.Count >= 3)
                        .ToList();

                    foreach (var group in groups)
                    {
                        while (group.Count >= 3)
                        {
                            var main = group
                                .OrderByDescending(i => inventory.IsEquipped(i.Uid))
                                .ThenByDescending(i => i.Level)
                                .ThenBy(i => i.Uid)
                                .First();
                            var ingredients = group
                                .Where(i => i.Uid != main.Uid)
                                .OrderBy(i => i.Level)
                                .ThenBy(i => i.Uid)
                                .Take(2)
                                .Select(i => i.Uid)
                                .ToList();

                            var outcome = _merge.Merge(inventory, main.Uid, ingredients);
                            if (!outcome.Succeeded)
                            {
                                throw new InvalidOperationException($"Merge all produced an invalid merge: {outcome.Error}.");
                            }
                            done.Add(outcome);
                            mergedInPass = true;
                            group.RemoveAll(i => i.Uid == main.Uid || ingredients.Contains(i.Uid));
                        }
                    }
                }
            } while (mergedInPass);
            return done;
        }
    }
}
