package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReadingPackSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var store: ReadingSqliteStore
    private val models = ViewModelStore()
    private val pool get() = ReadingContent.forGame(ReadingGame.MISSING, ReadingLevel.SPROUT)

    @Before fun setup() {
        application.deleteDatabase("pack-ui.db")
        store = ReadingSqliteStore(application, ReadingDatabase(application, "pack-ui.db"),
            File(application.filesDir, "no-legacy-pack-ui.json"))
        val data = ReadingEngine.addProfile(store.load(), "Ari", "reader-a")
        store.save(data)
    }

    @After fun close() { models.clear(); store.database.close() }

    private fun model(importer: ReadingPackImporter) =
        ReadingViewModel(application, store, Dispatchers.Unconfined, importer).also { models.put("reading", it) }

    @Test fun settingsImportsAndShowsVersionAndCountsThenActivitiesWorkOffline() {
        val model = model(readingTestImporter(store.catalog, application.cacheDir, pool))
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) { ReadingPackSettings(model) }
        }
        compose.onNodeWithText("No reading pack installed.").assertIsDisplayed()
        compose.onNodeWithTag("reading-pack-import").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { model.packStatus?.version == "test-1" && !model.importing }
        compose.onNodeWithText("Installed version: test-1 · 3 activities").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reading activities are ready to play offline.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { model.start(ReadingGame.MISSING) }
        compose.waitUntil(5_000) { model.data?.active != null && !model.saving }
        val first = requireNotNull(model.data?.active)
        assertNotNull(first.loadedContent)
        assertEquals(first, store.load().active)
    }

    @Test fun failedImportKeepsInstalledLibraryAndReaderProgress() {
        runBlocking { readingTestImporter(store.catalog, application.cacheDir, pool).importPack() }
        val original = store.start(store.load(), ReadingGame.MISSING, ReadingLevel.SPROUT, false)
        val model = model(ReadingPackImporter(store.catalog, application.cacheDir,
            ReadingPackTransport { _, _ -> throw IOException("Offline") }))
        compose.setContent { ReadingPackSettings(model) }
        compose.onNodeWithTag("reading-pack-import").performClick()
        compose.waitUntil(5_000) { !model.importing && model.importProgress?.startsWith("Import failed.") == true }
        assertEquals(original, store.load())
        assertEquals(3, model.packStatus?.counts?.values?.sum())
        assertNull(model.storageError)
        compose.onNodeWithTag("reading-pack-import").assertIsEnabled()
    }

    @Test fun emptyMenuGuidesImportAndRapidStartsAssignOnlyOneActivity() {
        val model = model(readingTestImporter(store.catalog, application.cacheDir, pool))
        compose.setContent { ReadingModule(model, {}, 100, {}) }
        compose.onNodeWithText("Import reading activities in Settings > Reading Settings to begin. Saved drafts can still be resumed.")
            .performScrollTo().assertIsDisplayed()
        compose.runOnIdle { model.start(ReadingGame.MISSING) }
        compose.waitUntil(5_000) { model.message?.contains("Import reading activities") == true }
        assertTrue(requireNotNull(model.data).attempts.isEmpty())
        compose.runOnIdle { model.importActivities() }
        compose.waitUntil(10_000) { !model.importing && model.packStatus?.version == "test-1" }
        compose.runOnIdle { repeat(10) { model.start(ReadingGame.MISSING) } }
        compose.waitUntil(5_000) { model.data?.active != null && !model.saving }
        compose.runOnIdle { assertEquals(1, requireNotNull(model.data).attempts.size) }
        assertEquals(1, store.load().attempts.size)
    }
}
