package com.ssmath.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawMegaEarly(celebration: Celebration, motion: Float) {
    val m = motion.coerceIn(-1f, 1f)
    when (celebration) {
        Celebration.MEGA_MEGANIUM -> megaEarlyMeganium(m)
        Celebration.MEGA_EMBOAR -> megaEarlyEmboar(m)
        Celebration.MEGA_FERALIGATR -> megaEarlyFeraligatr(m)
        Celebration.MEGA_BEEDRILL -> megaEarlyBeedrill(m)
        Celebration.MEGA_PIDGEOT -> megaEarlyPidgeot(m)
        Celebration.MEGA_AMPHAROS -> megaEarlyAmpharos(m)
        Celebration.MEGA_GYARADOS -> megaEarlyGyarados(m)
        Celebration.MEGA_BARBARACLE -> megaEarlyBarbaracle(m)
        Celebration.MEGA_STARMIE -> megaEarlyStarmie(m)
        Celebration.MEGA_FLOETTE -> megaEarlyFloette(m)
        Celebration.MEGA_MEOWSTIC -> megaEarlyMeowstic(m)
        Celebration.MEGA_PYROAR -> megaEarlyPyroar(m)
        Celebration.MEGA_RAICHU_X -> megaEarlyRaichuX(m)
        Celebration.MEGA_RAICHU_Y -> megaEarlyRaichuY(m)
        Celebration.MEGA_CLEFABLE -> megaEarlyClefable(m)
        Celebration.MEGA_ALAKAZAM -> megaEarlyAlakazam(m)
        Celebration.MEGA_GENGAR -> megaEarlyGengar(m)
        Celebration.MEGA_SCOLIPEDE -> megaEarlyScolipede(m)
        Celebration.MEGA_VICTREEBEL -> megaEarlyVictreebel(m)
        Celebration.MEGA_MEDICHAM -> megaEarlyMedicham(m)
        Celebration.MEGA_MANECTRIC -> megaEarlyManectric(m)
        Celebration.MEGA_GARDEVOIR -> megaEarlyGardevoir(m)
        Celebration.MEGA_GALLADE -> megaEarlyGallade(m)
        Celebration.MEGA_HOUNDOOM -> megaEarlyHoundoom(m)
        Celebration.MEGA_ALTARIA -> megaEarlyAltaria(m)
        Celebration.MEGA_AUDINO -> megaEarlyAudino(m)
        Celebration.MEGA_LOPUNNY -> megaEarlyLopunny(m)
        Celebration.MEGA_BANETTE -> megaEarlyBanette(m)
        Celebration.MEGA_CAMERUPT -> megaEarlyCamerupt(m)
        Celebration.MEGA_EXCADRILL -> megaEarlyExcadrill(m)
        Celebration.MEGA_GARCHOMP -> megaEarlyGarchomp(m)
        Celebration.MEGA_GARCHOMP_Z -> megaEarlyGarchompZ(m)
        else -> error("No early Mega artwork for $celebration")
    }
}

private fun DrawScope.megaEarlyShape(color: Long, contour: Path.() -> Unit) {
    val path = Path().apply { contour(); close() }
    drawPath(path, Color(color))
    drawPath(path, Color(KANTO_INK), style = Stroke(2f, join = StrokeJoin.Round))
}

private fun DrawScope.megaEarlyShade(color: Long, contour: Path.() -> Unit) {
    drawPath(Path().apply { contour(); close() }, Color(color))
}

private fun DrawScope.megaEarlyCurve(color: Long, width: Float, contour: Path.() -> Unit) {
    drawPath(Path().apply(contour), Color(color),
        style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.megaEarlyMeganium(m: Float) {
    val tail = m * 5f
    megaEarlyShape(0xFF78B75A) {
        moveTo(39f, 9f); cubicTo(65f, 14f, 74f, -4f + tail, 89f, -9f + tail)
        quadraticTo(87f, 19f + tail, 50f, 33f); lineTo(34f, 24f)
    }
    megaEarlyShape(0xFF589649) {
        moveTo(11f, 28f); lineTo(28f, 27f); lineTo(26f + m * 2f, 59f)
        lineTo(6f + m * 2f, 59f); quadraticTo(3f, 45f, 11f, 28f)
    }
    megaEarlyShape(0xFF8BCB65) {
        moveTo(-23f, -8f); cubicTo(-5f, -20f, 42f, -13f, 50f, 8f)
        cubicTo(66f, 35f, 37f, 46f, 8f, 37f); lineTo(-14f, 26f)
        quadraticTo(-33f, 22f, -23f, -8f)
    }
    megaEarlyShade(0xFFAEDD79) {
        moveTo(-8f, -5f); cubicTo(12f, -18f, 41f, -8f, 45f, 7f)
        cubicTo(23f, -2f, 13f, 12f, -4f, 11f)
    }
    for (x in listOf(-15f, 38f)) {
        val step = if (x < 0f) m * 3f else -m * 3f
        megaEarlyShape(0xFF89C765) {
            moveTo(x - 9f, 19f); quadraticTo(x + 12f, 16f, x + 10f, 37f)
            lineTo(x + 13f + step, 59f); quadraticTo(x, 64f, x - 13f + step, 59f)
            lineTo(x - 9f, 38f)
        }
        for (toe in 0..2) kantoOval(0xFFFFF7DF, x - 9f + toe * 7f + step, 55f, 5f, 6f)
    }
    // Four overlapping corollas conceal the elongated neck, not a single standard collar.
    val petals = listOf(0xFFFFF0E0, 0xFFF6ACBC, 0xFFE76A94, 0xFFCC426D)
    for (tier in 0..3) {
        val y = 8f - tier * 16f
        val spread = 34f - tier * 3f
        val sway = m * (tier + 1)
        megaEarlyShape(petals[tier]) {
            moveTo(-33f, y - 14f)
            cubicTo(-33f - spread + sway, y - 28f, -40f - spread, y + 5f, -31f, y + 8f)
            cubicTo(-28f, y + 29f, 2f, y + 23f, 0f + sway, y + 5f)
            cubicTo(30f + sway, y + 10f, 27f, y - 18f, -4f, y - 16f)
            quadraticTo(-16f, y - 26f, -33f, y - 14f)
        }
        megaEarlyCurve(0xFFFFD8D1, 2f) {
            moveTo(-44f, y - 5f); quadraticTo(-31f, y, -22f, y + 10f)
            moveTo(-13f, y + 7f); quadraticTo(0f, y - 3f, 11f, y - 5f)
        }
    }
    megaEarlyShape(0xFFA4D777) {
        moveTo(-37f, -62f); cubicTo(-23f, -74f, -3f, -61f, -7f, -46f)
        quadraticTo(-14f, -30f, -36f, -38f); cubicTo(-56f, -39f, -56f, -57f, -37f, -62f)
    }
    megaEarlyCurve(0xFFDEB856, 4f) {
        moveTo(-28f, -61f); cubicTo(-45f, -77f, -55f, -72f + m, -43f, -66f)
        moveTo(-14f, -61f); cubicTo(-9f, -76f, 10f, -73f - m, 3f, -64f)
    }
    kantoEyes(-31f, -51f, 13f, 3f)
    kantoSmile(-37f, -42f, 11f)
}

private fun DrawScope.megaEarlyEmboar(m: Float) {
    val swing = m * 5f
    megaEarlyShape(0xFFEF683B) {
        moveTo(-44f, -15f); cubicTo(-69f, -26f, -49f, -48f, -63f, -61f + swing)
        quadraticTo(-34f, -58f, -31f, -68f); lineTo(-19f, -58f)
        quadraticTo(-3f, -79f, 8f, -62f); lineTo(25f, -73f)
        quadraticTo(30f, -55f, 51f, -63f - swing)
        cubicTo(44f, -37f, 76f, -24f, 48f, 8f); lineTo(-40f, 14f)
    }
    megaEarlyShade(0xFFFFCD62) {
        moveTo(-39f, -7f); quadraticTo(-53f, -34f, -39f, -47f)
        lineTo(-33f, -37f); lineTo(-14f, -61f); lineTo(-9f, -46f)
        lineTo(17f, -62f); lineTo(18f, -43f)
        quadraticTo(53f, -51f, 48f, -13f); lineTo(27f, 14f)
    }
    for (side in listOf(-1f, 1f)) {
        val lift = side * m * 3f
        megaEarlyShape(0xFF454148) {
            moveTo(side * 12f, 30f); lineTo(side * 36f, 30f)
            lineTo(side * 40f, 56f + lift); quadraticTo(side * 26f, 65f + lift, side * 10f, 56f + lift)
        }
        kantoLine(0xFFDC7543, 4f, side * 15f, 47f + lift, side * 34f, 47f + lift)
        megaEarlyShape(0xFFDD744B) {
            moveTo(side * 27f, -12f); quadraticTo(side * 64f, -25f + side * swing, side * 69f, 11f + side * swing)
            lineTo(side * 50f, 24f + side * swing); quadraticTo(side * 34f, 12f, side * 27f, -12f)
        }
        kantoPolygon(0xFF393C45, side * 53f, 1f + side * swing, side * 73f, 7f + side * swing,
            side * 76f, 25f + side * swing, side * 62f, 33f + side * swing, side * 49f, 23f + side * swing)
        kantoLine(0xFFF3B753, 4f, side * 52f, 8f + side * swing, side * 72f, 14f + side * swing)
        kantoLine(KANTO_INK, 2f, side * 62f, 23f + side * swing, side * 67f, 29f + side * swing)
    }
    megaEarlyShape(0xFF454148) {
        moveTo(-28f, -19f); cubicTo(-52f, -5f, -45f, 38f, -22f, 45f)
        cubicTo(6f, 54f, 41f, 38f, 41f, 13f); quadraticTo(42f, -22f, 15f, -20f)
    }
    kantoPolygon(0xFFD98C43, -36f, 16f, 37f, 16f, 35f, 29f, -35f, 29f)
    megaEarlyCurve(0xFFFFCE61, 3f) {
        moveTo(-23f, 26f); lineTo(-23f, 19f); lineTo(-10f, 19f); lineTo(-10f, 25f)
        moveTo(4f, 26f); lineTo(4f, 19f); lineTo(20f, 19f); lineTo(20f, 26f)
    }
    kantoPolygon(0xFFE78F58, -25f, -31f, -33f, -53f, -12f, -44f, 8f, -44f,
        28f, -54f, 24f, -28f, 18f, -10f, -14f, -10f)
    kantoOval(0xFFEEAD75, -18f, -29f, 37f, 22f)
    kantoEyes(0f, -34f, 14f, 3.6f)
    kantoOval(0xFF8D4946, -9f, -23f, 7f, 6f)
    kantoOval(0xFF8D4946, 5f, -23f, 7f, 6f)
    kantoPolygon(0xFFFFF0CC, -18f, -12f, -27f, -28f, -25f, -7f, -12f, -2f)
    kantoPolygon(0xFFFFF0CC, 18f, -12f, 27f, -28f, 25f, -7f, 12f, -2f)
    kantoPolygon(0xFFFFCB59, -19f, -5f, -10f, 11f, -2f, 5f, 8f, 13f, 21f, -5f, 2f, 0f)
}

private fun DrawScope.megaEarlyFeraligatr(m: Float) {
    val jaw = m * 4f
    megaEarlyShape(0xFF407DAB) {
        moveTo(29f, 21f); cubicTo(63f, 37f, 82f, 30f, 91f, 10f + m * 4f)
        quadraticTo(99f, 43f, 59f, 51f); lineTo(25f, 40f)
    }
    kantoPolygon(0xFFDD5356, 60f, 26f, 60f, 6f + m * 3f, 70f, 19f, 80f, 0f + m * 3f, 83f, 27f)
    // The dorsal fin folds into a second enormous crocodilian jaw.
    megaEarlyShape(0xFF436D9E) {
        moveTo(-22f, -17f); lineTo(-51f, -43f - jaw); lineTo(-58f, -65f - jaw)
        quadraticTo(-5f, -77f, 51f, -54f - jaw); lineTo(63f, -30f); lineTo(42f, 9f)
    }
    megaEarlyShape(0xFFCD505E) {
        moveTo(-47f, -47f - jaw); quadraticTo(-2f, -63f - jaw, 49f, -41f - jaw)
        lineTo(33f, -12f); quadraticTo(-3f, -20f, -47f, -47f - jaw)
    }
    for (i in 0..5) {
        val x = -40f + i * 15f
        val y = -48f + (i - 2f) * (i - 2f) * 0.9f - jaw
        kantoPolygon(0xFFFFF1CA, x, y, x + 9f, y + 1f, x + 5f, y + 12f)
    }
    megaEarlyShape(0xFF65ADCA) {
        moveTo(-27f, -18f); cubicTo(-50f, 2f, -35f, 47f, -7f, 48f)
        cubicTo(23f, 56f, 49f, 31f, 37f, 1f); lineTo(14f, -23f)
    }
    kantoPolygon(0xFFF3D989, -26f, 8f, -8f, 13f, 2f, 32f, 18f, 10f, 30f, 10f, 15f, 39f, -10f, 39f)
    for (side in listOf(-1f, 1f)) {
        val step = side * m * 3f
        megaEarlyShape(0xFF63A8C5) {
            moveTo(side * 19f, 28f); quadraticTo(side * 49f, 22f, side * 46f, 48f + step)
            lineTo(side * 53f, 58f + step); lineTo(side * 20f, 59f + step)
            quadraticTo(side * 11f, 45f, side * 19f, 28f)
        }
        for (toe in 0..2) kantoPolygon(0xFFFFF3DB, side * (23f + toe * 10f), 53f + step,
            side * (27f + toe * 10f), 61f + step, side * (32f + toe * 10f), 53f + step)
        megaEarlyShape(0xFF77BBD0) {
            moveTo(side * 27f, -9f); quadraticTo(side * 49f, -14f + step, side * 59f, 5f + step)
            lineTo(side * 52f, 20f + step); lineTo(side * 40f, 13f + step)
            quadraticTo(side * 38f, 4f, side * 23f, 6f)
        }
        for (finger in 0..2) kantoPolygon(0xFFFFF3DB, side * (46f + finger * 5f), 13f + step,
            side * (49f + finger * 5f), 26f + step, side * (51f + finger * 5f), 14f + step)
    }
    megaEarlyShape(0xFF80C6D7) {
        moveTo(-24f, -39f); lineTo(-5f, -42f); lineTo(17f, -32f)
        lineTo(24f, -12f); quadraticTo(0f, 3f, -33f, -9f)
        quadraticTo(-44f, -19f, -37f, -29f); lineTo(-26f, -28f)
    }
    kantoPolygon(0xFFDE565B, -10f, -39f, -3f, -53f, 6f, -37f, 17f, -45f, 18f, -30f)
    kantoEyes(-9f, -27f, 15f, 3.5f)
    kantoLine(KANTO_INK, 2f, -34f, -15f, -18f, -10f, 17f, -14f)
    kantoPolygon(0xFFFFF4D7, -25f, -12f, -20f, -4f, -15f, -11f)
    kantoPolygon(0xFFFFF4D7, 5f, -12f, 10f, -5f, 14f, -13f)
}

private fun DrawScope.megaEarlyBeedrill(m: Float) {
    val beat = m * 7f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFE6F1F2) {
            moveTo(side * 10f, -12f)
            cubicTo(side * 20f, -47f, side * 63f, -72f + beat, side * 73f, -61f + beat)
            cubicTo(side * 83f, -40f + beat, side * 42f, -19f, side * 10f, -12f)
        }
        megaEarlyShape(0xFFD0E4EC) {
            moveTo(side * 12f, -6f)
            cubicTo(side * 43f, -28f, side * 72f, -25f - beat, side * 72f, -10f - beat)
            quadraticTo(side * 69f, 14f - beat, side * 15f, 10f)
        }
        megaEarlyCurve(0xFF9EBACD, 1.4f) {
            moveTo(side * 18f, -13f); quadraticTo(side * 43f, -35f, side * 65f, -54f + beat)
            moveTo(side * 19f, -3f); lineTo(side * 61f, -11f - beat)
        }
    }
    kantoPolygon(0xFFEECA53, -10f, 14f, 10f, 14f, 19f, 32f, 0f, 62f, -18f, 32f)
    kantoPolygon(0xFF44424F, -13f, 19f, 13f, 19f, 18f, 29f, -17f, 29f)
    kantoPolygon(0xFF44424F, -13f, 39f, 13f, 39f, 0f, 62f)
    megaEarlyShade(0xFFFFE67E) {
        moveTo(-10f, 15f); lineTo(-1f, 14f); lineTo(6f, 33f); lineTo(-2f, 49f); lineTo(-8f, 31f)
    }
    kantoOval(0xFF525565, -13f, -13f, 26f, 35f)
    for (side in listOf(-1f, 1f)) {
        val thrust = side * m * 5f
        kantoLine(KANTO_INK, 8f, side * 12f, -4f, side * 29f, 5f + thrust, side * 40f, -6f + thrust)
        kantoLine(0xFFECC551, 5f, side * 12f, -4f, side * 29f, 5f + thrust, side * 40f, -6f + thrust)
        kantoPolygon(0xFFF1F2E7, side * 36f, -11f + thrust, side * 51f, -7f + thrust,
            side * 85f, 34f + thrust, side * 43f, 7f + thrust)
        kantoLine(0xFFBCC8D0, 2f, side * 44f, -3f + thrust, side * 80f, 30f + thrust)
        kantoPolygon(0xFFE4D8A7, side * 9f, 17f, side * 25f, 21f - thrust,
            side * 49f, 49f - thrust, side * 21f, 30f - thrust)
        kantoLine(KANTO_INK, 3f, side * 10f, -36f, side * 16f, -53f, side * 28f, -61f + thrust)
    }
    megaEarlyShape(0xFFF3CF58) {
        moveTo(-17f, -37f); quadraticTo(0f, -50f, 18f, -35f)
        lineTo(22f, -19f); lineTo(8f, -8f); lineTo(-8f, -8f); lineTo(-23f, -23f)
    }
    kantoOval(0xFFD44967, -21f, -32f, 16f, 21f)
    kantoOval(0xFFD44967, 5f, -32f, 16f, 21f)
    kantoLine(0xFFFFB7BB, 3f, -16f, -27f, -14f, -21f)
    kantoLine(0xFFFFB7BB, 3f, 10f, -27f, 12f, -21f)
    kantoPolygon(0xFFEEEBCF, -5f, -13f, 0f, -4f, 5f, -13f)
}

