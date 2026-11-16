using System;
using System.Collections.Generic;
using System.Linq;

namespace Surv.Core.Run
{
    /// <summary>A skill in a run slot. <see cref="EvolutionId"/> is set once the active skill has evolved.</summary>
    public sealed class OwnedSkill
    {
        internal OwnedSkill(string skillId, int level)
        {
            SkillId = skillId;
            Level = level;
        }

        public string SkillId { get; }
        public int Level { get; internal set; }
        public string EvolutionId { get; internal set; }
        public bool IsEvolved => EvolutionId != null;
    }

    /// <summary>The 6 active + 6 passive slots of one run (SPEC W16). Active slot 1 holds the weapon skill.</summary>
    public sealed class Loadout
    {
        public const int ActiveSlots = 6;
        public const int PassiveSlots = 6;

        private readonly List<OwnedSkill> _actives = new List<OwnedSkill>();
        private readonly List<OwnedSkill> _passives = new List<OwnedSkill>();
        private readonly SkillCatalog _catalog;

        /// <param name="weaponSkillLevel">1, or 2 with the Excellent weapon bonus (SPEC W24).</param>
        public Loadout(SkillCatalog catalog, string weaponSkillId, int weaponSkillLevel = 1)
        {
            _catalog = catalog ?? throw new ArgumentNullException(nameof(catalog));
            var weapon = catalog.Get(weaponSkillId);
            if (!weapon.IsWeaponSkill) throw new ArgumentException($"'{weaponSkillId}' is not a weapon skill.", nameof(weaponSkillId));
            if (weaponSkillLevel < 1 || weaponSkillLevel > SkillCatalog.MaxLevel) throw new ArgumentOutOfRangeException(nameof(weaponSkillLevel));
            _actives.Add(new OwnedSkill(weaponSkillId, weaponSkillLevel));
        }

        public IReadOnlyList<OwnedSkill> Actives => _actives;
        public IReadOnlyList<OwnedSkill> Passives => _passives;
        public SkillCatalog Catalog => _catalog;

        public OwnedSkill Find(string skillId) =>
            _actives.FirstOrDefault(s => s.SkillId == skillId) ?? _passives.FirstOrDefault(s => s.SkillId == skillId);

        public bool Owns(string skillId) => Find(skillId) != null;

        public bool HasFreeSlot(SkillKind kind) =>
            kind == SkillKind.Active ? _actives.Count < ActiveSlots : _passives.Count < PassiveSlots;

        internal void AddNew(string skillId)
        {
            var skill = _catalog.Get(skillId);
            if (skill.IsWeaponSkill) throw new InvalidOperationException("Weapon skills only enter through slot 1.");
            if (Owns(skillId)) throw new InvalidOperationException($"'{skillId}' is already owned.");
            if (!HasFreeSlot(skill.Kind)) throw new InvalidOperationException($"No free {skill.Kind} slot.");
            (skill.Kind == SkillKind.Active ? _actives : _passives).Add(new OwnedSkill(skillId, 1));
        }

        internal void LevelUp(string skillId)
        {
            var owned = Find(skillId) ?? throw new InvalidOperationException($"'{skillId}' is not owned.");
            if (owned.IsEvolved || owned.Level >= SkillCatalog.MaxLevel) throw new InvalidOperationException($"'{skillId}' cannot level up.");
            owned.Level++;
        }

        /// <summary>True when the evolution's conditions hold and it has not happened yet (SPEC W17).</summary>
        public bool CanEvolve(EvolutionDefinition evolution)
        {
            var baseSkill = _actives.FirstOrDefault(s => s.SkillId == evolution.BaseActiveId);
            if (baseSkill == null || baseSkill.IsEvolved || baseSkill.Level < SkillCatalog.MaxLevel) return false;
            var partner = Find(evolution.PartnerId);
            if (partner == null) return false;
            if (_catalog.Get(evolution.PartnerId).Kind == SkillKind.Passive) return true;
            return !partner.IsEvolved && partner.Level >= SkillCatalog.MaxLevel;
        }

        internal void Evolve(EvolutionDefinition evolution)
        {
            if (!CanEvolve(evolution)) throw new InvalidOperationException($"Cannot evolve into '{evolution.Id}'.");
            var baseSkill = _actives.First(s => s.SkillId == evolution.BaseActiveId);
            baseSkill.EvolutionId = evolution.Id;
            if (_catalog.Get(evolution.PartnerId).Kind == SkillKind.Active)
            {
                _actives.RemoveAll(s => s.SkillId == evolution.PartnerId);
            }
        }

        /// <summary>Owned skills that can still gain a level (not evolved, below max).</summary>
        public IEnumerable<OwnedSkill> Upgradable => _actives.Concat(_passives).Where(s => !s.IsEvolved && s.Level < SkillCatalog.MaxLevel);

        public IEnumerable<EvolutionDefinition> EligibleEvolutions => _catalog.Evolutions.Where(CanEvolve);
    }
}
