package core.data.upload.session

import co.touchlab.kermit.Logger
import core.data.upload.BackgroundUploadDescriptorStorage
import core.domain.upload.BackgroundUploadScheduler
import core.domain.upload.BackgroundUploadSessionRenewer
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import core.domain.upload.model.BackgroundUploadStatus
import core.domain.upload.model.PartedUpload
import core.domain.upload.model.StoredPart
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

private const val HTTP_METHOD_POST = "POST"
private const val HTTP_METHOD_PUT = "PUT"
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_SERVER_ERROR_STATUS_CODES_START = 500
private const val HTTP_SUCCESS_STATUS_CODES_START = 200
private const val HTTP_SUCCESS_STATUS_CODES_END = 299
private const val HEADER_AUTHORIZATION = "Authorization"
private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_CONTENT_LENGTH = "Content-Length"
private const val RESPONSE_PREVIEW_LENGTH = 200
private const val EVENT_BUFFER_CAPACITY = 64
const val DEFAULT_UPLOAD_STALL_TIMEOUT_MILLIS = 2 * 60 * 1000L
const val DEFAULT_MAX_PART_ATTEMPTS = 3

/**
 * Runs uploads through a background transfer service one at a time: prepares each request with the
 * app's [requestHandler], writes multipart bodies to disk, adds a fresh bearer token, renews the
 * session once when the server answers 401, and reports progress and completions. Direct uploads
 * are recognised by the remote id stored in their descriptor, so a completion the OS delivers after
 * the app was killed and relaunched still commits.
 *
 * Parted uploads ([BackgroundUploadPreparation.DirectFileInParts]) send one part per transfer and
 * store each part's ETag in the descriptor before the next part starts. When a part keeps failing
 * on the network, or a part cannot be signed, the upload pauses instead of failing: its descriptor
 * stays, and [resumeInterruptedUploads] continues it from the first part that is not stored.
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
    private val partHandler: BackgroundUploadPartHandler<T>? = null,
    private val maxPartAttempts: Int = DEFAULT_MAX_PART_ATTEMPTS,
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

    // A part completion checks and saves the stored parts as one step, so two completions of the
    // same part (the OS finishing a task that a resume already restarted) continue the upload once.
    private val partStateMutex = Mutex()
    private val partAttemptsByTransferId = mutableMapOf<String, Int>()
    private val partProgressBaseByTransferId = mutableMapOf<String, PartProgressBase>()

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

    /**
     * Continues every parted upload whose descriptor is still stored, from its first part that is
     * not stored. Call it once after [reconnect] when the app starts. A part the OS is still sending
     * may be sent twice; the second completion of a stored part is ignored.
     */
    fun resumeInterruptedUploads() {
        descriptorStorage.loadAll()
            .filter { descriptor -> descriptor.partedUpload != null }
            .forEach { descriptor ->
                Logger.i { "[$logTag] Resuming parted upload uploadId=${descriptor.uploadId}" }
                coroutineScope.launch {
                    runCatching { uploadNextPart(descriptor) }
                        .onFailure { throwable -> Logger.e(throwable) { "[$logTag] Resume failed uploadId=${descriptor.uploadId}" } }
                }
            }
    }

    fun reconnect(onEventsDelivered: () -> Unit) {
        transport.reconnect(onEventsDelivered)
        deleteOrphanedBodyFiles()
        Logger.i { "[$logTag] Reconnected to background session" }
    }

    override fun onTransferProgress(uploadId: String, bytesSent: Long, bytesTotal: Long) {
        lastBytesSentByUploadId[uploadId] = bytesSent
        val transferId = BackgroundUploadTransferId.parse(uploadId)
        if (transferId.partNumber == null) {
            progressMutableFlow.tryEmit(UploadProgress(uploadId = uploadId, bytesSent = bytesSent, bytesTotal = bytesTotal))
            return
        }
        // A part reports its own bytes; the upload's progress adds the parts stored before it.
        // A part the OS kept running across an app kill has no base yet, so it reports nothing.
        val progressBase = partProgressBaseByTransferId[uploadId] ?: return
        progressMutableFlow.tryEmit(
            UploadProgress(
                uploadId = transferId.uploadId,
                bytesSent = progressBase.storedBytes + bytesSent,
                bytesTotal = progressBase.totalBytes,
            ),
        )
    }

    override fun onTransferCompleted(uploadId: String, outcome: BackgroundUploadTransferOutcome) {
        coroutineScope.launch {
            val transferId = BackgroundUploadTransferId.parse(uploadId)
            val partNumber = transferId.partNumber
            if (partNumber == null) {
                handleTransferOutcome(uploadId = uploadId, outcome = outcome)
            } else {
                handlePartOutcome(uploadId = transferId.uploadId, partNumber = partNumber, outcome = outcome)
            }
        }
    }

    private suspend fun startUpload(descriptor: BackgroundUploadDescriptor) {
        val startedAt = TimeSource.Monotonic.markNow()
        Logger.i { "[$logTag:STEP] START uploadId=${descriptor.uploadId} isVideo=${descriptor.isVideo}" }

        // Scheduling an upload that already stored parts continues it; preparing it again would
        // open a second multipart upload on the server.
        val storedPartedDescriptor = descriptorStorage.load(descriptor.uploadId)?.takeIf { stored -> stored.partedUpload != null }
        if (storedPartedDescriptor != null) {
            uploadNextPart(storedPartedDescriptor)
            return
        }

        when (val preparation = requestHandler.prepareUpload(descriptor)) {
            is BackgroundUploadPreparation.Multipart -> startMultipartUpload(descriptor, preparation, startedAt)
            is BackgroundUploadPreparation.DirectFile -> startDirectUpload(descriptor, preparation, startedAt)
            is BackgroundUploadPreparation.DirectFileInParts -> startPartedUpload(descriptor, preparation)
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
        // Without a token the request goes out unauthenticated; the server's 401 then takes the
        // same renew-once path as an expired token.
        val accessToken = sessionRenewer.fetchAccessToken()
        val requestHeaders = buildMap {
            accessToken?.let { token -> put(HEADER_AUTHORIZATION, "Bearer $token") }
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

    private suspend fun startPartedUpload(
        descriptor: BackgroundUploadDescriptor,
        preparation: BackgroundUploadPreparation.DirectFileInParts,
    ) {
        val partedUpload = try {
            PartedUpload(
                remoteKey = preparation.remoteKey,
                filePath = preparation.filePath,
                totalBytes = preparation.totalBytes,
                partSizeBytes = preparation.partSizeBytes,
            )
        } catch (exception: IllegalArgumentException) {
            Logger.e(exception) { "[$logTag:STEP] FAIL invalid_part_layout uploadId=${descriptor.uploadId}" }
            handleUploadFailure(descriptor.uploadId, BackgroundUploadFailureReason.INVALID_PART_LAYOUT)
            return
        }
        val partedDescriptor = descriptor.copy(partedUpload = partedUpload)
        descriptorStorage.save(partedDescriptor)
        Logger.i { "[$logTag:STEP] parted upload opened parts=${partedUpload.partCount} uploadId=${descriptor.uploadId}" }
        uploadNextPart(partedDescriptor)
    }

    private suspend fun uploadNextPart(descriptor: BackgroundUploadDescriptor) {
        val uploadId = descriptor.uploadId
        val partedUpload = requireNotNull(descriptor.partedUpload) { "Only parted descriptors reach the part loop" }
        val handler = partHandler
        if (handler == null) {
            Logger.e { "[$logTag:STEP] FAIL part_handler_missing uploadId=$uploadId" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.PART_HANDLER_MISSING)
            return
        }

        val partNumber = partedUpload.nextPendingPartNumber()
        if (partNumber == null) {
            completeParts(descriptor = descriptor, storedParts = partedUpload.storedParts, handler = handler)
            return
        }

        val transferId = BackgroundUploadTransferId(uploadId = uploadId, partNumber = partNumber).value
        val attempt = (partAttemptsByTransferId[transferId] ?: 0) + 1
        if (attempt > maxPartAttempts) {
            pausePartedUpload(uploadId = uploadId, transferId = transferId, detail = "attempts_exhausted part=$partNumber")
            return
        }
        partAttemptsByTransferId[transferId] = attempt

        val partUrl = try {
            handler.signPart(descriptor = descriptor, partNumber = partNumber)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag:STEP] sign part failed part=$partNumber uploadId=$uploadId" }
            pausePartedUpload(uploadId = uploadId, transferId = transferId, detail = "sign_failed part=$partNumber")
            return
        }

        val byteRange = partedUpload.byteRangeOf(partNumber)
        val bodyFilePath = fileStore.writePart(
            transferId = transferId,
            sourceFilePath = partedUpload.filePath,
            offset = byteRange.offset,
            length = byteRange.length,
        )
        if (bodyFilePath == null) {
            Logger.e { "[$logTag:STEP] FAIL write_part_file part=$partNumber uploadId=$uploadId" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.WRITE_PART_FILE_FAILED)
            return
        }

        // No headers: the presigned url carries the authorization, and an added header that is not
        // part of the signature makes storage reject the request.
        val request = BackgroundUploadTransferRequest(
            httpMethod = HTTP_METHOD_PUT,
            url = partUrl,
            headers = emptyMap(),
            bodyFilePath = bodyFilePath,
        )
        val task = transport.createTask(uploadId = transferId, request = request)
        if (task == null) {
            Logger.e { "[$logTag:STEP] FAIL invalid_presigned_url part=$partNumber uploadId=$uploadId" }
            handleUploadFailure(uploadId, BackgroundUploadFailureReason.INVALID_PRESIGNED_URL)
            return
        }

        partProgressBaseByTransferId[transferId] = PartProgressBase(
            storedBytes = partedUpload.storedBytes,
            totalBytes = partedUpload.totalBytes,
        )
        Logger.i { "[$logTag:STEP] part $partNumber/${partedUpload.partCount} attempt=$attempt uploadId=$uploadId" }
        resumeAndAwaitCompletion(task = task, uploadId = transferId, startedAt = TimeSource.Monotonic.markNow())
    }

    private suspend fun handlePartOutcome(uploadId: String, partNumber: Int, outcome: BackgroundUploadTransferOutcome) {
        val transferId = BackgroundUploadTransferId(uploadId = uploadId, partNumber = partNumber).value
        val decision = decidePartOutcome(uploadId = uploadId, partNumber = partNumber, outcome = outcome)
        releaseTransfer(transferId)

        when (decision) {
            PartOutcomeDecision.Ignore ->
                Logger.w { "[$logTag] Ignored completion of a part that is stored or no longer uploading part=$partNumber uploadId=$uploadId" }

            is PartOutcomeDecision.Retry -> {
                Logger.w { "[$logTag] Part failed, retrying part=$partNumber uploadId=$uploadId reason=${decision.reason}" }
                uploadNextPart(decision.descriptor)
            }

            is PartOutcomeDecision.Fail -> handleUploadFailure(uploadId, decision.reason)

            is PartOutcomeDecision.Stored -> {
                fileStore.delete(transferId)
                partAttemptsByTransferId.remove(transferId)
                uploadNextPart(decision.descriptor)
            }
        }
    }

    private suspend fun decidePartOutcome(
        uploadId: String,
        partNumber: Int,
        outcome: BackgroundUploadTransferOutcome,
    ): PartOutcomeDecision = partStateMutex.withLock {
        val descriptor = descriptorStorage.load(uploadId)
        val partedUpload = descriptor?.partedUpload
        if (descriptor == null || partedUpload == null || partedUpload.isStored(partNumber)) {
            return@withLock PartOutcomeDecision.Ignore
        }

        when (outcome) {
            is BackgroundUploadTransferOutcome.NetworkError -> PartOutcomeDecision.Retry(descriptor = descriptor, reason = outcome.failureReason)
            is BackgroundUploadTransferOutcome.Response -> decidePartResponse(
                descriptor = descriptor,
                partedUpload = partedUpload,
                partNumber = partNumber,
                response = outcome,
            )
        }
    }

    private fun decidePartResponse(
        descriptor: BackgroundUploadDescriptor,
        partedUpload: PartedUpload,
        partNumber: Int,
        response: BackgroundUploadTransferOutcome.Response,
    ): PartOutcomeDecision {
        val statusCode = response.statusCode
        // Storage answers 403 when the presigned url expired while the part waited, so the part is
        // signed again; a server error is worth another attempt too.
        val isRetryableStatusCode = statusCode == HTTP_FORBIDDEN || statusCode >= HTTP_SERVER_ERROR_STATUS_CODES_START
        if (isRetryableStatusCode) {
            return PartOutcomeDecision.Retry(descriptor = descriptor, reason = BackgroundUploadFailureReason.httpStatus(statusCode))
        }
        if (statusCode !in HTTP_SUCCESS_STATUS_CODES_START..HTTP_SUCCESS_STATUS_CODES_END) {
            val responsePreview = response.body?.take(RESPONSE_PREVIEW_LENGTH) ?: "(no body)"
            Logger.e { "[$logTag] HTTP_ERROR part=$partNumber uploadId=${descriptor.uploadId} statusCode=$statusCode body=$responsePreview" }
            return PartOutcomeDecision.Fail(reason = BackgroundUploadFailureReason.httpStatus(statusCode))
        }

        val entityTag = response.entityTag?.takeIf { tag -> tag.isNotBlank() }
        if (entityTag == null) {
            Logger.e { "[$logTag] Part stored without an ETag part=$partNumber uploadId=${descriptor.uploadId}" }
            return PartOutcomeDecision.Fail(reason = BackgroundUploadFailureReason.PART_ENTITY_TAG_MISSING)
        }

        val updatedDescriptor = descriptor.copy(
            partedUpload = partedUpload.withStoredPart(StoredPart(partNumber = partNumber, entityTag = entityTag)),
        )
        descriptorStorage.save(updatedDescriptor)
        return PartOutcomeDecision.Stored(descriptor = updatedDescriptor)
    }

    private suspend fun completeParts(
        descriptor: BackgroundUploadDescriptor,
        storedParts: List<StoredPart>,
        handler: BackgroundUploadPartHandler<T>,
    ) {
        val result = try {
            handler.completeParts(descriptor = descriptor, storedParts = storedParts)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag:STEP] FAIL complete parts uploadId=${descriptor.uploadId}" }
            handleUploadFailure(descriptor.uploadId, BackgroundUploadFailureReason.COMMIT_FAILED)
            return
        }
        handleUploadSuccess(uploadId = descriptor.uploadId, result = result)
    }

    private fun pausePartedUpload(uploadId: String, transferId: String, detail: String) {
        partAttemptsByTransferId.remove(transferId)
        fileStore.delete(transferId)
        Logger.w { "[$logTag:STEP] PAUSED $detail uploadId=$uploadId — resumes on the next resumeInterruptedUploads" }
    }

    private fun releaseTransfer(transferId: String) {
        pendingDeferreds.remove(transferId)?.complete(Unit)
        lastBytesSentByUploadId.remove(transferId)
        partProgressBaseByTransferId.remove(transferId)
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
        var lastObservedBytesSent: Long? = null
        while (true) {
            val completed = withTimeoutOrNull(stallTimeoutMillis) { deferred.await() }
            if (completed != null) return
            val bytesSentNow = lastBytesSentByUploadId[uploadId]
            val hasTransferMoved = bytesSentNow != null && (lastObservedBytesSent == null || bytesSentNow > lastObservedBytesSent)
            if (hasTransferMoved) {
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
        deleteBodyFiles(uploadId)
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
        abortPartedUpload(descriptorStorage.load(uploadId))
        sessionRenewedUploadIds.remove(uploadId)
        descriptorStorage.remove(uploadId)
        deleteBodyFiles(uploadId)
        completionMutableFlow.tryEmit(BackgroundUploadCompletion.PermanentFailure(uploadId = uploadId, reason = reason))
        pendingDeferreds.remove(uploadId)?.complete(Unit)
        Logger.e { "[$logTag] Failed uploadId=$uploadId reason=$reason" }
    }

    // Best-effort and off the failure path: the failure is reported whether or not the server
    // answers, and an aborted multipart upload stops costing storage.
    private fun abortPartedUpload(descriptor: BackgroundUploadDescriptor?) {
        val handler = partHandler ?: return
        if (descriptor?.partedUpload == null) return
        coroutineScope.launch {
            try {
                handler.abortParts(descriptor)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.e(exception) { "[$logTag] Abort of parted upload failed uploadId=${descriptor.uploadId}" }
            }
        }
    }

    // Covers the whole-file body and every part body: both parse back to this upload id.
    private fun deleteBodyFiles(uploadId: String) {
        fileStore.listUploadIds()
            .filter { transferId -> BackgroundUploadTransferId.parse(transferId).uploadId == uploadId }
            .forEach(fileStore::delete)
    }

    private fun deleteOrphanedBodyFiles() {
        fileStore.listUploadIds()
            .filter { transferId -> descriptorStorage.load(BackgroundUploadTransferId.parse(transferId).uploadId) == null }
            .forEach { transferId ->
                fileStore.delete(transferId)
                Logger.i { "[$logTag] Cleaned orphaned body file transferId=$transferId" }
            }
    }

    private data class PartProgressBase(
        val storedBytes: Long,
        val totalBytes: Long,
    )

    private sealed interface PartOutcomeDecision {
        data object Ignore : PartOutcomeDecision

        data class Retry(val descriptor: BackgroundUploadDescriptor, val reason: String) : PartOutcomeDecision

        data class Fail(val reason: String) : PartOutcomeDecision

        data class Stored(val descriptor: BackgroundUploadDescriptor) : PartOutcomeDecision
    }
}
