package com.surv20.game.core.run

import com.surv20.game.core.Balance
import kotlin.math.abs
import kotlin.math.max

// Map formats of SPEC W6-W9.
enum class MapFormat {
    OPEN, // infinite both ways, 15 min, 3 bosses (W6)
    CLOSED, // bounded rectangle, 8 min, 5 bosses, no arena circle (W7)
    CORRIDOR, // fixed width, infinite vertically, rules of W6 (W8)
}

enum class TimelineEventKind { SPAWN_PHASE, HORDE, ELITE, BOSS }

// One entry of a chapter timeline; time is the moment the event enters (SPEC W6, W7).
data class TimelineEvent(
    val timeSeconds: Double,
    val kind: TimelineEventKind,
    // Enemy set, horde, elite or boss definition id.
    val id: String,
    // Strength multiplier of a spawn phase (ignored for other kinds).
    val strength: Double = 1.0,
    val isFinalBoss: Boolean = false,
) {
    init {
        require(timeSeconds >= 0)
        require(id.isNotEmpty()) { "Event id is required." }
        require(!isFinalBoss || kind == TimelineEventKind.BOSS) { "Only a boss can be final." }
    }

    // Hordes and bosses get a warning; elites and spawn phases do not (SPEC W11).
    val hasAlarm: Boolean get() = kind == TimelineEventKind.HORDE || kind == TimelineEventKind.BOSS
}

class ChapterTimeline(val format: MapFormat, events: List<TimelineEvent>) {
    val events: List<TimelineEvent> = events.sortedBy { it.timeSeconds }

    companion object {
        // Nominal boss entry times of each format (SPEC W6, W7).
        fun bossTimes(format: MapFormat): List<Double> =
            if (format == MapFormat.CLOSED) listOf(90.0, 180.0, 270.0, 360.0, 480.0) else listOf(300.0, 600.0, 900.0)
    }
}

enum class TimelineSignalKind {
    ALARM, // show the warning and play the alarm sound (W11)
    FIRE, // the event enters now
    SKIP, // a non-boss event fell inside a boss fight (W10)
    WON, // the final boss died
}

data class TimelineSignal(val kind: TimelineSignalKind, val event: TimelineEvent, val atSeconds: Double)

// Plays a chapter timeline (SPEC W6, W7, W10, W11; PLAN P15): the clock always runs; during a boss fight non-boss
// events are skipped and a boss that falls due waits; after the fight a waiting boss gets its alarm at once and
// enters alarmLeadSeconds later. The same rule covers the closed map, where a living boss delays the next one.
class TimelineScheduler(timeline: ChapterTimeline, val alarmLeadSeconds: Double = Balance.ALARM_LEAD_SECONDS) {
    private enum class State { PENDING, WAITING_FOR_ARENA, DONE }

    private class Entry(val event: TimelineEvent) {
        var due = event.timeSeconds
        var alarmed = false
        var state = State.PENDING
    }

    private val entries = timeline.events.map { Entry(it) }
    private var active: Entry? = null

    var elapsedSeconds = 0.0
        private set
    var won = false
        private set
    val bossActive: Boolean get() = active != null
    val activeBoss: TimelineEvent? get() = active?.event

    init {
        require(alarmLeadSeconds >= 0)
    }

    // Moves the clock forward. Do not call while the game is paused (SPEC W5).
    fun advance(deltaSeconds: Double): List<TimelineSignal> {
        require(deltaSeconds >= 0)
        val signals = mutableListOf<TimelineSignal>()
        if (won) return signals
        elapsedSeconds += deltaSeconds
        for (entry in entries.filter { it.state == State.PENDING }.sortedBy { it.due }) {
            if (entry.event.hasAlarm && !entry.alarmed && !bossActive && elapsedSeconds >= entry.due - alarmLeadSeconds) {
                entry.alarmed = true
                signals += TimelineSignal(TimelineSignalKind.ALARM, entry.event, max(0.0, entry.due - alarmLeadSeconds))
            }
            if (elapsedSeconds < entry.due) continue
            if (bossActive) {
                if (entry.event.kind == TimelineEventKind.BOSS) {
                    entry.state = State.WAITING_FOR_ARENA
                } else {
                    entry.state = State.DONE
                    signals += TimelineSignal(TimelineSignalKind.SKIP, entry.event, entry.due)
                }
                continue
            }
            entry.state = State.DONE
            signals += TimelineSignal(TimelineSignalKind.FIRE, entry.event, entry.due)
            if (entry.event.kind == TimelineEventKind.BOSS) active = entry
        }
        return signals
    }

