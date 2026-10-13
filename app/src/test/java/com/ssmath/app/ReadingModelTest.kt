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

internal fun readingAnswer(data: ReadingData, index: Int, key: String = "answer") =
    ReadingEngine.submit(ReadingEngine.field(data, "answer", index.toString()), key, 1_700_000_000_100)

internal fun readingOrder(data: ReadingData, order: List<Int>): ReadingData {
    var result = data
    while (requireNotNull(result.active).order.isNotEmpty()) result = ReadingEngine.removeTile(result, 0)
    order.forEach { result = ReadingEngine.placeTile(result, it) }
    return result
}

internal fun readingSolve(data: ReadingData): ReadingData {
    var result = data
    val content = requireNotNull(data.active).content
    when (content.game) {
        ReadingGame.MISSING, ReadingGame.CAPTION, ReadingGame.DETECTIVE ->
            result = readingAnswer(result, requireNotNull(content.question).answer)
        ReadingGame.SCRAMBLE, ReadingGame.SEQUENCE -> {
            result = ReadingEngine.submit(readingOrder(result, content.validOrders.first()), "order")
            result = readingAnswer(result, requireNotNull(content.question).answer,
                if (content.game == ReadingGame.SCRAMBLE) "bonus" else "answer")
        }
        ReadingGame.CHAPTER -> {
            result = ReadingEngine.branch(result, 0)
            result = readingAnswer(result, content.branches[0].question.answer)
        }
        ReadingGame.STUDIO -> ReadingEngine.studioKeys.forEachIndexed { index, key ->
            result = ReadingEngine.submit(ReadingEngine.field(result, key, content.writingChoices[index][0]), key)
        }
        ReadingGame.PARTNERS -> {
            result = ReadingEngine.partner(result, "reader-b")
            repeat(4) { index ->
                result = ReadingEngine.submit(ReadingEngine.field(result, "turn", "Sentence ${index + 1}."), "turn")
            }
            result = ReadingEngine.reread(readingOrder(result, listOf(0, 1, 2, 3)))
            result = ReadingEngine.submit(result, "shared")
        }
    }
    if (content.game == ReadingGame.DETECTIVE)
        result = ReadingEngine.submit(ReadingEngine.field(result, "evidence", content.evidence.toString()), "evidence")
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
        var data = readingAnswer(readingFixture(), 0)
        assertEquals(ReadingAccuracy(0, 1, 0, 0), data.accuracy("reader-a"))
        assertEquals(0, data.points("reader-a"))
        data = readingAnswer(data, 2)
        assertEquals(ReadingAccuracy(0, 1, 0, 1), data.accuracy("reader-a"))
        assertEquals(1, data.points("reader-a"))
        repeat(5) { data = ReadingEngine.submit(data, "answer") }
        assertEquals(2, requireNotNull(data.active).checkpoints.getValue("answer").tries)
        assertEquals(1, data.points("reader-a"))
    }

    @Test fun hintsAndSuccessfullyQueuedNarrationPreventIndependentCreditButFailedSpeechDoesNot() {
        var hinted = ReadingEngine.hint(readingFixture())
        hinted = readingAnswer(hinted, 2)
        assertEquals(ReadingAccuracy(0, 1, 1, 0), hinted.accuracy("reader-a"))
        var narrated = ReadingEngine.narration(readingFixture(), queued = true)
        narrated = readingAnswer(narrated, 2)
        assertEquals(ReadingAccuracy(0, 1, 1, 0), narrated.accuracy("reader-a"))
        val failedSpeech = readingAnswer(ReadingEngine.narration(readingFixture(), queued = false), 2)
        assertEquals(ReadingAccuracy(1, 1, 0, 0), failedSpeech.accuracy("reader-a"))
        assertEquals(failedSpeech.accuracy("reader-a"),
            ReadingEngine.narration(ReadingEngine.hint(failedSpeech), true).accuracy("reader-a"))
        val both = readingAnswer(ReadingEngine.hint(readingAnswer(readingFixture(), 0)), 2)
        assertEquals(ReadingAccuracy(0, 1, 1, 1), both.accuracy("reader-a"))
    }

    @Test fun sequencingComprehensionAndEvidenceRequireTheirEarlierCheckpoints() {
        val sequence = readingFixture(ReadingContent.find("sequence-seed"))
        val earlyAnswer = readingAnswer(sequence, 2)
        assertEquals(0, earlyAnswer.points("reader-a"))
        assertFalse(requireNotNull(earlyAnswer.active).checkpoints.containsKey("answer"))
        val detective = ReadingEngine.field(readingFixture(ReadingContent.find("detective-bag")), "evidence", "1")
        assertEquals(detective, ReadingEngine.submit(detective, "evidence"))
    }

    @Test fun completedCheckpointsFreezeAnswersOrdersAndWriting() {
        val missing = readingAnswer(readingFixture(), 2)
        assertEquals(missing, ReadingEngine.field(missing, "answer", "0"))
        var sequence = readingFixture(ReadingContent.find("sequence-seed"))
        sequence = ReadingEngine.submit(readingOrder(sequence, listOf(1, 2, 0)), "order")
        assertEquals(sequence, ReadingEngine.removeTile(sequence, 0))
        var studio = readingFixture(ReadingContent.forGame(ReadingGame.STUDIO, ReadingLevel.SPROUT).first())
        studio = ReadingEngine.submit(ReadingEngine.field(studio, "character", "A fox"), "character")
        assertEquals(studio, ReadingEngine.field(studio, "character", "A dog"))
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
        var data = readingAnswer(readingFixture(), 2)
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
            data = ReadingEngine.submit(ReadingEngine.field(data, "turn", "Turn $index."), "turn")
            val repeated = ReadingEngine.submit(data, "turn")
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
        assertEquals(1, data.points("reader-a"))
        assertEquals(1, data.points("reader-b"))
        assertEquals(1, data.stories.size)
        assertEquals(listOf("reader-a", "reader-b"), data.stories.single().readerIds)
        ReadingEngine.validate(data)
    }
}
