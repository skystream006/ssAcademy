package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

internal fun legacyReadingPayload(data: ReadingData, activateRetired: Boolean = false): JsonObject {
    val root = readingPackJson.parseToJsonElement(readingPackJson.encodeToString(data)).jsonObject
    val retiredAttempts = mutableListOf<JsonObject>()
    val retiredSessions = mutableListOf<JsonObject>()
    val retiredContent = listOf("CHAPTER" to "chapter-path", "PARTNERS" to "partners-sprout-0", "CAPTION" to "caption-cat")
    retiredContent.forEach { (game, contentId) ->
        val ids = (0 until 15).map { "$game-$it" }
        ids.forEach { id ->
            retiredAttempts += readingPackJson.parseToJsonElement(
                """{"id":"$id","ownerId":"reader-a","contentId":"imported-$contentId","startedAt":1,
                    "activityRef":{"revision":"retired","game":"$game","level":"SPROUT","title":"Removed activity"}}"""
            ).jsonObject
        }
        retiredAttempts += readingPackJson.parseToJsonElement(
            """{"id":"legacy-$game","ownerId":"reader-a","contentId":"$contentId","startedAt":1}"""
        ).jsonObject
        retiredSessions += readingPackJson.parseToJsonElement(
            """{"id":"session-$game","ownerId":"reader-a","game":"$game","level":"SPROUT",
                "attemptIds":${JsonArray(ids.map(::JsonPrimitive))},"startedAt":1,"rewardsEnabledAtStart":false}"""
        ).jsonObject
    }
    val attempts = (root.getValue("attempts") as JsonArray).map { value ->
        val attempt = value.jsonObject
        JsonObject(attempt + mapOf(
            "branch" to readingPackJson.parseToJsonElement("null"),
            "partnerId" to readingPackJson.parseToJsonElement("null"),
            "turns" to JsonArray(emptyList()), "reread" to JsonPrimitive(false),
            "checkpoints" to JsonObject(attempt.getValue("checkpoints").jsonObject.mapValues { (_, checkpoint) ->
                JsonObject(checkpoint.jsonObject + ("shared" to JsonPrimitive(false)))
            })
        ))
    }
    val stories = (root.getValue("stories") as JsonArray).map {
        JsonObject(it.jsonObject + ("turns" to JsonArray(emptyList())))
    } + readingPackJson.parseToJsonElement(
        """{"attemptId":"legacy-PARTNERS","title":"Removed story","text":"Removed writing",
            "illustration":"book","readerIds":["reader-a","reader-b"],"turns":[],"savedAt":2}"""
    )
    val profiles = (root.getValue("profiles") as JsonArray).map { value ->
        if (activateRetired) JsonObject(value.jsonObject + ("activeAttemptId" to JsonPrimitive("legacy-CAPTION")))
        else value
    }
    return JsonObject(root + mapOf(
        "attempts" to JsonArray(attempts + retiredAttempts),
        "stories" to JsonArray(stories),
        "sessions" to JsonArray((root.getValue("sessions") as JsonArray) + retiredSessions),
        "profiles" to JsonArray(profiles)
    ))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingStoreTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "reading_test_private.json")
    private val settings get() = application.getSharedPreferences("settings", 0)

    @Before fun clean() {
        listOf(file, File(file.path + ".bak"), File(file.path + ".new")).forEach { it.delete() }
        settings.edit().clear().commit()
    }

    @Test fun retiredGamesArePurgedBeforeLegacyDecodingWithoutChangingOtherReadersOrStories() {
        val original = readingSolve(readingFixture(
            ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()))
        file.writeText(legacyReadingPayload(original, activateRetired = true).toString())
        val expected = original.copy(
            profiles = original.profiles.map { it.copy(activeAttemptId = null) },
            page = ReadingPage.MENU,
            deletedSessionIds = setOf("session-CHAPTER", "session-PARTNERS", "session-CAPTION")
        )
        assertEquals(expected, ReadingStore(file).load())
        assertEquals(expected, ReadingStore(file).load())
        val rewritten = file.readText()
        assertFalse(rewritten.contains("Removed writing"))
        assertFalse(rewritten.contains("chapter-path"))
        assertFalse(rewritten.contains("partners-sprout"))
        assertFalse(rewritten.contains("caption-cat"))
        assertFalse(rewritten.contains("\"turns\""))
        assertFalse(rewritten.contains("\"shared\""))
    }

    @Test fun retiringOtherGamesKeepsTheCurrentDraftAndRejectsUnrelatedCorruption() {
        val original = ReadingEngine.field(readingFixture(), "answer", "1")
        file.writeText(legacyReadingPayload(original).toString())
        assertEquals(original.copy(deletedSessionIds = setOf("session-CHAPTER", "session-PARTNERS", "session-CAPTION")),
            ReadingStore(file).load())
        val invalid = JsonObject(legacyReadingPayload(original) + ("profiles" to JsonPrimitive("invalid")))
        file.writeText(invalid.toString())
        assertThrows(IOException::class.java) { ReadingStore(file).load() }
        assertEquals(invalid.toString(), file.readText())
    }

    @Test fun draftsSupportAndStoriesRoundTripWithoutDuplicatingCheckpoints() {
        val store = ReadingStore(file)
        assertEquals(ReadingData(), store.load())
        ReadingContent.activities.forEach { content ->
            var data = readingFixture(content)
            data = ReadingEngine.hint(ReadingEngine.narration(data, true))
            if (content.game == ReadingGame.STUDIO) data = ReadingEngine.field(data, "character", "A typed character")
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
            assertEquals(complete.checkpointCount("reader-a"), reopened.checkpointCount("reader-a"))
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
        assertEquals(1, result.checkpointCount("reader-a"))
        assertEquals(ReadingAccuracy(0, 1, 0, 1), result.accuracy("reader-a"))
    }

    @Test fun sentenceSpacesAndLegacyCompactOrdersRoundTripAndResume() {
        val store = ReadingStore(file)
        store.load()
        val base = readingFixture(ReadingContent.find("scramble-cats"))
        for (order in listOf(emptyList(), listOf(1), listOf(1, 2), listOf(-1, -1, 0), listOf(1, -1, 0))) {
            val data = base.copy(attempts = listOf(requireNotNull(base.active).copy(order = order)))
            store.save(data)
            var restored = ReadingEngine.start(ReadingStore(file).load(), ReadingGame.SCRAMBLE, ReadingLevel.SPROUT)
            assertEquals(data, restored)
            listOf(1, 2, 0).forEach { restored = ReadingEngine.placeTile(restored, it) }
            restored = ReadingEngine.submit(restored, "order")
            assertEquals(1, restored.checkpointCount("reader-a"))
            store.save(restored)
            assertEquals(restored, ReadingEngine.submit(ReadingStore(file).load(), "order"))
        }
    }

    @Test fun legacyCheckpointsWithoutRewardFlagsKeepTheirProgressWithoutAwards() {
        val dataSets = listOf(
            readingSolve(readingFixture()),
            readingAnswer(readingFixture(), readingWrong(readingFixture())),
            readingSolve(readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()))
        )
        val store = ReadingStore(file)
        store.load()
        dataSets.forEach { original ->
            store.save(original)
            file.writeText(file.readText().replace(Regex(""","rewardGranted":(?:true|false)"""), ""))
            assertFalse(file.readText().contains("rewardGranted"))
            val restored = ReadingStore(file).load()
            assertEquals(original.active?.id, restored.active?.id)
            assertEquals(original.active?.contentId, restored.active?.contentId)
            assertEquals(original.stories, restored.stories)
            assertTrue(requireNotNull(restored.active).checkpoints.values.none { it.rewardGranted })
            listOf("reader-a", "reader-b").forEach { id ->
                assertEquals(original.checkpointCount(id), restored.checkpointCount(id))
                assertEquals(original.accuracy(id), restored.accuracy(id))
            }
            if (restored.active?.finishedAt != null) {
                assertEquals(restored, ReadingEngine.submit(restored, "answer", rewardsEnabled = false))
            } else {
                val disabled = readingAnswer(restored, readingCorrect(restored), rewardsEnabled = false)
                assertTrue(requireNotNull(disabled.active).earned("answer"))
                assertEquals(1, disabled.checkpointCount("reader-a"))
                assertFalse(requireNotNull(disabled.active).checkpoints.getValue("answer").rewardGranted)
            }
        }
    }

    @Test fun allLegacyActivityCompletionsAndStoriesSurviveReloadWithoutRetroactiveAwards() {
        val store = ReadingStore(file)
        store.load()
        ReadingContent.activities.forEach { content ->
            val complete = readingSolve(readingFixture(content), rewardsEnabled = false)
            store.save(complete)
            var restored = ReadingStore(file).load()
            assertEquals(content.id, complete, restored)
            repeat(3) {
                restored = ReadingEngine.resume(restored, requireNotNull(restored.active).id)
                requireNotNull(restored.active).checkpoints.keys.forEach { key ->
                    restored = ReadingEngine.submit(restored, key, rewardsEnabled = true)
                }
                restored = ReadingEngine.finish(restored)
                store.save(restored)
                restored = ReadingStore(file).load()
            }
            assertEquals(content.id, complete, restored)
            assertTrue(requireNotNull(restored.active).checkpoints.values.none { it.rewardGranted })
            assertTrue(restored.sessions.isEmpty())
        }
    }

    @Test fun mixedRewardDraftResumesWithoutRecreditingOrLosingItsStory() {
        val content = ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()
        var data = readingFixture(content)
        data = ReadingEngine.submit(ReadingEngine.field(data, "character", "A fox"), "character")
        data = ReadingEngine.submit(ReadingEngine.field(data, "setting", "a forest"), "setting", rewardsEnabled = false)
        data = ReadingEngine.field(data, "problem", "found a mystery")
        val store = ReadingStore(file)
        store.load()
        store.save(data)
        val restored = ReadingStore(file).load()
        assertEquals(data, restored)
        data = ReadingEngine.start(restored, content.game, content.level)
        assertEquals(data, ReadingEngine.submit(data, "setting", rewardsEnabled = true))
        assertEquals(data, ReadingEngine.submit(data, "character", rewardsEnabled = false))
        assertEquals(2, data.checkpointCount("reader-a"))
        data = ReadingEngine.submit(data, "problem")
        data = ReadingEngine.submit(ReadingEngine.field(data, "ending", "solved it"), "ending")
        data = ReadingEngine.finish(data)
        store.save(data)
        assertEquals(data, ReadingStore(file).load())
        assertEquals(4, data.checkpointCount("reader-a"))
        assertEquals("attempt-a", data.stories.single().attemptId)
        assertTrue(data.stories.single().text.contains("found a mystery"))
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
        assertThrows(IllegalArgumentException::class.java) {
            ReadingEngine.validate(base.copy(attempts = listOf(requireNotNull(base.active).copy(ownerId = "unknown"))))
        }
    }

    @Test fun actorSerializesRapidEditsAndCreditOffTheMainThread() {
        val storage = RecordingStorage(readingFixture())
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        repeat(20) { model.act { ReadingEngine.hint(it) } }
        model.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
        repeat(20) { model.submit("answer") }
        await { model.data?.checkpointCount("reader-a") == 1 && model.data?.active?.hints == 20 && !model.saving }
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
        assertEquals(20, storage.saved.active?.hints)
        assertEquals(1, storage.saved.active?.checkpoints?.get("answer")?.tries)
        assertTrue(storage.allWritesOffMain)
        assertEquals(1, storage.maximumConcurrentWrites)
    }

    @Test fun updatedNameSurvivesReloadWithAllProgressIntact() {
        val original = readingSolve(readingFixture(
            ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()))
        val store = ReadingStore(file)
        store.load()
        store.save(original)
        val model = ReadingViewModel(application, store)
        await { model.data != null }
        model.updateName("  Alex  ")
        await { model.data?.profile?.name == "Alex" && !model.saving }
        val restored = ReadingStore(file).load()
        assertEquals(ReadingEngine.renameProfile(original, "Alex"), restored)
    }

    @Test fun failedNameSaveKeepsOriginalUntilRetrySucceeds() {
        val original = readingFixture()
        val storage = RecordingStorage(original)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        model.updateName("Alex")
        await { model.storageError != null && !model.saving }
        assertEquals(original, model.data)
        assertEquals(original, storage.saved)
        storage.failSave = false
        model.retry()
        await { model.storageError == null && model.data?.profile?.name == "Alex" && !model.saving }
        assertEquals(ReadingEngine.renameProfile(original, "Alex"), storage.saved)
    }

    @Test fun failedHistoryDeletionKeepsTheGameUntilRetryAndRejectsStaleEdits() {
        val original = readingSessionFixture()
        val storage = RecordingStorage(original)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        model.deleteHistory(setOf(requireNotNull(original.active).id))
        model.act { ReadingEngine.hint(it) }
        await { model.storageError != null && !model.saving }
        assertEquals(original, model.data)
        assertEquals(original, storage.saved)
        storage.failSave = false
        model.retry()
        await { model.storageError == null && model.data?.attempts?.isEmpty() == true && !model.saving }
        assertEquals(ReadingEngine.clearHistory(original, "reader-a"), storage.saved)
    }

    @Test fun disabledGamesCanBeDeletedWithoutChangingOtherReadersDrafts() {
        var original = ReadingEngine.selectProfile(readingFixture(), "reader-b")
        original = ReadingEngine.start(original, ReadingGame.MISSING, ReadingLevel.SPROUT, true, "attempt-b")
        original = ReadingEngine.selectProfile(original, "reader-a")
        settings.edit().putStringSet("disabled_reading_games", setOf(ReadingGame.MISSING.name)).commit()
        val store = ReadingStore(file)
        store.load()
        store.save(original)
        val model = ReadingViewModel(application, store)
        await { model.data != null }
        model.clearHistory("reader-b")
        await { model.data?.attempts?.size == 1 && !model.saving }
        assertEquals(original.active, model.data?.active)
        model.clearHistory("reader-a")
        await { model.data?.attempts?.isEmpty() == true && !model.saving }
        assertEquals(model.data, ReadingStore(file).load())
        assertNull(model.message)
    }

    @Test fun resumingANonActiveResultQueuedBehindDeletionDoesNotCrashOrRestoreIt() {
        val original = ReadingEngine.start(readingFixture(), ReadingGame.MISSING, ReadingLevel.SPROUT,
            true, "current")
        val storage = RecordingStorage(original)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        storage.writeStarted = CountDownLatch(1)
        try {
            model.deleteHistory(setOf("attempt-a"))
            await { storage.writeStarted?.count == 0L }
            model.act { ReadingEngine.resume(it, "attempt-a") }
            model.act { ReadingEngine.hint(it) }
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.data?.active?.hints == 1 && !model.saving }
        assertEquals(listOf("current"), model.data?.attempts?.map { it.id })
        assertEquals("current", model.data?.active?.id)
        assertNull(model.storageError)
    }

    @Test fun failedSaveBlocksEditsAndRetryCommitsTheSamePendingCheckpointExactlyOnce() {
        val storage = RecordingStorage(ReadingEngine.field(readingFixture(), "answer", readingCorrect(readingFixture()).toString()))
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        model.submit("answer")
        await { model.storageError != null }
        assertEquals(0, model.data?.checkpointCount("reader-a"))
        assertEquals(0, storage.saved.checkpointCount("reader-a"))
        model.act { ReadingEngine.hint(it) }
        settings.edit().putBoolean("reading_rewards_enabled", false).commit()
        storage.failSave = false
        model.retry()
        await { model.storageError == null && model.data?.checkpointCount("reader-a") == 1 }
        model.submit("answer")
        await { !model.saving }
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
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

    @Test fun alreadyAcceptedEditsKeepTheirDisabledRewardSnapshotAfterAWriteFailure() {
        val storage = RecordingStorage(readingFixture())
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        settings.edit().putBoolean("reading_rewards_enabled", false).commit()
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        try {
            repeat(10) { model.act { ReadingEngine.hint(it) } }
            model.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
            model.submit("answer")
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.storageError != null }
        settings.edit().putBoolean("reading_rewards_enabled", true).commit()
        storage.failSave = false
        model.retry()
        await { model.data?.active?.earned("answer") == true && !model.saving }
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
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

    @Test fun delayedStorySaveRejectsEditsAndSubmissionsForTheCompletedCheckpoint() {
        var data = readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first())
        data = ReadingEngine.field(data, "character", "A patient inventor")
        val storage = RecordingStorage(data)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        storage.writeStarted = CountDownLatch(1)
        try {
            model.submit("character")
            await { storage.writeStarted?.count == 0L }
            model.act { ReadingEngine.field(it, "character", "Still typing") }
            model.submit("character")
            model.act { it.copy(page = ReadingPage.MENU) }
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.data?.page == ReadingPage.MENU && !model.saving }
        assertEquals("A patient inventor", storage.saved.active?.fields?.get("character"))
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
        assertEquals(0, storage.saved.checkpointCount("reader-b"))
    }

    @Test fun legacySubmitDoesNotAwardRewardsEvenWhenReadingRewardsDefaultOn() {
        settings.edit().putBoolean("rewards_enabled", false).commit()
        val storage = RecordingStorage(ReadingEngine.field(readingFixture(), "answer", readingCorrect(readingFixture()).toString()))
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        model.submit("answer")
        await { model.data?.active?.earned("answer") == true && !model.saving }
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
        assertFalse(requireNotNull(storage.saved.active).checkpoints.getValue("answer").rewardGranted)
        assertFalse(settings.getBoolean("rewards_enabled", true))
        assertFalse(settings.contains("reading_rewards_enabled"))
    }

    @Test fun readingRewardPreferencesNeverAwardLegacyCheckpoints() {
        settings.edit().putBoolean("rewards_enabled", true).putBoolean("reading_rewards_enabled", false).commit()
        val store = ReadingStore(file)
        store.load()
        store.save(ReadingEngine.field(readingFixture(), "answer", readingCorrect(readingFixture()).toString()))
        val model = ReadingViewModel(application, store)
        await { model.data != null }
        model.submit("answer")
        model.act { ReadingEngine.finish(it) }
        await { model.data?.active?.finishedAt != null && !model.saving }
        assertEquals(1, model.data?.checkpointCount("reader-a"))
        assertEquals(ReadingAccuracy(1, 1, 0, 0), model.data?.accuracy("reader-a"))
        assertEquals(1, model.data?.completions("reader-a"))
        assertTrue(settings.getBoolean("rewards_enabled", false))

        settings.edit().putBoolean("reading_rewards_enabled", true).commit()
        val resumed = ReadingViewModel(application, ReadingStore(file))
        await { resumed.data != null }
        resumed.submit("answer")
        resumed.act { ReadingEngine.start(it, ReadingGame.MISSING, ReadingLevel.SPROUT, id = "new-attempt") }
        await { resumed.data?.active?.id == "new-attempt" && !resumed.saving }
        assertEquals(1, resumed.data?.checkpointCount("reader-a"))
        resumed.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
        resumed.submit("answer")
        await { resumed.data?.active?.earned("answer") == true && !resumed.saving }
        val saved = ReadingStore(file).load()
        assertEquals(2, saved.checkpointCount("reader-a"))
        assertFalse(saved.attempts.first().checkpoints.getValue("answer").rewardGranted)
        assertFalse(saved.attempts.last().checkpoints.getValue("answer").rewardGranted)
    }

    @Test fun queuedLegacySubmitNeverAwardsCheckpointRewards() {
        settings.edit().putBoolean("reading_rewards_enabled", false).commit()
        val storage = RecordingStorage(ReadingEngine.field(readingFixture(), "answer", readingCorrect(readingFixture()).toString()))
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        storage.writeStarted = CountDownLatch(1)
        try {
            model.act { ReadingEngine.hint(it) }
            await { storage.writeStarted?.count == 0L }
            model.submit("answer")
            settings.edit().putBoolean("reading_rewards_enabled", true).commit()
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.data?.active?.earned("answer") == true && !model.saving }
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
        assertFalse(requireNotNull(storage.saved.active).checkpoints.getValue("answer").rewardGranted)
        assertEquals(ReadingAccuracy(0, 1, 1, 0), storage.saved.accuracy("reader-a"))
    }

    @Test fun failedLegacyCheckpointSaveDoesNotGainAwardsWhenRetriedWithRewardsEnabled() {
        settings.edit().putBoolean("reading_rewards_enabled", false).commit()
        val storage = RecordingStorage(ReadingEngine.field(readingFixture(), "answer", readingCorrect(readingFixture()).toString()))
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        model.submit("answer")
        await { model.storageError != null }
        assertFalse(requireNotNull(model.data?.active).earned("answer"))
        settings.edit().putBoolean("reading_rewards_enabled", true).commit()
        storage.failSave = false
        model.retry()
        await { model.data?.active?.earned("answer") == true && model.storageError == null && !model.saving }
        repeat(10) { model.submit("answer") }
        model.act { it.copy(page = ReadingPage.MENU) }
        await { model.data?.page == ReadingPage.MENU && !model.saving }
        assertEquals(1, storage.saved.checkpointCount("reader-a"))
        assertEquals(1, storage.saved.active?.checkpoints?.get("answer")?.tries)
        assertEquals(ReadingAccuracy(1, 1, 0, 0), storage.saved.accuracy("reader-a"))
        assertFalse(requireNotNull(storage.saved.active).checkpoints.getValue("answer").rewardGranted)
    }

    @Test fun sessionStartSnapshotsSettingsAndRepeatedSubmissionsAdvanceOnlyOnce() {
        settings.edit().putBoolean("reading_rewards_enabled", true).putBoolean("show_timer", true)
            .putInt("time_limit_minutes", 10).commit()
        val storage = RecordingStorage(readingFixture())
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        model.startSession(ReadingGame.MISSING, 15)
        await { model.data?.activeSession != null && !model.saving }
        val session = requireNotNull(model.data?.activeSession)
        assertEquals(15, session.questionCount)
        assertEquals(600_000L, session.timeLimitMs)
        assertTrue(session.rewardsEnabledAtStart)
        assertEquals(16, storage.saved.attempts.size)
        assertFalse(storage.saved.attempts.first().abandoned)
        model.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
        repeat(10) { model.submit("answer") }
        await { model.data?.active?.id == session.attemptIds[1] && !model.saving }
        assertEquals(1, requireNotNull(model.data).sessionCorrect(session))
        assertNotNull(storage.saved.attempts.first { it.id == session.attemptIds.first() }.finishedAt)
        repeat(10) { model.nextSessionActivity() }
        await { model.data?.active?.id == session.attemptIds[1] && !model.saving }
        assertEquals(1, requireNotNull(model.data).sessionAnswered(session))
        assertTrue(requireNotNull(model.data?.active).checkpoints.isEmpty())
    }

    @Test fun legacySerializedRewardFlagsRemainDecodableButNeverBecomeSessionPrizes() {
        val original = readingSolve(readingFixture())
        val legacy = original.copy(attempts = original.attempts.map { attempt ->
            attempt.copy(checkpoints = attempt.checkpoints.mapValues { (_, checkpoint) ->
                checkpoint.copy(rewardGranted = true)
            })
        })
        val store = ReadingStore(file)
        store.load()
        store.save(legacy)
        val restored = ReadingStore(file).load()
        assertEquals(legacy, restored)
        assertTrue(requireNotNull(restored.active).checkpoints.values.all { it.rewardGranted })
        assertTrue(restored.sessions.isEmpty())
        assertEquals(restored, ReadingEngine.finish(restored))
        assertEquals(restored, ReadingEngine.submit(restored, "answer"))
    }

    @Test fun submittingTheLastRequiredCheckpointAutomaticallySavesAndAdvancesEveryGame() {
        ReadingGame.entries.forEach { game ->
            val solved = readingSolve(readingSessionFixture(game))
            val completed = requireNotNull(solved.active)
            val key = ReadingEngine.requiredKeys(completed).last()
            val data = solved.copy(attempts = solved.attempts.map {
                if (it.id == completed.id) it.copy(finishedAt = null, checkpoints = it.checkpoints - key) else it
            }, stories = emptyList())
            val storage = RecordingStorage(data)
            val model = ReadingViewModel(application, storage)
            await { model.data != null }
            model.submit(key)
            await { model.data?.active?.id == data.activeSession?.attemptIds?.get(1) && !model.saving }
            val saved = storage.saved
            assertEquals(1, saved.sessionCorrect(requireNotNull(saved.activeSession)))
            assertNotNull(saved.attempts.first().finishedAt)
            assertEquals(if (game == ReadingGame.STUDIO) 1 else 0, saved.stories.size)
            assertTrue(requireNotNull(saved.active).fields.isEmpty())
            ReadingEngine.validate(saved)
        }
    }

    @Test fun failedSubmissionRetriesTheAnswerAndAdvanceTogetherWithoutChangingTheNextQuestion() {
        val session = readingSessionFixture()
        val data = ReadingEngine.field(session, "answer", readingWrong(session).toString())
        val storage = RecordingStorage(data)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        storage.failSave = true
        model.submit("answer")
        await { model.storageError != null && !model.saving }
        assertEquals(data, model.data)
        assertEquals(data, storage.saved)
        storage.failSave = false
        model.retry()
        await { model.data?.active?.id == data.activeSession?.attemptIds?.get(1) && !model.saving }
        val saved = storage.saved
        assertEquals(1, saved.sessionWrong(requireNotNull(saved.activeSession)))
        assertTrue(saved.attempts.first().feedback.contains("Correct answer:"))
        assertEquals(1, saved.attempts.first().checkpoints.getValue("answer").tries)
        assertTrue(requireNotNull(saved.active).checkpoints.isEmpty())
        assertEquals(saved, model.data)
        ReadingEngine.validate(saved)
    }

    @Test fun sessionsRoundTripWithCurrentFinishedMemberDraftAndEndedUnusedAssignments() {
        val store = ReadingStore(file)
        store.load()
        var data = readingSolve(readingSessionFixture())
        store.save(data)
        data = ReadingStore(file).load()
        val session = requireNotNull(data.activeSession)
        assertEquals(session.attemptIds.first(), data.active?.id)
        assertNotNull(data.active?.finishedAt)
        data = ReadingEngine.nextSessionActivity(data)
        data = ReadingEngine.field(data, "answer", readingWrong(data).toString())
        store.save(data)
        assertEquals(data, ReadingStore(file).load())
        data = ReadingEngine.submit(ReadingStore(file).load(), "answer")
        data = ReadingEngine.nextSessionActivity(data)
        data = readingAnswer(data, readingWrong(data))
        store.save(data)
        assertEquals(data, ReadingStore(file).load())
        assertEquals(3, data.sessionAnswered(session))
        assertEquals(1, data.sessionCorrect(session))
        assertEquals(2, data.sessionWrong(session))
        assertNotNull(data.activeSession?.finishedAt)
        assertEquals(12, data.attempts.count { it.abandoned })
    }

    @Test fun disabledGamesCannotBeStartedSubmittedResumedOrAdvancedThroughTheModel() {
        settings.edit().putStringSet("disabled_reading_games", setOf(ReadingGame.MISSING.name)).commit()
        val storage = RecordingStorage(readingSessionFixture().copy(page = ReadingPage.MENU))
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        val original = model.data
        model.startSession(ReadingGame.MISSING, 25)
        await { model.message != null }
        assertEquals(original, model.data)
        model.submit("answer")
        model.finishActivity()
        model.nextSessionActivity()
        model.act { ReadingEngine.resume(it, requireNotNull(it.active).id) }
        model.act { ReadingEngine.field(it, "answer", readingCorrect(it).toString()) }
        model.act { ReadingEngine.renameProfile(it, "Checked") }
        await { model.data?.profile?.name == "Checked" && !model.saving }
        assertEquals(original?.attempts, model.data?.attempts)
        assertEquals(ReadingPage.MENU, model.data?.page)
        assertEquals(1, model.data?.sessions?.size)
    }

    @Test fun failedSessionAllocationRetriesItsOriginalRewardAndTimerSnapshot() {
        settings.edit().putBoolean("reading_rewards_enabled", false).commit()
        val storage = RecordingStorage(readingFixture()).apply { failSave = true }
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        model.startSession(ReadingGame.MISSING, 25)
        await { model.storageError != null && !model.saving }
        assertTrue(storage.saved.sessions.isEmpty())
        settings.edit().putBoolean("reading_rewards_enabled", true).putBoolean("show_timer", true)
            .putInt("time_limit_minutes", 5).commit()
        storage.failSave = false
        model.retry()
        await { model.data?.activeSession != null && model.storageError == null && !model.saving }
        assertFalse(requireNotNull(model.data?.activeSession).rewardsEnabledAtStart)
        assertNull(model.data?.activeSession?.timeLimitMs)
        assertEquals(25, model.data?.activeSession?.questionCount)
        assertEquals(1, model.data?.sessions?.size)
    }

    @Test fun failedClockSavePreservesQueuedSubmissionForRetry() {
        val data = ReadingEngine.field(readingSessionFixture(), "answer",
            readingCorrect(readingSessionFixture()).toString())
        val storage = RecordingStorage(data)
        var clock = 0L
        val model = ReadingViewModel(application, storage, clock = { clock })
        await { model.data != null && !model.saving }
        model.setSessionForeground(true)
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        storage.writeStarted = CountDownLatch(1)
        storage.failSave = true
        try {
            clock += 1_000
            shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
            await { storage.writeStarted?.count == 0L }
            assertFalse(model.saving)
            model.submit("answer")
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.storageError != null && !model.saving }
        assertEquals(data, model.data)
        assertEquals(data, storage.saved)
        storage.failSave = false
        model.retry()
        await { model.data?.active?.id != data.active?.id && model.storageError == null && !model.saving }
        val saved = storage.saved
        assertEquals(1_000L, saved.activeSession?.elapsedMs)
        assertEquals(1, saved.sessionAnswered(requireNotNull(saved.activeSession)))
        assertEquals(1, saved.attempts.first().checkpoints.getValue("answer").tries)
        assertEquals(1, storage.maximumConcurrentWrites)
        model.setSessionForeground(false)
    }

    @Test fun timeoutSaveStillBlocksInteractionAndRejectsQueuedSubmission() {
        val data = ReadingEngine.field(readingSessionFixture(timeLimitMs = 300_000), "answer",
            readingCorrect(readingSessionFixture()).toString())
        val storage = RecordingStorage(data)
        var clock = 0L
        val model = ReadingViewModel(application, storage, clock = { clock })
        await { model.data != null && !model.saving }
        model.setSessionForeground(true)
        val gate = CountDownLatch(1)
        storage.writeGate = gate
        storage.writeStarted = CountDownLatch(1)
        try {
            clock += 300_000
            shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
            await { storage.writeStarted?.count == 0L }
            assertTrue(model.saving)
            model.submit("answer")
        } finally {
            storage.writeGate = null
            gate.countDown()
        }
        await { model.data?.activeSession?.timedOut == true && !model.saving }
        val saved = storage.saved
        assertEquals(300_000L, saved.activeSession?.elapsedMs)
        assertEquals(0, saved.sessionAnswered(requireNotNull(saved.activeSession)))
        assertTrue(saved.attempts.all { it.abandoned })
        assertEquals(1, storage.maximumConcurrentWrites)
        model.setSessionForeground(false)
    }

    @Test fun sessionTimerCountsVisibleTimeOnlyAndResumesPersistedElapsedBeforeTimeout() {
        val store = ReadingStore(file)
        store.load()
        store.save(readingSessionFixture(count = 25, timeLimitMs = 300_000))
        var clock = 0L
        val model = ReadingViewModel(application, store, clock = { clock })
        await { model.data != null }
        model.setSessionForeground(true)
        clock += 5_000
        model.act { ReadingEngine.hint(it) }
        await { model.data?.activeSession?.elapsedMs == 5_000L && model.data?.active?.hints == 1 && !model.saving }
        model.setSessionForeground(false)
        clock += 60_000
        model.act { it.copy(page = ReadingPage.MENU) }
        await { model.data?.page == ReadingPage.MENU && !model.saving }
        assertEquals(5_000L, model.sessionElapsedMs)
        assertEquals(295_000L, model.sessionRemainingMs)
        assertEquals(5_000L, ReadingStore(file).load().activeSession?.elapsedMs)

        clock += 1_000_000
        val resumed = ReadingViewModel(application, ReadingStore(file), clock = { clock })
        await { resumed.data != null }
        assertEquals(5_000L, resumed.sessionElapsedMs)
        resumed.act { ReadingEngine.resume(it, requireNotNull(it.active).id) }
        await { resumed.data?.page == ReadingPage.ACTIVITY && !resumed.saving }
        resumed.setSessionForeground(true)
        clock += 295_000
        resumed.submit("answer")
        await { resumed.data?.activeSession?.timedOut == true && !resumed.saving }
        val finished = requireNotNull(resumed.data)
        assertEquals(300_000L, finished.activeSession?.elapsedMs)
        assertEquals(0L, resumed.sessionRemainingMs)
        assertEquals(0, finished.sessionAnswered(requireNotNull(finished.activeSession)))
        assertNull(finished.activeSession?.prizeType)
        assertTrue(finished.attempts.all { it.abandoned })
        assertEquals(finished, ReadingStore(file).load())
        ReadingEngine.validate(finished)
        resumed.setSessionForeground(false)
    }

    @Test fun sessionPrizeUsesLiveFinishPreferencesAndFailedSaveCannotRerollIt() {
        var data = readingSessionFixture(count = 25)
        repeat(24) { data = ReadingEngine.nextSessionActivity(readingSolve(data)) }
        data = readingAnswer(data, readingCorrect(data))
        val storage = RecordingStorage(data)
        val model = ReadingViewModel(application, storage)
        await { model.data != null }
        settings.edit().putStringSet("disabled_rewards",
            (RewardType.entries.toSet() - RewardType.LOLLIPOP).map { it.name }.toSet()).commit()
        storage.failSave = true
        model.finishActivity()
        await { model.storageError != null && !model.saving }
        assertNull(model.data?.activeSession?.finishedAt)
        settings.edit().putBoolean("reading_rewards_enabled", false)
            .putStringSet("disabled_rewards", RewardType.entries.map { it.name }.toSet()).commit()
        storage.failSave = false
        model.retry()
        await { model.data?.activeSession?.finishedAt != null && model.storageError == null && !model.saving }
        assertEquals(RewardType.LOLLIPOP, model.data?.activeSession?.prizeType)
        assertEquals(25, requireNotNull(model.data).sessionCorrect(requireNotNull(model.data?.activeSession)))

        val disabledStorage = RecordingStorage(data)
        val disabledModel = ReadingViewModel(application, disabledStorage)
        await { disabledModel.data != null }
        disabledModel.finishActivity()
        await { disabledModel.data?.activeSession?.finishedAt != null && !disabledModel.saving }
        assertNull(disabledModel.data?.activeSession?.prizeType)
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
