package com.ssmath.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
internal fun ReadingImportRequiredDialog(model: ReadingViewModel, onDismiss: () -> Unit, onSettings: () -> Unit) {
    val loading = model.packStatus == null && model.storageError == null
    AlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.testTag("reading-import-required"),
        title = { Text(if (model.storageError != null) "Reading storage unavailable"
            else if (loading) "Checking reading activities" else "Import reading activities first") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(model.storageError ?: if (loading) "Please wait while your installed activities are checked."
                    else "Before playing reading games, go to Settings > Admin > Game Settings > Reading Settings and tap " +
                        "Import / Update Reading Activities. An internet connection is needed to import; " +
                        "afterward, the activities work offline. Your saved drafts and progress are kept.")
            }
        },
        confirmButton = {
            if (model.storageError != null) TextButton(onClick = model::retry,
                enabled = !model.saving, modifier = Modifier.testTag("reading-import-retry")) { Text("Retry storage") }
            else if (!loading) TextButton(onClick = onSettings,
                modifier = Modifier.testTag("reading-import-open-settings")) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("reading-import-dismiss")) { Text("Cancel") }
        })
}

@Composable
internal fun ReadingPackSettings(model: ReadingViewModel = rememberReadingViewModel()) {
    val status = model.packStatus
    Column(Modifier.fillMaxWidth().testTag("reading-pack-settings")) {
        Text("Reading activity library", style = MaterialTheme.typography.titleMedium)
        Text(if (status?.version == null) "No reading pack installed."
            else "Installed version: ${status.version} · ${status.counts.values.sum()} activities")
        status?.counts?.takeIf { it.isNotEmpty() }?.let { counts ->
            ReadingGame.entries.forEach { game ->
                Text("${game.title}: " + ReadingLevel.entries.joinToString(" · ") { level ->
                    "${level.name.lowercase()} ${counts["${game.name}:${level.name}"] ?: 0}"
                }, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = model::importActivities,
            enabled = model.data != null && !model.saving && !model.importing && model.storageError == null,
            modifier = Modifier.fillMaxWidth().testTag("reading-pack-import")) {
            Text("Import / Update Reading Activities")
        }
        if (model.importing) CircularProgressIndicator()
        model.importProgress?.let {
            Text(it, Modifier.testTag("reading-pack-progress").semantics { liveRegion = LiveRegionMode.Polite })
        }
        Text("Downloads from the ssAcademy GitHub repository. Internet is only needed to import or update. " +
            "Your saved progress and activity rotation are kept.", style = MaterialTheme.typography.bodySmall)
    }
}
