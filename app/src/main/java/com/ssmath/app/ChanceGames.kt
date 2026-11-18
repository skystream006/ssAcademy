package com.ssmath.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class ChanceGame(
    val label: String,
    val tag: String,
    val action: String,
    val animationDurationMs: Int = 2_200,
    val drawCount: Int = 5
) {
    SLOT_MACHINE("Slot machine", "slot-machine", "Pull lever", drawCount = 1),
    SPIN_THE_WHEEL("Spin the wheel", "spin-the-wheel", "Spin"),
    PLINKO("Plinko Drop", "plinko", "Drop ball", animationDurationMs = 4_800, drawCount = 10),
    CLAW_MACHINE("Claw Machine", "claw-machine", "Pick up ball", animationDurationMs = 4_800);

    val playDurationMs: Int get() = animationDurationMs + 400
    val outcomeUnit: String get() = if (this == SPIN_THE_WHEEL) "spin" else "ball"

    fun drawProgress(index: Int, elapsedMs: Int): Float =
        ((elapsedMs - index * playDurationMs).toFloat() / animationDurationMs).coerceIn(0f, 1f)
}

@Composable
internal fun ChanceGameDialog(model: MathViewModel, onDismiss: () -> Unit) {
    val game = model.chanceGame
    val tier = model.slotMachineSelectedTier
    val pool = model.slotMachinePool
    val roll = model.slotMachineRoll
    val busy = model.slotMachineBusy
    val activity = model.slotMachineActivityReward
    val fragment = model.slotMachineIsFragmentReward
    var celebrationDismissed by rememberSaveable(roll) { mutableStateOf(false) }
    var bonusIndex by rememberSaveable(roll) { mutableIntStateOf(0) }
    SettingsDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("${game.label}${tier?.let { " - ${it.label}" } ?: ": select a tier"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (activity) {
                    Text("Reward: Fragment")
                    Text("You earned a reward! Play for free to reveal one fragment.")
                    (if (model.rewardDialogVisible) model.rewardError else model.readingRewardError)?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (fragment) "Reward: Fragment" else "Reward: Whole reward", Modifier.weight(1f))
                        Switch(fragment, model::chooseSlotMachineFragmentReward,
                            enabled = !busy && !model.chanceRoundInProgress && model.slotMachineError == null,
                            modifier = Modifier.testTag("chance-fragment-reward").semantics {
                                contentDescription = "Fragment reward"
                            })
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(when (game) {
                        ChanceGame.SPIN_THE_WHEEL ->
                            "Spin 5 times, one at a time. The most common reward wins one prize."
                        ChanceGame.PLINKO ->
                            "Aim before each of 10 drops. The fullest bin wins one prize."
                        else ->
                            "Aim above the pile. Pick up 5 capsules, one at a time; the most common reward wins one prize."
                    })
                    Text("Ties are decided randomly." + if (game == ChanceGame.SPIN_THE_WHEEL) ""
                        else " Aiming changes the motion, not the reward chances.",
                        style = MaterialTheme.typography.bodySmall)
                    if (game == ChanceGame.CLAW_MACHINE) {
                        Text("Capsules hide their rewards until they reach the prize bin.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (tier == null) {
                    RewardTier.entries.forEach { option ->
                        Button(onClick = { model.chooseSlotMachineTier(option) },
                            enabled = availableRewardsForTier(option, model.disabledRewards,
                                model.tier3RewardChances).isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().testTag("chance-tier-${option.name}")) {
                            Text(option.label)
                        }
                    }
                } else {
                    val enabled = !busy && !model.removingReward && model.slotMachineError == null &&
                        pool != null && !(activity && model.slotMachineAwarded)
                    if (pool != null) {
                        ChanceGameBoard(game, pool, roll, fragment, enabled, busy, model.slotMachineAwarded,
                            model.chanceGamePosition, model::chooseChanceGamePosition, model::spinSlotMachine)
                    }
                    Text(when {
                        busy -> "Revealing / saving..."
                        model.slotMachineError != null -> requireNotNull(model.slotMachineError)
                        model.chanceMotionError != null -> requireNotNull(model.chanceMotionError)
                        model.slotMachineAwarded -> "You won ${
                            requireNotNull(roll?.winner).let { if (fragment) it.fragmentLabel else it.label }
                        }! ${if (fragment) "One fragment" else "One complete reward"} added."
                        model.chanceRoundInProgress -> "Ready for the next ${game.outcomeUnit}."
                        else -> "Ready to play!"
                    }, Modifier.testTag("chance-status").semantics { liveRegion = LiveRegionMode.Polite })
                    if (model.slotMachineError != null && roll?.winner != null && roll.chanceRoundComplete(game)) {
                        Button(onClick = model::retrySlotMachinePrize, enabled = !busy,
                            modifier = Modifier.testTag("chance-retry")) { Text("Retry saving reward") }
                    }
                    ChanceGameOdds(pool, fragment, game)
                    if (!activity) {
                        TextButton(onClick = { model.chooseSlotMachineTier(null) },
                            enabled = !busy && !model.chanceRoundInProgress) {
                            Text("Change tier")
                        }
                    }
                }
                if (!activity) AdminSlotMachineBonuses(model.pokemons, tier)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(if (!activity) "Back to Admin Menu" else if (model.slotMachineAwarded) "Done" else "Finish later")
            }
        },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        modifier = Modifier.testTag("chance-game-dialog")
    )
    if (model.slotMachineAwarded && !busy && !celebrationDismissed && roll != null) {
        SlotMachineWinDialog(roll, activity, title = "Reward revealed!", onDismiss = {
            celebrationDismissed = true
            if (activity) onDismiss()
        })
    }
    val bonuses = roll?.celebrationRewards.orEmpty()
    if (!activity && model.slotMachineAwarded && !busy && celebrationDismissed && bonusIndex < bonuses.size) {
        val index = bonusIndex
        CelebrationDialog(bonuses[index], replay = true, hasNextCelebration = index + 1 < bonuses.size,
            onFinished = { if (bonusIndex == index) bonusIndex++ })
    }
}

