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
        add(characterRow("qwertyuiop", supportsVietnameseAlternates = supportsVietnameseAlternates))
        add(characterRow("asdfghjkl", 0.5f, supportsVietnameseAlternates))
        add(bottomCharacterRow(supportsVietnameseAlternates))
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

private fun characterRow(
    characters: String,
    horizontalInsetUnits: Float = 0f,
    supportsVietnameseAlternates: Boolean = false,
) = KeyboardRow(
    keys = characters.map { characterKey(it, supportsVietnameseAlternates) },
    horizontalInsetUnits = horizontalInsetUnits,
)

private fun bottomCharacterRow(supportsVietnameseAlternates: Boolean) = KeyboardRow(
    keys = buildList {
        add(specialKey("shift", "", KeyRole.SHIFT, 1.5f, "Shift"))
        addAll("zxcvbnm".map { characterKey(it, supportsVietnameseAlternates) })
        add(specialKey("backspace", "", KeyRole.BACKSPACE, 1.5f, "Backspace"))
    },
)

private fun characterKey(character: Char, supportsVietnameseAlternates: Boolean) = KeySpec(
    id = "character-$character",
    label = character.toString(),
    role = KeyRole.CHARACTER,
    shiftedLabel = character.uppercaseChar().toString(),
    accessibilityLabel = character.toString(),
    alternates = if (supportsVietnameseAlternates) {
        VietnameseKeyAlternates.valuesFor(character)
    } else {
        emptyList()
    },
)

internal fun keyboardToolbarSpec() = SuggestionBarSpec(
    settingsKey = specialKey("settings", "", KeyRole.SETTINGS, accessibilityLabel = "Settings"),
    emojiKey = specialKey("emoji", "", KeyRole.EMOJI, accessibilityLabel = "Emoji"),
    suggestionsEnabled = KeyboardFeatures.SuggestionsEnabled,
)
