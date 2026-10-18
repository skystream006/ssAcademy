package com.ssmath.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

internal fun DrawScope.drawMegaLate(celebration: Celebration, motion: Float) {
    val m = motion.coerceIn(-1f, 1f)
    when (celebration) {
        Celebration.MEGA_SKARMORY -> megaLateSkarmory(m)
        Celebration.MEGA_DELPHOX -> megaLateDelphox(m)
        Celebration.MEGA_SALAMENCE -> megaLateSalamence(m)
        Celebration.MEGA_KANGASKHAN -> megaLateKangaskhan(m)
        Celebration.MEGA_DRAMPA -> megaLateDrampa(m)
        Celebration.MEGA_METAGROSS -> megaLateMetagross(m)
        Celebration.MEGA_ZYGARDE -> megaLateZygarde(m)
        Celebration.MEGA_DIANCIE -> megaLateDiancie(m)
        Celebration.MEGA_MEWTWO_X -> megaLateMewtwoX(m)
        Celebration.MEGA_MEWTWO_Y -> megaLateMewtwoY(m)
        Celebration.MEGA_SCOVILLAIN -> megaLateScovillain(m)
        Celebration.MEGA_GLIMMORA -> megaLateGlimmora(m)
        Celebration.MEGA_TATSUGIRI -> megaLateTatsugiriCurly(m)
        Celebration.MEGA_TATSUGIRI_C -> megaLateTatsugiriDroopy(m)
        Celebration.MEGA_TATSUGIRI_D -> megaLateTatsugiriStretchy(m)
        Celebration.MEGA_BAXCALIBUR -> megaLateBaxcalibur(m)
        Celebration.MEGA_SCEPTILE -> megaLateSceptile(m)
        Celebration.MEGA_BLAZIKEN -> megaLateBlaziken(m)
        Celebration.MEGA_SWAMPERT -> megaLateSwampert(m)
        Celebration.MEGA_CHIMECHO -> megaLateChimecho(m)
        Celebration.MEGA_GOLISOPOD -> megaLateGolisopod(m)
        Celebration.MEGA_GOLURK -> megaLateGolurk(m)
        Celebration.MEGA_STARAPTOR -> megaLateStaraptor(m)
        Celebration.MEGA_CRABOMINABLE -> megaLateCrabominable(m)
        Celebration.MEGA_HEATRAN -> megaLateHeatran(m)
        Celebration.MEGA_DARKRAI -> megaLateDarkrai(m)
        Celebration.MEGA_LATIAS -> megaLateLatias(m)
        Celebration.MEGA_LATIOS -> megaLateLatios(m)
        Celebration.MEGA_RAYQUAZA -> megaLateRayquaza(m)
        Celebration.MEGA_MAGEARNA -> megaLateMagearna(m)
        Celebration.MEGA_ZERAORA -> megaLateZeraora(m)
        else -> error("No late Mega artwork for $celebration")
    }
}

private fun DrawScope.megaLateForm(color: Long, geometry: Path.() -> Unit) {
    val shape = Path().apply(geometry).apply { close() }
    drawPath(shape, Color(color))
    drawPath(shape, Color(KANTO_INK), style = Stroke(2f, join = StrokeJoin.Round))
}

private fun DrawScope.megaLateCurve(color: Long, width: Float, geometry: Path.() -> Unit) {
    drawPath(
        Path().apply(geometry), Color(color),
        style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.megaLateSalamence(m: Float) {
    val wing = m * 7f
    megaLateForm(0xFFBA304D) {
        moveTo(-15f, 0f)
        cubicTo(-47f, -4f, -87f, -20f + wing, -98f, -63f + wing)
        cubicTo(-58f, -36f + wing, 43f, -40f - wing, 98f, -68f - wing)
        cubicTo(87f, -19f - wing, 54f, 2f, 18f, 4f)
        quadraticTo(2f, -21f, -15f, 0f)
    }
    megaLateCurve(0xFFFA8587, 3f) {
        moveTo(-86f, -51f + wing)
        quadraticTo(0f, -13f, 87f, -55f - wing)
    }
    megaLateForm(0xFF4597BC) {
        moveTo(24f, 9f)
        cubicTo(63f, 8f, 79f, 32f, 66f + m * 4f, 46f)
        cubicTo(81f, 40f, 90f, 14f, 35f, -5f)
    }
    kantoPolygon(0xFFBD3F51, 71f, 25f, 86f, 33f + m * 3f, 72f, 40f)
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF387998, s * 15f, 22f, s * 31f, 34f + m * s * 3f,
            s * 40f, 48f + m * s * 3f, s * 18f, 45f, s * 7f, 32f)
        kantoLine(0xFFEBE9DC, 4f, s * 23f, 44f, s * 32f, 45f)
    }
    megaLateForm(0xFF6EBAD2) {
        moveTo(-30f, -8f)
        cubicTo(-39f, 19f, -16f, 38f, 17f, 34f)
        cubicTo(45f, 30f, 40f, 2f, 24f, -15f)
        quadraticTo(-2f, -26f, -30f, -8f)
    }
    kantoPolygon(0xFFE9E6DE, -32f, -4f, -12f, -14f, 18f, -10f,
        36f, 4f, 22f, 25f, -6f, 29f, -26f, 18f)
    kantoLine(0xFFAAAAB5, 2f, -22f, 3f, 21f, 8f, 28f, 3f)
    kantoLine(0xFFAAAAB5, 2f, -19f, 13f, 14f, 19f, 26f, 14f)
    kantoPolygon(0xFF357EAB, -18f, -24f, -38f, -44f, -30f, -17f, -10f, -3f)
    kantoPolygon(0xFF357EAB, 8f, -26f, 32f, -45f, 26f, -15f, 9f, -2f)
    megaLateForm(0xFF64B6D4) {
        moveTo(-20f, -34f)
        quadraticTo(4f, -44f, 18f, -22f)
        lineTo(17f, -2f)
        quadraticTo(-5f, 11f, -27f, -7f)
        lineTo(-31f, -19f)
    }
    kantoPolygon(0xFFE7EEEF, -23f, -22f, -7f, -18f, -18f, -13f)
    kantoPolygon(0xFFE7EEEF, 1f, -20f, 14f, -25f, 11f, -14f)
    kantoEyes(-5f, -18f, 13f, 2.8f)
    kantoLine(KANTO_INK, 2f, -19f, -4f, -7f, 0f, 6f, -3f)
}

private fun DrawScope.megaLateKangaskhan(m: Float) {
    megaLateForm(0xFFA57B66) {
        moveTo(-24f, 22f)
        cubicTo(-54f, 15f, -75f, 32f, -90f, 44f + m * 5f)
        quadraticTo(-49f, 53f, -13f, 39f)
    }
    kantoPolygon(0xFFD9C5AA, -88f, 44f + m * 5f, -63f, 31f, -52f, 44f)
    for (s in listOf(-1f, 1f)) {
        kantoOval(0xFF8D695B, -18f + s * 21f, 26f, 29f, 32f)
        kantoPolygon(0xFFE5D8C1, s * 21f - 13f, 55f, s * 21f - 3f, 51f,
            s * 21f + 11f, 56f, s * 21f + 12f, 62f, s * 21f - 15f, 62f)
    }
    megaLateForm(0xFFBA9579) {
        moveTo(-31f, -22f)
        cubicTo(-48f, 5f, -34f, 42f, 0f, 48f)
        cubicTo(36f, 45f, 38f, 3f, 20f, -24f)
        quadraticTo(-5f, -37f, -31f, -22f)
    }
    kantoOval(0xFFEAD3AE, -24f, -4f, 47f, 48f)
    megaLateCurve(0xFF8E6957, 3f) {
        moveTo(-23f, 23f)
        quadraticTo(0f, 38f, 23f, 23f)
    }
    withTransform({ rotate(m * 9f, Offset(-25f, -11f)) }) {
        kantoPolygon(0xFFA78069, -28f, -18f, -48f, -10f, -58f, 10f,
            -44f, 20f, -34f, 4f, -18f, -3f)
        for (i in 0..2) kantoPolygon(0xFFF2E5CA, -53f + i * 5f, 10f,
            -55f + i * 5f, 20f, -48f + i * 5f, 15f)
    }
    kantoPolygon(0xFFB58F73, 17f, -16f, 38f, -4f - m * 5f,
        44f, 16f - m * 5f, 29f, 20f, 15f, 2f)
    kantoPolygon(0xFFA48670, -28f, -37f, -42f, -54f, -21f, -54f, -12f, -35f)
    kantoPolygon(0xFFA48670, 11f, -41f, 22f, -58f, 34f, -43f, 17f, -30f)
    kantoPolygon(0xFF403F4B, -30f, -44f, -34f, -51f, -23f, -48f)
    kantoPolygon(0xFF403F4B, 20f, -43f, 24f, -50f, 28f, -43f)
    kantoOval(0xFFC2A78C, -30f, -49f, 55f, 43f)
    kantoPolygon(0xFF9A8E7F, -32f, -40f, -17f, -56f, 8f, -55f, 27f, -39f,
        12f, -31f, -5f, -38f, -19f, -29f)
    kantoEyes(-4f, -28f, 13f, 3.4f)
    kantoOval(0xFFE7CCAB, -23f, -22f, 40f, 20f)
    kantoSmile(-4f, -12f, 16f)
    val hop = m * 4f
    kantoLine(0xFFB598B5, 12f, 63f, 28f + hop, 83f, 38f)
    kantoOval(0xFFBDA4C1, 39f, 8f + hop, 31f, 38f)
    kantoOval(0xFFF0DFE1, 46f, 17f + hop, 17f, 22f)
    kantoLine(0xFFBDA4C1, 9f, 44f, 18f + hop, 32f, 9f - hop)
    kantoLine(0xFFBDA4C1, 9f, 66f, 19f + hop, 77f, 5f + hop)
    kantoPolygon(0xFFAA8EAB, 41f, 0f + hop, 35f, -15f + hop, 49f, -8f + hop)
    kantoPolygon(0xFFAA8EAB, 60f, -6f + hop, 72f, -15f + hop, 68f, 6f + hop)
    kantoOval(0xFFC5AEC8, 38f, -11f + hop, 33f, 27f)
    kantoEyes(54f, 0f + hop, 8f, 2.7f)
    kantoSmile(54f, 9f + hop, 10f)
    kantoLine(0xFFAA8EAB, 9f, 45f, 41f + hop, 37f, 49f + hop)
    kantoLine(0xFFAA8EAB, 9f, 63f, 41f + hop, 70f, 49f + hop)
}

private fun DrawScope.megaLateMetagross(m: Float) {
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF477B9C, s * 24f, -19f, s * 52f, -44f + m * 4f,
            s * 78f, -49f + m * 4f, s * 64f, -22f, s * 34f, -2f)
        kantoPolygon(0xFF81B9CB, s * 59f, -40f + m * 4f, s * 77f, -63f + m * 4f,
            s * 75f, -38f + m * 4f, s * 89f, -52f + m * 4f,
            s * 81f, -25f + m * 4f, s * 63f, -21f)
        kantoPolygon(0xFF477B9C, s * 25f, 12f, s * 55f, 14f - m * 5f,
            s * 81f, 34f - m * 5f, s * 57f, 42f, s * 22f, 28f)
        kantoPolygon(0xFFB6D9E1, s * 64f, 28f - m * 5f, s * 91f, 22f - m * 5f,
            s * 80f, 37f - m * 5f, s * 94f, 41f - m * 5f,
            s * 68f, 49f - m * 5f, s * 53f, 37f)
    }
    megaLateForm(0xFF5698B6) {
        moveTo(-45f, -25f)
        quadraticTo(0f, -49f, 45f, -25f)
        lineTo(49f, 15f)
        quadraticTo(0f, 39f, -49f, 15f)
    }
    kantoPolygon(0xFF87C5D5, -37f, -22f, 0f, -34f, 37f, -22f, 24f, -8f, -25f, -8f)
    kantoPolygon(0xFF346D91, -44f, 8f, -25f, 24f, 26f, 24f, 44f, 8f,
        35f, 26f, 0f, 36f, -35f, 26f)
    kantoPolygon(0xFFFFEDB9, -36f, -26f, -25f, -30f, 36f, 18f,
        31f, 28f, 20f, 25f, -39f, -18f)
    kantoPolygon(0xFFFFD875, 28f, -31f, 38f, -25f, -29f, 28f,
        -39f, 19f, -32f, 9f)
    kantoPolygon(0xFFFFFFFF, -34f, -7f, -13f, -1f, -30f, 5f)
    kantoPolygon(0xFFFFFFFF, 34f, -7f, 13f, -1f, 30f, 5f)
    kantoOval(0xFFCF394C, -29f, -4f, 6f, 7f)
    kantoOval(0xFFCF394C, 23f, -4f, 6f, 7f)
    for (s in listOf(-1f, 1f)) {
        val lift = s * m * 5f
        kantoPolygon(0xFF7BAEC5, s * 17f, 17f, s * 38f, 17f,
            s * 52f, 36f + lift, s * 38f, 50f + lift, s * 15f, 37f)
        kantoPolygon(0xFF427898, s * 22f, 33f, s * 49f, 37f + lift,
            s * 51f, 52f + lift, s * 27f, 55f + lift, s * 13f, 44f)
        for (i in 0..2) {
            val x = s * (24f + i * 9f)
            kantoPolygon(0xFFEEEDE4, x, 45f + lift, x + s * 6f, 47f + lift,
                x + s * 3f, 60f + lift)
        }
    }
}

