package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
import org.robolectric.annotation.GraphicsMode

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
    private val disabledGames = mutableStateOf<Set<ReadingGame>>(emptySet())

    @Before fun clean() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        listOf(file, File(file.path + ".bak"), File(file.path + ".new")).forEach { it.delete() }
    }

    private fun show(data: ReadingData, speech: Boolean = false, percent: Int = 100, history: Boolean = false,
                     disabled: Set<ReadingGame> = emptySet(), welcome: Boolean = false) {
        val store = ReadingStore(file)
        store.load()
        store.save(data)
        textSize.intValue = percent
        disabledGames.value = disabled
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
                if (history) ReadingHistoryContent(model)
                else ReadingModule(model, { exited++ }, textSize.intValue, { textSize.intValue = it },
                    narrationAvailable = speech, speak = { lastSpoken = it; speechWorks }, stop = { stopped++ },
                    disabledGames = disabledGames.value)
                if (welcome) AcademyWelcome(model)
            }
        }
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.data != null
        }
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
        assertEquals(ReadingGame.MISSING.guidance, lastSpoken)
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

    @Test fun missingWordHasInstructionsBelowTitleWithSpeakerOnRightAndBackAtTopLeft() {
        val data = readingFixture()
        val content = requireNotNull(data.active).content
        show(data, speech = true)
        val back = compose.onNodeWithTag("reading-back").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(content.game.title).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val instructions = compose.onNodeWithText(content.game.guidance).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val speaker = compose.onNodeWithContentDescription("Listen to instructions")
            .assertIsDisplayed().assertHasClickAction().fetchSemanticsNode().boundsInRoot
        val activityTitle = compose.onNodeWithText(content.title).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag("reading-activity").fetchSemanticsNode().boundsInRoot
        assertTrue(back.bottom <= title.top)
        assertEquals(title.left, back.left, 1f)
        assertTrue(title.bottom <= instructions.top)
        assertTrue(instructions.bottom <= activityTitle.top)
        assertTrue(instructions.right <= speaker.left)
        assertEquals(instructions.center.y, speaker.center.y, 1f)
        assertEquals(screen.right - (title.left - screen.left), speaker.right, 1f)
        compose.onNodeWithText("Listen to instructions").assertDoesNotExist()
        compose.onNodeWithText("${data.name("reader-a")} · ${content.level.label}").assertDoesNotExist()
        compose.onNodeWithText("Instruction audio does not count as passage narration.").assertDoesNotExist()
        compose.onNodeWithText("Draft saved automatically.", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Listening is support, not independent reading.", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("reading-listen").assertDoesNotExist()
        compose.onNodeWithTag("reading-stop").assertDoesNotExist()
        val previousStops = stopped
        compose.onNodeWithTag("reading-back").performClick()
        waitFor { it.page == ReadingPage.MENU }
        assertTrue(stopped > previousStops)
        assertEquals(data.attempts, ReadingStore(file).load().attempts)
    }

    @Test fun missingWordInstructionFailureCanBeRetriedWithoutRecordingPassageNarration() {
        val data = readingFixture()
        show(data, speech = true)
        speechWorks = false
        click("reading-instructions")
        compose.onNodeWithText("Instruction audio is unavailable. You can still read the instructions above.")
            .assertIsDisplayed()
        speechWorks = true
        click("reading-instructions")
        compose.onNodeWithText("Instruction audio is unavailable.", substring = true).assertDoesNotExist()
        assertEquals(ReadingGame.MISSING.guidance, lastSpoken)
        assertEquals(data, model.data)
        assertEquals(data, ReadingStore(file).load())
    }

    @Test fun missingWordRemainsPlayableWithoutOfflineInstructionAudio() {
        show(readingFixture())
        compose.onNodeWithContentDescription("Listen to instructions").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("reading-listen").assertDoesNotExist()
        compose.onNodeWithTag("reading-stop").assertDoesNotExist()
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.active?.earned("answer") == true }
        assertEquals(ReadingAccuracy(1, 1, 0, 0), model.data?.accuracy("reader-a"))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun missingWordAnswersHaveDistinctColorsAndAnEqualWidthTwoColumnGridWithAnOddLastRow() {
        show(readingFixture())
        compose.onNodeWithTag("reading-answer-0").performScrollTo()
        val answers = (0..2).map {
            compose.onNodeWithTag("reading-answer-$it").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }
        assertEquals(answers[0].top, answers[1].top, 1f)
        assertTrue(answers[0].right < answers[1].left)
        assertTrue(maxOf(answers[0].bottom, answers[1].bottom) < answers[2].top)
        assertEquals(answers[0].left, answers[2].left, 1f)
        answers.forEach { assertEquals(answers[0].width, it.width, 1f) }
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                val colors = answers.map {
                    bitmap.getPixel(it.center.x.toInt(), (it.top + 4 * view.resources.displayMetrics.density).toInt())
                }
                assertEquals(3, colors.toSet().size)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun missingWordFourChoiceGridAndInstructionsFitAtLargeTextSizes() {
        val content = ReadingContent.find("missing-generous")
        val choices = requireNotNull(content.question).choices
        show(readingFixture(content), speech = true)
        for (percent in listOf(100, 150, 200)) {
            compose.runOnIdle { textSize.intValue = percent }
            compose.onNodeWithTag("reading-instructions").performScrollTo().assertIsDisplayed()
            val instructions = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(content.game.guidance).performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
            ) { it(instructions) }
            assertFalse("Instructions at $percent%", instructions.single().hasVisualOverflow)
            choices.indices.chunked(2).forEach { row ->
                compose.onNodeWithTag("reading-answer-${row.first()}").performScrollTo()
                val answers = row.map { index ->
                    val bounds = compose.onNodeWithTag("reading-answer-$index").assertIsDisplayed().assertIsEnabled()
                        .fetchSemanticsNode().boundsInRoot
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    compose.onNodeWithText(choices[index]).performSemanticsAction(
                        androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                    ) { it(layouts) }
                    val layout = layouts.single()
                    assertFalse("${choices[index]} at $percent%", layout.didOverflowHeight)
                    // The paragraph can retain a wider constraint than the wrap-content Text.
                    repeat(layout.lineCount) { line ->
                        assertFalse(layout.isLineEllipsized(line))
                        assertTrue("${choices[index]} line $line at $percent%",
                            layout.getLineLeft(line) >= 0 && layout.getLineRight(line) <= layout.size.width + 1)
                    }
                    bounds
                }
                assertEquals(answers[0].top, answers[1].top, 1f)
                assertEquals(answers[0].width, answers[1].width, 1f)
                assertTrue(answers[0].right < answers[1].left)
            }
        }
        chooseCorrectAnswer()
        val answer = readingCorrect(requireNotNull(model.data))
        compose.onNodeWithTag("reading-answer-$answer").assertTextContains("✓ ${choices[answer]}")
        click("reading-check-answer")
        waitFor { it.active?.earned("answer") == true }
        choices.indices.forEach { compose.onNodeWithTag("reading-answer-$it").assertIsNotEnabled() }
    }

    @Test fun disablingReadingRewardsStillSavesCompletionWithoutShowingPointAwards() {
        application.getSharedPreferences("settings", 0).edit().putBoolean("reading_rewards_enabled", false).commit()
        show(readingFixture())
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.active?.earned("answer") == true }
        compose.onNodeWithText("Answer checkpoint saved").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Answer checkpoint earned · +1").assertDoesNotExist()
        click("reading-finish")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithText("Individual points this attempt: 0").performScrollTo().assertIsDisplayed()
        assertEquals(0, ReadingStore(file).load().points("reader-a"))
        assertEquals(1, model.data?.accuracy("reader-a")?.firstIndependent)
    }

    @Test fun settingsHistoryFiltersReadersWithoutChangingDraftOrSelectedProfile() {
        val data = readingFixture()
        show(data, history = true)
        compose.onNodeWithTag("reading-history-entry-attempt-a").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("In progress · resumable").assertIsDisplayed()
        click("reading-history-reader-reader-b")
        compose.onNodeWithTag("reading-history-entry-attempt-a").assertDoesNotExist()
        compose.onNodeWithText("No reading practice results yet for this reader.").performScrollTo().assertIsDisplayed()
        click("reading-history-reader-reader-a")
        compose.onNodeWithTag("reading-history-entry-attempt-a").performScrollTo().assertIsDisplayed()
        assertEquals(data, model.data)
        assertEquals(data, ReadingStore(file).load())
    }

    @Test fun settingsHistoryShowsClearEmptyStateWithoutCreatingAReader() {
        show(ReadingData(), history = true)
        compose.onNodeWithText("No reading practice results yet. Play a reading game to get started.")
            .assertIsDisplayed()
        assertTrue(requireNotNull(model.data).profiles.isEmpty())
    }

    @Test fun recommendedGamesAppearFirstInTheRequestedOrder() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        val recommended = listOf("reading-game-MISSING", "reading-game-SEQUENCE", "reading-game-STUDIO")
        val nodes = compose.onAllNodes(
            hasTestTag(recommended[0]) or hasTestTag(recommended[1]) or hasTestTag(recommended[2])
        ).fetchSemanticsNodes()
        assertEquals(recommended, nodes.map { it.config[SemanticsProperties.TestTag] })
    }

    @Test fun disabledGamesAreHiddenWithoutChangingRemainingTileOrder() {
        show(readingFixture().copy(page = ReadingPage.MENU),
            disabled = setOf(ReadingGame.MISSING, ReadingGame.CAPTION))
        compose.onNodeWithTag("reading-game-MISSING").assertDoesNotExist()
        compose.onNodeWithTag("reading-new-MISSING").assertDoesNotExist()
        compose.onNodeWithTag("reading-game-CAPTION").assertDoesNotExist()
        val nodes = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("Game tile") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("reading-game-")
        }).fetchSemanticsNodes()
        assertEquals(listOf("reading-game-SEQUENCE", "reading-game-STUDIO",
            "reading-game-SCRAMBLE", "reading-game-DETECTIVE",
            "reading-game-CHAPTER", "reading-game-PARTNERS"), nodes.map {
            it.config[SemanticsProperties.TestTag]
        })
        click("reading-game-SEQUENCE")
        waitFor { it.page == ReadingPage.ACTIVITY && it.active?.content?.game == ReadingGame.SEQUENCE }
    }

    @Test fun allGamesCanBeDisabledWithoutLosingReaderProgressOrNavigation() {
        val data = readingFixture().copy(page = ReadingPage.MENU)
        show(data, disabled = ReadingGame.entries.toSet())
        ReadingGame.entries.forEach { compose.onNodeWithTag("reading-game-${it.name}").assertDoesNotExist() }
        compose.onNodeWithText("No reading games are active. Enable games in Settings > Reading Settings > Active Reading Games.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start here · three recommended games").assertDoesNotExist()
        compose.onNodeWithText("More ways to read and create").assertDoesNotExist()
        assertEquals(data, ReadingStore(file).load())
        compose.onNodeWithTag("reading-progress").assertDoesNotExist()
        click("reading-exit")
        assertEquals(1, exited)
    }

    @Test fun disablingRestoredDraftBlocksPlayAndHistoryResumeUntilReenabledWithoutLosingCredit() {
        val fixture = readingFixture()
        val data = readingAnswer(fixture, readingCorrect(fixture))
        show(data, disabled = setOf(ReadingGame.MISSING))
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        compose.onNodeWithTag("reading-activity").assertDoesNotExist()
        assertEquals(data, ReadingStore(file).load())
        compose.runOnIdle { model.act { it.copy(page = ReadingPage.PROGRESS) } }
        waitFor { it.page == ReadingPage.PROGRESS }
        compose.onNodeWithTag("reading-history-attempt-a").performScrollTo().assertIsNotEnabled()
        assertEquals(data.attempts, model.data?.attempts)
        assertEquals(1, model.data?.points("reader-a"))
        compose.runOnIdle { disabledGames.value = emptySet() }
        compose.onNodeWithTag("reading-history-attempt-a").assertIsEnabled().performClick()
        waitFor { it.page == ReadingPage.ACTIVITY }
        compose.onNodeWithTag("reading-activity").assertIsDisplayed()
        assertEquals(data.active, model.data?.active)
        assertEquals(data.attempts, ReadingStore(file).load().attempts)
    }

    @Test fun disabledGameHistoryStillOpensSummaryButCannotStartNextActivity() {
        val data = readingSolve(readingFixture()).copy(page = ReadingPage.PROGRESS)
        show(data, disabled = setOf(ReadingGame.MISSING))
        click("reading-history-attempt-a")
        waitFor { it.page == ReadingPage.ACTIVITY }
        compose.onNodeWithTag("reading-summary").assertIsDisplayed()
        compose.onNodeWithTag("reading-next").performScrollTo().assertIsNotEnabled()
        assertEquals(data.attempts, ReadingStore(file).load().attempts)
        compose.runOnIdle { disabledGames.value = emptySet() }
        compose.onNodeWithTag("reading-next").assertIsEnabled().performClick()
        waitFor { it.active?.id != "attempt-a" }
        assertEquals(2, model.data?.attempts?.size)
        assertEquals(1, model.data?.points("reader-a"))
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

    @Test fun storyStudioSavesEachPartWithoutClaimingDisabledPoints() {
        application.getSharedPreferences("settings", 0).edit().putBoolean("reading_rewards_enabled", false).commit()
        show(readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()))
        ReadingEngine.studioKeys.forEach { key ->
            click("reading-studio-$key-0")
            waitFor { !it.active?.fields?.get(key).isNullOrBlank() }
            click("reading-save-$key")
            waitFor { it.active?.earned(key) == true }
            compose.onNodeWithText("$key saved").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("$key saved · +1").assertDoesNotExist()
        }
        click("reading-finish")
        waitFor { it.stories.size == 1 }
        compose.onNodeWithText("Individual points this attempt: 0").performScrollTo().assertIsDisplayed()
        assertEquals(0, ReadingStore(file).load().points("reader-a"))
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
        compose.onNodeWithTag("reading-resume-STUDIO").performClick()
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
            compose.runOnIdle { model.act { ReadingEngine.level(it, ReadingLevel.TRAIL) } }
            waitForBlockedSave()
            click("reading-game-SEQUENCE")
        }
        waitFor { it.page == ReadingPage.ACTIVITY && it.active?.content?.level == ReadingLevel.TRAIL }
        assertEquals(ReadingLevel.TRAIL, model.data?.profile?.level)
        assertEquals(ReadingLevel.SPROUT, model.data?.attempts?.first()?.content?.level)
        assertFalse(requireNotNull(model.data).attempts.first().abandoned)
    }

    @Test fun legacyProfilePageOpensMenuWithoutReaderManagementOrLosingDrafts() {
        val data = ReadingEngine.field(readingFixture(), "answer", "1").copy(page = ReadingPage.PROFILES)
        show(data)
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        compose.onNodeWithTag("reading-profiles").assertDoesNotExist()
        compose.onNodeWithTag("reading-add-reader").assertDoesNotExist()
        compose.onNodeWithTag("reading-level-SPROUT").assertDoesNotExist()
        compose.onNodeWithTag("reading-progress").assertDoesNotExist()
        click("reading-game-MISSING")
        compose.onNodeWithTag("reading-resume-MISSING").performClick()
        waitFor { it.page == ReadingPage.ACTIVITY }
        assertEquals("attempt-a", model.data?.active?.id)
        assertEquals("1", model.data?.active?.fields?.get("answer"))
        assertEquals(data.profiles, model.data?.profiles)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun welcomeRequiresAValidNameAndSavesExactlyOneLocalUser() {
        show(ReadingData(), welcome = true)
        compose.onNodeWithText("Welcome to the Academy! What is your name?").assertIsDisplayed()
        compose.onNodeWithTag("academy-save-name").assertIsNotEnabled()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("academy-welcome").assertIsDisplayed()
        compose.onNodeWithTag("academy-name").performTextInput("   ")
        compose.onNodeWithTag("academy-save-name").assertIsNotEnabled()
        compose.onNodeWithTag("academy-name").performTextReplacement("  Ari  ")
        compose.onNodeWithTag("academy-save-name").performClick()
        waitFor { it.profile?.name == "Ari" }
        compose.onNodeWithTag("academy-welcome").assertDoesNotExist()
        compose.runOnIdle { model.setName("Another name") }
        compose.waitForIdle()
        val saved = ReadingStore(file).load()
        assertEquals("Ari", saved.profiles.single().name)
        assertEquals(saved, model.data)
    }

    @Test fun existingNameSkipsWelcomeAndKeepsLegacyHistory() {
        val data = readingFixture().copy(page = ReadingPage.MENU)
        show(data, welcome = true)
        compose.onNodeWithTag("academy-welcome").assertDoesNotExist()
        assertEquals(data, model.data)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun welcomeAtLargeTextKeepsTheNameAcrossRecreation() {
        model = ReadingViewModel(application, ReadingStore(file))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme(textSizePercent = 200) { AcademyWelcome(model) } }
        waitFor { it.profiles.isEmpty() }
        compose.onNodeWithTag("academy-name").performScrollTo().performTextInput("Ari")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("academy-name").performScrollTo().assertTextContains("Ari")
        compose.onNodeWithTag("academy-save-name").performScrollTo().assertIsDisplayed().performClick()
        waitFor { it.profile?.name == "Ari" }
        assertEquals("Ari", ReadingStore(file).load().profiles.single().name)
    }

    @Test fun unreadableReadingDataDoesNotBlockTheRestOfTheAppWithOnboarding() {
        file.writeText("unreadable reading data")
        model = ReadingViewModel(application, ReadingStore(file))
        compose.setContent { MathTheme { AcademyWelcome(model) } }
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.storageError != null
        }
        compose.onNodeWithTag("academy-welcome").assertDoesNotExist()
        assertNull(model.data)
        assertEquals("unreadable reading data", file.readText())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun failedNameSaveCanBeDeferredWithoutCreatingAnotherUserOnRetry() {
        var failSave = true
        var saved = ReadingData()
        model = ReadingViewModel(application, object : ReadingStorage {
            override fun load() = saved
            override fun save(data: ReadingData) {
                if (failSave) throw java.io.IOException("Storage full")
                saved = data
            }
        })
        compose.setContent { MathTheme { AcademyWelcome(model) } }
        waitFor { it.profiles.isEmpty() }
        compose.onNodeWithTag("academy-name").performTextInput("Ari")
        compose.onNodeWithTag("academy-save-name").performClick()
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.storageError != null
        }
        compose.onNodeWithText("Continue without reading").performClick()
        compose.onNodeWithTag("academy-welcome").assertDoesNotExist()
        assertTrue(saved.profiles.isEmpty())
        compose.runOnIdle { failSave = false; model.retry() }
        waitFor { it.profile?.name == "Ari" }
        assertEquals(1, saved.profiles.size)
        compose.onNodeWithTag("academy-welcome").assertDoesNotExist()
    }

    @Test fun newAttemptRequiresExplicitDiscardConfirmationAndCancelKeepsDraft() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        click("reading-game-MISSING")
        compose.onNodeWithTag("reading-new-MISSING").performClick()
        compose.onNodeWithTag("reading-keep-draft").performClick()
        assertEquals("attempt-a", model.data?.active?.id)
        click("reading-game-MISSING")
        compose.onNodeWithTag("reading-new-MISSING").performClick()
        compose.onNodeWithTag("reading-confirm-new").performClick()
        waitFor { it.active?.id != "attempt-a" }
        assertTrue(requireNotNull(model.data).attempts.first().abandoned)
        assertEquals(2, model.data?.attempts?.size)
    }

    @Test fun partnersDialogSupportsFirstStartResumeAndCompletionWithoutAnotherReader() {
        val data = ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a")
        show(data)
        click("reading-game-PARTNERS")
        compose.onNodeWithTag("reading-resume-PARTNERS").assertIsNotEnabled()
        compose.onNodeWithTag("reading-new-PARTNERS").performClick()
        waitFor { it.active?.content?.game == ReadingGame.PARTNERS }
        repeat(4) { index ->
            click("reading-turn-choice-0")
            waitFor { !it.active?.fields?.get("turn").isNullOrBlank() }
            click("reading-save-turn")
            waitFor { it.active?.turns?.size == index + 1 }
        }
        val draft = requireNotNull(model.data).active
        click("reading-back")
        waitFor { it.page == ReadingPage.MENU }
        click("reading-game-PARTNERS")
        compose.onNodeWithText("Resume Story Builder Partners").assertIsDisplayed()
        compose.onNodeWithText("Start a new attempt instead").assertIsDisplayed()
        compose.onNodeWithTag("reading-resume-PARTNERS").performClick()
        waitFor { it.page == ReadingPage.ACTIVITY }
        assertEquals(draft, model.data?.active)
        repeat(4) { index ->
            click("reading-tile-$index")
            waitFor { it.active?.order?.size == index + 1 }
        }
        click("reading-reread")
        waitFor { it.active?.reread == true }
        click("reading-shared")
        waitFor { it.active?.earned("shared") == true }
        click("reading-finish")
        waitFor { it.stories.size == 1 }
        assertEquals(1, model.data?.profiles?.size)
        assertEquals(1, model.data?.points("reader-a"))
        assertEquals(1, model.data?.sharedPoints("reader-a"))
        assertEquals(model.data, ReadingStore(file).load())
    }

    @Test fun partnersNewAttemptArchivesDraftWithoutLosingParticipationPoints() {
        var data = ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a")
        data = ReadingEngine.start(data, ReadingGame.PARTNERS, ReadingLevel.SPROUT, id = "old-story")
        data = ReadingEngine.submit(ReadingEngine.field(data, "turn", "A fox set out.", expectedTurn = 0),
            "turn", expectedTurn = 0)
        show(data.copy(page = ReadingPage.MENU))
        click("reading-game-PARTNERS")
        compose.onNodeWithTag("reading-new-PARTNERS").performClick()
        compose.onNodeWithTag("reading-confirm-new").performClick()
        waitFor { it.active?.id != "old-story" }
        assertTrue(requireNotNull(model.data).attempts.first().abandoned)
        assertEquals(data.attempts.first().checkpoints, model.data?.attempts?.first()?.checkpoints)
        assertEquals(1, model.data?.points("reader-a"))
        assertEquals(1, model.data?.profiles?.size)
        assertEquals(model.data, ReadingStore(file).load())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun readingTilesStaySquareAndLabelsFitAtLargeTextSizes() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        for (percent in listOf(100, 130, 150, 200)) {
            compose.runOnIdle { textSize.intValue = percent }
            ReadingGame.entries.forEach { game ->
                val bounds = compose.onNodeWithTag("reading-game-${game.name}").performScrollTo()
                    .assertHasClickAction().fetchSemanticsNode().boundsInRoot
                assertEquals(bounds.width, bounds.height, 1f)
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithText(game.title).performSemanticsAction(
                    androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                ) { it(layouts) }
                assertFalse("${game.title} at $percent%", layouts.single().hasVisualOverflow)
            }
        }
    }

    @Test fun otherReadingGamesRetainNarrationSupportAndStopControls() {
        show(readingFixture(ReadingContent.forGame(ReadingGame.CAPTION, ReadingLevel.SPROUT).first()), speech = true)
        compose.onNodeWithText("Listen to instructions").performScrollTo().assertIsDisplayed()
        click("reading-listen")
        waitFor { it.active?.narrations == 1 }
        val previousStops = stopped
        click("reading-stop")
        assertTrue(stopped > previousStops)
        chooseCorrectAnswer()
        click("reading-check-answer")
        waitFor { it.points("reader-a") == 1 }
        assertEquals(ReadingAccuracy(0, 1, 1, 0), model.data?.accuracy("reader-a"))

        compose.runOnIdle {
            speechWorks = false
            model.act { ReadingEngine.start(it, ReadingGame.CAPTION, ReadingLevel.SPROUT, true, "fresh") }
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
        compose.runOnIdle { model.act { it.copy(page = ReadingPage.PROGRESS) } }
        waitFor { it.page == ReadingPage.PROGRESS }
        compose.onNodeWithTag("reading-text-larger").performScrollTo().assertIsNotEnabled()
        click("reading-text-smaller")
        assertEquals(190, textSize.intValue)
        compose.onNodeWithTag("reading-back").performScrollTo().assertIsDisplayed()
    }
}
