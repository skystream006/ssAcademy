package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal fun grammarExample(level: ReadingLevel = ReadingLevel.SPROUT) = ReadingActivity(
    id = "grammar-test-${level.name.lowercase()}", game = ReadingGame.GRAMMAR, level = level,
    title = "Find the action", picture = ReadingPicture("📚", "Books for a word explorer"),
    passage = listOf("The rabbit hops."),
    question = ReadingQuestion("Which word is a verb?", listOf("rabbit", "hops", "The"), 1),
    hint = "A verb can tell what someone or something does.",
    explanation = "Hops is a verb because it tells what the rabbit does."
)

internal fun grammarSession(count: Int = 15, rewards: Boolean = false): ReadingData {
    val content = grammarExample()
    val data = ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a")
    return ReadingEngine.startSession(data, ReadingGame.GRAMMAR, content.level, count, rewards,
        id = "grammar-session", now = 1_700_000_000_000,
        assigned = List(count) { index ->
            ReadingAttempt("grammar-attempt-$index", "reader-a", content.id, 1_700_000_000_000,
                activityRef = ReadingActivityRef("grammar-test", content.game, content.level, content.title),
                loadedContent = content)
        })
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GrammarGameTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var database: ReadingDatabase
    private lateinit var store: ReadingSqliteStore
    private lateinit var cache: File

    @Before fun setup() {
        application.deleteDatabase("grammar-test.db")
        database = ReadingDatabase(application, "grammar-test.db")
        store = ReadingSqliteStore(application, database, File(application.filesDir, "grammar-legacy.json"))
        cache = File(application.cacheDir, "grammar-pack-test").apply { mkdirs() }
    }

    @After fun cleanup() {
        database.close()
        application.deleteDatabase("grammar-test.db")
        cache.deleteRecursively()
    }

    @Test fun grammarRequiresPassageQuestionAndBoundedTeachingExplanation() {
        val activity = grammarExample()
        ReadingPackValidator.validate(activity)
        listOf(
            activity.copy(passage = emptyList()), activity.copy(question = null),
            activity.copy(explanation = ""), activity.copy(explanation = " "),
            activity.copy(explanation = "x".repeat(4_001)),
            activity.copy(question = activity.question!!.copy(answer = 8)),
            activity.copy(question = activity.question.copy(choices = listOf("hops", "hops"))),
            activity.copy(tiles = listOf("rabbit"))
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { ReadingPackValidator.validate(invalid) }
        }
        val legacy = ReadingContent.activities.first()
        val encoded = readingPackJson.encodeToString(legacy)
        assertEquals(legacy, readingPackJson.decodeFromString<ReadingActivity>(
            encoded.replace(",\"explanation\":\"\"", "")))
    }

    @Test fun answersTeachWithoutAllowingDuplicateCreditOrChangingWrongResponses() {
        val fresh = grammarSession()
        assertEquals(listOf("answer"), ReadingEngine.requiredKeys(requireNotNull(fresh.active)))
        for (value in listOf("", "-1", "3", "not-a-number")) {
            val invalid = ReadingEngine.submit(ReadingEngine.field(fresh, "answer", value), "answer")
            assertTrue(requireNotNull(invalid.active).checkpoints.isEmpty())
            assertNull(invalid.active?.finishedAt)
        }
        val hinted = ReadingEngine.hint(fresh)
        assertEquals(grammarExample().hint, hinted.active?.feedback)
        var correct = readingAnswer(hinted, readingCorrect(hinted))
        assertTrue(requireNotNull(correct.active).feedback.contains(grammarExample().explanation))
        assertFalse(correct.active!!.checkpoints.getValue("answer").independent)
        assertEquals(correct, ReadingEngine.submit(correct, "answer"))
        correct = ReadingEngine.finish(correct)
        assertEquals(1, correct.sessionCorrect(requireNotNull(correct.activeSession)))
        assertEquals(correct, ReadingEngine.finish(correct))
        val wrong = readingAnswer(fresh, readingWrong(fresh))
        assertTrue(requireNotNull(wrong.active).feedback.contains(grammarExample().explanation))
        assertNotNull(wrong.active?.finishedAt)
        assertEquals(1, wrong.sessionWrong(requireNotNull(wrong.activeSession)))
        assertEquals(wrong, readingAnswer(wrong, readingCorrect(wrong)))
        ReadingEngine.validate(correct)
        ReadingEngine.validate(wrong)
    }

    @Test fun grammarUsesExistingQuestionCountsWrongLimitAndRewardsRules() {
        for (count in listOf(15, 25, 50)) {
            var data = grammarSession(count, rewards = true)
            repeat(count / 10 + 1) {
                data = ReadingEngine.nextSessionActivity(readingAnswer(data, readingWrong(data)))
            }
            assertNotNull(data.activeSession?.finishedAt)
            assertEquals(count / 10 + 1, data.sessionWrong(requireNotNull(data.activeSession)))
            assertTrue(data.attempts.filter { it.finishedAt == null }.all { it.abandoned })
            ReadingEngine.validate(data)
        }
        for (rewards in listOf(false, true)) {
            var data = grammarSession(25, rewards)
            repeat(25) { data = ReadingEngine.nextSessionActivity(readingSolve(data)) }
            assertNotNull(data.activeSession?.finishedAt)
            assertEquals(25, data.sessionCorrect(requireNotNull(data.activeSession)))
            assertEquals(rewards, data.activeSession?.prizeType != null)
            assertEquals(data, ReadingEngine.finish(data))
            ReadingEngine.validate(data)
        }
    }

    @Test fun publishedGrammarHasFiftyQuestionsPerLevelAndEveryChoiceIsScored() {
        val pack = ReadingRepositoryPack("reading-activities-v8-import.json")
        val grammar = pack.activities.filter { it.game == ReadingGame.GRAMMAR }
        assertEquals(150, grammar.size)
        assertEquals(150, grammar.map { it.id }.distinct().size)
        assertEquals(15_150, pack.activities.size)
        val legacy = ReadingRepositoryPack()
        assertEquals(legacy.activities, pack.activities.filter { it.game != ReadingGame.GRAMMAR })
        for (level in ReadingLevel.entries) {
            val pool = grammar.filter { it.level == level }
            assertEquals(50, pool.size)
            assertEquals(50, pool.map { it.passage to it.question!!.prompt }.distinct().size)
            pool.forEach { activity ->
                ReadingPackValidator.validate(activity)
                val question = requireNotNull(activity.question)
                val fresh = ReadingEngine.start(
                    ReadingEngine.addProfile(ReadingData(), "Ari", "reader-a"),
                    activity.game, level, selected = activity,
                    activityRef = ReadingActivityRef("test", activity.game, level, activity.title))
                question.choices.indices.forEach { choice ->
                    val result = readingAnswer(fresh, choice)
                    assertEquals(activity.id, choice == question.answer, result.active!!.earned("answer"))
                    assertTrue(result.active!!.feedback.contains(activity.explanation))
                }
                assertNotNull(readingSolve(fresh).active?.finishedAt)
            }
        }
    }

    @Test fun importingGrammarPreservesOldDraftAndReservesFiftyUniqueQuestionsOffline() {
        val oldPack = ReadingRepositoryPack()
        runBlocking { oldPack.importer(store.catalog, cache).importPack() }
        var data = ReadingEngine.addProfile(store.load(), "Ari", "reader-a")
        store.save(data)
        data = store.start(data, ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        val oldDraft = requireNotNull(data.active)
        val pack = ReadingRepositoryPack("reading-activities-v8-import.json")
        runBlocking { pack.importer(store.catalog, cache).importPack() }
        assertEquals(oldDraft, store.load().active)
        for (level in ReadingLevel.entries) {
            data = store.startSession(data, ReadingGame.GRAMMAR, level, 50, false)
            val session = requireNotNull(data.activeSession)
            val assigned = data.attempts.filter { it.id in session.attemptIds }
            assertEquals(50, assigned.map { it.contentId }.distinct().size)
            val first = requireNotNull(data.active)
            data = ReadingEngine.field(data, "answer", readingCorrect(data).toString())
            store.save(data)
            database.close()
            database = ReadingDatabase(application, "grammar-test.db")
            store = ReadingSqliteStore(application, database)
            data = store.load()
            assertEquals(assigned.map { it.id }, data.activeSession?.attemptIds)
            assertEquals(first.content, data.active?.content)
            assertEquals(readingCorrect(data).toString(), data.active?.fields?.get("answer"))
            data = readingSolve(data)
            store.save(data)
            val completed = requireNotNull(data.active)
            data = store.prepare(ReadingEngine.nextSessionActivity(data))
            store.save(data)
            val reviewed = store.review(completed.copy(loadedContent = null))
            assertEquals(completed.content.explanation, reviewed.content.explanation)
            assertEquals(completed.fields, reviewed.fields)
            assertEquals(1, data.sessionCorrect(session))
        }
        assertEquals(oldDraft.content, store.review(oldDraft.copy(loadedContent = null)).content)
    }
}
