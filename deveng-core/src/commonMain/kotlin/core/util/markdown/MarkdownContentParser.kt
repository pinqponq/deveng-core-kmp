package core.util.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private const val CODE_BACKGROUND_ALPHA = 0.12f

/**
 * Renders a markdown string as rich text, one line at a time.
 * Supports headings (# to ######), list items (-, * or + with optional indentation), fenced code blocks (```),
 * inline code (`code`), bold (**text**), italic (_text_ or *text*, not inside a word), links ([text](url))
 * and bare http/https URLs. Links open with the platform URI handler when tapped.
 *
 * @param markdownContent Raw markdown content to render.
 * @param textColor Color for all rendered text (paragraphs inherit via [BasicText] style; inline spans keep weight/style only).
 * @param modifier Modifier for the column that holds the lines.
 * @param textStyle Base text style (font family, size, line height); [textColor] replaces its color.
 * @param linkColor Color of links and bare URLs.
 * @param codeBackgroundColor Background of inline code and code blocks.
 */
@Composable
fun MarkdownContentParser(
    markdownContent: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    linkColor: Color = textColor,
    codeBackgroundColor: Color = textColor.copy(alpha = CODE_BACKGROUND_ALPHA),
) {
    val blocks = remember(markdownContent) { parseMarkdownBlocks(markdownContent = markdownContent) }
    val baseStyle = textStyle.copy(color = textColor)
    val inlineStyles = MarkdownInlineStyles(linkColor = linkColor, codeBackgroundColor = codeBackgroundColor)

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Line -> BasicText(
                    text = parseMarkdownLine(line = block.text, inlineStyles = inlineStyles),
                    style = baseStyle
                )

                is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(
                    code = block.code,
                    textStyle = baseStyle,
                    backgroundColor = codeBackgroundColor
                )
            }
        }
    }
}
