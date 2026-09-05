package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeySpec

data class KeyBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

data class ResolvedKey(
    val spec: KeySpec,
    val bounds: KeyBounds,
    val hitBounds: KeyBounds = bounds,
)

data class ResolvedSuggestionBar(
    val bounds: KeyBounds,
    val logoBounds: KeyBounds,
    val suggestionsBounds: KeyBounds,
    /**
     * Where a tap still counts as a suggestion. Wider than what is drawn: the band is only
     * as tall as the text needs, so the slack above it and half the gap below it belong to
     * the suggestions rather than to nothing at all. [KeyboardHitTargetResolver] fills it in.
     */
    val suggestionsHitBounds: KeyBounds = suggestionsBounds,
    val systemInputMethodKey: ResolvedKey?,
    val clipboardKey: ResolvedKey? = null,
    val emojiKey: ResolvedKey,
    val suggestionsEnabled: Boolean,
)

data class ResolvedKeyboard(
    val width: Float,
    val height: Float,
    val suggestionBar: ResolvedSuggestionBar?,
    val rows: List<List<ResolvedKey>>,
) {
    val keys: List<ResolvedKey> = buildList {
        suggestionBar?.systemInputMethodKey?.let(::add)
        suggestionBar?.clipboardKey?.let(::add)
        suggestionBar?.emojiKey?.let(::add)
        rows.forEach(::addAll)
    }
    private val hitTester = KeyboardHitTester(width, height, keys)

    fun keyAt(x: Float, y: Float): ResolvedKey? = hitTester.keyAt(x, y)
}
