package com.ssmath.app

import android.app.Application
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class ChangeAdminPinDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val preferences get() = application.getSharedPreferences("settings", 0)

    @Before fun setup() {
        preferences.edit().clear().commit()
        AdminPinStore(preferences).create("4826")
    }

    @Test fun failedSaveShowsAnErrorClearsFieldsAndKeepsTheOldPinWithoutCallingSuccess() {
        val previous = preferences.all.toMap()
        val changed = AtomicInteger()
        val dismissed = AtomicInteger()
        showDialog(interceptCommits { editor ->
            editor.commit()
            false
        }, changed, dismissed)
        submit()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Could not save the PIN. Please try again.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Could not save the PIN. Please try again.").performScrollTo().assertIsDisplayed()
        listOf("current", "new", "confirm").forEach {
            compose.onNodeWithTag("change-pin-$it").assertTextEquals("")
        }
        compose.onNodeWithTag("change-pin-submit").assertIsNotEnabled()
        assertEquals(0, changed.get())
        assertEquals(0, dismissed.get())
        assertEquals(previous, preferences.all)
        assertEquals(AdminPinStore.Result.INCORRECT, AdminPinStore(preferences).verify("0000"))
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("4826"))
    }

    @Test fun savingOffTheMainThreadBlocksDuplicateSubmissionsCancelAndBackUntilCompletion() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val changed = AtomicInteger()
        val dismissed = AtomicInteger()
        val commits = AtomicInteger()
        val onMainThread = AtomicBoolean(false)
        showDialog(interceptCommits { editor ->
            onMainThread.set(Looper.myLooper() == Looper.getMainLooper())
            commits.incrementAndGet()
            started.countDown()
            check(release.await(20, TimeUnit.SECONDS))
            editor.commit()
        }, changed, dismissed)
        try {
            submit()
            compose.waitUntil(10_000) { started.count == 0L }
            compose.onNodeWithTag("change-pin-submit").assertIsNotEnabled().performClick()
            compose.onNodeWithText("Cancel").assertIsNotEnabled().performClick()
            listOf("current", "new", "confirm").forEach {
                compose.onNodeWithTag("change-pin-$it").assertIsNotEnabled().assertTextEquals("")
            }
            compose.runOnIdle {
                (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
            }
            compose.onNodeWithTag("change-pin-dialog").assertIsDisplayed()
            assertEquals(0, changed.get())
            assertEquals(0, dismissed.get())
            assertFalse(onMainThread.get())
        } finally {
            release.countDown()
        }
        compose.waitUntil(10_000) { changed.get() == 1 }
        assertEquals(1, commits.get())
        assertEquals(0, dismissed.get())
        assertEquals(AdminPinStore.Result.INCORRECT, AdminPinStore(preferences).verify("4826"))
        assertEquals(AdminPinStore.Result.UNLOCKED, AdminPinStore(preferences).verify("0000"))
    }

    private fun showDialog(savedPreferences: SharedPreferences, changed: AtomicInteger, dismissed: AtomicInteger) {
        val context = object : ContextWrapper(compose.activity) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                if (name == "settings") savedPreferences else super.getSharedPreferences(name, mode)
        }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                MathTheme {
                    ChangeAdminPinDialog(onChanged = { changed.incrementAndGet() }, onDismiss = { dismissed.incrementAndGet() })
                }
            }
        }
    }

    private fun submit() {
        compose.onNodeWithTag("change-pin-current").performScrollTo().performTextInput("4826")
        compose.onNodeWithTag("change-pin-new").performScrollTo().performTextInput("0000")
        compose.onNodeWithTag("change-pin-confirm").performScrollTo().performTextInput("0000")
        compose.onNodeWithTag("change-pin-submit").performClick()
    }

    private fun interceptCommits(commit: (SharedPreferences.Editor) -> Boolean): SharedPreferences =
        object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putString(key: String?, value: String?) = apply { editor.putString(key, value) }
                    override fun putInt(key: String?, value: Int) = apply { editor.putInt(key, value) }
                    override fun putLong(key: String?, value: Long) = apply { editor.putLong(key, value) }
                    override fun commit(): Boolean = commit(editor)
                }
            }
        }
}
