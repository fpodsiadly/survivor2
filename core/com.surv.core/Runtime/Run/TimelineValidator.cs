using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Run
{
    /// <summary>Checks a chapter timeline against the format rules of SPEC W6/W7/W8 (PLAN P15). Empty list = valid.</summary>
    public static class TimelineValidator
    {
        private const double Epsilon = 1e-6;

        public static IReadOnlyList<string> Validate(ChapterTimeline timeline)
        {
            if (timeline == null) throw new ArgumentNullException(nameof(timeline));
            var errors = new List<string>();
            var events = timeline.Events;
            var expectedBossTimes = ChapterTimeline.BossTimes(timeline.Format);
            var bosses = events.Where(e => e.Kind == TimelineEventKind.Boss).ToList();

            CheckBosses(bosses, expectedBossTimes, errors);
            CheckSpawnPhases(events, expectedBossTimes, errors);

            if (timeline.Format == MapFormat.Closed)
            {
                CheckClosedGaps(events, expectedBossTimes, errors);
            }
            else
            {
                CheckOpenRules(events, expectedBossTimes, errors);
            }
            return errors;
        }

        private static void CheckBosses(IReadOnlyList<TimelineEvent> bosses, IReadOnlyList<double> expected, List<string> errors)
        {
            var times = bosses.Select(b => b.TimeSeconds).ToList();
            if (times.Count != expected.Count || times.Zip(expected, (a, b) => Math.Abs(a - b) > Epsilon).Any(x => x))
            {
                errors.Add($"Bosses must enter at {string.Join(", ", expected)} s; found {string.Join(", ", times)}.");
            }
            var finals = bosses.Where(b => b.IsFinalBoss).ToList();
            if (finals.Count != 1 || bosses.Count == 0 || !bosses.Last().IsFinalBoss)
            {
                errors.Add("Exactly one boss, the last one, must be final.");
            }
            if (bosses.Any(b => b.TimeSeconds > expected.Last() + Epsilon))
            {
                errors.Add("No boss may come after the final boss time.");
            }
        }

        private static void CheckSpawnPhases(IReadOnlyList<TimelineEvent> events, IReadOnlyList<double> bossTimes, List<string> errors)
        {
            var phases = events.Where(e => e.Kind == TimelineEventKind.SpawnPhase).ToList();
            if (phases.Count == 0 || phases[0].TimeSeconds > Epsilon)
            {
                errors.Add("A spawn phase must start at 0 s.");
                return;
            }
            for (var i = 1; i < phases.Count; i++)
            {
                if (phases[i].Strength + Epsilon < phases[i - 1].Strength)
                {
                    errors.Add($"Enemy strength must not drop over time (at {phases[i].TimeSeconds} s).");
                }
            }
            var finalTime = bossTimes.Last();
            if (events.Any(e => e.TimeSeconds > finalTime + Epsilon))
            {
                errors.Add($"No event may come after the final boss at {finalTime} s.");
            }
        }

        /// <summary>W6/W8: ≥3 elites, ≥3 hordes, each segment before a boss has an elite or horde, a new or stronger phase after each middle boss.</summary>
        private static void CheckOpenRules(IReadOnlyList<TimelineEvent> events, IReadOnlyList<double> bossTimes, List<string> errors)
        {
            if (events.Count(e => e.Kind == TimelineEventKind.Elite) < 3) errors.Add("At least 3 elites are required.");
            if (events.Count(e => e.Kind == TimelineEventKind.Horde) < 3) errors.Add("At least 3 hordes are required.");

            var start = 0.0;
            foreach (var bossTime in bossTimes)
            {
                var from = start;
                var hasPressure = events.Any(e =>
                    (e.Kind == TimelineEventKind.Elite || e.Kind == TimelineEventKind.Horde) &&
                    e.TimeSeconds > from - Epsilon && e.TimeSeconds < bossTime - Epsilon);
                if (!hasPressure) errors.Add($"Segment {from}–{bossTime} s needs at least one elite or horde.");
                start = bossTime;
            }

            var phases = events.Where(e => e.Kind == TimelineEventKind.SpawnPhase).ToList();
            for (var i = 0; i < bossTimes.Count - 1; i++)
            {
                var after = bossTimes[i];
                var before = bossTimes[i + 1];
                var previous = phases.LastOrDefault(p => p.TimeSeconds <= after + Epsilon);
                var changed = phases.Any(p =>
                    p.TimeSeconds > after - Epsilon && p.TimeSeconds < before - Epsilon && previous != null &&
                    (p.Id != previous.Id || p.Strength > previous.Strength + Epsilon));
                if (!changed) errors.Add($"After the boss at {after} s a new or stronger enemy phase is required.");
            }
        }

        /// <summary>W7: between each pair of consecutive bosses at least 2 hordes and 1 elite.</summary>
        private static void CheckClosedGaps(IReadOnlyList<TimelineEvent> events, IReadOnlyList<double> bossTimes, List<string> errors)
        {
            for (var i = 0; i < bossTimes.Count - 1; i++)
            {
                var from = bossTimes[i];
                var to = bossTimes[i + 1];
                var inGap = events.Where(e => e.TimeSeconds > from + Epsilon && e.TimeSeconds < to - Epsilon).ToList();
                if (inGap.Count(e => e.Kind == TimelineEventKind.Horde) < 2 || inGap.Count(e => e.Kind == TimelineEventKind.Elite) < 1)
                {
                    errors.Add($"Gap {from}–{to} s needs at least 2 hordes and 1 elite.");
                }
            }
        }
    }
}