@Composable
private fun ChanceGameOdds(pool: SlotMachineRewardPool?, fragment: Boolean, game: ChanceGame) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Available ${if (fragment) "fragments" else "rewards"}", style = MaterialTheme.typography.titleMedium)
        if (pool == null) Text("No available rewards")
        pool?.weights?.keys?.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardImage(type, fragment = fragment,
                    modifier = Modifier.size(64.dp).testTag("chance-available-image-${type.name}"))
                Text("${if (fragment) type.fragmentLabel else type.label}: " +
                    "${slotChancePercent(pool.reelChance(type))}% per ${game.outcomeUnit}",
                    modifier = Modifier.weight(1f).testTag("chance-odds-${type.name}"))
            }
        }
    }
}

@Composable
private fun ChanceGameBoard(
    game: ChanceGame,
    pool: SlotMachineRewardPool,
    roll: SlotMachineRoll?,
    fragment: Boolean,
    enabled: Boolean,
    busy: Boolean,
    awarded: Boolean,
    position: Float,
    onPosition: (Float) -> Unit,
    onPlay: () -> Unit
) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onSurfaceVariant)
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val draws = roll?.chanceDraws.orEmpty()
    val turns = roll?.chanceTurns.orEmpty()
    val elapsed = roll?.chanceElapsedMs ?: 0
    val completed = draws.take(turns.size).filterIndexed { index, _ -> game.drawProgress(index, elapsed) >= 1f }
    val activeIndex = (turns.size - 1).coerceAtLeast(0)
    val progress = game.drawProgress(activeIndex, elapsed)
    val p = FastOutSlowInEasing.transform(progress)
    val activeReward = draws.getOrNull(activeIndex)
    val winner = roll?.winner
    val types = pool.weights.keys.toList()
    fun landingAngle(type: RewardType?): Float {
        val index = types.indexOf(type).coerceAtLeast(0)
        val before = types.take(index).sumOf { pool.weights.getValue(it) }
        return 360f - (before + pool.weights.getValue(types[index]) / 2f) / pool.weights.values.sum() * 360f
    }
    val previousAngle = if (activeIndex == 0) 0f else landingAngle(draws.getOrNull(activeIndex - 1))
    val wheelAngle = if (roll == null) 0f else previousAngle +
        (1_800f + (landingAngle(activeReward) - previousAngle + 360f) % 360f) * p
    val initialClaw = remember(game) { if (game == ChanceGame.CLAW_MACHINE) initialClawScene() else null }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (game == ChanceGame.PLINKO) 1f / PLINKO_HEIGHT else 1f)
            .background(surface, RoundedCornerShape(12.dp))
            .pointerInput(game, enabled) {
                if (enabled && (game == ChanceGame.PLINKO || game == ChanceGame.CLAW_MACHINE)) {
                    fun aim(x: Float) {
                        val fraction = x / size.width
                        onPosition((if (game == ChanceGame.CLAW_MACHINE)
                            0.08f + (fraction - 0.105f) / 0.54f * 0.84f else fraction).coerceIn(0.08f, 0.92f))
                    }
                    detectHorizontalDragGestures(onDragStart = { aim(it.x) }) { change, _ ->
                        change.consume()
                        aim(change.position.x)
                    }
                }
            }
            .testTag("chance-board-${game.name}"), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = game.label }) {
                val w = size.width
                val h = size.height
                when (game) {
                    ChanceGame.SPIN_THE_WHEEL -> {
                        val radius = minOf(w, h) * 0.46f
                        rotate(wheelAngle) {
                            var start = -90f
                            types.forEachIndexed { index, type ->
                                val sweep = pool.reelChance(type).toFloat() * 360f
                                drawArc(colors[index % colors.size], start, sweep, true,
                                    Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2))
                                drawArc(ink, start, sweep, false,
                                    Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
                                    style = Stroke(2.dp.toPx()))
                                start += sweep
                            }
                        }
                        drawPath(Path().apply {
                            moveTo(center.x, center.y - radius + 16.dp.toPx())
                            lineTo(center.x - 10.dp.toPx(), center.y - radius - 5.dp.toPx())
                            lineTo(center.x + 10.dp.toPx(), center.y - radius - 5.dp.toPx())
                            close()
                        }, ink)
                    }
                    ChanceGame.PLINKO -> drawPlinkoRound(draws, turns, elapsed, position, types, colors, ink)
                    ChanceGame.CLAW_MACHINE -> {
                        val pickup = turns.lastOrNull()?.claw
                        val scene = pickup?.at(progress) ?: requireNotNull(initialClaw)
                        drawClawRound(if (!busy) scene.copy(claw = clawHome(position)) else scene, colors, ink)
                    }
                    ChanceGame.SLOT_MACHINE -> error("Slot machine uses its reel board")
                }
            }
            if (game == ChanceGame.SPIN_THE_WHEEL) {
                BoxWithConstraints(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    var start = -90f
                    types.forEach { type ->
                        val sweep = pool.reelChance(type).toFloat() * 360f
                        val angle = (start + sweep / 2 + wheelAngle) * PI / 180
                        val rotation = start + sweep / 2 + wheelAngle + 90f
                        val layout = wheelSliceLayout(sweep)
                        val radius = minOf(maxWidth, maxHeight) * 0.46f
                        RewardImage(type, fragment = fragment, showFragmentBadge = false, modifier = Modifier
                            .offset(x = radius * layout.distance * cos(angle).toFloat(),
                                y = radius * layout.distance * sin(angle).toFloat())
                            .size(radius * layout.imageSize)
                            .graphicsLayer { rotationZ = rotation }
                            .testTag("chance-wheel-image-${type.name}"))
                        start += sweep
                    }
                }
            }
            if (game == ChanceGame.CLAW_MACHINE) {
                Text("Prize bin", Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall)
            }
            if (winner != null && awarded) {
                RewardImage(winner, fragment = fragment,
                    modifier = Modifier.size(144.dp).testTag("chance-revealed-reward"))
            }
        }
        Text(when (game) {
            ChanceGame.SPIN_THE_WHEEL -> "Spins completed: ${completed.size}/${game.drawCount}"
            ChanceGame.PLINKO -> "Balls landed: ${completed.size}/${game.drawCount}"
            else -> "Balls in prize bin: ${completed.size}/${game.drawCount}"
        }, Modifier.testTag("chance-progress").semantics { liveRegion = LiveRegionMode.Polite })
        ChanceGameTally(types, completed, colors, fragment, if (game == ChanceGame.PLINKO) "plinko" else "tally")
        if (game == ChanceGame.PLINKO || game == ChanceGame.CLAW_MACHINE) {
            Text(if (game == ChanceGame.PLINKO) "Move the ball before each drop" else "Position the claw above a capsule")
            Slider(position, onValueChange = onPosition,
                valueRange = 0.08f..0.92f, enabled = enabled,
                modifier = Modifier.testTag("chance-position").semantics {
                    contentDescription = if (game == ChanceGame.PLINKO) "Drop position" else "Claw position"
                })
        }
        Button(onClick = onPlay, enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag("chance-play")) {
            Text(when {
                busy -> when (game) {
                    ChanceGame.SPIN_THE_WHEEL -> "Spinning..."
                    ChanceGame.PLINKO -> "Ball dropping..."
                    else -> "Picking up..."
                }
                awarded && enabled -> "Start new round"
                completed.size == game.drawCount -> "Round complete"
                else -> "${game.action} (${completed.size + 1}/${game.drawCount})"
            })
        }
    }
}

