package core.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_desc_header_center_icon
import global.deveng.deveng_core.generated.resources.shared_content_desc_icon_left
import global.deveng.deveng_core.generated.resources.shared_content_desc_icon_right
import global.deveng.deveng_core.generated.resources.shared_ic_angle_right
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_left
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable header component with left, right, and center icon support.
 * Commonly used for navigation headers in mobile applications.
 *
 * @param modifier Modifier to be applied to the header container.
 * @param leftIcon Drawable resource for the left icon button. Default is arrow left icon.
 * Used for backward compatibility. If provided and visible, appears as the leftmost icon.
 * @param leadingSlot Optional composable slot for leading icons/content.
 * Can contain multiple icons. If leftIcon is null or not visible, this slot aligns to the far left.
 * If leftIcon is visible, this slot appears to the right of leftIcon.
 * @param rightIcon Optional drawable resource for the right icon button.
 * Used for backward compatibility. If provided and visible, appears as the rightmost icon.
 * @param trailingSlot Optional composable slot for trailing icons/content.
 * Can contain multiple icons. If rightIcon is null or not visible, this slot aligns to the far right.
 * If rightIcon is visible, this slot appears to the left of rightIcon.
 * @param centerIcon Optional drawable resource for the center icon/logo.
 * @param centerSlot Optional composable slot for the center content (e.g. a Text logo).
 * Takes precedence over [centerIcon] when provided.
 * @param isCenterAtStart When true, the center content is aligned to the start (left) instead
 * of the center. Useful for placing a logo/title at the start of the header.
 * @param leftIconDescription Content description for accessibility for the left icon.
 * @param rightIconDescription Content description for accessibility for the right icon.
 * @param centerIconDescription Content description for accessibility for the center icon.
 * @param isLeftIconButtonVisible Whether the left icon button area is visible.
 * Default is true if leftIcon is not null or leadingSlot is provided.
 * @param isRightIconButtonVisible Whether the right icon button area is visible.
 * Default is true if rightIcon is not null or trailingSlot is provided.
 * @param isCenterIconVisible Whether the center icon is visible. Default is true.
 * @param containerPadding Padding values for the header container. If null, uses theme default.
 * @param backgroundColor Background color of the header. If null, uses theme default.
 * @param leftIconTint Color tint for the left icon. If null, uses theme default.
 * @param rightIconTint Color tint for the right icon. If null, uses theme default.
 * @param leftIconBackgroundColor Background color of the left icon button. If null, uses theme default.
 * @param rightIconBackgroundColor Background color of the right icon button. If null, uses theme default.
 * @param iconButtonSize Size of the icon buttons. If null, uses theme default.
 * @param leftIconsSpacing Spacing between leftIcon and leadingSlot content. Default is 10.dp.
 * @param rightIconsSpacing Spacing between trailingSlot content and rightIcon. Default is 10.dp.
 * @param onLeftIconClick Callback invoked when the left icon is clicked.
 * @param onRightIconClick Callback invoked when the right icon is clicked.
 */
