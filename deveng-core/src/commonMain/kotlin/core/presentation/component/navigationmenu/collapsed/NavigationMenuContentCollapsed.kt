package core.presentation.component.navigationmenu.collapsed

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.Slot
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun <T> NavigationMenuContentCollapsed(
    width: Dp,
    itemSelectedBackgroundColor: Color,
    itemUnselectedBackgroundColor: Color,
    horizontalDividerColor: Color,
    leadingSlot: Slot? = null,
    trailingSlot: Slot? = null,
    itemList: List<T>,
    isItemSelected: (T) -> Boolean,
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
                horizontal = navigationMenuTheme.collapsedHorizontalPadding,
                vertical = navigationMenuTheme.collapsedVerticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(navigationMenuTheme.collapsedHeaderItemSpacing)
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
            verticalArrangement = Arrangement.spacedBy(navigationMenuTheme.collapsedItemsSpacing)
        ) {
            itemList.forEach { item ->
                val isSelected = isItemSelected(item)
                NavigationMenuContentItemCollapsed(
                    icon = itemIcon(item),
                    iconTint = itemIconTint(item),
                    contentDescription = itemIconDescription(item),
                    backgroundColor = if (isSelected) itemSelectedBackgroundColor else itemUnselectedBackgroundColor,
                    isSelected = isSelected,
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