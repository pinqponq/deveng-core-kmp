package core.domain.camera.android

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import core.util.video.VideoFileDbgProbe
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Re-exports front-camera recordings with a horizontal flip so playback matches the mirrored
 * [androidx.camera.view.PreviewView] selfie preview (same role as iOS [FrontCameraVideoExportHelper]).
 */
internal object FrontCameraVideoExportHelper {
    private const val TAG = "CameraK"
    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun mirrorToMatchPreviewInPlace(context: Context, filePath: String): Boolean {
        val paths = withContext(Dispatchers.IO) {
            val source = File(filePath)
            if (!source.isFile) {
                Log.w(TAG, "front video mirror: missing file path=$filePath")
                return@withContext null
            }
            val temp = File("$filePath.mirror-preview.tmp.mp4")
            temp.delete()
            source to temp
        } ?: return false

        val (source, temp) = paths
        Log.d(TAG, "[RindleVideoDbg] phase=MIRROR_EXPORT_BEFORE | ${VideoFileDbgProbe.describe(source.absolutePath)}")
        val exported = exportHorizontallyFlipped(
            context = context.applicationContext,
            inputUri = source.toUri(),
            outputFile = temp,
        )
        if (!exported) {
            withContext(Dispatchers.IO) { temp.delete() }
            return false
        }
        return withContext(Dispatchers.IO) {
            if (!source.delete()) {
                temp.delete()
                Log.w(TAG, "front video mirror: could not remove source path=$filePath")
                return@withContext false
            }
            if (!temp.renameTo(source)) {
                Log.w(TAG, "front video mirror: rename failed path=$filePath")
                return@withContext false
            }
            Log.d(
                TAG,
                "[RindleVideoDbg] phase=MIRROR_EXPORT_AFTER | ${VideoFileDbgProbe.describe(source.absolutePath)}",
            )
            true
        }
    }

    /**
     * Media3 [Transformer] must be created, started, and cancelled on the application (main) thread.
     */
    private suspend fun exportHorizontallyFlipped(
        context: Context,
        inputUri: Uri,
        outputFile: File,
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val flip = ScaleAndRotateTransformation.Builder()
                .setScale(-1f, 1f)
                .build()
            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
                .setEffects(Effects(emptyList(), listOf(flip)))
                .build()
            val transformer = Transformer.Builder(context)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(true)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            Log.w(
                                TAG,
                                "front video mirror export failed: ${exportException.message}",
                            )
                            if (cont.isActive) cont.resume(false)
                        }
                    },
                )
                .build()
            cont.invokeOnCancellation {
                mainHandler.post {
                    runCatching { transformer.cancel() }.onFailure { error ->
                        Log.w(TAG, "front video mirror cancel failed: ${error.message}")
                    }
                }
            }
            transformer.start(editedMediaItem, outputFile.absolutePath)
        }
    }
}
