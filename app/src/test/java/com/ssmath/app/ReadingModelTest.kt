package com.ssmath.app

import org.junit.Assert.*
import org.junit.Test

internal fun readingFixture(content: ReadingActivity = ReadingContent.activities.first()): ReadingData =
    ReadingData(
        profiles = listOf(ReadingProfile("reader-a", "Ari", activeAttemptId = "attempt-a"),
            ReadingProfile("reader-b", "Bo")),
        selectedProfileId = "reader-a", page = ReadingPage.ACTIVITY,
        attempts = listOf(ReadingAttempt("attempt-a", "reader-a", content.id, 1_700_000_000_000))
    )

internal fun readingAnswer(data: ReadingData, index: Int, key: String = "answer", rewardsEnabled: Boolean = true) =
    ReadingEngine.submit(ReadingEngine.field(data, "answer", index.toString()), key, 1_700_000_000_100,
        rewardsEnabled = rewardsEnabled)

internal fun readingCorrect(data: ReadingData): Int =
    requireNotNull(ReadingEngine.question(requireNotNull(data.active))).answer

internal fun readingWrong(data: ReadingData): Int =
    (readingCorrect(data) + 1) % requireNotNull(ReadingEngine.question(requireNotNull(data.active))).choices.size

internal fun readingOrder(data: ReadingData, order: List<Int>): ReadingData {
    var result = data
    while (requireNotNull(result.active).order.isNotEmpty())
        result = ReadingEngine.removeTile(result, requireNotNull(result.active).order.first())
    order.forEach { result = ReadingEngine.placeTile(result, it) }
    return result
}

internal fun readingSolve(data: ReadingData, rewardsEnabled: Boolean = true): ReadingData {
    var result = data
    val content = requireNotNull(data.active).content
    when (content.game) {
        ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.DETECTIVE ->
            result = readingAnswer(result, requireNotNull(content.question).answer, rewardsEnabled = rewardsEnabled)
        ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
            result = ReadingEngine.submit(readingOrder(result, content.validOrders.first()), "order",
                rewardsEnabled = rewardsEnabled)
            result = readingAnswer(result, requireNotNull(content.question).answer,
                if (content.game == ReadingGame.SCRAMBLE) "bonus" else "answer", rewardsEnabled)
        }
        ReadingGame.CHAPTER -> {
            result = ReadingEngine.branch(result, 0)
            result = readingAnswer(result, content.branches[0].question.answer, rewardsEnabled = rewardsEnabled)
        }
        ReadingGame.STUDIO -> ReadingEngine.studioKeys.forEachIndexed { index, key ->
            result = ReadingEngine.submit(ReadingEngine.field(result, key, content.writingChoices[index][0]), key,
                rewardsEnabled = rewardsEnabled)
        }
        ReadingGame.PARTNERS -> {
            result = ReadingEngine.partner(result, "reader-b")
            repeat(4) { index ->
                result = ReadingEngine.submit(ReadingEngine.field(result, "turn", "Sentence ${index + 1}.",
                    expectedTurn = index), "turn", expectedTurn = index, rewardsEnabled = rewardsEnabled)
            }
            result = ReadingEngine.reread(readingOrder(result, listOf(0, 1, 2, 3)))
            result = ReadingEngine.submit(result, "shared", rewardsEnabled = rewardsEnabled)
        }
    }
    if (content.game == ReadingGame.DETECTIVE)
        result = ReadingEngine.submit(ReadingEngine.field(result, "evidence", content.evidence.toString()), "evidence",
            rewardsEnabled = rewardsEnabled)
    return ReadingEngine.finish(result, 1_700_000_000_200)
}

