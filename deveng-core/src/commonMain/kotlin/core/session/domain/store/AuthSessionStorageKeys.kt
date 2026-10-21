package core.session.domain.store

/**
 * The `Settings` keys of the stored session. An app that moves its own session store to core passes
 * its existing keys, so signed-in users stay signed in after the update.
 */
data class AuthSessionStorageKeys(
    val accessTokenKey: String = "auth_session_access_token",
    val refreshTokenKey: String = "auth_session_refresh_token",
    val isLoggedInKey: String = "auth_session_is_logged_in",
)
