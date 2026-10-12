package core.attribution.domain

/**
 * Platform seam for the raw install-attribution token: the AdServices attribution token on iOS
 * ([platform.AdServices.AAAttribution]), the Google Play install referrer string on Android; every
 * other platform is a no-op.
 */
interface AttributionTokenProvider {
    suspend fun fetchAttributionToken(): String?
}
