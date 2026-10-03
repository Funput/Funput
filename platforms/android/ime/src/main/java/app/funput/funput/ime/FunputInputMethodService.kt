package app.funput.funput.ime

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.lifecycle.ImeInputViewBinder
import app.funput.funput.ime.lifecycle.ImeRuntime
import app.funput.funput.ime.lifecycle.createImeRuntime
import app.funput.funput.ime.speech.integration.ImeSpeechSpike
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var runtime: ImeRuntime
    private val views = ImeInputViewBinder(this, serviceScope, { session }, { settings })
    private val session get() = runtime.session
    private val settings get() = runtime.settings
    private val hardwareKeyboard get() = runtime.hardwareKeyboard
    private val shortcuts get() = runtime.shortcuts
    private val speech by lazy { ImeSpeechSpike(this, session) }
    private val actionHandler get() = session.actionHandler
    private val editorRuntime get() = session.editorRuntime
    private val suggestionService get() = session.suggestionService

    override fun onCreate() {
        super.onCreate()
        runtime = createImeRuntime(this, serviceScope, views) { speech.invalidate() }
        runtime.observe(this, serviceScope) { speech.setEnabled(it) }
    }

    override fun onCreateInputView(): View = speech.wrap(views.create())

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        speech.startInput(attribute)
        editorRuntime.configure(attribute)
        editorRuntime.setAutoCapitalizeEnabled(settings.autoCapitalizeEnabled)
        session.startActionHandler()
        shortcuts.activate()
        suggestionService.start(editorRuntime.policy)
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        views.update()
        session.startInputView(editorRuntime.policy)
        editorRuntime.updateCapitalization(preserveCapsLock = false)
        speech.show()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        speech.hide()
        session.finishInputView()
        super.onFinishInputView(finishingInput)
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
        speech.selectionChanged(newSelStart, newSelEnd)
        actionHandler.onSelectionChanged(newSelStart, newSelEnd, candidatesEnd)
        suggestionService.consume(actionHandler.takeSuggestionUpdate())
        editorRuntime.updateCapitalization()
    }

    override fun onDisplayCompletions(completions: Array<out CompletionInfo>?) =
        editorRuntime.updateCompletions(completions)

    override fun onFinishInput() {
        speech.hide()
        shortcuts.cancel()
        session.finishInput()
        super.onFinishInput()
    }

    override fun onWindowHidden() {
        speech.hide()
        session.windowHidden()
        super.onWindowHidden()
    }

    override fun onTrimMemory(level: Int) {
        suggestionService.flush()
        super.onTrimMemory(level)
    }


    override fun onDestroy() {
        speech.close()
        shortcuts.cancel()
        session.close()
        serviceScope.cancel()
        actionHandler.finish()
        editorRuntime.finish()
        views.detach()
        // super.onDestroy() re-enters onFinishInput() while input is still open, which
        // routes back through the engine; close it only after the framework is done.
        super.onDestroy()
        session.closeEngines()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        speech.invalidate()
        return hardwareKeyboard.onKeyDown(event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        speech.invalidate()
        return hardwareKeyboard.onKeyUp(event) || super.onKeyUp(keyCode, event)
    }

    override fun onEvaluateInputViewShown() =
        super.onEvaluateInputViewShown() || hardwareKeyboard.showsSoftKeyboard
    // Implicit show requests (auto-show on focus) are refused separately beside a hardware keyboard.

    override fun onShowInputRequested(flags: Int, configChange: Boolean) =
        hardwareKeyboard.showsSoftKeyboard || super.onShowInputRequested(flags, configChange)


    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onConfigurationChanged(newConfig: Configuration) {
        speech.hide()
        super.onConfigurationChanged(newConfig)
        hardwareKeyboard.onConfigurationChanged(newConfig)
        views.update()
    }
}
