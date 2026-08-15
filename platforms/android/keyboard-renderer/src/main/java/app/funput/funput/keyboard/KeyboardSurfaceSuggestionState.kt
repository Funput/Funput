package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.ResolvedKeyboard

/**
 * Normalizes suggestion candidates and refreshes visible capacity.
 *
 * When candidates flip between empty and non-empty, [onUtilityKeysVisibilityChanged] rebuilds
 * toolbar geometry so utility keys (e.g. Clipboard) can yield space to suggestions.
 */
internal class KeyboardSurfaceSuggestionState(
    private val density: () -> Float,
    private val keyboard: () -> ResolvedKeyboard?,
    private val apply: (List<String>) -> Unit,
    private val onUtilityKeysVisibilityChanged: (Boolean) -> Unit = {},
) {
    private var requested = emptyList<String>()

    /** True when the toolbar has room to keep utility keys (no suggestion candidates). */
    var utilityKeysVisible: Boolean = true
        private set

    fun update(values: List<String>) {
        requested = SuggestionNormalizer.normalize(values)
        val nextUtilityKeysVisible = requested.isEmpty()
        if (nextUtilityKeysVisible != utilityKeysVisible) {
            utilityKeysVisible = nextUtilityKeysVisible
            onUtilityKeysVisibilityChanged(nextUtilityKeysVisible)
            return
        }
        refresh()
    }

    fun geometryChanged() = refresh()

    private fun refresh() {
        val width = keyboard()?.suggestionBar?.suggestionsBounds?.width ?: return
        val count = SuggestionCapacity.visibleCount(width, density(), requested.size)
        apply(requested.take(count))
    }
}
