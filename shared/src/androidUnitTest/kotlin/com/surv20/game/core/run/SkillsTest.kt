package com.surv20.game.core.run

import com.surv20.game.core.SeededRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal object SkillData {
    const val WEAPON = "w.thrown"
    const val MINES = "a.mines"
    const val FIRE = "a.fire"
    const val LIGHTNING = "a.lightning"
    const val BOOMERANG = "a.boomerang"
    const val MAGNET = "p.magnet"
    const val XP = "p.xp"

    // 8 actives, 7 passives: enough to fill all 12 slots and test leftovers.
    fun catalog(): SkillCatalog {
        val skills = mutableListOf(
            SkillDefinition(WEAPON, SkillKind.ACTIVE, isWeaponSkill = true),
            SkillDefinition("w.blade", SkillKind.ACTIVE, isWeaponSkill = true),
            SkillDefinition(MINES, SkillKind.ACTIVE), SkillDefinition(FIRE, SkillKind.ACTIVE),
            SkillDefinition(LIGHTNING, SkillKind.ACTIVE), SkillDefinition(BOOMERANG, SkillKind.ACTIVE),
            SkillDefinition("a.orbit", SkillKind.ACTIVE), SkillDefinition("a.aura", SkillKind.ACTIVE),
            SkillDefinition(MAGNET, SkillKind.PASSIVE), SkillDefinition(XP, SkillKind.PASSIVE),
        )
        (1..5).forEach { skills += SkillDefinition("p.extra$it", SkillKind.PASSIVE) }
        return SkillCatalog(
            skills,
            listOf(
                EvolutionDefinition("evo.boomerang", BOOMERANG, MAGNET),
                EvolutionDefinition("evo.thrown", WEAPON, XP),
                EvolutionDefinition("evo.mines.fire", MINES, FIRE),
                EvolutionDefinition("evo.mines.lightning", MINES, LIGHTNING),
            ),
        )
    }

    fun newLoadout() = Loadout(catalog(), WEAPON)

    fun max(loadout: Loadout, skillId: String) {
        if (!loadout.owns(skillId)) CardDraw.apply(loadout, Card.newSkill(skillId))
        while (loadout.find(skillId)!!.level < SkillCatalog.MAX_LEVEL) CardDraw.apply(loadout, Card.levelUp(skillId, 0))
    }
}

class LoadoutAndCardTest {
    private fun offer(loadout: Loadout, seed: Int) = CardDraw(SeededRandom(seed), 50).offer(loadout)

    @Test
    fun weaponSkillSitsInSlotOneAndExcellentStartsItAtLevelTwo() {
        val loadout = Loadout(SkillData.catalog(), SkillData.WEAPON, weaponSkillLevel = 2)
        assertEquals(SkillData.WEAPON, loadout.actives[0].skillId)
        assertEquals(2, loadout.actives[0].level)
    }

    @Test
    fun offerHasThreeDifferentCardsAndNeverOtherWeaponSkills() {
        repeat(200) { seed ->
            val cards = offer(SkillData.newLoadout(), seed)
            assertEquals(3, cards.size)
            assertEquals(3, cards.map { it.id }.distinct().size)
            assertFalse(cards.any { it.id == "w.blade" })
        }
    }

    @Test
    fun equippedWeaponSkillIsOfferedAsALevelUp() {
        assertTrue((0 until 200).any { seed ->
            offer(SkillData.newLoadout(), seed).any { it.kind == CardKind.LEVEL_UP && it.id == SkillData.WEAPON && it.levelAfter == 2 }
        })
    }

    @Test
    fun sixActiveSlotsLimitNewActiveCards() {
        val loadout = SkillData.newLoadout()
        listOf(SkillData.MINES, SkillData.FIRE, SkillData.LIGHTNING, SkillData.BOOMERANG, "a.orbit").forEach { CardDraw.apply(loadout, Card.newSkill(it)) }
        assertEquals(6, loadout.actives.size)
        repeat(200) { seed -> assertFalse(offer(loadout, seed).any { it.kind == CardKind.NEW_SKILL && it.id == "a.aura" }) }
    }

    @Test
    fun eligibleEvolutionIsAlwaysOffered() {
        val loadout = SkillData.newLoadout()
        SkillData.max(loadout, SkillData.BOOMERANG)
        CardDraw.apply(loadout, Card.newSkill(SkillData.MAGNET))
        repeat(100) { seed -> assertTrue(offer(loadout, seed).any { it.kind == CardKind.EVOLVE && it.id == "evo.boomerang" }) }
    }

    @Test
    fun evolutionNeedsMaxLevelBaseButAnyLevelPassive() {
        val loadout = SkillData.newLoadout()
        CardDraw.apply(loadout, Card.newSkill(SkillData.BOOMERANG))
        CardDraw.apply(loadout, Card.newSkill(SkillData.MAGNET))
        val evolution = loadout.catalog.evolution("evo.boomerang")
        assertFalse(loadout.canEvolve(evolution))
        SkillData.max(loadout, SkillData.BOOMERANG)
        assertEquals(1, loadout.find(SkillData.MAGNET)!!.level)
        assertTrue(loadout.canEvolve(evolution))
        CardDraw.apply(loadout, Card.evolve(evolution.id))
        assertEquals("evo.boomerang", loadout.find(SkillData.BOOMERANG)!!.evolutionId)
        assertTrue("passive partner stays", loadout.owns(SkillData.MAGNET))
        assertFalse(loadout.canEvolve(evolution))
    }

