package app.funput.funput.ime

import android.content.Context
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.model.ShiftState

/**
 * The collaborators an input session is built from, constructed together.
 *
 * They are wired to each other and to the service in one place so the service itself is left with
 * lifecycle and framework callbacks. Every dependency on the service arrives as a lambda, which
 * keeps this free of a back-reference to the running IME.
 */
internal class ImeEditingSession(
    val nativeEngine: NativeVietnameseEngine,
    val editorRuntime: ImeEditorRuntime,
    val actionHandler: ImeKeyActionHandler,
    val suggestionService: PersonalSuggestionService,
    val suggestionSettings: PersonalSuggestionSettings,
) {
    /** Ends the current input without tearing down the engine. */
    fun finishInput() {
        actionHandler.finish()
        editorRuntime.finish()
        suggestionService.finish()
    }
}

internal fun createImeEditingSession(
    context: Context,
    editor: InputConnectionEditor,
    connection: () -> InputConnection?,
    cursorCapsMode: (Int) -> Int,
    currentShiftState: () -> ShiftState,
    updateShiftState: (ShiftState) -> Unit,
    showSuggestions: (List<String>) -> Unit,
    acknowledgeReset: (String) -> Unit,
): ImeEditingSession {
    val nativeEngine = NativeVietnameseEngine()
    val editorRuntime = ImeEditorRuntime(
        cursorCapsMode = cursorCapsMode,
        currentShiftState = currentShiftState,
        updateShiftState = updateShiftState,
        connection = connection,
        onSuggestionsChanged = showSuggestions,
    )
    return ImeEditingSession(
        nativeEngine = nativeEngine,
        editorRuntime = editorRuntime,
        actionHandler = ImeKeyActionHandler(
            composition = AndroidCompositionSession(nativeEngine),
            editor = editor,
            connection = connection,
            enterCommand = { editorRuntime.policy.editorAction.command },
        ),
        suggestionService = PersonalSuggestionService(
            context = context,
            show = showSuggestions,
            acknowledgeReset = acknowledgeReset,
        ),
        suggestionSettings = PersonalSuggestionSettings(context),
    )
}
