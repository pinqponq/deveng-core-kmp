package core.presentation.component.scrollfog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.math.abs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollFogOverlayTest {

    @Test
    fun showFog_whenVisible_coversTopEdgeWithFogColor() = runComposeUiTest {
        var isFogVisible by mutableStateOf(false)
        setContent {
            AppTheme(darkTheme = false) {
                Box(modifier = Modifier.size(CANVAS_SIZE).background(CONTENT_COLOR)) {
                    ScrollFogOverlay(isVisible = isFogVisible, color = FOG_COLOR)
                }
            }
        }

        isFogVisible = true
        mainClock.advanceTimeBy(FOG_SETTLE_MILLIS)

        assertColorNear(FOG_COLOR, topEdgeColor())
    }

    @Test
    fun hideFog_whenNotVisible_leavesContentUncovered() = runComposeUiTest {
        var isFogVisible by mutableStateOf(true)
        setContent {
            AppTheme(darkTheme = false) {
                Box(modifier = Modifier.size(CANVAS_SIZE).background(CONTENT_COLOR)) {
                    ScrollFogOverlay(isVisible = isFogVisible, color = FOG_COLOR)
                }
            }
        }

        isFogVisible = false
        mainClock.advanceTimeBy(FOG_SETTLE_MILLIS)

        assertColorNear(CONTENT_COLOR, topEdgeColor())
    }

    private fun assertColorNear(expected: Color, actual: Color) = assertTrue(
        actual = abs(expected.red - actual.red) < COLOR_TOLERANCE &&
            abs(expected.green - actual.green) < COLOR_TOLERANCE &&
            abs(expected.blue - actual.blue) < COLOR_TOLERANCE,
        message = "expected $expected but was $actual"
    )

    private fun ComposeUiTest.topEdgeColor(): Color {
        val pixels = onRoot().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, 0]
    }

    private companion object {
        const val FOG_SETTLE_MILLIS = 500L
        const val COLOR_TOLERANCE = 0.02f
        val CANVAS_SIZE = 160.dp
        val CONTENT_COLOR = Color(0xFF3D5AFE)
        val FOG_COLOR = Color(0xFFFFF3E0)
    }
}
