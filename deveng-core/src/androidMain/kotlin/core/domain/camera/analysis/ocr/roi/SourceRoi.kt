package core.domain.camera.analysis.ocr.roi

/** Packed RGB pixels copied from the CameraX source frame and oriented for OCR. */
internal data class SourceRoi(
    val bounds: SourceRoiBounds,
    val pixels: IntArray,
    val pixelWidth: Int = bounds.width,
    val pixelHeight: Int = bounds.height
)