    // Call when the active boss dies.
    fun notifyBossDefeated(): List<TimelineSignal> {
        val defeated = checkNotNull(active) { "No boss is active." }
        active = null
        if (defeated.event.isFinalBoss) {
            won = true
            return listOf(TimelineSignal(TimelineSignalKind.WON, defeated.event, elapsedSeconds))
        }
        val waiting = entries.filter { it.state == State.WAITING_FOR_ARENA }.minByOrNull { it.event.timeSeconds }
            ?: return emptyList()
        waiting.state = State.PENDING
        waiting.due = elapsedSeconds + alarmLeadSeconds
        waiting.alarmed = true
        return listOf(TimelineSignal(TimelineSignalKind.ALARM, waiting.event, elapsedSeconds))
    }
}

// Checks a chapter timeline against the format rules of SPEC W6/W7/W8 (PLAN P15). Empty list = valid.
object TimelineValidator {
    private const val EPS = 1e-6

    fun validate(timeline: ChapterTimeline): List<String> {
        val errors = mutableListOf<String>()
        val events = timeline.events
        val bossTimes = ChapterTimeline.bossTimes(timeline.format)
        checkBosses(events.filter { it.kind == TimelineEventKind.BOSS }, bossTimes, errors)
        checkSpawnPhases(events, bossTimes, errors)
        if (timeline.format == MapFormat.CLOSED) checkClosedGaps(events, bossTimes, errors) else checkOpenRules(events, bossTimes, errors)
        return errors
    }

    private fun checkBosses(bosses: List<TimelineEvent>, expected: List<Double>, errors: MutableList<String>) {
        val times = bosses.map { it.timeSeconds }
        if (times.size != expected.size || times.zip(expected).any { (a, b) -> abs(a - b) > EPS }) {
            errors += "Bosses must enter at ${expected.joinToString()} s; found ${times.joinToString()}."
        }
        if (bosses.count { it.isFinalBoss } != 1 || bosses.isEmpty() || !bosses.last().isFinalBoss) {
            errors += "Exactly one boss, the last one, must be final."
        }
    }

    private fun checkSpawnPhases(events: List<TimelineEvent>, bossTimes: List<Double>, errors: MutableList<String>) {
        val phases = events.filter { it.kind == TimelineEventKind.SPAWN_PHASE }
        if (phases.isEmpty() || phases[0].timeSeconds > EPS) {
            errors += "A spawn phase must start at 0 s."
            return
        }
        phases.zipWithNext().forEach { (a, b) ->
            if (b.strength + EPS < a.strength) errors += "Enemy strength must not drop over time (at ${b.timeSeconds} s)."
        }
        if (events.any { it.timeSeconds > bossTimes.last() + EPS }) errors += "No event may come after the final boss at ${bossTimes.last()} s."
    }

    // W6/W8: ≥3 elites, ≥3 hordes, each segment before a boss has pressure, a new or stronger phase after each middle boss.
    private fun checkOpenRules(events: List<TimelineEvent>, bossTimes: List<Double>, errors: MutableList<String>) {
        if (events.count { it.kind == TimelineEventKind.ELITE } < 3) errors += "At least 3 elites are required."
        if (events.count { it.kind == TimelineEventKind.HORDE } < 3) errors += "At least 3 hordes are required."
        var start = 0.0
        for (bossTime in bossTimes) {
            val from = start
            val hasPressure = events.any {
                (it.kind == TimelineEventKind.ELITE || it.kind == TimelineEventKind.HORDE) &&
                    it.timeSeconds > from - EPS && it.timeSeconds < bossTime - EPS
            }
            if (!hasPressure) errors += "Segment ${from.fmt()}–${bossTime.fmt()} s needs at least one elite or horde."
            start = bossTime
        }
        val phases = events.filter { it.kind == TimelineEventKind.SPAWN_PHASE }
        for (i in 0 until bossTimes.size - 1) {
            val after = bossTimes[i]
            val before = bossTimes[i + 1]
            val previous = phases.lastOrNull { it.timeSeconds <= after + EPS }
            val changed = previous != null && phases.any {
                it.timeSeconds > after - EPS && it.timeSeconds < before - EPS &&
                    (it.id != previous.id || it.strength > previous.strength + EPS)
            }
            if (!changed) errors += "After the boss at ${after.fmt()} s a new or stronger enemy phase is required."
        }
    }

    // W7: between each pair of consecutive bosses at least 2 hordes and 1 elite.
    private fun checkClosedGaps(events: List<TimelineEvent>, bossTimes: List<Double>, errors: MutableList<String>) {
        for (i in 0 until bossTimes.size - 1) {
            val from = bossTimes[i]
            val to = bossTimes[i + 1]
            val gap = events.filter { it.timeSeconds > from + EPS && it.timeSeconds < to - EPS }
            if (gap.count { it.kind == TimelineEventKind.HORDE } < 2 || gap.count { it.kind == TimelineEventKind.ELITE } < 1) {
                errors += "Gap ${from.fmt()}–${to.fmt()} s needs at least 2 hordes and 1 elite."
            }
        }
    }

    private fun Double.fmt(): String = if (this == kotlin.math.floor(this)) toLong().toString() else toString()
}
