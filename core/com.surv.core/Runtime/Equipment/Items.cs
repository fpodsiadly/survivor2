using System;
using System.Collections.Generic;

namespace Surv.Core.Equipment
{
    /// <summary>Six equipment slots; weapon, necklace and gloves give ATK, the rest give HP (SPEC W22).</summary>
    public enum Slot
    {
        Weapon,
        Necklace,
        Gloves,
        Armor,
        Belt,
        Boots,
    }

    public enum StatKind
    {
        Attack,
        Health,
    }

    public static class SlotExtensions
    {
        public static StatKind MainStat(this Slot slot) =>
            slot == Slot.Weapon || slot == Slot.Necklace || slot == Slot.Gloves ? StatKind.Attack : StatKind.Health;
    }

    /// <summary>A bonus of an item type that starts working from <see cref="MinTier"/> (SPEC W24).</summary>
    public sealed class TierBonus
    {
        public TierBonus(Tier minTier, string bonusId)
        {
            if (string.IsNullOrEmpty(bonusId)) throw new ArgumentException("Bonus id is required.", nameof(bonusId));
            MinTier = minTier;
            BonusId = bonusId;
        }

        public Tier MinTier { get; }
        public string BonusId { get; }
    }

    /// <summary>Definition of an item type, e.g. one weapon or one belt design (SPEC W22).</summary>
    public sealed class ItemType
    {
        public ItemType(string id, Slot slot, IReadOnlyList<TierBonus> bonuses, string weaponSkillId = null)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Item type id is required.", nameof(id));
            if (slot == Slot.Weapon && string.IsNullOrEmpty(weaponSkillId))
            {
                throw new ArgumentException("A weapon type must name the skill it puts in active slot 1.", nameof(weaponSkillId));
            }
            Id = id;
            Slot = slot;
            Bonuses = bonuses ?? Array.Empty<TierBonus>();
            WeaponSkillId = weaponSkillId;
        }

        public string Id { get; }
        public Slot Slot { get; }
        public IReadOnlyList<TierBonus> Bonuses { get; }

        /// <summary>Skill that a weapon of this type grants in active slot 1; null for other slots.</summary>
        public string WeaponSkillId { get; }
    }

    /// <summary>An owned item. Immutable: merges and upgrades replace it in the <see cref="Inventory"/>.</summary>
    public sealed class Item
    {
        public Item(int uid, string typeId, Tier tier, int level)
        {
            if (uid <= 0) throw new ArgumentOutOfRangeException(nameof(uid), "Uid must be positive.");
            if (string.IsNullOrEmpty(typeId)) throw new ArgumentException("Type id is required.", nameof(typeId));
            if (level < 1) throw new ArgumentOutOfRangeException(nameof(level), "Level starts at 1.");
            Uid = uid;
            TypeId = typeId;
            Tier = tier;
            Level = level;
        }

        public int Uid { get; }
        public string TypeId { get; }
        public Tier Tier { get; }
        public int Level { get; }

        public Item WithTier(Tier tier) => new Item(Uid, TypeId, tier, Level);
        public Item WithLevel(int level) => new Item(Uid, TypeId, Tier, level);

        public override string ToString() => $"#{Uid} {TypeId} {Tier} L{Level}";
    }

    /// <summary>Lookup of item types by id.</summary>
    public sealed class ItemCatalog
    {
        private readonly Dictionary<string, ItemType> _types = new Dictionary<string, ItemType>();

        public ItemCatalog(IEnumerable<ItemType> types)
        {
            foreach (var type in types)
            {
                if (_types.ContainsKey(type.Id)) throw new ArgumentException($"Duplicate item type id '{type.Id}'.");
                _types.Add(type.Id, type);
            }
        }

        public ItemType Get(string typeId)
        {
            if (!_types.TryGetValue(typeId, out var type)) throw new KeyNotFoundException($"Unknown item type '{typeId}'.");
            return type;
        }

        public bool Contains(string typeId) => _types.ContainsKey(typeId);

        public IEnumerable<ItemType> All => _types.Values;
    }
}
