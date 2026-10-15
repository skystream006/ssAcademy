package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
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
@Config(sdk = [34], application = Application::class)
class ReadingEntryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var store: ReadingSqliteStore
    private val legacy get() = File(application.filesDir, "reading_private.json")
    private val pool get() = ReadingContent.forGame(ReadingGame.MISSING, ReadingLevel.SPROUT)

    @Before fun setup() {
        application.deleteDatabase("reading.db")
        File(application.filesDir, "practice_history.json").delete()
        listOf(legacy, File(legacy.path + ".bak"), File(legacy.path + ".new")).forEach { it.delete() }
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        DebugLog.initialize(application)
        store = ReadingSqliteStore(application)
    }

    @After fun close() {
        compose.activity.viewModelStore.clear()
        store.database.close()
        application.deleteDatabase("reading.db")
        listOf(legacy, File(legacy.path + ".bak"), File(legacy.path + ".new")).forEach { it.delete() }
    }

    private fun reading(io: CoroutineDispatcher = Dispatchers.IO): ReadingViewModel =
        ViewModelProvider(compose.activity, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReadingViewModel(application, store, io,
                    readingTestImporter(store.catalog, application.cacheDir, pool)) as T
        })[ReadingViewModel::class.java]

    private fun show(): MathViewModel {
        val app = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        compose.setContent { MathAppContent(app) }
        return app
    }

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    @Test fun missingImportShowsInstructionsAndCancelStaysHomeWithoutLosingLegacyDraft() {
        val original = readingFixture()
        ReadingStore(legacy).also { it.load(); it.save(original) }
        val reading = reading()
        val app = show()
        await { reading.packStatus != null && !reading.saving }
        compose.onNodeWithTag("subject-Reading").performClick()
        compose.onNodeWithText("Import reading activities first").assertIsDisplayed()
        compose.onNodeWithText("Before playing reading games, go to Settings > Reading Settings", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("reading-menu").assertDoesNotExist()
        assertNull(app.overlay)
        assertEquals(original, store.load().withoutCatalogueRefs())
        compose.onNodeWithTag("reading-import-dismiss").performClick()
        compose.onNodeWithTag("subject-grid").assertIsDisplayed()
        compose.onNodeWithTag("reading-import-required").assertDoesNotExist()
        assertEquals(original, store.load().withoutCatalogueRefs())
    }

    @Test fun settingsShortcutImportsPackThenHomeReadingOpensWithoutTheDialog() {
        store.save(ReadingEngine.addProfile(store.load(), "Ari", "reader-a"))
        val reading = reading()
        val app = show()
        await { reading.packStatus != null && !reading.saving }
        compose.onNodeWithTag("subject-Reading").performClick()
        compose.onNodeWithTag("reading-import-open-settings").performClick()
        assertEquals(Overlay.SETTINGS, app.overlay)
        compose.onNodeWithText("Reading Settings").performScrollTo().performClick()
        compose.onNodeWithTag("reading-pack-import").performScrollTo().performClick()
        await { reading.packStatus?.version == "test-1" && !reading.importing }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("subject-Reading").performClick()
        compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        compose.onNodeWithTag("reading-import-required").assertDoesNotExist()
        assertEquals(Overlay.READING, app.overlay)
        assertTrue(requireNotNull(reading.data).attempts.isEmpty())
    }

    @Test fun completedReadingBatchPresentsAndClaimsSharedRewardsWithoutMathHistory() {
        runBlocking { readingTestImporter(store.catalog, application.cacheDir, pool).importPack() }
        store.save(ReadingEngine.addProfile(store.load(), "Ari", "reader-a"))
        val reading = reading()
        val app = show()
        await { reading.data != null && reading.packStatus != null && !reading.saving }
        compose.onNodeWithTag("subject-Reading").performClick()
        compose.onNodeWithTag("reading-question-count-25").performClick()
        compose.onNodeWithTag("reading-game-MISSING").performScrollTo().performClick()
        await { reading.data?.activeSession?.questionCount == 25 && !reading.saving }
        val assigned = requireNotNull(reading.data?.activeSession).attemptIds
        repeat(25) { index ->
            await { reading.data?.active?.id == assigned[index] && !reading.saving }
            val question = requireNotNull(reading.data?.active?.content?.question)
            compose.runOnIdle {
                reading.act { data -> ReadingEngine.field(data, "answer", readingCorrect(data).toString()) }
                reading.submit("answer")
            }
            await {
                reading.data?.let { it.sessionAnswered(requireNotNull(it.activeSession)) == index + 1 } == true &&
                    !reading.saving
            }
            compose.onNodeWithTag("reading-previous-answer").performScrollTo()
                .assertTextContains("Correct answer: ${question.choices[question.answer]}", substring = true)
            if (index < 24) assertNull(reading.data?.attempts?.get(index)?.loadedContent)
        }
        compose.onNodeWithTag("reading-session-summary").assertIsDisplayed()
        await { app.readingRewardResults.singleOrNull()?.pokemonReward != null }
        compose.runOnIdle { app.dismissReadingCelebration() }
        compose.onNodeWithTag("gift-box").assertIsDisplayed().performClick()
        await { app.readingRewardResult?.prize != null && !app.claimingReadingReward }
        assertEquals(1, app.rewardBalances.values.sumOf { it.totalFragments })
        assertTrue(app.history.isEmpty())
        assertEquals(25, reading.data?.activeSession?.questionCount)
        val snapshot = HistoryStore(File(application.filesDir, "practice_history.json")).loadSnapshot()
        assertEquals(1, snapshot.readingResults.size)
        assertNotNull(snapshot.readingResults.single().prize)
        assertEquals(1, snapshot.pokemons.size)
    }

    @Test fun deletingReadingHistoryFromSettingsReconcilesRewardsWithoutLeavingTheScreen() {
        var data = readingSessionFixture(count = 25)
        repeat(25) { data = ReadingEngine.nextSessionActivity(readingSolve(data)) }
        ReadingStore(legacy).also { it.load(); it.save(data) }
        val session = requireNotNull(data.activeSession)
        val rewards = HistoryStore(File(application.filesDir, "practice_history.json"))
        rewards.addReadingResult(ReadingRewardResult(session.id, session.ownerId, session.game, session.level,
            requireNotNull(session.finishedAt), 25, 25, 25,
            prizeType = RewardType.LOLLIPOP, celebration = Celebration.PIKACHU))
        rewards.claimReadingReward(session.id)
        rewards.collectReadingCelebration(session.id, Celebration.PIKACHU)
        val reading = reading()
        val app = show()
        await { reading.data != null && !reading.saving && app.readingRewardResults.isNotEmpty() }
        compose.runOnIdle { app.openSubjectHistory(Subject.READING) }
        compose.onNodeWithTag("reading-delete-session-${session.id}").performScrollTo().performClick()
        compose.onNodeWithText("Delete").performClick()
        await { reading.data?.sessions?.isEmpty() == true }
        compose.onNodeWithText("No reading practice results yet for this reader.").assertIsDisplayed()
        await { app.readingRewardResults.isEmpty() }
        compose.onNodeWithTag("reading-practice-history-screen").assertIsDisplayed()
        assertEquals(RewardBalance(), app.rewardBalances[RewardType.LOLLIPOP])
        assertTrue(app.pokemons.isEmpty())
        assertEquals(setOf(session.id), store.load().deletedSessionIds)
        assertEquals(setOf(session.id), rewards.loadSnapshot().recordedReadingSessionIds)
    }

    @Test fun homeFinishesPersistedReadingRewardDeletionAfterRestart() {
        val rewards = HistoryStore(File(application.filesDir, "practice_history.json"))
        rewards.addReadingResult(ReadingRewardResult("deleted", "reader-a", ReadingGame.MISSING,
            ReadingLevel.SPROUT, 1, 25, 25, 25, prizeType = RewardType.LOLLIPOP))
        rewards.claimReadingReward("deleted")
        store.save(ReadingEngine.addProfile(store.load(), "Ari", "reader-a")
            .copy(deletedSessionIds = setOf("deleted")))
        val reading = reading()
        val app = show()
        await { reading.data != null && app.rewardBalances[RewardType.LOLLIPOP] == RewardBalance() }
        compose.onNodeWithTag("subject-grid").assertIsDisplayed()
        assertTrue(app.readingRewardResults.isEmpty())
        assertEquals(setOf("deleted"), rewards.loadSnapshot().recordedReadingSessionIds)
    }

    @Test fun earlyClickWaitsForCatalogueAndCancelDoesNotNavigateAfterLoading() {
        runBlocking { readingTestImporter(store.catalog, application.cacheDir, pool).importPack() }
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.submit { entered.countDown(); release.await(10, TimeUnit.SECONDS) }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        try {
            val reading = reading(dispatcher)
            val app = show()
            compose.onNodeWithTag("subject-Reading").performClick()
            compose.onNodeWithText("Checking reading activities").assertIsDisplayed()
            compose.onNodeWithText("Import reading activities first").assertDoesNotExist()
            compose.onNodeWithTag("reading-import-dismiss").performClick()
            release.countDown()
            await { reading.packStatus?.version != null && !reading.saving }
            compose.onNodeWithTag("subject-grid").assertIsDisplayed()
            assertNull(app.overlay)
            compose.onNodeWithTag("subject-Reading").performClick()
            compose.onNodeWithTag("reading-menu").assertIsDisplayed()
        } finally {
            release.countDown()
            dispatcher.close()
        }
    }

    @Test fun storageFailureShowsRetryRatherThanIncorrectImportAdvice() {
        legacy.writeText("{broken")
        val reading = reading()
        val app = show()
        await { reading.storageError != null }
        compose.onNodeWithTag("subject-Reading").performClick()
        compose.onNodeWithText("Reading storage unavailable").assertIsDisplayed()
        compose.onNodeWithTag("reading-import-retry").assertIsDisplayed()
        compose.onNodeWithText("Import reading activities first").assertDoesNotExist()
        assertNull(app.overlay)
        assertEquals("{broken", legacy.readText())
        legacy.delete()
        compose.onNodeWithTag("reading-import-retry").performClick()
        await { reading.storageError == null && reading.packStatus != null }
        compose.onNodeWithText("Import reading activities first").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun summitLevelCanBeSelectedAndSavedWithoutChangingAnExistingDraft() {
        val original = readingFixture()
        ReadingStore(legacy).also { it.load(); it.save(original) }
        val reading = reading()
        compose.setContent { MathTheme(textSizePercent = 200) { ReadingLevelDialog(reading, {}) } }
        await { reading.data != null && !reading.saving }
        compose.onNodeWithTag("reading-level-SUMMIT").performScrollTo().performClick()
        await { reading.data?.profile?.level == ReadingLevel.SUMMIT && !reading.saving }
        compose.onNodeWithTag("reading-level-SUMMIT").assertIsSelected().assertIsDisplayed()
        val saved = store.load()
        assertEquals(ReadingLevel.SUMMIT, saved.profile?.level)
        assertEquals(ReadingLevel.SPROUT, saved.active?.level)
        assertEquals(original.attempts, saved.withoutCatalogueRefs().attempts)
    }
}
