package app.funput.funput.ime.speech.integration

import android.content.BroadcastReceiver
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.R
import app.funput.funput.ime.editing.CompositionCompatibilityPolicy
import app.funput.funput.ime.speech.editor.SpeechEditorPolicy
import app.funput.funput.ime.speech.editor.SpeechEditorTracker
import app.funput.funput.ime.speech.integration.actions.SpeechSpikeInputActions
import app.funput.funput.ime.speech.integration.preparation.ImeSpeechPreparationBinding
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupReason
import app.funput.funput.ime.speech.preparation.navigation.SpeechSetupRequest
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.PlatformSpeechBackend
import app.funput.funput.ime.speech.session.SpeechPhase
import app.funput.funput.ime.speech.session.SpeechSessionController
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.FunputKeyboardView

internal class ImeSpeechSpike(
    private val service: InputMethodService,
    private val session: ImeEditingSession,
) {
    private val tracker = SpeechEditorTracker()
    private val editor = SpeechSpikeEditorGateway(service, session, tracker)
    private var host: SpeechSpikeHost? = null
    private val preparation = ImeSpeechPreparationBinding(service) { host?.message(it) }
    private val scheduler = SpeechSpikeMainScheduler()
    private val controller = SpeechSessionController(PlatformSpeechBackend(service), editor,
        scheduler, scheduler) {
        preparation.recordingError(it.backendErrorCode)
        host?.render(it)
    }
    private var closed = false
    private var enabled = true
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { hide() }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) service.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else service.registerReceiver(receiver, filter)
    }

    fun wrap(view: View): View {
        if (!service.resources.getBoolean(R.bool.speech_feature_available)) return view
        if (view is FunputKeyboardView) SpeechSpikeInputActions.bind(view, ::invalidate)
        return SpeechSpikeHost(service, view, ::start, ::stop, ::cancelSession,
            ::openPermission, ::invalidate).also { host = it }
    }

    fun startInput(info: EditorInfo) {
        invalidate()
        tracker.startInput(SpeechEditorPolicy.allows(info.inputType,
            CompositionCompatibilityPolicy.renderMode(info.packageName)), info.initialSelStart, info.initialSelEnd)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) invalidate()
    }

    fun show() {
        editor.visible = true
        preparation.show()
    }

    fun selectionChanged(start: Int, end: Int) {
        val changed = tracker.selectionChanged(start, end)
        if (editor.selectionUpdated()) return
        if (changed) {
            preparation.invalidate()
            controller.cancel()
        }
    }

    fun invalidate() = cancel(expectSelection = true)

    private fun cancelSession() = cancel(expectSelection = false)

    private fun cancel(expectSelection: Boolean) {
        preparation.invalidate()
        tracker.invalidate(expectSelection)
        editor.invalidate()
        controller.cancel()
    }

    fun hide() {
        editor.visible = false
        invalidate()
    }

    fun close() {
        if (closed) return
        closed = true
        hide()
        controller.close()
        service.unregisterReceiver(receiver)
        host = null
    }

    private fun start() {
        if (closed || !enabled || controller.state.phase !in
            listOf(SpeechPhase.IDLE, SpeechPhase.ERROR)) return
        if (!editor.permitted()) {
            host?.message(R.string.speech_spike_permission_required)
            return
        }
        val locale = if (session.actionHandler.language == KeyboardLanguage.VIETNAMESE) SpeechLocale.VI else SpeechLocale.EN
        preparation.begin(locale) {
            if (!closed && enabled && editor.permitted()) controller.start(locale)
        }
    }

    private fun stop() {
        if (preparation.stop()) cancelSession() else controller.stop()
    }

    private fun openPermission() {
        invalidate()
        try {
            val locale = if (session.actionHandler.language == KeyboardLanguage.VIETNAMESE) SpeechLocale.VI else SpeechLocale.EN
            val reason = if (editor.permitted()) SpeechSetupReason.MODEL else SpeechSetupReason.PERMISSION
            service.startActivity(SpeechSetupRequest(reason, locale).intent(service)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            host?.message(R.string.speech_spike_setup_unavailable)
        } catch (_: SecurityException) {
            host?.message(R.string.speech_spike_setup_unavailable)
        }
    }
}
