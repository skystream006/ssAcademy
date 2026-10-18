package com.ssmath.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal fun DrawScope.drawMegaMiddle(celebration: Celebration, motion: Float) {
    val m = motion.coerceIn(-1f, 1f)
    when (celebration) {
        Celebration.MEGA_SABLEYE -> middleSableye(m)
        Celebration.MEGA_MAWILE -> middleMawile(m)
        Celebration.MEGA_ABSOL -> middleAbsol(m)
        Celebration.MEGA_ABSOL_Z -> middleAbsolZ(m)
        Celebration.MEGA_LUCARIO -> middleLucario(m)
        Celebration.MEGA_LUCARIO_Z -> middleLucarioZ(m)
        Celebration.MEGA_SLOWBRO -> middleSlowbro(m)
        Celebration.MEGA_SHARPEDO -> middleSharpedo(m)
        Celebration.MEGA_EELEKTROSS -> middleEelektross(m)
        Celebration.MEGA_DRAGONITE -> middleDragonite(m)
        Celebration.MEGA_VENUSAUR -> middleVenusaur(m)
        Celebration.MEGA_CHARIZARD_X -> middleCharizardX(m)
        Celebration.MEGA_CHARIZARD_Y -> middleCharizardY(m)
        Celebration.MEGA_BLASTOISE -> middleBlastoise(m)
        Celebration.MEGA_MALAMAR -> middleMalamar(m)
        Celebration.MEGA_DRAGALGE -> middleDragalge(m)
        Celebration.MEGA_GLALIE -> middleGlalie(m)
        Celebration.MEGA_FROSLASS -> middleFroslass(m)
        Celebration.MEGA_ABOMASNOW -> middleAbomasnow(m)
        Celebration.MEGA_SCIZOR -> middleScizor(m)
        Celebration.MEGA_PINSIR -> middlePinsir(m)
        Celebration.MEGA_HERACROSS -> middleHeracross(m)
        Celebration.MEGA_HAWLUCHA -> middleHawlucha(m)
        Celebration.MEGA_SCRAFTY -> middleScrafty(m)
        Celebration.MEGA_CHANDELURE -> middleChandelure(m)
        Celebration.MEGA_AERODACTYL -> middleAerodactyl(m)
        Celebration.MEGA_STEELIX -> middleSteelix(m)
        Celebration.MEGA_AGGRON -> middleAggron(m)
        Celebration.MEGA_TYRANITAR -> middleTyranitar(m)
        Celebration.MEGA_GRENINJA -> middleGreninja(m)
        Celebration.MEGA_FALINKS -> middleFalinks(m)
        Celebration.MEGA_CHESNAUGHT -> middleChesnaught(m)
        else -> error("No middle Mega artwork for $celebration")
    }
}

private fun DrawScope.middleShape(color: Long, contour: Path.() -> Unit) {
    val path = Path().apply(contour).apply { close() }
    drawPath(path, Color(color))
    drawPath(path, Color(KANTO_INK), style = Stroke(2f, join = StrokeJoin.Round))
}

private fun DrawScope.middleCurve(color: Long, width: Float, contour: Path.() -> Unit) {
    drawPath(Path().apply(contour), Color(color),
        style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.middleGem(color: Long, x: Float, y: Float, w: Float, h: Float) {
    kantoPolygon(color, x, y - h, x + w, y, x, y + h, x - w, y)
    kantoLine(0xBFFFFFFF, 1.4f, x - w, y, x, y - h * .35f, x + w, y)
    kantoLine(0xBFFFFFFF, 1.4f, x, y - h, x, y + h)
}

private fun DrawScope.middleClaws(x: Float, y: Float, side: Float = 1f) {
    for (i in 0..2) {
        val dx = x + side * i * 5f
        kantoPolygon(0xFFF7F2DB, dx, y, dx + side * 2f, y - 5f, dx + side * 4f, y + 1f)
    }
}

private fun DrawScope.middleFlame(x: Float, y: Float, w: Float, h: Float, m: Float, color: Long) {
    middleShape(color) {
        moveTo(x, y)
        cubicTo(x - w, y - h * .2f, x - w * .5f, y - h * .6f, x - w * .65f, y - h * .85f)
        lineTo(x - w * .1f, y - h * .6f)
        quadraticTo(x + m * 5f, y - h * .85f, x + w * .2f + m * 3f, y - h)
        cubicTo(x + w * .15f, y - h * .5f, x + w, y - h * .35f, x + w * .6f, y - h * .1f)
        quadraticTo(x + w * .3f, y + 2f, x, y)
    }
    middleShape(0xFFD3FCFF) {
        moveTo(x, y - 2f)
        quadraticTo(x - w * .35f, y - h * .2f, x + w * .1f, y - h * .55f)
        quadraticTo(x + w * .5f, y - h * .13f, x, y - 2f)
    }
}

private fun DrawScope.middleSableye(m: Float) {
    val reach = m * 4f
    middleShape(0xFF756096) {
        moveTo(-47f, 2f)
        quadraticTo(-67f, 18f, -59f, 41f)
        lineTo(-71f, 56f + reach)
        lineTo(-49f, 56f + reach)
        lineTo(-40f, 34f)
        lineTo(-26f, 49f - reach)
        lineTo(-7f, 48f - reach)
        lineTo(-23f, 17f)
        quadraticTo(-24f, 1f, -47f, 2f)
    }
    kantoPolygon(0xFF614F86, -58f, -28f, -72f, -51f, -41f, -36f,
        -23f, -39f, -8f, -54f, -7f, -23f)
    middleShape(0xFF9370B5) {
        moveTo(-63f, -28f)
        cubicTo(-64f, -43f, -21f, -45f, -15f, -22f)
        quadraticTo(-12f, 1f, -35f, 7f)
        quadraticTo(-58f, 1f, -63f, -28f)
    }
    middleGem(0xFFFFD769, -50f, -24f, 9f, 10f)
    middleGem(0xFFFFDF84, -25f, -26f, 8f, 10f)
    kantoPolygon(KANTO_INK, -54f, -7f, -39f, -1f, -23f, -10f, -36f, 3f)
    for (i in 0..3) kantoPolygon(0xFFFCF8ED, -51f + i * 7f, -6f,
        -48f + i * 7f, 0f, -45f + i * 7f, -6f)
    middleGem(0xFF5AC9DF, -43f, 15f, 6f, 8f)
    kantoLine(KANTO_INK, 13f, -55f, 9f, -76f, 21f + reach, -61f, 34f + reach)
    kantoLine(0xFF9370B5, 9f, -55f, 9f, -76f, 21f + reach, -61f, 34f + reach)
    kantoPolygon(0xFFBA234D, -5f, -51f, 42f, -57f, 72f, -24f, 64f, 37f, 23f, 62f, -25f, 36f, -32f, -17f)
    kantoPolygon(0xFFF64D6A, -5f, -51f, 42f, -57f, 49f, -19f, 1f, -13f)
    kantoPolygon(0xFFE73B65, 1f, -13f, 49f, -19f, 44f, 26f, 8f, 34f)
    kantoPolygon(0xFF8F214B, 72f, -24f, 64f, 37f, 23f, 62f, 44f, 26f, 49f, -19f)
    kantoPolygon(0xFFFF8091, -32f, -17f, -5f, -51f, 1f, -13f, -25f, 36f)
    kantoLine(0xFFFFCCD2, 3f, -1f, -44f, 35f, -49f)
    kantoLine(0xFFFFA5B7, 2f, 7f, -8f, 12f, 24f, 38f, 20f)
    kantoOval(0xFF9370B5, -39f, 18f + reach, 18f, 11f)
    kantoLine(KANTO_INK, 1.6f, -32f, 20f + reach, -30f, 26f + reach)
    kantoOval(0xFF9370B5, 60f, 13f - reach, 12f, 16f)
}

private fun DrawScope.middleMawile(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val lift = m * s * 5f
        middleShape(0xFF373541) {
            moveTo(s * 6f, -12f)
            cubicTo(s * 22f, -46f, s * 47f, -70f + lift, s * 79f, -64f + lift)
            quadraticTo(s * 98f, -58f + lift, s * 93f, -30f + lift)
            lineTo(s * 79f, -4f + lift)
            lineTo(s * 72f, -28f + lift)
            quadraticTo(s * 43f, -39f + lift, s * 18f, -6f)
        }
        middleShape(0xFFB46582) {
            moveTo(s * 31f, -25f)
            quadraticTo(s * 68f, -63f + lift, s * 88f, -46f + lift)
            lineTo(s * 81f, -19f + lift)
            quadraticTo(s * 64f, -40f + lift, s * 31f, -25f)
        }
        for (i in 0..3) {
            val x = 45f + i * 10f
            val y = -39f - i * 3f + lift
            kantoPolygon(0xFFFFF5DD, s * x, y, s * (x + 8f), y - 3f,
                s * (x + 4f), y + 11f)
        }
        kantoPolygon(0xFFF7F2DB, s * 73f, -27f + lift, s * 81f, -34f + lift, s * 84f, -16f + lift)
        kantoLine(0xFF625363, 3f, s * 22f, -24f, s * 47f, -50f + lift, s * 74f, -57f + lift)
    }
    kantoPolygon(0xFF5B4652, -19f, 29f, -28f, 57f + m * 3f, -6f, 60f,
        0f, 39f, 9f, 61f - m * 3f, 31f, 55f - m * 3f, 19f, 26f)
    middleShape(0xFFF1C978) {
        moveTo(-16f, -1f)
        quadraticTo(-26f, 19f, -28f, 35f)
        quadraticTo(0f, 45f, 27f, 33f)
        lineTo(18f, 2f)
    }
    kantoPolygon(0xFFE0AB69, -12f, 8f, -8f, 31f, 1f, 36f, 9f, 8f)
    kantoLine(KANTO_INK, 10f, -15f, 3f, -30f, 14f + m * 5f, -36f, 2f + m * 5f)
    kantoLine(0xFFFFDF8C, 7f, -15f, 3f, -30f, 14f + m * 5f, -36f, 2f + m * 5f)
    kantoLine(KANTO_INK, 10f, 14f, 3f, 31f, 13f - m * 5f, 38f, 2f - m * 5f)
    kantoLine(0xFFFFDF8C, 7f, 14f, 3f, 31f, 13f - m * 5f, 38f, 2f - m * 5f)
    kantoOval(0xFFFFDF8C, -21f, -31f, 42f, 39f)
    middleShape(0xFF39333D) {
        moveTo(-21f, -18f)
        quadraticTo(-24f, -38f, 1f, -39f)
        quadraticTo(26f, -38f, 22f, -11f)
        lineTo(12f, -16f)
        lineTo(9f, -28f)
        lineTo(-4f, -16f)
        lineTo(-9f, -28f)
    }
    kantoEyes(0f, -9f, 9f, 3.2f)
    kantoSmile(0f, -1f, 8f)
}

private fun DrawScope.middleAbsol(m: Float) {
    middleShape(0xFF303B56) {
        moveTo(-46f, 14f)
        cubicTo(-90f, 20f, -95f, -9f + m * 5f, -79f, -29f + m * 5f)
        quadraticTo(-86f, 2f, -52f, -2f)
        lineTo(-35f, 9f)
    }
    for (s in listOf(-1f, 1f)) {
        val x = s * 32f
        val step = m * s * 4f
        middleShape(0xFFC9D3E0) {
            moveTo(x - 10f, 18f)
            lineTo(x - 14f, 44f + step)
            lineTo(x - 23f, 55f + step)
            lineTo(x + 1f, 55f + step)
            lineTo(x + 9f, 25f)
        }
        middleClaws(x - 20f, 54f + step)
    }
    middleShape(0xFFF4F5F5) {
        moveTo(-53f, 7f)
        cubicTo(-58f, -24f, -15f, -29f, 23f, -14f)
        quadraticTo(54f, -4f, 43f, 23f)
        cubicTo(18f, 37f, -33f, 38f, -53f, 7f)
    }
    for (s in listOf(-1f, 1f)) {
        val f = m * s * 6f
        middleShape(0xFFF9FAFF) {
            moveTo(s * 3f, -6f)
            quadraticTo(s * 32f, -58f + f, s * 68f, -69f + f)
            lineTo(s * 61f, -48f + f)
            lineTo(s * 76f, -53f + f)
            lineTo(s * 61f, -28f + f)
            lineTo(s * 70f, -32f + f)
            quadraticTo(s * 41f, 4f, s * 3f, 4f)
        }
        middleCurve(0xFFB7C7DB, 2f) {
            moveTo(s * 11f, -5f)
            quadraticTo(s * 35f, -27f + f, s * 58f, -47f + f)
        }
    }
    middleShape(0xFFFFFFFF) {
        moveTo(11f, -19f)
        quadraticTo(24f, -49f, 46f, -34f)
        lineTo(59f, -5f)
        lineTo(47f, 0f)
        lineTo(50f, 17f)
        lineTo(30f, 10f)
        lineTo(20f, 20f)
        lineTo(19f, 3f)
        lineTo(6f, 9f)
    }
    kantoOval(0xFF35425E, 31f, -29f, 30f, 24f)
    middleShape(0xFF36415C) {
        moveTo(43f, -31f)
        cubicTo(29f, -42f, 39f, -69f, 67f, -70f)
        quadraticTo(50f, -55f, 59f, -39f)
        lineTo(56f, -27f)
    }
    kantoPolygon(0xFFFFFFFF, 24f, -29f, 43f, -41f, 46f, -21f, 35f, -18f)
    kantoLine(0xFFD95F77, 3f, 47f, -19f, 54f, -20f)
    kantoOval(KANTO_INK, 55f, -11f, 7f, 5f)
}

private fun DrawScope.middleAbsolZ(m: Float) {
    val sweep = m * 4f
    for (i in 0..3) {
        val rise = i * 15f
        middleShape(if (i % 2 == 0) 0xFFC82F62 else 0xFFE64A79) {
            moveTo(-17f, -4f)
            cubicTo(-32f, -23f - rise, -61f, -40f - rise * .65f + sweep,
                -87f + i * 7f, -29f - rise + sweep)
            quadraticTo(-58f, -34f - rise * .5f + sweep, -52f, -8f - rise * .35f)
            quadraticTo(-43f, 8f, -17f, -4f)
        }
        middleCurve(0xFF8E234C, 1.5f) {
            moveTo(-22f, -5f)
            quadraticTo(-39f, -29f - rise * .55f, -68f, -27f - rise * .65f + sweep)
        }
    }
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 3f
        kantoPolygon(0xFF092B3F, s * 24f, 12f, s * 41f, 18f, s * 53f, 44f + step,
            s * 70f, 51f + step, s * 44f, 54f + step, s * 29f, 30f)
        for (i in 0..2) kantoPolygon(0xFF78929D, s * (47f + i * 7f), 49f + step,
            s * (52f + i * 7f), 55f + step, s * (53f + i * 7f), 49f + step)
    }
    middleShape(0xFF062435) {
        moveTo(-43f, 3f)
        cubicTo(-41f, -27f, 7f, -26f, 37f, -10f)
        quadraticTo(54f, 7f, 24f, 27f)
        quadraticTo(-22f, 33f, -43f, 3f)
    }
    kantoPolygon(0xFF082335, -38f, 2f, -63f, 3f, -48f, -11f, -66f, -16f,
        -44f, -21f, -53f, -35f, -31f, -29f, -34f, -49f, -16f, -38f,
        -9f, -56f, 6f, -28f, 28f, -15f, 16f, 19f, -14f, 20f)
    kantoPolygon(0xFF164255, -33f, -9f, -49f, -13f, -29f, -17f, -37f, -31f,
        -17f, -22f, -18f, -37f, -3f, -25f, 7f, 1f)
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 4f
        kantoPolygon(0xFF0B3045, s * 15f, 15f, s * 28f, 18f, s * 25f, 45f + step,
            s * 39f, 59f + step, s * 12f, 61f + step, s * 5f, 45f + step)
        for (i in 0..2) kantoPolygon(0xFF7F9EAA, s * (14f + i * 7f), 57f + step,
            s * (18f + i * 7f), 63f + step, s * (20f + i * 7f), 57f + step)
    }
    middleShape(0xFF072737) {
        moveTo(5f, -31f)
        quadraticTo(28f, -56f, 53f, -33f)
        lineTo(66f, -8f)
        lineTo(51f, -5f)
        lineTo(51f, 18f)
        lineTo(40f, 8f)
        lineTo(35f, 30f)
        lineTo(25f, 16f)
        lineTo(15f, 31f)
        lineTo(14f, 9f)
        lineTo(2f, 14f)
    }
    kantoPolygon(0xFF0C2234, 40f, -34f, 59f, -53f, 57f, -27f, 47f, -13f)
    kantoPolygon(0xFF345269, 22f, -32f, 31f, -54f, 24f, -75f, 45f, -57f,
        38f, -43f, 49f, -22f, 35f, -27f, 31f, -15f)
    kantoPolygon(0xFF204459, 37f, -29f, 50f, -21f, 57f, -8f, 47f, -6f,
        35f, -14f, 29f, -26f)
    kantoPolygon(0xFFE05476, 40f, -19f, 47f, -16f, 43f, -12f)
    kantoPolygon(0xFFE05476, 33f, -32f, 37f, -27f, 35f, -23f, 31f, -28f)
    kantoOval(0xFF061B2A, 56f, -10f, 8f, 5f)
    kantoLine(0xFF42647A, 1.5f, 42f, -3f, 50f, 0f)
}

