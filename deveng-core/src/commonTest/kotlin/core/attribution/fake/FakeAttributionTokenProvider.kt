package core.attribution.fake

import core.attribution.domain.AttributionTokenProvider

class FakeAttributionTokenProvider(
    private val attributionToken: String?,
) : AttributionTokenProvider {
    override suspend fun fetchAttributionToken(): String? = attributionToken
}
