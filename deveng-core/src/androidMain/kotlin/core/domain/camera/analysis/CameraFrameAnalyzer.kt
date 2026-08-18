package core.domain.camera.analysis

/**
 * Android-only, model-independent contract for analyzing frames from the camera component.
 *
 * The camera owns the [AndroidCameraFrame] lifecycle. Implementations must finish reading the
 * frame before [analyze] returns and must not close the frame themselves.
 */
interface CameraFrameAnalyzer<R> {
    /**
     * Analyzes one camera frame.
     *
     * Returning `null` means that this frame did not produce a result.
     */
    suspend fun analyze(frame: AndroidCameraFrame): R?

    /** Called when the analyzer is registered with a camera controller. */
    fun start() {}

    /** Called when the analyzer is unregistered. */
    fun stop() {}

    /** Releases analyzer resources permanently. */
    fun close() {}
}

/** Pixel format requested from CameraX for a generic frame analyzer. */
enum class FrameAnalysisOutputFormat {
    YUV_420_888,
    RGBA_8888,
}
