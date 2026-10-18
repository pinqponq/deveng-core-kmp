package core.push.presentation

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A ready Firebase Cloud Messaging service: registers rotated tokens through [pushNotifications],
 * creates the notification channel from its options, shows notification messages and opens
 * [launchActivityClass] on a tap, with the deep link from [resolveDeepLink] as the intent data.
 *
 * Subclass it, provide the three abstract members and declare the subclass in the app manifest with
 * the `com.google.firebase.MESSAGING_EVENT` intent filter. Override [handleDataMessage] for the app's
 * own data-only messages.
 */
abstract class PushNotificationMessagingService : FirebaseMessagingService() {

    protected abstract val pushNotifications: PushNotifications

    protected abstract val launchActivityClass: Class<out Activity>

    @get:DrawableRes
    protected abstract val notificationIconResourceId: Int

    protected open val defaultNotificationTitle: String
        get() = applicationInfo.loadLabel(packageManager).toString()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        tokenRegistrationScope.launch { pushNotifications.handleNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (handleDataMessage(message)) return

        val notification = message.notification ?: return
        showNotification(
            title = notification.title ?: defaultNotificationTitle,
            body = notification.body.orEmpty(),
            deepLink = resolveDeepLink(message)
        )
    }

    /**
     * Handles a message the app shows or processes itself (for example a data-only message that
     * updates a widget). Returns true when the message was handled and must not be shown by default.
     */
    protected open fun handleDataMessage(message: RemoteMessage): Boolean = false

    /**
     * The deep link a tap on the notification opens, or null to just open [launchActivityClass].
     */
    protected open fun resolveDeepLink(message: RemoteMessage): String? = null

    protected fun showNotification(title: String, body: String, deepLink: String?) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notificationRequestCode = System.currentTimeMillis().toInt()
        val launchIntent = Intent(this, launchActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLink != null) data = deepLink.toUri()
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            notificationRequestCode,
            launchIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, pushNotifications.options.androidChannelId)
            .setSmallIcon(notificationIconResourceId)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(notificationRequestCode, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val options = pushNotifications.options
        val channel = NotificationChannel(
            options.androidChannelId,
            options.androidChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = options.androidChannelDescription
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        // Process-wide on purpose: the system stops the service as soon as onNewToken returns, and a
        // scope tied to the service would cancel the registration request with it.
        val tokenRegistrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
