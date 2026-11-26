package com.ssmath.app

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal data class PortalAccount(val origin: String, val student: String) {
    companion object {
        fun from(address: String, student: String): PortalAccount {
            val url = address.trim().toHttpUrlOrNull()
            require(url != null && url.isHttps && url.username.isEmpty() && url.password.isEmpty() &&
                url.encodedPath == "/" && url.query == null && url.fragment == null) {
                "Enter the portal's HTTPS origin, without a path, password, query, or fragment."
            }
            val name = student.trim()
            require(name.length in 1..40 && name.none(Char::isISOControl)) {
                "Save a game name with 1-40 characters in Settings > Change Name."
            }
            return PortalAccount(url.toString().removeSuffix("/"), name)
        }
    }
}

@Serializable
internal data class PortalDevice(val status: String, val fingerprint: String) {
    init {
        require(status in setOf("pending", "approved", "expired", "revoked") &&
            fingerprint.matches(Regex("[0-9a-f]{16}")))
    }
    val canSync get() = status == "approved"
}

@Serializable
internal data class PortalApproval(val device: PortalDevice, val bearerToken: String? = null) {
    init {
        require(bearerToken == null || (device.canSync && bearerToken.matches(Regex("[0-9a-f]{64}"))))
    }
}

internal data class ServerCollection(val id: String, val revision: Long, val data: CollectionBackup)

private val importIdPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
internal fun normalizedImportId(value: String) = value.trim().lowercase().takeIf(importIdPattern::matches)

@Serializable private data class ImportReceipt(val id: String)
@Serializable private data class RewardRow(val id: RewardType, val whole: Int, val fragments: Int)
@Serializable private data class ImportDetail(
    val id: String, val revision: Long, val format: String, val version: Int,
    val rewards: List<RewardRow>, val animations: List<Celebration>
)

internal class PortalHttpError(val status: Int) : IOException("Portal returned HTTP $status.")
internal class ImportNotFound : IOException()

internal class PortalApi(callFactory: Call.Factory? = null) {
    private val calls by lazy { callFactory ?: OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .callTimeout(25, TimeUnit.SECONDS).build() }
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun approval(account: PortalAccount, fingerprintToken: String, register: Boolean): PortalApproval {
        val result = request(account, if (register) "devices/register" else "devices/status",
            body = buildJsonObject {
                put("student", account.student)
                put("fingerprintToken", fingerprintToken)
            })
        val approval = json.decodeFromJsonElement(PortalApproval.serializer(), result)
        if (approval.device.fingerprint != fingerprintToken.sha256().take(16))
            throw IOException("Unexpected device fingerprint.")
        return approval
    }

    fun download(account: PortalAccount, bearerToken: String, id: String): ServerCollection {
        val expected = requireNotNull(normalizedImportId(id))
        val result = try {
            request(account, "imports/$expected", bearerToken)
        } catch (error: PortalHttpError) {
            if (error.status == 404) throw ImportNotFound() else throw error
        }
        val detail = json.decodeFromJsonElement(ImportDetail.serializer(), result.importRecord())
        require(detail.id == expected && detail.revision >= 1)
        require(detail.animations.size <= 2048 && detail.rewards.distinctBy { it.id }.size == detail.rewards.size)
        val backup = CollectionBackup(detail.format, detail.version,
            detail.rewards.associate { it.id to RewardBalance(it.whole, it.fragments) }, detail.animations.toSet())
        return ServerCollection(expected, detail.revision, backup)
    }

    fun upload(account: PortalAccount, bearerToken: String, local: CollectionBackup, fingerprint: String): String {
        val result = request(account, "imports", bearerToken, buildJsonObject {
            put("source", "ssAcademy Android $fingerprint")
            put("data", json.encodeToJsonElement(CollectionBackup.serializer(), local))
        })
        return requireNotNull(normalizedImportId(json.decodeFromJsonElement(ImportReceipt.serializer(), result.importRecord()).id))
    }

    private fun JsonObject.importRecord() = requireNotNull(this["import"] as? JsonObject) { "Missing import record." }

    private fun request(account: PortalAccount, path: String, bearerToken: String? = null, body: JsonObject? = null): JsonObject {
        val request = Request.Builder().url("${account.origin}/ssacademy/api/$path")
            .header("Accept", "application/json")
        if (bearerToken != null) request.header("Authorization", "Bearer $bearerToken")
        if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
        calls.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw PortalHttpError(response.code)
            val source = response.body?.source() ?: throw IOException("Missing portal response.")
            // Detail responses include revision audit metadata in addition to the bounded collection.
            require(!source.request(1024L * 1024 + 1)) { "Portal response too large." }
            return Json.decodeFromString(JsonObject.serializer(), source.readByteArray().decodeToString(throwOnInvalidSequence = true))
        }
    }
}
