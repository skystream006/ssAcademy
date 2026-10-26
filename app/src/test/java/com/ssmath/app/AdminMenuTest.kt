package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.AnnotatedString
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
        compose.onNodeWithTag("admin-pin").assertIsFocused()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("12a4", "12a4")
        compose.onNodeWithText("Use 4–8 digits.").assertIsDisplayed()
        submit("4826", "4827")
        compose.onNodeWithText("PINs do not match.").assertIsDisplayed()
        assertFalse(AdminPinStore(preferences).hasPin)
        submit("4826", "4826")
        awaitTag("admin-dialog")
        listOf("Game Settings", "Add/Remove Rewards", "Available Rewards").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Reading activities").assertDoesNotExist()
        compose.onNodeWithText("My Pokémons").assertDoesNotExist()
        compose.onNodeWithText("My Rewards").assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin").assertIsFocused()
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
        compose.onNodeWithTag("admin-pin").assertIsFocused()
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

    @Test fun changingPinReturnsToAdminAndReopeningRequiresOnlyTheNewPin() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        submit("4826", "4826")
        awaitTag("admin-dialog")
        openChangePin()
        listOf("current", "new", "confirm").forEach {
            compose.onNodeWithTag("change-pin-$it").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        }
        compose.runOnIdle {
            val window = requireNotNull(ShadowDialog.getLatestDialog().window)
            assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
        fillChangePin("4826", "00123456", "00123456")
        compose.onNodeWithTag("change-pin-submit").performClick()
        awaitChangeClosed()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        submit("4826")
        awaitError("Incorrect PIN. Try again.")
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        submit("00123456")
        awaitTag("admin-dialog")
    }

    @Test fun changePinValidationMismatchAndWrongCurrentPinNeverReplaceCredentials() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.openAdminSettings()
        val previous = preferences.all.toMap()
        openChangePin()
        compose.onNodeWithTag("change-pin-submit").assertIsNotEnabled()
        listOf("123" to "0000", "4826" to "123", "4826" to "１２３４").forEach { (current, new) ->
            fillChangePin(current, new, new)
            compose.onNodeWithTag("change-pin-submit").performClick()
            awaitError("Use 4–8 digits for each PIN.")
            assertChangeFieldsEmpty()
            assertEquals(previous, preferences.all)
        }
        fillChangePin("4826", "0000", "1111")
        compose.onNodeWithTag("change-pin-submit").performClick()
        awaitError("PINs do not match.")
        assertChangeFieldsEmpty()
        assertEquals(previous, preferences.all)

        repeat(5) { attempt ->
            fillChangePin("9999", "0000", "0000")
            compose.onNodeWithTag("change-pin-submit").performClick()
            awaitError(if (attempt == 4) "Too many attempts. Try again in 30 seconds." else "Incorrect current PIN. Try again.")
            assertChangeFieldsEmpty()
        }
        assertEquals(previous["admin_pin_hash"], preferences.getString("admin_pin_hash", null))
        assertEquals(previous["admin_pin_salt"], preferences.getString("admin_pin_salt", null))
        assertEquals(AdminPinStore.Result.LOCKED, AdminPinStore(preferences).verify("4826"))
        fillChangePin("4826", "0000", "0000")
        compose.onNodeWithTag("change-pin-submit").performClick()
        awaitError("Too many attempts. Try again in 30 seconds.")
        compose.onNodeWithTag("change-pin-dialog").assertIsDisplayed()
    }

    @Test fun cancelAndBackDiscardChangePinFieldsWithoutChangingCredentials() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.openAdminSettings()
        val previous = preferences.all.toMap()
        openChangePin()
        fillChangePin("4826", "0000", "0000")
        compose.onNodeWithText("Cancel").performClick()
        awaitTag("admin-dialog")
        compose.onNodeWithTag("change-pin-dialog").assertDoesNotExist()
        assertEquals(previous, preferences.all)
        openChangePin()
        assertChangeFieldsEmpty()
        fillChangePin("4826", "0000", "0000")
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        awaitTag("admin-dialog")
        compose.onNodeWithTag("change-pin-dialog").assertDoesNotExist()
        assertEquals(previous, preferences.all)
        openChangePin()
        assertChangeFieldsEmpty()
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("4826"))
    }

    @Test fun restorationWhileChangingPinRelocksAndDiscardsAllPinText() {
        val model = MathViewModel(application)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SettingsScreen(model) } }
        compose.openAdminSettings()
        val previous = preferences.all.toMap()
        openChangePin()
        fillChangePin("4826", "0000", "0000")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("change-pin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        assertEquals(previous, preferences.all)
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-pin").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("4826")
        awaitTag("admin-dialog")
        openChangePin()
        assertChangeFieldsEmpty()
    }

    @Test fun debugPinBypassPersistsAndReenablingPreservesTheExistingPin() {
        val model = MathViewModel(application)
        AdminPinStore(preferences).create("4826")
        val hash = preferences.getString("admin_pin_hash", null)
        val salt = preferences.getString("admin_pin_salt", null)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.openDebugSettings()
        compose.onNodeWithContentDescription("Disable Admin PIN lock").assertIsOff().performClick().assertIsOn()
        assertFalse(MathViewModel(application).adminPinLockEnabled)
        compose.onNodeWithText("Close").performClick()
        repeat(2) {
            compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
            compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
            compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
            compose.onNodeWithText("Close").performClick()
        }
        compose.openDebugSettings()
        compose.onNodeWithContentDescription("Disable Admin PIN lock").performClick().assertIsOff()
        compose.onNodeWithText("Close").performClick()
        assertTrue(MathViewModel(application).adminPinLockEnabled)
        assertEquals(hash, preferences.getString("admin_pin_hash", null))
        assertEquals(salt, preferences.getString("admin_pin_salt", null))
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        submit("4826")
        awaitTag("admin-dialog")
    }

    @Test fun debugBypassWithoutPinAllowsCreatingOneAndReturnsToAdmin() {
        val model = MathViewModel(application)
        model.chooseAdminPinLockEnabled(false)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Create PIN").performClick()
        submit("4826", "4826")
        awaitTag("admin-dialog")
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("4826"))
        compose.onNodeWithText("Change PIN").assertIsDisplayed()
    }

    @Test fun pinBypassKeepsAdminReturnDestinationAfterRestoringEitherChildPage() {
        val model = MathViewModel(application)
        model.chooseAdminPinLockEnabled(false)
        model.openSettings()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathAppContent(model) }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.runOnIdle { model.openRewardsSetup() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("save-rewards-setup").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.runOnIdle { model.openReadingProgress() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("reading-back").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
    }

    @Test fun adminDefaultsReturnOnSaveAndBackAndRelockAfterRestoration() {
        val model = MathViewModel(application)
        model.openSettings()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathAppContent(model) }
        compose.openGameSettings()
        val sections = listOf("Basic Math Settings" to "rewards-setup",
            "Multiplication Matrix Settings" to "matrix-rewards-setup",
            "Logic Math Settings" to "logic-rewards-setup")
        sections.forEach { (section, tag) ->
            compose.onNodeWithText(section).performScrollTo().performClick()
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.onNodeWithTag("save-rewards-setup").performScrollTo().performClick()
            compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
            compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
        }
        compose.onNodeWithTag("logic-rewards-setup").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("rewards-setup-screen").assertDoesNotExist()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        submit("4826")
        awaitTag("rewards-setup-screen")
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun settingsFollowDarkAppearanceWithoutChangingSavedPreferences() {
        val model = MathViewModel(application)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        val appearances = COLOR_THEMES.flatMap { (theme, _) ->
            listOf(false, true).map { wave -> Triple(theme, null, wave) }
        } +
            COLOR_THEMES.flatMap { (theme, _) ->
                listOf(false, true).flatMap { wave ->
                    listOf(Triple(theme, "dark", wave), Triple(theme, "light", wave))
                }
            }
        appearances.forEach { (theme, mode, wave) ->
            compose.runOnIdle {
                model.chooseWaveAppearance(wave)
                model.chooseTheme(theme)
                if (mode != null) model.chooseDark(mode == "dark")
                model.chooseSkin(AppSkin.GALAXY)
                model.chooseSkins(true)
            }
            compose.onNodeWithTag("settings-screen").assertIsDisplayed()
            val luminance = screenCornerColor("settings-screen").luminance()
            assertTrue("Settings must follow dark appearance for $theme/$mode/$wave",
                if (isDarkTheme(theme, mode)) luminance < 0.1f else luminance > 0.8f)
            compose.runOnIdle {
                assertEquals(theme, model.theme)
                assertEquals(mode, model.mode)
                assertEquals(wave, model.waveAppearance)
                assertTrue(model.skinsEnabled)
                assertEquals(AppSkin.GALAXY, model.skin)
                assertEquals(theme, preferences.getString("theme", null))
                assertEquals(mode, preferences.getString("mode", null))
                assertEquals(wave, preferences.getBoolean("wave_appearance", !wave))
            }
        }
        compose.runOnIdle {
            model.chooseWaveAppearance(false)
            model.chooseTheme("black")
            model.chooseDark(true)
            model.chooseSkins(false)
            model.chooseTextSize(150)
        }
        compose.onNodeWithText("Appearance").performScrollTo().performClick()
        compose.onNodeWithText("Text size: 150%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dark appearance").performScrollTo().assertIsDisplayed()
        val darkSwitch = compose.onNodeWithContentDescription("Dark appearance")
        darkSwitch.performScrollTo().assertIsOn().performClick()
        assertTrue(screenCornerColor("settings-screen").luminance() > 0.8f)
        compose.runOnIdle { assertEquals("light", preferences.getString("mode", null)) }
        darkSwitch.assertIsOff().performClick()
        assertTrue(screenCornerColor("settings-screen").luminance() < 0.1f)
        compose.runOnIdle { assertEquals("dark", preferences.getString("mode", null)) }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("subject-Math").assertIsDisplayed()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            assertTrue(cornerColor(view, 1, view.height / 2).luminance() < 0.1f)
            assertEquals("dark", model.mode)
            assertEquals("black", preferences.getString("theme", null))
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun adminDialogsAndChildPagesFollowDarkAppearance() {
        val model = MathViewModel(application)
        model.chooseWaveAppearance(false)
        model.chooseTheme("black")
        model.chooseDark(true)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
        assertTrue(dialogCornerColor().luminance() < 0.1f)
        submit("4826", "4826")
        awaitTag("admin-dialog")
        assertTrue(dialogCornerColor().luminance() < 0.1f)
        compose.runOnIdle { model.openRewardsSetup() }
        compose.onNodeWithTag("rewards-setup-screen").assertIsDisplayed()
        assertTrue(screenCornerColor("settings-screen").luminance() < 0.1f)
        compose.onNodeWithTag("save-rewards-setup").performScrollTo().performClick()
        compose.runOnIdle { model.openReadingProgress() }
        compose.onNodeWithTag("reading-back").performScrollTo().assertIsDisplayed()
        assertTrue(screenCornerColor("settings-screen").luminance() < 0.1f)
        compose.runOnIdle { assertEquals("dark", model.mode) }
    }

    private fun screenCornerColor(tag: String): Color {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle {
            cornerColor(compose.activity.findViewById(android.R.id.content),
                bounds.left.toInt() + 1, bounds.top.toInt() + 1)
        }
    }

    private fun dialogCornerColor(): Color = compose.runOnIdle {
        cornerColor(ShadowDialog.getLatestDialog().findViewById(android.R.id.content), 1, 1)
    }

    private fun cornerColor(view: View, x: Int, y: Int): Color {
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(image))
            Color(image.getPixel(x, y))
        } finally {
            image.recycle()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun subsectionsGetProgressivelyLighterWithoutLosingTextContrast() {
        val appearances = listOf(Triple("green", "dark", true)) +
            COLOR_THEMES.flatMap { (theme, _) ->
                listOf(Triple(theme, "dark", false), Triple(theme, "light", false))
            }
        val appearance = mutableStateOf(appearances.first())
        var textColors = listOf<Color>()
        compose.setContent {
            val (theme, mode, wave) = appearance.value
            MathTheme(theme = theme, mode = mode, waveAppearance = wave) {
                textColors = listOf(MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Text("Parent")
                        CollapsibleSettingsSection("Outer", defaultExpanded = true) {
                            CollapsibleSettingsSection("Inner", defaultExpanded = true) {
                                CollapsibleSettingsSection("Deepest", defaultExpanded = true) { Text("Content") }
                            }
                        }
                    }
                }
            }
        }
        appearances.forEach { selection ->
            compose.runOnIdle { appearance.value = selection }
            val sections = listOf("Outer", "Inner", "Deepest").map {
                compose.onNodeWithTag("settings-section-$it").fetchSemanticsNode().boundsInRoot
            }
            compose.runOnIdle {
                val view = compose.activity.findViewById<View>(android.R.id.content)
                val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(image))
                    val parentColor = Color(image.getPixel(sections.first().center.x.toInt(), 1))
                    val sectionColors = sections.map { bounds ->
                        Color(image.getPixel(bounds.center.x.toInt(), bounds.top.toInt() + 1))
                    }
                    (listOf(parentColor) + sectionColors).zipWithNext().forEach { (parent, child) ->
                        if (parent == Color.White) assertEquals(Color.White, child)
                        else assertTrue("Nested sections must get lighter for $selection",
                            child.luminance() > parent.luminance())
                    }
                    sectionColors.forEach { background ->
                        textColors.forEach { text ->
                            val lighter = maxOf(text.luminance(), background.luminance())
                            val darker = minOf(text.luminance(), background.luminance())
                            assertTrue("Settings text must keep 4.5:1 contrast for $selection",
                                (lighter + 0.05f) / (darker + 0.05f) >= 4.5f)
                        }
                    }
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

    private fun openChangePin() {
        compose.onNodeWithTag("change-pin").performScrollTo().performClick()
        awaitTag("change-pin-dialog")
    }

    private fun fillChangePin(current: String, new: String, confirmation: String) {
        compose.onNodeWithTag("change-pin-current").performScrollTo().assertIsDisplayed().performTextReplacement(current)
        compose.onNodeWithTag("change-pin-new").performScrollTo().assertIsDisplayed().performTextReplacement(new)
        compose.onNodeWithTag("change-pin-confirm").performScrollTo().assertIsDisplayed().performTextReplacement(confirmation)
    }

    private fun assertChangeFieldsEmpty() {
        listOf("current", "new", "confirm").forEach {
            compose.onNodeWithTag("change-pin-$it").assertTextEquals("")
        }
        compose.onNodeWithTag("change-pin-submit").assertIsNotEnabled()
    }

    private fun awaitError(message: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(message).performScrollTo().assertIsDisplayed()
    }

    private fun awaitChangeClosed() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("change-pin-dialog").fetchSemanticsNodes().isEmpty() }
    }
}
