package core.pinqloq.data

import core.pinqloq.domain.PinqloqRequestMappers
import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.model.PinqloqLevel
import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.model.PinqloqPlatformInfo
import core.pinqloq.fake.FakePinqloqRequestSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class PinqloqLogRepositoryImplTest {

    private val requestSender = FakePinqloqRequestSender()

    @Test
    fun saveLog_withSecretKey_postsMappedEntryToEndpointWithSecretKeyHeader() = runTest {
        val repository = createRepository(createOptions(secretKey = "key"))

        repository.saveLog(ENTRY)

        val sentRequest = requestSender.sentRequests.single()
        assertEquals(ENDPOINT, sentRequest.endpoint)
        assertEquals(mapOf("X-Secret-Key" to "key"), sentRequest.headers)
        assertEquals(PinqloqRequestMappers.LevelWithDeviceIdentifier.mapEntry(ENTRY), sentRequest.body)
    }

    @Test
    fun saveLog_withoutSecretKey_sendsNoHeaders() = runTest {
        createRepository(createOptions()).saveLog(ENTRY)

        assertEquals(emptyMap(), requestSender.sentRequests.single().headers)
    }

    @Test
    fun saveLogs_withCollectionName_postsLogsAndCollectionNameToBatchEndpoint() = runTest {
        val repository = createRepository(createOptions(secretKey = "key", collectionName = "app-logs"))

        repository.saveLogs(listOf(ENTRY, ENTRY))

        val sentRequest = requestSender.sentRequests.single()
        assertEquals(BATCH_ENDPOINT, sentRequest.endpoint)
        assertEquals(mapOf("X-Secret-Key" to "key"), sentRequest.headers)
        assertEquals(setOf("collectionName", "logs"), sentRequest.body.keys)
        assertEquals(JsonPrimitive("app-logs"), sentRequest.body.getValue("collectionName"))
        assertEquals(
            JsonArray(List(2) { PinqloqRequestMappers.LevelWithDeviceIdentifier.mapEntry(ENTRY) }),
            sentRequest.body.getValue("logs")
        )
    }

    @Test
    fun saveLogs_withoutCollectionName_omitsCollectionName() = runTest {
        createRepository(createOptions()).saveLogs(listOf(ENTRY))

        assertEquals(setOf("logs"), requestSender.sentRequests.single().body.keys)
    }

    @Test
    fun saveLogs_withoutBatchEndpoint_fails() = runTest {
        val repository = createRepository(PinqloqClientOptions(endpoint = ENDPOINT))

        assertFailsWith<IllegalArgumentException> { repository.saveLogs(listOf(ENTRY)) }
    }

    private fun createRepository(clientOptions: PinqloqClientOptions) = PinqloqLogRepositoryImpl(
        clientOptions = clientOptions,
        requestSender = requestSender
    )

    private fun createOptions(secretKey: String? = null, collectionName: String? = null) = PinqloqClientOptions(
        endpoint = ENDPOINT,
        batchEndpoint = BATCH_ENDPOINT,
        secretKey = secretKey,
        collectionName = collectionName
    )

    private companion object {
        const val ENDPOINT = "client-logs"
        const val BATCH_ENDPOINT = "client-logs/bulk"
        val ENTRY = PinqloqLogEntry(
            event = "opened",
            level = PinqloqLevel.Information,
            metadata = mapOf("userId" to "42"),
            detail = emptyMap(),
            platformInfo = PinqloqPlatformInfo(
                appVersionName = "1.2.3",
                deviceUid = "device-uid",
                deviceName = "Pixel"
            ),
            occurredAt = Instant.fromEpochSeconds(1_700_000_000)
        )
    }
}
