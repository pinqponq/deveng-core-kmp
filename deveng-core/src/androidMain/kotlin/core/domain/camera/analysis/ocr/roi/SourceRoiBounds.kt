package core.domain.camera.analysis.ocr.roi

/** Integer pixel bounds used while reading a source-frame ROI. */
internal data class SourceRoiBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int
        get() = right - left + 1

    val height: Int
        get() = bottom - top + 1
}
