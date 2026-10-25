package com.ssmath.app

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CollectionTransferModelTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val historyFile get() = File(application.filesDir, "practice_history.json")
    private val models = ViewModelStore()
    private val dispatcher = QueuedDispatcher()
    private lateinit var model: MathViewModel
    private val uri = Uri.parse("content://test/collections")
    private val backup = CollectionBackup(CollectionBackup.FORMAT, CollectionBackup.VERSION,
        mapOf(RewardType.LOLLIPOP to RewardBalance(1, 1)), setOf(Celebration.PIKACHU))

    private class QueuedDispatcher : CoroutineDispatcher() {
        val pending = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { pending.addLast(block) }
        fun drain() {
            shadowOf(Looper.getMainLooper()).idle()
            while (pending.isNotEmpty()) {
                pending.removeFirst().run()
                shadowOf(Looper.getMainLooper()).idle()
            }
        }
    }

    @Before fun setup() {
        application.getSharedPreferences("settings", 0).edit().clear().commit()
        historyFile.delete()
        DebugLog.initialize(application)
        model = MathViewModel(application, ioDispatcher = dispatcher)
        models.put("model", model)
        dispatcher.drain()
        model.openSettings()
        shadowOf(application.contentResolver).registerInputStream(uri, CollectionBackup.encode(backup).byteInputStream())
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.drain()
        historyFile.delete()
    }

    @Test fun mergeWaitsForConcurrentRewardAdjustmentAndRepeatedConfirmationCannotDoubleImport() {
        model.previewCollectionImport(uri)
        dispatcher.drain()
        assertEquals(backup, model.collectionImportPreview)
        model.addReward(RewardType.LOLLIPOP, fragment = true)
        model.importCollections(CollectionImportMode.MERGE)
        model.importCollections(CollectionImportMode.MERGE)
        assertTrue(model.collectionTransferBusy)
        model.cancelCollectionImport()
        assertEquals(backup, model.collectionImportPreview)
        dispatcher.drain()
        assertEquals(RewardBalance(1, 2), model.rewardBalances[RewardType.LOLLIPOP])
        assertEquals(backup.pokemons, model.pokemons)
        assertEquals("Collections merged.", model.collectionTransferMessage)
        assertFalse(model.collectionTransferBusy)
        assertNull(model.collectionImportPreview)
        assertEquals(model.rewardBalances, HistoryStore(historyFile).loadSnapshot().rewards)
    }

    @Test fun exportUsesOneSavedSnapshotAndReportsSuccessOnlyAfterTheContentStreamCloses() {
        val output = object : ByteArrayOutputStream() {
            var closed = false
            override fun close() { closed = true; super.close() }
        }
        shadowOf(application.contentResolver).registerOutputStream(uri, output)
        model.addReward(RewardType.LOLLIPOP, fragment = false)
        model.exportCollections(uri)
        assertTrue(model.collectionTransferBusy)
        assertNull(model.collectionTransferMessage)
        dispatcher.drain()
        assertTrue(output.closed)
        val exported = output.toByteArray().inputStream().use(CollectionBackup::read)
        assertEquals(CollectionBackup.from(HistoryStore(historyFile).loadSnapshot()), exported)
        assertEquals(RewardBalance(1), exported.rewards[RewardType.LOLLIPOP])
        assertFalse(model.collectionTransferBusy)
        assertTrue(model.collectionTransferMessage.orEmpty().startsWith("Collections exported."))
    }

    @Test fun failingContentProviderCloseReportsExportFailureInsteadOfSuccess() {
        val output = object : ByteArrayOutputStream() {
            override fun close() { throw IOException("Provider could not save.") }
        }
        shadowOf(application.contentResolver).registerOutputStream(uri, output)
        model.exportCollections(uri)
        dispatcher.drain()
        assertFalse(model.collectionTransferBusy)
        assertTrue(model.collectionTransferMessage.orEmpty().startsWith("Unable to export"))
    }

    @Test fun anOverflowingMergeRetainsPreviewForAnExplicitReplacementAndDoesNotMutateInventory() {
        val maximum = backup.copy(rewards = mapOf(RewardType.LOLLIPOP to RewardBalance((Int.MAX_VALUE - 2) / 3, 2)))
        HistoryStore(historyFile).importCollections(maximum, CollectionImportMode.REPLACE)
        model.previewCollectionImport(uri)
        dispatcher.drain()
        val before = historyFile.readText()
        model.importCollections(CollectionImportMode.MERGE)
        dispatcher.drain()
        assertEquals(before, historyFile.readText())
        assertEquals(backup, model.collectionImportPreview)
        assertTrue(model.collectionTransferMessage.orEmpty().startsWith("Unable to import"))
        model.importCollections(CollectionImportMode.REPLACE)
        dispatcher.drain()
        assertEquals(backup, CollectionBackup.from(HistoryStore(historyFile).loadSnapshot()))
        assertEquals("Collections replaced.", model.collectionTransferMessage)
    }
}
