package com.ssmath.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput

internal fun ComposeContentTestRule.openAdminSettings() {
    onNodeWithTag("admin-menu").performScrollTo().performClick()
    onNodeWithTag("admin-pin").performScrollTo().performTextInput("4826")
    if (onAllNodesWithTag("admin-pin-confirm").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("admin-pin-confirm").performScrollTo().performTextInput("4826")
    }
    onNodeWithTag("admin-pin-submit").performScrollTo().performClick()
    waitUntil(10_000) {
        onAllNodesWithTag("admin-dialog").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag("admin-dialog").assertIsDisplayed()
}

internal fun ComposeContentTestRule.openDebugSettings() {
    if (onAllNodesWithTag("debug-logging-description").fetchSemanticsNodes().isEmpty()) {
        onNodeWithText("Debug logging").performScrollTo().performClick()
    }
    val description = onNodeWithTag("debug-logging-description").performScrollTo()
    repeat(7) { description.performClick() }
    onNodeWithTag("debug-dialog").assertIsDisplayed()
}

internal fun ComposeContentTestRule.openGameSettings(subject: Subject = Subject.MATH) {
    openAdminSettings()
    onNodeWithText("Game Settings").performScrollTo().performClick()
    onNodeWithText(
        if (subject == Subject.ADVANCED) "Advanced Subject Settings" else "${subject.label} Settings"
    ).performScrollTo().performClick()
}
