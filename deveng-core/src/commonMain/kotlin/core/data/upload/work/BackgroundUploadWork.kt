package core.data.upload.work

import core.domain.upload.model.BackgroundUploadDescriptor

/**
 * The app's part of a queued background upload on platforms that run uploads as scheduled work
 * (Android WorkManager): find what to send, call [BackgroundUploadWorkSession.start] once it is
 * certain there is something to send, then send it through the app's own API.
 * A thrown exception retries the work.
 */
fun interface BackgroundUploadWork<T> {

    suspend fun upload(
        descriptor: BackgroundUploadDescriptor,
        session: BackgroundUploadWorkSession,
    ): BackgroundUploadAttempt<T>
}

fun interface BackgroundUploadWorkSession {

    /** Renews the access token when it is about to expire and keeps the work running in the foreground. */
    suspend fun start()
}

sealed interface BackgroundUploadAttempt<out T> {

    data class Uploaded<out T>(val result: T) : BackgroundUploadAttempt<T>

    /** The queued item is gone (sent or discarded elsewhere); the work ends without a completion event. */
    data object NothingToUpload : BackgroundUploadAttempt<Nothing>

    data class Failed(val reason: String) : BackgroundUploadAttempt<Nothing>
}
