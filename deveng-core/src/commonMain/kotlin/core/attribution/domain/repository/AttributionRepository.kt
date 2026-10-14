package core.attribution.domain.repository

import core.attribution.domain.model.InstallAttribution

/**
 * Resolves a raw attribution token into [InstallAttribution]. Returns null when the token cannot
 * be resolved into an attribution result.
 */
interface AttributionRepository {
    suspend fun resolveAttribution(attributionToken: String): InstallAttribution?
}
