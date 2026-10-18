package com.ssmath.app

import android.app.Application
import android.os.SystemClock
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

interface ReadingStorage {
    fun load(): ReadingData
    fun save(data: ReadingData)
    fun prepare(data: ReadingData): ReadingData = data
    fun review(attempt: ReadingAttempt): ReadingAttempt = attempt
    fun start(data: ReadingData, game: ReadingGame, level: ReadingLevel, newAttempt: Boolean): ReadingData =
        ReadingEngine.start(data, game, level, newAttempt).also(::save)
    fun startSession(data: ReadingData, game: ReadingGame, level: ReadingLevel, count: Int,
                     rewardsEnabledAtStart: Boolean, timeLimitMs: Long? = null): ReadingData =
        ReadingEngine.startSession(data, game, level, count, rewardsEnabledAtStart,
            timeLimitMs = timeLimitMs).also(::save)
}

class ReadingStore(file: File) : ReadingStorage {
    private val atomic = AtomicFile(file)
    private val json = Json { encodeDefaults = true }
    private var loaded = false

    @Synchronized
    override fun load(): ReadingData {
        loaded = false
        var migrated = false
        val data = if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) ReadingData()
        else try {
            val text = atomic.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = json.parseToJsonElement(text)
            require(root is JsonObject && listOf("version", "profiles", "selectedProfileId", "attempts", "stories").all {
                it in root
            })
            val cleaned = ReadingMigration.migrate(root)
            migrated = cleaned != root
            json.decodeFromJsonElement(ReadingData.serializer(), cleaned)
        } catch (error: Exception) {
            throw IOException("Reading data could not be opened. The existing file has been kept.", error)
        }
        try {
            ReadingEngine.validate(data)
        } catch (error: Exception) {
            throw IOException("Reading data needs repair. The existing file has been kept.", error)
        }
        loaded = true
        try {
            if (migrated) save(data)
        } catch (error: Exception) {
            loaded = false
            throw error
        }
        return data
    }

    @Synchronized
    override fun save(data: ReadingData) {
        check(loaded) { "Load reading data successfully before saving." }
        ReadingEngine.validate(data)
        val bytes = json.encodeToString(ReadingData.serializer(), data).toByteArray(Charsets.UTF_8)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw IOException("Reading data could not be saved.", error)
        }
    }
}

