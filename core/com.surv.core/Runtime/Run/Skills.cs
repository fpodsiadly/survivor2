using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Run
{
    public enum SkillKind
    {
        Active,
        Passive,
    }

    /// <summary>A skill that can sit in a run slot (SPEC W16). Weapon skills only enter through active slot 1.</summary>
    public sealed class SkillDefinition
    {
        public SkillDefinition(string id, SkillKind kind, bool isWeaponSkill = false, double cardWeight = 1)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Skill id is required.", nameof(id));
            if (isWeaponSkill && kind != SkillKind.Active) throw new ArgumentException("A weapon skill is active.", nameof(isWeaponSkill));
            if (cardWeight <= 0) throw new ArgumentOutOfRangeException(nameof(cardWeight));
            Id = id;
            Kind = kind;
            IsWeaponSkill = isWeaponSkill;
            CardWeight = cardWeight;
        }

        public string Id { get; }
        public SkillKind Kind { get; }
        public bool IsWeaponSkill { get; }

        /// <summary>Draw weight on level-up cards (SPEC W15: default 1).</summary>
        public double CardWeight { get; }
    }

    /// <summary>
    /// An evolution (SPEC W17): <see cref="BaseActiveId"/> at max level plus <see cref="PartnerId"/>.
    /// A passive partner may be at any level; an active partner must also be at max level and its slot is freed.
    /// </summary>
    public sealed class EvolutionDefinition
    {
        public EvolutionDefinition(string id, string baseActiveId, string partnerId)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("Evolution id is required.", nameof(id));
            if (string.IsNullOrEmpty(baseActiveId)) throw new ArgumentException("Base skill is required.", nameof(baseActiveId));
            if (string.IsNullOrEmpty(partnerId)) throw new ArgumentException("Partner skill is required.", nameof(partnerId));
            Id = id;
            BaseActiveId = baseActiveId;
            PartnerId = partnerId;
        }

        public string Id { get; }
        public string BaseActiveId { get; }
        public string PartnerId { get; }
    }

    public sealed class SkillCatalog
    {
        public const int MaxLevel = 5;
        private readonly Dictionary<string, SkillDefinition> _skills;

        public SkillCatalog(IEnumerable<SkillDefinition> skills, IEnumerable<EvolutionDefinition> evolutions)
        {
            _skills = new Dictionary<string, SkillDefinition>();
            foreach (var skill in skills)
            {
                if (_skills.ContainsKey(skill.Id)) throw new ArgumentException($"Duplicate skill id '{skill.Id}'.");
                _skills.Add(skill.Id, skill);
            }
            Evolutions = evolutions.ToList();
            foreach (var evolution in Evolutions)
            {
                var baseSkill = Get(evolution.BaseActiveId);
                if (baseSkill.Kind != SkillKind.Active) throw new ArgumentException($"Evolution '{evolution.Id}' must start from an active skill.");
                Get(evolution.PartnerId);
            }
        }

        /// <summary>In data order; the chest takes the first eligible one (SPEC W17).</summary>
        public IReadOnlyList<EvolutionDefinition> Evolutions { get; }

        public IEnumerable<SkillDefinition> All => _skills.Values;

        public SkillDefinition Get(string id)
        {
            if (!_skills.TryGetValue(id, out var skill)) throw new KeyNotFoundException($"Unknown skill '{id}'.");
            return skill;
        }

        public EvolutionDefinition GetEvolution(string id) =>
            Evolutions.FirstOrDefault(e => e.Id == id) ?? throw new KeyNotFoundException($"Unknown evolution '{id}'.");
    }
}
