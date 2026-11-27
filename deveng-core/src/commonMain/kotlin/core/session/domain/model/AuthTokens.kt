package core.session.domain.model

/**
 * The token pair the backend returns on sign-in and on renewal. [refreshToken] is null when the
 * backend did not rotate it.
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
)
