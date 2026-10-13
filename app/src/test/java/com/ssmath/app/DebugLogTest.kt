package com.ssmath.app

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DebugLogTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var store: DebugLogStore

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("debug_logging", Context.MODE_PRIVATE).edit().clear().commit()
        directory = File(context.noBackupFilesDir, "debug_logs")
        directory.deleteRecursively()
        store = DebugLogStore(context)
    }

    @Test fun loggingIsOffByDefaultAndCreatesNoFiles() {
        store.event(DebugEvent.APP_STARTED)
        store.event(DebugEvent.APP_CRASH, error = IOException("secret"))
        assertFalse(store.enabled)
        assertEquals("", store.read())
        assertFalse(directory.exists())
    }

    @Test fun preferencePersistsAndDisablingStopsWritesWithoutRemovingSavedLogs() {
        assertTrue(store.setEnabled(true))
        assertTrue(DebugLogStore(context).enabled)
        store.event(DebugEvent.GAME_STARTED)
        val saved = store.read()
        assertTrue(saved.contains("GAME_STARTED"))
        assertTrue(store.setEnabled(false))
        store.event(DebugEvent.GAME_FINISHED)
        assertEquals(saved, store.read())
        assertFalse(DebugLogStore(context).enabled)
    }

    @Test fun diagnosticOutputOnlyContainsEventNamesAndExceptionClasses() {
        store.setEnabled(true)
        store.event(DebugEvent.HISTORY_FAILURE, error = IOException("7 + 5 = 13 secret answer"))
        val text = store.read()
        assertTrue(text.contains("HISTORY_FAILURE exception=java.io.IOException"))
        assertFalse(text.contains("secret"))
    }

    @Test fun fullModeFilesAreBoundedAndNewestEventsSurviveRotation() {
        store.setEnabled(true)
        repeat(4000) { store.event(DebugEvent.GAME_STARTED, status = it) }
        store.event(DebugEvent.GAME_FINISHED, status = 99999)
        val files = directory.listFiles()!!
        assertEquals(2, files.size)
        assertTrue(files.all { it.length() <= DebugLogStore.MAX_FILE_BYTES })
        assertTrue(store.read().endsWith("GAME_FINISHED status=99999\n"))
    }

    @Test fun reactiveModeKeepsOnlyTheLatestEvents() {
        store.setEnabled(true, DebugLogMode.REACTIVE)
        repeat(250) { store.event(DebugEvent.GAME_STARTED, status = it) }
        val lines = store.read().lineSequence().filter { it.isNotEmpty() }.toList()
        assertEquals(DebugLogStore.MAX_REACTIVE_ENTRIES, lines.size)
        assertTrue(lines.last().endsWith("GAME_STARTED status=249"))
    }

    @Test fun sharedLogUsesAppNameAndPreservesText() {
        val intent = DebugLog.shareIntent("diagnostic log")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("ssAcademy debug log", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("diagnostic log", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test fun clearingRemovesAllLogFilesButKeepsOptIn() {
        store.setEnabled(true)
        repeat(100) { store.event(DebugEvent.GAME_STARTED) }
        assertTrue(store.clear())
        assertEquals("", store.read())
        assertTrue(DebugLogStore(context).enabled)
    }
}
