package com.surv20.game.core.equipment

enum class IngredientMatch {
    // Same item type as the main item ("identyczny").
    SAME_TYPE,

    // Any item type of the main item's slot ("dowolny").
    ANY_TYPE_SAME_SLOT,
}

// What a main item of a given tier needs to merge into the next tier.
data class MergeRecipe(val ingredientCount: Int, val ingredientTier: Tier, val match: IngredientMatch)

// The merge table of SPEC W23 (RESEARCH §5.3). Ingredients are on a base tier except Excellent+2.
object MergeRules {
    fun recipeFor(mainTier: Tier): MergeRecipe? = when (mainTier) {
        Tier.NORMAL, Tier.GOOD, Tier.BETTER -> MergeRecipe(2, mainTier, IngredientMatch.SAME_TYPE)
        Tier.EXCELLENT -> MergeRecipe(1, Tier.EXCELLENT, IngredientMatch.ANY_TYPE_SAME_SLOT)
        Tier.EXCELLENT_1 -> MergeRecipe(2, Tier.EXCELLENT, IngredientMatch.ANY_TYPE_SAME_SLOT)
        Tier.EXCELLENT_2 -> MergeRecipe(1, Tier.EXCELLENT_2, IngredientMatch.SAME_TYPE)
        Tier.EPIC, Tier.EPIC_1, Tier.EPIC_2 -> MergeRecipe(1, Tier.EPIC, IngredientMatch.ANY_TYPE_SAME_SLOT)
        Tier.EPIC_3 -> MergeRecipe(2, Tier.EPIC, IngredientMatch.SAME_TYPE)
        Tier.LEGEND, Tier.LEGEND_1, Tier.LEGEND_2, Tier.LEGEND_3 ->
            MergeRecipe(1, Tier.LEGEND, IngredientMatch.ANY_TYPE_SAME_SLOT)
        Tier.LEGEND_4 -> null
    }
}

enum class MergeError {
    NONE, MAIN_NOT_FOUND, MAIN_AT_MAX_TIER, WRONG_INGREDIENT_COUNT, INGREDIENT_NOT_FOUND, INGREDIENT_IS_MAIN,
    DUPLICATE_INGREDIENT, INGREDIENT_EQUIPPED, INGREDIENT_WRONG_SLOT, INGREDIENT_WRONG_TYPE, INGREDIENT_WRONG_TIER,
}

data class MergeOutcome(
    val error: MergeError,
    val result: Item? = null,
    val refundedGold: Long = 0,
    val refundedPlans: Long = 0,
) {
    val succeeded: Boolean get() = error == MergeError.NONE
}

enum class UpgradeError { NONE, ITEM_NOT_FOUND, AT_LEVEL_CAP, NOT_ENOUGH_RESOURCES }

// Item levelling, "upgrade max" and reset (SPEC W25).
class UpgradeService(private val balance: EquipmentBalance = EquipmentBalance()) {
    // Gold and plans spent to bring an item from level 1 to the given level.
    fun investedUpTo(level: Int): Pair<Long, Long> {
        var gold = 0L
        var plans = 0L
        for (l in 1 until level) {
            gold += balance.goldCostForLevel(l)
            plans += balance.planCostForLevel(l)
        }
        return gold to plans
    }

    fun upgrade(inventory: Inventory, uid: Int): UpgradeError {
        val item = inventory.find(uid) ?: return UpgradeError.ITEM_NOT_FOUND
        if (item.level >= balance.levelCap(item.tier)) return UpgradeError.AT_LEVEL_CAP
        val slot = inventory.slotOf(item)
        val gold = balance.goldCostForLevel(item.level)
        val plans = balance.planCostForLevel(item.level)
        if (!inventory.canAfford(gold, slot, plans)) return UpgradeError.NOT_ENOUGH_RESOURCES
        inventory.spend(gold, slot, plans)
        inventory.replace(item.copy(level = item.level + 1))
        return UpgradeError.NONE
    }

    // Upgrades while affordable and below the cap; returns the levels gained.
    fun upgradeMax(inventory: Inventory, uid: Int): Int {
        var gained = 0
        while (upgrade(inventory, uid) == UpgradeError.NONE) gained++
        return gained
    }

    // Back to level 1 with 100% of the gold and plans refunded.
    fun reset(inventory: Inventory, uid: Int): Boolean {
        val item = inventory.find(uid) ?: return false
        val (gold, plans) = investedUpTo(item.level)
        inventory.addGold(gold)
        inventory.addPlans(inventory.slotOf(item), plans)
        inventory.replace(item.copy(level = 1))
        return true
    }
}

// Merges per SPEC W23: the main item keeps uid, type, level and equipped state and moves one tier up;
// ingredients are consumed and the gold and plans spent on upgrading them are refunded.
class MergeService(private val upgrades: UpgradeService) {
    fun validate(inventory: Inventory, mainUid: Int, ingredientUids: List<Int>): MergeError {
        val main = inventory.find(mainUid) ?: return MergeError.MAIN_NOT_FOUND
        val recipe = MergeRules.recipeFor(main.tier) ?: return MergeError.MAIN_AT_MAX_TIER
        if (ingredientUids.size != recipe.ingredientCount) return MergeError.WRONG_INGREDIENT_COUNT
        if (ingredientUids.distinct().size != ingredientUids.size) return MergeError.DUPLICATE_INGREDIENT
        for (uid in ingredientUids) {
            val error = checkIngredient(inventory, main, recipe, uid)
            if (error != MergeError.NONE) return error
        }
        return MergeError.NONE
    }

