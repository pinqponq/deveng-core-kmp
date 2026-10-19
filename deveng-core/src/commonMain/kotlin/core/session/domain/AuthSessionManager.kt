package core.session.domain

import co.touchlab.kermit.Logger
import core.session.domain.model.AuthTokens
import core.session.domain.store.AuthCredentialsStorage
import core.util.decodeJwtClaims
import core.util.jwtClaimsJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The sign-in session of an app: stores the token pair, hands every new access token to the HTTP
 * client, renews the tokens when the backend rejects the access token, and clears everything on sign-out.
 *
 * Keep one instance per app. To renew on HTTP 401 with deveng-networking-kmp, configure the client with
 * `sessionRefresher = DevengSessionRefresher(authSessionManager::refreshSession)` and start it with
 * `token = authSessionManager.getAccessToken().orEmpty()`.
 *
 * Product work (billing sync, push unregister, the app's own stores) runs in the sign-in and sign-out
 * hooks; core never names a product store.
 */
class AuthSessionManager(
    private val authSessionOptions: AuthSessionOptions,
    private val authCredentialsStorage: AuthCredentialsStorage,
) {
    private val logger = Logger.withTag(authSessionOptions.logTag)
    private val sessionGeneration = SessionGeneration()

    val isLoggedIn: StateFlow<Boolean> = authCredentialsStorage.isLoggedIn

    /** Null when no user is signed in, also while a sign-out is still revoking the refresh token. */
    fun getAccessToken(): String? {
        if (!authCredentialsStorage.isLoggedIn.value) return null
        return authCredentialsStorage.getAccessToken()
    }

    /**
     * Decodes the claims of the stored access token, or returns null when no user is signed in.
     * The signature is not verified, so use the result only to read claims on the client.
     *
     * @throws IllegalArgumentException when the stored token is not a JWT or its payload does not match [T].
     */
    inline fun <reified T> decodeClaims(json: Json = jwtClaimsJson): T? {
        val accessToken = getAccessToken() ?: return null
        return decodeJwtClaims<T>(token = accessToken, json = json)
    }

    /**
     * Stores the token pair and marks the user signed in, then runs [onSignedIn]. The access token reaches
     * the HTTP client before [isLoggedIn] turns true, so the first request of a signed-in screen is
     * authenticated. A failure of [onSignedIn] propagates; the session stays signed in.
     */
    suspend fun signIn(accessToken: String, refreshToken: String?, onSignedIn: suspend () -> Unit = {}) {
        sessionGeneration.advance {
            authCredentialsStorage.setAccessToken(accessToken)
            authCredentialsStorage.setRefreshToken(refreshToken)
            authSessionOptions.onAccessTokenChanged(accessToken)
            authCredentialsStorage.setLoggedIn(true)
        }
        onSignedIn()
    }

    /**
     * Ends the session on this device. [beforeSignOut] runs first, while requests are still authenticated
     * (push unregister, ending a chat session). Then [isLoggedIn] turns false, the refresh token is revoked,
     * the stored tokens are cleared and [onCleared] runs (the app's own stores, billing sync).
     *
     * Sign-out always finishes locally: a failing hook or revoke is only logged, and everything after
     * [isLoggedIn] turns false still runs when the caller's scope is cancelled, because the signed-in
     * screen that started the sign-out usually closes at that moment. When a new sign-in lands while the
     * revoke is running, the new session is kept and [onCleared] does not run. Only the first of two
     * overlapping sign-outs revokes the refresh token.
     */
    suspend fun signOut(beforeSignOut: suspend () -> Unit = {}, onCleared: suspend () -> Unit = {}) {
        runCatchingNonCancellation { beforeSignOut() }
            .onFailure { logger.w(it) { "A before-sign-out hook failed." } }

        val signOutStart = sessionGeneration.advance { newGeneration ->
            val refreshToken = authCredentialsStorage.getRefreshToken()
            authCredentialsStorage.setLoggedIn(false)
            authCredentialsStorage.setRefreshToken(null)
            SignOutStart(generation = newGeneration, refreshToken = refreshToken)
        }

        withContext(NonCancellable) {
            revokeRefreshToken(signOutStart.refreshToken)
            clearSignedOutSession(signOutStart.generation, onCleared)
        }
    }

    /**
     * Exchanges the stored refresh token for a new token pair. Returns true when the new tokens were stored.
     *
     * Returns false without a request when no user is signed in or no refresh token is stored, and false
     * when the renewal fails or the session was ended or replaced while it ran; its tokens are then dropped.
     * A renewal without a new refresh token keeps the stored one.
     */
    suspend fun refreshSession(): Boolean {
        val refreshStart = sessionGeneration.withCurrent { generation ->
            val storedRefreshToken = authCredentialsStorage.getRefreshToken()
            if (!authCredentialsStorage.isLoggedIn.value || storedRefreshToken.isNullOrBlank()) {
                return@withCurrent null
            }

            RefreshStart(generation = generation, refreshToken = storedRefreshToken)
        } ?: return false

        val renewedTokens = runCatchingNonCancellation { authSessionOptions.renewTokens(refreshStart.refreshToken) }
            .onFailure { logger.w(it) { "Session refresh failed." } }
            .getOrNull() ?: return false

        return sessionGeneration.runIfUnchanged(refreshStart.generation) {
            storeRenewedTokens(renewedTokens)
        }
    }

    private fun storeRenewedTokens(renewedTokens: AuthTokens) {
        authCredentialsStorage.setAccessToken(renewedTokens.accessToken)
        renewedTokens.refreshToken?.let { rotatedRefreshToken ->
            authCredentialsStorage.setRefreshToken(rotatedRefreshToken)
        }
        authSessionOptions.onAccessTokenChanged(renewedTokens.accessToken)
    }

    private suspend fun clearSignedOutSession(signedOutGeneration: Long, onCleared: suspend () -> Unit) {
        val isCleared = sessionGeneration.runIfUnchanged(signedOutGeneration) {
            authCredentialsStorage.clear()
            authSessionOptions.onAccessTokenChanged(null)
        }
        if (!isCleared) return

        runCatchingNonCancellation { onCleared() }
            .onFailure { logger.w(it) { "An after-sign-out hook failed." } }
    }

    private suspend fun revokeRefreshToken(refreshToken: String?) {
        val revokeRefreshToken = authSessionOptions.revokeRefreshToken ?: return
        if (refreshToken.isNullOrBlank()) return

        runCatchingNonCancellation { revokeRefreshToken(refreshToken) }
            .onFailure { logger.w(it) { "Refresh token revoke failed during sign-out." } }
    }

    private inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }

    private class SignOutStart(val generation: Long, val refreshToken: String?)

    private class RefreshStart(val generation: Long, val refreshToken: String)
}
