package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class GameModesTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val settings get() = application.getSharedPreferences("settings", 0)
    private val file get() = File(application.filesDir, "practice_history.json")
    private val models = mutableListOf<ViewModelStore>()
    private val otherScenes = Celebration.entries.filter { it.category == CelebrationCategory.OTHER }.toSet()
    private var now = 1_000L

    @Before fun setup() {
        settings.edit().clear().commit()
        file.delete()
        DebugLog.initialize(application)
    }

    @After fun cleanup() {
        models.forEach { it.clear() }
        shadowOf(Looper.getMainLooper()).idle()
        file.delete()
    }

    private fun model(progressIconRandom: Random = Random.Default): MathViewModel = MathViewModel(application,
        generator = ProblemGenerator(Random(7)), clock = { now }, ioDispatcher = Dispatchers.Unconfined,
        rewardRandom = Random(3), celebrationRandom = Random(5), progressIconRandom = progressIconRandom).also {
        models += ViewModelStore().apply { put("model", it) }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun readyPractice(model: MathViewModel, count: Int = 10, operation: Operation = Operation.ADDITION) {
        model.openPracticeGame()
        model.selectOperation(operation)
        model.updateQuestionCount(count.toString())
        model.submitSetup()
    }

    private fun readyRewards(model: MathViewModel, count: Int = 25, operation: Operation = Operation.ADDITION) {
        model.openRewardsGame()
        model.updateRewardsQuestionCount(count.toString())
        model.selectRewardsGame(operation)
    }

    private fun answer(model: MathViewModel, correct: Boolean = true) {
        model.updateAnswer((model.game!!.problem.answer + if (correct) 0 else 1).toString())
        model.submitAnswer()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun bothModesKeepTheSameIconForEveryJumpAndSelectOnlyWhenStartingANewGame() {
        var starts = 0
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("Expected a bounded icon selection")
            override fun nextInt(until: Int): Int {
                assertEquals(ProgressIcon.entries.size, until)
                return starts++ % until
            }
        }
        val model = model(random)
        model.start()
        model.submitAnswer()
        assertEquals(0, starts)

        fun answerAndCheckIcon(correct: Boolean = true) {
            val expected = model.progressIcon
            val before = starts
            answer(model, correct)
            assertEquals(before, starts)
            assertEquals(expected, model.progressIcon)
            model.submitAnswer()
            assertEquals(before, starts)
            assertEquals(expected, model.progressIcon)
        }

        GameMode.entries.forEach { mode ->
            ProgressIcon.entries.forEach { expected ->
                val beforeStart = starts
                val previous = model.progressIcon
                if (mode == GameMode.PRACTICE) readyPractice(model, 10) else readyRewards(model, 10)
                assertEquals(beforeStart, starts)
                assertEquals(previous, model.progressIcon)
                model.start()
                assertEquals(beforeStart + 1, starts)
                assertEquals(expected, model.progressIcon)
                model.start()
                answerAndCheckIcon()
                answerAndCheckIcon(correct = false)
                model.updateAnswer("invalid")
                model.submitAnswer()
                model.updateAnswer(model.game!!.problem.answer.toString())
                model.openSettings()
                model.start()
                model.submitAnswer()
                model.closeOverlay()
                model.setForeground(false)
                model.submitAnswer()
                model.setForeground(true)
                assertEquals(2, model.game!!.attempts.size)
                assertEquals(beforeStart + 1, starts)
                assertEquals(expected, model.progressIcon)
                if (expected.ordinal % 2 == 0) {
                    repeat(8) { answerAndCheckIcon() }
                    assertEquals(Screen.RESULTS, model.screen)
                    model.dismissCelebration()
                    model.done()
                } else model.backToSetup()
                assertEquals(Screen.SETUP, model.screen)
                model.submitAnswer()
                assertEquals(beforeStart + 1, starts)
                assertEquals(expected, model.progressIcon)
            }
        }
        assertEquals(20, starts)
    }

    @Test fun dialogsAndReadyBackNavigationAreModeSpecificAndOnlyOpenFromHome() {
        val model = model()
        assertEquals(Screen.SETUP, model.screen)
        assertFalse(model.mathGamesOpen)
        assertNull(model.setupDialog)
        model.selectRewardsGame(Operation.ADDITION)
        model.openRewardsSetup()
        model.start()
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.overlay)
        assertNull(model.game)
        model.openSettings()
        model.openMathGames()
        assertFalse(model.mathGamesOpen)
        model.openPracticeGame()
        model.openRewardsGame()
        assertNull(model.setupDialog)
        model.closeOverlay()
        model.openMathGames()
        assertTrue(model.mathGamesOpen)
        model.closeMathGames()
        assertFalse(model.mathGamesOpen)

        model.openPracticeGame()
        assertEquals(SetupDialog.PRACTICE, model.setupDialog)
        model.closeMathGames()
        assertTrue(model.mathGamesOpen)
        model.dismissSetupDialog()
        assertNull(model.setupDialog)
        readyPractice(model)
        assertEquals(GameMode.PRACTICE, model.gameMode)
        assertEquals(Screen.READY, model.screen)
        model.closeMathGames()
        assertTrue(model.mathGamesOpen)
        assertNull(model.setupDialog)
        model.openRewardsGame()
        assertNull(model.setupDialog)
        model.backToSetup()
        assertEquals(Screen.SETUP, model.screen)
        assertEquals(SetupDialog.PRACTICE, model.setupDialog)
        assertNull(model.readyOperation)
        assertNull(model.readyParameters)

        model.dismissSetupDialog()
        readyRewards(model)
        assertEquals(GameMode.REWARDS, model.gameMode)
        model.backToSetup()
        assertEquals(SetupDialog.REWARDS, model.setupDialog)
        assertEquals("25", model.rewardsQuestionCountText)
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
        model.openPracticeGame()
        model.openRewardsGame()
        model.openRewardsSetup()
        model.submitSetup()
        model.selectRewardsGame(Operation.DIVISION)
        assertEquals(Screen.PLAYING, model.screen)
        model.closeMathGames()
        assertTrue(model.mathGamesOpen)
        assertEquals(GameMode.REWARDS, model.gameMode)
        assertNull(model.setupDialog)
        model.backToSetup()
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.setupDialog)
        readyPractice(model, count = 1)
        model.start()
        answer(model)
        model.done()
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.setupDialog)
        assertTrue(model.mathGamesOpen)
        model.closeMathGames()
        assertFalse(model.mathGamesOpen)
    }

    @Test fun freshDefaultsMatchTheOldSetupAndCoverAllFourOperations() {
        val model = model()
        assertEquals(Operation.entries.toSet(), model.rewardsDefaults.keys)
        Operation.entries.forEach { operation ->
            assertEquals(RewardsGameDefaults.defaults(operation), model.rewardsDefaults[operation])
            assertTrue(model.rewardsDefaults.getValue(operation).isValid(operation))
        }
        assertEquals("1", model.minimumText)
        assertEquals("10", model.maximumText)
        assertEquals("10", model.questionCountText)
    }

    @Test fun validLegacyDefaultsMigrateOnceAndRemainSeparateFromPractice() {
        settings.edit().putString("operation", "DIVISION").putInt("minimum", 3).putInt("maximum", 18)
            .putInt("division_maximum_first", 81).putInt("division_maximum_second", 9)
            .putInt("question_count", 50).commit()
        val model = model()
        val original = model.rewardsDefaults
        Operation.entries.forEach { operation ->
            assertEquals(if (operation == Operation.DIVISION) RewardsGameDefaults("81", "9")
                else RewardsGameDefaults("3", "18"), original[operation])
        }
        model.updateMinimum("2")
        model.updateMaximum("7")
        readyPractice(model, count = 4)
        model.backToSetup()
        model.updateDivisionMaximumFirst("100")
        model.updateDivisionMaximumSecond("20")
        readyPractice(model, count = 5, operation = Operation.DIVISION)
        val restored = model()
        assertEquals(original, restored.rewardsDefaults)
        assertEquals("2", restored.minimumText)
        assertEquals("7", restored.maximumText)
        assertEquals("100", restored.divisionMaximumFirstText)
        assertEquals("20", restored.divisionMaximumSecondText)
        assertEquals("5", restored.questionCountText)
    }

    @Test fun invalidLegacyDefaultsFallBackWithoutBlockingRewardsSetup() {
        settings.edit().putInt("minimum", 20).putInt("maximum", 10)
            .putInt("division_maximum_first", 0).putInt("division_maximum_second", 10_001)
            .putInt("question_count", 1001).commit()
        val model = model()
        Operation.entries.forEach { operation ->
            assertEquals(RewardsGameDefaults.defaults(operation), model.rewardsDefaults[operation])
        }
        model.openSettings()
        model.openRewardsSetup()
        assertTrue(model.canSaveRewardsSetup)
    }

    @Test fun divisionMigrationUsesLegacyMaximumWhenItsSeparateLimitsAreAbsent() {
        settings.edit().putInt("minimum", 4).putInt("maximum", 27).putInt("question_count", 30).commit()
        assertEquals(RewardsGameDefaults("27", "27"), model().rewardsDefaults[Operation.DIVISION])
    }

    @Test fun invalidLegacyQuestionCountsDoNotDiscardValidNumberDefaults() {
        listOf(0, -1, 1001).forEach { count ->
            settings.edit().clear().putInt("minimum", 3).putInt("maximum", 18)
                .putInt("division_maximum_first", 81).putInt("division_maximum_second", 9)
                .putInt("question_count", count).commit()
            val model = model()
            Operation.entries.forEach { operation ->
                assertEquals(if (operation == Operation.DIVISION) RewardsGameDefaults("81", "9")
                    else RewardsGameDefaults("3", "18"), model.rewardsDefaults[operation])
            }
            assertEquals(count.toString(), model.questionCountText)
            assertEquals(count, settings.getInt("question_count", -2))
            assertEquals("", model.rewardsQuestionCountText)
            readyRewards(model, count = 25)
            assertEquals(Screen.READY, model.screen)
            assertEquals(GameParameters("3", "18", "25"), model.readyParameters)
        }
    }

    @Test fun obsoletePerOperationCountsAreIgnoredAndRemovedWithoutChangingPracticeCount() {
        settings.edit().putInt("question_count", 37).apply()
        val expected = Operation.entries.associateWith { operation ->
            val first = if (operation == Operation.DIVISION) 81 else operation.ordinal + 2
            val second = if (operation == Operation.DIVISION) 9 else operation.ordinal + 12
            settings.edit().putInt("rewards_${operation.name}_first", first)
                .putInt("rewards_${operation.name}_second", second)
                .putInt("rewards_${operation.name}_question_count", listOf(50, 0, -1, 1001)[operation.ordinal])
                .commit()
            RewardsGameDefaults(first.toString(), second.toString())
        }
        val model = model()
        assertEquals(expected, model.rewardsDefaults)
        assertEquals("37", model.questionCountText)
        Operation.entries.forEach { operation ->
            val key = "rewards_${operation.name}_question_count"
            assertFalse(settings.contains(key))
            settings.edit().putInt(key, 9999).commit()
        }
        model.openSettings()
        model.openRewardsSetup()
        model.saveRewardsSetup()
        assertEquals(Overlay.SETTINGS, model.overlay)
        Operation.entries.forEach { assertFalse(settings.contains("rewards_${it.name}_question_count")) }
        assertEquals(expected, model().rewardsDefaults)
        assertEquals(37, settings.getInt("question_count", -1))
        assertEquals("", model().rewardsQuestionCountText)
    }

    @Test fun independentDefaultsForEveryOperationSurviveRestartAndDriveTheirGames() {
        val defaults = mapOf(
            Operation.ADDITION to RewardsGameDefaults("3", "11"),
            Operation.SUBTRACTION to RewardsGameDefaults("4", "21"),
            Operation.MULTIPLICATION to RewardsGameDefaults("6", "13"),
            Operation.DIVISION to RewardsGameDefaults("70", "7")
        )
        val first = model()
        first.openSettings()
        first.openRewardsSetup()
        assertEquals(Screen.SETUP, first.screen)
        assertEquals(Overlay.REWARDS_SETUP, first.overlay)
        assertNull(first.setupDialog)
        defaults.forEach { (operation, parameters) -> first.updateRewardsParameters(operation, parameters) }
        first.saveRewardsSetup()
        assertEquals(Screen.SETUP, first.screen)
        assertEquals(Overlay.SETTINGS, first.overlay)
        assertNull(first.setupDialog)
        assertEquals(defaults, first.rewardsDefaults)
        val restored = model()
        assertEquals(defaults, restored.rewardsDefaults)
        defaults.entries.forEachIndexed { index, (operation, parameters) ->
            val count = 25 + index
            restored.openRewardsGame()
            assertEquals("", restored.rewardsQuestionCountText)
            restored.updateRewardsQuestionCount(count.toString())
            restored.selectRewardsGame(operation)
            assertEquals(Screen.READY, restored.screen)
            assertEquals(GameMode.REWARDS, restored.gameMode)
            assertEquals(operation, restored.readyOperation)
            assertEquals(GameParameters(parameters.firstText, parameters.secondText, count.toString()),
                restored.readyParameters)
            restored.start()
            val game = requireNotNull(restored.game)
            assertEquals(operation, game.operation)
            assertEquals(count, game.questionCount)
            assertEquals(if (operation == Operation.DIVISION) 1 else parameters.first, game.minimum)
            assertEquals(if (operation == Operation.DIVISION) parameters.first else parameters.second, game.maximum)
            assertEquals(parameters.second, game.maximumSecond)
            assertEquals("1", restored.minimumText)
            assertEquals("10", restored.maximumText)
            assertEquals("10", restored.questionCountText)
            restored.backToSetup()
        }
    }

    @Test fun parametersValidateBoundsCountsAndIndependentDivisionMaxima() {
        Operation.entries.forEach { operation ->
            val valid = GameParameters.defaults(operation)
            listOf("", "0", "-1", "10001", "1.5", "abc", "1234567").forEach { invalid ->
                assertFalse(valid.copy(firstText = invalid).isValid(operation))
                assertFalse(valid.copy(secondText = invalid).isValid(operation))
            }
            listOf("", "0", "-1", "1001", "1.5", "abc", "1234567").forEach { invalid ->
                assertFalse(valid.copy(questionCountText = invalid).isValid(operation))
            }
            assertTrue(GameParameters("1", "10000", "1").isValid(operation))
            assertTrue(GameParameters("10000", "10000", "1000").isValid(operation))
            assertEquals(operation == Operation.DIVISION, GameParameters("100", "3", "25").isValid(operation))
        }
    }

    @Test fun rewardsDefaultsValidateOnlyNumberBoundsAndIndependentDivisionMaxima() {
        Operation.entries.forEach { operation ->
            val valid = RewardsGameDefaults.defaults(operation)
            assertTrue(valid.isValid(operation))
            listOf("", "0", "-1", "10001", "1.5", "abc", "1234567").forEach { invalid ->
                assertFalse(valid.copy(firstText = invalid).isValid(operation))
                assertFalse(valid.copy(secondText = invalid).isValid(operation))
            }
            assertTrue(RewardsGameDefaults("1", "10000").isValid(operation))
            assertTrue(RewardsGameDefaults("10000", "10000").isValid(operation))
            assertEquals(operation == Operation.DIVISION, RewardsGameDefaults("100", "3").isValid(operation))
        }
    }

    @Test fun rewardsLaunchRequiresItsOwnValidCountAndSnapshotsBothBoundsWithoutSavingIt() {
        settings.edit().putInt("question_count", 37).commit()
        val model = model()
        model.openRewardsGame()
        val defaults = model.rewardsDefaults
        listOf("", " ", "0", "-1", "1001", "1.5", "abc", "999999").forEach { invalid ->
            model.updateRewardsQuestionCount(invalid)
            assertNull(model.rewardsQuestionCount)
            Operation.entries.forEach { model.selectRewardsGame(it) }
            assertEquals(Screen.SETUP, model.screen)
            assertEquals(SetupDialog.REWARDS, model.setupDialog)
            assertNull(model.readyParameters)
        }
        model.updateRewardsQuestionCount("1234567")
        assertEquals("123456", model.rewardsQuestionCountText)
        assertNull(model.rewardsQuestionCount)
        listOf(1, 1000).forEach { count ->
            Operation.entries.forEach { operation ->
                model.updateRewardsQuestionCount(count.toString())
                assertEquals(count, model.rewardsQuestionCount)
                model.selectRewardsGame(operation)
                val parameters = defaults.getValue(operation)
                assertEquals(GameParameters(parameters.firstText, parameters.secondText, count.toString()),
                    model.readyParameters)
                assertEquals(operation, model.readyOperation)
                assertEquals(Screen.READY, model.screen)
                model.backToSetup()
                assertEquals(count.toString(), model.rewardsQuestionCountText)
            }
        }
        assertEquals(defaults, model.rewardsDefaults)
        assertEquals("37", model.questionCountText)
        assertEquals(37, settings.getInt("question_count", -1))
        assertEquals("", model().rewardsQuestionCountText)
        Operation.entries.forEach { assertFalse(settings.contains("rewards_${it.name}_question_count")) }
    }

    @Test fun eachNewRewardsDialogRequiresAQuestionCountButReadyBackPreservesIt() {
        val model = model()
        readyRewards(model, count = 51)
        model.backToSetup()
        assertEquals(SetupDialog.REWARDS, model.setupDialog)
        assertEquals("51", model.rewardsQuestionCountText)
        model.selectRewardsGame(Operation.DIVISION)
        assertEquals(51, model.readyParameters!!.questionCount)
        model.backToSetup()
        model.dismissSetupDialog()
        model.openRewardsGame()
        assertEquals("", model.rewardsQuestionCountText)
        assertNull(model.rewardsQuestionCount)
        model.selectRewardsGame(Operation.ADDITION)
        assertEquals(Screen.SETUP, model.screen)
        model.updateRewardsQuestionCount("1")
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
        answer(model)
        model.done()
        model.openRewardsGame()
        assertEquals("", model.rewardsQuestionCountText)
        assertNull(model.rewardsQuestionCount)
    }

    @Test fun savedDefaultsNormalizeWhitespaceAndLeadingZerosBeforeReload() {
        val model = model()
        model.openSettings()
        model.openRewardsSetup()
        Operation.entries.forEach { operation ->
            model.updateRewardsParameters(operation, if (operation == Operation.DIVISION)
                RewardsGameDefaults(" 0100 ", " 0007 ")
            else RewardsGameDefaults(" 0002 ", " 0010 "))
        }
        assertTrue(model.canSaveRewardsSetup)
        model.saveRewardsSetup()
        val expected = Operation.entries.associateWith { operation ->
            if (operation == Operation.DIVISION) RewardsGameDefaults("100", "7")
            else RewardsGameDefaults("2", "10")
        }
        assertEquals(expected, model.rewardsDefaults)
        assertEquals(expected, model.rewardsSetupDraft)
        assertEquals(expected, model().rewardsDefaults)
        model.closeOverlay()
        model.openRewardsGame()
        model.updateRewardsQuestionCount("\t0050 ")
        model.selectRewardsGame(Operation.DIVISION)
        assertEquals(GameParameters("100", "7", "50"), model.readyParameters)
    }

    @Test fun invalidDraftNeverPartiallySavesAndCancelOrBackRestoresAllDefaults() {
        val model = model()
        val saved = model.rewardsDefaults
        model.openSettings()
        model.openRewardsSetup()
        model.updateRewardsParameters(Operation.ADDITION, RewardsGameDefaults("2", "12"))
        Operation.entries.forEach { operation ->
            val original = model.rewardsSetupDraft.getValue(operation)
            model.updateRewardsParameters(operation, original.copy(firstText = ""))
            assertFalse(model.canSaveRewardsSetup)
            model.saveRewardsSetup()
            assertEquals(Screen.SETUP, model.screen)
            assertEquals(Overlay.REWARDS_SETUP, model.overlay)
            assertEquals(saved, model.rewardsDefaults)
            assertEquals(saved, model().rewardsDefaults)
            model.updateRewardsParameters(operation, original)
        }
        model.cancelRewardsSetup()
        assertEquals(Screen.SETUP, model.screen)
        assertEquals(Overlay.SETTINGS, model.overlay)
        assertNull(model.setupDialog)
        assertEquals(saved, model.rewardsDefaults)
        assertEquals(saved, model.rewardsSetupDraft)
        model.openRewardsSetup()
        model.updateRewardsParameters(Operation.DIVISION, RewardsGameDefaults("40", "3"))
        model.closeOverlay()
        assertEquals(Overlay.SETTINGS, model.overlay)
        assertNull(model.setupDialog)
        model.openRewardsSetup()
        assertEquals(saved, model.rewardsSetupDraft)
        model.saveRewardsSetup()
        assertEquals(saved, model().rewardsDefaults)
    }

    @Test fun capturedReadyParametersCannotBeChangedByPracticeEditsOrInvalidDrafts() {
        val model = model()
        model.updateMinimum("4")
        model.updateMaximum("8")
        readyPractice(model, count = 3)
        model.updateMinimum("")
        model.updateMaximum("1")
        model.updateQuestionCount("")
        model.selectOperation(Operation.DIVISION)
        assertEquals(Operation.ADDITION, model.readyOperation)
        assertEquals(GameParameters("4", "8", "3"), model.readyParameters)
        model.start()
        assertEquals(GameMode.PRACTICE, model.gameMode)
        assertEquals(4, model.game!!.minimum)
        assertEquals(8, model.game!!.maximum)
        assertEquals(3, model.game!!.questionCount)
        model.backToSetup()

        readyRewards(model, count = 50, operation = Operation.MULTIPLICATION)
        val saved = model.rewardsDefaults
        model.updateMinimum("9999")
        model.updateMaximum("10000")
        model.updateDivisionMaximumFirst("")
        model.updateQuestionCount("1")
        model.selectOperation(Operation.SUBTRACTION)
        model.updateRewardsParameters(Operation.MULTIPLICATION, RewardsGameDefaults("", ""))
        model.updateRewardsQuestionCount("1")
        assertEquals(GameParameters("1", "10", "50"), model.readyParameters)
        model.start()
        assertEquals(GameMode.REWARDS, model.gameMode)
        assertEquals(Operation.MULTIPLICATION, model.game!!.operation)
        assertEquals(1, model.game!!.minimum)
        assertEquals(10, model.game!!.maximum)
        assertEquals(50, model.game!!.questionCount)
        assertEquals(saved, model.rewardsDefaults)
        assertEquals("4", model().minimumText)
        assertEquals("8", model().maximumText)
        assertEquals("3", model().questionCountText)
    }

    @Test fun rewardsDefaultsCanOnlyBeEditedFromSettingsNotFromTheLaunchDialogOrCollections() {
        val model = model()
        val saved = model.rewardsDefaults
        model.openRewardsGame()
        model.updateRewardsQuestionCount("25")
        model.openRewardsSetup()
        model.updateRewardsParameters(Operation.ADDITION, RewardsGameDefaults("3", "5"))
        model.saveRewardsSetup()
        model.cancelRewardsSetup()
        assertNull(model.overlay)
        assertEquals(SetupDialog.REWARDS, model.setupDialog)
        assertEquals("25", model.rewardsQuestionCountText)
        assertEquals(saved, model.rewardsDefaults)
        model.dismissSetupDialog()
        model.openRewards()
        model.openRewardsSetup()
        assertEquals(Overlay.REWARDS, model.overlay)
        model.closeOverlay()
        model.openPokemons()
        model.openRewardsSetup()
        assertEquals(Overlay.POKEMONS, model.overlay)
        model.closeOverlay()
        model.openSettings()
        model.openRewardsSetup()
        assertEquals(Overlay.REWARDS_SETUP, model.overlay)
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.setupDialog)
    }

    @Test fun savingRewardsDefaultsFromReadyDoesNotAlterTheCapturedGameInEitherMode() {
        GameMode.entries.forEach { mode ->
            val model = model()
            if (mode == GameMode.PRACTICE) readyPractice(model, 3) else readyRewards(model, 50)
            val captured = requireNotNull(model.readyParameters)
            val operation = model.readyOperation
            model.openSettings()
            model.openRewardsSetup()
            model.updateRewardsParameters(Operation.ADDITION,
                RewardsGameDefaults((captured.first!! + 1).toString(), (captured.second!! + 2).toString()))
            model.saveRewardsSetup()
            assertEquals(Overlay.SETTINGS, model.overlay)
            assertEquals(Screen.READY, model.screen)
            assertEquals(mode, model.gameMode)
            assertEquals(operation, model.readyOperation)
            assertEquals(captured, model.readyParameters)
            model.start()
            assertNull(model.game)
            model.closeOverlay()
            model.start()
            assertEquals(captured.questionCount, model.game!!.questionCount)
            assertEquals(captured.first, model.game!!.minimum)
            assertEquals(captured.second, model.game!!.maximum)
            model.backToSetup()
        }
    }

    @Test fun savingCancellingAndBackingOutOfRewardsSetupKeepEitherGamePausedUntilSettingsCloses() {
        GameMode.entries.forEach { mode ->
            listOf("save", "cancel", "back", "close").forEach { exit ->
                val model = model()
                model.chooseShowTimer(true)
                model.chooseTimeLimitSeconds(300)
                if (mode == GameMode.PRACTICE) readyPractice(model, 4) else readyRewards(model, 4)
                model.start()
                answer(model)
                model.updateAnswer("42")
                now += 5_000
                val game = model.game
                val ready = model.readyParameters
                val timeLimit = model.activeTimeLimitMs
                val original = model.rewardsDefaults
                model.openSettings()
                model.openRewardsSetup()
                assertEquals(Overlay.REWARDS_SETUP, model.overlay)
                val previous = original.getValue(Operation.ADDITION)
                val changed = RewardsGameDefaults((previous.first!! + 1).toString(), (previous.second!! + 2).toString())
                model.updateRewardsParameters(Operation.ADDITION, changed)
                now += 600_000
                assertEquals(5_000L, model.elapsedMs())
                assertFalse(model.checkTimeLimit())
                model.submitAnswer()
                assertSame(game, model.game)
                when (exit) {
                    "save" -> model.saveRewardsSetup()
                    "cancel" -> model.cancelRewardsSetup()
                    "back" -> model.backToSetup()
                    else -> model.closeOverlay()
                }
                assertEquals(Overlay.SETTINGS, model.overlay)
                assertEquals(Screen.PLAYING, model.screen)
                assertEquals(mode, model.gameMode)
                assertNull(model.setupDialog)
                assertSame(game, model.game)
                assertEquals(ready, model.readyParameters)
                assertEquals("42", model.answerText)
                assertEquals(timeLimit, model.activeTimeLimitMs)
                assertEquals(if (exit == "save") original + (Operation.ADDITION to changed) else original,
                    model.rewardsDefaults)
                assertEquals(model.rewardsDefaults, model.rewardsSetupDraft)
                now += 600_000
                assertEquals(5_000L, model.elapsedMs())
                assertFalse(model.checkTimeLimit())
                model.closeOverlay()
                assertNull(model.overlay)
                now += 1_000
                assertEquals(6_000L, model.elapsedMs())
                assertFalse(model.checkTimeLimit())
                answer(model)
                assertEquals(2, model.game!!.attempts.size)
                model.backToSetup()
            }
        }
    }

    @Test fun practiceNeverAwardsPrizesOrPokemonAtAnyLengthEvenWhenSettingsChange() {
        val model = model()
        listOf(1, 14, 15, 25, 50, 100).forEach { count ->
            readyPractice(model, count)
            model.chooseRewardsEnabled(count % 2 == 0)
            model.start()
            model.openSettings()
            model.chooseRewardsEnabled(false)
            RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
            model.chooseRewardsEnabled(true)
            RewardType.entries.forEach { model.chooseRewardEnabled(it, true) }
            model.chooseShowTimer(true)
            model.chooseTimeLimitSeconds(300)
            model.closeOverlay()
            repeat(count) { answer(model) }
            val scene = requireNotNull(model.celebration)
            assertEquals(CelebrationCategory.OTHER, scene.category)
            assertEquals(GameMode.PRACTICE, model.lastResult!!.gameMode)
            assertNull(model.lastResult!!.prizeType)
            assertNull(model.rewardResult)
            model.collectPresentedCelebration(Celebration.PIKACHU)
            assertNull(model.lastResult!!.pokemonReward)
            model.collectPresentedCelebration(scene)
            assertEquals(scene, model.lastResult!!.pokemonReward)
            model.dismissCelebration()
            model.claimReward()
            assertFalse(model.rewardDialogVisible)
            assertTrue(model.rewardBalances.isEmpty())
            assertTrue(model.pokemons.all { it.category == CelebrationCategory.OTHER })
            model.done()
        }
        val restored = model()
        assertEquals(6, restored.history.size)
        assertTrue(restored.history.all { it.gameMode == GameMode.PRACTICE && it.prizeType == null && it.prize == null })
        assertEquals(otherScenes, restored.pokemons)
    }

    @Test fun practiceRepeatsItsOtherScenesButRewardsKeepsTheFullCollectionRule() {
        val model = model()
        repeat(6) { index ->
            readyPractice(model, count = 1)
            model.start()
            answer(model)
            val scene = requireNotNull(model.celebration)
            assertEquals(CelebrationCategory.OTHER, scene.category)
            assertEquals(index == 5, scene in model.pokemons)
            model.collectPresentedCelebration(scene)
            model.done()
        }
        assertEquals(otherScenes, model.pokemons)
        readyRewards(model, count = 14)
        model.start()
        repeat(14) { answer(model) }
        assertNull(model.celebration)
        model.done()
        readyRewards(model, count = 15)
        model.start()
        repeat(15) { answer(model) }
        assertEquals(CelebrationCategory.POKEMONS, model.celebration!!.category)
        assertNull(model.lastResult!!.prizeType)
    }

    @Test fun completeCollectionStillCannotGivePracticeAPokemon() {
        val model = model()
        model.collectAllCelebrations()
        readyPractice(model, count = 50)
        model.start()
        repeat(50) { answer(model) }
        assertEquals(CelebrationCategory.OTHER, model.celebration!!.category)
        assertNull(model.lastResult!!.prizeType)
        assertEquals(Celebration.entries.toSet(), model.pokemons)
    }

    @Test fun rewardsRetainPokemonAnimationsPrizeTiersAndExistingInventory() {
        HistoryStore(file).collectPokemons(otherScenes)
        val model = model()
        listOf(25 to RewardTier.TIER_1, 50 to RewardTier.TIER_2).forEach { (count, tier) ->
            readyRewards(model, count)
            model.start()
            repeat(count) { answer(model) }
            assertEquals(GameMode.REWARDS, model.lastResult!!.gameMode)
            val scene = requireNotNull(model.celebration)
            assertEquals(CelebrationCategory.POKEMONS, scene.category)
            assertFalse(scene in model.pokemons)
            assertEquals(tier, model.lastResult!!.prizeType!!.tier)
            assertFalse(model.rewardDialogVisible)
            model.collectPresentedCelebration(scene)
            model.dismissCelebration()
            assertTrue(model.rewardDialogVisible)
            model.claimReward()
            assertNotNull(model.lastResult!!.prize)
            model.done()
        }
        val restored = model()
        assertEquals(2, restored.rewardBalances.values.sumOf { it.totalFragments })
        assertEquals(7, restored.pokemons.size)
        assertTrue(restored.history.all { it.gameMode == GameMode.REWARDS && it.prize != null })
        readyPractice(restored, count = 1)
        restored.start()
        answer(restored)
        assertEquals(model.rewardBalances, restored.rewardBalances)
    }

    @Test fun bothModesFinishOnlyAboveTenPercentWrongWithoutScenesOrPrizes() {
        GameMode.entries.forEach { mode ->
            mapOf(10 to 2, 15 to 2, 25 to 3, 50 to 6, 100 to 11).forEach { (count, limit) ->
                val model = model()
                if (mode == GameMode.PRACTICE) readyPractice(model, count) else readyRewards(model, count)
                model.start()
                repeat(limit - 1) { answer(model, correct = false) }
                assertEquals(Screen.PLAYING, model.screen)
                assertNull(model.lastResult)
                answer(model, correct = false)
                assertEquals(Screen.RESULTS, model.screen)
                assertEquals(limit, model.lastResult!!.wrong)
                assertNotNull(model.earlyFinishMessage)
                assertNull(model.celebration)
                assertNull(model.lastResult!!.prizeType)
                assertTrue(model.pokemons.isEmpty())
                model.done()
            }
        }
    }

    @Test fun practiceKeepsElapsedTimerWithoutApplyingTheRewardsTimeLimit() {
        val model = model()
        model.chooseRewardsEnabled(true)
        model.chooseShowTimer(true)
        model.chooseTimeLimitSeconds(60)
        readyPractice(model, count = 25)
        assertNull(model.readyTimeLimitMs)
        model.start()
        assertTrue(model.showTimer)
        assertNull(model.activeTimeLimitMs)
        now += 300_001L
        assertEquals(300_001L, model.elapsedMs())
        assertFalse(model.checkTimeLimit())
        repeat(25) { answer(model) }
        assertFalse(model.lastResult!!.timedOut)
        assertEquals(300_001L, model.lastResult!!.durationMs)
        assertEquals(CelebrationCategory.OTHER, model.celebration!!.category)
        assertNull(model.lastResult!!.prizeType)
        model.done()

        readyRewards(model)
        model.start()
        assertEquals(300_000L, model.activeTimeLimitMs)
        now += 300_000L
        assertTrue(model.checkTimeLimit())
        assertTrue(model.lastResult!!.timedOut)
        assertNull(model.celebration)
        assertNull(model.lastResult!!.prizeType)
    }

    @Test fun storedPracticeResultsCannotExposeOrClaimAnInjectedPrize() {
        val result = PracticeResult(1, 1, Operation.ADDITION, 10, 1000,
            List(25) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
            prizeType = RewardType.LOLLIPOP, gameMode = GameMode.PRACTICE)
        HistoryStore(file).add(result)
        val model = model()
        model.openHistory()
        model.showRewardForResult(result)
        model.claimReward()
        assertNull(model.rewardResult)
        assertTrue(model.rewardBalances.isEmpty())
    }
}
