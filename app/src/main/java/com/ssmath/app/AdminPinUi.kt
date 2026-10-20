package com.ssmath.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AdminPinDialog(onUnlocked: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { AdminPinStore(context.getSharedPreferences("settings", 0)) }
    val creating = !store.hasPin
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    SettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "Create Admin PIN" else "Enter Admin PIN") },
        text = {
            val pinFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { pinFocus.requestFocus() }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (creating) "Create a 4–8 digit PIN to protect the Admin Menu. You will need it each time you open this menu."
                    else "Enter your PIN to open the Admin Menu.")
                OutlinedTextField(pin, { pin = it; error = null },
                    label = { Text("PIN") }, singleLine = true, enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().focusRequester(pinFocus).testTag("admin-pin"))
                if (creating) {
                    OutlinedTextField(confirmation, { confirmation = it; error = null },
                        label = { Text("Confirm PIN") }, singleLine = true, enabled = !busy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("admin-pin-confirm"))
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (busy) Text("Checking PIN…")
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && pin.isNotEmpty() && (!creating || confirmation.isNotEmpty()),
                modifier = Modifier.testTag("admin-pin-submit"),
                onClick = {
                    if (!AdminPinStore.isValid(pin)) {
                        error = "Use 4–8 digits."
                    } else if (creating && pin != confirmation) {
                        error = "PINs do not match."
                    } else {
                        busy = true
                        val submitted = pin
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    if (creating) {
                                        store.create(submitted)
                                        AdminPinStore.Result.UNLOCKED
                                    } else store.verify(submitted)
                                }
                                when (result) {
                                    AdminPinStore.Result.UNLOCKED -> onUnlocked()
                                    AdminPinStore.Result.INCORRECT -> error = "Incorrect PIN. Try again."
                                    AdminPinStore.Result.LOCKED -> error = "Too many attempts. Try again in 30 seconds."
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = "Could not access the saved PIN. Please try again."
                            } finally {
                                pin = ""
                                confirmation = ""
                                busy = false
                            }
                        }
                    }
                }) { Text(if (creating) "Create PIN" else "Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn),
        modifier = Modifier.testTag("admin-pin-dialog")
    )
}

@Composable
internal fun ChangeAdminPinDialog(onChanged: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { AdminPinStore(context.getSharedPreferences("settings", 0)) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val clearPins = {
        currentPin = ""
        newPin = ""
        confirmation = ""
    }
    val dismiss = {
        if (!busy && !dismissed) {
            dismissed = true
            clearPins()
            onDismiss()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            dismissed = true
            clearPins()
        }
    }
    SettingsDialog(
        onDismissRequest = dismiss,
        title = { Text("Change PIN") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter your current PIN and choose a new 4–8 digit PIN.")
                ChangePinField("Current PIN", "change-pin-current", currentPin, !busy) {
                    currentPin = it
                    error = null
                }
                ChangePinField("New PIN", "change-pin-new", newPin, !busy) {
                    newPin = it
                    error = null
                }
                ChangePinField("Confirm new PIN", "change-pin-confirm", confirmation, !busy) {
                    confirmation = it
                    error = null
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (busy) Text("Saving PIN…")
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && currentPin.isNotEmpty() && newPin.isNotEmpty() && confirmation.isNotEmpty(),
                modifier = Modifier.testTag("change-pin-submit"),
                onClick = submit@{
                    if (busy || dismissed) return@submit
                    val submittedCurrent = currentPin
                    val submittedNew = newPin
                    val matches = newPin == confirmation
                    clearPins()
                    if (!AdminPinStore.isValid(submittedCurrent) || !AdminPinStore.isValid(submittedNew)) {
                        error = "Use 4–8 digits for each PIN."
                    } else if (!matches) {
                        error = "PINs do not match."
                    } else {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    store.change(submittedCurrent, submittedNew)
                                }
                                if (!dismissed) {
                                    when (result) {
                                        AdminPinStore.Result.UNLOCKED -> {
                                            dismissed = true
                                            onChanged()
                                        }
                                        AdminPinStore.Result.INCORRECT -> error = "Incorrect current PIN. Try again."
                                        AdminPinStore.Result.LOCKED -> error = "Too many attempts. Try again in 30 seconds."
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                if (!dismissed) error = "Could not save the PIN. Please try again."
                            } finally {
                                clearPins()
                                busy = false
                            }
                        }
                    }
                }
            ) { Text("Save PIN") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancel") } },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOn,
            dismissOnBackPress = !busy,
            dismissOnClickOutside = !busy
        ),
        modifier = Modifier.testTag("change-pin-dialog")
    )
}

@Composable
private fun ChangePinField(label: String, tag: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            enabled = enabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = label }
        )
    }
}
