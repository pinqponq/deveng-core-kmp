package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.util.decodeJwtClaims
import core.util.decodeJwtPayload
import core.util.jwtExpiresAt
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TOKEN_WITH_EXPIRY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vLXVzZXIiLCJub3RlIjoiPj4-Pz8_IiwiZXhwIjoxODAwMDAwMDAwfQ.c2lnbmF0dXJl"
private const val TOKEN_WITHOUT_EXPIRY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vLXVzZXIifQ.c2lnbmF0dXJl"
private const val TOKEN_WITH_INVALID_FORMAT = "not-a-jwt"

@Serializable
private data class DemoTokenClaims(
    @SerialName("sub")
    val subject: String
)

private data class JwtDemoSample(
    val title: String,
    val token: String
)

private val jwtDemoSamples = listOf(
    JwtDemoSample(title = "Token with exp (base64url payload, no padding)", token = TOKEN_WITH_EXPIRY),
    JwtDemoSample(title = "Token without exp", token = TOKEN_WITHOUT_EXPIRY),
    JwtDemoSample(title = "Invalid token", token = TOKEN_WITH_INVALID_FORMAT),
)

@Composable
internal fun JwtClaimsDemoScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 72.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 24.dp),
        ) {
            jwtDemoSamples.forEach { sample ->
                JwtDemoSampleSection(sample = sample)
            }
        }

        CustomIconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(all = 16.dp),
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )
    }
}

@Composable
private fun JwtDemoSampleSection(sample: JwtDemoSample) {
    val resultLines = readJwtDemoResult(sample.token)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(text = sample.title, style = CoreMediumTextStyle().copy(fontSize = 18.sp))
        resultLines.forEach { line ->
            Text(text = line, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
        }
    }
}

private fun readJwtDemoResult(token: String): List<String> =
    try {
        listOf(
            "decodeJwtPayload: ${decodeJwtPayload(token)}",
            "decodeJwtClaims: ${decodeJwtClaims<DemoTokenClaims>(token)}",
            "jwtExpiresAt: ${jwtExpiresAt(token)}",
        )
    } catch (exception: IllegalArgumentException) {
        listOf("IllegalArgumentException: ${exception.message}")
    }
