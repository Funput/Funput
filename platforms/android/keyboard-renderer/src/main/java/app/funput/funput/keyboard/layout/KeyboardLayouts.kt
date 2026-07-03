package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.model.SuggestionBarSpec

object KeyboardLayouts {
    fun forInputMethod(inputMethod: KeyboardInputMethod): KeyboardLayout = when (inputMethod) {
        KeyboardInputMethod.TELEX -> telex
        KeyboardInputMethod.VNI -> vni
    }

    val telex: KeyboardLayout = createLayout(
        id = "qwerty-telex",
        inputMethod = KeyboardInputMethod.TELEX,
        leadingRows = emptyList(),
    )

    val vni: KeyboardLayout = createLayout(
        id = "qwerty-vni",
        inputMethod = KeyboardInputMethod.VNI,
        leadingRows = listOf(vniModifierRow()),
    )

    private fun createLayout(
        id: String,
        inputMethod: KeyboardInputMethod,
        leadingRows: List<KeyboardRow>,
    ): KeyboardLayout = KeyboardLayout(
        id = id,
        inputMethod = inputMethod,
        suggestionBar = suggestionBar(),
        rows = buildList {
            addAll(leadingRows)
            add(characterRow("qwertyuiop"))
            add(characterRow("asdfghjkl", horizontalInsetUnits = 0.5f))
            add(bottomCharacterRow())
            add(actionRow())
        },
    )

    private fun characterRow(
        characters: String,
        horizontalInsetUnits: Float = 0f,
    ): KeyboardRow = KeyboardRow(
        keys = characters.map(::characterKey),
        horizontalInsetUnits = horizontalInsetUnits,
    )

    private fun bottomCharacterRow(): KeyboardRow = KeyboardRow(
        keys = buildList {
            add(specialKey("shift", "", KeyRole.SHIFT, widthWeight = 1.35f, accessibilityLabel = "Shift"))
            addAll("zxcvbnm".map(::characterKey))
            add(
                specialKey(
                    id = "backspace",
                    label = "",
                    role = KeyRole.BACKSPACE,
                    widthWeight = 1.35f,
                    accessibilityLabel = "Backspace",
                ),
            )
        },
    )

    private fun actionRow(): KeyboardRow = KeyboardRow(
        keys = listOf(
            specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.25f, accessibilityLabel = "Symbols"),
            specialKey("comma", ",", KeyRole.PUNCTUATION, 0.85f),
            KeySpec(
                id = "space",
                label = "VI ⇄ EN",
                role = KeyRole.SPACE,
                widthWeight = 5.3f,
                accessibilityLabel = "Space. Swipe to switch Vietnamese and English",
                horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
            ),
            specialKey("period", ".", KeyRole.PUNCTUATION, 0.85f),
            specialKey("enter", "", KeyRole.ENTER, 1.35f, accessibilityLabel = "Enter"),
        ),
    )

    private fun suggestionBar(): SuggestionBarSpec = SuggestionBarSpec(
        emojiKey = specialKey(
            id = "emoji",
            label = "",
            role = KeyRole.EMOJI,
            accessibilityLabel = "Emoji",
        ),
    )

    private fun vniModifierRow(): KeyboardRow {
        val hints = listOf("´", "`", "̉", "˜", "̣", "ˆ", "+", "˘", "đ", "×")
        val descriptions = listOf(
            "Sắc tone",
            "Huyền tone",
            "Hỏi tone",
            "Ngã tone",
            "Nặng tone",
            "Circumflex modifier",
            "Horn modifier",
            "Breve modifier",
            "D stroke modifier",
            "Remove tone",
        )

        return KeyboardRow(
            keys = (0..9).map { index ->
                val digit = if (index == 9) 0 else index + 1
                KeySpec(
                    id = "vni-$digit",
                    label = digit.toString(),
                    secondaryLabel = hints[index],
                    role = KeyRole.VNI_MODIFIER,
                    accessibilityLabel = descriptions[index],
                )
            },
        )
    }

    private fun characterKey(character: Char): KeySpec = KeySpec(
        id = "character-$character",
        label = character.toString(),
        role = KeyRole.CHARACTER,
        shiftedLabel = character.uppercaseChar().toString(),
        accessibilityLabel = character.toString(),
    )

    private fun specialKey(
        id: String,
        label: String,
        role: KeyRole,
        widthWeight: Float = 1f,
        accessibilityLabel: String = label,
    ): KeySpec = KeySpec(
        id = id,
        label = label,
        role = role,
        widthWeight = widthWeight,
        accessibilityLabel = accessibilityLabel,
    )
}
