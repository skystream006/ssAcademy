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
        assertEquals(Celebration.entries.toSet(), repeated)
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
        repeat(3) { model.collectPresentedCelebration(scene) }
        assertEquals(standard + scene, model.pokemons)
        assertEquals(scene, model.lastResult!!.pokemonReward)
        assertEquals(scene, HistoryStore(file).load().single().pokemonReward)
        assertEquals(model.pokemons, model().pokemons)
        model.dismissCelebration()
        val bytes = file.readText()
        model.collectPresentedCelebration(scene)
        assertEquals(bytes, file.readText())
        model.deleteResult(model.lastResult!!.id)
        assertEquals(standard, model.pokemons)
        model.collectPresentedCelebration(scene)
        assertEquals(standard, HistoryStore(file).loadSnapshot().pokemons)
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

    @Test fun readingReceiptsAndAdminCannotGrantMegaButKeepAlreadyEarnedForms() {
        val store = HistoryStore(file)
        val receipt = ReadingRewardResult("session", "reader", ReadingGame.MISSING, ReadingLevel.SPROUT,
            1, 121, 121, 121, celebration = Celebration.MEGA_MEGANIUM)
        assertNull(store.addReadingResult(receipt).readingResults.single().celebration)
        val before = file.readText()
        store.collectReadingCelebration(receipt.sessionId, Celebration.MEGA_MEGANIUM)
        assertEquals(before, file.readText())
        val existing = PracticeSnapshot(pokemons = setOf(Celebration.MEGA_MEGANIUM),
            readingResults = listOf(receipt))
        file.writeText(Json { encodeDefaults = true }.encodeToString(PracticeSnapshot.serializer(), existing))
        assertEquals(existing, store.collectReadingCelebration(receipt.sessionId, Celebration.MEGA_MEGANIUM))
        assertEquals(existing, store.collectPokemon(Celebration.MEGA_EMBOAR))
        val model = model()
        model.collectAllCelebrations()
        assertEquals(standard + Celebration.MEGA_MEGANIUM, model.pokemons)
        assertEquals(model.pokemons, store.collectPokemons(Celebration.entries).pokemons)
    }
}
