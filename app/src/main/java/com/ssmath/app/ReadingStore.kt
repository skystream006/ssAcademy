package com.ssmath.app

import android.app.Application
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

interface ReadingStorage {
    fun load(): ReadingData
    fun save(data: ReadingData)
}

class ReadingStore(file: File) : ReadingStorage {
    private val atomic = AtomicFile(file)
    private val json = Json { encodeDefaults = true }
    private var loaded = false

    @Synchronized
    override fun load(): ReadingData {
        loaded = false
        val data = if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) ReadingData()
        else try {
            val text = atomic.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = json.parseToJsonElement(text)
            require(root is JsonObject && listOf("version", "profiles", "selectedProfileId", "attempts", "stories").all {
                it in root
            })
            json.decodeFromJsonElement(ReadingData.serializer(), root)
        } catch (error: Exception) {
            throw IOException("Reading data could not be opened. The existing file has been kept.", error)
        }
        try {
            ReadingEngine.validate(data)
        } catch (error: Exception) {
            throw IOException("Reading data needs repair. The existing file has been kept.", error)
        }
        loaded = true
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
    private val storage: ReadingStorage = ReadingStore(File(application.filesDir, "reading_private.json")),
    private val io: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application,
        ReadingStore(File(application.filesDir, "reading_private.json")), Dispatchers.IO)

    var data by mutableStateOf<ReadingData?>(null)
        private set
    var storageError by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    private var pending: ReadingData? = null
    private val settings = application.getSharedPreferences("settings", 0)
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val deferred = ArrayDeque<Command.Edit>()
    private sealed interface Command {
        class Edit(
            val profileId: String?,
            val attemptId: String?,
            val block: (ReadingData) -> ReadingData
        ) : Command
        data object Retry : Command
    }

    init {
        viewModelScope.launch {
            load()
            for (command in commands) {
                when (command) {
                    Command.Retry -> {
                        if (pending != null) persist(requireNotNull(pending)) else load()
                        while (storageError == null && deferred.isNotEmpty()) apply(deferred.removeFirst())
                    }
                    is Command.Edit -> {
                        // Already accepted edits must survive a preceding failed write.
                        if (storageError != null) deferred.addLast(command) else apply(command)
                    }
                }
            }
        }
    }

    private suspend fun apply(command: Command.Edit) {
        val current = data ?: return
        if (current.selectedProfileId != command.profileId || current.profile?.activeAttemptId != command.attemptId) return
        val next = try {
            command.block(current)
        } catch (error: IllegalArgumentException) {
            message = error.message ?: "Please check that choice."
            return
        }
        if (next != current) persist(next)
    }

    private suspend fun load() {
        saving = true
        try {
            data = withContext(io) { storage.load() }
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

    private suspend fun persist(next: ReadingData) {
        saving = true
        pending = next
        try {
            withContext(io) { storage.save(next) }
            data = next
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

    fun submit(key: String, expectedTurn: Int? = null) {
        val rewardsEnabled = settings.getBoolean(Subject.READING.rewardsPreference, true)
        act { ReadingEngine.submit(it, key, expectedTurn = expectedTurn, rewardsEnabled = rewardsEnabled) }
    }

    fun setName(name: String) {
        act { if (it.profile == null) ReadingEngine.addProfile(it, name) else it }
    }

    fun retry() {
        commands.trySend(Command.Retry)
    }
}
