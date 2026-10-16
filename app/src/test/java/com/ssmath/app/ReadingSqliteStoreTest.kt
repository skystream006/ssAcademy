package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal fun ReadingData.withoutCatalogueRefs() = copy(attempts = attempts.map {
    it.copy(activityRef = null, loadedContent = null)
})

internal fun readingTestImporter(
    catalog: ReadingCatalog, cache: File, activities: List<ReadingActivity>, version: String = "test-1"
): ReadingPackImporter {
    val payload = activities.joinToString("\n", postfix = "\n") { readingPackJson.encodeToString(it) }
        .toByteArray(Charsets.UTF_8)
    val hash = readingSha256(payload)
    val manifest = ReadingPackManifest(1, version, "activities-$hash.jsonl", hash, payload.size.toLong(),
        activities.groupingBy { "${it.game.name}:${it.level.name}" }.eachCount())
    return ReadingPackImporter(catalog, cache, ReadingPackTransport { url, consume ->
        val bytes = if (url == READING_PACK_MANIFEST_URL) readingPackJson.encodeToString(manifest).toByteArray()
            else payload
        bytes.inputStream().use(consume)
    })
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingSqliteStoreTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var database: ReadingDatabase
    private lateinit var store: ReadingSqliteStore
    private val legacy get() = File(application.filesDir, "rotation-legacy.json")
    private val pool get() = ReadingContent.forGame(ReadingGame.MISSING, ReadingLevel.SPROUT)

    @Before fun setup() {
        application.deleteDatabase("rotation.db")
        listOf(legacy, File(legacy.path + ".bak"), File(legacy.path + ".new")).forEach { it.delete() }
        database = ReadingDatabase(application, "rotation.db")
        store = ReadingSqliteStore(application, database, legacy)
    }

    @After fun close() { database.close() }

    private fun install(activities: List<ReadingActivity> = pool, version: String = "test-1") = runBlocking {
        readingTestImporter(store.catalog, application.cacheDir, activities, version).importPack()
    }

    private fun reader(): ReadingData {
        val data = ReadingEngine.addProfile(store.load(), "Ari", "reader-a")
        store.save(data)
        return data
    }

    private fun scalar(sql: String) = database.readableDatabase.rawQuery(sql, null).use {
        check(it.moveToFirst())
        it.getLong(0)
    }

    @Test fun freshInstallIsEmptyAndEmptySelectionDoesNotConsumeAnything() {
        val data = reader()
        assertEquals(ReadingPackStatus(), store.catalog.status())
        val error = assertThrows(IllegalArgumentException::class.java) {
            store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        }
        assertTrue(error.message.orEmpty().contains("Import reading activities"))
        assertEquals(data, store.load())
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_content"))
    }

    @Test fun deletedSessionsStayDeletedAfterRestartWithoutResettingInstalledContentOrRotation() {
        install()
        val data = store.startSession(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, 15, true)
        val sessionId = requireNotNull(data.activeSession).id
        val presented = scalar("SELECT COUNT(*) FROM reading_presented")
        val cycle = scalar("SELECT cycle FROM reading_cycles")
        val contentCount = scalar("SELECT COUNT(*) FROM reading_content")
        val deleted = ReadingEngine.clearHistory(data, "reader-a")
        store.save(deleted)
        database.close()
        database = ReadingDatabase(application, "rotation.db")
        store = ReadingSqliteStore(application, database, legacy)
        val restored = store.load()
        assertEquals(deleted, restored)
        assertEquals(setOf(sessionId), restored.deletedSessionIds)
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_attempts"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_stories"))
        assertEquals(presented, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(cycle, scalar("SELECT cycle FROM reading_cycles"))
        assertEquals(contentCount, scalar("SELECT COUNT(*) FROM reading_content"))
        assertEquals(pool.size, store.catalog.status().counts.values.sum())
        assertEquals(15, store.startSession(restored, ReadingGame.MISSING, ReadingLevel.SPROUT, 15, true).attempts.size)
    }

    @Test fun randomCyclesExhaustPoolAndAvoidImmediateBoundaryRepeats() {
        install()
        val data = store.startBatch(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, 11)
        val ids = data.attempts.map { it.contentId }
        ids.chunked(3).forEach { assertEquals(it.size, it.distinct().size) }
        ids.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        assertEquals(4L, scalar("SELECT cycle FROM reading_cycles"))
        assertEquals(3L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(11, data.attempts.size)
        assertTrue(data.attempts.all { it.open })
        assertEquals(data.attempts.last().id, data.active?.id)
        assertEquals(1, data.attempts.count { it.loadedContent != null })
        assertEquals(data, store.load())
    }

    @Test fun reopenAndResumeKeepCycleAndAssignmentsIncludingSkippedActivities() {
        install()
        var data = store.start(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        val first = requireNotNull(data.active)
        database.close()
        database = ReadingDatabase(application, "rotation.db")
        store = ReadingSqliteStore(application, database, legacy)
        data = store.start(store.load(), ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertEquals(first, data.active)
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented"))
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, true)
        assertTrue(data.attempts.first().abandoned)
        assertNotEquals(first.contentId, data.active?.contentId)
        assertEquals(2L, scalar("SELECT COUNT(*) FROM reading_presented"))
    }

    @Test fun reimportAndAdditionsPreserveCycleAndPriorContentRevisions() {
        install(pool.take(2))
        var data = store.startBatch(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, 2)
        val previous = data.attempts.map { it.contentId }
        install(pool.take(2))
        assertEquals(2L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(2L, scalar("SELECT COUNT(*) FROM reading_content"))
        val changed = pool.first().copy(title = "Updated title", passage = listOf("A new sentence with a ___."))
        install(listOf(changed) + pool.drop(1), "test-2")
        assertEquals(data, store.load())
        assertEquals(1L, scalar("SELECT cycle FROM reading_cycles"))
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, true)
        assertFalse(data.active?.contentId in previous)
        assertEquals(pool.last().id, data.active?.contentId)
        assertEquals(1L, scalar("SELECT cycle FROM reading_cycles"))
        val archived = data.attempts.first { it.contentId == changed.id }
        assertEquals(pool.first(), store.catalog.find(archived.contentId, requireNotNull(archived.activityRef).revision))
        assertEquals(changed, store.catalog.latest(changed.id)?.activity)
        assertEquals(4L, scalar("SELECT COUNT(*) FROM reading_content"))
    }

    @Test fun reviewAfterRestartUsesAssignedRevisionAndKeepsTheCurrentDraftAndRotation() {
        val original = pool.first()
        install(listOf(original))
        var data = store.startSession(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, 15, false, null)
        data = ReadingEngine.submit(ReadingEngine.field(data, "answer", readingWrong(data).toString()), "answer")
        val answered = requireNotNull(data.active)
        assertNotNull(answered.finishedAt)
        data = store.prepare(ReadingEngine.nextSessionActivity(data))
        store.save(data)
        val question = requireNotNull(original.question)
        val changed = original.copy(title = "Updated activity", passage = listOf("A different ___"),
            question = question.copy(prompt = "Updated question?", choices = question.choices.map { "New $it" }))
        install(listOf(changed), "updated")
        database.close()
        database = ReadingDatabase(application, "rotation.db")
        store = ReadingSqliteStore(application, database, legacy)
        val reopened = store.load()
        val saved = reopened.attempts.first()
        assertNull(saved.loadedContent)
        val presented = scalar("SELECT COUNT(*) FROM reading_presented")
        val cycle = scalar("SELECT cycle FROM reading_cycles")
        val review = store.review(saved)
        assertEquals(original, review.content)
        assertEquals(answered, review)
        assertNotEquals(changed.question, review.content.question)
        assertEquals(reopened, store.load())
        assertEquals(data.active?.id, reopened.active?.id)
        assertEquals(presented, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(cycle, scalar("SELECT cycle FROM reading_cycles"))
        assertEquals(1, reopened.attempts.count { it.loadedContent != null })
    }

    @Test fun allGameResponsesAndStoriesRemainReviewableWithoutReactivatingThem() {
        install(ReadingContent.activities)
        var data = reader()
        val completed = mutableListOf<ReadingAttempt>()
        for (game in ReadingContent.activities.map { it.game }.distinct()) {
            data = store.startSession(data, game, ReadingLevel.SPROUT, 15, false, null)
            data = readingSolve(data)
            completed += requireNotNull(data.active)
            data = ReadingEngine.endSession(data, requireNotNull(data.activeSession).id, abandoned = true)
            store.save(data)
        }
        val reopened = store.load()
        for (expected in completed) {
            val stored = reopened.attempts.single { it.id == expected.id }
            val review = store.review(stored)
            assertEquals(expected, review)
            assertEquals(expected.content, review.content)
            assertTrue(ReadingEngine.ready(review))
        }
        assertEquals(1, reopened.stories.size)
        assertEquals(data.stories, reopened.stories)
        assertEquals(reopened, store.load())
    }

    @Test fun singleActivityMayRepeatButEachAttemptAndScoreRemainsSeparate() {
        install(pool.take(1))
        var data = store.start(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        data = readingSolve(data)
        store.save(data)
        val original = data.active
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertEquals(original?.contentId, data.active?.contentId)
        assertNotEquals(original?.id, data.active?.id)
        assertEquals(1, data.checkpointCount("reader-a"))
        assertEquals(original?.checkpoints, data.attempts.first().checkpoints)
        assertTrue(requireNotNull(data.active).checkpoints.isEmpty())
        assertEquals(2L, scalar("SELECT cycle FROM reading_cycles"))
    }

    @Test fun readersGamesAndLevelsHaveIndependentPresentationPools() {
        install(ReadingContent.activities)
        var data = reader()
        data = ReadingEngine.addProfile(data, "Bo", "reader-b")
        store.save(data)
        data = store.startBatch(data, ReadingGame.MISSING, ReadingLevel.SPROUT, 3)
        data = ReadingEngine.selectProfile(data, "reader-a")
        store.save(data)
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.TRAIL, false)
        data = store.start(data, ReadingGame.DETECTIVE, ReadingLevel.SPROUT, false)
        assertEquals(4L, scalar("SELECT COUNT(*) FROM reading_cycles"))
        assertEquals(3L, scalar("SELECT COUNT(*) FROM reading_presented WHERE reader_id='reader-b'"))
        assertEquals(3L, scalar("SELECT COUNT(*) FROM reading_presented WHERE reader_id='reader-a'"))
        assertEquals(1L, scalar("SELECT MAX(cycle) FROM reading_cycles"))
        assertEquals(data, store.load())
    }

    @Test fun failedLedgerWriteRollsBackActivityReservationAndStaleStartsAreRejected() {
        install()
        val data = reader()
        database.writableDatabase.execSQL("""CREATE TRIGGER fail_state BEFORE INSERT ON reading_state
            BEGIN SELECT RAISE(ABORT, 'Storage unavailable'); END""")
        assertThrows(Exception::class.java) {
            store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        }
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_cycles"))
        assertEquals(data, store.load())
        database.writableDatabase.execSQL("DROP TRIGGER fail_state")
        val next = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertThrows(IllegalArgumentException::class.java) {
            store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, true)
        }
        assertEquals(next, store.load())
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented"))
    }

    @Test fun legacyMigrationPreservesDraftsHistoryAndCreditWithoutInstallingAPack() {
        val original = readingSolve(readingFixture())
        ReadingStore(legacy).also { it.load(); it.save(original) }
        val migrated = store.load()
        assertEquals(original, migrated.withoutCatalogueRefs())
        assertEquals(original.active?.content, migrated.active?.content)
        assertEquals(original.checkpointCount("reader-a"), migrated.checkpointCount("reader-a"))
        assertEquals(ReadingPackStatus(), store.catalog.status())
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(original, ReadingStore(legacy).load())
        install()
        val next = store.start(migrated, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertNotEquals(original.active?.contentId, next.active?.contentId)
        assertEquals(original.active?.checkpoints, next.attempts.first().checkpoints)
        assertEquals(next, store.load())
    }

    @Test fun upgradesPurgeRetiredCataloguesAndHistoryWhileKeepingOtherRevisionsAndRotation() {
        for (version in listOf(1, 2, 3)) {
            install()
            val data = store.start(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, false)
            val original = requireNotNull(data.active)
            val raw = legacyReadingPayload(data, activateRetired = true)
            val db = database.writableDatabase
            val payload = readingPackJson.parseToJsonElement(readingPackJson.encodeToString(original.content)).jsonObject
            db.execSQL("UPDATE reading_content SET payload=? WHERE id=? AND revision=?", arrayOf(
                JsonObject(payload + ("branches" to JsonArray(emptyList()))).toString(),
                original.contentId, requireNotNull(original.activityRef).revision))
            for (game in listOf("CHAPTER", "PARTNERS", "CAPTION")) {
                db.execSQL("INSERT INTO reading_content VALUES(?,?,?)", arrayOf(
                    "retired-$game", "retired", """{"game":"$game","branches":[{"text":"Removed content"}]}"""))
                db.execSQL("INSERT INTO reading_content VALUES(?,?,?)", arrayOf(
                    "retired-$game", "archive", """{"game":"$game"}"""))
                repeat(120) { index ->
                    db.execSQL("INSERT INTO reading_content VALUES(?,?,?)", arrayOf(
                        "retired-$game", "archive-$index",
                        """{"game":"$game","branches":[{"text":"${"Retired text ".repeat(1000)}"}]}"""))
                }
                db.execSQL("INSERT INTO reading_current VALUES(?,?,?,?)", arrayOf("retired-$game", game, "SPROUT", "retired"))
                db.execSQL("INSERT INTO reading_cycles VALUES(?,?,?,1,NULL)", arrayOf("reader-a", game, "SPROUT"))
                db.execSQL("INSERT INTO reading_presented VALUES(?,?,?,?,1)",
                    arrayOf("reader-a", game, "SPROUT", "retired-$game"))
            }
            if (version == 1) {
                db.execSQL("DROP TABLE reading_attempts")
                db.execSQL("DROP TABLE reading_stories")
                db.execSQL("DROP TABLE reading_state")
                db.execSQL("CREATE TABLE reading_state(singleton INTEGER PRIMARY KEY,payload TEXT NOT NULL)")
                db.execSQL("INSERT INTO reading_state VALUES(1,?)", arrayOf(raw.toString()))
            } else {
                db.execSQL("DELETE FROM reading_attempts")
                db.execSQL("DELETE FROM reading_stories")
                (raw.getValue("attempts") as JsonArray).forEachIndexed { index, value ->
                    db.execSQL("INSERT INTO reading_attempts VALUES(?,?,?)",
                        arrayOf<Any>(value.jsonObject.getValue("id").jsonPrimitive.content, index, value.toString()))
                }
                (raw.getValue("stories") as JsonArray).forEachIndexed { index, value ->
                    db.execSQL("INSERT INTO reading_stories VALUES(?,?,?)",
                        arrayOf<Any>(value.jsonObject.getValue("attemptId").jsonPrimitive.content, index, value.toString()))
                }
                db.execSQL("UPDATE reading_state SET payload=?", arrayOf(JsonObject(raw + mapOf(
                    "attempts" to JsonArray(emptyList()), "stories" to JsonArray(emptyList()))).toString()))
            }
            db.version = version
            database.close()
            database = ReadingDatabase(application, "rotation.db")
            store = ReadingSqliteStore(application, database, legacy)
            val restored = store.load()
            val expected = data.copy(attempts = data.attempts.map { it.copy(loadedContent = null) },
                profiles = data.profiles.map { it.copy(activeAttemptId = null) }, page = ReadingPage.MENU,
                deletedSessionIds = setOf("session-CHAPTER", "session-PARTNERS", "session-CAPTION"))
            assertEquals(expected, restored)
            assertEquals(original.content, store.review(restored.attempts.single()).content)
            assertEquals(pool.size.toLong(), scalar("SELECT COUNT(*) FROM reading_content"))
            assertEquals(pool.size, store.catalog.browse().total)
            assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_cycles"))
            assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented"))
            assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_attempts"))
            assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_stories"))
            assertEquals(expected, store.load())
            store.save(restored)
            assertEquals(original.id, store.start(restored, ReadingGame.MISSING, ReadingLevel.SPROUT, false).active?.id)
            database.close()
            application.deleteDatabase("rotation.db")
            database = ReadingDatabase(application, "rotation.db")
            store = ReadingSqliteStore(application, database, legacy)
        }
    }

    @Test fun importingLegacyFileDropsRetiredGamesBeforeResolvingTheirContent() {
        val original = readingSolve(readingFixture())
        legacy.writeText(legacyReadingPayload(original).toString())
        val migrated = store.load()
        assertEquals(original.copy(deletedSessionIds = setOf("session-CHAPTER", "session-PARTNERS", "session-CAPTION")),
            migrated.withoutCatalogueRefs())
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_content"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(migrated, store.load())
    }

    @Test fun brokenLegacyFileNeverCreatesAnEmptyReplacementLedger() {
        legacy.writeText("{broken")
        assertThrows(Exception::class.java) { store.load() }
        assertEquals("{broken", legacy.readText())
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_state"))
    }

    @Test fun largeLibraryKeepsOnlyActivePayloadInTheLoadedReadingState() {
        install((1..1000).map { pool.first().copy(id = "question-$it", title = "Activity $it") })
        val data = store.startBatch(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, 25)
        assertEquals(1000L, scalar("SELECT COUNT(*) FROM reading_current"))
        assertEquals(25, data.attempts.map { it.contentId }.distinct().size)
        val restored = store.load()
        assertEquals(1, restored.attempts.count { it.loadedContent != null })
        assertTrue(restored.attempts.all { it.activityRef != null })
        assertEquals(data.accuracy("reader-a"), restored.accuracy("reader-a"))
        assertEquals(data.milestones("reader-a"), restored.milestones("reader-a"))
        val other = restored.attempts.first()
        val resumed = store.prepare(ReadingEngine.resume(restored.copy(
            attempts = restored.attempts.map { it.copy(abandoned = false) }), other.id))
        assertEquals(other.contentId, resumed.active?.content?.id)
        assertEquals(1, resumed.attempts.count { it.loadedContent != null })
    }

    @Test fun largeHistoryUsesBoundedRowsAndMigratesOversizedVersionOneLedgers() {
        install(pool.take(1))
        val solved = readingSolve(store.start(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, false))
        val completed = requireNotNull(solved.active)
        val attempts = (1..3200).map { index ->
            completed.copy(id = "history-${index.toString().padStart(128, '0')}", loadedContent = null)
        }
        val history = solved.copy(attempts = attempts, profiles = solved.profiles.map {
            it.copy(activeAttemptId = attempts.last().id)
        })
        val payload = readingPackJson.encodeToString(history)
        assertTrue(payload.toByteArray().size > 2 * 1024 * 1024)
        store.save(history)
        assertEquals(3200L, scalar("SELECT COUNT(*) FROM reading_attempts"))
        assertTrue(scalar("SELECT MAX(length(CAST(payload AS BLOB))) FROM reading_attempts") < 256 * 1024)
        assertTrue(scalar("SELECT length(CAST(payload AS BLOB)) FROM reading_state") < 65536)
        assertEquals(3200, store.load().checkpointCount("reader-a"))

        // Recreate the v1 shape, including a row too large to fetch into a default CursorWindow.
        database.writableDatabase.execSQL("DROP TABLE reading_attempts")
        database.writableDatabase.execSQL("DROP TABLE reading_stories")
        database.writableDatabase.execSQL("DROP TABLE reading_state")
        database.writableDatabase.execSQL(
            "CREATE TABLE reading_state(singleton INTEGER PRIMARY KEY CHECK(singleton=1),payload TEXT NOT NULL)")
        database.writableDatabase.execSQL("INSERT INTO reading_state VALUES(1,?)", arrayOf(payload))
        database.writableDatabase.version = 1
        database.close()
        database = ReadingDatabase(application, "rotation.db")
        store = ReadingSqliteStore(application, database, legacy)
        val upgraded = store.load()
        assertEquals(3200, upgraded.attempts.size)
        assertEquals(3200, upgraded.checkpointCount("reader-a"))
        assertEquals(1, upgraded.attempts.count { it.loadedContent != null })
        assertEquals(1L, scalar("SELECT normalized FROM reading_state"))
        assertEquals(3200L, scalar("SELECT COUNT(*) FROM reading_attempts"))
        assertEquals(1L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(upgraded, store.load())
    }

    @Test fun sessionBatchReservationsAndLedgerRollbackTogetherOnFailedWrite() {
        install()
        val data = reader()
        database.writableDatabase.execSQL("""CREATE TRIGGER fail_session BEFORE INSERT ON reading_state
            BEGIN SELECT RAISE(ABORT, 'Storage unavailable'); END""")
        assertThrows(Exception::class.java) {
            store.startSession(data, ReadingGame.MISSING, ReadingLevel.SPROUT, 25, true)
        }
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_presented"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_cycles"))
        assertEquals(0L, scalar("SELECT COUNT(*) FROM reading_attempts"))
        assertEquals(data, store.load())
        database.writableDatabase.execSQL("DROP TRIGGER fail_session")
        val started = store.startSession(data, ReadingGame.MISSING, ReadingLevel.SPROUT, 25, true)
        assertEquals(25, started.activeSession?.questionCount)
        assertEquals(25, started.attempts.size)
        assertTrue(started.attempts.all { it.open })
        assertEquals(started.attempts.first().id, started.active?.id)
        assertEquals(started, store.load())
        assertThrows(IllegalArgumentException::class.java) {
            store.startSession(data, ReadingGame.MISSING, ReadingLevel.SPROUT, 25, true)
        }
        assertEquals(started, store.load())
    }

    @Test fun everyAssignedSessionRevisionSurvivesReimportReopenAndResume() {
        install()
        var data = store.startSession(reader(), ReadingGame.MISSING, ReadingLevel.SPROUT, 15, true)
        val original = data
        val revisions = data.attempts.map { it.activityRef }
        install(pool.map { it.copy(title = "Replacement ${it.title}", hint = "Replacement hint") }, "updated")
        database.close()
        database = ReadingDatabase(application, "rotation.db")
        store = ReadingSqliteStore(application, database, legacy)
        data = store.load()
        assertEquals(original, data)
        repeat(15) { index ->
            assertEquals(revisions[index], data.active?.activityRef)
            assertEquals(pool.first { it.id == data.active?.contentId }, data.active?.content)
            data = readingAnswer(data, readingCorrect(data))
            data = ReadingEngine.finish(data)
            store.save(data)
            data = store.load()
            assertNotNull(data.active?.finishedAt)
            assertEquals(1, data.attempts.count { it.loadedContent != null })
            if (index < 14) {
                data = store.prepare(ReadingEngine.nextSessionActivity(data))
                store.save(data)
                data = store.prepare(ReadingEngine.resume(store.load(), requireNotNull(data.activeSession).attemptIds.last()))
                store.save(data)
            }
        }
        assertEquals(revisions, data.attempts.map { it.activityRef })
        assertEquals(15, data.sessionCorrect(requireNotNull(data.activeSession)))
        assertNotNull(data.activeSession?.finishedAt)
        assertEquals(data, store.load())
    }
}