private fun DrawScope.middleLucario(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val whip = m * s * 5f
        middleShape(0xFF333440) {
            moveTo(s * 13f, -34f)
            quadraticTo(s * 39f, -19f, s * 63f, -27f + whip)
            lineTo(s * 53f, -10f + whip)
            lineTo(s * 69f, -6f + whip)
            quadraticTo(s * 38f, 3f, s * 15f, -23f)
        }
        kantoPolygon(0xFF343844, s * 8f, 19f, s * 25f, 17f, s * 36f, 50f + whip,
            s * 47f, 57f + whip, s * 17f, 59f + whip, s * 11f, 39f)
        kantoPolygon(0xFFBA4B56, s * 18f, 40f + whip, s * 32f, 41f + whip,
            s * 39f, 57f + whip, s * 17f, 58f + whip)
    }
    middleShape(0xFF477DAD) {
        moveTo(-18f, -13f)
        quadraticTo(0f, -24f, 18f, -13f)
        lineTo(24f, 19f)
        lineTo(11f, 32f)
        lineTo(-14f, 31f)
        lineTo(-24f, 17f)
    }
    kantoPolygon(0xFFF0DDB2, -19f, -14f, 0f, -22f, 20f, -14f, 14f, 4f,
        25f, 17f, 6f, 12f, 1f, 28f, -7f, 14f, -23f, 20f, -14f, 2f)
    for (s in listOf(-1f, 1f)) {
        val lift = m * s * 7f
        kantoLine(KANTO_INK, 15f, s * 19f, -6f, s * 36f, 5f + lift, s * 51f, -8f + lift)
        kantoLine(0xFF3E78AE, 11f, s * 19f, -6f, s * 36f, 5f + lift, s * 51f, -8f + lift)
        kantoPolygon(0xFFB74757, s * 44f, -15f + lift, s * 62f, -17f + lift,
            s * 67f, -1f + lift, s * 47f, 4f + lift)
        kantoPolygon(0xFFEBE7D7, s * 49f, -15f + lift, s * 55f, -28f + lift, s * 58f, -14f + lift)
    }
    kantoPolygon(0xFF3C7BAE, -21f, -39f, -23f, -71f, -9f, -53f,
        6f, -54f, 19f, -72f, 20f, -38f)
    kantoPolygon(0xFF252F42, -19f, -62f, -11f, -49f, -18f, -46f)
    kantoPolygon(0xFF252F42, 15f, -62f, 17f, -46f, 9f, -50f)
    kantoOval(0xFF4C86B6, -23f, -50f, 46f, 34f)
    kantoPolygon(0xFF303343, -24f, -40f, -7f, -39f, 0f, -31f, 9f, -40f,
        25f, -41f, 19f, -27f, 0f, -24f, -20f, -28f)
    kantoLine(0xFFE85961, 3f, -17f, -35f, -10f, -33f)
    kantoLine(0xFFE85961, 3f, 11f, -33f, 18f, -36f)
    kantoPolygon(0xFF374150, -8f, -26f, 0f, -30f, 10f, -25f, 0f, -18f)
    kantoPolygon(0xFFEFECDD, -5f, -5f, 1f, -17f, 6f, -4f)
}

private fun DrawScope.middleLucarioZ(m: Float) {
    val flow = m * 4f
    middleShape(0xFF313E48) {
        moveTo(-8f, 11f)
        quadraticTo(-30f, 28f, -71f, 22f + flow)
        lineTo(-52f, 36f + flow)
        lineTo(-47f, 54f + flow)
        quadraticTo(-32f, 32f, 2f, 28f)
    }
    middleShape(0xFF5494A0) {
        moveTo(2f, -42f)
        cubicTo(41f, -51f, 71f, -30f + flow, 84f, -4f + flow)
        quadraticTo(94f, 10f + flow, 78f, 10f + flow)
        quadraticTo(54f, -13f + flow, 21f, -23f)
    }
    middleCurve(0xFF234F5B, 3f) {
        moveTo(79f, 7f + flow)
        quadraticTo(78f, 19f + flow, 90f, 15f + flow)
        quadraticTo(84f, 24f + flow, 97f, 19f + flow)
    }
    for (i in 0..3) kantoLine(0xFF8DB7BC, 1.5f, 32f + i * 12f, -33f + i * 7f + flow,
        27f + i * 13f, -20f + i * 6f + flow)
    middleCurve(0xFF2E6470, 4f) {
        moveTo(-7f, -45f)
        cubicTo(27f, -66f, 54f, -49f + flow, 82f, -39f + flow)
        lineTo(79f, -32f + flow)
        lineTo(88f, -34f + flow)
        lineTo(86f, -28f + flow)
        lineTo(94f, -30f + flow)
    }
    kantoLine(KANTO_INK, 14f, 9f, 16f, 34f, 20f - flow, 24f, 37f - flow)
    kantoLine(0xFF344952, 10f, 9f, 16f, 34f, 20f - flow, 24f, 37f - flow)
    kantoPolygon(0xFF4B919F, 22f, 31f - flow, 36f, 34f - flow, 40f, 47f - flow,
        29f, 53f - flow, 21f, 45f - flow)
    kantoOval(0xFFBAC3C5, 28f, 34f - flow, 5f, 10f)
    kantoLine(KANTO_INK, 15f, -6f, 16f, -12f, 35f + flow, -6f, 53f + flow)
    kantoLine(0xFF3B5059, 11f, -6f, 16f, -12f, 35f + flow, -6f, 53f + flow)
    kantoOval(0xFFBAC3C5, -16f, 32f + flow, 7f, 11f)
    kantoOval(0xFFBAC3C5, -10f, 45f + flow, 7f, 10f)
    kantoOval(0xFF5398A4, -13f, 53f + flow, 26f, 9f)
    kantoLine(0xFF2B6673, 1.5f, -3f, 55f + flow, -3f, 60f + flow)
    kantoLine(0xFF2B6673, 1.5f, 4f, 55f + flow, 4f, 60f + flow)
    kantoPolygon(0xFF3E5962, -14f, -24f, 12f, -23f, 20f, 9f, 10f, 22f, -16f, 20f, -23f, 0f)
    middleShape(0xFFD8D99D) {
        moveTo(-17f, -22f)
        lineTo(8f, -21f)
        lineTo(3f, -10f)
        lineTo(15f, -10f)
        lineTo(5f, 1f)
        lineTo(12f, 6f)
        lineTo(-4f, 13f)
        lineTo(-14f, 8f)
        lineTo(-20f, 12f)
        lineTo(-17f, -2f)
        lineTo(-27f, -1f)
    }
    middleShape(0xFFCDCE8E) {
        moveTo(-12f, 6f)
        quadraticTo(6f, 0f, 13f, 15f)
        lineTo(3f, 15f)
        lineTo(9f, 23f)
        lineTo(-3f, 22f)
        quadraticTo(-2f, 36f, -21f, 37f)
        lineTo(-16f, 29f)
        lineTo(-31f, 32f)
        lineTo(-23f, 20f)
        lineTo(-31f, 23f)
        quadraticTo(-30f, 10f, -12f, 6f)
    }
    for (s in listOf(-1f, 1f)) {
        val strike = m * s * 5f
        val handY = if (s < 0f) 1f + strike else -30f + strike
        kantoLine(KANTO_INK, 11f, s * 14f, -17f, s * 33f, -10f + strike, s * 46f, handY)
        kantoLine(0xFF408392, 7f, s * 14f, -17f, s * 33f, -10f + strike, s * 46f, handY)
        middleShape(0xFF559EAB) {
            moveTo(s * 39f, handY - 6f)
            lineTo(s * 46f, handY - 13f)
            lineTo(s * 60f, handY - 11f)
            lineTo(s * 65f, handY + 2f)
            lineTo(s * 58f, handY + 13f)
            lineTo(s * 43f, handY + 10f)
        }
        kantoLine(0xFF326B78, 1.5f, s * 44f, handY - 4f, s * 54f, handY - 2f,
            s * 58f, handY + 9f)
        kantoLine(0xFF9BC6C8, 2f, s * 48f, handY - 9f, s * 58f, handY - 7f)
    }
    kantoPolygon(0xFF35434A, -27f, -42f, -34f, -73f, -17f, -57f,
        -12f, -76f, 1f, -52f, 7f, -39f)
    kantoPolygon(0xFF829298, -29f, -65f, -22f, -53f, -24f, -46f)
    kantoOval(0xFF647A83, -32f, -53f, 39f, 32f)
    kantoPolygon(0xFF34464F, -32f, -41f, -19f, -41f, -15f, -34f, -6f, -44f,
        6f, -45f, 2f, -31f, -13f, -25f, -30f, -29f)
    kantoPolygon(0xFFF2F1CF, -29f, -38f, -21f, -35f, -26f, -31f)
    kantoPolygon(0xFFF2F1CF, -10f, -36f, 0f, -41f, -2f, -34f)
    kantoLine(0xFF39414A, 2f, -25f, -36f, -25f, -33f)
    kantoLine(0xFF39414A, 2f, -5f, -38f, -5f, -34f)
    kantoPolygon(0xFF566F79, -24f, -29f, -11f, -33f, -5f, -25f, -20f, -22f)
    kantoOval(0xFF263A44, -25f, -29f, 7f, 5f)
    middleCurve(0xFF457F8D, 6f) {
        moveTo(-28f, -47f)
        cubicTo(-48f, -48f, -51f, -16f, -25f, -18f)
    }
    middleCurve(0xFFB9D5CF, 2f) {
        moveTo(-30f, -47f)
        cubicTo(-45f, -46f, -46f, -22f, -29f, -21f)
    }
    middleCurve(0xFFE3DEA0, 4f) {
        moveTo(-21f, -24f)
        cubicTo(-9f, -12f, 13f, -7f + flow, 35f, -10f + flow)
        lineTo(32f, -3f + flow)
        lineTo(42f, -6f + flow)
    }
}

private fun DrawScope.middleSlowbro(m: Float) {
    val wave = m * 5f
    middleShape(0xFFCF83A4) {
        moveTo(-8f, 40f)
        cubicTo(-40f, 34f, -52f, 51f + wave, -74f, 44f + wave)
        quadraticTo(-75f, 61f + wave * .3f, -50f, 59f)
        quadraticTo(-18f, 66f, 7f, 50f)
    }
    kantoPolygon(0xFFC5CEDA, -39f, 2f, -62f, -13f, -52f, 17f,
        -63f, 27f, -38f, 31f, -29f, 55f, -9f, 42f)
    kantoPolygon(0xFFC5CEDA, 39f, 1f, 60f, -12f, 53f, 15f,
        64f, 30f, 39f, 31f, 25f, 56f, 12f, 39f)
    middleShape(0xFFDCE0E6) {
        moveTo(-34f, -15f)
        quadraticTo(-58f, 7f, -34f, 38f)
        quadraticTo(0f, 65f, 38f, 34f)
        quadraticTo(56f, 10f, 34f, -16f)
        lineTo(19f, -43f)
        lineTo(-18f, -43f)
    }
    middleShape(0xFFADB9CC) {
        moveTo(31f, -16f)
        quadraticTo(51f, 10f, 35f, 34f)
        quadraticTo(17f, 54f, -8f, 47f)
        quadraticTo(32f, 21f, 19f, -23f)
    }
    middleCurve(0xFF7E8CA9, 3f) {
        moveTo(-39f, 10f)
        cubicTo(-24f, 42f, 37f, 31f, 30f, -1f)
        cubicTo(25f, -19f, -20f, -14f, -20f, 10f)
        cubicTo(-15f, 25f, 16f, 19f, 10f, 4f)
    }
    kantoOval(0xFF7B7894, -33f, -28f, 66f, 27f)
    for (s in listOf(-1f, 1f)) {
        val arm = m * s * 5f
        middleShape(0xFFEFA3B4) {
            moveTo(s * 14f, -20f)
            quadraticTo(s * 34f, -16f, s * 48f, -27f + arm)
            quadraticTo(s * 61f, -27f + arm, s * 57f, -15f + arm)
            quadraticTo(s * 37f, 1f, s * 20f, -3f)
        }
        middleClaws(s * 48f, -17f + arm, s)
    }
    kantoOval(0xFFF1ABB9, -24f, -42f, 48f, 40f)
    kantoOval(0xFFF6B4BE, -30f, -65f, 60f, 41f)
    kantoPolygon(0xFFF1A8BA, -25f, -52f, -31f, -72f, -9f, -62f)
    kantoPolygon(0xFFF1A8BA, 12f, -63f, 31f, -70f, 26f, -50f)
    kantoOval(0xFFF8D8B8, -33f, -43f, 65f, 23f)
    kantoEyes(0f, -50f, 15f, 3.8f)
    kantoSmile(0f, -30f, 37f)
    kantoLine(0xFFBD798D, 2f, -12f, -10f, 9f, -10f)
    kantoPolygon(0xFFF4F5F7, -31f, 5f, -17f, -1f, -10f, 14f)
    kantoPolygon(0xFFF4F5F7, 12f, 24f, 27f, 19f, 27f, 35f)
    kantoOval(0xFF191E35, -30f, 5f, 5f, 8f)
    kantoOval(0xFF191E35, 26f, 5f, 5f, 8f)
}

private fun DrawScope.middleSharpedo(m: Float) {
    val fin = m * 7f
    kantoPolygon(0xFF345688, -35f, -5f, -75f, -33f + fin, -63f, -6f + fin,
        -87f, 16f + fin, -50f, 14f, -28f, 17f)
    kantoPolygon(0xFF304D79, -13f, -21f, 7f, -62f, 28f, -25f)
    kantoPolygon(0xFF6387B4, -8f, -28f, 7f, -51f, 17f, -27f)
    middleShape(0xFF426C9B) {
        moveTo(-49f, -17f)
        cubicTo(-31f, -45f, 33f, -38f, 57f, -15f)
        lineTo(91f, -4f)
        quadraticTo(99f, 8f, 88f, 18f)
        lineTo(49f, 33f)
        cubicTo(5f, 55f, -49f, 30f, -49f, -17f)
    }
    middleShape(0xFFF1E7CE) {
        moveTo(-43f, 9f)
        quadraticTo(-8f, 26f, 22f, 8f)
        quadraticTo(66f, -1f, 91f, 3f)
        lineTo(88f, 18f)
        lineTo(47f, 32f)
        quadraticTo(-10f, 54f, -43f, 9f)
    }
    middleShape(0xFF262D48) {
        moveTo(21f, 13f)
        quadraticTo(58f, 12f, 88f, 5f)
        quadraticTo(62f, 36f, 28f, 27f)
    }
    for (i in 0..5) {
        val x = 29f + i * 9f
        val y = 14f - i * 1.1f
        kantoPolygon(0xFFFFF7E5, x, y, x + 7f, y - 1f, x + 5f, y + 9f)
    }
    for (i in 0..4) {
        val x = 44f + i * 10f
        val y = -19f + i * 3f
        kantoPolygon(0xFFFFE595, x, y, x + 3f, y - 8f, x + 8f, y + 3f)
    }
    kantoPolygon(0xFFF3D66F, -16f, -23f, -10f, -16f, 1f, -18f, -6f, -9f,
        -1f, 0f, -13f, -4f, -22f, 2f, -19f, -10f, -27f, -18f, -17f, -16f)
    kantoPolygon(0xFF314F7D, -7f, 20f, 12f, 23f, 27f, 53f - fin,
        -7f, 41f - fin, -23f, 26f)
    kantoLine(0xFF6C8DAC, 2f, -8f, 27f, 17f, 44f - fin)
    kantoPolygon(0xFFEEEEDE, 18f, -13f, 41f, -12f, 31f, -2f)
    kantoOval(0xFFD44F65, 28f, -12f, 6f, 8f)
    kantoLine(KANTO_INK, 3f, 17f, -14f, 42f, -14f)
    for (i in 0..2) kantoLine(0xFF253F67, 2f, -33f + i * 6f, 1f, -30f + i * 6f, 11f)
}

