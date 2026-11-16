package com.surv20.game.core.run

import com.surv20.game.core.run.TimelineEventKind.BOSS
import com.surv20.game.core.run.TimelineEventKind.ELITE
import com.surv20.game.core.run.TimelineEventKind.HORDE
import com.surv20.game.core.run.TimelineEventKind.SPAWN_PHASE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal object TimelineData {
    fun phase(t: Double, id: String, strength: Double = 1.0) = TimelineEvent(t, SPAWN_PHASE, id, strength)
    fun horde(t: Double) = TimelineEvent(t, HORDE, "horde")
    fun elite(t: Double) = TimelineEvent(t, ELITE, "elite")
    fun boss(t: Double, id: String, final: Boolean = false) = TimelineEvent(t, BOSS, id, isFinalBoss = final)

    // A valid open-map timeline (own design, not a Survivor.io chapter).
    fun validOpen() = ChapterTimeline(
        MapFormat.OPEN,
        listOf(
            phase(0.0, "set.a"), horde(100.0), elite(200.0),
            boss(300.0, "boss.1"), phase(310.0, "set.b", 1.5), horde(420.0), elite(500.0),
            boss(600.0, "boss.2"), phase(610.0, "set.b", 2.0), horde(700.0), elite(800.0),
            boss(900.0, "boss.final", final = true),
        ),
    )

    fun validClosed(): ChapterTimeline {
        val events = mutableListOf(phase(0.0, "set.a"), horde(30.0), elite(60.0))
        val bosses = listOf(90.0, 180.0, 270.0, 360.0, 480.0)
        bosses.forEachIndexed { i, t ->
            events += boss(t, "boss.${i + 1}", final = i == bosses.lastIndex)
            if (i < bosses.lastIndex) {
                val gap = bosses[i + 1] - t
                events += listOf(horde(t + gap * 0.3), elite(t + gap * 0.5), horde(t + gap * 0.7))
            }
        }
        return ChapterTimeline(MapFormat.CLOSED, events)
    }
}

class TimelineSchedulerTest {
    private fun run(s: TimelineScheduler, until: Double, step: Double = 0.5): List<TimelineSignal> {
        val all = mutableListOf<TimelineSignal>()
        while (s.elapsedSeconds + 1e-9 < until) all += s.advance(step)
        return all
    }

    @Test
    fun hordeAndBossGetAnAlarmThreeSecondsEarlyElitesDoNot() {
        val signals = run(TimelineScheduler(TimelineData.validOpen()), 300.0)
        val alarms = signals.filter { it.kind == TimelineSignalKind.ALARM }
        assertEquals(setOf(HORDE, BOSS), alarms.map { it.event.kind }.toSet())
        assertEquals(97.0, alarms.single { it.event.kind == HORDE }.atSeconds, 1e-9)
        assertEquals(297.0, alarms.single { it.event.kind == BOSS }.atSeconds, 1e-9)
        assertTrue(signals.any { it.kind == TimelineSignalKind.FIRE && it.event.kind == ELITE })
    }

    @Test
    fun bossStartsAFightAndNonBossEventsInsideItAreSkipped() {
        val s = TimelineScheduler(TimelineData.validOpen())
        run(s, 300.0)
        assertEquals("boss.1", s.activeBoss!!.id)
        val during = run(s, 450.0)
        assertEquals(listOf(310.0, 420.0), during.filter { it.kind == TimelineSignalKind.SKIP }.map { it.event.timeSeconds })
        assertFalse(during.any { it.kind == TimelineSignalKind.ALARM })
        assertEquals("clock keeps running during the fight", 450.0, s.elapsedSeconds, 1e-9)
    }

    @Test
    fun bossFallingDueDuringAFightWaitsThenEntersThreeSecondsAfterIt() {
        val s = TimelineScheduler(TimelineData.validOpen())
        run(s, 630.0)
        assertEquals("boss 1 still alive at 10:30", "boss.1", s.activeBoss!!.id)
        val afterKill = s.notifyBossDefeated().single()
        assertEquals(TimelineSignalKind.ALARM, afterKill.kind)
        assertEquals("boss.2", afterKill.event.id)
        assertFalse(s.bossActive)
        val next = run(s, 633.5)
        val fire = next.single { it.kind == TimelineSignalKind.FIRE }
        assertEquals("boss.2", fire.event.id)
        assertEquals(633.0, fire.atSeconds, 1e-9)
        assertFalse("no second alarm", next.any { it.kind == TimelineSignalKind.ALARM })
    }

