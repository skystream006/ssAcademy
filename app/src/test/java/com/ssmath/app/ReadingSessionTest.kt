package com.ssmath.app

import org.junit.Assert.*
import org.junit.Test

internal fun readingSessionFixture(
    game: ReadingGame = ReadingGame.MISSING,
    count: Int = 15,
    rewardsEnabled: Boolean = true,
    timeLimitMs: Long? = null
): ReadingData = ReadingEngine.startSession(
    readingFixture().copy(attempts = emptyList(), profiles = readingFixture().profiles.map {
        it.copy(activeAttemptId = null)
    }), game, ReadingLevel.SPROUT, count, rewardsEnabled, id = "session-a",
    now = 1_700_000_000_000, timeLimitMs = timeLimitMs
)

class ReadingSessionTest {
    @Test fun configuredSessionCountsIncludeBoundariesAndCompleteNormally() {
        for (count in listOf(1, 14, 16, 20, 40, 1000)) {
            val data = readingSessionFixture(count = count)
            assertEquals(count, data.activeSession?.questionCount)
            assertEquals(count, data.attempts.size)
            ReadingEngine.validate(data)
        }
        for (count in listOf(1, 20, 40)) {
            var data = readingSessionFixture(count = count)
            repeat(count) {
                data = ReadingEngine.nextSessionActivity(readingSolve(data))
            }
            assertEquals(count, data.sessionCorrect(requireNotNull(data.activeSession)))
            assertNotNull(data.activeSession?.finishedAt)
            ReadingEngine.validate(data)
        }
    }

    @Test fun finishAndWrongFinalSubmitStoreNewThresholdsWithoutReinterpretingCompletedSessions() {
        val thresholds = RewardThresholds(15, 25)
        for (count in listOf(15, 25)) for (wrongFinalAnswer in listOf(false, true)) {
            var data = readingSessionFixture(count = count)
            repeat(count - 1) { data = ReadingEngine.nextSessionActivity(readingSolve(data)) }
            assertEquals(RewardThresholds(), data.activeSession?.rewardThresholds)
            val answer = if (wrongFinalAnswer) readingWrong(data) else readingCorrect(data)
            data = ReadingEngine.submit(ReadingEngine.field(data, "answer", answer.toString()), "answer",
                rewardThresholds = thresholds)
            data = ReadingEngine.finish(data, rewardThresholds = thresholds)
            val session = requireNotNull(data.activeSession)
            assertEquals(thresholds, session.rewardThresholds)
            assertEquals(thresholds.tierForQuestionCount(count), session.prizeType?.tier)
            assertEquals(count - if (wrongFinalAnswer) 1 else 0, data.sessionCorrect(session))
            ReadingEngine.validate(data)
            assertEquals(data, ReadingEngine.finish(data, rewardsEnabled = false, rewardThresholds = RewardThresholds(999, 1000)))
            assertEquals(data, ReadingEngine.endSession(data, session.id, rewardThresholds = RewardThresholds()))
            assertThrows(IllegalArgumentException::class.java) {
                ReadingEngine.validate(data.copy(sessions = listOf(session.copy(rewardThresholds = RewardThresholds()))))
            }
        }
    }

    @Test fun newThresholdsKeepReadingStartFinishAccuracyAndDisabledTierGuards() {
        val thresholds = RewardThresholds(15, 25)
        for (enabledAtStart in listOf(false, true)) for (enabledAtFinish in listOf(false, true)) {
            var data = readingSessionFixture(rewardsEnabled = enabledAtStart)
            repeat(14) { data = ReadingEngine.nextSessionActivity(readingSolve(data)) }
            data = readingAnswer(data, readingCorrect(data))
            val completed = ReadingEngine.finish(data, rewardsEnabled = enabledAtFinish, rewardThresholds = thresholds)
            assertEquals(enabledAtStart && enabledAtFinish, completed.activeSession?.prizeType != null)
            ReadingEngine.validate(completed)
            val disabled = ReadingEngine.finish(data, rewardThresholds = thresholds,
                disabledRewards = RewardType.entries.filter { it.tier == RewardTier.TIER_1 }.toSet())
            assertNull(disabled.activeSession?.prizeType)
            ReadingEngine.validate(disabled)
        }
        var failed = readingSessionFixture()
        repeat(2) {
            failed = ReadingEngine.nextSessionActivity(ReadingEngine.submit(
                ReadingEngine.field(failed, "answer", readingWrong(failed).toString()), "answer",
                rewardThresholds = thresholds))
        }
        assertNotNull(failed.activeSession?.finishedAt)
        assertEquals(thresholds, failed.activeSession?.rewardThresholds)
        assertNull(failed.activeSession?.prizeType)
        ReadingEngine.validate(failed)
    }

