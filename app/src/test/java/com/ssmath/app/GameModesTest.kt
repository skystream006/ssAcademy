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
        model.openRewardsSetup()
        model.updateRewardsParameters(operation,
            model.rewardsDefaults.getValue(operation).copy(questionCountText = count.toString()))
        model.saveRewardsSetup()
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
                if (mode == GameMode.PRACTICE) readyPractice(model, 3) else readyRewards(model, 3)
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
                    answerAndCheckIcon()
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
        assertNull(model.setupDialog)
        model.selectRewardsGame(Operation.ADDITION)
        model.openRewardsSetup()
        model.start()
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.game)
        model.openSettings()
        model.openPracticeGame()
        model.openRewardsGame()
        assertNull(model.setupDialog)
        model.closeOverlay()

        model.openPracticeGame()
        assertEquals(SetupDialog.PRACTICE, model.setupDialog)
        model.dismissSetupDialog()
        assertNull(model.setupDialog)
        readyPractice(model)
        assertEquals(GameMode.PRACTICE, model.gameMode)
        assertEquals(Screen.READY, model.screen)
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
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
        model.openPracticeGame()
        model.openRewardsGame()
        model.openRewardsSetup()
        model.submitSetup()
        model.selectRewardsGame(Operation.DIVISION)
        assertEquals(Screen.PLAYING, model.screen)
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
    }

    @Test fun freshDefaultsMatchTheOldSetupAndCoverAllFourOperations() {
        val model = model()
        assertEquals(Operation.entries.toSet(), model.rewardsDefaults.keys)
        Operation.entries.forEach { operation ->
            assertEquals(GameParameters.defaults(operation), model.rewardsDefaults[operation])
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
            assertEquals(if (operation == Operation.DIVISION) GameParameters("81", "9", "50")
                else GameParameters("3", "18", "50"), original[operation])
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
            assertEquals(GameParameters.defaults(operation), model.rewardsDefaults[operation])
        }
        model.openRewardsGame()
        model.openRewardsSetup()
        assertTrue(model.canSaveRewardsSetup)
    }

    @Test fun divisionMigrationUsesLegacyMaximumWhenItsSeparateLimitsAreAbsent() {
        settings.edit().putInt("minimum", 4).putInt("maximum", 27).putInt("question_count", 30).commit()
        assertEquals(GameParameters("27", "27", "30"), model().rewardsDefaults[Operation.DIVISION])
    }

    @Test fun independentDefaultsForEveryOperationSurviveRestartAndDriveTheirGames() {
        val defaults = mapOf(
            Operation.ADDITION to GameParameters("3", "11", "25"),
            Operation.SUBTRACTION to GameParameters("4", "21", "26"),
            Operation.MULTIPLICATION to GameParameters("6", "13", "50"),
            Operation.DIVISION to GameParameters("70", "7", "51")
        )
        val first = model()
        first.openRewardsGame()
        first.openRewardsSetup()
        assertEquals(Screen.REWARDS_SETUP, first.screen)
        assertNull(first.setupDialog)
        defaults.forEach { (operation, parameters) -> first.updateRewardsParameters(operation, parameters) }
        first.saveRewardsSetup()
        assertEquals(Screen.SETUP, first.screen)
        assertEquals(SetupDialog.REWARDS, first.setupDialog)
        assertEquals(defaults, first.rewardsDefaults)
        val restored = model()
        assertEquals(defaults, restored.rewardsDefaults)
        defaults.forEach { (operation, parameters) ->
            restored.openRewardsGame()
            restored.selectRewardsGame(operation)
            assertEquals(Screen.READY, restored.screen)
            assertEquals(GameMode.REWARDS, restored.gameMode)
            assertEquals(operation, restored.readyOperation)
            assertEquals(parameters, restored.readyParameters)
            restored.start()
            val game = requireNotNull(restored.game)
            assertEquals(operation, game.operation)
            assertEquals(parameters.questionCount, game.questionCount)
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

    @Test fun savedDefaultsNormalizeWhitespaceAndLeadingZerosBeforeReload() {
        val model = model()
        model.openRewardsGame()
        model.openRewardsSetup()
        Operation.entries.forEach { operation ->
            model.updateRewardsParameters(operation, if (operation == Operation.DIVISION)
                GameParameters(" 0100 ", " 0007 ", "\t0050 ")
            else GameParameters(" 0002 ", " 0010 ", "\t0025 "))
        }
        assertTrue(model.canSaveRewardsSetup)
        model.saveRewardsSetup()
        val expected = Operation.entries.associateWith { operation ->
            if (operation == Operation.DIVISION) GameParameters("100", "7", "50")
            else GameParameters("2", "10", "25")
        }
        assertEquals(expected, model.rewardsDefaults)
        assertEquals(expected, model.rewardsSetupDraft)
        assertEquals(expected, model().rewardsDefaults)
        model.selectRewardsGame(Operation.DIVISION)
        assertEquals(expected[Operation.DIVISION], model.readyParameters)
    }

    @Test fun invalidDraftNeverPartiallySavesAndCancelOrBackRestoresAllDefaults() {
        val model = model()
        val saved = model.rewardsDefaults
        model.openRewardsGame()
        model.openRewardsSetup()
        model.updateRewardsParameters(Operation.ADDITION, GameParameters("2", "12", "25"))
        Operation.entries.forEach { operation ->
            val original = model.rewardsSetupDraft.getValue(operation)
            model.updateRewardsParameters(operation, original.copy(questionCountText = ""))
            assertFalse(model.canSaveRewardsSetup)
            model.saveRewardsSetup()
            assertEquals(Screen.REWARDS_SETUP, model.screen)
            assertEquals(saved, model.rewardsDefaults)
            assertEquals(saved, model().rewardsDefaults)
            model.updateRewardsParameters(operation, original)
        }
        model.cancelRewardsSetup()
        assertEquals(Screen.SETUP, model.screen)
        assertEquals(SetupDialog.REWARDS, model.setupDialog)
        assertEquals(saved, model.rewardsDefaults)
        assertEquals(saved, model.rewardsSetupDraft)
        model.openRewardsSetup()
        model.updateRewardsParameters(Operation.DIVISION, GameParameters("40", "3", "50"))
        model.backToSetup()
        assertEquals(SetupDialog.REWARDS, model.setupDialog)
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
        model.updateRewardsParameters(Operation.MULTIPLICATION, GameParameters("", "", ""))
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
            model.chooseTimeLimitMinutes(5)
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

    @Test fun earlyPracticeFinishesDoNotAwardScenesOrPrizes() {
        val model = model()
        readyPractice(model, count = 50)
        model.start()
        repeat(5) { answer(model, correct = false) }
        assertEquals(Screen.RESULTS, model.screen)
        assertNotNull(model.earlyFinishMessage)
        assertNull(model.celebration)
        assertNull(model.lastResult!!.prizeType)
        assertTrue(model.pokemons.isEmpty())
    }

    @Test fun practiceKeepsElapsedTimerWithoutApplyingTheRewardsTimeLimit() {
        val model = model()
        model.chooseRewardsEnabled(true)
        model.chooseShowTimer(true)
        model.chooseTimeLimitMinutes(5)
        readyPractice(model, count = 25)
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
