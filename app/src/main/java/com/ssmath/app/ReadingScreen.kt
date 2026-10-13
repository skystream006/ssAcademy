package com.ssmath.app

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private tailrec fun Context.readingActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.readingActivity()
    else -> null
}

@Composable
fun ReadingScreen(onExit: () -> Unit, textSizePercent: Int, onTextSizeChange: (Int) -> Unit,
                  disabledGames: Set<ReadingGame> = emptySet()) {
    val activity = requireNotNull(LocalContext.current.readingActivity())
    val model: ReadingViewModel = viewModel(viewModelStoreOwner = activity)
    val speaker = rememberOfflineReadingSpeaker()
    ReadingModule(model, onExit, textSizePercent, onTextSizeChange,
        speaker.available, speaker::speak, speaker::stop, disabledGames)
}

@Composable
fun ReadingHistoryScreen() {
    val activity = requireNotNull(LocalContext.current.readingActivity())
    val model: ReadingViewModel = viewModel(viewModelStoreOwner = activity)
    ReadingHistoryContent(model)
}

@Composable
internal fun ReadingHistoryContent(model: ReadingViewModel) {
    val data = model.data
    var readerId by rememberSaveable { mutableStateOf<String?>(null) }
    ReadingColumn("reading-practice-history-screen") {
        if (model.storageError != null) {
            Text(requireNotNull(model.storageError), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            ReadingButton("Retry storage", "reading-retry", action = model::retry)
        } else if (data == null) {
            CircularProgressIndicator()
            Text("Opening your private reading shelf…")
        } else if (data.profiles.isEmpty()) {
            Text("No reading practice results yet. Add a reader in Reading to get started.")
        } else {
            val profile = data.profiles.find { it.id == readerId } ?: data.profile ?: data.profiles.first()
            Text("Choose a reader")
            data.profiles.forEach { reader ->
                ReadingChoice(reader.name, "reading-history-reader-${reader.id}", reader.id == profile.id) {
                    readerId = reader.id
                }
            }
            Text("Reader: ${profile.name}. Only this reader's history and shared participation are shown.")
            Text("Individual points: ${data.points(profile.id)} · Finished activities: ${data.completions(profile.id)}")
            ReadingHistoryEntries(data, profile.id)
        }
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
    disabledGames: Set<ReadingGame> = emptySet()
) {
    val data = model.data
    var failedNavigationToMenu by remember { mutableStateOf(false) }
    val disabledDraft = data?.active?.let { it.open && it.content.game in disabledGames } == true
    val page = if (failedNavigationToMenu || (data?.page == ReadingPage.ACTIVITY && disabledDraft))
        ReadingPage.MENU else data?.page ?: ReadingPage.MENU
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
        ReadingPage.MENU -> ReadingMenu(data, model, onExit, stop, disabledGames)
        ReadingPage.PROFILES -> ReadingProfiles(data, model, back, stop)
        ReadingPage.PROGRESS -> ReadingProgress(data, model, textSizePercent, onTextSizeChange,
            narrationAvailable, speak, stop, back, disabledGames)
        ReadingPage.ACTIVITY -> {
            val attempt = data.active
            if (attempt == null || profile == null) ReadingColumn("reading-empty") {
                Text("Choose a reader and an activity to begin.")
                ReadingButton("Back to reading menu", "reading-back", action = back)
            } else ReadingActivityScreen(data, model, attempt, narrationAvailable, speak, stop, back,
                attempt.content.game !in disabledGames)
        }
    }
}

@Composable
private fun ReadingColumn(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().testTag(tag).verticalScroll(rememberScrollState()).padding(16.dp),
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
                          enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth()
        .heightIn(min = 56.dp).testTag(tag).semantics {
            stateDescription = if (selected) "Selected" else "Not selected"
        }) {
        Text(if (selected) "✓ $label" else label, Modifier.padding(vertical = 4.dp))
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
                        disabledGames: Set<ReadingGame>) {
    val profile = data.profile
    var discard by remember { mutableStateOf<ReadingGame?>(null) }
    val recommended = listOf(ReadingGame.MISSING, ReadingGame.SEQUENCE, ReadingGame.STUDIO)
        .filterNot { it in disabledGames }
    val games = recommended + ReadingGame.entries.filterNot { it in recommended || it in disabledGames }
    ReadingColumn("reading-menu") {
        ReadingHeading("Reading adventures")
        Text("Read, imagine, and make stories. Take your time: hints and retries are always welcome.")
        ReadingButton("Readers · ${profile?.name ?: "add your first reader"}", "reading-profiles") {
            stop(); model.act { it.copy(page = ReadingPage.PROFILES) }
        }
        if (games.isEmpty()) Text("No reading games are active. Enable games in Settings > Reading Settings > Active Reading Games.")
        if (profile == null) Text("An adult can add a local reader to save each person's own progress.")
        else {
            Text("Hello, ${profile.name}! ${data.points(profile.id)} reading points")
            Text("Choose your reading level")
            ReadingLevel.entries.forEach { level ->
                ReadingChoice(level.label, "reading-level-${level.name}", profile.level == level) {
                    model.act { ReadingEngine.level(it, level) }
                }
            }
            if (recommended.isNotEmpty()) Text(
                if (recommended.size == 3) "Start here · three recommended games" else "Start here · recommended games",
                style = MaterialTheme.typography.titleLarge)
            games.forEachIndexed { index, game ->
                if (index == recommended.size) Text("More ways to read and create", style = MaterialTheme.typography.titleLarge)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(game.title, style = MaterialTheme.typography.titleLarge)
                        Text(game.guidance)
                        val draft = ReadingEngine.draft(data, game, profile.level)
                        ReadingButton(if (draft != null) "Resume ${game.title}" else "Play ${game.title}",
                            "reading-game-${game.name}") {
                            stop(); model.act { ReadingEngine.start(it, game, requireNotNull(it.profile).level) }
                        }
                        if (draft != null) ReadingChoice("Start a new attempt instead", "reading-new-${game.name}") {
                            discard = game
                        }
                    }
                }
            }
            ReadingButton("For grown-ups · private progress", "reading-progress") {
                stop(); model.act { it.copy(page = ReadingPage.PROGRESS) }
            }
            Text("Private on this device. No leaderboard, timer, or lost points.")
        }
        ReadingButton("Back to learning menu", "reading-exit") { stop(); onExit() }
    }
    discard?.let { game ->
        AlertDialog(onDismissRequest = { discard = null }, title = { Text("Start a different attempt?") },
            text = { Text("The current ${game.title} draft will be archived. Its earned points and first tries stay in progress. " +
                "A new attempt starts a different activity. A shared draft is archived for both partners.") },
            confirmButton = {
                ReadingButton("Archive draft and start new", "reading-confirm-new") {
                    discard = null
                    stop()
                    model.act { ReadingEngine.start(it, game, requireNotNull(it.profile).level, newAttempt = true) }
                }
            }, dismissButton = {
                ReadingChoice("Keep my draft", "reading-keep-draft") { discard = null }
            })
    }
}

