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

enum class Screen { SETUP, READY, PLAYING, RESULTS }

enum class SetupDialog { PRACTICE, REWARDS, LOGIC }

enum class Overlay { SETTINGS, REWARDS_SETUP, HISTORY, SUBJECT_HISTORY, REWARDS, POKEMONS, READING, READING_PROGRESS }

enum class Subject(val label: String, val rewardsPreference: String) {
    MATH("Math", "rewards_enabled"),
    READING("Reading", "reading_rewards_enabled"),
    SCIENCE("Science", "science_rewards_enabled"),
    ADVANCED("Advanced", "advanced_rewards_enabled")
}

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
    var mathGamesOpen by mutableStateOf(false)
        private set
    var setupDialog by mutableStateOf<SetupDialog?>(null)
        private set
    var gameMode by mutableStateOf(GameMode.PRACTICE)
        private set
    private data class ReadyGame(val mode: GameMode, val operation: Operation, val parameters: GameParameters,
        val isMultiplicationMatrix: Boolean = false, val isLogicMath: Boolean = false)
    private var readyGame by mutableStateOf<ReadyGame?>(null)
    val readyOperation: Operation? get() = readyGame?.operation
    val readyParameters: GameParameters? get() = readyGame?.parameters
    val readyMultiplicationMatrix: Boolean get() = readyGame?.isMultiplicationMatrix == true
    val readyLogicMath: Boolean get() = readyGame?.isLogicMath == true
    val currentMathRewardGame: MathRewardGame
        get() = when {
            readyMultiplicationMatrix || game?.isMultiplicationMatrix == true -> MathRewardGame.MATRIX
            setupDialog == SetupDialog.LOGIC || readyLogicMath || game?.logicSettings != null -> MathRewardGame.LOGIC
            else -> MathRewardGame.BASIC
        }
    val currentGameRewardsEnabled: Boolean get() = rewardsEnabled(currentMathRewardGame)
    val readyTimeLimitMs: Long?
        get() {
            val ready = readyGame ?: return null
            val count = ready.parameters.questionCount ?: return null
            return if (ready.mode == GameMode.REWARDS && currentGameRewardsEnabled && showTimer && timeLimitSeconds > 0)
                timeLimitSeconds * 1_000L * count / 5 else null
        }
    var overlay by mutableStateOf<Overlay?>(null)
        private set
    private var collectionReturnOverlay: Overlay? = null
    private var settingsReturnOverlay: Overlay? = null
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
    var rewardsQuestionCountText by mutableStateOf("")
        private set
    var rewardsDefaults by mutableStateOf(loadRewardsDefaults())
        private set
    var logicRewardsDefaults by mutableStateOf(loadLogicRewardsDefaults())
        private set
    var rewardsSetupSubject by mutableStateOf(MathRewardGame.BASIC)
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
    private var queuedCelebrations: List<Celebration> = emptyList()
    val hasNextCelebration: Boolean get() = queuedCelebrations.isNotEmpty()
    var selectingCelebration by mutableStateOf(false)
        private set
    var earlyFinishMessage by mutableStateOf<String?>(null)
        private set
    var history by mutableStateOf<List<PracticeResult>>(emptyList())
        private set
    var historyDetail by mutableStateOf<PracticeResult?>(null)
        private set
    var historySubject by mutableStateOf(Subject.MATH)
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
    var readingRewardResults by mutableStateOf<List<ReadingRewardResult>>(emptyList())
        private set
    var readingRewardResult by mutableStateOf<ReadingRewardResult?>(null)
        private set
    var readingCelebration by mutableStateOf<Celebration?>(null)
        private set
    var readingSelectingCelebration by mutableStateOf(false)
        private set
    var claimingReadingReward by mutableStateOf(false)
        private set
    var readingRewardError by mutableStateOf<String?>(null)
        private set
    private var readingRewardOverlay by mutableStateOf<Overlay?>(null)
    val readingRewardDialogVisible: Boolean get() = readingRewardResult != null && readingCelebration == null &&
        !readingSelectingCelebration && overlay == readingRewardOverlay
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
    var pauseInBackground by mutableStateOf(settings.getBoolean("math_pause_in_background", true))
        private set
    private var subjectRewards by mutableStateOf(Subject.entries.associateWith {
        settings.getBoolean(it.rewardsPreference, true)
    })
    val rewardsEnabled: Boolean get() = rewardsEnabled(Subject.MATH)
    fun rewardsEnabled(subject: Subject): Boolean = subjectRewards.getValue(subject)
    var disabledMathGameRewards by mutableStateOf(MathRewardGame.entries.filter {
        it.name in settings.getStringSet("disabled_math_game_rewards", emptySet()).orEmpty()
    }.toSet())
        private set
    var disabledReadingGameRewards by mutableStateOf(ReadingGame.entries.filter {
        it.name in settings.getStringSet("disabled_reading_game_rewards", emptySet()).orEmpty()
    }.toSet())
        private set
    fun rewardsEnabled(game: MathRewardGame): Boolean = rewardsEnabled && game !in disabledMathGameRewards
    fun rewardsEnabled(game: ReadingGame): Boolean =
        rewardsEnabled(Subject.READING) && game !in disabledReadingGameRewards
    var rewardThresholds by mutableStateOf(settings.readRewardThresholds())
        private set
    var matrixRewardTier by mutableStateOf(settings.readMatrixRewardTier())
        private set
    var disabledRewards by mutableStateOf(RewardType.entries.filter {
        it.name in settings.getStringSet("disabled_rewards", emptySet()).orEmpty()
    }.toSet())
        private set
    var disabledReadingGames by mutableStateOf(ReadingGame.entries.filter {
        it.name in settings.getStringSet("disabled_reading_games", emptySet()).orEmpty()
    }.toSet())
        private set
    var timeLimitSeconds by mutableStateOf(settings.readTimeLimitSeconds())
        private set
    var readingTimeLimitSeconds by mutableStateOf(settings.readReadingTimeLimitSeconds())
        private set
    var scienceTimeLimitSeconds by mutableStateOf(normalizeTimeLimitSeconds(settings.getInt("science_time_limit_seconds", 0)))
        private set
    var advancedTimeLimitSeconds by mutableStateOf(normalizeTimeLimitSeconds(settings.getInt("advanced_time_limit_seconds", 0)))
        private set
    var activeTimeLimitMs by mutableStateOf<Long?>(null)
        private set
    var showCorrectAnswers by mutableStateOf(settings.getBoolean("show_correct_answers", true))
        private set
    var verticalEquations by mutableStateOf(settings.getBoolean("vertical_equations", true))
        private set
    var randomizeMatrixQuestions by mutableStateOf(settings.getBoolean("randomize_matrix_questions", false))
        private set
    var matrixSetupRandomize by mutableStateOf(randomizeMatrixQuestions)
        private set
    var matrixSetupRewardTier by mutableStateOf(matrixRewardTier)
        private set
    var logicMathSettings by mutableStateOf(loadLogicMathSettings())
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
    var skinsEnabled by mutableStateOf(settings.getBoolean("skins_enabled", true))
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
    private class PendingReadingResult(var result: ReadingRewardResult, var saved: Boolean = false)
    private var pendingReadingResult: PendingReadingResult? = null
    private var readingRewardRequestVersion = 0L
    private var readingCelebrationDismissed = false
    private val openedReadingSessions = mutableSetOf<String>()

    val maximum: Int? get() = parseMaximum(maximumText)
    val minimum: Int? get() = parseMaximum(minimumText)?.takeIf { it <= (maximum ?: MAX_MAXIMUM) }
    val divisionMaximumFirst: Int? get() = parseMaximum(divisionMaximumFirstText)
    val divisionMaximumSecond: Int? get() = parseMaximum(divisionMaximumSecondText)
    val questionCount: Int? get() = parseQuestionCount(questionCountText)
    val rewardsQuestionCount: Int? get() = parseQuestionCount(rewardsQuestionCountText)
    val canSubmitSetup: Boolean get() = selectedOperation != null && questionCount != null &&
        if (selectedOperation == Operation.DIVISION) divisionMaximumFirst != null && divisionMaximumSecond != null
        else minimum != null && maximum != null
    val canSaveRewardsSetup: Boolean get() = rewardsSetupSubject == MathRewardGame.MATRIX ||
        Operation.entries.all { rewardsSetupDraft[it]?.isValid(it) == true }
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

    private fun loadRewardsDefaults(): Map<Operation, RewardsGameDefaults> {
        val defaults = Operation.entries.associateWith { operation ->
            val legacy = if (operation == Operation.DIVISION)
                RewardsGameDefaults(divisionMaximumFirstText, divisionMaximumSecondText)
            else RewardsGameDefaults(minimumText, maximumText)
            val fallback = legacy.takeIf { it.isValid(operation) } ?: RewardsGameDefaults.defaults(operation)
            val prefix = "rewards_${operation.name}_"
            RewardsGameDefaults(
                settings.getInt("${prefix}first", requireNotNull(fallback.first)).toString(),
                settings.getInt("${prefix}second", requireNotNull(fallback.second)).toString()
            ).takeIf { it.isValid(operation) } ?: fallback
        }
        // Freeze the migration now so future practice edits cannot change unsaved rewards defaults.
        persistRewardsDefaults(defaults)
        return defaults
    }

    private fun loadLogicRewardsDefaults(): Map<Operation, RewardsGameDefaults> {
        val defaults = Operation.entries.associateWith { operation ->
            val fallback = rewardsDefaults.getValue(operation)
            val prefix = "logic_rewards_${operation.name}_"
            RewardsGameDefaults(
                settings.getInt("${prefix}first", requireNotNull(fallback.first)).toString(),
                settings.getInt("${prefix}second", requireNotNull(fallback.second)).toString()
            ).takeIf { it.isValid(operation) } ?: fallback
        }
        // Snapshot the formerly shared defaults before either subject can be edited.
        persistRewardsDefaults(defaults, MathRewardGame.LOGIC)
        return defaults
    }

    private fun persistRewardsDefaults(
        defaults: Map<Operation, RewardsGameDefaults>,
        subject: MathRewardGame = MathRewardGame.BASIC
    ) {
        val edit = settings.edit()
        defaults.forEach { (operation, parameters) ->
            val prefix = if (subject == MathRewardGame.LOGIC) "logic_rewards_${operation.name}_"
                else "rewards_${operation.name}_"
            edit.putInt("${prefix}first", requireNotNull(parameters.first))
                .putInt("${prefix}second", requireNotNull(parameters.second))
                .remove("${prefix}question_count")
        }
        edit.apply()
    }

    private fun loadLogicMathSettings(): LogicMathSettings = try {
        val defaults = LogicMathSettings()
        LogicMathSettings(
            settings.getInt("logic_minimum_pictures", defaults.minimumPictures),
            settings.getInt("logic_maximum_pictures", defaults.maximumPictures),
            settings.getInt("logic_minimum_length", defaults.minimumLength),
            settings.getInt("logic_maximum_length", defaults.maximumLength)
        ).takeIf { it.isValid } ?: defaults
    } catch (_: ClassCastException) {
        LogicMathSettings()
    }

    fun chooseLogicMathSettings(value: LogicMathSettings) {
        if (!value.isValid) return
        logicMathSettings = value
        settings.edit()
            .putInt("logic_minimum_pictures", value.minimumPictures)
            .putInt("logic_maximum_pictures", value.maximumPictures)
            .putInt("logic_minimum_length", value.minimumLength)
            .putInt("logic_maximum_length", value.maximumLength).apply()
    }

    fun openMathGames() {
        if (screen == Screen.SETUP && overlay == null) mathGamesOpen = true
    }

    fun closeMathGames() {
        if (screen != Screen.SETUP || overlay != null || setupDialog != null) return
        mathGamesOpen = false
    }

    fun openPracticeGame() {
        if (screen != Screen.SETUP || overlay != null) return
        mathGamesOpen = true
        setupDialog = SetupDialog.PRACTICE
    }

    fun openReading() {
        if (screen != Screen.SETUP || overlay != null || setupDialog != null) return
        overlay = Overlay.READING
    }

    fun openRewardsGame() {
        if (screen != Screen.SETUP || overlay != null) return
        mathGamesOpen = true
        rewardsQuestionCountText = ""
        setupDialog = SetupDialog.REWARDS
    }

    fun openLogicMathGame() {
        if (screen != Screen.SETUP || overlay != null || setupDialog != null) return
        mathGamesOpen = true
        rewardsQuestionCountText = ""
        setupDialog = SetupDialog.LOGIC
    }

    fun openMultiplicationMatrixGame() {
        if (screen != Screen.SETUP || overlay != null || setupDialog != null) return
        ready(GameMode.REWARDS, Operation.MULTIPLICATION,
            GameParameters("1", MultiplicationMatrix.SIZE.toString(), MultiplicationMatrix.QUESTION_COUNT.toString()),
            isMultiplicationMatrix = true)
    }

    fun dismissSetupDialog() { setupDialog = null }

    fun openRewardsSetup(subject: MathRewardGame = MathRewardGame.BASIC) {
        if (overlay != Overlay.SETTINGS) return
        rewardsSetupSubject = subject
        resetRewardsSetupDraft()
        overlay = Overlay.REWARDS_SETUP
    }

    private fun resetRewardsSetupDraft() {
        rewardsSetupDraft = if (rewardsSetupSubject == MathRewardGame.LOGIC) logicRewardsDefaults else rewardsDefaults
        matrixSetupRandomize = randomizeMatrixQuestions
        matrixSetupRewardTier = matrixRewardTier
    }

    fun updateRewardsParameters(operation: Operation, parameters: RewardsGameDefaults) {
        if (overlay != Overlay.REWARDS_SETUP || rewardsSetupSubject == MathRewardGame.MATRIX) return
        rewardsSetupDraft = rewardsSetupDraft + (operation to parameters)
    }

    fun updateMatrixSetupRandomize(value: Boolean) {
        if (overlay != Overlay.REWARDS_SETUP || rewardsSetupSubject != MathRewardGame.MATRIX) return
        matrixSetupRandomize = value
    }

    fun updateMatrixSetupRewardTier(value: RewardTier) {
        if (overlay != Overlay.REWARDS_SETUP || rewardsSetupSubject != MathRewardGame.MATRIX) return
        matrixSetupRewardTier = value
    }

    fun saveRewardsSetup() {
        if (overlay != Overlay.REWARDS_SETUP || !canSaveRewardsSetup) return
        if (rewardsSetupSubject == MathRewardGame.MATRIX) {
            chooseRandomizeMatrixQuestions(matrixSetupRandomize)
            chooseMatrixRewardTier(matrixSetupRewardTier)
            overlay = Overlay.SETTINGS
            return
        }
        val defaults = rewardsSetupDraft.mapValues { (_, parameters) ->
            RewardsGameDefaults(requireNotNull(parameters.first).toString(), requireNotNull(parameters.second).toString())
        }
        persistRewardsDefaults(defaults, rewardsSetupSubject)
        if (rewardsSetupSubject == MathRewardGame.LOGIC) logicRewardsDefaults = defaults
        else rewardsDefaults = defaults
        rewardsSetupDraft = defaults
        overlay = Overlay.SETTINGS
    }

    fun cancelRewardsSetup() {
        if (overlay != Overlay.REWARDS_SETUP) return
        resetRewardsSetupDraft()
        overlay = Overlay.SETTINGS
    }

    fun selectRewardsGame(operation: Operation) {
        if (screen != Screen.SETUP || overlay != null ||
            (setupDialog != SetupDialog.REWARDS && setupDialog != SetupDialog.LOGIC)) return
        val defaults = if (setupDialog == SetupDialog.LOGIC) logicRewardsDefaults else rewardsDefaults
        val parameters = defaults[operation]?.takeIf { it.isValid(operation) } ?: return
        val count = rewardsQuestionCount ?: return
        ready(GameMode.REWARDS, operation, GameParameters(parameters.firstText, parameters.secondText, count.toString()),
            isLogicMath = setupDialog == SetupDialog.LOGIC)
    }

    private fun ready(mode: GameMode, operation: Operation, parameters: GameParameters,
        isMultiplicationMatrix: Boolean = false, isLogicMath: Boolean = false) {
        mathGamesOpen = true
        gameMode = mode
        readyGame = ReadyGame(mode, operation, parameters, isMultiplicationMatrix, isLogicMath)
        setupDialog = null
        screen = Screen.READY
    }

    fun selectOperation(operation: Operation) { selectedOperation = operation }

    fun updateMinimum(text: String) { minimumText = text.take(6) }

    fun updateMaximum(text: String) { maximumText = text.filter { it in '0'..'9' }.take(6) }

    fun updateDivisionMaximumFirst(text: String) { divisionMaximumFirstText = text.take(6) }

    fun updateDivisionMaximumSecond(text: String) { divisionMaximumSecondText = text.take(6) }

    fun updateQuestionCount(text: String) { questionCountText = text.take(6) }

    fun updateRewardsQuestionCount(text: String) { rewardsQuestionCountText = text.take(6) }

    fun submitSetup() {
        if (screen != Screen.SETUP || overlay != null ||
            (setupDialog != null && setupDialog != SetupDialog.PRACTICE) || !canSubmitSetup) return
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
        if (overlay == Overlay.REWARDS_SETUP) {
            cancelRewardsSetup()
            return
        }
        val returnDialog = if (screen == Screen.READY && !readyMultiplicationMatrix)
            if (readyLogicMath) SetupDialog.LOGIC
            else if (gameMode == GameMode.PRACTICE) SetupDialog.PRACTICE else SetupDialog.REWARDS
        else null
        if (screen == Screen.PLAYING) DebugLog.event(DebugEvent.GAME_ABANDONED)
        game = null
        answerText = ""
        celebration = null
        queuedCelebrations = emptyList()
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
        game = if (ready.isMultiplicationMatrix) GameState.startMultiplicationMatrix(randomizeMatrixQuestions)
            else GameState.start(operation, maximum, generator, questionCount, minimum, maximumSecond,
                logicSettings = logicMathSettings.takeIf { ready.isLogicMath })
        progressIcon = ProgressIcon.entries.random(progressIconRandom)
        answerText = ""
        celebration = null
        queuedCelebrations = emptyList()
        selectingCelebration = false
        earlyFinishMessage = null
        lastResult = null
        pendingResult = null
        dismissReward()
        rewardsEnabledAtStart = gameMode == GameMode.REWARDS && currentGameRewardsEnabled
        activeTimeLimitMs = readyTimeLimitMs
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
            isMultiplicationMatrix = state.isMultiplicationMatrix,
            isLogicMath = state.logicSettings != null,
            rewardThresholds = rewardThresholds, matrixRewardTier = matrixRewardTier,
            prizeType = selectPrize(state.questionCount, state.correct, state.attempts.size,
                rewardsEnabledAtStart, currentGameRewardsEnabled && gameMode == GameMode.REWARDS, expired, rewardRandom,
                disabledRewards, rewardThresholds, matrixRewardTier.takeIf { state.isMultiplicationMatrix }))
        val pending = PendingResult(result)
        pendingResult = pending
        lastResult = result
        val answeredAllQuestions = !expired && state.attempts.size == state.questionCount
        celebration = null
        queuedCelebrations = emptyList()
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
                        val selected = if (selectingCelebration && collectionLoaded) buildList {
                            Celebration.select(state.questionCount, celebrationRandom, pokemons, result.gameMode,
                                isMultiplicationMatrix = result.isMultiplicationMatrix)?.let { add(it) }
                            if (result.isMultiplicationMatrix) {
                                Celebration.select(state.questionCount, celebrationRandom, pokemons, result.gameMode)
                                    ?.let { add(it) }
                            }
                        } else emptyList()
                        celebration = selected.firstOrNull()
                        queuedCelebrations = selected.drop(1)
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
        queuedCelebrations = emptyList()
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
        celebration = queuedCelebrations.firstOrNull()
        queuedCelebrations = queuedCelebrations.drop(1)
        selectingCelebration = false
    }

    fun collectPresentedCelebration(presented: Celebration) {
        val pending = pendingResult ?: return
        if (celebration != presented || overlay != null || presented in pending.result.pokemonRewards) return
        if (!presented.isEligibleFor(pending.result.questionCount, pending.result.gameMode,
                pending.result.isMultiplicationMatrix)) return
        collectCelebrations(listOf(presented), pending)
    }

    fun collectAllCelebrations() { collectCelebrations(Celebration.entries) }

    private fun collectCelebrations(celebrations: Collection<Celebration>, pending: PendingResult? = null) {
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    if (pending != null && !pending.saved) persistResult(pending)
                    val snapshot = withContext(ioDispatcher) {
                        if (pending == null) store.collectPokemons(celebrations, includeMegaPokemons = true)
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
        if (!timerShouldRun || elapsedMs() < limit) return false
        finish(current, timedOut = true)
        return true
    }

    private fun resetTimer() {
        elapsedBefore = 0
        runningSince = null
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private val timerShouldRun: Boolean
        get() = screen == Screen.PLAYING && overlay == null && (foreground || !pauseInBackground)

    /** Overlays always pause the timer; background time follows the Math setting. */
    private fun updateTimer() {
        val shouldRun = timerShouldRun
        val since = runningSince
        if (shouldRun && since == null) runningSince = clock()
        else if (!shouldRun && since != null) {
            elapsedBefore += (clock() - since).coerceAtLeast(0)
            runningSince = null
        }
        if (!shouldRun || activeTimeLimitMs == null) {
            timeoutJob?.cancel()
            timeoutJob = null
        } else if (checkTimeLimit()) {
            return
        } else if (activeTimeLimitMs != null && timeoutJob == null) {
            timeoutJob = viewModelScope.launch {
                while (timerShouldRun) {
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
        if (overlay == null || overlay == Overlay.READING) settingsReturnOverlay = overlay
        overlay = Overlay.SETTINGS
        updateTimer()
    }

    fun openReadingProgress() {
        if (overlay != Overlay.SETTINGS) return
        overlay = Overlay.READING_PROGRESS
        updateTimer()
    }

    fun openReadingFromProgress() {
        if (overlay != Overlay.READING_PROGRESS) return
        overlay = Overlay.READING
        updateTimer()
    }

    fun openHistory() {
        dismissOverlayReward()
        historyDetail = null
        historySubject = Subject.MATH
        overlay = Overlay.HISTORY
        updateTimer()
        viewModelScope.launch { reloadHistory() }
    }

    fun openSubjectHistory(subject: Subject) {
        if (subject == Subject.MATH) {
            openHistory()
            return
        }
        dismissOverlayReward()
        historySubject = subject
        overlay = Overlay.SUBJECT_HISTORY
        updateTimer()
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
        if (overlay == Overlay.REWARDS_SETUP) {
            cancelRewardsSetup()
            return
        }
        dismissOverlayReward()
        if (overlay == Overlay.HISTORY && historyDetail != null) {
            historyDetail = null
            return
        }
        overlay = when (overlay) {
            Overlay.HISTORY, Overlay.SUBJECT_HISTORY, Overlay.READING_PROGRESS -> Overlay.SETTINGS
            Overlay.SETTINGS -> settingsReturnOverlay
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
        dismissReadingSessionReward()
    }

    fun openReadingSessionReward(data: ReadingData, session: ReadingSession) {
        if (overlay != Overlay.READING || session.id in openedReadingSessions) return
        prepareReadingReward(data, session, automatic = true)
    }

    fun showReadingRewardForSession(data: ReadingData, session: ReadingSession) {
        if (overlay != Overlay.READING && !(overlay == Overlay.SUBJECT_HISTORY && historySubject == Subject.READING)) return
        prepareReadingReward(data, session, automatic = false)
    }

    private fun prepareReadingReward(data: ReadingData, session: ReadingSession, automatic: Boolean) {
        if (session.id in deletedReadingSessionIds || session.id in data.deletedSessionIds) return
        val current = data.sessions.find { it.id == session.id } ?: return
        val finishedAt = current.finishedAt ?: return
        if (current.abandoned || current.attemptIds.isEmpty()) return
        val result = ReadingRewardResult(current.id, current.ownerId, current.game, current.level, finishedAt,
            current.attemptIds.size, data.sessionCorrect(current), data.sessionAnswered(current),
            current.timedOut, current.rewardsEnabledAtStart, current.prizeType,
            rewardThresholds = current.rewardThresholds)
        val pending = PendingReadingResult(result)
        pendingReadingResult = pending
        openedReadingSessions += current.id
        val request = ++readingRewardRequestVersion
        readingRewardOverlay = overlay
        readingCelebrationDismissed = !automatic
        readingCelebration = null
        readingSelectingCelebration = true
        readingRewardResult = null
        readingRewardError = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    val created = persistReadingResult(pending)
                    if (request == readingRewardRequestVersion && overlay == readingRewardOverlay) {
                        readingRewardResult = pending.result.takeIf {
                            pending.saved && it.prizeType != null && (!automatic || created)
                        }
                        readingCelebration = pending.result.celebration.takeIf {
                            created && !readingCelebrationDismissed
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                openedReadingSessions -= current.id
                if (request == readingRewardRequestVersion) {
                    readingRewardError = "Unable to save your reading rewards. Free device storage and try again."
                    readingRewardResult = result.takeIf { it.prizeType != null }
                }
            } finally {
                if (request == readingRewardRequestVersion) readingSelectingCelebration = false
            }
        }
    }

    /** Returns true only for the first durable receipt, not a resumed or retained session. */
    private suspend fun persistReadingResult(pending: PendingReadingResult): Boolean {
        if (pending.result.sessionId in deletedReadingSessionIds) return false
        if (pending.saved) return false
        var snapshot = withContext(ioDispatcher) { store.loadSnapshot() }
        val id = pending.result.sessionId
        val existing = snapshot.readingResults.find { it.sessionId == id }
        val created = existing == null && id !in snapshot.recordedReadingSessionIds
        if (created) {
            val selected = if (pending.result.completed)
                Celebration.select(pending.result.questionCount, celebrationRandom, snapshot.pokemons, GameMode.REWARDS)
            else null
            snapshot = withContext(ioDispatcher) {
                store.addReadingResult(pending.result.copy(celebration = selected))
            }
        }
        snapshot.readingResults.find { it.sessionId == id }?.let {
            pending.result = it
            pending.saved = true
        }
        applySnapshot(snapshot)
        return created
    }

    fun dismissReadingCelebration() {
        readingCelebrationDismissed = true
        readingCelebration = null
    }

    fun dismissReadingReward() {
        readingRewardRequestVersion++
        readingRewardResult = null
        readingRewardError = null
        readingSelectingCelebration = false
    }

    fun dismissReadingSessionReward() {
        dismissReadingCelebration()
        dismissReadingReward()
        readingRewardOverlay = null
        pendingReadingResult = null
    }

    fun collectPresentedReadingCelebration(presented: Celebration) {
        val pending = pendingReadingResult ?: return
        if (!foreground || overlay != Overlay.READING || overlay != readingRewardOverlay ||
            readingSelectingCelebration || readingCelebration != presented || !pending.saved ||
            pending.result.pokemonReward != null) return
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    val snapshot = withContext(ioDispatcher) {
                        store.collectReadingCelebration(pending.result.sessionId, presented)
                    }
                    applySnapshot(snapshot)
                    snapshot.readingResults.find { it.sessionId == pending.result.sessionId }?.let {
                        pending.result = it
                        if (readingRewardResult?.sessionId == it.sessionId) readingRewardResult = it
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

    fun claimReadingReward() {
        val target = readingRewardResult ?: return
        if (claimingReadingReward || !readingRewardDialogVisible || target.prizeType == null || target.prize != null) return
        val pending = pendingReadingResult?.takeIf { it.result.sessionId == target.sessionId }
        val request = readingRewardRequestVersion
        claimingReadingReward = true
        readingRewardError = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    if (pending != null && !pending.saved) persistReadingResult(pending)
                    val snapshot = withContext(ioDispatcher) { store.claimReadingReward(target.sessionId) }
                    applySnapshot(snapshot)
                    val claimed = snapshot.readingResults.find { it.sessionId == target.sessionId }
                    if (claimed?.prize != null) {
                        if (pending != null) pending.result = claimed
                        if (request == readingRewardRequestVersion) readingRewardResult = claimed
                    } else if (request == readingRewardRequestVersion) {
                        readingRewardError = "This prize is no longer available in reading history."
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                if (request == readingRewardRequestVersion)
                    readingRewardError = "Unable to save your prize. Free device storage and try again."
            } finally {
                claimingReadingReward = false
            }
        }
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

    fun addReward(type: RewardType, fragment: Boolean) = adjustReward(type, fragment, add = true)

    fun removeReward(type: RewardType, fragment: Boolean) = adjustReward(type, fragment, add = false)

    private fun adjustReward(type: RewardType, fragment: Boolean, add: Boolean) {
        val balance = rewardBalances[type] ?: RewardBalance()
        if (removingReward || overlay != Overlay.SETTINGS ||
            (!add && (if (fragment) balance.fragments else balance.whole) == 0)) return
        removingReward = true
        rewardRemovalError = null
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    applySnapshot(withContext(ioDispatcher) {
                        if (add) store.addReward(type, fragment) else store.removeReward(type, fragment)
                    })
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                rewardRemovalError = if (add)
                    "Unable to add this reward. Check the reward balance limit and device storage, then try again."
                else "Unable to remove this reward. Free device storage and try again."
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
        readingRewardResults = snapshot.readingResults
        rewardBalances = snapshot.rewards
        pokemons = snapshot.pokemons
        collectionLoaded = true
        latestResultId = maxOf(latestResultId, snapshot.lastResultId, snapshot.history.maxOfOrNull { it.id } ?: 0L)
    }

    var readingHistoryError by mutableStateOf<String?>(null)
        private set
    private var deletedReadingSessionIds = emptySet<String>()

    fun deleteReadingResults(sessionIds: Set<String>) {
        if (sessionIds.isEmpty()) return
        deletedReadingSessionIds = deletedReadingSessionIds + sessionIds
        if (pendingReadingResult?.result?.sessionId in sessionIds ||
            readingRewardResult?.sessionId in sessionIds) dismissReadingSessionReward()
        viewModelScope.launch {
            try {
                storeLock.withLock {
                    applySnapshot(withContext(ioDispatcher) { store.deleteReadingResults(sessionIds) })
                    readingHistoryError = null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                DebugLog.event(DebugEvent.HISTORY_FAILURE, error = error)
                readingHistoryError = "Reading history was deleted, but its rewards could not be removed. Retry to finish."
                message = readingHistoryError
            }
        }
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
            lastResult = lastResult?.copy(prizeType = null, prize = null, pokemonReward = null, megaPokemonReward = null)
            queuedCelebrations = emptyList()
            dismissCelebration()
        }
        pendingResult?.takeIf { it.saved && it.result.id == id }?.let {
            it.result = it.result.copy(prizeType = null, prize = null, pokemonReward = null, megaPokemonReward = null)
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

    fun choosePauseInBackground(value: Boolean) {
        pauseInBackground = value
        settings.edit().putBoolean("math_pause_in_background", value).apply()
        updateTimer()
    }

    fun chooseRewardsEnabled(value: Boolean) {
        chooseSubjectRewardsEnabled(Subject.MATH, value)
    }

    fun chooseSubjectRewardsEnabled(subject: Subject, value: Boolean) {
        subjectRewards = subjectRewards + (subject to value)
        settings.edit().putBoolean(subject.rewardsPreference, value).apply()
        if (subject == Subject.MATH && !value) {
            activeTimeLimitMs = null
            updateTimer()
        }
    }

    fun chooseMathGameRewardsEnabled(game: MathRewardGame, enabled: Boolean) {
        disabledMathGameRewards = if (enabled) disabledMathGameRewards - game else disabledMathGameRewards + game
        settings.edit().putStringSet("disabled_math_game_rewards", disabledMathGameRewards.map { it.name }.toSet()).apply()
        if (game == currentMathRewardGame && !enabled) {
            activeTimeLimitMs = null
            updateTimer()
        }
    }

    fun chooseReadingGameRewardsEnabled(game: ReadingGame, enabled: Boolean) {
        disabledReadingGameRewards = if (enabled) disabledReadingGameRewards - game else disabledReadingGameRewards + game
        settings.edit().putStringSet("disabled_reading_game_rewards", disabledReadingGameRewards.map { it.name }.toSet()).apply()
    }

    fun chooseRewardEnabled(type: RewardType, enabled: Boolean) {
        disabledRewards = if (enabled) disabledRewards - type else disabledRewards + type
        settings.edit().putStringSet("disabled_rewards", disabledRewards.map { it.name }.toSet()).apply()
    }

    fun chooseRewardThresholds(tier1Minimum: Int, tier2Minimum: Int) {
        if (tier1Minimum !in 1..1000 || tier2Minimum !in 1..1000 || tier1Minimum >= tier2Minimum) return
        if (rewardThresholds != RewardThresholds(tier1Minimum, tier2Minimum)) rewardsQuestionCountText = ""
        rewardThresholds = RewardThresholds(tier1Minimum, tier2Minimum)
        settings.edit().putInt("reward_tier_1_minimum", tier1Minimum)
            .putInt("reward_tier_2_minimum", tier2Minimum).apply()
    }

    fun chooseMatrixRewardTier(tier: RewardTier) {
        matrixRewardTier = tier
        settings.edit().putString("matrix_reward_tier", tier.name).apply()
    }

    fun chooseReadingGameEnabled(game: ReadingGame, enabled: Boolean) {
        disabledReadingGames = if (enabled) disabledReadingGames - game else disabledReadingGames + game
        settings.edit().putStringSet("disabled_reading_games", disabledReadingGames.map { it.name }.toSet()).apply()
    }

    fun chooseTimeLimitSeconds(value: Int) {
        timeLimitSeconds = normalizeTimeLimitSeconds(value)
        settings.edit().putInt("time_limit_seconds", timeLimitSeconds).apply()
    }

    fun chooseReadingTimeLimitSeconds(value: Int) {
        readingTimeLimitSeconds = normalizeTimeLimitSeconds(value)
        settings.edit().putInt("reading_time_limit_seconds", readingTimeLimitSeconds).apply()
    }

    fun chooseScienceTimeLimitSeconds(value: Int) {
        scienceTimeLimitSeconds = normalizeTimeLimitSeconds(value)
        settings.edit().putInt("science_time_limit_seconds", scienceTimeLimitSeconds).apply()
    }

    fun chooseAdvancedTimeLimitSeconds(value: Int) {
        advancedTimeLimitSeconds = normalizeTimeLimitSeconds(value)
        settings.edit().putInt("advanced_time_limit_seconds", advancedTimeLimitSeconds).apply()
    }

    fun chooseShowCorrectAnswers(value: Boolean) {
        showCorrectAnswers = value
        settings.edit().putBoolean("show_correct_answers", value).apply()
    }

    fun chooseVerticalEquations(value: Boolean) {
        verticalEquations = value
        settings.edit().putBoolean("vertical_equations", value).apply()
    }

    fun chooseRandomizeMatrixQuestions(value: Boolean) {
        randomizeMatrixQuestions = value
        settings.edit().putBoolean("randomize_matrix_questions", value).apply()
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
