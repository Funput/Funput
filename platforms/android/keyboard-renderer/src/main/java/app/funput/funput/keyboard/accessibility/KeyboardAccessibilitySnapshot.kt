package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.interaction.SuggestionTargetIds
import app.funput.funput.keyboard.interaction.ClipboardTargetId
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.popover.accessibility.AlternateAccessibilityAction
import app.funput.funput.keyboard.popover.accessibility.alternateAccessibilityActions
import app.funput.funput.keyboard.suggestion.SuggestionChip
import app.funput.funput.keyboard.suggestion.actions.SuggestionAction

/** Immutable virtual-node snapshot consumed by TalkBack and unit tests. */
internal class KeyboardAccessibilitySnapshot(
    keyboard: ResolvedKeyboard,
    shiftState: ShiftState,
    suggestions: List<SuggestionChip> = emptyList(),
    clipboardLabel: String? = null,
    clipboardKeyLabel: String? = null,
    smartGesturesEnabled: Boolean = true,
    breaksLineOnEnterHold: Boolean = false,
    suggestionEditable: (SuggestionSelection) -> Boolean = { false },
) {
    val nodes: List<KeyboardAccessibilityNode> = buildList {
        keyboard.keys.filterNot { it.spec.role == KeyRole.PLACEHOLDER }.forEach { key ->
            KeyboardAccessibilityNode(
                virtualId = size,
                keyId = key.spec.id,
                label = key.accessibilityLabel(shiftState, clipboardKeyLabel),
                bounds = key.bounds,
                hitBounds = key.hitBounds,
                selected = key.active || (key.spec.role == KeyRole.SHIFT && shiftState != ShiftState.OFF),
                alternateActions = key.spec.alternateAccessibilityActions(shiftState),
                customActions = SmartGestureAccessibility.actions(
                    key.spec.role, smartGesturesEnabled, breaksLineOnEnterHold,
                ),
            ).let(::add)
        }
        val bar = keyboard.suggestionBar
        val bounds = bar?.suggestionsBounds
        // Explore-by-touch follows the same generous region a finger does, not the drawn band.
        val hit = bar?.suggestionsHitBounds
        if (bounds != null && hit != null && suggestions.isNotEmpty()) {
            val width = bounds.width / suggestions.size
            suggestions.forEachIndexed { index, chip ->
                val segment = KeyBounds(bounds.left + width * index, bounds.top, bounds.left + width * (index + 1), bounds.bottom)
                val target = KeyBounds(segment.left, hit.top, segment.right, hit.bottom)
                val id = SuggestionTargetIds.id(index)
                // TalkBack cannot long-press a slot, so a learned word carries its actions instead.
                val actions = if (suggestions.selectionForTarget(id)?.let(suggestionEditable) == true) suggestionActions else emptyList()
                add(KeyboardAccessibilityNode(size, id, chip.accessibilityLabel, segment, target, false, customActions = actions))
            }
        } else if (bounds != null && hit != null && clipboardLabel != null) {
            add(KeyboardAccessibilityNode(size, ClipboardTargetId, clipboardLabel, bounds, hit, false))
        }
    }

    fun node(virtualId: Int): KeyboardAccessibilityNode? = nodes.getOrNull(virtualId)

    fun nodeAt(x: Float, y: Float): KeyboardAccessibilityNode? =
        nodes.firstOrNull { it.hitBounds.contains(x, y) }
}

internal data class KeyboardAccessibilityNode(
    val virtualId: Int,
    val keyId: String,
    val label: String,
    val bounds: KeyBounds,
    val hitBounds: KeyBounds,
    val selected: Boolean,
    val alternateActions: List<AlternateAccessibilityAction> = emptyList(),
    val customActions: List<SmartGestureAccessibility.Action> = emptyList(),
)

private val suggestionActions = SuggestionAction.entries.map {
    SmartGestureAccessibility.Action(it.accessibilityActionId, it.accessibilityLabel)
}

private fun app.funput.funput.keyboard.layout.ResolvedKey.accessibilityLabel(
    shiftState: ShiftState,
    clipboardKeyLabel: String?,
): String = when {
    spec.role == KeyRole.CLIPBOARD && clipboardKeyLabel != null -> clipboardKeyLabel
    spec.role == KeyRole.CHARACTER && shiftState.isActive -> spec.shiftedLabel ?: spec.accessibilityLabel
    else -> spec.accessibilityLabel
}
