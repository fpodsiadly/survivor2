package com.surv20.game.core

import kotlin.random.Random

// Source of randomness for game rules; seeded in tests so draws are reproducible.
interface GameRandom {
    // Integer in [from, until).
    fun nextInt(from: Int, until: Int): Int

    // Double in [0, 1).
    fun nextDouble(): Double
}

class SeededRandom(seed: Int) : GameRandom {
    private val random = Random(seed)
    override fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)
    override fun nextDouble(): Double = random.nextDouble()
}
