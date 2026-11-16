package com.surv20.game.core.run

import com.surv20.game.core.GameRandom
import com.surv20.game.core.SeededRandom
import com.surv20.game.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ArenaLayoutTest {
    private val rng: GameRandom = SeededRandom(5)
    private val tall = 16f / 9f

    @Test
    fun viewIsAlwaysTenWideTallerPhonesSeeMoreHeight() {
        for (aspect in listOf(16f / 9f, 20f / 9f)) {
            val layout = ArenaLayout(MapFormat.OPEN, aspect)
            assertEquals(5f, layout.viewHalfSize.x, 0f)
            assertEquals(5f * aspect, layout.viewHalfSize.y, 1e-5f)
        }
    }

    @Test
    fun relocatesPastTheViewEdgeOnTheMoveSide() {
        val layout = ArenaLayout(MapFormat.OPEN, tall)
        val p = Vec2.ZERO
        val edgeX = layout.viewHalfSize.x + layout.spawnMargin
        val edgeY = layout.viewHalfSize.y + layout.spawnMargin
        assertEquals(edgeX, layout.relocate(Vec2(-40f, 0f), p, p, Vec2(1f, 0f), rng)!!.x, 1e-5f)
        assertEquals(edgeY, layout.relocate(Vec2(-40f, 0f), p, p, Vec2(0f, 1f), rng)!!.y, 1e-5f)
        assertEquals(-edgeY, layout.relocate(Vec2(-40f, 0f), p, p, Vec2(0.2f, -1f), rng)!!.y, 1e-5f)
        val left = layout.relocate(Vec2(40f, 0f), p, p, Vec2(-1f, 0f), rng)!!
        assertEquals(-edgeX, left.x, 1e-5f)
        assertFalse("outside the view", layout.viewRect(p).contains(left))
    }

    @Test
    fun standingPlayerRelocatesToSomeSideOutsideTheView() {
        val layout = ArenaLayout(MapFormat.OPEN, tall)
        repeat(50) { seed ->
            val p = layout.relocate(Vec2(50f, 50f), Vec2.ZERO, Vec2.ZERO, Vec2.ZERO, SeededRandom(seed))!!
            assertFalse(layout.viewRect(Vec2.ZERO).contains(p))
        }
    }

    @Test
    fun nearEnemiesAndClosedMapAreNeverRelocated() {
        assertNull(ArenaLayout(MapFormat.OPEN, tall).relocate(Vec2(3f, 3f), Vec2.ZERO, Vec2.ZERO, Vec2(1f, 0f), rng))
        assertNull(ArenaLayout(MapFormat.CLOSED, tall).relocate(Vec2(500f, 0f), Vec2.ZERO, Vec2.ZERO, Vec2(1f, 0f), rng))
    }

    @Test
    fun corridorRelocatesToTheOpenEndInTheMoveDirectionInsideTheWalls() {
        val layout = ArenaLayout(MapFormat.CORRIDOR, tall)
        val p = layout.relocate(Vec2(0f, -60f), Vec2.ZERO, Vec2.ZERO, Vec2(0.9f, 0.1f), rng)!!
        assertTrue(p.y > layout.viewHalfSize.y)
        assertTrue(abs(p.x) <= layout.corridorHalfWidth)
    }

    @Test
    fun hordeSidesPerFormat() {
        assertEquals(setOf(Side.TOP, Side.BOTTOM), ArenaLayout(MapFormat.CORRIDOR, 2f).hordeSides(rng).toSet())
        for (format in listOf(MapFormat.OPEN, MapFormat.CLOSED)) {
            val layout = ArenaLayout(format, 2f)
            repeat(50) { seed ->
                val sides = layout.hordeSides(SeededRandom(seed))
                assertTrue(sides.size >= 3)
                assertEquals(sides.size, sides.distinct().size)
            }
        }
    }

    @Test
    fun closedMapClampsPlayerAndCameraToTheBounds() {
        val layout = ArenaLayout(MapFormat.CLOSED, tall)
        val b = layout.closedBounds
        val player = layout.clampPlayer(Vec2(1000f, -1000f))
        val camera = layout.cameraFor(player)
        assertEquals(b.maxX, player.x, 0f)
        assertEquals(b.minY, player.y, 0f)
        assertEquals("view edge on the wall", b.maxX, camera.x + layout.viewHalfSize.x, 1e-4f)
        assertEquals(b.minY, camera.y - layout.viewHalfSize.y, 1e-4f)
    }

    @Test
    fun corridorKeepsPlayerBetweenWallsAndVisible() {
        val layout = ArenaLayout(MapFormat.CORRIDOR, tall)
        val player = layout.clampPlayer(Vec2(99f, 12f))
        assertEquals(layout.corridorHalfWidth, player.x, 0f)
        assertEquals(12f, player.y, 0f)
        assertTrue(layout.viewRect(layout.cameraFor(player)).contains(player))
    }
}

