package com.ssmath.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SettingsDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable () -> Unit = {},
    properties: DialogProperties = DialogProperties(),
    scrollState: ScrollState = rememberScrollState(),
    status: @Composable () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall, title)
                }
                HorizontalDivider()
                status()
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(16.dp)) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge, text)
                }
                HorizontalDivider()
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}
