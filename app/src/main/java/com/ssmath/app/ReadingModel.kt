package com.ssmath.app

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
enum class ReadingPage { MENU, ACTIVITY, PROFILES, PROGRESS }

@Serializable
data class ReadingProfile(
    val id: String,
    val name: String,
    val level: ReadingLevel = ReadingLevel.SPROUT,
    val activeAttemptId: String? = null
)

@Serializable
data class ReadingCheckpoint(
    val tries: Int = 0,
    val firstCorrect: Boolean = false,
    val firstSupported: Boolean = false,
    val firstAt: Long = 0,
    val earnedAt: Long? = null,
    val supportedAtCompletion: Boolean = false,
    val readerId: String? = null,
    val shared: Boolean = false,
    val objective: Boolean = true,
    val rewardGranted: Boolean = true
) {
    val earned: Boolean get() = earnedAt != null
    val awardedPoint: Boolean get() = earned && rewardGranted
    val independent: Boolean get() = earned && firstCorrect && !firstSupported
    val retried: Boolean get() = earned && tries > 1
}

@Serializable
data class ReadingTurn(val readerId: String, val text: String)

@Serializable
data class ReadingAttempt(
    val id: String,
    val ownerId: String,
    val contentId: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val abandoned: Boolean = false,
    val fields: Map<String, String> = emptyMap(),
    val order: List<Int> = emptyList(),
    val branch: Int? = null,
    val partnerId: String? = null,
    val turns: List<ReadingTurn> = emptyList(),
    val reread: Boolean = false,
    val hints: Int = 0,
    val narrations: Int = 0,
    val checkpoints: Map<String, ReadingCheckpoint> = emptyMap(),
    val feedback: String = ""
) {
    val content: ReadingActivity get() = ReadingContent.find(contentId)
    val open: Boolean get() = finishedAt == null && !abandoned
    val supported: Boolean get() = hints > 0 || narrations > 0
    fun earned(key: String) = checkpoints[key]?.earned == true
    fun includes(readerId: String) = ownerId == readerId || partnerId == readerId
}

@Serializable
data class ReadingStory(
    val attemptId: String,
    val title: String,
    val text: String,
    val illustration: String,
    val readerIds: List<String>,
    val turns: List<ReadingTurn> = emptyList(),
    val savedAt: Long
)

@Serializable
data class ReadingData(
    val version: Int = 1,
    val profiles: List<ReadingProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val page: ReadingPage = ReadingPage.MENU,
    val attempts: List<ReadingAttempt> = emptyList(),
    val stories: List<ReadingStory> = emptyList()
) {
    val profile: ReadingProfile? get() = profiles.find { it.id == selectedProfileId }
    val active: ReadingAttempt? get() = attempts.find { it.id == profile?.activeAttemptId }
    fun name(id: String) = profiles.first { it.id == id }.name
    fun attemptsFor(id: String) = attempts.filter { it.includes(id) }
    fun points(id: String) = attempts.sumOf { a ->
        a.checkpoints.values.count { it.awardedPoint && !it.shared && it.readerId == id }
    }
    fun sharedPoints(id: String) = attemptsFor(id).count { it.checkpoints["shared"]?.awardedPoint == true }
    fun sharedCompletions(id: String) = attemptsFor(id).count { it.earned("shared") }
    fun completions(id: String) = attemptsFor(id).count { it.finishedAt != null }
    fun milestones(id: String): List<String> = buildList {
        if (completions(id) >= 1) add("First page · finished a reading activity")
        if (completions(id) >= 5) add("Book explorer · finished five activities")
        if (attemptsFor(id).filter { it.finishedAt != null }.map { it.content.game }.distinct().size >= 3)
            add("Curious reader · explored three games")
        if (stories.any { id in it.readerIds }) add("Story maker · saved an original story")
        if (sharedCompletions(id) > 0) add("Reading together · completed a shared story")
    }
}

data class ReadingAccuracy(
    val firstIndependent: Int,
    val attempted: Int,
    val supportedCompletions: Int,
    val retriedCompletions: Int
)