class ReadingModelTest {
    @Test fun everyGameHasThreeOriginalActivitiesAtEachLevelWithValidFiniteContent() {
        assertEquals(48, ReadingContent.activities.size)
        assertEquals(48, ReadingContent.activities.map { it.id }.distinct().size)
        ReadingGame.entries.forEach { game -> ReadingLevel.entries.forEach { level ->
            assertEquals("$game $level", 3, ReadingContent.forGame(game, level).size)
        } }
        ReadingContent.activities.forEach { c ->
            assertTrue(c.title.isNotBlank() && c.hint.isNotBlank())
            assertTrue(c.picture.symbol.isNotBlank() && c.picture.description.isNotBlank())
            (listOfNotNull(c.question) + c.branches.map { it.question }).forEach { q ->
                assertTrue(q.prompt.isNotBlank())
                assertTrue(q.choices.size in 2..4)
                assertEquals(q.choices.size, q.choices.distinct().size)
                assertTrue(q.answer in q.choices.indices)
            }
            c.validOrders.forEach { assertEquals(c.tiles.indices.toList(), it.sorted()) }
            when (c.game) {
                ReadingGame.MISSING -> assertTrue(c.passage.single().contains("___"))
                ReadingGame.SCRAMBLE -> assertTrue(c.validOrders.isNotEmpty())
                ReadingGame.SEQUENCE -> {
                    assertEquals(c.tiles.size, c.eventPictures.size)
                    assertFalse(c.tiles.indices.toList() in c.validOrders)
                }
                ReadingGame.DETECTIVE -> assertTrue(c.evidence in c.passage.indices)
                ReadingGame.CAPTION -> assertNotNull(c.question)
                ReadingGame.CHAPTER -> {
                    assertTrue(c.branches.size >= 2)
                    assertEquals(c.branches.size, c.branches.map { it.text }.distinct().size)
                }
                ReadingGame.STUDIO, ReadingGame.PARTNERS -> {
                    assertEquals(4, c.writingChoices.size)
                    assertTrue(c.writingChoices.all { choices -> choices.size >= 3 && choices.all { it.isNotBlank() } })
                }
            }
            ReadingGame.entries.take(6).forEach { game ->
                val answers = ReadingContent.activities.filter { it.game == game }
                    .flatMap { listOfNotNull(it.question) + it.branches.map { b -> b.question } }
                assertTrue("$game must vary answer positions", answers.map { it.answer }.distinct().size > 1)
            }
        }
    }

    @Test fun everyActivityCanBeCompletedWithOnlyItsIntendedPoints() {
        ReadingContent.activities.forEach { content ->
            val result = readingSolve(readingFixture(content))
            ReadingEngine.validate(result)
            val expected = when (content.game) {
                ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.CHAPTER, ReadingGame.PARTNERS -> 1
                ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE, ReadingGame.DETECTIVE -> 2
                ReadingGame.STUDIO -> 4
            }
            assertEquals(content.id, expected, result.points("reader-a"))
            assertEquals(content.id, 1, result.completions("reader-a"))
            assertTrue(requireNotNull(result.active).finishedAt != null)
            assertEquals(result, ReadingEngine.finish(result))
            ReadingEngine.requiredKeys(requireNotNull(result.active)).forEach {
                assertEquals(result, ReadingEngine.submit(result, it))
            }
        }
    }

    @Test fun disabledRewardsKeepEveryActivityCheckpointAccuracyMilestoneAndStoryWithoutPoints() {
        ReadingContent.activities.forEach { content ->
            val result = readingSolve(readingFixture(content), rewardsEnabled = false)
            val rewarded = readingSolve(readingFixture(content))
            ReadingEngine.validate(result)
            assertTrue(content.id, requireNotNull(result.active).checkpoints.values.all { it.earned && !it.awardedPoint })
            assertEquals(content.id, rewarded.active?.checkpoints?.keys, result.active?.checkpoints?.keys)
            assertEquals(content.id, rewarded.stories, result.stories)
            listOf("reader-a", "reader-b").forEach { id ->
                assertEquals(content.id, 0, result.points(id))
                assertEquals(content.id, 0, result.sharedPoints(id))
                assertEquals(content.id, rewarded.completions(id), result.completions(id))
                assertEquals(content.id, rewarded.sharedCompletions(id), result.sharedCompletions(id))
                assertEquals(content.id, rewarded.milestones(id), result.milestones(id))
                assertEquals(content.id, rewarded.accuracy(id), result.accuracy(id))
            }
            assertEquals(result, ReadingEngine.finish(result))
            result.active?.checkpoints?.keys?.forEach { key ->
                assertEquals(result, ReadingEngine.submit(result, key, rewardsEnabled = true))
            }
        }
    }

