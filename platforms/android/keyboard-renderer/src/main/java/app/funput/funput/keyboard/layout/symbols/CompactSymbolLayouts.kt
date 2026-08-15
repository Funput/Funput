package app.funput.funput.keyboard.layout.symbols

import app.funput.funput.keyboard.KeyboardFeatures
import app.funput.funput.keyboard.layout.TopNumberRowMode
import app.funput.funput.keyboard.layout.specialKey
import app.funput.funput.keyboard.layout.standardSpaceKey
import app.funput.funput.keyboard.layout.topNumberRow
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.model.SuggestionBarSpec

/** Four-row symbol pages used when Telex-family letters hide the number row. */
internal object CompactSymbolLayouts {
    fun primary(
        inputMethod: KeyboardInputMethod,
        suggestionsEnabled: Boolean = KeyboardFeatures.SuggestionsEnabled,
    ): KeyboardLayout = create(
        id = "symbols-primary-${inputMethod.name.lowercase()}-compact",
        inputMethod = inputMethod,
        suggestionsEnabled = suggestionsEnabled,
        rows = listOf(
            topNumberRow(TopNumberRowMode.PLAIN_PUNCTUATION, pageId = "compact-primary"),
            symbolRow("compact-primary", CompactSymbolPageContent.primaryMiddle),
            bottomRow(
                page = "compact-primary",
                switchRole = KeyRole.MORE_SYMBOLS,
                switchLabel = "=\\<",
                symbols = CompactSymbolPageContent.primaryBottom,
            ),
            actionRow("compact-primary"),
        ),
    )

    fun secondary(
        inputMethod: KeyboardInputMethod,
        suggestionsEnabled: Boolean = KeyboardFeatures.SuggestionsEnabled,
    ): KeyboardLayout = create(
        id = "symbols-secondary-${inputMethod.name.lowercase()}-compact",
        inputMethod = inputMethod,
        suggestionsEnabled = suggestionsEnabled,
        rows = listOf(
            symbolRow("compact-secondary-top", CompactSymbolPageContent.secondaryTop),
            symbolRow("compact-secondary-middle", CompactSymbolPageContent.secondaryMiddle),
            bottomRow(
                page = "compact-secondary",
                switchRole = KeyRole.SYMBOLS,
                switchLabel = "?123",
                symbols = CompactSymbolPageContent.secondaryBottom,
            ),
            actionRow("compact-secondary"),
        ),
    )

    private fun create(
        id: String,
        inputMethod: KeyboardInputMethod,
        suggestionsEnabled: Boolean,
        rows: List<KeyboardRow>,
    ) = KeyboardLayout(
        id = id + suggestionSuffix(suggestionsEnabled),
        inputMethod = inputMethod,
        suggestionBar = SuggestionBarSpec(
            clipboardKey = specialKey("clipboard-$id", "", KeyRole.CLIPBOARD, accessibilityLabel = "Clipboard"),
            emojiKey = specialKey("emoji-$id", "", KeyRole.EMOJI, accessibilityLabel = "Emoji"),
            suggestionsEnabled = KeyboardFeatures.SuggestionsEnabled && suggestionsEnabled,
        ).takeIf { KeyboardFeatures.EmojiToolbarEnabled },
        rows = rows,
    )

    private fun symbolRow(page: String, labels: List<String>) = KeyboardRow(
        keys = labels.mapIndexed { index, label ->
            KeySpec(
                id = "symbol-$page-$index",
                label = label,
                role = KeyRole.PUNCTUATION,
                accessibilityLabel = label,
            )
        },
    )

    private fun bottomRow(
        page: String,
        switchRole: KeyRole,
        switchLabel: String,
        symbols: List<String>,
    ) = KeyboardRow(
        keys = buildList {
            add(specialKey("switch-$page", switchLabel, switchRole, 1.5f, "Switch symbol page"))
            addAll(
                symbols.mapIndexed { index, label ->
                    KeySpec(
                        id = "symbol-$page-bottom-$index",
                        label = label,
                        role = KeyRole.PUNCTUATION,
                        accessibilityLabel = label,
                    )
                },
            )
            add(specialKey("backspace-$page", "", KeyRole.BACKSPACE, 1.5f, "Backspace"))
        },
    )

    private fun actionRow(page: String) = KeyboardRow(
        keys = listOf(
            specialKey("letters-$page", "ABC", KeyRole.LETTERS, 1.7f, "Letters"),
            specialKey("comma-$page", ",", KeyRole.PUNCTUATION),
            standardSpaceKey(),
            specialKey("period-$page", ".", KeyRole.PUNCTUATION),
            specialKey("enter-$page", "", KeyRole.ENTER, 1.7f, "Enter"),
        ),
    )

    private fun suggestionSuffix(suggestionsEnabled: Boolean): String =
        if (!KeyboardFeatures.SuggestionsEnabled || !suggestionsEnabled) "-no-suggestions" else ""
}
