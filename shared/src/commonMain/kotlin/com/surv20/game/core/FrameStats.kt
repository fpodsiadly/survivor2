package com.surv20.game.core

// Frame-rate meter for the performance gate (SPEC W43; PLAN P6): average FPS and "1% low" (the average FPS of the
// slowest 1% of frames) over a sliding window of the last windowSeconds.
class FrameStats(private val windowSeconds: Double = 60.0, capacity: Int = 60 * 150) {
    private val times = DoubleArray(capacity)
    private var start = 0
    private var count = 0
    private var total = 0.0

    val frames: Int get() = count
    val windowFilledSeconds: Double get() = total

    fun record(frameSeconds: Double) {
        if (frameSeconds <= 0) return
        if (count == times.size) dropOldest()
        times[(start + count) % times.size] = frameSeconds
        count++
        total += frameSeconds
        while (total > windowSeconds && count > 1) dropOldest()
    }

    fun averageFps(): Double = if (total <= 0) 0.0 else count / total

    fun onePercentLowFps(): Double {
        if (count == 0) return 0.0
        val sorted = DoubleArray(count) { times[(start + it) % times.size] }.also { it.sortDescending() }
        val n = maxOf(1, count / 100)
        var sum = 0.0
        for (i in 0 until n) sum += sorted[i]
        return n / sum
    }

    fun reset() {
        start = 0
        count = 0
        total = 0.0
    }

    private fun dropOldest() {
        total -= times[start]
        start = (start + 1) % times.size
        count--
    }
}