private fun DrawScope.middleEelektross(m: Float) {
    val coil = m * 4f
    middleShape(0xFFE6F4E9) {
        moveTo(-26f, -8f)
        quadraticTo(-53f, 4f, -72f, -10f + coil)
        quadraticTo(-91f, -28f + coil, -66f, -32f + coil)
        cubicTo(-55f, -52f + coil, -28f, -38f, -15f, -55f)
        quadraticTo(-8f, -65f, 1f, -58f)
        quadraticTo(-4f, -46f, -17f, -48f)
        quadraticTo(-29f, -35f, -38f, -24f)
        quadraticTo(-27f, -25f, -26f, -8f)
    }
    middleCurve(0xFFB5E5D9, 2f) {
        moveTo(-75f, -23f + coil)
        quadraticTo(-48f, -31f, -32f, -42f)
    }
    middleShape(0xFF263E4D) {
        moveTo(-58f, 14f)
        cubicTo(-48f, -12f, -18f, -30f, 8f, -21f)
        cubicTo(32f, -12f, 43f, -7f + coil, 69f, -21f + coil)
        lineTo(84f, -36f + coil)
        quadraticTo(80f, -16f + coil, 55f, 0f + coil)
        quadraticTo(28f, 12f + coil, 3f, -3f)
        quadraticTo(-24f, -10f, -41f, 24f)
    }
    middleShape(0xFFEBE4AB) {
        moveTo(-41f, -2f)
        quadraticTo(-24f, -35f, 20f, -50f + coil)
        quadraticTo(7f, -25f + coil, -10f, -27f)
        quadraticTo(-23f, -20f, -29f, -7f)
    }
    middleCurve(0xFF566673, 3f) {
        moveTo(-40f, -3f)
        quadraticTo(-19f, -34f, 20f, -50f + coil)
    }
    middleCurve(0xFFEBCE64, 4f) {
        moveTo(-47f, 13f)
        quadraticTo(-24f, -14f, -2f, -12f)
        quadraticTo(27f, 3f, 51f, -4f + coil)
    }
    for (i in 0..6) kantoOval(0xFFE8D471, -31f + i * 11f, -11f + i * 1.5f,
        4f, 6f)
    kantoOval(0xFF344B59, -65f, 6f, 29f, 25f)
    kantoOval(0xFFC34D4C, -65f, 11f, 23f, 22f)
    kantoOval(0xFF2D3441, -61f, 14f, 16f, 17f)
    kantoPolygon(0xFFFAF4DF, -59f, 14f, -54f, 15f, -54f, 20f)
    kantoPolygon(0xFFFAF4DF, -53f, 30f, -48f, 28f, -51f, 23f)
    kantoOval(0xFFF8E29B, -46f, 3f, 8f, 8f)
    kantoOval(0xFF202D39, -43f, 4f, 3f, 6f)
    middleShape(0xFFF0F7ED) {
        moveTo(6f, -5f)
        quadraticTo(35f, -2f, 37f, 20f + coil)
        quadraticTo(44f, 44f + coil, 64f, 26f + coil)
        quadraticTo(74f, 7f + coil, 65f, 2f + coil)
        cubicTo(83f, -9f, 87f, -26f, 94f, -30f)
        quadraticTo(103f, -51f, 95f, -54f)
        quadraticTo(94f, -31f, 80f, -35f)
        quadraticTo(76f, -16f, 63f, -18f)
        quadraticTo(63f, -34f, 68f, -40f)
        quadraticTo(57f, -37f, 55f, -23f)
        quadraticTo(32f, -20f, 28f, -13f)
    }
    middleCurve(0xFFAFDDD2, 3f) {
        moveTo(61f, 17f + coil)
        cubicTo(57f, -5f, 79f, -8f, 87f, -29f)
    }
    middleCurve(0xFFDBB860, 2f) {
        moveTo(35f, 2f)
        lineTo(44f, -17f)
        lineTo(42f, -5f)
        lineTo(52f, -18f)
    }
    for (s in listOf(-1f, 1f)) {
        val x = if (s < 0f) -73f else 46f
        val y = if (s < 0f) -15f + coil else 25f + coil
        kantoOval(0xFFED9485, x - 10f, y - 12f, 20f, 25f)
        kantoOval(0xFFF8FAE5, x - 7f, y - 9f, 14f, 19f)
        kantoLine(0xFFE3CF6B, 3f, x - 6f, y - 3f, x + 6f, y + 3f)
        kantoLine(0xFFE3CF6B, 3f, x - 2f, y - 9f, x + 1f, y + 9f)
    }
}

private fun DrawScope.middleDragonite(m: Float) {
    val wag = m * 3f
    middleShape(0xFFD9A553) {
        moveTo(21f, 27f)
        cubicTo(3f, 44f, -40f, 62f, -66f, 43f + wag)
        quadraticTo(-84f, 31f + wag, -81f, 49f + wag)
        cubicTo(-70f, 73f, -20f, 56f, 36f, 43f)
    }
    kantoOval(0xFF70AB73, -82f, 38f + wag, 22f, 23f)
    kantoOval(0xFFD8DEA0, -79f, 40f + wag, 9f, 11f)
    drawCircle(Color(0xFFF6F0C1), 3f, Offset(-74f, 43f + wag))
    middleShape(0xFF378F86) {
        moveTo(20f, -16f)
        quadraticTo(39f, -31f, 58f, -20f - wag)
        quadraticTo(44f, -20f - wag, 46f, -7f)
        quadraticTo(34f, -13f, 28f, 0f)
    }
    middleCurve(0xFFF0D69D, 3f) {
        moveTo(20f, -16f)
        quadraticTo(40f, -30f, 58f, -20f - wag)
    }
    middleShape(0xFFE6B465) {
        moveTo(-14f, -25f)
        quadraticTo(11f, -29f, 22f, -8f)
        cubicTo(33f, 3f, 58f, 16f, 49f, 40f)
        quadraticTo(39f, 58f, 8f, 51f)
        cubicTo(-22f, 48f, -30f, 11f, -14f, -25f)
    }
    middleShape(0xFFF4E5B6) {
        moveTo(-14f, -19f)
        quadraticTo(-22f, 7f, -6f, 33f)
        quadraticTo(6f, 51f, 26f, 49f)
        quadraticTo(30f, 36f, 12f, 17f)
        quadraticTo(-1f, -1f, -4f, -21f)
    }
    for (i in 0..3) {
        val y = 4f + i * 11f
        kantoLine(0xFFC7B987, 1.5f, -14f + i * 4f, y, 1f + i * 6f, y + 3f)
    }
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 3f
        val x = if (s < 0f) -8f else 37f
        kantoOval(0xFFDEAC60, x - 11f, 37f + step, 24f, 20f)
        kantoPolygon(0xFFE5B56A, x - 8f, 48f + step, x + 9f, 48f + step,
            x + 14f, 59f + step, x - 7f, 60f + step)
        middleClaws(x - 5f, 59f + step)
        val handX = if (s < 0f) -36f else 39f
        val handY = if (s < 0f) -1f + step else 0f + step
        kantoLine(KANTO_INK, 14f, s * 16f, -10f, handX, handY + 10f, handX + 2f, handY)
        kantoLine(0xFFEAB970, 10f, s * 16f, -10f, handX, handY + 10f, handX + 2f, handY)
        middleClaws(handX - 4f, handY)
    }
    kantoOval(0xFFE9BA70, -29f, -45f, 37f, 34f)
    kantoOval(0xFFF0C681, -37f, -28f, 34f, 18f)
    kantoOval(0xFF397768, -20f, -37f, 8f, 12f)
    kantoLine(0xFFFFF6D1, 2f, -18f, -35f, -18f, -30f)
    kantoLine(0xFF835F3B, 1.5f, -29f, -15f, -17f, -13f, -7f, -17f)
    for (s in listOf(-1f, 1f)) {
        val flap = m * s * 3f
        val root = -10f
        middleShape(0xFFF2E6C2) {
            moveTo(root + s * 5f, -37f)
            cubicTo(root + s * 23f, -46f, root + s * 9f, -65f + flap,
                root + s * 20f, -69f + flap)
            cubicTo(root + s * 42f, -75f + flap, root + s * 78f, -64f + flap,
                root + s * 82f, -47f + flap)
            quadraticTo(root + s * 92f, -29f + flap, root + s * 82f, -15f + flap)
            quadraticTo(root + s * 75f, -14f + flap, root + s * 73f, -36f + flap)
            quadraticTo(root + s * 76f, -11f + flap, root + s * 66f, -17f + flap)
            lineTo(root + s * 54f, -40f + flap)
            quadraticTo(root + s * 60f, -16f + flap, root + s * 49f, -23f + flap)
            lineTo(root + s * 38f, -41f + flap)
            quadraticTo(root + s * 38f, -26f + flap, root + s * 26f, -33f + flap)
            quadraticTo(root + s * 5f, -24f, root + s * 5f, -37f)
        }
        middleCurve(0xFFBCAF8C, 1.7f) {
            moveTo(root + s * 8f, -38f)
            cubicTo(root + s * 34f, -42f, root + s * 12f, -68f + flap,
                root + s * 30f, -65f + flap)
            quadraticTo(root + s * 58f, -60f + flap, root + s * 74f, -43f + flap)
        }
    }
}

private fun DrawScope.middleVenusaur(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val sway = m * s * 5f
        middleCurve(KANTO_INK, 9f) {
            moveTo(s * 27f, -25f)
            cubicTo(s * 58f, -44f, s * 95f, -15f + sway, s * 86f, 21f + sway)
        }
        middleCurve(0xFF547D43, 6f) {
            moveTo(s * 27f, -25f)
            cubicTo(s * 58f, -44f, s * 95f, -15f + sway, s * 86f, 21f + sway)
        }
        kantoPolygon(0xFF6CA455, s * 85f, 6f + sway, s * 71f, -1f + sway,
            s * 65f, 14f + sway, s * 84f, 18f + sway)
        kantoOval(0xFF479780, s * 42f - 17f, 26f + sway * .5f, 34f, 33f)
        middleClaws(s * 42f - 8f, 57f + sway * .5f)
    }
    middleShape(0xFF68AF96) {
        moveTo(-65f, 5f)
        cubicTo(-68f, -32f, 64f, -35f, 66f, 7f)
        quadraticTo(67f, 42f, 4f, 44f)
        quadraticTo(-61f, 46f, -65f, 5f)
    }
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF497C43, 0f, -17f, s * 26f, -44f, s * 38f, -29f,
            s * 67f, -34f, s * 61f, -16f, s * 82f, -8f, s * 51f, 0f, s * 35f, -3f)
        kantoLine(0xFF8FBB63, 2f, s * 11f, -18f, s * 42f, -20f, s * 65f, -14f)
    }
    kantoPolygon(0xFF9B7350, -15f, -21f, -11f, -57f, 9f, -58f, 16f, -19f)
    kantoLine(0xFFD2AA71, 3f, -3f, -48f, 0f, -22f)
    for (s in listOf(-1f, 1f)) {
        middleShape(0xFFD57587) {
            moveTo(0f, -54f)
            cubicTo(s * 25f, -78f, s * 70f, -72f, s * 75f, -51f)
            quadraticTo(s * 65f, -30f, s * 24f, -42f)
        }
        middleShape(0xFFEB8C9D) {
            moveTo(0f, -53f)
            quadraticTo(s * 28f, -60f, s * 48f, -42f)
            quadraticTo(s * 53f, -22f, s * 20f, -27f)
            quadraticTo(s * 4f, -32f, 0f, -53f)
        }
        kantoOval(0xFFFFDBB0, s * 45f - 5f, -59f, 10f, 6f)
        kantoOval(0xFFFFDBB0, s * 29f - 4f, -41f, 8f, 6f)
    }
    kantoOval(0xFFF0CD72, -18f, -64f, 36f, 23f)
    for (i in 0..4) drawCircle(Color(0xFFB89445), 2f, Offset(-11f + i * 5f, -53f - (i % 2) * 5f))
    middleShape(0xFF72B9A3) {
        moveTo(-38f, 1f)
        quadraticTo(-39f, -10f, -26f, -10f)
        lineTo(-17f, -1f)
        lineTo(18f, -1f)
        lineTo(29f, -12f)
        quadraticTo(43f, -7f, 40f, 9f)
        quadraticTo(44f, 42f, 0f, 42f)
        quadraticTo(-43f, 40f, -38f, 1f)
    }
    kantoPolygon(0xFFF3F3D7, -29f, 8f, -9f, 11f, -17f, 21f, -28f, 18f)
    kantoPolygon(0xFFF3F3D7, 10f, 11f, 30f, 8f, 28f, 18f, 18f, 21f)
    kantoEyes(0f, 15f, 21f, 4f)
    kantoSmile(0f, 29f, 46f)
    kantoPolygon(0xFFFFF4DA, -23f, 28f, -19f, 35f, -16f, 29f)
    kantoPolygon(0xFFFFF4DA, 15f, 29f, 20f, 35f, 24f, 28f)
    for (s in listOf(-1f, 1f)) kantoOval(0xFFF7A3B3, s * 6f - 9f, -10f, 18f, 13f)
    kantoOval(0xFFFFD67D, -6f, -7f, 12f, 10f)
    kantoOval(0xFF42937D, -54f, 11f, 10f, 7f)
    kantoOval(0xFF42937D, 47f, 10f, 12f, 9f)
}

