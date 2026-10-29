package core.data.upload.session

/** The platform's background transfer service (an iOS background `NSURLSession`). */
interface BackgroundUploadTransport {

    fun bind(listener: BackgroundUploadTransferListener)

    /** Creates a suspended task for [request]; null when the request's url is not a valid url. */
    fun createTask(uploadId: String, request: BackgroundUploadTransferRequest): BackgroundUploadTask?

    fun cancelAllTasks()

    /** Reattaches to transfers the OS kept running while the app was gone; [onEventsDelivered] is the OS completion handler. */
    fun reconnect(onEventsDelivered: () -> Unit)
}

interface BackgroundUploadTask {
    val taskIdentifier: Long

    fun resume()

    fun cancel()
}

data class BackgroundUploadTransferRequest(
    val httpMethod: String,
    val url: String,
    val headers: Map<String, String>,
    val bodyFilePath: String,
)

interface BackgroundUploadTransferListener {

    fun onTransferProgress(uploadId: String, bytesSent: Long, bytesTotal: Long)

    fun onTransferCompleted(uploadId: String, outcome: BackgroundUploadTransferOutcome)
}

sealed interface BackgroundUploadTransferOutcome {

    /** The request never got an HTTP response; [failureReason] names the platform error. */
    data class NetworkError(val failureReason: String) : BackgroundUploadTransferOutcome

    /** [entityTag] is the ETag response header; storage sends it for every stored part. */
    data class Response(
        val statusCode: Int,
        val body: String?,
        val entityTag: String? = null,
    ) : BackgroundUploadTransferOutcome
}
