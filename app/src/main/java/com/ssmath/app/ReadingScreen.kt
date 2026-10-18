package com.ssmath.app

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private tailrec fun Context.readingActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.readingActivity()
    else -> null
}

@Composable
internal fun rememberReadingViewModel(): ReadingViewModel {
    val activity = requireNotNull(LocalContext.current.readingActivity())
    return viewModel(viewModelStoreOwner = activity)
}

@Composable
internal fun AcademyWelcome(model: ReadingViewModel) {
    val data = model.data ?: return
    var deferredForStorageError by rememberSaveable { mutableStateOf(false) }
    if (data.profile != null || (deferredForStorageError && model.storageError != null)) return
    var name by rememberSaveable { mutableStateOf("") }
    val validName = name.trim().let { it.length in 1..40 && it.none(Char::isISOControl) }
    Dialog(onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false,
            usePlatformDefaultWidth = false)) {
        Card(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(16.dp).imePadding().testTag("academy-welcome")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Welcome to the Academy! What is your name?", style = MaterialTheme.typography.headlineSmall)
                if (model.storageError != null) {
                    Text(requireNotNull(model.storageError))
                    TextButton(onClick = model::retry) { Text("Retry storage") }
                    TextButton(onClick = { deferredForStorageError = true }) { Text("Continue without reading") }
                } else {
                    Text("One name for all your games, saved only on this device.")
                    OutlinedTextField(name, { name = it.take(40) }, label = { Text("Your name") },
                        singleLine = true, enabled = !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("academy-name"))
                    model.message?.let { Text(it) }
                    Button(onClick = { model.setName(name) },
                        enabled = validName && !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("academy-save-name")) { Text("Let's play") }
                }
            }
        }
    }
}

@Composable
fun ReadingScreen(onExit: () -> Unit, textSizePercent: Int, onTextSizeChange: (Int) -> Unit,
                  disabledGames: Set<ReadingGame> = emptySet(),
                  onRewards: () -> Unit = {}, onPokemons: () -> Unit = {},
                  appModel: MathViewModel? = null) {
    val model = rememberReadingViewModel()
    val speaker = rememberOfflineReadingSpeaker()
    ReadingModule(model, onExit, textSizePercent, onTextSizeChange,
        speaker.available, speaker::speak, speaker::stop, disabledGames, onRewards, onPokemons, appModel)
}

@Composable
internal fun ReadingProgressScreen(appModel: MathViewModel) {
    val model = rememberReadingViewModel()
    val speaker = rememberOfflineReadingSpeaker()
    BackHandler(onBack = appModel::closeOverlay)
    val data = model.data
    if (data?.profile != null && model.storageError == null) {
        ReadingProgress(data, model, appModel.textSizePercent, appModel::chooseTextSize,
            speaker.available, speaker::speak, speaker::stop, appModel::closeOverlay,
            appModel.disabledReadingGames, backLabel = "Back to settings",
            onOpenAttempt = { attempt ->
                speaker.stop()
                model.act { ReadingEngine.resume(it, attempt.id) }
                appModel.openReadingFromProgress()
            })
    } else ReadingColumn("reading-progress-loading") {
        Text(model.storageError ?: if (data == null) "Opening your private reading shelf…" else "Enter your name to get started.")
        if (model.storageError != null) ReadingButton("Retry storage", "reading-retry", action = model::retry)
        ReadingButton("Back to settings", "reading-back", action = appModel::closeOverlay)
    }
}

@Composable
fun ReadingHistoryScreen(appModel: MathViewModel? = null) {
    ReadingHistoryContent(rememberReadingViewModel(), appModel)
}

