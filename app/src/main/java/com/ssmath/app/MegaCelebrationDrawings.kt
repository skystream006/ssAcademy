package com.ssmath.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class MegaPose(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scaleX: Float,
    val scaleY: Float,
    val articulation: Float
)

internal fun megaScenePose(celebration: Celebration, progress: Float): MegaPose {
    val profile = megaPokemonProfile(celebration)
    val seed = megaSceneSeed(celebration)
    val phase = progress.coerceIn(0f, 1f) * PI.toFloat() *
        (3.1f + seed % 11 * 0.13f + seed % 997 * 0.00013f)
    val beat = sin(phase)
    val counter = cos(phase * 0.73f)
    val m = sin(phase * 1.37f + seed % 7 * 0.2f)
    val scale = 0.83f
    return when (profile.motion) {
        MegaMotion.FLIGHT -> MegaPose(160f + beat * 9f, 134f + counter * 6f, beat * 5f, scale, scale, m)
        MegaMotion.SWIM -> MegaPose(160f + beat * 12f, 142f + counter * 4f, counter * 4f, scale, scale, m)
        MegaMotion.FLOAT -> MegaPose(160f + counter * 4f, 136f + beat * 7f, counter * 3f, scale, scale, m)
        MegaMotion.COIL -> MegaPose(160f + beat * 6f, 138f + counter * 4f, beat * 4f, scale + m * 0.015f, scale, m)
        MegaMotion.POUNCE -> MegaPose(160f + beat * 8f, 143f - abs(counter) * 10f,
            beat * 3f, scale - m * 0.015f, scale + m * 0.02f, m)
        MegaMotion.TURN -> MegaPose(160f + beat * 3f, 140f + counter * 3f,
            beat * 5f, scale - abs(counter) * 0.025f, scale, m)
        MegaMotion.STEP -> MegaPose(160f + beat * 5f, 140f - abs(counter) * 4f,
            beat * 2f, scale, scale + m * 0.01f, m)
    }
}

private fun megaSceneSeed(celebration: Celebration): Int =
    requireNotNull(celebration.ndex) * 31 + celebration.ndexSuffix.sumOf { it.code }

internal fun DrawScope.drawMegaCelebration(celebration: Celebration, progress: Float) {
    val profile = megaPokemonProfile(celebration)
    val time = progress.coerceIn(0f, 1f)
    val pose = megaScenePose(celebration, time)
    drawMegaHabitat(celebration, time)
    drawOval(Color(0xFF142238).copy(alpha = 0.17f),
        Offset(pose.x - 51f, 199f), Size(102f, 9f))
    drawMegaTypeEffects(profile.types, megaSceneSeed(celebration), time, false)
    withTransform({
        translate(pose.x, pose.y)
        rotate(pose.rotation, Offset.Zero)
        scale(pose.scaleX, pose.scaleY, Offset.Zero)
    }) {
        drawMegaPokemon(celebration, pose.articulation)
    }
    drawMegaTypeEffects(profile.types, megaSceneSeed(celebration), time, true)
}

