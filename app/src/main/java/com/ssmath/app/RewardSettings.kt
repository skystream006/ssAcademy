package com.ssmath.app

import android.content.SharedPreferences
import android.util.Log

enum class MathRewardGame(val title: String) {
    BASIC("Challenge- Basic Math"),
    LOGIC("Challenge - Logic Math"),
    MATRIX("Multiplication Matrix Rewards Game")
}

internal fun SharedPreferences.readReadingRewardsEnabled(game: ReadingGame): Boolean =
    getBoolean(Subject.READING.rewardsPreference, true) &&
        game.name !in getStringSet("disabled_reading_game_rewards", emptySet()).orEmpty()

internal val MathRewardGame.rewardPreferencePrefix: String get() = "math_${name.lowercase()}_reward"
internal val ReadingGame.rewardPreferencePrefix: String get() = "reading_${name.lowercase()}_reward"

internal fun SharedPreferences.readRewardThresholds(game: MathRewardGame): RewardThresholds =
    readActivityRewardThresholds(game.rewardPreferencePrefix)

internal fun SharedPreferences.readRewardThresholds(game: ReadingGame): RewardThresholds =
    readActivityRewardThresholds(game.rewardPreferencePrefix)

private fun SharedPreferences.readActivityRewardThresholds(prefix: String): RewardThresholds {
    // Unedited activities inherit the old shared settings without changing saved results.
    val legacy = readRewardThresholds()
    if ((1..3).none { contains("${prefix}_tier_${it}_minimum") }) return legacy
    return try {
        RewardThresholds(getInt("${prefix}_tier_1_minimum", legacy.tier1Minimum),
            getInt("${prefix}_tier_2_minimum", legacy.tier2Minimum),
            getInt("${prefix}_tier_3_minimum", legacy.tier3Minimum))
    } catch (error: IllegalArgumentException) {
        Log.w("RewardSettings", "Invalid minimum questions for $prefix; using previous shared settings.", error)
        legacy
    } catch (error: ClassCastException) {
        Log.w("RewardSettings", "Invalid minimum question preference type for $prefix; using previous shared settings.", error)
        legacy
    }
}

internal fun SharedPreferences.saveRewardThresholds(prefix: String, thresholds: RewardThresholds) {
    edit().putInt("${prefix}_tier_1_minimum", thresholds.tier1Minimum)
        .putInt("${prefix}_tier_2_minimum", thresholds.tier2Minimum)
        .putInt("${prefix}_tier_3_minimum", thresholds.tier3Minimum).apply()
}

internal fun SharedPreferences.readRewardThresholds(): RewardThresholds = try {
    val tier1 = getInt("reward_tier_1_minimum", 25)
    val tier2 = getInt("reward_tier_2_minimum", 50)
    RewardThresholds(tier1, tier2, getInt("reward_tier_3_minimum", maxOf(100, tier2 + 1)))
} catch (_: IllegalArgumentException) {
    RewardThresholds()
} catch (_: ClassCastException) {
    RewardThresholds()
}

internal fun SharedPreferences.readTier3RewardChances(): Tier3RewardChances {
    if (listOf("reward_tier_3_toys_1_percentage", "reward_tier_3_toys_2_percentage",
        "reward_tier_3_toys_3_percentage", "reward_tier_3_good_behavior_point_percentage").none { contains(it) }) {
        return Tier3RewardChances()
    }
    return try {
        // Missing values in saved three-toy settings retain their original defaults.
        Tier3RewardChances(getInt("reward_tier_3_toys_1_percentage", 34),
            getInt("reward_tier_3_toys_2_percentage", 33), getInt("reward_tier_3_toys_3_percentage", 33),
            getInt("reward_tier_3_good_behavior_point_percentage", 0))
    } catch (error: IllegalArgumentException) {
        Log.w("RewardSettings", "Invalid Tier 3 reward chances; using defaults.", error)
        Tier3RewardChances()
    } catch (error: ClassCastException) {
        Log.w("RewardSettings", "Invalid Tier 3 reward chance preference type; using defaults.", error)
        Tier3RewardChances()
    }
}

internal fun SharedPreferences.readMatrixRewardTier(): RewardTier = try {
    RewardTier.entries.find { it.name == getString("matrix_reward_tier", null) } ?: RewardTier.TIER_2
} catch (_: ClassCastException) {
    RewardTier.TIER_2
}