    @Test fun onlyTheSuccessfulSubmissionDeterminesWhetherRetryEarnsPoints() {
        listOf(false, true).forEach { enabledAtCompletion ->
            var data = readingAnswer(readingFixture(), readingWrong(readingFixture()),
                rewardsEnabled = !enabledAtCompletion)
            assertEquals(ReadingAccuracy(0, 1, 0, 0), data.accuracy("reader-a"))
            assertEquals(0, data.points("reader-a"))
            data = readingAnswer(ReadingEngine.hint(data), readingCorrect(data),
                rewardsEnabled = enabledAtCompletion)
            assertEquals(ReadingAccuracy(0, 1, 1, 1), data.accuracy("reader-a"))
            assertEquals(if (enabledAtCompletion) 1 else 0, data.points("reader-a"))
            assertEquals(enabledAtCompletion, data.active?.checkpoints?.get("answer")?.awardedPoint)
            assertTrue(requireNotNull(data.active).earned("answer"))
            assertTrue(requireNotNull(data.active).feedback.contains("Checkpoint saved"))
            assertEquals(enabledAtCompletion, requireNotNull(data.active).feedback.contains("+1 point"))
            repeat(5) {
                assertEquals(data, ReadingEngine.submit(data, "answer", rewardsEnabled = !enabledAtCompletion))
            }
            assertEquals(2, data.active?.checkpoints?.get("answer")?.tries)
            assertEquals(1, ReadingEngine.finish(data).completions("reader-a"))
        }
    }

    @Test fun switchingRewardsAcrossAResumedStoryKeepsOldPointsAndNeverRetrocreditsDisabledCheckpoints() {
        val content = ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first()
        var data = readingFixture(content)
        ReadingEngine.studioKeys.forEachIndexed { index, key ->
            val enabled = index % 2 == 0
            data = ReadingEngine.submit(ReadingEngine.field(data, key, content.writingChoices[index][0]), key,
                rewardsEnabled = enabled)
            assertEquals(if (index < 2) 1 else 2, data.points("reader-a"))
            assertEquals(enabled, data.active?.checkpoints?.get(key)?.awardedPoint)
            assertEquals(data, ReadingEngine.submit(data, key, rewardsEnabled = !enabled))
            data = ReadingEngine.selectProfile(ReadingEngine.selectProfile(data, "reader-b"), "reader-a")
            data = ReadingEngine.start(data, content.game, content.level)
            assertEquals("attempt-a", data.active?.id)
        }
        data = ReadingEngine.finish(data)
        assertEquals(2, data.points("reader-a"))
        assertEquals(1, data.completions("reader-a"))
        assertEquals("attempt-a", data.stories.single().attemptId)
        assertEquals(4, data.active?.checkpoints?.size)
        ReadingEngine.validate(data)
    }

    @Test fun partnerParticipationAndSharedCompletionStaySeparateFromAwardedPoints() {
        val content = ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first()
        var data = ReadingEngine.partner(readingFixture(content), "reader-b")
        repeat(4) { turn ->
            data = ReadingEngine.submit(ReadingEngine.field(data, "turn", "Sentence $turn.", expectedTurn = turn),
                "turn", expectedTurn = turn, rewardsEnabled = turn in 1..2)
            assertEquals(data, ReadingEngine.submit(data, "turn", expectedTurn = turn, rewardsEnabled = true))
        }
        assertEquals(0, data.points("reader-a"))
        assertEquals(1, data.points("reader-b"))
        assertTrue(requireNotNull(data.active).earned("participation:reader-a"))
        data = ReadingEngine.submit(ReadingEngine.reread(readingOrder(data, listOf(0, 1, 2, 3))),
            "shared", rewardsEnabled = false)
        assertTrue(requireNotNull(data.active).feedback.contains("Checkpoint saved"))
        assertFalse(requireNotNull(data.active).feedback.contains(" point"))
        assertEquals(data, ReadingEngine.submit(data, "shared", rewardsEnabled = true))
        data = ReadingEngine.finish(data)
        listOf("reader-a", "reader-b").forEach { id ->
            assertEquals(0, data.sharedPoints(id))
            assertEquals(1, data.sharedCompletions(id))
            assertTrue(data.milestones(id).contains("Reading together · completed a shared story"))
        }
        assertEquals(1, data.stories.size)
        ReadingEngine.validate(data)
    }

    @Test fun everyExplicitScrambleOrderIsAcceptedAndUnlistedOrdersAreNot() {
        ReadingContent.activities.filter { it.game == ReadingGame.SCRAMBLE }.forEach { content ->
            content.validOrders.forEach { order ->
                val result = ReadingEngine.submit(readingOrder(readingFixture(content), order), "order")
                assertEquals(1, result.points("reader-a"))
                assertTrue(requireNotNull(result.active).earned("order"))
            }
            val incorrect = content.tiles.indices.reversed().toList()
            if (incorrect !in content.validOrders) {
                val result = ReadingEngine.submit(readingOrder(readingFixture(content), incorrect), "order")
                assertEquals(0, result.points("reader-a"))
                assertEquals(1, result.accuracy("reader-a").attempted)
            }
        }
        assertTrue(ReadingContent.activities.count { it.validOrders.size > 1 } >= 4)
    }

