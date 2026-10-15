package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingDatabaseBrowserTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val databaseName = "reading-browser-ui.db"
    private lateinit var database: ReadingDatabase
    private lateinit var catalog: ReadingCatalog
    private val models = ViewModelStore()
    private val first get() = ReadingContent.activities.first()

    @Before fun setup() {
        application.deleteDatabase(databaseName)
        database = ReadingDatabase(application, databaseName)
        catalog = ReadingCatalog(database)
    }

    @After fun cleanup() {
        compose.runOnIdle { models.clear() }
        database.close()
        application.deleteDatabase(databaseName)
    }

    private fun install(activities: List<ReadingActivity>) = runBlocking {
        readingTestImporter(catalog, application.cacheDir, activities).importPack()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun adminPanelOpensTheInstalledDatabaseAndBackReturnsToAdmin() {
        DebugLog.initialize(application)
        application.deleteDatabase("reading.db")
        ReadingDatabase(application).use { installed ->
            runBlocking { readingTestImporter(ReadingCatalog(installed), application.cacheDir, listOf(first)).importPack() }
        }
        val model = MathViewModel(application).also { models.put("math", it) }
        compose.setContent { MathTheme { SettingsScreen(model) } }
        compose.onNodeWithTag("reading-database-open").assertDoesNotExist()
        compose.onNodeWithText("Debug logging").performScrollTo().performClick()
        val description = compose.onNodeWithTag("debug-logging-description").performScrollTo()
        repeat(7) { description.performClick() }
        compose.onNodeWithText("Admin").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-open").performScrollTo().performClick()
        awaitTag("reading-database-row-${first.id}")
        compose.onNodeWithText("Read-only · installed activities").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-row-${first.id}").performClick()
        awaitTag("reading-database-detail")
        compose.onNodeWithText("Back to activities").performClick()
        awaitTag("reading-database-row-${first.id}")
        compose.onNodeWithText("Back to admin").performClick()
        compose.onNodeWithTag("reading-database-browser").assertDoesNotExist()
        compose.onNodeWithText("Admin").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-open").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Admin").assertDoesNotExist()
        ReadingDatabase(application).use { assertEquals(1, ReadingCatalog(it).status().counts.values.sum()) }
        application.deleteDatabase("reading.db")
    }

    @Test fun pagesInstalledActivitiesFiltersThemAndReturnsFromDetailWithoutChangingTheCatalog() {
        val entries = (0..44).map { first.copy(id = "entry-%02d".format(it), title = "Installed activity $it") }
        install(entries + ReadingContent.find("caption-cat").copy(id = "other-caption", level = ReadingLevel.TRAIL))
        val before = catalog.status()
        var closed = false
        compose.setContent { ReadingDatabaseBrowser({ closed = true }, databaseName) }
        awaitTag("reading-database-row-entry-00")
        compose.onNodeWithText("1–40 of 46 activities").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-previous").assertIsNotEnabled()
        compose.onNodeWithTag("reading-database-next").performClick()
        awaitTag("reading-database-row-entry-40")
        compose.onNodeWithText("41–46 of 46 activities").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-next").assertIsNotEnabled()
        compose.onNodeWithTag("reading-database-row-entry-40").performClick()
        awaitTag("reading-database-detail")
        compose.onNodeWithText("Installed activity 40").assertIsDisplayed()
        val question = requireNotNull(first.question)
        compose.onNodeWithText("Correct answer: ${question.answer + 1}. ${question.choices[question.answer]} " +
            "(index ${question.answer})").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reading-database-back").performClick()
        awaitTag("reading-database-row-entry-40")
        compose.onNodeWithTag("reading-database-previous").performClick()
        awaitTag("reading-database-row-entry-00")
        compose.onNodeWithTag("reading-database-game").performClick()
        compose.onNodeWithText(ReadingGame.CAPTION.title).performClick()
        awaitTag("reading-database-row-other-caption")
        compose.onNodeWithText("1–1 of 1 activities").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-level").performClick()
        compose.onNodeWithText(ReadingLevel.SPROUT.label).performClick()
        awaitTag("reading-database-empty")
        compose.onNodeWithText("No activities match these filters.").assertIsDisplayed()
        compose.onNodeWithTag("reading-database-back").performClick()
        compose.runOnIdle { assertTrue(closed); assertEquals(before, catalog.status()) }
    }

    @Test fun emptyInstalledDatabaseDoesNotFallBackToBundledContent() {
        catalog.insertLegacy(first)
        compose.setContent { ReadingDatabaseBrowser({}, databaseName, backLabel = "Back to test panel") }
        awaitTag("reading-database-empty")
        compose.onNodeWithText("Back to test panel").assertIsDisplayed()
        compose.onNodeWithText("No reading activities installed. Import reading activities in Reading Settings first.")
            .assertIsDisplayed()
        compose.onNodeWithTag("reading-database-next").assertIsNotEnabled()
        compose.onNodeWithTag("reading-database-previous").assertIsNotEnabled()
    }

    @Test fun failedPageCanBeRetriedAndMissingDetailsCanBeExited() {
        install(listOf(first))
        val original = requireNotNull(catalog.latest(first.id))
        database.writableDatabase.execSQL("UPDATE reading_content SET payload='invalid JSON'")
        compose.setContent { ReadingDatabaseBrowser({}, databaseName) }
        awaitTag("reading-database-error")
        compose.onNodeWithText("Unable to read the reading database. Please try again.").assertIsDisplayed()
        compose.runOnIdle {
            database.writableDatabase.execSQL("UPDATE reading_content SET payload=?",
                arrayOf(readingPackJson.encodeToString(first)))
        }
        compose.onNodeWithTag("reading-database-retry").performClick()
        awaitTag("reading-database-row-${first.id}")
        compose.runOnIdle { database.writableDatabase.execSQL("DELETE FROM reading_content") }
        compose.onNodeWithTag("reading-database-row-${first.id}").performClick()
        awaitTag("reading-database-error")
        compose.runOnIdle {
            database.writableDatabase.execSQL("INSERT INTO reading_content VALUES(?,?,?)",
                arrayOf(first.id, original.revision, readingPackJson.encodeToString(first)))
        }
        compose.onNodeWithTag("reading-database-retry").performClick()
        awaitTag("reading-database-detail")
        compose.onNodeWithTag("reading-database-back").performClick()
        awaitTag("reading-database-row-${first.id}")
    }

    @Test fun detailShowsPassageAndEvidenceWithExplicitIndices() {
        val activity = ReadingContent.activities.first { it.game == ReadingGame.DETECTIVE }
        compose.setContent { ReadingDatabaseDetail(activity, "test-revision") }
        compose.onNodeWithText("ID: ${activity.id}").assertIsDisplayed()
        compose.onNodeWithText("Revision: test-revision").assertIsDisplayed()
        compose.onNodeWithText("Sentence ${activity.evidence + 1} (index ${activity.evidence}): " +
            activity.passage[activity.evidence]).performScrollTo().assertIsDisplayed()
    }

    @Test fun detailShowsTilesAndAllCorrectOrders() {
        val activity = ReadingContent.activities.first { it.game == ReadingGame.SEQUENCE }
        compose.setContent { ReadingDatabaseDetail(activity, "test-revision") }
        activity.tiles.forEachIndexed { index, tile ->
            compose.onNodeWithText("${index + 1}. $tile").performScrollTo().assertIsDisplayed()
        }
        activity.validOrders.forEachIndexed { index, order ->
            compose.onNodeWithText("Order ${index + 1}: ${order.joinToString(" → ") { (it + 1).toString() }}")
                .performScrollTo().assertIsDisplayed()
        }
        activity.eventPictures.forEach {
            compose.onNodeWithText("${it.symbol} — ${it.description}").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun detailShowsAllWritingChoices() {
        val activity = ReadingContent.activities.first { it.game == ReadingGame.STUDIO }
        compose.setContent { ReadingDatabaseDetail(activity, "test-revision") }
        activity.writingChoices.forEach { choices ->
            choices.forEachIndexed { index, text ->
                compose.onNodeWithText("${index + 1}. $text").performScrollTo().assertIsDisplayed()
            }
        }
    }

}
