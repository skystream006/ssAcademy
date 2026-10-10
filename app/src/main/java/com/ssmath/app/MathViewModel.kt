package com.ssmath.app

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class Screen { SETUP, REWARDS_SETUP, READY, PLAYING, RESULTS }

enum class SetupDialog { PRACTICE, REWARDS }

enum class Overlay { SETTINGS, HISTORY, REWARDS, POKEMONS }

data class Feedback(val text: String, val correct: Boolean)

class MathViewModel(
    application: Application,
    private val generator: ProblemGenerator = ProblemGenerator(),
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val rewardRandom: Random = Random.Default,
    private val celebrationRandom: Random = Random.Default,
    private val progressIconRandom: Random = Random.Default
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, ProblemGenerator())

    private val settings = application.getSharedPreferences("settings", 0)
    private val store = HistoryStore(File(application.filesDir, "practice_history.json"))
    private val storeLock = Mutex()

    var screen by mutableStateOf(Screen.SETUP)
        private set
    var setupDialog by mutableStateOf<SetupDialog?>(null)
        private set
    var gameMode by mutableStateOf(GameMode.PRACTICE)
        private set
    private data class ReadyGame(val mode: GameMode, val operation: Operation, val parameters: GameParameters)
    private var readyGame by mutableStateOf<ReadyGame?>(null)
    val readyOperation: Operation? get() = readyGame?.operation
    val readyParameters: GameParameters? get() = readyGame?.parameters
    var overlay by mutableStateOf<Overlay?>(null)
        private set
    private var collectionReturnOverlay: Overlay? = null
    var selectedOperation by mutableStateOf(Operation.fromPreference(settings.getString("operation", null)))
        private set
    var minimumText by mutableStateOf(settings.getInt("minimum", MIN_MAXIMUM).toString())
        private set
    var maximumText by mutableStateOf(settings.getInt("maximum", 10).toString())
        private set
    var divisionMaximumFirstText by mutableStateOf(settings.getInt("division_maximum_first", settings.getInt("maximum", 10)).toString())
        private set
    var divisionMaximumSecondText by mutableStateOf(settings.getInt("division_maximum_second", settings.getInt("maximum", 10)).toString())
        private set
    var questionCountText by mutableStateOf(settings.getInt("question_count", DEFAULT_QUESTION_COUNT).toString())
        private set
    var rewardsDefaults by mutableStateOf(loadRewardsDefaults())
        private set
    var rewardsSetupDraft by mutableStateOf(rewardsDefaults)
        private set
    var game by mutableStateOf<GameState?>(null)
        private set
    internal var progressIcon by mutableStateOf(ProgressIcon.ROCKET)
        private set
    var answerText by mutableStateOf("")
        private set
    var lastResult by mutableStateOf<PracticeResult?>(null)
        private set
    var celebration by mutableStateOf<Celebration?>(null)
        private set
    var selectingCelebration by mutableStateOf(false)
        private set
    var earlyFinishMessage by mutableStateOf<String?>(null)
        private set
    var history by mutableStateOf<List<PracticeResult>>(emptyList())
        private set
    var historyDetail by mutableStateOf<PracticeResult?>(null)
        private set
    var rewardBalances by mutableStateOf<Map<RewardType, RewardBalance>>(emptyMap())
        private set
    var pokemons by mutableStateOf<Set<Celebration>>(emptySet())
        private set
    var rewardResult by mutableStateOf<PracticeResult?>(null)
        private set
    private var rewardDialogOverlay by mutableStateOf<Overlay?>(null)
    val rewardDialogVisible: Boolean get() = rewardResult != null && celebration == null &&
        !selectingCelebration && overlay == rewardDialogOverlay
    var claimingReward by mutableStateOf(false)
        private set
    var rewardError by mutableStateOf<String?>(null)
        private set
    var usingReward by mutableStateOf(false)
        private set
    var rewardUseError by mutableStateOf<String?>(null)
        private set
    var removingReward by mutableStateOf(false)
        private set
    var rewardRemovalError by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    var showTimer by mutableStateOf(settings.getBoolean("show_timer", false))
        private set
    var rewardsEnabled by mutableStateOf(settings.getBoolean("rewards_enabled", true))
        private set
    var disabledRewards by mutableStateOf(RewardType.entries.filter {
        it.name in settings.getStringSet("disabled_rewards", emptySet()).orEmpty()
    }.toSet())
        private set
    var timeLimitMinutes by mutableStateOf(normalizeTimeLimit(settings.getInt("time_limit_minutes", 0)))
        private set
    var activeTimeLimitMs by mutableStateOf<Long?>(null)
        private set
    var showCorrectAnswers by mutableStateOf(settings.getBoolean("show_correct_answers", false))
        private set
    var verticalEquations by mutableStateOf(settings.getBoolean("vertical_equations", false))
        private set
    var textSizePercent by mutableStateOf(settings.getInt("text_size_percent", DEFAULT_TEXT_SIZE_PERCENT)
        .coerceIn(MIN_TEXT_SIZE_PERCENT, MAX_TEXT_SIZE_PERCENT))
        private set
    var waveAppearance by mutableStateOf(settings.getBoolean("wave_appearance", true))
        private set
    var theme by mutableStateOf(colorThemeFromPreference(settings.getString("theme", null)))
        private set
    var mode by mutableStateOf(settings.getString("mode", null))
        private set
    var skinsEnabled by mutableStateOf(settings.getBoolean("skins_enabled", false))
        private set
    var skin by mutableStateOf(AppSkin.fromPreference(settings.getString("skin", null)))
        private set

    private var elapsedBefore = 0L
    private var runningSince: Long? = null
    private var foreground = true
    private var timeoutJob: Job? = null
    private var rewardsEnabledAtStart = false
    private var latestResultId = 0L
    private var rewardRequestVersion = 0L
    private class PendingResult(var result: PracticeResult, var saved: Boolean = false)
    private var pendingResult: PendingResult? = null
    private var collectionLoaded = false

    val maximum: Int? get() = parseMaximum(maximumText)
    val minimum: Int? get() = parseMaximum(minimumText)?.takeIf { it <= (maximum ?: MAX_MAXIMUM) }
    val divisionMaximumFirst: Int? get() = parseMaximum(divisionMaximumFirstText)
    val divisionMaximumSecond: Int? get() = parseMaximum(divisionMaximumSecondText)
    val questionCount: Int? get() = parseQuestionCount(questionCountText)
    val canSubmitSetup: Boolean get() = selectedOperation != null && questionCount != null &&
        if (selectedOperation == Operation.DIVISION) divisionMaximumFirst != null && divisionMaximumSecond != null
        else minimum != null && maximum != null
    val canSaveRewardsSetup: Boolean get() = Operation.entries.all { rewardsSetupDraft[it]?.isValid(it) == true }
    val feedback: Feedback?
        get() = game?.attempts?.lastOrNull()?.let { attempt ->
            when {
                attempt.correct -> Feedback("Correct! +1 point", true)
                showCorrectAnswers -> Feedback("Not quite: ${attempt.problem.text} = ${attempt.problem.answer}", false)
                else -> Feedback("Not quite!", false)
            }
        }

    init {
        viewModelScope.launch { reloadHistory() }
    }

    private fun loadRewardsDefaults(): Map<Operation, GameParameters> {
        val defaults = Operation.entries.associateWith { operation ->
            val legacy = if (operation == Operation.DIVISION)
                GameParameters(divisionMaximumFirstText, divisionMaximumSecondText, questionCountText)
            else GameParameters(minimumText, maximumText, questionCountText)
            val fallback = legacy.takeIf { it.isValid(operation) } ?: GameParameters.defaults(operation)
            val prefix = "rewards_${operation.name}_"
            GameParameters(
                settings.getInt("${prefix}first", requireNotNull(fallback.first)).toString(),
                settings.getInt("${prefix}second", requireNotNull(fallback.second)).toString(),
                settings.getInt("${prefix}question_count", requireNotNull(fallback.questionCount)).toString()
            ).takeIf { it.isValid(operation) } ?: fallback
        }
        // Freeze the migration now so future practice edits cannot change unsaved rewards defaults.
        persistRewardsDefaults(defaults)
        return defaults
    }

    private fun persistRewardsDefaults(defaults: Map<Operation, GameParameters>) {
        val edit = settings.edit()
        defaults.forEach { (operation, parameters) ->
            val prefix = "rewards_${operation.name}_"
            edit.putInt("${prefix}first", requireNotNull(parameters.first))
                .putInt("${prefix}second", requireNotNull(parameters.second))
                .putInt("${prefix}question_count", requireNotNull(parameters.questionCount))
        }
        edit.apply()
    }

    fun openPracticeGame() {
        if (screen == Screen.SETUP && overlay == null) setupDialog = SetupDialog.PRACTICE
    }

    fun openRewardsGame() {
        if (screen == Screen.SETUP && overlay == null) setupDialog = SetupDialog.REWARDS
    }

    fun dismissSetupDialog() { setupDialog = null }

    fun openRewardsSetup() {
        if (screen != Screen.SETUP || overlay != null || setupDialog != SetupDialog.REWARDS) return
        rewardsSetupDraft = rewardsDefaults
        setupDialog = null
        screen = Screen.REWARDS_SETUP
    }

    fun updateRewardsParameters(operation: Operation, parameters: GameParameters) {
        if (screen != Screen.REWARDS_SETUP || overlay != null) return
        rewardsSetupDraft = rewardsSetupDraft + (operation to parameters)
    }

    fun saveRewardsSetup() {
        if (screen != Screen.REWARDS_SETUP || overlay != null || !canSaveRewardsSetup) return
        val defaults = rewardsSetupDraft.mapValues { (_, parameters) ->
            GameParameters(requireNotNull(parameters.first).toString(), requireNotNull(parameters.second).toString(),
                requireNotNull(parameters.questionCount).toString())
        }
        persistRewardsDefaults(defaults)
        rewardsDefaults = defaults
        rewardsSetupDraft = defaults
        screen = Screen.SETUP
        setupDialog = SetupDialog.REWARDS
    }

    fun cancelRewardsSetup() {
        if (screen != Screen.REWARDS_SETUP || overlay != null) return
        rewardsSetupDraft = rewardsDefaults
        screen = Screen.SETUP
        setupDialog = SetupDialog.REWARDS
    }

    fun selectRewardsGame(operation: Operation) {
        if (screen != Screen.SETUP || overlay != null || setupDialog != SetupDialog.REWARDS) return
        val parameters = rewardsDefaults[operation]?.takeIf { it.isValid(operation) } ?: return
        ready(GameMode.REWARDS, operation, parameters)
    }

    private fun ready(mode: GameMode, operation: Operation, parameters: GameParameters) {
        gameMode = mode
        readyGame = ReadyGame(mode, operation, parameters)
        setupDialog = null
        screen = Screen.READY
    }

    fun selectOperation(operation: Operation) { selectedOperation = operation }

    fun updateMinimum(text: String) { minimumText = text.take(6) }

    fun updateMaximum(text: String) { maximumText = text.filter { it in '0'..'9' }.take(6) }

    fun updateDivisionMaximumFirst(text: String) { divisionMaximumFirstText = text.take(6) }

    fun updateDivisionMaximumSecond(text: String) { divisionMaximumSecondText = text.take(6) }

    fun updateQuestionCount(text: String) { questionCountText = text.take(6) }

    fun submitSetup() {
        if (screen != Screen.SETUP || overlay != null || setupDialog == SetupDialog.REWARDS || !canSubmitSetup) return
        val operation = selectedOperation ?: return
        val questionCount = questionCount ?: return
        val edit = settings.edit().putString("operation", operation.name).putInt("question_count", questionCount)
        if (operation == Operation.DIVISION) {
            edit.putInt("division_maximum_first", requireNotNull(divisionMaximumFirst))
                .putInt("division_maximum_second", requireNotNull(divisionMaximumSecond))
        } else {
            edit.putInt("maximum", requireNotNull(maximum)).putInt("minimum", requireNotNull(minimum))
        }
        edit.apply()
        val parameters = if (operation == Operation.DIVISION)
            GameParameters(divisionMaximumFirstText, divisionMaximumSecondText, questionCountText)
        else GameParameters(minimumText, maximumText, questionCountText)
        ready(GameMode.PRACTICE, operation, parameters)
    }

    fun backToSetup() {
        if (screen == Screen.REWARDS_SETUP) {
            cancelRewardsSetup()
            return
        }
        val returnDialog = if (screen == Screen.READY)
            if (gameMode == GameMode.PRACTICE) SetupDialog.PRACTICE else SetupDialog.REWARDS
        else null
        if (screen == Screen.PLAYING) DebugLog.event(DebugEvent.GAME_ABANDONED)
        game = null
        answerText = ""
        celebration = null
        selectingCelebration = false
        earlyFinishMessage = null
        lastResult = null
        pendingResult = null
        dismissReward()
        activeTimeLimitMs = null
        resetTimer()
        readyGame = null
        setupDialog = returnDialog
        screen = Screen.SETUP
    }

    fun start() {
        if (screen != Screen.READY || overlay != null) return
        val ready = readyGame ?: return
        val operation = ready.operation
        val parameters = ready.parameters
        val division = operation == Operation.DIVISION
        val minimum = if (division) MIN_MAXIMUM else parameters.first ?: return
        val maximum = (if (division) parameters.first else parameters.second) ?: return
        val maximumSecond = if (division) parameters.second ?: return else maximum
        val questionCount = parameters.questionCount ?: return
        gameMode = ready.mode
        game = GameState.start(operation, maximum, generator, questionCount, minimum, maximumSecond)
        progressIcon = ProgressIcon.entries.random(progressIconRandom)
        answerText = ""
        celebration = null
        selectingCelebration = false
        earlyFinishMessage = null
        lastResult = null
        pendingResult = null
        dismissReward()
        rewardsEnabledAtStart = gameMode == GameMode.REWARDS && rewardsEnabled
        activeTimeLimitMs = if (rewardsEnabledAtStart && showTimer && timeLimitMinutes > 0)
            timeLimitMinutes * 60_000L else null
        resetTimer()
        readyGame = null
        screen = Screen.PLAYING
        updateTimer()
        DebugLog.event(DebugEvent.GAME_STARTED)
    }

    fun updateAnswer(text: String) {
        if (screen == Screen.PLAYING && overlay == null && foreground) answerText = text.filter { it in '0'..'9' }.take(9)
    }

    fun submitAnswer() {
        val current = game ?: return
        if (screen != Screen.PLAYING || overlay != null || !foreground || current.finished) return
        if (checkTimeLimit()) return
        val value = parseAnswer(answerText) ?: return
        val next = current.answer(value, generator)
        game = next
        answerText = ""
        if (next.finished) finish(next)
    }

    private fun finish(state: GameState, timedOut: Boolean = false) {
        val elapsed = elapsedMs()
        val expired = timedOut || activeTimeLimitMs?.let { elapsed >= it } == true
        val duration = if (expired) activeTimeLimitMs ?: elapsed else elapsed
        resetTimer()
        val finishedAt = wallClock()
        latestResultId = maxOf(Math.addExact(latestResultId, 1), finishedAt)
        val result = PracticeResult(latestResultId, finishedAt, state.operation, state.maximum, duration, state.attempts,
            minimum = state.minimum, maximumSecond = state.maximumSecond.takeIf { state.operation == Operation.DIVISION },
            questionCount = state.questionCount, timedOut = expired, gameMode = gameMode,
            prizeType = selectPrize(state.questionCount, state.correct, state.attempts.size,
                rewardsEnabledAtStart, rewardsEnabled, expired, rewardRandom, disabledRewards))
        val pending = PendingResult(result)
        pendingResult = pending
        lastResult = result
        val answeredAllQuestions = !expired && state.attempts.size == state.questionCount
        celebration = null
        selectingCelebration = answeredAllQuestions
        earlyFinishMessage = if (expired) "Time's up! You got ${state.correct} right out of ${state.questionCount}"
            else if (answeredAllQuestions) null
            else "Nice try! You got ${state.correct} right out of ${state.questionCount}"
        screen = Screen.RESULTS
        rewardResult = result.takeIf { it.prizeType != null }
        DebugLog.event(DebugEvent.GAME_FINISHED)
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    if (pendingResult === pending) {
                        celebration = if (selectingCelebration && collectionLoaded)
                            Celebration.select(state.questionCount, celebrationRandom, pokemons, result.gameMode) else null
                        selectingCelebration = false
                    }
                    persistResult(pending)
                }
                DebugLog.event(DebugEvent.HISTORY_SAVED)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                if (pendingResult === pending) {
                    message = "Unable to save these results. Free device storage and try again."
                    if (rewardResult === pending.result) rewardError = "Unable to save your prize. Free device storage and try again."
                }
            } finally {
                if (pendingResult === pending) selectingCelebration = false
            }
        }
    }

    fun done() {
        lastResult = null
        game = null
        answerText = ""
        celebration = null
        selectingCelebration = false
        earlyFinishMessage = null
        pendingResult = null
        dismissReward()
        resetTimer()
        activeTimeLimitMs = null
        readyGame = null
        setupDialog = null
        screen = Screen.SETUP
    }

    fun dismissCelebration() {
        celebration = null
        selectingCelebration = false
    }

    fun collectPresentedCelebration(presented: Celebration) {
        val pending = pendingResult ?: return
        if (celebration != presented || overlay != null || pending.result.pokemonReward != null) return
        if (pending.result.gameMode == GameMode.PRACTICE && presented.category != CelebrationCategory.OTHER) return
        collectCelebrations(listOf(presented), pending)
    }

    fun collectAllCelebrations() { collectCelebrations(Celebration.entries) }

    private fun collectCelebrations(celebrations: Collection<Celebration>, pending: PendingResult? = null) {
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    if (pending != null && !pending.saved) persistResult(pending)
                    val snapshot = withContext(ioDispatcher) {
                        if (pending == null) store.collectPokemons(celebrations)
                        else store.collectPokemon(pending.result.id, celebrations.single())
                    }
                    applySnapshot(snapshot)
                    if (pending != null) {
                        snapshot.history.find { it.id == pending.result.id }?.let { collected ->
                            val previous = pending.result
                            pending.result = collected
                            if (lastResult == previous) lastResult = collected
                            if (rewardResult == previous) rewardResult = collected
                            if (historyDetail?.id == collected.id) historyDetail = collected
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                message = "Unable to save your celebrations. Free device storage and try again."
            }
        }
    }

    fun dismissEarlyFinishDialog() { earlyFinishMessage = null }

    fun elapsedMs(): Long = elapsedBefore + (runningSince?.let { (clock() - it).coerceAtLeast(0) } ?: 0L)

    fun checkTimeLimit(): Boolean {
        val limit = activeTimeLimitMs ?: return false
        val current = game ?: return false
        if (screen != Screen.PLAYING || overlay != null || !foreground || elapsedMs() < limit) return false
        finish(current, timedOut = true)
        return true
    }

    private fun resetTimer() {
        elapsedBefore = 0
        runningSince = null
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /** The timer only runs while a game is visible: it pauses in settings, history, or the background. */
    private fun updateTimer() {
        val shouldRun = screen == Screen.PLAYING && overlay == null && foreground
        val since = runningSince
        if (shouldRun && since == null) runningSince = clock()
        else if (!shouldRun && since != null) {
            elapsedBefore += (clock() - since).coerceAtLeast(0)
            runningSince = null
        }
        if (!shouldRun) {
            timeoutJob?.cancel()
            timeoutJob = null
        } else if (activeTimeLimitMs != null && timeoutJob == null) {
            timeoutJob = viewModelScope.launch {
                while (screen == Screen.PLAYING && overlay == null && foreground) {
                    if (checkTimeLimit()) break
                    delay(200)
                }
            }
        }
    }

    fun setForeground(value: Boolean) {
        foreground = value
        updateTimer()
    }

    fun openSettings() {
        dismissOverlayReward()
        rewardRemovalError = null
        overlay = Overlay.SETTINGS
        updateTimer()
    }

    fun openHistory() {
        dismissOverlayReward()
        historyDetail = null
        overlay = Overlay.HISTORY
        updateTimer()
        viewModelScope.launch { reloadHistory() }
    }

    fun openRewards() {
        dismissOverlayReward()
        rewardUseError = null
        if (overlay != Overlay.REWARDS && overlay != Overlay.POKEMONS) collectionReturnOverlay = overlay
        overlay = Overlay.REWARDS
        updateTimer()
        viewModelScope.launch { reloadHistory() }
    }

    fun openPokemons() {
        dismissOverlayReward()
        if (overlay != Overlay.REWARDS && overlay != Overlay.POKEMONS) collectionReturnOverlay = overlay
        overlay = Overlay.POKEMONS
        updateTimer()
        viewModelScope.launch { reloadHistory() }
    }

    fun closeOverlay() {
        dismissOverlayReward()
        if (overlay == Overlay.HISTORY && historyDetail != null) {
            historyDetail = null
            return
        }
        overlay = when (overlay) {
            Overlay.HISTORY -> Overlay.SETTINGS
            Overlay.REWARDS, Overlay.POKEMONS -> collectionReturnOverlay
            else -> null
        }
        updateTimer()
    }

    fun showHistoryDetail(result: PracticeResult?) { historyDetail = result }

    fun showRewardForResult(result: PracticeResult) {
        val current = history.find { it.id == result.id } ?: lastResult?.takeIf { it.id == result.id } ?: return
        if (current.gameMode != GameMode.REWARDS || current.prizeType == null) return
        rewardRequestVersion++
        rewardError = null
        rewardDialogOverlay = overlay
        rewardResult = current
    }

    fun dismissReward() {
        rewardRequestVersion++
        rewardResult = null
        rewardDialogOverlay = null
        rewardError = null
    }

    private fun dismissOverlayReward() {
        if (rewardDialogOverlay != null) dismissReward()
    }

    fun claimReward() {
        val target = rewardResult ?: return
        if (claimingReward || !rewardDialogVisible || target.gameMode != GameMode.REWARDS ||
            target.prizeType == null || target.prize != null) return
        val pending = pendingResult?.takeIf { it.result === target }
        val request = rewardRequestVersion
        claimingReward = true
        rewardError = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    if (pending != null && !pending.saved) persistResult(pending)
                    val id = pending?.result?.id ?: target.id
                    val snapshot = withContext(ioDispatcher) { store.claimReward(id) }
                    applySnapshot(snapshot)
                    val claimed = snapshot.history.find { it.id == id }
                    if (claimed?.prize != null) {
                        if ((pending != null && pendingResult === pending && lastResult != null) ||
                            (pending == null && lastResult?.id == id && pendingResult?.saved != false)) lastResult = claimed
                        if (historyDetail?.id == id) historyDetail = claimed
                        if (pendingResult?.saved == true && pendingResult?.result?.id == id) pendingResult?.result = claimed
                        if (rewardRequestVersion == request) rewardResult = claimed
                    } else if (rewardRequestVersion == request) {
                        rewardError = "This prize is no longer available in practice history."
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                if (rewardRequestVersion == request) rewardError = "Unable to save your prize. Free device storage and try again."
            } finally {
                claimingReward = false
            }
        }
    }

    fun useReward(type: RewardType) {
        if (usingReward || overlay != Overlay.REWARDS || (rewardBalances[type]?.whole ?: 0) == 0) return
        usingReward = true
        rewardUseError = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    applySnapshot(withContext(ioDispatcher) { store.useReward(type) })
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                rewardUseError = "Unable to use this reward. Free device storage and try again."
            } finally {
                usingReward = false
            }
        }
    }

    fun removeReward(type: RewardType, fragment: Boolean) {
        val balance = rewardBalances[type] ?: return
        if (removingReward || overlay != Overlay.SETTINGS ||
            (if (fragment) balance.fragments else balance.whole) == 0) return
        removingReward = true
        rewardRemovalError = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    applySnapshot(withContext(ioDispatcher) { store.removeReward(type, fragment) })
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                rewardRemovalError = "Unable to remove this reward. Free device storage and try again."
            } finally {
                removingReward = false
            }
        }
    }

    private suspend fun persistResult(pending: PendingResult) {
        if (pending.saved) return
        val saved = withContext(ioDispatcher) { store.addNewResult(pending.result) }
        val previous = pending.result
        pending.result = saved.result
        pending.saved = true
        latestResultId = maxOf(latestResultId, saved.result.id)
        applySnapshot(saved.snapshot)
        if (lastResult === previous) lastResult = saved.result
        if (rewardResult === previous) rewardResult = saved.result
    }

    private fun applySnapshot(snapshot: PracticeSnapshot) {
        history = snapshot.history
        rewardBalances = snapshot.rewards
        pokemons = snapshot.pokemons
        collectionLoaded = true
        latestResultId = maxOf(latestResultId, snapshot.lastResultId, snapshot.history.maxOfOrNull { it.id } ?: 0L)
    }

    fun deleteResult(id: Long) {
        historyDetail = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    withContext(ioDispatcher) { store.delete(id) }
                    applySnapshot(withContext(ioDispatcher) { store.loadSnapshot() })
                    removeDeletedResultReward(id)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                message = "Unable to delete this result."
            }
        }
    }

    fun clearHistory() {
        historyDetail = null
        val request = rewardRequestVersion
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    withContext(ioDispatcher) { store.clear() }
                    applySnapshot(withContext(ioDispatcher) { store.loadSnapshot() })
                    lastResult?.takeIf { pendingResult?.saved != false }?.let { removeDeletedResultReward(it.id) }
                    if (rewardRequestVersion == request) dismissReward()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                message = "Unable to clear practice history."
            }
        }
    }

    private fun removeDeletedResultReward(id: Long) {
        if (lastResult?.id == id) {
            lastResult = lastResult?.copy(prizeType = null, prize = null, pokemonReward = null)
            dismissCelebration()
        }
        pendingResult?.takeIf { it.saved && it.result.id == id }?.let {
            it.result = it.result.copy(prizeType = null, prize = null, pokemonReward = null)
        }
        if (rewardResult?.id == id) dismissReward()
    }

    private suspend fun reloadHistory() {
        try {
            storeLock.withLock { applySnapshot(withContext(ioDispatcher) { store.loadSnapshot() }) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
            message = "Unable to load rewards and practice history."
        }
    }

    fun consumeMessage() { message = null }

    fun chooseShowTimer(value: Boolean) {
        showTimer = value
        settings.edit().putBoolean("show_timer", value).apply()
    }

    fun chooseRewardsEnabled(value: Boolean) {
        rewardsEnabled = value
        settings.edit().putBoolean("rewards_enabled", value).apply()
    }

    fun chooseRewardEnabled(type: RewardType, enabled: Boolean) {
        disabledRewards = if (enabled) disabledRewards - type else disabledRewards + type
        settings.edit().putStringSet("disabled_rewards", disabledRewards.map { it.name }.toSet()).apply()
    }

    fun chooseTimeLimitMinutes(value: Int) {
        timeLimitMinutes = normalizeTimeLimit(value)
        settings.edit().putInt("time_limit_minutes", timeLimitMinutes).apply()
    }

    private fun normalizeTimeLimit(value: Int): Int = value.coerceIn(0, 60) / 5 * 5

    fun chooseShowCorrectAnswers(value: Boolean) {
        showCorrectAnswers = value
        settings.edit().putBoolean("show_correct_answers", value).apply()
    }

    fun chooseVerticalEquations(value: Boolean) {
        verticalEquations = value
        settings.edit().putBoolean("vertical_equations", value).apply()
    }

    fun chooseTextSize(value: Int) {
        textSizePercent = value.coerceIn(MIN_TEXT_SIZE_PERCENT, MAX_TEXT_SIZE_PERCENT)
        settings.edit().putInt("text_size_percent", textSizePercent).apply()
    }

    fun chooseWaveAppearance(enabled: Boolean) {
        waveAppearance = enabled
        settings.edit().putBoolean("wave_appearance", enabled).apply()
    }

    fun chooseTheme(value: String) {
        theme = colorThemeFromPreference(value)
        settings.edit().putString("theme", theme).apply()
    }

    fun chooseDark(dark: Boolean) {
        mode = if (dark) "dark" else "light"
        settings.edit().putString("mode", mode).apply()
    }

    fun chooseSkins(enabled: Boolean) {
        skinsEnabled = enabled
        settings.edit().putBoolean("skins_enabled", enabled).apply()
    }

    fun chooseSkin(value: AppSkin) {
        skin = value
        settings.edit().putString("skin", value.name).apply()
    }
}
