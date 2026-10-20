package com.ssmath.app

import java.math.BigInteger
import kotlin.random.Random
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class LogicMathTest {
    private val apple = LogicPicture.APPLE
    private val star = LogicPicture.STAR
    private val fish = LogicPicture.FISH
    private val car = LogicPicture.CAR

    @Test fun settingsDefaultsAndLimitsAreValidWithoutRejectingInvalidDrafts() {
        val defaults = LogicMathSettings()
        assertEquals(LogicMathSettings(2, 3, 3, 4), defaults)
        assertTrue(defaults.isValid)
        assertEquals(2, MIN_LOGIC_PICTURES)
        assertEquals(6, MAX_LOGIC_PICTURES)
        assertEquals(2, MIN_LOGIC_LENGTH)
        assertEquals(8, MAX_LOGIC_LENGTH)
        assertTrue(LogicMathSettings(2, 6, 2, 8).isValid)
        assertTrue(LogicMathSettings(6, 6, 8, 8).isValid)
        listOf(
            defaults.copy(minimumPictures = 1),
            defaults.copy(maximumPictures = 7),
            defaults.copy(minimumPictures = 4),
            defaults.copy(maximumPictures = 1),
            defaults.copy(minimumLength = 1),
            defaults.copy(maximumLength = 9),
            defaults.copy(minimumLength = 5),
            defaults.copy(maximumLength = 2),
            LogicMathSettings(3, 3, 2, 4),
            LogicMathSettings(2, 5, 3, 4),
            LogicMathSettings(Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
        ).forEach {
            assertFalse("$it", it.isValid)
            assertEquals(it, Json.decodeFromString<LogicMathSettings>(Json.encodeToString(it)))
            assertThrows(IllegalArgumentException::class.java) {
                LogicPuzzleGenerator(Random(1)).next(Operation.ADDITION, 10, it)
            }
        }
    }

    @Test fun picturesHaveStableDistinctOfflineGlyphsAndAccessibleNames() {
        assertEquals(listOf("APPLE", "STAR", "FISH", "CAR", "TREE", "BALL"), LogicPicture.entries.map { it.name })
        assertEquals(6, LogicPicture.entries.map { it.glyph }.toSet().size)
        assertEquals(6, LogicPicture.entries.map { it.label }.toSet().size)
        LogicPicture.entries.forEach {
            assertTrue(it.glyph.isNotBlank())
            assertTrue(it.label.all { letter -> letter in 'a'..'z' })
            assertFalse(it.glyph.any(Char::isDigit))
        }
    }

    @Test fun expressionRendersPicturesOnlyAndSpeaksEveryOperation() {
        val expression = LogicExpression(listOf(apple, star, fish, car, apple), Operation.entries)
        assertEquals("🍎 + ⭐ − 🐟 × 🚗 ÷ 🍎", expression.text)
        assertEquals("apple plus star minus fish times car divided by apple", expression.accessibilityText)
        assertFalse(expression.text.any(Char::isDigit))
    }

    @Test fun evaluatorUsesPrecedenceAndLeftAssociativeMultiplicationAndDivision() {
        val values = mapOf(apple to 120, star to 6, fish to 2, car to 4)
        assertEquals(146, LogicExpression(listOf(apple, star, car, fish),
            listOf(Operation.ADDITION, Operation.MULTIPLICATION, Operation.ADDITION)).evaluate(values))
        assertEquals(112, LogicExpression(listOf(apple, star, fish, car),
            listOf(Operation.SUBTRACTION, Operation.MULTIPLICATION, Operation.ADDITION)).evaluate(values))
        assertEquals(10, LogicExpression(listOf(apple, star, fish, car),
            listOf(Operation.DIVISION, Operation.MULTIPLICATION, Operation.DIVISION)).evaluate(values))
        assertEquals(18, LogicExpression(listOf(apple, star, fish),
            listOf(Operation.DIVISION, Operation.SUBTRACTION)).evaluate(values))
        assertEquals(10, LogicExpression(listOf(car, apple, star, fish),
            listOf(Operation.SUBTRACTION, Operation.DIVISION, Operation.ADDITION))
            .evaluate(mapOf(apple to 12, star to 3, fish to 10, car to 4)))
        assertEquals(120, LogicExpression(listOf(apple), emptyList()).evaluate(values))
    }

    @Test fun evaluatorRejectsMalformedExpressionsMissingValuesInexactDivisionAndOverflow() {
        assertThrows(IllegalArgumentException::class.java) { LogicExpression(emptyList(), emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { LogicExpression(listOf(apple, star), emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            LogicExpression(listOf(apple), listOf(Operation.ADDITION))
        }
        val division = LogicExpression(listOf(apple, star), listOf(Operation.DIVISION))
        assertThrows(NoSuchElementException::class.java) { division.evaluate(mapOf(apple to 10)) }
        listOf(10 to 3, 10 to 0).forEach { (left, right) ->
            assertThrows(IllegalArgumentException::class.java) { division.evaluate(mapOf(apple to left, star to right)) }
        }
        listOf(
            Triple(Operation.ADDITION, Int.MAX_VALUE, 1),
            Triple(Operation.SUBTRACTION, Int.MIN_VALUE, 1),
            Triple(Operation.MULTIPLICATION, Int.MAX_VALUE, 2),
            Triple(Operation.DIVISION, Int.MIN_VALUE, -1)
        ).forEach { (operation, left, right) ->
            assertThrows(ArithmeticException::class.java) {
                LogicExpression(listOf(apple, star), listOf(operation)).evaluate(mapOf(apple to left, star to right))
            }
        }
        assertThrows(ArithmeticException::class.java) {
            LogicExpression(List(8) { apple }, List(7) { Operation.MULTIPLICATION }).evaluate(mapOf(apple to 10_000))
        }
    }

    @Test fun seededPuzzlesRespectEveryOperationBoundsLengthsAndUniqueSolutions() {
        val generator = LogicPuzzleGenerator(Random(24680))
        val settings = listOf(
            LogicMathSettings(),
            LogicMathSettings(2, 2, 2, 2),
            LogicMathSettings(2, 6, 2, 8),
            LogicMathSettings(3, 5, 4, 7),
            LogicMathSettings(4, 4, 6, 6),
            LogicMathSettings(6, 6, 6, 8),
            LogicMathSettings(6, 6, 8, 8)
        )
        Operation.entries.forEach { operation ->
            settings.forEach { configuration ->
                listOf(1..1, 1..10, 3..12, 7..9, 10..10, 9_999..10_000, 10_000..10_000).forEach { bounds ->
                    repeat(20) {
                        val puzzle = generator.next(operation, bounds.last, configuration, bounds.first)
                        assertPuzzle(puzzle, operation, configuration, bounds.first, bounds.last, bounds.last)
                    }
                }
            }
        }
    }

    @Test fun everyValidPictureAndLengthSettingCanGenerateEveryOperation() {
        val generator = LogicPuzzleGenerator(Random(9901))
        for (minimumPictures in 2..6) for (maximumPictures in minimumPictures..6) {
            for (minimumLength in 2..8) for (maximumLength in minimumLength..8) {
                val settings = LogicMathSettings(minimumPictures, maximumPictures, minimumLength, maximumLength)
                if (settings.isValid) Operation.entries.forEach { operation ->
                    val puzzle = generator.next(operation, 10_000, settings, minimum = 10_000)
                    assertPuzzle(puzzle, operation, settings, 10_000, 10_000, 10_000)
                }
            }
        }
    }

    @Test fun divisionHonorsSeparateMaximumsAndPositiveExactFocusPairs() {
        val generator = LogicPuzzleGenerator(Random(203))
        val settings = LogicMathSettings(2, 6, 2, 8)
        listOf(1 to 1, 1 to 10_000, 10_000 to 1, 100 to 7, 7 to 100,
            10_000 to 10_000).forEach { (dividendMaximum, divisorMaximum) ->
            repeat(100) {
                val puzzle = generator.next(Operation.DIVISION, dividendMaximum, settings,
                    maximumSecond = divisorMaximum)
                assertPuzzle(puzzle, Operation.DIVISION, settings, 1, dividendMaximum, divisorMaximum)
            }
        }
        repeat(100) {
            val puzzle = generator.next(Operation.DIVISION, 100, settings, minimum = 7, maximumSecond = 9)
            assertPuzzle(puzzle, Operation.DIVISION, settings, 7, 100, 9)
        }
    }

    @Test fun smallFeasibleQuestionsPreferTheSelectedOperationWithoutMixing() {
        val generator = LogicPuzzleGenerator(Random(81))
        val pairs = LogicMathSettings(2, 2, 2, 2)
        Operation.entries.forEach { operation ->
            repeat(50) {
                assertEquals(listOf(operation), generator.next(operation, 10_000, pairs).question.operations)
            }
        }
        val long = LogicMathSettings(6, 6, 8, 8)
        listOf(Operation.ADDITION, Operation.MULTIPLICATION, Operation.DIVISION).forEach { operation ->
            repeat(20) {
                assertEquals(List(7) { operation }, generator.next(operation, 1, long).question.operations)
            }
        }
        repeat(20) {
            assertEquals(List(7) { Operation.MULTIPLICATION },
                generator.next(Operation.MULTIPLICATION, 10, long).question.operations)
        }
    }

    @Test fun largeProductsAndNarrowSubtractionsUseSafeMixedExpressions() {
        val generator = LogicPuzzleGenerator(Random(716))
        val settings = LogicMathSettings(6, 6, 8, 8)
        listOf(Operation.SUBTRACTION, Operation.MULTIPLICATION, Operation.DIVISION).forEach { operation ->
            repeat(30) {
                val puzzle = generator.next(operation, 10_000, settings, minimum = 10_000)
                assertTrue(puzzle.question.operations.contains(Operation.ADDITION))
                assertTrue(puzzle.question.operations.contains(operation))
                val expected = when (operation) {
                    Operation.SUBTRACTION -> 60_000
                    Operation.MULTIPLICATION -> 100_060_000
                    else -> 60_001
                }
                assertEquals(expected, puzzle.answer)
                assertEquals(expected, parseAnswer(puzzle.answer.toString()))
            }
        }
    }

    @Test fun seedIsReproducibleWhilePicturesValuesAndQuestionLengthsVary() {
        val first = LogicPuzzleGenerator(Random(780))
        val second = LogicPuzzleGenerator(Random(780))
        val settings = LogicMathSettings(2, 6, 2, 8)
        Operation.entries.forEach { operation ->
            val puzzles = List(100) {
                val puzzle = first.next(operation, 50, settings)
                assertEquals(puzzle, second.next(operation, 50, settings))
                puzzle
            }
            assertTrue(puzzles.toSet().size > 90)
            assertEquals((2..6).toSet(), puzzles.map { it.values.size }.toSet())
            assertEquals((2..8).toSet(), puzzles.map { it.question.pictures.size }.toSet())
            assertEquals(LogicPicture.entries.toSet(), puzzles.flatMap { it.values.keys }.toSet())
        }
    }

    @Test fun puzzleAndDraftSettingsRoundTripThroughSerializationWithoutComputedAnswerFields() {
        val generator = LogicPuzzleGenerator(Random(899))
        Operation.entries.forEach { operation ->
            val puzzle = generator.next(operation, 10_000, LogicMathSettings(6, 6, 8, 8), minimum = 10_000)
            val encoded = Json.encodeToString(puzzle)
            val restored = Json.decodeFromString<LogicPuzzle>(encoded)
            assertEquals(puzzle, restored)
            assertEquals(puzzle.answer, restored.answer)
            assertFalse(encoded.contains("\"answer\""))
            assertFalse(encoded.contains("\"text\""))
        }
    }

    @Test fun generatorRejectsInvalidMathBounds() {
        val generator = LogicPuzzleGenerator(Random(200))
        val settings = LogicMathSettings()
        Operation.entries.forEach { operation ->
            listOf(Int.MIN_VALUE, -1, 0, 10_001, Int.MAX_VALUE).forEach { invalid ->
                assertThrows(IllegalArgumentException::class.java) { generator.next(operation, invalid, settings) }
                assertThrows(IllegalArgumentException::class.java) {
                    generator.next(operation, 10, settings, minimum = invalid)
                }
                assertThrows(IllegalArgumentException::class.java) {
                    generator.next(operation, 10, settings, maximumSecond = invalid)
                }
            }
            assertThrows(IllegalArgumentException::class.java) { generator.next(operation, 10, settings, minimum = 11) }
            assertThrows(IllegalArgumentException::class.java) {
                generator.next(operation, 10, settings, minimum = 7, maximumSecond = 6)
            }
        }
    }

    private fun assertPuzzle(
        puzzle: LogicPuzzle,
        operation: Operation,
        settings: LogicMathSettings,
        minimum: Int,
        maximum: Int,
        maximumSecond: Int
    ) {
        assertTrue(puzzle.values.size in settings.minimumPictures..settings.maximumPictures)
        assertEquals(puzzle.values.keys, puzzle.question.pictures.toSet())
        assertEquals(puzzle.values.size, puzzle.clues.size)
        val upperBound = if (operation == Operation.DIVISION) maxOf(maximum, maximumSecond) else maximum
        assertTrue(puzzle.values.values.all { it in minimum..upperBound })
        (puzzle.clues.map { it.expression } + puzzle.question).forEach { expression ->
            assertTrue(expression.pictures.size in settings.minimumLength..settings.maximumLength)
            assertEquals(expression.pictures.size - 1, expression.operations.size)
            assertTrue(expression.pictures.all { it in puzzle.values })
            assertFalse(expression.text.any(Char::isDigit))
            assertFalse(expression.text.contains("="))
            assertFalse(expression.accessibilityText.any(Char::isDigit))
            assertEquals(independentEvaluate(expression, puzzle.values), expression.evaluate(puzzle.values))
        }
        puzzle.clues.forEach { assertEquals(it.total, independentEvaluate(it.expression, puzzle.values)) }
        assertTrue(puzzle.question.operations.contains(operation))
        assertTrue(puzzle.question.operations.all { it == operation || it == Operation.ADDITION })
        assertEquals(independentEvaluate(puzzle.question, puzzle.values), puzzle.answer)
        assertTrue(puzzle.answer in 0..999_999_999)
        assertEquals(puzzle.answer, parseAnswer(puzzle.answer.toString()))
        assertEquals(puzzle.question.text, puzzle.text)
        assertEquals(puzzle.values, solveClues(puzzle.clues))
        if (operation == Operation.DIVISION) {
            assertTrue(puzzle.answer > 0)
            var term = puzzle.values.getValue(puzzle.question.pictures.first())
            puzzle.question.operations.forEachIndexed { index, operator ->
                val right = puzzle.values.getValue(puzzle.question.pictures[index + 1])
                if (operator == Operation.DIVISION) {
                    assertTrue(term in minimum..maximum || term in 1 until minimum)
                    assertTrue(right in minimum..maximumSecond)
                    assertEquals(0, term % right)
                    term /= right
                } else {
                    term = right
                }
            }
        }
    }

    // Solve solely from the displayed clues, without consulting the hidden answer map.
    private fun solveClues(clues: List<LogicClue>): Map<LogicPicture, Int> {
        val remaining = clues.toMutableList()
        val solved = mutableMapOf<LogicPicture, Int>()
        repeat(clues.size) {
            val clue = remaining.firstOrNull { it.expression.pictures.filterNot(solved::containsKey).toSet().size == 1 }
            assertNotNull("Clues must expose one uniquely solvable unknown at every step", clue)
            clue!!
            assertTrue(clue.expression.operations.all { it == Operation.ADDITION })
            val unknown = clue.expression.pictures.first { it !in solved }
            val coefficient = clue.expression.pictures.count { it == unknown }
            val knownTotal = clue.expression.pictures.filter { it in solved }.sumOf { solved.getValue(it) }
            val residual = clue.total - knownTotal
            assertEquals(0, residual % coefficient)
            solved[unknown] = residual / coefficient
            remaining.remove(clue)
        }
        assertTrue(remaining.isEmpty())
        return solved
    }

    // An independent operator-stack evaluator uses arbitrary-precision arithmetic to detect overflow.
    private fun independentEvaluate(expression: LogicExpression, values: Map<LogicPicture, Int>): Int {
        val operands = mutableListOf<BigInteger>()
        val operators = mutableListOf<Operation>()
        fun precedence(operation: Operation) = when (operation) {
            Operation.MULTIPLICATION, Operation.DIVISION -> 2
            else -> 1
        }
        fun apply() {
            val right = operands.removeAt(operands.lastIndex)
            val left = operands.removeAt(operands.lastIndex)
            val result = when (operators.removeAt(operators.lastIndex)) {
                Operation.ADDITION -> left + right
                Operation.SUBTRACTION -> left - right
                Operation.MULTIPLICATION -> left * right
                Operation.DIVISION -> {
                    assertNotEquals(BigInteger.ZERO, right)
                    assertEquals(BigInteger.ZERO, left.remainder(right))
                    left / right
                }
            }
            operands += result
        }
        operands += BigInteger.valueOf(values.getValue(expression.pictures.first()).toLong())
        expression.operations.forEachIndexed { index, operation ->
            while (operators.isNotEmpty() && precedence(operators.last()) >= precedence(operation)) apply()
            operators += operation
            operands += BigInteger.valueOf(values.getValue(expression.pictures[index + 1]).toLong())
        }
        while (operators.isNotEmpty()) apply()
        return operands.single().intValueExact()
    }
}
