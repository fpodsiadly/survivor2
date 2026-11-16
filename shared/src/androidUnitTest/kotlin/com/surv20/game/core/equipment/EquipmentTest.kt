package com.surv20.game.core.equipment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal object EquipmentData {
    const val KUNAI = "weapon.thrown"
    const val BLADE = "weapon.blade"
    const val BELT_A = "belt.a"
    const val BELT_B = "belt.b"
    const val BONUS_ATK10 = "atk+10%"
    const val BONUS_SKILL30 = "weaponSkillDamage+30%"
    const val BONUS_START_LV2 = "weaponSkillStartsAtLv2"
    const val BONUS_ATK15 = "atk+15%"
    const val BONUS_LEGEND = "weaponLegendBehaviour"

    // Weapon bonus skeleton from SPEC W24.
    private fun weapon(id: String, skill: String) = ItemType(
        id, Slot.WEAPON,
        listOf(
            TierBonus(Tier.GOOD, BONUS_ATK10), TierBonus(Tier.BETTER, BONUS_SKILL30),
            TierBonus(Tier.EXCELLENT, BONUS_START_LV2), TierBonus(Tier.EPIC, BONUS_ATK15),
            TierBonus(Tier.LEGEND, BONUS_LEGEND),
        ),
        skill,
    )

    fun catalog() = ItemCatalog(
        listOf(
            weapon(KUNAI, "skill.thrown"), weapon(BLADE, "skill.blade"),
            ItemType(BELT_A, Slot.BELT, listOf(TierBonus(Tier.BETTER, "belt.speedAfterMeat"))),
            ItemType(BELT_B, Slot.BELT),
        ),
    )

    class Setup {
        val upgrades = UpgradeService(EquipmentBalance())
        val inv = Inventory(catalog())
        val merge = MergeService(upgrades)
    }
}

class MergeTest {
    private val s = EquipmentData.Setup()
    private val inv = s.inv
    private val merge = s.merge

    @Test
    fun threeIdenticalMergeIntoNextTierForNormalGoodBetter() {
        for ((tier, expected) in listOf(Tier.NORMAL to Tier.GOOD, Tier.GOOD to Tier.BETTER, Tier.BETTER to Tier.EXCELLENT)) {
            val local = EquipmentData.Setup()
            val main = local.inv.add(EquipmentData.KUNAI, tier)
            val a = local.inv.add(EquipmentData.KUNAI, tier)
            val b = local.inv.add(EquipmentData.KUNAI, tier)
            assertTrue(local.merge.merge(local.inv, main.uid, listOf(a.uid, b.uid)).succeeded)
            assertEquals(expected, local.inv.get(main.uid).tier)
            assertEquals(1, local.inv.items.size)
        }
    }

    @Test
    fun lowTiersRejectOtherTypeOfSameSlot() {
        val main = inv.add(EquipmentData.KUNAI, Tier.GOOD)
        val a = inv.add(EquipmentData.KUNAI, Tier.GOOD)
        val other = inv.add(EquipmentData.BLADE, Tier.GOOD)
        assertEquals(MergeError.INGREDIENT_WRONG_TYPE, merge.validate(inv, main.uid, listOf(a.uid, other.uid)))
    }

