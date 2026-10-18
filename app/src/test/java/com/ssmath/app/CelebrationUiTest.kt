package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class CelebrationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun dolphinsPlayAndFinish() = playsAndFinishes(Celebration.DOLPHINS)
    @Test fun whalesPlayAndFinish() = playsAndFinishes(Celebration.WHALES)
    @Test fun anchoviesPlayAndFinish() = playsAndFinishes(Celebration.ANCHOVIES)
    @Test fun partyPlaysAndFinishes() = playsAndFinishes(Celebration.PARTY)
    @Test fun candyShowerPlaysAndFinishes() = playsAndFinishes(Celebration.CANDY_SHOWER)
    @Test fun pikachuPlaysAndFinishes() = playsAndFinishes(Celebration.PIKACHU)
    @Test fun squirtlePlaysAndFinishes() = playsAndFinishes(Celebration.SQUIRTLE)
    @Test fun bulbasaurPlaysAndFinishes() = playsAndFinishes(Celebration.BULBASAUR)
    @Test fun charmanderPlaysAndFinishes() = playsAndFinishes(Celebration.CHARMANDER)
    @Test fun jigglypuffPlaysAndFinishes() = playsAndFinishes(Celebration.JIGGLYPUFF)
    @Test fun palafinPlaysAndFinishes() = playsAndFinishes(Celebration.PALAFIN)
    @Test fun finizenPlaysAndFinishes() = playsAndFinishes(Celebration.FINIZEN)
    @Test fun wailmerPlaysAndFinishes() = playsAndFinishes(Celebration.WAILMER)
    @Test fun wailordPlaysAndFinishes() = playsAndFinishes(Celebration.WAILORD)
    @Test fun bouffalantPlaysAndFinishes() = playsAndFinishes(Celebration.BOUFFALANT)
    @Test fun veluzaPlaysAndFinishes() = playsAndFinishes(Celebration.VELUZA)
    @Test fun mantykePlaysAndFinishes() = playsAndFinishes(Celebration.MANTYKE)
    @Test fun mantinePlaysAndFinishes() = playsAndFinishes(Celebration.MANTINE)
    @Test fun ivysaurPlaysAndFinishes() = playsAndFinishes(Celebration.IVYSAUR)
    @Test fun tentacruelGeodudePlaysAndFinishes() = playsAndFinishes(Celebration.TENTACRUEL_GEODUDE)
    @Test fun mewPlaysAndFinishes() = playsAndFinishes(Celebration.MEW)
    @Test fun megaMeganiumPlaysAndFinishes() = playsAndFinishes(Celebration.MEGA_MEGANIUM)
    @Test fun megaCharizardYCanBeSkipped() = skipsAndStaysFinished(Celebration.MEGA_CHARIZARD_Y)

    @Test fun everyCelebrationCanReplayWithItsLabelAndCloseOrFinish() {
        var selected by mutableStateOf<Celebration?>(null)
        var completions = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MathTheme {
                Text("Collection")
                selected?.let { celebration ->
                    CelebrationDialog(celebration, replay = true) {
                        completions++
                        selected = null
                    }
                }
            }
        }
        Celebration.entries.forEachIndexed { index, celebration ->
            compose.runOnIdle { selected = celebration }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithText(celebration.label).assertIsDisplayed()
            compose.onNodeWithContentDescription(celebration.description).assertIsDisplayed()
            compose.onNodeWithText("Hurray!!").assertIsDisplayed()
            compose.onNodeWithText("Congratulations!").assertDoesNotExist()
            compose.onNodeWithTag("close-celebration").assertIsDisplayed()
            if (celebration.category == CelebrationCategory.MEGA_POKEMONS) {
                compose.onNodeWithText("Mega Pokémon").assertIsDisplayed()
                compose.onNodeWithText(celebration.collectionLabel).assertIsDisplayed()
            }
            if (index % 2 == 0) compose.onNodeWithTag("close-celebration").performClick()
            compose.mainClock.advanceTimeBy(CELEBRATION_DURATION_MS.toLong())
            compose.onNodeWithTag("celebration-${celebration.name}").assertDoesNotExist()
            compose.onNodeWithText(celebration.label).assertDoesNotExist()
            compose.runOnIdle { assertEquals(index + 1, completions) }
        }
    }

    private fun playsAndFinishes(celebration: Celebration) {
        var finished by mutableStateOf(false)
        var completions = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MathTheme {
                if (finished) Text("Results")
                else CelebrationDialog(celebration) { completions++; finished = true }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Congratulations!").assertIsDisplayed()
        compose.onNodeWithText("You answered every question!").assertIsDisplayed()
        compose.onNodeWithText(celebration.label).assertIsDisplayed()
        compose.onNodeWithContentDescription(celebration.description).assertIsDisplayed()
        compose.onNodeWithText("Hurray!!").assertIsDisplayed()
        compose.onNodeWithTag("view-results").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(CELEBRATION_DURATION_MS / 2L)
        compose.onNodeWithText(celebration.label).assertIsDisplayed()
        compose.runOnIdle { assertFalse(finished) }
        compose.mainClock.advanceTimeBy(CELEBRATION_DURATION_MS.toLong())
        compose.onNodeWithText(celebration.label).assertDoesNotExist()
        compose.onNodeWithText("Results").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test fun celebrationCanBeSkippedImmediately() = skipsAndStaysFinished(Celebration.PARTY)
    @Test fun candyShowerCanBeSkipped() = skipsAndStaysFinished(Celebration.CANDY_SHOWER)
    @Test fun pikachuCanBeSkipped() = skipsAndStaysFinished(Celebration.PIKACHU)
    @Test fun squirtleCanBeSkipped() = skipsAndStaysFinished(Celebration.SQUIRTLE)
    @Test fun bulbasaurCanBeSkipped() = skipsAndStaysFinished(Celebration.BULBASAUR)
    @Test fun charmanderCanBeSkipped() = skipsAndStaysFinished(Celebration.CHARMANDER)
    @Test fun jigglypuffCanBeSkipped() = skipsAndStaysFinished(Celebration.JIGGLYPUFF)
    @Test fun palafinCanBeSkipped() = skipsAndStaysFinished(Celebration.PALAFIN)
    @Test fun finizenCanBeSkipped() = skipsAndStaysFinished(Celebration.FINIZEN)
    @Test fun wailmerCanBeSkipped() = skipsAndStaysFinished(Celebration.WAILMER)
    @Test fun wailordCanBeSkipped() = skipsAndStaysFinished(Celebration.WAILORD)
    @Test fun bouffalantCanBeSkipped() = skipsAndStaysFinished(Celebration.BOUFFALANT)
    @Test fun veluzaCanBeSkipped() = skipsAndStaysFinished(Celebration.VELUZA)
    @Test fun mantykeCanBeSkipped() = skipsAndStaysFinished(Celebration.MANTYKE)
    @Test fun mantineCanBeSkipped() = skipsAndStaysFinished(Celebration.MANTINE)

    private fun skipsAndStaysFinished(celebration: Celebration) {
        var finished by mutableStateOf(false)
        var completions = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MathTheme {
                if (!finished) CelebrationDialog(celebration) { completions++; finished = true }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("view-results").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("celebration-${celebration.name}").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(CELEBRATION_DURATION_MS.toLong())
        compose.runOnIdle {
            assertTrue(finished)
            assertEquals(1, completions)
        }
    }
}
