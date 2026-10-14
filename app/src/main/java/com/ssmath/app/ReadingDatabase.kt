package com.ssmath.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.encodeToString

class ReadingDatabase(context: Context, name: String = "reading.db") :
    SQLiteOpenHelper(context.applicationContext, name, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE reading_content (
            id TEXT NOT NULL, revision TEXT NOT NULL, payload TEXT NOT NULL,
            PRIMARY KEY(id, revision))""")
        db.execSQL("""CREATE TABLE reading_current (
            id TEXT PRIMARY KEY NOT NULL, game TEXT NOT NULL, level TEXT NOT NULL, revision TEXT NOT NULL)""")
        db.execSQL("CREATE INDEX reading_current_group ON reading_current(game, level, id)")
        db.execSQL("""CREATE TABLE reading_pack (
            singleton INTEGER PRIMARY KEY CHECK(singleton=1), version TEXT NOT NULL, checksum TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE reading_state (
            singleton INTEGER PRIMARY KEY CHECK(singleton=1), payload TEXT NOT NULL,
            normalized INTEGER NOT NULL DEFAULT 1)""")
        createHistoryTables(db)
        db.execSQL("""CREATE TABLE reading_cycles (
            reader_id TEXT NOT NULL, game TEXT NOT NULL, level TEXT NOT NULL,
            cycle INTEGER NOT NULL, last_id TEXT, PRIMARY KEY(reader_id,game,level))""")
        db.execSQL("""CREATE TABLE reading_presented (
            reader_id TEXT NOT NULL, game TEXT NOT NULL, level TEXT NOT NULL,
            activity_id TEXT NOT NULL, cycle INTEGER NOT NULL,
            PRIMARY KEY(reader_id,game,level,activity_id))""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion == 1 && newVersion == 2) {
            "A non-destructive reading database migration is required: $oldVersion to $newVersion"
        }
        db.execSQL("ALTER TABLE reading_state ADD COLUMN normalized INTEGER NOT NULL DEFAULT 0")
        createHistoryTables(db)
    }

    private fun createHistoryTables(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE reading_attempts (
            id TEXT PRIMARY KEY NOT NULL, position INTEGER NOT NULL, payload TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE reading_stories (
            attempt_id TEXT PRIMARY KEY NOT NULL, position INTEGER NOT NULL, payload TEXT NOT NULL)""")
    }
}

data class CatalogActivity(val activity: ReadingActivity, val revision: String)
data class ReadingPackStatus(val version: String? = null, val counts: Map<String, Int> = emptyMap())

class ReadingCatalog(val database: ReadingDatabase) {
    fun find(id: String, revision: String): ReadingActivity =
        database.readableDatabase.rawQuery(
            "SELECT payload FROM reading_content WHERE id=? AND revision=?", arrayOf(id, revision)
        ).use { cursor ->
            require(cursor.moveToFirst()) { "The saved reading activity is unavailable: $id" }
            readingPackJson.decodeFromString<ReadingActivity>(cursor.getString(0))
        }

    fun latest(id: String): CatalogActivity? =
        database.readableDatabase.rawQuery(
            """SELECT c.payload, r.revision FROM reading_current r
                JOIN reading_content c ON c.id=r.id AND c.revision=r.revision WHERE r.id=?""", arrayOf(id)
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else CatalogActivity(
                readingPackJson.decodeFromString<ReadingActivity>(cursor.getString(0)), cursor.getString(1))
        }

    // Selection only needs identifiers; payloads are fetched after choosing an activity.
    fun candidates(game: ReadingGame, level: ReadingLevel): List<String> =
        database.readableDatabase.rawQuery(
            "SELECT id FROM reading_current WHERE game=? AND level=? ORDER BY id",
            arrayOf(game.name, level.name)
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    fun status(): ReadingPackStatus {
        val db = database.readableDatabase
        val version = db.rawQuery("SELECT version FROM reading_pack WHERE singleton=1", null).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        val counts = db.rawQuery(
            "SELECT game, level, COUNT(*) FROM reading_current GROUP BY game, level", null
        ).use { cursor -> buildMap {
            while (cursor.moveToNext()) put("${cursor.getString(0)}:${cursor.getString(1)}", cursor.getInt(2))
        } }
        return ReadingPackStatus(version, counts)
    }

    fun insertLegacy(activity: ReadingActivity): String {
        ReadingPackValidator.validate(activity)
        val payload = readingPackJson.encodeToString(activity)
        val revision = readingSha256(payload.toByteArray(Charsets.UTF_8))
        val db = database.writableDatabase
        val ownTransaction = !db.inTransaction()
        if (ownTransaction) db.beginTransaction()
        try {
            requireCompatible(db, activity)
            db.execSQL("INSERT OR IGNORE INTO reading_content(id,revision,payload) VALUES(?,?,?)",
                arrayOf(activity.id, revision, payload))
            if (ownTransaction) db.setTransactionSuccessful()
        } finally {
            if (ownTransaction) db.endTransaction()
        }
        return revision
    }

    internal fun requireCompatible(db: SQLiteDatabase, activity: ReadingActivity) {
        db.rawQuery("SELECT game,level FROM reading_current WHERE id=?", arrayOf(activity.id)).use {
            if (it.moveToFirst()) {
                require(it.getString(0) == activity.game.name && it.getString(1) == activity.level.name) {
                    "An activity cannot change its game or level: ${activity.id}"
                }
                return
            }
        }
        db.rawQuery("SELECT payload FROM reading_content WHERE id=? LIMIT 1", arrayOf(activity.id)).use {
            if (it.moveToFirst()) {
                val previous = readingPackJson.decodeFromString<ReadingActivity>(it.getString(0))
                require(previous.game == activity.game && previous.level == activity.level) {
                    "An archived activity cannot change its game or level: ${activity.id}"
                }
            }
        }
    }
}