private fun DrawScope.megaLateDiancie(m: Float) {
    for (s in listOf(-1f, 1f)) {
        megaLateForm(0xFFF8F1F1) {
            moveTo(s * 12f, -4f)
            cubicTo(s * 48f, -16f, s * 51f, 34f + m * s * 5f, s * 88f, 43f + m * s * 5f)
            quadraticTo(s * 48f, 51f + m * s * 5f, s * 31f, 14f)
            lineTo(s * 8f, 5f)
        }
        kantoPolygon(0xFFBC8BBB, s * 12f, 19f, s * 43f, 31f,
            s * 59f, 53f, s * 25f, 57f, s * 5f, 41f)
        kantoPolygon(0xFFF09CCA, s * 8f, 23f, s * 35f, 34f,
            s * 25f, 57f, s * 4f, 46f)
    }
    kantoPolygon(0xFFF8D5E7, -31f, 28f, 0f, 10f, 31f, 28f, 24f, 52f, 0f, 63f, -24f, 52f)
    kantoPolygon(0xFFDA70AC, -31f, 28f, -7f, 35f, 0f, 63f, -24f, 52f)
    kantoPolygon(0xFFFFFFFF, -7f, 35f, 0f, 10f, 13f, 34f, 0f, 63f)
    kantoPolygon(0xFFEAA2C7, 13f, 34f, 31f, 28f, 24f, 52f, 0f, 63f)
    kantoPolygon(0xFF60566E, -13f, -14f, 13f, -14f, 17f, 13f, 0f, 26f, -17f, 13f)
    kantoPolygon(0xFFDDD5E4, -11f, -11f, 0f, -4f, 11f, -11f, 10f, 10f, 0f, 17f, -10f, 10f)
    for (s in listOf(-1f, 1f)) {
        kantoLine(0xFF756D83, 6f, s * 12f, -6f, s * 26f, 9f - m * s * 4f,
            s * 37f, -1f - m * s * 5f)
        kantoOval(0xFFD9D0DE, s * 37f - 5f, -7f - m * s * 5f, 10f, 10f)
    }
    kantoOval(0xFF5E576D, -19f, -42f, 38f, 32f)
    kantoPolygon(0xFFEDE4EC, -27f, -32f, -18f, -47f, 0f, -49f, 18f, -47f,
        27f, -32f, 17f, -22f, 14f, -35f, 0f, -30f, -14f, -35f, -17f, -22f)
    kantoEyes(0f, -26f, 9f, 3.7f)
    kantoSmile(0f, -16f, 8f)
    kantoPolygon(0xFFD779B4, -29f, -46f, -19f, -63f, -8f, -45f, -1f, -69f,
        10f, -46f, 22f, -62f, 29f, -45f, 0f, -35f)
    kantoPolygon(0xFFFFC9E3, -12f, -58f, 0f, -75f, 14f, -58f, 0f, -39f)
    kantoPolygon(0xFFFFFFFF, -12f, -58f, 0f, -75f, 0f, -39f)
    kantoPolygon(0xFFF7BADC, -28f, -45f, -39f, -32f, -23f, -23f, -20f, -40f)
    kantoPolygon(0xFFF7BADC, 28f, -45f, 39f, -32f, 23f, -23f, 20f, -40f)
    kantoLine(0xFFFFEDF7, 2f, 24f, 35f, 21f, 44f)
}

private fun DrawScope.megaLateMewtwoX(m: Float) {
    megaLateCurve(KANTO_INK, 15f) {
        moveTo(12f, 28f)
        cubicTo(59f, 62f, 92f, 28f + m * 5f, 68f, -4f + m * 5f)
    }
    megaLateCurve(0xFF996BC3, 11f) {
        moveTo(12f, 28f)
        cubicTo(59f, 62f, 92f, 28f + m * 5f, 68f, -4f + m * 5f)
    }
    for (s in listOf(-1f, 1f)) {
        val flex = s * m * 5f
        megaLateForm(0xFFB798D5) {
            moveTo(s * 8f, 17f)
            quadraticTo(s * 38f, 13f, s * 35f, 34f)
            lineTo(s * 24f, 46f + flex)
            lineTo(s * 35f, 59f + flex)
            lineTo(s * 12f, 60f + flex)
            lineTo(s * 8f, 45f)
            lineTo(s * 15f, 32f)
        }
        kantoLine(0xFFF1EAF5, 3f, s * 18f, 53f + flex, s * 26f, 56f + flex)
        kantoOval(0xFFE4DCEB, s * 32f - 16f, -30f, 32f, 29f)
        withTransform({ rotate(s * (m * 11f), Offset(s * 33f, -13f)) }) {
            megaLateForm(0xFFD6C8E3) {
                moveTo(s * 32f, -16f)
                quadraticTo(s * 49f, -19f, s * 52f, 0f)
                lineTo(s * 62f, 15f)
                quadraticTo(s * 52f, 27f, s * 42f, 15f)
                lineTo(s * 33f, 1f)
            }
            kantoOval(0xFFF0EAF4, s * 53f - 10f, 10f, 20f, 15f)
            kantoLine(0xFF8E7AAB, 2f, s * 48f, 18f, s * 57f, 18f)
        }
    }
    megaLateForm(0xFFE4DDEB) {
        moveTo(-25f, -24f)
        quadraticTo(0f, -39f, 25f, -24f)
        lineTo(19f, -2f)
        quadraticTo(12f, 12f, 16f, 29f)
        quadraticTo(0f, 38f, -16f, 29f)
        lineTo(-14f, 9f)
        lineTo(-22f, -2f)
    }
    kantoPolygon(0xFFB99ACC, -17f, 8f, 0f, 14f, 17f, 8f, 16f, 29f, 0f, 36f, -16f, 29f)
    kantoLine(0xFFBAACCA, 2f, -19f, -15f, -4f, -9f, 0f, -12f, 4f, -9f, 19f, -15f)
    kantoPolygon(0xFFE3DCEB, -19f, -45f, -22f, -66f, -9f, -58f, -6f, -41f)
    kantoPolygon(0xFFE3DCEB, 9f, -46f, 17f, -66f, 24f, -52f, 18f, -34f)
    kantoOval(0xFFEAE3EF, -20f, -58f, 42f, 33f)
    kantoPolygon(0xFFAA8CBC, -16f, -47f, -3f, -42f, -13f, -37f)
    kantoPolygon(0xFFAA8CBC, 16f, -47f, 3f, -42f, 13f, -37f)
    kantoEyes(0f, -42f, 11f, 2.8f)
    kantoLine(KANTO_INK, 2f, -5f, -31f, 6f, -31f)
}

private fun DrawScope.megaLateMewtwoY(m: Float) {
    megaLateForm(0xFFB98DCF) {
        moveTo(6f, -51f)
        cubicTo(72f, -71f, 98f, -28f + m * 7f, 72f, 12f + m * 6f)
        cubicTo(60f, 29f, 47f, 29f, 36f, 20f)
        cubicTo(67f, 31f, 88f, -24f + m * 7f, 43f, -38f)
        lineTo(10f, -35f)
    }
    megaLateCurve(0xFFDDC2E8, 4f) {
        moveTo(27f, -49f)
        cubicTo(80f, -49f, 90f, -13f + m * 5f, 59f, 18f)
    }
    megaLateForm(0xFFEAE1F1) {
        moveTo(-11f, -20f)
        quadraticTo(10f, -27f, 16f, -8f)
        lineTo(12f, 16f)
        quadraticTo(-2f, 30f, -15f, 12f)
        quadraticTo(-21f, -4f, -11f, -20f)
    }
    kantoPolygon(0xFFC6AAD9, -13f, 2f, 12f, 2f, 12f, 18f, 0f, 26f, -13f, 15f)
    for (s in listOf(-1f, 1f)) {
        megaLateCurve(KANTO_INK, 7f) {
            moveTo(s * 11f, -12f)
            quadraticTo(s * 21f, 0f, s * 34f, -2f - m * s * 7f)
        }
        megaLateCurve(0xFFE2D4EB, 4f) {
            moveTo(s * 11f, -12f)
            quadraticTo(s * 21f, 0f, s * 34f, -2f - m * s * 7f)
        }
        for (i in 0..2) kantoLine(0xFFD0B8E1, 3f, s * 33f, -2f - m * s * 7f,
            s * (39f + i * 3f), -8f + i * 5f - m * s * 7f)
        megaLateForm(0xFFC1A2D5) {
            moveTo(s * 7f, 18f)
            quadraticTo(s * 18f, 23f, s * 15f, 38f + m * s * 4f)
            lineTo(s * 29f, 48f + m * s * 4f)
            quadraticTo(s * 16f, 55f + m * s * 4f, s * 8f, 45f)
            lineTo(s * 3f, 29f)
        }
    }
    kantoPolygon(0xFFE9DFF0, -18f, -47f, -20f, -68f, -8f, -58f, -5f, -40f)
    kantoPolygon(0xFFE9DFF0, 9f, -48f, 19f, -65f, 23f, -50f, 16f, -35f)
    megaLateForm(0xFFF1E9F6) {
        moveTo(-18f, -52f)
        cubicTo(-36f, -37f, -23f, -19f, -4f, -19f)
        cubicTo(23f, -17f, 29f, -42f, 15f, -53f)
        quadraticTo(1f, -63f, -18f, -52f)
    }
    kantoPolygon(0xFFB994D1, -17f, -49f, -7f, -57f, 11f, -56f,
        17f, -49f, 8f, -43f, -4f, -46f)
    kantoPolygon(0xFFFFFFFF, -21f, -38f, -6f, -34f, -17f, -29f)
    kantoPolygon(0xFFFFFFFF, 17f, -39f, 4f, -33f, 13f, -29f)
    kantoEyes(-2f, -34f, 13f, 3f)
    kantoSmile(-2f, -24f, 7f)
}

private fun DrawScope.megaLateSceptile(m: Float) {
    withTransform({ rotate(m * 8f, Offset(22f, 22f)) }) {
        kantoPolygon(0xFF225E4B, 14f, 32f, 34f, 12f, 40f, -10f, 51f, 3f,
            57f, -30f, 67f, -12f, 79f, -57f, 81f, -21f, 96f, -20f,
            79f, 1f, 89f, 11f, 66f, 22f, 72f, 37f, 44f, 42f)
        kantoLine(0xFFB34452, 6f, 26f, 33f, 49f, 13f, 64f, -11f, 79f, -53f)
        kantoLine(0xFF67B780, 3f, 42f, 21f, 66f, 22f)
        kantoLine(0xFF67B780, 3f, 56f, 2f, 80f, 0f)
    }
    kantoPolygon(0xFF47955C, -10f, 15f, -30f, 35f + m * 5f, -41f, 54f + m * 5f,
        -16f, 57f, -15f, 43f, 3f, 32f)
    kantoPolygon(0xFF47955C, 10f, 19f, 30f, 39f - m * 4f, 25f, 54f,
        6f, 55f, 14f, 41f, -1f, 31f)
    for (s in listOf(-1f, 1f)) kantoLine(0xFFF0E9C8, 3f, s * 15f, 55f,
        s * 29f, 57f)
    megaLateForm(0xFF5FAE67) {
        moveTo(-11f, -22f)
        quadraticTo(13f, -20f, 18f, 9f)
        lineTo(21f, 31f)
        quadraticTo(0f, 45f, -18f, 28f)
        lineTo(-19f, 0f)
    }
    kantoPolygon(0xFFDB6B66, -8f, -10f, 1f, -8f, 7f, 29f, -8f, 28f)
    for (i in 0..2) kantoOval(0xFFF3D96C, 8f, -5f + i * 11f, 8f, 8f)
    withTransform({ rotate(m * 12f, Offset(-15f, -7f)) }) {
        kantoLine(0xFF559A62, 11f, -14f, -6f, -34f, 8f, -56f, 2f)
        kantoPolygon(0xFF2A784F, -32f, 5f, -48f, -17f, -49f, -1f,
            -68f, -13f, -58f, 9f, -38f, 14f)
        kantoLine(0xFFDA6E63, 2f, -41f, 7f, -58f, -3f)
    }
    kantoLine(0xFF559A62, 10f, 15f, -5f, 35f, -1f - m * 5f, 44f, -16f - m * 5f)
    kantoPolygon(0xFF2A784F, 30f, 0f, 44f, -25f - m * 5f, 42f, -8f,
        56f, -19f, 48f, 5f, 33f, 9f)
    kantoPolygon(0xFF5CAF69, -13f, -32f, -20f, -62f, -5f, -49f,
        5f, -68f, 13f, -42f, 26f, -35f, 10f, -18f, -21f, -18f, -32f, -28f)
    kantoPolygon(0xFFB84051, -20f, -31f, -3f, -37f, 17f, -32f, 3f, -23f, -14f, -24f)
    kantoPolygon(0xFFFFED95, -20f, -32f, -4f, -31f, -15f, -26f)
    kantoOval(KANTO_INK, -15f, -31f, 4f, 5f)
    kantoLine(KANTO_INK, 2f, -24f, -21f, -5f, -19f, 8f, -24f)
}

