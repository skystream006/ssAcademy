package com.ssmath.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubjectNavigationTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        File(application.filesDir, "practice_history.json").delete()
    }

    private fun model() = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined)

    @Test fun readingReturnsHomeWithoutChangingMathSetupOrRewards() {
        val model = model()
        model.selectOperation(Operation.MULTIPLICATION)
        model.updateMinimum("3")
        model.updateMaximum("12")
        model.updateQuestionCount("20")
        val defaults = model.rewardsDefaults
        val rewards = model.rewardBalances
        model.openReading()
        assertEquals(Overlay.READING, model.overlay)
        assertEquals(Screen.SETUP, model.screen)
        assertNull(model.game)
        model.closeOverlay()
        assertNull(model.overlay)
        assertEquals(Operation.MULTIPLICATION, model.selectedOperation)
        assertEquals("3", model.minimumText)
        assertEquals("12", model.maximumText)
        assertEquals("20", model.questionCountText)
        assertEquals(defaults, model.rewardsDefaults)
        assertEquals(rewards, model.rewardBalances)
        assertTrue(model.history.isEmpty())
    }

    @Test fun readingCannotReplaceSettingsOrPendingMathSetup() {
        val model = model()
        model.openSettings()
        model.openReading()
        assertEquals(Overlay.SETTINGS, model.overlay)
        model.closeOverlay()
        model.openPracticeGame()
        model.openReading()
        assertNull(model.overlay)
        assertEquals(SetupDialog.PRACTICE, model.setupDialog)
        model.dismissSetupDialog()
        model.openReading()
        assertEquals(Overlay.READING, model.overlay)
        model.openPracticeGame()
        model.openRewardsGame()
        assertNull(model.setupDialog)
    }
}