    @Test
    fun everyBossAppearsEvenWhenFightsRunLong() {
        val s = TimelineScheduler(TimelineData.validOpen())
        val fired = mutableListOf<String>()
        var enteredAt = 0.0
        var i = 0
        while (i++ < 4000 && !s.won) {
            s.advance(0.5).filter { it.kind == TimelineSignalKind.FIRE && it.event.kind == BOSS }.forEach {
                fired += it.event.id
                enteredAt = it.atSeconds
            }
            // Each boss lives 400 s, so every later boss falls due during a fight.
            if (s.bossActive && s.elapsedSeconds >= enteredAt + 400) s.notifyBossDefeated()
        }
        assertEquals(listOf("boss.1", "boss.2", "boss.final"), fired)
        assertTrue(s.won)
    }

    @Test
    fun killingTheFinalBossWinsAndStopsTheTimeline() {
        val s = TimelineScheduler(TimelineData.validOpen())
        run(s, 300.0); s.notifyBossDefeated()
        run(s, 600.0); s.notifyBossDefeated()
        run(s, 900.0)
        assertTrue(s.activeBoss!!.isFinalBoss)
        assertEquals(TimelineSignalKind.WON, s.notifyBossDefeated().single().kind)
        assertTrue(s.won)
        assertTrue(s.advance(10.0).isEmpty())
    }

    @Test
    fun closedMapLivingBossDelaysTheNextOne() {
        val s = TimelineScheduler(TimelineData.validClosed())
        run(s, 200.0)
        assertEquals("boss.1", s.activeBoss!!.id)
        s.notifyBossDefeated()
        assertEquals("boss.2", run(s, 203.5).single { it.kind == TimelineSignalKind.FIRE }.event.id)
    }

    @Test(expected = IllegalStateException::class)
    fun notifyWithoutABossThrows() {
        TimelineScheduler(TimelineData.validOpen()).notifyBossDefeated()
    }
}

class TimelineValidatorTest {
    private fun errors(format: MapFormat, events: List<TimelineEvent>) = TimelineValidator.validate(ChapterTimeline(format, events))

    @Test
    fun validOpenClosedAndCorridorTimelinesPass() {
        assertTrue(TimelineValidator.validate(TimelineData.validOpen()).isEmpty())
        assertTrue(TimelineValidator.validate(TimelineData.validClosed()).isEmpty())
        assertTrue(errors(MapFormat.CORRIDOR, TimelineData.validOpen().events).isEmpty())
    }

    @Test
    fun rejectsWrongBossTimesAndMissingFinalFlag() {
        val moved = TimelineData.validOpen().events.filter { it.id != "boss.2" } + TimelineData.boss(610.0, "boss.2")
        assertTrue(errors(MapFormat.OPEN, moved).any { "Bosses must enter" in it })
        val noFinal = TimelineData.validOpen().events.filter { !it.isFinalBoss } + TimelineData.boss(900.0, "boss.final")
        assertTrue(errors(MapFormat.OPEN, noFinal).any { "final" in it })
    }

    @Test
    fun rejectsTooFewElitesAndAnEmptySegment() {
        val events = TimelineData.validOpen().events.filter {
            !(it.kind == ELITE && it.timeSeconds == 800.0) && !(it.kind == HORDE && it.timeSeconds == 700.0)
        }
        val e = errors(MapFormat.OPEN, events)
        assertTrue(e.any { "3 elites" in it })
        assertTrue(e.any { "Segment 600–900" in it })
    }

    @Test
    fun rejectsNoNewPhaseAfterAMiddleBossAndDroppingStrength() {
        val noPhase = TimelineData.validOpen().events.filter { !(it.kind == SPAWN_PHASE && it.timeSeconds == 610.0) }
        assertTrue(errors(MapFormat.OPEN, noPhase).any { "After the boss at 600" in it })
        val weaker = TimelineData.validOpen().events + TimelineData.phase(850.0, "set.c", 1.0)
        assertTrue(errors(MapFormat.OPEN, weaker).any { "strength" in it })
    }

    @Test
    fun closedMapRequiresTwoHordesAndAnEliteInEachGap() {
        val events = TimelineData.validClosed().events.filter { !(it.kind == HORDE && it.timeSeconds > 270 && it.timeSeconds < 360) } +
            TimelineData.horde(300.0)
        val e = errors(MapFormat.CLOSED, events)
        assertEquals(listOf("Gap 270–360 s needs at least 2 hordes and 1 elite."), e)
    }
}
