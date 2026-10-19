package core.session.domain

import core.session.domain.model.AuthTokens

private const val DEFAULT_LOG_TAG = "AuthSession"

/**
 * The per-app parts of the session lifecycle in [AuthSessionManager].
 *
 * [onAccessTokenChanged] receives every new access token, and null on sign-out; pass it to the HTTP
 * client, for example `{ accessToken -> devengNetworkingModule.setToken(accessToken.orEmpty()) }`.
 */
class AuthSessionOptions(
    val renewTokens: suspend (refreshToken: String) -> AuthTokens,
    val onAccessTokenChanged: (accessToken: String?) -> Unit,
    val revokeRefreshToken: (suspend (refreshToken: String) -> Unit)? = null,
    val logTag: String = DEFAULT_LOG_TAG,
)
