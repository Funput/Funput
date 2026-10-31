package app.funput.funput.ime.lifecycle

import android.content.Context
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.clipboard.session.ImeClipboardSession
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.correction.createTypoCorrection
import app.funput.funput.ime.editing.entersend.EnterToSendHosts
import app.funput.funput.ime.editing.layout.LetterPageReturn
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.entersend.EnterToSendSettings
import app.funput.funput.ime.settings.layout.LetterPageReturnSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.ime.suggestions.SeedLanguageReportingEngine
import app.funput.funput.ime.suggestions.bar.SuggestionBarState
import app.funput.funput.keyboard.suggestion.SuggestionChip
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
    showSuggestions: (List<SuggestionChip>) -> Unit,
    acknowledgeReset: (String) -> Unit,
    onEnterKeyChanged: () -> Unit = {},
): ImeEditingSession {
    val nativeEngine = NativeVietnameseEngine()
    val bar = SuggestionBarState(showSuggestions)
    val enterToSend = EnterToSendHosts()
    val editorRuntime = ImeEditorRuntime(
        currentShiftState = currentShiftState,
        updateShiftState = updateShiftState,
        connection = connection,
        onSuggestionsChanged = bar::showPersonal,
        enterToSendPackages = { enterToSend.packages },
    )
    enterToSend.observe(EnterToSendSettings(context), scope) {
        if (editorRuntime.refreshEnterToSend()) onEnterKeyChanged()
    }
    val typo = createTypoCorrection(context, nativeEngine) { editorRuntime.policy }
    val suggestionService = PersonalSuggestionService(
        context = context, show = bar::showPersonal,
        shift = currentShiftState, acknowledgeReset = acknowledgeReset,
    )
    // Every configure and VI/EN switch of the document's composer goes through this, so
    // the shipped suggestion data follows the composer from one place.
    val composer = SeedLanguageReportingEngine(nativeEngine, suggestionService::setSeedLanguage)
    val composition = AndroidCompositionSession(composer, corrections = typo.corrections)
    val actionHandler = ImeKeyActionHandler(
        composition = composition,
        editor = editor,
        connection = connection,
        enterCommand = { editorRuntime.policy.editorAction.command },
    )
    val clipboard = ImeClipboardSession(
        context = context,
        scope = scope,
        commitText = actionHandler::onClipboardSelected,
        afterCommit = { suggestionService.consume(actionHandler.takeSuggestionUpdate()) },
        preparePanel = {
            actionHandler.finish()
            bar.showPersonal(emptyList())
        },
    )
    return ImeEditingSession(
        nativeEngine = nativeEngine,
        typo = typo,
        composition = composition,
        bar = bar,
        connection = connection,
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
