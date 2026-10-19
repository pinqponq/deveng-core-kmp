package core.session.data.store

import com.russhwolf.settings.Settings
import core.session.domain.store.AuthCredentialsStorage
import core.session.domain.store.AuthSessionStorageKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthCredentialsStorageImpl(
    private val settings: Settings,
    private val storageKeys: AuthSessionStorageKeys = AuthSessionStorageKeys(),
) : AuthCredentialsStorage {

    private val _isLoggedIn = MutableStateFlow(settings.getBoolean(storageKeys.isLoggedInKey, false))

    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override fun getAccessToken(): String? {
        return settings.getStringOrNull(storageKeys.accessTokenKey)
    }

    override fun setAccessToken(accessToken: String) {
        settings.putString(storageKeys.accessTokenKey, accessToken)
    }

    override fun getRefreshToken(): String? {
        return settings.getStringOrNull(storageKeys.refreshTokenKey)
    }

    override fun setRefreshToken(refreshToken: String?) {
        if (refreshToken == null) {
            settings.remove(storageKeys.refreshTokenKey)
        } else {
            settings.putString(storageKeys.refreshTokenKey, refreshToken)
        }
    }

    override fun setLoggedIn(isLoggedIn: Boolean) {
        settings.putBoolean(storageKeys.isLoggedInKey, isLoggedIn)
        _isLoggedIn.value = isLoggedIn
    }

    // Only the session's own keys: the app may keep other data in the same Settings.
    override fun clear() {
        settings.remove(storageKeys.accessTokenKey)
        settings.remove(storageKeys.refreshTokenKey)
        settings.remove(storageKeys.isLoggedInKey)
        _isLoggedIn.value = false
    }
}
