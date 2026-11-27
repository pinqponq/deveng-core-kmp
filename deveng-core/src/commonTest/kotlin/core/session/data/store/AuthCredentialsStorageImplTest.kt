package core.session.data.store

import com.russhwolf.settings.MapSettings
import core.session.domain.store.AuthSessionStorageKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthCredentialsStorageImplTest {

    private val settings = MapSettings()
    private val authCredentialsStorage = AuthCredentialsStorageImpl(settings = settings)

    @Test
    fun setAccessTokenAndRefreshToken_withBothTokens_readsThemBack() {
        authCredentialsStorage.setAccessToken(ACCESS_TOKEN)
        authCredentialsStorage.setRefreshToken(REFRESH_TOKEN)

        assertEquals(ACCESS_TOKEN, authCredentialsStorage.getAccessToken())
        assertEquals(REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
    }

    @Test
    fun setRefreshToken_withNull_removesStoredRefreshToken() {
        authCredentialsStorage.setRefreshToken(REFRESH_TOKEN)

        authCredentialsStorage.setRefreshToken(null)

        assertNull(authCredentialsStorage.getRefreshToken())
    }

    @Test
    fun setLoggedIn_withTrue_updatesFlow() {
        authCredentialsStorage.setLoggedIn(true)

        assertTrue(authCredentialsStorage.isLoggedIn.value)
    }

    @Test
    fun setLoggedIn_withTrue_isReadByNewInstance() {
        authCredentialsStorage.setLoggedIn(true)

        assertTrue(AuthCredentialsStorageImpl(settings = settings).isLoggedIn.value)
    }

    @Test
    fun clear_withOtherAppData_removesOnlySessionKeys() {
        settings.putString(OTHER_KEY, OTHER_VALUE)
        authCredentialsStorage.setAccessToken(ACCESS_TOKEN)
        authCredentialsStorage.setRefreshToken(REFRESH_TOKEN)
        authCredentialsStorage.setLoggedIn(true)

        authCredentialsStorage.clear()

        assertNull(authCredentialsStorage.getAccessToken())
        assertNull(authCredentialsStorage.getRefreshToken())
        assertFalse(authCredentialsStorage.isLoggedIn.value)
        assertFalse(settings.hasKey(AuthSessionStorageKeys().isLoggedInKey))
        assertEquals(OTHER_VALUE, settings.getStringOrNull(OTHER_KEY))
    }

    @Test
    fun newInstance_withCustomStorageKeys_readsExistingValues() {
        settings.putString(LEGACY_ACCESS_TOKEN_KEY, ACCESS_TOKEN)
        settings.putString(LEGACY_REFRESH_TOKEN_KEY, REFRESH_TOKEN)
        settings.putBoolean(LEGACY_IS_LOGGED_IN_KEY, true)

        val legacyStorage = AuthCredentialsStorageImpl(
            settings = settings,
            storageKeys = AuthSessionStorageKeys(
                accessTokenKey = LEGACY_ACCESS_TOKEN_KEY,
                refreshTokenKey = LEGACY_REFRESH_TOKEN_KEY,
                isLoggedInKey = LEGACY_IS_LOGGED_IN_KEY,
            ),
        )

        assertEquals(ACCESS_TOKEN, legacyStorage.getAccessToken())
        assertEquals(REFRESH_TOKEN, legacyStorage.getRefreshToken())
        assertTrue(legacyStorage.isLoggedIn.value)
    }

    private companion object {
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val OTHER_KEY = "app_language"
        const val OTHER_VALUE = "tr"
        const val LEGACY_ACCESS_TOKEN_KEY = "TOKEN"
        const val LEGACY_REFRESH_TOKEN_KEY = "REFRESH_TOKEN"
        const val LEGACY_IS_LOGGED_IN_KEY = "IS_LOGGED_IN"
    }
}