    @Test
    fun activeActiveEvolutionNeedsBothAtMaxAndFreesThePartnerSlot() {
        val loadout = SkillData.newLoadout()
        SkillData.max(loadout, SkillData.MINES)
        CardDraw.apply(loadout, Card.newSkill(SkillData.FIRE))
        val evolution = loadout.catalog.evolution("evo.mines.fire")
        assertFalse(loadout.canEvolve(evolution))
        SkillData.max(loadout, SkillData.FIRE)
        CardDraw.apply(loadout, Card.evolve(evolution.id))
        assertFalse(loadout.owns(SkillData.FIRE))
        assertEquals(2, loadout.actives.size)
    }

    @Test
    fun twoPossibleCombinationEvolutionsAreBothOffered() {
        val loadout = SkillData.newLoadout()
        listOf(SkillData.MINES, SkillData.FIRE, SkillData.LIGHTNING).forEach { SkillData.max(loadout, it) }
        assertEquals(
            setOf("evo.mines.fire", "evo.mines.lightning"),
            offer(loadout, 3).filter { it.kind == CardKind.EVOLVE }.map { it.id }.toSet(),
        )
    }

    @Test
    fun fewerThanThreeAvailableShowsOnlyThoseAndNothingGivesGold() {
        val loadout = SkillData.newLoadout()
        // No mines, so no combination evolution can free an active slot.
        listOf(SkillData.FIRE, SkillData.LIGHTNING, SkillData.BOOMERANG, "a.orbit", "a.aura").forEach { SkillData.max(loadout, it) }
        listOf(SkillData.MAGNET, "p.extra1", "p.extra2", "p.extra3", "p.extra4", "p.extra5").forEach { SkillData.max(loadout, it) }
        while (loadout.find(SkillData.WEAPON)!!.level < 4) CardDraw.apply(loadout, Card.levelUp(SkillData.WEAPON, 0))
        CardDraw.apply(loadout, Card.evolve("evo.boomerang"))
        assertTrue(loadout.eligibleEvolutions.isEmpty())

        val one = offer(loadout, 1).single()
        assertEquals(CardKind.LEVEL_UP, one.kind)
        assertEquals(SkillData.WEAPON, one.id)
        CardDraw.apply(loadout, one)
        val gold = offer(loadout, 1).single()
        assertEquals(CardKind.GOLD, gold.kind)
        assertEquals(50L, CardDraw.apply(loadout, gold))
    }
}

class RunChestTest {
    @Test
    fun upgradeCountFollows70_25_5() {
        val chest = RunChest(SeededRandom(42))
        val n = 10000
        val counts = (0 until n).groupingBy { chest.rollCount() }.eachCount()
        assertEquals(0.70, counts.getValue(1) / n.toDouble(), 0.02)
        assertEquals(0.25, counts.getValue(3) / n.toDouble(), 0.02)
        assertEquals(0.05, counts.getValue(5) / n.toDouble(), 0.02)
    }

    @Test
    fun evolutionHasPriorityAndFirstInDataOrderWins() {
        val loadout = SkillData.newLoadout()
        listOf(SkillData.MINES, SkillData.FIRE, SkillData.LIGHTNING).forEach { SkillData.max(loadout, it) }
        val result = RunChest(SeededRandom(0), listOf(1.0, 0.0, 0.0), 10).open(loadout).single()
        assertEquals(CardKind.EVOLVE, result.kind)
        assertEquals("evo.mines.fire", result.id)
        assertFalse(loadout.owns(SkillData.FIRE))
    }

    @Test
    fun upgradesOnlyOwnedSkillsAndFallsBackToGold() {
        val loadout = SkillData.newLoadout()
        val result = RunChest(SeededRandom(0), listOf(0.0, 0.0, 1.0), 10).open(loadout)
        assertEquals(5, result.size)
        assertTrue(result.take(4).all { it.kind == CardKind.LEVEL_UP && it.id == SkillData.WEAPON })
        assertEquals(CardKind.GOLD, result[4].kind)
        assertEquals(1, loadout.actives.size)
        assertEquals(5, loadout.find(SkillData.WEAPON)!!.level)
    }
}

class ExperienceAndCrystalTest {
    @Test
    fun excessXpCarriesOverAndMultipleLevelUpsAreCounted() {
        val bar = ExperienceBar { 10 }
        assertEquals(2, bar.add(25))
        assertEquals(3, bar.level)
        assertEquals(5L, bar.xp)
    }

    @Test
    fun defaultCurveGrows() {
        val bar = ExperienceBar()
        var last = 0L
        repeat(50) {
            assertTrue(bar.xpToNext >= last)
            last = bar.xpToNext
            bar.add(bar.xpToNext)
        }
    }

    @Test
    fun normalEnemiesDropYellowAboutTwoPercent() {
        val drops = CrystalDrops(SeededRandom(7))
        val n = 100000
        val yellow = (0 until n).count { drops.drop(EnemyClass.NORMAL) == CrystalKind.YELLOW }
        assertEquals(0.02, yellow / n.toDouble(), 0.005)
    }

    @Test
    fun elitesAndBossesAlwaysDropYellowAndClearedEnemiesDropNothing() {
        val drops = CrystalDrops(SeededRandom(7))
        assertEquals(CrystalKind.YELLOW, drops.drop(EnemyClass.ELITE))
        assertEquals(CrystalKind.YELLOW, drops.drop(EnemyClass.BOSS))
        assertEquals(CrystalKind.NONE, drops.drop(EnemyClass.NORMAL, removedByBossEntry = true))
        assertEquals(5, CrystalDrops.value(CrystalKind.BLUE))
    }
}
