package app.funput.funput.keyboard.ui.emoji

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow

internal object EmojiSearchKeyboardLayout {
    val layout = KeyboardLayout(
        id = "emoji-search-qwerty",
        inputMethod = KeyboardInputMethod.TELEX,
        suggestionBar = null,
        rows = listOf(
            letters("qwertyuiop"),
            letters("asdfghjkl", 0.5f),
            KeyboardRow(
                listOf(key("shift", "", KeyRole.SHIFT, 1.5f, "Shift")) +
                    "zxcvbnm".map(::letter) +
                    key("backspace", "", KeyRole.BACKSPACE, 1.5f, "Xóa"),
            ),
            KeyboardRow(
                listOf(
                    key("emoji", "☺", KeyRole.EMOJI, 1.7f, "Biểu tượng"),
                    key("space", "Tìm emoji", KeyRole.SPACE, 5.8f, "Dấu cách"),
                    key("done", "Xong", KeyRole.ENTER, 1.7f, "Xong"),
                ),
            ),
        ),
    )

    private fun letters(value: String, inset: Float = 0f) =
        KeyboardRow(value.map(::letter), inset)

    private fun letter(value: Char) = key(
        "search-$value",
        value.toString(),
        KeyRole.CHARACTER,
        shifted = value.uppercaseChar().toString(),
    )

    private fun key(
        id: String,
        label: String,
        role: KeyRole,
        weight: Float = 1f,
        accessibility: String = label,
        shifted: String? = null,
    ) = KeySpec(
        id,
        label,
        role,
        weight,
        shifted,
        accessibilityLabel = accessibility,
        spaceLabelOverride = label.takeIf { role == KeyRole.SPACE },
    )
}
