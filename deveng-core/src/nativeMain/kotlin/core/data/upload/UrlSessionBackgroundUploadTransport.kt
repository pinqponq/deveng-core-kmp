package core.data.upload

import core.data.upload.session.BackgroundUploadTask
import core.data.upload.session.BackgroundUploadTransferListener
import core.data.upload.session.BackgroundUploadTransferRequest
import core.data.upload.session.BackgroundUploadTransport
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionUploadTask
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue

/**
 * A background `NSURLSession` with its own [sessionIdentifier]: iOS keeps its transfers running
 * after the app is suspended or killed and relaunches the app to deliver their results.
 */
class UrlSessionBackgroundUploadTransport(
    private val sessionIdentifier: String,
    delegateLogTag: String,
) : BackgroundUploadTransport {

    private val delegate = UrlSessionBackgroundUploadDelegate(logTag = delegateLogTag)

    private val session: NSURLSession by lazy {
        NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(sessionIdentifier),
            delegate = delegate,
            delegateQueue = null,
        )
    }

    override fun bind(listener: BackgroundUploadTransferListener) {
        delegate.listener = listener
    }

    override fun createTask(uploadId: String, request: BackgroundUploadTransferRequest): BackgroundUploadTask? {
        val url = NSURL.URLWithString(request.url) ?: return null
        val urlRequest = NSMutableURLRequest.requestWithURL(url)
        urlRequest.setHTTPMethod(request.httpMethod)
        request.headers.forEach { (name, value) -> urlRequest.setValue(value, forHTTPHeaderField = name) }

        val task = session.uploadTaskWithRequest(urlRequest, fromFile = NSURL.fileURLWithPath(request.bodyFilePath))
        task.taskDescription = uploadId
        return UrlSessionBackgroundUploadTask(task)
    }

    override fun cancelAllTasks() {
        session.getTasksWithCompletionHandler { _, uploadTasks, _ ->
            uploadTasks
                ?.filterIsInstance<NSURLSessionTask>()
                ?.forEach { task -> task.cancel() }
        }
    }

    override fun reconnect(onEventsDelivered: () -> Unit) {
        delegate.backgroundCompletionHandler = onEventsDelivered
        session.getTasksWithCompletionHandler { _, _, _ -> }
    }

    private class UrlSessionBackgroundUploadTask(
        private val task: NSURLSessionUploadTask,
    ) : BackgroundUploadTask {
        override val taskIdentifier: Long get() = task.taskIdentifier.toLong()

        override fun resume() = task.resume()

        override fun cancel() = task.cancel()
    }
}
