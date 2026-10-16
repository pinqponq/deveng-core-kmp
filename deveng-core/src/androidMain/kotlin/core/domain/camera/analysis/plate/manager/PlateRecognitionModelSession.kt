package core.domain.camera.analysis.plate.manager

import core.domain.camera.analysis.plate.engine.PlateDetectorEngine
import core.domain.camera.analysis.plate.engine.PlateOcrEngine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One analyzer's temporary right to use manager-owned detector and OCR resources.
 *
 * The session is idempotent. Its close callback never closes the native model sessions.
 */
internal class PlateRecognitionModelSession internal constructor(
    internal val detector: PlateDetectorEngine,
    internal val ocr: PlateOcrEngine,
    private val onRelease: () -> Unit
) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            onRelease()
        }
    }
}