private fun DrawScope.megaLateBlaziken(m: Float) {
    for (s in listOf(-1f, 1f)) {
        megaLateForm(0xFFDAD6D3) {
            moveTo(s * 9f, -39f)
            cubicTo(s * 24f, -61f, s * 45f, -49f, s * 52f, -29f)
            lineTo(s * 39f, -34f)
            lineTo(s * 46f, -18f)
            lineTo(s * 25f, -23f)
            lineTo(s * 15f, -12f)
        }
        val step = s * m * 6f
        kantoPolygon(0xFFCE4941, s * 5f, 9f, s * 24f, 9f, s * 27f, 35f,
            s * 38f, 56f + step, s * 20f, 58f + step, s * 13f, 39f, s * 5f, 30f)
        kantoPolygon(0xFF43424A, s * 13f, 35f, s * 27f, 31f,
            s * 33f, 45f + step, s * 16f, 44f + step)
        kantoPolygon(0xFFDEB48C, s * 21f, 54f + step, s * 35f, 54f + step,
            s * 43f, 62f + step * 0.3f, s * 17f, 62f + step * 0.3f)
    }
    megaLateForm(0xFFCC4843) {
        moveTo(-17f, -29f)
        lineTo(17f, -29f)
        quadraticTo(25f, -15f, 15f, 9f)
        lineTo(23f, 21f)
        lineTo(6f, 17f)
        lineTo(0f, 26f)
        lineTo(-7f, 17f)
        lineTo(-24f, 21f)
        quadraticTo(-12f, 2f, -17f, -29f)
    }
    kantoPolygon(0xFF383942, -17f, -21f, 0f, -7f, 17f, -21f,
        14f, -3f, 0f, 10f, -14f, -3f)
    for (s in listOf(-1f, 1f)) {
        val handY = -1f - s * m * 10f
        kantoLine(0xFFC74A43, 12f, s * 17f, -21f, s * 35f, -10f, s * 50f, handY)
        kantoOval(0xFF41424B, s * 49f - 10f, handY - 9f, 20f, 18f)
        kantoOval(0xFFE7BE93, s * 57f - 8f, handY - 5f, 15f, 12f)
        megaLateForm(0xFFFF7C43) {
            moveTo(s * 43f, handY - 5f)
            cubicTo(s * 58f, handY - 23f, s * 84f, handY - 8f, s * 97f, handY - 26f)
            lineTo(s * 88f, handY - 7f)
            lineTo(s * 97f, handY - 9f)
            quadraticTo(s * 70f, handY + 9f, s * 49f, handY + 5f)
        }
        megaLateCurve(0xFFFFD781, 4f) {
            moveTo(s * 50f, handY)
            quadraticTo(s * 72f, handY - 9f, s * 88f, handY - 12f)
        }
    }
    kantoPolygon(0xFFDBD7D5, -13f, -38f, -28f, -70f, -7f, -58f,
        0f, -43f, 9f, -60f, 29f, -70f, 14f, -36f)
    kantoOval(0xFFD95446, -17f, -48f, 35f, 31f)
    kantoPolygon(0xFFEBE1D7, -18f, -41f, -3f, -36f, 0f, -26f,
        5f, -36f, 19f, -41f, 12f, -20f, 0f, -15f, -12f, -21f)
    kantoEyes(0f, -35f, 10f, 3f)
    kantoPolygon(0xFFDFC19A, -6f, -27f, 6f, -27f, 0f, -19f)
}

private fun DrawScope.megaLateSwampert(m: Float) {
    kantoPolygon(0xFF304D64, -16f, 16f, -44f, -5f + m * 4f, -50f, -31f + m * 4f,
        -30f, -23f, -24f, -51f, -6f, -25f, 12f, 13f)
    kantoPolygon(0xFF466C82, -27f, -15f, -25f, -35f, -10f, -12f, 1f, 20f)
    kantoOval(0xFF518FB4, -37f, 29f, 28f, 30f)
    kantoOval(0xFF518FB4, 10f, 29f, 28f, 30f)
    kantoOval(0xFF334F68, -43f, 49f, 36f, 14f)
    kantoOval(0xFF334F68, 7f, 49f, 36f, 14f)
    megaLateForm(0xFF559BBC) {
        moveTo(-30f, -24f)
        quadraticTo(0f, -41f, 30f, -24f)
        cubicTo(48f, 1f, 37f, 34f, 17f, 41f)
        quadraticTo(0f, 46f, -17f, 41f)
        cubicTo(-37f, 34f, -48f, 1f, -30f, -24f)
    }
    kantoOval(0xFFAACDDA, -24f, -6f, 48f, 44f)
    for (s in listOf(-1f, 1f)) {
        val lift = m * s * 5f
        megaLateForm(0xFF528EAE) {
            moveTo(s * 26f, -25f)
            cubicTo(s * 57f, -44f, s * 78f, -18f + lift, s * 68f, 8f + lift)
            lineTo(s * 74f, 31f + lift)
            quadraticTo(s * 57f, 51f + lift, s * 38f, 28f + lift)
            lineTo(s * 36f, 2f)
        }
        kantoOval(0xFF6BA7C3, s * 50f - 16f, -24f + lift, 32f, 29f)
        kantoPolygon(0xFFF29457, s * 41f, -12f + lift, s * 55f, -21f + lift,
            s * 62f, -11f + lift, s * 56f, 3f + lift, s * 43f, 5f + lift)
        kantoOval(0xFF405A72, s * 56f - 18f, 19f + lift, 36f, 27f)
        for (i in 0..2) kantoLine(0xFFADCBD6, 2f, s * (45f + i * 9f), 31f + lift,
            s * (45f + i * 9f), 40f + lift)
    }
    kantoPolygon(0xFF334B63, -19f, -37f, -28f, -72f, -8f, -59f,
        0f, -40f, 12f, -64f, 27f, -73f, 19f, -32f)
    kantoOval(0xFF67A9C5, -29f, -50f, 58f, 39f)
    for (s in listOf(-1f, 1f)) kantoPolygon(0xFFF18A53, s * 19f, -28f,
        s * 43f, -43f, s * 37f, -29f, s * 48f, -23f, s * 26f, -15f)
    kantoPolygon(0xFFB9D7DF, -24f, -25f, 0f, -19f, 24f, -25f,
        18f, -10f, 0f, -6f, -18f, -10f)
    kantoEyes(0f, -33f, 15f, 3.6f)
    kantoLine(KANTO_INK, 2f, -13f, -17f, 0f, -13f, 13f, -17f)
}

private fun DrawScope.megaLateLatias(m: Float) {
    val flap = m * 8f
    megaLateForm(0xFFB49CCF) {
        moveTo(-21f, 1f)
        quadraticTo(-60f, -24f, -86f, -57f + flap)
        lineTo(-68f, -10f + flap)
        lineTo(-88f, 12f + flap)
        quadraticTo(-46f, 20f, -20f, 18f)
    }
    megaLateForm(0xFFB49CCF) {
        moveTo(21f, 1f)
        quadraticTo(60f, -24f, 86f, -57f - flap)
        lineTo(68f, -10f - flap)
        lineTo(88f, 12f - flap)
        quadraticTo(46f, 20f, 20f, 18f)
    }
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFFD7C7E7, s * 27f, 5f, s * 73f, -37f + s * flap,
            s * 59f, -4f + s * flap, s * 72f, 8f + s * flap)
        kantoPolygon(0xFF9A7EBB, s * 8f, 26f, s * 42f, 49f,
            s * 36f, 60f, s * 7f, 45f)
    }
    megaLateForm(0xFFBFA9D6) {
        moveTo(-16f, -23f)
        quadraticTo(-37f, -6f, -23f, 25f)
        lineTo(0f, 51f)
        lineTo(23f, 25f)
        quadraticTo(37f, -6f, 16f, -23f)
    }
    kantoPolygon(0xFFF4EFF7, -17f, -20f, 17f, -20f, 20f, 9f,
        0f, 28f, -20f, 9f)
    kantoPolygon(0xFFE189A0, -11f, 6f, 11f, 6f, 0f, 18f)
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFFBAA3D1, s * 14f, 4f, s * 32f, 15f - s * m * 4f,
            s * 28f, 29f - s * m * 4f, s * 14f, 19f)
        kantoLine(0xFFF0EBF5, 2f, s * 21f, 19f, s * 27f, 22f - s * m * 4f)
    }
    kantoPolygon(0xFFB39CCD, -18f, -33f, -31f, -61f, -12f, -50f, -5f, -28f)
    kantoPolygon(0xFFB39CCD, 18f, -33f, 31f, -61f, 12f, -50f, 5f, -28f)
    megaLateForm(0xFFF4F0F7) {
        moveTo(-17f, -49f)
        quadraticTo(0f, -57f, 17f, -49f)
        lineTo(23f, -27f)
        quadraticTo(0f, -8f, -23f, -27f)
    }
    kantoPolygon(0xFFB69CCD, -21f, -36f, -5f, -32f, -17f, -24f)
    kantoPolygon(0xFFB69CCD, 21f, -36f, 5f, -32f, 17f, -24f)
    kantoOval(0xFFF0C457, -16f, -33f, 6f, 7f)
    kantoOval(0xFFF0C457, 10f, -33f, 6f, 7f)
    kantoSmile(0f, -22f, 8f)
}

private fun DrawScope.megaLateLatios(m: Float) {
    val flap = m * 7f
    for (s in listOf(-1f, 1f)) {
        megaLateForm(0xFF857AB3) {
            moveTo(s * 16f, -6f)
            lineTo(s * 59f, -22f)
            lineTo(s * 96f, -64f + s * flap)
            lineTo(s * 88f, -10f + s * flap)
            lineTo(s * 96f, 20f + s * flap)
            lineTo(s * 34f, 15f)
            lineTo(s * 14f, 22f)
        }
        kantoPolygon(0xFFB2A7D1, s * 31f, 0f, s * 84f, -41f + s * flap,
            s * 77f, -4f + s * flap, s * 89f, 13f + s * flap)
        kantoPolygon(0xFF6A649B, s * 6f, 25f, s * 35f, 39f,
            s * 46f, 60f, s * 11f, 45f)
    }
    megaLateForm(0xFF9789BD) {
        moveTo(-17f, -29f)
        quadraticTo(-34f, -5f, -19f, 28f)
        lineTo(0f, 58f)
        lineTo(19f, 28f)
        quadraticTo(34f, -5f, 17f, -29f)
    }
    kantoPolygon(0xFFECEBF5, -14f, -28f, 14f, -28f, 15f, 13f,
        0f, 34f, -15f, 13f)
    kantoPolygon(0xFF7A79B9, -11f, 6f, 11f, 6f, 0f, 20f)
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF8D80B5, s * 14f, -2f, s * 44f, 10f - m * s * 5f,
            s * 38f, 21f - m * s * 5f, s * 15f, 17f)
        kantoPolygon(0xFFDFDBEF, s * 31f, 13f, s * 43f, 10f - m * s * 5f,
            s * 39f, 20f - m * s * 5f)
    }
    kantoPolygon(0xFF8A7DB0, -18f, -37f, -30f, -70f, -12f, -56f, -7f, -30f)
    kantoPolygon(0xFF8A7DB0, 18f, -37f, 30f, -70f, 12f, -56f, 7f, -30f)
    megaLateForm(0xFFEFEDF6) {
        moveTo(-15f, -55f)
        lineTo(15f, -55f)
        lineTo(21f, -32f)
        lineTo(0f, -13f)
        lineTo(-21f, -32f)
    }
    kantoPolygon(0xFF8776AE, -19f, -42f, -5f, -37f, -15f, -28f)
    kantoPolygon(0xFF8776AE, 19f, -42f, 5f, -37f, 15f, -28f)
    kantoOval(0xFFCE505C, -15f, -39f, 5f, 7f)
    kantoOval(0xFFCE505C, 10f, -39f, 5f, 7f)
    kantoLine(KANTO_INK, 2f, -4f, -23f, 4f, -23f)
}

private fun DrawScope.megaLateRayquaza(m: Float) {
    megaLateCurve(KANTO_INK, 28f) {
        moveTo(8f, -41f)
        cubicTo(-66f, -62f, -73f, -15f, -12f, 0f)
        cubicTo(65f, 16f, 67f, 58f, 12f, 47f)
        cubicTo(-27f, 39f, -37f, 20f, -78f, 39f + m * 6f)
    }
    megaLateCurve(0xFF277E68, 24f) {
        moveTo(8f, -41f)
        cubicTo(-66f, -62f, -73f, -15f, -12f, 0f)
        cubicTo(65f, 16f, 67f, 58f, 12f, 47f)
        cubicTo(-27f, 39f, -37f, 20f, -78f, 39f + m * 6f)
    }
    megaLateCurve(0xFF7AC391, 5f) {
        moveTo(-10f, -42f)
        cubicTo(-56f, -52f, -65f, -19f, -12f, -5f)
        cubicTo(47f, 10f, 65f, 48f, 24f, 48f)
    }
    kantoPolygon(0xFF194F4D, -65f, 37f + m * 4f, -94f, 28f + m * 6f,
        -84f, 44f + m * 6f, -96f, 56f + m * 6f, -66f, 48f)
    for (i in 0..3) {
        val x = -47f + i * 14f
        val y = if (i < 2) -35f + i * 13f else -13f + (i - 2) * 8f
        kantoPolygon(0xFFEFC563, x, y - 8f, x + 7f, y - 6f,
            x + 7f, y + 3f, x + 1f, y + 6f, x - 4f, y)
    }
    kantoPolygon(0xFF1C6658, 27f, 13f, 52f, 8f, 44f, 23f, 50f, 39f, 31f, 31f)
    kantoPolygon(0xFFDB6C4C, 34f, 19f, 45f, 14f, 40f, 27f)
    for (s in listOf(-1f, 1f)) {
        val wave = s * m * 7f
        megaLateCurve(0xFFF8D578, 5f) {
            moveTo(12f, -35f)
            cubicTo(40f, -55f, 65f, -46f + wave, 93f, -63f + wave)
        }
        megaLateCurve(0xFFF8D578, 4f) {
            moveTo(16f, -21f)
            cubicTo(34f, -7f, 75f, -26f + wave, 93f, -6f + wave)
        }
    }
    kantoPolygon(0xFF287E67, -4f, -21f, -15f, 1f, -35f, 9f + m * 5f,
        -30f, 20f + m * 5f, -8f, 11f, 5f, -9f)
    kantoPolygon(0xFFEEE6D2, -34f, 9f + m * 5f, -45f, 11f + m * 5f,
        -35f, 16f + m * 5f, -42f, 21f + m * 5f, -29f, 20f + m * 5f)
    kantoPolygon(0xFF214E49, -5f, -43f, 11f, -73f, 18f, -47f,
        42f, -59f, 30f, -38f, 45f, -23f, 16f, -16f, -6f, -24f)
    kantoPolygon(0xFF469E78, -8f, -37f, 17f, -48f, 34f, -32f,
        27f, -18f, 1f, -18f, -16f, -26f)
    kantoPolygon(0xFFF0C85F, -8f, -34f, 17f, -42f, 23f, -37f,
        5f, -29f, -3f, -19f, -9f, -23f)
    kantoPolygon(0xFFFFF1D1, 11f, -34f, 24f, -35f, 18f, -28f)
    kantoOval(0xFFDA6750, 16f, -34f, 4f, 5f)
    kantoLine(KANTO_INK, 2f, 1f, -22f, 21f, -21f, 29f, -25f)
}

