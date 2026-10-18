package com.ssmath.app

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AdminPinStoreTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val preferences get() = application.getSharedPreferences("settings", 0)

    @Before fun setup() { preferences.edit().clear().commit() }

    @Test fun validatesDigitsAndLengthIncludingLeadingZeros() {
        listOf("", "123", "123456789", "12a4", " 1234", "１２３４").forEach {
            assertFalse(AdminPinStore.isValid(it))
        }
        listOf("0000", "01234567").forEach { assertTrue(AdminPinStore.isValid(it)) }
        assertThrows(IllegalArgumentException::class.java) { AdminPinStore(preferences).create("123") }
        assertFalse(AdminPinStore(preferences).hasPin)
    }

    @Test fun savesOnlySaltedHashAndReloadsWithoutChangingOtherSettings() {
        preferences.edit().putBoolean("show_timer", true).commit()
        val store = AdminPinStore(preferences)
        assertFalse(store.hasPin)
        store.create("0482")
        assertTrue(AdminPinStore(preferences).hasPin)
        assertEquals(AdminPinStore.Result.INCORRECT, AdminPinStore(preferences).verify("0483"))
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("0482"))
        assertTrue(preferences.getBoolean("show_timer", false))
        assertFalse(preferences.all.values.contains("0482"))
        val other = application.getSharedPreferences("other-pin-test", 0)
        other.edit().clear().commit()
        AdminPinStore(other).create("0482")
        assertNotEquals(preferences.getString("admin_pin_salt", null), other.getString("admin_pin_salt", null))
        assertNotEquals(preferences.getString("admin_pin_hash", null), other.getString("admin_pin_hash", null))
        assertThrows(IllegalStateException::class.java) { store.create("9999") }
        assertEquals(AdminPinStore.Result.UNLOCKED, store.verify("0482"))
    }

    @Test fun failedAttemptsAndTemporaryLockPersistAcrossReopening() {
        var time = 1_000L
        val store = AdminPinStore(preferences) { time }
        store.create("4826")
        repeat(4) { assertEquals(AdminPinStore.Result.INCORRECT, store.verify("9999")) }
        assertEquals(AdminPinStore.Result.LOCKED, store.verify("9999"))
        assertEquals(AdminPinStore.Result.LOCKED, AdminPinStore(preferences) { time }.verify("4826"))
        time += 30_000
        assertEquals(AdminPinStore.Result.UNLOCKED, store.verify("4826"))
        assertEquals(0, preferences.getInt("admin_pin_failures", -1))
        assertEquals(AdminPinStore.Result.INCORRECT, store.verify("9999"))
    }

    @Test fun corruptOrPartialCredentialsNeverAllowCreatingAReplacement() {
        preferences.edit().putString("admin_pin_salt", "broken").commit()
        val store = AdminPinStore(preferences)
        assertTrue(store.hasPin)
        assertThrows(IllegalStateException::class.java) { store.create("4826") }
        assertThrows(IllegalArgumentException::class.java) { store.verify("4826") }
    }

    @Test fun failedSaveNeverReportsSuccessfulCreation() {
        val failing = object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putString(key: String?, value: String?) = apply { editor.putString(key, value) }
                    override fun putInt(key: String?, value: Int) = apply { editor.putInt(key, value) }
                    override fun putLong(key: String?, value: Long) = apply { editor.putLong(key, value) }
                    override fun commit() = false
                }
            }
        }
        assertThrows(IllegalStateException::class.java) { AdminPinStore(failing).create("4826") }
        assertFalse(AdminPinStore(preferences).hasPin)
    }
}
