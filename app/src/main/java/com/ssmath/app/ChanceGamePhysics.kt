package com.ssmath.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val PHYSICS_STEP = 1f / 120f
internal const val PLINKO_HEIGHT = 0.68f
internal const val PLINKO_BIN_TOP = 0.54f
internal const val PLINKO_FLOOR = 0.66f
internal const val PLINKO_BALL_RADIUS = 0.018f
internal const val PLINKO_PEG_RADIUS = 0.009f
internal const val CLAW_BALL_RADIUS = 0.044f
internal const val CLAW_FLOOR = 0.9f
private const val CLAW_PILE_LEFT = 0.045f
internal const val CLAW_PILE_RIGHT = 0.62f
internal const val CLAW_BIN_LEFT = 0.67f
internal const val CLAW_BIN_RIGHT = 0.975f

internal val plinkoPegs: List<Offset> = buildList {
    repeat(6) { row ->
        repeat(if (row % 2 == 0) 7 else 6) { column ->
            add(Offset(0.095f + column * 0.135f + (row % 2) * 0.0675f, 0.105f + row * 0.073f))
        }
    }
}

internal data class WheelSliceLayout(val distance: Float, val imageSize: Float)

internal fun wheelSliceLayout(sweepDegrees: Float): WheelSliceLayout {
    require(sweepDegrees > 0f && sweepDegrees <= 360f)
    if (sweepDegrees >= 359.99f) return WheelSliceLayout(0f, 1.32f)
    val halfAngle = Math.toRadians(minOf(sweepDegrees, 180f).toDouble() / 2)
    val cotangent = (cos(halfAngle) / sin(halfAngle)).toFloat()
    // Fit a radially oriented square between both slice edges and the circular rim.
    val side = 2f / sqrt((2f + cotangent) * (2f + cotangent) + 1f)
    return WheelSliceLayout(side * (1f + cotangent) / 2f, side * 0.94f)
}

internal data class ChanceTurn(
    val position: Float,
    val startedAtMs: Int,
    val plinko: PlinkoDrop? = null,
    val claw: ClawPickup? = null
) {
    fun progress(game: ChanceGame, elapsedMs: Int): Float =
        ((elapsedMs - startedAtMs).toFloat() / game.animationDurationMs).coerceIn(0f, 1f)

    fun landedAtMs(game: ChanceGame): Int = startedAtMs + (plinko?.landingTimeMs ?: game.animationDurationMs)
}

internal data class PlinkoDrop(val frames: List<Offset>) {
    private val landingFrame = (frames.indexOfLast { (it - frames.last()).getDistance() > 0.001f } + 1)
        .coerceAtMost(frames.lastIndex)
    val landingTimeMs: Int = (landingFrame * ChanceGame.PLINKO.animationDurationMs + frames.lastIndex - 1) /
        frames.lastIndex

    fun at(progress: Float): Offset = frames[(progress.coerceIn(0f, 1f) * frames.lastIndex).toInt()]
}

internal fun plinkoBallRadius(binCount: Int): Float = minOf(PLINKO_BALL_RADIUS, 0.14f / binCount)

private class GravityBall(
    val id: Int,
    var center: Offset,
    val radius: Float,
    var velocity: Offset = Offset.Zero
) {
    fun advance(gravity: Float) {
        velocity += Offset(0f, gravity * PHYSICS_STEP)
        center += velocity * PHYSICS_STEP
    }

    fun obstacle(point: Offset, obstacleRadius: Float, restitution: Float) {
        val delta = center - point
        val distance = delta.getDistance()
        val separation = radius + obstacleRadius
        if (distance >= separation) return
        val normal = if (distance > 0.00001f) delta / distance else Offset(0f, -1f)
        center += normal * (separation - distance)
        val speed = velocity.x * normal.x + velocity.y * normal.y
        if (speed < 0f) velocity -= normal * ((1f + restitution) * speed)
    }

    fun contain(left: Float, right: Float, floor: Float, restitution: Float) {
        if (center.x < left + radius) {
            center = Offset(left + radius, center.y)
            velocity = Offset(abs(velocity.x) * restitution, velocity.y)
        } else if (center.x > right - radius) {
            center = Offset(right - radius, center.y)
            velocity = Offset(-abs(velocity.x) * restitution, velocity.y)
        }
        if (center.y > floor - radius) {
            center = Offset(center.x, floor - radius)
            velocity = Offset(velocity.x * 0.92f, -abs(velocity.y) * restitution)
        } else if (center.y < radius) {
            center = Offset(center.x, radius)
            velocity = Offset(velocity.x, abs(velocity.y) * restitution)
        }
    }
}