    @Test fun branchChoiceEarnsNothingAndQuestionsComeFromTheChosenBranch() {
        ReadingContent.activities.filter { it.game == ReadingGame.CHAPTER }.forEach { content ->
            content.branches.forEachIndexed { index, branch ->
                var data = ReadingEngine.branch(readingFixture(content), index)
                assertEquals(0, data.points("reader-a"))
                assertEquals(branch.question, ReadingEngine.question(requireNotNull(data.active)))
                assertEquals(data, ReadingEngine.branch(data, (index + 1) % content.branches.size))
                data = readingAnswer(data, (branch.question.answer + 1) % branch.question.choices.size)
                assertEquals(0, data.points("reader-a"))
                data = readingAnswer(data, branch.question.answer)
                assertEquals(1, data.points("reader-a"))
                assertEquals(0, data.accuracy("reader-a").firstIndependent)
                assertEquals(1, data.accuracy("reader-a").retriedCompletions)
            }
        }
    }

    @Test fun wrongFirstTryIsCountedImmediatelyAndRetriesNeverSubtractOrBecomeIndependent() {
        var data = readingAnswer(readingFixture(), readingWrong(readingFixture()))
        assertEquals(ReadingAccuracy(0, 1, 0, 0), data.accuracy("reader-a"))
        assertEquals(0, data.points("reader-a"))
        data = readingAnswer(data, readingCorrect(data))
        assertEquals(ReadingAccuracy(0, 1, 0, 1), data.accuracy("reader-a"))
        assertEquals(1, data.points("reader-a"))
        repeat(5) { data = ReadingEngine.submit(data, "answer") }
        assertEquals(2, requireNotNull(data.active).checkpoints.getValue("answer").tries)
        assertEquals(1, data.points("reader-a"))
    }

    @Test fun hintsAndSuccessfullyQueuedNarrationPreventIndependentCreditButFailedSpeechDoesNot() {
        var hinted = ReadingEngine.hint(readingFixture())
        hinted = readingAnswer(hinted, readingCorrect(hinted))
        assertEquals(ReadingAccuracy(0, 1, 1, 0), hinted.accuracy("reader-a"))
        var narrated = ReadingEngine.narration(readingFixture(), queued = true)
        narrated = readingAnswer(narrated, readingCorrect(narrated))
        assertEquals(ReadingAccuracy(0, 1, 1, 0), narrated.accuracy("reader-a"))
        val failedSpeech = readingAnswer(ReadingEngine.narration(readingFixture(), queued = false), readingCorrect(readingFixture()))
        assertEquals(ReadingAccuracy(1, 1, 0, 0), failedSpeech.accuracy("reader-a"))
        assertEquals(failedSpeech.accuracy("reader-a"),
            ReadingEngine.narration(ReadingEngine.hint(failedSpeech), true).accuracy("reader-a"))
        val both = readingAnswer(ReadingEngine.hint(readingAnswer(readingFixture(), readingWrong(readingFixture()))),
            readingCorrect(readingFixture()))
        assertEquals(ReadingAccuracy(0, 1, 1, 1), both.accuracy("reader-a"))
    }

    @Test fun sequencingComprehensionAndEvidenceRequireTheirEarlierCheckpoints() {
        val sequence = readingFixture(ReadingContent.find("sequence-seed"))
        val earlyAnswer = readingAnswer(sequence, readingCorrect(sequence))
        assertEquals(0, earlyAnswer.points("reader-a"))
        assertFalse(requireNotNull(earlyAnswer.active).checkpoints.containsKey("answer"))
        val detective = ReadingEngine.field(readingFixture(ReadingContent.find("detective-bag")), "evidence", "1")
        assertEquals(detective, ReadingEngine.submit(detective, "evidence"))
    }