private fun DrawScope.megaLateSkarmory(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val flap = s * m * 7f
        megaLateForm(0xFFCCB56D) {
            moveTo(s * 12f, -17f)
            quadraticTo(s * 49f, -44f + flap, s * 96f, -52f + flap)
            lineTo(s * 76f, -30f + flap)
            lineTo(s * 94f, -32f + flap)
            lineTo(s * 69f, -11f + flap)
            lineTo(s * 85f, -12f + flap)
            lineTo(s * 57f, 10f + flap)
            lineTo(s * 69f, 12f + flap)
            lineTo(s * 36f, 24f)
            lineTo(s * 14f, 10f)
        }
        kantoPolygon(0xFFAB3850, s * 27f, -12f, s * 86f, -44f + flap,
            s * 64f, -20f + flap, s * 79f, -22f + flap,
            s * 54f, 0f + flap, s * 65f, 0f + flap, s * 33f, 17f)
        kantoLine(0xFFFFF1BB, 3f, s * 24f, -12f, s * 57f, -29f + flap,
            s * 90f, -46f + flap)
        kantoLine(0xFF8D7649, 2f, s * 32f, 0f, s * 68f, -21f + flap)
        kantoPolygon(0xFF415776, s * 10f, 24f, s * 25f, 33f + s * m * 3f,
            s * 22f, 49f, s * 37f, 55f, s * 15f, 57f, s * 10f, 39f)
        kantoPolygon(0xFFE5C779, s * 18f, 45f, s * 29f, 47f,
            s * 47f, 57f, s * 27f, 53f, s * 37f, 63f, s * 18f, 58f)
        kantoLine(0xFFFFECAF, 2f, s * 23f, 49f, s * 36f, 55f)
    }
    kantoPolygon(0xFF7C90A9, -13f, 23f, -25f, 62f, 0f, 44f, 24f, 62f,
        13f, 22f, 0f, 31f)
    megaLateForm(0xFFE1C982) {
        moveTo(-13f, -27f)
        quadraticTo(-34f, -4f, -21f, 24f)
        quadraticTo(0f, 39f, 22f, 24f)
        quadraticTo(32f, -2f, 13f, -27f)
    }
    kantoPolygon(0xFF6B8098, -22f, 7f, 0f, 20f, 23f, 7f, 20f, 26f, 0f, 34f, -20f, 25f)
    kantoPolygon(0xFFE6CE8C, -19f, -5f, 0f, -17f, 19f, -5f,
        11f, 10f, 0f, 17f, -11f, 10f)
    kantoLine(0xFFF6F3DA, 3f, -11f, -4f, 0f, -11f, 10f, -4f)
    kantoPolygon(0xFFE4CD82, -13f, -35f, -12f, -65f, 2f, -54f,
        12f, -72f, 20f, -43f, 14f, -25f)
    kantoPolygon(0xFFAF3750, 2f, -51f, 11f, -68f, 15f, -43f, 7f, -32f)
    kantoOval(0xFFEBDD9E, -19f, -48f, 39f, 29f)
    kantoPolygon(0xFFD5BE7C, -20f, -32f, -43f, -22f, -16f, -19f, -3f, -25f)
    kantoPolygon(0xFFFFFFFF, -15f, -38f, -2f, -34f, -12f, -29f)
    kantoPolygon(0xFFFFFFFF, 5f, -35f, 17f, -39f, 13f, -29f)
    kantoEyes(0f, -34f, 11f, 2.5f)
}

private fun DrawScope.megaLateDelphox(m: Float) {
    megaLateForm(0xFFBF4D67) {
        moveTo(-10f, 8f)
        cubicTo(-42f, 12f, -49f, 40f, -85f, 40f + m * 5f)
        lineTo(-68f, 48f + m * 5f)
        lineTo(-79f, 54f + m * 5f)
        quadraticTo(-41f, 61f, -15f, 36f)
    }
    megaLateForm(0xFF663E66) {
        moveTo(-13f, -21f)
        quadraticTo(-19f, 16f, -44f - m * 3f, 54f)
        lineTo(-20f, 47f)
        lineTo(-9f, 63f)
        lineTo(4f, 52f)
        lineTo(21f, 61f)
        lineTo(37f + m * 3f, 47f)
        quadraticTo(17f, 9f, 15f, -21f)
    }
    kantoPolygon(0xFFBC5475, -8f, -9f, -20f, 47f, -9f, 63f,
        3f, 50f, 8f, 7f)
    kantoPolygon(0xFF934A72, 11f, -7f, 5f, 48f, 21f, 61f, 28f, 48f)
    kantoLine(0xFFE9858D, 2f, -6f, 8f, -12f, 43f)
    kantoLine(0xFF332E48, 3f, 17f, 13f, 21f, 41f)
    withTransform({ rotate(m * 8f, Offset(13f, -12f)) }) {
        megaLateForm(0xFF563954) {
            moveTo(12f, -20f)
            lineTo(34f, -10f)
            lineTo(56f, -28f)
            lineTo(63f, -15f)
            lineTo(44f, 9f)
            lineTo(25f, 3f)
            lineTo(16f, 12f)
        }
        kantoOval(0xFFE9CDA3, 51f, -28f, 14f, 12f)
        kantoLine(0xFF6D4D40, 5f, 49f, 17f, 66f, -49f)
        megaLateForm(0xFFFFA44D) {
            moveTo(64f, -41f)
            cubicTo(50f, -52f, 72f, -61f, 66f, -70f)
            cubicTo(83f, -59f, 72f, -46f, 78f, -45f)
            quadraticTo(72f, -35f, 64f, -41f)
        }
        kantoPolygon(0xFFFFE3A0, 65f, -45f, 71f, -59f, 72f, -43f)
    }
    megaLateForm(0xFF74415E) {
        moveTo(-13f, -21f)
        lineTo(-35f, -11f + m * 4f)
        lineTo(-53f, 7f + m * 4f)
        lineTo(-39f, 19f + m * 4f)
        lineTo(-20f, 6f)
    }
    kantoOval(0xFFE9CDA3, -52f, 3f + m * 4f, 12f, 12f)
    kantoPolygon(0xFFE7D6B8, -15f, -31f, 0f, -22f, 15f, -31f,
        18f, -9f, 6f, -13f, 0f, 0f, -7f, -13f, -18f, -9f)
    kantoPolygon(0xFFE7BF8D, -14f, -38f, -44f, -71f, -43f, -42f,
        -27f, -26f, -8f, -28f)
    kantoPolygon(0xFFE7BF8D, 14f, -38f, 42f, -71f, 43f, -43f,
        28f, -25f, 8f, -28f)
    kantoPolygon(0xFFCE6459, -22f, -39f, -36f, -64f, -33f, -37f,
        -50f, -31f, -26f, -24f)
    kantoPolygon(0xFFCE6459, 22f, -39f, 36f, -64f, 33f, -37f,
        50f, -31f, 26f, -24f)
    kantoOval(0xFFF2D6AD, -19f, -48f, 38f, 30f)
    kantoPolygon(0xFFDFAC79, -19f, -39f, -4f, -43f, 0f, -32f,
        5f, -43f, 19f, -39f, 13f, -29f, -13f, -29f)
    kantoEyes(0f, -35f, 11f, 2.8f)
    kantoPolygon(0xFF54414B, -4f, -25f, 4f, -25f, 0f, -21f)
}

private fun DrawScope.megaLateDrampa(m: Float) {
    megaLateForm(0xFF75B49E) {
        moveTo(-32f, -17f)
        cubicTo(-1f, -24f, 9f, 12f, 35f, 16f)
        cubicTo(61f, 20f, 74f, 1f, 91f, 19f + m * 6f)
        cubicTo(73f, 12f, 78f, 52f, 35f, 49f)
        cubicTo(-10f, 47f, -43f, 23f, -32f, -17f)
    }
    megaLateForm(0xFFCDE1C0) {
        moveTo(-28f, -11f)
        cubicTo(-17f, 6f, -13f, 32f, 29f, 35f)
        quadraticTo(54f, 39f, 70f, 28f)
        quadraticTo(57f, 55f, 24f, 45f)
        cubicTo(-16f, 40f, -44f, 20f, -28f, -11f)
    }
    for (i in 0..4) kantoLine(0xFF90B295, 2f, -17f + i * 14f, 22f + i * 4f,
        -20f + i * 14f, 30f + i * 3f)
    megaLateForm(0xFFE8E9DA) {
        moveTo(63f, 13f)
        quadraticTo(69f, -4f + m * 6f, 80f, 6f + m * 6f)
        quadraticTo(96f, -6f + m * 6f, 99f, 11f + m * 6f)
        lineTo(89f, 25f + m * 6f)
        lineTo(96f, 32f + m * 6f)
        quadraticTo(75f, 43f, 62f, 28f)
    }
    for (s in listOf(-1f, 1f)) {
        val y = 24f + s * m * 5f
        kantoLine(0xFF6DA68E, 12f, 7f + s * 17f, 21f, s * 32f, y + 15f)
        kantoPolygon(0xFFECEEDF, s * 32f, y + 6f, s * 44f, y + 13f,
            s * 49f, y + 25f, s * 37f, y + 22f, s * 32f, y + 29f,
            s * 23f, y + 20f)
    }
    megaLateForm(0xFF739F91) {
        moveTo(-37f, 15f)
        cubicTo(-66f, 3f, -61f, -33f, -47f, -45f)
        quadraticTo(-24f, -55f, -11f, -31f)
        lineTo(-8f, -5f)
        quadraticTo(-24f, 16f, -37f, 15f)
    }
    megaLateForm(0xFFF0F0E1) {
        moveTo(-46f, -41f)
        quadraticTo(-72f, -49f, -62f, -26f)
        quadraticTo(-78f, -17f, -60f, -4f)
        lineTo(-68f, 8f)
        lineTo(-52f, 5f)
        lineTo(-54f, 24f)
        lineTo(-38f, 15f)
        lineTo(-28f, 29f)
        lineTo(-15f, 9f)
        quadraticTo(0f, -10f, -18f, -35f)
    }
    kantoPolygon(0xFFDCCB94, -49f, -45f, -58f, -69f, -41f, -60f, -35f, -43f)
    kantoPolygon(0xFFDCCB94, -22f, -44f, -17f, -66f, -6f, -54f, -11f, -34f)
    kantoOval(0xFFB9D9AC, -58f, -56f, 49f, 36f)
    kantoOval(0xFFDCEAC8, -69f, -38f, 47f, 23f)
    kantoLine(0xFFF9F5E8, 7f, -54f, -45f, -44f, -49f, -34f, -44f)
    kantoLine(0xFFF9F5E8, 7f, -29f, -45f, -18f, -48f, -9f, -40f)
    kantoEyes(-35f, -41f, 13f, 3.2f)
    kantoSmile(-48f, -26f, 18f)
    megaLateCurve(0xFFEFEFE0, 5f) {
        moveTo(-57f, -24f)
        quadraticTo(-85f, -26f + m * 5f, -91f, -9f + m * 5f)
    }
    megaLateCurve(0xFFEFEFE0, 5f) {
        moveTo(-26f, -22f)
        quadraticTo(-1f, -23f - m * 5f, 5f, -3f - m * 5f)
    }
}

private fun DrawScope.megaLateZygarde(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val fan = s * m * 5f
        kantoPolygon(0xFF263C3D, s * 15f, -25f, s * 30f, -58f,
            s * 72f, -69f + fan, s * 94f, -53f + fan, s * 61f, -49f,
            s * 83f, -25f + fan, s * 52f, -30f, s * 61f, -7f, s * 26f, -10f)
        kantoPolygon(0xFF83D876, s * 32f, -50f, s * 69f, -61f + fan,
            s * 82f, -54f + fan, s * 54f, -44f, s * 70f, -31f + fan,
            s * 41f, -37f, s * 49f, -17f, s * 29f, -23f)
        kantoPolygon(0xFFE4E9D8, s * 25f, -17f, s * 53f, -3f,
            s * 61f, 16f + fan, s * 40f, 24f + fan, s * 23f, 2f)
        kantoPolygon(0xFF344947, s * 48f, 7f + fan, s * 70f, 12f + fan,
            s * 78f, 33f + fan, s * 54f, 39f + fan, s * 38f, 23f + fan)
        for (i in 0..2) kantoPolygon(0xFF95DF81, s * (51f + i * 8f), 26f + fan,
            s * (57f + i * 8f), 25f + fan, s * (59f + i * 8f), 37f + fan,
            s * (53f + i * 8f), 40f + fan)
        kantoPolygon(0xFF273C3A, s * 11f, 25f, s * 29f, 28f,
            s * 37f, 48f, s * 53f, 58f, s * 20f, 63f, s * 7f, 43f)
        kantoPolygon(0xFFE1E9D6, s * 19f, 35f, s * 29f, 38f,
            s * 32f, 52f, s * 24f, 55f, s * 15f, 45f)
    }
    kantoPolygon(0xFF263A38, -26f, -30f, 26f, -30f, 34f, 8f,
        24f, 33f, 0f, 48f, -24f, 33f, -34f, 8f)
    kantoPolygon(0xFFE9EEDC, -22f, -27f, -7f, -19f, 0f, -25f, 7f, -19f,
        22f, -27f, 21f, -5f, 9f, 6f, 0f, 28f, -9f, 6f, -21f, -5f)
    kantoPolygon(0xFF79D572, -12f, 8f, 0f, 1f, 12f, 8f, 12f, 23f, 0f, 30f, -12f, 23f)
    kantoPolygon(0xFF324A3C, -6f, 11f, 0f, 7f, 6f, 11f, 6f, 18f, 0f, 22f, -6f, 18f)
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(if (s < 0f) 0xFFEB8063 else 0xFF65BDDD,
            s * 11f, -14f, s * 18f, -18f, s * 23f, -9f, s * 17f, -3f, s * 11f, -6f)
        kantoPolygon(0xFF8BDD7B, s * 18f, 21f, s * 25f, 18f,
            s * 28f, 26f, s * 21f, 30f)
    }
    kantoPolygon(0xFF243B38, -17f, -35f, -23f, -59f, -9f, -50f,
        0f, -72f, 9f, -50f, 23f, -59f, 17f, -35f, 0f, -22f)
    kantoPolygon(0xFFF0F1E1, -14f, -45f, 0f, -54f, 14f, -45f,
        10f, -30f, 0f, -24f, -10f, -30f)
    kantoPolygon(0xFF75CC6C, -8f, -50f, 0f, -59f, 8f, -50f, 0f, -41f)
    kantoLine(KANTO_INK, 3f, -12f, -40f, -4f, -37f)
    kantoLine(KANTO_INK, 3f, 12f, -40f, 4f, -37f)
    kantoLine(0xFF72BC68, 2f, -4f, -31f, 4f, -31f)
}

