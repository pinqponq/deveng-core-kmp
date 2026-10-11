package core.attribution.data.datasource.remote

import core.attribution.data.datasource.remote.model.response.AdServicesAttributionResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

private const val ADSERVICES_ENDPOINT = "https://api-adservices.apple.com/api/v1/"
private const val MAX_ATTEMPTS = 3
private const val RETRY_DELAY_MILLIS = 5_000L

/**
 * Calls Apple's AdServices API to turn an attribution token into its raw response. The call goes to
 * Apple's own host with a plain-text body and no auth, so it takes a bare Ktor client from
 * [httpClientProvider] (the app passes its networking module's client, which attaches none of the
 * app's per-request headers) instead of the app's request pipeline: that pipeline is bound to the
 * app's base URL, forces a JSON body, attaches auth headers and turns any non-2xx into an exception.
 *
 * Apple returns 404 for a short window right after install while the token propagates, so a
 * not-found response is retried a few times before giving up. Returns null when the token cannot
 * be resolved or no client is available yet.
 */
class AppleAdServicesService(
    private val httpClientProvider: () -> HttpClient?,
    private val json: Json,
) {
    suspend fun fetchAttribution(attributionToken: String): AdServicesAttributionResponse? {
        val httpClient = httpClientProvider() ?: return null

        repeat(MAX_ATTEMPTS) { attempt ->
            val response = httpClient.post(ADSERVICES_ENDPOINT) {
                contentType(ContentType.Text.Plain)
                setBody(attributionToken)
            }

            when (response.status) {
                HttpStatusCode.OK -> return json.decodeFromString(response.bodyAsText())

                HttpStatusCode.NotFound -> {
                    val isLastAttempt = attempt == MAX_ATTEMPTS - 1
                    if (isLastAttempt) return null
                    delay(RETRY_DELAY_MILLIS)
                }

                else -> return null
            }
        }

        return null
    }
}
