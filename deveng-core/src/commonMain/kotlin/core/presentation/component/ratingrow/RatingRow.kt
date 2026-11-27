package core.presentation.component.ratingrow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import core.presentation.theme.CoreCustomAmberYellow
import core.presentation.theme.CoreCustomGrayHintColor
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_dest_icon_star
import global.deveng.deveng_core.generated.resources.shared_ic_star_filled
import global.deveng.deveng_core.generated.resources.shared_ic_star_unfilled
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/**
 * A rating component that displays a row of clickable rating icons for rating selection.
 * Supports custom icons, colors, animations, and spacing. Icons animate when clicked.
 *
 * @param modifier Modifier to be applied to the rating row container.
 * @param maxRating Maximum number of rating icons to display. Default is 5.
 * @param initialRating Initial rating value (0 to maxRating). Default is 0.
 * @param iconSize Size of each rating icon. If null, uses theme default.
 * @param horizontalSpacing Spacing between rating icons. If null, uses default arrangement.
 * @param isEditable Whether the rating can be changed by clicking. Default is true.
 * @param filledIcon Drawable resource for the filled star icon. Default is shared_ic_star_filled.
 * @param filledIconTint Color tint for filled rating icons. Default is CoreCustomAmberYellow.
 * @param unFilledIcon Drawable resource for the unfilled rating icon. Default is shared_ic_star_unfilled.
 * @param unFilledIconTint Color tint for unfilled rating icons. Default is CoreCustomGrayHintColor.
 * @param iconDescription Content description for accessibility. Default is shared_content_dest_icon_star.
 * @param animationScale Scale factor for the click animation. If null, uses theme default.
 * @param animationDampingRatio Damping ratio for the spring animation. If null, uses theme default.
 * @param animationStiffness Stiffness for the spring animation. If null, uses theme default.
 * @param onRatingChanged Callback invoked when the rating changes, providing the new rating value (0 to maxRating).
 */
@Composable
fun RatingRow(
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    initialRating: Int = 0,
    iconSize: Dp? = null,
    horizontalSpacing: Dp? = null,
    isEditable: Boolean = true,
    filledIcon: DrawableResource = Res.drawable.shared_ic_star_filled,
    filledIconTint: Color = CoreCustomAmberYellow,
    unFilledIcon: DrawableResource = Res.drawable.shared_ic_star_unfilled,
    unFilledIconTint: Color = CoreCustomGrayHintColor,
    iconDescription: String = stringResource(Res.string.shared_content_dest_icon_star),
    animationScale: Float? = null,
    animationDampingRatio: Float? = null,
    animationStiffness: Float? = null,
    onRatingChanged: (Int) -> Unit
) {
    val componentTheme = LocalComponentTheme.current
    val ratingRowTheme = componentTheme.ratingRow

    val preferredIconSize = iconSize ?: ratingRowTheme.iconSize
    val finalAnimationScale = animationScale ?: ratingRowTheme.animationScale
    val finalAnimationDampingRatio = animationDampingRatio ?: ratingRowTheme.animationDampingRatio
    val finalAnimationStiffness = animationStiffness ?: ratingRowTheme.animationStiffness

    val interactionSource = remember { MutableInteractionSource() }
    var internalRating by remember { mutableIntStateOf(initialRating) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (horizontalSpacing != null) {
            Arrangement.spacedBy(horizontalSpacing)
        } else {
            Arrangement.Start
        }
    ) {
        for (index in 1..maxRating) {
            var isIconSelected by remember { mutableStateOf(false) }

            val animationState by animateFloatAsState(
                targetValue = if (isIconSelected) finalAnimationScale else 1f,
                animationSpec = spring(
                    dampingRatio = finalAnimationDampingRatio,
                    stiffness = finalAnimationStiffness
                ),
                finishedListener = { scale ->
                    if (scale == finalAnimationScale) isIconSelected = false
                }
            )

            val isStarIconFilled = index <= internalRating

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = isEditable,
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        isIconSelected = true
                        internalRating = if (internalRating == index) 0 else index
                        onRatingChanged(internalRating)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(preferredIconSize)
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animationState,
                            scaleY = animationState,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        )
                ) {
                    RatingIcon(
                        isFilled = isStarIconFilled,
                        filledIcon = filledIcon,
                        filledIconTint = filledIconTint,
                        unFilledIcon = unFilledIcon,
                        unFilledIconTint = unFilledIconTint,
                        iconDescription = iconDescription,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}