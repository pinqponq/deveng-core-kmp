package core.session.data.remote.model.mapper

import core.session.data.remote.model.response.AuthTokensResponse
import core.session.domain.model.AuthTokens

internal class AuthTokensMapper {
    fun mapToDomain(response: AuthTokensResponse): AuthTokens {
        return AuthTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
    }
}
