@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ssmath.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

internal val CorrectGreen = Color(0xFF2E9E4F)
internal val WrongRed = Color(0xFFD32F2F)

@Composable
fun MathApp(model: MathViewModel) {
    UpdateNotification()
    MathAppContent(model)
}

@Composable
internal fun MathAppContent(model: MathViewModel) {
    MathTheme(model.theme, model.mode, model.waveAppearance, if (model.skinsEnabled) model.skin else null,
        textSizePercent = model.textSizePercent) {
        val snackbar = remember { SnackbarHostState() }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) model.setForeground(true)
                if (event == Lifecycle.Event.ON_PAUSE) model.setForeground(false)
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(model.message) {
            model.message?.let {
                snackbar.showSnackbar(it)
                model.consumeMessage()
            }
        }
        SkinBackground(Modifier.fillMaxSize()) {
            Scaffold(containerColor = appBackgroundColor(), contentColor = MaterialTheme.colorScheme.onBackground,
                snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                    when (model.overlay) {
                        Overlay.SETTINGS -> SettingsScreen(model)
                        Overlay.REWARDS_SETUP -> RewardsSetupScreen(model)
                        Overlay.HISTORY -> HistoryScreen(model)
                        Overlay.REWARDS -> RewardsScreen(model)
                        Overlay.POKEMONS -> PokemonsScreen(model)
                        null -> {
                            MainContent(model)
                            SettingsButton(model::openSettings, Modifier.align(Alignment.BottomEnd))
                        }
                    }
                    model.rewardResult?.let { result ->
                        if (model.rewardDialogVisible) RewardGiftDialog(result, model.claimingReward, model.rewardError,
                            onOpen = model::claimReward, onDismiss = model::dismissReward)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.padding(8.dp).alpha(0.5f).testTag("settings-button")) {
        Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun MainContent(model: MathViewModel) {
    var confirmQuit by rememberSaveable(model.screen) { mutableStateOf(false) }
    BackHandler(enabled = model.screen != Screen.SETUP || model.mathGamesOpen) {
        when (model.screen) {
            Screen.READY -> model.backToSetup()
            Screen.PLAYING -> confirmQuit = true
            Screen.RESULTS -> model.done()
            Screen.SETUP -> model.closeMathGames()
        }
    }
    when (model.screen) {
        Screen.SETUP -> HomeScreen(model)
        Screen.READY -> ReadyDialog(model)
        Screen.PLAYING -> PlayingScreen(model, onQuit = { confirmQuit = true })
        Screen.RESULTS -> model.lastResult?.let { result ->
            ResultsContent(result, title = "Results", showCorrectAnswers = model.showCorrectAnswers,
                verticalEquations = model.verticalEquations) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (result.prizeType != null && result.prize == null && model.celebration == null &&
                        !model.selectingCelebration) {
                        Button(onClick = { model.showRewardForResult(result) }) { Text("Open gift box") }
                    }
                    Button(onClick = model::done, modifier = Modifier.widthIn(min = 160.dp).testTag("done-button")) { Text("Done") }
                }
            }
            model.celebration?.let { celebration ->
                CelebrationDialog(celebration, onFinished = model::dismissCelebration,
                    onPresented = { model.collectPresentedCelebration(celebration) })
            }
            model.earlyFinishMessage?.let { message ->
                AlertDialog(onDismissRequest = model::dismissEarlyFinishDialog,
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = model::dismissEarlyFinishDialog,
                            modifier = Modifier.testTag("view-results")) { Text("View results") }
                    })
            }
        }
    }
    if (confirmQuit && model.screen == Screen.PLAYING) {
        AlertDialog(onDismissRequest = { confirmQuit = false },
            title = { Text("Quit this practice?") },
            text = { Text("Results are only saved after answering all ${model.game?.questionCount} questions or " +
                "reaching ${model.game?.maxWrongAnswers} wrong answers" +
                if (model.activeTimeLimitMs != null) ", or when time runs out." else ".") },
            confirmButton = { TextButton(onClick = { confirmQuit = false; model.backToSetup() }) { Text("Quit") } },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Keep practicing") } })
    }
}

@Composable
private fun DialogCard(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
    }
}

