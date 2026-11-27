package core.presentation.component.tabrow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.util.ifTrue
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable tab row component that supports both fixed and scrollable layouts.
 * It displays a list of tabs, highlights the selected tab with custom styling and an indicator line,
 * handles user interaction, and provides a slot for the corresponding content below.
 * Includes optional fade-out (brush) effects at the edges when scrolling is available.
 *
 * @param modifier Modifier to be applied to the outermost container of the tab row component.
 * @param tabs The list of items to be displayed as tabs.
 * @param selectedTab The currently selected tab item.
 * @param tabTitle A composable lambda that provides the title string for a given tab item.
 * @param tabHeight Height of the tab row. If null, uses theme default.
 * @param contentSpacing Spacing between the tab row and the main content below it. If null, uses theme default.
 * @param selectedTabBackgroundColor Background color of the selected tab. If null, uses theme default.
 * @param unSelectedTabBackgroundColor Background color of the unselected tabs. If null, uses theme default.
 * @param selectedTabTitleTextStyle Text style for the selected tab's title. If null, uses theme default.
 * @param unSelectedTabTitleTextStyle Text style for the unselected tabs' titles. If null, uses theme default.
 * @param selectedTabNameColor Text color of the selected tab's title. If null, uses theme default.
 * @param unSelectedTabNameColor Text color of the unselected tabs' titles. If null, uses theme default.
 * @param selectedTabLineColor Color of the bottom indicator line for the selected tab. If null, uses theme default.
 * @param unSelectedTabLineColor Color of the bottom indicator line for the unselected tabs. If null, uses theme default.
 * @param selectedTabLineHeight Height (thickness) of the bottom indicator line for the selected tab. If null, uses theme default.
 * @param unSelectedTabLineHeight Height (thickness) of the bottom indicator line for the unselected tabs. If null, uses theme default.
 * @param startBrushWidth Width of the fade-out brush effect at the start of the scrollable row. If null, uses theme default.
 * @param endBrushWidth Width of the fade-out brush effect at the end of the scrollable row. If null, uses theme default.
 * @param isSelectedTabLineVisible Whether the bottom indicator line is visible for the selected tab. Default is true.
 * @param isStartBrushVisible Whether the start fade-out brush effect is enabled in scrollable mode. Default is true.
 * @param isEndBrushVisible Whether the end fade-out brush effect is enabled in scrollable mode. Default is true.
 * @param layoutStyle Defines the layout behavior of the tabs (e.g., FIXED or SCROLLABLE). Default is [LayoutStyle.FIXED].
 * @param onClickTabItem Callback invoked when a tab item is clicked.
 * @param content The composable content to be displayed below the tab row, scoped to the currently selected tab.
 */