private fun DrawScope.middleCharizardX(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val flap = m * s * 6f
        middleShape(0xFF263546) {
            moveTo(s * 17f, -11f)
            lineTo(s * 53f, -60f + flap)
            lineTo(s * 92f, -66f + flap)
            lineTo(s * 77f, -42f + flap)
            lineTo(s * 100f, -6f + flap)
            quadraticTo(s * 77f, -15f + flap, s * 68f, 8f + flap)
            quadraticTo(s * 43f, -3f, s * 27f, 21f)
        }
        middleShape(0xFF4FC9DE) {
            moveTo(s * 25f, -10f)
            lineTo(s * 55f, -51f + flap)
            lineTo(s * 78f, -57f + flap)
            lineTo(s * 68f, -39f + flap)
            lineTo(s * 88f, -17f + flap)
            quadraticTo(s * 74f, -16f + flap, s * 66f, -2f + flap)
            quadraticTo(s * 43f, -11f, s * 25f, 10f)
        }
        kantoLine(0xFF27566E, 3f, s * 54f, -50f + flap, s * 65f, -4f + flap)
    }
    middleShape(0xFF34465B) {
        moveTo(13f, 35f)
        cubicTo(42f, 62f, 67f, 51f, 79f, 23f + m * 5f)
        lineTo(84f, 30f + m * 5f)
        quadraticTo(77f, 65f, 34f, 61f)
        lineTo(8f, 46f)
    }
    middleFlame(81f, 30f + m * 5f, 12f, 34f, m, 0xFF329DF4)
    middleShape(0xFF314358) {
        moveTo(-18f, -23f)
        quadraticTo(-33f, -5f, -28f, 26f)
        quadraticTo(-27f, 49f, 1f, 51f)
        quadraticTo(32f, 47f, 28f, 17f)
        lineTo(17f, -27f)
    }
    middleShape(0xFF63C8D8) {
        moveTo(-8f, -13f)
        quadraticTo(-21f, 12f, -14f, 37f)
        quadraticTo(1f, 48f, 18f, 34f)
        quadraticTo(17f, 7f, 7f, -18f)
    }
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 4f
        kantoOval(0xFF34445A, s * 23f - 15f, 39f + step, 30f, 19f)
        middleClaws(s * 23f - 7f, 57f + step)
        kantoLine(KANTO_INK, 13f, s * 19f, -5f, s * 34f, 8f + step, s * 43f, 0f + step)
        kantoLine(0xFF34465B, 9f, s * 19f, -5f, s * 34f, 8f + step, s * 43f, 0f + step)
        middleClaws(s * 44f - 6f, 3f + step)
        kantoPolygon(0xFF8EE5E5, s * 21f, -11f, s * 30f, -16f, s * 27f, -4f)
    }
    kantoPolygon(0xFF35465A, -20f, -37f, -23f, -67f, -8f, -53f,
        8f, -53f, 23f, -67f, 20f, -37f)
    middleShape(0xFF3D5367) {
        moveTo(-20f, -48f)
        quadraticTo(0f, -57f, 21f, -45f)
        lineTo(16f, -31f)
        lineTo(21f, -23f)
        quadraticTo(1f, -11f, -22f, -23f)
        lineTo(-17f, -33f)
    }
    kantoPolygon(0xFFF8F7E6, -17f, -39f, -4f, -36f, -9f, -31f)
    kantoPolygon(0xFFF8F7E6, 4f, -36f, 18f, -39f, 11f, -31f)
    kantoEyes(0f, -35f, 11f, 2.7f)
    kantoSmile(0f, -24f, 24f)
    middleFlame(-25f, -21f, 11f, 22f, -m, 0xFF36B5F3)
    middleFlame(28f, -20f, 11f, 22f, m, 0xFF36B5F3)
}

private fun DrawScope.middleCharizardY(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val flap = m * s * 6f
        middleShape(0xFFE89956) {
            moveTo(s * 17f, -8f)
            lineTo(s * 43f, -50f + flap)
            lineTo(s * 74f, -70f + flap)
            lineTo(s * 80f, -44f + flap)
            lineTo(s * 101f, 9f + flap)
            quadraticTo(s * 77f, -1f + flap, s * 73f, 24f + flap)
            quadraticTo(s * 48f, 10f, s * 27f, 27f)
        }
        middleShape(0xFF427E89) {
            moveTo(s * 27f, -6f)
            lineTo(s * 47f, -45f + flap)
            lineTo(s * 72f, -60f + flap)
            lineTo(s * 73f, -39f + flap)
            lineTo(s * 90f, -2f + flap)
            quadraticTo(s * 75f, -3f + flap, s * 68f, 13f + flap)
            quadraticTo(s * 45f, 4f, s * 27f, 16f)
        }
        kantoLine(0xFFE6A16B, 3f, s * 47f, -45f + flap, s * 67f, 12f + flap)
        kantoPolygon(0xFFF3C99B, s * 70f, -64f + flap, s * 74f, -73f + flap * .3f,
            s * 78f, -59f + flap)
    }
    middleShape(0xFFD58845) {
        moveTo(9f, 34f)
        quadraticTo(53f, 58f, 77f, 34f + m * 5f)
        lineTo(82f, 39f + m * 5f)
        quadraticTo(61f, 67f, 19f, 56f)
        lineTo(-3f, 44f)
    }
    middleFlame(81f, 39f + m * 5f, 13f, 35f, m, 0xFFF4894D)
    middleShape(0xFFECA269) {
        moveTo(-12f, -30f)
        lineTo(12f, -30f)
        quadraticTo(16f, -4f, 28f, 14f)
        quadraticTo(34f, 43f, 6f, 51f)
        quadraticTo(-24f, 51f, -28f, 29f)
        quadraticTo(-29f, 4f, -12f, -30f)
    }
    middleShape(0xFFFFE4AE) {
        moveTo(-7f, -17f)
        quadraticTo(-12f, 1f, -18f, 22f)
        quadraticTo(-20f, 44f, 4f, 44f)
        quadraticTo(25f, 38f, 15f, 15f)
        lineTo(7f, -18f)
    }
    for (s in listOf(-1f, 1f)) {
        val arm = m * s * 5f
        kantoOval(0xFFE09A59, s * 24f - 12f, 42f + arm * .6f, 26f, 16f)
        middleClaws(s * 24f - 6f, 56f + arm * .6f)
        kantoLine(KANTO_INK, 11f, s * 17f, -7f, s * 32f, 7f + arm, s * 43f, -1f + arm)
        kantoLine(0xFFEAA267, 7f, s * 17f, -7f, s * 32f, 7f + arm, s * 43f, -1f + arm)
        kantoPolygon(0xFFEBA369, s * 30f, 0f + arm, s * 40f, -15f + arm,
            s * 45f, 3f + arm, s * 33f, 11f + arm)
        middleClaws(s * 43f - 4f, 2f + arm)
    }
    kantoPolygon(0xFFE59A60, -18f, -36f, -25f, -63f, -9f, -51f,
        -1f, -74f, 8f, -51f, 25f, -62f, 17f, -34f)
    middleShape(0xFFECB078) {
        moveTo(-17f, -48f)
        quadraticTo(1f, -55f, 17f, -46f)
        lineTo(15f, -32f)
        lineTo(23f, -24f)
        quadraticTo(3f, -14f, -20f, -24f)
        lineTo(-13f, -34f)
    }
    kantoLine(0xFFFFF0C8, 3f, -16f, -40f, -6f, -37f)
    kantoLine(0xFFFFF0C8, 3f, 6f, -37f, 16f, -40f)
    kantoEyes(0f, -37f, 10f, 2.6f)
    kantoSmile(0f, -25f, 23f)
    kantoPolygon(0xFFFFF5D6, -9f, -24f, -6f, -19f, -3f, -24f)
}

private fun DrawScope.middleBlastoise(m: Float) {
    kantoOval(0xFF79614E, -47f, -29f, 94f, 77f)
    kantoOval(0xFFF1DEBD, -43f, -25f, 86f, 70f)
    kantoOval(0xFF7D6654, -37f, -23f, 74f, 62f)
    kantoPolygon(0xFF9A8470, -32f, -19f, -11f, -31f, 13f, -31f, 34f, -16f,
        34f, 21f, 12f, 36f, -15f, 33f, -34f, 14f)
    kantoPolygon(0xFF566779, -18f, -29f, -18f, -67f, 19f, -67f, 20f, -22f)
    kantoPolygon(0xFFB2C5CE, -18f, -65f, -9f, -72f, 24f, -72f, 19f, -65f)
    kantoPolygon(0xFF7D98A9, 19f, -65f, 24f, -72f, 24f, -26f, 19f, -20f)
    kantoOval(0xFFCCD9DB, -20f, -73f, 40f, 13f)
    kantoOval(0xFF283A4F, -13f, -70f, 26f, 7f)
    kantoLine(0xFFBDD7DF, 3f, -12f, -57f, -12f, -35f)
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 4f
        kantoOval(0xFF4D8BAF, s * 27f - 18f, 36f + step, 36f, 24f)
        middleClaws(s * 27f - 7f, 58f + step)
    }
    middleShape(0xFF5D98B9) {
        moveTo(-26f, -19f)
        quadraticTo(-37f, 1f, -29f, 33f)
        quadraticTo(0f, 54f, 29f, 32f)
        quadraticTo(38f, -1f, 26f, -19f)
    }
    kantoOval(0xFFEBD1A0, -27f, -5f, 54f, 48f)
    for (i in 0..2) {
        val y = 6f + i * 11f
        middleCurve(0xFFBA986D, 2f) {
            moveTo(-24f, y)
            quadraticTo(0f, y + 7f, 24f, y)
        }
    }
    for (s in listOf(-1f, 1f)) {
        val aim = m * s * 6f
        kantoLine(KANTO_INK, 22f, s * 26f, -8f, s * 45f, 7f + aim, s * 62f, 0f + aim)
        kantoLine(0xFF5593B4, 18f, s * 26f, -8f, s * 45f, 7f + aim, s * 62f, 0f + aim)
        kantoPolygon(0xFF816955, s * 43f, -11f + aim, s * 66f, -10f + aim,
            s * 74f, 13f + aim, s * 49f, 20f + aim, s * 38f, 5f + aim)
        kantoPolygon(0xFFADBFCA, s * 58f, -16f + aim, s * 77f, -21f + aim,
            s * 86f, 3f + aim, s * 68f, 11f + aim)
        kantoOval(0xFFD5E3E5, s * 78f - 10f, -24f + aim, 20f, 19f)
        kantoOval(0xFF2F4054, s * 78f - 6f, -20f + aim, 12f, 11f)
        kantoLine(0xFFEDDEBF, 2f, s * 45f, -7f + aim, s * 40f, 5f + aim, s * 50f, 16f + aim)
    }
    kantoOval(0xFF6CA8C5, -25f, -46f, 50f, 39f)
    kantoPolygon(0xFF62A0BD, -23f, -33f, -26f, -48f, -11f, -42f)
    kantoPolygon(0xFF62A0BD, 11f, -42f, 26f, -48f, 23f, -33f)
    kantoLine(0xFFF4F4DB, 5f, -18f, -33f, -7f, -30f)
    kantoLine(0xFFF4F4DB, 5f, 7f, -30f, 18f, -33f)
    kantoEyes(0f, -31f, 12f, 3f)
    kantoSmile(0f, -18f, 30f)
    kantoPolygon(0xFFFFF5DA, -13f, -19f, -9f, -13f, -6f, -18f)
}

private fun DrawScope.middleMalamar(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val curl = m * s * 5f
        middleShape(0xFFB164B1) {
            moveTo(s * 9f, -35f)
            cubicTo(s * 18f, -68f, s * 50f, -74f + curl, s * 70f, -58f + curl)
            cubicTo(s * 93f, -39f + curl, s * 71f, -20f, s * 56f, -28f)
            quadraticTo(s * 78f, -36f, s * 61f, -47f + curl)
            quadraticTo(s * 39f, -61f + curl, s * 28f, -29f)
        }
        middleShape(0xFFDE85C2) {
            moveTo(s * 2f, -37f)
            cubicTo(s * 3f, -72f, s * 28f, -79f, s * 44f, -65f + curl)
            quadraticTo(s * 24f, -68f + curl, s * 22f, -35f)
        }
        middleCurve(0xFFEDBACA, 2f) {
            moveTo(s * 27f, -41f)
            quadraticTo(s * 44f, -65f + curl, s * 63f, -51f + curl)
        }
    }
    middleShape(0xFF493D6C) {
        moveTo(-27f, -19f)
        cubicTo(-45f, 5f, -31f, 33f, -11f, 46f)
        lineTo(-24f, 61f)
        lineTo(-3f, 54f)
        lineTo(11f, 63f)
        lineTo(13f, 44f)
        quadraticTo(45f, 16f, 26f, -19f)
    }
    middleShape(0xFF9D7CB5) {
        moveTo(-18f, -8f)
        quadraticTo(-28f, 19f, 0f, 42f)
        quadraticTo(27f, 18f, 17f, -9f)
    }
    for (i in 0..4) {
        val y = -4f + i * 9f
        val w = 17f - i * 1.8f
        kantoOval(0xFFFFE587, -w, y, w * 2f, 5f)
    }
    for (s in listOf(-1f, 1f)) {
        val wave = m * s * 7f
        middleShape(0xFF715391) {
            moveTo(s * 23f, -6f)
            cubicTo(s * 42f, -5f, s * 46f, 29f + wave, s * 75f, 27f + wave)
            quadraticTo(s * 95f, 22f + wave, s * 98f, 7f + wave)
            quadraticTo(s * 101f, 50f + wave, s * 69f, 49f + wave)
            quadraticTo(s * 42f, 49f + wave, s * 21f, 14f)
        }
        middleCurve(0xFFEDADCF, 4f) {
            moveTo(s * 30f, 4f)
            cubicTo(s * 52f, 16f + wave, s * 53f, 49f + wave, s * 86f, 37f + wave)
        }
        for (i in 0..3) drawCircle(Color(0xFFFFD991), 2.5f,
            Offset(s * (43f + i * 11f), 22f + i * 5f + wave))
    }
    kantoOval(0xFF806A9C, -30f, -43f, 60f, 38f)
    kantoPolygon(0xFF312F4F, -29f, -32f, -4f, -30f, 0f, -19f,
        7f, -31f, 29f, -34f, 24f, -16f, 0f, -10f, -24f, -17f)
    kantoOval(0xFFFFECA0, -23f, -28f, 15f, 11f)
    kantoOval(0xFFFFECA0, 8f, -29f, 15f, 11f)
    kantoEyes(0f, -23f, 15f, 2.5f)
    kantoPolygon(0xFFD99F76, -5f, -15f, 0f, -20f, 7f, -15f, 1f, -8f)
}

private fun DrawScope.middleDragalge(m: Float) {
    val sway = m * 6f
    middleShape(0xFF866471) {
        moveTo(-14f, -38f)
        cubicTo(17f, -31f, -11f, 0f, 14f, 16f)
        cubicTo(49f, 33f, 28f, 64f, -3f, 57f)
        quadraticTo(-31f, 50f, -13f, 30f)
        quadraticTo(-22f, 46f, -1f, 46f)
        quadraticTo(18f, 42f, -1f, 26f)
        cubicTo(-36f, 4f, -18f, -14f, -31f, -32f)
    }
    middleCurve(0xFFD5B9B9, 6f) {
        moveTo(-12f, -25f)
        cubicTo(1f, 0f, -16f, 1f, 11f, 24f)
        quadraticTo(34f, 51f, 0f, 52f)
    }
    for (i in 0..4) kantoLine(0xFF766572, 2f, -14f + i * 2f, -12f + i * 8f,
        -1f + i * 3f, -17f + i * 8f)
    for (s in listOf(-1f, 1f)) {
        val wave = sway * s
        middleShape(0xFF685969) {
            moveTo(s * 9f, -12f)
            quadraticTo(s * 38f, -36f, s * 66f, -22f + wave)
            lineTo(s * 87f, -37f + wave)
            lineTo(s * 78f, -14f + wave)
            lineTo(s * 95f, -1f + wave)
            lineTo(s * 70f, 2f + wave)
            lineTo(s * 81f, 24f + wave)
            lineTo(s * 56f, 9f + wave)
            lineTo(s * 48f, 30f + wave)
            quadraticTo(s * 35f, 10f, s * 12f, 4f)
        }
        kantoPolygon(0xFFAA838F, s * 21f, -8f, s * 52f, -17f + wave,
            s * 71f, -6f + wave, s * 51f, 3f + wave)
        middleCurve(0xFFB7A58C, 2f) {
            moveTo(s * 16f, -5f)
            quadraticTo(s * 47f, -11f + wave, s * 80f, -19f + wave)
        }
        kantoPolygon(0xFFAF646D, s * 17f, -39f, s * 31f, -62f + wave,
            s * 56f, -69f + wave, s * 45f, -53f + wave, s * 62f, -48f + wave,
            s * 40f, -41f + wave, s * 32f, -24f)
        kantoLine(0xFFD99789, 2f, s * 23f, -39f, s * 39f, -53f + wave)
    }
    middleShape(0xFF937481) {
        moveTo(-28f, -45f)
        quadraticTo(-2f, -59f, 17f, -41f)
        lineTo(25f, -27f)
        lineTo(49f, -23f)
        lineTo(51f, -13f)
        lineTo(26f, -10f)
        lineTo(9f, -22f)
        quadraticTo(-23f, -13f, -28f, -45f)
    }
    kantoPolygon(0xFF728969, -20f, -42f, -24f, -60f, -40f, -74f,
        -17f, -66f, -6f, -52f, 8f, -71f, 17f, -63f, 8f, -44f)
    kantoPolygon(0xFFBE7C80, -17f, -38f, 0f, -48f, 15f, -38f, 6f, -27f, -10f, -28f)
    kantoLine(0xFFFFCE88, 4f, -7f, -37f, 6f, -37f)
    kantoOval(KANTO_INK, -1f, -39f, 4f, 7f)
    kantoLine(KANTO_INK, 2f, 31f, -18f, 45f, -18f)
    kantoPolygon(0xFF667965, 13f, 39f, 35f, 34f + sway, 54f, 45f + sway,
        33f, 46f + sway, 39f, 61f + sway * .3f, 16f, 52f)
}

