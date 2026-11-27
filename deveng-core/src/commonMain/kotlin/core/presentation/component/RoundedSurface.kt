package core.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A surface component with rounded corners and customizable border and colors.
 * Wraps content in a Material3 Surface with theme-aware styling.
 *
 * @param modifier Modifier to be applied to the surface.
 * @param borderStroke Border stroke configuration. Default is transparent (no border).
 * @param color Background color of the surface. If null, uses theme default.
 * @param shape Shape of the surface. Default is MaterialTheme.shapes.medium.
 * @param contentColor Color for content inside the surface. If null, uses theme default.
 * @param content Composable content to display inside the surface.
 */
@Composable
fun RoundedSurface(
    modifier: Modifier = Modifier,
    borderStroke: BorderStroke = BorderStroke(0.dp, Color.Transparent),
    color: Color? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    contentColor: Color? = null,
    content: Slot
) {
    val componentTheme = LocalComponentTheme.current
    val surfaceTheme = componentTheme.surface

    val finalColor = color ?: surfaceTheme.defaultColor
    val finalContentColor = contentColor ?: surfaceTheme.defaultContentColor
    Surface(
        modifier = modifier,
        color = finalColor,
        contentColor = finalContentColor,
        shape = shape,
        border = borderStroke
    ) {
        content()
    }
}

@Preview
@Composable
fun RoundedSurfacePreview() {
    AppTheme {
        RoundedSurface(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.primary,
            content = {}
        )
    }
}