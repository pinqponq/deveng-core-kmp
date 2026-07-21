package core.util.multiplatform

/**
 * A last-known location is accepted only if it is younger than this threshold.
 * Anything older triggers a fresh location request so callers never receive a stale fix.
 */
internal const val LOCATION_FRESHNESS_THRESHOLD_MS = 60_000L

/**
 * Upper bound on how long to wait for a fresh fix before falling back to the last-known location.
 */
internal const val LOCATION_LIVE_TIMEOUT_MS = 5_000L

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class MultiPlatformUtils {
    fun dialPhoneNumber(phoneNumber: String): Boolean
    fun copyToClipBoard(text: String)
    fun openMapsWithLocation(latitude: Double, longitude: Double)
    fun openUrl(url: String)
    fun getPlatformConfig(): PlatformConfig
    suspend fun getCurrentLocation(): Pair<Double, Double>?
    fun shareText(text: String)
}