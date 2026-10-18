package core.push.domain

import core.util.multiplatform.PlatformConfig
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val TOKEN_LOOKUP_TIMEOUT_FOR_UNREGISTER = 5.seconds

internal class PushTokenRegistration(
    private val pushTokenSource: PushTokenSource,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val pushTokenUnregistrar: PushTokenUnregistrar,
    private val platformConfigProvider: () -> PlatformConfig,
) {
    // Registration is triggered from several places, often near-concurrently during app start. The
    // last token actually sent turns a repeat call with an unchanged token into a no-op while a
    // rotated token still gets through; the mutex keeps two concurrent calls from both passing the
    // check. The guard is process-scoped on purpose: a fresh launch registers once again and
    // recovers if the backend ever loses the mapping.
    private val registrationMutex = Mutex()
    private var lastRegisteredToken: String? = null

    suspend fun registerCurrentToken() {
        val token = pushTokenSource.observeToken().filterNotNull().firstOrNull() ?: return
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        registrationMutex.withLock {
            if (token == lastRegisteredToken) return

            val platformConfig = platformConfigProvider()
            pushTokenRegistrar.register(
                token = token,
                platform = platformConfig.platform,
                deviceIdentifier = platformConfig.uuid
            )
            lastRegisteredToken = token
        }
    }

    suspend fun unregisterToken() {
        val token = registrationMutex.withLock { lastRegisteredToken }
            ?: findCurrentTokenForUnregister()
            ?: return

        try {
            pushTokenUnregistrar.unregister(
                token = token,
                deviceIdentifier = platformConfigProvider().uuid
            )
        } finally {
            // Dropped even when the call fails: the session it belonged to is ending either way, and
            // a stale token here would make the next sign-in skip its own registration.
            registrationMutex.withLock {
                if (lastRegisteredToken == token) lastRegisteredToken = null
            }
        }
    }

    private suspend fun findCurrentTokenForUnregister(): String? {
        return withTimeoutOrNull(TOKEN_LOOKUP_TIMEOUT_FOR_UNREGISTER) {
            pushTokenSource.observeToken().filterNotNull().firstOrNull()
        }
    }
}
