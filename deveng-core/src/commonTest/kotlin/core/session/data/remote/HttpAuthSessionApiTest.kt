package core.session.data.remote

import com.russhwolf.settings.MapSettings
import core.session.data.store.AuthCredentialsStorageImpl
import core.session.domain.AuthSessionManager
import core.session.domain.AuthSessionOptions
import core.session.domain.model.AuthTokens
import error_handling.DevengException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import networking.DevengNetworkingConfig
import networking.DevengNetworkingModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpAuthSessionApiTest {

    private val sentRequests = mutableListOf<SentRequest>()
    private val sentAuthorizationHeaders = mutableListOf<String?>()
    private var unauthorizedNotificationCount = 0

    @Test
    fun renewTokens_withRotatedRefreshToken_postsRefreshTokenAndReturnsBothTokens() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.OK to ROTATED_TOKENS_RESPONSE))

        val renewedTokens = authSessionApi.renewTokens(refreshToken = REFRESH_TOKEN)

        assertEquals(AuthTokens(accessToken = RENEWED_ACCESS_TOKEN, refreshToken = RENEWED_REFRESH_TOKEN), renewedTokens)
        assertEquals(listOf(SentRequest(HttpMethod.Post, "$BASE_URL$REFRESH_ENDPOINT", REFRESH_TOKEN_BODY)), sentRequests)
    }

    @Test
    fun renewTokens_withoutRotatedRefreshToken_returnsNullRefreshToken() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.OK to ACCESS_TOKEN_ONLY_RESPONSE))

        val renewedTokens = authSessionApi.renewTokens(refreshToken = REFRESH_TOKEN)

        assertEquals(AuthTokens(accessToken = RENEWED_ACCESS_TOKEN, refreshToken = null), renewedTokens)
    }

    @Test
    fun renewTokens_withRejectedRefreshToken_throwsWithStatusCode() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.Unauthorized to ERROR_RESPONSE))

        val failure = assertFailsWith<DevengException> { authSessionApi.renewTokens(refreshToken = REFRESH_TOKEN) }

        assertEquals(HttpStatusCode.Unauthorized.value, failure.statusCode)
        assertEquals(1, unauthorizedNotificationCount)
    }

    @Test
    fun renewTokens_withClientToken_sendsTheCurrentAccessToken() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.OK to ROTATED_TOKENS_RESPONSE))

        authSessionApi.renewTokens(refreshToken = REFRESH_TOKEN)

        assertEquals(listOf<String?>("Bearer $ACCESS_TOKEN"), sentAuthorizationHeaders)
    }

    @Test
    fun revokeRefreshToken_withUnauthorizedAnswer_throwsAndNotifiesTheClient() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.Unauthorized to ERROR_RESPONSE))

        val failure = assertFailsWith<DevengException> { authSessionApi.revokeRefreshToken(refreshToken = REFRESH_TOKEN) }

        assertEquals(HttpStatusCode.Unauthorized.value, failure.statusCode)
        assertEquals(1, unauthorizedNotificationCount)
    }

    @Test
    fun revokeRefreshToken_withNoContentAnswer_postsRefreshTokenToLogoutEndpoint() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.NoContent to ""))

        authSessionApi.revokeRefreshToken(refreshToken = REFRESH_TOKEN)

        assertEquals(listOf(SentRequest(HttpMethod.Post, "$BASE_URL$LOGOUT_ENDPOINT", REFRESH_TOKEN_BODY)), sentRequests)
    }

    @Test
    fun revokeRefreshToken_withJsonBodyAnswer_finishes() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.OK to LOGOUT_RESPONSE))

        authSessionApi.revokeRefreshToken(refreshToken = REFRESH_TOKEN)

        assertEquals(listOf("$BASE_URL$LOGOUT_ENDPOINT"), sentRequests.map { it.url })
    }

    @Test
    fun revokeRefreshToken_withServerError_throwsWithStatusCode() = runTest {
        val authSessionApi = createAuthSessionApi(responses = listOf(HttpStatusCode.InternalServerError to ERROR_RESPONSE))

        val failure = assertFailsWith<DevengException> { authSessionApi.revokeRefreshToken(refreshToken = REFRESH_TOKEN) }

        assertEquals(HttpStatusCode.InternalServerError.value, failure.statusCode)
        assertEquals(0, unauthorizedNotificationCount)
    }

    @Test
    fun create_withBlankRefreshEndpoint_failsFast() {
        val failure = assertFailsWith<IllegalArgumentException> {
            HttpAuthSessionApi(DevengNetworkingModule(), refreshEndpoint = " ", logoutEndpoint = LOGOUT_ENDPOINT)
        }

        assertEquals("refreshEndpoint must not be blank", failure.message)
    }

    @Test
    fun create_withBlankLogoutEndpoint_failsFast() {
        val failure = assertFailsWith<IllegalArgumentException> {
            HttpAuthSessionApi(DevengNetworkingModule(), refreshEndpoint = REFRESH_ENDPOINT, logoutEndpoint = "")
        }

        assertEquals("logoutEndpoint must not be blank", failure.message)
    }

    @Test
    fun authSessionManager_withHttpAuthSessionApi_renewsThenRevokesTheRotatedToken() = runTest {
        val authSessionApi = createAuthSessionApi(
            responses = listOf(HttpStatusCode.OK to ROTATED_TOKENS_RESPONSE, HttpStatusCode.NoContent to ""),
        )
        val accessTokensSentToClient = mutableListOf<String?>()
        val authSessionManager = AuthSessionManager(
            authSessionOptions = AuthSessionOptions(
                renewTokens = authSessionApi::renewTokens,
                onAccessTokenChanged = { accessToken -> accessTokensSentToClient += accessToken },
                revokeRefreshToken = authSessionApi::revokeRefreshToken,
            ),
            authCredentialsStorage = AuthCredentialsStorageImpl(settings = MapSettings()),
        )
        authSessionManager.signIn(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)

        val isRefreshStored = authSessionManager.refreshSession()
        authSessionManager.signOut()

        assertTrue(isRefreshStored)
        assertEquals(
            listOf(
                SentRequest(HttpMethod.Post, "$BASE_URL$REFRESH_ENDPOINT", REFRESH_TOKEN_BODY),
                SentRequest(HttpMethod.Post, "$BASE_URL$LOGOUT_ENDPOINT", RENEWED_REFRESH_TOKEN_BODY),
            ),
            sentRequests,
        )
        assertEquals(listOf(ACCESS_TOKEN, RENEWED_ACCESS_TOKEN, null), accessTokensSentToClient)
    }

    private fun createAuthSessionApi(responses: List<Pair<HttpStatusCode, String>>): HttpAuthSessionApi {
        val pendingResponses = ArrayDeque(responses)
        val mockEngine = MockEngine { request ->
            sentRequests += SentRequest(
                method = request.method,
                url = request.url.toString(),
                body = request.body.toByteArray().decodeToString(),
            )
            sentAuthorizationHeaders += request.headers[HttpHeaders.Authorization]
            val (status, body) = pendingResponses.removeFirst()
            respond(content = body, status = status, headers = JSON_HEADERS)
        }
        val devengNetworkingModule = DevengNetworkingModule()
        devengNetworkingModule.initDevengNetworkingModule(
            restBaseUrl = BASE_URL,
            config = DevengNetworkingConfig(
                loggingEnabled = false,
                token = ACCESS_TOKEN,
                onUnauthorized = { unauthorizedNotificationCount += 1 },
            ),
        )
        devengNetworkingModule.client?.close()
        devengNetworkingModule.client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        return HttpAuthSessionApi(
            devengNetworkingModule = devengNetworkingModule,
            refreshEndpoint = REFRESH_ENDPOINT,
            logoutEndpoint = LOGOUT_ENDPOINT,
        )
    }

    private data class SentRequest(val method: HttpMethod, val url: String, val body: String)

    private companion object {
        const val BASE_URL = "https://api.example.test/"
        const val REFRESH_ENDPOINT = "auth/refresh"
        const val LOGOUT_ENDPOINT = "auth/logout"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val RENEWED_ACCESS_TOKEN = "renewed-access-token"
        const val RENEWED_REFRESH_TOKEN = "renewed-refresh-token"
        const val REFRESH_TOKEN_BODY = """{"refreshToken":"$REFRESH_TOKEN"}"""
        const val RENEWED_REFRESH_TOKEN_BODY = """{"refreshToken":"$RENEWED_REFRESH_TOKEN"}"""
        const val ROTATED_TOKENS_RESPONSE =
            """{"accessToken":"$RENEWED_ACCESS_TOKEN","refreshToken":"$RENEWED_REFRESH_TOKEN","expiresIn":900}"""
        const val ACCESS_TOKEN_ONLY_RESPONSE = """{"accessToken":"$RENEWED_ACCESS_TOKEN"}"""
        const val LOGOUT_RESPONSE = """{"success":true}"""
        const val ERROR_RESPONSE = """{"message":"refresh_token_invalid"}"""
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
