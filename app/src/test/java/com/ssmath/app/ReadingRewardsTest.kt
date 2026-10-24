package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
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
class ReadingRewardsTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "practice_history.json")
    private val blockedWrite get() = File(application.filesDir, "practice_history.json.tmp")
    private val models = mutableListOf<ViewModelStore>()

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

    private fun model(dispatcher: CoroutineDispatcher = Dispatchers.Unconfined): MathViewModel {
        val model = MathViewModel(application, ioDispatcher = dispatcher, celebrationRandom = Random(7))
        models += ViewModelStore().apply { put("model", model) }
        shadowOf(Looper.getMainLooper()).idle()
        return model
    }

    private fun finished(
        id: String = "session",
        count: Int = 25,
        wrong: Int = 0,
        answered: Int = count,
        timedOut: Boolean = false,
        enabledAtStart: Boolean = true,
        enabledAtFinish: Boolean = true,
        disabled: Set<RewardType> = RewardType.entries.toSet() - RewardType.LOLLIPOP,
        thresholds: RewardThresholds = RewardThresholds(),
        chances: Tier3RewardChances = Tier3RewardChances()
    ): ReadingData {
        val attempts = List(count) { index ->
            ReadingAttempt("$id-$index", "reader", "missing-cup", 1,
                finishedAt = 2L.takeIf { index < answered },
                checkpoints = if (index < answered) mapOf("answer" to ReadingCheckpoint(
                    tries = 1, firstCorrect = index >= wrong, firstAt = 1, earnedAt = 2,
                    readerId = "reader"
                )) else emptyMap())
        }
        val session = ReadingSession(id, "reader", ReadingGame.MISSING, ReadingLevel.SPROUT,
            attempts.map { it.id }, 1, rewardsEnabledAtStart = enabledAtStart,
            timeLimitMs = 300_000L.takeIf { timedOut }, elapsedMs = if (timedOut) 300_000 else 0)
        val data = ReadingData(profiles = listOf(ReadingProfile("reader", "Reader")),
            selectedProfileId = "reader", attempts = attempts, sessions = listOf(session))
        return ReadingEngine.endSession(data, id, 3, timedOut = timedOut, rewardsEnabled = enabledAtFinish,
            disabledRewards = disabled, random = Random(3), rewardThresholds = thresholds,
            tier3RewardChances = chances)
    }

    private fun open(model: MathViewModel, data: ReadingData) {
        if (model.overlay == null) model.openReading()
        model.openReadingSessionReward(data, data.sessions.single())
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun readingReceiptsUseFinishedSessionThresholdsNotCurrentMathSettings() {
        for ((count, type) in listOf(15 to RewardType.LOLLIPOP, 25 to RewardType.VIDEO_GAME)) {
            val data = finished(id = "custom-$count", count = count, thresholds = RewardThresholds(15, 25),
                disabled = RewardType.entries.toSet() - type)
            val model = model()
            model.chooseRewardThresholds(999, 1000)
            model.chooseMatrixRewardTier(RewardTier.TIER_1)
            model.chooseSubjectRewardsEnabled(Subject.READING, false)
            RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
            open(model, data)
            assertEquals(CelebrationCategory.POKEMONS, model.readingCelebration?.category)
            assertEquals(RewardThresholds(15, 25), model.readingRewardResult?.rewardThresholds)
            assertEquals(type, model.readingRewardResult?.prizeType)
            model.dismissReadingSessionReward()
            val restored = model()
            restored.openSubjectHistory(Subject.READING)
            restored.showReadingRewardForSession(data, data.sessions.single())
            shadowOf(Looper.getMainLooper()).idle()
            repeat(3) { restored.claimReadingReward() }
            assertEquals(RewardBalance(0, 1), restored.rewardBalances[type])
            assertEquals(PrizeAward(type, RewardBalance(0, 1)), restored.readingRewardResult?.prize)
            assertEquals(RewardThresholds(15, 25), restored.readingRewardResult?.rewardThresholds)
            assertTrue(restored.history.isEmpty())
            assertEquals(restored.readingRewardResults, HistoryStore(file).loadSnapshot().readingResults)
        }
    }

    @Test fun toyAwardsAndLegacyTierTwoAwardsSurviveChangedSettingsReplayAndRepeatedClaims() {
        val cases = listOf(
            Triple("old-tier-two", RewardType.VIDEO_GAME, RewardThresholds(tier3Minimum = 1001)),
            Triple("new-toy-one", RewardType.TOYS_1, RewardThresholds()),
            Triple("new-toy-two", RewardType.TOYS_2, RewardThresholds()),
            Triple("new-toy-three", RewardType.TOYS_3, RewardThresholds())
        )
        for ((id, type, thresholds) in cases) {
            val data = finished(id, count = 100, disabled = RewardType.entries.toSet() - type,
                thresholds = thresholds)
            assertEquals(type, data.sessions.single().prizeType)
            val model = model()
            open(model, data)
            model.dismissReadingSessionReward()
            model.chooseTier3RewardChances(0, 0, 100)
            model.chooseRewardThresholds(998, 999, 1000)
            model.chooseSubjectRewardsEnabled(Subject.READING, false)
            RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
            val restored = model()
            open(restored, data)
            assertNull(restored.readingCelebration)
            restored.openSubjectHistory(Subject.READING)
            restored.showReadingRewardForSession(data, data.sessions.single())
            repeat(3) { restored.claimReadingReward() }
            assertEquals(PrizeAward(type, RewardBalance(0, 1)), restored.readingRewardResult?.prize)
            assertEquals(thresholds, restored.readingRewardResult?.rewardThresholds)
            val again = model()
            again.openSubjectHistory(Subject.READING)
            again.showReadingRewardForSession(data, data.sessions.single())
            again.claimReadingReward()
            assertEquals(RewardBalance(0, 1), again.rewardBalances[type])
            assertTrue(again.history.isEmpty())
        }
    }

    @Test fun completionCelebratesBeforeClaimAndCollectsOnlyTheActuallyPresentedScene() {
        val model = model()
        open(model, finished())
        val selected = requireNotNull(model.readingCelebration)
        assertFalse(model.readingRewardDialogVisible)
        assertTrue(model.history.isEmpty())
        assertTrue(model.pokemons.isEmpty())
        assertNull(model.readingRewardResults.single().pokemonReward)
        model.claimReadingReward()
        assertTrue(model.rewardBalances.isEmpty())
        model.collectPresentedReadingCelebration(Celebration.entries.first { it != selected })
        assertTrue(model.pokemons.isEmpty())
        repeat(3) { model.collectPresentedReadingCelebration(selected) }
        assertEquals(setOf(selected), model.pokemons)
        assertEquals(selected, model.readingRewardResults.single().pokemonReward)
        model.dismissReadingCelebration()
        assertTrue(model.readingRewardDialogVisible)
        repeat(3) { model.claimReadingReward() }
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(model.readingRewardResults.single(), model.readingRewardResult)
        assertTrue(model.history.isEmpty())
        assertTrue(model.readingRewardDialogVisible)
        model.dismissReadingReward()
        assertFalse(model.readingRewardDialogVisible)
        assertEquals(model.rewardBalances, HistoryStore(file).loadSnapshot().rewards)
    }

    @Test fun reopeningAndRestartingCannotReplayOrDuplicateButHistoryCanClaimAnEarnedGift() {
        val data = finished()
        val first = model()
        open(first, data)
        first.dismissReadingSessionReward()
        open(first, data)
        assertNull(first.readingCelebration)
        assertFalse(first.readingRewardDialogVisible)
        assertTrue(first.pokemons.isEmpty())
        val restored = model()
        restored.chooseSubjectRewardsEnabled(Subject.READING, false)
        RewardType.entries.forEach { restored.chooseRewardEnabled(it, false) }
        open(restored, data)
        assertNull(restored.readingCelebration)
        assertFalse(restored.readingRewardDialogVisible)
        restored.openSubjectHistory(Subject.READING)
        restored.showReadingRewardForSession(data, data.sessions.single())
        assertNull(restored.readingCelebration)
        assertTrue(restored.readingRewardDialogVisible)
        restored.claimReadingReward()
        val claimed = requireNotNull(restored.readingRewardResult?.prize)
        assertEquals(RewardBalance(0, 1), claimed.balance)
        val again = model()
        again.openSubjectHistory(Subject.READING)
        again.showReadingRewardForSession(data, data.sessions.single())
        repeat(3) { again.claimReadingReward() }
        assertEquals(claimed, again.readingRewardResult?.prize)
        assertEquals(RewardBalance(0, 1), again.rewardBalances[RewardType.LOLLIPOP])
        assertTrue(again.pokemons.isEmpty())
        assertTrue(again.history.isEmpty())
    }

    @Test fun deletionDismissesReadingRewardsAndBlocksStaleSessionCallbacksAfterRestart() {
        val data = finished()
        val model = model()
        open(model, data)
        val selected = requireNotNull(model.readingCelebration)
        model.collectPresentedReadingCelebration(selected)
        model.dismissReadingCelebration()
        model.claimReadingReward()
        model.deleteReadingResults(setOf("session"))
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(model.readingRewardResult)
        assertNull(model.readingCelebration)
        assertTrue(model.readingRewardResults.isEmpty())
        assertTrue(model.pokemons.isEmpty())
        assertEquals(RewardBalance(), model.rewardBalances[RewardType.LOLLIPOP])
        model.showReadingRewardForSession(data, data.sessions.single())
        model.collectPresentedReadingCelebration(selected)
        model.claimReadingReward()
        assertNull(model.readingRewardResult)
        val restored = model()
        restored.openSubjectHistory(Subject.READING)
        restored.showReadingRewardForSession(data, data.sessions.single())
        assertNull(restored.readingRewardResult)
        assertTrue(restored.readingRewardResults.isEmpty())
        assertEquals(setOf("session"), HistoryStore(file).loadSnapshot().recordedReadingSessionIds)
    }

    @Test fun durableDeletionReceiptsFinishRewardCleanupAfterFailureAndRestart() {
        val data = finished()
        val model = model()
        open(model, data)
        model.dismissReadingCelebration()
        model.claimReadingReward()
        val deleted = ReadingEngine.clearHistory(data, "reader")
        assertEquals(setOf("session"), deleted.deletedSessionIds)
        assertTrue(blockedWrite.mkdir())
        model.deleteReadingResults(deleted.deletedSessionIds)
        shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(model.readingHistoryError)
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertNull(model.readingRewardResult)
        assertTrue(blockedWrite.delete())
        val restored = model()
        restored.deleteReadingResults(deleted.deletedSessionIds)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(restored.readingHistoryError)
        assertTrue(restored.readingRewardResults.isEmpty())
        assertEquals(RewardBalance(), restored.rewardBalances[RewardType.LOLLIPOP])
        restored.deleteReadingResults(deleted.deletedSessionIds)
        assertEquals(RewardBalance(), restored.rewardBalances[RewardType.LOLLIPOP])
    }

    @Test fun readingUsesRewardsCategoryThresholdAndTheSharedUncollectedPool() {
        val model = model()
        open(model, finished(id = "short", count = 14))
        assertEquals(CelebrationCategory.OTHER, model.readingCelebration?.category)
        assertNull(model.readingRewardResult)
        model.dismissReadingSessionReward()
        HistoryStore(file).collectPokemons(Celebration.entries.filter { it.category == CelebrationCategory.OTHER })
        open(model, finished(id = "short-complete-pool", count = 14))
        assertNull(model.readingCelebration)
        model.dismissReadingSessionReward()
        open(model, finished(id = "pokemon", count = 15))
        assertEquals(CelebrationCategory.POKEMONS, model.readingCelebration?.category)
        assertNull(model.readingRewardResult)
        assertFalse(requireNotNull(model.readingCelebration) in model.pokemons)
    }

    @Test fun readingWith121QuestionsCannotUnlockMegaEvenAfterFinishingTheStandardCollection() {
        HistoryStore(file).collectPokemons(Celebration.standardCelebrations)
        val model = model()
        open(model, finished(count = 121))
        val selected = requireNotNull(model.readingCelebration)
        assertNotEquals(CelebrationCategory.MEGA_POKEMONS, selected.category)
        model.collectPresentedReadingCelebration(selected)
        assertEquals(Celebration.standardCelebrations.toSet(), model.pokemons)
        assertEquals(selected, model.readingRewardResults.single().pokemonReward)
    }

    @Test fun partialTimedOutAbandonedAndUnfinishedSessionsNeverCelebrateOrGrantRewards() {
        val model = model()
        listOf(
            finished(id = "partial", answered = 20, wrong = 3),
            finished(id = "timeout", timedOut = true),
            finished(id = "unfinished").let { data ->
                data.copy(sessions = data.sessions.map { it.copy(finishedAt = null, prizeType = null) })
            },
            finished(id = "abandoned").let { data ->
                data.copy(sessions = data.sessions.map { it.copy(abandoned = true, prizeType = null) })
            }
        ).forEach { data ->
            open(model, data)
            assertNull(model.readingCelebration)
            assertNull(model.readingRewardResult)
            assertTrue(model.pokemons.isEmpty())
            assertTrue(model.rewardBalances.isEmpty())
            model.dismissReadingSessionReward()
        }
        assertEquals(setOf("partial", "timeout"), model.readingRewardResults.map { it.sessionId }.toSet())
    }

    @Test fun rewardSnapshotAndLiveFinishSettingsAndEmptyTierMatchMathButStillCelebrate() {
        val model = model()
        listOf(
            finished(id = "disabled-at-start", enabledAtStart = false),
            finished(id = "disabled-at-finish", enabledAtFinish = false),
            finished(id = "disabled-tier", disabled = RewardType.entries.filter { it.tier == RewardTier.TIER_1 }.toSet()),
            finished(id = "ninety-percent", count = 30, wrong = 3)
        ).forEach { data ->
            open(model, data)
            assertNotNull(model.readingCelebration)
            assertNull(model.readingRewardResult)
            assertTrue(model.rewardBalances.isEmpty())
            model.dismissReadingSessionReward()
        }
        model.chooseRewardsEnabled(false)
        open(model, finished(id = "reading-independent", wrong = 2))
        assertEquals(RewardType.LOLLIPOP, model.readingRewardResult?.prizeType)
        assertEquals("92", model.readingRewardResult?.percentCorrectText)
        model.dismissReadingSessionReward()
        open(model, finished(id = "tier-two", count = 50,
            disabled = RewardType.entries.toSet() - RewardType.RESTAURANT))
        assertEquals(RewardType.RESTAURANT, model.readingRewardResult?.prizeType)
    }

    @Test fun collectionRequiresLiveReadingPresentationAndNavigationDismissesBothDialogs() {
        val model = model()
        val data = finished()
        model.openReadingSessionReward(data, data.sessions.single())
        assertTrue(model.readingRewardResults.isEmpty())
        open(model, data)
        val selected = requireNotNull(model.readingCelebration)
        model.setForeground(false)
        model.collectPresentedReadingCelebration(selected)
        assertTrue(model.pokemons.isEmpty())
        model.setForeground(true)
        model.openSettings()
        model.collectPresentedReadingCelebration(selected)
        assertTrue(model.pokemons.isEmpty())
        assertNull(model.readingCelebration)
        assertNull(model.readingRewardResult)
        model.closeOverlay()
        assertEquals(Overlay.READING, model.overlay)
        assertFalse(model.readingRewardDialogVisible)
        model.openSubjectHistory(Subject.SCIENCE)
        model.showReadingRewardForSession(data, data.sessions.single())
        assertNull(model.readingRewardResult)
        model.openSubjectHistory(Subject.READING)
        model.showReadingRewardForSession(data, data.sessions.single())
        assertTrue(model.readingRewardDialogVisible)
        model.openRewards()
        assertNull(model.readingRewardResult)
        assertFalse(model.readingRewardDialogVisible)
    }

    @Test fun queuedSaveAndClaimCannotReopenDialogsAfterNavigationOrDuplicateRapidTaps() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        dispatcher.drain()
        val data = finished()
        open(model, data)
        repeat(3) { model.openReadingSessionReward(data, data.sessions.single()) }
        assertTrue(model.readingSelectingCelebration)
        assertFalse(model.readingRewardDialogVisible)
        model.openSettings()
        dispatcher.drain()
        assertNull(model.readingCelebration)
        assertFalse(model.readingRewardDialogVisible)
        assertEquals(1, model.readingRewardResults.size)
        assertTrue(model.pokemons.isEmpty())
        model.openSubjectHistory(Subject.READING)
        model.showReadingRewardForSession(data, data.sessions.single())
        dispatcher.drain()
        repeat(3) { model.claimReadingReward() }
        assertTrue(model.claimingReadingReward)
        model.openRewards()
        dispatcher.drain()
        assertFalse(model.claimingReadingReward)
        assertFalse(model.readingRewardDialogVisible)
        assertNull(model.readingRewardResult)
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
    }

    @Test fun failedSaveAndClaimAreRetryableWithoutPrematureInventoryOrLostMathResults() {
        val model = model()
        val data = finished()
        assertTrue(blockedWrite.mkdir())
        open(model, data)
        assertNotNull(model.readingRewardError)
        assertTrue(model.readingRewardDialogVisible)
        assertTrue(model.readingRewardResults.isEmpty())
        assertTrue(model.pokemons.isEmpty())
        model.claimReadingReward()
        assertFalse(model.claimingReadingReward)
        assertNotNull(model.readingRewardError)
        assertTrue(model.rewardBalances.isEmpty())
        assertTrue(blockedWrite.delete())
        model.claimReadingReward()
        assertNull(model.readingRewardError)
        assertNotNull(model.readingRewardResult?.prize)
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertTrue(model.pokemons.isEmpty())
        assertTrue(model.history.isEmpty())
        model.clearHistory()
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(model.readingRewardResults, HistoryStore(file).loadSnapshot().readingResults)
    }

    @Test fun readingClaimsSerializeWithMathClaimsAndAdminInventoryChanges() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        dispatcher.drain()
        model.openRewardsGame()
        model.updateRewardsQuestionCount("25")
        RewardType.entries.forEach { model.chooseRewardEnabled(it, it == RewardType.LOLLIPOP) }
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
        repeat(25) {
            model.updateAnswer(requireNotNull(model.game).problem.answer.toString())
            model.submitAnswer()
        }
        dispatcher.drain()
        model.dismissCelebration()
        model.claimReward()
        model.done()
        val data = finished()
        open(model, data)
        dispatcher.drain()
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        model.dismissReadingCelebration()
        model.claimReadingReward()
        model.openSettings()
        model.removeReward(RewardType.LOLLIPOP, fragment = true)
        dispatcher.drain()
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(1, model.history.size)
        assertNotNull(model.history.single().prize)
        assertNotNull(model.readingRewardResults.single().prize)
        assertEquals(model.rewardBalances, HistoryStore(file).loadSnapshot().rewards)
    }

    @Test fun readingClaimsAndAdminAdditionsSerializeWithoutChangingAwardsOrLosingManualRewardsOnDeletion() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher)
        dispatcher.drain()
        open(model, finished())
        dispatcher.drain()
        model.dismissReadingCelebration()
        model.claimReadingReward()
        model.openSettings()
        model.addReward(RewardType.LOLLIPOP, fragment = true)
        assertTrue(model.claimingReadingReward)
        assertTrue(model.removingReward)
        dispatcher.drain()
        assertEquals(RewardBalance(0, 2), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(PrizeAward(RewardType.LOLLIPOP, RewardBalance(0, 1)),
            model.readingRewardResults.single().prize)
        val claimed = model.readingRewardResults
        model.chooseSubjectRewardsEnabled(Subject.READING, false)
        model.chooseRewardEnabled(RewardType.LOLLIPOP, false)
        model.addReward(RewardType.LOLLIPOP, fragment = false)
        dispatcher.drain()
        assertEquals(RewardBalance(1, 2), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(claimed, model.readingRewardResults)
        model.deleteReadingResults(setOf(claimed.single().sessionId))
        dispatcher.drain()
        assertTrue(model.readingRewardResults.isEmpty())
        assertTrue(model.history.isEmpty())
        assertEquals(RewardBalance(1, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertFalse(model.removingReward)
        assertFalse(model.claimingReadingReward)
        assertEquals(model.rewardBalances, model().rewardBalances)
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
