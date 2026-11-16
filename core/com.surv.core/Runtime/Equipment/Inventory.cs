using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Equipment
{
    /// <summary>Owned items, the equipped item per slot, and the gold and plans used for upgrades.</summary>
    public sealed class Inventory
    {
        private readonly Dictionary<int, Item> _items = new Dictionary<int, Item>();
        private readonly Dictionary<Slot, int> _equipped = new Dictionary<Slot, int>();
        private readonly Dictionary<Slot, long> _plans = new Dictionary<Slot, long>();
        private readonly ItemCatalog _catalog;
        private int _nextUid = 1;

        public Inventory(ItemCatalog catalog)
        {
            _catalog = catalog ?? throw new ArgumentNullException(nameof(catalog));
        }

        public long Gold { get; private set; }

        public IEnumerable<Item> Items => _items.Values;

        public ItemCatalog Catalog => _catalog;

        public Item Add(string typeId, Tier tier, int level = 1)
        {
            _catalog.Get(typeId);
            var item = new Item(_nextUid++, typeId, tier, level);
            _items.Add(item.Uid, item);
            return item;
        }

        /// <summary>Restores an item with a known uid (used by the save loader).</summary>
        public void Restore(Item item)
        {
            _catalog.Get(item.TypeId);
            if (_items.ContainsKey(item.Uid)) throw new ArgumentException($"Duplicate uid {item.Uid}.");
            _items.Add(item.Uid, item);
            _nextUid = Math.Max(_nextUid, item.Uid + 1);
        }

        public bool TryGet(int uid, out Item item) => _items.TryGetValue(uid, out item);

        public Item Get(int uid)
        {
            if (!_items.TryGetValue(uid, out var item)) throw new KeyNotFoundException($"No item with uid {uid}.");
            return item;
        }

        public ItemType TypeOf(Item item) => _catalog.Get(item.TypeId);

        public Slot SlotOf(Item item) => TypeOf(item).Slot;

        public void Equip(int uid)
        {
            var item = Get(uid);
            _equipped[SlotOf(item)] = uid;
        }

        public void Unequip(Slot slot) => _equipped.Remove(slot);

        public bool IsEquipped(int uid) => _equipped.ContainsValue(uid);

        public Item EquippedIn(Slot slot) => _equipped.TryGetValue(slot, out var uid) ? _items[uid] : null;

        public IReadOnlyDictionary<Slot, int> Equipped => _equipped;

        internal void Replace(Item item)
        {
            if (!_items.ContainsKey(item.Uid)) throw new KeyNotFoundException($"No item with uid {item.Uid}.");
            _items[item.Uid] = item;
        }

        internal void Remove(int uid)
        {
            if (IsEquipped(uid)) throw new InvalidOperationException($"Item {uid} is equipped and cannot be removed.");
            _items.Remove(uid);
        }

        public long PlansFor(Slot slot) => _plans.TryGetValue(slot, out var amount) ? amount : 0;

        public void AddGold(long amount)
        {
            if (amount < 0) throw new ArgumentOutOfRangeException(nameof(amount));
            Gold += amount;
        }

        public void AddPlans(Slot slot, long amount)
        {
            if (amount < 0) throw new ArgumentOutOfRangeException(nameof(amount));
            _plans[slot] = PlansFor(slot) + amount;
        }

        public bool CanAfford(long gold, Slot slot, long plans) => Gold >= gold && PlansFor(slot) >= plans;

        /// <summary>Spends gold if there is enough (talents, SPEC W30).</summary>
        public bool TrySpendGold(long amount)
        {
            if (amount < 0) throw new ArgumentOutOfRangeException(nameof(amount));
            if (Gold < amount) return false;
            Gold -= amount;
            return true;
        }

        internal void Spend(long gold, Slot slot, long plans)
        {
            if (!CanAfford(gold, slot, plans)) throw new InvalidOperationException("Not enough gold or plans.");
            Gold -= gold;
            _plans[slot] = PlansFor(slot) - plans;
        }

        public IEnumerable<Item> ItemsInSlot(Slot slot) => _items.Values.Where(i => SlotOf(i) == slot);
    }
}
