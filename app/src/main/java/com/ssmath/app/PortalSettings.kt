package com.ssmath.app

import android.content.SharedPreferences
import java.io.IOException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/** Only portable preferences: never identity, security, diagnostics, or activity data. */
internal object PortalSettings {
    private data class Rule(val range: IntRange? = null, val values: Set<String>? = null, val set: Boolean = false)
    private val rules = buildMap {
        listOf("slot_machine_fragment_reward", "show_timer", "math_pause_in_background",
            "rewards_enabled", "reading_rewards_enabled", "science_rewards_enabled", "advanced_rewards_enabled",
            "show_submitted_answer", "show_correct_answers", "vertical_equations", "randomize_matrix_questions",
            "wave_appearance", "skins_enabled").forEach { put(it, Rule()) }
        listOf("minimum", "maximum", "division_maximum_first", "division_maximum_second",
            "multiplication_maximum_first", "multiplication_maximum_second").forEach {
            put(it, Rule(MIN_MAXIMUM..MAX_MAXIMUM))
        }
        put("question_count", Rule(MIN_QUESTION_COUNT..MAX_QUESTION_COUNT))
        put("text_size_percent", Rule(MIN_TEXT_SIZE_PERCENT..MAX_TEXT_SIZE_PERCENT))
        put("time_limit_minutes", Rule(0..15))
        listOf("time_limit_seconds", "matrix_time_limit_seconds", "logic_time_limit_seconds",
            "reading_time_limit_seconds", "science_time_limit_seconds", "advanced_time_limit_seconds").forEach {
            put(it, Rule(0..900))
        }
        put("operation", Rule(values = Operation.entries.map { it.name }.toSet()))
        put("theme", Rule(values = COLOR_THEMES.map { it.first }.toSet()))
        put("mode", Rule(values = setOf("dark", "light")))
        put("skin", Rule(values = AppSkin.entries.map { it.name }.toSet()))
        put("matrix_reward_tier", Rule(values = RewardTier.entries.map { it.name }.toSet()))
        put("disabled_rewards", Rule(values = RewardType.entries.map { it.name }.toSet(), set = true))
        put("disabled_math_game_rewards", Rule(values = MathRewardGame.entries.map { it.name }.toSet(), set = true))
        listOf("disabled_reading_games", "disabled_reading_game_rewards").forEach {
            put(it, Rule(values = ReadingGame.entries.map { it.name }.toSet(), set = true))
        }
        (listOf("reward") + MathRewardGame.entries.map { it.rewardPreferencePrefix } +
            ReadingGame.entries.map { it.rewardPreferencePrefix }).forEach { prefix ->
            (1..3).forEach { put("${prefix}_tier_${it}_minimum", Rule(1..if (it == 3) 1001 else 1000)) }
        }
        listOf("toys_1", "toys_2", "toys_3", "good_behavior_point").forEach {
            put("reward_tier_3_${it}_percentage", Rule(0..100))
        }
        listOf("logic_minimum_pictures", "logic_maximum_pictures").forEach { put(it, Rule(2..6)) }
        listOf("logic_minimum_length", "logic_maximum_length").forEach { put(it, Rule(2..8)) }
        Operation.entries.forEach { operation ->
            listOf("rewards_${operation.name}_", "logic_rewards_${operation.name}_").forEach { prefix ->
                listOf("first", "second").forEach { put("$prefix$it", Rule(MIN_MAXIMUM..MAX_MAXIMUM)) }
            }
        }
        listOf("first", "second").forEach { put("rewards_MULTIPLICATION_maximum_$it", Rule(1..10000)) }
    }
    fun contains(key: String?) = key != null && key in rules

