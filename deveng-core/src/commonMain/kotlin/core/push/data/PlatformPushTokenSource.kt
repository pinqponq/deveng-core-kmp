package core.push.data

import core.push.domain.PushTokenSource
import kotlinx.coroutines.flow.Flow

/**
 * The push token of the running platform: Firebase Cloud Messaging on Android, the token the app
 * hands over through [IosPushTokenBridge] on iOS, and none on desktop and web.
 */
expect object PlatformPushTokenSource : PushTokenSource {
    override fun observeToken(): Flow<String?>
}