/** One Activity-scoped actor serializes edits and disk writes, including rapid taps and typed drafts. */
class ReadingViewModel(
    application: Application,
    private val storage: ReadingStorage = ReadingSqliteStore(application),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val importer: ReadingPackImporter? = null,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val rewardRandom: Random = Random.Default
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application,
        ReadingSqliteStore(application), Dispatchers.IO)

    var data by mutableStateOf<ReadingData?>(null)
        private set
    var storageError by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var packStatus by mutableStateOf<ReadingPackStatus?>(null)
        private set
    var importing by mutableStateOf(false)
        private set
    var importProgress by mutableStateOf<String?>(null)
        private set
    var sessionElapsedMs by mutableStateOf(0L)
        private set
    val sessionRemainingMs: Long? get() = data?.activeSession?.timeLimitMs?.let {
        (it - sessionElapsedMs).coerceAtLeast(0)
    }
    private var sessionForeground = false
    private var clockSessionId: String? = null
    private var runningSince: Long? = null
    private var timerJob: Job? = null
    private var pending: ReadingData? = null
    private var pendingStart: Command.Edit? = null
    private val settings = application.getSharedPreferences("settings", 0)
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val deferred = ArrayDeque<Command.Edit>()
    private sealed interface Command {
        class Edit(
            val profileId: String?,
            val attemptId: String?,
            val block: (ReadingData) -> ReadingData,
            val start: Start? = null,
            val historyDeletion: Boolean = false
        ) : Command
        data class Start(val game: ReadingGame, val level: ReadingLevel?, val newAttempt: Boolean,
                         val count: Int? = null, val rewardsEnabled: Boolean = false, val timeLimitMs: Long? = null)
        data object Tick : Command
        data object Retry : Command
        data object Import : Command
    }

    init {
        viewModelScope.launch {
            load()
            refreshSessionClock()
            for (command in commands) {
                when (command) {
                    Command.Import -> importPack()
                    Command.Tick -> updateSessionClock(flush = !sessionForeground)
                    Command.Retry -> {
                        if (pendingStart != null) apply(requireNotNull(pendingStart))
                        else if (pending != null) persist(requireNotNull(pending)) else load()
                        while (storageError == null && deferred.isNotEmpty()) apply(deferred.removeFirst())
                    }
                    is Command.Edit -> {
                        // Already accepted edits must survive a preceding failed write.
                        if (storageError != null) deferred.addLast(command) else apply(command)
                    }
                }
                refreshSessionClock()
            }
        }
    }

    private suspend fun apply(command: Command.Edit) {
        updateSessionClock()
        val current = data ?: return
        if (storageError != null && command != pendingStart) {
            deferred.addLast(command)
            return
        }
        if (current.selectedProfileId != command.profileId || current.profile?.activeAttemptId != command.attemptId) return
        command.start?.let { start ->
            if (isDisabled(start.game)) {
                message = "This reading game is disabled in Settings."
                pendingStart = null
                return
            }
            saving = true
            pendingStart = command
            try {
                data = withContext(io) {
                    val level = start.level ?: requireNotNull(current.profile).level
                    if (start.count == null) storage.start(current, start.game, level, start.newAttempt)
                    else storage.startSession(current, start.game, level, start.count,
                        start.rewardsEnabled, start.timeLimitMs)
                }
                pendingStart = null
                storageError = null
                message = null
            } catch (error: IllegalArgumentException) {
                pendingStart = null
                storageError = null
                message = error.message ?: "Please check that choice."
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                storageError = "Your activity could not be saved. Nothing was assigned. Free storage, then retry."
            } finally {
                saving = false
            }
            return
        }
        val next = try {
            command.block(current)
        } catch (error: IllegalArgumentException) {
            message = error.message ?: "Please check that choice."
            return
        }
        if (!command.historyDeletion && ((next.active?.open == true && isDisabled(requireNotNull(next.active).game) &&
            (next.page == ReadingPage.ACTIVITY || next.active != current.active)) ||
            (current.active?.open == true && isDisabled(requireNotNull(current.active).game) &&
                next.attempts != current.attempts))) {
            message = "This reading game is disabled in Settings."
            return
        }
        if (next != current) persist(next)
    }

    private suspend fun load() {
        saving = true
        try {
            data = withContext(io) { storage.load() }
            packStatus = withContext(io) { (storage as? ReadingSqliteStore)?.catalog?.status() }
            storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = "Your reading file could not be opened. Nothing has been overwritten. " +
                "Ask an adult to check device storage, then retry."
        } finally {
            saving = false
        }
    }

    private suspend fun persist(next: ReadingData, blockInteraction: Boolean = true) {
        saving = blockInteraction
        pending = next
        try {
            data = withContext(io) {
                val prepared = storage.prepare(next)
                storage.save(prepared)
                prepared
            }
            pending = null
            storageError = null
            message = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storageError = "Your latest change has not been saved. Reading is paused to protect your progress. " +
                "Free some device storage, then retry saving."
        } finally {
            saving = false
        }
    }

    fun act(block: (ReadingData) -> ReadingData) {
        val current = data
        if (current != null && storageError == null)
            commands.trySend(Command.Edit(current.selectedProfileId, current.profile?.activeAttemptId, block))
    }

    fun deleteHistory(attemptIds: Set<String>) {
        val current = data ?: return
        if (!saving && storageError == null) commands.trySend(Command.Edit(
            current.selectedProfileId, current.profile?.activeAttemptId,
            { ReadingEngine.deleteHistory(it, attemptIds) }, historyDeletion = true))
    }

    fun clearHistory(readerId: String) {
        deleteHistory(data?.attemptsFor(readerId)?.map { it.id }?.toSet() ?: return)
    }

    suspend fun review(attempt: ReadingAttempt): ReadingAttempt = withContext(io) {
        storage.review(attempt)
    }

    fun start(game: ReadingGame, level: ReadingLevel? = null, newAttempt: Boolean = false) {
        val current = data ?: return
        if (storageError == null && !importing) commands.trySend(Command.Edit(
            current.selectedProfileId, current.profile?.activeAttemptId, { it },
            Command.Start(game, level, newAttempt)))
    }

    fun startSession(game: ReadingGame, count: Int) {
        val current = data ?: return
        if (storageError != null || importing) return
        val enabled = settings.getBoolean(Subject.READING.rewardsPreference, true)
        val seconds = settings.readTimeLimitSeconds()
        val limit = (seconds * 1_000L).takeIf { enabled && settings.getBoolean("show_timer", false) && it > 0 }
        commands.trySend(Command.Edit(current.selectedProfileId, current.profile?.activeAttemptId, { it },
            Command.Start(game, null, true, count, enabled, limit)))
    }

    private fun isDisabled(game: ReadingGame) =
        game.name in settings.getStringSet("disabled_reading_games", emptySet()).orEmpty()

    private fun disabledRewards() = RewardType.entries.filter {
        it.name in settings.getStringSet("disabled_rewards", emptySet()).orEmpty()
    }.toSet()

    fun finishActivity() {
        act { current ->
            if (current.active?.let { isDisabled(it.game) } == true) current
            else ReadingEngine.nextSessionActivity(ReadingEngine.finish(current,
                rewardsEnabled = settings.getBoolean(Subject.READING.rewardsPreference, true),
                disabledRewards = disabledRewards(), random = rewardRandom,
                rewardThresholds = settings.readRewardThresholds()))
        }
    }

    fun nextSessionActivity() {
        act { ReadingEngine.nextSessionActivity(it) }
    }

    /** Call with true only while the activity screen is visible and its lifecycle is resumed. */
    fun setSessionForeground(visible: Boolean) {
        captureSessionClock()
        sessionForeground = visible
        refreshSessionClock()
        commands.trySend(Command.Tick)
    }

    private fun captureSessionClock() {
        val now = clock()
        runningSince?.let {
            sessionElapsedMs += (now - it).coerceAtLeast(0)
            runningSince = now
            data?.activeSession?.timeLimitMs?.let { limit -> sessionElapsedMs = sessionElapsedMs.coerceAtMost(limit) }
        }
    }

    private fun refreshSessionClock() {
        captureSessionClock()
        val session = data?.activeSession
        if (session?.id != clockSessionId || session?.finishedAt != null) {
            clockSessionId = session?.id
            sessionElapsedMs = session?.elapsedMs ?: 0
            runningSince = null
        }
        val running = session != null && session.finishedAt == null && sessionForeground &&
            data?.page == ReadingPage.ACTIVITY && storageError == null && !isDisabled(session.game)
        if (running) {
            if (runningSince == null) runningSince = clock()
            if (timerJob == null) timerJob = viewModelScope.launch {
                while (true) {
                    delay(200)
                    commands.trySend(Command.Tick)
                }
            }
        } else {
            runningSince = null
            timerJob?.cancel()
            timerJob = null
        }
    }

    private suspend fun updateSessionClock(flush: Boolean = true) {
        captureSessionClock()
        val current = data ?: return
        val session = current.activeSession ?: return
        if (session.finishedAt != null || storageError != null || session.id != clockSessionId) return
        val elapsed = maxOf(session.elapsedMs, sessionElapsedMs)
        if (elapsed == session.elapsedMs) return
        val expired = session.timeLimitMs?.let { elapsed >= it } == true
        if (!flush && !expired && elapsed - session.elapsedMs < 1_000) return
        var next = current.copy(sessions = current.sessions.map {
            if (it.id == session.id) it.copy(elapsedMs = elapsed) else it
        })
        if (expired)
            next = ReadingEngine.endSession(next, session.id, timedOut = true,
                rewardThresholds = settings.readRewardThresholds())
        // Routine clock saves must not toggle the enabled state of answer controls.
        persist(next, blockInteraction = expired)
    }

    fun importActivities() {
        if (importing || saving || storageError != null || storage !is ReadingSqliteStore) return
        importing = true
        importProgress = "Preparing reading import…"
        commands.trySend(Command.Import)
    }

    private suspend fun importPack() {
        try {
            if (storageError != null) {
                importProgress = "Retry saving your reading progress before importing activities."
                return
            }
            val store = storage as? ReadingSqliteStore ?: return
            packStatus = (importer ?: ReadingPackImporter(store.catalog, getApplication<Application>().cacheDir))
                .importPack { importProgress = it }
            importProgress = "Reading activities are ready to play offline."
            message = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            importProgress = "Import failed. Your installed activities and progress are unchanged. " +
                "Check your connection and available storage, then retry. The repository dataset must be public."
        } finally {
            importing = false
        }
    }

    fun submit(key: String) {
        act { current ->
            if (current.active?.let { isDisabled(it.game) } == true) current
            else {
                val rewardsEnabled = settings.getBoolean(Subject.READING.rewardsPreference, true)
                val disabled = disabledRewards()
                val thresholds = settings.readRewardThresholds()
                val submitted = ReadingEngine.submit(current, key,
                    rewardsEnabled = rewardsEnabled, disabledRewards = disabled, random = rewardRandom,
                    rewardThresholds = thresholds)
                ReadingEngine.nextSessionActivity(ReadingEngine.finish(submitted,
                    rewardsEnabled = rewardsEnabled, disabledRewards = disabled, random = rewardRandom,
                    rewardThresholds = thresholds))
            }
        }
    }

    fun setName(name: String) {
        act { if (it.profile == null) ReadingEngine.addProfile(it, name) else it }
    }

    fun updateName(name: String) {
        act { ReadingEngine.renameProfile(it, name) }
    }

    fun retry() {
        commands.trySend(Command.Retry)
    }
}