    @Test fun deletingAnySessionMemberRemovesTheWholeGameAndKeepsADurableReceipt() {
        var data = readingSessionFixture(ReadingGame.STUDIO)
        data = ReadingEngine.nextSessionActivity(readingSolve(data))
        assertEquals(1, data.stories.size)
        val deleted = ReadingEngine.deleteHistory(data, setOf(data.attempts.first().id))
        assertTrue(deleted.sessions.isEmpty())
        assertTrue(deleted.attempts.isEmpty())
        assertTrue(deleted.stories.isEmpty())
        assertEquals(setOf("session-a"), deleted.deletedSessionIds)
        assertNull(deleted.active)
        assertEquals(ReadingPage.MENU, deleted.page)
        ReadingEngine.validate(deleted)
        assertThrows(IllegalArgumentException::class.java) {
            ReadingEngine.startSession(deleted, ReadingGame.MISSING, ReadingLevel.SPROUT, 15, id = "session-a")
        }
        val next = ReadingEngine.startSession(deleted, ReadingGame.MISSING, ReadingLevel.SPROUT, 15, id = "new")
        assertEquals(deleted.deletedSessionIds, next.deletedSessionIds)
        ReadingEngine.validate(next)
    }

    @Test fun sessionsAllocateExactlyTheSelectedCountAndOnlyNextAdvances() {
        for (count in listOf(15, 25, 50)) {
            var data = readingSessionFixture(count = count)
            val session = requireNotNull(data.activeSession)
            assertEquals(count, session.questionCount)
            assertEquals(count, data.attempts.size)
            assertEquals(count, session.attemptIds.distinct().size)
            assertTrue(data.attempts.all { it.open })
            assertEquals(session.attemptIds.first(), data.active?.id)
            assertEquals(data, ReadingEngine.nextSessionActivity(data))
            repeat(count) { index ->
                val id = data.active?.id
                data = readingSolve(data)
                assertEquals(id, data.active?.id)
                assertEquals(index + 1, data.sessionCorrect(session))
                assertEquals(index + 1, data.sessionAnswered(session))
                assertEquals(0, data.sessionWrong(session))
                assertEquals(data, ReadingEngine.finish(data))
                ReadingEngine.validate(data)
                data = ReadingEngine.nextSessionActivity(data)
                if (index + 1 < count) assertEquals(session.attemptIds[index + 1], data.active?.id)
            }
            assertNotNull(data.activeSession?.finishedAt)
            assertFalse(requireNotNull(data.activeSession).abandoned)
            assertEquals(data, ReadingEngine.nextSessionActivity(data))
            assertEquals(count, data.attempts.size)
        }
    }

    @Test fun firstValidWrongAnswerFinishesActivityAndCannotBeRetriedOrSubmittedTwice() {
        var data = readingSessionFixture()
        data = ReadingEngine.submit(data, "answer")
        assertEquals(0, data.sessionAnswered(requireNotNull(data.activeSession)))
        assertTrue(requireNotNull(data.active).open)
        data = readingAnswer(data, readingWrong(data))
        val wrong = requireNotNull(data.active)
        assertNotNull(wrong.finishedAt)
        assertEquals(1, data.sessionWrong(requireNotNull(data.activeSession)))
        assertEquals(0, data.sessionCorrect(requireNotNull(data.activeSession)))
        repeat(5) {
            assertEquals(data, ReadingEngine.submit(data, "answer"))
            assertEquals(data, readingAnswer(data, readingCorrect(data)))
            assertEquals(data, ReadingEngine.finish(data))
        }
        assertEquals(1, wrong.checkpoints.getValue("answer").tries)
        assertNull(data.activeSession?.finishedAt)
        ReadingEngine.validate(data)
    }

