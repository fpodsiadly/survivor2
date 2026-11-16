using System;
using System.Collections.Generic;
using System.Linq;
using Surv.Core.Common;

namespace Surv.Core.Run
{
    public enum CardKind
    {
        NewSkill,
        LevelUp,
        Evolve,
        Gold,
    }

    public sealed class Card
    {
        private Card(CardKind kind, string id, int levelAfter, long gold)
        {
            Kind = kind;
            Id = id;
            LevelAfter = levelAfter;
            Gold = gold;
        }

        public CardKind Kind { get; }

        /// <summary>Skill id for NewSkill/LevelUp, evolution id for Evolve, null for Gold.</summary>
        public string Id { get; }

        /// <summary>Star level shown on the card (SPEC W15).</summary>
        public int LevelAfter { get; }

        public long Gold { get; }

        public static Card NewSkill(string skillId) => new Card(CardKind.NewSkill, skillId, 1, 0);
        public static Card LevelUp(string skillId, int levelAfter) => new Card(CardKind.LevelUp, skillId, levelAfter, 0);
        public static Card Evolve(string evolutionId) => new Card(CardKind.Evolve, evolutionId, 0, 0);
        public static Card GoldCard(long gold) => new Card(CardKind.Gold, null, 0, gold);

        public override string ToString() => $"{Kind} {Id} {(Kind == CardKind.Gold ? Gold.ToString() : LevelAfter.ToString())}";
    }

    /// <summary>Level-up card offer and application (SPEC W15, W16; PLAN P9).</summary>
    public sealed class CardDraw
    {
        public const int CardsPerLevelUp = 3;
        private readonly IRandom _random;
        private readonly long _goldWhenNothingLeft;

        public CardDraw(IRandom random, long goldWhenNothingLeft)
        {
            _random = random ?? throw new ArgumentNullException(nameof(random));
            _goldWhenNothingLeft = goldWhenNothingLeft;
        }

        /// <summary>
        /// Up to 3 different cards. Eligible evolutions are always included (a random subset when more than 3);
        /// the rest is drawn by weight without repeats. Nothing available → one gold card.
        /// </summary>
        public IReadOnlyList<Card> Offer(Loadout loadout)
        {
            var cards = new List<Card>();
            var evolutions = loadout.EligibleEvolutions.Select(e => Card.Evolve(e.Id)).ToList();
            cards.AddRange(TakeRandom(evolutions, CardsPerLevelUp));

            var pool = BuildPool(loadout);
            while (cards.Count < CardsPerLevelUp && pool.Count > 0)
            {
                var index = PickWeighted(pool.Select(p => p.weight).ToList());
                cards.Add(pool[index].card);
                pool.RemoveAt(index);
            }

            if (cards.Count == 0) cards.Add(Card.GoldCard(_goldWhenNothingLeft));
            return cards;
        }

        /// <summary>Applies the chosen card; returns gold earned (non-zero only for a gold card).</summary>
        public static long Apply(Loadout loadout, Card card)
        {
            switch (card.Kind)
            {
                case CardKind.NewSkill:
                    loadout.AddNew(card.Id);
                    return 0;
                case CardKind.LevelUp:
                    loadout.LevelUp(card.Id);
                    return 0;
                case CardKind.Evolve:
                    loadout.Evolve(loadout.Catalog.GetEvolution(card.Id));
                    return 0;
                case CardKind.Gold:
                    return card.Gold;
                default:
                    throw new ArgumentOutOfRangeException(nameof(card));
            }
        }

        private List<(Card card, double weight)> BuildPool(Loadout loadout)
        {
            var pool = new List<(Card, double)>();
            foreach (var skill in loadout.Catalog.All)
            {
                var owned = loadout.Find(skill.Id);
                if (owned == null)
                {
                    if (!skill.IsWeaponSkill && loadout.HasFreeSlot(skill.Kind)) pool.Add((Card.NewSkill(skill.Id), skill.CardWeight));
                }
                else if (!owned.IsEvolved && owned.Level < SkillCatalog.MaxLevel)
                {
                    pool.Add((Card.LevelUp(skill.Id, owned.Level + 1), skill.CardWeight));
                }
            }
            return pool;
        }

        private List<Card> TakeRandom(List<Card> source, int count)
        {
            var copy = source.ToList();
            var taken = new List<Card>();
            while (taken.Count < count && copy.Count > 0)
            {
                var index = _random.Next(0, copy.Count);
                taken.Add(copy[index]);
                copy.RemoveAt(index);
            }
            return taken;
        }

        private int PickWeighted(IReadOnlyList<double> weights)
        {
            var total = weights.Sum();
            var roll = _random.NextDouble() * total;
            for (var i = 0; i < weights.Count; i++)
            {
                roll -= weights[i];
                if (roll < 0) return i;
            }
            return weights.Count - 1;
        }
    }
}
