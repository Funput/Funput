package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardFeatures
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
    ): KeyboardLayout = when (mode) {
        KeyboardLayoutMode.LETTERS ->
            EditorKeyboardLayouts.resolve(inputMethod, editorMode, suggestionsEnabled)
        KeyboardLayoutMode.SYMBOLS_PRIMARY ->
            SymbolLayouts.primary(inputMethod, suggestionsEnabled, editorMode.isPassword)
        KeyboardLayoutMode.SYMBOLS_SECONDARY ->
            SymbolLayouts.secondary(inputMethod, suggestionsEnabled, editorMode.isPassword)
    }
}
