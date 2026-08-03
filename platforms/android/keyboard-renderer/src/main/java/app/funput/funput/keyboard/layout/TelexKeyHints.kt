package app.funput.funput.keyboard.layout

/** Tone and clear-key secondary hints for Telex-family letter keys (iOS parity). */
internal object TelexKeyHints {
    data class Hint(val glyph: String, val description: String)

    private val ByCharacter = mapOf(
        's' to Hint("´", "dấu sắc"),
        'f' to Hint("`", "dấu huyền"),
        'r' to Hint("̉", "dấu hỏi"),
        'x' to Hint("˜", "dấu ngã"),
        'j' to Hint("̣", "dấu nặng"),
        'z' to Hint("×", "xóa dấu"),
    )

    fun hint(character: Char): Hint? = ByCharacter[character.lowercaseChar()]

    fun accessibilityLabel(character: Char): String? {
        val hint = hint(character) ?: return null
        return "${character.uppercaseChar()}, ${hint.description}"
    }
}
