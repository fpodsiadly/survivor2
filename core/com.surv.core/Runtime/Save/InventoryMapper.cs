using System;
using System.Linq;
using Surv.Core.Equipment;

namespace Surv.Core.Save
{
    /// <summary>Copies equipment state between <see cref="Inventory"/> and <see cref="SaveData"/>.</summary>
    public static class InventoryMapper
    {
        public static void Write(Inventory inventory, SaveData save)
        {
            save.Gold = inventory.Gold;
            save.Items = inventory.Items
                .OrderBy(i => i.Uid)
                .Select(i => new SavedItem { Uid = i.Uid, TypeId = i.TypeId, Tier = i.Tier.ToString(), Level = i.Level })
                .ToList();
            save.Equipped = inventory.Equipped.ToDictionary(e => e.Key.ToString(), e => e.Value);
            save.Plans = Enum.GetValues(typeof(Slot)).Cast<Slot>()
                .Where(s => inventory.PlansFor(s) > 0)
                .ToDictionary(s => s.ToString(), s => inventory.PlansFor(s));
        }

        /// <summary>Items whose type no longer exists in the catalog are dropped.</summary>
        public static Inventory Read(SaveData save, ItemCatalog catalog)
        {
            var inventory = new Inventory(catalog);
            inventory.AddGold(save.Gold);
            foreach (var plan in save.Plans)
            {
                if (Enum.TryParse(plan.Key, out Slot slot)) inventory.AddPlans(slot, plan.Value);
            }
            foreach (var saved in save.Items)
            {
                if (!catalog.Contains(saved.TypeId) || !Enum.TryParse(saved.Tier, out Tier tier)) continue;
                inventory.Restore(new Item(saved.Uid, saved.TypeId, tier, saved.Level));
            }
            foreach (var equipped in save.Equipped)
            {
                if (inventory.TryGet(equipped.Value, out _)) inventory.Equip(equipped.Value);
            }
            return inventory;
        }
    }
}
