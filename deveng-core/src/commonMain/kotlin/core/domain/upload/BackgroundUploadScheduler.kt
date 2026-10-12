package core.domain.upload

import core.domain.upload.model.BackgroundUploadCompletion
import core.domain.upload.model.BackgroundUploadDescriptor
import core.domain.upload.model.BackgroundUploadStatus
import core.domain.upload.model.UploadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Uploads files in the background so a transfer survives the app going to the background or being
 * killed. [T] is what the app reads from a finished upload (e.g. the created media's id).
 */
interface BackgroundUploadScheduler<out T> {

    val completionEvents: Flow<BackgroundUploadCompletion<T>>

    /** Byte-level progress for the in-flight upload. Only schedulers that can observe transfer
     *  progress emit here (iOS); the default is empty so other platforms need no implementation. */
    val progressEvents: Flow<UploadProgress> get() = emptyFlow()

    suspend fun schedule(descriptor: BackgroundUploadDescriptor)

    /** Cancels every scheduled/active upload. There is no per-item cancel. */
    fun cancelAll()

    suspend fun queryStatus(uploadId: String): BackgroundUploadStatus
}
