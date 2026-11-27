package core.attribution.data

import core.attribution.domain.AttributionTokenProvider
import core.util.CustomLogger.log
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AdServices.AAAttribution
import platform.Foundation.NSError

/**
 * Reads the Apple Search Ads attribution token from the AdServices framework. Requires iOS 14.3+
 * and the AdServices.framework linked in the Xcode target. Kotlin/Native does not map the
 * Objective-C NSError out-parameter to an exception, so the error pointer is checked explicitly;
 * an error means the device has no attribution or the API is unavailable, in which case there is
 * simply no token to report.
 */
class IosAdServicesAttributionTokenProvider : AttributionTokenProvider {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun fetchAttributionToken(): String? = memScoped {
        val errorPointer = alloc<ObjCObjectVar<NSError?>>()
        val attributionToken = AAAttribution.attributionTokenWithError(errorPointer.ptr)
        val attributionError = errorPointer.value
        if (attributionError != null) {
            log("IosAdServicesAttributionTokenProvider no attribution token: ${attributionError.localizedDescription}")
            return@memScoped null
        }

        attributionToken
    }
}