private fun DrawScope.megaEarlyPidgeot(m: Float) {
    val wing = m * 8f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFC89A63) {
            moveTo(side * 15f, -15f); quadraticTo(side * 38f, -35f, side * 77f, -58f + wing)
            lineTo(side * 99f, -66f + wing); lineTo(side * 86f, -37f + wing)
            lineTo(side * 95f, -42f + wing); lineTo(side * 80f, -15f + wing)
            lineTo(side * 87f, -20f + wing); lineTo(side * 69f, 4f + wing)
            lineTo(side * 73f, 3f + wing); lineTo(side * 49f, 23f + wing)
            quadraticTo(side * 26f, 26f, side * 15f, -15f)
        }
        megaEarlyShape(0xFF4A7DAD) {
            moveTo(side * 76f, -48f + wing); lineTo(side * 99f, -66f + wing)
            lineTo(side * 86f, -37f + wing); lineTo(side * 95f, -42f + wing)
            lineTo(side * 80f, -15f + wing); lineTo(side * 87f, -20f + wing)
            lineTo(side * 69f, 4f + wing); lineTo(side * 73f, 3f + wing)
            lineTo(side * 53f, 19f + wing); lineTo(side * 64f, -9f + wing)
        }
        megaEarlyCurve(0xFFF2D4A1, 3f) {
            moveTo(side * 22f, -12f); quadraticTo(side * 38f, -30f, side * 73f, -43f + wing)
        }
        for (i in 0..2) kantoLine(KANTO_INK, 1.5f,
            side * (41f + i * 9f), -7f - i * 9f + wing,
            side * (51f + i * 12f), 13f - i * 13f + wing)
    }
    kantoPolygon(0xFFB76747, -14f, 29f, -33f - m * 3f, 58f, -5f, 52f, 5f, 63f,
        16f, 50f, 34f + m * 3f, 56f, 18f, 25f)
    kantoLine(0xFFEDB669, 3f, -3f, 33f, -14f, 51f)
    kantoLine(0xFFEDB669, 3f, 6f, 34f, 21f, 49f)
    megaEarlyShape(0xFFF1DDB1) {
        moveTo(-16f, -21f); cubicTo(-35f, -3f, -22f, 36f, 0f, 41f)
        cubicTo(30f, 32f, 34f, -4f, 15f, -22f)
    }
    megaEarlyShade(0xFFD9BA88) {
        moveTo(12f, -10f); quadraticTo(33f, 10f, 8f, 35f)
        lineTo(-1f, 36f); quadraticTo(19f, 11f, 12f, -10f)
    }
    for (side in listOf(-1f, 1f)) {
        kantoLine(0xFFAB6A55, 4f, side * 10f, 32f, side * 15f, 44f - side * m * 3f,
            side * 25f, 45f - side * m * 3f)
        kantoLine(0xFFAB6A55, 3f, side * 15f, 43f - side * m * 3f, side * 12f, 49f - side * m * 3f)
    }
    kantoOval(0xFFF3DCAD, -23f, -43f, 46f, 38f)
    megaEarlyShape(0xFFD34B57) {
        moveTo(-20f, -36f); quadraticTo(-2f, -67f, 24f, -49f)
        cubicTo(48f, -34f, 59f, -58f + m * 5f, 77f, -54f + m * 5f)
        cubicTo(63f, -23f, 25f, -16f, 5f, -37f); lineTo(-8f, -25f)
    }
    megaEarlyShape(0xFFF2C655) {
        moveTo(-13f, -39f); quadraticTo(-4f, -57f, 14f, -46f)
        quadraticTo(30f, -32f, 51f, -39f + m * 4f)
        quadraticTo(29f, -22f, 7f, -34f); lineTo(-5f, -27f)
    }
    kantoPolygon(0xFF51414B, -21f, -25f, -4f, -20f, -13f, -12f)
    kantoPolygon(0xFF51414B, 21f, -25f, 4f, -20f, 13f, -12f)
    kantoEyes(0f, -21f, 13f, 3f)
    kantoPolygon(0xFFD89A61, -7f, -17f, 7f, -17f, 0f, -4f)
}

private fun DrawScope.megaEarlyAmpharos(m: Float) {
    val curl = m * 5f
    megaEarlyCurve(KANTO_INK, 13f) {
        moveTo(18f, 29f); cubicTo(59f, 43f, 42f, 9f, 68f, 5f + curl)
    }
    megaEarlyCurve(0xFFE3BC53, 9f) {
        moveTo(18f, 29f); cubicTo(59f, 43f, 42f, 9f, 68f, 5f + curl)
    }
    megaEarlyShape(0xFFF3F4EE) {
        moveTo(55f, 15f + curl); cubicTo(35f, -5f + curl, 55f, -17f, 70f, -9f + curl)
        cubicTo(91f, -27f, 99f, 3f + curl, 82f, 9f + curl)
        cubicTo(94f, 26f + curl, 65f, 41f, 55f, 15f + curl)
    }
    kantoOval(0xFFD95A68, 68f, -6f + curl, 17f, 17f)
    kantoOval(0xFFE27881, 51f, 11f + curl, 12f, 12f)
    kantoOval(0xFFF6A0A3, 73f, 23f + curl, 9f, 9f)
    for (side in listOf(-1f, 1f)) {
        val step = side * m * 3f
        megaEarlyShape(0xFFF0CF68) {
            moveTo(side * 6f, 29f); lineTo(side * 25f, 30f)
            lineTo(side * 31f, 58f + step); lineTo(side * 9f, 59f + step)
        }
        kantoLine(0xFF474852, 5f, side * 11f, 48f + step, side * 27f, 48f + step)
        megaEarlyShape(0xFFF0CF68) {
            moveTo(side * 16f, -7f); quadraticTo(side * 45f, -13f, side * 51f, 11f + step)
            quadraticTo(side * 45f, 22f + step, side * 33f, 9f + step)
            lineTo(side * 17f, 10f)
        }
    }
    megaEarlyShape(0xFFE8C35A) {
        moveTo(-14f, -27f); cubicTo(-21f, -6f, -35f, 20f, -19f, 40f)
        quadraticTo(4f, 54f, 26f, 36f); cubicTo(37f, 13f, 12f, -6f, 14f, -27f)
    }
    megaEarlyShape(0xFFFFF0CA) {
        moveTo(-7f, -2f); cubicTo(-24f, 13f, -16f, 41f, 4f, 41f)
        cubicTo(27f, 39f, 20f, 11f, 7f, -2f)
    }
    kantoLine(0xFF444553, 5f, -16f, -10f, 17f, -10f)
    kantoLine(0xFF444553, 5f, -17f, -1f, 18f, -1f)
    megaEarlyShape(0xFFF7F7EE) {
        moveTo(-20f, -31f); cubicTo(-42f, -29f, -45f, -55f, -24f, -57f)
        cubicTo(-21f, -77f, 3f, -76f, 12f, -64f)
        cubicTo(27f, -72f, 43f, -52f, 31f, -41f)
        cubicTo(50f, -24f, 45f + curl, -3f, 26f + curl, -5f)
        lineTo(29f + curl, 7f); quadraticTo(3f, -1f, 4f, -27f)
    }
    kantoPolygon(0xFFDDC15A, -17f, -43f, -34f, -58f, -37f, -46f, -24f, -30f)
    kantoPolygon(0xFFDDC15A, 13f, -44f, 31f, -57f, 33f, -44f, 23f, -31f)
    kantoLine(0xFF444553, 5f, -33f, -51f, -25f, -40f)
    kantoLine(0xFF444553, 5f, 29f, -51f, 22f, -40f)
    kantoOval(0xFFF4D56F, -23f, -47f, 43f, 33f)
    kantoOval(0xFFDF5E6A, -8f, -55f, 15f, 13f)
    kantoEyes(-3f, -32f, 12f, 3f)
    kantoSmile(-3f, -23f, 11f)
    megaEarlyCurve(0xFFCDDDE1, 2f) {
        moveTo(20f, -33f); quadraticTo(31f + curl, -25f, 28f + curl, -15f)
    }
}

private fun DrawScope.megaEarlyGyarados(m: Float) {
    val fin = m * 5f
    megaEarlyShape(0xFF345C85) {
        moveTo(17f, 10f); cubicTo(34f, 44f, 63f, 42f, 69f, 23f)
        quadraticTo(74f, 8f, 66f, 1f); lineTo(79f, -3f)
        cubicTo(100f, 29f, 77f, 58f, 46f, 51f)
        quadraticTo(7f, 56f, -5f, 25f)
    }
    kantoPolygon(0xFFCF5363, 69f, 11f, 55f, -5f - fin, 64f, -6f - fin,
        64f, -26f - fin, 78f, -14f - fin, 94f, -29f - fin, 90f, -3f, 80f, 15f)
    for (i in 0..2) kantoLine(0xFF99BDCF, 2f,
        43f + i * 12f, 41f, 40f + i * 13f, 49f - i * 2f)
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFF345F90) {
            moveTo(side * 14f, -28f); lineTo(side * 50f, -42f - fin)
            lineTo(side * 80f, -52f - fin); lineTo(side * 71f, -25f - fin)
            lineTo(side * 89f, -17f - fin); lineTo(side * 57f, -2f)
            lineTo(side * 65f, 19f + fin); lineTo(side * 26f, 15f)
        }
        kantoPolygon(0xFFE26A73, side * 24f, -18f, side * 80f, -52f - fin,
            side * 70f, -22f - fin, side * 80f, -17f - fin, side * 49f, -1f)
        megaEarlyCurve(0xFFBCD9DE, 2f) {
            moveTo(side * 24f, -13f); lineTo(side * 54f, -23f - fin)
            moveTo(side * 27f, 0f); lineTo(side * 57f, 11f + fin)
        }
    }
    megaEarlyShape(0xFF477A9F) {
        moveTo(-25f, -36f); cubicTo(-46f, -4f, -24f, 42f, 5f, 44f)
        cubicTo(35f, 38f, 43f, -4f, 22f, -34f)
    }
    megaEarlyShape(0xFFEBCF97) {
        moveTo(-16f, -7f); quadraticTo(-26f, 24f, 5f, 41f)
        quadraticTo(22f, 24f, 21f, -5f); lineTo(2f, 4f)
    }
    for (y in listOf(12f, 23f, 32f)) kantoLine(0xFF9E9279, 2f, -12f + y / 5f, y, 19f, y + 2f)
    kantoPolygon(0xFF41688D, -31f, -46f, -15f, -53f, -3f, -46f, 13f, -53f,
        32f, -39f, 22f, -16f, -18f, -14f, -34f, -28f)
    kantoPolygon(0xFF48799D, -17f, -48f, -28f, -67f, -5f, -58f,
        2f, -74f, 13f, -58f, 31f, -62f, 22f, -45f)
    kantoPolygon(0xFFF5E3B2, -23f, -29f, -14f, -12f, 14f, -12f, 26f, -31f, 14f, -23f, -12f, -23f)
    kantoOval(0xFF643E57, -14f, -22f, 29f, 24f)
    kantoPolygon(0xFFFFF7DE, -12f, -20f, -7f, -10f, -3f, -21f)
    kantoPolygon(0xFFFFF7DE, 5f, -21f, 10f, -10f, 14f, -20f)
    kantoEyes(0f, -36f, 18f, 3.5f)
    megaEarlyCurve(0xFFEDE2B6, 3f) {
        moveTo(-24f, -20f); cubicTo(-49f, -17f, -45f, 20f + fin, -62f, 20f + fin)
        moveTo(24f, -20f); cubicTo(50f, -20f, 40f, 13f - fin, 62f, 9f - fin)
    }
}

private fun DrawScope.megaEarlyBarbaracle(m: Float) {
    for (side in listOf(-1f, 1f)) {
        for (arm in 0..2) {
            val reach = m * side * (3f + arm)
            val x = side * (55f + arm * 9f)
            val y = -46f + arm * 26f + reach
            kantoLine(KANTO_INK, 14f, side * 18f, -19f + arm * 14f,
                side * (39f + arm * 5f), -24f + arm * 18f + reach, x, y)
            kantoLine(0xFFBC825C, 10f, side * 18f, -19f + arm * 14f,
                side * (39f + arm * 5f), -24f + arm * 18f + reach, x, y)
            kantoPolygon(0xFF817F82, x - 12f, y - 8f, x - 7f, y - 19f,
                x + 8f, y - 19f, x + 15f, y - 4f, x + 6f, y + 12f, x - 9f, y + 9f)
            kantoPolygon(0xFFDDD5BB, x - 12f, y - 8f, x - 19f, y - 20f,
                x - 8f, y - 16f, x - 5f, y - 7f)
            kantoPolygon(0xFFDDD5BB, x + 4f, y - 14f, x + 8f, y - 26f,
                x + 14f, y - 12f, x + 11f, y - 4f)
            kantoOval(0xFFFFDF6D, x - 7f, y - 4f, 13f, 9f)
            kantoLine(KANTO_INK, 2.5f, x, y - 3f, x, y + 2f)
        }
        val step = m * side * 3f
        kantoPolygon(0xFFB48360, side * 9f, 20f, side * 32f, 18f, side * 35f, 43f + step,
            side * 23f, 52f + step, side * 9f, 39f)
        kantoPolygon(0xFF777980, side * 17f, 42f + step, side * 40f, 43f + step,
            side * 48f, 58f + step, side * 30f, 62f + step, side * 13f, 56f + step)
        kantoPolygon(0xFFDDD5BB, side * 30f, 55f + step, side * 46f, 60f + step, side * 39f, 48f + step)
    }
    kantoPolygon(0xFF77797E, -27f, -23f, -7f, -35f, 23f, -25f, 36f, -5f,
        24f, 18f, 28f, 33f, -19f, 35f, -32f, 12f)
    kantoPolygon(0xFFA8A6A0, -25f, -21f, -6f, -31f, 13f, -23f, -4f, -7f, -27f, 2f)
    kantoPolygon(0xFF555B66, -4f, -7f, 13f, -23f, 33f, -5f, 20f, 17f, 3f, 8f)
    kantoLine(0xFFCCC5AE, 2f, -24f, 12f, -8f, 22f, 21f, 18f)
    kantoPolygon(0xFFBF946C, -13f, -28f, -18f, -46f, -8f, -58f, 10f, -57f,
        21f, -42f, 14f, -25f, 0f, -19f)
    kantoPolygon(0xFF888B8D, -18f, -42f, -23f, -66f, -12f, -59f, -3f, -73f,
        5f, -60f, 15f, -70f, 20f, -44f, 11f, -34f, -8f, -34f)
    kantoPolygon(0xFFFFE877, -14f, -40f, -4f, -43f, 1f, -37f, -9f, -34f)
    kantoPolygon(0xFFFFE877, 4f, -43f, 15f, -40f, 11f, -34f, 2f, -37f)
    kantoEyes(0f, -38f, 9f, 2.4f)
    kantoLine(KANTO_INK, 2f, -7f, -27f, 1f, -24f, 9f, -28f)
}

