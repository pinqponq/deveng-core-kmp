package core.pinqloq.data

import kotlinx.serialization.json.JsonObject

fun interface PinqloqRequestSender {
    suspend fun post(endpoint: String, headers: Map<String, String>, body: JsonObject)
}
