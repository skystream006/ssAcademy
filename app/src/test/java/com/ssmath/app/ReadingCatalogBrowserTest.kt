package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
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
class ReadingCatalogBrowserTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val databaseName = "reading-browser-catalog.db"
    private lateinit var database: ReadingDatabase
    private lateinit var catalog: ReadingCatalog
    private val first get() = ReadingContent.activities.first()

    @Before fun setup() {
        application.deleteDatabase(databaseName)
        database = ReadingDatabase(application, databaseName)
        catalog = ReadingCatalog(database)
    }

    @After fun cleanup() {
        database.close()
        application.deleteDatabase(databaseName)
    }

    private fun install(activities: List<ReadingActivity>, version: String = "test-1") = runBlocking {
        readingTestImporter(catalog, application.cacheDir, activities, version).importPack()
    }

    @Test fun pagingReturnsOnlyCurrentInstalledRowsInStableIdOrderAndBoundsPayloadReads() {
        val activities = (0..84).map { first.copy(id = "catalog-%03d".format(it), title = "Installed title $it") }
        install(activities.reversed())
        catalog.insertLegacy(first.copy(id = "archive-only"))
        val pages = (0..2).map { catalog.browse(offset = it * 40) }
        assertEquals(listOf(40, 40, 5), pages.map { it.entries.size })
        assertTrue(pages.all { it.total == 85 })
        assertEquals(activities.map { it.id }, pages.flatMap { it.entries }.map { it.id })
        assertEquals(activities.map { it.title }, pages.flatMap { it.entries }.map { it.title })
        assertTrue(catalog.browse(offset = 85).entries.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { catalog.browse(limit = 101) }
        assertThrows(IllegalArgumentException::class.java) { catalog.browse(limit = 0) }
        assertThrows(IllegalArgumentException::class.java) { catalog.browse(offset = -1) }

        // An unreadable record outside this page must not be decoded while opening the first page.
        database.writableDatabase.execSQL("INSERT INTO reading_content VALUES('zz-broken','revision','not JSON')")
        database.writableDatabase.execSQL("INSERT INTO reading_current VALUES('zz-broken','MISSING','SPROUT','revision')")
        assertEquals(40, catalog.browse().entries.size)
        assertEquals(86, catalog.browse().total)
        assertThrows(Exception::class.java) { catalog.browse(offset = 85) }
    }

    @Test fun filtersCombineGameAndLevelAndDetailUsesTheRevisionSelectedInThePage() {
        install(ReadingContent.activities)
        val page = catalog.browse(ReadingGame.MISSING, ReadingLevel.SPROUT)
        assertEquals(3, page.total)
        assertTrue(page.entries.all { it.game == ReadingGame.MISSING && it.level == ReadingLevel.SPROUT })
        assertEquals(6, catalog.browse(game = ReadingGame.DETECTIVE).total)
        assertEquals(15, catalog.browse(level = ReadingLevel.TRAIL).total)
        assertEquals(0, catalog.browse(level = ReadingLevel.SUMMIT).total)
        val selected = page.entries.first { it.id == first.id }
        install(listOf(first.copy(title = "Updated installed title")), "test-2")
        assertEquals("Updated installed title", catalog.browse(ReadingGame.MISSING, ReadingLevel.SPROUT)
            .entries.first { it.id == first.id }.title)
        assertEquals(first, catalog.find(selected.id, selected.revision))
    }

    @Test fun independentReaderClosesItsConnectionsWithoutMutatingProgressOrClosingTheGameDatabase() = runBlocking {
        install(listOf(first))
        database.writableDatabase.execSQL("INSERT INTO reading_state(singleton,payload) VALUES(1,'saved progress')")
        val status = catalog.status()
        val reader = ReadingDatabaseReader(application, databaseName)
        val entry = reader.page(null, null, 0).entries.single()
        assertEquals(first, reader.activity(entry.id, entry.revision))
        repeat(3) { assertEquals(1, reader.page(first.game, first.level, 0).total) }
        assertEquals(status, catalog.status())
        database.readableDatabase.rawQuery("SELECT payload FROM reading_state", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("saved progress", it.getString(0))
        }
        val updated = first.copy(title = "Game database still open")
        install(listOf(updated), "test-2")
        assertEquals(readingPackJson.encodeToString(updated),
            readingPackJson.encodeToString(reader.activity(updated.id, requireNotNull(catalog.latest(updated.id)).revision)))
    }
}
