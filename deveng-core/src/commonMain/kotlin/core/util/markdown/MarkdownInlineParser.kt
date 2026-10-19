package core.util.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

private const val CODE_DELIMITER = '`'
private const val BOLD_DELIMITER = "**"
private const val LINK_TEXT_START = '['
private const val LINK_TEXT_END = ']'
private const val LINK_TEXT_END_AND_URL_START = "]("
private const val LINK_URL_END = ')'
private val ITALIC_DELIMITERS = setOf('*', '_')
private val BARE_URL_PREFIXES = listOf("https://", "http://")
private const val BARE_URL_TRAILING_PUNCTUATION = ".,;:!?)\"'"

internal data class MarkdownInlineStyles(
    val linkColor: Color,
    val codeBackgroundColor: Color
)

private sealed class InlineToken {
    abstract val endExclusive: Int

    data class Code(val code: String, override val endExclusive: Int) : InlineToken()

    data class Bold(val content: String, override val endExclusive: Int) : InlineToken()

    data class Italic(val content: String, override val endExclusive: Int) : InlineToken()

    data class Link(val text: String, val url: String, override val endExclusive: Int) : InlineToken()
}

internal fun AnnotatedString.Builder.appendInlineMarkdown(
    content: String,
    inlineStyles: MarkdownInlineStyles
) {
    val plainText = StringBuilder()
    var index = 0

    while (index < content.length) {
        val token = findInlineToken(content = content, start = index)
        if (token == null) {
            plainText.append(content[index])
            index++
            continue
        }

        append(plainText.toString())
        plainText.clear()
        appendInlineToken(token = token, inlineStyles = inlineStyles)
        index = token.endExclusive
    }

    append(plainText.toString())
}

private fun AnnotatedString.Builder.appendInlineToken(
    token: InlineToken,
    inlineStyles: MarkdownInlineStyles
) {
    when (token) {
        is InlineToken.Code -> withStyle(
            style = SpanStyle(fontFamily = FontFamily.Monospace, background = inlineStyles.codeBackgroundColor)
        ) {
            append(token.code)
        }

        is InlineToken.Bold -> withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            appendInlineMarkdown(content = token.content, inlineStyles = inlineStyles)
        }

        is InlineToken.Italic -> withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            appendInlineMarkdown(content = token.content, inlineStyles = inlineStyles)
        }

        is InlineToken.Link -> {
            val linkStyles = TextLinkStyles(
                style = SpanStyle(color = inlineStyles.linkColor, textDecoration = TextDecoration.Underline)
            )
            withLink(link = LinkAnnotation.Url(url = token.url, styles = linkStyles)) {
                append(token.text)
            }
        }
    }
}

private fun findInlineToken(content: String, start: Int): InlineToken? = when {
    content[start] == CODE_DELIMITER -> findCode(content = content, start = start)
    content.startsWith(BOLD_DELIMITER, startIndex = start) -> findBold(content = content, start = start)
    content[start] in ITALIC_DELIMITERS -> findItalic(content = content, start = start)
    content[start] == LINK_TEXT_START -> findLink(content = content, start = start)
    else -> findBareUrl(content = content, start = start)
}

private fun findCode(content: String, start: Int): InlineToken? {
    val closingIndex = content.indexOf(CODE_DELIMITER, startIndex = start + 1)
    if (closingIndex <= start + 1) return null
    return InlineToken.Code(code = content.substring(start + 1, closingIndex), endExclusive = closingIndex + 1)
}

private fun findBold(content: String, start: Int): InlineToken? {
    val contentStart = start + BOLD_DELIMITER.length
    val closingIndex = content.indexOf(BOLD_DELIMITER, startIndex = contentStart)
    if (closingIndex <= contentStart) return null
    return InlineToken.Bold(
        content = content.substring(contentStart, closingIndex),
        endExclusive = closingIndex + BOLD_DELIMITER.length
    )
}

// An italic delimiter inside a word (snake_case_names, 2*3*4) is literal text, as in CommonMark.
private fun findItalic(content: String, start: Int): InlineToken? {
    val delimiter = content[start]
    val isAfterWordCharacter = start > 0 && content[start - 1].isLetterOrDigit()
    val nextCharacter = content.getOrNull(start + 1)
    val isOpening = !isAfterWordCharacter && nextCharacter != null && !nextCharacter.isWhitespace() && nextCharacter != delimiter
    if (!isOpening) return null

    val closingIndex = (start + 2 until content.length).firstOrNull { index ->
        content[index] == delimiter && isClosingItalicDelimiter(content = content, index = index)
    } ?: return null
    return InlineToken.Italic(content = content.substring(start + 1, closingIndex), endExclusive = closingIndex + 1)
}

private fun isClosingItalicDelimiter(content: String, index: Int): Boolean {
    val isAfterWhitespace = content[index - 1].isWhitespace()
    val isBeforeWordCharacter = content.getOrNull(index + 1)?.isLetterOrDigit() == true
    return !isAfterWhitespace && !isBeforeWordCharacter
}

private fun findLink(content: String, start: Int): InlineToken? {
    val textEnd = content.indexOf(LINK_TEXT_END, startIndex = start + 1)
    val isLinkText = textEnd > start + 1 && content.startsWith(LINK_TEXT_END_AND_URL_START, startIndex = textEnd)
    if (!isLinkText) return null
    val urlStart = textEnd + LINK_TEXT_END_AND_URL_START.length
    val urlEnd = content.indexOf(LINK_URL_END, startIndex = urlStart)
    if (urlEnd <= urlStart) return null
    return InlineToken.Link(
        text = content.substring(start + 1, textEnd),
        url = content.substring(urlStart, urlEnd),
        endExclusive = urlEnd + 1
    )
}

private fun findBareUrl(content: String, start: Int): InlineToken? {
    val isAfterWordCharacter = start > 0 && content[start - 1].isLetterOrDigit()
    val isUrlStart = BARE_URL_PREFIXES.any { prefix -> content.startsWith(prefix, startIndex = start) }
    if (isAfterWordCharacter || !isUrlStart) return null

    val whitespaceIndex = (start until content.length).firstOrNull { index -> content[index].isWhitespace() }
    val url = content.substring(start, whitespaceIndex ?: content.length).trimEnd { character ->
        character in BARE_URL_TRAILING_PUNCTUATION
    }
    return InlineToken.Link(text = url, url = url, endExclusive = start + url.length)
}
