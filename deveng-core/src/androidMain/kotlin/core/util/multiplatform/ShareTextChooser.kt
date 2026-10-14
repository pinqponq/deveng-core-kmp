package core.util.multiplatform

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

internal const val SHARE_CHOOSER_TITLE = "Share via"
private const val TARGET_CHOSEN_ACTION_SUFFIX = ".SHARE_TEXT_TARGET_CHOSEN."

/**
 * Shows the system chooser for plain text and waits until it closes.
 *
 * Android only reports the picked component (through the chooser's `IntentSender`), never whether
 * the text was sent, and it reports nothing when the chooser is dismissed. A dismissal is therefore
 * read from the app's own activity coming back to the foreground without a picked component.
 */
internal class ShareTextChooser(context: Context) {

    private val application = context.applicationContext as Application

    suspend fun share(text: String): ShareTextResult = suspendCancellableCoroutine { continuation ->
        val shareRequestCode = nextShareRequestCode.incrementAndGet()
        val targetChosenAction = application.packageName + TARGET_CHOSEN_ACTION_SUFFIX + shareRequestCode
        val shareListener = ShareListener(continuation = continuation, application = application)

        ContextCompat.registerReceiver(
            application,
            shareListener.targetChosenReceiver,
            IntentFilter(targetChosenAction),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        application.registerActivityLifecycleCallbacks(shareListener.appReturnCallbacks)
        continuation.invokeOnCancellation { shareListener.release() }

        try {
            application.startActivity(
                createChooserIntent(
                    text = text,
                    targetChosenAction = targetChosenAction,
                    shareRequestCode = shareRequestCode
                )
            )
        } catch (exception: RuntimeException) {
            shareListener.release()
            throw exception
        }
    }

    private fun createChooserIntent(text: String, targetChosenAction: String, shareRequestCode: Int): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val targetChosenIntent = Intent(targetChosenAction).setPackage(application.packageName)
        val targetChosenPendingIntent = PendingIntent.getBroadcast(
            application,
            shareRequestCode,
            targetChosenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutablePendingIntentFlag()
        )

        return Intent.createChooser(sendIntent, SHARE_CHOOSER_TITLE, targetChosenPendingIntent.intentSender).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // The chooser writes the picked component into the broadcast, which needs a mutable intent.
    private fun mutablePendingIntentFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    private class ShareListener(
        private val continuation: CancellableContinuation<ShareTextResult>,
        private val application: Application,
    ) {

        private val isReleased = AtomicBoolean(false)

        val targetChosenReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val chosenComponent = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_CHOSEN_COMPONENT,
                    ComponentName::class.java
                )
                finish(
                    shareTextResult = ShareTextResult(
                        outcome = ShareTextOutcome.TARGET_SELECTED,
                        targetApp = chosenComponent?.packageName
                    )
                )
            }
        }

        val appReturnCallbacks = object : Application.ActivityLifecycleCallbacks {
            private var isAppActivityPaused = false

            override fun onActivityPaused(activity: Activity) {
                isAppActivityPaused = true
            }

            override fun onActivityResumed(activity: Activity) {
                if (!isAppActivityPaused) return

                finish(shareTextResult = ShareTextResult(outcome = ShareTextOutcome.DISMISSED))
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        }

        fun finish(shareTextResult: ShareTextResult) {
            if (!release()) return

            if (continuation.isActive) {
                continuation.resume(shareTextResult)
            }
        }

        /** Unregisters both listeners once; returns false when they were already released. */
        fun release(): Boolean {
            if (!isReleased.compareAndSet(false, true)) return false

            application.unregisterReceiver(targetChosenReceiver)
            application.unregisterActivityLifecycleCallbacks(appReturnCallbacks)
            return true
        }
    }

    private companion object {
        val nextShareRequestCode = AtomicInteger(0)
    }
}
