package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.model.PinqloqLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PinqloqClientOptionsTest {

    @Test
    fun create_withBlankEndpoint_fails() {
        assertFailsWith<IllegalArgumentException> { PinqloqClientOptions(endpoint = " ") }
    }

    @Test
    fun create_withZeroMaxEventLength_fails() {
        assertFailsWith<IllegalArgumentException> { PinqloqClientOptions(endpoint = ENDPOINT, maxEventLength = 0) }
    }

    @Test
    fun create_withZeroBatchSize_fails() {
        assertFailsWith<IllegalArgumentException> { PinqloqClientOptions(endpoint = ENDPOINT, batchSize = 0) }
    }

    @Test
    fun create_withZeroFlushInterval_fails() {
        assertFailsWith<IllegalArgumentException> {
            PinqloqClientOptions(endpoint = ENDPOINT, flushInterval = Duration.ZERO)
        }
    }

    @Test
    fun create_withQueueCapacityBelowBatchSize_fails() {
        assertFailsWith<IllegalArgumentException> {
            PinqloqClientOptions(
                endpoint = ENDPOINT,
                batchEndpoint = BATCH_ENDPOINT,
                batchSize = 5,
                queueCapacity = 4
            )
        }
    }

    @Test
    fun create_withBatchingAndNoBatchEndpoint_fails() {
        assertFailsWith<IllegalArgumentException> { PinqloqClientOptions(endpoint = ENDPOINT, batchSize = 2) }
    }

    @Test
    fun create_withBatchingAndBlankBatchEndpoint_fails() {
        assertFailsWith<IllegalArgumentException> {
            PinqloqClientOptions(endpoint = ENDPOINT, batchEndpoint = " ", batchSize = 2)
        }
    }

    @Test
    fun create_withBatchingAndBatchEndpoint_succeeds() {
        val clientOptions = PinqloqClientOptions(
            endpoint = ENDPOINT,
            batchEndpoint = BATCH_ENDPOINT,
            batchSize = 2,
            queueCapacity = 2,
            flushInterval = 1.seconds
        )

        assertEquals(2, clientOptions.batchSize)
    }

    @Test
    fun create_withOnlyEndpoint_usesDocumentedDefaults() {
        val clientOptions = PinqloqClientOptions(endpoint = ENDPOINT)

        assertEquals(1000, clientOptions.maxEventLength)
        assertEquals(1, clientOptions.batchSize)
        assertEquals(2.seconds, clientOptions.flushInterval)
        assertEquals(1000, clientOptions.queueCapacity)
        assertEquals(true, clientOptions.enabled())
    }

    @Test
    fun toString_withSecretKey_doesNotContainTheSecretKey() {
        val clientOptions = PinqloqClientOptions(endpoint = ENDPOINT, secretKey = "top-secret")

        assertFalse(clientOptions.toString().contains("top-secret"))
    }

    @Test
    fun toString_withAndWithoutSecretKey_showsEndpointAndWhetherSecretKeyIsSet() {
        val withSecretKey = PinqloqClientOptions(endpoint = ENDPOINT, secretKey = "top-secret").toString()
        val withoutSecretKey = PinqloqClientOptions(endpoint = ENDPOINT).toString()

        assertTrue(withSecretKey.contains("endpoint=$ENDPOINT"))
        assertTrue(withSecretKey.contains("isSecretKeySet=true"))
        assertTrue(withoutSecretKey.contains("isSecretKeySet=false"))
    }

    @Test
    fun backendValue_ofEveryLevel_matchesTheValuesTheBackendParses() {
        assertEquals(
            listOf("Debug", "Information", "Warning", "Error", "Fatal"),
            PinqloqLevel.entries.map { level -> level.backendValue }
        )
    }

    private companion object {
        const val ENDPOINT = "client-logs"
        const val BATCH_ENDPOINT = "client-logs/bulk"
    }
}
