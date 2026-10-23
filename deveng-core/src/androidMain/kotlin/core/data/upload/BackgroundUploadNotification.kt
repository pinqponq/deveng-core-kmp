package core.data.upload

import androidx.annotation.DrawableRes

/** The ongoing notification Android shows while a [BackgroundUploadWorker] runs in the foreground. */
data class BackgroundUploadNotification(
    val notificationId: Int,
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    val title: String,
    @DrawableRes
    val smallIconResourceId: Int,
)
