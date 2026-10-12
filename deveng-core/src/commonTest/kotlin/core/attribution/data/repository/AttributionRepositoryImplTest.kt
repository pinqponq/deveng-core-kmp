package core.attribution.data.repository

import core.attribution.data.datasource.remote.AppleAdServicesService
import core.attribution.data.datasource.remote.model.mapper.AdServicesAttributionMapper
import core.attribution.domain.model.InstallAttribution
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AttributionRepositoryImplTest {

    private val requestBodies = mutableListOf<String>()
    private val requestContentTypes = mutableListOf<String?>()
    private val requestUrls = mutableListOf<String>()
    private val requestMethods = mutableListOf<HttpMethod>()

    @Test
    fun resolveAttribution_withAttributedResponse_mapsAppleSearchAdsFields() = runTest {
        val repository = createRepository(responses = listOf(HttpStatusCode.OK to ATTRIBUTED_RESPONSE))

        val attribution = repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN)

        assertEquals(EXPECTED_ATTRIBUTION, attribution)
        assertEquals(listOf(ADSERVICES_ENDPOINT), requestUrls)
        assertEquals(listOf(HttpMethod.Post), requestMethods)
        assertEquals(listOf(ATTRIBUTION_TOKEN), requestBodies)
        assertEquals(listOf<String?>("text/plain"), requestContentTypes.map { it?.substringBefore(";") })
    }

    @Test
    fun resolveAttribution_withClientDefaultingToJson_stillSendsPlainTextToken() = runTest {
        val repository = createRepository(
            responses = listOf(HttpStatusCode.OK to ATTRIBUTED_RESPONSE),
            isJsonContentTypeDefault = true,
        )

        repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN)

        assertEquals(listOf<String?>("text/plain"), requestContentTypes.map { it?.substringBefore(";") })
    }

    @Test
    fun resolveAttribution_withOrganicResponse_isNotAttributed() = runTest {
        val repository = createRepository(responses = listOf(HttpStatusCode.OK to """{"attribution":false}"""))

        val attribution = repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN)

        assertEquals(false, attribution?.attributed)
        assertNull(attribution?.campaignId)
    }

    @Test
    fun resolveAttribution_whenTokenNotYetPropagated_retriesAfterDelay() = runTest {
        val repository = createRepository(
            responses = listOf(HttpStatusCode.NotFound to "", HttpStatusCode.OK to ATTRIBUTED_RESPONSE),
        )

        val attribution = repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN)

        assertEquals(EXPECTED_ATTRIBUTION, attribution)
        assertEquals(2, requestBodies.size)
        assertEquals(RETRY_DELAY_MILLIS, currentTime)
    }

    @Test
    fun resolveAttribution_whenNotFoundOnEveryAttempt_givesUpAfterThreeRequests() = runTest {
        val repository = createRepository(responses = List(MAX_ATTEMPTS + 1) { HttpStatusCode.NotFound to "" })

        val attribution = repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN)

        assertNull(attribution)
        assertEquals(MAX_ATTEMPTS, requestBodies.size)
        assertEquals(RETRY_DELAY_MILLIS * (MAX_ATTEMPTS - 1), currentTime)
    }

    @Test
    fun resolveAttribution_withServerError_returnsNullWithoutRetry() = runTest {
        val repository = createRepository(
            responses = listOf(HttpStatusCode.InternalServerError to "", HttpStatusCode.OK to ATTRIBUTED_RESPONSE),
        )

        val attribution = repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN)

        assertNull(attribution)
        assertEquals(1, requestBodies.size)
    }

    @Test
    fun resolveAttribution_withoutHttpClient_returnsNull() = runTest {
        val repository = AttributionRepositoryImpl(
            appleAdServicesService = AppleAdServicesService(httpClientProvider = { null }, json = JSON),
            adServicesAttributionMapper = AdServicesAttributionMapper(),
        )

        assertNull(repository.resolveAttribution(attributionToken = ATTRIBUTION_TOKEN))
    }

    private fun createRepository(
        responses: List<Pair<HttpStatusCode, String>>,
        isJsonContentTypeDefault: Boolean = false,
    ): AttributionRepositoryImpl {
        val pendingResponses = ArrayDeque(responses)
        val mockEngine = MockEngine { request ->
            requestUrls += request.url.toString()
            requestMethods += request.method
            requestBodies += request.body.toByteArray().decodeToString()
            requestContentTypes += (request.body as? OutgoingContent)?.contentType?.toString()
            val (status, body) = pendingResponses.removeFirst()
            respond(content = body, status = status)
        }
        val httpClient = HttpClient(mockEngine) {
            if (isJsonContentTypeDefault) {
                defaultRequest { contentType(ContentType.Application.Json) }
            }
        }

        return AttributionRepositoryImpl(
            appleAdServicesService = AppleAdServicesService(httpClientProvider = { httpClient }, json = JSON),
            adServicesAttributionMapper = AdServicesAttributionMapper(),
        )
    }

    private companion object {
        const val ADSERVICES_ENDPOINT = "https://api-adservices.apple.com/api/v1/"
        const val ATTRIBUTION_TOKEN = "apple-attribution-token"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 5_000L
        const val CLICK_DATE = "2026-10-01T10:00Z"
        const val ATTRIBUTED_RESPONSE = """{"attribution":true,"orgId":14,"campaignId":11,"conversionType":"Download",""" +
            """"adGroupId":12,"countryOrRegion":"US","keywordId":13,"adId":15,"clickDate":"$CLICK_DATE","extra":1}"""
        val JSON = Json { ignoreUnknownKeys = true }
        val EXPECTED_ATTRIBUTION = InstallAttribution(
            attributed = true,
            orgId = 14,
            campaignId = 11,
            adGroupId = 12,
            keywordId = 13,
            adId = 15,
            conversionType = "Download",
            countryOrRegion = "US",
            clickDate = CLICK_DATE,
            googleClickId = null,
            campaignSource = null,
            campaignMedium = null,
            campaignName = null,
            campaignTerm = null,
            campaignContent = null,
        )
    }
}
