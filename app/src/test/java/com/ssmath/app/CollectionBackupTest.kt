package com.ssmath.app

import java.io.File
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CollectionBackupTest {
    @get:Rule val directory = TemporaryFolder()
    private val file get() = File(directory.root, "history.json")
    private val store get() = HistoryStore(file)
    private val backup = CollectionBackup(
        CollectionBackup.FORMAT, CollectionBackup.VERSION,
        RewardType.entries.associateWith { RewardBalance(it.ordinal + 1, it.ordinal % 3) },
        Celebration.entries.toSet()
    )

    private fun decode(text: String) = text.byteInputStream().use(CollectionBackup::read)

    private fun seedHistory() {
        store.addNewResult(PracticeResult(10, 10, Operation.ADDITION, 10, 100,
            List(25) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
            prizeType = RewardType.LOLLIPOP))
        store.claimReward(10)
        store.collectPokemon(10, Celebration.PIKACHU)
        store.addReadingResult(ReadingRewardResult("reading", "reader", ReadingGame.MISSING,
            ReadingLevel.SPROUT, 10, 25, 25, 25, prizeType = RewardType.LOLLIPOP,
            celebration = Celebration.PIKACHU))
        store.claimReadingReward("reading")
        store.collectReadingCelebration("reading", Celebration.PIKACHU)
    }

    @Test fun portableFileRoundTripsEveryRewardAndCelebrationWithoutHistoryOrDeviceData() {
        seedHistory()
        store.importCollections(backup, CollectionImportMode.REPLACE)
        val before = file.readText()
        val exported = CollectionBackup.encode(CollectionBackup.from(store.loadSnapshot()))
        assertEquals(setOf("format", "version", "rewards", "pokemons"), Json.parseToJsonElement(exported).jsonObject.keys)
        assertEquals(backup, decode(exported))
        assertTrue(exported.toByteArray().size < CollectionBackup.MAX_BYTES)
        val destination = HistoryStore(File(directory.root, "other-device.json"))
        destination.importCollections(decode(exported), CollectionImportMode.REPLACE)
        val restored = destination.loadSnapshot()
        assertEquals(backup.rewards, restored.rewards)
        assertEquals(Celebration.entries.toSet(), restored.pokemons)
        assertTrue(restored.history.isEmpty())
        assertTrue(restored.readingResults.isEmpty())
        assertTrue(restored.claimedResultIds.isEmpty())
        assertTrue(restored.recordedReadingSessionIds.isEmpty())
        assertEquals(before, file.readText())
    }

    @Test fun emptyAndByteOrderMarkedFilesRoundTrip() {
        val empty = CollectionBackup.from(PracticeSnapshot())
        val text = CollectionBackup.encode(empty)
        assertEquals(empty, decode(text))
        assertEquals(empty, decode("\uFEFF$text"))
        assertEquals(empty, CollectionBackup.from(store.importCollections(empty, CollectionImportMode.REPLACE)))
    }

    @Test fun incompleteMalformedUnknownAndInvalidFilesAreRejectedBeforeChangingCollections() {
        seedHistory()
        val text = CollectionBackup.encode(backup)
        val invalid = listOf(
            "", "not json", "[]", "{}", file.readText(),
            text.replace("\"version\": 1", "\"version\": 2"),
            text.replace("\"version\": 1,", ""),
            text.replace("\"format\": \"ssAcademy-collections\",", ""),
            text.replace("ssAcademy-collections", "different-app"),
            text.replace("\"rewards\"", "\"missingRewards\""),
            text.replace("\"pokemons\"", "\"missingPokemons\""),
            text.replace("\"PIKACHU\"", "\"UNKNOWN_POKEMON\""),
            text.replace("\"LOLLIPOP\"", "\"UNKNOWN_REWARD\""),
            text.replace("\"whole\": 1,", "\"whole\": -1,"),
            text.replace("\"whole\": 1,", "\"whole\": 2147483647,"),
            text.replace("\"fragments\": 0", "\"fragments\": 3"),
            text.replace("\"fragments\": 0", "\"fragments\": -1"),
            text.dropLast(2)
        )
        val before = file.readText()
        invalid.forEach { contents ->
            assertThrows(IOException::class.java) { decode(contents) }
            assertEquals(before, file.readText())
        }
        assertThrows(IOException::class.java) {
            byteArrayOf(0xFF.toByte()).inputStream().use(CollectionBackup::read)
        }
    }

    @Test fun sizeLimitIsEnforcedAtExactByteBoundary() {
        val text = CollectionBackup.encode(backup)
        val atLimit = text + " ".repeat(CollectionBackup.MAX_BYTES - text.toByteArray().size)
        assertEquals(backup, decode(atLimit))
        assertThrows(IOException::class.java) { decode("$atLimit ") }
    }

    @Test fun mergeAddsAndNormalizesBalancesUnionsAnimationsAndKeepsHistoryClaims() {
        seedHistory()
        val before = store.loadSnapshot()
        val merged = store.importCollections(backup, CollectionImportMode.MERGE)
        assertEquals(before.history, merged.history)
        assertEquals(before.readingResults, merged.readingResults)
        assertEquals(before.claimedResultIds, merged.claimedResultIds)
        assertEquals(before.recordedReadingSessionIds, merged.recordedReadingSessionIds)
        assertEquals(before.lastResultId, merged.lastResultId)
        assertEquals(Celebration.entries.toSet(), merged.pokemons)
        backup.rewards.forEach { (type, balance) ->
            assertEquals(balance.totalFragments + (before.rewards[type]?.totalFragments ?: 0),
                merged.rewards.getValue(type).totalFragments)
        }
        assertEquals(merged, store.loadSnapshot())
        store.delete(10)
        val deleted = store.deleteReadingResults(setOf("reading"))
        assertEquals(backup.rewards, deleted.rewards)
        assertEquals(backup.pokemons, deleted.pokemons)
        assertEquals(setOf(10L), deleted.claimedResultIds)
        assertEquals(setOf("reading"), deleted.recordedReadingSessionIds)
    }

    @Test fun replacementKeepsHistoricalGiftsButTheirDeletionCannotDeductFromImportedInventory() {
        seedHistory()
        val before = store.loadSnapshot()
        val replaced = store.importCollections(backup, CollectionImportMode.REPLACE)
        assertEquals(backup.rewards, replaced.rewards)
        assertEquals(backup.pokemons, replaced.pokemons)
        assertEquals(before.history.map { it.copy(prize = it.prize?.copy(deductOnHistoryDeletion = false)) },
            replaced.history)
        assertEquals(before.readingResults.map { it.copy(prize = it.prize?.copy(deductOnHistoryDeletion = false)) },
            replaced.readingResults)
        assertEquals(before.claimedResultIds, replaced.claimedResultIds)
        assertEquals(before.recordedReadingSessionIds, replaced.recordedReadingSessionIds)
        assertEquals(before.lastResultId, replaced.lastResultId)
        assertEquals(replaced, store.claimReward(10))
        assertEquals(replaced, store.claimReadingReward("reading"))
        assertEquals(replaced, store.loadSnapshot())
        store.clear()
        store.deleteReadingResults(setOf("reading"))
        assertEquals(backup, CollectionBackup.from(store.loadSnapshot()))
    }

    @Test fun unopenedGiftsCanStillBeClaimedAndDeletedAfterReplacement() {
        seedHistory()
        val before = store.loadSnapshot()
        val math = before.history.single().copy(id = 20, prize = null, pokemonReward = null)
        store.add(math)
        store.addReadingResult(before.readingResults.single().copy(sessionId = "unopened", prize = null,
            pokemonReward = null))
        store.importCollections(backup, CollectionImportMode.REPLACE)
        store.claimReward(20)
        val claimed = store.claimReadingReward("unopened")
        assertEquals(backup.rewards.getValue(RewardType.LOLLIPOP).totalFragments + 2,
            claimed.rewards.getValue(RewardType.LOLLIPOP).totalFragments)
        store.delete(20)
        store.deleteReadingResults(setOf("unopened"))
        assertEquals(backup, CollectionBackup.from(store.loadSnapshot()))
    }

    @Test fun replacingWithEmptyCollectionsClearsInventoryButNotReceipts() {
        seedHistory()
        store.importCollections(backup, CollectionImportMode.MERGE)
        val empty = CollectionBackup.from(PracticeSnapshot())
        val replaced = store.importCollections(empty, CollectionImportMode.REPLACE)
        assertEquals(empty, CollectionBackup.from(replaced))
        assertTrue(replaced.importedPokemons.isEmpty())
        assertEquals(1, replaced.history.size)
        assertEquals(1, replaced.readingResults.size)
    }

    @Test fun repeatedReplacementDoesNotDoubleRewardsAndExplicitRepeatedMergeAddsThemAgain() {
        val first = store.importCollections(backup, CollectionImportMode.REPLACE)
        assertEquals(first, store.importCollections(backup, CollectionImportMode.REPLACE))
        val merged = store.importCollections(backup, CollectionImportMode.MERGE)
        backup.rewards.forEach { (type, balance) ->
            assertEquals(balance.totalFragments * 2, merged.rewards.getValue(type).totalFragments)
        }
        assertEquals(backup.pokemons, merged.pokemons)
    }

    @Test fun overflowingMergeAndFailedAtomicSaveLeaveBothCollectionsUnchanged() {
        val maximum = backup.copy(rewards = mapOf(RewardType.LOLLIPOP to RewardBalance((Int.MAX_VALUE - 2) / 3, 2)))
        store.importCollections(maximum, CollectionImportMode.REPLACE)
        val bytes = file.readText()
        assertThrows(IllegalArgumentException::class.java) {
            store.importCollections(backup, CollectionImportMode.MERGE)
        }
        assertEquals(bytes, file.readText())
        assertTrue(File(directory.root, "history.json.tmp").mkdir())
        assertThrows(IOException::class.java) {
            store.importCollections(backup, CollectionImportMode.REPLACE)
        }
        assertEquals(bytes, file.readText())
    }
}