internal fun DrawScope.drawMegaPokemon(celebration: Celebration, motion: Float) {
    require(celebration.category == CelebrationCategory.MEGA_POKEMONS)
    when (celebration) {
        Celebration.MEGA_MEGANIUM, Celebration.MEGA_EMBOAR, Celebration.MEGA_FERALIGATR,
        Celebration.MEGA_BEEDRILL, Celebration.MEGA_PIDGEOT, Celebration.MEGA_AMPHAROS,
        Celebration.MEGA_GYARADOS, Celebration.MEGA_BARBARACLE, Celebration.MEGA_STARMIE,
        Celebration.MEGA_FLOETTE, Celebration.MEGA_MEOWSTIC, Celebration.MEGA_PYROAR,
        Celebration.MEGA_RAICHU_X, Celebration.MEGA_RAICHU_Y, Celebration.MEGA_CLEFABLE,
        Celebration.MEGA_ALAKAZAM, Celebration.MEGA_GENGAR, Celebration.MEGA_SCOLIPEDE,
        Celebration.MEGA_VICTREEBEL, Celebration.MEGA_MEDICHAM, Celebration.MEGA_MANECTRIC,
        Celebration.MEGA_GARDEVOIR, Celebration.MEGA_GALLADE, Celebration.MEGA_HOUNDOOM,
        Celebration.MEGA_ALTARIA, Celebration.MEGA_AUDINO, Celebration.MEGA_LOPUNNY,
        Celebration.MEGA_BANETTE, Celebration.MEGA_CAMERUPT, Celebration.MEGA_EXCADRILL,
        Celebration.MEGA_GARCHOMP, Celebration.MEGA_GARCHOMP_Z -> drawMegaEarly(celebration, motion)
        Celebration.MEGA_SABLEYE, Celebration.MEGA_MAWILE, Celebration.MEGA_ABSOL,
        Celebration.MEGA_ABSOL_Z, Celebration.MEGA_LUCARIO, Celebration.MEGA_LUCARIO_Z,
        Celebration.MEGA_SLOWBRO, Celebration.MEGA_SHARPEDO, Celebration.MEGA_EELEKTROSS,
        Celebration.MEGA_DRAGONITE, Celebration.MEGA_VENUSAUR, Celebration.MEGA_CHARIZARD_X,
        Celebration.MEGA_CHARIZARD_Y, Celebration.MEGA_BLASTOISE, Celebration.MEGA_MALAMAR,
        Celebration.MEGA_DRAGALGE, Celebration.MEGA_GLALIE, Celebration.MEGA_FROSLASS,
        Celebration.MEGA_ABOMASNOW, Celebration.MEGA_SCIZOR, Celebration.MEGA_PINSIR,
        Celebration.MEGA_HERACROSS, Celebration.MEGA_HAWLUCHA, Celebration.MEGA_SCRAFTY,
        Celebration.MEGA_CHANDELURE, Celebration.MEGA_AERODACTYL, Celebration.MEGA_STEELIX,
        Celebration.MEGA_AGGRON, Celebration.MEGA_TYRANITAR, Celebration.MEGA_GRENINJA,
        Celebration.MEGA_FALINKS, Celebration.MEGA_CHESNAUGHT -> drawMegaMiddle(celebration, motion)
        Celebration.MEGA_SKARMORY, Celebration.MEGA_DELPHOX, Celebration.MEGA_SALAMENCE,
        Celebration.MEGA_KANGASKHAN, Celebration.MEGA_DRAMPA, Celebration.MEGA_METAGROSS,
        Celebration.MEGA_ZYGARDE, Celebration.MEGA_DIANCIE, Celebration.MEGA_MEWTWO_X,
        Celebration.MEGA_MEWTWO_Y, Celebration.MEGA_SCOVILLAIN, Celebration.MEGA_GLIMMORA,
        Celebration.MEGA_TATSUGIRI, Celebration.MEGA_TATSUGIRI_C, Celebration.MEGA_TATSUGIRI_D,
        Celebration.MEGA_BAXCALIBUR, Celebration.MEGA_SCEPTILE, Celebration.MEGA_BLAZIKEN,
        Celebration.MEGA_SWAMPERT, Celebration.MEGA_CHIMECHO, Celebration.MEGA_GOLISOPOD,
        Celebration.MEGA_GOLURK, Celebration.MEGA_STARAPTOR, Celebration.MEGA_CRABOMINABLE,
        Celebration.MEGA_HEATRAN, Celebration.MEGA_DARKRAI, Celebration.MEGA_LATIAS,
        Celebration.MEGA_LATIOS, Celebration.MEGA_RAYQUAZA, Celebration.MEGA_MAGEARNA,
        Celebration.MEGA_ZERAORA -> drawMegaLate(celebration, motion)
        else -> error("No Mega artwork for $celebration")
    }
}

