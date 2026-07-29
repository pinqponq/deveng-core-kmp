package core.presentation.component.scrolltoedgebutton

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import core.presentation.component.CustomIconButton
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.presentation.theme.ScrollToEdgeButtonTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_desc_scroll_to_bottom
import global.deveng.deveng_core.generated.resources.shared_content_desc_scroll_to_top
import global.deveng.deveng_core.generated.resources.shared_ic_angle_down
import global.deveng.deveng_core.generated.resources.shared_ic_angle_up
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val SCROLL_EDGE_SLIDE_FRACTION = 2

/**
 * A scroll-to-edge button: it shows once [listState] is more than [threshold] away from [target]
 * and, when tapped, smoothly scrolls to that edge. Drop it into a `Box` and align it wherever you
 * like — no visibility state or coroutine wiring needed. Pass [target] to choose direction:
 * [ScrollTarget.Top] scrolls back to the first item, [ScrollTarget.Bottom] to the last.
 *
 * [threshold] and [iconSize] default to the theme's `scrollToEdgeButton` values; the button
 * container styling comes from the theme's `iconButton`. Any optional parameter overrides its
 * theme default for a single call site.
 *
 * @param listState the list this button controls and observes.
 * @param target the edge to scroll toward and measure visibility against.
 * @param modifier Modifier applied to the button (use it to align/position within a parent).
 * @param threshold scroll distance before the button appears. If null, uses the theme default.
 * @param iconSize size of the chevron icon. If null, uses the theme default.
 * @param onScrollClick optional click override. When null, scrolls [listState] to [target] with animation.
 * @param icon optional icon override. When null, a chevron pointing toward [target] is used.
 * @param contentDescription optional accessibility description override.
 * @param backgroundColor optional background color override; falls back to the icon button theme.
 * @param iconTint optional icon tint override; falls back to the icon button theme.
 * @param buttonSize optional button size override; falls back to the icon button theme.
 * @param shadowElevation optional shadow elevation override; falls back to the icon button theme.
 */
@Composable
fun ScrollToEdgeButton(
    listState: LazyListState,
    target: ScrollTarget,
    modifier: Modifier = Modifier,
    threshold: Dp? = null,
    iconSize: Dp? = null,
    onScrollClick: (() -> Unit)? = null,
    icon: DrawableResource? = null,
    contentDescription: String? = null,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
    buttonSize: Dp? = null,
    shadowElevation: Dp? = null,
) {
    val componentTheme = LocalComponentTheme.current
    val scrollToEdgeButtonTheme = componentTheme.scrollToEdgeButton

    val finalThreshold = threshold ?: scrollToEdgeButtonTheme.visibilityThreshold
    val finalIconSize = iconSize ?: scrollToEdgeButtonTheme.iconSize
    val finalBackgroundColor = backgroundColor ?: scrollToEdgeButtonTheme.backgroundColor
    val finalIconTint = iconTint ?: scrollToEdgeButtonTheme.iconTint
    val finalButtonSize = buttonSize ?: scrollToEdgeButtonTheme.buttonSize
    val finalShadowElevation = shadowElevation ?: scrollToEdgeButtonTheme.shadowElevation
    val finalIcon = icon ?: when (target) {
        ScrollTarget.Top -> Res.drawable.shared_ic_angle_up
        ScrollTarget.Bottom -> Res.drawable.shared_ic_angle_down
    }
    val finalContentDescription = contentDescription ?: when (target) {
        ScrollTarget.Top -> stringResource(Res.string.shared_content_desc_scroll_to_top)
        ScrollTarget.Bottom -> stringResource(Res.string.shared_content_desc_scroll_to_bottom)
    }

    val isVisible by rememberIsScrolledAwayFromEdge(listState = listState, target = target, threshold = finalThreshold)
    val coroutineScope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / SCROLL_EDGE_SLIDE_FRACTION },
        exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight / SCROLL_EDGE_SLIDE_FRACTION },
    ) {
        CustomIconButton(
            buttonSize = finalButtonSize,
            backgroundColor = finalBackgroundColor,
            shadowElevation = finalShadowElevation,
            icon = finalIcon,
            iconDescription = finalContentDescription,
            iconTint = finalIconTint,
            iconModifier = Modifier
                .size(size = finalIconSize),
            onClick = onScrollClick ?: {
                coroutineScope.launch {
                    val targetIndex = when (target) {
                        ScrollTarget.Top -> 0
                        ScrollTarget.Bottom -> (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                    }
                    listState.animateScrollToItem(index = targetIndex)
                }
                Unit
            },
        )
    }
}

@Preview
@Composable
fun ScrollToEdgeButtonTopPreview() {
    AppTheme {
        CustomIconButton(
            icon = Res.drawable.shared_ic_angle_up,
            iconDescription = "",
            iconModifier = Modifier
                .size(size = LocalComponentTheme.current.scrollToEdgeButton.iconSize),
            onClick = {},
        )
    }
}

@Preview
@Composable
fun ScrollToEdgeButtonBottomPreview() {
    AppTheme {
        CustomIconButton(
            icon = Res.drawable.shared_ic_angle_down,
            iconDescription = "",
            iconModifier = Modifier
                .size(size = LocalComponentTheme.current.scrollToEdgeButton.iconSize),
            onClick = {},
        )
    }
}