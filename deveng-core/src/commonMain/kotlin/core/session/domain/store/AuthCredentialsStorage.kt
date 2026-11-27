package core.session.domain.store

import kotlinx.coroutines.flow.StateFlow

interface AuthCredentialsStorage {
    val isLoggedIn: StateFlow<Boolean>
    fun getAccessToken(): String?
    fun setAccessToken(accessToken: String)
    fun getRefreshToken(): String?

    /** Null removes the stored refresh token. */
    fun setRefreshToken(refreshToken: String?)
    fun setLoggedIn(isLoggedIn: Boolean)
    fun clear()
}
