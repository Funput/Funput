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
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.suggestions.PersonalSuggestionService
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.ShiftState
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
    private val systemInputMethodSwitcher by lazy { SystemInputMethodSwitcher(this) }
    private val themeRepository by lazy { installedImeThemeRepository() }
    private var keyboardView: FunputKeyboardView? = null
    private lateinit var nativeEngine: NativeVietnameseEngine
    private lateinit var actionHandler: ImeKeyActionHandler
    private lateinit var editorRuntime: ImeEditorRuntime
    private lateinit var settings: ImeSettingsController
    private lateinit var suggestionService: PersonalSuggestionService
    private lateinit var suggestionSettings: PersonalSuggestionSettings

    override fun onCreate() {
        super.onCreate()
        nativeEngine = NativeVietnameseEngine()
        suggestionSettings = PersonalSuggestionSettings(this)
        suggestionService = PersonalSuggestionService(
            context = this,
            show = { values -> keyboardView?.suggestions = values },
            acknowledgeReset = { token -> serviceScope.launch { suggestionSettings.acknowledgeReset(token) } },
        )
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
            onPersonalSuggestionsChanged = suggestionService::configure,
        )
        settings.observe(this, serviceScope)
    }
    override fun onCreateInputView(): View = FunputKeyboardView(this).also { view ->
        keyboardView = view
        updateInputView(view)
        ImeKeyboardCallbackBinder.bind(view, actionHandler, editorRuntime, suggestionService,
            systemInputMethodSwitcher)
    }
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorRuntime.configure(attribute)
        actionHandler.start(editorRuntime.policy.editorMode.supportsVietnameseComposition)
        suggestionService.start(editorRuntime.policy)
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
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            candidatesStart, candidatesEnd)
        actionHandler.onSelectionChanged(newSelStart, newSelEnd, candidatesEnd)
        suggestionService.consume(actionHandler.takeSuggestionUpdate())
        editorRuntime.updateCapitalization()
    }
    override fun onDisplayCompletions(completions: Array<out CompletionInfo>?) =
        editorRuntime.updateCompletions(completions)

    override fun onFinishInput() {
        actionHandler.finish()
        editorRuntime.finish()
        suggestionService.finish()
        super.onFinishInput()
    }

    override fun onWindowHidden() {
        suggestionService.flush()
        super.onWindowHidden()
    }

    override fun onTrimMemory(level: Int) {
        suggestionService.flush()
        super.onTrimMemory(level)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        actionHandler.finish()
        editorRuntime.finish()
        keyboardView = null
        suggestionService.close()
        // super.onDestroy() re-enters onFinishInput() while input is still open, which
        // routes back through the engine; close it only after the framework is done.
        super.onDestroy()
        nativeEngine.close()
    }

    private fun restartComposition(method: KeyboardInputMethod) {
        actionHandler.finish()
        actionHandler.start(editorRuntime.policy.editorMode.supportsVietnameseComposition)
        suggestionService.start(editorRuntime.policy)
        keyboardView?.inputMethod = method
    }

    private fun updateInputView(view: FunputKeyboardView) {
        val descriptor = themeRepository.resolve(settings.keyboardThemeId)
        view.configureForEditor(
            inputMethod = settings.inputMethod,
            policy = editorRuntime.policy,
            currentLanguage = actionHandler.language,
            feedback = settings.feedback,
            sizingProfile = settings.sizingProfile,
            keyboardTheme = descriptor.theme,
            keyboardThemeBackgroundImage = descriptor.backgroundImage,
        )
    }
}
