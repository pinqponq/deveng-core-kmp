package core.data.upload

import co.touchlab.kermit.Logger
import core.data.upload.session.BackgroundUploadTransferListener
import core.data.upload.session.BackgroundUploadTransferOutcome
import core.domain.camera.utils.toByteArray
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionDelegateProtocol
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskDelegateProtocol
import platform.darwin.NSObject

private const val PERCENT_MULTIPLIER = 100
private const val BYTES_PER_KILOBYTE = 1024
private const val NO_HTTP_RESPONSE_FAILURE_REASON = "network_no_http_response"

/** Collects one background session's callbacks per task (task description = upload id) and hands them to the listener. */
internal class UrlSessionBackgroundUploadDelegate(
    private val logTag: String,
) : NSObject(), NSURLSessionDataDelegateProtocol, NSURLSessionTaskDelegateProtocol, NSURLSessionDelegateProtocol {

    var listener: BackgroundUploadTransferListener? = null

    var backgroundCompletionHandler: (() -> Unit)? = null

    private val accumulatedChunks = mutableMapOf<String, MutableList<ByteArray>>()

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData,
    ) {
        val uploadId = dataTask.taskDescription ?: return
        accumulatedChunks.getOrPut(uploadId) { mutableListOf() }.add(didReceiveData.toByteArray())
        Logger.i { "[$logTag] didReceiveData uploadId=$uploadId chunkSize=${didReceiveData.length}B" }
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didSendBodyData: Long,
        totalBytesSent: Long,
        totalBytesExpectedToSend: Long,
    ) {
        val uploadId = task.taskDescription ?: return
        val percent = (totalBytesSent * PERCENT_MULTIPLIER / totalBytesExpectedToSend).takeIf { totalBytesExpectedToSend > 0 }
        Logger.i { "[$logTag] progress uploadId=$uploadId sent=${totalBytesSent / BYTES_PER_KILOBYTE}KB total=${totalBytesExpectedToSend / BYTES_PER_KILOBYTE}KB pct=${percent ?: "unknown"}%" }
        listener?.onTransferProgress(uploadId = uploadId, bytesSent = totalBytesSent, bytesTotal = totalBytesExpectedToSend)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        val uploadId = task.taskDescription ?: return
        val chunks = accumulatedChunks.remove(uploadId)
        val httpResponse = task.response as? NSHTTPURLResponse
        Logger.i { "[$logTag] didComplete uploadId=$uploadId taskId=${task.taskIdentifier} statusCode=${httpResponse?.statusCode} error=${didCompleteWithError?.localizedDescription}" }

        val outcome = when {
            didCompleteWithError != null -> BackgroundUploadTransferOutcome.NetworkError(
                failureReason = "network_${didCompleteWithError.domain}_${didCompleteWithError.code}: ${didCompleteWithError.localizedDescription}",
            )

            httpResponse == null -> BackgroundUploadTransferOutcome.NetworkError(failureReason = NO_HTTP_RESPONSE_FAILURE_REASON)

            else -> BackgroundUploadTransferOutcome.Response(
                statusCode = httpResponse.statusCode.toInt(),
                body = chunks?.let(::concatenate)?.decodeToString(),
            )
        }
        listener?.onTransferCompleted(uploadId = uploadId, outcome = outcome)
    }

    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
        Logger.i { "[$logTag] URLSessionDidFinishEvents — calling backgroundCompletionHandler" }
        val handler = backgroundCompletionHandler
        backgroundCompletionHandler = null
        handler?.invoke()
    }

    override fun URLSession(session: NSURLSession, didBecomeInvalidWithError: NSError?) {
        Logger.e { "[$logTag] SESSION_INVALID error=${didBecomeInvalidWithError?.localizedDescription}" }
    }

    private fun concatenate(chunks: List<ByteArray>): ByteArray {
        val combined = ByteArray(chunks.sumOf { chunk -> chunk.size })
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(combined, offset)
            offset += chunk.size
        }
        return combined
    }
}
