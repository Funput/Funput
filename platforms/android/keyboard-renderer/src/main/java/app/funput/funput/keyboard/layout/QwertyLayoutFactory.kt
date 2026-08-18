package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardFeatures
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.model.SuggestionBarSpec
import app.funput.funput.keyboard.popover.model.VietnameseKeyAlternates

internal fun qwertyLayout(
    id: String,
    inputMethod: KeyboardInputMethod,
    leadingRows: List<KeyboardRow> = emptyList(),
    actionKeys: List<KeySpec>,
    showSuggestionBar: Boolean = KeyboardFeatures.EmojiToolbarEnabled,
    supportsVietnameseAlternates: Boolean = false,
    showsTelexHints: Boolean = false,
): KeyboardLayout = KeyboardLayout(
    id = id,
    inputMethod = inputMethod,
    suggestionBar = if (showSuggestionBar) {
        keyboardToolbarSpec()
    } else {
        null
    },
    rows = buildList {
        addAll(leadingRows)
        add(characterRow("qwertyuiop", supportsVietnameseAlternates = supportsVietnameseAlternates, showsTelexHints = showsTelexHints))
        add(characterRow("asdfghjkl", 0.5f, supportsVietnameseAlternates, showsTelexHints))
        add(bottomCharacterRow(supportsVietnameseAlternates, showsTelexHints))
        add(KeyboardRow(actionKeys))
    },
)

internal fun standardSpaceKey(widthWeight: Float = 5.8f) = KeySpec(
    id = "space",
    label = "Tiếng Việt",
    role = KeyRole.SPACE,
    widthWeight = widthWeight,
    accessibilityLabel = "Dấu cách. Vuốt để đổi Tiếng Việt và Tiếng Anh",
    horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
)

internal fun asciiSpaceKey(widthWeight: Float) = KeySpec(
    id = "space",
    label = "Tiếng Anh",
    role = KeyRole.SPACE,
    widthWeight = widthWeight,
    accessibilityLabel = "Dấu cách",
)

internal fun specialKey(
    id: String,
    label: String,
    role: KeyRole,
    widthWeight: Float = 1f,
    accessibilityLabel: String = label,
) = KeySpec(id, label, role, widthWeight, accessibilityLabel = accessibilityLabel)

private fun characterRow(
    characters: String,
    horizontalInsetUnits: Float = 0f,
    supportsVietnameseAlternates: Boolean = false,
    showsTelexHints: Boolean = false,
) = KeyboardRow(
    keys = characters.map { characterKey(it, supportsVietnameseAlternates, showsTelexHints) },
    horizontalInsetUnits = horizontalInsetUnits,
)

private fun bottomCharacterRow(
    supportsVietnameseAlternates: Boolean,
    showsTelexHints: Boolean,
) = KeyboardRow(
    keys = buildList {
        add(specialKey("shift", "", KeyRole.SHIFT, 1.5f, "Shift"))
        addAll("zxcvbnm".map { characterKey(it, supportsVietnameseAlternates, showsTelexHints) })
        add(specialKey("backspace", "", KeyRole.BACKSPACE, 1.5f, "Xóa"))
    },
)

private fun characterKey(
    character: Char,
    supportsVietnameseAlternates: Boolean,
    showsTelexHints: Boolean,
): KeySpec {
    val hint = TelexKeyHints.hint(character).takeIf { showsTelexHints }
    return KeySpec(
        id = "character-$character",
        label = character.toString(),
        role = KeyRole.CHARACTER,
        shiftedLabel = character.uppercaseChar().toString(),
        secondaryLabel = hint?.glyph,
        accessibilityLabel = hint?.let { TelexKeyHints.accessibilityLabel(character) }
            ?: character.toString(),
        alternates = if (supportsVietnameseAlternates) {
            VietnameseKeyAlternates.valuesFor(character)
        } else {
            emptyList()
        },
    )
}

internal fun keyboardToolbarSpec() = SuggestionBarSpec(
    clipboardKey = specialKey("clipboard", "", KeyRole.CLIPBOARD, accessibilityLabel = "Lịch sử clipboard"),
    emojiKey = specialKey("emoji", "", KeyRole.EMOJI, accessibilityLabel = "Biểu tượng cảm xúc"),
    suggestionsEnabled = KeyboardFeatures.SuggestionsEnabled,
)
