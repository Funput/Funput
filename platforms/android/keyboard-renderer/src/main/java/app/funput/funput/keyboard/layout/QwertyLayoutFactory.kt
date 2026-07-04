package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.model.SuggestionBarSpec

internal fun qwertyLayout(
    id: String,
    inputMethod: KeyboardInputMethod,
    leadingRows: List<KeyboardRow> = emptyList(),
    actionKeys: List<KeySpec>,
): KeyboardLayout = KeyboardLayout(
    id = id,
    inputMethod = inputMethod,
    suggestionBar = SuggestionBarSpec(
        emojiKey = specialKey("emoji", "", KeyRole.EMOJI, accessibilityLabel = "Emoji"),
    ),
    rows = buildList {
        addAll(leadingRows)
        add(characterRow("qwertyuiop"))
        add(characterRow("asdfghjkl", horizontalInsetUnits = 0.5f))
        add(bottomCharacterRow())
        add(KeyboardRow(actionKeys))
    },
)

internal fun standardSpaceKey(widthWeight: Float = 5.3f) = KeySpec(
    id = "space",
    label = "Tiếng Việt",
    role = KeyRole.SPACE,
    widthWeight = widthWeight,
    accessibilityLabel = "Dấu cách. Vuốt để đổi Tiếng Việt và Tiếng Anh",
    horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
)

internal fun asciiSpaceKey(widthWeight: Float) = KeySpec(
    id = "space",
    label = "English",
    role = KeyRole.SPACE,
    widthWeight = widthWeight,
    accessibilityLabel = "Space",
)

internal fun specialKey(
    id: String,
    label: String,
    role: KeyRole,
    widthWeight: Float = 1f,
    accessibilityLabel: String = label,
) = KeySpec(id, label, role, widthWeight, accessibilityLabel = accessibilityLabel)

internal fun vniModifierRow(): KeyboardRow {
    val hints = listOf("´", "`", "̉", "˜", "̣", "ˆ", "+", "˘", "đ", "×")
    val descriptions = listOf(
        "Sắc tone", "Huyền tone", "Hỏi tone", "Ngã tone", "Nặng tone",
        "Circumflex modifier", "Horn modifier", "Breve modifier",
        "D stroke modifier", "Remove tone",
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

private fun characterRow(characters: String, horizontalInsetUnits: Float = 0f) = KeyboardRow(
    keys = characters.map(::characterKey),
    horizontalInsetUnits = horizontalInsetUnits,
)

private fun bottomCharacterRow() = KeyboardRow(
    keys = buildList {
        add(specialKey("shift", "", KeyRole.SHIFT, 1.35f, "Shift"))
        addAll("zxcvbnm".map(::characterKey))
        add(specialKey("backspace", "", KeyRole.BACKSPACE, 1.35f, "Backspace"))
    },
)

private fun characterKey(character: Char) = KeySpec(
    id = "character-$character",
    label = character.toString(),
    role = KeyRole.CHARACTER,
    shiftedLabel = character.uppercaseChar().toString(),
    accessibilityLabel = character.toString(),
)
