package com.ssmath.app

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    private val models = ViewModelStore()
    private lateinit var portal: TestPortal
    private lateinit var restoration: StateRestorationTester

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        historyFile.delete()
        portal = TestPortal(application)
        val model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined,
            portalLink = portal.link, portalApi = portal.api)
        models.put("model", model)
        restoration = StateRestorationTester(compose)
        restoration.setContent { MathTheme { SettingsScreen(model) } }
    }
    @After fun cleanup() { models.clear(); historyFile.delete() }

    @Test fun requestAndDownloadControlsAreAdminGatedAndRequireServerApproval() {
        compose.onNodeWithTag("portal-address").assertDoesNotExist()
        compose.openAdminSettings()
        compose.onNodeWithTag("collection-transfer").performScrollTo().performClick()
        compose.onNodeWithTag("portal-address").performTextInput("https://portal.example")
        compose.onNodeWithTag("portal-username").performScrollTo().performTextInput("Learner")
        compose.onNodeWithText("Request sync access").performScrollTo().performClick()
        compose.onNodeWithText("Upload collections").assertDoesNotExist()
        compose.onNodeWithText("Download collections").assertDoesNotExist()
        compose.onNodeWithTag("collection-transfer-message").assertTextContains("pending", substring = true)
        portal.approve()
        compose.onNodeWithText("Check approval").performScrollTo().performClick()
        compose.onNodeWithText("Sync now").assertExists()
        compose.onNodeWithText("Upload collections").assertExists()
        compose.onNodeWithText("Download collections").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("admin-pin-dialog").assertIsDisplayed()
        compose.onNodeWithText("Download collections").assertDoesNotExist()
    }
}
