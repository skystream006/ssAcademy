package com.ssmath.app

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val MAX_HISTORY_RESULTS = 500

/** A finished game saved with the date and time it ended. */
@Serializable
data class PracticeResult(
    val id: Long,
    val finishedAt: Long,
    val operation: Operation,
    val maximum: Int,
    val durationMs: Long,
    val attempts: List<Attempt>,
    val minimum: Int = MIN_MAXIMUM,
    val questionCount: Int = attempts.size,
    val timedOut: Boolean = false,
    val prizeType: RewardType? = null,
    val prize: PrizeAward? = null,
    val maximumSecond: Int? = null,
    val pokemonReward: Celebration? = null,
    val gameMode: GameMode = GameMode.REWARDS,
    val isMultiplicationMatrix: Boolean = false,
    val megaPokemonReward: Celebration? = null,
    val rewardThresholds: RewardThresholds = RewardThresholds(),
    val matrixRewardTier: RewardTier = RewardTier.TIER_2,
    val isLogicMath: Boolean = false
) {
    val pokemonRewards: Set<Celebration> get() = setOfNotNull(pokemonReward, megaPokemonReward)
    val gameLabel: String get() = when {
        isMultiplicationMatrix -> "Multiplication Matrix"
        isLogicMath -> "Challenge - Logic Math · ${operation.label}"
        else -> operation.label
    }
    val numberDescription: String get() =
        if (operation == Operation.DIVISION && maximumSecond != null) "first up to $maximum · second up to $maximumSecond"
        else "numbers $minimum to $maximum"
    val correct: Int get() = attempts.count { it.correct }
    val wrong: Int get() = attempts.count { !it.correct }
    val percentCorrect: Int get() = if (attempts.isEmpty()) 0 else (correct.toLong() * 100 / attempts.size).toInt()
    val percentCorrectText: String
        get() {
            if (attempts.isEmpty()) return "0"
            val numerator = correct.toLong() * 100
            return if (numerator % attempts.size == 0L) (numerator / attempts.size).toString()
            else String.format(Locale.ROOT, "%.2f", numerator.toDouble() / attempts.size).trimEnd('0').trimEnd('.')
        }
}

/** Reading receipts share inventory, but never become Math practice results. */
@Serializable
data class ReadingRewardResult(
    val sessionId: String,
    val ownerId: String,
    val game: ReadingGame,
    val level: ReadingLevel,
    val finishedAt: Long,
    val questionCount: Int,
    val correct: Int,
    val answered: Int,
    val timedOut: Boolean = false,
    val rewardsEnabledAtStart: Boolean = true,
    val prizeType: RewardType? = null,
    val prize: PrizeAward? = null,
    val celebration: Celebration? = null,
    val pokemonReward: Celebration? = null,
    val rewardThresholds: RewardThresholds = RewardThresholds()
) {
    val percentCorrectText: String
        get() {
            if (answered == 0) return "0"
            val numerator = correct.toLong() * 100
            return if (numerator % answered == 0L) (numerator / answered).toString()
            else String.format(Locale.ROOT, "%.2f", numerator.toDouble() / answered).trimEnd('0').trimEnd('.')
        }
    val completed: Boolean get() = questionCount > 0 && answered == questionCount && !timedOut
}

@Serializable
data class PracticeSnapshot(
    val history: List<PracticeResult> = emptyList(),
    val rewards: Map<RewardType, RewardBalance> = emptyMap(),
    val claimedResultIds: Set<Long> = emptySet(),
    val lastResultId: Long = 0,
    val pokemons: Set<Celebration> = emptySet(),
    val readingResults: List<ReadingRewardResult> = emptyList(),
    val recordedReadingSessionIds: Set<String> = emptySet()
)

data class SavedPractice(val result: PracticeResult, val snapshot: PracticeSnapshot)

