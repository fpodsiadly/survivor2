using Surv.Core.Equipment;

namespace Surv.Core.Tests.Equipment
{
    internal static class EquipmentTestData
    {
        public const string Kunai = "weapon.thrown";
        public const string Blade = "weapon.blade";
        public const string BeltA = "belt.a";
        public const string BeltB = "belt.b";

        public const string BonusAtk10 = "atk+10%";
        public const string BonusSkillDmg30 = "weaponSkillDamage+30%";
        public const string BonusStartLv2 = "weaponSkillStartsAtLv2";
        public const string BonusAtk15 = "atk+15%";
        public const string BonusLegend = "weaponLegendBehaviour";

        public static ItemCatalog Catalog() => new ItemCatalog(new[]
        {
            Weapon(Kunai, "skill.thrown"),
            Weapon(Blade, "skill.blade"),
            new ItemType(BeltA, Slot.Belt, new[] { new TierBonus(Tier.Better, "belt.speedAfterMeat") }),
            new ItemType(BeltB, Slot.Belt, new TierBonus[0]),
        });

        /// <summary>Weapon bonus skeleton from SPEC W24.</summary>
        private static ItemType Weapon(string id, string skill) => new ItemType(id, Slot.Weapon, new[]
        {
            new TierBonus(Tier.Good, BonusAtk10),
            new TierBonus(Tier.Better, BonusSkillDmg30),
            new TierBonus(Tier.Excellent, BonusStartLv2),
            new TierBonus(Tier.Epic, BonusAtk15),
            new TierBonus(Tier.Legend, BonusLegend),
        }, skill);

        public static (Inventory inventory, MergeService merge, UpgradeService upgrades) Setup()
        {
            var balance = EquipmentBalance.CreateDefault();
            var upgrades = new UpgradeService(balance);
            return (new Inventory(Catalog()), new MergeService(upgrades), upgrades);
        }
    }
}
