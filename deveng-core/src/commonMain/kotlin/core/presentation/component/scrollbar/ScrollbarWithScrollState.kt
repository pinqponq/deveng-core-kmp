package core.presentation.component.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import core.presentation.theme.LocalComponentTheme

/**
 * A modifier extension that adds a vertical scrollbar to a scrollable composable (Column, LazyColumn, etc.).
 * The scrollbar position and size are calculated based on the ScrollState.
 *
 * @param scrollState The ScrollState of the scrollable composable to track.
 * @param alwaysShowScrollBar Whether to always show the scrollbar even when content doesn't require scrolling. If null, uses theme default.
 * @param width Width of the scrollbar. If null, uses theme default.
 * @param isScrollBarTrackVisible Whether to display the scrollbar track (background). If null, uses theme default.
 * @param scrollBarTrackColor Color of the scrollbar track. If null, uses theme default.
 * @param scrollBarColor Color of the scrollbar thumb (the draggable part). If null, uses theme default.
 * @param scrollBarCornerRadius Corner radius of the scrollbar and track. If null, uses theme default.
 * @param endPadding Padding from the end (right side in LTR). If null, uses theme default.
 * @param topPadding Top padding for the scrollbar. If null, uses theme default.
 * @param bottomPadding Bottom padding for the scrollbar. If null, uses theme default.
 * @return Modifier with scrollbar drawing applied.
 */
@Composable
fun Modifier.scrollbarWithScrollState(
    scrollState: ScrollState,
    alwaysShowScrollBar: Boolean? = null,
    width: Dp? = null,
    isScrollBarTrackVisible: Boolean? = null,
    scrollBarTrackColor: Color? = null,
    scrollBarColor: Color? = null,
    scrollBarCornerRadius: Float? = null,
    endPadding: Float? = null,
    topPadding: Dp? = null,
    bottomPadding: Dp? = null
): Modifier {
    val componentTheme = LocalComponentTheme.current
    val scrollbarTheme = componentTheme.scrollbarWithScrollState

    val finalAlwaysShowScrollbar = alwaysShowScrollBar ?: scrollbarTheme.alwaysShowScrollBar
    val finalWidth = width ?: scrollbarTheme.scrollBarWidth
    val finalIsTrackVisible =
        isScrollBarTrackVisible ?: scrollbarTheme.isScrollBarTrackVisible
    val finalTrackColor = scrollBarTrackColor ?: scrollbarTheme.scrollBarTrackColor
    val finalScrollBarColor = scrollBarColor ?: scrollbarTheme.scrollBarColor
    val finalCornerRadius = scrollBarCornerRadius ?: scrollbarTheme.scrollBarCornerRadius
    val finalEndPadding = endPadding ?: scrollbarTheme.scrollBarEndPadding
    val finalTopPadding = topPadding ?: scrollbarTheme.scrollBarTopPadding
    val finalBottomPadding = bottomPadding ?: scrollbarTheme.scrollBarBottomPadding

    val shouldShowScrollbar =
        finalAlwaysShowScrollbar || isContentSizeEnoughToShowScrollbar(scrollState)

    return if (shouldShowScrollbar) {
        this.then(
            drawWithContent {
                drawContent()

                val viewportHeight = this.size.height -
                        (finalTopPadding.toPx() + finalBottomPadding.toPx())
                val totalScrollRange = scrollState.maxValue.toFloat()
                val totalContentHeight = viewportHeight + totalScrollRange
                val scrollValue = scrollState.value.toFloat()

                val scrollBarHeight =
                    (viewportHeight / totalContentHeight.coerceAtLeast(viewportHeight)) * viewportHeight
                val scrollableHeight = (viewportHeight - scrollBarHeight).coerceAtLeast(0f)

                val scrollBarStartOffset =
                    (scrollValue / totalScrollRange.coerceAtLeast(1f)) * scrollableHeight +
                            finalTopPadding.toPx()

                if (finalIsTrackVisible) {
                    drawRoundRect(
                        cornerRadius = CornerRadius(finalCornerRadius),
                        color = finalTrackColor,
                        topLeft = Offset(this.size.width - finalEndPadding, finalTopPadding.toPx()),
                        size = Size(finalWidth.toPx(), viewportHeight),
                    )
                }

                drawRoundRect(
                    cornerRadius = CornerRadius(finalCornerRadius),
                    color = finalScrollBarColor,
                    topLeft = Offset(this.size.width - finalEndPadding, scrollBarStartOffset),
                    size = Size(finalWidth.toPx(), scrollBarHeight)
                )
            }
        )
    } else {
        this
    }
}

@Composable
fun isContentSizeEnoughToShowScrollbar(scrollState: ScrollState): Boolean {
    return scrollState.maxValue > 0
}
