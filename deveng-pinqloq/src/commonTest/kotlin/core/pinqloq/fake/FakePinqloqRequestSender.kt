package core.pinqloq.fake

import core.pinqloq.data.PinqloqRequestSender
import kotlinx.serialization.json.JsonObject

class FakePinqloqRequestSender : PinqloqRequestSender {
    data class SentRequest(
        val endpoint: String,
        val headers: Map<String, String>,
        val body: JsonObject
    )

    val sentRequests = mutableListOf<SentRequest>()

    override suspend fun post(endpoint: String, headers: Map<String, String>, body: JsonObject) {
        sentRequests += SentRequest(endpoint, headers, body)
    }
}
