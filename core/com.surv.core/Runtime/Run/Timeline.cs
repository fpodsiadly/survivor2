using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Run
{
    /// <summary>Map formats of SPEC W6–W9.</summary>
    public enum MapFormat
    {
        /// <summary>Infinite in both axes, 15 min, 3 bosses (W6).</summary>
        Open,

        /// <summary>Bounded rectangle, 8 min, 5 bosses, no arena circle (W7).</summary>
        Closed,

        /// <summary>Fixed width, infinite vertically, rules of W6 (W8).</summary>
        Corridor,
    }

    public enum TimelineEventKind
    {
        /// <summary>Changes which enemies spawn and how strong they are.</summary>
        SpawnPhase,
        Horde,
        Elite,
        Boss,
    }

    /// <summary>One entry of a chapter timeline (SPEC W6, W7). Time is the moment the event enters.</summary>
    public sealed class TimelineEvent
    {
        public TimelineEvent(double timeSeconds, TimelineEventKind kind, string id, double strength = 1, bool isFinalBoss = false)
        {
            if (timeSeconds < 0) throw new ArgumentOutOfRangeException(nameof(timeSeconds));
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Event id is required.", nameof(id));
            if (isFinalBoss && kind != TimelineEventKind.Boss) throw new ArgumentException("Only a boss can be final.", nameof(isFinalBoss));
            TimeSeconds = timeSeconds;
            Kind = kind;
            Id = id;
            Strength = strength;
            IsFinalBoss = isFinalBoss;
        }

        public double TimeSeconds { get; }
        public TimelineEventKind Kind { get; }

        /// <summary>Enemy set, horde, elite or boss definition id.</summary>
        public string Id { get; }

        /// <summary>Strength multiplier of a spawn phase (ignored for other kinds).</summary>
        public double Strength { get; }

        public bool IsFinalBoss { get; }

        /// <summary>Hordes and bosses get a warning; elites and spawn phases do not (SPEC W11).</summary>
        public bool HasAlarm => Kind == TimelineEventKind.Horde || Kind == TimelineEventKind.Boss;

        public override string ToString() => $"{TimeSeconds:0.#}s {Kind} {Id}{(IsFinalBoss ? " (final)" : "")}";
    }

    public sealed class ChapterTimeline
    {
        public ChapterTimeline(MapFormat format, IEnumerable<TimelineEvent> events)
        {
            Format = format;
            Events = (events ?? throw new ArgumentNullException(nameof(events))).OrderBy(e => e.TimeSeconds).ToList();
        }

        public MapFormat Format { get; }
        public IReadOnlyList<TimelineEvent> Events { get; }

        /// <summary>Nominal boss entry times of each format (SPEC W6, W7).</summary>
        public static IReadOnlyList<double> BossTimes(MapFormat format) =>
            format == MapFormat.Closed
                ? new double[] { 90, 180, 270, 360, 480 }
                : new double[] { 300, 600, 900 };
    }
}
