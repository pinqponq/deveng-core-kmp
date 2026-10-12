package core.attribution.fake

import core.attribution.domain.model.InstallAttribution
import core.attribution.domain.repository.AttributionRepository

class FakeAttributionRepository(
    private val attribution: InstallAttribution? = null,
    private val failure: Exception? = null,
) : AttributionRepository {
    override suspend fun resolveAttribution(attributionToken: String): InstallAttribution? {
        failure?.let { exception -> throw exception }
        return attribution
    }
}
