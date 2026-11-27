package core.push.domain

/**
 * Removes the device's push token from the app's backend, so a signed-out device stops receiving
 * the previous user's notifications.
 */
fun interface PushTokenUnregistrar {
    suspend fun unregister(token: String, deviceIdentifier: String)
}
