package app.funput.funput.ime

import android.content.Context
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.clipboard.controller.ImeClipboardController
import app.funput.funput.ime.clipboard.controller.ImeClipboardHistoryController
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.AndroidClipboardGateway
import app.funput.funput.ime.clipboard.ui.ImeClipboardUiBinding
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.ClipboardSettings
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope

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
    val clipboardController: ImeClipboardController,
    val clipboardHistoryController: ImeClipboardHistoryController,
    val clipboardUiBinding: ImeClipboardUiBinding,
) {
    fun startActionHandler() {
        actionHandler.start(
            allowComposition = editorRuntime.policy.editorMode.supportsVietnameseComposition,
            allowSuggestions = editorRuntime.policy.allowsPersonalSuggestions,
            renderMode = editorRuntime.policy.compositionRenderMode,
        )
    }

    fun startInputView(policy: EditorInfoPolicy) =
        Unit.also {
            clipboardController.start(policy.editorMode)
            clipboardHistoryController.start(policy.editorMode)
        }

    /** Ends the current input without tearing down the engine. */
    fun finishInput() {
        clipboardController.stop()
        clipboardHistoryController.stop()
        actionHandler.finish()
        editorRuntime.finish()
        suggestionService.finish()
    }

    fun finishInputView() { clipboardController.stop(); clipboardHistoryController.stop() }

    fun bindClipboard(view: FunputKeyboardView) = clipboardUiBinding.attach(view)

    fun restartComposition(method: KeyboardInputMethod, view: FunputKeyboardView?) {
        actionHandler.finish()
        startActionHandler()
        suggestionService.start(editorRuntime.policy)
        view?.inputMethod = method
    }

    fun windowHidden() {
        clipboardController.stop()
        clipboardHistoryController.stop()
        suggestionService.flush()
    }

    fun close() {
        clipboardUiBinding.close()
        clipboardHistoryController.close()
        clipboardController.close()
        suggestionService.close()
    }
}

internal fun createImeEditingSession(
    context: Context,
    scope: CoroutineScope,
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
    val actionHandler = ImeKeyActionHandler(
        composition = AndroidCompositionSession(nativeEngine),
        editor = editor,
        connection = connection,
        enterCommand = { editorRuntime.policy.editorAction.command },
    )
    val suggestionService = PersonalSuggestionService(
        context = context, show = showSuggestions,
        capitalized = { currentShiftState().isActive }, acknowledgeReset = acknowledgeReset,
    )
    val clipboardPreferences = ClipboardSettings(context).preferences
    val clipboardController = ImeClipboardController(
        parentScope = scope,
        preferences = clipboardPreferences,
        gateway = AndroidClipboardGateway(context),
        storeFactory = { expiry -> ClipboardHistoryStore.from(context, expiry) },
        commitText = actionHandler::onClipboardSelected,
        afterCommit = { suggestionService.consume(actionHandler.takeSuggestionUpdate()) },
    )
    val clipboardHistoryController = ImeClipboardHistoryController(
        parentScope = scope,
        preferences = clipboardPreferences,
        storeFactory = { expiry -> ClipboardHistoryStore.from(context, expiry) },
        commitText = actionHandler::onClipboardSelected,
        afterCommit = { suggestionService.consume(actionHandler.takeSuggestionUpdate()) },
        preparePanel = { actionHandler.finish(); showSuggestions(emptyList()) },
        onCleared = clipboardController::historyCleared,
    )
    val clipboardUiBinding = ImeClipboardUiBinding(
        context, scope, clipboardController, clipboardHistoryController,
    )
    return ImeEditingSession(
        nativeEngine = nativeEngine,
        editorRuntime = editorRuntime,
        actionHandler = actionHandler,
        suggestionService = suggestionService,
        suggestionSettings = PersonalSuggestionSettings(context),
        clipboardController = clipboardController,
        clipboardHistoryController = clipboardHistoryController,
        clipboardUiBinding = clipboardUiBinding,
    )
}
