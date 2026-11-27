package core.pinqloq.domain.model

import core.pinqloq.domain.PinqloqFailureListener
import core.pinqloq.domain.PinqloqRequestMapper
import core.pinqloq.domain.PinqloqRequestMappers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_MAX_EVENT_LENGTH = 1000
private const val DEFAULT_BATCH_SIZE = 1
private const val DEFAULT_QUEUE_CAPACITY = 1000
private val DEFAULT_FLUSH_INTERVAL = 2.seconds

/**
 * @param endpoint path that receives one log per request.
 * @param batchEndpoint path that receives `{"collectionName": …, "logs": […]}`. Required when [batchSize] is above 1.
 * @param secretKey sent as the `X-Secret-Key` header when set.
 * @param collectionName sent as `collectionName` in batch requests only.
 * @param maxEventLength longer events are cut to this length and end with `...(truncated)`.
 * @param batchSize 1 sends every log at once; a larger value buffers logs and sends them together.
 * @param flushInterval longest time a buffered log waits before it is sent.
 * @param queueCapacity most logs that can wait; when full, the oldest one is dropped.
 * @param enabled checked on every log; when it returns false the log is not built or sent.
 * @param requestMapper wire shape of one log.
 * @param failureListener called when a log is dropped.
 */
data class PinqloqClientOptions(
    val endpoint: String,
    val batchEndpoint: String? = null,
    val secretKey: String? = null,
    val collectionName: String? = null,
    val maxEventLength: Int = DEFAULT_MAX_EVENT_LENGTH,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val flushInterval: Duration = DEFAULT_FLUSH_INTERVAL,
    val queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
    val enabled: () -> Boolean = { true },
    val requestMapper: PinqloqRequestMapper = PinqloqRequestMappers.LevelWithDeviceIdentifier,
    val failureListener: PinqloqFailureListener = PinqloqFailureListener.LogToConsole
) {
    init {
        require(endpoint.isNotBlank()) { "PinqloqClientOptions.endpoint must not be blank" }
        require(maxEventLength > 0) { "PinqloqClientOptions.maxEventLength must be above 0" }
        require(batchSize >= 1) { "PinqloqClientOptions.batchSize must be at least 1" }
        require(flushInterval.isPositive()) { "PinqloqClientOptions.flushInterval must be above zero" }
        require(queueCapacity >= batchSize) {
            "PinqloqClientOptions.queueCapacity must be at least batchSize"
        }
        require(batchSize == 1 || !batchEndpoint.isNullOrBlank()) {
            "PinqloqClientOptions.batchEndpoint is required when batchSize is above 1"
        }
    }

    override fun toString(): String =
        "PinqloqClientOptions(endpoint=$endpoint, batchEndpoint=$batchEndpoint, collectionName=$collectionName, " +
            "isSecretKeySet=${secretKey != null}, maxEventLength=$maxEventLength, batchSize=$batchSize, " +
            "flushInterval=$flushInterval, queueCapacity=$queueCapacity)"
}
