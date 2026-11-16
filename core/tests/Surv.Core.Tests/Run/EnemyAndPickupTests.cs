using System.Linq;
using NUnit.Framework;
using Surv.Core.Common;
using Surv.Core.Meta;
using Surv.Core.Run;

namespace Surv.Core.Tests.Run
{
    public class BombAndTimerTests
    {
        [Test]
        public void Bomb_kills_regular_enemies_on_screen_and_takes_ten_percent_from_elites_and_bosses()
        {
            var hits = Bomb.Explode(new[]
            {
                new BombTarget(EnemyClass.Normal, 50, 50, onScreen: true),
                new BombTarget(EnemyClass.Reinforced, 80, 30, onScreen: true),
                new BombTarget(EnemyClass.Normal, 50, 50, onScreen: false),
                new BombTarget(EnemyClass.Elite, 1000, 1000, onScreen: true),
                new BombTarget(EnemyClass.Boss, 5000, 300, onScreen: true),
            });

            Assert.That(hits.Select(h => h.Killed), Is.EqualTo(new[] { true, true, false, false, true }), "a boss below 10% dies like from any other damage");
            Assert.That(hits[3].Damage, Is.EqualTo(100));
            Assert.That(hits[4].Damage, Is.EqualTo(300), "never more than the boss has left");
            Assert.That(hits[2].Damage, Is.Zero);
        }

        [Test]
        public void Pickup_vanishes_after_sixty_continuous_seconds_off_screen()
        {
            var timer = new OffscreenTimer();

            for (var i = 0; i < 59; i++) Assert.That(timer.Tick(1, onScreen: false), Is.False);
            Assert.That(timer.Tick(0.5, onScreen: true), Is.False, "back on screen resets the count");
            for (var i = 0; i < 59; i++) Assert.That(timer.Tick(1, onScreen: false), Is.False);
            Assert.That(timer.Tick(1, onScreen: false), Is.True);
            Assert.That(timer.Expired, Is.True);
        }
    }

    public class ChargerTests
    {
        [Test]
        public void Charger_stops_and_telegraphs_at_least_0_7_s_then_charges_along_the_locked_direction()
        {
            var brain = new ChargerBrain(walkSpeed: 1, chargeSpeed: 10, triggerDistance: 5);
            var self = new Vec2(0, 0);

            var approach = brain.Tick(0.1, self, new Vec2(10, 0));
            Assert.That(brain.State, Is.EqualTo(ChargerState.Chase));
            Assert.That(approach.X, Is.EqualTo(1).Within(1e-5));

            var stop = brain.Tick(0.1, self, new Vec2(4, 0));
            Assert.That(brain.State, Is.EqualTo(ChargerState.Telegraph));
            Assert.That(stop, Is.EqualTo(Vec2.Zero));

            var elapsed = 0.0;
            while (brain.State == ChargerState.Telegraph)
            {
                Assert.That(brain.Tick(0.05, self, new Vec2(0, 4)), Is.EqualTo(Vec2.Zero), "player moved, charger waits");
                elapsed += 0.05;
            }
            Assert.That(elapsed, Is.GreaterThanOrEqualTo(0.7 - 1e-9));

            var charge = brain.Tick(0.05, self, new Vec2(0, 4));
            Assert.That(brain.State, Is.EqualTo(ChargerState.Charge));
            Assert.That(charge.X, Is.EqualTo(10).Within(1e-4), "direction locked at telegraph start");
            Assert.That(charge.Y, Is.EqualTo(0).Within(1e-4));
        }

        [Test]
        public void Telegraph_shorter_than_0_7_s_is_rejected()
        {
            Assert.Throws<System.ArgumentOutOfRangeException>(() => new ChargerBrain(1, 10, 5, telegraphSeconds: 0.5));
        }
    }

    public class BossValidatorTests
    {
        [Test]
        public void Valid_boss_passes()
        {
            var boss = new BossDefinition("b1", new[] { new BossAttack("charge", 0.25, 0.8), new BossAttack("volley", 0.10, 0) }, 0.10);
            Assert.That(BossValidator.Validate(boss), Is.Empty);
        }

        [Test]
        public void Flags_single_attack_missing_telegraph_and_heavy_contact()
        {
            var boss = new BossDefinition("b2", new[] { new BossAttack("slam", 0.30, 0.5) }, 0.20);

            var errors = BossValidator.Validate(boss);

            Assert.That(errors, Has.Count.EqualTo(3));
            Assert.That(errors, Has.Some.Contains("2 attacks"));
            Assert.That(errors, Has.Some.Contains("slam"));
            Assert.That(errors, Has.Some.Contains("contact"));
        }
    }

    public class LanguageTests
    {
        [TestCase(null, "pl-PL", "pl")]
        [TestCase(null, "Polish", "pl")]
        [TestCase(null, "en-GB", "en")]
        [TestCase(null, "de", "en")]
        [TestCase(null, null, "en")]
        [TestCase("pl", "en", "pl")]
        [TestCase("en", "pl", "en")]
        [TestCase("fr", "pl", "pl")]
        public void Picks_saved_then_system_then_english(string saved, string system, string expected)
        {
            Assert.That(Language.Pick(saved, system), Is.EqualTo(expected));
        }
    }
}
