package core.session.data.remote.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshTokenRequest(
    val refreshToken: String,
)
