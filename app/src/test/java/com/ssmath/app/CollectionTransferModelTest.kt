package com.ssmath.app

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Duration
import java.util.UUID
import javax.crypto.KeyGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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
    var student = "Learner"
    var status = "pending"
    val records = mutableMapOf<String, ServerCollection>()
    private val originals = mutableMapOf<Pair<String, CollectionBackup>, String>()
    fun insert(data: CollectionBackup, source: String = "Other tablet"): String = originals.getOrPut(source to data) {
        UUID.randomUUID().toString().also { records[it] = ServerCollection(it, 1, data) }
    }
    fun edit(id: String, data: CollectionBackup) {
        records[id] = records.getValue(id).let { it.copy(revision = it.revision + 1, data = data) }
    }
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
        assertNull(req.header("Cookie"))
        assertNull(req.header("Origin"))
        var code = 200
        val body = req.body?.let { Buffer().apply { it.writeTo(this) }.readUtf8() }
        val input = body?.let { Json.decodeFromString(JsonObject.serializer(), it) }
        val path = req.url.encodedPath
        val result = when {
            path in setOf("/ssacademy/api/devices/register", "/ssacademy/api/devices/status") -> {
                assertNull(req.header("Authorization"))
                assertEquals("POST", req.method)
                assertEquals(student, input!!.getValue("student").jsonPrimitive.content)
                val secret = input.getValue("fingerprintToken").jsonPrimitive.content
                assertTrue(secret.matches(Regex("[a-zA-Z0-9_-]{43}")))
                if (token == null) token = secret else assertEquals(token, secret)
                assertEquals(setOf("student", "fingerprintToken"), input.keys)
                if (path.endsWith("register")) code = 202
                buildJsonObject {
                    put("device", buildJsonObject {
                        put("status", status); put("fingerprint", secret.sha256().take(16)); put("expiresAt", 1790000000000L)
                    })
                }.toString()
            }
            path.startsWith("/ssacademy/api/imports/") -> {
                assertEquals("Bearer $token", req.header("Authorization"))
                assertEquals("GET", req.method)
                val record = records[path.substringAfterLast('/')]
                reads++
                code = readError ?: if (record == null) 404 else 200
                readBody ?: if (record == null) "{}" else buildJsonObject {
                    put("import", buildJsonObject {
                        put("id", record.id); put("revision", record.revision)
                        put("format", record.data.format); put("version", record.data.version)
                        put("ownerId", "owner"); put("edits", buildJsonArray {})
                        put("rewards", buildJsonArray {
                            record.data.rewards.forEach { (type, balance) -> add(buildJsonObject {
                                put("id", type.name); put("label", type.name)
                                put("whole", balance.whole); put("fragments", balance.fragments)
                            }) }
                        })
                        put("animations", buildJsonArray { record.data.pokemons.forEach { add(JsonPrimitive(it.name)) } })
                    })
                }.toString()
            }
            path == "/ssacademy/api/imports" -> {
                assertEquals("Bearer $token", req.header("Authorization"))
                assertEquals("POST", req.method)
                assertEquals(setOf("source", "data"), input!!.keys)
                beforeUpload?.invoke()
                val collection = Json.decodeFromJsonElement(CollectionBackup.serializer(), input.getValue("data"))
                val source = input.getValue("source").jsonPrimitive.content
                val duplicate = (source to collection) in originals
                val id = insert(collection, source)
                uploads += collection
                code = if (duplicate) 200 else 201
                buildJsonObject {
                    put("duplicate", duplicate)
                    put("import", buildJsonObject {
                        put("id", id); put("source", source); put("format", collection.format)
                        put("version", collection.version); put("importedAt", 1790000000000L)
                    })
                }.toString()
            }
            else -> error("Unexpected portal route")
        }
        Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body(result.toResponseBody("application/json".toMediaType())).build()
    }.build())

    init { prefs.edit().clear().commit() }
    fun approve() { status = "approved" }
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
    private fun request() { sync.request("https://portal.example", portal.student); looper.idle() }
    private fun tick() { looper.idleFor(Duration.ofSeconds(3)) }
    private fun connect() {
        portal.approve(); request(); tick()
        if (sync.importId == null) { sync.upload(); looper.idle() }
    }

    @Before fun setup() {
        file.delete()
        portal = TestPortal(application)
        start()
    }
    @After fun cleanup() { models.clear(); file.delete() }

    @Test fun studentNamesUseGameNameLimitsBeforeEnrollment() {
        for (invalid in listOf("", "   ", "x".repeat(41), "A\nB")) {
            sync.request("https://portal.example", invalid); looper.idle()
            assertNull(sync.account)
            assertNull(portal.token)
            assertNotNull(sync.message)
        }
        portal.student = "A"
        sync.request("https://portal.example", "  A  "); looper.idle()
        assertEquals("A", sync.account?.student)
        sync.check(); looper.idle()
        assertNotNull(portal.token)
    }

    @Test fun legacyUsernameLinksKeepTheirEncryptedCredentialAndOwnedImport() {
        connect()
        val secret = requireNotNull(portal.token)
        val id = requireNotNull(sync.importId)
        val prefs = application.getSharedPreferences("test_portal", 0)
        assertTrue(prefs.edit().putString("username", "Learner").remove("student").commit())
        start()
        assertEquals("Learner", sync.account?.student)
        assertEquals(secret, portal.link.token(requireNotNull(sync.account)))
        assertEquals(id, sync.importId)
        sync.check(); looper.idle()
        assertTrue(sync.approved)
        sync.download(); looper.idle()
        assertEquals(id, sync.preview?.server?.id)
    }

    @Test fun enrollmentRetriesTheSameSecretAndNormalApprovalAllowsUploadAndOwnedDownload() {
        sync.request("http://portal.example", "Learner"); looper.idle()
        assertNull(sync.account)
        request()
        val secret = requireNotNull(portal.token)
        assertEquals(secret.sha256().take(16), sync.fingerprint)
        assertFalse(application.getSharedPreferences("test_portal", 0).all.values.any { "$it".contains(secret) })
        start()
        request()
        sync.check(); looper.idle(); tick()
        assertFalse(sync.approved)
        assertEquals(0, portal.reads)
        portal.approve()
        sync.check(); looper.idle(); tick()
        assertTrue(sync.approved)
        assertTrue(portal.uploads.isEmpty())
        sync.upload(); looper.idle()
        assertEquals(1, portal.uploads.size)
        val id = requireNotNull(sync.importId)
        assertEquals(portal.records.getValue(id).data, portal.uploads.single())
        start()
        assertEquals(id, sync.importId)
        sync.download(); looper.idle()
        assertEquals(id, sync.preview?.server?.id)
        sync.cancelPreview()
        sync.disconnect(); looper.idle()
        assertNull(sync.account)
        assertNull(portal.link.importId)
    }

    @Test fun savedRewardChangesCoalesceAndReloadsAndFailedSavesDoNotUpload() {
        connect()
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        model.addReward(RewardType.LOLLIPOP, false); looper.idle()
        tick()
        val saved = CollectionBackup.from(HistoryStore(file).loadSnapshot())
        assertEquals(RewardBalance(1, 1), saved.rewards[RewardType.LOLLIPOP])
        assertEquals(listOf(CollectionBackup.from(PracticeSnapshot()), saved), portal.uploads)
        val reads = portal.reads
        model.openRewards(); looper.idle()
        model.openSettings()
        val blocked = File(file.parentFile, "${file.name}.tmp")
        assertTrue(blocked.mkdir())
        try {
            model.addReward(RewardType.TOYS_1, false); looper.idle(); tick()
            assertNotNull(model.rewardRemovalError)
            assertEquals(reads, portal.reads)
        } finally { blocked.delete() }
    }

    @Test fun remoteChangesDownloadWithoutEchoAndConcurrentEditsNeedExplicitResolution() {
        connect()
        val id = requireNotNull(sync.importId)
        portal.edit(id, prize)
        sync.sync(); looper.idle(); tick()
        assertEquals(prize, CollectionBackup.from(HistoryStore(file).loadSnapshot()))
        assertEquals(1, portal.uploads.size)
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        val edited = prize.copy(pokemons = setOf(Celebration.PARTY))
        portal.edit(id, edited)
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
        assertNotEquals(id, sync.importId)
        assertEquals(edited, portal.records.getValue(id).data)
    }

    @Test fun downloadingByIdOnlyLinksAfterConfirmationAndRechecksLocalDataAndRemoteRevision() {
        connect()
        val before = requireNotNull(sync.importId)
        val other = portal.insert(prize)
        sync.download("../imports"); looper.idle()
        assertNull(sync.preview)
        sync.download(UUID.randomUUID().toString()); looper.idle()
        assertTrue(sync.approved)
        assertEquals(before, sync.importId)
        assertTrue(sync.message.orEmpty().contains("not found"))
        sync.download(other); looper.idle()
        assertEquals(other, sync.preview?.server?.id)
        sync.cancelPreview()
        assertEquals(before, sync.importId)
        sync.download(other); looper.idle()
        assertNotNull(sync.preview)
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertNull(sync.preview)
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
        sync.download(other); looper.idle()
        portal.edit(other, prize.copy(pokemons = emptySet()))
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertNull(sync.preview)
        assertEquals(before, sync.importId)
        sync.download(other); looper.idle()
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertEquals(other, sync.importId)
        assertEquals(portal.records.getValue(other).data, CollectionBackup.from(HistoryStore(file).loadSnapshot()))
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
        assertEquals(CollectionBackup.from(HistoryStore(file).loadSnapshot()), portal.records.getValue(sync.importId!!).data)
    }

    @Test fun duplicateReceiptSurvivesFailedReadAndCorrectedServerValuesNeverReplaceLocalWithoutReview() {
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        connect()
        val original = requireNotNull(sync.importId)
        portal.edit(original, prize)
        model.addReward(RewardType.LOLLIPOP, false); looper.idle()
        sync.upload(); looper.idle()
        assertNotEquals(original, sync.importId)
        model.removeReward(RewardType.LOLLIPOP, false); looper.idle()
        portal.readError = 503
        sync.upload(); looper.idle()
        assertEquals(original, sync.importId)
        assertFalse(portal.link.verified)
        portal.readError = null
        start(); tick()
        assertEquals(prize, sync.preview?.server?.data)
        assertEquals(RewardBalance(0, 1), model.rewardBalances[RewardType.LOLLIPOP])
    }

    @Test fun invalidDownloadsAndFailedLocalWritesCannotAcknowledgeOrChangeTheCollection() {
        connect()
        model.addReward(RewardType.LOLLIPOP, false); looper.idle(); tick()
        val before = file.readText()
        val base = requireNotNull(portal.link.importId)
        val shape = """{"id":"$base","revision":2,"format":"ssAcademy-collections","version":1,"rewards":[],"animations":[]}"""
        for (bad in listOf("""{"import":{}}""", """{"import":${shape.replace(base, UUID.randomUUID().toString())}}""",
            """{"import":${shape.replace("\"rewards\":[]", "\"rewards\":[{\"id\":\"TOYS_3\",\"whole\":1,\"fragments\":3}]")}}""",
            "x".repeat(1024 * 1024 + 1))) {
            portal.readBody = bad
            sync.download(); looper.idle()
            assertNull(sync.preview)
            assertNotNull(sync.message)
            assertEquals(before, file.readText())
            assertEquals(base, portal.link.importId)
        }
        portal.readBody = null
        portal.edit(base, prize)
        sync.download(); looper.idle()
        val blocked = File(file.parentFile, "${file.name}.tmp")
        assertTrue(blocked.mkdir())
        try {
            sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
            assertNotNull(sync.preview)
            assertEquals(before, file.readText())
            assertEquals(base, portal.link.importId)
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
