package core.data.upload

import core.data.upload.session.BackgroundUploadSessionScheduler

/**
 * Finds the scheduler that owns a background session, so the app delegate's
 * `application(_:handleEventsForBackgroundURLSession:completionHandler:)` can be forwarded by identifier.
 */
object BackgroundUploadSessionRegistry {

    private val schedulersBySessionIdentifier = mutableMapOf<String, BackgroundUploadSessionScheduler<*>>()

    internal fun register(sessionIdentifier: String, scheduler: BackgroundUploadSessionScheduler<*>) {
        schedulersBySessionIdentifier[sessionIdentifier] = scheduler
    }

    /** Calls [completionHandler] right away when no scheduler owns [sessionIdentifier]. */
    fun handleEventsForBackgroundUrlSession(sessionIdentifier: String, completionHandler: () -> Unit) {
        val scheduler = schedulersBySessionIdentifier[sessionIdentifier]
        if (scheduler == null) {
            completionHandler()
            return
        }
        scheduler.reconnect(onEventsDelivered = completionHandler)
    }
}
