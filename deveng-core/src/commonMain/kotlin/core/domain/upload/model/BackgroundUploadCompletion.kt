package core.domain.upload.model

sealed class BackgroundUploadCompletion<out T> {

    abstract val uploadId: String

    data class Success<out T>(
        override val uploadId: String,
        val result: T,
    ) : BackgroundUploadCompletion<T>()

    data class PermanentFailure(
        override val uploadId: String,
        val reason: String,
    ) : BackgroundUploadCompletion<Nothing>()
}
