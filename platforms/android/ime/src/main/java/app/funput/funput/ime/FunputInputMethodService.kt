package app.funput.funput.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.EditorInfoActionResolver
import app.funput.funput.ime.editing.EditorInfoKeyboardModeResolver
import app.funput.funput.ime.editing.ImeEditorAction
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** System entry point that owns the Funput keyboard view inside the IME window. */
class FunputInputMethodService : InputMethodService() {
    private val editor = InputConnectionEditor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var editorAction = ImeEditorAction.NewLine
    private var editorMode = KeyboardEditorMode.TEXT
    private var inputMethod = InputMethodSettings.DefaultInputMethod
    private var keyboardView: FunputKeyboardView? = null
    private lateinit var nativeEngine: NativeVietnameseEngine
    private lateinit var actionHandler: ImeKeyActionHandler

    override fun onCreate() {
        super.onCreate()
        nativeEngine = NativeVietnameseEngine()
        actionHandler = ImeKeyActionHandler(
            composition = AndroidCompositionSession(nativeEngine),
            editor = editor,
            connection = { currentInputConnection },
            enterCommand = { editorAction.command },
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
        resolveEditor(attribute)
        actionHandler.start(inputMethod, editorMode.supportsVietnameseComposition)
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        resolveEditor(attribute)
        keyboardView?.let(::updateInputView)
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
    }

    override fun onFinishInput() {
        actionHandler.finish()
        super.onFinishInput()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        actionHandler.finish()
        nativeEngine.close()
        keyboardView = null
        super.onDestroy()
    }

    private fun bindCallbacks(view: FunputKeyboardView) = with(view.callbacks) {
        onKeyAction = actionHandler::onKeyAction
        onEmojiSelected = actionHandler::onEmojiSelected
        onSuggestionSelected = actionHandler::onSuggestionSelected
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
        actionHandler.start(method, editorMode.supportsVietnameseComposition)
        keyboardView?.inputMethod = method
    }

    private fun resolveEditor(info: EditorInfo) {
        editorAction = EditorInfoActionResolver.resolve(info)
        editorMode = EditorInfoKeyboardModeResolver.resolve(info)
    }

    private fun updateInputView(view: FunputKeyboardView) = with(view) {
        showLettersPanel()
        inputMethod = this@FunputInputMethodService.inputMethod
        editorMode = this@FunputInputMethodService.editorMode
        language = if (editorMode.supportsVietnameseComposition) {
            actionHandler.language
        } else {
            KeyboardLanguage.ENGLISH
        }
        enterAction = editorAction.presentation
    }
}
