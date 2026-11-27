package core.push.domain

import co.touchlab.kermit.Logger
import core.util.multiplatform.PlatformConfig
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val LOG_TAG = "PushTokenRegistration"

// The token can arrive late (on iOS only after APNs answered); a later token still reaches the backend
// through PushNotifications.handleNewToken, so the caller does not wait for it here.
private val TOKEN_LOOKUP_TIMEOUT_FOR_REGISTER = 10.seconds
private val TOKEN_LOOKUP_TIMEOUT_FOR_UNREGISTER = 5.seconds

internal class PushTokenRegistration(
    private val pushTokenSource: PushTokenSource,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val pushTokenUnregistrar: PushTokenUnregistrar,
    private val platformConfigProvider: () -> PlatformConfig,
) {
    private val logger = Logger.withTag(LOG_TAG)

    // Registration is triggered from several places, often near-concurrently during app start. The
    // last token actually sent turns a repeat call with an unchanged token into a no-op while a
    // rotated token still gets through; the mutex keeps two concurrent calls from both passing the
    // check. The guard is process-scoped on purpose: a fresh launch registers once again and
    // recovers if the backend ever loses the mapping.
    private val registrationMutex = Mutex()
    private var lastRegisteredToken: String? = null

    suspend fun registerCurrentToken() {
        val token = findCurrentToken(timeout = TOKEN_LOOKUP_TIMEOUT_FOR_REGISTER)
        if (token == null) {
            logger.w { "No push token available; registration skipped." }
            return
        }

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
            ?: findCurrentToken(timeout = TOKEN_LOOKUP_TIMEOUT_FOR_UNREGISTER)
        if (token == null) {
            logger.w { "No push token available; the device could not be unregistered." }
            return
        }

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

    private suspend fun findCurrentToken(timeout: Duration): String? {
        return withTimeoutOrNull(timeout) {
            pushTokenSource.observeToken().filterNotNull().firstOrNull()
        }
    }
}
