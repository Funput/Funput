package app.funput.funput.ime.editing

import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection

/** Owns editor-scoped policy and clears transient state whenever the editor changes. */
internal class ImeEditorRuntime(
    cursorCapsMode: (requestedModes: Int) -> Int,
    currentShiftState: () -> ShiftState,
    updateShiftState: (ShiftState) -> Unit,
    connection: () -> InputConnection?,
    onSuggestionsChanged: (List<String>) -> Unit,
) {
    private val autoCapitalization = AutoCapitalizationController(
        cursorCapsMode,
        currentShiftState,
        updateShiftState,
    )
    private val completions = EditorCompletionSession<CompletionInfo>(
        text = { completion -> completion.text },
        commit = { completion -> connection()?.commitCompletion(completion) == true },
        onSuggestionsChanged = onSuggestionsChanged,
    )

    var policy: EditorInfoPolicy = EditorInfoPolicy.Default
        private set

    fun configure(info: EditorInfo) {
        policy = EditorInfoPolicyResolver.resolve(info)
        autoCapitalization.configure(policy.capitalizationModes)
        completions.configure(policy.suggestionSource == ImeSuggestionSource.EDITOR)
    }

    fun finish() {
        policy = EditorInfoPolicy.Default
        autoCapitalization.configure(capitalizationModes = 0)
        completions.configure(enabled = false)
    }

    fun setAutoCapitalizeEnabled(enabled: Boolean) {
        autoCapitalization.setEnabled(enabled)
        autoCapitalization.update()
    }

    fun updateCapitalization(preserveCapsLock: Boolean = true) =
        autoCapitalization.update(preserveCapsLock)

    fun updateCompletions(values: Array<out CompletionInfo>?) = completions.update(values)

    fun selectCompletion(selection: SuggestionSelection, beforeCommit: () -> Unit): Boolean {
        if (policy.suggestionSource != ImeSuggestionSource.EDITOR) return false
        beforeCommit()
        return completions.select(selection)
    }
}
