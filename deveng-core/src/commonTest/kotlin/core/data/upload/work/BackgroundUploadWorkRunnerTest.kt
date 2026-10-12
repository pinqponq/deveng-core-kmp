package core.data.upload.work

import com.russhwolf.settings.MapSettings
import core.data.upload.BackgroundUploadDescriptorStorage
import core.data.upload.fake.FakeBackgroundUploadSessionRenewer
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadFailureReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackgroundUploadWorkRunnerTest {

    private val descriptorStorage = BackgroundUploadDescriptorStorage(descriptorSettings = MapSettings())
    private val sessionRenewer = FakeBackgroundUploadSessionRenewer()
    private val completions = mutableListOf<BackgroundUploadCompletion<String>>()
    private val runner = BackgroundUploadWorkRunner<String>(
        descriptorStorage = descriptorStorage,
        sessionRenewer = sessionRenewer,
        onCompletion = completions::add,
        logTag = LOG_TAG,
        maxRunAttempts = MAX_RUN_ATTEMPTS,
    )
    private var foregroundPromotionCount = 0

    @Test
    fun run_withoutUploadId_failsWithoutCallingWork() = runTest {
        var isWorkCalled = false

        val result = runWork(uploadId = null) { _, _ ->
            isWorkCalled = true
            BackgroundUploadAttempt.NothingToUpload
        }

        assertEquals(BackgroundUploadWorkResult.Failure, result)
        assertEquals(false, isWorkCalled)
    }

    @Test
    fun run_afterMaxAttempts_reportsMaxAttemptsExceededAndDropsDescriptor() = runTest {
        descriptorStorage.save(DESCRIPTOR)

        val result = runWork(runAttemptCount = MAX_RUN_ATTEMPTS) { _, _ -> BackgroundUploadAttempt.Uploaded(RESULT) }

        assertEquals(BackgroundUploadWorkResult.Failure, result)
        assertEquals<List<BackgroundUploadCompletion<String>>>(listOf(failure(BackgroundUploadFailureReason.MAX_ATTEMPTS_EXCEEDED)), completions)
        assertNull(descriptorStorage.load(UPLOAD_ID))
    }

    @Test
    fun run_withoutStoredDescriptor_succeedsWithoutCallingWork() = runTest {
        var isWorkCalled = false

        val result = runWork { _, _ ->
            isWorkCalled = true
            BackgroundUploadAttempt.NothingToUpload
        }

        assertEquals(BackgroundUploadWorkResult.Success, result)
        assertEquals(false, isWorkCalled)
        assertTrue(completions.isEmpty())
    }

    @Test
    fun run_whenWorkUploads_reportsResultAndDropsDescriptor() = runTest {
        descriptorStorage.save(DESCRIPTOR)

        val result = runWork(runAttemptCount = MAX_RUN_ATTEMPTS - 1) { descriptor, _ ->
            assertEquals(DESCRIPTOR, descriptor)
            BackgroundUploadAttempt.Uploaded(RESULT)
        }

        assertEquals(BackgroundUploadWorkResult.Success, result)
        assertEquals<List<BackgroundUploadCompletion<String>>>(listOf(BackgroundUploadCompletion.Success(uploadId = UPLOAD_ID, result = RESULT)), completions)
        assertNull(descriptorStorage.load(UPLOAD_ID))
    }

    @Test
    fun run_whenNothingLeftToUpload_succeedsWithoutCompletion() = runTest {
        descriptorStorage.save(DESCRIPTOR)

        val result = runWork { _, _ -> BackgroundUploadAttempt.NothingToUpload }

        assertEquals(BackgroundUploadWorkResult.Success, result)
        assertTrue(completions.isEmpty())
        assertNull(descriptorStorage.load(UPLOAD_ID))
    }

    @Test
    fun run_whenWorkFailsPermanently_reportsReasonAndFails() = runTest {
        descriptorStorage.save(DESCRIPTOR)

        val result = runWork { _, _ -> BackgroundUploadAttempt.Failed(BackgroundUploadFailureReason.BYTES_MISSING) }

        assertEquals(BackgroundUploadWorkResult.Failure, result)
        assertEquals<List<BackgroundUploadCompletion<String>>>(listOf(failure(BackgroundUploadFailureReason.BYTES_MISSING)), completions)
        assertNull(descriptorStorage.load(UPLOAD_ID))
    }

    @Test
    fun run_whenWorkThrows_retriesAndKeepsDescriptor() = runTest {
        descriptorStorage.save(DESCRIPTOR)

        val result = runWork { _, _ -> throw IllegalStateException(FAILURE_MESSAGE) }

        assertEquals(BackgroundUploadWorkResult.Retry, result)
        assertTrue(completions.isEmpty())
        assertEquals(DESCRIPTOR, descriptorStorage.load(UPLOAD_ID))
    }

    @Test
    fun run_whenCancelled_rethrowsCancellation() = runTest {
        descriptorStorage.save(DESCRIPTOR)

        assertFailsWith<CancellationException> {
            runWork { _, _ -> throw CancellationException(FAILURE_MESSAGE) }
        }
    }

    @Test
    fun sessionStart_renewsExpiringSessionThenPromotesToForeground() = runTest {
        descriptorStorage.save(DESCRIPTOR)
        val renewRequestsAtPromotion = mutableListOf<Boolean>()

        runWork(
            promoteToForeground = {
                renewRequestsAtPromotion += sessionRenewer.renewRequests
                foregroundPromotionCount++
            },
        ) { _, session ->
            session.start()
            BackgroundUploadAttempt.Uploaded(RESULT)
        }

        assertEquals(listOf(false), renewRequestsAtPromotion)
        assertEquals(1, foregroundPromotionCount)
    }

    private suspend fun runWork(
        uploadId: String? = UPLOAD_ID,
        runAttemptCount: Int = 0,
        promoteToForeground: suspend () -> Unit = { foregroundPromotionCount++ },
        work: BackgroundUploadWork<String>,
    ): BackgroundUploadWorkResult = runner.run(
        uploadId = uploadId,
        runAttemptCount = runAttemptCount,
        work = work,
        promoteToForeground = promoteToForeground,
    )

    private fun failure(reason: String) = BackgroundUploadCompletion.PermanentFailure(uploadId = UPLOAD_ID, reason = reason)

    private companion object {
        const val LOG_TAG = "TestUploadWorker"
        const val UPLOAD_ID = "upload-1"
        const val RESULT = "sent"
        const val FAILURE_MESSAGE = "boom"
        const val MAX_RUN_ATTEMPTS = 2
        val DESCRIPTOR = BackgroundUploadDescriptor(
            uploadId = UPLOAD_ID,
            mediaFileName = "photo.jpg",
            isVideo = false,
            contentType = "image/jpeg",
        )
    }
}
