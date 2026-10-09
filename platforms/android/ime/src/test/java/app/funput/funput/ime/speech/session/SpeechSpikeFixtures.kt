package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechBackend
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.model.SpeechRecording

internal class SpeechSpikeFixture {
    val backend = SpikeBackend()
    val editor = SpikeEditor()
    val scheduler = SpikeScheduler()
    val states = mutableListOf<SpeechSessionState>()
    var onState: (SpeechSessionState) -> Unit = {}
    val controller = SpeechSessionController(backend, editor, scheduler, scheduler) {
        states += it
        onState(it)
    }

    fun start(): SpikeRecording {
        controller.start(SpeechLocale.VI)
        return backend.recordings.last()
    }
}

internal class SpikeBackend : SpeechBackend {
    val recordings = mutableListOf<SpikeRecording>()
    val locales = mutableListOf<SpeechLocale>()
    var onStart: (SpikeRecording) -> Unit = {}

    override fun open(locale: SpeechLocale, listener: (SpeechEvent) -> Unit): SpeechRecording {
        locales += locale
        return SpikeRecording(listener) { onStart(it) }.also { recordings += it }
    }
}

internal class SpikeRecording(
    private val listener: (SpeechEvent) -> Unit,
    private val onStart: (SpikeRecording) -> Unit,
) : SpeechRecording {
    var starts = 0
    var stops = 0
    var cancels = 0
    var closes = 0
    var onStop: () -> Unit = {}
    var onClose: () -> Unit = {}

    fun emit(event: SpeechEvent) = listener(event)

    override fun start() {
        starts++
        onStart(this)
    }

    override fun stop() {
        stops++
        onStop()
    }

    override fun cancel() {
        cancels++
    }

    override fun close() {
        closes++
        onClose()
    }
}

internal class SpikeEditor : SpeechEditorGateway {
    var anchor: SpeechEditorAnchor? = SpeechEditorAnchor(1, 0, 0)
    val commits = mutableListOf<String>()
    var commitAttempts = 0
    var accepts = true
    var onCommit: () -> Unit = {}
    var onPrepare: () -> Unit = {}

    var automaticPreparation = true
    var preparationCancels = 0
    var prepared: ((SpeechEditorAnchor?) -> Unit)? = null
    var onValidate: () -> Unit = {}

    override fun prepareAnchor(listener: (SpeechEditorAnchor?) -> Unit): SpeechCancellation {
        prepared = listener
        onPrepare()
        if (automaticPreparation) listener(anchor)
        return SpeechCancellation { preparationCancels++ }
    }

    override fun isValid(anchor: SpeechEditorAnchor): Boolean {
        onValidate()
        return this.anchor == anchor
    }

    override fun commit(anchor: SpeechEditorAnchor, text: String): Boolean {
        commitAttempts++
        onCommit()
        if (!accepts || this.anchor != anchor) return false
        commits += text
        return true
    }
}

internal class SpikeScheduler : SpeechScheduler, SpeechClock {
    override fun nowMillis() = now
    class Task(val at: Long, val action: () -> Unit) : SpeechCancellation {
        var cancellations = 0
        var delivered = false
        override fun cancel() {
            cancellations++
        }
    }

    var now = 0L
    val tasks = mutableListOf<Task>()

    override fun schedule(delayMillis: Long, task: () -> Unit): SpeechCancellation {
        return Task(now + delayMillis, task).also { tasks += it }
    }

    fun advance(millis: Long) {
        val target = now + millis
        while (true) {
            val next = tasks.filter { !it.delivered && it.cancellations == 0 && it.at <= target }
                .minByOrNull { it.at } ?: break
            now = next.at
            next.delivered = true
            next.action()
        }
        now = target
    }
}
