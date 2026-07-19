package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyboardEditorMode

internal enum class ImeSuggestionSource {
    NONE,
    FUNPUT,
    EDITOR,
}

internal data class EditorInfoPolicy(
    val editorMode: KeyboardEditorMode,
    val editorAction: ImeEditorAction,
    val capitalizationModes: Int,
    val isMultiline: Boolean,
    val suggestionSource: ImeSuggestionSource,
    val allowsPersonalizedLearning: Boolean,
    val allowsPersonalSuggestions: Boolean,
) {
    val showsSuggestionBar: Boolean get() = suggestionSource != ImeSuggestionSource.NONE

    companion object {
        val Default = EditorInfoPolicy(
            editorMode = KeyboardEditorMode.TEXT,
            editorAction = ImeEditorAction.NewLine,
            capitalizationModes = 0,
            isMultiline = false,
            suggestionSource = ImeSuggestionSource.FUNPUT,
            allowsPersonalizedLearning = true,
            allowsPersonalSuggestions = true,
        )
    }
}
