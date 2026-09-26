package app.funput.funput.ime

import android.content.Context
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.clipboard.session.ImeClipboardSession
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.layout.LetterPageReturn
import app.funput.funput.ime.localtext.ImeLocalTextFields
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.layout.LetterPageReturnSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope

/**
 * Wires input collaborators together, leaving lifecycle and framework callbacks to the service.
 * Service dependencies arrive as lambdas.
 */
internal class ImeEditingSession(
    val nativeEngine: NativeVietnameseEngine,
    val editorRuntime: ImeEditorRuntime,
    val actionHandler: ImeKeyActionHandler,
    val suggestionService: PersonalSuggestionService,
    val suggestionSettings: PersonalSuggestionSettings,
    val clipboard: ImeClipboardSession,
    val letterReturn: LetterPageReturn,
) {
    private val localText = ImeLocalTextFields(nativeEngine) { actionHandler.language }

    fun startActionHandler() {
        actionHandler.start(
            allowComposition = editorRuntime.policy.editorMode.supportsVietnameseComposition,
            allowSuggestions = editorRuntime.policy.allowsPersonalSuggestions,
            allowShortcuts = editorRuntime.policy.allowsShortcuts,
            renderMode = editorRuntime.policy.compositionRenderMode,
        )
    }

    fun startInputView(policy: EditorInfoPolicy) {
        actionHandler.typingSession.begin()
        clipboard.start(policy.editorMode)
    }

    /** Ends the current input without tearing down the engine. */
    fun finishInput() {
        actionHandler.typingSession.end()
        clipboard.stop()
        actionHandler.finish()
        editorRuntime.finish()
        suggestionService.finish()
    }

    fun finishInputView() {
        actionHandler.typingSession.end()
        clipboard.stop()
    }

    fun bindPanels(view: FunputKeyboardView) {
        clipboard.attach(view)
        localText.attach(view)
    }

    fun restartComposition(method: KeyboardInputMethod, view: FunputKeyboardView?) {
        actionHandler.finish()
        startActionHandler()
        suggestionService.start(editorRuntime.policy)
        view?.inputMethod = method
    }

    fun windowHidden() {
        actionHandler.typingSession.end()
        clipboard.stop()
        suggestionService.flush()
    }

    fun close() {
        clipboard.close()
        suggestionService.close()
    }

    /** Only once the framework is done with input: closed engines refuse every call. */
    fun closeEngines() {
        nativeEngine.close()
        localText.close()
    }
}

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
        preparePanel = { actionHandler.finish(); showSuggestions(emptyList()) },
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
