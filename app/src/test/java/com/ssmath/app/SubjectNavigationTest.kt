package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubjectNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()

    @Before fun setup() {
        DebugLog.initialize(application)
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        File(application.filesDir, "practice_history.json").delete()
        application.deleteDatabase("reading.db")
        listOf("reading_private.json", "reading_private.json.bak", "reading_private.json.new").forEach {
            File(application.filesDir, it).delete()
        }
    }

    private fun model() = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)

    private fun installReadingPack() {
        val database = ReadingDatabase(application)
        try {
            runBlocking {
                readingTestImporter(ReadingCatalog(database), application.cacheDir, ReadingContent.activities).importPack()
            }
        } finally {
            database.close()
        }
    }

    @Test fun subjectHistoriesReturnToSettingsAndNeverShowMathResults() {
        val model = model()
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        Subject.entries.forEach { subject ->
            compose.onNodeWithText(
                if (subject == Subject.ADVANCED) "Advanced Subject Settings" else "${subject.label} Settings")
                .performScrollTo().performClick()
            compose.onNodeWithText("${subject.label} Practice History").performScrollTo().performClick()
            compose.onNodeWithText("${subject.label} Practice History").assertIsDisplayed()
            if (subject == Subject.READING) {
                compose.waitUntil(5_000) {
                    compose.onAllNodesWithTag("reading-practice-history-screen").fetchSemanticsNodes().isNotEmpty()
                }
            }
            assertEquals(if (subject == Subject.MATH) Overlay.HISTORY else Overlay.SUBJECT_HISTORY, model.overlay)
            if (subject == Subject.SCIENCE || subject == Subject.ADVANCED) {
                compose.onNodeWithText("No ${subject.label.lowercase()} practice results yet. More activities are coming soon.")
                    .assertIsDisplayed()
            }
            compose.onNodeWithContentDescription("Back").performClick()
            assertEquals(Overlay.SETTINGS, model.overlay)
        }
    }

    @Test fun subjectHistoryDoesNotResumeOrResetAnActiveMathGame() {
        var now = 0L
        val model = MathViewModel(application, clock = { now }, ioDispatcher = Dispatchers.Unconfined)
        model.selectOperation(Operation.ADDITION)
        model.submitSetup()
        model.start()
        now = 1_000
        model.openSettings()
        val game = model.game
        val elapsed = model.elapsedMs()
        Subject.entries.forEach {
            model.openSubjectHistory(it)
            now += 1_000
            assertEquals(elapsed, model.elapsedMs())
            assertEquals(game, model.game)
            model.closeOverlay()
            assertEquals(Overlay.SETTINGS, model.overlay)
        }
        model.closeOverlay()
        now += 1_000
        assertEquals(elapsed + 1_000, model.elapsedMs())
    }

    @Test fun readingReturnsHomeWithoutChangingMathSetupOrRewards() {
        val model = model()
        model.selectOperation(Operation.MULTIPLICATION)
        model.updateMinimum("3")
        model.updateMaximum("12")
        model.updateQuestionCount("20")
        val defaults = model.rewardsDefaults
        val rewards = model.rewardBalances
        model.openReading()
        assertEquals(Overlay.READING, model.overlay)
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.game)
        model.closeOverlay()
        assertNull(model.overlay)
        assertEquals(Operation.MULTIPLICATION, model.selectedOperation)
        assertEquals("3", model.minimumText)
        assertEquals("12", model.maximumText)
        assertEquals("20", model.questionCountText)
        assertEquals(defaults, model.rewardsDefaults)
        assertEquals(rewards, model.rewardBalances)
        assertTrue(model.history.isEmpty())
    }

    @Test fun readingCannotReplaceSettingsOrPendingMathSetup() {
        val model = model()
        model.openSettings()
        model.openReading()
        assertEquals(Overlay.SETTINGS, model.overlay)
        model.closeOverlay()
        model.openPracticeGame()
        model.openReading()
        assertNull(model.overlay)
        assertEquals(SetupDialog.PRACTICE, model.setupDialog)
        model.dismissSetupDialog()
        model.openReading()
        assertEquals(Overlay.READING, model.overlay)
        model.openPracticeGame()
        model.openRewardsGame()
        assertNull(model.setupDialog)
    }

    @Test fun readingSubjectReturnsToSubjectsBeforeOpeningMathChoices() {
        installReadingPack()
        val model = model()
        compose.setContent { MathAppContent(model) }
        repeat(2) { returnMethod ->
            compose.onNodeWithTag("subject-Reading").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("reading-menu").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("reading-menu").assertIsDisplayed()
            compose.onNodeWithTag("subject-grid").assertDoesNotExist()
            compose.onNodeWithTag("settings-button").assertIsDisplayed()
            if (returnMethod == 0) compose.onNodeWithTag("reading-exit").performScrollTo().performClick()
            else compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithTag("home-screen").assertIsDisplayed()
            compose.onNodeWithTag("subject-grid").assertIsDisplayed()
            compose.onNodeWithTag("practice-game").assertDoesNotExist()
            assertFalse(model.mathGamesOpen)
            assertNull(model.overlay)
        }
        compose.onNodeWithTag("subject-Math").performScrollTo().performClick()
        compose.onNodeWithTag("practice-game").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rewards-game").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("subject-Reading").assertDoesNotExist()
        assertNull(model.overlay)
    }

    @Test fun activeReadingGameSettingsApplyOnEntryAndKeepSavedDrafts() {
        val data = readingFixture().copy(page = ReadingPage.MENU)
        val store = ReadingStore(File(application.filesDir, "reading_private.json"))
        store.load()
        store.save(data)
        installReadingPack()
        val readingModel = ViewModelProvider(compose.activity,
            ViewModelProvider.AndroidViewModelFactory(application))[ReadingViewModel::class.java]
        val model = model()
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Reading Settings").performScrollTo().performClick()
        compose.onNodeWithTag("active-reading-games").performScrollTo().performClick()
        compose.onNodeWithContentDescription(ReadingGame.MISSING.title).performScrollTo()
            .assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("subject-Reading").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("reading-menu").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { assertEquals(data, readingModel.data?.withoutCatalogueRefs()) }
        compose.onNodeWithTag("reading-game-MISSING").assertDoesNotExist()
        compose.onNodeWithTag("reading-game-SEQUENCE").performScrollTo().assertIsDisplayed()
        assertEquals(data, ReadingSqliteStore(application).load().withoutCatalogueRefs())
    }

    @Test fun readingCollectionsReturnToReadingAndSettingsControlsPreserveDrafts() {
        val data = readingFixture().copy(page = ReadingPage.MENU)
        val store = ReadingStore(File(application.filesDir, "reading_private.json"))
        store.load()
        store.save(data)
        val readingModel = ViewModelProvider(compose.activity,
            ViewModelProvider.AndroidViewModelFactory(application))[ReadingViewModel::class.java]
        val model = model()
        model.openReading()
        compose.setContent { MathAppContent(model) }
        compose.waitUntil(5_000) { readingModel.data != null }
        listOf("reading-rewards", "reading-pokemons").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
            assertEquals(if (tag.endsWith("rewards")) Overlay.REWARDS else Overlay.POKEMONS, model.overlay)
            compose.onNodeWithContentDescription("Back").performClick()
            assertEquals(Overlay.READING, model.overlay)
            compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        }
        compose.onNodeWithTag("settings-button").performClick()
        compose.onNodeWithText("Reading Settings").performScrollTo().performClick()
        compose.onNodeWithTag("set-reading-level").performScrollTo().performClick()
        compose.onNodeWithTag("reading-level-SPROUT").assertIsSelected()
        compose.onNodeWithTag("reading-level-TRAIL").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            readingModel.data?.profile?.level == ReadingLevel.TRAIL && !readingModel.saving
        }
        compose.onNodeWithTag("reading-level-TRAIL").assertIsSelected()
        compose.onNodeWithText("Done").performClick()
        val persisted = ReadingSqliteStore(application).load().withoutCatalogueRefs()
        assertEquals(data.attempts, persisted.attempts)
        assertEquals(ReadingLevel.TRAIL, persisted.profile?.level)
        compose.onNodeWithTag("reading-progress").performScrollTo().performClick()
        compose.onNodeWithTag("reading-dashboard").assertIsDisplayed()
        compose.onNodeWithTag("reading-back").performScrollTo().performClick()
        assertEquals(Overlay.SETTINGS, model.overlay)
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(Overlay.READING, model.overlay)
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        assertEquals(data.attempts, readingModel.data?.withoutCatalogueRefs()?.attempts)
        compose.onNodeWithTag("reading-progress").assertDoesNotExist()
        compose.onNodeWithTag("reading-level-SPROUT").assertDoesNotExist()
    }

    @Test fun progressCanResumeDraftWhileKeepingUnderlyingMathGamePaused() {
        val data = readingFixture().copy(page = ReadingPage.MENU)
        val store = ReadingStore(File(application.filesDir, "reading_private.json"))
        store.load()
        store.save(data)
        val readingModel = ViewModelProvider(compose.activity,
            ViewModelProvider.AndroidViewModelFactory(application))[ReadingViewModel::class.java]
        var now = 0L
        val model = MathViewModel(application, clock = { now }, ioDispatcher = Dispatchers.Unconfined)
        model.selectOperation(Operation.ADDITION)
        model.submitSetup()
        model.start()
        now = 1_000
        model.openSettings()
        val game = model.game
        val elapsed = model.elapsedMs()
        model.openReadingProgress()
        compose.setContent { MathAppContent(model) }
        compose.waitUntil(5_000) { readingModel.data != null }
        compose.onNodeWithTag("reading-history-attempt-a").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            readingModel.data?.page == ReadingPage.ACTIVITY
        }
        assertEquals(Overlay.READING, model.overlay)
        compose.onNodeWithTag("reading-activity").assertIsDisplayed()
        now += 5_000
        assertEquals(elapsed, model.elapsedMs())
        assertEquals(game, model.game)
        assertEquals(data.active, readingModel.data?.withoutCatalogueRefs()?.active)
    }
}
