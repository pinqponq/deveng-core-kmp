package core.util.markdown

private const val CODE_FENCE = "```"

internal fun parseMarkdownBlocks(markdownContent: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val codeLines = mutableListOf<String>()
    var isInsideCodeBlock = false

    markdownContent.lines().forEach { line ->
        val isCodeFence = line.trimStart().startsWith(CODE_FENCE)
        when {
            isCodeFence && isInsideCodeBlock -> {
                blocks += MarkdownBlock.CodeBlock(code = codeLines.joinToString(separator = "\n"))
                codeLines.clear()
                isInsideCodeBlock = false
            }

            isCodeFence -> isInsideCodeBlock = true
            isInsideCodeBlock -> codeLines += line
            else -> blocks += MarkdownBlock.Line(text = line)
        }
    }

    if (isInsideCodeBlock) {
        blocks += MarkdownBlock.CodeBlock(code = codeLines.joinToString(separator = "\n"))
    }

    return blocks
}