@Composable
private fun HomeScreen(model: MathViewModel) {
    val textMeasurer = rememberTextMeasurer()
    val labelWidth = listOf("Rewards", "Pokémon").maxOf {
        textMeasurer.measure(it, MaterialTheme.typography.labelLarge, softWrap = false).size.width
    }
    // Both buttons must fit the wider label, image, spacing, and horizontal padding.
    val minimumButtonWidth = with(LocalDensity.current) { labelWidth.toDp() } + 24.dp + 8.dp + 16.dp
    Column(Modifier.fillMaxSize().testTag("home-screen"), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
            Button(onClick = model::openRewards,
                modifier = Modifier.weight(1f).widthIn(min = minimumButtonWidth)
                    .heightIn(min = 48.dp).testTag("home-rewards"),
                contentPadding = PaddingValues(8.dp)) {
                Image(painterResource(R.drawable.ic_gift_box), contentDescription = null,
                    modifier = Modifier.size(24.dp).testTag("home-rewards-image"))
                Spacer(Modifier.width(8.dp))
                Text("Rewards", Modifier.width(IntrinsicSize.Max), textAlign = TextAlign.Center)
            }
            Button(onClick = model::openPokemons,
                modifier = Modifier.weight(1f).widthIn(min = minimumButtonWidth)
                    .heightIn(min = 48.dp).testTag("home-pokemons"),
                contentPadding = PaddingValues(8.dp)) {
                Image(painterResource(R.drawable.ic_pokeball), contentDescription = null,
                    modifier = Modifier.size(24.dp).testTag("home-pokemons-image"))
                Spacer(Modifier.width(8.dp))
                Text("Pokémon", Modifier.width(IntrinsicSize.Max), textAlign = TextAlign.Center)
            }
        }
        if (model.mathGamesOpen) {
            Column(Modifier.weight(1f).widthIn(max = 440.dp).fillMaxWidth().testTag("math-games-screen")
                .padding(bottom = 64.dp).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                TextButton(onClick = model::closeMathGames, modifier = Modifier.testTag("subjects-back")) {
                    Text("Back to subjects")
                }
                Text("Math", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = model::openPracticeGame,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("practice-game")) {
                    Text("Practice Game", textAlign = TextAlign.Center)
                }
                Button(onClick = model::openRewardsGame,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("rewards-game")) {
                    Text("Rewards Game", textAlign = TextAlign.Center)
                }
            }
        } else {
            SubjectGrid(model::openMathGames, Modifier.weight(1f))
        }
    }
    when (model.setupDialog) {
        SetupDialog.PRACTICE -> GameSetupDialog(model::dismissSetupDialog) {
            Text("Practice Game", style = MaterialTheme.typography.headlineSmall)
            Text("Practice with Other animations only. No prizes or Pokémon are awarded.")
            SetupForm(model, Modifier.fillMaxWidth())
            TextButton(onClick = model::dismissSetupDialog, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel")
            }
        }
        SetupDialog.REWARDS -> GameSetupDialog(model::dismissSetupDialog) {
            Text("Rewards Game", style = MaterialTheme.typography.headlineSmall)
            Text("How many questions would you like?", style = MaterialTheme.typography.titleLarge)
            RewardsQuestionCountChoices(model)
            RewardsCriteria(model)
            Text("Choose a math type to play with its saved number limits.")
            Operation.entries.forEach { operation ->
                Button(onClick = { model.selectRewardsGame(operation) },
                    enabled = model.rewardsQuestionCount != null,
                    modifier = Modifier.fillMaxWidth().testTag("rewards-operation-${operation.name}")) {
                    Text("${operation.symbol}  ${operation.label}", textAlign = TextAlign.Center)
                }
            }
            TextButton(onClick = model::dismissSetupDialog, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel")
            }
        }
        null -> Unit
    }
}

