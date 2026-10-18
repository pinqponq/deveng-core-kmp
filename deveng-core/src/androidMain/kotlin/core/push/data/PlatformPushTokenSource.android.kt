package core.push.data

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import core.push.domain.PushTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

private const val LOG_TAG = "PlatformPushTokenSource"

actual object PlatformPushTokenSource : PushTokenSource {
    override fun observeToken(): Flow<String?> = flow {
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (tokenFailure: Exception) {
            Logger.withTag(LOG_TAG).e(tokenFailure) { "Firebase Cloud Messaging token could not be read." }
            null
        }

        emit(token)
    }
}
