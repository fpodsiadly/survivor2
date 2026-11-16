package com.surv20.game.core.meta

import com.surv20.game.core.Balance
import com.surv20.game.core.GameRandom
import com.surv20.game.core.equipment.Inventory
import com.surv20.game.core.equipment.Tier
import com.surv20.game.core.run.ModifierSource
import com.surv20.game.core.run.Stat
import com.surv20.game.core.run.StatModifier
import com.surv20.game.core.save.ChapterRecord
import com.surv20.game.core.save.SaveData
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class RunReward(val gold: Long, val accountXp: Long, val itemCount: Int, val itemTier: Tier, val plans: Long)

// End-of-run rewards (SPEC W28; PLAN P22): gold collected in the run always counts in full; the win bonus and
// account XP scale with survival time / full map time on a loss; items only on a win.
class RewardCalculator(private val random: GameRandom) {
    fun compute(chapter: Int, won: Boolean, survivedSeconds: Double, fullMapSeconds: Double, collectedGold: Long): RunReward {
        require(chapter >= 1)
        require(fullMapSeconds > 0)
        val share = if (won) 1.0 else min(1.0, max(0.0, survivedSeconds / fullMapSeconds))
        return RunReward(
            gold = collectedGold + floor(Balance.winGoldBonus(chapter) * share).toLong(),
            accountXp = floor(Balance.winAccountXp(chapter) * share).toLong(),
            itemCount = if (won) rollItemCount() else 0,
            itemTier = itemTierFor(chapter),
            plans = if (won) Balance.plansOnWin(chapter) else if (random.nextDouble() < Balance.PLAN_DROP_CHANCE_ON_LOSS) 1 else 0,
        )
    }

    private fun itemTierFor(chapter: Int): Tier = when {
        chapter <= 1 -> Tier.NORMAL
        chapter == 2 -> Tier.GOOD
        else -> Tier.BETTER
    } // placeholder table per chapter

    private fun rollItemCount(): Int {
        var roll = random.nextDouble()
        Balance.ITEM_COUNT_ODDS.forEachIndexed { i, p ->
            roll -= p
            if (roll < 0) return i + 1
        }
        return Balance.ITEM_COUNT_ODDS.size
    }
}

// Account level from run (and from F2 patrol) XP, capped (SPEC W29: 10 in F1).
class AccountProgress(
    level: Int = 1,
    xp: Long = 0,
    private val xpToNext: (Int) -> Long = Balance::accountXpToNext,
    val maxLevel: Int = Balance.ACCOUNT_MAX_LEVEL_F1,
) {
    var level = level
        private set
    var xp = xp
        private set

    init {
        require(level in 1..maxLevel)
    }

    // Returns the number of level-ups; XP stops counting at the cap.
    fun add(amount: Long): Int {
        require(amount >= 0)
        if (level >= maxLevel) return 0
        xp += amount
        var ups = 0
        while (level < maxLevel && xp >= xpToNext(level)) {
            xp -= xpToNext(level)
            level++
            ups++
        }
        if (level >= maxLevel) xp = 0
        return ups
    }
}

// A stat talent bought with gold (SPEC W30). Value per level is a percentage of the stat.
data class TalentDefinition(val id: String, val stat: Stat, val valuePerLevel: Double, val goldForLevel: (Int) -> Long)

enum class TalentError { NONE, UNKNOWN_TALENT, ABOVE_ACCOUNT_LEVEL, NOT_ENOUGH_GOLD }

class TalentBook(talents: List<TalentDefinition>, levels: Map<String, Int> = emptyMap()) {
    private val byId = talents.associateBy { it.id }
    private val levelsById = levels.toMutableMap()
    val levels: Map<String, Int> get() = levelsById

    fun levelOf(id: String): Int = levelsById[id] ?: 0

    // A talent's level may not exceed the account level (SPEC W29, W30).
    fun upgrade(id: String, accountLevel: Int, wallet: Inventory): TalentError {
        val talent = byId[id] ?: return TalentError.UNKNOWN_TALENT
        val level = levelOf(id)
        if (level + 1 > accountLevel) return TalentError.ABOVE_ACCOUNT_LEVEL
        if (!wallet.trySpendGold(talent.goldForLevel(level))) return TalentError.NOT_ENOUGH_GOLD
        levelsById[id] = level + 1
        return TalentError.NONE
    }

    fun modifiers(): List<StatModifier> =
        byId.values.filter { levelOf(it.id) > 0 }.map { StatModifier(it.stat, it.valuePerLevel * levelOf(it.id), ModifierSource.TALENT) }
}

// Chapter unlocks, best times and the tutorial flag (SPEC W26, W37).
class ChapterProgress(private val save: SaveData, private val chapterCount: Int) {
    init {
        require(chapterCount >= 1)
    }

    fun isUnlocked(chapter: Int): Boolean = chapter >= 1 && chapter <= min(save.unlockedChapter, chapterCount)

    // Records a finished run (win or loss). Interrupted runs are never recorded (SPEC W42).
    fun recordRun(chapter: Int, won: Boolean, seconds: Double) {
        check(isUnlocked(chapter)) { "Chapter $chapter is locked." }
        val record = save.chapters.getOrPut(chapter) { ChapterRecord() }
        record.bestSurvivalSeconds = max(record.bestSurvivalSeconds, seconds)
        if (won) {
            record.won = true
            record.bestWinSeconds = record.bestWinSeconds?.let { min(it, seconds) } ?: seconds
            if (chapter == save.unlockedChapter && chapter < chapterCount) save.unlockedChapter = chapter + 1
        }
        save.tutorialDone = true
    }

    // Shortest win; before the first win the longest survival (SPEC W26).
    fun bestTime(chapter: Int): Double? {
        val record = save.chapters[chapter] ?: return null
        return if (record.won) record.bestWinSeconds else record.bestSurvivalSeconds
    }

    companion object {
        // Each chapter's enemy multiplier must be at least 1.5× the previous one (SPEC W26).
        fun validateMultipliers(multipliers: List<Double>, minRatio: Double = Balance.CHAPTER_MIN_STRENGTH_RATIO): List<String> =
            multipliers.zipWithNext().mapIndexedNotNull { i, (a, b) ->
                if (b + 1e-9 < a * minRatio) "Chapter ${i + 2} multiplier $b is below $minRatio× chapter ${i + 1} ($a)." else null
            }
    }
}

// Language choice (SPEC W44): a saved choice wins; otherwise the system language if Polish or English, else English.
object Language {
    const val POLISH = "pl"
    const val ENGLISH = "en"

    fun pick(saved: String?, systemLanguage: String?): String {
        if (saved == POLISH || saved == ENGLISH) return saved
        val value = systemLanguage?.trim().orEmpty()
        return when {
            value.startsWith("pl", ignoreCase = true) || value.equals("Polish", ignoreCase = true) -> POLISH
            else -> ENGLISH
        }
    }
}
