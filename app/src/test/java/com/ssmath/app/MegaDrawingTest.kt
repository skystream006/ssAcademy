package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MegaDrawingTest {
    private fun image(width: Int = 320, height: Int = 220, draw: DrawScope.() -> Unit): ImageBitmap {
        val bitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat()), draw)
        return bitmap
    }

    private fun ImageBitmap.pixels() = IntArray(width * height).also { readPixels(it) }

    private fun body(pokemon: Celebration, motion: Float): IntArray = image {
        drawRect(Color.White)
        withTransform({ translate(160f, 129f) }) { drawMegaPokemon(pokemon, motion) }
    }.pixels()

    @Test fun allNinetyFiveBodiesHaveDistinctGeometryAndArticulateWithoutEffects() {
        val white = Color.White.toArgb()
        val fingerprints = mutableSetOf<Int>()
        val silhouettes = mutableSetOf<Int>()
        Celebration.megaPokemons.forEach { pokemon ->
            val still = body(pokemon, 0f)
            val moved = body(pokemon, 0.85f)
            assertTrue("${pokemon.label} needs substantial visible body art", still.count { it != white } > 1800)
            assertTrue("${pokemon.name} repeats another body", fingerprints.add(still.contentHashCode()))
            assertTrue("${pokemon.name} repeats another silhouette",
                silhouettes.add(still.map { it != white }.hashCode()))
            assertTrue("${pokemon.name} must articulate without stage movement or particles",
                still.indices.count { still[it] != moved[it] } > 100)
            assertFalse("${pokemon.name} must fit its artboard", still.indices.any {
                (it % 320 < 35 || it % 320 > 285 || it / 320 < 24 || it / 320 > 215) && still[it] != white
            })
        }
    }

    @Test fun everySceneAnimatesOpaqueDeterministicFramesThroughoutPlayback() {
        val ink = Color(KANTO_INK).toArgb()
        Celebration.megaPokemons.forEach { pokemon ->
            var previous = image { drawCelebrationArtwork(pokemon, 0f) }.pixels()
            listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f).forEach { progress ->
                val frame = image { drawCelebrationArtwork(pokemon, progress) }.pixels()
                assertTrue("${pokemon.name} at $progress must fill every pixel", frame.all { it ushr 24 == 255 })
                assertTrue("${pokemon.name} at $progress must animate", frame.indices.count { frame[it] != previous[it] } > 500)
                assertTrue("${pokemon.name} at $progress must move its body outline",
                    frame.indices.count { (frame[it] == ink) != (previous[it] == ink) } > 80)
                assertFalse("${pokemon.name} clips its body at $progress", frame.indices.any {
                    (it % 320 == 0 || it % 320 == 319 || it / 320 == 0 || it / 320 == 219) && frame[it] == ink
                })
                previous = frame
            }
            assertArrayEquals(pokemon.name, previous, image { drawCelebrationArtwork(pokemon, 1f) }.pixels())
        }
    }

    @Test fun previewsAndWideScenesRemainOpaqueWithVisibleCreatures() {
        Celebration.megaPokemons.forEach { pokemon ->
            listOf(96 to 66, 480 to 220).forEach { (width, height) ->
                listOf(0f, 0.5f, 1f).forEach { progress ->
                    val frame = image(width, height) { drawCelebrationArtwork(pokemon, progress) }.pixels()
                    assertTrue("${pokemon.name} at ${width}x$height", frame.all { it ushr 24 == 255 })
                    assertTrue("${pokemon.name} needs visible thumbnail detail", frame.count {
                        ((it ushr 16) and 255) + ((it ushr 8) and 255) + (it and 255) < 480
                    } > 20)
                }
            }
        }
    }

    @Test fun everyFormHasASeparateHabitatCompositionAndMotionPath() {
        val habitats = mutableSetOf<Int>()
        val paths = mutableSetOf<List<MegaPose>>()
        Celebration.megaPokemons.forEach { pokemon ->
            val background = image { drawMegaHabitat(pokemon, 0.5f) }.pixels()
            assertTrue(pokemon.name, habitats.add(background.contentHashCode()))
            assertTrue(pokemon.name, background.all { it ushr 24 == 255 })
            val poses = (0..20).map { megaScenePose(pokemon, it / 20f) }
            assertTrue(pokemon.name, paths.add(poses))
            assertEquals(21, poses.distinct().size)
            assertEquals(megaScenePose(pokemon, 0f), megaScenePose(pokemon, -1f))
            assertEquals(megaScenePose(pokemon, 1f), megaScenePose(pokemon, 2f))
        }
    }

    @Test fun multipartAndLetterFormsRemainVisuallyDifferent() {
        val groups = listOf(
            listOf(Celebration.MEGA_RAICHU_X, Celebration.MEGA_RAICHU_Y),
            listOf(Celebration.MEGA_CHARIZARD_X, Celebration.MEGA_CHARIZARD_Y),
            listOf(Celebration.MEGA_GARCHOMP, Celebration.MEGA_GARCHOMP_Z),
            listOf(Celebration.MEGA_ABSOL, Celebration.MEGA_ABSOL_Z),
            listOf(Celebration.MEGA_LUCARIO, Celebration.MEGA_LUCARIO_Z),
            listOf(Celebration.MEGA_MEWTWO_X, Celebration.MEGA_MEWTWO_Y),
            listOf(Celebration.MEGA_TATSUGIRI, Celebration.MEGA_TATSUGIRI_C, Celebration.MEGA_TATSUGIRI_D),
            listOf(Celebration.MEGA_LATIAS, Celebration.MEGA_LATIOS)
        )
        groups.forEach { forms ->
            val frames = forms.map { body(it, 0.4f) }
            for (a in frames.indices) for (b in a + 1 until frames.size) {
                assertTrue("${forms[a]} and ${forms[b]} must differ beyond tiny marks",
                    frames[a].indices.count { frames[a][it] != frames[b][it] } > 1400)
            }
        }
        assertThrows(IllegalArgumentException::class.java) { image { drawMegaPokemon(Celebration.GENGAR, 0f) } }
    }

    @Test fun garchompZRetainsRedBladesWhiteSpikesAndNavyBodyAcrossMotion() {
        val bladeColors = setOf(Color(0xFFCE553C).toArgb(), Color(0xFFE77543).toArgb())
        val spike = Color(0xFFF3EEE3).toArgb()
        val navy = Color(0xFF53556F).toArgb()
        val gold = Color(0xFFF2CB65).toArgb()
        listOf(-1f, 0f, 1f).forEach { motion ->
            val frame = body(Celebration.MEGA_GARCHOMP_Z, motion)
            for (side in listOf(-1, 1)) {
                val arm = frame.indices.filter { side * (it % 320 - 160) in 40..118 }
                assertTrue("Each Z forearm needs a broad red blade at $motion",
                    arm.count { frame[it] in bladeColors } > 450)
                assertTrue("Each Z blade needs visible white outer spikes at $motion",
                    arm.count { frame[it] == spike } > 160)
            }
            val torso = frame.indices.filter { it % 320 in 136..184 && it / 320 in 102..155 }
            assertTrue("Z needs a navy body, not purple wing membranes", torso.count { frame[it] == navy } > 150)
            assertTrue("Z needs the yellow chest chevrons", torso.count { frame[it] == gold } > 70)
        }
    }

    @Test fun renderReviewContactSheet() {
        val sheet = Bitmap.createBitmap(800, 19 * 128, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(sheet)
        val label = Paint().apply {
            color = android.graphics.Color.rgb(35, 42, 66)
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawColor(android.graphics.Color.WHITE)
        Celebration.megaPokemons.forEachIndexed { index, pokemon ->
            val x = (index % 5) * 160f
            val y = (index / 5) * 128f
            canvas.drawBitmap(image(160, 110) { drawCelebrationArtwork(pokemon, 0.43f) }.asAndroidBitmap(), x, y, null)
            canvas.drawText(pokemon.collectionLabel, x + 3f, y + 122f, label)
        }
        val output = File("build/reports/mega-artwork/contact-sheet.png")
        requireNotNull(output.parentFile).mkdirs()
        output.outputStream().use { assertTrue(sheet.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
