package core.push.data

import core.push.domain.PushTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual object PlatformPushTokenSource : PushTokenSource {
    override fun observeToken(): Flow<String?> = emptyFlow()
}