private fun DrawScope.megaEarlyStarmie(m: Float) {
    val stride = m * 7f
    kantoPolygon(0xFF7E66B7, -15f, 3f, -2f, 10f, -15f - stride, 33f,
        -22f + stride, 60f, -37f + stride, 62f, -29f - stride, 31f)
    kantoPolygon(0xFFAF83C7, 3f, 10f, 17f, 2f, 35f + stride, 29f,
        32f - stride, 60f, 17f - stride, 62f, 20f + stride, 33f)
    megaEarlyShade(0xFFD1A7E3) {
        moveTo(12f, 12f); lineTo(27f + stride, 31f); lineTo(24f - stride, 55f)
        lineTo(21f - stride, 58f); lineTo(20f + stride, 33f)
    }
    kantoLine(0xFFD7BBEA, 2f, -21f - stride, 32f, -27f + stride, 54f)
    kantoPolygon(0xFF746BAE, 0f, -54f, 14f, -34f, 40f, -44f, 29f, -18f,
        46f, 1f, 22f, 2f, 16f, 25f, 0f, 10f, -16f, 25f,
        -24f, 2f, -46f, 1f, -29f, -18f, -40f, -44f, -14f, -34f)
    megaEarlyShape(0xFFAB85C9) {
        moveTo(0f, -69f); lineTo(13f, -30f); lineTo(41f, -19f)
        lineTo(72f, -25f - stride); lineTo(46f, -5f - stride)
        lineTo(20f, -5f); lineTo(17f, 13f); lineTo(0f, 24f)
        lineTo(-17f, 13f); lineTo(-20f, -5f); lineTo(-47f, -6f + stride)
        lineTo(-72f, -28f + stride); lineTo(-40f, -19f)
        lineTo(-13f, -30f)
    }
    megaEarlyShade(0xFFD0A4E6) {
        moveTo(0f, -64f); lineTo(0f, -20f); lineTo(-37f, -14f); lineTo(-12f, -29f)
    }
    megaEarlyShade(0xFF8D65B4) {
        moveTo(0f, -19f); lineTo(40f, -16f); lineTo(64f, -23f - stride)
        lineTo(44f, -8f - stride); lineTo(17f, -8f); lineTo(12f, 12f); lineTo(0f, 20f)
    }
    kantoPolygon(0xFFE7C775, 0f, -37f, 20f, -27f, 22f, -6f, 0f, 9f, -22f, -6f, -20f, -27f)
    kantoPolygon(0xFF937455, 0f, -32f, 16f, -23f, 17f, -9f, 0f, 4f, -17f, -9f, -16f, -23f)
    kantoPolygon(0xFFC9497C, 0f, -29f, 13f, -21f, 13f, -9f, 0f, 1f, -13f, -9f, -13f, -21f)
    kantoPolygon(0xFFFFBCD4, 0f, -27f, -11f, -20f, -4f, -14f, 4f, -22f)
    kantoPolygon(0xFFED6C9E, -4f, -14f, 11f, -19f, 11f, -9f, 0f, -1f)
    kantoLine(0xFFFFE8F1, 2f, -7f, -21f, -3f, -24f)
    kantoLine(0xFF5C5290, 2f, -34f, -15f, -53f, -18f + stride)
    kantoLine(0xFF5C5290, 2f, 34f, -15f, 53f, -17f - stride)
}

private fun DrawScope.megaEarlyFloette(m: Float) {
    val petal = m * 5f
    // The enormous Eternal Flower is part of the body silhouette.
    megaEarlyCurve(KANTO_INK, 8f) {
        moveTo(-29f, 13f); cubicTo(4f, 39f, 40f, 41f, 34f, 3f)
        quadraticTo(24f, -20f, 37f, -33f)
    }
    megaEarlyCurve(0xFFCA4D62, 5f) {
        moveTo(-29f, 13f); cubicTo(4f, 39f, 40f, 41f, 34f, 3f)
        quadraticTo(24f, -20f, 37f, -33f)
    }
    megaEarlyShape(0xFF37445C) {
        moveTo(38f, -29f); cubicTo(29f, -42f, 12f + petal, -58f, 21f + petal, -72f)
        cubicTo(42f, -67f, 56f, -57f, 50f, -30f)
    }
    megaEarlyShape(0xFF37445C) {
        moveTo(46f, -29f); cubicTo(60f, -47f, 83f, -41f - petal, 99f, -24f - petal)
        cubicTo(91f, -9f - petal, 62f, -5f, 46f, -18f)
    }
    megaEarlyShape(0xFF37445C) {
        moveTo(39f, -19f); cubicTo(56f, -11f, 68f + petal, 13f, 62f + petal, 29f)
        cubicTo(40f, 25f, 21f, 1f, 32f, -17f)
    }
    kantoPolygon(0xFF7187BF, 25f + petal, -64f, 44f, -53f, 43f, -31f, 35f, -47f)
    kantoPolygon(0xFF7187BF, 56f, -25f, 79f, -31f - petal, 92f, -25f - petal, 67f, -17f)
    kantoPolygon(0xFF7187BF, 41f, -10f, 57f + petal, 20f, 44f, 8f, 36f, -7f)
    megaEarlyShape(0xFFE65972) {
        moveTo(35f, -35f); quadraticTo(22f, -43f, 19f, -28f)
        quadraticTo(9f, -21f, 24f, -13f); quadraticTo(26f, 3f, 40f, -8f)
        quadraticTo(56f, -3f, 54f, -20f); quadraticTo(64f, -36f, 47f, -35f)
        quadraticTo(43f, -47f, 35f, -35f)
    }
    kantoOval(0xFF492F4D, 29f, -30f, 20f, 20f)
    kantoOval(0xFF9DE3EB, 34f, -26f, 10f, 10f)
    megaEarlyShape(0xFF638DC1) {
        moveTo(-52f, -3f); quadraticTo(-29f, -9f, -22f, 14f)
        lineTo(-17f + m * 3f, 36f); lineTo(-29f, 29f); lineTo(-34f, 46f)
        lineTo(-40f, 30f); lineTo(-52f - m * 3f, 37f); lineTo(-49f, 16f)
    }
    megaEarlyCurve(0xFFFBF9ED, 6f) {
        moveTo(-49f, 0f); quadraticTo(-70f, 4f + petal, -72f, 18f + petal)
        moveTo(-27f, 0f); quadraticTo(-13f, 15f, -3f, 11f + petal)
    }
    megaEarlyShape(0xFFFCF9ED) {
        moveTo(-56f, -31f); quadraticTo(-38f, -46f, -21f, -27f)
        lineTo(-13f, -19f); lineTo(-22f, -8f)
        quadraticTo(-33f, 8f, -52f, -4f); quadraticTo(-69f, -10f, -56f, -31f)
    }
    kantoPolygon(0xFF6490C6, -57f, -22f, -68f, -41f - petal, -48f, -33f, -41f, -19f)
    kantoPolygon(0xFF6490C6, -31f, -26f, -25f, -49f + petal, -13f, -36f, -19f, -17f)
    kantoOval(0xFFF3D94E, -54f, -31f, 12f, 11f)
    kantoOval(0xFFF3D94E, -27f, -30f, 12f, 11f)
    kantoEyes(-38f, -13f, 10f, 3.5f)
    kantoSmile(-37f, -4f, 8f)
}

private fun DrawScope.megaEarlyMeowstic(m: Float) {
    val fan = m * 5f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFF384D77) {
            moveTo(side * 12f, 21f)
            cubicTo(side * 47f, 16f, side * 63f, 28f + side * fan, side * 53f, 47f + side * fan)
            quadraticTo(side * 32f, 61f, side * 24f, 42f)
            quadraticTo(side * 47f, 48f, side * 40f, 32f); lineTo(side * 16f, 34f)
        }
        megaEarlyShade(0xFFEEF1E9) {
            moveTo(side * 21f, 23f); quadraticTo(side * 45f, 18f, side * 53f, 32f + side * fan)
            quadraticTo(side * 59f, 46f + side * fan, side * 41f, 50f)
            quadraticTo(side * 52f, 38f, side * 40f, 29f); lineTo(side * 22f, 29f)
        }
        megaEarlyShape(0xFF405C86) {
            moveTo(side * 11f, -28f); quadraticTo(side * 19f, -61f, side * 54f, -71f + fan)
            quadraticTo(side * 67f, -56f + fan, side * 48f, -31f)
            lineTo(side * 20f, -16f)
        }
        megaEarlyShape(0xFFF3EFE4) {
            moveTo(side * 19f, -27f); quadraticTo(side * 29f, -51f, side * 53f, -63f + fan)
            quadraticTo(side * 51f, -42f, side * 38f, -31f); lineTo(side * 25f, -23f)
        }
        kantoOval(0xFFCFA6DD, side * 37f - 8f, -48f + fan * 0.5f, 16f, 16f)
        kantoOval(0xFFF2DF85, side * 37f - 4f, -44f + fan * 0.5f, 8f, 8f)
        kantoLine(KANTO_INK, 2f, side * 37f, -43f + fan * 0.5f, side * 37f, -38f + fan * 0.5f)
        megaEarlyShape(0xFFF0F1EB) {
            moveTo(side * 7f, 25f); lineTo(side * 20f, 25f); lineTo(side * 24f, 55f - side * fan)
            quadraticTo(side * 17f, 64f - side * fan, side * 8f, 55f - side * fan)
        }
    }
    megaEarlyShape(0xFF425A85) {
        moveTo(-18f, -12f); quadraticTo(-35f, 15f, -20f, 37f)
        quadraticTo(0f, 49f, 21f, 34f); quadraticTo(32f, 15f, 17f, -13f)
    }
    megaEarlyShape(0xFFF2F3EC) {
        moveTo(-11f, -9f); lineTo(14f, -9f); lineTo(22f, 7f)
        lineTo(11f, 4f); lineTo(2f, 27f); lineTo(-7f, 8f); lineTo(-20f, 10f)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyCurve(0xFF415780, 8f) {
            moveTo(side * 19f, 4f); quadraticTo(side * 30f, 15f, side * 38f, -1f - side * fan)
        }
        kantoOval(0xFFFAF7EA, side * 38f - 6f, -8f - side * fan, 12f, 13f)
    }
    megaEarlyShape(0xFFF4F2EA) {
        moveTo(-22f, -32f); lineTo(-27f, -16f); quadraticTo(-27f, 4f, 0f, 3f)
        quadraticTo(29f, 2f, 26f, -21f); lineTo(21f, -36f)
        lineTo(7f, -31f); lineTo(0f, -45f); lineTo(-10f, -31f)
    }
    megaEarlyShade(0xFF405780) {
        moveTo(0f, -44f); lineTo(10f, -28f); lineTo(23f, -32f)
        lineTo(25f, -14f); quadraticTo(8f, -18f, 0f, -4f)
    }
    kantoOval(0xFF99D9D8, -20f, -18f, 13f, 10f)
    kantoOval(0xFFF0B57C, 7f, -18f, 13f, 10f)
    kantoEyes(0f, -14f, 13f, 2.7f)
    kantoSmile(0f, -4f, 8f)
}

private fun DrawScope.megaEarlyPyroar(m: Float) {
    val mane = m * 5f
    megaEarlyCurve(KANTO_INK, 6f) {
        moveTo(44f, 10f); cubicTo(75f, 28f, 76f, -3f, 89f, -4f + mane)
    }
    megaEarlyShape(0xFFCE5149) {
        moveTo(84f, -7f + mane); lineTo(88f, -22f + mane); lineTo(97f, -11f + mane)
        quadraticTo(102f, 3f + mane, 84f, 6f + mane)
    }
    megaEarlyShape(0xFF735848) {
        moveTo(-12f, -7f); cubicTo(16f, -18f, 56f, -6f, 57f, 20f)
        quadraticTo(56f, 39f, 29f, 38f); lineTo(-14f, 28f)
    }
    megaEarlyShade(0xFF9C7657) {
        moveTo(4f, -5f); quadraticTo(38f, -12f, 50f, 8f)
        lineTo(31f, 14f); lineTo(2f, 8f)
    }
    for (x in listOf(-6f, 38f)) {
        val step = if (x < 0f) m * 3f else -m * 3f
        megaEarlyShape(0xFF705348) {
            moveTo(x - 7f, 19f); lineTo(x + 10f, 22f); lineTo(x + 5f + step, 56f)
            lineTo(x - 11f + step, 57f); lineTo(x - 5f, 39f)
        }
        kantoOval(0xFFCBAA77, x - 13f + step, 51f, 22f, 10f)
        kantoLine(0xFF715748, 1.5f, x - 5f + step, 54f, x - 5f + step, 59f)
    }
    megaEarlyShape(0xFFCF514B) {
        moveTo(-25f, -42f); cubicTo(-52f, -52f, -73f, -26f, -63f, -3f)
        lineTo(-76f, 3f + mane); lineTo(-57f, 15f); lineTo(-67f, 37f + mane)
        lineTo(-39f, 29f); lineTo(-36f, 54f); lineTo(-18f, 36f)
        lineTo(6f, 47f - mane); lineTo(5f, 21f); lineTo(28f, 19f)
        lineTo(12f, -2f); cubicTo(18f, -31f, -3f, -54f, -25f, -42f)
    }
    megaEarlyShape(0xFFE8A84C) {
        moveTo(-35f, -37f); quadraticTo(-38f, -64f, -22f, -71f)
        lineTo(-11f, -53f); lineTo(2f, -60f + mane)
        quadraticTo(2f, -26f, -11f, -16f); lineTo(-30f, 21f)
        lineTo(-44f, 5f); lineTo(-57f, 12f + mane); lineTo(-48f, -11f)
        lineTo(-61f, -23f); lineTo(-43f, -24f)
    }
    megaEarlyShape(0xFFF3C45A) {
        moveTo(-16f, -30f); quadraticTo(17f, -52f, 44f, -34f + mane)
        quadraticTo(31f, -17f, 10f, -21f); lineTo(-1f, -6f)
    }
    megaEarlyCurve(0xFF9E3E43, 2f) {
        moveTo(-45f, -4f); quadraticTo(-52f, 8f, -55f, 23f + mane)
        moveTo(-10f, 9f); quadraticTo(-3f, 20f, -1f, 32f - mane)
    }
    kantoOval(0xFF7C5849, -48f, -33f, 13f, 15f)
    kantoOval(0xFF7C5849, -7f, -34f, 13f, 15f)
    megaEarlyShape(0xFFC2A578) {
        moveTo(-39f, -29f); quadraticTo(-21f, -38f, -7f, -24f)
        lineTo(-3f, -7f); quadraticTo(-18f, 12f, -37f, -3f); lineTo(-44f, -14f)
    }
    kantoPolygon(0xFF654B46, -28f, -25f, -14f, -25f, -19f, -12f, -24f, -12f)
    kantoEyes(-23f, -17f, 12f, 3.2f)
    kantoOval(0xFFEAD3AA, -35f, -7f, 25f, 13f)
    kantoPolygon(KANTO_INK, -27f, -7f, -17f, -7f, -22f, -2f)
    kantoSmile(-22f, 2f, 13f)
}

