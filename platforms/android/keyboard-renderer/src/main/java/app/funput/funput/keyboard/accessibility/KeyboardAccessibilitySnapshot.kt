package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.ShiftState

/** Immutable virtual-node snapshot consumed by TalkBack and unit tests. */
internal class KeyboardAccessibilitySnapshot(
    keyboard: ResolvedKeyboard,
    shiftState: ShiftState,
) {
    val nodes: List<KeyboardAccessibilityNode> = keyboard.keys
        .filterNot { it.spec.role == KeyRole.PLACEHOLDER }
        .mapIndexed { index, key ->
            KeyboardAccessibilityNode(
                virtualId = index,
                keyId = key.spec.id,
                label = key.accessibilityLabel(shiftState),
                bounds = key.bounds,
                hitBounds = key.hitBounds,
                selected = key.spec.role == KeyRole.SHIFT && shiftState != ShiftState.OFF,
            )
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
)

private fun app.funput.funput.keyboard.layout.ResolvedKey.accessibilityLabel(
    shiftState: ShiftState,
): String = if (spec.role == KeyRole.CHARACTER && shiftState.isActive) {
    spec.shiftedLabel ?: spec.accessibilityLabel
} else {
    spec.accessibilityLabel
}
