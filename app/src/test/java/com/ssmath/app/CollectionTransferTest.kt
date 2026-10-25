package com.ssmath.app

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class CollectionTransferTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val historyFile get() = File(application.filesDir, "practice_history.json")
    private val document get() = File(application.cacheDir, "collections-test.json")
    private val blockedWrite get() = File(application.filesDir, "practice_history.json.tmp")
    private val models = ViewModelStore()
    private lateinit var restoration: StateRestorationTester
    private lateinit var model: MathViewModel
    private var requestCode = 0
    private var launchedIntent: Intent? = null
    private var missingPicker = false
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I,
                                     options: ActivityOptionsCompat?) {
            if (missingPicker) throw ActivityNotFoundException()
            this@CollectionTransferTest.requestCode = requestCode
            launchedIntent = contract.createIntent(application, input)
        }
    }
    private val backup = CollectionBackup(CollectionBackup.FORMAT, CollectionBackup.VERSION,
        mapOf(RewardType.LOLLIPOP to RewardBalance(2, 2), RewardType.TOYS_3 to RewardBalance(3, 1)),
        setOf(Celebration.PIKACHU, Celebration.PARTY,
            Celebration.entries.first { it.category == CelebrationCategory.MEGA_POKEMONS }))

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        historyFile.delete()
        document.delete()
        blockedWrite.delete()
        DebugLog.initialize(application)
        HistoryStore(historyFile).addReward(RewardType.LOLLIPOP, fragment = true)
        HistoryStore(historyFile).collectPokemon(Celebration.SQUIRTLE)
        model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)
        models.put("model", model)
        restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }) {
                MathTheme { SettingsScreen(model) }
            }
        }
        compose.waitForIdle()
    }

    @After fun cleanup() {
        models.clear()
        blockedWrite.delete()
        document.delete()
    }

    private fun openTransfer() {
        compose.openAdminSettings()
        compose.onNodeWithTag("collection-transfer").performScrollTo().performClick()
        compose.onNodeWithTag("collection-transfer-dialog").assertIsDisplayed()
    }

    private fun selectDocument(cancelled: Boolean = false) {
        compose.runOnIdle {
            registry.dispatchResult(requestCode, if (cancelled) Activity.RESULT_CANCELED else Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(document)))
        }
        compose.waitForIdle()
    }

    private fun preview() {
        document.writeText(CollectionBackup.encode(backup))
        compose.onNodeWithTag("import-collections").performScrollTo().performClick()
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, launchedIntent?.action)
        selectDocument()
        compose.onNodeWithTag("collection-import-preview").performScrollTo().assertIsDisplayed()
    }

    @Test fun collectionTransferIsOnlyAvailableAfterUnlockingAdminAndCloseReturnsThere() {
        compose.onNodeWithTag("collection-transfer").assertDoesNotExist()
        compose.onNodeWithTag("admin-menu").performScrollTo().performClick()
        compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
        compose.onNodeWithTag("collection-transfer").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performClick()
        openTransfer()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("admin-dialog").assertIsDisplayed()
    }

    @Test fun exportUsesCreateDocumentAndWritesOnePortableFileWithoutChangingSource() {
        openTransfer()
        val before = historyFile.readText()
        document.writeText("stale data".repeat(1000))
        compose.onNodeWithTag("export-collections").performScrollTo().performClick()
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, launchedIntent?.action)
        assertEquals("application/json", launchedIntent?.type)
        assertEquals("ssAcademy-collections.json", launchedIntent?.getStringExtra(Intent.EXTRA_TITLE))
        selectDocument()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Collections exported.", substring = true)
        val exported = document.inputStream().use(CollectionBackup::read)
        assertEquals(CollectionBackup.from(HistoryStore(historyFile).loadSnapshot()), exported)
        assertEquals(before, historyFile.readText())
    }

    @Test fun importPreviewOffersBothModesAndCancelAndFilePickerCancellationNeverChangeData() {
        openTransfer()
        val before = historyFile.readText()
        preview()
        assertEquals(before, historyFile.readText())
        compose.onNodeWithTag("replace-collections").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("merge-collections").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Cancel import").performClick()
        compose.onNodeWithTag("collection-import-preview").assertDoesNotExist()
        compose.onNodeWithTag("import-collections").performScrollTo().performClick()
        selectDocument(cancelled = true)
        compose.onNodeWithTag("collection-import-preview").assertDoesNotExist()
        assertNull(model.collectionTransferMessage)
        compose.onNodeWithTag("export-collections").performScrollTo().performClick()
        selectDocument(cancelled = true)
        assertNull(model.collectionTransferMessage)
        assertEquals(before, historyFile.readText())
    }

    @Test fun replaceUpdatesLiveCollectionsAndSurvivesReloadWithoutChangingSettings() {
        openTransfer()
        val settings = application.getSharedPreferences("settings", 0).all
        preview()
        compose.onNodeWithTag("replace-collections").performScrollTo().performClick()
        compose.onNodeWithTag("collection-transfer-message").assertTextEquals("Collections replaced.")
        assertEquals(backup.rewards, model.rewardBalances)
        assertEquals(backup.pokemons, model.pokemons)
        assertEquals(backup, CollectionBackup.from(HistoryStore(historyFile).loadSnapshot()))
        assertEquals(settings, application.getSharedPreferences("settings", 0).all)
        compose.onNodeWithTag("collection-import-preview").assertDoesNotExist()
    }

    @Test fun mergeAddsFragmentsAndCombinesEveryAnimationCategoryWithoutDuplicates() {
        openTransfer()
        preview()
        compose.onNodeWithTag("merge-collections").performScrollTo().performClick()
        compose.onNodeWithTag("collection-transfer-message").assertTextEquals("Collections merged.")
        assertEquals(RewardBalance(3, 0), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(backup.rewards[RewardType.TOYS_3], model.rewardBalances[RewardType.TOYS_3])
        assertEquals(backup.pokemons + Celebration.SQUIRTLE, model.pokemons)
        val restored = HistoryStore(historyFile).loadSnapshot()
        assertEquals(model.rewardBalances, restored.rewards)
        assertEquals(model.pokemons, restored.pokemons)
    }

    @Test fun unreadableInvalidAndUnwritableFilesShowErrorsAndPreserveBothCollections() {
        openTransfer()
        val before = historyFile.readText()
        compose.onNodeWithTag("import-collections").performScrollTo().performClick()
        selectDocument()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Unable to read", substring = true)
        document.writeText("{}")
        compose.onNodeWithTag("import-collections").performScrollTo().performClick()
        selectDocument()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Unable to read", substring = true)
        compose.onNodeWithTag("collection-import-preview").assertDoesNotExist()
        assertEquals(before, historyFile.readText())
        preview()
        assertTrue(blockedWrite.mkdir())
        compose.onNodeWithTag("replace-collections").performScrollTo().performClick()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Unable to import", substring = true)
        compose.onNodeWithTag("replace-collections").assertIsEnabled()
        assertEquals(before, historyFile.readText())
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(setOf(Celebration.SQUIRTLE), model.pokemons)
        assertTrue(blockedWrite.delete())
        compose.onNodeWithTag("replace-collections").performClick()
        compose.onNodeWithTag("collection-transfer-message").assertTextEquals("Collections replaced.")
    }

    @Test fun missingFilePickerShowsAnActionableError() {
        openTransfer()
        missingPicker = true
        compose.onNodeWithTag("export-collections").performScrollTo().performClick()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Unable to open the file picker", substring = true)
        compose.onNodeWithTag("import-collections").performScrollTo().performClick()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Unable to open the file picker", substring = true)
        assertFalse(model.collectionTransferBusy)
    }

    @Test fun recreationRelocksAdminAndRetainsPreviewWithoutImportingAutomatically() {
        openTransfer()
        val before = historyFile.readText()
        preview()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
        compose.onNodeWithTag("replace-collections").assertDoesNotExist()
        assertEquals(before, historyFile.readText())
        compose.onNodeWithTag("admin-pin").performScrollTo().performTextInput("4826")
        compose.onNodeWithTag("admin-pin-submit").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("collection-import-preview").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("collection-import-preview").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancel import").performClick()
        assertEquals(before, historyFile.readText())
    }

    @Test fun exportFailureDoesNotClaimSuccessAndKeepsSourceCollections() {
        openTransfer()
        val before = historyFile.readText()
        assertTrue(document.mkdir())
        compose.onNodeWithTag("export-collections").performScrollTo().performClick()
        selectDocument()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("Unable to export", substring = true)
        assertFalse(model.collectionTransferBusy)
        assertEquals(before, historyFile.readText())
    }
}
