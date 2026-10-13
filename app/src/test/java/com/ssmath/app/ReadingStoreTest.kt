package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingStoreTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "reading_test_private.json")

    @Before fun clean() {
        listOf(file, File(file.path + ".bak"), File(file.path + ".new")).forEach { it.delete() }
    }

    @Test fun draftsSupportBranchAttributionAndStoriesRoundTripWithoutDuplicatingPoints() {
        val store = ReadingStore(file)
        assertEquals(ReadingData(), store.load())
        ReadingContent.activities.forEach { content ->
            var data = readingFixture(content)
            data = ReadingEngine.hint(ReadingEngine.narration(data, true))
            if (content.game == ReadingGame.CHAPTER) data = ReadingEngine.branch(data, 1)
            if (content.game == ReadingGame.PARTNERS) {
                data = ReadingEngine.partner(data, "reader-b")
                data = ReadingEngine.submit(ReadingEngine.field(data, "turn", "A new adventure began.", expectedTurn = 0),
                    "turn", expectedTurn = 0)
                data = ReadingEngine.field(data, "turn", "An unfinished sentence", expectedTurn = 1)
            } else if (content.game == ReadingGame.STUDIO) data = ReadingEngine.field(data, "character", "A typed character")
            else if (content.tiles.isNotEmpty()) data = ReadingEngine.placeTile(data, 0)
            else data = ReadingEngine.field(data, "answer", "0")
            store.save(data)
            val restored = ReadingStore(file).load()
            assertEquals(content.id, data, restored)
            assertEquals(data.active?.id, restored.active?.id)

            val complete = readingSolve(readingFixture(content))
            store.save(complete)
            var reopened = ReadingStore(file).load()
            repeat(4) {
                reopened = ReadingEngine.submit(ReadingEngine.finish(reopened), "answer")
            }
            assertEquals(content.id, complete, reopened)
            assertEquals(complete.points("reader-a"), reopened.points("reader-a"))
            assertEquals(complete.stories, reopened.stories)
        }
    }

    @Test fun failedFirstTrySurvivesReloadAndDoesNotBecomeIndependentOnRetry() {
        val store = ReadingStore(file)
        store.load()
        store.save(readingAnswer(readingFixture(), readingWrong(readingFixture())))
        val restored = ReadingStore(file).load()
        assertEquals(ReadingAccuracy(0, 1, 0, 0), restored.accuracy("reader-a"))
        var result = readingAnswer(restored, readingCorrect(restored))
        store.save(result)
        result = readingAnswer(ReadingStore(file).load(), readingCorrect(restored))
        assertEquals(1, result.points("reader-a"))
        assertEquals(ReadingAccuracy(0, 1, 0, 1), result.accuracy("reader-a"))
    }

    @Test fun corruptUnreadableOrUnsupportedFilesAreNotSilentlyReplaced() {
        for (text in listOf("{broken", "{}", """{"version":99,"profiles":[],"selectedProfileId":null,"attempts":[],"stories":[]}""")) {
            file.writeText(text)
            val store = ReadingStore(file)
            assertThrows(IOException::class.java) { store.load() }
            assertThrows(IllegalStateException::class.java) { store.save(ReadingData()) }
            assertEquals(text, file.readText())
        }
        file.delete()
        file.mkdir()
        val directoryStore = ReadingStore(file)
        assertThrows(IOException::class.java) { directoryStore.load() }
        assertTrue(file.isDirectory)
        file.delete()
    }

    @Test fun atomicBackupRecoversTheLastCommittedSnapshot() {
        val store = ReadingStore(file)
        store.load()
        val data = readingAnswer(readingFixture(), readingCorrect(readingFixture()))
        store.save(data)
        file.copyTo(File(file.path + ".bak"), overwrite = true)
        file.writeText("interrupted replacement")
        assertEquals(data, ReadingStore(file).load())
        assertFalse(File(file.path + ".bak").exists())
    }

    @Test fun invalidAttributionAndDuplicateIdsAreRejectedRatherThanRepaired() {
        val base = readingFixture()
        assertThrows(IllegalArgumentException::class.java) {
            ReadingEngine.validate(base.copy(attempts = base.attempts + base.attempts))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReadingEngine.validate(base.copy(profiles = base.profiles + base.profiles.first()))
        }
        val partners = readingFixture(ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first())
        assertThrows(IllegalArgumentException::class.java) {
            ReadingEngine.validate(partners.copy(attempts = listOf(requireNotNull(partners.active).copy(
                partnerId = "reader-b", turns = listOf(ReadingTurn("reader-b", "Wrong author."))))))
        }
    }

    @Test fun actorSerializesRapidEditsAndCreditOffTheMainThread() {
        val storage = RecordingStorage(readingFixture())
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        repeat(20) { model.act { ReadingEngine.hint(it) } }
        model.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
        repeat(20) { model.act { ReadingEngine.submit(it, "answer") } }
        await { model.data?.points("reader-a") == 1 && model.data?.active?.hints == 20 && !model.saving }
        assertEquals(1, storage.saved.points("reader-a"))
        assertEquals(20, storage.saved.active?.hints)
        assertEquals(1, storage.saved.active?.checkpoints?.get("answer")?.tries)
        assertTrue(storage.allWritesOffMain)
        assertEquals(1, storage.maximumConcurrentWrites)
    }

    @Test fun failedSaveBlocksEditsAndRetryCommitsTheSamePendingCheckpointExactlyOnce() {
        val storage = RecordingStorage(ReadingEngine.field(readingFixture(), "answer", readingCorrect(readingFixture()).toString()))
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        model.act { ReadingEngine.submit(it, "answer") }
        await { model.storageError != null }
        assertEquals(0, model.data?.points("reader-a"))
        assertEquals(0, storage.saved.points("reader-a"))
        model.act { ReadingEngine.hint(it) }
        storage.failSave = false
        model.retry()
        await { model.storageError == null && model.data?.points("reader-a") == 1 }
        model.act { ReadingEngine.submit(it, "answer") }
        await { !model.saving }
        assertEquals(1, storage.saved.points("reader-a"))
        assertEquals(0, storage.saved.active?.hints)
    }

    @Test fun failedLoadExposesRetryWithoutAnyWrites() {
        val storage = RecordingStorage(readingFixture()).apply { failLoad = true }
        val model = ReadingViewModel(application, storage)
        await { model.storageError != null }
        assertNull(model.data)
        model.act { ReadingEngine.addProfile(it, "Nobody") }
        assertEquals(0, storage.writeCount)
        storage.failLoad = false
        model.retry()
        await { model.data != null && model.storageError == null }
        assertEquals("reader-a", model.data?.selectedProfileId)
        assertEquals(0, storage.writeCount)
    }

    @Test fun alreadyAcceptedEditsAreReplayedInOrderAfterAWriteFailure() {
        val storage = RecordingStorage(readingFixture())
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        repeat(10) { model.act { ReadingEngine.hint(it) } }
        model.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
        model.act { ReadingEngine.submit(it, "answer") }
        await { model.storageError != null }
        storage.failSave = false
        model.retry()
        await { model.data?.points("reader-a") == 1 && !model.saving }
        assertEquals(10, storage.saved.active?.hints)
        assertEquals(ReadingAccuracy(0, 1, 1, 0), storage.saved.accuracy("reader-a"))
    }

    @Test fun staleQueuedCallbacksCannotChangeAnotherReaderOrNewAttempt() {
        val second = ReadingAttempt("attempt-b", "reader-b", "missing-sun", 1_700_000_000_001)
        val initial = readingFixture().let { data ->
            data.copy(attempts = data.attempts + second, profiles = data.profiles.map {
                if (it.id == "reader-b") it.copy(activeAttemptId = "attempt-b") else it
            })
        }
        val storage = RecordingStorage(initial)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        model.act { ReadingEngine.selectProfile(it, "reader-b") }
        model.act { ReadingEngine.field(it, "answer", "1") }
        await { model.data?.selectedProfileId == "reader-b" && !model.saving }
        assertTrue(requireNotNull(model.data?.active).fields.isEmpty())
        model.act { ReadingEngine.start(it, ReadingGame.MISSING, ReadingLevel.SPROUT, true, "new-attempt") }
        model.act { ReadingEngine.hint(it) }
        await { model.data?.active?.id == "new-attempt" && !model.saving }
        assertEquals(0, model.data?.active?.hints)
    }

    @Test fun delayedPartnerSaveRejectsEditsAndSubmissionsForThePreviousTurn() {
        var data = readingFixture(ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first())
        data = ReadingEngine.partner(data, "reader-b")
        data = ReadingEngine.field(data, "turn", "Ari's sentence.", expectedTurn = 0)
        val storage = RecordingStorage(data)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        storage.writeStarted = CountDownLatch(1)
        try {
            model.act { ReadingEngine.submit(it, "turn", expectedTurn = 0) }
            await { storage.writeStarted?.count == 0L }
            model.act { ReadingEngine.field(it, "turn", "Still Ari typing.", expectedTurn = 0) }
            model.act { ReadingEngine.submit(it, "turn", expectedTurn = 0) }
            model.act { it.copy(page = ReadingPage.MENU) }
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.data?.page == ReadingPage.MENU && !model.saving }
        assertEquals(listOf(ReadingTurn("reader-a", "Ari's sentence.")), storage.saved.active?.turns)
        assertNull(storage.saved.active?.fields?.get("turn"))
        assertEquals(1, storage.saved.points("reader-a"))
        assertEquals(0, storage.saved.points("reader-b"))
    }

    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + 5_000_000_000
        while (!condition() && System.nanoTime() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Timed out waiting for the serialized reading writer", condition())
    }

    private class RecordingStorage(@Volatile var saved: ReadingData) : ReadingStorage {
        @Volatile var failLoad = false
        @Volatile var failSave = false
        @Volatile var writeCount = 0
        @Volatile var allWritesOffMain = true
        @Volatile var maximumConcurrentWrites = 0
        @Volatile var writeGate: CountDownLatch? = null
        @Volatile var writeStarted: CountDownLatch? = null
        private var writing = 0
        override fun load(): ReadingData {
            if (failLoad) throw IOException("Simulated unavailable storage")
            return saved
        }
        override fun save(data: ReadingData) {
            allWritesOffMain = allWritesOffMain && Looper.myLooper() != Looper.getMainLooper()
            writing++
            maximumConcurrentWrites = maxOf(maximumConcurrentWrites, writing)
            try {
                writeStarted?.countDown()
                check(writeGate?.await(5, TimeUnit.SECONDS) != false)
                if (failSave) throw IOException("Simulated full storage")
                ReadingEngine.validate(data)
                saved = data
                writeCount++
            } finally {
                writing--
            }
        }
    }
}
