using System.Collections.Generic;
using System.Linq;
using NUnit.Framework;
using Surv.Core.Run;
using K = Surv.Core.Run.TimelineEventKind;

namespace Surv.Core.Tests.Run
{
    internal static class TimelineData
    {
        public static TimelineEvent Phase(double t, string id, double strength = 1) => new TimelineEvent(t, K.SpawnPhase, id, strength);
        public static TimelineEvent Horde(double t) => new TimelineEvent(t, K.Horde, "horde");
        public static TimelineEvent Elite(double t) => new TimelineEvent(t, K.Elite, "elite");
        public static TimelineEvent Boss(double t, string id, bool final = false) => new TimelineEvent(t, K.Boss, id, isFinalBoss: final);

        /// <summary>A valid open-map timeline (own design, not a Survivor.io chapter).</summary>
        public static ChapterTimeline ValidOpen() => new ChapterTimeline(MapFormat.Open, new[]
        {
            Phase(0, "set.a"), Horde(100), Elite(200),
            Boss(300, "boss.1"), Phase(310, "set.b", 1.5), Horde(420), Elite(500),
            Boss(600, "boss.2"), Phase(610, "set.b", 2), Horde(700), Elite(800),
            Boss(900, "boss.final", final: true),
        });

        public static ChapterTimeline ValidClosed()
        {
            var events = new List<TimelineEvent> { Phase(0, "set.a"), Horde(30), Elite(60) };
            var bosses = new double[] { 90, 180, 270, 360, 480 };
            for (var i = 0; i < bosses.Length; i++)
            {
                events.Add(Boss(bosses[i], $"boss.{i + 1}", final: i == bosses.Length - 1));
                if (i == bosses.Length - 1) break;
                var gapStart = bosses[i];
                var gap = bosses[i + 1] - gapStart;
                events.Add(Horde(gapStart + gap * 0.3));
                events.Add(Elite(gapStart + gap * 0.5));
                events.Add(Horde(gapStart + gap * 0.7));
            }
            return new ChapterTimeline(MapFormat.Closed, events);
        }
    }

    public class TimelineSchedulerTests
    {
        private static List<TimelineSignal> Run(TimelineScheduler s, double until, double step = 0.5)
        {
            var all = new List<TimelineSignal>();
            while (s.ElapsedSeconds + 1e-9 < until) all.AddRange(s.Advance(step));
            return all;
        }

        [Test]
        public void Horde_and_boss_get_an_alarm_three_seconds_early_elites_do_not()
        {
            var s = new TimelineScheduler(TimelineData.ValidOpen());

            var signals = Run(s, 300);

            var alarms = signals.Where(x => x.Kind == TimelineSignalKind.Alarm).ToList();
            Assert.That(alarms.Select(a => a.Event.Kind), Is.EquivalentTo(new[] { K.Horde, K.Boss }));
            Assert.That(alarms.Single(a => a.Event.Kind == K.Horde).AtSeconds, Is.EqualTo(97));
            Assert.That(alarms.Single(a => a.Event.Kind == K.Boss).AtSeconds, Is.EqualTo(297));
            Assert.That(signals.Any(x => x.Kind == TimelineSignalKind.Fire && x.Event.Kind == K.Elite), Is.True);
        }

        [Test]
        public void Boss_starts_a_fight_and_non_boss_events_inside_it_are_skipped()
        {
            var s = new TimelineScheduler(TimelineData.ValidOpen());
            Run(s, 300);
            Assert.That(s.BossActive, Is.True);
            Assert.That(s.ActiveBoss.Id, Is.EqualTo("boss.1"));

            var during = Run(s, 450);

            Assert.That(during.Where(x => x.Kind == TimelineSignalKind.Skip).Select(x => x.Event.TimeSeconds), Is.EquivalentTo(new[] { 310.0, 420.0 }));
            Assert.That(during.Any(x => x.Kind == TimelineSignalKind.Alarm), Is.False);
            Assert.That(s.ElapsedSeconds, Is.EqualTo(450).Within(1e-9), "clock keeps running during the fight");
        }

        [Test]
        public void Boss_falling_due_during_a_fight_waits_then_enters_three_seconds_after_it()
        {
            var s = new TimelineScheduler(TimelineData.ValidOpen());
            Run(s, 630);
            Assert.That(s.ActiveBoss.Id, Is.EqualTo("boss.1"), "boss 1 still alive at 10:30");

            var afterKill = s.NotifyBossDefeated();

            Assert.That(afterKill.Single().Kind, Is.EqualTo(TimelineSignalKind.Alarm));
            Assert.That(afterKill.Single().Event.Id, Is.EqualTo("boss.2"));
            Assert.That(s.BossActive, Is.False);

            var next = Run(s, 633.5);
            var fire = next.Single(x => x.Kind == TimelineSignalKind.Fire);
            Assert.That(fire.Event.Id, Is.EqualTo("boss.2"));
            Assert.That(fire.AtSeconds, Is.EqualTo(633).Within(1e-9));
            Assert.That(next.Any(x => x.Kind == TimelineSignalKind.Alarm), Is.False, "no second alarm");
        }

