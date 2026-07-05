package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object EditorKeyboardLayouts {
    fun resolve(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        suggestionBarEnabled: Boolean,
    ): KeyboardLayout {
        val layout = when (editorMode) {
            KeyboardEditorMode.TEXT -> KeyboardLayouts.forInputMethod(inputMethod)
            KeyboardEditorMode.EMAIL -> emailLayouts.getValue(inputMethod)
            KeyboardEditorMode.URL -> urlLayouts.getValue(inputMethod)
            KeyboardEditorMode.PHONE -> PhoneKeyboardLayouts.resolve(inputMethod)
            KeyboardEditorMode.PASSWORD -> PasswordKeyboardLayouts.text(inputMethod)
            KeyboardEditorMode.PIN -> PasswordKeyboardLayouts.pin(inputMethod)
            KeyboardEditorMode.NUMBER,
            KeyboardEditorMode.NUMBER_DECIMAL,
            KeyboardEditorMode.NUMBER_SIGNED,
            KeyboardEditorMode.NUMBER_SIGNED_DECIMAL,
            -> NumberKeyboardLayouts.resolve(inputMethod, editorMode)
        }
        return if (suggestionBarEnabled || layout.suggestionBar == null) {
            layout
        } else {
            layout.copy(id = "${layout.id}-no-suggestions", suggestionBar = null)
        }
    }

    private val emailLayouts = KeyboardInputMethod.entries.associateWith { method ->
        qwertyLayout(
            id = "qwerty-email-${method.name.lowercase()}",
            inputMethod = method,
            actionKeys = listOf(
                specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.25f, "Symbols"),
                specialKey("at", "@", KeyRole.PUNCTUATION, 0.85f, "At sign"),
                asciiSpaceKey(3.3f),
                specialKey("period", ".", KeyRole.PUNCTUATION, 0.8f, "Period"),
                specialKey("dot-com", ".com", KeyRole.PUNCTUATION, 1.2f, "Dot com"),
                specialKey("enter", "", KeyRole.ENTER, 1.35f, "Enter"),
            ),
        )
    }

    private val urlLayouts = KeyboardInputMethod.entries.associateWith { method ->
        qwertyLayout(
            id = "qwerty-url-${method.name.lowercase()}",
            inputMethod = method,
            actionKeys = listOf(
                specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.25f, "Symbols"),
                specialKey("slash", "/", KeyRole.PUNCTUATION, 0.8f, "Slash"),
                asciiSpaceKey(3.3f),
                specialKey("period", ".", KeyRole.PUNCTUATION, 0.8f, "Period"),
                specialKey("dot-com", ".com", KeyRole.PUNCTUATION, 1.2f, "Dot com"),
                specialKey("enter", "", KeyRole.ENTER, 1.35f, "Enter"),
            ),
        )
    }
}
