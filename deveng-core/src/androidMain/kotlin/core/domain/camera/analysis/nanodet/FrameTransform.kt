package core.domain.camera.analysis.nanodet

/**
 * Geometry applied between the source camera frame and the detector input.
 *
 * [scaleX] and [scaleY] convert coordinates from the rotated source image to
 * detector coordinates. Padding values are expressed in detector coordinates.
 * [mirrored] means the detector-space image is horizontally mirrored after
 * rotation and before the resize is inverted.
 */
internal data class FrameTransform(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val rotatedWidth: Int,
    val rotatedHeight: Int,
    val inputWidth: Int,
    val inputHeight: Int,
    val scaleX: Float,
    val scaleY: Float,
    val paddingX: Float = 0f,
    val paddingY: Float = 0f,
    val rotationDegrees: Int,
    val mirrored: Boolean = false,
) {
    init {
        require(sourceWidth > 0 && sourceHeight > 0) {
            "Source frame dimensions must be positive."
        }
        require(inputWidth > 0 && inputHeight > 0) {
            "Detector input dimensions must be positive."
        }
        require(rotationDegrees in SUPPORTED_ROTATIONS) {
            "Unsupported frame rotation: $rotationDegrees"
        }
        val expectedRotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            sourceHeight
        } else {
            sourceWidth
        }
        val expectedRotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            sourceWidth
        } else {
            sourceHeight
        }
        require(rotatedWidth == expectedRotatedWidth && rotatedHeight == expectedRotatedHeight) {
            "Rotated dimensions ${rotatedWidth}x$rotatedHeight do not match the source " +
                "${sourceWidth}x$sourceHeight at rotation $rotationDegrees."
        }
        require(scaleX.isFinite() && scaleX > 0f && scaleY.isFinite() && scaleY > 0f) {
            "Transform scale must be finite and positive."
        }
        require(paddingX.isFinite() && paddingX >= 0f && paddingY.isFinite() && paddingY >= 0f) {
            "Transform padding must be finite and non-negative."
        }
    }

    companion object {
        private val SUPPORTED_ROTATIONS = setOf(0, 90, 180, 270)

        /** Creates the transform used by the current direct, non-letterboxed resize paths. */
        fun directResize(
            sourceWidth: Int,
            sourceHeight: Int,
            inputWidth: Int,
            inputHeight: Int,
            rotationDegrees: Int,
            mirrored: Boolean = false,
        ): FrameTransform {
            require(rotationDegrees in SUPPORTED_ROTATIONS) {
                "Unsupported frame rotation: $rotationDegrees"
            }
            val rotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
                sourceHeight
            } else {
                sourceWidth
            }
            val rotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
                sourceWidth
            } else {
                sourceHeight
            }
            return FrameTransform(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                rotatedWidth = rotatedWidth,
                rotatedHeight = rotatedHeight,
                inputWidth = inputWidth,
                inputHeight = inputHeight,
                scaleX = inputWidth.toFloat() / rotatedWidth,
                scaleY = inputHeight.toFloat() / rotatedHeight,
                rotationDegrees = rotationDegrees,
                mirrored = mirrored,
            )
        }
    }
}