@Composable
fun CustomHeader(
    modifier: Modifier = Modifier,
    leftIcon: DrawableResource? = Res.drawable.shared_ic_arrow_left,
    leadingSlot: Slot? = null,
    rightIcon: DrawableResource? = null,
    trailingSlot: Slot? = null,
    centerIcon: DrawableResource? = null,
    centerSlot: Slot? = null,
    isCenterAtStart: Boolean = false,
    leftIconDescription: String = stringResource(Res.string.shared_content_desc_icon_left),
    rightIconDescription: String = stringResource(Res.string.shared_content_desc_icon_right),
    centerIconDescription: String = stringResource(Res.string.shared_content_desc_header_center_icon),
    isLeftIconButtonVisible: Boolean = leftIcon != null || leadingSlot != null,
    isRightIconButtonVisible: Boolean = rightIcon != null || trailingSlot != null,
    isCenterIconVisible: Boolean = true,
    containerPadding: PaddingValues? = null,
    backgroundColor: Color? = null,
    leftIconTint: Color? = null,
    rightIconTint: Color? = null,
    leftIconBackgroundColor: Color? = null,
    rightIconBackgroundColor: Color? = null,
    iconButtonSize: Dp? = null,
    leftIconsSpacing: Dp = 10.dp,
    rightIconsSpacing: Dp = 10.dp,
    onLeftIconClick: () -> Unit = {},
    onRightIconClick: () -> Unit = {}
) {
    val componentTheme = LocalComponentTheme.current
    val headerTheme = componentTheme.header

    val finalPadding = containerPadding ?: headerTheme.containerPadding
    val finalBackgroundColor = backgroundColor ?: headerTheme.backgroundColor
    val finalLeftTint = leftIconTint ?: headerTheme.leftIconTint
    val finalRightTint = rightIconTint ?: headerTheme.rightIconTint
    val finalLeftBackground = leftIconBackgroundColor ?: headerTheme.leftIconBackgroundColor
    val finalRightBackground = rightIconBackgroundColor ?: headerTheme.rightIconBackgroundColor
    val finalIconButtonSize = iconButtonSize ?: headerTheme.iconButtonSize

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(finalBackgroundColor)
            .padding(finalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isLeftIconButtonVisible) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(leftIconsSpacing)
            ) {
                if (leftIcon != null) {
                    CustomIconButton(
                        iconModifier = Modifier,
                        buttonSize = finalIconButtonSize,
                        backgroundColor = finalLeftBackground,
                        icon = leftIcon,
                        iconDescription = leftIconDescription,
                        iconTint = finalLeftTint,
                        onClick = onLeftIconClick
                    )
                }

                leadingSlot?.invoke()
            }
        }

        val centerAlignment = if (isCenterAtStart) Alignment.CenterStart else Alignment.Center
        if (isCenterIconVisible) {
            when {
                centerSlot != null -> {
                    Box(modifier = Modifier.align(centerAlignment)) {
                        centerSlot()
                    }
                }
                else -> {
                    val resolvedCenterIcon = centerIcon ?: headerTheme.icon
                    if (resolvedCenterIcon != null) {
                        Image(
                            modifier = Modifier
                                .width(126.dp)
                                .height(45.dp)
                                .align(centerAlignment),
                            painter = painterResource(resolvedCenterIcon),
                            contentDescription = centerIconDescription
                        )
                    }
                }
            }
        }

        if (isRightIconButtonVisible) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(rightIconsSpacing)
            ) {
                trailingSlot?.invoke()

                if (rightIcon != null) {
                    CustomIconButton(
                        buttonSize = finalIconButtonSize,
                        backgroundColor = finalRightBackground,
                        icon = rightIcon,
                        iconDescription = rightIconDescription,
                        iconTint = finalRightTint,
                        onClick = onRightIconClick
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CustomHeaderLogoAtStartPreview() {
    AppTheme {
        CustomHeader(
            leftIcon = null,
            centerSlot = {
                androidx.compose.material3.Text(
                    text = "rindle",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )
            },
            isCenterAtStart = true,
            rightIcon = Res.drawable.shared_ic_angle_right,
            onRightIconClick = {}
        )
    }
}

@Preview
@Composable
fun CustomHeaderNoRightIconPreview() {
    AppTheme {
        CustomHeader(onLeftIconClick = {})
    }
}

@Preview
@Composable
fun CustomHeaderPreview() {
    AppTheme {
        CustomHeader(
            leftIcon = Res.drawable.shared_ic_arrow_left,
            leftIconDescription = stringResource(Res.string.shared_content_desc_icon_left),
            onLeftIconClick = {},
            onRightIconClick = {},
            rightIcon = Res.drawable.shared_ic_angle_right,
            rightIconDescription = stringResource(Res.string.shared_content_desc_icon_left),
            centerIcon = Res.drawable.shared_ic_angle_right
        )
    }
}

@Preview
@Composable
fun CustomHeaderTrailingSlotPreview() {
    AppTheme {
        CustomHeader(
            onLeftIconClick = {},
            trailingSlot = {
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
            }
        )
    }
}

@Preview
@Composable
fun CustomHeaderTrailingSlotWithRightIconPreview() {
    AppTheme {
        CustomHeader(
            onLeftIconClick = {},
            trailingSlot = {
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
            },
            rightIcon = Res.drawable.shared_ic_angle_right,
            onRightIconClick = {}
        )
    }
}

@Preview
@Composable
fun CustomHeaderLeadingSlotPreview() {
    AppTheme {
        CustomHeader(
            leadingSlot = {
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
            }
        )
    }
}

@Preview
@Composable
fun CustomHeaderLeadingSlotWithLeftIconPreview() {
    AppTheme {
        CustomHeader(
            onLeftIconClick = {},
            leadingSlot = {
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
                CustomIconButton(
                    icon = Res.drawable.shared_ic_angle_right,
                    iconDescription = "",
                    onClick = {}
                )
            }
        )
    }
}