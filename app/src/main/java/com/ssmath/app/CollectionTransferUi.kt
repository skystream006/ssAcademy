package com.ssmath.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun CollectionTransferDialog(
    sync: CollectionSync,
    readingModel: ReadingViewModel,
    onDismiss: () -> Unit
) {
    val busy = sync.busy
    val preview = sync.preview
    val account = sync.account
    val student = readingModel.data?.profile?.name
    val nameReady = student != null && !readingModel.saving && readingModel.storageError == null
    var address by rememberSaveable { mutableStateOf("") }
    var downloadId by rememberSaveable(account) { mutableStateOf("") }
    LaunchedEffect(preview) { if (preview != null) downloadId = "" }
    SettingsDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Sync with server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sync whole rewards, fragments, collected animations, and game and appearance settings " +
                    "with ssPortal using your saved game name. History, reading progress, local names, " +
                    "and the Admin PIN and lock stay on this device.")
                if (readingModel.storageError != null) {
                    Text(requireNotNull(readingModel.storageError))
                    TextButton(onClick = readingModel::retry, enabled = !readingModel.saving) { Text("Retry storage") }
                } else if (student == null) {
                    Text(if (readingModel.data == null) "Opening your saved game name..."
                        else "Enter your name at the welcome screen before requesting sync access.")
                } else {
                    Text("Student: $student", Modifier.testTag("portal-student"))
                }
                if (account == null) {
                    OutlinedTextField(address, { address = it }, label = { Text("Portal HTTPS address") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("portal-address"))
                    Text("Your game name is sent for device registration and approval checks. " +
                        "Request access only from a portal you trust. Once approved, reward, collection, " +
                        "and settings changes sync automatically.")
                    Button(onClick = { sync.request(address, requireNotNull(student)) },
                        enabled = !busy && address.isNotBlank() && nameReady) {
                        Text("Request sync access")
                    }
                } else {
                    Text("${account.student} at ${account.origin}\nDevice fingerprint: ${sync.fingerprint}")
                    sync.importId?.let { id -> SelectionContainer { Text("Linked import ID: $id") } }
                    if (student != null && account.student != student) {
                        Text("This device is linked to a different student name. " +
                            "Disconnect and request access again to sync as $student.")
                    } else if (!nameReady) {
                        Text("Sync controls will be available once your game name is saved and readable.")
                    } else if (preview != null) {
                        val data = preview.server.data
                        Text("Import ${preview.server.id}, revision ${preview.server.revision}: " +
                            "${data.rewards.values.sumOf { it.whole.toLong() }} whole rewards, " +
                            "${data.rewards.values.sumOf { it.fragments.toLong() }} fragments, " +
                            "${data.pokemons.size} animations.", Modifier.testTag("collection-import-preview"))
                        Text("Replace overwrites this device. Merge adds the server balances to this device " +
                            "and uploads the result. Merging again adds the rewards again.")
                        Text(when {
                            data.settings == null -> "This import has no settings. Local settings will be kept."
                            data.settings.isEmpty() -> "Replace restores default game and appearance settings. " +
                                "Merge keeps local settings."
                            else -> "Synced settings: ${data.settings.size} saved overrides. " +
                                "Replace restores these settings; Merge keeps local settings."
                        }, Modifier.testTag("collection-settings-preview"))
                        Button(onClick = { sync.confirmDownload(CollectionImportMode.REPLACE) }, enabled = !busy) {
                            Text("Replace device collections")
                        }
                        Button(onClick = { sync.confirmDownload(CollectionImportMode.MERGE) }, enabled = !busy) {
                            Text("Merge collections")
                        }
                        if (preview.allowUpload) Button(onClick = sync::confirmUpload, enabled = !busy) {
                            Text("Upload device snapshot instead")
                        }
                    } else if (sync.approved) {
                        Text("Reward, collection, and settings changes upload snapshots automatically. " +
                            "To restore on another approved device " +
                            "linked to the same portal account, copy the linked import ID. Devices cannot list or find newer imports.")
                        TextButton(onClick = sync::check, enabled = !busy) { Text("Check approval") }
                        Button(onClick = sync::sync, enabled = !busy) { Text("Sync now") }
                        Button(onClick = sync::upload, enabled = !busy) { Text("Upload collections") }
                        OutlinedTextField(downloadId, { downloadId = it }, label = { Text("Import ID") },
                            placeholder = { Text("Leave blank to use the linked ID") }, singleLine = true,
                            enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("portal-import-id"))
                        Button(onClick = { sync.download(downloadId) },
                            enabled = !busy && (sync.importId != null || downloadId.isNotBlank())) { Text("Download collections") }
                    } else {
                        Text("In ssPortal, open ssAcademy > Sync Tokens, compare the fingerprint, " +
                            "and approve this device.")
                        Button(onClick = sync::check, enabled = !busy) { Text("Check approval") }
                        TextButton(onClick = { sync.request(account.origin, requireNotNull(student)) }, enabled = !busy) {
                            Text("Retry access request")
                        }
                    }
                    if (preview == null || account.student != student) {
                        TextButton(onClick = sync::disconnect, enabled = !busy) { Text("Disconnect device") }
                    }
                }
            }
        },
        status = {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            sync.message?.let {
                Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("collection-transfer-message"))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
        },
        dismissButton = {
            if (preview != null) TextButton(onClick = sync::cancelPreview, enabled = !busy) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        modifier = Modifier.testTag("collection-transfer-dialog")
    )
}
