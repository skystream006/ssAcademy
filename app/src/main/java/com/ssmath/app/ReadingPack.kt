package com.ssmath.app

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal val readingPackJson = Json { encodeDefaults = true }
internal fun readingSha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

@Serializable
data class ReadingPackManifest(
    val schemaVersion: Int,
    val version: String,
    val file: String,
    val sha256: String,
    val bytes: Long,
    val counts: Map<String, Int>
)

object ReadingPackValidator {
    private fun text(value: String, maximum: Int = 4_000) {
        require(value.isNotBlank() && value.length <= maximum &&
            value.none { it.isISOControl() && it != '\n' && it != '\t' }) { "Invalid reading text" }
    }

    private fun picture(value: ReadingPicture) {
        text(value.symbol, 80)
        text(value.description, 500)
    }

    private fun question(value: ReadingQuestion) {
        text(value.prompt)
        require(value.choices.size in 2..4 && value.choices.distinct().size == value.choices.size) {
            "A reading question needs two to four distinct choices"
        }
        value.choices.forEach { text(it) }
        require(value.answer in value.choices.indices) { "The answer is outside the available choices" }
    }

    fun validate(activity: ReadingActivity) = with(activity) {
        require(id.matches(Regex("[a-z0-9][a-z0-9_-]{0,119}"))) { "Invalid reading activity ID" }
        text(title, 200)
        text(hint)
        picture(picture)
        require(passage.size <= 24 && tiles.size <= 24 && validOrders.size <= 24 &&
            eventPictures.size <= 24 && writingChoices.size <= 4) {
            "Reading activity exceeds its schema limits"
        }
        passage.forEach { text(it) }
        tiles.forEach { text(it) }
        eventPictures.forEach { picture(it) }
        question?.let { question(it) }
        require(validOrders.distinct().size == validOrders.size) { "Duplicate tile orders" }
        validOrders.forEach {
            require(it.sorted() == tiles.indices.toList()) { "Tile orders must be complete permutations" }
        }
        writingChoices.forEach { choices ->
            require(choices.size in 3..12 && choices.distinct().size == choices.size) { "Invalid writing choices" }
            choices.forEach { text(it) }
        }
        when (game) {
            ReadingGame.MISSING -> require(passage.size == 1 && passage.single().contains("___") && question != null)
            ReadingGame.SCRAMBLE -> require(tiles.size >= 2 && validOrders.isNotEmpty() && question != null)
            ReadingGame.SEQUENCE -> require(tiles.size >= 2 && validOrders.isNotEmpty() && question != null &&
                eventPictures.size == tiles.size && tiles.indices.toList() !in validOrders)
            ReadingGame.DETECTIVE -> require(passage.isNotEmpty() && evidence in passage.indices && question != null)
            ReadingGame.STUDIO -> require(writingChoices.size == 4)
        }
        if (game !in listOf(ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE))
            require(tiles.isEmpty() && validOrders.isEmpty())
        if (game != ReadingGame.SEQUENCE) require(eventPictures.isEmpty())
        if (game != ReadingGame.STUDIO) require(writingChoices.isEmpty())
    }
}

fun interface ReadingPackTransport {
    suspend fun read(url: String, consume: (InputStream) -> Unit)
}

private const val READING_PACK_ROOT =
    "https://raw.githubusercontent.com/skystream006/ssAcademy/main/datasets/reading/"
const val READING_PACK_MANIFEST_URL = "${READING_PACK_ROOT}reading-activities-import.json"

internal fun requireReadingPackUrl(url: String) {
    val parsed = url.toHttpUrl()
    require(parsed.scheme == "https" && parsed.host == "raw.githubusercontent.com" && parsed.port == 443 &&
        parsed.username.isEmpty() && parsed.password.isEmpty() && parsed.query == null && parsed.fragment == null &&
        url.startsWith(READING_PACK_ROOT) && parsed.toString() == url) { "Reading pack URL is not allowed" }
    val filename = url.removePrefix(READING_PACK_ROOT)
    require(filename in setOf("reading-activities-import.json", "manifest.json") ||
        filename.matches(Regex("activities-[a-f0-9]{64}\\.jsonl"))) {
        "Only the configured repository's reading pack files are allowed"
    }
}

