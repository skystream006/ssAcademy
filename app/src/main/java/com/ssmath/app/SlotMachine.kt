package com.ssmath.app

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.floor
import kotlin.math.sin

internal const val SLOT_MACHINE_DURATION_MS = 2_600

internal data class SlotMachineRoll(
    val reels: List<RewardType>,
    val rollNumber: Long = 0,
    val fragmentReward: Boolean = false
) {
    init {
        require(reels.size == 3)
    }

    // Cache bonuses without replacing the roll or restarting its reels on a save retry.
    internal var celebrationRewards by mutableStateOf<List<Celebration>?>(null)

    val winner: RewardType? get() = reels.first().takeIf { type -> reels.all { it == type } }
}

@Composable
internal fun SlotMachineDialog(model: MathViewModel, onDismiss: () -> Unit) {
    if (model.chanceGame != ChanceGame.SLOT_MACHINE) {
        ChanceGameDialog(model, onDismiss)
        return
    }
    val tier = model.slotMachineSelectedTier
    val pool = model.slotMachinePool
    val roll = model.slotMachineRoll
    val busy = model.slotMachineBusy
    val activityReward = model.slotMachineActivityReward
    val fragmentReward = model.slotMachineIsFragmentReward
    var celebrationDismissed by rememberSaveable(roll) { mutableStateOf(false) }
    var bonusIndex by rememberSaveable(roll) { mutableIntStateOf(0) }
    SlotMachineRollingSound(roll, model.slotMachineSpinning)
    SettingsDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (tier == null) "Slot machine: select a tier" else "Slot machine - ${tier.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (activityReward) {
                    Text("Reward: Fragment")
                    Text("You earned a reward! Pull for free until you win one fragment. No whole rewards are awarded.")
                    val saveError = if (model.rewardDialogVisible) model.rewardError else model.readingRewardError
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (fragmentReward) "Reward: Fragment" else "Reward: Whole reward",
                            Modifier.weight(1f))
                        Switch(fragmentReward, model::chooseSlotMachineFragmentReward,
                            enabled = !busy && model.slotMachineError == null,
                            modifier = Modifier.testTag("slot-fragment-reward").semantics {
                                contentDescription = "Fragment reward"
                            })
                    }
                }
                Text(if (fragmentReward) "Match three fragments to win one fragment. Each pull is free."
                    else "Match three rewards to win one complete reward. Each pull is free.")
                Text("Every tenth spin in this session guarantees a random matching set.")
                if (tier == null) {
                    Text("Uses enabled rewards and your Tier 3 chance settings.")
                    RewardTier.entries.forEach { option ->
                        val available = availableRewardsForTier(option, model.disabledRewards, model.tier3RewardChances)
                        Button(onClick = { model.chooseSlotMachineTier(option) },
                            enabled = available.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().testTag("slot-tier-${option.name}")) {
                            Text(option.label)
                        }
                        if (available.isEmpty()) Text("${option.label}: no available rewards")
                    }
                    AdminSlotMachineBonuses(model.pokemons, tier)
                } else {
                    val types = pool?.weights?.keys?.toList().orEmpty()
                    if (types.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { index ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    SlotMachineReel(types, roll, index, fragmentReward, Modifier.fillMaxWidth())
                                    val type = roll?.reels?.get(index)
                                    Text(if (busy) "Spinning..." else if (fragmentReward) type?.fragmentLabel ?: "Fragment"
                                        else type?.label ?: "Reward",
                                        textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.testTag("slot-label-$index"))
                                }
                            }
                        }
                    }
                    val status = when {
                        busy -> "Spinning / saving..."
                        model.slotMachineError != null -> requireNotNull(model.slotMachineError)
                        model.slotMachineAwarded -> if (requireNotNull(roll).fragmentReward)
                            "You won ${requireNotNull(roll.winner).fragmentLabel}! One fragment added."
                            else "You won ${requireNotNull(roll.winner).label}! One complete reward added."
                        roll != null -> "No match. Pull again!"
                        else -> "Pull the lever to spin all three slots."
                    }
                    Text(status, Modifier.testTag("slot-status").semantics { liveRegion = LiveRegionMode.Polite })
                    if (model.slotMachineError != null && roll?.winner != null) {
                        Button(onClick = model::retrySlotMachinePrize, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().testTag("slot-retry")) { Text("Retry saving reward") }
                    }
                    Button(onClick = model::spinSlotMachine,
                        enabled = !busy && !model.removingReward && model.slotMachineError == null &&
                            pool != null && !(activityReward && model.slotMachineAwarded),
                        modifier = Modifier.fillMaxWidth().testTag("slot-lever")) {
                        val leverColor = MaterialTheme.colorScheme.onPrimary
                        Canvas(Modifier.size(36.dp)) {
                            val pivot = Offset(size.width * 0.5f, size.height * 0.85f)
                            val handle = Offset(size.width * (if (busy) 0.85f else 0.25f),
                                size.height * (if (busy) 0.65f else 0.15f))
                            drawLine(leverColor, pivot, handle, strokeWidth = 4.dp.toPx())
                            drawCircle(leverColor, radius = 6.dp.toPx(), center = handle)
                            drawCircle(leverColor, radius = 4.dp.toPx(), center = pivot)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "Lever pulled" else "Pull lever")
                    }
                    SlotMachineAvailableRewards(pool, tier, fragmentReward)
                    if (!activityReward) {
                        AdminSlotMachineBonuses(model.pokemons, tier)
                        TextButton(onClick = { model.chooseSlotMachineTier(null) }, enabled = !busy) {
                            Text("Change tier")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(if (!activityReward) "Back to Admin Menu" else if (model.slotMachineAwarded) "Done" else "Finish later")
            }
        },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        modifier = Modifier.testTag("slot-machine-dialog")
    )
    if (model.slotMachineAwarded && !busy && !celebrationDismissed) {
        roll?.takeIf { it.winner != null }?.let { winningRoll ->
            SlotMachineWinDialog(winningRoll, activityReward, onDismiss = {
                celebrationDismissed = true
                if (activityReward) onDismiss()
            })
        }
    }
    val bonuses = roll?.celebrationRewards.orEmpty()
    if (!activityReward && model.slotMachineAwarded && !busy && celebrationDismissed && bonusIndex < bonuses.size) {
        val index = bonusIndex
        CelebrationDialog(bonuses[index], replay = true, hasNextCelebration = index + 1 < bonuses.size,
            onFinished = { if (bonusIndex == index) bonusIndex++ })
    }
}