private fun DrawScope.middleGlalie(m: Float) {
    val gape = m * 4f
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF2D354D, s * 22f, -37f, s * 43f, -70f, s * 49f, -42f,
            s * 62f, -19f, s * 41f, -18f)
        kantoPolygon(0xFF728DAD, s * 35f, -40f, s * 42f, -58f, s * 45f, -30f)
        kantoPolygon(0xFFA2D8E9, s * 37f, 4f, s * 69f, 11f + s * gape,
            s * 59f, 22f + s * gape, s * 70f, 33f + s * gape, s * 44f, 32f)
    }
    middleShape(0xFFD6F3F6) {
        moveTo(-48f, -28f)
        quadraticTo(-30f, -58f, 1f, -53f)
        quadraticTo(40f, -57f, 51f, -26f)
        lineTo(53f, 9f)
        lineTo(38f, 29f)
        lineTo(-37f, 29f)
        lineTo(-54f, 6f)
    }
    kantoPolygon(0xFF9FCEDD, -47f, -30f, -30f, -45f, -14f, -25f, -28f, -3f, -52f, 4f)
    kantoPolygon(0xFFB5E0E9, 17f, -28f, 31f, -47f, 48f, -31f, 52f, 1f, 30f, -3f)
    kantoPolygon(0xFFF4FFFF, -13f, -47f, 13f, -47f, 21f, -29f, 0f, -18f, -23f, -29f)
    middleShape(0xFF242E47) {
        moveTo(-39f, -1f)
        quadraticTo(0f, -13f, 40f, -1f)
        lineTo(47f, 23f + gape)
        quadraticTo(32f, 61f + gape, 0f, 59f + gape)
        quadraticTo(-36f, 60f + gape, -45f, 23f + gape)
    }
    middleShape(0xFF943F64) {
        moveTo(-30f, 18f)
        quadraticTo(0f, 0f, 32f, 17f)
        quadraticTo(30f, 47f + gape, 0f, 49f + gape)
        quadraticTo(-29f, 49f + gape, -30f, 18f)
    }
    middleShape(0xFFD486A1) {
        moveTo(-22f, 39f + gape)
        quadraticTo(-5f, 28f + gape, 4f, 40f + gape)
        quadraticTo(20f, 33f + gape, 27f, 39f + gape)
        quadraticTo(3f, 58f + gape, -22f, 39f + gape)
    }
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFFF7FEFF, s * 10f, -1f, s * 31f, -1f, s * 24f, 25f)
        kantoPolygon(0xFFEBFBFD, s * 12f, 52f + gape, s * 30f, 43f + gape, s * 25f, 26f + gape)
        kantoPolygon(0xFF29354C, s * 6f, -22f, s * 38f, -27f,
            s * 32f, -10f, s * 12f, -10f)
        kantoLine(0xFF73C2F3, 5f, s * 13f, -18f, s * 28f, -20f)
        kantoOval(KANTO_INK, s * 22f - 2f, -23f, 4f, 9f)
    }
    middleCurve(0xFFDCF7F9, 6f) {
        moveTo(-42f, 25f + gape)
        quadraticTo(-31f, 62f + gape * .2f, 0f, 58f + gape)
        quadraticTo(31f, 62f + gape * .2f, 43f, 25f + gape)
    }
    kantoPolygon(0xFFBFE7F0, -7f, 8f, 0f, -2f, 9f, 8f, 1f, 15f)
}

private fun DrawScope.middleFroslass(m: Float) {
    val ribbon = m * 6f
    middleShape(0xFFC25172) {
        moveTo(5f, 7f)
        cubicTo(42f, -1f, 42f, 49f + ribbon, 74f, 40f + ribbon)
        lineTo(67f, 58f + ribbon * .5f)
        quadraticTo(29f, 57f, 14f, 19f)
    }
    middleShape(0xFFE9F6FA) {
        moveTo(-12f, -23f)
        lineTo(14f, -23f)
        quadraticTo(17f, 3f, 21f, 24f)
        lineTo(37f, 59f)
        lineTo(17f, 54f)
        lineTo(5f, 64f)
        lineTo(-7f, 54f)
        lineTo(-28f, 61f)
        quadraticTo(-18f, 16f, -12f, -23f)
    }
    kantoPolygon(0xFFB1DDEA, -4f, 19f, 7f, 15f, 19f, 49f, 6f, 60f, -7f, 49f)
    for (s in listOf(-1f, 1f)) {
        val fan = m * s * 7f
        middleShape(0xFFF6FCFE) {
            moveTo(s * 9f, -19f)
            quadraticTo(s * 28f, -15f, s * 60f, -32f + fan)
            lineTo(s * 82f, -29f + fan)
            quadraticTo(s * 72f, 0f + fan, s * 88f, 29f + fan)
            lineTo(s * 73f, 27f + fan)
            lineTo(s * 70f, 48f + fan)
            lineTo(s * 55f, 34f + fan)
            lineTo(s * 42f, 41f + fan)
            quadraticTo(s * 40f, 10f + fan, s * 18f, -2f)
        }
        middleShape(0xFF8094B9) {
            moveTo(s * 60f, -16f + fan)
            lineTo(s * 72f, -17f + fan)
            quadraticTo(s * 61f, 9f + fan, s * 70f, 31f + fan)
            lineTo(s * 54f, 21f + fan)
            quadraticTo(s * 55f, 2f + fan, s * 60f, -16f + fan)
        }
        kantoPolygon(0xFFBBE3EE, s * 38f, 0f + fan, s * 48f, 4f + fan,
            s * 51f, 20f + fan, s * 43f, 14f + fan)
        kantoLine(0xFFC1DFE9, 2f, s * 23f, -9f, s * 53f, -19f + fan)
    }
    kantoPolygon(0xFFD15A7A, -17f, 4f, 17f, 4f, 19f, 18f, -18f, 17f)
    kantoPolygon(0xFFE68498, 1f, 9f, 24f, -2f, 28f, 20f, 3f, 14f,
        -25f, 21f, -23f, -2f)
    kantoOval(0xFFA84467, -5f, 6f, 11f, 12f)
    middleShape(0xFFF5FBFD) {
        moveTo(-22f, -48f)
        quadraticTo(-23f, -68f, 0f, -65f)
        quadraticTo(24f, -67f, 23f, -45f)
        lineTo(29f, -22f)
        lineTo(15f, -25f)
        lineTo(0f, -14f)
        lineTo(-15f, -25f)
        lineTo(-30f, -23f)
    }
    kantoPolygon(0xFF94C6E1, -20f, -52f, -35f, -73f, -11f, -66f, -8f, -53f)
    kantoPolygon(0xFF94C6E1, 9f, -54f, 13f, -67f, 36f, -73f, 22f, -51f)
    kantoPolygon(0xFF515B82, -17f, -47f, -6f, -40f, -7f, -27f, -20f, -36f)
    kantoPolygon(0xFF515B82, 6f, -40f, 18f, -47f, 20f, -36f, 7f, -27f)
    kantoLine(0xFFF4CC72, 3f, -16f, -38f, -10f, -36f)
    kantoLine(0xFFF4CC72, 3f, 10f, -36f, 16f, -38f)
    middleGem(0xFFAFDEEE, 0f, -51f, 6f, 9f)
    kantoSmile(0f, -25f, 8f)
}

private fun DrawScope.middleAbomasnow(m: Float) {
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF9CDCE8, s * 21f, -13f, s * 42f, -59f, s * 55f, -51f,
            s * 61f, -17f, s * 45f, 0f)
        kantoPolygon(0xFFDDF7F7, s * 42f, -59f, s * 46f, -21f, s * 55f, -51f)
        kantoPolygon(0xFF9DD5E3, s * 41f, -5f, s * 73f, -38f, s * 79f, -22f,
            s * 68f, 6f, s * 46f, 12f)
        val step = m * s * 3f
        kantoPolygon(0xFF42696A, s * 9f, 32f, s * 39f, 28f, s * 50f, 56f + step,
            s * 22f, 61f + step, s * 9f, 49f)
        kantoLine(0xFF729695, 3f, s * 26f, 40f, s * 32f, 54f + step)
    }
    middleShape(0xFFF5F5F3) {
        moveTo(-23f, -41f)
        quadraticTo(-53f, -29f, -55f, 7f)
        lineTo(-44f, 1f)
        lineTo(-49f, 28f)
        lineTo(-35f, 23f)
        lineTo(-35f, 43f)
        lineTo(-13f, 37f)
        lineTo(1f, 50f)
        lineTo(14f, 37f)
        lineTo(35f, 43f)
        lineTo(35f, 23f)
        lineTo(50f, 27f)
        lineTo(44f, 1f)
        lineTo(54f, 8f)
        quadraticTo(56f, -29f, 23f, -41f)
    }
    kantoPolygon(0xFFCCDCDD, -24f, -6f, -11f, 5f, 0f, -3f, 12f, 7f,
        27f, -4f, 19f, 20f, 0f, 35f, -23f, 19f)
    for (s in listOf(-1f, 1f)) {
        val swing = m * s * 5f
        middleShape(0xFFEDF2F1) {
            moveTo(s * 39f, -23f)
            quadraticTo(s * 75f, -21f, s * 89f, 14f + swing)
            lineTo(s * 82f, 15f + swing)
            lineTo(s * 91f, 33f + swing)
            lineTo(s * 78f, 29f + swing)
            lineTo(s * 73f, 43f + swing)
            lineTo(s * 55f, 34f + swing)
            quadraticTo(s * 44f, 11f, s * 39f, -23f)
        }
        kantoPolygon(0xFF457574, s * 62f, 30f + swing, s * 82f, 27f + swing,
            s * 86f, 45f + swing, s * 76f, 50f + swing, s * 59f, 44f + swing)
        for (i in 0..2) kantoLine(0xFF264E55, 2f, s * (65f + i * 6f), 36f + swing,
            s * (67f + i * 6f), 45f + swing)
        kantoLine(0xFFB8CFD1, 2f, s * 54f, -7f, s * 62f, 15f + swing)
    }
    kantoPolygon(0xFFF5F8F7, -27f, -31f, -21f, -57f, -11f, -50f,
        0f, -74f, 12f, -51f, 21f, -60f, 30f, -30f, 19f, -10f, 0f, -1f, -21f, -10f)
    kantoPolygon(0xFF405F67, -22f, -35f, -5f, -31f, 0f, -23f, 7f, -32f,
        23f, -36f, 17f, -21f, 0f, -16f, -16f, -21f)
    kantoLine(0xFFDDEB96, 3f, -16f, -30f, -9f, -28f)
    kantoLine(0xFFDDEB96, 3f, 9f, -28f, 17f, -30f)
    kantoPolygon(0xFF436E70, -8f, -22f, 8f, -22f, 0f, -13f)
    kantoPolygon(0xFFD6E2E3, -15f, -14f, -8f, -3f, 0f, -8f, 9f, -3f, 17f, -14f, 0f, 4f)
}

private fun DrawScope.middleScizor(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val wing = m * s * 6f
        middleShape(0xFFD8E9EF) {
            moveTo(s * 9f, -7f)
            quadraticTo(s * 35f, -62f + wing, s * 61f, -69f + wing)
            quadraticTo(s * 60f, -28f + wing, s * 9f, 10f)
        }
        kantoLine(0xFF9BAFC3, 1.8f, s * 17f, -7f, s * 52f, -55f + wing)
        kantoPolygon(0xFF393E51, s * 6f, 17f, s * 18f, 15f, s * 30f, 38f,
            s * 19f, 43f, s * 8f, 32f)
        val step = m * s * 4f
        kantoPolygon(0xFFD5575F, s * 19f, 33f, s * 31f, 34f, s * 37f, 55f + step,
            s * 49f, 58f + step, s * 20f, 60f + step, s * 14f, 48f)
        kantoLine(0xFFF98A82, 3f, s * 23f, 39f, s * 26f, 52f + step)
    }
    kantoOval(0xFF313C4E, -15f, -10f, 30f, 42f)
    kantoPolygon(0xFFDC5A62, -21f, -17f, -8f, -25f, 11f, -24f, 22f, -15f,
        15f, 2f, 0f, 8f, -16f, 1f)
    kantoPolygon(0xFFD75160, -13f, 12f, 12f, 12f, 18f, 29f, 0f, 38f, -18f, 28f)
    kantoLine(0xFF7E364F, 3f, -13f, 25f, 13f, 25f)
    for (s in listOf(-1f, 1f)) {
        val snap = m * s * 5f
        kantoLine(KANTO_INK, 11f, s * 20f, -10f, s * 37f, -1f + snap, s * 48f, -12f + snap)
        kantoLine(0xFFB74654, 7f, s * 20f, -10f, s * 37f, -1f + snap, s * 48f, -12f + snap)
        middleShape(0xFFE56068) {
            moveTo(s * 43f, -21f + snap)
            lineTo(s * 58f, -39f + snap)
            lineTo(s * 87f, -38f + snap)
            lineTo(s * 96f, -15f + snap)
            lineTo(s * 70f, -12f + snap)
            lineTo(s * 60f, -1f + snap)
            lineTo(s * 45f, -4f + snap)
        }
        kantoPolygon(0xFF343A4B, s * 52f, -3f + snap, s * 61f, -13f + snap,
            s * 70f, -6f + snap, s * 78f, -15f + snap, s * 86f, -8f + snap,
            s * 96f, -15f + snap, s * 88f, 8f + snap, s * 67f, 15f + snap)
        kantoPolygon(0xFFCA4859, s * 57f, 7f + snap, s * 74f, 8f + snap,
            s * 90f, 2f + snap, s * 88f, 14f + snap, s * 68f, 20f + snap)
        kantoOval(0xFFF4D784, s * 64f - 6f, -29f + snap, 12f, 11f)
        kantoOval(KANTO_INK, s * 65f - 2f, -27f + snap, 4f, 7f)
        kantoLine(0xFFFFA79D, 3f, s * 59f, -34f + snap, s * 82f, -33f + snap)
    }
    kantoPolygon(0xFFDB5561, -19f, -36f, -24f, -57f, -9f, -49f,
        0f, -60f, 8f, -49f, 24f, -57f, 20f, -35f, 12f, -24f, -13f, -25f)
    kantoPolygon(0xFF344154, -19f, -41f, -5f, -37f, 0f, -30f, 6f, -38f,
        19f, -42f, 13f, -30f, -12f, -30f)
    kantoLine(0xFFDBF1A0, 3f, -15f, -37f, -8f, -34f)
    kantoLine(0xFFDBF1A0, 3f, 8f, -34f, 15f, -38f)
    kantoPolygon(0xFFF3817D, -5f, -45f, 0f, -53f, 5f, -45f, 0f, -37f)
}