private fun DrawScope.megaLateScovillain(m: Float) {
    kantoPolygon(0xFF366E42, -18f, 27f, -32f, 52f, -46f, 60f, -18f, 60f, -4f, 39f)
    kantoPolygon(0xFF366E42, 18f, 27f, 32f, 52f, 46f, 60f, 18f, 60f, 4f, 39f)
    megaLateForm(0xFF4F8A4B) {
        moveTo(-16f, -8f)
        quadraticTo(-39f, 21f, -20f, 40f)
        lineTo(0f, 33f)
        lineTo(20f, 40f)
        quadraticTo(39f, 21f, 16f, -8f)
    }
    kantoPolygon(0xFF243F37, -24f, 1f, -7f, 5f, 0f, 27f, 7f, 5f,
        24f, 1f, 13f, 27f, 0f, 36f, -13f, 27f)
    for (s in listOf(-1f, 1f)) {
        val nod = m * s * 5f
        kantoLine(0xFF609B4F, 12f, s * 11f, 5f, s * 29f, -21f + nod)
        kantoPolygon(0xFF75AB58, s * 13f, -10f, s * 34f, -6f + nod,
            s * 48f, -18f + nod, s * 43f, 2f + nod, s * 28f, 9f,
            s * 26f, 23f, s * 12f, 10f)
        withTransform({ rotate(s * m * 12f, Offset(s * 23f, 11f)) }) {
            kantoPolygon(0xFF4E8A45, s * 22f, 7f, s * 46f, 4f,
                s * 61f, 15f, s * 78f, 12f, s * 70f, 24f,
                s * 78f, 32f, s * 57f, 31f, s * 41f, 18f)
            kantoLine(0xFF99B768, 2f, s * 36f, 11f, s * 60f, 22f, s * 71f, 21f)
        }
        val pepper = if (s < 0f) 0xFFCD4545 else 0xFF649C43
        val light = if (s < 0f) 0xFFF07759 else 0xFFA8C55B
        megaLateForm(pepper) {
            moveTo(s * 20f, -48f + nod)
            cubicTo(s * 30f, -67f + nod, s * 65f, -57f + nod, s * 65f, -36f + nod)
            lineTo(s * 57f, -12f + nod)
            lineTo(s * 43f, -5f + nod)
            lineTo(s * 28f, -11f + nod)
            cubicTo(s * 11f, -15f + nod, s * 11f, -32f + nod, s * 20f, -48f + nod)
        }
        megaLateCurve(light, 5f) {
            moveTo(s * 30f, -48f + nod)
            quadraticTo(s * 21f, -36f + nod, s * 29f, -23f + nod)
        }
        kantoPolygon(0xFF345B39, s * 26f, -52f + nod, s * 21f, -62f + nod,
            s * 39f, -58f + nod, s * 50f, -68f + nod, s * 53f, -53f + nod,
            s * 65f, -48f + nod, s * 48f, -45f + nod)
        kantoPolygon(0xFFF7E7B8, s * 21f, -35f + nod, s * 36f, -31f + nod,
            s * 25f, -25f + nod)
        kantoPolygon(0xFFF7E7B8, s * 43f, -31f + nod, s * 59f, -35f + nod,
            s * 55f, -24f + nod)
        kantoOval(KANTO_INK, s * 29f - 2f, -32f + nod, 4f, 5f)
        kantoOval(KANTO_INK, s * 51f - 2f, -32f + nod, 4f, 5f)
        kantoLine(KANTO_INK, 2f, s * 28f, -17f + nod, s * 36f, -21f + nod,
            s * 43f, -16f + nod, s * 54f, -20f + nod)
    }
}

private fun DrawScope.megaLateGlimmora(m: Float) {
    for (i in 0 until 12) {
        val spread = if (i % 2 == 0) m * 4f else -m * 4f
        withTransform({ rotate(i * 30f + m * 3f, Offset(0f, -4f)) }) {
            kantoPolygon(if (i % 2 == 0) 0xFFABA4DB else 0xFF858CC7,
                -8f, -34f - spread, -13f, -52f - spread, 0f, -68f - spread,
                13f, -52f - spread, 8f, -34f - spread, 0f, -26f - spread)
            kantoPolygon(0xFFCDD3EF, 0f, -68f - spread, 10f, -52f - spread,
                0f, -32f - spread)
            kantoLine(0xFFB1E7BD, 2f, 0f, -61f - spread, 0f, -36f - spread)
        }
    }
    for (i in 0 until 6) {
        withTransform({ rotate(i * 60f - m * 6f, Offset(0f, -4f)) }) {
            kantoPolygon(0xFF81C9AE, -7f, -16f, -10f, -33f, 0f, -45f,
                10f, -33f, 7f, -16f, 0f, -8f)
            kantoLine(0xFFC0F0D1, 2f, 0f, -36f, 0f, -17f)
        }
    }
    megaLateForm(0xFF303E66) {
        moveTo(-19f, -21f)
        quadraticTo(0f, -35f, 19f, -21f)
        lineTo(16f, 8f)
        lineTo(0f, 35f)
        lineTo(-16f, 8f)
    }
    kantoPolygon(0xFF52648B, -16f, -20f, -4f, -27f, -2f, 20f, -12f, 6f)
    kantoPolygon(0xFF182C4F, 5f, -24f, 17f, -18f, 14f, 7f, 0f, 35f)
    kantoOval(0xFFF3D965, -13f, -13f, 10f, 15f)
    kantoOval(0xFFF3D965, 3f, -13f, 10f, 15f)
    kantoOval(0xFF182F54, -9f, -10f, 3f, 9f)
    kantoOval(0xFF182F54, 7f, -10f, 3f, 9f)
    kantoPolygon(0xFF8BCFB8, -9f, 15f, 0f, 10f, 9f, 15f, 0f, 29f)
    kantoLine(0xFFF0F8E3, 2f, -8f, -22f, -3f, -24f)
}

private fun DrawScope.megaLateTatsugiriCurly(m: Float) {
    megaLateForm(0xFFE6ECE1) {
        moveTo(-48f, 10f)
        quadraticTo(-70f, 9f, -68f, 28f)
        quadraticTo(-87f, 39f, -64f, 48f)
        quadraticTo(-55f, 63f, -32f, 53f)
        quadraticTo(-16f, 64f, 2f, 53f)
        quadraticTo(31f, 63f, 48f, 43f)
        lineTo(50f, 17f)
        quadraticTo(20f, -7f, -6f, 11f)
    }
    megaLateCurve(0xFFC0D3CF, 3f) {
        moveTo(-56f, 40f)
        quadraticTo(-36f, 27f, -20f, 42f)
        quadraticTo(3f, 29f, 19f, 45f)
        quadraticTo(32f, 34f, 39f, 33f)
    }
    megaLateForm(0xFFF09545) {
        moveTo(-48f, -23f)
        cubicTo(-12f, -38f, 16f, -5f, 36f, -18f)
        cubicTo(60f, -32f, 18f, -56f, 35f, -62f)
        cubicTo(83f, -72f, 92f, -15f + m * 4f, 49f, 15f)
        cubicTo(20f, 36f, -19f, 15f, -48f, 3f)
    }
    megaLateCurve(0xFFFFC569, 5f) {
        moveTo(-19f, -19f)
        cubicTo(6f, -15f, 25f, 11f, 52f, -5f)
        quadraticTo(77f, -21f, 61f, -43f)
    }
    kantoPolygon(0xFFBA503C, 36f, -56f, 21f, -71f + m * 4f,
        46f, -67f, 60f, -56f)
    kantoPolygon(0xFFED713F, 16f, -12f, 22f, -33f, 37f, -19f, 47f, -27f,
        44f, -7f, 31f, 4f)
    withTransform({ rotate(m * 12f, Offset(-12f, 5f)) }) {
        megaLateForm(0xFFD4663C) {
            moveTo(-18f, 3f)
            quadraticTo(-17f, 24f, 10f, 35f)
            lineTo(5f, 19f)
            lineTo(17f, 20f)
            lineTo(1f, 3f)
        }
        kantoLine(0xFFFFB465, 2f, -9f, 8f, 3f, 24f)
    }
    megaLateForm(0xFFFFA653) {
        moveTo(-58f, -38f)
        quadraticTo(-30f, -43f, -20f, -23f)
        lineTo(-23f, -1f)
        quadraticTo(-48f, 11f, -68f, -6f)
        quadraticTo(-79f, -20f, -58f, -38f)
    }
    kantoPolygon(0xFFB84937, -59f, -34f, -49f, -47f, -43f, -36f,
        -30f, -45f, -29f, -24f)
    kantoOval(0xFFFFE6B3, -62f, -12f, 39f, 18f)
    kantoEyes(-47f, -22f, 12f, 3.5f)
    kantoSmile(-46f, -3f, 12f)
    megaLateCurve(0xFFFFD488, 3f) {
        moveTo(-65f, -6f)
        quadraticTo(-86f, -18f + m * 5f, -94f, -7f + m * 5f)
    }
    megaLateCurve(0xFFFFD488, 3f) {
        moveTo(-26f, -4f)
        quadraticTo(-12f, -15f - m * 4f, 3f, -7f - m * 4f)
    }
}

private fun DrawScope.megaLateTatsugiriDroopy(m: Float) {
    megaLateForm(0xFFEDEAE6) {
        moveTo(-58f, 7f)
        quadraticTo(-71f, 19f, -55f, 31f)
        quadraticTo(-70f, 46f, -46f, 53f)
        quadraticTo(-20f, 64f, 0f, 51f)
        quadraticTo(15f, 63f, 40f, 50f)
        quadraticTo(67f, 55f, 70f, 35f)
        quadraticTo(78f, 16f, 55f, 12f)
        quadraticTo(-4f, -14f, -58f, 7f)
    }
    megaLateCurve(0xFFCFD8D7, 3f) {
        moveTo(-45f, 39f)
        quadraticTo(-30f, 28f, -17f, 41f)
        quadraticTo(0f, 31f, 13f, 43f)
        quadraticTo(33f, 28f, 48f, 39f)
    }
    megaLateForm(0xFFD57CA4) {
        moveTo(42f, -9f)
        cubicTo(27f, -50f, -3f, -12f, -29f, -26f)
        cubicTo(-60f, -42f, -72f, -23f, -71f, -6f + m * 6f)
        cubicTo(-91f, -18f + m * 6f, -88f, -55f, -53f, -53f)
        cubicTo(-20f, -54f, -5f, -35f, 13f, -43f)
        cubicTo(46f, -59f, 79f, -15f, 58f, 15f)
        lineTo(34f, 16f)
    }
    megaLateCurve(0xFFF5B5CC, 5f) {
        moveTo(-71f, -37f)
        cubicTo(-39f, -57f, -21f, -23f, 10f, -33f)
        quadraticTo(40f, -46f, 51f, -10f)
    }
    megaLateForm(0xFFB76097) {
        moveTo(-75f, -17f + m * 6f)
        quadraticTo(-86f, -9f + m * 6f, -89f, 14f + m * 6f)
        lineTo(-79f, 6f + m * 6f)
        lineTo(-73f, 20f + m * 6f)
        quadraticTo(-59f, 0f, -68f, -17f)
    }
    withTransform({ rotate(-m * 11f, Offset(20f, -6f)) }) {
        megaLateForm(0xFFC269A0) {
            moveTo(15f, -11f)
            quadraticTo(-5f, 2f, 0f, 30f)
            lineTo(10f, 17f)
            lineTo(15f, 27f)
            quadraticTo(28f, 7f, 26f, -8f)
        }
        kantoLine(0xFFF6B5CF, 2f, 20f, -3f, 9f, 17f)
    }
    megaLateForm(0xFFE594B4) {
        moveTo(33f, -25f)
        quadraticTo(64f, -36f, 71f, -13f)
        lineTo(69f, 11f)
        quadraticTo(56f, 30f, 28f, 13f)
        lineTo(24f, -8f)
    }
    kantoPolygon(0xFFC36398, 35f, -24f, 29f, -37f, 47f, -30f,
        59f, -36f, 63f, -22f)
    kantoOval(0xFFF9D8DC, 28f, 1f, 42f, 19f)
    kantoEyes(48f, -7f, 12f, 3.2f)
    kantoLine(0xFFA65C8C, 3f, 31f, -13f, 42f, -10f)
    kantoLine(0xFFA65C8C, 3f, 55f, -10f, 67f, -13f)
    kantoSmile(49f, 12f, 10f)
    megaLateCurve(0xFFF8CDDF, 3f) {
        moveTo(68f, 9f)
        quadraticTo(94f, 3f - m * 5f, 88f, 29f - m * 5f)
    }
}

