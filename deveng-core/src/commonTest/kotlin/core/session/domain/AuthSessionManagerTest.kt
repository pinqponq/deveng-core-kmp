package core.session.domain

import app.cash.turbine.test
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.russhwolf.settings.MapSettings
import core.testing.RecordingLogWriter
import core.session.data.store.AuthCredentialsStorageImpl
import core.session.domain.model.AuthTokens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSessionManagerTest {

    private val authCredentialsStorage = AuthCredentialsStorageImpl(settings = MapSettings())
    private val recordingLogWriter = RecordingLogWriter()
    private val events = mutableListOf<String>()
    private val accessTokensSentToClient = mutableListOf<String?>()
    private val renewedRefreshTokens = mutableListOf<String>()
    private val revokedRefreshTokens = mutableListOf<String>()
    private var renewResponse: CompletableDeferred<AuthTokens> = CompletableDeferred(AuthTokens(RENEWED_ACCESS_TOKEN, RENEWED_REFRESH_TOKEN))
    private var revokeResponse: CompletableDeferred<Unit> = CompletableDeferred(Unit)

    @BeforeTest
    fun setUp() {
        Logger.setLogWriters(recordingLogWriter)
    }

    @AfterTest
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun signIn_whileSignedOut_storesTokensAndSendsAccessTokenBeforeLoggedIn() = runTest {
        val authSessionManager = createAuthSessionManager(onAccessTokenChanged = { accessToken ->
            events += "token:$accessToken loggedIn:${authCredentialsStorage.isLoggedIn.value}"
        })

        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN, onSignedIn = {
            events += "onSignedIn loggedIn:${authSessionManager.isLoggedIn.value}"
        })

        assertEquals(listOf("token:$ACCESS_TOKEN loggedIn:false", "onSignedIn loggedIn:true"), events)
        assertEquals(ACCESS_TOKEN, authSessionManager.getAccessToken())
        assertEquals(REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
    }

    @Test
    fun signInThenSignOut_fromSignedOut_isLoggedInEmitsEachChange() = runTest {
        val authSessionManager = createAuthSessionManager()

        authSessionManager.isLoggedIn.test {
            assertFalse(awaitItem())

            authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)
            assertTrue(awaitItem())

            authSessionManager.signOut()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun signOut_whileSignedIn_runsStepsInOrder() = runTest {
        val authSessionManager = createAuthSessionManager(
            onAccessTokenChanged = { accessToken -> events += "token:$accessToken" },
            revokeRefreshToken = { refreshToken ->
                events += "revoke:$refreshToken loggedIn:${authCredentialsStorage.isLoggedIn.value}"
            },
        )
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)
        events.clear()

        authSessionManager.signOut(
            beforeSignOut = { events += "beforeSignOut loggedIn:${authSessionManager.isLoggedIn.value}" },
            onCleared = { events += "onCleared refreshToken:${authCredentialsStorage.getRefreshToken()}" },
        )

        assertEquals(
            listOf(
                "beforeSignOut loggedIn:true",
                "revoke:$REFRESH_TOKEN loggedIn:false",
                "token:null",
                "onCleared refreshToken:null",
            ),
            events
        )
        assertNull(authSessionManager.getAccessToken())
    }

    @Test
    fun signOut_whenHooksAndRevokeFail_stillClearsSessionAndLogs() = runTest {
        val authSessionManager = createAuthSessionManager(revokeRefreshToken = { throw IllegalStateException(REVOKE_FAILURE) })
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        authSessionManager.signOut(
            beforeSignOut = { throw IllegalStateException(BEFORE_SIGN_OUT_FAILURE) },
            onCleared = { throw IllegalStateException(ON_CLEARED_FAILURE) },
        )

        assertFalse(authSessionManager.isLoggedIn.value)
        assertNull(authSessionManager.getAccessToken())
        assertNull(authCredentialsStorage.getRefreshToken())
        assertEquals(
            listOf(BEFORE_SIGN_OUT_FAILURE, REVOKE_FAILURE, ON_CLEARED_FAILURE),
            recordingLogWriter.warningThrowables.map { it?.message }
        )
    }

    @Test
    fun signOut_withoutRefreshToken_doesNotRevoke() = runTest {
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, refreshToken = null)

        authSessionManager.signOut()

        assertTrue(revokedRefreshTokens.isEmpty())
        assertFalse(authSessionManager.isLoggedIn.value)
    }

    @Test
    fun signOut_withoutRevokeOption_clearsSession() = runTest {
        val authSessionManager = AuthSessionManager(
            authSessionOptions = AuthSessionOptions(
                renewTokens = { renewResponse.await() },
                onAccessTokenChanged = { accessToken -> accessTokensSentToClient += accessToken },
            ),
            authCredentialsStorage = authCredentialsStorage,
        )
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        authSessionManager.signOut()

        assertNull(authCredentialsStorage.getRefreshToken())
        assertEquals(listOf(ACCESS_TOKEN, null), accessTokensSentToClient)
    }

    @Test
    fun signIn_duringSignOutRevoke_keepsNewSessionAndSkipsOnCleared() = runTest {
        revokeResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)
        var isOnClearedRun = false

        val signOut = launch { authSessionManager.signOut(onCleared = { isOnClearedRun = true }) }
        runCurrent()
        authSessionManager.signIn(SECOND_ACCESS_TOKEN, SECOND_REFRESH_TOKEN)
        revokeResponse.complete(Unit)
        signOut.join()

        assertTrue(authSessionManager.isLoggedIn.value)
        assertEquals(SECOND_ACCESS_TOKEN, authSessionManager.getAccessToken())
        assertEquals(SECOND_REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
        assertEquals(listOf(REFRESH_TOKEN), revokedRefreshTokens)
        assertFalse(isOnClearedRun)
    }

    @Test
    fun refreshSession_withRotatedTokens_storesThemAndSendsAccessToken() = runTest {
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val isRefreshed = authSessionManager.refreshSession()

        assertTrue(isRefreshed)
        assertEquals(listOf(REFRESH_TOKEN), renewedRefreshTokens)
        assertEquals(RENEWED_ACCESS_TOKEN, authSessionManager.getAccessToken())
        assertEquals(RENEWED_REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
        assertEquals(listOf<String?>(ACCESS_TOKEN, RENEWED_ACCESS_TOKEN), accessTokensSentToClient)
    }

    @Test
    fun refreshSession_withoutRotatedRefreshToken_keepsStoredOne() = runTest {
        renewResponse = CompletableDeferred(AuthTokens(accessToken = RENEWED_ACCESS_TOKEN, refreshToken = null))
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val isRefreshed = authSessionManager.refreshSession()

        assertTrue(isRefreshed)
        assertEquals(RENEWED_ACCESS_TOKEN, authSessionManager.getAccessToken())
        assertEquals(REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
    }

    @Test
    fun refreshSession_whenSignedOut_returnsFalseWithoutRenewing() = runTest {
        val authSessionManager = createAuthSessionManager()
        authCredentialsStorage.setRefreshToken(REFRESH_TOKEN)

        assertFalse(authSessionManager.refreshSession())
        assertTrue(renewedRefreshTokens.isEmpty())
    }

    @Test
    fun refreshSession_withBlankRefreshToken_returnsFalseWithoutRenewing() = runTest {
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, refreshToken = " ")

        assertFalse(authSessionManager.refreshSession())
        assertTrue(renewedRefreshTokens.isEmpty())
    }

    @Test
    fun refreshSession_whenRenewFails_returnsFalseKeepsTokensAndLogs() = runTest {
        renewResponse = CompletableDeferred<AuthTokens>().apply { completeExceptionally(IllegalStateException(RENEW_FAILURE)) }
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val isRefreshed = authSessionManager.refreshSession()

        assertFalse(isRefreshed)
        assertEquals(ACCESS_TOKEN, authSessionManager.getAccessToken())
        assertEquals(REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
        assertEquals(listOf(RENEW_FAILURE), recordingLogWriter.warningThrowables.map { it?.message })
    }

    @Test
    fun refreshSession_finishingAfterSignOut_writesNothing() = runTest {
        renewResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val refresh = async { authSessionManager.refreshSession() }
        runCurrent()
        authSessionManager.signOut()
        renewResponse.complete(AuthTokens(RENEWED_ACCESS_TOKEN, RENEWED_REFRESH_TOKEN))

        assertFalse(refresh.await())
        assertNull(authSessionManager.getAccessToken())
        assertNull(authCredentialsStorage.getRefreshToken())
        assertFalse(authSessionManager.isLoggedIn.value)
        assertEquals(listOf(ACCESS_TOKEN, null), accessTokensSentToClient)
    }

    @Test
    fun refreshSession_finishingWhileSignOutRevokes_writesNothing() = runTest {
        renewResponse = CompletableDeferred()
        revokeResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val refresh = async { authSessionManager.refreshSession() }
        runCurrent()
        val signOut = launch { authSessionManager.signOut() }
        runCurrent()
        renewResponse.complete(AuthTokens(RENEWED_ACCESS_TOKEN, RENEWED_REFRESH_TOKEN))
        val isRefreshed = refresh.await()
        revokeResponse.complete(Unit)
        signOut.join()

        assertFalse(isRefreshed)
        assertNull(authSessionManager.getAccessToken())
        assertNull(authCredentialsStorage.getRefreshToken())
        assertEquals(listOf(ACCESS_TOKEN, null), accessTokensSentToClient)
    }

    @Test
    fun refreshSession_fromPreviousSessionFinishingAfterNewSignIn_keepsNewTokens() = runTest {
        renewResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val refresh = async { authSessionManager.refreshSession() }
        runCurrent()
        authSessionManager.signOut()
        authSessionManager.signIn(SECOND_ACCESS_TOKEN, SECOND_REFRESH_TOKEN)
        renewResponse.complete(AuthTokens(RENEWED_ACCESS_TOKEN, RENEWED_REFRESH_TOKEN))

        assertFalse(refresh.await())
        assertEquals(SECOND_ACCESS_TOKEN, authSessionManager.getAccessToken())
        assertEquals(SECOND_REFRESH_TOKEN, authCredentialsStorage.getRefreshToken())
    }

    @Test
    fun refreshSession_whenRenewFails_logsWithConfiguredTag() = runTest {
        val loggedTags = mutableListOf<String>()
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                loggedTags += tag
            }
        })
        renewResponse = CompletableDeferred<AuthTokens>().apply { completeExceptionally(IllegalStateException(RENEW_FAILURE)) }
        val authSessionManager = AuthSessionManager(
            authSessionOptions = AuthSessionOptions(
                renewTokens = { renewResponse.await() },
                onAccessTokenChanged = {},
                logTag = CUSTOM_LOG_TAG,
            ),
            authCredentialsStorage = authCredentialsStorage,
        )
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        authSessionManager.refreshSession()

        assertEquals(listOf(CUSTOM_LOG_TAG), loggedTags)
    }

    @Test
    fun decodeClaims_whenSignedIn_decodesStoredAccessToken() = runTest {
        val authSessionManager = createAuthSessionManager()

        authSessionManager.signIn(createJwt(USER_CLAIMS_JSON), REFRESH_TOKEN)

        assertEquals(UserClaims(userId = USER_ID), authSessionManager.decodeClaims<UserClaims>())
    }

    @Test
    fun decodeClaims_whenSignedOut_returnsNull() = runTest {
        val authSessionManager = createAuthSessionManager()

        assertNull(authSessionManager.decodeClaims<UserClaims>())
    }

    @Test
    fun getAccessToken_withLeftoverTokenWhileSignedOut_returnsNull() = runTest {
        authCredentialsStorage.setAccessToken(ACCESS_TOKEN)
        val authSessionManager = createAuthSessionManager()

        assertNull(authSessionManager.getAccessToken())
    }

    @Test
    fun signIn_whenOnSignedInFails_throwsAndStaysSignedIn() = runTest {
        val authSessionManager = createAuthSessionManager()

        val failure = assertFailsWith<IllegalStateException> {
            authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN, onSignedIn = { throw IllegalStateException(ON_SIGNED_IN_FAILURE) })
        }

        assertEquals(ON_SIGNED_IN_FAILURE, failure.message)
        assertTrue(authSessionManager.isLoggedIn.value)
        assertEquals(ACCESS_TOKEN, authSessionManager.getAccessToken())
    }

    @Test
    fun signOut_whenCallerIsCancelledDuringRevoke_stillClearsSession() = runTest {
        revokeResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)
        var isOnClearedRun = false

        val signOut = launch { authSessionManager.signOut(onCleared = { isOnClearedRun = true }) }
        runCurrent()
        signOut.cancel()
        revokeResponse.complete(Unit)
        signOut.join()

        assertNull(authCredentialsStorage.getAccessToken())
        assertNull(authCredentialsStorage.getRefreshToken())
        assertEquals(listOf(ACCESS_TOKEN, null), accessTokensSentToClient)
        assertTrue(isOnClearedRun)
    }

    @Test
    fun signOut_whileAnotherSignOutRevokes_doesNothing() = runTest {
        revokeResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        val firstSignOut = launch { authSessionManager.signOut(onCleared = { events += "first onCleared" }) }
        runCurrent()
        val secondSignOut = launch {
            authSessionManager.signOut(
                beforeSignOut = { events += "second beforeSignOut" },
                onCleared = { events += "second onCleared" },
            )
        }
        runCurrent()
        revokeResponse.complete(Unit)
        firstSignOut.join()
        secondSignOut.join()

        assertEquals(listOf(REFRESH_TOKEN), revokedRefreshTokens)
        assertEquals(listOf("first onCleared"), events)
        assertNull(authCredentialsStorage.getAccessToken())
    }

    @Test
    fun signOut_whenRevokeNeverAnswers_clearsSessionAfterTimeout() = runTest {
        revokeResponse = CompletableDeferred()
        val authSessionManager = createAuthSessionManager()
        authSessionManager.signIn(ACCESS_TOKEN, REFRESH_TOKEN)

        authSessionManager.signOut()

        assertNull(authCredentialsStorage.getAccessToken())
        assertEquals(listOf(ACCESS_TOKEN, null), accessTokensSentToClient)
        assertEquals(1, recordingLogWriter.warningThrowables.size)
    }

    private fun createAuthSessionManager(
        onAccessTokenChanged: (String?) -> Unit = { accessToken -> accessTokensSentToClient += accessToken },
        revokeRefreshToken: suspend (String) -> Unit = { refreshToken ->
            revokedRefreshTokens += refreshToken
            revokeResponse.await()
        },
    ) = AuthSessionManager(
        authSessionOptions = AuthSessionOptions(
            renewTokens = { refreshToken ->
                renewedRefreshTokens += refreshToken
                renewResponse.await()
            },
            onAccessTokenChanged = onAccessTokenChanged,
            revokeRefreshToken = revokeRefreshToken,
        ),
        authCredentialsStorage = authCredentialsStorage,
    )

    @Serializable
    private data class UserClaims(
        @SerialName("sub")
        val userId: String,
    )

    private companion object {
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val SECOND_ACCESS_TOKEN = "second-access-token"
        const val SECOND_REFRESH_TOKEN = "second-refresh-token"
        const val RENEWED_ACCESS_TOKEN = "renewed-access-token"
        const val RENEWED_REFRESH_TOKEN = "renewed-refresh-token"
        const val REVOKE_FAILURE = "revoke failed"
        const val RENEW_FAILURE = "renew failed"
        const val BEFORE_SIGN_OUT_FAILURE = "before sign-out failed"
        const val ON_CLEARED_FAILURE = "on cleared failed"
        const val ON_SIGNED_IN_FAILURE = "on signed in failed"
        const val USER_ID = "user-42"
        const val CUSTOM_LOG_TAG = "AppSession"
        const val USER_CLAIMS_JSON = """{"sub":"$USER_ID","exp":1900000000}"""

        val payloadEncoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

        fun createJwt(payloadJson: String): String =
            "header.${payloadEncoder.encode(payloadJson.encodeToByteArray())}.signature"
    }
}
