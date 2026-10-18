package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class AdminMenuTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val preferences get() = application.getSharedPreferences("settings", 0)

    @Before fun setup() {
        preferences.edit().clear().commit()
        DebugLog.initialize(application)
    }

    @Test fun adminMenuAppearsBelowAppearance() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().assertIsDisplayed()
        val appearance = compose.onNodeWithText("Appearance").fetchSemanticsNode().boundsInRoot
        val admin = compose.onNodeWithTag("admin-menu").fetchSemanticsNode().boundsInRoot
        assertTrue("Admin Menu should follow Appearance", admin.top >= appearance.bottom)
    }

    @Test fun firstEntryRequiresValidMatchingPinAndEachReopeningRequiresVerification() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Create Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("12a4", "12a4")
        compose.onNodeWithText("Use 4–8 digits.").assertIsDisplayed()
        submit("4826", "4827")
        compose.onNodeWithText("PINs do not match.").assertIsDisplayed()
        assertFalse(AdminPinStore(preferences).hasPin)
        submit("4826", "4826")
        awaitTag("admin-dialog")
        listOf("Game Settings", "My Rewards", "Available Rewards").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Reading activities").assertDoesNotExist()
        compose.onNodeWithText("My Pokémons").assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin-confirm").assertDoesNotExist()
        submit("9999")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Incorrect PIN. Try again.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("4826")
        awaitTag("admin-dialog")
    }

    @Test fun cancellingOrBackingOutOfCreationDoesNotSetPinOrExposeControls() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-pin").performTextInput("4826")
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(AdminPinStore(preferences).hasPin)
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Create Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
        assertFalse(AdminPinStore(preferences).hasPin)
    }

    @Test fun restorationAndLeavingSettingsRelockAccessWithoutLosingPin() {
        val model = MathViewModel(application)
        model.openSettings()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathAppContent(model) }
        compose.openAdminSettings()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { model.openSettings() }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        submit("4826")
        awaitTag("admin-dialog")
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun pinCreationIsScrollableAtDoubleTextSize() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme(textSizePercent = 200) { SettingsScreen(model) } }
        compose.openAdminSettings()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun subsectionsGetProgressivelyDarkerInLightDarkAndWaveThemes() {
        val appearance = mutableStateOf(0)
        compose.setContent {
            MathTheme(mode = if (appearance.value == 1) "dark" else "light", waveAppearance = appearance.value == 2) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Text("Parent")
                        CollapsibleSettingsSection("Outer", defaultExpanded = true) {
                            CollapsibleSettingsSection("Inner", defaultExpanded = true) { Text("Content") }
                        }
                    }
                }
            }
        }
        repeat(3) { mode ->
            compose.runOnIdle { appearance.value = mode }
            val outer = compose.onNodeWithTag("settings-section-Outer").fetchSemanticsNode().boundsInRoot
            val inner = compose.onNodeWithTag("settings-section-Inner").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle {
                val view = compose.activity.findViewById<View>(android.R.id.content)
                val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(image))
                    val parentColor = Color(image.getPixel(outer.center.x.toInt(), 1))
                    val outerColor = Color(image.getPixel(outer.center.x.toInt(), outer.top.toInt() + 1))
                    val innerColor = Color(image.getPixel(inner.center.x.toInt(), inner.top.toInt() + 1))
                    assertTrue(outerColor.luminance() < parentColor.luminance())
                    assertTrue(innerColor.luminance() < outerColor.luminance())
                } finally {
                    image.recycle()
                }
            }
        }
    }

    private fun submit(pin: String, confirmation: String? = null) {
        compose.onNodeWithTag("admin-pin").performScrollTo().performTextReplacement(pin)
        confirmation?.let {
            compose.onNodeWithTag("admin-pin-confirm").performScrollTo().performTextReplacement(it)
        }
        compose.onNodeWithTag("admin-pin-submit").performClick()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }
}
