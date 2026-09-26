package app.funput.funput.ime

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.hardware.HardwareKeyboard
import app.funput.funput.ime.shortcuts.ImeShortcutsController
import app.funput.funput.ime.shortcuts.createImeShortcutsController
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.emoji.EmojiCatalogPreloader
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
    private lateinit var hardwareKeyboard: HardwareKeyboard
    private lateinit var shortcuts: ImeShortcutsController
    private val nativeEngine get() = session.nativeEngine
    private val actionHandler get() = session.actionHandler
    private val editorRuntime get() = session.editorRuntime
    private val suggestionService get() = session.suggestionService
    override fun onCreate() {
        super.onCreate()
        session = createImeEditingSession(
            context = this,
            scope = serviceScope,
            editor = editor,
            connection = { currentInputConnection },
            currentShiftState = { keyboardView?.shiftState ?: ShiftState.OFF },
            updateShiftState = { state -> keyboardView?.shiftState = state },
            showSuggestions = { values -> keyboardView?.suggestions = values },
            acknowledgeReset = { token ->
                serviceScope.launch { session.suggestionSettings.acknowledgeReset(token) }
            },
        )
        settings = ImeSettingsController(
            engine = nativeEngine,
            onInputMethodChanged = { method -> session.restartComposition(method, keyboardView) },
            onViewSettingsChanged = { keyboardView?.let(::updateInputView) },
            onPersonalSuggestionsChanged = suggestionService::configure,
            onAutoCapitalizeChanged = editorRuntime::setAutoCapitalizeEnabled,
        )
        settings.observe(this, serviceScope)
        shortcuts = createImeShortcutsController(this, serviceScope, actionHandler)
        hardwareKeyboard = HardwareKeyboard.bind(this, session, serviceScope) { keyboardView?.shiftState ?: ShiftState.OFF }
    }
    override fun onCreateInputView(): View = FunputKeyboardView(this).also { view ->
        keyboardView = view
        updateInputView(view)
        ImeKeyboardCallbackBinder.bind(view, actionHandler, editorRuntime, suggestionService,
            systemInputMethodSwitcher)
        ImePlacementBinder.bind(view, this, serviceScope)
        session.bindPanels(view)
        EmojiCatalogPreloader.schedule(view)
    }
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorRuntime.configure(attribute)
        editorRuntime.setAutoCapitalizeEnabled(settings.autoCapitalizeEnabled)
        session.startActionHandler()
        shortcuts.activate()
        suggestionService.start(editorRuntime.policy)
    }
    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        keyboardView?.let(::updateInputView)
        session.startInputView(editorRuntime.policy)
        editorRuntime.updateCapitalization(preserveCapsLock = false)
    }
    override fun onFinishInputView(finishingInput: Boolean) =
        session.finishInputView().also { super.onFinishInputView(finishingInput) }
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
        shortcuts.cancel()
        session.finishInput()
        super.onFinishInput()
    }
    override fun onWindowHidden() = session.windowHidden().also { super.onWindowHidden() }
    override fun onTrimMemory(level: Int) {
        suggestionService.flush()
        super.onTrimMemory(level)
    }

    override fun onDestroy() {
        shortcuts.cancel()
        session.close()
        serviceScope.cancel()
        actionHandler.finish()
        editorRuntime.finish()
        keyboardView = null
        // super.onDestroy() re-enters onFinishInput() while input is still open, which
        // routes back through the engine; close it only after the framework is done.
        super.onDestroy()
        session.closeEngines()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent) =
        hardwareKeyboard.onKeyDown(event) || super.onKeyDown(keyCode, event)
    override fun onKeyUp(keyCode: Int, event: KeyEvent) =
        hardwareKeyboard.onKeyUp(event) || super.onKeyUp(keyCode, event)
    override fun onEvaluateInputViewShown() =
        super.onEvaluateInputViewShown() || hardwareKeyboard.showsSoftKeyboard
    // Implicit show requests (auto-show on focus) are refused separately beside a hardware keyboard.
    override fun onShowInputRequested(flags: Int, configChange: Boolean) =
        hardwareKeyboard.showsSoftKeyboard || super.onShowInputRequested(flags, configChange)

    override fun onEvaluateFullscreenMode(): Boolean = false
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hardwareKeyboard.onConfigurationChanged(newConfig)
        keyboardView?.let(::updateInputView)
    }

    private fun updateInputView(view: FunputKeyboardView) {
        view.applyImeState(settings, editorRuntime.policy, actionHandler.language, themeRepository, isDarkAppearance())
        actionHandler.smartGesturesEnabled = view.areSmartGesturesEnabled
    }
}
