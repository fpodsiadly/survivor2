using System.Collections.Generic;
using System.Linq;
using NUnit.Framework;
using Surv.Core.Common;
using Surv.Core.Run;

namespace Surv.Core.Tests.Run
{
    internal static class SkillData
    {
        public const string Weapon = "w.thrown";
        public const string Mines = "a.mines";
        public const string Fire = "a.fire";
        public const string Lightning = "a.lightning";
        public const string Boomerang = "a.boomerang";
        public const string Magnet = "p.magnet";
        public const string Xp = "p.xp";

        /// <summary>8 actives, 7 passives: enough to fill all 12 slots and test leftovers.</summary>
        public static SkillCatalog Catalog()
        {
            var skills = new List<SkillDefinition>
            {
                new SkillDefinition(Weapon, SkillKind.Active, isWeaponSkill: true),
                new SkillDefinition("w.blade", SkillKind.Active, isWeaponSkill: true),
                new SkillDefinition(Mines, SkillKind.Active),
                new SkillDefinition(Fire, SkillKind.Active),
                new SkillDefinition(Lightning, SkillKind.Active),
                new SkillDefinition(Boomerang, SkillKind.Active),
                new SkillDefinition("a.orbit", SkillKind.Active),
                new SkillDefinition("a.aura", SkillKind.Active),
                new SkillDefinition(Magnet, SkillKind.Passive),
                new SkillDefinition(Xp, SkillKind.Passive),
            };
            for (var i = 1; i <= 5; i++) skills.Add(new SkillDefinition($"p.extra{i}", SkillKind.Passive));

            return new SkillCatalog(skills, new[]
            {
                new EvolutionDefinition("evo.boomerang", Boomerang, Magnet),
                new EvolutionDefinition("evo.thrown", Weapon, Xp),
                new EvolutionDefinition("evo.mines.fire", Mines, Fire),
                new EvolutionDefinition("evo.mines.lightning", Mines, Lightning),
            });
        }

        public static Loadout NewLoadout() => new Loadout(Catalog(), Weapon);

        public static void Max(Loadout loadout, string skillId)
        {
            if (!loadout.Owns(skillId)) CardDraw.Apply(loadout, Card.NewSkill(skillId));
            while (loadout.Find(skillId).Level < SkillCatalog.MaxLevel) CardDraw.Apply(loadout, Card.LevelUp(skillId, 0));
        }
    }

    public class LoadoutAndCardTests
    {
        [Test]
        public void Weapon_skill_sits_in_slot_one_and_excellent_starts_it_at_level_two()
        {
            var loadout = new Loadout(SkillData.Catalog(), SkillData.Weapon, weaponSkillLevel: 2);

            Assert.That(loadout.Actives[0].SkillId, Is.EqualTo(SkillData.Weapon));
            Assert.That(loadout.Actives[0].Level, Is.EqualTo(2));
        }

        [Test]
        public void Offer_has_three_different_cards_and_never_other_weapon_skills()
        {
            for (var seed = 0; seed < 200; seed++)
            {
                var cards = new CardDraw(new SeededRandom(seed), 50).Offer(SkillData.NewLoadout());
                Assert.That(cards.Count, Is.EqualTo(3));
                Assert.That(cards.Select(c => c.Id).Distinct().Count(), Is.EqualTo(3));
                Assert.That(cards.Any(c => c.Id == "w.blade"), Is.False);
            }
        }

        [Test]
        public void Equipped_weapon_skill_is_offered_as_a_level_up()
        {
            var seen = false;
            for (var seed = 0; seed < 200 && !seen; seed++)
            {
                seen = new CardDraw(new SeededRandom(seed), 50).Offer(SkillData.NewLoadout())
                    .Any(c => c.Kind == CardKind.LevelUp && c.Id == SkillData.Weapon && c.LevelAfter == 2);
            }
            Assert.That(seen, Is.True);
        }

        [Test]
        public void Six_active_slots_limit_new_active_cards()
        {
            var loadout = SkillData.NewLoadout();
            foreach (var id in new[] { SkillData.Mines, SkillData.Fire, SkillData.Lightning, SkillData.Boomerang, "a.orbit" })
            {
                CardDraw.Apply(loadout, Card.NewSkill(id));
            }

            Assert.That(loadout.Actives.Count, Is.EqualTo(6));
            for (var seed = 0; seed < 200; seed++)
            {
                var cards = new CardDraw(new SeededRandom(seed), 50).Offer(loadout);
                Assert.That(cards.Any(c => c.Kind == CardKind.NewSkill && c.Id == "a.aura"), Is.False);
            }
        }

