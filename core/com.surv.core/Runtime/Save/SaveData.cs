using System.Collections.Generic;

namespace Surv.Core.Save
{
    /// <summary>
    /// Persistent meta progress (SPEC W42). Plain settable members so Newtonsoft can map it; the in-run state is
    /// never saved (killing the app mid-run loses the run, SPEC W42 / P10).
    /// </summary>
    public sealed class SaveData
    {
        public const int CurrentVersion = 1;

        public int Version { get; set; } = CurrentVersion;

        public long Gold { get; set; }

        /// <summary>Plans per slot name (Equipment.Slot).</summary>
        public Dictionary<string, long> Plans { get; set; } = new Dictionary<string, long>();

        public List<SavedItem> Items { get; set; } = new List<SavedItem>();

        /// <summary>Equipped item uid per slot name.</summary>
        public Dictionary<string, int> Equipped { get; set; } = new Dictionary<string, int>();

        public int AccountLevel { get; set; } = 1;
        public long AccountXp { get; set; }

        /// <summary>Talent level per talent id.</summary>
        public Dictionary<string, int> Talents { get; set; } = new Dictionary<string, int>();

        /// <summary>Highest chapter that can be played (1-based).</summary>
        public int UnlockedChapter { get; set; } = 1;

        public Dictionary<int, ChapterRecord> Chapters { get; set; } = new Dictionary<int, ChapterRecord>();

        public bool TutorialDone { get; set; }

        public SettingsData Settings { get; set; } = new SettingsData();
    }

    public sealed class SavedItem
    {
        public int Uid { get; set; }
        public string TypeId { get; set; }
        public string Tier { get; set; }
        public int Level { get; set; }
    }

    public sealed class ChapterRecord
    {
        public bool Won { get; set; }

        /// <summary>Shortest winning time in seconds, or null before the first win.</summary>
        public double? BestWinSeconds { get; set; }

        /// <summary>Longest survival in seconds (shown before the first win, SPEC W26).</summary>
        public double BestSurvivalSeconds { get; set; }
    }

    public sealed class SettingsData
    {
        public float MusicVolume { get; set; } = 0.8f;
        public float EffectsVolume { get; set; } = 1f;
        public bool Vibration { get; set; } = true;
        public bool DamageNumbers { get; set; } = true;

        /// <summary>"pl", "en", or null to follow the system language (SPEC W44).</summary>
        public string Language { get; set; }
    }
}
