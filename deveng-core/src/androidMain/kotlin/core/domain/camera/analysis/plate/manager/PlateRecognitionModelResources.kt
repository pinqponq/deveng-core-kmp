package core.domain.camera.analysis.plate.manager

import core.domain.camera.analysis.plate.engine.PlateDetectorEngine
import core.domain.camera.analysis.plate.engine.PlateOcrEngine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native detector and OCR resources owned by [PlateRecognitionModelManager].
 *
 * An analyzer receives a [PlateRecognitionModelSession], not ownership of this object.
 * Closing the session releases only the analyzer's usage right; closing this object releases the
 * native sessions exactly once.
 */
internal class PlateRecognitionModelResources(
    internal val detector: PlateDetectorEngine,
    internal val ocr: PlateOcrEngine
) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)

    internal fun openSession(onRelease: () -> Unit): PlateRecognitionModelSession {
        check(!isClosed.get()) { "Plate recognition model resources are already closed." }
        return PlateRecognitionModelSession(
            detector = detector,
            ocr = ocr,
            onRelease = onRelease
        )
    }

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        try {
            ocr.close()
        } finally {
            detector.close()
        }
    }
}
