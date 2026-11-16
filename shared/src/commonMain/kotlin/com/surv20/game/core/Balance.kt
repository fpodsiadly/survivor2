package com.surv20.game.core

// All tunable numbers of the game in one place (SPEC W41, PLAN.md K4).
// Values are starting points: from RESEARCH §5 where known, otherwise placeholders (decyzja autora).
object Balance {
    // --- Player base values (SPEC "Wartości bazowe") ---
    const val PLAYER_MAX_HEALTH = 100.0
    const val PLAYER_MOVE_SPEED = 3.0
    const val PLAYER_PICKUP_RADIUS = 1.5
    const val INVULNERABLE_SECONDS = 0.5 // SPEC W13
    const val MAX_ALIVE_ENEMIES = 400

    // --- View and maps (SPEC W3, W7, W8, W10) ---
    const val VIEW_WIDTH = 10f // world units at the 9:16 reference
    const val SPAWN_MARGIN = 1f
    const val RELOCATE_RADIUS_FACTOR = 1.6f // placeholder
    const val ARENA_DIAMETER_FRACTION = 0.8f
    const val ALARM_LEAD_SECONDS = 3.0 // SPEC W11

    // --- XP and crystals (SPEC W14) ---
    const val CRYSTAL_GREEN = 1
    const val CRYSTAL_BLUE = 5
    const val CRYSTAL_YELLOW = 25
    const val YELLOW_CRYSTAL_CHANCE = 0.02

    // XP to go from the level to the next. Placeholder: 5 for level 1, +3 per level.
    fun xpToNextLevel(level: Int): Long = 5L + 3L * (level - 1)

    // --- Cards and in-run chest (SPEC W15) ---
    const val GOLD_WHEN_NO_CARD = 50L // placeholder
    val CHEST_COUNT_ODDS = listOf(0.70, 0.25, 0.05) // 1, 3, 5 upgrades
    const val GOLD_PER_EMPTY_CHEST_UPGRADE = 10L // placeholder

    // --- Map pickups (SPEC W18) ---
    const val BOMB_ELITE_BOSS_SHARE = 0.10
    const val MEAT_HEAL_SHARE = 0.30
    const val PICKUP_OFFSCREEN_SECONDS = 60.0

    // --- Enemies and bosses (SPEC W12, W20) ---
    const val MIN_TELEGRAPH_SECONDS = 0.7
    const val TELEGRAPH_DAMAGE_THRESHOLD = 0.15

    // --- Equipment (SPEC W24, W25; RESEARCH §5.3) ---
    val LEVEL_CAPS = listOf(10, 20, 30, 40, 45, 50, 60, 65, 70, 75, 80, 85, 90, 95, 100)
    private const val LEGEND_ATTACK = 231.0
    val WEAPON_BASE_STAT = listOf(
        10.0, 20.0, 30.0, 50.0, 60.0, 70.0, 132.0, 154.0, 176.0, 198.0, LEGEND_ATTACK,
        LEGEND_ATTACK * 1.1, LEGEND_ATTACK * 1.2, LEGEND_ATTACK * 1.3, LEGEND_ATTACK * 1.4,
    )
    const val HEALTH_SLOT_MULTIPLIER = 10.0
    const val ATTACK_PER_LEVEL = 2.0 // placeholder
    const val HEALTH_PER_LEVEL = 20.0 // placeholder
    fun upgradeGold(level: Int): Long = 100L * level // placeholder
    fun upgradePlans(level: Int): Long = 1L + level / 10 // placeholder

    // --- Rewards and account (SPEC W28, W29) ---
    fun winGoldBonus(chapter: Int): Long = 500L + 250L * (chapter - 1) // placeholder
    fun winAccountXp(chapter: Int): Long = 1000L + 150L * (chapter - 1)
    val ITEM_COUNT_ODDS = listOf(0.6, 0.3, 0.1) // 1, 2, 3 items on a win, placeholder
    const val PLAN_DROP_CHANCE_ON_LOSS = 0.5 // placeholder
    fun plansOnWin(chapter: Int): Long = 2L + chapter // placeholder
    fun accountXpToNext(level: Int): Long = 1000L * level // placeholder
    const val ACCOUNT_MAX_LEVEL_F1 = 10

    // --- Chapters (SPEC W26) ---
    const val CHAPTER_MIN_STRENGTH_RATIO = 1.5
}
