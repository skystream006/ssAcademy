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
class PokemonCollectionTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "practice_history.json")
    private val blockedWrite get() = File(application.filesDir, "practice_history.json.tmp")
    private val models = mutableListOf<ViewModelStore>()
    private var now = 1_000L

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
        chosen: Celebration = Celebration.PIKACHU,
        dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ): MathViewModel {
        val seed = (0..10_000).first { Celebration.entries.random(Random(it)) == chosen }
        return MathViewModel(application, clock = { now }, ioDispatcher = dispatcher,
            celebrationRandom = Random(seed)).also {
            models += ViewModelStore().apply { put("model", it) }
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun start(model: MathViewModel, count: Int = 15) {
        if (model.screen != Screen.SETUP) model.done()
        model.openRewardsGame()
        model.updateRewardsQuestionCount(count.toString())
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
    }

    private fun answer(model: MathViewModel, correct: Boolean = true) {
        model.updateAnswer((model.game!!.problem.answer + if (correct) 0 else 1).toString())
        model.submitAnswer()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun originalPokemonsKeepExactLabelsAndAllStoredNamesAreStable() {
        assertEquals("Pokémons", CelebrationCategory.POKEMONS.label)
        assertEquals(listOf("Pikachu", "Squirtle", "Bulbasaur", "Charmander", "Jigglypuff",
            "Palafin", "Finizen", "Wailmer", "Wailord", "Bouffalant", "Veluza", "Mantyke", "Mantine"),
            Celebration.pokemons.take(13).map { it.label })
        assertEquals(162, Celebration.entries.size)
        assertEquals("Other", CelebrationCategory.OTHER.label)
        assertEquals(setOf(Celebration.DOLPHINS, Celebration.WHALES, Celebration.ANCHOVIES,
            Celebration.PARTY, Celebration.CANDY_SHOWER),
            Celebration.entries.filter { it.category == CelebrationCategory.OTHER }.toSet())
        Celebration.entries.forEach {
            assertEquals("\"${it.name}\"", Json.encodeToString(Celebration.serializer(), it))
            assertEquals(it, Json.decodeFromString(Celebration.serializer(), "\"${it.name}\""))
        }
    }

    @Test fun pokemonSelectionStartsAtFifteenQuestions() {
        (1..14).forEach { count ->
            val selected = (0..200).map { Celebration.select(count, Random(it)) }.toSet()
            assertEquals(Celebration.entries.filter { it.category == CelebrationCategory.OTHER }.toSet(), selected)
        }
        listOf(15, 16, 25, 1000).forEach { count ->
            assertEquals(Celebration.entries.toSet(),
                (0..10_000).map { Celebration.select(count, Random(it)) }.toSet())
        }
    }

    @Test fun selectionOnlyUsesUncollectedEligibleCelebrations() {
        val remaining = setOf(Celebration.PARTY, Celebration.PIKACHU, Celebration.MEW)
        val collected = Celebration.entries.toSet() - remaining
        listOf(1, 14, 15, 25, 1000).forEach { count ->
            val selected = (0..200).map { Celebration.select(count, Random(it), collected) }.toSet()
            assertEquals(if (count < 15) setOf(Celebration.PARTY) else remaining, selected)
        }
    }

    @Test fun lastMissingCelebrationIsAlwaysSelectedBeforeAnyRepeat() {
        Celebration.entries.forEach { missing ->
            val collected = Celebration.entries.toSet() - missing
            repeat(20) { seed ->
                assertEquals(missing, Celebration.select(15, Random(seed), collected))
            }
        }
    }

    @Test fun shortPracticesDoNotRepeatOtherScenesWhilePokemonCollectionIsIncomplete() {
        val collected = Celebration.entries.toSet() - Celebration.MEW
        (1..14).forEach { count ->
            assertNull(Celebration.select(count, Random(0), collected))
        }
    }

    @Test fun completeCollectionAllowsEveryEligibleCelebrationAgain() {
        val collected = Celebration.entries.toSet()
        listOf(1, 14, 15, 1000).forEach { count ->
            val expected = if (count < 15) collected.filter { it.category == CelebrationCategory.OTHER }.toSet()
                else collected
            assertEquals(expected, (0..10_000).map { Celebration.select(count, Random(it), collected) }.toSet())
        }
    }

    @Test fun originalPokemonsKeepNationalPokedexNumbersAndPaddedCollectionLabels() {
        val expected = mapOf(
            Celebration.PIKACHU to (25 to "#0025 Pikachu"),
            Celebration.SQUIRTLE to (7 to "#0007 Squirtle"),
            Celebration.BULBASAUR to (1 to "#0001 Bulbasaur"),
            Celebration.CHARMANDER to (4 to "#0004 Charmander"),
            Celebration.JIGGLYPUFF to (39 to "#0039 Jigglypuff"),
            Celebration.PALAFIN to (964 to "#0964 Palafin"),
            Celebration.FINIZEN to (963 to "#0963 Finizen"),
            Celebration.WAILMER to (320 to "#0320 Wailmer"),
            Celebration.WAILORD to (321 to "#0321 Wailord"),
            Celebration.BOUFFALANT to (626 to "#0626 Bouffalant"),
            Celebration.VELUZA to (976 to "#0976 Veluza"),
            Celebration.MANTYKE to (458 to "#0458 Mantyke"),
            Celebration.MANTINE to (226 to "#0226 Mantine")
        )
        assertEquals(Celebration.pokemons.take(13).toSet(), expected.keys)
        expected.forEach { (pokemon, values) ->
            assertEquals(pokemon.name, values.first, pokemon.ndex)
            assertEquals(values.second, pokemon.collectionLabel)
        }
    }

    @Test fun otherCelebrationsKeepTheirNamesWithoutNationalPokedexNumbers() {
        Celebration.entries.filter { it.category == CelebrationCategory.OTHER }.forEach {
            assertNull(it.ndex)
            assertEquals(it.label, it.collectionLabel)
        }
    }

    @Test fun shortPracticesOnlyPresentAndCollectOtherCelebrationsEvenAfterUnlockingEverything() {
        val model = model()
        model.collectAllCelebrations()
        listOf(1, 14).forEach { count ->
            start(model, count)
            repeat(count) { answer(model) }
            val presented = model.celebration!!
            assertEquals(CelebrationCategory.OTHER, presented.category)
            model.collectPresentedCelebration(presented)
            assertEquals(Celebration.entries.toSet(), model.pokemons)
            model.done()
        }
    }

    @Test fun eachPresentedCelebrationIsCollectedOnceEvenWithRewardsDisabled() {
        repeat(Celebration.entries.size) { index ->
            val model = model()
            model.chooseRewardsEnabled(false)
            start(model)
            repeat(15) { answer(model, correct = it != 0) }
            val pokemon = requireNotNull(model.celebration)
            assertFalse(pokemon in model.pokemons)
            assertNull(model.lastResult!!.pokemonReward)
            repeat(3) { model.collectPresentedCelebration(pokemon) }
            assertEquals(index + 1, model.pokemons.size)
            assertEquals(pokemon, model.lastResult!!.pokemonReward)
            assertEquals(pokemon, model.history.first().pokemonReward)
            assertEquals(model.history, HistoryStore(file).load())
            model.dismissCelebration()
            model.done()
            assertEquals(model.pokemons, model().pokemons)
            assertTrue(model.rewardBalances.isEmpty())
        }
        assertEquals(Celebration.entries.toSet(), HistoryStore(file).loadSnapshot().pokemons)
    }

    @Test fun consecutivePracticesCollectEverySceneBeforeAllowingRepeats() {
        val model = model()
        repeat(Celebration.entries.size) { index ->
            start(model)
            repeat(15) { answer(model) }
            val presented = requireNotNull(model.celebration)
            assertFalse(presented in model.pokemons)
            model.collectPresentedCelebration(presented)
            assertEquals(index + 1, model.pokemons.size)
            model.done()
        }
        assertEquals(Celebration.entries.toSet(), model.pokemons)
        start(model)
        repeat(15) { answer(model) }
        val repeated = requireNotNull(model.celebration)
        assertTrue(repeated in model.pokemons)
        model.collectPresentedCelebration(repeated)
        assertEquals(Celebration.entries.toSet(), model.pokemons)
        assertEquals(repeated, model.lastResult!!.pokemonReward)
        assertEquals(repeated, HistoryStore(file).load().first().pokemonReward)
    }

    @Test fun exhaustedShortPracticesShowResultsUntilTheFullCollectionIsComplete() {
        val model = model()
        repeat(5) {
            start(model, count = 1)
            answer(model)
            val presented = requireNotNull(model.celebration)
            assertEquals(CelebrationCategory.OTHER, presented.category)
            assertFalse(presented in model.pokemons)
            model.collectPresentedCelebration(presented)
            model.done()
        }
        listOf(1, 14).forEach { count ->
            start(model, count)
            repeat(count) { answer(model) }
            assertNull(model.celebration)
            assertNull(model.earlyFinishMessage)
            assertEquals(Screen.RESULTS, model.screen)
            assertEquals(count, model.lastResult!!.attempts.size)
            assertEquals(5, model.pokemons.size)
            model.done()
        }
        start(model)
        repeat(15) { answer(model) }
        assertEquals(CelebrationCategory.POKEMONS, model.celebration!!.category)
    }

    @Test fun restoredCollectionStillPreventsRepeatsAfterHistoryIsCleared() {
        val collected = Celebration.entries.toSet() - Celebration.MEW
        HistoryStore(file).collectPokemons(collected)
        val model = model()
        start(model, count = 1)
        answer(model)
        assertNull(model.celebration)
        model.done()
        model.clearHistory()
        val restored = model()
        assertTrue(restored.history.isEmpty())
        assertEquals(collected, restored.pokemons)
        start(restored, count = 25)
        repeat(25) { answer(restored) }
        assertEquals(Celebration.MEW, restored.celebration)
        restored.collectPresentedCelebration(Celebration.MEW)
        restored.dismissCelebration()
        assertTrue(restored.rewardDialogVisible)
        restored.claimReward()
        assertEquals(1, restored.rewardBalances.values.sumOf { it.totalFragments })
        assertEquals(Celebration.entries.toSet(), model().pokemons)
    }

    @Test fun selectionWaitsForThePersistedCollectionToLoad() {
        val collected = Celebration.entries.toSet() - Celebration.MEW
        HistoryStore(file).collectPokemons(collected)
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        start(model, count = 1)
        answer(model)
        assertTrue(model.selectingCelebration)
        assertNull(model.celebration)
        dispatcher.drain()
        assertFalse(model.selectingCelebration)
        assertNull(model.celebration)
        assertEquals(collected, model.pokemons)
        assertEquals(Screen.RESULTS, model.screen)
        assertEquals(1, model.history.size)
    }

    @Test fun pendingCollectionIsSavedBeforeTheNextCelebrationIsSelected() {
        val collected = Celebration.entries.toSet() - setOf(Celebration.PARTY, Celebration.MEW)
        HistoryStore(file).collectPokemons(collected)
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        dispatcher.drain()
        start(model, count = 1)
        answer(model)
        assertEquals(Celebration.PARTY, model.celebration)
        model.collectPresentedCelebration(Celebration.PARTY)
        model.done()
        start(model, count = 25)
        repeat(25) { answer(model) }
        assertTrue(model.selectingCelebration)
        assertNull(model.celebration)
        assertFalse(model.rewardDialogVisible)
        dispatcher.drain()
        assertFalse(model.selectingCelebration)
        assertEquals(Celebration.MEW, model.celebration)
        assertFalse(model.rewardDialogVisible)
        model.dismissCelebration()
        assertTrue(model.rewardDialogVisible)
    }

    @Test fun leavingResultsBeforeCollectionLoadsDoesNotShowALateCelebration() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        start(model)
        repeat(15) { answer(model) }
        assertTrue(model.selectingCelebration)
        model.done()
        dispatcher.drain()
        assertFalse(model.selectingCelebration)
        assertNull(model.celebration)
        assertEquals(Screen.SETUP, model.screen)
        assertEquals(1, model.history.size)
    }

    @Test fun dismissingBeforeSelectionFinishesStillAllowsThePrizeWithoutALateAnimation() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        start(model, count = 25)
        repeat(25) { answer(model) }
        assertTrue(model.selectingCelebration)
        assertFalse(model.rewardDialogVisible)
        model.dismissCelebration()
        assertFalse(model.selectingCelebration)
        assertTrue(model.rewardDialogVisible)
        model.claimReward()
        dispatcher.drain()
        assertNull(model.celebration)
        assertTrue(model.pokemons.isEmpty())
        assertEquals(1, model.rewardBalances.values.sumOf { it.totalFragments })
        assertNull(model.lastResult!!.pokemonReward)
    }

    @Test fun unreadableCollectionDoesNotAllowPotentialDuplicates() {
        file.writeText("{")
        val model = model()
        start(model)
        repeat(15) { answer(model) }
        assertFalse(model.selectingCelebration)
        assertNull(model.celebration)
        assertNotNull(model.message)
        assertEquals(Screen.RESULTS, model.screen)
        assertEquals("{", file.readText())
    }

    @Test fun hiddenOrMismatchedCelebrationsAndEarlyFinishesDoNotCollect() {
        val model = model()
        start(model)
        repeat(5) { answer(model, correct = false) }
        assertNull(model.celebration)
        model.collectPresentedCelebration(Celebration.PIKACHU)
        assertTrue(model.pokemons.isEmpty())
        model.chooseShowTimer(true)
        model.chooseTimeLimitMinutes(5)
        start(model)
        now += 300_000L
        assertTrue(model.checkTimeLimit())
        model.collectPresentedCelebration(Celebration.PIKACHU)
        assertTrue(model.pokemons.isEmpty())
        start(model)
        repeat(15) { answer(model) }
        assertEquals(Celebration.PIKACHU, model.celebration)
        model.collectPresentedCelebration(Celebration.PARTY)
        assertTrue(model.pokemons.isEmpty())
        model.openPokemons()
        model.collectPresentedCelebration(Celebration.PIKACHU)
        assertTrue(model.pokemons.isEmpty())
    }

    @Test fun galleryPausesPracticeAndReturnsThroughSettings() {
        val model = model()
        start(model, count = 10)
        now += 1_000
        model.openSettings()
        model.openPokemons()
        now += 5_000
        assertEquals(1_000L, model.elapsedMs())
        model.updateAnswer("123")
        assertEquals("", model.answerText)
        model.closeOverlay()
        assertEquals(Overlay.SETTINGS, model.overlay)
        now += 5_000
        assertEquals(1_000L, model.elapsedMs())
        model.closeOverlay()
        now += 1_000
        assertEquals(2_000L, model.elapsedMs())
    }

    @Test fun lateCollectionSerializesWithClaimAndHistoryClearAfterDismissal() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        dispatcher.drain()
        start(model, count = 25)
        repeat(25) { answer(model) }
        repeat(3) { model.collectPresentedCelebration(Celebration.PIKACHU) }
        model.dismissCelebration()
        model.claimReward()
        model.clearHistory()
        dispatcher.drain()
        assertNull(model.celebration)
        assertTrue(model.pokemons.isEmpty())
        assertEquals(0, model.rewardBalances.values.sumOf { it.totalFragments })
        assertTrue(model.history.isEmpty())
        assertNull(model.lastResult!!.pokemonReward)
        val restored = model()
        assertEquals(model.pokemons, restored.pokemons)
        assertEquals(model.rewardBalances, restored.rewardBalances)
    }

    @Test fun failedCollectionIsNotReportedAsCollectedAndCanBeRetried() {
        val model = model()
        start(model)
        repeat(15) { answer(model) }
        val before = file.readText()
        assertTrue(blockedWrite.mkdir())
        model.collectPresentedCelebration(Celebration.PIKACHU)
        assertTrue(model.pokemons.isEmpty())
        assertNull(model.lastResult!!.pokemonReward)
        assertNotNull(model.message)
        assertEquals(before, file.readText())
        assertTrue(blockedWrite.delete())
        model.collectPresentedCelebration(Celebration.PIKACHU)
        assertEquals(setOf(Celebration.PIKACHU), model().pokemons)
        assertEquals(Celebration.PIKACHU, model.lastResult!!.pokemonReward)
        assertEquals(model.lastResult, HistoryStore(file).load().single())
    }

    @Test fun bulkCollectionSerializesWithPendingResultsClaimsAndHistoryClear() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        dispatcher.drain()
        start(model, count = 25)
        repeat(25) { answer(model) }
        model.collectPresentedCelebration(Celebration.PIKACHU)
        model.dismissCelebration()
        model.claimReward()
        repeat(2) { model.collectAllCelebrations() }
        model.clearHistory()
        dispatcher.drain()
        assertEquals(Celebration.entries.toSet() - Celebration.PIKACHU, model.pokemons)
        assertEquals(0, model.rewardBalances.values.sumOf { it.totalFragments })
        assertTrue(model.history.isEmpty())
        assertEquals(model.pokemons, model().pokemons)
    }

    @Test fun deletingOrClearingTheLiveResultRemovesItsPokemonAndPreventsRecollection() {
        listOf(false, true).forEach { clear ->
            val model = model()
            model.chooseRewardsEnabled(false)
            start(model)
            repeat(15) { answer(model) }
            val presented = requireNotNull(model.celebration)
            model.collectPresentedCelebration(presented)
            assertEquals(presented, model.lastResult!!.pokemonReward)
            if (clear) model.clearHistory() else model.deleteResult(model.lastResult!!.id)
            assertTrue(model.history.isEmpty())
            assertTrue(model.pokemons.isEmpty())
            assertNull(model.lastResult!!.pokemonReward)
            assertNull(model.celebration)
            model.collectPresentedCelebration(presented)
            assertTrue(HistoryStore(file).loadSnapshot().pokemons.isEmpty())
            model.done()
        }
    }

    @Test fun queuedCollectionUpdatesItsOwnHistoryDetailWithoutChangingTheNextPractice() {
        val dispatcher = QueuedDispatcher()
        val model = model(dispatcher = dispatcher)
        dispatcher.drain()
        start(model)
        repeat(15) { answer(model) }
        val presented = requireNotNull(model.celebration)
        model.collectPresentedCelebration(presented)
        model.done()
        dispatcher.drain()
        val previous = model.history.single()
        assertEquals(presented, previous.pokemonReward)
        assertNull(model.lastResult)
        start(model)
        repeat(15) { answer(model) }
        val next = requireNotNull(model.celebration)
        model.collectPresentedCelebration(next)
        model.dismissCelebration()
        model.openHistory()
        model.showHistoryDetail(model.lastResult)
        dispatcher.drain()
        assertEquals(next, model.historyDetail!!.pokemonReward)
        assertEquals(next, model.lastResult!!.pokemonReward)
        assertEquals(presented, model.history.last().pokemonReward)
    }

    @Test fun failedBulkCollectionPreservesExistingEntriesAndCanBeRetried() {
        HistoryStore(file).collectPokemon(Celebration.PARTY)
        val model = model()
        val before = file.readText()
        assertTrue(blockedWrite.mkdir())
        model.collectAllCelebrations()
        assertEquals(setOf(Celebration.PARTY), model.pokemons)
        assertNotNull(model.message)
        assertEquals(before, file.readText())
        assertTrue(blockedWrite.delete())
        model.collectAllCelebrations()
        assertEquals(Celebration.entries.toSet(), model().pokemons)
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
