package com.ssmath.app

import android.content.SharedPreferences

internal val timeLimitOptionsSeconds = listOf(0, 5, 10, 11, 12, 13, 14, 15, 20, 30, 45) +
    ((1..4) + (5..15 step 5)).map { it * 60 }

internal fun normalizeTimeLimitSeconds(value: Int): Int =
    timeLimitOptionsSeconds.last { it <= value.coerceAtLeast(0) }

internal fun SharedPreferences.readTimeLimitSeconds(): Int {
    val seconds = if (contains("time_limit_seconds")) getInt("time_limit_seconds", 0)
    else getInt("time_limit_minutes", 0).coerceIn(0, 15) * 60
    return normalizeTimeLimitSeconds(seconds)
}

internal fun SharedPreferences.readReadingTimeLimitSeconds(): Int {
    if (!contains("reading_time_limit_seconds")) {
        // Snapshot the formerly shared setting before either subject can change it.
        val legacySeconds = if (getBoolean("show_timer", false)) readTimeLimitSeconds() else 0
        edit().putInt("reading_time_limit_seconds", legacySeconds).apply()
    }
    return normalizeTimeLimitSeconds(getInt("reading_time_limit_seconds", 0))
}
