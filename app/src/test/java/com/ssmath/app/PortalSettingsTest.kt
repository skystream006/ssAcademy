package com.ssmath.app

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PortalSettingsTest {
    private fun snapshot(text: String) = Json.decodeFromString(JsonObject.serializer(), text)

    @Test fun settingsRoundTripNormalizeAndReplaceOnlyPortablePreferences() {
        val prefs = ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("portable-test", 0)
        prefs.edit().clear().putString("admin_pin_hash", "private").putBoolean("admin_pin_lock_enabled", false)
            .putString("game_name", "Local").putInt("text_size_percent", 180).commit()
        val remote = snapshot("""{"theme":"pink","disabled_rewards":["TOYS_3","TOYS_1","TOYS_3"],"show_timer":true}""")
        PortalSettings.replace(prefs, remote)
        assertEquals(snapshot("""{"disabled_rewards":["TOYS_1","TOYS_3"],"show_timer":true,"theme":"pink"}"""),
            PortalSettings.read(prefs))
        assertEquals(listOf("disabled_rewards", "show_timer", "theme"), PortalSettings.read(prefs).keys.toList())
        assertFalse(prefs.contains("text_size_percent"))
        assertEquals("private", prefs.getString("admin_pin_hash", null))
        assertFalse(prefs.getBoolean("admin_pin_lock_enabled", true))
        assertEquals("Local", prefs.getString("game_name", null))
        PortalSettings.replace(prefs, snapshot("{}"))
        assertEquals(snapshot("{}"), PortalSettings.read(prefs))
        assertEquals("private", prefs.getString("admin_pin_hash", null))
    }

    @Test fun invalidSettingsAndCrossFieldCombinationsAreRejected() {
        val invalid = listOf(
            """{"admin_pin_lock_enabled":false}""", """{"bearerToken":"secret"}""",
            """{"game_name":"Learner"}""", """{"debug_logging":true}""",
            """{"show_timer":1}""", """{"minimum":"1"}""", """{"minimum":0}""",
            """{"minimum":20,"maximum":10}""", """{"text_size_percent":201}""",
            """{"theme":"GREEN"}""", """{"theme":"green\u0000"}""", """{"mode":null}""",
            """{"disabled_rewards":["UNKNOWN"]}""", """{"question_count":1001}""",
            """{"time_limit_seconds":901}""", """{"reward_tier_1_minimum":70}""",
            """{"math_basic_reward_tier_2_minimum":20}""",
            """{"reward_tier_3_toys_1_percentage":1}""", """{"logic_maximum_pictures":6}""",
            """{"logic_rewards_MULTIPLICATION_first":20,"logic_rewards_MULTIPLICATION_second":10}"""
        )
        invalid.forEach { text ->
            assertThrows(text, IllegalArgumentException::class.java) { PortalSettings.validate(snapshot(text)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            PortalSettings.validate(snapshot("""{"disabled_rewards":[${List(257) { "\"TOYS_1\"" }.joinToString()}]}"""))
        }
        PortalSettings.validate(snapshot("""{"reward_tier_1_minimum":999,"reward_tier_2_minimum":1000,
            "reward_tier_3_minimum":1001,"division_maximum_first":10000,"division_maximum_second":1,
            "rewards_MULTIPLICATION_maximum_first":10000,"rewards_MULTIPLICATION_maximum_second":1}"""))
    }

    @Test fun failedPreferenceCommitRestoresPreviousOverridesAndReportsFailure() {
        val prefs = ApplicationProvider.getApplicationContext<Application>().getSharedPreferences("failed-portable-test", 0)
        prefs.edit().clear().putInt("text_size_percent", 150).putString("admin_pin_hash", "private").commit()
        var fail = true
        val failing = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val editor = prefs.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit(): Boolean {
                        val saved = editor.commit()
                        return if (fail) { fail = false; false } else saved
                    }
                }
            }
        }
        assertThrows(IOException::class.java) {
            PortalSettings.replace(failing, snapshot("""{"text_size_percent":120}"""))
        }
        assertEquals(150, prefs.getInt("text_size_percent", 0))
        assertEquals("private", prefs.getString("admin_pin_hash", null))
    }
}
