package com.surv20.game.core.run

import com.surv20.game.core.Balance
import com.surv20.game.core.EnemyType
import com.surv20.game.core.FrameStats
import com.surv20.game.core.SeededRandom
import com.surv20.game.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class JoystickTest {
    private val screenH = 1600f

    @Test
    fun touchInTopThirdDoesNotCreateAStick() {
        val stick = Joystick(maxRadiusPx = 100f, deadZonePx = 10f)
        assertFalse(stick.onDown(1, Vec2(500f, 400f), screenH))
        assertFalse(stick.active)
        assertTrue(stick.onDown(1, Vec2(500f, 1200f), screenH))
    }

    @Test
    fun secondFingerIsIgnoredAndReleaseStops() {
        val stick = Joystick(100f, 10f)
        stick.onDown(1, Vec2(500f, 1200f), screenH)
        assertFalse(stick.onDown(2, Vec2(800f, 1300f), screenH))
        stick.onMove(2, Vec2(900f, 1300f))
        stick.onMove(1, Vec2(600f, 1200f))
        assertEquals(1f, stick.direction().x, 1e-5f)
        stick.onUp(2)
        assertTrue(stick.active)
        stick.onUp(1)
        assertEquals(Vec2.ZERO, stick.direction())
        assertNull(stick.knob())
    }

    @Test
    fun speedGrowsWithDragUpToMaxAndScreenUpIsWorldUp() {
        val stick = Joystick(110f, 10f)
        stick.onDown(1, Vec2(500f, 1200f), screenH)
        stick.onMove(1, Vec2(500f, 1195f))
        assertEquals("dead zone", Vec2.ZERO, stick.direction())
        stick.onMove(1, Vec2(500f, 1140f))
        assertEquals(0.5f, stick.direction().y, 1e-5f)
        stick.onMove(1, Vec2(500f, 900f))
        assertEquals(1f, stick.direction().y, 1e-5f)
        assertEquals(1090f, stick.knob()!!.y, 1e-3f)
    }
}

class FrameStatsTest {
    @Test
    fun averageAndOnePercentLow() {
        val stats = FrameStats()
        repeat(990) { stats.record(1.0 / 60) }
        repeat(10) { stats.record(1.0 / 20) }
        assertEquals(1000 / (990.0 / 60 + 10.0 / 20), stats.averageFps(), 1e-6)
        assertEquals(20.0, stats.onePercentLowFps(), 1e-6)
    }

    @Test
    fun keepsOnlyTheLastWindow() {
        val stats = FrameStats(windowSeconds = 1.0)
        repeat(100) { stats.record(1.0 / 10) } // 10 s of slow frames
        repeat(60) { stats.record(1.0 / 60) } // then 1 s of 60 FPS
        assertEquals(60.0, stats.averageFps(), 0.5)
        assertTrue(stats.windowFilledSeconds <= 1.0 + 1e-9)
    }
}

class RunSimTest {
    private fun sim(stress: Boolean = false, format: MapFormat = MapFormat.OPEN) =
        RunSim(ArenaLayout(format, 16f / 9f), SeededRandom(11), stress)

    private fun run(s: RunSim, seconds: Double, move: Vec2 = Vec2.ZERO) {
        var t = 0.0
        while (t < seconds) {
            s.step(1.0 / 60, RunInput(move))
            t += 1.0 / 60
        }
    }

    @Test
    fun playerMovesWithInputAtBaseSpeed() {
        val s = sim()
        run(s, 1.0, Vec2(1f, 0f))
        assertEquals(3f, s.playerX, 0.06f)
    }

    @Test
    fun enemiesSpawnOutsideTheViewAndWalkTowardThePlayer() {
        val s = sim()
        while (s.enemyCount == 0) s.step(1.0 / 60, RunInput(Vec2.ZERO))
        assertFalse("spawned outside the view", s.layout.viewRect(Vec2.ZERO).contains(Vec2(s.enemyX[0], s.enemyY[0])))
        val before = (0 until s.enemyCount).map { dist(s.enemyX[it], s.enemyY[it]) }
        s.step(0.1, RunInput(Vec2.ZERO))
        val after = (0 until s.enemyCount).map { dist(s.enemyX[it], s.enemyY[it]) }
        assertTrue(after.average() < before.average())
    }

    @Test
    fun weaponKillsEnemiesWhichDropCrystalsThatGiveXp() {
        val s = sim()
        run(s, 40.0)
        assertTrue("kills ${s.kills}", s.kills > 10)
        assertTrue("level ${s.experience.level}", s.experience.level > 1 || s.experience.xp > 0)
    }

