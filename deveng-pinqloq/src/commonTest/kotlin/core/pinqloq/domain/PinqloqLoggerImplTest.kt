package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.model.PinqloqFailureReason
import core.pinqloq.domain.model.PinqloqLevel
import core.pinqloq.domain.model.PinqloqPlatformInfo
import core.pinqloq.fake.FakePinqloqFailureListener
import core.pinqloq.fake.FakePinqloqLogRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class PinqloqLoggerImplTest {

    private val logRepository = FakePinqloqLogRepository()
    private val failureListener = FakePinqloqFailureListener()

    @Test
    fun log_withEventAtMaxLength_keepsEventUnchanged() = runTest {
        val event = "a".repeat(MAX_EVENT_LENGTH)

        createLogger(clientOptions = createOptions()).log(PinqloqLevel.Information, event)

        assertEquals(event, logRepository.savedEntries.single().event)
    }

    @Test
    fun log_withEventOneCharacterOverMaxLength_cutsEventAndAddsSuffix() = runTest {
        val event = "a".repeat(MAX_EVENT_LENGTH + 1)

        createLogger(clientOptions = createOptions()).log(PinqloqLevel.Information, event)

        assertEquals("a".repeat(MAX_EVENT_LENGTH) + "...(truncated)", logRepository.savedEntries.single().event)
    }

    @Test
    fun log_withLevelDetailAndTime_buildsEntryFromCallAndClock() = runTest {
        createLogger(clientOptions = createOptions()).log(
            level = PinqloqLevel.Warning,
            event = EVENT,
            detail = mapOf("stack" to "trace")
        )

        val entry = logRepository.savedEntries.single()
        assertEquals(PinqloqLevel.Warning, entry.level)
        assertEquals(mapOf("stack" to "trace"), entry.detail)
        assertEquals(PLATFORM_INFO, entry.platformInfo)
        assertEquals(OCCURRED_AT, entry.occurredAt)
    }

    @Test
    fun log_whenDisabled_buildsAndSendsNothing() = runTest {
        var isMetadataRequested = false
        val logger = createLogger(
            clientOptions = createOptions(enabled = { false }),
            metadataProvider = PinqloqMetadataProvider {
                isMetadataRequested = true
                emptyMap()
            }
        )

        logger.log(PinqloqLevel.Error, EVENT)

        assertTrue(logRepository.savedEntries.isEmpty())
        assertEquals(false, isMetadataRequested)
        assertTrue(failureListener.failures.isEmpty())
    }

    @Test
    fun log_whenEnabledChangesBetweenCalls_sendsOnlyWhileEnabled() = runTest {
        var isSignedIn = false
        val logger = createLogger(clientOptions = createOptions(enabled = { isSignedIn }))

        logger.log(PinqloqLevel.Information, "before sign in")
        isSignedIn = true
        logger.log(PinqloqLevel.Information, "after sign in")

        assertEquals(listOf("after sign in"), logRepository.savedEntries.map { entry -> entry.event })
    }

    @Test
    fun log_withProviderAndPlatformMetadata_mergesAllSources() = runTest {
        val logger = createLogger(
            clientOptions = createOptions(),
            metadataProvider = PinqloqMetadataProvider { mapOf("userId" to "42") }
        )

        logger.log(PinqloqLevel.Information, EVENT, metadata = mapOf("screen" to "home"))

        assertEquals(
            mapOf("screen" to "home", "userId" to "42", "deviceName" to DEVICE_NAME),
            logRepository.savedEntries.single().metadata
        )
    }

    @Test
    fun log_whenProviderKeyCollidesWithPlatformKey_keepsPlatformValue() = runTest {
        val logger = createLogger(
            clientOptions = createOptions(),
            metadataProvider = PinqloqMetadataProvider { mapOf("deviceName" to "provider device") }
        )

        logger.log(PinqloqLevel.Information, EVENT)

        assertEquals(DEVICE_NAME, logRepository.savedEntries.single().metadata.getValue("deviceName"))
    }

    @Test
    fun log_whenCallKeyCollidesWithProviderKey_keepsProviderValue() = runTest {
        val logger = createLogger(
            clientOptions = createOptions(),
            metadataProvider = PinqloqMetadataProvider { mapOf("userId" to "42") }
        )

        logger.log(PinqloqLevel.Information, EVENT, metadata = mapOf("userId" to "from call"))

        assertEquals("42", logRepository.savedEntries.single().metadata.getValue("userId"))
    }

    @Test
    fun log_whenRepositoryFails_doesNotThrowAndRecordsFailure() = runTest {
        val cause = IllegalStateException("offline")
        logRepository.failure = cause

        createLogger(clientOptions = createOptions()).log(PinqloqLevel.Error, EVENT)

        val failure = failureListener.failures.single()
        assertEquals(PinqloqFailureReason.DELIVERY_FAILED, failure.reason)
        assertEquals(1, failure.droppedLogCount)
        assertEquals(cause, failure.cause)
    }

    @Test
    fun log_whenFailureListenerThrows_doesNotThrow() = runTest {
        logRepository.failure = IllegalStateException("offline")
        val logger = createLogger(
            clientOptions = createOptions(failureListener = PinqloqFailureListener { error("listener failed") })
        )

        logger.log(PinqloqLevel.Error, EVENT)
    }

    @Test
    fun log_whenRepositoryIsCancelled_rethrowsCancellation() = runTest {
        logRepository.failure = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            createLogger(clientOptions = createOptions()).log(PinqloqLevel.Error, EVENT)
        }
        assertTrue(failureListener.failures.isEmpty())
    }

    @Test
    fun log_whenMetadataProviderFails_sendsNothingAndRecordsRejectedEntry() = runTest {
        val cause = IllegalStateException("no user")
        val logger = createLogger(
            clientOptions = createOptions(),
            metadataProvider = PinqloqMetadataProvider { throw cause }
        )

        logger.log(PinqloqLevel.Error, EVENT)

        assertTrue(logRepository.savedEntries.isEmpty())
        val failure = failureListener.failures.single()
        assertEquals(PinqloqFailureReason.ENTRY_REJECTED, failure.reason)
        assertEquals(cause, failure.cause)
    }

    @Test
    fun log_whenEnabledGateFails_sendsNothingAndRecordsRejectedEntry() = runTest {
        val logger = createLogger(clientOptions = createOptions(enabled = { error("gate failed") }))

        logger.log(PinqloqLevel.Error, EVENT)

        assertTrue(logRepository.savedEntries.isEmpty())
        assertEquals(PinqloqFailureReason.ENTRY_REJECTED, failureListener.failures.single().reason)
    }

    @Test
    fun log_whenMetadataProviderIsCancelled_rethrowsCancellation() = runTest {
        val logger = createLogger(
            clientOptions = createOptions(),
            metadataProvider = PinqloqMetadataProvider { throw CancellationException("cancelled") }
        )

        assertFailsWith<CancellationException> { logger.log(PinqloqLevel.Error, EVENT) }
    }

    @Test
    fun log_whenEnabledGateIsCancelled_rethrowsCancellation() = runTest {
        val logger = createLogger(clientOptions = createOptions(enabled = { throw CancellationException("cancelled") }))

        assertFailsWith<CancellationException> { logger.log(PinqloqLevel.Error, EVENT) }
    }

    @Test
    fun log_withBatchSizeOne_sendsEachLogAtOnceAndNotAsBatch() = runTest {
        val logger = createLogger(clientOptions = createOptions())

        logger.log(PinqloqLevel.Information, "first")
        logger.log(PinqloqLevel.Information, "second")

        assertEquals(listOf("first", "second"), logRepository.savedEntries.map { entry -> entry.event })
        assertTrue(logRepository.savedBatches.isEmpty())
    }

    @Test
    fun log_withBatchingBelowBatchSize_sendsNothingBeforeFlushInterval() = runTest {
        val logger = createBatchingLogger(batchSize = 3)

        logger.log(PinqloqLevel.Information, "first")
        logger.log(PinqloqLevel.Information, "second")
        advanceTimeBy(FLUSH_INTERVAL - 1.seconds)
        runCurrent()

        assertTrue(logRepository.savedBatches.isEmpty())
        assertTrue(logRepository.savedEntries.isEmpty())
    }

    @Test
    fun log_withBatchingWhenBatchSizeReached_sendsOneBatchInOrder() = runTest {
        val logger = createBatchingLogger(batchSize = 3)

        logger.log(PinqloqLevel.Information, "first")
        logger.log(PinqloqLevel.Information, "second")
        logger.log(PinqloqLevel.Information, "third")
        runCurrent()

        assertEquals(
            listOf(listOf("first", "second", "third")),
            logRepository.savedBatches.map { batch -> batch.map { entry -> entry.event } }
        )
    }

    @Test
    fun log_withBatchingWhenFlushIntervalPasses_sendsPartialBatch() = runTest {
        val logger = createBatchingLogger(batchSize = 3)

        logger.log(PinqloqLevel.Information, "first")
        logger.log(PinqloqLevel.Information, "second")
        advanceTimeBy(FLUSH_INTERVAL)
        runCurrent()

        assertEquals(
            listOf(listOf("first", "second")),
            logRepository.savedBatches.map { batch -> batch.map { entry -> entry.event } }
        )
    }

    @Test
    fun log_withBatchingAfterFlushInterval_startsNewTimerForNextLog() = runTest {
        val logger = createBatchingLogger(batchSize = 3)

        logger.log(PinqloqLevel.Information, "first")
        advanceTimeBy(FLUSH_INTERVAL)
        runCurrent()
        logger.log(PinqloqLevel.Information, "second")
        advanceTimeBy(FLUSH_INTERVAL)
        runCurrent()

        assertEquals(
            listOf(listOf("first"), listOf("second")),
            logRepository.savedBatches.map { batch -> batch.map { entry -> entry.event } }
        )
    }

    @Test
    fun log_withBatchingMoreLogsThanBatchSize_sendsFullBatchesThenTheRest() = runTest {
        val logger = createBatchingLogger(batchSize = 2)

        repeat(5) { index -> logger.log(PinqloqLevel.Information, "log $index") }
        runCurrent()
        advanceTimeBy(FLUSH_INTERVAL)
        runCurrent()

        assertEquals(
            listOf(2, 2, 1),
            logRepository.savedBatches.map { batch -> batch.size }
        )
    }

    @Test
    fun log_withBatchingWhenQueueIsFull_dropsOldestLogAndRecordsOverflow() = runTest {
        val logger = createBatchingLogger(batchSize = 2, queueCapacity = 3)
        val gate = CompletableDeferred<Unit>()
        logRepository.gate = gate

        listOf("a", "b").forEach { event -> logger.log(PinqloqLevel.Information, event) }
        runCurrent()
        listOf("c", "d", "e", "f").forEach { event -> logger.log(PinqloqLevel.Information, event) }
        gate.complete(Unit)
        runCurrent()

        val failure = failureListener.failures.single()
        assertEquals(PinqloqFailureReason.QUEUE_OVERFLOW, failure.reason)
        assertEquals(1, failure.droppedLogCount)
        assertEquals(
            listOf(listOf("a", "b"), listOf("d", "e"), listOf("f")),
            logRepository.savedBatches.map { batch -> batch.map { entry -> entry.event } }
        )
    }

    @Test
    fun log_withBatchingWhenBatchDeliveryFails_recordsDroppedCountAndKeepsSendingLater() = runTest {
        val logger = createBatchingLogger(batchSize = 2)
        logRepository.failure = IllegalStateException("offline")

        logger.log(PinqloqLevel.Information, "lost 1")
        logger.log(PinqloqLevel.Information, "lost 2")
        runCurrent()
        logRepository.failure = null
        logger.log(PinqloqLevel.Information, "kept 1")
        logger.log(PinqloqLevel.Information, "kept 2")
        runCurrent()

        val failure = failureListener.failures.single()
        assertEquals(PinqloqFailureReason.DELIVERY_FAILED, failure.reason)
        assertEquals(2, failure.droppedLogCount)
        assertEquals(
            listOf(listOf("kept 1", "kept 2")),
            logRepository.savedBatches.map { batch -> batch.map { entry -> entry.event } }
        )
    }

    private fun TestScope.createBatchingLogger(batchSize: Int, queueCapacity: Int = 1000): PinqloqLogger =
        createLogger(
            clientOptions = createOptions(batchSize = batchSize, queueCapacity = queueCapacity),
            flushScope = this.backgroundScope
        )

    private fun createLogger(
        clientOptions: PinqloqClientOptions,
        metadataProvider: PinqloqMetadataProvider = PinqloqMetadataProvider.None,
        flushScope: CoroutineScope = CoroutineScope(Job())
    ): PinqloqLogger = PinqloqLoggerImpl(
        clientOptions = clientOptions,
        logRepository = logRepository,
        platformInfoProvider = { PLATFORM_INFO },
        metadataProvider = metadataProvider,
        flushScope = flushScope,
        clock = object : Clock {
            override fun now(): Instant = OCCURRED_AT
        }
    )

    private fun createOptions(
        batchSize: Int = 1,
        queueCapacity: Int = 1000,
        enabled: () -> Boolean = { true },
        failureListener: PinqloqFailureListener = this.failureListener
    ): PinqloqClientOptions = PinqloqClientOptions(
        endpoint = "client-logs",
        batchEndpoint = "client-logs/bulk",
        maxEventLength = MAX_EVENT_LENGTH,
        batchSize = batchSize,
        flushInterval = FLUSH_INTERVAL,
        queueCapacity = queueCapacity,
        enabled = enabled,
        requestMapper = PinqloqRequestMappers.LevelWithDeviceIdentifier,
        failureListener = failureListener
    )

    private companion object {
        const val EVENT = "event"
        const val DEVICE_NAME = "Pixel"
        const val MAX_EVENT_LENGTH = 20
        val FLUSH_INTERVAL = 2.seconds
        val OCCURRED_AT = Instant.fromEpochSeconds(1_700_000_000)
        val PLATFORM_INFO = PinqloqPlatformInfo(
            appVersionName = "1.2.3",
            deviceUid = "device-uid",
            deviceName = DEVICE_NAME
        )
    }
}
