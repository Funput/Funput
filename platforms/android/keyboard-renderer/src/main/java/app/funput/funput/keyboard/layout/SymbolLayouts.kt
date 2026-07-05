package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.model.SuggestionBarSpec

internal object SymbolLayouts {
    fun primary(inputMethod: KeyboardInputMethod, secure: Boolean = false): KeyboardLayout = create(
        id = "symbols-primary",
        inputMethod = inputMethod,
        secure = secure,
        rows = listOf(
            symbolRow("primary", 0, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")),
            symbolRow("primary", 1, listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")),
            bottomRow("primary", KeyRole.MORE_SYMBOLS, "=\\<", listOf("*", "\"", "'", ":", ";", "!", "?")),
            actionRow("primary", secure),
        ),
    )

    fun secondary(inputMethod: KeyboardInputMethod, secure: Boolean = false): KeyboardLayout = create(
        id = "symbols-secondary",
        inputMethod = inputMethod,
        secure = secure,
        rows = listOf(
            symbolRow("secondary", 0, listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")),
            symbolRow("secondary", 1, listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")),
            bottomRow("secondary", KeyRole.SYMBOLS, "?123", listOf("`", "´", "©", "®", "™", "✓", "×")),
            actionRow("secondary", secure),
        ),
    )

    private fun create(
        id: String,
        inputMethod: KeyboardInputMethod,
        secure: Boolean,
        rows: List<KeyboardRow>,
    ) = KeyboardLayout(
        id = "$id-${inputMethod.name.lowercase()}${if (secure) "-secure" else ""}",
        inputMethod = inputMethod,
        suggestionBar = if (secure) null else SuggestionBarSpec(
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

    private fun actionRow(page: String, secure: Boolean) = KeyboardRow(
        keys = listOf(
            specialKey("letters-$page", "ABC", KeyRole.LETTERS, 1.25f, "Letters"),
            specialKey("comma-$page", ",", KeyRole.PUNCTUATION, 0.85f),
            symbolSpace(page, secure),
            specialKey("period-$page", ".", KeyRole.PUNCTUATION, 0.85f),
            specialKey("enter-$page", "", KeyRole.ENTER, 1.35f, "Enter"),
        ),
    )

    private fun symbolSpace(page: String, secure: Boolean) = KeySpec(
        id = "space-$page",
        label = if (secure) "English" else "Tiếng Việt",
        role = KeyRole.SPACE,
        widthWeight = 5.3f,
        accessibilityLabel = if (secure) "Space" else "Dấu cách. Vuốt để đổi ngôn ngữ",
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE.takeUnless { secure },
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