    @Test
    fun excellentPlusOneTakesAnyExcellentOfTheSlotButNotOtherSlotsOrPlusTiers() {
        val main = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT)
        val belt = inv.add(EquipmentData.BELT_A, Tier.EXCELLENT)
        val plus = inv.add(EquipmentData.BLADE, Tier.EXCELLENT_1)
        val other = inv.add(EquipmentData.BLADE, Tier.EXCELLENT)
        assertEquals(MergeError.INGREDIENT_WRONG_SLOT, merge.validate(inv, main.uid, listOf(belt.uid)))
        assertEquals(MergeError.INGREDIENT_WRONG_TIER, merge.validate(inv, main.uid, listOf(plus.uid)))
        assertTrue(merge.merge(inv, main.uid, listOf(other.uid)).succeeded)
        assertEquals(Tier.EXCELLENT_1, inv.get(main.uid).tier)
    }

    @Test
    fun excellentPlusTwoTakesTwoAnyExcellent() {
        val main = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT_1)
        val a = inv.add(EquipmentData.BLADE, Tier.EXCELLENT)
        val b = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT)
        assertEquals(MergeError.WRONG_INGREDIENT_COUNT, merge.validate(inv, main.uid, listOf(a.uid)))
        assertTrue(merge.merge(inv, main.uid, listOf(a.uid, b.uid)).succeeded)
        assertEquals(Tier.EXCELLENT_2, inv.get(main.uid).tier)
    }

    @Test
    fun epicNeedsAnIdenticalExcellentPlusTwo() {
        val main = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT_2)
        val otherType = inv.add(EquipmentData.BLADE, Tier.EXCELLENT_2)
        val plain = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT)
        val twin = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT_2)
        assertEquals(MergeError.INGREDIENT_WRONG_TYPE, merge.validate(inv, main.uid, listOf(otherType.uid)))
        assertEquals(MergeError.INGREDIENT_WRONG_TIER, merge.validate(inv, main.uid, listOf(plain.uid)))
        assertTrue(merge.merge(inv, main.uid, listOf(twin.uid)).succeeded)
        assertEquals(Tier.EPIC, inv.get(main.uid).tier)
    }

    @Test
    fun epicPlusStepsTakeOneAnyPlainEpic() {
        for ((tier, expected) in listOf(Tier.EPIC to Tier.EPIC_1, Tier.EPIC_1 to Tier.EPIC_2, Tier.EPIC_2 to Tier.EPIC_3)) {
            val l = EquipmentData.Setup()
            val main = l.inv.add(EquipmentData.KUNAI, tier)
            val other = l.inv.add(EquipmentData.BLADE, Tier.EPIC)
            val plus = l.inv.add(EquipmentData.BLADE, Tier.EPIC_1)
            assertEquals(MergeError.INGREDIENT_WRONG_TIER, l.merge.validate(l.inv, main.uid, listOf(plus.uid)))
            assertTrue(l.merge.merge(l.inv, main.uid, listOf(other.uid)).succeeded)
            assertEquals(expected, l.inv.get(main.uid).tier)
        }
    }

    @Test
    fun legendNeedsTwoIdenticalPlainEpics() {
        val main = inv.add(EquipmentData.KUNAI, Tier.EPIC_3)
        val twin1 = inv.add(EquipmentData.KUNAI, Tier.EPIC)
        val twin2 = inv.add(EquipmentData.KUNAI, Tier.EPIC)
        val otherType = inv.add(EquipmentData.BLADE, Tier.EPIC)
        assertEquals(MergeError.INGREDIENT_WRONG_TYPE, merge.validate(inv, main.uid, listOf(twin1.uid, otherType.uid)))
        assertTrue(merge.merge(inv, main.uid, listOf(twin1.uid, twin2.uid)).succeeded)
        assertEquals(Tier.LEGEND, inv.get(main.uid).tier)
    }

    @Test
    fun legendPlusStepsTakeOneAnyPlainLegendUpToPlusFour() {
        val main = inv.add(EquipmentData.KUNAI, Tier.LEGEND)
        for (expected in listOf(Tier.LEGEND_1, Tier.LEGEND_2, Tier.LEGEND_3, Tier.LEGEND_4)) {
            val other = inv.add(EquipmentData.BLADE, Tier.LEGEND)
            assertTrue(merge.merge(inv, main.uid, listOf(other.uid)).succeeded)
            assertEquals(expected, inv.get(main.uid).tier)
        }
        val more = inv.add(EquipmentData.BLADE, Tier.LEGEND)
        assertEquals(MergeError.MAIN_AT_MAX_TIER, merge.validate(inv, main.uid, listOf(more.uid)))
    }

    @Test
    fun equippedItemCannotBeAnIngredientButCanBeMain() {
        val main = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        val equipped = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        val c = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        inv.equip(equipped.uid)
        assertEquals(MergeError.INGREDIENT_EQUIPPED, merge.validate(inv, main.uid, listOf(equipped.uid, c.uid)))
        assertTrue(merge.merge(inv, equipped.uid, listOf(main.uid, c.uid)).succeeded)
        assertEquals(equipped.uid, inv.equippedIn(Slot.WEAPON)!!.uid)
        assertEquals(Tier.GOOD, inv.equippedIn(Slot.WEAPON)!!.tier)
    }

    @Test
    fun rejectsMainAsIngredientDuplicatesAndMissingMain() {
        val main = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        val a = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        assertEquals(MergeError.INGREDIENT_IS_MAIN, merge.validate(inv, main.uid, listOf(main.uid, a.uid)))
        assertEquals(MergeError.DUPLICATE_INGREDIENT, merge.validate(inv, main.uid, listOf(a.uid, a.uid)))
        assertEquals(MergeError.MAIN_NOT_FOUND, merge.validate(inv, 999, listOf(a.uid)))
    }

    @Test
    fun failedMergeChangesNothing() {
        val main = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        val a = inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        val other = inv.add(EquipmentData.BLADE, Tier.NORMAL)
        assertFalse(merge.merge(inv, main.uid, listOf(a.uid, other.uid)).succeeded)
        assertEquals(3, inv.items.size)
        assertEquals(Tier.NORMAL, inv.get(main.uid).tier)
    }

    @Test
    fun resultKeepsMainLevelAndRefundsIngredientUpgradesOnly() {
        val main = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT, level = 7)
        val ingredient = inv.add(EquipmentData.BLADE, Tier.EXCELLENT, level = 5)
        val (gold, plans) = s.upgrades.investedUpTo(5)
        val outcome = merge.merge(inv, main.uid, listOf(ingredient.uid))
        assertEquals(7, inv.get(main.uid).level)
        assertEquals(gold, outcome.refundedGold)
        assertEquals(gold, inv.gold)
        assertEquals(plans, inv.plansFor(Slot.WEAPON))
        assertTrue(gold > 0)
    }

    @Test
    fun candidatesListOnlyValidIngredients() {
        val main = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT)
        val ok1 = inv.add(EquipmentData.BLADE, Tier.EXCELLENT)
        val ok2 = inv.add(EquipmentData.KUNAI, Tier.EXCELLENT)
        inv.add(EquipmentData.BLADE, Tier.EXCELLENT_1)
        inv.add(EquipmentData.BELT_A, Tier.EXCELLENT)
        assertEquals(setOf(ok1.uid, ok2.uid), merge.candidates(inv, main.uid).map { it.uid }.toSet())
    }
}

