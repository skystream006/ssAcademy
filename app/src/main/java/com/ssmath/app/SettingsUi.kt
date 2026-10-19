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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt

@Composable
internal fun ScreenScaffold(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {},
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
    var showingDebug by rememberSaveable { mutableStateOf(false) }
    var showingAdmin by remember { mutableStateOf(false) }
    var adminUnlocked by remember { mutableStateOf(false) }
    var showingName by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold("Settings", model::closeOverlay) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UpdateSettings()
            ListItem(headlineContent = { Text("Change Name") },
                supportingContent = { Text("Update the name used for all your games") },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { showingName = true }.testTag("change-name"))
            CollapsibleSettingsSection("Rewards", defaultExpanded = false) {
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
            }
            HorizontalDivider()
            CollapsibleSettingsSection("Result Histories", defaultExpanded = false) {
                Subject.entries.forEach { subject -> SubjectHistorySetting(model, subject) }
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
            ListItem(headlineContent = { Text("Admin Menu") },
                supportingContent = { Text("PIN-protected game settings and reward controls") },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    adminUnlocked = false
                    showingAdmin = true
                }.testTag("admin-menu"))
            HorizontalDivider()
            DebugLogSettings(onOpenDebug = { showingDebug = true })
            Text("ssAcademy ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 20.dp))
        }
    }
    if (showingDebug) {
        DebugDialog(model, onDismiss = { showingDebug = false })
    }
    if (showingAdmin) {
        val dismiss = { showingAdmin = false; adminUnlocked = false }
        if (adminUnlocked) {
            AdminDialog(model, onDismiss = dismiss)
        } else {
            AdminPinDialog(onUnlocked = { adminUnlocked = true }, onDismiss = dismiss)
        }
    }
    if (showingName) {
        ChangeNameDialog(rememberReadingViewModel(), onDismiss = { showingName = false })
    }
}