private fun DrawScope.megaEarlyRaichuX(m: Float) {
    val punch = m * 6f
    megaEarlyCurve(KANTO_INK, 5f) {
        moveTo(22f, 25f); cubicTo(63f, 49f, 70f, 14f, 70f, -10f + punch)
    }
    kantoPolygon(0xFFEFCB56, 68f, -18f + punch, 64f, -43f + punch, 77f, -32f + punch,
        91f, -41f + punch, 89f, -23f + punch, 100f, -12f + punch,
        82f, -9f + punch, 76f, 8f + punch, 68f, -7f + punch, 53f, -12f + punch)
    kantoPolygon(0xFFF9E792, 73f, -29f + punch, 76f, -14f + punch, 87f, -16f + punch,
        78f, -9f + punch, 76f, 0f + punch, 69f, -11f + punch)
    for (side in listOf(-1f, 1f)) {
        val step = side * m * 3f
        megaEarlyShape(0xFF635047) {
            moveTo(side * 9f, 31f); lineTo(side * 25f, 28f); lineTo(side * 34f, 49f + step)
            quadraticTo(side * 34f, 64f + step, side * 9f, 58f + step)
        }
        kantoLine(0xFF9B7652, 2f, side * 18f, 53f + step, side * 29f, 54f + step)
    }
    megaEarlyShape(0xFFEAA14E) {
        moveTo(-19f, -20f); cubicTo(-46f, -1f, -33f, 41f, -12f, 45f)
        quadraticTo(16f, 51f, 31f, 28f); cubicTo(41f, 11f, 28f, -17f, 12f, -21f)
    }
    megaEarlyShape(0xFFFFE5AF) {
        moveTo(-9f, -2f); cubicTo(-24f, 6f, -19f, 38f, 1f, 39f)
        cubicTo(22f, 38f, 22f, 6f, 7f, -3f)
    }
    for (side in listOf(-1f, 1f)) {
        val hit = side * punch
        megaEarlyShape(0xFFDB984D) {
            moveTo(side * 22f, -9f); quadraticTo(side * 39f, -21f, side * 47f, -4f + hit)
            lineTo(side * 50f, 9f + hit); lineTo(side * 30f, 14f)
        }
        megaEarlyShape(0xFF48434A) {
            moveTo(side * 43f, -11f + hit); lineTo(side * 63f, -15f + hit)
            quadraticTo(side * 77f, -8f + hit, side * 71f, 10f + hit)
            lineTo(side * 56f, 20f + hit); lineTo(side * 40f, 9f + hit)
        }
        kantoLine(0xFF7E6E66, 2f, side * 56f, -6f + hit, side * 66f, -3f + hit)
        kantoLine(0xFF7E6E66, 2f, side * 53f, 1f + hit, side * 64f, 4f + hit)
        megaEarlyShape(0xFF634949) {
            moveTo(side * 14f, -33f); lineTo(side * 33f, -70f + side * m * 3f)
            quadraticTo(side * 49f, -67f, side * 41f, -44f)
            lineTo(side * 30f, -25f)
        }
        kantoPolygon(0xFFEABD57, side * 19f, -34f, side * 34f, -59f,
            side * 34f, -45f, side * 28f, -29f)
    }
    megaEarlyShape(0xFFF2B059) {
        moveTo(-26f, -38f); quadraticTo(-4f, -51f, 22f, -37f)
        lineTo(28f, -20f); quadraticTo(20f, 0f, -2f, -2f)
        quadraticTo(-30f, -2f, -31f, -20f)
    }
    kantoOval(0xFFFFDD63, -31f, -20f, 13f, 12f)
    kantoOval(0xFFFFDD63, 16f, -20f, 13f, 12f)
    kantoEyes(-2f, -25f, 13f, 3.5f)
    kantoPolygon(KANTO_INK, -5f, -17f, 2f, -17f, -2f, -13f)
    kantoSmile(-2f, -10f, 13f)
}

private fun DrawScope.megaEarlyRaichuY(m: Float) {
    val flare = m * 5f
    megaEarlyCurve(KANTO_INK, 4f) {
        moveTo(12f, 26f); cubicTo(40f, 28f, 27f, 47f, 47f, 43f)
        cubicTo(67f, 38f, 51f, 15f, 72f, 14f + flare)
    }
    kantoPolygon(0xFFF6D86A, 65f, 17f + flare, 58f, -10f + flare, 73f, -7f + flare,
        66f, -32f + flare, 89f, -21f + flare, 81f, -3f + flare,
        96f, 0f + flare, 73f, 27f + flare)
    megaEarlyShade(0xFFFFECB0) {
        moveTo(72f, 17f + flare); lineTo(73f, -1f + flare); lineTo(67f, -7f + flare)
        lineTo(78f, -9f + flare); lineTo(78f, -21f + flare); lineTo(83f, -20f + flare)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFE3A653) {
            moveTo(side * 7f, 24f); quadraticTo(side * 26f, 22f, side * 25f, 43f - side * flare)
            quadraticTo(side * 17f, 59f - side * flare, side * 7f, 46f - side * flare)
        }
        megaEarlyShape(0xFFE6AC59) {
            moveTo(side * 16f, -4f); quadraticTo(side * 32f, -7f, side * 45f, 3f + side * flare)
            quadraticTo(side * 43f, 15f + side * flare, side * 29f, 11f)
            lineTo(side * 15f, 11f)
        }
    }
    megaEarlyShape(0xFFF1BF6C) {
        moveTo(-16f, -14f); cubicTo(-25f, 2f, -24f, 29f, -7f, 36f)
        quadraticTo(16f, 42f, 23f, 18f); quadraticTo(27f, -3f, 13f, -15f)
    }
    megaEarlyShape(0xFFFFEFBA) {
        moveTo(-7f, -3f); quadraticTo(-19f, 20f, 0f, 30f)
        quadraticTo(20f, 24f, 8f, -5f)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFF664B48) {
            moveTo(side * 13f, -35f); quadraticTo(side * 27f, -62f, side * 44f, -68f + side * m * 3f)
            quadraticTo(side * 53f, -47f, side * 34f, -33f); lineTo(side * 22f, -25f)
        }
        kantoPolygon(0xFFF4CE6C, side * 23f, -33f, side * 39f, -58f,
            side * 40f, -44f, side * 32f, -34f)
        // Temple tufts, rather than literal wings, give Y its wide forked silhouette.
        kantoPolygon(0xFFF9D773, side * 19f, -31f, side * 47f, -47f + side * flare,
            side * 43f, -32f + side * flare, side * 65f, -32f + side * flare,
            side * 49f, -21f + side * flare, side * 58f, -14f + side * flare, side * 26f, -10f)
        kantoLine(0xFFCD994A, 2f, side * 29f, -25f, side * 46f, -30f + side * flare)
    }
    megaEarlyShape(0xFFF5C472) {
        moveTo(-22f, -40f); quadraticTo(-1f, -52f, 20f, -39f)
        lineTo(27f, -22f); quadraticTo(24f, -1f, 0f, -1f)
        quadraticTo(-23f, 0f, -28f, -18f)
    }
    kantoOval(0xFFFFE277, -28f, -20f, 12f, 11f)
    kantoOval(0xFFFFE277, 16f, -20f, 12f, 11f)
    kantoOval(0xFF62B4D2, -17f, -30f, 10f, 12f)
    kantoOval(0xFF62B4D2, 7f, -30f, 10f, 12f)
    kantoEyes(0f, -24f, 12f, 2.6f)
    kantoPolygon(KANTO_INK, -3f, -17f, 3f, -17f, 0f, -14f)
    kantoSmile(0f, -10f, 11f)
}

private fun DrawScope.megaEarlyClefable(m: Float) {
    val wing = m * 6f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFC47CAD) {
            moveTo(side * 14f, -10f)
            cubicTo(side * 27f, -47f, side * 64f, -71f + wing, side * 89f, -62f + wing)
            quadraticTo(side * 88f, -30f, side * 72f, -15f)
            quadraticTo(side * 89f, 1f, side * 73f, 24f + wing)
            quadraticTo(side * 42f, 25f, side * 18f, 12f)
        }
        megaEarlyShape(0xFFE2A3CB) {
            moveTo(side * 23f, -12f); quadraticTo(side * 48f, -53f, side * 80f, -56f + wing)
            quadraticTo(side * 76f, -21f, side * 43f, -10f); lineTo(side * 27f, 6f)
        }
        megaEarlyShape(0xFFF2D98D) {
            moveTo(side * 31f, 6f); quadraticTo(side * 58f, -3f, side * 72f, -8f)
            quadraticTo(side * 81f, 7f, side * 67f, 18f + wing); lineTo(side * 41f, 13f)
        }
        megaEarlyCurve(0xFF9D6095, 2f) {
            moveTo(side * 25f, -5f); quadraticTo(side * 51f, -29f, side * 73f, -45f + wing)
            moveTo(side * 43f, -20f); lineTo(side * 52f, -44f + wing)
        }
    }
    megaEarlyShape(0xFFE6A7BF) {
        moveTo(19f, 28f); cubicTo(52f, 9f, 66f, 31f + m * 3f, 51f, 48f)
        quadraticTo(33f, 64f, 18f, 43f); lineTo(34f, 43f)
        quadraticTo(48f, 33f, 32f, 31f)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFE5A6BE) {
            moveTo(side * 7f, 28f); lineTo(side * 23f, 27f)
            lineTo(side * 30f, 50f - side * m * 4f)
            quadraticTo(side * 17f, 60f - side * m * 4f, side * 7f, 48f)
        }
    }
    megaEarlyShape(0xFFF5BED0) {
        moveTo(-22f, -24f); quadraticTo(-39f, -3f, -32f, 24f)
        quadraticTo(-23f, 48f, 0f, 47f); quadraticTo(29f, 47f, 33f, 23f)
        quadraticTo(40f, -5f, 20f, -28f)
    }
    megaEarlyShade(0xFFFFD9DF) {
        moveTo(-17f, -17f); quadraticTo(-31f, 2f, -18f, 24f)
        quadraticTo(-3f, 36f, 16f, 28f); quadraticTo(5f, 41f, -13f, 36f)
        quadraticTo(-34f, 21f, -25f, -8f)
    }
    for (side in listOf(-1f, 1f)) {
        kantoPolygon(0xFFF1B9CC, side * 10f, -23f, side * 27f, -54f,
            side * 31f, -29f, side * 23f, -12f)
        kantoPolygon(0xFF5A4555, side * 18f, -31f, side * 26f, -48f, side * 27f, -29f)
        megaEarlyShape(0xFFF2BACE) {
            moveTo(side * 25f, 0f); quadraticTo(side * 43f, 0f, side * 48f, -10f + side * wing)
            quadraticTo(side * 55f, 2f + side * wing, side * 43f, 14f + side * wing)
            lineTo(side * 29f, 18f)
        }
    }
    megaEarlyShape(0xFFFAD0DB) {
        moveTo(-17f, -26f); cubicTo(-21f, -43f, 10f, -44f, 12f, -29f)
        quadraticTo(10f, -16f, -4f, -22f); quadraticTo(8f, -26f, -1f, -31f)
    }
    kantoEyes(0f, -8f, 13f, 3.5f)
    kantoOval(0xFFEE9DB8, -25f, -1f, 10f, 6f)
    kantoOval(0xFFEE9DB8, 15f, -1f, 10f, 6f)
    kantoSmile(0f, 4f, 13f)
}

private fun DrawScope.megaEarlyAlakazam(m: Float) {
    val mind = m * 4f
    // Five independently bobbing spoons form the characteristic psychic crown.
    for (i in 0..4) {
        val x = (i - 2) * 23f
        val y = -64f + (i - 2) * (i - 2) * 5f + if (i % 2 == 0) mind else -mind
        kantoLine(KANTO_INK, 4f, x, y + 10f, x + (i - 2) * 2f, y + 24f)
        kantoLine(0xFFBFCBD4, 2f, x, y + 10f, x + (i - 2) * 2f, y + 24f)
        kantoOval(0xFFDDE8E9, x - 6f, y - 9f, 12f, 18f)
        kantoLine(0xFF97A9BB, 1.5f, x - 2f, y - 3f, x - 1f, y + 3f)
    }
    kantoPolygon(0xFFAD8651, -13f, 12f, -31f, 20f, -39f - mind, 41f,
        -11f, 47f, 4f, 36f, -17f, 31f)
    kantoPolygon(0xFFBFA064, 13f, 12f, 33f, 20f, 40f + mind, 42f,
        13f, 49f, -5f, 37f, 20f, 31f)
    kantoPolygon(0xFFF0D374, -32f - mind, 34f, -46f - mind, 40f, -36f, 47f, -19f, 42f)
    kantoPolygon(0xFFF0D374, 31f + mind, 35f, 47f + mind, 40f, 34f, 49f, 20f, 43f)
    megaEarlyShape(0xFF99704B) {
        moveTo(-16f, -15f); lineTo(18f, -15f); lineTo(22f, 13f)
        quadraticTo(1f, 34f, -21f, 13f)
    }
    kantoPolygon(0xFFE4C164, -12f, -10f, 12f, -10f, 9f, 18f, -7f, 18f)
    for (side in listOf(-1f, 1f)) {
        kantoLine(KANTO_INK, 9f, side * 18f, -6f, side * 31f, 15f,
            side * 42f, 6f + side * mind)
        kantoLine(0xFFE8CC72, 6f, side * 18f, -6f, side * 31f, 15f,
            side * 42f, 6f + side * mind)
        kantoOval(0xFFEAD382, side * 45f - 7f, -2f + side * mind, 14f, 10f)
        kantoLine(0xFFC4A157, 2f, side * 39f, 1f + side * mind,
            side * 45f, 4f + side * mind, side * 51f, 0f + side * mind)
    }
    megaEarlyShape(0xFFF1EEE2) {
        moveTo(-23f, -21f); lineTo(-38f, -5f); lineTo(-24f, -8f)
        lineTo(-29f, 11f); lineTo(-16f, 6f); lineTo(-14f, 27f)
        lineTo(-3f, 19f); lineTo(2f, 35f); lineTo(12f, 20f)
        lineTo(24f, 25f); lineTo(21f, 6f); lineTo(33f, 11f)
        lineTo(27f, -8f); lineTo(38f, -5f); lineTo(21f, -23f)
    }
    kantoPolygon(0xFFEAD179, -20f, -37f, -28f, -54f, -10f, -43f, 10f, -43f,
        28f, -54f, 21f, -33f, 15f, -17f, 0f, -10f, -16f, -18f)
    megaEarlyShape(0xFFE3B6C5) {
        moveTo(-15f, -39f); quadraticTo(-16f, -56f, 0f, -55f)
        quadraticTo(18f, -55f, 15f, -39f); lineTo(7f, -30f); lineTo(-7f, -30f)
    }
    kantoOval(0xFFBD556B, -6f, -41f, 12f, 10f)
    kantoLine(KANTO_INK, 2f, -20f, -28f, -11f, -25f, -5f, -27f)
    kantoLine(KANTO_INK, 2f, 5f, -27f, 12f, -25f, 21f, -28f)
    kantoPolygon(0xFFF2D583, -7f, -24f, 7f, -24f, 0f, -13f)
    megaEarlyCurve(0xFFFAF5E8, 5f) {
        moveTo(-3f, -17f); quadraticTo(-21f, -19f, -33f, -11f + mind)
        moveTo(3f, -17f); quadraticTo(21f, -19f, 33f, -11f - mind)
    }
    megaEarlyCurve(0xFFC4C4C7, 1.5f) {
        moveTo(-12f, -3f); lineTo(-8f, 11f)
        moveTo(9f, 0f); lineTo(5f, 21f)
    }
}

