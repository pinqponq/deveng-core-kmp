package core.widget.android

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import core.widget.domain.HomeScreenWidgetReloader
import java.util.concurrent.TimeUnit

/** Fallback for pushes that never arrive (app force-stopped, notification data lost, network down). */
private const val DEFAULT_PERIODIC_REFRESH_INTERVAL_MINUTES = 30L

/**
 * Runs the app's widget refresh worker right away and every [periodicRefreshIntervalMinutes]. The work names are
 * WorkManager's unique work names: keep them unchanged across releases, or installed apps keep the old periodic
 * work next to the new one.
 */
class AndroidHomeScreenWidgetRefreshScheduler(
    private val context: Context,
    private val refreshWorkerClass: Class<out ListenableWorker>,
    private val immediateRefreshWorkName: String,
    private val periodicRefreshWorkName: String,
    private val periodicRefreshIntervalMinutes: Long = DEFAULT_PERIODIC_REFRESH_INTERVAL_MINUTES,
) : HomeScreenWidgetReloader {

    override fun reloadHomeScreenWidgets() {
        enqueueImmediateRefresh()
        schedulePeriodicRefresh()
    }

    /**
     * Expedited, because it usually runs straight after a widget push while the app is briefly allowed to do
     * background work; a newer request replaces an older one still waiting.
     */
    fun enqueueImmediateRefresh() {
        val refreshRequest = OneTimeWorkRequest.Builder(refreshWorkerClass)
            .setConstraints(buildNetworkConnectedConstraints())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(immediateRefreshWorkName, ExistingWorkPolicy.REPLACE, refreshRequest)
    }

    fun cancelPeriodicRefresh() {
        WorkManager.getInstance(context).cancelUniqueWork(periodicRefreshWorkName)
    }

    private fun schedulePeriodicRefresh() {
        val periodicRequest = PeriodicWorkRequest.Builder(refreshWorkerClass, periodicRefreshIntervalMinutes, TimeUnit.MINUTES)
            .setConstraints(buildNetworkConnectedConstraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(periodicRefreshWorkName, ExistingPeriodicWorkPolicy.KEEP, periodicRequest)
    }

    private fun buildNetworkConnectedConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
