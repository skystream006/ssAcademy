package com.ssmath.app

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AppUpdaterTest {
    private val cacheDir: File = Files.createTempDirectory("updates").toFile()
    private val download = "https://github.com/skystream006/ssAcademy/releases/download/v0.01.12/ssAcademy-v0.01.12.apk"

    @After fun cleanup() { cacheDir.deleteRecursively() }

    private fun release(
        tag: String = "v0.01.12",
        draft: Boolean = false,
        prerelease: Boolean = false,
        name: String = "ssAcademy-v0.01.12.apk",
        url: String = download,
        size: Long = 1234
    ) = """{"tag_name":"$tag","draft":$draft,"prerelease":$prerelease,"assets":[{"name":"$name","browser_download_url":"$url","size":$size}]}"""

    @Test fun versionsCompareNumerically() {
        assertTrue(UpdateVersion.parse("0.01.12")!! > UpdateVersion.parse("0.01.09")!!)
        assertTrue(UpdateVersion.parse("0.01.100")!! > UpdateVersion.parse("0.01.99")!!)
        assertTrue(UpdateVersion.parse("0.02.00")!! > UpdateVersion.parse("0.01.99")!!)
        assertTrue(UpdateVersion.parse("1.0.0")!! > UpdateVersion.parse("0.99.999")!!)
        assertEquals(UpdateVersion(0, 1, 0), UpdateVersion.parse("0.01.00"))
        assertEquals(0, UpdateVersion.parse("0.01.12")!!.compareTo(UpdateVersion.parse("0.1.12")!!))
        listOf("", "v0.01.12", "0.01", "0.01.12-beta", "0.01.-1", "1.0.9999999999").forEach {
            assertNull(it, UpdateVersion.parse(it))
        }
    }

    @Test fun stableReleaseSelectsExpectedUniversalApk() {
        assertEquals("https://api.github.com/repos/skystream006/ssAcademy/releases/latest", UPDATE_ENDPOINT)
        val parsed = parseUpdateRelease(release())
        assertEquals("0.01.12", parsed.version)
        assertEquals(download, parsed.url.toString())
        assertEquals(1234L, parsed.bytes)
        assertEquals(100L * 1024 * 1024, MAX_UPDATE_BYTES)
        assertEquals(MAX_UPDATE_BYTES, parseUpdateRelease(release(size = MAX_UPDATE_BYTES)).bytes)
    }

    @Test fun rejectsReleasesWithMoreThanOneApk() {
        val multiple = release().replace("}]}", """},{"name":"arm64.apk"}]}""")
        assertThrows(IOException::class.java) { parseUpdateRelease(multiple) }
    }

    @Test fun rejectsUnpublishedMalformedAndMissingAssets() {
        listOf(
            release(draft = true), release(prerelease = true), release(tag = "latest"),
            release(tag = "0.01.12"), release(tag = "v0.01.12-beta"),
            release(name = "arm64.apk"), release(size = 0), release(size = MAX_UPDATE_BYTES + 1),
            release(url = "https://example.com/update.apk"),
            release(url = download.replace("/v0.01.12/", "/v0.01.13/")),
            release(url = download.replace("ssAcademy-v0.01.12.apk", "other.apk")),
            """{"tag_name":"v0.01.12","draft":false,"prerelease":false,"assets":[]}""",
            "{}", "[]", "not json"
        ).forEach { body -> assertThrows(body, IOException::class.java) { parseUpdateRelease(body) } }
    }

    @Test fun initialUrlIsRestrictedToThisRepository() {
        assertTrue(trustedUpdateUrl(download.toHttpUrl(), initial = true))
        listOf(
            download.replace("https:", "http:"),
            download.replace("github.com", "github.com.evil.example"),
            download.replace("github.com", "username@github.com"),
            download.replace("github.com", "github.com:444"),
            download.replace("skystream006", "someone-else"),
            download.replace("/ssAcademy/", "/another-app/"),
            download.replace("/releases/download/", "/blob/"),
            "$download?token=anything", "$download#fragment",
            download.replace("v0.01.12.apk", "v0.01.12%2Fother.apk"),
            "https://release-assets.githubusercontent.com/signed.apk"
        ).forEach { assertFalse(it, trustedUpdateUrl(it.toHttpUrl(), initial = true)) }
    }

    @Test fun redirectsOnlyAllowExactHttpsGithubAssetHosts() {
        listOf("release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com").forEach {
            assertTrue(trustedUpdateUrl("https://$it/assets/file.apk?signature=value".toHttpUrl(), initial = false))
        }
        listOf(
            "http://release-assets.githubusercontent.com/file.apk",
            "https://release-assets.githubusercontent.com.evil.example/file.apk",
            "https://evil.githubusercontent.com/file.apk",
            "https://github.com/login",
            "https://username@objects.githubusercontent.com/file.apk",
            "https://objects.githubusercontent.com:444/file.apk",
            "https://example.com/file.apk"
        ).forEach { assertFalse(it, trustedUpdateUrl(it.toHttpUrl(), initial = false)) }
    }

    @Test fun errorsOfferUsefulNextSteps() {
        assertTrue(updateHttpError(404).message!!.contains("first release"))
        assertTrue(updateHttpError(403).message!!.contains("Wait"))
        assertTrue(updateHttpError(429).message!!.contains("rate-limited"))
        assertTrue(updateHttpError(500).message!!.contains("try again"))
    }

    @Test fun streamedDownloadReportsProgressAndRetainsOnlyCompleteBytes() = runBlocking {
        val file = File(cacheDir, "update-test.part")
        val bytes = ByteArray(150_000) { (it % 127).toByte() }
        val progress = mutableListOf<Long>()
        try {
            saveUpdateApk(ByteArrayInputStream(bytes), file, bytes.size.toLong()) { progress.add(it) }
            assertArrayEquals(bytes, file.readBytes())
            assertEquals(bytes.size.toLong(), progress.last())
            assertTrue(progress.zipWithNext().all { (a, b) -> b > a })
        } finally { file.delete() }
    }

    @Test fun truncatedOrOversizedDownloadDeletesPartialFile() = runBlocking {
        val file = File(cacheDir, "update-test.part")
        try {
            listOf(50L, 200L, MAX_UPDATE_BYTES + 1).forEach { expected ->
                val failure = runCatching {
                    saveUpdateApk(ByteArrayInputStream(ByteArray(100)), file, expected) {}
                }.exceptionOrNull()
                assertTrue(failure is IOException)
                assertFalse(file.exists())
            }
        } finally { file.delete() }
    }

    @Test fun cancellingStreamClosesInputAndDeletesPartialFile() = runBlocking {
        val file = File(cacheDir, "update-test.part")
        var closed = false
        val input = object : ByteArrayInputStream(ByteArray(150_000)) {
            override fun close() { closed = true; super.close() }
        }
        try {
            val job = launch {
                saveUpdateApk(input, file, 150_000) { cancel() }
            }
            job.join()
            assertTrue(job.isCancelled)
            assertTrue(closed)
            assertFalse(file.exists())
        } finally { file.delete() }
    }
}
