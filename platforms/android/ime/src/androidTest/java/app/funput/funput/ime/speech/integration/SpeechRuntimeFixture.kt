package app.funput.funput.ime.speech.integration

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import app.funput.funput.ime.ImeKeyboardCallbackBinder
import app.funput.funput.ime.SystemInputMethodSwitcher
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.support.HostEditor
import app.funput.funput.ime.lifecycle.createImeEditingSession
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

internal class SpeechRuntimeFixture : AutoCloseable {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val host = HostEditor(context)
    val service = SpeechTestService(context, host.connection)
    val keyboard = FunputKeyboardView(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val editing = createImeEditingSession(context, scope, InputConnectionEditor(), { service.connection },
        { keyboard.shiftState }, { keyboard.shiftState = it }, { keyboard.suggestions = it }, {})
    val backend = SpeechTestBackend()
    val preparation = SpeechTestPreparation()
    val runtime = ImeSpeechRuntime(service, editing, backend, preparation)
    val info = EditorInfo().apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        packageName = "speech.test.editor"
        fieldId = 1
        initialSelStart = 0
        initialSelEnd = 0
    }

    init {
        host.layOutNarrow()
        editing.editorRuntime.configure(info)
        editing.startActionHandler()
        ImeKeyboardCallbackBinder.bind(keyboard, editing.actionHandler, editing.editorRuntime,
            editing.suggestionService, SystemInputMethodSwitcher(service), editing.letterReturn)
        editing.bindPanels(keyboard)
        runtime.bind(keyboard)
        runtime.startInput(info)
        runtime.show()
    }

    fun start(): SpeechTestRecording {
        keyboard.callbacks.onSpeechRequested!!.invoke()
        return backend.recordings.last()
    }

    override fun close() {
        runtime.close()
        editing.finishInput()
        editing.close()
        scope.cancel()
        editing.closeEngines()
    }
}
