package core.util.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val CODE_BLOCK_VERTICAL_PADDING = 4.dp
private val CODE_BLOCK_CORNER_RADIUS = 8.dp
private val CODE_BLOCK_CONTENT_PADDING = 10.dp

@Composable
internal fun MarkdownCodeBlock(
    code: String,
    textStyle: TextStyle,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CODE_BLOCK_VERTICAL_PADDING)
            .background(color = backgroundColor, shape = RoundedCornerShape(CODE_BLOCK_CORNER_RADIUS))
    ) {
        BasicText(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(CODE_BLOCK_CONTENT_PADDING),
            text = code,
            style = textStyle.copy(fontFamily = FontFamily.Monospace),
            softWrap = false
        )
    }
}
