package app.funput.funput.keyboard.layout.keys

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.popover.model.KeyAlternate

/** Utilities belong to action-row commas, not decimal keypad separators. */
internal fun commaKey(id: String) = KeySpec(
    id = id,
    label = ",",
    role = KeyRole.PUNCTUATION,
    accessibilityLabel = "Dấu phẩy",
    alternates = listOf(KeyAlternate.Action.PLACEMENT, KeyAlternate.Action.SETTINGS),
    alternatePaletteColumns = 2,
)
