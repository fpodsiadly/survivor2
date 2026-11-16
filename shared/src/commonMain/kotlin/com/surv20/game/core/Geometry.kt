package com.surv20.game.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// Minimal 2D vector for engine-independent rules.
data class Vec2(val x: Float, val y: Float) {
    val length: Float get() = sqrt(x * x + y * y)
    val lengthSquared: Float get() = x * x + y * y

    val normalized: Vec2
        get() {
            val l = length
            return if (l < 1e-6f) ZERO else Vec2(x / l, y / l)
        }

    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec2(x * k, y * k)

    companion object {
        val ZERO = Vec2(0f, 0f)
        fun distance(a: Vec2, b: Vec2): Float = (a - b).length
    }
}

// Axis-aligned rectangle given by its center and half size.
data class Rect2(val center: Vec2, val halfSize: Vec2) {
    val minX: Float get() = center.x - halfSize.x
    val maxX: Float get() = center.x + halfSize.x
    val minY: Float get() = center.y - halfSize.y
    val maxY: Float get() = center.y + halfSize.y

    fun contains(p: Vec2): Boolean = p.x in minX..maxX && p.y in minY..maxY
    fun clamp(p: Vec2): Vec2 = Vec2(max(minX, min(maxX, p.x)), max(minY, min(maxY, p.y)))
}
