package com.ssmath.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReadingDatabaseReader(context: Context, private val databaseName: String = "reading.db") {
    private val applicationContext = context.applicationContext

    private suspend fun <T> read(query: (ReadingCatalog) -> T): T = withContext(Dispatchers.IO) {
        // Each request owns its connection, including cancellation/close; never close the game store's database.
        ReadingDatabase(applicationContext, databaseName).use { query(ReadingCatalog(it)) }
    }

    suspend fun page(game: ReadingGame?, level: ReadingLevel?, offset: Int): ReadingCatalogPage =
        read { it.browse(game, level, offset, READING_DATABASE_PAGE_SIZE) }

    suspend fun activity(id: String, revision: String): ReadingActivity = read { it.find(id, revision) }
}

private const val READING_DATABASE_PAGE_SIZE = 40

private class ReadingDatabaseResult<T>(value: T? = null, failed: Boolean = false) {
    val value = value
    val failed = failed
}

@Composable
private fun <T> databaseResult(
    vararg keys: Any?,
    query: suspend () -> T
): State<ReadingDatabaseResult<T>> = produceState(ReadingDatabaseResult<T>(), *keys) {
    value = ReadingDatabaseResult()
    value = try {
        ReadingDatabaseResult(query())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ReadingDatabaseResult(failed = true)
    }
}