fun formatFinishedAt(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(zone).format(Instant.ofEpochMilli(epochMillis))

/** Stores practice results privately as JSON, newest first. Not thread-safe; callers serialize access. */
class HistoryStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(PracticeResult.serializer())

    fun load(): List<PracticeResult> = loadSnapshot().history

    fun loadSnapshot(): PracticeSnapshot {
        if (!file.exists()) return PracticeSnapshot()
        val text = file.readText(Charsets.UTF_8)
        var migrated = false
        val snapshot = try {
            val element = json.parseToJsonElement(text)
            if (element is JsonArray) PracticeSnapshot(history = json.decodeFromJsonElement(serializer, element))
            else {
                // A damaged inventory must never be treated as an empty legacy history.
                require(element is JsonObject && "history" in element && "rewards" in element)
                val receipts = element["readingResults"]?.let { it as JsonArray }.orEmpty()
                val retired = receipts.filter {
                    it.jsonObject["game"]?.jsonPrimitive?.content in ReadingMigration.retiredGames
                }
                val cleaned = if (retired.isEmpty()) element else JsonObject(element +
                    ("readingResults" to JsonArray(receipts - retired.toSet())))
                val decoded = json.decodeFromJsonElement(PracticeSnapshot.serializer(), cleaned)
                migrated = retired.isNotEmpty()
                if (!migrated) decoded else {
                    val ids = retired.map { it.jsonObject.getValue("sessionId").jsonPrimitive.content }.toSet()
                    val fragments = retired.mapNotNull {
                        it.jsonObject["prize"]?.takeUnless { prize -> prize == JsonNull }?.let { prize ->
                            json.decodeFromJsonElement(PrizeAward.serializer(), prize).type
                        }
                    }.groupingBy { it }.eachCount()
                    val collected = retired.mapNotNull {
                        it.jsonObject["pokemonReward"]?.takeUnless { reward -> reward == JsonNull }?.let { reward ->
                            json.decodeFromJsonElement(Celebration.serializer(), reward)
                        }
                    }.toSet()
                    withoutReadingResults(decoded, ids, fragments, collected)
                }
            }
        } catch (error: Exception) {
            if (text.trimStart().startsWith("{")) throw IOException("Unable to read rewards and practice history.", error)
            return PracticeSnapshot()
        }
        val sorted = snapshot.copy(history = snapshot.history.sortedWith(
            compareByDescending<PracticeResult> { it.finishedAt }.thenByDescending { it.id }))
        return if (migrated) save(sorted) else sorted
    }

    fun add(result: PracticeResult): List<PracticeResult> {
        val snapshot = loadSnapshot()
        val existing = snapshot.history.find { it.id == result.id }
        if (existing == null && result.id in snapshot.claimedResultIds) return snapshot.history
        return save(withResult(snapshot, existing?.takeIf { it.prize != null || it.pokemonRewards.isNotEmpty() } ?: result)).history
    }

    /** Allocate the ID and save in the same caller-held lock, including after a clock rollback. */
    fun addNewResult(result: PracticeResult): SavedPractice {
        val snapshot = loadSnapshot()
        val largestId = maxOf(snapshot.lastResultId, snapshot.history.maxOfOrNull { it.id } ?: 0L,
            snapshot.claimedResultIds.maxOrNull() ?: 0L)
        val savedResult = result.copy(id = maxOf(result.id, Math.addExact(largestId, 1)))
        return SavedPractice(savedResult, save(withResult(snapshot, savedResult).copy(lastResultId = savedResult.id)))
    }

    // Retention follows monotonic IDs so a clock rollback cannot evict the result just saved.
    private fun withResult(snapshot: PracticeSnapshot, result: PracticeResult) = snapshot.copy(
        history = (listOf(result) + snapshot.history.filter { it.id != result.id })
            .sortedByDescending { it.id }
            .take(MAX_HISTORY_RESULTS)
            .sortedWith(compareByDescending<PracticeResult> { it.finishedAt }.thenByDescending { it.id })
    )

    fun claimReward(id: Long): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val result = snapshot.history.find { it.id == id } ?: return snapshot
        if (result.gameMode != GameMode.REWARDS) return snapshot
        // Keep the awarded type even if its tier assignment changed after this game.
        val type = result.prizeType ?: return snapshot
        val matrixTier = result.matrixRewardTier.takeIf { result.isMultiplicationMatrix }
        if (result.prize != null || id in snapshot.claimedResultIds ||
            !qualifiesForReward(result.questionCount, result.correct, result.attempts.size, result.timedOut,
                result.rewardThresholds, matrixTier)) return snapshot
        val balance = (snapshot.rewards[type] ?: RewardBalance()).addFragment()
        val claimed = result.copy(prize = PrizeAward(type, balance))
        return save(snapshot.copy(
            history = snapshot.history.map { if (it.id == id) claimed else it },
            rewards = snapshot.rewards + (type to balance),
            claimedResultIds = snapshot.claimedResultIds + id
        ))
    }

    fun addReadingResult(result: ReadingRewardResult): PracticeSnapshot {
        val snapshot = loadSnapshot()
        if (result.sessionId in snapshot.recordedReadingSessionIds ||
            snapshot.readingResults.any { it.sessionId == result.sessionId }) return snapshot
        require(result.sessionId.isNotBlank() && result.questionCount > 0 &&
            result.answered in 0..result.questionCount && result.correct in 0..result.answered)
        require(result.prize == null && result.pokemonReward == null)
        val eligible = result.rewardsEnabledAtStart &&
            qualifiesForReward(result.questionCount, result.correct, result.answered, result.timedOut, result.rewardThresholds)
        val saved = result.copy(
            prizeType = result.prizeType?.takeIf {
                eligible && it.tier == result.rewardThresholds.tierForQuestionCount(result.questionCount)
            },
            celebration = result.celebration?.takeIf {
                result.completed && it.isEligibleFor(result.questionCount)
            }
        )
        return save(snapshot.copy(
            // Reading sessions are not capped; keep their reward ownership until explicitly deleted.
            readingResults = listOf(saved) + snapshot.readingResults,
            // Retain receipts after deletion so old sessions cannot grant another reward.
            recordedReadingSessionIds = snapshot.recordedReadingSessionIds + result.sessionId
        ))
    }

    fun claimReadingReward(sessionId: String): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val result = snapshot.readingResults.find { it.sessionId == sessionId } ?: return snapshot
        val type = result.prizeType ?: return snapshot
        if (result.prize != null || !result.rewardsEnabledAtStart ||
            type.tier != result.rewardThresholds.tierForQuestionCount(result.questionCount) ||
            !qualifiesForReward(result.questionCount, result.correct, result.answered, result.timedOut,
                result.rewardThresholds)) return snapshot
        val balance = (snapshot.rewards[type] ?: RewardBalance()).addFragment()
        return save(snapshot.copy(
            readingResults = snapshot.readingResults.map {
                if (it.sessionId == sessionId) it.copy(prize = PrizeAward(type, balance)) else it
            },
            rewards = snapshot.rewards + (type to balance)
        ))
    }

    fun collectReadingCelebration(sessionId: String, celebration: Celebration): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val result = snapshot.readingResults.find { it.sessionId == sessionId } ?: return snapshot
        if (!result.completed || result.pokemonReward != null || result.celebration != celebration ||
            !celebration.isEligibleFor(result.questionCount)) return snapshot
        return save(snapshot.copy(
            readingResults = snapshot.readingResults.map {
                if (it.sessionId == sessionId) it.copy(pokemonReward = celebration) else it
            },
            pokemons = snapshot.pokemons + celebration
        ))
    }

    fun useReward(type: RewardType): PracticeSnapshot = removeReward(type, fragment = false)

    fun addReward(type: RewardType, fragment: Boolean): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val balance = snapshot.rewards[type] ?: RewardBalance()
        val added = if (fragment) balance.addFragment() else balance.addWhole()
        return save(snapshot.copy(
            rewards = snapshot.rewards + (type to added)
        ))
    }

    fun removeReward(type: RewardType, fragment: Boolean): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val balance = snapshot.rewards[type] ?: return snapshot
        val remaining = if (fragment) {
            if (balance.fragments == 0) return snapshot
            balance.copy(fragments = balance.fragments - 1)
        } else {
            if (balance.whole == 0) return snapshot
            balance.copy(whole = balance.whole - 1)
        }
        return save(snapshot.copy(
            rewards = snapshot.rewards + (type to remaining)
        ))
    }

    fun collectPokemon(celebration: Celebration): PracticeSnapshot = collectPokemons(listOf(celebration))

    fun collectPokemon(id: Long, celebration: Celebration): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val result = snapshot.history.find { it.id == id } ?: return snapshot
        val mega = celebration.category == CelebrationCategory.MEGA_POKEMONS
        if (result.pokemonRewards.any { (it.category == CelebrationCategory.MEGA_POKEMONS) == mega }) return snapshot
        if (result.gameMode == GameMode.PRACTICE && celebration.category != CelebrationCategory.OTHER) return snapshot
        if (mega && !celebration.isEligibleFor(result.questionCount, result.gameMode, result.isMultiplicationMatrix)) return snapshot
        if ((mega || result.isMultiplicationMatrix) &&
            (result.timedOut || result.attempts.size != result.questionCount)) return snapshot
        val collected = if (mega) result.copy(megaPokemonReward = celebration)
            else result.copy(pokemonReward = celebration,
                megaPokemonReward = result.megaPokemonReward ?:
                    result.pokemonReward?.takeIf { it.category == CelebrationCategory.MEGA_POKEMONS })
        return save(snapshot.copy(
            history = snapshot.history.map { if (it.id == id) collected else it },
            pokemons = snapshot.pokemons + celebration
        ))
    }

    fun collectPokemons(celebrations: Collection<Celebration>, includeMegaPokemons: Boolean = false): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val collected = snapshot.pokemons + celebrations.filter {
            includeMegaPokemons || it.category != CelebrationCategory.MEGA_POKEMONS
        }
        if (collected == snapshot.pokemons) return snapshot
        return save(snapshot.copy(pokemons = collected))
    }

    fun delete(id: Long): List<PracticeResult> {
        val snapshot = loadSnapshot()
        val result = snapshot.history.find { it.id == id } ?: return snapshot.history
        return save(withoutResults(snapshot, listOf(result))).history
    }

    fun clear(): List<PracticeResult> {
        val snapshot = loadSnapshot()
        if (snapshot.rewards.isNotEmpty() || snapshot.claimedResultIds.isNotEmpty() ||
            snapshot.lastResultId != 0L || snapshot.pokemons.isNotEmpty() ||
            snapshot.readingResults.isNotEmpty() || snapshot.recordedReadingSessionIds.isNotEmpty()) {
            save(withoutResults(snapshot, snapshot.history))
        } else if (file.exists() && !file.delete()) throw IOException("Unable to clear practice history.")
        return emptyList()
    }

    fun deleteReadingResults(sessionIds: Set<String>): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val removed = snapshot.readingResults.filter { it.sessionId in sessionIds }
        if (removed.isEmpty() && snapshot.recordedReadingSessionIds.containsAll(sessionIds)) return snapshot
        val fragments = removed.mapNotNull { it.prize?.type }.groupingBy { it }.eachCount()
        return save(withoutReadingResults(snapshot, sessionIds, fragments, removed.mapNotNull { it.pokemonReward }.toSet()))
    }

    private fun withoutReadingResults(
        snapshot: PracticeSnapshot, sessionIds: Set<String>, fragments: Map<RewardType, Int>,
        collected: Set<Celebration>
    ): PracticeSnapshot {
        val remaining = snapshot.readingResults.filterNot { it.sessionId in sessionIds }
        val pokemonsToRemove = collected -
            remaining.mapNotNull { it.pokemonReward }.toSet() -
            snapshot.history.flatMap { it.pokemonRewards }.toSet()
        return snapshot.copy(
            readingResults = remaining,
            recordedReadingSessionIds = snapshot.recordedReadingSessionIds + sessionIds,
            rewards = snapshot.rewards.mapValues { (type, balance) -> balance.removeFragments(fragments[type] ?: 0) },
            pokemons = snapshot.pokemons - pokemonsToRemove
        )
    }

    private fun withoutResults(snapshot: PracticeSnapshot, removed: List<PracticeResult>): PracticeSnapshot {
        val ids = removed.map { it.id }.toSet()
        val fragments = removed.mapNotNull { it.prize?.type }.groupingBy { it }.eachCount()
        val remaining = snapshot.history.filterNot { it.id in ids }
        val pokemonsToRemove = removed.flatMap { it.pokemonRewards }.toSet() -
            remaining.flatMap { it.pokemonRewards }.toSet() -
            snapshot.readingResults.mapNotNull { it.pokemonReward }.toSet()
        // Keep claimed IDs to prevent a stale save from restoring a deleted prize.
        return snapshot.copy(
            history = remaining,
            rewards = snapshot.rewards.mapValues { (type, balance) -> balance.removeFragments(fragments[type] ?: 0) },
            pokemons = snapshot.pokemons - pokemonsToRemove
        )
    }

    private fun save(snapshot: PracticeSnapshot): PracticeSnapshot {
        val directory = file.absoluteFile.parentFile ?: throw IOException("Invalid history location.")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Unable to create history folder.")
        val temporary = File(directory, "${file.name}.tmp")
        try {
            temporary.writeText(json.encodeToString(PracticeSnapshot.serializer(), snapshot), Charsets.UTF_8)
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            if (temporary.isFile) temporary.delete()
        }
        return snapshot
    }
}
