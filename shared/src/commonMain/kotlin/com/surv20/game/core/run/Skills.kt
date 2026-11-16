package com.surv20.game.core.run

import com.surv20.game.core.Balance
import com.surv20.game.core.GameRandom

enum class SkillKind { ACTIVE, PASSIVE }

// A skill that can sit in a run slot (SPEC W16). Weapon skills only enter through active slot 1.
data class SkillDefinition(
    val id: String,
    val kind: SkillKind,
    val isWeaponSkill: Boolean = false,
    // Draw weight on level-up cards (SPEC W15: default 1).
    val cardWeight: Double = 1.0,
) {
    init {
        require(id.isNotEmpty()) { "Skill id is required." }
        require(!isWeaponSkill || kind == SkillKind.ACTIVE) { "A weapon skill is active." }
        require(cardWeight > 0)
    }
}

// An evolution (SPEC W17): baseActiveId at max level plus partnerId. A passive partner may be at any level;
// an active partner must also be at max level and its slot is freed.
data class EvolutionDefinition(val id: String, val baseActiveId: String, val partnerId: String)

class SkillCatalog(skills: List<SkillDefinition>, evolutions: List<EvolutionDefinition>) {
    private val byId: Map<String, SkillDefinition>

    // In data order; the chest takes the first eligible one (SPEC W17).
    val evolutions: List<EvolutionDefinition> = evolutions.toList()
    val all: Collection<SkillDefinition> get() = byId.values

    init {
        val map = LinkedHashMap<String, SkillDefinition>()
        skills.forEach { require(map.put(it.id, it) == null) { "Duplicate skill id '${it.id}'." } }
        byId = map
        evolutions.forEach {
            require(get(it.baseActiveId).kind == SkillKind.ACTIVE) { "Evolution '${it.id}' must start from an active skill." }
            get(it.partnerId)
        }
    }

    fun get(id: String): SkillDefinition = byId[id] ?: throw NoSuchElementException("Unknown skill '$id'.")
    fun evolution(id: String): EvolutionDefinition =
        evolutions.firstOrNull { it.id == id } ?: throw NoSuchElementException("Unknown evolution '$id'.")

    companion object {
        const val MAX_LEVEL = 5
    }
}

// A skill in a run slot; evolutionId is set once the active skill has evolved.
class OwnedSkill internal constructor(val skillId: String, level: Int) {
    var level: Int = level
        internal set
    var evolutionId: String? = null
        internal set
    val isEvolved: Boolean get() = evolutionId != null
}

// The 6 active + 6 passive slots of one run (SPEC W16). Active slot 1 holds the weapon skill.
class Loadout(val catalog: SkillCatalog, weaponSkillId: String, weaponSkillLevel: Int = 1) {
    private val activeList = mutableListOf<OwnedSkill>()
    private val passiveList = mutableListOf<OwnedSkill>()

    val actives: List<OwnedSkill> get() = activeList
    val passives: List<OwnedSkill> get() = passiveList

    init {
        require(catalog.get(weaponSkillId).isWeaponSkill) { "'$weaponSkillId' is not a weapon skill." }
        // Level 2 with the Excellent weapon bonus (SPEC W24).
        require(weaponSkillLevel in 1..SkillCatalog.MAX_LEVEL)
        activeList += OwnedSkill(weaponSkillId, weaponSkillLevel)
    }

    fun find(skillId: String): OwnedSkill? = activeList.firstOrNull { it.skillId == skillId } ?: passiveList.firstOrNull { it.skillId == skillId }
    fun owns(skillId: String): Boolean = find(skillId) != null
    fun hasFreeSlot(kind: SkillKind): Boolean =
        if (kind == SkillKind.ACTIVE) activeList.size < ACTIVE_SLOTS else passiveList.size < PASSIVE_SLOTS

    internal fun addNew(skillId: String) {
        val skill = catalog.get(skillId)
        check(!skill.isWeaponSkill) { "Weapon skills only enter through slot 1." }
        check(!owns(skillId)) { "'$skillId' is already owned." }
        check(hasFreeSlot(skill.kind)) { "No free ${skill.kind} slot." }
        (if (skill.kind == SkillKind.ACTIVE) activeList else passiveList) += OwnedSkill(skillId, 1)
    }

    internal fun levelUp(skillId: String) {
        val owned = checkNotNull(find(skillId)) { "'$skillId' is not owned." }
        check(!owned.isEvolved && owned.level < SkillCatalog.MAX_LEVEL) { "'$skillId' cannot level up." }
        owned.level++
    }