class MergeAllTest {
    private val s = EquipmentData.Setup()

    @Test
    fun chainsFromNormalUpToExcellent() {
        repeat(27) { s.inv.add(EquipmentData.KUNAI, Tier.NORMAL) }
        MergeAllService(s.merge).mergeAll(s.inv)
        assertEquals(Tier.EXCELLENT, s.inv.items.single().tier)
    }

    @Test
    fun neverMergesExcellentOrMixesTypes() {
        repeat(3) { s.inv.add(EquipmentData.KUNAI, Tier.EXCELLENT) }
        s.inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        s.inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        s.inv.add(EquipmentData.BLADE, Tier.NORMAL)
        assertTrue(MergeAllService(s.merge).mergeAll(s.inv).isEmpty())
        assertEquals(6, s.inv.items.size)
    }

    @Test
    fun keepsEquippedItemAsMain() {
        val low = s.inv.add(EquipmentData.KUNAI, Tier.NORMAL, level = 1)
        s.inv.add(EquipmentData.KUNAI, Tier.NORMAL, level = 9)
        s.inv.add(EquipmentData.KUNAI, Tier.NORMAL, level = 3)
        s.inv.equip(low.uid)
        MergeAllService(s.merge).mergeAll(s.inv)
        assertEquals(low.uid, s.inv.equippedIn(Slot.WEAPON)!!.uid)
        assertEquals(Tier.GOOD, s.inv.equippedIn(Slot.WEAPON)!!.tier)
    }

    @Test
    fun picksHighestLevelAsMainWhenNothingIsEquipped() {
        s.inv.add(EquipmentData.KUNAI, Tier.NORMAL, level = 2)
        val high = s.inv.add(EquipmentData.KUNAI, Tier.NORMAL, level = 8)
        s.inv.add(EquipmentData.KUNAI, Tier.NORMAL, level = 4)
        MergeAllService(s.merge).mergeAll(s.inv)
        val result = s.inv.items.single()
        assertEquals(high.uid, result.uid)
        assertEquals(8, result.level)
    }
}

class UpgradeTest {
    private val s = EquipmentData.Setup()

