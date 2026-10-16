package core.domain.camera.analysis.nanodet.model

/** Bounding box of a plate. */
internal data class PlateBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** Result of a plate detection. */
internal data class PlateDetectionResult(
    val boundingBox: PlateBoundingBox,
    val confidence: Float
)