@Composable
private fun ChanceGameTally(types: List<RewardType>, completed: List<RewardType>, colors: List<Color>,
                            fragment: Boolean, tagPrefix: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        types.forEachIndexed { index, type ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                RewardImage(type, fragment = fragment,
                    modifier = Modifier.size(56.dp).testTag("chance-$tagPrefix-image-${type.name}"))
                Text("${completed.count { it == type }}", color = colors[index % colors.size],
                    modifier = Modifier.testTag("chance-count-${type.name}").semantics {
                        contentDescription = "${type.label}: ${completed.count { it == type }}"
                    })
            }
        }
    }
}

private fun DrawScope.drawPlinkoRound(draws: List<RewardType>, turns: List<ChanceTurn>, elapsed: Int, position: Float,
                                     types: List<RewardType>, colors: List<Color>, ink: Color) {
    val w = size.width
    val game = ChanceGame.PLINKO
    val radius = w * plinkoBallRadius(types.size)
    plinkoPegs.forEach { peg ->
        drawCircle(ink.copy(alpha = 0.15f), w * PLINKO_PEG_RADIUS * 1.4f, peg * w + Offset(1.dp.toPx(), 2.dp.toPx()))
        drawCircle(ink, w * PLINKO_PEG_RADIUS, peg * w)
        drawCircle(Color.White.copy(alpha = 0.7f), w * PLINKO_PEG_RADIUS * 0.35f,
            peg * w - Offset(w * 0.002f, w * 0.002f))
    }
    types.forEachIndexed { index, _ ->
        val left = w * index / types.size
        val right = w * (index + 1) / types.size
        drawRect(colors[index % colors.size].copy(alpha = 0.16f), Offset(left, w * PLINKO_BIN_TOP),
            Size(right - left, w * (PLINKO_FLOOR - PLINKO_BIN_TOP)))
        drawPath(Path().apply {
            moveTo(left, w * PLINKO_BIN_TOP)
            lineTo(left, w * PLINKO_FLOOR)
            lineTo(right, w * PLINKO_FLOOR)
            lineTo(right, w * PLINKO_BIN_TOP)
        }, ink, style = Stroke(w * 0.006f))
    }
    drawLine(ink, Offset(w * 0.01f, 0f), Offset(w * 0.01f, w * PLINKO_FLOOR), w * 0.006f)
    drawLine(ink, Offset(w * 0.99f, 0f), Offset(w * 0.99f, w * PLINKO_FLOOR), w * 0.006f)
    turns.forEachIndexed { index, turn ->
        val point = requireNotNull(turn.plinko).at(game.drawProgress(index, elapsed))
        drawChanceBall(point * w, radius, colors[types.indexOf(draws[index]) % colors.size])
    }
    if (turns.size < game.drawCount && (turns.isEmpty() || game.drawProgress(turns.lastIndex, elapsed) >= 1f)) {
        drawChanceBall(Offset(w * position, w * 0.035f), radius, ink)
    }
}

