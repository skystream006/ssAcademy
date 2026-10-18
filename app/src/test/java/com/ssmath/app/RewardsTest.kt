package com.ssmath.app

import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RewardsTest {
    private fun prize(count: Int, correct: Int = count, answered: Int = count,
        atStart: Boolean = true, atFinish: Boolean = true, timedOut: Boolean = false, seed: Int = 1,
        disabledRewards: Set<RewardType> = emptySet()) =
        selectPrize(count, correct, answered, atStart, atFinish, timedOut, Random(seed), disabledRewards)

    @Test fun configurableThresholdsValidateAndMapEveryBoundary() {
        listOf(RewardThresholds(), RewardThresholds(15, 25), RewardThresholds(1, 2),
            RewardThresholds(999, 1000)).forEach { thresholds ->
            assertNull(thresholds.tierForQuestionCount(Int.MIN_VALUE))
            assertNull(thresholds.tierForQuestionCount(thresholds.tier1Minimum - 1))
            assertEquals(RewardTier.TIER_1, thresholds.tierForQuestionCount(thresholds.tier1Minimum))
            assertEquals(RewardTier.TIER_1, thresholds.tierForQuestionCount(thresholds.tier2Minimum - 1))
            assertEquals(RewardTier.TIER_2, thresholds.tierForQuestionCount(thresholds.tier2Minimum))
            assertEquals(RewardTier.TIER_2, thresholds.tierForQuestionCount(Int.MAX_VALUE))
            assertEquals(thresholds.tier1Minimum, thresholds.minimum(RewardTier.TIER_1))
            assertEquals(thresholds.tier2Minimum, thresholds.minimum(RewardTier.TIER_2))
            assertEquals(thresholds, Json.decodeFromString(RewardThresholds.serializer(),
                Json.encodeToString(RewardThresholds.serializer(), thresholds)))
        }
        assertEquals("15–24 questions", RewardThresholds(15, 25).questionCountLabel(RewardTier.TIER_1))
        assertEquals("25 or more questions", RewardThresholds(15, 25).questionCountLabel(RewardTier.TIER_2))
        listOf(0 to 50, -1 to 50, 25 to 25, 50 to 25, 25 to 0, 25 to 1001,
            1000 to 1001, Int.MIN_VALUE to Int.MAX_VALUE).forEach { (first, second) ->
            assertThrows(IllegalArgumentException::class.java) { RewardThresholds(first, second) }
        }
        RewardTier.entries.forEach {
            assertEquals("\"${it.name}\"", Json.encodeToString(RewardTier.serializer(), it))
            assertEquals(it, Json.decodeFromString(RewardTier.serializer(), "\"${it.name}\""))
        }
    }

    @Test fun changedThresholdsPreserveAccuracyCompletionAndEnabledGuards() {
        val thresholds = RewardThresholds(15, 25)
        listOf(14, 15, 24, 25, 50).forEach { count ->
            assertEquals(thresholds.tierForQuestionCount(count),
                selectPrize(count, count, count, true, true, rewardThresholds = thresholds)?.tier)
            assertNull(selectPrize(count, count - 1, count - 1, true, true, rewardThresholds = thresholds))
            assertNull(selectPrize(count, count, count, false, true, rewardThresholds = thresholds))
            assertNull(selectPrize(count, count, count, true, false, rewardThresholds = thresholds))
            assertNull(selectPrize(count, count, count, true, true, timedOut = true, rewardThresholds = thresholds))
            assertNull(selectPrize(count, count, count, true, true, rewardThresholds = thresholds,
                disabledRewards = RewardType.entries.filter { it.tier == thresholds.tierForQuestionCount(count) }.toSet()))
        }
        assertNull(selectPrize(20, 18, 20, true, true, rewardThresholds = thresholds))
        assertNotNull(selectPrize(20, 19, 20, true, true, rewardThresholds = thresholds))
    }

    @Test fun selectedMatrixTierIgnoresMinimaButNeverQualificationOrAvailability() {
        for (thresholds in listOf(RewardThresholds(15, 25), RewardThresholds(200, 300))) {
            for (tier in RewardTier.entries) {
                fun matrix(correct: Int = 109, answered: Int = 121, count: Int = 121,
                    start: Boolean = true, finish: Boolean = true, timeout: Boolean = false,
                    disabled: Set<RewardType> = emptySet()) =
                    selectPrize(count, correct, answered, start, finish, timeout,
                        disabledRewards = disabled, rewardThresholds = thresholds, matrixRewardTier = tier)
                assertEquals(tier, matrix()?.tier)
                assertNull(matrix(correct = 108))
                assertNull(matrix(correct = 120, answered = 120))
                assertNull(matrix(count = 120, correct = 120, answered = 120))
                assertNull(matrix(correct = 122))
                assertNull(matrix(start = false))
                assertNull(matrix(finish = false))
                assertNull(matrix(timeout = true))
                assertNull(matrix(disabled = RewardType.entries.filter { it.tier == tier }.toSet()))
                val only = RewardType.entries.first { it.tier == tier }
                assertEquals(only, matrix(disabled = RewardType.entries.toSet() - only))
            }
        }
    }

    @Test fun questionCountsMapToNamedTiersAtTheBoundaries() {
        listOf(Int.MIN_VALUE, 0, 1, 24).forEach { assertNull(rewardTierForQuestionCount(it)) }
        (25..49).forEach { assertEquals(RewardTier.TIER_1, rewardTierForQuestionCount(it)) }
        listOf(50, 51, 100, 1000, Int.MAX_VALUE).forEach {
            assertEquals(RewardTier.TIER_2, rewardTierForQuestionCount(it))
        }
        assertEquals("Tier 1", RewardTier.TIER_1.label)
        assertEquals("25–49 questions", RewardTier.TIER_1.questionCountLabel)
        assertEquals("Tier 2", RewardTier.TIER_2.label)
        assertEquals("50 or more questions", RewardTier.TIER_2.questionCountLabel)
    }

    @Test fun rewardTypesBelongToTheirAssignedTier() {
        assertEquals(
            setOf(RewardType.LOLLIPOP, RewardType.ICE_CREAM, RewardType.GUMMI_BEAR, RewardType.RAMEN,
                RewardType.BED_TIME),
            RewardType.entries.filter { it.tier == RewardTier.TIER_1 }.toSet()
        )
        assertEquals(setOf(RewardType.VIDEO_GAME, RewardType.RESTAURANT),
            RewardType.entries.filter { it.tier == RewardTier.TIER_2 }.toSet())
    }

    @Test fun tierMetadataDoesNotChangeStoredRewardNames() {
        RewardType.entries.forEach { type ->
            val stored = "\"${type.name}\""
            assertEquals(stored, Json.encodeToString(RewardType.serializer(), type))
            assertEquals(type, Json.decodeFromString(RewardType.serializer(), stored))
        }
    }

    @Test fun countBoundariesSelectTheRightCategory() {
        listOf(1, 24).forEach { assertNull(prize(it)) }
        listOf(25, 26, 49).forEach { count ->
            val selected = (1..100).map { prize(count, seed = it) }.toSet()
            assertEquals(RewardType.entries.filter { it.tier == RewardTier.TIER_1 }.toSet(), selected)
            assertTrue(selected.all { it?.tier == RewardTier.TIER_1 })
        }
        listOf(50, 51, 100, 1000).forEach { count ->
            val selected = (1..100).map { prize(count, seed = it) }.toSet()
            assertEquals(RewardType.entries.filter { it.tier == RewardTier.TIER_2 }.toSet(), selected)
            assertTrue(selected.all { it?.tier == RewardTier.TIER_2 })
        }
    }

    @Test fun accuracyMustBeStrictlyOverNinetyPercentWithoutRounding() {
        assertNull(prize(30, 27))
        assertNull(prize(50, 45))
        assertNull(prize(100, 90))
        assertNotNull(prize(100, 91))
        assertNotNull(prize(25, 23))
        assertNull(prize(25, 22))
        assertNotNull(prize(26, 24))
        assertNull(prize(26, 23))
        assertNotNull(prize(49, 45))
        assertNull(prize(49, 44))
        assertNotNull(prize(50, 46))
    }

    @Test fun selectionUsesOnlyEnabledRewardsFromTheEligibleTier() {
        listOf(25, 49, 50, 1000).forEach { count ->
            val tierRewards = RewardType.entries.filter { it.tier == rewardTierForQuestionCount(count) }.toSet()
            tierRewards.forEach { disabled ->
                val selected = (0..200).map { prize(count, seed = it, disabledRewards = setOf(disabled)) }.toSet()
                assertEquals(tierRewards - disabled, selected)
            }
            tierRewards.forEach { onlyEnabled ->
                repeat(10) {
                    assertEquals(onlyEnabled, prize(count, seed = it,
                        disabledRewards = RewardType.entries.toSet() - onlyEnabled))
                }
            }
        }
    }

    @Test fun disablingAnEntireTierGivesNoPrizeAndNeverFallsBackToAnotherTier() {
        RewardTier.entries.forEach { tier ->
            val disabled = RewardType.entries.filter { it.tier == tier }.toSet()
            listOf(25, 49, 50, 1000).forEach { count ->
                if (rewardTierForQuestionCount(count) == tier) {
                    assertNull(prize(count, disabledRewards = disabled))
                } else {
                    assertEquals(rewardTierForQuestionCount(count), prize(count, disabledRewards = disabled)?.tier)
                }
                assertNull(prize(count, disabledRewards = RewardType.entries.toSet()))
            }
        }
    }

    @Test fun rewardsRequireBothSettingsChecksEveryQuestionAndNoTimeout() {
        listOf(25, 26, 50).forEach { count ->
            assertNull(prize(count, atStart = false))
            assertNull(prize(count, atFinish = false))
            assertNull(prize(count, atStart = false, atFinish = false))
            assertNull(prize(count, correct = count - 1, answered = count - 1))
            assertNull(prize(count, correct = count + 1, answered = count + 1))
            assertNull(prize(count, timedOut = true))
        }
    }

    @Test fun everyThirdFragmentBecomesAWholeRewardWithZeroRemainder() {
        var balance = RewardBalance()
        repeat(9) {
            balance = balance.addFragment()
            assertEquals(it + 1, balance.totalFragments)
            assertEquals((it + 1) / 3, balance.whole)
            assertEquals((it + 1) % 3, balance.fragments)
        }
        assertEquals(RewardBalance(3, 0), balance)
    }

    @Test fun addingWholeRewardsPreservesEveryFragmentRemainder() {
        (0..2).forEach { fragments ->
            val balance = RewardBalance(2, fragments)
            assertTrue(balance.canAddWhole)
            assertTrue(balance.canAddFragment)
            assertEquals(RewardBalance(3, fragments), balance.addWhole())
            assertEquals(balance.totalFragments + 3, balance.addWhole().totalFragments)
        }
        assertEquals(RewardBalance(1, 0), RewardBalance().addWhole())
    }

    @Test fun additionLimitsAllowSafeCarriesAndRejectOverflowWithoutChangingBalances() {
        val maximumWhole = (Int.MAX_VALUE - 2) / 3
        (0..2).forEach { fragments ->
            val nearLimit = RewardBalance(maximumWhole - 1, fragments)
            assertTrue(nearLimit.canAddWhole)
            assertTrue(nearLimit.canAddFragment)
            assertEquals(RewardBalance(maximumWhole, fragments), nearLimit.addWhole())
            val limit = RewardBalance(maximumWhole, fragments)
            assertFalse(limit.canAddWhole)
            assertEquals(fragments < 2, limit.canAddFragment)
            assertThrows(IllegalArgumentException::class.java) { limit.addWhole() }
            if (fragments < 2) assertEquals(RewardBalance(maximumWhole, fragments + 1), limit.addFragment())
            else assertThrows(IllegalArgumentException::class.java) { limit.addFragment() }
            assertEquals(maximumWhole * 3 + fragments, limit.totalFragments)
        }
        assertEquals(RewardBalance(maximumWhole, 0), RewardBalance(maximumWhole - 1, 2).addFragment())
    }

    @Test fun invalidBalancesAreRejectedRatherThanLosingInventory() {
        listOf(-1 to 0, 0 to -1, 0 to 3, Int.MAX_VALUE to 0).forEach { (whole, fragments) ->
            assertThrows(IllegalArgumentException::class.java) { RewardBalance(whole, fragments) }
        }
    }

    @Test fun removingFragmentsUnpacksWholeRewardsAndClampsSpentBalancesAtZero() {
        assertEquals(RewardBalance(0, 2), RewardBalance(1, 0).removeFragments(1))
        assertEquals(RewardBalance(1, 1), RewardBalance(2, 2).removeFragments(4))
        assertEquals(RewardBalance(), RewardBalance().removeFragments(1))
        assertEquals(RewardBalance(), RewardBalance(1, 0).removeFragments(Int.MAX_VALUE))
        assertEquals(RewardBalance(2, 2), RewardBalance(2, 2).removeFragments(0))
        assertThrows(IllegalArgumentException::class.java) { RewardBalance().removeFragments(-1) }
    }

    @Test fun rewardTypesHaveSingularPluralAndFragmentLabels() {
        assertEquals("Ice Cream Cone", RewardType.ICE_CREAM.label)
        assertEquals("Ice Cream Cones", RewardType.ICE_CREAM.pluralLabel)
        assertEquals("Ice Cream Cone Fragment", RewardType.ICE_CREAM.fragmentLabel)
        assertEquals("Bed Time", RewardType.BED_TIME.label)
        assertEquals("Restaurant", RewardType.RESTAURANT.label)
        RewardType.entries.forEach {
            assertTrue(it.label.isNotBlank())
            assertTrue(it.pluralLabel.isNotBlank())
            assertEquals("${it.label} Fragment", it.fragmentLabel)
        }
    }
}
