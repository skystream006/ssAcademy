package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal class ReadingRepositoryPack(manifestFile: String = "reading-activities-import.json") {
    private val root = listOf(File("../datasets/reading"), File("datasets/reading")).first { it.isDirectory }
    private val manifestBytes = File(root, manifestFile).readBytes()
    val manifest = readingPackJson.decodeFromString<ReadingPackManifest>(manifestBytes.toString(Charsets.UTF_8))
    val contentFile = File(root, manifest.file)
    val bytes by lazy { contentFile.readBytes() }
    val activities by lazy {
        buildList { forEachRecord { _, activity -> add(activity) } }
    }

    fun forEachRecord(consume: (String, ReadingActivity) -> Unit) {
        contentFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line -> consume(line, readingPackJson.decodeFromString<ReadingActivity>(line)) }
        }
    }

    fun importer(catalog: ReadingCatalog, cache: File) = ReadingPackImporter(catalog, cache,
        ReadingPackTransport { url, consume ->
            requireReadingPackUrl(url)
            if (url == READING_PACK_MANIFEST_URL) manifestBytes.inputStream().use(consume) else {
                assertEquals(READING_PACK_MANIFEST_URL.substringBeforeLast('/') + "/" + manifest.file, url)
                contentFile.inputStream().use(consume)
            }
        })
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingPackTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var database: ReadingDatabase
    private lateinit var catalog: ReadingCatalog
    private lateinit var cache: File
    private val first get() = ReadingContent.activities.first()

    @Before fun setup() {
        application.deleteDatabase("reading-pack-test.db")
        database = ReadingDatabase(application, "reading-pack-test.db")
        catalog = ReadingCatalog(database)
        cache = File(application.cacheDir, "reading-pack-test").apply { mkdirs() }
    }

    @After fun cleanup() {
        database.close()
        application.deleteDatabase("reading-pack-test.db")
        cache.deleteRecursively()
    }

    private fun contentBytes(activities: List<ReadingActivity>) =
        activities.joinToString(separator = "\n", postfix = "\n") { readingPackJson.encodeToString(it) }
            .toByteArray(Charsets.UTF_8)

    private fun manifest(bytes: ByteArray, activities: List<ReadingActivity>, version: String = "test-v1") =
        ReadingPackManifest(1, version, "activities-${readingSha256(bytes)}.jsonl", readingSha256(bytes),
            bytes.size.toLong(), activities.groupingBy { "${it.game.name}:${it.level.name}" }.eachCount())

    private fun transport(bytes: ByteArray, manifest: ReadingPackManifest): ReadingPackTransport =
        ReadingPackTransport { url, consume ->
            requireReadingPackUrl(url)
            val data = if (url == READING_PACK_MANIFEST_URL)
                readingPackJson.encodeToString(manifest).toByteArray(Charsets.UTF_8)
            else {
                assertTrue(url.endsWith("/${manifest.file}"))
                bytes
            }
            ByteArrayInputStream(data).use(consume)
        }

    private fun install(
        activities: List<ReadingActivity> = listOf(first),
        bytes: ByteArray = contentBytes(activities),
        manifest: ReadingPackManifest = manifest(bytes, activities),
        onProgress: (String) -> Unit = {}
    ): ReadingPackStatus = runBlocking {
        ReadingPackImporter(catalog, cache, transport(bytes, manifest)).importPack(onProgress)
    }

    private fun rowCount(table: String): Int =
        database.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

    private fun storedChecksum(): String =
        database.readableDatabase.rawQuery("SELECT checksum FROM reading_pack WHERE singleton=1", null).use {
            it.moveToFirst()
            it.getString(0)
        }

    private fun assertCleanFailure(action: () -> Unit) {
        val oldStatus = catalog.status()
        val oldContent = rowCount("reading_content")
        val oldCurrent = rowCount("reading_current")
        assertThrows(Exception::class.java, action)
        assertEquals(oldStatus, catalog.status())
        assertEquals(oldContent, rowCount("reading_content"))
        assertEquals(oldCurrent, rowCount("reading_current"))
        assertEquals(0, cache.listFiles().orEmpty().size)
        database.readableDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE name='reading_import_stage'", null
        ).use { assertFalse(it.moveToFirst()) }
    }

    @Test fun repositoryPackPreservesAll30LegacyActivitiesAndAdds15SummitActivities() {
        val pack = ReadingRepositoryPack("manifest-v2.json")
        val manifest = pack.manifest
        val bytes = pack.bytes
        val activities = pack.activities
        val legacy = ReadingRepositoryPack("manifest-v1.json")
        assertEquals(1, manifest.schemaVersion)
        assertEquals("v2", manifest.version)
        assertEquals(29081L, manifest.bytes)
        assertEquals("da742a95437b288c409e814d22f3e40f0fb356a03639e4fa0bfc0c57c52b173e",
            manifest.sha256)
        assertEquals(45, manifest.counts.values.sum())
        assertEquals(45, activities.size)
        assertEquals(45, activities.map { it.id }.distinct().size)
        assertEquals(bytes.size.toLong(), manifest.bytes)
        assertEquals(readingSha256(bytes), manifest.sha256)
        assertEquals("activities-${manifest.sha256}.jsonl", manifest.file)
        assertArrayEquals(legacy.bytes, bytes.copyOfRange(0, legacy.bytes.size))
        assertEquals(ReadingContent.activities, activities.take(30))
        assertEquals(ReadingContent.activities, activities.filter { it.level != ReadingLevel.SUMMIT })
        assertEquals(15, activities.count { it.level == ReadingLevel.SUMMIT })
        assertEquals(manifest.counts, activities.groupingBy { "${it.game.name}:${it.level.name}" }.eachCount())
        activities.forEach(ReadingPackValidator::validate)
        assertEquals(manifest.counts, runBlocking { pack.importer(catalog, cache).importPack() }.counts)
        assertEquals("v2", catalog.status().version)
        assertEquals(manifest.sha256, storedChecksum())
        ReadingGame.entries.forEach { game -> ReadingLevel.entries.forEach { level ->
            assertEquals(if (game == ReadingGame.GRAMMAR) 0 else 3, catalog.candidates(game, level).size)
        } }
        activities.forEach { activity ->
            val latest = requireNotNull(catalog.latest(activity.id))
            assertEquals(activity, latest.activity)
            assertEquals(activity, catalog.find(activity.id, latest.revision))
        }
    }

    @Test fun archivedManifestAndRemaining30ActivityPackRemainImportable() {
        val pack = ReadingRepositoryPack("manifest-v1.json")
        assertEquals("v1", pack.manifest.version)
        assertEquals(1, pack.manifest.schemaVersion)
        assertEquals(14369L, pack.manifest.bytes)
        assertEquals("26ee2a35b6bdaa22cb7e82447f12ac18a9e410fd3c3d1173ff24f4ee84a25f92",
            pack.manifest.sha256)
        assertEquals(pack.manifest.bytes, pack.bytes.size.toLong())
        assertEquals(pack.manifest.sha256, readingSha256(pack.bytes))
        assertEquals(ReadingContent.activities, pack.activities)
        assertEquals(30, pack.activities.size)
        assertEquals(pack.manifest.counts, runBlocking { pack.importer(catalog, cache).importPack() }.counts)
        assertEquals("v1", catalog.status().version)
        assertEquals(pack.manifest.sha256, storedChecksum())
        ReadingGame.entries.forEach { game ->
            listOf(ReadingLevel.SPROUT, ReadingLevel.TRAIL).forEach { level ->
                assertEquals(if (game == ReadingGame.GRAMMAR) 0 else 3, catalog.candidates(game, level).size)
            }
            assertTrue(catalog.candidates(game, ReadingLevel.SUMMIT).isEmpty())
        }
    }

    @Test fun importRequestsDescriptiveManifestThenUnchangedChecksumData() {
        val activities = listOf(first)
        val bytes = contentBytes(activities)
        val current = manifest(bytes, activities)
        val response = transport(bytes, current)
        val requests = mutableListOf<String>()
        val status = runBlocking {
            ReadingPackImporter(catalog, cache, ReadingPackTransport { url, consume ->
                requests += url
                response.read(url, consume)
            }).importPack()
        }
        assertEquals("test-v1", status.version)
        assertEquals("reading-activities-v8-import.json", requests.first().substringAfterLast('/'))
        assertEquals("activities-${current.sha256}.jsonl", requests.last().substringAfterLast('/'))
        assertEquals(first, catalog.latest(first.id)?.activity)
        assertCleanFailure {
            install(activities, bytes, current.copy(file = "activities-${"0".repeat(64)}.jsonl"))
        }
        assertEquals("test-v1", install(activities, bytes, manifest(bytes, activities)).version)
    }

    @Test fun canonicalAndLegacyPublishedImportsHaveIdenticalPayloadBytesAndMetadata() {
        val current = ReadingRepositoryPack()
        val legacy = ReadingRepositoryPack("manifest.json")
        assertEquals("activities-${current.manifest.sha256}.jsonl", current.manifest.file)
        assertEquals(current.manifest, legacy.manifest)
        assertEquals(current.contentFile, legacy.contentFile)
        assertEquals(current.manifest.sha256, readingSha256(current.bytes))
        assertArrayEquals(legacy.bytes, current.bytes)
    }

    @Test fun emptyCatalogueRemainsEmptyUntilACompletePackHasBeenInstalled() {
        assertEquals(ReadingPackStatus(), catalog.status())
        assertNull(catalog.latest(first.id))
        assertTrue(catalog.candidates(first.game, first.level).isEmpty())
        assertThrows(IllegalArgumentException::class.java) { catalog.find(first.id, "unknown") }
    }

    @Test fun legacyArchiveIsNotSelectableAndSharesTheCanonicalRevisionWithImportedContent() {
        val revision = catalog.insertLegacy(first)
        assertEquals(first, catalog.find(first.id, revision))
        assertEquals(revision, catalog.insertLegacy(first))
        assertEquals(1, rowCount("reading_content"))
        assertNull(catalog.latest(first.id))
        assertEquals(ReadingPackStatus(), catalog.status())
        install()
        assertEquals(revision, catalog.latest(first.id)?.revision)
        assertEquals(1, rowCount("reading_content"))
    }

    @Test fun updatesAreAdditiveAndKeepOldRevisionsAcrossReopeningTheDatabase() {
        install(ReadingContent.activities.take(2))
        val original = requireNotNull(catalog.latest(first.id))
        val changed = first.copy(title = "An updated title")
        val addition = first.copy(id = "missing-new")
        val entries = listOf(changed, addition)
        val bytes = contentBytes(entries)
        install(entries, bytes, manifest(bytes, entries, "test-v2"))
        assertNotEquals(original.revision, catalog.latest(first.id)?.revision)
        assertEquals(first, catalog.find(first.id, original.revision))
        assertEquals(changed, catalog.latest(first.id)?.activity)
        assertNotNull(catalog.latest(ReadingContent.activities[1].id))
        assertEquals(3, catalog.status().counts["MISSING:SPROUT"])
        database.close()
        database = ReadingDatabase(application, "reading-pack-test.db")
        catalog = ReadingCatalog(database)
        assertEquals("test-v2", catalog.status().version)
        assertEquals(first, catalog.find(first.id, original.revision))
        assertEquals(changed, catalog.latest(first.id)?.activity)
        assertEquals(addition, catalog.latest(addition.id)?.activity)
    }

    @Test fun repeatedImportsPreserveReaderStateCyclesAndPresentedHistory() {
        val db = database.writableDatabase
        db.execSQL("INSERT INTO reading_state(singleton,payload) VALUES(1,'state sentinel')")
        db.execSQL("INSERT INTO reading_cycles VALUES('reader','MISSING','SPROUT',7,'missing-cup')")
        db.execSQL("INSERT INTO reading_presented VALUES('reader','MISSING','SPROUT','missing-cup',7)")
        val original = install()
        repeat(3) { assertEquals(original, install()) }
        assertEquals(1, rowCount("reading_content"))
        assertEquals(1, rowCount("reading_current"))
        db.rawQuery("SELECT payload FROM reading_state", null).use {
            assertTrue(it.moveToFirst()); assertEquals("state sentinel", it.getString(0))
        }
        db.rawQuery("SELECT cycle,last_id FROM reading_cycles", null).use {
            assertTrue(it.moveToFirst()); assertEquals(7, it.getInt(0)); assertEquals(first.id, it.getString(1))
        }
        db.rawQuery("SELECT activity_id,cycle FROM reading_presented", null).use {
            assertTrue(it.moveToFirst()); assertEquals(first.id, it.getString(0)); assertEquals(7, it.getInt(1))
        }
    }

    @Test fun catalogMethodsParticipateInTheCallersTransaction() {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val revision = catalog.insertLegacy(first)
            assertEquals(first, catalog.find(first.id, revision))
            db.execSQL("INSERT INTO reading_state(singleton,payload) VALUES(1,'temporary')")
        } finally {
            db.endTransaction()
        }
        assertEquals(0, rowCount("reading_content"))
        assertEquals(0, rowCount("reading_state"))
    }

    @Test fun malformedJsonUnknownGamesAndUnexpectedFieldsRollBackAnExistingCatalogue() {
        install()
        val valid = contentBytes(listOf(first.copy(id = "new-before-failure")))
        val corrupt = listOf(
            "{not json}\n",
            readingPackJson.encodeToString(first.copy(id = "bad-game")).replace("\"MISSING\"", "\"UNKNOWN\"") + "\n",
            readingPackJson.encodeToString(first.copy(id = "retired-game")).replace("\"MISSING\"", "\"CHAPTER\"") + "\n",
            readingPackJson.encodeToString(first.copy(id = "retired-game")).replace("\"MISSING\"", "\"PARTNERS\"") + "\n",
            readingPackJson.encodeToString(first.copy(id = "retired-game")).replace("\"MISSING\"", "\"CAPTION\"") + "\n",
            readingPackJson.encodeToString(first.copy(id = "bad-level")).replace("\"SPROUT\"", "\"EXPERT\"") + "\n",
            readingPackJson.encodeToString(first.copy(id = "bad-field")).replace("\"title\":", "\"surprise\":") + "\n"
        )
        corrupt.forEach { invalid ->
            val bytes = valid + invalid.toByteArray(Charsets.UTF_8)
            assertCleanFailure { install(bytes = bytes, manifest = manifest(bytes, listOf(first, first))) }
        }
        assertEquals(first, catalog.latest(first.id)?.activity)
    }

    @Test fun everyGameRejectsItsInvalidSchemaAndAnswers() {
        val invalid = ReadingContent.activities.map { it.game }.distinct().map { game ->
            val activity = ReadingContent.forGame(game, ReadingLevel.SPROUT).first()
            when (game) {
                ReadingGame.MISSING -> activity.copy(passage = listOf("No gap here."))
                ReadingGame.SCRAMBLE -> activity.copy(validOrders = listOf(listOf(0, 0, 2)))
                ReadingGame.SEQUENCE -> activity.copy(validOrders = listOf(listOf(0, 1, 2)))
                ReadingGame.DETECTIVE -> activity.copy(evidence = activity.passage.size)
                ReadingGame.STUDIO -> activity.copy(writingChoices = listOf(listOf("Only one")))
                ReadingGame.GRAMMAR -> activity.copy(explanation = "")
            }
        } + listOf(
            first.copy(question = requireNotNull(first.question).copy(answer = 4)),
            first.copy(question = requireNotNull(first.question).copy(choices = listOf("same", "same"), answer = 0)),
            first.copy(question = requireNotNull(first.question).copy(prompt = "")),
            first.copy(picture = first.picture.copy(description = "")),
            first.copy(id = "../bad"),
            first.copy(hint = "bad\u0000text"),
            ReadingContent.find("sequence-seed").copy(eventPictures = emptyList()),
            ReadingContent.find("scramble-cats").copy(validOrders = emptyList()),
            ReadingContent.find("scramble-cats").copy(validOrders = listOf(listOf(1, 2, 3)))
        )
        install()
        invalid.forEach { activity ->
            assertCleanFailure { install(listOf(first.copy(id = "new-before-invalid"), activity)) }
        }
    }

    @Test fun duplicateActivityIdsAreRejectedBeforeInstallingAnyRows() {
        install()
        assertCleanFailure {
            install(listOf(first.copy(id = "same-new"), first.copy(id = "same-new", title = "Different payload")))
        }
    }

    @Test fun idsCannotMoveGameOrLevelEvenWhenOnlyAnArchivedRevisionExists() {
        val movedLevel = first.copy(level = ReadingLevel.TRAIL)
        val movedGame = ReadingContent.find("detective-bag").copy(id = first.id)
        catalog.insertLegacy(first)
        listOf(movedLevel, movedGame).forEach { assertCleanFailure { install(listOf(it)) } }
        install()
        listOf(movedLevel, movedGame).forEach { assertCleanFailure { install(listOf(it)) } }
        assertThrows(IllegalArgumentException::class.java) { catalog.insertLegacy(movedLevel) }
    }

    @Test fun checksumCountAndByteFailuresNeverReplaceTheInstalledPack() {
        install()
        val addition = listOf(first.copy(id = "new-checksum"))
        val bytes = contentBytes(addition)
        val good = manifest(bytes, addition, "v2")
        val zeroHash = "0".repeat(64)
        listOf(
            good.copy(sha256 = zeroHash, file = "activities-$zeroHash.jsonl"),
            good.copy(counts = mapOf("MISSING:SPROUT" to 2)),
            good.copy(counts = mapOf("MISSING:TRAIL" to 1)),
            good.copy(bytes = good.bytes - 1),
            good.copy(bytes = good.bytes + 1)
        ).forEach { invalid -> assertCleanFailure { install(addition, bytes, invalid) } }
        assertEquals("test-v1", catalog.status().version)
    }

    @Test fun manifestsRejectUnsupportedVersionsPathsAndUnboundedCountsBeforeDownloading() {
        val bytes = contentBytes(listOf(first))
        val good = manifest(bytes, listOf(first))
        listOf(
            good.copy(schemaVersion = 2),
            good.copy(version = ""),
            good.copy(version = "v".repeat(81)),
            good.copy(file = "activities-v1.jsonl"),
            good.copy(file = "reading_activities_import.jsonl"),
            good.copy(file = "reading_activities_import-${"0".repeat(64)}.jsonl"),
            good.copy(file = "../secret.jsonl"),
            good.copy(file = "https://other.example/pack.jsonl"),
            good.copy(bytes = ReadingPackImporter.MAX_PACK_BYTES + 1),
            good.copy(bytes = 0),
            good.copy(counts = emptyMap()),
            good.copy(counts = mapOf("MISSING:SPROUT" to 0)),
            good.copy(counts = mapOf("MISSING:SPROUT" to -1)),
            good.copy(counts = mapOf("MISSING:SPROUT" to Int.MAX_VALUE)),
            good.copy(counts = mapOf("MISSING:SPROUT" to ReadingPackImporter.MAX_ACTIVITIES,
                "MISSING:TRAIL" to ReadingPackImporter.MAX_ACTIVITIES)),
            good.copy(counts = mapOf("UNKNOWN:SPROUT" to 1))
        ).forEach { invalid ->
            var requests = 0
            val transport = ReadingPackTransport { url, consume ->
                requests++
                assertEquals(READING_PACK_MANIFEST_URL, url)
                consume(ByteArrayInputStream(readingPackJson.encodeToString(invalid).toByteArray()))
            }
            assertCleanFailure {
                runBlocking { ReadingPackImporter(catalog, cache, transport).importPack() }
            }
            assertEquals(1, requests)
        }
    }

    @Test fun onlyExactCredentialFreeHttpsRepositoryPathsAreAllowed() {
        val url = READING_PACK_MANIFEST_URL
        val root = url.substringBeforeLast('/') + "/"
        requireReadingPackUrl(url)
        requireReadingPackUrl(root + "manifest.json")
        requireReadingPackUrl(root + "activities-${"a".repeat(64)}.jsonl")
        listOf(
            url.replace("https:", "http:"),
            url.replace("raw.githubusercontent.com", "raw.githubusercontent.com.attacker.example"),
            url.replace("https://", "https://reader@"),
            url.replace("raw.githubusercontent.com", "raw.githubusercontent.com:444"),
            url.replace("/main/", "/other-branch/"),
            url.replace("/ssAcademy/", "/another-repo/"),
            root + "../secret",
            root + "%72eading-activities-import.json",
            root + "reading_activities_import.json",
            root + "reading_activities_import-${"a".repeat(64)}.jsonl",
            root + "reading_activities_import.jsonl",
            "$url?token=secret",
            "$url#fragment"
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { requireReadingPackUrl(invalid) }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { HttpsReadingPackTransport().read(invalid) { fail("Network must not be used") } }
            }
        }
    }

    @Test fun oversizedManifestAndRecordInputsAreBoundedAndInvalidUtf8IsRejected() {
        val transport = ReadingPackTransport { _, consume ->
            var supplied = 0
            val input = object : InputStream() {
                override fun read(): Int {
                    supplied++
                    require(supplied <= ReadingPackImporter.MAX_MANIFEST_BYTES + 8 * 1024)
                    return ' '.code
                }
            }
            consume(input)
        }
        assertCleanFailure { runBlocking { ReadingPackImporter(catalog, cache, transport).importPack() } }
        val tooLong = (" ".repeat(ReadingPackImporter.MAX_LINE_BYTES + 1) + "\n").toByteArray()
        assertCleanFailure { install(bytes = tooLong) }
        val invalidUtf8 = contentBytes(listOf(first)) + byteArrayOf(0xc3.toByte(), 0x28, 0x0a)
        assertCleanFailure { install(bytes = invalidUtf8) }
        val emptyLine = contentBytes(listOf(first)) + "\n".toByteArray()
        assertCleanFailure { install(bytes = emptyLine) }
    }

    @Test fun aFailedStreamOrCancellationLeavesThePreviousPackAndCleansItsDownload() {
        install()
        val bytes = contentBytes(listOf(first.copy(id = "network-addition")))
        val manifest = manifest(bytes, listOf(first))
        listOf(false, true).forEach { cancelled ->
            val transport = ReadingPackTransport { url, consume ->
                if (url == READING_PACK_MANIFEST_URL)
                    consume(ByteArrayInputStream(readingPackJson.encodeToString(manifest).toByteArray()))
                else consume(object : InputStream() {
                    override fun read(): Int =
                        if (cancelled) throw CancellationException("Test cancellation") else throw IOException("Offline")
                })
            }
            assertCleanFailure { runBlocking { ReadingPackImporter(catalog, cache, transport).importPack() } }
        }
        assertEquals(first, catalog.latest(first.id)?.activity)
    }

    @Test fun cancellationAfterStagingRowsRollsBackTheTransaction() {
        install()
        val entries = (1..101).map { first.copy(id = "cancel-$it") }
        val bytes = contentBytes(entries)
        val manifest = manifest(bytes, entries)
        assertCleanFailure {
            runBlocking {
                val job = requireNotNull(currentCoroutineContext()[Job])
                ReadingPackImporter(catalog, cache, transport(bytes, manifest)).importPack { message ->
                    if (message.startsWith("Validated 100")) job.cancel(CancellationException("Cancelled during staging"))
                }
            }
        }
        assertEquals(1, rowCount("reading_current"))
        assertEquals(first, catalog.latest(first.id)?.activity)
        assertNull(catalog.latest("cancel-1"))
    }

    @Test fun serializationFormattingDoesNotCreateNewContentRevisions() {
        install()
        val original = requireNotNull(catalog.latest(first.id))
        val bytes = ("  " + readingPackJson.encodeToString(first) + "  \n").toByteArray()
        install(bytes = bytes)
        assertEquals(original.revision, catalog.latest(first.id)?.revision)
        assertEquals(1, rowCount("reading_content"))
    }

    @Test fun downloadProgressIncludesThrottledPercentAndByteCounts() {
        val entries = (1..600).map { first.copy(id = "progress-$it") }
        val bytes = contentBytes(entries)
        val messages = mutableListOf<String>()
        install(entries, bytes, onProgress = messages::add)
        val downloads = messages.filter { it.startsWith("Downloading reading activities:") }
        assertTrue(downloads.size in 2..21)
        assertEquals("Downloading reading activities: 100% (${bytes.size}/${bytes.size} bytes)", downloads.last())
        val percentages = downloads.map { it.substringAfter(": ").substringBefore('%').toInt() }
        assertEquals(percentages.sorted().distinct(), percentages)
        percentages.dropLast(1).zipWithNext().forEach { (previous, next) -> assertTrue(next - previous >= 5) }
    }
}
