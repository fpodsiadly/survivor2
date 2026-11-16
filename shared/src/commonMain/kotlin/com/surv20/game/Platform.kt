package com.surv20.game

import com.surv20.game.core.KeyValueStore

// What differs between Android and iOS. Everything else (rules, screens, drawing) is shared.
interface GamePlatform {
    // Version shown on the main screen, e.g. "0.1.0 · build 2026-11-16 15:00".
    val version: String

    // Storage for the save (SPEC W42, PLAN.md K6).
    val store: KeyValueStore

    // Monotonic clock in ms, used for frame timing outside Compose.
    fun elapsedMillis(): Long

    // True in debug builds: shows the developer menu and FPS meter (SPEC W43, PLAN K11).
    val isDebugBuild: Boolean

    // Developer log line (Android: logcat tag "Surv20"); used for the W43 stress result (PLAN P6).
    fun log(message: String)
}
