package core.presentation.component.navigationmenu

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Selection indicator that slides to the selected item of a [MenuMode.BottomBar] or [MenuMode.SideBar] menu.
 *
 * @param color Fill color of the indicator.
 * @param shape Shape of the indicator.
 * @param inset Space between the item's cell and the indicator on every side. Ignored when [size] is set.
 * @param size Fixed size of the indicator, centered in the item's cell. If null, the indicator fills the cell minus [inset].
 * @param animationSpec Animation used when the indicator moves to a newly selected item.
 */
data class NavigationMenuIndicator(
    val color: Color,
    val shape: Shape = CircleShape,
    val inset: Dp = 6.dp,
    val size: DpSize? = null,
    val animationSpec: AnimationSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