    @Test
    fun crowdSeparatesInsteadOfStackingOnOnePoint() {
        val s = sim(stress = true)
        run(s, 5.0)
        // Pairs overlapping by more than half the smaller body: a melted blob has thousands.
        var deep = 0
        for (i in 0 until s.enemyCount) for (j in i + 1 until s.enemyCount) {
            val dx = s.enemyX[i] - s.enemyX[j]
            val dy = s.enemyY[i] - s.enemyY[j]
            if (dx * dx + dy * dy < 0.2f * 0.2f) deep++
        }
        assertTrue("deeply overlapping pairs $deep", deep < s.enemyCount / 4)
    }

    @Test
    fun farEnemiesAreRelocatedNearTheView() {
        val s = sim()
        run(s, 5.0)
        run(s, 20.0, Vec2(1f, 0f)) // run away for 60 units
        val half = s.layout.viewHalfSize
        val far = (0 until s.enemyCount).count { dist(s.enemyX[it] - s.playerX, s.enemyY[it] - s.playerY) > s.layout.relocateRadius + 1f }
        assertEquals(0, far)
        assertTrue(half.x > 0)
    }

    @Test
    fun stressScenarioHoldsTheCountsAndThePlayerCannotDie() {
        val s = sim(stress = true)
        run(s, 3.0)
        assertTrue(s.enemyCount >= RunSim.STRESS_ENEMIES)
        assertTrue(s.projectileCount >= RunSim.STRESS_PROJECTILES - 5)
        assertTrue(s.crystalCount >= RunSim.STRESS_CRYSTALS - 5)
        assertFalse(s.dead)
    }

    @Test
    fun enemyLimitIsFourHundred() {
        val s = sim()
        run(s, 240.0, Vec2(0.3f, 0.2f))
        assertTrue(s.enemyCount <= 400)
    }

    @Test
    fun corridorKeepsEnemiesBetweenTheWalls() {
        val s = sim(format = MapFormat.CORRIDOR)
        run(s, 20.0)
        for (i in 0 until s.enemyCount) assertTrue(kotlin.math.abs(s.enemyX[i]) <= s.layout.corridorHalfWidth + 1e-3f)
    }

    // Not a pass/fail check: prints the JVM cost of one stress step for the record (PLAN P6).
    @Test
    fun stressStepCostOnTheJvm() {
        val s = sim(stress = true)
        run(s, 2.0)
        val steps = 1200
        val start = System.nanoTime()
        repeat(steps) { s.step(1.0 / 60, RunInput(Vec2.ZERO)) }
        val msPerStep = (System.nanoTime() - start) / 1e6 / steps
        println("RunSim stress step: %.3f ms (enemies=%d, projectiles=%d, crystals=%d)".format(msPerStep, s.enemyCount, s.projectileCount, s.crystalCount))
        assertTrue(msPerStep < 16.0)
    }

    private fun dist(x: Float, y: Float) = sqrt(x * x + y * y)
}

class RunSimContactTest {
    @Test
    fun enemiesPressAgainstTheDroneAndStillHurtIt() {
        val s = RunSim(ArenaLayout(MapFormat.OPEN, 16f / 9f), SeededRandom(3))
        var t = 0.0
        while (t < 60.0 && !s.dead) {
            s.step(1.0 / 60, RunInput(Vec2.ZERO))
            t += 1.0 / 60
        }
        assertTrue("standing still must eventually get hurt", s.health.health < s.health.maxHealth)
        for (i in 0 until s.enemyCount) {
            val dx = s.enemyX[i] - s.playerX
            val dy = s.enemyY[i] - s.playerY
            assertTrue("enemy inside the drone", sqrt(dx * dx + dy * dy) >= Balance.PLAYER_RADIUS + 0.2f - 1e-3f)
        }
    }
}

// Final check fixes 4-8 (PLAN E1): relocation side, weapon targeting, crystal merge, stress scenario, spawn queue.
class RunSimRulesTest {
    private fun sim(stress: Boolean = false, seed: Int = 5) =
        RunSim(ArenaLayout(MapFormat.OPEN, 16f / 9f), SeededRandom(seed), stress)

    private fun run(s: RunSim, seconds: Double, move: Vec2 = Vec2.ZERO) {
        var t = 0.0
        while (t < seconds) {
            s.step(1.0 / 60, RunInput(move))
            t += 1.0 / 60
        }
    }