private fun DrawScope.megaLateTatsugiriStretchy(m: Float) {
    megaLateForm(0xFFE9EEE3) {
        moveTo(-72f, 17f)
        quadraticTo(-85f, 35f, -60f, 44f)
        quadraticTo(-47f, 58f, -26f, 49f)
        quadraticTo(-8f, 61f, 9f, 47f)
        quadraticTo(37f, 57f, 52f, 40f)
        quadraticTo(75f, 41f, 79f, 20f)
        quadraticTo(36f, 9f, -6f, 23f)
        quadraticTo(-45f, 8f, -72f, 17f)
    }
    megaLateCurve(0xFFC7DAD2, 3f) {
        moveTo(-57f, 35f)
        quadraticTo(-35f, 24f, -17f, 40f)
        quadraticTo(0f, 29f, 17f, 39f)
        quadraticTo(37f, 24f, 59f, 30f)
    }
    megaLateForm(0xFFF0CA55) {
        moveTo(-56f, -21f)
        cubicTo(-15f, -42f, 23f, 4f, 53f, -8f)
        quadraticTo(72f, -16f, 72f, -42f + m * 6f)
        lineTo(90f, -47f + m * 6f)
        cubicTo(96f, -11f, 70f, 13f, 44f, 14f)
        cubicTo(4f, 16f, -25f, -8f, -56f, 2f)
    }
    megaLateCurve(0xFFFFE991, 5f) {
        moveTo(-27f, -20f)
        cubicTo(0f, -20f, 29f, 7f, 55f, 1f)
        quadraticTo(78f, -6f, 82f, -28f + m * 4f)
    }
    kantoPolygon(0xFFDAA043, 72f, -34f + m * 6f, 61f, -60f + m * 6f,
        79f, -49f + m * 6f, 94f, -61f + m * 6f, 89f, -35f + m * 6f)
    kantoPolygon(0xFFE0A244, -20f, -24f, -4f, -47f, 8f, -22f, 23f, -34f,
        30f, -8f, 8f, -10f)
    for (s in listOf(-1f, 1f)) {
        withTransform({ rotate(s * m * 12f, Offset(-20f, -2f)) }) {
            kantoPolygon(0xFFE4AE49, -28f, -6f, -9f, -2f,
                4f + s * 10f, 22f, -12f, 14f, -18f, 22f)
            kantoLine(0xFFFFE991, 2f, -18f, 0f, -7f, 14f)
        }
    }
    megaLateForm(0xFFFFD768) {
        moveTo(-64f, -36f)
        quadraticTo(-37f, -43f, -28f, -20f)
        lineTo(-32f, 0f)
        quadraticTo(-55f, 13f, -78f, -2f)
        lineTo(-80f, -17f)
    }
    kantoPolygon(0xFFCB923D, -68f, -33f, -60f, -53f, -50f, -34f,
        -36f, -45f, -34f, -25f)
    kantoOval(0xFFFFEDBD, -77f, -7f, 45f, 18f)
    kantoEyes(-55f, -20f, 13f, 3.3f)
    kantoSmile(-55f, 4f, 13f)
    megaLateCurve(0xFFFFE58C, 3f) {
        moveTo(-76f, -3f)
        quadraticTo(-97f, -5f + m * 4f, -97f, -24f + m * 4f)
    }
    megaLateCurve(0xFFFFE58C, 3f) {
        moveTo(-35f, -3f)
        quadraticTo(-12f, -11f - m * 5f, 3f, -1f - m * 5f)
    }
}

private fun DrawScope.megaLateBaxcalibur(m: Float) {
    megaLateForm(0xFF334D66) {
        moveTo(18f, 21f)
        cubicTo(45f, 10f, 64f, 41f, 94f, 41f + m * 5f)
        quadraticTo(69f, 61f, 32f, 45f)
        lineTo(7f, 40f)
    }
    kantoPolygon(0xFFA9D8E4, 38f, 23f, 56f, 12f, 60f, 31f, 80f, 25f,
        75f, 42f, 50f, 36f)
    kantoPolygon(0xFFE1F5F3, 12f, 16f, 14f, -47f, 34f, -75f, 45f, -43f,
        41f, -7f, 55f, 17f, 33f, 35f)
    kantoPolygon(0xFF92C4D9, 34f, -75f, 45f, -43f, 41f, -7f,
        55f, 17f, 35f, 25f, 26f, -13f)
    kantoLine(0xFFFFFFFF, 3f, 31f, -57f, 24f, -33f, 26f, -7f)
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 4f
        kantoPolygon(0xFF345069, s * 9f, 21f, s * 31f, 29f,
            s * 33f, 47f + step, s * 42f, 58f + step, s * 13f, 60f + step,
            s * 8f, 43f)
        for (i in 0..2) kantoPolygon(0xFFDCECE8, s * (15f + i * 8f), 53f + step,
            s * (21f + i * 8f), 55f + step, s * (22f + i * 8f), 61f + step)
    }
    megaLateForm(0xFF42647B) {
        moveTo(-16f, -28f)
        quadraticTo(12f, -40f, 24f, -9f)
        lineTo(28f, 23f)
        quadraticTo(1f, 46f, -27f, 25f)
        lineTo(-30f, 1f)
    }
    kantoPolygon(0xFF9AC6CF, -17f, -9f, 3f, -17f, 19f, 2f,
        17f, 27f, -5f, 35f, -22f, 19f)
    kantoLine(0xFF6795AD, 2f, -15f, 4f, 16f, 7f)
    kantoLine(0xFF6795AD, 2f, -17f, 16f, 15f, 20f)
    for (s in listOf(-1f, 1f)) {
        withTransform({ rotate(s * m * 10f, Offset(s * 20f, -7f)) }) {
            kantoPolygon(0xFF405D74, s * 18f, -15f, s * 38f, -5f,
                s * 53f, 12f, s * 44f, 27f, s * 27f, 11f)
            kantoPolygon(0xFFD8595E, s * 35f, -1f, s * 60f, -13f,
                s * 51f, 12f, s * 37f, 16f)
            kantoPolygon(0xFFE3F1EB, s * 44f, 15f, s * 60f, 20f,
                s * 49f, 23f, s * 58f, 29f, s * 41f, 28f)
        }
    }
    kantoPolygon(0xFFBCDCE3, -25f, -37f, -23f, -67f, -10f, -51f,
        5f, -64f, 9f, -40f)
    kantoPolygon(0xFF4F7388, -26f, -52f, -6f, -56f, 14f, -39f,
        12f, -22f, -15f, -16f, -36f, -29f)
    kantoPolygon(0xFFB6D4DB, -35f, -30f, -13f, -28f, 11f, -32f,
        5f, -19f, -18f, -13f, -32f, -21f)
    kantoPolygon(0xFFE8F0D9, -26f, -42f, -10f, -37f, -22f, -32f)
    kantoOval(0xFFCD5951, -23f, -39f, 5f, 6f)
    kantoLine(KANTO_INK, 2f, -29f, -22f, -13f, -21f, 1f, -25f)
}

private fun DrawScope.megaLateChimecho(m: Float) {
    megaLateForm(0xFFCBE4E4) {
        moveTo(-6f, 11f)
        cubicTo(4f, 25f, -18f + m * 6f, 31f, -9f + m * 6f, 45f)
        lineTo(13f + m * 6f, 59f)
        lineTo(27f + m * 6f, 39f)
        cubicTo(10f, 30f, 21f, 17f, 9f, 9f)
    }
    kantoPolygon(0xFFE28186, -9f + m * 6f, 44f, 16f + m * 6f, 49f,
        13f + m * 6f, 59f, 1f + m * 6f, 53f)
    kantoLine(0xFF7DAFB8, 2f, 3f, 17f, 6f + m * 6f, 35f)
    for (s in listOf(-1f, 1f)) {
        val breeze = s * m * 6f
        megaLateForm(0xFFDCEEE8) {
            moveTo(s * 17f, -11f)
            cubicTo(s * 40f, -33f, s * 44f, 5f + breeze, s * 69f, 0f + breeze)
            quadraticTo(s * 88f, -8f + breeze, s * 89f, 8f + breeze)
            lineTo(s * 77f, 40f + breeze)
            lineTo(s * 58f, 29f + breeze)
            lineTo(s * 66f, 13f + breeze)
            cubicTo(s * 41f, 21f + breeze, s * 35f, -9f, s * 16f, 3f)
        }
        kantoPolygon(0xFFE59096, s * 63f, 22f + breeze, s * 82f, 26f + breeze,
            s * 77f, 40f + breeze, s * 58f, 29f + breeze)
        kantoLine(0xFF98C7C9, 2f, s * 72f, 11f + breeze, s * 69f, 27f + breeze)
    }
    megaLateForm(0xFFBFDDDF) {
        moveTo(-30f, -30f)
        quadraticTo(-37f, -13f, -23f, 8f)
        quadraticTo(0f, 25f, 23f, 8f)
        quadraticTo(37f, -13f, 30f, -30f)
        quadraticTo(0f, -48f, -30f, -30f)
    }
    kantoOval(0xFFE4F1E9, -23f, -27f, 46f, 32f)
    kantoPolygon(0xFFE7848B, -30f, -26f, -19f, -36f, 0f, -30f,
        19f, -36f, 30f, -26f, 16f, -21f, 0f, -25f, -16f, -21f)
    megaLateForm(0xFFCADFE0) {
        moveTo(-6f, -39f)
        quadraticTo(-21f, -58f, -37f, -51f)
        quadraticTo(-50f, -44f, -43f, -31f)
        quadraticTo(-63f, -43f, -49f, -60f)
        quadraticTo(-28f, -76f, -8f, -55f)
        lineTo(0f, -49f)
        lineTo(8f, -55f)
        quadraticTo(28f, -76f, 49f, -60f)
        quadraticTo(63f, -43f, 43f, -31f)
        quadraticTo(50f, -44f, 37f, -51f)
        quadraticTo(21f, -58f, 6f, -39f)
    }
    kantoOval(0xFFECC66C, -13f, -57f, 26f, 21f)
    kantoOval(0xFFFFE9AD, -9f, -54f, 13f, 10f)
    kantoEyes(0f, -13f, 12f, 3.5f)
    kantoSmile(0f, -1f, 11f)
    kantoOval(0xFFE6A1A0, -23f, -7f, 9f, 5f)
    kantoOval(0xFFE6A1A0, 14f, -7f, 9f, 5f)
}

private fun DrawScope.megaLateGolisopod(m: Float) {
    for (s in listOf(-1f, 1f)) {
        for (i in 0..2) {
            val y = 10f + i * 13f
            kantoPolygon(0xFF594C75, s * 19f, y, s * (41f + i * 3f), y + s * m * 3f,
                s * (50f + i * 3f), y + 14f, s * (41f + i * 3f), y + 19f,
                s * 32f, y + 7f)
        }
        kantoPolygon(0xFF9BABB0, s * 14f, -25f, s * 39f, -31f,
            s * 68f, -46f + s * m * 5f, s * 75f, -29f + s * m * 5f,
            s * 46f, -9f, s * 26f, -5f)
        kantoPolygon(0xFFD8E2D8, s * 61f, -35f + s * m * 5f,
            s * 89f, -52f + s * m * 5f, s * 91f, -27f + s * m * 5f,
            s * 76f, -8f + s * m * 5f, s * 79f, -31f + s * m * 5f)
    }
    kantoPolygon(0xFF596174, -27f, -32f, 0f, -44f, 27f, -32f,
        34f, 12f, 24f, 40f, 0f, 55f, -24f, 40f, -34f, 12f)
    for (i in 0..4) {
        val y = -19f + i * 13f
        val w = 29f - i * 2f
        kantoPolygon(if (i % 2 == 0) 0xFFC3D3CF else 0xFFA9BFBC,
            -w, y - 7f, 0f, y, w, y - 7f, w - 1f, y + 7f,
            0f, y + 15f, -w + 1f, y + 7f)
        kantoLine(0xFF758A91, 2f, -w + 6f, y + 4f, 0f, y + 10f, w - 6f, y + 4f)
    }
    for (s in listOf(-1f, 1f)) {
        val slash = s * m * 6f
        kantoPolygon(0xFF68577F, s * 26f, -13f, s * 47f, -8f + slash,
            s * 54f, 16f + slash, s * 36f, 20f + slash, s * 23f, 2f)
        megaLateForm(0xFFD8E0D5) {
            moveTo(s * 41f, -6f + slash)
            quadraticTo(s * 70f, -7f + slash, s * 74f, 19f + slash)
            lineTo(s * 66f, 45f + slash)
            lineTo(s * 57f, 28f + slash)
            lineTo(s * 47f, 40f + slash)
            lineTo(s * 36f, 12f + slash)
        }
        kantoPolygon(0xFF91A9AD, s * 43f, 6f + slash, s * 62f, 4f + slash,
            s * 66f, 19f + slash, s * 57f, 28f + slash, s * 46f, 23f + slash)
        for (i in 0..2) kantoPolygon(0xFFE8EEE0, s * (45f + i * 9f), 22f + slash,
            s * (54f + i * 9f), 20f + slash, s * (52f + i * 9f), 47f + slash)
    }
    kantoPolygon(0xFFD4DFD6, -21f, -42f, -22f, -61f, -7f, -54f, 0f, -69f,
        7f, -54f, 22f, -61f, 21f, -42f, 12f, -25f, -12f, -25f)
    kantoPolygon(0xFF8A7A9E, -18f, -43f, 0f, -50f, 18f, -43f,
        12f, -27f, 0f, -22f, -12f, -27f)
    kantoPolygon(0xFFF1F0CF, -16f, -41f, -3f, -37f, -12f, -32f)
    kantoPolygon(0xFFF1F0CF, 16f, -41f, 3f, -37f, 12f, -32f)
    kantoEyes(0f, -37f, 11f, 2.7f)
    kantoPolygon(0xFFDDE5D9, -12f, -28f, -3f, -23f, -4f, -14f, -15f, -20f)
    kantoPolygon(0xFFDDE5D9, 12f, -28f, 3f, -23f, 4f, -14f, 15f, -20f)
    kantoLine(0xFFBACEC4, 3f, -14f, -49f, -35f, -66f, -48f, -60f)
    kantoLine(0xFFBACEC4, 3f, 14f, -49f, 35f, -66f, 48f, -60f)
}

