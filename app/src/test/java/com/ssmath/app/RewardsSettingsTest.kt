package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class RewardsSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        File(application.filesDir, "practice_history.json").delete()
        DebugLog.initialize(application)
    }

    @Test fun rewardsAndMathSectionsStartCollapsedAndKeepChoicesWhenToggledIndependently() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        val rewards = compose.onNodeWithText("Rewards")
        val math = compose.onNodeWithText("Math Settings")
        val collapsed = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed")
        val expanded = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded")
        rewards.assertHasClickAction().assert(collapsed)
        math.assertHasClickAction().assert(collapsed)
        compose.onNodeWithTag("my-rewards").assertDoesNotExist()
        compose.onNodeWithTag("my-pokemons").assertDoesNotExist()
        compose.onNodeWithContentDescription("Math Rewards System").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show timer").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show correct answers").assertDoesNotExist()
        compose.onNodeWithContentDescription("Vertical equations").assertDoesNotExist()
        compose.onNodeWithTag("rewards-setup").assertDoesNotExist()
        compose.onNodeWithTag("practice-history").assertDoesNotExist()

        rewards.performClick().assert(expanded)
        compose.onNodeWithTag("my-rewards").assertIsDisplayed()
        compose.onNodeWithTag("my-pokemons").assertIsDisplayed()
        math.assert(collapsed).performScrollTo().performClick().assert(expanded)
        compose.onNodeWithContentDescription("Math Rewards System").performScrollTo()
            .assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag("practice-history").performScrollTo().assert(hasText("Math Practice History"))
        compose.onNodeWithContentDescription("Show timer").performScrollTo().assertIsOff()
        compose.onNodeWithContentDescription("Show correct answers").performScrollTo()
            .assertIsOn().performClick().assertIsOff()
        compose.onNodeWithContentDescription("Vertical equations").performScrollTo()
            .assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag("rewards-setup").performScrollTo()
            .assert(hasText("Default Math Reward Game Settings"))
        compose.onNodeWithText("Rewards Game Setup").assertDoesNotExist()

        rewards.performScrollTo().performClick().assert(collapsed)
        compose.onNodeWithTag("my-rewards").assertDoesNotExist()
        compose.onNodeWithTag("my-pokemons").assertDoesNotExist()
        compose.onNodeWithContentDescription("Math Rewards System").assertExists()
        math.assert(expanded).performScrollTo().performClick().assert(collapsed)
        compose.onNodeWithContentDescription("Math Rewards System").assertDoesNotExist()
        compose.onNodeWithTag("practice-history").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show timer").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show correct answers").assertDoesNotExist()
        compose.onNodeWithContentDescription("Vertical equations").assertDoesNotExist()
        compose.onNodeWithTag("rewards-setup").assertDoesNotExist()

        rewards.performScrollTo().performClick()
        math.performScrollTo().performClick()
        compose.onNodeWithContentDescription("Math Rewards System").performScrollTo().assertIsOff()
        compose.onNodeWithContentDescription("Show correct answers").performScrollTo().assertIsOff()
        compose.onNodeWithContentDescription("Vertical equations").performScrollTo().assertIsOff()
        val restored = MathViewModel(application)
        assertFalse(restored.rewardsEnabled)
        assertFalse(restored.showCorrectAnswers)
        assertFalse(restored.verticalEquations)
    }

    @Test fun rewardsAndMathExpansionSurvivesSavedStateRestorationIndependently() {
        val model = MathViewModel(application)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SettingsScreen(model) } }
        val rewards = compose.onNodeWithText("Rewards")
        val math = compose.onNodeWithText("Math Settings")
        rewards.performClick()
        restoration.emulateSavedInstanceStateRestore()
        rewards.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        math.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithTag("my-rewards").assertExists()
        compose.onNodeWithTag("rewards-setup").assertDoesNotExist()
        rewards.performScrollTo().performClick()
        math.performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rewards.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        math.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        compose.onNodeWithTag("my-rewards").assertDoesNotExist()
        compose.onNodeWithTag("rewards-setup").performScrollTo().assertIsDisplayed()
    }

    @Test fun previouslySavedDisabledOptionsOverrideEnabledDefaults() {
        application.getSharedPreferences("settings", 0).edit()
            .putBoolean("rewards_enabled", false)
            .putBoolean("show_correct_answers", false)
            .putBoolean("vertical_equations", false)
            .commit()
        val model = MathViewModel(application)
        assertFalse(model.rewardsEnabled)
        assertFalse(model.showCorrectAnswers)
        assertFalse(model.verticalEquations)
    }

    @Test fun subjectRewardPreferencesAreIndependentAndPreserveLegacyMathChoice() {
        application.getSharedPreferences("settings", 0).edit().putBoolean("rewards_enabled", false).commit()
        val model = MathViewModel(application)
        assertFalse(model.rewardsEnabled)
        Subject.entries.filter { it != Subject.MATH }.forEach { assertTrue(model.rewardsEnabled(it)) }
        Subject.entries.forEach { subject ->
            model.chooseSubjectRewardsEnabled(subject, false)
            val before = Subject.entries.associateWith { model.rewardsEnabled(it) }
            model.chooseSubjectRewardsEnabled(subject, true)
            Subject.entries.forEach { other ->
                assertEquals(other == subject || before.getValue(other), model.rewardsEnabled(other))
            }
            model.chooseSubjectRewardsEnabled(subject, false)
        }
        val restored = MathViewModel(application)
        Subject.entries.forEach { assertFalse(restored.rewardsEnabled(it)) }
        restored.chooseRewardsEnabled(true)
        assertTrue(restored.rewardsEnabled)
        assertFalse(restored.rewardsEnabled(Subject.READING))
    }

    @Test fun eachSubjectHasItsOwnCollapsibleRewardsAndHistorySettings() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        Subject.entries.forEach { subject ->
            val section = compose.onNodeWithText(
                if (subject == Subject.ADVANCED) "Advanced Subject Settings" else "${subject.label} Settings")
            val toggle = compose.onNodeWithContentDescription("${subject.label} Rewards System")
            val history = compose.onNodeWithText("${subject.label} Practice History")
            toggle.assertDoesNotExist()
            history.assertDoesNotExist()
            section.performScrollTo().performClick()
            toggle.performScrollTo().assertIsOn().performClick().assertIsOff()
            history.performScrollTo().assertIsDisplayed().assertHasClickAction()
            Subject.entries.filter { it != subject }.forEach { other ->
                assertTrue(model.rewardsEnabled(other))
            }
            toggle.performScrollTo().performClick().assertIsOn()
            section.performScrollTo().performClick()
            toggle.assertDoesNotExist()
            history.assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("Rewards system").assertDoesNotExist()
        compose.onNodeWithText("Practice History").assertDoesNotExist()
    }

    @Test fun activeReadingGamesDialogSavesIndependentChoicesAndAllowsAllGamesToBeDisabled() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("active-reading-games").assertDoesNotExist()
        compose.onNodeWithText("Reading Settings").performScrollTo().performClick()
        compose.onNodeWithTag("active-reading-games").performScrollTo().performClick()
        compose.onNodeWithTag("active-reading-games-dialog").assertIsDisplayed()
        compose.onNodeWithContentDescription("Caption Quest").assertDoesNotExist()
        compose.onNodeWithText("Caption Quest").assertDoesNotExist()
        ReadingGame.entries.forEach { game ->
            compose.onNodeWithContentDescription(game.title).performScrollTo().assertIsOn()
                .performClick().assertIsOff()
        }
        assertEquals(ReadingGame.entries.toSet(), model.disabledReadingGames)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("active-reading-games-dialog").assertDoesNotExist()
        compose.onNodeWithTag("active-reading-games").performClick()
        ReadingGame.entries.forEach { game ->
            compose.onNodeWithContentDescription(game.title).performScrollTo().assertIsOff()
        }
        compose.onNodeWithContentDescription(ReadingGame.STUDIO.title).performScrollTo()
            .performClick().assertIsOn()
        compose.onNodeWithText("Close").performClick()
        val restored = MathViewModel(application)
        assertEquals(ReadingGame.entries.toSet() - ReadingGame.STUDIO, restored.disabledReadingGames)
        Subject.entries.forEach { assertTrue(restored.rewardsEnabled(it)) }
        assertTrue(restored.disabledRewards.isEmpty())
    }

    @Test fun activeReadingGamesDialogSurvivesStateRestorationAndBackKeepsChangesAtLargeTextSize() {
        val model = MathViewModel(application)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme(textSizePercent = 200) { SettingsScreen(model) } }
        compose.onNodeWithText("Reading Settings").performScrollTo().performClick()
        compose.onNodeWithTag("active-reading-games").performScrollTo().performClick()
        compose.onNodeWithContentDescription(ReadingGame.STUDIO.title).performScrollTo()
            .assertIsDisplayed().performClick().assertIsOff()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("active-reading-games-dialog").assertIsDisplayed()
        compose.onNodeWithContentDescription(ReadingGame.STUDIO.title).performScrollTo().assertIsOff()
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        compose.onNodeWithTag("active-reading-games-dialog").assertDoesNotExist()
        compose.onNodeWithText("Settings").assertIsDisplayed()
        assertEquals(setOf(ReadingGame.STUDIO), MathViewModel(application).disabledReadingGames)
    }

    @Test fun readingGamePreferencesIgnoreUnknownNamesAndCanRestoreAllDefaults() {
        application.getSharedPreferences("settings", 0).edit()
            .putStringSet("disabled_reading_games", setOf("MISSING", "CAPTION", "UNKNOWN_GAME")).commit()
        val model = MathViewModel(application)
        assertEquals(setOf(ReadingGame.MISSING), model.disabledReadingGames)
        model.chooseReadingGameEnabled(ReadingGame.MISSING, true)
        assertTrue(MathViewModel(application).disabledReadingGames.isEmpty())
    }

    @Test fun myRewardsIsBelowUpdatesAndReturnsToSettings() {
        val model = MathViewModel(application)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Rewards").performClick()
        val updates = compose.onNodeWithText("App updates").fetchSemanticsNode().boundsInRoot
        val rewards = compose.onNodeWithTag("my-rewards").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val history = compose.onNodeWithText("Math Settings").fetchSemanticsNode().boundsInRoot
        assertTrue(updates.bottom <= rewards.top)
        assertTrue(rewards.bottom <= history.top)
        compose.onNodeWithTag("my-rewards").performClick()
        assertEquals(Overlay.REWARDS, model.overlay)
        compose.onNodeWithText("My Rewards").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(Overlay.SETTINGS, model.overlay)
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test fun timeLimitRequiresBothSwitchesAndRemembersSelection() {
        val model = MathViewModel(application)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithText("Math Settings").performScrollTo().performClick()
        compose.onNodeWithText("Earn prize fragments in Rewards Game by completing 25 or more questions with over 90% correct")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Math Rewards System").performScrollTo().assertIsOn()
        compose.onNodeWithContentDescription("Show correct answers").performScrollTo().assertIsOn()
        compose.onNodeWithTag("time-limit").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show timer").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("time-limit").performScrollTo().performClick()
        compose.onNodeWithText("5 minutes").performClick()
        assertEquals(300, model.timeLimitSeconds)
        compose.onNodeWithContentDescription("Show timer").performScrollTo().performClick()
        compose.onNodeWithTag("time-limit").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show timer").performClick()
        compose.onNodeWithTag("time-limit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Math Rewards System").performScrollTo().performClick()
        compose.onNodeWithTag("time-limit").assertDoesNotExist()
        val restored = MathViewModel(application)
        assertFalse(restored.rewardsEnabled)
        assertTrue(restored.showTimer)
        assertEquals(300, restored.timeLimitSeconds)
    }

    @Test fun upperRightUseRewardsActionConfirmsAndUpdatesTheInventory() {
        val file = File(application.filesDir, "practice_history.json")
        val store = HistoryStore(file)
        repeat(5) {
            val result = PracticeResult(it + 1L, it + 1L, Operation.ADDITION, 10, 1_000,
                List(26) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
                prizeType = RewardType.LOLLIPOP)
            store.add(result)
            store.claimReward(result.id)
        }
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Main.immediate)
        model.chooseRewardsEnabled(false)
        model.openRewards()
        compose.setContent { MathAppContent(model) }
        assertFalse(model.rewardsEnabled)
        val title = compose.onNodeWithText("My Rewards").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Use rewards").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(button.left >= title.right)
        assertEquals(title.center.y, button.center.y, 1f)
        compose.onNodeWithText("Use rewards").performClick()
        compose.onNodeWithTag("use-reward-LOLLIPOP").assert(hasText("Whole: 1")).performClick()
        compose.onNodeWithText("You would like to use 1 Lollipop?").assertIsDisplayed()
        compose.onNodeWithText("No").performClick()
        assertEquals(RewardBalance(1, 2), model.rewardBalances[RewardType.LOLLIPOP])
        compose.onNodeWithTag("use-reward-LOLLIPOP").performClick()
        compose.onNodeWithText("Yes").performClick()
        compose.onNodeWithTag("use-reward-LOLLIPOP").assertIsNotEnabled().assert(hasText("Whole: 0"))
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("inventory-whole-LOLLIPOP").onChildren()
            .filterToOne(hasText("Whole: 0")).assertIsDisplayed()
        compose.onNodeWithTag("inventory-fragments-LOLLIPOP").onChildren()
            .filterToOne(hasText("Fragments: 2/3")).assertIsDisplayed()
        assertEquals(RewardBalance(0, 2), HistoryStore(file).loadSnapshot().rewards[RewardType.LOLLIPOP])
        assertEquals(5, model.history.size)
    }

    @Test fun timeLimitOffersShortLimitsPerFiveQuestionsAndKeepsExistingChoices() {
        val model = MathViewModel(application)
        model.chooseRewardsEnabled(true)
        model.chooseShowTimer(true)
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithText("Math Settings").performScrollTo().performClick()
        compose.onNodeWithText("Time limit per 5 questions").performScrollTo().assertIsDisplayed()
        listOf(30 to "30 seconds", 60 to "1 minute", 120 to "2 minutes",
            180 to "3 minutes", 240 to "4 minutes").forEach { (seconds, label) ->
            compose.onNodeWithTag("time-limit").performScrollTo().performClick()
            compose.onNodeWithText(label).performScrollTo().performClick()
            compose.onNodeWithTag("time-limit").assertTextEquals(label)
            assertEquals(seconds, model.timeLimitSeconds)
            assertEquals(seconds, MathViewModel(application).timeLimitSeconds)
        }
        compose.onNodeWithTag("time-limit").performScrollTo().performClick()
        (5..60 step 5).forEach { minutes ->
            compose.onNodeWithText("$minutes minutes").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("60 minutes").performClick()
        assertEquals(3_600, model.timeLimitSeconds)
        compose.onNodeWithTag("time-limit").performClick()
        compose.onNodeWithText("None").performScrollTo().performClick()
        assertEquals(0, model.timeLimitSeconds)
    }

    @Test fun adminOffersPersistentRewardSwitchesForEveryTierEvenWithRewardsOff() {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Main.immediate)
        model.chooseRewardsEnabled(false)
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Debug logging").performScrollTo().performClick()
        val description = compose.onNodeWithTag("debug-logging-description").performScrollTo()
        repeat(7) { description.performClick() }
        compose.onNodeWithText("Admin").assertIsDisplayed()
        RewardTier.entries.forEach { tier ->
            compose.onNodeWithText("${tier.label} · ${tier.questionCountLabel}").performScrollTo().assertIsDisplayed()
            RewardType.entries.filter { it.tier == tier }.forEach { type ->
                compose.onNodeWithContentDescription("${type.label} reward")
                    .performScrollTo().assertIsOn().performClick().assertIsOff()
            }
        }
        assertEquals(RewardType.entries.toSet(), model.disabledRewards)
        assertEquals(model.disabledRewards, MathViewModel(application).disabledRewards)
        compose.onNodeWithText("Close").performClick()
        repeat(7) { description.performClick() }
        RewardType.entries.forEach { type ->
            compose.onNodeWithContentDescription("${type.label} reward")
                .performScrollTo().assertIsOff().performClick().assertIsOn()
        }
        assertTrue(MathViewModel(application).disabledRewards.isEmpty())
        compose.onNodeWithText("Close").performClick()
        assertFalse(model.rewardsEnabled)
        assertFalse(DebugLog.enabled.value)
        assertTrue(model.pokemons.isEmpty())
        assertEquals(Overlay.SETTINGS, model.overlay)
    }

    @Test fun resultShowsAwardAndItsHistoricalBalance() {
        val result = PracticeResult(1, 1, Operation.ADDITION, 10, 1_000,
            List(50) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
            prizeType = RewardType.VIDEO_GAME,
            prize = PrizeAward(RewardType.VIDEO_GAME, RewardBalance(whole = 1, fragments = 0)))
        compose.setContent {
            MathTheme { ResultsContent(result, "Results", showCorrectAnswers = true) {} }
        }
        compose.onNodeWithText("Prize: Video Game Fragment").assertIsDisplayed()
        compose.onNodeWithText("Total collected: 3 fragments").assertIsDisplayed()
        compose.onNodeWithText("1 Video Games · 0/3 fragments").assertIsDisplayed()
    }

    @Test fun adminCanCancelOrRemoveEitherUnitForEveryRewardWithEarningDisabled() {
        val file = File(application.filesDir, "practice_history.json")
        val store = HistoryStore(file)
        RewardType.entries.forEachIndexed { index, type ->
            repeat(5) {
                val result = PracticeResult(index * 5 + it + 1L, index * 5 + it + 1L,
                    Operation.ADDITION, 10, 1_000,
                    List(50) { Attempt(Problem(1, 1, Operation.ADDITION), 2) }, prizeType = type)
                store.add(result)
                store.claimReward(result.id)
            }
        }
        store.collectPokemon(Celebration.PIKACHU)
        val before = store.loadSnapshot()
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Main.immediate)
        model.chooseRewardsEnabled(false)
        RewardType.entries.forEach { model.chooseRewardEnabled(it, false) }
        model.openSettings()
        compose.setContent { MathAppContent(model) }
        compose.onNodeWithText("Remove rewards").assertDoesNotExist()
        compose.onNodeWithText("Debug logging").performScrollTo().performClick()
        val description = compose.onNodeWithTag("debug-logging-description").performScrollTo()
        repeat(7) { description.performClick() }
        compose.onNodeWithText("Remove rewards").performScrollTo().performClick()
        RewardTier.entries.forEach { tier ->
            compose.onNodeWithText(tier.label).performScrollTo().assertIsDisplayed()
            RewardType.entries.filter { it.tier == tier }.forEach { type ->
                compose.onNodeWithContentDescription("Remove 1 ${type.label} fragment")
                    .performScrollTo().performClick()
                compose.onNodeWithText("Permanently remove 1 ${type.label} fragment?").assertIsDisplayed()
                compose.onNodeWithText("Cancel").performClick()
                assertEquals(RewardBalance(1, 2), model.rewardBalances[type])
                compose.onNodeWithContentDescription("Remove 1 ${type.label} fragment")
                    .performScrollTo().performClick()
                compose.onNodeWithText("Remove").performClick()
                assertEquals(RewardBalance(1, 1), model.rewardBalances[type])
                compose.onNodeWithContentDescription("Remove 1 whole ${type.label} reward")
                    .performScrollTo().performClick()
                compose.onNodeWithText("Permanently remove 1 whole ${type.label} reward?").assertIsDisplayed()
                compose.onNodeWithText("Remove").performClick()
                compose.onNodeWithContentDescription("Remove 1 whole ${type.label} reward")
                    .performScrollTo().assertIsNotEnabled()
                compose.onNodeWithTag("remove-reward-${type.name}").onChildren()
                    .filterToOne(hasText("Whole: 0 · Fragments: 1/3")).assertExists()
                assertEquals(RewardBalance(0, 1), model.rewardBalances[type])
            }
        }
        assertEquals(before.copy(rewards = model.rewardBalances), HistoryStore(file).loadSnapshot())
        compose.onNodeWithText("Back to admin").performClick()
        compose.onNodeWithText("Admin").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        assertEquals(Overlay.SETTINGS, model.overlay)
        assertFalse(DebugLog.enabled.value)
        assertFalse(model.rewardsEnabled)
        assertEquals(RewardType.entries.toSet(), model.disabledRewards)
    }

    @Test fun emptyRemovalInventoryDisablesBothUnitsForEveryReward() {
        compose.setContent {
            MathTheme {
                RemoveRewardsDialog(emptyMap(), false, null, { _, _ -> fail("Nothing can be removed") }, {})
            }
        }
        compose.onNodeWithText("No rewards or fragments available.").assertIsDisplayed()
        RewardType.entries.forEach { type ->
            compose.onNodeWithContentDescription("Remove 1 ${type.label} fragment")
                .performScrollTo().assertIsNotEnabled()
            compose.onNodeWithContentDescription("Remove 1 whole ${type.label} reward")
                .performScrollTo().assertIsNotEnabled()
        }
    }

    @Test fun removalShowsSaveErrorsAllowsRetryAndDisablesControlsWhileSaving() {
        val saving = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        var requests = 0
        compose.setContent {
            MathTheme {
                RemoveRewardsDialog(mapOf(RewardType.LOLLIPOP to RewardBalance(1, 1)), saving.value, error.value,
                    { _, _ -> requests++; saving.value = true; error.value = null }, {})
            }
        }
        compose.onNodeWithContentDescription("Remove 1 Lollipop fragment").performScrollTo().performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithText("Removing reward…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove 1 Lollipop fragment").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Remove 1 whole Lollipop reward").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {
            saving.value = false
            error.value = "Unable to remove this reward. Free device storage and try again."
        }
        compose.onNodeWithText(error.value!!).assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove 1 Lollipop fragment").performScrollTo().performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.runOnIdle { assertEquals(2, requests) }
    }

    @Test fun giftFollowsCelebrationAndUnopenedPrizeCanBeClaimedFromHistory() {
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Main.immediate)
        assertTrue(model.rewardsEnabled)
        model.openRewardsGame()
        model.updateRewardsQuestionCount("50")
        model.selectRewardsGame(Operation.ADDITION)
        model.start()
        compose.mainClock.autoAdvance = false
        compose.setContent { MathAppContent(model) }
        compose.runOnIdle {
            repeat(50) {
                model.updateAnswer(model.game!!.problem.answer.toString())
                model.submitAnswer()
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("You answered every question!").assertIsDisplayed()
        compose.onNodeWithTag("gift-box").assertDoesNotExist()
        compose.onNodeWithTag("view-results").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Congratulations! You've answered 100% correct! Tap to get a prize!").assertIsDisplayed()
        compose.onNodeWithTag("gift-box").assertIsDisplayed()
        compose.runOnIdle {
            model.dismissReward()
            model.done()
            model.openSettings()
            model.openHistory()
            model.showHistoryDetail(model.history.single())
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Open gift box").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("gift-box").performClick()
        compose.mainClock.advanceTimeBy(2_000)
        val type = model.history.single().prizeType!!
        compose.runOnIdle {
            assertEquals(RewardTier.TIER_2, type.tier)
            assertEquals(RewardBalance(fragments = 1), model.rewardBalances[type])
            assertNotNull(model.history.single().prize)
            model.dismissReward()
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Prize: ${type.fragmentLabel}").assertIsDisplayed()
        compose.onNodeWithText("Total collected: 1 fragments").assertIsDisplayed()
        compose.onNodeWithText("Open gift box").assertDoesNotExist()
    }
}
