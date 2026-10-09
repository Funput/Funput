package app.funput.funput.ime.speech.integration.flow

import app.funput.funput.ime.speech.integration.ImeSpeechUiBinder
import app.funput.funput.ime.speech.integration.presentation.SpeechNotice
import app.funput.funput.ime.speech.integration.presentation.SpeechUiPort
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.*
import app.funput.funput.ime.speech.session.SpikeBackend
import app.funput.funput.ime.speech.session.SpikeEditor
import app.funput.funput.ime.speech.session.SpikeScheduler
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.speech.SpeechPanelState

internal class SpeechFlowFixture {
    val backend = SpikeBackend()
    val editor = SpikeEditor()
    val scheduler = SpikeScheduler()
    val preparation = FlowPreparation()
    val port = FlowUiPort()
    val ui = ImeSpeechUiBinder(port)
    var eligible = true
    var permitted = true
    var language = KeyboardLanguage.VIETNAMESE
    val invalidations = mutableListOf<Boolean>()
    val setups = mutableListOf<SpeechLocale>()
    val flow = ImeSpeechFlow(backend, editor, preparation, scheduler, scheduler, ui,
        { eligible }, { permitted }, { language }, { invalidations += it }, { setups += it })

    fun start() = flow.start().also { preparation.operations.last().deliver(SpeechCapability.READY) }
}

internal class FlowUiPort : SpeechUiPort {
    val panels = mutableListOf<SpeechPanelState>()
    var dismissals = 0
    var microphoneVisible = false
    var microphoneActive = false
    var onPresent: () -> Unit = {}
    var onDismiss: () -> Unit = {}
    override fun present(state: SpeechPanelState) { panels += state; onPresent() }
    override fun dismiss() { dismissals++; onDismiss() }
    override fun microphone(visible: Boolean, active: Boolean, language: KeyboardLanguage) {
        microphoneVisible = visible
        microphoneActive = active
    }
    override fun message(notice: SpeechNotice) = notice.name
}

internal class FlowPreparation : SpeechPreparationService {
    val operations = mutableListOf<FlowOperation>()
    var onStart: (FlowOperation) -> Unit = {}
    var invalidations = 0
    var failure: RuntimeException? = null
    override fun availability() = SpeechAvailability.AVAILABLE
    override fun invalidate(locale: SpeechLocale?) { invalidations++ }
    override fun check(locale: SpeechLocale, listener: (SpeechCapability) -> Unit): SpeechPreparationOperation {
        failure?.let { throw it }
        return FlowOperation(listener) { onStart(it) }.also { operations += it }
    }
    override fun download(locale: SpeechLocale, listener: (SpeechDownloadEvent) -> Unit): SpeechPreparationOperation =
        error("An IME must not download models")
}

internal class FlowOperation(
    private val listener: (SpeechCapability) -> Unit,
    private val onStart: (FlowOperation) -> Unit,
) : SpeechPreparationOperation {
    var starts = 0
    var closes = 0
    fun deliver(capability: SpeechCapability) = listener(capability)
    override fun start() { starts++; onStart(this) }
    override fun close() { closes++ }
}
