package com.ssmath.app

import kotlin.math.pow
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class RewardTier(val label: String) {
    TIER_1("Tier 1"),
    TIER_2("Tier 2"),
    TIER_3("Tier 3");

    val questionCountLabel: String get() = RewardThresholds().questionCountLabel(this)
}

@Serializable
data class RewardThresholds(
    val tier1Minimum: Int = 25,
    val tier2Minimum: Int = 50,
    val tier3Minimum: Int = maxOf(100, tier2Minimum + 1)
) {
    init {
        require(tier1Minimum in 1..1000 && tier2Minimum in 1..1000 && tier3Minimum in 1..1001 &&
            tier1Minimum < tier2Minimum && tier2Minimum < tier3Minimum)
    }

    fun minimum(tier: RewardTier): Int = when (tier) {
        RewardTier.TIER_1 -> tier1Minimum
        RewardTier.TIER_2 -> tier2Minimum
        RewardTier.TIER_3 -> tier3Minimum
    }

    fun questionCountLabel(tier: RewardTier): String = when (tier) {
        RewardTier.TIER_1 -> "$tier1Minimum–${tier2Minimum - 1} questions"
        RewardTier.TIER_2 -> if (tier3Minimum == 1001) "$tier2Minimum or more questions"
            else "$tier2Minimum–${tier3Minimum - 1} questions"
        RewardTier.TIER_3 -> "$tier3Minimum or more questions"
    }

    fun tierForQuestionCount(count: Int): RewardTier? = when {
        count >= tier3Minimum -> RewardTier.TIER_3
        count >= tier2Minimum -> RewardTier.TIER_2
        count >= tier1Minimum -> RewardTier.TIER_1
        else -> null
    }
}

/** Missing historical rules predate Tier 3; do not reinterpret an already awarded prize. */
internal object HistoricalRewardThresholdsSerializer :
    JsonTransformingSerializer<RewardThresholds>(RewardThresholds.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if ("tier3Minimum" in element.jsonObject) element
        else JsonObject(element.jsonObject + ("tier3Minimum" to JsonPrimitive(1001)))

    override fun transformSerialize(element: JsonElement): JsonElement =
        if ("tier3Minimum" in element.jsonObject) element
        else JsonObject(element.jsonObject + ("tier3Minimum" to JsonPrimitive(
            maxOf(100, (element.jsonObject["tier2Minimum"]?.jsonPrimitive?.int ?: 50) + 1))))
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
    RESTAURANT("Restaurant", "Restaurants", "Restaurant Fragment", RewardTier.TIER_2),
    TOYS_1("$1 Toys", "$1 Toys", "$1 Toys Fragment", RewardTier.TIER_3),
    TOYS_2("$2 Toys", "$2 Toys", "$2 Toys Fragment", RewardTier.TIER_3),
    TOYS_3("$3 Toys", "$3 Toys", "$3 Toys Fragment", RewardTier.TIER_3),
    GOOD_BEHAVIOR_POINT("1 Good Behavior Point", "Good Behavior Points",
        "1 Good Behavior Point Fragment", RewardTier.TIER_3)
}

@Serializable
data class Tier3RewardChances(
    val toys1: Int,
    val toys2: Int,
    val toys3: Int,
    val goodBehaviorPoint: Int = 0
) {
    constructor() : this(35, 25, 10, 30)

    init {
        require(toys1 in 0..100 && toys2 in 0..100 && toys3 in 0..100 && goodBehaviorPoint in 0..100 &&
            toys1 + toys2 + toys3 + goodBehaviorPoint == 100)
    }

    fun percentage(type: RewardType): Int = when (type) {
        RewardType.TOYS_1 -> toys1
        RewardType.TOYS_2 -> toys2
        RewardType.TOYS_3 -> toys3
        RewardType.GOOD_BEHAVIOR_POINT -> goodBehaviorPoint
        else -> 0
    }
}

@Serializable
data class RewardBalance(val whole: Int = 0, val fragments: Int = 0) {
    init {
        require(whole in 0..(Int.MAX_VALUE - 2) / 3 && fragments in 0..2)
    }

    val totalFragments: Int get() = whole * 3 + fragments
    val canAddWhole: Boolean get() = whole < (Int.MAX_VALUE - 2) / 3
    val canAddFragment: Boolean get() = canAddWhole || fragments < 2

    fun addFragment(): RewardBalance {
        require(canAddFragment) { "The reward balance limit has been reached." }
        val total = Math.addExact(totalFragments, 1)
        return RewardBalance(total / 3, total % 3)
    }

    fun addWhole(): RewardBalance {
        require(canAddWhole) { "The reward balance limit has been reached." }
        return copy(whole = whole + 1)
    }