internal fun DrawScope.drawMegaHabitat(celebration: Celebration, progress: Float) {
    val habitat = megaPokemonProfile(celebration).habitat
    val seed = megaSceneSeed(celebration)
    val drift = sin(progress * 4f + seed % 9) * 4f
    val night = habitat in listOf(MegaHabitat.CAVE, MegaHabitat.MOON, MegaHabitat.RUINS, MegaHabitat.STORM)
    val colors = when (habitat) {
        MegaHabitat.GARDEN -> 0xFFB6E6D5 to 0xFFF6EDBD
        MegaHabitat.FOREST -> 0xFF246C70 to 0xFFB8D99A
        MegaHabitat.VOLCANO -> 0xFF4E385F to 0xFFDD926C
        MegaHabitat.OCEAN -> 0xFF256F9D to 0xFF8DE0DB
        MegaHabitat.RIVER -> 0xFF92D2DD to 0xFFDFEFC7
        MegaHabitat.SKY -> 0xFF77B9E5 to 0xFFE4F3EE
        MegaHabitat.STORM -> 0xFF283558 to 0xFF8392AF
        MegaHabitat.CAVE -> 0xFF262B4B to 0xFF7985A3
        MegaHabitat.MOON -> 0xFF232947 to 0xFF897DAB
        MegaHabitat.SNOW -> 0xFF7EAAC8 to 0xFFEFF7FF
        MegaHabitat.DESERT -> 0xFFEAA96F to 0xFFFFE0A3
        MegaHabitat.RUINS -> 0xFF354B65 to 0xFF9EA6B3
        MegaHabitat.DOJO -> 0xFFBBC9E0 to 0xFFF9DDC8
    }
    drawRect(Brush.verticalGradient(listOf(Color(colors.first), Color(colors.second)), 0f, 220f),
        size = Size(320f, 220f))
    val celestialX = 35f + seed % 250
    drawCircle(if (night) Color(0xFFF4EBCB) else Color(0xFFFFF4C5),
        15f + seed % 9, Offset(celestialX, 32f + seed % 13))
    if (night) {
        repeat(18) { i ->
            val x = ((seed + i * 67) % 316).toFloat() + 2f
            val y = ((seed / 7 + i * 31) % 87).toFloat() + 5f
            drawCircle(Color.White.copy(alpha = 0.35f + (i % 3) * 0.15f), 0.8f + i % 2, Offset(x, y))
        }
    } else {
        repeat(4) { i ->
            val x = (i * 89 + seed % 43).toFloat() + drift
            drawOval(Color.White.copy(alpha = 0.42f), Offset(x - 30f, 38f + i % 2 * 27f), Size(74f, 18f))
        }
    }
    when (habitat) {
        MegaHabitat.GARDEN, MegaHabitat.FOREST -> {
            for (layer in 0..2) {
                val y = 131f + layer * 22f
                val green = listOf(0xFF83B39B, 0xFF548F7B, 0xFF356E61)[layer]
                drawPath(Path().apply {
                    moveTo(0f, y); cubicTo(70f, y - 38f - seed % 17, 165f, y + 30f, 320f, y - 8f)
                    lineTo(320f, 220f); lineTo(0f, 220f); close()
                }, Color(green))
            }
            repeat(6) { i ->
                val x = if (i < 3) 8f + i * 15f else 283f + (i - 3) * 15f
                val top = 68f + (seed + i * 13) % 35
                drawLine(Color(0xFF477669), Offset(x, top), Offset(x, 205f), 6f)
                drawOval(Color(0xFF508B78), Offset(x - 18f, top - 13f), Size(36f, 53f))
                if (habitat == MegaHabitat.GARDEN) {
                    repeat(4) { p ->
                        val a = p * PI.toFloat() / 2f
                        drawCircle(Color(0xFFF7BDD0), 4f, Offset(x + cos(a) * 5f, 193f + sin(a) * 5f))
                    }
                    drawCircle(Color(0xFFFFDD80), 3f, Offset(x, 193f))
                }
            }
        }
        MegaHabitat.OCEAN, MegaHabitat.RIVER -> {
            drawRect(Color(0xFF318FA8).copy(alpha = 0.72f), Offset(0f, 157f), Size(320f, 63f))
            repeat(5) { i ->
                val y = 160f + i * 12f
                drawPath(Path().apply {
                    moveTo(-20f, y)
                    for (x in -20..340 step 40) {
                        quadraticTo(x + 10f + drift, y - 5f, x + 40f, y)
                    }
                }, Color(0xFFBCF0E7).copy(alpha = 0.6f), style = Stroke(2f))
            }
            for (side in listOf(0f, 277f)) {
                drawOval(Color(0xFF607E85), Offset(side - 12f, 193f), Size(70f, 38f))
                repeat(4) { i ->
                    val x = side + i * 10f
                    drawPath(Path().apply {
                        moveTo(x, 220f)
                        cubicTo(x - 8f, 193f, x + 15f + drift, 184f, x + drift, 171f - i * 3)
                    }, Color(0xFF407C74), style = Stroke(4f, cap = StrokeCap.Round))
                }
            }
        }
        MegaHabitat.SKY, MegaHabitat.SNOW -> {
            repeat(4) { i ->
                val x = i * 100f - 40f + seed % 25
                val y = 112f + (seed + i * 11) % 29
                megaSceneryPolygon(0xFF8AAEC1, x - 50f, 199f, x + 48f, y, x + 123f, 201f)
                megaSceneryPolygon(0xFFE9F4F6, x + 15f, y + 32f, x + 48f, y,
                    x + 78f, y + 36f, x + 49f, y + 23f)
            }
            repeat(5) { i ->
                drawOval(Color(0xFFF3FAFA).copy(alpha = 0.85f),
                    Offset(i * 80f - 40f + drift, 186f + i % 2 * 12f), Size(110f, 39f))
            }
            if (habitat == MegaHabitat.SNOW) repeat(18) { i ->
                drawCircle(Color.White, 1.5f, Offset((seed + i * 53) % 320f,
                    (i * 23f + progress * 54f) % 215f))
            }
        }
        MegaHabitat.VOLCANO, MegaHabitat.DESERT -> {
            val rock = if (habitat == MegaHabitat.VOLCANO) 0xFF6D5261 else 0xFFD39866
            repeat(5) { i ->
                val x = i * 82f - 30f + seed % 21
                megaSceneryPolygon(rock, x - 35f, 190f, x + 20f, 96f + i % 2 * 29f,
                    x + 48f, 97f + i % 2 * 29f, x + 112f, 191f)
            }
            drawRect(Color(if (habitat == MegaHabitat.VOLCANO) 0xFF594656 else 0xFFEFC285),
                Offset(0f, 190f), Size(320f, 30f))
            repeat(6) { i ->
                val x = i * 62f + seed % 19
                drawPath(Path().apply {
                    moveTo(x, 218f); lineTo(x + 18f, 206f); lineTo(x + 5f, 197f)
                }, Color(if (habitat == MegaHabitat.VOLCANO) 0xFFFFBE65 else 0xFFD9A16E),
                    style = Stroke(if (habitat == MegaHabitat.VOLCANO) 3f else 1.5f))
            }
        }
        MegaHabitat.CAVE, MegaHabitat.RUINS -> {
            repeat(7) { i ->
                val x = i * 52f - 5f + seed % 17
                val height = 24f + (seed + i * 17) % 49
                megaSceneryPolygon(0xFF454D6B, x - 28f, 0f, x + 11f, height, x + 32f, 0f)
            }
            drawOval(Color(0xFF647388), Offset(-20f, 181f), Size(360f, 67f))
            for (side in listOf(17f, 291f)) {
                if (habitat == MegaHabitat.CAVE) {
                    repeat(3) { i ->
                        val x = side + (i - 1) * 11f
                        val top = 144f - i * 12f - seed % 10
                        megaSceneryPolygon(0xFFA2B8D0, x - 7f, 203f, x - 7f, top + 12f,
                            x, top, x + 8f, top + 13f, x + 6f, 203f)
                        drawLine(Color(0xFFE5EDFC), Offset(x, top + 5f), Offset(x, 197f), 1.5f)
                    }
                } else {
                    drawRect(Color(0xFF9CA9B7), Offset(side - 11f, 101f), Size(22f, 102f))
                    drawRect(Color(0xFFBAC5CC), Offset(side - 16f, 98f), Size(32f, 9f))
                    repeat(4) { i ->
                        drawLine(Color(0xFF7D899E), Offset(side - 10f, 121f + i * 23f),
                            Offset(side + 10f, 121f + i * 23f), 2f)
                    }
                }
            }
        }
        MegaHabitat.MOON, MegaHabitat.STORM -> {
            repeat(3) { i ->
                val y = 167f + i * 17f
                drawPath(Path().apply {
                    moveTo(0f, y); cubicTo(79f, y - 24f, 211f, y + 12f, 320f, y - 7f)
                    lineTo(320f, 220f); lineTo(0f, 220f); close()
                }, Color(listOf(0xFF626589, 0xFF4C5375, 0xFF394463)[i]))
            }
            repeat(4) { i ->
                val x = i * 96f + drift + seed % 15
                drawOval(Color(0xFF596383).copy(alpha = 0.5f),
                    Offset(x - 35f, 40f + i % 2 * 18f), Size(94f, 26f))
            }
            if (habitat == MegaHabitat.STORM) {
                val x = 20f + seed % 260
                drawPath(Path().apply {
                    moveTo(x, 54f); lineTo(x - 9f, 76f); lineTo(x + 7f, 73f); lineTo(x - 3f, 97f)
                }, Color(0xFFFFEDAD).copy(alpha = 0.55f + 0.2f * sin(progress * 21f)),
                    style = Stroke(2.5f))
            }
        }
        MegaHabitat.DOJO -> {
            drawRect(Color(0xFF8E8B9C), Offset(0f, 169f), Size(320f, 51f))
            repeat(7) { i ->
                drawLine(Color(0xFFB7ADB6), Offset(i * 57f - 10f, 170f),
                    Offset(i * 66f - 41f, 220f), 1.5f)
            }
            repeat(3) { i ->
                drawLine(Color(0xFFB7ADB6), Offset(0f, 177f + i * 17f), Offset(320f, 177f + i * 17f), 1.5f)
            }
            for (x in listOf(15f, 305f)) {
                drawRect(Color(0xFF775F72), Offset(x - 4f, 78f), Size(8f, 99f))
                drawRect(Color(0xFFB56F75), Offset(x - 10f, 74f), Size(20f, 9f))
                drawRect(Color(0xFFE3BA91), Offset(x - 6f, 91f), Size(12f, 34f))
                drawLine(Color(0xFFAA7C77), Offset(x, 96f), Offset(x, 119f), 2f)
            }
        }
    }
}

