package core.data.upload.session

import com.russhwolf.settings.MapSettings
import core.data.upload.BackgroundUploadDescriptorStorage
import core.data.upload.MultipartBody
import core.data.upload.fake.FakeBackgroundUploadFileStore
import core.data.upload.fake.FakeBackgroundUploadRequestHandler
import core.data.upload.fake.FakeBackgroundUploadSessionRenewer
import core.data.upload.fake.FakeBackgroundUploadTransport
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import core.domain.upload.model.BackgroundUploadStatus
import core.domain.upload.model.UploadProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundUploadSessionSchedulerTest {

    private val settings = MapSettings()
    private val descriptorStorage = BackgroundUploadDescriptorStorage(descriptorSettings = settings)
    private val requestHandler = FakeBackgroundUploadRequestHandler()
    private val sessionRenewer = FakeBackgroundUploadSessionRenewer(accessToken = ACCESS_TOKEN)
    private val transport = FakeBackgroundUploadTransport()
    private val fileStore = FakeBackgroundUploadFileStore()

    @Test
    fun schedule_withMultipartPreparation_writesBodyAndSendsBearerRequest() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()

        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        val (uploadId, request) = transport.createdRequests.single()
        assertEquals(PHOTO_UPLOAD_ID, uploadId)
        assertEquals("POST", request.httpMethod)
        assertEquals(UPLOAD_URL, request.url)
        assertEquals(fileStore.pathOf(PHOTO_UPLOAD_ID), request.bodyFilePath)
        assertEquals("Bearer $ACCESS_TOKEN", request.headers["Authorization"])
        assertEquals(MULTIPART_BODY.contentType, request.headers["Content-Type"])
        assertEquals(MULTIPART_BODY.contentLength.toString(), request.headers["Content-Length"])
        assertEquals(APP_HEADER_VALUE, request.headers[APP_HEADER_NAME])
        assertContentEquals(MULTIPART_BODY.bytes, fileStore.filesByUploadId[PHOTO_UPLOAD_ID])
        assertEquals(listOf(false), sessionRenewer.renewRequests)
        assertTrue(transport.tasks.single().isResumed)
    }

    @Test
    fun schedule_withoutAccessToken_sendsRequestWithoutAuthorization() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()
        sessionRenewer.accessToken = null

        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        assertFalse(transport.createdRequests.single().second.headers.containsKey("Authorization"))
    }

    @Test
    fun transfer_withProgressStuckAtSameByteCount_isCancelled() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        transport.reportProgress(PHOTO_UPLOAD_ID, bytesSent = FIRST_BYTES_SENT, bytesTotal = TOTAL_BYTES)
        advanceTimeBy(2 * STALL_TIMEOUT_MILLIS + 1)
        runCurrent()

        assertTrue(transport.tasks.single().isCancelled)
    }

    @Test
    fun transferCompleted_withParsableResponse_reportsSuccessAndCleansUp() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        transport.complete(PHOTO_UPLOAD_ID, okResponse(body = "${FakeBackgroundUploadRequestHandler.RESULT_PREFIX}$RESULT"))
        runCurrent()

        val success = assertIs<BackgroundUploadCompletion.Success<String>>(completions.single())
        assertEquals(PHOTO_UPLOAD_ID, success.uploadId)
        assertEquals(RESULT, success.result)
        assertEquals(listOf(PHOTO_UPLOAD_ID to RESULT), requestHandler.succeededUploads)
        assertNull(descriptorStorage.load(PHOTO_UPLOAD_ID))
        assertTrue(fileStore.filesByUploadId.isEmpty())
    }

    @Test
    fun transferCompleted_whenSuccessFollowUpThrows_stillReportsSuccess() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        requestHandler.successFollowUpFailure = IllegalStateException(FAILURE_MESSAGE)
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        transport.complete(PHOTO_UPLOAD_ID, okResponse(body = "${FakeBackgroundUploadRequestHandler.RESULT_PREFIX}$RESULT"))
        runCurrent()

        assertEquals(listOf(BackgroundUploadCompletion.Success(uploadId = PHOTO_UPLOAD_ID, result = RESULT)), completions)
    }

    @Test
    fun transferCompleted_withUnauthorizedResponse_renewsSessionOnceAndRetriesWithNewToken() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        sessionRenewer.renewedAccessToken = RENEWED_ACCESS_TOKEN
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_UNAUTHORIZED, body = null))
        runCurrent()

        assertEquals(listOf(false, true, false), sessionRenewer.renewRequests)
        assertEquals(2, transport.createdRequests.size)
        assertEquals("Bearer $RENEWED_ACCESS_TOKEN", transport.createdRequests.last().second.headers["Authorization"])
        assertTrue(transport.tasks.last().isResumed)
        assertTrue(completions.isEmpty())
    }

    @Test
    fun transferCompleted_withSecondUnauthorizedResponse_reportsUnauthorized() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()
        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_UNAUTHORIZED, body = null))
        runCurrent()

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_UNAUTHORIZED, body = null))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, BackgroundUploadFailureReason.UNAUTHORIZED)), completions)
        assertEquals(2, transport.createdRequests.size)
        assertNull(descriptorStorage.load(PHOTO_UPLOAD_ID))
    }

    @Test
    fun transferCompleted_withUnauthorizedResponseAndRejectedRefresh_reportsUnauthorizedWithoutRetry() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()
        sessionRenewer.isRenewable = false

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_UNAUTHORIZED, body = null))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, BackgroundUploadFailureReason.UNAUTHORIZED)), completions)
        assertEquals(1, transport.createdRequests.size)
    }

    @Test
    fun transferCompleted_withUnauthorizedResponseAfterDescriptorRemoved_reportsUnauthorized() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_UNAUTHORIZED, body = null))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, BackgroundUploadFailureReason.UNAUTHORIZED)), completions)
        assertTrue(sessionRenewer.renewRequests.isEmpty())
    }

    @Test
    fun schedule_withDirectFilePreparation_storesRemoteIdAndPutsFileWithoutAuthorization() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = directFilePreparation()

        scheduler.schedule(saveDescriptor(VIDEO_DESCRIPTOR))
        runCurrent()

        val request = transport.createdRequests.single().second
        assertEquals("PUT", request.httpMethod)
        assertEquals(PRESIGNED_URL, request.url)
        assertEquals(VIDEO_FILE_PATH, request.bodyFilePath)
        assertEquals(mapOf("Content-Type" to VIDEO_CONTENT_TYPE), request.headers)
        assertEquals(REMOTE_ID, descriptorStorage.load(VIDEO_UPLOAD_ID)?.remoteId)
        assertTrue(fileStore.filesByUploadId.isEmpty())
    }

    @Test
    fun transferCompleted_forDirectUpload_commitsRemoteIdAndReportsCommitResult() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = directFilePreparation()
        scheduler.schedule(saveDescriptor(VIDEO_DESCRIPTOR))
        runCurrent()

        transport.complete(VIDEO_UPLOAD_ID, okResponse(body = null))
        runCurrent()

        assertEquals(listOf(REMOTE_ID), requestHandler.committedRemoteIds)
        assertEquals(listOf(BackgroundUploadCompletion.Success(uploadId = VIDEO_UPLOAD_ID, result = COMMITTED_RESULT)), completions)
        assertNull(descriptorStorage.load(VIDEO_UPLOAD_ID))
    }

    @Test
    fun transferCompleted_forDirectUploadDeliveredAfterRelaunch_commitsFromStoredDescriptor() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(remoteId = REMOTE_ID))
        val relaunchedScheduler = createScheduler()
        val completions = collectCompletions(relaunchedScheduler)

        transport.complete(VIDEO_UPLOAD_ID, okResponse(body = null))
        runCurrent()

        assertEquals(listOf(REMOTE_ID), requestHandler.committedRemoteIds)
        assertEquals(listOf(BackgroundUploadCompletion.Success(uploadId = VIDEO_UPLOAD_ID, result = COMMITTED_RESULT)), completions)
    }

    @Test
    fun transferCompleted_forDirectUploadWithUnauthorizedResponse_failsWithoutRenewingSession() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(remoteId = REMOTE_ID))
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(VIDEO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_UNAUTHORIZED, body = null))
        runCurrent()

        assertEquals(listOf(failure(VIDEO_UPLOAD_ID, "http_$HTTP_UNAUTHORIZED")), completions)
        assertTrue(sessionRenewer.renewRequests.isEmpty())
    }

    @Test
    fun transferCompleted_whenCommitThrows_reportsCommitFailed() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(remoteId = REMOTE_ID))
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.commitFailure = IllegalStateException(FAILURE_MESSAGE)

        transport.complete(VIDEO_UPLOAD_ID, okResponse(body = null))
        runCurrent()

        assertEquals(listOf(failure(VIDEO_UPLOAD_ID, BackgroundUploadFailureReason.COMMIT_FAILED)), completions)
        assertNull(descriptorStorage.load(VIDEO_UPLOAD_ID))
    }

    @Test
    fun transferCompleted_withServerError_reportsHttpStatus() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_SERVER_ERROR, body = FAILURE_MESSAGE))
        runCurrent()

        val failure = assertIs<BackgroundUploadCompletion.PermanentFailure>(completions.single())
        assertEquals(PHOTO_UPLOAD_ID, failure.uploadId)
        assertEquals("http_$HTTP_SERVER_ERROR", failure.reason)
        assertNull(descriptorStorage.load(PHOTO_UPLOAD_ID))
        assertTrue(fileStore.filesByUploadId.isEmpty())
    }

    @Test
    fun transferCompleted_withRedirectStatus_reportsHttpStatus() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.Response(statusCode = HTTP_REDIRECT, body = null))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, "http_$HTTP_REDIRECT")), completions)
    }

    @Test
    fun transferCompleted_withNetworkError_reportsPlatformReason() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(PHOTO_UPLOAD_ID, BackgroundUploadTransferOutcome.NetworkError(failureReason = NETWORK_FAILURE_REASON))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, NETWORK_FAILURE_REASON)), completions)
    }

    @Test
    fun transferCompleted_withEmptyBody_reportsEmptyResponse() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(PHOTO_UPLOAD_ID, okResponse(body = ""))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, BackgroundUploadFailureReason.EMPTY_RESPONSE)), completions)
    }

    @Test
    fun transferCompleted_withUnreadableBody_reportsParseError() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(PHOTO_UPLOAD_ID, okResponse(body = UNREADABLE_BODY))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, "parse_error: Unexpected body: $UNREADABLE_BODY")), completions)
    }

    @Test
    fun schedule_withFailedPreparation_reportsReasonWithoutTransfer() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = BackgroundUploadPreparation.Failed(reason = BackgroundUploadFailureReason.BYTES_MISSING)

        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        assertEquals(listOf(failure(PHOTO_UPLOAD_ID, BackgroundUploadFailureReason.BYTES_MISSING)), completions)
        assertTrue(transport.createdRequests.isEmpty())
        assertNull(descriptorStorage.load(PHOTO_UPLOAD_ID))
    }

    @Test
    fun schedule_withAbandonedPreparation_stopsWithoutCompletion() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = BackgroundUploadPreparation.Abandoned(logDetail = ABANDON_DETAIL)

        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        assertTrue(completions.isEmpty())
        assertTrue(transport.createdRequests.isEmpty())
        assertEquals(listOf(PHOTO_DESCRIPTOR), requestHandler.preparedDescriptors)
    }

    @Test
    fun schedule_whenBodyCannotBeWritten_stopsBeforeRenewingSession() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()
        fileStore.isWritable = false

        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        assertTrue(transport.createdRequests.isEmpty())
        assertTrue(sessionRenewer.renewRequests.isEmpty())
    }

    @Test
    fun schedule_withInvalidMultipartUrl_stopsWithoutCompletion() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = multipartPreparation()
        transport.isCreatingTasks = false

        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        assertTrue(completions.isEmpty())
        assertEquals(PHOTO_DESCRIPTOR, descriptorStorage.load(PHOTO_UPLOAD_ID))
    }

    @Test
    fun schedule_withInvalidPresignedUrl_reportsInvalidPresignedUrl() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = directFilePreparation()
        transport.isCreatingTasks = false

        scheduler.schedule(saveDescriptor(VIDEO_DESCRIPTOR))
        runCurrent()

        assertEquals(listOf(failure(VIDEO_UPLOAD_ID, BackgroundUploadFailureReason.INVALID_PRESIGNED_URL)), completions)
    }

    @Test
    fun schedule_whilePreviousTransferRuns_resumesNextTaskOnlyAfterCompletion() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        scheduler.schedule(saveDescriptor(SECOND_PHOTO_DESCRIPTOR))
        runCurrent()

        val isSecondResumedBeforeFirstCompletes = transport.tasks.last().isResumed
        transport.complete(PHOTO_UPLOAD_ID, okResponse(body = "${FakeBackgroundUploadRequestHandler.RESULT_PREFIX}$RESULT"))
        runCurrent()

        assertFalse(isSecondResumedBeforeFirstCompletes)
        assertTrue(transport.tasks.last().isResumed)
    }

    @Test
    fun transfer_withoutProgressForStallWindow_isCancelledAndReleasesSlot() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        scheduler.schedule(saveDescriptor(SECOND_PHOTO_DESCRIPTOR))
        runCurrent()

        advanceTimeBy(STALL_TIMEOUT_MILLIS + 1)
        runCurrent()

        assertTrue(transport.tasks.first().isCancelled)
        assertTrue(transport.tasks.last().isResumed)
    }

    @Test
    fun transfer_withProgressInEachWindow_isNotCancelled() = runTest {
        val scheduler = createScheduler()
        requestHandler.preparation = multipartPreparation()
        scheduler.schedule(saveDescriptor(PHOTO_DESCRIPTOR))
        runCurrent()

        transport.reportProgress(PHOTO_UPLOAD_ID, bytesSent = FIRST_BYTES_SENT, bytesTotal = TOTAL_BYTES)
        advanceTimeBy(STALL_TIMEOUT_MILLIS + 1)
        runCurrent()
        transport.reportProgress(PHOTO_UPLOAD_ID, bytesSent = SECOND_BYTES_SENT, bytesTotal = TOTAL_BYTES)
        advanceTimeBy(STALL_TIMEOUT_MILLIS)
        runCurrent()

        assertFalse(transport.tasks.single().isCancelled)
    }

    @Test
    fun transferProgress_isEmittedAsUploadProgress() = runTest {
        val scheduler = createScheduler()
        val progressEvents = mutableListOf<UploadProgress>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { scheduler.progressEvents.collect(progressEvents::add) }

        transport.reportProgress(PHOTO_UPLOAD_ID, bytesSent = FIRST_BYTES_SENT, bytesTotal = TOTAL_BYTES)

        assertEquals(
            listOf(UploadProgress(uploadId = PHOTO_UPLOAD_ID, bytesSent = FIRST_BYTES_SENT, bytesTotal = TOTAL_BYTES)),
            progressEvents,
        )
    }

    @Test
    fun queryStatus_reportsInProgressOnlyWhileDescriptorIsStored() = runTest {
        val scheduler = createScheduler()
        saveDescriptor(PHOTO_DESCRIPTOR)

        assertEquals(BackgroundUploadStatus.InProgress, scheduler.queryStatus(PHOTO_UPLOAD_ID))
        assertEquals(BackgroundUploadStatus.NotFound, scheduler.queryStatus(SECOND_PHOTO_UPLOAD_ID))
    }

    @Test
    fun cancelAll_cancelsTransportTasks() = runTest {
        val scheduler = createScheduler()

        scheduler.cancelAll()

        assertEquals(1, transport.cancelAllCount)
    }

    @Test
    fun reconnect_handsOverCompletionHandlerAndDeletesOnlyOrphanedBodies() = runTest {
        val scheduler = createScheduler()
        saveDescriptor(PHOTO_DESCRIPTOR)
        fileStore.write(PHOTO_UPLOAD_ID, MULTIPART_BODY.bytes)
        fileStore.write(SECOND_PHOTO_UPLOAD_ID, MULTIPART_BODY.bytes)
        var deliveredEventCount = 0

        scheduler.reconnect(onEventsDelivered = { deliveredEventCount++ })
        transport.reconnectHandlers.single().invoke()

        assertEquals(setOf(PHOTO_UPLOAD_ID), fileStore.filesByUploadId.keys)
        assertEquals(1, deliveredEventCount)
    }

    private fun TestScope.createScheduler() = BackgroundUploadSessionScheduler(
        descriptorStorage = descriptorStorage,
        requestHandler = requestHandler,
        sessionRenewer = sessionRenewer,
        transport = transport,
        fileStore = fileStore,
        coroutineScope = backgroundScope,
        logTag = LOG_TAG,
        stallTimeoutMillis = STALL_TIMEOUT_MILLIS,
    )

    private fun TestScope.collectCompletions(
        scheduler: BackgroundUploadSessionScheduler<String>,
    ): List<BackgroundUploadCompletion<String>> {
        val completions = mutableListOf<BackgroundUploadCompletion<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { scheduler.completionEvents.collect(completions::add) }
        return completions
    }

    private fun saveDescriptor(descriptor: BackgroundUploadDescriptor): BackgroundUploadDescriptor {
        descriptorStorage.save(descriptor)
        return descriptor
    }

    private fun multipartPreparation() = BackgroundUploadPreparation.Multipart(
        url = UPLOAD_URL,
        headers = mapOf(APP_HEADER_NAME to APP_HEADER_VALUE),
        body = MULTIPART_BODY,
    )

    private fun directFilePreparation() = BackgroundUploadPreparation.DirectFile(
        url = PRESIGNED_URL,
        contentType = VIDEO_CONTENT_TYPE,
        filePath = VIDEO_FILE_PATH,
        remoteId = REMOTE_ID,
    )

    private fun okResponse(body: String?) = BackgroundUploadTransferOutcome.Response(statusCode = HTTP_OK, body = body)

    private fun failure(uploadId: String, reason: String) =
        BackgroundUploadCompletion.PermanentFailure(uploadId = uploadId, reason = reason)

    private companion object {
        const val LOG_TAG = "TestUpload"
        const val ACCESS_TOKEN = "access-token"
        const val RENEWED_ACCESS_TOKEN = "renewed-access-token"
        const val PHOTO_UPLOAD_ID = "photo-1"
        const val SECOND_PHOTO_UPLOAD_ID = "photo-2"
        const val VIDEO_UPLOAD_ID = "video-1"
        const val UPLOAD_URL = "https://api.example.com/api/uploads"
        const val PRESIGNED_URL = "https://storage.example.com/video?signature=abc"
        const val APP_HEADER_NAME = "X-App"
        const val APP_HEADER_VALUE = "sample"
        const val VIDEO_CONTENT_TYPE = "video/mp4"
        const val VIDEO_FILE_PATH = "/videos/clip.mp4"
        const val REMOTE_ID = 42
        const val RESULT = "photo-result"
        const val COMMITTED_RESULT = "committed:42"
        const val FAILURE_MESSAGE = "boom"
        const val UNREADABLE_BODY = "{}"
        const val ABANDON_DETAIL = "item_not_found"
        const val NETWORK_FAILURE_REASON = "network_NSURLErrorDomain_-1009: offline"
        const val HTTP_OK = 200
        const val HTTP_REDIRECT = 302
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_SERVER_ERROR = 500
        const val STALL_TIMEOUT_MILLIS = 1_000L
        const val FIRST_BYTES_SENT = 10L
        const val SECOND_BYTES_SENT = 20L
        const val TOTAL_BYTES = 100L
        val PHOTO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = PHOTO_UPLOAD_ID,
            mediaFileName = "photo.jpg",
            isVideo = false,
            contentType = "image/jpeg",
        )
        val SECOND_PHOTO_DESCRIPTOR = PHOTO_DESCRIPTOR.copy(uploadId = SECOND_PHOTO_UPLOAD_ID)
        val VIDEO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = VIDEO_UPLOAD_ID,
            mediaFileName = "video.mp4",
            isVideo = true,
            contentType = VIDEO_CONTENT_TYPE,
        )
        val MULTIPART_BODY = MultipartBody(bytes = byteArrayOf(1, 2, 3), boundary = "TestBoundary")
    }
}
