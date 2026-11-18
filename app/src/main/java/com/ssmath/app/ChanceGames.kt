package com.ssmath.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    SPIN_THE_WHEEL("Spin the wheel", "spin-the-wheel", "Spin 5 times"),
    PLINKO("Plinko Drop", "plinko", "Drop 10 balls", animationDurationMs = 4_800, drawCount = 10),
    CLAW_MACHINE("Claw Machine", "claw-machine", "Pick up 5 balls");

    val drawIntervalMs: Int get() = if (this == PLINKO) 500 else animationDurationMs + 400
    val playDurationMs: Int get() = (drawCount - 1) * drawIntervalMs + animationDurationMs + 400
    val outcomeUnit: String get() = if (this == SPIN_THE_WHEEL) "spin" else "ball"

    fun drawProgress(index: Int, elapsedMs: Int): Float =
        ((elapsedMs - index * drawIntervalMs).toFloat() / animationDurationMs).coerceIn(0f, 1f)
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
                            enabled = !busy && model.slotMachineError == null,
                            modifier = Modifier.testTag("chance-fragment-reward").semantics {
                                contentDescription = "Fragment reward"
                            })
                    }
                }
                Text(if (game == ChanceGame.SPIN_THE_WHEEL)
                    "Spin 5 times. The reward received most often wins one prize."
                    else if (game == ChanceGame.PLINKO)
                        "Drop 10 balls. The bin with the most balls wins one prize."
                    else "Pick up 5 balls and place them in the prize bin. The most common ball wins one prize.")
                Text("Ties are decided randomly among the tied leaders. Position does not change the chances.")
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
                        ChanceGameBoard(game, pool, roll, fragment, enabled, model::spinSlotMachine)
                    }
                    Text(when {
                        busy -> "Revealing / saving..."
                        model.slotMachineError != null -> requireNotNull(model.slotMachineError)
                        model.slotMachineAwarded -> "You won ${
                            requireNotNull(roll?.winner).let { if (fragment) it.fragmentLabel else it.label }
                        }! ${if (fragment) "One fragment" else "One complete reward"} added."
                        else -> "Ready to play!"
                    }, Modifier.testTag("chance-status").semantics { liveRegion = LiveRegionMode.Polite })
                    if (model.slotMachineError != null && roll?.winner != null) {
                        Button(onClick = model::retrySlotMachinePrize, enabled = !busy,
                            modifier = Modifier.testTag("chance-retry")) { Text("Retry saving reward") }
                    }
                    ChanceGameOdds(pool, fragment, game)
                    if (!activity) {
                        TextButton(onClick = { model.chooseSlotMachineTier(null) }, enabled = !busy) {
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
    onPlay: () -> Unit
) {
    var position by rememberSaveable(game) { mutableFloatStateOf(0.5f) }
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onSurfaceVariant)
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val draws = roll?.chanceDraws.orEmpty()
    val elapsed = roll?.chanceElapsedMs ?: 0
    val completed = draws.filterIndexed { index, _ -> game.drawProgress(index, elapsed) >= 1f }
    val activeIndex = (elapsed / game.drawIntervalMs).coerceAtMost(game.drawCount - 1)
    val p = FastOutSlowInEasing.transform(game.drawProgress(activeIndex, elapsed))
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
    val finished = draws.isNotEmpty() && completed.size == game.drawCount
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(surface)
            .testTag("chance-board-${game.name}"), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(12.dp).semantics { contentDescription = game.label }) {
                val w = size.width
                val h = size.height
                when (game) {
                    ChanceGame.SPIN_THE_WHEEL -> {
                        val radius = minOf(w, h) * 0.43f
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
                    ChanceGame.PLINKO -> drawPlinkoRound(draws, elapsed, position, types, colors, ink)
                    ChanceGame.CLAW_MACHINE -> drawClawRound(draws, elapsed, position, types, colors, ink)
                    ChanceGame.SLOT_MACHINE -> error("Slot machine uses its reel board")
                }
            }
            if (game == ChanceGame.SPIN_THE_WHEEL) {
                BoxWithConstraints(Modifier.matchParentSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    var start = -90f
                    types.forEach { type ->
                        val sweep = pool.reelChance(type).toFloat() * 360f
                        val angle = (start + sweep / 2 + wheelAngle) * PI / 180
                        val radius = minOf(maxWidth, maxHeight) * 0.29f
                        RewardImage(type, fragment = fragment, modifier = Modifier
                            .offset(x = radius * cos(angle).toFloat(), y = radius * sin(angle).toFloat())
                            .size(56.dp).testTag("chance-wheel-image-${type.name}"))
                        start += sweep
                    }
                }
            }
            if (game == ChanceGame.CLAW_MACHINE) {
                Text("Prize bin", Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
            if (game == ChanceGame.PLINKO) {
                ChanceGameTally(types, completed, colors, fragment, "plinko",
                    Modifier.align(Alignment.BottomCenter))
            }
            if (winner != null && finished) {
                RewardImage(winner, fragment = fragment,
                    modifier = Modifier.size(144.dp).testTag("chance-revealed-reward"))
            }
        }
        Text(when (game) {
            ChanceGame.SPIN_THE_WHEEL -> "Spins completed: ${completed.size}/${game.drawCount}"
            ChanceGame.PLINKO -> "Balls landed: ${completed.size}/${game.drawCount}"
            else -> "Balls in prize bin: ${completed.size}/${game.drawCount}"
        }, Modifier.testTag("chance-progress").semantics { liveRegion = LiveRegionMode.Polite })
        if (game != ChanceGame.PLINKO) ChanceGameTally(types, completed, colors, fragment, "tally")
        if (game == ChanceGame.PLINKO || game == ChanceGame.CLAW_MACHINE) {
            Text(if (game == ChanceGame.PLINKO) "Choose a drop position" else "Position the claw")
            Slider(position, onValueChange = { position = it },
                valueRange = 0.08f..0.92f, enabled = enabled,
                modifier = Modifier.testTag("chance-position").semantics {
                    contentDescription = if (game == ChanceGame.PLINKO) "Drop position" else "Claw position"
                })
        }
        Button(onClick = onPlay, enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag("chance-play")) { Text(game.action) }
    }
}

