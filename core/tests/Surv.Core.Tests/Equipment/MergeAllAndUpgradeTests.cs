using System.Linq;
using NUnit.Framework;
using Surv.Core.Equipment;
using static Surv.Core.Tests.Equipment.EquipmentTestData;

namespace Surv.Core.Tests.Equipment
{
    public class MergeAllTests
    {
        [Test]
        public void Chains_from_normal_up_to_excellent()
        {
            var (inv, merge, _) = Setup();
            for (var i = 0; i < 27; i++) inv.Add(Kunai, Tier.Normal);

            new MergeAllService(merge).MergeAll(inv);

            Assert.That(inv.Items.Count(), Is.EqualTo(1));
            Assert.That(inv.Items.Single().Tier, Is.EqualTo(Tier.Excellent));
        }

        [Test]
        public void Never_merges_excellent_or_mixes_types()
        {
            var (inv, merge, _) = Setup();
            for (var i = 0; i < 3; i++) inv.Add(Kunai, Tier.Excellent);
            inv.Add(Kunai, Tier.Normal);
            inv.Add(Kunai, Tier.Normal);
            inv.Add(Blade, Tier.Normal);

            var merges = new MergeAllService(merge).MergeAll(inv);

            Assert.That(merges, Is.Empty);
            Assert.That(inv.Items.Count(), Is.EqualTo(6));
        }

        [Test]
        public void Keeps_equipped_item_as_main()
        {
            var (inv, merge, _) = Setup();
            var low = inv.Add(Kunai, Tier.Normal, level: 1);
            inv.Add(Kunai, Tier.Normal, level: 9);
            inv.Add(Kunai, Tier.Normal, level: 3);
            inv.Equip(low.Uid);

            new MergeAllService(merge).MergeAll(inv);

            Assert.That(inv.EquippedIn(Slot.Weapon).Uid, Is.EqualTo(low.Uid));
            Assert.That(inv.EquippedIn(Slot.Weapon).Tier, Is.EqualTo(Tier.Good));
        }

        [Test]
        public void Picks_highest_level_as_main_when_nothing_is_equipped()
        {
            var (inv, merge, _) = Setup();
            inv.Add(Kunai, Tier.Normal, level: 2);
            var high = inv.Add(Kunai, Tier.Normal, level: 8);
            inv.Add(Kunai, Tier.Normal, level: 4);

            new MergeAllService(merge).MergeAll(inv);

            var result = inv.Items.Single();
            Assert.That(result.Uid, Is.EqualTo(high.Uid));
            Assert.That(result.Level, Is.EqualTo(8));
        }
    }

    public class UpgradeTests
    {
        [TestCase(Tier.Normal, 10)]
        [TestCase(Tier.Good, 20)]
        [TestCase(Tier.Better, 30)]
        [TestCase(Tier.Excellent, 40)]
        [TestCase(Tier.Excellent1, 45)]
        [TestCase(Tier.Excellent2, 50)]
        [TestCase(Tier.Epic, 60)]
        [TestCase(Tier.Epic1, 65)]
        [TestCase(Tier.Epic2, 70)]
        [TestCase(Tier.Epic3, 75)]
        [TestCase(Tier.Legend, 80)]
        [TestCase(Tier.Legend1, 85)]
        [TestCase(Tier.Legend2, 90)]
        [TestCase(Tier.Legend3, 95)]
        [TestCase(Tier.Legend4, 100)]
        public void Upgrade_max_stops_at_the_tier_cap(Tier tier, int cap)
        {
            var (inv, _, upgrades) = Setup();
            var item = inv.Add(Kunai, tier);
            inv.AddGold(long.MaxValue / 2);
            inv.AddPlans(Slot.Weapon, long.MaxValue / 2);

            upgrades.UpgradeMax(inv, item.Uid);

            Assert.That(inv.Get(item.Uid).Level, Is.EqualTo(cap));
            Assert.That(upgrades.Upgrade(inv, item.Uid), Is.EqualTo(UpgradeError.AtLevelCap));
        }