@Composable
private fun ReadingProfiles(data: ReadingData, model: ReadingViewModel, back: () -> Unit, stop: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    ReadingColumn("reading-profiles-screen") {
        ReadingHeading("Local readers")
        Text("Each reader has a private reading shelf. Use a nickname; no account is needed.")
        data.profiles.forEach { profile ->
            ReadingChoice("Read as ${profile.name}", "reading-profile-${profile.id}",
                data.selectedProfileId == profile.id) {
                stop(); model.act { ReadingEngine.selectProfile(it, profile.id) }
            }
        }
        OutlinedTextField(name, { name = it.take(40) }, label = { Text("Reader name") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("reading-reader-name"))
        model.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        ReadingButton("Add reader", "reading-add-reader", name.trim().isNotEmpty()) {
            stop(); model.act { ReadingEngine.addProfile(it, name) }
        }
        ReadingButton("Back to reading menu", "reading-back", action = back)
    }
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
    Text("Listening is support, not independent reading. Your points are unchanged.")
}

@Composable
private fun ReadingActivityScreen(data: ReadingData, model: ReadingViewModel, a: ReadingAttempt,
                                  available: Boolean, speak: (String) -> Boolean, stop: () -> Unit, back: () -> Unit,
                                  gameEnabled: Boolean) {
    if (a.finishedAt != null) {
        ReadingSummary(data, model, a, available, speak, stop, back, gameEnabled)
        return
    }
    val content = a.content
    var instructionsUnavailable by remember(a.id) { mutableStateOf(false) }
    ReadingColumn("reading-activity") {
        ReadingHeading(content.game.title)
        Text("${data.name(a.ownerId)} · ${content.level.label}")
        Text(content.title, style = MaterialTheme.typography.titleLarge)
        ReadingPictureView(content.picture)
        Text(content.game.guidance)
        ReadingChoice("Listen to instructions", "reading-instructions", enabled = available) {
            instructionsUnavailable = !speak(content.game.guidance)
        }
        if (instructionsUnavailable) Text("Instruction audio is unavailable. You can still read the instructions above.")
        Text("Instruction audio does not count as passage narration.")
        Text("Draft saved automatically. +1 per checkpoint when Reading Rewards System is on; hints and retries never remove points.")
        when (content.game) {
            ReadingGame.MISSING, ReadingGame.CAPTION -> {
                content.passage.forEach { Text(it, style = MaterialTheme.typography.headlineSmall) }
                ReadingQuestionView(a, model, "answer")
            }
            ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
                ReadingTiles(a, model, content.tiles, content.eventPictures)
                ReadingButton("Check order", "reading-check-order", !a.earned("order") && a.order.size == content.tiles.size) {
                    model.submit("order")
                }
                if (a.earned("order")) {
                    Text(if (a.checkpoints["order"]?.awardedPoint == true)
                        "Order checkpoint earned · +1" else "Order checkpoint saved")
                    if (content.game == ReadingGame.SCRAMBLE) Text("Optional bonus · answer for an extra checkpoint, or finish now.")
                    ReadingQuestionView(a, model, if (content.game == ReadingGame.SCRAMBLE) "bonus" else "answer")
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
                if (a.earned("answer")) {
                    Text("Now tap the sentence that proves your answer.")
                    ReadingButton("Check evidence", "reading-check-evidence",
                        !a.earned("evidence") && a.fields["evidence"] != null) {
                        model.submit("evidence")
                    }
                }
            }
            ReadingGame.CHAPTER -> {
                content.passage.forEach { Text(it) }
                Text("Choose a chapter. Both choices are welcome; choosing earns no point. " +
                    "Your branch stays fixed for this attempt.")
                content.branches.forEachIndexed { index, branch ->
                    ReadingChoice(branch.label, "reading-branch-$index", a.branch == index, a.branch == null) {
                        stop(); model.act { ReadingEngine.branch(it, index) }
                    }
                }
                a.branch?.let { index ->
                    Text(content.branches[index].text, style = MaterialTheme.typography.bodyLarge)
                    ReadingQuestionView(a, model, "answer")
                }
            }
            ReadingGame.STUDIO -> ReadingStudio(a, model)
            ReadingGame.PARTNERS -> ReadingPartners(data, a, model, stop)
        }
        if (a.feedback.isNotBlank()) Text(a.feedback,
            Modifier.testTag("reading-feedback").semantics { liveRegion = LiveRegionMode.Polite })
        val earned = a.checkpoints.values.count { it.earned && !it.shared }
        Text("Saved checkpoints: $earned · Hints: ${a.hints} · Narrations: ${a.narrations}")
        if (ReadingEngine.ready(a)) ReadingButton("Finish and save", "reading-finish") {
            stop(); model.act { ReadingEngine.finish(it) }
        }
        ReadingChoice("Give me a hint", "reading-hint") { model.act { ReadingEngine.hint(it) } }
        val text = when (content.game) {
            ReadingGame.CHAPTER -> (content.passage + listOfNotNull(a.branch?.let { content.branches[it].text })).joinToString(" ")
            ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> if (a.order.isEmpty())
                content.tiles.joinToString(". ") else a.order.joinToString(" ") { content.tiles[it] }
            ReadingGame.CAPTION -> content.question?.choices?.joinToString(". ").orEmpty()
            ReadingGame.STUDIO -> ReadingEngine.storyText(a)
            ReadingGame.PARTNERS -> if (a.order.isNotEmpty()) ReadingEngine.storyText(a)
                else a.turns.joinToString(" ") { it.text }
            else -> content.passage.joinToString(" ")
        }
        ReadingListen(text, available, speak, stop) {
            model.act { ReadingEngine.narration(it, queued = true) }
        }
        ReadingButton("Back to reading menu", "reading-back", action = back)
    }
}

@Composable
private fun ReadingQuestionView(a: ReadingAttempt, model: ReadingViewModel, key: String) {
    val question = ReadingEngine.question(a) ?: return
    Text(question.prompt, style = MaterialTheme.typography.titleLarge)
    question.choices.forEachIndexed { index, choice ->
        ReadingChoice(choice, "reading-answer-$index", a.fields["answer"] == index.toString(), !a.earned(key)) {
            model.act { ReadingEngine.field(it, "answer", index.toString()) }
        }
    }
    ReadingButton(if (key == "bonus") "Check bonus" else "Check answer", "reading-check-$key",
        !a.earned(key) && a.fields["answer"] != null) {
        model.submit(key)
    }
    if (a.earned(key)) Text(if (a.checkpoints[key]?.awardedPoint == true)
        "Answer checkpoint earned · +1" else "Answer checkpoint saved")
}

@Composable
private fun ReadingTiles(a: ReadingAttempt, model: ReadingViewModel, tiles: List<String>,
                         pictures: List<ReadingPicture> = emptyList()) {
    val locked = a.earned("order") || a.earned("shared")
    Text("Your order", style = MaterialTheme.typography.titleLarge)
    if (a.order.isEmpty()) Text("Tap the tiles below to begin.")
    a.order.forEachIndexed { position, tile ->
        ReadingChoice("${position + 1}. ${tiles[tile]} · tap to remove", "reading-placed-$position",
            enabled = !locked) { model.act { ReadingEngine.removeTile(it, tile) } }
    }
    Text("Available tiles")
    tiles.forEachIndexed { index, tile ->
        if (index !in a.order) {
            if (index in pictures.indices) ReadingPictureView(pictures[index])
            ReadingChoice(tile, "reading-tile-$index", enabled = !locked) {
                model.act { ReadingEngine.placeTile(it, index) }
            }
        }
    }
    Text("To reorder: remove a placed tile, then tap it to add it at the end. No dragging needed.")
}

private data class ReadingEditor(val text: String, val update: (String) -> Unit)

@Composable
private fun rememberReadingEditor(a: ReadingAttempt, key: String, model: ReadingViewModel): ReadingEditor {
    val turn = if (key == "turn") a.turns.size else -1
    // Presets and typing share one immediate value; an older disk acknowledgement cannot replace it.
    var text by rememberSaveable(a.id, key, model.data?.selectedProfileId, turn) {
        mutableStateOf(a.fields[key].orEmpty())
    }
    val locked = a.content.game == ReadingGame.STUDIO && a.earned(key)
    androidx.compose.runtime.LaunchedEffect(a.id, key, model.data?.selectedProfileId, locked) {
        if (locked) text = a.fields[key].orEmpty()
    }
    return ReadingEditor(text) { raw ->
        val value = raw.take(500)
        text = value
        model.act { ReadingEngine.field(it, key, value, expectedTurn = a.turns.size) }
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
                a.content.writingChoices[index].forEachIndexed { choiceIndex, choice ->
                    ReadingChoice(choice, "reading-studio-$key-$choiceIndex", editor.text == choice) {
                        editor.update(choice)
                    }
                }
            }
            ReadingDraftText(editor, key, "Your $key idea", !a.earned(key))
            ReadingButton("Save $key", "reading-save-$key", !a.earned(key) && !a.fields[key].isNullOrBlank()) {
                model.submit(key)
            }
            if (a.earned(key)) Text(if (a.checkpoints[key]?.awardedPoint == true)
                "$key saved · +1" else "$key saved")
        } else Text("Save the previous part to unlock this checkpoint.")
    }
    if (ReadingEngine.ready(a)) {
        Text("Your finished story", style = MaterialTheme.typography.titleLarge)
        Text(ReadingEngine.storyText(a))
    }
}

