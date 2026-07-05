package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLayout

/** Adds Android's low-priority system-IME switch action to an existing toolbar. */
internal fun KeyboardLayout.withSystemInputMethodSwitcher(visible: Boolean): KeyboardLayout {
    if (!visible) return this
    val bar = suggestionBar ?: return this
    if (bar.systemInputMethodKey != null) return this

    return copy(
        id = "$id-system-switcher",
        suggestionBar = bar.copy(systemInputMethodKey = systemInputMethodSwitcher()),
    )
}

private fun systemInputMethodSwitcher() = KeySpec(
    id = "system-input-method",
    label = "",
    role = KeyRole.SYSTEM_INPUT_METHOD,
    accessibilityLabel = "Switch keyboard",
)
