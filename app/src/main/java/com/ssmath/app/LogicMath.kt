package com.ssmath.app

import kotlin.random.Random
import kotlinx.serialization.Serializable

const val MIN_LOGIC_PICTURES = 2
const val MAX_LOGIC_PICTURES = 6
const val MIN_LOGIC_LENGTH = 2
const val MAX_LOGIC_LENGTH = 8
private const val MAX_LOGIC_ANSWER = 999_999_999

@Serializable
data class LogicMathSettings(
    val minimumPictures: Int = 2,
    val maximumPictures: Int = 3,
    val minimumLength: Int = 3,
    val maximumLength: Int = 4
) {
    val isValid: Boolean
        get() = minimumPictures in MIN_LOGIC_PICTURES..MAX_LOGIC_PICTURES &&
            maximumPictures in minimumPictures..MAX_LOGIC_PICTURES &&
            minimumLength in MIN_LOGIC_LENGTH..MAX_LOGIC_LENGTH &&
            maximumLength in minimumLength..MAX_LOGIC_LENGTH &&
            minimumPictures <= minimumLength && maximumPictures <= maximumLength
}

@Serializable
enum class LogicPicture(val glyph: String, val label: String) {
    APPLE("🍎", "apple"),
    STAR("⭐", "star"),
    FISH("🐟", "fish"),
    CAR("🚗", "car"),
    TREE("🌳", "tree"),
    BALL("⚽", "ball")
}

@Serializable
data class LogicExpression(
    val pictures: List<LogicPicture>,
    val operations: List<Operation>
) {
    init {
        require(pictures.isNotEmpty() && operations.size == pictures.size - 1) {
            "An expression needs one fewer operation than pictures."
        }
    }

    val text: String get() = render({ it.glyph }, { it.symbol })

    val accessibilityText: String
        get() = render({ it.label }) {
            when (it) {
                Operation.ADDITION -> "plus"
                Operation.SUBTRACTION -> "minus"
                Operation.MULTIPLICATION -> "times"
                Operation.DIVISION -> "divided by"
            }
        }

    private fun render(picture: (LogicPicture) -> String, operation: (Operation) -> String) =
        buildString {
            append(picture(pictures.first()))
            operations.forEachIndexed { index, operator ->
                append(" ${operation(operator)} ${picture(pictures[index + 1])}")
            }
        }

    /** Multiplication and exact division precede addition and subtraction, left to right. */
    fun evaluate(values: Map<LogicPicture, Int>): Int {
        var total = 0
        var term = values.getValue(pictures.first())
        var additive = Operation.ADDITION
        fun accumulate(): Int = when (additive) {
            Operation.ADDITION -> Math.addExact(total, term)
            else -> Math.subtractExact(total, term)
        }
        operations.forEachIndexed { index, operation ->
            val right = values.getValue(pictures[index + 1])
            when (operation) {
                Operation.ADDITION, Operation.SUBTRACTION -> {
                    total = accumulate()
                    additive = operation
                    term = right
                }
                Operation.MULTIPLICATION -> term = Math.multiplyExact(term, right)
                Operation.DIVISION -> {
                    require(right != 0 && term % right == 0) { "Division must be exact with a nonzero divisor." }
                    if (term == Int.MIN_VALUE && right == -1) throw ArithmeticException("Integer overflow")
                    term /= right
                }
            }
        }
        return accumulate()
    }
}

@Serializable
data class LogicClue(val expression: LogicExpression, val total: Int)

@Serializable
data class LogicPuzzle(
    val clues: List<LogicClue>,
    val question: LogicExpression,
    val values: Map<LogicPicture, Int>
) {
    val answer: Int get() = question.evaluate(values)
    val text: String get() = question.text
}