private fun DrawScope.middlePinsir(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val flap = m * s * 6f
        middleShape(0xFFE8A863) {
            moveTo(s * 14f, -15f)
            cubicTo(s * 37f, -53f, s * 78f, -61f + flap, s * 96f, -43f + flap)
            quadraticTo(s * 102f, -18f + flap, s * 75f, -1f + flap)
            quadraticTo(s * 82f, 28f + flap, s * 61f, 30f + flap)
            quadraticTo(s * 37f, 22f, s * 14f, 7f)
        }
        middleCurve(0xFF654B43, 3f) {
            moveTo(s * 19f, -9f)
            quadraticTo(s * 48f, -23f, s * 87f, -41f + flap)
        }
        kantoLine(0xFF654B43, 2f, s * 47f, -21f, s * 68f, 20f + flap)
        kantoLine(0xFF654B43, 2f, s * 64f, -29f + flap, s * 86f, -12f + flap)
        val step = m * s * 4f
        kantoLine(KANTO_INK, 13f, s * 15f, 26f, s * 29f, 44f + step, s * 39f, 52f + step)
        kantoLine(0xFF92735A, 9f, s * 15f, 26f, s * 29f, 44f + step, s * 39f, 52f + step)
        middleClaws(s * 36f - 5f, 55f + step)
    }
    middleShape(0xFF9A7B61) {
        moveTo(-24f, -32f)
        quadraticTo(-37f, -8f, -28f, 22f)
        quadraticTo(0f, 49f, 29f, 22f)
        quadraticTo(37f, -9f, 23f, -32f)
    }
    kantoPolygon(0xFFBF9875, -21f, -18f, -7f, -27f, -2f, 29f, -19f, 17f)
    kantoPolygon(0xFF745847, 7f, -27f, 24f, -14f, 19f, 18f, 2f, 30f)
    for (s in listOf(-1f, 1f)) {
        val lift = m * s * 4f
        kantoLine(KANTO_INK, 12f, s * 27f, -9f, s * 47f, 6f + lift, s * 57f, -10f + lift)
        kantoLine(0xFF9A7B61, 8f, s * 27f, -9f, s * 47f, 6f + lift, s * 57f, -10f + lift)
        kantoPolygon(0xFFF1EBD8, s * 44f, 0f + lift, s * 39f, -13f + lift, s * 51f, -3f + lift)
        middleClaws(s * 56f - 5f, -9f + lift)
        middleShape(0xFFD8D2BE) {
            moveTo(s * 18f, -28f)
            cubicTo(s * 38f, -38f, s * 49f, -61f, s * 34f, -75f)
            quadraticTo(s * 61f, -67f, s * 52f, -40f)
            lineTo(s * 30f, -18f)
        }
        kantoPolygon(0xFFEBE7D6, s * 34f, -42f, s * 23f, -50f, s * 39f, -50f)
        kantoPolygon(0xFFEBE7D6, s * 29f, -31f, s * 18f, -37f, s * 35f, -39f)
        kantoLine(0xFFAFAC9E, 2f, s * 46f, -59f, s * 44f, -43f, s * 29f, -26f)
    }
    kantoOval(0xFFAE8B6C, -24f, -45f, 48f, 36f)
    kantoPolygon(0xFFF7EBC9, -20f, -32f, -6f, -32f, -10f, -22f, -20f, -22f)
    kantoPolygon(0xFFF7EBC9, 6f, -32f, 20f, -32f, 20f, -22f, 10f, -22f)
    kantoEyes(0f, -27f, 14f, 2.7f)
    kantoPolygon(0xFF393346, -9f, -19f, 9f, -19f, 13f, 13f, 0f, 21f, -13f, 13f)
    for (i in 0..3) {
        val y = -13f + i * 8f
        kantoPolygon(0xFFF5F0D5, -9f, y, -2f, y + 3f, -10f, y + 6f)
        kantoPolygon(0xFFF5F0D5, 9f, y, 2f, y + 3f, 10f, y + 6f)
    }
}

private fun DrawScope.middleHeracross(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 4f
        kantoPolygon(0xFF3E6798, s * 6f, 23f, s * 22f, 19f, s * 38f, 44f + step,
            s * 40f, 59f + step, s * 16f, 57f + step, s * 13f, 42f)
        kantoPolygon(0xFF6795BD, s * 22f, 34f, s * 33f, 44f + step, s * 29f, 55f + step, s * 18f, 48f)
        middleClaws(s * 25f - 6f, 57f + step)
    }
    kantoOval(0xFF2C4A79, -31f, -17f, 62f, 64f)
    middleShape(0xFF497CB1) {
        moveTo(-29f, -11f)
        quadraticTo(-25f, -35f, 0f, -32f)
        quadraticTo(27f, -36f, 30f, -10f)
        lineTo(20f, 22f)
        lineTo(0f, 31f)
        lineTo(-22f, 21f)
    }
    kantoLine(0xFF2B4C79, 3f, 0f, -26f, 0f, 29f)
    kantoPolygon(0xFF759ECC, -24f, -12f, -6f, -22f, -7f, 6f, -18f, 12f)
    for (s in listOf(-1f, 1f)) {
        val punch = m * s * 6f
        kantoLine(KANTO_INK, 17f, s * 26f, -7f, s * 42f, 7f + punch, s * 54f, -3f + punch)
        kantoLine(0xFF456FA0, 13f, s * 26f, -7f, s * 42f, 7f + punch, s * 54f, -3f + punch)
        middleShape(0xFF4776A9) {
            moveTo(s * 47f, -16f + punch)
            quadraticTo(s * 56f, -43f + punch, s * 80f, -32f + punch)
            quadraticTo(s * 99f, -21f + punch, s * 87f, 12f + punch)
            lineTo(s * 68f, 22f + punch)
            quadraticTo(s * 50f, 13f + punch, s * 47f, -16f + punch)
        }
        kantoOval(0xFF7EA1C6, s * 72f - 15f, -31f + punch, 30f, 24f)
        kantoOval(0xFF263E62, s * 73f - 9f, -27f + punch, 18f, 16f)
        kantoOval(0xFF405D86, s * 73f - 5f, -23f + punch, 10f, 8f)
        kantoLine(0xFF2C4C79, 3f, s * 59f, 2f + punch, s * 83f, 6f + punch)
        kantoPolygon(0xFFDDE5DF, s * 55f, 5f + punch, s * 48f, 16f + punch, s * 62f, 13f + punch)
        kantoPolygon(0xFFDDE5DF, s * 80f, 13f + punch, s * 91f, 22f + punch, s * 76f, 23f + punch)
    }
    kantoOval(0xFF5288B8, -25f, -48f, 50f, 36f)
    kantoPolygon(0xFFECAC68, -25f, -35f, -15f, -44f, 0f, -37f, 15f, -44f,
        25f, -35f, 18f, -23f, 1f, -25f, -17f, -23f)
    kantoEyes(0f, -32f, 15f, 4f)
    middleShape(0xFF497BAC) {
        moveTo(-8f, -35f)
        lineTo(-12f, -60f)
        lineTo(-25f, -72f)
        lineTo(-9f, -73f)
        lineTo(0f, -63f)
        lineTo(10f, -73f)
        lineTo(25f, -72f)
        lineTo(12f, -59f)
        lineTo(8f, -35f)
    }
    kantoPolygon(0xFF90B5D5, -5f, -39f, -6f, -60f, 0f, -56f, 5f, -61f, 5f, -39f)
    kantoSmile(0f, -20f, 15f)
}

private fun DrawScope.middleHawlucha(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val spread = m * s * 6f
        middleShape(0xFFB44662) {
            moveTo(s * 15f, -24f)
            quadraticTo(s * 43f, -46f, s * 71f, -53f + spread)
            lineTo(s * 65f, -30f + spread)
            lineTo(s * 86f, -39f + spread)
            lineTo(s * 78f, -14f + spread)
            lineTo(s * 98f, -19f + spread)
            lineTo(s * 77f, 7f + spread)
            lineTo(s * 88f, 13f + spread)
            lineTo(s * 61f, 27f + spread)
            lineTo(s * 61f, 41f + spread)
            quadraticTo(s * 29f, 27f, s * 17f, -1f)
        }
        middleShape(0xFF418D78) {
            moveTo(s * 24f, -17f)
            lineTo(s * 65f, -39f + spread)
            lineTo(s * 57f, -19f + spread)
            lineTo(s * 75f, -25f + spread)
            lineTo(s * 60f, -3f + spread)
            lineTo(s * 74f, -6f + spread)
            lineTo(s * 50f, 18f + spread)
            quadraticTo(s * 36f, 4f, s * 24f, -17f)
        }
        kantoLine(0xFFF1CF78, 3f, s * 28f, -14f, s * 48f, 8f + spread)
        val kick = m * s * 6f
        kantoLine(KANTO_INK, 16f, s * 12f, 19f, s * 27f, 35f + kick, s * 38f, 48f + kick)
        kantoLine(0xFF384055, 12f, s * 12f, 19f, s * 27f, 35f + kick, s * 38f, 48f + kick)
        kantoPolygon(0xFFE5BD72, s * 29f, 43f + kick, s * 43f, 43f + kick,
            s * 51f, 55f + kick, s * 29f, 57f + kick)
        middleClaws(s * 38f - 5f, 55f + kick)
    }
    middleShape(0xFF343748) {
        moveTo(-27f, -27f)
        quadraticTo(0f, -38f, 28f, -27f)
        lineTo(35f, -8f)
        lineTo(21f, 11f)
        lineTo(16f, 28f)
        quadraticTo(0f, 36f, -17f, 26f)
        lineTo(-23f, 8f)
        lineTo(-35f, -8f)
    }
    for (s in listOf(-1f, 1f)) {
        middleShape(0xFFF7ECDD) {
            moveTo(s * 3f, -21f)
            quadraticTo(s * 23f, -28f, s * 29f, -13f)
            quadraticTo(s * 26f, 1f, s * 5f, -4f)
        }
        kantoLine(KANTO_INK, 13f, s * 28f, -19f, s * 47f, -22f + m * s * 5f, s * 59f, -35f + m * s * 5f)
        kantoLine(0xFF3F927C, 9f, s * 28f, -19f, s * 47f, -22f + m * s * 5f, s * 59f, -35f + m * s * 5f)
        middleClaws(s * 57f - 4f, -35f + m * s * 5f)
    }
    kantoPolygon(0xFFE5B767, -16f, 15f, -3f, 20f, 0f, 12f, 4f, 20f, 17f, 15f, 12f, 27f, -12f, 27f)
    kantoPolygon(0xFFC44D62, -13f, -43f, -26f, -67f, -8f, -63f, 0f, -75f,
        9f, -62f, 29f, -67f, 16f, -41f)
    kantoOval(0xFF4FA584, -23f, -55f, 46f, 34f)
    kantoPolygon(0xFFF1C76C, -18f, -43f, -7f, -46f, 0f, -36f, 9f, -47f,
        19f, -43f, 11f, -31f, -10f, -31f)
    kantoEyes(0f, -39f, 11f, 3.4f)
    kantoPolygon(0xFFF2D388, -7f, -32f, 0f, -36f, 8f, -32f, 1f, -22f)
    kantoLine(0xFFD76370, 3f, -2f, -53f, 0f, -43f)
}

private fun DrawScope.middleScrafty(m: Float) {
    val swagger = m * 5f
    middleShape(0xFFB89154) {
        moveTo(13f, 19f)
        quadraticTo(57f, 23f, 72f, 44f + swagger)
        lineTo(87f, 39f + swagger)
        quadraticTo(87f, 61f, 58f, 53f)
        lineTo(12f, 35f)
    }
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 4f
        kantoPolygon(0xFFDEB565, s * 7f, 18f, s * 31f, 23f, s * 34f, 49f + step,
            s * 26f, 57f + step, s * 11f, 52f + step)
        kantoOval(0xFFD77B4C, s * 25f - 14f, 50f + step, 28f, 11f)
        kantoLine(0xFF936E42, 2f, s * 18f, 35f, s * 21f, 48f + step)
    }
    middleShape(0xFFD98552) {
        moveTo(-15f, -21f)
        quadraticTo(-25f, -2f, -19f, 24f)
        quadraticTo(0f, 37f, 24f, 22f)
        lineTo(17f, -19f)
    }
    kantoPolygon(0xFFB2AFAB, -10f, -17f, 10f, -17f, 14f, 19f, 0f, 26f, -15f, 18f)
    for (i in 0..3) kantoLine(0xFF7D7B7B, 2f, -10f, -6f + i * 7f, 11f, -6f + i * 7f)
    for (s in listOf(-1f, 1f)) {
        val swing = m * s * 6f
        middleShape(0xFFE9CA7D) {
            moveTo(s * 12f, -23f)
            lineTo(s * 34f, -20f)
            lineTo(s * 43f, -2f + swing)
            lineTo(s * 60f, 8f + swing)
            lineTo(s * 53f, 24f + swing)
            lineTo(s * 42f, 17f + swing)
            lineTo(s * 38f, 36f + swing)
            lineTo(s * 24f, 30f + swing)
            lineTo(s * 17f, 43f + swing)
            quadraticTo(s * 19f, 8f, s * 12f, -23f)
        }
        kantoPolygon(0xFFC79F57, s * 17f, -16f, s * 28f, -20f, s * 36f, -3f + swing,
            s * 26f, 6f + swing, s * 23f, 19f)
        kantoLine(KANTO_INK, 11f, s * 40f, 4f + swing, s * 60f, 1f + swing, s * 67f, -10f + swing)
        kantoLine(0xFFE4965A, 7f, s * 40f, 4f + swing, s * 60f, 1f + swing, s * 67f, -10f + swing)
        kantoOval(0xFFE59A60, s * 66f - 7f, -18f + swing, 14f, 13f)
    }
    kantoPolygon(0xFFC64D52, -12f, -44f, -18f, -62f, -9f, -61f, -5f, -75f,
        6f, -66f, 14f, -73f, 19f, -52f, 10f, -40f)
    middleShape(0xFFE69A5C) {
        moveTo(-20f, -48f)
        quadraticTo(0f, -63f, 23f, -47f)
        lineTo(27f, -27f)
        quadraticTo(25f, -8f, 0f, -10f)
        quadraticTo(-28f, -11f, -27f, -28f)
    }
    kantoOval(0xFFF2E6C3, -23f, -41f, 18f, 15f)
    kantoOval(0xFFF2E6C3, 6f, -41f, 18f, 15f)
    kantoEyes(0f, -32f, 15f, 3f)
    kantoLine(0xFF77747C, 5f, -22f, -39f, -7f, -36f)
    kantoLine(0xFF77747C, 5f, 7f, -36f, 23f, -40f)
    kantoSmile(1f, -21f, 30f)
    kantoPolygon(0xFFFFEAD2, 13f, -21f, 15f, -15f, 19f, -22f)
}

private fun DrawScope.middleChandelure(m: Float) {
    for (s in listOf(-1f, 1f)) {
        for (tier in 0..2) {
            val x = 41f + tier * 24f
            val y = -21f + tier * 13f
            val sway = m * s * (3f + tier)
            middleCurve(KANTO_INK, 8f) {
                moveTo(s * 10f, 16f + tier * 5f)
                cubicTo(s * x, 36f + tier * 4f, s * (x + 8f), 12f + sway,
                    s * x, y + sway)
            }
            middleCurve(0xFF6C5C89, 4f) {
                moveTo(s * 11f, 16f + tier * 5f)
                cubicTo(s * x, 35f + tier * 4f, s * (x + 8f), 12f + sway,
                    s * x, y + sway)
            }
            kantoOval(0xFF41364F, s * x - 10f, y - 3f + sway, 20f, 8f)
            kantoPolygon(0xFFB9AECF, s * x - 5f, y + sway, s * x - 5f, y - 10f + sway,
                s * x + 5f, y - 10f + sway, s * x + 5f, y + sway)
            middleFlame(s * x, y - 7f + sway, 10f, 24f, m * s, 0xFF9B77DB)
        }
    }
    middleShape(0xFF373449) {
        moveTo(-21f, -30f)
        quadraticTo(0f, -48f, 22f, -29f)
        lineTo(28f, 4f)
        lineTo(21f, 34f)
        lineTo(7f, 46f)
        lineTo(0f, 63f)
        lineTo(-8f, 46f)
        lineTo(-23f, 32f)
        lineTo(-29f, 4f)
    }
    middleShape(0xFFB7A0D7) {
        moveTo(-17f, -24f)
        quadraticTo(0f, -37f, 17f, -24f)
        lineTo(21f, 5f)
        quadraticTo(21f, 30f, 0f, 36f)
        quadraticTo(-22f, 29f, -21f, 4f)
    }
    middleShape(0xFFD3C8EB) {
        moveTo(-9f, -24f)
        quadraticTo(-3f, -32f, 2f, -25f)
        lineTo(-2f, 31f)
        quadraticTo(-13f, 26f, -9f, -24f)
    }
    for (s in listOf(-1f, 1f)) {
        middleCurve(0xFF413850, 3f) {
            moveTo(s * 9f, -30f)
            quadraticTo(s * 17f, -4f, s * 7f, 33f)
        }
        kantoOval(0xFFFFE98F, s * 10f - 5f, -3f, 10f, 14f)
        kantoPolygon(0xFF645377, s * 17f, -28f, s * 26f, -41f,
            s * 25f, -20f, s * 16f, -11f)
    }
    kantoLine(KANTO_INK, 3f, -16f, 18f, 16f, 18f)
    kantoOval(0xFF41384F, -18f, -40f, 36f, 11f)
    middleFlame(0f, -37f, 17f, 37f, m, 0xFF8D6AD1)
    kantoLine(0xFF8B7DA4, 2f, -17f, 36f, 0f, 43f, 17f, 36f)
    middleGem(0xFFCCADEA, 0f, 50f, 5f, 8f)
}

