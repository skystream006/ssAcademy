package com.ssmath.app

import java.io.File
import java.io.IOException
import java.time.ZoneOffset
import java.util.UUID
import java.util.Locale
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class HistoryStoreTest {
    private companion object {
        val snapshotJson = Json { encodeDefaults = true }
    }

    private val directory: File = File("build/test-data/history-${UUID.randomUUID()}").apply { mkdirs() }
    private val file = File(directory, "practice_history.json")
    private val store = HistoryStore(file)

    @After fun cleanup() { directory.deleteRecursively() }

    private fun result(finishedAt: Long, correct: Int = 2) = PracticeResult(
        id = finishedAt, finishedAt = finishedAt, operation = Operation.ADDITION, maximum = 10, durationMs = 61_000,
        attempts = List(correct) { Attempt(Problem(1, it + 1, Operation.ADDITION), it + 2) } +
            List(5) { Attempt(Problem(2, 2, Operation.ADDITION), 5) })

    @Test fun savesResultsNewestFirstAndReloadsThem() {
        assertEquals(emptyList<PracticeResult>(), store.load())
        store.add(result(1_000))
        store.add(result(3_000, correct = 4))
        store.add(result(2_000))
        val loaded = HistoryStore(file).load()
        assertEquals(listOf(3_000L, 2_000L, 1_000L), loaded.map { it.finishedAt })
        assertEquals(4, loaded.first().correct)
        assertEquals(5, loaded.first().wrong)
        assertTrue(loaded.first().attempts.first().correct)
        assertEquals(result(3_000, correct = 4), loaded.first())
    }

    @Test fun savesTheSelectedMinimum() {
        val result = result(1_000).copy(minimum = 4,
            attempts = listOf(Attempt(Problem(4, 5, Operation.ADDITION), 9)))
        store.add(result)
        assertEquals(result, HistoryStore(file).load().single())
    }

    @Test fun olderResultsWithoutAMinimumStillLoad() {
        file.writeText("""[{"id":1000,"finishedAt":1000,"operation":"ADDITION","maximum":10,
            "durationMs":1000,"attempts":[{"problem":{"left":2,"right":3,"operation":"ADDITION"},"given":5}]}]""")
        val result = store.load().single()
        assertEquals(1, result.minimum)
        assertEquals(10, result.maximum)
        assertEquals(1, result.correct)
        assertFalse(result.isMultiplicationMatrix)
        assertFalse(result.isLogicMath)
        assertNull(result.attempts.single().problem.logicPuzzle)
    }

    @Test fun matrixIdentityAndAnswersRoundTripWithoutReclassifyingOrdinaryMultiplication() {
        val attempts = List(121) { index ->
            val problem = MultiplicationMatrix.problemAt(index)
            Attempt(problem, if (index == 11) 0 else problem.answer)
        }
        val matrix = PracticeResult(1, 1, Operation.MULTIPLICATION, 11, 1000, attempts,
            isMultiplicationMatrix = true)
        val ordinary = matrix.copy(id = 2, finishedAt = 2, isMultiplicationMatrix = false)
        store.add(matrix)
        store.add(ordinary)
        assertEquals(listOf(ordinary, matrix), HistoryStore(file).load())
        assertEquals("Multiplication Matrix", matrix.gameLabel)
        assertEquals("Multiplication", ordinary.gameLabel)
    }

    @Test fun bothGameModesRoundTripWithoutChangingResultMetadata() {
        val practice = result(1).copy(gameMode = GameMode.PRACTICE)
        val rewards = eligible(2).copy(gameMode = GameMode.REWARDS)
        store.add(practice)
        store.add(rewards)
        assertEquals(listOf(rewards, practice), HistoryStore(file).load())
        assertTrue(file.readText().contains("\"gameMode\":\"PRACTICE\""))
        assertTrue(file.readText().contains("\"gameMode\":\"REWARDS\""))
    }

    @Test fun legacyResultsDefaultToRewardsAndKeepPrizeClaimsAndCollections() {
        val pending = eligible(1)
        val encoded = Json.encodeToString(PracticeResult.serializer(), pending)
        assertFalse(encoded.contains("gameMode"))
        listOf(
            "[$encoded]",
            """{"history":[$encoded],"rewards":{"LOLLIPOP":{"whole":2,"fragments":1}},
                "pokemons":["PIKACHU"]}"""
        ).forEach { legacy ->
            file.writeText(legacy)
            val before = store.loadSnapshot()
            assertEquals(GameMode.REWARDS, before.history.single().gameMode)
            assertEquals(RewardThresholds(), before.history.single().rewardThresholds)
            assertEquals(RewardTier.TIER_2, before.history.single().matrixRewardTier)
            assertEquals(pending, before.history.single())
            val claimed = store.claimReward(1)
            val expected = (before.rewards[RewardType.LOLLIPOP] ?: RewardBalance()).addFragment()
            assertEquals(expected, claimed.rewards[RewardType.LOLLIPOP])
            assertEquals(before.pokemons, claimed.pokemons)
            assertEquals(PrizeAward(RewardType.LOLLIPOP, expected), claimed.history.single().prize)
            assertEquals(claimed, HistoryStore(file).loadSnapshot())
            assertEquals(claimed, store.claimReward(1))
        }
    }

    @Test fun practiceCannotClaimAPrizeOrCollectAPokemonButCanCollectOtherScenes() {
        val practice = eligible(1).copy(gameMode = GameMode.PRACTICE)
        store.add(practice)
        val before = store.loadSnapshot()
        val bytes = file.readText()
        assertEquals(before, store.claimReward(1))
        assertEquals(before, store.collectPokemon(1, Celebration.PIKACHU))
        assertEquals(bytes, file.readText())
        val collected = store.collectPokemon(1, Celebration.PARTY)
        assertEquals(setOf(Celebration.PARTY), collected.pokemons)
        assertEquals(practice.copy(pokemonReward = Celebration.PARTY), collected.history.single())
        assertTrue(collected.rewards.isEmpty())
        assertEquals(collected, HistoryStore(file).loadSnapshot())
    }

    @Test fun divisionLimitsRoundTripWithoutReinterpretingLegacyResults() {
        val legacy = result(1).copy(operation = Operation.DIVISION, minimum = 3, maximum = 12)
        val current = legacy.copy(id = 2, finishedAt = 2, minimum = 1, maximum = 100, maximumSecond = 7)
        store.add(legacy)
        store.add(current)
        assertEquals(listOf(current, legacy), HistoryStore(file).load())
        assertEquals("first up to 100 · second up to 7", store.load().first().numberDescription)
        assertEquals("numbers 3 to 12", store.load().last().numberDescription)
        file.writeText("""[{"id":3,"finishedAt":3,"operation":"DIVISION","minimum":3,"maximum":12,
            "durationMs":1000,"attempts":[]}]""")
        assertNull(store.load().single().maximumSecond)
        assertEquals("numbers 3 to 12", store.load().single().numberDescription)
    }

    @Test fun deletesAndClearsResults() {
        store.add(result(1_000))
        store.add(result(2_000))
        assertEquals(listOf(2_000L), store.delete(1_000).map { it.id })
        assertEquals(listOf(2_000L), store.load().map { it.id })
        assertEquals(emptyList<PracticeResult>(), store.clear())
        assertFalse(file.exists())
        assertEquals(emptyList<PracticeResult>(), store.load())
    }

    @Test fun keepsOnlyTheMostRecentResults() {
        repeat(MAX_HISTORY_RESULTS + 5) { store.add(result(it.toLong() + 1)) }
        val loaded = store.load()
        assertEquals(MAX_HISTORY_RESULTS, loaded.size)
        assertEquals((MAX_HISTORY_RESULTS + 5).toLong(), loaded.first().id)
    }

    @Test fun corruptHistoryIsIgnored() {
        file.writeText("not json")
        assertEquals(emptyList<PracticeResult>(), store.load())
        store.add(result(5_000))
        assertEquals(listOf(5_000L), store.load().map { it.id })
    }

    @Test fun formatsDateAndTime() {
        val text = formatFinishedAt(0, ZoneOffset.UTC)
        assertTrue(text, text.contains("1970"))
    }

    @Test fun collectsAllCelebrationsOnceAndReloadsWithoutChangingHistoryOrRewards() {
        store.add(eligible(1))
        store.claimReward(1)
        val before = store.loadSnapshot()
        Celebration.entries.forEach { store.collectPokemon(it) }
        val expected = before.copy(pokemons = Celebration.standardCelebrations.toSet())
        assertEquals(expected, HistoryStore(file).loadSnapshot())
        val bytes = file.readText()
        Celebration.entries.forEach { assertEquals(expected, store.collectPokemon(it)) }
        assertEquals(bytes, file.readText())
    }

    @Test fun clearingOrDeletingHistoryKeepsCelebrationsWithoutLinkedResults() {
        store.collectPokemon(Celebration.PIKACHU)
        store.collectPokemon(Celebration.PARTY)
        val expected = setOf(Celebration.PIKACHU, Celebration.PARTY)
        store.clear()
        assertEquals(expected, HistoryStore(file).loadSnapshot().pokemons)
        val saved = store.addNewResult(eligible(1))
        store.claimReward(saved.result.id)
        store.delete(saved.result.id)
        store.clear()
        val restored = HistoryStore(file).loadSnapshot()
        assertEquals(expected, restored.pokemons)
        assertEquals(RewardBalance(), restored.rewards[RewardType.LOLLIPOP])
        assertTrue(restored.history.isEmpty())
    }

    @Test fun presentedCelebrationAndResultAreSavedTogetherWithoutChangingFragmentClaims() {
        val pending = eligible(1)
        store.add(pending)
        val collected = store.collectPokemon(1, Celebration.PIKACHU)
        assertEquals(pending.copy(pokemonReward = Celebration.PIKACHU), collected.history.single())
        assertEquals(setOf(Celebration.PIKACHU), collected.pokemons)
        assertEquals(collected, HistoryStore(file).loadSnapshot())
        val bytes = file.readText()
        assertEquals(collected, store.collectPokemon(1, Celebration.PIKACHU))
        assertEquals(collected, store.collectPokemon(1, Celebration.PARTY))
        assertEquals(bytes, file.readText())
        store.add(pending)
        assertEquals(collected, store.loadSnapshot())
        val claimed = store.claimReward(1)
        assertEquals(Celebration.PIKACHU, claimed.history.single().pokemonReward)
        assertEquals(RewardBalance(0, 1), claimed.rewards[RewardType.LOLLIPOP])
        assertEquals(claimed, store.collectPokemon(1, Celebration.PIKACHU))
    }

    @Test fun deletingAndClearingRemoveLinkedCelebrationsButKeepOtherResultsAndUnlinkedEntries() {
        store.collectPokemon(Celebration.SQUIRTLE)
        (1L..3L).forEach { store.add(eligible(it)) }
        store.collectPokemon(1, Celebration.PIKACHU)
        store.collectPokemon(2, Celebration.PIKACHU)
        store.claimReward(3)
        store.collectPokemon(3, Celebration.PARTY)
        assertNotNull(store.load().first().prize)
        store.delete(1)
        assertEquals(setOf(Celebration.SQUIRTLE, Celebration.PIKACHU, Celebration.PARTY),
            HistoryStore(file).loadSnapshot().pokemons)
        store.delete(2)
        assertEquals(setOf(Celebration.SQUIRTLE, Celebration.PARTY),
            HistoryStore(file).loadSnapshot().pokemons)
        val afterDelete = store.loadSnapshot()
        assertEquals(afterDelete, store.collectPokemon(2, Celebration.PIKACHU))
        assertEquals(afterDelete, store.collectPokemon(999, Celebration.MEW))
        store.clear()
        val cleared = HistoryStore(file).loadSnapshot()
        assertTrue(cleared.history.isEmpty())
        assertEquals(setOf(Celebration.SQUIRTLE), cleared.pokemons)
        assertEquals(RewardBalance(), cleared.rewards[RewardType.LOLLIPOP])
        assertEquals(cleared, store.collectPokemon(3, Celebration.PARTY))
    }

    @Test fun failedResultCollectionLeavesBothHistoryAndCollectionUntouchedAndCanBeRetried() {
        store.add(eligible(1))
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.collectPokemon(1, Celebration.PIKACHU) }
        assertEquals(before, file.readText())
        assertNull(store.load().single().pokemonReward)
        assertTrue(store.loadSnapshot().pokemons.isEmpty())
        assertTrue(blocked.delete())
        val collected = store.collectPokemon(1, Celebration.PIKACHU)
        assertEquals(Celebration.PIKACHU, collected.history.single().pokemonReward)
        assertEquals(setOf(Celebration.PIKACHU), collected.pokemons)
    }

    @Test fun automaticHistoryTrimmingDoesNotRemoveCollectedRewards() {
        store.add(eligible(1))
        store.collectPokemon(1, Celebration.PIKACHU)
        repeat(MAX_HISTORY_RESULTS) { store.add(result(it + 2L)) }
        assertFalse(store.load().any { it.id == 1L })
        store.clear()
        assertEquals(setOf(Celebration.PIKACHU), HistoryStore(file).loadSnapshot().pokemons)
    }

    @Test fun collectionMigratesBothLegacyHistoryFormats() {
        listOf(
            """[{"id":1,"finishedAt":1,"operation":"ADDITION","maximum":10,"durationMs":1000,"attempts":[]}]""",
            """{"history":[{"id":1,"finishedAt":1,"operation":"ADDITION","maximum":10,"durationMs":1000,"attempts":[]}],
                "rewards":{"LOLLIPOP":{"whole":2,"fragments":1}}}"""
        ).forEach { legacy ->
            file.writeText(legacy)
            val before = store.loadSnapshot()
            assertTrue(before.pokemons.isEmpty())
            assertNull(before.history.single().pokemonReward)
            val collected = store.collectPokemon(Celebration.FINIZEN)
            assertEquals(before.copy(pokemons = setOf(Celebration.FINIZEN)), collected)
            assertEquals(collected, HistoryStore(file).loadSnapshot())
        }
    }

    @Test fun failedCollectionPreservesSavedDataAndCanBeRetried() {
        store.add(eligible(1))
        store.collectPokemon(Celebration.SQUIRTLE)
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.collectPokemon(Celebration.PALAFIN) }
        assertEquals(before, file.readText())
        assertTrue(blocked.delete())
        assertEquals(setOf(Celebration.SQUIRTLE, Celebration.PALAFIN),
            store.collectPokemon(Celebration.PALAFIN).pokemons)
    }

    @Test fun bulkCollectionExtendsExistingPokemonSnapshotsWithoutChangingOtherData() {
        file.writeText("""{"history":[],"rewards":{"LOLLIPOP":{"whole":2,"fragments":1}},
            "claimedResultIds":[1],"lastResultId":1,"pokemons":["PIKACHU"]}""")
        val before = store.loadSnapshot()
        assertEquals(setOf(Celebration.PIKACHU), before.pokemons)
        val expected = before.copy(pokemons = Celebration.standardCelebrations.toSet())
        assertEquals(expected, store.collectPokemons(Celebration.entries))
        assertEquals(expected, HistoryStore(file).loadSnapshot())
        val bytes = file.readText()
        repeat(2) { assertEquals(expected, store.collectPokemons(Celebration.entries)) }
        assertEquals(bytes, file.readText())
    }

    @Test fun failedBulkCollectionPreservesSavedDataAndCanBeRetried() {
        store.add(eligible(1))
        store.claimReward(1)
        store.collectPokemon(Celebration.SQUIRTLE)
        val expected = store.loadSnapshot().copy(pokemons = Celebration.standardCelebrations.toSet())
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.collectPokemons(Celebration.entries) }
        assertEquals(before, file.readText())
        assertTrue(blocked.delete())
        assertEquals(expected, store.collectPokemons(Celebration.entries))
    }

    private fun eligible(id: Long, type: RewardType = RewardType.LOLLIPOP) = PracticeResult(
        id, id, Operation.ADDITION, 10, 1_000,
        List(if (type.tier == RewardTier.TIER_2) 50 else 26) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
        prizeType = type
    )

    private fun reading(id: String = "reading-1") = ReadingRewardResult(
        id, "reader", ReadingGame.MISSING, ReadingLevel.SPROUT, 100, 25, 25, 25,
        prizeType = RewardType.LOLLIPOP, celebration = Celebration.PIKACHU
    )

    @Test fun customMathAndReadingRuleSnapshotsRoundTripAndClaimOnlyOnce() {
        val thresholds = RewardThresholds(15, 25)
        listOf(15 to RewardType.LOLLIPOP, 25 to RewardType.VIDEO_GAME).forEach { (count, type) ->
            val math = eligible(count.toLong(), type).copy(
                attempts = List(count) { Attempt(Problem(1, 1, Operation.ADDITION), 2) },
                questionCount = count, rewardThresholds = thresholds)
            store.add(math)
            val reading = reading("reading-$count").copy(questionCount = count, correct = count, answered = count,
                prizeType = type, rewardThresholds = thresholds)
            store.addReadingResult(reading)
            val restored = HistoryStore(file)
            assertEquals(math, restored.load().first { it.id == math.id })
            assertEquals(reading, restored.loadSnapshot().readingResults.first { it.sessionId == reading.sessionId })
            restored.claimReward(math.id)
            val claimed = restored.claimReadingReward(reading.sessionId)
            assertEquals(RewardBalance(0, 2), claimed.rewards[type])
            repeat(3) {
                assertEquals(claimed, HistoryStore(file).claimReward(math.id))
                assertEquals(claimed, HistoryStore(file).claimReadingReward(reading.sessionId))
                assertEquals(claimed, HistoryStore(file).addReadingResult(reading.copy(rewardThresholds = RewardThresholds())))
            }
            assertEquals(RewardBalance(0, 1), claimed.history.first { it.id == math.id }.prize?.balance)
            assertEquals(RewardBalance(0, 2),
                claimed.readingResults.first { it.sessionId == reading.sessionId }.prize?.balance)
        }
        val readingResult = store.loadSnapshot().readingResults.first()
        val deleted = store.deleteReadingResults(setOf(readingResult.sessionId))
        assertEquals(deleted, store.addReadingResult(readingResult.copy(prize = null)))
        assertEquals(deleted, store.claimReadingReward(readingResult.sessionId))
    }

    @Test fun matrixClaimsUseStoredTierEvenAboveQuestionMinimaAndRejectIncompleteGames() {
        val thresholds = RewardThresholds(200, 300)
        RewardTier.entries.forEachIndexed { index, tier ->
            val type = RewardType.entries.first { it.tier == tier }
            val matrix = eligible(index + 1L, type).copy(operation = Operation.MULTIPLICATION, maximum = 11,
                questionCount = 121, isMultiplicationMatrix = true, rewardThresholds = thresholds, matrixRewardTier = tier,
                attempts = List(121) { index -> MultiplicationMatrix.problemAt(index).let { Attempt(it, it.answer) } })
            store.add(matrix)
            val claimed = HistoryStore(file).claimReward(matrix.id)
            assertEquals(type, claimed.history.first { it.id == matrix.id }.prize?.type)
            assertEquals(RewardBalance(0, 1), claimed.rewards[type])
            assertEquals(claimed, HistoryStore(file).claimReward(matrix.id))
        }
        val baseline = store.loadSnapshot()
        val matrix = baseline.history.first().copy(prize = null)
        listOf(
            matrix.copy(questionCount = 120, attempts = matrix.attempts.take(120)),
            matrix.copy(attempts = matrix.attempts.take(120)),
            matrix.copy(timedOut = true),
            matrix.copy(isMultiplicationMatrix = false),
            matrix.copy(gameMode = GameMode.PRACTICE)
        ).forEachIndexed { index, invalid ->
            val candidate = invalid.copy(id = 10L + index)
            store.add(candidate)
            assertEquals(baseline.rewards, store.claimReward(candidate.id).rewards)
        }
    }

    @Test fun legacyMathAwardsRemainClaimableAfterTheirRewardTypeChangesTier() {
        for (count in listOf(50, 121)) {
            val matrix = count == 121
            val old = PracticeResult(count.toLong(), count.toLong(),
                if (matrix) Operation.MULTIPLICATION else Operation.ADDITION, 11, 1000,
                List(count) { index ->
                    val problem = if (matrix) MultiplicationMatrix.problemAt(index) else Problem(1, 1, Operation.ADDITION)
                    Attempt(problem, problem.answer)
                }, isMultiplicationMatrix = matrix, prizeType = RewardType.BED_TIME)
            val encoded = Json.encodeToString(PracticeResult.serializer(), old)
            assertFalse(encoded.contains("rewardThresholds"))
            assertFalse(encoded.contains("matrixRewardTier"))
            file.writeText("[$encoded]")
            val claimed = HistoryStore(file).claimReward(old.id)
            assertEquals(PrizeAward(RewardType.BED_TIME, RewardBalance(0, 1)), claimed.history.single().prize)
            assertEquals(RewardBalance(0, 1), claimed.rewards[RewardType.BED_TIME])
            assertEquals(claimed, HistoryStore(file).claimReward(old.id))
        }
    }

    @Test fun legacyReadingReceiptsKeepDefaultRulesAndNewRulesDoNotCreateRetroactiveAwards() {
        val old = reading()
        val encoded = Json.encodeToString(ReadingRewardResult.serializer(), old)
        assertFalse(encoded.contains("rewardThresholds"))
        file.writeText("""{"history":[],"rewards":{},"readingResults":[$encoded]}""")
        assertEquals(RewardThresholds(), store.loadSnapshot().readingResults.single().rewardThresholds)
        val claimed = store.claimReadingReward(old.sessionId)
        assertEquals(RewardBalance(0, 1), claimed.rewards[RewardType.LOLLIPOP])
        assertEquals(claimed, HistoryStore(file).claimReadingReward(old.sessionId))

        val noPrize = reading("no-prize").copy(questionCount = 15, answered = 15, correct = 15, prizeType = null)
        store.addReadingResult(noPrize)
        val before = store.loadSnapshot()
        assertEquals(before, store.addReadingResult(noPrize.copy(prizeType = RewardType.LOLLIPOP,
            rewardThresholds = RewardThresholds(15, 25))))
        assertEquals(before, store.claimReadingReward(noPrize.sessionId))
        val wrongTier = reading("wrong-tier").copy(questionCount = 25, answered = 25, correct = 25,
            rewardThresholds = RewardThresholds(15, 25))
        assertNull(store.addReadingResult(wrongTier).readingResults.first().prizeType)
        assertEquals(before.rewards, store.claimReadingReward(wrongTier.sessionId).rewards)
    }

    @Test fun retiredReadingReceiptsMigrateBeforeEnumDecodingAndPreserveUnrelatedInventory() {
        store.add(eligible(1))
        store.claimReward(1)
        store.collectPokemon(1, Celebration.PIKACHU)
        store.collectPokemon(Celebration.SQUIRTLE)
        val games = listOf("retired-chapter" to ReadingGame.DETECTIVE, "retired-partners" to ReadingGame.STUDIO,
            "retired-caption" to ReadingGame.SEQUENCE, "remaining" to ReadingGame.MISSING)
        games.forEach { (id, game) ->
            val celebration = when (game) {
                ReadingGame.STUDIO -> Celebration.PARTY
                ReadingGame.SEQUENCE -> Celebration.MEW
                else -> Celebration.PIKACHU
            }
            store.addReadingResult(reading(id).copy(game = game, celebration = celebration))
            store.claimReadingReward(id)
            store.collectReadingCelebration(id, celebration)
        }
        val before = file.readText().replace("\"game\":\"DETECTIVE\"", "\"game\":\"CHAPTER\"")
            .replace("\"game\":\"STUDIO\"", "\"game\":\"PARTNERS\"")
            .replace("\"game\":\"SEQUENCE\"", "\"game\":\"CAPTION\"")
        val removed = setOf("retired-chapter", "retired-partners", "retired-caption")
        val expected = store.deleteReadingResults(removed)
        file.writeText(before)
        assertEquals(expected, HistoryStore(file).loadSnapshot())
        assertEquals(RewardBalance(0, 2), expected.rewards[RewardType.LOLLIPOP])
        assertEquals(setOf(Celebration.PIKACHU, Celebration.SQUIRTLE), expected.pokemons)
        val cleaned = file.readText()
        assertFalse(cleaned.contains("\"game\":\"CHAPTER\""))
        assertFalse(cleaned.contains("\"game\":\"PARTNERS\""))
        assertFalse(cleaned.contains("\"game\":\"CAPTION\""))
        assertEquals(expected, store.loadSnapshot())
        assertEquals(expected, store.deleteReadingResults(removed))
        assertEquals(expected, store.addReadingResult(reading("retired-chapter")))
        assertEquals(expected, store.addReadingResult(reading("retired-caption")))
        assertEquals(cleaned, file.readText())
        assertEquals(2, store.add(eligible(2)).size)
    }

    @Test fun failedRetiredRewardMigrationKeepsTheOriginalFileAndRetriesWithoutDoubleSubtraction() {
        store.addReadingResult(reading("retired"))
        store.claimReadingReward("retired")
        val original = file.readText().replace("\"game\":\"MISSING\"", "\"game\":\"CAPTION\"")
        file.writeText(original)
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.loadSnapshot() }
        assertEquals(original, file.readText())
        assertTrue(blocked.delete())
        val migrated = store.loadSnapshot()
        assertTrue(migrated.readingResults.isEmpty())
        assertEquals(RewardBalance(), migrated.rewards[RewardType.LOLLIPOP])
        assertEquals(setOf("retired"), migrated.recordedReadingSessionIds)
        assertEquals(migrated, store.loadSnapshot())
        assertEquals(migrated, store.claimReadingReward("retired"))
    }

    @Test fun unknownReadingReceiptGamesStillFailWithoutOverwritingRewards() {
        store.addReadingResult(reading())
        val original = file.readText().replace("\"game\":\"MISSING\"", "\"game\":\"UNKNOWN\"")
        file.writeText(original)
        assertThrows(IOException::class.java) { store.loadSnapshot() }
        assertEquals(original, file.readText())
    }

    @Test fun readingRewardsMigrateLegacySnapshotsWithoutBecomingMathHistory() {
        val math = eligible(1)
        listOf(
            "[${Json.encodeToString(PracticeResult.serializer(), math)}]",
            """{"history":[${Json.encodeToString(PracticeResult.serializer(), math)}],"rewards":{"LOLLIPOP":{"whole":2,"fragments":1}}}"""
        ).forEach { legacy ->
            file.writeText(legacy)
            val before = store.loadSnapshot()
            val saved = store.addReadingResult(reading())
            assertEquals(before.history, saved.history)
            assertEquals(before.rewards, saved.rewards)
            assertEquals(listOf(reading()), saved.readingResults)
            assertEquals(setOf("reading-1"), saved.recordedReadingSessionIds)
            assertEquals(saved, HistoryStore(file).loadSnapshot())
        }
    }

    @Test fun readingAndMathClaimsShareFragmentCarryWithoutDuplicateAwardsAfterRestart() {
        store.add(eligible(1))
        store.claimReward(1)
        (1..2).forEach { store.addReadingResult(reading("reading-$it")) }
        store.claimReadingReward("reading-1")
        val claimed = store.claimReadingReward("reading-2")
        assertEquals(RewardBalance(1, 0), claimed.rewards[RewardType.LOLLIPOP])
        val restored = HistoryStore(file)
        val bytes = file.readText()
        repeat(3) {
            assertEquals(claimed, restored.claimReadingReward("reading-2"))
            assertEquals(claimed, restored.addReadingResult(reading("reading-2").copy(prizeType = RewardType.BED_TIME)))
        }
        assertEquals(bytes, file.readText())
        assertEquals(listOf(1L), claimed.history.map { it.id })
        assertEquals(PrizeAward(RewardType.LOLLIPOP, RewardBalance(1, 0)), claimed.readingResults.first().prize)
    }

    @Test fun readingClaimsRequireCompleteAccurateUntimedRewardSessionsAndTheCorrectTier() {
        listOf(
            reading().copy(questionCount = 24, answered = 24, correct = 24),
            reading().copy(correct = 22),
            reading().copy(answered = 24, correct = 24),
            reading().copy(timedOut = true),
            reading().copy(rewardsEnabledAtStart = false),
            reading().copy(prizeType = RewardType.RESTAURANT)
        ).forEachIndexed { index, invalid ->
            val result = invalid.copy(sessionId = "ineligible-$index")
            val saved = store.addReadingResult(result)
            assertNull(saved.readingResults.first().prizeType)
            assertEquals(saved, store.claimReadingReward(result.sessionId))
            assertTrue(saved.rewards.isEmpty())
        }
    }

    @Test fun readingCollectionRequiresTheSelectedPresentedCelebrationAndIsIdempotent() {
        val saved = store.addReadingResult(reading())
        assertTrue(saved.pokemons.isEmpty())
        assertNull(saved.readingResults.single().pokemonReward)
        assertEquals(saved, store.collectReadingCelebration("reading-1", Celebration.PARTY))
        assertEquals(saved, store.collectReadingCelebration("missing", Celebration.PIKACHU))
        val collected = store.collectReadingCelebration("reading-1", Celebration.PIKACHU)
        assertEquals(setOf(Celebration.PIKACHU), collected.pokemons)
        assertEquals(Celebration.PIKACHU, collected.readingResults.single().pokemonReward)
        assertTrue(collected.rewards.isEmpty())
        assertEquals(collected, HistoryStore(file).collectReadingCelebration("reading-1", Celebration.PIKACHU))
        assertEquals(collected, store.addReadingResult(reading()))
    }

    @Test fun mathDeletionAndClearKeepReadingResultsInventoryAndSharedCelebrations() {
        store.add(eligible(1))
        store.claimReward(1)
        store.collectPokemon(1, Celebration.PIKACHU)
        store.addReadingResult(reading())
        store.claimReadingReward("reading-1")
        store.collectReadingCelebration("reading-1", Celebration.PIKACHU)
        val readingResults = store.loadSnapshot().readingResults
        store.delete(1)
        store.clear()
        val remaining = HistoryStore(file).loadSnapshot()
        assertTrue(remaining.history.isEmpty())
        assertEquals(readingResults, remaining.readingResults)
        assertEquals(RewardBalance(0, 1), remaining.rewards[RewardType.LOLLIPOP])
        assertEquals(setOf(Celebration.PIKACHU), remaining.pokemons)
        file.writeText(snapshotJson.encodeToString(PracticeSnapshot.serializer(),
            PracticeSnapshot(readingResults = listOf(reading()), recordedReadingSessionIds = setOf("reading-1"))))
        store.clear()
        assertEquals(listOf(reading()), store.loadSnapshot().readingResults)
    }

    @Test fun readingDeletionRemovesOnlyLinkedRewardsAndKeepsMathOtherReadersAndSharedCelebrations() {
        store.add(eligible(1))
        store.claimReward(1)
        store.collectPokemon(1, Celebration.PIKACHU)
        store.collectPokemon(Celebration.SQUIRTLE)
        for (id in listOf("first", "second", "other-reader")) {
            store.addReadingResult(reading(id).copy(ownerId = if (id == "other-reader") "other" else "reader",
                celebration = if (id == "other-reader") Celebration.PIKACHU else Celebration.PARTY))
            store.claimReadingReward(id)
            store.collectReadingCelebration(id, if (id == "other-reader") Celebration.PIKACHU else Celebration.PARTY)
        }
        val before = store.loadSnapshot()
        val first = store.deleteReadingResults(setOf("first"))
        assertEquals(before.history, first.history)
        assertEquals(RewardBalance(1, 0), first.rewards[RewardType.LOLLIPOP])
        assertEquals(before.pokemons, first.pokemons)
        val second = store.deleteReadingResults(setOf("second", "unrecorded"))
        assertEquals(before.history, second.history)
        assertEquals(listOf("other-reader"), second.readingResults.map { it.sessionId })
        assertEquals(RewardBalance(0, 2), second.rewards[RewardType.LOLLIPOP])
        assertEquals(setOf(Celebration.PIKACHU, Celebration.SQUIRTLE), second.pokemons)
        assertEquals(before.claimedResultIds, second.claimedResultIds)
        assertEquals(before.recordedReadingSessionIds + "unrecorded", second.recordedReadingSessionIds)
        val restored = HistoryStore(file)
        assertEquals(second, restored.loadSnapshot())
        assertEquals(second, restored.deleteReadingResults(setOf("first", "second")))
        assertEquals(second, restored.addReadingResult(reading("first")))
        assertEquals(second, restored.addReadingResult(reading("unrecorded")))
        assertEquals(second, restored.claimReadingReward("first"))
        assertEquals(second, restored.collectReadingCelebration("first", Celebration.PARTY))
        val last = restored.deleteReadingResults(setOf("other-reader"))
        assertEquals(RewardBalance(0, 1), last.rewards[RewardType.LOLLIPOP])
        assertEquals(second.pokemons, last.pokemons)
    }

    @Test fun failedReadingDeletionLeavesRewardsTogetherAndRetryCannotSubtractUsedRewardsTwice() {
        repeat(3) {
            store.addReadingResult(reading("reading-$it"))
            store.claimReadingReward("reading-$it")
        }
        store.useReward(RewardType.LOLLIPOP)
        val before = store.loadSnapshot()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.deleteReadingResults(setOf("reading-0")) }
        assertEquals(before, store.loadSnapshot())
        assertTrue(blocked.delete())
        val deleted = store.deleteReadingResults(setOf("reading-0"))
        assertEquals(RewardBalance(), deleted.rewards[RewardType.LOLLIPOP])
        store.addReadingResult(reading("new"))
        store.claimReadingReward("new")
        val after = store.deleteReadingResults(setOf("reading-0"))
        assertEquals(RewardBalance(0, 1), after.rewards[RewardType.LOLLIPOP])
    }

    @Test fun readingRewardsRemainLinkedBeyondMathRetentionUntilExplicitDeletion() {
        val results = List(MAX_HISTORY_RESULTS) { reading("reading-$it") }
        file.writeText(snapshotJson.encodeToString(PracticeSnapshot.serializer(), PracticeSnapshot(
            readingResults = results, recordedReadingSessionIds = results.map { it.sessionId }.toSet(),
            rewards = mapOf(RewardType.LOLLIPOP to RewardBalance(1, 0)), pokemons = setOf(Celebration.PIKACHU)
        )))
        val oldest = results.last()
        store.claimReadingReward(oldest.sessionId)
        store.collectReadingCelebration(oldest.sessionId, Celebration.PIKACHU)
        val newest = reading("newest").copy(finishedAt = 1)
        val saved = store.addReadingResult(newest)
        assertEquals(MAX_HISTORY_RESULTS + 1, saved.readingResults.size)
        assertEquals(newest, saved.readingResults.first())
        assertTrue(saved.readingResults.any { it.sessionId == oldest.sessionId })
        assertEquals(saved, HistoryStore(file).addReadingResult(oldest))
        assertEquals(saved, store.claimReadingReward(oldest.sessionId))
        assertEquals(saved, store.collectReadingCelebration(oldest.sessionId, Celebration.PIKACHU))
        val deleted = store.deleteReadingResults(setOf(oldest.sessionId))
        assertEquals(RewardBalance(1, 0), deleted.rewards[RewardType.LOLLIPOP])
        assertTrue(deleted.pokemons.isEmpty())
        assertEquals(deleted, HistoryStore(file).addReadingResult(oldest))
        assertEquals(deleted, store.claimReadingReward(oldest.sessionId))
    }

    @Test fun readingClaimAndCollectionWriteFailuresAreAtomicAndRetryable() {
        store.addReadingResult(reading())
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.claimReadingReward("reading-1") }
        assertThrows(IOException::class.java) { store.collectReadingCelebration("reading-1", Celebration.PIKACHU) }
        assertEquals(before, file.readText())
        assertTrue(blocked.delete())
        store.claimReadingReward("reading-1")
        val collected = store.collectReadingCelebration("reading-1", Celebration.PIKACHU)
        assertEquals(RewardBalance(0, 1), collected.rewards[RewardType.LOLLIPOP])
        assertEquals(setOf(Celebration.PIKACHU), collected.pokemons)
        assertEquals(collected, HistoryStore(file).loadSnapshot())
    }

    @Test fun pendingPrizeAndAllResultMetadataSurviveReload() {
        val result = eligible(1).copy(questionCount = 26, minimum = 1)
        store.add(result)
        assertEquals(result, HistoryStore(file).load().single())
        assertTrue(store.loadSnapshot().rewards.isEmpty())
        assertNull(store.load().single().prize)
        assertEquals(100, store.load().single().percentCorrect)
    }

    @Test fun claimsAreAtomicIdempotentAndConvertEveryThreeFragments() {
        repeat(3) { index ->
            val id = index + 1L
            store.add(eligible(id))
            val claimed = store.claimReward(id)
            val expected = RewardBalance((index + 1) / 3, (index + 1) % 3)
            assertEquals(expected, claimed.rewards[RewardType.LOLLIPOP])
            assertEquals(PrizeAward(RewardType.LOLLIPOP, expected), claimed.history.first().prize)
            assertEquals(claimed, HistoryStore(file).loadSnapshot())
            val bytes = file.readText()
            repeat(3) { assertEquals(claimed, HistoryStore(file).claimReward(id)) }
            assertEquals(bytes, file.readText())
        }
        assertEquals(RewardBalance(1, 0), store.loadSnapshot().rewards[RewardType.LOLLIPOP])
    }

    @Test fun failedClaimDoesNotChangeEitherHistoryOrInventoryAndCanBeRetried() {
        store.add(eligible(1))
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.claimReward(1) }
        assertEquals(before, file.readText())
        assertNull(store.load().single().prize)
        assertTrue(store.loadSnapshot().rewards.isEmpty())
        assertTrue(blocked.delete())
        assertEquals(RewardBalance(0, 1), store.claimReward(1).rewards[RewardType.LOLLIPOP])
        assertEquals(RewardBalance(0, 1), HistoryStore(file).claimReward(1).rewards[RewardType.LOLLIPOP])
    }

    @Test fun usingEachRewardPersistsExactlyOneWholeWithoutChangingFragmentsOrHistory() {
        RewardType.entries.forEachIndexed { index, type ->
            repeat(8) {
                val id = index * 8 + it + 1L
                store.addNewResult(eligible(id, type))
                store.claimReward(id)
            }
        }
        RewardType.entries.forEach { type ->
            val before = store.loadSnapshot()
            val expected = before.copy(rewards = before.rewards + (type to RewardBalance(1, 2)))
            assertEquals(expected, store.useReward(type))
            assertEquals(expected, HistoryStore(file).loadSnapshot())
            before.history.forEach { store.claimReward(it.id) }
            assertEquals(expected, store.loadSnapshot())
        }
        store.delete(store.load().first().id)
        store.clear()
        assertTrue(HistoryStore(file).loadSnapshot().rewards.values.all { it == RewardBalance() })
    }

    @Test fun unavailableRewardsNeverConsumeFragmentsOrCreateNegativeBalances() {
        assertEquals(PracticeSnapshot(), store.useReward(RewardType.LOLLIPOP))
        assertFalse(file.exists())
        repeat(5) {
            store.add(eligible(it + 1L))
            store.claimReward(it + 1L)
        }
        assertEquals(RewardBalance(0, 2), store.useReward(RewardType.LOLLIPOP).rewards[RewardType.LOLLIPOP])
        val before = file.readText()
        repeat(3) { store.useReward(RewardType.LOLLIPOP) }
        store.useReward(RewardType.VIDEO_GAME)
        assertEquals(before, file.readText())
    }

    @Test fun adminAdditionsCreateEachRewardAndUnitFromEmptyInventoryWithoutGameResults() {
        RewardType.entries.forEach { type ->
            listOf(true, false).forEach { fragment ->
                file.delete()
                val balance = if (fragment) RewardBalance(0, 1) else RewardBalance(1, 0)
                val expected = PracticeSnapshot(rewards = mapOf(type to balance))
                assertEquals(expected, store.addReward(type, fragment))
                assertEquals(expected, HistoryStore(file).loadSnapshot())
                val removed = store.removeReward(type, fragment)
                assertEquals(expected.copy(rewards = mapOf(type to RewardBalance())), removed)
                assertEquals(removed, HistoryStore(file).loadSnapshot())
            }
        }
    }

    @Test fun adminAdditionsCarryFragmentsAndPreserveAllOtherSnapshotData() {
        store.addNewResult(eligible(1))
        store.claimReward(1)
        store.addNewResult(eligible(2))
        store.addReadingResult(reading())
        store.claimReadingReward("reading-1")
        store.collectReadingCelebration("reading-1", Celebration.PIKACHU)
        store.addReadingResult(reading("pending-reading"))
        store.deleteReadingResults(setOf("deleted-reading"))
        RewardType.entries.forEach { type ->
            listOf(true, false, true, true).forEach { fragment ->
                val before = store.loadSnapshot()
                val previous = before.rewards[type] ?: RewardBalance()
                val total = previous.totalFragments + if (fragment) 1 else 3
                val expected = before.copy(rewards = before.rewards + (type to RewardBalance(total / 3, total % 3)))
                assertEquals(expected, store.addReward(type, fragment))
                assertEquals(expected, HistoryStore(file).loadSnapshot())
            }
        }
    }

    @Test fun manualAdditionsSurviveEarnedHistoryDeletionWithoutAllowingReclaims() {
        val mathResults = (1L..2L).map { store.addNewResult(eligible(it)).result }
        mathResults.forEach { store.claimReward(it.id) }
        val readingResults = (1..2).map { reading("reading-$it") }
        readingResults.forEach { store.addReadingResult(it); store.claimReadingReward(it.sessionId) }
        store.addReward(RewardType.LOLLIPOP, fragment = false)
        val added = store.addReward(RewardType.LOLLIPOP, fragment = true)
        assertEquals(RewardBalance(2, 2), added.rewards[RewardType.LOLLIPOP])
        val restored = HistoryStore(file)
        mathResults.forEach { restored.add(it); assertEquals(added, restored.claimReward(it.id)) }
        readingResults.forEach {
            assertEquals(added, restored.addReadingResult(it))
            assertEquals(added, restored.claimReadingReward(it.sessionId))
        }
        restored.delete(mathResults.first().id)
        assertEquals(RewardBalance(2, 1), restored.loadSnapshot().rewards[RewardType.LOLLIPOP])
        restored.clear()
        assertEquals(RewardBalance(2, 0), restored.loadSnapshot().rewards[RewardType.LOLLIPOP])
        val cleared = restored.deleteReadingResults(readingResults.map { it.sessionId }.toSet())
        assertEquals(RewardBalance(1, 1), cleared.rewards[RewardType.LOLLIPOP])
        assertTrue(cleared.history.isEmpty())
        assertTrue(cleared.readingResults.isEmpty())
        assertEquals(added.claimedResultIds, cleared.claimedResultIds)
        assertEquals(added.recordedReadingSessionIds, cleared.recordedReadingSessionIds)
        mathResults.forEach { restored.add(it); assertEquals(cleared, restored.claimReward(it.id)) }
        readingResults.forEach {
            assertEquals(cleared, restored.addReadingResult(it))
            assertEquals(cleared, restored.claimReadingReward(it.sessionId))
        }
        assertEquals(cleared, HistoryStore(file).loadSnapshot())
    }

    @Test fun failedAdminAdditionsPreserveTheSnapshotAndRetryAddsExactlyOneUnit() {
        store.addNewResult(eligible(1))
        store.claimReward(1)
        store.addReadingResult(reading())
        store.collectReadingCelebration("reading-1", Celebration.PIKACHU)
        listOf(true, false).forEach { fragment ->
            val before = store.loadSnapshot()
            val bytes = file.readText()
            val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
            repeat(2) {
                assertThrows(IOException::class.java) { store.addReward(RewardType.LOLLIPOP, fragment) }
                assertEquals(bytes, file.readText())
                assertEquals(before, store.loadSnapshot())
            }
            assertTrue(blocked.delete())
            val total = before.rewards.getValue(RewardType.LOLLIPOP).totalFragments + if (fragment) 1 else 3
            val expected = before.copy(rewards = mapOf(RewardType.LOLLIPOP to RewardBalance(total / 3, total % 3)))
            assertEquals(expected, store.addReward(RewardType.LOLLIPOP, fragment))
            assertEquals(expected, HistoryStore(file).loadSnapshot())
        }
    }

    @Test fun adminAdditionCapsRejectOverflowAtomicallyAndPermitRemainingFragments() {
        store.addNewResult(eligible(1))
        store.claimReward(1)
        store.addReadingResult(reading())
        store.collectReadingCelebration("reading-1", Celebration.PIKACHU)
        val maximumWhole = (Int.MAX_VALUE - 2) / 3
        val original = store.loadSnapshot().copy(rewards = mapOf(
            RewardType.LOLLIPOP to RewardBalance(maximumWhole - 1, 0),
            RewardType.VIDEO_GAME to RewardBalance(3, 1)
        ))
        file.writeText(Json.encodeToString(PracticeSnapshot.serializer(), original))
        val wholeAdded = store.addReward(RewardType.LOLLIPOP, fragment = false)
        assertEquals(original.copy(rewards = original.rewards + (RewardType.LOLLIPOP to RewardBalance(maximumWhole, 0))),
            wholeAdded)
        (0..2).forEach { fragments ->
            val before = store.loadSnapshot()
            val bytes = file.readText()
            assertThrows(IllegalArgumentException::class.java) { store.addReward(RewardType.LOLLIPOP, fragment = false) }
            assertEquals(bytes, file.readText())
            if (fragments < 2) {
                val expected = before.copy(rewards = before.rewards +
                    (RewardType.LOLLIPOP to RewardBalance(maximumWhole, fragments + 1)))
                assertEquals(expected, store.addReward(RewardType.LOLLIPOP, fragment = true))
                assertEquals(expected, HistoryStore(file).loadSnapshot())
            } else {
                assertThrows(IllegalArgumentException::class.java) { store.addReward(RewardType.LOLLIPOP, fragment = true) }
                assertEquals(bytes, file.readText())
                assertEquals(before, HistoryStore(file).loadSnapshot())
            }
        }
    }

    @Test fun adminRemovalPersistsEachUnitWithoutChangingHistoryCollectionsOrOtherRewards() {
        RewardType.entries.forEachIndexed { index, type ->
            repeat(5) {
                val saved = store.addNewResult(eligible(index * 5 + it + 1L, type))
                store.claimReward(saved.result.id)
            }
        }
        store.collectPokemon(Celebration.PIKACHU)
        val original = store.loadSnapshot()
        RewardType.entries.forEach { type ->
            val before = store.loadSnapshot()
            val fragmentRemoved = before.copy(rewards = before.rewards + (type to RewardBalance(1, 1)))
            assertEquals(fragmentRemoved, store.removeReward(type, fragment = true))
            assertEquals(fragmentRemoved, HistoryStore(file).loadSnapshot())
            val wholeRemoved = fragmentRemoved.copy(rewards = fragmentRemoved.rewards + (type to RewardBalance(0, 1)))
            assertEquals(wholeRemoved, store.removeReward(type, fragment = false))
            assertEquals(wholeRemoved, HistoryStore(file).loadSnapshot())
            original.history.forEach {
                store.add(it.copy(prize = null))
                store.claimReward(it.id)
            }
            assertEquals(wholeRemoved, store.loadSnapshot())
        }
        store.delete(original.history.first().id)
        store.clear()
        val cleared = HistoryStore(file).loadSnapshot()
        assertTrue(cleared.rewards.values.all { it == RewardBalance() })
        assertEquals(original.claimedResultIds, cleared.claimedResultIds)
        assertEquals(original.pokemons, cleared.pokemons)
    }

    @Test fun adminRemovalNeverBreaksWholeRewardsOrConsumesTheWrongUnit() {
        listOf(true, false).forEach {
            assertEquals(PracticeSnapshot(), store.removeReward(RewardType.LOLLIPOP, fragment = it))
        }
        assertFalse(file.exists())
        repeat(3) {
            store.add(eligible(it + 1L))
            store.claimReward(it + 1L)
        }
        val wholeOnly = file.readText()
        store.removeReward(RewardType.LOLLIPOP, fragment = true)
        store.removeReward(RewardType.VIDEO_GAME, fragment = false)
        assertEquals(wholeOnly, file.readText())
        store.removeReward(RewardType.LOLLIPOP, fragment = false)
        repeat(2) {
            store.add(eligible(it + 4L))
            store.claimReward(it + 4L)
        }
        val fragmentsOnly = file.readText()
        store.removeReward(RewardType.LOLLIPOP, fragment = false)
        assertEquals(fragmentsOnly, file.readText())
        repeat(4) { store.removeReward(RewardType.LOLLIPOP, fragment = true) }
        assertEquals(RewardBalance(), HistoryStore(file).loadSnapshot().rewards[RewardType.LOLLIPOP])
        val empty = file.readText()
        listOf(true, false).forEach { store.removeReward(RewardType.LOLLIPOP, fragment = it) }
        assertEquals(empty, file.readText())
    }

    @Test fun failedAdminRemovalPreservesTheSnapshotAndAllowsRetryForEitherUnit() {
        repeat(5) {
            store.add(eligible(it + 1L))
            store.claimReward(it + 1L)
        }
        listOf(true, false).forEach { fragment ->
            val before = file.readText()
            val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
            assertThrows(IOException::class.java) { store.removeReward(RewardType.LOLLIPOP, fragment) }
            assertEquals(before, file.readText())
            assertTrue(blocked.delete())
            val removed = store.removeReward(RewardType.LOLLIPOP, fragment)
            assertEquals(removed, HistoryStore(file).loadSnapshot())
        }
        assertEquals(RewardBalance(0, 1), store.loadSnapshot().rewards[RewardType.LOLLIPOP])
        store.add(eligible(6))
        assertEquals(RewardBalance(0, 2), store.claimReward(6).rewards[RewardType.LOLLIPOP])
    }

    @Test fun failedRewardUseLeavesTheSavedSnapshotUntouchedAndCanBeRetried() {
        repeat(3) {
            store.add(eligible(it + 1L))
            store.claimReward(it + 1L)
        }
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.useReward(RewardType.LOLLIPOP) }
        assertEquals(before, file.readText())
        assertTrue(blocked.delete())
        val used = store.useReward(RewardType.LOLLIPOP)
        assertEquals(RewardBalance(), used.rewards[RewardType.LOLLIPOP])
        assertEquals(used, HistoryStore(file).loadSnapshot())
    }

    @Test fun deletionRemovesRewardsButRetentionTrimmingPreservesThemWithoutDuplicatingClaims() {
        store.add(eligible(1))
        store.claimReward(1)
        store.delete(1)
        store.add(eligible(1))
        assertTrue(store.load().isEmpty())
        assertEquals(RewardBalance(), store.claimReward(1).rewards[RewardType.LOLLIPOP])
        store.add(eligible(2, RewardType.VIDEO_GAME))
        store.claimReward(2)
        repeat(MAX_HISTORY_RESULTS) { store.add(result(it + 3L)) }
        assertFalse(store.load().any { it.id == 2L })
        store.add(eligible(2, RewardType.VIDEO_GAME))
        assertEquals(RewardBalance(0, 1), store.claimReward(2).rewards[RewardType.VIDEO_GAME])
        val rewards = store.loadSnapshot().rewards
        store.clear()
        assertTrue(file.exists())
        assertTrue(store.load().isEmpty())
        assertEquals(rewards, HistoryStore(file).loadSnapshot().rewards)
        assertEquals(rewards, store.claimReward(2).rewards)
        store.add(eligible(MAX_HISTORY_RESULTS + 3L, RewardType.VIDEO_GAME))
        store.claimReward(MAX_HISTORY_RESULTS + 3L)
        store.clear()
        assertEquals(rewards, HistoryStore(file).loadSnapshot().rewards)
    }

    @Test fun deletingAClaimedResultRemovesOnlyItsFragmentIncludingFromWholeRewards() {
        RewardType.entries.forEachIndexed { index, type ->
            repeat(3) {
                val id = index * 3 + it + 1L
                store.add(eligible(id, type))
                store.claimReward(id)
            }
        }
        val before = store.loadSnapshot()
        val deleted = before.history.first()
        val type = deleted.prize!!.type
        store.delete(deleted.id)
        val after = HistoryStore(file).loadSnapshot()
        assertEquals(before.history.filterNot { it.id == deleted.id }, after.history)
        assertEquals(before.rewards + (type to RewardBalance(0, 2)), after.rewards)
        assertEquals(before.claimedResultIds, after.claimedResultIds)
        store.delete(deleted.id)
        store.claimReward(deleted.id)
        assertEquals(after, store.loadSnapshot())
    }

    @Test fun deletingUnclaimedOrUnrewardedResultsDoesNotDeductInventory() {
        store.add(eligible(1))
        store.claimReward(1)
        store.add(eligible(2))
        store.add(result(3))
        val rewards = store.loadSnapshot().rewards
        store.delete(2)
        store.delete(3)
        store.delete(999)
        assertEquals(rewards, store.loadSnapshot().rewards)
    }

    @Test fun deletingSpentRewardsNeverCreatesDebtOrRestoresOldBalances() {
        repeat(3) {
            store.add(eligible(it + 1L))
            store.claimReward(it + 1L)
        }
        store.useReward(RewardType.LOLLIPOP)
        store.delete(1)
        assertEquals(RewardBalance(), store.loadSnapshot().rewards[RewardType.LOLLIPOP])
        store.add(eligible(4))
        store.claimReward(4)
        assertEquals(RewardBalance(0, 1), store.loadSnapshot().rewards[RewardType.LOLLIPOP])
        store.clear()
        assertEquals(RewardBalance(), HistoryStore(file).loadSnapshot().rewards[RewardType.LOLLIPOP])
    }

    @Test fun failedDeletionAndClearKeepHistoryAndRewardsTogetherAndAllowRetry() {
        store.add(eligible(1))
        store.claimReward(1)
        store.collectPokemon(1, Celebration.PIKACHU)
        val before = file.readText()
        val blocked = File(directory, "${file.name}.tmp").apply { mkdir() }
        assertThrows(IOException::class.java) { store.delete(1) }
        assertEquals(before, file.readText())
        assertThrows(IOException::class.java) { store.clear() }
        assertEquals(before, file.readText())
        assertTrue(blocked.delete())
        store.clear()
        assertTrue(store.load().isEmpty())
        assertEquals(RewardBalance(), HistoryStore(file).loadSnapshot().rewards[RewardType.LOLLIPOP])
        assertTrue(HistoryStore(file).loadSnapshot().pokemons.isEmpty())
    }

    @Test fun staleResultSaveCannotOverwriteAnAlreadyClaimedPrize() {
        val pending = eligible(1)
        store.add(pending)
        val claimed = store.claimReward(1)
        store.add(pending)
        assertEquals(claimed.history, store.load())
        assertEquals(claimed.rewards, store.claimReward(1).rewards)
    }

    @Test fun legacyArrayMigratesOnTheNextWriteWithoutRetroactivePrizes() {
        file.writeText("""[{"id":1000,"finishedAt":1000,"operation":"ADDITION","maximum":10,
            "durationMs":1000,"attempts":[{"problem":{"left":2,"right":3,"operation":"ADDITION"},"given":5}]}]""")
        val legacy = store.load().single()
        assertEquals(legacy.attempts.size, legacy.questionCount)
        assertFalse(legacy.timedOut)
        assertNull(legacy.prizeType)
        assertNull(legacy.prize)
        store.add(eligible(1001))
        assertTrue(file.readText().startsWith("{"))
        assertEquals(legacy, store.load().last())
        assertTrue(store.loadSnapshot().rewards.isEmpty())
    }

    @Test fun malformedSnapshotsCannotSilentlyEraseRewards() {
        listOf(
            """{"history":[],"rewards":{"LOLLIPOP":{"whole":1,"fragments":3}}}""",
            """{"history":[],"rewards":{"FUTURE_REWARD":{"whole":1,"fragments":0}}}""",
            """{"history":[],"rewards":""",
            """{"history":[]}""",
            """{"history":"bad","rewards":{"LOLLIPOP":{"whole":1,"fragments":0}}}"""
        ).forEach { text ->
            file.writeText(text)
            assertThrows(IOException::class.java) { store.loadSnapshot() }
            assertThrows(IOException::class.java) { store.add(result(2)) }
            assertThrows(IOException::class.java) { store.claimReward(1) }
            assertThrows(IOException::class.java) { store.useReward(RewardType.LOLLIPOP) }
            assertThrows(IOException::class.java) { store.removeReward(RewardType.LOLLIPOP, fragment = true) }
            assertThrows(IOException::class.java) { store.removeReward(RewardType.LOLLIPOP, fragment = false) }
            assertThrows(IOException::class.java) { store.addReward(RewardType.LOLLIPOP, fragment = true) }
            assertThrows(IOException::class.java) { store.addReward(RewardType.LOLLIPOP, fragment = false) }
            assertThrows(IOException::class.java) { store.delete(1) }
            assertThrows(IOException::class.java) { store.clear() }
            assertEquals(text, file.readText())
        }
    }

    @Test fun resultIdsRemainUniqueAcrossReloadClockRollbackAndClearedRewardHistory() {
        val first = store.addNewResult(eligible(1000)).result
        val second = store.addNewResult(eligible(1000)).result
        val third = HistoryStore(file).addNewResult(eligible(500)).result
        assertEquals(listOf(1000L, 1001L, 1002L), listOf(first.id, second.id, third.id))
        store.claimReward(third.id)
        store.clear()
        assertEquals(1003L, HistoryStore(file).addNewResult(eligible(400)).result.id)
    }

    @Test fun clearingUnclaimedHistoryKeepsAllocatedIdsUniqueAfterReload() {
        val first = store.addNewResult(eligible(1000).copy(prizeType = null)).result
        store.clear()
        assertTrue(store.load().isEmpty())
        assertTrue(store.loadSnapshot().rewards.isEmpty())
        val next = HistoryStore(file).addNewResult(eligible(500).copy(prizeType = null)).result
        assertTrue(next.id > first.id)
    }

    @Test fun newResultSurvivesFullHistoryTrimmingAfterClockRollbackAndRemainsClaimable() {
        repeat(MAX_HISTORY_RESULTS) { store.add(result(1_000_000L + it)) }
        val saved = store.addNewResult(eligible(1))
        assertEquals(MAX_HISTORY_RESULTS, saved.snapshot.history.size)
        assertEquals(saved.result, saved.snapshot.history.last())
        assertEquals(1L, saved.result.finishedAt)
        assertTrue(saved.result.id > 1_000_000L + MAX_HISTORY_RESULTS - 1)
        assertEquals(saved.snapshot.history.map { it.finishedAt }.sortedDescending(),
            saved.snapshot.history.map { it.finishedAt })
        assertTrue(HistoryStore(file).load().any { it.id == saved.result.id })
        val claimed = HistoryStore(file).claimReward(saved.result.id)
        assertNotNull(claimed.history.single { it.id == saved.result.id }.prize)
        assertEquals(RewardBalance(fragments = 1), claimed.rewards[RewardType.LOLLIPOP])
    }

    @Test fun nonEligibleAndTimedOutResultsCannotBeClaimed() {
        val candidates = listOf(
            eligible(1).copy(prizeType = null),
            eligible(2).copy(timedOut = true),
            eligible(3).copy(questionCount = 30),
            eligible(4).copy(attempts = eligible(4).attempts.take(25)),
            eligible(5).copy(attempts = List(30) {
                Attempt(Problem(1, 1, Operation.ADDITION), if (it < 27) 2 else 3)
            })
        )
        candidates.forEach { store.add(it); assertTrue(store.claimReward(it.id).rewards.isEmpty()) }
        assertTrue(store.load().all { it.prize == null })
    }

    @Test fun timedOutResultMetadataAndZeroPercentRoundTrip() {
        val result = eligible(1).copy(attempts = emptyList(), questionCount = 26, timedOut = true, prizeType = null)
        store.add(result)
        assertEquals(result, store.load().single())
        assertEquals(0, store.load().single().percentCorrect)
    }

    @Test fun percentageTextKeepsWholeNumbersAndUpToTwoLocaleIndependentDecimalsOtherwise() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            fun percentage(correct: Int, count: Int) = eligible(1).copy(
                attempts = List(count) { Attempt(Problem(1, 1, Operation.ADDITION), if (it < correct) 2 else 3) },
                questionCount = count
            ).percentCorrectText
            assertEquals("100", percentage(26, 26))
            assertEquals("90", percentage(27, 30))
            assertEquals("90.1", percentage(901, 1000))
            assertEquals("90.01", percentage(892, 991))
            assertEquals("96.15", percentage(25, 26))
            assertEquals("0", percentage(0, 26))
            assertEquals("0", percentage(0, 0))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
