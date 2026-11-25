package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Duration
import javax.crypto.KeyGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

internal class TestPortal(application: Application) {
    private val prefs = application.getSharedPreferences("test_portal", 0)
    private val key = KeyGenerator.getInstance("AES").generateKey()
    val link = PortalLink(prefs) { key }
    var token: String? = null
    var granted = false
    var status = "pending"
    var remote = ServerCollection("v0", null)
    val uploads = mutableListOf<CollectionBackup>()
    var reads = 0
    var readError: Int? = null
    var readBody: String? = null
    var beforeUpload: (() -> Unit)? = null
    val api = PortalApi(OkHttpClient.Builder().addInterceptor { chain ->
        val req = chain.request()
        assertEquals("https", req.url.scheme)
        assertEquals("portal.example", req.url.host)
        assertNull(req.url.query)
        var code = 200
        val body = req.body?.let { Buffer().apply { it.writeTo(this) }.readUtf8() }
        val input = body?.let { Json.decodeFromString(JsonObject.serializer(), it) }
        val result = when (req.url.encodedPath) {
            "/ssacademy/api/devices/register", "/ssacademy/api/devices/status" -> {
                assertNull(req.header("Authorization"))
                assertEquals("Learner", input!!.getValue("username").jsonPrimitive.content)
                val secret = input.getValue("fingerprintToken").jsonPrimitive.content
                assertTrue(secret.matches(Regex("[a-zA-Z0-9_-]{43}")))
                if (token == null) token = secret else assertEquals(token, secret)
                if (req.url.encodedPath.endsWith("register")) {
                    assertEquals("true", input.getValue("requestSync").toString())
                    code = 202
                } else assertEquals(setOf("username", "fingerprintToken"), input.keys)
                buildJsonObject {
                    put("device", buildJsonObject {
                        put("status", status); put("fingerprint", secret.sha256().take(16)); put("syncApproved", granted)
                    })
                }.toString()
            }
            "/ssacademy/api/sync" -> {
                assertEquals("Bearer $token", req.header("Authorization"))
                assertEquals("GET", req.method)
                reads++
                code = readError ?: 200
                readBody ?: Json.encodeToString(ServerCollection.serializer(), remote)
            }
            "/ssacademy/api/imports" -> {
                assertEquals("Bearer $token", req.header("Authorization"))
                assertEquals("POST", req.method)
                assertEquals(setOf("source", "data", "baseVersion"), input!!.keys)
                beforeUpload?.invoke()
                if (input.getValue("baseVersion").jsonPrimitive.content != remote.version) {
                    code = 409
                    "{}"
                } else {
                    val collection = Json.decodeFromJsonElement(CollectionBackup.serializer(), input.getValue("data"))
                    uploads += collection
                    remote = ServerCollection("upload${uploads.size}", collection)
                    code = 201
                    buildJsonObject { put("sync", Json.encodeToJsonElement(ServerCollection.serializer(), remote)) }.toString()
                }
            }
            else -> error("Unexpected portal route")
        }
        Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body(result.toResponseBody("application/json".toMediaType())).build()
    }.build())

