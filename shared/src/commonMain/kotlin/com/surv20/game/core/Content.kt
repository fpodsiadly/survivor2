package com.surv20.game.core

// Game content (SPEC W41, AGENTS.md): enemies now; skills, items, bosses and chapters move here in E2-E3.

// Enemy archetypes (SPEC W20 types 1-2); the rest arrives in E3. All numbers are placeholders.
// speedFactor is relative to the player's base speed; contactRatePerSecond is the share of base HP per second.
enum class EnemyType(val speedFactor: Float, val health: Float, val radius: Float, val contactRatePerSecond: Double) {
    WALKER(0.6f, 12f, 0.32f, 0.10),
    SWARMER(1.1f, 4f, 0.22f, 0.06),
}
