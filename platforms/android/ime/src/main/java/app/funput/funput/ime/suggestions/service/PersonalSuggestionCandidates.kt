package app.funput.funput.ime.suggestions

import app.funput.funput.keyboard.model.ShiftState

/**
 * The words on the bar, kept in step in two shapes: as the engine stored them, and in
 * the case the user is typing in.
 *
 * A tap is matched against what was drawn, and an edit is applied to what was stored,
 * so both must always describe the same list: a bar drawn from a list nobody kept is a
 * bar whose every tap is silently refused.
 */
internal class PersonalSuggestionCandidates(
    private val show: (List<String>) -> Unit,
    private val shift: () -> ShiftState,
) {
    private var received = emptyList<String>()

    /** What the bar shows. */
    var shown = emptyList<String>()
        private set

    fun receive(values: List<String>, prefix: String) {
        received = values
        draw(prefix)
    }

    /** Cases the stored words for [prefix] and the current Shift, and shows them if they changed. */
    fun draw(prefix: String) {
        val next = received.map { PersonalSuggestionCasing.apply(it, prefix, shift()) }
        if (next == shown) return
        shown = next
        runCatching { show(shown) }
    }

    /** The raw list goes with the shown one, or a later Shift would redraw what was dropped. */
    fun clear() {
        received = emptyList()
        if (shown.isEmpty()) return
        shown = emptyList()
        runCatching { show(emptyList()) }
    }

    fun isShown(index: Int, text: String): Boolean = shown.getOrNull(index) == text

    /** The stored spelling of the word shown at [index], when it still reads [text]. */
    fun stored(index: Int, text: String): String? = received.getOrNull(index)?.takeIf { isShown(index, text) }
}