private fun DrawScope.megaLateGolurk(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val step = s * m * 4f
        kantoPolygon(0xFF436F79, s * 8f, 25f, s * 29f, 24f,
            s * 31f, 48f + step, s * 43f, 60f + step, s * 11f, 61f + step,
            s * 8f, 41f)
        kantoPolygon(0xFF769F9B, s * 12f, 38f, s * 28f, 37f,
            s * 30f, 47f + step, s * 13f, 49f + step)
        kantoLine(0xFFD6B77A, 3f, s * 13f, 52f + step, s * 32f, 52f + step)
        val arm = s * m * 6f
        kantoPolygon(0xFF6B9897, s * 25f, -29f, s * 50f, -31f + arm,
            s * 58f, -14f + arm, s * 46f, 4f + arm, s * 25f, -3f)
        kantoOval(0xFF315965, s * 53f - 11f, -14f + arm, 22f, 24f)
        kantoPolygon(0xFF53858A, s * 41f, -5f + arm, s * 73f, -12f + arm,
            s * 86f, 1f + arm, s * 84f, 27f + arm, s * 55f, 39f + arm,
            s * 38f, 23f + arm)
        kantoPolygon(0xFF88B1A7, s * 43f, 0f + arm, s * 72f, -5f + arm,
            s * 78f, 6f + arm, s * 47f, 13f + arm)
        kantoLine(0xFFD9B56C, 4f, s * 45f, 18f + arm, s * 79f, 10f + arm)
        kantoLine(0xFF31545F, 2f, s * 57f, 25f + arm, s * 60f, 32f + arm)
        kantoLine(0xFF31545F, 2f, s * 69f, 21f + arm, s * 73f, 28f + arm)
    }
    kantoPolygon(0xFF5F9696, -29f, -30f, -10f, -39f, 11f, -39f,
        29f, -30f, 35f, 9f, 24f, 35f, -24f, 35f, -35f, 9f)
    kantoPolygon(0xFF244C57, -19f, -21f, 0f, -30f, 19f, -21f,
        22f, 17f, 0f, 30f, -22f, 17f)
    megaLateForm(0xFFFFCF71) {
        moveTo(-9f, 21f)
        cubicTo(-21f, 9f, 1f, 0f, -7f, -13f)
        lineTo(3f, -28f)
        quadraticTo(1f, -5f, 12f, -3f)
        quadraticTo(23f, 17f, 0f, 26f)
    }
    kantoPolygon(0xFFFFEDBA, -5f, 16f, 4f, -7f, 8f, 15f, 0f, 23f)
    kantoPolygon(0xFFD1AE6D, -31f, -18f, -23f, -26f, -11f, -17f,
        -15f, -9f, 29f, 16f, 25f, 24f, -21f, -4f)
    kantoLine(0xFF96BEB1, 3f, -27f, 15f, -19f, 25f)
    kantoLine(0xFF96BEB1, 3f, 26f, -13f, 29f, 3f)
    kantoPolygon(0xFF6D9C99, -18f, -57f, -9f, -67f, 12f, -64f,
        23f, -49f, 17f, -30f, -17f, -30f, -23f, -46f)
    kantoPolygon(0xFF426A78, -20f, -51f, 20f, -51f, 16f, -33f, -17f, -33f)
    kantoLine(0xFFE6C475, 4f, -20f, -53f, 18f, -53f)
    kantoPolygon(0xFFFFDE83, -14f, -44f, -3f, -44f, -5f, -38f, -14f, -39f)
    kantoPolygon(0xFFFFDE83, 14f, -44f, 3f, -44f, 5f, -38f, 14f, -39f)
    kantoLine(0xFFB8CDC0, 2f, -8f, -59f, 6f, -59f)
}

private fun DrawScope.megaLateStaraptor(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val wing = m * s * 6f
        megaLateForm(0xFF494754) {
            moveTo(s * 15f, -20f)
            quadraticTo(s * 39f, -55f + wing, s * 76f, -66f + wing)
            lineTo(s * 72f, -47f + wing)
            lineTo(s * 96f, -56f + wing)
            lineTo(s * 85f, -32f + wing)
            lineTo(s * 97f, -37f + wing)
            lineTo(s * 80f, -11f + wing)
            lineTo(s * 92f, -15f + wing)
            lineTo(s * 70f, 9f + wing)
            lineTo(s * 78f, 8f + wing)
            lineTo(s * 43f, 30f)
            lineTo(s * 20f, 11f)
        }
        kantoPolygon(0xFFAAA5A7, s * 27f, -17f, s * 67f, -53f + wing,
            s * 58f, -28f + wing, s * 83f, -41f + wing,
            s * 59f, -9f + wing, s * 75f, -16f + wing, s * 44f, 13f)
        kantoLine(0xFF68636D, 3f, s * 30f, -10f, s * 48f, -25f + wing)
        kantoPolygon(0xFFB85456, s * 51f, -32f + wing, s * 69f, -45f + wing,
            s * 58f, -18f + wing, s * 40f, -3f)
        kantoLine(0xFFE3AD66, 7f, s * 11f, 31f, s * 21f, 46f + s * m * 3f)
        kantoLine(0xFFE3AD66, 4f, s * 21f, 46f + s * m * 3f,
            s * 34f, 51f + s * m * 3f)
        kantoLine(0xFFE3AD66, 4f, s * 21f, 46f + s * m * 3f,
            s * 22f, 56f + s * m * 3f)
        kantoLine(0xFFE3AD66, 4f, s * 21f, 46f + s * m * 3f,
            s * 10f, 52f + s * m * 3f)
    }
    kantoPolygon(0xFF44434F, -15f, 20f, -34f, 51f, -13f, 49f,
        0f, 64f, 13f, 49f, 34f, 51f, 15f, 20f)
    kantoPolygon(0xFFE2DADD, -16f, 41f, 0f, 52f, 16f, 41f,
        10f, 54f, 0f, 64f, -10f, 54f)
    megaLateForm(0xFF69616A) {
        moveTo(-19f, -29f)
        quadraticTo(-36f, 0f, -18f, 28f)
        quadraticTo(0f, 44f, 18f, 28f)
        quadraticTo(36f, 0f, 19f, -29f)
    }
    kantoPolygon(0xFFE7DFDE, -24f, -17f, -10f, -6f, 0f, -14f, 10f, -6f,
        24f, -17f, 21f, 5f, 12f, 11f, 0f, 31f, -12f, 11f, -21f, 5f)
    kantoPolygon(0xFF46434E, -11f, 2f, 0f, 9f, 11f, 2f, 0f, 23f)
    kantoOval(0xFF55515C, -23f, -47f, 46f, 35f)
    kantoPolygon(0xFFF1E9E4, -24f, -39f, -10f, -42f, 0f, -32f,
        10f, -42f, 24f, -39f, 16f, -20f, -16f, -20f)
    megaLateForm(0xFF46414F) {
        moveTo(-21f, -41f)
        quadraticTo(-32f, -69f, -4f, -68f)
        cubicTo(20f, -71f, 31f, -56f, 53f, -63f + m * 5f)
        quadraticTo(49f, -44f + m * 5f, 24f, -44f)
        lineTo(7f, -39f)
        lineTo(-1f, -26f)
        lineTo(-9f, -42f)
    }
    kantoPolygon(0xFFCA5A5F, -8f, -60f, 13f, -60f, 32f, -52f,
        49f, -59f + m * 5f, 34f, -46f, 8f, -43f, -1f, -29f)
    kantoEyes(0f, -30f, 12f, 3f)
    kantoPolygon(0xFFE8B65E, -6f, -25f, 6f, -25f, 8f, -16f, 0f, -11f, -8f, -16f)
}

private fun DrawScope.megaLateCrabominable(m: Float) {
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF817196, s * 14f, 27f, s * 39f, 35f, s * 51f, 54f,
            s * 36f, 59f, s * 27f, 46f, s * 13f, 46f)
        kantoPolygon(0xFFF1EFE7, s * 30f, 42f, s * 42f, 41f, s * 57f, 56f,
            s * 46f, 61f, s * 37f, 55f, s * 29f, 61f, s * 21f, 52f)
        kantoLine(0xFF937DA4, 10f, s * 23f, 25f, s * 49f, 22f + m * s * 3f)
        kantoPolygon(0xFFE8EDE6, s * 45f, 13f, s * 62f, 22f + m * s * 3f,
            s * 62f, 37f + m * s * 3f, s * 51f, 31f, s * 43f, 35f)
    }
    megaLateForm(0xFFEAEFE8) {
        moveTo(-29f, -25f)
        lineTo(-17f, -36f)
        lineTo(-4f, -29f)
        lineTo(9f, -37f)
        lineTo(23f, -27f)
        lineTo(35f, -12f)
        lineTo(32f, 9f)
        lineTo(41f, 21f)
        lineTo(25f, 40f)
        lineTo(12f, 35f)
        lineTo(0f, 46f)
        lineTo(-13f, 36f)
        lineTo(-29f, 41f)
        lineTo(-38f, 18f)
        lineTo(-31f, 7f)
        quadraticTo(-41f, -10f, -29f, -25f)
    }
    kantoPolygon(0xFFBFD4D4, -20f, 3f, 0f, 13f, 20f, 3f,
        25f, 25f, 11f, 24f, 0f, 35f, -11f, 24f, -25f, 25f)
    for (s in listOf(-1f, 1f)) {
        val punch = s * m * 7f
        kantoLine(0xFF8E7FA3, 18f, s * 24f, -15f, s * 44f, -25f + punch)
        megaLateForm(0xFFF1F0E8) {
            moveTo(s * 39f, -38f + punch)
            lineTo(s * 52f, -49f + punch)
            lineTo(s * 65f, -44f + punch)
            lineTo(s * 78f, -49f + punch)
            quadraticTo(s * 100f, -33f + punch, s * 94f, -7f + punch)
            lineTo(s * 98f, 7f + punch)
            lineTo(s * 81f, 21f + punch)
            lineTo(s * 69f, 15f + punch)
            lineTo(s * 55f, 24f + punch)
            lineTo(s * 41f, 6f + punch)
            lineTo(s * 43f, -6f + punch)
            quadraticTo(s * 31f, -20f + punch, s * 39f, -38f + punch)
        }
        kantoOval(0xFF75618F, s * 68f - 20f, -28f + punch, 40f, 33f)
        kantoOval(0xFFC1A1BF, s * 68f - 12f, -22f + punch, 24f, 20f)
        for (i in 0..2) kantoOval(0xFF9980A8, s * (51f + i * 15f) - 4f,
            -36f + punch, 8f, 8f)
        kantoLine(0xFFBCD1D1, 2f, s * 50f, 10f + punch, s * 55f, 16f + punch)
        kantoLine(0xFFBCD1D1, 2f, s * 83f, 10f + punch, s * 79f, 15f + punch)
    }
    kantoOval(0xFFD6D5DD, -24f, -38f, 48f, 35f)
    kantoPolygon(0xFFEEE9D2, -27f, -31f, -31f, -57f, -13f, -48f,
        -4f, -62f, 6f, -50f, 24f, -56f, 26f, -31f, 14f, -37f,
        0f, -31f, -14f, -37f)
    kantoEyes(0f, -24f, 12f, 3.6f)
    kantoPolygon(0xFFB694B1, -13f, -15f, 0f, -20f, 13f, -15f, 8f, -5f, -8f, -5f)
    kantoLine(0xFFEEEAE1, 3f, -6f, -11f, 6f, -11f)
}

private fun DrawScope.megaLateHeatran(m: Float) {
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF7E493F, s * 20f, 9f, s * 48f, -1f,
            s * 73f, 14f + m * s * 4f, s * 80f, 38f + m * s * 4f,
            s * 54f, 43f, s * 47f, 24f, s * 25f, 28f)
        kantoOval(0xFF9B9895, s * 68f - 19f, 25f + m * s * 4f, 38f, 21f)
        for (i in 0..2) kantoPolygon(0xFFD9D8C9, s * (52f + i * 12f), 34f + m * s * 4f,
            s * (61f + i * 12f), 35f + m * s * 4f,
            s * (65f + i * 12f), 46f + m * s * 4f)
    }
    megaLateForm(0xFF914D3F) {
        moveTo(-48f, -17f)
        quadraticTo(-46f, -52f, -11f, -56f)
        quadraticTo(26f, -66f, 48f, -30f)
        lineTo(55f, 5f)
        quadraticTo(36f, 33f, -7f, 28f)
        quadraticTo(-58f, 29f, -48f, -17f)
    }
    kantoPolygon(0xFF5A5355, -43f, -20f, -28f, -43f, -15f, -31f,
        -3f, -52f, 12f, -36f, 26f, -50f, 44f, -24f, 35f, -11f,
        44f, 7f, 13f, 19f, -18f, 14f, -38f, 1f)
    kantoPolygon(0xFFABA6A0, -32f, -33f, -31f, -58f, -13f, -44f,
        -3f, -71f, 11f, -49f, 31f, -64f, 32f, -37f, 13f, -28f, -11f, -32f)
    kantoPolygon(0xFFD4CEC0, -3f, -71f, 7f, -49f, -4f, -35f, -12f, -44f)
    kantoLine(0xFFFFA447, 5f, -37f, -16f, -22f, -22f, -5f, -12f, 12f, -22f,
        29f, -14f, 43f, -19f)
    kantoLine(0xFFFFD174, 2f, -34f, -16f, -22f, -21f, -6f, -12f)
    for (s in listOf(-1f, 1f)) {
        val stomp = m * s * 4f
        kantoPolygon(0xFFA95B43, s * 24f, 8f, s * 43f, 12f,
            s * 53f, 35f + stomp, s * 44f, 52f + stomp,
            s * 17f, 50f + stomp, s * 14f, 32f)
        kantoPolygon(0xFF8F9295, s * 20f, 28f, s * 46f, 27f + stomp,
            s * 48f, 44f + stomp, s * 41f, 54f + stomp,
            s * 18f, 52f + stomp, s * 13f, 42f)
        for (i in 0..2) kantoPolygon(0xFFD7D7CB, s * (18f + i * 10f), 45f + stomp,
            s * (27f + i * 10f), 45f + stomp, s * (28f + i * 10f), 57f + stomp)
        kantoOval(0xFFFFBB58, s * 29f - 5f, 17f, 10f, 8f)
    }
    kantoPolygon(0xFFA8A49D, -35f, -16f, -20f, -28f, 0f, -24f, 22f, -29f,
        36f, -13f, 32f, 15f, 17f, 31f, -17f, 31f, -32f, 15f)
    kantoPolygon(0xFF6C4B46, -25f, -10f, -8f, -7f, 0f, -12f, 8f, -7f,
        25f, -10f, 27f, 12f, 13f, 23f, -13f, 23f, -27f, 12f)
    kantoPolygon(0xFFFFD87B, -23f, -4f, -7f, 0f, -20f, 6f)
    kantoPolygon(0xFFFFD87B, 23f, -4f, 7f, 0f, 20f, 6f)
    kantoEyes(0f, 1f, 17f, 2.5f)
    kantoLine(0xFFFFAB54, 4f, -19f, 15f, -8f, 18f, 0f, 14f, 8f, 18f, 19f, 15f)
    for (s in listOf(-1f, 1f)) kantoPolygon(0xFFDDD9C7, s * 17f, 14f,
        s * 11f, 15f, s * 14f, 24f)
}

