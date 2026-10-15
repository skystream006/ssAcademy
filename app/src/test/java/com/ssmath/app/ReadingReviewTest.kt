package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class ReadingReviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun visible(text: String) = compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()

    private fun show(data: ReadingData, ids: List<String> = data.attempts.map { it.id },
                     load: suspend (ReadingAttempt) -> ReadingAttempt = { it }) {
        compose.setContent {
            MathTheme { ReadingReviewDialog(data, ids, load) {} }
        }
    }

    @Test fun everyGameShowsSavedQuestionsAndResponsesAfterSerialization() {
        val current = mutableStateOf<ReadingData?>(null)
        compose.setContent {
            current.value?.let { data ->
                MathTheme { ReadingReviewDialog(data, data.attempts.map { it.id }, { it }) {} }
            }
        }
        for (game in ReadingGame.entries) {
            val completed = readingSolve(readingFixture(ReadingContent.forGame(game, ReadingLevel.SPROUT).first()))
            val data = Json.decodeFromString<ReadingData>(Json.encodeToString(ReadingData.serializer(), completed))
            compose.runOnIdle { current.value = data }
            val attempt = requireNotNull(data.active)
            val content = attempt.content
            visible(content.title)
            visible("Picture: ${content.picture.symbol} — ${content.picture.description}")
            content.passage.forEach(::visible)
            if (game !in listOf(ReadingGame.STUDIO, ReadingGame.PARTNERS)) {
                val question = requireNotNull(ReadingEngine.question(attempt))
                visible(question.prompt)
                question.choices.forEachIndexed { index, choice -> visible("${index + 1}. $choice") }
                visible("Saved answer: ${question.choices[question.answer]}")
                visible("Correct answer: ${question.choices[question.answer]}")
            }
            when (game) {
                ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
                    visible("Saved answer: ${attempt.order.joinToString(" → ") { content.tiles[it] }}")
                    content.validOrders.forEach { order -> visible(order.joinToString(" → ") { content.tiles[it] }) }
                }
                ReadingGame.DETECTIVE -> {
                    visible("Saved answer: ${content.passage[content.evidence]}")
                    visible("Correct evidence: ${content.passage[content.evidence]}")
                }
                ReadingGame.CHAPTER -> {
                    visible("Chosen chapter: ${content.branches[0].label}")
                    visible(content.branches[0].text)
                    compose.onNodeWithText(content.branches[1].text).assertDoesNotExist()
                }
                ReadingGame.STUDIO -> ReadingEngine.studioKeys.forEach {
                    visible("Your $it idea")
                    visible("Saved answer: ${attempt.fields[it]}")
                }
                ReadingGame.PARTNERS -> {
                    attempt.turns.forEach { visible("Saved answer: ${it.text}") }
                    visible("Turn 2 · Bo · Add a place.")
                    visible("Story reread")
                }
                else -> Unit
            }
            if (game in listOf(ReadingGame.STUDIO, ReadingGame.PARTNERS)) {
                visible("Creative writing · no single correct answer")
                compose.onNodeWithTag("reading-review-story").performScrollTo()
                    .assertTextEquals(data.stories.single().text)
                compose.onNodeWithTag("reading-review-outcome").assertTextEquals("Completed")
            }
            assertEquals(completed, data)
        }
    }

    @Test fun wrongAnswersAndUnplayedQuestionsKeepTheirSessionOrderAndSurviveRotation() {
        var data = readingSessionFixture()
        repeat(2) {
            data = readingAnswer(data, readingWrong(data))
            data = ReadingEngine.nextSessionActivity(data)
        }
        val session = requireNotNull(data.activeSession)
        val first = data.attempts.first()
        val question = requireNotNull(first.content.question)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MathTheme { ReadingReviewDialog(data, session.attemptIds, { it }) {} }
        }
        compose.onNodeWithTag("reading-review-previous").assertIsNotEnabled()
        visible("Saved answer: ${question.choices[first.fields.getValue("answer").toInt()]}")
        visible("Correct answer: ${question.choices[question.answer]}")
        compose.onNodeWithTag("reading-review-outcome").assertTextEquals("Incorrect")
        repeat(2) { compose.onNodeWithTag("reading-review-next").performClick() }
        visible("Saved answer: Not answered")
        visible("Not submitted")
        compose.onNodeWithTag("reading-review-position").assertTextEquals("Question 3 of 15")
        compose.onNodeWithTag("reading-review-outcome").assertTextEquals("Not completed · archived")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("reading-review-position").assertTextEquals("Question 3 of 15")
        repeat(12) { compose.onNodeWithTag("reading-review-next").performClick() }
        compose.onNodeWithTag("reading-review-next").assertIsNotEnabled()
        compose.onNodeWithTag("reading-review-previous").assertIsEnabled().performClick()
        compose.onNodeWithTag("reading-review-position").assertTextEquals("Question 14 of 15")
        assertEquals(2, data.sessionWrong(session))
    }

    @Test fun partialDetectiveRetainsAnswerAndDraftEvidenceWithoutCallingItCorrect() {
        var data = readingSessionFixture(ReadingGame.DETECTIVE)
        data = readingAnswer(data, readingCorrect(data))
        val evidence = requireNotNull(data.active).content.evidence
        data = ReadingEngine.field(data, "evidence", evidence.toString())
        data = ReadingEngine.endSession(data, requireNotNull(data.activeSession).id, abandoned = true)
        show(data, listOf(requireNotNull(data.active).id))
        visible("Saved answer: ${requireNotNull(data.active).content.passage[evidence]}")
        visible("Unsubmitted draft")
        compose.onNodeWithTag("reading-review-outcome").assertTextEquals("Not completed · archived")
    }

    @Test fun incompleteScrambleRetainsEmptySpacesAndAcceptedAlternativeOrders() {
        val data = ReadingEngine.placeTileInSpace(readingFixture(ReadingContent.find("scramble-cats")), 0, 2)
        show(data)
        visible("Saved answer: ___ → ___ → softly")
        visible("Unsubmitted draft")
        visible("Cats → walk → softly")
        visible("Cats → softly → walk")
        compose.onNodeWithText(requireNotNull(data.active?.content?.question).prompt).assertDoesNotExist()
    }

    @Test fun legacyRetriesAreLabeledWithoutInventingEarlierResponses() {
        var data = readingFixture()
        data = readingAnswer(data, readingWrong(data))
        data = readingSolve(data)
        show(data)
        visible("This older activity includes retries. Only the latest saved answer is available, not each earlier response.")
        visible("Correct after retry")
        compose.onNodeWithTag("reading-review-outcome").assertTextEquals("Completed after retry")
    }

    @Test fun loadingFailureCanBeRetriedWithoutSavingOrShowingAnotherQuestion() {
        val data = readingSessionFixture()
        var fail = true
        val requests = mutableListOf<String>()
        show(data, load = {
            requests += it.id
            if (fail) throw IOException("Unavailable")
            it
        })
        visible("Unable to open this saved question. Your history has not been changed.")
        compose.runOnIdle { fail = false }
        compose.onNodeWithTag("reading-review-retry").performClick()
        visible(data.attempts.first().title)
        assertEquals(listOf(data.attempts.first().id, data.attempts.first().id), requests)
    }

    @Test fun navigatingDuringLoadNeverShowsAStaleResponseAndBackDismissesReview() {
        val data = readingSessionFixture()
        val gate = CompletableDeferred<Unit>()
        val first = data.attempts.first()
        val requests = mutableListOf<String>()
        var dismissed = false
        compose.setContent {
            MathTheme {
                ReadingReviewDialog(data, data.attempts.map { it.id }, {
                    requests += it.id
                    if (it.id == first.id) gate.await()
                    it
                }) { dismissed = true }
            }
        }
        compose.onNodeWithTag("reading-review-next").performClick()
        visible(data.attempts[1].title)
        compose.runOnIdle { gate.complete(Unit) }
        compose.onNodeWithTag("reading-review-position").assertTextEquals("Question 2 of 15")
        assertEquals(data.attempts.take(2).map { it.id }, requests)
        compose.onNodeWithTag("reading-review-back").performClick()
        assertTrue(dismissed)
    }
}
