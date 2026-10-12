package core.data.upload.session

import co.touchlab.kermit.Logger
import core.data.upload.BackgroundUploadDescriptorStorage
import core.domain.upload.BackgroundUploadScheduler
import core.domain.upload.BackgroundUploadSessionRenewer
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import core.domain.upload.model.BackgroundUploadStatus
import core.domain.upload.model.UploadProgress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

private const val HTTP_METHOD_POST = "POST"
private const val HTTP_METHOD_PUT = "PUT"
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_SUCCESS_STATUS_CODES_START = 200
private const val HTTP_SUCCESS_STATUS_CODES_END = 299
private const val HEADER_AUTHORIZATION = "Authorization"
private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_CONTENT_LENGTH = "Content-Length"
private const val RESPONSE_PREVIEW_LENGTH = 200
private const val EVENT_BUFFER_CAPACITY = 64
const val DEFAULT_UPLOAD_STALL_TIMEOUT_MILLIS = 2 * 60 * 1000L

/**
 * Runs uploads through a background transfer service one at a time: prepares each request with the
 * app's [requestHandler], writes multipart bodies to disk, adds a fresh bearer token, renews the
 * session once when the server answers 401, and reports progress and completions. Direct uploads
 * are recognised by the remote id stored in their descriptor, so a completion the OS delivers after
 * the app was killed and relaunched still commits.
 */
