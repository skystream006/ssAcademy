package com.ssmath.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class KantoScenePose(
    val x: Float = 160f,
    val y: Float = 145f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val articulation: Float = 0f
)

internal fun KantoScenePose.project(point: Offset): Offset {
    val angle = rotation * PI.toFloat() / 180f
    val x = point.x * scaleX
    val y = point.y * scaleY
    return Offset(this.x + x * cos(angle) - y * sin(angle), this.y + x * sin(angle) + y * cos(angle))
}

internal data class KantoWaterJet(val origin: Offset, val reach: Offset)

internal fun kantoSuspensionAnchor(ndex: Int, articulation: Float): Offset = when (ndex) {
    11 -> Offset(-13f, -62f)
    14 -> Offset(0f, -62f)
    70 -> Offset(36f, -45f + articulation * 3f)
    else -> error("No suspension anchor for Ndex $ndex")
}

internal fun kantoWaterJets(ndex: Int, articulation: Float): List<KantoWaterJet> = when (ndex) {
    9 -> listOf(
        KantoWaterJet(Offset(-39f, -57.5f), Offset(-15f, -47f)),
        KantoWaterJet(Offset(39f, -57.5f), Offset(15f, -47f))
    )
    55 -> listOf(KantoWaterJet(Offset(0f, -8f), Offset(73f, -28f)))
    62 -> listOf(
        KantoWaterJet(Offset(-76f, -17f - articulation * 4f), Offset(-38f, -22f)),
        KantoWaterJet(Offset(76f, -17f + articulation * 4f), Offset(38f, -22f))
    )
    99 -> listOf(KantoWaterJet(Offset(0f, 20f), Offset(-82f, -12f)))
    117 -> listOf(KantoWaterJet(Offset(56f, -25.5f), Offset(53f, -5f)))
    119 -> listOf(KantoWaterJet(Offset(-62f, 15.5f), Offset(-48f, -8f)))
    130 -> listOf(KantoWaterJet(Offset(-48f, 0f), Offset(-63f, -8f)))
    139 -> listOf(KantoWaterJet(Offset(0f, 18f), Offset(72f, 17f)))
    else -> error("No water jet emitter for Ndex $ndex")
}

