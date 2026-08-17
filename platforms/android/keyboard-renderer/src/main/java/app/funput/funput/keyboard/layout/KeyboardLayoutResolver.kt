package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardFeatures
import app.funput.funput.keyboard.layout.symbols.CompactSymbolLayouts
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode

internal object KeyboardLayoutResolver {
    fun resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        suggestionsEnabled: Boolean = KeyboardFeatures.SuggestionsEnabled,
        systemInputMethodSwitcherVisible: Boolean = false,
        showsNumberRow: Boolean = true,
    ): KeyboardLayout {
        val usesCompact = usesCompactLetterRows(inputMethod, editorMode, showsNumberRow)
        val layout = when (mode) {
            KeyboardLayoutMode.LETTERS -> EditorKeyboardLayouts.resolve(
                inputMethod = inputMethod,
                editorMode = editorMode,
                suggestionsEnabled = suggestionsEnabled,
                showsNumberRow = showsNumberRow,
            )
            KeyboardLayoutMode.SYMBOLS_PRIMARY -> if (usesCompact) {
                CompactSymbolLayouts.primary(inputMethod, suggestionsEnabled)
            } else {
                SymbolLayouts.primary(inputMethod, suggestionsEnabled, editorMode.isPassword)
            }
            KeyboardLayoutMode.SYMBOLS_SECONDARY -> if (usesCompact) {
                CompactSymbolLayouts.secondary(inputMethod, suggestionsEnabled)
            } else {
                SymbolLayouts.secondary(inputMethod, suggestionsEnabled, editorMode.isPassword)
            }
        }
        return layout.withSystemInputMethodSwitcher(systemInputMethodSwitcherVisible)
    }
}
