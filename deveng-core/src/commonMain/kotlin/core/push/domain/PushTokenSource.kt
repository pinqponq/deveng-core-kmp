package core.push.domain

import kotlinx.coroutines.flow.Flow

/**
 * Supplies the device's push token. The flow may emit null while no token is known yet; a flow that
 * completes without a token means the platform has none.
 */
interface PushTokenSource {
    fun observeToken(): Flow<String?>
}
