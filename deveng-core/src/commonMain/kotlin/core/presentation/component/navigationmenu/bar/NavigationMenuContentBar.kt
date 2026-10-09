package core.presentation.component.navigationmenu.bar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import core.presentation.component.navigationmenu.NavigationMenuIndicator
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun <T> NavigationMenuContentBar(
    isVertical: Boolean,
    indicator: NavigationMenuIndicator?,
    isItemRippleEnabled: Boolean,
    itemList: List<T>,
    isItemSelected: (T) -> Boolean,
    itemContent: (@Composable (item: T, isSelected: Boolean) -> Unit)?,
    itemIcon: (T) -> DrawableResource?,
    itemIconTint: (T) -> Color?,
    itemIconDescription: @Composable (T) -> String?,
    onItemClick: (T) -> Unit
) {
    val selectedItemIndex = itemList.indexOfFirst(isItemSelected)

    // The indicator stays on the last selected item while no item is selected, so it does not
    // jump to the first item on destinations that are not part of the menu.
    var indicatorItemIndex by remember { mutableIntStateOf(selectedItemIndex.coerceAtLeast(0)) }
    LaunchedEffect(selectedItemIndex) {
        if (selectedItemIndex >= 0) {
            indicatorItemIndex = selectedItemIndex
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (indicator != null && itemList.isNotEmpty()) {
            val mainAxisLength = if (isVertical) maxHeight else maxWidth
            NavigationMenuBarIndicator(
                indicator = indicator,
                isVertical = isVertical,
                itemCellLength = mainAxisLength / itemList.size,
                itemIndex = indicatorItemIndex
            )
        }

        val itemContentOrIcon: @Composable (T, Boolean) -> Unit = itemContent ?: { item, _ ->
            NavigationMenuBarItemIcon(
                icon = itemIcon(item),
                iconTint = itemIconTint(item),
                contentDescription = itemIconDescription(item)
            )
        }

        if (isVertical) {
            Column(modifier = Modifier.fillMaxSize().selectableGroup()) {
                itemList.forEach { item ->
                    val isSelected = isItemSelected(item)
                    NavigationMenuContentItemBar(
                        isSelected = isSelected,
                        isRippleEnabled = isItemRippleEnabled,
                        onItemClick = { onItemClick(item) },
                        itemModifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        itemContentOrIcon(item, isSelected)
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
                itemList.forEach { item ->
                    val isSelected = isItemSelected(item)
                    NavigationMenuContentItemBar(
                        isSelected = isSelected,
                        isRippleEnabled = isItemRippleEnabled,
                        onItemClick = { onItemClick(item) },
                        itemModifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        itemContentOrIcon(item, isSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationMenuBarIndicator(
    indicator: NavigationMenuIndicator,
    isVertical: Boolean,
    itemCellLength: Dp,
    itemIndex: Int
) {
    val indicatorOffset by animateDpAsState(
        targetValue = itemCellLength * itemIndex,
        animationSpec = indicator.animationSpec,
        label = "navigationMenuBarIndicatorOffset"
    )

    val cellModifier = if (isVertical) {
        Modifier
            .offset(y = indicatorOffset)
            .fillMaxWidth()
            .height(itemCellLength)
    } else {
        Modifier
            .offset(x = indicatorOffset)
            .fillMaxHeight()
            .width(itemCellLength)
    }

    val indicatorSize = indicator.size
    val indicatorModifier = if (indicatorSize != null) {
        Modifier.size(indicatorSize)
    } else {
        Modifier
            .fillMaxSize()
            .padding(indicator.inset)
    }

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = indicatorModifier.background(
                color = indicator.color,
                shape = indicator.shape
            )
        )
    }
}
