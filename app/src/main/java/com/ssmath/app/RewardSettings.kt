package com.ssmath.app

import android.content.SharedPreferences

enum class MathRewardGame(val title: String) {
    BASIC("Challenge- Basic Math"),
    LOGIC("Challenge - Logic Math"),
    MATRIX("Multiplication Matrix Rewards Game")
}

internal fun SharedPreferences.readReadingRewardsEnabled(game: ReadingGame): Boolean =
    getBoolean(Subject.READING.rewardsPreference, true) &&
        game.name !in getStringSet("disabled_reading_game_rewards", emptySet()).orEmpty()

internal fun SharedPreferences.readRewardThresholds(): RewardThresholds = try {
    val tier1 = getInt("reward_tier_1_minimum", 25)
    val tier2 = getInt("reward_tier_2_minimum", 50)
    RewardThresholds(tier1, tier2, getInt("reward_tier_3_minimum", maxOf(100, tier2 + 1)))
} catch (_: IllegalArgumentException) {
    RewardThresholds()
} catch (_: ClassCastException) {
    RewardThresholds()
}

internal fun SharedPreferences.readTier3RewardChances(): Tier3RewardChances = try {
    Tier3RewardChances(getInt("reward_tier_3_toys_1_percentage", 34),
        getInt("reward_tier_3_toys_2_percentage", 33), getInt("reward_tier_3_toys_3_percentage", 33))
} catch (_: IllegalArgumentException) {
    Tier3RewardChances()
} catch (_: ClassCastException) {
    Tier3RewardChances()
}

internal fun SharedPreferences.readMatrixRewardTier(): RewardTier = try {
    RewardTier.entries.find { it.name == getString("matrix_reward_tier", null) } ?: RewardTier.TIER_2
} catch (_: ClassCastException) {
    RewardTier.TIER_2
}
