package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object EditorKeyboardLayouts {
    fun resolve(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        suggestionsEnabled: Boolean,
    ): KeyboardLayout {
        val layout = when (editorMode) {
            KeyboardEditorMode.TEXT -> KeyboardLayouts.forInputMethod(inputMethod)
            KeyboardEditorMode.SEARCH -> searchLayouts.getValue(inputMethod)
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
        return if (suggestionsEnabled || layout.suggestionBar == null) {
            layout
        } else {
            layout.copy(
                id = "${layout.id}-no-suggestions",
                suggestionBar = layout.suggestionBar.copy(suggestionsEnabled = false),
            )
        }
    }

    private val emailLayouts = KeyboardInputMethod.entries.associateWith { method ->
        qwertyLayout(
            id = "qwerty-email-${method.name.lowercase()}",
            inputMethod = method,
            leadingRows = listOf(
                topNumberRow(TopNumberRowMode.PLAIN_CHARACTER, pageId = "email-${method.name.lowercase()}"),
            ),
            actionKeys = webActionKeys(
                middleId = "at",
                middleLabel = "@",
                middleAccessibilityLabel = "At sign",
            ),
        )
    }

    private val searchLayouts = KeyboardInputMethod.entries.associateWith { method ->
        webNavigationLayout(
            idPrefix = "search",
            inputMethod = method,
        )
    }

    private val urlLayouts = KeyboardInputMethod.entries.associateWith { method ->
        webNavigationLayout(
            idPrefix = "url",
            inputMethod = method,
        )
    }

    private fun webNavigationLayout(
        idPrefix: String,
        inputMethod: KeyboardInputMethod,
    ): KeyboardLayout = qwertyLayout(
        id = "qwerty-$idPrefix-${inputMethod.name.lowercase()}",
        inputMethod = inputMethod,
        leadingRows = listOf(
            topNumberRow(TopNumberRowMode.PLAIN_CHARACTER, pageId = "$idPrefix-${inputMethod.name.lowercase()}"),
        ),
        actionKeys = webActionKeys(
            middleId = "slash",
            middleLabel = "/",
            middleAccessibilityLabel = "Slash",
        ),
    )

    private fun webActionKeys(
        middleId: String,
        middleLabel: String,
        middleAccessibilityLabel: String,
    ) = listOf(
        specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.7f, "Symbols"),
        specialKey(middleId, middleLabel, KeyRole.PUNCTUATION, accessibilityLabel = middleAccessibilityLabel),
        asciiSpaceKey(5.8f),
        specialKey("period", ".", KeyRole.PUNCTUATION, accessibilityLabel = "Period"),
        specialKey("enter", "", KeyRole.ENTER, 1.7f, "Enter"),
    )
}
