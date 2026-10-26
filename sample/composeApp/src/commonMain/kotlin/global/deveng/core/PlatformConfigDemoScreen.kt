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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.util.multiplatform.PlatformConfig
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private const val MISSING_VALUE_TEXT = "null"
private const val UNAVAILABLE_TEXT = "MultiPlatformUtils is not created on this platform in the sample."

private data class PlatformConfigDemoRow(
    val fieldName: String,
    val fieldValue: String,
)

@Composable
internal fun PlatformConfigDemoScreen(onBack: () -> Unit) {
    val multiPlatformUtils = rememberSampleMultiPlatformUtils()
    val platformConfigRows = remember(multiPlatformUtils) {
        multiPlatformUtils?.getPlatformConfig()?.let(::readPlatformConfigRows).orEmpty()
    }
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
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            Text(text = "getPlatformConfig()", style = CoreMediumTextStyle().copy(fontSize = 18.sp))
            if (platformConfigRows.isEmpty()) {
                Text(text = UNAVAILABLE_TEXT, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            }
            platformConfigRows.forEach { row ->
                PlatformConfigDemoRowSection(row = row)
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
private fun PlatformConfigDemoRowSection(row: PlatformConfigDemoRow) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(text = row.fieldName, style = CoreMediumTextStyle().copy(fontSize = 14.sp))
        Text(text = row.fieldValue, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
    }
}

private fun readPlatformConfigRows(platformConfig: PlatformConfig): List<PlatformConfigDemoRow> =
    listOf(
        PlatformConfigDemoRow(fieldName = "platform", fieldValue = platformConfig.platform.name),
        PlatformConfigDemoRow(fieldName = "systemLanguage", fieldValue = platformConfig.systemLanguage),
        PlatformConfigDemoRow(fieldName = "deviceName", fieldValue = platformConfig.deviceName),
        PlatformConfigDemoRow(fieldName = "packageVersionName", fieldValue = platformConfig.packageVersionName),
        PlatformConfigDemoRow(
            fieldName = "operatingSystemVersion",
            fieldValue = platformConfig.operatingSystemVersion ?: MISSING_VALUE_TEXT,
        ),
    )
