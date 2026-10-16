package core.presentation.component.shimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import core.presentation.motion.shimmer
import core.presentation.theme.LocalComponentTheme

/**
 * A rounded loading placeholder with a shimmer. Size it with [modifier] to match the content it
 * stands in for.
 *
 * @param modifier Modifier to be applied to the placeholder, usually its size.
 * @param cornerRadius Corner radius of the placeholder. If null, uses theme default.
 * @param baseColor Placeholder color. If null, uses theme default.
 * @param highlightColor Color of the sweeping highlight. If null, uses theme default.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    baseColor: Color? = null,
    highlightColor: Color? = null
) {
    val shimmerTheme = LocalComponentTheme.current.shimmer
    val finalCornerRadius = cornerRadius ?: shimmerTheme.cornerRadius

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(finalCornerRadius))
            .shimmer(baseColor = baseColor, highlightColor = highlightColor)
    )
}

