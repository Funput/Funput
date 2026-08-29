package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object EditorKeyboardLayouts {
    fun resolve(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        suggestionsEnabled: Boolean,
        showsNumberRow: Boolean = true,
    ): KeyboardLayout {
        val layout = when (editorMode) {
            KeyboardEditorMode.TEXT -> KeyboardLayouts.forInputMethod(inputMethod, showsNumberRow)
            KeyboardEditorMode.SEARCH,
            KeyboardEditorMode.EMAIL,
            KeyboardEditorMode.URL,
            -> webLayout(editorMode, inputMethod, showsNumberRow)
            KeyboardEditorMode.PHONE -> PhoneKeyboardLayouts.resolve(inputMethod, suggestionsEnabled)
            KeyboardEditorMode.PASSWORD -> PasswordKeyboardLayouts.text(inputMethod)
            KeyboardEditorMode.PIN -> PasswordKeyboardLayouts.pin(inputMethod)
            KeyboardEditorMode.NUMBER,
            KeyboardEditorMode.NUMBER_DECIMAL,
            KeyboardEditorMode.NUMBER_SIGNED,
            KeyboardEditorMode.NUMBER_SIGNED_DECIMAL,
            -> NumberKeyboardLayouts.resolve(inputMethod, editorMode, suggestionsEnabled)
        }
        return if (suggestionsEnabled || layout.suggestionBar == null) {
            layout
        } else {
            layout.copy(
                id = "${layout.id}-no-suggestions",
                suggestionBar = layout.suggestionBar.copy(suggestionsEnabled = false),
            )
        }
    }

    /**
     * The QWERTY page behind search, email and URL: one shape, one action row, and one answer to
     * the number row preference.
     *
     * Search composes Vietnamese, so it keeps the tone hints and the VNI modifier row of the
     * letters page; email and URL do not compose, so their row stays plain digits. All three
     * follow the letters page on the preference — with the row hidden, the digits come back as
     * long-press alternates on the top row.
     */
    private fun webLayout(
        editorMode: KeyboardEditorMode,
        inputMethod: KeyboardInputMethod,
        showsNumberRow: Boolean,
    ): KeyboardLayout {
        val composesVietnamese = editorMode.supportsVietnameseComposition
        val pageId = "${editorMode.name.lowercase()}-${inputMethod.name.lowercase()}"
        val isCompact = usesCompactLetterRows(inputMethod, editorMode, showsNumberRow)
        val layout = qwertyLayout(
            id = if (isCompact) "qwerty-$pageId-compact" else "qwerty-$pageId",
            inputMethod = inputMethod,
            leadingRows = if (isCompact) {
                emptyList()
            } else {
                listOf(topNumberRowFor(inputMethod, pageId, composesVietnamese))
            },
            actionKeys = webActionKeys(editorMode),
            supportsVietnameseAlternates = composesVietnamese,
            showsTelexHints = composesVietnamese && inputMethod.isTelexFamily,
        )
        return if (isCompact) CompactDigitAlternates.decorate(layout) else layout
    }

    /**
     * Email reaches for `@`; search and URL reach for `/`. Only search keeps the language swipe on
     * the space bar, because only search composes.
     */
    private fun webActionKeys(editorMode: KeyboardEditorMode): List<KeySpec> {
        val middle = if (editorMode == KeyboardEditorMode.EMAIL) {
            Triple("at", "@", "A còng")
        } else {
            Triple("slash", "/", "Dấu gạch chéo")
        }
        return listOf(
            specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.7f, "Ký hiệu"),
            specialKey(
                id = middle.first,
                label = middle.second,
                role = KeyRole.PUNCTUATION,
                accessibilityLabel = middle.third,
            ),
            if (editorMode.supportsVietnameseComposition) {
                standardSpaceKey(5.8f)
            } else {
                asciiSpaceKey(5.8f)
            },
            specialKey("period", ".", KeyRole.PUNCTUATION, accessibilityLabel = "Dấu chấm"),
            specialKey("enter", "", KeyRole.ENTER, 1.7f, "Enter"),
        )
    }
}
