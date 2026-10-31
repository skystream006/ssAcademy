package com.ssmath.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal const val REWARD_OPEN_DURATION_MS = 1_600

internal fun rewardImageDescription(type: RewardType, fragment: Boolean): String {
    if (!fragment) return when (type) {
        RewardType.TOYS_1 -> "\$1 Toys: a colorful spinning top"
        RewardType.TOYS_2 -> "\$2 Toys: a colorful toy train"
        RewardType.TOYS_3 -> "\$3 Toys: a friendly toy robot"
        RewardType.GOOD_BEHAVIOR_POINT -> "1 Good Behavior Point: a sparkling gold medal"
        else -> type.label
    }
    return when (type) {
        RewardType.LOLLIPOP -> "One third of a lollipop"
        RewardType.ICE_CREAM -> "One third of an ice cream cone"
        RewardType.GUMMI_BEAR -> "One third of a gummi bear"
        RewardType.RAMEN -> "One third of a square of dried ramen"
        RewardType.VIDEO_GAME -> "One third of a video game controller"
        RewardType.BED_TIME -> "One third of a bedtime scene"
        RewardType.RESTAURANT -> "One third of a restaurant"
        RewardType.TOYS_1 -> "One third of a \$1 toy spinning top"
        RewardType.TOYS_2 -> "One third of a \$2 toy train"
        RewardType.TOYS_3 -> "One third of a \$3 toy robot"
        RewardType.GOOD_BEHAVIOR_POINT -> "One third of a good behavior point medal"
    }
}

@Composable
internal fun RewardImage(
    type: RewardType,
    fragment: Boolean,
    modifier: Modifier = Modifier,
    showFragmentBadge: Boolean = true
) {
    val animated = when (type) {
        RewardType.BED_TIME, RewardType.RESTAURANT,
        RewardType.TOYS_1, RewardType.TOYS_2, RewardType.TOYS_3, RewardType.GOOD_BEHAVIOR_POINT -> true
        else -> false
    }
    val animation = if (animated) {
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            rememberInfiniteTransition(label = "reward-artwork").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing)),
                label = "${type.name} motion"
            )
        } else null
    } else null
    Box(modifier.size(144.dp).semantics(mergeDescendants = true) {
        contentDescription = rewardImageDescription(type, fragment)
        role = Role.Image
    }) {
        Canvas(Modifier.matchParentSize()) { drawRewardArtwork(type, fragment, animation?.value ?: 0f) }
        if (fragment && showFragmentBadge) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF51417B),
                contentColor = Color.White
            ) {
                Text("1/3", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
internal fun RewardGiftDialog(
    result: PracticeResult,
    claiming: Boolean,
    error: String?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) = RewardGiftDialog(result.id, result.percentCorrectText, result.prize, claiming, error, onOpen, onDismiss)

@Composable
internal fun RewardGiftDialog(
    result: ReadingRewardResult,
    claiming: Boolean,
    error: String?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) = RewardGiftDialog(result.sessionId, result.percentCorrectText, result.prize, claiming, error, onOpen, onDismiss)

@Composable
private fun RewardGiftDialog(
    resultKey: Any,
    percentCorrectText: String,
    prize: PrizeAward?,
    claiming: Boolean,
    error: String?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val hop = remember(resultKey) { Animatable(0f) }
    val opening = remember(resultKey) { Animatable(if (prize == null) 0f else 1f) }
    var requested by remember(resultKey) { mutableStateOf(false) }
    val canOpen = prize == null && !claiming && !requested
    val requestOpen = {
        if (canOpen && !requested) {
            requested = true
            onOpen()
        }
    }

    LaunchedEffect(resultKey, requested, claiming, error) {
        // A retry can fail before a frame observes claiming=true or a changed error.
        if (requested && !claiming && error != null) requested = false
    }
    LaunchedEffect(resultKey, lifecycle, prize != null, claiming, requested) {
        hop.snapTo(0f)
        if (prize == null && !claiming && !requested) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    hop.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
                    hop.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
                    delay(420)
                }
            }
        }
    }
    LaunchedEffect(resultKey, lifecycle, prize != null) {
        if (prize != null && opening.value < 1f) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val remaining = ((1f - opening.value) * REWARD_OPEN_DURATION_MS).toInt().coerceAtLeast(1)
                opening.animateTo(1f, tween(remaining, easing = LinearEasing))
            }
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text(if (prize == null) "You earned a gift!" else "A fragment for you!") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()).testTag("reward-gift-content"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (prize == null) {
                    Text("Congratulations! You've answered $percentCorrectText% correct! Tap to get a prize!")
                }
                val interaction = if (prize == null) {
                    Modifier.clickable(enabled = canOpen, role = Role.Button, onClick = requestOpen)
                        .semantics {
                            contentDescription = "Open gift box"
                            stateDescription = if (claiming || requested) "Saving prize" else "Ready to open"
                        }
                } else {
                    Modifier.semantics {
                        contentDescription = "Opened gift box"
                        role = Role.Image
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp))
                        .testTag("gift-box").then(interaction),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRewardGift(opening.value, hop.value)
                    }
                    if (prize != null && opening.value >= 0.5f) {
                        RewardImage(prize.type, fragment = true,
                            modifier = Modifier.size(160.dp).testTag("awarded-fragment"))
                    }
                }
                if (prize != null) {
                    Column(
                        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(prize.type.fragmentLabel, style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center)
                        Text("Added 1 fragment to your collection.", textAlign = TextAlign.Center)
                        Text("Whole: ${prize.balance.whole} · Fragments: ${prize.balance.fragments}/3",
                            textAlign = TextAlign.Center, modifier = Modifier.testTag("awarded-balance"))
                        Text("3 fragments = 1 reward", style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center)
                    }
                } else if (claiming || requested) {
                    Text("Saving your prize…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (error != null && prize == null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        },
        confirmButton = {
            if (prize == null && error != null) {
                TextButton(onClick = requestOpen, enabled = canOpen) { Text("Retry") }
            } else {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("view-results")) {
                    Text(if (prize == null) "Not now" else "View results")
                }
            }
        },
        dismissButton = {
            if (prize == null && error != null) {
                TextButton(onClick = onDismiss) { Text("Not now") }
            }
        }
    )
}

