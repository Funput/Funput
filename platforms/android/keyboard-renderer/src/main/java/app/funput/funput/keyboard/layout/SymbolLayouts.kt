package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.model.SuggestionBarSpec

internal object SymbolLayouts {
    fun primary(inputMethod: KeyboardInputMethod): KeyboardLayout = create(
        id = "symbols-primary",
        inputMethod = inputMethod,
        rows = listOf(
            symbolRow("primary", 0, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")),
            symbolRow("primary", 1, listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")),
            bottomRow("primary", KeyRole.MORE_SYMBOLS, "=\\<", listOf("*", "\"", "'", ":", ";", "!", "?")),
            actionRow("primary"),
        ),
    )

    fun secondary(inputMethod: KeyboardInputMethod): KeyboardLayout = create(
        id = "symbols-secondary",
        inputMethod = inputMethod,
        rows = listOf(
            symbolRow("secondary", 0, listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")),
            symbolRow("secondary", 1, listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")),
            bottomRow("secondary", KeyRole.SYMBOLS, "?123", listOf("`", "´", "©", "®", "™", "✓", "×")),
            actionRow("secondary"),
        ),
    )

    private fun create(
        id: String,
        inputMethod: KeyboardInputMethod,
        rows: List<KeyboardRow>,
    ) = KeyboardLayout(
        id = "$id-${inputMethod.name.lowercase()}",
        inputMethod = inputMethod,
        suggestionBar = SuggestionBarSpec(
            emojiKey = specialKey("emoji-$id", "", KeyRole.EMOJI, accessibilityLabel = "Emoji"),
        ),
        rows = rows,
    )

    private fun symbolRow(page: String, row: Int, labels: List<String>) = KeyboardRow(
        keys = labels.mapIndexed { index, label -> symbolKey(page, row, index, label) },
    )

    private fun bottomRow(
        page: String,
        switchRole: KeyRole,
        switchLabel: String,
        symbols: List<String>,
    ) = KeyboardRow(
        keys = buildList {
            add(specialKey("switch-$page", switchLabel, switchRole, 1.35f, "Switch symbol page"))
            addAll(symbols.mapIndexed { index, label -> symbolKey(page, 2, index, label) })
            add(specialKey("backspace-$page", "", KeyRole.BACKSPACE, 1.35f, "Backspace"))
        },
    )

    private fun actionRow(page: String) = KeyboardRow(
        keys = listOf(
            specialKey("letters-$page", "ABC", KeyRole.LETTERS, 1.25f, "Letters"),
            specialKey("comma-$page", ",", KeyRole.PUNCTUATION, 0.85f),
            KeySpec(
                id = "space-$page",
                label = "Tiếng Việt",
                role = KeyRole.SPACE,
                widthWeight = 5.3f,
                accessibilityLabel = "Dấu cách. Vuốt để đổi Tiếng Việt và Tiếng Anh",
                horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
            ),
            specialKey("period-$page", ".", KeyRole.PUNCTUATION, 0.85f),
            specialKey("enter-$page", "", KeyRole.ENTER, 1.35f, "Enter"),
        ),
    )

    private fun symbolKey(page: String, row: Int, index: Int, label: String) = KeySpec(
        id = "symbol-$page-$row-$index",
        label = label,
        role = KeyRole.PUNCTUATION,
        accessibilityLabel = label,
    )

    private fun specialKey(
        id: String,
        label: String,
        role: KeyRole,
        widthWeight: Float = 1f,
        accessibilityLabel: String = label,
    ) = KeySpec(id, label, role, widthWeight, accessibilityLabel = accessibilityLabel)
}