        [Test]
        public void Upgrade_fails_without_resources_and_changes_nothing()
        {
            var (inv, _, upgrades) = Setup();
            var item = inv.Add(Kunai, Tier.Normal);

            Assert.That(upgrades.Upgrade(inv, item.Uid), Is.EqualTo(UpgradeError.NotEnoughResources));
            Assert.That(inv.Get(item.Uid).Level, Is.EqualTo(1));
        }

        [Test]
        public void Upgrade_max_spends_only_what_is_affordable()
        {
            var (inv, _, upgrades) = Setup();
            var item = inv.Add(Kunai, Tier.Better);
            var (gold, plans) = upgrades.InvestedUpTo(6);
            inv.AddGold(gold + 1);
            inv.AddPlans(Slot.Weapon, plans);

            Assert.That(upgrades.UpgradeMax(inv, item.Uid), Is.EqualTo(5));
            Assert.That(inv.Gold, Is.EqualTo(1));
        }

        [Test]
        public void Reset_refunds_everything_and_sets_level_one()
        {
            var (inv, _, upgrades) = Setup();
            var item = inv.Add(BeltA, Tier.Epic);
            var (gold, plans) = upgrades.InvestedUpTo(30);
            inv.AddGold(gold);
            inv.AddPlans(Slot.Belt, plans);
            upgrades.UpgradeMax(inv, item.Uid);
            Assert.That(inv.Get(item.Uid).Level, Is.EqualTo(30));
            Assert.That(inv.Gold, Is.Zero);

            upgrades.Reset(inv, item.Uid);

            Assert.That(inv.Get(item.Uid).Level, Is.EqualTo(1));
            Assert.That(inv.Gold, Is.EqualTo(gold));
            Assert.That(inv.PlansFor(Slot.Belt), Is.EqualTo(plans));
        }
    }

    public class ItemStatsTests
    {
        [TestCase(Tier.Normal, 10)]
        [TestCase(Tier.Excellent, 50)]
        [TestCase(Tier.Epic, 132)]
        [TestCase(Tier.Legend, 231)]
        [TestCase(Tier.Legend4, 323.4)]
        public void Weapon_base_attack_follows_the_tier_table(Tier tier, double expected)
        {
            var stats = new ItemStats(EquipmentBalance.CreateDefault(), Catalog());

            Assert.That(stats.MainStat(new Item(1, Kunai, tier, 1)), Is.EqualTo(expected).Within(1e-9));
        }

        [Test]
        public void Health_slot_uses_ten_times_the_scale_plus_per_level()
        {
            var balance = EquipmentBalance.CreateDefault();
            var stats = new ItemStats(balance, Catalog());

            var value = stats.MainStat(new Item(1, BeltA, Tier.Good, 3));

            Assert.That(stats.MainStatKind(new Item(1, BeltA, Tier.Good, 1)), Is.EqualTo(StatKind.Health));
            Assert.That(value, Is.EqualTo(20 * 10 + 2 * balance.HealthPerLevel));
        }

        [Test]
        public void Excellent_weapon_unlocks_the_start_at_level_two_bonus()
        {
            var stats = new ItemStats(EquipmentBalance.CreateDefault(), Catalog());

            var better = stats.UnlockedBonuses(new Item(1, Kunai, Tier.Better, 1)).Select(b => b.BonusId);
            var excellent = stats.UnlockedBonuses(new Item(1, Kunai, Tier.Excellent2, 1)).Select(b => b.BonusId);

            Assert.That(better, Does.Not.Contain(BonusStartLv2));
            Assert.That(excellent, Is.EquivalentTo(new[] { BonusAtk10, BonusSkillDmg30, BonusStartLv2 }));
        }

        [Test]
        public void Plus_marks_and_base_colour()
        {
            Assert.That(Tier.Epic2.GetBase(), Is.EqualTo(BaseTier.Epic));
            Assert.That(Tier.Epic2.PlusMarks(), Is.EqualTo(2));
            Assert.That(Tier.Legend.PlusMarks(), Is.Zero);
        }
    }
}
