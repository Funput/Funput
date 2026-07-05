package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode

internal object KeyboardLayoutResolver {
    fun resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        suggestionBarEnabled: Boolean = true,
    ): KeyboardLayout = when (mode) {
        KeyboardLayoutMode.LETTERS ->
            EditorKeyboardLayouts.resolve(inputMethod, editorMode, suggestionBarEnabled)
        KeyboardLayoutMode.SYMBOLS_PRIMARY ->
            SymbolLayouts.primary(inputMethod, suggestionBarEnabled, editorMode.isPassword)
        KeyboardLayoutMode.SYMBOLS_SECONDARY ->
            SymbolLayouts.secondary(inputMethod, suggestionBarEnabled, editorMode.isPassword)
    }
}