class LogicPuzzleGenerator(private val random: Random = Random.Default) {
    fun next(
        operation: Operation,
        maximum: Int,
        settings: LogicMathSettings,
        minimum: Int = MIN_MAXIMUM,
        maximumSecond: Int = maximum
    ): LogicPuzzle {
        require(settings.isValid) { "Picture counts and equation lengths must be valid." }
        require(maximum in MIN_MAXIMUM..MAX_MAXIMUM) { "Maximum is outside the math bounds." }
        require(minimum in MIN_MAXIMUM..maximum) { "Minimum must be positive and no greater than the maximum." }
        require(maximumSecond in minimum..MAX_MAXIMUM) { "Second maximum is outside the math bounds." }

        val count = random.nextInt(settings.minimumPictures, settings.maximumPictures + 1)
        val selected = LogicPicture.entries.shuffled(random).take(count)
        val length = random.nextInt(maxOf(settings.minimumLength, count), settings.maximumLength + 1)
        val commonMaximum = if (operation == Operation.DIVISION) minOf(maximum, maximumSecond) else maximum
        val values = selected.associateWith { random.nextInt(minimum, commonMaximum + 1) }.toMutableMap()

        val question = if (operation == Operation.DIVISION) {
            val divisor = random.nextInt(minimum, commonMaximum + 1)
            values[selected[0]] = divisor * random.nextInt(1, maximum / divisor + 1)
            values[selected[1]] = divisor
            divisionQuestion(selected, values, length)
        } else {
            val pictures = (selected + List(length - count) { selected.random(random) }).shuffled(random)
            when (operation) {
                Operation.ADDITION -> expression(pictures, Operation.ADDITION)
                Operation.SUBTRACTION -> subtractionQuestion(pictures, values)
                Operation.MULTIPLICATION -> multiplicationQuestion(pictures, values)
                Operation.DIVISION -> error("Division is handled separately.")
            }
        }

        // Each row introduces one unknown with a nonzero coefficient, so the solution is unique
        // even when different pictures have equal values or the saved range contains one number.
        val questionCounts = question.pictures.groupingBy { it }.eachCount()
        val clues = selected.mapIndexed { index, picture ->
            val clueLength = random.nextInt(settings.minimumLength, settings.maximumLength + 1)
            val pictures = if (index == 0) List(clueLength) { picture } else
                listOf(picture) + List(clueLength - 1) { selected[random.nextInt(index)] }
            // Reordering an addition does not make a new question. Repeating the new
            // picture instead still determines its value without revealing the final row.
            val distinctPictures = if (operation == Operation.ADDITION &&
                pictures.groupingBy { it }.eachCount() == questionCounts
            ) List(clueLength) { picture } else pictures
            val clue = expression(distinctPictures.shuffled(random), Operation.ADDITION)
            LogicClue(clue, clue.evaluate(values))
        }.shuffled(random)
        return LogicPuzzle(clues, question, values.toMap())
    }

    private fun expression(pictures: List<LogicPicture>, operation: Operation) =
        LogicExpression(pictures, List(pictures.size - 1) { operation })

    private fun subtractionQuestion(
        pictures: List<LogicPicture>,
        values: Map<LogicPicture, Int>
    ): LogicExpression {
        val ordered = pictures.toMutableList()
        val largest = ordered.indices.maxBy { values.getValue(ordered[it]) }
        val first = ordered[0]
        ordered[0] = ordered[largest]
        ordered[largest] = first
        val pure = values.getValue(ordered.first()) >= ordered.drop(1).sumOf { values.getValue(it) }
        return LogicExpression(ordered, List(ordered.size - 1) {
            if (pure || it == 0) Operation.SUBTRACTION else Operation.ADDITION
        })
    }

    private fun multiplicationQuestion(
        pictures: List<LogicPicture>,
        values: Map<LogicPicture, Int>
    ): LogicExpression {
        var product = 1
        val pure = pictures.all {
            val value = values.getValue(it)
            if (product > MAX_LOGIC_ANSWER / value) false else {
                product *= value
                true
            }
        }
        if (pure) return expression(pictures, Operation.MULTIPLICATION)
        val focus = random.nextInt(pictures.size - 1)
        // One product is at most 10000²; the remaining (at most six) terms are additions.
        return LogicExpression(pictures, List(pictures.size - 1) {
            if (it == focus) Operation.MULTIPLICATION else Operation.ADDITION
        })
    }

    private fun divisionQuestion(
        selected: List<LogicPicture>,
        values: Map<LogicPicture, Int>,
        length: Int
    ): LogicExpression {
        val divisors = selected.drop(1)
        val extra = (selected.drop(2) + List(length - selected.size) { divisors.random(random) }).shuffled(random)
        val pictures = selected.take(2) + extra
        var quotient = values.getValue(pictures.first())
        val pure = pictures.drop(1).all {
            val divisor = values.getValue(it)
            if (quotient % divisor != 0) false else {
                quotient /= divisor
                true
            }
        }
        if (pure) return expression(pictures, Operation.DIVISION)
        val focus = random.nextInt(extra.size + 1)
        val ordered = extra.take(focus) + selected.take(2) + extra.drop(focus)
        return LogicExpression(ordered, List(ordered.size - 1) {
            if (it == focus) Operation.DIVISION else Operation.ADDITION
        })
    }
}
