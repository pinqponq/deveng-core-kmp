package core.data.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import core.data.upload.work.BackgroundUploadWork
import core.data.upload.work.BackgroundUploadWorkResult
import core.data.upload.work.BackgroundUploadWorkRunner
import core.data.upload.work.DEFAULT_MAX_UPLOAD_RUN_ATTEMPTS
import core.domain.upload.BackgroundUploadSessionRenewer

/**
 * Runs one upload queued by [WorkManagerBackgroundUploadScheduler]. Subclasses supply the app's
 * collaborators (usually injected) and the foreground notification; the upload itself is the app's
 * [uploadWork].
 */
abstract class BackgroundUploadWorker<T>(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    protected abstract val uploadScheduler: WorkManagerBackgroundUploadScheduler<T>
    protected abstract val descriptorStorage: BackgroundUploadDescriptorStorage
    protected abstract val sessionRenewer: BackgroundUploadSessionRenewer
    protected abstract val uploadWork: BackgroundUploadWork<T>
    protected abstract val notification: BackgroundUploadNotification
    protected abstract val logTag: String
    protected open val maxRunAttempts: Int = DEFAULT_MAX_UPLOAD_RUN_ATTEMPTS

    override suspend fun doWork(): Result {
        val runner = BackgroundUploadWorkRunner(
            descriptorStorage = descriptorStorage,
            sessionRenewer = sessionRenewer,
            onCompletion = uploadScheduler::reportCompletion,
            logTag = logTag,
            maxRunAttempts = maxRunAttempts,
        )
        val workResult = try {
            runner.run(
                uploadId = inputData.getString(KEY_UPLOAD_ID),
                runAttemptCount = runAttemptCount,
                work = uploadWork,
                promoteToForeground = ::promoteToForeground,
            )
        } catch (outOfMemoryError: OutOfMemoryError) {
            Logger.e(outOfMemoryError) { "[$logTag] OOM during upload — will retry" }
            BackgroundUploadWorkResult.Retry
        }
        return when (workResult) {
            BackgroundUploadWorkResult.Success -> Result.success()
            BackgroundUploadWorkResult.Failure -> Result.failure()
            BackgroundUploadWorkResult.Retry -> Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        ensureNotificationChannel()
        val ongoingNotification = buildNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notification.notificationId, ongoingNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notification.notificationId, ongoingNotification)
        }
    }

    private suspend fun promoteToForeground() {
        runCatching { setForeground(getForegroundInfo()) }
            .onFailure { throwable -> Logger.w(throwable) { "[$logTag] setForeground failed — continuing without foreground priority" } }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(applicationContext, notification.channelId)
            .setSmallIcon(notification.smallIconResourceId)
            .setContentTitle(notification.title)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(notification.channelId) != null) return
        val channel = NotificationChannel(
            notification.channelId,
            notification.channelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = notification.channelDescription
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
    }
}
