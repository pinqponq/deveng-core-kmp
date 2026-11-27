package core.attribution.data

import core.attribution.domain.AttributionTokenProvider

/**
 * Used on every platform without an install-attribution API (Desktop, Web), so these platforms
 * report no token.
 */
class NoOpAttributionTokenProvider : AttributionTokenProvider {
    override suspend fun fetchAttributionToken(): String? = null
}
