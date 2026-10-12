package core.data.upload.work

import co.touchlab.kermit.Logger
import core.data.upload.BackgroundUploadDescriptorStorage
import core.domain.upload.BackgroundUploadSessionRenewer
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadFailureReason
import kotlin.coroutines.cancellation.CancellationException

const val DEFAULT_MAX_UPLOAD_RUN_ATTEMPTS = 2

enum class BackgroundUploadWorkResult {
    Success,
    Failure,
    Retry,
}

/**
 * One run of a queued background upload: gives up after [maxRunAttempts], skips uploads whose
 * descriptor is gone, lets the app's [BackgroundUploadWork] send the file and reports the outcome
 * through [onCompletion].
 */
class BackgroundUploadWorkRunner<T>(
    private val descriptorStorage: BackgroundUploadDescriptorStorage,
    private val sessionRenewer: BackgroundUploadSessionRenewer,
    private val onCompletion: (BackgroundUploadCompletion<T>) -> Unit,
    private val logTag: String,
    private val maxRunAttempts: Int = DEFAULT_MAX_UPLOAD_RUN_ATTEMPTS,
) {

    suspend fun run(
        uploadId: String?,
        runAttemptCount: Int,
        work: BackgroundUploadWork<T>,
        promoteToForeground: suspend () -> Unit,
    ): BackgroundUploadWorkResult {
        Logger.i { "[$logTag] doWork started uploadId=$uploadId attempt=$runAttemptCount" }
        if (uploadId == null) {
            Logger.e { "[$logTag] Missing uploadId — failing permanently" }
            return BackgroundUploadWorkResult.Failure
        }

        if (runAttemptCount >= maxRunAttempts) {
            Logger.e { "[$logTag] Exceeded max attempts uploadId=$uploadId attempt=$runAttemptCount — failing permanently" }
            descriptorStorage.remove(uploadId)
            onCompletion(BackgroundUploadCompletion.PermanentFailure(uploadId = uploadId, reason = BackgroundUploadFailureReason.MAX_ATTEMPTS_EXCEEDED))
            return BackgroundUploadWorkResult.Failure
        }

        val descriptor = descriptorStorage.load(uploadId)
        if (descriptor == null) {
            Logger.w { "[$logTag] No descriptor found uploadId=$uploadId — treating as done" }
            return BackgroundUploadWorkResult.Success
        }

        val session = BackgroundUploadWorkSession {
            sessionRenewer.renewSession(isForced = false)
            promoteToForeground()
        }
        val attempt = try {
            work.upload(descriptor = descriptor, session = session)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Logger.e(exception) { "[$logTag] Send failed uploadId=$uploadId" }
            return BackgroundUploadWorkResult.Retry
        }
        return finishAttempt(uploadId = uploadId, attempt = attempt)
    }

    private fun finishAttempt(uploadId: String, attempt: BackgroundUploadAttempt<T>): BackgroundUploadWorkResult {
        descriptorStorage.remove(uploadId)
        return when (attempt) {
            is BackgroundUploadAttempt.Uploaded -> {
                onCompletion(BackgroundUploadCompletion.Success(uploadId = uploadId, result = attempt.result))
                Logger.i { "[$logTag] Success uploadId=$uploadId" }
                BackgroundUploadWorkResult.Success
            }

            BackgroundUploadAttempt.NothingToUpload -> {
                Logger.w { "[$logTag] Nothing left to upload uploadId=$uploadId — cleaning up" }
                BackgroundUploadWorkResult.Success
            }

            is BackgroundUploadAttempt.Failed -> {
                onCompletion(BackgroundUploadCompletion.PermanentFailure(uploadId = uploadId, reason = attempt.reason))
                Logger.e { "[$logTag] Failed permanently uploadId=$uploadId reason=${attempt.reason}" }
                BackgroundUploadWorkResult.Failure
            }
        }
    }
}
