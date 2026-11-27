package core.push.domain

import core.util.multiplatform.Platform

/**
 * Sends the device's push token to the app's backend.
 */
fun interface PushTokenRegistrar {
    suspend fun register(token: String, platform: Platform, deviceIdentifier: String)
}
