package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardRow

internal fun keypadRow(vararg keys: KeySpec) = KeyboardRow(keys.toList())

internal fun keypadDigit(value: Char) = KeySpec(
    id = "keypad-digit-$value",
    label = value.toString(),
    role = KeyRole.CHARACTER,
    accessibilityLabel = value.toString(),
)

internal fun keypadCommand(id: String, role: KeyRole, accessibilityLabel: String) = KeySpec(
    id = id,
    label = "",
    role = role,
    accessibilityLabel = accessibilityLabel,
)

internal fun keypadText(id: String, value: String, accessibilityLabel: String) = KeySpec(
    id = id,
    label = value,
    role = KeyRole.PUNCTUATION,
    accessibilityLabel = accessibilityLabel,
)

internal fun keypadEmpty(position: String) = KeySpec(
    id = "placeholder-$position",
    label = "",
    role = KeyRole.PLACEHOLDER,
    accessibilityLabel = "Empty keypad position",
)
