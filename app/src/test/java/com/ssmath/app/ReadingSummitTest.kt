package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingSummitTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var database: ReadingDatabase
    private lateinit var store: ReadingSqliteStore
    private lateinit var cache: File
    private val legacy get() = File(application.filesDir, "summit-legacy.json")

    @Before fun setup() {
        application.deleteDatabase("summit.db")
        listOf(legacy, File(legacy.path + ".bak"), File(legacy.path + ".new")).forEach { it.delete() }
        cache = File(application.cacheDir, "summit-pack-test").apply { mkdirs() }
        database = ReadingDatabase(application, "summit.db")
        store = ReadingSqliteStore(application, database, legacy)
    }

    @After fun cleanup() {
        database.close()
        application.deleteDatabase("summit.db")
        cache.deleteRecursively()
    }

    private fun install(manifestFile: String = "manifest-v2.json") = runBlocking {
        ReadingRepositoryPack(manifestFile).importer(store.catalog, cache).importPack()
    }

    private fun persist(data: ReadingData): ReadingData {
        store.save(data)
        return store.load().also { assertEquals(store.prepare(data), it) }
    }

    private fun reopen(): ReadingData {
        database.close()
        database = ReadingDatabase(application, "summit.db")
        store = ReadingSqliteStore(application, database, legacy)
        return store.load()
    }

    private fun readers(): ReadingData {
        var data = ReadingEngine.addProfile(store.load(), "Ari", "reader-a")
        data = ReadingEngine.level(data, ReadingLevel.SUMMIT)
        data = ReadingEngine.addProfile(data, "Bo", "reader-b")
        data = ReadingEngine.level(data, ReadingLevel.TRAIL)
        return persist(ReadingEngine.selectProfile(data, "reader-a"))
    }

    private fun rows(sql: String): List<List<String?>> = database.readableDatabase.rawQuery(sql, null).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) })
        }
    }

    private fun cycles() = rows("SELECT * FROM reading_cycles ORDER BY reader_id,game,level")
    private fun presented() = rows("SELECT * FROM reading_presented ORDER BY reader_id,game,level,activity_id")

    private fun answer(data: ReadingData, key: String = "answer"): ReadingData {
        val attempt = requireNotNull(data.active)
        val question = requireNotNull(ReadingEngine.question(attempt))
        question.choices.indices.forEach { choice ->
            val candidate = readingAnswer(data, choice, key)
            assertEquals("${attempt.contentId}: $key choice $choice", choice == question.answer,
                requireNotNull(candidate.active).earned(key))
        }
        return persist(readingAnswer(data, question.answer, key))
    }

    private fun solve(data: ReadingData, variant: Int): ReadingData {
        var result = data
        val activity = requireNotNull(data.active).content
        when (activity.game) {
            ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.DETECTIVE -> result = answer(result)
            ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
                val order = activity.validOrders[variant % activity.validOrders.size]
                result = persist(ReadingEngine.submit(readingOrder(result, order), "order"))
                assertTrue(activity.id, requireNotNull(result.active).earned("order"))
                result = answer(result, if (activity.game == ReadingGame.SCRAMBLE) "bonus" else "answer")
            }
            ReadingGame.CHAPTER -> {
                val branch = variant % activity.branches.size
                result = persist(ReadingEngine.branch(result, branch))
                assertEquals(branch, result.active?.branch)
                assertEquals(result, ReadingEngine.branch(result, (branch + 1) % activity.branches.size))
                result = answer(result)
            }
            ReadingGame.STUDIO -> ReadingEngine.studioKeys.forEachIndexed { index, key ->
                val choices = activity.writingChoices[index]
                result = persist(ReadingEngine.submit(
                    ReadingEngine.field(result, key, choices[variant % choices.size]), key))
            }
            ReadingGame.PARTNERS -> {
                result = persist(ReadingEngine.partner(result, "reader-b"))
                activity.writingChoices.forEachIndexed { index, choices ->
                    val idea = choices[variant % choices.size]
                    val sentence = when (index) {
                        0 -> "$idea began an adventure."
                        1 -> "The adventure led to $idea."
                        2 -> "The character $idea."
                        else -> "At last, the character $idea."
                    }
                    result = persist(ReadingEngine.submit(
                        ReadingEngine.field(result, "turn", sentence, expectedTurn = index),
                        "turn", expectedTurn = index))
                    assertEquals(sentence, result.active?.turns?.get(index)?.text)
                }
                result = persist(ReadingEngine.reread(readingOrder(result, listOf(0, 1, 2, 3))))
                result = persist(ReadingEngine.submit(result, "shared"))
            }
        }
        if (activity.game == ReadingGame.DETECTIVE) {
            activity.passage.indices.forEach { evidence ->
                val candidate = ReadingEngine.submit(
                    ReadingEngine.field(result, "evidence", evidence.toString()), "evidence")
                assertEquals("${activity.id}: evidence $evidence", evidence == activity.evidence,
                    requireNotNull(candidate.active).earned("evidence"))
            }
            result = persist(ReadingEngine.submit(
                ReadingEngine.field(result, "evidence", activity.evidence.toString()), "evidence"))
        }
        assertTrue(activity.id, ReadingEngine.ready(requireNotNull(result.active)))
        result = persist(ReadingEngine.finish(result))
        assertNotNull(activity.id, result.active?.finishedAt)
        assertEquals(result, ReadingEngine.finish(result))
        return result
    }

    @Test fun everySummitActivityAndEveryBranchAndAcceptedOrderCanBeCompletedThroughSqlite() {
        install()
        val activities = ReadingRepositoryPack("manifest-v2.json").activities.filter { it.level == ReadingLevel.SUMMIT }
        assertEquals(24, activities.size)
        var data = readers()
        val branches = mutableSetOf<Pair<String, Int>>()
        val orders = mutableSetOf<Pair<String, List<Int>>>()
        repeat(2) { variant ->
            ReadingGame.entries.forEach { game ->
                val seen = mutableSetOf<String>()
                repeat(3) {
                    data = store.start(data, game, ReadingLevel.SUMMIT, false)
                    val content = requireNotNull(data.active).content
                    assertTrue(content.id, seen.add(content.id))
                    assertEquals(ReadingLevel.SUMMIT, content.level)
                    assertEquals(game, content.game)
                    data = solve(data, variant)
                    val completed = requireNotNull(data.active)
                    completed.branch?.let { branches.add(content.id to it) }
                    if (content.validOrders.isNotEmpty()) orders.add(content.id to completed.order)
                    assertTrue(completed.checkpoints.values.all { it.earned && it.tries == 1 })
                }
                assertEquals(activities.filter { it.game == game }.map { it.id }.toSet(), seen)
            }
        }
        assertEquals(activities.flatMap { a -> a.branches.indices.map { a.id to it } }.toSet(), branches)
        assertEquals(activities.flatMap { a -> a.validOrders.map { a.id to it } }.toSet(), orders)
        assertEquals(48, data.attempts.size)
        assertEquals(48, data.completions("reader-a"))
        assertEquals(84, data.checkpointCount("reader-a"))
        assertEquals(6, data.checkpointCount("reader-b"))
        assertTrue(data.attempts.flatMap { it.checkpoints.values }.none { it.rewardGranted })
        assertEquals(6, data.completions("reader-b"))
        assertEquals(6, data.sharedCompletions("reader-a"))
        assertEquals(6, data.sharedCompletions("reader-b"))
        assertEquals(12, data.stories.size)
        assertEquals(data, reopen())
    }

    @Test fun readerPreferencesDraftsAndLevelPoolsStayIndependentAcrossReopening() {
        install()
        var data = readers()
        data = store.startBatch(data, ReadingGame.MISSING, requireNotNull(data.profile).level, 3)
        val firstCycle = data.attempts.map { it.contentId }.toSet()
        val summitDraft = requireNotNull(data.active)
        assertEquals(3, firstCycle.size)
        data = persist(ReadingEngine.level(data, ReadingLevel.SPROUT))
        data = store.start(data, ReadingGame.MISSING, requireNotNull(data.profile).level, false)
        assertEquals(ReadingLevel.SPROUT, data.active?.level)
        data = persist(ReadingEngine.level(data, ReadingLevel.SUMMIT))
        data = persist(ReadingEngine.selectProfile(data, "reader-b"))
        assertEquals(ReadingLevel.TRAIL, data.profile?.level)
        data = store.start(data, ReadingGame.MISSING, requireNotNull(data.profile).level, false)
        val trailDraft = requireNotNull(data.active)
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SUMMIT, false)
        assertEquals(ReadingLevel.TRAIL, data.profile?.level)
        val oldCycles = cycles()
        val oldPresented = presented()
        assertEquals(data, reopen())
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        assertEquals(ReadingLevel.SUMMIT, data.profiles.first { it.id == "reader-a" }.level)
        assertEquals(ReadingLevel.TRAIL, data.profiles.first { it.id == "reader-b" }.level)

        data = store.start(data, ReadingGame.MISSING, requireNotNull(data.profile).level, false)
        assertEquals(trailDraft, data.active)
        data = persist(ReadingEngine.selectProfile(data, "reader-a"))
        data = store.start(data, ReadingGame.MISSING, requireNotNull(data.profile).level, false)
        assertEquals(summitDraft, data.active)
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SUMMIT, true)
        assertNotEquals(summitDraft.contentId, data.active?.contentId)
        assertTrue(data.active?.contentId in firstCycle)
        data = persist(ReadingEngine.selectProfile(data, "reader-b"))
        data = store.startBatch(data, ReadingGame.MISSING, ReadingLevel.SUMMIT, 2)
        assertEquals(firstCycle, data.attempts.filter {
            it.ownerId == "reader-b" && it.level == ReadingLevel.SUMMIT
        }.map { it.contentId }.toSet())
        assertEquals(listOf(listOf("reader-a", "2"), listOf("reader-b", "1")),
            rows("SELECT reader_id,cycle FROM reading_cycles WHERE level='SUMMIT' ORDER BY reader_id"))
        assertEquals(oldCycles.filter { it[2] != "SUMMIT" }, cycles().filter { it[2] != "SUMMIT" })
        assertEquals(data, reopen())
    }

    @Test fun upgradingTheRealV1PackPreservesAttemptsRevisionsAndCyclesBeforeAddingSummit() {
        install("manifest-v1.json")
        val oldActivities = ReadingRepositoryPack("manifest-v1.json").activities
        val revisions = oldActivities.associate { it.id to requireNotNull(store.catalog.latest(it.id)).revision }
        var data = persist(ReadingEngine.level(readers(), ReadingLevel.SPROUT))
        data = store.startBatch(data, ReadingGame.MISSING, ReadingLevel.SPROUT, 2)
        data = persist(readingSolve(data))
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        data = persist(ReadingEngine.hint(readingAnswer(data, readingWrong(data))))
        val unfinished = requireNotNull(data.active)
        data = store.start(data, ReadingGame.SCRAMBLE, ReadingLevel.TRAIL, false)
        val firstTile = requireNotNull(data.active).content.validOrders.first().first()
        data = persist(ReadingEngine.placeTile(data, firstTile))
        val before = data
        val oldCycles = cycles()
        val oldPresented = presented()

        assertEquals("v2", install().version)
        assertEquals(before, store.load())
        assertEquals(before, reopen())
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        revisions.forEach { (id, revision) ->
            assertEquals(revision, store.catalog.latest(id)?.revision)
            assertEquals(oldActivities.first { it.id == id }, store.catalog.find(id, revision))
        }
        assertEquals(listOf(listOf("72")), rows("SELECT COUNT(*) FROM reading_content"))
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertEquals(unfinished, data.active)
        assertEquals(oldCycles, cycles())
        assertEquals(oldPresented, presented())
        data = persist(readingSolve(data))
        val completed = requireNotNull(data.active)
        assertEquals(2, completed.checkpoints["answer"]?.tries)
        assertEquals(1, completed.hints)
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        assertNotEquals(completed.contentId, data.active?.contentId)
        assertEquals(completed.copy(loadedContent = null),
            data.attempts.first { it.id == completed.id })
        assertEquals(listOf(listOf("2")),
            rows("SELECT cycle FROM reading_cycles WHERE game='MISSING' AND level='SPROUT'"))
        data = persist(ReadingEngine.level(data, ReadingLevel.SUMMIT))
        data = store.start(data, ReadingGame.MISSING, requireNotNull(data.profile).level, false)
        assertEquals(ReadingLevel.SUMMIT, data.active?.level)
        assertEquals(listOf(listOf("1")),
            rows("SELECT cycle FROM reading_cycles WHERE game='MISSING' AND level='SUMMIT'"))
        val upgradedCycles = cycles()
        val upgradedPresented = presented()
        install()
        assertEquals(data, reopen())
        assertEquals(upgradedCycles, cycles())
        assertEquals(upgradedPresented, presented())
        assertEquals(before.attempts.last().copy(loadedContent = null),
            data.attempts.first { it.id == before.attempts.last().id })
    }
}
