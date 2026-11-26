package com.ssmath.app

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

internal class PortalLink(
    private val prefs: SharedPreferences,
    private val key: () -> SecretKey = ::deviceKey
) {
    val account: PortalAccount? get() = prefs.getString("origin", null)?.let {
        PortalAccount(it, requireNotNull(prefs.getString("username", null)))
    }
    val fingerprint: String? get() = prefs.getString("fingerprint", null)
    val granted get() = prefs.getBoolean("granted", false)
    val importId: String? get() = prefs.getString("importId", null)
    val baseDigest: String? get() = prefs.getString("baseDigest", null)
    val verified get() = prefs.getBoolean("verifiedImport", false)

    fun enroll(target: PortalAccount) {
        if (account == target) return
        require(account == null) { "Disconnect before changing the portal or username." }
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(target.binding())
        val sealed = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        prefs.edit().clear().putString("origin", target.origin).putString("username", target.username)
            .putString("fingerprint", token.sha256().take(16))
            .putString("credential", Base64.getEncoder().encodeToString(sealed)).persist()
    }

    fun token(target: PortalAccount): String {
        if (account != target) throw IOException("The portal connection changed.")
        val sealed = Base64.getDecoder().decode(requireNotNull(prefs.getString("credential", null)))
        require(sealed.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, 12))
        cipher.updateAAD(target.binding())
        return cipher.doFinal(sealed, 12, sealed.size - 12).toString(Charsets.UTF_8)
    }

    fun grant(value: Boolean) = prefs.edit().putBoolean("granted", value).persist()
    fun rememberImport(id: String, digest: String, verified: Boolean) =
        prefs.edit().putString("importId", id).putString("baseDigest", digest).putBoolean("verifiedImport", verified).persist()
    fun forget() = prefs.edit().clear().persist()

    private fun PortalAccount.binding() = "$origin\n$username".toByteArray(Charsets.UTF_8)
    private fun SharedPreferences.Editor.persist() {
        if (!commit()) throw IOException("Unable to save the portal connection on this device.")
    }
}

private fun deviceKey(): SecretKey {
    val alias = "ssacademy.portal.device"
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        generateKey()
    }
}
