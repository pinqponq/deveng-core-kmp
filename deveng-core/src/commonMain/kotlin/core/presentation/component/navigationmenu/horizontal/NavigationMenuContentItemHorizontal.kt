package core.presentation.component.navigationmenu.horizontal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun NavigationMenuContentItemHorizontal(
    text: String,
    textStyle: TextStyle,
    icon: DrawableResource?,
    iconSize: Dp? = null,
    iconTint: Color?,
    iconDescription: String?,
    isSelected: Boolean = false,
    backgroundColor: Color,
    isIconVisible: Boolean = true,
    onItemClick: () -> Unit
) {
    val navigationMenuTheme = LocalComponentTheme.current.navigationMenu
    val finalIconSize = iconSize ?: navigationMenuTheme.expandedItemIconSize
    val showIcon = isIconVisible && icon != null && iconTint != null

    Row(
        modifier = Modifier
            .height(navigationMenuTheme.expandedItemHeight)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(navigationMenuTheme.expandedItemCornerRadius)
            )
            .selectable(
                selected = isSelected,
                onClick = onItemClick
            )
            .padding(horizontal = navigationMenuTheme.expandedItemStartPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(navigationMenuTheme.expandedItemSpacedBy)
    ) {
        if (showIcon) {
            Icon(
                modifier = Modifier.size(finalIconSize),
                painter = painterResource(icon),
                contentDescription = iconDescription,
                tint = iconTint
            )
        }

        Text(
            text = text,
            style = textStyle
        )
    }
}

