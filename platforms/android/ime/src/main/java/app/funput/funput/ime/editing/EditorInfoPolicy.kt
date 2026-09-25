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
    /**
     * Whether the field is one a sentence is ever typed into. False for passwords,
     * addresses and non-text editors, which is what keeps Funput's own sentence
     * detection out of them now that it no longer waits to be asked.
     */
    val allowsAutoCapitalization: Boolean,
    val isMultiline: Boolean,
    val suggestionSource: ImeSuggestionSource,
    val allowsPersonalizedLearning: Boolean,
    val allowsPersonalSuggestions: Boolean,
    val allowsShortcuts: Boolean,
    val compositionRenderMode: CompositionRenderMode,
) {
    val showsSuggestionBar: Boolean get() = suggestionSource != ImeSuggestionSource.NONE

    companion object {
        val Default = EditorInfoPolicy(
            editorMode = KeyboardEditorMode.TEXT,
            editorAction = ImeEditorAction.NewLine,
            capitalizationModes = 0,
            allowsAutoCapitalization = true,
            isMultiline = false,
            suggestionSource = ImeSuggestionSource.FUNPUT,
            allowsPersonalizedLearning = true,
            allowsPersonalSuggestions = true,
            allowsShortcuts = true,
            compositionRenderMode = CompositionRenderMode.COMPOSING,
        )
    }
}
