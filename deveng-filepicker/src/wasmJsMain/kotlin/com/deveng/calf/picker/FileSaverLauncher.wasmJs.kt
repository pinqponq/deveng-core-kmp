@file:OptIn(ExperimentalWasmJsInterop::class)

package com.deveng.calf.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.deveng.calf.core.ExperimentalCalfApi
import com.deveng.calf.io.KmpFile
import org.w3c.files.Blob
import org.w3c.files.File

@ExperimentalCalfApi
@Composable
actual fun rememberFileSaverLauncher(
    settings: FilePickerSettings,
    onResult: (KmpFile?) -> Unit,
): FileSaverLauncher {
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        FileSaverLauncher(onLaunchBytes = { bytes, fileName ->
            val blob = createBlob(bytes ?: ByteArray(0), mimeType = "application/octet-stream")
            downloadBlob(blob, fileName)
            currentOnResult(KmpFile(File(arrayOf<JsAny?>(blob).toJsArray(), fileName)))
        }, onLaunchFile = { file, fileName ->
            downloadBlob(file.file, fileName)
            currentOnResult(file)
        })
    }
}

@ExperimentalCalfApi
@Stable
actual class FileSaverLauncher internal constructor(
    private val onLaunchBytes: (bytes: ByteArray?, fileName: String) -> Unit,
    private val onLaunchFile: (file: KmpFile, fileName: String) -> Unit,
) {
    actual fun launch(
        bytes: ByteArray?,
        baseName: String,
        extension: String,
        initialDirectory: String?,
    ) {
        onLaunchBytes(bytes, "$baseName.$extension")
    }

    actual fun launch(
        file: KmpFile,
        baseName: String,
        extension: String,
        initialDirectory: String?,
    ) {
        onLaunchFile(file, "$baseName.$extension")
    }
}

@JsFun(
    "(blob, fileName) => { const url = URL.createObjectURL(blob); " +
        "const anchor = document.createElement('a'); anchor.href = url; anchor.download = fileName; " +
        "document.body.appendChild(anchor); anchor.click(); document.body.removeChild(anchor); " +
        "URL.revokeObjectURL(url); }"
)
private external fun downloadBlob(blob: Blob, fileName: String)