internal fun plinkoDrop(
    position: Float,
    bin: Int,
    binCount: Int,
    previous: List<ChanceTurn>,
    startedAtMs: Int
): PlinkoDrop? {
    require(position in 0.08f..0.92f && bin in 0 until binCount)
    val radius = plinkoBallRadius(binCount)
    val steps = ChanceGame.PLINKO.animationDurationMs * 120 / 1_000
    // Select a real launch trajectory for the already-weighted reward, never steer a falling ball.
    repeat(801) { attempt ->
        val direction = if ((attempt + previous.size) % 2 == 0) 1f else -1f
        val speed = direction * (0.001f + ((attempt + 1) / 2) * 0.005f)
        val ball = GravityBall(0, Offset(position, 0.035f), radius, Offset(speed, 0f))
        val frames = ArrayList<Offset>(steps / 2 + 1)
        frames += ball.center
        repeat(steps) { step ->
            ball.advance(1.15f)
            plinkoPegs.forEach { ball.obstacle(it, PLINKO_PEG_RADIUS, 0.62f) }
            val elapsedMs = startedAtMs + (step + 1) * 1_000 / 120
            previous.forEach { turn ->
                if (elapsedMs >= turn.startedAtMs) {
                    ball.obstacle(requireNotNull(turn.plinko).at(turn.progress(ChanceGame.PLINKO, elapsedMs)),
                        radius, 0.25f)
                }
            }
            for (divider in 1 until binCount) {
                ball.obstacle(Offset(divider.toFloat() / binCount,
                    ball.center.y.coerceIn(PLINKO_BIN_TOP, PLINKO_FLOOR)), 0.003f, 0.4f)
            }
            ball.contain(0.01f, 0.99f, PLINKO_FLOOR, 0.3f)
            if (step % 2 == 1) frames += ball.center
        }
        if ((ball.center.x * binCount).toInt() == bin && ball.center.y > PLINKO_BIN_TOP &&
            ball.velocity.getDistance() < 0.08f) return PlinkoDrop(frames)
    }
    return null
}

internal data class ClawBall(val id: Int, val center: Offset)

internal data class ClawScene(
    val balls: List<ClawBall>,
    val collected: Set<Int>,
    val claw: Offset,
    val opening: Float = 1f,
    val heldBall: Int? = null
)

internal data class ClawPickup(val ballId: Int, val frames: List<ClawScene>) {
    fun at(progress: Float): ClawScene = frames[(progress.coerceIn(0f, 1f) * frames.lastIndex).toInt()]
}

internal fun clawHome(position: Float): Offset = Offset(
    0.105f + (position.coerceIn(0.08f, 0.92f) - 0.08f) / 0.84f * 0.45f, 0.13f
)

internal fun clawFingers(claw: Offset, opening: Float): List<List<Offset>> {
    val spread = 0.048f + opening * 0.025f
    val tip = 0.018f + opening * 0.065f
    return listOf(-1f, 1f).map { side ->
        listOf(claw + Offset(side * 0.012f, 0.01f), claw + Offset(side * spread, 0.07f),
            claw + Offset(side * spread, 0.105f), claw + Offset(side * tip, 0.125f))
    }
}

private fun settleClawBalls(
    balls: List<GravityBall>,
    collected: Set<Int>,
    held: Int? = null,
    fingers: List<Offset> = emptyList(),
    target: Int? = null
) {
    val free = balls.filter { it.id != held }
    val carried = balls.firstOrNull { it.id == held }
    free.forEach { it.advance(4.2f) }
    repeat(5) {
        free.forEachIndexed { index, ball ->
            for (otherIndex in index + 1 until free.size) {
                val other = free[otherIndex]
                val delta = ball.center - other.center
                val distance = delta.getDistance()
                val separation = ball.radius + other.radius
                if (distance >= separation) continue
                val normal = if (distance > 0.00001f) delta / distance else Offset(1f, 0f)
                val correction = normal * ((separation - distance) / 2)
                ball.center += correction
                other.center -= correction
                val relative = ball.velocity - other.velocity
                val speed = relative.x * normal.x + relative.y * normal.y
                if (speed < 0f) {
                    val impulse = normal * (speed * 0.6f)
                    ball.velocity -= impulse
                    other.velocity += impulse
                }
            }
        }
        free.forEach { ball ->
            if (carried != null) ball.obstacle(carried.center, carried.radius, 0.2f)
            if (ball.id != target) fingers.forEach { ball.obstacle(it, 0.006f, 0.2f) }
            val inChute = ball.id in collected
            ball.contain(if (inChute) CLAW_BIN_LEFT else CLAW_PILE_LEFT,
                if (inChute) CLAW_BIN_RIGHT else CLAW_PILE_RIGHT, CLAW_FLOOR, 0.24f)
        }
    }
    free.forEach { it.velocity *= 0.994f }
}

