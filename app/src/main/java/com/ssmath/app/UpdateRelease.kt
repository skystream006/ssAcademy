package com.ssmath.app

import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Response

internal const val MAX_UPDATE_BYTES = 100L * 1024 * 1024
internal const val UPDATE_ENDPOINT = "https://api.github.com/repos/skystream006/ssAcademy/releases/latest"
private const val RELEASE_PATH = "/skystream006/ssAcademy/releases/download/"
internal const val MAX_METADATA_BYTES = 1024L * 1024

internal data class UpdateVersion(val major: Long, val minor: Long, val revision: Long) : Comparable<UpdateVersion> {
    override fun compareTo(other: UpdateVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.revision })

    companion object {
        fun parse(value: String): UpdateVersion? {
            // Zero-padded parts such as 0.01.00 are allowed and compared numerically.
            if (!Regex("[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}").matches(value)) return null
            val parts = value.split('.').map { it.toLongOrNull() ?: return null }
            return UpdateVersion(parts[0], parts[1], parts[2])
        }
    }
}

internal suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

internal data class UpdateRelease(val version: String, val url: HttpUrl, val bytes: Long)

internal fun trustedUpdateUrl(url: HttpUrl, initial: Boolean): Boolean {
    if (!url.isHttps || url.port != 443 || url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return false
    if (url.host == "github.com") {
        val parts = url.encodedPath.removePrefix(RELEASE_PATH).split('/')
        return url.encodedPath.startsWith(RELEASE_PATH) && parts.size == 2 &&
            parts.all { it.isNotBlank() && '%' !in it && it != "." && it != ".." } &&
            (!initial || url.query == null)
    }
    return !initial && url.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com",
        "github-releases.githubusercontent.com")
}

internal fun parseUpdateRelease(body: String): UpdateRelease {
    val root = try { Json.parseToJsonElement(body) as? JsonObject } catch (_: Exception) { null }
        ?: throw IOException("GitHub returned invalid release information. Try checking again later.")
    fun text(key: String) = (root[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    if ((root["draft"] as? JsonPrimitive)?.booleanOrNull != false ||
        (root["prerelease"] as? JsonPrimitive)?.booleanOrNull != false) {
        throw IOException("No stable public release is available yet. Check again later.")
    }
    val tag = text("tag_name").orEmpty()
    val version = tag.removePrefix("v")
    if (!tag.startsWith("v") || UpdateVersion.parse(version) == null) {
        throw IOException("The latest release has an unsupported version tag. Ask the maintainer to publish v0.01.NN.")
    }
    val filename = "ssAcademy-v$version.apk"
    val assets = (root["assets"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    val asset = assets.filter {
        (it["name"] as? JsonPrimitive)?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true
    }.singleOrNull()?.takeIf { (it["name"] as? JsonPrimitive)?.contentOrNull == filename }
        ?: throw IOException("The latest release has no universal APK ($filename). Check again after publication finishes.")
    val url = (asset["browser_download_url"] as? JsonPrimitive)?.contentOrNull?.toHttpUrlOrNull()
    if (url == null || !trustedUpdateUrl(url, initial = true) || url.encodedPath != "$RELEASE_PATH$tag/$filename") {
        throw IOException("The release APK has an untrusted download address. Contact the maintainer.")
    }
    val bytes = (asset["size"] as? JsonPrimitive)?.longOrNull ?: 0
    if (bytes !in 1..MAX_UPDATE_BYTES) throw IOException("The release APK has an invalid size. Contact the maintainer.")
    return UpdateRelease(version, url, bytes)
}

internal fun updateHttpError(code: Int): IOException = IOException(when (code) {
    404 -> "No published update was found. Check again after the first release is available."
    403, 429 -> "GitHub denied or rate-limited the request. Wait before retrying, or try another network."
    else -> "GitHub returned HTTP $code. Check your connection and try again later."
})

internal suspend fun saveUpdateApk(input: InputStream, file: File, expectedBytes: Long, progress: (Long) -> Unit) {
    try {
        input.use {
            if (expectedBytes !in 1..MAX_UPDATE_BYTES) throw IOException("The release APK has an invalid size.")
            var count = 0L
            file.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    count += read
                    if (count > expectedBytes) throw IOException("The update exceeded its expected size.")
                    output.write(buffer, 0, read)
                    progress(count)
                }
            }
            if (count != expectedBytes) throw IOException("The APK download was incomplete. Try downloading again.")
        }
    } catch (e: Exception) {
        file.delete()
        throw e
    }
}
