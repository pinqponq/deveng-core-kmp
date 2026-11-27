package core.presentation.component.navigationmenu

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class NavigationMenuItem(
    val screenRoute: String,
    val text: StringResource,
    val textColor: Color,
    val icon: DrawableResource? = null,
    val iconTint: Color? = null,
    val iconDescription: StringResource? = null
)