package com.ssmath.app

import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
enum class RewardTier(val label: String) {
    TIER_1("Tier 1"),
    TIER_2("Tier 2");

    val questionCountLabel: String get() = RewardThresholds().questionCountLabel(this)
}

@Serializable
data class RewardThresholds(val tier1Minimum: Int = 25, val tier2Minimum: Int = 50) {
    init {
        require(tier1Minimum in 1..1000 && tier2Minimum in 1..1000 && tier1Minimum < tier2Minimum)
    }

    fun minimum(tier: RewardTier): Int = when (tier) {
        RewardTier.TIER_1 -> tier1Minimum
        RewardTier.TIER_2 -> tier2Minimum
    }

    fun questionCountLabel(tier: RewardTier): String = when (tier) {
        RewardTier.TIER_1 -> "$tier1Minimum–${tier2Minimum - 1} questions"
        RewardTier.TIER_2 -> "$tier2Minimum or more questions"
    }

    fun tierForQuestionCount(count: Int): RewardTier? = when {
        count >= tier2Minimum -> RewardTier.TIER_2
        count >= tier1Minimum -> RewardTier.TIER_1
        else -> null
    }
}

internal fun rewardTierForQuestionCount(questionCount: Int): RewardTier? =
    RewardThresholds().tierForQuestionCount(questionCount)

internal fun rewardTierForGame(questionCount: Int, rewardThresholds: RewardThresholds,
                               matrixRewardTier: RewardTier?): RewardTier? =
    if (matrixRewardTier == null) rewardThresholds.tierForQuestionCount(questionCount)
    else matrixRewardTier.takeIf { questionCount == MultiplicationMatrix.QUESTION_COUNT }

@Serializable
enum class RewardType(val label: String, val pluralLabel: String, val fragmentLabel: String, val tier: RewardTier) {
    LOLLIPOP("Lollipop", "Lollipops", "Lollipop Fragment", RewardTier.TIER_1),
    ICE_CREAM("Ice Cream Cone", "Ice Cream Cones", "Ice Cream Cone Fragment", RewardTier.TIER_1),
    GUMMI_BEAR("Gummi Bear", "Gummi Bears", "Gummi Bear Fragment", RewardTier.TIER_1),
    RAMEN("Ramen", "Ramen", "Ramen Fragment", RewardTier.TIER_1),
    VIDEO_GAME("Video Game", "Video Games", "Video Game Fragment", RewardTier.TIER_2),
    BED_TIME("Bed Time", "Bed Time", "Bed Time Fragment", RewardTier.TIER_1),
    RESTAURANT("Restaurant", "Restaurants", "Restaurant Fragment", RewardTier.TIER_2)
}

@Serializable
data class RewardBalance(val whole: Int = 0, val fragments: Int = 0) {
    init {
        require(whole in 0..(Int.MAX_VALUE - 2) / 3 && fragments in 0..2)
    }

    val totalFragments: Int get() = whole * 3 + fragments

    fun addFragment(): RewardBalance {
        val total = Math.addExact(totalFragments, 1)
        return RewardBalance(total / 3, total % 3)
    }

    fun removeFragments(count: Int): RewardBalance {
        require(count >= 0)
        val total = (totalFragments - count).coerceAtLeast(0)
        return RewardBalance(total / 3, total % 3)
    }
}

/** The inventory balance immediately after this result's fragment was claimed. */
@Serializable
data class PrizeAward(val type: RewardType, val balance: RewardBalance)

internal fun qualifiesForReward(questionCount: Int, correct: Int, answered: Int, timedOut: Boolean,
                                rewardThresholds: RewardThresholds = RewardThresholds(),
                                matrixRewardTier: RewardTier? = null): Boolean =
    !timedOut && rewardTierForGame(questionCount, rewardThresholds, matrixRewardTier) != null &&
        correct in 0..answered &&
        answered == questionCount && correct.toLong() * 100 > questionCount.toLong() * 90

fun selectPrize(
    questionCount: Int,
    correct: Int,
    answered: Int,
    enabledAtStart: Boolean,
    enabledAtFinish: Boolean,
    timedOut: Boolean = false,
    random: Random = Random.Default,
    disabledRewards: Set<RewardType> = emptySet(),
    rewardThresholds: RewardThresholds = RewardThresholds(),
    matrixRewardTier: RewardTier? = null
): RewardType? {
    if (!enabledAtStart || !enabledAtFinish ||
        !qualifiesForReward(questionCount, correct, answered, timedOut, rewardThresholds, matrixRewardTier)) return null
    val tier = rewardTierForGame(questionCount, rewardThresholds, matrixRewardTier) ?: return null
    val prizes = RewardType.entries.filter { it.tier == tier && it !in disabledRewards }
    return prizes.singleOrNull() ?: prizes.randomOrNull(random)
}