@Composable
private fun ReadingPartners(data: ReadingData, a: ReadingAttempt, model: ReadingViewModel, stop: () -> Unit) {
    Text("Reader 1: ${data.name(a.ownerId)}")
    if (a.partnerId == null) {
        Text("Choose a different existing reader as Reader 2.")
        val others = data.profiles.filter { it.id != a.ownerId }
        if (others.isEmpty()) Text("Add another reader using Readers on the menu, then resume this draft.")
        others.forEach { profile ->
            ReadingChoice("Reader 2: ${profile.name}", "reading-partner-${profile.id}") {
                stop(); model.act { ReadingEngine.partner(it, profile.id) }
            }
        }
        return
    }
    Text("Reader 2: ${data.name(a.partnerId)}")
    Text("When Reading Rewards System is on, each reader earns 1 individual participation point, once. Finishing earns 1 shared point, " +
        "recorded once for the team and never added to either individual's points.")
    a.turns.forEachIndexed { index, turn ->
        Text("${index + 1}. ${data.name(turn.readerId)}: ${turn.text}")
    }
    if (a.turns.size < 4) {
        val editor = rememberReadingEditor(a, "turn", model)
        val author = if (a.turns.size % 2 == 0) a.ownerId else a.partnerId
        Text("Turn ${a.turns.size + 1} of 4 · ${data.name(author)} writes",
            style = MaterialTheme.typography.titleLarge)
        Text(listOf("Introduce a character.", "Add a place.", "Tell us the problem.", "Give the story an ending.")[a.turns.size])
        val choices = a.content.writingChoices[a.turns.size].map {
            when (a.turns.size) {
                0 -> "$it began an adventure."
                1 -> "The adventure led to $it."
                2 -> "The character $it."
                else -> "At last, the character $it."
            }
        }
        choices.forEachIndexed { index, choice ->
            ReadingChoice(choice, "reading-turn-choice-$index", editor.text == choice) {
                editor.update(choice)
            }
        }
        androidx.compose.runtime.key(a.turns.size) {
            ReadingDraftText(editor, "turn", "Your sentence", true)
        }
        ReadingButton("Save sentence and pass turn", "reading-save-turn", !a.fields["turn"].isNullOrBlank()) {
            stop(); model.submit("turn", expectedTurn = a.turns.size)
        }
    } else {
        Text("Arrange your four sentences and reread them together.")
        ReadingTiles(a, model, a.turns.map { "${data.name(it.readerId)}: ${it.text}" })
        if (a.order.size == 4) {
            Text(ReadingEngine.storyText(a))
            ReadingChoice("We have reread our story", "reading-reread", a.reread, !a.earned("shared")) {
                model.act { ReadingEngine.reread(it) }
            }
            ReadingButton("Complete our shared story", "reading-shared", a.reread && !a.earned("shared")) {
                model.submit("shared")
            }
        }
    }
}

