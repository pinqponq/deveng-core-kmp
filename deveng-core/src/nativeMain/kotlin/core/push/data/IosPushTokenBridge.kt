package core.push.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Receives the Firebase Cloud Messaging token from Swift. FirebaseMessaging lives in the iOS app, so
 * the app's `MessagingDelegate` calls [updateToken] with every token it receives (Firebase delivers it
 * on each launch and again after a rotation).
 */
object IosPushTokenBridge {
    private val mutableToken = MutableStateFlow<String?>(null)

    val token: StateFlow<String?> = mutableToken.asStateFlow()

    fun updateToken(token: String) {
        mutableToken.value = token
    }
}
