package com.surv20.game.core.run

import com.surv20.game.core.Balance
import com.surv20.game.core.GameRandom
import com.surv20.game.core.Rect2
import com.surv20.game.core.Vec2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Side { TOP, BOTTOM, LEFT, RIGHT }

// World layout of a run (SPEC W3, W7, W8, W19; PLAN P3/P4/P16). One "ekran" = view width = 10 units at the 9:16
// reference; the view is always 10 wide and taller phones see more height.
class ArenaLayout(
    val format: MapFormat,
    // Screen height / width, e.g. 16/9 or 20/9.
    aspect: Float,
    val spawnMargin: Float = Balance.SPAWN_MARGIN,
    relocateRadiusFactor: Float = Balance.RELOCATE_RADIUS_FACTOR,
) {
    private val viewHeight: Float

    init {
        require(aspect >= 1f) { "Portrait only (SPEC W2)." }
        viewHeight = VIEW_WIDTH * aspect
    }

    val viewHalfSize = Vec2(VIEW_WIDTH / 2f, viewHeight / 2f)

    // Enemies farther than this from the player are relocated (SPEC W19).
    val relocateRadius: Float = relocateRadiusFactor * max(VIEW_WIDTH, viewHeight) / 2f

    // Closed map: 3 × 3 screens (SPEC W7).
    val closedBounds = Rect2(Vec2.ZERO, Vec2(1.5f * VIEW_WIDTH, 1.5f * viewHeight))

    // Corridor: 1.5 screens wide, infinite vertically (SPEC W8).
    val corridorHalfWidth = 0.75f * VIEW_WIDTH

    // Where the player may stand.
    fun clampPlayer(p: Vec2): Vec2 = when (format) {
        MapFormat.CLOSED -> closedBounds.clamp(p)
        MapFormat.CORRIDOR -> Vec2(p.x.coerceIn(-corridorHalfWidth, corridorHalfWidth), p.y)
        MapFormat.OPEN -> p
    }

    // Camera center: open map follows; closed map stays inside the bounds; corridor keeps the player visible.
    fun cameraFor(player: Vec2): Vec2 = when (format) {
        MapFormat.CLOSED -> Rect2(
            closedBounds.center,
            Vec2(closedBounds.halfSize.x - viewHalfSize.x, closedBounds.halfSize.y - viewHalfSize.y),
        ).clamp(player)
        MapFormat.CORRIDOR -> {
            val slack = corridorHalfWidth - viewHalfSize.x
            Vec2(player.x.coerceIn(-slack, slack), player.y)
        }
        MapFormat.OPEN -> player
    }

    fun viewRect(camera: Vec2) = Rect2(camera, viewHalfSize)

    // Sides a horde enters from (SPEC W11): open ≥3 of 4, corridor both ends, closed ≥3 map edges.
    fun hordeSides(random: GameRandom): List<Side> {
        if (format == MapFormat.CORRIDOR) return listOf(Side.TOP, Side.BOTTOM)
        val all = Side.entries.toMutableList()
        if (random.nextInt(0, 2) == 0) return all
        all.removeAt(random.nextInt(0, all.size))
        return all
    }

    // A spawn point just outside the view on the given side (closed map: on the map edge).
    fun spawnPoint(camera: Vec2, side: Side, random: GameRandom): Vec2 {
        val outer = if (format == MapFormat.CLOSED) closedBounds
        else Rect2(camera, Vec2(viewHalfSize.x + spawnMargin, viewHalfSize.y + spawnMargin))
        val t = random.nextDouble().toFloat()
        val point = when (side) {
            Side.TOP -> Vec2(lerp(outer.minX, outer.maxX, t), outer.maxY)
            Side.BOTTOM -> Vec2(lerp(outer.minX, outer.maxX, t), outer.minY)
            Side.LEFT -> Vec2(outer.minX, lerp(outer.minY, outer.maxY, t))
            Side.RIGHT -> Vec2(outer.maxX, lerp(outer.minY, outer.maxY, t))
        }
        return if (format == MapFormat.CORRIDOR) clampPlayer(point) else point
    }

    // SPEC W19: a far enemy moves just past the view edge on the side the player moves to (random when standing).
    // Corridor: to the open end in the move direction. Closed map: never relocated (null).
    fun relocate(enemy: Vec2, player: Vec2, camera: Vec2, moveDirection: Vec2, random: GameRandom): Vec2? {
        if (format == MapFormat.CLOSED) return null
        if (Vec2.distance(enemy, player) <= relocateRadius) return null
        val dir = moveDirection.normalized
        if (format == MapFormat.CORRIDOR) {
            val up = if (dir.lengthSquared > 0) dir.y >= 0 else random.nextInt(0, 2) == 0
            return spawnPoint(camera, if (up) Side.TOP else Side.BOTTOM, random)
        }
        val side = if (dir.lengthSquared > 0) sideOf(dir) else Side.entries[random.nextInt(0, 4)]
        return spawnPoint(camera, side, random)
    }

    companion object {
        const val VIEW_WIDTH = Balance.VIEW_WIDTH

        // The view side a direction points at.
        fun sideOf(d: Vec2): Side =
            if (abs(d.x) > abs(d.y)) (if (d.x > 0) Side.RIGHT else Side.LEFT) else (if (d.y > 0) Side.TOP else Side.BOTTOM)

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }
}

// Caps living enemies (SPEC "Wartości bazowe": 400); the surplus waits in a queue.
class SpawnQueue(val maxAlive: Int = Balance.MAX_ALIVE_ENEMIES) {
    private val waitingIds = ArrayDeque<String>()
    var alive = 0
        private set
    val waiting: Int get() = waitingIds.size

    init {
        require(maxAlive > 0)
    }

    // Requests spawns; returns the enemy ids that may spawn now (in request order).
    fun request(enemyIds: List<String>): List<String> {
        waitingIds.addAll(enemyIds)
        return release()
    }

    // Call when enemies die or are removed; returns ids that may now spawn.
    fun onRemoved(count: Int = 1): List<String> {
        alive = max(0, alive - count)
        return release()
    }

    // Boss entry clears the field and the queue (SPEC W10).
    fun clearAll() {
        alive = 0
        waitingIds.clear()
    }

    private fun release(): List<String> {
        val out = mutableListOf<String>()
        while (alive < maxAlive && waitingIds.isNotEmpty()) {
            out += waitingIds.removeFirst()
            alive++
        }
        return out
    }
}

// Boss arena circle (SPEC W10): centered on the player at boss entry, diameter ≈ 80% of view width.
class BossArena(val center: Vec2, diameterFraction: Float = Balance.ARENA_DIAMETER_FRACTION) {
    val radius: Float = ArenaLayout.VIEW_WIDTH * diameterFraction / 2f

    // Keeps a point (player or boss) inside the circle.
    fun clamp(position: Vec2, bodyRadius: Float = 0f): Vec2 {
        val limit = max(0f, radius - bodyRadius)
        val offset = position - center
        return if (offset.length <= limit) position else center + offset.normalized * min(limit, offset.length)
    }
}
