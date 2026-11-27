package core.attribution.data

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import core.attribution.domain.AttributionTokenProvider
import core.util.CustomLogger.log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reads the install referrer string from the Google Play Install Referrer service. A response code
 * other than OK means the device has no Play Store or the service is unavailable, in which case
 * there is simply no referrer to report.
 */
class GooglePlayInstallReferrerTokenProvider(
    private val applicationContext: Context,
) : AttributionTokenProvider {
    override suspend fun fetchAttributionToken(): String? {
        val installReferrerClient = InstallReferrerClient.newBuilder(applicationContext).build()

        try {
            val responseCode = connect(installReferrerClient)
            if (responseCode != InstallReferrerResponse.OK) {
                log("GooglePlayInstallReferrerTokenProvider no install referrer: response code $responseCode")
                return null
            }

            return installReferrerClient.installReferrer.installReferrer
        } finally {
            installReferrerClient.endConnection()
        }
    }

    private suspend fun connect(installReferrerClient: InstallReferrerClient): Int =
        suspendCancellableCoroutine { continuation ->
            installReferrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (continuation.isActive) continuation.resume(responseCode)
                }

                override fun onInstallReferrerServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(InstallReferrerResponse.SERVICE_DISCONNECTED)
                }
            })
        }
}
