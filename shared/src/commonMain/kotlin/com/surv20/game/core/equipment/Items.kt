package com.surv20.game.core.equipment

import com.surv20.game.core.Balance

// The 15-step rarity ladder (SPEC W23).
enum class Tier {
    NORMAL, GOOD, BETTER,
    EXCELLENT, EXCELLENT_1, EXCELLENT_2,
    EPIC, EPIC_1, EPIC_2, EPIC_3,
    LEGEND, LEGEND_1, LEGEND_2, LEGEND_3, LEGEND_4;

    // The six base tiers own a frame colour; "+N" sub-tiers share it and show N marks.
    val base: BaseTier
        get() = when (this) {
            NORMAL -> BaseTier.NORMAL
            GOOD -> BaseTier.GOOD
            BETTER -> BaseTier.BETTER
            EXCELLENT, EXCELLENT_1, EXCELLENT_2 -> BaseTier.EXCELLENT
            EPIC, EPIC_1, EPIC_2, EPIC_3 -> BaseTier.EPIC
            LEGEND, LEGEND_1, LEGEND_2, LEGEND_3, LEGEND_4 -> BaseTier.LEGEND
        }

    val plusMarks: Int get() = ordinal - base.tier.ordinal
    val isMax: Boolean get() = this == LEGEND_4

    fun next(): Tier {
        check(!isMax) { "Legend+4 is the highest tier." }
        return entries[ordinal + 1]
    }
}

enum class BaseTier(val tier: Tier) {
    NORMAL(Tier.NORMAL), GOOD(Tier.GOOD), BETTER(Tier.BETTER),
    EXCELLENT(Tier.EXCELLENT), EPIC(Tier.EPIC), LEGEND(Tier.LEGEND),
}

// Six slots; weapon, necklace and gloves give ATK, the rest give HP (SPEC W22).
enum class Slot {
    WEAPON, NECKLACE, GLOVES, ARMOR, BELT, BOOTS;

    val mainStat: StatKind
        get() = if (this == WEAPON || this == NECKLACE || this == GLOVES) StatKind.ATTACK else StatKind.HEALTH
}

enum class StatKind { ATTACK, HEALTH }

// A bonus of an item type that starts working from minTier (SPEC W24).
data class TierBonus(val minTier: Tier, val bonusId: String) {
    init {
        require(bonusId.isNotEmpty()) { "Bonus id is required." }
    }
}

// Definition of an item type, e.g. one weapon or one belt design (SPEC W22).
data class ItemType(
    val id: String,
    val slot: Slot,
    val bonuses: List<TierBonus> = emptyList(),
    // Skill a weapon of this type puts in active slot 1; null for other slots.
    val weaponSkillId: String? = null,
) {
    init {
        require(id.isNotEmpty()) { "Item type id is required." }
        require(slot != Slot.WEAPON || !weaponSkillId.isNullOrEmpty()) { "A weapon type must name its skill." }
    }
}

// An owned item. Immutable: merges and upgrades replace it in the Inventory.
data class Item(val uid: Int, val typeId: String, val tier: Tier, val level: Int) {
    init {
        require(uid > 0) { "Uid must be positive." }
        require(typeId.isNotEmpty()) { "Type id is required." }
        require(level >= 1) { "Level starts at 1." }
    }
}

class ItemCatalog(types: List<ItemType>) {
    private val byId: Map<String, ItemType>

    init {
        val map = LinkedHashMap<String, ItemType>()
        types.forEach { require(map.put(it.id, it) == null) { "Duplicate item type id '${it.id}'." } }
        byId = map
    }

    val all: Collection<ItemType> get() = byId.values

    fun get(typeId: String): ItemType = byId[typeId] ?: throw NoSuchElementException("Unknown item type '$typeId'.")
    fun contains(typeId: String): Boolean = typeId in byId
}

// Tunable equipment numbers; defaults come from Balance (SPEC W24, W25, W41).
class EquipmentBalance(
    val levelCaps: List<Int> = Balance.LEVEL_CAPS,
    val weaponBaseStat: List<Double> = Balance.WEAPON_BASE_STAT,
    val healthSlotMultiplier: Double = Balance.HEALTH_SLOT_MULTIPLIER,
    val attackPerLevel: Double = Balance.ATTACK_PER_LEVEL,
    val healthPerLevel: Double = Balance.HEALTH_PER_LEVEL,
    // Cost to go from the given level to the next.
    val goldCostForLevel: (Int) -> Long = Balance::upgradeGold,
    val planCostForLevel: (Int) -> Long = Balance::upgradePlans,
) {
    init {
        require(levelCaps.size == TIER_COUNT) { "One level cap per tier is required." }
        require(weaponBaseStat.size == TIER_COUNT) { "One base stat per tier is required." }
        require(levelCaps.zipWithNext().all { (a, b) -> b >= a }) { "Level caps must not decrease with tier." }
    }

    fun levelCap(tier: Tier): Int = levelCaps[tier.ordinal]

    companion object {
        val TIER_COUNT = Tier.entries.size
    }
}