fun ReadingData.accuracy(id: String, game: ReadingGame? = null, level: ReadingLevel? = null): ReadingAccuracy {
    val checks = attemptsFor(id).filter {
        (game == null || it.content.game == game) && (level == null || it.content.level == level)
    }.flatMap { it.checkpoints.values }.filter { it.readerId == id && it.objective && it.tries > 0 }
    return ReadingAccuracy(checks.count { it.independent }, checks.size,
        checks.count { it.earned && it.supportedAtCompletion }, checks.count { it.retried })
}

/** All credit is derived from this ledger, never incremented in a UI callback or duplicated on resume. */
object ReadingEngine {
    val studioKeys = listOf("character", "setting", "problem", "ending")

    fun addProfile(data: ReadingData, rawName: String, id: String = UUID.randomUUID().toString()): ReadingData {
        val name = rawName.trim()
        require(name.length in 1..40 && name.none { it.isISOControl() }) { "Use a name with 1–40 letters or symbols." }
        require(data.profiles.none { it.name.equals(name, ignoreCase = true) }) { "Choose a different reader name." }
        require(data.profiles.none { it.id == id }) { "That reader already exists." }
        return data.copy(profiles = data.profiles + ReadingProfile(id, name), selectedProfileId = id,
            page = ReadingPage.MENU)
    }

    fun selectProfile(data: ReadingData, id: String): ReadingData {
        require(data.profiles.any { it.id == id })
        return data.copy(selectedProfileId = id, page = ReadingPage.MENU)
    }

    fun renameProfile(data: ReadingData, rawName: String): ReadingData {
        val profile = requireNotNull(data.profile) { "Enter your name to get started." }
        val name = rawName.trim()
        require(name.length in 1..40 && name.none { it.isISOControl() }) { "Use a name with 1–40 letters or symbols." }
        require(data.profiles.none { it.id != profile.id && it.name.equals(name, ignoreCase = true) }) {
            "Choose a different reader name."
        }
        return data.copy(profiles = data.profiles.map {
            if (it.id == profile.id) it.copy(name = name) else it
        })
    }

    fun level(data: ReadingData, level: ReadingLevel) = data.copy(profiles = data.profiles.map {
        if (it.id == data.selectedProfileId) it.copy(level = level) else it
    })

    fun draft(data: ReadingData, game: ReadingGame, level: ReadingLevel): ReadingAttempt? =
        data.attempts.lastOrNull { it.includes(data.selectedProfileId ?: "") && it.open &&
            it.content.game == game && it.content.level == level }

    fun start(data: ReadingData, game: ReadingGame, level: ReadingLevel, newAttempt: Boolean = false,
              id: String = UUID.randomUUID().toString(), now: Long = System.currentTimeMillis()): ReadingData {
        val owner = requireNotNull(data.profile)
        val existing = draft(data, game, level)
        if (existing != null && !newAttempt) return resume(data, existing.id)
        require(data.attempts.none { it.id == id })
        val previous = data.attempts.count { it.ownerId == owner.id && it.content.game == game && it.content.level == level }
        val choices = ReadingContent.forGame(game, level)
        val attempt = ReadingAttempt(id, owner.id, choices[previous % choices.size].id, now)
        return data.copy(attempts = data.attempts.map {
            if (newAttempt && it.id == existing?.id) it.copy(abandoned = true) else it
        } + attempt, profiles = data.profiles.map {
            if (it.id == owner.id) it.copy(activeAttemptId = id) else it
        }, page = ReadingPage.ACTIVITY)
    }

    fun resume(data: ReadingData, id: String): ReadingData {
        val attempt = data.attempts.first { it.id == id }
        require(attempt.includes(requireNotNull(data.selectedProfileId)) && !attempt.abandoned)
        return data.copy(page = ReadingPage.ACTIVITY, profiles = data.profiles.map {
            if (it.id == data.selectedProfileId) it.copy(activeAttemptId = id) else it
        })
    }

