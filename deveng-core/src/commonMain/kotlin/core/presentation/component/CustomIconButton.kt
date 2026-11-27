package core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.CoreCustomBlackColor
import core.presentation.theme.LocalComponentTheme
import core.presentation.theme.CorePrimaryColor
import core.util.debouncedCombinedClickable
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_next
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable icon button component with shadow and styling options.
 *
 * @param modifier Modifier to be applied to the icon button container.
 * @param isEnabled Whether the button is enabled and can be clicked. Default is true.
 * @param buttonSize Size of the button. If null, uses theme default.
 * @param iconModifier Modifier to be applied to the icon.
 * @param backgroundColor Background color of the button. If null, uses theme default.
 * @param shape Shape of the button. If null, uses theme default (typically CircleShape).
 * @param icon Drawable resource for the icon to display.
 * @param iconDescription Content description for accessibility.
 * @param iconTint Color tint for the icon. If null, uses theme default.
 * @param shadowElevation Shadow elevation of the button. If null, uses theme default.
 * @param clickDebounceMillis Minimum gap between accepted clicks (see [debouncedCombinedClickable]); default matches extension (600ms).
 * @param onClick Callback invoked when the button is clicked.
 */
@Composable
fun CustomIconButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    buttonSize: Dp? = null,
    iconModifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    shape: Shape? = null,
    icon: DrawableResource,
    iconDescription: String,
    iconTint: Color? = null,
    shadowElevation: Dp? = null,
    clickDebounceMillis: Long = 600L,
    onClick: () -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val iconButtonTheme = componentTheme.iconButton
    val finalButtonSize = buttonSize ?: iconButtonTheme.buttonSize
    val baseBackgroundColor = backgroundColor ?: iconButtonTheme.backgroundColor
    val finalBackgroundColor = if (isEnabled) {
        baseBackgroundColor
    } else {
        baseBackgroundColor.copy(alpha = 0.7f)
    }
    val finalIconTint = iconTint ?: iconButtonTheme.iconTint
    val finalShadowElevation = shadowElevation ?: iconButtonTheme.shadowElevation
    val finalShape = shape ?: iconButtonTheme.shape

    Box(
        modifier = modifier
            .size(finalButtonSize)
            .shadow(
                elevation = finalShadowElevation,
                shape = finalShape
            )
            .background(
                color = finalBackgroundColor,
                shape = finalShape
            )
            .clip(shape = finalShape)
            .debouncedCombinedClickable(
                debounceMillis = clickDebounceMillis,
                shape = finalShape,
            ) {
                if (isEnabled) {
                    onClick()
                }
            }
    ) {
        Icon(
            modifier = iconModifier
                .align(Alignment.Center),
            painter = painterResource(icon),
            contentDescription = iconDescription,
            tint = finalIconTint
        )
    }
}

@Preview
@Composable
fun CustomIconButtonPreview() {
    AppTheme {
        CustomIconButton(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = CoreCustomBlackColor,
                    shape = CircleShape
                ),
            iconModifier = Modifier
                .size(
                    height = 16.dp,
                    width = 10.dp
                ),
            buttonSize = 66.dp,
            backgroundColor = CorePrimaryColor,
            icon = Res.drawable.shared_ic_arrow_next,
            iconDescription = "",
            iconTint = Color.White,
            onClick = {}
        )
    }
}