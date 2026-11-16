package com.surv20.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.surv20.game.core.FrameStats
import com.surv20.game.core.SeededRandom
import com.surv20.game.core.Vec2
import com.surv20.game.core.run.ArenaLayout
import com.surv20.game.core.run.CrystalKind
import com.surv20.game.core.run.EnemyType
import com.surv20.game.core.run.Joystick
import com.surv20.game.core.run.MapFormat
import com.surv20.game.core.run.RunInput
import com.surv20.game.core.run.RunSim
import com.surv20.game.res.Res
import com.surv20.game.res.back
import com.surv20.game.res.defeat
import com.surv20.game.res.fps
import com.surv20.game.res.kills
import com.surv20.game.res.level
import com.surv20.game.res.pause
import com.surv20.game.res.quit
import com.surv20.game.res.resume
import com.surv20.game.res.survived
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.random.Random

// Placeholder look until Recraft art exists (PLAN K7): shapes and points only.
private val Ground = Color(0xFF0E1B2B)
private val GridLine = Color(0xFF17304A)
private val WalkerColor = Color(0xFF9B5DE5)
private val SwarmerColor = Color(0xFFFF5D73)
private val ProjectileColor = Color(0xFFFFE066)
private val CrystalColors = listOf(Color(0xFF4ADE80), Color(0xFF38BDF8), Color(0xFFFACC15))

private data class Hud(
    val seconds: Int = 0,
    val kills: Int = 0,
    val level: Int = 1,
    val xpShare: Float = 0f,
    val hpShare: Float = 1f,
    val avgFps: Int = 0,
    val lowFps: Int = 0,
    val measuredSeconds: Int = 0,
)

