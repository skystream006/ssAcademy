package com.ssmath.app

import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal class AdminPinStore(
    private val preferences: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis
) {
    enum class Result { UNLOCKED, INCORRECT, LOCKED }

    val hasPin: Boolean
        get() = preferences.contains("admin_pin_hash") || preferences.contains("admin_pin_salt")

    fun create(pin: String) {
        check(!hasPin)
        require(isValid(pin))
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        check(preferences.edit()
            .putString("admin_pin_salt", encode(salt))
            .putString("admin_pin_hash", encode(derive(pin, salt)))
            .putInt("admin_pin_failures", 0)
            .putLong("admin_pin_locked_until", 0)
            .commit())
    }

    fun verify(pin: String): Result {
        check(hasPin)
        val lockedUntil = preferences.getLong("admin_pin_locked_until", 0)
        if (now() < lockedUntil) return Result.LOCKED
        val salt = Base64.decode(requireNotNull(preferences.getString("admin_pin_salt", null)), Base64.NO_WRAP)
        val expected = Base64.decode(requireNotNull(preferences.getString("admin_pin_hash", null)), Base64.NO_WRAP)
        check(salt.size == 16 && expected.size == 32)
        if (isValid(pin) && MessageDigest.isEqual(expected, derive(pin, salt))) {
            check(preferences.edit().putInt("admin_pin_failures", 0).putLong("admin_pin_locked_until", 0).commit())
            return Result.UNLOCKED
        }
        val failures = (if (lockedUntil > 0) 0 else preferences.getInt("admin_pin_failures", 0)) + 1
        val locked = failures >= 5
        check(preferences.edit()
            .putInt("admin_pin_failures", failures)
            .putLong("admin_pin_locked_until", if (locked) now() + 30_000 else 0)
            .commit())
        return if (locked) Result.LOCKED else Result.INCORRECT
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val password = pin.toCharArray()
        val spec = PBEKeySpec(password, salt, 600_000, 256)
        password.fill('\u0000')
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)

    companion object {
        fun isValid(pin: String) = pin.length in 4..8 && pin.all { it in '0'..'9' }
    }
}
