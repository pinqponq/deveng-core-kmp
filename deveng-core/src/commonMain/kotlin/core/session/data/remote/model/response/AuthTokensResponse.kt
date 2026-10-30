package core.session.data.remote.model.response

import kotlinx.serialization.Serializable

@Serializable
internal data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String? = null,
)
