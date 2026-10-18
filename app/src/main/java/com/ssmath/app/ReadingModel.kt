package com.ssmath.app

import java.util.UUID
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val objective: Boolean = true,
    @Deprecated("Legacy checkpoint rewards are not awarded.")
    val rewardGranted: Boolean = false
) {
    val earned: Boolean get() = earnedAt != null
    val independent: Boolean get() = earned && firstCorrect && !firstSupported
    val retried: Boolean get() = earned && tries > 1
}

@Serializable
data class ReadingActivityRef(
    val revision: String,
    val game: ReadingGame,
    val level: ReadingLevel,
    val title: String
)

@Serializable
data class ReadingAttempt(
    val id: String,
    val ownerId: String,
    val contentId: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val abandoned: Boolean = false,
    val fields: Map<String, String> = emptyMap(),
    // Sentence Scramble uses -1 for empty spaces; missing trailing entries are also empty.
    val order: List<Int> = emptyList(),
    val hints: Int = 0,
    val narrations: Int = 0,
    val checkpoints: Map<String, ReadingCheckpoint> = emptyMap(),
    val feedback: String = "",
    val activityRef: ReadingActivityRef? = null,
    @Transient val loadedContent: ReadingActivity? = null
) {
    val content: ReadingActivity get() = loadedContent ?: if (activityRef == null) ReadingContent.find(contentId)
        else error("Activity content must be loaded before opening an attempt.")
    val game: ReadingGame get() = activityRef?.game ?: content.game
    val level: ReadingLevel get() = activityRef?.level ?: content.level
    val title: String get() = activityRef?.title ?: content.title
    val open: Boolean get() = finishedAt == null && !abandoned
    val supported: Boolean get() = hints > 0 || narrations > 0
    val orderComplete: Boolean get() = order.size == content.tiles.size && order.all { it in content.tiles.indices }
    fun earned(key: String) = checkpoints[key]?.earned == true
    fun includes(readerId: String) = ownerId == readerId
}

@Serializable
data class ReadingStory(
    val attemptId: String,
    val title: String,
    val text: String,
    val illustration: String,
    val readerIds: List<String>,
    val savedAt: Long
)

@Serializable
data class ReadingSession(
    val id: String,
    val ownerId: String,
    val game: ReadingGame,
    val level: ReadingLevel,
    val attemptIds: List<String>,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val abandoned: Boolean = false,
    val rewardsEnabledAtStart: Boolean,
    val prizeType: RewardType? = null,
    val timedOut: Boolean = false,
    val timeLimitMs: Long? = null,
    val elapsedMs: Long = 0
) {
    val questionCount: Int get() = attemptIds.size
}

@Serializable
data class ReadingData(
    val version: Int = 1,
    val profiles: List<ReadingProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val page: ReadingPage = ReadingPage.MENU,
    val attempts: List<ReadingAttempt> = emptyList(),
    val stories: List<ReadingStory> = emptyList(),
    val sessions: List<ReadingSession> = emptyList(),
    // Durable deletion receipts allow shared rewards to be cleaned up after a failed write or restart.
    val deletedSessionIds: Set<String> = emptySet()
) {
    val profile: ReadingProfile? get() = profiles.find { it.id == selectedProfileId }
    val active: ReadingAttempt? get() = attempts.find { it.id == profile?.activeAttemptId }
    val activeSession: ReadingSession? get() = sessions.find {
        it.ownerId == selectedProfileId && profile?.activeAttemptId in it.attemptIds
    }
    fun sessionAnswered(session: ReadingSession) =
        attempts.count { it.id in session.attemptIds && it.finishedAt != null }
    fun sessionWrong(session: ReadingSession) =
        attempts.count { it.id in session.attemptIds && it.finishedAt != null && ReadingEngine.wrong(it) }
    fun sessionCorrect(session: ReadingSession) = sessionAnswered(session) - sessionWrong(session)
    fun name(id: String) = profiles.first { it.id == id }.name
    fun attemptsFor(id: String) = attempts.filter { it.includes(id) }
    fun completions(id: String) = attemptsFor(id).count { it.finishedAt != null }
    fun milestones(id: String): List<String> = buildList {
        if (completions(id) >= 1) add("First page · finished a reading activity")
        if (completions(id) >= 5) add("Book explorer · finished five activities")
        if (attemptsFor(id).filter { it.finishedAt != null }.map { it.game }.distinct().size >= 3)
            add("Curious reader · explored three games")
        if (stories.any { id in it.readerIds }) add("Story maker · saved an original story")
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
        (game == null || it.game == game) && (level == null || it.level == level)
    }.flatMap { it.checkpoints.values }.filter { it.readerId == id && it.objective && it.tries > 0 }
    return ReadingAccuracy(checks.count { it.independent }, checks.size,
        checks.count { it.earned && it.supportedAtCompletion }, checks.count { it.retried })
}

