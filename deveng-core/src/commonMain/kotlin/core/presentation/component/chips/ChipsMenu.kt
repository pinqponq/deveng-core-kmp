package core.presentation.component.chips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable horizontally scrollable row of chips (selection menu).
 * Displays a list of options with support for selection states, custom spacing,
 * and visual fade-out (brush) effects at the edges to indicate scrollability.
 *
 * @param modifier Modifier to be applied to the outermost LazyRow container.
 * @param optionList The list of items to be displayed as selectable chips.
 * @param optionText A lambda that provides the text representation for a given item.
 * @param selectedOption The currently selected item. If null, no chip is highlighted as selected.
 * @param spaceBetweenItems The horizontal spacing between each chip. If null, uses theme default.
 * @param startBrushWidth The width of the fade-out effect at the start (left) of the row. If null, uses theme default.
 * @param endBrushWidth The width of the fade-out effect at the end (right) of the row. If null, uses theme default.
 * @param isStartBrushVisible Whether the fade-out effect at the start of the row is enabled when scrolling backward. Default is true.
 * @param isEndBrushVisible Whether the fade-out effect at the end of the row is enabled when scrolling forward. Default is true.
 * @param onOptionClick Callback invoked when a chip item is clicked, providing the clicked item.
 */

@Composable
fun <T> ChipsMenu(
    modifier: Modifier = Modifier,
    optionList: List<T>,
    optionText: (T) -> String,
    selectedOption: T? = null,
    spaceBetweenItems: Dp? = null,
    startBrushWidth: Dp? = null,
    endBrushWidth: Dp? = null,
    isStartBrushVisible: Boolean = true,
    isEndBrushVisible: Boolean = true,
    onOptionClick: (T) -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val chipsMenuTheme = componentTheme.chipsMenu

    val finalSpaceBetweenItems = spaceBetweenItems ?: chipsMenuTheme.spaceBetweenItems
    val finalStartBrushWidth = startBrushWidth ?: chipsMenuTheme.startBrushWidth
    val finalEndBrushWidth = endBrushWidth ?: chipsMenuTheme.endBrushWidth

    val lazyListState = rememberLazyListState()

    LazyRow(
        state = lazyListState,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()

                if (isStartBrushVisible && lazyListState.canScrollBackward) {
                    val colors = listOf(Color.Black, Color.Transparent)
                    val fadeWidthPx = finalStartBrushWidth.toPx()

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = colors,
                            startX = fadeWidthPx,
                            endX = 0f
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }

                if (isEndBrushVisible && lazyListState.canScrollForward) {
                    val colors = listOf(Color.Black, Color.Transparent)
                    val width = size.width
                    val fadeWidthPx = finalEndBrushWidth.toPx()

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = colors,
                            startX = width - fadeWidthPx,
                            endX = width
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            },
        horizontalArrangement = Arrangement.Start
    ) {
        optionList.forEachIndexed { index, item ->
            item {
                ChipItem(
                    text = optionText(item),
                    isSelected = item == selectedOption,
                    onClick = { onOptionClick(item) }
                )

                val isLastItem = index == optionList.lastIndex

                if (!isLastItem) {
                    Spacer(modifier = Modifier.width(finalSpaceBetweenItems))
                }
            }
        }
    }
}

@Preview
@Composable
fun ChipsMenuPreview() {
    val campList = listOf("Kamp Alanları", "Kamp Malzemeleri", "Satın Alım", "Kiralama")

    AppTheme {
        ChipsMenu(
            optionList = campList,
            optionText = { it },
            onOptionClick = {}
        )
    }
}