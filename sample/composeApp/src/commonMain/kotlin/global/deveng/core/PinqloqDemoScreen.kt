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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.pinqloq.data.PinqloqLogRepositoryImpl
import core.pinqloq.domain.PinqloqLogger
import core.pinqloq.domain.PinqloqLoggerImpl
import core.pinqloq.domain.PinqloqMetadataProvider
import core.pinqloq.domain.PinqloqRequestMappers
import core.pinqloq.domain.model.PinqloqClientOptions
import core.pinqloq.domain.model.PinqloqLevel
import core.pinqloq.domain.model.PinqloqPlatformInfo
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val DEMO_ENDPOINT = "client-logs"
private const val DEMO_BATCH_ENDPOINT = "client-logs/bulk"
private const val DEMO_MAX_EVENT_LENGTH = 40
private const val DEMO_BATCH_SIZE = 3
private const val LONG_EVENT_LENGTH = 100
private const val PREVIEW_LENGTH = 90

@Composable
internal fun PinqloqDemoScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val requestLines = remember { mutableStateListOf<String>() }
    var isSignedIn by remember { mutableStateOf(true) }
    val immediateLogger = remember {
        createDemoLogger(
            coroutineScope = coroutineScope,
            requestLines = requestLines,
            batchSize = 1,
            isSignedIn = { isSignedIn },
        )
    }
    val batchingLogger = remember {
        createDemoLogger(
            coroutineScope = coroutineScope,
            requestLines = requestLines,
            batchSize = DEMO_BATCH_SIZE,
            isSignedIn = { isSignedIn },
        )
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

        Text(text = "Signed in: $isSignedIn", style = CoreMediumTextStyle().copy(fontSize = 16.sp))

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (isSignedIn) "Sign out (logs are not sent)" else "Sign in (logs are sent)",
            onClick = { isSignedIn = !isSignedIn },
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log a warning at once",
            onClick = {
                coroutineScope.launch {
                    immediateLogger.log(PinqloqLevel.Warning, "Profile photo upload failed")
                }
            },
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log an event longer than $DEMO_MAX_EVENT_LENGTH characters",
            onClick = {
                coroutineScope.launch {
                    immediateLogger.log(PinqloqLevel.Information, "x".repeat(LONG_EVENT_LENGTH))
                }
            },
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log 1 event, sent after the flush interval",
            onClick = {
                coroutineScope.launch {
                    batchingLogger.log(PinqloqLevel.Debug, "Buffered event")
                }
            },
        )

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log $DEMO_BATCH_SIZE events, sent as one batch",
            onClick = {
                coroutineScope.launch {
                    repeat(DEMO_BATCH_SIZE) { index ->
                        batchingLogger.log(PinqloqLevel.Debug, "Batched event ${index + 1}")
                    }
                }
            },
        )

        Text(text = "Requests", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        requestLines.forEach { line ->
            Text(text = line, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
        }
    }
}

private fun createDemoLogger(
    coroutineScope: CoroutineScope,
    requestLines: MutableList<String>,
    batchSize: Int,
    isSignedIn: () -> Boolean,
): PinqloqLogger {
    val clientOptions = PinqloqClientOptions(
        endpoint = DEMO_ENDPOINT,
        batchEndpoint = DEMO_BATCH_ENDPOINT,
        secretKey = "demo-secret-key",
        maxEventLength = DEMO_MAX_EVENT_LENGTH,
        batchSize = batchSize,
        queueCapacity = DEMO_BATCH_SIZE * 2,
        flushInterval = 3.seconds,
        enabled = isSignedIn,
        requestMapper = PinqloqRequestMappers.LevelWithDeviceIdentifier,
    )
    return PinqloqLoggerImpl(
        clientOptions = clientOptions,
        logRepository = PinqloqLogRepositoryImpl(
            clientOptions = clientOptions,
            requestSender = { endpoint, headers, body ->
                requestLines += "POST $endpoint headers=${headers.keys} ${body.toString().take(PREVIEW_LENGTH)}"
            },
        ),
        platformInfoProvider = {
            PinqloqPlatformInfo(appVersionName = "1.0.0", deviceUid = "demo-device", deviceName = "Sample")
        },
        metadataProvider = PinqloqMetadataProvider { mapOf("userId" to "demo-user") },
        flushScope = coroutineScope,
    )
}
