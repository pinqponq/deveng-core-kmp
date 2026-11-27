package core.domain.camera.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.VideoInputFrameGrabber
import java.awt.image.BufferedImage

class CameraGrabber(
    private val frameChannel: Channel<BufferedImage>,
    private val errorHandler: (Throwable) -> Unit,
    private val targetResolution: Pair<Int, Int>? = null,
    /**
     * Optional fan-out sink for grabbed frames. When provided, every successfully converted
     * frame is also offered to this flow via `tryEmit` in addition to being sent to
     * [frameChannel]. Used by plugins (e.g. [core.domain.camera.scanner.QrScannerPlugin])
     * that need the live frame stream without stealing frames from the preview.
     */
    private val frameFlow: MutableSharedFlow<BufferedImage>? = null,
) {
    private val converter = FrameConverter()
    private var grabber: FrameGrabber? = null
    private var job: Job? = null

    private var isHorizontallyFlipped = false

    fun setHorizontalFlip(flip: Boolean) {
        this.isHorizontallyFlipped = flip
        converter.setHorizontalFlip(flip)
    }

    fun grabCurrentFrame(): BufferedImage? {
        val frame = grabber?.grab()
        return if (frame?.image != null) {
            converter.convert(frame)
        } else {
            null
        }
    }

    fun start(coroutineScope: CoroutineScope, customGrabber: FrameGrabber? = null) {
        if (job?.isActive == true) return

        grabber =
            (customGrabber ?: createGrabber()).apply {
                try {
                    // Set format for macOS AVFoundation before starting
                    if (this is FFmpegFrameGrabber && getOperatingSystem() == OperatingSystem.MAC) {
                        this.format = "avfoundation"
                    }
                    start()
                } catch (e: Exception) {
                    errorHandler(e)
                    return
                }
            }

        converter.setHorizontalFlip(isHorizontallyFlipped)

        job =
            coroutineScope.launch(Dispatchers.IO) {
                var frameCount = 0
                var lastFpsTime = System.currentTimeMillis()

                try {
                    while (isActive) {
                        val frame = grabber?.grab()
                        if (frame?.image != null) {
                            converter.convert(frame)?.let { image ->
                                frameChannel.trySend(image)
                                frameFlow?.tryEmit(image)
                                println("DEBUG: Sent frame to channel, image size: ${image.width}x${image.height}")

                                frameCount++
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastFpsTime >= 1000) {
                                    println("Camera FPS: $frameCount")
                                    frameCount = 0
                                    lastFpsTime = currentTime
                                }
                            }
                        }

                        delay(1)
                    }
                } catch (e: Exception) {
                    errorHandler(e)
                }
            }
    }

    fun stop() {
        job?.cancel()
        try {
            grabber?.stop()
            grabber?.release()
            converter.release()
        } catch (e: Exception) {
            errorHandler(e)
        }
    }

    private fun createGrabber(): FrameGrabber = when (getOperatingSystem()) {
        OperatingSystem.MAC -> {
            // On macOS, use AVFoundation with device index as string
            FFmpegFrameGrabber("0")
        }
        OperatingSystem.WINDOWS -> {
            VideoInputFrameGrabber(0)
        }
        OperatingSystem.LINUX -> {
            // Prefer /dev/video0 but fall back to the first available readable device (#52, #53)
            val videoDevice = findBestVideoDevice()
            println("CameraK: Using video device: $videoDevice")
            FFmpegFrameGrabber(videoDevice)
        }
    }.apply {
        // Prefer 60 fps capture when the driver allows; recording pipeline falls back if the stream is slower.
        frameRate = 60.0
        // Default to 720p (was 480p) to use higher resolution by default (#52)
        val (width, height) = targetResolution ?: (1280 to 720)
        imageWidth = width
        imageHeight = height
    }

    private fun findBestVideoDevice(): String {
        val defaultDevice = "/dev/video0"
        val defaultFile = java.io.File(defaultDevice)

        if (defaultFile.exists() && defaultFile.canRead()) return defaultDevice

        val devDir = java.io.File("/dev")
        val candidates =
            devDir
                .listFiles { file ->
                    file.name.startsWith("video") && file.canRead()
                }?.sortedBy { it.name } ?: emptyList()

        return candidates.firstOrNull()?.absolutePath ?: defaultDevice
    }

    private fun getOperatingSystem(): OperatingSystem {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> OperatingSystem.MAC
            os.contains("windows") -> OperatingSystem.WINDOWS
            else -> OperatingSystem.LINUX
        }
    }
}

enum class OperatingSystem {
    WINDOWS,
    MAC,
    LINUX,
}