@Composable
internal fun ReadingHistoryContent(model: ReadingViewModel, appModel: MathViewModel? = null) {
    val data = model.data
    var readerId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteAttemptId by rememberSaveable { mutableStateOf<String?>(null) }
    var clearReaderId by rememberSaveable { mutableStateOf<String?>(null) }
    ReadingColumn("reading-practice-history-screen") {
        appModel?.readingHistoryError?.let { error ->
            Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            ReadingButton("Retry reward cleanup", "reading-history-retry-rewards") {
                data?.deletedSessionIds?.let(appModel::deleteReadingResults)
            }
        }
        if (model.storageError != null) {
            Text(requireNotNull(model.storageError), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            ReadingButton("Retry storage", "reading-retry", action = model::retry)
        } else if (data == null) {
            CircularProgressIndicator()
            Text("Opening your private reading shelf…")
        } else if (data.profiles.isEmpty()) {
            Text("No reading practice results yet. Play a reading game to get started.")
        } else {
            val profile = data.profiles.find { it.id == readerId } ?: data.profile ?: data.profiles.first()
            if (data.profiles.size > 1) {
                Text("Saved reader history")
                data.profiles.forEach { reader ->
                    ReadingChoice(reader.name, "reading-history-reader-${reader.id}", reader.id == profile.id) {
                        readerId = reader.id
                    }
                }
            }
            Text("Reader: ${profile.name}. Only this reader's history is shown.")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Finished activities: ${data.completions(profile.id)}", Modifier.weight(1f))
                if (data.attemptsFor(profile.id).isNotEmpty()) {
                    IconButton(onClick = { clearReaderId = profile.id }, enabled = !model.saving,
                        modifier = Modifier.testTag("reading-clear-history")) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history")
                    }
                }
            }
            data.sessions.filter { it.ownerId == profile.id }.asReversed().forEach { session ->
                Card(Modifier.fillMaxWidth().testTag("reading-session-history-${session.id}")) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${session.game.title} · ${session.questionCount} questions")
                        Text("Correct: ${data.sessionCorrect(session)} · Wrong: ${data.sessionWrong(session)}")
                        Text(session.finishedAt?.let { formatFinishedAt(it) } ?:
                            if (session.abandoned) "Archived game" else "In progress")
                        ReadingReviewButton(data, model, session.attemptIds, "reading-review-session-${session.id}")
                        IconButton(onClick = { deleteAttemptId = session.attemptIds.first() }, enabled = !model.saving,
                            modifier = Modifier.testTag("reading-delete-session-${session.id}")) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete result")
                        }
                        if (session.prizeType != null && appModel != null) {
                            TextButton(onClick = { appModel.showReadingRewardForSession(data, session) }) {
                                Text("View gift")
                            }
                        }
                    }
                }
            }
            ReadingHistoryEntries(data, profile.id, reviewModel = model, deletionEnabled = !model.saving,
                onDelete = { deleteAttemptId = it.id })
        }
    }
    val deleting = data?.attempts?.find { it.id == deleteAttemptId }
    val clearing = data?.profiles?.find { it.id == clearReaderId }
    if (deleting != null || clearing != null) {
        val session = data?.sessions?.find { deleting?.id in it.attemptIds }
        SettingsDialog(onDismissRequest = { deleteAttemptId = null; clearReaderId = null },
            title = { Text(if (clearing != null) "Clear reading history?" else "Delete this result?") },
            text = {
                Text((if (clearing != null) "All reading history for ${clearing.name}" else if (session != null)
                    "This entire ${session.questionCount}-question game" else "This activity") +
                    " and its saved answers, drafts, stories, progress, earned reward fragments and unshared " +
                    "My Pokémons rewards will be permanently removed. " +
                    "Other history is kept. Used rewards cannot be recovered.")
            },
            confirmButton = {
                TextButton(enabled = !model.saving && model.storageError == null, onClick = {
                    if (clearing != null) model.clearHistory(clearing.id)
                    else deleting?.let { model.deleteHistory(setOf(it.id)) }
                    deleteAttemptId = null
                    clearReaderId = null
                }) { Text(if (clearing != null) "Clear" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteAttemptId = null; clearReaderId = null }) { Text("Cancel") }
            })
    }
}