        [Test]
        public void Every_boss_appears_even_when_fights_run_long()
        {
            var s = new TimelineScheduler(TimelineData.ValidOpen());
            var fired = new List<string>();
            var bossEnteredAt = 0.0;
            for (var i = 0; i < 4000 && !s.Won; i++)
            {
                foreach (var signal in s.Advance(0.5))
                {
                    if (signal.Kind != TimelineSignalKind.Fire || signal.Event.Kind != K.Boss) continue;
                    fired.Add(signal.Event.Id);
                    bossEnteredAt = signal.AtSeconds;
                }
                // Each boss lives 400 s, so every later boss falls due during a fight.
                if (s.BossActive && s.ElapsedSeconds >= bossEnteredAt + 400) s.NotifyBossDefeated();
            }

            Assert.That(fired, Is.EqualTo(new[] { "boss.1", "boss.2", "boss.final" }));
            Assert.That(s.Won, Is.True);
        }

        [Test]
        public void Killing_the_final_boss_wins_and_stops_the_timeline()
        {
            var s = new TimelineScheduler(TimelineData.ValidOpen());
            Run(s, 300); s.NotifyBossDefeated();
            Run(s, 600); s.NotifyBossDefeated();
            Run(s, 900);
            Assert.That(s.ActiveBoss.IsFinalBoss, Is.True);

            var signals = s.NotifyBossDefeated();

            Assert.That(signals.Single().Kind, Is.EqualTo(TimelineSignalKind.Won));
            Assert.That(s.Won, Is.True);
            Assert.That(s.Advance(10), Is.Empty);
        }

        [Test]
        public void Closed_map_living_boss_delays_the_next_one()
        {
            var s = new TimelineScheduler(TimelineData.ValidClosed());
            Run(s, 200);
            Assert.That(s.ActiveBoss.Id, Is.EqualTo("boss.1"));

            s.NotifyBossDefeated();
            var next = Run(s, 203.5);

            Assert.That(next.Single(x => x.Kind == TimelineSignalKind.Fire).Event.Id, Is.EqualTo("boss.2"));
        }

        [Test]
        public void Notify_without_a_boss_throws()
        {
            var s = new TimelineScheduler(TimelineData.ValidOpen());
            Assert.Throws<System.InvalidOperationException>(() => s.NotifyBossDefeated());
        }
    }

    public class TimelineValidatorTests
    {
        [Test]
        public void Valid_open_and_closed_timelines_pass()
        {
            Assert.That(TimelineValidator.Validate(TimelineData.ValidOpen()), Is.Empty);
            Assert.That(TimelineValidator.Validate(TimelineData.ValidClosed()), Is.Empty);
            var corridor = new ChapterTimeline(MapFormat.Corridor, TimelineData.ValidOpen().Events);
            Assert.That(TimelineValidator.Validate(corridor), Is.Empty);
        }

        [Test]
        public void Rejects_wrong_boss_times()
        {
            var events = TimelineData.ValidOpen().Events.Where(e => e.Id != "boss.2").Append(TimelineData.Boss(610, "boss.2"));
            var errors = TimelineValidator.Validate(new ChapterTimeline(MapFormat.Open, events));
            Assert.That(errors, Has.Some.Contains("Bosses must enter"));
        }

        [Test]
        public void Rejects_missing_final_flag()
        {
            var events = TimelineData.ValidOpen().Events.Where(e => !e.IsFinalBoss).Append(TimelineData.Boss(900, "boss.final"));
            Assert.That(TimelineValidator.Validate(new ChapterTimeline(MapFormat.Open, events)), Has.Some.Contains("final"));
        }

        [Test]
        public void Rejects_too_few_elites_and_an_empty_segment()
        {
            var events = TimelineData.ValidOpen().Events.Where(e => !(e.Kind == K.Elite && e.TimeSeconds == 800) && !(e.Kind == K.Horde && e.TimeSeconds == 700));
            var errors = TimelineValidator.Validate(new ChapterTimeline(MapFormat.Open, events));
            Assert.That(errors, Has.Some.Contains("3 elites"));
            Assert.That(errors, Has.Some.Contains("Segment 600–900"));
        }

        [Test]
        public void Rejects_no_new_phase_after_a_middle_boss()
        {
            var events = TimelineData.ValidOpen().Events.Where(e => !(e.Kind == K.SpawnPhase && e.TimeSeconds == 610));
            Assert.That(TimelineValidator.Validate(new ChapterTimeline(MapFormat.Open, events)), Has.Some.Contains("After the boss at 600"));
        }

        [Test]
        public void Rejects_dropping_strength()
        {
            var events = TimelineData.ValidOpen().Events.Append(TimelineData.Phase(850, "set.c", 1.0));
            Assert.That(TimelineValidator.Validate(new ChapterTimeline(MapFormat.Open, events)), Has.Some.Contains("strength"));
        }

        [Test]
        public void Closed_map_requires_two_hordes_and_an_elite_in_each_gap()
        {
            var events = TimelineData.ValidClosed().Events.Where(e => !(e.Kind == K.Horde && e.TimeSeconds > 270 && e.TimeSeconds < 360)).ToList();
            events.Add(TimelineData.Horde(300));
            var errors = TimelineValidator.Validate(new ChapterTimeline(MapFormat.Closed, events));
            Assert.That(errors, Has.Some.Contains("Gap 270–360"));
            Assert.That(errors.Count, Is.EqualTo(1));
        }
    }
}