private fun DrawScope.megaEarlyGengar(m: Float) {
    val claw = m * 5f
    megaEarlyShape(0xFF694A8E) {
        moveTo(-39f, 5f); lineTo(-52f, -9f); lineTo(-42f, -13f)
        lineTo(-48f, -32f); lineTo(-30f, -24f); lineTo(-26f, -58f)
        lineTo(-10f, -43f); lineTo(0f, -69f); lineTo(11f, -43f)
        lineTo(28f, -57f); lineTo(33f, -26f); lineTo(48f, -33f)
        lineTo(45f, -15f); lineTo(56f, -10f); lineTo(43f, 7f)
        quadraticTo(52f, 36f, 23f, 46f); lineTo(18f, 55f)
        lineTo(7f, 48f); lineTo(-7f, 53f); lineTo(-19f, 46f)
        quadraticTo(-55f, 42f, -39f, 5f)
    }
    megaEarlyShade(0xFF9566B1) {
        moveTo(-32f, -14f); lineTo(-26f, -43f); lineTo(-15f, -29f); lineTo(0f, -54f)
        lineTo(10f, -31f); lineTo(25f, -43f); lineTo(28f, -10f)
        quadraticTo(0f, -29f, -32f, -14f)
    }
    megaEarlyShade(0xFF533F78) {
        moveTo(31f, -3f); quadraticTo(50f, 29f, 17f, 41f)
        lineTo(8f, 44f); quadraticTo(25f, 30f, 31f, -3f)
    }
    for (side in listOf(-1f, 1f)) {
        val spread = side * claw
        megaEarlyShape(0xFF80559F) {
            moveTo(side * 33f, 7f); quadraticTo(side * 53f, 0f, side * 62f, 20f + spread)
            lineTo(side * 78f, 19f + spread); lineTo(side * 74f, 31f + spread)
            lineTo(side * 86f, 35f + spread); lineTo(side * 72f, 42f + spread)
            lineTo(side * 75f, 50f + spread); lineTo(side * 58f, 45f + spread)
            quadraticTo(side * 39f, 45f, side * 32f, 27f)
        }
        kantoLine(0xFFB082C7, 2f, side * 52f, 19f + spread, side * 64f, 29f + spread)
        kantoLine(KANTO_INK, 2f, side * 59f, 34f + spread, side * 73f, 33f + spread)
        kantoLine(KANTO_INK, 2f, side * 58f, 41f + spread, side * 69f, 42f + spread)
        kantoPolygon(0xFFE56779, side * 8f, -9f, side * 33f, -18f,
            side * 27f, -2f, side * 13f, 0f)
        kantoLine(KANTO_INK, 3f, side * 21f, -11f, side * 19f, -3f)
    }
    megaEarlyShape(0xFFFFF0BF) {
        moveTo(-31f, 7f); quadraticTo(1f, 20f, 33f, 6f)
        quadraticTo(25f, 37f, 1f, 37f); quadraticTo(-23f, 36f, -31f, 7f)
    }
    kantoLine(0xFF90637E, 1.5f, -17f, 13f, -16f, 30f)
    kantoLine(0xFF90637E, 1.5f, -3f, 16f, -2f, 36f)
    kantoLine(0xFF90637E, 1.5f, 12f, 15f, 12f, 34f)
    kantoLine(0xFF90637E, 1.5f, 25f, 11f, 24f, 25f)
    megaEarlyShape(0xFFF0D471) {
        moveTo(-12f, -27f); quadraticTo(0f, -44f, 12f, -27f)
        quadraticTo(0f, -13f, -12f, -27f)
    }
    kantoOval(0xFFB74461, -4f, -34f, 8f, 15f)
    kantoLine(0xFFFFE3CB, 2f, -1f, -31f, -1f, -26f)
}

private fun DrawScope.megaEarlyScolipede(m: Float) {
    val sway = m * 4f
    // A rearing, plated centipede; rear segments remain visible behind its tall thorax.
    for (i in 4 downTo 0) {
        val x = 19f + i * 13f
        val y = 28f - i * 3f
        val step = if (i % 2 == 0) sway else -sway
        kantoPolygon(0xFF5B516A, x - 6f, y + 5f, x + 5f, y + 9f,
            x + 7f + step, 54f - i * 2f, x - 4f + step, 58f - i * 2f)
        megaEarlyShape(0xFFAF4D76) {
            moveTo(x - 13f, y - 8f); quadraticTo(x, y - 27f, x + 16f, y - 9f)
            lineTo(x + 13f, y + 14f); quadraticTo(x, y + 26f, x - 13f, y + 13f)
        }
        kantoOval(0xFF7B4A94, x - 3f, y - 7f, 12f, 16f)
        kantoOval(0xFFE899B9, x, y - 3f, 6f, 8f)
        kantoPolygon(0xFF65546C, x - 2f, y - 16f, x + 4f, y - 27f, x + 10f, y - 15f)
    }
    kantoPolygon(0xFF974875, 69f, 17f, 90f, 2f + sway, 98f, 6f + sway, 80f, 25f)
    kantoPolygon(0xFF974875, 72f, 27f, 94f, 22f - sway, 99f, 30f - sway, 81f, 35f)
    megaEarlyShape(0xFF565367) {
        moveTo(-27f, -38f); cubicTo(-47f, -21f, -49f, 14f, -27f, 40f)
        quadraticTo(-3f, 59f, 21f, 38f); quadraticTo(7f, 13f, 13f, -30f)
        lineTo(-8f, -46f)
    }
    for (i in 0..3) {
        val y = -21f + i * 16f
        megaEarlyShape(0xFFC7658A) {
            moveTo(-36f + i * 2f, y); quadraticTo(-14f, y - 13f, 10f + i * 2f, y - 2f)
            lineTo(11f + i * 2f, y + 8f); quadraticTo(-10f, y + 2f, -30f + i * 2f, y + 10f)
        }
        kantoLine(0xFFF0A9C1, 2f, -30f + i * 2f, y, -18f, y - 3f, 4f, y + 1f)
        for (side in listOf(-1f, 1f)) {
            val bend = side * sway + i
            kantoPolygon(0xFF594A69, -13f + side * 23f, y + 3f,
                -13f + side * (42f + i * 2f), y + 11f + bend,
                -13f + side * (37f + i * 2f), y + 23f + bend,
                -13f + side * 29f, y + 13f)
        }
    }
    megaEarlyShape(0xFFBD587F) {
        moveTo(-31f, -48f); lineTo(-17f, -56f); lineTo(2f, -49f)
        lineTo(12f, -29f); lineTo(0f, -13f); lineTo(-29f, -18f); lineTo(-41f, -34f)
    }
    kantoPolygon(0xFF66546E, -31f, -44f, -45f, -72f + sway, -36f, -71f + sway, -19f, -43f)
    kantoPolygon(0xFF66546E, -5f, -47f, 6f, -73f - sway * 0.5f, 15f, -65f, 6f, -35f)
    kantoLine(0xFFDC75A1, 4f, -40f, -61f + sway * 0.5f, -34f, -63f + sway * 0.5f)
    kantoLine(0xFFDC75A1, 4f, 3f, -59f, 11f, -57f)
    kantoPolygon(0xFFF6D37B, -31f, -34f, -19f, -31f, -25f, -25f)
    kantoPolygon(0xFFF6D37B, -11f, -31f, 0f, -34f, -5f, -24f)
    kantoEyes(-15f, -29f, 11f, 2f)
    kantoPolygon(0xFFE5D6B4, -26f, -18f, -20f, -8f, -15f, -18f)
    kantoPolygon(0xFFE5D6B4, -11f, -17f, -7f, -8f, -2f, -20f)
}

private fun DrawScope.megaEarlyVictreebel(m: Float) {
    val leaf = m * 6f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFF69994E) {
            moveTo(side * 29f, 1f); cubicTo(side * 52f, -25f, side * 69f, -19f - side * leaf, side * 86f, -27f - side * leaf)
            lineTo(side * 79f, -7f); lineTo(side * 92f, -7f - side * leaf)
            quadraticTo(side * 68f, 31f + side * leaf, side * 34f, 25f)
        }
        megaEarlyCurve(0xFFABC775, 2f) {
            moveTo(side * 37f, 15f); quadraticTo(side * 58f, 3f, side * 76f, -12f - side * leaf)
        }
    }
    megaEarlyShape(0xFFE8CF76) {
        moveTo(-23f, -24f); cubicTo(-24f, -6f, -49f, -4f, -47f, 27f)
        cubicTo(-50f, 57f, -15f, 65f, 18f, 57f)
        cubicTo(54f, 54f, 61f, 15f, 40f, -4f); lineTo(23f, -25f)
    }
    megaEarlyShade(0xFFFFE997) {
        moveTo(-26f, -1f); cubicTo(-43f, 17f, -37f, 46f, -19f, 50f)
        quadraticTo(-7f, 51f, -14f, 44f); cubicTo(-28f, 30f, -25f, 10f, -13f, 0f)
    }
    megaEarlyShade(0xFFC1A962) {
        moveTo(33f, -2f); cubicTo(58f, 22f, 40f, 58f, 7f, 59f)
        quadraticTo(35f, 39f, 30f, 19f)
    }
    for ((x, y) in listOf(-28f to 18f, 19f to 17f, -10f to 42f, 34f to 37f)) {
        megaEarlyShape(0xFF9C774E) {
            moveTo(x - 5f, y - 4f); quadraticTo(x + 6f, y - 10f, x + 9f, y)
            quadraticTo(x + 3f, y + 9f, x - 5f, y + 4f)
        }
    }
    kantoOval(0xFFDFC66F, -28f, -40f, 53f, 28f)
    kantoOval(0xFF65534C, -22f, -36f, 42f, 15f)
    megaEarlyShape(0xFF64944E) {
        moveTo(-25f, -42f); cubicTo(-54f, -50f, -27f, -75f, -3f, -69f)
        cubicTo(19f, -75f, 49f, -51f + leaf, 61f, -32f + leaf)
        quadraticTo(27f, -44f, 18f, -37f); quadraticTo(-4f, -48f, -25f, -42f)
    }
    megaEarlyCurve(0xFFA4C775, 2f) {
        moveTo(-23f, -48f); quadraticTo(2f, -63f, 43f, -42f + leaf)
    }
    // Constricting vines seal the swollen acid pitcher.
    megaEarlyCurve(KANTO_INK, 8f) {
        moveTo(-26f, -21f); cubicTo(22f, -11f, 38f, -34f, 13f, -34f)
        cubicTo(-13f, -35f, -35f, -9f, 29f, -11f)
    }
    megaEarlyCurve(0xFF628651, 5f) {
        moveTo(-26f, -21f); cubicTo(22f, -11f, 38f, -34f, 13f, -34f)
        cubicTo(-13f, -35f, -35f, -9f, 29f, -11f)
    }
    megaEarlyCurve(0xFF628651, 4f) {
        moveTo(25f, -14f); cubicTo(50f, -24f, 56f, -55f + leaf, 71f, -50f + leaf)
        quadraticTo(82f, -43f + leaf, 71f, -36f + leaf)
    }
    kantoPolygon(0xFFFFF6D0, -18f, -31f, -13f, -19f, -8f, -30f)
    kantoPolygon(0xFFFFF6D0, 6f, -30f, 11f, -19f, 16f, -31f)
    kantoOval(0xFFF8F2DC, -28f, -4f, 17f, 10f)
    kantoOval(0xFFF8F2DC, 8f, -4f, 17f, 10f)
    kantoEyes(-2f, 1f, 18f, 3f)
}

private fun DrawScope.megaEarlyMedicham(m: Float) {
    val dance = m * 5f
    for (side in listOf(-1f, 1f)) {
        for (arm in 0..1) {
            val y = -20f + arm * 22f
            megaEarlyShape(0xFFEDE6DD) {
                moveTo(side * 12f, y); cubicTo(side * 26f, y - 14f,
                    side * 47f, y - 3f, side * 55f, y - 22f + side * dance)
                quadraticTo(side * 64f, y - 20f + side * dance, side * 58f, y - 9f + side * dance)
                quadraticTo(side * 43f, y + 16f, side * 16f, y + 6f)
            }
            kantoOval(0xFFF1CA67, side * 51f - 5f, y - 10f + side * dance, 10f, 9f)
            kantoLine(0xFFBCB1C2, 1.5f, side * 58f, y - 18f + side * dance,
                side * 55f, y - 14f + side * dance)
        }
    }
    megaEarlyShape(0xFFC75679) {
        moveTo(-11f, 6f); quadraticTo(-33f, 5f, -39f, 27f)
        quadraticTo(-39f, 46f, -19f, 40f); lineTo(-4f, 29f)
        lineTo(11f, 41f); quadraticTo(33f, 47f, 36f, 24f); quadraticTo(30f, 5f, 11f, 6f)
    }
    for (side in listOf(-1f, 1f)) {
        kantoOval(0xFFF0BF61, side * 23f - 7f, 20f, 14f, 14f)
        megaEarlyShape(0xFFF3E9DA) {
            moveTo(side * 19f, 35f); lineTo(side * 28f, 35f)
            lineTo(side * 25f + side * dance, 53f)
            lineTo(side * 38f + side * dance, 57f); lineTo(side * 14f + side * dance, 61f)
            lineTo(side * 13f + side * dance, 52f)
        }
        kantoLine(0xFFE5BB65, 4f, side * 16f + side * dance, 50f, side * 25f + side * dance, 50f)
    }
    megaEarlyShape(0xFFF0E7D9) {
        moveTo(-13f, -27f); quadraticTo(-22f, -15f, -8f, 12f)
        quadraticTo(0f, 18f, 9f, 10f); quadraticTo(21f, -11f, 12f, -28f)
    }
    kantoLine(0xFFE0B85E, 5f, -10f, 7f, 10f, 7f)
    for (side in listOf(-1f, 1f)) {
        megaEarlyCurve(KANTO_INK, 7f) {
            moveTo(side * 13f, -17f); lineTo(side * 28f, -3f + side * dance); lineTo(side * 19f, 11f)
        }
        megaEarlyCurve(0xFFF2E8DA, 4f) {
            moveTo(side * 13f, -17f); lineTo(side * 28f, -3f + side * dance); lineTo(side * 19f, 11f)
        }
        kantoOval(0xFFC75578, side * 20f - 5f, 5f, 10f, 12f)
    }
    kantoOval(0xFFF0E7DA, -19f, -46f, 38f, 29f)
    megaEarlyShape(0xFFE58AAB) {
        moveTo(-21f, -43f); quadraticTo(-28f, -62f, -9f, -66f)
        lineTo(-2f, -73f); lineTo(8f, -66f); quadraticTo(29f, -61f, 21f, -43f)
        lineTo(9f, -39f); lineTo(0f, -45f); lineTo(-9f, -39f)
    }
    kantoLine(0xFFC55D86, 2f, -15f, -59f, -10f, -47f)
    kantoLine(0xFFC55D86, 2f, 15f, -59f, 10f, -47f)
    kantoOval(0xFFF5D072, -6f, -51f, 12f, 12f)
    kantoPolygon(0xFFCC6F9B, 0f, -48f, 4f, -45f, 0f, -41f, -4f, -45f)
    kantoEyes(0f, -33f, 11f, 3f)
    kantoSmile(0f, -24f, 9f)
    kantoOval(0xFFE6BE60, -25f, -31f, 8f, 13f)
    kantoOval(0xFFE6BE60, 17f, -31f, 8f, 13f)
}