@Composable
private fun SubjectGrid(onMath: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val comingSoon = {
        Toast.makeText(context, "More Activities are coming soon!", Toast.LENGTH_SHORT).show()
    }
    Column(modifier.widthIn(max = 440.dp).fillMaxWidth().testTag("subject-grid")
        .padding(bottom = 64.dp).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SubjectButton("Math", R.drawable.subject_math, onMath, Modifier.weight(1f))
            SubjectButton("Reading", R.drawable.subject_reading, comingSoon, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SubjectButton("Science", R.drawable.subject_science, comingSoon, Modifier.weight(1f))
            SubjectButton("Advanced", R.drawable.subject_advanced, comingSoon, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SubjectButton(label: String, background: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.aspectRatio(1f).testTag("subject-$label"),
        shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(background), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            Text(label, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xF2112440)).padding(horizontal = 8.dp, vertical = 10.dp),
                color = Color.White, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RewardsQuestionCountChoices(model: MathViewModel) {
    val choices = listOf(
        Triple(15, R.drawable.ic_pokeball, "Poké Ball"),
        Triple(25, R.drawable.ic_gift_box_blue, "Blue gift box"),
        Triple(50, R.drawable.ic_gift_box_royal_purple, "Royal purple gift box")
    )
    val textMeasurer = rememberTextMeasurer()
    val labelWidth = choices.maxOf { (count, _, _) ->
        textMeasurer.measure(count.toString(), MaterialTheme.typography.labelLarge, softWrap = false).size.width
    }
    val minimumButtonWidth = maxOf(64.dp, with(LocalDensity.current) { labelWidth.toDp() } + 16.dp)
    FlowRow(Modifier.fillMaxWidth().selectableGroup().testTag("rewards-question-count-choices"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
        choices.forEach { (count, image, description) ->
            val selected = model.rewardsQuestionCount == count
            OutlinedButton(onClick = { model.updateRewardsQuestionCount(count.toString()) },
                modifier = Modifier.weight(1f).widthIn(min = minimumButtonWidth)
                    .testTag("rewards-question-count-$count").semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = "$count questions"
                    },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(painterResource(image), contentDescription = description, modifier = Modifier.size(40.dp))
                    Text(count.toString(), Modifier.width(IntrinsicSize.Max), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RewardsCriteria(model: MathViewModel) {
    Column(Modifier.fillMaxWidth().testTag("rewards-criteria"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Obtainable rewards (requires 90% correct)", style = MaterialTheme.typography.titleMedium)
        Text("• Pokémon: 15 or more questions.")
        if (!model.rewardsEnabled) {
            Text("Prize fragments are unavailable while the Rewards system is OFF.")
        }
        RewardTier.entries.forEach { tier ->
            val available = RewardType.entries.filter { it.tier == tier && it !in model.disabledRewards }
            Text("• ${tier.label}: ${tier.questionCountLabel}", style = MaterialTheme.typography.titleSmall)
            if (!model.rewardsEnabled) {
                Text("Unavailable: Rewards system is OFF.")
            }
            if (available.isEmpty()) {
                Text("Unavailable: no rewards enabled in this tier.")
            }
        }
    }
}

@Composable
private fun GameSetupDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(16.dp).imePadding(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
private fun RewardsSetupScreen(model: MathViewModel) {
    BackHandler(onBack = model::cancelRewardsSetup)
    Column(Modifier.fillMaxSize().testTag("rewards-setup-screen").verticalScroll(rememberScrollState())
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = model::cancelRewardsSetup) { Text("Back") }
        Text("Rewards Game Setup", style = MaterialTheme.typography.headlineSmall)
        Text("Save default number limits for each math type. Choose the number of questions when starting a Rewards Game. " +
            "Practice Game settings stay separate.")
        Operation.entries.forEach { operation ->
            val parameters = model.rewardsSetupDraft.getValue(operation)
            val division = operation == Operation.DIVISION
            Card(Modifier.widthIn(max = 600.dp).fillMaxWidth().align(Alignment.CenterHorizontally)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(operation.label, style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(parameters.firstText,
                        { model.updateRewardsParameters(operation, parameters.copy(firstText = it)) },
                        label = { Text(if (division) "Maximum First number" else "Minimum number") },
                        supportingText = { Text(if (division) "Whole number from $MIN_MAXIMUM to ${"%,d".format(MAX_MAXIMUM)}"
                            else "Whole number from $MIN_MAXIMUM to the maximum number") },
                        isError = parameters.firstText.isNotEmpty() && (parameters.first == null ||
                            (!division && parameters.second != null && (parameters.first ?: 0) > requireNotNull(parameters.second))),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("rewards-${operation.name}-first"))
                    OutlinedTextField(parameters.secondText,
                        { model.updateRewardsParameters(operation, parameters.copy(secondText = it)) },
                        label = { Text(if (division) "Maximum Second number" else "Maximum number") },
                        supportingText = { Text("Whole number from $MIN_MAXIMUM to ${"%,d".format(MAX_MAXIMUM)}") },
                        isError = parameters.secondText.isNotEmpty() && parameters.second == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().testTag("rewards-${operation.name}-second"))
                }
            }
        }
        Button(onClick = model::saveRewardsSetup, enabled = model.canSaveRewardsSetup,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().align(Alignment.CenterHorizontally)
                .testTag("save-rewards-setup")) { Text("Save defaults") }
    }
}

@Composable
private fun SetupForm(model: MathViewModel, modifier: Modifier) {
    val division = model.selectedOperation == Operation.DIVISION
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("What would you like to practice?", style = MaterialTheme.typography.titleLarge)
        Operation.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { operation ->
                    val selected = model.selectedOperation == operation
                    val modifier = Modifier.weight(1f).heightIn(min = 72.dp).semantics { this.selected = selected }
                        .testTag("operation-${operation.name}")
                    val padding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    val label: @Composable () -> Unit = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(operation.symbol, style = MaterialTheme.typography.titleLarge)
                            Text(operation.label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (selected) Button(onClick = { model.selectOperation(operation) }, modifier, contentPadding = padding) { label() }
                    else OutlinedButton(onClick = { model.selectOperation(operation) }, modifier, contentPadding = padding) { label() }
                }
            }
        }
        Text(if (division) "What is the Maximum First number?" else "What is the minimum number?",
            style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(if (division) model.divisionMaximumFirstText else model.minimumText,
            if (division) model::updateDivisionMaximumFirst else model::updateMinimum, singleLine = true,
            label = { Text(if (division) "Maximum First number" else "Minimum number") },
            supportingText = { Text(if (division) "Whole number from $MIN_MAXIMUM to ${"%,d".format(MAX_MAXIMUM)}"
                else "Whole number from $MIN_MAXIMUM to the maximum number") },
            isError = if (division) model.divisionMaximumFirstText.isNotEmpty() && model.divisionMaximumFirst == null
                else model.minimumText.isNotEmpty() && model.minimum == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("minimum-input"))
        Text(if (division) "What is the Maximum Second number?" else "What is the maximum number?",
            style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(if (division) model.divisionMaximumSecondText else model.maximumText,
            if (division) model::updateDivisionMaximumSecond else model::updateMaximum, singleLine = true,
            label = { Text(if (division) "Maximum Second number" else "Maximum number") },
            supportingText = { Text("Whole number from $MIN_MAXIMUM to ${"%,d".format(MAX_MAXIMUM)}") },
            isError = if (division) model.divisionMaximumSecondText.isNotEmpty() && model.divisionMaximumSecond == null
                else model.maximumText.isNotEmpty() && model.maximum == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("maximum-input"))
        Text("How many questions would you like?", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(model.questionCountText, model::updateQuestionCount, singleLine = true,
            label = { Text("Number of questions") },
            supportingText = { Text("Whole number from $MIN_QUESTION_COUNT to ${"%,d".format(MAX_QUESTION_COUNT)}") },
            isError = model.questionCountText.isNotEmpty() && model.questionCount == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { model.submitSetup() }),
            modifier = Modifier.fillMaxWidth().testTag("question-count-input"))
        Button(onClick = model::submitSetup, enabled = model.canSubmitSetup,
            modifier = Modifier.fillMaxWidth().testTag("submit-setup")) { Text("Submit") }
    }
}

@Composable
private fun ReadyDialog(model: MathViewModel) {
    val operation = model.readyOperation ?: return
    val parameters = model.readyParameters ?: return
    val numbers = if (operation == Operation.DIVISION)
        "first up to ${parameters.first} · second up to ${parameters.second}"
        else "numbers ${parameters.first} to ${parameters.second}"
    DialogCard {
        Text(if (model.gameMode == GameMode.PRACTICE) "Practice Game" else "Rewards Game",
            style = MaterialTheme.typography.titleMedium)
        Text("Press Start when Ready", style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("${operation.label} · $numbers · ${parameters.questionCount} questions",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        if (model.gameMode == GameMode.REWARDS && model.rewardsEnabled && model.showTimer && model.timeLimitMinutes > 0) {
            Text("Time limit: ${model.timeLimitMinutes} minutes", modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }
        Button(onClick = model::start, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("start-button")) {
            Text("Start", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = model::backToSetup, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}

@Composable
private fun PlayingScreen(model: MathViewModel, onQuit: () -> Unit) {
    val game = model.game ?: return
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val statistics: @Composable () -> Unit = {
        Text("Points: ${game.correct}", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("points"))
        if (model.showTimer || model.activeTimeLimitMs != null) TimerText(model)
    }
    val wrongTally: @Composable () -> Unit = {
        Text("Wrong: ${game.wrong}", style = MaterialTheme.typography.titleMedium, color = WrongRed,
            modifier = Modifier.padding(20.dp).testTag("wrong-tally"))
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // In short windows, only Quit stays fixed so the keyboard cannot hide the answer controls.
        val compact = maxHeight < 400.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    if (!compact) statistics()
                }
                OutlinedButton(onClick = onQuit,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("quit-game")) { Text("Quit game") }
            }
            Column(Modifier.weight(1f).align(Alignment.CenterHorizontally).widthIn(max = 440.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                if (compact) Column(Modifier.align(Alignment.Start)) { statistics() }
                Text("Question ${game.attempts.size + 1} of ${game.questionCount}",
                   style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("question-progress"))
                GameProgress(game.attempts.size, game.questionCount, model.progressIcon)
                Equation(game.problem, "?", model.verticalEquations,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    modifier = Modifier.testTag("problem"))
                OutlinedTextField(model.answerText, model::updateAnswer, singleLine = true,
                    label = { Text("Your answer") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { model.submitAnswer() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("answer-input"))
                Button(onClick = model::submitAnswer, enabled = parseAnswer(model.answerText) != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("submit-answer")) { Text("Submit") }
                model.feedback?.let {
                    val correction = game.attempts.lastOrNull()?.takeIf {
                        model.verticalEquations && model.showCorrectAnswers && !it.correct
                    }?.problem
                    Text(if (correction == null) it.text else "Not quite:",
                        color = if (it.correct) CorrectGreen else WrongRed,
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (correction != null) Equation(correction, correction.answer.toString(), vertical = true,
                        style = MaterialTheme.typography.titleMedium.copy(color = WrongRed),
                        modifier = Modifier.testTag("feedback-equation"))
                }
                if (compact) Box(Modifier.align(Alignment.Start)) { wrongTally() }
            }
            if (!compact) wrongTally()
        }
    }
}

@Composable
internal fun Equation(problem: Problem, answer: String, vertical: Boolean, style: TextStyle, modifier: Modifier = Modifier) {
    val description = "${problem.text} = $answer"
    if (!vertical) {
        Text(description, style = style, modifier = modifier)
        return
    }
    val left = problem.left.toString()
    val right = problem.right.toString()
    val places = maxOf(left.length, right.length, answer.length)
    val operands = "  ${left.padStart(places)}\n${problem.operation.symbol} ${right.padStart(places)}"
    val result = "  ${answer.padStart(places)}"
    val text = "$operands\n$result"
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Equal-width digits and a shared right edge keep every place aligned, even in RTL locales.
    val numberStyle = style.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.sp,
        textAlign = TextAlign.Right, textDirection = TextDirection.Ltr)
    BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = description }) {
        val fittedStyle = remember(text, numberStyle, measurer, constraints.maxWidth) {
            val availableWidth = (constraints.maxWidth - 1).coerceAtLeast(1)
            fun scaledStyle(scale: Float) = numberStyle.copy(
                fontSize = numberStyle.fontSize * scale, lineHeight = numberStyle.lineHeight * scale)
            if (measurer.measure(text, numberStyle, softWrap = false).size.width <= availableWidth) numberStyle
            else {
                // Measure each candidate because Android's accessibility font scaling is nonlinear.
                var lower = 0f
                var upper = 1f
                repeat(12) {
                    val scale = (lower + upper) / 2
                    if (measurer.measure(text, scaledStyle(scale), softWrap = false).size.width <= availableWidth) lower = scale
                    else upper = scale
                }
                scaledStyle(lower)
            }
        }
        val width = with(density) { measurer.measure(text, fittedStyle, softWrap = false).size.width.toDp() }
        Column(Modifier.width(width)) {
            Text(operands, style = fittedStyle, softWrap = false,
                modifier = Modifier.fillMaxWidth().testTag("equation-operands"))
            HorizontalDivider(color = LocalContentColor.current, modifier = Modifier.testTag("equation-line"))
            Text(result, style = fittedStyle, softWrap = false,
                modifier = Modifier.fillMaxWidth().testTag("equation-answer"))
        }
    }
}

@Composable
internal fun GameProgress(completed: Int, total: Int, icon: ProgressIcon, modifier: Modifier = Modifier) {
    val position = remember(total) { Animatable(completed.toFloat()) }
    LaunchedEffect(completed, total) {
        position.animateTo(completed.toFloat(), animationSpec = tween(500))
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(64.dp).testTag("game-progress").semantics {
        contentDescription = "Practice progress"
        stateDescription = "$completed of $total questions answered"
        progressBarRangeInfo = ProgressBarRangeInfo(completed.toFloat() / total, 0f..1f, total - 1)
    }) {
        val travel = (maxWidth - 32.dp).coerceAtLeast(0.dp)
        LinearProgressIndicator(progress = { position.value / total },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp).height(8.dp).clearAndSetSemantics {},
            drawStopIndicator = {})
        Canvas(Modifier.offset {
            val hop = sin(PI * (position.value % 1f)).toFloat()
            IntOffset((travel.toPx() * position.value / total).roundToInt(), (24.dp.toPx() - 20.dp.toPx() * hop).roundToInt())
        }.size(32.dp).testTag("progress-icon").semantics {
            contentDescription = "${icon.label} progress icon"
        }) { drawProgressIcon(icon) }
    }
}

@Composable
private fun TimerText(model: MathViewModel) {
    var elapsed by remember { mutableLongStateOf(model.elapsedMs()) }
    LaunchedEffect(model) {
        while (true) {
            elapsed = model.elapsedMs()
            delay(250)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(4.dp))
        val limit = model.activeTimeLimitMs
        Text(if (limit == null) formatDuration(elapsed) else "Left: ${formatDuration(limit - elapsed)}",
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("timer"))
    }
}

/** Shows a practice result: the score and every answered problem with a check or an X. */
@Composable
internal fun ResultsContent(result: PracticeResult, title: String?, showCorrectAnswers: Boolean, modifier: Modifier = Modifier,
    verticalEquations: Boolean = false, actions: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    title?.let { Text(it, style = MaterialTheme.typography.headlineMedium) }
                    Text("You got ${result.correct} right!", style = MaterialTheme.typography.titleLarge,
                        color = CorrectGreen, modifier = Modifier.testTag("result-correct"))
                    Text("${result.operation.label} · ${result.numberDescription}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Wrong: ${result.wrong} · Time: ${formatDuration(result.durationMs)}",
                        style = MaterialTheme.typography.bodyMedium)
                    if (result.timedOut) Text("Time's up!", color = WrongRed)
                    result.pokemonReward?.let {
                        Text("My Pokémons: ${it.collectionLabel}", style = MaterialTheme.typography.titleMedium)
                    }
                    result.prize?.let { prize ->
                        RewardImage(prize.type, fragment = true, modifier = Modifier.size(88.dp))
                        Text("Prize: ${prize.type.fragmentLabel}", style = MaterialTheme.typography.titleMedium)
                        Text("Total collected: ${prize.balance.totalFragments} fragments",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("${prize.balance.whole} ${prize.type.pluralLabel} · ${prize.balance.fragments}/3 fragments",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.prizeType != null && result.prize == null) {
                        Text("Your prize is waiting!", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(formatFinishedAt(result.finishedAt), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            itemsIndexed(result.attempts) { index, attempt -> AttemptRow(index + 1, attempt, showCorrectAnswers, verticalEquations) }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { actions() }
    }
}

@Composable
private fun AttemptRow(number: Int, attempt: Attempt, showCorrectAnswers: Boolean, verticalEquations: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (attempt.correct) Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct", tint = CorrectGreen)
            else Icon(Icons.Rounded.Cancel, contentDescription = "Wrong", tint = WrongRed)
            Text("$number.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Equation(attempt.problem, attempt.given.toString(), verticalEquations, style = MaterialTheme.typography.titleMedium)
                if (!attempt.correct && showCorrectAnswers) Text("Correct answer: ${attempt.problem.answer}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
