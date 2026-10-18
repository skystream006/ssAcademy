package com.ssmath.app

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class MathGameTest {
    private val generator = ProblemGenerator(Random(42))

    @Test fun multiplicationMatrixVisitsEveryPairInRowOrderExactlyOnce() {
        var game = GameState.startMultiplicationMatrix()
        for (left in 1..11) {
            for (right in 1..11) {
                assertFalse(game.finished)
                assertTrue(game.isMultiplicationMatrix)
                assertEquals(121, game.questionCount)
                assertEquals(Problem(left, right, Operation.MULTIPLICATION), game.problem)
                game = game.answer(left * right, generator)
            }
        }
        assertTrue(game.perfect)
        assertEquals(121, game.attempts.map { it.problem }.toSet().size)
        assertThrows(IllegalStateException::class.java) { game.answer(121, generator) }
    }

    @Test fun multiplicationMatrixUsesTheExistingWrongAnswerLimit() {
        var game = GameState.startMultiplicationMatrix()
        assertEquals(13, game.maxWrongAnswers)
        repeat(12) { game = game.answer(0, generator) }
        assertFalse(game.finished)
        assertEquals(Problem(2, 2, Operation.MULTIPLICATION), game.problem)
        game = game.answer(0, generator)
        assertTrue(game.finished)
        assertEquals(13, game.wrong)
    }

    @Test fun randomizedMatrixVisitsEveryPairOnceAndShufflesEachNewGame() {
        val rowOrder = (0 until MultiplicationMatrix.QUESTION_COUNT).map(MultiplicationMatrix::problemAt)
        val random = Random(42)
        val orders = (1..3).map {
            var game = GameState.startMultiplicationMatrix(randomizeQuestionOrder = true, random = random)
            repeat(121) { index ->
                assertFalse(game.finished)
                assertTrue(game.isMultiplicationMatrix)
                assertEquals(121, game.questionCount)
                game = game.answer(if (index < 12) 0 else game.problem.answer, generator)
            }
            assertTrue(game.finished)
            assertEquals(109, game.correct)
            assertEquals(12, game.wrong)
            val order = game.attempts.map { it.problem }
            assertEquals(rowOrder.toSet(), order.toSet())
            assertNotEquals(rowOrder, order)
            assertThrows(IllegalStateException::class.java) { game.answer(0, generator) }
            order
        }
        assertEquals(3, orders.toSet().size)
    }

    @Test fun randomizedMatrixStopsAt13WrongAnswersWithoutRepeatingQuestions() {
        var game = GameState.startMultiplicationMatrix(randomizeQuestionOrder = true, random = Random(7))
        repeat(13) {
            assertFalse(game.finished)
            game = game.answer(0, generator)
        }
        assertTrue(game.finished)
        assertEquals(13, game.wrong)
        assertEquals(13, game.attempts.map { it.problem }.toSet().size)
    }

    @Test fun everyOperationKeepsBothNumbersWithinTheMaximum() {
        listOf(1, 2, 7, 12, 100).forEach { maximum ->
            Operation.entries.forEach { operation ->
                repeat(500) {
                    val problem = generator.next(operation, maximum)
                    assertEquals(operation, problem.operation)
                    assertTrue("$problem", problem.left in 1..maximum)
                    assertTrue("$problem", problem.right in 1..maximum)
                }
            }
        }
    }

    @Test fun everyOperationKeepsBothNumbersWithinTheSelectedRange() {
        listOf(1..1, 3..12, 7..9, 10..10, 9_999..MAX_MAXIMUM, MAX_MAXIMUM..MAX_MAXIMUM).forEach { range ->
            Operation.entries.forEach { operation ->
                repeat(100) {
                    val problem = generator.next(operation, range.last, minimum = range.first)
                    assertEquals(operation, problem.operation)
                    assertTrue("$problem", problem.left in range)
                    assertTrue("$problem", problem.right in range)
                    if (operation == Operation.SUBTRACTION) assertTrue(problem.answer >= 0)
                    if (operation == Operation.DIVISION) assertEquals(0, problem.left % problem.right)
                }
            }
        }
    }

    @Test fun minimumMustBePositiveAndNoGreaterThanMaximum() {
        Operation.entries.forEach { operation ->
            listOf(-1, 0, 11, MAX_MAXIMUM + 1).forEach { minimum ->
                assertThrows(IllegalArgumentException::class.java) {
                    generator.next(operation, 10, minimum = minimum)
                }
                assertThrows(IllegalArgumentException::class.java) {
                    GameState.start(operation, 10, generator, minimum = minimum)
                }
            }
        }
    }

    @Test fun selectedRangeIsRetainedThroughoutTheGame() {
        Operation.entries.forEach { operation ->
            var game = GameState.start(operation, 12, generator, minimum = 7)
            repeat(DEFAULT_QUESTION_COUNT) {
                assertEquals(7, game.minimum)
                assertEquals(12, game.maximum)
                assertTrue(game.problem.left in 7..12)
                assertTrue(game.problem.right in 7..12)
                game = game.answer(game.problem.answer, generator)
            }
            assertTrue(game.perfect)
        }
    }

    @Test fun answersMatchTheOperation() {
        assertEquals(12, Problem(7, 5, Operation.ADDITION).answer)
        assertEquals(2, Problem(7, 5, Operation.SUBTRACTION).answer)
        assertEquals(35, Problem(7, 5, Operation.MULTIPLICATION).answer)
        assertEquals(4, Problem(12, 3, Operation.DIVISION).answer)
        assertEquals("7 + 5", Problem(7, 5, Operation.ADDITION).text)
    }

    @Test fun subtractionIsNeverNegativeAndDivisionIsAlwaysWhole() {
        repeat(1000) {
            val difference = generator.next(Operation.SUBTRACTION, 50)
            assertTrue("$difference", difference.answer >= 0)
            val quotient = generator.next(Operation.DIVISION, 50)
            assertEquals("$quotient", 0, quotient.left % quotient.right)
            assertTrue("$quotient", quotient.answer >= 1)
        }
    }

    @Test fun divisionUsesVariousDivisors() {
        val divisors = (1..500).map { generator.next(Operation.DIVISION, 20).right }.toSet()
        assertTrue(divisors.size > 5)
    }

    @Test fun divisionCanUseEveryValidPairInTheSelectedRange() {
        val problems = (1..1000).map { generator.next(Operation.DIVISION, 12, minimum = 3) }.toSet()
        val expected = (3..12).flatMap { divisor ->
            (1..12 / divisor).map { quotient -> Problem(divisor * quotient, divisor, Operation.DIVISION) }
        }.toSet()
        assertEquals(expected, problems)
    }

    @Test fun divisionUsesIndependentOperandMaximumsWithWholeAnswers() {
        listOf(1 to 1, 1 to MAX_MAXIMUM, MAX_MAXIMUM to 1, 100 to 7, 7 to 100,
            MAX_MAXIMUM to MAX_MAXIMUM).forEach { (first, second) ->
            var game = GameState.start(Operation.DIVISION, first, generator, questionCount = 100, maximumSecond = second)
            repeat(100) {
                assertEquals(first, game.maximum)
                assertEquals(second, game.maximumSecond)
                assertTrue(game.problem.left in 1..first)
                assertTrue(game.problem.right in 1..second)
                assertEquals(0, game.problem.left % game.problem.right)
                game = game.answer(game.problem.answer, generator)
            }
            assertTrue(game.perfect)
        }
    }

    @Test fun divisionCanUseEveryPairWithinIndependentMaximums() {
        listOf(12 to 3, 3 to 12).forEach { (first, second) ->
            val expected = (1..first).flatMap { left ->
                (1..second).filter { left % it == 0 }.map { right -> Problem(left, right, Operation.DIVISION) }
            }.toSet()
            val actual = (1..2000).map {
                generator.next(Operation.DIVISION, first, maximumSecond = second)
            }.toSet()
            assertEquals(expected, actual)
        }
    }

    @Test fun divisionRejectsInvalidOperandMaximums() {
        listOf(0, -1, MAX_MAXIMUM + 1, Int.MAX_VALUE).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                GameState.start(Operation.DIVISION, invalid, generator, maximumSecond = 10)
            }
            assertThrows(IllegalArgumentException::class.java) {
                GameState.start(Operation.DIVISION, 10, generator, maximumSecond = invalid)
            }
        }
    }

    @Test fun avoidsRepeatingTheSameProblemConsecutively() {
        var previous = generator.next(Operation.ADDITION, 3)
        repeat(200) {
            val next = generator.next(Operation.ADDITION, 3, previous)
            assertNotEquals(previous, next)
            previous = next
        }
        // With only one possible problem the generator still returns it.
        assertEquals(Problem(1, 1, Operation.DIVISION), generator.next(Operation.DIVISION, 1, Problem(1, 1, Operation.DIVISION)))
        assertEquals(Problem(7, 7, Operation.DIVISION),
            generator.next(Operation.DIVISION, 7, Problem(7, 7, Operation.DIVISION), minimum = 7))
    }

    @Test fun maximumMustBeAWholeNumberInRange() {
        assertEquals(1, parseMaximum("1"))
        assertEquals(12, parseMaximum(" 12 "))
        assertEquals(MAX_MAXIMUM, parseMaximum(MAX_MAXIMUM.toString()))
        listOf("", "0", "-3", "1.5", "abc", (MAX_MAXIMUM + 1).toString(), "99999999999").forEach {
            assertNull(it, parseMaximum(it))
        }
        assertThrows(IllegalArgumentException::class.java) { generator.next(Operation.ADDITION, 0) }
    }

    @Test fun answersMustBeNonNegativeWholeNumbers() {
        assertEquals(0, parseAnswer("0"))
        assertEquals(100000000, parseAnswer("100000000"))
        listOf("", "-1", "2.5", "x", "1234567890").forEach { assertNull(it, parseAnswer(it)) }
    }

    @Test fun questionCountMustBeAWholeNumberInRange() {
        assertEquals(MIN_QUESTION_COUNT, parseQuestionCount("1"))
        assertEquals(12, parseQuestionCount(" 12 "))
        assertEquals(MAX_QUESTION_COUNT, parseQuestionCount(MAX_QUESTION_COUNT.toString()))
        listOf("", "0", "-3", "1.5", "abc", (MAX_QUESTION_COUNT + 1).toString(), "99999999999").forEach {
            assertNull(it, parseQuestionCount(it))
        }
        listOf(0, -1, MAX_QUESTION_COUNT + 1).forEach { count ->
            assertThrows(IllegalArgumentException::class.java) {
                GameState.start(Operation.ADDITION, 10, generator, count)
            }
        }
    }

    @Test fun perfectGameEndsExactlyAtTheQuestionCount() {
        Operation.entries.forEach { operation ->
            listOf(1, 3, DEFAULT_QUESTION_COUNT).forEach { count ->
                var game = GameState.start(operation, 12, generator, count)
                repeat(count) {
                    assertFalse(game.finished)
                    assertFalse(game.perfect)
                    val problem = game.problem
                    game = game.answer(problem.answer, generator)
                    if (it == count - 1) assertEquals(problem, game.problem)
                }
                assertTrue(game.finished)
                assertTrue(game.perfect)
                assertEquals(count, game.correct)
                assertEquals(count, game.attempts.size)
                val finished = game
                assertThrows(IllegalStateException::class.java) { finished.answer(0, generator) }
            }
        }
    }

    @Test fun wrongAnswersAlsoCountTowardsTheQuestionLimit() {
        var game = GameState.start(Operation.ADDITION, 12, generator, 20)
        game = game.answer(game.problem.answer + 1, generator)
        repeat(18) {
            assertFalse(game.finished)
            game = game.answer(game.problem.answer, generator)
        }
        assertFalse(game.finished)
        game = game.answer(game.problem.answer + 1, generator)
        assertTrue(game.finished)
        assertFalse(game.perfect)
        assertEquals(18, game.correct)
        assertEquals(2, game.wrong)
        assertEquals(20, game.attempts.size)
    }

    @Test fun correctAnswersScoreAndWrongAnswersAreTalliedUntilAboveTenPercent() {
        var game = GameState.start(Operation.MULTIPLICATION, 12, generator)
        repeat(3) {
            val problem = game.problem
            game = game.answer(problem.answer, generator)
            assertEquals(Operation.MULTIPLICATION, game.problem.operation)
            assertNotEquals(problem, game.problem)
        }
        assertEquals(3, game.correct)
        assertEquals(0, game.wrong)
        game = game.answer(game.problem.answer + 1, generator)
        assertFalse(game.finished)
        assertEquals(1, game.wrong)
        val last = game.problem
        game = game.answer(last.answer + 1, generator)
        assertTrue(game.finished)
        assertFalse(game.perfect)
        assertEquals(2, game.wrong)
        assertEquals(3, game.correct)
        assertEquals(5, game.attempts.size)
        assertEquals(last, game.attempts.last().problem)
        assertFalse(game.attempts.last().correct)
        val finished = game
        assertThrows(IllegalStateException::class.java) { finished.answer(0, generator) }
    }

    @Test fun formatsDurations() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:00", formatDuration(-5))
        assertEquals("01:05", formatDuration(65_999))
        assertEquals("1:00:01", formatDuration(3_601_000))
    }

    @Test fun everyGameEndsOnlyWhenWrongAnswersExceedTenPercentOfTheTotal() {
        Operation.entries.forEach { operation ->
            mapOf(1 to 1, 2 to 1, 9 to 1, 10 to 2, 11 to 2, 15 to 2, 19 to 2, 20 to 3,
                25 to 3, 26 to 3, 49 to 5, 50 to 6, 51 to 6, 59 to 6, 60 to 7,
                99 to 10, 100 to 11, 101 to 11, 999 to 100, 1_000 to 101).forEach { (count, limit) ->
                var game = GameState.start(operation, 10, generator, count)
                assertEquals(limit, game.maxWrongAnswers)
                assertFalse(game.finished)
                repeat(limit - 1) {
                    game = game.answer(game.problem.answer + 1, generator)
                    assertFalse("Game with $count questions ended at ${game.wrong} wrong", game.finished)
                }
                game = game.answer(game.problem.answer + 1, generator)
                assertTrue(game.finished)
                assertEquals(limit, game.wrong)
                assertEquals(limit, game.attempts.size)
                assertThrows(IllegalStateException::class.java) { game.answer(0, generator) }
            }
        }
    }
}
