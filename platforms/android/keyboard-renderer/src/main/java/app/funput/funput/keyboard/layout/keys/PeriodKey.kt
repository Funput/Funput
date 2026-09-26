package app.funput.funput.keyboard.layout.keys

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
    alternatePaletteColumns = 8,
)

private val PeriodAlternates = listOf(
    KeyAlternate.Text("@", accessibilityLabel = "a còng"),
    KeyAlternate.Text("&", accessibilityLabel = "dấu và"),
    KeyAlternate.Text("%", accessibilityLabel = "phần trăm"),
    KeyAlternate.Text("+", accessibilityLabel = "dấu cộng"),
    KeyAlternate.Text(";", accessibilityLabel = "dấu chấm phẩy"),
    KeyAlternate.Text("/", accessibilityLabel = "dấu gạch chéo"),
    KeyAlternate.Text("(", accessibilityLabel = "ngoặc mở"),
    KeyAlternate.Text(")", accessibilityLabel = "ngoặc đóng"),
    KeyAlternate.Text("\"", accessibilityLabel = "dấu nháy kép"),
    KeyAlternate.Text("'", accessibilityLabel = "dấu nháy đơn"),
    KeyAlternate.Text("#", accessibilityLabel = "dấu thăng"),
    KeyAlternate.Text("-", accessibilityLabel = "dấu trừ"),
    KeyAlternate.Text(":", accessibilityLabel = "dấu hai chấm"),
    KeyAlternate.Text("!", accessibilityLabel = "dấu chấm than"),
    KeyAlternate.Text("?", accessibilityLabel = "dấu hỏi"),
)
