package core.util.multiplatform

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class MultiPlatformUtils(private val context: Context) {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    actual fun dialPhoneNumber(phoneNumber: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$phoneNumber".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val packageManager = context.packageManager
        val canHandleIntent = intent.resolveActivity(packageManager) != null

        return if (canHandleIntent) {
            context.startActivity(intent)
            true
        } else {
            copyToClipBoard(phoneNumber)
            false
        }
    }

    actual fun copyToClipBoard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Phone number", text)
        clipboard.setPrimaryClip(clip)
    }

    actual fun openMapsWithLocation(latitude: Double, longitude: Double) {
        val uriString = "geo:$latitude,$longitude?q=$latitude,$longitude"
        val mapIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uriString.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(mapIntent)
    }

    actual fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    @SuppressLint("HardwareIds", "PackageManagerGetSignatures")
    actual fun getPlatformConfig(): PlatformConfig {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "Unknown"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

        return PlatformConfig(
            platform = Platform.ANDROID,
            systemLanguage = android.content.res.Resources.getSystem().configuration.locales[0].language,
            uuid = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
            deviceName = "${Build.MANUFACTURER} - ${Build.VERSION.SDK_INT} - ${Build.MODEL}",
            packageVersionName = "$versionName - $versionCode",
        )
    }

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocationPermission && !hasCoarseLocationPermission) {
            return null
        }

        val lastKnownLocation = getLastKnownLocation()
        if (lastKnownLocation != null &&
            lastKnownLocation.ageMillis() <= LOCATION_FRESHNESS_THRESHOLD_MS
        ) {
            return Pair(lastKnownLocation.latitude, lastKnownLocation.longitude)
        }

        val freshLocation = withTimeoutOrNull(LOCATION_LIVE_TIMEOUT_MS) {
            requestFreshLocation(hasFineLocationPermission = hasFineLocationPermission)
        }
        if (freshLocation != null) {
            return Pair(freshLocation.latitude, freshLocation.longitude)
        }

        return lastKnownLocation?.let { Pair(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        hasFineLocationPermission: Boolean,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val providers = buildList {
            if (hasFineLocationPermission &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        if (providers.isEmpty()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }

            override fun onProviderDisabled(provider: String) {}

            override fun onProviderEnabled(provider: String) {}
        }

        continuation.invokeOnCancellation {
            locationManager.removeUpdates(locationListener)
        }

        providers.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                0L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun Location.ageMillis(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L

    actual fun shareText(text: String) {
        if (text.isBlank()) return

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val chooser = Intent.createChooser(sendIntent, "Share via").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val providers = mutableListOf<String>()
        if (hasFineLocation && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER)
        }

        var bestLocation: Location? = null
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                    bestLocation = location
                }
            }
        }
        return bestLocation
    }
}