        [Test]
        public void Eligible_evolution_is_always_offered()
        {
            var loadout = SkillData.NewLoadout();
            SkillData.Max(loadout, SkillData.Boomerang);
            CardDraw.Apply(loadout, Card.NewSkill(SkillData.Magnet));

            for (var seed = 0; seed < 100; seed++)
            {
                var cards = new CardDraw(new SeededRandom(seed), 50).Offer(loadout);
                Assert.That(cards.Any(c => c.Kind == CardKind.Evolve && c.Id == "evo.boomerang"), Is.True);
            }
        }

        [Test]
        public void Evolution_needs_max_level_base_but_any_level_passive()
        {
            var loadout = SkillData.NewLoadout();
            CardDraw.Apply(loadout, Card.NewSkill(SkillData.Boomerang));
            CardDraw.Apply(loadout, Card.NewSkill(SkillData.Magnet));
            var evolution = loadout.Catalog.GetEvolution("evo.boomerang");

            Assert.That(loadout.CanEvolve(evolution), Is.False);
            SkillData.Max(loadout, SkillData.Boomerang);
            Assert.That(loadout.Find(SkillData.Magnet).Level, Is.EqualTo(1));
            Assert.That(loadout.CanEvolve(evolution), Is.True);

            CardDraw.Apply(loadout, Card.Evolve(evolution.Id));
            Assert.That(loadout.Find(SkillData.Boomerang).EvolutionId, Is.EqualTo("evo.boomerang"));
            Assert.That(loadout.Owns(SkillData.Magnet), Is.True, "passive partner stays");
            Assert.That(loadout.CanEvolve(evolution), Is.False);
        }

        [Test]
        public void Active_active_evolution_needs_both_at_max_and_frees_the_partner_slot()
        {
            var loadout = SkillData.NewLoadout();
            SkillData.Max(loadout, SkillData.Mines);
            CardDraw.Apply(loadout, Card.NewSkill(SkillData.Fire));
            var evolution = loadout.Catalog.GetEvolution("evo.mines.fire");
            Assert.That(loadout.CanEvolve(evolution), Is.False);

            SkillData.Max(loadout, SkillData.Fire);
            CardDraw.Apply(loadout, Card.Evolve(evolution.Id));

            Assert.That(loadout.Owns(SkillData.Fire), Is.False);
            Assert.That(loadout.Actives.Count, Is.EqualTo(2));
        }

        [Test]
        public void Two_possible_combination_evolutions_are_both_offered()
        {
            var loadout = SkillData.NewLoadout();
            SkillData.Max(loadout, SkillData.Mines);
            SkillData.Max(loadout, SkillData.Fire);
            SkillData.Max(loadout, SkillData.Lightning);

            var cards = new CardDraw(new SeededRandom(3), 50).Offer(loadout);

            Assert.That(cards.Where(c => c.Kind == CardKind.Evolve).Select(c => c.Id),
                Is.EquivalentTo(new[] { "evo.mines.fire", "evo.mines.lightning" }));
        }

        [Test]
        public void Fewer_than_three_available_shows_only_those_and_nothing_gives_gold()
        {
            var loadout = SkillData.NewLoadout();
            // No mines, so no combination evolution can free an active slot.
            foreach (var id in new[] { SkillData.Fire, SkillData.Lightning, SkillData.Boomerang, "a.orbit", "a.aura" }) SkillData.Max(loadout, id);
            foreach (var id in new[] { SkillData.Magnet, "p.extra1", "p.extra2", "p.extra3", "p.extra4", "p.extra5" }) SkillData.Max(loadout, id);
            while (loadout.Find(SkillData.Weapon).Level < 4) CardDraw.Apply(loadout, Card.LevelUp(SkillData.Weapon, 0));
            CardDraw.Apply(loadout, Card.Evolve("evo.boomerang"));
            Assert.That(loadout.EligibleEvolutions, Is.Empty);

            var one = new CardDraw(new SeededRandom(1), 50).Offer(loadout);
            Assert.That(one.Single().Kind, Is.EqualTo(CardKind.LevelUp));
            Assert.That(one.Single().Id, Is.EqualTo(SkillData.Weapon));

            CardDraw.Apply(loadout, one.Single());
            var gold = new CardDraw(new SeededRandom(1), 50).Offer(loadout);
            Assert.That(gold.Single().Kind, Is.EqualTo(CardKind.Gold));
            Assert.That(CardDraw.Apply(loadout, gold.Single()), Is.EqualTo(50));
        }
    }

