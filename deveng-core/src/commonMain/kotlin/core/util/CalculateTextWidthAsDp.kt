package core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

@Composable
fun calculateTextWidthAsDp(
    text: String,
    style: TextStyle
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widthPx = remember(text, style) {
        textMeasurer
            .measure(
                text = AnnotatedString(text),
                style = style
            )
            .size
            .width
    }
    return with(density) { widthPx.toDp() }
}