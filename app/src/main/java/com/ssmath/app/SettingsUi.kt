@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ssmath.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt

@Composable
private fun ScreenScaffold(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            windowInsets = WindowInsets(0))
        content()
    }
}

@Composable
fun SettingsScreen(model: MathViewModel) {
    var showingAdmin by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold("Settings", model::closeOverlay) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UpdateSettings()
            ListItem(headlineContent = { Text("My Rewards") },
                supportingContent = { Text("Your prizes and fragments") },
                leadingContent = { Icon(Icons.Rounded.CardGiftcard, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = model::openRewards).testTag("my-rewards"))
            ListItem(headlineContent = { Text("My Pokémons") },
                supportingContent = { Text("Your collected Pokémon and other celebrations") },
                leadingContent = { Icon(Icons.Rounded.Collections, null) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${model.pokemons.size}/${Celebration.entries.size}")
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = model::openPokemons).testTag("my-pokemons"))
            HorizontalDivider()
            ListItem(headlineContent = { Text("Practice History") },
                supportingContent = { Text("Review your previous results") },
                leadingContent = { Icon(Icons.Rounded.History, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = model::openHistory).testTag("practice-history"))
            ListItem(headlineContent = { Text("Rewards Game Setup") },
                supportingContent = { Text("Default number limits for each math type") },
                leadingContent = { Icon(Icons.Rounded.Settings, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = model::openRewardsSetup).testTag("rewards-setup"))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rewards system")
                    Text("Earn prize fragments in Rewards Game by completing 25 or more questions with over 90% correct",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(model.rewardsEnabled, model::chooseRewardsEnabled,
                    modifier = Modifier.semantics { contentDescription = "Rewards system" })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show timer")
                    Text("Display the elapsed time while practicing", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(model.showTimer, model::chooseShowTimer,
                    modifier = Modifier.semantics { contentDescription = "Show timer" })
            }
            if (model.rewardsEnabled && model.showTimer) {
                TimeLimitSetting(model.timeLimitMinutes, model::chooseTimeLimitMinutes)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show correct answers")
                    Text("Reveal correct answers after mistakes, including in results and history",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(model.showCorrectAnswers, model::chooseShowCorrectAnswers,
                    modifier = Modifier.semantics { contentDescription = "Show correct answers" })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Vertical equations")
                    Text("Stack numbers with ones, tens, and hundreds aligned in practice, results, and history",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(model.verticalEquations, model::chooseVerticalEquations,
                    modifier = Modifier.semantics { contentDescription = "Vertical equations" })
            }
            HorizontalDivider()
            CollapsibleSettingsSection("Appearance", defaultExpanded = false) {
                Column {
                    Text("Text size: ${model.textSizePercent}%")
                    Text("Adjust text throughout the app", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = model.textSizePercent.toFloat(),
                        onValueChange = { model.chooseTextSize(it.roundToInt()) },
                        valueRange = MIN_TEXT_SIZE_PERCENT.toFloat()..MAX_TEXT_SIZE_PERCENT.toFloat(),
                        steps = (MAX_TEXT_SIZE_PERCENT - MIN_TEXT_SIZE_PERCENT) / 10 - 1,
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Text size"
                            stateDescription = "${model.textSizePercent}%"
                        })
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Blue Wave", "Color theme").forEachIndexed { index, label ->
                        SegmentedButton(selected = model.waveAppearance == (index == 0),
                            onClick = { model.chooseWaveAppearance(index == 0) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(label) }
                    }
                }
                if (!model.waveAppearance) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        COLOR_THEMES.forEach { (id, color) ->
                            Box(Modifier.size(48.dp).semantics { contentDescription = "$id theme"; selected = model.theme == id }
                                .clickable { model.chooseTheme(id) }.padding(6.dp)
                                .border(if (model.theme == id) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .padding(5.dp).background(Color(color), CircleShape))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark appearance", Modifier.weight(1f))
                        Switch(isDarkTheme(model.theme, model.mode), model::chooseDark)
                    }
                }
                SkinSetting(model.skinsEnabled, model.skin, model::chooseSkin, model::chooseSkins)
            }
            HorizontalDivider()
            DebugLogSettings(onOpenAdmin = { showingAdmin = true })
            Text("ssMath ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 20.dp))
        }
    }
    if (showingAdmin) {
        AdminDialog(model, onDismiss = { showingAdmin = false })
    }
}

@Composable
private fun AdminDialog(model: MathViewModel, onDismiss: () -> Unit) {
    var showingRemoval by rememberSaveable { mutableStateOf(false) }
    if (showingRemoval) {
        RemoveRewardsDialog(
            model.rewardBalances, model.removingReward, model.rewardRemovalError,
            model::removeReward, { showingRemoval = false }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin", Modifier.fillMaxWidth()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("My Pokémons", style = MaterialTheme.typography.titleMedium)
                Text("Add every celebration to your collection without changing practice history or rewards.")
                Button(onClick = model::collectAllCelebrations,
                    enabled = model.pokemons.size < Celebration.entries.size) {
                    Text("Add all animations", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Text("${model.pokemons.size}/${Celebration.entries.size} animations collected")
                HorizontalDivider()
                Text("My Rewards", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showingRemoval = true }) {
                    Text("Remove rewards", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                HorizontalDivider()
                Text("Available rewards", style = MaterialTheme.typography.titleMedium)
                Text("Choose which rewards can be awarded when a Rewards Game finishes. Existing prizes and fragments stay available.")
                RewardTier.entries.forEach { tier ->
                    Text("${tier.label} · ${tier.questionCountLabel}", Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleSmall)
                    RewardType.entries.filter { it.tier == tier }.forEach { type ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(type.label, Modifier.weight(1f))
                            Switch(type !in model.disabledRewards,
                                onCheckedChange = { model.chooseRewardEnabled(type, it) },
                                modifier = Modifier.semantics { contentDescription = "${type.label} reward" })
                        }
                    }
                }
                Text("If all rewards in a tier are disabled, Rewards Games in that tier give no prize.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun RemoveRewardsDialog(
    balances: Map<RewardType, RewardBalance>,
    removingReward: Boolean,
    error: String?,
    onRemove: (RewardType, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf<RewardType?>(null) }
    var fragment by rememberSaveable { mutableStateOf(false) }
    val choice = selected
    val balance = balances[choice] ?: RewardBalance()
    AlertDialog(
        onDismissRequest = { if (choice != null) selected = null else onDismiss() },
        title = { Text(if (choice == null) "Remove rewards" else "Remove reward?") },
        text = {
            if (choice != null) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (fragment) "Permanently remove 1 ${choice.label} fragment?"
                        else "Permanently remove 1 whole ${choice.label} reward?")
                    Text("Practice history and My Pokémons will not change. This cannot be undone.")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (removingReward) {
                        Text("Removing reward…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Remove one fragment or whole reward at a time. Removing a fragment does not break a whole reward.")
                        if (balances.values.none { it.totalFragments > 0 }) {
                            Text("No rewards or fragments available.")
                        }
                        RewardTier.entries.forEach { tier ->
                            Text(tier.label, style = MaterialTheme.typography.titleMedium)
                            RewardType.entries.filter { it.tier == tier }.forEach { type ->
                                val available = balances[type] ?: RewardBalance()
                                Column(Modifier.fillMaxWidth().testTag("remove-reward-${type.name}"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(type.label, style = MaterialTheme.typography.titleSmall)
                                    Text("Whole: ${available.whole} · Fragments: ${available.fragments}/3")
                                    OutlinedButton(
                                        onClick = { selected = type; fragment = true },
                                        enabled = !removingReward && available.fragments > 0,
                                        modifier = Modifier.fillMaxWidth().semantics {
                                            contentDescription = "Remove 1 ${type.label} fragment"
                                        }
                                    ) { Text("Remove 1 fragment", textAlign = TextAlign.Center) }
                                    OutlinedButton(
                                        onClick = { selected = type; fragment = false },
                                        enabled = !removingReward && available.whole > 0,
                                        modifier = Modifier.fillMaxWidth().semantics {
                                            contentDescription = "Remove 1 whole ${type.label} reward"
                                        }
                                    ) { Text("Remove 1 whole reward", textAlign = TextAlign.Center) }
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
                    enabled = !removingReward && (if (fragment) balance.fragments else balance.whole) > 0,
                    onClick = {
                        if (selected == choice) {
                            selected = null
                            onRemove(choice, fragment)
                        }
                    }
                ) { Text("Remove") }
            } else {
                TextButton(onClick = onDismiss) { Text("Back to admin") }
            }
        },
        dismissButton = {
            if (choice != null) {
                TextButton(onClick = { selected = null }) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun TimeLimitSetting(minutes: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Time limit", style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("time-limit")) {
                Text(if (minutes == 0) "None" else "$minutes minutes")
                Icon(Icons.Rounded.ExpandMore, null)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                (0..60 step 5).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (option == 0) "None" else "$option minutes") },
                        onClick = { onChange(option); expanded = false },
                        modifier = Modifier.semantics { selected = minutes == option })
                }
            }
        }
        Text("Applies to the next Rewards Game. Time pauses in settings and in the background.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RewardsScreen(model: MathViewModel) {
    var usingRewards by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold("My Rewards", model::closeOverlay, actions = {
        TextButton(onClick = { usingRewards = true }) { Text("Use rewards") }
    }) {
        RewardInventory(model.rewardBalances)
    }
    if (usingRewards) {
        UseRewardsDialog(
            model.rewardBalances, model.usingReward, model.rewardUseError,
            model::useReward, { usingRewards = false }
        )
    }
}

@Composable
fun PokemonsScreen(model: MathViewModel) {
    var replay by rememberSaveable { mutableStateOf<Celebration?>(null) }
    val collected = Celebration.entries.filter { it in model.pokemons }
    val minimumCellWidth = maxOf(112.dp, with(LocalDensity.current) {
        MaterialTheme.typography.labelLarge.fontSize.toDp() * 8
    })
    BackHandler(onBack = model::closeOverlay)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = model::closeOverlay) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("My Pokémons", Modifier.weight(1f).padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minimumCellWidth),
            modifier = Modifier.fillMaxSize().testTag("pokemon-gallery"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CelebrationCategory.entries.forEach { category ->
                val categoryCelebrations = collected.filter { it.category == category }.sortedBy { it.ndex }
                val categoryTotal = Celebration.entries.count { it.category == category }
                item(key = "category-${category.name}", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(category.label, Modifier.weight(1f, fill = false).width(IntrinsicSize.Max),
                            style = MaterialTheme.typography.titleMedium)
                        Text("${categoryCelebrations.size}/$categoryTotal",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(IntrinsicSize.Max).testTag("pokemon-count-${category.name}").semantics {
                                contentDescription = "${categoryCelebrations.size} of $categoryTotal collected in ${category.label}"
                            })
                    }
                }
                items(categoryCelebrations, key = { it.name }) { pokemon ->
                    Column(
                        Modifier.clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(role = Role.Button, onClickLabel = "Replay ${pokemon.label}") { replay = pokemon }
                            .padding(8.dp).testTag("pokemon-${pokemon.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(Modifier.fillMaxWidth().aspectRatio(320f / 220f)
                            .clip(MaterialTheme.shapes.small).semantics {
                                contentDescription = "${pokemon.label} celebration"
                                role = Role.Image
                            }) {
                            drawCelebrationArtwork(pokemon, 0.6f)
                        }
                        Text(pokemon.collectionLabel, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (collected.isEmpty()) {
                item(key = "empty-collection", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Finish practices to collect Other celebrations. Pokémon can appear in Rewards Game after 15 or more questions.",
                        Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    replay?.let { pokemon ->
        CelebrationDialog(pokemon, onFinished = { replay = null }, replay = true)
    }
}

@Composable
fun HistoryScreen(model: MathViewModel) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val detail = model.historyDetail
    if (detail != null) {
        ScreenScaffold("Practice result", model::closeOverlay, actions = {
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete result") }
        }) {
            ResultsContent(detail, title = null, showCorrectAnswers = model.showCorrectAnswers,
                verticalEquations = model.verticalEquations) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (detail.prizeType != null && detail.prize == null) {
                        Button(onClick = { model.showRewardForResult(detail) }) { Text("Open gift box") }
                    }
                    OutlinedButton(onClick = model::closeOverlay, modifier = Modifier.widthIn(min = 160.dp)) { Text("Back") }
                }
            }
        }
    } else {
        ScreenScaffold("Practice History", model::closeOverlay, actions = {
            if (model.history.isNotEmpty()) IconButton(onClick = { confirmClear = true }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history")
            }
        }) {
            if (model.history.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No practice results yet. Finish a practice to see it here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().testTag("history-list"), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(model.history, key = { it.id }) { result ->
                        ListItem(
                            headlineContent = { Text("${result.operation.label} · ${result.numberDescription.removePrefix("numbers ")}") },
                            supportingContent = {
                                Column {
                                    Text("${formatFinishedAt(result.finishedAt)}\n${result.correct} right · ${result.wrong} wrong · ${formatDuration(result.durationMs)}")
                                    result.prize?.let { Text("Prize: ${it.type.fragmentLabel}") }
                                    result.pokemonReward?.let { Text("My Pokémons: ${it.collectionLabel}") }
                                    if (result.prizeType != null && result.prize == null) Text("Prize waiting — open this result to claim")
                                }
                            },
                            leadingContent = {
                                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    Text(result.operation.symbol, style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            },
                            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (result.gameMode == GameMode.REWARDS)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent),
                            modifier = Modifier.clickable { model.showHistoryDetail(result) })
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(onDismissRequest = { confirmClear = false },
            title = { Text("Clear practice history?") },
            text = { Text("All saved results and their earned reward fragments and My Pokémons rewards will be permanently removed. Used rewards cannot be recovered.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; model.clearHistory() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } })
    }
    if (confirmDelete && detail != null) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this result?") },
            text = { Text("This result and its earned reward fragment will be permanently removed, along with its My Pokémons reward unless another saved result also earned it. Used rewards cannot be recovered.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; model.deleteResult(detail.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    }
}

@Composable
internal fun SkinSetting(
    enabled: Boolean,
    skin: AppSkin,
    onSkinChange: (AppSkin) -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    var showSkinPicker by rememberSaveable(enabled) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Choose skin") { showSkinPicker = true }
            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Skins")
                Text(skin.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (enabled) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
        }
        Switch(enabled, onEnabledChange, modifier = Modifier.semantics { contentDescription = "Skins" })
    }
    if (enabled && showSkinPicker) {
        SkinPickerDialog(skin, onSkinChange = {
            onSkinChange(it)
            showSkinPicker = false
        }, dismiss = { showSkinPicker = false })
    }
}

@Composable
private fun SkinPickerDialog(skin: AppSkin, onSkinChange: (AppSkin) -> Unit, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text("Choose skin") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Background images • Your color theme stays the same", style = MaterialTheme.typography.bodySmall)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSkin.entries.forEach { option ->
                    val selected = skin == option
                    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.medium)
                        .selectable(selected, role = Role.RadioButton, onClick = { onSkinChange(option) })
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(painterResource(option.drawable), contentDescription = null,
                            modifier = Modifier.size(width = 64.dp, height = 112.dp).clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.titleSmall)
                            Text(option.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected, onClick = null)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Close") } })
}

@Composable
internal fun CollapsibleSettingsSection(
    title: String,
    defaultExpanded: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    var manualExpansion by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val expanded = manualExpansion ?: defaultExpanded
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse $title" else "Expand $title") {
                    manualExpansion = !expanded
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
        if (expanded) content()
    }
}
