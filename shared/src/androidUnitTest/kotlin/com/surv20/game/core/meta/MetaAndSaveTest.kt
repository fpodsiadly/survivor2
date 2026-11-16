package com.surv20.game.core.meta

import com.surv20.game.core.KeyValueStore
import com.surv20.game.core.MemoryStore
import com.surv20.game.core.SeededRandom
import com.surv20.game.core.equipment.EquipmentData
import com.surv20.game.core.equipment.Slot
import com.surv20.game.core.equipment.Tier
import com.surv20.game.core.run.Stat
import com.surv20.game.core.save.ChapterRecord
import com.surv20.game.core.save.InventoryMapper
import com.surv20.game.core.save.SaveData
import com.surv20.game.core.save.SaveSource
import com.surv20.game.core.save.SaveStore
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardTest {
    @Test
    fun lossScalesBonusAndXpBySurvivalShareAndKeepsCollectedGold() {
        val r = RewardCalculator(SeededRandom(1)).compute(2, won = false, survivedSeconds = 450.0, fullMapSeconds = 900.0, collectedGold = 300)
        assertEquals(300 + (500 + 250) / 2L, r.gold)
        assertEquals((1000 + 150) / 2L, r.accountXp)
        assertEquals(0, r.itemCount)
    }

    @Test
    fun winGivesFullXpAndOneToThreeItems() {
        repeat(50) { seed ->
            val r = RewardCalculator(SeededRandom(seed)).compute(1, won = true, survivedSeconds = 930.0, fullMapSeconds = 900.0, collectedGold = 0)
            assertEquals(1000L, r.accountXp)
            assertTrue(r.itemCount in 1..3)
        }
    }
}

class AccountTalentChapterTest {
    @Test
    fun accountLevelCapsAtTen() {
        val account = AccountProgress(xpToNext = { 100 })
        assertEquals(9, account.add(100000))
        assertEquals(10, account.level)
        assertEquals(0, account.add(500))
        assertEquals(0L, account.xp)
    }

    @Test
    fun talentCannotExceedAccountLevelAndNeedsGold() {
        val inventory = EquipmentData.Setup().inv
        val book = TalentBook(listOf(TalentDefinition("t.atk", Stat.ATTACK, 0.02) { level -> 100L * (level + 1) }))
        assertEquals(TalentError.NOT_ENOUGH_GOLD, book.upgrade("t.atk", 2, inventory))
        inventory.addGold(1000)
        assertEquals(TalentError.NONE, book.upgrade("t.atk", 2, inventory))
        assertEquals(TalentError.NONE, book.upgrade("t.atk", 2, inventory))
        assertEquals(TalentError.ABOVE_ACCOUNT_LEVEL, book.upgrade("t.atk", 2, inventory))
        assertEquals(1000L - 100 - 200, inventory.gold)
        assertEquals(0.04, book.modifiers().single().value, 1e-9)
    }

    @Test
    fun winUnlocksNextChapterLossDoesNotAndLastStaysLast() {
        val save = SaveData()
        val chapters = ChapterProgress(save, 3)
        chapters.recordRun(1, won = false, seconds = 400.0)
        assertTrue(!chapters.isUnlocked(2))
        chapters.recordRun(1, won = true, seconds = 920.0)
        assertTrue(chapters.isUnlocked(2) && !chapters.isUnlocked(3))
        val last = SaveData(unlockedChapter = 3)
        ChapterProgress(last, 3).recordRun(3, true, 500.0)
        assertEquals(3, last.unlockedChapter)
    }

    @Test
    fun bestTimeIsLongestSurvivalBeforeAWinThenShortestWin() {
        val chapters = ChapterProgress(SaveData(), 3)
        chapters.recordRun(1, false, 300.0)
        chapters.recordRun(1, false, 500.0)
        assertEquals(500.0, chapters.bestTime(1)!!, 0.0)
        listOf(950.0, 910.0, 990.0).forEach { chapters.recordRun(1, true, it) }
        assertEquals(910.0, chapters.bestTime(1)!!, 0.0)
    }

    @Test
    fun firstFinishedRunSetsTheTutorialFlagAndLockedChapterThrows() {
        val save = SaveData()
        ChapterProgress(save, 3).recordRun(1, won = false, seconds = 60.0)
        assertTrue(save.tutorialDone)
        assertTrue(runCatching { ChapterProgress(SaveData(), 3).recordRun(2, true, 100.0) }.isFailure)
    }

    @Test
    fun multipliersMustGrowByOneAndAHalf() {
        assertTrue(ChapterProgress.validateMultipliers(listOf(1.0, 1.5, 2.25)).isEmpty())
        assertEquals(1, ChapterProgress.validateMultipliers(listOf(1.0, 1.4)).size)
    }

