package core.data.upload.work

import com.russhwolf.settings.MapSettings
import core.data.upload.BackgroundUploadDescriptorStorage
import core.data.upload.fake.FakeBackgroundUploadPartHandler
import core.data.upload.session.BackgroundUploadPreparation
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import core.domain.upload.model.PartByteRange
import core.domain.upload.model.PartedUpload
import core.domain.upload.model.StoredPart
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackgroundPartedUploadLoopTest {

    private val descriptorStorage = BackgroundUploadDescriptorStorage(descriptorSettings = MapSettings())
    private val partHandler = FakeBackgroundUploadPartHandler()
    private val sentParts = mutableListOf<SentPart>()
    private val answersByUrl = mutableMapOf<String, BackgroundUploadPartAnswer>()
    private var sendFailure: Exception? = null
    private var openCount = 0
    private val partSender = BackgroundUploadPartSender { url, filePath, byteRange ->
        sentParts += SentPart(url = url, filePath = filePath, byteRange = byteRange)
        sendFailure?.let { failure -> throw failure }
        answersByUrl[url] ?: BackgroundUploadPartAnswer(statusCode = HTTP_OK, entityTag = entityTagOf(byteRange))
    }
    private val loop = BackgroundPartedUploadLoop(
        descriptorStorage = descriptorStorage,
        partHandler = partHandler,
        partSender = partSender,
        logTag = LOG_TAG,
    )

    @Test
    fun upload_withoutStoredParts_opensSendsEveryPartAndCompletes() = runTest {
        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertEquals(BackgroundUploadAttempt.Uploaded(result = "${FakeBackgroundUploadPartHandler.COMPLETED_RESULT_PREFIX}$REMOTE_KEY"), attempt)
        assertEquals(1, openCount)
        assertEquals(
            listOf(
                SentPart(url = FakeBackgroundUploadPartHandler.urlOf(1), filePath = VIDEO_FILE_PATH, byteRange = PartByteRange(offset = 0, length = PART_SIZE_BYTES)),
                SentPart(url = FakeBackgroundUploadPartHandler.urlOf(2), filePath = VIDEO_FILE_PATH, byteRange = PartByteRange(offset = PART_SIZE_BYTES, length = PART_SIZE_BYTES)),
                SentPart(url = FakeBackgroundUploadPartHandler.urlOf(3), filePath = VIDEO_FILE_PATH, byteRange = PartByteRange(offset = 2 * PART_SIZE_BYTES, length = LAST_PART_BYTES)),
            ),
            sentParts,
        )
        assertEquals(listOf(listOf(storedPart(1, 0), storedPart(2, PART_SIZE_BYTES), storedPart(3, 2 * PART_SIZE_BYTES))), partHandler.completedParts)
    }

    @Test
    fun upload_withStoredParts_continuesFromTheFirstPendingPartWithoutOpening() = runTest {
        descriptorStorage.save(VIDEO_DESCRIPTOR.copy(partedUpload = partedUpload(storedPart(1, 0))))

        loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertEquals(0, openCount)
        assertEquals(listOf(2, 3), partHandler.signedPartNumbers)
    }

    @Test
    fun upload_whenSendingThrows_keepsStoredPartsForTheNextRun() = runTest {
        val failingSender = BackgroundPartedUploadLoop(
            descriptorStorage = descriptorStorage,
            partHandler = partHandler,
            partSender = BackgroundUploadPartSender { url, filePath, byteRange ->
                if (byteRange.offset > 0) throw IllegalStateException(FAILURE_MESSAGE)
                partSender.sendPart(url, filePath, byteRange)
            },
            logTag = LOG_TAG,
        )

        assertFailsWith<IllegalStateException> { failingSender.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts) }

        assertEquals(listOf(storedPart(1, 0)), descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload?.storedParts)
        assertTrue(partHandler.abortedUploadIds.isEmpty())
    }

    @Test
    fun upload_whenSignatureExpired_signsOnceMoreAndStoresThePart() = runTest {
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(partNumber = 1, signature = 1)] = BackgroundUploadPartAnswer(statusCode = HTTP_FORBIDDEN, entityTag = null)

        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertTrue(attempt is BackgroundUploadAttempt.Uploaded)
        assertEquals(listOf(1, 1, 2, 3), partHandler.signedPartNumbers)
        assertEquals(FakeBackgroundUploadPartHandler.urlOf(partNumber = 1, signature = 2), sentParts[1].url)
    }

    @Test
    fun upload_whenFreshSignatureIsRejectedToo_failsAndAborts() = runTest {
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(partNumber = 1, signature = 1)] = BackgroundUploadPartAnswer(statusCode = HTTP_FORBIDDEN, entityTag = null)
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(partNumber = 1, signature = 2)] = BackgroundUploadPartAnswer(statusCode = HTTP_FORBIDDEN, entityTag = null)

        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertEquals(BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.httpStatus(HTTP_FORBIDDEN)), attempt)
        assertEquals(listOf(VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
    }

    @Test
    fun upload_whenStorageRejectsAPart_failsAndAborts() = runTest {
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(2)] = BackgroundUploadPartAnswer(statusCode = HTTP_NOT_FOUND, entityTag = null)

        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertEquals(BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.httpStatus(HTTP_NOT_FOUND)), attempt)
        assertEquals(listOf(VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
        assertEquals(listOf(1, 2), partHandler.signedPartNumbers)
    }

    @Test
    fun upload_withRedirectOrLastSuccessStatus_tellsStoredFromNotStored() = runTest {
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(1)] = BackgroundUploadPartAnswer(statusCode = HTTP_LAST_SUCCESS, entityTag = ENTITY_TAG)
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(2)] = BackgroundUploadPartAnswer(statusCode = HTTP_REDIRECT, entityTag = ENTITY_TAG)

        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertEquals(BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.httpStatus(HTTP_REDIRECT)), attempt)
        assertEquals(listOf(StoredPart(partNumber = 1, entityTag = ENTITY_TAG)), descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload?.storedParts)
    }

    @Test
    fun upload_whenAPartHasNoOrABlankEntityTag_failsAndAborts() = runTest {
        listOf(null, "").forEachIndexed { index, entityTag ->
            descriptorStorage.remove(VIDEO_UPLOAD_ID)
            val partUrl = FakeBackgroundUploadPartHandler.urlOf(partNumber = 1, signature = index + 1)
            answersByUrl[partUrl] = BackgroundUploadPartAnswer(statusCode = HTTP_OK, entityTag = entityTag)

            val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

            assertEquals(BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.PART_ENTITY_TAG_MISSING), attempt, "entityTag=$entityTag")
        }
        assertEquals(listOf(VIDEO_UPLOAD_ID, VIDEO_UPLOAD_ID), partHandler.abortedUploadIds)
    }

    @Test
    fun upload_whenAbortThrows_stillReportsTheFailure() = runTest {
        partHandler.abortFailure = IllegalStateException(FAILURE_MESSAGE)
        answersByUrl[FakeBackgroundUploadPartHandler.urlOf(1)] = BackgroundUploadPartAnswer(statusCode = HTTP_NOT_FOUND, entityTag = null)

        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts)

        assertEquals(BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.httpStatus(HTTP_NOT_FOUND)), attempt)
    }

    @Test
    fun upload_withInvalidPartLayout_failsWithoutStoringOrSending() = runTest {
        val attempt = loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = { openParts().copy(partSizeBytes = 0) })

        assertEquals(BackgroundUploadAttempt.Failed(reason = BackgroundUploadFailureReason.INVALID_PART_LAYOUT), attempt)
        assertEquals(null, descriptorStorage.load(VIDEO_UPLOAD_ID))
        assertTrue(sentParts.isEmpty())
    }

    @Test
    fun upload_whenCompletingThrows_keepsEveryStoredPartForTheNextRun() = runTest {
        partHandler.completeFailure = IllegalStateException(FAILURE_MESSAGE)

        assertFailsWith<IllegalStateException> { loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts) }

        assertEquals(3, descriptorStorage.load(VIDEO_UPLOAD_ID)?.partedUpload?.storedParts?.size)
        partHandler.completeFailure = null
        assertTrue(loop.upload(descriptor = VIDEO_DESCRIPTOR, openParts = ::openParts) is BackgroundUploadAttempt.Uploaded)
        assertEquals(3, sentParts.size)
    }

    private fun openParts(): BackgroundUploadPreparation.DirectFileInParts {
        openCount++
        return BackgroundUploadPreparation.DirectFileInParts(
            filePath = VIDEO_FILE_PATH,
            totalBytes = TOTAL_BYTES,
            partSizeBytes = PART_SIZE_BYTES,
            remoteKey = REMOTE_KEY,
        )
    }

    private fun partedUpload(vararg storedParts: StoredPart) = PartedUpload(
        remoteKey = REMOTE_KEY,
        filePath = VIDEO_FILE_PATH,
        totalBytes = TOTAL_BYTES,
        partSizeBytes = PART_SIZE_BYTES,
        storedParts = storedParts.toList(),
    )

    private fun storedPart(partNumber: Int, offset: Long) =
        StoredPart(partNumber = partNumber, entityTag = entityTagOf(PartByteRange(offset = offset, length = 0)))

    private fun entityTagOf(byteRange: PartByteRange) = "\"etag-${byteRange.offset}\""

    private data class SentPart(
        val url: String,
        val filePath: String,
        val byteRange: PartByteRange,
    )

    private companion object {
        const val LOG_TAG = "TestPartedUpload"
        const val VIDEO_UPLOAD_ID = "video-1"
        const val VIDEO_FILE_PATH = "/videos/clip.mp4"
        const val REMOTE_KEY = "upload-session-7"
        const val PART_SIZE_BYTES = 10L
        const val LAST_PART_BYTES = 5L
        const val TOTAL_BYTES = 2 * PART_SIZE_BYTES + LAST_PART_BYTES
        const val ENTITY_TAG = "\"etag\""
        const val FAILURE_MESSAGE = "boom"
        const val HTTP_OK = 200
        const val HTTP_LAST_SUCCESS = 299
        const val HTTP_REDIRECT = 302
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        val VIDEO_DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = VIDEO_UPLOAD_ID,
            mediaFileName = "clip.mp4",
            isVideo = true,
            contentType = "video/mp4",
        )
    }
}
