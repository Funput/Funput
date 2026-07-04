package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode

internal object KeyboardLayoutResolver {
    fun resolve(inputMethod: KeyboardInputMethod, mode: KeyboardLayoutMode): KeyboardLayout = when (mode) {
        KeyboardLayoutMode.LETTERS -> KeyboardLayouts.forInputMethod(inputMethod)
        KeyboardLayoutMode.SYMBOLS_PRIMARY -> SymbolLayouts.primary(inputMethod)
        KeyboardLayoutMode.SYMBOLS_SECONDARY -> SymbolLayouts.secondary(inputMethod)
    }
}
