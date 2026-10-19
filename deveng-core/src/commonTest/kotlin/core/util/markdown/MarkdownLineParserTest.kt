package core.util.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownLineParserTest {

    @Test
    fun parseMarkdownLine_withHeadingLevels_appliesSizePerLevel() {
        val firstLevel = parse("# Title")
        val secondLevel = parse("## Section")
        val thirdLevel = parse("### Part")
        val sixthLevel = parse("###### Deep")

        assertEquals("Title", firstLevel.text)
        assertEquals(listOf(styleRange(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold), 0, 5)), firstLevel.spanStyles)
        assertEquals(listOf(styleRange(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold), 0, 7)), secondLevel.spanStyles)
        assertEquals(listOf(styleRange(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium), 0, 4)), thirdLevel.spanStyles)
        assertEquals(listOf(styleRange(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium), 0, 4)), sixthLevel.spanStyles)
    }

    @Test
    fun parseMarkdownLine_withHashWithoutSpace_keepsTextLiteral() {
        val line = parse("#hashtag")

        assertEquals("#hashtag", line.text)
        assertTrue(line.spanStyles.isEmpty())
    }

    @Test
    fun parseMarkdownLine_withListMarkers_drawsBulletAndKeepsIndentation() {
        assertEquals("• dash", parse("- dash").text)
        assertEquals("• star", parse("* star").text)
        assertEquals("• plus", parse("+ plus").text)
        assertEquals("  • nested", parse("  - nested").text)
    }

    @Test
    fun parseMarkdownLine_withOrderedListItem_keepsNumberAndFormatsContent() {
        val line = parse("1. **First** step")

        assertEquals("1. First step", line.text)
        assertEquals(listOf(styleRange(SpanStyle(fontWeight = FontWeight.Bold), 3, 8)), line.spanStyles)
    }

    @Test
    fun parseMarkdownLine_withBoldAndItalic_stylesOnlyTheirContent() {
        val line = parse("You accept **safe usage** and cancel _anytime_.")

        assertEquals("You accept safe usage and cancel anytime.", line.text)
        assertEquals(
            listOf(
                styleRange(SpanStyle(fontWeight = FontWeight.Bold), 11, 21),
                styleRange(SpanStyle(fontStyle = FontStyle.Italic), 33, 40)
            ),
            line.spanStyles
        )
    }

    @Test
    fun parseMarkdownLine_withStarItalic_stylesContent() {
        val line = parse("an *important* word")

        assertEquals("an important word", line.text)
        assertEquals(listOf(styleRange(SpanStyle(fontStyle = FontStyle.Italic), 3, 12)), line.spanStyles)
    }

    @Test
    fun parseMarkdownLine_withUnderscoresInsideWords_keepsThemLiteral() {
        val line = parse("set pending_backend_verification and 2*3*4 here")

        assertEquals("set pending_backend_verification and 2*3*4 here", line.text)
        assertTrue(line.spanStyles.isEmpty())
    }

    @Test
    fun parseMarkdownLine_withDelimiterFollowedBySpace_keepsItLiteral() {
        val line = parse("a _ b _ c and * d *")

        assertEquals("a _ b _ c and * d *", line.text)
        assertTrue(line.spanStyles.isEmpty())
    }

    @Test
    fun parseMarkdownLine_withUnclosedDelimiters_keepsThemLiteral() {
        val line = parse("open **bold and `code and _italic")

        assertEquals("open **bold and `code and _italic", line.text)
        assertTrue(line.spanStyles.isEmpty())
    }

    @Test
    fun parseMarkdownLine_withInlineCode_usesMonospaceAndKeepsMarkdownInsideLiteral() {
        val line = parse("run `gradle **build**` now")

        assertEquals("run gradle **build** now", line.text)
        assertEquals(
            listOf(styleRange(SpanStyle(fontFamily = FontFamily.Monospace, background = CODE_BACKGROUND_COLOR), 4, 20)),
            line.spanStyles
        )
    }

    @Test
    fun parseMarkdownLine_withEmptyCodeSpan_keepsBackticksLiteral() {
        assertEquals("a `` b", parse("a `` b").text)
    }

    @Test
    fun parseMarkdownLine_withMarkdownLink_addsUrlAnnotationOverLinkText() {
        val line = parse("See [the PR](https://github.com/pinqponq/repo/pull/2) please")

        assertEquals("See the PR please", line.text)
        val link = line.getLinkAnnotations(start = 0, end = line.length).single()
        assertEquals(4, link.start)
        assertEquals(10, link.end)
        val url = link.item as LinkAnnotation.Url
        assertEquals("https://github.com/pinqponq/repo/pull/2", url.url)
        assertEquals(SpanStyle(color = LINK_COLOR, textDecoration = TextDecoration.Underline), url.styles?.style)
    }

    @Test
    fun parseMarkdownLine_withBrokenLinkSyntax_keepsTextLiteral() {
        assertEquals("[text] (url) and [](x) and [a](", parse("[text] (url) and [](x) and [a](").text)
        assertTrue(parse("[text] (url)").getLinkAnnotations(0, 12).isEmpty())
    }

    @Test
    fun parseMarkdownLine_withBareUrl_linksItWithoutTrailingPunctuation() {
        val line = parse("Open https://example.com/a_b_c. Then http://x.io)")

        assertEquals("Open https://example.com/a_b_c. Then http://x.io)", line.text)
        val links = line.getLinkAnnotations(start = 0, end = line.length)
        assertEquals(listOf("https://example.com/a_b_c", "http://x.io"), links.map { (it.item as LinkAnnotation.Url).url })
        assertEquals(listOf(5 to 30, 37 to 48), links.map { it.start to it.end })
        assertTrue(line.spanStyles.isEmpty())
    }

    @Test
    fun parseMarkdownLine_withUrlPrefixInsideWord_keepsItLiteral() {
        val line = parse("xhttps://example.com")

        assertTrue(line.getLinkAnnotations(start = 0, end = line.length).isEmpty())
    }

    @Test
    fun parseMarkdownLine_withItalicInsideBold_appliesBothStyles() {
        val line = parse("**bold _and italic_**")

        assertEquals("bold and italic", line.text)
        assertEquals(
            listOf(
                styleRange(SpanStyle(fontWeight = FontWeight.Bold), 0, 15),
                styleRange(SpanStyle(fontStyle = FontStyle.Italic), 5, 15)
            ),
            line.spanStyles
        )
    }

    private fun parse(line: String): AnnotatedString = parseMarkdownLine(
        line = line,
        inlineStyles = MarkdownInlineStyles(linkColor = LINK_COLOR, codeBackgroundColor = CODE_BACKGROUND_COLOR)
    )

    private fun styleRange(style: SpanStyle, start: Int, end: Int) = AnnotatedString.Range(item = style, start = start, end = end)

    private companion object {
        val LINK_COLOR = Color(0xFF3366FF)
        val CODE_BACKGROUND_COLOR = Color(0x22000000)
    }
}
