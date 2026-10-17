package app.funput.funput.ime

import android.util.Log
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.clipboard.session.ImeClipboardSession
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.correction.TypoCorrectionParts
import app.funput.funput.ime.editing.correction.preview.CorrectionBarActions
import app.funput.funput.ime.editing.correction.preview.CorrectionPreviewer
import app.funput.funput.ime.editing.correction.report.TypoSessionReport
import app.funput.funput.ime.editing.layout.LetterPageReturn
import app.funput.funput.ime.localtext.ImeLocalTextFields
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.ime.suggestions.bar.SuggestionBarState
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionChipKind
import app.funput.funput.keyboard.ui.FunputKeyboardView

/**
 * Wires input collaborators together, leaving lifecycle and framework callbacks to the service.
 * Service dependencies arrive as lambdas.
 */
internal class ImeEditingSession(
    val nativeEngine: NativeVietnameseEngine,
    val typo: TypoCorrectionParts,
    private val composition: AndroidCompositionSession,
    private val bar: SuggestionBarState,
    connection: () -> InputConnection?,
    val editorRuntime: ImeEditorRuntime,
    val actionHandler: ImeKeyActionHandler,
    val suggestionService: PersonalSuggestionService,
    val suggestionSettings: PersonalSuggestionSettings,
    val clipboard: ImeClipboardSession,
    val letterReturn: LetterPageReturn,
) {
    private val localText = ImeLocalTextFields(nativeEngine) { actionHandler.language }
    private val previewer = CorrectionPreviewer(typo.corrections)
    private val correctionBar = CorrectionBarActions(composition, connection) { dispatch(KeyAction.Space) }
    private val typoReport = TypoSessionReport(typo.corrections) { Log.i("FunputTypo", it) }

    /** One key, from the soft keyboard or a hardware one, and everything it updates. */
    fun dispatch(action: KeyAction) {
        actionHandler.onKeyAction(action)
        suggestionService.consume(actionHandler.takeSuggestionUpdate())
        refreshCorrection()
    }

    /** A tap on the bar, routed by what the slot does. */
    fun select(selection: SuggestionSelection) {
        when (selection.kind) {
            SuggestionChipKind.CORRECTION_TYPED -> correctionBar.refuse(selection.text)
            SuggestionChipKind.CORRECTION_CANDIDATE -> correctionBar.accept(selection.text)
            SuggestionChipKind.CORRECTION_UNDO -> correctionBar.undo()
            SuggestionChipKind.SUGGESTION -> bar.personalSelection(selection)?.let { picked ->
                if (!editorRuntime.selectCompletion(picked, actionHandler::finish)) {
                    suggestionService.select(picked, actionHandler)
                }
            }
        }
        refreshCorrection()
    }

    private fun refreshCorrection() = bar.showCorrection(previewer.compute(composition.composingText))

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
        refreshCorrection()
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
        typoReport.report()
    }

    fun close() {
        clipboard.close()
        suggestionService.close()
        typo.wordSource.close()
    }

    /** Only once the framework is done with input: closed engines refuse every call. */
    fun closeEngines() {
        nativeEngine.close()
        localText.close()
    }
}
