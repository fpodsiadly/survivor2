package com.surv20.game.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// Frame cap and fixed simulation step (SPEC W43: measured with frames capped at 60).
// Fed with every display vsync; shows at most `fps` frames per second (on 90/120 Hz screens some vsyncs are
// skipped) and tells how many fixed simulation steps the shown frame needs, so the run plays the same at any
// display rate.
class FramePacer(fps: Int = Balance.FRAME_RATE_CAP) {
    private val period = 1.0 / fps
    private var last = Double.NaN
    private var showAcc = 0.0
    private var sinceShown = 0.0
    private var simAcc = 0.0

    // Time since the previous shown frame (what the FPS meter records).
    var frameSeconds = 0.0
        private set

    // Fixed steps of Balance.SIM_STEP_SECONDS to run for the shown frame.
    var simSteps = 0
        private set

    // Returns true when this vsync shows a frame.
    fun onVsync(nowSeconds: Double): Boolean {
        if (last.isNaN()) {
            last = nowSeconds
            return false
        }
        val dt = max(0.0, nowSeconds - last)
        last = nowSeconds
        sinceShown += dt
        showAcc += dt
        if (showAcc + TOLERANCE < period) return false
        showAcc = (showAcc - period).coerceIn(-TOLERANCE, period) // an early frame is paid back by the next one

        frameSeconds = sinceShown
        sinceShown = 0.0
        val step = Balance.SIM_STEP_SECONDS
        simAcc += min(frameSeconds, step * Balance.MAX_SIM_STEPS_PER_FRAME)
        simSteps = min(floor((simAcc + TOLERANCE) / step).toInt(), Balance.MAX_SIM_STEPS_PER_FRAME)
        simAcc = max(-TOLERANCE, simAcc - simSteps * step)
        return true
    }

    private companion object {
        // Vsync timestamps jitter by a fraction of a millisecond; a frame this close to due is shown.
        const val TOLERANCE = 0.002
    }
}