@Composable
private fun ReadingSummary(data: ReadingData, model: ReadingViewModel, a: ReadingAttempt,
                           available: Boolean, speak: (String) -> Boolean, stop: () -> Unit, back: () -> Unit,
                           gameEnabled: Boolean) {
    ReadingColumn("reading-summary") {
        ReadingPictureView(ReadingPicture("📖 🌱 ✨", "An open book with a growing shoot and sparkling stars"))
        ReadingHeading("Your reading is growing!")
        Text("Congratulations on finishing ${a.content.game.title}.")
        Text("Individual points this attempt: ${a.checkpoints.values.count { it.awardedPoint && !it.shared }}")
        if (a.partnerId != null) Text("Shared completion points: ${a.checkpoints.values.count { it.awardedPoint && it.shared }}, stored once for the team. " +
            "Each reader's participation is credited only to that reader.")
        val checks = a.checkpoints.values.filter { it.objective && it.tries > 0 }
        if (checks.isNotEmpty()) {
            Text("First-try independent: ${checks.count { it.independent }} / ${checks.size}")
            Text("Completed with support: ${checks.count { it.earned && it.supportedAtCompletion }} · " +
                "Completed after retry: ${checks.count { it.retried }}")
        } else Text("Creative checkpoints celebrate participation, not independent reading accuracy.")
        Text("Hints used: ${a.hints} · Narrations: ${a.narrations}")
        data.stories.find { it.attemptId == a.id }?.let { story ->
            Text("Saved to your private story shelf", style = MaterialTheme.typography.titleLarge)
            ReadingPictureView(ReadingPicture(story.illustration, "Your story's illustration"))
            Text(story.text, Modifier.testTag("reading-saved-story"))
            ReadingListen(story.text, available, speak, stop) {}
        }
        Text("Your private milestones", style = MaterialTheme.typography.titleLarge)
        data.milestones(requireNotNull(data.selectedProfileId)).forEach { Text("✦ $it") }
        ReadingButton("Next activity · new attempt", "reading-next", enabled = gameEnabled) {
            stop(); model.act { ReadingEngine.start(it, a.content.game, a.content.level) }
        }
        ReadingButton("Back to reading menu", "reading-back", action = back)
    }
}