class BackgroundUploadSessionScheduler<T>(
    private val descriptorStorage: BackgroundUploadDescriptorStorage,
    private val requestHandler: BackgroundUploadRequestHandler<T>,
    private val sessionRenewer: BackgroundUploadSessionRenewer,
    private val transport: BackgroundUploadTransport,
    private val fileStore: BackgroundUploadFileStore,
    private val coroutineScope: CoroutineScope,
    private val logTag: String,
    private val stallTimeoutMillis: Long = DEFAULT_UPLOAD_STALL_TIMEOUT_MILLIS,
) : BackgroundUploadScheduler<T>, BackgroundUploadTransferListener {

    private val completionMutableFlow = MutableSharedFlow<BackgroundUploadCompletion<T>>(
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
    )
    private val progressMutableFlow = MutableSharedFlow<UploadProgress>(
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // Serializes active transfers so only one file uploads at a time: concurrent background tasks
    // compete for bandwidth and trigger OS restarts.
    private val uploadMutex = Mutex()
    private val pendingDeferreds = mutableMapOf<String, CompletableDeferred<Unit>>()

    // Last reported byte count per active task; lets the wait loop tell a slow-but-moving
    // transfer apart from a dead one, so a large video is never cancelled mid-flight.
    private val lastBytesSentByUploadId = mutableMapOf<String, Long>()

    // One renewal attempt per upload: a refresh token the backend already rejected would
    // otherwise send the same upload around this loop forever.
    private val sessionRenewedUploadIds = mutableSetOf<String>()

    override val completionEvents: Flow<BackgroundUploadCompletion<T>> = completionMutableFlow.asSharedFlow()

    override val progressEvents: Flow<UploadProgress> = progressMutableFlow.asSharedFlow()

    init {
        transport.bind(listener = this)
    }

    /** Returns immediately; preparation and the transfer run in the scheduler's scope. */
    override suspend fun schedule(descriptor: BackgroundUploadDescriptor) {
        coroutineScope.launch {
            runCatching { startUpload(descriptor) }
                .onFailure { throwable -> Logger.e(throwable) { "[$logTag] Schedule failed uploadId=${descriptor.uploadId}" } }
        }
    }

    override fun cancelAll() {
        transport.cancelAllTasks()
        Logger.i { "[$logTag] Cancel requested — cancelled all tasks" }
    }

    override suspend fun queryStatus(uploadId: String): BackgroundUploadStatus =
        if (descriptorStorage.load(uploadId) != null) {
            BackgroundUploadStatus.InProgress
        } else {
            BackgroundUploadStatus.NotFound
        }

    fun reconnect(onEventsDelivered: () -> Unit) {
        transport.reconnect(onEventsDelivered)
        deleteOrphanedBodyFiles()
        Logger.i { "[$logTag] Reconnected to background session" }
    }

    override fun onTransferProgress(uploadId: String, bytesSent: Long, bytesTotal: Long) {
        lastBytesSentByUploadId[uploadId] = bytesSent
        progressMutableFlow.tryEmit(UploadProgress(uploadId = uploadId, bytesSent = bytesSent, bytesTotal = bytesTotal))
    }

    override fun onTransferCompleted(uploadId: String, outcome: BackgroundUploadTransferOutcome) {
        coroutineScope.launch { handleTransferOutcome(uploadId = uploadId, outcome = outcome) }
    }

    private suspend fun startUpload(descriptor: BackgroundUploadDescriptor) {
        val startedAt = TimeSource.Monotonic.markNow()
        Logger.i { "[$logTag:STEP] START uploadId=${descriptor.uploadId} isVideo=${descriptor.isVideo}" }

        when (val preparation = requestHandler.prepareUpload(descriptor)) {
            is BackgroundUploadPreparation.Multipart -> startMultipartUpload(descriptor, preparation, startedAt)
            is BackgroundUploadPreparation.DirectFile -> startDirectUpload(descriptor, preparation, startedAt)
            is BackgroundUploadPreparation.Failed -> handleUploadFailure(descriptor.uploadId, preparation.reason)
            is BackgroundUploadPreparation.Abandoned ->
                Logger.e { "[$logTag:STEP] FAIL ${preparation.logDetail} uploadId=${descriptor.uploadId}" }
        }
    }

    private suspend fun startMultipartUpload(
        descriptor: BackgroundUploadDescriptor,
        preparation: BackgroundUploadPreparation.Multipart,
        startedAt: TimeSource.Monotonic.ValueTimeMark,
    ) {
        val bodyFilePath = fileStore.write(uploadId = descriptor.uploadId, bytes = preparation.body.bytes)
        if (bodyFilePath == null) {
            Logger.e { "[$logTag:STEP] FAIL write_multipart_file uploadId=${descriptor.uploadId}" }
            return
        }

        sessionRenewer.renewSession(isForced = false)
        val requestHeaders = buildMap {
            put(HEADER_AUTHORIZATION, "Bearer ${sessionRenewer.fetchAccessToken()}")
            put(HEADER_CONTENT_TYPE, preparation.body.contentType)
            put(HEADER_CONTENT_LENGTH, preparation.body.contentLength.toString())
            putAll(preparation.headers)
        }
        val request = BackgroundUploadTransferRequest(
            httpMethod = HTTP_METHOD_POST,
            url = preparation.url,
            headers = requestHeaders,
            bodyFilePath = bodyFilePath,
        )
        val task = transport.createTask(uploadId = descriptor.uploadId, request = request)
        if (task == null) {
            Logger.e { "[$logTag:STEP] FAIL invalid_url uploadId=${descriptor.uploadId}" }
            return
        }

        resumeAndAwaitCompletion(task = task, uploadId = descriptor.uploadId, startedAt = startedAt)
    }

    private suspend fun startDirectUpload(
        descriptor: BackgroundUploadDescriptor,
        preparation: BackgroundUploadPreparation.DirectFile,
        startedAt: TimeSource.Monotonic.ValueTimeMark,
    ) {
        descriptorStorage.save(descriptor.copy(remoteId = preparation.remoteId))
        Logger.i { "[$logTag:STEP] presigned remoteId=${preparation.remoteId} uploadId=${descriptor.uploadId}" }

        val request = BackgroundUploadTransferRequest(
            httpMethod = HTTP_METHOD_PUT,
            url = preparation.url,
            headers = mapOf(HEADER_CONTENT_TYPE to preparation.contentType),
            bodyFilePath = preparation.filePath,
        )
        val task = transport.createTask(uploadId = descriptor.uploadId, request = request)
        if (task == null) {
            Logger.e { "[$logTag:STEP] FAIL invalid_presigned_url uploadId=${descriptor.uploadId}" }
            handleUploadFailure(descriptor.uploadId, BackgroundUploadFailureReason.INVALID_PRESIGNED_URL)
            return
        }

        resumeAndAwaitCompletion(task = task, uploadId = descriptor.uploadId, startedAt = startedAt)
    }

    private suspend fun resumeAndAwaitCompletion(
        task: BackgroundUploadTask,
        uploadId: String,
        startedAt: TimeSource.Monotonic.ValueTimeMark,
    ) {
        uploadMutex.withLock {
            val deferred = CompletableDeferred<Unit>()
            pendingDeferreds[uploadId] = deferred
            task.resume()
            Logger.i { "[$logTag:STEP] TASK_RESUMED uploadId=$uploadId taskId=${task.taskIdentifier} totalElapsed=${startedAt.elapsedNow()}" }
            awaitCompletionOrStall(task = task, uploadId = uploadId, deferred = deferred)
            lastBytesSentByUploadId.remove(uploadId)
        }
    }

    // A fixed deadline would cancel a healthy transfer on a slow link, so the guard is a stall
    // check: give up only when no bytes have moved for a whole window (e.g. delegate callbacks
    // lost after an app kill mid-upload).
    private suspend fun awaitCompletionOrStall(
        task: BackgroundUploadTask,
        uploadId: String,
        deferred: CompletableDeferred<Unit>,
    ) {
        var lastObservedBytesSent = -1L
        while (true) {
            val completed = withTimeoutOrNull(stallTimeoutMillis) { deferred.await() }
            if (completed != null) return
            val bytesSentNow = lastBytesSentByUploadId[uploadId] ?: -1L
            if (bytesSentNow > lastObservedBytesSent) {
                lastObservedBytesSent = bytesSentNow
                continue
            }
            Logger.w { "[$logTag:STEP] STALLED uploadId=$uploadId bytesSent=$bytesSentNow — cancelling task and releasing slot" }
            task.cancel()
            pendingDeferreds.remove(uploadId)
            return
        }
    }

    private suspend fun handleTransferOutcome(uploadId: String, outcome: BackgroundUploadTransferOutcome) {
        when (outcome) {
            is BackgroundUploadTransferOutcome.NetworkError -> {
                Logger.e { "[$logTag] NETWORK_ERROR uploadId=$uploadId reason=${outcome.failureReason}" }
                handleUploadFailure(uploadId, outcome.failureReason)
            }

            is BackgroundUploadTransferOutcome.Response -> handleResponse(uploadId = uploadId, response = outcome)
        }
    }

    private suspend fun handleResponse(uploadId: String, response: BackgroundUploadTransferOutcome.Response) {
        val descriptor = descriptorStorage.load(uploadId)
        val directUploadRemoteId = descriptor?.remoteId
        val isSuccessStatusCode = response.statusCode in HTTP_SUCCESS_STATUS_CODES_START..HTTP_SUCCESS_STATUS_CODES_END

        // A bearer that expired while the OS held the task must not drop the media. Direct uploads
        // carry a presigned url instead of a bearer, so their 401 is not a session problem.
        if (response.statusCode == HTTP_UNAUTHORIZED && directUploadRemoteId == null) {
            Logger.w { "[$logTag] UNAUTHORIZED uploadId=$uploadId — renewing the session before a retry" }
            retryWithRenewedSession(uploadId)
            return
        }

        if (!isSuccessStatusCode) {
            val responsePreview = response.body?.take(RESPONSE_PREVIEW_LENGTH) ?: "(no body)"
            Logger.e { "[$logTag] HTTP_ERROR uploadId=$uploadId statusCode=${response.statusCode} body=$responsePreview" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.httpStatus(response.statusCode))
            return
        }

        if (descriptor != null && directUploadRemoteId != null) {
            commitDirectUpload(descriptor = descriptor, remoteId = directUploadRemoteId)
            return
        }

        val responseBody = response.body
        if (responseBody.isNullOrEmpty()) {
            Logger.e { "[$logTag] EMPTY_RESPONSE uploadId=$uploadId statusCode=${response.statusCode}" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.EMPTY_RESPONSE)
            return
        }

        Logger.i { "[$logTag] parsing response uploadId=$uploadId body=$responseBody" }
        val result = try {
            requestHandler.parseResponse(responseBody)
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag] PARSE_ERROR uploadId=$uploadId" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.parseError(exception.message))
            return
        }
        handleUploadSuccess(uploadId = uploadId, result = result)
    }

    private suspend fun commitDirectUpload(descriptor: BackgroundUploadDescriptor, remoteId: Int) {
        val result = try {
            requestHandler.commitDirectUpload(descriptor = descriptor, remoteId = remoteId)
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag:STEP] FAIL commit remoteId=$remoteId uploadId=${descriptor.uploadId}" }
            handleUploadFailure(descriptor.uploadId, BackgroundUploadFailureReason.COMMIT_FAILED)
            return
        }
        handleUploadSuccess(uploadId = descriptor.uploadId, result = result)
    }

    private suspend fun retryWithRenewedSession(uploadId: String) {
        if (!sessionRenewedUploadIds.add(uploadId)) {
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.UNAUTHORIZED)
            return
        }

        val descriptor = descriptorStorage.load(uploadId)
        if (descriptor == null) {
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.UNAUTHORIZED)
            return
        }

        val isSessionRenewed = sessionRenewer.renewSession(isForced = true)
        if (!isSessionRenewed) {
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.UNAUTHORIZED)
            return
        }

        // Releases the slot the failed task still occupies before the replacement asks for it.
        pendingDeferreds.remove(uploadId)?.complete(Unit)
        lastBytesSentByUploadId.remove(uploadId)

        try {
            startUpload(descriptor)
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag] Retry after session renewal failed uploadId=$uploadId" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.UNAUTHORIZED)
        }
    }

    private suspend fun handleUploadSuccess(uploadId: String, result: T) {
        sessionRenewedUploadIds.remove(uploadId)
        descriptorStorage.remove(uploadId)
        fileStore.delete(uploadId)
        completionMutableFlow.tryEmit(BackgroundUploadCompletion.Success(uploadId = uploadId, result = result))
        pendingDeferreds.remove(uploadId)?.complete(Unit)
        Logger.i { "[$logTag] Success uploadId=$uploadId" }

        // Last and best-effort: the upload has landed, so app follow-ups must neither break the
        // completion handler nor hold the upload slot open.
        try {
            requestHandler.onUploadSucceeded(uploadId = uploadId, result = result)
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag] Follow-up after success failed uploadId=$uploadId" }
        }
    }

    private fun handleUploadFailure(uploadId: String, reason: String) {
        sessionRenewedUploadIds.remove(uploadId)
        descriptorStorage.remove(uploadId)
        fileStore.delete(uploadId)
        completionMutableFlow.tryEmit(BackgroundUploadCompletion.PermanentFailure(uploadId = uploadId, reason = reason))
        pendingDeferreds.remove(uploadId)?.complete(Unit)
        Logger.e { "[$logTag] Failed uploadId=$uploadId reason=$reason" }
    }

    private fun deleteOrphanedBodyFiles() {
        fileStore.listUploadIds()
            .filter { uploadId -> descriptorStorage.load(uploadId) == null }
            .forEach { uploadId ->
                fileStore.delete(uploadId)
                Logger.i { "[$logTag] Cleaned orphaned body file uploadId=$uploadId" }
            }
    }
}
