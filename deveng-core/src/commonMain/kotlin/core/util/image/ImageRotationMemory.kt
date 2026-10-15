package core.util.image

private const val ARGB_BYTES_PER_PIXEL = 4L

// Rotating keeps the decoded bitmap and its rotated copy in memory at the same time.
private const val BITMAPS_HELD_DURING_ROTATION = 2L

// Only half of the free heap is spent, the rest stays for JPEG encoding and the rest of the app.
private const val FREE_HEAP_SHARE_DIVISOR = 2L

/**
 * Whether decoding a [width] x [height] image and rotating its pixels fits in [freeHeapBytes].
 * When it does not, callers keep the original bytes and their EXIF orientation tag instead of
 * risking an out-of-memory crash on low-RAM devices.
 */
internal fun isPixelRotationWithinMemory(
    width: Int,
    height: Int,
    freeHeapBytes: Long,
): Boolean {
    if (width <= 0 || height <= 0) return false
    val rotationPeakBytes = width.toLong() * height * ARGB_BYTES_PER_PIXEL * BITMAPS_HELD_DURING_ROTATION
    return rotationPeakBytes <= freeHeapBytes / FREE_HEAP_SHARE_DIVISOR
}
