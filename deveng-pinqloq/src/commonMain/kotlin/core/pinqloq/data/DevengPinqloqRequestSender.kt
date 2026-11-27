package core.pinqloq.data

import kotlinx.serialization.json.JsonObject
import networking.DevengNetworkingModule
import networking.util.DevengHttpMethod

class DevengPinqloqRequestSender(
    private val networkingModule: DevengNetworkingModule
) : PinqloqRequestSender {
    override suspend fun post(endpoint: String, headers: Map<String, String>, body: JsonObject) {
        networkingModule.sendRequest<JsonObject, Unit>(
            endpoint = endpoint,
            requestMethod = DevengHttpMethod.POST,
            requestBody = body,
            requestHeaders = headers
        )
    }
}
