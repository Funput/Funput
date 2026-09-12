package app.funput.funput.keyboard.layout

/**
 * Symbol key labels grouped by page and row for the 5-row Samsung-style grid.
 *
 * Rows keep a 10 / 10 / 7 shape so every key shares one width (the bottom row also
 * carries the 1.5-weight page-switch and backspace keys). Symbols shared with Gboard
 * follow its page order; Funput-only glyphs fill the remaining trailing positions.
 * No glyph is repeated across the two pages.
 */
internal object SymbolPageContent {
    val primaryRow1 = listOf("@", "#", "₫", "_", "&", "-", "+", "(", ")", "/")
    val primaryRow2 = listOf("*", "\"", "'", ":", ";", "!", "?", "…", "<", ">")
    val primaryRow3 = listOf("¥", "¶", "·", "≠", "±", "≈", "≤")

    val secondaryRow1 = listOf("~", "`", "|", "•", "√", "÷", "×", "§", "£", "€")
    val secondaryRow2 = listOf("$", "¢", "^", "°", "=", "{", "}", "\\", "%", "©")
    val secondaryRow3 = listOf("®", "™", "✓", "[", "]", "≥", "∞")
}
