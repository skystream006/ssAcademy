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
import kotlinx.serialization.json.JsonObject

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
    val gameMode: GameMode = GameMode.REWARDS
) {
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
    val pokemonReward: Celebration? = null
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
        val snapshot = try {
            val element = json.parseToJsonElement(text)
            if (element is JsonArray) PracticeSnapshot(history = json.decodeFromJsonElement(serializer, element))
            else {
                // A damaged inventory must never be treated as an empty legacy history.
                require(element is JsonObject && "history" in element && "rewards" in element)
                json.decodeFromJsonElement(PracticeSnapshot.serializer(), element)
            }
        } catch (error: Exception) {
            if (text.trimStart().startsWith("{")) throw IOException("Unable to read rewards and practice history.", error)
            return PracticeSnapshot()
        }
        return snapshot.copy(history = snapshot.history.sortedWith(
            compareByDescending<PracticeResult> { it.finishedAt }.thenByDescending { it.id }))
    }

    fun add(result: PracticeResult): List<PracticeResult> {
        val snapshot = loadSnapshot()
        val existing = snapshot.history.find { it.id == result.id }
        if (existing == null && result.id in snapshot.claimedResultIds) return snapshot.history
        return save(withResult(snapshot, existing?.takeIf { it.prize != null || it.pokemonReward != null } ?: result)).history
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
        val type = result.prizeType ?: return snapshot
        if (result.prize != null || id in snapshot.claimedResultIds ||
            !qualifiesForReward(result.questionCount, result.correct, result.attempts.size, result.timedOut)) return snapshot
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
            qualifiesForReward(result.questionCount, result.correct, result.answered, result.timedOut)
        val saved = result.copy(
            prizeType = result.prizeType?.takeIf { eligible && it.tier == rewardTierForQuestionCount(result.questionCount) },
            celebration = result.celebration?.takeIf {
                result.completed && (it.category == CelebrationCategory.OTHER || result.questionCount >= 15)
            }
        )
        return save(snapshot.copy(
            readingResults = (listOf(saved) + snapshot.readingResults).take(MAX_HISTORY_RESULTS),
            // Retain receipts even after result retention, so old sessions cannot grant another reward.
            recordedReadingSessionIds = snapshot.recordedReadingSessionIds + result.sessionId
        ))
    }

    fun claimReadingReward(sessionId: String): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val result = snapshot.readingResults.find { it.sessionId == sessionId } ?: return snapshot
        val type = result.prizeType ?: return snapshot
        if (result.prize != null || !result.rewardsEnabledAtStart ||
            type.tier != rewardTierForQuestionCount(result.questionCount) ||
            !qualifiesForReward(result.questionCount, result.correct, result.answered, result.timedOut)) return snapshot
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
        if (!result.completed || result.pokemonReward != null || result.celebration != celebration) return snapshot
        return save(snapshot.copy(
            readingResults = snapshot.readingResults.map {
                if (it.sessionId == sessionId) it.copy(pokemonReward = celebration) else it
            },
            pokemons = snapshot.pokemons + celebration
        ))
    }

    fun useReward(type: RewardType): PracticeSnapshot = removeReward(type, fragment = false)

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
        if (result.pokemonReward != null) return snapshot
        if (result.gameMode == GameMode.PRACTICE && celebration.category != CelebrationCategory.OTHER) return snapshot
        return save(snapshot.copy(
            history = snapshot.history.map { if (it.id == id) it.copy(pokemonReward = celebration) else it },
            pokemons = snapshot.pokemons + celebration
        ))
    }

    fun collectPokemons(celebrations: Collection<Celebration>): PracticeSnapshot {
        val snapshot = loadSnapshot()
        val collected = snapshot.pokemons + celebrations
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
        val remaining = snapshot.readingResults.filterNot { it.sessionId in sessionIds }
        val fragments = removed.mapNotNull { it.prize?.type }.groupingBy { it }.eachCount()
        val pokemonsToRemove = removed.mapNotNull { it.pokemonReward }.toSet() -
            remaining.mapNotNull { it.pokemonReward }.toSet() -
            snapshot.history.mapNotNull { it.pokemonReward }.toSet()
        return save(snapshot.copy(
            readingResults = remaining,
            recordedReadingSessionIds = snapshot.recordedReadingSessionIds + sessionIds,
            rewards = snapshot.rewards.mapValues { (type, balance) -> balance.removeFragments(fragments[type] ?: 0) },
            pokemons = snapshot.pokemons - pokemonsToRemove
        ))
    }

    private fun withoutResults(snapshot: PracticeSnapshot, removed: List<PracticeResult>): PracticeSnapshot {
        val ids = removed.map { it.id }.toSet()
        val fragments = removed.mapNotNull { it.prize?.type }.groupingBy { it }.eachCount()
        val remaining = snapshot.history.filterNot { it.id in ids }
        val pokemonsToRemove = removed.mapNotNull { it.pokemonReward }.toSet() -
            remaining.mapNotNull { it.pokemonReward }.toSet() -
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
