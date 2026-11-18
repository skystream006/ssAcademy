package com.ssmath.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import org.junit.Assert.*
import org.junit.Test

class ChanceGamePhysicsTest {
    @Test fun wheelArtworkFitsInsideWideAndNarrowWeightedSlices() {
        listOf(3.6f, 18f, 72f, 120f, 180f, 288f, 356.4f, 360f).forEach { sweep ->
            val layout = wheelSliceLayout(sweep)
            for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) {
                val corner = Offset(layout.distance + x * layout.imageSize / 2, y * layout.imageSize / 2)
                assertTrue("$sweep stays inside the rim", corner.getDistance() <= 1f)
                if (sweep < 360f) {
                    assertTrue("$sweep stays inside its slice",
                        Math.toDegrees(abs(atan2(corner.y, corner.x)).toDouble()) <= sweep / 2)
                }
            }
        }
        assertTrue("Five-slice icons exceed the old 56dp size on a 320dp screen",
            wheelSliceLayout(72f).imageSize * (320 - 32) * 0.46f >= 64)
        assertTrue("A lone prize fills the wheel", wheelSliceLayout(360f).imageSize * 288 * 0.46f >= 160)
    }

    @Test fun plinkoUsesGravityAndPegBouncesToReachEveryBinFromEitherEdge() {
        for (bins in 1..5) for (bin in 0 until bins) {
            for (position in listOf(0.08f, 0.29f, 0.5f, 0.71f, 0.92f)) {
                val drop = requireNotNull(plinkoDrop(position, bin, bins, emptyList(), bin))
                assertEquals(Offset(position, 0.035f), drop.frames.first())
                assertEquals(bin, (drop.frames.last().x * bins).toInt())
                assertTrue(drop.frames.last().y > PLINKO_BIN_TOP)
                drop.frames.forEach { point ->
                    assertTrue(point.x >= PLINKO_BALL_RADIUS && point.x <= 1f - PLINKO_BALL_RADIUS)
                    assertTrue(point.y >= PLINKO_BALL_RADIUS && point.y <= PLINKO_FLOOR - PLINKO_BALL_RADIUS + 0.0001f)
                    plinkoPegs.forEach { peg ->
                        assertTrue("The ball does not pass through a peg",
                            (point - peg).getDistance() >= PLINKO_BALL_RADIUS + PLINKO_PEG_RADIUS - 0.001f)
                    }
                }
                assertTrue("No scripted jumps", drop.frames.zipWithNext().all { (a, b) ->
                    (a - b).getDistance() < 0.055f
                })
            }
        }
        val drop = requireNotNull(plinkoDrop(0.5f, 1, 3, emptyList(), 0))
        val firstFall = drop.frames[1].y - drop.frames[0].y
        assertTrue(drop.frames[2].y - drop.frames[1].y > firstFall)
        assertTrue("Peg impacts can bounce upward", drop.frames.zipWithNext().any { (a, b) -> b.y < a.y - 0.0005f })
    }

    @Test fun tenPlinkoBallsStackInsideShortBinsWithoutOverlapping() {
        val landed = mutableListOf<Offset>()
        repeat(10) { turn ->
            val drop = requireNotNull(plinkoDrop(if (turn % 2 == 0) 0.08f else 0.92f, 2, 5, landed, turn))
            val end = drop.frames.last()
            assertEquals(2, (end.x * 5).toInt())
            landed.forEach {
                assertTrue((end - it).getDistance() >= PLINKO_BALL_RADIUS * 2 - 0.001f)
            }
            landed += end
        }
        assertTrue("Bins occupy less than a fifth of the playfield",
            (PLINKO_FLOOR - PLINKO_BIN_TOP) / PLINKO_HEIGHT < 0.2f)
    }

    @Test fun historicalPoolsWithMoreRewardsStillFitTenBallsInOneBin() {
        val bins = RewardType.entries.size
        val landed = mutableListOf<Offset>()
        repeat(10) { turn ->
            val drop = requireNotNull(plinkoDrop(if (turn % 2 == 0) 0.08f else 0.92f, 0, bins, landed, turn))
            val end = drop.frames.last()
            assertEquals(0, (end.x * bins).toInt())
            landed.forEach { assertTrue((end - it).getDistance() >= plinkoBallRadius(bins) * 2 - 0.001f) }
            landed += end
        }
    }

    @Test fun clawPicksExposedExistingCapsulesAndDropsThemUnderGravity() {
        var scene = initialClawScene()
        val originalIds = scene.balls.map { it.id }.toSet()
        val pickedIds = mutableSetOf<Int>()
        var pileShifted = false
        for (position in listOf(0.08f, 0.92f, 0.5f, 0.2f, 0.8f)) {
            val pickup = clawPickup(scene, position)
            val picked = scene.balls.single { it.id == pickup.ballId }
            assertTrue(pickedIds.add(picked.id))
            assertFalse("The claw cannot grab through a ball above its target", scene.balls.any {
                it.id != picked.id && it.id !in scene.collected && it.center.y < picked.center.y &&
                    abs(it.center.x - picked.center.x) < CLAW_BALL_RADIUS * 2f
            })
            assertEquals(scene.balls, pickup.frames.first().balls)
            val lifted = pickup.at(0.6f)
            assertEquals(picked.id, lifted.heldBall)
            assertEquals(lifted.claw.x, lifted.balls.single { it.id == picked.id }.center.x, 0.001f)
            assertEquals(lifted.claw.y + 0.085f, lifted.balls.single { it.id == picked.id }.center.y, 0.001f)
            val falling = pickup.frames.filterIndexed { index, _ ->
                index.toFloat() / pickup.frames.lastIndex in 0.8f..0.85f
            }.map { frame -> frame.balls.single { it.id == picked.id }.center.y }
            val distances = falling.zipWithNext { a, b -> b - a }
            assertTrue("Released capsules accelerate downward", distances.zipWithNext().all { (a, b) -> b > a })
            pickup.frames.zipWithNext().forEach { (before, after) ->
                before.balls.zip(after.balls).forEach { (a, b) ->
                    assertTrue("Capsule ${a.id} must not teleport from ${a.center} to ${b.center}",
                        (a.center - b.center).getDistance() < 0.045f)
                }
            }
            pileShifted = pileShifted || scene.balls.any { ball ->
                ball.id != picked.id && ball.id !in scene.collected &&
                    (ball.center - pickup.frames.last().balls.single { it.id == ball.id }.center).getDistance() > 0.005f
            }
            scene = pickup.frames.last()
            assertEquals(originalIds, scene.balls.map { it.id }.toSet())
            assertEquals(pickedIds, scene.collected)
            scene.balls.forEach { ball ->
                assertTrue(ball.center.y <= CLAW_FLOOR - CLAW_BALL_RADIUS + 0.001f)
                assertTrue(if (ball.id in scene.collected) ball.center.x in 0.81f..0.94f
                    else ball.center.x in 0.085f..0.67f)
            }
        }
        assertTrue("The remaining pile settles after pickups instead of staying in fixed slots", pileShifted)
    }
}
