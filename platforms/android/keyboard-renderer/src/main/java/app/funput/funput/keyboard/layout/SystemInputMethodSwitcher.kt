package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow

/** Adds Android's system-IME switch action without coupling layouts to the framework. */
internal fun KeyboardLayout.withSystemInputMethodSwitcher(visible: Boolean): KeyboardLayout {
    if (!visible) return this
    val rowIndex = rows.indexOfLast { row -> row.keys.any { it.role == KeyRole.SPACE } }
        .takeIf { it >= 0 }
        ?: rows.indexOfLast { row -> row.keys.any { it.role == KeyRole.PLACEHOLDER } }
    if (rowIndex < 0) return this

    val updatedRow = rows[rowIndex].withSystemInputMethodSwitcher()
    return copy(
        id = "$id-system-switcher",
        rows = rows.toMutableList().apply { this[rowIndex] = updatedRow },
    )
}

private fun KeyboardRow.withSystemInputMethodSwitcher(): KeyboardRow {
    val spaceIndex = keys.indexOfFirst { it.role == KeyRole.SPACE }
    if (spaceIndex >= 0) {
        val updated = keys.toMutableList()
        updated[spaceIndex] = updated[spaceIndex].copy(
            widthWeight = (updated[spaceIndex].widthWeight - SwitcherWidth).coerceAtLeast(MinimumSpaceWidth),
        )
        updated.add(spaceIndex, systemInputMethodSwitcher(widthWeight = SwitcherWidth))
        return copy(keys = updated)
    }

    val placeholderIndex = keys.indexOfFirst { it.role == KeyRole.PLACEHOLDER }
    return copy(keys = keys.toMutableList().apply {
        this[placeholderIndex] = systemInputMethodSwitcher(widthWeight = keys[placeholderIndex].widthWeight)
    })
}

private fun systemInputMethodSwitcher(widthWeight: Float) = KeySpec(
    id = "system-input-method",
    label = "",
    role = KeyRole.SYSTEM_INPUT_METHOD,
    widthWeight = widthWeight,
    accessibilityLabel = "Switch keyboard",
)

private const val SwitcherWidth = 1.3f
private const val MinimumSpaceWidth = 2.4f