    fun removeFragments(count: Int): RewardBalance {
        require(count >= 0)
        val total = (totalFragments - count).coerceAtLeast(0)
        return RewardBalance(total / 3, total % 3)
    }
}

/** The inventory balance immediately after this result's fragment was claimed. */
@Serializable
data class PrizeAward(
    val type: RewardType,
    val balance: RewardBalance,
    val deductOnHistoryDeletion: Boolean = true
)

@Serializable
data class SlotMachineRewardPool(val tier: RewardTier, val weights: Map<RewardType, Int>) {
    init {
        require(weights.isNotEmpty() && weights.values.all { it in 1..100 })
    }

    val winChance: Double get() = weights.keys.sumOf(::winChance)

    fun reelChance(type: RewardType): Double =
        (weights[type] ?: 0).toDouble() / weights.values.sum()

    fun winChance(type: RewardType): Double = reelChance(type).pow(3)

    fun prizeChance(type: RewardType): Double = winChance(type) / winChance

    internal fun clawBallReward(ballId: Int): RewardType {
        require(ballId >= 0)
        return weights.keys.elementAt(ballId % weights.size)
    }

    internal fun select(random: Random): RewardType {
        if (tier != RewardTier.TIER_3) weights.keys.singleOrNull()?.let { return it }
        var ticket = random.nextInt(weights.values.sum())
        return weights.entries.first {
            ticket -= it.value
            ticket < 0
        }.key
    }
}

internal fun slotMachineRewardPool(
    tier: RewardTier,
    disabledRewards: Set<RewardType>,
    tier3RewardChances: Tier3RewardChances
): SlotMachineRewardPool? {
    val weights = availableRewardsForTier(tier, disabledRewards, tier3RewardChances).associateWith {
        if (tier == RewardTier.TIER_3) tier3RewardChances.percentage(it) else 1
    }
    return weights.takeIf { it.isNotEmpty() }?.let { SlotMachineRewardPool(tier, it) }
}

internal fun firstRewardToThree(draws: List<RewardType>): RewardType? {
    val counts = mutableMapOf<RewardType, Int>()
    draws.forEach { type ->
        val count = counts.getOrDefault(type, 0) + 1
        if (count == 3) return type
        counts[type] = count
    }
    return null
}

internal fun qualifiesForReward(questionCount: Int, correct: Int, answered: Int, timedOut: Boolean,
                                rewardThresholds: RewardThresholds = RewardThresholds(),
                                matrixRewardTier: RewardTier? = null): Boolean =
    !timedOut && rewardTierForGame(questionCount, rewardThresholds, matrixRewardTier) != null &&
        correct in 0..answered &&
        answered == questionCount && correct.toLong() * 100 >= questionCount.toLong() * 90

internal fun completionSlotReward(
    questionCount: Int,
    correct: Int,
    answered: Int,
    enabledAtStart: Boolean,
    enabledAtFinish: Boolean,
    timedOut: Boolean = false,
    disabledRewards: Set<RewardType> = emptySet(),
    rewardThresholds: RewardThresholds = RewardThresholds(),
    matrixRewardTier: RewardTier? = null,
    tier3RewardChances: Tier3RewardChances = Tier3RewardChances()
): SlotMachineRewardPool? {
    if (!enabledAtStart || !enabledAtFinish ||
        !qualifiesForReward(questionCount, correct, answered, timedOut, rewardThresholds, matrixRewardTier)) return null
    val tier = rewardTierForGame(questionCount, rewardThresholds, matrixRewardTier) ?: return null
    return slotMachineRewardPool(tier, disabledRewards, tier3RewardChances)
}

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
    matrixRewardTier: RewardTier? = null,
    tier3RewardChances: Tier3RewardChances = Tier3RewardChances()
): RewardType? {
    if (!enabledAtStart || !enabledAtFinish ||
        !qualifiesForReward(questionCount, correct, answered, timedOut, rewardThresholds, matrixRewardTier)) return null
    val tier = rewardTierForGame(questionCount, rewardThresholds, matrixRewardTier) ?: return null
    return selectRewardFromTier(tier, random, disabledRewards, tier3RewardChances)
}

internal fun availableRewardsForTier(
    tier: RewardTier,
    disabledRewards: Set<RewardType>,
    tier3RewardChances: Tier3RewardChances
): List<RewardType> = RewardType.entries.filter {
    it.tier == tier && it !in disabledRewards &&
        (tier != RewardTier.TIER_3 || tier3RewardChances.percentage(it) > 0)
}

internal fun selectRewardFromTier(
    tier: RewardTier,
    random: Random,
    disabledRewards: Set<RewardType>,
    tier3RewardChances: Tier3RewardChances
): RewardType? = slotMachineRewardPool(tier, disabledRewards, tier3RewardChances)?.select(random)
