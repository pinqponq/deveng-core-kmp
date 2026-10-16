package core.domain.camera.analysis.ocr.roi

/** Configurable expansion applied to a source-frame plate bounding box before OCR. */
internal data class OcrBboxPadding(
    val horizontalRatio: Float = 0f,
    val verticalRatio: Float = 0f
)