package com.ssmath.app

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal data class PortalAccount(val origin: String, val username: String) {
    companion object {
        fun from(address: String, username: String): PortalAccount {
            val url = address.trim().toHttpUrlOrNull()
            require(url != null && url.isHttps && url.username.isEmpty() && url.password.isEmpty() &&
                url.encodedPath == "/" && url.query == null && url.fragment == null) {
                "Enter the portal's HTTPS origin, without a path, password, query, or fragment."
            }
            val name = username.trim()
            require(name.length in 2..64 && name.none(Char::isISOControl)) {
                "Enter an existing portal username (2-64 characters)."
            }
            return PortalAccount(url.toString().removeSuffix("/"), name)
        }
    }
}

@Serializable
internal data class PortalApproval(val status: String, val fingerprint: String, val syncApproved: Boolean = false) {
    init {
        require(status in setOf("pending", "approved", "expired", "revoked") &&
            fingerprint.matches(Regex("[0-9a-f]{16}")))
    }
    val canSync get() = status == "approved" && syncApproved
}

@Serializable
internal data class ServerCollection(val version: String, val data: CollectionBackup?) {
    init { require(version.isNotBlank() && version.length <= 160) }
}

internal class PortalHttpError(val status: Int) : IOException("Portal returned HTTP $status.")

internal class PortalApi(callFactory: Call.Factory? = null) {
    private val calls by lazy { callFactory ?: OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .callTimeout(25, TimeUnit.SECONDS).build() }
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun approval(account: PortalAccount, token: String, register: Boolean): PortalApproval {
        val result = request(account, if (register) "devices/register" else "devices/status",
            body = buildJsonObject {
                put("username", account.username)
                put("fingerprintToken", token)
                if (register) put("requestSync", true)
            })
        val device = json.decodeFromJsonElement(PortalApproval.serializer(),
            result["device"] ?: throw IOException("Missing device approval."))
        if (device.fingerprint != token.sha256().take(16)) throw IOException("Unexpected device fingerprint.")
        return device
    }

    fun download(account: PortalAccount, token: String): ServerCollection =
        decodeCollection(request(account, "sync", token))

    fun upload(account: PortalAccount, token: String, base: String, local: CollectionBackup): ServerCollection {
        val result = request(account, "imports", token, buildJsonObject {
            put("source", "ssAcademy Android ${token.sha256().take(16)}")
            put("data", json.encodeToJsonElement(CollectionBackup.serializer(), local))
            put("baseVersion", base)
        })
        return decodeCollection(result["sync"]).also {
            if (it.data == null) throw IOException("Missing collection in upload receipt.")
        }
    }

    private fun decodeCollection(element: JsonElement?): ServerCollection {
        val record = element as? JsonObject ?: throw IOException("Missing server collection.")
        val version = (record["version"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw IOException("Missing server version.")
        val data = record["data"] ?: throw IOException("Missing server data.")
        return ServerCollection(version, if (data == JsonNull) null else
            data.toString().byteInputStream().use(CollectionBackup::read))
    }

    private fun request(account: PortalAccount, path: String, token: String? = null, body: JsonObject? = null): JsonObject {
        val request = Request.Builder().url("${account.origin}/ssacademy/api/$path")
            .header("Accept", "application/json")
        if (token != null) request.header("Authorization", "Bearer $token")
        if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
        calls.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw PortalHttpError(response.code)
            val source = response.body?.source() ?: throw IOException("Missing portal response.")
            // Allow space for the upload receipt around the bounded collection object.
            if (source.request((CollectionBackup.MAX_BYTES + 4097).toLong())) throw IOException("Portal response too large.")
            return Json.decodeFromString(JsonObject.serializer(), source.readByteArray().decodeToString(throwOnInvalidSequence = true))
        }
    }
}
