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

    @Test fun changeRotatesCredentialsAndPersistsLeadingZerosWithoutChangingOtherSettings() {
        preferences.edit().putBoolean("show_timer", true).putInt("reward_points", 42).commit()
        val store = AdminPinStore(preferences)
        store.create("0482")
        val previousSalt = preferences.getString("admin_pin_salt", null)
        val previousHash = preferences.getString("admin_pin_hash", null)

        assertEquals(AdminPinStore.Result.UNLOCKED, store.change("0482", "00123456"))

        assertNotEquals(previousSalt, preferences.getString("admin_pin_salt", null))
        assertNotEquals(previousHash, preferences.getString("admin_pin_hash", null))
        assertFalse(preferences.all.values.contains("0482"))
        assertFalse(preferences.all.values.contains("00123456"))
        assertTrue(preferences.getBoolean("show_timer", false))
        assertEquals(42, preferences.getInt("reward_points", 0))
        val reopened = AdminPinStore(application.getSharedPreferences("settings", 0))
        assertEquals(AdminPinStore.Result.INCORRECT, reopened.verify("0482"))
        assertEquals(AdminPinStore.Result.UNLOCKED, reopened.verify("00123456"))
    }

    @Test fun invalidNewPinsLeaveCredentialsAndFailedAttemptProtectionUntouched() {
        val store = AdminPinStore(preferences)
        store.create("0482")
        assertEquals(AdminPinStore.Result.INCORRECT, store.verify("1111"))
        val previous = preferences.all.toMap()

        listOf("", "123", "123456789", "12a4", " 1234", "１２３４", "1234\n").forEach { pin ->
            assertThrows(IllegalArgumentException::class.java) { store.change("0482", pin) }
            assertEquals(previous, preferences.all)
        }

        assertEquals(AdminPinStore.Result.UNLOCKED, store.verify("0482"))
    }

    @Test fun changingPinSharesPersistentWrongAttemptLockoutWithVerification() {
        var time = 1_000L
        val store = AdminPinStore(preferences) { time }
        store.create("0482")
        val previousSalt = preferences.getString("admin_pin_salt", null)
        val previousHash = preferences.getString("admin_pin_hash", null)
        repeat(2) { assertEquals(AdminPinStore.Result.INCORRECT, store.verify("1111")) }
        repeat(2) {
            assertEquals(AdminPinStore.Result.INCORRECT, AdminPinStore(preferences) { time }.change("1111", "0000"))
        }
        assertEquals(AdminPinStore.Result.LOCKED, store.change("1111", "0000"))
        assertEquals(AdminPinStore.Result.LOCKED, AdminPinStore(preferences) { time }.verify("0482"))
        assertEquals(AdminPinStore.Result.LOCKED, AdminPinStore(preferences) { time }.change("0482", "0000"))
        assertEquals(previousSalt, preferences.getString("admin_pin_salt", null))
        assertEquals(previousHash, preferences.getString("admin_pin_hash", null))

        time += 30_000
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences) { time }.change("0482", "0000"))
        assertEquals(0, preferences.getInt("admin_pin_failures", -1))
        assertEquals(0L, preferences.getLong("admin_pin_locked_until", -1))
        assertEquals(AdminPinStore.Result.INCORRECT, store.verify("0482"))
        assertEquals(AdminPinStore.Result.UNLOCKED, store.verify("0000"))
    }

    @Test fun malformedCurrentPinIsAnIncorrectAttemptWithoutReplacingCredentials() {
        val store = AdminPinStore(preferences)
        store.create("0482")
        val previousHash = preferences.getString("admin_pin_hash", null)
        assertEquals(AdminPinStore.Result.INCORRECT, store.change("０４８２", "0000"))
        assertEquals(1, preferences.getInt("admin_pin_failures", -1))
        assertEquals(previousHash, preferences.getString("admin_pin_hash", null))
        assertEquals(AdminPinStore.Result.UNLOCKED, store.verify("0482"))
    }

    @Test fun missingOrCorruptCredentialsBlockChangeWithoutOverwritingAnything() {
        val store = AdminPinStore(preferences)
        assertThrows(IllegalStateException::class.java) { store.change("0482", "0000") }
        assertFalse(store.hasPin)
        store.create("0482")
        val valid = preferences.all.toMap()
        listOf("admin_pin_salt", "admin_pin_hash").forEach { key ->
            preferences.edit().remove(key).commit()
            val partial = preferences.all.toMap()
            assertThrows(IllegalArgumentException::class.java) { store.change("0482", "0000") }
            assertEquals(partial, preferences.all)
            preferences.edit().putString(key, "A").commit()
            val corrupt = preferences.all.toMap()
            assertThrows(IllegalArgumentException::class.java) { store.change("0482", "0000") }
            assertEquals(corrupt, preferences.all)
            preferences.edit().putString(key, valid[key] as String).commit()
        }
        assertEquals(AdminPinStore.Result.UNLOCKED, store.verify("0482"))
    }

    @Test fun failedCredentialSaveKeepsOldPinAndAttemptCountWhenMemoryWasNotUpdated() {
        assertFailedChangeRestoresPin(updateMemory = false, throwOnFailure = false)
    }

    @Test fun failedCredentialSaveRestoresOldPinAndAttemptCountAfterMemoryWasUpdated() {
        assertFailedChangeRestoresPin(updateMemory = true, throwOnFailure = false)
    }

    @Test fun throwingCredentialSaveRestoresOldPinAndAttemptCountAfterMemoryWasUpdated() {
        assertFailedChangeRestoresPin(updateMemory = true, throwOnFailure = true)
    }

    private fun assertFailedChangeRestoresPin(updateMemory: Boolean, throwOnFailure: Boolean) {
        val store = AdminPinStore(preferences)
        store.create("0482")
        repeat(4) { assertEquals(AdminPinStore.Result.INCORRECT, store.verify("1111")) }
        val previous = preferences.all.toMap()
        val credentialWrites = mutableListOf<Set<String>>()
        val failing = object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                val keys = mutableSetOf<String>()
                return object : SharedPreferences.Editor by editor {
                    override fun putString(key: String?, value: String?) = apply {
                        key?.let(keys::add)
                        editor.putString(key, value)
                    }
                    override fun putInt(key: String?, value: Int) = apply { editor.putInt(key, value) }
                    override fun putLong(key: String?, value: Long) = apply { editor.putLong(key, value) }
                    override fun commit(): Boolean {
                        if ("admin_pin_hash" !in keys) return editor.commit()
                        credentialWrites += keys.toSet()
                        if (updateMemory) editor.commit()
                        if (throwOnFailure) throw IllegalStateException("Storage unavailable")
                        return false
                    }
                }
            }
        }

        assertThrows(IllegalStateException::class.java) { AdminPinStore(failing).change("0482", "0000") }
        assertEquals(previous, preferences.all)
        assertEquals(2, credentialWrites.size)
        credentialWrites.forEach { assertEquals(setOf("admin_pin_salt", "admin_pin_hash"), it) }
        assertEquals(AdminPinStore.Result.LOCKED, AdminPinStore(preferences).verify("0000"))
        assertEquals(5, preferences.getInt("admin_pin_failures", -1))
        preferences.edit().putLong("admin_pin_locked_until", 1L).commit()
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("0482"))
    }
}
