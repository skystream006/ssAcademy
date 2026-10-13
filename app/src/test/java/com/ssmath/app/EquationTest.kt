package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w320dp-h800dp")
class EquationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        File(application.filesDir, "practice_history.json").delete()
        DebugLog.initialize(application)
    }

    private fun model() = MathViewModel(application, ProblemGenerator(Random(7)))

    @Test fun verticalEquationsSettingAppearsBetweenShowCorrectAnswersAndAppearance() {
        val model = model()
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithText("Math Settings").performScrollTo().performClick()
        compose.onNodeWithText("Appearance").performScrollTo()

        val answers = compose.onNodeWithText("Show correct answers").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val vertical = compose.onNodeWithText("Vertical equations").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val appearance = compose.onNodeWithText("Appearance").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Vertical equations must follow Show correct answers", answers.bottom <= vertical.top)
        assertTrue("Vertical equations must precede Appearance", vertical.bottom <= appearance.top)
    }

    @Test fun settingDefaultsToVerticalAndSwitchesLiveWithoutChangingPractice() {
        val model = model()
        assertTrue(model.verticalEquations)
        model.selectOperation(Operation.ADDITION)
        model.submitSetup()
        model.start()
        val game = model.game!!
        val description = "${game.problem.text} = ?"
        model.updateAnswer("123")
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithTag("problem").assertContentDescriptionEquals(description)

        listOf(false, true).forEach { vertical ->
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Math Settings").performScrollTo().performClick()
            val toggle = compose.onNodeWithContentDescription("Vertical equations").performScrollTo()
            if (vertical) toggle.assertIsOff() else toggle.assertIsOn()
            toggle.performClick()
            assertEquals(vertical, model.verticalEquations)
            assertEquals(vertical, model().verticalEquations)
            compose.onNodeWithContentDescription("Back").performClick()
            if (vertical) compose.onNodeWithTag("problem").assertContentDescriptionEquals(description)
            else compose.onNodeWithTag("problem").assertTextEquals(description)
            compose.onNodeWithTag("answer-input").assertTextContains("123")
            assertEquals(game, model.game)
        }
    }

    @Test fun verticalPracticeSubmitsAnswersAndShowsResultsAndHistoryWithoutRevealingHiddenAnswers() {
        val model = model()
        model.chooseVerticalEquations(true)
        model.chooseShowCorrectAnswers(false)
        model.selectOperation(Operation.MULTIPLICATION)
        model.updateQuestionCount("2")
        model.submitSetup()
        model.start()
        compose.setContent { MathAppContent(model) }
        val first = model.game!!.problem
        compose.onNodeWithTag("answer-input").performScrollTo().performTextReplacement(first.answer.toString())
        compose.onNodeWithTag("submit-answer").performScrollTo().performClick()
        compose.onNodeWithTag("points").assertTextEquals("Points: 1")
        val second = model.game!!.problem
        val given = second.answer + 1
        compose.onNodeWithTag("answer-input").performScrollTo().performTextReplacement(given.toString())
        compose.onNodeWithTag("submit-answer").performScrollTo().performClick()
        assertEquals(Screen.RESULTS, model.screen)
        compose.waitForIdle()
        compose.waitUntil(5_000) { model.history.size == 1 && !model.selectingCelebration }
        compose.runOnIdle { model.dismissCelebration() }

        val description = "${second.text} = $given"
        compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Wrong").assertIsDisplayed()
        compose.onNodeWithText("Correct answer:", substring = true).assertDoesNotExist()
        val attempts = listOf(Attempt(first, first.answer), Attempt(second, given))
        assertEquals(attempts, model.lastResult!!.attempts)
        assertEquals(attempts, HistoryStore(File(application.filesDir, "practice_history.json")).load().single().attempts)

        compose.onNodeWithTag("done-button").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Math Settings").performScrollTo().performClick()
        compose.onNodeWithTag("practice-history").performClick()
        compose.onNodeWithText("Multiplication · 1 to 10").performClick()
        compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Correct answer:", substring = true).assertDoesNotExist()
        compose.runOnIdle { model.chooseShowCorrectAnswers(true) }
        compose.onNodeWithText("Correct answer: ${second.answer}").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { model.chooseVerticalEquations(false) }
        compose.onNodeWithText(description).performScrollTo().assertIsDisplayed()
        assertEquals(attempts, model.historyDetail!!.attempts)
    }

    @Test fun practiceCorrectionUsesVerticalLayoutByDefault() {
        val model = model()
        model.selectOperation(Operation.SUBTRACTION)
        model.submitSetup()
        model.start()
        compose.setContent { MathAppContent(model) }
        val problem = model.game!!.problem
        compose.onNodeWithTag("answer-input").performTextReplacement((problem.answer + 1).toString())
        compose.onNodeWithTag("submit-answer").performClick()
        compose.onNodeWithTag("feedback-equation").performScrollTo()
            .assertContentDescriptionEquals("${problem.text} = ${problem.answer}")
        compose.runOnIdle { model.chooseShowCorrectAnswers(false) }
        compose.onNodeWithTag("feedback-equation").assertDoesNotExist()
        compose.runOnIdle { model.chooseShowCorrectAnswers(true) }
        compose.onNodeWithTag("feedback-equation").performScrollTo()
            .assertContentDescriptionEquals("${problem.text} = ${problem.answer}")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun verticalDigitsAlignByPlaceForEveryOperationAndOperandLength() {
        val problem = mutableStateOf(Problem(1234, 56, Operation.ADDITION))
        val answer = mutableStateOf("?")
        compose.setContent {
            MathTheme {
                Equation(problem.value, answer.value, vertical = true,
                    style = MaterialTheme.typography.displayMedium, modifier = Modifier.testTag("equation"))
            }
        }
        val cases = listOf(1234 to 56, 7 to 890, 10000 to 1, 10000 to 10000, 1 to 1)
        Operation.entries.forEach { operation ->
            cases.forEach { (left, right) ->
                listOf("?", operation.apply(left, right).toString()).forEach { displayedAnswer ->
                    compose.runOnIdle {
                        problem.value = Problem(left, right, operation)
                        answer.value = displayedAnswer
                    }
                    assertAligned(problem.value, displayedAnswer)
                }
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeOperandsAndNineDigitAnswersFitNarrowLayoutsWithLargeTextAndRtl() {
        val percent = mutableIntStateOf(80)
        val problem = Problem(10000, 10000, Operation.MULTIPLICATION)
        val answer = mutableStateOf("?")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                MathTheme(textSizePercent = percent.intValue) {
                    Box(Modifier.width(180.dp).testTag("available-space")) {
                        Equation(problem, answer.value, vertical = true,
                            style = MaterialTheme.typography.displayMedium, modifier = Modifier.testTag("equation"))
                    }
                }
            }
        }
        for (size in 80..200 step 10) {
            listOf("?", "100000000", "999999999").forEach { displayedAnswer ->
                compose.runOnIdle {
                    percent.intValue = size
                    answer.value = displayedAnswer
                }
                assertAligned(problem, displayedAnswer)
                val available = compose.onNodeWithTag("available-space").fetchSemanticsNode().boundsInRoot
                val equation = compose.onNodeWithTag("equation").fetchSemanticsNode().boundsInRoot
                assertTrue(equation.left >= available.left && equation.right <= available.right)
            }
        }
    }

    private fun assertAligned(problem: Problem, answer: String) {
        val operands = layout("equation-operands")
        val result = layout("equation-answer")
        val left = problem.left.toString()
        val right = problem.right.toString()
        val places = maxOf(left.length, right.length, answer.length)
        val operandText = "  ${left.padStart(places)}\n${problem.operation.symbol} ${right.padStart(places)}"
        assertEquals(operandText, operands.layoutInput.text.text)
        assertEquals("  ${answer.padStart(places)}", result.layoutInput.text.text)
        assertEquals("Unexpected operand layout: ${operands.layoutInput}", 2, operands.lineCount)
        assertEquals(1, result.lineCount)
        listOf(operands, result).forEach {
            assertFalse("Equation must not clip horizontally", it.didOverflowWidth)
            assertFalse("Equation must not clip vertically", it.didOverflowHeight)
        }
        val newline = operandText.indexOf('\n')
        for (place in 0 until minOf(left.length, right.length)) {
            val top = operands.getBoundingBox(newline - 1 - place)
            val bottom = operands.getBoundingBox(operandText.lastIndex - place)
            assertEquals("Place $place must align", top.left, bottom.left, 0.5f)
            assertEquals(top.right, bottom.right, 0.5f)
        }
        for (place in 0 until minOf(left.length, answer.length)) {
            val top = operands.getBoundingBox(newline - 1 - place)
            val bottom = result.getBoundingBox(result.layoutInput.text.lastIndex - place)
            assertEquals("Answer place $place must align", top.left, bottom.left, 0.5f)
            assertEquals(top.right, bottom.right, 0.5f)
        }
        val operandBounds = compose.onNodeWithTag("equation-operands", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val line = compose.onNodeWithTag("equation-line", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val answerBounds = compose.onNodeWithTag("equation-answer", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(operandBounds.right, answerBounds.right, 0.5f)
        assertTrue(line.top >= operandBounds.bottom && line.bottom <= answerBounds.top)
        compose.onNodeWithTag("equation").assertContentDescriptionEquals("${problem.text} = $answer")
    }

    private fun layout(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(results)) }
        return results.single()
    }
}
