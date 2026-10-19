package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.session.data.store.AuthCredentialsStorageImpl
import core.session.domain.AuthSessionManager
import core.session.domain.AuthSessionOptions
import core.session.domain.model.AuthTokens
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

private val DEMO_RENEW_DURATION = 2.seconds
private const val DEMO_ACCESS_TOKEN =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vLXVzZXIifQ.c2lnbmF0dXJl"
private const val DEMO_REFRESH_TOKEN_PREFIX = "demo-refresh-"

@Serializable
private data class DemoUserClaims(
    @SerialName("sub")
    val userId: String,
)

@Composable
internal fun AuthSessionDemoScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val eventLines = remember { mutableStateListOf<String>() }
    var renewCount by remember { mutableIntStateOf(0) }
    var lastRefreshResult by remember { mutableStateOf("-") }
    val authSessionManager = remember {
        AuthSessionManager(
            options = AuthSessionOptions(
                renewTokens = { refreshToken ->
                    eventLines += "renew with $refreshToken (takes $DEMO_RENEW_DURATION)"
                    delay(DEMO_RENEW_DURATION)
                    renewCount += 1
                    AuthTokens(accessToken = DEMO_ACCESS_TOKEN, refreshToken = "$DEMO_REFRESH_TOKEN_PREFIX$renewCount")
                },
                onAccessTokenChanged = { accessToken ->
                    eventLines += "client token: ${accessToken?.take(n = 12) ?: "none"}"
                },
                revokeRefreshToken = { refreshToken -> eventLines += "revoke $refreshToken" },
            ),
            authCredentialsStorage = AuthCredentialsStorageImpl(settings = Settings()),
        )
    }
    val isLoggedIn by authSessionManager.isLoggedIn.collectAsState()

    fun refreshSession() {
        coroutineScope.launch {
            lastRefreshResult = "running"
            lastRefreshResult = if (authSessionManager.refreshSession()) "stored" else "dropped"
        }
    }

    fun signOut() {
        coroutineScope.launch {
            authSessionManager.signOut(
                beforeSignOut = { eventLines += "beforeSignOut: unregister push" },
                onCleared = { eventLines += "onCleared: clear product stores" },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        CustomIconButton(
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )

        Text(text = "Signed in: $isLoggedIn", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        Text(
            text = "User id claim: ${authSessionManager.claims<DemoUserClaims>()?.userId ?: "-"}",
            style = CoreMediumTextStyle().copy(fontSize = 16.sp),
        )
        Text(text = "Last refresh: $lastRefreshResult", style = CoreMediumTextStyle().copy(fontSize = 16.sp))

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Sign in",
            onClick = {
                coroutineScope.launch {
                    authSessionManager.signIn(
                        accessToken = DEMO_ACCESS_TOKEN,
                        refreshToken = "${DEMO_REFRESH_TOKEN_PREFIX}0",
                        onSignedIn = { eventLines += "onSignedIn: sync billing user" },
                    )
                }
            },
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Refresh session",
            onClick = ::refreshSession,
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Refresh, then sign out before it finishes",
            onClick = {
                refreshSession()
                signOut()
            },
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Sign out",
            onClick = ::signOut,
        )

        Text(text = "Events", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        eventLines.forEach { line ->
            Text(text = line, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
        }
    }
}