    private fun edit(data: ReadingData, block: (ReadingAttempt) -> ReadingAttempt): ReadingData {
        val attempt = data.active ?: return data
        if (!attempt.open || !attempt.includes(data.selectedProfileId ?: "")) return data
        val changed = block(attempt)
        return if (changed == attempt) data else data.copy(attempts = data.attempts.map {
            if (it.id == attempt.id) changed else it
        })
    }

    fun field(data: ReadingData, key: String, value: String, expectedTurn: Int? = null): ReadingData = edit(data) { a ->
        val permitted = when (a.content.game) {
            ReadingGame.STUDIO -> key in studioKeys
            ReadingGame.PARTNERS -> key == "turn" && a.turns.size < 4 && expectedTurn == a.turns.size
            else -> key == "answer" || (key == "evidence" && a.content.game == ReadingGame.DETECTIVE)
        }
        if (!permitted || a.earned(key) || (key == "answer" && a.earned("bonus"))) a
        else a.copy(fields = a.fields + (key to value.take(500)), feedback = "")
    }

    fun placeTile(data: ReadingData, index: Int): ReadingData = edit(data) { a ->
        val size = if (a.content.game == ReadingGame.PARTNERS) a.turns.size else a.content.tiles.size
        if (a.earned("order") || a.earned("shared") || index !in 0 until size || index in a.order) a
        else a.copy(order = a.order + index, reread = false, feedback = "")
    }

    fun removeTile(data: ReadingData, tileId: Int): ReadingData = edit(data) { a ->
        if (a.earned("order") || a.earned("shared") || tileId !in a.order) a
        else a.copy(order = a.order.filterNot { it == tileId }, reread = false, feedback = "")
    }

    fun hint(data: ReadingData): ReadingData = edit(data) { it.copy(hints = it.hints + 1, feedback = it.content.hint) }

    fun narration(data: ReadingData, queued: Boolean): ReadingData =
        if (!queued) data else edit(data) { it.copy(narrations = it.narrations + 1) }

    fun branch(data: ReadingData, index: Int): ReadingData = edit(data) { a ->
        if (a.content.game != ReadingGame.CHAPTER || a.branch != null || index !in a.content.branches.indices) a
        else a.copy(branch = index, feedback = "")
    }

    fun partner(data: ReadingData, id: String): ReadingData = edit(data) { a ->
        if (a.content.game != ReadingGame.PARTNERS || a.partnerId != null || a.turns.isNotEmpty()) a
        else {
            require(id != a.ownerId && data.profiles.any { it.id == id }) { "Choose two different existing readers." }
            a.copy(partnerId = id, feedback = "")
        }
    }

    fun reread(data: ReadingData): ReadingData = edit(data) { a ->
        if (a.content.game == ReadingGame.PARTNERS && a.turns.size == 4 && a.order.size == 4)
            a.copy(reread = true) else a
    }

    fun question(a: ReadingAttempt): ReadingQuestion? =
        if (a.content.game == ReadingGame.CHAPTER) a.branch?.let { a.content.branches[it].question }
        else a.content.question

    fun requiredKeys(a: ReadingAttempt): List<String> = when (a.content.game) {
        ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.CHAPTER -> listOf("answer")
        ReadingGame.SCRAMBLE -> listOf("order")
        ReadingGame.SEQUENCE -> listOf("order", "answer")
        ReadingGame.DETECTIVE -> listOf("answer", "evidence")
        ReadingGame.STUDIO -> studioKeys
        ReadingGame.PARTNERS -> listOf("shared")
    }

    fun ready(a: ReadingAttempt) = requiredKeys(a).all { a.earned(it) }

