package com.ssmath.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

internal fun ComposeContentTestRule.openAdminSettings() {
    if (onAllNodesWithTag("debug-logging-description").fetchSemanticsNodes().isEmpty()) {
        onNodeWithText("Debug logging").performScrollTo().performClick()
    }
    val description = onNodeWithTag("debug-logging-description").performScrollTo()
    repeat(7) { description.performClick() }
    onNodeWithTag("admin-dialog").assertIsDisplayed()
}

internal fun ComposeContentTestRule.openGameSettings(subject: Subject = Subject.MATH) {
    openAdminSettings()
    onNodeWithText("Game Settings").performScrollTo().performClick()
    onNodeWithText(
        if (subject == Subject.ADVANCED) "Advanced Subject Settings" else "${subject.label} Settings"
    ).performScrollTo().performClick()
}
