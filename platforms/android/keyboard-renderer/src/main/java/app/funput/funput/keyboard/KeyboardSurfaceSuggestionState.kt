package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.ResolvedKeyboard

internal class KeyboardSurfaceSuggestionState(
    private val density: () -> Float,
    private val keyboard: () -> ResolvedKeyboard?,
    private val apply: (List<String>) -> Unit,
) {
    private var requested = emptyList<String>()

    fun update(values: List<String>) {
        requested = SuggestionNormalizer.normalize(values)
        refresh()
    }

    fun geometryChanged() = refresh()

    private fun refresh() {
        val width = keyboard()?.suggestionBar?.suggestionsBounds?.width ?: return
        val count = SuggestionCapacity.visibleCount(width, density(), requested.size)
        apply(requested.take(count))
    }
}
