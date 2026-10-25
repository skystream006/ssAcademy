package com.ssmath.app

import kotlin.random.Random
import kotlinx.serialization.Serializable

const val MIN_MAXIMUM = 1
const val MAX_MAXIMUM = 10_000
const val MIN_QUESTION_COUNT = 1
const val MAX_QUESTION_COUNT = 1_000
const val DEFAULT_QUESTION_COUNT = 10
private const val MAX_ANSWER_DIGITS = 9

@Serializable
enum class GameMode { PRACTICE, REWARDS }

data class RewardsGameDefaults(
    val firstText: String = "1",
    val secondText: String = "10"
) {
    val first: Int? get() = parseMaximum(firstText)
    val second: Int? get() = parseMaximum(secondText)

    fun isValid(operation: Operation, isLogicMath: Boolean = false): Boolean {
        val first = first ?: return false
        val second = second ?: return false
        return operation.usesIndependentMaximums(isLogicMath) || first <= second
    }

    companion object {
        fun defaults(operation: Operation, isLogicMath: Boolean = false) = RewardsGameDefaults(
            firstText = if (operation.usesIndependentMaximums(isLogicMath)) "10" else "1"
        )
    }
}

data class GameParameters(
    val firstText: String = "1",
    val secondText: String = "10",
    val questionCountText: String = DEFAULT_QUESTION_COUNT.toString()
) {
    val first: Int? get() = parseMaximum(firstText)
    val second: Int? get() = parseMaximum(secondText)
    val questionCount: Int? get() = parseQuestionCount(questionCountText)

    fun isValid(operation: Operation): Boolean =
        questionCount != null && RewardsGameDefaults(firstText, secondText).isValid(operation)

    companion object {
        fun defaults(operation: Operation) = GameParameters(
            firstText = if (operation.usesIndependentMaximums()) "10" else "1"
        )
    }
}

@Serializable
enum class Operation(val label: String, val symbol: String) {
    ADDITION("Addition", "+"),
    SUBTRACTION("Subtraction", "−"),
    MULTIPLICATION("Multiplication", "×"),
    DIVISION("Division", "÷");

    fun usesIndependentMaximums(isLogicMath: Boolean = false): Boolean =
        this == DIVISION || (this == MULTIPLICATION && !isLogicMath)

    fun apply(left: Int, right: Int): Int = when (this) {
        ADDITION -> left + right
        SUBTRACTION -> left - right
        MULTIPLICATION -> left * right
        DIVISION -> left / right
    }

    companion object {
        fun fromPreference(value: String?): Operation? = entries.find { it.name == value }
    }
}

@Serializable
data class Problem(val left: Int, val right: Int, val operation: Operation, val logicPuzzle: LogicPuzzle? = null) {
    val answer: Int get() = logicPuzzle?.answer ?: operation.apply(left, right)
    val text: String get() = logicPuzzle?.text ?: "$left ${operation.symbol} $right"
}

/** One answered problem. [given] is what the user entered. */
@Serializable
data class Attempt(val problem: Problem, val given: Int) {
    val correct: Boolean get() = given == problem.answer
}

object MultiplicationMatrix {
    const val SIZE = 11
    const val QUESTION_COUNT = SIZE * SIZE

    fun problemAt(index: Int): Problem {
        require(index in 0 until QUESTION_COUNT)
        return Problem(index / SIZE + 1, index % SIZE + 1, Operation.MULTIPLICATION)
    }
}

/** Accepts whole numbers from [MIN_MAXIMUM] to [MAX_MAXIMUM]; anything else is invalid. */
fun parseMaximum(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > 6 || !trimmed.all { it in '0'..'9' }) return null
    return trimmed.toInt().takeIf { it in MIN_MAXIMUM..MAX_MAXIMUM }
}

/** Accepts a whole-number session length from [MIN_QUESTION_COUNT] to [MAX_QUESTION_COUNT]. */
fun parseQuestionCount(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > 6 || !trimmed.all { it in '0'..'9' }) return null
    return trimmed.toIntOrNull()?.takeIf { it in MIN_QUESTION_COUNT..MAX_QUESTION_COUNT }
}

/** Accepts a non-negative whole-number answer. */
fun parseAnswer(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_ANSWER_DIGITS || !trimmed.all { it in '0'..'9' }) return null
    return trimmed.toIntOrNull()
}

class ProblemGenerator(private val random: Random = Random.Default) {
    fun nextLogic(operation: Operation, maximum: Int, settings: LogicMathSettings,
        minimum: Int = MIN_MAXIMUM, maximumSecond: Int = maximum): Problem =
        Problem(0, 0, operation, LogicPuzzleGenerator(random).next(operation, maximum, settings, minimum, maximumSecond))

    /**
     * Addition and subtraction use numbers between [minimum] and [maximum].
     * Multiplication bounds the first factor by [maximum] and the second by [maximumSecond].
     * Subtraction never goes below zero. Division has a whole-number answer, with the
     * dividend bounded by [maximum] and the divisor by [maximumSecond].
     */
    fun next(operation: Operation, maximum: Int, previous: Problem? = null, minimum: Int = MIN_MAXIMUM,
        maximumSecond: Int = maximum): Problem {
        require(maximum in MIN_MAXIMUM..MAX_MAXIMUM) { "Maximum must be between $MIN_MAXIMUM and $MAX_MAXIMUM." }
        require(minimum in MIN_MAXIMUM..maximum) { "Minimum must be between $MIN_MAXIMUM and the maximum." }
        require(maximumSecond in minimum..MAX_MAXIMUM) { "Second maximum must be between the minimum and $MAX_MAXIMUM." }
        var problem = create(operation, minimum, maximum, maximumSecond)
        // Avoid showing the identical problem twice in a row whenever another one exists.
        var retries = 0
        while (problem == previous && retries++ < 20) problem = create(operation, minimum, maximum, maximumSecond)
        return problem
    }

