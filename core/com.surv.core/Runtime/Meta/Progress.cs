using System;
using System.Collections.Generic;
using System.Linq;
using Surv.Core.Common;
using Surv.Core.Equipment;
using Surv.Core.Run;
using Surv.Core.Save;

namespace Surv.Core.Meta
{
    /// <summary>Reward numbers (SPEC W28, W29; decyzja autora, all in data).</summary>
    public sealed class RewardBalance
    {
        public Func<int, long> WinGoldBonus { get; set; } = chapter => 500L + 250L * (chapter - 1);
        public Func<int, long> WinAccountXp { get; set; } = chapter => 1000L + 150L * (chapter - 1);

        /// <summary>Chance of 1, 2 and 3 items on a win.</summary>
        public IReadOnlyList<double> ItemCountOdds { get; set; } = new[] { 0.6, 0.3, 0.1 };

        public Func<int, Tier> ItemTierForChapter { get; set; } = chapter => chapter <= 1 ? Tier.Normal : chapter == 2 ? Tier.Good : Tier.Better;
        public double PlanDropChanceOnLoss { get; set; } = 0.5;
        public Func<int, long> PlansOnWin { get; set; } = chapter => 2L + chapter;
    }

    public sealed class RunReward
    {
        public long Gold { get; set; }
        public long AccountXp { get; set; }
        public int ItemCount { get; set; }
        public Tier ItemTier { get; set; }
        public long Plans { get; set; }
    }

    /// <summary>
    /// End-of-run rewards (SPEC W28; PLAN P22): gold collected in the run always counts in full; the win bonus and
    /// account XP scale with survival time / full map time on a loss; items only on a win.
    /// </summary>
    public sealed class RewardCalculator
    {
        private readonly RewardBalance _balance;
        private readonly IRandom _random;

        public RewardCalculator(RewardBalance balance, IRandom random)
        {
            _balance = balance ?? throw new ArgumentNullException(nameof(balance));
            _random = random ?? throw new ArgumentNullException(nameof(random));
        }

        public RunReward Compute(int chapter, bool won, double survivedSeconds, double fullMapSeconds, long collectedGold)
        {
            if (chapter < 1) throw new ArgumentOutOfRangeException(nameof(chapter));
            if (fullMapSeconds <= 0) throw new ArgumentOutOfRangeException(nameof(fullMapSeconds));
            var share = won ? 1.0 : Math.Max(0, Math.Min(1, survivedSeconds / fullMapSeconds));
            var reward = new RunReward
            {
                Gold = collectedGold + (long)Math.Floor(_balance.WinGoldBonus(chapter) * share),
                AccountXp = (long)Math.Floor(_balance.WinAccountXp(chapter) * share),
                ItemTier = _balance.ItemTierForChapter(chapter),
            };
            if (won)
            {
                reward.ItemCount = RollItemCount();
                reward.Plans = _balance.PlansOnWin(chapter);
            }
            else
            {
                reward.Plans = _random.NextDouble() < _balance.PlanDropChanceOnLoss ? 1 : 0;
            }
            return reward;
        }

        private int RollItemCount()
        {
            var roll = _random.NextDouble();
            for (var i = 0; i < _balance.ItemCountOdds.Count; i++)
            {
                roll -= _balance.ItemCountOdds[i];
                if (roll < 0) return i + 1;
            }
            return _balance.ItemCountOdds.Count;
        }
    }

    /// <summary>Account level from run (and from F2 patrol) XP, capped (SPEC W29: 10 in F1).</summary>
    public sealed class AccountProgress
    {
        private readonly Func<int, long> _xpToNext;

        public AccountProgress(int level, long xp, Func<int, long> xpToNext, int maxLevel = 10)
        {
            if (level < 1 || level > maxLevel) throw new ArgumentOutOfRangeException(nameof(level));
            Level = level;
            Xp = xp;
            MaxLevel = maxLevel;
            _xpToNext = xpToNext ?? throw new ArgumentNullException(nameof(xpToNext));
        }

        public int Level { get; private set; }
        public long Xp { get; private set; }
        public int MaxLevel { get; }

        public static Func<int, long> DefaultCurve => level => 1000L * level;

        /// <summary>Returns the number of level-ups; XP stops counting at the cap.</summary>
        public int Add(long xp)
        {
            if (xp < 0) throw new ArgumentOutOfRangeException(nameof(xp));
            if (Level >= MaxLevel) return 0;
            Xp += xp;
            var ups = 0;
            while (Level < MaxLevel && Xp >= _xpToNext(Level))
            {
                Xp -= _xpToNext(Level);
                Level++;
                ups++;
            }
            if (Level >= MaxLevel) Xp = 0;
            return ups;
        }
    }

