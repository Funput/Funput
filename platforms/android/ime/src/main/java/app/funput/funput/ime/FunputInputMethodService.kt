package app.funput.funput.ime

import android.content.res.Configuration
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
    private lateinit var session: ImeEditingSession
    private lateinit var settings: ImeSettingsController

    private val nativeEngine get() = session.nativeEngine
    private val actionHandler get() = session.actionHandler
    private val editorRuntime get() = session.editorRuntime
    private val suggestionService get() = session.suggestionService

    override fun onCreate() {
        super.onCreate()
        session = createImeEditingSession(
            context = this,
            editor = editor,
            connection = { currentInputConnection },
            cursorCapsMode = { modes -> currentInputConnection?.getCursorCapsMode(modes) ?: 0 },
            currentShiftState = { keyboardView?.shiftState ?: ShiftState.OFF },
            updateShiftState = { state -> keyboardView?.shiftState = state },
            showSuggestions = { values -> keyboardView?.suggestions = values },
            acknowledgeReset = { token ->
                serviceScope.launch { session.suggestionSettings.acknowledgeReset(token) }
            },
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
        session.finishInput()
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

    // Switching the system between light and dark changes which theme applies, and no settings
    // flow fires for it because nothing the user stored has changed.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        keyboardView?.let(::updateInputView)
    }

    private fun updateInputView(view: FunputKeyboardView) = view.applyImeState(
        settings = settings,
        policy = editorRuntime.policy,
        currentLanguage = actionHandler.language,
        themeRepository = themeRepository,
        darkAppearance = isDarkAppearance(),
    )
}
