using System;
using System.Collections.Generic;
using System.Linq;
using Surv.Core.Common;

namespace Surv.Core.Run
{
    /// <summary>One upgrade handed out by an in-run chest.</summary>
    public sealed class ChestUpgrade
    {
        public ChestUpgrade(CardKind kind, string id, long gold)
        {
            Kind = kind;
            Id = id;
            Gold = gold;
        }

        /// <summary>LevelUp, Evolve or Gold.</summary>
        public CardKind Kind { get; }
        public string Id { get; }
        public long Gold { get; }
    }

    /// <summary>
    /// Chest from an elite or a middle boss (SPEC W15; PLAN P12): 1, 3 or 5 upgrades by the given odds.
    /// Each upgrade is an eligible evolution (first in data order, it has priority) or +1 level of a random
    /// owned skill; with nothing left to upgrade it gives gold. The player does not choose.
    /// </summary>
    public sealed class RunChest
    {
        private static readonly int[] Counts = { 1, 3, 5 };
        private readonly IRandom _random;
        private readonly IReadOnlyList<double> _odds;
        private readonly long _goldPerEmptyUpgrade;

        /// <param name="odds">Chance of 1, 3 and 5 upgrades; default 0.70 / 0.25 / 0.05.</param>
        public RunChest(IRandom random, IReadOnlyList<double> odds, long goldPerEmptyUpgrade)
        {
            _random = random ?? throw new ArgumentNullException(nameof(random));
            if (odds == null || odds.Count != Counts.Length || odds.Any(o => o < 0) || Math.Abs(odds.Sum() - 1) > 1e-9)
            {
                throw new ArgumentException("Three odds summing to 1 are required.", nameof(odds));
            }
            _odds = odds;
            _goldPerEmptyUpgrade = goldPerEmptyUpgrade;
        }

        public static IReadOnlyList<double> DefaultOdds => new[] { 0.70, 0.25, 0.05 };

        public int RollCount()
        {
            var roll = _random.NextDouble();
            for (var i = 0; i < Counts.Length; i++)
            {
                roll -= _odds[i];
                if (roll < 0) return Counts[i];
            }
            return Counts[Counts.Length - 1];
        }

        /// <summary>Rolls the count and applies every upgrade to the loadout.</summary>
        public IReadOnlyList<ChestUpgrade> Open(Loadout loadout)
        {
            var count = RollCount();
            var result = new List<ChestUpgrade>();
            for (var i = 0; i < count; i++) result.Add(ApplyOne(loadout));
            return result;
        }

        private ChestUpgrade ApplyOne(Loadout loadout)
        {
            var evolution = loadout.EligibleEvolutions.FirstOrDefault();
            if (evolution != null)
            {
                CardDraw.Apply(loadout, Card.Evolve(evolution.Id));
                return new ChestUpgrade(CardKind.Evolve, evolution.Id, 0);
            }

            var upgradable = loadout.Upgradable.ToList();
            if (upgradable.Count == 0) return new ChestUpgrade(CardKind.Gold, null, _goldPerEmptyUpgrade);

            var pick = upgradable[_random.Next(0, upgradable.Count)];
            CardDraw.Apply(loadout, Card.LevelUp(pick.SkillId, pick.Level + 1));
            return new ChestUpgrade(CardKind.LevelUp, pick.SkillId, 0);
        }
    }
}
