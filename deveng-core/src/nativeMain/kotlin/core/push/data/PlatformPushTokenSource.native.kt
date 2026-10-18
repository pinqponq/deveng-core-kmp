package core.push.data

import core.push.domain.PushTokenSource
import kotlinx.coroutines.flow.Flow

actual object PlatformPushTokenSource : PushTokenSource {
    override fun observeToken(): Flow<String?> = IosPushTokenBridge.token
}
