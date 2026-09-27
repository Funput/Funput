package app.funput.funput.ui.about.licenses

/** One block of a licence notice, as the screen draws it. */
internal sealed interface LicenseBlock {
    /** A `#` or `##` heading; [level] counts the hashes. */
    data class Heading(val text: String, val level: Int) : LicenseBlock

    /** Running text. */
    data class Paragraph(val text: String) : LicenseBlock

    /**
     * A fenced block: a licence quoted in the notice. Its hard-wrapped lines are rejoined into
     * paragraphs, since wrapping them again at the screen's width leaves a ragged, broken read.
     */
    data class Quote(val text: String) : LicenseBlock
}

/**
 * Splits a notice into blocks. Markdown notices get headings, fenced blocks and soft-wrapped
 * paragraphs joined into one line each; plain-text licences keep their line breaks, since their
 * clauses and numbered lists depend on them. Inline markup (`code`, <links>, **bold**) is reduced
 * to its text: a licence screen is for reading, not for rendering Markdown faithfully.
 */
internal fun parseLicense(text: String, markdown: Boolean): List<LicenseBlock> {
    val blocks = mutableListOf<LicenseBlock>()
    val paragraph = mutableListOf<String>()
    val verbatim = mutableListOf<String>()
    var inFence = false
    fun flushParagraph() {
        if (paragraph.isEmpty()) return
        val joined = if (markdown) paragraph.joinToString(" ") { it.trim() } else paragraph.joinToString("\n")
        blocks += LicenseBlock.Paragraph(joined.inlineText())
        paragraph.clear()
    }
    for (line in text.lines()) {
        when {
            markdown && line.trimStart().startsWith("```") -> {
                if (inFence) blocks += LicenseBlock.Quote(verbatim.reflowed()) else flushParagraph()
                verbatim.clear()
                inFence = !inFence
            }
            inFence -> verbatim += line
            markdown && line.startsWith("#") -> {
                flushParagraph()
                val level = line.takeWhile { it == '#' }.length
                blocks += LicenseBlock.Heading(line.drop(level).trim().inlineText(), level)
            }
            line.isBlank() -> flushParagraph()
            else -> paragraph += line
        }
    }
    flushParagraph()
    return blocks
}

/** Joins each blank-line-separated paragraph's lines into one, collapsing runs of spaces. */
private fun List<String>.reflowed(): String =
    joinToString("\n").trim().split(Regex("\n\\s*\n")).joinToString("\n\n") { paragraph ->
        paragraph.lines().joinToString(" ") { it.trim() }.replace(Regex(" {2,}"), " ")
    }

private fun String.inlineText(): String = this
    .replace(Regex("<(https?://[^>]+)>"), "$1")
    .replace("**", "")
    .replace("`", "")