private fun DrawScope.middleAerodactyl(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val flap = m * s * 6f
        middleShape(0xFF9C8BB7) {
            moveTo(s * 17f, -13f)
            lineTo(s * 58f, -53f + flap)
            lineTo(s * 86f, -69f + flap)
            lineTo(s * 98f, -7f + flap)
            quadraticTo(s * 77f, -16f + flap, s * 65f, 10f + flap)
            quadraticTo(s * 44f, 3f, s * 28f, 30f)
        }
        middleShape(0xFFBB87BE) {
            moveTo(s * 27f, -12f)
            lineTo(s * 60f, -44f + flap)
            lineTo(s * 82f, -58f + flap)
            lineTo(s * 88f, -18f + flap)
            quadraticTo(s * 74f, -20f + flap, s * 62f, 1f + flap)
            quadraticTo(s * 43f, -3f, s * 27f, 17f)
        }
        kantoLine(0xFF786783, 3f, s * 60f, -44f + flap, s * 62f, 1f + flap)
        kantoPolygon(0xFF4D4A60, s * 52f, -46f + flap, s * 60f, -65f + flap,
            s * 70f, -47f + flap, s * 63f, -35f + flap)
        kantoPolygon(0xFF4D4A60, s * 82f, -32f + flap, s * 97f, -40f + flap,
            s * 93f, -21f + flap, s * 85f, -17f + flap)
    }
    middleShape(0xFF827894) {
        moveTo(0f, 26f)
        quadraticTo(24f, 41f, 60f, 42f + m * 5f)
        lineTo(77f, 32f + m * 5f)
        lineTo(72f, 51f + m * 5f)
        lineTo(57f, 46f + m * 5f)
        quadraticTo(22f, 56f, -6f, 41f)
    }
    middleShape(0xFF9D91B1) {
        moveTo(-18f, -29f)
        quadraticTo(-30f, -11f, -26f, 17f)
        quadraticTo(0f, 42f, 26f, 17f)
        quadraticTo(31f, -13f, 17f, -29f)
    }
    kantoPolygon(0xFFD6B9D0, -13f, -10f, 12f, -10f, 18f, 16f, 0f, 29f, -18f, 16f)
    for (s in listOf(-1f, 1f)) {
        val kick = m * s * 4f
        kantoLine(KANTO_INK, 13f, s * 18f, 20f, s * 32f, 32f + kick, s * 28f, 46f + kick)
        kantoLine(0xFF8E819F, 9f, s * 18f, 20f, s * 32f, 32f + kick, s * 28f, 46f + kick)
        kantoPolygon(0xFF454558, s * 24f, 43f + kick, s * 39f, 52f + kick,
            s * 30f, 54f + kick, s * 19f, 47f + kick)
        middleClaws(s * 28f - 6f, 53f + kick)
        kantoPolygon(0xFF515165, s * 18f, 9f, s * 35f, 16f, s * 22f, 20f)
    }
    kantoPolygon(0xFF8D829F, -21f, -34f, -27f, -68f, -9f, -51f,
        10f, -51f, 25f, -68f, 22f, -34f)
    middleShape(0xFFA89BB9) {
        moveTo(-20f, -49f)
        quadraticTo(1f, -58f, 22f, -45f)
        lineTo(17f, -29f)
        lineTo(30f, -23f)
        quadraticTo(11f, -9f, -16f, -17f)
        lineTo(-27f, -25f)
    }
    kantoPolygon(0xFF393C51, -17f, -22f, 25f, -24f, 13f, -8f, -12f, -12f)
    for (i in 0..3) kantoPolygon(0xFFF2EDD9, -11f + i * 8f, -21f,
        -6f + i * 8f, -21f, -8f + i * 8f, -14f)
    kantoLine(0xFFEFDEA3, 4f, -15f, -40f, -6f, -36f)
    kantoLine(0xFFEFDEA3, 4f, 7f, -36f, 17f, -40f)
    kantoEyes(0f, -37f, 11f, 2.5f)
    kantoPolygon(0xFF4A495E, -19f, -13f, -6f, -10f, 0f, -1f, 9f, -11f,
        20f, -14f, 14f, 4f, 6f, 0f, 0f, 13f, -8f, 0f, -15f, 3f)
}

private fun DrawScope.middleSteelix(m: Float) {
    val tail = m * 5f
    kantoPolygon(0xFF98A9BA, 59f, 38f + tail, 75f, 32f + tail, 98f, 43f + tail,
        77f, 49f + tail, 64f, 47f + tail)
    kantoPolygon(0xFFD2E4E8, 70f, 36f + tail, 95f, 43f + tail, 75f, 42f + tail)
    kantoPolygon(0xFF8395AC, 39f, 26f + tail * .7f, 65f, 26f + tail,
        78f, 42f + tail, 62f, 56f + tail, 39f, 49f)
    kantoPolygon(0xFFD7E5E8, 42f, 28f + tail * .7f, 62f, 28f + tail,
        68f, 37f + tail, 46f, 38f)
    kantoPolygon(0xFF76899F, 7f, 26f, 33f, 19f, 47f, 33f + tail * .5f,
        42f, 53f, 17f, 59f, 2f, 43f)
    kantoPolygon(0xFFAFD3DD, 13f, 26f, 34f, 23f, 40f, 34f, 18f, 39f, 7f, 36f)
    kantoPolygon(0xFF91A6BA, -25f, 11f, 1f, 14f, 16f, 33f, 7f, 52f,
        -17f, 47f, -33f, 31f)
    kantoPolygon(0xFFB8D8E1, -26f, 12f, -2f, 16f, 6f, 30f, -14f, 29f, -28f, 23f)
    kantoPolygon(0xFF8196AC, -52f, -7f, -26f, -3f, -16f, 16f,
        -28f, 35f, -54f, 25f, -63f, 9f)
    kantoPolygon(0xFFBBD5DD, -52f, -6f, -28f, -1f, -25f, 9f, -46f, 12f, -59f, 6f)
    middleGem(0xFF80D1E5, -51f, 18f, 13f, 15f)
    middleGem(0xFFB9A7E7, -14f, 39f, 10f, 14f)
    middleGem(0xFFAFDCB9, 27f, 43f, 10f, 14f)
    for (s in listOf(-1f, 1f)) {
        val flex = m * s * 3f
        kantoPolygon(0xFFA2D5E1, -35f + s * 29f, 4f, -35f + s * 53f, -12f + flex,
            -35f + s * 46f, 12f + flex, -35f + s * 30f, 17f)
        kantoLine(0xFFF4FAF5, 2f, -35f + s * 34f, 7f, -35f + s * 47f, -4f + flex)
    }
    kantoPolygon(0xFFB9C9D0, -61f, -41f, -47f, -61f, -18f, -65f,
        13f, -47f, 8f, -19f, -8f, -4f, -41f, -9f, -61f, -25f)
    kantoPolygon(0xFFE0E9E6, -58f, -42f, -46f, -57f, -18f, -60f,
        6f, -46f, -18f, -39f, -38f, -43f)
    kantoPolygon(0xFF748B9E, -18f, -39f, 10f, -46f, 7f, -22f,
        -8f, -10f, -17f, -17f)
    kantoPolygon(0xFFC7D5D8, -54f, -47f, -68f, -63f, -43f, -56f)
    kantoPolygon(0xFFC7D5D8, -13f, -57f, 1f, -73f, 3f, -51f)
    kantoPolygon(0xFFD2E3E7, -31f, -57f, -26f, -76f, -18f, -60f)
    kantoPolygon(0xFF263A51, -56f, -34f, -35f, -30f, -20f, -34f,
        -22f, -20f, -40f, -20f, -55f, -25f)
    kantoLine(0xFFEE827D, 3f, -51f, -30f, -43f, -28f)
    kantoLine(0xFFEE827D, 3f, -33f, -28f, -26f, -30f)
    val jaw = m * 2f
    kantoPolygon(0xFF343E54, -51f, -14f, -10f, -10f, -6f, 6f + jaw,
        -22f, 13f + jaw, -48f, 3f + jaw)
    kantoPolygon(0xFFCFDCE0, -56f, -11f, -48f, 2f + jaw, -22f, 11f + jaw,
        -7f, 3f + jaw, -8f, 13f + jaw, -23f, 21f + jaw, -53f, 9f + jaw)
    for (i in 0..4) kantoPolygon(0xFFF2EEDD, -48f + i * 8f, -12f + i * .8f,
        -42f + i * 8f, -11f + i * .8f, -43f + i * 8f, -3f + i * .8f)
}

private fun DrawScope.middleAggron(m: Float) {
    middleShape(0xFF788D9B) {
        moveTo(22f, 27f)
        quadraticTo(50f, 39f, 89f, 22f + m * 5f)
        lineTo(98f, 25f + m * 5f)
        quadraticTo(73f, 58f, 29f, 53f)
        lineTo(11f, 42f)
    }
    kantoPolygon(0xFFD9E2E5, 52f, 34f, 67f, 15f + m * 5f, 70f, 32f + m * 4f)
    kantoPolygon(0xFFD9E2E5, 72f, 31f + m * 4f, 84f, 14f + m * 5f, 86f, 28f + m * 5f)
    for (s in listOf(-1f, 1f)) {
        val stomp = m * s * 3f
        kantoPolygon(0xFF4C606F, s * 10f, 22f, s * 32f, 20f, s * 43f, 45f + stomp,
            s * 37f, 60f + stomp, s * 11f, 58f + stomp)
        kantoPolygon(0xFFD4DEE1, s * 15f, 33f, s * 36f, 33f, s * 41f, 49f + stomp,
            s * 33f, 55f + stomp, s * 15f, 50f + stomp)
        middleClaws(s * 24f - 6f, 60f + stomp)
    }
    middleShape(0xFFE0E7E7) {
        moveTo(-19f, -42f)
        quadraticTo(-49f, -38f, -54f, -10f)
        lineTo(-39f, 14f)
        lineTo(40f, 14f)
        lineTo(54f, -10f)
        quadraticTo(48f, -39f, 18f, -43f)
    }
    middleShape(0xFF526977) {
        moveTo(-21f, -20f)
        quadraticTo(-33f, 1f, -26f, 28f)
        quadraticTo(0f, 48f, 27f, 28f)
        quadraticTo(34f, 1f, 21f, -21f)
    }
    for (i in 0..3) {
        val y = -7f + i * 11f
        kantoPolygon(0xFFBDCDD3, -23f, y, 0f, y + 4f, 24f, y,
            22f, y + 8f, 0f, y + 12f, -22f, y + 8f)
    }
    for (s in listOf(-1f, 1f)) {
        val punch = m * s * 5f
        kantoLine(KANTO_INK, 23f, s * 35f, -19f, s * 47f, 2f + punch, s * 62f, 14f + punch)
        kantoLine(0xFF617581, 19f, s * 35f, -19f, s * 47f, 2f + punch, s * 62f, 14f + punch)
        kantoPolygon(0xFFECF0EE, s * 26f, -23f, s * 43f, -39f,
            s * 60f, -18f + punch, s * 49f, -5f + punch, s * 31f, -8f)
        kantoPolygon(0xFFD8E3E5, s * 50f, -1f + punch, s * 71f, -3f + punch,
            s * 81f, 21f + punch, s * 65f, 35f + punch, s * 49f, 21f + punch)
        kantoPolygon(0xFFF1F5F0, s * 70f, 4f + punch, s * 90f, -8f + punch,
            s * 78f, 18f + punch)
        kantoOval(0xFF5A6D7C, s * 64f - 10f, 23f + punch, 20f, 16f)
        middleClaws(s * 63f - 5f, 36f + punch)
        kantoLine(0xFF97B1BC, 2f, s * 56f, 4f + punch, s * 65f, 23f + punch)
    }
    kantoPolygon(0xFFECF0EC, -32f, -36f, -39f, -62f, -20f, -52f,
        -13f, -68f, 0f, -75f, 14f, -65f, 20f, -50f, 40f, -62f,
        34f, -34f, 22f, -18f, -24f, -18f)
    kantoPolygon(0xFFB6CCD4, -29f, -37f, -11f, -48f, 0f, -35f,
        13f, -48f, 30f, -38f, 23f, -24f, -24f, -24f)
    kantoPolygon(0xFF344756, -24f, -33f, -7f, -33f, -9f, -22f, -24f, -23f)
    kantoPolygon(0xFF344756, 7f, -33f, 24f, -33f, 24f, -23f, 9f, -22f)
    kantoLine(0xFF89D5DE, 3f, -19f, -29f, -12f, -28f)
    kantoLine(0xFF89D5DE, 3f, 12f, -28f, 19f, -29f)
    kantoPolygon(0xFFDCE7E6, -19f, -23f, 0f, -29f, 20f, -23f, 20f, -13f,
        0f, -7f, -19f, -13f)
    kantoLine(KANTO_INK, 2f, -14f, -15f, 0f, -12f, 15f, -15f)
    kantoPolygon(0xFFFAFBF1, -5f, -39f, 1f, -66f, 8f, -39f, 1f, -30f)
}

