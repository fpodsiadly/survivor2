package com.surv20.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacerTest {
    // Feeds vsyncs at the display rate for the given time; returns shown frames and simulation steps.
    private fun drive(pacer: FramePacer, hz: Double, seconds: Double, start: Double = 0.0): Pair<Int, Int> {
        var shown = 0
        var steps = 0
        val n = (seconds * hz).toInt()
        for (k in 0..n) {
            if (pacer.onVsync(start + k / hz)) {
                shown++
                steps += pacer.simSteps
            }
        }
        return shown to steps
    }

    @Test
    fun sixtyHertzShowsEveryFrameWithOneStep() {
        val (shown, steps) = drive(FramePacer(), 60.0, 10.0)
        assertEquals(600, shown)
        assertEquals(600, steps)
    }

    @Test
    fun fastDisplaysAreCappedAtSixty() {
        for (hz in listOf(90.0, 120.0, 144.0)) {
            val pacer = FramePacer()
            val (shown, steps) = drive(pacer, hz, 10.0)
            assertEquals("shown at $hz Hz", 600.0, shown.toDouble(), 2.0)
            assertEquals("steps at $hz Hz", 600.0, steps.toDouble(), 2.0)
        }
    }

    @Test
    fun slowFramesCatchUpWithAtMostThreeSteps() {
        val pacer = FramePacer()
        pacer.onVsync(0.0)
        assertTrue(pacer.onVsync(1.0 / 30))
        assertEquals(2, pacer.simSteps)
        assertTrue(pacer.onVsync(1.0 / 30 + 0.5)) // a stall
        assertEquals(Balance.MAX_SIM_STEPS_PER_FRAME, pacer.simSteps)
        assertEquals(0.5, pacer.frameSeconds, 1e-9)
    }

    @Test
    fun jitterDoesNotDropFramesAtSixty() {
        val pacer = FramePacer()
        var t = 0.0
        var shown = 0
        pacer.onVsync(t)
        repeat(600) { k ->
            t += 1.0 / 60 + (if (k % 2 == 0) -0.0012 else 0.0012)
            if (pacer.onVsync(t)) shown++
        }
        assertEquals(600, shown)
    }
}