class HttpsReadingPackTransport : ReadingPackTransport {
    private val client = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false)
        .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
        .cookieJar(CookieJar.NO_COOKIES).retryOnConnectionFailure(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES).build()

    override suspend fun read(url: String, consume: (InputStream) -> Unit) {
        requireReadingPackUrl(url)
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(Request.Builder().url(url).get().build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            require(it.code == 200) { "Reading pack download failed (HTTP ${it.code})" }
                            consume(requireNotNull(it.body).byteStream())
                        }
                        continuation.resume(Unit)
                    } catch (failure: Exception) {
                        continuation.resumeWithException(failure)
                    }
                }
            })
        }
    }
}

class ReadingPackImporter(
    private val catalog: ReadingCatalog,
    private val cacheDir: File,
    private val transport: ReadingPackTransport = HttpsReadingPackTransport()
) {
    companion object {
        internal const val MAX_PACK_BYTES = 64L * 1024 * 1024
        internal const val MAX_LINE_BYTES = 64 * 1024
        internal const val MAX_MANIFEST_BYTES = 16 * 1024
        internal const val MAX_ACTIVITIES = 100_000
    }

    suspend fun importPack(onProgress: (String) -> Unit = {}): ReadingPackStatus = withContext(Dispatchers.IO) {
        val context = currentCoroutineContext()
        onProgress("Checking the reading pack…")
        val manifestBytes = ByteArrayOutputStream()
        transport.read(READING_PACK_MANIFEST_URL) { input ->
            copyBounded(input, MAX_MANIFEST_BYTES.toLong(), { context.ensureActive() }) { bytes, count ->
                manifestBytes.write(bytes, 0, count)
            }
        }
        context.ensureActive()
        val manifest = readingPackJson.decodeFromString<ReadingPackManifest>(decodeUtf8(manifestBytes.toByteArray()))
        validateManifest(manifest)
        require(cacheDir.isDirectory || cacheDir.mkdirs()) { "Reading download storage is unavailable" }
        val file = File.createTempFile("reading-pack-", ".jsonl", cacheDir)
        try {
            onProgress("Downloading ${manifest.counts.values.sum()} reading activities…")
            val digest = MessageDigest.getInstance("SHA-256")
            val size = file.outputStream().use { output ->
                var downloaded = 0L
                var reportedBytes = 0L
                var reportedPercent = 0
                transport.read(READING_PACK_ROOT + manifest.file) { input ->
                    copyBounded(input, manifest.bytes, { context.ensureActive() }) { bytes, count ->
                        digest.update(bytes, 0, count)
                        output.write(bytes, 0, count)
                        downloaded += count
                        val percent = (downloaded * 100 / manifest.bytes).toInt()
                        if (downloaded == manifest.bytes || percent - reportedPercent >= 5 ||
                            downloaded - reportedBytes >= 256 * 1024) {
                            onProgress("Downloading reading activities: $percent% ($downloaded/${manifest.bytes} bytes)")
                            reportedBytes = downloaded
                            reportedPercent = percent
                        }
                    }
                }
                downloaded
            }
            require(size == manifest.bytes) { "Reading pack byte count does not match the manifest" }
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            require(checksum == manifest.sha256) { "Reading pack checksum does not match the manifest" }
            context.ensureActive()
            onProgress("Validating and installing reading activities…")
            install(file, manifest, onProgress) { context.ensureActive() }
        } finally {
            file.delete()
        }
    }

    private fun validateManifest(manifest: ReadingPackManifest) = with(manifest) {
        require(schemaVersion == 1) { "Unsupported reading pack schema" }
        require(version.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))) { "Invalid reading pack version" }
        require(sha256.matches(Regex("[a-f0-9]{64}")) && file == "activities-$sha256.jsonl") {
            "Reading packs must use their immutable checksum as the filename"
        }
        require(bytes in 1..MAX_PACK_BYTES) { "Reading pack is too large or empty" }
        val validKeys = ReadingGame.entries.flatMap { game ->
            ReadingLevel.entries.map { level -> "${game.name}:${level.name}" }
        }.toSet()
        require(counts.isNotEmpty() && counts.keys.all { it in validKeys } &&
            counts.values.all { it in 1..MAX_ACTIVITIES } &&
            counts.values.sumOf { it.toLong() } <= MAX_ACTIVITIES) { "Invalid reading pack counts" }
        requireReadingPackUrl(READING_PACK_ROOT + file)
    }

    private fun install(
        file: File,
        manifest: ReadingPackManifest,
        onProgress: (String) -> Unit,
        checkCancelled: () -> Unit
    ): ReadingPackStatus {
        val db = catalog.database.writableDatabase
        db.beginTransaction()
        try {
            // An on-disk staging table bounds memory and keeps all installed rows unchanged until validation succeeds.
            db.execSQL("""CREATE TABLE reading_import_stage (
                id TEXT PRIMARY KEY NOT NULL, game TEXT NOT NULL, level TEXT NOT NULL,
                revision TEXT NOT NULL, payload TEXT NOT NULL)""")
            val counts = mutableMapOf<String, Int>()
            var total = 0
            db.compileStatement("INSERT INTO reading_import_stage VALUES(?,?,?,?,?)").use { statement ->
                readLines(file, checkCancelled) { line ->
                    require(++total <= MAX_ACTIVITIES) { "Too many reading activities" }
                    val activity = readingPackJson.decodeFromString<ReadingActivity>(line)
                    ReadingPackValidator.validate(activity)
                    catalog.requireCompatible(db, activity)
                    val payload = readingPackJson.encodeToString(activity)
                    statement.bindString(1, activity.id)
                    statement.bindString(2, activity.game.name)
                    statement.bindString(3, activity.level.name)
                    statement.bindString(4, readingSha256(payload.toByteArray(Charsets.UTF_8)))
                    statement.bindString(5, payload)
                    statement.executeInsert()
                    val key = "${activity.game.name}:${activity.level.name}"
                    counts[key] = (counts[key] ?: 0) + 1
                    if (total % 100 == 0) onProgress("Validated $total reading activities…")
                }
            }
            require(counts == manifest.counts) { "Reading pack activity counts do not match the manifest" }
            checkCancelled()
            db.execSQL("""INSERT OR IGNORE INTO reading_content(id,revision,payload)
                SELECT id,revision,payload FROM reading_import_stage""")
            db.execSQL("""INSERT OR REPLACE INTO reading_current(id,game,level,revision)
                SELECT id,game,level,revision FROM reading_import_stage""")
            db.execSQL("INSERT OR REPLACE INTO reading_pack(singleton,version,checksum) VALUES(1,?,?)",
                arrayOf(manifest.version, manifest.sha256))
            db.execSQL("DROP TABLE reading_import_stage")
            checkCancelled()
            val status = catalog.status()
            db.setTransactionSuccessful()
            return status
        } finally {
            db.endTransaction()
        }
    }

    private fun readLines(file: File, checkCancelled: () -> Unit, consume: (String) -> Unit) {
        file.inputStream().buffered().use { input ->
            val line = ByteArrayOutputStream()
            while (true) {
                checkCancelled()
                val byte = input.read()
                if (byte == -1) {
                    if (line.size() > 0) consume(decodeUtf8(line.toByteArray()))
                    break
                }
                if (byte == '\n'.code) {
                    require(line.size() > 0) { "Empty reading activity line" }
                    consume(decodeUtf8(line.toByteArray()))
                    line.reset()
                } else {
                    require(line.size() < MAX_LINE_BYTES) { "Reading activity line is too large" }
                    line.write(byte)
                }
            }
        }
    }
}

private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
    .decode(ByteBuffer.wrap(bytes)).toString()

private fun copyBounded(
    input: InputStream,
    limit: Long,
    checkCancelled: () -> Unit,
    consume: (ByteArray, Int) -> Unit
): Long {
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        checkCancelled()
        val count = input.read(buffer)
        if (count < 0) return total
        total += count
        require(total <= limit) { "Reading download exceeds its byte limit" }
        consume(buffer, count)
    }
}
