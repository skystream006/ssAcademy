package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.IOException
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
    private val models = ViewModelStore()
    private lateinit var portal: TestPortal
    private lateinit var restoration: StateRestorationTester
    private lateinit var reading: ReadingViewModel
    private lateinit var savedReading: ReadingData
    private var failNameSave = false

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        historyFile.delete()
        portal = TestPortal(application)
        savedReading = ReadingEngine.addProfile(ReadingData(), portal.student)
        reading = ViewModelProvider(compose.activity, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReadingViewModel(application, object : ReadingStorage {
                    override fun load() = savedReading
                    override fun save(data: ReadingData) {
                        if (failNameSave) throw IOException("Name save failed.")
                        savedReading = data
                    }
                }, Dispatchers.Unconfined) as T
        })[ReadingViewModel::class.java]
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined,
            portalLink = portal.link, portalApi = portal.api)
        models.put("model", model)
        restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SettingsScreen(model) } }
    }
    @After fun cleanup() {
        models.clear()
        compose.activity.viewModelStore.clear()
        historyFile.delete()
    }

    @Test fun requestAndDownloadControlsAreAdminGatedAndRequireServerApproval() {
        compose.onNodeWithTag("portal-address").assertDoesNotExist()
        compose.openAdminSettings()
        compose.onNodeWithTag("collection-transfer").performScrollTo().performClick()
        compose.onNodeWithTag("portal-address").performTextInput("https://portal.example")
        compose.onNodeWithTag("portal-username").assertDoesNotExist()
        compose.onNodeWithTag("portal-student").assertTextEquals("Student: Learner")
        compose.onNodeWithText("Request sync access").performScrollTo().performClick()
        compose.onNodeWithText("Upload collections").assertDoesNotExist()
        compose.onNodeWithText("Download collections").assertDoesNotExist()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("pending", substring = true)
        portal.approve()
        compose.onNodeWithText("Check approval").performScrollTo().performClick()
        compose.onNodeWithText("Sync now").assertExists()
        compose.onNodeWithText("Upload collections").assertExists()
        compose.onNodeWithText("Download collections").assertIsNotEnabled()
        val bearer = portal.bearerToken
        portal.approve()
        compose.onNodeWithText("Check approval").performScrollTo().performClick()
        compose.onNodeWithText("Sync now").assertExists()
        assertNotEquals(bearer, portal.link.bearerToken(requireNotNull(portal.link.account)))
        val id = portal.insert(CollectionBackup.from(PracticeSnapshot(pokemons = setOf(Celebration.PIKACHU))))
        compose.onNodeWithTag("portal-import-id").performScrollTo().performTextInput(id)
        compose.onNodeWithText("Download collections").performScrollTo().performClick()
        compose.onNodeWithTag("collection-import-preview").performScrollTo().assertTextContains(id, substring = true)
        assertNull(portal.link.importId)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Download collections").assertDoesNotExist()
    }

    @Test fun syncUsesTheSavedGameNameAndChangedNamesRequireReconnection() {
        compose.onNodeWithTag("change-name").performScrollTo().performClick()
        compose.onNodeWithTag("settings-name").performTextReplacement("  A  ")
        compose.onNodeWithTag("settings-save-name").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("change-name-dialog").fetchSemanticsNodes().isEmpty() }
        assertEquals("A", savedReading.profile?.name)
        portal.student = "A"
        compose.openAdminSettings()
        compose.onNodeWithTag("collection-transfer").performScrollTo().performClick()
        compose.onNodeWithTag("portal-address").performTextInput("https://portal.example")
        compose.onNodeWithTag("portal-student").assertTextEquals("Student: A")
        compose.onNodeWithTag("portal-username").assertDoesNotExist()
        compose.onNodeWithText("Request sync access").performScrollTo().performClick()
        val firstToken = requireNotNull(portal.token)
        compose.runOnIdle { reading.updateName("New Name") }
        compose.waitUntil(5_000) { reading.data?.profile?.name == "New Name" && !reading.saving }
        compose.onNodeWithTag("portal-student").assertTextEquals("Student: New Name")
        compose.onNodeWithText("Disconnect and request access again", substring = true).assertExists()
        compose.onNodeWithText("Check approval").assertDoesNotExist()
        compose.onNodeWithText("Retry access request").assertDoesNotExist()
        compose.onNodeWithText("Disconnect device").performScrollTo().performClick()
        portal.student = "New Name"
        portal.token = null
        compose.onNodeWithText("Request sync access").performScrollTo().performClick()
        assertEquals("New Name", portal.link.account?.student)
        assertNotEquals(firstToken, portal.token)
        compose.onNodeWithText("Upload collections").assertDoesNotExist()
    }

    @Test fun missingOrUnsavedGameNamesCannotEnrollAndStorageErrorsCanBeRetried() {
        compose.runOnIdle { reading.act { ReadingData() } }
        compose.openAdminSettings()
        compose.onNodeWithTag("collection-transfer").performScrollTo().performClick()
        compose.onNodeWithTag("portal-address").performTextInput("https://portal.example")
        compose.onNodeWithText("Request sync access").assertIsNotEnabled()
        compose.onNodeWithText("Enter your name at the welcome screen", substring = true).assertExists()
        failNameSave = true
        compose.runOnIdle { reading.setName("A") }
        compose.onNodeWithText("Request sync access").assertIsNotEnabled()
        compose.onNodeWithText("Your latest change has not been saved", substring = true).assertExists()
        assertNull(portal.token)
        failNameSave = false
        compose.onNodeWithText("Retry storage").performScrollTo().performClick()
        compose.onNodeWithTag("portal-student").assertTextEquals("Student: A")
        portal.student = "A"
        compose.onNodeWithText("Request sync access").performScrollTo().performClick()
        assertEquals("A", portal.link.account?.student)
    }
}