private fun DrawScope.middleTyranitar(m: Float) {
    val tail = m * 5f
    kantoPolygon(0xFF6C976F, 13f, 25f, 47f, 28f, 59f, 13f + tail,
        62f, 32f + tail, 79f, 19f + tail, 80f, 36f + tail, 99f, 35f + tail,
        80f, 54f + tail, 49f, 58f, 19f, 45f)
    kantoLine(0xFF3D6159, 3f, 49f, 35f, 47f, 51f)
    kantoLine(0xFF3D6159, 3f, 66f, 38f + tail, 64f, 52f + tail)
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFF82A77C, s * 17f, -22f, s * 23f, -68f, s * 39f, -44f,
            s * 57f, -62f, s * 53f, -32f, s * 73f, -39f, s * 65f, -13f,
            s * 85f, -13f, s * 66f, 12f, s * 38f, 14f)
        kantoPolygon(0xFFA5BF8B, s * 28f, -40f, s * 29f, -57f,
            s * 39f, -30f, s * 58f, -34f, s * 47f, -18f)
        val stomp = m * s * 3f
        kantoOval(0xFF79A077, s * 26f - 19f, 31f + stomp, 38f, 30f)
        middleClaws(s * 26f - 7f, 60f + stomp)
    }
    middleShape(0xFF8EAF81) {
        moveTo(-27f, -29f)
        quadraticTo(-44f, -4f, -35f, 28f)
        quadraticTo(0f, 51f, 34f, 29f)
        quadraticTo(46f, -2f, 27f, -28f)
    }
    kantoPolygon(0xFF364C5F, -19f, -2f, 0f, -17f, 22f, 0f, 19f, 27f, 0f, 38f, -19f, 25f)
    kantoPolygon(0xFFCD6072, -14f, 0f, 0f, -10f, 16f, 0f, 13f, 23f, 0f, 31f, -13f, 22f)
    kantoLine(0xFF993C58, 2f, -13f, 8f, 13f, 8f)
    kantoLine(0xFF993C58, 2f, -12f, 19f, 12f, 19f)
    for (s in listOf(-1f, 1f)) {
        val reach = m * s * 5f
        kantoLine(KANTO_INK, 17f, s * 30f, -15f, s * 47f, 0f + reach, s * 54f, 14f + reach)
        kantoLine(0xFF85A97D, 13f, s * 30f, -15f, s * 47f, 0f + reach, s * 54f, 14f + reach)
        kantoPolygon(0xFF9CBD89, s * 36f, -12f, s * 54f, -10f + reach,
            s * 69f, 7f + reach, s * 54f, 23f + reach, s * 40f, 9f + reach)
        middleClaws(s * 54f - 5f, 22f + reach)
        kantoPolygon(0xFF344E53, s * 30f, -3f, s * 38f, 7f, s * 28f, 10f)
        kantoPolygon(0xFF344E53, s * 28f, 15f, s * 34f, 24f, s * 25f, 28f)
    }
    kantoPolygon(0xFF96B684, -22f, -35f, -28f, -58f, -12f, -50f,
        -3f, -75f, 8f, -54f, 24f, -66f, 23f, -37f,
        30f, -24f, 12f, -13f, -16f, -18f, -29f, -26f)
    kantoPolygon(0xFFBACD97, -13f, -40f, -3f, -59f, 7f, -39f, -2f, -31f)
    kantoPolygon(0xFF344857, -23f, -37f, -7f, -35f, -10f, -25f, -20f, -26f)
    kantoPolygon(0xFF344857, 6f, -35f, 22f, -38f, 19f, -26f, 10f, -26f)
    kantoLine(0xFFF4CC98, 3f, -18f, -33f, -11f, -31f)
    kantoLine(0xFFF4CC98, 3f, 10f, -31f, 18f, -33f)
    kantoSmile(0f, -23f, 29f)
    kantoPolygon(0xFFEFECD2, -13f, -23f, -9f, -17f, -6f, -23f)
}

private fun DrawScope.middleGreninja(m: Float) {
    rotate(m * 9f, pivot = Offset(0f, -7f)) {
        middleShape(0xFF4CB5D4) {
            moveTo(-8f, -32f)
            lineTo(9f, -67f)
            lineTo(18f, -28f)
            lineTo(72f, -38f)
            lineTo(38f, -9f)
            lineTo(81f, 10f)
            lineTo(31f, 15f)
            lineTo(20f, 53f)
            lineTo(1f, 28f)
            lineTo(-31f, 48f)
            lineTo(-25f, 12f)
            lineTo(-77f, 8f)
            lineTo(-35f, -14f)
            lineTo(-60f, -48f)
            lineTo(-17f, -29f)
        }
        kantoPolygon(0xFF9DEBF1, -9f, -24f, 7f, -55f, 11f, -21f, 61f, -33f,
            28f, -8f, 67f, 7f, 22f, 8f, 16f, 39f, 0f, 18f,
            -23f, 34f, -19f, 6f, -60f, 5f, -25f, -11f, -45f, -37f)
        kantoOval(0xFF398FAE, -19f, -26f, 38f, 38f)
    }
    for (s in listOf(-1f, 1f)) {
        val grip = m * s * 5f
        kantoLine(KANTO_INK, 13f, s * 9f, -5f, s * 30f, -30f + grip, s * 42f, -46f + grip)
        kantoLine(0xFF273C60, 9f, s * 9f, -5f, s * 30f, -30f + grip, s * 42f, -46f + grip)
        kantoPolygon(0xFF5D99C2, s * 24f, -25f + grip, s * 33f, -39f + grip,
            s * 43f, -33f + grip, s * 33f, -16f + grip)
        kantoLine(0xFFAEDBE0, 3f, s * 40f, -46f + grip, s * 54f, -49f + grip)
        kantoLine(0xFFAEDBE0, 3f, s * 42f, -46f + grip, s * 47f, -55f + grip)
        kantoLine(KANTO_INK, 10f, s * 12f, 8f, s * 37f, 7f - grip, s * 47f, 25f - grip)
        kantoLine(0xFF2D436A, 6f, s * 12f, 8f, s * 37f, 7f - grip, s * 47f, 25f - grip)
        kantoOval(0xFFD8E5D3, s * 35f - 5f, 3f - grip, 10f, 10f)
        for (i in 0..2) kantoLine(0xFFB3DCE0, 2.5f, s * 46f, 22f - grip,
            s * (50f + i * 5f), 30f - grip - i * 2f)
    }
    middleShape(0xFF2F4166) {
        moveTo(-12f, -15f)
        quadraticTo(0f, -24f, 14f, -14f)
        lineTo(18f, 11f)
        lineTo(2f, 24f)
        lineTo(-18f, 11f)
    }
    kantoPolygon(0xFF8EC5CE, -9f, -12f, 7f, -12f, 10f, 9f, 0f, 17f, -11f, 8f)
    middleShape(0xFFE77899) {
        moveTo(12f, 13f)
        cubicTo(-25f, 1f, -43f, 32f + m * 7f, -84f, 18f + m * 7f)
        quadraticTo(-78f, 39f + m * 7f, -49f, 34f + m * 7f)
        quadraticTo(-12f, 16f, 17f, 23f)
    }
    kantoLine(0xFFF7A5BC, 2f, -64f, 29f + m * 7f, -44f, 26f + m * 7f, -26f, 18f)
    kantoPolygon(0xFF2B4167, -21f, 17f, -37f, 8f, -32f, 35f,
        -16f, 41f, 0f, 58f, 17f, 42f, 33f, 36f, 39f, 8f, 20f, 17f)
    kantoPolygon(0xFF639DC1, -22f, 23f, -9f, 27f, 0f, 47f, 10f, 26f,
        25f, 23f, 18f, 41f, 0f, 56f, -18f, 41f)
    kantoPolygon(0xFFF2F0D8, -24f, 23f, -7f, 27f, -14f, 36f, -25f, 31f)
    kantoPolygon(0xFFF2F0D8, 7f, 27f, 24f, 23f, 25f, 31f, 14f, 36f)
    kantoEyes(0f, 30f, 17f, 3.6f)
    kantoLine(0xFFE66189, 4f, -17f, 18f, 0f, 15f, 19f, 18f)
}

private fun DrawScope.middleFalinks(m: Float) {
    for (s in listOf(-1f, 1f)) {
        val march = m * s * 4f
        kantoLine(KANTO_INK, 15f, s * 16f, 12f, s * 31f, 28f, s * 33f, 44f + march)
        kantoLine(0xFF6D5A45, 10f, s * 16f, 12f, s * 31f, 28f, s * 33f, 44f + march)
        kantoOval(0xFF443B40, s * 33f - 16f, 35f + march, 32f, 26f)
        middleShape(0xFFE5B958) {
            moveTo(s * 17f, 44f + march)
            quadraticTo(s * 20f, 24f + march, s * 39f, 30f + march)
            quadraticTo(s * 55f, 33f + march, s * 51f, 49f + march)
            lineTo(s * 41f, 43f + march)
            lineTo(s * 33f, 53f + march)
            lineTo(s * 27f, 44f + march)
        }
        kantoOval(0xFFFFDA78, s * 35f - 4f, 46f + march, 8f, 7f)
        kantoPolygon(0xFFC34E5B, s * 21f, 34f + march, s * 27f, 21f + march,
            s * 41f, 22f + march, s * 45f, 34f + march)
        val guard = m * s * 4f
        kantoLine(KANTO_INK, 15f, s * 20f, -13f, s * 40f, -6f + guard, s * 57f, 5f + guard)
        kantoLine(0xFF725D43, 10f, s * 20f, -13f, s * 40f, -6f + guard, s * 57f, 5f + guard)
        kantoOval(0xFF473D3F, s * 62f - 18f, -12f + guard, 36f, 37f)
        kantoPolygon(0xFFE9BF5E, s * 43f, -7f + guard, s * 52f, -27f + guard,
            s * 73f, -29f + guard, s * 83f, -6f + guard, s * 74f, 17f + guard,
            s * 63f, 23f + guard, s * 50f, 13f + guard)
        kantoPolygon(0xFFAC793C, s * 51f, -5f + guard, s * 61f, -14f + guard,
            s * 73f, -4f + guard, s * 74f, 8f + guard, s * 61f, 15f + guard, s * 50f, 6f + guard)
        kantoOval(0xFF32323E, s * 61f - 10f, -3f + guard, 20f, 10f)
        drawCircle(Color(0xFFFFDC77), 2.5f, Offset(s * 58f, 1f + guard))
        drawCircle(Color(0xFFFFDC77), 2.5f, Offset(s * 66f, 1f + guard))
        kantoPolygon(0xFFD55962, s * 55f, -25f + guard, s * 56f, -40f + guard,
            s * 72f, -38f + guard, s * 73f, -27f + guard)
    }
    kantoOval(0xFF473D42, -21f, 3f, 42f, 35f)
    kantoPolygon(0xFFDEAC4C, -24f, 6f, -12f, -7f, 13f, -7f, 25f, 7f,
        20f, 25f, 0f, 36f, -20f, 25f)
    kantoPolygon(0xFF31343F, -14f, 14f, 14f, 14f, 9f, 25f, -9f, 25f)
    kantoEyes(0f, 19f, 6f, 2.5f)
    kantoPolygon(0xFFFFDB7B, -17f, 6f, 0f, 0f, 18f, 6f, 15f, 11f, 0f, 8f, -14f, 11f)
    kantoOval(0xFF3C3740, -28f, -49f, 56f, 48f)
    middleShape(0xFFE9BC58) {
        moveTo(-32f, -30f)
        quadraticTo(-37f, -65f, 0f, -64f)
        quadraticTo(38f, -65f, 34f, -29f)
        lineTo(26f, -13f)
        lineTo(17f, -29f)
        lineTo(7f, -19f)
        lineTo(0f, -29f)
        lineTo(-9f, -20f)
        lineTo(-17f, -30f)
        lineTo(-27f, -13f)
    }
    kantoPolygon(0xFFF8D784, -24f, -44f, -18f, -56f, 18f, -56f, 26f, -43f,
        14f, -39f, 0f, -47f, -14f, -39f)
    kantoPolygon(0xFFC64D59, -12f, -59f, -18f, -73f, 19f, -73f, 14f, -59f)
    kantoLine(0xFFF08077, 2f, -10f, -69f, 12f, -69f)
    kantoOval(0xFFFFD769, -18f, -21f, 11f, 9f)
    kantoOval(0xFFFFD769, 8f, -21f, 11f, 9f)
    kantoLine(0xFF8F663B, 2f, -29f, -31f, -23f, -24f)
    kantoLine(0xFF8F663B, 2f, 29f, -31f, 23f, -24f)
}

private fun DrawScope.middleChesnaught(m: Float) {
    middleShape(0xFF9A4850) {
        moveTo(-26f, -27f)
        lineTo(27f, -28f)
        quadraticTo(40f, 5f, 58f, 50f + m * 4f)
        lineTo(36f, 42f + m * 4f)
        lineTo(30f, 59f)
        lineTo(5f, 49f)
        lineTo(-16f, 60f)
        lineTo(-27f, 47f)
        lineTo(-48f, 52f - m * 4f)
        quadraticTo(-31f, 6f, -26f, -27f)
    }
    kantoPolygon(0xFFC66763, -20f, -22f, -12f, 42f, -33f, 47f, -26f, 20f)
    for (s in listOf(-1f, 1f)) {
        val step = m * s * 3f
        kantoLine(KANTO_INK, 20f, s * 14f, 22f, s * 25f, 40f + step, s * 25f, 54f + step)
        kantoLine(0xFF967758, 16f, s * 14f, 22f, s * 25f, 40f + step, s * 25f, 54f + step)
        kantoPolygon(0xFFE8E5CF, s * 13f, 42f + step, s * 33f, 41f + step,
            s * 39f, 58f + step, s * 13f, 60f + step)
        middleClaws(s * 25f - 6f, 60f + step)
    }
    middleShape(0xFF6A885A) {
        moveTo(-68f, -21f)
        lineTo(-51f, -56f)
        lineTo(-28f, -52f)
        lineTo(-12f, -65f)
        lineTo(14f, -65f)
        lineTo(30f, -52f)
        lineTo(54f, -56f)
        lineTo(70f, -20f)
        lineTo(48f, 5f)
        lineTo(-47f, 5f)
    }
    middleShape(0xFFF0EAD2) {
        moveTo(-59f, -21f)
        lineTo(-47f, -46f)
        lineTo(-27f, -42f)
        lineTo(-8f, -55f)
        lineTo(9f, -55f)
        lineTo(28f, -41f)
        lineTo(48f, -46f)
        lineTo(61f, -21f)
        lineTo(39f, -7f)
        lineTo(-39f, -7f)
    }
    for (s in listOf(-1f, 1f)) {
        kantoPolygon(0xFFE8E0BB, s * 34f, -45f, s * 44f, -73f,
            s * 54f, -44f, s * 43f, -30f)
        kantoLine(0xFFA8AA7B, 2f, s * 43f, -61f, s * 43f, -35f)
    }
    middleShape(0xFFE8E5D3) {
        moveTo(-20f, -27f)
        quadraticTo(-29f, -2f, -23f, 24f)
        lineTo(-10f, 20f)
        lineTo(0f, 36f)
        lineTo(11f, 21f)
        lineTo(25f, 26f)
        quadraticTo(30f, -2f, 20f, -27f)
    }
    kantoPolygon(0xFFBDBCA1, -17f, -4f, 0f, 6f, 17f, -5f, 11f, 16f, 0f, 23f, -12f, 15f)
    for (s in listOf(-1f, 1f)) {
        val brace = m * s * 4f
        kantoLine(KANTO_INK, 20f, s * 27f, -16f, s * 42f, 7f + brace, s * 62f, 19f + brace)
        kantoLine(0xFF9A7C56, 16f, s * 27f, -16f, s * 42f, 7f + brace, s * 62f, 19f + brace)
        kantoPolygon(0xFF628855, s * 38f, -8f + brace, s * 58f, -17f + brace,
            s * 83f, -2f + brace, s * 88f, 29f + brace, s * 60f, 45f + brace,
            s * 38f, 22f + brace)
        kantoPolygon(0xFFE7E7CC, s * 46f, -4f + brace, s * 59f, -8f + brace,
            s * 75f, 2f + brace, s * 78f, 25f + brace, s * 61f, 35f + brace,
            s * 46f, 18f + brace)
        kantoPolygon(0xFFEEE6C3, s * 66f, 4f + brace, s * 99f, -8f + brace,
            s * 78f, 16f + brace, s * 65f, 22f + brace)
        kantoLine(0xFF9BAA79, 2f, s * 52f, 3f + brace, s * 55f, 21f + brace, s * 63f, 25f + brace)
    }
    kantoOval(0xFF937A57, -20f, -43f, 40f, 30f)
    kantoPolygon(0xFFFBF4DD, -23f, -35f, -13f, -30f, 0f, -36f, 14f, -30f,
        24f, -36f, 20f, -17f, 11f, -19f, 0f, -7f, -11f, -20f, -19f, -17f)
    kantoPolygon(0xFF49694F, -23f, -39f, -13f, -50f, 0f, -46f, 14f, -50f,
        24f, -39f, 15f, -32f, 0f, -40f, -15f, -32f)
    kantoLine(0xFF344C46, 3f, -16f, -29f, -9f, -27f)
    kantoLine(0xFF344C46, 3f, 9f, -27f, 16f, -29f)
    kantoPolygon(0xFF5B584A, -5f, -24f, 6f, -24f, 0f, -19f)
}
