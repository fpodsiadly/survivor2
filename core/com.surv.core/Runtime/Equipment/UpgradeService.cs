using System;

namespace Surv.Core.Equipment
{
    public enum UpgradeError
    {
        None,
        ItemNotFound,
        AtLevelCap,
        NotEnoughResources,
    }

    /// <summary>Item levelling, "upgrade max" and reset (SPEC W25).</summary>
    public sealed class UpgradeService
    {
        private readonly EquipmentBalance _balance;

        public UpgradeService(EquipmentBalance balance)
        {
            _balance = balance ?? throw new ArgumentNullException(nameof(balance));
        }

        /// <summary>Gold and plans spent to bring an item from level 1 to <paramref name="level"/>.</summary>
        public (long gold, long plans) InvestedUpTo(int level)
        {
            long gold = 0, plans = 0;
            for (var l = 1; l < level; l++)
            {
                gold += _balance.GoldCostForLevel(l);
                plans += _balance.PlanCostForLevel(l);
            }
            return (gold, plans);
        }

        public UpgradeError Upgrade(Inventory inventory, int uid)
        {
            if (!inventory.TryGet(uid, out var item)) return UpgradeError.ItemNotFound;
            if (item.Level >= _balance.LevelCap(item.Tier)) return UpgradeError.AtLevelCap;

            var slot = inventory.SlotOf(item);
            var gold = _balance.GoldCostForLevel(item.Level);
            var plans = _balance.PlanCostForLevel(item.Level);
            if (!inventory.CanAfford(gold, slot, plans)) return UpgradeError.NotEnoughResources;

            inventory.Spend(gold, slot, plans);
            inventory.Replace(item.WithLevel(item.Level + 1));
            return UpgradeError.None;
        }

        /// <summary>Upgrades while affordable and below the cap; returns the number of levels gained.</summary>
        public int UpgradeMax(Inventory inventory, int uid)
        {
            var gained = 0;
            while (Upgrade(inventory, uid) == UpgradeError.None) gained++;
            return gained;
        }

        /// <summary>Sets the item back to level 1 and refunds 100% of the gold and plans spent on it.</summary>
        public bool Reset(Inventory inventory, int uid)
        {
            if (!inventory.TryGet(uid, out var item)) return false;
            var (gold, plans) = InvestedUpTo(item.Level);
            inventory.AddGold(gold);
            inventory.AddPlans(inventory.SlotOf(item), plans);
            inventory.Replace(item.WithLevel(1));
            return true;
        }
    }
}