@Composable
internal fun RewardInventory(balances: Map<RewardType, RewardBalance>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth().testTag("reward-inventory"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.fillMaxWidth().testTag("reward-inventory-header"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("3 fragments = 1 reward", style = MaterialTheme.typography.titleMedium)
                Text("Collect three matching fragments to complete a reward. Your collection stays on this device.")
            }
        }
        rewardTierItems { type ->
            val balance = balances[type] ?: RewardBalance()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().testTag("reward-card-${type.name}")
            ) {
                Column(
                    Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(type.pluralLabel, style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center)
                    RewardInventoryPart(type, false, "Whole: ${balance.whole}",
                        Modifier.fillMaxWidth().testTag("inventory-whole-${type.name}"))
                    RewardInventoryPart(type, true, "Fragments: ${balance.fragments}/3",
                        Modifier.fillMaxWidth().testTag("inventory-fragments-${type.name}"))
                }
            }
        }
    }
}

@Composable
internal fun UseRewardsDialog(
    balances: Map<RewardType, RewardBalance>,
    usingReward: Boolean,
    error: String?,
    onUse: (RewardType) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf<RewardType?>(null) }
    val choice = selected
    AlertDialog(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { if (selected != null) selected = null else onDismiss() },
        title = { Text(if (choice == null) "What reward would you like to use?" else "Use reward?") },
        text = {
            if (choice != null) {
                val rewardLabel = if (choice == RewardType.GOOD_BEHAVIOR_POINT) choice.label else "1 ${choice.label}"
                Text("You would like to use $rewardLabel?")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (usingReward) {
                        Text("Saving your reward…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    } else if (balances.values.none { it.whole > 0 }) {
                        Text("No whole rewards available yet.")
                    }
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("use-rewards-grid"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rewardTierItems { type ->
                            val whole = balances[type]?.whole ?: 0
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth().testTag("use-reward-${type.name}")
                                    .clickable(enabled = whole > 0 && !usingReward, role = Role.Button) {
                                        selected = type
                                    }
                                    .semantics { stateDescription = "$whole available" }
                            ) {
                                Column(
                                    Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(type.label, style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center)
                                    RewardInventoryPart(type, false, "Whole: $whole", Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (choice != null) {
                TextButton(
                    enabled = !usingReward && (balances[choice]?.whole ?: 0) > 0,
                    onClick = {
                        if (selected == choice) {
                            selected = null
                            onUse(choice)
                        }
                    }
                ) { Text("Yes") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        dismissButton = {
            if (choice != null) {
                TextButton(onClick = { selected = null }) { Text("No") }
            }
        }
    )
}

private fun LazyGridScope.rewardTierItems(
    content: @Composable LazyGridItemScope.(RewardType) -> Unit) {
    RewardTier.entries.forEach { tier ->
        item(key = tier.name, span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(tier.label, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            }
        }
        items(RewardType.entries.filter { it.tier == tier }, key = { it.name }, itemContent = content)
    }
}

@Composable
private fun RewardInventoryPart(type: RewardType, fragment: Boolean, count: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (type) {
                RewardType.LOLLIPOP -> Color(0xFFFFE4EE)
                RewardType.ICE_CREAM -> Color(0xFFFFEBDC)
                RewardType.GUMMI_BEAR -> Color(0xFFDDF5EA)
                RewardType.RAMEN -> Color(0xFFFFF1D2)
                RewardType.VIDEO_GAME -> Color(0xFFEAE5FF)
                RewardType.BED_TIME -> Color(0xFFE3E6FF)
                RewardType.RESTAURANT -> Color(0xFFFFE5D7)
                RewardType.TOYS_1 -> Color(0xFFFFF1D2)
                RewardType.TOYS_2 -> Color(0xFFDDF4F5)
                RewardType.TOYS_3 -> Color(0xFFF0E6FF)
                RewardType.GOOD_BEHAVIOR_POINT -> Color(0xFFE0F5E9)
            }
        ) {
            RewardImage(type, fragment, Modifier.fillMaxWidth().aspectRatio(1f).padding(6.dp),
                showFragmentBadge = false)
        }
        Text(count, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    }
}