/** Activity results are derived from the ledger and cannot be duplicated on resume. */
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

    fun deleteHistory(data: ReadingData, attemptIds: Set<String>): ReadingData {
        val sessions = data.sessions.filter { session -> session.attemptIds.any { it in attemptIds } }
        val removed = attemptIds + sessions.flatMap { it.attemptIds }
        if (data.attempts.none { it.id in removed }) return data
        val activeRemoved = data.profile?.activeAttemptId in removed
        return data.copy(
            attempts = data.attempts.filterNot { it.id in removed },
            sessions = data.sessions - sessions.toSet(),
            stories = data.stories.filterNot { it.attemptId in removed },
            profiles = data.profiles.map {
                if (it.activeAttemptId in removed) it.copy(activeAttemptId = null) else it
            },
            page = if (activeRemoved && data.page == ReadingPage.ACTIVITY) ReadingPage.MENU else data.page,
            deletedSessionIds = data.deletedSessionIds + sessions.map { it.id }
        )
    }

    fun clearHistory(data: ReadingData, readerId: String): ReadingData =
        deleteHistory(data, data.attemptsFor(readerId).map { it.id }.toSet())

    fun draft(data: ReadingData, game: ReadingGame, level: ReadingLevel): ReadingAttempt? {
        fun matches(attempt: ReadingAttempt) = attempt.includes(data.selectedProfileId ?: "") &&
            attempt.game == game && attempt.level == level &&
            data.sessions.none { attempt.id in it.attemptIds }
        data.active?.takeIf { it.open && matches(it) }?.let { return it }
        // A completed legacy batch must not resurrect an older, unused allocation as its draft.
        return data.attempts.lastOrNull(::matches)?.takeIf { it.open }
    }

    fun startSession(
        data: ReadingData, game: ReadingGame, level: ReadingLevel, count: Int,
        rewardsEnabledAtStart: Boolean = true, id: String = UUID.randomUUID().toString(),
        now: Long = System.currentTimeMillis(), timeLimitMs: Long? = null,
        assigned: List<ReadingAttempt>? = null
    ): ReadingData {
        require(count in listOf(15, 25, 50)) { "Choose 15, 25, or 50 questions." }
        val owner = requireNotNull(data.profile)
        require(data.sessions.none { it.id == id } && id !in data.deletedSessionIds && id.isNotBlank())
        require(timeLimitMs == null || (rewardsEnabledAtStart && timeLimitMs > 0))
        val attempts = assigned ?: run {
            val choices = ReadingContent.forGame(game, level)
            require(choices.isNotEmpty()) { "Import reading activities in Settings > Admin > Game Settings > Reading Settings." }
            val previous = data.attempts.count { it.ownerId == owner.id && it.game == game && it.level == level }
            List(count) { index ->
                ReadingAttempt(UUID.randomUUID().toString(), owner.id, choices[(previous + index) % choices.size].id, now)
            }
        }
        require(attempts.size == count && attempts.all {
            it.ownerId == owner.id && it.game == game && it.level == level && it.open && it.checkpoints.isEmpty()
        })
        require(attempts.map { it.id }.distinct().size == count && attempts.none { a -> data.attempts.any { it.id == a.id } })
        var next = data
        data.sessions.filter { it.ownerId == owner.id && it.finishedAt == null }.forEach {
            next = endSession(next, it.id, now, abandoned = true)
        }
        val session = ReadingSession(id, owner.id, game, level, attempts.map { it.id }, now,
            rewardsEnabledAtStart = rewardsEnabledAtStart, timeLimitMs = timeLimitMs)
        return next.copy(attempts = next.attempts + attempts, sessions = next.sessions + session,
            profiles = next.profiles.map {
                if (it.id == owner.id) it.copy(activeAttemptId = attempts.first().id) else it
            }, page = ReadingPage.ACTIVITY)
    }

    fun start(data: ReadingData, game: ReadingGame, level: ReadingLevel, newAttempt: Boolean = false,
              id: String = UUID.randomUUID().toString(), now: Long = System.currentTimeMillis(),
              selected: ReadingActivity? = null, activityRef: ReadingActivityRef? = null): ReadingData {
        val owner = requireNotNull(data.profile)
        val existing = draft(data, game, level)
        if (existing != null && !newAttempt) return resume(data, existing.id)
        require(data.attempts.none { it.id == id })
        val content = selected ?: run {
            val previous = data.attempts.count { it.ownerId == owner.id && it.game == game && it.level == level }
            val choices = ReadingContent.forGame(game, level)
            require(choices.isNotEmpty()) { "Import reading activities in Settings > Admin > Game Settings > Reading Settings." }
            choices[previous % choices.size]
        }
        require(content.game == game && content.level == level)
        val attempt = ReadingAttempt(id, owner.id, content.id, now, activityRef = activityRef,
            loadedContent = if (activityRef == null) null else content)
        return data.copy(attempts = data.attempts.map {
            if (newAttempt && it.id == existing?.id) it.copy(abandoned = true) else it
        } + attempt, profiles = data.profiles.map {
            if (it.id == owner.id) it.copy(activeAttemptId = id) else it
        }, page = ReadingPage.ACTIVITY)
    }

    fun resume(data: ReadingData, id: String): ReadingData {
        val attempt = data.attempts.find { it.id == id } ?: return data
        require(attempt.includes(requireNotNull(data.selectedProfileId)) && !attempt.abandoned)
        val session = data.sessions.find { id in it.attemptIds }
        val activeId = if (session != null && session.finishedAt == null) {
            require(session.ownerId == data.selectedProfileId)
            data.profile?.activeAttemptId?.takeIf { it in session.attemptIds }
                ?: session.attemptIds.first { member -> data.attempts.first { it.id == member }.open }
        } else id
        return data.copy(page = ReadingPage.ACTIVITY, profiles = data.profiles.map {
            if (it.id == data.selectedProfileId) it.copy(activeAttemptId = activeId) else it
        })
    }

    private fun edit(data: ReadingData, block: (ReadingAttempt) -> ReadingAttempt): ReadingData {
        val attempt = data.active ?: return data
        if (!attempt.open || !attempt.includes(data.selectedProfileId ?: "")) return data
        if (data.sessions.any { attempt.id in it.attemptIds &&
            (it.finishedAt != null || it.ownerId != data.selectedProfileId) }) return data
        val changed = block(attempt)
        return if (changed == attempt) data else data.copy(attempts = data.attempts.map {
            if (it.id == attempt.id) changed else it
        })
    }

    fun field(data: ReadingData, key: String, value: String): ReadingData = edit(data) { a ->
        val permitted = when (a.game) {
            ReadingGame.STUDIO -> key in studioKeys
            else -> key == "answer" || (key == "evidence" && a.game == ReadingGame.DETECTIVE)
        }
        if (!permitted || a.earned(key) || (key == "answer" && a.earned("bonus"))) a
        else a.copy(fields = a.fields + (key to value.take(500)), feedback = "")
    }

    fun placeTile(data: ReadingData, index: Int): ReadingData = edit(data) { a ->
        val size = a.content.tiles.size
        if (a.earned("order") || index !in 0 until size || index in a.order) a
        else if (a.game == ReadingGame.SCRAMBLE) {
            val position = a.order.indexOf(-1).takeIf { it >= 0 } ?: a.order.size
            placeSentenceTile(a, index, position)
        }
        else a.copy(order = a.order + index, feedback = "")
    }

    fun placeTileInSpace(data: ReadingData, tileId: Int, position: Int): ReadingData =
        edit(data) { placeSentenceTile(it, tileId, position) }

    private fun placeSentenceTile(a: ReadingAttempt, tileId: Int, position: Int): ReadingAttempt {
        if (a.game != ReadingGame.SCRAMBLE || a.earned("order") ||
            tileId !in a.content.tiles.indices || position !in a.content.tiles.indices ||
            (a.order.getOrNull(position) ?: -1) != -1) return a
        val order = MutableList(a.content.tiles.size) { a.order.getOrNull(it) ?: -1 }
        order.replaceAll { if (it == tileId) -1 else it }
        order[position] = tileId
        return a.copy(order = order.dropLastWhile { it == -1 }, feedback = "")
    }

    fun removeTile(data: ReadingData, tileId: Int): ReadingData = edit(data) { a ->
        if (a.earned("order") || tileId < 0 || tileId !in a.order) a
        else if (a.game == ReadingGame.SCRAMBLE)
            a.copy(order = a.order.map { if (it == tileId) -1 else it }.dropLastWhile { it == -1 },
                feedback = "")
        else a.copy(order = a.order.filterNot { it == tileId }, feedback = "")
    }

    fun hint(data: ReadingData): ReadingData = edit(data) { it.copy(hints = it.hints + 1, feedback = it.content.hint) }

    fun narration(data: ReadingData, queued: Boolean): ReadingData =
        if (!queued) data else edit(data) { it.copy(narrations = it.narrations + 1) }

    fun question(a: ReadingAttempt): ReadingQuestion? = a.content.question

    fun requiredKeys(a: ReadingAttempt): List<String> = when (a.game) {
        ReadingGame.MISSING, ReadingGame.GRAMMAR -> listOf("answer")
        ReadingGame.SCRAMBLE -> listOf("order")
        ReadingGame.SEQUENCE -> listOf("order", "answer")
        ReadingGame.DETECTIVE -> listOf("answer", "evidence")
        ReadingGame.STUDIO -> studioKeys
    }

    fun ready(a: ReadingAttempt) = requiredKeys(a).all { a.earned(it) }

    fun wrong(a: ReadingAttempt) = requiredKeys(a).any {
        a.checkpoints[it]?.let { check -> check.objective && check.tries > 0 && !check.firstCorrect } == true
    }

    private fun award(a: ReadingAttempt, key: String, correct: Boolean, now: Long,
                      objective: Boolean = true): ReadingAttempt {
        val old = a.checkpoints[key] ?: ReadingCheckpoint(readerId = a.ownerId, objective = objective)
        if (old.earned) return a
        val check = old.copy(tries = old.tries + 1,
            firstCorrect = if (old.tries == 0) correct else old.firstCorrect,
            firstSupported = if (old.tries == 0) a.supported else old.firstSupported,
            firstAt = if (old.tries == 0) now else old.firstAt,
            earnedAt = if (correct) now else null,
            supportedAtCompletion = correct && a.supported,
            rewardGranted = false)
        val answer = when (key) {
            "answer", "bonus" -> question(a)?.let { it.choices[it.answer] }
            "order" -> a.content.validOrders.first().joinToString(" ") { a.content.tiles[it] }
            "evidence" -> a.content.passage[a.content.evidence]
            else -> null
        }
        val feedback = if (answer != null)
            "${if (correct) "Correct!" else "Not quite."} Correct answer: $answer"
        else "Checkpoint saved."
        return a.copy(checkpoints = a.checkpoints + (key to check), feedback =
            if (a.game == ReadingGame.GRAMMAR) "$feedback\n${a.content.explanation}" else feedback)
    }

    fun submit(data: ReadingData, key: String, now: Long = System.currentTimeMillis(),
               rewardsEnabled: Boolean = true,
               disabledRewards: Set<RewardType> = emptySet(), random: Random = Random.Default): ReadingData {
        val session = data.activeSession
        if (session != null && key == "bonus") return data
        val updated = submitCheckpoint(data, key, now)
        if (session == null || updated == data || updated.active?.let(::wrong) != true) return updated
        val finished = edit(updated) { it.copy(finishedAt = now) }
        return completeSessionIfNeeded(finished, now, rewardsEnabled, disabledRewards, random)
    }

    private fun submitCheckpoint(data: ReadingData, key: String, now: Long): ReadingData = edit(data) { a ->
        if (a.earned(key)) return@edit a
        val game = a.game
        when {
            key == "order" && game in listOf(ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE) -> {
                if (!a.orderComplete) a.copy(feedback = "Place every tile before submitting.")
                else award(a, key, a.order in a.content.validOrders, now)
            }
            (key == "answer" && game in listOf(ReadingGame.MISSING, ReadingGame.GRAMMAR,
                ReadingGame.SEQUENCE, ReadingGame.DETECTIVE)) ||
                (key == "bonus" && game == ReadingGame.SCRAMBLE) -> {
                val question = question(a)
                val answer = a.fields["answer"]?.toIntOrNull()
                if ((game == ReadingGame.SEQUENCE || game == ReadingGame.SCRAMBLE) && !a.earned("order")) a
                else if (question == null || answer == null || answer !in question.choices.indices)
                    a.copy(feedback = "Choose an answer before submitting.")
                else award(a, key, answer == question.answer, now)
            }
            key == "evidence" && game == ReadingGame.DETECTIVE && a.earned("answer") -> {
                val index = a.fields["evidence"]?.toIntOrNull()
                if (index == null || index !in a.content.passage.indices) a.copy(feedback = "Tap a sentence in the passage.")
                else award(a, key, index == a.content.evidence, now)
            }
            key in studioKeys && game == ReadingGame.STUDIO -> {
                val before = studioKeys.takeWhile { it != key }
                if (!before.all { a.earned(it) }) a
                else if (a.fields[key].isNullOrBlank()) a.copy(feedback = "Choose a story tile or type your own idea.")
                else award(a.copy(fields = a.fields + (key to a.fields.getValue(key).trim())), key, true, now,
                    objective = false)
            }
            else -> a
        }
    }

    fun storyText(a: ReadingAttempt): String =
        "${a.fields["character"].orEmpty()} lived in ${a.fields["setting"].orEmpty()}. " +
        "One day, this character ${a.fields["problem"].orEmpty()}. " +
        "In the end, this character ${a.fields["ending"].orEmpty()}."

    fun finish(data: ReadingData, now: Long = System.currentTimeMillis(), rewardsEnabled: Boolean = true,
               disabledRewards: Set<RewardType> = emptySet(), random: Random = Random.Default): ReadingData {
        val a = data.active ?: return data
        if (!a.open || !ready(a)) return data
        val updated = edit(data) { it.copy(finishedAt = now) }
        if (updated == data) return data
        if (a.game != ReadingGame.STUDIO)
            return completeSessionIfNeeded(updated, now, rewardsEnabled, disabledRewards, random)
        val story = ReadingStory(a.id, a.content.title, storyText(a), a.content.picture.symbol, listOf(a.ownerId), now)
        return completeSessionIfNeeded(updated.copy(stories = updated.stories.filterNot { it.attemptId == a.id } + story),
            now, rewardsEnabled, disabledRewards, random)
    }

    fun nextSessionActivity(data: ReadingData): ReadingData {
        val session = data.activeSession ?: return data
        val active = data.active ?: return data
        if (session.finishedAt != null || active.finishedAt == null) return data
        val nextId = session.attemptIds.getOrNull(session.attemptIds.indexOf(active.id) + 1) ?: return data
        return data.copy(profiles = data.profiles.map {
            if (it.id == session.ownerId) it.copy(activeAttemptId = nextId) else it
        }, page = ReadingPage.ACTIVITY)
    }

    private fun completeSessionIfNeeded(data: ReadingData, now: Long, rewardsEnabled: Boolean,
                                       disabledRewards: Set<RewardType>, random: Random): ReadingData {
        val session = data.activeSession ?: return data
        return if (data.sessionWrong(session) * 10 > session.questionCount ||
            data.sessionAnswered(session) == session.questionCount)
            endSession(data, session.id, now, rewardsEnabled = rewardsEnabled,
                disabledRewards = disabledRewards, random = random)
        else data
    }

    fun endSession(data: ReadingData, id: String, now: Long = System.currentTimeMillis(),
                   abandoned: Boolean = false, timedOut: Boolean = false, rewardsEnabled: Boolean = false,
                   disabledRewards: Set<RewardType> = emptySet(), random: Random = Random.Default): ReadingData {
        val session = data.sessions.first { it.id == id }
        if (session.finishedAt != null) return data
        require(!timedOut || (session.timeLimitMs != null && session.elapsedMs >= session.timeLimitMs))
        require(abandoned || timedOut || data.sessionAnswered(session) == session.questionCount ||
            data.sessionWrong(session) * 10 > session.questionCount)
        val prize = if (abandoned) null else selectPrize(session.questionCount, data.sessionCorrect(session),
            data.sessionAnswered(session), session.rewardsEnabledAtStart, rewardsEnabled, timedOut, random, disabledRewards)
        return data.copy(sessions = data.sessions.map {
            if (it.id == id) it.copy(finishedAt = now, abandoned = abandoned, timedOut = timedOut, prizeType = prize) else it
        }, attempts = data.attempts.map {
            if (it.id in session.attemptIds && it.open) it.copy(abandoned = true) else it
        })
    }

    fun validate(data: ReadingData, resolve: (ReadingAttempt) -> ReadingAttempt = { it }) {
        require(data.version == 1) { "Unsupported reading data version." }
        val profileIds = data.profiles.map { it.id }
        require(profileIds.distinct().size == profileIds.size && profileIds.none { it.isBlank() })
        require(data.profiles.all { it.name.isNotBlank() && it.name.length <= 40 })
        require(data.selectedProfileId == null || data.selectedProfileId in profileIds)
        require(data.profiles.isEmpty() == (data.selectedProfileId == null))
        require(data.attempts.map { it.id }.distinct().size == data.attempts.size)
        val attemptsById = data.attempts.associateBy { it.id }
        require(data.sessions.map { it.id }.distinct().size == data.sessions.size)
        require(data.deletedSessionIds.none { it.isBlank() })
        require(data.sessions.none { it.id in data.deletedSessionIds })
        val sessionAttempts = data.sessions.flatMap { it.attemptIds }
        require(sessionAttempts.distinct().size == sessionAttempts.size)
        data.sessions.forEach { session ->
            require(session.id.isNotBlank() && session.ownerId in profileIds)
            require(session.questionCount in listOf(15, 25, 50))
            require(session.startedAt >= 0 && (session.finishedAt == null || session.finishedAt >= session.startedAt))
            require(session.elapsedMs >= 0 && (session.timeLimitMs == null ||
                (session.timeLimitMs > 0 && session.rewardsEnabledAtStart && session.elapsedMs <= session.timeLimitMs)))
            require(!session.timedOut || (session.finishedAt != null && session.timeLimitMs != null &&
                session.elapsedMs == session.timeLimitMs))
            require(!session.abandoned || session.finishedAt != null)
            val members = session.attemptIds.map { requireNotNull(attemptsById[it]) }
            require(members.all { it.ownerId == session.ownerId && it.game == session.game && it.level == session.level })
            require(members.all { a -> a.checkpoints.values.all {
                it.tries == 1 && (!it.objective || it.earned == it.firstCorrect)
            } })
            val answered = data.sessionAnswered(session)
            require(members.take(answered).all { it.finishedAt != null })
            require(members.drop(answered).all { it.finishedAt == null })
            require(members.drop(answered + 1).all {
                it.fields.isEmpty() && it.checkpoints.isEmpty() && it.order.isEmpty()
            })
            if (session.finishedAt == null) {
                require(!session.abandoned && !session.timedOut && session.prizeType == null)
                require(answered < session.questionCount && data.sessionWrong(session) * 10 <= session.questionCount)
                require(members.none { it.abandoned })
                require(data.sessions.count { it.ownerId == session.ownerId && it.finishedAt == null } == 1)
                val activeId = data.profiles.first { it.id == session.ownerId }.activeAttemptId
                if (activeId in session.attemptIds) require(session.attemptIds.indexOf(activeId) in
                    maxOf(0, answered - 1)..answered)
            } else {
                require(members.none { it.open })
                require(session.abandoned || session.timedOut || answered == session.questionCount ||
                    data.sessionWrong(session) * 10 > session.questionCount)
            }
            session.prizeType?.let {
                require(session.finishedAt != null && !session.abandoned && session.rewardsEnabledAtStart)
                require(qualifiesForReward(session.questionCount, data.sessionCorrect(session), answered, session.timedOut))
                require(it.tier == rewardTierForQuestionCount(session.questionCount))
            }
        }
        data.attempts.forEach { stored ->
            val a = resolve(stored)
            require(a.id.isNotBlank() && a.ownerId in profileIds && a.hints >= 0 && a.narrations >= 0)
            val size = a.content.tiles.size
            val placed = if (a.game == ReadingGame.SCRAMBLE) a.order.filterNot { it == -1 } else a.order
            require(a.order.size <= size && placed.distinct().size == placed.size && placed.all { it in 0 until size })
            require(a.checkpoints.all { (key, c) ->
                val validKeys = requiredKeys(a) + if (a.game == ReadingGame.SCRAMBLE) listOf("bonus") else emptyList()
                key in validKeys && c.tries > 0 &&
                    c.readerId == a.ownerId &&
                    c.objective == (a.game != ReadingGame.STUDIO) &&
                    (!c.firstCorrect || (c.earned && c.tries == 1))
            })
            require(a.finishedAt == null || ready(a) || (a.id in sessionAttempts && wrong(a)))
            if (a.id in sessionAttempts) {
                require(!(a.finishedAt != null && a.abandoned))
                require(!wrong(a) || a.finishedAt != null)
            }
        }
        data.profiles.forEach { profile ->
            require(profile.activeAttemptId == null || data.attempts.any {
                it.id == profile.activeAttemptId && it.includes(profile.id)
            })
        }
        require(data.stories.map { it.attemptId }.distinct().size == data.stories.size)
        data.stories.forEach { story ->
            val attempt = data.attempts.first { it.id == story.attemptId }
            require(attempt.finishedAt != null && attempt.game == ReadingGame.STUDIO &&
                story.readerIds == listOf(attempt.ownerId))
            require(story.text == storyText(attempt))
        }
    }
}
