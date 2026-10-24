package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
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
class GrammarScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var model: ReadingViewModel
    private lateinit var saved: ReadingData
    private val disabled = mutableStateOf<Set<ReadingGame>>(emptySet())
    private var instructions = ""

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
    }

    private fun show(data: ReadingData) {
        saved = data
        model = ReadingViewModel(application, object : ReadingStorage {
            override fun load() = saved
            override fun save(data: ReadingData) { saved = data }
            override fun startSession(data: ReadingData, game: ReadingGame, level: ReadingLevel, count: Int,
                                      rewardsEnabledAtStart: Boolean, timeLimitMs: Long?): ReadingData {
                assertEquals(ReadingGame.GRAMMAR, game)
                return grammarSession(count, rewardsEnabledAtStart).also(::save)
            }
        })
        compose.setContent {
            MathTheme {
                ReadingModule(model, {}, 100, {}, narrationAvailable = true,
                    speak = { instructions = it; true }, disabledGames = disabled.value)
            }
        }
        waitFor { true }
    }

    private fun waitFor(condition: (ReadingData) -> Boolean) {
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.data?.let(condition) == true && !model.saving
        }
        compose.waitForIdle()
    }

    private fun click(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    @Test fun menuStartsGrammarAndShowsHintsInstructionsAndExplanationsAfterAdvancing() {
        show(ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a"))
        click("reading-game-GRAMMAR")
        click("reading-question-count-15")
        compose.onNodeWithTag("reading-start-game").performClick()
        waitFor { it.activeSession?.game == ReadingGame.GRAMMAR }
        compose.onNodeWithText(grammarExample().passage.single()).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reading-submit-answer").performScrollTo().assertIsNotEnabled()
        click("reading-instructions")
        assertEquals(ReadingGame.GRAMMAR.guidance, instructions)
        assertEquals(0, model.data?.active?.narrations)
        click("reading-hint")
        waitFor { it.active?.hints == 1 }
        compose.onNodeWithTag("reading-feedback").performScrollTo().assertTextContains(grammarExample().hint)
        click("reading-answer-1")
        waitFor { it.active?.fields?.get("answer") == "1" }
        click("reading-submit-answer")
        waitFor { it.active?.id == "grammar-attempt-1" }
        compose.onNodeWithTag("reading-question-progress").assertTextContains("Question 2 of 15")
        compose.onNodeWithTag("reading-previous-answer").performScrollTo()
            .assertTextContains(grammarExample().explanation, substring = true)
        assertEquals(1, saved.sessionCorrect(requireNotNull(saved.activeSession)))
        assertFalse(saved.attempts.first().checkpoints.getValue("answer").independent)
        click("reading-back")
        waitFor { it.page == ReadingPage.MENU }
        click("reading-resume-GRAMMAR")
        waitFor { it.page == ReadingPage.ACTIVITY }
        assertEquals(15, saved.attempts.size)
        assertEquals("grammar-attempt-1", saved.active?.id)
    }

    @Test fun wrongAnswersEndAtExistingLimitAndReviewExplainsSavedAnswer() {
        show(grammarSession())
        repeat(2) { index ->
            click("reading-answer-0")
            waitFor { it.active?.fields?.get("answer") == "0" }
            click("reading-submit-answer")
            waitFor { it.sessionAnswered(requireNotNull(it.activeSession)) == index + 1 }
        }
        compose.onNodeWithTag("reading-session-summary").assertIsDisplayed()
        compose.onNodeWithText("Answered: 2 · Wrong: 2").assertIsDisplayed()
        compose.onNodeWithTag("reading-previous-answer").performScrollTo()
            .assertTextContains(grammarExample().explanation, substring = true)
        val before = saved
        click("reading-review-session-grammar-session")
        compose.onNodeWithTag("reading-review-outcome").assertTextContains("Incorrect")
        compose.onNodeWithText("Saved answer: rabbit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Correct answer: hops").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(grammarExample().explanation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reading-review-back").performClick()
        assertEquals(before, saved)
    }

    @Test fun disabledGrammarRetainsDraftAndCannotStartOrResumeUntilEnabled() {
        val data = grammarSession()
        application.getSharedPreferences("settings", 0).edit()
            .putStringSet("disabled_reading_games", setOf(ReadingGame.GRAMMAR.name)).commit()
        disabled.value = setOf(ReadingGame.GRAMMAR)
        show(data)
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        compose.onNodeWithTag("reading-game-GRAMMAR").assertDoesNotExist()
        compose.onNodeWithTag("reading-resume-GRAMMAR").assertDoesNotExist()
        compose.runOnIdle { model.startSession(ReadingGame.GRAMMAR, 25) }
        waitFor { it == data }
        assertEquals(data, saved)
        application.getSharedPreferences("settings", 0).edit().remove("disabled_reading_games").commit()
        compose.runOnIdle { disabled.value = emptySet() }
        compose.onNodeWithTag("reading-activity").assertIsDisplayed()
        assertEquals(data.activeSession?.attemptIds, saved.activeSession?.attemptIds)
    }
}
