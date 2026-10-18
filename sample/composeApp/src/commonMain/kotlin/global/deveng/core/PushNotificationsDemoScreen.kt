package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import core.presentation.component.LabeledSwitch
import core.presentation.permission.BindEffect
import core.presentation.permission.PermissionsController
import core.presentation.permission.rememberPermissionsControllerFactory
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.push.data.store.NotificationPermissionStorageImpl
import core.push.domain.PushNotificationOptions
import core.push.domain.PushTokenSource
import core.push.domain.SoftAskPolicy
import core.push.domain.model.NotificationPermissionStatus
import core.push.presentation.PushNotifications
import core.util.multiplatform.PlatformConfig
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

private const val DEMO_TOKEN_PREFIX = "demo-push-token-"
private const val DEMO_DEVICE_IDENTIFIER = "demo-device"
private const val DEMO_MAX_ASK_COUNT = 3
private val DEMO_MINIMUM_ASK_INTERVAL = 1.minutes

/**
 * Playground for core push notifications against a fake backend: the sample has no Firebase
 * configuration, so the token comes from a demo source and every backend call lands in the log below.
 */
@Composable
internal fun PushNotificationsDemoScreen(onBack: () -> Unit) {
    val permissionsController = rememberPermissionsControllerFactory().createPermissionsController()
    BindEffect(permissionsController)

    val coroutineScope = rememberCoroutineScope()
    val backendLogLines = remember { mutableStateListOf<String>() }
    var isSignedIn by remember { mutableStateOf(true) }
    var tokenVersion by remember { mutableIntStateOf(1) }
    var permissionStatus by remember { mutableStateOf<NotificationPermissionStatus?>(null) }
    var lastActionResult by remember { mutableStateOf("-") }
    val demoPushTokenSource = remember { DemoPushTokenSource(initialToken = "$DEMO_TOKEN_PREFIX$tokenVersion") }
    val notificationPermissionStorage = remember { NotificationPermissionStorageImpl(settings = Settings()) }
    val pushNotifications = remember {
        PushNotifications(
            options = PushNotificationOptions(
                androidChannelId = "sample_notifications",
                androidChannelName = "Sample notifications",
                softAskPolicy = SoftAskPolicy(maxAskCount = DEMO_MAX_ASK_COUNT, minimumInterval = DEMO_MINIMUM_ASK_INTERVAL),
            ),
            pushTokenSource = demoPushTokenSource,
            pushTokenRegistrar = { token, platform, deviceIdentifier ->
                backendLogLines += "register $token ($platform, $deviceIdentifier)"
            },
            pushTokenUnregistrar = { token, deviceIdentifier ->
                backendLogLines += "unregister $token ($deviceIdentifier)"
            },
            platformConfigProvider = { DEMO_PLATFORM_CONFIG },
            notificationPermissionStorage = notificationPermissionStorage,
            isSignedIn = { isSignedIn },
        )
    }

    suspend fun refreshPermissionStatus(controller: PermissionsController) {
        permissionStatus = pushNotifications.resolvePermissionStatus(controller)
    }

    LaunchedEffect(pushNotifications) {
        refreshPermissionStatus(permissionsController)
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

        Text(text = "Permission: ${permissionStatus ?: "-"}", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        Text(text = "Last result: $lastActionResult", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        Text(
            text = "Asks: ${notificationPermissionStorage.getAskCount()} of $DEMO_MAX_ASK_COUNT, at least $DEMO_MINIMUM_ASK_INTERVAL apart",
            style = CoreRegularTextStyle().copy(fontSize = 14.sp),
        )

        LabeledSwitch(
            label = "Signed in",
            isChecked = isSignedIn,
            onSwitchClick = { isChecked -> isSignedIn = isChecked },
        )

        CustomButton(
            text = "Soft ask",
            onClick = {
                coroutineScope.launch {
                    val softAskStatus = pushNotifications.softAsk(permissionsController)
                    lastActionResult = softAskStatus?.let { "show own request ($it)" } ?: "no request due"
                    refreshPermissionStatus(permissionsController)
                }
            },
        )

        CustomButton(
            text = "Enable notifications",
            onClick = {
                coroutineScope.launch {
                    val promptResult = pushNotifications.enableNotifications(permissionsController)
                    lastActionResult = promptResult?.toString() ?: "already allowed"
                    refreshPermissionStatus(permissionsController)
                }
            },
        )

        CustomButton(
            text = "Register token if permitted",
            onClick = {
                coroutineScope.launch { pushNotifications.registerTokenIfPermitted(permissionsController) }
            },
        )

        CustomButton(
            text = "Rotate token",
            onClick = {
                tokenVersion += 1
                val rotatedToken = "$DEMO_TOKEN_PREFIX$tokenVersion"
                demoPushTokenSource.token.value = rotatedToken
                coroutineScope.launch { pushNotifications.handleNewToken(rotatedToken) }
            },
        )

        CustomButton(
            text = "Sign out (unregister)",
            containerColor = Color(0xFFC62828),
            onClick = {
                coroutineScope.launch {
                    pushNotifications.unregisterToken()
                    isSignedIn = false
                }
            },
        )

        CustomButton(
            text = "Reset ask history",
            containerColor = Color(0xFF616161),
            onClick = {
                notificationPermissionStorage.clear()
                lastActionResult = "ask history cleared"
            },
        )

        Text(text = "Backend calls", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        backendLogLines.forEach { backendLogLine ->
            Text(text = backendLogLine, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
        }
    }
}

private class DemoPushTokenSource(initialToken: String) : PushTokenSource {
    val token = MutableStateFlow<String?>(initialToken)

    override fun observeToken(): Flow<String?> = token
}

private val DEMO_PLATFORM_CONFIG = PlatformConfig(
    platform = samplePlatform,
    systemLanguage = "en",
    uuid = DEMO_DEVICE_IDENTIFIER,
    deviceName = "Sample device",
    packageVersionName = "sample",
)
