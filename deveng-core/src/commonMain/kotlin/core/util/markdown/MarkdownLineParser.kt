package core.util.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private const val BULLET = "• "
private val HEADING_PATTERN = Regex("""^(#{1,6}) (.*)$""")
private val UNORDERED_LIST_ITEM_PATTERN = Regex("""^(\s*)[-*+] (.*)$""")

private val FIRST_LEVEL_HEADING_STYLE = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
private val SECOND_LEVEL_HEADING_STYLE = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
private val OTHER_HEADING_STYLE = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)

internal fun parseMarkdownLine(
    line: String,
    inlineStyles: MarkdownInlineStyles
): AnnotatedString {
    val headingMatch = HEADING_PATTERN.matchEntire(line)
    val listItemMatch = UNORDERED_LIST_ITEM_PATTERN.matchEntire(line)

    return buildAnnotatedString {
        when {
            headingMatch != null -> {
                val (hashes, content) = headingMatch.destructured
                withStyle(style = headingStyle(level = hashes.length)) {
                    appendInlineMarkdown(content = content, inlineStyles = inlineStyles)
                }
            }

            listItemMatch != null -> {
                val (indent, content) = listItemMatch.destructured
                append(indent)
                append(BULLET)
                appendInlineMarkdown(content = content, inlineStyles = inlineStyles)
            }

            else -> appendInlineMarkdown(content = line, inlineStyles = inlineStyles)
        }
    }
}

private fun headingStyle(level: Int): SpanStyle = when (level) {
    1 -> FIRST_LEVEL_HEADING_STYLE
    2 -> SECOND_LEVEL_HEADING_STYLE
    else -> OTHER_HEADING_STYLE
}
