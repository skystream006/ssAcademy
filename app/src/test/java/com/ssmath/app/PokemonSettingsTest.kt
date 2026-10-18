package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class PokemonSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "practice_history.json")
    private val models = ViewModelStore()

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        file.delete()
        DebugLog.initialize(application)
    }

    @After fun cleanup() {
        compose.runOnIdle { models.clear() }
        file.delete()
    }

    private fun model(chosen: Celebration = Celebration.PIKACHU, questionCount: Int = 15): MathViewModel {
        val seed = (0..10_000).first { Celebration.select(questionCount, Random(it)) == chosen }
        return MathViewModel(application, ioDispatcher = Dispatchers.Main.immediate,
            celebrationRandom = Random(seed)).also { models.put("model", it) }
    }

    @Test fun emptyCollectionIsBelowRewardsWithSectionCountsAndReturnsToSettings() {
        val model = model()
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Rewards").performClick()
        val rewards = compose.onNodeWithTag("my-rewards").fetchSemanticsNode().boundsInRoot
        val pokemons = compose.onNodeWithTag("my-pokemons").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val history = compose.onNodeWithText("Result Histories").fetchSemanticsNode().boundsInRoot
        assertTrue(rewards.bottom <= pokemons.top)
        assertTrue(pokemons.bottom <= history.top)
        compose.onNodeWithTag("my-pokemons").performClick()
        assertEquals(Overlay.POKEMONS, model.overlay)
        compose.onNodeWithText("My Pokémons").assertIsDisplayed()
        compose.onNodeWithTag("pokemon-count").assertDoesNotExist()
        assertSectionCount(CelebrationCategory.POKEMONS, "0/157")
        assertSectionCount(CelebrationCategory.OTHER, "0/5")
        assertSectionCount(CelebrationCategory.MEGA_POKEMONS, "0/95")
        compose.onNodeWithText("Finish practices", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(Overlay.SETTINGS, model.overlay)
    }

    @Test fun collectedImageReplaysAndClosesWithoutChangingPracticeOrInventory() {
        val store = HistoryStore(file)
        repeat(2) { store.collectPokemon(Celebration.PALAFIN) }
        val before = store.loadSnapshot()
        val model = model()
        model.chooseRewardsEnabled(false)
        model.openPokemons()
        compose.mainClock.autoAdvance = false
        compose.setContent { MathAppContent(model) }
        compose.mainClock.advanceTimeBy(64)
        assertSectionCount(CelebrationCategory.POKEMONS, "1/157")
        assertSectionCount(CelebrationCategory.OTHER, "0/5")
        compose.onAllNodesWithTag("pokemon-PALAFIN").assertCountEquals(1)
        compose.onNodeWithTag("pokemon-PIKACHU").assertDoesNotExist()
        compose.onNodeWithTag("pokemon-PALAFIN").assert(hasText("#0964 Palafin")).performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-PALAFIN").assertIsDisplayed()
        compose.onNodeWithText("You answered every question!").assertDoesNotExist()
        compose.onNodeWithTag("close-celebration").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-PALAFIN").assertDoesNotExist()
        compose.onNodeWithTag("pokemon-PALAFIN").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-PALAFIN").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(CELEBRATION_DURATION_MS.toLong())
        compose.onNodeWithTag("celebration-PALAFIN").assertDoesNotExist()
        assertSectionCount(CelebrationCategory.POKEMONS, "1/157")
        assertSectionCount(CelebrationCategory.OTHER, "0/5")
        assertEquals(Overlay.POKEMONS, model.overlay)
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.celebration)
        assertNull(model.rewardResult)
        assertEquals(before, store.loadSnapshot())
    }

    @Test fun presentingAPokemonAddsItBeforeSkippingTheAnimation() =
        presentationCollects(Celebration.PIKACHU, questionCount = 15)

    @Test fun presentingANewPokemonAddsItBeforeSkippingTheAnimation() =
        presentationCollects(Celebration.MEW, questionCount = 15)

    @Test fun presentingAnOtherCelebrationAfterAShortPracticeCollectsIt() =
        presentationCollects(Celebration.PARTY, questionCount = 1)

    @Test fun matrixPresentationAddsAMegaToItsOwnSectionAndReplayDoesNotAwardItAgain() {
        val store = HistoryStore(file)
        store.collectPokemons(Celebration.standardCelebrations)
        val model = model()
        model.chooseRewardsEnabled(false)
        model.openMultiplicationMatrixGame()
        model.start()
        compose.mainClock.autoAdvance = false
        compose.setContent { MathAppContent(model) }
        compose.runOnIdle {
            repeat(121) {
                model.updateAnswer(model.game!!.problem.answer.toString())
                model.submitAnswer()
            }
        }
        compose.mainClock.advanceTimeBy(64)
        val mega = requireNotNull(model.celebration)
        assertEquals(CelebrationCategory.MEGA_POKEMONS, mega.category)
        compose.onNodeWithTag("celebration-${mega.name}").assertIsDisplayed()
        compose.onNodeWithText("Mega Pokémon").assertIsDisplayed()
        compose.waitUntil(5_000) { model.lastResult?.megaPokemonReward == mega }
        compose.onNodeWithTag("view-results").performClick()
        compose.mainClock.advanceTimeBy(64)
        val standard = requireNotNull(model.celebration)
        assertTrue(standard in Celebration.standardCelebrations)
        compose.onNodeWithTag("celebration-${standard.name}").assertIsDisplayed()
        compose.waitUntil(5_000) { model.lastResult?.pokemonReward == standard }
        compose.onNodeWithTag("view-results").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("My Pokémons: ${mega.collectionLabel}").assertIsDisplayed()
        compose.onNodeWithText("My Pokémons: ${standard.collectionLabel}").assertIsDisplayed()
        compose.runOnIdle {
            model.done()
            model.openHistory()
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("My Pokémons: ${mega.collectionLabel}").assertIsDisplayed()
        compose.onNodeWithText("My Pokémons: ${standard.collectionLabel}").assertIsDisplayed()
        compose.runOnIdle {
            model.closeOverlay()
            model.closeOverlay()
            model.openPokemons()
        }
        compose.mainClock.advanceTimeBy(64)
        assertSectionCount(CelebrationCategory.MEGA_POKEMONS, "1/95")
        val before = store.loadSnapshot()
        compose.onNodeWithTag("pokemon-gallery").performScrollToKey(mega.name)
        compose.onNodeWithTag("pokemon-${mega.name}").assert(hasText(mega.collectionLabel)).performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-${mega.name}").assertIsDisplayed()
        compose.onNodeWithTag("close-celebration").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        assertEquals(before, store.loadSnapshot())
    }

    private fun presentationCollects(celebration: Celebration, questionCount: Int) {
        val model = model(celebration, questionCount)
        model.chooseRewardsEnabled(false)
        model.openRewardsGame()
        model.updateRewardsQuestionCount(questionCount.toString())
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
        compose.mainClock.autoAdvance = false
        compose.setContent { MathAppContent(model) }
        compose.runOnIdle {
            repeat(questionCount) {
                model.updateAnswer(model.game!!.problem.answer.toString())
                model.submitAnswer()
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-${celebration.name}").assertIsDisplayed()
        assertEquals(setOf(celebration), model.pokemons)
        assertEquals(celebration, model.lastResult!!.pokemonReward)
        compose.onNodeWithTag("view-results").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-${celebration.name}").assertDoesNotExist()
        assertNull(model.celebration)
        assertEquals(Screen.RESULTS, model.screen)
        compose.onNodeWithText("My Pokémons: ${celebration.collectionLabel}").assertIsDisplayed()
        compose.mainClock.autoAdvance = true
        compose.runOnIdle {
            model.done()
            model.openHistory()
        }
        compose.onNodeWithText("My Pokémons: ${celebration.collectionLabel}").assertIsDisplayed().performClick()
        compose.onNodeWithText("My Pokémons: ${celebration.collectionLabel}").assertIsDisplayed()
        compose.runOnIdle {
            model.openSettings()
        }
        compose.onNodeWithText("Rewards").performScrollTo().performClick()
        compose.onNodeWithTag("my-pokemons").performClick()
        assertSectionCount(CelebrationCategory.POKEMONS,
            if (celebration.category == CelebrationCategory.POKEMONS) "1/157" else "0/157")
        assertSectionCount(CelebrationCategory.OTHER,
            if (celebration.category == CelebrationCategory.OTHER) "1/5" else "0/5")
        compose.onNodeWithText(celebration.category.label).assertIsDisplayed()
        compose.onNodeWithTag("pokemon-${celebration.name}").assert(hasText(celebration.collectionLabel)).assertIsDisplayed()
        assertEquals(setOf(celebration), HistoryStore(file).loadSnapshot().pokemons)
    }

    @Test fun historyDeletionAndClearRemovePokemonAndOtherRewardsFromTheCollection() {
        val store = HistoryStore(file)
        listOf(Celebration.PIKACHU, Celebration.PARTY).forEachIndexed { index, celebration ->
            val id = index + 1L
            store.add(PracticeResult(id, id, Operation.ADDITION, 10, 1000,
                List(15) { Attempt(Problem(1, 1, Operation.ADDITION), 2) }))
            store.collectPokemon(id, celebration)
        }
        val model = model()
        model.openHistory()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("My Pokémons: #0025 Pikachu").performClick()
        compose.onNodeWithText("My Pokémons: #0025 Pikachu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete result").performClick()
        compose.onNodeWithText("My Pokémons reward unless another saved result", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { assertEquals(setOf(Celebration.PARTY), model.pokemons) }
        compose.onNodeWithText("My Pokémons: #0025 Pikachu").assertDoesNotExist()
        compose.onNodeWithText("My Pokémons: ${Celebration.PARTY.collectionLabel}").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear history").performClick()
        compose.onNodeWithText("My Pokémons rewards", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Clear").performClick()
        compose.runOnIdle {
            assertTrue(model.history.isEmpty())
            assertTrue(model.pokemons.isEmpty())
            model.openPokemons()
        }
        assertSectionCount(CelebrationCategory.POKEMONS, "0/157")
        assertSectionCount(CelebrationCategory.OTHER, "0/5")
        assertTrue(HistoryStore(file).loadSnapshot().pokemons.isEmpty())
    }

    @Test fun otherCelebrationsHaveTheirOwnCategoryAndCanReplayWithoutChangingInventory() {
        val store = HistoryStore(file)
        store.collectPokemon(Celebration.PIKACHU)
        store.collectPokemon(Celebration.PARTY)
        val before = store.loadSnapshot()
        val model = model()
        model.openPokemons()
        compose.mainClock.autoAdvance = false
        compose.setContent { MathAppContent(model) }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("pokemon-count").assertDoesNotExist()
        assertSectionCount(CelebrationCategory.POKEMONS, "1/157")
        assertSectionCount(CelebrationCategory.OTHER, "1/5")
        compose.onNodeWithTag("pokemon-count-POKEMONS")
            .assertContentDescriptionEquals("1 of 157 collected in Pokémons")
        compose.onNodeWithTag("pokemon-count-OTHER")
            .assertContentDescriptionEquals("1 of 5 collected in Other")
        compose.onNodeWithText("Pokémons").assertIsDisplayed()
        compose.onNodeWithText("Other").assertIsDisplayed()
        compose.onNodeWithTag("pokemon-PARTY").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-PARTY").assertIsDisplayed()
        compose.onNodeWithText("You answered every question!").assertDoesNotExist()
        compose.onNodeWithTag("close-celebration").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-PARTY").assertDoesNotExist()
        assertEquals(before, store.loadSnapshot())
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.celebration)
        assertNull(model.rewardResult)
    }

    @Test fun gallerySortsPokemonsByNdexAndKeepsOtherCelebrationsInTheirOriginalOrder() {
        val expected = listOf(
            Celebration.BULBASAUR, Celebration.IVYSAUR, Celebration.VENUSAUR,
            Celebration.CHARMANDER, Celebration.CHARIZARD, Celebration.SQUIRTLE,
            Celebration.BLASTOISE, Celebration.PIKACHU, Celebration.NIDORAN_FEMALE,
            Celebration.NIDORAN_MALE, Celebration.JIGGLYPUFF, Celebration.TENTACRUEL_GEODUDE,
            Celebration.FARFETCHD, Celebration.MR_MIME, Celebration.MEWTWO, Celebration.MEW, Celebration.MANTINE,
            Celebration.WAILMER, Celebration.WAILORD, Celebration.MANTYKE,
            Celebration.BOUFFALANT, Celebration.FINIZEN, Celebration.PALAFIN, Celebration.VELUZA,
            Celebration.DOLPHINS, Celebration.WHALES, Celebration.ANCHOVIES,
            Celebration.PARTY, Celebration.CANDY_SHOWER
        )
        HistoryStore(file).collectPokemons(expected.reversed())
        val model = model()
        model.openPokemons()
        compose.setContent { MathAppContent(model) }
        expected.zipWithNext().forEach { (first, second) ->
            compose.onNodeWithTag("pokemon-gallery").performScrollToKey(first.name)
            val firstBounds = compose.onNodeWithTag("pokemon-${first.name}")
                .assert(hasText(first.collectionLabel)).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val secondBounds = compose.onNodeWithTag("pokemon-${second.name}")
                .assert(hasText(second.collectionLabel)).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("${first.label} should appear before ${second.label}",
                firstBounds.top < secondBounds.top ||
                    (firstBounds.top == secondBounds.top && firstBounds.right <= secondBounds.left))
        }
    }

    @Test fun sevenDescriptionTapsOpenDebugMenuAndOnlyTheButtonAddsAnimations() {
        val store = HistoryStore(file)
        store.collectPokemon(Celebration.SQUIRTLE)
        val before = store.loadSnapshot()
        val model = model()
        model.chooseRewardsEnabled(false)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Debug logging").performScrollTo().performClick()
        val description = compose.onNodeWithTag("debug-logging-description").performScrollTo()
        repeat(6) { description.performClick() }
        assertEquals(before.pokemons, model.pokemons)
        assertEquals(before, store.loadSnapshot())
        compose.onNodeWithText("Debug Menu").assertDoesNotExist()
        description.performClick()
        compose.onNodeWithText("Debug Menu").assertIsDisplayed()
        listOf("Game Settings", "My Rewards", "Add/Remove Rewards", "Available Rewards").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
        compose.onNodeWithText("Add all animations").assertDoesNotExist()
        compose.onNodeWithText("My Pokémons").performScrollTo().performClick()
        compose.onNodeWithText("Add all animations").assertIsDisplayed()
        assertEquals(before, store.loadSnapshot())
        compose.onNodeWithText("Close").performClick()
        assertEquals(before.pokemons, model.pokemons)
        repeat(7) { description.performClick() }
        compose.onNodeWithText("My Pokémons").performScrollTo().performClick()
        compose.onNodeWithText("Add all animations").performClick()
        val expected = before.copy(pokemons = Celebration.entries.toSet())
        assertEquals(expected.pokemons, model.pokemons)
        assertEquals(expected, store.loadSnapshot())
        compose.onNodeWithText("Add all animations").assertIsNotEnabled()
        compose.onNodeWithText("${Celebration.entries.size}/${Celebration.entries.size} animations collected").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        repeat(7) { description.performClick() }
        compose.onNodeWithText("My Pokémons").performScrollTo().performClick()
        compose.onNodeWithText("Add all animations").assertIsNotEnabled()
        assertEquals(expected, store.loadSnapshot())
        compose.onNodeWithText("Close").performClick()
        assertFalse(DebugLog.enabled.value)
        compose.onNodeWithContentDescription("Debug logging").performScrollTo().assertIsOff()
        compose.onNodeWithText("Rewards").performScrollTo().performClick()
        compose.onNodeWithTag("my-pokemons").performScrollTo().performClick()
        assertSectionCount(CelebrationCategory.POKEMONS, "157/157")
        assertSectionCount(CelebrationCategory.OTHER, "5/5")
        assertSectionCount(CelebrationCategory.MEGA_POKEMONS, "95/95")
    }

    @Test fun addAllAnimationsStaysEnabledWhenOnlyMegaPokemonsAreMissing() {
        val store = HistoryStore(file)
        store.collectPokemons(Celebration.standardCelebrations)
        val before = store.loadSnapshot()
        val model = model()
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Debug logging").performScrollTo().performClick()
        val description = compose.onNodeWithTag("debug-logging-description").performScrollTo()
        repeat(7) { description.performClick() }
        compose.onNodeWithText("My Pokémons").performScrollTo().performClick()
        compose.onNodeWithText("162/${Celebration.entries.size} animations collected").assertIsDisplayed()
        compose.onNodeWithText("Add all animations").assertIsEnabled().performClick().assertIsNotEnabled()
        assertEquals(before.copy(pokemons = Celebration.entries.toSet()), store.loadSnapshot())
        assertEquals(Celebration.entries.toSet(), model().pokemons)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun adminActionsAndEveryRewardAreReadableAndScrollableWithLargeText() {
        val model = model()
        model.chooseTextSize(200)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Debug logging").performScrollTo().performClick()
        val description = compose.onNodeWithTag("debug-logging-description").performScrollTo()
        repeat(7) { description.performClick() }
        assertTextFits("Debug Menu")
        compose.onNodeWithText("My Pokémons").performScrollTo().performClick()
        compose.onNodeWithText("Add all animations").performScrollTo()
        assertTextFits("Add all animations")
        compose.onNodeWithText("Close").performClick()
        compose.openAdminSettings()
        compose.onNodeWithText("Available Rewards").performScrollTo().performClick()
        RewardTier.entries.forEach { tier ->
            val heading = "${tier.label} · ${tier.questionCountLabel}"
            compose.onNodeWithText(heading).performScrollTo()
            assertTextFits(heading)
            RewardType.entries.filter { it.tier == tier }.forEach { type ->
                compose.onNodeWithText(type.label).performScrollTo()
                assertTextFits(type.label)
                compose.onNodeWithContentDescription("${type.label} reward").assertIsDisplayed().assertIsOn()
            }
        }
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        assertTrue(model.pokemons.isEmpty())
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun allCollectedNamesAreReadableAndScrollableWithLargeText() {
        file.writeText(kotlinx.serialization.json.Json { encodeDefaults = true }.encodeToString(PracticeSnapshot.serializer(),
            PracticeSnapshot(pokemons = Celebration.entries.toSet())))
        val model = model()
        model.chooseTextSize(200)
        model.openPokemons()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithTag("pokemon-count").assertDoesNotExist()
        assertTextFits("My Pokémons")
        CelebrationCategory.entries.forEach { category ->
            val count = Celebration.entries.count { it.category == category }
            val fraction = "$count/$count"
            assertSectionCount(category, fraction)
            assertTextFits(category.label)
            assertTextFits(fraction)
        }
        Celebration.entries.forEach { pokemon ->
            compose.onNodeWithTag("pokemon-gallery").performScrollToKey(pokemon.name)
            assertTextFits(pokemon.collectionLabel)
        }
    }

    private fun assertSectionCount(category: CelebrationCategory, fraction: String) {
        compose.onNodeWithTag("pokemon-gallery").performScrollToKey("category-${category.name}")
        val heading = compose.onNodeWithText(category.label).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val count = compose.onNodeWithTag("pokemon-count-${category.name}").assertTextEquals(fraction)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(heading.right <= count.left)
        assertEquals(heading.center.y, count.center.y, 1f)
    }

    private fun assertTextFits(text: String) {
        compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                action(layouts)
                val layout = layouts.single()
                assertFalse("$text overflows horizontally: ${layout.size.width} < ${layout.multiParagraph.width}",
                    layout.didOverflowWidth)
                assertFalse("$text overflows vertically: ${layout.size.height} < ${layout.multiParagraph.height}",
                    layout.didOverflowHeight)
            }
    }
}
