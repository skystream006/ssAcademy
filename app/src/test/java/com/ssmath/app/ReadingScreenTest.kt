package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class ReadingScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "reading_compose_test.json")
    private lateinit var model: ReadingViewModel
    private var exited = 0
    private var stopped = 0
    private var speechWorks = true
    private var lastSpoken = ""
    @Volatile private var saveGate: CountDownLatch? = null
    @Volatile private var saveEntered: CountDownLatch? = null
    private val textSize = mutableIntStateOf(100)

    @Before fun clean() {
        listOf(file, File(file.path + ".bak"), File(file.path + ".new")).forEach { it.delete() }
    }

    private fun show(data: ReadingData, speech: Boolean = false, percent: Int = 100) {
        val store = ReadingStore(file)
        store.load()
        store.save(data)
        textSize.intValue = percent
        model = ReadingViewModel(application, object : ReadingStorage {
            override fun load() = store.load()
            override fun save(data: ReadingData) {
                val gate = saveGate
                saveEntered?.countDown()
                check(gate?.await(5, TimeUnit.SECONDS) != false)
                store.save(data)
            }
        })
        compose.setContent {
            MathTheme(textSizePercent = textSize.intValue) {
                ReadingModule(model, { exited++ }, textSize.intValue, { textSize.intValue = it },
                    narrationAvailable = speech, speak = { lastSpoken = it; speechWorks }, stop = { stopped++ })
            }
        }
        compose.waitUntil(5_000) { model.data != null }
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun waitFor(condition: (ReadingData) -> Boolean) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.data?.let(condition) == true && !model.saving
        }
        compose.waitForIdle()
    }

    private fun chooseCorrectAnswer() {
        val answer = readingCorrect(requireNotNull(model.data))
        click("reading-answer-$answer")
        waitFor { it.active?.fields?.get("answer") == answer.toString() }
    }

    private fun delayedWrites(block: () -> Unit) {
        val gate = CountDownLatch(1)
        saveEntered = CountDownLatch(1)
        saveGate = gate
        try {
            block()
        } finally {
            saveGate = null
            gate.countDown()
        }
    }

    private fun waitForBlockedSave() {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            saveEntered?.count == 0L
        }
    }

    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    private fun assertPresetThenImmediateTyping(data: ReadingData, presetTag: String, key: String, preset: String) {
        show(data)
        delayedWrites {
            click(presetTag)
            waitForBlockedSave()
            val editor = compose.onNodeWithTag("reading-write-$key").performScrollTo()
            editor.assertTextContains(preset)
            editor.performTextInputSelection(TextRange(preset.length))
            editor.performTextInput("!")
            editor.assertTextContains("$preset!")
            assertEquals(data.active?.fields?.get(key), model.data?.active?.fields?.get(key))
        }
        waitFor { it.active?.fields?.get(key) == "$preset!" }
        assertEquals("$preset!", ReadingStore(file).load().active?.fields?.get(key))
    }

    @Test fun missingWordCanBeCompletedAndBackReturnsToMenuThenExits() {
        show(readingFixture(), speech = true)
        click("reading-instructions")
        assertEquals(0, model.data?.active?.narrations)
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.points("reader-a") == 1 }
        compose.onNodeWithTag("reading-check-answer").performScrollTo().assertIsNotEnabled()
        click("reading-finish")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithTag("reading-summary").assertIsDisplayed()
        compose.onNodeWithText("First-try independent: 1 / 1").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { it.page == ReadingPage.MENU }
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(1, exited)
        assertTrue(stopped > 0)
        assertEquals(1, ReadingStore(file).load().points("reader-a"))
    }

    @Test fun recommendedGamesAppearFirstInTheRequestedOrder() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        val recommended = listOf("reading-game-MISSING", "reading-game-SEQUENCE", "reading-game-STUDIO")
        val nodes = compose.onAllNodes(
            hasTestTag(recommended[0]) or hasTestTag(recommended[1]) or hasTestTag(recommended[2])
        ).fetchSemanticsNodes()
        assertEquals(recommended, nodes.map { it.config[SemanticsProperties.TestTag] })
    }

    @Test fun sequencerSupportsTapRemovalOrderedEventsAndComprehension() {
        show(readingFixture(ReadingContent.find("sequence-seed")))
        click("reading-tile-0")
        waitFor { it.active?.order == listOf(0) }
        click("reading-placed-0")
        waitFor { it.active?.order?.isEmpty() == true }
        listOf(1, 2, 0).forEachIndexed { index, tile ->
            click("reading-tile-$tile")
            waitFor { it.active?.order?.size == index + 1 }
        }
        click("reading-check-order")
        waitFor { it.active?.earned("order") == true }
        compose.onNodeWithTag("reading-placed-0").performScrollTo().assertIsNotEnabled()
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.points("reader-a") == 2 }
        click("reading-finish")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithTag("reading-summary").assertIsDisplayed()
        assertEquals(2, requireNotNull(model.data).accuracy("reader-a").firstIndependent)
    }

    @Test fun storyStudioCompletesFourCheckpointsAndSavesReadableIllustratedStory() {
        show(readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()))
        ReadingEngine.studioKeys.forEach { key ->
            click("reading-studio-$key-0")
            waitFor { !it.active?.fields?.get(key).isNullOrBlank() }
            click("reading-save-$key")
            waitFor { it.active?.earned(key) == true }
            compose.onNodeWithTag("reading-write-$key").performScrollTo().assertIsNotEnabled()
        }
        click("reading-finish")
        waitFor { it.stories.size == 1 }
        compose.onNodeWithTag("reading-saved-story").performScrollTo().assertIsDisplayed()
        assertEquals(4, model.data?.points("reader-a"))
        assertEquals(0, model.data?.accuracy("reader-a")?.attempted)
        assertTrue(requireNotNull(model.data).stories.single().text.contains("A kind fox"))
        assertEquals(model.data?.stories, ReadingStore(file).load().stories)
    }

    @Test fun typedStoryDraftKeepsRapidCharactersAndRestoresAfterLeavingActivity() {
        show(readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()))
        val editor = compose.onNodeWithTag("reading-write-character").performScrollTo()
        editor.performTextInput("A")
        editor.performTextInput(" curious")
        editor.performTextInput(" fox")
        waitFor { it.active?.fields?.get("character") == "A curious fox" }
        editor.assertTextContains("A curious fox")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { it.page == ReadingPage.MENU }
        click("reading-game-STUDIO")
        waitFor { it.page == ReadingPage.ACTIVITY }
        compose.onNodeWithTag("reading-write-character").performScrollTo().assertTextContains("A curious fox")
        assertEquals("A curious fox", ReadingStore(file).load().active?.fields?.get("character"))
    }

    @Test fun studioPresetAndImmediateTypingShareTheSameBufferDuringSlowSave() {
        val data = ReadingEngine.field(
            readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()),
            "character", "A kind fox")
        assertPresetThenImmediateTyping(data, "reading-studio-character-1", "character", "A tiny robot")
    }

    @Test fun studioLockRestoresCanonicalTextAfterEditsQueuedBehindASlowCheckpointSave() {
        val data = ReadingEngine.field(
            readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()),
            "character", "  A kind fox  ")
        show(data)
        delayedWrites {
            click("reading-save-character")
            waitForBlockedSave()
            click("reading-studio-character-1")
            val editor = compose.onNodeWithTag("reading-write-character").performScrollTo()
            editor.assertTextContains("A tiny robot")
            editor.performTextInput("!")
        }
        waitFor { it.active?.earned("character") == true }
        compose.onNodeWithTag("reading-write-character").performScrollTo()
            .assertIsNotEnabled().assertTextContains("A kind fox")
        assertEquals("A kind fox", model.data?.active?.fields?.get("character"))
        ReadingEngine.studioKeys.drop(1).forEach { key ->
            click("reading-studio-$key-0")
            waitFor { !it.active?.fields?.get(key).isNullOrBlank() }
            click("reading-save-$key")
            waitFor { it.active?.earned(key) == true }
        }
        click("reading-finish")
        waitFor { it.stories.size == 1 }
        val story = requireNotNull(model.data).stories.single().text
        assertTrue(story.startsWith("A kind fox lived in "))
        assertFalse(story.contains("robot"))
        assertEquals(story, ReadingStore(file).load().stories.single().text)
    }

    @Test fun partnerPresetAndImmediateTypingShareTheSameTurnBufferDuringSlowSave() {
        var data = readingFixture(ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first())
        data = ReadingEngine.partner(data, "reader-b")
        data = ReadingEngine.field(data, "turn", "An old sentence.", expectedTurn = 0)
        assertPresetThenImmediateTyping(data, "reading-turn-choice-1", "turn", "A tiny robot began an adventure.")
    }

    @Test fun gameStartUsesTheQueuedLevelSelectionEvenBeforeItsSaveFinishes() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        delayedWrites {
            click("reading-level-TRAIL")
            waitForBlockedSave()
            click("reading-game-MISSING")
        }
        waitFor { it.page == ReadingPage.ACTIVITY && it.active?.content?.level == ReadingLevel.TRAIL }
        assertEquals(ReadingLevel.TRAIL, model.data?.profile?.level)
        assertEquals(ReadingLevel.SPROUT, model.data?.attempts?.first()?.content?.level)
        assertFalse(requireNotNull(model.data).attempts.first().abandoned)
    }

    @Test fun localProfilesSwitchWithoutLosingDraftsAndDashboardOnlyShowsSelectedReader() {
        val data = ReadingEngine.field(readingFixture(), "answer", "1").copy(page = ReadingPage.MENU)
        show(data)
        click("reading-profiles")
        waitFor { it.page == ReadingPage.PROFILES }
        click("reading-profile-reader-b")
        waitFor { it.selectedProfileId == "reader-b" }
        click("reading-progress")
        waitFor { it.page == ReadingPage.PROGRESS }
        compose.onNodeWithTag("reading-dashboard").assertIsDisplayed()
        compose.onNodeWithText("Reader: Bo. Only this reader's history and shared participation are shown.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reading-history-attempt-a").assertDoesNotExist()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { it.page == ReadingPage.MENU }
        click("reading-profiles")
        waitFor { it.page == ReadingPage.PROFILES }
        click("reading-profile-reader-a")
        waitFor { it.selectedProfileId == "reader-a" }
        click("reading-game-MISSING")
        waitFor { it.page == ReadingPage.ACTIVITY }
        assertEquals("attempt-a", model.data?.active?.id)
        assertEquals("1", model.data?.active?.fields?.get("answer"))
    }

    @Test fun addingReaderTrimsNameAndHardwareBackFromProfilesKeepsMenu() {
        show(ReadingData(page = ReadingPage.PROFILES))
        compose.onNodeWithTag("reading-reader-name").performScrollTo().performTextInput("  New reader  ")
        click("reading-add-reader")
        waitFor { it.profile?.name == "New reader" }
        click("reading-profiles")
        waitFor { it.page == ReadingPage.PROFILES }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { it.page == ReadingPage.MENU }
        assertEquals(0, exited)
    }

    @Test fun newAttemptRequiresExplicitDiscardConfirmationAndCancelKeepsDraft() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        click("reading-new-MISSING")
        compose.onNodeWithTag("reading-keep-draft").performClick()
        assertEquals("attempt-a", model.data?.active?.id)
        click("reading-new-MISSING")
        compose.onNodeWithTag("reading-confirm-new").performClick()
        waitFor { it.active?.id != "attempt-a" }
        assertTrue(requireNotNull(model.data).attempts.first().abandoned)
        assertEquals(2, model.data?.attempts?.size)
    }

    @Test fun successfulNarrationIsSupportBeforeCheckingAndUnavailableSpeechDoesNotCount() {
        show(readingFixture(), speech = true)
        click("reading-listen")
        waitFor { it.active?.narrations == 1 }
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.points("reader-a") == 1 }
        assertEquals(ReadingAccuracy(0, 1, 1, 0), model.data?.accuracy("reader-a"))

        compose.runOnIdle {
            speechWorks = false
            model.act { ReadingEngine.start(it, ReadingGame.MISSING, ReadingLevel.SPROUT, true, "fresh") }
        }
        waitFor { it.active?.id == "fresh" }
        click("reading-listen")
        compose.onNodeWithText("Offline narration is unavailable. Reading and every activity still work without it.")
            .performScrollTo().assertIsDisplayed()
        assertEquals(0, model.data?.active?.narrations)
    }

    @Test fun partnerNarrationFollowsTheArrangedOrderNotTheAuthoredTurnOrder() {
        var data = readingFixture(ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first())
        data = ReadingEngine.partner(data, "reader-b")
        repeat(4) { index ->
            data = ReadingEngine.field(data, "turn", "Sentence $index.", expectedTurn = index)
            data = ReadingEngine.submit(data, "turn", expectedTurn = index)
        }
        data = readingOrder(data, listOf(3, 1, 0, 2))
        show(data, speech = true)
        click("reading-listen")
        waitFor { it.active?.narrations == 1 }
        assertEquals("Sentence 3. Sentence 1. Sentence 0. Sentence 2.", lastSpoken)
    }

    @Test fun twoHundredPercentTextHasReachableControlsAndLargeTouchTargets() {
        show(readingFixture(), percent = 200)
        val answer = readingCorrect(requireNotNull(model.data))
        val button = compose.onNodeWithTag("reading-answer-$answer").performScrollTo()
            .assertIsDisplayed().assertIsEnabled().fetchSemanticsNode().boundsInRoot
        assertTrue(button.height >= 48)
        assertTrue(button.width > 0 && button.width <= compose.activity.resources.displayMetrics.widthPixels)
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.active?.earned("answer") == true }
        click("reading-finish")
        waitFor { it.active?.finishedAt != null }
        click("reading-back")
        waitFor { it.page == ReadingPage.MENU }
        click("reading-progress")
        waitFor { it.page == ReadingPage.PROGRESS }
        compose.onNodeWithTag("reading-text-larger").performScrollTo().assertIsNotEnabled()
        click("reading-text-smaller")
        assertEquals(190, textSize.intValue)
        compose.onNodeWithTag("reading-back").performScrollTo().assertIsDisplayed()
    }
}