@Composable
private fun ReadingProgress(data: ReadingData, model: ReadingViewModel, textSizePercent: Int,
                            onTextSizeChange: (Int) -> Unit, available: Boolean, speak: (String) -> Boolean,
                            stop: () -> Unit, back: () -> Unit, disabledGames: Set<ReadingGame>) {
    val profile = data.profile ?: return
    ReadingColumn("reading-dashboard") {
        ReadingHeading("Private reading progress")
        Text("Reader: ${profile.name}. Only this reader's history and shared participation are shown.")
        Text("Individual points: ${data.points(profile.id)} · Finished activities: ${data.completions(profile.id)}")
        Text("Shared team completions: ${data.sharedCompletions(profile.id)} (not individual points)")
        Text("Shared completion points: ${data.sharedPoints(profile.id)}")
        val accuracy = data.accuracy(profile.id)
        Text("First-try independent accuracy: ${accuracy.firstIndependent} / ${accuracy.attempted}")
        Text("A first incorrect try counts immediately, even before completion. Hints or successful passage narration " +
            "before a first answer prevent independent credit. Retries and supported completions earn points when Reading Rewards System is on. " +
            "Creative writing and partner participation are not accuracy questions.")
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
                val attempts = data.attemptsFor(profile.id).filter { it.content.game == game && it.content.level == level }
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
            stop(); model.act { ReadingEngine.resume(it, attempt.id) }
        }

        HorizontalDivider()
        Text("Saved story shelf", style = MaterialTheme.typography.titleLarge)
        val stories = data.stories.filter { profile.id in it.readerIds }
        if (stories.isEmpty()) Text("Stories from Story Studio and Story Builder Partners will appear here.")
        stories.asReversed().forEach { story ->
            ReadingPictureView(ReadingPicture(story.illustration, "An illustration for ${story.title}"))
            Text(story.title, style = MaterialTheme.typography.titleLarge)
            Text("By ${story.readerIds.joinToString(" and ") { data.name(it) }} · ${formatFinishedAt(story.savedAt)}")
            Text(story.text)
            story.turns.forEach { Text("${data.name(it.readerId)} wrote: ${it.text}") }
            ReadingListen(story.text, available, speak, stop) {}
        }
        ReadingButton("Back to reading menu", "reading-back", action = back)
    }
}

