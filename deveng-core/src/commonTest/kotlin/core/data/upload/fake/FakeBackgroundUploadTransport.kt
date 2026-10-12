package core.data.upload.fake

import core.data.upload.session.BackgroundUploadTask
import core.data.upload.session.BackgroundUploadTransferListener
import core.data.upload.session.BackgroundUploadTransferOutcome
import core.data.upload.session.BackgroundUploadTransferRequest
import core.data.upload.session.BackgroundUploadTransport

class FakeBackgroundUploadTransport : BackgroundUploadTransport {
    var listener: BackgroundUploadTransferListener? = null
    var isCreatingTasks = true
    val createdRequests = mutableListOf<Pair<String, BackgroundUploadTransferRequest>>()
    val tasks = mutableListOf<FakeBackgroundUploadTask>()
    var cancelAllCount = 0
    val reconnectHandlers = mutableListOf<() -> Unit>()

    override fun bind(listener: BackgroundUploadTransferListener) {
        this.listener = listener
    }

    override fun createTask(uploadId: String, request: BackgroundUploadTransferRequest): BackgroundUploadTask? {
        if (!isCreatingTasks) return null
        createdRequests += uploadId to request
        return FakeBackgroundUploadTask(taskIdentifier = tasks.size.toLong(), uploadId = uploadId).also { task -> tasks += task }
    }

    override fun cancelAllTasks() {
        cancelAllCount++
    }

    override fun reconnect(onEventsDelivered: () -> Unit) {
        reconnectHandlers += onEventsDelivered
    }

    fun complete(uploadId: String, outcome: BackgroundUploadTransferOutcome) {
        requireNotNull(listener) { "The scheduler binds itself on creation" }.onTransferCompleted(uploadId, outcome)
    }

    fun reportProgress(uploadId: String, bytesSent: Long, bytesTotal: Long) {
        requireNotNull(listener) { "The scheduler binds itself on creation" }.onTransferProgress(uploadId, bytesSent, bytesTotal)
    }
}

class FakeBackgroundUploadTask(
    override val taskIdentifier: Long,
    val uploadId: String,
) : BackgroundUploadTask {
    var isResumed = false
    var isCancelled = false

    override fun resume() {
        isResumed = true
    }

    override fun cancel() {
        isCancelled = true
    }
}
