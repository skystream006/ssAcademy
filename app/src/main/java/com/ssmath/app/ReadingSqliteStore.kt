package com.ssmath.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Content, selection reservations, and the progress ledger share one commit boundary. */
class ReadingSqliteStore(
    context: Context,
    val database: ReadingDatabase = ReadingDatabase(context),
    private val legacyFile: File = File(context.filesDir, "reading_private.json")
) : ReadingStorage {
    val catalog = ReadingCatalog(database)
    private val json = Json { encodeDefaults = true }
    private var loaded = false
    private var expectedState: String? = null

    @Synchronized
    override fun load(): ReadingData {
        loaded = false
        val result = transaction {
            var migratedState = false
            val existing = readState { migratedState = true }
            val data = existing ?: run {
                val legacy = if (legacyFile.exists() || File(legacyFile.path + ".bak").exists())
                    ReadingStore(legacyFile).load() else ReadingData()
                val migrated = legacy.copy(attempts = legacy.attempts.map { attempt ->
                    val activity = attempt.content
                    val revision = catalog.insertLegacy(activity)
                    attempt.copy(activityRef = ReadingActivityRef(revision, activity.game, activity.level, activity.title))
                })
                // Existing assignments count as seen when the corresponding pack is imported later.
                migrated.attempts.forEach { a ->
                    execSQL(
                        "INSERT OR IGNORE INTO reading_cycles(reader_id,game,level,cycle,last_id) VALUES(?,?,?,1,NULL)",
                        arrayOf(a.ownerId, a.game.name, a.level.name)
                    )
                    record(a.ownerId, a.game, a.level, a.contentId, 1)
                }
                writeState(migrated)
                migrated
            }
            ReadingEngine.validate(data, ::resolve)
            if (existing != null && (!normalizedState() || migratedState)) writeState(data)
            prepare(data)
        }
        expectedState = encode(result)
        loaded = true
        return result
    }

    @Synchronized
    override fun save(data: ReadingData) {
        transaction {
            check(loaded) { "Load reading data successfully before saving." }
            require(encode(requireNotNull(readState())) == expectedState) {
                "Reading progress changed. Reopen reading before saving."
            }
            ReadingEngine.validate(data, ::resolve)
            writeState(data)
        }
        expectedState = encode(data)
    }

    override fun prepare(data: ReadingData): ReadingData = data.copy(attempts = data.attempts.map {
        if (it.id == data.profile?.activeAttemptId) resolve(it) else it.copy(loadedContent = null)
    })

    @Synchronized
    override fun review(attempt: ReadingAttempt): ReadingAttempt = resolve(attempt)

    override fun start(data: ReadingData, game: ReadingGame, level: ReadingLevel, newAttempt: Boolean): ReadingData =
        startBatch(data, game, level, 1, newAttempt)

    @Synchronized
    fun startBatch(
        data: ReadingData, game: ReadingGame, level: ReadingLevel, count: Int, newAttempt: Boolean = true
    ): ReadingData {
        val result = transaction {
            check(loaded) { "Load reading data successfully before starting." }
            require(count in 1..1000)
            // Never accept a stale allocation, including from a second activity/window.
            require(encode(data) == encode(requireNotNull(readState()))) {
                "Reading progress changed. Reopen reading before starting another activity."
            }
            val owner = requireNotNull(data.profile)
            val draft = ReadingEngine.draft(data, game, level)
            if (draft != null && !newAttempt) {
                val resumed = prepare(ReadingEngine.resume(data, draft.id))
                writeState(resumed)
                return@transaction resumed
            }
            val assigned = allocate(owner.id, game, level, count)
            val first = assigned.first()
            var next = ReadingEngine.start(data, game, level, newAttempt = true, id = first.id,
                now = first.startedAt, selected = first.content, activityRef = first.activityRef)
            next = next.copy(attempts = next.attempts + assigned.drop(1), profiles = next.profiles.map {
                if (it.id == owner.id) it.copy(activeAttemptId = assigned.last().id) else it
            })
            ReadingEngine.validate(next, ::resolve)
            writeState(next)
            prepare(next)
        }
        expectedState = encode(result)
        return result
    }

    @Synchronized
    override fun startSession(data: ReadingData, game: ReadingGame, level: ReadingLevel, count: Int,
                              rewardsEnabledAtStart: Boolean, timeLimitMs: Long?): ReadingData {
        val result = transaction {
            check(loaded) { "Load reading data successfully before starting." }
            require(count in listOf(15, 25, 50)) { "Choose 15, 25, or 50 questions." }
            require(encode(data) == encode(requireNotNull(readState()))) {
                "Reading progress changed. Reopen reading before starting another session."
            }
            val now = System.currentTimeMillis()
            val assigned = allocate(requireNotNull(data.profile).id, game, level, count, now)
            val next = ReadingEngine.startSession(data, game, level, count, rewardsEnabledAtStart,
                now = now, timeLimitMs = timeLimitMs, assigned = assigned)
            ReadingEngine.validate(next, ::resolve)
            writeState(next)
            prepare(next)
        }
        expectedState = encode(result)
        return result
    }

    private fun SQLiteDatabase.allocate(ownerId: String, game: ReadingGame, level: ReadingLevel, count: Int,
                                        now: Long = System.currentTimeMillis()): List<ReadingAttempt> =
        List(count) {
            val chosen = select(ownerId, game, level)
            ReadingAttempt(UUID.randomUUID().toString(), ownerId, chosen.activity.id, now,
                activityRef = ReadingActivityRef(chosen.revision, game, level, chosen.activity.title),
                loadedContent = chosen.activity)
        }

    private fun SQLiteDatabase.select(reader: String, game: ReadingGame, level: ReadingLevel): CatalogActivity {
        val keys = arrayOf(reader, game.name, level.name)
        execSQL("INSERT OR IGNORE INTO reading_cycles(reader_id,game,level,cycle,last_id) VALUES(?,?,?,1,NULL)", keys)
        var cycle = 1L
        var last: String? = null
        rawQuery("SELECT cycle,last_id FROM reading_cycles WHERE reader_id=? AND game=? AND level=?", keys).use {
            check(it.moveToFirst())
            cycle = it.getLong(0)
            last = it.getString(1)
        }
        fun unseen(): String? = rawQuery(
            """SELECT c.id FROM reading_current c WHERE c.game=? AND c.level=?
                AND NOT EXISTS(SELECT 1 FROM reading_presented p
                    WHERE p.reader_id=? AND p.game=c.game AND p.level=c.level
                    AND p.activity_id=c.id AND p.cycle=?)
                ORDER BY CASE WHEN c.id=? THEN 1 ELSE 0 END, RANDOM() LIMIT 1""",
            arrayOf(game.name, level.name, reader, cycle.toString(), last.orEmpty())
        ).use { if (it.moveToFirst()) it.getString(0) else null }
        var id = unseen()
        if (id == null) {
            val available = rawQuery("SELECT 1 FROM reading_current WHERE game=? AND level=? LIMIT 1",
                arrayOf(game.name, level.name)).use { it.moveToFirst() }
            require(available) { "Import reading activities in Settings > Admin Menu > Game Settings > Reading Settings." }
            check(cycle < Long.MAX_VALUE)
            cycle++
            id = requireNotNull(unseen())
        }
        record(reader, game, level, id, cycle)
        return requireNotNull(catalog.latest(id))
    }

    private fun SQLiteDatabase.record(reader: String, game: ReadingGame, level: ReadingLevel, id: String, cycle: Long) {
        execSQL(
            "INSERT OR REPLACE INTO reading_presented(reader_id,game,level,activity_id,cycle) VALUES(?,?,?,?,?)",
            arrayOf<Any>(reader, game.name, level.name, id, cycle)
        )
        execSQL("UPDATE reading_cycles SET cycle=?,last_id=? WHERE reader_id=? AND game=? AND level=?",
            arrayOf<Any>(cycle, id, reader, game.name, level.name))
    }

    private fun resolve(attempt: ReadingAttempt): ReadingAttempt {
        val ref = requireNotNull(attempt.activityRef) { "Missing activity revision." }
        val content = attempt.loadedContent ?: catalog.find(attempt.contentId, ref.revision)
        require(content.id == attempt.contentId && content.game == ref.game &&
            content.level == ref.level && content.title == ref.title)
        return attempt.copy(loadedContent = content)
    }

    private fun SQLiteDatabase.normalizedState(): Boolean =
        rawQuery("SELECT normalized FROM reading_state WHERE singleton=1", null).use {
            it.moveToFirst() && it.getInt(0) == 1
        }

    private fun SQLiteDatabase.readState(onMigration: () -> Unit = {}): ReadingData? {
        val bytes = rawQuery("SELECT length(CAST(payload AS BLOB)) FROM reading_state WHERE singleton=1", null).use {
            if (!it.moveToFirst()) return null
            it.getInt(0)
        }
        // Also recovers version-one ledgers larger than Android's CursorWindow limit.
        val output = ByteArrayOutputStream()
        var offset = 1
        while (offset <= bytes) {
            rawQuery("SELECT substr(CAST(payload AS BLOB),?,65536) FROM reading_state WHERE singleton=1",
                arrayOf(offset.toString())).use {
                check(it.moveToFirst())
                output.write(it.getBlob(0))
            }
            offset += 65536
        }
        val header = json.parseToJsonElement(output.toString(Charsets.UTF_8.name())).jsonObject
        fun decode(root: JsonObject): ReadingData {
            val cleaned = ReadingMigration.migrate(root)
            if (cleaned != root) onMigration()
            return json.decodeFromJsonElement(ReadingData.serializer(), cleaned)
        }
        if (!normalizedState()) return decode(header)
        val attempts = rawQuery("SELECT payload FROM reading_attempts ORDER BY position", null).use {
            buildList { while (it.moveToNext()) add(json.parseToJsonElement(it.getString(0))) }
        }
        val stories = rawQuery("SELECT payload FROM reading_stories ORDER BY position", null).use {
            buildList { while (it.moveToNext()) add(json.parseToJsonElement(it.getString(0))) }
        }
        val root = JsonObject(header + mapOf("attempts" to JsonArray(attempts), "stories" to JsonArray(stories)))
        return decode(root)
    }

    private fun SQLiteDatabase.writeState(data: ReadingData) {
        writeRows("reading_attempts", "id", data.attempts.map {
            it.id to json.encodeToString(ReadingAttempt.serializer(), it)
        })
        writeRows("reading_stories", "attempt_id", data.stories.map {
            it.attemptId to json.encodeToString(ReadingStory.serializer(), it)
        })
        execSQL("INSERT OR REPLACE INTO reading_state(singleton,payload,normalized) VALUES(1,?,1)",
            arrayOf(encode(data.copy(attempts = emptyList(), stories = emptyList()))))
    }

    private fun SQLiteDatabase.writeRows(table: String, key: String, rows: List<Pair<String, String>>) {
        val removed = rawQuery("SELECT $key FROM $table", null).use {
            buildSet { while (it.moveToNext()) add(it.getString(0)) }.toMutableSet()
        }
        rows.forEachIndexed { position, (id, payload) ->
            // Each content-free history record stays well below the platform cursor-window limit.
            require(payload.toByteArray(Charsets.UTF_8).size <= 256 * 1024) { "Reading history record is too large." }
            removed.remove(id)
            execSQL("INSERT OR IGNORE INTO $table($key,position,payload) VALUES(?,?,?)",
                arrayOf<Any>(id, position, payload))
            execSQL("UPDATE $table SET position=?,payload=? WHERE $key=? AND (position<>? OR payload<>?)",
                arrayOf<Any>(position, payload, id, position, payload))
        }
        removed.forEach { execSQL("DELETE FROM $table WHERE $key=?", arrayOf(it)) }
    }

    private fun encode(data: ReadingData) = json.encodeToString(ReadingData.serializer(), data)

    private fun <T> transaction(block: SQLiteDatabase.() -> T): T {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val result = db.block()
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }
}
