package com.surv20.game.core.run

import com.surv20.game.core.Vec2
import kotlin.math.min

// Floating joystick (SPEC W1; PLAN P3): a touch in the lower 2/3 of the screen places the stick; dragging moves the
// player with speed proportional to the drag up to the max radius; lifting stops. Only the first finger steers.
// Screen coordinates: y grows downward. direction() returns world coordinates: y grows upward.
class Joystick(private val maxRadiusPx: Float, private val deadZonePx: Float) {
    private var pointerId: Long? = null

    var origin: Vec2? = null
        private set
    var current: Vec2? = null
        private set

    val active: Boolean get() = pointerId != null

    // Returns true when this touch now controls the stick.
    fun onDown(id: Long, position: Vec2, screenHeight: Float): Boolean {
        if (pointerId != null) return false
        if (position.y < screenHeight / 3f) return false
        pointerId = id
        origin = position
        current = position
        return true
    }

    fun onMove(id: Long, position: Vec2) {
        if (id == pointerId) current = position
    }

    fun onUp(id: Long) {
        if (id != pointerId) return
        pointerId = null
        origin = null
        current = null
    }

    // Movement in world space: length 0 (inside the dead zone) up to 1 (at the max radius).
    fun direction(): Vec2 {
        val o = origin ?: return Vec2.ZERO
        val c = current ?: return Vec2.ZERO
        val drag = Vec2(c.x - o.x, o.y - c.y)
        val length = drag.length
        if (length <= deadZonePx) return Vec2.ZERO
        val strength = min(1f, (length - deadZonePx) / (maxRadiusPx - deadZonePx))
        return drag.normalized * strength
    }

    // Knob position on screen, kept within the max radius (for drawing).
    fun knob(): Vec2? {
        val o = origin ?: return null
        val c = current ?: return null
        val drag = c - o
        return if (drag.length <= maxRadiusPx) c else o + drag.normalized * maxRadiusPx
    }
}
