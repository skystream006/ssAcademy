package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
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
import org.robolectric.shadows.ShadowToast

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
        click("reading-submit-answer")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithText("Check answer").assertDoesNotExist()
        compose.onNodeWithText("Finish and save").assertDoesNotExist()
        compose.onNodeWithTag("reading-summary").assertIsDisplayed()
        compose.onNodeWithText("First-try independent: 1 / 1").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { it.page == ReadingPage.MENU }
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(1, exited)
        assertTrue(stopped > 0)
        assertTrue(requireNotNull(ReadingStore(file).load().active).earned("answer"))
    }

    private fun assertSimplifiedActivityHeader(data: ReadingData) {
        val content = requireNotNull(data.active).content
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
    }

    @Test fun allReadingGamesHaveInstructionsBelowTitleWithSpeakerOnRightAndBackAtTopLeft() {
        show(readingFixture(), speech = true)
        ReadingGame.entries.forEach { game ->
            compose.runOnIdle { model.act { ReadingEngine.start(it, game, ReadingLevel.SPROUT) } }
            waitFor { it.page == ReadingPage.ACTIVITY && it.active?.content?.game == game }
            val data = requireNotNull(model.data)
            assertSimplifiedActivityHeader(data)
            click("reading-instructions")
            assertEquals(game.guidance, lastSpoken)
            assertEquals(data, model.data)
            val previousStops = stopped
            click("reading-back")
            waitFor { it.page == ReadingPage.MENU }
            assertTrue(stopped > previousStops)
            assertEquals(data.attempts, ReadingStore(file).load().attempts)
        }
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
        click("reading-submit-answer")
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
        click("reading-submit-answer")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithTag("reading-summary").assertIsDisplayed()
    }

    private fun choiceColor(tag: String): Int {
        val bounds = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                bitmap.getPixel(bounds.center.x.toInt(),
                    (bounds.top + 4 * view.resources.displayMetrics.density).toInt())
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun assertChoiceGrid(prefix: String, choices: List<String>, pictured: Boolean = false) {
        choices.indices.chunked(2).forEach { row ->
            compose.onNodeWithTag("$prefix-${row.first()}").performScrollTo()
            val nodes = row.map { index ->
                val node = compose.onNodeWithTag("$prefix-$index").assertIsEnabled().fetchSemanticsNode()
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithText(choices[index], useUnmergedTree = true).performSemanticsAction(
                    androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                ) { it(layouts) }
                val layout = layouts.single()
                assertFalse("$prefix-$index", layout.didOverflowHeight)
                repeat(layout.lineCount) { line ->
                    assertFalse(layout.isLineEllipsized(line))
                    assertTrue("$prefix-$index line $line",
                        layout.getLineLeft(line) >= 0 && layout.getLineRight(line) <= layout.size.width + 1)
                }
                node
            }
            if (nodes.size == 2) {
                assertEquals(nodes[0].size.width.toFloat(), nodes[1].size.width.toFloat(), 1f)
                assertTrue(nodes[0].positionInRoot.x + nodes[0].size.width < nodes[1].positionInRoot.x)
                if (!pictured) assertEquals(nodes[0].positionInRoot.y, nodes[1].positionInRoot.y, 1f)
            } else {
                val first = compose.onNodeWithTag("$prefix-0").fetchSemanticsNode()
                assertEquals(first.size.width.toFloat(), nodes.single().size.width.toFloat(), 1f)
                assertEquals(first.positionInRoot.x, nodes.single().positionInRoot.x, 1f)
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun everyLegacyReadingGameUsesColoredTwoColumnChoicesThatFitAtLargeTextSizes() {
        show(readingFixture(), speech = true)
        for (level in listOf(ReadingLevel.SPROUT, ReadingLevel.TRAIL)) {
            ReadingGame.entries.forEach { game ->
                compose.runOnIdle { model.act { ReadingEngine.start(it, game, level) } }
                waitFor { it.active?.content?.game == game && it.active?.content?.level == level }
                val content = requireNotNull(model.data?.active).content
                val (prefix, choices) = when (game) {
                    ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.DETECTIVE ->
                        "reading-answer" to requireNotNull(content.question).choices
                    ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> "reading-tile" to content.tiles
                    ReadingGame.CHAPTER -> "reading-branch" to content.branches.map { it.label }
                    ReadingGame.STUDIO -> "reading-studio-character" to content.writingChoices.first()
                    ReadingGame.PARTNERS -> "reading-turn-choice" to content.writingChoices.first().map {
                        "$it began an adventure."
                    }
                }
                compose.runOnIdle { textSize.intValue = 100 }
                assertEquals(choices.size.coerceAtMost(4),
                    choices.indices.map { choiceColor("$prefix-$it") }.toSet().size)
                for (percent in listOf(100, 200)) {
                    compose.runOnIdle { textSize.intValue = percent }
                    compose.onNodeWithTag("reading-instructions").performScrollTo().assertIsDisplayed()
                    val instructions = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    compose.onNodeWithText(game.guidance).performSemanticsAction(
                        androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                    ) { it(instructions) }
                    assertFalse("$game instructions at $percent%", instructions.single().hasVisualOverflow)
                    assertChoiceGrid(prefix, choices, pictured = content.eventPictures.isNotEmpty())
                }
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun sequencingTilesKeepTheirPictureAndColorWhenChoicesReflow() {
        val content = ReadingContent.find("sequence-seed")
        show(readingFixture(content))
        val color = choiceColor("reading-tile-2")
        click("reading-tile-0")
        waitFor { it.active?.order == listOf(0) }
        compose.onNodeWithTag("reading-tile-0").assertDoesNotExist()
        compose.onNodeWithContentDescription(content.eventPictures[0].description).assertDoesNotExist()
        assertEquals(color, choiceColor("reading-tile-2"))
        val picture = compose.onNodeWithContentDescription(content.eventPictures[2].description)
            .fetchSemanticsNode()
        val tile = compose.onNodeWithTag("reading-tile-2").fetchSemanticsNode()
        assertTrue(picture.positionInRoot.y < tile.positionInRoot.y)
        assertEquals(picture.positionInRoot.x, tile.positionInRoot.x, 1f)
        click("reading-placed-0")
        waitFor { it.active?.order?.isEmpty() == true }
        compose.onNodeWithTag("reading-tile-0").assertExists()
        assertEquals(color, choiceColor("reading-tile-2"))
    }

    private fun dragTile(sourceTag: String, targetTag: String, cancelled: Boolean = false) {
        compose.onNodeWithTag(sourceTag).performScrollTo()
        compose.onNodeWithTag(targetTag).performScrollTo()
        val source = compose.onNodeWithTag(sourceTag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.center
        val target = compose.onNodeWithTag(targetTag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.center
        val origin = compose.onRoot().fetchSemanticsNode().boundsInRoot.topLeft
        compose.onRoot().performTouchInput {
            down(source - origin)
            advanceEventTime(600)
            moveTo(source - origin)
            repeat(12) { step ->
                moveTo(source + (target - source) * ((step + 1) / 12f) - origin, delayMillis = 16)
            }
            if (cancelled) cancel() else up()
        }
    }

    @Test fun sentenceTilesDragIntoAnyEmptySpaceBetweenSpacesAndBackOut() {
        show(readingFixture(ReadingContent.find("scramble-cats")))
        compose.onNodeWithTag("reading-submit-order").performScrollTo().assertIsNotEnabled()
        dragTile("reading-tile-0", "reading-space-2")
        waitFor { it.active?.order == listOf(-1, -1, 0) }
        compose.onNodeWithTag("reading-tile-0").assertDoesNotExist()
        compose.onNodeWithTag("reading-submit-order").assertIsNotEnabled()
        assertEquals(listOf(-1, -1, 0), ReadingStore(file).load().active?.order)
        dragTile("reading-tile-1", "reading-space-0")
        waitFor { it.active?.order == listOf(1, -1, 0) }
        dragTile("reading-placed-2", "reading-space-1")
        waitFor { it.active?.order == listOf(1, 0) }
        compose.onNodeWithTag("reading-placed-2").assertDoesNotExist()
        dragTile("reading-placed-1", "reading-tile-bank")
        waitFor { it.active?.order == listOf(1) }
        compose.onNodeWithTag("reading-tile-0").assertExists()
        dragTile("reading-tile-0", "reading-space-2")
        waitFor { it.active?.order == listOf(1, -1, 0) }
        dragTile("reading-tile-2", "reading-space-1")
        waitFor { it.active?.order == listOf(1, 2, 0) }
        dragTile("reading-placed-1", "reading-tile-bank")
        waitFor { it.active?.order == listOf(1, -1, 0) }
        dragTile("reading-tile-2", "reading-space-1")
        waitFor { it.active?.order == listOf(1, 2, 0) }
        click("reading-submit-order")
        waitFor { it.active?.finishedAt != null }
        val locked = model.data
        compose.onNodeWithTag("reading-placed-0").assertDoesNotExist()
        compose.runOnIdle { model.act { ReadingEngine.removeTile(it, 0) } }
        assertEquals(locked, model.data)
        assertTrue(requireNotNull(ReadingStore(file).load().active).earned("order"))
    }

    @Test fun invalidAndCancelledSentenceDropsLeaveTheSavedOrderUnchanged() {
        val data = ReadingEngine.placeTileInSpace(readingFixture(ReadingContent.find("scramble-cats")), 1, 0)
        show(data)
        dragTile("reading-tile-0", "reading-space-0")
        assertEquals(data, model.data)
        dragTile("reading-tile-0", "reading-space-1", cancelled = true)
        assertEquals(data, model.data)
        dragTile("reading-placed-0", "reading-submit-order")
        assertEquals(data, model.data)
        assertEquals(data, ReadingStore(file).load())
        compose.onNodeWithTag("reading-tile-0").assertExists()
        compose.onNodeWithTag("reading-drag-preview").assertDoesNotExist()
    }

    @Test fun sparseSentenceDraftRestoresAndTapControlsFillItsFirstEmptySpace() {
        val data = ReadingEngine.level(ReadingEngine.placeTileInSpace(
            readingFixture(ReadingContent.find("scramble-lantern")), 3, 3), ReadingLevel.TRAIL)
        show(data, percent = 200)
        compose.onNodeWithTag("reading-space-0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reading-placed-3").performScrollTo().assertTextContains("softly")
        click("reading-tile-2")
        waitFor { it.active?.order == listOf(2, -1, -1, 3) }
        click("reading-tile-1")
        waitFor { it.active?.order == listOf(2, 1, -1, 3) }
        click("reading-placed-0")
        waitFor { it.active?.order == listOf(-1, 1, -1, 3) }
        click("reading-back")
        waitFor { it.page == ReadingPage.MENU }
        click("reading-resume-SCRAMBLE")
        waitFor { it.page == ReadingPage.ACTIVITY }
        assertEquals(listOf(-1, 1, -1, 3), model.data?.active?.order)
        click("reading-tile-2")
        waitFor { it.active?.order == listOf(2, 1, -1, 3) }
        click("reading-tile-0")
        waitFor { it.active?.order == listOf(2, 1, 0, 3) }
        click("reading-submit-order")
        waitFor { it.active?.earned("order") == true }
        assertTrue(requireNotNull(ReadingStore(file).load().active).earned("order"))
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun sentenceDragScrollsTowardOffscreenSpacesAndCancellationKeepsTheDraft() {
        val data = readingFixture(ReadingContent.find("scramble-lantern"))
        show(data, percent = 200)
        compose.onNodeWithTag("reading-tile-3").performScrollTo()
        val source = compose.onNodeWithTag("reading-tile-3").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot.center
        val viewport = compose.onNodeWithTag("reading-activity").fetchSemanticsNode().boundsInRoot
        fun scrollOffset() = compose.onNodeWithTag("reading-activity").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = scrollOffset()
        assertTrue(before > 0)
        compose.mainClock.autoAdvance = false
        try {
            compose.onRoot().performTouchInput {
                down(source)
                advanceEventTime(600)
                moveTo(source)
                moveTo(Offset(source.x, viewport.top + 1f), delayMillis = 200)
            }
            compose.mainClock.advanceTimeBy(1_000)
            assertTrue("Dragging at the top should reveal earlier spaces", scrollOffset() < before)
            compose.onRoot().performTouchInput { cancel() }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
        assertEquals(data, model.data)
        assertEquals(data, ReadingStore(file).load())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun stagedAnswerGridsKeepChapterAndOrderCheckpointsLocked() {
        show(readingFixture())
        for (game in listOf(ReadingGame.CHAPTER, ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE)) {
            compose.runOnIdle { model.act { ReadingEngine.start(it, game, ReadingLevel.TRAIL) } }
            waitFor { it.active?.content?.game == game }
            val content = requireNotNull(model.data?.active).content
            compose.onNodeWithTag("reading-answer-0").assertDoesNotExist()
            if (game == ReadingGame.CHAPTER) {
                click("reading-branch-1")
                waitFor { it.active?.branch == 1 }
                content.branches.indices.forEach {
                    compose.onNodeWithTag("reading-branch-$it").assertIsNotEnabled()
                }
            } else {
                content.validOrders.first().forEachIndexed { position, tile ->
                    click("reading-tile-$tile")
                    waitFor { it.active?.order?.size == position + 1 }
                }
                click("reading-submit-order")
                waitFor { it.active?.earned("order") == true }
                if (game == ReadingGame.SCRAMBLE) {
                    compose.onNodeWithTag("reading-summary").assertIsDisplayed()
                    assertNotNull(model.data?.active?.finishedAt)
                    continue
                }
                compose.onNodeWithTag("reading-placed-0").assertIsNotEnabled()
            }
            val question = requireNotNull(ReadingEngine.question(requireNotNull(model.data?.active)))
            assertChoiceGrid("reading-answer", question.choices)
            chooseCorrectAnswer()
            click("reading-submit-answer")
            waitFor { it.active?.finishedAt != null }
            question.choices.indices.forEach { compose.onNodeWithTag("reading-answer-$it").assertDoesNotExist() }
        }
    }

    @Test fun disablingReadingRewardsStillSavesCompletionWithoutShowingPointAwards() {
        application.getSharedPreferences("settings", 0).edit().putBoolean("reading_rewards_enabled", false).commit()
        show(readingFixture())
        chooseCorrectAnswer()
        click("reading-submit-answer")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithText("Answer checkpoint earned · +1").assertDoesNotExist()
        compose.onNodeWithText("Individual points this attempt:", substring = true).assertDoesNotExist()
        assertEquals(1, ReadingStore(file).load().completions("reader-a"))
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
        click("reading-question-count-15")
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
        assertTrue(requireNotNull(model.data?.active).earned("answer"))
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
        waitFor { it.page == ReadingPage.MENU }
        assertEquals(data.attempts, model.data?.attempts)
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
        click("reading-submit-order")
        waitFor { it.active?.earned("order") == true }
        compose.onNodeWithTag("reading-placed-0").performScrollTo().assertIsNotEnabled()
        chooseCorrectAnswer()
        click("reading-submit-answer")
        waitFor { it.active?.earned("answer") == true && it.active?.earned("order") == true }
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
            if (key != "ending") compose.onNodeWithTag("reading-write-$key").performScrollTo().assertIsNotEnabled()
        }
        waitFor { it.stories.size == 1 }
        compose.onNodeWithTag("reading-saved-story").performScrollTo().assertIsDisplayed()
        assertEquals(4, model.data?.active?.checkpoints?.values?.count { it.earned })
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
            if (key != "ending") compose.onNodeWithText("$key saved").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("$key saved · +1").assertDoesNotExist()
        }
        waitFor { it.stories.size == 1 }
        compose.onNodeWithText("Individual points this attempt:", substring = true).assertDoesNotExist()
        assertEquals(1, ReadingStore(file).load().stories.size)
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
        click("reading-resume-STUDIO")
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

    @Test fun gameStartWaitsForTheQueuedLevelSelectionToSave() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        click("reading-question-count-15")
        delayedWrites {
            compose.runOnIdle { model.act { ReadingEngine.level(it, ReadingLevel.TRAIL) } }
            waitForBlockedSave()
            compose.onNodeWithTag("reading-game-SEQUENCE").assertIsNotEnabled()
        }
        waitFor { it.profile?.level == ReadingLevel.TRAIL }
        click("reading-game-SEQUENCE")
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
        click("reading-resume-MISSING")
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

    @Test fun gameTapStartsTheSelectedBatchAndRetainsLegacyDraftHistory() {
        show(readingFixture().copy(page = ReadingPage.MENU))
        compose.onNodeWithTag("reading-game-MISSING").assertIsEnabled()
        click("reading-question-count-25")
        click("reading-game-MISSING")
        waitFor { it.activeSession?.questionCount == 25 }
        assertEquals(26, model.data?.attempts?.size)
        assertEquals("attempt-a", model.data?.attempts?.first()?.id)
        val session = requireNotNull(model.data?.activeSession)
        assertEquals(25, session.attemptIds.distinct().size)
        assertEquals(session.attemptIds.first(), model.data?.active?.id)
        compose.onNodeWithTag("reading-question-progress").assertTextContains("Question 1 of 25")
    }

    @Test fun everyGamePromptsForQuestionCountWithoutStartingOrChangingTheDraft() {
        val data = readingFixture().copy(page = ReadingPage.MENU)
        show(data)
        ReadingGame.entries.forEach { game ->
            ShadowToast.reset()
            click("reading-game-${game.name}")
            assertEquals("Please select the number of questions.", ShadowToast.getTextOfLatestToast())
            assertEquals(data, model.data)
            assertEquals(data, ReadingStore(file).load())
        }
        click("reading-question-count-15")
        ShadowToast.reset()
        click("reading-game-MISSING")
        waitFor { it.activeSession?.questionCount == 15 }
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    @Test fun submitAdvancesImmediatelyAndKeepsThePreviousCorrectionBelowTheNewProblem() {
        val data = readingSessionFixture()
        val first = requireNotNull(data.active)
        val question = requireNotNull(first.content.question)
        show(data)
        compose.onNodeWithTag("reading-submit-answer").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("reading-previous-answer").assertDoesNotExist()
        click("reading-answer-${readingWrong(data)}")
        waitFor { it.active?.fields?.get("answer") != null }
        click("reading-submit-answer")
        waitFor { it.active?.id != first.id }
        compose.onNodeWithTag("reading-question-progress").assertTextContains("Question 2 of 15")
        compose.onNodeWithTag("reading-question-result").assertDoesNotExist()
        compose.onNodeWithTag("reading-finish").assertDoesNotExist()
        compose.onNodeWithTag("reading-next").assertDoesNotExist()
        val correction = "Previous question · ${first.title}\nNot quite. Correct answer: ${question.choices[question.answer]}"
        val feedback = compose.onNodeWithTag("reading-previous-answer").performScrollTo()
            .assertIsDisplayed().assertTextContains(correction).fetchSemanticsNode().boundsInRoot
        val problem = compose.onNodeWithTag("reading-problem").fetchSemanticsNode().boundsInRoot
        assertTrue(feedback.top >= problem.bottom)
        chooseCorrectAnswer()
        compose.onNodeWithTag("reading-previous-answer").assertTextContains(correction)
        assertEquals(1, requireNotNull(model.data).sessionWrong(requireNotNull(data.activeSession)))
        assertEquals(model.data, ReadingStore(file).load())
        click("reading-submit-answer")
        waitFor { it.activeSession?.let { session -> it.sessionAnswered(session) == 2 } == true }
        compose.onNodeWithTag("reading-question-progress").assertTextContains("Question 3 of 15")
        compose.onNodeWithTag("reading-previous-answer").assertTextContains("Correct! Correct answer:", substring = true)
    }

    @Test fun restoredCompletedQuestionAutomaticallyContinuesAndReadyLegacyDraftCanStillSubmit() {
        val data = readingSolve(readingSessionFixture())
        show(data)
        waitFor { it.active?.id == it.activeSession?.attemptIds?.get(1) }
        compose.onNodeWithTag("reading-question-progress").assertTextContains("Question 2 of 15")
        val ready = readingAnswer(readingFixture(), readingCorrect(readingFixture()))
        compose.runOnIdle { model.act { ready } }
        waitFor { it.active?.id == "attempt-a" }
        click("reading-submit")
        waitFor { it.active?.finishedAt != null }
        compose.onNodeWithTag("reading-summary").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun mainReadingProblemHasAVisibleBorder() {
        show(readingFixture())
        compose.onNodeWithTag("reading-problem").performScrollTo()
        val bounds = compose.onNodeWithTag("reading-problem").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                val density = view.resources.displayMetrics.density
                val y = bounds.center.y.toInt()
                val edge = bitmap.getPixel((bounds.left + density).toInt(), y)
                val inside = bitmap.getPixel((bounds.left + 6 * density).toInt(), y)
                assertNotEquals(inside, edge)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun partnersBatchSupportsFirstStartResumeAndCompletionWithoutAnotherReader() {
        val data = ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a")
        show(data)
        click("reading-question-count-15")
        click("reading-game-PARTNERS")
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
        click("reading-resume-PARTNERS")
        waitFor { it.page == ReadingPage.ACTIVITY }
        assertEquals(draft, model.data?.active)
        repeat(4) { index ->
            click("reading-tile-$index")
            waitFor { it.active?.order?.size == index + 1 }
        }
        click("reading-reread")
        waitFor { it.active?.reread == true }
        click("reading-shared")
        waitFor { it.stories.size == 1 }
        assertEquals(1, model.data?.profiles?.size)
        assertTrue(requireNotNull(model.data).attempts.first().earned("participation:reader-a"))
        assertEquals(2, requireNotNull(model.data?.activeSession).attemptIds.indexOf(model.data?.active?.id) + 1)
        assertEquals(1, model.data?.sharedCompletions("reader-a"))
        assertEquals(model.data, ReadingStore(file).load())
    }

    @Test fun partnersNewBatchRetainsExistingParticipationHistory() {
        var data = ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a")
        data = ReadingEngine.start(data, ReadingGame.PARTNERS, ReadingLevel.SPROUT, id = "old-story")
        data = ReadingEngine.submit(ReadingEngine.field(data, "turn", "A fox set out.", expectedTurn = 0),
            "turn", expectedTurn = 0)
        show(data.copy(page = ReadingPage.MENU))
        click("reading-question-count-15")
        click("reading-game-PARTNERS")
        waitFor { it.active?.id != "old-story" }
        assertEquals(data.attempts.first().checkpoints, model.data?.attempts?.first()?.checkpoints)
        assertTrue(requireNotNull(model.data).attempts.first().earned("participation:reader-a"))
        assertEquals(1, model.data?.profiles?.size)
        assertEquals(model.data, ReadingStore(file).load())
    }

    @Test fun wrongAnswersEndTheSelectedBatchOnlyAfterMoreThanTenPercent() {
        show(ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a"))
        compose.onNodeWithText("How many questions would you like?").assertIsDisplayed()
        compose.onNodeWithText("reading points", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Read, imagine, and make stories.", substring = true).assertDoesNotExist()
        listOf(15, 25, 50).forEach { compose.onNodeWithTag("reading-question-count-$it").assertIsDisplayed() }
        click("reading-question-count-15")
        click("reading-game-MISSING")
        waitFor { it.activeSession != null }
        repeat(2) { index ->
            val wrong = (readingCorrect(requireNotNull(model.data)) + 1) % requireNotNull(model.data?.active?.content?.question).choices.size
            click("reading-answer-$wrong")
            waitFor { it.active?.fields?.get("answer") == wrong.toString() }
            click("reading-submit-answer")
            waitFor { it.activeSession?.let { session -> it.sessionWrong(session) == index + 1 } == true }
            if (index == 0) {
                assertNull(model.data?.activeSession?.finishedAt)
                compose.onNodeWithTag("reading-next").assertDoesNotExist()
                compose.onNodeWithTag("reading-question-progress").assertTextContains("Question 2 of 15")
                waitFor { it.active?.finishedAt == null }
            }
        }
        waitFor { it.activeSession?.finishedAt != null }
        compose.onNodeWithTag("reading-session-summary").assertIsDisplayed()
        compose.onNodeWithTag("reading-next").assertDoesNotExist()
        assertNull(model.data?.activeSession?.prizeType)
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

    @Test fun captionInstructionsDoNotAffectIndependentCreditAndUnavailableAudioCanBeRetried() {
        show(readingFixture(ReadingContent.forGame(ReadingGame.CAPTION, ReadingLevel.SPROUT).first()), speech = true)
        click("reading-instructions")
        assertEquals(ReadingGame.CAPTION.guidance, lastSpoken)
        assertEquals(0, model.data?.active?.narrations)
        chooseCorrectAnswer()
        click("reading-submit-answer")
        waitFor { it.active?.earned("answer") == true }
        assertEquals(ReadingAccuracy(1, 1, 0, 0), model.data?.accuracy("reader-a"))

        compose.runOnIdle {
            speechWorks = false
            model.act { ReadingEngine.start(it, ReadingGame.CAPTION, ReadingLevel.SPROUT, true, "fresh") }
        }
        waitFor { it.active?.id == "fresh" }
        click("reading-instructions")
        compose.onNodeWithText("Instruction audio is unavailable. You can still read the instructions above.")
            .performScrollTo().assertIsDisplayed()
        speechWorks = true
        click("reading-instructions")
        compose.onNodeWithText("Instruction audio is unavailable.", substring = true).assertDoesNotExist()
        assertEquals(0, model.data?.active?.narrations)
    }

    @Test fun previouslyNarratedDraftRetainsItsSupportHistoryWithoutNarrationControls() {
        val content = ReadingContent.forGame(ReadingGame.CAPTION, ReadingLevel.SPROUT).first()
        show(ReadingEngine.narration(readingFixture(content), queued = true), speech = true)
        compose.onNodeWithTag("reading-listen").assertDoesNotExist()
        compose.onNodeWithTag("reading-stop").assertDoesNotExist()
        click("reading-instructions")
        chooseCorrectAnswer()
        click("reading-submit-answer")
        waitFor { it.active?.earned("answer") == true }
        assertEquals(ReadingAccuracy(0, 1, 1, 0), model.data?.accuracy("reader-a"))
        assertEquals(1, ReadingStore(file).load().active?.narrations)
    }

    @Test fun partnerActivityHasOnlyInstructionsButSavedStoryStillReadsInArrangedOrder() {
        var data = readingFixture(ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first())
        data = ReadingEngine.partner(data, "reader-b")
        repeat(4) { index ->
            data = ReadingEngine.field(data, "turn", "Sentence $index.", expectedTurn = index)
            data = ReadingEngine.submit(data, "turn", expectedTurn = index)
        }
        data = readingOrder(data, listOf(3, 1, 0, 2))
        show(data, speech = true)
        compose.onNodeWithTag("reading-listen").assertDoesNotExist()
        compose.onNodeWithTag("reading-stop").assertDoesNotExist()
        click("reading-instructions")
        assertEquals(ReadingGame.PARTNERS.guidance, lastSpoken)
        assertEquals(0, model.data?.active?.narrations)
        click("reading-reread")
        waitFor { it.active?.reread == true }
        click("reading-shared")
        waitFor { it.active?.earned("shared") == true }
        waitFor { it.active?.finishedAt != null }
        val completed = model.data
        click("reading-listen")
        assertEquals("Sentence 3. Sentence 1. Sentence 0. Sentence 2.", lastSpoken)
        val previousStops = stopped
        click("reading-stop")
        assertTrue(stopped > previousStops)
        assertEquals(completed, model.data)
        assertEquals(completed, ReadingStore(file).load())
    }

    @Test fun twoHundredPercentTextHasReachableControlsAndLargeTouchTargets() {
        show(readingFixture(), percent = 200)
        val answer = readingCorrect(requireNotNull(model.data))
        val button = compose.onNodeWithTag("reading-answer-$answer").performScrollTo()
            .assertIsDisplayed().assertIsEnabled().fetchSemanticsNode().boundsInRoot
        assertTrue(button.height >= 48)
        assertTrue(button.width > 0 && button.width <= compose.activity.resources.displayMetrics.widthPixels)
        chooseCorrectAnswer()
        click("reading-submit-answer")
        waitFor { it.active?.earned("answer") == true }
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