    public class RunChestTests
    {
        [Test]
        public void Upgrade_count_follows_70_25_5()
        {
            var chest = new RunChest(new SeededRandom(42), RunChest.DefaultOdds, 10);
            var counts = new Dictionary<int, int> { [1] = 0, [3] = 0, [5] = 0 };
            const int n = 10000;
            for (var i = 0; i < n; i++) counts[chest.RollCount()]++;

            Assert.That(counts[1] / (double)n, Is.EqualTo(0.70).Within(0.02));
            Assert.That(counts[3] / (double)n, Is.EqualTo(0.25).Within(0.02));
            Assert.That(counts[5] / (double)n, Is.EqualTo(0.05).Within(0.02));
        }

        [Test]
        public void Evolution_has_priority_and_first_in_data_order_wins()
        {
            var loadout = SkillData.NewLoadout();
            SkillData.Max(loadout, SkillData.Mines);
            SkillData.Max(loadout, SkillData.Fire);
            SkillData.Max(loadout, SkillData.Lightning);
            var chest = new RunChest(new SeededRandom(0), new[] { 1.0, 0, 0 }, 10);

            var result = chest.Open(loadout).Single();

            Assert.That(result.Kind, Is.EqualTo(CardKind.Evolve));
            Assert.That(result.Id, Is.EqualTo("evo.mines.fire"));
            Assert.That(loadout.Owns(SkillData.Fire), Is.False);
        }

        [Test]
        public void Upgrades_only_owned_skills_and_falls_back_to_gold()
        {
            var loadout = SkillData.NewLoadout();
            var chest = new RunChest(new SeededRandom(0), new[] { 0, 0, 1.0 }, 10);

            var result = chest.Open(loadout);

            Assert.That(result.Count, Is.EqualTo(5));
            Assert.That(result.Take(4).All(r => r.Kind == CardKind.LevelUp && r.Id == SkillData.Weapon), Is.True);
            Assert.That(result[4].Kind, Is.EqualTo(CardKind.Gold));
            Assert.That(loadout.Actives.Count, Is.EqualTo(1));
            Assert.That(loadout.Find(SkillData.Weapon).Level, Is.EqualTo(5));
        }
    }

    public class ExperienceAndCrystalTests
    {
        [Test]
        public void Excess_xp_carries_over_and_multiple_level_ups_are_counted()
        {
            var bar = new ExperienceBar(level => 10);

            Assert.That(bar.Add(25), Is.EqualTo(2));
            Assert.That(bar.Level, Is.EqualTo(3));
            Assert.That(bar.Xp, Is.EqualTo(5));
        }

        [Test]
        public void Default_curve_grows()
        {
            var curve = ExperienceBar.DefaultCurve;
            for (var level = 1; level < 50; level++) Assert.That(curve(level + 1), Is.GreaterThanOrEqualTo(curve(level)));
        }

        [Test]
        public void Normal_enemies_drop_yellow_about_two_percent()
        {
            var drops = new CrystalDrops(new SeededRandom(7));
            const int n = 100000;
            var yellow = Enumerable.Range(0, n).Count(_ => drops.Drop(EnemyClass.Normal) == CrystalKind.Yellow);

            Assert.That(yellow / (double)n, Is.EqualTo(0.02).Within(0.005));
        }

        [Test]
        public void Elites_and_bosses_always_drop_yellow_and_cleared_enemies_drop_nothing()
        {
            var drops = new CrystalDrops(new SeededRandom(7));

            Assert.That(drops.Drop(EnemyClass.Elite), Is.EqualTo(CrystalKind.Yellow));
            Assert.That(drops.Drop(EnemyClass.Boss), Is.EqualTo(CrystalKind.Yellow));
            Assert.That(drops.Drop(EnemyClass.Normal, removedByBossEntry: true), Is.EqualTo(CrystalKind.None));
            Assert.That(CrystalDrops.Value(CrystalKind.Blue), Is.EqualTo(5));
        }
    }
}