private fun DrawScope.megaLateDarkrai(m: Float) {
    megaLateForm(0xFF373448) {
        moveTo(-16f, 1f)
        cubicTo(-45f, 15f, -31f, 38f, -61f, 55f + m * 4f)
        lineTo(-32f, 48f)
        lineTo(-38f, 63f)
        lineTo(-9f, 43f)
        lineTo(0f, 62f)
        lineTo(16f, 40f)
        lineTo(44f, 57f)
        lineTo(34f, 40f)
        lineTo(67f, 49f - m * 5f)
        cubicTo(28f, 25f, 50f, 11f, 16f, -1f)
    }
    megaLateCurve(0xFF635477, 3f) {
        moveTo(-9f, 12f)
        quadraticTo(-10f, 39f, -35f, 49f)
    }
    megaLateCurve(0xFF635477, 3f) {
        moveTo(15f, 9f)
        quadraticTo(12f, 31f, 37f, 43f)
    }
    for (s in listOf(-1f, 1f)) {
        val sweep = m * s * 7f
        megaLateForm(0xFF2E3042) {
            moveTo(s * 15f, -24f)
            cubicTo(s * 42f, -35f, s * 44f, -10f + sweep, s * 56f, -4f + sweep)
            lineTo(s * 84f, -11f + sweep)
            lineTo(s * 72f, 2f + sweep)
            lineTo(s * 96f, 10f + sweep)
            lineTo(s * 73f, 13f + sweep)
            lineTo(s * 86f, 27f + sweep)
            lineTo(s * 61f, 18f + sweep)
            lineTo(s * 55f, 28f + sweep)
            cubicTo(s * 41f, 12f + sweep, s * 20f, 6f, s * 12f, -6f)
        }
        kantoLine(0xFF65506D, 2f, s * 28f, -13f, s * 46f, 4f + sweep,
            s * 66f, 10f + sweep)
    }
    megaLateForm(0xFF454051) {
        moveTo(-17f, -31f)
        quadraticTo(0f, -39f, 17f, -31f)
        lineTo(25f, -6f)
        quadraticTo(15f, 21f, 0f, 30f)
        quadraticTo(-25f, 9f, -25f, -6f)
    }
    kantoPolygon(0xFF302C41, -8f, -14f, 10f, -16f, 16f, 2f, 0f, 21f, -10f, 3f)
    kantoPolygon(0xFFAE3756, -8f, -25f, -21f, -44f, -31f, -36f,
        -42f, -50f, -38f, -25f, -25f, -10f, -12f, -6f, 0f, -14f,
        12f, -6f, 25f, -10f, 38f, -25f, 42f, -50f, 31f, -36f, 21f, -44f, 8f, -25f)
    kantoLine(0xFFE86578, 3f, -30f, -30f, -20f, -18f, -11f, -17f)
    kantoLine(0xFFE86578, 3f, 30f, -30f, 20f, -18f, 11f, -17f)
    kantoOval(0xFF343744, -18f, -52f, 36f, 29f)
    megaLateForm(0xFFE5EBE8) {
        moveTo(-19f, -36f)
        quadraticTo(-37f, -54f, -13f, -65f)
        cubicTo(3f, -73f, 30f, -61f, 38f + m * 5f, -75f)
        quadraticTo(39f + m * 5f, -51f, 16f, -48f)
        lineTo(2f, -32f)
        lineTo(-6f, -40f)
        lineTo(-17f, -27f)
    }
    megaLateCurve(0xFFB9CCD1, 3f) {
        moveTo(-15f, -53f)
        quadraticTo(3f, -62f, 22f, -59f)
    }
    kantoPolygon(0xFF81D8E2, 3f, -39f, 15f, -44f, 12f, -33f, 4f, -31f)
    kantoLine(0xFFEFFDFC, 2f, 8f, -37f, 12f, -39f)
}

private fun DrawScope.megaLateMagearna(m: Float) {
    kantoOval(0xFF907C67, -17f, 42f, 13f, 21f)
    kantoOval(0xFF907C67, 4f, 42f, 13f, 21f)
    kantoOval(0xFFD4C2A6, -23f, 55f, 20f, 9f)
    kantoOval(0xFFD4C2A6, 3f, 55f, 20f, 9f)
    megaLateForm(0xFF92868D) {
        moveTo(-13f, -3f)
        quadraticTo(-43f, 3f, -57f, 38f)
        quadraticTo(-32f, 62f, 0f, 54f)
        quadraticTo(32f, 62f, 57f, 38f)
        quadraticTo(43f, 3f, 13f, -3f)
    }
    for (i in -2..2) {
        val x = i * 19f
        val sway = m * i * 1.4f
        megaLateForm(if (i % 2 == 0) 0xFFE6DCD3 else 0xFFC6B8B1) {
            moveTo(i * 5f - 7f, -1f)
            quadraticTo(x - 18f, 13f, x - 11f + sway, 42f)
            quadraticTo(x + sway, 60f - kotlin.math.abs(i) * 5f, x + 11f + sway, 42f)
            quadraticTo(x + 18f, 13f, i * 5f + 7f, -1f)
        }
        kantoLine(0xFFD5B475, 3f, x - 8f + sway, 39f, x + sway, 46f,
            x + 8f + sway, 39f)
        kantoOval(0xFF9B8B8C, x - 3f + sway, 27f, 6f, 7f)
    }
    for (s in listOf(-1f, 1f)) {
        val arm = m * s * 7f
        kantoOval(0xFFD2AF70, s * 27f - 12f, -27f, 24f, 24f)
        for (i in 0..3) {
            withTransform({ rotate(i * 45f, Offset(s * 27f, -15f)) }) {
                kantoPolygon(0xFFBD995F, s * 27f - 3f, -31f, s * 27f + 3f, -31f,
                    s * 27f + 3f, -24f, s * 27f - 3f, -24f)
            }
        }
        kantoOval(0xFFEEE0BC, s * 27f - 5f, -20f, 10f, 10f)
        kantoLine(0xFF9C8974, 6f, s * 33f, -11f, s * 49f, -1f + arm)
        kantoOval(0xFFDDBA76, s * 49f - 5f, -6f + arm, 10f, 10f)
        kantoPolygon(0xFFDCD4CD, s * 48f, -1f + arm, s * 58f, -17f + arm,
            s * 67f, -14f + arm, s * 64f, 5f + arm, s * 53f, 10f + arm)
        kantoPolygon(0xFFCAB99D, s * 58f, -16f + arm, s * 63f, -26f + arm,
            s * 72f, -20f + arm, s * 69f, -10f + arm)
    }
    kantoPolygon(0xFFD2C8C0, -19f, -27f, 19f, -27f, 17f, -5f,
        9f, 7f, -9f, 7f, -17f, -5f)
    kantoOval(0xFFB59B70, -13f, -24f, 26f, 26f)
    kantoOval(0xFF64556B, -9f, -20f, 18f, 18f)
    kantoPolygon(0xFFF0AAC2, -6f, -13f, -3f, -17f, 0f, -14f,
        3f, -17f, 6f, -13f, 0f, -6f)
    kantoPolygon(0xFFE1D7CB, -15f, -43f, -25f, -69f, -15f, -73f,
        -3f, -49f, 3f, -49f, 15f, -73f, 25f, -69f, 15f, -43f)
    kantoLine(0xFFB6966B, 3f, -17f, -65f, -10f, -50f)
    kantoLine(0xFFB6966B, 3f, 17f, -65f, 10f, -50f)
    kantoOval(0xFFEAE0D4, -22f, -52f, 44f, 32f)
    kantoPolygon(0xFFD9B779, -23f, -39f, -14f, -46f, -6f, -37f,
        0f, -43f, 6f, -37f, 14f, -46f, 23f, -39f, 17f, -30f, -17f, -30f)
    kantoOval(0xFFE26F91, -15f, -39f, 9f, 11f)
    kantoOval(0xFFE26F91, 6f, -39f, 9f, 11f)
    kantoOval(0xFFFFECE9, -13f, -38f, 3f, 4f)
    kantoOval(0xFFFFECE9, 8f, -38f, 3f, 4f)
    kantoSmile(0f, -26f, 8f)
}

private fun DrawScope.megaLateZeraora(m: Float) {
    megaLateForm(0xFFE0B73B) {
        moveTo(10f, -30f)
        cubicTo(31f, -57f, 66f, -22f, 83f, -38f + m * 5f)
        lineTo(73f, -11f + m * 5f)
        lineTo(94f, -17f + m * 5f)
        lineTo(78f, 7f + m * 5f)
        lineTo(92f, 12f + m * 5f)
        lineTo(61f, 29f)
        lineTo(69f, 41f)
        lineTo(39f, 29f)
        quadraticTo(47f, 6f, 23f, -3f)
    }
    kantoPolygon(0xFFFAE16D, 28f, -24f, 64f, -17f, 56f, -7f, 75f, 1f,
        52f, 10f, 58f, 21f, 37f, 13f)
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 5f
        kantoPolygon(0xFFD9B63E, s * 7f, 13f, s * 27f, 21f,
            s * 34f, 41f + step, s * 46f, 55f + step, s * 24f, 58f + step,
            s * 12f, 40f, s * 4f, 32f)
        kantoPolygon(0xFF354858, s * 19f, 33f, s * 31f, 31f,
            s * 35f, 44f + step, s * 26f, 49f + step)
        kantoPolygon(0xFF8BCED4, s * 25f, 52f + step, s * 41f, 51f + step,
            s * 47f, 59f + step, s * 24f, 60f + step)
    }
    megaLateForm(0xFFF0CD4E) {
        moveTo(-18f, -26f)
        lineTo(17f, -29f)
        quadraticTo(26f, -7f, 15f, 13f)
        lineTo(24f, 27f)
        lineTo(11f, 23f)
        lineTo(0f, 35f)
        lineTo(-10f, 25f)
        lineTo(-24f, 29f)
        lineTo(-17f, 11f)
        quadraticTo(-28f, -8f, -18f, -26f)
    }
    kantoPolygon(0xFF314957, -19f, -22f, -4f, -12f, 0f, -19f, 5f, -12f,
        19f, -22f, 16f, -3f, 0f, 16f, -16f, -3f)
    kantoPolygon(0xFF83DBDD, -5f, -9f, 5f, -9f, 0f, 4f)
    for (s in listOf(-1f, 1f)) {
        val swipe = m * s * 7f
        kantoPolygon(0xFFEBC949, s * 18f, -19f, s * 40f, -13f + swipe,
            s * 48f, 2f + swipe, s * 29f, 10f + swipe, s * 15f, -4f)
        kantoPolygon(0xFF344B59, s * 36f, -8f + swipe, s * 57f, -10f + swipe,
            s * 70f, 10f + swipe, s * 57f, 27f + swipe, s * 35f, 16f + swipe)
        kantoPolygon(0xFF55BCCA, s * 46f, -4f + swipe, s * 57f, -3f + swipe,
            s * 63f, 8f + swipe, s * 54f, 17f + swipe, s * 43f, 11f + swipe)
        for (i in 0..2) kantoPolygon(0xFFCFEEF0, s * (46f + i * 8f), 15f + swipe,
            s * (52f + i * 8f), 12f + swipe, s * (56f + i * 8f), 31f + swipe)
        kantoPolygon(0xFFF7D75D, s * 33f, -9f + swipe, s * 28f, -25f + swipe,
            s * 44f, -16f + swipe, s * 49f, -23f + swipe, s * 53f, -9f + swipe)
    }
    kantoPolygon(0xFF354858, -18f, -40f, -31f, -71f, -10f, -60f,
        0f, -43f, 13f, -60f, 30f, -71f, 19f, -39f)
    kantoPolygon(0xFFF1CE4E, -25f, -63f, -13f, -56f, -10f, -44f)
    kantoPolygon(0xFFF1CE4E, 24f, -63f, 13f, -56f, 10f, -44f)
    kantoPolygon(0xFFF3D458, -24f, -49f, -9f, -55f, 0f, -67f,
        9f, -55f, 24f, -49f, 25f, -33f, 35f, -23f, 18f, -27f,
        11f, -17f, 0f, -22f, -12f, -17f, -18f, -27f, -35f, -23f, -25f, -33f)
    kantoPolygon(0xFF344957, -23f, -42f, -5f, -36f, -12f, -28f, -22f, -32f)
    kantoPolygon(0xFF344957, 23f, -42f, 5f, -36f, 12f, -28f, 22f, -32f)
    kantoLine(0xFF9CECEF, 3f, -19f, -36f, -11f, -34f)
    kantoLine(0xFF9CECEF, 3f, 19f, -36f, 11f, -34f)
    kantoPolygon(0xFF42515A, -4f, -28f, 4f, -28f, 0f, -24f)
}
