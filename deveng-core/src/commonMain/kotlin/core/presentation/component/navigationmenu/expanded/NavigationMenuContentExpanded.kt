package core.presentation.component.navigationmenu.expanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.Slot
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun <T> NavigationMenuContentExpanded(
    width: Dp,
    itemSelectedBackgroundColor: Color,
    itemUnselectedBackgroundColor: Color,
    leadingSlot: Slot? = null,
    trailingSlot: Slot? = null,
    horizontalDividerColor: Color,
    itemList: List<T>,
    isItemSelected: (T) -> Boolean,
    itemText: @Composable (T) -> String,
    itemTextStyle: @Composable (T) -> TextStyle,
    itemIcon: (T) -> DrawableResource?,
    itemIconTint: (T) -> Color?,
    itemIconDescription: @Composable (T) -> String?,
    onItemClick: (T) -> Unit
) {
    val navigationMenuTheme = LocalComponentTheme.current.navigationMenu

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .padding(
                horizontal = navigationMenuTheme.expandedHorizontalPadding,
                vertical = navigationMenuTheme.expandedVerticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(navigationMenuTheme.expandedHeaderItemSpacing)
    ) {
        if (leadingSlot != null) {
            leadingSlot()

            HorizontalDivider(
                thickness = 1.dp,
                color = horizontalDividerColor
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(navigationMenuTheme.expandedItemsSpacing)
        ) {
            itemList.forEach { item ->
                val isSelected = isItemSelected(item)
                NavigationMenuContentItemExpanded(
                    text = itemText(item),
                    textStyle = itemTextStyle(item),
                    icon = itemIcon(item),
                    iconTint = itemIconTint(item),
                    iconDescription = itemIconDescription(item),
                    isSelected = isSelected,
                    backgroundColor = if (isSelected) itemSelectedBackgroundColor else itemUnselectedBackgroundColor,
                    onItemClick = {
                        onItemClick(item)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(
            thickness = 1.dp,
            color = horizontalDividerColor
        )

        if (trailingSlot != null) {
            trailingSlot()
        }
    }
}