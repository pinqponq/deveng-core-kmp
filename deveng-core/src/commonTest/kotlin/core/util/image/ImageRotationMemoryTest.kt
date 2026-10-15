package core.util.image

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageRotationMemoryTest {

    @Test
    fun isPixelRotationWithinMemory_whenTwoBitmapsFitInHalfTheFreeHeap_returnsTrue() {
        val isWithinMemory = isPixelRotationWithinMemory(
            width = TWELVE_MEGAPIXEL_WIDTH,
            height = TWELVE_MEGAPIXEL_HEIGHT,
            freeHeapBytes = TWELVE_MEGAPIXEL_ROTATION_PEAK_BYTES * 2,
        )

        assertTrue(isWithinMemory)
    }

    @Test
    fun isPixelRotationWithinMemory_whenOneByteShortOfHalfTheFreeHeap_returnsFalse() {
        val isWithinMemory = isPixelRotationWithinMemory(
            width = TWELVE_MEGAPIXEL_WIDTH,
            height = TWELVE_MEGAPIXEL_HEIGHT,
            freeHeapBytes = TWELVE_MEGAPIXEL_ROTATION_PEAK_BYTES * 2 - 2,
        )

        assertFalse(isWithinMemory)
    }

    @Test
    fun isPixelRotationWithinMemory_onLowRamFreeHeap_returnsFalse() {
        val isWithinMemory = isPixelRotationWithinMemory(
            width = TWELVE_MEGAPIXEL_WIDTH,
            height = TWELVE_MEGAPIXEL_HEIGHT,
            freeHeapBytes = LOW_RAM_FREE_HEAP_BYTES,
        )

        assertFalse(isWithinMemory)
    }

    @Test
    fun isPixelRotationWithinMemory_withHugeDimensions_doesNotOverflowAndReturnsFalse() {
        val isWithinMemory = isPixelRotationWithinMemory(
            width = 50_000,
            height = 50_000,
            freeHeapBytes = LOW_RAM_FREE_HEAP_BYTES,
        )

        assertFalse(isWithinMemory)
    }

    @Test
    fun isPixelRotationWithinMemory_withUnknownDimensions_returnsFalse() {
        assertFalse(isPixelRotationWithinMemory(width = 0, height = TWELVE_MEGAPIXEL_HEIGHT, freeHeapBytes = Long.MAX_VALUE))
        assertFalse(isPixelRotationWithinMemory(width = TWELVE_MEGAPIXEL_WIDTH, height = 0, freeHeapBytes = Long.MAX_VALUE))
        assertFalse(isPixelRotationWithinMemory(width = -1, height = -1, freeHeapBytes = Long.MAX_VALUE))
    }

    private companion object {
        const val TWELVE_MEGAPIXEL_WIDTH = 4_000
        const val TWELVE_MEGAPIXEL_HEIGHT = 3_000

        // 4000 x 3000 px x 4 bytes (ARGB) x 2 bitmaps (decoded + rotated)
        const val TWELVE_MEGAPIXEL_ROTATION_PEAK_BYTES = 96_000_000L
        const val LOW_RAM_FREE_HEAP_BYTES = 120_000_000L
    }
}
