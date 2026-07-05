package app.funput.funput.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditorRuntime
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    private val editor = InputConnectionEditor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var inputMethod = InputMethodSettings.DefaultInputMethod
    private var keyboardView: FunputKeyboardView? = null
    private lateinit var nativeEngine: NativeVietnameseEngine
    private lateinit var actionHandler: ImeKeyActionHandler
    private lateinit var editorRuntime: ImeEditorRuntime

    override fun onCreate() {
        super.onCreate()
        nativeEngine = NativeVietnameseEngine()
        editorRuntime = ImeEditorRuntime(
            cursorCapsMode = { modes -> currentInputConnection?.getCursorCapsMode(modes) ?: 0 },
            currentShiftState = { keyboardView?.shiftState ?: ShiftState.OFF },
            updateShiftState = { state -> keyboardView?.shiftState = state },
            connection = { currentInputConnection },
            onSuggestionsChanged = { suggestions -> keyboardView?.suggestions = suggestions },
        )
        actionHandler = ImeKeyActionHandler(
            composition = AndroidCompositionSession(nativeEngine),
            editor = editor,
            connection = { currentInputConnection },
            enterCommand = { editorRuntime.policy.editorAction.command },
        )
        observeInputMethod(InputMethodSettings(this))
    }
    override fun onCreateInputView(): View = FunputKeyboardView(this).also { view ->
        keyboardView = view
        view.inputMethod = inputMethod
        updateInputView(view)
        bindCallbacks(view)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorRuntime.configure(attribute)
        actionHandler.start(inputMethod, editorRuntime.policy.editorMode.supportsVietnameseComposition)
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        keyboardView?.let(::updateInputView)
        editorRuntime.updateCapitalization(preserveCapsLock = false)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
        )
        actionHandler.onSelectionChanged(newSelStart, newSelEnd, candidatesEnd)
        editorRuntime.updateCapitalization()
    }

    override fun onDisplayCompletions(completions: Array<out CompletionInfo>?) {
        editorRuntime.updateCompletions(completions)
    }

    override fun onFinishInput() {
        actionHandler.finish()
        editorRuntime.finish()
        super.onFinishInput()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        actionHandler.finish()
        editorRuntime.finish()
        nativeEngine.close()
        keyboardView = null
        super.onDestroy()
    }

    private fun bindCallbacks(view: FunputKeyboardView) = with(view.callbacks) {
        onKeyAction = actionHandler::onKeyAction
        onEmojiSelected = actionHandler::onEmojiSelected
        onSuggestionSelected = ::onSuggestionSelected
    }

    private fun observeInputMethod(settings: InputMethodSettings) {
        serviceScope.launch {
            settings.inputMethod.collect(::applyInputMethod)
        }
    }

    private fun applyInputMethod(method: KeyboardInputMethod) {
        if (method == inputMethod) return
        actionHandler.finish()
        inputMethod = method
        actionHandler.start(method, editorRuntime.policy.editorMode.supportsVietnameseComposition)
        keyboardView?.inputMethod = method
    }

    private fun updateInputView(view: FunputKeyboardView) = with(view) {
        val policy = editorRuntime.policy
        showLettersPanel()
        inputMethod = this@FunputInputMethodService.inputMethod
        editorMode = policy.editorMode
        suggestionBarEnabled = policy.showsSuggestionBar
        language = if (policy.editorMode.supportsVietnameseComposition) {
            actionHandler.language
        } else {
            KeyboardLanguage.ENGLISH
        }
        enterAction = policy.editorAction.presentation
    }

    private fun onSuggestionSelected(selection: SuggestionSelection) {
        if (!editorRuntime.selectCompletion(selection, actionHandler::finish)) {
            actionHandler.onSuggestionSelected(selection)
        }
    }
}