@Composable
internal fun AdminSlotMachineBonuses(collected: Set<Celebration>, tier: RewardTier?) {
    val standard = remember(collected) { availableCelebrations(Celebration.standardCelebrations, collected) }
    val mega = remember(collected) { availableCelebrations(Celebration.megaPokemons, collected) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bonus animations", style = MaterialTheme.typography.titleSmall)
        Text("Every Admin win also gives a Pokémon or Other animation. " +
            "${standard.size} available · ${slotChancePercent(1.0 / standard.size)}% each per win.",
            style = MaterialTheme.typography.bodySmall)
        if (tier == null || tier == RewardTier.TIER_3) {
            Text("Tier 3 wins also give one Mega Pokémon. " +
                "${mega.size} available · ${slotChancePercent(1.0 / mega.size)}% each per win.",
                style = MaterialTheme.typography.bodySmall)
        }
        Text("Uncollected animations are selected first; completed pools may repeat.",
            style = MaterialTheme.typography.bodySmall)
    }
}

internal fun slotChancePercent(chance: Double): String =
    if (chance > 0 && chance * 100 < 0.0001) "<0.0001"
    else String.format(Locale.ROOT, "%.4f", chance * 100).trimEnd('0').trimEnd('.')

@Composable
internal fun SlotMachineAvailableRewards(pool: SlotMachineRewardPool?, tier: RewardTier,
                                         fragmentReward: Boolean = true, tagPrefix: String = "slot") {
    Column(Modifier.fillMaxWidth().testTag("$tagPrefix-available-${tier.name}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Available ${if (fragmentReward) "fragments" else "rewards"} - ${tier.label}",
            style = MaterialTheme.typography.titleMedium)
        if (pool == null) {
            Text("${tier.label}: no available rewards")
        } else {
            Text("Every tenth spin guarantees a match, selected using the per-reel chances. " +
                "Other spins use three independent reels; the win odds and share of wins below apply only to those spins.",
                style = MaterialTheme.typography.bodySmall)
            pool.weights.keys.forEach { type ->
                Row(Modifier.fillMaxWidth().testTag("$tagPrefix-odds-${type.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RewardImage(type, fragment = fragmentReward, modifier = Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (fragmentReward) type.fragmentLabel else type.label)
                        Text("Per reel: ${slotChancePercent(pool.reelChance(type))}% · " +
                            "Win per pull: ${slotChancePercent(pool.winChance(type))}%",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Share of wins: ${slotChancePercent(pool.prizeChance(type))}%",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Win per pull: ${slotChancePercent(pool.winChance)}% · " +
                "No match: ${slotChancePercent(1 - pool.winChance)}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("$tagPrefix-total-odds-${tier.name}"))
        }
    }
}

@Composable
private fun SlotMachineRollingSound(roll: SlotMachineRoll?, spinning: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(roll, spinning, lifecycle) {
        if (roll == null || !spinning) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val sound = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 55)
            } catch (error: RuntimeException) {
                DebugLog.event(DebugEvent.SOUND_FAILURE, error = error)
                return@repeatOnLifecycle
            }
            try {
                while (isActive) {
                    if (!sound.startTone(ToneGenerator.TONE_PROP_BEEP, 45)) {
                        DebugLog.event(DebugEvent.SOUND_FAILURE)
                        break
                    }
                    delay(120)
                }
            } finally {
                sound.stopTone()
                sound.release()
            }
        }
    }
}

@Composable
internal fun SlotMachineWinDialog(
    roll: SlotMachineRoll,
    activityReward: Boolean,
    title: String = "Three matching items!",
    onDismiss: () -> Unit
) {
    val winner = requireNotNull(roll.winner)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pop = remember(roll) { Animatable(0f) }
    val burst = remember(roll) { Animatable(0f) }
    LaunchedEffect(roll, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f))
        }
    }
    LaunchedEffect(roll, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            burst.animateTo(1f, tween(((1f - burst.value) * 1_600).toInt().coerceAtLeast(1)))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val colors = listOf(MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize().testTag("slot-win-confetti")) {
                        repeat(24) { index ->
                            val progress = burst.value
                            val x = size.width / 2 + (index % 9 - 4) * size.width / 9 * progress
                            val y = size.height / 2 - sin(progress * Math.PI).toFloat() *
                                size.height * (0.3f + index % 4 * 0.1f) + progress * size.height * 0.4f
                            drawCircle(colors[index % colors.size], radius = (3 + index % 4).dp.toPx(),
                                center = Offset(x, y), alpha = 1f - progress)
                        }
                    }
                    RewardImage(winner, fragment = roll.fragmentReward,
                        modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f).testTag("slot-win-reward").graphicsLayer {
                            scaleX = 0.5f + pop.value * 0.5f
                            scaleY = 0.5f + pop.value * 0.5f
                            rotationZ = (1f - pop.value) * -12f
                        })
                }
                Text("You won ${if (roll.fragmentReward) winner.fragmentLabel else winner.label}!",
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(if (roll.fragmentReward) "One fragment added to My Rewards."
                    else "One complete reward added to My Rewards.", textAlign = TextAlign.Center)
                roll.celebrationRewards.orEmpty().forEach {
                    Text("Bonus: ${it.collectionLabel}", textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("slot-win-dismiss")) {
                Text(if (activityReward) "Done" else if (roll.celebrationRewards.isNullOrEmpty()) "Keep playing"
                    else if (requireNotNull(roll.celebrationRewards).size > 1) "View bonuses" else "View bonus")
            }
        },
        modifier = Modifier.testTag("slot-win-dialog")
    )
}

@Composable
private fun SlotMachineReel(
    types: List<RewardType>,
    roll: SlotMachineRoll?,
    index: Int,
    fragmentReward: Boolean,
    modifier: Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(roll) {
        progress.snapTo(0f)
        if (roll != null) {
            val target = types.size * (6 + index) + types.indexOf(roll.reels[index])
            progress.animateTo(target.toFloat(), tween(1_800 + index * 350, easing = FastOutSlowInEasing))
        }
    }
    BoxWithConstraints(modifier.aspectRatio(0.85f).clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).testTag("slot-reel-$index")) {
        val step = floor(progress.value).toInt()
        val fraction = progress.value - step
        for (offset in 0..1) {
            RewardImage(types[(step + offset) % types.size], fragment = fragmentReward,
                modifier = Modifier.fillMaxWidth().height(maxHeight)
                    .offset(y = maxHeight * (offset - fraction)).padding(8.dp))
        }
    }
}