private fun DrawScope.megaEarlyManectric(m: Float) {
    val charge = m * 4f
    kantoPolygon(0xFF4B8AA2, 41f, 12f, 71f, -6f + charge, 84f, -33f + charge,
        90f, -7f + charge, 72f, 20f, 46f, 26f)
    for (x in listOf(-27f, 34f)) {
        val step = if (x < 0f) charge else -charge
        megaEarlyShape(0xFF43768F) {
            moveTo(x - 6f, 13f); lineTo(x + 10f, 17f); lineTo(x + 4f + step, 45f)
            lineTo(x + 10f + step, 57f); lineTo(x - 12f + step, 57f); lineTo(x - 8f, 35f)
        }
    }
    megaEarlyShape(0xFF5D9DB2) {
        moveTo(-43f, -9f); cubicTo(-21f, -29f, 24f, -18f, 49f, 1f)
        quadraticTo(60f, 24f, 34f, 34f); lineTo(-18f, 27f); lineTo(-43f, 14f)
    }
    for (x in listOf(-17f, 43f)) {
        val step = if (x < 0f) -charge else charge
        kantoPolygon(0xFF63A6B7, x - 8f, 17f, x + 10f, 19f, x + 4f + step, 44f,
            x + 13f + step, 58f, x - 12f + step, 60f, x - 8f + step, 44f)
        kantoPolygon(0xFFE6D165, x - 8f + step, 43f, x + 5f + step, 42f,
            x + 13f + step, 58f, x - 12f + step, 60f)
        kantoLine(0xFF8C824C, 1.5f, x - 4f + step, 53f, x - 4f + step, 58f)
    }
    // A single enormous lightning-bolt mane arches from forehead to hindquarters.
    megaEarlyShape(0xFFF0D771) {
        moveTo(-53f, -10f); lineTo(-47f, -43f); lineTo(-28f, -72f)
        lineTo(-21f, -43f); lineTo(-1f, -61f); lineTo(1f, -35f)
        lineTo(24f, -47f - charge); lineTo(21f, -19f); lineTo(43f, -27f)
        lineTo(38f, -1f); lineTo(61f, -7f + charge); lineTo(55f, 25f)
        lineTo(32f, 17f); lineTo(20f, 5f); lineTo(-1f, 13f)
        lineTo(-16f, 0f); lineTo(-37f, 7f)
    }
    megaEarlyShade(0xFFFFEB9B) {
        moveTo(-46f, -12f); lineTo(-28f, -62f); lineTo(-24f, -30f)
        lineTo(-6f, -46f); lineTo(-8f, -15f); lineTo(11f, -24f)
        lineTo(8f, -2f); lineTo(-15f, -8f); lineTo(-30f, -1f)
    }
    megaEarlyShade(0xFFC7B255) {
        moveTo(11f, -20f); lineTo(23f, -38f - charge); lineTo(16f, -9f)
        lineTo(36f, -18f); lineTo(32f, 8f); lineTo(52f, 4f + charge)
        lineTo(50f, 18f); lineTo(34f, 11f); lineTo(20f, -1f); lineTo(8f, 7f)
    }
    megaEarlyShape(0xFF5C98AF) {
        moveTo(-48f, -28f); lineTo(-29f, -21f); lineTo(-29f, 0f)
        lineTo(-49f, 16f); lineTo(-68f, 5f); lineTo(-64f, -9f); lineTo(-53f, -10f)
    }
    kantoPolygon(0xFF78B2C1, -62f, -9f, -47f, -5f, -36f, -11f, -36f, 0f, -51f, 11f, -66f, 3f)
    kantoPolygon(0xFF394856, -64f, -1f, -55f, 0f, -59f, 6f, -68f, 3f)
    kantoPolygon(0xFFF2DA6D, -49f, -20f, -34f, -18f, -39f, -11f)
    kantoOval(0xFFBA4E55, -44f, -18f, 6f, 7f)
    kantoLine(KANTO_INK, 2f, -56f, 8f, -46f, 7f)
}

private fun DrawScope.megaEarlyGardevoir(m: Float) {
    val skirt = m * 4f
    megaEarlyShape(0xFFD9DDE6) {
        moveTo(-11f, 1f); quadraticTo(-30f, 16f, -52f - skirt, 47f)
        quadraticTo(-28f, 63f, 0f, 60f); quadraticTo(35f, 64f, 57f + skirt, 47f)
        quadraticTo(31f, 12f, 11f, 0f)
    }
    megaEarlyShape(0xFFFFFAF0) {
        moveTo(-8f, -5f); cubicTo(-22f, 13f, -27f, 28f, -63f - skirt, 43f)
        quadraticTo(-48f, 58f, -25f, 51f); quadraticTo(-10f, 64f, 6f, 55f)
        quadraticTo(30f, 64f, 43f, 49f); quadraticTo(58f, 54f, 66f + skirt, 43f)
        cubicTo(30f, 26f, 24f, 9f, 9f, -5f)
    }
    megaEarlyShade(0xFFE0E7E8) {
        moveTo(-6f, 6f); quadraticTo(-9f, 31f, -25f, 51f)
        quadraticTo(-8f, 44f, -2f, 29f); lineTo(0f, 53f)
        quadraticTo(5f, 29f, 5f, 8f)
    }
    megaEarlyShade(0xFFE9E9E6) {
        moveTo(12f, 10f); quadraticTo(22f, 28f, 45f, 47f)
        quadraticTo(32f, 51f, 23f, 43f); quadraticTo(16f, 25f, 12f, 10f)
    }
    megaEarlyCurve(0xFFB8C8CE, 1.5f) {
        moveTo(-22f, 24f); quadraticTo(-31f, 42f, -46f - skirt, 47f)
        moveTo(23f, 23f); quadraticTo(35f, 39f, 53f + skirt, 45f)
    }
    megaEarlyShape(0xFFF9F7ED) {
        moveTo(-10f, -33f); lineTo(12f, -33f); lineTo(15f, -18f)
        lineTo(7f, 7f); quadraticTo(0f, 12f, -9f, 5f); lineTo(-16f, -19f)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFFDF9F0) {
            moveTo(side * 11f, -28f); quadraticTo(side * 26f, -32f, side * 30f, -11f + side * skirt)
            quadraticTo(side * 39f, -1f, side * 48f, -10f + side * skirt)
            lineTo(side * 52f, -4f + side * skirt); quadraticTo(side * 34f, 13f, side * 23f, -3f)
            lineTo(side * 12f, -15f)
        }
        kantoLine(0xFF719C90, 2f, side * 42f, -3f + side * skirt, side * 49f, -7f + side * skirt)
    }
    kantoPolygon(0xFFD95C75, -2f, -23f, -23f, -32f, -19f, -13f, 0f, -8f)
    kantoPolygon(0xFFEB8094, 1f, -24f, 24f, -34f, 20f, -15f, 0f, -8f)
    kantoOval(0xFFF8F4E9, -18f, -54f, 36f, 30f)
    megaEarlyShape(0xFF68AB8B) {
        moveTo(-23f, -30f); cubicTo(-35f, -53f, -11f, -72f, 10f, -66f)
        quadraticTo(31f, -65f, 29f, -41f); lineTo(20f, -25f); lineTo(16f, -48f)
        lineTo(3f, -40f); lineTo(-11f, -51f); lineTo(-13f, -29f)
    }
    megaEarlyShade(0xFF9FD0AB) {
        moveTo(-21f, -52f); quadraticTo(-3f, -70f, 17f, -57f)
        lineTo(5f, -56f); quadraticTo(-9f, -61f, -21f, -52f)
    }
    kantoOval(0xFFD96979, -11f, -43f, 7f, 9f)
    kantoOval(0xFFD96979, 5f, -43f, 7f, 9f)
    kantoEyes(0f, -39f, 8f, 2f)
    kantoSmile(0f, -30f, 8f)
}

private fun DrawScope.megaEarlyGallade(m: Float) {
    val slash = m * 5f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFF4F2E9) {
            moveTo(side * 9f, -27f); quadraticTo(side * 36f, -28f, side * 38f, -2f)
            quadraticTo(side * 50f, 28f, side * 75f, 42f + side * slash)
            quadraticTo(side * 39f, 51f, side * 16f, 25f); lineTo(side * 8f, -3f)
        }
        megaEarlyShade(0xFFD5DEE1) {
            moveTo(side * 21f, -13f); quadraticTo(side * 32f, 25f, side * 63f, 39f + side * slash)
            quadraticTo(side * 32f, 33f, side * 16f, 11f)
        }
        megaEarlyShape(0xFFEAEFE7) {
            moveTo(side * 7f, 15f); lineTo(side * 19f, 17f)
            lineTo(side * 19f + side * slash, 43f); lineTo(side * 30f + side * slash, 58f)
            lineTo(side * 12f + side * slash, 60f); lineTo(side * 5f + side * slash, 39f)
        }
    }
    kantoPolygon(0xFF64A786, -19f, -5f, 20f, -5f, 19f, 18f, 0f, 28f, -18f, 17f)
    megaEarlyShape(0xFFFAF7EE) {
        moveTo(-10f, -34f); lineTo(12f, -34f); lineTo(18f, -15f)
        lineTo(4f, 8f); lineTo(-5f, 8f); lineTo(-18f, -15f)
    }
    kantoPolygon(0xFFE16B79, -2f, -27f, -14f, -21f, 0f, -7f, 17f, -29f, 5f, -25f)
    for (side in listOf(-1f, 1f)) {
        val lift = side * slash
        kantoLine(KANTO_INK, 9f, side * 15f, -23f, side * 31f, -5f + lift,
            side * 47f, -11f + lift)
        kantoLine(0xFFF0F2E8, 6f, side * 15f, -23f, side * 31f, -5f + lift,
            side * 47f, -11f + lift)
        megaEarlyShape(0xFFE56876) {
            moveTo(side * 31f, -7f + lift); quadraticTo(side * 49f, -22f + lift, side * 72f, -39f + lift)
            quadraticTo(side * 68f, -13f + lift, side * 39f, 9f + lift)
            lineTo(side * 30f, 4f + lift)
        }
        kantoLine(0xFFFFA5AE, 2f, side * 38f, -2f + lift, side * 63f, -28f + lift)
        kantoOval(0xFFECF2E6, side * 48f - 5f, -15f + lift, 10f, 10f)
    }
    megaEarlyShape(0xFFF8F6EC) {
        moveTo(-18f, -56f); quadraticTo(-2f, -67f, 17f, -53f)
        lineTo(21f, -39f); quadraticTo(0f, -19f, -19f, -39f)
    }
    megaEarlyShape(0xFF5CA78A) {
        moveTo(-23f, -43f); quadraticTo(-30f, -58f, -8f, -65f)
        lineTo(4f, -74f); lineTo(8f, -64f); quadraticTo(28f, -63f, 26f, -43f)
        lineTo(10f, -45f); lineTo(1f, -53f); lineTo(-8f, -43f)
    }
    kantoPolygon(0xFFE57483, 0f, -57f, -5f, -69f, 7f, -75f, 10f, -58f)
    kantoPolygon(0xFFEAF1E7, -24f, -45f, -39f, -55f, -34f, -36f, -16f, -37f)
    kantoPolygon(0xFFEAF1E7, 24f, -45f, 39f, -55f, 34f, -36f, 16f, -37f)
    kantoEyes(0f, -40f, 10f, 2.8f)
    kantoSmile(0f, -30f, 8f)
}

private fun DrawScope.megaEarlyHoundoom(m: Float) {
    val prowl = m * 4f
    megaEarlyCurve(KANTO_INK, 7f) {
        moveTo(42f, 6f); cubicTo(78f, 26f, 84f, -15f, 72f, -26f + prowl)
    }
    kantoPolygon(0xFF3D414D, 71f, -21f + prowl, 64f, -38f + prowl,
        78f, -39f + prowl, 82f, -24f + prowl, 76f, -30f + prowl)
    megaEarlyShape(0xFF3F414E) {
        moveTo(-32f, -10f); cubicTo(-7f, -23f, 33f, -19f, 52f, 0f)
        quadraticTo(63f, 27f, 32f, 30f); lineTo(-18f, 24f); lineTo(-36f, 4f)
    }
    megaEarlyShade(0xFF65616A) {
        moveTo(-14f, -13f); quadraticTo(16f, -23f, 43f, -2f)
        lineTo(30f, 3f); quadraticTo(2f, -11f, -14f, -13f)
    }
    for (x in listOf(-20f, 35f)) {
        val step = if (x < 0f) prowl else -prowl
        kantoPolygon(0xFF42434F, x - 7f, 16f, x + 9f, 17f, x + 3f + step, 39f,
            x + 8f + step, 57f, x - 12f + step, 58f, x - 10f + step, 43f)
        kantoPolygon(0xFFD2665B, x - 10f + step, 43f, x + 3f + step, 42f,
            x + 8f + step, 57f, x - 12f + step, 58f)
        kantoLine(0xFFFFE5BC, 2f, x - 5f + step, 52f, x - 5f + step, 57f)
        kantoLine(0xFFFFE5BC, 2f, x + 1f + step, 52f, x + 1f + step, 57f)
    }
    for (x in listOf(-2f, 14f, 29f)) {
        megaEarlyCurve(KANTO_INK, 7f) {
            moveTo(x - 5f, -14f); quadraticTo(x + 10f, -6f, x + 9f, 13f)
        }
        megaEarlyCurve(0xFFE6E0CB, 4f) {
            moveTo(x - 5f, -14f); quadraticTo(x + 10f, -6f, x + 9f, 13f)
        }
    }
    megaEarlyShape(0xFFDEDAC9) {
        moveTo(-36f, -23f); lineTo(-10f, -20f); lineTo(-4f, -8f)
        lineTo(-15f, 4f); lineTo(-17f, 23f); lineTo(-31f, 19f)
        lineTo(-43f, 2f); lineTo(-40f, -10f)
    }
    kantoPolygon(0xFF5B5159, -34f, -7f, -26f, -9f, -27f, 0f, -33f, 3f)
    kantoPolygon(0xFF5B5159, -21f, -8f, -12f, -5f, -16f, 4f, -22f, 1f)
    megaEarlyShape(0xFF43414C) {
        moveTo(-42f, -48f); lineTo(-20f, -46f); lineTo(-12f, -28f)
        lineTo(-26f, -9f); lineTo(-51f, -16f); lineTo(-60f, -28f); lineTo(-46f, -35f)
    }
    kantoPolygon(0xFFB86D52, -47f, -32f, -35f, -28f, -21f, -30f,
        -24f, -17f, -47f, -18f, -57f, -28f)
    for (side in listOf(-1f, 1f)) {
        val x = -31f + side * 13f
        megaEarlyShape(0xFFE2DDCA) {
            moveTo(x, -43f); cubicTo(x + side * 25f, -48f, x + side * 21f, -72f, x + side * 4f, -73f)
            cubicTo(x + side * 11f, -62f, x + side * 8f, -55f, x - side * 4f, -51f)
        }
    }
    kantoPolygon(0xFFE6DFC8, -40f, -48f, -29f, -57f, -21f, -47f, -30f, -37f)
    kantoOval(0xFFE97959, -43f, -37f, 8f, 8f)
    kantoOval(0xFFE97959, -24f, -35f, 7f, 8f)
    kantoEyes(-30f, -32f, 10f, 2.4f)
    kantoPolygon(KANTO_INK, -57f, -29f, -48f, -30f, -49f, -24f, -55f, -24f)
    kantoLine(KANTO_INK, 2f, -45f, -19f, -31f, -17f)
}

