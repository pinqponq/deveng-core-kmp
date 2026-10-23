package core.pinqloq.data

import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.model.PinqloqLogEntry
import core.pinqloq.domain.repository.PinqloqLogRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private const val SECRET_KEY_HEADER = "X-Secret-Key"

class PinqloqLogRepositoryImpl(
    private val clientOptions: PinqloqClientOptions,
    private val requestSender: PinqloqRequestSender
) : PinqloqLogRepository {
    override suspend fun saveLog(entry: PinqloqLogEntry) {
        requestSender.post(
            endpoint = clientOptions.endpoint,
            headers = buildHeaders(),
            body = clientOptions.requestMapper.mapEntry(entry)
        )
    }

    override suspend fun saveLogs(entries: List<PinqloqLogEntry>) {
        val batchEndpoint = requireNotNull(clientOptions.batchEndpoint) {
            "PinqloqClientOptions.batchEndpoint is required to send a batch"
        }
        requestSender.post(
            endpoint = batchEndpoint,
            headers = buildHeaders(),
            body = buildBatchBody(entries)
        )
    }

    private fun buildHeaders(): Map<String, String> {
        val secretKey = clientOptions.secretKey ?: return emptyMap()
        return mapOf(SECRET_KEY_HEADER to secretKey)
    }

    private fun buildBatchBody(entries: List<PinqloqLogEntry>): JsonObject = buildJsonObject {
        clientOptions.collectionName?.let { collectionName ->
            put("collectionName", JsonPrimitive(collectionName))
        }
        put("logs", JsonArray(entries.map(clientOptions.requestMapper::mapEntry)))
    }
}