    @Test
    fun languagePicksSavedThenSystemThenEnglish() {
        val cases = listOf(
            Triple(null, "pl-PL", "pl"), Triple(null, "Polish", "pl"), Triple(null, "en-GB", "en"), Triple(null, "de", "en"),
            Triple(null, null, "en"), Triple("pl", "en", "pl"), Triple("en", "pl", "en"), Triple("fr", "pl", "pl"),
        )
        cases.forEach { (saved, system, expected) -> assertEquals("$saved/$system", expected, Language.pick(saved, system)) }
    }
}

// Store that can fail on a chosen write to simulate a crash mid-save.
private class CrashingStore(val inner: MemoryStore = MemoryStore()) : KeyValueStore {
    var failOnKey: String? = null
    override fun getString(key: String): String? = inner.getString(key)
    override fun putString(key: String, value: String?) {
        if (key == failOnKey) throw IllegalStateException("simulated crash writing $key")
        inner.putString(key, value)
    }
}

class SaveStoreTest {
    private fun data(gold: Long) = SaveData(gold = gold, accountLevel = 3, unlockedChapter = 2)

    @Test
    fun roundTripKeepsTheData() {
        val store = SaveStore(MemoryStore())
        val d = data(123)
        d.chapters[1] = ChapterRecord(won = true, bestWinSeconds = 912.5, bestSurvivalSeconds = 912.5)
        d.settings.language = "pl"
        store.save(d)
        val loaded = store.load()
        assertEquals(SaveSource.MAIN, loaded.source)
        assertEquals(123L, loaded.data.gold)
        assertEquals(912.5, loaded.data.chapters[1]!!.bestWinSeconds!!, 0.0)
        assertEquals("pl", loaded.data.settings.language)
    }

    @Test
    fun missingSaveGivesANewOne() {
        val loaded = SaveStore(MemoryStore()).load()
        assertEquals(SaveSource.NEW, loaded.source)
        assertEquals(1, loaded.data.accountLevel)
    }

    @Test
    fun crashWhileWritingTheMainKeyKeepsThePreviousSave() {
        val raw = CrashingStore()
        val store = SaveStore(raw)
        store.save(data(100))
        raw.failOnKey = SaveStore.MAIN_KEY
        assertTrue(runCatching { store.save(data(200)) }.isFailure)
        assertEquals(100L, store.load().data.gold)
    }

    @Test
    fun corruptMainLoadsTheBackup() {
        val raw = MemoryStore()
        val store = SaveStore(raw)
        store.save(data(100))
        store.save(data(200))
        raw.putString(SaveStore.MAIN_KEY, "{ not json")
        val loaded = store.load()
        assertEquals(SaveSource.BACKUP, loaded.source)
        assertEquals(100L, loaded.data.gold)
    }

    @Test
    fun versionZeroSaveIsMigrated() {
        val raw = MemoryStore()
        // Hypothetical old format: gold was called "coins" and there was no version field.
        raw.putString(SaveStore.MAIN_KEY, """{ "coins": 77 }""")
        val migrations = mapOf(0 to { json: JsonObject -> JsonObject(json - "coins" + ("gold" to json.getValue("coins"))) })
        val loaded = SaveStore(raw, migrations).load()
        assertEquals(SaveSource.MAIN, loaded.source)
        assertEquals(77L, loaded.data.gold)
        assertEquals(SaveData.CURRENT_VERSION, loaded.data.version)
    }

    @Test
    fun newerVersionThanTheAppIsNotRead() {
        val raw = MemoryStore()
        raw.putString(SaveStore.MAIN_KEY, """{ "version": 99, "gold": 5 }""")
        assertEquals(SaveSource.NEW, SaveStore(raw).load().source)
    }

    @Test
    fun inventorySurvivesSaveAndLoad() {
        val inv = EquipmentData.Setup().inv
        val weapon = inv.add(EquipmentData.KUNAI, Tier.EPIC_2, level = 12)
        inv.add(EquipmentData.BELT_A, Tier.GOOD)
        inv.equip(weapon.uid)
        inv.addGold(500)
        inv.addPlans(Slot.BELT, 7)
        val store = SaveStore(MemoryStore())
        val save = SaveData()
        InventoryMapper.write(inv, save)
        store.save(save)
        val restored = InventoryMapper.read(store.load().data, EquipmentData.catalog())
        assertEquals(Tier.EPIC_2, restored.equippedIn(Slot.WEAPON)!!.tier)
        assertEquals(12, restored.equippedIn(Slot.WEAPON)!!.level)
        assertEquals(500L, restored.gold)
        assertEquals(7L, restored.plansFor(Slot.BELT))
        assertTrue("new uids do not collide", restored.add(EquipmentData.BELT_B, Tier.NORMAL).uid > weapon.uid)
        assertNull(restored.equippedIn(Slot.BELT))
    }
}
