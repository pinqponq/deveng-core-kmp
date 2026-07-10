package core.util.multiplatform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.preferredLanguages
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIPasteboard
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class MultiPlatformUtils {

    private val locationManager = CLLocationManager()
    actual fun dialPhoneNumber(phoneNumber: String): Boolean {
        val url = NSURL.URLWithString("tel:$phoneNumber")
        return if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(
                url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null
            )
            true
        } else {
            copyToClipBoard(phoneNumber)
            false
        }
    }

    actual fun copyToClipBoard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun openMapsWithLocation(latitude: Double, longitude: Double) {
        val urlString = "http://maps.apple.com/?ll=$latitude,$longitude"
        val url = NSURL(string = urlString)

        UIApplication.sharedApplication.openURL(url)
    }

    actual fun openUrl(url: String) {
        NSURL.URLWithString(url)?.let { nsUrl ->
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }

    actual fun getPlatformConfig(): PlatformConfig {
        val languageCode = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
        return PlatformConfig(
            platform = Platform.NATIVE,
            systemLanguage = languageCode.substringBefore("-"),
            uuid = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_ios_uid",
            deviceName = "${UIDevice.currentDevice.name} - ${UIDevice.currentDevice.systemVersion} - ${UIDevice.currentDevice.model}",
            packageVersionName = "0.0.0"
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val authStatus = CLLocationManager.authorizationStatus()

        if (authStatus != kCLAuthorizationStatusAuthorizedWhenInUse &&
            authStatus != kCLAuthorizationStatusAuthorizedAlways) {
            return null
        }

        val freshLocation = withTimeoutOrNull(LOCATION_LIVE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                        val location = didUpdateLocations.lastOrNull() as? CLLocation
                        if (location == null ||
                            location.ageMillis() > LOCATION_FRESHNESS_THRESHOLD_MS
                        ) {
                            return
                        }
                        manager.stopUpdatingLocation()
                        manager.delegate = null
                        val coordinates = location.coordinate.useContents {
                            Pair(latitude, longitude)
                        }
                        continuation.resume(coordinates)
                    }

                    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                        manager.stopUpdatingLocation()
                        manager.delegate = null
                        continuation.resume(null)
                    }
                }

                locationManager.delegate = delegate
                locationManager.desiredAccuracy = kCLLocationAccuracyBest
                locationManager.startUpdatingLocation()

                continuation.invokeOnCancellation {
                    locationManager.stopUpdatingLocation()
                    locationManager.delegate = null
                }
            }
        }
        if (freshLocation != null) {
            return freshLocation
        }

        return locationManager.location?.coordinate?.useContents {
            Pair(latitude, longitude)
        }
    }

    actual fun shareText(text: String) {
        if (text.isBlank()) return

        val activityController = platform.UIKit.UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, animated = true, completion = null)
    }
}

private fun CLLocation.ageMillis(): Long {
    val fixTimestamp = timestamp ?: return Long.MAX_VALUE
    return ((NSDate().timeIntervalSince1970 - fixTimestamp.timeIntervalSince1970) * 1000.0).toLong()
}
