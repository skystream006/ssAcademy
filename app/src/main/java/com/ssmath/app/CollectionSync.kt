package com.ssmath.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CollectionChanged : IOException()

internal data class CollectionPreview(
    val server: ServerCollection,
    val local: CollectionBackup,
    val allowUpload: Boolean
)

internal class CollectionSync(
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val link: PortalLink,
    private val api: PortalApi,
    private val readLocal: suspend () -> CollectionBackup,
    private val importLocal: suspend (CollectionBackup, CollectionBackup, CollectionImportMode) -> Unit
) {
    var account by mutableStateOf(link.account)
        private set
    var fingerprint by mutableStateOf(link.fingerprint)
        private set
    var approved by mutableStateOf(link.granted)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var preview by mutableStateOf<CollectionPreview?>(null)
        private set

    private var scheduled: Job? = null
    private var pending = false
    private var retryDelay = 0L

    fun request(address: String, username: String) = run {
        val target = try {
            PortalAccount.from(address, username)
        } catch (error: IllegalArgumentException) {
            message = error.message
            return@run
        }
        withContext(io) { link.enroll(target) }
        checkApproval(target, register = true)
    }

    fun check() = run { checkApproval(requireNotNull(account), register = false) }

    private suspend fun checkApproval(target: PortalAccount, register: Boolean) {
        val device = withContext(io) {
            api.approval(target, link.token(target), register).also { link.grant(it.canSync) }
        }
        approved = device.canSync
        message = when {
            approved -> "Approved for upload and download."
            device.status == "approved" -> "Approved for upload only. Ask a portal administrator to grant collection sync."
            else -> "Device ${device.status}. Ask a portal administrator to approve collection sync, then check again."
        }
        if (approved) changed()
    }

    fun disconnect() = run {
        withContext(io) { link.forget() }
        approved = false
        pending = false
        preview = null
        retryDelay = 0L
        scheduled?.cancel()
        message = "Disconnected. Your collections are still on this device."
    }

    // Changes during an upload are coalesced, not dropped. The persisted baseline also
    // detects unsent changes after process death, even if shutdown preceded this callback.
    fun changed() {
        if (!approved) return
        pending = true
        schedule()
    }

    private fun schedule() {
        if (!approved || !pending || busy || preview != null || scheduled?.isActive == true) return
        scheduled = scope.launch {
            delay(maxOf(2_100L, retryDelay))
            scheduled = null
            if (!busy && approved && preview == null && pending) sync()
        }
    }

    fun sync() = run(retry = true) {
        if (!approved) return@run
        pending = false
        preview = null
        val remote = fetch()
        val local = readLocal()
        val digest = local.digest()
        val remoteDigest = remote.data?.digest()
        when {
            remoteDigest == digest -> acknowledge(remote, digest)
            remote.data == null && link.baseVersion == null -> push(remote, local)
            remote.data != null && (link.baseDigest == digest || (link.baseDigest == null && local.empty)) -> {
                importLocal(local, remote.data, CollectionImportMode.REPLACE)
                acknowledge(remote, requireNotNull(remoteDigest))
            }
            link.baseVersion == remote.version || (remoteDigest != null && remoteDigest == link.baseDigest) ->
                push(remote, local)
            else -> {
                preview = CollectionPreview(remote, local, allowUpload = true)
                message = "The device and server differ. Choose which collection to keep; nothing was overwritten."
            }
        }
    }

    fun upload() = run {
        val remote = fetch()
        val local = readLocal()
        when {
            remote.data == null -> push(remote, local)
            remote.data.digest() == local.digest() -> acknowledge(remote, local.digest())
            else -> {
                preview = CollectionPreview(remote, local, allowUpload = true)
                message = "Confirm to make this device's collection current on the server."
            }
        }
    }

    fun download() = run {
        val remote = fetch()
        if (remote.data == null) message = "There is no collection on the server yet."
        else {
            preview = CollectionPreview(remote, readLocal(), allowUpload = false)
            message = "Review the server collection before changing this device."
        }
    }

    fun confirmUpload() {
        val choice = preview?.takeIf { it.allowUpload } ?: return
        run {
            if (readLocal() != choice.local) throw CollectionChanged()
            push(choice.server, choice.local)
        }
    }

    fun confirmDownload(mode: CollectionImportMode) {
        val choice = preview ?: return
        val data = choice.server.data ?: return
        run {
            if (fetch().version != choice.server.version) throw CollectionChanged()
            importLocal(choice.local, data, mode)
            // Never repeat an additive merge if writing the checkpoint or uploading fails.
            preview = null
            acknowledge(choice.server, data.digest())
            if (mode == CollectionImportMode.MERGE) changed()
            message = if (mode == CollectionImportMode.MERGE) "Collections merged. Sync queued." else "Collections replaced."
        }
    }

    fun cancelPreview() {
        if (busy) return
        preview = null
        // An unchanged conflict should not reopen immediately after dismissal.
        message = null
        schedule()
    }

    private suspend fun fetch(): ServerCollection = withContext(io) {
        val target = requireNotNull(account)
        api.download(target, link.token(target))
    }

    private suspend fun push(remote: ServerCollection, local: CollectionBackup) {
        val result = withContext(io) {
            val target = requireNotNull(account)
            api.upload(target, link.token(target), remote.version, local)
        }
        val digest = local.digest()
        if (result.data?.digest() != digest) {
            preview = CollectionPreview(result, readLocal(), allowUpload = false)
            message = "The portal retained an administrator's edits. Review the server values before applying them."
        } else acknowledge(result, digest)
    }

    private suspend fun acknowledge(remote: ServerCollection, digest: String) {
        if (remote.version != link.baseVersion || digest != link.baseDigest)
            withContext(io) { link.acknowledge(remote.version, digest) }
        preview = null
        retryDelay = 0L
        message = "Collections are in sync."
    }

    private fun run(retry: Boolean = false, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                action()
            } catch (error: PortalHttpError) {
                DebugLog.event(DebugEvent.COLLECTION_SYNC_FAILURE, status = error.status)
                message = when (error.status) {
                    401, 403, 404 -> {
                        approved = false
                        preview = null
                        try {
                            withContext(io) { link.grant(false) }
                        } catch (failure: IOException) {
                            DebugLog.event(DebugEvent.COLLECTION_SYNC_FAILURE, error = failure)
                        }
                        "Portal access unavailable (${error.status}). Check the server address, sync support, and device approval."
                    }
                    409 -> {
                        preview = null
                        "Portal conflict. Check the registration or sync again to review the latest collection."
                    }
                    429 -> "Portal rate limit reached. Wait before retrying."
                    else -> "Portal request failed (HTTP ${error.status}). Check the server and try again."
                }
                if (retry && (error.status == 429 || error.status >= 500)) retryLater()
            } catch (error: CollectionChanged) {
                preview = null
                message = "The collection changed during this request. Sync again to review the latest values."
            } catch (error: IOException) {
                DebugLog.event(DebugEvent.COLLECTION_SYNC_FAILURE, error = error)
                message = "Sync did not finish. Check connectivity, the portal's HTTPS certificate, and device storage, then retry."
                if (retry) retryLater()
            } catch (error: GeneralSecurityException) {
                DebugLog.event(DebugEvent.COLLECTION_SYNC_FAILURE, error = error)
                message = "Unable to unlock the device credential. Disconnect and request access again."
            } catch (error: IllegalArgumentException) {
                DebugLog.event(DebugEvent.COLLECTION_SYNC_FAILURE, error = error)
                message = "Unsupported collection, invalid response, or reward balance limit exceeded. Check both apps and retry."
            } finally {
                account = link.account
                fingerprint = link.fingerprint
                busy = false
                schedule()
            }
        }
    }

    private fun retryLater() {
        retryDelay = (retryDelay * 2).coerceIn(60_000L, 900_000L)
        pending = true
    }
}
