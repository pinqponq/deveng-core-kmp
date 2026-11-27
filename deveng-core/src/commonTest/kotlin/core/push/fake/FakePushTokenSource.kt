package core.push.fake

import core.push.domain.PushTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePushTokenSource(initialToken: String? = null) : PushTokenSource {
    val token = MutableStateFlow(initialToken)

    override fun observeToken(): Flow<String?> = token
}
