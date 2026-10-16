package com.ssmath.app

import android.content.SharedPreferences

internal val timeLimitOptionsSeconds = listOf(0, 30) + ((1..4) + (5..15 step 5)).map { it * 60 }

internal fun normalizeTimeLimitSeconds(value: Int): Int =
    timeLimitOptionsSeconds.last { it <= value.coerceAtLeast(0) }

internal fun SharedPreferences.readTimeLimitSeconds(): Int {
    val seconds = if (contains("time_limit_seconds")) getInt("time_limit_seconds", 0)
    else getInt("time_limit_minutes", 0).coerceIn(0, 15) * 60
    return normalizeTimeLimitSeconds(seconds)
}
