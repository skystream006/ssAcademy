package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.random.Random
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
class MegaRewardsTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "practice_history.json")
    private val models = mutableListOf<ViewModelStore>()
    private val standard get() = Celebration.standardCelebrations.toSet()
    private val json = Json { encodeDefaults = true }
    private var now = 1_000L

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        file.delete()
        DebugLog.initialize(application)
    }

    @After fun cleanup() {
        models.forEach { it.clear() }
        shadowOf(Looper.getMainLooper()).idle()
        file.delete()
    }

    private fun model(): MathViewModel = MathViewModel(application,
        clock = { now }, ioDispatcher = Dispatchers.Unconfined, celebrationRandom = Random(7)).also {
        models += ViewModelStore().apply { put("model", it) }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun answer(model: MathViewModel, correct: Boolean = true) {
        model.updateAnswer((model.game!!.problem.answer + if (correct) 0 else 1).toString())
        model.submitAnswer()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun everyMegaIsReachableOnlyFromTheMatrixPool() {
        val matrixSelections = (0..10_000).map {
            Celebration.select(121, Random(it), standard, isMultiplicationMatrix = true)
        }.toSet()
        assertEquals(Celebration.megaPokemons.toSet(), matrixSelections)
        listOf(1, 14, 15, 25, 50, 121, 1000).forEach { count ->
            repeat(100) { seed ->
                assertNotEquals(CelebrationCategory.MEGA_POKEMONS,
                    Celebration.select(count, Random(seed), standard)?.category)
                assertEquals(CelebrationCategory.OTHER,
                    Celebration.select(count, Random(seed), standard, GameMode.PRACTICE,
                        isMultiplicationMatrix = true)?.category)
                if (count != 121) {
                    assertNotEquals(CelebrationCategory.MEGA_POKEMONS,
                        Celebration.select(count, Random(seed), standard, isMultiplicationMatrix = true)?.category)
                }
            }
        }
    }

    @Test fun incompleteMegaCollectionDoesNotBlockStandardRepeatsAndMatrixPrefersMissingForms() {
        repeat(100) { seed ->
            assertTrue(Celebration.select(121, Random(seed), standard) in standard)
            assertEquals(CelebrationCategory.OTHER, Celebration.select(10, Random(seed), standard)?.category)
        }
        Celebration.megaPokemons.forEach { missing ->
            assertEquals(missing, Celebration.select(121, Random(0),
                Celebration.entries.toSet() - missing, isMultiplicationMatrix = true))
        }
        val repeated = (0..10_000).map {
            Celebration.select(121, Random(it), Celebration.entries.toSet(), isMultiplicationMatrix = true)
        }.toSet()
        assertEquals(Celebration.megaPokemons.toSet(), repeated)
    }

    @Test fun completedMatrixCollectsOnlyOnLivePresentationAndSurvivesReload() {
        HistoryStore(file).collectPokemons(standard)
        val model = model()
        model.openMultiplicationMatrixGame()
        model.start()
        repeat(121) { answer(model) }
        val scene = requireNotNull(model.celebration)
        assertEquals(CelebrationCategory.MEGA_POKEMONS, scene.category)
        assertTrue(model.lastResult!!.isMultiplicationMatrix)
        assertEquals(standard, model.pokemons)
        assertNull(model.lastResult!!.pokemonReward)
        assertNull(model.lastResult!!.megaPokemonReward)
        assertEquals(RewardTier.TIER_2, model.lastResult!!.prizeType!!.tier)
        assertFalse(model.rewardDialogVisible)
        repeat(3) { model.collectPresentedCelebration(scene) }
        assertEquals(standard + scene, model.pokemons)
        assertEquals(scene, model.lastResult!!.megaPokemonReward)
        assertEquals(scene, HistoryStore(file).load().single().megaPokemonReward)
        assertEquals(model.pokemons, model().pokemons)
        model.dismissCelebration()
        val next = requireNotNull(model.celebration)
        assertTrue(next in standard)
        assertFalse(model.rewardDialogVisible)
        repeat(3) { model.collectPresentedCelebration(next) }
        assertEquals(next, model.lastResult!!.pokemonReward)
        assertEquals(setOf(scene, next), model.lastResult!!.pokemonRewards)
        assertEquals(model.lastResult, HistoryStore(file).load().single())
        model.dismissCelebration()
        assertNull(model.celebration)
        assertTrue(model.rewardDialogVisible)
        model.claimReward()
        shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(model.lastResult!!.prize)
        val bytes = file.readText()
        model.collectPresentedCelebration(scene)
        assertEquals(bytes, file.readText())
        model.deleteResult(model.lastResult!!.id)
        assertEquals(standard - next, model.pokemons)
        model.collectPresentedCelebration(scene)
        assertEquals(standard - next, HistoryStore(file).loadSnapshot().pokemons)
        assertTrue(model.lastResult!!.pokemonRewards.isEmpty())
    }

    @Test fun ordinaryMultiplicationWith121QuestionsCannotEarnMega() {
        HistoryStore(file).collectPokemons(standard)
        val model = model()
        model.openRewardsGame()
        model.updateRewardsQuestionCount("121")
        model.selectRewardsGame(Operation.MULTIPLICATION)
        model.start()
        repeat(121) { answer(model) }
        assertFalse(model.lastResult!!.isMultiplicationMatrix)
        assertTrue(model.celebration in standard)
        model.collectPresentedCelebration(Celebration.MEGA_MEGANIUM)
        assertEquals(standard, model.pokemons)
    }

    @Test fun matrixAlwaysSelectsBothPoolsIndependentlyAndDoesNotRequirePrizes() {
        listOf(emptySet(), standard, Celebration.megaPokemons.toSet(), Celebration.entries.toSet()).forEach { collected ->
            file.writeText(json.encodeToString(PracticeSnapshot.serializer(), PracticeSnapshot(pokemons = collected)))
            val model = model()
            model.chooseRewardsEnabled(false)
            model.openMultiplicationMatrixGame()
            model.start()
            repeat(121) { answer(model) }
            val mega = requireNotNull(model.celebration)
            assertEquals(CelebrationCategory.MEGA_POKEMONS, mega.category)
            model.collectPresentedCelebration(mega)
            model.dismissCelebration()
            val normal = requireNotNull(model.celebration)
            assertTrue(normal in standard)
            model.collectPresentedCelebration(normal)
            model.dismissCelebration()
            assertNull(model.celebration)
            assertNull(model.lastResult!!.prizeType)
            assertEquals(collected + mega + normal, model.pokemons)
            assertEquals(setOf(mega, normal), HistoryStore(file).load().single().pokemonRewards)
            model.done()
        }
    }

    @Test fun leavingOrDeletingResultsDropsTheQueuedCelebrationWithoutCollectingIt() {
        val model = model()
        repeat(3) { action ->
            model.openMultiplicationMatrixGame()
            model.start()
            repeat(121) { answer(model) }
            val mega = requireNotNull(model.celebration)
            model.collectPresentedCelebration(mega)
            when (action) {
                0 -> model.done()
                1 -> model.deleteResult(model.lastResult!!.id)
                else -> model.clearHistory()
            }
            shadowOf(Looper.getMainLooper()).idle()
            model.dismissCelebration()
            assertNull(model.celebration)
            assertFalse(model.pokemons.any { it in standard })
            model.done()
        }
    }

    @Test fun matrixClaimsKeepBothSlotsAndLegacyMegaReceiptsWithoutDuplicateAwards() {
        val attempts = List(121) {
            val problem = MultiplicationMatrix.problemAt(it)
            Attempt(problem, problem.answer)
        }
        val result = PracticeResult(1, 1, Operation.MULTIPLICATION, 11, 1000, attempts,
            questionCount = 121, isMultiplicationMatrix = true)
        val store = HistoryStore(file)
        store.add(result)
        store.collectPokemon(1, Celebration.MEGA_GARCHOMP_Z)
        store.collectPokemon(1, Celebration.PIKACHU)
        val both = store.loadSnapshot()
        assertEquals(setOf(Celebration.MEGA_GARCHOMP_Z, Celebration.PIKACHU), both.history.single().pokemonRewards)
        assertEquals(both, store.collectPokemon(1, Celebration.MEGA_MEGANIUM))
        assertEquals(both, store.collectPokemon(1, Celebration.PARTY))
        store.add(result)
        assertEquals(both, store.loadSnapshot())
        store.add(result.copy(id = 2))
        store.collectPokemon(2, Celebration.MEGA_GARCHOMP_Z)
        store.collectPokemon(2, Celebration.PARTY)
        store.delete(1)
        assertEquals(setOf(Celebration.MEGA_GARCHOMP_Z, Celebration.PARTY), store.loadSnapshot().pokemons)
        store.clear()
        assertTrue(store.loadSnapshot().pokemons.isEmpty())

        // Older matrix receipts stored the Mega in the original single reward field.
        file.writeText(json.encodeToString(PracticeSnapshot.serializer(), PracticeSnapshot(
            history = listOf(result.copy(pokemonReward = Celebration.MEGA_GARCHOMP_Z)),
            pokemons = setOf(Celebration.MEGA_GARCHOMP_Z))).replace(",\"megaPokemonReward\":null", ""))
        assertEquals(setOf(Celebration.MEGA_GARCHOMP_Z), store.load().single().pokemonRewards)
        store.collectPokemon(1, Celebration.PARTY)
        assertEquals(Celebration.MEGA_GARCHOMP_Z, store.load().single().megaPokemonReward)
        assertEquals(Celebration.PARTY, store.load().single().pokemonReward)
        store.clear()
        assertTrue(store.loadSnapshot().pokemons.isEmpty())
    }

    @Test fun failedTimedOutAndAbandonedMatricesCannotCollectMega() {
        val model = model()
        model.openMultiplicationMatrixGame()
        model.start()
        repeat(13) { answer(model, correct = false) }
        assertNull(model.celebration)
        assertTrue(model.pokemons.isEmpty())
        model.done()
        model.chooseShowTimer(true)
        model.chooseTimeLimitSeconds(30)
        model.openMultiplicationMatrixGame()
        model.start()
        now += 726_000
        answer(model)
        assertTrue(model.lastResult!!.timedOut)
        assertNull(model.celebration)
        model.done()
        model.openMultiplicationMatrixGame()
        model.start()
        answer(model)
        model.backToSetup()
        model.collectPresentedCelebration(Celebration.MEGA_MEGANIUM)
        assertTrue(model.pokemons.isEmpty())
        assertEquals(2, model.history.size)
    }

    @Test fun persistenceRejectsNonMatrixIncompleteTimedOutAndPracticeMegaClaims() {
        val attempts = List(121) { index ->
            val problem = MultiplicationMatrix.problemAt(index)
            Attempt(problem, problem.answer)
        }
        val completed = PracticeResult(1, 1, Operation.MULTIPLICATION, 11, 1000, attempts,
            minimum = 1, questionCount = 121, isMultiplicationMatrix = true)
        val store = HistoryStore(file)
        listOf(
            completed.copy(isMultiplicationMatrix = false),
            completed.copy(attempts = attempts.dropLast(1)),
            completed.copy(timedOut = true),
            completed.copy(gameMode = GameMode.PRACTICE),
            completed.copy(questionCount = 120, attempts = attempts.dropLast(1))
        ).forEach { invalid ->
            file.delete()
            store.add(invalid)
            val before = file.readText()
            store.collectPokemon(invalid.id, Celebration.MEGA_MEGANIUM)
            assertEquals(before, file.readText())
        }
        file.delete()
        store.add(completed)
        assertEquals(setOf(Celebration.MEGA_MEGANIUM),
            store.collectPokemon(completed.id, Celebration.MEGA_MEGANIUM).pokemons)
    }

    @Test fun readingReceiptsCannotGrantMegaButAdminAddsAllWithoutChangingHistoryOrRewards() {
        val store = HistoryStore(file)
        val receipt = ReadingRewardResult("session", "reader", ReadingGame.MISSING, ReadingLevel.SPROUT,
            1, 121, 121, 121, celebration = Celebration.MEGA_MEGANIUM)
        assertNull(store.addReadingResult(receipt).readingResults.single().celebration)
        val before = file.readText()
        store.collectReadingCelebration(receipt.sessionId, Celebration.MEGA_MEGANIUM)
        assertEquals(before, file.readText())
        val existing = PracticeSnapshot(pokemons = setOf(Celebration.MEGA_MEGANIUM),
            readingResults = listOf(receipt))
        file.writeText(json.encodeToString(PracticeSnapshot.serializer(), existing))
        assertEquals(existing, store.collectReadingCelebration(receipt.sessionId, Celebration.MEGA_MEGANIUM))
        assertEquals(existing, store.collectPokemon(Celebration.MEGA_EMBOAR))
        val model = model()
        repeat(2) { model.collectAllCelebrations() }
        assertEquals(Celebration.entries.toSet(), model.pokemons)
        assertEquals(existing.copy(pokemons = Celebration.entries.toSet()), store.loadSnapshot())
        assertEquals(model.pokemons, model().pokemons)
    }
}
