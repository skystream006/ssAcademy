package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class SettingsDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun fullScreenDialogKeepsTitleAndActionsPinnedWhileBodyScrolls() {
        verifyFullScreenDialog(360, 700)
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun fullScreenDialogAlsoFillsLandscapeAndScrolls() {
        verifyFullScreenDialog(800, 300)
    }

    private fun verifyFullScreenDialog(width: Int, minimumHeight: Int) {
        compose.setContent {
            MathTheme {
                SettingsDialog(
                    onDismissRequest = {},
                    title = { Text("Dialog title", Modifier.testTag("title")) },
                    text = { Column { repeat(80) { Text("Setting $it") } } },
                    confirmButton = { TextButton(onClick = {}, modifier = Modifier.testTag("done")) { Text("Done") } },
                    modifier = Modifier.testTag("dialog")
                )
            }
        }
        compose.onNodeWithTag("dialog").assertWidthIsEqualTo(width.dp)
            .assertHeightIsAtLeast(minimumHeight.dp)
        val title = compose.onNodeWithTag("title").getUnclippedBoundsInRoot()
        val done = compose.onNodeWithTag("done").getUnclippedBoundsInRoot()
        compose.onNodeWithText("Setting 79").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("title").assertIsDisplayed()
        compose.onNodeWithTag("done").assertIsDisplayed()
        assertEquals(title, compose.onNodeWithTag("title").getUnclippedBoundsInRoot())
        assertEquals(done, compose.onNodeWithTag("done").getUnclippedBoundsInRoot())
    }

    @Test fun rewardAdjustmentsHaveFourColumnsWithArtworkBetweenUpAndDownControls() {
        val additions = mutableListOf<Pair<RewardType, Boolean>>()
        val removals = mutableListOf<Pair<RewardType, Boolean>>()
        compose.setContent {
            MathTheme {
                AddRemoveRewardsDialog(
                    RewardType.entries.associateWith { RewardBalance(2, 1) },
                    false, null, { type, fragment -> additions += type to fragment },
                    { type, fragment -> removals += type to fragment }, {}
                )
            }
        }
        compose.onNodeWithTag("adjust-rewards-dialog").assertWidthIsEqualTo(360.dp)
            .assertHeightIsAtLeast(700.dp)
        val firstRow = RewardType.entries.filter { it.tier == RewardTier.TIER_1 }.take(4)
        val columns = firstRow.map { compose.onNodeWithTag("adjust-reward-${it.name}").getUnclippedBoundsInRoot() }
        columns.zipWithNext().forEach { (left, right) ->
            assertEquals(left.top, right.top)
            assertTrue(left.right <= right.left)
            assertEquals((left.right - left.left).value, (right.right - right.left).value, 1f)
        }
        firstRow.forEach { type ->
            val up = compose.onNodeWithContentDescription("Add 1 whole ${type.label} reward")
                .getUnclippedBoundsInRoot()
            val image = compose.onNodeWithTag("adjust-image-${type.name}-false").getUnclippedBoundsInRoot()
            val down = compose.onNodeWithContentDescription("Remove 1 whole ${type.label} reward")
                .getUnclippedBoundsInRoot()
            assertTrue(up.bottom <= image.top)
            assertTrue(image.bottom <= down.top)
            assertEquals((up.left + up.right).value, (image.left + image.right).value, 2f)
            assertEquals((down.left + down.right).value, (image.left + image.right).value, 2f)
        }
        compose.onNodeWithContentDescription("Add 1 Lollipop fragment").performScrollTo().performClick()
        compose.onNodeWithText("Add reward?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(RewardType.LOLLIPOP to true), additions) }
        compose.onNodeWithTag("adjust-rewards-dialog").assertWidthIsEqualTo(360.dp)
        compose.onNodeWithContentDescription("Remove 1 whole Lollipop reward").performScrollTo().performClick()
        compose.onNodeWithText("Remove reward?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(RewardType.LOLLIPOP to false), removals) }
    }
}
