package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.popover.model.KeyAlternate

/**
 * Digit hints and long-press digits for the top character row.
 *
 * A compact page — letters, search, email or URL — hides the number row, which leaves the symbols
 * page as the only way to reach a digit. Printing the digit on the keycap and offering it as the
 * pre-selected long-press alternate restores that reach without spending a row.
 */
internal object CompactDigitAlternates {
    private const val RowCharacters = "qwertyuiop"
    private const val Digits = "1234567890"

    fun decorate(layout: KeyboardLayout): KeyboardLayout =
        layout.copy(rows = layout.rows.map(::decorate))

    /**
     * Matches the row by its own labels rather than by index: the compact page has no number
     * row in front of it, so the top character row is not at a fixed position.
     */
    private fun decorate(row: KeyboardRow): KeyboardRow {
        if (row.keys.map { it.label } != RowCharacters.map(Char::toString)) return row
        return row.copy(keys = row.keys.mapIndexed { index, key -> apply(Digits[index], key) })
    }

    private fun apply(digit: Char, key: KeySpec): KeySpec = key.copy(
        // A Telex tone hint keeps the slot it already owns — `r` stays the hỏi key — and the
        // digit joins it on the right, where every other key carries one.
        secondaryLabel = key.secondaryLabel?.let { "$it $digit" } ?: digit.toString(),
        accessibilityLabel = "${key.accessibilityLabel}, số $digit",
        // First place wins the palette's default selection, which is what makes a plain
        // hold-and-release type the digit.
        alternates = listOf(
            KeyAlternate(digit.toString(), accessibilityLabel = "Số $digit"),
        ) + key.alternates,
    )
}
