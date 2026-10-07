package core.presentation.component.navigationmenu.bar

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun NavigationMenuContentItemBar(
    isSelected: Boolean,
    isRippleEnabled: Boolean,
    onItemClick: () -> Unit,
    itemModifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = itemModifier.selectable(
            selected = isSelected,
            interactionSource = null,
            indication = if (isRippleEnabled) LocalIndication.current else null,
            role = Role.Tab,
            onClick = onItemClick
        ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Default icon of a [core.presentation.component.navigationmenu.MenuMode.BottomBar] or
 * [core.presentation.component.navigationmenu.MenuMode.SideBar] item, for use inside `barItemContent`
 * when only some items are drawn differently. Draws nothing when [icon] or [iconTint] is null.
 *
 * @param icon Icon drawable resource.
 * @param iconTint Tint applied to the icon.
 * @param contentDescription Accessibility description of the icon.
 */
@Composable
fun NavigationMenuBarItemIcon(
    icon: DrawableResource?,
    iconTint: Color?,
    contentDescription: String?
) {
    if (icon == null || iconTint == null) return

    val navigationMenuTheme = LocalComponentTheme.current.navigationMenu

    Icon(
        modifier = Modifier.size(navigationMenuTheme.barItemIconSize),
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = iconTint
    )
}
