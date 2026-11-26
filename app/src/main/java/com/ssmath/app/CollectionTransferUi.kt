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
    onDismiss: () -> Unit
) {
    val busy = sync.busy
    val preview = sync.preview
    val account = sync.account
    var address by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var downloadId by rememberSaveable(account) { mutableStateOf("") }
    LaunchedEffect(preview) { if (preview != null) downloadId = "" }
    SettingsDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Sync with server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sync whole rewards, fragments, and collected animations with your ssPortal account. " +
                    "History, reading progress, settings, and the Admin PIN stay on this device.")
                if (account == null) {
                    OutlinedTextField(address, { address = it }, label = { Text("Portal HTTPS address") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("portal-address"))
                    OutlinedTextField(username, { username = it }, label = { Text("Portal username") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("portal-username"))
                    Text("Request access only from a portal you trust. Once approved, reward changes sync automatically.")
                    Button(onClick = { sync.request(address, username) }, enabled = !busy && address.isNotBlank() && username.isNotBlank()) {
                        Text("Request sync access")
                    }
                } else {
                    Text("${account.username} at ${account.origin}\nDevice fingerprint: ${sync.fingerprint}")
                    sync.importId?.let { id -> SelectionContainer { Text("Linked import ID: $id") } }
                    if (preview != null) {
                        val data = preview.server.data
                        Text("Import ${preview.server.id}, revision ${preview.server.revision}: " +
                            "${data.rewards.values.sumOf { it.whole.toLong() }} whole rewards, " +
                            "${data.rewards.values.sumOf { it.fragments.toLong() }} fragments, " +
                            "${data.pokemons.size} animations.", Modifier.testTag("collection-import-preview"))
                        Text("Replace overwrites this device. Merge adds the server balances to this device " +
                            "and uploads the result. Merging again adds the rewards again.")
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
                        Text("Reward changes upload snapshots automatically. To restore on another approved device, " +
                            "copy the linked import ID. Devices cannot list or find newer imports.")
                        Button(onClick = sync::sync, enabled = !busy) { Text("Sync now") }
                        Button(onClick = sync::upload, enabled = !busy) { Text("Upload collections") }
                        OutlinedTextField(downloadId, { downloadId = it }, label = { Text("Import ID") },
                            placeholder = { Text("Leave blank to use the linked ID") }, singleLine = true,
                            enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("portal-import-id"))
                        Button(onClick = { sync.download(downloadId) },
                            enabled = !busy && (sync.importId != null || downloadId.isNotBlank())) { Text("Download collections") }
                    } else {
                        Text("In ssPortal, open ssAcademy > Upload tokens, compare the fingerprint, " +
                            "and approve this device.")
                        Button(onClick = sync::check, enabled = !busy) { Text("Check approval") }
                        TextButton(onClick = { sync.request(account.origin, account.username) }, enabled = !busy) {
                            Text("Retry access request")
                        }
                    }
                    if (preview == null) TextButton(onClick = sync::disconnect, enabled = !busy) { Text("Disconnect device") }
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
