using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Equipment
{
    /// <summary>Main stat and unlocked bonuses of an item (SPEC W24).</summary>
    public sealed class ItemStats
    {
        private readonly EquipmentBalance _balance;
        private readonly ItemCatalog _catalog;

        public ItemStats(EquipmentBalance balance, ItemCatalog catalog)
        {
            _balance = balance ?? throw new ArgumentNullException(nameof(balance));
            _catalog = catalog ?? throw new ArgumentNullException(nameof(catalog));
        }

        public StatKind MainStatKind(Item item) => _catalog.Get(item.TypeId).Slot.MainStat();

        /// <summary>Tier base (ATK scale, ×multiplier for HP slots) plus a fixed amount per level above 1.</summary>
        public double MainStat(Item item)
        {
            var kind = MainStatKind(item);
            var tierBase = _balance.WeaponBaseStat[(int)item.Tier];
            var levels = item.Level - 1;
            return kind == StatKind.Attack
                ? tierBase + levels * _balance.AttackPerLevel
                : tierBase * _balance.HealthSlotMultiplier + levels * _balance.HealthPerLevel;
        }

        /// <summary>Bonuses of the item's type whose minimum tier is reached.</summary>
        public IReadOnlyList<TierBonus> UnlockedBonuses(Item item) =>
            _catalog.Get(item.TypeId).Bonuses.Where(b => item.Tier >= b.MinTier).ToList();

        /// <summary>All bonuses with a flag telling whether each one already works (item card shows locked ones greyed out).</summary>
        public IReadOnlyList<(TierBonus bonus, bool unlocked)> BonusesWithState(Item item) =>
            _catalog.Get(item.TypeId).Bonuses.Select(b => (b, item.Tier >= b.MinTier)).ToList();
    }
}