@Composable
fun <T> TabRow(
    modifier: Modifier = Modifier,
    tabs: List<T>,
    selectedTab: T,
    tabTitle: @Composable (T) -> String,
    tabHeight: Dp? = null,
    contentSpacing: Dp? = null,
    selectedTabBackgroundColor: Color? = null,
    unSelectedTabBackgroundColor: Color? = null,
    selectedTabTitleTextStyle: TextStyle? = null,
    unSelectedTabTitleTextStyle: TextStyle? = null,
    selectedTabNameColor: Color? = null,
    unSelectedTabNameColor: Color? = null,
    selectedTabLineColor: Color? = null,
    unSelectedTabLineColor: Color? = null,
    selectedTabLineHeight: Dp? = null,
    unSelectedTabLineHeight: Dp? = null,
    startBrushWidth: Dp? = null,
    endBrushWidth: Dp? = null,
    isSelectedTabLineVisible: Boolean = true,
    isStartBrushVisible: Boolean = true,
    isEndBrushVisible: Boolean = true,
    layoutStyle: LayoutStyle = LayoutStyle.FIXED,
    onClickTabItem: (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val tabRowTheme = componentTheme.tabRow

    val finalTabHeight = tabHeight ?: tabRowTheme.tabHeight
    val finalContentSpacing = contentSpacing ?: tabRowTheme.contentSpacing
    val finalSelectedTabBackgroundColor =
        selectedTabBackgroundColor ?: tabRowTheme.selectedTabBackgroundColor
    val finalUnselectedTabBackgroundColor =
        unSelectedTabBackgroundColor ?: tabRowTheme.unselectedTabBackgroundColor
    val finalSelectedTabTitleTextStyle =
        selectedTabTitleTextStyle ?: tabRowTheme.selectedTabTitleTextStyle
    val finalUnselectedTabTitleTextStyle =
        unSelectedTabTitleTextStyle ?: tabRowTheme.unselectedTabTitleTextStyle
    val finalSelectedTabNameColor = selectedTabNameColor ?: tabRowTheme.selectedTabNameColor
    val finalUnselectedTabNameColor =
        unSelectedTabNameColor ?: tabRowTheme.unselectedTabNameColor
    val finalSelectedTabLineColor = selectedTabLineColor ?: tabRowTheme.selectedTabLineColor
    val finalUnselectedTabLineColor =
        unSelectedTabLineColor ?: tabRowTheme.unselectedTabLineColor
    val finalSelectedTabLineHeight =
        selectedTabLineHeight ?: tabRowTheme.selectedTabLineHeight
    val finalUnselectedTabLineHeight =
        unSelectedTabLineHeight ?: tabRowTheme.unselectedTabLineHeight
    val finalStartBrushWidth = startBrushWidth ?: tabRowTheme.startBrushWidth
    val finalEndBrushWidth = endBrushWidth ?: tabRowTheme.endBrushWidth

    val isScrollable = layoutStyle == LayoutStyle.SCROLLABLE
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .ifTrue(isScrollable) {
                    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
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
                        }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isTabSelected = tab == selectedTab
                val finalTabBackgroundColor =
                    if (isTabSelected) finalSelectedTabBackgroundColor else finalUnselectedTabBackgroundColor
                val finalTabTitleTextStyle =
                    if (isTabSelected) finalSelectedTabTitleTextStyle else finalUnselectedTabTitleTextStyle
                val finalTabLineColor =
                    if (isTabSelected) finalSelectedTabLineColor else finalUnselectedTabLineColor
                val finalTabLineHeight =
                    if (isTabSelected) finalSelectedTabLineHeight else finalUnselectedTabLineHeight

                item {
                    Box(
                        modifier = Modifier
                            .height(finalTabHeight)
                            .ifTrue(!isScrollable) {
                                fillParentMaxWidth(1f / tabs.size)
                            }
                            .background(color = finalTabBackgroundColor)
                            .clickable(onClick = { onClickTabItem(tab) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.ifTrue(isScrollable) {
                                padding(horizontal = 16.dp)
                            },
                            text = tabTitle(tab),
                            style = finalTabTitleTextStyle.copy(
                                color = if (isTabSelected) {
                                    finalSelectedTabNameColor
                                } else finalUnselectedTabNameColor
                            ),
                            textAlign = TextAlign.Center
                        )

                        val shouldDrawLine =
                            (isTabSelected && isSelectedTabLineVisible) || !isTabSelected

                        if (shouldDrawLine) {
                            Box(
                                modifier = Modifier.matchParentSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(finalTabLineHeight)
                                        .background(finalTabLineColor)
                                        .align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (finalContentSpacing > 0.dp) {
            Spacer(modifier = Modifier.height(finalContentSpacing))
        }

        content(selectedTab)
    }
}

@Preview
@Composable
fun TabRowPreview() {
    AppTheme {
        TabRow(
            tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
            selectedTab = "Tab 1",
            tabTitle = { it },
            onClickTabItem = {},
            content = {
                Text(text = it)
            }
        )
    }
}