@Composable
internal fun ReadingDatabaseBrowser(
    onDismiss: () -> Unit,
    databaseName: String = "reading.db",
    backLabel: String = "Back"
) {
    val context = LocalContext.current.applicationContext
    val reader = remember(context, databaseName) { ReadingDatabaseReader(context, databaseName) }
    var game by rememberSaveable { mutableStateOf<ReadingGame?>(null) }
    var level by rememberSaveable { mutableStateOf<ReadingLevel?>(null) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRevision by rememberSaveable { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val back = {
        if (selectedId == null) onDismiss() else {
            selectedId = null
            selectedRevision = null
        }
    }
    LaunchedEffect(game, level, offset) { listState.scrollToItem(0) }

    Dialog(onDismissRequest = back, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("reading-database-browser")) {
            Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = back, modifier = Modifier.testTag("reading-database-back")) {
                    Text(if (selectedId == null) backLabel else "Back to activities")
                }
                Text("Reading activities database", style = MaterialTheme.typography.titleLarge)
                Text("Read-only · installed activities", style = MaterialTheme.typography.bodySmall)
                val id = selectedId
                val revision = selectedRevision
                if (id != null && revision != null) {
                    val detail by databaseResult(reader, id, revision, retry) { reader.activity(id, revision) }
                    val activity = detail.value
                    when {
                        detail.failed -> DatabaseError { retry++ }
                        activity != null -> ReadingDatabaseDetail(activity, revision, Modifier.weight(1f))
                        else -> DatabaseLoading()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            DatabaseFilter("Game", game?.title ?: "All games", "reading-database-game",
                                listOf(null to "All games") + ReadingGame.entries.map { it to it.title }) {
                                game = it
                                offset = 0
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            DatabaseFilter("Level", level?.name ?: "All levels", "reading-database-level",
                                listOf(null to "All levels") + ReadingLevel.entries.map { it to it.label }) {
                                level = it
                                offset = 0
                            }
                        }
                    }
                    val page by databaseResult(reader, game, level, offset, retry) { reader.page(game, level, offset) }
                    val loaded = page.value
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            page.failed -> DatabaseError { retry++ }
                            loaded == null -> DatabaseLoading()
                            loaded.entries.isEmpty() -> {
                                Text(if (game == null && level == null)
                                    "No reading activities installed. Import reading activities in Reading Settings first."
                                else "No activities match these filters.",
                                    Modifier.testTag("reading-database-empty"))
                            }
                            else -> {
                                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()
                                    .testTag("reading-database-list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(loaded.entries, key = { it.id }) { entry ->
                                        Card(onClick = {
                                            selectedId = entry.id
                                            selectedRevision = entry.revision
                                        }, modifier = Modifier.fillMaxWidth().testTag("reading-database-row-${entry.id}")) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                                Text("ID: ${entry.id}", style = MaterialTheme.typography.bodySmall)
                                                Text("${entry.game.title} · ${entry.level.name}",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    loaded?.let {
                        Text(if (it.entries.isEmpty()) "0 activities" else
                            "${offset + 1}–${offset + it.entries.size} of ${it.total} activities",
                            Modifier.testTag("reading-database-page"))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { offset = (offset - READING_DATABASE_PAGE_SIZE).coerceAtLeast(0) },
                            enabled = loaded != null && offset > 0,
                            modifier = Modifier.testTag("reading-database-previous")) { Text("Previous") }
                        Button(onClick = { offset += READING_DATABASE_PAGE_SIZE },
                            enabled = loaded != null && offset + loaded.entries.size < loaded.total,
                            modifier = Modifier.testTag("reading-database-next")) { Text("Next") }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> DatabaseFilter(
    label: String,
    selected: String,
    tag: String,
    options: List<Pair<T?, String>>,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag(tag)) {
            Text("$label: $selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    expanded = false
                    onSelect(value)
                })
            }
        }
    }
}

@Composable
private fun DatabaseLoading() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("reading-database-loading").semantics { liveRegion = LiveRegionMode.Polite }) {
        CircularProgressIndicator()
        Text("Loading reading activities…")
    }
}

@Composable
private fun DatabaseError(onRetry: () -> Unit) {
    Column(Modifier.testTag("reading-database-error").semantics { liveRegion = LiveRegionMode.Polite }) {
        Text("Unable to read the reading database. Please try again.")
        TextButton(onClick = onRetry, modifier = Modifier.testTag("reading-database-retry")) { Text("Retry") }
    }
}

@Composable
internal fun ReadingDatabaseDetail(activity: ReadingActivity, revision: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("reading-database-detail"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(activity.title, style = MaterialTheme.typography.titleLarge)
        Text("ID: ${activity.id}")
        Text("Game: ${activity.game.title} (${activity.game.name})")
        Text("Level: ${activity.level.label} (${activity.level.name})")
        Text("Revision: $revision", style = MaterialTheme.typography.bodySmall)
        Text("Picture: ${activity.picture.symbol} — ${activity.picture.description}")
        Text("Hint: ${activity.hint}")
        DatabaseSection("Passage")
        if (activity.passage.isEmpty()) Text("No passage")
        activity.passage.forEachIndexed { index, text -> Text("${index + 1}. $text") }
        activity.question?.let { DatabaseQuestion(it) }
        if (activity.game == ReadingGame.DETECTIVE) {
            DatabaseSection("Evidence")
            Text("Sentence ${activity.evidence + 1} (index ${activity.evidence}): " +
                activity.passage.getOrElse(activity.evidence) { "Unavailable" })
        }
        if (activity.tiles.isNotEmpty()) {
            DatabaseSection("Tiles / events")
            activity.tiles.forEachIndexed { index, tile ->
                Text("${index + 1}. $tile")
                activity.eventPictures.getOrNull(index)?.let { Text("${it.symbol} — ${it.description}") }
            }
            DatabaseSection("Correct orders")
            activity.validOrders.forEachIndexed { index, order ->
                Text("Order ${index + 1}: ${order.joinToString(" → ") { (it + 1).toString() }}")
                Text(order.joinToString(" ") { activity.tiles[it] })
            }
        }
        if (activity.writingChoices.isNotEmpty()) {
            DatabaseSection("Writing choices")
            val labels = listOf("Character", "Setting", "Problem", "Ending")
            activity.writingChoices.forEachIndexed { index, choices ->
                Text(labels.getOrElse(index) { "Part ${index + 1}" }, style = MaterialTheme.typography.titleSmall)
                choices.forEachIndexed { choice, text -> Text("${choice + 1}. $text") }
            }
        }
    }
}

@Composable
private fun DatabaseSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DatabaseQuestion(question: ReadingQuestion) {
    DatabaseSection("Question")
    Text(question.prompt)
    question.choices.forEachIndexed { index, choice -> Text("${index + 1}. $choice") }
    Text("Correct answer: ${question.answer + 1}. ${question.choices[question.answer]} (index ${question.answer})")
}
