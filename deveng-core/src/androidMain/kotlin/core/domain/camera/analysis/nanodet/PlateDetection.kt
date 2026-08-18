package core.domain.camera.analysis.nanodet

/** Bounding box in NanoDet's 416 x 416 detector coordinate system. */
internal data class DetectorBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Bounding box in the source CameraX frame coordinate system. */
internal data class SourceBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal data class PlateDetection(
    val boundingBox: DetectorBoundingBox,
    val confidence: Float,
)

internal data class SourcePlateDetection(
    val boundingBox: SourceBoundingBox,
    val confidence: Float,
)
