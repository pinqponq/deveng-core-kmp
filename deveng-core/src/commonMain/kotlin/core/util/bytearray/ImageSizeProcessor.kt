package core.util.bytearray

expect class ImageSizeProcessor {
    /**
     * @param inputBytes      Original encoded image bytes (JPEG/PNG/etc).
     * @param targetMaxSizePx Max size of the longest side in pixels
     * @param quality 1..100 (JPEG-like semantics). Clamp outside values.
     *
     * @return New encoded image bytes after resizing/compression.
     */
    suspend fun resizeAndCompressBytes(
        inputBytes: ByteArray,
        targetMaxSizePx: Int,
        quality: Int
    ): ByteArray
}

/**
 * Predefined image processing presets.
 *
 * These profiles are designed to produce predictable output file sizes
 * for typical smartphone images (12-50 MP, 3000-8000px on long side).
 *
 * Scaling formula: targetMaxSizePx limits the longest side while maintaining aspect ratio.
 * Quality: JPEG compression level (1-100), where higher values preserve more detail.
 *
 * Profiles are ordered from highest to lowest quality/size.
 */
enum class ImageProcessingProfile(
    val targetMaxSizePx: Int,
    val quality: Int,
    val description: String
) {
    /**
     * HIGH (3000px @ 100% quality)
     *
     * - CPU: heavy
     * - Use case: High-detail imagery, professional photos, zoom-sensitive content
     * - For 4080×3060px (6 MB): Resizes to 3000×2250px → ~900 KB - 1.3 MB
     * - Typical output: 900 KB - 1.5 MB for standard smartphone photos
     */
    HIGH(
        targetMaxSizePx = 3000,
        quality = 100,
        description = "Highest quality, ~900 KB - 1.5 MB, for professional and detail-sensitive imagery."
    ),

    /**
     * MEDIUM_HIGH (2200px @ 95% quality)
     *
     * - CPU: moderate-heavy
     * - Use case: High-quality general purpose, product photos, social media premium
     * - For 4080×3060px (6 MB): Resizes to 2200×1650px → ~500-850 KB
     * - Typical output: 500-900 KB for standard smartphone photos
     */
    MEDIUM_HIGH(
        targetMaxSizePx = 2200,
        quality = 95,
        description = "High quality, ~500-900 KB, for premium product photos and social media."
    ),

    /**
     * MEDIUM (1600px @ 90% quality)
     *
     * - CPU: moderate
     * - Use case: General purpose, product photos, social media, standard uploads
     * - For 4080×3060px (6 MB): Resizes to 1600×1200px → ~280-450 KB
     * - Typical output: 280-520 KB for standard smartphone photos
     */
    MEDIUM(
        targetMaxSizePx = 1600,
        quality = 90,
        description = "Balanced quality, ~280-520 KB, recommended default for most use cases."
    ),

    /**
     * MEDIUM_LOW (800px @ 85% quality)
     *
     * - CPU: light-moderate
     * - Use case: Quick uploads, preview images, moderate compression needs
     * - For 4080×3060px (6 MB): Resizes to 800×600px → ~90-170 KB
     * - Typical output: 90-190 KB for standard smartphone photos
     */
    MEDIUM_LOW(
        targetMaxSizePx = 800,
        quality = 85,
        description = "Moderate compression, ~90-190 KB, for quick uploads and preview images."
    ),

    /**
     * LOW (320px @ 80% quality)
     *
     * - CPU: light
     * - Use case: App thumbnails, icon-sized images, minimal bandwidth usage
     * - For 4080×3060px (6 MB): Resizes to 320×240px → ~18-35 KB
     * - Typical output: 18-45 KB for app thumbnails
     *
     * Note: This profile produces very small thumbnail-sized images suitable for
     * app icons, list thumbnails, and other space-constrained UI elements.
     * Lower quality (80%) is acceptable at this tiny size as visual artifacts
     * are minimal when the image is already significantly downscaled.
     */
    LOW(
        targetMaxSizePx = 320,
        quality = 80,
        description = "Thumbnail size, ~18-45 KB, for app thumbnails and minimal bandwidth usage."
    );
}

/**
 * Convenience overload using a predefined [ImageProcessingProfile].
 *
 * This allows clean call sites without passing magic numbers.
 */
suspend fun ImageSizeProcessor.resizeAndCompressBytes(
    inputBytes: ByteArray,
    profile: ImageProcessingProfile
): ByteArray {
    return resizeAndCompressBytes(
        inputBytes = inputBytes,
        targetMaxSizePx = profile.targetMaxSizePx,
        quality = profile.quality
    )
}