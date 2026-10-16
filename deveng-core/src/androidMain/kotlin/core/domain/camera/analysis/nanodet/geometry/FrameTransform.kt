package core.domain.camera.analysis.nanodet.geometry

/**
 * Geometry applied between the source camera frame and the detector input.
 *
 * [scaleX] and [scaleY] convert coordinates from the rotated source image to
 * detector coordinates. Padding values are expressed in detector coordinates.
 */
internal data class FrameTransform(
    val sourceWidth: Int,  // kamera genişliği
    val sourceHeight: Int,  // kamera yüksekliği
    val rotatedWidth: Int, // rotasyon sonrası genişlik
    val rotatedHeight: Int,  // rotasyon sonrası yükseklik
    val inputWidth: Int,  // modelin input genişliği
    val inputHeight: Int,  // modelin input yüksekliği
    val scaleX: Float,  // yatayda küçültme oranı (yani kamera genişliği -> model genişliği dönüşümündeki oran)
    val scaleY: Float,  // düşeyde küçültme oranı (yani kamera yüksekliği -> model yüksekliği dönüşümündeki oran)
    val paddingX: Float = 0f,  // yatay padding
    val paddingY: Float = 0f,  // düşey padding
    val rotationDegrees: Int  // kamera sensöründen gelen açı
) {
    companion object {

        /** Creates the transform used by the current direct, non-letterboxed resize paths. */
        fun directResize(
            sourceWidth: Int,
            sourceHeight: Int,
            inputWidth: Int,
            inputHeight: Int,
            rotationDegrees: Int
        ): FrameTransform {

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
                rotationDegrees = rotationDegrees
            )
        }
    }
}
