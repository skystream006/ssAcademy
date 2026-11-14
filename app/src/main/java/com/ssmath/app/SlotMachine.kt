package com.ssmath.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlin.math.floor

internal const val SLOT_MACHINE_DURATION_MS = 2_600

internal data class SlotMachineRoll(
    val reels: List<RewardType>,
    val rollNumber: Long = 0,
    val fragmentReward: Boolean = false
) {
    init {
        require(reels.size == 3 && reels.all { it.tier == reels.first().tier })
    }

    val winner: RewardType? get() = reels.first().takeIf { type -> reels.all { it == type } }
}

@Composable
internal fun SlotMachineDialog(model: MathViewModel, onDismiss: () -> Unit) {
    val tier = model.slotMachineTier
    val roll = model.slotMachineRoll
    val busy = model.slotMachineBusy
    val fragmentReward = model.slotMachineFragmentReward
    SettingsDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (tier == null) "Slot machine: select a tier" else "Slot machine - ${tier.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (fragmentReward) "Reward: Fragment" else "Reward: Whole reward",
                        Modifier.weight(1f))
                    Switch(fragmentReward, model::chooseSlotMachineFragmentReward,
                        enabled = !busy && model.slotMachineError == null,
                        modifier = Modifier.testTag("slot-fragment-reward").semantics {
                            contentDescription = "Fragment reward"
                        })
                }
                Text(if (fragmentReward) "Match three fragments to win one fragment. Each pull is free."
                    else "Match three rewards to win one complete reward. Each pull is free.")
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
                } else {
                    val types = remember(tier) { RewardType.entries.filter { it.tier == tier } }
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
                            availableRewardsForTier(tier, model.disabledRewards, model.tier3RewardChances).isNotEmpty(),
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
                    TextButton(onClick = { model.chooseSlotMachineTier(null) }, enabled = !busy) {
                        Text("Change tier")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Back to Admin Menu") }
        },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        modifier = Modifier.testTag("slot-machine-dialog")
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
