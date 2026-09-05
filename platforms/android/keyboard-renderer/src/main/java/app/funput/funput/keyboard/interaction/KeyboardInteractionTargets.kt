package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.SuggestionSelection

internal object SuggestionTargetIds {
    private val ids = arrayOf("suggestion-0", "suggestion-1", "suggestion-2")

    fun id(index: Int): String = ids[index]

    fun indexOf(targetId: String): Int? {
        val index = ids.indexOf(targetId)
        return index.takeIf { it >= 0 }
    }
}

internal const val ClipboardTargetId = "clipboard-paste"

internal fun ResolvedKeyboard.interactionTargetAt(
    x: Float,
    y: Float,
    suggestionCount: Int,
    clipboardVisible: Boolean = false,
): String? {
    keyAt(x, y)?.let { return it.spec.id }
    val bar = suggestionBar ?: return null
    if (!bar.suggestionsHitBounds.contains(x, y)) return null
    if (suggestionCount <= 0) return ClipboardTargetId.takeIf { clipboardVisible }

    val bounds = bar.suggestionsHitBounds
    val segmentWidth = bounds.width / suggestionCount
    val index = ((x - bounds.left) / segmentWidth).toInt().coerceIn(0, suggestionCount - 1)
    return SuggestionTargetIds.id(index)
}

internal fun List<String>.selectionForTarget(targetId: String): SuggestionSelection? {
    val index = SuggestionTargetIds.indexOf(targetId) ?: return null
    val text = getOrNull(index) ?: return null
    return SuggestionSelection(index, text)
}
