package com.ssmath.app

import android.app.Application
import android.content.SharedPreferences
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
    var failSave = false
    val link = PortalLink(object : SharedPreferences by prefs {
        override fun edit(): SharedPreferences.Editor {
            val editor = prefs.edit()
            return object : SharedPreferences.Editor by editor {
                override fun putString(key: String?, value: String?) = apply { editor.putString(key, value) }
                override fun putBoolean(key: String?, value: Boolean) = apply { editor.putBoolean(key, value) }
                override fun remove(key: String?) = apply { editor.remove(key) }
                override fun clear() = apply { editor.clear() }
                override fun commit() = if (failSave) false else editor.commit()
            }
        }
    }) { key }
    var token: String? = null
    var bearerToken: String? = null
        private set
    var pendingBearerToken: String? = null
        private set
    var approvalBody: String? = null
    var beforeStatus: (() -> Unit)? = null
    var statusChecks = 0
    var syncRequests = 0
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
        if (path.startsWith("/ssacademy/api/imports")) syncRequests++
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
                else {
                    statusChecks++
                    beforeStatus?.invoke()
                }
                approvalBody ?: buildJsonObject {
                    put("device", buildJsonObject {
                        put("status", status); put("fingerprint", secret.sha256().take(16)); put("expiresAt", 1790000000000L)
                    })
                    if (path.endsWith("status") && status == "approved") pendingBearerToken?.let {
                        put("bearerToken", it)
                        pendingBearerToken = null
                    }
                }.toString()
            }
            path.startsWith("/ssacademy/api/imports") &&
                (bearerToken == null || req.header("Authorization") != "Bearer $bearerToken") -> {
                code = 401
                "{}"
            }
            path.startsWith("/ssacademy/api/imports/") -> {
                assertEquals("Bearer $bearerToken", req.header("Authorization"))
                assertEquals("GET", req.method)
                val record = records[path.substringAfterLast('/')]
                reads++
                code = readError ?: if (record == null) 404 else 200
                readBody ?: if (record == null) "{}" else buildJsonObject {
                    put("import", buildJsonObject {
                        put("id", record.id); put("revision", record.revision)
                        put("format", record.data.format); put("version", record.data.version)
                        record.data.settings?.let { put("settings", it) }
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
                assertEquals("Bearer $bearerToken", req.header("Authorization"))
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
    fun approve() {
        status = "approved"
        bearerToken = UUID.randomUUID().toString().sha256()
        pendingBearerToken = bearerToken
    }
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
    private val settings get() = application.getSharedPreferences("settings", 0)
    private fun localSync() = CollectionBackup.from(HistoryStore(file).loadSnapshot())
        .copy(settings = PortalSettings.read(settings))

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
        settings.edit().clear().commit()
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
        assertEquals(secret, portal.link.fingerprintToken(requireNotNull(sync.account)))
        assertEquals(id, sync.importId)
        sync.check(); looper.idle()
        assertTrue(sync.approved)
        sync.download(); looper.idle()
        assertEquals(id, sync.preview?.server?.id)
    }

    @Test fun enrollmentRetriesTheSameIdentifierAndProvisionedBearerAllowsUploadAndOwnedDownload() {
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
        val checks = portal.statusChecks
        portal.beforeStatus = { sync.check() }
        sync.check(); looper.idle(); tick()
        portal.beforeStatus = null
        assertEquals(checks + 1, portal.statusChecks)
        assertTrue(sync.approved)
        val bearer = requireNotNull(portal.bearerToken)
        assertNotEquals(secret, bearer)
        assertEquals(bearer, portal.link.bearerToken(requireNotNull(sync.account)))
        assertNull(portal.pendingBearerToken)
        assertFalse(application.getSharedPreferences("test_portal", 0).all.values.any { "$it".contains(bearer) })
        assertEquals(1, portal.uploads.size) // A settings-only device now has a portable snapshot.
        sync.upload(); looper.idle()
        assertEquals(2, portal.uploads.size)
        val id = requireNotNull(sync.importId)
        assertEquals(portal.records.getValue(id).data, portal.uploads.last())
        start()
        assertEquals(id, sync.importId)
        sync.check(); looper.idle()
        assertTrue(sync.approved)
        assertEquals(bearer, portal.link.bearerToken(requireNotNull(sync.account)))
        sync.download(); looper.idle()
        assertEquals(id, sync.preview?.server?.id)
        sync.cancelPreview()
        sync.disconnect(); looper.idle()
        assertNull(sync.account)
        assertNull(portal.link.importId)
    }

    @Test fun legacyApprovedIdentifiersRequireBearerProvisioningWithoutReenrollment() {
        connect()
        val secret = requireNotNull(portal.token)
        val id = requireNotNull(sync.importId)
        val requests = portal.syncRequests
        val prefs = application.getSharedPreferences("test_portal", 0)
        assertTrue(prefs.edit().remove("bearerCredential").putBoolean("granted", true).commit())
        start(); tick()
        assertFalse(sync.approved)
        assertEquals(requests, portal.syncRequests)
        request(); tick()
        assertFalse(sync.approved)
        assertTrue(sync.message.orEmpty().contains("generate a bearer token"))
        assertEquals(secret, portal.link.fingerprintToken(requireNotNull(sync.account)))
        assertEquals(id, sync.importId)
        assertEquals(requests, portal.syncRequests)
        portal.approve()
        sync.check(); looper.idle(); tick()
        assertTrue(sync.approved)
        assertEquals(id, sync.importId)
        assertEquals(secret, portal.token)
    }

    @Test fun rotatedBearerRecoversRejectedAccessAndKeepsDuplicateReceiptsAndAdministratorEdits() {
        connect()
        val secret = requireNotNull(portal.token)
        val bearer = requireNotNull(portal.bearerToken)
        val id = requireNotNull(sync.importId)
        portal.approve()
        sync.sync(); looper.idle()
        assertFalse(sync.approved)
        assertFalse(portal.link.granted)
        sync.check(); looper.idle()
        assertTrue(sync.approved)
        assertNotEquals(bearer, portal.link.bearerToken(requireNotNull(sync.account)))
        assertEquals(secret, portal.token)
        portal.edit(id, prize)
        sync.upload(); looper.idle()
        assertEquals(id, sync.importId)
        assertEquals(prize, sync.preview?.server?.data)
        assertFalse(sync.preview!!.allowUpload)
        assertEquals(CollectionBackup.from(PracticeSnapshot()), CollectionBackup.from(HistoryStore(file).loadSnapshot()))
    }

    @Test fun malformedOrUnapprovedBearerResponsesCannotEnableSync() {
        request()
        portal.approve()
        val device = """"fingerprint":"${sync.fingerprint}""""
        for (response in listOf(
            """{"device":{"status":"approved",$device},"bearerToken":"${"a".repeat(63)}"}""",
            """{"device":{"status":"approved",$device},"bearerToken":"${"g".repeat(64)}"}""",
            """{"device":{"status":"approved",$device},"bearerToken":123}""",
            """{"device":{"status":"pending",$device},"bearerToken":"${portal.bearerToken}"}"""
        )) {
            portal.approvalBody = response
            sync.check(); looper.idle(); tick()
            assertFalse(sync.approved)
            assertFalse(portal.link.granted)
            assertNotNull(sync.message)
            assertEquals(0, portal.syncRequests)
        }
        portal.approvalBody = null
        sync.check(); looper.idle()
        assertTrue(sync.approved)
    }

    @Test fun failedOneTimeCredentialSaveRequiresFreshDeliveryBeforeSyncCanResume() {
        request()
        portal.approve()
        portal.failSave = true
        sync.check(); looper.idle(); tick()
        assertFalse(sync.approved)
        assertFalse(portal.link.granted)
        assertNull(portal.pendingBearerToken)
        assertTrue(sync.message.orEmpty().contains("generate a bearer token"))
        assertEquals(0, portal.syncRequests)
        portal.failSave = false
        sync.check(); looper.idle(); tick()
        assertFalse(sync.approved)
        assertTrue(sync.message.orEmpty().contains("generate a bearer token"))
        assertEquals(0, portal.syncRequests)
        portal.approve()
        sync.check(); looper.idle()
        assertTrue(sync.approved)
        sync.upload(); looper.idle()
        assertEquals(2, portal.uploads.size)
    }

    @Test fun savedRewardIncreasesDecreasesAndSpendingSyncWithoutWaiting() {
        connect()
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        assertEquals(RewardBalance(0, 1), portal.uploads.last().rewards[RewardType.LOLLIPOP])
        model.addReward(RewardType.LOLLIPOP, false); looper.idle()
        assertEquals(RewardBalance(1, 1), portal.uploads.last().rewards[RewardType.LOLLIPOP])
        model.removeReward(RewardType.LOLLIPOP, true); looper.idle()
        assertEquals(RewardBalance(1), portal.uploads.last().rewards[RewardType.LOLLIPOP])
        model.openRewards(); looper.idle()
        model.useReward(RewardType.LOLLIPOP); looper.idle()
        val saved = CollectionBackup.from(HistoryStore(file).loadSnapshot())
        assertEquals(0, saved.rewards.getValue(RewardType.LOLLIPOP).totalFragments)
        assertEquals(localSync(), portal.uploads.last())
        assertEquals(5, portal.uploads.size)
    }

    @Test fun collectionAdditionsAndHistoryDeletionSyncWithoutWaiting() {
        val store = HistoryStore(file)
        val result = store.addNewResult(PracticeResult(
            id = 1, finishedAt = 1, operation = Operation.ADDITION, maximum = 10,
            durationMs = 1, attempts = emptyList()
        )).result
        store.collectPokemon(result.id, Celebration.PIKACHU)
        start()
        connect()
        assertEquals(setOf(Celebration.PIKACHU), portal.uploads.last().pokemons)
        model.deleteResult(result.id); looper.idle()
        assertTrue(portal.uploads.last().pokemons.isEmpty())
        model.collectAllCelebrations(); looper.idle()
        assertEquals(Celebration.entries.toSet(), portal.uploads.last().pokemons)
        assertEquals(3, portal.uploads.size)
        assertEquals(localSync(), portal.uploads.last())
    }

    @Test fun reloadsAndFailedSavesDoNotUpload() {
        connect()
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
        val edited = prize.copy(pokemons = setOf(Celebration.PARTY))
        portal.edit(id, edited)
        model.addReward(RewardType.LOLLIPOP, true); looper.idle()
        assertEquals(edited, sync.preview?.server?.data)
        assertEquals(setOf(Celebration.PIKACHU), model.pokemons)
        assertEquals(1, portal.uploads.size)
        sync.confirmDownload(CollectionImportMode.MERGE)
        sync.confirmDownload(CollectionImportMode.MERGE)
        looper.idle(); tick()
        assertEquals(RewardBalance(2), model.rewardBalances[RewardType.TOYS_3])
        assertEquals(setOf(Celebration.PIKACHU, Celebration.PARTY), model.pokemons)
        assertEquals(localSync(), portal.uploads.last())
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
        val saved = requireNotNull(sync.importId)
        assertNotEquals(before, saved)
        assertEquals(localSync(), portal.records.getValue(saved).data)
        sync.download(other); looper.idle()
        portal.edit(other, prize.copy(pokemons = emptySet()))
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertNull(sync.preview)
        assertEquals(saved, sync.importId)
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
        assertEquals(localSync(), portal.records.getValue(sync.importId!!).data)
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

    @Test fun settingsOnlyEditsUploadAndSecretsDoNotScheduleOrLeaveTheDevice() {
        connect()
        assertNotNull(portal.uploads.single().settings)
        model.chooseTextSize(120); looper.idle()
        assertEquals(JsonPrimitive(120), portal.uploads.last().settings?.get("text_size_percent"))
        val count = portal.uploads.size
        settings.edit().putString("game_name", "Local").putString("admin_pin_hash", "private")
            .putBoolean("admin_pin_lock_enabled", false).commit()
        looper.idle()
        assertEquals(count, portal.uploads.size)
        assertFalse(portal.uploads.last().settings!!.keys.any { it.startsWith("admin_pin") || it == "game_name" })
    }

    @Test fun linkedRemoteSettingsChangesApplyAutomaticallyWithoutEcho() {
        connect()
        val id = requireNotNull(sync.importId)
        val remote = localSync().copy(settings = JsonObject(mapOf(
            "text_size_percent" to JsonPrimitive(130), "show_correct_answers" to JsonPrimitive(false))))
        portal.edit(id, remote)
        val uploads = portal.uploads.size
        sync.sync(); looper.idle()
        assertEquals(130, model.textSizePercent)
        assertFalse(model.showCorrectAnswers)
        assertEquals(remote.settings, PortalSettings.read(settings))
        assertEquals(remote.digest(), portal.link.baseDigest)
        assertEquals(uploads, portal.uploads.size)
        assertNull(sync.preview)
    }

    @Test fun replacementAppliesSettingsReactivelyAndMergeAndLegacyPreserveLocalSettings() {
        connect()
        model.chooseTextSize(150); looper.idle()
        val remoteSettings = Json.decodeFromString(JsonObject.serializer(), """{"text_size_percent":120,
            "theme":"pink","show_timer":true,"reading_rewards_enabled":false,
            "logic_minimum_pictures":2,"logic_maximum_pictures":4,"logic_minimum_length":3,"logic_maximum_length":5}""")
        val remote = portal.insert(prize.copy(settings = remoteSettings))
        sync.download(remote); looper.idle()
        sync.confirmDownload(CollectionImportMode.MERGE); looper.idle()
        assertEquals(150, model.textSizePercent)
        assertEquals(localSync(), portal.uploads.last())
        sync.download(remote); looper.idle()
        val uploads = portal.uploads.size
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertEquals(120, model.textSizePercent)
        assertEquals("pink", model.theme)
        assertTrue(model.showTimer)
        assertFalse(model.rewardsEnabled(Subject.READING))
        assertEquals(4, model.logicMathSettings.maximumPictures)
        assertEquals(remoteSettings, PortalSettings.read(settings))
        assertEquals(uploads, portal.uploads.size)
        val legacy = portal.insert(prize)
        sync.download(legacy); looper.idle()
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertEquals(120, model.textSizePercent)
        assertEquals(remoteSettings, PortalSettings.read(settings))
        val defaults = portal.insert(prize.copy(settings = JsonObject(emptyMap())))
        sync.download(defaults); looper.idle()
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertEquals(100, model.textSizePercent)
        assertEquals("green", model.theme)
        assertFalse(model.showTimer)
        assertTrue(model.rewardsEnabled(Subject.READING))
        assertEquals(JsonObject(emptyMap()), PortalSettings.read(settings))
    }

    @Test fun settingsChangedAfterPreviewCannotBeOverwrittenAndChangesDuringUploadAreResent() {
        connect()
        val remote = portal.insert(prize.copy(settings = JsonObject(mapOf("text_size_percent" to JsonPrimitive(120)))))
        sync.download(remote); looper.idle()
        model.chooseTextSize(170); looper.idle()
        sync.confirmDownload(CollectionImportMode.REPLACE); looper.idle()
        assertEquals(170, model.textSizePercent)
        assertEquals(localSync(), portal.uploads.last())
        portal.beforeUpload = {
            portal.beforeUpload = null
            model.chooseTextSize(180)
        }
        model.chooseTextSize(190); looper.idle()
        assertEquals(JsonPrimitive(180), portal.uploads.last().settings?.get("text_size_percent"))
    }
}