    private fun award(a: ReadingAttempt, key: String, correct: Boolean, now: Long, rewardsEnabled: Boolean,
                      readerId: String? = a.ownerId, shared: Boolean = false, objective: Boolean = true): ReadingAttempt {
        val old = a.checkpoints[key] ?: ReadingCheckpoint(readerId = readerId, shared = shared, objective = objective)
        if (old.earned) return a
        val check = old.copy(tries = old.tries + 1,
            firstCorrect = if (old.tries == 0) correct else old.firstCorrect,
            firstSupported = if (old.tries == 0) a.supported else old.firstSupported,
            firstAt = if (old.tries == 0) now else old.firstAt,
            earnedAt = if (correct) now else null,
            supportedAtCompletion = correct && a.supported,
            rewardGranted = correct && rewardsEnabled)
        return a.copy(checkpoints = a.checkpoints + (key to check), feedback = if (correct)
            if (!rewardsEnabled) "Checkpoint saved · rewards are off."
            else if (shared) "Your shared story earned 1 shared completion point!" else "Checkpoint saved · +1 point!"
        else "Not quite yet. Reread, try a hint, and try again. Your points stay safe.")
    }

    fun submit(data: ReadingData, key: String, now: Long = System.currentTimeMillis(),
               expectedTurn: Int? = null, rewardsEnabled: Boolean = true): ReadingData = edit(data) { a ->
        if (a.earned(key)) return@edit a
        val game = a.content.game
        when {
            key == "order" && game in listOf(ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE) -> {
                if (a.order.size != a.content.tiles.size) a.copy(feedback = "Place every tile before checking.")
                else award(a, key, a.order in a.content.validOrders, now, rewardsEnabled)
            }
            (key == "answer" && game in listOf(ReadingGame.MISSING, ReadingGame.SEQUENCE, ReadingGame.DETECTIVE,
                ReadingGame.CAPTION, ReadingGame.CHAPTER)) || (key == "bonus" && game == ReadingGame.SCRAMBLE) -> {
                val question = question(a)
                val answer = a.fields["answer"]?.toIntOrNull()
                if ((game == ReadingGame.SEQUENCE || game == ReadingGame.SCRAMBLE) && !a.earned("order")) a
                else if (question == null || answer == null || answer !in question.choices.indices)
                    a.copy(feedback = "Choose an answer before checking.")
                else award(a, key, answer == question.answer, now, rewardsEnabled)
            }
            key == "evidence" && game == ReadingGame.DETECTIVE && a.earned("answer") -> {
                val index = a.fields["evidence"]?.toIntOrNull()
                if (index == null || index !in a.content.passage.indices) a.copy(feedback = "Tap a sentence in the passage.")
                else award(a, key, index == a.content.evidence, now, rewardsEnabled)
            }
            key in studioKeys && game == ReadingGame.STUDIO -> {
                val before = studioKeys.takeWhile { it != key }
                if (!before.all { a.earned(it) }) a
                else if (a.fields[key].isNullOrBlank()) a.copy(feedback = "Choose a story tile or type your own idea.")
                else award(a.copy(fields = a.fields + (key to a.fields.getValue(key).trim())), key, true, now,
                    rewardsEnabled, objective = false)
            }
            key == "turn" && game == ReadingGame.PARTNERS &&
                a.turns.size < 4 && expectedTurn == a.turns.size -> {
                val text = a.fields["turn"]?.trim().orEmpty()
                if (text.isEmpty()) a.copy(feedback = "Pick a sentence idea or type your own sentence.")
                else {
                    val author = if (a.turns.size % 2 == 0) a.ownerId else a.partnerId ?: a.ownerId
                    val next = a.copy(turns = a.turns + ReadingTurn(author, text), fields = a.fields - "turn",
                        feedback = "Sentence saved. Pass the story to the next reader.")
                    award(next, "participation:$author", true, now, rewardsEnabled, readerId = author, objective = false)
                }
            }
            key == "shared" && game == ReadingGame.PARTNERS && a.turns.size == 4 &&
                a.order.sorted() == listOf(0, 1, 2, 3) && a.reread ->
                award(a, key, true, now, rewardsEnabled, readerId = null, shared = true, objective = false)
            else -> a
        }
    }

