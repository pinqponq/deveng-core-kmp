package core.presentation.component.navigationmenu.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import core.util.EMPTY
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun NavigationMenuContentItemExpanded(
    text: String = String.EMPTY,
    textStyle: TextStyle,
    icon: DrawableResource?,
    iconSize: Dp? = null,
    iconTint: Color?,
    iconDescription: String?,
    isSelected: Boolean = false,
    backgroundColor: Color,
    onItemClick: () -> Unit
) {
    val navigationMenuTheme = LocalComponentTheme.current.navigationMenu
    val finalIconSize = iconSize ?: navigationMenuTheme.expandedItemIconSize

    Row(
        modifier = Modifier
            .height(navigationMenuTheme.expandedItemHeight)
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(navigationMenuTheme.expandedItemCornerRadius)
            )
            .selectable(
                selected = isSelected,
                onClick = onItemClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(navigationMenuTheme.expandedItemStartPadding))

        if (icon != null && iconTint != null) {
            Icon(
                modifier = Modifier.size(finalIconSize),
                painter = painterResource(icon),
                contentDescription = iconDescription,
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(navigationMenuTheme.expandedItemSpacedBy))
        }

        Text(
            text = text,
            style = textStyle
        )
    }
}