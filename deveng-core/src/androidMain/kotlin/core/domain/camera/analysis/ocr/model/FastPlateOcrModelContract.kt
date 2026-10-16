package core.domain.camera.analysis.ocr.model

/**
 * Architectural constants for the FastPlateOCR v5 model.
 */
internal object FastPlateOcrModelContract {
    const val MAX_PLATE_SLOTS = 10
    const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_"
    const val PAD_CHARACTER = '_'
    const val OUTPUT_CLASSES: Int = 37
    const val OUTPUT_VALUE_COUNT: Int = MAX_PLATE_SLOTS * OUTPUT_CLASSES
}