    // True when the evolution's conditions hold and it has not happened yet (SPEC W17).
    fun canEvolve(evolution: EvolutionDefinition): Boolean {
        val base = activeList.firstOrNull { it.skillId == evolution.baseActiveId } ?: return false
        if (base.isEvolved || base.level < SkillCatalog.MAX_LEVEL) return false
        val partner = find(evolution.partnerId) ?: return false
        if (catalog.get(evolution.partnerId).kind == SkillKind.PASSIVE) return true
        return !partner.isEvolved && partner.level >= SkillCatalog.MAX_LEVEL
    }

    internal fun evolve(evolution: EvolutionDefinition) {
        check(canEvolve(evolution)) { "Cannot evolve into '${evolution.id}'." }
        activeList.first { it.skillId == evolution.baseActiveId }.evolutionId = evolution.id
        if (catalog.get(evolution.partnerId).kind == SkillKind.ACTIVE) activeList.removeAll { it.skillId == evolution.partnerId }
    }

    // Owned skills that can still gain a level.
    val upgradable: List<OwnedSkill> get() = (activeList + passiveList).filter { !it.isEvolved && it.level < SkillCatalog.MAX_LEVEL }
    val eligibleEvolutions: List<EvolutionDefinition> get() = catalog.evolutions.filter { canEvolve(it) }

    companion object {
        const val ACTIVE_SLOTS = 6
        const val PASSIVE_SLOTS = 6
    }
}

enum class CardKind { NEW_SKILL, LEVEL_UP, EVOLVE, GOLD }

// One level-up card. id: skill id (NEW_SKILL/LEVEL_UP), evolution id (EVOLVE), null (GOLD).
data class Card(val kind: CardKind, val id: String?, val levelAfter: Int = 0, val gold: Long = 0) {
    companion object {
        fun newSkill(skillId: String) = Card(CardKind.NEW_SKILL, skillId, 1)
        fun levelUp(skillId: String, levelAfter: Int) = Card(CardKind.LEVEL_UP, skillId, levelAfter)
        fun evolve(evolutionId: String) = Card(CardKind.EVOLVE, evolutionId)
        fun gold(amount: Long) = Card(CardKind.GOLD, null, gold = amount)
    }
}

// Level-up card offer and application (SPEC W15, W16; PLAN P9).
class CardDraw(private val random: GameRandom, private val goldWhenNothingLeft: Long = Balance.GOLD_WHEN_NO_CARD) {
    // Up to 3 different cards. Eligible evolutions are always included (a random subset when more than 3);
    // the rest is drawn by weight without repeats. Nothing available → one gold card.
    fun offer(loadout: Loadout): List<Card> {
        val cards = takeRandom(loadout.eligibleEvolutions.map { Card.evolve(it.id) }, CARDS_PER_LEVEL_UP).toMutableList()
        val pool = buildPool(loadout)
        while (cards.size < CARDS_PER_LEVEL_UP && pool.isNotEmpty()) {
            val index = pickWeighted(pool.map { it.second })
            cards += pool.removeAt(index).first
        }
        if (cards.isEmpty()) cards += Card.gold(goldWhenNothingLeft)
        return cards
    }

    private fun buildPool(loadout: Loadout): MutableList<Pair<Card, Double>> {
        val pool = mutableListOf<Pair<Card, Double>>()
        for (skill in loadout.catalog.all) {
            val owned = loadout.find(skill.id)
            if (owned == null) {
                if (!skill.isWeaponSkill && loadout.hasFreeSlot(skill.kind)) pool += Card.newSkill(skill.id) to skill.cardWeight
            } else if (!owned.isEvolved && owned.level < SkillCatalog.MAX_LEVEL) {
                pool += Card.levelUp(skill.id, owned.level + 1) to skill.cardWeight
            }
        }
        return pool
    }

    private fun takeRandom(source: List<Card>, count: Int): List<Card> {
        val copy = source.toMutableList()
        val taken = mutableListOf<Card>()
        while (taken.size < count && copy.isNotEmpty()) taken += copy.removeAt(random.nextInt(0, copy.size))
        return taken
    }

    private fun pickWeighted(weights: List<Double>): Int {
        var roll = random.nextDouble() * weights.sum()
        weights.forEachIndexed { i, w ->
            roll -= w
            if (roll < 0) return i
        }
        return weights.lastIndex
    }

