package com.ssmath.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

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
