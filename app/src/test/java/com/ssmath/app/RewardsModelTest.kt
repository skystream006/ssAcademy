package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
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
class RewardsModelTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "practice_history.json")
    private val blockedWrite get() = File(application.filesDir, "practice_history.json.tmp")
    private val models = mutableListOf<ViewModelStore>()
    private var now = 1_000L
    private var wallTime = 1_700_000_000_000L

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        file.delete()
        blockedWrite.deleteRecursively()
        DebugLog.initialize(application)
    }

    @After fun cleanup() {
        models.forEach { it.clear() }
        shadowOf(Looper.getMainLooper()).idle()
        blockedWrite.deleteRecursively()
        file.delete()
    }

    private fun model(
        dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        rewardRandom: Random = Random(3)
    ): MathViewModel {
        val model = MathViewModel(application, ProblemGenerator(Random(7)), clock = { now },
            wallClock = { wallTime }, ioDispatcher = dispatcher, rewardRandom = rewardRandom)
        models += ViewModelStore().apply { put("model", model) }
        shadowOf(Looper.getMainLooper()).idle()
        return model
    }

    private fun start(model: MathViewModel, count: Int = 26, rewards: Boolean = true, minutes: Int = 0) {
        if (model.screen != Screen.SETUP) model.done()
        model.openRewardsGame()
        model.updateRewardsQuestionCount(count.toString())
        model.chooseRewardsEnabled(rewards)
        model.chooseTimeLimitSeconds(minutes * 60)
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
    }

    private fun answer(model: MathViewModel, correct: Boolean = true) {
        model.updateAnswer((model.game!!.problem.answer + if (correct) 0 else 1).toString())
        model.submitAnswer()
    }

    private fun finish(model: MathViewModel, count: Int = 26, wrong: Int = 0) {
        repeat(count) { answer(model, it < count - wrong) }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun perGameRewardsDefaultOnRespectLegacySubjectSwitchesAndPersistIndependently() {
        val settings = application.getSharedPreferences("settings", 0)
        settings.edit().putBoolean("rewards_enabled", false).putBoolean("reading_rewards_enabled", false).commit()
        val model = model()
        MathRewardGame.entries.forEach { assertFalse(model.rewardsEnabled(it)) }
        ReadingGame.entries.forEach { assertFalse(model.rewardsEnabled(it)) }
        assertTrue(model.disabledMathGameRewards.isEmpty())
        assertTrue(model.disabledReadingGameRewards.isEmpty())
        model.chooseRewardsEnabled(true)
        model.chooseSubjectRewardsEnabled(Subject.READING, true)
        MathRewardGame.entries.forEach { assertTrue(model.rewardsEnabled(it)) }
        ReadingGame.entries.forEach { assertTrue(model.rewardsEnabled(it)) }
        model.chooseMathGameRewardsEnabled(MathRewardGame.LOGIC, false)
        model.chooseReadingGameRewardsEnabled(ReadingGame.STUDIO, false)
        val restored = model()
        MathRewardGame.entries.forEach { assertEquals(it != MathRewardGame.LOGIC, restored.rewardsEnabled(it)) }
        ReadingGame.entries.forEach { assertEquals(it != ReadingGame.STUDIO, restored.rewardsEnabled(it)) }
        assertTrue(restored.disabledReadingGames.isEmpty())
        assertTrue(restored.disabledRewards.isEmpty())
        restored.chooseRewardsEnabled(false)
        restored.chooseRewardsEnabled(true)
        assertFalse(restored.rewardsEnabled(MathRewardGame.LOGIC))
    }

    private fun readyRewardGame(model: MathViewModel, game: MathRewardGame) {
        model.done()
        when (game) {
            MathRewardGame.BASIC -> model.openRewardsGame()
            MathRewardGame.LOGIC -> model.openLogicMathGame()
            MathRewardGame.MATRIX -> model.openMultiplicationMatrixGame()
        }
        if (game != MathRewardGame.MATRIX) {
            model.updateRewardsQuestionCount("25")
            model.selectRewardsGame(Operation.ADDITION)
        }
        assertEquals(game, model.currentMathRewardGame)
    }

    @Test fun eachMathGameRequiresItsOwnRewardsAtStartAndFinishAndDisablingRemovesItsLimit() {
        val model = model()
        model.chooseShowTimer(true)
        MathRewardGame.entries.forEach { game ->
            model.chooseMathTimeLimitSeconds(game, 30)
            for (atStart in listOf(false, true)) for (atFinish in listOf(false, true)) {
                MathRewardGame.entries.forEach { model.chooseMathGameRewardsEnabled(it, it != game) }
                model.chooseMathGameRewardsEnabled(game, atStart)
                readyRewardGame(model, game)
                val count = if (game == MathRewardGame.MATRIX) 121 else 25
                val limit = (30_000L * count / 5).takeIf { atStart }
                assertEquals(limit, model.readyTimeLimitMs)
                model.start()
                assertEquals(Screen.PLAYING, model.screen)
                assertEquals(game, model.currentMathRewardGame)
                model.chooseMathGameRewardsEnabled(game, atFinish)
                assertEquals(limit.takeIf { atFinish }, model.activeTimeLimitMs)
                finish(model, count)
                val result = requireNotNull(model.lastResult)
                assertEquals(atStart && atFinish, result.prizeType != null)
                assertNotNull(model.celebration)
                assertEquals(result, model().history.first { it.id == result.id })
                model.chooseMathGameRewardsEnabled(game, false)
                while (model.celebration != null) model.dismissCelebration()
                if (result.prizeType != null) {
                    model.showRewardForResult(result)
                    model.claimReward()
                    shadowOf(Looper.getMainLooper()).idle()
                    assertNotNull(model.history.first { it.id == result.id }.prize)
                }
            }
        }
    }

    @Test fun disablingMathRewardsDuringPlayIgnoresTheDeadlineAndKeepsElapsedTime() {
        val model = model()
        model.chooseShowTimer(true)
        MathRewardGame.entries.forEach { model.chooseMathTimeLimitSeconds(it, 5) }
        for (game in MathRewardGame.entries) for (disableSubject in listOf(false, true)) {
            model.chooseRewardsEnabled(true)
            MathRewardGame.entries.forEach { model.chooseMathGameRewardsEnabled(it, true) }
            readyRewardGame(model, game)
            model.start()
            val limit = requireNotNull(model.activeTimeLimitMs)
            MathRewardGame.entries.filter { it != game }.forEach {
                model.chooseMathGameRewardsEnabled(it, false)
            }
            model.chooseSubjectRewardsEnabled(Subject.READING, false)
            assertEquals(limit, model.activeTimeLimitMs)
            model.openSettings()
            if (disableSubject) model.chooseRewardsEnabled(false)
            else model.chooseMathGameRewardsEnabled(game, false)
            model.closeOverlay()
            assertNull(model.activeTimeLimitMs)
            now += limit + 1_000
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            assertEquals(Screen.PLAYING, model.screen)
            assertFalse(model.checkTimeLimit())
            answer(model)
            assertEquals(1, model.game?.attempts?.size)
            finish(model, requireNotNull(model.game?.questionCount) - 1)
            val result = requireNotNull(model.lastResult)
            assertFalse(result.timedOut)
            assertEquals(limit + 1_000, result.durationMs)
            assertNull(result.prizeType)
            assertNotNull(model.celebration)
            assertEquals(5, model.timeLimitSeconds)
        }
    }

    @Test fun reenablingMathRewardsAppliesTimeLimitsOnlyToFutureGames() {
        val model = model()
        model.chooseShowTimer(true)
        start(model, count = 25, minutes = 1)
        model.chooseMathGameRewardsEnabled(MathRewardGame.BASIC, false)
        model.chooseMathGameRewardsEnabled(MathRewardGame.BASIC, true)
        assertNull(model.activeTimeLimitMs)
        now += 400_000
        assertFalse(model.checkTimeLimit())
        start(model, count = 25, minutes = 1)
        assertEquals(300_000L, model.activeTimeLimitMs)
        now += 300_000
        assertTrue(model.checkTimeLimit())
    }

    @Test fun rewardRuleSettingsPersistIndependentlyAndInvalidEditsAreIgnored() {
        val model = model()
        assertEquals(RewardThresholds(), model.rewardThresholds)
        assertEquals(RewardTier.TIER_2, model.matrixRewardTier)
        model.chooseSubjectRewardsEnabled(Subject.READING, false)
        model.updateQuestionCount("7")
        model.selectOperation(Operation.ADDITION)
        model.submitSetup()
        val defaults = model.rewardsDefaults
        model.chooseRewardEnabled(RewardType.RAMEN, false)
        model.chooseRewardThresholds(15, 25)
        model.chooseMatrixRewardTier(RewardTier.TIER_1)
        listOf(0 to 25, 15 to 15, 25 to 15, -1 to 25, 15 to 1001, 1000 to 1001).forEach { (first, second) ->
            model.chooseRewardThresholds(first, second)
            assertEquals(RewardThresholds(15, 25), model.rewardThresholds)
        }
        val restored = model()
        assertEquals(RewardThresholds(15, 25), restored.rewardThresholds)
        assertEquals(RewardTier.TIER_1, restored.matrixRewardTier)
        assertTrue(restored.rewardsEnabled)
        assertFalse(restored.rewardsEnabled(Subject.READING))
        assertEquals("7", restored.questionCountText)
        assertEquals(defaults, restored.rewardsDefaults)
        assertEquals(setOf(RewardType.RAMEN), restored.disabledRewards)
        restored.chooseRewardThresholds(1, 1000)
        restored.chooseMatrixRewardTier(RewardTier.TIER_2)
        assertEquals(RewardThresholds(1, 1000), model().rewardThresholds)
        assertEquals(RewardTier.TIER_2, model().matrixRewardTier)
        val settings = application.getSharedPreferences("settings", 0)
        assertEquals(1, settings.getInt("reward_tier_1_minimum", -1))
        assertEquals(1000, settings.getInt("reward_tier_2_minimum", -1))
        assertEquals("TIER_2", settings.getString("matrix_reward_tier", null))
    }

    @Test fun invalidStoredThresholdPairsAndUnknownMatrixTiersUseSafeDefaults() {
        val settings = application.getSharedPreferences("settings", 0)
        listOf(0 to 25, 15 to 15, 50 to 25, 25 to 1001, -1 to 25).forEach { (first, second) ->
            settings.edit().putInt("reward_tier_1_minimum", first)
                .putInt("reward_tier_2_minimum", second).putString("matrix_reward_tier", "unknown").commit()
            assertEquals(RewardThresholds(), model().rewardThresholds)
            assertEquals(RewardThresholds(), settings.readRewardThresholds())
            assertEquals(RewardTier.TIER_2, model().matrixRewardTier)
        }
        settings.edit().putString("reward_tier_1_minimum", "15").putInt("reward_tier_2_minimum", 25)
            .putInt("matrix_reward_tier", 1).commit()
        assertEquals(RewardThresholds(), model().rewardThresholds)
        assertEquals(RewardTier.TIER_2, model().matrixRewardTier)
        settings.edit().putInt("reward_tier_1_minimum", 15).putString("reward_tier_2_minimum", "25").commit()
        assertEquals(RewardThresholds(), settings.readRewardThresholds())
    }

    @Test fun ordinaryRewardsUseFinishThresholdsAndKeepTheirRulesThroughReloadAndClaim() {
        val model = model()
        listOf(15 to RewardTier.TIER_1, 25 to RewardTier.TIER_2).forEach { (count, tier) ->
            model.chooseRewardThresholds(25, 50)
            RewardType.entries.forEach { model.chooseRewardEnabled(it, true) }
            start(model, count)
            repeat(count - 1) { answer(model) }
            model.chooseRewardThresholds(15, 25)
            answer(model)
            shadowOf(Looper.getMainLooper()).idle()
            val result = requireNotNull(model.lastResult)
            val type = requireNotNull(result.prizeType)
            assertEquals(tier, type.tier)
            assertEquals(RewardThresholds(15, 25), result.rewardThresholds)
            assertTrue(requireNotNull(model.celebration).isEligibleFor(count, GameMode.REWARDS))
            model.chooseRewardThresholds(200, 300)
            model.chooseRewardsEnabled(false)
            RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
            val restored = model()
            assertEquals(result, restored.history.first { it.id == result.id })
            restored.showRewardForResult(result)
            repeat(3) { restored.claimReward() }
            shadowOf(Looper.getMainLooper()).idle()
            val claimed = restored.history.first { it.id == result.id }
            assertEquals(PrizeAward(type, RewardBalance(0, 1)), claimed.prize)
            assertEquals(result.rewardThresholds, claimed.rewardThresholds)
            assertEquals(claimed, model().history.first { it.id == result.id })
        }
    }

    @Test fun raisingThresholdsAtFinishSuppressesPrizeAndLaterLoweringCannotGrantItRetroactively() {
        val model = model()
        model.chooseRewardThresholds(15, 25)
        start(model, 25)
        repeat(24) { answer(model) }
        model.chooseRewardThresholds(100, 200)
        answer(model)
        shadowOf(Looper.getMainLooper()).idle()
        val result = requireNotNull(model.lastResult)
        assertNull(result.prizeType)
        assertEquals(RewardThresholds(100, 200), result.rewardThresholds)
        assertNotNull(model.celebration)
        model.chooseRewardThresholds(1, 2)
        model.dismissCelebration()
        model.showRewardForResult(result)
        model.claimReward()
        assertTrue(model.rewardBalances.isEmpty())
        assertEquals(result, model().history.single())
    }

    @Test fun rewardsDefaultOnAndSettingsPersistWithValidTimeSteps() {
        val first = model()
        assertTrue(first.rewardsEnabled)
        assertEquals(0, first.timeLimitSeconds)
        assertEquals(0, first.readingTimeLimitSeconds)
        assertNull(first.activeTimeLimitMs)
        first.chooseRewardsEnabled(false)
        first.chooseTimeLimitSeconds(900)
        val second = model()
        assertFalse(second.rewardsEnabled)
        assertEquals(900, second.timeLimitSeconds)
        second.chooseRewardsEnabled(true)
        assertTrue(model().rewardsEnabled)
        listOf(Int.MIN_VALUE to 0, -5 to 0, 0 to 0, 4 to 0, 5 to 5, 9 to 5,
            10 to 10, 11 to 11, 12 to 12, 13 to 13, 14 to 14, 15 to 15, 19 to 15, 20 to 20, 29 to 20,
            30 to 30, 44 to 30, 45 to 45, 59 to 45,
            60 to 60, 90 to 60, 120 to 120, 180 to 180, 240 to 240, 300 to 300,
            420 to 300, 600 to 600, 900 to 900, 901 to 900, 1_200 to 900,
            3_600 to 900, Int.MAX_VALUE to 900).forEach { (input, expected) ->
            second.chooseTimeLimitSeconds(input)
            assertEquals(expected, second.timeLimitSeconds)
            assertEquals(expected, model().timeLimitSeconds)
        }
    }

    @Test fun legacyMinuteSelectionsKeepSupportedDurationsAndCapLongerLimits() {
        val settings = application.getSharedPreferences("settings", 0)
        listOf(Int.MIN_VALUE to 0, 0 to 0, 1 to 60, 2 to 120, 3 to 180, 4 to 240,
            5 to 300, 7 to 300, 10 to 600, 15 to 900, 16 to 900,
            20 to 900, 60 to 900, Int.MAX_VALUE to 900)
            .forEach { (minutes, seconds) ->
                settings.edit().clear().putInt("time_limit_minutes", minutes).commit()
                val model = model()
                assertEquals(seconds, model.timeLimitSeconds)
                model.chooseTimeLimitSeconds(30)
                assertEquals(30, model().timeLimitSeconds)
                model.chooseTimeLimitSeconds(0)
                assertEquals(0, model().timeLimitSeconds)
            }
    }

    @Test fun savedSecondSelectionsAboveFifteenMinutesAreCappedOnLoad() {
        val settings = application.getSharedPreferences("settings", 0)
        ((20..60 step 5).map { it * 60 } + listOf(901, Int.MAX_VALUE)).forEach { seconds ->
            settings.edit().putInt("time_limit_seconds", seconds)
                .putInt("time_limit_minutes", 1).commit()
            assertEquals(900, model().timeLimitSeconds)
        }
    }

    @Test fun mathStartupSnapshotsLegacyReadingLimitBeforeAnyMathChanges() {
        val settings = application.getSharedPreferences("settings", 0)
        for (showTimer in listOf(false, true)) {
            listOf("time_limit_minutes" to 3, "time_limit_seconds" to 45).forEach { (key, value) ->
                settings.edit().clear().putBoolean("show_timer", showTimer)
                    .putBoolean("rewards_enabled", false).putInt(key, value).commit()
                val expected = if (!showTimer) 0 else if (key == "time_limit_minutes") 180 else 45
                val model = model()
                assertEquals(expected, model.readingTimeLimitSeconds)
                assertEquals(expected, settings.getInt("reading_time_limit_seconds", -1))
                model.chooseShowTimer(!showTimer)
                model.chooseTimeLimitSeconds(600)
                model.chooseRewardsEnabled(true)
                assertEquals(expected, model.readingTimeLimitSeconds)
                assertEquals(expected, model().readingTimeLimitSeconds)
                assertEquals(expected, settings.readReadingTimeLimitSeconds())
            }
        }
    }

    @Test fun readingLimitUsesTheSameNormalizationAndPersistsIndependentlyIncludingNone() {
        val settings = application.getSharedPreferences("settings", 0)
        settings.edit().putBoolean("show_timer", true).putInt("time_limit_seconds", 300).commit()
        val cases = timeLimitOptionsSeconds.map { it to it } + listOf(
            Int.MIN_VALUE to 0, 4 to 0, 9 to 5, 19 to 15, 29 to 20,
            44 to 30, 59 to 45, 90 to 60,
            420 to 300, 899 to 600, 901 to 900, Int.MAX_VALUE to 900)
        cases.forEach { (input, expected) ->
            settings.edit().putInt("reading_time_limit_seconds", input).commit()
            val model = model()
            assertEquals(expected, model.readingTimeLimitSeconds)
            assertEquals(expected, settings.readReadingTimeLimitSeconds())
            model.chooseReadingTimeLimitSeconds(input)
            assertEquals(expected, model.readingTimeLimitSeconds)
            assertEquals(expected, settings.getInt("reading_time_limit_seconds", -1))
            val restored = model()
            assertEquals(expected, restored.readingTimeLimitSeconds)
            assertEquals(300, model.timeLimitSeconds)
            assertEquals(300, restored.timeLimitSeconds)
            assertTrue(restored.showTimer)
            assertTrue(restored.rewardsEnabled)
        }
    }

    @Test fun futureSubjectLimitsDefaultToNoneAndNormalizeAndPersistIndependently() {
        val settings = application.getSharedPreferences("settings", 0)
        settings.edit().putBoolean("show_timer", true).putInt("time_limit_seconds", 11)
            .putInt("reading_time_limit_seconds", 12).commit()
        val first = model()
        assertEquals(0, first.scienceTimeLimitSeconds)
        assertEquals(0, first.advancedTimeLimitSeconds)
        first.chooseScienceTimeLimitSeconds(13)
        first.chooseAdvancedTimeLimitSeconds(14)
        val cases = timeLimitOptionsSeconds.map { it to it } + listOf(
            Int.MIN_VALUE to 0, -1 to 0, 4 to 0, 9 to 5, 19 to 15, 29 to 20,
            44 to 30, 59 to 45, 90 to 60, 420 to 300, 899 to 600, 901 to 900, Int.MAX_VALUE to 900)
        listOf(Subject.SCIENCE, Subject.ADVANCED).forEach { subject ->
            val key = if (subject == Subject.SCIENCE) "science_time_limit_seconds" else "advanced_time_limit_seconds"
            cases.forEach { (input, expected) ->
                settings.edit().putInt(key, input).commit()
                val loaded = model()
                if (subject == Subject.SCIENCE) {
                    assertEquals(expected, loaded.scienceTimeLimitSeconds)
                    loaded.chooseScienceTimeLimitSeconds(input)
                    assertEquals(expected, loaded.scienceTimeLimitSeconds)
                } else {
                    assertEquals(expected, loaded.advancedTimeLimitSeconds)
                    loaded.chooseAdvancedTimeLimitSeconds(input)
                    assertEquals(expected, loaded.advancedTimeLimitSeconds)
                }
                assertEquals(expected, settings.getInt(key, -1))
                val restored = model()
                assertEquals(if (subject == Subject.SCIENCE) expected else 13, restored.scienceTimeLimitSeconds)
                assertEquals(if (subject == Subject.ADVANCED) expected else 14, restored.advancedTimeLimitSeconds)
                assertEquals(11, restored.timeLimitSeconds)
                assertEquals(12, restored.readingTimeLimitSeconds)
                assertTrue(restored.showTimer)
                Subject.entries.forEach { assertTrue(restored.rewardsEnabled(it)) }
            }
            first.chooseScienceTimeLimitSeconds(13)
            first.chooseAdvancedTimeLimitSeconds(14)
        }
        first.chooseTimeLimitSeconds(0)
        first.chooseReadingTimeLimitSeconds(0)
        assertEquals(13, model().scienceTimeLimitSeconds)
        assertEquals(14, model().advancedTimeLimitSeconds)
    }

    @Test fun otherSubjectChoicesDoNotChangeMathReadyOrActiveTimeLimits() {
        val model = model()
        model.chooseShowTimer(true)
        model.chooseTimeLimitSeconds(60)
        model.openRewardsGame()
        model.updateRewardsQuestionCount("25")
        model.selectRewardsGame(Operation.ADDITION)
        listOf(11, 12, 13, 14, 30, 900, 0).forEach { seconds ->
            model.chooseReadingTimeLimitSeconds(seconds)
            model.chooseScienceTimeLimitSeconds(seconds)
            model.chooseAdvancedTimeLimitSeconds(seconds)
            assertEquals(300_000L, model.readyTimeLimitMs)
        }
        model.start()
        listOf(11, 12, 13, 14, 900, 0).forEach { seconds ->
            model.chooseReadingTimeLimitSeconds(seconds)
            model.chooseScienceTimeLimitSeconds(seconds)
            model.chooseAdvancedTimeLimitSeconds(seconds)
            assertEquals(300_000L, model.activeTimeLimitMs)
        }
        listOf(Subject.READING, Subject.SCIENCE, Subject.ADVANCED).forEach {
            model.chooseSubjectRewardsEnabled(it, false)
        }
        assertEquals(300_000L, model.activeTimeLimitMs)
        assertEquals(60, model.timeLimitSeconds)
        assertTrue(model.showTimer)
        assertTrue(model.rewardsEnabled)
    }

    @Test fun rewardsTimeLimitsScalePerFiveQuestionsIncludingShortAndLargeGames() {
        val model = model()
        model.chooseShowTimer(true)
        listOf(5, 10, 11, 12, 13, 14, 15, 20, 30, 45, 60, 120, 180, 240, 300, 600, 900).forEach { seconds ->
            listOf(1 to 200L, 5 to 1_000L, 15 to 3_000L, 25 to 5_000L,
                26 to 5_200L, 50 to 10_000L, 1000 to 200_000L).forEach { (count, perSecond) ->
                model.openRewardsGame()
                model.updateRewardsQuestionCount(count.toString())
                model.chooseTimeLimitSeconds(seconds)
                model.selectRewardsGame(Operation.ADDITION)
                assertEquals(perSecond * seconds, model.readyTimeLimitMs)
                model.updateQuestionCount("100")
                model.updateRewardsQuestionCount("100")
                model.start()
                assertEquals(perSecond * seconds, model.activeTimeLimitMs)
                now += perSecond * seconds - 1
                assertFalse(model.checkTimeLimit())
                now++
                assertTrue(model.checkTimeLimit())
                assertEquals(perSecond * seconds, model.lastResult!!.durationMs)
                assertTrue(model.lastResult!!.timedOut)
                assertNull(model.lastResult!!.prizeType)
                model.done()
            }
        }
    }

    @Test fun noTimeLimitWithoutRewardsTimerOrSelectedDuration() {
        val model = model()
        listOf(Triple(false, true, 30), Triple(true, false, 30),
            Triple(false, true, 60), Triple(true, false, 60), Triple(true, true, 0))
            .forEach { (rewards, timer, seconds) ->
                model.chooseRewardsEnabled(rewards)
                model.chooseShowTimer(timer)
                model.chooseTimeLimitSeconds(seconds)
                model.openRewardsGame()
                model.updateRewardsQuestionCount("25")
                model.selectRewardsGame(Operation.ADDITION)
                assertNull(model.readyTimeLimitMs)
                model.start()
                assertNull(model.activeTimeLimitMs)
                now += 600_000L
                assertFalse(model.checkTimeLimit())
                model.backToSetup()
            }
    }

    @Test fun celebrationPrecedesThePrizeAndClaimIsIdempotentUntilExplicitDismissal() {
        val model = model()
        start(model)
        finish(model)
        assertNotNull(model.celebration)
        assertFalse(model.rewardDialogVisible)
        assertNotNull(model.rewardResult?.prizeType)
        assertNull(model.history.single().prize)
        model.claimReward()
        assertTrue(model.rewardBalances.isEmpty())
        model.dismissCelebration()
        assertTrue(model.rewardDialogVisible)
        repeat(3) { model.claimReward() }
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
        assertNotNull(model.lastResult?.prize)
        assertEquals(model.lastResult, model.rewardResult)
        assertEquals(model.lastResult, model.history.single())
        assertTrue(model.rewardDialogVisible)
        model.dismissReward()
        assertFalse(model.rewardDialogVisible)
        model.done()
        assertEquals(1, model.history.size)
    }

    @Test fun individualRewardsDefaultOnAndDisablingAndReenablingPersist() {
        val first = model()
        assertTrue(first.disabledRewards.isEmpty())
        RewardType.entries.forEach { first.chooseRewardEnabled(it, false) }
        val restored = model()
        assertEquals(RewardType.entries.toSet(), restored.disabledRewards)
        assertTrue(restored.rewardsEnabled)
        RewardType.entries.forEach { type ->
            restored.chooseRewardEnabled(type, true)
            assertFalse(type in restored.disabledRewards)
            assertEquals(restored.disabledRewards, model().disabledRewards)
        }
        assertTrue(model().disabledRewards.isEmpty())
    }

    @Test fun unknownDisabledRewardNamesAreIgnoredWithoutLosingKnownPreferences() {
        application.getSharedPreferences("settings", 0).edit()
            .putStringSet("disabled_rewards", setOf("LOLLIPOP", "UNKNOWN_REWARD")).commit()
        assertEquals(setOf(RewardType.LOLLIPOP), model().disabledRewards)
    }

    @Test fun practicesUsePersistedRewardChoicesAndStillCelebrateWithAnEmptyTier() {
        val first = model()
        RewardType.entries.forEach { first.chooseRewardEnabled(it, false) }
        val restored = model()
        RewardType.entries.forEach { type ->
            val count = if (type.tier == RewardTier.TIER_1) 25 else 50
            start(restored, count)
            finish(restored, count)
            assertNull(restored.lastResult!!.prizeType)
            assertNull(restored.rewardResult)
            assertNotNull(restored.celebration)
            restored.done()

            restored.chooseRewardEnabled(type, true)
            start(restored, count)
            finish(restored, count)
            assertEquals(type, restored.lastResult!!.prizeType)
            restored.done()
            restored.chooseRewardEnabled(type, false)
        }
    }

    @Test fun rewardChoicesAtFinishApplyToAnAlreadyRunningPractice() {
        val model = model()
        start(model, 50)
        model.openSettings()
        RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
        model.chooseRewardEnabled(RewardType.RESTAURANT, true)
        model.closeOverlay()
        finish(model, 50)
        assertEquals(RewardType.RESTAURANT, model.lastResult!!.prizeType)
    }

    @Test fun disablingRewardsDoesNotRemoveInventoryOrPreventClaimingAndUsingExistingPrizes() {
        val model = model()
        RewardType.entries.forEach { model.chooseRewardEnabled(it, it == RewardType.BED_TIME) }
        repeat(3) {
            start(model)
            finish(model)
            model.dismissCelebration()
            model.claimReward()
            model.done()
        }
        start(model)
        finish(model)
        val pending = model.lastResult!!
        model.done()
        val before = HistoryStore(file).loadSnapshot()
        model.chooseRewardEnabled(RewardType.BED_TIME, false)
        assertEquals(before, HistoryStore(file).loadSnapshot())

        val restored = model()
        assertEquals(RewardType.entries.toSet(), restored.disabledRewards)
        restored.openHistory()
        restored.showRewardForResult(pending)
        restored.claimReward()
        assertEquals(RewardBalance(1, 1), restored.rewardBalances[RewardType.BED_TIME])
        restored.openRewards()
        restored.useReward(RewardType.BED_TIME)
        assertEquals(RewardBalance(0, 1), restored.rewardBalances[RewardType.BED_TIME])
        assertEquals(restored.rewardBalances, HistoryStore(file).loadSnapshot().rewards)
        assertEquals(before.pokemons, restored.pokemons)
    }

    @Test fun pendingHistoryPrizeCanBeClaimedAfterReloadWithoutChangingUnrelatedLastResult() {
        val first = model()
        start(first)
        finish(first)
        val pending = first.history.single()
        first.done()
        val restored = model()
        assertFalse(restored.rewardDialogVisible)
        restored.openHistory()
        restored.showHistoryDetail(restored.history.single())
        restored.showRewardForResult(pending)
        assertEquals(pending, restored.rewardResult)
        assertTrue(restored.rewardDialogVisible)
        restored.claimReward()
        assertNull(restored.lastResult)
        assertNotNull(restored.historyDetail?.prize)
        assertEquals(1, restored.rewardBalances.values.sumOf { it.totalFragments })
        val again = model()
        again.showRewardForResult(pending)
        repeat(3) { again.claimReward() }
        assertEquals(1, again.rewardBalances.values.sumOf { it.totalFragments })
        assertNotNull(again.rewardResult?.prize)
    }

    @Test fun automaticPrizeDialogDoesNotAppearOverUnrelatedHistoryOrSettings() {
        val model = model()
        start(model)
        finish(model)
        model.openHistory()
        model.dismissCelebration()
        assertFalse(model.rewardDialogVisible)
        model.closeOverlay()
        assertEquals(Overlay.SETTINGS, model.overlay)
        assertFalse(model.rewardDialogVisible)
        model.closeOverlay()
        assertTrue(model.rewardDialogVisible)
        model.dismissReward()
        model.openHistory()
        model.showRewardForResult(model.history.single())
        assertTrue(model.rewardDialogVisible)
        model.openSettings()
        assertFalse(model.rewardDialogVisible)
        model.openHistory()
        assertFalse(model.rewardDialogVisible)
    }

    @Test fun countAndAccuracyBoundariesAreAppliedToCompletedPractices() {
        val model = model()
        listOf(Triple(24, 0, false), Triple(25, 0, true), Triple(25, 2, true), Triple(25, 3, false),
            Triple(26, 2, true), Triple(49, 4, true), Triple(50, 0, true), Triple(50, 4, true),
            Triple(50, 5, false), Triple(51, 4, true), Triple(100, 10, false)).forEach { (count, wrong, eligible) ->
            start(model, count)
            finish(model, count, wrong)
            assertEquals(count, model.lastResult!!.questionCount)
            assertEquals(eligible, model.lastResult!!.prizeType != null)
            if (eligible) {
                val type = model.lastResult!!.prizeType!!
                assertEquals(if (count >= 50) RewardTier.TIER_2 else RewardTier.TIER_1, type.tier)
                model.dismissCelebration()
                model.claimReward()
                assertEquals(type, model.lastResult!!.prize!!.type)
                assertEquals(model.lastResult, HistoryStore(file).load().first())
                assertEquals(model.rewardBalances, HistoryStore(file).loadSnapshot().rewards)
            }
            model.done()
        }
    }

    @Test fun bedTimeAndRestaurantCanBeEarnedClaimedAndReloaded() {
        listOf(RewardType.BED_TIME to 25, RewardType.RESTAURANT to 50).forEach { (type, count) ->
            val seed = (0..100).first {
                selectPrize(count, count, count, true, true, random = Random(it)) == type
            }
            val model = model(rewardRandom = Random(seed))
            start(model, count)
            finish(model, count)
            assertEquals(type, model.lastResult!!.prizeType)
            model.dismissCelebration()
            model.claimReward()
            assertEquals(PrizeAward(type, RewardBalance(0, 1)), model.lastResult!!.prize)
            assertEquals(RewardBalance(0, 1), model().rewardBalances[type])
            model.done()
        }
    }

    @Test fun rewardsMustBeEnabledAtBothStartAndFinish() {
        val model = model()
        model.chooseShowTimer(true)
        start(model, rewards = false, minutes = 5)
        model.chooseRewardsEnabled(true)
        assertNull(model.activeTimeLimitMs)
        finish(model)
        assertNull(model.lastResult!!.prizeType)
        start(model)
        model.chooseRewardsEnabled(false)
        finish(model)
        assertNull(model.lastResult!!.prizeType)
        start(model, count = 100)
        repeat(10) { answer(model, correct = false) }
        assertEquals(Screen.PLAYING, model.screen)
        answer(model, correct = false)
        assertEquals(Screen.RESULTS, model.screen)
        assertEquals(11, model.lastResult!!.attempts.size)
        assertNull(model.lastResult!!.prizeType)
    }

    @Test fun timeoutIsCheckedBeforeParsingAndCapsDurationAtTheExactLimit() {
        val model = model()
        model.chooseShowTimer(true)
        start(model, count = 25, minutes = 1)
        now += 299_999
        assertFalse(model.checkTimeLimit())
        model.updateAnswer("")
        now++
        model.submitAnswer()
        assertEquals(Screen.RESULTS, model.screen)
        assertTrue(model.lastResult!!.timedOut)
        assertEquals(300_000L, model.lastResult!!.durationMs)
        assertTrue(model.lastResult!!.attempts.isEmpty())
        assertNull(model.celebration)
        assertNull(model.lastResult!!.prizeType)
        assertFalse(model.rewardDialogVisible)
        assertFalse(model.checkTimeLimit())
        model.submitAnswer()
        assertEquals(1, model.history.size)
        assertTrue(HistoryStore(file).load().single().timedOut)
    }

    @Test fun timerCoroutineFinishesPracticeWithoutAnyInput() {
        val model = model()
        model.chooseShowTimer(true)
        start(model, count = 25, minutes = 1)
        now += 400_000
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(Screen.RESULTS, model.screen)
        assertTrue(model.lastResult!!.timedOut)
        assertEquals(300_000L, model.lastResult!!.durationMs)
        assertEquals(1, model.history.size)
    }

    @Test fun limitAndInputPauseInEveryOverlayAndInTheBackground() {
        val model = model()
        model.chooseShowTimer(true)
        start(model, count = 25, minutes = 1)
        now += 1_000
        val initialAnswer = model.game!!.problem.answer.toString()
        model.updateAnswer(initialAnswer)
        listOf<() -> Unit>(model::openSettings, model::openHistory, model::openRewards).forEach { open ->
            open()
            now += 400_000
            model.updateAnswer("99")
            model.submitAnswer()
            assertEquals(initialAnswer, model.answerText)
            assertTrue(model.game!!.attempts.isEmpty())
            assertFalse(model.checkTimeLimit())
            assertEquals(1_000L, model.elapsedMs())
            while (model.overlay != null) model.closeOverlay()
        }
        model.setForeground(false)
        now += 400_000
        model.updateAnswer("99")
        model.submitAnswer()
        assertFalse(model.checkTimeLimit())
        assertEquals(initialAnswer, model.answerText)
        assertEquals(1_000L, model.elapsedMs())
        model.setForeground(true)
        now += 298_999
        assertFalse(model.checkTimeLimit())
        now++
        assertTrue(model.checkTimeLimit())
        assertEquals(300_000L, model.lastResult!!.durationMs)
    }

    @Test fun backgroundPausingDefaultsOnAndIsRemembered() {
        val first = model()
        assertTrue(first.pauseInBackground)
        first.choosePauseInBackground(false)
        val restored = model()
        assertFalse(restored.pauseInBackground)
        restored.choosePauseInBackground(true)
        assertTrue(model().pauseInBackground)
    }

    @Test fun disablingBackgroundPausingKeepsCountingButStillBlocksInputAndPausesInOverlays() {
        val model = model()
        model.chooseShowTimer(true)
        model.choosePauseInBackground(false)
        start(model, count = 25, minutes = 1)
        now += 1_000
        val initialAnswer = model.game!!.problem.answer.toString()
        model.updateAnswer(initialAnswer)
        model.setForeground(false)
        now += 2_000
        model.updateAnswer("99")
        model.submitAnswer()
        assertEquals(initialAnswer, model.answerText)
        assertTrue(model.game!!.attempts.isEmpty())
        assertEquals(3_000L, model.elapsedMs())
        listOf<() -> Unit>(model::openSettings, model::openHistory, model::openRewards,
            model::openPokemons).forEach { open ->
            open()
            now += 400_000
            assertEquals(3_000L, model.elapsedMs())
            assertFalse(model.checkTimeLimit())
            while (model.overlay != null) model.closeOverlay()
        }
        model.setForeground(true)
        now += 1_000
        assertEquals(4_000L, model.elapsedMs())
        assertEquals(Screen.PLAYING, model.screen)
    }

    @Test fun disablingBackgroundPausingAllowsTimeoutWithoutInput() {
        val model = model()
        model.chooseShowTimer(true)
        model.choosePauseInBackground(false)
        start(model, count = 25, minutes = 1)
        model.setForeground(false)
        now += 400_000
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(Screen.RESULTS, model.screen)
        assertTrue(model.lastResult!!.timedOut)
        assertEquals(300_000L, model.lastResult!!.durationMs)
        assertNull(model.lastResult!!.prizeType)
        assertNull(model.celebration)
        model.setForeground(true)
        assertEquals(1, model.history.size)
    }

    @Test fun backgroundTimeoutIsCheckedImmediatelyOnResumeAfterSuspendedCallbacks() {
        val model = model()
        model.chooseShowTimer(true)
        model.choosePauseInBackground(false)
        start(model, count = 25, minutes = 1)
        answer(model)
        model.setForeground(false)
        now += 400_000
        model.setForeground(true)
        assertEquals(Screen.RESULTS, model.screen)
        assertTrue(model.lastResult!!.timedOut)
        assertEquals(300_000L, model.lastResult!!.durationMs)
        assertEquals(1, model.lastResult!!.attempts.size)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(1, model.history.size)
    }

    @Test fun changingBackgroundPausingDoesNotCountPreviouslyPausedTimeOrLoseElapsedTime() {
        val model = model()
        start(model)
        now += 1_000
        model.setForeground(false)
        now += 10_000
        model.choosePauseInBackground(false)
        assertEquals(1_000L, model.elapsedMs())
        now += 2_000
        model.choosePauseInBackground(true)
        now += 10_000
        assertEquals(3_000L, model.elapsedMs())
        model.setForeground(true)
        now += 1_000
        assertEquals(4_000L, model.elapsedMs())
    }

    @Test fun timeLimitIsCapturedAtStartAndCannotBeDisabledByHidingTheTimer() {
        val model = model()
        model.chooseShowTimer(true)
        start(model, count = 25, minutes = 1)
        assertEquals(300_000L, model.activeTimeLimitMs)
        model.chooseShowTimer(false)
        model.chooseTimeLimitSeconds(900)
        now += 300_000
        assertTrue(model.checkTimeLimit())
        assertTrue(model.lastResult!!.timedOut)
        start(model, minutes = 5)
        assertNull(model.activeTimeLimitMs)
        model.chooseShowTimer(true)
        now += 400_000
        assertFalse(model.checkTimeLimit())
        model.backToSetup()
        assertNull(model.activeTimeLimitMs)
    }

    @Test fun saveAndClaimFailuresLeaveAPendingPrizeThatCanBeRetriedExactlyOnce() {
        val model = model()
        start(model)
        assertTrue(blockedWrite.mkdir())
        finish(model)
        assertTrue(model.history.isEmpty())
        assertNotNull(model.message)
        model.dismissCelebration()
        model.claimReward()
        assertNotNull(model.rewardError)
        assertNull(model.rewardResult?.prize)
        assertTrue(model.rewardBalances.isEmpty())
        assertTrue(blockedWrite.delete())
        model.claimReward()
        assertNull(model.rewardError)
        assertNotNull(model.rewardResult?.prize)
        assertEquals(1, model.history.size)
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
        model.claimReward()
        assertEquals(model.history, HistoryStore(file).load())
        assertEquals(model.rewardBalances, HistoryStore(file).loadSnapshot().rewards)
    }

    @Test fun claimFailureDoesNotUpdateDisplayedOrPersistedBalances() {
        val model = model()
        start(model)
        finish(model)
        model.dismissCelebration()
        val before = file.readText()
        assertTrue(blockedWrite.mkdir())
        model.claimReward()
        assertNotNull(model.rewardError)
        assertFalse(model.claimingReward)
        assertTrue(model.rewardBalances.isEmpty())
        assertNull(model.lastResult!!.prize)
        assertEquals(before, file.readText())
        assertTrue(blockedWrite.delete())
        model.claimReward()
        assertNotNull(model.lastResult!!.prize)
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
    }

    @Test fun claimsWaitForResultSavingAndRepeatedTapsDoNotDuplicateIt() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        start(model)
        finish(model)
        model.dismissCelebration()
        repeat(4) { model.claimReward() }
        assertTrue(model.claimingReward)
        assertTrue(model.history.isEmpty())
        dispatcher.drain()
        assertFalse(model.claimingReward)
        assertNotNull(model.lastResult!!.prize)
        assertEquals(1, model.history.size)
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
    }

    @Test fun lateSavesAndClaimsDoNotReopenResultsAfterNavigation() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        start(model)
        finish(model)
        model.dismissCelebration()
        model.claimReward()
        model.done()
        dispatcher.drain()
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.lastResult)
        assertNull(model.rewardResult)
        assertFalse(model.rewardDialogVisible)
        assertEquals(1, model.history.size)
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
        start(model)
        finish(model)
        model.backToSetup()
        dispatcher.drain()
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.lastResult)
        assertNull(model.rewardResult)
        assertEquals(2, model.history.size)
    }

    @Test fun clockRollbackAndLateSavesKeepResultIdsUniqueAndTheNewestScreenIntact() {
        val original = model()
        start(original)
        finish(original)
        val firstId = original.lastResult!!.id
        wallTime -= 100_000
        val dispatcher = QueuedDispatcher()
        val restored = model(dispatcher)
        start(restored)
        finish(restored)
        restored.done()
        start(restored, count = 50)
        finish(restored, count = 50)
        restored.dismissCelebration()
        restored.claimReward()
        dispatcher.drain()
        assertEquals(3, restored.history.map { it.id }.toSet().size)
        assertTrue(restored.history.filter { it.id != firstId }.all { it.id > firstId })
        assertEquals(50, restored.lastResult!!.questionCount)
        assertEquals(RewardTier.TIER_2, restored.rewardResult!!.prize!!.type.tier)
        assertEquals(restored.lastResult!!.prizeType, restored.rewardResult!!.prize!!.type)
        assertEquals(1, restored.rewardBalances.values.sumOf { it.totalFragments })
    }

    @Test fun deletingAndClearingResultsRemovesTheirRewardsThroughTheModel() {
        val model = model()
        start(model)
        finish(model)
        model.dismissCelebration()
        model.claimReward()
        val deleted = model.lastResult!!
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
        model.deleteResult(model.lastResult!!.id)
        assertTrue(model.history.isEmpty())
        assertEquals(0, model.rewardBalances.values.sumOf { it.totalFragments })
        assertNull(model.lastResult!!.prize)
        assertNull(model.lastResult!!.prizeType)
        model.showRewardForResult(deleted)
        assertNull(model.rewardResult)
        assertEquals(model.rewardBalances, model().rewardBalances)
        model.done()
        start(model)
        finish(model)
        model.dismissCelebration()
        model.claimReward()
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
        model.clearHistory()
        assertEquals(0, model().rewardBalances.values.sumOf { it.totalFragments })
        assertNull(model.lastResult!!.prize)
        assertNull(model.rewardResult)
    }

    @Test fun unreadableInventoryReportsAnErrorAndCannotBeCleared() {
        file.writeText("""{"history":[],"rewards":{"LOLLIPOP":{"whole":1,"fragments":9}}}""")
        val before = file.readText()
        val model = model()
        assertNotNull(model.message)
        model.clearHistory()
        assertNotNull(model.message)
        assertEquals(before, file.readText())
    }

    @Test fun deletionAndClearSerializeWithPendingClaimsInEitherOrder() {
        listOf(false, true).forEach { clear ->
            listOf(false, true).forEach { claimFirst ->
                val dispatcher = QueuedDispatcher()
                val model = model(dispatcher)
                dispatcher.drain()
                start(model)
                finish(model)
                dispatcher.drain()
                model.dismissCelebration()
                val id = model.lastResult!!.id
                if (claimFirst) model.claimReward()
                if (clear) model.clearHistory() else model.deleteResult(id)
                if (!claimFirst) model.claimReward()
                dispatcher.drain()
                assertTrue(model.history.isEmpty())
                assertEquals(0, model.rewardBalances.values.sumOf { it.totalFragments })
                assertEquals(model.rewardBalances, HistoryStore(file).loadSnapshot().rewards)
                assertNull(model.rewardResult)
                assertNull(model.lastResult!!.prizeType)
                assertFalse(model.claimingReward)
            }
        }
    }

    @Test fun usingRewardsWorksWhenEarningIsDisabledAndSurvivesReload() {
        collectLollipops()
        val model = model()
        model.chooseRewardsEnabled(false)
        val history = model.history
        assertFalse(model.rewardsEnabled)
        model.openRewards()
        model.useReward(RewardType.LOLLIPOP)
        assertFalse(model.usingReward)
        assertNull(model.rewardUseError)
        assertEquals(RewardBalance(1, 2), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(history, model.history)
        assertEquals(model.rewardBalances, model().rewardBalances)
        model.clearHistory()
        assertEquals(RewardBalance(), model().rewardBalances[RewardType.LOLLIPOP])
    }

    @Test fun useRequiresTheRewardsPageAndAnAvailableWholeReward() {
        collectLollipops()
        val model = model()
        model.useReward(RewardType.LOLLIPOP)
        assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
        model.openRewards()
        model.useReward(RewardType.VIDEO_GAME)
        repeat(4) { model.useReward(RewardType.LOLLIPOP) }
        assertEquals(RewardBalance(0, 2), model.rewardBalances[RewardType.LOLLIPOP])
        assertNull(model.rewardUseError)
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    @Test fun rapidUseRequestsOnlyConsumeOnceAndSerializeWithHistoryChanges() {
        collectLollipops()
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        dispatcher.drain()
        model.openSettings()
        model.openRewards()
        dispatcher.drain()
        repeat(4) { model.useReward(RewardType.LOLLIPOP) }
        assertTrue(model.usingReward)
        assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
        model.clearHistory()
        model.closeOverlay()
        dispatcher.drain()
        assertFalse(model.usingReward)
        assertEquals(Overlay.SETTINGS, model.overlay)
        assertTrue(model.history.isEmpty())
        assertEquals(RewardBalance(), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    @Test fun failedUseDoesNotChangeBalancesAndAllowsRetry() {
        collectLollipops()
        val model = model()
        model.openRewards()
        val before = file.readText()
        assertTrue(blockedWrite.mkdir())
        repeat(2) {
            model.useReward(RewardType.LOLLIPOP)
            assertFalse(model.usingReward)
            assertNotNull(model.rewardUseError)
            assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
            assertEquals(before, file.readText())
        }
        assertTrue(blockedWrite.delete())
        model.useReward(RewardType.LOLLIPOP)
        assertNull(model.rewardUseError)
        assertEquals(RewardBalance(1, 2), model.rewardBalances[RewardType.LOLLIPOP])
    }

    @Test fun adminAdditionsAllowEveryRewardWhenEarningIsDisabledAndPersistWithoutGameResults() {
        val model = model()
        Subject.entries.forEach { model.chooseSubjectRewardsEnabled(it, false) }
        RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
        model.openSettings()
        RewardType.entries.forEach { type ->
            model.addReward(type, fragment = true)
            assertEquals(RewardBalance(0, 1), model.rewardBalances[type])
            model.addReward(type, fragment = false)
            assertEquals(RewardBalance(1, 1), model.rewardBalances[type])
            repeat(2) { model.addReward(type, fragment = true) }
            assertEquals(RewardBalance(2, 0), model.rewardBalances[type])
            assertFalse(model.removingReward)
            assertNull(model.rewardRemovalError)
        }
        val restored = model()
        assertEquals(model.rewardBalances, restored.rewardBalances)
        assertEquals(RewardType.entries.toSet(), restored.disabledRewards)
        assertTrue(Subject.entries.none { restored.rewardsEnabled(it) })
        assertEquals(PracticeSnapshot(rewards = model.rewardBalances), HistoryStore(file).loadSnapshot())
    }

    @Test fun adminAdditionRequiresSettingsButNotAnExistingInventoryEntry() {
        val model = model()
        listOf(true, false).forEach { model.addReward(RewardType.LOLLIPOP, it) }
        model.openRewards()
        listOf(true, false).forEach { model.addReward(RewardType.LOLLIPOP, it) }
        assertTrue(model.rewardBalances.isEmpty())
        assertFalse(file.exists())
        assertFalse(model.removingReward)
        assertNull(model.rewardRemovalError)
        model.openSettings()
        model.addReward(RewardType.LOLLIPOP, fragment = false)
        assertEquals(RewardBalance(1, 0), model.rewardBalances[RewardType.LOLLIPOP])
    }

    @Test fun adminAdditionsPreserveCurrentAwardsAndAllPersistedHistoryMetadata() {
        val model = model()
        start(model)
        finish(model)
        model.dismissCelebration()
        model.claimReward()
        val current = requireNotNull(model.lastResult)
        val before = HistoryStore(file).loadSnapshot()
        val type = requireNotNull(current.prize).type
        model.openSettings()
        model.addReward(type, fragment = false)
        model.addReward(type, fragment = true)
        assertEquals(RewardBalance(1, 2), model.rewardBalances[type])
        assertEquals(current, model.lastResult)
        assertEquals(before.history, model.history)
        assertEquals(before.pokemons, model.pokemons)
        assertEquals(before.copy(rewards = model.rewardBalances), HistoryStore(file).loadSnapshot())
    }

    @Test fun adminAddAndRemoveShareOneBusyGateForBothUnitsAndEitherRequestOrder() {
        listOf(true, false).forEach { addFirst ->
            listOf(true, false).forEach { fragment ->
                file.delete()
                collectLollipops()
                val dispatcher = QueuedDispatcher()
                val model = model(dispatcher)
                dispatcher.drain()
                model.openSettings()
                if (addFirst) model.addReward(RewardType.LOLLIPOP, fragment)
                else model.removeReward(RewardType.LOLLIPOP, fragment)
                repeat(3) {
                    model.addReward(RewardType.LOLLIPOP, fragment)
                    model.removeReward(RewardType.LOLLIPOP, fragment)
                    model.addReward(RewardType.VIDEO_GAME, !fragment)
                }
                assertTrue(model.removingReward)
                assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
                dispatcher.drain()
                val total = 8 + (if (addFirst) 1 else -1) * (if (fragment) 1 else 3)
                assertEquals(RewardBalance(total / 3, total % 3), model.rewardBalances[RewardType.LOLLIPOP])
                assertNull(model.rewardBalances[RewardType.VIDEO_GAME])
                assertFalse(model.removingReward)
                assertNull(model.rewardRemovalError)
                if (addFirst) model.removeReward(RewardType.LOLLIPOP, fragment)
                else model.addReward(RewardType.LOLLIPOP, fragment)
                dispatcher.drain()
                val expected = if (addFirst && fragment) RewardBalance(3, 0) else RewardBalance(2, 2)
                assertEquals(expected, model.rewardBalances[RewardType.LOLLIPOP])
                assertEquals(model.rewardBalances, model().rewardBalances)
            }
        }
    }

    @Test fun adminAdditionsSerializeWithHistoryClearingAndKeepOnlyTheManualIncrement() {
        listOf(true, false).forEach { addFirst ->
            file.delete()
            collectLollipops()
            val dispatcher = QueuedDispatcher()
            val model = model(dispatcher)
            dispatcher.drain()
            model.openSettings()
            if (addFirst) model.addReward(RewardType.LOLLIPOP, fragment = true)
            model.clearHistory()
            if (!addFirst) model.addReward(RewardType.LOLLIPOP, fragment = true)
            model.closeOverlay()
            dispatcher.drain()
            assertTrue(model.history.isEmpty())
            assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
            assertNull(model.overlay)
            assertFalse(model.removingReward)
            assertEquals(model.rewardBalances, model().rewardBalances)
        }
    }

    @Test fun adminAdditionSerializesWithRewardUseAfterNavigation() {
        collectLollipops()
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        dispatcher.drain()
        model.openSettings()
        model.addReward(RewardType.LOLLIPOP, fragment = false)
        model.openRewards()
        model.useReward(RewardType.LOLLIPOP)
        dispatcher.drain()
        assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(Overlay.REWARDS, model.overlay)
        assertFalse(model.removingReward)
        assertFalse(model.usingReward)
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    @Test fun failedAdminAdditionsLeaveBalancesUnchangedAndClearErrorsOnEntryAndRetry() {
        collectLollipops()
        val model = model()
        model.openSettings()
        listOf(true, false).forEach { fragment ->
            val before = file.readText()
            val balance = model.rewardBalances
            assertTrue(blockedWrite.mkdir())
            repeat(2) {
                model.addReward(RewardType.LOLLIPOP, fragment)
                assertFalse(model.removingReward)
                assertTrue(requireNotNull(model.rewardRemovalError).startsWith("Unable to add"))
                assertEquals(balance, model.rewardBalances)
                assertEquals(before, file.readText())
            }
            model.closeOverlay()
            model.openSettings()
            assertNull(model.rewardRemovalError)
            model.addReward(RewardType.LOLLIPOP, fragment)
            assertNotNull(model.rewardRemovalError)
            assertTrue(blockedWrite.delete())
            model.addReward(RewardType.LOLLIPOP, fragment)
            assertNull(model.rewardRemovalError)
        }
        assertEquals(RewardBalance(4, 0), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    @Test fun adminAdditionOverflowReportsAnErrorWithoutSavingAndCanRetryAfterRemoval() {
        collectLollipops()
        val maximumWhole = (Int.MAX_VALUE - 2) / 3
        val original = HistoryStore(file).loadSnapshot().copy(rewards = mapOf(
            RewardType.LOLLIPOP to RewardBalance(maximumWhole, 2)
        ))
        file.writeText(Json.encodeToString(PracticeSnapshot.serializer(), original))
        val model = model()
        model.openSettings()
        listOf(true, false).forEach { fragment ->
            val before = file.readText()
            model.addReward(RewardType.LOLLIPOP, fragment)
            assertFalse(model.removingReward)
            assertTrue(requireNotNull(model.rewardRemovalError).startsWith("Unable to add"))
            assertEquals(original.rewards, model.rewardBalances)
            assertEquals(before, file.readText())
            assertEquals(original, HistoryStore(file).loadSnapshot())
        }
        model.removeReward(RewardType.LOLLIPOP, fragment = false)
        assertNull(model.rewardRemovalError)
        assertTrue(model.rewardBalances.getValue(RewardType.LOLLIPOP).canAddWhole)
        model.addReward(RewardType.LOLLIPOP, fragment = false)
        assertNull(model.rewardRemovalError)
        assertEquals(original.rewards, model.rewardBalances)
        assertEquals(original, HistoryStore(file).loadSnapshot())
    }

    @Test fun adminRemovalWorksWithRewardsDisabledAndPreservesHistoryAcrossReloads() {
        collectLollipops()
        HistoryStore(file).collectPokemon(Celebration.PIKACHU)
        val model = model()
        val before = HistoryStore(file).loadSnapshot()
        model.chooseRewardsEnabled(false)
        model.chooseRewardEnabled(RewardType.LOLLIPOP, false)
        model.openSettings()
        model.removeReward(RewardType.LOLLIPOP, fragment = true)
        assertEquals(RewardBalance(2, 1), model.rewardBalances[RewardType.LOLLIPOP])
        model.removeReward(RewardType.LOLLIPOP, fragment = false)
        assertEquals(RewardBalance(1, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertFalse(model.removingReward)
        assertNull(model.rewardRemovalError)
        assertEquals(before.history, model.history)
        assertEquals(before.pokemons, model.pokemons)
        assertEquals(model.rewardBalances, model().rewardBalances)
        assertEquals(before.copy(rewards = model.rewardBalances), HistoryStore(file).loadSnapshot())
    }

    @Test fun adminRemovalRequiresSettingsAndAnAvailableUnit() {
        collectLollipops()
        val model = model()
        model.removeReward(RewardType.LOLLIPOP, fragment = true)
        model.openRewards()
        model.removeReward(RewardType.LOLLIPOP, fragment = false)
        assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
        model.openSettings()
        model.removeReward(RewardType.VIDEO_GAME, fragment = true)
        repeat(4) { model.removeReward(RewardType.LOLLIPOP, fragment = true) }
        assertEquals(RewardBalance(2, 0), model.rewardBalances[RewardType.LOLLIPOP])
        repeat(4) { model.removeReward(RewardType.LOLLIPOP, fragment = false) }
        assertEquals(RewardBalance(), model.rewardBalances[RewardType.LOLLIPOP])
        assertNull(model.rewardRemovalError)
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    @Test fun rapidAdminRemovalsOnlyRemoveOnceAndSerializeWithUseAndHistoryChanges() {
        collectLollipops()
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        dispatcher.drain()
        model.openSettings()
        repeat(4) {
            model.removeReward(RewardType.LOLLIPOP, fragment = true)
            model.removeReward(RewardType.LOLLIPOP, fragment = false)
        }
        assertTrue(model.removingReward)
        assertEquals(RewardBalance(2, 2), model.rewardBalances[RewardType.LOLLIPOP])
        model.openRewards()
        model.useReward(RewardType.LOLLIPOP)
        dispatcher.drain()
        assertFalse(model.removingReward)
        assertFalse(model.usingReward)
        assertEquals(Overlay.REWARDS, model.overlay)
        assertEquals(RewardBalance(1, 1), model.rewardBalances[RewardType.LOLLIPOP])
        model.openSettings()
        model.removeReward(RewardType.LOLLIPOP, fragment = false)
        model.clearHistory()
        model.closeOverlay()
        dispatcher.drain()
        assertNull(model.overlay)
        assertTrue(model.history.isEmpty())
        assertEquals(RewardBalance(), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    @Test fun failedAdminRemovalDoesNotChangeBalancesAndAllowsRetry() {
        collectLollipops()
        val model = model()
        model.openSettings()
        listOf(true, false).forEach { fragment ->
            val before = file.readText()
            val balance = model.rewardBalances
            assertTrue(blockedWrite.mkdir())
            repeat(2) {
                model.removeReward(RewardType.LOLLIPOP, fragment)
                assertFalse(model.removingReward)
                assertNotNull(model.rewardRemovalError)
                assertEquals(balance, model.rewardBalances)
                assertEquals(before, file.readText())
            }
            assertTrue(blockedWrite.delete())
            model.removeReward(RewardType.LOLLIPOP, fragment)
            assertNull(model.rewardRemovalError)
        }
        assertEquals(RewardBalance(1, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(model.rewardBalances, model().rewardBalances)
    }

    private fun collectLollipops() {
        val store = HistoryStore(file)
        repeat(8) {
            val result = PracticeResult(it + 1L, it + 1L, Operation.ADDITION, 10, 1_000,
                List(26) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
                prizeType = RewardType.LOLLIPOP)
            store.add(result)
            store.claimReward(result.id)
        }
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.addLast(block) }
        fun drain() {
            shadowOf(Looper.getMainLooper()).idle()
            while (tasks.isNotEmpty()) {
                tasks.removeFirst().run()
                shadowOf(Looper.getMainLooper()).idle()
            }
        }
    }
}