@Composable
private fun ReadingHistoryEntries(data: ReadingData, readerId: String, disabledGames: Set<ReadingGame> = emptySet(),
                                  onOpen: ((ReadingAttempt) -> Unit)? = null) {
    Text("Dated activity history", style = MaterialTheme.typography.titleLarge)
    val attempts = data.attemptsFor(readerId).asReversed()
    if (attempts.isEmpty()) Text("No reading practice results yet for this reader.")
    attempts.forEach { a ->
        Card(Modifier.fillMaxWidth().testTag("reading-history-entry-${a.id}")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${a.content.game.title} · ${a.content.title} · ${a.content.level.name.lowercase()}")
                Text("Started ${formatFinishedAt(a.startedAt)}")
                Text(a.finishedAt?.let { "Finished ${formatFinishedAt(it)}" } ?:
                    if (a.abandoned) "Archived draft" else "In progress · resumable")
                val checks = a.checkpoints.values.filter { it.readerId == readerId }
                Text("${checks.count { it.awardedPoint }} individual points · ${a.hints} hints · ${a.narrations} narrations")
                val objective = checks.filter { it.objective && it.tries > 0 }
                if (objective.isNotEmpty()) Text("${objective.count { it.independent }}/${objective.size} first-try independent · " +
                    "${objective.count { it.earned && it.supportedAtCompletion }} supported · ${objective.count { it.retried }} retried")
                if (onOpen != null && !a.abandoned) ReadingChoice(
                    if (a.finishedAt != null) "Read summary" else "Resume draft", "reading-history-${a.id}",
                    enabled = a.finishedAt != null || a.content.game !in disabledGames
                ) { onOpen(a) }
            }
        }
    }
}
