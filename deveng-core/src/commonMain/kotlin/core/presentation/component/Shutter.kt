package core.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import core.presentation.theme.AppTheme
import core.presentation.theme.CoreBoldTextStyle
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_description_angle_down
import global.deveng.deveng_core.generated.resources.shared_ic_angle_down
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A customizable expandable shutter (accordion) component that consists of a clickable header and an animated content section.
 * Supports custom styling, optional descriptions, and smooth expand/collapse animations.
 *
 * @param containerModifier Modifier to be applied to the outermost container of the shutter.
 * @param headerModifier Modifier to be applied specifically to the clickable header row.
 * @param title Primary text displayed in the header.
 * @param description Optional secondary text displayed below the title in the header. If null, the description section is hidden.
 * @param headerBackgroundColor Background color of the header. If null, uses theme default.
 * @param headerInnerPadding Padding applied inside the header. If null, uses theme default.
 * @param headerContentSpacing Spacing between the title and description text in the header. If null, uses theme default.
 * @param headerShadowHeight Elevation height for the header's shadow. If null, uses theme default.
 * @param headerShadowShape Shape of the header's shadow. If null, uses theme default.
 * @param titleTextStyle Text style for the header title. If null, uses theme default.
 * @param descriptionTextStyle Text style for the header description. If null, uses theme default.
 * @param expandIcon Drawable resource for the expand/collapse indicator icon. If null, uses theme default.
 * @param expandIconSize Size of the expand/collapse icon. If null, uses theme default.
 * @param expandIconColor Color tint for the expand/collapse icon. If null, uses theme default.
 * @param expandIconDescription Content description for accessibility for the expand icon.
 * @param animationDurationMillis Duration of the expand/collapse animation in milliseconds. Default is 400.
 * @param isShutterExpanded Whether the shutter content is currently visible (expanded). Default is true.
 * @param onClickShutter Callback invoked when the shutter header is clicked to toggle its state.
 * @param content The composable content to be displayed when the shutter is expanded.
 */

@Composable
fun Shutter(
    containerModifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    headerBackgroundColor: Color? = null,
    headerInnerPadding: Dp? = null,
    headerContentSpacing: Dp? = null,
    headerShadowHeight: Dp? = null,
    headerShadowShape: Shape? = null,
    titleTextStyle: TextStyle? = null,
    descriptionTextStyle: TextStyle? = null,
    expandIcon: DrawableResource? = null,
    expandIconSize: Dp? = null,
    expandIconColor: Color? = null,
    expandIconDescription: StringResource = Res.string.shared_content_description_angle_down,
    animationDurationMillis: Int = 400,
    isShutterExpanded: Boolean = true,
    onClickShutter: () -> Unit,
    content: Slot
) {
    val componentTheme = LocalComponentTheme.current
    val shutterTheme = componentTheme.shutter

    val finalHeaderBackgroundColor = headerBackgroundColor ?: shutterTheme.headerBackgroundColor
    val finalHeaderInnerPadding = headerInnerPadding ?: shutterTheme.headerInnerPadding
    val finalHeaderContentSpacing = headerContentSpacing ?: shutterTheme.headerContentSpacing
    val finalHeaderShadowHeight = headerShadowHeight ?: shutterTheme.headerShadowHeight
    val finalHeaderShadowShape = headerShadowShape ?: shutterTheme.headerShadowShape
    val finalTitleTextStyle = titleTextStyle ?: shutterTheme.titleTextStyle
    val finalDescriptionTextStyle = descriptionTextStyle ?: shutterTheme.descriptionTextStyle
    val finalExpandIcon = expandIcon ?: shutterTheme.expandIcon ?: Res.drawable.shared_ic_angle_down
    val finalExpandIconSize = expandIconSize ?: shutterTheme.expandIconSize
    val finalExpandIconColor = expandIconColor ?: shutterTheme.expandIconColor
    val iconRotation by animateFloatAsState(
        targetValue = if (isShutterExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = animationDurationMillis)
    )

    val currentElevation by animateDpAsState(
        targetValue = if (isShutterExpanded) finalHeaderShadowHeight else 0.dp,
        animationSpec = tween(durationMillis = animationDurationMillis)
    )

    Column(
        modifier = containerModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = headerModifier
                .fillMaxWidth()
                .zIndex(1f)
                .shadow(
                    elevation = currentElevation,
                    shape = finalHeaderShadowShape,
                    clip = false
                )
                .background(color = finalHeaderBackgroundColor)
                .clickable(onClick = { onClickShutter() })
                .padding(all = finalHeaderInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = finalTitleTextStyle
                )

                if (!description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(finalHeaderContentSpacing))

                    Text(
                        text = description,
                        style = finalDescriptionTextStyle
                    )
                }
            }

            Icon(
                modifier = Modifier
                    .size(finalExpandIconSize)
                    .graphicsLayer { rotationZ = iconRotation },
                painter = painterResource(finalExpandIcon),
                tint = finalExpandIconColor,
                contentDescription = stringResource(expandIconDescription),
            )
        }

        AnimatedVisibility(
            visible = isShutterExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = animationDurationMillis)
            ) + fadeIn(
                animationSpec = tween(durationMillis = animationDurationMillis)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = animationDurationMillis)
            ) + fadeOut(
                animationSpec = tween(durationMillis = animationDurationMillis)
            )
        ) {
            content()
        }
    }
}

@Preview
@Composable
fun ShutterPreview() {
    AppTheme {
        Shutter(
            title = "Title",
            description = "Description",
            onClickShutter = {},
            content = {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .background(color = Color.White)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Content",
                        style = CoreBoldTextStyle().copy(
                            color = Color.Black
                        )
                    )
                }
            }
        )
    }
}