internal fun initialClawScene(): ClawScene {
    val balls = buildList {
        repeat(5) { row ->
            repeat(if (row % 2 == 0) 6 else 5) { column ->
                add(GravityBall(size, Offset(0.105f + column * 0.088f + (row % 2) * 0.044f,
                    0.84f - row * 0.095f), CLAW_BALL_RADIUS))
            }
        }
    }
    repeat(360) { settleClawBalls(balls, emptySet()) }
    return ClawScene(balls.map { ClawBall(it.id, it.center) }, emptySet(), clawHome(0.5f))
}

internal fun clawPickup(previous: ClawScene, position: Float): ClawPickup {
    val home = clawHome(position)
    val pile = previous.balls.filter { it.id !in previous.collected }
    val exposed = pile.filter { ball ->
        pile.none { other -> other.id != ball.id && other.center.y < ball.center.y &&
            abs(other.center.x - ball.center.x) < CLAW_BALL_RADIUS * 2f }
    }
    val selected = exposed.minBy { abs(it.center.x - home.x) }
    val balls = previous.balls.map { GravityBall(it.id, it.center, CLAW_BALL_RADIUS) }
    val picked = balls.first { it.id == selected.id }
    val releaseX = when (previous.collected.size % 3) {
        0 -> (CLAW_BIN_LEFT + CLAW_BIN_RIGHT) / 2
        1 -> CLAW_BIN_LEFT + CLAW_BALL_RADIUS + 0.008f
        else -> CLAW_BIN_RIGHT - CLAW_BALL_RADIUS - 0.008f
    }
    val frames = mutableListOf<ClawScene>()
    val steps = ChanceGame.CLAW_MACHINE.animationDurationMs * 120 / 1_000
    var grasp: Offset? = null
    fun motor(from: Float, to: Float, progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        return from + (to - from) * t * t * (3 - 2 * t)
    }
    for (step in 0..steps) {
        val progress = step.toFloat() / steps
        if (grasp == null && progress >= 0.43f) grasp = picked.center
        val target = grasp ?: picked.center
        val x = when {
            progress < 0.1f -> motor(home.x, target.x, progress / 0.1f)
            progress < 0.61f -> target.x
            progress < 0.75f -> motor(target.x, releaseX, (progress - 0.61f) / 0.14f)
            progress < 0.84f -> releaseX
            else -> motor(releaseX, home.x, (progress - 0.84f) / 0.16f)
        }
        val bottom = target.y - 0.085f
        val y = when {
            progress < 0.1f -> home.y
            progress < 0.36f -> motor(home.y, bottom, (progress - 0.1f) / 0.26f)
            progress < 0.43f -> bottom
            progress < 0.61f -> motor(bottom, home.y, (progress - 0.43f) / 0.18f)
            else -> home.y
        }
        val held = progress >= 0.43f && progress < 0.79f
        val released = progress >= 0.79f
        val collected = if (released) previous.collected + picked.id else previous.collected
        val opening = when {
            progress < 0.36f -> 1f
            progress < 0.43f -> 1f - (progress - 0.36f) / 0.07f
            progress < 0.75f -> 0f
            else -> ((progress - 0.75f) / 0.04f).coerceIn(0f, 1f)
        }
        val claw = Offset(x, y)
        if (step > 0) {
            if (held) {
                picked.center = Offset(x, y + 0.085f)
                picked.velocity = Offset.Zero
            }
            settleClawBalls(balls, collected, if (held) picked.id else null,
                clawFingers(claw, opening).flatMap { it.drop(1) }, picked.id)
        }
        if (step % 2 == 0) frames += ClawScene(balls.map { ClawBall(it.id, it.center) },
            collected, claw, opening, picked.id.takeIf { held })
    }
    return ClawPickup(picked.id, frames)
}