    fun storyText(a: ReadingAttempt): String = if (a.content.game == ReadingGame.PARTNERS)
        a.order.joinToString(" ") { a.turns[it].text }
    else "${a.fields["character"].orEmpty()} lived in ${a.fields["setting"].orEmpty()}. " +
        "One day, this character ${a.fields["problem"].orEmpty()}. " +
        "In the end, this character ${a.fields["ending"].orEmpty()}."

    fun finish(data: ReadingData, now: Long = System.currentTimeMillis()): ReadingData {
        val a = data.active ?: return data
        if (!a.open || !ready(a)) return data
        val updated = edit(data) { it.copy(finishedAt = now, feedback = "Congratulations! Your reading is growing.") }
        if (a.content.game !in listOf(ReadingGame.STUDIO, ReadingGame.PARTNERS)) return updated
        val readers = listOfNotNull(a.ownerId, a.partnerId)
        val story = ReadingStory(a.id, a.content.title, storyText(a), a.content.picture.symbol, readers,
            if (a.content.game == ReadingGame.PARTNERS) a.order.map { a.turns[it] } else emptyList(), now)
        return updated.copy(stories = updated.stories.filterNot { it.attemptId == a.id } + story)
    }

    fun validate(data: ReadingData) {
        require(data.version == 1) { "Unsupported reading data version." }
        val profileIds = data.profiles.map { it.id }
        require(profileIds.distinct().size == profileIds.size && profileIds.none { it.isBlank() })
        require(data.profiles.all { it.name.isNotBlank() && it.name.length <= 40 })
        require(data.selectedProfileId == null || data.selectedProfileId in profileIds)
        require(data.profiles.isEmpty() == (data.selectedProfileId == null))
        require(data.attempts.map { it.id }.distinct().size == data.attempts.size)
        data.attempts.forEach { a ->
            require(a.id.isNotBlank() && a.ownerId in profileIds && a.hints >= 0 && a.narrations >= 0)
            require(a.partnerId == null || (a.content.game == ReadingGame.PARTNERS &&
                a.partnerId in profileIds && a.partnerId != a.ownerId))
            require(a.branch == null || a.branch in a.content.branches.indices)
            val size = if (a.content.game == ReadingGame.PARTNERS) a.turns.size else a.content.tiles.size
            require(a.order.distinct().size == a.order.size && a.order.all { it in 0 until size })
            require(a.turns.size <= 4 && a.turns.withIndex().all { (i, turn) ->
                turn.text.isNotBlank() && turn.readerId == if (i % 2 == 0) a.ownerId else a.partnerId ?: a.ownerId
            })
            require(a.checkpoints.all { (key, c) ->
                val validKeys = requiredKeys(a) + if (a.content.game == ReadingGame.SCRAMBLE) listOf("bonus") else emptyList()
                val participation = a.content.game == ReadingGame.PARTNERS &&
                    key in listOfNotNull("participation:${a.ownerId}", a.partnerId?.let { "participation:$it" })
                val expectedReader = if (participation) key.removePrefix("participation:") else a.ownerId
                (key in validKeys || participation) && c.tries > 0 &&
                    c.shared == (key == "shared") &&
                    c.readerId == (if (c.shared) null else expectedReader) &&
                    c.objective == (a.content.game !in listOf(ReadingGame.STUDIO, ReadingGame.PARTNERS)) &&
                    (!c.firstCorrect || (c.earned && c.tries == 1))
            })
            require(a.finishedAt == null || ready(a))
        }
        data.profiles.forEach { profile ->
            require(profile.activeAttemptId == null || data.attempts.any {
                it.id == profile.activeAttemptId && it.includes(profile.id)
            })
        }
        require(data.stories.map { it.attemptId }.distinct().size == data.stories.size)
        data.stories.forEach { story ->
            val attempt = data.attempts.first { it.id == story.attemptId }
            require(attempt.finishedAt != null && story.readerIds == listOfNotNull(attempt.ownerId, attempt.partnerId))
            require(story.text == storyText(attempt))
        }
    }
}
