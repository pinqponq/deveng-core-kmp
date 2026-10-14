package core.util.multiplatform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSBundle
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
import platform.posix.uname
import platform.posix.utsname
import kotlin.coroutines.resume

private const val BUNDLE_SHORT_VERSION_KEY = "CFBundleShortVersionString"
private const val BUNDLE_VERSION_KEY = "CFBundleVersion"
private const val UNKNOWN_VALUE = "Unknown"

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
            deviceName = "${UIDevice.currentDevice.model} - ${UIDevice.currentDevice.systemVersion} - ${readHardwareModelIdentifier()}",
            packageVersionName = readPackageVersionName()
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

        IosShareSheetPresenter.presentOnMainQueue(activityItems = listOf(text))
    }
}

private fun readPackageVersionName(): String {
    val bundle = NSBundle.mainBundle
    val shortVersion = bundle.objectForInfoDictionaryKey(BUNDLE_SHORT_VERSION_KEY) as? String
    val buildNumber = bundle.objectForInfoDictionaryKey(BUNDLE_VERSION_KEY) as? String

    return "${shortVersion ?: UNKNOWN_VALUE} - ${buildNumber ?: UNKNOWN_VALUE}"
}

/**
 * [UIDevice.model] only ever reports the product family ("iPhone"), and since iOS 16 [UIDevice.name]
 * falls back to that same family name unless the app holds the user-assigned device name entitlement.
 * The hardware identifier ("iPhone17,2") is therefore the only model detail available to us.
 */
@OptIn(ExperimentalForeignApi::class)
private fun readHardwareModelIdentifier(): String = memScoped {
    val systemInfo = alloc<utsname>()
    if (uname(systemInfo.ptr) != 0) {
        return@memScoped UNKNOWN_VALUE
    }

    systemInfo.machine.toKString()
}

private fun CLLocation.ageMillis(): Long {
    val fixTimestamp = timestamp ?: return Long.MAX_VALUE
    return ((NSDate().timeIntervalSince1970 - fixTimestamp.timeIntervalSince1970) * 1000.0).toLong()
}