@Composable
private fun ChanceGameTally(types: List<RewardType>, completed: List<RewardType>, colors: List<Color>,
                            fragment: Boolean, tagPrefix: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        types.forEachIndexed { index, type ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

private fun DrawScope.drawPlinkoRound(draws: List<RewardType>, elapsed: Int, position: Float,
                                     types: List<RewardType>, colors: List<Color>, ink: Color) {
    val w = size.width
    val h = size.height
    val game = ChanceGame.PLINKO
    repeat(6) { row ->
        repeat(7) { col ->
            drawCircle(ink, 3.dp.toPx(),
                Offset(w * (col + 0.5f + (row % 2) * 0.35f) / 7.5f, h * (0.08f + row * 0.075f)))
        }
    }
    types.forEachIndexed { index, _ ->
        drawRect(colors[index % colors.size].copy(alpha = 0.15f), Offset(w * index / types.size, h * 0.5f),
            Size(w / types.size - 2.dp.toPx(), h * 0.49f))
        drawRect(ink, Offset(w * index / types.size, h * 0.5f),
            Size(w / types.size - 2.dp.toPx(), h * 0.49f), style = Stroke(2.dp.toPx()))
    }
    draws.forEachIndexed { index, type ->
        if (elapsed < index * game.drawIntervalMs) return@forEachIndexed
        val p = FastOutSlowInEasing.transform(game.drawProgress(index, elapsed))
        val bin = types.indexOf(type)
        val slot = draws.take(index).count { it == type }
        val endX = (bin + 0.5f) / types.size + (slot % 3 - 1) * 0.035f
        val endY = 0.64f - (slot / 3) * 0.04f
        val x = position * (1 - p) + endX * p +
            sin(p * PI * 12).toFloat() * 0.035f * sin(p * PI).toFloat()
        drawCircle(colors[bin % colors.size], 5.dp.toPx(), Offset(w * x, h * (0.04f * (1 - p) + endY * p)))
    }
    if (draws.isEmpty()) drawCircle(colors.first(), 5.dp.toPx(), Offset(w * position, h * 0.04f))
}

private fun DrawScope.drawClawRound(draws: List<RewardType>, elapsed: Int, position: Float,
                                   types: List<RewardType>, colors: List<Color>, ink: Color) {
    val w = size.width
    val h = size.height
    val game = ChanceGame.CLAW_MACHINE
    val index = (elapsed / game.drawIntervalMs).coerceAtMost(game.drawCount - 1)
    val p = FastOutSlowInEasing.transform(game.drawProgress(index, elapsed))
    val pickX = w * (0.1f + position * 0.5f)
    val binX = w * (0.77f + (index % 3) * 0.08f)
    val binY = h * (0.82f - (index / 3) * 0.09f)
    drawRect(ink, Offset(w * 0.7f, h * 0.4f), Size(w * 0.29f, h * 0.5f), style = Stroke(3.dp.toPx()))
    repeat(7) { ball ->
        drawCircle(colors[ball % types.size % colors.size], 6.dp.toPx(),
            Offset(w * (0.08f + ball * 0.08f), h * 0.85f))
    }
    draws.forEachIndexed { drawIndex, type ->
        if (game.drawProgress(drawIndex, elapsed) >= 1f) {
            drawCircle(colors[types.indexOf(type) % colors.size], 8.dp.toPx(),
                Offset(w * (0.77f + (drawIndex % 3) * 0.08f), h * (0.82f - (drawIndex / 3) * 0.09f)))
        }
    }
    val x = when {
        p < 0.5f -> pickX
        p < 0.75f -> pickX + (binX - pickX) * (p - 0.5f) / 0.25f
        p < 0.9f -> binX
        else -> binX + (pickX - binX) * (p - 0.9f) / 0.1f
    }
    val y = h * when {
        p < 0.3f -> 0.1f + p / 0.3f * 0.5f
        p < 0.5f -> 0.6f - (p - 0.3f) / 0.2f * 0.5f
        else -> 0.1f
    }
    drawLine(ink, Offset(x, 0f), Offset(x, y), 3.dp.toPx())
    val opening = if (p < 0.3f || p >= 0.75f) 0.06f else 0.025f
    drawLine(ink, Offset(x, y), Offset(x - w * opening, y + h * 0.1f), 4.dp.toPx())
    drawLine(ink, Offset(x, y), Offset(x + w * opening, y + h * 0.1f), 4.dp.toPx())
    val type = draws.getOrNull(index)
    if (type != null && game.drawProgress(index, elapsed) < 1f) {
        val ball = when {
            p < 0.3f -> Offset(pickX, h * 0.7f)
            p < 0.75f -> Offset(x, y + h * 0.1f)
            p < 0.9f -> Offset(binX, h * 0.2f + (binY - h * 0.2f) * (p - 0.75f) / 0.15f)
            else -> Offset(binX, binY)
        }
        drawCircle(colors[types.indexOf(type) % colors.size], 8.dp.toPx(), ball)
    }
}
