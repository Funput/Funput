package app.funput.funput.ime.speech.integration

import android.inputmethodservice.InputMethodService
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.editing.CompositionCompatibilityPolicy
import app.funput.funput.ime.speech.editor.SpeechEditorPolicy
import app.funput.funput.ime.speech.editor.SpeechEditorTracker
import app.funput.funput.ime.speech.integration.actions.ImeSpeechInputActions
import app.funput.funput.ime.speech.integration.flow.ImeSpeechFlow
import app.funput.funput.ime.speech.integration.lifecycle.ImeSpeechEnvironment
import app.funput.funput.ime.speech.integration.presentation.AndroidSpeechUiPort
import app.funput.funput.ime.speech.integration.presentation.ImeSpeechSetupLauncher
import app.funput.funput.ime.speech.model.SpeechBackend
import app.funput.funput.ime.speech.preparation.SpeechPreparationService
import app.funput.funput.ime.speech.platform.PlatformSpeechBackend
import app.funput.funput.ime.speech.preparation.OnDeviceSpeechPreparationService
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.keyboard.ui.FunputKeyboardView

/** Owns collaborators for the lifetime of the IME. Showing only probes availability. */
internal class ImeSpeechRuntime(
    private val service: InputMethodService,
    editing: ImeEditingSession,
    backend: SpeechBackend = PlatformSpeechBackend(service),
    private val preparation: SpeechPreparationService = OnDeviceSpeechPreparationService(service),
) {
    private val tracker = SpeechEditorTracker()
    private val editor = ImeSpeechEditorGateway(service, editing, tracker)
    private val port = AndroidSpeechUiPort(service)
    private val ui = ImeSpeechUiBinder(port)
    private val scheduler = ImeSpeechMainScheduler()
    private var enabled = true
    private var available = false
    private var closed = false
    private val flow = ImeSpeechFlow(backend, editor, preparation, scheduler,
        scheduler, ui, ::eligible, editor::permitted, { editing.actionHandler.language }, {
            tracker.invalidate(it)
            editor.invalidate()
        }, { locale ->
            if (!ImeSpeechSetupLauncher.open(service, locale, editor.permitted())) flowSetupFailed()
        })
    private val environment = ImeSpeechEnvironment(service, ::hide)

    fun bind(view: FunputKeyboardView) {
        if (closed) return
        flow.cancel(false)
        port.attach(view)
        ImeSpeechInputActions.bind(view, { !closed && port.isCurrent(view) }, { port.presenting },
            ::invalidate, flow::refresh, flow::start, flow::action)
        flow.refresh()
    }

    fun startInput(info: EditorInfo) {
        if (closed) return
        flow.cancel(false)
        tracker.startInput(SpeechEditorPolicy.allows(info.inputType,
            CompositionCompatibilityPolicy.renderMode(info.packageName)), info.initialSelStart, info.initialSelEnd)
        flow.refresh()
    }

    fun show() {
        if (closed) return
        // Setup owns a separate facade; models may have changed while this view was hidden.
        if (!editor.visible) preparation.invalidate()
        editor.visible = true
        available = preparation.availability() == SpeechAvailability.AVAILABLE
        flow.refresh()
    }

    fun setEnabled(value: Boolean) {
        if (closed) return
        enabled = value
        if (!value) flow.cancel(false)
        flow.refresh()
    }

    fun selectionChanged(start: Int, end: Int) {
        if (closed) return
        val changed = tracker.selectionChanged(start, end)
        if (editor.selectionUpdated()) return
        if (changed && !flow.committing) flow.cancel(false)
        flow.refresh()
    }

    fun invalidate(expectSelection: Boolean = true) {
        if (!closed) flow.cancel(expectSelection)
    }

    fun hide() {
        if (closed) return
        editor.visible = false
        flow.cancel(false)
    }

    fun back() = !closed && flow.back()

    fun close() {
        if (closed) return
        closed = true
        editor.visible = false
        flow.close()
        environment.close()
        port.detach()
    }

    private fun eligible(): Boolean = !closed && enabled && available && editor.visible &&
        editor.foreground() && tracker.anchor() != null

    private fun flowSetupFailed(): Unit = flow.setupFailed()
}
