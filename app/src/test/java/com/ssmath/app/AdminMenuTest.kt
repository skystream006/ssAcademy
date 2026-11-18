package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToneGenerator
import org.robolectric.Shadows.shadowOf
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.time.Duration
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class AdminMenuTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val preferences get() = application.getSharedPreferences("settings", 0)

    @Before fun setup() {
        preferences.edit().clear().commit()
        DebugLog.initialize(application)
    }

    @Test fun adminMenuAppearsBelowAppearance() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().assertIsDisplayed()
        val appearance = compose.onNodeWithText("Appearance").fetchSemanticsNode().boundsInRoot
        val admin = compose.onNodeWithTag("admin-menu").fetchSemanticsNode().boundsInRoot
        assertTrue("Admin Menu should follow Appearance", admin.top >= appearance.bottom)
    }

    @Test fun chanceGamesAreGroupedBehindAdminAndRetainDirectRewardAdjustments() {
        val model = MathViewModel(application)
        model.openSettings()
        compose.setContent { MathTheme { SettingsScreen(model) } }
        ChanceGame.entries.forEach { compose.onNodeWithTag(it.tag).assertDoesNotExist() }
        compose.openAdminSettings()
        ChanceGame.entries.forEach { compose.onNodeWithTag(it.tag).assertDoesNotExist() }
        compose.onNodeWithText("Chance games").performScrollTo().performClick()
        ChanceGame.entries.forEach { game ->
            compose.onNodeWithTag(game.tag).performScrollTo().performClick()
            compose.onNodeWithTag(if (game == ChanceGame.SLOT_MACHINE) "slot-machine-dialog"
                else "chance-game-dialog").assertIsDisplayed()
            assertEquals(game, model.chanceGame)
            compose.onNodeWithText("Back to Admin Menu").performClick()
        }
        compose.onNodeWithText("Add/Remove Rewards").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Add 1 whole Lollipop reward").performScrollTo().assertIsEnabled()
    }

    @Test fun wheelPlaysLocksAndRestoresWithoutAwardingAgain() = checkChanceBoard(ChanceGame.SPIN_THE_WHEEL)
    @Test fun chestsPlayLockAndRestoreWithoutAwardingAgain() = checkChanceBoard(ChanceGame.TREASURE_CHESTS)
    @Test fun plinkoPlaysLocksAndRestoresWithoutAwardingAgain() = checkChanceBoard(ChanceGame.PLINKO)
    @Test fun scratchCardPlaysLocksAndRestoresWithoutAwardingAgain() = checkChanceBoard(ChanceGame.SCRATCH_CARD)
    @Test fun clawPlaysLocksAndRestoresWithoutAwardingAgain() = checkChanceBoard(ChanceGame.CLAW_MACHINE)
    @Test fun balloonsPlayLockAndRestoreWithoutAwardingAgain() = checkChanceBoard(ChanceGame.BALLOON_POP)

    private fun checkChanceBoard(game: ChanceGame) {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        model.openSettings()
        model.chooseTier3RewardChances(100, 0, 0, 0)
        model.openAdminSlotMachine(game)
        model.chooseSlotMachineTier(RewardTier.TIER_3)
        model.chooseSlotMachineFragmentReward(true)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SlotMachineDialog(model, onDismiss = {}) } }
        compose.onNodeWithTag("chance-board-${game.name}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("chance-odds-TOYS_1").performScrollTo()
            .assertTextEquals("${RewardType.TOYS_1.fragmentLabel}: 100% per play")
        val choiceGame = game == ChanceGame.TREASURE_CHESTS || game == ChanceGame.BALLOON_POP
        val action = if (choiceGame) "chance-choice-2" else "chance-play"
        if (game == ChanceGame.PLINKO || game == ChanceGame.CLAW_MACHINE) {
            compose.onNodeWithTag("chance-position").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(0.8f) }
        }
        compose.onNodeWithTag(action).performScrollTo()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(64)
        if (game == ChanceGame.SCRATCH_CARD) {
            compose.onNodeWithTag("chance-board-${game.name}").performScrollTo().performTouchInput {
                repeat(4) {
                    swipe(Offset(width * 0.1f, height * 0.5f),
                        Offset(width * 0.9f, height * 0.5f), 200)
                }
            }
        } else compose.onNodeWithTag(action).performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { assertTrue(model.slotMachineBusy) }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag(action).assertIsNotEnabled()
        compose.onNodeWithTag("chance-fragment-reward").assertIsNotEnabled()
        compose.onNodeWithText("Back to Admin Menu").assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SLOT_MACHINE_DURATION_MS.toLong()))
        }
        compose.mainClock.advanceTimeBy(SLOT_MACHINE_DURATION_MS.toLong() + 64)
        compose.onNodeWithTag("slot-win-dialog").assertIsDisplayed()
        compose.onNodeWithTag("slot-win-reward").assertContentDescriptionEquals(
            rewardImageDescription(RewardType.TOYS_1, fragment = true))
        compose.runOnIdle {
            assertEquals(1, HistoryStore(File(application.filesDir, "practice_history.json"))
                .loadSnapshot().rewards.getValue(RewardType.TOYS_1).totalFragments)
        }
        compose.onNodeWithTag("slot-win-dismiss").performClick()
        repeat(2) {
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithTag("close-celebration").performClick()
        }
        compose.mainClock.advanceTimeBy(64)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("slot-win-dialog").assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }

    @Test fun slotMachineOpensBehindAdminPinAndSelectsAvailableTiers() {
        val model = MathViewModel(application)
        model.openSettings()
        RewardType.entries.filter { it.tier == RewardTier.TIER_2 }.forEach { model.chooseRewardEnabled(it, false) }
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("slot-machine").assertDoesNotExist()
        compose.openAdminSettings()
        compose.onNodeWithText("Chance games").performScrollTo().performClick()
        compose.onNodeWithTag("slot-machine").performScrollTo().performClick()
        compose.onNodeWithTag("slot-machine-dialog").assertIsDisplayed()
        compose.onNodeWithTag("slot-fragment-reward").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Reward: Fragment").assertIsDisplayed()
        compose.onNodeWithTag("slot-tier-TIER_2").assertIsNotEnabled()
        compose.onNodeWithTag("slot-tier-TIER_1").performClick()
        repeat(3) { compose.onNodeWithTag("slot-reel-$it").assertIsDisplayed() }
        compose.onNodeWithTag("slot-lever").assertIsEnabled()
        compose.onNodeWithText("Change tier").performScrollTo().performClick()
        compose.onNodeWithTag("slot-tier-TIER_3").performClick()
        compose.onNodeWithText("Slot machine - Tier 3").assertIsDisplayed()
        compose.onNodeWithTag("slot-fragment-reward").assertIsOn()
        compose.onNodeWithText("Back to Admin Menu").performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun slotAndAdminShowAvailableRewardsAndEffectiveOddsAtLargeTextSize() {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        model.openSettings()
        model.chooseTier3RewardChances(20, 80, 0)
        model.chooseTextSize(200)
        compose.setContent { MathTheme(textSizePercent = 200) { SettingsScreen(model) } }
        compose.openAdminSettings()
        compose.onNodeWithText("Available Rewards").performScrollTo().performClick()
        compose.onNodeWithTag("admin-slot-odds-TOYS_1").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Per reel: 20% · Win per pull: 0.8%") and
            hasAnyAncestor(hasTestTag("admin-slot-odds-TOYS_1"))).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("admin-slot-total-odds-TIER_3").performScrollTo()
            .assertTextEquals("Win per pull: 52% · No match: 48%")
        compose.onNodeWithTag("admin-slot-odds-TOYS_3").assertDoesNotExist()
        compose.onNodeWithTag("admin-slot-odds-GOOD_BEHAVIOR_POINT").assertDoesNotExist()
        compose.onNodeWithContentDescription("${RewardType.TOYS_2.label} reward").performScrollTo().performClick()
        compose.onNodeWithTag("admin-slot-odds-TOYS_2").assertDoesNotExist()
        compose.onNodeWithTag("admin-slot-total-odds-TIER_3").performScrollTo()
            .assertTextEquals("Win per pull: 100% · No match: 0%")
        compose.onNodeWithText("Chance games").performScrollTo().performClick()
        compose.onNodeWithTag("slot-machine").performScrollTo().performClick()
        compose.onNodeWithTag("slot-tier-TIER_3").performScrollTo().performClick()
        compose.onNodeWithTag("slot-odds-TOYS_1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("slot-odds-TOYS_2").assertDoesNotExist()
        compose.onNodeWithTag("slot-odds-TOYS_3").assertDoesNotExist()
        val odds = compose.onNode(hasText("Per reel: 100% · Win per pull: 100%") and
            hasAnyAncestor(hasTestTag("slot-odds-TOYS_1")))
        odds.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        odds.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertTrue(layouts.any { it.lineCount > 1 })
        assertTrue(layouts.none { it.hasVisualOverflow })
        compose.onNodeWithTag("slot-total-odds-TIER_3").performScrollTo()
            .assertTextEquals("Win per pull: 100% · No match: 0%")
        compose.onNodeWithText("Back to Admin Menu").performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test fun tierThreeAdminWinPresentsItsStandardAndMegaBonusesWithoutCreatingGameHistory() {
        val standard = Celebration.PIKACHU
        val mega = Celebration.megaPokemons.first()
        val file = File(application.filesDir, "practice_history.json")
        HistoryStore(file).collectPokemons(Celebration.entries - setOf(standard, mega), includeMegaPokemons = true)
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        model.openSettings()
        model.chooseTier3RewardChances(100, 0, 0)
        model.chooseSlotMachineFragmentReward(true)
        model.chooseSlotMachineTier(RewardTier.TIER_3)
        compose.setContent { MathTheme { SlotMachineDialog(model, onDismiss = {}) } }
        compose.onNodeWithTag("slot-lever").performScrollTo()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("slot-lever").performClick()
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SLOT_MACHINE_DURATION_MS.toLong()))
        }
        compose.mainClock.advanceTimeBy(SLOT_MACHINE_DURATION_MS.toLong() + 64)
        compose.onNodeWithTag("slot-win-dialog").assertIsDisplayed()
        val confetti = compose.onNodeWithTag("slot-win-confetti").getUnclippedBoundsInRoot()
        assertEquals((confetti.right - confetti.left).value, (confetti.bottom - confetti.top).value, 1f)
        val reward = compose.onNodeWithTag("slot-win-reward").getUnclippedBoundsInRoot()
        assertEquals((confetti.right - confetti.left).value * 0.72f, (reward.right - reward.left).value, 1f)
        compose.onNodeWithText("Bonus: ${standard.collectionLabel}").assertIsDisplayed()
        compose.onNodeWithText("Bonus: ${mega.collectionLabel}").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(setOf(standard, mega), HistoryStore(file).loadSnapshot().slotMachinePokemons)
        }
        compose.onNodeWithTag("slot-win-dismiss").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-${standard.name}").assertIsDisplayed()
        compose.onNodeWithText("You answered every question!").assertDoesNotExist()
        compose.onNodeWithTag("close-celebration").assertTextEquals("Next celebration").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-${standard.name}").assertDoesNotExist()
        compose.onNodeWithTag("celebration-${mega.name}").assertIsDisplayed()
        compose.onNodeWithTag("close-celebration").assertTextEquals("Close").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("celebration-${mega.name}").assertDoesNotExist()
        compose.onNodeWithTag("slot-lever").assertIsEnabled()
        compose.runOnIdle {
            assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.TOYS_1])
            assertTrue(model.history.isEmpty())
            assertTrue(model.readingRewardResults.isEmpty())
        }
        compose.mainClock.autoAdvance = true
    }

    @Test fun slotMachinePullLocksControlsAndAwardsOnceAcrossRestorationAndRepeatedMatches() {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        model.openSettings()
        RewardType.entries.forEach { model.chooseRewardEnabled(it, it == RewardType.LOLLIPOP) }
        model.chooseSlotMachineTier(RewardTier.TIER_1)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SlotMachineDialog(model, onDismiss = {}) } }
        compose.mainClock.autoAdvance = false
        repeat(2) { spin ->
            if (spin == 1) {
                compose.mainClock.autoAdvance = true
                compose.onNodeWithTag("slot-fragment-reward").performScrollTo().performClick().assertIsOn()
                compose.onNodeWithTag("slot-status").assertTextEquals("Pull the lever to spin all three slots.")
                compose.mainClock.autoAdvance = false
            }
            compose.onNodeWithTag("slot-lever").performScrollTo()
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithTag("slot-lever").performClick()
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithTag("slot-lever").assertIsNotEnabled()
            compose.onNodeWithText("Back to Admin Menu").assertIsNotEnabled()
            compose.onNodeWithText("Change tier").assertIsNotEnabled()
            compose.onNodeWithTag("slot-fragment-reward").assertIsNotEnabled()
            compose.onNodeWithTag("slot-win-dialog").assertDoesNotExist()
            restoration.emulateSavedInstanceStateRestore()
            compose.runOnIdle {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SLOT_MACHINE_DURATION_MS.toLong()))
            }
            compose.mainClock.advanceTimeBy(64)
            val initialPrizeWidth = compose.onNodeWithTag("slot-win-reward")
                .fetchSemanticsNode().boundsInRoot.width
            compose.mainClock.advanceTimeBy(SLOT_MACHINE_DURATION_MS.toLong() + 64)
            val settledPrizeWidth = compose.onNodeWithTag("slot-win-reward")
                .fetchSemanticsNode().boundsInRoot.width
            assertTrue("The winning prize should pop into view", settledPrizeWidth > initialPrizeWidth)
            compose.onNodeWithTag("slot-win-dialog").assertIsDisplayed()
            compose.onNodeWithText(if (spin == 0) "You won Lollipop!"
                else "You won Lollipop Fragment!").assertIsDisplayed()
            compose.onNodeWithTag("slot-win-reward").assertContentDescriptionEquals(
                rewardImageDescription(RewardType.LOLLIPOP, fragment = spin == 1))
            compose.onNodeWithTag("slot-win-confetti").assertIsDisplayed()
            compose.mainClock.autoAdvance = true
            compose.onNodeWithTag("slot-win-dismiss").performClick()
            compose.onNodeWithTag("slot-win-dialog").assertDoesNotExist()
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithTag("slot-win-dialog").assertDoesNotExist()
            compose.mainClock.autoAdvance = false
            repeat(3) {
                compose.onNodeWithTag("slot-label-$it").assertTextEquals(
                    if (spin == 0) "Lollipop" else "Lollipop Fragment")
            }
            compose.onNodeWithTag("slot-status").assertTextEquals(
                if (spin == 0) "You won Lollipop! One complete reward added."
                else "You won Lollipop Fragment! One fragment added.")
            compose.onNodeWithTag("slot-lever").assertIsEnabled()
            compose.onNodeWithTag("slot-fragment-reward").assertIsEnabled()
            compose.runOnIdle { assertEquals(RewardBalance(1, spin), model.rewardBalances[RewardType.LOLLIPOP]) }
        }
        compose.mainClock.autoAdvance = true
    }

    @Test fun slotMachineNonMatchDoesNotCelebrate() {
        val random = object : Random() {
            private var next = 0
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(until: Int): Int = next++.mod(until)
        }
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined, rewardRandom = random)
        model.openSettings()
        model.chooseSlotMachineTier(RewardTier.TIER_2)
        compose.setContent { MathTheme { SlotMachineDialog(model, onDismiss = {}) } }
        compose.onNodeWithTag("slot-lever").performScrollTo().performClick()
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SLOT_MACHINE_DURATION_MS.toLong()))
        }
        compose.onNodeWithTag("slot-status").assertTextEquals("No match. Pull again!")
        compose.onNodeWithTag("slot-win-dialog").assertDoesNotExist()
    }

    @Test fun slotMachineDingsOnlyWhileSpinningAndResumed() {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        model.openSettings()
        model.chooseSlotMachineTier(RewardTier.TIER_1)
        compose.setContent { MathTheme { SlotMachineDialog(model, onDismiss = {}) } }
        assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty())
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("slot-lever").performScrollTo()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("slot-lever").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(360))
        }
        compose.mainClock.advanceTimeBy(360)
        val rollingCount = ShadowToneGenerator.getPlayedTones().size
        assertTrue("Expected repeated dings, got $rollingCount", rollingCount > 1)
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(360))
        }
        compose.mainClock.advanceTimeBy(360)
        assertEquals(rollingCount, ShadowToneGenerator.getPlayedTones().size)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(360))
        }
        compose.mainClock.advanceTimeBy(360)
        assertTrue(ShadowToneGenerator.getPlayedTones().size > rollingCount)
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SLOT_MACHINE_DURATION_MS.toLong()))
        }
        compose.mainClock.advanceTimeBy(SLOT_MACHINE_DURATION_MS.toLong() + 64)
        val finishedCount = ShadowToneGenerator.getPlayedTones().size
        compose.runOnIdle {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(360))
        }
        compose.mainClock.advanceTimeBy(360)
        assertEquals(finishedCount, ShadowToneGenerator.getPlayedTones().size)
        compose.mainClock.autoAdvance = true
    }

    @Test fun slotMachineOnlyCelebratesAfterFailedSaveIsSuccessfullyRetried() {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        model.openSettings()
        RewardType.entries.forEach { model.chooseRewardEnabled(it, it == RewardType.LOLLIPOP) }
        model.chooseSlotMachineTier(RewardTier.TIER_1)
        val blockedWrite = File(application.filesDir, "practice_history.json.tmp")
        assertTrue(blockedWrite.mkdir())
        try {
            compose.setContent { MathTheme { SlotMachineDialog(model, onDismiss = {}) } }
            compose.onNodeWithTag("slot-lever").performScrollTo().performClick()
            compose.runOnIdle {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SLOT_MACHINE_DURATION_MS.toLong()))
            }
            compose.onNodeWithTag("slot-win-dialog").assertDoesNotExist()
            compose.onNodeWithTag("slot-retry").assertIsDisplayed()
            assertTrue(blockedWrite.delete())
            compose.onNodeWithTag("slot-retry").performClick()
            compose.onNodeWithTag("slot-win-dialog").assertIsDisplayed()
            compose.onNodeWithText("One complete reward added to My Rewards.").assertIsDisplayed()
        } finally {
            blockedWrite.delete()
        }
    }

    @Test fun firstEntryRequiresValidMatchingPinAndEachReopeningRequiresVerification() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Create Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin").assertIsFocused()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("12a4", "12a4")
        compose.onNodeWithText("Use 4–8 digits.").assertIsDisplayed()
        submit("4826", "4827")
        compose.onNodeWithText("PINs do not match.").assertIsDisplayed()
        assertFalse(AdminPinStore(preferences).hasPin)
        submit("4826", "4826")
        awaitTag("admin-dialog")
        listOf("Game Settings", "Add/Remove Rewards", "Available Rewards").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Reading activities").assertDoesNotExist()
        compose.onNodeWithText("My Pokémons").assertDoesNotExist()
        compose.onNodeWithText("My Rewards").assertDoesNotExist()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin").assertIsFocused()
        compose.onNodeWithTag("admin-pin-confirm").assertDoesNotExist()
        submit("9999")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Incorrect PIN. Try again.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("4826")
        awaitTag("admin-dialog")
    }

    @Test fun cancellingOrBackingOutOfCreationDoesNotSetPinOrExposeControls() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-pin").performTextInput("4826")
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(AdminPinStore(preferences).hasPin)
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Create Admin PIN").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin").assertIsFocused()
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
        assertFalse(AdminPinStore(preferences).hasPin)
    }

    @Test fun restorationAndLeavingSettingsRelockAccessWithoutLosingPin() {
        val model = MathViewModel(application)
        model.openSettings()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathAppContent(model) }
        compose.openAdminSettings()
        compose.openDebugSettings()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("debug-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { model.openSettings() }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        submit("4826")
        awaitTag("admin-dialog")
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun pinCreationIsScrollableAtDoubleTextSize() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme(textSizePercent = 200) { SettingsScreen(model) } }
        compose.openAdminSettings()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test fun changingPinReturnsToAdminAndReopeningRequiresOnlyTheNewPin() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        submit("4826", "4826")
        awaitTag("admin-dialog")
        openChangePin()
        listOf("current", "new", "confirm").forEach {
            compose.onNodeWithTag("change-pin-$it").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        }
        compose.runOnIdle {
            val window = requireNotNull(ShadowDialog.getLatestDialog().window)
            assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
        fillChangePin("4826", "00123456", "00123456")
        compose.onNodeWithTag("change-pin-submit").performClick()
        awaitChangeClosed()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        submit("4826")
        awaitError("Incorrect PIN. Try again.")
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        submit("00123456")
        awaitTag("admin-dialog")
    }

    @Test fun changePinValidationMismatchAndWrongCurrentPinNeverReplaceCredentials() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.openAdminSettings()
        val previous = preferences.all.toMap()
        openChangePin()
        compose.onNodeWithTag("change-pin-submit").assertIsNotEnabled()
        listOf("123" to "0000", "4826" to "123", "4826" to "１２３４").forEach { (current, new) ->
            fillChangePin(current, new, new)
            compose.onNodeWithTag("change-pin-submit").performClick()
            awaitError("Use 4–8 digits for each PIN.")
            assertChangeFieldsEmpty()
            assertEquals(previous, preferences.all)
        }
        fillChangePin("4826", "0000", "1111")
        compose.onNodeWithTag("change-pin-submit").performClick()
        awaitError("PINs do not match.")
        assertChangeFieldsEmpty()
        assertEquals(previous, preferences.all)

        repeat(5) { attempt ->
            fillChangePin("9999", "0000", "0000")
            compose.onNodeWithTag("change-pin-submit").performClick()
            awaitError(if (attempt == 4) "Too many attempts. Try again in 30 seconds." else "Incorrect current PIN. Try again.")
            assertChangeFieldsEmpty()
        }
        assertEquals(previous["admin_pin_hash"], preferences.getString("admin_pin_hash", null))
        assertEquals(previous["admin_pin_salt"], preferences.getString("admin_pin_salt", null))
        assertEquals(AdminPinStore.Result.LOCKED, AdminPinStore(preferences).verify("4826"))
        fillChangePin("4826", "0000", "0000")
        compose.onNodeWithTag("change-pin-submit").performClick()
        awaitError("Too many attempts. Try again in 30 seconds.")
        compose.onNodeWithTag("change-pin-dialog").assertIsDisplayed()
    }

    @Test fun cancelAndBackDiscardChangePinFieldsWithoutChangingCredentials() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.openAdminSettings()
        val previous = preferences.all.toMap()
        openChangePin()
        fillChangePin("4826", "0000", "0000")
        compose.onNodeWithText("Cancel").performClick()
        awaitTag("admin-dialog")
        compose.onNodeWithTag("change-pin-dialog").assertDoesNotExist()
        assertEquals(previous, preferences.all)
        openChangePin()
        assertChangeFieldsEmpty()
        fillChangePin("4826", "0000", "0000")
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        awaitTag("admin-dialog")
        compose.onNodeWithTag("change-pin-dialog").assertDoesNotExist()
        assertEquals(previous, preferences.all)
        openChangePin()
        assertChangeFieldsEmpty()
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("4826"))
    }

    @Test fun restorationWhileChangingPinRelocksAndDiscardsAllPinText() {
        val model = MathViewModel(application)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SettingsScreen(model) } }
        compose.openAdminSettings()
        val previous = preferences.all.toMap()
        openChangePin()
        fillChangePin("4826", "0000", "0000")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("change-pin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("change-pin").assertDoesNotExist()
        assertEquals(previous, preferences.all)
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-pin").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("admin-pin-submit").assertIsNotEnabled()
        submit("4826")
        awaitTag("admin-dialog")
        openChangePin()
        assertChangeFieldsEmpty()
    }

    @Test fun debugPinBypassPersistsAndReenablingPreservesTheExistingPin() {
        val model = MathViewModel(application)
        AdminPinStore(preferences).create("4826")
        val hash = preferences.getString("admin_pin_hash", null)
        val salt = preferences.getString("admin_pin_salt", null)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.openDebugSettings()
        compose.onNodeWithContentDescription("Disable Admin PIN lock").assertIsOff().performClick().assertIsOn()
        assertFalse(MathViewModel(application).adminPinLockEnabled)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        repeat(2) {
            compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
            compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
            compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
            compose.onNodeWithText("Close").performClick()
        }
        compose.openDebugSettings()
        compose.onNodeWithContentDescription("Disable Admin PIN lock").performClick()
        compose.onNodeWithTag("debug-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-dialog").assertDoesNotExist()
        compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
        assertTrue(MathViewModel(application).adminPinLockEnabled)
        assertEquals(hash, preferences.getString("admin_pin_hash", null))
        assertEquals(salt, preferences.getString("admin_pin_salt", null))
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        submit("4826")
        awaitTag("admin-dialog")
    }

    @Test fun debugBypassWithoutPinAllowsCreatingOneAndReturnsToAdmin() {
        val model = MathViewModel(application)
        model.chooseAdminPinLockEnabled(false)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Create PIN").performClick()
        submit("4826", "4826")
        awaitTag("admin-dialog")
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("4826"))
        compose.onNodeWithText("Change PIN").assertIsDisplayed()
    }

    @Test fun pinBypassKeepsAdminReturnDestinationAfterRestoringEitherChildPage() {
        val model = MathViewModel(application)
        model.chooseAdminPinLockEnabled(false)
        model.openSettings()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathAppContent(model) }
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.runOnIdle { model.openRewardsSetup() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("save-rewards-setup").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.runOnIdle { model.openReadingProgress() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("reading-back").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
        compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
    }

    @Test fun adminDefaultsReturnOnSaveAndBackAndRelockAfterRestoration() {
        val model = MathViewModel(application)
        model.openSettings()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathAppContent(model) }
        compose.openGameSettings()
        val sections = listOf("Basic Math Settings" to "rewards-setup",
            "Multiplication Matrix Settings" to "matrix-rewards-setup",
            "Logic Math Settings" to "logic-rewards-setup")
        sections.forEach { (section, tag) ->
            compose.onNodeWithText(section).performScrollTo().performClick()
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.onNodeWithTag("save-rewards-setup").performScrollTo().performClick()
            compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
            compose.onNodeWithTag("admin-pin-dialog").assertDoesNotExist()
        }
        compose.onNodeWithTag("logic-rewards-setup").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("rewards-setup-screen").assertDoesNotExist()
        compose.onNodeWithText("Enter Admin PIN").assertIsDisplayed()
        submit("4826")
        awaitTag("rewards-setup-screen")
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun settingsFollowAppearanceWithoutChangingSavedPreferences() {
        val model = MathViewModel(application)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        val appearances = listOf(Triple("green", null, true)) +
            COLOR_THEMES.map { (theme, _) -> Triple(theme, null, false) } +
            COLOR_THEMES.flatMap { (theme, _) ->
                listOf(Triple(theme, "dark", false), Triple(theme, "light", false))
            }
        appearances.forEach { (theme, mode, wave) ->
            compose.runOnIdle {
                model.chooseWaveAppearance(wave)
                model.chooseTheme(theme)
                if (mode != null) model.chooseDark(mode == "dark")
                model.chooseSkin(AppSkin.GALAXY)
                model.chooseSkins(true)
            }
            compose.onNodeWithTag("settings-screen").assertIsDisplayed()
            val dark = wave || isDarkTheme(theme, mode)
            val expectedSurface = when {
                wave -> Color(0xFF080C12)
                theme == "black" -> if (dark) Color(0xFF181818) else Color.White
                dark -> Color(0xFF191E1C)
                else -> Color(0xFFF6F8F6)
            }
            assertEquals("Settings must follow $theme/$mode/$wave",
                expectedSurface, screenCornerColor("settings-screen"))
            compose.runOnIdle {
                assertEquals(theme, model.theme)
                assertEquals(mode, model.mode)
                assertEquals(wave, model.waveAppearance)
                assertTrue(model.skinsEnabled)
                assertEquals(AppSkin.GALAXY, model.skin)
                assertEquals(theme, preferences.getString("theme", null))
                assertEquals(mode, preferences.getString("mode", null))
                assertEquals(wave, preferences.getBoolean("wave_appearance", !wave))
            }
        }
        compose.runOnIdle {
            model.chooseWaveAppearance(false)
            model.chooseTheme("black")
            model.chooseDark(true)
            model.chooseSkins(false)
            model.chooseTextSize(150)
        }
        compose.onNodeWithText("Appearance").performScrollTo().performClick()
        compose.onNodeWithText("Text size: 150%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dark appearance").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("subject-Math").assertIsDisplayed()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            assertTrue(cornerColor(view, 1, view.height / 2).luminance() < 0.1f)
            assertEquals("dark", model.mode)
            assertEquals("black", preferences.getString("theme", null))
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun darkAppearanceSwitchImmediatelyUpdatesSettingsAndSurvivesReopening() {
        val model = MathViewModel(application)
        model.chooseWaveAppearance(false)
        model.chooseDark(false)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Appearance").performScrollTo().performClick()
        val toggle = compose.onNodeWithContentDescription("Dark appearance")
        toggle.performScrollTo().assertIsOff().performClick().assertIsOn()
        assertTrue(screenCornerColor("settings-screen").luminance() < 0.1f)
        assertEquals("dark", preferences.getString("mode", null))
        toggle.performClick().assertIsOff()
        assertTrue(screenCornerColor("settings-screen").luminance() > 0.8f)
        assertEquals("light", preferences.getString("mode", null))
        toggle.performClick().assertIsOn()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        assertTrue(screenCornerColor("settings-screen").luminance() < 0.1f)
        compose.runOnIdle { assertEquals("dark", MathViewModel(application).mode) }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun adminDialogsAndChildPagesFollowLightDarkAndWaveAppearance() {
        val model = MathViewModel(application)
        model.chooseTheme("black")
        model.openSettings()
        AdminPinStore(preferences).create("4826")
        compose.setContent { MathAppContent(model) }
        listOf(false to false, false to true, true to false).forEach { (wave, dark) ->
            compose.runOnIdle {
                model.chooseWaveAppearance(wave)
                model.chooseDark(dark)
            }
            val surface = screenCornerColor("settings-screen")
            compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
            compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
            assertEquals(surface, dialogCornerColor())
            submit("4826")
            awaitTag("admin-dialog")
            assertEquals(surface, dialogCornerColor())
            openChangePin()
            assertEquals(surface, dialogCornerColor())
            compose.onNodeWithText("Cancel").performClick()
            compose.runOnIdle { model.openRewardsSetup() }
            compose.onNodeWithTag("rewards-setup-screen").assertIsDisplayed()
            assertEquals(surface, screenCornerColor("settings-screen"))
            compose.onNodeWithTag("save-rewards-setup").performScrollTo().performClick()
            compose.runOnIdle { model.openReadingProgress() }
            compose.onNodeWithTag("reading-back").performScrollTo().assertIsDisplayed()
            assertEquals(surface, screenCornerColor("settings-screen"))
            compose.onNodeWithTag("reading-back").performClick()
            compose.onNodeWithText("Close").performClick()
            compose.openDebugSettings()
            assertEquals(surface, dialogCornerColor())
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithText("Close").performClick()
            compose.runOnIdle { assertEquals(if (dark) "dark" else "light", model.mode) }
        }
    }

    private fun screenCornerColor(tag: String): Color {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle {
            cornerColor(compose.activity.findViewById(android.R.id.content),
                bounds.left.toInt() + 1, bounds.top.toInt() + 1)
        }
    }

    private fun dialogCornerColor(): Color = compose.runOnIdle {
        cornerColor(ShadowDialog.getLatestDialog().findViewById(android.R.id.content), 1, 1)
    }

    private fun cornerColor(view: View, x: Int, y: Int): Color {
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(image))
            Color(image.getPixel(x, y))
        } finally {
            image.recycle()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun subsectionsGetDarkerInLightThemesAndLighterInDarkAndWaveThemes() {
        val appearances = listOf(Triple("green", null, true)) +
            COLOR_THEMES.map { (theme, _) -> Triple(theme, null, false) } +
            COLOR_THEMES.flatMap { (theme, _) ->
                listOf(Triple(theme, "dark", false), Triple(theme, "light", false))
            }
        val appearance = mutableStateOf(appearances.first())
        var mutedTextColor = Color.Unspecified
        compose.setContent {
            val (theme, mode, wave) = appearance.value
            MathTheme(theme = theme, mode = mode, waveAppearance = wave) {
                mutedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Text("Parent")
                        CollapsibleSettingsSection("Outer", defaultExpanded = true) {
                            CollapsibleSettingsSection("Inner", defaultExpanded = true) {
                                CollapsibleSettingsSection("Deepest", defaultExpanded = true) { Text("Content") }
                            }
                        }
                    }
                }
            }
        }
        appearances.forEach { current ->
            compose.runOnIdle { appearance.value = current }
            val outer = compose.onNodeWithTag("settings-section-Outer").fetchSemanticsNode().boundsInRoot
            val inner = compose.onNodeWithTag("settings-section-Inner").fetchSemanticsNode().boundsInRoot
            val deepest = compose.onNodeWithTag("settings-section-Deepest").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle {
                val view = compose.activity.findViewById<View>(android.R.id.content)
                val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(image))
                    val parentColor = Color(image.getPixel(outer.center.x.toInt(), 1))
                    val outerColor = Color(image.getPixel(outer.center.x.toInt(), outer.top.toInt() + 1))
                    val innerColor = Color(image.getPixel(inner.center.x.toInt(), inner.top.toInt() + 1))
                    val deepestColor = Color(image.getPixel(deepest.center.x.toInt(), deepest.top.toInt() + 1))
                    val dark = current.third || isDarkTheme(current.first, current.second)
                    listOf(parentColor, outerColor, innerColor, deepestColor).zipWithNext().forEach { (parent, child) ->
                        if (dark) {
                            assertTrue("Nested sections must get lighter for $current",
                                child.luminance() > parent.luminance())
                        } else {
                            assertTrue("Nested sections must get darker for $current",
                                child.luminance() < parent.luminance())
                        }
                    }
                    if (dark) {
                        val contrast = (mutedTextColor.luminance() + 0.05f) / (deepestColor.luminance() + 0.05f)
                        assertTrue("Nested dark sections must keep readable text for $current", contrast >= 4.5f)
                    }
                } finally {
                    image.recycle()
                }
            }
        }
    }

    private fun submit(pin: String, confirmation: String? = null) {
        compose.onNodeWithTag("admin-pin").performScrollTo().performTextReplacement(pin)
        confirmation?.let {
            compose.onNodeWithTag("admin-pin-confirm").performScrollTo().performTextReplacement(it)
        }
        compose.onNodeWithTag("admin-pin-submit").performClick()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun openChangePin() {
        compose.onNodeWithTag("change-pin").performScrollTo().performClick()
        awaitTag("change-pin-dialog")
    }

    private fun fillChangePin(current: String, new: String, confirmation: String) {
        compose.onNodeWithTag("change-pin-current").performScrollTo().assertIsDisplayed().performTextReplacement(current)
        compose.onNodeWithTag("change-pin-new").performScrollTo().assertIsDisplayed().performTextReplacement(new)
        compose.onNodeWithTag("change-pin-confirm").performScrollTo().assertIsDisplayed().performTextReplacement(confirmation)
    }

    private fun assertChangeFieldsEmpty() {
        listOf("current", "new", "confirm").forEach {
            compose.onNodeWithTag("change-pin-$it").assertTextEquals("")
        }
        compose.onNodeWithTag("change-pin-submit").assertIsNotEnabled()
    }

    private fun awaitError(message: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(message).performScrollTo().assertIsDisplayed()
    }

    private fun awaitChangeClosed() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("change-pin-dialog").fetchSemanticsNodes().isEmpty() }
    }
}
