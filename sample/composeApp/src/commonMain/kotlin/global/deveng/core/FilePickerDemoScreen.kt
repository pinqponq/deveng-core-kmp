package global.deveng.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deveng.calf.io.KmpFile
import com.deveng.calf.io.getName
import com.deveng.calf.io.getSize
import com.deveng.calf.picker.FilePickerFileType
import com.deveng.calf.picker.FilePickerSelectionMode
import com.deveng.calf.picker.FilePickerSettings
import com.deveng.calf.picker.rememberFilePickerLauncher
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreRegularTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private const val BYTES_PER_KILOBYTE = 1024
private const val BYTES_PER_MEGABYTE = 1024 * 1024
private const val MAX_FILE_SIZE_BYTES = 2L * BYTES_PER_MEGABYTE
private const val MAX_ITEM_COUNT = 5

/** One line per picked file: its name and size, or a message when the file is over the size limit. */
@Composable
internal fun FilePickerDemoScreen(onBack: () -> Unit) {
    var resultLines by remember { mutableStateOf(listOf("Nothing picked yet")) }

    val pickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.ImageVideo,
        selectionMode = FilePickerSelectionMode.Multiple(maxItems = MAX_ITEM_COUNT),
        settings = FilePickerSettings(maxFileSizeBytes = MAX_FILE_SIZE_BYTES),
        onResult = { files -> resultLines = files.map(::describePickedFile).ifEmpty { listOf("Picker closed without a pick") } },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CustomIconButton(
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )

        Text(
            text = "Pick up to $MAX_ITEM_COUNT photos or videos. Files over ${MAX_FILE_SIZE_BYTES / BYTES_PER_MEGABYTE} MB are reported as too large.",
            style = CoreRegularTextStyle(),
        )

        CustomButton(
            text = "Pick photos and videos",
            onClick = pickerLauncher::launch,
        )

        resultLines.forEach { line ->
            Text(text = line, style = CoreRegularTextStyle())
        }
    }
}

private fun describePickedFile(file: KmpFile): String {
    val name = file.getName() ?: "unnamed file"
    val size = file.getSize()
    return when {
        size == null -> "$name: size unknown"
        size > MAX_FILE_SIZE_BYTES -> "$name: too large (${size / BYTES_PER_MEGABYTE} MB)"
        else -> "$name: ${size / BYTES_PER_KILOBYTE} KB"
    }
}