@Composable
internal fun ReadingModule(
    model: ReadingViewModel,
    onExit: () -> Unit,
    textSizePercent: Int,
    onTextSizeChange: (Int) -> Unit,
    narrationAvailable: Boolean = false,
    speak: (String) -> Boolean = { false },
    stop: () -> Unit = {},
    disabledGames: Set<ReadingGame> = emptySet(),
    onRewards: () -> Unit = {},
    onPokemons: () -> Unit = {},
    appModel: MathViewModel? = null
) {
    val data = model.data
    var failedNavigationToMenu by remember { mutableStateOf(false) }
    val disabledDraft = data?.active?.let {
        it.game in disabledGames && (it.open ||
            data.activeSession?.let { session -> session.finishedAt == null && !session.abandoned } == true)
    } == true
    val page = if (failedNavigationToMenu || data?.page == ReadingPage.PROFILES ||
        (data?.page == ReadingPage.ACTIVITY && disabledDraft))
        ReadingPage.MENU else data?.page ?: ReadingPage.MENU
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(model, lifecycle, page, disabledDraft) {
        fun update() = model.setSessionForeground(page == ReadingPage.ACTIVITY && !disabledDraft &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(observer)
        update()
        onDispose {
            lifecycle.removeObserver(observer)
            model.setSessionForeground(false)
        }
    }
    val back: () -> Unit = {
        stop()
        if (page == ReadingPage.MENU) onExit()
        else if (model.storageError != null) failedNavigationToMenu = true
        else model.act { it.copy(page = ReadingPage.MENU) }
    }
    BackHandler(onBack = back)
    DisposableEffect(data?.selectedProfileId, page, data?.active?.id) {
        onDispose { stop() }
    }
    if (data == null || model.storageError != null) {
        ReadingColumn("reading-loading") {
            ReadingHeading("Reading")
            if (model.storageError != null) {
                Text(requireNotNull(model.storageError), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                ReadingButton("Retry storage", "reading-retry") { failedNavigationToMenu = false; model.retry() }
            } else {
                CircularProgressIndicator()
                Text("Opening your private reading shelf…")
            }
            ReadingButton(if (page == ReadingPage.MENU) "Back to learning menu" else "Back to reading menu",
                "reading-back", action = back)
        }
        return
    }
    val profile = data.profile
    when (page) {
        ReadingPage.MENU, ReadingPage.PROFILES -> ReadingMenu(data, model, onExit, stop, disabledGames,
            onRewards, onPokemons, appModel?.rewardThresholds ?: RewardThresholds())
        ReadingPage.PROGRESS -> ReadingProgress(data, model, textSizePercent, onTextSizeChange,
            narrationAvailable, speak, stop, back, disabledGames)
        ReadingPage.ACTIVITY -> {
            val attempt = data.active
            if (attempt == null || profile == null) ReadingColumn("reading-empty") {
                Text("Choose an activity to begin.")
                ReadingButton("Back to reading menu", "reading-back", action = back)
            } else {
                val session = data.activeSession
                if (session?.finishedAt != null) {
                    ReadingSessionSummary(data, model, session, appModel, back)
                } else if (session != null && attempt.finishedAt != null) {
                    LaunchedEffect(attempt.id) {
                        stop()
                        model.nextSessionActivity()
                    }
                } else ReadingActivityScreen(data, model, attempt, narrationAvailable, speak, stop, back,
                    attempt.game !in disabledGames, appModel?.showTimer == true)
            }
        }
    }
}

@Composable
private fun ReadingColumn(tag: String, modifier: Modifier = Modifier,
                          scrollState: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().testTag(tag).verticalScroll(scrollState)
        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun ReadingHeading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun ReadingButton(label: String, tag: String, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag)) {
        Text(label, Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun ReadingChoice(label: String, tag: String, selected: Boolean = false,
                          enabled: Boolean = true, modifier: Modifier = Modifier,
                          colors: ButtonColors = ButtonDefaults.outlinedButtonColors(), action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, colors = colors, modifier = modifier.fillMaxWidth()
        .heightIn(min = 56.dp).testTag(tag).semantics {
            stateDescription = if (selected) "Selected" else "Not selected"
        }) {
        Text(if (selected) "✓ $label" else label, Modifier.padding(vertical = 4.dp))
    }
}

private val ReadingChoiceColors = listOf(
    Color(0xFFD5E8FF), Color(0xFFD6F4DF), Color(0xFFFFE7AC), Color(0xFFF5D7F2)
)

@Composable
private fun ReadingChoiceGrid(choices: List<String>, tagPrefix: String,
                              selected: (Int) -> Boolean = { false }, enabled: Boolean = true,
                              indices: List<Int> = choices.indices.toList(),
                              pictures: List<ReadingPicture> = emptyList(),
                              tileModifier: (Int) -> Modifier = { Modifier }, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        indices.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { index ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (index in pictures.indices) ReadingPictureView(pictures[index])
                        ReadingChoice(choices[index], "$tagPrefix-$index", selected(index), enabled,
                            modifier = tileModifier(index),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ReadingChoiceColors[index % ReadingChoiceColors.size],
                                contentColor = Color(0xFF18212F))) {
                            onChoose(index)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReadingPictureView(picture: ReadingPicture) {
    Text(picture.symbol, style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.clearAndSetSemantics { contentDescription = picture.description })
    Text(picture.description, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.clearAndSetSemantics {})
}

@Composable
private fun ReadingMenu(data: ReadingData, model: ReadingViewModel, onExit: () -> Unit, stop: () -> Unit,
                        disabledGames: Set<ReadingGame>, onRewards: () -> Unit, onPokemons: () -> Unit,
                        thresholds: RewardThresholds) {
    val context = LocalContext.current
    val profile = data.profile
    var questionCount by rememberSaveable(thresholds) { mutableStateOf<Int?>(null) }
    val games = listOf(ReadingGame.MISSING, ReadingGame.SEQUENCE, ReadingGame.STUDIO, ReadingGame.SCRAMBLE,
        ReadingGame.DETECTIVE, ReadingGame.GRAMMAR)
        .filterNot { it in disabledGames }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        CollectionButtons({ stop(); onRewards() }, { stop(); onPokemons() }, "reading")
        Column(Modifier.weight(1f).widthIn(max = 600.dp).fillMaxWidth().testTag("reading-menu")
            .verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReadingHeading("Reading adventures")
            Text("How many questions would you like?", style = MaterialTheme.typography.titleLarge)
            RewardsQuestionCountChoices(questionCount, { questionCount = it }, "reading",
                enabled = !model.saving && !model.importing, thresholds = thresholds)
            model.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (model.packStatus?.counts?.values?.sum() == 0)
                Text("Import reading activities in Settings > Admin Menu > Game Settings > Reading Settings to begin. Saved drafts can still be resumed.")
            if (games.isEmpty()) Text("No reading games are active. Enable games in Settings > Admin Menu > Game Settings > Reading Settings > Active Reading Games.")
            if (profile == null) Text("Enter your name at the welcome screen to save your progress.")
            else {
                data.active?.takeIf { it.game !in disabledGames && !it.abandoned &&
                    (it.open || data.activeSession?.let { session -> session.finishedAt == null && !session.abandoned } == true)
                }?.let { draft ->
                    ReadingChoice("Resume ${draft.game.title}", "reading-resume-${draft.game.name}",
                        enabled = !model.saving && !model.importing) {
                        stop(); model.act { ReadingEngine.resume(it, draft.id) }
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val labelSize = with(LocalDensity.current) { MaterialTheme.typography.titleMedium.fontSize.toDp() }
                    val columns = if (maxWidth < 320.dp || labelSize > 22.dp) 1 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        games.chunked(columns).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { game ->
                                    SubjectButton(game.title, game.backgroundImage, {
                                       val count = questionCount
                                       if (count == null) {
                                           Toast.makeText(context, "Please select the number of questions.",
                                               Toast.LENGTH_SHORT).show()
                                       } else {
                                           stop()
                                           model.startSession(game, count)
                                       }
                                    }, Modifier.weight(1f), "reading-game-${game.name}",
                                        enabled = !model.saving && !model.importing)
                                }
                                if (row.size < columns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            ReadingButton("Back to learning menu", "reading-exit") { stop(); onExit() }
        }
    }
}

private val ReadingGame.backgroundImage: Int
    get() = when (this) {
        ReadingGame.MISSING -> R.drawable.reading_missing
        ReadingGame.SEQUENCE -> R.drawable.reading_sequence
        ReadingGame.STUDIO -> R.drawable.reading_studio
        ReadingGame.SCRAMBLE -> R.drawable.reading_scramble
        ReadingGame.DETECTIVE -> R.drawable.reading_detective
        ReadingGame.GRAMMAR -> R.drawable.subject_reading
    }

@Composable
private fun ReadingListen(text: String, available: Boolean, speak: (String) -> Boolean,
                          stop: () -> Unit, onSuccess: () -> Unit) {
    var unavailable by remember(text) { mutableStateOf(false) }
    ReadingChoice("Listen to the reading", "reading-listen", enabled = available && text.isNotBlank()) {
        if (speak(text)) { unavailable = false; onSuccess() } else unavailable = true
    }
    ReadingChoice("Stop narration", "reading-stop", action = stop)
    if (!available || unavailable) Text("Offline narration is unavailable. Reading and every activity still work without it.")
    Text("Listening is support, not independent reading.")
}

@Composable
private fun ReadingActivityScreen(data: ReadingData, model: ReadingViewModel, a: ReadingAttempt,
                                  available: Boolean, speak: (String) -> Boolean, stop: () -> Unit, back: () -> Unit,
                                  gameEnabled: Boolean, showTimer: Boolean = false) {
    if (a.finishedAt != null) {
        ReadingSummary(data, model, a, available, speak, stop, back, gameEnabled)
        return
    }
    val content = a.content
    var instructionsUnavailable by remember(a.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(a.id) { scrollState.scrollTo(0) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    ReadingColumn("reading-activity", Modifier.onGloballyPositioned { viewport = it.boundsInRoot() }, scrollState) {
        Button(onClick = back, modifier = Modifier.heightIn(min = 56.dp).testTag("reading-back")) {
            Text("Back to reading menu", Modifier.padding(vertical = 4.dp))
        }
        ReadingHeading(content.game.title)
        data.activeSession?.let { session ->
            Text("Question ${session.attemptIds.indexOf(a.id) + 1} of ${session.questionCount}",
                Modifier.testTag("reading-question-progress"))
            Text("Correct: ${data.sessionCorrect(session)} · Wrong: ${data.sessionWrong(session)}")
            ReadingSessionTimer(model, showTimer)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(content.game.guidance, Modifier.weight(1f))
            IconButton(onClick = { instructionsUnavailable = !speak(content.game.guidance) },
                enabled = available, modifier = Modifier.size(48.dp).testTag("reading-instructions")) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen to instructions")
            }
        }
        if (instructionsUnavailable) Text("Instruction audio is unavailable. You can still read the instructions above.")
        Column(Modifier.fillMaxWidth().testTag("reading-problem")
            .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(content.title, style = MaterialTheme.typography.titleLarge)
            ReadingPictureView(content.picture)
            when (content.game) {
                ReadingGame.MISSING, ReadingGame.GRAMMAR -> {
                    content.passage.forEach { Text(it, style = MaterialTheme.typography.headlineSmall) }
                    ReadingQuestionView(a, model, "answer")
                }
                ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
                    if (content.game == ReadingGame.SCRAMBLE) SentenceScrambleTiles(a, model, scrollState, viewport)
                    else ReadingTiles(a, model, content.tiles, content.eventPictures)
                    if (!a.earned("order")) ReadingButton("Submit Answer", "reading-submit-order",
                        !model.saving && a.orderComplete) {
                        model.submit("order")
                    }
                    if (a.earned("order")) {
                        Text("Order checkpoint saved")
                        if (content.game == ReadingGame.SEQUENCE) {
                            ReadingQuestionView(a, model, "answer")
                        }
                    }
                }
                ReadingGame.DETECTIVE -> {
                    Text("The passage · each button is one exact evidence segment")
                    content.passage.forEachIndexed { index, sentence ->
                        ReadingChoice(sentence, "reading-evidence-$index", a.fields["evidence"] == index.toString(),
                            a.earned("answer") && !a.earned("evidence")) {
                            model.act { ReadingEngine.field(it, "evidence", index.toString()) }
                        }
                    }
                    ReadingQuestionView(a, model, "answer")
                    if (a.earned("answer") && !a.earned("evidence")) {
                        Text("Now tap the sentence that proves your answer.")
                        ReadingButton("Submit Answer", "reading-submit-evidence",
                            !model.saving && a.fields["evidence"] != null) {
                            model.submit("evidence")
                        }
                    }
                }
                ReadingGame.STUDIO -> ReadingStudio(a, model)
            }
        }
        if (a.feedback.isNotBlank()) Text(a.feedback,
            Modifier.testTag("reading-feedback").semantics { liveRegion = LiveRegionMode.Polite })
        val earned = a.checkpoints.values.count { it.earned }
        Text("Saved checkpoints: $earned · Hints: ${a.hints} · Narrations: ${a.narrations}")
        if (ReadingEngine.ready(a)) ReadingButton("Submit Answer", "reading-submit", enabled = !model.saving) {
            stop(); model.finishActivity()
        }
        ReadingChoice("Give me a hint", "reading-hint") { model.act { ReadingEngine.hint(it) } }
        data.activeSession?.let { ReadingPreviousAnswer(data, it) }
    }
}

@Composable
private fun ReadingQuestionView(a: ReadingAttempt, model: ReadingViewModel, key: String) {
    val question = ReadingEngine.question(a) ?: return
    Text(question.prompt, style = MaterialTheme.typography.titleLarge)
    ReadingChoiceGrid(question.choices, "reading-answer",
        selected = { a.fields["answer"] == it.toString() }, enabled = !a.earned(key)) { index ->
        model.act { ReadingEngine.field(it, "answer", index.toString()) }
    }
    if (!a.earned(key)) ReadingButton("Submit Answer", "reading-submit-$key",
        !model.saving && a.fields["answer"] != null) {
        model.submit(key)
    }
    if (a.earned(key)) Text("Answer checkpoint saved")
}

private class SentenceDragState {
    var tile by mutableStateOf<Int?>(null)
    var position by mutableStateOf(Offset.Zero)
    val sources = mutableMapOf<Int, LayoutCoordinates>()
    val spaces = mutableMapOf<Int, LayoutCoordinates>()
    var bank: LayoutCoordinates? = null
    var board: LayoutCoordinates? = null

    fun spaceAt(point: Offset): Int? = spaces.entries.firstOrNull {
        it.value.isAttached && it.value.boundsInRoot().contains(point)
    }?.key

    fun overBank(point: Offset) = bank?.let { it.isAttached && it.boundsInRoot().contains(point) } == true
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceScrambleTiles(a: ReadingAttempt, model: ReadingViewModel,
                                  scrollState: ScrollState, viewport: Rect) {
    val tiles = a.content.tiles
    val locked = a.earned("order")
    val drag = remember(a.id, locked) { SentenceDragState() }
    val drop by rememberUpdatedState<(Int, Offset) -> Unit> { tile, point ->
        val space = drag.spaceAt(point)
        if (space != null) model.act { ReadingEngine.placeTileInSpace(it, tile, space) }
        else if (drag.overBank(point)) model.act { ReadingEngine.removeTile(it, tile) }
    }
    val density = LocalDensity.current
    val edge = with(density) { 48.dp.toPx() }
    val speed = with(density) { 360.dp.toPx() }
    val currentViewport by rememberUpdatedState(viewport)
    LaunchedEffect(drag.tile) {
        if (drag.tile == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (drag.tile != null) {
            val frame = withFrameNanos { it }
            val bounds = currentViewport
            val point = drag.position
            val direction = when {
                point.x !in bounds.left..bounds.right -> 0f
                point.y < bounds.top + edge -> -1f
                point.y > bounds.bottom - edge -> 1f
                else -> 0f
            }
            scrollState.scrollBy(direction * speed * ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.05f))
            previousFrame = frame
        }
    }
    fun draggable(tile: Int): Modifier = Modifier
        .onGloballyPositioned { drag.sources[tile] = it }
        .alpha(if (drag.tile == tile) 0.35f else 1f)
        .pointerInput(a.id, tile, locked) {
            if (!locked) detectDragGesturesAfterLongPress(
                onDragStart = { local ->
                    drag.sources[tile]?.takeIf { it.isAttached }?.let {
                        drag.position = it.positionInRoot() + local
                        drag.tile = tile
                    }
                },
                onDrag = { change, amount ->
                    change.consume()
                    drag.position += amount
                },
                onDragEnd = {
                    val dragged = drag.tile
                    drag.tile = null
                    if (dragged != null) drop(dragged, drag.position)
                },
                onDragCancel = { drag.tile = null }
            )
        }
    Box(Modifier.fillMaxWidth().onGloballyPositioned { drag.board = it }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your sentence", style = MaterialTheme.typography.titleLarge)
            FlowRow(Modifier.fillMaxWidth().testTag("reading-sentence"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.indices.forEach { position ->
                    val tile = a.order.getOrNull(position)?.takeIf { it >= 0 }
                    val highlighted = drag.tile != null && tile == null && drag.spaceAt(drag.position) == position
                    val color = if (tile == null) MaterialTheme.colorScheme.surface
                        else ReadingChoiceColors[tile % ReadingChoiceColors.size]
                    Box(Modifier.onGloballyPositioned { drag.spaces[position] = it }
                        .testTag("reading-space-$position")) {
                        OutlinedButton(onClick = { if (tile != null) model.act { ReadingEngine.removeTile(it, tile) } },
                            enabled = !locked,
                            modifier = Modifier.widthIn(min = 88.dp).heightIn(min = 56.dp)
                                .then(if (tile == null) Modifier else draggable(tile))
                                .then(if (tile == null) Modifier else Modifier.testTag("reading-placed-$position"))
                                .semantics {
                                    contentDescription = if (tile == null) "Empty sentence space ${position + 1}"
                                        else "Sentence space ${position + 1}: ${tiles[tile]}. Tap to remove."
                                },
                            border = BorderStroke(if (highlighted) 3.dp else 1.dp,
                                if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = color,
                                contentColor = if (tile == null) MaterialTheme.colorScheme.onSurface else Color(0xFF18212F))) {
                            Text(tile?.let { tiles[it] } ?: "___", Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().testTag("reading-tile-bank")
                .onGloballyPositioned { drag.bank = it },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(if (drag.tile != null && drag.overBank(drag.position)) 3.dp else 1.dp,
                    MaterialTheme.colorScheme.outline)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Available tiles")
                    ReadingChoiceGrid(tiles, "reading-tile", enabled = !locked,
                        indices = tiles.indices.filterNot { it in a.order }, tileModifier = ::draggable) { index ->
                        model.act { ReadingEngine.placeTile(it, index) }
                    }
                    Text("Drop tiles here to remove them.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        drag.tile?.let { tile ->
            Surface(Modifier.offset {
                (drag.position - (drag.board?.positionInRoot() ?: Offset.Zero) - Offset(edge, edge * 1.5f)).round()
            }.widthIn(max = 240.dp).clearAndSetSemantics {}.testTag("reading-drag-preview"),
                shape = MaterialTheme.shapes.small, shadowElevation = 8.dp,
                color = ReadingChoiceColors[tile % ReadingChoiceColors.size], contentColor = Color(0xFF18212F)) {
                Text(tiles[tile], Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun ReadingTiles(a: ReadingAttempt, model: ReadingViewModel, tiles: List<String>,
                         pictures: List<ReadingPicture> = emptyList()) {
    val locked = a.earned("order")
    Text("Your order", style = MaterialTheme.typography.titleLarge)
    if (a.order.isEmpty()) Text("Tap the tiles below to begin.")
    a.order.forEachIndexed { position, tile ->
        ReadingChoice("${position + 1}. ${tiles[tile]} · tap to remove", "reading-placed-$position",
            enabled = !locked) { model.act { ReadingEngine.removeTile(it, tile) } }
    }
    Text("Available tiles")
    ReadingChoiceGrid(tiles, "reading-tile", enabled = !locked,
        indices = tiles.indices.filterNot { it in a.order }, pictures = pictures) { index ->
        model.act { ReadingEngine.placeTile(it, index) }
    }
    Text("To reorder: remove a placed tile, then tap it to add it at the end. No dragging needed.")
}

private data class ReadingEditor(val text: String, val update: (String) -> Unit)

@Composable
private fun rememberReadingEditor(a: ReadingAttempt, key: String, model: ReadingViewModel): ReadingEditor {
    // Presets and typing share one immediate value; an older disk acknowledgement cannot replace it.
    var text by rememberSaveable(a.id, key, model.data?.selectedProfileId) {
        mutableStateOf(a.fields[key].orEmpty())
    }
    val locked = a.game == ReadingGame.STUDIO && a.earned(key)
    androidx.compose.runtime.LaunchedEffect(a.id, key, model.data?.selectedProfileId, locked) {
        if (locked) text = a.fields[key].orEmpty()
    }
    return ReadingEditor(text) { raw ->
        val value = raw.take(500)
        text = value
        model.act { ReadingEngine.field(it, key, value) }
    }
}

@Composable
private fun ReadingDraftText(editor: ReadingEditor, key: String, label: String, enabled: Boolean) {
    OutlinedTextField(editor.text, editor.update, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag("reading-write-$key"))
}

@Composable
private fun ReadingStudio(a: ReadingAttempt, model: ReadingViewModel) {
    ReadingEngine.studioKeys.forEachIndexed { index, key ->
        val unlocked = ReadingEngine.studioKeys.take(index).all { a.earned(it) }
        Text("${index + 1}. ${key.replaceFirstChar { it.uppercase() }} · one checkpoint",
            style = MaterialTheme.typography.titleLarge)
        if (unlocked) {
            val editor = rememberReadingEditor(a, key, model)
            if (!a.earned(key)) {
                val choices = a.content.writingChoices[index]
                ReadingChoiceGrid(choices, "reading-studio-$key", selected = { editor.text == choices[it] }) {
                    editor.update(choices[it])
                }
            }
            ReadingDraftText(editor, key, "Your $key idea", !a.earned(key))
            ReadingButton("Submit Answer", "reading-save-$key",
                !model.saving && !a.earned(key) && !a.fields[key].isNullOrBlank()) {
                model.submit(key)
            }
            if (a.earned(key)) Text("$key saved")
        } else Text("Save the previous part to unlock this checkpoint.")
    }
    if (ReadingEngine.ready(a)) {
        Text("Your finished story", style = MaterialTheme.typography.titleLarge)
        Text(ReadingEngine.storyText(a))
    }
}

@Composable
private fun ReadingSessionTimer(model: ReadingViewModel, showTimer: Boolean) {
    val remaining = model.sessionRemainingMs
    if (remaining != null || showTimer) {
        Text(if (remaining != null) "Time remaining: ${formatDuration(remaining)}"
            else "Time: ${formatDuration(model.sessionElapsedMs)}", Modifier.testTag("reading-timer"))
    }
}

@Composable
private fun ReadingPreviousAnswer(data: ReadingData, session: ReadingSession) {
    val previous = data.attempts.lastOrNull { it.id in session.attemptIds && it.finishedAt != null } ?: return
    if (previous.feedback.isNotBlank()) {
        Text("Previous question · ${previous.title}\n${previous.feedback}",
            Modifier.testTag("reading-previous-answer").semantics { liveRegion = LiveRegionMode.Polite },
            color = if (ReadingEngine.wrong(previous)) WrongRed else CorrectGreen,
            style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ReadingSessionSummary(data: ReadingData, model: ReadingViewModel, session: ReadingSession,
                                  appModel: MathViewModel?, back: () -> Unit) {
    LaunchedEffect(session.id, appModel) { appModel?.openReadingSessionReward(data, session) }
    DisposableEffect(session.id, appModel) {
        onDispose { appModel?.dismissReadingSessionReward() }
    }
    ReadingColumn("reading-session-summary") {
        ReadingHeading("Results")
        val answered = data.sessionAnswered(session)
        val correct = data.sessionCorrect(session)
        Text(session.game.title)
        Text(when {
            session.abandoned -> "Game archived. You got $correct right out of ${session.questionCount}"
            session.timedOut -> "Time's up! You got $correct right out of ${session.questionCount}"
            answered < session.questionCount -> "Nice try! You got $correct right out of ${session.questionCount}"
            else -> "You got $correct right out of ${session.questionCount}"
        })
        Text("Answered: $answered · Wrong: ${data.sessionWrong(session)}")
        if (answered < session.questionCount && !session.timedOut && !session.abandoned)
            Text("The game ended because more than 10% were wrong.")
        if (appModel != null) {
            appModel.readingRewardError?.let { Text(it) }
            if (appModel.readingRewardError != null || (session.prizeType != null &&
                !appModel.readingSelectingCelebration && appModel.readingCelebration == null)) {
                ReadingButton(if (appModel.readingRewardError != null) "Retry saving rewards" else "View gift",
                    "reading-session-gift") { appModel.showReadingRewardForSession(data, session) }
            }
        }
        ReadingButton("Done", "reading-session-done", enabled = !model.saving, action = back)
        ReadingReviewButton(data, model, session.attemptIds, "reading-review-session-${session.id}")
        ReadingPreviousAnswer(data, session)
    }
}

@Composable
private fun ReadingSummary(data: ReadingData, model: ReadingViewModel, a: ReadingAttempt,
                           available: Boolean, speak: (String) -> Boolean, stop: () -> Unit, back: () -> Unit,
                           gameEnabled: Boolean) {
    ReadingColumn("reading-summary") {
        ReadingPictureView(ReadingPicture("📖 🌱 ✨", "An open book with a growing shoot and sparkling stars"))
        ReadingHeading("Your reading is growing!")
        model.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        Text("Congratulations on finishing ${a.game.title}.")
        val checks = a.checkpoints.values.filter { it.objective && it.tries > 0 }
        if (checks.isNotEmpty()) {
            Text("First-try independent: ${checks.count { it.independent }} / ${checks.size}")
            Text("Completed with support: ${checks.count { it.earned && it.supportedAtCompletion }} · " +
                "Completed after retry: ${checks.count { it.retried }}")
        } else Text("Creative checkpoints celebrate participation, not independent reading accuracy.")
        Text("Hints used: ${a.hints} · Narrations: ${a.narrations}")
        ReadingReviewButton(data, model, listOf(a.id), "reading-review-attempt-${a.id}")
        data.stories.find { it.attemptId == a.id }?.let { story ->
            Text("Saved to your private story shelf", style = MaterialTheme.typography.titleLarge)
            ReadingPictureView(ReadingPicture(story.illustration, "Your story's illustration"))
            Text(story.text, Modifier.testTag("reading-saved-story"))
            ReadingListen(story.text, available, speak, stop) {}
        }
        Text("Your private milestones", style = MaterialTheme.typography.titleLarge)
        data.milestones(requireNotNull(data.selectedProfileId)).forEach { Text("✦ $it") }
        ReadingButton("Choose another game", "reading-next", enabled = gameEnabled) {
            stop(); model.act { it.copy(page = ReadingPage.MENU) }
        }
        ReadingButton("Back to reading menu", "reading-back", action = back)
        if (a.feedback.isNotBlank()) Text(a.feedback, Modifier.testTag("reading-feedback"))
    }
}

@Composable
private fun ReadingProgress(data: ReadingData, model: ReadingViewModel, textSizePercent: Int,
                            onTextSizeChange: (Int) -> Unit, available: Boolean, speak: (String) -> Boolean,
                            stop: () -> Unit, back: () -> Unit, disabledGames: Set<ReadingGame>,
                            backLabel: String = "Back to reading menu",
                            onOpenAttempt: ((ReadingAttempt) -> Unit)? = null) {
    val profile = data.profile ?: return
    ReadingColumn("reading-dashboard") {
        ReadingHeading("Reading Progress")
        Text("Reader: ${profile.name}. Only this reader's history is shown.")
        Text("Finished activities: ${data.completions(profile.id)}")
        val accuracy = data.accuracy(profile.id)
        Text("First-try independent accuracy: ${accuracy.firstIndependent} / ${accuracy.attempted}")
        Text("A first incorrect try counts immediately, even before completion. Hints or successful passage narration " +
            "before a first answer prevent independent credit. " +
            "Creative writing is not an accuracy question.")
        Text("Supported completions: ${accuracy.supportedCompletions} · Retried completions: ${accuracy.retriedCompletions}")
        Text("Support and retry counts can overlap. Eventual success is not called mastery.")
        Text("Text size: $textSizePercent%")
        ReadingChoice("Smaller text", "reading-text-smaller", enabled = textSizePercent > 80) {
            onTextSizeChange((textSizePercent - 10).coerceAtLeast(80))
        }
        ReadingChoice("Larger text", "reading-text-larger", enabled = textSizePercent < 200) {
            onTextSizeChange((textSizePercent + 10).coerceAtMost(200))
        }
        Text("By game and level", style = MaterialTheme.typography.titleLarge)
        ReadingGame.entries.forEach { game ->
            ReadingLevel.entries.forEach { level ->
                val attempts = data.attemptsFor(profile.id).filter { it.game == game && it.level == level }
                if (attempts.isNotEmpty()) {
                    val metric = data.accuracy(profile.id, game, level)
                    Text("${game.title} · ${level.name.lowercase()}: " +
                        "${attempts.count { it.finishedAt != null }} finished; " +
                        "${metric.firstIndependent}/${metric.attempted} first-try independent; " +
                        "${metric.supportedCompletions} supported; ${metric.retriedCompletions} retried")
                }
            }
        }
        Text("Private milestones", style = MaterialTheme.typography.titleLarge)
        if (data.milestones(profile.id).isEmpty()) Text("Your first completed activity will begin this collection.")
        data.milestones(profile.id).forEach { Text("✦ $it") }
        HorizontalDivider()
        ReadingHistoryEntries(data, profile.id, disabledGames) { attempt ->
            if (onOpenAttempt != null) onOpenAttempt(attempt)
            else { stop(); model.act { ReadingEngine.resume(it, attempt.id) } }
        }

        HorizontalDivider()
        Text("Saved story shelf", style = MaterialTheme.typography.titleLarge)
        val stories = data.stories.filter { profile.id in it.readerIds }
        if (stories.isEmpty()) Text("Stories from Story Studio will appear here.")
        stories.asReversed().forEach { story ->
            ReadingPictureView(ReadingPicture(story.illustration, "An illustration for ${story.title}"))
            Text(story.title, style = MaterialTheme.typography.titleLarge)
            Text("By ${story.readerIds.joinToString(" and ") { data.name(it) }} · ${formatFinishedAt(story.savedAt)}")
            Text(story.text)
            ReadingListen(story.text, available, speak, stop) {}
        }
        ReadingButton(backLabel, "reading-back", action = back)
    }
}

@Composable
private fun ReadingHistoryEntries(data: ReadingData, readerId: String, disabledGames: Set<ReadingGame> = emptySet(),
                                  reviewModel: ReadingViewModel? = null,
                                  deletionEnabled: Boolean = true,
                                  onDelete: ((ReadingAttempt) -> Unit)? = null,
                                  onOpen: ((ReadingAttempt) -> Unit)? = null) {
    Text("Dated activity history", style = MaterialTheme.typography.titleLarge)
    val attemptsById = data.attempts.associateBy { it.id }
    val sessionsByAttempt = data.sessions.flatMap { session -> session.attemptIds.map { it to session } }.toMap()
    val nextAttemptIds = data.sessions.filter { it.finishedAt == null && !it.abandoned }.mapNotNull { session ->
        session.attemptIds.firstOrNull { attemptsById[it]?.finishedAt == null }
    }.toSet()
    val attempts = data.attemptsFor(readerId).filter {
        it.id !in sessionsByAttempt || it.finishedAt != null || it.id in nextAttemptIds
    }.asReversed()
    if (attempts.isEmpty()) Text("No reading practice results yet for this reader.")
    attempts.forEach { a ->
        Card(Modifier.fillMaxWidth().testTag("reading-history-entry-${a.id}")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${a.game.title} · ${a.title} · ${a.level.name.lowercase()}")
                Text("Started ${formatFinishedAt(a.startedAt)}")
                Text(a.finishedAt?.let { "Finished ${formatFinishedAt(it)}" } ?:
                    if (a.abandoned) "Archived draft" else "In progress · resumable")
                val checks = a.checkpoints.values.filter { it.readerId == readerId }
                Text("${a.hints} hints · ${a.narrations} narrations")
                val objective = checks.filter { it.objective && it.tries > 0 }
                if (objective.isNotEmpty()) Text("${objective.count { it.independent }}/${objective.size} first-try independent · " +
                    "${objective.count { it.earned && it.supportedAtCompletion }} supported · ${objective.count { it.retried }} retried")
                if (reviewModel != null) ReadingReviewButton(data, reviewModel, listOf(a.id),
                    "reading-review-attempt-${a.id}")
                if (onDelete != null && a.id !in sessionsByAttempt) {
                    IconButton(onClick = { onDelete(a) }, enabled = deletionEnabled,
                        modifier = Modifier.testTag("reading-delete-attempt-${a.id}")) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete result")
                    }
                }
                if (onOpen != null && !a.abandoned) ReadingChoice(
                    if (a.finishedAt != null) "Read summary" else "Resume draft", "reading-history-${a.id}",
                    enabled = a.finishedAt != null || a.game !in disabledGames
                ) { onOpen(a) }
            }
        }
    }
}