private fun DrawScope.megaEarlyAltaria(m: Float) {
    val fluff = m * 6f
    kantoPolygon(0xFF609BC9, -7f, 24f, -24f - fluff, 56f, -4f, 48f, 1f, 61f,
        10f, 47f, 29f + fluff, 52f, 15f, 24f)
    kantoLine(0xFFBCD9E5, 2f, 0f, 33f, 0f, 49f)
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFF8F5E8) {
            moveTo(side * 14f, -4f); cubicTo(side * 14f, -25f, side * 35f, -33f, side * 45f, -23f - fluff)
            cubicTo(side * 51f, -44f - fluff, side * 74f, -44f - fluff, side * 76f, -25f - fluff)
            cubicTo(side * 97f, -25f - fluff, side * 99f, -1f - fluff, side * 86f, 6f - fluff)
            cubicTo(side * 98f, 24f, side * 77f, 38f, side * 63f, 27f)
            cubicTo(side * 51f, 45f, side * 30f, 33f, side * 33f, 19f)
            quadraticTo(side * 11f, 21f, side * 14f, -4f)
        }
        megaEarlyShade(0xFFDCE5E6) {
            moveTo(side * 37f, 9f); quadraticTo(side * 54f, 26f, side * 67f, 17f)
            quadraticTo(side * 83f, 30f, side * 87f, 12f)
            quadraticTo(side * 92f, 31f, side * 65f, 27f)
            quadraticTo(side * 48f, 41f, side * 37f, 24f)
        }
        megaEarlyCurve(0xFFDBE2E3, 2f) {
            moveTo(side * 40f, -6f - fluff); quadraticTo(side * 48f, -21f - fluff, side * 61f, -13f - fluff)
            moveTo(side * 71f, 0f - fluff); quadraticTo(side * 80f, -9f - fluff, side * 87f, -1f - fluff)
        }
    }
    megaEarlyShape(0xFF71AED2) {
        moveTo(-12f, -31f); cubicTo(-25f, -10f, -29f, 25f, -10f, 34f)
        quadraticTo(17f, 45f, 29f, 20f); quadraticTo(35f, 5f, 12f, -21f)
    }
    megaEarlyShape(0xFFF8F5E9) {
        moveTo(-23f, -13f); quadraticTo(-37f, -9f, -26f, 6f)
        quadraticTo(-39f, 18f, -23f, 24f); quadraticTo(-23f, 40f, -6f, 35f)
        quadraticTo(5f, 47f, 17f, 34f); quadraticTo(34f, 33f, 31f, 17f)
        quadraticTo(44f, 4f, 24f, -5f); lineTo(5f, -14f)
    }
    megaEarlyShape(0xFF83BEDB) {
        moveTo(-17f, -36f); quadraticTo(0f, -55f, 17f, -38f)
        quadraticTo(30f, -21f, 13f, -5f); quadraticTo(-8f, 0f, -20f, -15f)
    }
    megaEarlyShape(0xFFF8F4E7) {
        moveTo(-19f, -36f); cubicTo(-35f, -41f, -29f, -60f, -15f, -58f)
        cubicTo(-14f, -75f, 8f, -76f, 13f, -61f)
        cubicTo(29f, -65f, 37f, -46f, 21f, -37f)
        quadraticTo(8f, -29f, -4f, -37f); quadraticTo(-14f, -29f, -19f, -36f)
    }
    megaEarlyCurve(0xFF79B0D3, 3f) {
        moveTo(-8f, -52f); quadraticTo(-24f, -62f, -30f, -68f + fluff * 0.5f)
        moveTo(3f, -53f); quadraticTo(15f, -65f, 31f, -65f - fluff * 0.5f)
    }
    kantoEyes(0f, -23f, 11f, 3f)
    kantoPolygon(0xFFAAA898, -5f, -18f, 6f, -18f, 0f, -9f)
    kantoOval(0xFFF0D7C7, -20f, -19f, 8f, 5f)
    kantoOval(0xFFF0D7C7, 12f, -19f, 8f, 5f)
}

private fun DrawScope.megaEarlyAudino(m: Float) {
    val ribbon = m * 5f
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFF2D4D9) {
            moveTo(side * 11f, 27f); lineTo(side * 27f, 27f)
            lineTo(side * 32f, 53f - side * ribbon)
            quadraticTo(side * 25f, 65f - side * ribbon * 0.5f, side * 10f, 56f - side * ribbon)
        }
    }
    megaEarlyShape(0xFFFCF5E9) {
        moveTo(-18f, -15f); quadraticTo(-31f, -6f, -27f, 12f)
        lineTo(-42f, 34f); lineTo(-29f, 34f); lineTo(-27f, 45f)
        lineTo(-12f, 40f); quadraticTo(0f, 51f, 13f, 41f)
        lineTo(29f, 45f); lineTo(29f, 32f); lineTo(42f, 33f)
        lineTo(28f, 11f); quadraticTo(32f, -9f, 17f, -15f)
    }
    megaEarlyShade(0xFFE9E2DC) {
        moveTo(15f, 2f); quadraticTo(18f, 22f, 28f, 37f)
        lineTo(17f, 33f); quadraticTo(7f, 41f, 0f, 42f); lineTo(5f, 18f)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFFF8F2E8) {
            moveTo(side * 21f, -9f); quadraticTo(side * 39f, -15f, side * 43f, 4f + side * ribbon)
            lineTo(side * 50f, 12f + side * ribbon); lineTo(side * 37f, 19f + side * ribbon)
            quadraticTo(side * 24f, 14f, side * 21f, -9f)
        }
        kantoOval(0xFFF0C3CC, side * 44f - 6f, 5f + side * ribbon, 12f, 11f)
        megaEarlyShape(0xFFF8F2E7) {
            moveTo(side * 17f, -42f); cubicTo(side * 35f, -63f, side * 58f, -61f + side * ribbon, side * 62f, -42f + side * ribbon)
            cubicTo(side * 65f, -25f, side * 42f, -16f, side * 26f, -29f)
        }
        megaEarlyShape(0xFFE3ABC0) {
            moveTo(side * 30f, -40f); quadraticTo(side * 44f, -55f, side * 54f, -45f + side * ribbon)
            quadraticTo(side * 53f, -27f, side * 34f, -31f)
        }
        megaEarlyCurve(0xFFF6EEDF, 6f) {
            moveTo(side * 34f, -26f); cubicTo(side * 57f, -13f, side * 69f, 7f + side * ribbon, side * 53f, 12f + side * ribbon)
        }
        kantoOval(0xFFDAB2C5, side * 52f - 7f, 7f + side * ribbon, 14f, 11f)
    }
    megaEarlyShape(0xFFFAF5E9) {
        moveTo(-24f, -41f); quadraticTo(-23f, -62f, -4f, -58f)
        quadraticTo(12f, -69f, 18f, -50f); quadraticTo(34f, -38f, 26f, -20f)
        quadraticTo(16f, -2f, -5f, -8f); quadraticTo(-36f, -10f, -24f, -41f)
    }
    megaEarlyShape(0xFFF1CBD0) {
        moveTo(-22f, -28f); quadraticTo(-8f, -40f, 1f, -27f)
        quadraticTo(15f, -40f, 24f, -26f); quadraticTo(23f, -8f, 0f, -9f)
        quadraticTo(-23f, -8f, -22f, -28f)
    }
    kantoOval(0xFF8CB9CE, -17f, -28f, 10f, 11f)
    kantoOval(0xFF8CB9CE, 7f, -28f, 10f, 11f)
    kantoEyes(0f, -23f, 12f, 2.7f)
    kantoSmile(0f, -13f, 10f)
    kantoPolygon(0xFFE6B3C5, 0f, -2f, -17f, -8f, -15f, 7f, 0f, 3f,
        15f, 7f, 17f, -8f)
    kantoOval(0xFFFFFBEE, -5f, -4f, 10f, 10f)
    kantoLine(0xFFD4C5C4, 1.5f, -8f, 12f, -8f, 27f)
    kantoLine(0xFFD4C5C4, 1.5f, 8f, 12f, 8f, 27f)
}

private fun DrawScope.megaEarlyLopunny(m: Float) {
    val kick = m * 6f
    kantoOval(0xFFF4E4CD, 15f, 17f, 22f, 22f)
    for (side in listOf(-1f, 1f)) {
        val sway = side * kick
        megaEarlyShape(0xFF5C4850) {
            moveTo(side * 8f, -34f)
            cubicTo(side * 25f, -66f, side * 50f, -74f + sway * 0.4f, side * 61f, -53f)
            cubicTo(side * 77f, -28f, side * 59f, -11f + sway, side * 42f, -16f + sway)
            quadraticTo(side * 61f, -31f, side * 42f, -44f)
            quadraticTo(side * 30f, -48f, side * 17f, -24f)
        }
        megaEarlyShape(0xFFF3D9B9) {
            moveTo(side * 27f, -53f); quadraticTo(side * 44f, -67f, side * 57f, -52f)
            lineTo(side * 60f, -38f); quadraticTo(side * 46f, -49f, side * 35f, -39f)
        }
        kantoLine(0xFFD7B890, 2f, side * 33f, -50f, side * 41f, -54f, side * 49f, -49f)
    }
    megaEarlyShape(0xFF674F56) {
        moveTo(-12f, -16f); quadraticTo(-21f, -8f, -10f, 12f)
        lineTo(-14f, 24f); quadraticTo(0f, 35f, 14f, 22f)
        lineTo(9f, 10f); quadraticTo(20f, -12f, 11f, -19f)
    }
    megaEarlyShape(0xFFF1D8B5) {
        moveTo(-7f, -13f); lineTo(7f, -13f); quadraticTo(2f, -1f, 7f, 11f)
        lineTo(0f, 20f); lineTo(-7f, 11f); quadraticTo(-1f, 0f, -7f, -13f)
    }
    for (side in listOf(-1f, 1f)) {
        val step = side * kick
        megaEarlyShape(0xFF59454E) {
            moveTo(side * 5f, 19f); quadraticTo(side * 29f, 10f, side * 29f, 32f - step)
            lineTo(side * 17f, 43f - step); lineTo(side * 31f, 55f - step)
            quadraticTo(side * 30f, 63f - step * 0.25f, side * 10f, 59f - step * 0.25f)
            lineTo(side * 4f, 41f - step); lineTo(side * 9f, 30f)
        }
        kantoPolygon(0xFFF3DDC2, side * 14f, 23f - step * 0.3f, side * 24f, 24f - step * 0.3f,
            side * 19f, 32f - step * 0.3f, side * 8f, 31f - step * 0.3f)
        kantoPolygon(0xFFF3DDC2, side * 10f, 34f - step, side * 20f, 37f - step,
            side * 17f, 43f - step, side * 6f, 39f - step)
        megaEarlyCurve(KANTO_INK, 8f) {
            moveTo(side * 12f, -9f); lineTo(side * 30f, 7f + step); lineTo(side * 43f, -8f + step)
        }
        megaEarlyCurve(0xFF6D5359, 5f) {
            moveTo(side * 12f, -9f); lineTo(side * 30f, 7f + step); lineTo(side * 43f, -8f + step)
        }
        kantoOval(0xFFF1DCC0, side * 43f - 9f, -17f + step, 18f, 20f)
    }
    megaEarlyShape(0xFF78595D) {
        moveTo(-19f, -38f); quadraticTo(0f, -52f, 19f, -37f)
        lineTo(23f, -22f); quadraticTo(10f, -7f, -1f, -11f)
        quadraticTo(-23f, -13f, -23f, -28f)
    }
    kantoOval(0xFFE6ADC0, -17f, -31f, 10f, 9f)
    kantoOval(0xFFE6ADC0, 7f, -31f, 10f, 9f)
    kantoEyes(0f, -27f, 12f, 2.7f)
    kantoOval(0xFFF1DCC4, -11f, -23f, 22f, 12f)
    kantoPolygon(0xFFB7757C, -4f, -23f, 4f, -23f, 0f, -19f)
    kantoSmile(0f, -16f, 9f)
    kantoLine(0xFFF0DABC, 4f, -19f, -38f, -12f, -39f)
    kantoLine(0xFFF0DABC, 4f, 12f, -39f, 19f, -38f)
}

private fun DrawScope.megaEarlyBanette(m: Float) {
    val haunt = m * 5f
    megaEarlyShape(0xFF554A65) {
        moveTo(-14f, 22f); lineTo(-29f, 42f); lineTo(-45f - haunt, 44f)
        lineTo(-38f - haunt, 56f); lineTo(-18f, 50f); lineTo(-3f, 33f)
    }
    megaEarlyShape(0xFF554A65) {
        moveTo(5f, 24f); lineTo(19f, 42f); lineTo(34f + haunt, 39f)
        lineTo(43f + haunt, 49f); lineTo(22f, 55f); lineTo(1f, 38f)
    }
    megaEarlyShape(0xFF675A78) {
        moveTo(-22f, -17f); quadraticTo(-36f, 1f, -22f, 29f)
        quadraticTo(-5f, 43f, 22f, 30f); quadraticTo(36f, 9f, 19f, -19f)
    }
    megaEarlyShade(0xFF8A7795) {
        moveTo(-17f, -9f); quadraticTo(-27f, 10f, -13f, 29f)
        lineTo(-4f, 30f); quadraticTo(-13f, 9f, -8f, -9f)
    }
    kantoPolygon(0xFFE678A3, -5f, -2f, 6f, -2f, 11f, 23f, 0f, 36f, -9f, 21f)
    kantoLine(0xFFE6C575, 3f, -7f, 1f, -10f, 23f, -2f, 32f)
    kantoLine(0xFFE6C575, 3f, 8f, 1f, 13f, 22f, 4f, 33f)
    for (side in listOf(-1f, 1f)) {
        val reach = side * haunt
        megaEarlyShape(0xFF67576F) {
            moveTo(side * 21f, -13f); quadraticTo(side * 47f, -10f, side * 49f, 10f + reach)
            lineTo(side * 43f, 22f + reach); lineTo(side * 28f, 9f); lineTo(side * 17f, 2f)
        }
        megaEarlyShape(0xFFE777A2) {
            moveTo(side * 40f, 6f + reach); lineTo(side * 52f, 1f + reach)
            lineTo(side * 73f, -8f + reach); lineTo(side * 71f, 2f + reach)
            lineTo(side * 60f, 12f + reach); lineTo(side * 82f, 11f + reach)
            lineTo(side * 77f, 20f + reach); lineTo(side * 60f, 24f + reach)
            lineTo(side * 72f, 34f + reach); lineTo(side * 63f, 37f + reach)
            lineTo(side * 44f, 24f + reach)
        }
        kantoLine(0xFFE8CB7D, 4f, side * 40f, 5f + reach, side * 49f, 23f + reach)
        for (i in 0..3) kantoLine(0xFF765D53, 1.5f, side * (41f + i * 2f), 7f + i * 4f + reach,
            side * (45f + i * 2f), 5f + i * 4f + reach)
    }
    megaEarlyShape(0xFF62516E) {
        moveTo(-30f, -42f); lineTo(-46f, -61f + haunt); lineTo(-25f, -56f)
        quadraticTo(-9f, -73f, 7f, -61f); lineTo(26f, -68f - haunt * 0.5f)
        lineTo(22f, -45f); quadraticTo(40f, -38f, 32f, -17f)
        quadraticTo(3f, 1f, -29f, -16f); quadraticTo(-42f, -27f, -30f, -42f)
    }
    kantoPolygon(0xFFDE6B9B, -18f, -48f, -7f, -66f, -4f, -53f, 6f, -64f,
        7f, -46f, 19f, -58f, 15f, -39f)
    kantoLine(0xFFECCB71, 3f, -19f, -48f, -3f, -44f, 15f, -40f)
    kantoPolygon(0xFFDD687D, -27f, -35f, -10f, -30f, -20f, -22f)
    kantoPolygon(0xFFDD687D, 9f, -30f, 27f, -36f, 21f, -22f)
    kantoEyes(0f, -29f, 19f, 2.6f)
    kantoLine(0xFFECCB76, 4f, -23f, -15f, 0f, -10f, 24f, -16f)
    for (i in -3..3) kantoLine(0xFF76634F, 1.5f, i * 6f, -14f, i * 6f - 1f, -8f)
    kantoOval(0xFFF1D17C, 24f, -18f, 9f, 12f)
    kantoOval(0xFF65526B, 27f, -15f, 3f, 5f)
}