class SpawnQueueAndBossArenaTest {
    @Test
    fun surplusWaitsAndSpawnsWhenEnemiesDie() {
        val queue = SpawnQueue(maxAlive = 3)
        assertEquals(listOf("a", "b", "c"), queue.request(listOf("a", "b", "c", "d", "e")))
        assertEquals(2, queue.waiting)
        assertEquals(listOf("d"), queue.onRemoved())
        queue.clearAll()
        assertEquals(0, queue.alive)
        assertEquals(0, queue.waiting)
    }

    @Test
    fun arenaCircleIsEightyPercentOfViewWidthAndKeepsThePlayerIn() {
        val arena = BossArena(Vec2(10f, 10f))
        assertEquals(4f, arena.radius, 1e-6f)
        assertEquals(14f, arena.clamp(Vec2(30f, 10f)).x, 1e-5f)
        assertEquals(Vec2(11f, 11f), arena.clamp(Vec2(11f, 11f)))
    }
}

class BombTimerChargerBossTest {
    @Test
    fun bombKillsRegularEnemiesOnScreenAndTakesTenPercentFromElitesAndBosses() {
        val hits = Bomb.explode(
            listOf(
                BombTarget(EnemyClass.NORMAL, 50.0, 50.0, onScreen = true),
                BombTarget(EnemyClass.REINFORCED, 80.0, 30.0, onScreen = true),
                BombTarget(EnemyClass.NORMAL, 50.0, 50.0, onScreen = false),
                BombTarget(EnemyClass.ELITE, 1000.0, 1000.0, onScreen = true),
                BombTarget(EnemyClass.BOSS, 5000.0, 300.0, onScreen = true),
            ),
        )
        assertEquals("a boss below 10% dies like from any other damage", listOf(true, true, false, false, true), hits.map { it.killed })
        assertEquals(100.0, hits[3].damage, 0.0)
        assertEquals("never more than the boss has left", 300.0, hits[4].damage, 0.0)
        assertEquals(0.0, hits[2].damage, 0.0)
    }

    @Test
    fun pickupVanishesAfterSixtyContinuousSecondsOffScreen() {
        val timer = OffscreenTimer()
        repeat(59) { assertFalse(timer.tick(1.0, onScreen = false)) }
        assertFalse("back on screen resets the count", timer.tick(0.5, onScreen = true))
        repeat(59) { assertFalse(timer.tick(1.0, onScreen = false)) }
        assertTrue(timer.tick(1.0, onScreen = false))
        assertTrue(timer.expired)
    }

