package core.data.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import core.domain.upload.BackgroundUploadScheduler
import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val BACKOFF_DELAY_SECONDS = 30L
private const val COMPLETION_BUFFER_CAPACITY = 64

/**
 * Queues uploads as WorkManager work on one unique-work lane ([queueName]), so they run one after
 * another and survive the app being killed. [workerClass] is a [BackgroundUploadWorker] subclass;
 * keep its class name, [queueName] and [uploadIdTagPrefix] stable across releases, WorkManager
 * stores them with work queued by an earlier install.
 */
class WorkManagerBackgroundUploadScheduler<T>(
    private val context: Context,
    private val queueName: String,
    private val uploadIdTagPrefix: String,
    private val workerClass: Class<out ListenableWorker>,
    private val logTag: String,
) : BackgroundUploadScheduler<T> {

    private val completionMutableFlow = MutableSharedFlow<BackgroundUploadCompletion<T>>(
        extraBufferCapacity = COMPLETION_BUFFER_CAPACITY,
    )

    override val completionEvents: Flow<BackgroundUploadCompletion<T>> = completionMutableFlow.asSharedFlow()

    override suspend fun schedule(descriptor: BackgroundUploadDescriptor) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequest.Builder(workerClass)
            .setInputData(workDataOf(BackgroundUploadWorker.KEY_UPLOAD_ID to descriptor.uploadId))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .addTag(uploadIdTag(descriptor.uploadId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(queueName, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            .enqueue()
        Logger.i { "[$logTag] Enqueued uploadId=${descriptor.uploadId} tag=${uploadIdTag(descriptor.uploadId)}" }
    }

    override fun cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork(queueName)
        Logger.i { "[$logTag] Cancelled queue" }
    }

    override suspend fun queryStatus(uploadId: String): BackgroundUploadStatus {
        val workInfo = WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(uploadIdTag(uploadId))
            .first()
            .firstOrNull()
            ?: return BackgroundUploadStatus.NotFound
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> BackgroundUploadStatus.Completed
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> BackgroundUploadStatus.Failed
            else -> BackgroundUploadStatus.InProgress
        }
    }

    fun reportCompletion(completion: BackgroundUploadCompletion<T>) {
        val isEmitted = completionMutableFlow.tryEmit(completion)
        Logger.i { "[$logTag] reportCompletion emitted=$isEmitted completion=$completion subscribers=${completionMutableFlow.subscriptionCount.value}" }
    }

    private fun uploadIdTag(uploadId: String): String = "$uploadIdTagPrefix$uploadId"
}
