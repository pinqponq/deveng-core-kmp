package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.model.PinqloqFailure
import core.pinqloq.domain.model.PinqloqFailureReason
import core.pinqloq.domain.model.PinqloqLevel
import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.repository.PinqloqLogRepository
import core.util.CustomLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TRUNCATION_SUFFIX = "...(truncated)"

/**
 * Metadata keys are merged in this order, and a later source replaces an earlier one:
 * the keys passed to [log], then the [PinqloqMetadataProvider] keys, then the platform keys of the request mapper.
 * Every replaced key is written to [CustomLogger].
 */
class PinqloqLoggerImpl(
    private val clientOptions: PinqloqClientOptions,
    private val logRepository: PinqloqLogRepository,
    private val platformInfoProvider: PinqloqPlatformInfoProvider,
    private val metadataProvider: PinqloqMetadataProvider,
    private val flushScope: CoroutineScope,
    private val clock: Clock = Clock.System
) : PinqloqLogger {
    private val queueMutex = Mutex()
    private val queuedEntries = ArrayDeque<PinqloqLogEntry>()
    private var flushTimerJob: Job? = null
    private var isQueueBeingSent = false

    override suspend fun log(
        level: PinqloqLevel,
        event: String,
        metadata: Map<String, String>,
        detail: Map<String, String>
    ) {
        if (!isEnabled()) {
            return
        }

        val entry = buildEntry(level, event, metadata, detail) ?: return

        CustomLogger.log("Pinqloq: $entry")

        if (clientOptions.batchSize == 1) {
            deliver(droppedLogCount = 1) { logRepository.saveLog(entry) }
        } else {
            enqueue(entry)
        }
    }

    private fun isEnabled(): Boolean = try {
        clientOptions.enabled()
    } catch (exception: Exception) {
        rethrowCancellation(exception)
        reportFailure(PinqloqFailure(PinqloqFailureReason.ENTRY_REJECTED, droppedLogCount = 1, cause = exception))
        false
    }

    private fun buildEntry(
        level: PinqloqLevel,
        event: String,
        metadata: Map<String, String>,
        detail: Map<String, String>
    ): PinqloqLogEntry? = try {
        val platformInfo = platformInfoProvider.providePlatformInfo()
        PinqloqLogEntry(
            event = truncateEvent(event),
            level = level,
            metadata = mergeMetadata(
                callSiteMetadata = metadata,
                providerMetadata = metadataProvider.provideMetadata(),
                platformMetadata = clientOptions.requestMapper.platformMetadata(platformInfo)
            ),
            detail = detail,
            platformInfo = platformInfo,
            occurredAt = clock.now()
        )
    } catch (exception: Exception) {
        rethrowCancellation(exception)
        reportFailure(PinqloqFailure(PinqloqFailureReason.ENTRY_REJECTED, droppedLogCount = 1, cause = exception))
        null
    }

    private fun truncateEvent(event: String): String {
        if (event.length <= clientOptions.maxEventLength) {
            return event
        }

        return event.take(clientOptions.maxEventLength) + TRUNCATION_SUFFIX
    }

    private fun mergeMetadata(
        callSiteMetadata: Map<String, String>,
        providerMetadata: Map<String, String>,
        platformMetadata: Map<String, String>
    ): Map<String, String> {
        val replacedKeys = callSiteMetadata.keys.intersect(providerMetadata.keys + platformMetadata.keys) +
            providerMetadata.keys.intersect(platformMetadata.keys)

        if (replacedKeys.isNotEmpty()) {
            CustomLogger.log("Pinqloq: metadata keys replaced by a later source: ${replacedKeys.sorted()}")
        }

        return callSiteMetadata + providerMetadata + platformMetadata
    }

    private suspend fun enqueue(entry: PinqloqLogEntry) {
        var droppedLogCount = 0

        queueMutex.withLock {
            if (queuedEntries.size >= clientOptions.queueCapacity) {
                queuedEntries.removeFirst()
                droppedLogCount = 1
            }
            queuedEntries.addLast(entry)

            if (queuedEntries.size >= clientOptions.batchSize) {
                startSendingQueue()
            } else {
                startFlushTimer()
            }
        }

        if (droppedLogCount > 0) {
            reportFailure(PinqloqFailure(PinqloqFailureReason.QUEUE_OVERFLOW, droppedLogCount))
        }
    }

    private fun startFlushTimer() {
        if (flushTimerJob?.isActive == true) {
            return
        }

        flushTimerJob = flushScope.launch {
            delay(clientOptions.flushInterval)
            queueMutex.withLock {
                if (queuedEntries.isNotEmpty()) {
                    startSendingQueue()
                }
            }
        }
    }

    private fun startSendingQueue() {
        if (isQueueBeingSent) {
            return
        }

        isQueueBeingSent = true
        flushScope.launch { sendQueuedBatches() }
    }

    private suspend fun sendQueuedBatches() {
        while (true) {
            val batch = takeNextBatch() ?: return
            deliver(droppedLogCount = batch.size) { logRepository.saveLogs(batch) }
        }
    }

    private suspend fun takeNextBatch(): List<PinqloqLogEntry>? = queueMutex.withLock {
        if (queuedEntries.isEmpty()) {
            isQueueBeingSent = false
            null
        } else {
            val batchSize = minOf(clientOptions.batchSize, queuedEntries.size)
            List(batchSize) { queuedEntries.removeFirst() }
        }
    }

    private suspend fun deliver(droppedLogCount: Int, send: suspend () -> Unit) {
        try {
            send()
        } catch (exception: Exception) {
            rethrowCancellation(exception)
            reportFailure(PinqloqFailure(PinqloqFailureReason.DELIVERY_FAILED, droppedLogCount, exception))
        }
    }

    private fun reportFailure(failure: PinqloqFailure) {
        try {
            clientOptions.failureListener.onFailure(failure)
        } catch (exception: Exception) {
            rethrowCancellation(exception)
            CustomLogger.log("Pinqloq: the failure listener threw: ${exception.stackTraceToString()}")
        }
    }

    private fun rethrowCancellation(exception: Exception) {
        if (exception is CancellationException) {
            throw exception
        }
    }
}
