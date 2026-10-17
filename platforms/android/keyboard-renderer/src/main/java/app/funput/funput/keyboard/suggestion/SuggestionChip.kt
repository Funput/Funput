package app.funput.funput.keyboard.suggestion

/** What a slot on the suggestion bar does when tapped. */
enum class SuggestionChipKind {
    /** A word the personal store or the editor offers to complete. */
    SUGGESTION,

    /** The word exactly as typed: tapping keeps it and turns the repair down. */
    CORRECTION_TYPED,

    /** A repair of the word just typed: tapping writes it in. */
    CORRECTION_CANDIDATE,

    /** The word a repair replaced: tapping puts it back. */
    CORRECTION_UNDO,
}

/** One slot on the suggestion bar. */
data class SuggestionChip(
    val text: String,
    val kind: SuggestionChipKind = SuggestionChipKind.SUGGESTION,
) {
    /** What the slot shows: the typed word quoted, the undo marked with an arrow. */
    val title: String
        get() = when (kind) {
            SuggestionChipKind.CORRECTION_TYPED -> "“$text”"
            SuggestionChipKind.CORRECTION_UNDO -> "↩ $text"
            else -> text
        }

    /** What TalkBack reads, naming what a tap does rather than only the word. */
    val accessibilityLabel: String
        get() = when (kind) {
            SuggestionChipKind.SUGGESTION -> "Gợi ý, $text"
            SuggestionChipKind.CORRECTION_TYPED -> "Giữ nguyên, $text"
            SuggestionChipKind.CORRECTION_CANDIDATE -> "Sửa thành, $text"
            SuggestionChipKind.CORRECTION_UNDO -> "Hoàn tác, $text"
        }
}

/**
 * The slot the theme accents: the first repair when there is one, otherwise the leading
 * suggestion. Never the typed word or the undo, which are ways out rather than the pick.
 */
fun List<SuggestionChip>.accentIndex(): Int {
    val candidate = indexOfFirst { it.kind == SuggestionChipKind.CORRECTION_CANDIDATE }
    if (candidate >= 0) return candidate
    return indexOfFirst { it.kind == SuggestionChipKind.SUGGESTION }
}

/** Plain words, as editor completions and the personal store produce them. */
fun List<String>.asSuggestionChips(): List<SuggestionChip> = map(::SuggestionChip)