private fun DrawScope.megaEarlyCamerupt(m: Float) {
    val shag = m * 3f
    megaEarlyShape(0xFF864950) {
        moveTo(-32f, 18f); lineTo(54f, 19f); lineTo(57f, 50f)
        lineTo(37f, 54f); lineTo(31f, 35f); lineTo(-9f, 35f)
        lineTo(-17f, 54f); lineTo(-39f, 50f)
    }
    megaEarlyShape(0xFFB7604E) {
        moveTo(-55f, -9f); cubicTo(-32f, -37f, 43f, -34f, 67f, -7f)
        lineTo(71f, 18f); lineTo(80f, 26f + shag); lineTo(66f, 28f)
        lineTo(66f, 46f); lineTo(50f, 40f); lineTo(43f, 59f)
        lineTo(25f, 50f); lineTo(13f, 59f); lineTo(0f, 50f)
        lineTo(-16f, 59f); lineTo(-29f, 45f); lineTo(-44f, 49f)
        lineTo(-47f, 31f); lineTo(-64f, 30f); lineTo(-58f, 15f)
    }
    megaEarlyShade(0xFFDD865E) {
        moveTo(-42f, -9f); cubicTo(-19f, -27f, 25f, -28f, 54f, -5f)
        lineTo(42f, 20f); lineTo(28f, 13f); lineTo(12f, 31f)
        lineTo(-2f, 16f); lineTo(-16f, 27f); lineTo(-29f, 10f)
    }
    for (x in listOf(-29f, -7f, 17f, 43f)) {
        megaEarlyCurve(0xFF9A4E48, 2f) {
            moveTo(x, 23f); quadraticTo(x + 5f + shag, 34f, x - 2f + shag, 45f)
        }
    }
    // The two ordinary humps merge into one broad, craggy volcano.
    megaEarlyShape(0xFF62534E) {
        moveTo(-23f, -18f); lineTo(-15f, -44f); lineTo(-3f, -58f)
        lineTo(29f, -58f); lineTo(44f, -43f); lineTo(55f, -13f)
        lineTo(34f, -7f); lineTo(17f, -14f); lineTo(-1f, -7f)
    }
    kantoPolygon(0xFF887464, -17f, -24f, -10f, -44f, 0f, -53f, 8f, -32f, 0f, -15f)
    kantoPolygon(0xFF403D41, 24f, -52f, 37f, -43f, 47f, -17f, 32f, -14f, 27f, -34f)
    kantoOval(0xFF41383C, -4f, -64f, 36f, 15f)
    kantoOval(0xFFE8784B, 1f, -61f, 26f, 8f)
    kantoLine(0xFFFFC55F, 2f, 5f, -57f, 13f + shag, -57f, 21f, -56f)
    kantoPolygon(0xFFEFA14F, 8f, -52f, 18f, -53f, 17f, -34f, 24f, -25f,
        16f, -19f, 9f, -32f)
    megaEarlyShape(0xFFAE5F4F) {
        moveTo(-58f, -29f); quadraticTo(-77f, -25f, -75f, -7f)
        lineTo(-81f, 11f); quadraticTo(-78f, 29f, -51f, 24f)
        quadraticTo(-32f, 16f, -39f, -6f); lineTo(-43f, -25f)
    }
    kantoPolygon(0xFFB45E4D, -70f, -24f, -82f, -31f - shag, -88f, -21f - shag, -72f, -10f)
    kantoPolygon(0xFFB45E4D, -42f, -23f, -29f, -33f + shag, -24f, -19f + shag, -39f, -10f)
    kantoPolygon(0xFFE3D1AD, -67f, -27f, -62f, -42f, -55f, -26f)
    kantoPolygon(0xFFE3D1AD, -46f, -25f, -40f, -39f, -38f, -19f)
    kantoOval(0xFFC38965, -79f, 2f, 32f, 20f)
    kantoEyes(-58f, -7f, 12f, 3f)
    kantoOval(0xFF65484A, -72f, 8f, 5f, 5f)
    kantoOval(0xFF65484A, -58f, 8f, 5f, 5f)
    kantoSmile(-65f, 17f, 13f)
    kantoPolygon(0xFF6C5260, -55f, -21f, -49f, -16f, -52f, -10f, -59f, -15f)
}

private fun DrawScope.megaEarlyExcadrill(m: Float) {
    val dig = m * 5f
    for (side in listOf(-1f, 1f)) {
        kantoPolygon(0xFF63464C, side * 7f, 28f, side * 27f, 26f,
            side * 33f, 51f - side * dig, side * 13f, 57f - side * dig)
        for (toe in 0..2) kantoPolygon(0xFFD6DDE0,
            side * (13f + toe * 6f), 50f - side * dig,
            side * (17f + toe * 6f), 61f - side * dig * 0.5f,
            side * (21f + toe * 6f), 49f - side * dig)
    }
    megaEarlyShape(0xFF64484E) {
        moveTo(-23f, -24f); cubicTo(-46f, -5f, -37f, 31f, -16f, 42f)
        quadraticTo(5f, 52f, 28f, 35f); cubicTo(47f, 15f, 28f, -17f, 13f, -25f)
    }
    kantoPolygon(0xFFC45B62, -24f, -12f, -8f, -4f, -18f, 8f, -4f, 21f,
        -15f, 31f, -31f, 10f, -22f, 0f)
    kantoPolygon(0xFFC45B62, 20f, -8f, 10f, 3f, 21f, 14f, 8f, 31f,
        22f, 27f, 32f, 10f)
    for (side in listOf(-1f, 1f)) {
        val swing = side * dig
        megaEarlyShape(0xFF76545A) {
            moveTo(side * 21f, -14f); quadraticTo(side * 41f, -26f, side * 50f, -2f + swing)
            lineTo(side * 45f, 21f + swing); lineTo(side * 26f, 20f)
        }
        megaEarlyShape(0xFFB9C2CA) {
            moveTo(side * 37f, -10f + swing); lineTo(side * 53f, -24f + swing)
            lineTo(side * 64f, -10f + swing); lineTo(side * 71f, -12f + swing)
            lineTo(side * 73f, 3f + swing); lineTo(side * 81f, 1f + swing)
            lineTo(side * 80f, 18f + swing); lineTo(side * 89f, 19f + swing)
            lineTo(side * 72f, 50f + swing); lineTo(side * 43f, 28f + swing)
        }
        megaEarlyShade(0xFFE1E6E7) {
            moveTo(side * 44f, -5f + swing); lineTo(side * 53f, -16f + swing)
            lineTo(side * 63f, 2f + swing); lineTo(side * 69f, 42f + swing)
            lineTo(side * 54f, 21f + swing)
        }
        for (i in 0..2) {
            val y = -4f + i * 14f + swing
            kantoPolygon(0xFFD99058, side * (43f + i * 5f), y,
                side * (64f + i * 7f), y + 7f, side * (67f + i * 6f), y + 14f,
                side * (45f + i * 6f), y + 6f)
            kantoLine(0xFF92574D, 1.5f, side * (47f + i * 5f), y + 4f,
                side * (64f + i * 6f), y + 10f)
        }
    }
    megaEarlyShape(0xFFD1D9DE) {
        moveTo(-25f, -21f); lineTo(-26f, -36f); lineTo(-18f, -35f)
        lineTo(-17f, -48f); lineTo(-9f, -46f); lineTo(-7f, -60f)
        lineTo(0f, -74f); lineTo(9f, -57f); lineTo(9f, -45f)
        lineTo(18f, -46f); lineTo(18f, -32f); lineTo(28f, -33f)
        lineTo(24f, -18f)
    }
    kantoPolygon(0xFFF0F0E7, 0f, -70f, 3f, -40f, -3f, -24f, -13f, -30f)
    kantoLine(0xFFD7965B, 5f, -14f, -45f, 8f, -38f, 15f, -39f)
    kantoLine(0xFFD7965B, 5f, -22f, -31f, -2f, -23f, 20f, -25f)
    megaEarlyShape(0xFFE4C1A2) {
        moveTo(-25f, -18f); quadraticTo(-4f, -29f, 23f, -17f)
        lineTo(16f, -2f); lineTo(-2f, 8f); lineTo(-18f, -2f)
    }
    kantoEyes(-1f, -14f, 14f, 3f)
    kantoOval(0xFFC55C68, -10f, -8f, 18f, 11f)
    kantoLine(KANTO_INK, 2f, -11f, 2f, -2f, 6f, 9f, 1f)
}

private fun DrawScope.megaEarlyGarchomp(m: Float) {
    val scythe = m * 6f
    megaEarlyShape(0xFF596888) {
        moveTo(12f, 26f); cubicTo(43f, 30f, 62f, 47f, 88f, 23f + scythe * 0.5f)
        lineTo(79f, 47f); quadraticTo(51f, 61f, 23f, 42f)
    }
    for (side in listOf(-1f, 1f)) {
        megaEarlyShape(0xFF617291) {
            moveTo(side * 9f, 20f); quadraticTo(side * 36f, 17f, side * 36f, 36f)
            lineTo(side * 25f + side * m * 3f, 50f)
            lineTo(side * 34f + side * m * 3f, 59f)
            lineTo(side * 8f + side * m * 3f, 61f); lineTo(side * 9f, 42f)
        }
        for (toe in 0..2) kantoPolygon(0xFFE5DBBF, side * (10f + toe * 7f) + side * m * 3f, 55f,
            side * (14f + toe * 7f) + side * m * 3f, 63f,
            side * (18f + toe * 7f) + side * m * 3f, 55f)
    }
    kantoPolygon(0xFF52617F, -13f, -25f, -15f, -52f, 5f, -40f, 21f, -24f)
    megaEarlyShape(0xFF637797) {
        moveTo(-19f, -24f); cubicTo(-37f, -10f, -22f, 24f, -12f, 35f)
        quadraticTo(7f, 48f, 27f, 29f); quadraticTo(33f, 0f, 17f, -24f)
    }
    kantoPolygon(0xFFCB695E, -15f, -19f, 15f, -20f, 21f, -1f, 11f, 28f,
        0f, 39f, -12f, 23f, -19f, 0f)
    kantoPolygon(0xFFDCC17B, -13f, 17f, 0f, 22f, 13f, 17f, 5f, 34f, -1f, 39f)
    for (side in listOf(-1f, 1f)) {
        val chop = side * scythe
        kantoPolygon(0xFF647896, side * 16f, -22f, side * 36f, -23f,
            side * 50f, -8f + chop, side * 43f, 6f + chop, side * 23f, -2f)
        // Solid forearm scythes distinguish this grounded Mega from Z's flight wings.
        megaEarlyShape(0xFFC5B28F) {
            moveTo(side * 39f, -12f + chop); lineTo(side * 61f, -20f + chop)
            quadraticTo(side * 84f, 0f + chop, side * 85f, 35f + chop)
            lineTo(side * 74f, 47f + chop); quadraticTo(side * 73f, 16f + chop, side * 47f, 10f + chop)
            lineTo(side * 34f, 7f + chop)
        }
        megaEarlyShade(0xFFEAD9B3) {
            moveTo(side * 55f, -13f + chop); quadraticTo(side * 77f, 6f + chop, side * 79f, 34f + chop)
            quadraticTo(side * 70f, 7f + chop, side * 47f, 3f + chop)
        }
        kantoPolygon(0xFFE2D8BE, side * 26f, -8f, side * 37f, -1f, side * 29f, 7f)
        kantoPolygon(0xFFE2D8BE, side * 24f, 3f, side * 33f, 12f, side * 24f, 17f)
    }
    megaEarlyShape(0xFF647999) {
        moveTo(-22f, -48f); lineTo(-43f, -54f); lineTo(-44f, -37f)
        lineTo(-26f, -33f); lineTo(-23f, -20f); lineTo(-4f, -11f)
        lineTo(18f, -21f); lineTo(25f, -32f); lineTo(43f, -36f)
        lineTo(43f, -54f); lineTo(20f, -48f); lineTo(0f, -58f)
    }
    kantoPolygon(0xFF8EA1B8, -37f, -49f, -29f, -46f, -28f, -37f, -37f, -40f)
    kantoPolygon(0xFF8EA1B8, 28f, -46f, 36f, -49f, 37f, -40f, 28f, -36f)
    kantoPolygon(0xFFE8BF69, -6f, -50f, 6f, -50f, 0f, -37f)
    kantoPolygon(0xFFF5EBCD, -19f, -32f, -4f, -28f, -10f, -24f)
    kantoPolygon(0xFFF5EBCD, 4f, -28f, 20f, -33f, 12f, -24f)
    kantoEyes(0f, -28f, 12f, 2.5f)
    kantoLine(KANTO_INK, 2f, -16f, -18f, -3f, -15f, 15f, -20f)
    kantoPolygon(0xFFFFEED0, 4f, -17f, 8f, -10f, 12f, -19f)
}

private fun DrawScope.megaEarlyGarchompZ(m: Float) {
    val sweep = m * 5f
    megaEarlyShape(0xFF8F4E7B) {
        moveTo(5f, 12f); cubicTo(16f, 34f, 45f, 28f, 62f, 45f + sweep)
        quadraticTo(36f, 37f, 30f, 49f); quadraticTo(1f, 54f, -9f, 21f)
    }
    kantoPolygon(0xFFBD7198, 32f, 31f, 40f, 24f, 49f, 37f, 62f, 45f + sweep, 40f, 39f)
    for (side in listOf(-1f, 1f)) {
        val wing = side * sweep
        // Z has an aerial frame, head fins, and broad hooked wings with extra talons.
        megaEarlyShape(0xFFA85A8D) {
            moveTo(side * 11f, -22f); quadraticTo(side * 31f, -29f, side * 46f, -40f + wing)
            lineTo(side * 74f, -68f + wing); lineTo(side * 79f, -43f + wing)
            quadraticTo(side * 98f, -21f + wing, side * 99f, 19f + wing)
            lineTo(side * 87f, 10f + wing); lineTo(side * 80f, 25f + wing)
            lineTo(side * 73f, 5f + wing); lineTo(side * 62f, 13f + wing)
            quadraticTo(side * 52f, -5f + wing, side * 27f, -2f); lineTo(side * 13f, 9f)
        }
        megaEarlyShade(0xFFD28AAF) {
            moveTo(side * 22f, -20f); quadraticTo(side * 51f, -35f, side * 70f, -58f + wing)
            lineTo(side * 70f, -32f + wing); quadraticTo(side * 86f, -12f + wing, side * 92f, 7f + wing)
            quadraticTo(side * 69f, -21f + wing, side * 44f, -15f); lineTo(side * 27f, -9f)
        }
        megaEarlyShape(0xFF3D3C50) {
            moveTo(side * 69f, -38f + wing); quadraticTo(side * 95f, -28f + wing, side * 99f, 19f + wing)
            lineTo(side * 87f, 10f + wing); quadraticTo(side * 85f, -11f + wing, side * 69f, -20f + wing)
        }
        kantoPolygon(0xFF444052, side * 61f, -9f + wing, side * 76f, -5f + wing,
            side * 80f, 25f + wing, side * 71f, 8f + wing)
        kantoPolygon(0xFF444052, side * 47f, -11f + wing, side * 61f, -6f + wing,
            side * 62f, 13f + wing, side * 53f, 2f + wing)
        kantoLine(0xFFEDAFCE, 2f, side * 30f, -18f, side * 53f, -30f + wing, side * 66f, -46f + wing)
    }
    megaEarlyShape(0xFFAF648F) {
        moveTo(-14f, -30f); quadraticTo(-28f, -10f, -14f, 17f)
        quadraticTo(-5f, 32f, 9f, 22f); quadraticTo(25f, 7f, 16f, -27f)
    }
    kantoPolygon(0xFF363A4E, -9f, -25f, 10f, -25f, 15f, -7f, 5f, 23f,
        -4f, 21f, -17f, -3f)
    kantoPolygon(0xFFE6BE75, -5f, -20f, 5f, -20f, 1f, -9f)
    for (side in listOf(-1f, 1f)) {
        val tuck = side * m * 3f
        kantoPolygon(0xFFAA5E89, side * 7f, 15f, side * 21f, 14f,
            side * 29f, 29f + tuck, side * 21f, 34f + tuck, side * 10f, 25f)
        kantoPolygon(0xFF3C3B4E, side * 20f, 27f + tuck, side * 30f, 27f + tuck,
            side * 32f, 40f + tuck, side * 26f, 35f + tuck, side * 20f, 39f + tuck)
    }
    megaEarlyShape(0xFFB66C97) {
        moveTo(-17f, -50f); lineTo(-35f, -56f); lineTo(-30f, -41f)
        lineTo(-18f, -35f); lineTo(-18f, -25f); lineTo(-1f, -17f)
        lineTo(16f, -27f); lineTo(19f, -36f); lineTo(35f, -44f)
        lineTo(37f, -61f); lineTo(16f, -52f); lineTo(0f, -62f)
    }
    kantoPolygon(0xFFDB95B8, -19f, -43f, -43f, -49f, -31f, -29f, -15f, -29f)
    kantoPolygon(0xFFDB95B8, 18f, -43f, 44f, -51f, 32f, -31f, 15f, -29f)
    kantoLine(0xFF70486D, 2f, -31f, -42f, -24f, -34f)
    kantoLine(0xFF70486D, 2f, 31f, -43f, 24f, -35f)
    kantoPolygon(0xFFECCB7D, -5f, -54f, 5f, -54f, 0f, -43f)
    kantoPolygon(0xFFF2E8CA, -15f, -36f, -5f, -32f, -11f, -28f)
    kantoPolygon(0xFFF2E8CA, 5f, -32f, 16f, -37f, 11f, -28f)
    kantoEyes(0f, -32f, 11f, 2.2f)
    kantoLine(KANTO_INK, 2f, -11f, -23f, -1f, -21f, 10f, -25f)
}
