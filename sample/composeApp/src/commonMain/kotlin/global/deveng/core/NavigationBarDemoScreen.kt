package global.deveng.core

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomButton
import core.presentation.component.navigationmenu.NavigationBarPlacement
import core.presentation.component.navigationmenu.NavigationMenu
import core.presentation.component.navigationmenu.NavigationMenuIndicator
import core.presentation.component.navigationmenu.bar.NavigationMenuBarItemIcon
import core.presentation.component.navigationmenu.currentNavigationBarPlacement
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.deveng_logo
import deveng_core_kmp.sample.composeapp.generated.resources.ic_cyclone
import deveng_core_kmp.sample.composeapp.generated.resources.ic_dark_mode
import deveng_core_kmp.sample.composeapp.generated.resources.ic_light_mode
import deveng_core_kmp.sample.composeapp.generated.resources.ic_photo_library
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val DEMO_BAR_BACKGROUND_COLOR = Color(0xFF0E0E0E)
private val DEMO_BAR_INDICATOR_COLOR = Color(0xFF262626)
private val DEMO_BAR_SELECTED_ICON_COLOR = Color(0xFFFFC964)
private val DEMO_BAR_UNSELECTED_ICON_COLOR = Color(0xFFE7E7E7)
private val DEMO_BAR_THICKNESS = 54.dp
private val DEMO_BAR_CORNER_RADIUS = 28.dp
private val DEMO_BAR_SHADOW_ELEVATION = 8.dp
private val DEMO_BAR_CENTER_ITEM_SIZE = 36.dp
private const val DEMO_BAR_INDICATOR_DAMPING_RATIO = 0.6f

private enum class NavigationBarDemoDestination(val title: String, val icon: DrawableResource) {
    Today(title = "Today", icon = Res.drawable.ic_light_mode),
    Plans(title = "Plans", icon = Res.drawable.ic_cyclone),
    Camera(title = "Camera", icon = Res.drawable.deveng_logo),
    Gallery(title = "Gallery", icon = Res.drawable.ic_photo_library),
    Profile(title = "Profile", icon = Res.drawable.ic_dark_mode)
}

/**
 * Rindle's bottom bar rebuilt with [NavigationMenu]: sliding indicator, a distinct center item and
 * window-size placement. The buttons force a placement so the side variants can be tried on a phone.
 */
@Composable
internal fun NavigationBarDemoScreen(onBack: () -> Unit) {
    val windowPlacement = currentNavigationBarPlacement()
    var forcedPlacement by remember { mutableStateOf<NavigationBarPlacement?>(null) }
    var selectedDestination by remember { mutableStateOf(NavigationBarDemoDestination.Today) }
    val placement = forcedPlacement ?: windowPlacement

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(placement.contentPadding(barThickness = DEMO_BAR_THICKNESS))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomButton(text = "Back", onClick = onBack)
            Text(text = "Selected: ${selectedDestination.title}")
            Text(text = "Placement: ${placement.name} (window: ${windowPlacement.name})")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomButton(text = "Window", onClick = { forcedPlacement = null })
                NavigationBarPlacement.entries.forEach { placementOption ->
                    CustomButton(text = placementOption.name, onClick = { forcedPlacement = placementOption })
                }
            }
        }

        val barShape = demoBarShape(placement = placement)
        val barAlignment = when (placement) {
            NavigationBarPlacement.Bottom -> Alignment.BottomCenter
            NavigationBarPlacement.Start -> Alignment.CenterStart
            NavigationBarPlacement.End -> Alignment.CenterEnd
        }

        NavigationMenu(
            isExpanded = false,
            menuMode = placement.menuMode,
            menuAlignment = placement.menuAlignment,
            modifier = Modifier
                .align(barAlignment)
                .shadow(elevation = DEMO_BAR_SHADOW_ELEVATION, shape = barShape, clip = false),
            backgroundColor = DEMO_BAR_BACKGROUND_COLOR,
            shape = barShape,
            barThickness = DEMO_BAR_THICKNESS,
            barIndicator = NavigationMenuIndicator(
                color = DEMO_BAR_INDICATOR_COLOR,
                animationSpec = spring(
                    dampingRatio = DEMO_BAR_INDICATOR_DAMPING_RATIO,
                    stiffness = Spring.StiffnessLow
                )
            ),
            barItemContent = { destination, isSelected ->
                if (destination == NavigationBarDemoDestination.Camera) {
                    Image(
                        modifier = Modifier.size(DEMO_BAR_CENTER_ITEM_SIZE),
                        painter = painterResource(destination.icon),
                        contentDescription = destination.title
                    )
                } else {
                    NavigationMenuBarItemIcon(
                        icon = destination.icon,
                        iconTint = if (isSelected) DEMO_BAR_SELECTED_ICON_COLOR else DEMO_BAR_UNSELECTED_ICON_COLOR,
                        contentDescription = destination.title
                    )
                }
            },
            itemList = NavigationBarDemoDestination.entries,
            isItemSelected = { destination -> destination == selectedDestination },
            itemText = { destination -> destination.title },
            itemTextStyle = { TextStyle(fontSize = 12.sp) },
            itemIcon = { destination -> destination.icon },
            itemIconTint = { DEMO_BAR_UNSELECTED_ICON_COLOR },
            itemIconDescription = { destination -> destination.title },
            onItemClick = { destination -> selectedDestination = destination }
        )
    }
}

private fun demoBarShape(placement: NavigationBarPlacement): Shape = when (placement) {
    NavigationBarPlacement.Bottom -> RoundedCornerShape(topStart = DEMO_BAR_CORNER_RADIUS, topEnd = DEMO_BAR_CORNER_RADIUS)
    NavigationBarPlacement.Start -> RoundedCornerShape(topEnd = DEMO_BAR_CORNER_RADIUS, bottomEnd = DEMO_BAR_CORNER_RADIUS)
    NavigationBarPlacement.End -> RoundedCornerShape(topStart = DEMO_BAR_CORNER_RADIUS, bottomStart = DEMO_BAR_CORNER_RADIUS)
}
