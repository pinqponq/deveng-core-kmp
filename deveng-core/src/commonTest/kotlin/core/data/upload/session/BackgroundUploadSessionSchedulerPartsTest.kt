package core.data.upload.session

import com.russhwolf.settings.MapSettings
import core.data.upload.BackgroundUploadDescriptorStorage
import core.data.upload.fake.FakeBackgroundUploadFileStore
import core.data.upload.fake.FakeBackgroundUploadPartHandler
import core.data.upload.fake.FakeBackgroundUploadRequestHandler
import core.data.upload.fake.FakeBackgroundUploadSessionRenewer
import core.data.upload.fake.FakeBackgroundUploadTransport
import core.data.upload.fake.WrittenPart
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import core.domain.upload.model.BackgroundUploadStatus
import core.domain.upload.model.PartedUpload
import core.domain.upload.model.StoredPart
import core.domain.upload.model.UploadProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundUploadSessionSchedulerPartsTest {

    private val settings = MapSettings()
    private val descriptorStorage = BackgroundUploadDescriptorStorage(descriptorSettings = settings)
    private val requestHandler = FakeBackgroundUploadRequestHandler().apply { preparation = partsPreparation() }
    private val partHandler = FakeBackgroundUploadPartHandler()
    private val sessionRenewer = FakeBackgroundUploadSessionRenewer(accessToken = ACCESS_TOKEN)
    private val transport = FakeBackgroundUploadTransport()
    private val fileStore = FakeBackgroundUploadFileStore()

    @Test
    fun schedule_withPartsPreparation_storesTheLayoutAndSendsTheFirstPartUnsigned() = runTest {
        val scheduler = createScheduler()

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        val storedUpload = descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload
        assertEquals(PartedUpload(remoteKey = REMOTE_KEY, filePath = VIDEO_FILE_PATH, totalBytes = TOTAL_BYTES, partSizeBytes = PART_SIZE_BYTES), storedUpload)
        assertEquals(listOf(1), partHandler.signedPartNumbers)
        assertEquals(listOf(WrittenPart(transferId = partTransferId(1), sourceFilePath = VIDEO_FILE_PATH, offset = 0, length = PART_SIZE_BYTES)), fileStore.writtenParts)
        val (transferId, request) = transport.createdRequests.single()
        assertEquals(partTransferId(1), transferId)
        assertEquals("PUT", request.httpMethod)
        assertEquals(FakeBackgroundUploadPartHandler.urlOf(partNumber = 1), request.url)
        assertEquals(emptyMap(), request.headers)
        assertEquals(fileStore.pathOf(partTransferId(1)), request.bodyFilePath)
        assertTrue(transport.tasks.single().isResumed)
        assertEquals(BackgroundUploadStatus.InProgress, scheduler.queryStatus(VIDEO_UPLOAD_ID))
    }

    @Test
    fun partStored_savesItsEntityTagAndSendsTheNextPartRange() = runTest {
        val scheduler = createScheduler()
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), storedResponse(entityTag = ENTITY_TAG_1))
        runCurrent()

        assertEquals(listOf(StoredPart(partNumber = 1, entityTag = ENTITY_TAG_1)), descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload?.storedParts)
        assertNull(fileStore.filesByUploadId[partTransferId(1)])
        assertEquals(listOf(1, 2), partHandler.signedPartNumbers)
        assertEquals(WrittenPart(transferId = partTransferId(2), sourceFilePath = VIDEO_FILE_PATH, offset = PART_SIZE_BYTES, length = PART_SIZE_BYTES), fileStore.writtenParts.last())
        assertEquals(partTransferId(2), transport.createdRequests.last().first)
        assertTrue(transport.tasks.last().isResumed)
    }

    @Test
    fun lastPartStored_completesWithEveryPartInOrderAndCleansUp() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        fileStore.filesByUploadId[partTransferId(STALE_PART_NUMBER)] = byteArrayOf(1)
        storeAllParts()

        assertEquals(listOf(STORED_PARTS), partHandler.completedParts)
        assertEquals(WrittenPart(transferId = partTransferId(3), sourceFilePath = VIDEO_FILE_PATH, offset = 2 * PART_SIZE_BYTES, length = LAST_PART_BYTES), fileStore.writtenParts.last())
        val success = assertIs<BackgroundUploadCompletion.Success<String>>(completions.single())
        assertEquals(VIDEO_UPLOAD_ID, success.uploadId)
        assertEquals("${FakeBackgroundUploadPartHandler.COMPLETED_RESULT_PREFIX}$REMOTE_KEY", success.result)
        assertNull(descriptorStorage.load(VIDEO_UPLOAD_ID))
        assertTrue(fileStore.filesByUploadId.isEmpty())
        assertTrue(partHandler.abortedUploadIds.isEmpty())
    }

    @Test
    fun partProgress_reportsTheUploadsBytesIncludingStoredParts() = runTest {
        val scheduler = createScheduler()
        val progressEvents = collectProgress(scheduler)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.reportProgress(partTransferId(1), bytesSent = 4, bytesTotal = PART_SIZE_BYTES)
        transport.complete(partTransferId(1), storedResponse(entityTag = ENTITY_TAG_1))
        runCurrent()
        transport.reportProgress(partTransferId(2), bytesSent = 3, bytesTotal = PART_SIZE_BYTES)

        assertEquals(
            listOf(
                UploadProgress(uploadId = VIDEO_UPLOAD_ID, bytesSent = 4, bytesTotal = TOTAL_BYTES),
                UploadProgress(uploadId = VIDEO_UPLOAD_ID, bytesSent = PART_SIZE_BYTES + 3, bytesTotal = TOTAL_BYTES),
            ),
            progressEvents,
        )
    }

    @Test
    fun partProgress_ofAPartStartedBeforeTheAppWasKilled_reportsNothing() = runTest {
        val scheduler = createScheduler()
        val progressEvents = collectProgress(scheduler)

        transport.reportProgress(partTransferId(2), bytesSent = 3, bytesTotal = PART_SIZE_BYTES)

        assertTrue(progressEvents.isEmpty())
    }

    @Test
    fun partAnsweredForbidden_isSignedAgainAndResent() = runTest {
        val scheduler = createScheduler()
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = HTTP_FORBIDDEN, body = null))
        runCurrent()

        assertEquals(listOf(1, 1), partHandler.signedPartNumbers)
        assertEquals(FakeBackgroundUploadPartHandler.urlOf(partNumber = 1, signature = 2), transport.createdRequests.last().second.url)
        assertEquals(partTransferId(1), transport.createdRequests.last().first)
    }

    @Test
    fun partWithNetworkErrorOrServerError_isSentAgain() = runTest {
        val scheduler = createScheduler()
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.NetworkError(failureReason = NETWORK_FAILURE_REASON))
        runCurrent()
        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = HTTP_SERVER_ERROR, body = null))
        runCurrent()

        assertEquals(listOf(1, 1, 1), partHandler.signedPartNumbers)
        assertEquals(3, transport.createdRequests.size)
    }

    @Test
    fun partFailingEveryAttempt_pausesWithoutFailingAndResumesFromThatPart() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()
        transport.complete(partTransferId(1), storedResponse(entityTag = ENTITY_TAG_1))
        runCurrent()

        repeat(MAX_PART_ATTEMPTS) {
            transport.complete(partTransferId(2), BackgroundUploadTransferOutcome.NetworkError(failureReason = NETWORK_FAILURE_REASON))
            runCurrent()
        }

        assertTrue(completions.isEmpty())
        assertEquals(listOf(1, 2, 2, 2), partHandler.signedPartNumbers)
        assertNull(fileStore.filesByUploadId[partTransferId(2)])
        assertEquals(listOf(StoredPart(partNumber = 1, entityTag = ENTITY_TAG_1)), descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload?.storedParts)

        scheduler.resumeInterruptedUploads()
        runCurrent()

        assertEquals(listOf(1, 2, 2, 2, 2), partHandler.signedPartNumbers)
        assertEquals(partTransferId(2), transport.createdRequests.last().first)
    }

    @Test
    fun partThatCannotBeSigned_pausesAndKeepsTheUpload() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        partHandler.signFailure = IllegalStateException(FAILURE_MESSAGE)

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        assertTrue(completions.isEmpty())
        assertTrue(transport.createdRequests.isEmpty())
        assertEquals(BackgroundUploadStatus.InProgress, scheduler.queryStatus(VIDEO_UPLOAD_ID))

        partHandler.signFailure = null
        scheduler.resumeInterruptedUploads()
        runCurrent()

        assertEquals(partTransferId(1), transport.createdRequests.single().first)
    }

    @Test
    fun partThatCannotBeSignedAgainAndAgain_isSignedOnEveryResume() = runTest {
        val scheduler = createScheduler()
        partHandler.signFailure = IllegalStateException(FAILURE_MESSAGE)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        repeat(MAX_PART_ATTEMPTS) {
            scheduler.resumeInterruptedUploads()
            runCurrent()
        }

        assertEquals(List(MAX_PART_ATTEMPTS + 1) { 1 }, partHandler.signedPartNumbers)
    }

    @Test
    fun resumeInterruptedUploads_continuesOnlyPartedUploadsFromTheirFirstPendingPart() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(partedUpload = partedUploadWith(StoredPart(partNumber = 1, entityTag = ENTITY_TAG_1))))
        descriptorStorage.save(PHOTO_DESCRIPTOR)
        val scheduler = createScheduler()

        scheduler.resumeInterruptedUploads()
        runCurrent()

        assertTrue(requestHandler.preparedDescriptors.isEmpty())
        assertEquals(listOf(2), partHandler.signedPartNumbers)
        assertEquals(partTransferId(2), transport.createdRequests.single().first)
    }

    @Test
    fun schedule_ofAnUploadWithStoredParts_continuesWithoutOpeningASecondUpload() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(partedUpload = partedUploadWith(StoredPart(partNumber = 1, entityTag = ENTITY_TAG_1))))
        val scheduler = createScheduler()

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        assertTrue(requestHandler.preparedDescriptors.isEmpty())
        assertEquals(listOf(2), partHandler.signedPartNumbers)
    }

    @Test
    fun secondCompletionOfAStoredPart_isIgnored() = runTest {
        val scheduler = createScheduler()
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()
        transport.complete(partTransferId(1), storedResponse(entityTag = ENTITY_TAG_1))
        runCurrent()

        transport.complete(partTransferId(1), storedResponse(entityTag = "\"late\""))
        runCurrent()

        assertEquals(listOf(1, 2), partHandler.signedPartNumbers)
        assertEquals(listOf(StoredPart(partNumber = 1, entityTag = ENTITY_TAG_1)), descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload?.storedParts)
    }

    @Test
    fun partCompletion_ofAnUploadThatIsGone_isIgnored() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)

        transport.complete(partTransferId(1), storedResponse(entityTag = ENTITY_TAG_1))
        runCurrent()

        assertTrue(completions.isEmpty())
        assertTrue(partHandler.signedPartNumbers.isEmpty())
    }

    @Test
    fun partRejectedByStorage_failsAndAbortsTheUpload() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = HTTP_NOT_FOUND, body = "NoSuchUpload"))
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.httpStatus(HTTP_NOT_FOUND))), completions)
        assertEquals(listOf(VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
        assertNull(descriptorStorage.load(VIDEO_UPLOAD_ID))
        assertTrue(fileStore.filesByUploadId.isEmpty())
    }

    @Test
    fun partRedirected_failsAsNotStored() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = HTTP_REDIRECT, body = null, entityTag = ENTITY_TAG_1))
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.httpStatus(HTTP_REDIRECT))), completions)
    }

    @Test
    fun partAnsweredJustOutsideTheSuccessRange_failsAsNotStored() = runTest {
        listOf(HTTP_BELOW_SUCCESS, HTTP_ABOVE_SUCCESS).forEach { statusCode ->
            val scheduler = createScheduler()
            val completions = collectCompletions(scheduler)
            scheduler.schedule(VIDEO_DESCRIPTOR)
            runCurrent()

            transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = statusCode, body = null, entityTag = ENTITY_TAG_1))
            runCurrent()

            assertEquals(listOf(failure(BackgroundUploadFailureReason.httpStatus(statusCode))), completions)
        }
    }

    @Test
    fun partStoredWithLastSuccessStatus_counts() = runTest {
        val scheduler = createScheduler()
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = HTTP_LAST_SUCCESS, body = null, entityTag = ENTITY_TAG_1))
        runCurrent()

        assertEquals(listOf(1, 2), partHandler.signedPartNumbers)
    }

    @Test
    fun partStoredWithoutOrWithBlankEntityTag_fails() = runTest {
        listOf(null, " ").forEach { entityTag ->
            val scheduler = createScheduler()
            val completions = collectCompletions(scheduler)
            scheduler.schedule(VIDEO_DESCRIPTOR)
            runCurrent()

            transport.complete(partTransferId(1), storedResponse(entityTag = entityTag))
            runCurrent()

            assertEquals(listOf(failure(BackgroundUploadFailureReason.PART_ENTITY_TAG_MISSING)), completions, "entityTag=$entityTag")
        }
    }

    @Test
    fun completeFailure_failsAsCommitFailedAndAborts() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        partHandler.completeFailure = IllegalStateException(FAILURE_MESSAGE)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        storeAllParts()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.COMMIT_FAILED)), completions)
        assertEquals(listOf(VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
    }

    @Test
    fun abortFailure_isOnlyLogged() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        partHandler.abortFailure = IllegalStateException(FAILURE_MESSAGE)
        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        transport.complete(partTransferId(1), BackgroundUploadTransferOutcome.Response(statusCode = HTTP_NOT_FOUND, body = null))
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.httpStatus(HTTP_NOT_FOUND))), completions)
        assertEquals(listOf(VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
    }

    @Test
    fun schedule_withoutPartHandler_failsWithoutAbort() = runTest {
        val scheduler = createScheduler(withPartHandler = false)
        val completions = collectCompletions(scheduler)

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.PART_HANDLER_MISSING)), completions)
        assertTrue(partHandler.abortedUploadIds.isEmpty())
        assertNull(descriptorStorage.load(VIDEO_UPLOAD_ID))
    }

    @Test
    fun schedule_withInvalidPartLayout_failsWithoutStoringIt() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        requestHandler.preparation = partsPreparation(totalBytes = 0)

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.INVALID_PART_LAYOUT)), completions)
        assertNull(descriptorStorage.load(VIDEO_UPLOAD_ID))
        assertTrue(partHandler.signedPartNumbers.isEmpty())
    }

    @Test
    fun partFileThatCannotBeWritten_failsAndAborts() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        fileStore.isWritable = false

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.WRITE_PART_FILE_FAILED)), completions)
        assertEquals(listOf(VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
        assertTrue(transport.createdRequests.isEmpty())
    }

    @Test
    fun partUrlTheTransportRejects_failsAndRemovesThePartFile() = runTest {
        val scheduler = createScheduler()
        val completions = collectCompletions(scheduler)
        transport.isCreatingTasks = false

        scheduler.schedule(VIDEO_DESCRIPTOR)
        runCurrent()

        assertEquals(listOf(failure(BackgroundUploadFailureReason.INVALID_PRESIGNED_URL)), completions)
        assertTrue(fileStore.filesByUploadId.isEmpty())
    }

    @Test
    fun reconnect_keepsPartFilesOfStoredUploadsAndDeletesOrphans() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(partedUpload = partedUploadWith()))
        fileStore.filesByUploadId[partTransferId(2)] = byteArrayOf(1)
        fileStore.filesByUploadId[BackgroundUploadTransferId(uploadId = ORPHAN_UPLOAD_ID, partNumber = 1).value] = byteArrayOf(2)
        val scheduler = createScheduler()

        scheduler.reconnect(onEventsDelivered = {})

        assertEquals(setOf(partTransferId(2)), fileStore.filesByUploadId.keys)
    }

    private fun TestScope.storeAllParts() {
        STORED_PARTS.forEach { part ->
            transport.complete(partTransferId(part.partNumber), storedResponse(entityTag = part.entityTag))
            runCurrent()
        }
    }

    private fun TestScope.createScheduler(withPartHandler: Boolean = true) = BackgroundUploadSessionScheduler(
        descriptorStorage = descriptorStorage,
        requestHandler = requestHandler,
        sessionRenewer = sessionRenewer,
        transport = transport,
        fileStore = fileStore,
        coroutineScope = backgroundScope,
        logTag = LOG_TAG,
        stallTimeoutMillis = STALL_TIMEOUT_MILLIS,
        partHandler = if (withPartHandler) partHandler else null,
        maxPartAttempts = MAX_PART_ATTEMPTS,
    )

    private fun TestScope.collectCompletions(
        scheduler: BackgroundUploadSessionScheduler<String>,
    ): List<BackgroundUploadCompletion<String>> {
        val completions = mutableListOf<BackgroundUploadCompletion<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { scheduler.completionEvents.collect(completions::add) }
        return completions
    }

    private fun TestScope.collectProgress(scheduler: BackgroundUploadSessionScheduler<String>): List<UploadProgress> {
        val progressEvents = mutableListOf<UploadProgress>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { scheduler.progressEvents.collect(progressEvents::add) }
        return progressEvents
    }

    private fun partsPreparation(totalBytes: Long = TOTAL_BYTES) = BackgroundUploadPreparation.DirectFileInParts(
        filePath = VIDEO_FILE_PATH,
        totalBytes = totalBytes,
        partSizeBytes = PART_SIZE_BYTES,
        remoteKey = REMOTE_KEY,
    )

    private fun partedUploadWith(vararg storedParts: StoredPart) = PartedUpload(
        remoteKey = REMOTE_KEY,
        filePath = VIDEO_FILE_PATH,
        totalBytes = TOTAL_BYTES,
        partSizeBytes = PART_SIZE_BYTES,
        storedParts = storedParts.toList(),
    )

    private fun partTransferId(partNumber: Int) = BackgroundUploadTransferId(uploadId = VIDEO_UPLOAD_ID, partNumber = partNumber).value

    private fun storedResponse(entityTag: String?) =
        BackgroundUploadTransferOutcome.Response(statusCode = HTTP_OK, body = null, entityTag = entityTag)

    private fun failure(reason: String) = BackgroundUploadCompletion.PermanentFailure(uploadId = VIDEO_UPLOAD_ID, reason = reason)

    private companion object {
        const val LOG_TAG = "TestUpload"
        const val ACCESS_TOKEN = "access-token"
        const val VIDEO_UPLOAD_ID = "video-1"
        const val ORPHAN_UPLOAD_ID = "video-gone"
        const val VIDEO_FILE_PATH = "/videos/clip.mp4"
        const val REMOTE_KEY = "upload-session-7"
        const val PART_SIZE_BYTES = 10L
        const val LAST_PART_BYTES = 5L
        const val TOTAL_BYTES = 2 * PART_SIZE_BYTES + LAST_PART_BYTES
        const val MAX_PART_ATTEMPTS = 3
        const val STALL_TIMEOUT_MILLIS = 1_000L
        const val ENTITY_TAG_1 = "\"etag-1\""
        const val FAILURE_MESSAGE = "boom"
        const val NETWORK_FAILURE_REASON = "network_NSURLErrorDomain_-1005: connection lost"
        const val STALE_PART_NUMBER = 9
        const val HTTP_BELOW_SUCCESS = 199
        const val HTTP_OK = 200
        const val HTTP_LAST_SUCCESS = 299
        const val HTTP_ABOVE_SUCCESS = 300
        const val HTTP_REDIRECT = 302
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_SERVER_ERROR = 500
        val STORED_PARTS = listOf(
            StoredPart(partNumber = 1, entityTag = ENTITY_TAG_1),
            StoredPart(partNumber = 2, entityTag = "\"etag-2\""),
            StoredPart(partNumber = 3, entityTag = "\"etag-3\""),
        )
        val VIDEO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = VIDEO_UPLOAD_ID,
            mediaFileName = "clip.mp4",
            isVideo = true,
            contentType = "video/mp4",
        )
        val PHOTO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = "photo-1",
            mediaFileName = "photo.jpg",
            isVideo = false,
            contentType = "image/jpeg",
        )
    }
}