    companion object {
        const val CARDS_PER_LEVEL_UP = 3

        // Applies the chosen card; returns gold earned (non-zero only for a gold card).
        fun apply(loadout: Loadout, card: Card): Long {
            when (card.kind) {
                CardKind.NEW_SKILL -> loadout.addNew(card.id!!)
                CardKind.LEVEL_UP -> loadout.levelUp(card.id!!)
                CardKind.EVOLVE -> loadout.evolve(loadout.catalog.evolution(card.id!!))
                CardKind.GOLD -> return card.gold
            }
            return 0
        }
    }
}

data class ChestUpgrade(val kind: CardKind, val id: String?, val gold: Long = 0)

// Chest from an elite or a middle boss (SPEC W15; PLAN P12): 1, 3 or 5 upgrades by the given odds. Each upgrade is
// an eligible evolution (first in data order, it has priority) or +1 level of a random owned skill; with nothing
// left to upgrade it gives gold. The player does not choose.
class RunChest(
    private val random: GameRandom,
    private val odds: List<Double> = Balance.CHEST_COUNT_ODDS,
    private val goldPerEmptyUpgrade: Long = Balance.GOLD_PER_EMPTY_CHEST_UPGRADE,
) {
    init {
        require(odds.size == COUNTS.size && odds.all { it >= 0 } && kotlin.math.abs(odds.sum() - 1) < 1e-9) {
            "Three odds summing to 1 are required."
        }
    }

    fun rollCount(): Int {
        var roll = random.nextDouble()
        odds.forEachIndexed { i, p ->
            roll -= p
            if (roll < 0) return COUNTS[i]
        }
        return COUNTS.last()
    }

    // Rolls the count and applies every upgrade to the loadout.
    fun open(loadout: Loadout): List<ChestUpgrade> = List(rollCount()) { applyOne(loadout) }

    private fun applyOne(loadout: Loadout): ChestUpgrade {
        loadout.eligibleEvolutions.firstOrNull()?.let {
            CardDraw.apply(loadout, Card.evolve(it.id))
            return ChestUpgrade(CardKind.EVOLVE, it.id)
        }
        val upgradable = loadout.upgradable
        if (upgradable.isEmpty()) return ChestUpgrade(CardKind.GOLD, null, goldPerEmptyUpgrade)
        val pick = upgradable[random.nextInt(0, upgradable.size)]
        CardDraw.apply(loadout, Card.levelUp(pick.skillId, pick.level + 1))
        return ChestUpgrade(CardKind.LEVEL_UP, pick.skillId)
    }

    private companion object {
        val COUNTS = listOf(1, 3, 5)
    }
}

// In-run level and XP bar (SPEC W14).
class ExperienceBar(private val xpToNextLevel: (Int) -> Long = Balance::xpToNextLevel) {
    var level = 1
        private set
    var xp = 0L
        private set
    val xpToNext: Long get() = xpToNextLevel(level)

    // Adds XP carrying the excess over; returns how many level-ups happened (each shows a card choice).
    fun add(amount: Long): Int {
        require(amount >= 0)
        xp += amount
        var ups = 0
        while (xp >= xpToNext) {
            xp -= xpToNext
            level++
            ups++
        }
        return ups
    }
}

enum class CrystalKind { NONE, GREEN, BLUE, YELLOW }

enum class EnemyClass { NORMAL, REINFORCED, ELITE, BOSS }

// Which crystal an enemy leaves (SPEC W14; PLAN P5 reading of "elites always yellow").
class CrystalDrops(private val random: GameRandom, private val yellowChance: Double = Balance.YELLOW_CRYSTAL_CHANCE) {
    init {
        require(yellowChance in 0.0..1.0)
    }

    // Enemies cleared when a boss enters drop nothing (SPEC W14).
    fun drop(enemy: EnemyClass, removedByBossEntry: Boolean = false): CrystalKind = when {
        removedByBossEntry -> CrystalKind.NONE
        enemy == EnemyClass.ELITE || enemy == EnemyClass.BOSS -> CrystalKind.YELLOW
        random.nextDouble() < yellowChance -> CrystalKind.YELLOW
        enemy == EnemyClass.REINFORCED -> CrystalKind.BLUE
        else -> CrystalKind.GREEN
    }

    companion object {
        fun value(kind: CrystalKind): Int = when (kind) {
            CrystalKind.GREEN -> Balance.CRYSTAL_GREEN
            CrystalKind.BLUE -> Balance.CRYSTAL_BLUE
            CrystalKind.YELLOW -> Balance.CRYSTAL_YELLOW
            CrystalKind.NONE -> 0
        }
    }
}