    @Test
    fun chargerStopsTelegraphsAtLeast07sThenChargesAlongTheLockedDirection() {
        val brain = ChargerBrain(walkSpeed = 1f, chargeSpeed = 10f, triggerDistance = 5f)
        val self = Vec2.ZERO
        assertEquals(1f, brain.tick(0.1, self, Vec2(10f, 0f)).x, 1e-5f)
        assertEquals(ChargerState.CHASE, brain.state)
        assertEquals(Vec2.ZERO, brain.tick(0.1, self, Vec2(4f, 0f)))
        assertEquals(ChargerState.TELEGRAPH, brain.state)
        var elapsed = 0.0
        while (brain.state == ChargerState.TELEGRAPH) {
            assertEquals("player moved, charger waits", Vec2.ZERO, brain.tick(0.05, self, Vec2(0f, 4f)))
            elapsed += 0.05
        }
        assertTrue(elapsed >= 0.7 - 1e-9)
        val charge = brain.tick(0.05, self, Vec2(0f, 4f))
        assertEquals(ChargerState.CHARGE, brain.state)
        assertEquals("direction locked at telegraph start", 10f, charge.x, 1e-4f)
        assertEquals(0f, charge.y, 1e-4f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun telegraphShorterThan07sIsRejected() {
        ChargerBrain(1f, 10f, 5f, telegraphSeconds = 0.5)
    }

    @Test
    fun bossValidator() {
        val good = BossDefinition("b1", listOf(BossAttack("charge", 0.25, 0.8), BossAttack("volley", 0.10, 0.0)), 0.10)
        assertTrue(BossValidator.validate(good).isEmpty())
        val bad = BossValidator.validate(BossDefinition("b2", listOf(BossAttack("slam", 0.30, 0.5)), 0.20))
        assertEquals(3, bad.size)
        assertTrue(bad.any { "2 attacks" in it } && bad.any { "slam" in it } && bad.any { "contact" in it })
    }
}

class StatsAndHealthTest {
    @Test
    fun passiveLevelsTwoToFourAreLinear() {
        val atk = PassiveEffect("p.atk", Stat.ATTACK, 0.10, 0.50)
        listOf(0.10, 0.20, 0.30, 0.40, 0.50).forEachIndexed { i, v -> assertEquals(v, atk.valueAt(i + 1), 1e-9) }
    }

    @Test
    fun talentAndPassiveBonusesMultiplyEquipmentBonusesAdd() {
        val mods = listOf(
            StatModifier(Stat.ATTACK, 0.10, ModifierSource.EQUIPMENT),
            StatModifier(Stat.ATTACK, 0.15, ModifierSource.EQUIPMENT),
            StatModifier(Stat.ATTACK, 0.20, ModifierSource.TALENT),
            StatModifier(Stat.ATTACK, 0.50, ModifierSource.PASSIVE),
        )
        assertEquals(100 * 1.25 * 1.20 * 1.50, StatCalculator.attack(100.0, mods), 1e-9)
    }

    @Test
    fun damageReductionsCombineWithoutReachingOne() {
        val mods = listOf(StatModifier(Stat.DAMAGE_REDUCTION, 0.5, ModifierSource.TALENT), StatModifier(Stat.DAMAGE_REDUCTION, 0.5, ModifierSource.PASSIVE))
        assertEquals(0.75, StatCalculator.factor(Stat.DAMAGE_REDUCTION, mods), 1e-9)
    }

    @Test
    fun passiveModifiersFollowOwnedLevels() {
        val loadout = SkillData.newLoadout()
        CardDraw.apply(loadout, Card.newSkill(SkillData.MAGNET))
        CardDraw.apply(loadout, Card.levelUp(SkillData.MAGNET, 2))
        val mod = StatCalculator.fromPassives(loadout, listOf(PassiveEffect(SkillData.MAGNET, Stat.PICKUP_RADIUS, 1.0, 5.0))).single()
        assertEquals(2.0, mod.value, 1e-9)
        assertEquals(ModifierSource.PASSIVE, mod.source)
    }

    @Test
    fun secondHitWithinHalfASecondIsIgnored() {
        val hp = PlayerHealth(100.0)
        assertEquals(5.0, hp.takeHit(10.0, 5.0, 0.0), 0.0)
        assertEquals(0.0, hp.takeHit(10.4, 5.0, 0.0), 0.0)
        assertEquals(5.0, hp.takeHit(10.5, 5.0, 0.0), 0.0)
        assertEquals(90.0, hp.health, 0.0)
    }

    @Test
    fun contactDamageIsRateTimesWindowReductionAppliesDeathAndMeat() {
        val hit = PlayerHealth.contactHit(0.10, 100.0)
        assertEquals(5.0, hit, 1e-9)
        assertEquals(4.0, PlayerHealth(100.0).takeHit(0.0, hit, 0.2), 1e-9)
        val hp = PlayerHealth(100.0)
        hp.takeHit(0.0, 1000.0, 0.0)
        assertTrue(hp.isDead)
        assertEquals(0.0, hp.health, 0.0)
        assertEquals(200 * 0.30 * 1.20, PlayerHealth.meatHeal(200.0, 0.05 * 4), 1e-9)
    }
}