private fun DrawScope.drawChanceBall(point: Offset, radius: Float, color: Color) {
    drawCircle(Brush.radialGradient(listOf(Color.White, color, color.copy(alpha = 0.8f)),
        center = point - Offset(radius * 0.35f, radius * 0.4f), radius = radius * 1.7f), radius, point)
    drawCircle(Color.Black.copy(alpha = 0.3f), radius, point, style = Stroke(radius * 0.08f))
}

private fun DrawScope.drawClawRound(scene: ClawScene, colors: List<Color>, ink: Color) {
    val w = size.width
    drawRoundRect(Brush.verticalGradient(listOf(colors.first().copy(alpha = 0.12f), Color.White.copy(alpha = 0.08f))),
        Offset(w * 0.025f, w * 0.025f), Size(w * 0.96f, w * 0.91f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.025f))
    drawRoundRect(ink, Offset(w * 0.025f, w * 0.025f), Size(w * 0.96f, w * 0.91f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.025f), style = Stroke(w * 0.012f))
    drawLine(ink, Offset(w * 0.05f, w * 0.065f), Offset(w * 0.96f, w * 0.065f), w * 0.012f)
    drawRect(ink.copy(alpha = 0.15f), Offset(w * 0.77f, w * 0.37f), Size(w * 0.205f, w * 0.53f))
    drawPath(Path().apply {
        moveTo(w * 0.735f, w * 0.37f)
        lineTo(w * 0.735f, w * CLAW_FLOOR)
        lineTo(w * 0.975f, w * CLAW_FLOOR)
    }, ink, style = Stroke(w * 0.012f))
    drawLine(ink, Offset(w * 0.045f, w * CLAW_FLOOR), Offset(w * 0.71f, w * CLAW_FLOOR), w * 0.012f)
    scene.balls.forEach { ball ->
        val center = ball.center * w
        val radius = w * CLAW_BALL_RADIUS
        drawChanceBall(center, radius, colors[ball.id % colors.size])
        drawArc(Color.White.copy(alpha = 0.65f), 5f, 170f, false,
            center - Offset(radius, radius * 0.35f), Size(radius * 2, radius * 0.7f),
            style = Stroke(w * 0.003f))
    }
    val x = scene.claw.x * w
    val y = scene.claw.y * w
    drawRoundRect(ink, Offset(x - w * 0.035f, w * 0.045f), Size(w * 0.07f, w * 0.04f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.01f))
    drawLine(ink, Offset(x, w * 0.085f), Offset(x, y), w * 0.008f)
    drawCircle(ink, w * 0.022f, Offset(x, y))
    clawFingers(scene.claw, scene.opening).forEach { finger ->
        drawPath(Path().apply {
            moveTo(finger.first().x * w, finger.first().y * w)
            finger.drop(1).forEach { point -> lineTo(point.x * w, point.y * w) }
        }, ink, style = Stroke(w * 0.012f, cap = StrokeCap.Round))
        drawCircle(colors[1], w * 0.009f, finger[1] * w)
    }
    drawLine(Color.White.copy(alpha = 0.15f), Offset(w * 0.05f, w * 0.13f),
        Offset(w * 0.22f, w * 0.48f), w * 0.028f, cap = StrokeCap.Round)
}