    /// <summary>A stat talent bought with gold (SPEC W30). Value per level is a percentage of the stat.</summary>
    public sealed class TalentDefinition
    {
        public TalentDefinition(string id, Stat stat, double valuePerLevel, Func<int, long> goldForLevel)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Talent id is required.", nameof(id));
            Id = id;
            Stat = stat;
            ValuePerLevel = valuePerLevel;
            GoldForLevel = goldForLevel ?? throw new ArgumentNullException(nameof(goldForLevel));
        }

        public string Id { get; }
        public Stat Stat { get; }
        public double ValuePerLevel { get; }

        /// <summary>Gold to go from the given level to the next.</summary>
        public Func<int, long> GoldForLevel { get; }
    }

    public enum TalentError
    {
        None,
        UnknownTalent,
        AboveAccountLevel,
        NotEnoughGold,
    }

    public sealed class TalentBook
    {
        private readonly Dictionary<string, TalentDefinition> _talents;
        private readonly Dictionary<string, int> _levels;

        public TalentBook(IEnumerable<TalentDefinition> talents, IDictionary<string, int> levels = null)
        {
            _talents = talents.ToDictionary(t => t.Id);
            _levels = levels == null ? new Dictionary<string, int>() : new Dictionary<string, int>(levels);
        }

        public int LevelOf(string id) => _levels.TryGetValue(id, out var level) ? level : 0;

        public IReadOnlyDictionary<string, int> Levels => _levels;

        /// <summary>A talent's level may not exceed the account level (SPEC W29, W30).</summary>
        public TalentError Upgrade(string id, int accountLevel, Inventory wallet)
        {
            if (!_talents.TryGetValue(id, out var talent)) return TalentError.UnknownTalent;
            var level = LevelOf(id);
            if (level + 1 > accountLevel) return TalentError.AboveAccountLevel;
            if (!wallet.TrySpendGold(talent.GoldForLevel(level))) return TalentError.NotEnoughGold;
            _levels[id] = level + 1;
            return TalentError.None;
        }

        public IEnumerable<StatModifier> Modifiers() =>
            _talents.Values
                .Where(t => LevelOf(t.Id) > 0)
                .Select(t => new StatModifier(t.Stat, t.ValuePerLevel * LevelOf(t.Id), ModifierSource.Talent));
    }

    /// <summary>Chapter unlocks, best times and the tutorial flag (SPEC W26, W37).</summary>
    public sealed class ChapterProgress
    {
        private readonly SaveData _save;
        private readonly int _chapterCount;

        public ChapterProgress(SaveData save, int chapterCount)
        {
            _save = save ?? throw new ArgumentNullException(nameof(save));
            if (chapterCount < 1) throw new ArgumentOutOfRangeException(nameof(chapterCount));
            _chapterCount = chapterCount;
        }

        public bool IsUnlocked(int chapter) => chapter >= 1 && chapter <= Math.Min(_save.UnlockedChapter, _chapterCount);

        /// <summary>Records a finished run (win or loss). Interrupted runs are never recorded (SPEC W42).</summary>
        public void RecordRun(int chapter, bool won, double seconds)
        {
            if (!IsUnlocked(chapter)) throw new InvalidOperationException($"Chapter {chapter} is locked.");
            if (!_save.Chapters.TryGetValue(chapter, out var record))
            {
                record = new ChapterRecord();
                _save.Chapters[chapter] = record;
            }
            record.BestSurvivalSeconds = Math.Max(record.BestSurvivalSeconds, seconds);
            if (won)
            {
                record.Won = true;
                record.BestWinSeconds = record.BestWinSeconds.HasValue ? Math.Min(record.BestWinSeconds.Value, seconds) : seconds;
                if (chapter == _save.UnlockedChapter && chapter < _chapterCount) _save.UnlockedChapter = chapter + 1;
            }
            _save.TutorialDone = true;
        }

        /// <summary>Shortest win; before the first win the longest survival (SPEC W26).</summary>
        public double? BestTime(int chapter)
        {
            if (!_save.Chapters.TryGetValue(chapter, out var record)) return null;
            return record.Won ? record.BestWinSeconds : record.BestSurvivalSeconds;
        }

        /// <summary>Each chapter's enemy multiplier must be at least 1.5× the previous one (SPEC W26).</summary>
        public static IReadOnlyList<string> ValidateMultipliers(IReadOnlyList<double> multipliers, double minRatio = 1.5)
        {
            var errors = new List<string>();
            for (var i = 1; i < multipliers.Count; i++)
            {
                if (multipliers[i] + 1e-9 < multipliers[i - 1] * minRatio)
                {
                    errors.Add($"Chapter {i + 1} multiplier {multipliers[i]} is below {minRatio}× chapter {i} ({multipliers[i - 1]}).");
                }
            }
            return errors;
        }
    }
}
