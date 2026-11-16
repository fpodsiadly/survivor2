using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Run
{
    public enum TimelineSignalKind
    {
        /// <summary>Show the warning banner and play the alarm sound (SPEC W11).</summary>
        Alarm,

        /// <summary>The event enters now.</summary>
        Fire,

        /// <summary>A non-boss event fell inside a boss fight and does not happen (SPEC W10).</summary>
        Skip,

        /// <summary>The final boss died: the run is won (SPEC W10, W13).</summary>
        Won,
    }

    public sealed class TimelineSignal
    {
        public TimelineSignal(TimelineSignalKind kind, TimelineEvent timelineEvent, double atSeconds)
        {
            Kind = kind;
            Event = timelineEvent;
            AtSeconds = atSeconds;
        }

        public TimelineSignalKind Kind { get; }
        public TimelineEvent Event { get; }
        public double AtSeconds { get; }

        public override string ToString() => $"{AtSeconds:0.##}s {Kind} {Event}";
    }

    /// <summary>
    /// Plays a chapter timeline (SPEC W6, W7, W10, W11; PLAN P15):
    /// the clock always runs; during a boss fight non-boss events are skipped and a boss that falls due waits;
    /// after the fight a waiting boss gets its alarm at once and enters <see cref="AlarmLeadSeconds"/> later.
    /// The same rule covers the closed map, where a living boss delays the next one.
    /// </summary>
    public sealed class TimelineScheduler
    {
        private enum EntryState
        {
            Pending,
            WaitingForArena,
            Done,
        }

        private sealed class Entry
        {
            public Entry(TimelineEvent timelineEvent)
            {
                Event = timelineEvent;
                Due = timelineEvent.TimeSeconds;
            }

            public TimelineEvent Event { get; }
            public double Due { get; set; }
            public bool Alarmed { get; set; }
            public EntryState State { get; set; } = EntryState.Pending;
        }

        private readonly List<Entry> _entries;
        private Entry _activeBoss;

        public TimelineScheduler(ChapterTimeline timeline, double alarmLeadSeconds = 3)
        {
            if (timeline == null) throw new ArgumentNullException(nameof(timeline));
            if (alarmLeadSeconds < 0) throw new ArgumentOutOfRangeException(nameof(alarmLeadSeconds));
            _entries = timeline.Events.Select(e => new Entry(e)).ToList();
            AlarmLeadSeconds = alarmLeadSeconds;
        }

        public double AlarmLeadSeconds { get; }
        public double ElapsedSeconds { get; private set; }
        public bool BossActive => _activeBoss != null;
        public TimelineEvent ActiveBoss => _activeBoss?.Event;
        public bool Won { get; private set; }

        /// <summary>Moves the clock forward. Do not call while the game is paused (SPEC W5).</summary>
        public IReadOnlyList<TimelineSignal> Advance(double deltaSeconds)
        {
            if (deltaSeconds < 0) throw new ArgumentOutOfRangeException(nameof(deltaSeconds));
            var signals = new List<TimelineSignal>();
            if (Won) return signals;

            ElapsedSeconds += deltaSeconds;
            foreach (var entry in _entries.Where(e => e.State == EntryState.Pending).OrderBy(e => e.Due).ToList())
            {
                if (entry.Event.HasAlarm && !entry.Alarmed && !BossActive && ElapsedSeconds >= entry.Due - AlarmLeadSeconds)
                {
                    entry.Alarmed = true;
                    signals.Add(new TimelineSignal(TimelineSignalKind.Alarm, entry.Event, Math.Max(0, entry.Due - AlarmLeadSeconds)));
                }

                if (ElapsedSeconds < entry.Due) continue;

                if (BossActive)
                {
                    if (entry.Event.Kind == TimelineEventKind.Boss)
                    {
                        entry.State = EntryState.WaitingForArena;
                    }
                    else
                    {
                        entry.State = EntryState.Done;
                        signals.Add(new TimelineSignal(TimelineSignalKind.Skip, entry.Event, entry.Due));
                    }
                    continue;
                }

                entry.State = EntryState.Done;
                signals.Add(new TimelineSignal(TimelineSignalKind.Fire, entry.Event, entry.Due));
                if (entry.Event.Kind == TimelineEventKind.Boss) _activeBoss = entry;
            }
            return signals;
        }

        /// <summary>Call when the active boss dies.</summary>
        public IReadOnlyList<TimelineSignal> NotifyBossDefeated()
        {
            if (!BossActive) throw new InvalidOperationException("No boss is active.");
            var signals = new List<TimelineSignal>();
            var defeated = _activeBoss;
            _activeBoss = null;

            if (defeated.Event.IsFinalBoss)
            {
                Won = true;
                signals.Add(new TimelineSignal(TimelineSignalKind.Won, defeated.Event, ElapsedSeconds));
                return signals;
            }

            var waiting = _entries
                .Where(e => e.State == EntryState.WaitingForArena)
                .OrderBy(e => e.Event.TimeSeconds)
                .FirstOrDefault();
            if (waiting != null)
            {
                waiting.State = EntryState.Pending;
                waiting.Due = ElapsedSeconds + AlarmLeadSeconds;
                waiting.Alarmed = true;
                signals.Add(new TimelineSignal(TimelineSignalKind.Alarm, waiting.Event, ElapsedSeconds));
            }
            return signals;
        }
    }
}
