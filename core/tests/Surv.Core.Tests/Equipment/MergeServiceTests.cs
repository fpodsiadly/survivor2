using System.Linq;
using NUnit.Framework;
using Surv.Core.Equipment;
using static Surv.Core.Tests.Equipment.EquipmentTestData;

namespace Surv.Core.Tests.Equipment
{
    /// <summary>Every row of the SPEC W23 merge table, with valid and invalid ingredients.</summary>
    public class MergeServiceTests
    {
        // Rows "3 identical" for Normal, Good, Better.
        [TestCase(Tier.Normal, Tier.Good)]
        [TestCase(Tier.Good, Tier.Better)]
        [TestCase(Tier.Better, Tier.Excellent)]
        public void Three_identical_items_merge_into_next_tier(Tier tier, Tier expected)
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, tier);
            var a = inv.Add(Kunai, tier);
            var b = inv.Add(Kunai, tier);

            var outcome = merge.Merge(inv, main.Uid, new[] { a.Uid, b.Uid });

            Assert.That(outcome.Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(expected));
            Assert.That(inv.Items.Count(), Is.EqualTo(1));
        }

        [TestCase(Tier.Normal)]
        [TestCase(Tier.Good)]
        [TestCase(Tier.Better)]
        public void Low_tiers_reject_other_type_of_same_slot(Tier tier)
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, tier);
            var a = inv.Add(Kunai, tier);
            var other = inv.Add(Blade, tier);

            Assert.That(merge.Validate(inv, main.Uid, new[] { a.Uid, other.Uid }), Is.EqualTo(MergeError.IngredientWrongType));
        }

        [Test]
        public void Excellent_plus_one_takes_any_excellent_of_the_slot()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent);
            var other = inv.Add(Blade, Tier.Excellent);

            Assert.That(merge.Merge(inv, main.Uid, new[] { other.Uid }).Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(Tier.Excellent1));
        }

        [Test]
        public void Excellent_plus_one_rejects_excellent_of_another_slot()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent);
            var belt = inv.Add(BeltA, Tier.Excellent);

            Assert.That(merge.Validate(inv, main.Uid, new[] { belt.Uid }), Is.EqualTo(MergeError.IngredientWrongSlot));
        }

        [Test]
        public void Excellent_plus_one_rejects_a_plus_tier_ingredient()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent);
            var plus = inv.Add(Blade, Tier.Excellent1);

            Assert.That(merge.Validate(inv, main.Uid, new[] { plus.Uid }), Is.EqualTo(MergeError.IngredientWrongTier));
        }

        [Test]
        public void Excellent_plus_two_takes_two_any_excellent()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent1);
            var a = inv.Add(Blade, Tier.Excellent);
            var b = inv.Add(Kunai, Tier.Excellent);

            Assert.That(merge.Validate(inv, main.Uid, new[] { a.Uid }), Is.EqualTo(MergeError.WrongIngredientCount));
            Assert.That(merge.Merge(inv, main.Uid, new[] { a.Uid, b.Uid }).Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(Tier.Excellent2));
        }

        [Test]
        public void Epic_needs_an_identical_excellent_plus_two()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent2);
            var otherType = inv.Add(Blade, Tier.Excellent2);
            var plainExcellent = inv.Add(Kunai, Tier.Excellent);
            var twin = inv.Add(Kunai, Tier.Excellent2);

            Assert.That(merge.Validate(inv, main.Uid, new[] { otherType.Uid }), Is.EqualTo(MergeError.IngredientWrongType));
            Assert.That(merge.Validate(inv, main.Uid, new[] { plainExcellent.Uid }), Is.EqualTo(MergeError.IngredientWrongTier));
            Assert.That(merge.Merge(inv, main.Uid, new[] { twin.Uid }).Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(Tier.Epic));
        }

        [TestCase(Tier.Epic, Tier.Epic1)]
        [TestCase(Tier.Epic1, Tier.Epic2)]
        [TestCase(Tier.Epic2, Tier.Epic3)]
        public void Epic_plus_steps_take_one_any_plain_epic(Tier tier, Tier expected)
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, tier);
            var other = inv.Add(Blade, Tier.Epic);
            var plusEpic = inv.Add(Blade, Tier.Epic1);

            Assert.That(merge.Validate(inv, main.Uid, new[] { plusEpic.Uid }), Is.EqualTo(MergeError.IngredientWrongTier));
            Assert.That(merge.Merge(inv, main.Uid, new[] { other.Uid }).Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(expected));
        }

        [Test]
        public void Legend_needs_two_identical_plain_epics()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Epic3);
            var twin1 = inv.Add(Kunai, Tier.Epic);
            var twin2 = inv.Add(Kunai, Tier.Epic);
            var otherType = inv.Add(Blade, Tier.Epic);

            Assert.That(merge.Validate(inv, main.Uid, new[] { twin1.Uid, otherType.Uid }), Is.EqualTo(MergeError.IngredientWrongType));
            Assert.That(merge.Merge(inv, main.Uid, new[] { twin1.Uid, twin2.Uid }).Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(Tier.Legend));
        }

        [TestCase(Tier.Legend, Tier.Legend1)]
        [TestCase(Tier.Legend1, Tier.Legend2)]
        [TestCase(Tier.Legend2, Tier.Legend3)]
        [TestCase(Tier.Legend3, Tier.Legend4)]
        public void Legend_plus_steps_take_one_any_plain_legend(Tier tier, Tier expected)
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, tier);
            var other = inv.Add(Blade, Tier.Legend);

            Assert.That(merge.Merge(inv, main.Uid, new[] { other.Uid }).Succeeded, Is.True);
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(expected));
        }

        [Test]
        public void Legend_plus_four_is_the_top()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Legend4);
            var other = inv.Add(Blade, Tier.Legend);

            Assert.That(merge.Validate(inv, main.Uid, new[] { other.Uid }), Is.EqualTo(MergeError.MainAtMaxTier));
        }

        [Test]
        public void Equipped_item_cannot_be_an_ingredient_but_can_be_main()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Normal);
            var equipped = inv.Add(Kunai, Tier.Normal);
            var c = inv.Add(Kunai, Tier.Normal);
            inv.Equip(equipped.Uid);

            Assert.That(merge.Validate(inv, main.Uid, new[] { equipped.Uid, c.Uid }), Is.EqualTo(MergeError.IngredientEquipped));
            Assert.That(merge.Merge(inv, equipped.Uid, new[] { main.Uid, c.Uid }).Succeeded, Is.True);
            Assert.That(inv.EquippedIn(Slot.Weapon).Uid, Is.EqualTo(equipped.Uid));
            Assert.That(inv.EquippedIn(Slot.Weapon).Tier, Is.EqualTo(Tier.Good));
        }

        [Test]
        public void Rejects_main_as_ingredient_and_duplicates()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Normal);
            var a = inv.Add(Kunai, Tier.Normal);

            Assert.That(merge.Validate(inv, main.Uid, new[] { main.Uid, a.Uid }), Is.EqualTo(MergeError.IngredientIsMain));
            Assert.That(merge.Validate(inv, main.Uid, new[] { a.Uid, a.Uid }), Is.EqualTo(MergeError.DuplicateIngredient));
            Assert.That(merge.Validate(inv, 999, new[] { a.Uid }), Is.EqualTo(MergeError.MainNotFound));
        }

        [Test]
        public void Failed_merge_changes_nothing()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Normal);
            var a = inv.Add(Kunai, Tier.Normal);
            var other = inv.Add(Blade, Tier.Normal);

            var outcome = merge.Merge(inv, main.Uid, new[] { a.Uid, other.Uid });

            Assert.That(outcome.Succeeded, Is.False);
            Assert.That(inv.Items.Count(), Is.EqualTo(3));
            Assert.That(inv.Get(main.Uid).Tier, Is.EqualTo(Tier.Normal));
        }

        [Test]
        public void Result_keeps_main_level_and_refunds_ingredient_upgrades_only()
        {
            var (inv, merge, upgrades) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent, level: 7);
            var ingredient = inv.Add(Blade, Tier.Excellent, level: 5);
            var (gold, plans) = upgrades.InvestedUpTo(5);

            var outcome = merge.Merge(inv, main.Uid, new[] { ingredient.Uid });

            Assert.That(inv.Get(main.Uid).Level, Is.EqualTo(7));
            Assert.That(outcome.RefundedGold, Is.EqualTo(gold));
            Assert.That(inv.Gold, Is.EqualTo(gold));
            Assert.That(inv.PlansFor(Slot.Weapon), Is.EqualTo(plans));
            Assert.That(gold, Is.GreaterThan(0));
        }

        [Test]
        public void Candidates_lists_only_valid_ingredients()
        {
            var (inv, merge, _) = Setup();
            var main = inv.Add(Kunai, Tier.Excellent);
            var ok1 = inv.Add(Blade, Tier.Excellent);
            var ok2 = inv.Add(Kunai, Tier.Excellent);
            inv.Add(Blade, Tier.Excellent1);
            inv.Add(BeltA, Tier.Excellent);

            var candidates = merge.Candidates(inv, main.Uid).Select(i => i.Uid);

            Assert.That(candidates, Is.EquivalentTo(new[] { ok1.Uid, ok2.Uid }));
        }
    }
}