    @Test fun moreThanTenPercentWrongEndsSessionImmediatelyAndAbandonsUnplayedAssignments() {
        for (count in listOf(15, 25, 50)) {
            var data = readingSessionFixture(count = count)
            val session = requireNotNull(data.activeSession)
            repeat(count / 10) {
                data = readingAnswer(data, readingWrong(data))
                assertNull(data.activeSession?.finishedAt)
                assertFalse(requireNotNull(data.active).open)
                data = ReadingEngine.nextSessionActivity(data)
            }
            val lastId = data.active?.id
            data = readingAnswer(data, readingWrong(data))
            assertEquals(lastId, data.active?.id)
            assertNotNull(data.activeSession?.finishedAt)
            assertNull(data.activeSession?.prizeType)
            assertEquals(count / 10 + 1, data.sessionWrong(session))
            assertEquals(count / 10 + 1, data.sessionAnswered(session))
            assertTrue(data.attempts.filter { it.finishedAt == null }.all { it.abandoned })
            assertNull(ReadingEngine.draft(data, session.game, session.level))
            assertEquals(data, ReadingEngine.nextSessionActivity(data))
            data.attempts.filter { it.abandoned }.forEach { attempt ->
                assertThrows(IllegalArgumentException::class.java) { ReadingEngine.resume(data, attempt.id) }
            }
            ReadingEngine.validate(data)
        }
    }

    @Test fun multiCheckpointActivitiesCountAsOneAndFailedEvidenceDoesNotCountEarlierAnswer() {
        for (game in listOf(ReadingGame.SEQUENCE, ReadingGame.DETECTIVE)) {
            var data = readingSessionFixture(game)
            if (game == ReadingGame.SEQUENCE) {
                data = ReadingEngine.submit(readingOrder(data, requireNotNull(data.active).content.validOrders.first()), "order")
            } else {
                data = readingAnswer(data, readingCorrect(data))
            }
            val session = requireNotNull(data.activeSession)
            assertEquals(0, data.sessionCorrect(session))
            assertEquals(0, data.sessionAnswered(session))
            data = if (game == ReadingGame.SEQUENCE) readingAnswer(data, readingWrong(data))
            else ReadingEngine.submit(ReadingEngine.field(data, "evidence",
                ((requireNotNull(data.active).content.evidence + 1) % requireNotNull(data.active).content.passage.size).toString()),
                "evidence", now = 1_700_000_000_100)
            assertEquals(1, data.sessionWrong(session))
            assertEquals(0, data.sessionCorrect(session))
            assertEquals(1, data.sessionAnswered(session))
            ReadingEngine.validate(data)
        }
    }

    @Test fun creativeGamesCountOnceAndOptionalScrambleBonusIsNotPartOfSessionScoring() {
        for (game in listOf(ReadingGame.STUDIO, ReadingGame.SCRAMBLE)) {
            var data = readingSessionFixture(game)
            data = readingSolve(data)
            val session = requireNotNull(data.activeSession)
            assertEquals(1, data.sessionCorrect(session))
            assertEquals(1, data.sessionAnswered(session))
            assertEquals(0, data.sessionWrong(session))
            if (game == ReadingGame.SCRAMBLE) assertFalse(requireNotNull(data.active).earned("bonus"))
            else assertEquals(1, data.stories.size)
            ReadingEngine.validate(data)
        }
        var scramble = readingSessionFixture(ReadingGame.SCRAMBLE)
        scramble = ReadingEngine.submit(readingOrder(scramble,
            requireNotNull(scramble.active).content.validOrders.first()), "order")
        val ready = scramble
        scramble = readingAnswer(scramble, readingWrong(scramble), "bonus")
        assertEquals(ready.active?.checkpoints, scramble.active?.checkpoints)
        assertTrue(requireNotNull(scramble.active).open)
        assertEquals(1, ReadingEngine.finish(scramble).sessionCorrect(requireNotNull(scramble.activeSession)))
    }

    @Test fun resumingAnyAssignedMemberReturnsTheCurrentMemberWithoutAllocatingAgain() {
        for (correct in listOf(false, true)) {
            var data = readingSessionFixture()
            data = if (correct) readingSolve(data) else readingAnswer(data, readingWrong(data))
            val session = requireNotNull(data.activeSession)
            val completed = data.active
            data = ReadingEngine.resume(data.copy(page = ReadingPage.MENU), session.attemptIds.last())
            assertEquals(completed, data.active)
            assertNotNull(data.active?.finishedAt)
            assertEquals(data, ReadingEngine.submit(data, "answer"))
            assertEquals(data, ReadingEngine.finish(data))
            data = ReadingEngine.nextSessionActivity(data)
            data = ReadingEngine.field(data, "answer", "1")
            for (requested in listOf(session.attemptIds.first(), session.attemptIds.last())) {
                val resumed = ReadingEngine.resume(data.copy(page = ReadingPage.MENU), requested)
                assertEquals(data, resumed)
                assertEquals(session.attemptIds[1], resumed.active?.id)
                assertEquals(15, resumed.attempts.size)
                ReadingEngine.validate(resumed)
            }
        }
    }

