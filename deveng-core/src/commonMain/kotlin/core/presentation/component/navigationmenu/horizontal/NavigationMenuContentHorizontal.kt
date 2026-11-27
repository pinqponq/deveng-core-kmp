package core.presentation.component.navigationmenu.horizontal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import core.presentation.component.Slot
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun <T> NavigationMenuContentHorizontal(
    height: Dp,
    itemSelectedBackgroundColor: Color,
    itemUnselectedBackgroundColor: Color,
    leadingSlot: Slot? = null,
    trailingSlot: Slot? = null,
    horizontalDividerColor: Color,
    isIconVisible: Boolean = true,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(
                horizontal = navigationMenuTheme.expandedHorizontalPadding,
                vertical = navigationMenuTheme.expandedVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(navigationMenuTheme.expandedHeaderItemSpacing)
    ) {
        if (leadingSlot != null) {
            leadingSlot()

            VerticalDivider(
                modifier = Modifier
                    .height(navigationMenuTheme.verticalDividerThickness * 2),
                thickness = navigationMenuTheme.verticalDividerThickness,
                color = horizontalDividerColor
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemList.forEachIndexed { index, item ->
                if (index > 0) {
                    Spacer(modifier = Modifier.width(navigationMenuTheme.expandedItemsSpacing))
                }
                val isSelected = isItemSelected(item)
                val icon = itemIcon(item)
                NavigationMenuContentItemHorizontal(
                    text = itemText(item),
                    textStyle = itemTextStyle(item),
                    icon = icon,
                    iconTint = itemIconTint(item),
                    iconDescription = if (icon != null) itemIconDescription(item) else null,
                    isSelected = isSelected,
                    backgroundColor = if (isSelected) itemSelectedBackgroundColor else itemUnselectedBackgroundColor,
                    isIconVisible = isIconVisible,
                    onItemClick = {
                        onItemClick(item)
                    }
                )
            }
        }

        if (trailingSlot != null) {
            VerticalDivider(
                modifier = Modifier
                    .height(navigationMenuTheme.verticalDividerThickness * 2),
                thickness = navigationMenuTheme.verticalDividerThickness,
                color = horizontalDividerColor
            )
            trailingSlot()
        }
    }
}

