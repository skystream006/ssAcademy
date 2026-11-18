package com.ssmath.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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

internal enum class ChanceGame(val label: String, val tag: String, val action: String) {
    SLOT_MACHINE("Slot machine", "slot-machine", "Pull lever"),
    SPIN_THE_WHEEL("Spin the wheel", "spin-the-wheel", "Spin wheel"),
    TREASURE_CHESTS("Mystery Treasure Chests", "treasure-chests", "Open chest"),
    PLINKO("Plinko Drop", "plinko", "Drop ball"),
    SCRATCH_CARD("Scratch-and-Reveal Cards", "scratch-card", "Reveal card"),
    CLAW_MACHINE("Claw Machine", "claw-machine", "Grab capsule"),
    BALLOON_POP("Balloon Pop", "balloon-pop", "Pop balloon")
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
                Text("Every play reveals one reward. Choices and position do not change the prize chances.")
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
                    ChanceGameOdds(pool, fragment)
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
private fun ChanceGameOdds(pool: SlotMachineRewardPool?, fragment: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Available ${if (fragment) "fragments" else "rewards"}", style = MaterialTheme.typography.titleMedium)
        if (pool == null) Text("No available rewards")
        pool?.weights?.keys?.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardImage(type, fragment = fragment, modifier = Modifier.size(40.dp))
                Text("${if (fragment) type.fragmentLabel else type.label}: " +
                    "${slotChancePercent(pool.reelChance(type))}% per play",
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
    val progress = remember(roll) { Animatable(0f) }
    var position by rememberSaveable(game) { mutableFloatStateOf(0.5f) }
    var selected by rememberSaveable(game) { mutableIntStateOf(if (game == ChanceGame.CLAW_MACHINE) 3 else 1) }
    val scratches = remember(roll) { mutableStateListOf<Pair<Offset, Offset>>() }
    var scratched by remember(roll) { mutableFloatStateOf(0f) }
    LaunchedEffect(roll) {
        if (roll != null) progress.animateTo(1f, tween(2_200, easing = FastOutSlowInEasing))
    }
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary)
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val p = progress.value
    val winner = roll?.winner
    val types = pool.weights.keys.toList()
    val winnerIndex = types.indexOf(winner).coerceAtLeast(0)
    val before = types.take(winnerIndex).sumOf { pool.weights.getValue(it) }
    val midpoint = (before + pool.weights.getValue(types[winnerIndex]) / 2f) /
        pool.weights.values.sum() * 360f
    val wheelAngle = (1_800f + 360f - midpoint) * p
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(surface)
            .testTag("chance-board-${game.name}")
            .pointerInput(game, enabled, roll) {
                if (game == ChanceGame.SCRATCH_CARD && enabled) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        scratches += (change.position - amount) to change.position
                        scratched += amount.getDistance()
                        if (scratched >= size.width * 1.5f) onPlay()
                    }
                }
            }, contentAlignment = Alignment.Center) {
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
                    ChanceGame.TREASURE_CHESTS -> repeat(3) { index ->
                        val x = w * (index + 0.5f) / 3
                        val y = h * 0.55f
                        val bw = w * 0.26f
                        drawRoundRect(colors[index], Offset(x - bw / 2, y), Size(bw, h * 0.25f))
                        val lift = if (index == selected) p * h * 0.25f else 0f
                        drawRoundRect(colors[index], Offset(x - bw / 2, y - h * 0.1f - lift),
                            Size(bw, h * 0.1f), style = Stroke(5.dp.toPx()))
                        drawCircle(ink, 4.dp.toPx(), Offset(x, y + h * 0.08f))
                    }
                    ChanceGame.PLINKO -> {
                        repeat(6) { row ->
                            repeat(7) { col ->
                                drawCircle(ink, 3.dp.toPx(),
                                    Offset(w * (col + 0.5f + (row % 2) * 0.35f) / 7.5f, h * (row + 1) / 8))
                            }
                        }
                        repeat(types.size) { index ->
                            drawRect(colors[index % colors.size], Offset(w * index / types.size, h * 0.9f),
                                Size(w / types.size - 2.dp.toPx(), h * 0.1f))
                        }
                        val end = (types.indexOf(winner).coerceAtLeast(0) + 0.5f) / types.size
                        val x = position * (1 - p) + end * p + sin(p * PI * 12).toFloat() * 0.035f * sin(p * PI).toFloat()
                        drawCircle(colors[0], 9.dp.toPx(), Offset(w * x, h * (0.04f + p * 0.91f)))
                    }
                    ChanceGame.SCRATCH_CARD -> {
                        if (p < 1f) {
                            drawRoundRect(colors[1], size = size)
                            scratches.forEach { (from, to) ->
                                val inset = Offset(12.dp.toPx(), 12.dp.toPx())
                                drawLine(surface, from - inset, to - inset, strokeWidth = 24.dp.toPx())
                            }
                            repeat(9) { index ->
                                val y = h * (index + 0.5f) / 9
                                drawLine(surface, Offset(0f, y), Offset(w * p, y),
                                    strokeWidth = h / 10)
                            }
                        }
                    }
                    ChanceGame.CLAW_MACHINE -> {
                        repeat(7) { index ->
                            if (index != selected || p < 0.6f) drawCircle(colors[index % colors.size],
                                w * 0.055f, Offset(w * (index + 0.5f) / 7, h * 0.85f))
                        }
                        val drop = if (p < 0.6f) p / 0.6f else (1 - p) / 0.4f
                        val x = w * position
                        val y = h * (0.12f + drop * 0.6f)
                        drawLine(ink, Offset(x, 0f), Offset(x, y), 3.dp.toPx())
                        val opening = if (p < 0.5f) 0.1f else 0.04f
                        drawLine(ink, Offset(x, y), Offset(x - w * opening, y + h * 0.12f), 5.dp.toPx())
                        drawLine(ink, Offset(x, y), Offset(x + w * opening, y + h * 0.12f), 5.dp.toPx())
                        if (p >= 0.6f) drawCircle(colors[selected % colors.size], w * 0.055f,
                            Offset(x, y + h * 0.1f))
                    }
                    ChanceGame.BALLOON_POP -> repeat(3) { index ->
                        val x = w * (index + 0.5f) / 3
                        if (index != selected || p < 0.5f) {
                            val scale = if (index == selected) 1 + p * 0.5f else 1f
                            drawOval(colors[index], Offset(x - w * 0.11f * scale, h * 0.2f),
                                Size(w * 0.22f * scale, h * 0.4f * scale))
                            drawLine(ink, Offset(x, h * 0.6f), Offset(x, h * 0.9f), 2.dp.toPx())
                        } else {
                            repeat(12) { piece ->
                                val angle = piece * PI / 6
                                val distance = w * (p - 0.5f)
                                drawCircle(colors[index], 4.dp.toPx(),
                                    Offset(x + cos(angle).toFloat() * distance,
                                        h * 0.4f + sin(angle).toFloat() * distance), alpha = 2 * (1 - p))
                            }
                        }
                    }
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
                            .size(32.dp))
                        start += sweep
                    }
                }
            }
            if (game == ChanceGame.PLINKO) {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    types.forEach { type ->
                        RewardImage(type, fragment = fragment, modifier = Modifier.size(32.dp))
                    }
                }
            }
            if (winner != null && p >= 1f) {
                RewardImage(winner, fragment = fragment,
                    modifier = Modifier.size(88.dp).testTag("chance-revealed-reward"))
            }
        }
        when (game) {
            ChanceGame.TREASURE_CHESTS, ChanceGame.BALLOON_POP -> {
                Text(if (game == ChanceGame.TREASURE_CHESTS) "Choose a chest" else "Choose a balloon")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Button(onClick = { selected = index; onPlay() }, enabled = enabled,
                            modifier = Modifier.weight(1f).testTag("chance-choice-$index")) {
                            Text("${if (game == ChanceGame.TREASURE_CHESTS) "Chest" else "Pop"} ${index + 1}")
                        }
                    }
                }
            }
            ChanceGame.PLINKO, ChanceGame.CLAW_MACHINE -> {
                Text(if (game == ChanceGame.PLINKO) "Choose a drop position" else "Position the claw")
                Slider(position, onValueChange = { position = it; selected = (it * 7).toInt().coerceAtMost(6) },
                    valueRange = 0.08f..0.92f, enabled = enabled,
                    modifier = Modifier.testTag("chance-position").semantics {
                        contentDescription = if (game == ChanceGame.PLINKO) "Drop position" else "Claw position"
                    })
            }
            ChanceGame.SCRATCH_CARD -> {
                Text("Scratch across the card to reveal your reward, or use Reveal card.")
            }
            else -> Unit
        }
        if (game != ChanceGame.TREASURE_CHESTS && game != ChanceGame.BALLOON_POP) {
            Button(onClick = onPlay, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("chance-play")) { Text(game.action) }
        }
    }
}
