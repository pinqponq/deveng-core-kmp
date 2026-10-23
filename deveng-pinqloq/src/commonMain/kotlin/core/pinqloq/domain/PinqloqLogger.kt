package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqLevel

interface PinqloqLogger {
    /**
     * Sends one log. Never throws: a failure goes to the configured [PinqloqFailureListener].
     * Only coroutine cancellation is rethrown.
     */
    suspend fun log(
        level: PinqloqLevel,
        event: String,
        metadata: Map<String, String> = emptyMap(),
        detail: Map<String, String> = emptyMap()
    )
}