    @Test
    fun standingPlayerGetsFarEnemiesBackOnRandomSides() {
        val s = sim()
        run(s, 12.0, Vec2(1f, 0f)) // last move: right
        assertTrue("enemies ${s.enemyCount}", s.enemyCount >= 8)
        for (i in 0 until s.enemyCount) s.enemyX[i] = s.playerX - 100f
        s.step(1.0 / 60, RunInput(Vec2.ZERO))
        val half = s.layout.viewHalfSize
        val right = (0 until s.enemyCount).count { s.enemyX[it] > s.playerX + half.x }
        assertTrue("all ${s.enemyCount} relocated to the last move side", right < s.enemyCount)
    }

    @Test
    fun movingPlayerGetsFarEnemiesBackOnTheMoveSide() {
        val s = sim()
        run(s, 12.0, Vec2(1f, 0f))
        for (i in 0 until s.enemyCount) s.enemyX[i] = s.playerX - 100f
        s.step(1.0 / 60, RunInput(Vec2(1f, 0f)))
        val half = s.layout.viewHalfSize
        for (i in 0 until s.enemyCount) assertTrue(s.enemyX[i] > s.playerX + half.x)
    }

    @Test
    fun weaponShootsTheNearestEnemyOnScreen() {
        val s = sim()
        s.addEnemy(3f, 0f, EnemyType.WALKER)
        s.addEnemy(0f, -2f, EnemyType.WALKER)
        s.step(1.0 / 60, RunInput(Vec2.ZERO))
        assertEquals(1, s.projectileCount)
        assertTrue("flies down", s.projY[0] < 0f)
        assertEquals(0f, s.projX[0], 1e-3f)
    }

    @Test
    fun weaponIgnoresEnemiesOffScreenEvenWhenNearer() {
        val s = sim()
        val half = s.layout.viewHalfSize
        s.addEnemy(half.x + 0.5f, 0f, EnemyType.WALKER) // off screen, distance ≈ 5.5
        s.addEnemy(half.x - 0.5f, half.y - 0.5f, EnemyType.WALKER) // on screen, in the corner, farther
        s.step(1.0 / 60, RunInput(Vec2.ZERO))
        assertEquals(1, s.projectileCount)
        assertTrue("flies to the on-screen enemy (up)", s.projY[0] > 0.05f)
    }

    @Test
    fun noShotWithoutEnemiesOnScreen() {
        val s = sim()
        s.addEnemy(s.layout.viewHalfSize.x + 3f, 0f, EnemyType.WALKER)
        s.step(1.0 / 60, RunInput(Vec2.ZERO))
        assertEquals(0, s.projectileCount)
    }

    @Test
    fun atThreeHundredCrystalsTheOldestMergeAndValueIsKept() {
        val s = sim()
        repeat(300) { s.addCrystal(100f + it, 50f, CrystalKind.GREEN) }
        s.removeCrystal(0) // the newest moves into slot 0: slot order is no longer age order
        s.addCrystal(500f, 50f, CrystalKind.GREEN)
        s.addCrystal(600f, 50f, CrystalKind.YELLOW)
        assertEquals(300, s.crystalCount)
        assertEquals(299 + 1 + 25, (0 until s.crystalCount).sumOf { s.crystalValue[it] })
        val oldest = (0 until s.crystalCount).single { s.crystalX[it] == 101f }
        assertEquals("the two oldest (x=101, x=102) merged", 2, s.crystalValue[oldest])
        assertFalse((0 until s.crystalCount).any { s.crystalX[it] == 102f })
    }

    @Test
    fun stressScenarioDoesNotKillOrHurt() {
        val s = sim(stress = true)
        run(s, 5.0)
        assertEquals(0, s.kills)
        assertEquals(s.health.maxHealth, s.health.health, 1e-9)
    }

    @Test
    fun surplusOverTheLimitWaitsAndFillsFreedSlots() {
        val s = sim()
        // The player is healed every step (as by a meat pickup) so the run lasts until the crowd hits the limit.
        fun tick() {
            s.health.heal(s.health.maxHealth)
            s.step(1.0 / 60, RunInput(Vec2(0.3f, 0.2f)))
        }
        var t = 0.0
        while (t < 600.0 && !(s.enemyCount == s.enemyCapacity && s.spawnWaiting > 0)) {
            tick()
            t += 1.0 / 60
        }
        assertEquals(s.enemyCapacity, s.enemyCount)
        assertTrue("surplus queued", s.spawnWaiting > 0)
        val kills = s.kills
        while (s.kills == kills) tick()
        tick()
        assertEquals("freed slot refilled from the queue", s.enemyCapacity, s.enemyCount)
    }
}
