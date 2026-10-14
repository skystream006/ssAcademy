package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.DataInputStream
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Base64
import java.util.Locale
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingBulkPackTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var database: ReadingDatabase
    private lateinit var store: ReadingSqliteStore
    private lateinit var cache: File
    private val legacy get() = File(application.filesDir, "bulk-reading-legacy.json")
    private val expectedCounts = ReadingGame.entries.flatMap { game ->
        ReadingLevel.entries.map { level -> "${game.name}:${level.name}" to 1000 }
    }.toMap()

    @Before fun setup() {
        application.deleteDatabase("bulk-reading.db")
        listOf(legacy, File(legacy.path + ".bak"), File(legacy.path + ".new")).forEach { it.delete() }
        database = ReadingDatabase(application, "bulk-reading.db")
        store = ReadingSqliteStore(application, database, legacy)
        cache = File(application.cacheDir, "bulk-reading-pack-test").apply { mkdirs() }
    }

    @After fun cleanup() {
        database.close()
        application.deleteDatabase("bulk-reading.db")
        cache.deleteRecursively()
        listOf(legacy, File(legacy.path + ".bak"), File(legacy.path + ".new")).forEach { it.delete() }
    }

    private fun normalizedQuestion(question: ReadingQuestion): ReadingQuestion {
        val choices = question.choices.sorted()
        return question.copy(choices = choices, answer = choices.indexOf(question.choices[question.answer]))
    }

    private fun answers(activity: ReadingActivity): List<String> =
        (listOfNotNull(activity.question) + activity.branches.map { it.question }).map { question ->
            Normalizer.normalize(question.choices[question.answer], Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
        }

    private fun substantive(activity: ReadingActivity): String {
        val tileIndices = activity.tiles.indices.sortedBy { activity.tiles[it] }
        val positions = tileIndices.withIndex().associate { it.value to it.index }
        // Relabeling an activity or shuffling its choices/tiles does not make new reading content.
        val normalized = activity.copy(
            id = "", title = "", hint = "", game = ReadingGame.MISSING, level = ReadingLevel.SPROUT,
            question = activity.question?.let(::normalizedQuestion),
            tiles = tileIndices.map { activity.tiles[it] },
            validOrders = activity.validOrders.map { order -> order.map { positions.getValue(it) } }
                .sortedBy { it.joinToString(",") },
            eventPictures = if (activity.eventPictures.isEmpty()) emptyList()
                else tileIndices.map { activity.eventPictures[it] },
            branches = activity.branches.map { it.copy(question = normalizedQuestion(it.question)) }
                .sortedBy { it.label },
            writingChoices = activity.writingChoices.map { it.sorted() }
        )
        return readingPackJson.encodeToString(normalized)
    }

    private fun fresh(activity: ReadingActivity): ReadingData = ReadingEngine.start(
        ReadingData(
            profiles = listOf(ReadingProfile("reader-a", "Ari"), ReadingProfile("reader-b", "Bo")),
            selectedProfileId = "reader-a"
        ),
        activity.game, activity.level, id = "bulk-attempt", now = 1_700_000_000_000,
        selected = activity,
        activityRef = ReadingActivityRef("selected-test-content", activity.game, activity.level, activity.title)
    ).also { assertEquals(activity, it.active?.content) }

    private fun answer(data: ReadingData, key: String = "answer"): ReadingData {
        val attempt = requireNotNull(data.active)
        val question = requireNotNull(ReadingEngine.question(attempt))
        question.choices.indices.forEach { index ->
            val candidate = readingAnswer(data, index, key)
            assertEquals("${attempt.contentId}: $key choice $index", index == question.answer,
                requireNotNull(candidate.active).earned(key))
        }
        return readingAnswer(data, question.answer, key)
    }

    private fun partnerSentence(idea: String, index: Int): String = when (index) {
        0 -> "$idea began an adventure."
        1 -> "The adventure led to $idea."
        2 -> "The character $idea."
        else -> "At last, the character $idea."
    }

    private fun completeEveryPath(activity: ReadingActivity) {
        val variants = when (activity.game) {
            ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> activity.validOrders.size
            ReadingGame.CHAPTER -> activity.branches.size
            else -> 1
        }
        repeat(variants) { variant ->
            var data = fresh(activity)
            when (activity.game) {
                ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.DETECTIVE -> data = answer(data)
                ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
                    val order = activity.validOrders[variant]
                    data = ReadingEngine.submit(readingOrder(data, order), "order")
                    assertEquals(activity.id, order, data.active?.order)
                    assertTrue(activity.id, requireNotNull(data.active).earned("order"))
                    data = answer(data, if (activity.game == ReadingGame.SCRAMBLE) "bonus" else "answer")
                }
                ReadingGame.CHAPTER -> {
                    data = ReadingEngine.branch(data, variant)
                    assertEquals(variant, data.active?.branch)
                    assertEquals(data, ReadingEngine.branch(data, (variant + 1) % activity.branches.size))
                    data = answer(data)
                }
                ReadingGame.STUDIO -> ReadingEngine.studioKeys.forEachIndexed { index, key ->
                    val choices = activity.writingChoices[index]
                    choices.forEach { choice ->
                        val candidate = ReadingEngine.submit(ReadingEngine.field(data, key, choice), key)
                        assertTrue("${activity.id}: $key", requireNotNull(candidate.active).earned(key))
                        assertEquals(activity.id, choice, candidate.active?.fields?.get(key))
                    }
                    data = ReadingEngine.submit(ReadingEngine.field(data, key, choices.first()), key)
                }
                ReadingGame.PARTNERS -> {
                    data = ReadingEngine.partner(data, "reader-b")
                    activity.writingChoices.forEachIndexed { index, choices ->
                        fun turn(idea: String) = ReadingEngine.submit(
                            ReadingEngine.field(data, "turn", partnerSentence(idea, index), expectedTurn = index),
                            "turn", expectedTurn = index)
                        choices.forEach { choice ->
                            val candidate = turn(choice)
                            val saved = requireNotNull(candidate.active).turns[index]
                            assertEquals(activity.id, partnerSentence(choice, index), saved.text)
                            assertEquals(if (index % 2 == 0) "reader-a" else "reader-b", saved.readerId)
                        }
                        data = turn(choices.first())
                    }
                    data = ReadingEngine.reread(readingOrder(data, listOf(0, 1, 2, 3)))
                    data = ReadingEngine.submit(data, "shared")
                }
            }
            if (activity.game == ReadingGame.DETECTIVE) {
                activity.passage.indices.forEach { index ->
                    val candidate = ReadingEngine.submit(
                        ReadingEngine.field(data, "evidence", index.toString()), "evidence")
                    assertEquals("${activity.id}: evidence $index", index == activity.evidence,
                        requireNotNull(candidate.active).earned("evidence"))
                }
                data = ReadingEngine.submit(
                    ReadingEngine.field(data, "evidence", activity.evidence.toString()), "evidence")
            }
            assertTrue(activity.id, ReadingEngine.ready(requireNotNull(data.active)))
            data = ReadingEngine.finish(data, 1_700_000_000_200)
            assertNotNull(activity.id, data.active?.finishedAt)
            assertEquals(activity.id, 1, data.attempts.size)
            assertTrue(activity.id, requireNotNull(data.active).checkpoints.values.all { it.earned && it.tries == 1 })
            assertEquals(data, ReadingEngine.finish(data))
            assertEquals(activity.id, 1, data.completions("reader-a"))
            assertEquals(activity.id, when (activity.game) {
                ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE, ReadingGame.DETECTIVE -> 2
                ReadingGame.STUDIO -> 4
                else -> 1
            }, data.checkpointCount("reader-a"))
            assertTrue(activity.id, requireNotNull(data.active).checkpoints.values.none { it.rewardGranted })
            if (activity.game == ReadingGame.PARTNERS) {
                assertEquals(1, data.completions("reader-b"))
                assertEquals(1, data.checkpointCount("reader-b"))
                assertEquals(1, data.sharedCompletions("reader-a"))
                assertEquals(1, data.sharedCompletions("reader-b"))
            }
            ReadingEngine.validate(data)
        }
    }

    @Test fun v4HasOneThousandDistinctAnswerActivitiesPerPoolAndEveryActivityCanBeCompleted() {
        val pack = ReadingRepositoryPack()
        val archived = ReadingRepositoryPack("manifest-v2.json")
        val oldActivities = archived.activities
        val oldIds = oldActivities.map { it.id }.toSet()
        val manifest = pack.manifest
        assertEquals("v4", manifest.version)
        assertEquals(1, manifest.schemaVersion)
        assertEquals(24, expectedCounts.size)
        assertEquals(expectedCounts, manifest.counts)
        assertEquals(24_000, manifest.counts.values.sum())
        assertTrue(manifest.counts.values.sum() <= ReadingPackImporter.MAX_ACTIVITIES)
        assertTrue(manifest.bytes in 1..ReadingPackImporter.MAX_PACK_BYTES)
        assertEquals(manifest.bytes, pack.contentFile.length())
        assertEquals("activities-${manifest.sha256}.jsonl", manifest.file)
        assertEquals(72, oldActivities.size)
        DataInputStream(pack.contentFile.inputStream()).use { input ->
            val prefix = ByteArray(archived.bytes.size)
            input.readFully(prefix)
            assertArrayEquals(archived.bytes, prefix)
        }
        val checksum = MessageDigest.getInstance("SHA-256")
        DigestInputStream(pack.contentFile.inputStream(), checksum).use { input ->
            val buffer = ByteArray(8192)
            while (input.read(buffer) != -1) { /* Hash without retaining the full pack. */ }
        }
        assertEquals(manifest.sha256, checksum.digest().joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        })
        val ids = mutableSetOf<String>()
        val counts = mutableMapOf<String, Int>()
        val fingerprints = mutableMapOf<String, String>()
        val poolAnswers = mutableMapOf<String, MutableSet<String>>()
        val digest = MessageDigest.getInstance("SHA-256")
        var records = 0
        var bytes = 0L
        pack.forEachRecord { line, activity ->
            val size = line.toByteArray(Charsets.UTF_8).size
            assertTrue(activity.id, size in 1..ReadingPackImporter.MAX_LINE_BYTES)
            bytes += size + 1L
            ReadingPackValidator.validate(activity)
            assertTrue("Duplicate ID: ${activity.id}", ids.add(activity.id))
            val key = "${activity.game.name}:${activity.level.name}"
            counts[key] = counts.getOrDefault(key, 0) + 1
            answers(activity).forEach { answer ->
                assertTrue("Repeated answer in $key: $answer (${activity.id})",
                    poolAnswers.getOrPut(key) { mutableSetOf() }.add(answer))
            }
            if (records < oldActivities.size) assertEquals(oldActivities[records], activity)
            val fingerprint = Base64.getEncoder().encodeToString(
                digest.digest(substantive(activity).toByteArray(Charsets.UTF_8)))
            val previous = fingerprints.putIfAbsent(fingerprint, activity.id)
            if (previous != null) {
                assertTrue("Substantive duplicate: $previous and ${activity.id}",
                    previous in oldIds && activity.id in oldIds)
            }
            completeEveryPath(activity)
            records++
        }
        assertEquals(24_000, records)
        assertEquals(24_000, ids.size)
        assertEquals(expectedCounts, counts)
        assertEquals(manifest.bytes, bytes)
        assertTrue(ids.containsAll(oldIds))
        ReadingGame.entries.forEach { game ->
            ReadingLevel.entries.forEach { level ->
                val expected = when (game) {
                    ReadingGame.CHAPTER -> 2000
                    ReadingGame.STUDIO, ReadingGame.PARTNERS -> 0
                    else -> 1000
                }
                assertEquals("$game:$level", expected, poolAnswers["$game:$level"].orEmpty().size)
            }
        }
    }

    private fun install(pack: ReadingRepositoryPack) = runBlocking {
        pack.importer(store.catalog, cache).importPack()
    }.also { assertTrue(cache.listFiles().orEmpty().isEmpty()) }

    private fun persist(data: ReadingData): ReadingData {
        store.save(data)
        return store.load().also { assertEquals(store.prepare(data), it) }
    }

    private fun reopen(): ReadingData {
        database.close()
        database = ReadingDatabase(application, "bulk-reading.db")
        store = ReadingSqliteStore(application, database, legacy)
        return store.load()
    }

    private fun rows(sql: String): List<List<String?>> =
        database.readableDatabase.rawQuery(sql, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) })
            }
        }

    private fun scalar(sql: String): Long = database.readableDatabase.rawQuery(sql, null).use {
        assertTrue(it.moveToFirst())
        it.getLong(0)
    }

    private fun cycles() = rows("SELECT * FROM reading_cycles ORDER BY reader_id,game,level")
    private fun presented() = rows("SELECT * FROM reading_presented ORDER BY reader_id,game,level,activity_id")

    @Test fun realV2ToV4ImportPreservesProgressAndExhaustsAThousandItemPoolAcrossReopening() {
        val archived = ReadingRepositoryPack("manifest-v2.json")
        assertEquals("v2", install(archived).version)
        val oldActivities = archived.activities
        val oldIds = oldActivities.map { it.id }.toSet()
        val oldRevisions = oldActivities.associate { it.id to requireNotNull(store.catalog.latest(it.id)).revision }
        var data = ReadingEngine.addProfile(store.load(), "Ari", "reader-a")
        data = ReadingEngine.level(data, ReadingLevel.SUMMIT)
        data = ReadingEngine.addProfile(data, "Bo", "reader-b")
        data = ReadingEngine.level(data, ReadingLevel.TRAIL)
        data = persist(ReadingEngine.selectProfile(data, "reader-a"))
        data = store.startBatch(data, ReadingGame.MISSING, ReadingLevel.SPROUT, 3)
        data = persist(readingSolve(data))
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        data = persist(ReadingEngine.hint(readingAnswer(data, readingWrong(data))))
        val missingDraft = requireNotNull(data.active)
        data = store.start(data, ReadingGame.PARTNERS, ReadingLevel.SUMMIT, false)
        data = persist(readingSolve(data))
        data = store.start(data, ReadingGame.CHAPTER, ReadingLevel.SUMMIT, false)
        data = persist(ReadingEngine.branch(data, 1))
        data = store.start(data, ReadingGame.SCRAMBLE, ReadingLevel.TRAIL, false)
        val tile = requireNotNull(data.active).content.validOrders.first().first()
        data = persist(ReadingEngine.placeTile(data, tile))
        val scrambleDraft = requireNotNull(data.active)
        val before = data
        val oldCycles = cycles()
        val oldPresented = presented()
        assertEquals(2, before.completions("reader-a"))
        assertEquals(1, before.completions("reader-b"))
        assertEquals(2, before.checkpointCount("reader-a"))
        assertEquals(1, before.checkpointCount("reader-b"))
        assertEquals(1, before.sharedCompletions("reader-a"))
        assertEquals(1, before.stories.size)
        assertTrue(before.attempts.all { it.activityRef != null })
        assertEquals(2L, scalar("SELECT cycle FROM reading_cycles WHERE game='MISSING' AND level='SPROUT'"))

        val pack = ReadingRepositoryPack()
        val status = install(pack)
        assertEquals(ReadingPackStatus("v4", expectedCounts), status)
        assertEquals(before, store.load())
        assertEquals(before, reopen())
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        assertEquals(24_000L, scalar("SELECT COUNT(*) FROM reading_content"))
        assertEquals(24_000L, scalar("SELECT COUNT(*) FROM reading_current"))
        oldActivities.forEach { activity ->
            assertEquals(oldRevisions.getValue(activity.id), store.catalog.latest(activity.id)?.revision)
            assertEquals(activity, store.catalog.find(activity.id, oldRevisions.getValue(activity.id)))
        }
        val samples = ReadingGame.entries.flatMap { game ->
            ReadingLevel.entries.map { level ->
                val candidates = store.catalog.candidates(game, level)
                assertEquals("${game.name}:${level.name}", 1000, candidates.size)
                val id = candidates.first { it !in oldIds }
                val sample = requireNotNull(store.catalog.latest(id))
                assertEquals(game, sample.activity.game)
                assertEquals(level, sample.activity.level)
                assertEquals(sample.activity, store.catalog.find(id, sample.revision))
                ReadingPackValidator.validate(sample.activity)
                sample
            }
        }
        assertEquals(24, samples.map { it.activity.id }.distinct().size)
        data = store.start(data, ReadingGame.SCRAMBLE, ReadingLevel.TRAIL, false)
        assertEquals(scrambleDraft, data.active)
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertEquals(missingDraft, data.active)
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())

        val beforeReimport = data
        assertEquals(status, install(pack))
        assertEquals(beforeReimport, reopen())
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        assertEquals(24_000L, scalar("SELECT COUNT(*) FROM reading_content"))
        assertEquals(24_000L, scalar("SELECT COUNT(*) FROM reading_current"))
        samples.forEach { assertEquals(it, store.catalog.latest(it.activity.id)) }
        assertEquals(listOf(listOf(pack.manifest.sha256)), rows("SELECT checksum FROM reading_pack"))

        val pool = store.catalog.candidates(ReadingGame.CAPTION, ReadingLevel.SUMMIT).toSet()
        val offset = data.attempts.size
        data = store.startBatch(data, ReadingGame.CAPTION, ReadingLevel.SUMMIT, 500)
        assertEquals(500, data.attempts.drop(offset).map { it.contentId }.distinct().size)
        val halfwayDraft = requireNotNull(data.active)
        val halfwayCycles = cycles()
        val halfwayPresented = presented()
        assertEquals(data, reopen())
        data = store.start(data, ReadingGame.CAPTION, ReadingLevel.SUMMIT, false)
        assertEquals(halfwayDraft, data.active)
        assertEquals(halfwayCycles, cycles())
        assertEquals(halfwayPresented, presented())
        data = store.startBatch(data, ReadingGame.CAPTION, ReadingLevel.SUMMIT, 500)
        val firstCycle = data.attempts.drop(offset).map { it.contentId }
        assertEquals(1000, firstCycle.size)
        assertEquals(1000, firstCycle.distinct().size)
        assertEquals(pool, firstCycle.toSet())
        assertEquals(1L, scalar("SELECT cycle FROM reading_cycles WHERE game='CAPTION' AND level='SUMMIT'"))
        assertEquals(1000L, scalar("SELECT COUNT(*) FROM reading_presented WHERE game='CAPTION' AND level='SUMMIT'"))

        data = store.start(data, ReadingGame.CAPTION, ReadingLevel.SUMMIT, true)
        assertNotEquals(firstCycle.last(), data.active?.contentId)
        assertTrue(data.active?.contentId in pool)
        assertEquals(2L, scalar("SELECT cycle FROM reading_cycles WHERE game='CAPTION' AND level='SUMMIT'"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented WHERE game='CAPTION' AND level='SUMMIT' AND cycle=2"))
        assertEquals(999L, scalar("SELECT COUNT(*) FROM reading_presented WHERE game='CAPTION' AND level='SUMMIT' AND cycle=1"))
        assertEquals(oldCycles, cycles().filterNot { it[1] == "CAPTION" && it[2] == "SUMMIT" })
        assertEquals(oldPresented, presented().filterNot { it[1] == "CAPTION" && it[2] == "SUMMIT" })
        assertEquals(before.stories, data.stories)
        assertEquals(before.completions("reader-a"), data.completions("reader-a"))
        assertEquals(before.completions("reader-b"), data.completions("reader-b"))
        assertEquals(before.checkpointCount("reader-a"), data.checkpointCount("reader-a"))
        assertEquals(before.checkpointCount("reader-b"), data.checkpointCount("reader-b"))
        assertEquals(before.sharedCompletions("reader-a"), data.sharedCompletions("reader-a"))
        assertEquals(1, data.attempts.count { it.loadedContent != null })
        assertEquals(data, reopen())
        assertEquals(status, store.catalog.status())
    }

    @Test fun v3ToV4ReplacesRepeatedAnswersWithoutAddingIdsOrChangingSavedAttempts() {
        val archived = ReadingRepositoryPack("manifest-v3.json")
        val pack = ReadingRepositoryPack()
        val previous = archived.activities.associateBy { it.id }
        val current = pack.activities.associateBy { it.id }
        assertEquals(previous.keys, current.keys)
        val revised = previous.keys.filter { previous.getValue(it) != current.getValue(it) }
        assertTrue(revised.isNotEmpty())
        val originalCounts = previous.values.flatMap { activity ->
            answers(activity).map { Triple(activity.game, activity.level, it) }
        }.groupingBy { it }.eachCount()
        revised.forEach { id ->
            val activity = previous.getValue(id)
            assertTrue("Unrelated activity changed: $id", answers(activity).any {
                originalCounts.getValue(Triple(activity.game, activity.level, it)) > 1
            })
        }
        val wide = previous.values.filter {
            it.game == ReadingGame.MISSING && it.level == ReadingLevel.TRAIL && answers(it) == listOf("wide")
        }
        assertTrue(wide.size > 1)
        assertEquals(wide.first(), current.getValue(wide.first().id))
        assertEquals(1, current.values.count {
            it.game == ReadingGame.MISSING && it.level == ReadingLevel.TRAIL && "wide" in answers(it)
        })
        val changedWide = wide.filter { it.id in revised }.take(2)
        assertEquals(2, changedWide.size)

        assertEquals("v3", install(archived).version)
        var data = persist(ReadingEngine.addProfile(store.load(), "Ari", "reader-a"))
        data = store.startBatch(data, ReadingGame.MISSING, ReadingLevel.TRAIL, 1000)
        fun attemptId(activity: ReadingActivity) = data.attempts.first { it.contentId == activity.id }.id
        data = persist(ReadingEngine.resume(data, attemptId(changedWide.first())))
        data = persist(readingSolve(data))
        val completed = requireNotNull(data.active)
        data = persist(ReadingEngine.resume(data, attemptId(changedWide.last())))
        data = persist(ReadingEngine.hint(readingAnswer(data, readingWrong(data))))
        val draft = requireNotNull(data.active)
        val before = data
        val oldCycles = cycles()
        val oldPresented = presented()

        val status = install(pack)
        assertEquals(ReadingPackStatus("v4", expectedCounts), status)
        assertEquals(before, reopen())
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        assertEquals(24_000L, scalar("SELECT COUNT(*) FROM reading_current"))
        assertEquals(24_000L + revised.size, scalar("SELECT COUNT(*) FROM reading_content"))
        listOf(completed, draft).forEach { attempt ->
            assertEquals(previous.getValue(attempt.contentId),
                store.catalog.find(attempt.contentId, requireNotNull(attempt.activityRef).revision))
            assertEquals(current.getValue(attempt.contentId), store.catalog.latest(attempt.contentId)?.activity)
        }
        assertEquals(status, install(pack))
        assertEquals(before, reopen())
        assertEquals(24_000L + revised.size, scalar("SELECT COUNT(*) FROM reading_content"))
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.TRAIL, false)
        assertEquals(draft, data.active)
        data = persist(readingSolve(data))
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.TRAIL, true)
        val next = requireNotNull(data.active)
        assertEquals(current.getValue(next.contentId), next.content)
        assertEquals(2L, scalar("SELECT cycle FROM reading_cycles WHERE game='MISSING' AND level='TRAIL'"))
        assertEquals(data, reopen())
    }
}