    @Test
    fun upgradeMaxStopsAtTheCapOfEveryTier() {
        val caps = listOf(10, 20, 30, 40, 45, 50, 60, 65, 70, 75, 80, 85, 90, 95, 100)
        Tier.entries.forEachIndexed { i, tier ->
            val l = EquipmentData.Setup()
            val item = l.inv.add(EquipmentData.KUNAI, tier)
            l.inv.addGold(Long.MAX_VALUE / 2)
            l.inv.addPlans(Slot.WEAPON, Long.MAX_VALUE / 2)
            l.upgrades.upgradeMax(l.inv, item.uid)
            assertEquals("tier $tier", caps[i], l.inv.get(item.uid).level)
            assertEquals(UpgradeError.AT_LEVEL_CAP, l.upgrades.upgrade(l.inv, item.uid))
        }
    }

    @Test
    fun upgradeFailsWithoutResourcesAndChangesNothing() {
        val item = s.inv.add(EquipmentData.KUNAI, Tier.NORMAL)
        assertEquals(UpgradeError.NOT_ENOUGH_RESOURCES, s.upgrades.upgrade(s.inv, item.uid))
        assertEquals(1, s.inv.get(item.uid).level)
    }

    @Test
    fun upgradeMaxSpendsOnlyWhatIsAffordable() {
        val item = s.inv.add(EquipmentData.KUNAI, Tier.BETTER)
        val (gold, plans) = s.upgrades.investedUpTo(6)
        s.inv.addGold(gold + 1)
        s.inv.addPlans(Slot.WEAPON, plans)
        assertEquals(5, s.upgrades.upgradeMax(s.inv, item.uid))
        assertEquals(1L, s.inv.gold)
    }

    @Test
    fun resetRefundsEverythingAndSetsLevelOne() {
        val item = s.inv.add(EquipmentData.BELT_A, Tier.EPIC)
        val (gold, plans) = s.upgrades.investedUpTo(30)
        s.inv.addGold(gold)
        s.inv.addPlans(Slot.BELT, plans)
        s.upgrades.upgradeMax(s.inv, item.uid)
        assertEquals(30, s.inv.get(item.uid).level)
        assertEquals(0L, s.inv.gold)
        s.upgrades.reset(s.inv, item.uid)
        assertEquals(1, s.inv.get(item.uid).level)
        assertEquals(gold, s.inv.gold)
        assertEquals(plans, s.inv.plansFor(Slot.BELT))
    }
}

class ItemStatsTest {
    private val stats = ItemStats(EquipmentBalance(), EquipmentData.catalog())

    @Test
    fun weaponBaseAttackFollowsTheTierTable() {
        val expected = mapOf(Tier.NORMAL to 10.0, Tier.EXCELLENT to 50.0, Tier.EPIC to 132.0, Tier.LEGEND to 231.0, Tier.LEGEND_4 to 323.4)
        expected.forEach { (tier, value) -> assertEquals(value, stats.mainStat(Item(1, EquipmentData.KUNAI, tier, 1)), 1e-9) }
    }

    @Test
    fun healthSlotUsesTenTimesTheScalePlusPerLevel() {
        val balance = EquipmentBalance()
        assertEquals(StatKind.HEALTH, stats.mainStatKind(Item(1, EquipmentData.BELT_A, Tier.GOOD, 1)))
        assertEquals(20 * 10 + 2 * balance.healthPerLevel, stats.mainStat(Item(1, EquipmentData.BELT_A, Tier.GOOD, 3)), 1e-9)
    }

    @Test
    fun excellentWeaponUnlocksTheStartAtLevelTwoBonus() {
        val better = stats.unlockedBonuses(Item(1, EquipmentData.KUNAI, Tier.BETTER, 1)).map { it.bonusId }
        val excellent = stats.unlockedBonuses(Item(1, EquipmentData.KUNAI, Tier.EXCELLENT_2, 1)).map { it.bonusId }
        assertFalse(EquipmentData.BONUS_START_LV2 in better)
        assertEquals(setOf(EquipmentData.BONUS_ATK10, EquipmentData.BONUS_SKILL30, EquipmentData.BONUS_START_LV2), excellent.toSet())
    }

    @Test
    fun plusMarksAndBaseColour() {
        assertEquals(BaseTier.EPIC, Tier.EPIC_2.base)
        assertEquals(2, Tier.EPIC_2.plusMarks)
        assertEquals(0, Tier.LEGEND.plusMarks)
    }
}