    private fun create(operation: Operation, minimum: Int, maximum: Int, maximumSecond: Int): Problem {
        fun number() = random.nextInt(minimum, maximum + 1)
        return when (operation) {
            Operation.ADDITION -> Problem(number(), number(), operation)
            Operation.MULTIPLICATION -> Problem(number(), random.nextInt(minimum, maximumSecond + 1), operation)
            Operation.SUBTRACTION -> {
                val a = number()
                val b = number()
                Problem(maxOf(a, b), minOf(a, b), operation)
            }
            Operation.DIVISION -> {
                // Each divisor is in range, so every positive multiple up to the maximum is too.
                val total = (minimum..minOf(maximum, maximumSecond)).sumOf { maximum / it }
                var pick = random.nextInt(total)
                var divisor = minimum
                while (pick >= maximum / divisor) {
                    pick -= maximum / divisor
                    divisor++
                }
                val quotient = pick + 1
                Problem(divisor * quotient, divisor, operation)
            }
        }
    }
}

data class GameState(
    val operation: Operation,
    val maximum: Int,
    val problem: Problem,
    val attempts: List<Attempt> = emptyList(),
    val questionCount: Int = DEFAULT_QUESTION_COUNT,
    val minimum: Int = MIN_MAXIMUM,
    val maximumSecond: Int = maximum,
    val isMultiplicationMatrix: Boolean = false,
    val logicSettings: LogicMathSettings? = null,
    private val matrixQuestionOrder: List<Int> = if (isMultiplicationMatrix)
        (0 until MultiplicationMatrix.QUESTION_COUNT).toList() else emptyList()
) {
    init {
        require(minimum in MIN_MAXIMUM..maximum) { "Minimum must be between $MIN_MAXIMUM and the maximum." }
        require(maximumSecond in minimum..MAX_MAXIMUM) { "Second maximum must be between the minimum and $MAX_MAXIMUM." }
        require(questionCount in MIN_QUESTION_COUNT..MAX_QUESTION_COUNT) {
            "Question count must be between $MIN_QUESTION_COUNT and $MAX_QUESTION_COUNT."
        }
        require(!isMultiplicationMatrix || (operation == Operation.MULTIPLICATION &&
            minimum == 1 && maximum == MultiplicationMatrix.SIZE && maximumSecond == MultiplicationMatrix.SIZE &&
            questionCount == MultiplicationMatrix.QUESTION_COUNT))
        require(logicSettings == null || (logicSettings.isValid && !isMultiplicationMatrix))
        require((logicSettings != null) == (problem.logicPuzzle != null))
    }

    val correct: Int get() = attempts.count { it.correct }
    val wrong: Int get() = attempts.count { !it.correct }
    val maxWrongAnswers: Int get() = questionCount / 10 + 1
    val finished: Boolean get() = attempts.size >= questionCount || wrong >= maxWrongAnswers
    val perfect: Boolean get() = attempts.size == questionCount && wrong == 0

    /** Records an answer and moves on to a new problem of the same type until the game ends. */
    fun answer(value: Int, generator: ProblemGenerator): GameState {
        check(!finished) { "The game is over." }
        val next = copy(attempts = attempts + Attempt(problem, value))
        return if (next.finished) next else next.copy(problem = when {
            isMultiplicationMatrix -> MultiplicationMatrix.problemAt(matrixQuestionOrder[next.attempts.size])
            logicSettings != null -> generator.nextLogic(operation, maximum, logicSettings, minimum, maximumSecond)
            else -> generator.next(operation, maximum, problem, minimum, maximumSecond)
        })
    }

    companion object {
        fun startMultiplicationMatrix(randomizeQuestionOrder: Boolean = false, random: Random = Random.Default): GameState {
            val order = (0 until MultiplicationMatrix.QUESTION_COUNT).toList().let {
                if (randomizeQuestionOrder) it.shuffled(random) else it
            }
            return GameState(
                Operation.MULTIPLICATION, MultiplicationMatrix.SIZE, MultiplicationMatrix.problemAt(order.first()),
                questionCount = MultiplicationMatrix.QUESTION_COUNT, isMultiplicationMatrix = true,
                matrixQuestionOrder = order)
        }

        fun start(operation: Operation, maximum: Int, generator: ProblemGenerator,
            questionCount: Int = DEFAULT_QUESTION_COUNT, minimum: Int = MIN_MAXIMUM, maximumSecond: Int = maximum,
            logicSettings: LogicMathSettings? = null) =
            GameState(operation, maximum,
                if (logicSettings != null) generator.nextLogic(operation, maximum, logicSettings, minimum, maximumSecond)
                else generator.next(operation, maximum, minimum = minimum, maximumSecond = maximumSecond),
                questionCount = questionCount, minimum = minimum, maximumSecond = maximumSecond, logicSettings = logicSettings)
    }
}

fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds.coerceAtLeast(0) / 1000)
    val hours = seconds / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, seconds / 60 % 60, seconds % 60)
    else "%02d:%02d".format(seconds / 60, seconds % 60)
}