internal fun kantoScenePose(ndex: Int, motion: KantoMotion, progress: Float): KantoScenePose {
    val time = progress.coerceIn(0f, 1f) * PI.toFloat() * 2f * (2.3f + ndex % 7 * 0.17f) +
        ndex * 0.071f
    val beat = sin(time)
    val travel = sin(time * 0.63f + ndex * 0.31f)
    val lift = abs(beat)
    val pose = KantoScenePose(articulation = beat)
    return when (motion) {
        KantoMotion.SWAY -> pose.copy(x = 160f + beat * 4f, rotation = beat * 6f)
        KantoMotion.CRAWL -> pose.copy(x = 160f + travel * 27f, y = 146f - lift * 3f,
            scaleX = 1f + beat * 0.045f, scaleY = 1f - beat * 0.035f)
        KantoMotion.HANG -> pose.copy(x = 160f + beat * 15f, y = 134f + lift * 3f,
            rotation = beat * -12f)
        KantoMotion.FLUTTER -> pose.copy(x = 160f + travel * 24f, y = 135f + beat * 9f,
            rotation = travel * 5f, scaleX = 0.92f + beat * 0.08f)
        KantoMotion.FLY -> pose.copy(x = 160f + travel * 23f, y = 135f + beat * 8f,
            rotation = travel * 7f, scaleX = 0.92f, scaleY = 0.92f)
        KantoMotion.SCURRY -> pose.copy(x = 160f + travel * 32f, y = 147f - lift * 5f,
            rotation = beat * 2f)
        KantoMotion.SLITHER -> pose.copy(x = 160f + travel * 25f, y = 146f + beat * 2f,
            scaleX = 1f + beat * 0.07f, scaleY = 1f - beat * 0.04f)
        KantoMotion.HOP -> pose.copy(x = 160f + travel * 15f, y = 147f - lift * 22f,
            rotation = beat * 3f)
        KantoMotion.PROWL -> pose.copy(x = 160f + travel * 25f, y = 147f - lift * 4f,
            rotation = beat * 3f)
        KantoMotion.GALLOP -> pose.copy(x = 160f + travel * 29f, y = 143f - lift * 12f,
            rotation = beat * 6f, scaleX = 0.94f, scaleY = 0.94f)
        KantoMotion.SWIM -> pose.copy(x = 160f + travel * 27f, y = 141f + beat * 8f,
            rotation = cos(time) * 8f, scaleX = 0.93f, scaleY = 0.93f)
        KantoMotion.FLOAT -> pose.copy(x = 160f + travel * 12f, y = 136f + beat * 11f,
            rotation = travel * 5f)
        KantoMotion.BOB -> pose.copy(x = 160f + travel * 9f, y = 146f + beat * 6f,
            rotation = beat * 4f)
        KantoMotion.DIG -> pose.copy(x = 160f + travel * 10f, y = 151f + (beat + 1f) * 20f)
        KantoMotion.ROLL -> pose.copy(x = 160f + travel * 28f, y = 145f - lift * 5f,
            rotation = time * 57.3f, scaleX = 0.7f, scaleY = 0.7f)
        KantoMotion.PULSE -> pose.copy(x = 160f + travel * 9f, y = 139f + beat * 7f,
            scaleX = 0.96f + beat * 0.07f, scaleY = 0.96f - beat * 0.07f)
        KantoMotion.LEVITATE -> pose.copy(x = 160f + travel * 14f, y = 136f + beat * 10f,
            rotation = beat * 4f)
        KantoMotion.TELEPORT -> pose.copy(x = 160f + travel * 32f, y = 139f - lift * 8f,
            scaleX = 0.85f + beat * 0.12f, scaleY = 0.95f - beat * 0.04f)
        KantoMotion.SPAR -> pose.copy(x = 160f + beat * 13f, y = 145f - lift * 7f,
            rotation = beat * 9f, scaleX = 0.93f, scaleY = 0.93f)
        KantoMotion.STOMP -> pose.copy(x = 160f + travel * 12f, y = 145f - lift * 9f,
            rotation = beat * 4f, scaleY = 1f - lift * 0.025f)
        KantoMotion.SPIN -> pose.copy(x = 160f + travel * 12f, y = 138f + beat * 5f,
            rotation = time * 45f, scaleX = 0.78f, scaleY = 0.78f)
        KantoMotion.BREATHE -> pose.copy(x = 160f + travel * 3f, y = 147f - beat * 3f,
            scaleX = 1f + beat * 0.025f, scaleY = 1f + beat * 0.04f)
    }
}