    @Test fun completedCheckpointsFreezeAnswersOrdersAndWriting() {
        val missing = readingAnswer(readingFixture(), readingCorrect(readingFixture()))
        assertEquals(missing, ReadingEngine.field(missing, "answer", readingWrong(missing).toString()))
        var sequence = readingFixture(ReadingContent.find("sequence-seed"))
        sequence = ReadingEngine.submit(readingOrder(sequence, listOf(1, 2, 0)), "order")
        assertEquals(sequence, ReadingEngine.removeTile(sequence, 0))
        var studio = readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first())
        studio = ReadingEngine.submit(ReadingEngine.field(studio, "character", "A fox"), "character")
        assertEquals(studio, ReadingEngine.field(studio, "character", "A dog"))
    }

    @Test fun tileRemovalUsesStableIdentityForRepeatedAndDistinctQueuedTaps() {
        var data = readingOrder(readingFixture(ReadingContent.find("sequence-seed")), listOf(1, 2, 0))
        data = ReadingEngine.removeTile(data, 1)
        assertEquals(listOf(2, 0), data.active?.order)
        assertEquals(data, ReadingEngine.removeTile(data, 1))
        data = ReadingEngine.removeTile(data, 0)
        assertEquals(listOf(2), data.active?.order)
    }

    @Test fun profilesAreTrimmedValidatedAndDraftsStayIsolatedAcrossSwitches() {
        var data = ReadingEngine.addProfile(ReadingData(), "  Ari  ", "a")
        assertEquals("Ari", data.profile?.name)
        assertThrows(IllegalArgumentException::class.java) { ReadingEngine.addProfile(data, "  ") }
        assertThrows(IllegalArgumentException::class.java) { ReadingEngine.addProfile(data, "ari") }
        data = ReadingEngine.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, id = "draft")
        data = ReadingEngine.field(data, "answer", "0")
        data = ReadingEngine.addProfile(data, "Bo", "b")
        assertNull(data.active)
        assertNull(ReadingEngine.draft(data, ReadingGame.MISSING, ReadingLevel.SPROUT))
        assertEquals(0, data.points("b"))
        data = ReadingEngine.selectProfile(data, "a")
        assertEquals("0", data.active?.fields?.get("answer"))
        assertEquals("draft", ReadingEngine.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT).active?.id)
    }

    @Test fun explicitNewAttemptArchivesOldDraftWithoutLosingCreditOrItsFirstTry() {
        var data = readingAnswer(readingFixture(), readingCorrect(readingFixture()))
        val resumed = ReadingEngine.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT)
        assertEquals("attempt-a", resumed.active?.id)
        data = ReadingEngine.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, true, "new-attempt")
        assertTrue(data.attempts.first().abandoned)
        assertEquals(1, data.points("reader-a"))
        assertEquals("new-attempt", data.active?.id)
        data = readingAnswer(data, requireNotNull(data.active?.content?.question).answer)
        assertEquals(2, data.points("reader-a"))
    }

    @Test fun partnersMustBeDistinctAlternateAndNeverReceiveEachOthersCredit() {
        var data = readingFixture(ReadingContent.forGame(ReadingGame.PARTNERS, ReadingLevel.SPROUT).first())
        assertThrows(IllegalArgumentException::class.java) { ReadingEngine.partner(data, "reader-a") }
        assertThrows(IllegalArgumentException::class.java) { ReadingEngine.partner(data, "missing") }
        data = ReadingEngine.partner(data, "reader-b")
        repeat(4) { index ->
            data = ReadingEngine.submit(ReadingEngine.field(data, "turn", "Turn $index.", expectedTurn = index),
                "turn", expectedTurn = index)
            val repeated = ReadingEngine.submit(data, "turn", expectedTurn = index)
            assertEquals(data.active?.turns, repeated.active?.turns)
            assertEquals(data.active?.checkpoints, repeated.active?.checkpoints)
            assertEquals(index + 1, data.active?.turns?.size)
        }
        assertEquals(listOf("reader-a", "reader-b", "reader-a", "reader-b"), data.active?.turns?.map { it.readerId })
        assertEquals(1, data.points("reader-a"))
        assertEquals(1, data.points("reader-b"))
        assertEquals(0, data.sharedCompletions("reader-a"))
        assertEquals(0, data.accuracy("reader-a").attempted)
        data = readingOrder(data, listOf(0, 1, 2, 3))
        assertEquals(data, ReadingEngine.submit(data, "shared"))
        data = ReadingEngine.submit(ReadingEngine.reread(data), "shared")
        data = ReadingEngine.finish(data)
        assertEquals(1, data.sharedCompletions("reader-a"))
        assertEquals(1, data.sharedCompletions("reader-b"))
        assertEquals(1, data.sharedPoints("reader-a"))
        assertEquals(1, data.sharedPoints("reader-b"))
        assertEquals(data, ReadingEngine.submit(data, "shared", rewardsEnabled = false))
        assertEquals(1, data.points("reader-a"))
        assertEquals(1, data.points("reader-b"))
        assertEquals(1, data.stories.size)
        assertEquals(listOf("reader-a", "reader-b"), data.stories.single().readerIds)
        ReadingEngine.validate(data)
    }
}
