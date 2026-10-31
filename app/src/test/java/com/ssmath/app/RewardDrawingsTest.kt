package com.ssmath.app

import android.app.Application
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RewardDrawingsTest {
    @Test fun allRewardsRenderDistinctWholeAndPartialArtwork() {
        val signatures = mutableSetOf<Int>()
        RewardType.entries.forEach { type ->
            val whole = artwork(type, false)
            val fragment = artwork(type, true)
            val wholeArea = whole.count { it ushr 24 > 64 }
            val fragmentArea = fragment.count { it ushr 24 > 64 }
            assertTrue("$type has a detailed whole illustration", wholeArea > 4_000)
            assertTrue("$type fragment remains visible", fragmentArea > 1_000)
            assertTrue("$type fragment is a cut portion, not another whole reward",
                fragmentArea.toFloat() / wholeArea in 0.10f..0.46f)
            assertTrue("$type whole has varied colors", whole.toSet().size > 20)
            assertTrue("$type fragment has varied colors", fragment.toSet().size > 20)
            assertTrue("$type fracture must not leave a stroke above the artwork",
                fragment.take(16 * 200).all { it ushr 24 == 0 })
            assertTrue("$type fracture must not leave a stroke below the artwork",
                fragment.takeLast(16 * 200).all { it ushr 24 == 0 })
            signatures += whole.contentHashCode()
            signatures += fragment.contentHashCode()
        }
        assertEquals(RewardType.entries.size * 2, signatures.size)
    }

    @Test fun newRewardAnimationsMoveWholeAndFragmentDetailsAndLoopSmoothly() {
        listOf(RewardType.BED_TIME, RewardType.RESTAURANT,
            RewardType.TOYS_1, RewardType.TOYS_2, RewardType.TOYS_3,
            RewardType.GOOD_BEHAVIOR_POINT).forEach { type ->
            listOf(false, true).forEach { fragment ->
                fun frame(progress: Float) = render(200, 200) { drawRewardArtwork(type, fragment, progress) }
                val first = frame(0f)
                listOf(0.25f, 0.5f, 0.75f).forEach { progress ->
                    val animated = frame(progress)
                    assertTrue("$type fragment=$fragment moves at $progress",
                        first.indices.count { first[it] != animated[it] } > 50)
                    assertTrue(animated.take(16 * 200).all { it ushr 24 == 0 })
                    assertTrue(animated.takeLast(16 * 200).all { it ushr 24 == 0 })
                }
                val last = frame(1f)
                assertTrue("$type fragment=$fragment returns to its initial pose",
                    first.indices.count { first[it] != last[it] } < 10)
                assertArrayEquals(first, frame(-1f))
                assertArrayEquals(last, frame(2f))
            }
        }
    }

    @Test fun toyWholeArtworkHasDistinctStablePriceBadgesThatAreNotCopiedOntoFragments() {
        val badges = mutableSetOf<Int>()
        listOf(RewardType.TOYS_1, RewardType.TOYS_2, RewardType.TOYS_3).forEach { type ->
            val badgePixels = (23..69).flatMap { y ->
                (132..178).filter { x -> (x - 155) * (x - 155) + (y - 46) * (y - 46) < 23 * 23 }
                    .map { x -> y * 200 + x }
            }
            fun badge(progress: Float): IntArray {
                val pixels = render(200, 200) { drawRewardArtwork(type, false, progress) }
                return badgePixels.map { pixels[it] }.toIntArray()
            }
            val first = badge(0f)
            assertTrue("$type badge is solid and legible", first.all { it ushr 24 == 255 })
            assertTrue("$type badge includes dark denomination lettering",
                first.count { it == Color(0xFF38324F).toArgb() } > 150)
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { assertArrayEquals(first, badge(it)) }
            badges += first.contentHashCode()
            val fragment = artwork(type, true)
            assertTrue("A $type fragment does not contain a whole price badge",
                badgePixels.all { fragment[it] ushr 24 == 0 })
        }
        assertEquals("Each price is drawn with a different numeral", 3, badges.size)
    }

    @Test fun tierThreeFragmentsKeepFullSizeDetailsAndJaggedCutEdgesThroughoutTheirAnimations() {
        listOf(RewardType.TOYS_1, RewardType.TOYS_2, RewardType.TOYS_3,
            RewardType.GOOD_BEHAVIOR_POINT).forEach { type ->
            val cutX = if (type == RewardType.TOYS_2 || type == RewardType.TOYS_3) 83 else 78
            listOf(0f, 0.25f, 0.5f, 0.75f).forEach { progress ->
                val whole = render(200, 200) { drawRewardArtwork(type, false, progress) }
                val fragment = render(200, 200) { drawRewardArtwork(type, true, progress) }
                // Native curves can rasterize differently after clipping; compare their solid interiors.
                val source = whole.indices.filter { pixel ->
                    pixel % 200 in 1 until cutX - 5 && pixel / 200 in 1..198 &&
                        whole[pixel] ushr 24 == 255 &&
                        listOf(pixel - 1, pixel + 1, pixel - 200, pixel + 200).all { neighbor ->
                            whole[neighbor] ushr 24 == 255 && listOf(0, 8, 16).all { shift ->
                                abs((whole[pixel] ushr shift and 255) -
                                    (whole[neighbor] ushr shift and 255)) <= 8
                            }
                        }
                }
                assertTrue("$type retains a substantial full-size piece", source.size > 1_000)
                val matching = source.count { pixel ->
                    listOf(0, 8, 16, 24).all { shift ->
                        abs((whole[pixel] ushr shift and 255) -
                            (fragment[pixel + 32] ushr shift and 255)) <= 2
                    }
                }
                assertTrue("$type fragment keeps full-size animated details at $progress: $matching/${source.size}",
                    matching >= source.size * 0.995f)
                val rows = if (type == RewardType.TOYS_3) 108..136 else 88..120
                val edges = rows.map { y ->
                    (0 until 200).last { x -> fragment[y * 200 + x] ushr 24 > 128 }
                }
                assertTrue("$type has a visibly broken edge at $progress", edges.toSet().size >= 5)
            }
        }
    }

    @Test fun goodBehaviorPointMedalKeepsItsNumeralLegibleThroughoutAnimation() {
        val type = RewardType.GOOD_BEHAVIOR_POINT
        val numeralPixels = (64..121).flatMap { y -> (82..121).map { x -> y * 200 + x } }
        fun numeral(progress: Float): IntArray {
            val whole = render(200, 200) { drawRewardArtwork(type, false, progress) }
            return numeralPixels.map { whole[it] }.toIntArray()
        }
        val first = numeral(0f)
        assertTrue(first.all { it ushr 24 == 255 })
        assertTrue(first.count { it == Color(0xFF38324F).toArgb() } > 400)
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { assertArrayEquals(first, numeral(it)) }
    }

    @Test fun existingRewardArtworkDoesNotChangeWithAnimationProgress() {
        listOf(RewardType.LOLLIPOP, RewardType.ICE_CREAM, RewardType.GUMMI_BEAR,
            RewardType.RAMEN, RewardType.VIDEO_GAME).forEach { type ->
            listOf(false, true).forEach { fragment ->
                assertArrayEquals(artwork(type, fragment),
                    render(200, 200) { drawRewardArtwork(type, fragment, 0.5f) })
            }
        }
    }

    @Test fun ramenFragmentKeepsFullHeightAndHasARealJaggedMissingEdge() {
        val whole = artwork(RewardType.RAMEN, false)
        val fragment = artwork(RewardType.RAMEN, true)
        fun opaque(pixels: IntArray) = pixels.indices.filter { pixels[it] ushr 24 > 128 }
        val wholePoints = opaque(whole)
        val fragmentPoints = opaque(fragment)
        val wholeHeight = wholePoints.maxOf { it / 200 } - wholePoints.minOf { it / 200 }
        val fragmentHeight = fragmentPoints.maxOf { it / 200 } - fragmentPoints.minOf { it / 200 }
        assertTrue("A fragment must retain the full-size noodle block's height",
            abs(wholeHeight - fragmentHeight) <= 3)
        val rightEdges = (56..144).map { y ->
            (0 until 200).last { x -> fragment[y * 200 + x] ushr 24 > 128 }
        }
        assertTrue("The broken side must visibly zig-zag", rightEdges.toSet().size >= 5)
        val width = fragmentPoints.maxOf { it % 200 } - fragmentPoints.minOf { it % 200 }
        assertTrue("Only about a third of the square remains", width in 40..53)
    }

    @Test fun videoGameWholeIncludesTabletButFragmentIsOnlyPartOfController() {
        val whole = artwork(RewardType.VIDEO_GAME, false)
        val fragment = artwork(RewardType.VIDEO_GAME, true)
        assertTrue("Whole game includes the tablet above the controller",
            whole.take(60 * 200).count { it ushr 24 > 128 } > 2_000)
        assertTrue("No tablet is included in a game fragment",
            fragment.take(60 * 200).all { it ushr 24 == 0 })
        assertTrue(fragment.drop(75 * 200).count { it ushr 24 > 128 } > 1_000)
    }

    @Test fun illustrationsStayCenteredAndUndistortedInWideAndTallBounds() {
        RewardType.entries.forEach { type ->
            listOf(false, true).forEach { fragment ->
                val square = artwork(type, fragment)
                val wide = render(400, 200) { drawRewardArtwork(type, fragment) }
                val tall = render(200, 400) { drawRewardArtwork(type, fragment) }
                assertTrue(wide.indices.filter { it % 400 < 100 || it % 400 >= 300 }
                    .all { wide[it] == 0 })
                assertTrue(tall.take(100 * 200).all { it == 0 })
                val wideCenter = IntArray(40_000) { wide[it / 200 * 400 + it % 200 + 100] }
                val tallCenter = tall.copyOfRange(20_000, 60_000)
                listOf(wideCenter, tallCenter).forEach { centered ->
                    val matching = square.indices.count { pixel ->
                        listOf(0, 8, 16, 24).all { shift ->
                            abs((square[pixel] ushr shift and 255) -
                                (centered[pixel] ushr shift and 255)) <= 2
                        }
                    }
                    // Native gradient/edge rasterization can round differently after translation.
                    assertTrue("$type preserves size, shape, and color: $matching of ${square.size} pixels match",
                        matching >= square.size * 0.995f)
                }
            }
        }
    }

    @Test fun giftHopsOpensAndFinishesWithAStableConfettiFreeFrame() {
        fun gift(progress: Float, hop: Float = 0f) = render(280, 220) { drawRewardGift(progress, hop) }
        val closed = gift(0f)
        val hopping = gift(0f, 1f)
        val opening = gift(0.3f)
        val burst = gift(0.6f)
        val finished = gift(1f)
        fun changed(a: IntArray, b: IntArray) = a.indices.count { a[it] != b[it] }
        assertTrue(changed(closed, hopping) > 2_000)
        assertTrue(changed(closed, opening) > 2_000)
        assertTrue(changed(opening, burst) > 2_000)
        assertTrue(changed(burst, finished) > 100)
        assertArrayEquals(finished, gift(1.5f))
        val confettiColors = setOf(Color(0xFF38C9B2).toArgb(), Color(0xFF55BEEA).toArgb())
        fun outwardConfetti(pixels: IntArray) = pixels.indices.count {
            (it % 280 < 84 || it % 280 > 196 || it / 280 < 80) && pixels[it] in confettiColors
        }
        assertTrue("Confetti shoots beyond the box before disappearing",
            outwardConfetti(burst) > outwardConfetti(finished) + 20)
    }

    private fun artwork(type: RewardType, fragment: Boolean) =
        render(200, 200) { drawRewardArtwork(type, fragment) }

    private fun render(width: Int, height: Int, draw: DrawScope.() -> Unit): IntArray {
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image),
            Size(width.toFloat(), height.toFloat()), draw)
        return IntArray(width * height).also { image.readPixels(it) }
    }
}
