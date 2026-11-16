using System.Linq;
using NUnit.Framework;
using Surv.Core.Common;
using Surv.Core.Equipment;
using Surv.Core.Meta;
using Surv.Core.Run;
using Surv.Core.Save;
using Surv.Core.Tests.Equipment;
using Surv.Core.Tests.Run;

namespace Surv.Core.Tests.Meta
{
    public class StatTests
    {
        [Test]
        public void Passive_levels_two_to_four_are_linear()
        {
            var attack = new PassiveEffect("p.atk", Stat.Attack, 0.10, 0.50);

            Assert.That(Enumerable.Range(1, 5).Select(attack.ValueAt), Is.EqualTo(new[] { 0.10, 0.20, 0.30, 0.40, 0.50 }).Within(1e-9));
        }

        [Test]
        public void Talent_and_passive_bonuses_multiply_equipment_bonuses_add()
        {
            var modifiers = new[]
            {
                new StatModifier(Stat.Attack, 0.10, ModifierSource.Equipment),
                new StatModifier(Stat.Attack, 0.15, ModifierSource.Equipment),
                new StatModifier(Stat.Attack, 0.20, ModifierSource.Talent),
                new StatModifier(Stat.Attack, 0.50, ModifierSource.Passive),
            };

            Assert.That(StatCalculator.Attack(100, modifiers), Is.EqualTo(100 * 1.25 * 1.20 * 1.50).Within(1e-9));
        }

        [Test]
        public void Damage_reductions_combine_without_reaching_one()
        {
            var modifiers = new[]
            {
                new StatModifier(Stat.DamageReduction, 0.5, ModifierSource.Talent),
                new StatModifier(Stat.DamageReduction, 0.5, ModifierSource.Passive),
            };

            Assert.That(StatCalculator.Factor(Stat.DamageReduction, modifiers), Is.EqualTo(0.75).Within(1e-9));
        }

        [Test]
        public void Passive_modifiers_follow_owned_levels()
        {
            var loadout = SkillData.NewLoadout();
            CardDraw.Apply(loadout, Card.NewSkill(SkillData.Magnet));
            CardDraw.Apply(loadout, Card.LevelUp(SkillData.Magnet, 2));
            var effects = new[] { new PassiveEffect(SkillData.Magnet, Stat.PickupRadius, 1.0, 5.0) };

            var modifier = StatCalculator.FromPassives(loadout, effects).Single();

            Assert.That(modifier.Value, Is.EqualTo(2.0).Within(1e-9));
            Assert.That(modifier.Source, Is.EqualTo(ModifierSource.Passive));
        }
    }

    public class PlayerHealthTests
    {
        [Test]
        public void Second_hit_within_half_a_second_is_ignored()
        {
            var hp = new PlayerHealth(100);

            Assert.That(hp.TakeHit(10.0, 5, 0), Is.EqualTo(5));
            Assert.That(hp.TakeHit(10.4, 5, 0), Is.Zero);
            Assert.That(hp.TakeHit(10.5, 5, 0), Is.EqualTo(5));
            Assert.That(hp.Health, Is.EqualTo(90));
        }

        [Test]
        public void Contact_damage_is_rate_times_window_and_reduction_applies()
        {
            var hit = PlayerHealth.ContactHit(0.10, 100);
            var hp = new PlayerHealth(100);

            Assert.That(hit, Is.EqualTo(5).Within(1e-9));
            Assert.That(hp.TakeHit(0, hit, 0.2), Is.EqualTo(4).Within(1e-9));
        }

        [Test]
        public void Death_at_zero_and_meat_talent_scales_healing()
        {
            var hp = new PlayerHealth(100);
            hp.TakeHit(0, 1000, 0);

            Assert.That(hp.IsDead, Is.True);
            Assert.That(hp.Health, Is.Zero);
            Assert.That(PlayerHealth.MeatHeal(200, 0.05 * 4), Is.EqualTo(200 * 0.30 * 1.20).Within(1e-9));
        }
    }

    public class RewardTests
    {
        [Test]
        public void Loss_scales_bonus_and_xp_by_survival_share_and_keeps_collected_gold()
        {
            var balance = new RewardBalance();
            var reward = new RewardCalculator(balance, new SeededRandom(1)).Compute(2, won: false, survivedSeconds: 450, fullMapSeconds: 900, collectedGold: 300);

            Assert.That(reward.Gold, Is.EqualTo(300 + balance.WinGoldBonus(2) / 2));
            Assert.That(reward.AccountXp, Is.EqualTo((1000 + 150) / 2));
            Assert.That(reward.ItemCount, Is.Zero);
        }