    @Test fun newSessionAbandonsPreviousUnusedMembersButKeepsLegacyDraftAndCompletedHistory() {
        val legacy = ReadingEngine.field(readingFixture(), "answer", "1")
        var data = ReadingEngine.startSession(legacy, ReadingGame.MISSING, ReadingLevel.SPROUT, 15,
            now = 1_700_000_000_000)
        data = readingSolve(data)
        val previous = requireNotNull(data.activeSession)
        data = ReadingEngine.startSession(data, ReadingGame.DETECTIVE, ReadingLevel.SPROUT, 25)
        val archived = data.sessions.first()
        assertTrue(archived.abandoned)
        assertNotNull(archived.finishedAt)
        assertNull(archived.prizeType)
        assertEquals(1, data.sessionCorrect(previous))
        assertEquals(14, data.attempts.count { it.id in previous.attemptIds && it.abandoned })
        assertEquals(legacy.active, data.attempts.first())
        assertEquals(25, data.activeSession?.questionCount)
        ReadingEngine.validate(data)
    }

    @Test fun prizesUseWholeSessionAccuracyAndStartAndFinishRewardPreferences() {
        for (enabledAtStart in listOf(false, true)) for (enabledAtFinish in listOf(false, true)) {
            var data = readingSessionFixture(count = 25, rewardsEnabled = enabledAtStart)
            repeat(24) {
                data = ReadingEngine.nextSessionActivity(readingSolve(data))
            }
            data = readingAnswer(data, readingCorrect(data))
            val disabled = RewardType.entries.toSet() - RewardType.LOLLIPOP
            data = ReadingEngine.finish(data, rewardsEnabled = enabledAtFinish, disabledRewards = disabled)
            assertEquals(if (enabledAtStart && enabledAtFinish) RewardType.LOLLIPOP else null, data.activeSession?.prizeType)
            assertTrue(data.attempts.all { it.checkpoints.values.none { c -> c.rewardGranted } })
            ReadingEngine.validate(data)
        }
        var data = readingSessionFixture(count = 50)
        repeat(50) { index ->
            data = if (index < 5) readingAnswer(data, readingWrong(data)) else readingSolve(data)
            data = ReadingEngine.nextSessionActivity(data)
        }
        assertEquals(45, data.sessionCorrect(requireNotNull(data.activeSession)))
        assertNotNull(data.activeSession?.finishedAt)
        assertNull(data.activeSession?.prizeType)
        ReadingEngine.validate(data)
    }

    @Test fun invalidSessionMembershipCountsResultsAndPrizesAreRejected() {
        val data = readingSessionFixture()
        val session = requireNotNull(data.activeSession)
        val invalid = listOf(
            data.copy(sessions = listOf(session, session)),
            data.copy(sessions = listOf(session.copy(attemptIds = emptyList()))),
            data.copy(sessions = listOf(session.copy(attemptIds = List(15) { session.attemptIds.first() }))),
            data.copy(sessions = listOf(session.copy(attemptIds = session.attemptIds.dropLast(1) + "missing"))),
            data.copy(sessions = listOf(session.copy(ownerId = "reader-b"))),
            data.copy(sessions = listOf(session.copy(game = ReadingGame.STUDIO))),
            data.copy(sessions = listOf(session.copy(prizeType = RewardType.LOLLIPOP))),
            data.copy(sessions = listOf(session.copy(finishedAt = session.startedAt))),
            data.copy(sessions = listOf(session.copy(timedOut = true))),
            data.copy(sessions = listOf(session.copy(elapsedMs = -1)))
        )
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { ReadingEngine.validate(it) } }
        for (count in listOf(Int.MIN_VALUE, -1, 0, 1001, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { readingSessionFixture(count = count) }
        }
    }
}
