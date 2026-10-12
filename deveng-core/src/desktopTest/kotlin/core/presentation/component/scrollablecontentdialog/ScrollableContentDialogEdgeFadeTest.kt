package core.presentation.component.scrollablecontentdialog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollableContentDialogEdgeFadeTest {

    @Test
    fun showDialog_atScrollTop_fadesOnlyBottomEdge() = runComposeUiTest {
        showLongDialog()

        with(captureBands()) {
            assertVisible(topEdge, "top edge at scroll top")
            assertFaded(bottomEdge, "bottom edge at scroll top")
            assertVisible(bottomFadeStart, "start of the bottom fade")
            assertOpaque()
        }
    }

    @Test
    fun scrollDialog_toMiddle_fadesBothEdges() = runComposeUiTest {
        showLongDialog()

        scrollBy(MIDDLE_SCROLL_PX)
        with(captureBands()) {
            assertFaded(topEdge, "top edge in the middle")
            assertVisible(topFadeEnd, "end of the top fade")
            assertFaded(bottomEdge, "bottom edge in the middle")
            assertOpaque()
        }
    }

    @Test
    fun scrollDialog_toBottom_fadesOnlyTopEdge() = runComposeUiTest {
        showLongDialog()

        scrollBy(Float.MAX_VALUE)
        with(captureBands()) {
            assertFaded(topEdge, "top edge at scroll bottom")
            assertVisible(bottomEdge, "bottom edge at scroll bottom")
        }
    }

    private fun ComposeUiTest.showLongDialog() {
        val componentTheme = DefaultComponentTheme.copy(
            scrollableContentDialog = DefaultComponentTheme.scrollableContentDialog.copy(
                backgroundColor = Color.White,
                contentTextColor = Color.Black,
                edgeFadeHeight = FADE_HEIGHT_DP.dp
            )
        )
        setContent {
            AppTheme(darkTheme = false, componentTheme = componentTheme) {
                ScrollableContentDialog(
                    title = TITLE,
                    content = LONG_CONTENT,
                    isDialogVisible = true,
                    onBackIconClick = {},
                    onDismissRequest = {}
                )
            }
        }
    }

    private fun ComposeUiTest.scrollBy(pixels: Float) {
        onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, pixels) }
        waitForIdle()
    }

    private fun ComposeUiTest.captureBands(): EdgeBands {
        val scrollNode = onNode(hasScrollAction())
        val scrollTop = scrollNode.fetchSemanticsNode().boundsInRoot.top
        val titleBottom = onNodeWithText(TITLE).fetchSemanticsNode().boundsInRoot.bottom
        val fadeHeightPx = (FADE_HEIGHT_DP * density.density).roundToInt()
        val pixels = scrollNode.captureToImage().toPixelMap()
        val headerBottom = (titleBottom - scrollTop).roundToInt() + HEADER_MARGIN_PX
        val contentBottom = pixels.height - CORNER_MARGIN_PX
        return EdgeBands(
            pixels = pixels,
            topEdge = headerBottom until headerBottom + fadeHeightPx / 3,
            topFadeEnd = headerBottom + fadeHeightPx * 3 / 4 until headerBottom + fadeHeightPx,
            bottomEdge = contentBottom - fadeHeightPx / 3 until contentBottom,
            bottomFadeStart = contentBottom - fadeHeightPx until contentBottom - fadeHeightPx * 3 / 4,
            body = headerBottom until contentBottom
        )
    }

    private fun EdgeBands.assertVisible(rows: IntRange, label: String) = assertTrue(
        actual = darkestLuminance(rows) < VISIBLE_INK_LUMINANCE,
        message = "$label: expected visible text"
    )

    private fun EdgeBands.assertFaded(rows: IntRange, label: String) = assertTrue(
        actual = darkestLuminance(rows) > FADED_INK_LUMINANCE,
        message = "$label: expected faded text"
    )

    private fun EdgeBands.assertOpaque() = assertTrue(
        actual = body.all { y -> (0 until pixels.width).all { x -> pixels[x, y].alpha > OPAQUE_ALPHA } },
        message = "fade must keep the dialog background opaque"
    )

    private class EdgeBands(
        val pixels: PixelMap,
        val topEdge: IntRange,
        val topFadeEnd: IntRange,
        val bottomEdge: IntRange,
        val bottomFadeStart: IntRange,
        val body: IntRange
    ) {
        fun darkestLuminance(rows: IntRange): Float =
            rows.minOf { y -> (0 until pixels.width).minOf { x -> pixels[x, y].run { (red + green + blue) / 3 } } }
    }

    private companion object {
        const val TITLE = "Terms of Use"
        const val FADE_HEIGHT_DP = 100
        const val HEADER_MARGIN_PX = 20
        const val CORNER_MARGIN_PX = 12
        const val MIDDLE_SCROLL_PX = 800f
        const val VISIBLE_INK_LUMINANCE = 0.5f
        const val FADED_INK_LUMINANCE = 0.4f
        const val OPAQUE_ALPHA = 0.99f
        val LONG_CONTENT = List(2000) { "Mmmm" }.joinToString(" ")
    }
}