private fun DrawScope.megaSceneryPolygon(color: Long, vararg points: Float) {
    drawPath(Path().apply {
        moveTo(points[0], points[1])
        for (index in 2 until points.size step 2) lineTo(points[index], points[index + 1])
        close()
    }, Color(color))
}

private fun DrawScope.drawMegaTypeEffects(types: List<String>, seed: Int, progress: Float, foreground: Boolean) {
    val type = types[if (foreground) types.lastIndex else 0]
    repeat(if (foreground) 5 else 8) { i ->
        val phase = (progress * (0.8f + i * 0.03f) + i * 0.173f + seed % 23 / 23f) % 1f
        val side = if (i % 2 == 0) -1f else 1f
        val x = 160f + side * (87f + i % 3 * 12f) + sin(phase * 7f + i) * 8f
        val y = if (foreground) 206f - phase * 65f else 173f - phase * 115f
        val alpha = (sin(phase * PI.toFloat()) * 0.7f).coerceIn(0f, 0.7f)
        val center = Offset(x, y)
        val color = Color(when (type) {
            "Fire" -> 0xFFFFBF6C
            "Water", "Ice" -> 0xFFB6F4FF
            "Grass", "Bug" -> 0xFFB7E998
            "Electric" -> 0xFFFFF1A0
            "Fairy", "Psychic" -> 0xFFF9C8F0
            "Ghost", "Poison", "Dark" -> 0xFFD8B8F1
            "Steel", "Rock", "Ground" -> 0xFFE1D3B7
            else -> 0xFFDCF1FF
        }).copy(alpha = alpha)
        when (type) {
            "Fire" -> drawPath(Path().apply {
                moveTo(x, y - 10f); cubicTo(x + 1f, y - 3f, x + 9f, y + 1f, x + 3f, y + 6f)
                cubicTo(x - 8f, y + 9f, x - 7f, y, x, y - 10f); close()
            }, color)
            "Water" -> {
                drawCircle(color, 3f + i % 3, center, style = Stroke(1.6f))
                drawCircle(color, 1f, Offset(x - 1.2f, y - 1.4f))
            }
            "Grass", "Bug" -> rotate(side * phase * 160f, center) {
                drawPath(Path().apply {
                    moveTo(x - 6f, y); quadraticTo(x, y - 9f, x + 7f, y)
                    quadraticTo(x, y + 6f, x - 6f, y); close()
                }, color)
                drawLine(Color(0xFF508767).copy(alpha = alpha), Offset(x - 4f, y), Offset(x + 4f, y), 1f)
            }
            "Electric" -> drawPath(Path().apply {
                moveTo(x + 3f, y - 9f); lineTo(x - 4f, y); lineTo(x + 3f, y - 1f); lineTo(x - 3f, y + 9f)
            }, color, style = Stroke(2.2f))
            "Ice", "Fairy", "Psychic", "Steel" -> {
                drawLine(color, Offset(x - 5f, y), Offset(x + 5f, y), 1.6f)
                drawLine(color, Offset(x, y - 7f), Offset(x, y + 7f), 1.6f)
                if (type == "Ice") {
                    drawLine(color, Offset(x - 4f, y - 4f), Offset(x + 4f, y + 4f), 1.2f)
                    drawLine(color, Offset(x + 4f, y - 4f), Offset(x - 4f, y + 4f), 1.2f)
                }
            }
            "Poison", "Ghost", "Dark" -> drawPath(Path().apply {
                moveTo(x, y + 9f); cubicTo(x - 12f, y + 1f, x + 10f, y - 3f, x, y - 12f)
            }, color, style = Stroke(3f, cap = StrokeCap.Round))
            "Rock", "Ground" -> rotate(phase * 160f, center) {
                drawPath(Path().apply {
                    moveTo(x - 4f, y - 2f); lineTo(x + 2f, y - 5f)
                    lineTo(x + 5f, y + 2f); lineTo(x - 1f, y + 5f); close()
                }, color)
            }
            "Fighting" -> {
                drawLine(color, Offset(x - side * 12f, y + 7f), Offset(x + side * 4f, y - 5f), 2f)
                drawLine(color, Offset(x - side * 9f, y + 12f), Offset(x + side * 6f, y), 1.4f)
            }
            else -> drawPath(Path().apply {
                moveTo(x - 12f, y + 5f); quadraticTo(x, y - 6f, x + 14f, y - 2f)
            }, color, style = Stroke(1.8f, cap = StrokeCap.Round))
        }
    }
}