    init { prefs.edit().clear().commit() }
    fun approve() { status = "approved"; granted = true }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CollectionTransferModelTest {
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val file get() = File(application.filesDir, "practice_history.json")
    private val models = ViewModelStore()
    private lateinit var model: MathViewModel
    private lateinit var portal: TestPortal
    private val sync get() = model.collectionSync
    private val looper get() = shadowOf(Looper.getMainLooper())
    private val prize = CollectionBackup.from(PracticeSnapshot(rewards = mapOf(RewardType.TOYS_3 to RewardBalance(1)),
        pokemons = setOf(Celebration.PIKACHU)))

    private fun start() {
        model = MathViewModel(application, ioDispatcher = Dispatchers.Unconfined, portalLink = portal.link, portalApi = portal.api)
        models.put("model", model)
        model.openSettings()
        looper.idle()
    }
    private fun request() { sync.request("https://portal.example", "Learner"); looper.idle() }
    private fun tick() { looper.idleFor(Duration.ofSeconds(3)) }
    private fun connect() { portal.approve(); request(); tick() }

    @Before fun setup() {
        file.delete()
        portal = TestPortal(application)
        start()
    }
    @After fun cleanup() { models.clear(); file.delete() }

    @Test fun enrollmentIsOriginBoundRetriesTheSameSecretAndNeedsAnExplicitSyncGrant() {
        sync.request("http://portal.example", "Learner"); looper.idle()
        assertNull(sync.account)
        request()
        val secret = requireNotNull(portal.token)
        assertEquals(secret.sha256().take(16), sync.fingerprint)
        assertFalse(application.getSharedPreferences("test_portal", 0).all.values.any { "$it".contains(secret) })
        start()
        request()
        portal.status = "approved"
        sync.check(); looper.idle(); tick()
        assertFalse(sync.approved)
        assertEquals(0, portal.reads)
        portal.approve()
        sync.check(); looper.idle(); tick()
        assertTrue(sync.approved)
        assertEquals(1, portal.uploads.size)
        sync.disconnect(); looper.idle()
        assertNull(sync.account)
        assertNull(portal.link.baseVersion)
    }

    @Test fun savedRewardChangesCoalesceAndReloadsAndFailedSavesDoNotUpload() {
        connect()
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        model.addReward(RewardType.LOLLIPOP, false); looper.idle()
        tick()
        val saved = CollectionBackup.from(HistoryStore(file).loadSnapshot())
        assertEquals(RewardBalance(1, 1), saved.rewards[RewardType.LOLLIPOP])
        assertEquals(listOf(CollectionBackup.from(PracticeSnapshot()), saved), portal.uploads)
        model.openRewards(); looper.idle()
        model.openSettings()
        val blocked = File(file.parentFile, "${file.name}.tmp")
        assertTrue(blocked.mkdir())
        try {
            model.addReward(RewardType.TOYS_1, false); looper.idle(); tick()
            assertNotNull(model.rewardRemovalError)
            assertEquals(2, portal.reads)
        } finally { blocked.delete() }
    }

    @Test fun remoteChangesDownloadWithoutEchoAndConcurrentEditsNeedExplicitResolution() {
        connect()
        portal.remote = ServerCollection("admin-edit", prize)
        sync.sync(); looper.idle(); tick()
        assertEquals(prize, CollectionBackup.from(HistoryStore(file).loadSnapshot()))
        assertEquals(1, portal.uploads.size)
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        val edited = prize.copy(pokemons = setOf(Celebration.PARTY))
        portal.remote = ServerCollection("second-edit", edited)
        tick()
        assertEquals(edited, sync.preview?.server?.data)
        assertEquals(setOf(Celebration.PIKACHU), model.pokemons)
        assertEquals(1, portal.uploads.size)
        sync.confirmDownload(CollectionImportMode.MERGE)
        sync.confirmDownload(CollectionImportMode.MERGE)
        looper.idle(); tick()
        assertEquals(RewardBalance(2), model.rewardBalances[RewardType.TOYS_3])
        assertEquals(setOf(Celebration.PIKACHU, Celebration.PARTY), model.pokemons)
        assertEquals(CollectionBackup.from(HistoryStore(file).loadSnapshot()), portal.uploads.last())
    }

    @Test fun stalePreviewAndRacingServerWriteCannotOverwriteEitherSide() {
        portal.remote = ServerCollection("existing", prize)
        model.addReward(RewardType.LOLLIPOP, false); looper.idle()
        connect()
        assertNotNull(sync.preview)
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertNull(sync.preview)
        assertEquals(RewardBalance(1, 1), model.rewardBalances[RewardType.LOLLIPOP])
        sync.upload(); looper.idle()
        portal.beforeUpload = { portal.remote = ServerCollection("newer", prize.copy(pokemons = emptySet())) }
        sync.confirmUpload(); looper.idle()
        assertTrue(sync.message.orEmpty().contains("conflict"))
        assertTrue(portal.uploads.isEmpty())
        assertNull(portal.link.baseVersion)
    }

    @Test fun mutationDuringUploadSendsLatestDurableCopyInTheFollowingSync() {
        connect()
        portal.beforeUpload = {
            portal.beforeUpload = null
            model.addReward(RewardType.TOYS_1, true)
        }
        model.addReward(RewardType.LOLLIPOP, true); looper.idle(); tick(); tick()
        assertEquals(3, portal.uploads.size)
        assertNull(portal.uploads[1].rewards[RewardType.TOYS_1])
        assertEquals(RewardBalance(0, 1), portal.uploads[2].rewards[RewardType.TOYS_1])
        assertEquals(CollectionBackup.from(HistoryStore(file).loadSnapshot()), portal.remote.data)
    }

    @Test fun invalidDownloadsAndFailedLocalWritesCannotAcknowledgeOrChangeTheCollection() {
        connect()
        model.addReward(RewardType.LOLLIPOP, false); looper.idle(); tick()
        val before = file.readText()
        val base = portal.link.baseVersion
        for (bad in listOf("""{"version":"new"}""",
            """{"version":"new","data":{}}""", "x".repeat(CollectionBackup.MAX_BYTES + 4097))) {
            portal.readBody = bad
            sync.download(); looper.idle()
            assertNull(sync.preview)
            assertNotNull(sync.message)
            assertEquals(before, file.readText())
            assertEquals(base, portal.link.baseVersion)
        }
        portal.readBody = null
        portal.remote = ServerCollection("remote", prize)
        sync.download(); looper.idle()
        val blocked = File(file.parentFile, "${file.name}.tmp")
        assertTrue(blocked.mkdir())
        try {
            sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
            assertNotNull(sync.preview)
            assertEquals(before, file.readText())
            assertEquals(base, portal.link.baseVersion)
        } finally { blocked.delete() }
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertEquals(prize, CollectionBackup.from(HistoryStore(file).loadSnapshot()))
    }

    @Test fun offlineRewardSurvivesRestartAndRevocationStopsAutomaticRequests() {
        connect()
        portal.readError = 503
        model.addReward(RewardType.LOLLIPOP, false); looper.idle(); tick()
        assertEquals(1, portal.uploads.size)
        portal.readError = null
        start(); tick()
        assertEquals(RewardBalance(1), portal.uploads.last().rewards[RewardType.LOLLIPOP])
        portal.readError = 403
        model.addReward(RewardType.TOYS_1, false); looper.idle(); tick()
        assertFalse(sync.approved)
        val reads = portal.reads
        start()
        model.addReward(RewardType.TOYS_2, false); looper.idle(); tick()
        assertEquals(reads, portal.reads)
    }
}
