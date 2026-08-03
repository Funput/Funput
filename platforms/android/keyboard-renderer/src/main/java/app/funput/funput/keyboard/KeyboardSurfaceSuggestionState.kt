package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.ResolvedKeyboard

/**
 * Normalizes suggestion candidates and refreshes visible capacity.
 *
 * When candidates flip between empty and non-empty, [onShowSettingsChanged] rebuilds
 * toolbar geometry so Settings can yield space to suggestions.
 */
internal class KeyboardSurfaceSuggestionState(
    private val density: () -> Float,
    private val keyboard: () -> ResolvedKeyboard?,
    private val apply: (List<String>) -> Unit,
    private val onShowSettingsChanged: (Boolean) -> Unit = {},
) {
    private var requested = emptyList<String>()

    /** True when the toolbar should keep the Settings key (no suggestion candidates). */
    var showSettings: Boolean = true
        private set

    fun update(values: List<String>) {
        requested = SuggestionNormalizer.normalize(values)
        val nextShowSettings = requested.isEmpty()
        if (nextShowSettings != showSettings) {
            showSettings = nextShowSettings
            onShowSettingsChanged(nextShowSettings)
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
