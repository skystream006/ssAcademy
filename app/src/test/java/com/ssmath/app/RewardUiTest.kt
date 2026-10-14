package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
class RewardUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun controlAnimationClock() {
        compose.mainClock.autoAdvance = false
    }

    @Test fun allRewardIllustrationsExposeAccurateImageDescriptions() {
        compose.setContent {
            MathTheme {
                Column {
                    RewardType.entries.forEach { type ->
                        Row {
                            RewardImage(type, false, Modifier.size(90.dp))
                            RewardImage(type, true, Modifier.size(90.dp))
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        RewardType.entries.forEach { type ->
            compose.onNodeWithContentDescription(type.label).assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            compose.onNodeWithContentDescription(rewardImageDescription(type, true)).assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        }
        compose.onNodeWithContentDescription("Ice Cream Cone").assertExists()
        compose.onNodeWithContentDescription("One third of an ice cream cone").assertExists()
        compose.onNodeWithContentDescription("One third of a video game controller").assertExists()
        compose.onNodeWithContentDescription("One third of a bedtime scene").assertExists()
        compose.onNodeWithContentDescription("One third of a restaurant").assertExists()
        compose.onAllNodesWithText("1/3").assertCountEquals(RewardType.entries.size)
    }

    @Test fun giftIsAnAccessibleButtonAndRapidTapsOnlyOpenOnce() {
        var opens = 0
        compose.setContent {
            MathTheme { RewardGiftDialog(result(), false, null, { opens++ }, {}) }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Congratulations! You've answered 96.67% correct! Tap to get a prize!")
            .assertIsDisplayed()
        val gift = compose.onNodeWithTag("gift-box")
        gift.assertContentDescriptionEquals("Open gift box")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
        gift.performClick()
        gift.performClick()
        compose.mainClock.advanceTimeBy(REWARD_OPEN_DURATION_MS.toLong() + 64)
        gift.assertIsNotEnabled()
        compose.onNodeWithText("Saving your prize…").assertIsDisplayed()
        compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun anEligibleFractionalScoreIsNotTruncatedToNinetyPercent() =
        assertScoreText(901, 1_000, "90.1")

    @Test fun anEligibleScoreJustAboveNinetyKeepsTwoDecimalPlaces() =
        assertScoreText(892, 991, "90.01")

    @Test fun readingGiftUsesTheSameClaimAnimationAndResetsForANewSession() {
        var saved by mutableStateOf(ReadingRewardResult("reading-1", "reader", ReadingGame.MISSING,
            ReadingLevel.SPROUT, 1, 30, 29, 30, prizeType = RewardType.LOLLIPOP))
        var opens = 0
        compose.setContent {
            MathTheme { RewardGiftDialog(saved, false, null, { opens++ }, {}) }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Congratulations! You've answered 96.67% correct! Tap to get a prize!")
            .assertIsDisplayed()
        repeat(2) { compose.onNodeWithTag("gift-box").performClick() }
        compose.runOnIdle {
            assertEquals(1, opens)
            saved = saved.copy(prize = PrizeAward(RewardType.LOLLIPOP, RewardBalance(1, 0)))
        }
        compose.mainClock.advanceTimeBy(REWARD_OPEN_DURATION_MS.toLong() + 64)
        compose.onNodeWithTag("awarded-fragment").assertIsDisplayed()
        compose.onNodeWithTag("awarded-balance").assertTextEquals("Whole: 1 · Fragments: 0/3")
        compose.onNodeWithTag("gift-box").assertHasNoClickAction()
        compose.runOnIdle { saved = saved.copy(sessionId = "reading-2", prize = null) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(128)
        compose.onNodeWithTag("gift-box").assertContentDescriptionEquals("Open gift box").assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, opens) }
    }

    @Test fun claimingDisablesTheBoxWithoutPrematurelyRevealingAnAward() {
        var opens = 0
        compose.setContent {
            MathTheme { RewardGiftDialog(result(), true, null, { opens++ }, {}) }
        }
        compose.mainClock.advanceTimeBy(REWARD_OPEN_DURATION_MS.toLong() + 64)
        compose.onNodeWithTag("gift-box").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, opens) }
    }

    @Test fun successfulPersistenceRevealsTheFragmentAndUpdatedCarryBalancePermanently() {
        var saved by mutableStateOf(result())
        var claiming by mutableStateOf(false)
        var opens = 0
        var dismissed = false
        compose.setContent {
            MathTheme {
                RewardGiftDialog(saved, claiming, null, { opens++; claiming = true }, { dismissed = true })
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("gift-box").performClick()
        compose.runOnIdle {
            saved = saved.copy(prize = PrizeAward(RewardType.LOLLIPOP, RewardBalance(2, 0)))
            claiming = false
        }
        compose.mainClock.advanceTimeBy(REWARD_OPEN_DURATION_MS.toLong() + 64)
        compose.onNodeWithTag("awarded-fragment")
            .assertContentDescriptionEquals("One third of a lollipop").assertIsDisplayed()
        compose.onNodeWithText("Lollipop Fragment").assertIsDisplayed()
        compose.onNodeWithTag("awarded-balance").assertTextEquals("Whole: 2 · Fragments: 0/3")
        compose.onNodeWithTag("gift-box").assertHasNoClickAction()
        compose.mainClock.advanceTimeBy(12_000)
        compose.onNodeWithTag("awarded-fragment").assertIsDisplayed()
        compose.onNodeWithText("View results").performClick()
        compose.runOnIdle {
            assertEquals(1, opens)
            assertTrue(dismissed)
        }
    }

    @Test fun bedTimeAndRestaurantGiftsRevealTheirAnimatedFragmentsAfterSaving() {
        var saved by mutableStateOf(result())
        var opens = 0
        compose.setContent {
            MathTheme { RewardGiftDialog(saved, false, null, { opens++ }, {}) }
        }
        listOf(RewardType.BED_TIME to 25, RewardType.RESTAURANT to 50).forEachIndexed { index, (type, count) ->
            compose.runOnIdle {
                saved = result().copy(id = index.toLong(), prizeType = type, questionCount = count,
                    attempts = List(count) { Attempt(Problem(1, 1, Operation.ADDITION), 2) })
            }
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithTag("gift-box").performClick()
            compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
            compose.runOnIdle { saved = saved.copy(prize = PrizeAward(type, RewardBalance(0, 1))) }
            compose.mainClock.advanceTimeBy(REWARD_OPEN_DURATION_MS.toLong() + 64)
            compose.onNodeWithTag("awarded-fragment").assertIsDisplayed()
                .assertContentDescriptionEquals(rewardImageDescription(type, true))
            compose.onNodeWithText(type.fragmentLabel).assertIsDisplayed()
            compose.onNodeWithTag("awarded-balance").assertTextEquals("Whole: 0 · Fragments: 1/3")
            compose.mainClock.advanceTimeBy(8_000)
            compose.onNodeWithTag("awarded-fragment").assertIsDisplayed()
            compose.runOnIdle { assertEquals(index + 1, opens) }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun newRewardImagesAnimateOnlyWhileResumed() {
        compose.setContent {
            MathTheme {
                Column {
                    listOf(RewardType.BED_TIME, RewardType.RESTAURANT).forEach { type ->
                        Row {
                            RewardImage(type, false)
                            RewardImage(type, true)
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        fun pixels(): IntArray = compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            IntArray(image.width * image.height).also {
                image.getPixels(it, 0, image.width, 0, 0, image.width, image.height)
                image.recycle()
            }
        }
        val first = pixels()
        compose.mainClock.advanceTimeBy(800)
        assertFalse("Artwork animates on the Compose clock", first.contentEquals(pixels()))
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.mainClock.advanceTimeBy(64)
        val paused = pixels()
        compose.mainClock.advanceTimeBy(800)
        assertArrayEquals("Artwork is static in the background", paused, pixels())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(64)
        val resumed = pixels()
        compose.mainClock.advanceTimeBy(800)
        assertFalse("Artwork animates again after resuming", resumed.contentEquals(pixels()))
    }

    @Test fun aPersistenceErrorOffersOneRetryAndCanStillBeDismissed() {
        var claiming by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var opens = 0
        var dismissed = false
        compose.setContent {
            MathTheme {
                RewardGiftDialog(result(), claiming, error,
                    { opens++; claiming = true; error = null }, { dismissed = true })
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("gift-box").performClick()
        compose.runOnIdle {
            claiming = false
            error = "Couldn't save your prize. Please try again."
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText(error!!).assertIsDisplayed()
        compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithTag("gift-box").performClick()
        compose.runOnIdle { assertEquals(2, opens) }
        compose.onNodeWithText("Not now").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun repeatedImmediateFailuresWithTheSameMessageRemainRetryable() {
        var claiming by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var opens = 0
        val failure = "Couldn't save your prize. Please try again."
        compose.setContent {
            MathTheme {
                RewardGiftDialog(result(), claiming, error, {
                    opens++
                    claiming = true
                    error = null
                    claiming = false
                    error = failure
                }, {})
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("gift-box").performClick()
        compose.mainClock.advanceTimeBy(64)
        val retry = compose.onNodeWithText("Retry").assertIsEnabled()
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle {
            retry()
            retry()
            assertEquals("Rapid callbacks must not duplicate the second attempt", 2, opens)
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText(failure).assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsEnabled().performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Retry").assertIsEnabled()
        compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, opens) }
    }

    @Test fun anUnclaimedGiftCanBeDismissedWithoutRequestingAPrize() {
        var opens = 0
        var dismissed by mutableStateOf(false)
        compose.setContent {
            MathTheme {
                if (dismissed) Text("Results")
                else RewardGiftDialog(result(), false, null, { opens++ }, { dismissed = true })
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Not now").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Results").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, opens) }
    }

    @Test fun openingWaitsInTheBackgroundAndResumesWithoutLosingTheAward() {
        var saved by mutableStateOf(result())
        compose.setContent {
            MathTheme { RewardGiftDialog(saved, false, null, {}, {}) }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            saved = saved.copy(prize = PrizeAward(RewardType.LOLLIPOP, RewardBalance(0, 1)))
        }
        compose.mainClock.advanceTimeBy(200)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.mainClock.advanceTimeBy(4_000)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("awarded-fragment").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(REWARD_OPEN_DURATION_MS.toLong())
        compose.onNodeWithTag("awarded-fragment").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun inventoryDisplaysFourRewardsPerRow() {
        assertFourRewardsPerRow()
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun inventoryDisplaysFourRewardsPerRowOnNarrowScreens() {
        assertFourRewardsPerRow()
    }

    @Test
    @Config(qualifiers = "w800dp-h600dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun inventoryDisplaysFourRewardsPerRowOnWideScreens() {
        assertFourRewardsPerRow()
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun inventoryScrollsThroughAllWholeAndFragmentCountsAtDoubleTextSize() {
        val balances = RewardType.entries.mapIndexed { index, type ->
            type to RewardBalance(index + 1, index % 3)
        }.toMap()
        compose.setContent {
            MathTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    RewardInventory(balances)
                }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("3 fragments = 1 reward").assertIsDisplayed()
        compose.onNodeWithText("1/3").assertDoesNotExist()
        RewardTier.entries.forEach { tier ->
            compose.onNodeWithTag("reward-inventory").performScrollToKey(tier.name)
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithText(tier.label).assertIsDisplayed().assert(hasText(tier.questionCountLabel))
            listOf(tier.label, tier.questionCountLabel).forEach { text ->
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(text, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue(layouts.isNotEmpty())
                val layout = layouts.single()
                assertFalse("$text overflows horizontally: ${layout.size}, paragraph width ${layout.multiParagraph.width}",
                    layout.didOverflowWidth)
                assertFalse("$text overflows vertically: ${layout.size}, paragraph height ${layout.multiParagraph.height}",
                    layout.didOverflowHeight)
            }
        }
        RewardType.entries.forEach { type ->
            val balance = balances.getValue(type)
            compose.onNodeWithTag("reward-inventory").performScrollToKey(type.name)
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("inventory-whole-${type.name}").onChildren()
                .filterToOne(hasText("Whole: ${balance.whole}")).assertIsDisplayed()
            compose.onNodeWithTag("inventory-fragments-${type.name}").onChildren()
                .filterToOne(hasText("Fragments: ${balance.fragments}/3")).assertIsDisplayed()
            listOf(type.pluralLabel, "Whole: ${balance.whole}", "Fragments: ${balance.fragments}/3")
                .forEach { text ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("reward-card-${type.name}")))
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertTrue(layouts.isNotEmpty())
                    assertFalse("$text overflows", layouts.single().hasVisualOverflow)
                }
            compose.onNodeWithContentDescription(type.label).assertIsDisplayed()
            compose.onNodeWithContentDescription(rewardImageDescription(type, true)).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun giftRemainsReachableOnASmallScreenAtDoubleTextSize() {
        var opens = 0
        var dismissed = false
        compose.setContent {
            MathTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    RewardGiftDialog(result(), false, null, { opens++ }, { dismissed = true })
                }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("gift-box").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Not now").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, opens)
            assertTrue(dismissed)
        }
    }

    @Test fun emptyInventoryStillShowsEveryRewardWithZeroCounts() {
        compose.setContent { MathTheme { RewardInventory(emptyMap()) } }
        compose.mainClock.advanceTimeBy(64)
        RewardType.entries.forEach { type ->
            compose.onNodeWithTag("reward-inventory").performScrollToKey(type.name)
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("inventory-whole-${type.name}").onChildren()
                .filterToOne(hasText("Whole: 0")).assertIsDisplayed()
            compose.onNodeWithTag("inventory-fragments-${type.name}").onChildren()
                .filterToOne(hasText("Fragments: 0/3")).assertIsDisplayed()
        }
    }

    @Test fun usePickerShowsOnlyWholePicturesAndTheirAvailableCounts() {
        compose.mainClock.autoAdvance = true
        val balances = RewardType.entries.mapIndexed { index, type ->
            type to RewardBalance(index, 2)
        }.toMap()
        compose.setContent { MathTheme { UseRewardsDialog(balances, false, null, {}, {}) } }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("What reward would you like to use?").assertIsDisplayed()
        RewardTier.entries.forEach { tier ->
            compose.onNodeWithTag("use-rewards-grid").performScrollToKey(tier.name)
            compose.onNodeWithText(tier.label).assertIsDisplayed().assert(hasText(tier.questionCountLabel))
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            val header = compose.onNodeWithText(tier.label).fetchSemanticsNode().boundsInRoot
            val firstReward = RewardType.entries.first { it.tier == tier }
            val card = compose.onNodeWithTag("use-reward-${firstReward.name}").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            assertTrue(header.bottom <= card.top)
        }
        RewardType.entries.forEachIndexed { index, type ->
            compose.onNodeWithTag("use-rewards-grid").performScrollToKey(type.name)
            val card = compose.onNodeWithTag("use-reward-${type.name}")
                .assertIsDisplayed().assert(hasText("Whole: $index"))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            if (index == 0) card.assertIsNotEnabled() else card.assertIsEnabled()
            compose.onNodeWithContentDescription(type.label).assertIsDisplayed()
            compose.onNodeWithContentDescription(rewardImageDescription(type, true)).assertDoesNotExist()
        }
        compose.onNodeWithText("1/3").assertDoesNotExist()
    }

    @Test fun choosingAndDecliningARewardDoesNotUseIt() {
        compose.mainClock.autoAdvance = true
        var used = 0
        var dismissed = false
        compose.setContent {
            MathTheme {
                UseRewardsDialog(mapOf(RewardType.LOLLIPOP to RewardBalance(2, 1)), false, null,
                    { used++ }, { dismissed = true })
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithContentDescription("Lollipop").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("You would like to use 1 Lollipop?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, used) }
        compose.onNodeWithText("No").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("use-reward-LOLLIPOP").assert(hasText("Whole: 2"))
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertEquals(0, used)
            assertTrue(dismissed)
        }
    }

    @Test fun confirmingOnlyUsesOneRewardEvenWithDuplicateCallbacks() {
        compose.mainClock.autoAdvance = true
        var balances by mutableStateOf(mapOf(RewardType.LOLLIPOP to RewardBalance(1, 2)))
        var used = 0
        compose.setContent {
            MathTheme {
                UseRewardsDialog(balances, false, null, { type ->
                    assertEquals(RewardType.LOLLIPOP, type)
                    used++
                    balances = mapOf(type to RewardBalance(0, 2))
                }, {})
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithContentDescription("Lollipop").performClick()
        compose.mainClock.advanceTimeBy(64)
        val confirm = compose.onNodeWithText("Yes").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle {
            confirm()
            confirm()
            assertEquals(1, used)
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("No whole rewards available yet.").assertIsDisplayed()
        compose.onNodeWithTag("use-reward-LOLLIPOP").assertIsNotEnabled().assert(hasText("Whole: 0"))
        compose.onNodeWithText("Yes").assertDoesNotExist()
    }

    @Test fun emptyPickerCannotRequestAnyReward() {
        compose.mainClock.autoAdvance = true
        var used = 0
        compose.setContent { MathTheme { UseRewardsDialog(emptyMap(), false, null, { used++ }, {}) } }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("No whole rewards available yet.").assertIsDisplayed()
        RewardType.entries.forEach { type ->
            compose.onNodeWithTag("use-rewards-grid").performScrollToKey(type.name)
            compose.onNodeWithTag("use-reward-${type.name}")
                .assertIsNotEnabled().assert(hasText("Whole: 0")).performClick()
        }
        compose.onNodeWithText("Yes").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, used) }
    }

    @Test fun savingDisablesThePickerAndFailureAllowsAnotherConfirmation() {
        compose.mainClock.autoAdvance = true
        var saving by mutableStateOf(true)
        var error by mutableStateOf<String?>(null)
        var used = 0
        compose.setContent {
            MathTheme {
                UseRewardsDialog(mapOf(RewardType.LOLLIPOP to RewardBalance(1, 2)), saving, error,
                    { used++ }, {})
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Saving your reward…").assertIsDisplayed()
        compose.onNodeWithTag("use-reward-LOLLIPOP").assertIsNotEnabled().performClick()
        compose.runOnIdle {
            assertEquals(0, used)
            saving = false
            error = "Unable to use this reward. Free device storage and try again."
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText(error!!).assertIsDisplayed()
        compose.onNodeWithTag("use-reward-LOLLIPOP").assertIsEnabled().performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Yes").performClick()
        compose.runOnIdle { assertEquals(1, used) }
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun usePickerScrollsToEveryRewardAtDoubleTextSize() {
        compose.mainClock.autoAdvance = true
        compose.setContent {
            MathTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    UseRewardsDialog(RewardType.entries.associateWith { RewardBalance(1, 2) }, false, null, {}, {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        RewardType.entries.forEach { type ->
            compose.onNodeWithTag("use-rewards-grid").performScrollToKey(type.name)
            compose.onNodeWithTag("use-reward-${type.name}").assertIsDisplayed().performClick()
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithText("You would like to use 1 ${type.label}?").assertIsDisplayed()
            compose.onNodeWithText("Yes").assertIsDisplayed()
            compose.onNodeWithText("No").performClick()
            compose.mainClock.advanceTimeBy(64)
        }
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    private fun assertFourRewardsPerRow() {
        compose.setContent { MathTheme { RewardInventory(emptyMap()) } }
        compose.mainClock.advanceTimeBy(64)
        val grid = compose.onNodeWithTag("reward-inventory").fetchSemanticsNode().boundsInRoot
        val firstRow = RewardType.entries.take(4).map { type ->
            compose.onNodeWithTag("reward-card-${type.name}").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
        }
        firstRow.forEach { card ->
            assertEquals(firstRow.first().top, card.top, 1f)
            assertEquals(firstRow.first().width, card.width, 1f)
            assertTrue(card.left >= grid.left && card.right <= grid.right)
        }
        firstRow.zipWithNext().forEach { (left, right) ->
            assertTrue(left.right < right.left)
        }
        val header = compose.onNodeWithTag("reward-inventory-header").fetchSemanticsNode().boundsInRoot
        assertTrue(header.bottom <= firstRow.first().top)
        assertEquals(firstRow.first().left, header.left, 1f)
        assertEquals(firstRow.last().right, header.right, 1f)
        val tierOne = compose.onNodeWithText("Tier 1").assertIsDisplayed()
            .assert(hasText("25–49 questions"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNode().boundsInRoot
        assertTrue(header.bottom <= tierOne.top)
        assertTrue(tierOne.bottom <= firstRow.first().top)
        assertEquals(header.left, tierOne.left, 1f)
        assertEquals(header.right, tierOne.right, 1f)
        compose.onNodeWithTag("reward-inventory").performScrollToKey(RewardTier.TIER_2.name)
        compose.mainClock.advanceTimeByFrame()
        val tierTwo = compose.onNodeWithText("Tier 2").assertIsDisplayed()
            .assert(hasText("50 or more questions"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNode().boundsInRoot
        val nextRow = compose.onNodeWithTag("reward-card-${RewardType.entries[4].name}")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(firstRow.first().left, nextRow.left, 1f)
        assertTrue(tierTwo.bottom <= nextRow.top)
        assertEquals(header.left, tierTwo.left, 1f)
        assertEquals(header.right, tierTwo.right, 1f)
    }

    private fun assertScoreText(correct: Int, count: Int, expected: String) {
        val fractional = result().copy(
            attempts = List(count) { Attempt(Problem(1, 1, Operation.ADDITION), if (it < correct) 2 else 3) },
            questionCount = count,
            prizeType = RewardType.VIDEO_GAME
        )
        compose.setContent {
            MathTheme { RewardGiftDialog(fractional, false, null, {}, {}) }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Congratulations! You've answered $expected% correct! Tap to get a prize!")
            .assertIsDisplayed()
    }

    private fun result() = PracticeResult(
        id = 7, finishedAt = 7, operation = Operation.ADDITION, maximum = 10, durationMs = 1_000,
        attempts = List(30) { Attempt(Problem(1, 1, Operation.ADDITION), if (it == 0) 3 else 2) },
        prizeType = RewardType.LOLLIPOP
    )
}
