package core.util.markdown

internal sealed class MarkdownBlock {
    data class Line(val text: String) : MarkdownBlock()

    data class CodeBlock(val code: String) : MarkdownBlock()
}
