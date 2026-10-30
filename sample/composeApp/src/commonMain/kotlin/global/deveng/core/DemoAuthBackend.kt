package global.deveng.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import networking.DevengNetworkingConfig
import networking.DevengNetworkingModule
import kotlin.time.Duration.Companion.seconds

private const val DEMO_BASE_URL = "https://demo.deveng.test/"
private val DEMO_RENEW_DURATION = 2.seconds

internal class DemoAuthRequest(val method: String, val endpoint: String, val body: String)

// The sample has no backend, so a mock engine answers the renew request after a delay and the revoke request with 204.
internal fun createDemoAuthBackend(
    refreshEndpoint: String,
    onRequest: (DemoAuthRequest) -> Unit,
    renewedTokensBody: () -> String,
): DevengNetworkingModule {
    val mockEngine = MockEngine { request ->
        val endpoint = request.url.toString().removePrefix(DEMO_BASE_URL)
        onRequest(DemoAuthRequest(request.method.value, endpoint, request.body.toByteArray().decodeToString()))

        if (endpoint == refreshEndpoint) {
            delay(DEMO_RENEW_DURATION)
            respond(
                content = renewedTokensBody(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        } else {
            respond(content = "", status = HttpStatusCode.NoContent)
        }
    }

    val devengNetworkingModule = DevengNetworkingModule()
    devengNetworkingModule.initDevengNetworkingModule(
        restBaseUrl = DEMO_BASE_URL,
        config = DevengNetworkingConfig(loggingEnabled = false),
    )
    devengNetworkingModule.client?.close()
    devengNetworkingModule.client = HttpClient(mockEngine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    return devengNetworkingModule
}
