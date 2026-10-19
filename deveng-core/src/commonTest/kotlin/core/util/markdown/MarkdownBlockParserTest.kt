package core.util.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownBlockParserTest {

    @Test
    fun parseMarkdownBlocks_withPlainLines_keepsEveryLineIncludingBlankOnes() {
        val blocks = parseMarkdownBlocks(markdownContent = "First\n\nSecond")

        assertEquals(
            listOf(MarkdownBlock.Line(text = "First"), MarkdownBlock.Line(text = ""), MarkdownBlock.Line(text = "Second")),
            blocks
        )
    }

    @Test
    fun parseMarkdownBlocks_withFencedCode_returnsCodeBlockWithoutFencesOrLanguage() {
        val blocks = parseMarkdownBlocks(markdownContent = "Before\n```kotlin\nval a = 1\n  val b = 2\n```\nAfter")

        assertEquals(
            listOf(
                MarkdownBlock.Line(text = "Before"),
                MarkdownBlock.CodeBlock(code = "val a = 1\n  val b = 2"),
                MarkdownBlock.Line(text = "After")
            ),
            blocks
        )
    }

    @Test
    fun parseMarkdownBlocks_withMarkdownInsideCode_keepsItLiteral() {
        val blocks = parseMarkdownBlocks(markdownContent = "```\n# not a heading\n- not a list\n```")

        assertEquals(listOf(MarkdownBlock.CodeBlock(code = "# not a heading\n- not a list")), blocks)
    }

    @Test
    fun parseMarkdownBlocks_withUnclosedFence_returnsRestAsCodeBlock() {
        val blocks = parseMarkdownBlocks(markdownContent = "Text\n```\nline one\nline two")

        assertEquals(
            listOf(MarkdownBlock.Line(text = "Text"), MarkdownBlock.CodeBlock(code = "line one\nline two")),
            blocks
        )
    }
}
