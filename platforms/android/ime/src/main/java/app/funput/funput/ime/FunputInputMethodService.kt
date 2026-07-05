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
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.theme.KeyboardThemeCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    private val editor = InputConnectionEditor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val systemInputMethodSwitcher by lazy { SystemInputMethodSwitcher(this) }
    private var keyboardView: FunputKeyboardView? = null
    private lateinit var nativeEngine: NativeVietnameseEngine
    private lateinit var actionHandler: ImeKeyActionHandler
    private lateinit var editorRuntime: ImeEditorRuntime
    private lateinit var settings: ImeSettingsController

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
        settings = ImeSettingsController(
            engine = nativeEngine,
            onInputMethodChanged = ::restartComposition,
            onViewSettingsChanged = { keyboardView?.let(::updateInputView) },
        )
        settings.observe(this, serviceScope)
    }

    override fun onCreateInputView(): View = FunputKeyboardView(this).also { view ->
        keyboardView = view
        updateInputView(view)
        bindCallbacks(view)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorRuntime.configure(attribute)
        actionHandler.start(settings.inputMethod, editorRuntime.policy.editorMode.supportsVietnameseComposition)
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
        onInputMethodSwitchRequested = {
            actionHandler.finish()
            systemInputMethodSwitcher.switch()
        }
        onEmojiSelected = actionHandler::onEmojiSelected
        onSuggestionSelected = ::onSuggestionSelected
    }

    private fun restartComposition(method: KeyboardInputMethod) {
        actionHandler.finish()
        actionHandler.start(method, editorRuntime.policy.editorMode.supportsVietnameseComposition)
        keyboardView?.inputMethod = method
    }

    private fun updateInputView(view: FunputKeyboardView) = view.configureForEditor(
        inputMethod = settings.inputMethod,
        policy = editorRuntime.policy,
        currentLanguage = actionHandler.language,
        feedback = settings.feedback,
        sizingProfile = settings.sizingProfile,
        keyboardTheme = KeyboardThemeCatalog.resolve(settings.keyboardThemeId),
        systemInputMethodSwitcherVisible = systemInputMethodSwitcher.isAvailable(),
    )

    private fun onSuggestionSelected(selection: SuggestionSelection) {
        if (!editorRuntime.selectCompletion(selection, actionHandler::finish)) {
            actionHandler.onSuggestionSelected(selection)
        }
    }
}