    fun merge(inventory: Inventory, mainUid: Int, ingredientUids: List<Int>): MergeOutcome {
        val error = validate(inventory, mainUid, ingredientUids)
        if (error != MergeError.NONE) return MergeOutcome(error)

        val main = inventory.get(mainUid)
        val slot = inventory.slotOf(main)
        var gold = 0L
        var plans = 0L
        for (uid in ingredientUids) {
            val (g, p) = upgrades.investedUpTo(inventory.get(uid).level)
            gold += g
            plans += p
            inventory.remove(uid)
        }
        inventory.addGold(gold)
        inventory.addPlans(slot, plans)
        val result = main.copy(tier = main.tier.next())
        inventory.replace(result)
        return MergeOutcome(MergeError.NONE, result, gold, plans)
    }

    // Items that may be used as ingredients for the main item (UI highlighting).
    fun candidates(inventory: Inventory, mainUid: Int): List<Item> {
        val main = inventory.find(mainUid) ?: return emptyList()
        val recipe = MergeRules.recipeFor(main.tier) ?: return emptyList()
        return inventory.items
            .filter { checkIngredient(inventory, main, recipe, it.uid) == MergeError.NONE }
            .sortedWith(compareBy<Item> { it.level }.thenBy { it.uid })
    }

    private fun checkIngredient(inventory: Inventory, main: Item, recipe: MergeRecipe, uid: Int): MergeError {
        if (uid == main.uid) return MergeError.INGREDIENT_IS_MAIN
        val ingredient = inventory.find(uid) ?: return MergeError.INGREDIENT_NOT_FOUND
        if (inventory.isEquipped(uid)) return MergeError.INGREDIENT_EQUIPPED
        if (inventory.slotOf(ingredient) != inventory.slotOf(main)) return MergeError.INGREDIENT_WRONG_SLOT
        if (recipe.match == IngredientMatch.SAME_TYPE && ingredient.typeId != main.typeId) return MergeError.INGREDIENT_WRONG_TYPE
        if (ingredient.tier != recipe.ingredientTier) return MergeError.INGREDIENT_WRONG_TIER
        return MergeError.NONE
    }
}

// "Połącz wszystko" (SPEC W23): 3 identical items on Normal, Good and Better, chained until nothing is left.
// Main item: the equipped one if in the group, otherwise the highest level, then the lowest uid (PLAN P20).
// Ingredients are the lowest-level remaining items, so the smallest refunds are paid.
class MergeAllService(private val merge: MergeService) {
    fun mergeAll(inventory: Inventory): List<MergeOutcome> {
        val done = mutableListOf<MergeOutcome>()
        var mergedInPass: Boolean
        do {
            mergedInPass = false
            for (tier in AUTO_TIERS) {
                val groups = inventory.items.filter { it.tier == tier }.groupBy { it.typeId }.values
                    .map { it.toMutableList() }.filter { it.size >= 3 }
                for (group in groups) {
                    while (group.size >= 3) {
                        val main = group.sortedWith(
                            compareByDescending<Item> { inventory.isEquipped(it.uid) }
                                .thenByDescending { it.level }
                                .thenBy { it.uid },
                        ).first()
                        val ingredients = group.filter { it.uid != main.uid }
                            .sortedWith(compareBy<Item> { it.level }.thenBy { it.uid })
                            .take(2).map { it.uid }
                        val outcome = merge.merge(inventory, main.uid, ingredients)
                        check(outcome.succeeded) { "Merge all produced an invalid merge: ${outcome.error}." }
                        done += outcome
                        mergedInPass = true
                        group.removeAll { it.uid == main.uid || it.uid in ingredients }
                    }
                }
            }
        } while (mergedInPass)
        return done
    }

    private companion object {
        val AUTO_TIERS = listOf(Tier.NORMAL, Tier.GOOD, Tier.BETTER)
    }
}

// Main stat and unlocked bonuses of an item (SPEC W24).
class ItemStats(private val balance: EquipmentBalance, private val catalog: ItemCatalog) {
    fun mainStatKind(item: Item): StatKind = catalog.get(item.typeId).slot.mainStat

    // Tier base (ATK scale, × multiplier for HP slots) plus a fixed amount per level above 1.
    fun mainStat(item: Item): Double {
        val tierBase = balance.weaponBaseStat[item.tier.ordinal]
        val levels = item.level - 1
        return when (mainStatKind(item)) {
            StatKind.ATTACK -> tierBase + levels * balance.attackPerLevel
            StatKind.HEALTH -> tierBase * balance.healthSlotMultiplier + levels * balance.healthPerLevel
        }
    }

    fun unlockedBonuses(item: Item): List<TierBonus> = catalog.get(item.typeId).bonuses.filter { item.tier >= it.minTier }

    // All bonuses with a flag telling whether each one already works (locked ones are greyed out).
    fun bonusesWithState(item: Item): List<Pair<TierBonus, Boolean>> =
        catalog.get(item.typeId).bonuses.map { it to (item.tier >= it.minTier) }
}
