package com.ssmath.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException

@Composable
internal fun ReadingReviewButton(data: ReadingData, model: ReadingViewModel, attemptIds: List<String>, tag: String) {
    var reviewing by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { reviewing = true }, modifier = Modifier.testTag(tag)) {
        Text("Review questions and answers")
    }
    if (reviewing) ReadingReviewDialog(data, attemptIds, model::review) { reviewing = false }
}

@Composable
internal fun ReadingReviewDialog(
    data: ReadingData,
    attemptIds: List<String>,
    load: suspend (ReadingAttempt) -> ReadingAttempt,
    onDismiss: () -> Unit
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val attempts = remember(data.attempts, attemptIds) {
        val byId = data.attempts.associateBy { it.id }
        attemptIds.mapNotNull(byId::get)
    }
    val attempt = attempts.getOrNull(index)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("reading-review")) {
            Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("reading-review-back")) {
                    Text("Back")
                }
                Text("Questions and answers", style = MaterialTheme.typography.titleLarge)
                Text("Read-only · saved reading game")
                if (attempt == null) {
                    Text("No saved questions are available.")
                } else {
                    Text("Question ${index + 1} of ${attempts.size}", Modifier.testTag("reading-review-position"))
                    key(attempt.id) {
                        var loaded by remember(attempt) { mutableStateOf<ReadingAttempt?>(null) }
                        var failed by remember { mutableStateOf(false) }
                        var retry by remember { mutableIntStateOf(0) }
                        LaunchedEffect(attempt, retry) {
                            failed = false
                            loaded = null
                            try {
                                loaded = load(attempt)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failed = true
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when {
                                failed -> {
                                    Text("Unable to open this saved question. Your history has not been changed.")
                                    TextButton(onClick = { retry++ }, modifier = Modifier.testTag("reading-review-retry")) {
                                        Text("Retry")
                                    }
                                }
                                loaded != null -> ReadingAttemptReview(data, requireNotNull(loaded))
                                else -> CircularProgressIndicator()
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { index-- }, enabled = index > 0,
                            modifier = Modifier.testTag("reading-review-previous")) { Text("Previous") }
                        OutlinedButton(onClick = { index++ }, enabled = index < attempts.lastIndex,
                            modifier = Modifier.testTag("reading-review-next")) { Text("Next") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReadingAttemptReview(data: ReadingData, attempt: ReadingAttempt) {
    val content = attempt.content
    Text("${attempt.game.title} · ${attempt.level.label}")
    Text(attempt.title, style = MaterialTheme.typography.titleLarge)
    Text("Started ${formatFinishedAt(attempt.startedAt)}")
    attempt.finishedAt?.let { Text("Finished ${formatFinishedAt(it)}") }
    val creative = attempt.game == ReadingGame.STUDIO
    Text(when {
        attempt.finishedAt != null && creative -> "Completed"
        attempt.finishedAt != null && ReadingEngine.ready(attempt) && ReadingEngine.wrong(attempt) -> "Completed after retry"
        attempt.finishedAt != null && ReadingEngine.wrong(attempt) -> "Incorrect"
        attempt.finishedAt != null -> "Correct"
        attempt.abandoned -> "Not completed · archived"
        else -> "Not completed · in progress"
    }, Modifier.testTag("reading-review-outcome"))
    Text("${attempt.hints} hints · ${attempt.narrations} narrations")
    if (attempt.checkpoints.values.any { it.tries > 1 }) {
        Text("This older activity includes retries. Only the latest saved answer is available, not each earlier response.")
    }
    Text("Picture: ${content.picture.symbol} — ${content.picture.description}")
    content.passage.forEach { Text(it) }
    HorizontalDivider()
    when (attempt.game) {
        ReadingGame.MISSING, ReadingGame.GRAMMAR -> ReviewQuestion(attempt, "answer")
        ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
            Text(if (attempt.game == ReadingGame.SCRAMBLE) "Put the words in order." else "Put the events in order.",
                style = MaterialTheme.typography.titleMedium)
            content.tiles.forEachIndexed { i, tile ->
                Text("${i + 1}. $tile")
                content.eventPictures.getOrNull(i)?.let { Text("${it.symbol} — ${it.description}") }
            }
            ReviewResponse(attempt, "order", attempt.order.takeIf { it.isNotEmpty() }?.joinToString(" → ") {
                content.tiles.getOrNull(it) ?: "___"
            })
            Text("Accepted orders:")
            content.validOrders.forEach { order -> Text(order.joinToString(" → ") { content.tiles[it] }) }
            if (attempt.game == ReadingGame.SEQUENCE) ReviewQuestion(attempt, "answer")
            else if ("bonus" in attempt.checkpoints || "answer" in attempt.fields) ReviewQuestion(attempt, "bonus")
        }
        ReadingGame.DETECTIVE -> {
            ReviewQuestion(attempt, "answer")
            Text("Which sentence proves your answer?", style = MaterialTheme.typography.titleMedium)
            ReviewResponse(attempt, "evidence",
                attempt.fields["evidence"]?.toIntOrNull()?.let(content.passage::getOrNull))
            Text("Correct evidence: ${content.passage[content.evidence]}")
        }
        ReadingGame.STUDIO -> {
            Text("Creative writing · no single correct answer")
            ReadingEngine.studioKeys.forEach { field ->
                Text("Your $field idea", style = MaterialTheme.typography.titleMedium)
                ReviewResponse(attempt, field, attempt.fields[field])
            }
        }
    }
    data.stories.find { it.attemptId == attempt.id }?.let {
        HorizontalDivider()
        Text("Saved story", style = MaterialTheme.typography.titleMedium)
        Text(it.text, Modifier.testTag("reading-review-story"))
    }
}

@Composable
private fun ReviewQuestion(attempt: ReadingAttempt, checkpoint: String) {
    val question = ReadingEngine.question(attempt) ?: return
    Text(question.prompt, style = MaterialTheme.typography.titleMedium)
    question.choices.forEachIndexed { i, choice -> Text("${i + 1}. $choice") }
    ReviewResponse(attempt, checkpoint, attempt.fields["answer"]?.toIntOrNull()?.let(question.choices::getOrNull))
    Text("Correct answer: ${question.choices[question.answer]}")
    if (attempt.game == ReadingGame.GRAMMAR) Text(attempt.content.explanation)
}

@Composable
private fun ReviewResponse(attempt: ReadingAttempt, key: String, response: String?) {
    val check = attempt.checkpoints[key]
    Text("Saved answer: ${response?.takeIf { it.isNotBlank() } ?: "Not answered"}")
    Text(when {
        check == null || check.tries == 0 -> if (response.isNullOrBlank()) "Not submitted" else "Unsubmitted draft"
        !check.objective && check.earned -> "Checkpoint completed"
        check.earned && check.tries > 1 -> "Correct after retry"
        check.earned -> "Correct"
        attempt.finishedAt == null || key == "bonus" ->
            "Previous submissions: ${check.tries} incorrect; the saved draft may have changed."
        else -> "Incorrect submission"
    })
}
