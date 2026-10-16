package core.presentation.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class ShimmerHighlightTest {

    @Test
    fun shimmerHighlightStartX_atStart_placesGradientOneWidthBeforeElement() {
        assertEquals(-WIDTH, shimmerHighlightStartX(progress = 0f, width = WIDTH))
    }

    @Test
    fun shimmerHighlightStartX_halfway_alignsGradientWithElement() {
        assertEquals(0f, shimmerHighlightStartX(progress = 0.5f, width = WIDTH))
    }

    @Test
    fun shimmerHighlightStartX_atEnd_placesGradientAtElementEnd() {
        assertEquals(WIDTH, shimmerHighlightStartX(progress = 1f, width = WIDTH))
    }

    private companion object {
        const val WIDTH = 200f
    }
}