    fun validate(input: JsonObject): JsonObject {
        require(input.size <= 128 && input.toString().toByteArray(Charsets.UTF_8).size <= 64 * 1024)
        val result = JsonObject(input.toSortedMap().mapValues { (key, value) ->
            val rule = requireNotNull(rules[key]) { "Unsupported portable setting." }
            if (rule.set) {
                val array = requireNotNull(value as? JsonArray)
                require(array.size <= 256)
                JsonArray(array.map {
                    val text = requireNotNull(it as? JsonPrimitive)
                    require(text.isString && validString(text.content) && text.content in requireNotNull(rule.values))
                    text.content
                }.distinct().sorted().map(::JsonPrimitive))
            } else {
                val primitive = requireNotNull(value as? JsonPrimitive)
                when {
                    rule.range != null -> require(!primitive.isString && primitive.intOrNull in rule.range)
                    rule.values != null -> require(primitive.isString && validString(primitive.content) &&
                        primitive.content in rule.values)
                    else -> require(!primitive.isString && primitive.booleanOrNull != null)
                }
                primitive
            }
        })
        fun number(key: String, fallback: Int) = (result[key] as? JsonPrimitive)?.intOrNull ?: fallback
        require(number("minimum", 1) <= number("maximum", 10))
        val legacy = RewardThresholds(number("reward_tier_1_minimum", 25), number("reward_tier_2_minimum", 50),
            number("reward_tier_3_minimum", maxOf(100, number("reward_tier_2_minimum", 50) + 1)))
        (MathRewardGame.entries.map { it.rewardPreferencePrefix } +
            ReadingGame.entries.map { it.rewardPreferencePrefix }).forEach { prefix ->
            RewardThresholds(number("${prefix}_tier_1_minimum", legacy.tier1Minimum),
                number("${prefix}_tier_2_minimum", legacy.tier2Minimum),
                number("${prefix}_tier_3_minimum", legacy.tier3Minimum))
        }
        val chanceKeys = listOf("toys_1", "toys_2", "toys_3", "good_behavior_point")
            .map { "reward_tier_3_${it}_percentage" }
        if (chanceKeys.any { it in result }) Tier3RewardChances(number(chanceKeys[0], 34),
            number(chanceKeys[1], 33), number(chanceKeys[2], 33), number(chanceKeys[3], 0))
        require(LogicMathSettings(number("logic_minimum_pictures", 2), number("logic_maximum_pictures", 3),
            number("logic_minimum_length", 3), number("logic_maximum_length", 4)).isValid)
        Operation.entries.forEach { operation ->
            val prefix = "rewards_${operation.name}_"
            val first = number("${prefix}first", if (operation == Operation.DIVISION)
                number("division_maximum_first", number("maximum", 10)) else number("minimum", 1))
            val second = number("${prefix}second", if (operation == Operation.DIVISION)
                number("division_maximum_second", number("maximum", 10)) else number("maximum", 10))
            require(RewardsGameDefaults(first.toString(), second.toString()).isValid(operation, isLogicMath = true))
            val logicPrefix = "logic_rewards_${operation.name}_"
            require(RewardsGameDefaults(number("${logicPrefix}first", first).toString(),
                number("${logicPrefix}second", second).toString()).isValid(operation, isLogicMath = true))
        }
        return result
    }

    private fun validString(value: String) = value.length <= 128 && value.none(Char::isISOControl)

    fun read(preferences: SharedPreferences): JsonObject = validate(JsonObject(
        preferences.all.filterKeys(::contains).mapValues { (_, value) ->
            when (value) {
                is Boolean -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Set<*> -> JsonArray(value.map { JsonPrimitive(requireNotNull(it as? String)) })
                else -> throw IllegalArgumentException("Invalid portable preference type.")
            }
        }
    ))

    fun replace(preferences: SharedPreferences, snapshot: JsonObject) {
        val normalized = validate(snapshot)
        val previous = read(preferences)
        fun write(values: JsonObject): Boolean {
            val editor = preferences.edit()
            rules.keys.forEach(editor::remove)
            values.forEach { (key, value) ->
                when {
                    value is JsonArray -> editor.putStringSet(key, value.map { (it as JsonPrimitive).content }.toSet())
                    rules.getValue(key).range != null -> editor.putInt(key, (value as JsonPrimitive).intOrNull!!)
                    rules.getValue(key).values != null -> editor.putString(key, (value as JsonPrimitive).content)
                    else -> editor.putBoolean(key, (value as JsonPrimitive).booleanOrNull!!)
                }
            }
            return editor.commit()
        }
        if (!write(normalized)) {
            write(previous)
            throw IOException("Unable to save portable settings.")
        }
    }
}