@Composable
internal fun ChangeNameDialog(model: ReadingViewModel, onDismiss: () -> Unit) {
    val profile = model.data?.profile
    var name by rememberSaveable(profile?.id, profile?.name) { mutableStateOf(profile?.name.orEmpty()) }
    var requestedName by rememberSaveable { mutableStateOf<String?>(null) }
    val validName = name.trim().let { it.length in 1..40 && it.none(Char::isISOControl) }
    LaunchedEffect(profile?.name, requestedName, model.saving, model.storageError) {
        if (requestedName != null && profile?.name == requestedName && !model.saving && model.storageError == null) {
            onDismiss()
        }
    }
    SettingsDialog(onDismissRequest = onDismiss,
        title = { Text("Change Name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("One name for all your games, saved only on this device. Your progress stays the same.")
                if (model.storageError != null) {
                    Text(requireNotNull(model.storageError))
                    TextButton(onClick = model::retry, enabled = !model.saving) { Text("Retry storage") }
                } else if (profile == null) {
                    Text(if (model.data == null) "Opening your private reading shelf…" else "Enter your name to get started.")
                } else {
                    OutlinedTextField(name, { name = it.take(40) }, label = { Text("Your name") },
                        singleLine = true, enabled = !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("settings-name"))
                    model.message?.let { Text(it) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                requestedName = name.trim()
                model.updateName(name)
            }, enabled = profile != null && validName && name.trim() != profile.name &&
                !model.saving && model.storageError == null,
                modifier = Modifier.testTag("settings-save-name")) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        modifier = Modifier.testTag("change-name-dialog"))
}

@Composable
internal fun ReadingLevelDialog(model: ReadingViewModel, onDismiss: () -> Unit) {
    val profile = model.data?.profile
    SettingsDialog(onDismissRequest = onDismiss,
        title = { Text("Set Reading Level") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (model.storageError != null) {
                    Text(requireNotNull(model.storageError))
                    TextButton(onClick = model::retry) { Text("Retry storage") }
                } else if (profile == null) {
                    Text(if (model.data == null) "Opening your private reading shelf…" else "Enter your name to get started.")
                } else {
                    Text("Choose a level for new activities. Your saved drafts stay at their original level.")
                    Column(Modifier.selectableGroup()) {
                        ReadingLevel.entries.forEach { level ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(
                                selected = profile.level == level,
                                role = Role.RadioButton,
                                enabled = !model.saving,
                                onClick = { model.act { ReadingEngine.level(it, level) } }
                            ).testTag("reading-level-${level.name}"),
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = profile.level == level, onClick = null)
                                Text(level.label, Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        modifier = Modifier.testTag("reading-level-dialog"))
}

@Composable
private fun ActiveReadingGamesDialog(model: MathViewModel, onDismiss: () -> Unit) {
    SettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text("Active Reading Games") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose which games are available. Changes are saved automatically. " +
                    "Saved drafts, history, and rewards are kept when a game is disabled.")
                ReadingGame.entries.forEach { game ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(game.title, Modifier.weight(1f))
                        Switch(game !in model.disabledReadingGames,
                            onCheckedChange = { model.chooseReadingGameEnabled(game, it) },
                            modifier = Modifier.semantics { contentDescription = game.title })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        modifier = Modifier.testTag("active-reading-games-dialog")
    )
}

@Composable
private fun SubjectRewardsSetting(model: MathViewModel, subject: Subject, description: String) {
    val label = "${subject.label} Rewards System"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(model.rewardsEnabled(subject), { model.chooseSubjectRewardsEnabled(subject, it) },
            modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun SubjectHistorySetting(model: MathViewModel, subject: Subject) {
    ListItem(headlineContent = { Text("${subject.label} Practice History") },
        supportingContent = { Text("Review your previous ${subject.label.lowercase()} results") },
        leadingContent = { Icon(Icons.Rounded.History, null) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { model.openSubjectHistory(subject) }
            .testTag(if (subject == Subject.MATH) "practice-history" else "${subject.name.lowercase()}-practice-history"))
}

@Composable
fun SubjectHistoryScreen(model: MathViewModel) {
    ScreenScaffold("${model.historySubject.label} Practice History", model::closeOverlay) {
        if (model.historySubject == Subject.READING) {
            ReadingHistoryScreen(model)
        } else {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No ${model.historySubject.label.lowercase()} practice results yet. More activities are coming soon.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DebugDialog(model: MathViewModel, onDismiss: () -> Unit) {
    var browsingReading by rememberSaveable { mutableStateOf(false) }
    val sectionState = rememberSaveableStateHolder()
    val scrollState = rememberScrollState()
    if (browsingReading) {
        ReadingDatabaseBrowser(onDismiss = { browsingReading = false }, backLabel = "Back to Debug Menu")
        return
    }
    SettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug Menu", Modifier.fillMaxWidth()) },
        text = {
            sectionState.SaveableStateProvider("debug-sections") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CollapsibleSettingsSection("My Pokémons", defaultExpanded = false) {
                        Text("Add all celebrations, including Mega Pokémons, without changing practice history or rewards.")
                        Button(onClick = model::collectAllCelebrations,
                            enabled = !model.pokemons.containsAll(Celebration.entries)) {
                            Text("Add all animations", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                        Text("${model.pokemons.size}/${Celebration.entries.size} animations collected")
                    }
                    HorizontalDivider()
                    CollapsibleSettingsSection("Reading activities", defaultExpanded = false) {
                        Button(onClick = { browsingReading = true },
                            modifier = Modifier.fillMaxWidth().testTag("reading-database-open")) {
                            Text("View reading activities database", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        scrollState = scrollState,
        modifier = Modifier.testTag("debug-dialog")
    )
}

@Composable
private fun AdminDialog(model: MathViewModel, onDismiss: () -> Unit) {
    var showingAdjustments by rememberSaveable { mutableStateOf(false) }
    var changingPin by remember { mutableStateOf(false) }
    var showingReadingGames by rememberSaveable { mutableStateOf(false) }
    var showingReadingLevel by rememberSaveable { mutableStateOf(false) }
    val sectionState = rememberSaveableStateHolder()
    val scrollState = rememberScrollState()
    if (changingPin) {
        ChangeAdminPinDialog(onChanged = onDismiss, onDismiss = { changingPin = false })
        return
    }
    if (showingAdjustments) {
        AddRemoveRewardsDialog(
            model.rewardBalances, model.removingReward, model.rewardRemovalError,
            model::addReward, model::removeReward, { showingAdjustments = false }
        )
        return
    }
    if (showingReadingGames) {
        ActiveReadingGamesDialog(model, onDismiss = { showingReadingGames = false })
        return
    }
    if (showingReadingLevel) {
        ReadingLevelDialog(rememberReadingViewModel(), onDismiss = { showingReadingLevel = false })
        return
    }
    SettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Menu", Modifier.fillMaxWidth()) },
        text = {
            sectionState.SaveableStateProvider("admin-sections") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { changingPin = true }, modifier = Modifier.fillMaxWidth().testTag("change-pin")) {
                        Text("Change PIN", textAlign = TextAlign.Center)
                    }
                    Button(onClick = { showingAdjustments = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add/Remove Rewards", textAlign = TextAlign.Center)
                    }
                    HorizontalDivider()
                    CollapsibleSettingsSection("Available Rewards", defaultExpanded = false) {
                        Text("Choose which rewards can be awarded when a Rewards Game finishes. Existing prizes and fragments stay available.")
                        RewardMinimumSettings(model)
                        RewardTier.entries.forEach { tier ->
                            Text("${tier.label} · ${model.rewardThresholds.questionCountLabel(tier)}", Modifier.fillMaxWidth(),
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
                    HorizontalDivider()
                    CollapsibleSettingsSection("Game Settings", defaultExpanded = false) {
                        GameSettings(model,
                            onRewardsSetup = { onDismiss(); model.openRewardsSetup() },
                            onReadingProgress = { onDismiss(); model.openReadingProgress() },
                            onReadingLevel = { showingReadingLevel = true },
                            onReadingGames = { showingReadingGames = true })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        scrollState = scrollState,
        modifier = Modifier.testTag("admin-dialog")
    )
}

@Composable
private fun RewardMinimumSettings(model: MathViewModel) {
    var tier1Text by rememberSaveable(model.rewardThresholds) {
        mutableStateOf(model.rewardThresholds.tier1Minimum.toString())
    }
    var tier2Text by rememberSaveable(model.rewardThresholds) {
        mutableStateOf(model.rewardThresholds.tier2Minimum.toString())
    }
    val tier1 = parseQuestionCount(tier1Text)
    val tier2 = parseQuestionCount(tier2Text)
    val valid = tier1 != null && tier2 != null && tier1 < tier2
    Text("Minimum questions for Math and Reading rewards. Tier 2 must be higher than Tier 1. " +
        "Changes apply when a game finishes; saved prizes keep their original rules.")
    Text("Tier 1 minimum questions", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(tier1Text, { tier1Text = it.take(6) },
        supportingText = { Text("Whole number from 1 to $MAX_QUESTION_COUNT") },
        isError = tier1 == null, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("reward-tier-1-minimum")
            .semantics { contentDescription = "Tier 1 minimum questions" })
    Text("Tier 2 minimum questions", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(tier2Text, { tier2Text = it.take(6) },
        supportingText = { Text("Whole number from 1 to $MAX_QUESTION_COUNT, higher than Tier 1") },
        isError = !valid, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("reward-tier-2-minimum")
            .semantics { contentDescription = "Tier 2 minimum questions" })
    Button(onClick = {
        if (tier1 != null && tier2 != null && tier1 < tier2) model.chooseRewardThresholds(tier1, tier2)
    }, enabled = valid && (tier1 != model.rewardThresholds.tier1Minimum ||
        tier2 != model.rewardThresholds.tier2Minimum),
        modifier = Modifier.fillMaxWidth().testTag("save-reward-minimums")) {
        Text("Save minimum questions", textAlign = TextAlign.Center)
    }
}

@Composable
private fun GameSettings(
    model: MathViewModel,
    onRewardsSetup: () -> Unit,
    onReadingProgress: () -> Unit,
    onReadingLevel: () -> Unit,
    onReadingGames: () -> Unit
) {
    CollapsibleSettingsSection("Math Settings", defaultExpanded = false) {
        SubjectRewardsSetting(model, Subject.MATH,
            "Earn prize fragments in Rewards Game by completing ${model.rewardThresholds.tier1Minimum} " +
                "or more questions with over 90% correct")
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
            TimeLimitSetting(model.timeLimitSeconds, model::chooseTimeLimitSeconds,
                model.pauseInBackground, model::choosePauseInBackground)
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Randomize matrix question order")
                Text("Shuffle all 121 questions once per Multiplication Matrix Rewards Game. Applies to new games.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(model.randomizeMatrixQuestions, model::chooseRandomizeMatrixQuestions,
                modifier = Modifier.semantics { contentDescription = "Randomize matrix question order" })
        }
        Text("Matrix reward tier", style = MaterialTheme.typography.titleSmall)
        Text("Use this tier instead of the question minimums for Multiplication Matrix Rewards Game. " +
            "Changes apply when the game finishes. Accuracy, timer, and enabled-reward rules still apply.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup()) {
            RewardTier.entries.forEach { tier ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                    selected = model.matrixRewardTier == tier, role = Role.RadioButton,
                    onClick = { model.chooseMatrixRewardTier(tier) }
                ).testTag("matrix-reward-${tier.name}"),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = model.matrixRewardTier == tier, onClick = null)
                    Text(tier.label, Modifier.padding(start = 8.dp))
                }
            }
        }
        ListItem(headlineContent = { Text("Default Math Reward Game Settings") },
            supportingContent = { Text("Default number limits for each math type") },
            leadingContent = { Icon(Icons.Rounded.Settings, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onRewardsSetup).testTag("rewards-setup"))
    }
    HorizontalDivider()
    CollapsibleSettingsSection("Reading Settings", defaultExpanded = false) {
        ReadingPackSettings()
        SubjectRewardsSetting(model, Subject.READING,
            "Earn prize fragments by completing ${model.rewardThresholds.tier1Minimum} " +
                "or more questions with over 90% correct.")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Time limit per reading game", style = MaterialTheme.typography.titleSmall)
            TimeLimitDropdown(model.readingTimeLimitSeconds, model::chooseReadingTimeLimitSeconds,
                tag = "reading-time-limit")
            Text("Total time, regardless of question count. Requires Reading Rewards System. " +
                "None disables the limit. Saved games keep their original limit.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ListItem(headlineContent = { Text("Reading Progress") },
            supportingContent = { Text("Private progress and saved stories") },
            leadingContent = { Icon(Icons.Rounded.History, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onReadingProgress).testTag("reading-progress"))
        ListItem(headlineContent = { Text("Set Reading Level") },
            leadingContent = { Icon(Icons.Rounded.Settings, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onReadingLevel).testTag("set-reading-level"))
        ListItem(headlineContent = { Text("Active Reading Games") },
            supportingContent = { Text("Choose which reading games are available") },
            leadingContent = { Icon(Icons.Rounded.Settings, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onReadingGames).testTag("active-reading-games"))
    }
    listOf(Subject.SCIENCE, Subject.ADVANCED).forEach { subject ->
        HorizontalDivider()
        val title = if (subject == Subject.ADVANCED) "Advanced Subject Settings" else "${subject.label} Settings"
        CollapsibleSettingsSection(title, defaultExpanded = false) {
            SubjectRewardsSetting(model, subject,
                "Choose whether to earn rewards when ${subject.label.lowercase()} activities become available.")
            Text("More activities are coming soon.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun AddRemoveRewardsDialog(
    balances: Map<RewardType, RewardBalance>,
    updatingReward: Boolean,
    error: String?,
    onAdd: (RewardType, Boolean) -> Unit,
    onRemove: (RewardType, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf<RewardType?>(null) }
    var fragment by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val choice = selected
    val balance = balances[choice] ?: RewardBalance()
    val canConfirm = if (adding) {
        if (fragment) balance.canAddFragment else balance.canAddWhole
    } else (if (fragment) balance.fragments else balance.whole) > 0
    SettingsDialog(
        onDismissRequest = { if (choice != null) selected = null else onDismiss() },
        title = { Text(if (choice == null) "Add/Remove Rewards" else if (adding) "Add reward?" else "Remove reward?") },
        status = {
            if (choice == null && (updatingReward || error != null)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (updatingReward) {
                        Text("Updating reward…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
            }
        },
        text = {
            if (choice != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (adding) {
                        if (fragment) "Add 1 ${choice.label} fragment?" else "Add 1 whole ${choice.label} reward?"
                    } else {
                        if (fragment) "Permanently remove 1 ${choice.label} fragment?"
                        else "Permanently remove 1 whole ${choice.label} reward?"
                    })
                    if (adding && fragment) Text("Every 3 matching fragments become 1 whole reward.")
                    Text("Practice history and My Pokémons will not change.")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Add or remove one fragment or whole reward at a time. " +
                            "3 matching fragments become 1 whole reward. Removing a fragment does not break a whole reward.")
                        if (balances.values.none { it.totalFragments > 0 }) {
                            Text("No rewards or fragments available.")
                        }
                        RewardTier.entries.forEach { tier ->
                            Text(tier.label, style = MaterialTheme.typography.titleMedium)
                            RewardType.entries.filter { it.tier == tier }.chunked(4).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { type ->
                                        val available = balances[type] ?: RewardBalance()
                                        Column(Modifier.weight(1f).testTag("adjust-reward-${type.name}"),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(type.label, style = MaterialTheme.typography.labelMedium,
                                                textAlign = TextAlign.Center, modifier = Modifier.heightIn(min = 48.dp))
                                            Text("Whole: ${available.whole} · Fragments: ${available.fragments}/3",
                                                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                            listOf(false, true).forEach { part ->
                                                RewardAdjustmentReel(type, part, available, updatingReward,
                                                    onAdd = { selected = type; fragment = part; adding = true },
                                                    onRemove = { selected = type; fragment = part; adding = false })
                                            }
                                        }
                                    }
                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
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
                    enabled = !updatingReward && canConfirm,
                    onClick = {
                        if (selected == choice) {
                            selected = null
                            if (adding) onAdd(choice, fragment) else onRemove(choice, fragment)
                        }
                    }
                ) { Text(if (adding) "Add" else "Remove") }
            } else {
                TextButton(onClick = onDismiss) { Text("Back to Admin Menu") }
            }
        },
        dismissButton = {
            if (choice != null) {
                TextButton(onClick = { selected = null }) { Text("Cancel") }
            }
        },
        modifier = Modifier.testTag("adjust-rewards-dialog")
    )
}

@Composable
private fun RewardAdjustmentReel(
    type: RewardType,
    fragment: Boolean,
    balance: RewardBalance,
    busy: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val description = if (fragment) "${type.label} fragment" else "whole ${type.label} reward"
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
        horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onAdd,
            enabled = !busy && if (fragment) balance.canAddFragment else balance.canAddWhole) {
            Icon(Icons.Rounded.ExpandLess, "Add 1 $description")
        }
        Text(if (fragment) "${balance.fragments}/3" else "${balance.whole}",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        RewardImage(type, fragment, Modifier.fillMaxWidth().aspectRatio(1f)
            .testTag("adjust-image-${type.name}-$fragment"))
        Text(if (fragment) "Fragments" else "Whole", style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center)
        IconButton(onClick = onRemove,
            enabled = !busy && (if (fragment) balance.fragments else balance.whole) > 0) {
            Icon(Icons.Rounded.ExpandMore, "Remove 1 $description")
        }
    }
}

@Composable
private fun TimeLimitSetting(
    seconds: Int,
    onChange: (Int) -> Unit,
    pauseInBackground: Boolean,
    onPauseInBackgroundChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Time limit per 5 questions", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeLimitDropdown(seconds, onChange, tag = "time-limit")
            OutlinedIconToggleButton(checked = pauseInBackground, onCheckedChange = onPauseInBackgroundChange,
                modifier = Modifier.semantics {
                    contentDescription = "Pause in background"
                    stateDescription = if (pauseInBackground) "On" else "Off"
                }) {
                Icon(if (pauseInBackground) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
            }
        }
        Text(if (pauseInBackground) "Pause in background: On. Math time pauses when you leave the app."
            else "Pause in background: Off. Math time keeps running when you leave the app.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Rewards Game only, not Practice Game. 1 minute per 5 questions gives 5 minutes for 25 questions. " +
            "Time always pauses in settings.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimeLimitDropdown(seconds: Int, onChange: (Int) -> Unit, tag: String) {
    var expanded by remember { mutableStateOf(false) }
    fun label(value: Int) = when (value) {
        0 -> "None"
        in 1..59 -> "$value seconds"
        60 -> "1 minute"
        else -> "${value / 60} minutes"
    }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag(tag)) {
            Text(label(seconds))
            Icon(Icons.Rounded.ExpandMore, null)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            timeLimitOptionsSeconds.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = { onChange(option); expanded = false },
                    modifier = Modifier.semantics { selected = seconds == option })
            }
        }
    }
}

@Composable
fun RewardsScreen(model: MathViewModel) {
    var usingRewards by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold("My Rewards", model::closeOverlay, actions = {
        TextButton(onClick = { usingRewards = true }) { Text("Use rewards") }
    }) {
        RewardInventory(model.rewardBalances, thresholds = model.rewardThresholds)
    }
    if (usingRewards) {
        UseRewardsDialog(
            model.rewardBalances, model.usingReward, model.rewardUseError,
            model::useReward, { usingRewards = false }, thresholds = model.rewardThresholds
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
                val categoryCelebrations = collected.filter { it.category == category }
                    .sortedWith(compareBy<Celebration> { it.ndex }.thenBy { it.ndexSuffix })
                val categoryTotal = Celebration.entries.count { it.category == category }
                item(key = "category-${category.name}", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
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
                        if (category == CelebrationCategory.MEGA_POKEMONS) {
                            Text("Only obtainable in the Multiplication Matrix Rewards Game.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp))
                        }
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
                        if (pokemon.category == CelebrationCategory.MEGA_POKEMONS) {
                            Text("Mega Pokémon", style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (collected.isEmpty()) {
                item(key = "empty-collection", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Finish practices to collect Other celebrations. Pokémon can appear in Rewards Game after 15 or more questions. " +
                        "Mega Pokémons are exclusive to the Multiplication Matrix Rewards Game.",
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
        ScreenScaffold("Math Practice History", model::closeOverlay, actions = {
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
                            headlineContent = { Text("${result.gameLabel} · ${result.numberDescription.removePrefix("numbers ")}") },
                            supportingContent = {
                                Column {
                                    Text("${formatFinishedAt(result.finishedAt)}\n${result.correct} right · ${result.wrong} wrong · ${formatDuration(result.durationMs)}")
                                    result.prize?.let { Text("Prize: ${it.type.fragmentLabel}") }
                                    result.pokemonRewards.forEach { Text("My Pokémons: ${it.collectionLabel}") }
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
        SettingsDialog(onDismissRequest = { confirmClear = false },
            title = { Text("Clear practice history?") },
            text = { Text("All saved results and their earned reward fragments and My Pokémons rewards will be permanently removed. Used rewards cannot be recovered.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; model.clearHistory() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } })
    }
    if (confirmDelete && detail != null) {
        SettingsDialog(onDismissRequest = { confirmDelete = false },
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
    SettingsDialog(onDismissRequest = dismiss, title = { Text("Choose skin") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.16f), MaterialTheme.shapes.medium)
        .padding(8.dp).testTag("settings-section-$title"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
