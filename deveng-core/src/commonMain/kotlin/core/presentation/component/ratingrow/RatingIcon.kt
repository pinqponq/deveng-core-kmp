package core.presentation.component.ratingrow

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun RatingIcon(
    modifier: Modifier = Modifier,
    filledIcon: DrawableResource,
    filledIconTint: Color,
    unFilledIcon: DrawableResource,
    unFilledIconTint: Color,
    iconDescription: String? = null,
    isFilled: Boolean
) {
    val starIcon = if (isFilled) filledIcon else unFilledIcon
    val starIconTint = if (isFilled) filledIconTint else unFilledIconTint

    Icon(
        modifier = modifier,
        painter = painterResource(starIcon),
        tint = starIconTint,
        contentDescription = iconDescription
    )
}