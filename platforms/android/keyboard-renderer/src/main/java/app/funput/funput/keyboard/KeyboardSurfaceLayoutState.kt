package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode

/** Owns layout-affecting state and rebuilds the immutable layout when it changes. */
internal class KeyboardSurfaceLayoutState(private val onLayoutChanged: () -> Unit) {
    var inputMethod = KeyboardInputMethod.TELEX
        set(value) {
            if (field == value) return
            field = value
            refresh()
        }
    var layoutMode = KeyboardLayoutMode.LETTERS
        set(value) {
            if (field == value) return
            field = value
            refresh()
        }
    var editorMode = KeyboardEditorMode.TEXT
        set(value) {
            if (field == value) return
            field = value
            refresh()
        }
    var suggestionsEnabled = KeyboardFeatures.SuggestionsEnabled
        set(value) {
            if (field == value) return
            field = value
            refresh()
        }
    var systemInputMethodSwitcherVisible = false
        set(value) {
            if (field == value) return
            field = value
            refresh()
        }

    var layout: KeyboardLayout = resolve()
        private set

    private fun refresh() {
        layout = resolve()
        onLayoutChanged()
    }

    private fun resolve() = KeyboardLayoutResolver.resolve(
        inputMethod = inputMethod,
        mode = layoutMode,
        editorMode = editorMode,
        suggestionsEnabled = suggestionsEnabled,
        systemInputMethodSwitcherVisible = systemInputMethodSwitcherVisible,
    )
}
