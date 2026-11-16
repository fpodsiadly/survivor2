package com.surv20.game.core.run

import com.surv20.game.core.Balance
import com.surv20.game.core.GameRandom
import com.surv20.game.core.Vec2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Enemy archetypes of the prototype (SPEC W20 types 1-2); the rest arrives in E3. Numbers are placeholders.
enum class EnemyType(val speedFactor: Float, val health: Float, val radius: Float, val contactRatePerSecond: Double) {
    WALKER(0.6f, 12f, 0.32f, 0.10),
    SWARMER(1.1f, 4f, 0.22f, 0.06),
}

// Input of one simulation step.
data class RunInput(val move: Vec2)

// What happened during a step (for sounds, effects and the HUD).
class StepEvents {
    var levelUps = 0
    var kills = 0
    var hitsTaken = 0
    fun clear() {
        levelUps = 0
        kills = 0
        hitsTaken = 0
    }
}

// The live run as plain arrays (PLAN K5): enemies, projectiles and crystals are struct-of-arrays, updated in one
// step(dt, input) with a spatial hash for neighbour and hit queries. No physics engine. Pure Kotlin, tested.
// Prototype scope (E1): weapon B1 (thrown projectile at the nearest enemy), two enemy types, crystals and XP,
// constant spawn ramp. The timeline, skills, bosses and pickups plug in during E2-E3.
class RunSim(
    val layout: ArenaLayout,
    private val random: GameRandom,
    val stress: Boolean = false,
) {
    // --- Player ---
    var playerX = 0f
        private set
    var playerY = 0f
        private set
    var lastMove = Vec2.ZERO
        private set
    val health = PlayerHealth(Balance.PLAYER_MAX_HEALTH)
    val experience = ExperienceBar()
    var time = 0.0
        private set
    var kills = 0
        private set
    val dead: Boolean get() = health.isDead && !stress

    // --- Enemies ---
    val enemyCapacity = Balance.MAX_ALIVE_ENEMIES
    val enemyX = FloatArray(enemyCapacity)
    val enemyY = FloatArray(enemyCapacity)
    val enemyHp = FloatArray(enemyCapacity)
    val enemyType = IntArray(enemyCapacity)
    val enemyFlash = FloatArray(enemyCapacity)
    var enemyCount = 0
        private set

    // --- Projectiles ---
    val projectileCapacity = 512
    val projX = FloatArray(projectileCapacity)
    val projY = FloatArray(projectileCapacity)
    private val projVx = FloatArray(projectileCapacity)
    private val projVy = FloatArray(projectileCapacity)
    private val projLife = FloatArray(projectileCapacity)
    private val projDamage = FloatArray(projectileCapacity)
    var projectileCount = 0
        private set

    // --- Crystals ---
    val crystalCapacity = 600
    val crystalX = FloatArray(crystalCapacity)
    val crystalY = FloatArray(crystalCapacity)
    val crystalKind = IntArray(crystalCapacity) // CrystalKind.ordinal
    private val crystalValue = IntArray(crystalCapacity)
    private val crystalPulled = BooleanArray(crystalCapacity)
    var crystalCount = 0
        private set

    // --- Spatial hash of enemies ---
    private val cellHead = IntArray(GRID_BUCKETS)
    private val cellNext = IntArray(enemyCapacity)

    private val drops = CrystalDrops(random)
    private var weaponCooldown = 0.0
    private var spawnBudget = 0.0
    val events = StepEvents()

    fun step(dt: Double, input: RunInput): StepEvents {
        events.clear()
        if (dead) return events
        time += dt
        val dtf = dt.toFloat()
        movePlayer(dtf, input)
        if (stress) keepStressCounts() else spawnEnemies(dt)
        buildGrid()
        moveEnemies(dtf)
        fireWeapon(dt)
        moveProjectiles(dtf)
        moveCrystals(dtf)
        return events
    }

    // ----------------------------------------------------------------------------------------------------------------

    private fun movePlayer(dt: Float, input: RunInput) {
        val move = if (stress) Vec2(-sin(time.toFloat() * 0.6f), cos(time.toFloat() * 0.6f)) else input.move
        if (move.lengthSquared > 0f) lastMove = move.normalized
        val speed = Balance.PLAYER_MOVE_SPEED.toFloat()
        val next = layout.clampPlayer(Vec2(playerX + move.x * speed * dt, playerY + move.y * speed * dt))
        playerX = next.x
        playerY = next.y
    }

    private fun spawnEnemies(dt: Double) {
        // Placeholder ramp until the chapter timeline drives spawning (E3): 1.5/s rising by 0.1/s each second.
        spawnBudget += dt * (1.5 + time * 0.1)
        while (spawnBudget >= 1.0) {
            spawnBudget -= 1.0
            if (enemyCount >= enemyCapacity) continue
            val type = if (random.nextDouble() < 0.3) EnemyType.SWARMER else EnemyType.WALKER
            spawnEnemyAtEdge(type)
        }
    }

    private fun spawnEnemyAtEdge(type: EnemyType) {
        val camera = layout.cameraFor(Vec2(playerX, playerY))
        val side = Side.entries[random.nextInt(0, 4)]
        val p = layout.spawnPoint(camera, side, random)
        addEnemy(p.x, p.y, type)
    }

    private fun addEnemy(x: Float, y: Float, type: EnemyType) {
        if (enemyCount >= enemyCapacity) return
        val i = enemyCount++
        enemyX[i] = x
        enemyY[i] = y
        enemyHp[i] = type.health
        enemyType[i] = type.ordinal
        enemyFlash[i] = 0f
    }

    private fun removeEnemy(i: Int) {
        val last = --enemyCount
        enemyX[i] = enemyX[last]
        enemyY[i] = enemyY[last]
        enemyHp[i] = enemyHp[last]
        enemyType[i] = enemyType[last]
        enemyFlash[i] = enemyFlash[last]
    }

    private fun buildGrid() {
        cellHead.fill(-1)
        for (i in 0 until enemyCount) {
            val h = bucket(cellOf(enemyX[i]), cellOf(enemyY[i]))
            cellNext[i] = cellHead[h]
            cellHead[h] = i
        }
    }

    private fun moveEnemies(dt: Float) {
        val baseSpeed = Balance.PLAYER_MOVE_SPEED.toFloat()
        val relocateR2 = layout.relocateRadius * layout.relocateRadius
        var i = 0
        while (i < enemyCount) {
            val type = ENEMY_TYPES[enemyType[i]]
            val ex = enemyX[i]
            val ey = enemyY[i]
            var dx = playerX - ex
            var dy = playerY - ey
            val d2 = dx * dx + dy * dy

            // SPEC W19: far enemies come back just past the view edge on the side the player moves to.
            if (d2 > relocateR2) {
                val camera = layout.cameraFor(Vec2(playerX, playerY))
                val p = layout.relocate(Vec2(ex, ey), Vec2(playerX, playerY), camera, lastMove, random)
                if (p != null) {
                    enemyX[i] = p.x
                    enemyY[i] = p.y
                    i++
                    continue
                }
            }

            val d = sqrt(d2)
            if (d > 1e-4f) {
                dx /= d
                dy /= d
            }
            var vx = dx * type.speedFactor * baseSpeed
            var vy = dy * type.speedFactor * baseSpeed

            // Separation: push away from overlapping neighbours in the 3×3 cells around.
            var sx = 0f
            var sy = 0f
            val cx = cellOf(ex)
            val cy = cellOf(ey)
            for (ox in -1..1) for (oy in -1..1) {
                var j = cellHead[bucket(cx + ox, cy + oy)]
                while (j >= 0) {
                    if (j != i) {
                        val rx = ex - enemyX[j]
                        val ry = ey - enemyY[j]
                        val r2 = rx * rx + ry * ry
                        val minDist = type.radius + ENEMY_TYPES[enemyType[j]].radius
                        if (r2 < minDist * minDist && r2 > 1e-8f) {
                            val r = sqrt(r2)
                            val push = (minDist - r) / r
                            sx += rx * push
                            sy += ry * push
                        }
                    }
                    j = cellNext[j]
                }
            }
            // Position correction (half the overlap per frame, capped) keeps the crowd readable (SPEC W21)
            // instead of melting into one blob as a velocity push did.
            val sl = sqrt(sx * sx + sy * sy)
            if (sl > SEPARATION_MAX_STEP) {
                sx *= SEPARATION_MAX_STEP / sl
                sy *= SEPARATION_MAX_STEP / sl
            }
            var nx = ex + vx * dt + sx * SEPARATION_SHARE
            var ny = ey + vy * dt + sy * SEPARATION_SHARE
            // Enemies press against the drone instead of entering it, so the crowd rings the player.
            val bodyGap = type.radius + PLAYER_RADIUS
            val px = nx - playerX
            val py = ny - playerY
            val pd2 = px * px + py * py
            if (pd2 < bodyGap * bodyGap && pd2 > 1e-8f) {
                val pd = sqrt(pd2)
                nx = playerX + px / pd * bodyGap
                ny = playerY + py / pd * bodyGap
            }
            if (layout.format == MapFormat.OPEN) {
                enemyX[i] = nx
                enemyY[i] = ny
            } else {
                val pos = layout.clampPlayer(Vec2(nx, ny))
                enemyX[i] = pos.x
                enemyY[i] = pos.y
            }
            if (enemyFlash[i] > 0f) enemyFlash[i] = max(0f, enemyFlash[i] - dt)

            // Contact damage (SPEC W13): per hit = rate × invulnerability window × base HP.
            val touch = type.radius + PLAYER_RADIUS
            if (d < touch + CONTACT_MARGIN && !stress) {
                val taken = health.takeHit(time, PlayerHealth.contactHit(type.contactRatePerSecond, Balance.PLAYER_MAX_HEALTH), 0.0)
                if (taken > 0) events.hitsTaken++
            }
            i++
        }
    }

    private fun fireWeapon(dt: Double) {
        weaponCooldown -= dt
        if (weaponCooldown > 0) return
        val target = nearestEnemyInView() ?: return
        weaponCooldown = B1_COOLDOWN
        val dx = enemyX[target] - playerX
        val dy = enemyY[target] - playerY
        val d = max(1e-4f, sqrt(dx * dx + dy * dy))
        addProjectile(playerX, playerY, dx / d * B1_SPEED, dy / d * B1_SPEED, B1_DAMAGE)
    }

    private fun nearestEnemyInView(): Int? {
        val half = layout.viewHalfSize
        var best = -1
        var bestD2 = Float.MAX_VALUE
        for (i in 0 until enemyCount) {
            val dx = enemyX[i] - playerX
            val dy = enemyY[i] - playerY
            if (dx > half.x || dx < -half.x || dy > half.y || dy < -half.y) continue
            val d2 = dx * dx + dy * dy
            if (d2 < bestD2) {
                bestD2 = d2
                best = i
            }
        }
        return if (best >= 0) best else null
    }

    private fun addProjectile(x: Float, y: Float, vx: Float, vy: Float, damage: Float) {
        if (projectileCount >= projectileCapacity) return
        val i = projectileCount++
        projX[i] = x
        projY[i] = y
        projVx[i] = vx
        projVy[i] = vy
        projLife[i] = PROJECTILE_LIFE
        projDamage[i] = damage
    }

    private fun removeProjectile(i: Int) {
        val last = --projectileCount
        projX[i] = projX[last]
        projY[i] = projY[last]
        projVx[i] = projVx[last]
        projVy[i] = projVy[last]
        projLife[i] = projLife[last]
        projDamage[i] = projDamage[last]
    }

    private fun moveProjectiles(dt: Float) {
        var i = 0
        while (i < projectileCount) {
            projX[i] += projVx[i] * dt
            projY[i] += projVy[i] * dt
            projLife[i] -= dt
            val hit = enemyAt(projX[i], projY[i], PROJECTILE_RADIUS)
            // Zero-damage projectiles (stress scenario) still pay for the hit test but pass through.
            if (hit >= 0 && projDamage[i] > 0f) {
                damageEnemy(hit, projDamage[i])
                removeProjectile(i)
                continue
            }
            if (projLife[i] <= 0f) {
                removeProjectile(i)
                continue
            }
            i++
        }
    }

    private fun enemyAt(x: Float, y: Float, radius: Float): Int {
        val cx = cellOf(x)
        val cy = cellOf(y)
        for (ox in -1..1) for (oy in -1..1) {
            var j = cellHead[bucket(cx + ox, cy + oy)]
            while (j >= 0) {
                if (j < enemyCount) {
                    val rx = x - enemyX[j]
                    val ry = y - enemyY[j]
                    val reach = radius + ENEMY_TYPES[enemyType[j]].radius
                    if (rx * rx + ry * ry <= reach * reach) return j
                }
                j = cellNext[j]
            }
        }
        return -1
    }

    private fun damageEnemy(i: Int, damage: Float) {
        enemyHp[i] -= damage
        enemyFlash[i] = HIT_FLASH_SECONDS
        if (enemyHp[i] > 0f) return
        val kind = drops.drop(EnemyClass.NORMAL)
        addCrystal(enemyX[i], enemyY[i], kind)
        kills++
        events.kills++
        // The grid still points at this slot this frame; moving the last enemy here keeps lookups valid enough
        // for one frame (a projectile may test the moved enemy's old cell), and the grid is rebuilt next step.
        removeEnemy(i)
    }

    private fun addCrystal(x: Float, y: Float, kind: CrystalKind) {
        if (kind == CrystalKind.NONE) return
        if (crystalCount >= crystalCapacity) {
            // Full: fold the value into the oldest crystal instead of dropping it (PLAN P5).
            crystalValue[0] += CrystalDrops.value(kind)
            return
        }
        val i = crystalCount++
        crystalX[i] = x
        crystalY[i] = y
        crystalKind[i] = kind.ordinal
        crystalValue[i] = CrystalDrops.value(kind)
        crystalPulled[i] = false
    }

    private fun removeCrystal(i: Int) {
        val last = --crystalCount
        crystalX[i] = crystalX[last]
        crystalY[i] = crystalY[last]
        crystalKind[i] = crystalKind[last]
        crystalValue[i] = crystalValue[last]
        crystalPulled[i] = crystalPulled[last]
    }

    private fun moveCrystals(dt: Float) {
        val pickup = Balance.PLAYER_PICKUP_RADIUS.toFloat()
        var i = 0
        while (i < crystalCount) {
            val dx = playerX - crystalX[i]
            val dy = playerY - crystalY[i]
            val d = sqrt(dx * dx + dy * dy)
            if (d < COLLECT_DISTANCE) {
                events.levelUps += experience.add(crystalValue[i].toLong())
                removeCrystal(i)
                continue
            }
            if (d < pickup) crystalPulled[i] = true
            if (crystalPulled[i]) {
                val step = min(d, CRYSTAL_PULL_SPEED * dt)
                crystalX[i] += dx / d * step
                crystalY[i] += dy / d * step
            }
            i++
        }
    }

    // Stress scenario (SPEC W43; PLAN P6): the player circles and cannot die; counts are topped up every step.
    private fun keepStressCounts() {
        while (enemyCount < STRESS_ENEMIES) {
            val angle = random.nextDouble() * 6.2832
            val r = 2f + random.nextDouble().toFloat() * 6f
            addEnemy(playerX + (cos(angle) * r).toFloat(), playerY + (sin(angle) * r).toFloat(), EnemyType.entries[random.nextInt(0, 2)])
        }
        while (projectileCount < STRESS_PROJECTILES) {
            val angle = random.nextDouble() * 6.2832
            addProjectile(playerX, playerY, (cos(angle) * B1_SPEED).toFloat(), (sin(angle) * B1_SPEED).toFloat(), 0f)
        }
        while (crystalCount < STRESS_CRYSTALS) {
            val x = playerX + (random.nextDouble().toFloat() - 0.5f) * layout.viewHalfSize.x * 2f
            val y = playerY + (random.nextDouble().toFloat() - 0.5f) * layout.viewHalfSize.y * 2f
            addCrystal(x, y, CrystalKind.entries[1 + random.nextInt(0, 3)])
        }
        for (i in 0 until enemyCount) enemyHp[i] = max(enemyHp[i], 1f) // projectiles must not thin the crowd
    }

    companion object {
        const val PLAYER_RADIUS = 0.35f
        const val PROJECTILE_RADIUS = 0.12f
        const val COLLECT_DISTANCE = 0.4f
        const val CRYSTAL_PULL_SPEED = 12f
        const val HIT_FLASH_SECONDS = 0.1f // SPEC W39
        const val CONTACT_MARGIN = 0.05f // enemies pressed against the drone still touch it
        const val SEPARATION_SHARE = 0.5f // share of the overlap removed per frame
        const val SEPARATION_MAX_STEP = 0.25f // world units per frame, placeholder
        const val PROJECTILE_LIFE = 1.6f
        const val B1_COOLDOWN = 0.8 // placeholder
        const val B1_SPEED = 9f // placeholder
        const val B1_DAMAGE = 15f // 10 ATK × 1.5 at level 1 (RESEARCH §5.1)

        // Stress scenario counts (SPEC W43).
        const val STRESS_ENEMIES = 300
        const val STRESS_PROJECTILES = 200
        const val STRESS_CRYSTALS = 300

        private const val CELL_SIZE = 1f
        private const val GRID_BUCKETS = 4096
        private val ENEMY_TYPES = EnemyType.entries

        private fun cellOf(v: Float): Int = floor(v / CELL_SIZE).toInt()
        private fun bucket(cx: Int, cy: Int): Int = ((cx * 73856093) xor (cy * 19349663)) and (GRID_BUCKETS - 1)
    }
}