        [Test]
        public void Win_gives_full_xp_and_one_to_three_items()
        {
            for (var seed = 0; seed < 50; seed++)
            {
                var reward = new RewardCalculator(new RewardBalance(), new SeededRandom(seed)).Compute(1, won: true, survivedSeconds: 930, fullMapSeconds: 900, collectedGold: 0);
                Assert.That(reward.AccountXp, Is.EqualTo(1000));
                Assert.That(reward.ItemCount, Is.InRange(1, 3));
            }
        }
    }

    public class AccountAndTalentTests
    {
        [Test]
        public void Account_level_caps_at_ten()
        {
            var account = new AccountProgress(1, 0, level => 100);

            Assert.That(account.Add(100000), Is.EqualTo(9));
            Assert.That(account.Level, Is.EqualTo(10));
            Assert.That(account.Add(500), Is.Zero);
            Assert.That(account.Xp, Is.Zero);
        }

        [Test]
        public void Talent_cannot_exceed_account_level_and_needs_gold()
        {
            var (inventory, _, _) = EquipmentTestData.Setup();
            var book = new TalentBook(new[] { new TalentDefinition("t.atk", Stat.Attack, 0.02, level => 100L * (level + 1)) });

            Assert.That(book.Upgrade("t.atk", accountLevel: 2, inventory), Is.EqualTo(TalentError.NotEnoughGold));
            inventory.AddGold(1000);
            Assert.That(book.Upgrade("t.atk", 2, inventory), Is.EqualTo(TalentError.None));
            Assert.That(book.Upgrade("t.atk", 2, inventory), Is.EqualTo(TalentError.None));
            Assert.That(book.Upgrade("t.atk", 2, inventory), Is.EqualTo(TalentError.AboveAccountLevel));
            Assert.That(inventory.Gold, Is.EqualTo(1000 - 100 - 200));
            Assert.That(book.Modifiers().Single().Value, Is.EqualTo(0.04).Within(1e-9));
        }
    }

    public class ChapterProgressTests
    {
        [Test]
        public void Win_unlocks_next_chapter_loss_does_not()
        {
            var save = new SaveData();
            var chapters = new ChapterProgress(save, chapterCount: 3);

            chapters.RecordRun(1, won: false, seconds: 400);
            Assert.That(chapters.IsUnlocked(2), Is.False);
            chapters.RecordRun(1, won: true, seconds: 920);
            Assert.That(chapters.IsUnlocked(2), Is.True);
            Assert.That(chapters.IsUnlocked(3), Is.False);
        }

        [Test]
        public void Last_chapter_does_not_unlock_beyond_the_count()
        {
            var save = new SaveData { UnlockedChapter = 3 };
            var chapters = new ChapterProgress(save, 3);

            chapters.RecordRun(3, true, 500);

            Assert.That(save.UnlockedChapter, Is.EqualTo(3));
            Assert.That(chapters.IsUnlocked(4), Is.False);
        }

        [Test]
        public void Best_time_is_longest_survival_before_a_win_then_shortest_win()
        {
            var chapters = new ChapterProgress(new SaveData(), 3);

            chapters.RecordRun(1, false, 300);
            chapters.RecordRun(1, false, 500);
            Assert.That(chapters.BestTime(1), Is.EqualTo(500));

            chapters.RecordRun(1, true, 950);
            chapters.RecordRun(1, true, 910);
            chapters.RecordRun(1, true, 990);
            Assert.That(chapters.BestTime(1), Is.EqualTo(910));
        }

        [Test]
        public void First_finished_run_sets_the_tutorial_flag_even_on_a_loss()
        {
            var save = new SaveData();
            new ChapterProgress(save, 3).RecordRun(1, won: false, seconds: 60);
            Assert.That(save.TutorialDone, Is.True);
        }

        [Test]
        public void Locked_chapter_cannot_be_recorded()
        {
            Assert.Throws<System.InvalidOperationException>(() => new ChapterProgress(new SaveData(), 3).RecordRun(2, true, 100));
        }

        [Test]
        public void Multipliers_must_grow_by_one_and_a_half()
        {
            Assert.That(ChapterProgress.ValidateMultipliers(new[] { 1.0, 1.5, 2.25 }), Is.Empty);
            Assert.That(ChapterProgress.ValidateMultipliers(new[] { 1.0, 1.4 }), Has.Count.EqualTo(1));
        }
    }
}
