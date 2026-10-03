package app.funput.funput.ime.lifecycle

import android.content.Context
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.clipboard.session.ImeClipboardSession
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.layout.LetterPageReturn
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.layout.LetterPageReturnSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.model.ShiftState
import kotlinx.coroutines.CoroutineScope

/** Builds the editing collaborators without owning service lifecycle. */
internal fun createImeEditingSession(
    context: Context,
    scope: CoroutineScope,
    editor: InputConnectionEditor,
    connection: () -> InputConnection?,
    currentShiftState: () -> ShiftState,
    updateShiftState: (ShiftState) -> Unit,
    showSuggestions: (List<String>) -> Unit,
    acknowledgeReset: (String) -> Unit,
): ImeEditingSession {
    val nativeEngine = NativeVietnameseEngine()
    val editorRuntime = ImeEditorRuntime(
        currentShiftState = currentShiftState,
        updateShiftState = updateShiftState,
        connection = connection,
        onSuggestionsChanged = showSuggestions,
    )
    val actionHandler = ImeKeyActionHandler(
        composition = AndroidCompositionSession(nativeEngine),
        editor = editor,
        connection = connection,
        enterCommand = { editorRuntime.policy.editorAction.command },
    )
    val suggestionService = PersonalSuggestionService(
        context = context, show = showSuggestions,
        shift = currentShiftState, acknowledgeReset = acknowledgeReset,
    )
    val clipboard = ImeClipboardSession(
        context = context,
        scope = scope,
        commitText = actionHandler::onClipboardSelected,
        afterCommit = { suggestionService.consume(actionHandler.takeSuggestionUpdate()) },
        preparePanel = {
            actionHandler.finish()
            showSuggestions(emptyList())
        },
    )
    return ImeEditingSession(
        nativeEngine = nativeEngine,
        editorRuntime = editorRuntime,
        actionHandler = actionHandler,
        suggestionService = suggestionService,
        suggestionSettings = PersonalSuggestionSettings(context),
        clipboard = clipboard,
        letterReturn = LetterPageReturn(connection).also {
            it.observe(LetterPageReturnSettings(context), scope)
        },
    )
}
