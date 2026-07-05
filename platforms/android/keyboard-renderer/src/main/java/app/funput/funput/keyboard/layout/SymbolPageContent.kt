package app.funput.funput.keyboard.layout

/**
 * Symbol key labels grouped by page and row for the 5-row Samsung-style grid.
 *
 * Rows keep a 10 / 10 / 7 shape so every key shares one width (the bottom row also
 * carries the 1.5-weight page-switch and backspace keys). Page 1 holds the everyday
 * symbols for fast access; page 2 holds technical, foreign-currency and math glyphs.
 * No glyph is repeated across the two pages.
 */
internal object SymbolPageContent {
    val primaryRow1 = listOf("@", "#", "₫", "$", "%", "&", "*", "+", "=", "/")
    val primaryRow2 = listOf("(", ")", "-", "_", ":", ";", "'", "\"", "!", "?")
    val primaryRow3 = listOf("~", "•", "…", "°", "×", "÷", "^")

    val secondaryRow1 = listOf("[", "]", "{", "}", "<", ">", "\\", "|", "`", "§")
    val secondaryRow2 = listOf("€", "£", "¥", "¢", "©", "®", "™", "¶", "·", "✓")
    val secondaryRow3 = listOf("≠", "±", "≈", "≤", "≥", "√", "∞")
}
