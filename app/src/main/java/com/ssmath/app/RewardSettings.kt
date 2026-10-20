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
    RewardThresholds(getInt("reward_tier_1_minimum", 25), getInt("reward_tier_2_minimum", 50))
} catch (_: IllegalArgumentException) {
    RewardThresholds()
} catch (_: ClassCastException) {
    RewardThresholds()
}

internal fun SharedPreferences.readMatrixRewardTier(): RewardTier = try {
    RewardTier.entries.find { it.name == getString("matrix_reward_tier", null) } ?: RewardTier.TIER_2
} catch (_: ClassCastException) {
    RewardTier.TIER_2
}
