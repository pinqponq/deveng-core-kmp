package com.deveng.calf.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.deveng.calf.io.KmpFile
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList

@Composable
actual fun rememberFilePickerLauncher(
    type: FilePickerFileType,
    selectionMode: FilePickerSelectionMode,
    settings: FilePickerSettings,
    onPickerDismissed: (fileCount: Int) -> Unit,
    onResult: (List<KmpFile>) -> Unit,
): FilePickerLauncher {
    var fileDialogVisible by rememberSaveable { mutableStateOf(false) }
    val currentType by rememberUpdatedState(type)
    val currentSelectionMode by rememberUpdatedState(selectionMode)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        FilePickerLauncher(
            type = type,
            selectionMode = selectionMode,
            onLaunch = {
                if (currentType == FilePickerFileType.Folder) {
                    // Folder picking is not supported on Web
                    currentOnResult(emptyList())
                    return@FilePickerLauncher
                }

                val fileInputElement = document.createElement("input") as HTMLInputElement

                fun cleanup() {
                    fileInputElement.remove()
                }

                with(fileInputElement) {
                    style.display = "none"
                    this.type = "file"
                    name = "file"

                    accept =
                        if (currentType is FilePickerFileType.Extension)
                            currentType.value.joinToString(", ") { ".$it" }
                        else
                            currentType.value.joinToString(", ")

                    multiple = currentSelectionMode is FilePickerSelectionMode.Multiple

                    onchange = { event ->
                        try {
                            // Get the selected files
                            val files = event.target
                                ?.unsafeCast<HTMLInputElement>()
                                ?.files
                                ?.asList()
                                .orEmpty()

                            // Apply maxItems limit if set
                            val maxItems = (currentSelectionMode as? FilePickerSelectionMode.Multiple)?.maxItems
                            val limitedFiles = if (maxItems != null) files.take(maxItems) else files

                            // Return the result
                            currentOnResult(limitedFiles.map { KmpFile(it) })
                            fileDialogVisible = false
                        } catch (_: Throwable) {
                            currentOnResult(emptyList())
                        } finally {
                            cleanup()
                        }
                    }

                    oncancel = {
                        currentOnResult(emptyList())
                        cleanup()
                    }

                    // Append to DOM so events fire reliably, then trigger
                    document.body?.appendChild(this)
                    click()
                }
            },
        )
    }
}


actual class FilePickerLauncher actual constructor(
    type: FilePickerFileType,
    selectionMode: FilePickerSelectionMode,
    private val onLaunch: () -> Unit,
) {
    actual fun launch() {
        onLaunch()
    }
}

@Deprecated(
    message = "Binary compatibility with the published Calf API.",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun rememberFilePickerLauncher(
    type: FilePickerFileType = FilePickerFileType.All,
    selectionMode: FilePickerSelectionMode = FilePickerSelectionMode.Single,
    settings: FilePickerSettings = defaultFilePickerSettings(),
    onResult: (List<KmpFile>) -> Unit,
): FilePickerLauncher = rememberFilePickerLauncher(
    type = type,
    selectionMode = selectionMode,
    settings = settings,
    onPickerDismissed = {},
    onResult = onResult,
)