// Live run (PLAN P3-P6, K5): RunSim stepped in a withFrameNanos loop and drawn on a Canvas; the crowd, projectiles
// and crystals are drawn as batched points so a frame is a handful of draw calls regardless of their number.
@Composable
fun RunView(stress: Boolean, showFps: Boolean, onExit: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Ground)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val sim = remember(stress) {
            RunSim(ArenaLayout(MapFormat.OPEN, maxOf(1f, heightPx / widthPx)), SeededRandom(Random.nextInt()), stress)
        }
        val joystick = remember { with(density) { Joystick(maxRadiusPx = 56.dp.toPx(), deadZonePx = 6.dp.toPx()) } }
        val stats = remember { FrameStats() }
        val buffers = remember { PointBuffers(sim) }
        var frame by remember { mutableLongStateOf(0L) }
        var paused by remember { mutableStateOf(false) }
        var hud by remember { mutableStateOf(Hud()) }

        LaunchedEffect(sim) {
            var last = withFrameNanos { it }
            var hudTimer = 0.0
            while (true) {
                withFrameNanos { now ->
                    val raw = (now - last) / 1_000_000_000.0
                    last = now
                    stats.record(raw)
                    val dt = raw.coerceIn(0.0, 0.05)
                    if (!paused && !sim.dead) sim.step(dt, RunInput(joystick.direction()))
                    frame++
                    hudTimer += raw
                    if (hudTimer >= 0.25) {
                        hudTimer = 0.0
                        hud = Hud(
                            seconds = sim.time.toInt(),
                            kills = sim.kills,
                            level = sim.experience.level,
                            xpShare = (sim.experience.xp.toFloat() / sim.experience.xpToNext).coerceIn(0f, 1f),
                            hpShare = (sim.health.health / sim.health.maxHealth).toFloat(),
                            avgFps = stats.averageFps().toInt(),
                            lowFps = stats.onePercentLowFps().toInt(),
                            measuredSeconds = stats.windowFilledSeconds.toInt(),
                        )
                    }
                }
            }
        }

        Canvas(
            Modifier.fillMaxSize().pointerInput(sim) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            val id = change.id.value
                            val pos = Vec2(change.position.x, change.position.y)
                            when {
                                change.pressed && !change.previousPressed -> joystick.onDown(id, pos, size.height.toFloat())
                                change.pressed -> joystick.onMove(id, pos)
                                else -> joystick.onUp(id)
                            }
                        }
                    }
                }
            },
        ) {
            frame // read the frame counter so the canvas redraws every frame
            drawRun(sim, buffers)
            drawJoystick(joystick)
        }

        // HUD (SPEC W5): time, kills, level with XP bar, HP bar; FPS meter in debug builds and the stress test.
        Column(Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(hud.seconds), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("  " + stringResource(Res.string.kills, hud.kills), color = Color.White)
                Text("  " + stringResource(Res.string.level, hud.level), color = Teal)
                Box(Modifier.weight(1f))
                OutlinedButton(onClick = { paused = !paused }) { Text(stringResource(if (paused) Res.string.resume else Res.string.pause)) }
            }
            LinearProgressIndicator(progress = { hud.xpShare }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Teal)
            LinearProgressIndicator(progress = { hud.hpShare }, modifier = Modifier.width(120.dp).height(6.dp), color = Color(0xFFEF4444))
            if (showFps || stress) {
                Text(stringResource(Res.string.fps, hud.avgFps, hud.lowFps, hud.measuredSeconds), color = Amber, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (paused || sim.dead) {
            Column(
                Modifier.align(Alignment.Center).background(Color(0xCC0E1B2B), RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sim.dead) {
                    Text(stringResource(Res.string.defeat), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(stringResource(Res.string.survived, formatTime(hud.seconds)), color = Color.White)
                    Button(onClick = onExit) { Text(stringResource(Res.string.back)) }
                } else {
                    Button(onClick = { paused = false }) { Text(stringResource(Res.string.resume)) }
                    OutlinedButton(onClick = onExit) { Text(stringResource(Res.string.quit)) }
                }
            }
        }
    }
}

// Reused point arrays: one batch per colour, unused slots parked far off screen (no allocation per frame).
private class PointBuffers(sim: RunSim) {
    val enemies = Array(EnemyType.entries.size) { FloatArray(sim.enemyCapacity * 2) }
    val flashing = FloatArray(sim.enemyCapacity * 2)
    val projectiles = FloatArray(sim.projectileCapacity * 2)
    val crystals = Array(3) { FloatArray(sim.crystalCapacity * 2) }
    val enemyPaints = EnemyType.entries.map { pointPaint(if (it == EnemyType.WALKER) WalkerColor else SwarmerColor) }
    val flashPaint = pointPaint(Color.White)
    val projectilePaint = pointPaint(ProjectileColor)
    val crystalPaints = CrystalColors.map { pointPaint(it) }

    companion object {
        const val OFF = -100_000f
        fun pointPaint(color: Color) = Paint().apply {
            this.color = color
            strokeCap = StrokeCap.Round
        }
    }
}

private fun DrawScope.drawRun(sim: RunSim, b: PointBuffers) {
    val scale = size.width / ArenaLayout.VIEW_WIDTH
    val camera = sim.layout.cameraFor(Vec2(sim.playerX, sim.playerY))
    val cx = size.width / 2f
    val cy = size.height / 2f
    fun sx(x: Float) = cx + (x - camera.x) * scale
    fun sy(y: Float) = cy - (y - camera.y) * scale

    // Ground grid every 2 units, scrolling with the camera.
    val step = 2f
    var gx = floor((camera.x - ArenaLayout.VIEW_WIDTH) / step) * step
    while (sx(gx) < size.width) {
        drawLine(GridLine, Offset(sx(gx), 0f), Offset(sx(gx), size.height), 1f)
        gx += step
    }
    var gy = floor((camera.y - sim.layout.viewHalfSize.y * 2) / step) * step
    while (sy(gy) > 0f) {
        drawLine(GridLine, Offset(0f, sy(gy)), Offset(size.width, sy(gy)), 1f)
        gy += step
    }

    // Crystals by colour.
    for (k in 0 until 3) b.crystals[k].fill(PointBuffers.OFF)
    val crystalFill = IntArray(3)
    for (i in 0 until sim.crystalCount) {
        val k = sim.crystalKind[i] - CrystalKind.GREEN.ordinal
        val n = crystalFill[k]++
        b.crystals[k][n * 2] = sx(sim.crystalX[i])
        b.crystals[k][n * 2 + 1] = sy(sim.crystalY[i])
    }

    // Enemies by type, hit flash on top.
    for (t in b.enemies.indices) b.enemies[t].fill(PointBuffers.OFF)
    b.flashing.fill(PointBuffers.OFF)
    val enemyFill = IntArray(b.enemies.size)
    var flashFill = 0
    for (i in 0 until sim.enemyCount) {
        val t = sim.enemyType[i]
        val n = enemyFill[t]++
        val x = sx(sim.enemyX[i])
        val y = sy(sim.enemyY[i])
        b.enemies[t][n * 2] = x
        b.enemies[t][n * 2 + 1] = y
        if (sim.enemyFlash[i] > 0f) {
            b.flashing[flashFill * 2] = x
            b.flashing[flashFill * 2 + 1] = y
            flashFill++
        }
    }

    b.projectiles.fill(PointBuffers.OFF)
    for (i in 0 until sim.projectileCount) {
        b.projectiles[i * 2] = sx(sim.projX[i])
        b.projectiles[i * 2 + 1] = sy(sim.projY[i])
    }

    drawIntoCanvas { canvas ->
        for (k in 0 until 3) {
            b.crystalPaints[k].strokeWidth = scale * (0.16f + 0.06f * k)
            canvas.drawRawPoints(PointMode.Points, b.crystals[k], b.crystalPaints[k])
        }
        EnemyType.entries.forEachIndexed { t, type ->
            b.enemyPaints[t].strokeWidth = scale * type.radius * 2f
            canvas.drawRawPoints(PointMode.Points, b.enemies[t], b.enemyPaints[t])
        }
        b.flashPaint.strokeWidth = scale * 0.4f
        canvas.drawRawPoints(PointMode.Points, b.flashing, b.flashPaint)
        b.projectilePaint.strokeWidth = scale * RunSim.PROJECTILE_RADIUS * 2f
        canvas.drawRawPoints(PointMode.Points, b.projectiles, b.projectilePaint)
    }

    // Player: a drone triangle pointing where it last moved.
    val px = sx(sim.playerX)
    val py = sy(sim.playerY)
    val dir = if (sim.lastMove.lengthSquared > 0f) sim.lastMove else Vec2(0f, 1f)
    val r = RunSim.PLAYER_RADIUS * scale * 1.3f
    val tip = Offset(px + dir.x * r, py - dir.y * r)
    val left = Offset(px - dir.y * r * 0.7f - dir.x * r * 0.6f, py - dir.x * r * 0.7f + dir.y * r * 0.6f)
    val right = Offset(px + dir.y * r * 0.7f - dir.x * r * 0.6f, py + dir.x * r * 0.7f + dir.y * r * 0.6f)
    drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(right.x, right.y); close() }, Teal)
    drawCircle(Amber, radius = r * 0.25f, center = Offset(px, py))
}

private fun DrawScope.drawJoystick(stick: Joystick) {
    val origin = stick.origin ?: return
    val knob = stick.knob() ?: return
    drawCircle(Color.White.copy(alpha = 0.15f), radius = 56.dp.toPx(), center = Offset(origin.x, origin.y))
    drawCircle(Color.White.copy(alpha = 0.35f), radius = 56.dp.toPx(), center = Offset(origin.x, origin.y), style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = 22.dp.toPx(), center = Offset(knob.x, knob.y))
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${if (m < 10) "0" else ""}$m:${if (s < 10) "0" else ""}$s"
}
