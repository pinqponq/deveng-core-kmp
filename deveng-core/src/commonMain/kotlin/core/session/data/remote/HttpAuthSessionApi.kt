package core.session.data.remote

import core.session.data.remote.model.mapper.AuthTokensMapper
import core.session.data.remote.model.request.RefreshTokenRequest
import core.session.data.remote.model.response.AuthTokensResponse
import core.session.domain.model.AuthTokens
import networking.DevengNetworkingModule
import networking.util.DevengHttpMethod

/**
 * The renew and revoke requests of [core.session.domain.AuthSessionManager], sent through the app's
 * [DevengNetworkingModule]. Both endpoints take the body `{"refreshToken": "…"}`. The renew endpoint answers
 * `{"accessToken": "…", "refreshToken": "…"}`, where `refreshToken` is optional. The revoke endpoint may
 * answer any 2xx status, with or without a body.
 *
 * Pass both functions to the session options:
 * `AuthSessionOptions(renewTokens = authSessionApi::renewTokens, revokeRefreshToken = authSessionApi::revokeRefreshToken, …)`.
 *
 * @param refreshEndpoint the renew path relative to the client's base URL, for example `auth/refresh`.
 * @param logoutEndpoint the revoke path relative to the client's base URL, for example `auth/logout`.
 */
class HttpAuthSessionApi(
    private val devengNetworkingModule: DevengNetworkingModule,
    private val refreshEndpoint: String,
    private val logoutEndpoint: String,
) {
    private val authTokensMapper = AuthTokensMapper()

    init {
        require(refreshEndpoint.isNotBlank()) { "refreshEndpoint must not be blank" }
        require(logoutEndpoint.isNotBlank()) { "logoutEndpoint must not be blank" }
    }

    /** Throws the client's `DevengException` when the backend answers with a non-2xx status. */
    suspend fun renewTokens(refreshToken: String): AuthTokens {
        val response = devengNetworkingModule.sendRequest<RefreshTokenRequest, AuthTokensResponse>(
            endpoint = refreshEndpoint,
            requestMethod = DevengHttpMethod.POST,
            requestBody = RefreshTokenRequest(refreshToken = refreshToken),
        )

        return authTokensMapper.mapToDomain(response = response)
    }

    /** Throws the client's `DevengException` when the backend answers with a non-2xx status. */
    suspend fun revokeRefreshToken(refreshToken: String) {
        devengNetworkingModule.sendRequestForHttpResponse<RefreshTokenRequest>(
            endpoint = logoutEndpoint,
            requestMethod = DevengHttpMethod.POST,
            requestBody = RefreshTokenRequest(refreshToken = refreshToken),
        )
    }
}
