package com.ssmath.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json

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
            val existing = readState()
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
            var next = data
            repeat(count) {
                val chosen = select(owner.id, game, level)
                val ref = ReadingActivityRef(chosen.revision, game, level, chosen.activity.title)
                next = prepare(ReadingEngine.start(next, game, level, newAttempt = true,
                    id = UUID.randomUUID().toString(), selected = chosen.activity, activityRef = ref))
            }
            ReadingEngine.validate(next, ::resolve)
            writeState(next)
            prepare(next)
        }
        expectedState = encode(result)
        return result
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
            require(available) { "Import reading activities in Settings > Reading Settings." }
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

    private fun SQLiteDatabase.readState(): ReadingData? =
        rawQuery("SELECT payload FROM reading_state WHERE singleton=1", null).use {
            if (it.moveToFirst()) json.decodeFromString<ReadingData>(it.getString(0)) else null
        }

    private fun SQLiteDatabase.writeState(data: ReadingData) {
        execSQL("INSERT OR REPLACE INTO reading_state(singleton,payload) VALUES(1,?)",
            arrayOf(encode(data)))
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