internal fun DrawScope.drawKantoEffect(
    ndex: Int,
    effect: KantoEffect,
    pose: KantoScenePose,
    progress: Float
) {
    val blue = Color(0xFF85DEEF)
    val gold = Color(0xFFFFDC79)
    val green = Color(0xFF71BF79)
    val purple = Color(0xFFCAABF4)
    val beat = sin(progress * 19f + ndex)
    when (effect) {
        KantoEffect.WATER_JET -> {
            withTransform({
                translate(pose.x, pose.y)
                rotate(pose.rotation, pivot = Offset.Zero)
                scale(pose.scaleX, pose.scaleY, pivot = Offset.Zero)
            }) {
                kantoWaterJets(ndex, pose.articulation).forEach { jet ->
                    drawLine(blue, jet.origin, jet.origin + jet.reach * 0.15f, 3f, StrokeCap.Round)
                    repeat(9) { i ->
                        val t = (progress * 3f + i / 9f) % 1f
                        val point = jet.origin + jet.reach * t + Offset(0f, t * t * 6f)
                        drawCircle(blue.copy(alpha = 1f - t * 0.6f), 2f + t * 3f, point)
                    }
                }
            }
        }
        KantoEffect.RIPPLES -> repeat(3) { i ->
            val t = (progress * 2.3f + i / 3f) % 1f
            drawOval(blue.copy(alpha = (1f - t) * 0.8f),
                Offset(pose.x - 40f - t * 42f, 189f - t * 5f),
                Size(80f + t * 84f, 10f + t * 11f), style = Stroke(2f))
        }
        KantoEffect.PSYCHIC -> repeat(3) { i ->
            val t = (progress * 1.8f + i / 3f) % 1f
            drawOval(purple.copy(alpha = 0.7f * (1f - t)),
                Offset(pose.x - 58f - t * 35f, pose.y - 47f - t * 23f),
                Size(116f + t * 70f, 95f + t * 46f), style = Stroke(2f))
        }
        KantoEffect.VINES -> for (side in listOf(-1f, 1f)) {
            drawPath(Path().apply {
                moveTo(pose.x + side * 28f, pose.y + 18f)
                cubicTo(pose.x + side * 106f, pose.y + 21f,
                    pose.x + side * (91f + beat * 7f), pose.y - 78f,
                    pose.x + side * 66f, pose.y - 51f)
            }, green, style = Stroke(4f, cap = StrokeCap.Round))
        }
        KantoEffect.SILK -> repeat(3) { i ->
            val x = pose.x - 73f + i * 70f
            drawPath(Path().apply {
                moveTo(x, 48f)
                quadraticTo(x + beat * 8f, 70f, x + 8f, 94f + i * 4f)
            }, Color(0xFFEFF1D9).copy(alpha = 0.65f), style = Stroke(1.2f))
        }
        KantoEffect.SLASH -> repeat(3) { i ->
            drawArc(Color(0xFFE4F4C0).copy(alpha = 0.6f + beat * 0.2f),
                220f + beat * 20f, 80f, false,
                Offset(pose.x + 20f + i * 9f, pose.y - 47f + i * 7f),
                Size(75f, 55f), style = Stroke(3f, cap = StrokeCap.Round))
        }
        else -> repeat(8) { i ->
            val t = (progress * (1.6f + ndex % 5 * 0.13f) + i * 0.137f) % 1f
            val side = if (i % 2 == 0) -1f else 1f
            val x = pose.x + side * (58f + t * 45f)
            val y = pose.y + 28f - t * 94f + sin(i + t * 6f) * 8f
            val fade = sin(t * PI.toFloat()).coerceIn(0f, 1f)
            withTransform({
                translate(x, y)
                rotate(if (effect == KantoEffect.SLEEP || effect == KantoEffect.MUSIC) 0f
                    else side * t * 90f, pivot = Offset.Zero)
            }) {
                when (effect) {
                    KantoEffect.LEAVES -> {
                        drawOval(green.copy(alpha = fade), Offset(-6f, -3f), Size(12f, 6f))
                        drawLine(Color(0xFF367B55).copy(alpha = fade), Offset(-4f, 0f), Offset(4f, 0f), 1f)
                    }
                    KantoEffect.PETALS -> drawOval(Color(0xFFF398BD).copy(alpha = fade),
                        Offset(-4f, -3f), Size(8f, 6f))
                    KantoEffect.FIRE -> {
                        drawPath(Path().apply {
                            moveTo(-5f, 6f); quadraticTo(-9f, 0f, 1f, -13f)
                            quadraticTo(0f, -3f, 6f, 2f); quadraticTo(7f, 9f, -5f, 6f)
                        }, Color(0xFFFF9854).copy(alpha = fade))
                        drawCircle(gold.copy(alpha = fade), 2.5f, Offset(0f, 3f))
                    }
                    KantoEffect.BUBBLES -> {
                        drawCircle(blue.copy(alpha = fade), 3f + t * 4f, Offset.Zero, style = Stroke(1.5f))
                        drawCircle(Color.White.copy(alpha = fade), 1.2f, Offset(-2f, -2f))
                    }
                    KantoEffect.POLLEN, KantoEffect.SPORES -> {
                        val color = if (effect == KantoEffect.POLLEN) gold else purple
                        drawCircle(color.copy(alpha = fade * 0.3f), 7f, Offset.Zero)
                        drawCircle(color.copy(alpha = fade), 2.5f, Offset.Zero)
                    }
                    KantoEffect.WIND -> drawArc(Color.White.copy(alpha = fade * 0.8f),
                        200f, 125f, false, Offset(-17f, -4f), Size(34f, 10f), style = Stroke(2f))
                    KantoEffect.DUST, KantoEffect.ROCKS -> {
                        val color = Color(0xFFBC9469).copy(alpha = fade)
                        if (effect == KantoEffect.DUST) drawCircle(color, 3f + t * 3f,
                            Offset(0f, t * 87f))
                        else drawPath(Path().apply {
                            moveTo(-5f, 2f); lineTo(-2f, -5f); lineTo(5f, -3f)
                            lineTo(6f, 4f); lineTo(-3f, 5f); close()
                        }, color)
                    }
                    KantoEffect.ELECTRIC -> drawPath(Path().apply {
                        moveTo(2f, -12f); lineTo(-5f, 0f); lineTo(4f, -1f); lineTo(-2f, 12f)
                    }, gold.copy(alpha = fade), style = Stroke(3f, cap = StrokeCap.Round))
                    KantoEffect.STARS, KantoEffect.IMPACT -> {
                        val radius = if (effect == KantoEffect.IMPACT) 11f else 6f
                        drawPath(Path().apply {
                            moveTo(0f, -radius); lineTo(2f, -2f); lineTo(radius, 0f)
                            lineTo(2f, 2f); lineTo(0f, radius); lineTo(-2f, 2f)
                            lineTo(-radius, 0f); lineTo(-2f, -2f); close()
                        }, gold.copy(alpha = fade))
                    }
                    KantoEffect.COINS -> {
                        drawOval(gold.copy(alpha = fade), Offset(-4f, -6f), Size(8f, 12f))
                        drawLine(Color(0xFFBA863C).copy(alpha = fade), Offset(0f, -3f), Offset(0f, 3f), 1.5f)
                    }
                    KantoEffect.ICE -> repeat(3) { spoke ->
                        val angle = spoke * PI.toFloat() / 3f
                        val end = Offset(cos(angle) * 6f, sin(angle) * 6f)
                        drawLine(Color(0xFFDCF9FF).copy(alpha = fade), -end, end, 2f)
                    }
                    KantoEffect.SLUDGE, KantoEffect.MIST -> {
                        val color = if (effect == KantoEffect.SLUDGE) Color(0xFFB68FCA) else purple
                        drawCircle(color.copy(alpha = fade * 0.35f), 9f + t * 5f, Offset.Zero)
                        drawCircle(color.copy(alpha = fade * 0.3f), 7f, Offset(7f, -3f))
                    }
                    KantoEffect.HEARTS -> drawPath(Path().apply {
                        moveTo(0f, 6f); cubicTo(-15f, -2f, -5f, -11f, 0f, -4f)
                        cubicTo(5f, -11f, 15f, -2f, 0f, 6f)
                    }, Color(0xFFF59EBA).copy(alpha = fade))
                    KantoEffect.MUSIC -> {
                        drawCircle(purple.copy(alpha = fade), 3f, Offset(-2f, 5f))
                        drawLine(purple.copy(alpha = fade), Offset(1f, 5f), Offset(1f, -8f), 2f)
                        drawLine(purple.copy(alpha = fade), Offset(1f, -8f), Offset(7f, -5f), 2f)
                    }
                    KantoEffect.PIXELS -> drawRect(blue.copy(alpha = fade),
                        Offset(-4f, -4f), Size(7f, 7f), style = Stroke(1.5f))
                    KantoEffect.SLEEP -> drawPath(Path().apply {
                        moveTo(-4f, -5f); lineTo(4f, -5f); lineTo(-4f, 5f); lineTo(4f, 5f)
                    }, Color(0xFFF5E8B4).copy(alpha = fade), style = Stroke(2f))
                }
            }
        }
    }
}
