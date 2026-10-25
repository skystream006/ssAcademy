package com.ssmath.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun CollectionTransferDialog(
    model: MathViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = model.collectionTransferBusy
    val preview = model.collectionImportPreview
    SettingsDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Export / Import Collections") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Transfer all whole rewards, fragments, and collected animations in one file. " +
                    "Includes standard and Mega Pokémon and Other celebrations.")
                Text("Game history, reading progress, settings, and the Admin PIN are not exported or replaced.")
                if (preview == null) {
                    Button(onClick = onExport, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("export-collections")) {
                        Text("Export collections")
                    }
                    Button(onClick = onImport, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("import-collections")) {
                        Text("Import collections")
                    }
                    Text("Save the JSON file, copy it to the other device, then open this menu there to import it. " +
                        "Importing always asks you to replace or merge.")
                } else {
                    Text("Ready to import: ${preview.rewards.values.sumOf { it.whole.toLong() }} whole rewards, " +
                        "${preview.rewards.values.sumOf { it.fragments.toLong() }} fragments, " +
                        "and ${preview.pokemons.size} collected animations.",
                        Modifier.testTag("collection-import-preview"))
                    Text("Replace overwrites both collections on this device with the file's contents. " +
                        "Export this device's collections first if you want to keep a backup.")
                    Text("Merge adds reward balances and combines animations without duplicates. " +
                        "Merging the same file again adds its rewards again.")
                    Button(onClick = { model.importCollections(CollectionImportMode.REPLACE) }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("replace-collections")) {
                        Text("Replace collections")
                    }
                    Button(onClick = { model.importCollections(CollectionImportMode.MERGE) }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("merge-collections")) {
                        Text("Merge collections")
                    }
                }
            }
        },
        status = {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            model.collectionTransferMessage?.let {
                Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("collection-transfer-message"))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
        },
        dismissButton = {
            if (preview != null) TextButton(onClick = model::cancelCollectionImport, enabled = !busy) {
                Text("Cancel import")
            }
        },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        modifier = Modifier.testTag("collection-transfer-dialog")
    )
}
