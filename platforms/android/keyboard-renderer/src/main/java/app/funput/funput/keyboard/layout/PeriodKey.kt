package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.popover.model.KeyAlternate

/** The shared action-row period key; decimal keypad punctuation has its own policy. */
internal fun periodKey(id: String) = KeySpec(
    id = id,
    label = ".",
    role = KeyRole.PUNCTUATION,
    accessibilityLabel = "Dấu chấm",
    alternates = PeriodAlternates,
    preferredAlternateText = ",",
    alternatePaletteColumns = 8,
)

private val PeriodAlternates = listOf(
    KeyAlternate("@", accessibilityLabel = "a còng"),
    KeyAlternate("&", accessibilityLabel = "dấu và"),
    KeyAlternate("%", accessibilityLabel = "phần trăm"),
    KeyAlternate("+", accessibilityLabel = "dấu cộng"),
    KeyAlternate(";", accessibilityLabel = "dấu chấm phẩy"),
    KeyAlternate("/", accessibilityLabel = "dấu gạch chéo"),
    KeyAlternate("(", accessibilityLabel = "ngoặc mở"),
    KeyAlternate(")", accessibilityLabel = "ngoặc đóng"),
    KeyAlternate("\"", accessibilityLabel = "dấu nháy kép"),
    KeyAlternate("'", accessibilityLabel = "dấu nháy đơn"),
    KeyAlternate("#", accessibilityLabel = "dấu thăng"),
    KeyAlternate("-", accessibilityLabel = "dấu trừ"),
    KeyAlternate(":", accessibilityLabel = "dấu hai chấm"),
    KeyAlternate("!", accessibilityLabel = "dấu chấm than"),
    KeyAlternate(",", accessibilityLabel = "dấu phẩy"),
    KeyAlternate("?", accessibilityLabel = "dấu hỏi"),
)
