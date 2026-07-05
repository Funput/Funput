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
    val suggestionsBounds: KeyBounds,
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
        suggestionBar?.emojiKey?.let(::add)
        rows.forEach(::addAll)
    }
    private val hitTester = KeyboardHitTester(width, height, keys)

    fun keyAt(x: Float, y: Float): ResolvedKey? = hitTester.keyAt(x, y)
}
